# r54 聚焦验证记录

> 历史归档（2026-09-08 整理）：保留当时的设计、实现和验证口径，不代表当前源码状态。文中的“当前”“下一步”属于记录当时。
> 当前入口：[进度](../../../CURRENT-PROGRESS.md) · [有效规划](../../../plans/README.md)。

2026-09-05；Java 17.0.20。非 Forge 运行报告，非 exact-artifact 报告。

## 独立策略与 NBT/journal

策略类及 StrengthPolicySelfTest 使用 `--release 17 -Xlint:all -Werror` 独立编译，执行得到：

```text
StrengthPolicySelfTest: PASS (54-key versioned preset matrix, safety floors, ceilings, 14 action controls, pending restart, legacy schema, extension seam, TargetEpoch, commit gate)
```

StrengthPolicyNbtCodec、RootCommandJournal、StrengthPolicyPersistenceSelfTest 使用相同严格编译选项，并使用 build-support 中的 NBT stubs，执行得到：

```text
StrengthPolicyPersistenceSelfTest: PASS (schema 3 round-trip, schema 1/2 migration, strict 54+14 rows, historical journal tag/hash preservation, action tamper)
```

## Root 逻辑离线验证

当前 persistence/policy/root-api/contract/NativeSnapshotCodec/LifecycleIntegrationReceipt/LineageIndex 源码编译后，配合 NBT stubs 与 r51 未改依赖执行 DurableRootStoreSelfTest。运行输出：

```text
DurableRootStoreSelfTest: PASS (fresh realm, legal recovery, migration, corruption fence, Root cutover, COMMIT_UNKNOWN replay, atomic batch, strength policy, legacy profile-matrix migration, compact journal/action-domain conflict, checksummed atomic file, 10k migration parity, wiring)
```

注意：此测试的历史输出带 wiring 字样，但该离线 classpath 可读取 r51 runtime/TerminalLifecycle；**此处只记录当前 Root 存储逻辑用例通过，不认可当前生产 wiring 通过**。新增 verifiesOrdinaryActionRestartGate 验证 active 边界、OFF 拒绝新终结、保留旧 receipt 和 live policy 动作 hash drift。

## 尚未通过的门

- wrapper 获取 Gradle 8.8：Network is unreachable，完整 Gradle 任务未运行。
- 使用少量旧 ABI stubs 进行完整游戏层编译会缺 Forge/Minecraft 类与成员；这些 stubs 不用于生成生产 JAR。
- 当前 ProjectionWiringSelfTest 的 committed gate 接线断言、全部 42 项分层任务、当前重混淆 JAR 和实机验证均 OPEN。

复跑方式见根目录 7.17-ATTACK-ACTION-POLICY-HANDOFF.md；不将本记录代替 Gradle 实际输出。
