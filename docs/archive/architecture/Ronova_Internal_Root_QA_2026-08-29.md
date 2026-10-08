# Ronova Internal Root revision pack — consistency QA

> 历史归档（2026-09-08 整理）：保留当时的设计、实现和验证口径，不代表当前源码状态。文中的“当前”“下一步”属于记录当时。
> 当前入口：[进度](../../CURRENT-PROGRESS.md) · [有效规划](../../plans/README.md)。

Status: **HISTORICAL DOCUMENT PASS / r27 JAVA SOURCE RECONCILED / 14D DOCUMENTATION READY**

本 QA 不把候选包外部提供的 Native/PE 静态结论冒充为本工作区重新验证的事实；这些结论在本轮报告中归为 DOCUMENT-ONLY，除非对应原始源码、二进制审计输入或运行证据可直接追溯。

Scope: architecture/document consistency plus read-only inspection of the r27-era workspace Java source. Native source and Windows PE import conclusions are carried from the supplied report and are not reverified in this workspace. This historical QA does not claim a new r27 exact-artifact Forge release PASS, and it is not the current r50 status record; current status is maintained in `docs/CURRENT-PROGRESS.md`.

## Checked

- canonical document authority order;
- FULL L5 vs FULL L6 Internal writer semantics;
- removal of External Dominion Root / mandatory cross-process Root transport;
- bounded helper/restart process policy;
- `COMMIT_UNKNOWN` semantics after removal of network ACKs;
- `FenceToken` / `RootInstanceId` / `commandSequence` separation from `DominionEpoch`;
- P0-A assumption that FULL L6 Internal Root is already authoritative;
- P0-D fixtures no longer depend on stale network sessions;
- Markdown code-fence balance;
- standalone-pack link behavior;
- r27 历史审计对象的 production startup path (`RonovaDominion` → `NativeBridge` → `AutoAttach`);
- r27 历史审计对象的 7.14B/7.14C source presence and wiring;
- r27 历史审计对象的 registered standalone SelfTest count (`34`);
- 7.14D synchronous-vs-deferrable budget boundary。

## Corrections applied

1. `CURRENT-PROGRESS` no longer describes r12/7.14A as the current working source head.
2. r12 `32/32 PASS` remains historical 7.14A frozen evidence; it is not presented as r27 evidence.
3. r27 is recorded as `0.2.0-alpha.7.14b-r27`, with 7.14B and 7.14C source implementations present and 7.14D next.
4. Canonical document versions are advanced to L6 V2.4 / Baseline V1.4 / L6E-0 V1.3 / P0-A V1.3.
5. D13 / `PROC-INV-1` is no longer a strict zero-child-process invariant.
6. `AutoAttach` / `AttachHelper` is allowed when it is a bounded bootstrap/attach helper carrying no Authority truth and terminating after handoff.
7. Controlled JVM/Minecraft relaunch is allowed when the old JVM exits and the successor becomes the sole active game runtime.
8. External Dominion Root, persistent companion Authority, independent external combat/target-monitoring plane and canonical cross-process Root transport remain forbidden.
9. 7.14D receives a dedicated implementation spec with a hard scope boundary.
10. `PresencePolicy` / `AuthorityView` are explicitly synchronous hot-path queries and may be measured but not deferred by FrameBudget exhaustion.

## Source-grounded current state

- `gradle.properties`: `mod_version=0.2.0-alpha.7.14b-r27`, Forge `47.4.22`.
- `build.gradle`: 34 registered standalone SelfTests.
- 7.14B source: `AuthorityView`, `AuthorityBinder`, `LegacyAuthorityView`, `AuthoritySplitFlags`.
- 7.14C source: `PresencePolicy`, `OwnerImmunity`, `PresenceSplitFlags`; `ProjectionCoordinator` wires `PresencePolicy(OwnerImmunity.of(topology::owns))`.
- `AutoAttach` remains a conditional helper path under Native-unavailable + `DUAL_AGENT`; this is now compliant in principle with D13 provided bounded-role verification passes.

## Remaining evidence, not document blockers

- a separately attached, source-pinned r27 exact-artifact build/test report if a release-grade claim is needed; the workspace contains the r27 JAR and prior build result, but no standalone `verification-report.json PASS` is attached to this revision pack;
- real Windows process-tree evidence if `PROC-INV-1` is being release-gated;
- 14D implementation and its dedicated self-tests;
- later 7.15+ migration evidence.
