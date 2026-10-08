# R3 Native Root Foundation

> 历史归档（2026-09-08 整理）：保留当时的设计、实现和验证口径，不代表当前源码状态。文中的“当前”“下一步”属于记录当时。
> 当前入口：[进度](../../CURRENT-PROGRESS.md) · [有效规划](../../plans/README.md)。

> 历史设计/验证记录：对应 alpha.7.10 R3 阶段，不是当前 r50 canonical 规范。当前阶段与路线以 `docs/plans/README.md` 和 `docs/CURRENT-PROGRESS.md` 为准。

Version: `0.2.0-alpha.7.10`  
Network protocol: `8` (unchanged)

## Delivered boundary

R3 now has one process-local Native source of truth for the immutable Dominion snapshot. Java publishes a bounded `AXR3` payload carrying the snapshot epoch and all four decision domains. Native accepts only a newer epoch or the byte-identical current epoch; it rejects rollback and same-epoch conflicts.

After Native accepts the first snapshot, `LocalDominionCore` switches to a Native-first decision source. Authority, Judgment, Admission and Presence are answered from Native-owned state. An unavailable Native answer falls back to the frozen Java policy and increments `degraded_decisions`, so degradation is visible rather than silent.

`ProjectionCoordinator` remains the Java gameplay adapter, not an independent truth owner. It acknowledges Native epoch + fingerprint and, on a reconciliation edge, rebuilds a missing or drifted Java projection from the exported Native snapshot after validating both epoch and fingerprint.

## Responsibility split

| Unit | Sole responsibility |
|---|---|
| `NativeBridge` | Extract/load the two DLLs and start/stop their watchdogs |
| `NativeSnapshotCodec` | Canonical bounded Java ↔ Native snapshot format |
| `NativeRootBridge` | Private static JNI protocol and primitive packing |
| `NativeDominionRoot` | Process cutover, status and snapshot coordination |
| `NativeDominionDecisionSource` | Native-first decision lane with counted fallback |
| `native_root.cpp` | Pure C++ parsing, monotonic state, decisions and reconciliation state |
| `native_root_jni.cpp` | Thin JNI conversion plus Windows watchdog adapter |

The existing `ronova.dll` stays byte-identical to alpha.7.9 and owns JVMTI integrity only. The new `ronova_root.dll` owns R3 state and decisions only. This prevents either DLL from becoming a mixed-responsibility failure domain.

## Verification completed

- 20 packaged-JAR Java SelfTests.
- Pure C++ Root model compiled with warnings as errors.
- Java-generated `AXR3` snapshot accepted and fingerprinted identically by C++.
- Two independent reobfuscation passes produced byte-identical output.
- Root DLL verified as Windows x64 PE with the exact 10-entry JNI export surface.
- Original integrity DLL hash verified unchanged from alpha.7.9.

Release hashes:

- JAR: `7c72031d0b810c3cb0ac38611603273688fd6a155f6187daf6b5e7d04c62597c`
- Integrity DLL: `828e3796ef33a551fc8ef4cdd457d83309ab4d539ec1dc7e62876cc65f5c66c5`
- Root DLL: `f481818e13ef1ba33a46b3dc4b6d4514949084e0014b226b43755f5e5691afe0`

## Honest open gate

This is the R3 foundation, not FULL L5. Native independently owns snapshot truth, decisions and projection reconciliation, but critical entity lifecycle enforcement is not yet independently attested by the Root DLL. That later gate must be implemented and proven before any FULL L5 claim.

The remaining alpha.7.10 runtime gate is a Windows Forge smoke test. Run `/ronova status` and require:

- `contract=R3_NATIVE_ROOT`
- `root=ACTIVE`
- matching Java and Native epochs
- `projection_current=true`
- `native_decisions` increases during owner checks and actions
- `degraded_decisions=0`

Then repeat bind/give, single judgment, same-lineage respawn rejection, clear, save, exit and world re-entry on a backup world.
