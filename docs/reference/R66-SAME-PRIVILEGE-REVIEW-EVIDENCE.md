# r66 同权限干预面 · 融合复核依据

日期：2026-09-15。性质：**fix7 的只读源码复核与有限外部查证**，不是原作者的勘误原稿，更不是实机攻击结果。

[当前处理回执](../plans/RESTRUCTURE-REVIEW.md#review-fix7-surface) · [现有接线规划](../plans/R66-CAPABILITY-WIRING-PLAN.md#same-privilege-surface) · [收到的完整原文](R66-SAME-PRIVILEGE-SURFACE.md) · [逐节映射](../plans/RESTRUCTURE-MAP.md#fix7-surface-merge)

输入源码 ZIP SHA-256：`1266d8c2dd46b949178d7341ee467333be678fb9b373c1403b576a56c73fd763`。本页的源码计数为限定语料内的词法清点与人工对照；包访问、转换链和 MAC 的规范出处另列。没有运行 Mod、未验证 agent 顺序、未读用户真实逐行验收账本。

<a id="findings"></a>
## 原稿判断与本轮独立复核

| 原稿主题 | 处理 | 本轮所依据的范围 |
|---|---|---|
| 规模与名单 | 源码复算一致 | 378 个 Java 源文件/40,428 物理行；31 个 mixin 源文件/872 行；24 个去重目标；41 Accessor、26 Inject、6 Redirect、0 Overwrite。这里的源文件数不是编译后含内部类的 class 数。 |
| 14/10 与 5/6 | 保留数字，收窄含义 | 14 个目标名与 16 个 MC 原生名单重合，10 个不重合；6 个 Redirect 中 5 个的目标未列入。名单重合不证明该类每个 Mixin 注入都由语义合同验证，更不证明租约当前有效。 |
| 语义链不能还原 | 显式修正 | 没有 MC 原始镜像整体回滚，但 NativeTransformBridge → RonovaTransformer 已调用 repair/patch 并产生替换字节；SemanticGateVerifier 校验登记控制流。不能写成完全没有重施或修复，也不能扩大为任意 Mixin 可恢复。 |
| 全部机制在下游 | 保留残余风险，撤下绝对顺序 | 同权限后续转换可能改变已有注入；JVMTI 先按是否可重转换分组、再按环境创建顺序派发，重转换还区分初始字节与复用输出。ModLauncher 实际管线、agent 顺序和最终落地字节本轮未测，不采用“全部固定下游”的通用结论。 |
| policy 封印名单 | 源码前缀确认，构建数归来源 | 111 个前缀只有 RecoveryChainPolicy/TerminalActionPolicy 两个 policy 类；点名的决定侧生产者未选入。源码包没有构建后的封印 manifest，283 类是交接原稿所报，512/4 MiB 是选择器上限；本轮不证明实际封印镜像装载、覆盖或字节余量。 |
| ceiling 与执行 | 区分生产路径及范围 | 生产源码中 .withCapability 有 3 处显式调用行（全局两处、实体局部一处），所示授予最多 PROTECT_RONOVA；通用 RecoveryChainPolicy.resolve 有 8 处显式调用行，四类 registered* 有 13 处。局部 registered* 按请求及绑定条件计算，可表达 rank 3/4；不等于广谱已经获得 rank 4，也不将有限源码外推为未来所有运行路径永远如此。 |
| 局部固定五键 | 补上下文 | TerminalActionPolicy 有 5 个固定键，resolve 另并入所选动作 requiredKeys；不能把循环发布集合永远记为五键。 |
| 全 54 键/所有动作必废 | 代码路径推论未实测 | 决定侧完整性缺口成立，但“任一 ceiling OFF 改动必使所有局部动作失效”不能由通用解析推出；局部视图可重新赋值，恢复注册通道另读 requested。作用范围、具体改动和实际消费者都需验证，不能从名单缺项证明一次静默全废攻击。 |
| 71 个 API 调用包归属 | 与原稿不同 | 71 处显式 ServerRuntimeRegistry.runtime 调用分布在 8 个其他包，bootstrap 同包为 0。只去掉 public 会破坏这些源码调用；应先拆只读/提交/生命周期门面和迁移必要调用，不照抄“多数包内”。 |
| 破坏性 public 入口 | 接受为优先加固面 | invalidatePasses、clearIdentity、release 的方法体没有操作级授权前置；存在空值/同步等检查不等于授权。连同注册表的公开转发入口与运行时返回的可变对象一起审阅。普通 public 调用不改方法字节，封印不能替代请求授权；但可用反射/agent 等能力的同权限对手不因访问修饰符而得到隔离。 |
| 恢复源全程无绑定 | 收窄为边界信任待确认 | approveRecoverySourcePlan 确实没有显式 bindingVerified 参数，传 true；但 evidence.fresh() 同时要求 observerVerified，重建调用方有 verified/boundary/nativeCurrent/stillBound，维护路径也有资格、语义观察与采后 stillBound。缺口是公开审批边界是否可信接收证据/布尔值、是否在持久提交前复核发行来源与对象绑定；不是已有链完全没有检查，也未证明可利用。 |
| 落盘无密钥 | 当前实现确认，补限制 | 本轮 Java 文本检索未定位 HMAC/Mac/SecretKey/Signature 等，当前 Java 后端与 native Hash 的可见路径使用无密钥摘要。摘要一致不等于可信来源或不可回滚；源码可读不等于能推出运行期随机密钥，但如果同权限环境可取得密钥或改验证路径，HMAC 同样不提供隔离保证。 |
| 无 JPMS/无信号/最稳最脆 | 不将词法零命中变成全局结论 | 源码没有 module-info.java，未观察运行时模块图；integrity 包没有特定 Mixin 专项调用不代表语义校验失败和注入失败完全无信号。HEAD/Redirect 的稳定性排序仅为原作者判断，本轮只核对位置与合同缺口。 |

<a id="mixin-inventory"></a>
## 24 目标与 31 个 Mixin 的静态对账

**“在名单”仅为 native MC 名单重合。所有行的现场租约、逐注入合同和最终生效状态均未在本轮验证。**

| 目标内部名 | Mixin 类 | 侧别 | 在 MC 名单 | Redirect 数 |
|---|---|---|---|---|
| `net/minecraft/client/Minecraft` | `MinecraftClientRuntimeMixin` / `MinecraftScreenGuardMixin` | client | 否 | 0 |
| `net/minecraft/client/MouseHandler` | `MouseHandlerInputMixin` | client | 否 | 0 |
| `net/minecraft/client/multiplayer/ClientLevel` | `ClientLevelAccessor` | client | 是 | 0 |
| `net/minecraft/client/renderer/entity/ItemRenderer` | `ItemRendererMixin` | client | 否 | 0 |
| `net/minecraft/nbt/CompoundTag` | `CompoundTagWriteObservationMixin` | common | 是 | 1 |
| `net/minecraft/nbt/ListTag` | `ListTagWriteObservationMixin` | common | 否 | 1 |
| `net/minecraft/nbt/NbtIo` | `NbtCompressedWriteObservationMixin` | common | 否 | 1 |
| `net/minecraft/network/Connection` | `ConnectionProtectionMixin` | common | 否 | 0 |
| `net/minecraft/network/syncher/SynchedEntityData` | `SynchedEntityDataStateAccessor` | common | 是 | 0 |
| `net/minecraft/network/syncher/SynchedEntityData$DataItem` | `SynchedDataItemStateAccessor` | common | 是 | 0 |
| `net/minecraft/server/MinecraftServer` | `MinecraftServerRuntimeMixin` | common | 否 | 0 |
| `net/minecraft/server/level/ServerLevel` | `ServerLevelAccessor` / `ServerLevelAdmissionMixin` / `ServerLevelTickAdmissionMixin` | common | 是 | 0 |
| `net/minecraft/server/network/ServerGamePacketListenerImpl` | `ServerConnectionProtectionMixin` | common | 否 | 0 |
| `net/minecraft/util/ClassInstanceMultiMap` | `ClassInstanceMultiMapAccessor` | common | 是 | 0 |
| `net/minecraft/world/entity/Entity` | `EntityProtectionMixin` / `EntityStateAccessor` | common | 是 | 0 |
| `net/minecraft/world/entity/EntityType` | `EntityTypeFactoryAdmissionMixin` / `EntityTypeNbtAdmissionMixin` | common | 是 | 0 |
| `net/minecraft/world/entity/LivingEntity` | `LivingEntityProtectionMixin` / `LivingEntityStateAccessor` | common | 是 | 0 |
| `net/minecraft/world/level/entity/EntityLookup` | `EntityLookupAccessor` | common | 是 | 0 |
| `net/minecraft/world/level/entity/EntitySection` | `EntitySectionAccessor` | common | 是 | 0 |
| `net/minecraft/world/level/entity/EntitySectionStorage` | `EntitySectionStorageAccessor` | common | 是 | 0 |
| `net/minecraft/world/level/entity/EntityTickList` | `EntityTickListAccessor` | common | 是 | 0 |
| `net/minecraft/world/level/entity/PersistentEntitySectionManager` | `PersistentEntityAdmissionMixin` / `PersistentEntityManagerAccessor` | common | 是 | 0 |
| `net/minecraft/world/level/saveddata/SavedData` | `SavedDataRecoverySaveMixin` | common | 否 | 2 |
| `net/minecraft/world/level/storage/DimensionDataStorage` | `DimensionDataStorageRecoveryMixin` | common | 否 | 1 |

<a id="runtime-calls"></a>
## 71 处显式 runtime 调用的包归属

注册表位于 `dev.ronova.coremod.bootstrap`；这 71 处显式调用同包数为 **0**。只统计源码中限定表达式，不声称反射或动态调用总量。

| 调用方包 | 匹配行数 |
|---|---:|
| `dev.ronova.coremod.authority` | 6 |
| `dev.ronova.coremod.baseline` | 2 |
| `dev.ronova.coremod.command` | 10 |
| `dev.ronova.coremod.engine` | 38 |
| `dev.ronova.coremod.event` | 4 |
| `dev.ronova.coremod.mixin` | 4 |
| `dev.ronova.coremod.network` | 5 |
| `dev.ronova.coremod.runtime` | 2 |

逐调用文件、行号与原行保存在融合清单 `same_privilege_handoffs[0].source_evidence.inventory.runtime_qualified_call_lines`，不在此复制 71 行第二张目录。

<a id="external"></a>
## 本轮外部核对（与用户源码分开）

JDK 17 对 ClassFileLoadHook 的派发先区分不可重转换/可重转换环境，再按各组环境的创建顺序；重转换对不可重转换输出有复用规则。因此记录观察位置限制，但不从挂载时间一项推出整个管线顺序。框架配置、最终字节与此用户 VM 行为均未验证。

JLS 的包访问只允许同包访问，父子包名相近也不是同包。Mixin 0.8.5 的 parseRequirements/postInject 使用各注入实际的 require/defaultRequire 和成功计数处理失败；配置失败或类加载失败与运行中合同失效应分开。没有用 defaultRequire=0 来隐藏必需注入缺失。

Mac API 的消息认证依赖秘密密钥；如何存取密钥、处理旧格式及回滚是工程设计，而不是 Mac 接口自动解决的事情。以下只引用相应规范或官方源码，不据网页证明本地构建支持。

- [JDK 17 JVMTI — ClassFileLoadHook / RetransformClasses / Environments](https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html#ClassFileLoadHook)：转换链分组与次序、重转换输入、同进程环境相互影响；不是用户现场顺序证明；查阅 2026-09-15。
- [JLS 17 §6.6.1 — Determining Accessibility](https://docs.oracle.com/javase/specs/jls/se17/html/jls-6.html#jls-6.6.1)：包访问限定同包；不证明整个游戏的运行时模块图；查阅 2026-09-15。
- [Mixin 0.8.5 InjectionInfo — parseRequirements / postInject](https://raw.githubusercontent.com/SpongePowered/Mixin/0.8.5/src/main/java/org/spongepowered/asm/mixin/injection/struct/InjectionInfo.java)：注入计数与失败处理；没有据此对所有注入点稳定性排序；查阅 2026-09-15。
- [JDK 17 javax.crypto.Mac](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/javax/crypto/Mac.html)：基于秘密密钥的消息认证；密钥治理及抗回滚安排是拟实施设计；查阅 2026-09-15。

<a id="source-excerpts"></a>
## 直接源码摘录

以下行号均指原 r66 源码；换行规范化为 LF 用于展示与逐行核对，原文件字节哈希另存清单。没有把源码放入待覆盖生产路径。

<a id="source-evidence-fresh"></a>
### source-evidence-fresh

来源：`src/main/java/dev/ronova/coremod/root/api/RecoverySourceEvidence.java:1–17`；原文件 SHA-256：`9ce9a8776387fb9be3b9e5f9415b0b8198c61c4948d8a821c15782ea07c68743`。

```text
   1  package dev.ronova.coremod.root.api;
   2  
   3  import java.util.Objects;
   4  
   5  /** Short-lived evidence supplied by the registered observer; never serialized as a live reference. */
   6  public record RecoverySourceEvidence(String sourceLocator, AuthorityStamp authority, long sourceRevision,
   7                                       int copyDepth, boolean observerVerified, long observedAtNanos) {
   8      public static final long MAX_AGE_NANOS = 5_000_000_000L;
   9      public RecoverySourceEvidence {
  10          sourceLocator = EffectRecord.checkedHash(sourceLocator); Objects.requireNonNull(authority);
  11          if (sourceRevision < 1 || copyDepth < 0) throw new IllegalArgumentException("invalid source evidence revision");
  12      }
  13      public boolean fresh(long now) {
  14          long age = now - observedAtNanos;
  15          return observerVerified && age >= 0 && age <= MAX_AGE_NANOS;
  16      }
  17  }
```

<a id="entity-local"></a>
### entity-local

来源：`src/main/java/dev/ronova/coremod/policy/TerminalActionPolicy.java:18–49`；原文件 SHA-256：`2300395e2c35b9a68d25a715981aacd226a07e614b216c5214506a3886ce884b`。

```text
  18      private static final Set<StrengthOptionKey> KEYS=Set.of(StrengthOptionKey.ATTACK_STATE_SOURCE,
  19          StrengthOptionKey.RUNTIME_NATIVE,StrengthOptionKey.RUNTIME_JVMTI,
  20          StrengthOptionKey.RUNTIME_CRITICAL_CLASS_WATCH,StrengthOptionKey.PROTECT_CODE_INTEGRITY);
  21      private TerminalActionPolicy(){}
  22      public static MechanismLevel requestedDepth(StrengthPolicySnapshot policy,Step step) {
  23          var keys=EnumSet.copyOf(KEYS);var depth=MechanismLevel.STRICT_ALL;
  24          for(var action:step.actions()) {
  25              depth=MechanismLevel.lowerOf(depth,policy.activeActions().resolve(policy.activeProfile(),action).depth());
  26              keys.addAll(action.requiredKeys());
  27          }
  28          for(var key:keys)depth=MechanismLevel.lowerOf(depth,ProfilePresetMatrix.resolve(policy.activeMatrixRevision(),
  29              policy.activeProfile(),key,policy.activeLevels().get(key)).requestedLevel());
  30          return depth;
  31      }
  32      public static AttackResolution resolve(StrengthPolicySnapshot policy,Step step,boolean boundaryVerified) {
  33          Objects.requireNonNull(policy);Objects.requireNonNull(step);
  34          if(policy.pendingRestart())return new AttackResolution(MechanismLevel.OFF,MechanismLevel.OFF,false,"PENDING_RESTART");
  35          // Only this registered executor receives the measured boundary capability.
  36          // Do not advertise these broad option keys as supported for other surfaces.
  37          var keys=EnumSet.copyOf(KEYS);
  38          step.actions().forEach(action->keys.addAll(action.requiredKeys()));
  39          var scoped=policy;
  40          for(var key:keys)scoped=scoped.withCapability(key,MechanismLevel.PROTECT_RONOVA,
  41              boundaryVerified?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,
  42              boundaryVerified?EvidenceState.VERIFIED:EvidenceState.UNOBSERVED,
  43              boundaryVerified?ActivationState.ACTIVE:ActivationState.UNAVAILABLE);
  44          var result=scoped.resolveAttack(step.actions(),KEYS);
  45          if(!result.permitted())return result;
  46          // These executors act on one registered entity and its exact index references.
  47          // Selecting Extreme does not claim producer, native or arbitrary resource coverage.
  48          return new AttackResolution(result.requestedDepth(),MechanismLevel.PROTECT_RONOVA,true,"REGISTERED_ENTITY_ONLY");
  49      }
```

<a id="native-targets"></a>
### native-targets

来源：`native/src/ronova_native.cpp:292–321`；原文件 SHA-256：`9914ab5bb358f641f3a3fca9cf176fc8d10eb6b0129557b504aa4cc2c9652baa`。

```text
 292  constexpr LONG TARGET_ENTITY = 0x01;
 293  constexpr LONG TARGET_LIVING = 0x02;
 294  constexpr LONG TARGET_LEVEL = 0x04;
 295  constexpr LONG TARGET_SERVER_LEVEL = 0x08;
 296  constexpr LONG TARGET_CLIENT_LEVEL = 0x10;
 297  
 298  struct SemanticTarget {
 299      const char* name;
 300      const char* signature;
 301      LONG bit;
 302  };
 303  #define RONOVA_SEMANTIC_TARGET(name, bit) {name, "L" name ";", bit}
 304  constexpr SemanticTarget SEMANTIC_TARGETS[] = {
 305      RONOVA_SEMANTIC_TARGET("net/minecraft/world/entity/Entity", TARGET_ENTITY),
 306      RONOVA_SEMANTIC_TARGET("net/minecraft/world/entity/LivingEntity", TARGET_LIVING),
 307      RONOVA_SEMANTIC_TARGET("net/minecraft/world/level/Level", TARGET_LEVEL),
 308      RONOVA_SEMANTIC_TARGET("net/minecraft/server/level/ServerLevel", TARGET_SERVER_LEVEL),
 309      RONOVA_SEMANTIC_TARGET("net/minecraft/client/multiplayer/ClientLevel", TARGET_CLIENT_LEVEL),
 310      RONOVA_SEMANTIC_TARGET("net/minecraft/network/syncher/SynchedEntityData", 0x20),
 311      RONOVA_SEMANTIC_TARGET("net/minecraft/network/syncher/SynchedEntityData$DataItem", 0x40),
 312      RONOVA_SEMANTIC_TARGET("net/minecraft/world/level/entity/EntityLookup", 0x80),
 313      RONOVA_SEMANTIC_TARGET("net/minecraft/world/level/entity/EntityTickList", 0x100),
 314      RONOVA_SEMANTIC_TARGET("net/minecraft/world/level/entity/EntitySectionStorage", 0x200),
 315      RONOVA_SEMANTIC_TARGET("net/minecraft/world/level/entity/EntitySection", 0x400),
 316      RONOVA_SEMANTIC_TARGET("net/minecraft/util/ClassInstanceMultiMap", 0x800),
 317      RONOVA_SEMANTIC_TARGET("net/minecraft/world/level/entity/PersistentEntitySectionManager", 0x1000),
 318      RONOVA_SEMANTIC_TARGET("net/minecraft/nbt/CompoundTag", 0x2000),
 319      RONOVA_SEMANTIC_TARGET("net/minecraft/world/entity/EntityType", 0x4000),
 320      RONOVA_SEMANTIC_TARGET("net/minecraft/network/syncher/EntityDataAccessor", 0x8000)
 321  };
```

<a id="native-hook"></a>
### native-hook

来源：`native/src/ronova_native.cpp:490–564`；原文件 SHA-256：`9914ab5bb358f641f3a3fca9cf176fc8d10eb6b0129557b504aa4cc2c9652baa`。

```text
 490  void JNICALL class_file_load_hook(
 491      jvmtiEnv* jvmti, JNIEnv* env, jclass redefining, jobject loader, const char* name, jobject,
 492      jint class_data_len, const unsigned char* class_data, jint* new_class_data_len,
 493      unsigned char** new_class_data
 494  ) {
 495      const bool task_owned=task_bit(name)!=0&&g_task_retransform&&redefining!=nullptr;
 496      if(task_bit(name)!=0&&!task_owned) {TaskGuard guard;++g_task_changes;g_task_verified&=~task_bit(name);}
 497  #ifdef RONOVA_CODE_SEALS
 498      if(g_code_seals_enabled.load())if(auto* image=code_image(name); registered_code_loader(env,loader,image)) {
 499          invalidate_code_semantics();
 500          auto* cycle_entry=redefining!=nullptr?code_cycle_entry(image):nullptr;
 501          const bool own_cycle=cycle_entry!=nullptr;
 502          {
 503              CodeStateGuard guard;
 504              if(!own_cycle)++g_code_outside_changes;
 505              image->proposed.store(false);image->verified.store(false);
 506          }
 507          bool proposed=false;
 508          if(class_data!=nullptr&&class_data_len==image->length&&std::memcmp(class_data,image->data,image->length)==0)proposed=true;
 509          else {
 510              unsigned char* replacement=nullptr;
 511              if(jvmti->Allocate(image->length,&replacement)==JVMTI_ERROR_NONE) {
 512                  std::memcpy(replacement,image->data,image->length);*new_class_data_len=image->length;*new_class_data=replacement;
 513                  proposed=true;g_code_replacements.fetch_add(1);
 514              }
 515          }
 516          if(own_cycle){++cycle_entry->callbacks;cycle_entry->proposed=proposed;}
 517          else request_integrity_refresh();
 518          // Hook output is a proposal, not a completed retransformation receipt.
 519          return;
 520      }
 521  #endif
 522      // Invalidate before transformation starts, including a newly defining loader copy.
 523      if (is_core_target(name) && (!g_core_cycle_retransform || redefining == nullptr)) {
 524          SemanticStateGuard guard;
 525          ++g_semantic_outside_changes;
 526          g_last_success_tick.store(0, std::memory_order_release);
 527          g_semantic_required_mask.fetch_or(target_bit(name), std::memory_order_relaxed);
 528          request_integrity_refresh();
 529      }
 530      if (class_data_len < 0 || class_data_len > 8 * 1024 * 1024) {
 531          record_semantic_proof(name, false,task_owned); return;
 532      }
 533      if (!is_candidate(name, class_data, class_data_len)) return;
 534      if (g_transform_bridge == nullptr || g_transform_method == nullptr || g_in_transform) {
 535          record_semantic_proof(name, false,task_owned); return;
 536      }
 537      g_in_transform = true;
 538      bool verified = false;
 539      jstring class_name = env->NewStringUTF(name);
 540      jbyteArray input = env->NewByteArray(class_data_len);
 541      if (class_name != nullptr && input != nullptr) {
 542          env->SetByteArrayRegion(input, 0, class_data_len, reinterpret_cast<const jbyte*>(class_data));
 543          auto output = static_cast<jbyteArray>(env->CallStaticObjectMethod(g_transform_bridge, g_transform_method, class_name, input));
 544          if (env->ExceptionCheck()) env->ExceptionClear();
 545          else if (output != nullptr) {
 546              jsize length = env->GetArrayLength(output);
 547              unsigned char* allocated = nullptr;
 548              if (length > 0 && length <= 8 * 1024 * 1024 && jvmti->Allocate(length, &allocated) == JVMTI_ERROR_NONE) {
 549                  env->GetByteArrayRegion(output, 0, length, reinterpret_cast<jbyte*>(allocated));
 550                  if (env->ExceptionCheck()) { env->ExceptionClear(); jvmti->Deallocate(allocated); }
 551                  else {
 552                      verified = has_semantic_proof(env, class_name, output, loader);
 553                      *new_class_data_len = length; *new_class_data = allocated;
 554                  }
 555              }
 556          } else verified = has_semantic_proof(env, class_name, input, loader);
 557          if (output != nullptr) env->DeleteLocalRef(output);
 558      }
 559      if (env->ExceptionCheck()) env->ExceptionClear();
 560      if (input != nullptr) env->DeleteLocalRef(input);
 561      if (class_name != nullptr) env->DeleteLocalRef(class_name);
 562      record_semantic_proof(name, verified,task_owned);
 563      g_in_transform = false;
 564  }
```

<a id="native-reassert"></a>
### native-reassert

来源：`native/src/ronova_native.cpp:629–691`；原文件 SHA-256：`9914ab5bb358f641f3a3fca9cf176fc8d10eb6b0129557b504aa4cc2c9652baa`。

```text
 629  bool reassert_core_targets(JNIEnv* env) {
 630      if (g_jvmti == nullptr || env == nullptr) return false;
 631      std::uint64_t initial_changes;
 632      {
 633          SemanticStateGuard guard;
 634          if (g_semantic_cycle_active.load()) return false;
 635          g_semantic_cycle_active.store(true, std::memory_order_release);
 636          g_last_success_tick.store(0, std::memory_order_release);
 637          g_semantic_cycle_mask.store(0);
 638          g_semantic_cycle_failed_mask.store(0);
 639          g_semantic_cycle_callbacks = 0;
 640          initial_changes = g_semantic_outside_changes;
 641      }
 642      jint class_count = 0;
 643      jclass* classes = nullptr;
 644      bool inventory = g_jvmti->GetLoadedClasses(&class_count, &classes) == JVMTI_ERROR_NONE && classes != nullptr;
 645      LONG required = 0;
 646      std::vector<jclass> targets;
 647      targets.reserve(sizeof(SEMANTIC_TARGETS) / sizeof(SEMANTIC_TARGETS[0]));
 648      if (inventory) for (jint index = 0; index < class_count; ++index) {
 649          char* signature = nullptr;
 650          if (g_jvmti->GetClassSignature(classes[index], &signature, nullptr) != JVMTI_ERROR_NONE || signature == nullptr) {
 651              inventory = false;
 652              if (signature != nullptr) g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(signature));
 653              continue;
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
 671          g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(classes));
 672      }
 673      SemanticStateGuard guard;
 674      required |= g_semantic_required_mask.load();
 675      g_semantic_required_mask.store(required, std::memory_order_release);
 676      const LONG failed = g_semantic_cycle_failed_mask.load();
 677      const LONG verified = g_semantic_cycle_mask.load() & ~failed & required;
 678      const bool complete = retransformed && required != 0 && (verified & required) == required
 679          && g_semantic_cycle_callbacks == targets.size() && initial_changes == g_semantic_outside_changes;
 680      g_semantic_verified_mask.store(verified, std::memory_order_release);
 681      if (complete) {
 682          g_semantic_generation.fetch_add(1, std::memory_order_relaxed);
 683          g_successful_leases.fetch_add(1, std::memory_order_relaxed);
 684          g_consecutive_lease_failures.store(0, std::memory_order_relaxed);
 685          g_last_success_tick.store(GetTickCount64(), std::memory_order_release);
 686      } else {
 687          g_failed_leases.fetch_add(1, std::memory_order_relaxed);
 688          g_consecutive_lease_failures.fetch_add(1, std::memory_order_relaxed);
 689      }
 690      g_semantic_cycle_active.store(false, std::memory_order_release);
 691      return complete;
```

<a id="bridge"></a>
### bridge

来源：`src/main/java/dev/ronova/coremod/agent/NativeTransformBridge.java:9–35`；原文件 SHA-256：`a5e63a1e620c8321271bc7391d0a9adfa9bb6db4631a1a2a25fe9ef9fcd9df94`。

```text
   9      public static boolean verify(String internalName,byte[] classBytes,ClassLoader definingLoader) {
  10          if(JdkTaskBoundaryContract.registered(internalName)) {
  11              boolean verified=definingLoader==null&&JdkTaskBoundaryContract.enabled()&&JdkTaskBoundaryContract.verify(internalName,classBytes);
  12              if(verified)FAILURES.remove(internalName);else FAILURES.put(internalName,"jdk-task-platform-image-or-boundary-unverified");
  13              return verified;
  14          }
  15          if(!RonovaTransformer.isTarget(internalName))return false;
  16          boolean loader=definingLoader==NativeTransformBridge.class.getClassLoader();
  17          boolean verified=loader&&SemanticGateVerifier.verify(internalName,classBytes);
  18          if(verified)FAILURES.remove(internalName);
  19          else FAILURES.put(internalName,loader?SemanticGateVerifier.explainFailure(internalName,classBytes):"defining-loader-mismatch");
  20          return verified;
  21      }
  22      public static java.util.Map<String,String> verificationFailures(){return java.util.Map.copyOf(FAILURES);}
  23  
  24      public static byte[] transform(String internalName, byte[] classBytes) {
  25          if (!RonovaTransformer.shouldInspect(internalName, classBytes)) {
  26              return null;
  27          }
  28          return TRANSFORMER.transform(
  29              null,
  30              NativeTransformBridge.class.getClassLoader(),
  31              internalName,
  32              null,
  33              null,
  34              classBytes
  35          );
```

<a id="transform-repair"></a>
### transform-repair

来源：`src/main/java/dev/ronova/coremod/agent/RonovaTransformer.java:65–120`；原文件 SHA-256：`6baba73a0fdb0c7df5d8e12121dd4c40ba48d6141a7ffabc60d6f1ca355fda33`。

```text
  65              || CLIENT_LEVEL.equals(internalName);
  66      }
  67  
  68      static boolean shouldInspect(String internalName, byte[] classBytes) {
  69          if(JdkTaskBoundaryContract.enabled()&&JdkTaskBoundaryContract.registered(internalName))return classBytes!=null;
  70          if (internalName == null || classBytes == null || internalName.startsWith("java/")
  71              || internalName.startsWith("jdk/") || internalName.startsWith("sun/")) {
  72              return false;
  73          }
  74          return isTarget(internalName)
  75              || hasEntityReference(classBytes)
  76              || (containsAscii(classBytes, "net/minecraft/client/renderer/entity/")
  77                  && (containsAscii(classBytes, "render") || containsAscii(classBytes, "m_7392_")))
  78              || hasClientControlReference(classBytes)
  79              || containsAscii(classBytes, "f_146795_")
  80              || containsAscii(classBytes, "isAddedToWorld")
  81              || containsAscii(classBytes, "f_20890_")
  82              || containsAscii(classBytes, "f_20919_")
  83              || containsAscii(classBytes, "removalReason")
  84              || containsAscii(classBytes, "deathTime");
  85      }
  86  
  87      @Override
  88      public byte[] transform(
  89          Module module,
  90          ClassLoader loader,
  91          String className,
  92          Class<?> classBeingRedefined,
  93          ProtectionDomain protectionDomain,
  94          byte[] classfileBuffer
  95      ) {
  96          if (!shouldInspect(className, classfileBuffer)) {
  97              return null;
  98          }
  99          if(JdkTaskBoundaryContract.registered(className))return JdkTaskBoundaryContract.transform(className,classfileBuffer);
 100          try {
 101              ClassNode node = new ClassNode();
 102              // Preserve ModLauncher's frames. Recomputing every frame in Entity during
 103              // premain is unsafe because its transforming module hierarchy is not yet
 104              // visible to the system class loader.
 105              new ClassReader(classfileBuffer).accept(node, 0);
 106              boolean changed = false;
 107              changed |= RawAccessorContract.repair(node);
 108              changed |= EntityDataAccessorContract.repair(node);
 109              changed |= NbtCopyContract.patch(node);
 110              changed |= NbtFactoryContract.patch(node);
 111              if (ENTITY.equals(className)) {
 112                  changed |= patchEntity(node);
 113              } else if (LIVING.equals(className)) {
 114                  changed |= patchLiving(node);
 115              }
 116              if (LEVEL.equals(className) || SERVER_LEVEL.equals(className) || CLIENT_LEVEL.equals(className)) {
 117                  changed |= patchQueryReturns(node);
 118              }
 119              if (!className.startsWith("net/minecraft/")
 120                  && !className.startsWith("dev/ronova/coremod/")) {
```

<a id="gate-verifier"></a>
### gate-verifier

来源：`src/main/java/dev/ronova/coremod/agent/SemanticGateVerifier.java:26–54`；原文件 SHA-256：`78c51d80ce798e2d0e0babf46605c840591806fcd8a8ea00cc00f82293dcd562`。

```text
  26      public static boolean verify(String name,byte[] bytes) {
  27          if(name==null||bytes==null||bytes.length>8*1024*1024)return false;
  28          try {
  29              var node=new ClassNode();new ClassReader(bytes).accept(node,0);if(!name.equals(node.name))return false;
  30              if(!RawAccessorContract.verify(node))return false;
  31              if(name.equals(EntityDataAccessorContract.OWNER))return EntityDataAccessorContract.verify(node);
  32              if(name.equals(NbtCopyContract.OWNER))return NbtCopyContract.verify(node);
  33              if(name.equals(NbtFactoryContract.OWNER))return NbtFactoryContract.verify(node);
  34              if(name.equals("net/minecraft/world/entity/Entity"))return required(node,Set.of("remove","m_142687_"),
  35                  "(Lnet/minecraft/world/entity/Entity$RemovalReason;)V","shouldBlockRemoval",Opcodes.ALOAD)
  36                  &&required(node,Set.of("revive"),"()V","shouldBlockRevive",0);
  37              if(name.equals("net/minecraft/world/entity/LivingEntity"))return required(node,Set.of("hurt","m_6469_"),
  38                  "(Lnet/minecraft/world/damagesource/DamageSource;F)Z","shouldBlockDamage",0)
  39                  &&required(node,Set.of("die","m_6667_"),"(Lnet/minecraft/world/damagesource/DamageSource;)V","shouldBlockDeath",0)
  40                  &&required(node,Set.of("setHealth","m_21153_"),"(F)V","shouldBlockHealth",Opcodes.FLOAD);
  41              if(!Set.of("net/minecraft/world/level/Level","net/minecraft/server/level/ServerLevel","net/minecraft/client/multiplayer/ClientLevel").contains(name))
  42                  return RawAccessorContract.registered(name);
  43              int checked=0;
  44              for(var method:node.methods) {
  45                  if(method.name.startsWith("ronova$"))continue; // Raw observers intentionally bypass gameplay Presence filtering.
  46                  String type=Type.getReturnType(method.desc).getDescriptor();String cast;
  47                  if(type.equals("Ljava/util/List;")||type.equals("Ljava/lang/Iterable;"))cast=type.substring(1,type.length()-1);
  48                  else if(type.equals("Lnet/minecraft/world/entity/Entity;"))cast="net/minecraft/world/entity/Entity";
  49                  else continue;
  50                  if((method.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))!=0)continue;
  51                  for(var instruction:method.instructions)if(instruction.getOpcode()==Opcodes.ARETURN) {
  52                      checked++;if(!filteredReturn(method,instruction,cast.equals("net/minecraft/world/entity/Entity")?"filterQuarantinedEntity":"filterQuarantinedResult",cast))return false;
  53                  }
  54              }
```

<a id="projection"></a>
### projection

来源：`src/main/java/dev/ronova/coremod/policy/RuntimeCapabilityProjection.java:1–26`；原文件 SHA-256：`c0c2920dddc4847947baa960e1805098531fc69f1eaa513636c61bf637b0d04b`。

```text
   1  package dev.ronova.coremod.policy;
   2  
   3  import dev.ronova.coremod.integrity.*;
   4  import java.util.Set;
   5  
   6  /** Connects measured support to policy presentation without granting new attack permissions. */
   7  public final class RuntimeCapabilityProjection {
   8      private RuntimeCapabilityProjection() {}
   9      public static StrengthPolicySnapshot apply(StrengthPolicySnapshot policy,EnforcementProof enforcement,CodeIntegrityProof code) {
  10          var result=policy;
  11          for(var key:Set.of(StrengthOptionKey.PROTECT_CODE_INTEGRITY,StrengthOptionKey.RUNTIME_CRITICAL_CLASS_WATCH,
  12              StrengthOptionKey.INTERCEPT_INSTRUMENTATION)) {
  13              result=result.withCapability(key,code.supported()?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,
  14                  code.current()?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,
  15                  code.current()?EvidenceState.VERIFIED:code.supported()?EvidenceState.FAILED:EvidenceState.UNOBSERVED,
  16                  code.current()?ActivationState.ACTIVE:code.supported()?ActivationState.FENCED:ActivationState.UNAVAILABLE);
  17          }
  18          for(var key:Set.of(StrengthOptionKey.RUNTIME_JVMTI,StrengthOptionKey.RUNTIME_NATIVE,StrengthOptionKey.RUNTIME_BACKGROUND_INTEGRITY)) {
  19              boolean verified=enforcement.current();
  20              result=result.withCapability(key,enforcement.supported()?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,
  21                  verified?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,verified?EvidenceState.VERIFIED:EvidenceState.UNOBSERVED,
  22                  verified?ActivationState.ACTIVE:ActivationState.UNAVAILABLE);
  23          }
  24          return result;
  25      }
  26  }
```

<a id="policy-ceiling"></a>
### policy-ceiling

来源：`src/main/java/dev/ronova/coremod/policy/StrengthPolicySnapshot.java:220–243`；原文件 SHA-256：`8881c5987ebe7e2af71332ed342e5c58f8678b95fd67b51f022032eb75833faf`。

```text
 220          ProfilePresetMatrix.Resolution activeResolution = ProfilePresetMatrix.resolve(
 221              activeMatrixRevision, activeProfile, option, activeLevels.get(option)
 222          );
 223          MechanismLevel activeRequested = activeResolution.requestedLevel();
 224          MechanismLevel supportedCeiling = MechanismLevel.runnable(
 225              supportedCeilings.get(option)
 226          );
 227          MechanismLevel evidenceCeiling = MechanismLevel.runnable(
 228              evidenceCeilings.get(option)
 229          );
 230          MechanismLevel effective = MechanismLevel.lowerOf(
 231              MechanismLevel.lowerOf(activeRequested, supportedCeiling),
 232              evidenceCeiling
 233          );
 234          EvidenceState evidence = evidenceStates.get(option);
 235          ActivationState activation = activationState(
 236              option, activeRequested, supportedCeiling,
 237              evidenceCeiling, evidence
 238          );
 239          return new StrengthResolutionReceipt(
 240              revision, configuredMatrixRevision, activeMatrixRevision,
 241              option, configured, activeResolution.presetLevel(), activeRequested,
 242              supportedCeiling, evidenceCeiling, effective, evidence, activation,
 243              activeResolution.minimumFloorApplied(),
```

<a id="recovery-policy"></a>
### recovery-policy

来源：`src/main/java/dev/ronova/coremod/policy/RecoveryChainPolicy.java:48–122`；原文件 SHA-256：`5ee8b9291b37a1727afd794d6f44f4110d6aa39ac352681fbc091cb10cb4450c`。

```text
  48      public static Decision resolve(StrengthPolicySnapshot policy, Step step) {
  49          Objects.requireNonNull(policy); Objects.requireNonNull(step);
  50          MechanismLevel requested = requested(policy, step);
  51          boolean observes = requested.rank() >= step.minimum().rank();
  52          if (!observes) return new Decision(step, requested, MechanismLevel.OFF, false, false,
  53              "OUTSIDE_ACTIVE_RECOVERY_DEPTH");
  54          var resolution = policy.resolveAttack(step.actions(), step.keys());
  55          boolean executes = resolution.permitted() && resolution.effectiveDepth().rank() >= step.minimum().rank();
  56          return new Decision(step, requested, resolution.effectiveDepth(), true, executes,
  57              executes ? "POLICY_PERMITTED_REQUIRES_CURRENT_SOURCE_AND_WRITE_LEASE"
  58                  : resolution.permitted() ? "RECOVERY_DEPTH_LIMITED_BY_DEPENDENCY" : resolution.reason());
  59      }
  60      private static MechanismLevel requested(StrengthPolicySnapshot policy, Step step) {
  61          MechanismLevel requested = MechanismLevel.STRICT_ALL;
  62          for (AttackAction action : step.actions()) requested = MechanismLevel.lowerOf(requested,
  63              policy.activeActions().resolve(policy.activeProfile(), action).depth());
  64          for (StrengthOptionKey key : step.keys()) requested = MechanismLevel.lowerOf(requested,
  65              ProfilePresetMatrix.resolve(policy.activeMatrixRevision(), policy.activeProfile(), key,
  66                  policy.activeLevels().get(key)).requestedLevel());
  67          return requested;
  68      }
  69      public static Map<Step, Decision> snapshot(StrengthPolicySnapshot policy) {
  70          var decisions = new EnumMap<Step, Decision>(Step.class);
  71          for (Step step : Step.values()) decisions.put(step, resolve(policy, step));
  72          return Collections.unmodifiableMap(decisions);
  73      }
  74      /** Exact static AtomicReference invalidation; this does not raise the general record or save ceiling. */
  75      public static Decision registeredAtomicSnapshot(StrengthPolicySnapshot policy, int copyDepth, boolean bindingVerified) {
  76          if (copyDepth < 0) throw new IllegalArgumentException("invalid copy depth");
  77          Step step = Step.RECORD_DISPOSAL;
  78          var requested = requested(policy, step);
  79          var minimum = step.minimum();
  80          if (copyDepth > 1) {
  81              requested = MechanismLevel.lowerOf(requested, requested(policy, Step.DERIVED_SOURCE));
  82              minimum = Step.DERIVED_SOURCE.minimum();
  83          }
  84          for (var key : NBT_GATE_KEYS) requested = MechanismLevel.lowerOf(requested,
  85              ProfilePresetMatrix.resolve(policy.activeMatrixRevision(), policy.activeProfile(), key,
  86                  policy.activeLevels().get(key)).requestedLevel());
  87          boolean observes = requested.rank() >= minimum.rank();
  88          boolean permitted = observes && !policy.pendingRestart() && bindingVerified;
  89          return new Decision(step, requested, permitted ? requested : MechanismLevel.OFF, observes, permitted,
  90              !observes ? "OUTSIDE_ACTIVE_RECOVERY_DEPTH" : policy.pendingRestart() ? "PENDING_RESTART"
  91                  : !bindingVerified ? "STATIC_ATOMIC_BINDING_UNVERIFIED" : "PERMITTED_FOR_THIS_STATIC_ATOMIC_SNAPSHOT_ONLY");
  92      }
  93      /** Scopes capability evidence to the registered NBT outlet; it is never published as a global key ceiling. */
  94      public static Decision registeredNbtOutlet(StrengthPolicySnapshot policy, int copyDepth, boolean boundaryVerified) {
  95          Step step = copyDepth > 1 ? Step.DERIVED_SOURCE : Step.REBUILD_OUTLET;
  96          if (copyDepth < 0) throw new IllegalArgumentException("invalid copy depth");
  97          var requested = requested(policy, step);
  98          for (var key : NBT_GATE_KEYS) requested = MechanismLevel.lowerOf(requested,
  99              ProfilePresetMatrix.resolve(policy.activeMatrixRevision(), policy.activeProfile(), key,
 100                  policy.activeLevels().get(key)).requestedLevel());
 101          boolean observes = requested.rank() >= step.minimum().rank();
 102          boolean permitted = observes && !policy.pendingRestart() && boundaryVerified;
 103          return new Decision(step, requested, permitted ? requested : MechanismLevel.OFF, observes, permitted,
 104              !observes ? "OUTSIDE_ACTIVE_RECOVERY_DEPTH" : policy.pendingRestart() ? "PENDING_RESTART"
 105                  : !boundaryVerified ? "REGISTERED_NBT_BOUNDARY_UNVERIFIED" : "PERMITTED_FOR_THIS_OBSERVED_NBT_SOURCE_ONLY");
 106      }
 107      public static Decision registeredTaskDisposition(StrengthPolicySnapshot policy,int copyDepth,boolean bindingVerified) {
 108          return registeredWork(policy,copyDepth>1?Step.DESCENDANT_TASK:Step.DIRECT_TASK,copyDepth,bindingVerified);
 109      }
 110      public static Decision registeredStorageCommit(StrengthPolicySnapshot policy,int copyDepth,boolean bindingVerified) {
 111          return registeredWork(policy,copyDepth>1?Step.RELOAD_RECONSTRUCTION:Step.SAVED_RECORD_DISPOSAL,copyDepth,bindingVerified);
 112      }
 113      private static Decision registeredWork(StrengthPolicySnapshot policy,Step step,int copyDepth,boolean bindingVerified) {
 114          if(copyDepth<0)throw new IllegalArgumentException("invalid copy depth");
 115          var requested=requested(policy,step);
 116          for(var key:NBT_GATE_KEYS)requested=MechanismLevel.lowerOf(requested,
 117              ProfilePresetMatrix.resolve(policy.activeMatrixRevision(),policy.activeProfile(),key,policy.activeLevels().get(key)).requestedLevel());
 118          boolean observes=requested.rank()>=step.minimum().rank(),permitted=observes&&!policy.pendingRestart()&&bindingVerified;
 119          return new Decision(step,requested,permitted?requested:MechanismLevel.OFF,observes,permitted,
 120              !observes?"OUTSIDE_ACTIVE_RECOVERY_DEPTH":policy.pendingRestart()?"PENDING_RESTART":!bindingVerified?"RECOVERY_WORK_BINDING_UNVERIFIED":"PERMITTED_FOR_THIS_REGISTERED_RECOVERY_WORK_ONLY");
 121      }
 122  }
```

<a id="recovery-approval"></a>
### recovery-approval

来源：`src/main/java/dev/ronova/coremod/persistence/ShadowDurableRootStore.java:769–794`；原文件 SHA-256：`b7a492bc2b36a48303d3e86e568214442fb537a2fc2f95426d760a91d258af90`。

```text
 769      /** Read-only conditional permission derived from the already committed terminal command. */
 770      public synchronized dev.ronova.coremod.root.api.RecoverySourcePlan approveRecoverySourcePlan(
 771          dev.ronova.coremod.root.api.TerminalOperationPlan terminal,
 772          dev.ronova.coremod.root.api.RecoverySourceEvidence evidence) {
 773          if (!authoritative || readOnly || persistencePending || terminal == null || evidence == null || !evidence.fresh(System.nanoTime())) return null;
 774          var current = approveTerminalPlan(terminal.subjectKey().subjectId(), terminal.physicalNonce());
 775          if (current == null || !current.qualification().equals(terminal.qualification())
 776              || !current.commandId().equals(terminal.commandId()) || !current.authorityHash().equals(terminal.authorityHash())
 777              || current.authorityCommitIndex() != terminal.authorityCommitIndex()
 778              || !evidence.authority().sameAuthority(authorityStamp(current.subjectKey().subjectId()))) return null;
 779          var direct = dev.ronova.coremod.policy.RecoveryChainPolicy.registeredNbtOutlet(strengthPolicySnapshot(), evidence.copyDepth(), true);
 780          if (!direct.executionPermitted()) return null;
 781          var derived = dev.ronova.coremod.policy.RecoveryChainPolicy.registeredNbtOutlet(strengthPolicySnapshot(), 2, true);
 782          return new dev.ronova.coremod.root.api.RecoverySourcePlan(current, evidence.sourceLocator(),
 783              derived.executionPermitted() ? Integer.MAX_VALUE : 1, dev.ronova.coremod.root.api.RecoverySourcePlan.CONTRACT);
 784      }
 785      /** Independent record-write permission; disabling the rebuild outlet does not disable a permitted direct record action. */
 786      public synchronized dev.ronova.coremod.root.api.RecoverySnapshotPlan approveStaticSnapshotPlan(
 787          dev.ronova.coremod.root.api.TerminalOperationPlan terminal,
 788          dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, long sourceVersion, boolean bindingVerified) {
 789          if (!authoritative || readOnly || persistencePending || !bindingVerified || terminal == null || evidence == null
 790              || sourceVersion < 1 || sourceVersion > evidence.sourceRevision() || !evidence.fresh(System.nanoTime())) return null;
 791          var current = approveTerminalPlan(terminal.subjectKey().subjectId(), terminal.physicalNonce());
 792          if (current == null || !current.qualification().equals(terminal.qualification())
 793              || !current.commandId().equals(terminal.commandId()) || !current.authorityHash().equals(terminal.authorityHash())
 794              || current.authorityCommitIndex() != terminal.authorityCommitIndex()
```

<a id="recovery-held"></a>
### recovery-held

来源：`src/main/java/dev/ronova/coremod/persistence/ShadowDurableRootStore.java:1153–1199`；原文件 SHA-256：`b7a492bc2b36a48303d3e86e568214442fb537a2fc2f95426d760a91d258af90`。

```text
1153      public synchronized dev.ronova.coremod.root.api.RecoverySourcePlan qualifyRecoverySource(
1154          dev.ronova.coremod.root.api.TerminalOperationPlan terminal,
1155          dev.ronova.coremod.root.api.RecoverySourceEvidence evidence) {
1156          if (!authoritative || readOnly || terminal == null || evidence == null || !evidence.fresh(System.nanoTime())) return null;
1157          if (persistencePending && !persistAuthoritative()) return null;
1158          var plan = approveRecoverySourcePlan(terminal, evidence);
1159          if (plan == null) return null;
1160          var existing = effectJournal.get(plan.effectKey());
1161          if (existing != null) return existing.state() == EffectRecord.State.EFFECT_CONFIRMED ? plan : null;
1162          var record = EffectRecord.sourceQualification(plan);
1163          // A migrated image without retained policy provenance may be observed, but cannot mint an unverifiable durable row.
1164          if (!commandJournal.policyAt(record.planRevision()).equals(activeStrengthPolicyConfiguration)) return null;
1165          var nextEffects = effectJournal.copy(); nextEffects.put(record);
1166          DurableRootFileBackend.PreparedWrite prepared;
1167          try {
1168              CompoundTag image = writeImage(new CompoundTag(), snapshot, lifecycleReceipts.values(),
1169                  strengthPolicyConfiguration, qualifications, commandJournal, nextEffects);
1170              image.putBoolean("persistence_pending", false);
1171              prepared = preflight(image, nextEffects, true);
1172          } catch (StorageCapacityException | DurableRootPersistenceException blocked) {
1173              storageRejections++; persistenceFailure = normalizedFailure(blocked); return null;
1174          }
1175          effectJournal = nextEffects; persistencePending = true; setDirty();
1176          return persistAuthoritative(prepared) ? plan : null;
1177      }
1178      /** Read-only final outlet decision; an old plan cannot survive clear, Owner promotion or policy change. */
1179      public synchronized boolean recoverySourceHeld(dev.ronova.coremod.root.api.RecoverySourcePlan plan,
1180          int copyDepth, UUID proposedSubject) {
1181          if (plan == null || !authoritative || readOnly || persistencePending || !plan.coversDepth(copyDepth)
1182              || proposedSubject != null && ownerRegistry.contains(proposedSubject)) return false;
1183          var prior = plan.terminal();
1184          var current = approveTerminalPlan(prior.subjectKey().subjectId(), prior.physicalNonce());
1185          if (current == null || !current.qualification().equals(prior.qualification())
1186              || !current.commandId().equals(prior.commandId()) || !current.authorityHash().equals(prior.authorityHash())
1187              || current.activePolicyRevision() != prior.activePolicyRevision()
1188              || current.configuredPolicyRevision() != prior.configuredPolicyRevision()) return false;
1189          var record = effectJournal.get(plan.effectKey());
1190          return record != null && record.state() == EffectRecord.State.EFFECT_CONFIRMED
1191              && record.authorityPayloadHash().equals(current.authorityHash())
1192              && dev.ronova.coremod.policy.RecoveryChainPolicy.registeredNbtOutlet(strengthPolicySnapshot(), copyDepth, true).executionPermitted();
1193      }
1194      /** Holds a freshly proved result without waiting for source metadata I/O or allowing a probe revival. */
1195      public synchronized boolean recoverySourceHeld(dev.ronova.coremod.root.api.TerminalOperationPlan terminal,
1196          dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, UUID proposedSubject) {
1197          return !(proposedSubject != null && ownerRegistry.contains(proposedSubject))
1198              && approveRecoverySourcePlan(terminal, evidence) != null;
1199      }
```

<a id="reconstruction-caller"></a>
### reconstruction-caller

来源：`src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java:853–876`；原文件 SHA-256：`1f357b8dc9c884b11baf42cd4941fc861a071ce77ce65f2be461e86cbaaeccd0`。

```text
 853              if (stamp == null || !stamp.settled() || stamp.owner() || !source.terminal.qualification().equals(stamp.qualification())) return false;
 854              // A seed sent directly to a factory also proves use; copy observation is not fabricated.
 855              source.witnessRevision = next(version); version = source.witnessRevision;
 856              witnessed = next(witnessed);
 857              // Historical coverage debt remains in Root, but does not disable a newly
 858              // proved edge. Unobserved/ambiguous copies cannot pass the resolution checks above.
 859              boolean boundary=RecoveryBoundaryProof.reconstruction();
 860              boolean nativeCurrent=observation!=null&&observation.stillCurrent();
 861              lastReconstructionProof=Map.of("copy_verified",resolved.evidenceVerified()?1L:0L,"boundary_verified",boundary?1L:0L,
 862                  "observation_open",observation!=null?1L:0L,"native_current",nativeCurrent?1L:0L,"depth",(long)resolved.copyDepth());
 863              if(boundary)lastBootstrapReconstructionProof=lastReconstructionProof;
 864              boolean verified = resolved.evidenceVerified() && boundary && nativeCurrent;
 865              var candidate = new RecoverySourceEvidence(source.key.locator(), stamp, source.witnessRevision,
 866                  resolved.copyDepth(), verified, System.nanoTime());
 867              UUID proposedId = tag.hasUUID("UUID") ? tag.getUUID("UUID") : null;
 868              // Source identity is checked again immediately before the read-only Root outlet decision.
 869              boolean held=verified
 870                  &&dev.ronova.coremod.policy.RecoveryChainPolicy.registeredNbtOutlet(policy(), resolved.copyDepth(), true).executionPermitted()
 871                  &&(resolved.copyDepth()>0||!source.detached&&source.stillBound())
 872                  &&runtime.durableStore().recoverySourceHeld(source.terminal,candidate,proposedId);
 873              verified=verified&&observation.finish();
 874              // Only publish an attestation after the complete read/decision observation survived.
 875              // A later successful Native cycle cannot turn this attempt's failed proof into a live witness.
 876              source.evidence=new RecoverySourceEvidence(source.key.locator(),stamp,source.witnessRevision,
```

<a id="qualification-caller"></a>
### qualification-caller

来源：`src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java:1119–1140`；原文件 SHA-256：`1f357b8dc9c884b11baf42cd4941fc861a071ce77ce65f2be461e86cbaaeccd0`。

```text
1119              try { source.release.run(); } catch(RuntimeException error) {
1120                  if(failure==null)failure=error;else if(failure!=error)failure.addSuppressed(error);
1121              }
1122              if(failure!=null)throw failure;
1123          } else {
1124              if(source.memoryDisposition!=null) { advanceDisposedStorage(source); return; }
1125              var evidence = source.evidence;
1126              if (!current(source.terminal.qualification()) || evidence == null || !evidence.fresh(System.nanoTime())
1127                  || !runtime.enforcement().observesRegisteredSemantics(0x6000)) return;
1128              if (!queueRecord(source, evidence)) clearSnapshot(source, evidence);
1129              if (source.plan != null && runtime.durableStore().recoverySourceHeld(source.plan, evidence.copyDepth(), null)) return;
1130              // Durable I/O happens in the budgeted maintenance lane, never in the factory gate.
1131              var approved = runtime.durableStore().qualifyRecoverySource(source.terminal, evidence);
1132              if (approved != null && (!source.detached&&source.stillBound()||source.detached&&evidence.copyDepth()>0)
1133                  && current(source.terminal.qualification())) source.plan = approved;
1134          }
1135      }
1136      /** Queue only direct records in registered concurrent maps. World checks stay on the server thread. */
1137      private boolean queueRecord(Source source, RecoverySourceEvidence evidence) {
1138          var existing = recordTasks.get(source.key);
1139          if (existing != null) return existing.task.active();
1140          if (recordTasks.size() >= SOURCE_LIMIT) {
```

<a id="registry"></a>
### registry

来源：`src/main/java/dev/ronova/coremod/bootstrap/ServerRuntimeRegistry.java:16–34`；原文件 SHA-256：`445ecc6a58791304f5937c1d6ccad2852ddc8c5dfef32f630d5c18b4f25ff58c`。

```text
  16  /** Process registry for explicitly composed per-server runtimes. */
  17  public final class ServerRuntimeRegistry {
  18      private static final StartupRegistry<MinecraftServer, DominionServerRuntime> SERVERS = new StartupRegistry<>();
  19  
  20      private ServerRuntimeRegistry() {}
  21  
  22      public static DominionServerRuntime runtime(MinecraftServer server) {
  23          if (server == null) {
  24              throw new IllegalArgumentException("server is required");
  25          }
  26          synchronized (SERVERS) {
  27              return SERVERS.getOrCreate(server, DominionCompositionRoot::create,
  28                  value -> releaseResources(value, null));
  29          }
  30      }
  31  
  32      /** Lookup only: cleanup and diagnostics must never initialize a runtime. */
  33      public static DominionServerRuntime getIfPresent(MinecraftServer server) {
  34          return server == null ? null : SERVERS.get(server);
```

<a id="registry-release"></a>
### registry-release

来源：`src/main/java/dev/ronova/coremod/bootstrap/ServerRuntimeRegistry.java:120–174`；原文件 SHA-256：`445ecc6a58791304f5937c1d6ccad2852ddc8c5dfef32f630d5c18b4f25ff58c`。

```text
 120  
 121      public static void invalidateAdmissionPasses() {
 122          synchronized (SERVERS) {
 123              SERVERS.values().forEach(value -> value.spawnAdmission().invalidatePasses());
 124          }
 125      }
 126  
 127      public static void clearAdmissionIdentity(UUID uuid) {
 128          if (uuid == null) {
 129              return;
 130          }
 131          synchronized (SERVERS) {
 132              SERVERS.values().forEach(value -> value.spawnAdmission().clearIdentity(uuid));
 133          }
 134      }
 135  
 136      public static void release(MinecraftServer server) {
 137          if (server == null) {
 138              return;
 139          }
 140          DominionServerRuntime runtime = SERVERS.remove(server);
 141          releaseResources(server, runtime);
 142      }
 143  
 144      private static void releaseResources(MinecraftServer server, DominionServerRuntime runtime) {
 145          java.util.List<Runnable> cleanup = new java.util.ArrayList<>();
 146          cleanup.add(() -> dev.ronova.coremod.runtime.RuntimeCoordinator.release(server));
 147          if (runtime != null) {
 148              cleanup.add(() -> runtime.authorityState().beginShutdown());
 149              cleanup.add(() -> runtime.ingressWork().close());
 150              cleanup.add(() -> runtime.recoverySources().flushShutdownResults());
 151              cleanup.add(() -> runtime.obligations().flushCheckpoint());
 152              cleanup.add(() -> runtime.terminalWatch().clear());
 153              cleanup.add(() -> runtime.observation().clear());
 154              cleanup.add(() -> runtime.spawnAdmission().close());
 155              cleanup.add(runtime::clearIncarnations);
 156              cleanup.add(() -> runtime.terminalPlans().clear());
 157              cleanup.add(() -> runtime.recoverySources().close());
 158              cleanup.add(() -> runtime.obligations().clear());
 159              cleanup.add(() -> runtime.authorityState().release());
 160          }
 161          cleanup.add(() -> AuthorityIdentity.clearServer(server));
 162          cleanup.add(() -> AuthorityView.release(server));
 163          cleanup.add(() -> ProjectionCoordinator.release(server));
 164          cleanup.add(() -> LocalDominionStore.release(server));
 165          Throwable failed = null;
 166          for (Runnable step : cleanup) {
 167              try { step.run(); }
 168              catch (RuntimeException | Error error) {
 169                  if (failed == null) failed = error; else if (failed != error) failed.addSuppressed(error);
 170              }
 171          }
 172          if (failed instanceof RuntimeException error) throw error;
 173          if (failed instanceof Error error) throw error;
 174      }
```

<a id="runtime-accessors"></a>
### runtime-accessors

来源：`src/main/java/dev/ronova/coremod/runtime/DominionServerRuntime.java:156–230`；原文件 SHA-256：`1063602985d85d9ccac59c99c948b23b9627893cbe931a289f382cd2c5593911`。

```text
 156      public RootCommandBus commands() { return commands; }
 157      public UUID incarnationNonce(Object entity) { return spawnAdmission.incarnationNonce(entity); }
 158      public void clearIncarnations() { spawnAdmission.clearIncarnations(); }
 159      public dev.ronova.coremod.budget.HostResourceGovernor resources() { return spawnAdmission.resources(); }
 160      public dev.ronova.coremod.surface.RuntimeDiagnostics diagnostics() { return diagnostics; }
 161      public dev.ronova.coremod.engine.TerminalPlanExecutor terminalPlans() { return terminalPlans; }
 162      public dev.ronova.coremod.obligation.ObligationCoordinator obligations() { return obligations; }
 163      public dev.ronova.coremod.observation.RecoverySourceTracker recoverySources() { return recoverySources; }
 164      public IngressWorkRegistry<net.minecraft.network.Connection> ingressWork() { return ingressWork; }
 165  
 166      /** Pure Authority query view for this server's current projection. */
 167      public AuthorityView authorityView() { return authorityView; }
 168  
 169      public boolean authoritySplitEnabled() { return authoritySplitSelection.enabled(); }
 170  
 171      public AuthoritySplitFlags.Source authoritySplitFlagSource() {
 172          return authoritySplitSelection.source();
 173      }
 174  
 175      public String authoritySplitMode() {
 176          return authoritySplitEnabled() ? "SPLIT" : "LEGACY";
 177      }
 178  
 179      public ProjectionBundle projection() { return projections.published(); }
 180  
 181      public ProjectionCoordinator projectionCoordinator() { return projections; }
 182  
 183      public AuthorityRuntimeState authorityState() { return authorityState; }
 184  
 185      public CapabilityRuntime capabilities() { return capabilities; }
 186  
 187      public AuthorityEnforcement authorityEnforcement() { return authorityEnforcement; }
 188  
 189      public SpawnAdmissionRuntime spawnAdmission() { return spawnAdmission; }
 190  
 191      public EnforcementRuntime enforcement() { return enforcement; }
 192  
 193      public TopologyObservation observation() { return observation; }
 194  
 195      public TerminalWatchSet terminalWatch() { return terminalWatch; }
 196  
 197      public FrameBudgetGovernor frameBudgetGovernor() { return frameBudgetGovernor; }
 198  
 199      /** The authoritative SavedData adapter used by the composed server runtime. */
 200      public LocalDominionStore durableStore() { return durableStore; }
 201  
 202      /**
 203       * Coherent configured/active strength view. A configured change remains
 204       * PENDING_RESTART until the durable Root is loaded by the next JVM.
 205       */
 206      public StrengthPolicySnapshot strengthPolicy() {
 207          return durableStore == null
 208              ? StrengthPolicySnapshot.defaults()
 209              : dev.ronova.coremod.policy.RuntimeCapabilityProjection.apply(durableStore.strengthPolicySnapshot(),enforcement.published(),enforcement.codeIntegrity());
 210      }
 211  
 212      /** Claims once-only lifecycle evidence without making the runtime a second writer. */
 213      public boolean claimLifecycleIntegration(UUID judgmentId, String physicalIdentity) {
 214          return durableStore != null
 215              && durableStore.claimLifecycleIntegration(judgmentId, physicalIdentity) != null;
 216      }
 217  
 218      /** Executes outside the Root monitor after durable intent and the one-attempt entry check. */
 219      public boolean integrateLifecycleOnce(UUID subjectId, String physicalIdentity, Runnable body) {
 220          if (durableStore == null || body == null) return false;
 221          dev.ronova.coremod.root.api.EffectClaim claim = durableStore.prepareLifecycleEffect(subjectId, physicalIdentity);
 222          if (claim.status() == dev.ronova.coremod.root.api.EffectClaim.Status.LEGACY_UNQUALIFIED) {
 223              if (!claimLifecycleIntegration(subjectId, physicalIdentity)) return false;
 224              body.run();
 225              return true;
 226          }
 227          if (!claim.granted()) return false;
 228          try {
 229              if (!durableStore.enterEffect(claim)) return false;
 230              body.run();
```

<a id="admission-invalidate"></a>
### admission-invalidate

来源：`src/main/java/dev/ronova/coremod/engine/spawn/SpawnAdmissionRuntime.java:334–337`；原文件 SHA-256：`361a2ce8d88ca1913ada2e8764356878f090e83030c537645730ae5aa2c61bcb`。

```text
 334      public void invalidatePasses() {
 335          passed.clear();
 336          passes.clear();
 337      }
```

<a id="admission-clear"></a>
### admission-clear

来源：`src/main/java/dev/ronova/coremod/engine/spawn/SpawnAdmissionRuntime.java:449–461`；原文件 SHA-256：`361a2ce8d88ca1913ada2e8764356878f090e83030c537645730ae5aa2c61bcb`。

```text
 449      public void clearIdentity(UUID uuid) {
 450          if (uuid == null) {
 451              return;
 452          }
 453          if(classLineages!=null)classLineages.forget(uuid);
 454          passed.removeIf(entity -> uuid.equals(entity.getUUID()));
 455          passes.removeIf(entity -> uuid.equals(entity.getUUID()));
 456          rejected.removeIf(entity -> uuid.equals(entity.getUUID()));
 457          trackedConstruction.removeIf(entity -> uuid.equals(entity.getUUID()));
 458          trackedFactoryResults.removeIf(entity -> uuid.equals(entity.getUUID()));
 459          constructed.removeIf(work -> uuid.equals(work.uuid()));
 460          terminalWatch.clearIdentity(uuid);
 461      }
```

<a id="fence"></a>
### fence

来源：`src/main/java/dev/ronova/coremod/root/api/RootExecutionFence.java:8–73`；原文件 SHA-256：`05a9c0bbe81411e0cee97383e61e347c4a7819ccf9e890a21728e63b6e61da7d`。

```text
   8      public static final long MAX_DURATION_NANOS=250_000_000L;
   9      private final LongSupplier clock;
  10      private Lease active;
  11      public RootExecutionFence() { this(System::nanoTime); }
  12      RootExecutionFence(LongSupplier clock) { this.clock=Objects.requireNonNull(clock); }
  13      public synchronized Lease tryAcquire(TerminalOperationPlan plan) {
  14          Objects.requireNonNull(plan);
  15          if(active!=null)return null;
  16          active=new Lease(plan,clock.getAsLong());return active;
  17      }
  18      public synchronized Lease tryAcquireOwner(AuthorityStamp authority) {
  19          Objects.requireNonNull(authority);
  20          if(active!=null||!authority.owner()||!authority.settled())return null;
  21          active=new Lease(authority,clock.getAsLong());return active;
  22      }
  23      public synchronized boolean busy() { return active!=null; }
  24      public synchronized boolean ownsStorage(Lease lease,RecoveryStoragePlan plan) {
  25          return lease!=null&&active==lease&&lease.valid(plan);
  26      }
  27      public synchronized Lease tryAcquireRecord(RecoveryRecordPlan plan) {
  28          Objects.requireNonNull(plan);
  29          if(active!=null)return null;
  30          active=new Lease(plan,clock.getAsLong());return active;
  31      }
  32      public synchronized Lease tryAcquireSnapshot(RecoverySnapshotPlan plan) {
  33          Objects.requireNonNull(plan);
  34          if(active!=null)return null;
  35          active=new Lease(plan,clock.getAsLong());return active;
  36      }
  37      public synchronized Lease tryAcquireTask(RecoveryTaskPlan plan) {
  38          Objects.requireNonNull(plan);if(active!=null)return null;active=new Lease(plan,clock.getAsLong());return active;
  39      }
  40      public synchronized Lease tryAcquireStorage(RecoveryStoragePlan plan) {
  41          Objects.requireNonNull(plan);if(active!=null)return null;active=new Lease(plan,clock.getAsLong());return active;
  42      }
  43  
  44      public final class Lease implements AutoCloseable {
  45          private final Object scope;
  46          private final long issued;
  47          private final Thread owner=Thread.currentThread();
  48          private boolean revoked;
  49          private Lease(Object scope,long issued) { this.scope=scope;this.issued=issued; }
  50          public boolean valid(TerminalOperationPlan expected) {return validScope(expected);}
  51          public boolean valid(AuthorityStamp expected) {return validScope(expected);}
  52          public boolean valid(RecoverySnapshotPlan expected) {return validScope(expected);}
  53          public boolean valid(RecoveryRecordPlan expected) {return validScope(expected);}
  54          public boolean valid(RecoveryTaskPlan expected) {return validScope(expected);}
  55          public boolean valid(RecoveryStoragePlan expected) {return validScope(expected);}
  56          private boolean validScope(Object expected) {
  57              synchronized(RootExecutionFence.this) {
  58                  long age=clock.getAsLong()-issued;
  59                  if(active!=this||revoked)return false;
  60                  if(Thread.currentThread()!=owner||!scope.equals(expected)||age<0||age>=MAX_DURATION_NANOS){revoked=true;return false;}
  61                  return true;
  62              }
  63          }
  64          @Override public void close() {
  65              synchronized(RootExecutionFence.this) {
  66                  if(Thread.currentThread()!=owner)throw new IllegalStateException("Root execution fence belongs to another thread");
  67                  if(active==this)active=null;
  68              }
  69          }
  70      }
  71      // Expiry revokes further writes but never forcibly releases a possibly paused
  72      // writer. The owning try/finally must leave the step before a new commit enters.
  73  }
```

<a id="durable-checksum"></a>
### durable-checksum

来源：`src/main/java/dev/ronova/coremod/persistence/DurableRootFileBackend.java:96–139`；原文件 SHA-256：`f8e9c946e54649ad2a52f0db1e9a290995f996c67aea690f17083c2020004e51`。

```text
  96          }
  97  
  98          try (var permit=filePermit(); DataInputStream input = new DataInputStream(
  99              new BufferedInputStream(Files.newInputStream(target))
 100          )) {
 101              int magic = input.readInt();
 102              int version = input.readInt();
 103              int payloadLength = input.readInt();
 104              if (magic != MAGIC) {
 105                  throw new DurableRootPersistenceException("durable Root magic mismatch");
 106              }
 107              if (version != FORMAT_VERSION) {
 108                  throw new DurableRootPersistenceException(
 109                      "unsupported durable Root file version: " + version
 110                  );
 111              }
 112              if (payloadLength < 0 || payloadLength > MAX_PAYLOAD_BYTES
 113                  || fileSize != HEADER_BYTES + (long) payloadLength) {
 114                  throw new DurableRootPersistenceException(
 115                      "durable Root payload length is invalid"
 116                  );
 117              }
 118              byte[] expectedDigest = new byte[DIGEST_BYTES];
 119              input.readFully(expectedDigest);
 120              byte[] payload = new byte[payloadLength];
 121              input.readFully(payload);
 122              if (!MessageDigest.isEqual(expectedDigest, digest(payload))) {
 123                  throw new DurableRootPersistenceException(
 124                      "durable Root payload checksum mismatch"
 125                  );
 126              }
 127              CompoundTag root = NbtIo.read(new DataInputStream(
 128                  new ByteArrayInputStream(payload)
 129              ));
 130              if (root == null) {
 131                  throw new DurableRootPersistenceException("durable Root NBT is empty");
 132              }
 133              return root;
 134          } catch (DurableRootPersistenceException failure) {
 135              throw failure;
 136          } catch (IOException | RuntimeException failure) {
 137              throw failure("cannot decode durable Root file", failure);
 138          }
 139      }
```

<a id="native-hash"></a>
### native-hash

来源：`native/src/storage_transaction.hpp:86–96`；原文件 SHA-256：`b283ba62b7cdbff070e30baad910e2b3fde7428c7ba28de9376d2843dba0bc26`。

```text
  86  struct Hash {
  87      BCRYPT_ALG_HANDLE algorithm=nullptr;BCRYPT_HASH_HANDLE handle=nullptr;
  88      ~Hash(){if(handle)BCryptDestroyHash(handle);if(algorithm)BCryptCloseAlgorithmProvider(algorithm,0);}
  89      bool open(){return BCryptOpenAlgorithmProvider(&algorithm,BCRYPT_SHA256_ALGORITHM,nullptr,0)>=0
  90          &&BCryptCreateHash(algorithm,&handle,nullptr,0,nullptr,0,0)>=0;}
  91      bool add(const unsigned char* bytes,ULONG size){return BCryptHashData(handle,const_cast<PUCHAR>(bytes),size,0)>=0;}
  92      bool finish(std::string& out){unsigned char digest[32];if(BCryptFinishHash(handle,digest,32,0)<0)return false;
  93          static const char hex[]="0123456789abcdef";out.clear();for(auto b:digest){out+=hex[b>>4];out+=hex[b&15];}return true;}
  94  };
  95  static bool hash_file(HANDLE file,std::string& hash,ULONGLONG deadline) {
  96      LARGE_INTEGER zero{};if(!SetFilePointerEx(file,zero,nullptr,FILE_BEGIN))return false;
```

<a id="seal-limits"></a>
### seal-limits

来源：`native/code_seal_inventory.py:1–36`；原文件 SHA-256：`cce9eba2ba9b5303cf5cc6992227719771fb8d94d9c55374957e5a5a1adef07e`。

```text
   1  """Bounded, complete selection of build-defined code images from a production JAR."""
   2  from collections import Counter
   3  
   4  # Phase-one storage adds required top-level/nested images beyond 256. Native uses the generated array length.
   5  # Keep a finite count ceiling and the unchanged 4 MiB aggregate image limit; never omit required families.
   6  MAX_CLASSES = 512
   7  MAX_TOTAL_BYTES = 4 * 1024 * 1024
   8  
   9  
  10  def select_registered_images(source, prefixes):
  11      prefixes = tuple(prefixes)
  12      if not prefixes or len(set(prefixes)) != len(prefixes):
  13          raise ValueError("Code seals require a nonempty unique prefix inventory")
  14      entries = sorted((entry for entry in source.infolist()
  15          if entry.filename.endswith('.class') and any(
  16              entry.filename == prefix + '.class' or entry.filename.startswith(prefix + '$')
  17              for prefix in prefixes)), key=lambda entry: entry.filename[:-6])
  18      names = Counter(entry.filename for entry in entries)
  19      duplicates = sorted(name for name, count in names.items() if count != 1)
  20      if duplicates:
  21          raise ValueError("Duplicate registered class entries: " + ', '.join(duplicates))
  22      missing = sorted(prefix for prefix in prefixes if prefix + '.class' not in names)
  23      if missing:
  24          raise ValueError("Missing required registered classes: " + ', '.join(missing))
  25      total = sum(entry.file_size for entry in entries)
  26      if not 1 <= len(entries) <= MAX_CLASSES or not 0 < total <= MAX_TOTAL_BYTES:
  27          raise ValueError(f"Code seal inventory exceeds its budget: {len(entries)}/{MAX_CLASSES} classes, "
  28              f"{total}/{MAX_TOTAL_BYTES} bytes")
  29      images = {}
  30      used = 0
  31      for entry in entries:
  32          # Bound decompression too; a metadata estimate must not authorize an unbounded read.
  33          with source.open(entry) as stream:
  34              image = stream.read(MAX_TOTAL_BYTES - used + 1)
  35          used += len(image)
  36          if used > MAX_TOTAL_BYTES or len(image) != entry.file_size:
```

