> **来源记录 · 写于 fix7 之前。** 本文针对 **fix6** 回执所引源码依据，不是对 fix7 的回应；fix7 的回执、融合章节与加固处理**不改变**本文任何一条。三条结论（定值载体 / 无到期字段 / 无单归属断言）不在 fix7 讨论范围内。方向为 Ronova 侧 → 外部研究方（**发出**）。

# R66 · V1.3 核验补遗（对 fix6 回执的独立源码复核）

**方向：** Ronova 侧 → 外部研究方（**发出**）
**性质：** 对 **fix6 回执所引源码依据**的独立只读复核。**不是新的加强提案**，不新增方向、编号或批次。
**依据包：** `ronova-docs-0.2.0-alpha.7.17-r66-ms-restructured-fix6.zip`（SHA-256 `46DB9A93…`）
**核验方式：** 只读源码读取 + 逐文件行计数 + 逐字面检索。**未编译、未启动 JVM/游戏、未改源码、未改 `validation/`、未回填任何 PASS。**

---

## 0. 这份补遗想解决什么

fix6 的五条源码依据**全部成立**。但其中三条，依据成立与结论可用之间还有一段距离：

| 条 | 依据成立？ | 结论是否需要再收一格 |
|---|---|---|
| `WorkBudget`/`FrameBudgetGovernor`/`HostResourceGovernor` 已存在 | ✅ 成立 | 否 |
| `TerminalOperationPlan` 已有四个成员 | ✅ 成立 | **是** —— 四者是**固定常量**，不是逐计划可变值 |
| `CompletionTracker` 已有依赖版本与失效 | ✅ 成立 | **是** —— 是**采样间隔陈旧界**，没有到期字段 |
| owner/lease/claim 守卫已存在 | ✅ 成立 | **是** —— 但**没有**"恰好一个结算归属者"的检查 |
| `DurableTerminalWatchSet` 不只靠弱引用 | ✅ 成立 | 否 —— 反向**推翻**了原稿 ② 的前提 |

下面逐条给证据。**所有锚点都是本轮实际读取过的，可以直接反驳。**

---

## 1. 逐条核验

### 1.1 现有预算机制 — 确认，且已在生产使用

| 类 | 路径 | 行数 | 关键行 |
|---|---|---|---|
| `WorkBudget` | `coremod/budget/` | 291 | `tryConsumeCount`:59 · `remainingCount`:78 · `hasTimeRemaining`:88 · `deferCount`:96 · `refundCount`:107 · `finish`:120/124 · `recordMeasurement`:162 · `receipt`:191 · 构造器包私有`:32` |
| `FrameBudgetGovernor` | `coremod/budget/` | 274 | `TICK_BUDGET_US=2_000L`:11 · `beginTick`:67 · `endTick`:82 · `budget(category)`:91 · `recordSynchronousQuery`:115 · `recordMeasurement`:133 · `snapshot`:151 |
| `HostResourceGovernor` | `coremod/budget/` | 124 | `record Budget(soft,hard,recoveryReserve)`:10 · `declareAccounting`:60 · `reserve(map,Priority)`:72 · `defaultBudgets`:113 |
| `HostResourceDomain` | 同包 | 13 | —— |
| `JvmResourceSampler` | 同包 | 45 | —— |

**已在生产接线：** `DominionServerRuntime.java:159` `public HostResourceGovernor resources()`；`TerminalPlanExecutor.java:47` 预留 `plan.reservedHeapBytes()`。

`BudgetCategory.java`（33 行）恰好 6 个常量：`TERMINAL:5` `CAPABILITY:6` `DISCOVERY:7` `ADMISSION:8` **`OBSERVATION:9`** `MAINTENANCE:10`。

> **无分歧。** 三方（原稿 ①、fix6 回执、本补遗）对"六类存在、但缺 `(surface, source)` 二级归集"的判断一致。`OBSERVATION` 作为一级类别**本来就存在**（200µs/256），① 要的是**二级归集维度**，不是新建类别。这一点希望在后续文档里不要再被读成"观测成本模型不存在"。

---

### 1.2 `TerminalOperationPlan` 四个成员 — 存在，但是**固定常量**

**文件：** `src/main/java/dev/ronova/coremod/root/api/TerminalOperationPlan.java`，**41 行**（在 `root.api`，**不是** `runtime`）。

```
:10   long configuredPolicyRevision, String ownerIdentity, String completionContract,
:11   List<Step> orderedSteps, Set<RuntimeSurface> requiredSurfaces, Set<RuntimeSurface> optionalSurfaces,
:12   long reservedHeapBytes, String recoveryPolicy) {
```

**但现在每个只装一个定值：**

```
:19     reservedHeapBytes != 16384            → throw
:22-24  requiredSurfaces == {REGISTRY, TEMPORAL, DATA}
        optionalSurfaces == {NETWORK, PRESENTATION}
:30-36  工厂 terminal(...) 把四个全部硬编码
:25     ownerIdentity 钉为 "root/" + qualification.subjectKey().realmId().canonical()
```

**另外：全仓没有 `RootOwner` 类型**（检索零命中）。`ownerIdentity` 是一个 String，不是类型化的 Root owner 对象。

**这条为什么重要：** ⑦ 真正需要的是**逐计划可变的预算切片**。载体在，但把定值变成可变值不是免费的——**得先定义谁有权改 `reservedHeapBytes`、改的时候走哪个 revision、旧的切片怎么撤销**。否则 ⑦ 落地时会撞上一个"字段存在但语义锁死"的坑。

---

### 1.3 `CompletionTracker` — 确认，但"超时"不是"到期"

**文件：** `src/main/java/dev/ronova/coremod/runtime/CompletionTracker.java`，**88 行**。

**确实已存在：**

```
:15   public record Scope(UUID commandId, String authorityHash, SubjectQualification qualification,
                          long activePolicy, long configuredPolicy, String contract)
:32   public record Evidence(State state, long dependencyRevision, String reference)
:36   VERIFIED 要求 revision 非零
:58   boolean changed = !next.equals(scope) || !snapshot.equals(previous);
:62   else if (changed || gap || stableSince < 0) stableSince = now;      ← 依赖变化重置稳定窗口
:9    public static final long STABLE_TICKS = 20, MAX_SAMPLE_GAP = 10;
:76-84 if (now < sampledAt || now - sampledAt > MAX_SAMPLE_GAP) { stableSince = -1; ... }
:73   /** A silent observer cannot leave a previously valid certificate displayed forever. */
```

**测试佐证：** `CompletionTrackerSelfTest.java:33-34`
`var expired = tracker.current(56); require(!expired.complete() && expired.fenced(), "stopped observer left completion green");`

**但以下两点不成立：**

1. **没有到期字段。** `:76-84` 是**距上次采样的间隔上界**（10 tick），不是证书 TTL，类里**没有 `expiresAt`**。
2. **没有声明式期望版本集。** "dependency versions" 实际是每 Condition 一个 `long dependencyRevision` 加整快照相等；**没有版本匹配校验**。

**真正的挂钟到期范式在别处，⑤ 该沿它：**

```
RecoverySourceEvidence.java:8    public static final long MAX_AGE_NANOS = 5_000_000_000L;
RecoverySourceEvidence.java:13-15 public boolean fresh(long now) {
                                     ... return observerVerified && age >= 0 && age <= MAX_AGE_NANOS; }
ShadowDurableRootStore.java:773  || !evidence.fresh(System.nanoTime())
```

`fresh(now)` 是**显式的挂钟有效期 + 已在使用点强制**，比 `CompletionTracker` 的采样间隔更接近 ⑤ 需要的形状。

---

### 1.4 owner/lease/claim 守卫 — 存在；但"恰好一个结算归属者"**不存在**

**在的（全部本轮读过）：**

| 机制 | 文件 | 行数 | 关键 |
|---|---|---|---|
| 租约 | `runtime/PlanExecutionLease.java` | 34 | `owner = Thread.currentThread()`:11 · `valid()`:24-28 = 线程限定 + `!closed` + `plan.equals(expected)` + `elapsed < duration` · `MAX_DURATION_NANOS`:9 |
| 单活跃互斥 | `root/api/RootExecutionFence.java` | 73 | `if(active!=null)return null;` 见 `:15,20,29,34,38,41` · `ownsStorage`:24 · `:20` 要求 `authority.owner() && authority.settled()` · `:71-72` "Expiry revokes further writes but never forcibly releases a possibly paused writer." |
| 权威戳 | `root/api/AuthorityStamp.java` | 24 | `:8` `long policyRevision, boolean owner, long ownerAuthorizationRevision, boolean settled,` |
| 归属绑定 | `root/api/OwnershipBinding.java` | 10 | `:6` `record OwnershipBinding(long revision, SubjectKey owner)` · `:5` "Null owner is a retained revocation record, never an implicit default owner." |
| 每域唯一 | `SubjectRevisions.java` | —— | `:9` `Map<StateDomainId, OwnershipBinding> ownership` · `:32-39` `own(domain, owner)` 每域替换单绑定 · `:16` `ownership.size() > 128` |
| 解码去重 | `SubjectRevisionNbtCodec.java` | —— | `:51` `if (ownership.putIfAbsent(domain, binding) != null) throw new IllegalArgumentException("duplicate ownership domain");` |
| 一次性主张 | `EffectClaim.java` / `LifecycleClaimResult.java` / `EffectJournal.java` | 18 / 33 / —— | `LifecycleClaimResult:28` `ALREADY_CLAIMED_EFFECT_UNKNOWN` · `EffectJournal:251` 槽位重复即抛 |
| 测试 | `RootExecutionFenceSelfTest.java` | —— | `:14,16,32-38,59` 断言互斥 + owner/settled 闸 |

**不在的：**

- 全仓 `settlement` / `Settlement` / `SettlementOwner` / `settleOwner` / `settledBy` **零类型声明**。
- "settlement" 只出现在注释里（`RecoveryTaskDisposition.java:62,71,135,149`）和 recovery-work 事实里（`RecoveryWorkResult.Fact`，经 `ShadowDurableRootStore.observeRecoveryWork` 按 `EffectKey` 观察）。
- **没有任何"一次结算恰好一个归属者"的检查。**

**最接近的两个结构保证都不是它：**
- `RootExecutionFence` 的单活跃租约 = **Root 写令牌**的排他，不是结算归属；
- ownership Map = 每 `StateDomainId` 至多一个 `OwnershipBinding`，是**域归属**，不是结算归属。

> **这条对 fix6 的定位有影响：** ⑥ 不是"扩展一个已有断言"，而是"**在已有 owner/lease/claim 基础上新建那个断言**"。它放在第一批仍然合理（守卫齐备，只缺那一个检查），但工作性质是**新增**而非**扩展**，两者的验收写法不同。

---

### 1.5 `DurableTerminalWatchSet` — 确认；**原稿 ② 的前提被推翻**

**文件：** `src/main/java/dev/ronova/coremod/terminal/DurableTerminalWatchSet.java`，**恰好 463 行**。

**持久成员资格的权威是不可变投影，不是弱引用：**

```
:36-38    "Logical membership is reconstructed from the immutable Tombstone projection and
           therefore has no retry-count expiry; physical references remain weak and all work
           is count/time budgeted."
:318-322  private boolean logicallyRetained(Entry entry, AdmissionProjection current) {
               return entry.durableBacked ? current.hasTombstone(entry.tombstoneId)
                                          : current.matches(entry.subject); }
:251-255  if (!logicallyRetained(entry, current)) { remove(entry.mark.uuid(), Completion.TOMBSTONE_CLEARED); return; }
:127-128  AdmissionProjection current = projection.get(); refreshSeed(current);
:221-230  按 projection epoch 短路；:232-249 pendingSeeds = current.marks() 重新播种
```

**持久条目建立时弱引用是空的：**

```
:443-451  private static Entry durable(JudgmentMark mark) { ... entry.durableBacked = true;
              entry.tombstoneId = mark.uuid(); entry.reference = new WeakReference<>(null); return entry; }
:83       entry = Entry.durable(mark);
```

**弱引用只是三个实体解析候选之一：**

```
:256-270  Entity referenced = entry.reference.get();          ← 候选一：弱引用
          for (Entity candidate : observation.snapshot(entry.mark.uuid()))  ← 候选二：TopologyObservation
          Entity direct = level.getEntity(entry.mark.dimension());          ← 候选三：level 权威索引
:287-288  // Presence is written by the validated plan; a weak observation cannot authorize it.
          SpawnFirewall.reassert(candidate);
:301-302  // Convergence is not disappearance. Retain the weak reference for later drift; it still
          // cannot keep the physical entity alive after its real owners release it.
:304-306  if (!entry.durableBacked && entry.absentCycles >= TRANSIENT_ABSENT_LIMIT) { remove(...); }
```

**结论：** 弱引用状态是**活体 incarnation 的缓存/索引**，叠在权威状态（投影 tombstone + `JudgmentMark`/`SubjectIdentity` + level 索引 + `authorityView.isProtected`）之上。**由弱引用缺席决定的唯一权威路径是 transient 条目**（`observeIncarnation` `:102-122` 建立，按构造从未 tombstone-backed）。

> **原稿 ② 那句"持久死亡权威目前由弱引用轮询兜着"被 `:318-322` 和 `:304` 直接反驳。** 我们接受这个反驳，② 的前提不成立。

---

## 2. 三条请回给研究方的精度修正

不是重复，是**在你们结论上再收一格**：

**（一）`TerminalOperationPlan` 的四个成员目前是固定常量。**
`reservedHeapBytes != 16384` 即抛（`:19`），表面集精确相等（`:22-24`），工厂全硬编码（`:30-36`）。所以"载体存在"与"⑦ 可落地"之间还有一段：**要先定义谁有权改这些定值、走哪个 revision、旧切片如何撤销**。建议写进 ⑦ 的前置，别让它到实施时才发现。

**（二）"恰好一个结算归属者"确实不存在。**
全仓零 `settlement` 类型声明。⑥ 的性质是**新建**该断言，不是扩展已有断言——已有的 owner/lease/claim 是它的**基础**，不是它的**实现**。建议在批次表里把 ⑥ 的措辞从"扩展已有守卫"细化为"在已有守卫上新增结算端口唯一性检查"。

**（三）`CompletionTracker` 没有到期字段，其"超时"是采样间隔陈旧界。**
真正的挂钟到期范式是 `RecoverySourceEvidence.MAX_AGE_NANOS`（`:8`）+ `fresh(now)`（`:13-15`），已在 `ShadowDurableRootStore:773` 强制。⑤ 该沿后者。另外 ⑤ 要的**声明式依赖集**目前不存在——只有每 Condition 一个 `long dependencyRevision` 加整快照相等，**没有版本匹配校验**。这一点支持 ⑤ 保留在第二批。

---

## 3. 核验后九条的状态

| 方向 | fix6 回执 | 核验后 |
|---|---|---|
| ① 观测成本模型 | 接受 | 一致。"六类存在、缺二级归集"三方无分歧 |
| ② 对象生命期账本 | 修改 / 条件第四批 | **前提被推翻**（见 1.5）。只剩"补回收观测"，方向实质性减弱 |
| ③ JIT 时机调度 | 修改 / 先测 | 一致 |
| ④ 类血缘执行器 | 修改 / 第二批 | 一致 |
| ⑤ 证书到期闸 | 修改 / 第二批 | **实质仍缺**：无到期字段、无声明式依赖集。沿 `RecoverySourceEvidence.fresh()` |
| ⑥ 结算归属断言 | 修改 / 第一批 | **实质仍缺**：无单归属检查。性质是新建，不是扩展 |
| ⑦ 跨表面预算分配 | 接受 / 第三批 | **实质仍缺**：载体是固定常量，需先解锁定值 |
| ⑧ 行号表信号 | 废弃 | 一致，无异议 |
| ⑨ 绕过面报告 | 接受 / 第一批 | 一致 |

**没有要求改批次。** 上面的修正只影响 ⑥ 的工作性质描述、⑤ 的实现路径选择和 ⑦ 的前置，不影响它们的批次位置。

---

## 4. 边界与口径

- **L0 三条铁律与 UX1 不变。** 本补遗只读源码，不修改任何依赖/Mod/Forge 文件、不针对、不删除对方文件、不起外部进程。
- 本补遗**不新增** `SubjectKey`、Root、或不变量编号（D*/INV*/A*/R*/ID-*/T*）。
- **头文件/字段存在 ≠ 运行时可用；字面命中数 ≠ 实现状态。** 本补遗全部结论来自静态读取。
- **未做：** 未编译、未启动 JVM/游戏、未做 GC/JIT/隐藏类场景测试、未改 `validation/`、未回填任何 PASS。
- 本补遗**不撤销** fix6 的任何回执结论，只在其上加三处精度修正。
