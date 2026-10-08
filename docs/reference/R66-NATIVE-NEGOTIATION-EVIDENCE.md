# r66 原生申请与任务清理：fix5 静态证据

日期：2026-09-14。来源为用户提供的原 r66 源码包；**只读静态摘录，不是构建/获授/回调触达/运行结果**。此页补充但不改写 fix4 的源码核对记录。

[当前接线章节](../plans/R66-CAPABILITY-WIRING-PLAN.md#native-negotiation) · [逐节融合处置](../plans/RESTRUCTURE-MAP.md#fix5-addendum-merge) · [来源与机器清单](../plans/restructure/enhancement-addendum-merge.json)

`native/src/ronova_native.cpp` 原字节 SHA-256：`9914ab5bb358f641f3a3fca9cf176fc8d10eb6b0129557b504aa4cc2c9652baa`。下面的 L 行号均指该文件；展示移除了行终止符差异，但没有改写行内内容。

**检索读法：** `can_retransform_any_class` 在一个源码行出现两次字段拼写：一次读取 potential，一次写入 requested；没有后续同名获授回读。词法计数不证明 VM 内部能力未被使用，也不证明整项 B7 的实现状态。`GetCapabilities` 在扫描的 native 源文件中零命中，只能据此说明未发现独立回读。

**清理读法：** `native_task_exit` 是正常配对释放；`task_thread_end` 是线程结束时的剩余帧释放。后者未单独检查 enable 返回值，不等于前者不存在。采纳的是诊断与资源兜底缺口，而不是已经复现的泄漏。


<a id="bootstrap"></a>
## 原生入口与申请/回调/派生 mask · L780–L847

```cpp
 780  JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
 781      g_vm = vm;
 782      void* environment = nullptr;
 783      if (vm->GetEnv(&environment, JVMTI_VERSION_1_2) == JNI_OK) {
 784          g_jvmti = static_cast<jvmtiEnv*>(environment);
 785      }
 786      return JNI_VERSION_1_8;
 787  }
 788  
 789  extern "C" JNIEXPORT jint JNICALL
 790  Java_dev_ronova_coremod_nativebridge_NativeBridge_bootstrap(JNIEnv* env, jclass owner) {
 791      if (g_jvmti == nullptr) return 0;
 792      jint mask = CAP_JVMTI;
 793      jobject loader=nullptr;
 794      if(g_jvmti->GetClassLoader(owner,&loader)==JVMTI_ERROR_NONE&&loader!=nullptr) {
 795          g_root_loader=env->NewGlobalRef(loader);env->DeleteLocalRef(loader);
 796      }
 797  
 798      jclass local_bridge = env->FindClass("dev/ronova/coremod/agent/NativeTransformBridge");
 799      if (local_bridge == nullptr) {
 800          env->ExceptionClear();
 801          return mask;
 802      }
 803      g_transform_bridge = static_cast<jclass>(env->NewGlobalRef(local_bridge));
 804      env->DeleteLocalRef(local_bridge);
 805      g_transform_method = env->GetStaticMethodID(
 806          g_transform_bridge, "transform", "(Ljava/lang/String;[B)[B"
 807      );
 808      if (g_transform_method == nullptr) {
 809          env->ExceptionClear();
 810          return mask;
 811      }
 812      g_verify_method = env->GetStaticMethodID(
 813          g_transform_bridge, "verify", "(Ljava/lang/String;[BLjava/lang/ClassLoader;)Z"
 814      );
 815      if (g_verify_method == nullptr) { env->ExceptionClear(); return mask; }
 816  
 817      jvmtiCapabilities potential{};
 818      jvmtiCapabilities requested{};
 819      g_jvmti->GetPotentialCapabilities(&potential);
 820      requested.can_retransform_classes = potential.can_retransform_classes;
 821      requested.can_retransform_any_class = potential.can_retransform_any_class;
 822      requested.can_generate_all_class_hook_events = potential.can_generate_all_class_hook_events;
 823      requested.can_access_local_variables = potential.can_access_local_variables;
 824      if (g_jvmti->AddCapabilities(&requested) == JVMTI_ERROR_NONE && requested.can_retransform_classes) {
 825          mask |= CAP_RETRANSFORM;
 826      }
 827  
 828      jvmtiEventCallbacks callbacks{};
 829      callbacks.ClassFileLoadHook = &class_file_load_hook;
 830      callbacks.ThreadEnd = &task_thread_end;
 831  #ifdef RONOVA_CODE_SEALS
 832      callbacks.ClassPrepare = &code_class_prepared;
 833  #endif
 834      if (g_jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks)) == JVMTI_ERROR_NONE &&
 835          g_jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, nullptr) == JVMTI_ERROR_NONE) {
 836          mask |= CAP_HOOK;
 837          g_jvmti->SetEventNotificationMode(JVMTI_ENABLE,JVMTI_EVENT_THREAD_END,nullptr);
 838      }
 839      if ((mask & CAP_HOOK) != 0 && (mask & CAP_RETRANSFORM) != 0) {
 840          // One eager pass establishes query/lifecycle filters. The watchdog is
 841          // only a low-frequency fallback; ordinary retransforms are handled by
 842          // ClassFileLoadHook as they happen.
 843          retransform_targets(env, true);
 844          reassert_core_targets(env);
 845          mask |= CAP_SEMANTIC_LEASE;
 846      }
 847      return mask;
```


<a id="task-fallback"></a>
## 任务捕获、正常退出及 ThreadEnd 清理 · L87–L108

```cpp
  87  }
  88  jlong JNICALL native_task_enter(JNIEnv* env,jclass,jobject receiver) {
  89      if(receiver==nullptr||g_task_depth>=128||g_task_serial==INT64_MAX)return 0;
  90      jmethodID method=task_hook_boundary(env,true);if(method==nullptr)return 0;
  91      LONG bit=execution_task_bit(env,method);
  92      std::uint64_t changes;
  93      {TaskGuard guard;if(g_task_cycle||(g_task_verified&bit)!=bit)return 0;changes=g_task_changes;}
  94      jweak weak=env->NewWeakGlobalRef(receiver);if(weak==nullptr)return 0;
  95      jlong token=++g_task_serial;g_task_frames[g_task_depth++]={weak,bit,token,changes};return token;
  96  }
  97  void JNICALL native_task_exit(JNIEnv* env,jclass,jlong token) {
  98      if(token==0||g_task_depth==0)return;
  99      auto& frame=g_task_frames[g_task_depth-1];
 100      jmethodID boundary=task_hook_boundary(env,false);
 101      // Retransformation can turn an active frame's jmethodID into an obsolete id.
 102      // The paired token and certified boundary role remain stable across that change.
 103      if(frame.token!=token||boundary==nullptr||execution_task_bit(env,boundary)!=frame.boundary_bit)return;
 104      env->DeleteWeakGlobalRef(frame.receiver);frame={};--g_task_depth;
 105  }
 106  void JNICALL task_thread_end(jvmtiEnv*,JNIEnv* env,jthread) {
 107      while(g_task_depth>0){auto& frame=g_task_frames[--g_task_depth];env->DeleteWeakGlobalRef(frame.receiver);frame={};}
 108  }
```


<a id="task-state"></a>
## 任务状态公开接口与接收者回退 · L933–L980

```cpp
 933  extern "C" JNIEXPORT jlongArray JNICALL
 934  Java_dev_ronova_coremod_nativebridge_NativeBridge_taskObserverState0(JNIEnv* env,jclass) {
 935      jlong values[5];
 936      {TaskGuard guard;values[0]=g_task_bridge_ready.load()?1:0;values[1]=g_task_verified;
 937          values[2]=static_cast<jlong>(g_task_generation);values[3]=static_cast<jlong>(g_task_changes);values[4]=g_task_cycle?1:0;}
 938      jlongArray result=env->NewLongArray(5);if(result!=nullptr)env->SetLongArrayRegion(result,0,5,values);return result;
 939  }
 940  
 941  extern "C" JNIEXPORT jboolean JNICALL
 942  Java_dev_ronova_coremod_nativebridge_NativeBridge_currentTaskReceiver0(JNIEnv* env,jclass,jobject expected) {
 943      if(expected==nullptr||g_jvmti==nullptr||!g_task_bridge_ready.load())return JNI_FALSE;
 944      jthread thread=nullptr;if(g_jvmti->GetCurrentThread(&thread)!=JVMTI_ERROR_NONE||thread==nullptr)return JNI_FALSE;
 945      jvmtiFrameInfo frames[64];jint count=0;bool matched=false;
 946      if(g_jvmti->GetStackTrace(thread,0,64,frames,&count)==JVMTI_ERROR_NONE)for(jint i=0;i<count;++i) {
 947          jclass owner=nullptr;char* signature=nullptr;char* method=nullptr;char* descriptor=nullptr;
 948          if(g_jvmti->GetMethodDeclaringClass(frames[i].method,&owner)!=JVMTI_ERROR_NONE||owner==nullptr)continue;
 949          bool candidate=false;
 950          if(g_jvmti->GetClassSignature(owner,&signature,nullptr)==JVMTI_ERROR_NONE&&signature!=nullptr
 951              &&g_jvmti->GetMethodName(frames[i].method,&method,&descriptor,nullptr)==JVMTI_ERROR_NONE&&method!=nullptr) {
 952              candidate=execution_task_bit(env,frames[i].method)!=0;
 953          }
 954          if(signature!=nullptr)g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
 955          if(method!=nullptr)g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(method));
 956          if(descriptor!=nullptr)g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(descriptor));
 957          if(candidate) {
 958              jobject defining_loader=nullptr;
 959              candidate=g_jvmti->GetClassLoader(owner,&defining_loader)==JVMTI_ERROR_NONE&&defining_loader==nullptr;
 960              if(defining_loader!=nullptr)env->DeleteLocalRef(defining_loader);
 961          }
 962          env->DeleteLocalRef(owner);
 963          if(candidate) {
 964              jobject receiver=nullptr;
 965              LONG bit=execution_task_bit(env,frames[i].method);
 966              jvmtiError local=(bit==8||bit==64)?g_jvmti->GetLocalObject(thread,i,bit==8?2:1,&receiver):g_jvmti->GetLocalInstance(thread,i,&receiver);
 967              matched=local==JVMTI_ERROR_NONE&&receiver!=nullptr&&env->IsSameObject(receiver,expected);
 968              // Live JVMTI cannot acquire can_access_local_variables. In that mode only an
 969              // entry captured at the certified JDK callsite can establish receiver identity.
 970              if(local==JVMTI_ERROR_MUST_POSSESS_CAPABILITY&&g_task_depth>0) {
 971                  const auto& captured=g_task_frames[g_task_depth-1];
 972                  TaskGuard guard;
 973                  matched=!g_task_cycle&&(g_task_verified&bit)==bit&&captured.changes==g_task_changes
 974                      &&captured.boundary_bit==bit&&env->IsSameObject(captured.receiver,expected);
 975              }
 976              if(receiver!=nullptr)env->DeleteLocalRef(receiver);
 977              break; // A deeper parent cannot impersonate the innermost executing task.
 978          }
 979      }
 980      env->DeleteLocalRef(thread);return matched?JNI_TRUE:JNI_FALSE;
```


<a id="retransform-paths"></a>
## 既有任务目标重转换 · L206–L229

```cpp
 206      if(complete)for(jint i=0;i<count;++i) {
 207          char* signature=nullptr;jobject loader=nullptr;
 208          if(g_jvmti->GetClassSignature(classes[i],&signature,nullptr)!=JVMTI_ERROR_NONE)complete=false;
 209          else if(auto* image=code_image_signature(signature);image!=nullptr) {
 210              if(g_jvmti->GetClassLoader(classes[i],&loader)!=JVMTI_ERROR_NONE)complete=false;
 211              else if(registered_code_loader(env,loader,image)) {
 212                  { CodeStateGuard guard; image->loaded.store(true); }
 213                  jboolean modifiable=JNI_FALSE;
 214                  if(g_jvmti->IsModifiableClass(classes[i],&modifiable)!=JVMTI_ERROR_NONE||modifiable!=JNI_TRUE)complete=false;
 215                  else {
 216                      targets.push_back(classes[i]);entries.push_back({image});
 217                  }
 218              }
 219          }
 220          if(signature!=nullptr)g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
 221          if(loader!=nullptr)env->DeleteLocalRef(loader);
 222      }
 223      if(complete&&!targets.empty()) {
 224          // One VM transition for the full inventory; no verified image is published during the batch.
 225          g_code_cycle_entries=&entries;
 226          const auto error=g_jvmti->RetransformClasses(static_cast<jint>(targets.size()),targets.data());
 227          g_code_cycle_entries=nullptr;
 228          for(auto& entry:entries) {
 229              const bool accepted=error==JVMTI_ERROR_NONE&&entry.callbacks==1&&entry.proposed;
```


<a id="retransform-core"></a>
## 既有核心目标重转换 · L575–L591

```cpp
 575          if(g_jvmti->GetClassSignature(classes[i],&signature,nullptr)!=JVMTI_ERROR_NONE||signature==nullptr){valid=false;continue;}
 576          bool target=false;
 577          for(const auto* name:TASK_TARGETS) {size_t n=std::strlen(name);if(std::strlen(signature)==n+2&&signature[0]=='L'
 578              &&std::strncmp(signature+1,name,n)==0&&signature[n+1]==';'){target=true;break;}}
 579          g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
 580          if(!target)continue;
 581          jobject loader=nullptr;jboolean modifiable=JNI_FALSE;
 582          bool eligible=g_jvmti->GetClassLoader(classes[i],&loader)==JVMTI_ERROR_NONE&&loader==nullptr
 583              &&g_jvmti->IsModifiableClass(classes[i],&modifiable)==JVMTI_ERROR_NONE&&modifiable==JNI_TRUE;
 584          if(loader!=nullptr)env->DeleteLocalRef(loader);
 585          if(eligible)selected.push_back(classes[i]);else valid=false;
 586      }
 587      g_task_retransform=true;
 588      bool completed=valid&&!selected.empty()&&g_jvmti->RetransformClasses(static_cast<jint>(selected.size()),selected.data())==JVMTI_ERROR_NONE;
 589      g_task_retransform=false;
 590      if(classes!=nullptr){for(jint i=0;i<count;++i)env->DeleteLocalRef(classes[i]);g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(classes));}
 591      {TaskGuard guard;
```


<a id="retransform-targets"></a>
## 既有目标重转换结果 · L611–L626

```cpp
 611          if (g_jvmti->GetClassSignature(classes[index], &signature, nullptr) != JVMTI_ERROR_NONE || signature == nullptr) {
 612              continue;
 613          }
 614          bool match = is_loaded_candidate_signature(signature, include_query_targets);
 615          g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
 616          jboolean modifiable = JNI_FALSE;
 617          if (match && g_jvmti->IsModifiableClass(classes[index], &modifiable) == JVMTI_ERROR_NONE
 618              && modifiable == JNI_TRUE) {
 619              targets.push_back(classes[index]);
 620          }
 621      }
 622      bool success = targets.empty() ||
 623          g_jvmti->RetransformClasses(static_cast<jint>(targets.size()), targets.data()) == JVMTI_ERROR_NONE;
 624      for (jint i = 0; i < class_count; ++i) env->DeleteLocalRef(classes[i]);
 625      g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(classes));
 626      return success;
```


<a id="retransform-semantics"></a>
## 既有语义重转换结果 · L654–L670

```cpp
 654          }
 655          const LONG bit = target_bit_from_signature(signature);
 656          g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
 657          if (bit == 0) continue;
 658          required |= bit;
 659          jboolean modifiable = JNI_FALSE;
 660          if (g_jvmti->IsModifiableClass(classes[index], &modifiable) == JVMTI_ERROR_NONE && modifiable == JNI_TRUE)
 661              targets.push_back(classes[index]);
 662          else inventory = false; // A second modifiable copy cannot cover this one.
 663      }
 664      g_semantic_reassertions.fetch_add(1, std::memory_order_relaxed);
 665      g_core_cycle_retransform = true;
 666      const bool retransformed = inventory && !targets.empty()
 667          && g_jvmti->RetransformClasses(static_cast<jint>(targets.size()), targets.data()) == JVMTI_ERROR_NONE;
 668      g_core_cycle_retransform = false;
 669      if (classes != nullptr) {
 670          for (jint i = 0; i < class_count; ++i) env->DeleteLocalRef(classes[i]);
```


<a id="external-scope"></a>
## 外部核对范围（与本地源码观察分开）

对 V1.2 / MERGE-NOTE 中的 AddCapabilities 待确认项，本轮查阅 JDK 17 官方接口说明与 OpenJDK `jdk-17.0.16+8` 标签的 `JvmtiManageCapabilities::add_capabilities`。当前解读、条件及引用集中在[原生申请语义](../plans/R66-CAPABILITY-WIRING-PLAN.md#native-add-capabilities)，此处不把它复制成第二个裁决版本。

本轮没有读取用户 Windows 上那份 `jvmti.h`，其本机路径/行号仅作为增补原文的来源说明保留；没有拿 Linux 环境或网页替代用户 VM 的现场证明。HotSpot 外部源码路径结论不证明该失败在用户现场发生。`DynamicCodeGenerated` 继续按 fix4 的有限范围规划，本轮未独立触发该事件。
