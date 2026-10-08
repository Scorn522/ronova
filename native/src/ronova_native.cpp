#include <jni.h>
#include <jvmti.h>
#include <windows.h>

#include <atomic>
#include <cstdint>
#include <cstring>
#include <cstdio>
#include <new>
#include <vector>
#include <string>
#include "storage_transaction.hpp"
#if __has_include("registered_code_images.hpp")
#include "registered_code_images.hpp"
#endif

namespace {
JavaVM* g_vm = nullptr;
jvmtiEnv* g_jvmti = nullptr;
jclass g_transform_bridge = nullptr;
jmethodID g_transform_method = nullptr;
jmethodID g_verify_method = nullptr;
jobject g_root_loader = nullptr;
std::atomic<bool> g_code_seals_enabled{false};
std::atomic<std::int64_t> g_code_replacements{0};
std::atomic<std::uint64_t> g_code_challenge_generation{0};
std::atomic<std::uint64_t> g_code_challenge_attempts{0};
std::atomic<std::uint64_t> g_code_challenge_successes{0};
std::atomic<std::uint64_t> g_code_challenge_failures{0};
void invalidate_code_semantics();
void request_integrity_refresh();
std::atomic<bool> g_task_bridge_ready{false};
constexpr const char* TASK_TARGETS[] = {
    "java/util/concurrent/FutureTask", "java/util/concurrent/ScheduledThreadPoolExecutor$ScheduledFutureTask",
    "java/util/concurrent/ForkJoinTask", "java/util/concurrent/ThreadPoolExecutor",
    "java/util/concurrent/ScheduledThreadPoolExecutor", "java/util/concurrent/ForkJoinPool",
    "java/util/concurrent/ThreadPoolExecutor$CallerRunsPolicy"
};
LONG task_bit(const char* name) {
    if(name==nullptr)return 0;
    for(unsigned i=0;i<sizeof(TASK_TARGETS)/sizeof(TASK_TARGETS[0]);++i)if(std::strcmp(name,TASK_TARGETS[i])==0)return 1L<<i;
    return 0;
}
SRWLOCK g_task_lock=SRWLOCK_INIT;
struct TaskGuard {TaskGuard(){AcquireSRWLockExclusive(&g_task_lock);}~TaskGuard(){ReleaseSRWLockExclusive(&g_task_lock);}};
bool g_task_cycle=false;
LONG g_task_verified=0,g_task_cycle_verified=0,g_task_cycle_seen=0,g_task_cycle_failed=0;
std::uint64_t g_task_generation=0,g_task_changes=0,g_task_callbacks=0;
thread_local bool g_task_retransform=false;
jclass g_task_hooks=nullptr;
struct NativeTaskFrame {jweak receiver=nullptr;LONG boundary_bit=0;jlong token=0;std::uint64_t changes=0;};
thread_local NativeTaskFrame g_task_frames[128];
thread_local unsigned g_task_depth=0;
thread_local jlong g_task_serial=0;
LONG execution_task_bit(JNIEnv* env,jmethodID method) {
    jclass owner=nullptr;char* signature=nullptr;char* name=nullptr;char* descriptor=nullptr;LONG bit=0;
    if(g_jvmti->GetMethodDeclaringClass(method,&owner)!=JVMTI_ERROR_NONE||owner==nullptr)return 0;
    if(g_jvmti->GetClassSignature(owner,&signature,nullptr)==JVMTI_ERROR_NONE&&signature!=nullptr
        &&g_jvmti->GetMethodName(method,&name,&descriptor,nullptr)==JVMTI_ERROR_NONE&&name!=nullptr&&descriptor!=nullptr) {
        if(std::strcmp(signature,"Ljava/util/concurrent/FutureTask;")==0
            &&((std::strcmp(name,"run")==0&&std::strcmp(descriptor,"()V")==0)||(std::strcmp(name,"runAndReset")==0&&std::strcmp(descriptor,"()Z")==0)))bit=1;
        if(std::strcmp(signature,"Ljava/util/concurrent/ScheduledThreadPoolExecutor$ScheduledFutureTask;")==0&&std::strcmp(name,"run")==0&&std::strcmp(descriptor,"()V")==0)bit=2;
        if(std::strcmp(signature,"Ljava/util/concurrent/ForkJoinTask;")==0&&std::strcmp(name,"doExec")==0&&std::strcmp(descriptor,"()I")==0)bit=4;
        if(std::strcmp(signature,"Ljava/util/concurrent/ThreadPoolExecutor;")==0&&std::strcmp(name,"runWorker")==0&&std::strcmp(descriptor,"(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V")==0)bit=8;
        if(std::strcmp(signature,"Ljava/util/concurrent/ThreadPoolExecutor$CallerRunsPolicy;")==0&&std::strcmp(name,"rejectedExecution")==0&&std::strcmp(descriptor,"(Ljava/lang/Runnable;Ljava/util/concurrent/ThreadPoolExecutor;)V")==0)bit=64;
        jobject loader=nullptr;if(g_jvmti->GetClassLoader(owner,&loader)!=JVMTI_ERROR_NONE||loader!=nullptr)bit=0;
        if(loader!=nullptr)env->DeleteLocalRef(loader);
    }
    if(signature!=nullptr)g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
    if(name!=nullptr)g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(name));
    if(descriptor!=nullptr)g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(descriptor));
    env->DeleteLocalRef(owner);return bit;
}
bool exact_hook_frame(JNIEnv* env,jmethodID method,const char* expected) {
    jclass owner=nullptr;char* name=nullptr;bool matched=false;
    if(g_jvmti->GetMethodDeclaringClass(method,&owner)==JVMTI_ERROR_NONE&&owner!=nullptr
        &&env->IsSameObject(owner,g_task_hooks)&&g_jvmti->GetMethodName(method,&name,nullptr,nullptr)==JVMTI_ERROR_NONE)
        matched=name!=nullptr&&std::strcmp(name,expected)==0;
    if(name!=nullptr)g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(name));if(owner!=nullptr)env->DeleteLocalRef(owner);return matched;
}
jmethodID task_hook_boundary(JNIEnv* env,bool entering) {
    jthread thread=nullptr;if(g_jvmti==nullptr||g_task_hooks==nullptr||g_jvmti->GetCurrentThread(&thread)!=JVMTI_ERROR_NONE)return nullptr;
    jvmtiFrameInfo frames[4];jint count=0;jmethodID result=nullptr;
    if(g_jvmti->GetStackTrace(thread,0,4,frames,&count)==JVMTI_ERROR_NONE&&count==4
        &&exact_hook_frame(env,frames[0].method,entering?"nativeEnter":"nativeExit")
        &&exact_hook_frame(env,frames[1].method,entering?"enterFrame":"exitFrame")
        &&exact_hook_frame(env,frames[2].method,entering?"enter":"exit")
        &&(!entering||frames[3].location==1||execution_task_bit(env,frames[3].method)==8||execution_task_bit(env,frames[3].method)==64)
        &&execution_task_bit(env,frames[3].method)!=0)result=frames[3].method;
    if(thread!=nullptr)env->DeleteLocalRef(thread);return result;
}
jlong JNICALL native_task_enter(JNIEnv* env,jclass,jobject receiver) {
    if(receiver==nullptr||g_task_depth>=128||g_task_serial==INT64_MAX)return 0;
    jmethodID method=task_hook_boundary(env,true);if(method==nullptr)return 0;
    LONG bit=execution_task_bit(env,method);
    std::uint64_t changes;
    {TaskGuard guard;if(g_task_cycle||(g_task_verified&bit)!=bit)return 0;changes=g_task_changes;}
    jweak weak=env->NewWeakGlobalRef(receiver);if(weak==nullptr)return 0;
    jlong token=++g_task_serial;g_task_frames[g_task_depth++]={weak,bit,token,changes};return token;
}
void JNICALL native_task_exit(JNIEnv* env,jclass,jlong token) {
    if(token==0||g_task_depth==0)return;
    auto& frame=g_task_frames[g_task_depth-1];
    jmethodID boundary=task_hook_boundary(env,false);
    // Retransformation can turn an active frame's jmethodID into an obsolete id.
    // The paired token and certified boundary role remain stable across that change.
    if(frame.token!=token||boundary==nullptr||execution_task_bit(env,boundary)!=frame.boundary_bit)return;
    env->DeleteWeakGlobalRef(frame.receiver);frame={};--g_task_depth;
}
void JNICALL task_thread_end(jvmtiEnv*,JNIEnv* env,jthread) {
    while(g_task_depth>0){auto& frame=g_task_frames[--g_task_depth];env->DeleteWeakGlobalRef(frame.receiver);frame={};}
}
struct TaskCancelContext {
    jobject task=nullptr,lease=nullptr,plan=nullptr,state_handle=nullptr;
    jmethodID valid=nullptr,close=nullptr;
    LONG required=0;std::uint64_t changes=0;
    bool checked=false,allowed=false,committed=false,uncertain=false;
};
thread_local TaskCancelContext* g_task_cancel=nullptr;
bool cancel_hook_boundary(JNIEnv* env,const char* native_name) {
    jthread thread=nullptr;jvmtiFrameInfo frames[3];jint count=0;bool matched=false;
    if(g_jvmti->GetCurrentThread(&thread)!=JVMTI_ERROR_NONE||thread==nullptr)return false;
    if(g_jvmti->GetStackTrace(thread,0,3,frames,&count)==JVMTI_ERROR_NONE&&count==3
        &&exact_hook_frame(env,frames[0].method,native_name)&&exact_hook_frame(env,frames[1].method,"cancelState")) {
        jclass owner=nullptr,expected=env->FindClass("java/util/concurrent/FutureTask");char* name=nullptr;char* signature=nullptr;
        if(expected!=nullptr&&g_jvmti->GetMethodDeclaringClass(frames[2].method,&owner)==JVMTI_ERROR_NONE&&owner!=nullptr
            &&env->IsSameObject(owner,expected)&&g_jvmti->GetMethodName(frames[2].method,&name,&signature,nullptr)==JVMTI_ERROR_NONE)
            matched=name!=nullptr&&signature!=nullptr&&std::strcmp(name,"cancel")==0&&std::strcmp(signature,"(Z)Z")==0;
        if(owner!=nullptr)env->DeleteLocalRef(owner);if(expected!=nullptr)env->DeleteLocalRef(expected);
        if(name!=nullptr)g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(name));if(signature!=nullptr)g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
    }
    env->DeleteLocalRef(thread);if(env->ExceptionCheck()){env->ExceptionClear();return false;}return matched;
}
jboolean JNICALL native_cancel_permit(JNIEnv* env,jclass,jobject handle,jobject task,jint expected,jint update) {
    auto* context=g_task_cancel;if(context==nullptr)return JNI_TRUE;
    context->checked=true;
    if(expected!=0||update!=4||!env->IsSameObject(task,context->task)||!env->IsSameObject(handle,context->state_handle)
        ||!cancel_hook_boundary(env,"nativeCancelPermit"))return JNI_FALSE;
    if(env->CallBooleanMethod(context->lease,context->valid,context->plan)!=JNI_TRUE||env->ExceptionCheck()) {
        if(env->ExceptionCheck()){env->ExceptionClear();context->uncertain=true;}return JNI_FALSE;
    }
    {TaskGuard guard;if(g_task_cycle||g_task_changes!=context->changes||(g_task_verified&context->required)!=context->required)return JNI_FALSE;}
    context->allowed=true;return JNI_TRUE;
}
void JNICALL native_cancel_committed(JNIEnv* env,jclass,jobject task,jboolean linearized) {
    auto* context=g_task_cancel;if(context==nullptr)return;
    if(!context->allowed||!env->IsSameObject(task,context->task)||!cancel_hook_boundary(env,"nativeCancelCommitted")){context->uncertain=true;return;}
    context->committed=true;
    // The FutureTask state CAS is the cancellation linearization point. Java reports whether
    // ENTER/SUBMIT publication shared the same per-task gate; without that guarantee the CAS
    // still happened, but queue settlement is effect-unknown and must not mint NOT_EXECUTED.
    if(linearized!=JNI_TRUE)context->uncertain=true;
    // Linearization precedes waiter cleanup. No shared pool lock or user callback
    // is allowed to keep this Root's qualification fence after the state CAS.
    env->CallVoidMethod(context->lease,context->close);
    if(env->ExceptionCheck()){env->ExceptionClear();context->uncertain=true;}
}
void reassert_task_targets(JNIEnv* env);
#ifdef RONOVA_CODE_SEALS
SRWLOCK g_code_state_lock = SRWLOCK_INIT;
struct CodeStateGuard {
    CodeStateGuard() { AcquireSRWLockExclusive(&g_code_state_lock); }
    ~CodeStateGuard() { ReleaseSRWLockExclusive(&g_code_state_lock); }
};
bool g_code_cycle_active = false;
std::uint64_t g_code_outside_changes = 0;
struct CodeCycleEntry { RegisteredCodeImage* image; unsigned callbacks=0; bool proposed=false; };
thread_local std::vector<CodeCycleEntry>* g_code_cycle_entries = nullptr;
CodeCycleEntry* code_cycle_entry(RegisteredCodeImage* image) {
    if(g_code_cycle_entries!=nullptr)for(auto& entry:*g_code_cycle_entries)if(entry.image==image)return &entry;
    return nullptr;
}
RegisteredCodeImage* code_image(const char* name) {
    if (name == nullptr) return nullptr;
    for (auto& image : CODE_IMAGES) if (std::strcmp(name,image.name)==0) return &image;
    return nullptr;
}
RegisteredCodeImage* code_image_signature(const char* signature) {
    if(signature==nullptr||signature[0]!='L')return nullptr;
    for(auto& image:CODE_IMAGES) {
        const auto size=std::strlen(image.name);
        if(std::strlen(signature)==size+2&&std::strncmp(signature+1,image.name,size)==0&&signature[size+1]==';')return &image;
    }
    return nullptr;
}
bool root_loader(JNIEnv* env,jobject loader){return g_root_loader!=nullptr&&env->IsSameObject(g_root_loader,loader);}
bool registered_code_loader(JNIEnv* env, jobject loader, RegisteredCodeImage* image) {
    if (image == nullptr) return false;
    // Only the JDK-only bridge is expected in the bootstrap loader.
    return (std::strcmp(image->name, "dev/ronova/coremod/agent/BootstrapHooks") == 0
        ||std::strcmp(image->name,"dev/ronova/coremod/agent/BootstrapTaskHooks")==0
        ||std::strncmp(image->name,"dev/ronova/coremod/agent/BootstrapTaskHooks$",sizeof("dev/ronova/coremod/agent/BootstrapTaskHooks$")-1)==0)
        ? loader == nullptr : root_loader(env, loader);
}
void reassert_registered_code(JNIEnv* env) {
    if(!g_code_seals_enabled.load()||env==nullptr||g_jvmti==nullptr)return;
    std::uint64_t outside_before;
    {
        CodeStateGuard guard;
        if(g_code_cycle_active)return;
        // Expire paired Java observations before the inventory/verification state changes,
        // including cycles that fail before any ClassFileLoadHook is delivered.
        invalidate_code_semantics();
        g_code_cycle_active=true;
        outside_before=g_code_outside_changes;
        for(auto& image:CODE_IMAGES)image.verified.store(false);
    }
    jint count=0;jclass* classes=nullptr;
    bool complete=g_jvmti->GetLoadedClasses(&count,&classes)==JVMTI_ERROR_NONE;
    std::vector<RegisteredCodeImage*> confirmed;
    std::vector<jclass> targets;
    std::vector<CodeCycleEntry> entries;
    unsigned reported_failures=0;
    if(complete)for(jint i=0;i<count;++i) {
        char* signature=nullptr;jobject loader=nullptr;
        if(g_jvmti->GetClassSignature(classes[i],&signature,nullptr)!=JVMTI_ERROR_NONE)complete=false;
        else if(auto* image=code_image_signature(signature);image!=nullptr) {
            if(g_jvmti->GetClassLoader(classes[i],&loader)!=JVMTI_ERROR_NONE)complete=false;
            else if(registered_code_loader(env,loader,image)) {
                { CodeStateGuard guard; image->loaded.store(true); }
                jboolean modifiable=JNI_FALSE;
                if(g_jvmti->IsModifiableClass(classes[i],&modifiable)!=JVMTI_ERROR_NONE||modifiable!=JNI_TRUE)complete=false;
                else {
                    targets.push_back(classes[i]);entries.push_back({image});
                }
            }
        }
        if(signature!=nullptr)g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
        if(loader!=nullptr)env->DeleteLocalRef(loader);
    }
    if(complete&&!targets.empty()) {
        // One VM transition for the full inventory; no verified image is published during the batch.
        g_code_cycle_entries=&entries;
        const auto error=g_jvmti->RetransformClasses(static_cast<jint>(targets.size()),targets.data());
        g_code_cycle_entries=nullptr;
        for(auto& entry:entries) {
            const bool accepted=error==JVMTI_ERROR_NONE&&entry.callbacks==1&&entry.proposed;
            if(accepted)confirmed.push_back(entry.image);else {
                complete=false;
                if(reported_failures++<4)std::fprintf(stderr,"RONOVA_CODE_SEAL_REJECT class=%s jvmti=%d callbacks=%u proposed=%d\n",
                    entry.image->name,static_cast<int>(error),entry.callbacks,entry.proposed?1:0);
            }
        }
    }
    if(classes!=nullptr) {
        for(jint i=0;i<count;++i)env->DeleteLocalRef(classes[i]);
        g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(classes));
    }
    {
        CodeStateGuard guard;
        // A global proposed flag is never a completion receipt for another thread's transform.
        if(complete&&outside_before==g_code_outside_changes)
            for(auto* image:confirmed)image->verified.store(true);
        else if(complete)std::fprintf(stderr,"RONOVA_CODE_SEAL_RETRY outside_changes=%llu\n",
            static_cast<unsigned long long>(g_code_outside_changes-outside_before));
        g_code_cycle_active=false;
    }
}
void JNICALL code_class_prepared(jvmtiEnv* jvmti,JNIEnv* env,jthread,jclass type) {
    if(!g_code_seals_enabled.load())return;
    char* signature=nullptr;jobject loader=nullptr;
    if(jvmti->GetClassSignature(type,&signature,nullptr)!=JVMTI_ERROR_NONE)return;
    auto* image=code_image_signature(signature);
    if(image!=nullptr&&jvmti->GetClassLoader(type,&loader)==JVMTI_ERROR_NONE&&registered_code_loader(env,loader,image)) {
        invalidate_code_semantics();
        CodeStateGuard guard;
        ++g_code_outside_changes;image->loaded.store(true);image->verified.store(false);
        // ClassPrepare does not attest bytes returned by later transformers.
        // A complete registered-code cycle must observe the now prepared class.
        request_integrity_refresh();
    }
    if(loader!=nullptr)env->DeleteLocalRef(loader);
    if(signature!=nullptr)jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
}
#endif
volatile LONG g_watchdog_running = 0;
volatile LONG g_watchdog_interval = 30000;
struct WatchdogRun {
    HANDLE wake=nullptr;
    HANDLE thread=nullptr;
    std::atomic<bool> stop{false};
};
SRWLOCK g_watchdog_lifecycle=SRWLOCK_INIT;
WatchdogRun* g_watchdog_run=nullptr;
std::atomic<bool> g_refresh_pending{false};
std::atomic<bool> g_integrity_shutdown{false};
void request_integrity_refresh() {
    g_refresh_pending.store(true,std::memory_order_release);
    AcquireSRWLockShared(&g_watchdog_lifecycle);
    if(g_watchdog_run!=nullptr&&!g_watchdog_run->stop.load())SetEvent(g_watchdog_run->wake);
    ReleaseSRWLockShared(&g_watchdog_lifecycle);
}
thread_local bool g_in_transform = false;

constexpr jint CAP_JVMTI = 0x1;
constexpr jint CAP_HOOK = 0x2;
constexpr jint CAP_RETRANSFORM = 0x4;
constexpr jint CAP_SEMANTIC_LEASE = 0x40;

constexpr LONG TARGET_ENTITY = 0x01;
constexpr LONG TARGET_LIVING = 0x02;
constexpr LONG TARGET_LEVEL = 0x04;
constexpr LONG TARGET_SERVER_LEVEL = 0x08;
constexpr LONG TARGET_CLIENT_LEVEL = 0x10;

struct SemanticTarget {
    const char* name;
    const char* signature;
    LONG bit;
};
#define RONOVA_SEMANTIC_TARGET(name, bit) {name, "L" name ";", bit}
constexpr SemanticTarget SEMANTIC_TARGETS[] = {
    RONOVA_SEMANTIC_TARGET("net/minecraft/world/entity/Entity", TARGET_ENTITY),
    RONOVA_SEMANTIC_TARGET("net/minecraft/world/entity/LivingEntity", TARGET_LIVING),
    RONOVA_SEMANTIC_TARGET("net/minecraft/world/level/Level", TARGET_LEVEL),
    RONOVA_SEMANTIC_TARGET("net/minecraft/server/level/ServerLevel", TARGET_SERVER_LEVEL),
    RONOVA_SEMANTIC_TARGET("net/minecraft/client/multiplayer/ClientLevel", TARGET_CLIENT_LEVEL),
    RONOVA_SEMANTIC_TARGET("net/minecraft/network/syncher/SynchedEntityData", 0x20),
    RONOVA_SEMANTIC_TARGET("net/minecraft/network/syncher/SynchedEntityData$DataItem", 0x40),
    RONOVA_SEMANTIC_TARGET("net/minecraft/world/level/entity/EntityLookup", 0x80),
    RONOVA_SEMANTIC_TARGET("net/minecraft/world/level/entity/EntityTickList", 0x100),
    RONOVA_SEMANTIC_TARGET("net/minecraft/world/level/entity/EntitySectionStorage", 0x200),
    RONOVA_SEMANTIC_TARGET("net/minecraft/world/level/entity/EntitySection", 0x400),
    RONOVA_SEMANTIC_TARGET("net/minecraft/util/ClassInstanceMultiMap", 0x800),
    RONOVA_SEMANTIC_TARGET("net/minecraft/world/level/entity/PersistentEntitySectionManager", 0x1000),
    RONOVA_SEMANTIC_TARGET("net/minecraft/nbt/CompoundTag", 0x2000),
    RONOVA_SEMANTIC_TARGET("net/minecraft/world/entity/EntityType", 0x4000),
    RONOVA_SEMANTIC_TARGET("net/minecraft/network/syncher/EntityDataAccessor", 0x8000)
};
#undef RONOVA_SEMANTIC_TARGET

std::atomic<LONG> g_semantic_required_mask{0};
std::atomic<LONG> g_semantic_verified_mask{0};
std::atomic<LONG> g_semantic_cycle_mask{0};
std::atomic<LONG> g_semantic_cycle_failed_mask{0};
std::atomic<bool> g_semantic_cycle_active{false};
SRWLOCK g_semantic_state_lock = SRWLOCK_INIT;
struct SemanticStateGuard {
    SemanticStateGuard() { AcquireSRWLockExclusive(&g_semantic_state_lock); }
    ~SemanticStateGuard() { ReleaseSRWLockExclusive(&g_semantic_state_lock); }
};
thread_local bool g_core_cycle_retransform = false;
std::uint64_t g_semantic_outside_changes = 0;
std::size_t g_semantic_cycle_callbacks = 0;
std::atomic<std::int64_t> g_semantic_generation{0};
std::atomic<std::int64_t> g_successful_leases{0};
std::atomic<std::int64_t> g_failed_leases{0};
std::atomic<std::int64_t> g_semantic_transformations{0};
std::atomic<std::int64_t> g_semantic_failures{0};
std::atomic<std::int64_t> g_semantic_reassertions{0};
std::atomic<std::int64_t> g_consecutive_lease_failures{0};
std::atomic<std::uint64_t> g_last_success_tick{0};

void invalidate_code_semantics() {
    SemanticStateGuard guard;
    ++g_semantic_outside_changes;
    g_semantic_transformations.fetch_add(1, std::memory_order_relaxed);
    g_last_success_tick.store(0, std::memory_order_release);
}


LONG target_bit(const char* name) {
    if (name == nullptr) return 0;
    for (const auto& target : SEMANTIC_TARGETS)
        if (std::strcmp(name, target.name) == 0) return target.bit;
    return 0;
}

LONG target_bit_from_signature(const char* signature) {
    if (signature == nullptr || signature[0] != 'L') return 0;
    for (const auto& target : SEMANTIC_TARGETS)
        if (std::strcmp(signature, target.signature) == 0) return target.bit;
    return 0;
}

bool is_core_target(const char* name) {
    return target_bit(name) != 0;
}

bool is_loaded_candidate_signature(const char* signature, bool include_external) {
    if (signature == nullptr) return false;
    if(g_task_bridge_ready.load()&&signature[0]=='L')for(const auto* target:TASK_TARGETS) {
        size_t n=std::strlen(target);if(std::strlen(signature)==n+2&&std::strncmp(signature+1,target,n)==0&&signature[n+1]==';')return true;
    }
    const bool core = target_bit_from_signature(signature) != 0;
    if (core || !include_external || signature[0] != 'L') return core;
    if (std::strncmp(signature, "Ljava/", 6) == 0 ||
        std::strncmp(signature, "Ljdk/", 5) == 0 ||
        std::strncmp(signature, "Lsun/", 5) == 0 ||
        std::strncmp(signature, "Ldev/ronova/coremod/", 20) == 0) {
        return false;
    }
    // The bytecode prefilter still makes the final decision. Names are used
    // only to give already-loaded lifecycle, query and client-control helpers
    // one eager pass.
    return std::strstr(signature, "Entity") != nullptr ||
        std::strstr(signature, "Renderer") != nullptr ||
        std::strstr(signature, "Client") != nullptr ||
        std::strstr(signature, "Screen") != nullptr ||
        std::strstr(signature, "Input") != nullptr ||
        std::strstr(signature, "Mouse") != nullptr ||
        std::strstr(signature, "Keyboard") != nullptr ||
        std::strstr(signature, "Attack") != nullptr ||
        std::strstr(signature, "Combat") != nullptr ||
        std::strstr(signature, "Execution") != nullptr ||
        std::strstr(signature, "Lifecycle") != nullptr ||
        std::strstr(signature, "Control") != nullptr ||
        std::strstr(signature, "Guard") != nullptr ||
        std::strstr(signature, "Hook") != nullptr ||
        std::strstr(signature, "Mixin") != nullptr;
}

bool contains_bytes(const unsigned char* data, jint length, const char* needle) {
    if (data == nullptr || needle == nullptr) return false;
    const size_t needle_length = std::strlen(needle);
    if (needle_length == 0 || length < static_cast<jint>(needle_length)) return false;
    for (jint start = 0; start <= length - static_cast<jint>(needle_length); ++start) {
        if (std::memcmp(data + start, needle, needle_length) == 0) return true;
    }
    return false;
}

bool has_semantic_proof(JNIEnv* env, jstring name, jbyteArray bytes, jobject loader) {
    if (g_verify_method == nullptr || name == nullptr || bytes == nullptr) return false;
    const jboolean verified = env->CallStaticBooleanMethod(g_transform_bridge, g_verify_method, name, bytes, loader);
    if (env->ExceptionCheck()) { env->ExceptionClear(); return false; }
    return verified == JNI_TRUE;
}

void record_semantic_proof(
    const char* name, bool verified, bool task_owned=false
) {
    const LONG task=task_bit(name);
    if(task!=0) {
        TaskGuard guard;
        if(task_owned&&g_task_cycle) {
            ++g_task_callbacks;
            if(verified&&(g_task_cycle_seen&task)==0)g_task_cycle_verified|=task;else g_task_cycle_failed|=task;
            g_task_cycle_seen|=task;
        } else {++g_task_changes;g_task_verified&=~task;request_integrity_refresh();}
        return;
    }
    const LONG bit = target_bit(name);
    if (bit == 0) return;
    SemanticStateGuard guard;
    g_semantic_required_mask.fetch_or(bit, std::memory_order_relaxed);
    g_semantic_transformations.fetch_add(1, std::memory_order_relaxed);
    if (verified) {
        g_semantic_verified_mask.fetch_or(bit, std::memory_order_release);
        if (g_core_cycle_retransform && g_semantic_cycle_active.load(std::memory_order_acquire)) {
            ++g_semantic_cycle_callbacks;
            g_semantic_cycle_mask.fetch_or(bit, std::memory_order_release);
        }
    } else {
        // A successful different loader copy cannot renew this failed lease.
        // Only a complete all-copy cycle may issue the next timestamp.
        g_last_success_tick.store(0, std::memory_order_release);
        g_semantic_failures.fetch_add(1, std::memory_order_relaxed);
        g_semantic_verified_mask.fetch_and(~bit, std::memory_order_release);
        if (g_semantic_cycle_active.load(std::memory_order_acquire)) {
            g_semantic_cycle_failed_mask.fetch_or(bit, std::memory_order_release);
        }
    }
}

bool is_candidate(const char* name, const unsigned char* data, jint length) {
    if(task_bit(name)!=0)return g_task_bridge_ready.load();
    if (is_core_target(name)) return true;
    if (name == nullptr || std::strncmp(name, "java/", 5) == 0 ||
        std::strncmp(name, "jdk/", 4) == 0 || std::strncmp(name, "sun/", 4) == 0) {
        return false;
    }
    const bool external = std::strncmp(name, "net/minecraft/", 14) != 0 &&
        std::strncmp(name, "dev/ronova/coremod/", 19) != 0;
    const bool entity_reference = external && (
        contains_bytes(data, length, "net/minecraft/world/entity/") ||
        contains_bytes(data, length, "net/minecraft/world/level/entity/EntityAccess") ||
        (contains_bytes(data, length, "/entity/") && contains_bytes(data, length, "Entity")));
    const bool entity_renderer = external &&
        contains_bytes(data, length, "net/minecraft/client/renderer/entity/") &&
        (contains_bytes(data, length, "render") || contains_bytes(data, length, "m_7392_"));
    const bool client_control = external && (
        contains_bytes(data, length, "net/minecraft/client/Minecraft") ||
        contains_bytes(data, length, "net/minecraft/client/KeyMapping") ||
        contains_bytes(data, length, "net/minecraft/client/MouseHandler") ||
        contains_bytes(data, length, "net/minecraft/client/KeyboardHandler") ||
        contains_bytes(data, length, "net/minecraft/client/gui/screens/Screen") ||
        contains_bytes(data, length, "net/minecraft/client/gui/screens/Overlay"));
    return entity_reference || entity_renderer || client_control ||
        contains_bytes(data, length, "f_146795_") ||
        contains_bytes(data, length, "isAddedToWorld") ||
        contains_bytes(data, length, "f_20890_") ||
        contains_bytes(data, length, "f_20919_") ||
        contains_bytes(data, length, "removalReason") ||
        contains_bytes(data, length, "deathTime");
}

void JNICALL class_file_load_hook(
    jvmtiEnv* jvmti, JNIEnv* env, jclass redefining, jobject loader, const char* name, jobject,
    jint class_data_len, const unsigned char* class_data, jint* new_class_data_len,
    unsigned char** new_class_data
) {
    const bool task_owned=task_bit(name)!=0&&g_task_retransform&&redefining!=nullptr;
    if(task_bit(name)!=0&&!task_owned) {TaskGuard guard;++g_task_changes;g_task_verified&=~task_bit(name);}
#ifdef RONOVA_CODE_SEALS
    if(g_code_seals_enabled.load())if(auto* image=code_image(name); registered_code_loader(env,loader,image)) {
        invalidate_code_semantics();
        auto* cycle_entry=redefining!=nullptr?code_cycle_entry(image):nullptr;
        const bool own_cycle=cycle_entry!=nullptr;
        {
            CodeStateGuard guard;
            if(!own_cycle)++g_code_outside_changes;
            image->proposed.store(false);image->verified.store(false);
        }
        bool proposed=false;
        if(class_data!=nullptr&&class_data_len==image->length&&std::memcmp(class_data,image->data,image->length)==0)proposed=true;
        else {
            unsigned char* replacement=nullptr;
            if(jvmti->Allocate(image->length,&replacement)==JVMTI_ERROR_NONE) {
                std::memcpy(replacement,image->data,image->length);*new_class_data_len=image->length;*new_class_data=replacement;
                proposed=true;g_code_replacements.fetch_add(1);
            }
        }
        if(own_cycle){++cycle_entry->callbacks;cycle_entry->proposed=proposed;}
        else request_integrity_refresh();
        // Hook output is a proposal, not a completed retransformation receipt.
        return;
    }
#endif
    // Invalidate before transformation starts, including a newly defining loader copy.
    if (is_core_target(name) && (!g_core_cycle_retransform || redefining == nullptr)) {
        SemanticStateGuard guard;
        ++g_semantic_outside_changes;
        g_last_success_tick.store(0, std::memory_order_release);
        g_semantic_required_mask.fetch_or(target_bit(name), std::memory_order_relaxed);
        request_integrity_refresh();
    }
    if (class_data_len < 0 || class_data_len > 8 * 1024 * 1024) {
        record_semantic_proof(name, false,task_owned); return;
    }
    if (!is_candidate(name, class_data, class_data_len)) return;
    if (g_transform_bridge == nullptr || g_transform_method == nullptr || g_in_transform) {
        record_semantic_proof(name, false,task_owned); return;
    }
    g_in_transform = true;
    bool verified = false;
    jstring class_name = env->NewStringUTF(name);
    jbyteArray input = env->NewByteArray(class_data_len);
    if (class_name != nullptr && input != nullptr) {
        env->SetByteArrayRegion(input, 0, class_data_len, reinterpret_cast<const jbyte*>(class_data));
        auto output = static_cast<jbyteArray>(env->CallStaticObjectMethod(g_transform_bridge, g_transform_method, class_name, input));
        if (env->ExceptionCheck()) env->ExceptionClear();
        else if (output != nullptr) {
            jsize length = env->GetArrayLength(output);
            unsigned char* allocated = nullptr;
            if (length > 0 && length <= 8 * 1024 * 1024 && jvmti->Allocate(length, &allocated) == JVMTI_ERROR_NONE) {
                env->GetByteArrayRegion(output, 0, length, reinterpret_cast<jbyte*>(allocated));
                if (env->ExceptionCheck()) { env->ExceptionClear(); jvmti->Deallocate(allocated); }
                else {
                    verified = has_semantic_proof(env, class_name, output, loader);
                    *new_class_data_len = length; *new_class_data = allocated;
                }
            }
        } else verified = has_semantic_proof(env, class_name, input, loader);
        if (output != nullptr) env->DeleteLocalRef(output);
    }
    if (env->ExceptionCheck()) env->ExceptionClear();
    if (input != nullptr) env->DeleteLocalRef(input);
    if (class_name != nullptr) env->DeleteLocalRef(class_name);
    record_semantic_proof(name, verified,task_owned);
    g_in_transform = false;
}

void reassert_task_targets(JNIEnv* env) {
    if(!g_task_bridge_ready.load()||env==nullptr||g_jvmti==nullptr)return;
    std::uint64_t initial;
    {TaskGuard guard;if(g_task_cycle)return;g_task_cycle=true;g_task_verified=0;g_task_cycle_verified=0;
        g_task_cycle_seen=0;g_task_cycle_failed=0;g_task_callbacks=0;initial=g_task_changes;}
    jint count=0;jclass* classes=nullptr;std::vector<jclass> selected;
    bool valid=g_jvmti->GetLoadedClasses(&count,&classes)==JVMTI_ERROR_NONE&&classes!=nullptr;
    if(valid)for(jint i=0;i<count;++i) {
        char* signature=nullptr;
        if(g_jvmti->GetClassSignature(classes[i],&signature,nullptr)!=JVMTI_ERROR_NONE||signature==nullptr){valid=false;continue;}
        bool target=false;
        for(const auto* name:TASK_TARGETS) {size_t n=std::strlen(name);if(std::strlen(signature)==n+2&&signature[0]=='L'
            &&std::strncmp(signature+1,name,n)==0&&signature[n+1]==';'){target=true;break;}}
        g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
        if(!target)continue;
        jobject loader=nullptr;jboolean modifiable=JNI_FALSE;
        bool eligible=g_jvmti->GetClassLoader(classes[i],&loader)==JVMTI_ERROR_NONE&&loader==nullptr
            &&g_jvmti->IsModifiableClass(classes[i],&modifiable)==JVMTI_ERROR_NONE&&modifiable==JNI_TRUE;
        if(loader!=nullptr)env->DeleteLocalRef(loader);
        if(eligible)selected.push_back(classes[i]);else valid=false;
    }
    g_task_retransform=true;
    bool completed=valid&&!selected.empty()&&g_jvmti->RetransformClasses(static_cast<jint>(selected.size()),selected.data())==JVMTI_ERROR_NONE;
    g_task_retransform=false;
    if(classes!=nullptr){for(jint i=0;i<count;++i)env->DeleteLocalRef(classes[i]);g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(classes));}
    {TaskGuard guard;
        if(completed&&initial==g_task_changes&&g_task_callbacks==selected.size()) {
            g_task_verified=g_task_cycle_verified&~g_task_cycle_failed;++g_task_generation;
        }
        g_task_cycle=false;
    }
}

bool retransform_targets(JNIEnv* env, bool include_query_targets) {
    if (g_jvmti == nullptr || env == nullptr) return false;
    jint class_count = 0;
    jclass* classes = nullptr;
    if (g_jvmti->GetLoadedClasses(&class_count, &classes) != JVMTI_ERROR_NONE || classes == nullptr) {
        return false;
    }

    std::vector<jclass> targets;
    targets.reserve(128);
    for (jint index = 0; index < class_count; ++index) {
        char* signature = nullptr;
        if (g_jvmti->GetClassSignature(classes[index], &signature, nullptr) != JVMTI_ERROR_NONE || signature == nullptr) {
            continue;
        }
        bool match = is_loaded_candidate_signature(signature, include_query_targets);
        g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
        jboolean modifiable = JNI_FALSE;
        if (match && g_jvmti->IsModifiableClass(classes[index], &modifiable) == JVMTI_ERROR_NONE
            && modifiable == JNI_TRUE) {
            targets.push_back(classes[index]);
        }
    }
    bool success = targets.empty() ||
        g_jvmti->RetransformClasses(static_cast<jint>(targets.size()), targets.data()) == JVMTI_ERROR_NONE;
    for (jint i = 0; i < class_count; ++i) env->DeleteLocalRef(classes[i]);
    g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(classes));
    return success;
}

bool reassert_core_targets(JNIEnv* env) {
    if (g_jvmti == nullptr || env == nullptr) return false;
    std::uint64_t initial_changes;
    {
        SemanticStateGuard guard;
        if (g_semantic_cycle_active.load()) return false;
        g_semantic_cycle_active.store(true, std::memory_order_release);
        g_last_success_tick.store(0, std::memory_order_release);
        g_semantic_cycle_mask.store(0);
        g_semantic_cycle_failed_mask.store(0);
        g_semantic_cycle_callbacks = 0;
        initial_changes = g_semantic_outside_changes;
    }
    jint class_count = 0;
    jclass* classes = nullptr;
    bool inventory = g_jvmti->GetLoadedClasses(&class_count, &classes) == JVMTI_ERROR_NONE && classes != nullptr;
    LONG required = 0;
    std::vector<jclass> targets;
    targets.reserve(sizeof(SEMANTIC_TARGETS) / sizeof(SEMANTIC_TARGETS[0]));
    if (inventory) for (jint index = 0; index < class_count; ++index) {
        char* signature = nullptr;
        if (g_jvmti->GetClassSignature(classes[index], &signature, nullptr) != JVMTI_ERROR_NONE || signature == nullptr) {
            inventory = false;
            if (signature != nullptr) g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
            continue;
        }
        const LONG bit = target_bit_from_signature(signature);
        g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
        if (bit == 0) continue;
        required |= bit;
        jboolean modifiable = JNI_FALSE;
        if (g_jvmti->IsModifiableClass(classes[index], &modifiable) == JVMTI_ERROR_NONE && modifiable == JNI_TRUE)
            targets.push_back(classes[index]);
        else inventory = false; // A second modifiable copy cannot cover this one.
    }
    g_semantic_reassertions.fetch_add(1, std::memory_order_relaxed);
    g_core_cycle_retransform = true;
    const bool retransformed = inventory && !targets.empty()
        && g_jvmti->RetransformClasses(static_cast<jint>(targets.size()), targets.data()) == JVMTI_ERROR_NONE;
    g_core_cycle_retransform = false;
    if (classes != nullptr) {
        for (jint i = 0; i < class_count; ++i) env->DeleteLocalRef(classes[i]);
        g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(classes));
    }
    SemanticStateGuard guard;
    required |= g_semantic_required_mask.load();
    g_semantic_required_mask.store(required, std::memory_order_release);
    const LONG failed = g_semantic_cycle_failed_mask.load();
    const LONG verified = g_semantic_cycle_mask.load() & ~failed & required;
    const bool complete = retransformed && required != 0 && (verified & required) == required
        && g_semantic_cycle_callbacks == targets.size() && initial_changes == g_semantic_outside_changes;
    g_semantic_verified_mask.store(verified, std::memory_order_release);
    if (complete) {
        g_semantic_generation.fetch_add(1, std::memory_order_relaxed);
        g_successful_leases.fetch_add(1, std::memory_order_relaxed);
        g_consecutive_lease_failures.store(0, std::memory_order_relaxed);
        g_last_success_tick.store(GetTickCount64(), std::memory_order_release);
    } else {
        g_failed_leases.fetch_add(1, std::memory_order_relaxed);
        g_consecutive_lease_failures.fetch_add(1, std::memory_order_relaxed);
    }
    g_semantic_cycle_active.store(false, std::memory_order_release);
    return complete;
}

bool semantic_lease_current(std::uint64_t now) {
    const LONG required = g_semantic_required_mask.load(std::memory_order_acquire);
    const LONG verified = g_semantic_verified_mask.load(std::memory_order_acquire);
    const std::uint64_t last = g_last_success_tick.load(std::memory_order_acquire);
    const std::uint64_t interval = static_cast<std::uint64_t>(
        InterlockedCompareExchange(&g_watchdog_interval, 0, 0)
    );
    const std::uint64_t maximum_age = interval * 2ULL + 5000ULL;
    return !g_semantic_cycle_active.load(std::memory_order_acquire)
        && g_consecutive_lease_failures.load(std::memory_order_acquire) == 0
        && required != 0 && (verified & required) == required && last != 0
        && now >= last && now - last <= maximum_age;
}

DWORD WINAPI watchdog_main(void* argument) {
    auto* run=static_cast<WatchdogRun*>(argument);
    JNIEnv* env = nullptr;
    const bool attached=g_vm!=nullptr&&g_vm->AttachCurrentThreadAsDaemon(reinterpret_cast<void**>(&env),nullptr)==JNI_OK;
    std::uint64_t last_cycle=0;
    while(attached&&!run->stop.load(std::memory_order_acquire)) {
        DWORD wait=WaitForSingleObject(run->wake,static_cast<DWORD>(InterlockedCompareExchange(&g_watchdog_interval,0,0)));
        if(wait==WAIT_FAILED||run->stop.load())break;
        if(wait==WAIT_OBJECT_0&&!g_refresh_pending.load(std::memory_order_acquire))continue;
        // Coalesce load/transform bursts. Never retransform recursively inside a class hook,
        // and do not turn a stream of outside changes into an unbounded full-JVM scan loop.
        while(!run->stop.load()&&last_cycle!=0&&GetTickCount64()-last_cycle<1000)Sleep(25);
        if(!run->stop.load()) {
            g_refresh_pending.store(false,std::memory_order_release);
            last_cycle=GetTickCount64();
#ifdef RONOVA_CODE_SEALS
            reassert_registered_code(env);
#endif
            reassert_core_targets(env);
            reassert_task_targets(env);
            last_cycle=GetTickCount64(); // Coalesce relative to completion, including expensive VM safepoints.
        }
    }
    if(attached)g_vm->DetachCurrentThread();
    AcquireSRWLockExclusive(&g_watchdog_lifecycle);
    if(g_watchdog_run==run){g_watchdog_run=nullptr;InterlockedExchange(&g_watchdog_running,0);}
    CloseHandle(run->wake);CloseHandle(run->thread);
    ReleaseSRWLockExclusive(&g_watchdog_lifecycle);
    delete run;
    return 0;
}
} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_dev_ronova_coremod_nativebridge_NativeBridge_enableCodeSeals0(JNIEnv* env,jclass,jstring id) {
#ifdef RONOVA_CODE_SEALS
    if(id==nullptr||g_jvmti==nullptr||g_root_loader==nullptr)return JNI_FALSE;
    const char* text=env->GetStringUTFChars(id,nullptr);if(text==nullptr)return JNI_FALSE;
    bool match=std::strcmp(text,CODE_SEAL_BUILD_ID)==0;env->ReleaseStringUTFChars(id,text);
    if(!match)return JNI_FALSE;
    g_code_seals_enabled.store(true);
    if(g_jvmti->SetEventNotificationMode(JVMTI_ENABLE,JVMTI_EVENT_CLASS_PREPARE,nullptr)!=JVMTI_ERROR_NONE){g_code_seals_enabled.store(false);return JNI_FALSE;}
    reassert_registered_code(env);return JNI_TRUE;
#else
    return JNI_FALSE;
#endif
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_dev_ronova_coremod_nativebridge_NativeBridge_codeSealState0(JNIEnv* env,jclass) {
    jlong values[5]={0,0,0,0,g_code_replacements.load()};
#ifdef RONOVA_CODE_SEALS
    {
        CodeStateGuard guard;
        values[0]=g_code_seals_enabled.load()?1:0;
        values[1]=sizeof(CODE_IMAGES)/sizeof(CODE_IMAGES[0]);
        for(auto& image:CODE_IMAGES)if(image.loaded.load()){
            ++values[2];if(!g_code_cycle_active&&image.verified.load())++values[3];
        }
        values[4]=g_code_replacements.load();
    }
#endif
    jlongArray result=env->NewLongArray(5);if(result!=nullptr)env->SetLongArrayRegion(result,0,5,values);return result;
}

extern "C" JNIEXPORT void JNICALL
Java_dev_ronova_coremod_nativebridge_NativeBridge_refreshCodeSeals0(JNIEnv* env,jclass) {
#ifdef RONOVA_CODE_SEALS
    reassert_registered_code(env);
#endif
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_dev_ronova_coremod_nativebridge_NativeBridge_codeSealChallenge0(JNIEnv* env,jclass,jclass probe,jlong challenge) {
    jlong values[6]={0,0,static_cast<jlong>(g_code_challenge_generation.load()),
        static_cast<jlong>(g_code_challenge_attempts.load()),static_cast<jlong>(g_code_challenge_successes.load()),
        static_cast<jlong>(g_code_challenge_failures.load())};
#ifdef RONOVA_CODE_SEALS
    bool supported=false,ready=false;std::uint64_t outside_before=0;
    {
        CodeStateGuard guard;
        auto* image=code_image("dev/ronova/coremod/integrity/CodeSealProbe");
        supported=g_code_seals_enabled.load()&&image!=nullptr&&image->loaded.load();
        ready=supported&&!g_code_cycle_active&&image->verified.load();
        outside_before=g_code_outside_changes;
    }
    values[0]=supported?1:0;
    if(supported) {
        g_code_challenge_attempts.fetch_add(1,std::memory_order_relaxed);
        bool matched=false,identity=false;
        if(ready&&probe!=nullptr) {
            char* signature=nullptr;jobject loader=nullptr;
            if(g_jvmti!=nullptr&&g_jvmti->GetClassSignature(probe,&signature,nullptr)==JVMTI_ERROR_NONE&&signature!=nullptr
                &&std::strcmp(signature,"Ldev/ronova/coremod/integrity/CodeSealProbe;")==0
                &&g_jvmti->GetClassLoader(probe,&loader)==JVMTI_ERROR_NONE) {
                auto* image=code_image_signature(signature);identity=image!=nullptr&&registered_code_loader(env,loader,image);
            }
            if(signature!=nullptr)g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
            if(loader!=nullptr)env->DeleteLocalRef(loader);
            jmethodID method=identity?env->GetStaticMethodID(probe,"evaluate","(J)J"):nullptr;
            if(method!=nullptr&&!env->ExceptionCheck()) {
                const jlong observed=env->CallStaticLongMethod(probe,method,challenge);
                if(!env->ExceptionCheck()) {
                    const std::uint64_t mixed=static_cast<std::uint64_t>(challenge)^0x7170D15EA5E12345ULL;
                    const std::uint64_t expected=((mixed<<17)|(mixed>>47))+0x13579BDF2468ACE0ULL;
                    bool still_ready=false;
                    {
                        CodeStateGuard guard;
                        auto* image=code_image("dev/ronova/coremod/integrity/CodeSealProbe");
                        still_ready=image!=nullptr&&image->loaded.load()&&image->verified.load()
                            &&!g_code_cycle_active&&outside_before==g_code_outside_changes;
                    }
                    matched=still_ready&&static_cast<std::uint64_t>(observed)==expected;
                } else env->ExceptionClear();
            } else if(env->ExceptionCheck()) env->ExceptionClear();
        }
        if(matched) {
            g_code_challenge_successes.fetch_add(1,std::memory_order_relaxed);
            g_code_challenge_generation.fetch_add(1,std::memory_order_relaxed);
            values[1]=1;
        } else g_code_challenge_failures.fetch_add(1,std::memory_order_relaxed);
        values[2]=static_cast<jlong>(g_code_challenge_generation.load());
        values[3]=static_cast<jlong>(g_code_challenge_attempts.load());
        values[4]=static_cast<jlong>(g_code_challenge_successes.load());
        values[5]=static_cast<jlong>(g_code_challenge_failures.load());
    }
#endif
    jlongArray result=env->NewLongArray(6);
    if(result!=nullptr)env->SetLongArrayRegion(result,0,6,values);
    return result;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    g_vm = vm;
    void* environment = nullptr;
    if (vm->GetEnv(&environment, JVMTI_VERSION_1_2) == JNI_OK) {
        g_jvmti = static_cast<jvmtiEnv*>(environment);
    }
    return JNI_VERSION_1_8;
}

extern "C" JNIEXPORT jint JNICALL
Java_dev_ronova_coremod_nativebridge_NativeBridge_bootstrap(JNIEnv* env, jclass owner) {
    if (g_jvmti == nullptr) return 0;
    jint mask = CAP_JVMTI;
    jobject loader=nullptr;
    if(g_jvmti->GetClassLoader(owner,&loader)==JVMTI_ERROR_NONE&&loader!=nullptr) {
        g_root_loader=env->NewGlobalRef(loader);env->DeleteLocalRef(loader);
    }

    jclass local_bridge = env->FindClass("dev/ronova/coremod/agent/NativeTransformBridge");
    if (local_bridge == nullptr) {
        env->ExceptionClear();
        return mask;
    }
    g_transform_bridge = static_cast<jclass>(env->NewGlobalRef(local_bridge));
    env->DeleteLocalRef(local_bridge);
    g_transform_method = env->GetStaticMethodID(
        g_transform_bridge, "transform", "(Ljava/lang/String;[B)[B"
    );
    if (g_transform_method == nullptr) {
        env->ExceptionClear();
        return mask;
    }
    g_verify_method = env->GetStaticMethodID(
        g_transform_bridge, "verify", "(Ljava/lang/String;[BLjava/lang/ClassLoader;)Z"
    );
    if (g_verify_method == nullptr) { env->ExceptionClear(); return mask; }

    jvmtiCapabilities potential{};
    jvmtiCapabilities requested{};
    g_jvmti->GetPotentialCapabilities(&potential);
    requested.can_retransform_classes = potential.can_retransform_classes;
    requested.can_retransform_any_class = potential.can_retransform_any_class;
    requested.can_generate_all_class_hook_events = potential.can_generate_all_class_hook_events;
    requested.can_access_local_variables = potential.can_access_local_variables;
    if (g_jvmti->AddCapabilities(&requested) == JVMTI_ERROR_NONE && requested.can_retransform_classes) {
        mask |= CAP_RETRANSFORM;
    }

    jvmtiEventCallbacks callbacks{};
    callbacks.ClassFileLoadHook = &class_file_load_hook;
    callbacks.ThreadEnd = &task_thread_end;
#ifdef RONOVA_CODE_SEALS
    callbacks.ClassPrepare = &code_class_prepared;
#endif
    if (g_jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks)) == JVMTI_ERROR_NONE &&
        g_jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, nullptr) == JVMTI_ERROR_NONE) {
        mask |= CAP_HOOK;
        g_jvmti->SetEventNotificationMode(JVMTI_ENABLE,JVMTI_EVENT_THREAD_END,nullptr);
    }
    if ((mask & CAP_HOOK) != 0 && (mask & CAP_RETRANSFORM) != 0) {
        // One eager pass establishes query/lifecycle filters. The watchdog is
        // only a low-frequency fallback; ordinary retransforms are handled by
        // ClassFileLoadHook as they happen.
        retransform_targets(env, true);
        reassert_core_targets(env);
        mask |= CAP_SEMANTIC_LEASE;
    }
    return mask;
}

extern "C" JNIEXPORT void JNICALL
Java_dev_ronova_coremod_nativebridge_NativeBridge_startWatchdog(JNIEnv*, jclass, jint interval) {
    if(interval<15000)interval=15000;
    if(interval>60000)interval=60000;
    InterlockedExchange(&g_watchdog_interval, interval);
    AcquireSRWLockExclusive(&g_watchdog_lifecycle);
    if(g_integrity_shutdown.load()) {ReleaseSRWLockExclusive(&g_watchdog_lifecycle);return;}
    if(g_watchdog_run!=nullptr&&WaitForSingleObject(g_watchdog_run->thread,0)==WAIT_OBJECT_0) {
        // A terminated worker could not execute its own cleanup. Reclaim its exact run
        // before replacing it; a sleeping/stopping live worker is never duplicated.
        CloseHandle(g_watchdog_run->wake);CloseHandle(g_watchdog_run->thread);
        delete g_watchdog_run;g_watchdog_run=nullptr;InterlockedExchange(&g_watchdog_running,0);
    }
    if(g_watchdog_run==nullptr) {
        auto* run=new(std::nothrow) WatchdogRun();
        if(run!=nullptr) {
            run->wake=CreateEventW(nullptr,FALSE,FALSE,nullptr);
            if(run->wake!=nullptr) {
                g_watchdog_run=run;InterlockedExchange(&g_watchdog_running,1);
                run->thread=CreateThread(nullptr,0,&watchdog_main,run,0,nullptr);
                if(run->thread!=nullptr) {
                    if(g_refresh_pending.load())SetEvent(run->wake);
                } else {
                    g_watchdog_run=nullptr;InterlockedExchange(&g_watchdog_running,0);
                    CloseHandle(run->wake);delete run;
                }
            } else delete run;
        }
    }
    ReleaseSRWLockExclusive(&g_watchdog_lifecycle);
}

extern "C" JNIEXPORT void JNICALL
Java_dev_ronova_coremod_nativebridge_NativeBridge_shutdown(JNIEnv*, jclass) {
    AcquireSRWLockExclusive(&g_watchdog_lifecycle);
    g_integrity_shutdown.store(true,std::memory_order_release);
    InterlockedExchange(&g_watchdog_running, 0);
    if(g_watchdog_run!=nullptr){g_watchdog_run->stop.store(true,std::memory_order_release);SetEvent(g_watchdog_run->wake);}
    ReleaseSRWLockExclusive(&g_watchdog_lifecycle);
}

extern "C" JNIEXPORT jclass JNICALL
Java_dev_ronova_coremod_nativebridge_NativeBridge_prepareTaskBridge0(JNIEnv* env,jclass,jstring jar_path) {
    if(g_jvmti==nullptr||jar_path==nullptr)return nullptr;
    // JVMTI filesystem segments use the Windows platform encoding, not JNI modified UTF-8.
    const jchar* wide=env->GetStringChars(jar_path,nullptr);if(wide==nullptr)return nullptr;
    jint length=env->GetStringLength(jar_path);BOOL substituted=FALSE;
    int bytes=WideCharToMultiByte(CP_ACP,WC_NO_BEST_FIT_CHARS,reinterpret_cast<const wchar_t*>(wide),length,nullptr,0,nullptr,&substituted);
    std::string path(bytes>0?bytes:0,'\0');
    if(bytes>0)WideCharToMultiByte(CP_ACP,WC_NO_BEST_FIT_CHARS,reinterpret_cast<const wchar_t*>(wide),length,path.data(),bytes,nullptr,&substituted);
    env->ReleaseStringChars(jar_path,wide);
    if(bytes<=0||substituted){env->ThrowNew(env->FindClass("java/io/IOException"),"bootstrap JAR path is not representable in Windows platform encoding");return nullptr;}
    jvmtiError appended=g_jvmti->AddToBootstrapClassLoaderSearch(path.c_str());
    if(appended!=JVMTI_ERROR_NONE){char message[96];std::snprintf(message,sizeof(message),"AddToBootstrapClassLoaderSearch failed: %d",static_cast<int>(appended));env->ThrowNew(env->FindClass("java/io/IOException"),message);return nullptr;}
    jclass class_type=env->FindClass("java/lang/Class");if(class_type==nullptr)return nullptr;
    jmethodID find=env->GetStaticMethodID(class_type,"forName","(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;");
    jmethodID module=env->GetMethodID(class_type,"getModule","()Ljava/lang/Module;");
    if(find==nullptr||module==nullptr){env->DeleteLocalRef(class_type);return nullptr;}
    jstring name=env->NewStringUTF("dev.ronova.coremod.agent.BootstrapTaskHooks");
    auto hooks=static_cast<jclass>(env->CallStaticObjectMethod(class_type,find,name,JNI_TRUE,nullptr));
    env->DeleteLocalRef(name);
    if(env->ExceptionCheck()||hooks==nullptr){env->DeleteLocalRef(class_type);return nullptr;}
    jclass object_type=env->FindClass("java/lang/Object");
    jobject base_module=env->CallObjectMethod(object_type,module),hook_module=env->CallObjectMethod(hooks,module);
    bool ready=!env->ExceptionCheck()&&base_module!=nullptr&&hook_module!=nullptr
        &&g_jvmti->AddModuleReads(base_module,hook_module)==JVMTI_ERROR_NONE;
    if(base_module!=nullptr)env->DeleteLocalRef(base_module);if(hook_module!=nullptr)env->DeleteLocalRef(hook_module);
    env->DeleteLocalRef(object_type);env->DeleteLocalRef(class_type);
    if(!ready){env->DeleteLocalRef(hooks);return nullptr;}
    JNINativeMethod task_methods[]={
        {const_cast<char*>("nativeEnter"),const_cast<char*>("(Ljava/lang/Object;)J"),reinterpret_cast<void*>(&native_task_enter)},
        {const_cast<char*>("nativeExit"),const_cast<char*>("(J)V"),reinterpret_cast<void*>(&native_task_exit)},
        {const_cast<char*>("nativeCancelPermit"),const_cast<char*>("(Ljava/lang/Object;Ljava/lang/Object;II)Z"),reinterpret_cast<void*>(&native_cancel_permit)},
        {const_cast<char*>("nativeCancelCommitted"),const_cast<char*>("(Ljava/lang/Object;Z)V"),reinterpret_cast<void*>(&native_cancel_committed)}
    };
    if(env->RegisterNatives(hooks,task_methods,4)!=JNI_OK){env->DeleteLocalRef(hooks);return nullptr;}
    g_task_hooks=static_cast<jclass>(env->NewGlobalRef(hooks));if(g_task_hooks==nullptr){env->DeleteLocalRef(hooks);return nullptr;}
    g_task_bridge_ready.store(true,std::memory_order_release);return hooks;
}

extern "C" JNIEXPORT void JNICALL
Java_dev_ronova_coremod_nativebridge_NativeBridge_refreshTaskObservers0(JNIEnv* env,jclass) {reassert_task_targets(env);}

extern "C" JNIEXPORT jlongArray JNICALL
Java_dev_ronova_coremod_nativebridge_NativeBridge_taskObserverState0(JNIEnv* env,jclass) {
    jlong values[5];
    {TaskGuard guard;values[0]=g_task_bridge_ready.load()?1:0;values[1]=g_task_verified;
        values[2]=static_cast<jlong>(g_task_generation);values[3]=static_cast<jlong>(g_task_changes);values[4]=g_task_cycle?1:0;}
    jlongArray result=env->NewLongArray(5);if(result!=nullptr)env->SetLongArrayRegion(result,0,5,values);return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_dev_ronova_coremod_nativebridge_NativeBridge_currentTaskReceiver0(JNIEnv* env,jclass,jobject expected) {
    if(expected==nullptr||g_jvmti==nullptr||!g_task_bridge_ready.load())return JNI_FALSE;
    jthread thread=nullptr;if(g_jvmti->GetCurrentThread(&thread)!=JVMTI_ERROR_NONE||thread==nullptr)return JNI_FALSE;
    jvmtiFrameInfo frames[64];jint count=0;bool matched=false;
    if(g_jvmti->GetStackTrace(thread,0,64,frames,&count)==JVMTI_ERROR_NONE)for(jint i=0;i<count;++i) {
        jclass owner=nullptr;char* signature=nullptr;char* method=nullptr;char* descriptor=nullptr;
        if(g_jvmti->GetMethodDeclaringClass(frames[i].method,&owner)!=JVMTI_ERROR_NONE||owner==nullptr)continue;
        bool candidate=false;
        if(g_jvmti->GetClassSignature(owner,&signature,nullptr)==JVMTI_ERROR_NONE&&signature!=nullptr
            &&g_jvmti->GetMethodName(frames[i].method,&method,&descriptor,nullptr)==JVMTI_ERROR_NONE&&method!=nullptr) {
            candidate=execution_task_bit(env,frames[i].method)!=0;
        }
        if(signature!=nullptr)g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
        if(method!=nullptr)g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(method));
        if(descriptor!=nullptr)g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(descriptor));
        if(candidate) {
            jobject defining_loader=nullptr;
            candidate=g_jvmti->GetClassLoader(owner,&defining_loader)==JVMTI_ERROR_NONE&&defining_loader==nullptr;
            if(defining_loader!=nullptr)env->DeleteLocalRef(defining_loader);
        }
        env->DeleteLocalRef(owner);
        if(candidate) {
            jobject receiver=nullptr;
            LONG bit=execution_task_bit(env,frames[i].method);
            jvmtiError local=(bit==8||bit==64)?g_jvmti->GetLocalObject(thread,i,bit==8?2:1,&receiver):g_jvmti->GetLocalInstance(thread,i,&receiver);
            matched=local==JVMTI_ERROR_NONE&&receiver!=nullptr&&env->IsSameObject(receiver,expected);
            // Live JVMTI cannot acquire can_access_local_variables. In that mode only an
            // entry captured at the certified JDK callsite can establish receiver identity.
            if(local==JVMTI_ERROR_MUST_POSSESS_CAPABILITY&&g_task_depth>0) {
                const auto& captured=g_task_frames[g_task_depth-1];
                TaskGuard guard;
                matched=!g_task_cycle&&(g_task_verified&bit)==bit&&captured.changes==g_task_changes
                    &&captured.boundary_bit==bit&&env->IsSameObject(captured.receiver,expected);
            }
            if(receiver!=nullptr)env->DeleteLocalRef(receiver);
            break; // A deeper parent cannot impersonate the innermost executing task.
        }
    }
    env->DeleteLocalRef(thread);return matched?JNI_TRUE:JNI_FALSE;
}

static int registered_future_kind(JNIEnv* env,jobject task) {
    if(task==nullptr)return 0;
    jclass actual=env->GetObjectClass(task),future=env->FindClass("java/util/concurrent/FutureTask");
    if(actual==nullptr||future==nullptr){if(env->ExceptionCheck())env->ExceptionClear();if(actual!=nullptr)env->DeleteLocalRef(actual);if(future!=nullptr)env->DeleteLocalRef(future);return 0;}
    int kind=env->IsSameObject(actual,future)?1:0;
    if(kind==0) {
        jclass scheduled=env->FindClass("java/util/concurrent/ScheduledThreadPoolExecutor$ScheduledFutureTask");
        if(scheduled!=nullptr){if(env->IsSameObject(actual,scheduled))kind=2;env->DeleteLocalRef(scheduled);}
        if(env->ExceptionCheck())env->ExceptionClear();
    }
    env->DeleteLocalRef(actual);env->DeleteLocalRef(future);return kind;
}
extern "C" JNIEXPORT jint JNICALL
Java_dev_ronova_coremod_nativebridge_NativeBridge_registeredFutureKind0(JNIEnv* env,jclass,jobject task) {
    return registered_future_kind(env,task);
}
extern "C" JNIEXPORT jint JNICALL
Java_dev_ronova_coremod_nativebridge_NativeBridge_cancelRegisteredFuture0(JNIEnv* env,jclass,jobject task,jobject lease,jobject plan) {
    int kind=registered_future_kind(env,task);if(kind==0||lease==nullptr||plan==nullptr||g_task_cancel!=nullptr)return 0;
    LONG required=kind==1?1:3;std::uint64_t changes;
    {TaskGuard guard;if(g_task_cycle||(g_task_verified&required)!=required)return 0;changes=g_task_changes;}
    jclass future=env->FindClass("java/util/concurrent/FutureTask");
    jmethodID cancel=future==nullptr?nullptr:env->GetMethodID(future,"cancel","(Z)Z");
    if(cancel==nullptr){if(env->ExceptionCheck())env->ExceptionClear();if(future!=nullptr)env->DeleteLocalRef(future);return 0;}
    // Only these exact platform classes inherit the empty FutureTask.done(). Calling
    // the base method avoids ScheduledFutureTask.cancel's separate queue-removal lock.
    // false never interrupts the runner or any shared pool thread.
    jclass lease_class=env->GetObjectClass(lease);
    jmethodID valid=lease_class==nullptr?nullptr:env->GetMethodID(lease_class,"valid","(Ldev/ronova/coremod/root/api/RecoveryTaskPlan;)Z");
    jmethodID close=lease_class==nullptr?nullptr:env->GetMethodID(lease_class,"close","()V");
    jfieldID state=future==nullptr?nullptr:env->GetStaticFieldID(future,"STATE","Ljava/lang/invoke/VarHandle;");
    jobject handle=state==nullptr?nullptr:env->GetStaticObjectField(future,state);
    if(lease_class!=nullptr)env->DeleteLocalRef(lease_class);
    if(valid==nullptr||close==nullptr||handle==nullptr||env->ExceptionCheck()) {
        if(env->ExceptionCheck())env->ExceptionClear();if(handle!=nullptr)env->DeleteLocalRef(handle);env->DeleteLocalRef(future);return 0;
    }
    TaskCancelContext context;context.task=task;context.lease=lease;context.plan=plan;context.state_handle=handle;
    context.valid=valid;context.close=close;context.required=required;context.changes=changes;g_task_cancel=&context;
    jboolean accepted=env->CallNonvirtualBooleanMethod(task,future,cancel,JNI_FALSE);g_task_cancel=nullptr;
    env->DeleteLocalRef(handle);env->DeleteLocalRef(future);
    if(env->ExceptionCheck()){env->ExceptionClear();return 3;}
    if(context.uncertain||accepted==JNI_TRUE&&!context.committed)return 3;
    if(context.checked&&!context.allowed)return 0;
    {TaskGuard guard;if(g_task_cycle||g_task_changes!=changes||(g_task_verified&required)!=required)return 3;}
    return accepted==JNI_TRUE?1:2;
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_dev_ronova_coremod_nativebridge_NativeIntegrityBridge_state0(JNIEnv* env, jclass) {
    constexpr jsize STATUS_SIZE = 11;
    jlong values[STATUS_SIZE]{};
    {
    SemanticStateGuard guard;
    const std::uint64_t now = GetTickCount64();
    const std::uint64_t last = g_last_success_tick.load(std::memory_order_acquire);
    const bool current = semantic_lease_current(now);
    values[0] = (current ? 1L : 0L)
        | (InterlockedCompareExchange(&g_watchdog_running, 1, 1) == 1 ? 2L : 0L);
    values[1] = g_semantic_required_mask.load(std::memory_order_acquire);
    values[2] = g_semantic_verified_mask.load(std::memory_order_acquire);
    values[3] = g_semantic_generation.load(std::memory_order_relaxed);
    values[4] = g_successful_leases.load(std::memory_order_relaxed);
    values[5] = g_failed_leases.load(std::memory_order_relaxed);
    values[6] = g_semantic_transformations.load(std::memory_order_relaxed);
    values[7] = g_semantic_failures.load(std::memory_order_relaxed);
    values[8] = g_semantic_reassertions.load(std::memory_order_relaxed);
    values[9] = last == 0 || now < last ? -1L : static_cast<jlong>(now - last);
    values[10] = g_consecutive_lease_failures.load(std::memory_order_relaxed);
    }
    jlongArray result = env->NewLongArray(STATUS_SIZE);
    if (result != nullptr) env->SetLongArrayRegion(result, 0, STATUS_SIZE, values);
    return result;
}


// Read-only storage observation: retain one content handle without denying other users write/delete access.
// This is not a commit gate or a durable Root receipt.
extern "C" JNIEXPORT jlongArray JNICALL
Java_dev_ronova_coremod_nativebridge_NativeBridge_savedFileIdentity0(JNIEnv* env,jclass,jobject descriptor) {
    if(descriptor==nullptr)return nullptr;
    jclass platform=env->FindClass("java/io/FileDescriptor"),actual=env->GetObjectClass(descriptor);
    bool exact=platform!=nullptr&&actual!=nullptr&&env->IsSameObject(platform,actual);
    if(actual!=nullptr)env->DeleteLocalRef(actual);
    if(!exact){if(platform!=nullptr)env->DeleteLocalRef(platform);return nullptr;}
    jfieldID field=env->GetFieldID(platform,"handle","J");env->DeleteLocalRef(platform);
    if(field==nullptr){if(env->ExceptionCheck())env->ExceptionClear();return nullptr;}
    // FileDescriptor.close uses this same monitor. Duplicate while the descriptor
    // is live so concurrent close cannot recycle the numeric handle under us.
    if(env->MonitorEnter(descriptor)!=JNI_OK)return nullptr;
    HANDLE original=reinterpret_cast<HANDLE>(static_cast<intptr_t>(env->GetLongField(descriptor,field)));
    HANDLE retained=INVALID_HANDLE_VALUE;
    bool copied=original!=INVALID_HANDLE_VALUE&&original!=nullptr&&!env->ExceptionCheck()
        &&DuplicateHandle(GetCurrentProcess(),original,GetCurrentProcess(),&retained,0,FALSE,DUPLICATE_SAME_ACCESS);
    env->MonitorExit(descriptor);
    if(!copied)return nullptr;
    FILE_ID_INFO id{};BY_HANDLE_FILE_INFORMATION info{};
    bool valid=GetFileType(retained)==FILE_TYPE_DISK&&GetFileInformationByHandle(retained,&info)
        &&!(info.dwFileAttributes&(FILE_ATTRIBUTE_DIRECTORY|FILE_ATTRIBUTE_REPARSE_POINT))
        &&GetFileInformationByHandleEx(retained,FileIdInfo,&id,sizeof(id));
    CloseHandle(retained);
    if(!valid||env->ExceptionCheck())return nullptr;
    jlong values[3]={static_cast<jlong>(id.VolumeSerialNumber),0,0};
    std::memcpy(&values[1],id.FileId.Identifier,8);std::memcpy(&values[2],id.FileId.Identifier+8,8);
    jlongArray result=env->NewLongArray(3);if(result!=nullptr)env->SetLongArrayRegion(result,0,3,values);return result;
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_dev_ronova_coremod_nativebridge_NativeBridge_readFileImage0(JNIEnv* env,jclass,jstring requested,jlong limit,jobject observer) {
    jlong values[8]={-4,0,0,0,0,0,0,0};
    auto result=[&]() -> jlongArray {
        if(env->ExceptionCheck())return nullptr;
        jlongArray out=env->NewLongArray(8);if(out!=nullptr)env->SetLongArrayRegion(out,0,8,values);return out;
    };
    if(requested==nullptr||observer==nullptr||limit<1||limit>16LL*1024*1024)return result();
    jsize length=env->GetStringLength(requested);if(length<3||length>4096)return result();
    const jchar* chars=env->GetStringChars(requested,nullptr);if(chars==nullptr)return nullptr;
    wchar_t path[4097];bool valid=true;
    for(jsize i=0;i<length;++i){path[i]=static_cast<wchar_t>(chars[i]);if(chars[i]==0||(chars[i]==':'&&i!=1))valid=false;}
    path[length]=0;env->ReleaseStringChars(requested,chars);
    if(!valid||!((path[0]>=L'A'&&path[0]<=L'Z')||(path[0]>=L'a'&&path[0]<=L'z'))
        ||path[1]!=L':'||(path[2]!=L'\\'&&path[2]!=L'/'))return result();
    jclass type=env->GetObjectClass(observer);
    jmethodID allow=type==nullptr?nullptr:env->GetMethodID(type,"allow","()Z");
    jmethodID accept=type==nullptr?nullptr:env->GetMethodID(type,"accept","([BI)V");
    if(type!=nullptr)env->DeleteLocalRef(type);
    if(allow==nullptr||accept==nullptr)return result();
    auto permitted=[&](){return env->CallBooleanMethod(observer,allow)==JNI_TRUE&&!env->ExceptionCheck();};
    if(!permitted()){values[0]=-1;return result();}
    struct ReadHandle {HANDLE value=INVALID_HANDLE_VALUE;~ReadHandle(){if(value!=INVALID_HANDLE_VALUE)CloseHandle(value);}} handle;
    handle.value=CreateFileW(path,GENERIC_READ,FILE_SHARE_READ|FILE_SHARE_WRITE|FILE_SHARE_DELETE,nullptr,OPEN_EXISTING,
        FILE_FLAG_OPEN_REPARSE_POINT|FILE_FLAG_SEQUENTIAL_SCAN,nullptr);
    if(handle.value==INVALID_HANDLE_VALUE){values[0]=-5;values[7]=GetLastError();return result();}
    BY_HANDLE_FILE_INFORMATION before{},after{};FILE_ID_INFO id_before{},id_after{};
    if(GetFileType(handle.value)!=FILE_TYPE_DISK||!GetFileInformationByHandle(handle.value,&before)
        ||(before.dwFileAttributes&(FILE_ATTRIBUTE_DIRECTORY|FILE_ATTRIBUTE_REPARSE_POINT))!=0){values[0]=-4;return result();}
    if(!GetFileInformationByHandleEx(handle.value,FileIdInfo,&id_before,sizeof(id_before))){values[0]=-6;values[7]=GetLastError();return result();}
    std::uint64_t expected=(static_cast<std::uint64_t>(before.nFileSizeHigh)<<32)|before.nFileSizeLow;
    if(expected>static_cast<std::uint64_t>(limit)){values[0]=-2;return result();}
    jbyteArray bytes=env->NewByteArray(16384);if(bytes==nullptr)return nullptr;
    struct LocalBytes {JNIEnv* env;jbyteArray bytes;~LocalBytes(){env->DeleteLocalRef(bytes);}} local{env,bytes};
    unsigned char buffer[16384];std::uint64_t total=0;
    for(;;) {
        if(!permitted()){values[0]=-1;return result();}
        DWORD want=static_cast<DWORD>((static_cast<std::uint64_t>(limit)-total+1)<sizeof(buffer)?(static_cast<std::uint64_t>(limit)-total+1):sizeof(buffer));
        DWORD read=0;
        if(!ReadFile(handle.value,buffer,want,&read,nullptr)){values[0]=-5;values[7]=GetLastError();return result();}
        if(read==0)break;
        total+=read;values[1]=static_cast<jlong>(total);
        if(total>static_cast<std::uint64_t>(limit)){values[0]=-2;return result();}
        env->SetByteArrayRegion(bytes,0,static_cast<jsize>(read),reinterpret_cast<const jbyte*>(buffer));
        if(env->ExceptionCheck())return nullptr;
        env->CallVoidMethod(observer,accept,bytes,static_cast<jint>(read));
        if(env->ExceptionCheck())return nullptr;
    }
    if(!permitted()){values[0]=-1;return result();}
    if(!GetFileInformationByHandle(handle.value,&after)||!GetFileInformationByHandleEx(handle.value,FileIdInfo,&id_after,sizeof(id_after))){values[0]=-6;values[7]=GetLastError();return result();}
    if(id_before.VolumeSerialNumber!=id_after.VolumeSerialNumber||std::memcmp(id_before.FileId.Identifier,id_after.FileId.Identifier,16)!=0
        ||before.nFileSizeHigh!=after.nFileSizeHigh||before.nFileSizeLow!=after.nFileSizeLow||total!=expected
        ||before.ftLastWriteTime.dwHighDateTime!=after.ftLastWriteTime.dwHighDateTime||before.ftLastWriteTime.dwLowDateTime!=after.ftLastWriteTime.dwLowDateTime){values[0]=-3;return result();}
    // Detect replacement of the named entry even if the old content handle remains valid.
    ReadHandle named;
    named.value=CreateFileW(path,GENERIC_READ,FILE_SHARE_READ|FILE_SHARE_WRITE|FILE_SHARE_DELETE,nullptr,OPEN_EXISTING,FILE_FLAG_OPEN_REPARSE_POINT,nullptr);
    FILE_ID_INFO named_id{};BY_HANDLE_FILE_INFORMATION named_info{};
    if(named.value==INVALID_HANDLE_VALUE||!GetFileInformationByHandleEx(named.value,FileIdInfo,&named_id,sizeof(named_id))
        ||!GetFileInformationByHandle(named.value,&named_info)){values[0]=-3;return result();}
    if((named_info.dwFileAttributes&(FILE_ATTRIBUTE_DIRECTORY|FILE_ATTRIBUTE_REPARSE_POINT))!=0
        ||named_id.VolumeSerialNumber!=id_after.VolumeSerialNumber||std::memcmp(named_id.FileId.Identifier,id_after.FileId.Identifier,16)!=0
        ||named_info.nFileSizeHigh!=after.nFileSizeHigh||named_info.nFileSizeLow!=after.nFileSizeLow
        ||named_info.ftLastWriteTime.dwHighDateTime!=after.ftLastWriteTime.dwHighDateTime||named_info.ftLastWriteTime.dwLowDateTime!=after.ftLastWriteTime.dwLowDateTime){values[0]=-3;return result();}
    values[0]=1;values[2]=static_cast<jlong>(id_after.VolumeSerialNumber);
    std::memcpy(&values[3],id_after.FileId.Identifier,8);std::memcpy(&values[4],id_after.FileId.Identifier+8,8);
    values[5]=static_cast<jlong>((static_cast<std::uint64_t>(after.ftLastWriteTime.dwHighDateTime)<<32)|after.ftLastWriteTime.dwLowDateTime);
    values[6]=static_cast<jlong>(expected);return result();
}
