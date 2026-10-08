# Ronova Runtime Process Boundary — r27 Historical Source Audit

> 历史归档（2026-09-08 整理）：保留当时的设计、实现和验证口径，不代表当前源码状态。文中的“当前”“下一步”属于记录当时。
> 当前入口：[进度](../../CURRENT-PROGRESS.md) · [有效规划](../../plans/README.md)。

Date: 2026-08-29  
Status: **HISTORICAL r27 JAVA-PATH SOURCE-GROUNDED / POLICY-RECONCILED / NATIVE-PE DOCUMENT-ONLY**  

本文件中的 Java 启动路径对应 r27 历史审计对象；所引用的外部 `r27(1).zip`、Native 源码及 PE 导入审计输入未随本工作区公开，因此相关 Native/PE 结论在本轮审计报告中标为 DOCUMENT-ONLY。本文不是当前 r50 exact-artifact 的运行验收记录，当前状态见 `docs/CURRENT-PROGRESS.md`。

Reviewed source archive: `r27(1).zip`  
Archive SHA-256: `5B784BF18AD1D98D2E06C08970048FC190D1D8A2136EB72FB348D819DADDBBC2`

## 1. Scope

This audit distinguishes four separate questions:

1. Is Authority or durable truth hosted by an External Root process?
2. Can Ronova create a bounded helper process for bootstrap/Agent attach?
3. Can Ronova perform a controlled JVM/Minecraft relaunch where the old runtime exits and the new runtime becomes authoritative?
4. Do build, validation, launcher, or ordinary Minecraft client/server processes exist outside the current JVM?

The canonical policy is **not** "zero child processes". Questions 2 and 3 are allowed when they are bounded bootstrap/recovery/relaunch mechanisms. The prohibited boundary is an external process becoming a durable Authority writer, persistent companion combat plane, independent target-monitoring/attack plane, or Root transport owner.

Build and validation tools are out-of-band and do not become production architecture dependencies. A remote Minecraft client or dedicated server is also a normal game deployment boundary, not an External Dominion Root.

## 2. Source-grounded verdict

| Boundary | r27 current source | Canonical target |
|---|---|---|
| External Dominion Root executable/service | Absent | Forbidden |
| Root Named Pipe / authenticated cross-process IPC | Absent | Forbidden as Authority channel |
| JNI/JVMTI DLLs | Loaded into the Minecraft JVM | In-process implementation layer |
| Native watchdogs | Win32 threads in the Minecraft process | In-process only |
| `AutoAttach` helper JVM | **Conditionally present** | Allowed as bounded bootstrap/attach helper |
| Controlled JVM/Minecraft relaunch | Not established by this r27 audit | Allowed if old runtime exits and successor becomes the sole active game runtime |
| Persistent companion Authority process | Absent | Forbidden |

The current source supports this precise statement:

> The authoritative Root path is process-local. r27 contains a conditional short-lived Agent-attach helper, but that helper is bootstrap infrastructure rather than an External Root. It is acceptable only while it carries no durable Authority state, owns no continuing combat/enforcement loop, and exits after attach handoff.

## 3. Exact production startup path

`RonovaDominion` performs:

```text
NativeBridge.load(...)
    |
    +-- NativeBridge.isActive() == true
    |      -> NATIVE_ACTIVE
    |      -> AutoAttach is not called
    |
    +-- NativeBridge.isActive() == false
           -> AutoAttach.ensureAttached(modJar)
                  |
                  +-- agent already installed / MIXIN / development JAR missing
                  |      -> no helper JVM
                  |
                  +-- DUAL_AGENT + regular mod JAR
                         -> ProcessBuilder(java.exe ... AttachHelper ...)
                         -> short-lived helper JVM attaches and loads the Agent
                         -> helper exits after handoff
```

Source evidence:

- `RonovaDominion.java:28-30` conditionally calls `AutoAttach` only when Native is not active.
- `StartupPolicy.java:31-38` defaults invalid or missing configuration to `DUAL_AGENT`.
- `AutoAttach.java:22-39` selects `java.exe`, starts it with `ProcessBuilder`, waits up to 15 seconds, and destroys it on timeout.
- `AttachHelper.java:5-20` explicitly declares a short-lived helper JVM and uses `com.sun.tools.attach.VirtualMachine` to attach/load/detach.
- The supplied runtime log records `native: ACTIVE/0x7f, agent attach: NATIVE_ACTIVE`; that observed run took the no-helper branch. It does not prove the fallback branch can never execute.

## 4. Native source and binary result

The reviewed Native source uses JNI/JVMTI state, locks, atomics, and `CreateThread` for watchdog threads. `CreateThread` creates an in-process thread, not a process.

The two supplied Windows x64 DLLs were also inspected through their PE import tables:

| DLL | SHA-256 | Process/IPC/network result |
|---|---|---|
| `ronova.dll` | `E005F1EF2D4F808B96D10E21B3CB9A03CB8E8BE214CF6FDA38B2A85EAA55E650` | No `CreateProcess*`, `ShellExecute*`, `WinExec`, process injection, Named Pipe, or Socket imports found |
| `ronova_root.dll` | `74AE9E9085D57668E240B055A5A5F6F7E32F5E400E8E8449FFC1F58047256502` | No `CreateProcess*`, `ShellExecute*`, `WinExec`, process injection, Named Pipe, or Socket imports found |

No production Java path other than `AutoAttach`/`AttachHelper` referenced `ProcessBuilder`, `Runtime.exec`, the Attach API, a direct socket API, or a Named Pipe API in the reviewed r27 source.

## 5. Frozen correction: D13 / `PROC-INV-1`

`PROC-INV-1` is redefined as an **Authority/process-role boundary**, not a zero-process-count rule:

> **PROC-INV-1 — Ronova may create bounded helper/restart processes for bootstrap, Agent attach, recovery or relaunch, but authoritative combat state, Judgment/Terminal truth, ownership, durable Root mutation and continuing enforcement ownership must reside in the currently active Minecraft JVM. No helper/sidecar/service may become a durable Authority writer, persistent combat plane, independent target-monitoring/attack plane or External Dominion Root.**

Allowed examples:

- r27 historical `AutoAttach` → short-lived `AttachHelper` JVM → Agent load → helper exit;
- launcher/bootstrap helper that terminates after handoff;
- controlled relaunch into a new Minecraft JVM with launch-time `-javaagent`, where the old JVM exits and the new JVM becomes the only active game runtime;
- build/validation processes that are never invoked as runtime Authority dependencies.

Forbidden examples:

- a long-lived companion process holding Terminal/Judgment/Ownership truth;
- an External Root service receiving canonical Root commands over Named Pipe/local-loopback/shared memory;
- a helper continuing to inspect or modify target Mod/runtime state after bootstrap handoff;
- old and new Minecraft JVMs both remaining active as competing Authority writers after a relaunch.

## 6. Required verification

`PROC-INV-1` passes when all process-launch sites are enumerated and role-bounded against the exact packaged artifact:

- production static scan enumerates every `ProcessBuilder`, `Runtime.exec`, Attach API and equivalent launch path; no hidden/unclassified path is allowed;
- each allowed helper has an explicit purpose (`BOOTSTRAP_ATTACH`, `RECOVERY`, or `RELAUNCH`) and no API for durable Authority mutation;
- process-tree capture proves attach helpers are short-lived and terminate after handoff;
- relaunch testing proves the old JVM exits and the successor Minecraft JVM becomes the sole active runtime;
- durable truth is recovered through the canonical durable store/recovery protocol, not preserved in the helper process;
- Native source/PE audit continues to show no hidden External Root transport, process injection or independent network command plane;
- no helper/service/daemon remains running as a continuing combat/target-monitoring process after the Minecraft runtime is established.

## 7. Terminology that does not imply an External Root

- `service/` in P0-A is a Java package for domain services, not a Windows Service.
- `External-facing Presence` means visibility to query/render/tracking consumers, not an External Root.
- `ResolverProvider SPI` means an in-JVM provider interface by default; an out-of-process provider may only assist bootstrap/diagnostics and may not become an Authority writer.
- `Native Shared Projection` means read-only memory shared between Java and Native inside the same process. It must not become the canonical cross-process Root state.
- Minecraft protocol/channel traffic is ordinary client/server game networking, not Root transport and not a second Authority writer.
