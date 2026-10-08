// Optional Windows NTFS transaction backend. Staging never commits public data.
#include <bcrypt.h>
#include <ktmw32.h>
#include <memory>
#include <limits>

namespace recovery_storage {
struct Api {
    HMODULE module=nullptr;
    decltype(&CreateTransaction) create=nullptr;
    decltype(&CommitTransaction) commit=nullptr;
    decltype(&RollbackTransaction) rollback=nullptr;
    decltype(&CreateFileTransactedW) open=nullptr;
    Api() {
        module=LoadLibraryExW(L"KtmW32.dll",nullptr,LOAD_LIBRARY_SEARCH_SYSTEM32);
        if(module) {
            create=reinterpret_cast<decltype(create)>(GetProcAddress(module,"CreateTransaction"));
            commit=reinterpret_cast<decltype(commit)>(GetProcAddress(module,"CommitTransaction"));
            rollback=reinterpret_cast<decltype(rollback)>(GetProcAddress(module,"RollbackTransaction"));
        }
        open=reinterpret_cast<decltype(open)>(GetProcAddress(GetModuleHandleW(L"kernel32.dll"),"CreateFileTransactedW"));
    }
    bool supported()const{return create&&commit&&rollback&&open;}
};
static Api& api(){static Api value;return value;}
struct File {
    HANDLE value=INVALID_HANDLE_VALUE;
    bool close(){if(value==INVALID_HANDLE_VALUE)return true;HANDLE old=value;value=INVALID_HANDLE_VALUE;return CloseHandle(old)!=0;}
    ~File(){close();}
};
struct Transaction {
    HANDLE handle=INVALID_HANDLE_VALUE;jobject request=nullptr;jlong token=0;
    ULONGLONG expires=0;
    ~Transaction(){if(handle!=INVALID_HANDLE_VALUE){api().rollback(handle);CloseHandle(handle);}}
};
static SRWLOCK slots_lock=SRWLOCK_INIT;
static std::unique_ptr<Transaction> slots[2];
static std::atomic<int> admitted{0};
static std::atomic<jlong> next_token{1};
static jlongArray result(JNIEnv* env,jlong status,jlong token=0,jlong error=0) {
    if(env->ExceptionCheck())return nullptr;
    jlong values[3]={status,token,error};auto out=env->NewLongArray(3);
    if(out)env->SetLongArrayRegion(out,0,3,values);return out;
}
static std::unique_ptr<Transaction> take(JNIEnv* env,jobject owner,jlong token) {
    AcquireSRWLockExclusive(&slots_lock);std::unique_ptr<Transaction> found;
    for(auto& slot:slots)if(slot&&slot->token==token&&env->IsSameObject(slot->request,owner)){found=std::move(slot);break;}
    ReleaseSRWLockExclusive(&slots_lock);return found;
}
static void release(JNIEnv* env,std::unique_ptr<Transaction>& tx) {
    if(!tx)return;
    if(tx->request)env->DeleteGlobalRef(tx->request);
    tx.reset();admitted.fetch_sub(1);
}
static void reap_expired(JNIEnv* env) {
    std::unique_ptr<Transaction> expired[2];ULONGLONG now=GetTickCount64();
    AcquireSRWLockExclusive(&slots_lock);
    for(int i=0;i<2;i++)if(slots[i]&&now>=slots[i]->expires)expired[i]=std::move(slots[i]);
    ReleaseSRWLockExclusive(&slots_lock);
    // No Root permit is involved in rollback. Never keep the registry lock across OS I/O.
    for(auto& tx:expired)release(env,tx);
}
static bool exact(JNIEnv* env,jobject object,const char* name) {
    if(!object)return false;auto expected=env->FindClass(name);auto actual=env->GetObjectClass(object);
    bool ok=expected&&actual&&env->IsSameObject(expected,actual);
    if(expected)env->DeleteLocalRef(expected);if(actual)env->DeleteLocalRef(actual);return ok;
}
static bool string(JNIEnv* env,jobject object,const char* method,std::string& value) {
    auto type=env->GetObjectClass(object);auto id=env->GetMethodID(type,method,"()Ljava/lang/String;");env->DeleteLocalRef(type);
    if(!id)return false;auto text=static_cast<jstring>(env->CallObjectMethod(object,id));
    if(env->ExceptionCheck()||!text)return false;
    const char* bytes=env->GetStringUTFChars(text,nullptr);if(!bytes){env->DeleteLocalRef(text);return false;}
    value=bytes;env->ReleaseStringUTFChars(text,bytes);env->DeleteLocalRef(text);return true;
}
static bool path(JNIEnv* env,jobject request,std::wstring& value) {
    auto type=env->GetObjectClass(request);auto method=env->GetMethodID(type,"path","()Ljava/lang/String;");env->DeleteLocalRef(type);
    if(!method)return false;auto text=static_cast<jstring>(env->CallObjectMethod(request,method));
    if(env->ExceptionCheck()||!text)return false;jsize count=env->GetStringLength(text);
    if(count<3||count>4096){env->DeleteLocalRef(text);return false;}
    const jchar* chars=env->GetStringChars(text,nullptr);if(!chars){env->DeleteLocalRef(text);return false;}
    bool ok=true;value.resize(count);
    for(jsize i=0;i<count;i++){value[i]=chars[i]==L'/'?L'\\':chars[i];if(chars[i]==0||(chars[i]==':'&&i!=1))ok=false;}
    env->ReleaseStringChars(text,chars);env->DeleteLocalRef(text);
    return ok&&((value[0]>=L'A'&&value[0]<=L'Z')||(value[0]>=L'a'&&value[0]<=L'z'))&&value[1]==L':'&&value[2]==L'\\';
}
struct Hash {
    BCRYPT_ALG_HANDLE algorithm=nullptr;BCRYPT_HASH_HANDLE handle=nullptr;
    ~Hash(){if(handle)BCryptDestroyHash(handle);if(algorithm)BCryptCloseAlgorithmProvider(algorithm,0);}
    bool open(){return BCryptOpenAlgorithmProvider(&algorithm,BCRYPT_SHA256_ALGORITHM,nullptr,0)>=0
        &&BCryptCreateHash(algorithm,&handle,nullptr,0,nullptr,0,0)>=0;}
    bool add(const unsigned char* bytes,ULONG size){return BCryptHashData(handle,const_cast<PUCHAR>(bytes),size,0)>=0;}
    bool finish(std::string& out){unsigned char digest[32];if(BCryptFinishHash(handle,digest,32,0)<0)return false;
        static const char hex[]="0123456789abcdef";out.clear();for(auto b:digest){out+=hex[b>>4];out+=hex[b&15];}return true;}
};
static bool hash_file(HANDLE file,std::string& hash,ULONGLONG deadline) {
    LARGE_INTEGER zero{};if(!SetFilePointerEx(file,zero,nullptr,FILE_BEGIN))return false;
    Hash digest;if(!digest.open())return false;unsigned char buffer[16384];size_t total=0;
    for(;;){if(GetTickCount64()>=deadline)return false;DWORD count=0;if(!ReadFile(file,buffer,sizeof(buffer),&count,nullptr))return false;
        if(!count)break;total+=count;if(total>16*1024*1024||!digest.add(buffer,count))return false;}
    return digest.finish(hash);
}
static bool identity(HANDLE file,const std::wstring& requested,std::string& out) {
    BY_HANDLE_FILE_INFORMATION info{};FILE_ID_INFO id{};wchar_t fs[32],final_path[4104];
    if(GetFileType(file)!=FILE_TYPE_DISK||!GetFileInformationByHandle(file,&info)||info.nNumberOfLinks!=1
        ||(info.dwFileAttributes&(FILE_ATTRIBUTE_DIRECTORY|FILE_ATTRIBUTE_REPARSE_POINT|FILE_ATTRIBUTE_ENCRYPTED|FILE_ATTRIBUTE_SPARSE_FILE|FILE_ATTRIBUTE_COMPRESSED))
        ||!GetFileInformationByHandleEx(file,FileIdInfo,&id,sizeof(id))
        ||!GetVolumeInformationByHandleW(file,nullptr,0,nullptr,nullptr,nullptr,fs,32)||_wcsicmp(fs,L"NTFS")!=0)return false;
    DWORD length=GetFinalPathNameByHandleW(file,final_path,4104,FILE_NAME_NORMALIZED|VOLUME_NAME_DOS);
    if(!length||length>=4104||_wcsicmp(final_path,(L"\\\\?\\"+requested).c_str())!=0)return false;
    unsigned long long low=0,high=0;std::memcpy(&low,id.FileId.Identifier,8);std::memcpy(&high,id.FileId.Identifier+8,8);
    char text[80];std::snprintf(text,sizeof(text),"win32-file-id/1:%llx:%llx:%llx",static_cast<unsigned long long>(id.VolumeSerialNumber),low,high);
    out=text;return true;
}
}

// Result: status, opaque token, Win32 error. Token is also bound to the exact private-created request object.
extern "C" JNIEXPORT jlongArray JNICALL
Java_dev_ronova_coremod_nativebridge_NativeBridge_prepareStorageTransaction0(JNIEnv* env,jclass,jobject request) try {
    using namespace recovery_storage;
    if(!api().supported())return result(env,4);
    if(!exact(env,request,"dev/ronova/coremod/observation/RecoverySourceTracker$StorageCommitRequest"))return result(env,0);
    reap_expired(env);
    int previous=admitted.fetch_add(1);if(previous>=2){admitted.fetch_sub(1);return result(env,6,0,ERROR_BUSY);}
    bool retained=false;struct Admission{bool& retained;~Admission(){if(!retained)admitted.fetch_sub(1);}} admission{retained};
    std::wstring requested;std::string expected_id,baseline,expected;
    if(!path(env,request,requested)||!string(env,request,"fileIdentity",expected_id)
        ||!string(env,request,"baselineHash",baseline)||!string(env,request,"expectedHash",expected))return result(env,0);
    auto type=env->GetObjectClass(request);auto getter=env->GetMethodID(type,"encoded","()[B");env->DeleteLocalRef(type);
    if(!getter)return result(env,0);auto bytes=static_cast<jbyteArray>(env->CallObjectMethod(request,getter));
    if(env->ExceptionCheck()||!bytes)return result(env,0);jsize size=env->GetArrayLength(bytes);
    if(size<1||size>16*1024*1024){env->DeleteLocalRef(bytes);return result(env,0);}
    std::vector<unsigned char> image(size);env->GetByteArrayRegion(bytes,0,size,reinterpret_cast<jbyte*>(image.data()));env->DeleteLocalRef(bytes);
    if(env->ExceptionCheck())return nullptr;
    Hash encoded;std::string actual;if(!encoded.open()||!encoded.add(image.data(),size)||!encoded.finish(actual)||actual!=expected)return result(env,0);
    auto tx=std::make_unique<Transaction>();
    tx->expires=GetTickCount64()+5000;
    tx->handle=api().create(nullptr,nullptr,0,0,0,5000,const_cast<wchar_t*>(L"Ronova registered recovery storage"));
    if(tx->handle==INVALID_HANDLE_VALUE)return result(env,4,0,GetLastError());
    File file;file.value=api().open(requested.c_str(),GENERIC_READ|GENERIC_WRITE,0,nullptr,OPEN_EXISTING,FILE_FLAG_OPEN_REPARSE_POINT,nullptr,tx->handle,nullptr,nullptr);
    if(file.value==INVALID_HANDLE_VALUE)return result(env,6,0,GetLastError());
    ULONGLONG deadline=GetTickCount64()+4000;std::string file_id;
    if(!identity(file.value,requested,file_id)||file_id!=expected_id)return result(env,5);
    if(!hash_file(file.value,actual,deadline))return result(env,6,0,GetLastError());
    if(actual!=baseline)return result(env,5);
    LARGE_INTEGER zero{};if(!SetFilePointerEx(file.value,zero,nullptr,FILE_BEGIN))return result(env,6,0,GetLastError());
    for(size_t offset=0;offset<image.size();) {
        if(GetTickCount64()>=deadline)return result(env,6,0,WAIT_TIMEOUT);
        DWORD count=0,want=static_cast<DWORD>((image.size()-offset)>16384?16384:(image.size()-offset));
        if(!WriteFile(file.value,image.data()+offset,want,&count,nullptr)||count!=want)return result(env,6,0,GetLastError());offset+=count;
    }
    if(!SetEndOfFile(file.value)||!FlushFileBuffers(file.value)||!hash_file(file.value,actual,deadline))return result(env,6,0,GetLastError());
    if(actual!=expected||!identity(file.value,requested,file_id)||file_id!=expected_id)return result(env,5);
    // Windows requires transaction file handles to be closed before commit/rollback.
    if(!file.close())return result(env,6,0,GetLastError());
    tx->token=next_token.fetch_add(1);if(tx->token<=0||tx->token==std::numeric_limits<jlong>::max())return result(env,6);
    tx->request=env->NewGlobalRef(request);if(!tx->request)return nullptr;
    jlong token=tx->token;auto response=result(env,1,token);if(!response){env->DeleteGlobalRef(tx->request);return nullptr;}
    AcquireSRWLockExclusive(&slots_lock);
    for(auto& slot:slots)if(!slot){slot=std::move(tx);retained=true;break;}
    ReleaseSRWLockExclusive(&slots_lock);
    if(!retained){env->DeleteGlobalRef(tx->request);return result(env,6,0,ERROR_BUSY);}return response;
} catch(...) {
    if(!env->ExceptionCheck()){auto error=env->FindClass("java/lang/IllegalStateException");if(error){env->ThrowNew(error,"Native transaction preparation unavailable");env->DeleteLocalRef(error);}}
    return nullptr;
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_dev_ronova_coremod_nativebridge_NativeBridge_finishStorageTransaction0(JNIEnv* env,jclass,jobject request,jlong token,jobject permit) {
    using namespace recovery_storage;
    auto tx=take(env,request,token);if(!tx)return result(env,0);
    bool allowed=false;
    if(permit&&exact(env,permit,"dev/ronova/coremod/observation/RecoverySourceTracker$StoragePreparationResult$StorageWritePermit")) {
        auto type=env->GetObjectClass(permit);auto valid=env->GetMethodID(type,"beginCommitFor","(Ldev/ronova/coremod/observation/RecoverySourceTracker$StorageCommitRequest;)Z");env->DeleteLocalRef(type);
        if(valid)allowed=env->CallBooleanMethod(permit,valid,request)==JNI_TRUE&&!env->ExceptionCheck();
    }
    jlong status=0,error=0;
    if(allowed) {if(api().commit(tx->handle))status=1;else{status=3;error=GetLastError();}}
    else {if(api().rollback(tx->handle))status=2;else{status=3;error=GetLastError();}}
    CloseHandle(tx->handle);tx->handle=INVALID_HANDLE_VALUE;release(env,tx);return result(env,status,0,error);
}
