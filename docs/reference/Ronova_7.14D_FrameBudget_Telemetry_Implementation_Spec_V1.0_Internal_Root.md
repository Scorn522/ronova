# Ronova — 7.14D FrameBudget / Telemetry Implementation Spec V1.0 — Source-Reconciled Internal Root

> 已实施阶段的有效参考合同：预算与同步查询边界继续适用，正文的 r42/r50 状态属于历史。当前施工见 [进度](../CURRENT-PROGRESS.md)。

> **Status:** IMPLEMENTATION SPEC FROZEN — SOURCE RECONCILED  
> **Stage:** frozen 7.14D companion; 7.15 DurableRootStore Shadow follows in the current r50 source line
> **Source evidence head:** r42 (`0.2.0-alpha.7.14b-r42`)
> **Current working source:** r50 (`0.2.0-alpha.7.15-r50`)
> **Authority:** subordinate to Canonical L6 V2.4 + Baseline V1.4  
> **Implementation status:** 本文是 14D implementation contract。r42 已完成 7.14D Java implementation candidate；其预算、同步查询、continuity repair、retry worker 与 receipt accounting 边界仍由当前 r50 继承。7.15 DurableRootStore Shadow 不在本文的 14D scope 内。

## 1. Objective

7.14D only introduces a common bounded-work seam and structured telemetry seam.

```text
有限预算
可测量
可回滚
```

It does **not** change Authority truth, Presence semantics, Terminal lifetime, retry lifetime, durability, Judgment batching, or Root ownership.

## 2. Hard scope boundary

Allowed in 7.14D:

- per-server `FrameBudgetGovernor`;
- stable budget category IDs;
- count/time receipts;
- structured immutable telemetry snapshots;
- backlog/over-budget counters;
- rollback flag selecting legacy/unbudgeted scheduling where required;
- wiring budget checks into work that is semantically safe to defer;
- measurement-only instrumentation on synchronous critical queries.

Forbidden in 7.14D:

- `DurableRootStore` implementation/cutover;
- TerminalWatch durable takeover;
- deleting the existing finite retry mechanism;
- `JudgmentBatch` durable transaction cutover;
- adaptive STORM controller/backoff redesign;
- P0-A identity migration;
- P0-B Dispatch / AllReturn;
- P0-C Convergence/Reconstitution or full resource-resilience takeover;
- P0-D complete Integrity Authority closure, repair or revalidation engine;
- 7.15+ durable truth migration or `DurableRootStore` cutover;
- Native Render Firewall;
- changing `PresencePolicy` or `AuthorityView` truth semantics.

## 3. Canonical package

Use one canonical package only:

```text
dev.ronova.coremod.budget
├─ FrameBudgetGovernor
├─ BudgetCategory
├─ WorkBudget
├─ BudgetReceipt
├─ BudgetTelemetrySnapshot
└─ BudgetSplitFlags
```

Do not introduce parallel `scheduler/`, `storm/`, `resource/` or second governor implementations in 14D.

## 4. Budget categories

Initial stable categories:

```text
TERMINAL
CAPABILITY
DISCOVERY
ADMISSION
OBSERVATION
MAINTENANCE
```

`PRESENCE_QUERY` and `AUTHORITY_QUERY` may appear as telemetry-only categories, but **must not be deferred**.

## 5. Synchronous vs deferrable rule

### 5.1 Deferrable work

The following may stop when their budget is exhausted and resume on a later tick, provided their existing semantic ordering is preserved:

- the existing finite retry worker in `JudgmentEngine.tick()` (the current terminal worker, capped by `RETRY_BUDGET_PER_TICK = 256`);
- constructed/admission queue drain;
- an explicit stale observation/reference maintenance drain owned by `TopologyObservation` / `TopologyGraph`;
- telemetry/maintenance flushing.

These are scheduler seams only. `PassiveTerminalWatchSet` never owns this work and never schedules, resolves or reasserts an entity.

### 5.2 Synchronous critical queries and continuity repair

The following remain synchronous and complete for the current gameplay call:

```text
AuthorityView query
PresencePolicy / QuarantineView decision
admission decision for the object currently entering
Judgment control-plane decision
TargetDiscovery.ray / area / loaded query
```

`TargetDiscovery.loaded()` is a synchronous gameplay discovery query. It must scan and return the complete eligible target set for that call. A 14D budget exhaustion may record elapsed time, candidate count and an over-budget signal, but it must never return a partial set or carry the current attack's remaining targets into a later tick.

The following inherited authority/integrity maintenance paths are also measurement-only in 14D:

- `RuntimeCoordinator` HEAD `maintainAnchors()`;
- `RuntimeCoordinator` TAIL `maintainAnchors()`; the HEAD + TAIL pair is continuity repair and remains double-executed;
- `DominionServerRuntime.reconcileNative()`;
- `EnforcementRuntime.pulse()`.

No budget exhaustion may skip, reorder or defer those paths in this stage. The implementation must account for both HEAD and TAIL calls separately while preserving their current execution order.

Budget exhaustion must **not** produce:

- "defer Presence until next tick";
- false/unknown substitution for a known Authority result;
- temporary visibility caused only by budget exhaustion;
- mutation of Authority truth.

For synchronous queries 14D records latency/counters only: `FrameBudgetGovernor` may measure, account, report and performance-gate them, but **MUST NOT** defer their synchronous result because a frame budget is exhausted. If latency exceeds the gate, the implementation fails performance validation; it does not change semantics to fit the budget.

## 6. Initial per-server frame envelope

The governor is per `MinecraftServer` runtime. Budgets are not globally shared across integrated/dedicated server instances.

Initial engineering envelope:

```text
TICK_BUDGET_US = 2000 us per server tick
terminal      <= 500 us
capability    <= 400 us
discovery     <= 300 us
admission     <= 400 us
observation   <= 200 us
maintenance   <= 200 us
```

These are initial tuning values, not Authority semantics. Changing them must not change the set of objects that ultimately converge to the same authoritative state.

## 7. Minimal API contract

以下是语义冻结的最小 API；Java 拼写只有在不改变这些语义时才可调整：

```text
FrameBudgetGovernor.beginTick(long tickId)
WorkBudget budget = governor.budget(BudgetCategory category)
boolean allowed = budget.tryConsumeCount(int count)
boolean remaining = budget.hasTimeRemaining()
BudgetReceipt receipt = budget.finish(long processed, long deferred)
BudgetTelemetrySnapshot snapshot = governor.snapshot()
```

`tryConsumeCount` 返回 false 时，调用方必须保留未处理工作并由 receipt 的 `deferred` 字段记录积压，不能静默丢弃；`hasTimeRemaining` 只约束可延期工作，不得成为同步关键查询的放行条件。

Requirements:

- monotonic tick identity within one server runtime;
- no wall-clock timestamps as truth;
- budget accounting failure never mutates Authority;
- snapshot is immutable/read-only;
- telemetry reads have no gameplay side effects;
- no caller may "borrow" another category's budget in 14D.

## 8. Rollback

Add one 7.14D selection point:

```text
ronova.714d.frameBudget.enabled
RONOVA_714D_FRAME_BUDGET_ENABLED
```

Default: enabled after the 14D implementation candidate is built.

Invalid explicit configuration must select the documented LEGACY rollback scheduling path and emit one warning, matching the 14B/14C split pattern. Rollback changes scheduling only; it must not restore pre-14A Raw/Presence coupling or pre-14B Authority side effects.

## 9. Required wiring for 14D

Minimum implementation candidate wiring:

1. `RuntimeCoordinator.serverTickHead()` is the per-server dispatch seam. It acquires one `FrameBudgetGovernor` for the current server runtime; the paired TAIL path closes the same tick receipt. Budgets are never process-global.
2. `TargetDiscovery.loaded()` remains a synchronous full-set query. It receives measurement-only discovery accounting; it does not consume a deferrable budget, segment its result, or schedule a continuation.
3. `SpawnAdmissionRuntime.drainConstructed()` gains bounded drain accounting. Unprocessed weak references remain queued and are represented as deferred work; they must not be silently dropped.
4. stale observation/reference maintenance gains an explicit bounded drain at the existing per-server observation owner (`TopologyObservation` / `TopologyGraph`); the maintenance path must not import or call `presence`.
5. `maintainAnchors()` in both HEAD and TAIL is inherited continuity repair in the r42 source line and remains measurement-only, double-executed. `reconcileNative()` and `enforcement().pulse()` are also measurement-only because no safe deferral evidence exists in this stage. No capability truth or explicit repair boundary changes.
6. `JudgmentEngine.tick()` remains the actual TERMINAL worker and its existing finite retry lane remains the only terminal scheduler. It consumes the TERMINAL budget seam without changing retry delays, lifecycle ownership or `RETRY_BUDGET_PER_TICK = 256`. `PassiveTerminalWatchSet` remains a diagnostic/telemetry mirror only.
7. `PresencePolicy` and `AuthorityView` receive telemetry-only latency/call counters; no deferral and no scheduler dependency may be introduced.

The only behavior-changing scheduler gates in 14D are the explicitly deferrable admission, observation and finite retry drains. If a seam cannot preserve ordering and ownership, it stays LEGACY/measurement-only behind the rollback flag. No implementation may fake boundedness by dropping work or by returning a partial synchronous query.

## 10. Telemetry

### 10.1 Frozen telemetry model

The following model is the minimum immutable, bounded data contract:

```text
BudgetReceipt {
  tickId
  category
  attempted
  processed
  deferred
  carryIn
  carryOut
  elapsedUs
  overBudget
  maxObservedBacklog
}

SynchronousQueryTelemetry {
  queryKind              // AUTHORITY_QUERY or PRESENCE_QUERY
  callCount
  totalElapsedUs
  maxElapsedUs
}

BudgetTelemetrySnapshot {
  tickId
  snapshotEpoch          // diagnostic only
  categoryReceipts       // immutable, one receipt per enabled category
  synchronousQueries     // bounded counters/ring only
  overBudgetCounts       // immutable per-category counters
}
```

`attempted` means the work units actually covered by this receipt, including a unit rejected by an exhausted budget. For every receipt, `processed + deferred == attempted`. `carryIn` and `carryOut` are explicit cross-tick backlog observations; they must never be folded into `attempted` or used to excuse an accounting mismatch. A worker that stops before claiming the next unit leaves that unit in `carryOut`; it does not invent a deferred count for work that was never attempted.

`BudgetTelemetrySnapshot` must use immutable copies and bounded counters/ring buffers; it must not retain per-entity history or unbounded log text. `snapshotEpoch` is diagnostic context only and never an Authority version.

At minimum expose per category:

- work items attempted;
- work items completed;
- deferred/backlog count;
- elapsed microseconds;
- over-budget count;
- max observed backlog;
- tick id / snapshot epoch for diagnostics only.

For synchronous query telemetry:

- call count;
- total elapsed;
- max elapsed;
- optional sampled histogram/ring buffer.

Telemetry must be bounded. No unbounded per-entity history, stack trace accumulation, or per-tick log spam.

## 11. 14D acceptance gate

The 14D implementation candidate may be marked complete only when all of the following pass:

- `FrameBudgetGovernorSelfTest` — category isolation, reset-per-tick, receipt accounting, no negative budget;
- `BudgetRollbackSelfTest` — enabled/default/invalid/legacy selection behavior;
- `BudgetSemanticsSelfTest` — exhausted budget defers work but never drops it;
- `PresenceBudgetSeparationSelfTest` — Presence/Authority decisions remain synchronous and semantically identical under zero remaining deferred-work budget;
- existing `AuthoritySplitSelfTest`, `PresenceSplitSelfTest`, `RawObservationSeparationSelfTest`, `HotPathProjectionSelfTest`, `ProjectionWiringSelfTest` remain PASS;
- static scan confirms `dev.ronova.coremod.presence` and `authority` do not depend on a deferred scheduler to answer a synchronous query;
- representative load test shows bounded per-tick worker cost and no unbounded telemetry growth.

## 12. Exit condition

r42 已达到本文所定义的 7.14D 本地 implementation-candidate 状态：common budget/telemetry seam、rollback、`mc_check`、`mc_build` 与 38 个 standalone SelfTest 均已通过。当前 r50 已在其上接续 7.15 DurableRootStore Shadow；该结论不豁免 representative load、Windows Forge、GPU、process-tree 或独立发布证据。

7.14D 不以实现 FULL resource resilience 为结束条件。Adaptive backoff、storm classification、durable watch ownership 与 control-plane batching 仍属于后续阶段。
