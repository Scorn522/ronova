# Ronova 7.14A validation

> 历史归档（2026-09-08 整理）：保留当时的设计、实现和验证口径，不代表当前源码状态。文中的“当前”“下一步”属于记录当时。
> 当前入口：[进度](../../../CURRENT-PROGRESS.md) · [有效规划](../../../plans/README.md)。

> 历史文档：本文只描述 r12 的 7.14A validation workflow，不是当前 r50 或 7.14D 的验证入口。当前状态见 `docs/CURRENT-PROGRESS.md`。
>
> **冻结状态：** 7.14A 已冻结为 implementation candidate；冻结对象不得混入 14B/14C/14D 或 7.16 生产内容。该冻结不代表最终 `verification-report.json PASS` 或正式发布验收。

7.14A 的实现和验证工具曾用于 r12 冻结基线。该历史候选
`build/libs/ronova-0.2.0-alpha.7.13-r12.jar` 已完成当时的 `clean build`，32 个
已注册 standalone tests 全部通过，候选 SHA-256 为
`8570321AC5C178A5B802B8B423988F931E148B49201840D247ABD878C3B0F3AD`。
迁移前 RAW smoke 和 LEGACY/RAW 性能结果仍是历史证据；本轮 fixture 由
`validationFixtureJar` 输出到 `build/libs/ronova-7.14a-fixture-1.0.0.jar`，完整 scripted
semantic/rollback 矩阵也尚未为当前候选生成最终 verifier PASS。当前阶段对照见
`docs/CURRENT-PROGRESS.md`。

## 1. Build and packaged-JAR gates

From the project root, run:

```powershell
.\gradlew.bat runTopologyGraphSelfTest
.\gradlew.bat runRawObservationSeparationSelfTest
.\gradlew.bat runTerminalWatchSetSelfTest
.\gradlew.bat clean check validationFixtureJar
```

This historical workflow runs the 32 registered standalone tests through `check`, builds the
r12-era production candidate, and builds/reobfuscates the separately packaged fixture.
Expected artifacts:

- `build/libs/ronova-0.2.0-alpha.7.13-r12.jar`（r12 冻结候选；省略 `-CandidateJar` 时脚本会选择 `build/libs/` 中最新的 `r*` 候选）
- `build/libs/ronova-7.14a-fixture-1.0.0.jar`（由 `validationFixtureJar` 生成）

r12 冻结候选 SHA-256：

- candidate：`8570321AC5C178A5B802B8B423988F931E148B49201840D247ABD878C3B0F3AD`；
- fixture：`CF511EE5909B027DE49AAF276BC85527C3A93AE6F35AB1ACDB7F74838672CA05`。

r12 冻结候选故意保留 7.13 语义版本，直到当时的发布门通过；`-r10` 是更早阶段的构建标识，不代表当前 r50，也不代表 7.14A 已发布。fixture 只用于语义验证，绝不能安装到性能运行或普通游戏中。

`run-forge-scenarios.ps1` 是该历史阶段的统一 `Prepare` / `Drive` / `Capture` / `Verify` 入口；目录中的 `Prepare-*.ps1` 是早期现场辅助脚本，包含历史环境路径和历史哈希，不属于当前 r50，也不能与当前候选混用。

## 2. Prepare one evidence run

Before preparing evidence, make two staging mod directories:

- semantic: the exact candidate mod set plus the validation fixture;
- performance: the same set with only the fixture removed.

Also locate the exact Java executable used by the launcher and the Forge
47.4.22 runtime artifact installed by that launcher. Then create two
byte-identical copies of a fresh validation save while Minecraft is stopped.
Determine the owner's
name/UUID and choose a fixed probe UUID, then run:

```powershell
.\validation\7.14a\run-forge-scenarios.ps1 `
  -Action Prepare `
  -CandidateJar .\build\libs\ronova-0.2.0-alpha.7.13-r12.jar `
  -FixtureJar .\build\validation-fixture\ronova-7.14a-fixture-1.0.0.jar `
  -JavaExecutable <launcher-java.exe> `
  -ForgeArtifact <forge-47.4.22-runtime-jar> `
  -SemanticModsDirectory <semantic-mods-directory> `
  -PerformanceModsDirectory <performance-mods-directory> `
  -OwnerName <player-name> `
  -OwnerUuid <owner-uuid> `
  -ProbeUuid <fixed-probe-uuid> `
  -Seed <world-seed> `
  -Difficulty normal `
  -JvmArguments @('-Xmx8G','<every-other-launcher-jvm-argument>') `
  -LegacySave <legacy-save-copy> `
  -RawSave <raw-save-copy>
```

`Prepare` refuses non-identical save copies, hashes Java/Forge/every mod JAR,
verifies the two mod sets differ only by the fixture, verifies that the fixture
is absent from the production JAR, pins the frozen baseline, and creates the
complete scenario tree under `validation/7.14a/evidence/<run-id>/`.

Each scenario/mode directory contains:

- `commands.txt`: exact console transcript and mandatory tick waits;
- `run.properties.txt`: required startup flag and fixture policy;
- `scenario-manifest.json`: pinned inputs and hashes;
- `manual-assertions.json`: observations that cannot be established from logs.

Use `-Dronova.714a.rawObservation.enabled=false` for legacy modes and `true` for
raw modes. Every mode also requires `-Dronova.baseline.enabled=true`, a 60-second
warmup, and a 300-second measurement window; the exact arguments are written in
its `run.properties.txt`. Install both candidate and fixture for semantic
scenarios. Install only the candidate for `performance/legacy` and
`performance/raw`.

For a dedicated server with RCON enabled, the runner can execute the prepared
transcript and all tick waits itself:

```powershell
.\validation\7.14a\run-forge-scenarios.ps1 `
  -Action Drive `
  -RunId <run-id> `
  -Scenario raw-terminal-visibility `
  -Mode raw `
  -RconHost 127.0.0.1 `
  -RconPort 25575 `
  -RconPassword <password>
```

It writes `console-transcript.txt` without recording the password. Integrated
client cases (especially the required one-JVM World A to World B scenario) can
execute the same `commands.txt` in-game; `latest.log` remains the authoritative
machine-readable output captured below.

## 3. Capture each completed run

Immediately after a scenario's baseline window completes, capture it before
starting another instance:

```powershell
.\validation\7.14a\run-forge-scenarios.ps1 `
  -Action Capture `
  -RunId <run-id> `
  -Scenario raw-terminal-visibility `
  -Mode raw `
  -GameDirectory <minecraft-instance-directory>
```

`Capture` first verifies the active instance's `mods` directory against the
prepared semantic/performance fingerprint. It then copies `latest.log`, the
complete `BASELINE-*` archive, diagnostic lines, and source hashes. It refuses
to replace an earlier capture unless `-ReplaceCapture` is supplied explicitly.

For every capture, inspect the result and change a field in
`manual-assertions.json` to `true` only when it was directly verified. The
multi-server scenario requires World A then World B in one client JVM. The
rollback scenario requires a clean raw-mode stop followed by the same save in
legacy mode.

## 4. Verify all gates

After capturing all modes and completing the manual assertions, run:

```powershell
.\validation\7.14a\run-forge-scenarios.ps1 `
  -Action Verify `
  -RunId <run-id>
```

The verifier exits non-zero on any failure and writes
`verification-report.json`. It checks:

- production/fixture JAR identity and isolation;
- legacy/raw mode and passive-watch selection;
- terminal raw visibility and fixed-probe first/repeat Judgment results;
- generation-2 raw re-observation;
- six-attempt passive-watch diagnostics and exact active-interval retry CSV
  equivalence (leading/trailing all-zero baseline padding is excluded because
  the wall-clock warmup boundary may land one server tick apart);
- two-server isolation and rollback selection;
- zero notification failures and zero dropped tick samples;
- legacy/raw P50/P95/P99 against the frozen +5%/+5%/+10% gates;
- every explicit manual assertion.

Only after `verification-report.json` says `PASS` should `mod_version` and
`docs/archive/reports/CHANGELOG.md` be changed to the 7.14A release candidate.

## 5. Completed paired performance evidence

The manual integrated-client performance pair is archived at
`C:\Users\Scorn\Desktop\MOD\7.14A-test\paired-performance-20260816`.

- LEGACY run: `20260816-074145-245Z`, p50/p95/p99 =
  5148/8159/23506 us.
- RAW run: `20260816-081437-881Z`, p50/p95/p99 =
  4884/7313/18935 us.
- RAW deltas: -5.13%/-10.37%/-19.45%; gate PASS.
- Both runs used the same starting-save fingerprint, source hash,
  `-Xmx7680m`, 6000 tick samples, and zero dropped tick samples.

This closes only the formal performance gate. It does not replace the eight
semantic/rollback captures or the required final `verification-report.json`.
