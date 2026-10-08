# R66 · 八方向融合的独立源码复核依据（fix10）

本页与 [Ronova 侧原核验稿](R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md)分开。原稿采用 grep/read、报告 shell 故障；本页在收到的原 r66 ZIP 上独立静态读取，不复现其工作区，也没有 Java/Native 编译、JVM/游戏或能力实测。日期：2026-09-15。

[当前融合规划](../plans/R66-CAPABILITY-WIRING-PLAN.md#eight-directions) · [八条回执](../plans/RESTRUCTURE-REVIEW.md#review-fix10-eight) · [逐节来源映射](../plans/RESTRUCTURE-MAP.md#fix10-eight-merge)

<a id="findings"></a>
## 复核结论与来源差异

| 方向 | 原核验稿支持的基础 | 独立补读与本轮采用的边界 |
|---|---|---|
| S1 | `CodeSealProbe`、重转换/语义周期和桥接验证已有 | `reassert_core_targets` 枚举的是 `SEMANTIC_TARGETS` 的 Minecraft 类，不是 Ronova-owned canary；自有镜像另有 `reassert_registered_code`。能复用机制，不等于四种隔离挑战已经完整存在或实测成功。 |
| S2 | `RecoveryMutationIntent` 有种类、effectKey、prepared/receipt，没有该通用写集协议 | 已有 plan/lease/qualification/slot 都是边界基础；不能说现有任何越界防护都不存在。声明是约束元数据，执行端口执行了约束并有结果，才构成相应能力的行为证据。 |
| S3 | 观察位累积，`WRITE_UNVERIFIED` 不被后来的 observed 清除 | 位掩码不是四态流程：`WRITE_OBSERVED | NOT_EXECUTED` 可以同时存在；`WRITE_OBSERVED | WRITE_UNVERIFIED` 不认可。必须保留不确定性累积和 memory-only 范围，不逐项改名为 REQUESTED/EXECUTED/OBSERVED/VERIFIED。 |
| S4 | 原稿找到 `BootstrapTaskHooks.droppedEvents()`，认为仅单点计数 | `RecoveryTaskObserver.merge` 已消费 `LOST`、dropped、proof changes 并调用 `graph.evidenceLost()`；已有 inbox-full 等原因、局部丢弃诊断、顺序屏障。`TaskExecutionGraph` 将受影响登记标失证并使 `executionsExited()` 不成立。缺口是跨源、代际/序列、scope化传播及重建，不是从零建立丢失后失证。 |
| A1 | 两个已有 Graph 不是通用能力证据依赖图 | `CompletionTracker` 已比较依赖快照、revision 和采样缺口；复用这类失效模板，补反向依赖索引，而不是说运行时完全没有依赖检查。不能从几个新名字零命中证明全树语义缺失。 |
| A2 | 已有 `CODE_SEAL_BUILD_ID` 比对 | `NativeBridge.bootstrap` 的返回值、`CAP_*` 位、`capabilityMask()` 也已有；缺少 featureMask 同名字段不等于没有掩码。构建 ID 在可选 code-seal 开启路径，Java 当前没有消费 `enableCodeSeals0` 返回值；不扩大成全部 Native 协议已握手。 |
| A3 | `ShadowVerification` 有10个枚举结果及 accepted/matches/rejected | 保留 VERIFIED 与 MIRRORED/RECEIPT_MIRRORED 的差异；它是 SavedData→shadow 的专属结果，推广比较纪律，不原封移植领域词汇、不改变旧持久化结果。 |
| A4 | ActivationState 六值，EvidenceState 五值且含 DEGRADED | 还有 per-key map、状态解析与“active with degraded evidence”理由，不只是枚举声明。但枚举/解析不证明熔断的阈值、失证传播、恢复门、并发或序列化兼容已经完成；不采用“只加1–2值/低一数量级”工时承诺。 |

原稿§0的表格、汇总句与聊天摘要对 A2/A3 的归类不完全一致。原字节均保留；本轮逐方向给回执，不重新发布“六条/两条”的评分类结论。

<a id="scope-notes"></a>
## 设计推论（不是原作者实测）

S1一次挑战只能支撑记录的对象/加载器/代际/出口与时间范围，不是同权限 JVM 内不可伪造的安全根。S2的 declared-vs-actual 比对只覆盖已纳入的真实端口；未知出口和未捕获副作用必须报告未知，不声称监测任意内存写入。S3见证路径与执行路径分离减少同一路径自证，不证明硬隔离；原 uncertainty 不能凭新一轮成功清掉。S4序列检测已进入计数域的流连续性，不能保证发现进入计数域前被屏蔽的回调。A1共享依赖失败可影响多个scope，只有独立依赖才可隔离；对54个键不预先宣称DAG一定优于扫描。A3缺少人工/独立基准时只能统计决策差异、未知率和成本，不能把差异率命名为误判率/覆盖率。

<a id="external-notes"></a>
## 外部补充（不属于核验原稿）

2026-09-15 核对 JDK 17 官方文档，仅用来界定接口含义：

- [JNI_OnLoad](https://docs.oracle.com/en/java/javase/17/docs/specs/jni/invocation.html#jni_onload)返回所需 JNI 接口版本；Ronova 自有协议兼容是另一个工程合同，不能把 JNI 版本或构建 ID 当成全部应用 feature 协商。
- [JVMTI RetransformClasses](https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html#RetransformClasses)有能力、类可修改性及执行条件；一个自有 probe 的结果不能替代其他类/执行出口的实际前提验证。本轮不新增 JVMTI 能力位、不用规范存在性冒充获授。

<a id="source-excerpts"></a>
## 精确源码摘录

以下行号指收到的原 r66 源包，不是新源码；每段在融合 JSON 中另有原文件 SHA-256 和归一化摘录 SHA-256。检查器只证明摘录与给定源包一致，不证明上面的人工语义推论。

<a id="activation-enum"></a>
### activation-enum

`src/main/java/dev/ronova/coremod/policy/ActivationState.java:1–11`

```text
   1  package dev.ronova.coremod.policy;
   2  
   3  /** Runtime activation is independent from the configured and supported levels. */
   4  public enum ActivationState {
   5      INACTIVE,
   6      ACTIVE,
   7      PENDING_RESTART,
   8      UNAVAILABLE,
   9      REJECTED,
  10      FENCED
  11  }
```

<a id="evidence-enum"></a>
### evidence-enum

`src/main/java/dev/ronova/coremod/policy/EvidenceState.java:1–10`

```text
   1  package dev.ronova.coremod.policy;
   2  
   3  /** What the current artifact can prove about one configured mechanism. */
   4  public enum EvidenceState {
   5      UNOBSERVED,
   6      OBSERVED,
   7      VERIFIED,
   8      DEGRADED,
   9      FAILED
  10  }
```

<a id="policy-dimensions"></a>
### policy-dimensions

`src/main/java/dev/ronova/coremod/policy/StrengthPolicySnapshot.java:8–27`

```text
   8  /**
   9   * Immutable 7.17 strength policy image. Configuration, preset matrix, runtime ceiling,
  10   * evidence and activation remain separate so the UI cannot overclaim EXTREME.
  11   */
  12  public final class StrengthPolicySnapshot {
  13      public static final int LEGACY_SCHEMA_VERSION = 1;
  14      public static final int SCHEMA_VERSION = 3;
  15  
  16      private final PolicyRevision revision;
  17      private final int configuredMatrixRevision;
  18      private final int activeMatrixRevision;
  19      private final StrengthProfile configuredProfile;
  20      private final StrengthProfile activeProfile;
  21      private final Map<StrengthOptionKey, MechanismLevel> configuredLevels;
  22      private final Map<StrengthOptionKey, MechanismLevel> activeLevels;
  23      private final Map<StrengthOptionKey, MechanismLevel> supportedCeilings;
  24      private final Map<StrengthOptionKey, MechanismLevel> evidenceCeilings;
  25      private final Map<StrengthOptionKey, EvidenceState> evidenceStates;
  26      private final Map<StrengthOptionKey, ActivationState> runtimeStates;
  27      private final AttackActionPolicy configuredActions;
```

<a id="policy-projection"></a>
### policy-projection

`src/main/java/dev/ronova/coremod/policy/StrengthPolicySnapshot.java:282–326`

```text
 282      public StrengthPolicySnapshot withCapability(
 283          StrengthOptionKey option,
 284          MechanismLevel supportedCeiling,
 285          EvidenceState evidenceState,
 286          ActivationState runtimeState
 287      ) {
 288          MechanismLevel inferredEvidenceCeiling = switch (
 289              Objects.requireNonNull(evidenceState, "evidenceState")
 290          ) {
 291              case UNOBSERVED, FAILED -> MechanismLevel.OFF;
 292              case OBSERVED, VERIFIED, DEGRADED ->
 293                  MechanismLevel.runnable(supportedCeiling);
 294          };
 295          return withCapability(
 296              option, supportedCeiling, inferredEvidenceCeiling,
 297              evidenceState, runtimeState
 298          );
 299      }
 300  
 301      public StrengthPolicySnapshot withCapability(
 302          StrengthOptionKey option,
 303          MechanismLevel supportedCeiling,
 304          MechanismLevel evidenceCeiling,
 305          EvidenceState evidenceState,
 306          ActivationState runtimeState
 307      ) {
 308          Objects.requireNonNull(option, "option");
 309          EnumMap<StrengthOptionKey, MechanismLevel> ceilings =
 310              new EnumMap<>(supportedCeilings);
 311          EnumMap<StrengthOptionKey, MechanismLevel> evidenceLimits =
 312              new EnumMap<>(evidenceCeilings);
 313          EnumMap<StrengthOptionKey, EvidenceState> evidence =
 314              new EnumMap<>(evidenceStates);
 315          EnumMap<StrengthOptionKey, ActivationState> states =
 316              new EnumMap<>(runtimeStates);
 317          ceilings.put(option, MechanismLevel.runnable(supportedCeiling));
 318          evidenceLimits.put(option, MechanismLevel.runnable(evidenceCeiling));
 319          evidence.put(option, Objects.requireNonNull(evidenceState, "evidenceState"));
 320          states.put(option, Objects.requireNonNull(runtimeState, "runtimeState"));
 321          return new StrengthPolicySnapshot(
 322              revision, configuredMatrixRevision, activeMatrixRevision,
 323              configuredProfile, activeProfile,
 324              configuredLevels, activeLevels, ceilings, evidenceLimits,
 325              evidence, states, configuredActions, activeActions
 326          );
```

<a id="policy-state-resolution"></a>
### policy-state-resolution

`src/main/java/dev/ronova/coremod/policy/StrengthPolicySnapshot.java:359–433`

```text
 359      private ActivationState activationState(
 360          StrengthOptionKey option,
 361          MechanismLevel activeRequested,
 362          MechanismLevel supportedCeiling,
 363          MechanismLevel evidenceCeiling,
 364          EvidenceState evidence
 365      ) {
 366          // One saved profile becomes one coherent runtime image. Hot activation
 367          // of only part of a profile would split attack/protection/runtime truth.
 368          // Compare configuration syntax as well as resolved strength: changing
 369          // FOLLOW_PROFILE to an equal explicit level is still a new durable image.
 370          if (configuredMatrixRevision != activeMatrixRevision
 371              || configuredProfile != activeProfile
 372              || !configuredLevels.equals(activeLevels)
 373              || !configuredActions.equals(activeActions)) {
 374              return ActivationState.PENDING_RESTART;
 375          }
 376          ActivationState explicit = runtimeStates.get(option);
 377          if (explicit == ActivationState.FENCED || explicit == ActivationState.REJECTED) {
 378              return explicit;
 379          }
 380          if (activeRequested == MechanismLevel.OFF) {
 381              return ActivationState.INACTIVE;
 382          }
 383          if (supportedCeiling == MechanismLevel.OFF
 384              || evidenceCeiling == MechanismLevel.OFF
 385              || evidence == EvidenceState.UNOBSERVED) {
 386              return ActivationState.UNAVAILABLE;
 387          }
 388          if (evidence == EvidenceState.FAILED) {
 389              return ActivationState.FENCED;
 390          }
 391          return explicit == ActivationState.UNAVAILABLE
 392              ? ActivationState.UNAVAILABLE : ActivationState.ACTIVE;
 393      }
 394  
 395      private static String reason(
 396          MechanismLevel activeRequested,
 397          MechanismLevel effective,
 398          MechanismLevel supportedCeiling,
 399          MechanismLevel evidenceCeiling,
 400          EvidenceState evidence,
 401          ActivationState activation,
 402          boolean minimumFloorApplied
 403      ) {
 404          return switch (activation) {
 405              case PENDING_RESTART -> "configured policy awaits restart";
 406              case UNAVAILABLE -> "runtime capability is unavailable or unobserved";
 407              case REJECTED -> "runtime rejected activation";
 408              case FENCED -> "runtime evidence is fenced or failed";
 409              case INACTIVE -> "option is disabled";
 410              case ACTIVE -> minimumFloorApplied
 411                  ? "active at mandatory safety floor"
 412                  : effective.rank() < activeRequested.rank()
 413                      ? effective.rank() < supportedCeiling.rank()
 414                          ? "active at evidence ceiling"
 415                          : "active at supported ceiling"
 416                      : evidence == EvidenceState.DEGRADED
 417                          ? "active with degraded evidence" : "active and supported";
 418          };
 419      }
 420  
 421      private static Map<StrengthOptionKey, MechanismLevel> evidenceCeilings(
 422          Map<StrengthOptionKey, MechanismLevel> supported,
 423          Map<StrengthOptionKey, EvidenceState> evidence
 424      ) {
 425          EnumMap<StrengthOptionKey, MechanismLevel> values =
 426              new EnumMap<>(StrengthOptionKey.class);
 427          for (StrengthOptionKey option : StrengthOptionKey.values()) {
 428              EvidenceState state = evidence.get(option);
 429              values.put(option, state == EvidenceState.UNOBSERVED
 430                      || state == EvidenceState.FAILED
 431                  ? MechanismLevel.OFF : supported.get(option));
 432          }
 433          return Collections.unmodifiableMap(values);
```

<a id="policy-receipt"></a>
### policy-receipt

`src/main/java/dev/ronova/coremod/policy/StrengthResolutionReceipt.java:1–68`

```text
   1  package dev.ronova.coremod.policy;
   2  
   3  import java.util.Objects;
   4  
   5  /** Machine-checkable configured/supported/effective result for one option. */
   6  public record StrengthResolutionReceipt(
   7      PolicyRevision policyRevision,
   8      int configuredMatrixRevision,
   9      int activeMatrixRevision,
  10      StrengthOptionKey option,
  11      MechanismLevel configuredLevel,
  12      MechanismLevel presetLevel,
  13      MechanismLevel requestedLevel,
  14      MechanismLevel supportedCeiling,
  15      MechanismLevel evidenceCeiling,
  16      MechanismLevel effectiveLevel,
  17      EvidenceState evidenceState,
  18      ActivationState activationState,
  19      boolean minimumFloorApplied,
  20      String reason
  21  ) {
  22      public StrengthResolutionReceipt {
  23          Objects.requireNonNull(policyRevision, "policyRevision");
  24          if (!ProfilePresetMatrix.supports(configuredMatrixRevision)
  25              || !ProfilePresetMatrix.supports(activeMatrixRevision)) {
  26              throw new IllegalArgumentException(
  27                  "unsupported profile preset matrix revision"
  28              );
  29          }
  30          Objects.requireNonNull(option, "option");
  31          Objects.requireNonNull(configuredLevel, "configuredLevel");
  32          presetLevel = MechanismLevel.runnable(presetLevel);
  33          requestedLevel = MechanismLevel.runnable(requestedLevel);
  34          supportedCeiling = MechanismLevel.runnable(supportedCeiling);
  35          evidenceCeiling = MechanismLevel.runnable(evidenceCeiling);
  36          effectiveLevel = MechanismLevel.runnable(effectiveLevel);
  37          Objects.requireNonNull(evidenceState, "evidenceState");
  38          Objects.requireNonNull(activationState, "activationState");
  39          reason = Objects.toString(reason, "").trim();
  40          if (effectiveLevel.rank() > supportedCeiling.rank()) {
  41              throw new IllegalArgumentException("effective level exceeds supported ceiling");
  42          }
  43          if (effectiveLevel.rank() > evidenceCeiling.rank()) {
  44              throw new IllegalArgumentException("effective level exceeds evidence ceiling");
  45          }
  46          if (effectiveLevel.rank() > requestedLevel.rank()) {
  47              throw new IllegalArgumentException("effective level exceeds requested level");
  48          }
  49          if (requestedLevel.rank()
  50              < ProfilePresetMatrix.minimumLevel(option).rank()) {
  51              throw new IllegalArgumentException(
  52                  "requested level is below the mandatory safety floor"
  53              );
  54          }
  55          if (activationState == ActivationState.ACTIVE
  56              && effectiveLevel == MechanismLevel.OFF) {
  57              throw new IllegalArgumentException("OFF option cannot be ACTIVE");
  58          }
  59      }
  60  
  61      public boolean pendingRestart() {
  62          return activationState == ActivationState.PENDING_RESTART;
  63      }
  64  
  65      public boolean available() {
  66          return activationState == ActivationState.ACTIVE
  67              || activationState == ActivationState.INACTIVE;
  68      }
```

<a id="memory-observations"></a>
### memory-observations

`src/main/java/dev/ronova/coremod/root/api/RecoveryMutationIntent.java:1–82`

```text
   1  package dev.ronova.coremod.root.api;
   2  
   3  import java.util.Objects;
   4  import java.util.UUID;
   5  
   6  /** One physical source incarnation's mutation intent. It is never a persistence-completion receipt. */
   7  public record RecoveryMutationIntent(RecoverySnapshotPlan source, UUID incarnation, Kind kind) {
   8      public enum Kind {
   9          STATIC_RECORD(0), STATIC_SNAPSHOT(1), LOADED_SNAPSHOT(2), LOADED_RECORD(3);
  10          final int wireId;
  11          Kind(int wireId) { this.wireId = wireId; }
  12          public boolean loaded() { return this == LOADED_SNAPSHOT || this == LOADED_RECORD; }
  13          public boolean record() { return this == STATIC_RECORD || this == LOADED_RECORD; }
  14      }
  15      public enum Observation {
  16          WRITE_OBSERVED(1), WRITE_UNVERIFIED(2), NOT_CURRENT(4), NOT_EXECUTED(8);
  17          final int bit;
  18          Observation(int bit) { this.bit = bit; }
  19      }
  20      public static final String OPERATION = "ronova:recovery-memory-mutation/v1";
  21      public record Receipt(RecoveryMutationIntent intent, Observation observation) {
  22          public Receipt { Objects.requireNonNull(intent); Objects.requireNonNull(observation); }
  23      }
  24      public RecoveryMutationIntent {
  25          Objects.requireNonNull(source); Objects.requireNonNull(incarnation); Objects.requireNonNull(kind);
  26          boolean loaded = source.storageScope() == RecoverySnapshotPlan.StorageScope.LOADED_SAVED_DATA;
  27          if (loaded != kind.loaded()) throw new IllegalArgumentException("mutation storage scope mismatch");
  28      }
  29      public EffectKey effectKey() {
  30          var terminal = source.terminal();
  31          return new EffectKey(terminal.commandId(), terminal.qualification().targetEpoch(), OPERATION,
  32              "source/" + source.sourceLocator() + "/" + incarnation.toString().replace("-", "")
  33                  + "/" + kind.wireId + "/" + source.copyDepth());
  34      }
  35      public EffectRecord prepared() {
  36          var terminal = source.terminal(); var key = effectKey();
  37          return new EffectRecord(key, terminal.authorityHash(), terminal.authorityCommitIndex(),
  38              payloadHash(key, terminal.authorityHash()), terminal.activePolicyRevision(), terminal.planId(),
  39              terminal.planRevision(), "recovery-memory-mutation", UUID.randomUUID(),
  40              EffectRecord.SideEffectClass.EXTERNAL_OPAQUE, EffectRecord.RecoveryCapability.QUERYABLE,
  41              EffectRecord.State.PREPARED, 1, "memory-mutation-intent;task-and-save-unproven");
  42      }
  43      public static int depth(EffectKey key) {
  44          if (!OPERATION.equals(key.semanticOperationId())
  45              || !key.semanticSlotId().matches("source/[0-9a-f]{64}/[0-9a-f]{32}/[0-3]/(0|[1-9][0-9]{0,9})"))
  46              throw new IllegalArgumentException("unregistered recovery mutation slot");
  47          return Integer.parseInt(key.semanticSlotId().substring(key.semanticSlotId().lastIndexOf('/') + 1));
  48      }
  49      public static String payloadHash(EffectKey key, String authorityHash) {
  50          depth(key);
  51          return EffectRecord.hash(OPERATION + "\nexact-registered-memory-cell-only\n" + key.canonical()
  52              + "\n" + EffectRecord.checkedHash(authorityHash));
  53      }
  54      /** Accumulate observed outcomes; a later rejected retry must not erase an earlier write. */
  55      public static String observations(EffectRecord prior, Observation observation) {
  56          String prefix = "memory-observations/", suffix = ";task-and-save-unproven";
  57          int mask = observationMask(prior);
  58          return prefix + (mask | observation.bit) + suffix;
  59      }
  60      /** Validate persisted evidence before interpreting it; arbitrary text is not an empty observation set. */
  61      public static int observationMask(EffectRecord record) {
  62          if (!OPERATION.equals(record.key().semanticOperationId()))
  63              throw new IllegalArgumentException("not a recovery mutation record");
  64          String evidence = record.evidenceRef();
  65          if (record.state() == EffectRecord.State.PREPARED
  66              && evidence.equals("memory-mutation-intent;task-and-save-unproven")) return 0;
  67          if (record.state() == EffectRecord.State.EFFECT_UNKNOWN) {
  68              if (evidence.equals("restart-prepared-effect-unknown") || evidence.equals("prepared-without-live-attempt")) return 0;
  69              if (evidence.matches("memory-observations/([1-9]|1[0-5]);task-and-save-unproven"))
  70                  return Integer.parseInt(evidence.substring("memory-observations/".length(), evidence.indexOf(';')));
  71          }
  72          throw new IllegalArgumentException("invalid memory mutation evidence/state");
  73      }
  74      /** An uncertain write is not cleared by a later observed bit. This covers memory only. */
  75      public boolean acknowledgedWrite(EffectRecord record) {
  76          if(record==null||!effectKey().equals(record.key())
  77              ||!source.terminal().authorityHash().equals(record.authorityPayloadHash())
  78              ||!payloadHash(effectKey(),source.terminal().authorityHash()).equals(record.effectPayloadHash()))return false;
  79          int mask=observationMask(record);
  80          return (mask&Observation.WRITE_OBSERVED.bit)!=0&&(mask&Observation.WRITE_UNVERIFIED.bit)==0;
  81      }
  82  }
```

<a id="shadow-result"></a>
### shadow-result

`src/main/java/dev/ronova/coremod/persistence/ShadowVerification.java:1–50`

```text
   1  package dev.ronova.coremod.persistence;
   2  
   3  import dev.ronova.coremod.contract.DominionEpoch;
   4  import java.util.Objects;
   5  
   6  /** Result of one authoritative-SavedData to shadow comparison. */
   7  public record ShadowVerification(
   8      Status status,
   9      DominionEpoch expectedEpoch,
  10      DominionEpoch observedEpoch,
  11      String expectedHash,
  12      String observedHash,
  13      String detail
  14  ) {
  15      public ShadowVerification {
  16          status = Objects.requireNonNull(status, "status");
  17          expectedEpoch = Objects.requireNonNull(expectedEpoch, "expectedEpoch");
  18          observedEpoch = Objects.requireNonNull(observedEpoch, "observedEpoch");
  19          expectedHash = Objects.toString(expectedHash, "");
  20          observedHash = Objects.toString(observedHash, "");
  21          detail = Objects.toString(detail, "");
  22      }
  23  
  24      public boolean accepted() {
  25          return status == Status.VERIFIED
  26              || status == Status.MIRRORED
  27              || status == Status.RECEIPT_MIRRORED;
  28      }
  29  
  30      public boolean matches() {
  31          return status == Status.VERIFIED;
  32      }
  33  
  34      public boolean rejected() {
  35          return !accepted();
  36      }
  37  
  38      public enum Status {
  39          VERIFIED,
  40          MIRRORED,
  41          RECEIPT_MIRRORED,
  42          REALM_MISMATCH,
  43          ROLLBACK_REJECTED,
  44          STATE_CONFLICT,
  45          RECEIPT_DRIFT,
  46          READ_ONLY,
  47          FAILPOINT,
  48          INVALID_INPUT
  49      }
  50  }
```

<a id="probe"></a>
### probe

`src/main/java/dev/ronova/coremod/integrity/CodeSealProbe.java:1–10`

```text
   1  package dev.ronova.coremod.integrity;
   2  
   3  /** Stateless execution canary for the registered Native code-image guard. */
   4  public final class CodeSealProbe {
   5      private CodeSealProbe() {}
   6      public static long evaluate(long challenge) {
   7          long mixed=challenge^0x7170D15EA5E12345L;
   8          return ((mixed<<17)|(mixed>>>47))+0x13579BDF2468ACE0L;
   9      }
  10  }
```

<a id="native-transform-bridge"></a>
### native-transform-bridge

`src/main/java/dev/ronova/coremod/agent/NativeTransformBridge.java:1–37`

```text
   1  package dev.ronova.coremod.agent;
   2  
   3  /** Called from the native JVMTI ClassFileLoadHook. */
   4  public final class NativeTransformBridge {
   5      private static final RonovaTransformer TRANSFORMER = new RonovaTransformer("native-jvmti");
   6      private static final java.util.Map<String,String> FAILURES = new java.util.concurrent.ConcurrentHashMap<>();
   7  
   8      private NativeTransformBridge() {}
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
  36      }
  37  }
```

<a id="registered-code"></a>
### registered-code

`native/src/ronova_native.cpp:173–244`

```text
 173          const auto size=std::strlen(image.name);
 174          if(std::strlen(signature)==size+2&&std::strncmp(signature+1,image.name,size)==0&&signature[size+1]==';')return &image;
 175      }
 176      return nullptr;
 177  }
 178  bool root_loader(JNIEnv* env,jobject loader){return g_root_loader!=nullptr&&env->IsSameObject(g_root_loader,loader);}
 179  bool registered_code_loader(JNIEnv* env, jobject loader, RegisteredCodeImage* image) {
 180      if (image == nullptr) return false;
 181      // Only the JDK-only bridge is expected in the bootstrap loader.
 182      return (std::strcmp(image->name, "dev/ronova/coremod/agent/BootstrapHooks") == 0
 183          ||std::strcmp(image->name,"dev/ronova/coremod/agent/BootstrapTaskHooks")==0
 184          ||std::strncmp(image->name,"dev/ronova/coremod/agent/BootstrapTaskHooks$",sizeof("dev/ronova/coremod/agent/BootstrapTaskHooks$")-1)==0)
 185          ? loader == nullptr : root_loader(env, loader);
 186  }
 187  void reassert_registered_code(JNIEnv* env) {
 188      if(!g_code_seals_enabled.load()||env==nullptr||g_jvmti==nullptr)return;
 189      std::uint64_t outside_before;
 190      {
 191          CodeStateGuard guard;
 192          if(g_code_cycle_active)return;
 193          // Expire paired Java observations before the inventory/verification state changes,
 194          // including cycles that fail before any ClassFileLoadHook is delivered.
 195          invalidate_code_semantics();
 196          g_code_cycle_active=true;
 197          outside_before=g_code_outside_changes;
 198          for(auto& image:CODE_IMAGES)image.verified.store(false);
 199      }
 200      jint count=0;jclass* classes=nullptr;
 201      bool complete=g_jvmti->GetLoadedClasses(&count,&classes)==JVMTI_ERROR_NONE;
 202      std::vector<RegisteredCodeImage*> confirmed;
 203      std::vector<jclass> targets;
 204      std::vector<CodeCycleEntry> entries;
 205      unsigned reported_failures=0;
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
 230              if(accepted)confirmed.push_back(entry.image);else {
 231                  complete=false;
 232                  if(reported_failures++<4)std::fprintf(stderr,"RONOVA_CODE_SEAL_REJECT class=%s jvmti=%d callbacks=%u proposed=%d\n",
 233                      entry.image->name,static_cast<int>(error),entry.callbacks,entry.proposed?1:0);
 234              }
 235          }
 236      }
 237      if(classes!=nullptr) {
 238          for(jint i=0;i<count;++i)env->DeleteLocalRef(classes[i]);
 239          g_jvmti->Deallocate(reinterpret_cast<unsigned char*>(classes));
 240      }
 241      {
 242          CodeStateGuard guard;
 243          // A global proposed flag is never a completion receipt for another thread's transform.
 244          if(complete&&outside_before==g_code_outside_changes)
```

<a id="semantic-targets"></a>
### semantic-targets

`native/src/ronova_native.cpp:304–344`

```text
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
 322  #undef RONOVA_SEMANTIC_TARGET
 323  
 324  std::atomic<LONG> g_semantic_required_mask{0};
 325  std::atomic<LONG> g_semantic_verified_mask{0};
 326  std::atomic<LONG> g_semantic_cycle_mask{0};
 327  std::atomic<LONG> g_semantic_cycle_failed_mask{0};
 328  std::atomic<bool> g_semantic_cycle_active{false};
 329  SRWLOCK g_semantic_state_lock = SRWLOCK_INIT;
 330  struct SemanticStateGuard {
 331      SemanticStateGuard() { AcquireSRWLockExclusive(&g_semantic_state_lock); }
 332      ~SemanticStateGuard() { ReleaseSRWLockExclusive(&g_semantic_state_lock); }
 333  };
 334  thread_local bool g_core_cycle_retransform = false;
 335  std::uint64_t g_semantic_outside_changes = 0;
 336  std::size_t g_semantic_cycle_callbacks = 0;
 337  std::atomic<std::int64_t> g_semantic_generation{0};
 338  std::atomic<std::int64_t> g_successful_leases{0};
 339  std::atomic<std::int64_t> g_failed_leases{0};
 340  std::atomic<std::int64_t> g_semantic_transformations{0};
 341  std::atomic<std::int64_t> g_semantic_failures{0};
 342  std::atomic<std::int64_t> g_semantic_reassertions{0};
 343  std::atomic<std::int64_t> g_consecutive_lease_failures{0};
 344  std::atomic<std::uint64_t> g_last_success_tick{0};
```

<a id="semantic-reassert"></a>
### semantic-reassert

`native/src/ronova_native.cpp:629–705`

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
 692  }
 693  
 694  bool semantic_lease_current(std::uint64_t now) {
 695      const LONG required = g_semantic_required_mask.load(std::memory_order_acquire);
 696      const LONG verified = g_semantic_verified_mask.load(std::memory_order_acquire);
 697      const std::uint64_t last = g_last_success_tick.load(std::memory_order_acquire);
 698      const std::uint64_t interval = static_cast<std::uint64_t>(
 699          InterlockedCompareExchange(&g_watchdog_interval, 0, 0)
 700      );
 701      const std::uint64_t maximum_age = interval * 2ULL + 5000ULL;
 702      return !g_semantic_cycle_active.load(std::memory_order_acquire)
 703          && g_consecutive_lease_failures.load(std::memory_order_acquire) == 0
 704          && required != 0 && (verified & required) == required && last != 0
 705          && now >= last && now - last <= maximum_age;
```

<a id="native-task-exit"></a>
### native-task-exit

`native/src/ronova_native.cpp:83–113`

```text
  83          &&exact_hook_frame(env,frames[2].method,entering?"enter":"exit")
  84          &&(!entering||frames[3].location==1||execution_task_bit(env,frames[3].method)==8||execution_task_bit(env,frames[3].method)==64)
  85          &&execution_task_bit(env,frames[3].method)!=0)result=frames[3].method;
  86      if(thread!=nullptr)env->DeleteLocalRef(thread);return result;
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
 109  struct TaskCancelContext {
 110      jobject task=nullptr,lease=nullptr,plan=nullptr,state_handle=nullptr;
 111      jmethodID valid=nullptr,close=nullptr;
 112      LONG required=0;std::uint64_t changes=0;
 113      bool checked=false,allowed=false,committed=false,uncertain=false;
```

<a id="seal-build-id"></a>
### seal-build-id

`native/src/ronova_native.cpp:741–754`

```text
 741  extern "C" JNIEXPORT jboolean JNICALL
 742  Java_dev_ronova_coremod_nativebridge_NativeBridge_enableCodeSeals0(JNIEnv* env,jclass,jstring id) {
 743  #ifdef RONOVA_CODE_SEALS
 744      if(id==nullptr||g_jvmti==nullptr||g_root_loader==nullptr)return JNI_FALSE;
 745      const char* text=env->GetStringUTFChars(id,nullptr);if(text==nullptr)return JNI_FALSE;
 746      bool match=std::strcmp(text,CODE_SEAL_BUILD_ID)==0;env->ReleaseStringUTFChars(id,text);
 747      if(!match)return JNI_FALSE;
 748      g_code_seals_enabled.store(true);
 749      if(g_jvmti->SetEventNotificationMode(JVMTI_ENABLE,JVMTI_EVENT_CLASS_PREPARE,nullptr)!=JVMTI_ERROR_NONE){g_code_seals_enabled.store(false);return JNI_FALSE;}
 750      reassert_registered_code(env);return JNI_TRUE;
 751  #else
 752      return JNI_FALSE;
 753  #endif
 754  }
```

<a id="native-capabilities"></a>
### native-capabilities

`native/src/ronova_native.cpp:787–846`

```text
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
```

<a id="bridge-build-and-mask"></a>
### bridge-build-and-mask

`src/main/java/dev/ronova/coremod/nativebridge/NativeBridge.java:13–96`

```text
  13  public final class NativeBridge {
  14      private static final String RESOURCE = "/native/windows-x86_64/ronova.dll";
  15      private static final String ROOT_RESOURCE =
  16          "/native/windows-x86_64/ronova_root.dll";
  17      private static final int CAP_ROOT_STATE = 0x8;
  18      private static final int CAP_ROOT_DECISION = 0x10;
  19      private static final int CAP_ROOT_RECONCILE = 0x20;
  20      private static final int CAP_SEMANTIC_LEASE = 0x40;
  21      private static final int ROOT_CAPABILITIES =
  22          CAP_ROOT_STATE | CAP_ROOT_DECISION | CAP_ROOT_RECONCILE;
  23      private static volatile State state = State.NOT_ATTEMPTED;
  24      private static volatile int capabilities;
  25      private static volatile boolean watchdogRequested;
  26      private static volatile int watchdogInterval=30000;
  27      private static volatile Class<?> taskHooks;
  28  
  29      private NativeBridge() {}
  30  
  31      public static synchronized State load(Path gameDirectory) {
  32          if (state != State.NOT_ATTEMPTED) {
  33              return state;
  34          }
  35          String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
  36          String architecture = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
  37          if (!os.contains("win") || !(architecture.equals("amd64") || architecture.equals("x86_64"))) {
  38              return state = State.UNSUPPORTED_PLATFORM;
  39          }
  40  
  41          try {
  42              System.load(extract(gameDirectory, RESOURCE, "ronova").toString());
  43              int integrityCapabilities = bootstrap();
  44              try {
  45                  taskHooks=prepareTaskBridge0(extract(gameDirectory,"/META-INF/ronova/task-hook-support.jar","ronova-task-hooks",".jar").toString());
  46                  if(taskHooks!=null){dev.ronova.coremod.agent.JdkTaskBoundaryContract.enable();refreshTaskObservers0();}
  47              } catch(Throwable unavailable){taskHooks=null;System.err.println("RONOVA_TASK_BRIDGE_UNAVAILABLE "+unavailable);}
  48              try (InputStream sealId=NativeBridge.class.getResourceAsStream("/META-INF/ronova/code-seal-id.txt")) {
  49                  if(sealId!=null){String id=new String(sealId.readNBytes(65),java.nio.charset.StandardCharsets.US_ASCII);
  50                      if(id.matches("[0-9a-f]{64}"))enableCodeSeals0(id);}
  51              }
  52              int rootCapabilities = 0;
  53              try {
  54                  System.load(extract(
  55                      gameDirectory, ROOT_RESOURCE, "ronova_root"
  56                  ).toString());
  57                  rootCapabilities = NativeRootBridge.bootstrapLibrary();
  58              } catch (Throwable ignored) {
  59                  // Integrity remains available while /ronova status reports Java fallback.
  60              }
  61              capabilities = integrityCapabilities | rootCapabilities;
  62              state = (integrityCapabilities & 0x7) == 0x7
  63                  ? State.ACTIVE : State.JVMTI_LIMITED;
  64              Runtime.getRuntime().addShutdownHook(new Thread(() -> {
  65                  watchdogRequested=false;
  66                  try {
  67                      NativeRootBridge.shutdownLibrary();
  68                  } catch (Throwable ignored) {
  69                      // Root DLL is optional on unsupported or degraded installations.
  70                  }
  71                  try {
  72                      shutdown();
  73                  } catch (Throwable ignored) {
  74                      // The operating system will reclaim the library at process exit.
  75                  }
  76              }, "Ronova-Native-Shutdown"));
  77          } catch (Throwable ignored) {
  78              state = State.LOAD_FAILED;
  79          }
  80          return state;
  81      }
  82  
  83      public static boolean isActive() {
  84          return state == State.ACTIVE;
  85      }
  86  
  87      public static boolean canRetransform() {
  88          return (capabilities & 0x4) != 0;
  89      }
  90  
  91      public static boolean isRootCapable() {
  92          return isActive() && (capabilities & ROOT_CAPABILITIES) == ROOT_CAPABILITIES;
  93      }
  94  
  95      public static boolean canAttestEnforcement() {
  96          return isActive() && (capabilities & CAP_SEMANTIC_LEASE) != 0;
```

<a id="bridge-mask-accessor"></a>
### bridge-mask-accessor

`src/main/java/dev/ronova/coremod/nativebridge/NativeBridge.java:115–121`

```text
 115      public static State state() {
 116          return state;
 117      }
 118  
 119      public static int capabilityMask() {
 120          return capabilities;
 121      }
```

<a id="task-drop-producer"></a>
### task-drop-producer

`src/main/java/dev/ronova/coremod/agent/BootstrapTaskHooks.java:33–87`

```text
  33      /** Opaque token is returned only at a registered JDK method entry and must be closed in its finally. */
  34      public static Object enter(Object task) {
  35          try{return enterFrame(task);}catch(Throwable unavailable){lost();return null;}
  36      }
  37      private static Object enterFrame(Object task) {
  38          if(observer==null||task==null||Boolean.TRUE.equals(DELIVERING.get())||!boundary(false))return null;
  39          Frame parent=CURRENT.get();
  40          Integer overflow=OVERFLOW_DEPTH.get();
  41          if(overflow!=null){OVERFLOW_DEPTH.set(Math.addExact(overflow,1));return OVERFLOW_TOKEN;}
  42          if(parent!=null&&parent.depth>=MAX_DEPTH){OVERFLOW_DEPTH.set(1);emit(OVERFLOW,task,null,parent.task,null);return OVERFLOW_TOKEN;}
  43          Frame frame=new Frame(task,parent,nativeEnter(task));
  44          CURRENT.set(frame);
  45          if(parent==null||parent.task!=task)emit(ENTER,task,null,parent==null?null:parent.task,frame);
  46          return frame;
  47      }
  48      public static void exit(Object token) {
  49          try{exitFrame(token);}catch(Throwable unavailable){lost();
  50              if(token instanceof Frame frame&&frame.thread==Thread.currentThread()&&CURRENT.get()==frame) {
  51                  if(frame.previous==null)CURRENT.remove();else CURRENT.set(frame.previous);
  52              }
  53          }
  54      }
  55      private static void exitFrame(Object token) {
  56          if(token==OVERFLOW_TOKEN&&boundary(false)) {
  57              Integer depth=OVERFLOW_DEPTH.get();if(depth!=null){if(depth==1)OVERFLOW_DEPTH.remove();else OVERFLOW_DEPTH.set(depth-1);}return;
  58          }
  59          if(!(token instanceof Frame frame)||frame.thread!=Thread.currentThread()||CURRENT.get()!=frame||!boundary(false))return;
  60          // Keep the executing parent context during exit delivery, but never permit event-handler recursion.
  61          try {if(frame.previous==null||frame.previous.task!=frame.task)emit(EXIT,frame.task,null,
  62              frame.previous==null?null:frame.previous.task,frame);}
  63          finally {nativeExit(frame.nativeToken);if(frame.previous==null)CURRENT.remove();else CURRENT.set(frame.previous);}
  64      }
  65      public static void submitted(Object scheduler,Object task) {
  66          try{submit(scheduler,task);}catch(Throwable unavailable){lost();}
  67      }
  68      private static void submit(Object scheduler,Object task) {
  69          if(observer==null||task==null||Boolean.TRUE.equals(DELIVERING.get())||!boundary(true))return;
  70          if(scheduler==task&&task instanceof java.util.concurrent.ForkJoinTask<?>) {
  71              scheduler=java.util.concurrent.ForkJoinTask.getPool();if(scheduler==null)scheduler=java.util.concurrent.ForkJoinPool.commonPool();
  72          }
  73          Frame frame=CURRENT.get();
  74          emit(SUBMIT,task,scheduler,frame==null?null:frame.task,frame);
  75      }
  76      public static Object currentTask(){Frame frame=CURRENT.get();return OVERFLOW_DEPTH.get()!=null||Boolean.TRUE.equals(DELIVERING.get())||frame==null?null:frame.task;}
  77      public static Object currentExecution(){return OVERFLOW_DEPTH.get()!=null||Boolean.TRUE.equals(DELIVERING.get())?null:CURRENT.get();}
  78      public static long droppedEvents(){return DROPPED.get();}
  79      private static void lost(){for(;;){long n=DROPPED.get();if(n==Long.MAX_VALUE||DROPPED.compareAndSet(n,n+1))return;}}
  80      private static void emit(int kind,Object task,Object scheduler,Object parent,Object execution) {
  81          Consumer<Object[]> sink=observer;
  82          if(sink==null||Boolean.TRUE.equals(DELIVERING.get()))return;
  83          DELIVERING.set(true);
  84          try {sink.accept(new Object[]{kind,task,scheduler,parent,execution,Thread.currentThread(),System.nanoTime(),DROPPED.get()});}
  85          catch(Throwable unavailable){lost();}
  86          finally {DELIVERING.remove();}
  87      }
```

<a id="task-loss-consumer"></a>
### task-loss-consumer

`src/main/java/dev/ronova/coremod/observation/RecoveryTaskObserver.java:166–253`

```text
 166      public int merge(int allowance) {
 167          requireOwner();if(allowance<0||allowance>64)throw new IllegalArgumentException("invalid task merge allowance");
 168          if(closed)return 0;
 169          for(int i=0;i<16;i++) {
 170              Identity collected=(Identity)COLLECTED_EXECUTIONS.poll();if(collected==null)break;
 171              if(EXECUTIONS.remove(collected)!=null)EXECUTION_SLOTS.release();
 172          }
 173          if(!install()){graph.evidenceLost();return 0;}
 174          long lost=LOST.get();if(lost!=observedLosses){graph.evidenceLost();observedLosses=lost;}
 175          int count=0;Captured captured;
 176          while(count<allowance&&(captured=poll())!=null) {
 177              count++;
 178              if(captured instanceof Copy copy){copyMerge.accept(copy);merged++;continue;}
 179              if(captured instanceof Run run) {
 180                  var proof=NativeBridge.taskObservers();var value=run.value;
 181                  if(!proof.covers(63)||proof.changes()!=value.changes||value.dropped!=observedDrops) {
 182                      observedDrops=value.dropped;graph.evidenceLost();continue;
 183                  }
 184                  value.mergeInto(graph);merged=Math.addExact(merged,value.events);continue;
 185              }
 186              Event event=(Event)captured;var proof=NativeBridge.taskObservers();
 187              Object task=event.task;
 188              if(task==null||!proof.covers(63)||proof.changes()!=event.changes||event.dropped!=observedDrops) {
 189                  observedDrops=event.dropped;graph.evidenceLost();continue;
 190              }
 191              if(event.kind==1)graph.entered(task,event.execution,event.parent);
 192              else if(event.kind==2)graph.exited(task,event.execution);
 193              else graph.submitted(task,event.scheduler,event.parent);
 194              merged++;
 195          }
 196          for(int i=0;i<16;i++) {
 197              Identity key=originScan.poll();if(key==null)break;
 198              if(key.get()==null){if(origins.remove(key)!=null)originSlots.release();}else originScan.offer(key);
 199          }
 200          graph.retireUnboundCollected(16);
 201              for(int i=0;i<16;i++) {
 202                  Identity key=ROUTE_SCAN.poll();if(key==null)break;Route route=ROUTES.get(key);
 203                  RecoveryTaskObserver owner=route==null?null:route.owner.get();
 204                  if(key.get()==null||owner==null){if(route!=null&&ROUTES.remove(key,route))ROUTE_SLOTS.release();}else ROUTE_SCAN.offer(key);
 205              }
 206          return count;
 207      }
 208      public TaskExecutionGraph graph(){return graph;}
 209      public long merged(){return merged;}
 210      public long lostEvents(){return LOST.get();}
 211      public Map<String,Long> lossReasons(){return Map.of("proof_unavailable",LOSS_REASONS.get(0),"receiver_unproven",LOSS_REASONS.get(1),
 212          "proof_changed",LOSS_REASONS.get(2),"inbox_full",LOSS_REASONS.get(3),"capture_exception",LOSS_REASONS.get(4));}
 213      public Map<String,Long> captureDiagnostics(){return Map.of("local_inbox_drops",localInboxDrops.get(),"captured_events",capturedEvents.get(),
 214          "queued_objects",(long)inbox.size(),"reserved_slots",(long)queued(),"compacted_events",compactedEvents.get());}
 215      public int queued(){return INBOX_LIMIT-permits.availablePermits();}
 216      private static void lost(){LOST.updateAndGet(value->value==Long.MAX_VALUE?value:value+1);}
 217      private static void lost(int reason){LOSS_REASONS.updateAndGet(reason,value->value==Long.MAX_VALUE?value:value+1);lost();}
 218      private void requireOwner(){if(Thread.currentThread()!=owner)throw new IllegalStateException("task evidence merge requires its server lane");}
 219      private boolean enqueue(Captured captured) {
 220          synchronized(inboxLock) {
 221              if(closed)return false;
 222              if(captured instanceof Event event&&tailRun!=null&&tailRun.value.append(event.kind,event.task,event.scheduler,
 223                  event.parent,event.execution,event.changes,event.dropped)) {
 224                  capturedEvents.incrementAndGet();compactedEvents.incrementAndGet();return true;
 225              }
 226              tailRun=null; // Different events (especially copies) may never be reordered into an older run.
 227              if(!permits.tryAcquire()){localInboxDrops.incrementAndGet();lost(3);return false;}
 228              boolean queued=false;
 229              try {
 230                  if(captured instanceof Event event&&event.kind==1)
 231                      captured=tailRun=new Run(new TaskEventRun(event.task,event.parent,event.execution,event.changes,event.dropped));
 232                  inbox.offer(captured);queued=true;
 233                  if(!(captured instanceof Copy))capturedEvents.incrementAndGet();
 234                  return true;
 235              } finally {if(!queued){tailRun=null;permits.release();}}
 236          }
 237      }
 238      private Captured poll() {
 239          synchronized(inboxLock) {
 240              Captured captured=inbox.poll();if(captured==null)return null;
 241              if(captured==tailRun)tailRun=null;
 242              permits.release();return captured;
 243          }
 244      }
 245      private void discardClosed(){for(int i=0;i<INBOX_LIMIT;i++)if(poll()==null)break;}
 246      @Override public void close() {
 247          requireOwner();if(closed)return;closed=true;
 248          discardClosed();
 249          origins.clear();originScan.clear();
 250          // Routes keep only weak owners and objects; late events cannot enter a replacement runtime.
 251          graph.evidenceLost();
 252          release.run();
 253      }
```

<a id="task-graph-results"></a>
### task-graph-results

`src/main/java/dev/ronova/coremod/observation/TaskExecutionGraph.java:6–34`

```text
   6  /** Volatile execution evidence. Root alone owns durable intents and permissions. No foreign methods are invoked. */
   7  public final class TaskExecutionGraph {
   8      public enum State { SUBMISSION_UNCONFIRMED, EXECUTING, EXIT_OBSERVED, CANCEL_REQUESTED, CANCEL_RETURNED, UNKNOWN }
   9      /** Queue disposition kept separate from execution exit: a queued task that never ran must not claim EXECUTION_EXITED. */
  10      public enum QueueState { UNOBSERVED_SUBMISSION, SUBMITTED_NEVER_ENTERED, ENTERED, EVIDENCE_LOST }
  11      public record SourceUse(dev.ronova.coremod.root.api.SubjectQualification qualification,String locator,long sourceVersion,int copyDepth) {
  12          public SourceUse {Objects.requireNonNull(qualification);locator=dev.ronova.coremod.root.api.EffectRecord.checkedHash(locator);if(sourceVersion<1||copyDepth<0)throw new IllegalArgumentException("invalid source version/depth");}
  13      }
  14      public record View(UUID registration,State state,long submissions,long entries,long exits,int activeExecutions,
  15                         Set<UUID> parents,Set<UUID> children,boolean cancellationReturned,boolean cancellationAccepted,
  16                         boolean taskReachable,boolean evidenceLost,long revision,Set<SourceUse> sources) {
  17          public View { parents=Set.copyOf(parents);children=Set.copyOf(children);sources=Set.copyOf(sources); }
  18          public boolean executionsExited(){return !evidenceLost&&entries>0&&activeExecutions==0&&entries==exits;}
  19          /**
  20           * Observed submission with no entered execution. This is intentionally not an exit fact:
  21           * a wrapper that is cancelled while still queued never ran, so it cannot discharge
  22           * EXECUTION_EXITED, and it cannot discharge a resource or descendant obligation either.
  23           */
  24          public QueueState queueState(){
  25              if(evidenceLost)return QueueState.EVIDENCE_LOST;
  26              if(entries>0||activeExecutions>0)return QueueState.ENTERED;
  27              return submissions>0?QueueState.SUBMITTED_NEVER_ENTERED:QueueState.UNOBSERVED_SUBMISSION;
  28          }
  29          /** True only when this node was actually observed being submitted and has never entered execution. */
  30          public boolean queuedNeverEntered(){return queueState()==QueueState.SUBMITTED_NEVER_ENTERED;}
  31          // No queue-publication, descendant, resource or storage success is inferred from this observation.
  32      }
  33      private static final int MAX_RELATIONS=64,MAX_EXECUTIONS=64;
  34      private final int capacity;
```

<a id="task-graph-invalidation"></a>
### task-graph-invalidation

`src/main/java/dev/ronova/coremod/observation/TaskExecutionGraph.java:165–183`

```text
 165          if(target.cancellationResults.containsKey(acknowledgedAttempt))return;
 166          if(target.cancellationResults.size()>=MAX_RELATIONS){target.lost=true;lost();return;}
 167          target.cancellationResults.put(acknowledgedAttempt,null);target.cancelRequested=true;touch(target);
 168      }
 169      synchronized void cancellationReturned(UUID registration,UUID acknowledgedAttempt,boolean accepted) {
 170          Node target=registrations.get(registration);
 171          if(target==null||!target.cancellationResults.containsKey(acknowledgedAttempt)){lost();return;}
 172          Boolean old=target.cancellationResults.get(acknowledgedAttempt);
 173          if(old!=null){if(old!=accepted){target.lost=true;lost();}return;}
 174          target.cancellationResults.put(acknowledgedAttempt,accepted);target.cancelReturned=true;target.cancelAccepted|=accepted;touch(target);
 175      }
 176      synchronized void evidenceLost(){lost();for(Node node:registrations.values()){node.lost=true;touch(node);}}
 177      synchronized void sourceUsed(Object task,SourceUse source) {
 178          Node node=node(task);if(node==null)return;
 179          if(node.sources.contains(source))return;
 180          var prior=node.sources.stream().filter(value->value.qualification.equals(source.qualification)&&value.locator.equals(source.locator)
 181              &&value.sourceVersion==source.sourceVersion).findFirst().orElse(null);
 182          if(prior!=null){if(prior.copyDepth>=source.copyDepth)return;node.sources.remove(prior);}
 183          if(node.sources.size()>=MAX_RELATIONS){node.lost=true;lost();return;}
```

<a id="completion-dependencies"></a>
### completion-dependencies

`src/main/java/dev/ronova/coremod/runtime/CompletionTracker.java:1–88`

```text
   1  package dev.ronova.coremod.runtime;
   2  
   3  import dev.ronova.coremod.root.api.SubjectQualification;
   4  import dev.ronova.coremod.root.api.TerminalOperationPlan;
   5  import java.util.*;
   6  
   7  /** Current evidence for the frozen server-result contract. Never changes a qualification or resolves an effect. */
   8  public final class CompletionTracker {
   9      public static final long STABLE_TICKS = 20, MAX_SAMPLE_GAP = 10;
  10      public enum Condition {
  11          BODY_DISABLED, WORLD_RECORDS, OLD_EPOCH_REJECTED, RECOVERY_TASKS,
  12          REBUILD_ENTRYPOINTS, SAVE_QUALIFICATION, TRACKING, SERVER_PRESENCE, NO_PENDING_WORK
  13      }
  14      public enum State { MISSING, VERIFIED, FAILED }
  15      public record Scope(UUID commandId, String authorityHash, SubjectQualification qualification,
  16                          long activePolicy, long configuredPolicy, String contract) {
  17          public Scope(UUID commandId,String authorityHash,SubjectQualification qualification,long activePolicy,long configuredPolicy) {
  18              this(commandId,authorityHash,qualification,activePolicy,configuredPolicy,"ronova:terminal-server/v4");
  19          }
  20          public Scope {
  21              Objects.requireNonNull(commandId); Objects.requireNonNull(authorityHash); Objects.requireNonNull(qualification);
  22              Objects.requireNonNull(contract);
  23              if(contract.isBlank()||contract.length()>128)throw new IllegalArgumentException("invalid completion contract");
  24              if (!qualification.terminal() || activePolicy < 1 || configuredPolicy < 1)
  25                  throw new IllegalArgumentException("invalid completion scope");
  26          }
  27          public static Scope of(TerminalOperationPlan plan) {
  28              return new Scope(plan.commandId(), plan.authorityHash(), plan.qualification(),
  29                  plan.activePolicyRevision(), plan.configuredPolicyRevision(),plan.completionContract());
  30          }
  31      }
  32      public record Evidence(State state, long dependencyRevision, String reference) {
  33          public Evidence {
  34              Objects.requireNonNull(state); Objects.requireNonNull(reference);
  35              if (dependencyRevision < 0 || reference.isBlank() || reference.length() > 128
  36                  || state == State.VERIFIED && dependencyRevision == 0)
  37                  throw new IllegalArgumentException("invalid completion evidence");
  38          }
  39      }
  40      public record Evaluation(Scope scope, long revision, boolean complete, boolean fenced,
  41                               Set<Condition> missing, long stableTicks, boolean unresolvedEffect) {
  42          public Evaluation { missing = Set.copyOf(missing); }
  43      }
  44      private Scope scope;
  45      private Map<Condition, Evidence> previous = Map.of();
  46      private long sampledAt = -1, stableSince = -1, revision;
  47      private Evaluation current, lastSatisfied;
  48  
  49      public synchronized Evaluation sample(Scope next, long now, Map<Condition, Evidence> evidence,
  50                                            boolean authorityCurrent, boolean effectsResolved) {
  51          Objects.requireNonNull(next); Objects.requireNonNull(evidence);
  52          if (now < 0) throw new IllegalArgumentException("negative observation time");
  53          EnumMap<Condition, Evidence> snapshot = new EnumMap<>(Condition.class);
  54          evidence.forEach((key, value) -> snapshot.put(Objects.requireNonNull(key), Objects.requireNonNull(value)));
  55          EnumSet<Condition> missing = EnumSet.allOf(Condition.class);
  56          snapshot.forEach((condition, proof) -> { if (proof.state() == State.VERIFIED) missing.remove(condition); });
  57          boolean fenced = !authorityCurrent || snapshot.values().stream().anyMatch(e -> e.state() == State.FAILED);
  58          boolean changed = !next.equals(scope) || !snapshot.equals(previous);
  59          boolean gap = sampledAt < 0 || now < sampledAt || now - sampledAt > MAX_SAMPLE_GAP;
  60          boolean eligible = !fenced && missing.isEmpty() && effectsResolved;
  61          if (!eligible) stableSince = -1;
  62          else if (changed || gap || stableSince < 0) stableSince = now;
  63          long stable = stableSince < 0 ? 0 : now - stableSince;
  64          boolean complete = eligible && stable >= STABLE_TICKS;
  65          if (current == null || changed || gap || current.complete() != complete || current.fenced() != fenced
  66              || current.unresolvedEffect() == effectsResolved || !current.missing().equals(missing)) revision = Math.addExact(revision, 1);
  67          scope = next; previous = Map.copyOf(snapshot); sampledAt = now;
  68          current = new Evaluation(scope, revision, complete, fenced, missing, stable, !effectsResolved);
  69          if (complete) lastSatisfied = current;
  70          return current;
  71      }
  72  
  73      /** A silent observer cannot leave a previously valid certificate displayed forever. */
  74      public synchronized Evaluation current(long now) {
  75          if (current == null) return null;
  76          if (now < sampledAt || now - sampledAt > MAX_SAMPLE_GAP) {
  77              stableSince = -1;
  78              if (!current.fenced() || current.complete() || !current.missing().equals(Set.of(Condition.values()))) {
  79                  revision = Math.addExact(revision, 1);
  80                  current = new Evaluation(scope, revision, false, true, Set.of(Condition.values()), 0, current.unresolvedEffect());
  81              }
  82              // Invalidate the dependency snapshot too; renewed observations start a fresh window.
  83              previous = Map.of();
  84          }
  85          return current;
  86      }
  87      public synchronized Evaluation lastSatisfied() { return lastSatisfied; }
  88  }
```

<a id="entity-graph"></a>
### entity-graph

`src/main/java/dev/ronova/coremod/observation/TopologyGraph.java:20–42`

```text
  20  import net.minecraft.resources.ResourceKey;
  21  import net.minecraft.server.MinecraftServer;
  22  import net.minecraft.server.level.ServerLevel;
  23  import net.minecraft.world.entity.Entity;
  24  import net.minecraft.world.level.Level;
  25  
  26  /** Per-server weak raw graph. Terminal state is deliberately outside this type. */
  27  public final class TopologyGraph {
  28      private final MinecraftServer owner;
  29      private final WeakTopologyCore<Entity, ResourceKey<Level>, MinecraftServer> core;
  30  
  31      public TopologyGraph(MinecraftServer owner) {
  32          this.owner = Objects.requireNonNull(owner, "owner");
  33          core = new WeakTopologyCore<>(
  34              owner,
  35              TopologyGraph::serverOf,
  36              TopologyGraph::levelKeyOf,
  37              Entity::getUUID,
  38              Entity::getId,
  39              TopologyGraph::shouldIndexRaw
  40          );
  41      }
  42  
```

<a id="existing-plan-lease"></a>
### existing-plan-lease

`src/main/java/dev/ronova/coremod/runtime/PlanExecutionLease.java:1–34`

```text
   1  package dev.ronova.coremod.runtime;
   2  
   3  import dev.ronova.coremod.root.api.TerminalOperationPlan;
   4  import java.util.Objects;
   5  import java.util.function.LongSupplier;
   6  
   7  /** Short-lived, thread-confined permission for one exact Root plan. Never grants Root authority. */
   8  public final class PlanExecutionLease implements AutoCloseable {
   9      public static final long MAX_DURATION_NANOS = 5_000_000_000L;
  10      private final TerminalOperationPlan plan;
  11      private final Thread owner = Thread.currentThread();
  12      private final LongSupplier clock;
  13      private final long issued, duration;
  14      private boolean closed;
  15  
  16      public PlanExecutionLease(TerminalOperationPlan plan, LongSupplier clock, long duration) {
  17          this.plan = Objects.requireNonNull(plan);
  18          this.clock = Objects.requireNonNull(clock);
  19          if (duration <= 0 || duration > MAX_DURATION_NANOS) throw new IllegalArgumentException("invalid lease duration");
  20          this.duration = duration;
  21          issued = clock.getAsLong();
  22      }
  23  
  24      public boolean valid(TerminalOperationPlan expected) {
  25          if (Thread.currentThread() != owner || closed || !plan.equals(expected)) return false;
  26          long elapsed = clock.getAsLong() - issued;
  27          return elapsed >= 0 && elapsed < duration;
  28      }
  29  
  30      @Override public void close() {
  31          if (Thread.currentThread() != owner) throw new IllegalStateException("lease belongs to another execution thread");
  32          closed = true;
  33      }
  34  }
```

<a id="existing-operation-plan"></a>
### existing-operation-plan

`src/main/java/dev/ronova/coremod/root/api/TerminalOperationPlan.java:1–41`

```text
   1  package dev.ronova.coremod.root.api;
   2  
   3  import dev.ronova.coremod.contract.SubjectKey;
   4  import dev.ronova.coremod.surface.RuntimeSurface;
   5  import java.util.*;
   6  
   7  /** Derived by the existing Root writer from one retained, committed qualification receipt. */
   8  public record TerminalOperationPlan(UUID planId, long planRevision, UUID commandId, String authorityHash,
   9      long authorityCommitIndex, SubjectQualification qualification, long activePolicyRevision,
  10      long configuredPolicyRevision, String ownerIdentity, String completionContract,
  11      List<Step> orderedSteps, Set<RuntimeSurface> requiredSurfaces, Set<RuntimeSurface> optionalSurfaces,
  12      long reservedHeapBytes, String recoveryPolicy) {
  13      public enum Step { ADMISSION_FENCE, SERVER_PRESENCE, SOURCE_QUALIFICATION, TERMINAL_LIFECYCLE, TERMINAL_STATE, WORLD_RECORDS, NETWORK_PROJECTION }
  14      public TerminalOperationPlan {
  15          Objects.requireNonNull(planId); Objects.requireNonNull(commandId); Objects.requireNonNull(qualification);
  16          authorityHash = EffectRecord.checkedHash(authorityHash);
  17          if (planRevision < authorityCommitIndex || !planId.equals(commandId) || authorityCommitIndex < 1 || !qualification.terminal()
  18              || qualification.revisions() == null || qualification.revisions().activeIncarnation() == null
  19              || activePolicyRevision < 1 || configuredPolicyRevision < 1 || reservedHeapBytes != 16384)
  20              throw new IllegalArgumentException("invalid terminal operation plan");
  21          orderedSteps = List.copyOf(orderedSteps); requiredSurfaces = Set.copyOf(requiredSurfaces); optionalSurfaces = Set.copyOf(optionalSurfaces);
  22          if (!orderedSteps.equals(List.of(Step.values()))
  23              || !requiredSurfaces.equals(Set.of(RuntimeSurface.REGISTRY, RuntimeSurface.TEMPORAL, RuntimeSurface.DATA))
  24              || !optionalSurfaces.equals(Set.of(RuntimeSurface.NETWORK, RuntimeSurface.PRESENTATION))
  25              || !Objects.equals(ownerIdentity, "root/" + qualification.subjectKey().realmId().canonical())
  26              || !Objects.equals(completionContract, "ronova:terminal-server/v4")
  27              || !Objects.equals(recoveryPolicy, "CONTROLLED_REASSERT_OPAQUE_NO_REPLAY"))
  28              throw new IllegalArgumentException("unregistered plan contract");
  29      }
  30      public static TerminalOperationPlan terminal(UUID command, String hash, long index, long planRevision,
  31          SubjectQualification qualification, long activePolicy, long configuredPolicy) {
  32          return new TerminalOperationPlan(command, planRevision, command, hash, index, qualification, activePolicy, configuredPolicy,
  33              "root/" + qualification.subjectKey().realmId().canonical(), "ronova:terminal-server/v4", List.of(Step.values()),
  34              Set.of(RuntimeSurface.REGISTRY, RuntimeSurface.TEMPORAL, RuntimeSurface.DATA),
  35              Set.of(RuntimeSurface.NETWORK, RuntimeSurface.PRESENTATION), 16384, "CONTROLLED_REASSERT_OPAQUE_NO_REPLAY");
  36      }
  37      public SubjectKey subjectKey() { return qualification.subjectKey(); }
  38      public UUID physicalNonce() { return qualification.revisions().activeIncarnation().nonce(); }
  39      public EffectKey effectKey() { return new EffectKey(commandId, qualification.targetEpoch(), EffectRecord.TERMINAL_LIFECYCLE, EffectRecord.ONCE_SLOT); }
  40      public String causalityKey() { return commandId + "/" + subjectKey().canonical() + ":" + qualification.targetEpoch().generation() + "/" + planRevision; }
  41  }
```
