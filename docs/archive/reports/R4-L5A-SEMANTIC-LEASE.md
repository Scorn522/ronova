# R4 / L5A Semantic Enforcement Lease

> 历史归档（2026-09-08 整理）：保留当时的设计、实现和验证口径，不代表当前源码状态。文中的“当前”“下一步”属于记录当时。
> 当前入口：[进度](../../CURRENT-PROGRESS.md) · [有效规划](../../plans/README.md)。

> 历史设计/验证记录：对应 alpha.7.13 R4/L5A 阶段，不是当前 r50 canonical 规范。当前阶段与路线以 `docs/plans/README.md` 和 `docs/CURRENT-PROGRESS.md` 为准。

Version: `0.2.0-alpha.7.13`  
Network protocol: `8` (unchanged)  
Frozen live input baseline: `0.2.0-alpha.7.12`

## Purpose

Alpha.7.12 could ask JVMTI to retransform critical classes, but the Native
integrity layer did not prove that the resulting bytecode still contained the
Ronova lifecycle and presence gates. R4/L5A turns that blind retry into a
renewable semantic lease.

This is an actual enforcement-strength increase, not another component split.
It is intentionally limited to one independent integrity domain and does not
change Judgment scope, spawn lineage, player capabilities, Root schema or the
network protocol.

## Native proof boundary

The integrity DLL independently recognizes five loaded core targets:

| Target | Required semantic evidence |
| --- | --- |
| `Entity` | removal and revive gates |
| `LivingEntity` | damage, death and health gates |
| `Level` | quarantined query-result or entity filter |
| `ServerLevel` | quarantined query-result or entity filter |
| `ClientLevel` | quarantined query-result or entity filter |

Each watchdog renewal performs a bounded retransformation of the loaded,
modifiable core targets only. The cycle starts with empty success and failure
masks. A target is verified only when its transformed class bytes contain the
expected `BootstrapHooks` owner and semantic markers. A failure for any copy of
a target is latched for the whole cycle, so a second ClassLoader cannot hide it
by later producing one successful copy.

The Native proof state contains:

- dynamically loaded required and verified masks;
- monotonic lease generation;
- successful, failed and consecutive-failure counts;
- semantic transformation and marker-failure counts;
- renewal count and last-success age;
- watchdog-armed and lease-current flags.

The capability bit `0x40` means this Native binary supports semantic leases. It
does not by itself mean a lease is current.

## Java boundary

```text
Native ClassFileLoadHook / watchdog
  -> Native semantic proof state
  -> NativeIntegrityBridge (one JNI status call)
  -> EnforcementRuntime (one immutable poll per 20 server ticks)
  -> /ronova status
```

`AuthorityGuard`, `JudgmentExecutor`, `SpawnFirewall` and
`SpawnAdmissionRuntime` do not call the integrity JNI adapter. Existing
gameplay enforcement continues during proof degradation; the reported
effective contract degrades instead of disabling the player or pretending
that configured capability equals observed strength.

Contract states are:

- `R4_L5A_SEMANTIC_NATIVE_ROOT` — Root ready and semantic lease current;
- `R4_L5A_DEGRADED_NATIVE_ROOT` — Root ready but lease incomplete/stale;
- `R4_L5A_JAVA_FALLBACK` — Native Root unavailable.

## Deterministic Native build

The Windows x64 integrity DLL is now linked without debug data. This removes
LLD's per-build PDB GUID while retaining all JNI exports and runtime code. Two
independent Zig cross-builds are byte-identical. The Root DLL is unchanged.

## Verification

- 27/27 Java SelfTests pass against the packaged, reobfuscated candidate JAR.
- New tests cover bounded proof polling, immutable publication, read-failure
  isolation, five-class transformer evidence, cold-JNI dependency boundaries,
  Native exports and embedded semantic markers.
- Java-generated `AXR3` fixture passes the pure C++ Root model unchanged.
- Two reobfuscation passes rewrite `47 / 47` mapped references and are
  byte-identical; the idempotence pass rewrites `0`.
- Two deterministic packages are byte-identical.
- External ABI parity against alpha.7.12 passes `642` common method names,
  `672` common argument shapes and `72` common fields.
- Network protocol remains `8`; the five packet registrations and payloads are
  unchanged.
- Integrity DLL SHA-256:
  `66c9a2a55cfb611aee046fcdc25525944ad9adcf1c29da75065136ae67a23b7b`.
- Root DLL SHA-256 remains:
  `f481818e13ef1ba33a46b3dc4b6d4514949084e0014b226b43755f5e5691afe0`.
- Candidate JAR SHA-256:
  `32ff5b64e44e196e73d70ebf5ac9d9558710b6a8a547c57c8cba58e3cc4031f4`.

Alpha.7.13 is L5A, not FULL L5. Windows Forge must still verify that the lease
becomes `ACTIVE`, renews over time without semantic failures, and preserves all
alpha.7.12 gameplay behavior before this version replaces the live baseline.
