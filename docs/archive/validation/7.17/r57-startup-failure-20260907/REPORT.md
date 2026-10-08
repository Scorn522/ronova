# r57 旧世界启动失败 — r58 修复记录

> 历史归档（2026-09-08 整理）：保留当时的设计、实现和验证口径，不代表当前源码状态。文中的“当前”“下一步”属于记录当时。
> 当前入口：[进度](../../../../CURRENT-PROGRESS.md) · [有效规划](../../../../plans/README.md)。

日期：2026-09-07。原始错误包及世界路径、归档输入哈希见 [manifest.json](../../../../../validation/7.17/r57-startup-failure-20260907/manifest.json)。归档输入保持原字节。

## 原因

`LocalDominionStore` 启动时，已 cutover 的 canonical Root 被旧 SavedData 镜像的完整快照比较否决，抛出 `STATE_CONFLICT: shadow state differs from authoritative snapshot`。两份数据的 Realm、epoch、Owner、墓碑和 lifecycle 事实一致；组装快照时的 `persisted-*` / `shadow-persisted-*` 派生 reason 不同，引发假冲突。停服清理再次调用创建型 runtime，重复触发原错误。

单独读取并校验 Root 不能复现两条启动路径之间的比较问题；r58 测试改用与生产相同的完整文件启动入口。

## 修复与证据

- 有效 Root 优先恢复；镜像仅作事实诊断与重建，覆盖前保存原字节。首次旧 Ledger cutover 和真实 Root 损坏/缺失拒绝规则保留。
- 退出仅查询已有运行时；首次初始化异常保留，失败与退出均回收资源，不重新创建。
- 原始三件数据副本通过完整 LocalDominionStore 的启动、保存和再次加载，Root 字节不变；新增镜像异常、Root 损坏和初始化失败用例通过。
- 最终 r58 JAR 的实际 Forge 47.4.23 [round 2](../../../../../validation/7.17/r58-game-smoke/round-2.json) 与 [round 3](../../../../../validation/7.17/r58-game-smoke/round-3.json) 均在同一世界副本进入、保存、正常退出，退出码 0；无同类 Root/退出崩溃。
- 两轮 epoch 1:2、commit index 2、Owner/Tombstone/claim 各 1、effect 0。副本的 canonical Root SHA-256 保持 `2ecc3ed3369e50e85257fa195af807d0d86aca081d4883a164a679e9126d96f1`。
- 原世界 31 文件哈希不变，原 r57 成品保留。其他模组缺失物品 `generated_mod:creation_myth_disc` 的函数错误仍存在，与本次 fatal Root 异常分别记录。

最终成品 SHA-256：`ce24012fb99eed5d3fab4e2b6ea9fb98e7377a2fc1af6658e14e3ce41163cef4`。
详见 [r58 交接](../../../handoffs/7.17-ROOT-BOOTSTRAP-HOTFIX-HANDOFF.md) 与 [完整构建](../../../../../build-r58.log)。该热修复已验证；完整 7.17 验收仍待推进。
