# Ronova Internal Root 路线修订摘要 — 2026-08-29

> 历史归档（2026-09-08 整理）：保留当时的设计、实现和验证口径，不代表当前源码状态。文中的“当前”“下一步”属于记录当时。
> 当前入口：[进度](../../CURRENT-PROGRESS.md) · [有效规划](../../plans/README.md)。

> 修订记录与交叉审计摘要；不取代四份 canonical 架构文档，也不代表当前 r50 已通过完整运行验收；其中的 r27 进度只属于历史记录。

## 冻结决策

Ronova 主线取消 External Dominion Root / 持续 external companion Authority process。

```text
FULL L5
DurableRootStore
= authoritative durable store/writer

8.x
Internal Root Consolidation

FULL L6 Internal
InternalDominionRoot
= sole logical Authority writer
        ↓
DurableRootStore
= sole durable backend
```

## 8.x 新路线

1. `8.0 Internal Root Shadow`
2. `8.1 Exclusive Root Gateway`
3. `8.2 Internal Authority Cutover`
4. `8.3 Recovery & Presence Closure`
5. `8.4 FULL L6 Internal Release Gate`

## 删除的 mandatory 设计

- External Root executable / service
- Named Pipe
- authenticated cross-process IPC
- HMAC/HKDF transport handshake
- PID/DACL transport binding
- second-JVM / companion process requirement
- External Root HA / replica consensus

## 保留并内部化的语义

- single writer
- `DominionEpoch`
- `RootInstanceId`
- `FenceToken`
- `command_id`
- canonical payload hash
- `commit_index`
- durable receipt / dedup
- `COMMIT_UNKNOWN`
- anti-rollback checkpoint lineage
- immutable projection
- fail-static / fence / repair / revalidation

## COMMIT_UNKNOWN 新定义

不再来自网络 ACK 丢失，而来自：

```text
Durable write 可能已经成功
        ↓
JVM crash / I/O ambiguity / receipt 未返回
        ↓
COMMIT_UNKNOWN
        ↓
recover same command_id + same canonical hash
```

## 能力边界变化

FULL L6 Internal 不再宣称：

- truth 位于 JVM 外故障域；
- JVM 完全失陷时 Root 仍物理不可达；
- 两个独立 JVM 的 clone/fork 可以由同一外部 Root 全局仲裁。

FULL L6 Internal 仍保证（在声明的 JVM/进程工作区间内）：

- 唯一逻辑 Authority writer；
- direct Store writer bypass = invariant violation；
- stale root/fence/epoch reject；
- durable tombstone / terminal dominance；
- command idempotency / recovery；
- projection atomicity；
- P0-A/B/C/D 可继续在内部 Root 上扩展。

## 对当前 7.14A–7.14D / 7.15–7.17 的影响

当前阶段不引入 External Root/companion Authority service，也不需要因为路线调整回滚 7.14A/B/C/D。r27 现有 Native-failure `AutoAttach` helper 现归类为允许的 bounded bootstrap/attach path；它无需为进入 FULL L5 而强制删除，但必须满足后文 D13/`PROC-INV-1` 的角色、生命周期与 handoff 约束。

`7.15 → 7.16 → 7.17 FULL L5` 基本保持原方向；真正改变的是 `7.17` 之后的 `8.x`。

## Consistency review corrections

Post-pack review corrected several mechanical remnants from the External→Internal rewrite without changing the frozen architecture:

- fixed the Master Plan header typo that accidentally said `InternalDominionRoot` was cancelled;
- removed the obsolete `ipc/` layer from the target dependency DAG;
- replaced transport-era `ACK loss / reconnect / connection timeout` tests with local durable-write / receipt ambiguity and JVM-restart recovery semantics;
- replaced stale `session` semantics with `RootInstanceId` / writer-fence semantics;
- corrected P0-A wording to state that it no longer assumes an **External** Root and instead consumes an already-authoritative `InternalDominionRoot`;
- renamed execution-lease `RootHolderId` to `ExecutionHolderId` and defined it as an in-JVM executor identity;
- clarified that External-facing Presence means public/query/render visibility only, never an external process.

## r27 Source Reconciliation — 2026-08-29

本轮把路线文档与 `r27(1).zip` 的可审计源码、Native 源码和 Windows PE 导入做了逐项对照；其中 Native/PE 输入未随当前工作区公开，本轮仅保留为 DOCUMENT-ONLY 记录：

- 当前工作区 Java 源码确认 `RonovaDominion → NativeBridge.isActive() → NATIVE_ACTIVE` 的正常 Native-active 路径不创建 helper process；
- 当前工作区 Java 源码确认 Native 不可用且 `StartupPolicy=DUAL_AGENT` 时，`AutoAttach` 会用 `ProcessBuilder` 启动短命 `AttachHelper` JVM，再通过 Attach API 注入 agent；
- 候选包报告声称 Native C++ 源码只创建 watchdog thread，PE import 未发现 process creation、Named Pipe、Socket 或其他跨进程 Root transport；该结论本轮不作为重新确认的源码事实；
- 因此当前 Java 源码支持“没有 External Root/service/Named Pipe Authority channel”的路线结论；production runtime 的 bounded helper path 仍需 exact-artifact process-tree 验证；
- D13 / `PROC-INV-1` 的最终冻结语义见下节：不要求 child-process count=0，而要求所有 helper/relaunch 都非权威、用途有界、正确 handoff/termination；
- `AutoAttach` / `AttachHelper` 不再是 pre-L5 mandatory deletion item。

---

## 2026-08-29 Bootstrap / Restart Boundary Reconciliation

The earlier strict-zero-child-process interpretation of D13 / `PROC-INV-1` is superseded.

New frozen rule:

> Ronova may use bounded helper/restart processes for bootstrap, Agent attach, recovery or JVM/Minecraft relaunch. Authoritative combat state, Judgment/Terminal truth, ownership, durable Root mutation and continuing enforcement ownership must remain in the currently active Minecraft JVM. No helper/sidecar/service may become a durable Authority writer, persistent combat plane, independent target-monitoring/attack plane or External Dominion Root.

Consequences:

- r27 `AutoAttach` / `AttachHelper` is no longer scheduled for mandatory deletion before FULL L5;
- a controlled relaunch with launch-time Agent is permitted if the old JVM exits and the new Minecraft JVM becomes the sole active runtime;
- release verification changes from "child process count = 0" to "all process-launch sites enumerated, purpose-bounded, non-authoritative, and terminated/handed off correctly";
- External Root/service/Named Pipe/local-loopback/shared-memory Authority channels remain forbidden;
- Native/JNI/JVMTI remain in-process implementation layers and do not become competing truth writers.

## 2026-08-29 r27 Progress Reconciliation（历史记录）

The working source head is now documented separately from the historical r12 7.14A frozen evidence:

- current source: `0.2.0-alpha.7.14b-r27`;
- registered standalone SelfTests: `34`;
- 7.14B Authority Split: source implemented/wired;
- 7.14C Presence/OwnerImmunity Split: source implemented/wired;
- 7.14D: next stage;
- r12 `32/32 PASS` remains historical evidence only.

A new 7.14D FrameBudget / Telemetry Implementation Spec V1.0 was added. It explicitly forbids deferring synchronous `AuthorityView` / `PresencePolicy` decisions when a frame-work budget is exhausted.
