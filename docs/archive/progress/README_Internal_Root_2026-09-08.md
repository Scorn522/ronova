# Ronova architecture chain — Internal Root revision

> 历史归档（2026-09-08 整理）：保留当时的设计、实现和验证口径，不代表当前源码状态。文中的“当前”“下一步”属于记录当时。
> 当前入口：[进度](../../CURRENT-PROGRESS.md) · [有效规划](../../plans/README.md)。

These documents are the canonical design chain after the Internal Root decision and the 2026-09-05 V2.6 contract-integration revision:

1. [L6 Architecture Master Plan V2.4 — Internal Root](../../plans/Ronova_L6_Architecture_Master_Plan_V2.4_Internal_Root.md) — canonical route and frozen Internal Root decisions.
2. [7.14A Baseline and All Enhancements Master V1.4 — Internal Root](../../plans/Ronova_7.14A_Baseline_All_Enhancements_Master_V1.4_Internal_Root.md) — current-line → FULL L5 → FULL L6 Internal → L6E route.
3. [L6E-0 Contract Freeze V1.3 — Internal Root](../../plans/Ronova_L6E-0_Contract_Freeze_V1.3_Internal_Root.md) — shared cross-layer contracts with no External Root dependency.
4. [7.16+ JVM Extreme Strength Enhancement Master V2.6](../../plans/Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md) — current strength/execution increment, constrained by the preceding shared contracts.
5. [P0-A Truth Authority Implementation Spec V1.3 — Internal Root](../../plans/Ronova_L6E_P0-A_Truth_Authority_Implementation_Spec_V1.3_Internal_Root.md) and stage implementation specs — implementation under the preceding contracts.

Normative authority order is exactly the list above, also defined in V2.6 §1.1. A downstream document cannot weaken an upstream frozen decision. Existing source/JAR evidence records implementation facts; it cannot be silently changed by a design document or used to invent a competing normative hierarchy. UI examples, handoffs and progress reports do not override contracts.

Current-stage implementation companion:

- [7.14D FrameBudget / Telemetry Implementation Spec V1.0](../../reference/Ronova_7.14D_FrameBudget_Telemetry_Implementation_Spec_V1.0_Internal_Root.md)
- [7.17 r53 ProfilePresetMatrix Handoff](../handoffs/7.17-PROFILE-PRESET-MATRIX-HANDOFF.md)
- [7.17 r54 AttackActionPolicy Handoff](../handoffs/7.17-ATTACK-ACTION-POLICY-HANDOFF.md)
- [7.17 r55 Lifecycle Claim Recovery Handoff](../handoffs/7.17-LIFECYCLE-CLAIM-RECOVERY-HANDOFF.md)
- [7.17 r56 Subject Qualification Handoff](../handoffs/7.17-SUBJECT-QUALIFICATION-HANDOFF.md)
- [7.17 r57 Effect Journal Handoff](../handoffs/7.17-EFFECT-JOURNAL-HANDOFF.md)
- [7.17 r58 Root Bootstrap Hotfix Handoff](../handoffs/7.17-ROOT-BOOTSTRAP-HOTFIX-HANDOFF.md)
- [7.17 r59 Qualification Preconditions Handoff](../handoffs/7.17-QUALIFICATION-PRECONDITIONS-HANDOFF.md)
- [7.17 r60 Subject Binding Handoff](../handoffs/7.17-SUBJECT-BINDING-HANDOFF.md)
- [7.17 r61 Deferred Authority Handoff](../handoffs/7.17-DEFERRED-AUTHORITY-HANDOFF.md)
- [7.17 r62 Runtime Inventory Handoff](../handoffs/7.17-RUNTIME-INVENTORY-HANDOFF.md)
- [7.17 r63 Terminal Operation Plan Handoff](../handoffs/7.17-TERMINAL-OPERATION-PLAN-HANDOFF.md)
- [7.17 r64 Obligation Review Handoff](../handoffs/7.17-OBLIGATION-REVIEW-HANDOFF.md)
- [7.17 r65 Obligation Checkpoint Handoff](../handoffs/7.17-OBLIGATION-CHECKPOINT-HANDOFF.md)
- [7.17 r66 Versioned Projection Handoff](../handoffs/7.17-VERSIONED-PROJECTION-HANDOFF.md)
- [7.16 r52 Implementation Handoff — historical](../handoffs/7.16-IMPLEMENTATION-HANDOFF.md)

Historical enhancement snapshots (not parallel implementation authorities):

- [V2.5 — historical](../architecture/Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.5_Internal_Root.md)
- [V2.4 — historical](../architecture/Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.4_Internal_Root.md)
- V2.1 X01–X23 source definitions are not supplied. V2.6 §19.1.1 registers that gap and defines new B01–B23 tests without claiming historical equivalence.

[V2.6 revision and verification note](../reports/DOC-REVISION-V2.6.md) records the historical document-only change set. V2.6 remains Design Freeze Candidate. r54 begins its action sub-policy foundation; r55 repairs existing lifecycle intent persistence recovery; r56 adds acknowledged read-head isolation and per-subject terminal/clear qualification. r57 adds the first Root-owned OPAQUE effect journal and storage preflight. r58 fixes canonical Root bootstrap and lookup-only shutdown after the real r57 old-world failure. Queryable/idempotent/compensation execution, full Binding/Ownership and broader runtime closure remain open. See the respective handoffs for wiring and evidence limits.

Source-grounding evidence:

- [Runtime Process Boundary — r27 Source Audit](../architecture/Ronova_Runtime_Process_Boundary_Source_Audit_2026-08-29.md)
- [Current Progress — r66 versioned projection; 7.17 implementation candidate](../../CURRENT-PROGRESS.md)

These evidence/progress files do not become additional architecture authorities.

## Internal Root freeze

```text
FULL L5
DurableRootStore = authoritative durable store/writer

FULL L6 Internal
InternalDominionRoot = sole logical Authority writer
        ↓
DurableRootStore = sole durable backend
```

Ronova does not use an External Dominion Root executable/service as its Authority plane. Named Pipe, local-loopback, shared-memory IPC or a second JVM must not become the canonical Root command/truth channel.

## Bootstrap / restart process boundary

The route does **not** require absolute zero child processes.

A bounded helper or successor JVM is allowed for:

- Java Agent attach/bootstrap;
- recovery/bootstrap diagnostics;
- controlled Minecraft/JVM relaunch;
- launch-time preparation required to enter the intended in-JVM runtime.

The invariant is instead:

> **All authoritative combat state, Judgment/Terminal truth, ownership, durable Root mutation and continuing enforcement ownership remain in the currently active Minecraft JVM. A helper/restart process may bootstrap or hand off into that JVM, but it may not become a durable Authority writer, persistent companion combat plane, independent target-monitoring process or External Dominion Root.**

Therefore the current `AutoAttach` / `AttachHelper` fallback is not automatically disqualifying. It remains subject to bounded-process verification: it must exist only for attach/bootstrap, carry no durable Authority state, and terminate after handoff.

A full JVM/Minecraft relaunch is also allowed: the old JVM exits and the new Minecraft JVM becomes the single active runtime. Durable truth must recover through the documented store/recovery protocol rather than remain hosted in a sidecar.

## Current source state

The r66 working source reports:

```text
mod_version = 0.2.0-alpha.7.17-r66
Forge build = 47.4.22; r66 world-copy runtime = 47.4.23
network protocol = 10; Native wire unchanged
standalone SelfTests = 52 (50 production JAR + 2 development outputs)
Root schema = 5/6 (checkpoint activation); command journal = 3; effect = 1; qualification = 1/2
policy schema = 3 (54 advanced keys + 14 ordinary actions)
r66 build = PASS
r66 exact final artifact = VERSIONED_PROJECTION_STALE_REJECTION_RESYNC_PASS (rounds 1/2)
r58 original artifact = PRESERVED; current source-world copy inputs = UNCHANGED
```

r66 introduces network protocol 10 with connection-bound Realm/stream/context/revision projection transactions, atomic chunk assembly, Owner authorization versions and bounded resync. Client display remains a read-only projection and does not prove arbitrary renderer completion. Full participant contracts and 7.17 gates remain open.

The inherited r65 implementation persists bounded obligation detail/coverage in the same Root image with independent checkpoint revisions, ACK recovery and anti-forgetting validation. Schema 6 activates only when needed; restart retains evidence but requires revalidation. Periodic unacknowledged observations and full restriction handoff remain explicit limits.

The inherited r64 implementation adds bounded current-context candidate evidence and subject-fair obligation review. Persistent unknown effects reconstruct from the same Root journal; raw detail and overflow remain runtime diagnostics. Production observer attestation, durable candidate restrictions and full recovery execution remain open.

The inherited r63 implementation connects production bound terminal judgments to a Root-derived ordered operation plan. Controlled registry/presence steps, an opaque once-only lifecycle bundle, and dispatch-only network receipts remain explicitly PARTIAL. Legacy/new effect aliases cannot create another attempt; callback revocation fences stale continuation. Generic recovery, completion, actual WriteSink concurrency and broader 7.17 gates remain open.

The inherited r62 implementation adds an eight-category observation registry (four S1, four S0) and a 15-domain resource inventory. Only construction/ingress slot estimates are currently charged to the shared governor; host JMX usage is separate, and other owned domains remain unobserved. Observation-only adapters cannot claim enforcement or FULL. Full allocation coverage and typed Root-plan execution/recovery remain open.

The inherited r61 implementation adds acknowledged authority stamps and scoped Owner authorization history to packet/construction work, physical admission caches and normal/passenger ticks. Raw held entities remain saved and resume after explicit Root rebind. This is not complete third-party scheduler/WriteSink or cross-surface recovery coverage.

The inherited r60 implementation extends the same Root subject record with physical incarnation and independent Binding/Judgment/Admission and per-domain Ownership revisions. Production judgment binds and terminalizes in one transaction; guarded rebind/reactivate/revoke retains effect identity. The Java decision entry enforces requested scope ceilings, and identity-scoped non-player metadata survives storage. Full canonical identity/resolver migration, admission/task/network propagation and actual WriteSink concurrency remain open.

The inherited r58 canonical Root restores independently of derived SavedData descriptions; mirror differences are preserved and rebuilt from validated Root facts. Real corruption and missing-after-cutover still reject startup. Shutdown no longer constructs a runtime, and failed startup retains the original failure while releasing partial resources. The final r58 JAR passed two actual Forge client runs on the same copied old world: enter, save, exit, then reenter. Original world hashes and copied canonical Root bytes remained unchanged.

The existing public-death entry retains a Root-owned durable intent, a one-use attempt entry, conservative EFFECT_UNKNOWN recording, and storage headroom preflight. Six owned child JVM halt scenarios and the delayed opaque callback fixture still pass without effect replay. These fixtures and the scoped client restart test do not establish real third-party effect completion, full Native/WriteSink proof or the complete 7.17 exit gates.

The maintainer selected V2.6 B01–B23 + X24–X68 on 2026-09-06. Historical X01–X23 source remains missing and equivalence remains unproven. The [68-case evidence ledger](../../../validation/7.17/acceptance-matrix.json) records scoped evidence without automatically promoting complete B/X cases or Design Freeze.

r51 is the historical 7.15 frozen input; r52–r57 remain historical implementation evidence. The r55/r56/r57 artifacts and source archives are preserved. [User runtime evidence](../validation/7.17/runtime-r56-user-20260906/REPORT.md) belongs to r56; [r58 round 2](../../../validation/7.17/r58-game-smoke/round-2.json) and [r58 round 3](../../../validation/7.17/r58-game-smoke/round-3.json) record the exact final hotfix artifact. The separate opt-in validation fixture is excluded from the production JAR.

Historical r59 runtime evidence: [round 1](../../../validation/7.17/r59-game-smoke/round-1.json), [round 2](../../../validation/7.17/r59-game-smoke/round-2.json). These use a controlled logical subject and do not establish complete physical binding or arbitrary third-party effects.

Historical r60 final-artifact evidence: [round 5](../../../validation/7.17/r60-game-smoke/metadata/round-5.json), [round 6](../../../validation/7.17/r60-game-smoke/metadata/round-6.json). Initial candidate scope failure and intermediate runs are preserved separately; full 7.17 exit gates remain open.

Historical r61 final-artifact evidence: [round 4](../../../validation/7.17/r61-game-smoke/round-4.json), [round 5](../../../validation/7.17/r61-game-smoke/round-5.json). Static-refmap and scheduling-registration failures are preserved separately. Full 7.17 remains in progress.

Historical r62 final-artifact evidence: [round 1](../../../validation/7.17/r62-game-smoke/round-1.json), [round 2](../../../validation/7.17/r62-game-smoke/round-2.json), [diagnostic export](../../../validation/7.17/r62-game-smoke/round-2.diagnostics.json). Full C/D and 7.17 remain incomplete.

Historical r63 final-artifact evidence: [round 2](../../../validation/7.17/r63-game-smoke/round-2.json), [round 3](../../../validation/7.17/r63-game-smoke/round-3.json), [step reports](../../../validation/7.17/r63-game-smoke/round-3.plan-report.json). Opaque effects and full 7.17 completion remain unresolved.

Historical r64 final-artifact evidence: [round 4](../../../validation/7.17/r64-game-smoke/round-4.json), [round 5](../../../validation/7.17/r64-game-smoke/round-5.json), [obligation export](../../../validation/7.17/r64-game-smoke/round-5.obligations.json). Full 7.17 remains incomplete.

Historical r65 final-artifact evidence: [round 1](../../../validation/7.17/r65-game-smoke/round-1.json), [round 2](../../../validation/7.17/r65-game-smoke/round-2.json), [checkpoint proof](../../../validation/7.17/r65-game-smoke/round-2.checkpoint.json). Full 7.17 remains incomplete.

Current r66 final-artifact evidence: [round 1](../../../validation/7.17/r66-game-smoke/round-1.json), [round 2](../../../validation/7.17/r66-game-smoke/round-2.json). Network protocol 10 requires matching clients/server; full 7.17 remains incomplete.
