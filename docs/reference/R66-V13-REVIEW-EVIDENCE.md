# r66 · V1.3 方向复核依据

**范围：** 本轮只读核对收到的 r66 源码快照与 V1.3。源码哈希/摘录和词法计数不证明完整调用图、运行可达或能力效果；用户头文件路径、台账 mtime 未在本环境复跑。

[当前融合规划](../plans/R66-CAPABILITY-WIRING-PLAN.md#v13-receipt) · [逐条回执](../plans/RESTRUCTURE-REVIEW.md#review-fix6-v13) · [机器清单](../plans/restructure/enhancement-addendum-merge.json)

## 本轮新增静态发现

- ⑤不是从零实现失效检查：CompletionTracker 已含 dependencyRevision、scope、authorityCurrent、采样间隔及历史/当前分离；新增工作是枚举依赖、精确失效原因与使用边界的一致快照。
- ⑥不是从零建立归属：TerminalOperationPlan 已有 ownerIdentity、commandId、planRevision、effectKey；PlanExecutionLease 检查线程和 plan；OperationPlanRunner 有重入、claim 与最后写入权限检查。它们不自动证明任意跨表面结算唯一，新增的是统一覆盖与诊断。
- ④有四处 Java 词法命中，实际分布为键定义、两个 AttackAction 依赖、预设矩阵；不是“源码+书”的同一计数。没有从词法检索推出不存在任何间接执行。
- ②弱引用除了发现缺席，还用于获得仍存活对象；r66 的持久标记另与 projection、JudgmentMark 和 subject 相连，不能说持久权威完全靠弱引用。
- ⑦ budget 包没有 CrossSurface/RuntimeSurface 命中，但 TerminalOperationPlan 已有表面集合和 reservedHeapBytes；缺的是所提多表面成本归集/计划分配，不是所有计划和资源载体都不存在。

## 词法检查

| 范围 | 正则 | 命中行数 | 含义边界 |
|---|---|---:|---|
| `native` | `can_tag_objects&#124;OBJECT_FREE&#124;SetTag\(&#124;GetTag\(` | 0 | 仅在记录的文件集合中搜索，不证明语义不存在 |
| `native` | `COMPILED_METHOD_LOAD&#124;CompiledMethodLoad` | 0 | 仅在记录的文件集合中搜索，不证明语义不存在 |
| `native` | `DYNAMIC_CODE_GENERATED&#124;DynamicCodeGenerated` | 0 | 仅在记录的文件集合中搜索，不证明语义不存在 |
| `budget` | `CrossSurface&#124;crossSurface` | 0 | 仅在记录的文件集合中搜索，不证明语义不存在 |
| `budget` | `RuntimeSurface` | 0 | 仅在记录的文件集合中搜索，不证明语义不存在 |
| `java` | `target_class_lineage&#124;TARGET_CLASS_LINEAGE` | 4 | 仅在记录的文件集合中搜索，不证明语义不存在 |

## 外部规范核对（与原稿分开）

**[JDK 17 ObjectFree / Object Tags](https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html#ObjectFree)**：ObjectFree 针对被打 tag 且被 GC 释放的对象；回调无 jobject，不准调用 JNI，仅可用特许的 JVMTI 函数。tag 局限于 JVMTI environment。

**[JDK 17 WeakReference](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ref/WeakReference.html)**：弱可达时清除弱引用；可选 ReferenceQueue 通知。因此原生回收通知不等同弱引用清除，也不证明只有轮询一种 Java 路线。

**[JDK 17 CompiledMethodLoad / CompiledMethodUnload / GenerateEvents](https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html#CompiledMethodLoad)**：同一方法可能有多个编译版本，另有卸载事件；晚订阅可补发当前信息，但未收到事件不能反证未编译。

**[JVMS 17 §4.7.12](https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.7.12)**：LineNumberTable 是可选调试属性；规范本身不提供改写分类器的判别力。

**[OpenJDK 17u 固定标签规范源](https://raw.githubusercontent.com/openjdk/jdk17u/jdk-17.0.16%2B8/src/hotspot/share/prims/jvmti.xml)**：作为事件/标签规范的固定版本参考，不作为用户本机 capability 获授或成本实测。

核对日期为 2026-09-14；未运行 JVMTI。成本、线程/阶段实测和能力获授继续待证。

## 源码摘录

<a id="budget-categories"></a>
### budget-categories

`src/main/java/dev/ronova/coremod/budget/BudgetCategory.java`，L1–L31；文件 SHA-256：`173315408b7501036d7a5caddc009eba26f80d6ad22dc572b6407fbc61adfbbf`。

```text
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
```

<a id="budget-governor"></a>
### budget-governor

`src/main/java/dev/ronova/coremod/budget/FrameBudgetGovernor.java`，L10–L58；文件 SHA-256：`b7f957672367a6fb45f5d7378e676d85642fb1e92ced1de25cb65a50fc9b6325`。

```text
  10  public final class FrameBudgetGovernor {
  11      public static final long TICK_BUDGET_US = 2_000L;
  12  
  13      private final BudgetSplitFlags.Selection selection;
  14      private final LongSupplier clock;
  15      private final EnumMap<BudgetCategory, Long> budgetUs;
  16      private final EnumMap<BudgetCategory, Integer> countLimits;
  17      private final AtomicLongArray overBudgetCounts =
  18          new AtomicLongArray(BudgetCategory.values().length);
  19      private final EnumMap<BudgetTelemetrySnapshot.QueryKind, QueryCounters> queryCounters =
  20          new EnumMap<>(BudgetTelemetrySnapshot.QueryKind.class);
  21      private final Object lock = new Object();
  22      private EnumMap<BudgetCategory, WorkBudget> budgets;
  23      private long currentTickId;
  24      private long snapshotEpoch;
  25      private boolean started;
  26  
  27      public FrameBudgetGovernor() {
  28          this(BudgetSplitFlags.resolve());
  29      }
  30  
  31      public FrameBudgetGovernor(boolean enabled) {
  32          this(new BudgetSplitFlags.Selection(
  33              enabled,
  34              enabled ? BudgetSplitFlags.Source.DEFAULT : BudgetSplitFlags.Source.JVM_PROPERTY,
  35              null
  36          ));
  37      }
  38  
  39      public FrameBudgetGovernor(BudgetSplitFlags.Selection selection) {
  40          this(selection, System::nanoTime, null, null);
  41      }
  42  
  43      FrameBudgetGovernor(
  44          BudgetSplitFlags.Selection selection,
  45          LongSupplier clock,
  46          Map<BudgetCategory, Long> budgetUs,
  47          Map<BudgetCategory, Integer> countLimits
  48      ) {
  49          this.selection = Objects.requireNonNull(selection, "selection");
  50          this.clock = Objects.requireNonNull(clock, "clock");
  51          this.budgetUs = copyBudgetUs(budgetUs);
  52          this.countLimits = copyCountLimits(countLimits);
  53          for (BudgetTelemetrySnapshot.QueryKind kind
  54              : BudgetTelemetrySnapshot.QueryKind.values()) {
  55              queryCounters.put(kind, new QueryCounters());
  56          }
  57      }
  58  
```

<a id="watch-bind"></a>
### watch-bind

`src/main/java/dev/ronova/coremod/terminal/DurableTerminalWatchSet.java`，L76–L121；文件 SHA-256：`a7d3a6a4b37b4e60e9eb336f71576411185f6ed75320e53db5b3740ae7f02a89`。

```text
  76      @Override
  77      public synchronized void track(JudgmentMark mark, Entity incarnation) {
  78          if (mark == null || mark.uuid() == null) {
  79              return;
  80          }
  81          Entry entry = entries.get(mark.uuid());
  82          if (entry == null) {
  83              entry = Entry.durable(mark);
  84              entries.put(mark.uuid(), entry);
  85          } else {
  86              entry.mark = mark;
  87              entry.subject = subject(mark);
  88              entry.durableBacked = true;
  89              entry.tombstoneId = mark.uuid();
  90          }
  91          if (belongsToServer(incarnation)) {
  92              entry.reference = new WeakReference<>(incarnation);
  93              entry.subject = MinecraftSubjectAdapter.from(incarnation);
  94              entry.absentCycles = 0;
  95              observeSchedule(entry, server.getTickCount() + 1L);
  96          } else if (entry.scheduled == null) {
  97              schedule(entry, Phase.COLD, server.getTickCount());
  98          }
  99      }
 100  
 101      @Override
 102      public synchronized void observeIncarnation(Entity incarnation) {
 103          if (!belongsToServer(incarnation) || authorityView.isProtected(incarnation)) {
 104              return;
 105          }
 106          SubjectIdentity subject = MinecraftSubjectAdapter.from(incarnation);
 107          if (!projection.get().matches(subject)) {
 108              return;
 109          }
 110          UUID uuid = incarnation.getUUID();
 111          Entry entry = entries.get(uuid);
 112          if (entry == null) {
 113              JudgmentMark mark = JudgmentMark.from(incarnation, JudgmentScope.IDENTITY, 0.0D);
 114              entry = Entry.transientIncarnation(mark, subject, incarnation);
 115              entries.put(uuid, entry);
 116          } else {
 117              entry.reference = new WeakReference<>(incarnation);
 118              entry.subject = subject;
 119          }
 120          entry.absentCycles = 0;
 121          observeSchedule(entry, server.getTickCount() + 1L);
```

<a id="watch-maintain"></a>
### watch-maintain

`src/main/java/dev/ronova/coremod/terminal/DurableTerminalWatchSet.java`，L251–L310；文件 SHA-256：`a7d3a6a4b37b4e60e9eb336f71576411185f6ed75320e53db5b3740ae7f02a89`。

```text
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
```

<a id="watch-entry"></a>
### watch-entry

`src/main/java/dev/ronova/coremod/terminal/DurableTerminalWatchSet.java`，L429–L461；文件 SHA-256：`a7d3a6a4b37b4e60e9eb336f71576411185f6ed75320e53db5b3740ae7f02a89`。

```text
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
```

<a id="completion"></a>
### completion

`src/main/java/dev/ronova/coremod/runtime/CompletionTracker.java`，L7–L87；文件 SHA-256：`26278f689bd25df741ffd8c38454d44c3873076d844d1ff2b7ddcf8b0e606695`。

```text
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
```

<a id="execution-lease"></a>
### execution-lease

`src/main/java/dev/ronova/coremod/runtime/PlanExecutionLease.java`，L7–L33；文件 SHA-256：`fefa75f75fe7c00486b6a42c039967cb7097e8e595f403fc8fd0619adcc6bdaf`。

```text
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
```

<a id="operation-runner"></a>
### operation-runner

`src/main/java/dev/ronova/coremod/runtime/OperationPlanRunner.java`，L8–L81；文件 SHA-256：`92cb9976a2734db2221b0df44dbe18244417dff1c6c141301032013fe29d661f`。

```text
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
```

<a id="operation-plan"></a>
### operation-plan

`src/main/java/dev/ronova/coremod/root/api/TerminalOperationPlan.java`，L7–L41；文件 SHA-256：`f0b33e28581da997912d44ea1001443ed6d9ce656dab2adb0f72aebb065636d9`。

```text
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

<a id="lineage-actions"></a>
### lineage-actions

`src/main/java/dev/ronova/coremod/policy/AttackAction.java`，L1–L51；文件 SHA-256：`21c6de9a6810fe7fdd080dcfebd4d8d4eab4e85853ba99d2813e99af416f5923`。

```text
   1  package dev.ronova.coremod.policy;
   2  
   3  import java.util.Set;
   4  
   5  /** Ordinary attack controls; these do not add advanced StrengthOptionKeys. */
   6  public enum AttackAction {
   7      STRONG_ATTACK("strongAttack", "强力攻击"),
   8      REWRITE_LIFE("rewriteLife", "改写生命", StrengthOptionKey.ATTACK_STATE_SOURCE),
   9      TERMINALIZE("terminalize", "强制终结", StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER,
  10          StrengthOptionKey.ATTACK_REGISTRATION_SOURCE),
  11      DIRECT_CLEAR("directClear", "直接清除", StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER),
  12      CLEAR_ENTITY_RECORDS("clearEntityRecords", "实体记录清除",
  13          StrengthOptionKey.ATTACK_STATE_SOURCE, StrengthOptionKey.ATTACK_REGISTRATION_SOURCE),
  14      SUPPRESS_RESPAWN("suppressRespawn", "重生压制", StrengthOptionKey.ATTACK_REGISTRATION_SOURCE),
  15      SUPPRESS_REBUILD("suppressRebuild", "重建压制", StrengthOptionKey.ATTACK_RECOVERY_SOURCE,
  16          StrengthOptionKey.ATTACK_TARGET_CLASS_LINEAGE),
  17      SUPPRESS_ADMISSION("suppressAdmission", "生成压制", StrengthOptionKey.ATTACK_REGISTRATION_SOURCE,
  18          StrengthOptionKey.ATTACK_NETWORK_RECONSTRUCTION, StrengthOptionKey.ATTACK_SAVE_RECONSTRUCTION),
  19      ATTACK_RENDER("attackRender", "渲染攻击", StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER),
  20      CLEAR_TRACKING("clearTracking", "追踪清除", StrengthOptionKey.ATTACK_NETWORK_RECONSTRUCTION,
  21          StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER),
  22      CLEAR_HUD("clearHud", "血条/HUD 清除", StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER),
  23      STRIP_CODE_DEFENSE("stripCodeDefense", "代码防御剥离",
  24          StrengthOptionKey.ATTACK_RUNTIME_PATCH_DISABLEMENT, StrengthOptionKey.ATTACK_DYNAMIC_CODE,
  25          StrengthOptionKey.ATTACK_TARGET_CLASS_LINEAGE),
  26      ATTACK_CALL_CHAIN("attackCallChain", "调用链攻击", StrengthOptionKey.ATTACK_LOGIC_SOURCE,
  27          StrengthOptionKey.ATTACK_RECOVERY_SOURCE, StrengthOptionKey.ATTACK_TASK_CHAIN,
  28          StrengthOptionKey.ATTACK_CALLBACK_CHAIN, StrengthOptionKey.ATTACK_DISPATCH),
  29      ALL_RETURN("allReturn", "全返回", StrengthOptionKey.ATTACK_DISPATCH);
  30  
  31      private final String canonical;
  32      private final String displayName;
  33      private final Set<StrengthOptionKey> requiredKeys;
  34  
  35      AttackAction(String canonical, String displayName, StrengthOptionKey... requiredKeys) {
  36          this.canonical = canonical;
  37          this.displayName = displayName;
  38          this.requiredKeys = Set.of(requiredKeys);
  39      }
  40  
  41      public String canonical() { return canonical; }
  42      public String displayName() { return displayName; }
  43      public Set<StrengthOptionKey> requiredKeys() { return requiredKeys; }
  44  
  45      public static AttackAction fromCanonical(String value) {
  46          for (AttackAction action : values()) {
  47              if (action.canonical.equals(value)) return action;
  48          }
  49          throw new IllegalArgumentException("unknown attack action: " + value);
  50      }
  51  }
```

<a id="policy-keys"></a>
### policy-keys

`src/main/java/dev/ronova/coremod/policy/StrengthOptionKey.java`，L4–L76；文件 SHA-256：`7395854c31105d62aa5762aa4cbbc1850e2e1860705f5d510634b6000e71d3ed`。

```text
   4  import java.util.Map;
   5  
   6  /** Stable 7.16 policy vocabulary. A key existing does not claim its executor is active. */
   7  public enum StrengthOptionKey {
   8      ATTACK_NON_ENTITY("attack.non_entity", PlaneId.ATTACK, false),
   9      ATTACK_LOGIC_SOURCE("attack.logic_source", PlaneId.ATTACK, false),
  10      ATTACK_RECOVERY_SOURCE("attack.recovery_source", PlaneId.ATTACK, false),
  11      ATTACK_STATE_SOURCE("attack.state_source", PlaneId.ATTACK, false),
  12      ATTACK_REGISTRATION_SOURCE("attack.registration_source", PlaneId.ATTACK, false),
  13      ATTACK_TASK_CHAIN("attack.task_chain", PlaneId.ATTACK, false),
  14      ATTACK_CALLBACK_CHAIN("attack.callback_chain", PlaneId.ATTACK, false),
  15      ATTACK_NETWORK_RECONSTRUCTION("attack.network_reconstruction", PlaneId.ATTACK, false),
  16      ATTACK_SAVE_RECONSTRUCTION("attack.save_reconstruction", PlaneId.ATTACK, false),
  17      ATTACK_DYNAMIC_CODE("attack.dynamic_code", PlaneId.ATTACK, true),
  18      ATTACK_DISPATCH("attack.dispatch", PlaneId.ATTACK, true),
  19      ATTACK_RUNTIME_PATCH_DISABLEMENT("attack.runtime_patch_disablement", PlaneId.ATTACK, true),
  20      ATTACK_TARGET_CLASS_LINEAGE("attack.target_class_lineage", PlaneId.ATTACK, true),
  21      ATTACK_FINAL_RESULT_TAKEOVER("attack.final_result_takeover", PlaneId.ATTACK, false),
  22      ATTACK_SOURCE_TRACE("attack.source_trace", PlaneId.ATTACK, true),
  23  
  24      INTERCEPT_MIXIN("intercept.mixin", PlaneId.INTERCEPTION, true),
  25      INTERCEPT_ASM("intercept.asm", PlaneId.INTERCEPTION, true),
  26      INTERCEPT_INSTRUMENTATION("intercept.instrumentation", PlaneId.INTERCEPTION, true),
  27      INTERCEPT_JVMTI("intercept.jvmti", PlaneId.INTERCEPTION, true),
  28      INTERCEPT_UNSAFE_REFLECTION("intercept.unsafe_reflection", PlaneId.INTERCEPTION, true),
  29      INTERCEPT_CLASSLOADER("intercept.classloader", PlaneId.INTERCEPTION, true),
  30      INTERCEPT_JNI_NATIVE("intercept.jni_native", PlaneId.INTERCEPTION, true),
  31      INTERCEPT_MODLAUNCHER("intercept.modlauncher", PlaneId.INTERCEPTION, true),
  32      INTERCEPT_THREAD_TASK("intercept.thread_task", PlaneId.INTERCEPTION, false),
  33      INTERCEPT_ALL_RETURN("intercept.all_return", PlaneId.INTERCEPTION, true),
  34  
  35      PROTECT_LIFE("protect.life", PlaneId.PROTECTION, false),
  36      PROTECT_DEATH("protect.death", PlaneId.PROTECTION, false),
  37      PROTECT_REMOVAL("protect.removal", PlaneId.PROTECTION, false),
  38      PROTECT_ENTITY_RECORD("protect.entity_record", PlaneId.PROTECTION, false),
  39      PROTECT_IDENTITY_RESPAWN("protect.identity_respawn", PlaneId.PROTECTION, false),
  40      PROTECT_DISPLAY("protect.display", PlaneId.PROTECTION, false),
  41      PROTECT_ANTI_INTERCEPTION("protect.anti_interception", PlaneId.PROTECTION, true),
  42      PROTECT_ANTI_ALL_RETURN("protect.anti_all_return", PlaneId.PROTECTION, true),
  43      PROTECT_INPUT_COMMUNICATION("protect.input_communication", PlaneId.PROTECTION, false),
  44      PROTECT_CALL("protect.call", PlaneId.PROTECTION, true),
  45      PROTECT_CODE_INTEGRITY("protect.code_integrity", PlaneId.PROTECTION, true),
  46      PROTECT_CORE_AUTHORITY("protect.core_authority", PlaneId.PROTECTION, true),
  47      PROTECT_AUTO_REPAIR("protect.auto_repair", PlaneId.PROTECTION, true),
  48      PROTECT_STATE_RECOVERY("protect.state_recovery", PlaneId.PROTECTION, false),
  49      PROTECT_RESTART_RECOVERY("protect.restart_recovery", PlaneId.PROTECTION, true),
  50      PROTECT_ROLLBACK("protect.rollback", PlaneId.PROTECTION, true),
  51      PROTECT_LOOP_GUARD("protect.loop_guard", PlaneId.PROTECTION, false),
  52  
  53      RUNTIME_JAVA_AGENT("runtime.java_agent", PlaneId.RUNTIME, true),
  54      RUNTIME_INSTRUMENTATION("runtime.instrumentation", PlaneId.RUNTIME, true),
  55      RUNTIME_JVMTI("runtime.jvmti", PlaneId.RUNTIME, true),
  56      RUNTIME_NATIVE("runtime.native", PlaneId.RUNTIME, true),
  57      RUNTIME_NATIVE_LOADER("runtime.native_loader", PlaneId.RUNTIME, true),
  58      RUNTIME_RENDER_GUARD("runtime.render_guard", PlaneId.RUNTIME, true),
  59      RUNTIME_CRITICAL_CLASS_WATCH("runtime.critical_class_watch", PlaneId.RUNTIME, true),
  60      RUNTIME_DYNAMIC_CODE_WATCH("runtime.dynamic_code_watch", PlaneId.RUNTIME, true),
  61      RUNTIME_NATIVE_MODULE_WATCH("runtime.native_module_watch", PlaneId.RUNTIME, true),
  62      RUNTIME_THREAD_CONTINUITY("runtime.thread_continuity", PlaneId.RUNTIME, false),
  63      RUNTIME_BACKGROUND_INTEGRITY("runtime.background_integrity", PlaneId.RUNTIME, true),
  64      RUNTIME_ANOMALOUS_CALL_LOG("runtime.anomalous_call_log", PlaneId.RUNTIME, false);
  65  
  66      private static final Map<String, StrengthOptionKey> BY_CANONICAL = index();
  67      private final String canonical;
  68      private final PlaneId plane;
  69      private final boolean restartRequired;
  70  
  71      StrengthOptionKey(String canonical, PlaneId plane, boolean restartRequired) {
  72          this.canonical = canonical;
  73          this.plane = plane;
  74          this.restartRequired = restartRequired;
  75      }
  76  
```

<a id="projection"></a>
### projection

`src/main/java/dev/ronova/coremod/policy/RuntimeCapabilityProjection.java`，L1–L26；文件 SHA-256：`c0c2920dddc4847947baa960e1805098531fc69f1eaa513636c61bf637b0d04b`。

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

<a id="surfaces"></a>
### surfaces

`src/main/java/dev/ronova/coremod/surface/RuntimeSurface.java`，L1–L3；文件 SHA-256：`62970ea66f4fad8df9ec6bc88f4378620593be6a796b93df4c11da3063222d92`。

```text
   1  package dev.ronova.coremod.surface;
   2  
   3  public enum RuntimeSurface { DATA, NETWORK, PRESENTATION, INPUT, EVENT, TEMPORAL, REGISTRY, RESOURCE }
```

<a id="native-callbacks"></a>
### native-callbacks

`native/src/ronova_native.cpp`，L815–L845；文件 SHA-256：`9914ab5bb358f641f3a3fca9cf176fc8d10eb6b0129557b504aa4cc2c9652baa`。

```text
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
```

