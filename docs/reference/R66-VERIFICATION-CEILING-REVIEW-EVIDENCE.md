# r66 核验补遗、天花板与消费侧：fix8 独立静态复核

日期：2026-09-15。来源：原 r66 源码包（不是用户已继续开发的工作区）；本页为融合方独立读取结果，不是回写原稿。

源码 ZIP SHA-256：`1266d8c2dd46b949178d7341ee467333be678fb9b373c1403b576a56c73fd763`。

[当前采用的回执](../plans/RESTRUCTURE-REVIEW.md#review-fix8-verification) · [当前接线规划](../plans/R66-CAPABILITY-WIRING-PLAN.md#verification-ceiling) · [原交接索引](v13-verification-ceiling/README-INDEX.md)

## 核对边界

只读文件、词法检索和有范围的代码摘录；没有编译、执行 Java/JVM 或恢复链实机测试。零类型声明不是语义不存在的独立证明，源码分支存在不等于运行已达档。当前规划的决定与本页事实分别标注。

## 检索清单

### with-capability

限定 `src/main/java/dev/ronova/**/*.java`，378 文件；正则 `\.withCapability\(`；3 个命中行。

| 文件 | 行 | 原文本 |
|---|---:|---|
| `src/main/java/dev/ronova/coremod/policy/RuntimeCapabilityProjection.java` | 13 | `result=result.withCapability(key,code.supported()?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,` |
| `src/main/java/dev/ronova/coremod/policy/RuntimeCapabilityProjection.java` | 20 | `result=result.withCapability(key,enforcement.supported()?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,` |
| `src/main/java/dev/ronova/coremod/policy/TerminalActionPolicy.java` | 40 | `for(var key:keys)scoped=scoped.withCapability(key,MechanismLevel.PROTECT_RONOVA,` |

仅为上述语料的文本事实，不排除异名实现、运行期构造或其它输入。

### generic-recovery

限定 `src/main/java/dev/ronova/**/*.java`，378 文件；正则 `RecoveryChainPolicy\.resolve\(`；8 个命中行。

| 文件 | 行 | 原文本 |
|---|---:|---|
| `src/main/java/dev/ronova/coremod/engine/TerminalPlanExecutor.java` | 77 | `var policy = dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(runtime.strengthPolicy(),` |
| `src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java` | 579 | `if (!dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),` |
| `src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java` | 612 | `boolean derived = dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),` |
| `src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java` | 653 | `boolean derived=dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),` |
| `src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java` | 705 | `&#124;&#124; !dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),` |
| `src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java` | 887 | `&#124;&#124; !dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),` |
| `src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java` | 917 | `boolean observe = dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),` |
| `src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java` | 1435 | `&#124;&#124;!dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),` |

仅为上述语料的文本事实，不排除异名实现、运行期构造或其它输入。

### registered-recovery

限定 `src/main/java/dev/ronova/**/*.java`，378 文件；正则 `RecoveryChainPolicy\.registered(?:AtomicSnapshot|NbtOutlet|TaskDisposition|StorageCommit)\(`；13 个命中行。

| 文件 | 行 | 原文本 |
|---|---:|---|
| `src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java` | 870 | `&&dev.ronova.coremod.policy.RecoveryChainPolicy.registeredNbtOutlet(policy(), resolved.copyDepth(), true).executionPermitted()` |
| `src/main/java/dev/ronova/coremod/persistence/EffectJournal.java` | 200 | `?dev.ronova.coremod.policy.RecoveryChainPolicy.registeredStorageCommit(policy,description.copyDepth(),true)` |
| `src/main/java/dev/ronova/coremod/persistence/EffectJournal.java` | 201 | `:dev.ronova.coremod.policy.RecoveryChainPolicy.registeredTaskDisposition(policy,description.permissionDepth(),true);` |
| `src/main/java/dev/ronova/coremod/persistence/EffectJournal.java` | 217 | `&#124;&#124; !dev.ronova.coremod.policy.RecoveryChainPolicy.registeredAtomicSnapshot(` |
| `src/main/java/dev/ronova/coremod/persistence/EffectJournal.java` | 232 | `&#124;&#124; !dev.ronova.coremod.policy.RecoveryChainPolicy.registeredNbtOutlet(` |
| `src/main/java/dev/ronova/coremod/persistence/ShadowDurableRootStore.java` | 779 | `var direct = dev.ronova.coremod.policy.RecoveryChainPolicy.registeredNbtOutlet(strengthPolicySnapshot(), evidence.copyDepth(), true);` |
| `src/main/java/dev/ronova/coremod/persistence/ShadowDurableRootStore.java` | 781 | `var derived = dev.ronova.coremod.policy.RecoveryChainPolicy.registeredNbtOutlet(strengthPolicySnapshot(), 2, true);` |
| `src/main/java/dev/ronova/coremod/persistence/ShadowDurableRootStore.java` | 796 | `&#124;&#124; !dev.ronova.coremod.policy.RecoveryChainPolicy.registeredAtomicSnapshot(` |
| `src/main/java/dev/ronova/coremod/persistence/ShadowDurableRootStore.java` | 978 | `&#124;&#124;!dev.ronova.coremod.policy.RecoveryChainPolicy.registeredStorageCommit(` |
| `src/main/java/dev/ronova/coremod/persistence/ShadowDurableRootStore.java` | 1006 | `?dev.ronova.coremod.policy.RecoveryChainPolicy.registeredStorageCommit(strengthPolicySnapshot(),description.copyDepth(),true)` |
| `src/main/java/dev/ronova/coremod/persistence/ShadowDurableRootStore.java` | 1007 | `:dev.ronova.coremod.policy.RecoveryChainPolicy.registeredTaskDisposition(strengthPolicySnapshot(),description.permissionDepth(),true);` |
| `src/main/java/dev/ronova/coremod/persistence/ShadowDurableRootStore.java` | 1034 | `&&dev.ronova.coremod.policy.RecoveryChainPolicy.registeredTaskDisposition(` |
| `src/main/java/dev/ronova/coremod/persistence/ShadowDurableRootStore.java` | 1192 | `&& dev.ronova.coremod.policy.RecoveryChainPolicy.registeredNbtOutlet(strengthPolicySnapshot(), copyDepth, true).executionPermitted();` |

仅为上述语料的文本事实，不排除异名实现、运行期构造或其它输入。

### settlement-type-names

限定 `src/main/java/dev/ronova/**/*.java`，378 文件；正则 `\b(?:class|interface|enum|record)\s+\w*(?:[Ss]ettlement|RootOwner)\w*`；0 个命中行。

| 文件 | 行 | 原文本 |
|---|---:|---|

仅为上述语料的文本事实，不排除异名实现、运行期构造或其它输入。

### root-owner-name

限定 `src/main/java/dev/ronova/**/*.java`，378 文件；正则 `\bRootOwner\b`；0 个命中行。

| 文件 | 行 | 原文本 |
|---|---:|---|

仅为上述语料的文本事实，不排除异名实现、运行期构造或其它输入。

<a id="consumer-summary"></a>
## 8 个通用调用的实际消费者

八处均读取 `observationRequested()`：六处 DIRECT_SOURCE、两处 DERIVED_SOURCE。没有一处在这些调用点读取 `executionPermitted()`；因此执行位受天花板约束不能推出观察链不可达，也不能据此要求八处全部改成 registered*。完整邻接行如下。

| 原调用 | Step | 消费 | 当前融合选择 |
|---|---|---|---|
| [TerminalPlanExecutor.java:77](#generic-call-1) | `DIRECT_SOURCE` | `observationRequested()` | 保留观察语义；不将观察许可升级成写许可 |
| [RecoverySourceTracker.java:579](#generic-call-2) | `DIRECT_SOURCE` | `observationRequested()` | 保留观察语义；不将观察许可升级成写许可 |
| [RecoverySourceTracker.java:612](#generic-call-3) | `DERIVED_SOURCE` | `observationRequested()` | 保留观察语义；不将观察许可升级成写许可 |
| [RecoverySourceTracker.java:653](#generic-call-4) | `DERIVED_SOURCE` | `observationRequested()` | 保留观察语义；不将观察许可升级成写许可 |
| [RecoverySourceTracker.java:705](#generic-call-5) | `DIRECT_SOURCE` | `observationRequested()` | 保留观察语义；不将观察许可升级成写许可 |
| [RecoverySourceTracker.java:887](#generic-call-6) | `DIRECT_SOURCE` | `observationRequested()` | 保留观察语义；不将观察许可升级成写许可 |
| [RecoverySourceTracker.java:917](#generic-call-7) | `DIRECT_SOURCE` | `observationRequested()` | 保留观察语义；不将观察许可升级成写许可 |
| [RecoverySourceTracker.java:1435](#generic-call-8) | `DIRECT_SOURCE` | `observationRequested()` | 保留观察语义；不将观察许可升级成写许可 |

## 外部核对（与原稿用语分开）

JDK 17 `System.nanoTime()` 表示同一 JVM 内用于经过时间差值的时间源，不是日历挂钟。当前规划将 `RecoverySourceEvidence.fresh(System.nanoTime())` 称为经过时间新鲜度样式；不移植绝对时间戳，也不自动复制五秒常量。原稿“挂钟”仍原样保留。

[JDK 17 System.nanoTime 官方规范](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/System.html#nanoTime())。Python 的 `PurePath.as_posix()` 明确产生正斜杠形式；本轮修复只将记录路径标准化，不吞掉内容、语料或构建产物差异。[Python pathlib 官方文档](https://docs.python.org/3/library/pathlib.html#pathlib.PurePath.as_posix)。

## 原行摘录

<a id="operation-plan"></a>
### operation-plan

`src/main/java/dev/ronova/coremod/root/api/TerminalOperationPlan.java:1–41`；文件 SHA-256 `f0b33e28581da997912d44ea1001443ed6d9ce656dab2adb0f72aebb065636d9`。

```java
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

<a id="completion"></a>
### completion

`src/main/java/dev/ronova/coremod/runtime/CompletionTracker.java:1–88`；文件 SHA-256 `26278f689bd25df741ffd8c38454d44c3873076d844d1ff2b7ddcf8b0e606695`。

```java
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

<a id="evidence-age"></a>
### evidence-age

`src/main/java/dev/ronova/coremod/root/api/RecoverySourceEvidence.java:1–17`；文件 SHA-256 `9ce9a8776387fb9be3b9e5f9415b0b8198c61c4948d8a821c15782ea07c68743`。

```java
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

<a id="source-approval"></a>
### source-approval

`src/main/java/dev/ronova/coremod/persistence/ShadowDurableRootStore.java:763–787`；文件 SHA-256 `b7a492bc2b36a48303d3e86e568214442fb537a2fc2f95426d760a91d258af90`。

```java
 763      }
 764      public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireOwnerWrite(dev.ronova.coremod.root.api.AuthorityStamp expected) {
 765          if(!authoritative||readOnly||persistencePending||expected==null||!expected.owner()||!expected.settled()
 766              ||!expected.sameAuthority(authorityStamp(expected.subjectKey().subjectId())))return null;
 767          return executionFence.tryAcquireOwner(expected);
 768      }
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
```

<a id="lease"></a>
### lease

`src/main/java/dev/ronova/coremod/runtime/PlanExecutionLease.java:1–34`；文件 SHA-256 `fefa75f75fe7c00486b6a42c039967cb7097e8e595f403fc8fd0619adcc6bdaf`。

```java
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

<a id="fence"></a>
### fence

`src/main/java/dev/ronova/coremod/root/api/RootExecutionFence.java:1–73`；文件 SHA-256 `05a9c0bbe81411e0cee97383e61e347c4a7819ccf9e890a21728e63b6e61da7d`。

```java
   1  package dev.ronova.coremod.root.api;
   2  
   3  import java.util.Objects;
   4  import java.util.function.LongSupplier;
   5  
   6  /** Excludes new canonical commits during one registered raw-write step of this Root instance. */
   7  public final class RootExecutionFence {
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

<a id="authority"></a>
### authority

`src/main/java/dev/ronova/coremod/root/api/AuthorityStamp.java:1–24`；文件 SHA-256 `cb33be91cff4e20015da45595e65b4a0ed522c6b11af5ec3faed127f01d32f81`。

```java
   1  package dev.ronova.coremod.root.api;
   2  
   3  import dev.ronova.coremod.contract.SubjectKey;
   4  import java.util.Objects;
   5  
   6  /** One acknowledged Root read. It records expectations, not permission to create new Truth. */
   7  public record AuthorityStamp(SubjectKey subjectKey, SubjectQualification qualification,
   8      long policyRevision, boolean owner, long ownerAuthorizationRevision, boolean settled,
   9      dev.ronova.coremod.contract.DominionEpoch authorityEpoch) {
  10      public AuthorityStamp {
  11          Objects.requireNonNull(subjectKey, "subjectKey");
  12          Objects.requireNonNull(authorityEpoch, "authorityEpoch");
  13          if (policyRevision <= 0 || ownerAuthorizationRevision < 0
  14              || qualification != null && !subjectKey.equals(qualification.subjectKey())) {
  15              throw new IllegalArgumentException("invalid authority stamp");
  16          }
  17      }
  18      /** Unrelated Root transactions do not change this subject's or actor's qualification. */
  19      public boolean sameAuthority(AuthorityStamp other) {
  20          return other != null && subjectKey.equals(other.subjectKey) && Objects.equals(qualification, other.qualification)
  21              && policyRevision == other.policyRevision && owner == other.owner
  22              && ownerAuthorizationRevision == other.ownerAuthorizationRevision && settled == other.settled;
  23      }
  24  }
```

<a id="ownership"></a>
### ownership

`src/main/java/dev/ronova/coremod/root/api/OwnershipBinding.java:1–10`；文件 SHA-256 `71521659c11068f50cefbfaf5b805fd7a175d9c4b0ce408111f20740d2176e14`。

```java
   1  package dev.ronova.coremod.root.api;
   2  
   3  import dev.ronova.coremod.contract.SubjectKey;
   4  
   5  /** Null owner is a retained revocation record, never an implicit default owner. */
   6  public record OwnershipBinding(long revision, SubjectKey owner) {
   7      public OwnershipBinding {
   8          if (revision <= 0L) throw new IllegalArgumentException("ownership revision must be positive");
   9      }
  10  }
```

<a id="revisions"></a>
### revisions

`src/main/java/dev/ronova/coremod/root/api/SubjectRevisions.java:1–48`；文件 SHA-256 `c286b3956396e497e5909d3fe6cfb30f09b0fe757c4c0b806d7eea088f3b8f5d`。

```java
   1  package dev.ronova.coremod.root.api;
   2  
   3  import dev.ronova.coremod.contract.*;
   4  import java.util.*;
   5  
   6  /** Independently scoped counters in the existing Root subject record. */
   7  public record SubjectRevisions(long bindingRevision, PhysicalIncarnationId activeIncarnation,
   8      long judgmentRevision, long admissionTokenRevision, long revokedThroughEpoch,
   9      Map<StateDomainId, OwnershipBinding> ownership) {
  10      public SubjectRevisions {
  11          if (bindingRevision < 0 || judgmentRevision < 0 || admissionTokenRevision < 0 || revokedThroughEpoch < 0
  12              || (bindingRevision == 0) != (activeIncarnation == null)) {
  13              throw new IllegalArgumentException("invalid subject revision vector");
  14          }
  15          ownership = Collections.unmodifiableMap(new TreeMap<>(Objects.requireNonNull(ownership, "ownership")));
  16          if (ownership.size() > 128 || ownership.values().stream().anyMatch(Objects::isNull)) {
  17              throw new IllegalArgumentException("invalid ownership domain count or value");
  18          }
  19      }
  20      public static SubjectRevisions initial() { return new SubjectRevisions(0, null, 0, 0, 0, Map.of()); }
  21      public SubjectRevisions bind(SubjectKey key, UUID nonce) {
  22          if (activeIncarnation != null && activeIncarnation.nonce().equals(nonce)) return this;
  23          return new SubjectRevisions(Math.addExact(bindingRevision, 1), new PhysicalIncarnationId(key,
  24              activeIncarnation == null ? 1 : Math.addExact(activeIncarnation.sequence(), 1), nonce),
  25              judgmentRevision, admissionTokenRevision, revokedThroughEpoch, ownership);
  26      }
  27      public SubjectRevisions qualify(long previousEpoch, boolean judgmentChanged) {
  28          return new SubjectRevisions(bindingRevision, activeIncarnation,
  29              judgmentChanged ? Math.addExact(judgmentRevision, 1) : judgmentRevision,
  30              Math.addExact(admissionTokenRevision, 1), previousEpoch, ownership);
  31      }
  32      public SubjectRevisions own(StateDomainId domain, SubjectKey owner) {
  33          OwnershipBinding old = ownership.get(domain);
  34          if (Objects.equals(old == null ? null : old.owner(), owner)) return this;
  35          var next = new TreeMap<>(ownership);
  36          next.put(domain, new OwnershipBinding(old == null ? 1 : Math.addExact(old.revision(), 1), owner));
  37          return new SubjectRevisions(bindingRevision, activeIncarnation, judgmentRevision,
  38              admissionTokenRevision, revokedThroughEpoch, next);
  39      }
  40      public String canonical() {
  41          StringBuilder value = new StringBuilder().append(bindingRevision).append('|')
  42              .append(activeIncarnation == null ? "-" : activeIncarnation.canonical()).append('|')
  43              .append(judgmentRevision).append('|').append(admissionTokenRevision).append('|')
  44              .append(revokedThroughEpoch);
  45          ownership.forEach((domain, binding) -> value.append('|').append(domain.value()).append('=')
  46              .append(binding.revision()).append(':').append(binding.owner() == null ? "-" : binding.owner().canonical()));
  47          return value.toString();
  48      }
```

<a id="ownership-codec"></a>
### ownership-codec

`src/main/java/dev/ronova/coremod/persistence/SubjectRevisionNbtCodec.java:35–56`；文件 SHA-256 `ad7b709444620feb4d8de86a0f6b04f93f0badae06f2593bd409f5b8ffe5a6bd`。

```java
  35              if (!tag.contains("incarnation_sequence", Tag.TAG_LONG) || !tag.hasUUID("incarnation_nonce")) {
  36                  throw new IllegalArgumentException("incomplete incarnation");
  37              }
  38              incarnation = new PhysicalIncarnationId(subject, tag.getLong("incarnation_sequence"), tag.getUUID("incarnation_nonce"));
  39          }
  40          if (!(tag.get("ownership") instanceof ListTag original)) throw new IllegalArgumentException("missing domain ownership");
  41          ListTag rows = tag.getList("ownership", Tag.TAG_COMPOUND);
  42          if (rows.size() != original.size() || rows.size() > 128) throw new IllegalArgumentException("invalid domain ownership rows");
  43          Map<StateDomainId, OwnershipBinding> ownership = new TreeMap<>();
  44          for (Tag raw : rows) {
  45              CompoundTag row = (CompoundTag) raw;
  46              if (!row.contains("domain", Tag.TAG_STRING) || !row.contains("revision", Tag.TAG_LONG)
  47                  || row.contains("owner") && !row.hasUUID("owner")) throw new IllegalArgumentException("malformed domain owner");
  48              StateDomainId domain = new StateDomainId(row.getString("domain"));
  49              OwnershipBinding binding = new OwnershipBinding(row.getLong("revision"), row.hasUUID("owner")
  50                  ? new SubjectKey(subject.realmId(), row.getUUID("owner")) : null);
  51              if (ownership.putIfAbsent(domain, binding) != null) throw new IllegalArgumentException("duplicate ownership domain");
  52          }
  53          return new SubjectRevisions(tag.getLong("binding"), incarnation, tag.getLong("judgment"),
  54              tag.getLong("admission"), tag.getLong("revoked_through"), ownership);
  55      }
  56  }
```

<a id="operation-runner"></a>
### operation-runner

`src/main/java/dev/ronova/coremod/runtime/OperationPlanRunner.java:1–82`；文件 SHA-256 `92cb9976a2734db2221b0df44dbe18244417dff1c6c141301032013fe29d661f`。

```java
   1  package dev.ronova.coremod.runtime;
   2  
   3  import dev.ronova.coremod.root.api.*;
   4  import java.util.*;
   5  import java.util.function.LongSupplier;
   6  import java.util.function.BooleanSupplier;
   7  
   8  /** Fixed ordered plan runner; ports cannot renew a stale Root plan or choose another effect identity. */
   9  public final class OperationPlanRunner {
  10      public interface Ports {
  11          boolean current(TerminalOperationPlan plan);
  12          OperationReport.StepReceipt controlled(TerminalOperationPlan plan, TerminalOperationPlan.Step step);
  13          default OperationReport.StepReceipt controlled(TerminalOperationPlan plan, TerminalOperationPlan.Step step,
  14              BooleanSupplier permitted) {
  15              return permitted.getAsBoolean() ? controlled(plan, step)
  16                  : new OperationReport.StepReceipt(step, OperationReport.Status.FENCED, "final-write-permission-expired");
  17          }
  18          EffectClaim prepare(TerminalOperationPlan plan);
  19          boolean enter(EffectClaim claim);
  20          void opaque(TerminalOperationPlan plan);
  21          default void opaque(TerminalOperationPlan plan, BooleanSupplier permitted) {
  22              if (permitted.getAsBoolean()) opaque(plan);
  23          }
  24          void finishUnknown(EffectClaim claim);
  25      }
  26      private final ThreadLocal<Set<String>> active = ThreadLocal.withInitial(HashSet::new);
  27      private final LongSupplier clock;
  28      public OperationPlanRunner() { this(System::nanoTime); }
  29      OperationPlanRunner(LongSupplier clock) { this.clock = Objects.requireNonNull(clock); }
  30      public OperationReport run(TerminalOperationPlan plan, Ports ports) {
  31          Objects.requireNonNull(plan); Objects.requireNonNull(ports);
  32          Set<String> stack = active.get();
  33          String cause = plan.commandId() + "/" + plan.subjectKey().canonical();
  34          if (!stack.add(cause)) return new OperationReport(plan, List.of(), false, false, "REENTRANT");
  35          List<OperationReport.StepReceipt> receipts = new ArrayList<>(); boolean entered = false, scopedFailure = false;
  36          try (var lease = new PlanExecutionLease(plan, clock, PlanExecutionLease.MAX_DURATION_NANOS)) {
  37              for (var step : plan.orderedSteps()) {
  38                  if (!lease.valid(plan) || !ports.current(plan)) {
  39                      receipts.add(new OperationReport.StepReceipt(step, OperationReport.Status.FENCED, "qualification-or-plan-revision-changed"));
  40                      return new OperationReport(plan, receipts, entered, false, "FENCED");
  41                  }
  42                  if (step == TerminalOperationPlan.Step.TERMINAL_LIFECYCLE) {
  43                      EffectClaim claim = ports.prepare(plan);
  44                      if (claim.granted()) {
  45                          if (!claim.record().key().equals(plan.effectKey())) throw new IllegalStateException("Root returned a different semantic effect key");
  46                          try {
  47                              if (lease.valid(plan) && ports.current(plan) && ports.enter(claim)) {
  48                                  // Entering the durable claim can itself invalidate the execution context.
  49                                  if (lease.valid(plan) && ports.current(plan)) {
  50                                      entered = true;
  51                                      ports.opaque(plan, () -> lease.valid(plan) && ports.current(plan));
  52                                  }
  53                              }
  54                          } catch (RuntimeException failed) {
  55                              // A callback exception cannot prove absence of its physical effects.
  56                          } finally { ports.finishUnknown(claim); }
  57                      }
  58                      boolean unresolved = claim.record() != null || claim.status() == EffectClaim.Status.LEGACY_CLAIM_UNKNOWN;
  59                      receipts.add(new OperationReport.StepReceipt(step, unresolved ? OperationReport.Status.EFFECT_UNKNOWN : OperationReport.Status.FENCED,
  60                          "root-effect-claim:" + claim.status()));
  61                      if (!unresolved) scopedFailure = true;
  62                  } else {
  63                      OperationReport.StepReceipt result;
  64                      try { result = Objects.requireNonNull(ports.controlled(plan, step, () -> lease.valid(plan) && ports.current(plan))); }
  65                      catch (RuntimeException failed) { result = new OperationReport.StepReceipt(step, OperationReport.Status.UNAVAILABLE, failed.getClass().getSimpleName()); }
  66                      if (result.step() != step) throw new IllegalStateException("executor returned another step receipt");
  67                      receipts.add(result);
  68                      if (result.status() == OperationReport.Status.FENCED || result.status() == OperationReport.Status.UNAVAILABLE) {
  69                          scopedFailure = true;
  70                          if (step == TerminalOperationPlan.Step.ADMISSION_FENCE || step == TerminalOperationPlan.Step.SERVER_PRESENCE)
  71                              return new OperationReport(plan, receipts, entered, false, "FENCED");
  72                      }
  73                  }
  74                  if (!lease.valid(plan) || !ports.current(plan)) {
  75                      // Preserve EFFECT_UNKNOWN: revocation cannot erase an effect already attempted.
  76                      return new OperationReport(plan, receipts, entered, false, "FENCED");
  77                  }
  78              }
  79              return new OperationReport(plan, receipts, entered, false, scopedFailure ? "FENCED" : "PARTIAL");
  80          } finally { stack.remove(cause); if (stack.isEmpty()) active.remove(); }
  81      }
  82  }
```

<a id="effect-claim"></a>
### effect-claim

`src/main/java/dev/ronova/coremod/root/api/EffectClaim.java:1–18`；文件 SHA-256 `aa90f6947bfaf38d72ff81563594c82742f801c85505f4b1737da8c384412e2f`。

```java
   1  package dev.ronova.coremod.root.api;
   2  
   3  import java.util.Objects;
   4  
   5  /** Preparation result. The issuing Root must also admit this exact one-use instance before execution. */
   6  public record EffectClaim(Status status, EffectRecord record) {
   7      public EffectClaim {
   8          Objects.requireNonNull(status, "status");
   9          if (status == Status.GRANTED && (record == null || record.state() != EffectRecord.State.PREPARED)) {
  10              throw new IllegalArgumentException("effect grant requires a prepared record");
  11          }
  12      }
  13      public boolean granted() { return status == Status.GRANTED; }
  14      public enum Status {
  15          GRANTED, ALREADY_RECORDED, PERSISTENCE_UNKNOWN, STORAGE_BLOCKED,
  16          LEGACY_UNQUALIFIED, LEGACY_CLAIM_UNKNOWN, REJECTED
  17      }
  18  }
```

<a id="claim-result"></a>
### claim-result

`src/main/java/dev/ronova/coremod/terminal/LifecycleClaimResult.java:1–33`；文件 SHA-256 `fdb6bae8dbf47af496b080b6bca481b474b582687855cf421c5d35095f44e05d`。

```java
   1  package dev.ronova.coremod.terminal;
   2  
   3  import java.util.Objects;
   4  
   5  /**
   6   * Result of reserving the inherited once-only death entry. A claim is an intent,
   7   * never evidence that death, loot or an opaque callback actually completed.
   8   */
   9  public record LifecycleClaimResult(Status status, LifecycleIntegrationReceipt receipt) {
  10      public LifecycleClaimResult {
  11          Objects.requireNonNull(status, "status");
  12          if ((status == Status.GRANTED || status == Status.ALREADY_CLAIMED_EFFECT_UNKNOWN)
  13              && receipt == null) {
  14              throw new IllegalArgumentException("resolved claim requires its receipt");
  15          }
  16          if (status == Status.REJECTED && receipt != null) {
  17              throw new IllegalArgumentException("rejected claim cannot grant a receipt");
  18          }
  19      }
  20  
  21      /** Only the original successful prepare may enter the public death callback. */
  22      public boolean mayExecute() {
  23          return status == Status.GRANTED;
  24      }
  25  
  26      public enum Status {
  27          GRANTED,
  28          ALREADY_CLAIMED_EFFECT_UNKNOWN,
  29          CLAIM_PERSISTENCE_UNKNOWN,
  30          REJECTED,
  31          STORAGE_BLOCKED
  32      }
  33  }
```

<a id="effect-journal"></a>
### effect-journal

`src/main/java/dev/ronova/coremod/persistence/EffectJournal.java:189–257`；文件 SHA-256 `6eedc208498adf2bcf261745101d06d1b3d294cab4acba8b47fbf788a7856a7f`。

```java
 189                      throw new IllegalArgumentException("recovery work schema/description missing");
 190                  description=dev.ronova.coremod.root.api.RecoveryWorkDescriptor.parse(row.getString("work_description"));
 191                  if(!description.matches(record.key())||!EffectRecord.hash(description.canonical()).equals(row.getString("work_description_hash"))
 192                      ||record.recoveryCapability()!=EffectRecord.RecoveryCapability.QUERYABLE||record.sideEffectClass()!=EffectRecord.SideEffectClass.EXTERNAL_OPAQUE
 193                      ||record.state().confirmed()||!record.planId().equals(record.key().canonicalCommandId())||!record.stepId().equals("recovery-work")
 194                      ||record.planRevision()<record.authorityCommitIndex()||record.planRevision()>commands.commitIndex()
 195                      ||!description.payloadHash(record.key(),record.authorityPayloadHash()).equals(record.effectPayloadHash()))
 196                      throw new IllegalArgumentException("invalid recovery work contract");
 197                  var configuration=commands.policyAt(record.planRevision());
 198                  var policy=dev.ronova.coremod.policy.StrengthPolicySnapshot.fromConfiguration(configuration);
 199                  var decision=description.kind()==dev.ronova.coremod.root.api.RecoveryWorkDescriptor.Kind.STORAGE_COMMIT
 200                      ?dev.ronova.coremod.policy.RecoveryChainPolicy.registeredStorageCommit(policy,description.copyDepth(),true)
 201                      :dev.ronova.coremod.policy.RecoveryChainPolicy.registeredTaskDisposition(policy,description.permissionDepth(),true);
 202                  if(configuration.revision().value()!=record.policyRevision()||!decision.executionPermitted())throw new IllegalArgumentException("recovery work lacks historical permission");
 203              } else if(row.contains("work_description")||row.contains("work_description_hash"))throw new IllegalArgumentException("description on a different semantic operation");
 204              if (recoveryMutation) {
 205                  int depth = dev.ronova.coremod.root.api.RecoveryMutationIntent.depth(record.key());
 206                  if (record.recoveryCapability() != EffectRecord.RecoveryCapability.QUERYABLE
 207                      || record.sideEffectClass() != EffectRecord.SideEffectClass.EXTERNAL_OPAQUE
 208                      || record.state().confirmed()
 209                      || !record.planId().equals(record.key().canonicalCommandId())
 210                      || !record.stepId().equals("recovery-memory-mutation")
 211                      || record.planRevision() < record.authorityCommitIndex() || record.planRevision() > commands.commitIndex()
 212                      || !record.effectPayloadHash().equals(dev.ronova.coremod.root.api.RecoveryMutationIntent.payloadHash(
 213                          record.key(), record.authorityPayloadHash())))
 214                      throw new IllegalArgumentException("recovery mutation contract is inconsistent");
 215                  var policy = commands.policyAt(record.planRevision());
 216                  if (policy.revision().value() != record.policyRevision()
 217                      || !dev.ronova.coremod.policy.RecoveryChainPolicy.registeredAtomicSnapshot(
 218                          dev.ronova.coremod.policy.StrengthPolicySnapshot.fromConfiguration(policy), depth, true).executionPermitted())
 219                      throw new IllegalArgumentException("recovery mutation lacks historical policy permission");
 220              } else if (sourceQualification) {
 221                  if (record.recoveryCapability() != EffectRecord.RecoveryCapability.IDEMPOTENT
 222                      || record.sideEffectClass() != EffectRecord.SideEffectClass.RONOVA_CONTROLLED
 223                      || record.state() != EffectRecord.State.EFFECT_CONFIRMED
 224                      || record.revision() != 1
 225                      || !record.planId().equals(record.key().canonicalCommandId())
 226                      || !record.stepId().equals("source-qualification") || record.planRevision() < record.authorityCommitIndex()
 227                      || record.planRevision() > commands.commitIndex()
 228                      || !record.effectPayloadHash().equals(EffectRecord.sourceQualificationPayloadHash(record.key(), record.authorityPayloadHash())))
 229                      throw new IllegalArgumentException("source qualification contract is inconsistent");
 230                  var policy = commands.policyAt(record.planRevision());
 231                  if (policy.revision().value() != record.policyRevision()
 232                      || !dev.ronova.coremod.policy.RecoveryChainPolicy.registeredNbtOutlet(
 233                          dev.ronova.coremod.policy.StrengthPolicySnapshot.fromConfiguration(policy), 1, true).executionPermitted())
 234                      throw new IllegalArgumentException("source qualification lacks its historical policy permission");
 235              } else if (!recoveryWork&&((!publicDeath && !terminal)
 236                  || !EffectRecord.ONCE_SLOT.equals(record.key().semanticSlotId())
 237                  || record.recoveryCapability() != EffectRecord.RecoveryCapability.OPAQUE
 238                  || record.sideEffectClass() != EffectRecord.SideEffectClass.EXTERNAL_OPAQUE
 239                  || !record.effectPayloadHash().equals(publicDeath ? EffectRecord.publicDeathPayloadHash(record.key(), record.authorityPayloadHash())
 240                      : EffectRecord.terminalPayloadHash(record.key(), record.authorityPayloadHash())))) {
 241                  throw new IllegalArgumentException("effect semantic contract is unsupported");
 242              }
 243              if (terminal && (!record.planId().equals(record.key().canonicalCommandId()) || !record.stepId().equals("terminal-lifecycle")
 244                  || record.planRevision() < record.authorityCommitIndex() || record.planRevision() > commands.commitIndex()))
 245                  throw new IllegalArgumentException("terminal effect plan provenance is inconsistent");
 246              if(publicDeath&&(!record.planId().equals(record.key().canonicalCommandId())
 247                  ||!record.stepId().equals("public-death")||record.planRevision()!=1))
 248                  throw new IllegalArgumentException("public death effect plan provenance is inconsistent");
 249              String slot = record.key().canonicalCommandId() + "/" + record.key().targetEpoch().subjectKey().canonical()
 250                  + "/" + record.key().targetEpoch().generation();
 251              if (!sourceQualification && !recoveryMutation && !recoveryWork && !lifecycleSlots.add(slot)) throw new IllegalArgumentException("legacy/new lifecycle aliases duplicated one effect slot");
 252              canonical.append(EffectRecord.hash(record.canonical())).append('\n');
 253              result.put(record);
 254              if (row.contains("source_location") || row.contains("source_location_hash")) {
 255                  if (tag.getInt("schema") < 2 || !row.contains("source_location", Tag.TAG_STRING)
 256                      || !row.contains("source_location_hash", Tag.TAG_STRING))
 257                      throw new IllegalArgumentException("source location schema mismatch");
```

<a id="budgets"></a>
### budgets

`src/main/java/dev/ronova/coremod/budget/BudgetCategory.java:1–33`；文件 SHA-256 `173315408b7501036d7a5caddc009eba26f80d6ad22dc572b6407fbc61adfbbf`。

```java
   1  package dev.ronova.coremod.budget;
   2  
   3  /** Stable 7.14D budget category identifiers and initial per-tick envelope. */
   4  public enum BudgetCategory {
   5      TERMINAL("terminal", 500L, 256),
   6      CAPABILITY("capability", 400L, 64),
   7      DISCOVERY("discovery", 300L, Integer.MAX_VALUE),
   8      ADMISSION("admission", 400L, 128),
   9      OBSERVATION("observation", 200L, 256),
  10      MAINTENANCE("maintenance", 200L, 64);
  11  
  12      private final String id;
  13      private final long defaultBudgetUs;
  14      private final int defaultCount;
  15  
  16      BudgetCategory(String id, long defaultBudgetUs, int defaultCount) {
  17          this.id = id;
  18          this.defaultBudgetUs = defaultBudgetUs;
  19          this.defaultCount = defaultCount;
  20      }
  21  
  22      public String id() {
  23          return id;
  24      }
  25  
  26      public long defaultBudgetUs() {
  27          return defaultBudgetUs;
  28      }
  29  
  30      public int defaultCount() {
  31          return defaultCount;
  32      }
  33  }
```

<a id="budget-reserve"></a>
### budget-reserve

`src/main/java/dev/ronova/coremod/engine/TerminalPlanExecutor.java:28–53`；文件 SHA-256 `6d06243b598f19a5c3f6fbb4849f26ab2b1c147360ec270fddf6cf644565dd8d`。

```java
  28          completion = new TerminalCompletionMonitor(runtime);
  29          indices = new TerminalIndexRepair(runtime);
  30          stateFeedback = new TerminalStateFeedback(runtime);
  31          runtime.resources().declareAccounting(HostResourceDomain.JAVA_HEAP, HostResourceGovernor.Accounting.PARTIAL_ESTIMATE,
  32              "terminal-plan-live/16384-bytes");
  33          runtime.resources().declareAccounting(HostResourceDomain.JAVA_HEAP, HostResourceGovernor.Accounting.PARTIAL_ESTIMATE,
  34              "frozen-client-contract/4096+256-per-member");
  35      }
  36      public boolean execute(Entity entity, AuthorityView view) {
  37          if (entity == null || view == null || runtime.durableStore() == null) return false;
  38          var identity=readIdentity(entity);
  39          if(identity==null){countFenced();return false;}
  40          var level=identity.level();
  41          var store = runtime.durableStore();
  42          TerminalOperationPlan plan = store.approveTerminalPlan(identity.subject(), identity.nonce());
  43          if (plan == null) { countFenced(); return false; }
  44          int initialEntityId=identity.entityId();
  45          runtime.spawnAdmission().rememberClassLineage(entity,plan);
  46          Participation participation=participation(plan,level.getServer());
  47          var resource = runtime.resources().reserve(Map.of(HostResourceDomain.JAVA_HEAP, plan.reservedHeapBytes()), HostResourceGovernor.Priority.RECOVERY);
  48          if (!resource.granted()) { countFenced(); return false; }
  49          try (var ignored = resource.reservation();
  50               var observations = new dev.ronova.coremod.observation.RecoveryObservationScope(runtime.recoverySources(), level.getServer())) {
  51              OperationReport report = runner.run(plan, new OperationPlanRunner.Ports() {
  52                  @Override public boolean current(TerminalOperationPlan value) {
  53                      var current=readIdentity(entity);
```

<a id="watch-doc"></a>
### watch-doc

`src/main/java/dev/ronova/coremod/terminal/DurableTerminalWatchSet.java:29–44`；文件 SHA-256 `a7d3a6a4b37b4e60e9eb336f71576411185f6ed75320e53db5b3740ae7f02a89`。

```java
  29  import net.minecraft.resources.ResourceLocation;
  30  import net.minecraft.server.MinecraftServer;
  31  import net.minecraft.server.level.ServerLevel;
  32  import net.minecraft.world.entity.Entity;
  33  import net.minecraft.world.level.Level;
  34  
  35  /**
  36   * Per-server 7.16 terminal worker. Logical membership is reconstructed from the
  37   * immutable Tombstone projection and therefore has no retry-count expiry;
  38   * physical references remain weak and all work is count/time budgeted.
  39   */
  40  public final class DurableTerminalWatchSet implements TerminalWatchSet {
  41      private static final int[] WARM_DELAYS = {2, 5, 10, 20};
  42      private static final int COLD_DELAY = 100;
  43      private static final int TRANSIENT_ABSENT_LIMIT = 4;
  44      private static final Comparator<Scheduled> SCHEDULE_ORDER =
```

<a id="watch-maintain"></a>
### watch-maintain

`src/main/java/dev/ronova/coremod/terminal/DurableTerminalWatchSet.java:221–322`；文件 SHA-256 `a7d3a6a4b37b4e60e9eb336f71576411185f6ed75320e53db5b3740ae7f02a89`。

```java
 221      private void refreshSeed(AdmissionProjection current) {
 222          if (observedProjectionEpoch != null
 223              && observedProjectionEpoch.equals(current.epoch())) {
 224              return;
 225          }
 226          if(seedCursor<pendingSeeds.size())return;
 227          observedProjectionEpoch = current.epoch();
 228          pendingSeeds = current.marks();
 229          seedCursor = 0;
 230      }
 231  
 232      private void trackSeed(JudgmentMark mark, long now) {
 233          if (mark == null || mark.uuid() == null) {
 234              return;
 235          }
 236          Entry entry = entries.get(mark.uuid());
 237          if (entry == null) {
 238              entry = Entry.durable(mark);
 239              entries.put(mark.uuid(), entry);
 240              schedule(entry, Phase.COLD, now);
 241              return;
 242          }
 243          entry.mark = mark;
 244          entry.durableBacked = true;
 245          entry.tombstoneId = mark.uuid();
 246          if (entry.scheduled == null) {
 247              schedule(entry, Phase.COLD, now);
 248          }
 249      }
 250  
 251      private void maintainEntry(Entry entry, AdmissionProjection current, long now) {
 252          if (!logicallyRetained(entry, current)) {
 253              remove(entry.mark.uuid(), Completion.TOMBSTONE_CLEARED);
 254              return;
 255          }
 256          Set<Entity> candidates = Collections.newSetFromMap(new IdentityHashMap<>());
 257          Entity referenced = entry.reference.get();
 258          if (belongsToServer(referenced)) {
 259              candidates.add(referenced);
 260          }
 261          for (Entity candidate : observation.snapshot(entry.mark.uuid())) {
 262              if (belongsToServer(candidate)) {
 263                  candidates.add(candidate);
 264              }
 265          }
 266          ServerLevel level = server.getLevel(dimension(entry.mark.dimension()));
 267          Entity direct = level == null ? null : level.getEntity(entry.mark.uuid());
 268          if (belongsToServer(direct)) {
 269              candidates.add(direct);
 270          }
 271  
 272          boolean resolved = false;
 273          boolean reasserted = false;
 274          for (Entity candidate : candidates) {
 275              boolean needsRepair = physicallyActive(candidate);
 276              boolean periodicIndexReview = entry.durableBacked && entry.phase == Phase.COLD;
 277              if ((!needsRepair && !periodicIndexReview) || authorityView.isProtected(candidate)) {
 278                  continue;
 279              }
 280              SubjectIdentity candidateSubject = MinecraftSubjectAdapter.from(candidate);
 281              if (!entry.durableBacked && !current.matches(candidateSubject)) {
 282                  continue;
 283              }
 284              resolved = true;
 285              entry.reference = new WeakReference<>(candidate);
 286              entry.subject = candidateSubject;
 287              // Presence is written by the validated plan; a weak observation cannot authorize it.
 288              SpawnFirewall.reassert(candidate);
 289              reasserted |= needsRepair || physicallyActive(candidate);
 290          }
 291          entry.attempts = increment(entry.attempts);
 292          attemptNotifications = increment(attemptNotifications);
 293          entry.lastResolved = resolved;
 294          entry.lastReasserted = reasserted;
 295  
 296          if (reasserted) {
 297              entry.absentCycles = 0;
 298              schedule(entry, Phase.HOT, now + 1L);
 299              return;
 300          }
 301          // Convergence is not disappearance. Retain the weak reference for later drift;
 302          // it still cannot keep the physical entity alive after its real owners release it.
 303          entry.absentCycles = Math.min(Integer.MAX_VALUE, entry.absentCycles + 1);
 304          if (!entry.durableBacked && entry.absentCycles >= TRANSIENT_ABSENT_LIMIT) {
 305              remove(entry.mark.uuid(), Completion.ENTITY_UNRESOLVED);
 306              return;
 307          }
 308          if (entry.absentCycles <= WARM_DELAYS.length) {
 309              schedule(
 310                  entry, Phase.WARM,
 311                  now + WARM_DELAYS[Math.max(0, entry.absentCycles - 1)]
 312              );
 313          } else {
 314              schedule(entry, Phase.COLD, now + COLD_DELAY);
 315          }
 316      }
 317  
 318      private boolean logicallyRetained(Entry entry, AdmissionProjection current) {
 319          return entry.durableBacked
 320              ? current.hasTombstone(entry.tombstoneId)
 321              : current.matches(entry.subject);
 322      }
```

<a id="watch-entry"></a>
### watch-entry

`src/main/java/dev/ronova/coremod/terminal/DurableTerminalWatchSet.java:429–463`；文件 SHA-256 `a7d3a6a4b37b4e60e9eb336f71576411185f6ed75320e53db5b3740ae7f02a89`。

```java
 429      private static final class Entry {
 430          private JudgmentMark mark;
 431          private SubjectIdentity subject;
 432          private boolean durableBacked;
 433          private UUID tombstoneId;
 434          private WeakReference<Entity> reference;
 435          private Phase phase = Phase.COLD;
 436          private Scheduled scheduled;
 437          private long nextTick;
 438          private long attempts;
 439          private int absentCycles;
 440          private boolean lastResolved;
 441          private boolean lastReasserted;
 442  
 443          private static Entry durable(JudgmentMark mark) {
 444              Entry entry = new Entry();
 445              entry.mark = mark;
 446              entry.subject = subject(mark);
 447              entry.durableBacked = true;
 448              entry.tombstoneId = mark.uuid();
 449              entry.reference = new WeakReference<>(null);
 450              return entry;
 451          }
 452  
 453          private static Entry transientIncarnation(
 454              JudgmentMark mark, SubjectIdentity subject, Entity incarnation
 455          ) {
 456              Entry entry = new Entry();
 457              entry.mark = mark;
 458              entry.subject = subject;
 459              entry.reference = new WeakReference<>(incarnation);
 460              return entry;
 461          }
 462      }
 463  }
```

<a id="preset"></a>
### preset

`src/main/java/dev/ronova/coremod/policy/ProfilePresetMatrix.java:96–176`；文件 SHA-256 `dfd1f61bfb0ac38566ad03d4c497cc07904c115f8bdbdc8dbe326bd211ac877a`。

```java
  96      private static Map<StrengthProfile, Map<StrengthOptionKey, MechanismLevel>>
  97      revisionOne() {
  98          EnumMap<StrengthProfile, Map<StrengthOptionKey, MechanismLevel>> profiles =
  99              new EnumMap<>(StrengthProfile.class);
 100  
 101          EnumMap<StrengthOptionKey, MechanismLevel> compatible =
 102              filled(MechanismLevel.MONITOR);
 103  
 104          // Compatible attack: stable target/Authority paths are active; broad
 105          // source, task, callback and dynamic-code paths remain observational.
 106          compatible.put(StrengthOptionKey.ATTACK_STATE_SOURCE,
 107              MechanismLevel.PROTECT_RONOVA);
 108          compatible.put(StrengthOptionKey.ATTACK_REGISTRATION_SOURCE,
 109              MechanismLevel.PROTECT_RONOVA);
 110          compatible.put(StrengthOptionKey.ATTACK_NETWORK_RECONSTRUCTION,
 111              MechanismLevel.PROTECT_RONOVA);
 112          compatible.put(StrengthOptionKey.ATTACK_SAVE_RECONSTRUCTION,
 113              MechanismLevel.PROTECT_RONOVA);
 114          compatible.put(StrengthOptionKey.ATTACK_DISPATCH,
 115              MechanismLevel.PROTECT_RONOVA);
 116          compatible.put(StrengthOptionKey.ATTACK_RUNTIME_PATCH_DISABLEMENT,
 117              MechanismLevel.PROTECT_RONOVA);
 118          compatible.put(StrengthOptionKey.ATTACK_TARGET_CLASS_LINEAGE,
 119              MechanismLevel.PROTECT_RONOVA);
 120          compatible.put(StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER,
 121              MechanismLevel.PROTECT_RONOVA);
 122  
 123          // Interception and protection in Compatible are scoped to Ronova.
 124          putPlane(compatible, PlaneId.INTERCEPTION, MechanismLevel.PROTECT_RONOVA);
 125          putPlane(compatible, PlaneId.PROTECTION, MechanismLevel.PROTECT_RONOVA);
 126  
 127          // Compatible runtime distinguishes active Ronova continuity from broad
 128          // inventory/diagnostic observation.
 129          compatible.put(StrengthOptionKey.RUNTIME_JAVA_AGENT,
 130              MechanismLevel.PROTECT_RONOVA);
 131          compatible.put(StrengthOptionKey.RUNTIME_NATIVE,
 132              MechanismLevel.PROTECT_RONOVA);
 133          compatible.put(StrengthOptionKey.RUNTIME_NATIVE_LOADER,
 134              MechanismLevel.PROTECT_RONOVA);
 135          compatible.put(StrengthOptionKey.RUNTIME_RENDER_GUARD,
 136              MechanismLevel.PROTECT_RONOVA);
 137          compatible.put(StrengthOptionKey.RUNTIME_CRITICAL_CLASS_WATCH,
 138              MechanismLevel.PROTECT_RONOVA);
 139          compatible.put(StrengthOptionKey.RUNTIME_THREAD_CONTINUITY,
 140              MechanismLevel.PROTECT_RONOVA);
 141          compatible.put(StrengthOptionKey.RUNTIME_BACKGROUND_INTEGRITY,
 142              MechanismLevel.PROTECT_RONOVA);
 143  
 144          profiles.put(StrengthProfile.COMPATIBLE, immutable(compatible));
 145          profiles.put(StrengthProfile.ENHANCED,
 146              immutable(filled(MechanismLevel.RELATED_PATHS)));
 147          profiles.put(StrengthProfile.EXTREME,
 148              immutable(filled(MechanismLevel.STRICT_ALL)));
 149          profiles.put(StrengthProfile.CUSTOM,
 150              immutable(filled(MechanismLevel.MONITOR)));
 151  
 152          validateComplete(profiles);
 153          return Collections.unmodifiableMap(profiles);
 154      }
 155  
 156      private static Map<StrengthOptionKey, MechanismLevel> safetyFloors() {
 157          EnumMap<StrengthOptionKey, MechanismLevel> floors =
 158              new EnumMap<>(StrengthOptionKey.class);
 159          floors.put(StrengthOptionKey.PROTECT_CORE_AUTHORITY,
 160              MechanismLevel.PROTECT_RONOVA);
 161          floors.put(StrengthOptionKey.PROTECT_RESTART_RECOVERY,
 162              MechanismLevel.PROTECT_RONOVA);
 163          floors.put(StrengthOptionKey.PROTECT_ROLLBACK,
 164              MechanismLevel.PROTECT_RONOVA);
 165          floors.put(StrengthOptionKey.PROTECT_LOOP_GUARD,
 166              MechanismLevel.PROTECT_RONOVA);
 167          return Collections.unmodifiableMap(floors);
 168      }
 169  
 170      private static EnumMap<StrengthOptionKey, MechanismLevel> filled(
 171          MechanismLevel level
 172      ) {
 173          MechanismLevel runnable = MechanismLevel.runnable(level);
 174          EnumMap<StrengthOptionKey, MechanismLevel> values =
 175              new EnumMap<>(StrengthOptionKey.class);
 176          for (StrengthOptionKey option : StrengthOptionKey.values()) {
```

<a id="mechanism"></a>
### mechanism

`src/main/java/dev/ronova/coremod/policy/MechanismLevel.java:1–37`；文件 SHA-256 `0038bf4e8b622881d1d2d0588112b727e623eb21c47b258153cd311ade2ff633`。

```java
   1  package dev.ronova.coremod.policy;
   2  
   3  /** Ordered enforcement levels for mechanism interception options. */
   4  public enum MechanismLevel {
   5      FOLLOW_PROFILE,
   6      OFF,
   7      MONITOR,
   8      PROTECT_RONOVA,
   9      RELATED_PATHS,
  10      STRICT_ALL;
  11  
  12      public boolean isStricterThan(MechanismLevel other) {
  13          return other != null && rank() > other.rank();
  14      }
  15  
  16      /** FOLLOW_PROFILE is configuration syntax, not a runnable strength. */
  17      public int rank() {
  18          return switch (this) {
  19              case FOLLOW_PROFILE -> -1;
  20              case OFF -> 0;
  21              case MONITOR -> 1;
  22              case PROTECT_RONOVA -> 2;
  23              case RELATED_PATHS -> 3;
  24              case STRICT_ALL -> 4;
  25          };
  26      }
  27  
  28      public static MechanismLevel lowerOf(MechanismLevel requested, MechanismLevel ceiling) {
  29          MechanismLevel left = runnable(requested);
  30          MechanismLevel right = runnable(ceiling);
  31          return left.rank() <= right.rank() ? left : right;
  32      }
  33  
  34      public static MechanismLevel runnable(MechanismLevel value) {
  35          return value == null || value == FOLLOW_PROFILE ? OFF : value;
  36      }
  37  }
```

<a id="attack-level"></a>
### attack-level

`src/main/java/dev/ronova/coremod/policy/AttackActionLevel.java:1–15`；文件 SHA-256 `38bf65ff972552792a4c8520226d5c7028c21794b63870e1718ee53ac1a509ca`。

```java
   1  package dev.ronova.coremod.policy;
   2  
   3  /** Configuration syntax, independent of the advanced mechanism vocabulary. */
   4  public enum AttackActionLevel {
   5      FOLLOW_PROFILE, DISABLED, COMPATIBLE, ENHANCED, EXTREME;
   6  
   7      public MechanismLevel depth() {
   8          return switch (this) {
   9              case FOLLOW_PROFILE, DISABLED -> MechanismLevel.OFF;
  10              case COMPATIBLE -> MechanismLevel.PROTECT_RONOVA;
  11              case ENHANCED -> MechanismLevel.RELATED_PATHS;
  12              case EXTREME -> MechanismLevel.STRICT_ALL;
  13          };
  14      }
  15  }
```

<a id="snapshot"></a>
### snapshot

`src/main/java/dev/ronova/coremod/policy/StrengthPolicySnapshot.java:179–247`；文件 SHA-256 `8881c5987ebe7e2af71332ed342e5c58f8678b95fd67b51f022032eb75833faf`。

```java
 179      public AttackResolution resolveAttack(
 180          java.util.Set<AttackAction> actions,
 181          java.util.Set<StrengthOptionKey> additionalKeys
 182      ) {
 183          Objects.requireNonNull(actions, "actions");
 184          Objects.requireNonNull(additionalKeys, "additionalKeys");
 185          if (actions.isEmpty() || actions.equals(java.util.Set.of(AttackAction.STRONG_ATTACK))) {
 186              return new AttackResolution(MechanismLevel.OFF, MechanismLevel.OFF, false,
 187                  "NO_EXECUTABLE_ACTION");
 188          }
 189          java.util.EnumSet<StrengthOptionKey> required =
 190              java.util.EnumSet.noneOf(StrengthOptionKey.class);
 191          MechanismLevel requested = MechanismLevel.STRICT_ALL;
 192          for (AttackAction action : actions) {
 193              MechanismLevel depth = activeActions.resolve(activeProfile, action).depth();
 194              if (depth == MechanismLevel.OFF) {
 195                  return new AttackResolution(MechanismLevel.OFF, MechanismLevel.OFF, false,
 196                      "ACTION_DISABLED:" + action.canonical());
 197              }
 198              requested = MechanismLevel.lowerOf(requested, depth);
 199              required.addAll(action.requiredKeys());
 200          }
 201          required.addAll(additionalKeys);
 202          MechanismLevel effective = requested;
 203          for (StrengthOptionKey key : required) {
 204              StrengthResolutionReceipt receipt = resolve(key);
 205              if (receipt.activationState() != ActivationState.ACTIVE) {
 206                  return new AttackResolution(requested, MechanismLevel.OFF, false,
 207                      receipt.activationState() + ":" + key.canonical());
 208              }
 209              effective = MechanismLevel.lowerOf(effective, receipt.effectiveLevel());
 210          }
 211          boolean permitted = effective.rank() >= MechanismLevel.PROTECT_RONOVA.rank();
 212          return new AttackResolution(requested, effective, permitted,
 213              permitted ? effective == requested ? "PERMITTED" : "LIMITED_BY_DEPENDENCY"
 214                  : "DEPENDENCY_MONITOR_ONLY");
 215      }
 216  
 217      public StrengthResolutionReceipt resolve(StrengthOptionKey option) {
 218          Objects.requireNonNull(option, "option");
 219          MechanismLevel configured = configuredLevels.get(option);
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
 244              reason(
 245                  activeRequested, effective, supportedCeiling, evidenceCeiling,
 246                  evidence, activation, activeResolution.minimumFloorApplied()
 247              )
```

<a id="projection"></a>
### projection

`src/main/java/dev/ronova/coremod/policy/RuntimeCapabilityProjection.java:1–26`；文件 SHA-256 `c0c2920dddc4847947baa960e1805098531fc69f1eaa513636c61bf637b0d04b`。

```java
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

<a id="terminal-policy"></a>
### terminal-policy

`src/main/java/dev/ronova/coremod/policy/TerminalActionPolicy.java:1–50`；文件 SHA-256 `2300395e2c35b9a68d25a715981aacd226a07e614b216c5214506a3886ce884b`。

```java
   1  package dev.ronova.coremod.policy;
   2  
   3  import java.util.*;
   4  
   5  /** Fixed permissions for the currently implemented entity-local terminal executors. */
   6  public final class TerminalActionPolicy {
   7      public enum Step {
   8          LIFE(0x8063,Set.of(AttackAction.REWRITE_LIFE)),
   9          REMOVAL(0x3,Set.of(AttackAction.DIRECT_CLEAR)),
  10          REMOVED_RECORDS(0x1f89,Set.of(AttackAction.CLEAR_ENTITY_RECORDS)),
  11          BYPASS_RECORDS(0x1f89,Set.of(AttackAction.CLEAR_ENTITY_RECORDS,AttackAction.DIRECT_CLEAR));
  12          private final int mask;
  13          private final Set<AttackAction> actions;
  14          Step(int mask,Set<AttackAction> actions){this.mask=mask;this.actions=Set.copyOf(actions);}
  15          public Set<AttackAction> actions(){return actions;}
  16          public int mask(){return mask;}
  17      }
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
  50  }
```

<a id="recovery-policy"></a>
### recovery-policy

`src/main/java/dev/ronova/coremod/policy/RecoveryChainPolicy.java:1–122`；文件 SHA-256 `5ee8b9291b37a1727afd794d6f44f4110d6aa39ac352681fbc091cb10cb4450c`。

```java
   1  package dev.ronova.coremod.policy;
   2  
   3  import java.util.*;
   4  
   5  /** Fixed production requirements for recovery-chain work; a caller cannot omit a dependency. */
   6  public final class RecoveryChainPolicy {
   7      private static final Set<StrengthOptionKey> NBT_GATE_KEYS = Set.of(
   8          StrengthOptionKey.RUNTIME_NATIVE, StrengthOptionKey.RUNTIME_JVMTI,
   9          StrengthOptionKey.RUNTIME_CRITICAL_CLASS_WATCH, StrengthOptionKey.PROTECT_CODE_INTEGRITY);
  10      public enum Step {
  11          DIRECT_SOURCE(AttackAction.SUPPRESS_RESPAWN, MechanismLevel.RELATED_PATHS,
  12              StrengthOptionKey.ATTACK_RECOVERY_SOURCE),
  13          RECORD_DISPOSAL(AttackAction.SUPPRESS_RESPAWN, MechanismLevel.RELATED_PATHS,
  14              StrengthOptionKey.ATTACK_RECOVERY_SOURCE, StrengthOptionKey.ATTACK_STATE_SOURCE),
  15          SAVED_RECORD_DISPOSAL(AttackAction.SUPPRESS_RESPAWN, MechanismLevel.RELATED_PATHS,
  16              StrengthOptionKey.ATTACK_RECOVERY_SOURCE, StrengthOptionKey.ATTACK_STATE_SOURCE,
  17              StrengthOptionKey.ATTACK_SAVE_RECONSTRUCTION),
  18          DIRECT_TASK(AttackAction.ATTACK_CALL_CHAIN, MechanismLevel.RELATED_PATHS),
  19          REBUILD_OUTLET(AttackAction.SUPPRESS_REBUILD, MechanismLevel.RELATED_PATHS),
  20          DERIVED_SOURCE(AttackAction.SUPPRESS_REBUILD, MechanismLevel.STRICT_ALL,
  21              StrengthOptionKey.ATTACK_LOGIC_SOURCE, StrengthOptionKey.ATTACK_STATE_SOURCE),
  22          DESCENDANT_TASK(AttackAction.ATTACK_CALL_CHAIN, MechanismLevel.STRICT_ALL),
  23          RELOAD_RECONSTRUCTION(AttackAction.SUPPRESS_REBUILD, MechanismLevel.STRICT_ALL,
  24              StrengthOptionKey.ATTACK_STATE_SOURCE, StrengthOptionKey.ATTACK_SAVE_RECONSTRUCTION);
  25  
  26          private final Set<AttackAction> actions;
  27          private final Set<StrengthOptionKey> keys;
  28          private final MechanismLevel minimum;
  29          Step(AttackAction action, MechanismLevel minimum, StrengthOptionKey... extras) {
  30              this.minimum = minimum;
  31              // Every chain action also requires the ordinary suppression switch.
  32              var requiredActions = EnumSet.of(AttackAction.SUPPRESS_RESPAWN, action);
  33              if (action == AttackAction.ATTACK_CALL_CHAIN && minimum == MechanismLevel.STRICT_ALL)
  34                  requiredActions.add(AttackAction.SUPPRESS_REBUILD);
  35              actions = Set.copyOf(requiredActions);
  36              var requiredKeys = EnumSet.noneOf(StrengthOptionKey.class);
  37              actions.forEach(value -> requiredKeys.addAll(value.requiredKeys()));
  38              requiredKeys.addAll(Arrays.asList(extras)); keys = Set.copyOf(requiredKeys);
  39          }
  40          public Set<AttackAction> actions() { return actions; }
  41          public Set<StrengthOptionKey> keys() { return keys; }
  42          public MechanismLevel minimum() { return minimum; }
  43      }
  44      public record Decision(Step step, MechanismLevel requested, MechanismLevel effective,
  45                             boolean observationRequested, boolean executionPermitted, String reason) {}
  46      private RecoveryChainPolicy() {}
  47  
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

<a id="generic-call-1"></a>
### generic-call-1

`src/main/java/dev/ronova/coremod/engine/TerminalPlanExecutor.java:75–83`；文件 SHA-256 `6d06243b598f19a5c3f6fbb4849f26ab2b1c147360ec270fddf6cf644565dd8d`。

```java
  75                          }
  76                          case SOURCE_QUALIFICATION -> {
  77                              var policy = dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(runtime.strengthPolicy(),
  78                                  dev.ronova.coremod.policy.RecoveryChainPolicy.Step.DIRECT_SOURCE);
  79                              if (!policy.observationRequested()) return new OperationReport.StepReceipt(step,
  80                                  OperationReport.Status.CONVERGED, "source-discovery-outside-active-depth;base-admission-retained");
  81                              runtime.recoverySources().watch(level, value);
  82                              runtime.recoverySources().pulse(runtime.budget(BudgetCategory.OBSERVATION));
  83                              return new OperationReport.StepReceipt(step, OperationReport.Status.DISPATCHED,
```

<a id="generic-call-2"></a>
### generic-call-2

`src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java:577–585`；文件 SHA-256 `1f357b8dc9c884b11baf42cd4941fc861a071ce77ce65f2be461e86cbaaeccd0`。

```java
 577          if (closed || !level.getServer().isSameThread() || !current(plan.qualification())) return true;
 578          if (!worldAdmission.allows(level)) { gap(plan.qualification().targetEpoch()); return false; }
 579          if (!dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),
 580              dev.ronova.coremod.policy.RecoveryChainPolicy.Step.DIRECT_SOURCE).observationRequested()) return true;
 581          TargetEpoch key = plan.qualification().targetEpoch();
 582          Watch existing = watches.get(key);
 583          if (existing != null) {
 584              if (current(existing.plan.qualification())) return true;
 585              watches.remove(key); rotation.remove(key); existing.close();
```

<a id="generic-call-3"></a>
### generic-call-3

`src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java:610–618`；文件 SHA-256 `1f357b8dc9c884b11baf42cd4941fc861a071ce77ce65f2be461e86cbaaeccd0`。

```java
 610          boolean bound=binding!=null&&current(binding.terminal.qualification())
 611              &&(origin.copyDepth()>0||!binding.detached&&binding.stillBound());
 612          boolean derived = dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),
 613              dev.ronova.coremod.policy.RecoveryChainPolicy.Step.DERIVED_SOURCE).observationRequested();
 614          boolean copyVerified=bound&&RecoveryBoundaryProof.copy()&&runtime.enforcement().observesRegisteredSemantics(0x6000);
 615          if(copyVerified&&origin.copyDepth()==0)trackSeed(binding.key,(CompoundTag)source,true);
 616          else if(copyVerified)publishCopyOrigin(source);
 617          copies.copied(source, result, derived ? Integer.MAX_VALUE : 1,copyVerified);
 618          publishCopyOrigin(result);
```

<a id="generic-call-4"></a>
### generic-call-4

`src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java:651–659`；文件 SHA-256 `1f357b8dc9c884b11baf42cd4941fc861a071ce77ce65f2be461e86cbaaeccd0`。

```java
 651              &&origin.evidenceVerified()&&ticket.depth()==origin.copyDepth()&&(origin.copyDepth()>0||!source.detached&&source.stillBound())
 652              &&runtime.enforcement().observesRegisteredSemantics(0x6000);
 653          boolean derived=dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),
 654              dev.ronova.coremod.policy.RecoveryChainPolicy.Step.DERIVED_SOURCE).observationRequested();
 655          copies.copied(copy.source(),copy.result(),derived?Integer.MAX_VALUE:1,verified);publishCopyOrigin(copy.result());
 656          var output=copies.resolve(copy.result());
 657          if(verified&&output.state()==CopyProvenanceIndex.State.RESOLVED&&output.evidenceVerified()&&ticket.source().equals(output.sourceKey()))
 658              taskObserver.graph().sourceUsed(copy.task(),new TaskExecutionGraph.SourceUse(ticket.qualification(),ticket.source().locator(),ticket.source().observedVersion(),output.copyDepth()));
 659          else if(source!=null)gap(source.key.targetEpoch());
```

<a id="generic-call-5"></a>
### generic-call-5

`src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java:703–711`；文件 SHA-256 `1f357b8dc9c884b11baf42cd4941fc861a071ce77ce65f2be461e86cbaaeccd0`。

```java
 703      private void discoverStaticSeed(CompoundTag seed, MinecraftServer server, List<Class<?>> callers) {
 704          if (callers.isEmpty() || runtime.durableStore() == null
 705              || !dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),
 706                  dev.ronova.coremod.policy.RecoveryChainPolicy.Step.DIRECT_SOURCE).observationRequested()) return;
 707          try (var observation = runtime.enforcement().beginObservation(0x6000)) {
 708              if (observation == null || !observation.stillCurrent() || !seed.hasUUID("UUID")) return;
 709              UUID subject = seed.getUUID("UUID");
 710              var stamp = runtime.durableStore().authorityStamp(subject);
 711              var q = stamp == null ? null : stamp.qualification();
```

<a id="generic-call-6"></a>
### generic-call-6

`src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java:885–893`；文件 SHA-256 `1f357b8dc9c884b11baf42cd4941fc861a071ce77ce65f2be461e86cbaaeccd0`。

```java
 885      public synchronized void resume(MinecraftServer server, WorkBudget budget) {
 886          if (closed || !server.isSameThread() || runtime.durableStore() == null
 887              || !dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),
 888                  dev.ronova.coremod.policy.RecoveryChainPolicy.Step.DIRECT_SOURCE).observationRequested()) return;
 889          var subjects = runtime.projection().snapshot().decisions();
 890          if (subjects.isEmpty()) { coldCursor = 0; return; }
 891          int processed = 0;
 892          try {
 893              for (int i = 0; i < Math.min(2, subjects.size()) && budget.tryConsumeCount(1); i++) {
```

<a id="generic-call-7"></a>
### generic-call-7

`src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java:915–923`；文件 SHA-256 `1f357b8dc9c884b11baf42cd4941fc861a071ce77ce65f2be461e86cbaaeccd0`。

```java
 915          collectRecordTasks();
 916          if ((budget.tickId() & 1) == 0) flushMutationReceipts(budget);
 917          boolean observe = dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),
 918              dev.ronova.coremod.policy.RecoveryChainPolicy.Step.DIRECT_SOURCE).observationRequested();
 919          // Static copies may stop completely after a qualification is retired. Do not
 920          // rely on another copy callback to release their bounded discovery slots.
 921          for (var iterator = staticScans.iterator(); iterator.hasNext();) {
 922              var scan = iterator.next();
 923              if (!observe || scan.caller.get() == null || retired(scan.qualification)) {
```

<a id="generic-call-8"></a>
### generic-call-8

`src/main/java/dev/ronova/coremod/observation/RecoverySourceTracker.java:1433–1441`；文件 SHA-256 `1f357b8dc9c884b11baf42cd4941fc861a071ce77ce65f2be461e86cbaaeccd0`。

```java
1433          var store=runtime.durableStore();
1434          if(closed||server==null||!server.isSameThread()||store==null||store.readOnly()||store.persistencePending()
1435              ||!dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(policy(),
1436                  dev.ronova.coremod.policy.RecoveryChainPolicy.Step.DIRECT_SOURCE).observationRequested())return;
1437          int[] consumed={0};
1438          java.util.function.BooleanSupplier allowance=()->{if(!budget.tryConsumeCount(1))return false;consumed[0]++;return true;};
1439          try {
1440              for(int i=0;i<2&&allowance.getAsBoolean();i++) {
1441                  if(historyCursor>=store.effectRecordCount())historyCursor=0;
```
