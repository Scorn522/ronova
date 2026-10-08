# Ronova 当前进度对照 — Internal Root Route

> 历史归档（2026-09-08 整理）：保留当时的设计、实现和验证口径，不代表当前源码状态。文中的“当前”“下一步”属于记录当时。
> 当前入口：[进度](../../CURRENT-PROGRESS.md) · [有效规划](../../plans/README.md)。

2026-09-08 已按用户要求恢复 7.17 施工。右键 / Shift＋右键松开后触发的改动保留。
下列较早施工记录保留作历史；完整 7.17 尚未宣告完成。

## 当前源码施工（等待 7.17 大阶段集中验证）

- 已接入九项条件的完成状态观察与 GUI 缺项说明；证据中断、资格变化和合同变化会失效重评。
  类血缘改用实际 Class 身份和继承关系；序列化类名不再单独授权拒绝准入。当前网络协议为 14。
- 本轮新增 `WORLD_RECORDS` 步骤，派生计划合同为 `ronova:terminal-server/v3`。
  `TerminalIndexRepair` 按对象引用清理 ID/UUID/tick 索引，分片扫描区段和类缓存，保存原始数字 ID 与扫描位置，
  避开正在遍历的 tick 列表。写前重查 Root 许可和槽位身份；不重新调用死亡/移除回调。
  扫描结束仍不冒充未来重插来源已受控，相关完成条件继续如实保留缺口。
- 客户端参与集合按稳定 effectKey 保留；Root 重规划只刷新要求的投影，不删除已断线的 required 成员。
  增加采样间隔、等待期限、Owner 授权版本检查和轮转观察；新发布未确认时旧 ACK 不再有效。
  自动投影刷新按连接合并并节流；临时持久化未确认不释放原参与集合。
- 按用户要求，本轮仅推进源码，没有运行构建、测试或游戏验证；现有候选 JAR 不包含本轮新增实现。
  下方旧版通过记录只适用于当时的源码，不能当作本轮结果。完整 7.17、FULL L5 仍未完成。

## 最新实现：独立终结状态恢复

- Root 派生计划增加 `TERMINAL_STATE`，计划合同升为 `ronova:terminal-server/v2`。
  原生命周期 effectKey 不变；不因增加恢复步骤重新发放一次性副作用执行资格。
- 死亡和移除各只由计划调用一次，去掉会再次进入伤害/死亡的 `kill()` 和重复移除入口。
  `TerminalStateSink` 独立修复血量、dead、deathTime、removalReason、addedToWorld 和 callback 字段，
  在精确存储位置检查原计划许可，读取写后状态；不重放第三方生命周期或数据更新回调。
- 巡检读取实际字段，保留弱引用以发现后续漂移。已移除标记不再掩盖恢复的血量。
  巡检不直接写入 presence；实体改为未受裁决的新 UUID 后也不能退回旧分支继续物理写入。
- 删除遮蔽自动生成结果的旧静态 refmap，成品只使用当前编译的字段映射。
- 本段相关计划、Mixin 和巡检检查通过；Root 文件/副作用恢复检查在本段过程中通过。
  实际专用服确认连续两次字段漂移均恢复、回调不重放、换 UUID 不越界，
  见 [运行结果](../../../validation/7.17/dedicated-integration/round-7.json) 和
  [运行日志](../../../validation/7.17/dedicated-integration/round-7.log)。
- 这证明登记字段恢复链，不证明所有实体索引均已清理或任意第三方副作用已完成。
  整体操作仍保留 `completionSatisfied=false`，未知副作用记录不被恢复步骤抹掉。

候选成品：[integration-candidate](../../../build/libs/ronova-0.2.0-alpha.7.17-integration-candidate.jar)。
当前接续内容以本节为准；[上次暂停记录](../handoffs/7.17-PAUSE-2026-09-08.md) 保留作历史。

更新时间：2026-09-07。已交付基线：`0.2.0-alpha.7.17-r66`；当前源码正在进行完整 7.17 集成施工。
基线状态：**完整 build、52 项分层测试与两轮实际 Forge 多片/旧包/Owner/resync PASS**。本次 B/D、C/E、F/G 合并交付，完整 7.17 尚未完成。

## 在建集成批次（非最终交付）

- 当前新增施工：Candidate 原因、目标范围汇总与 Root 资格转换解除已接线；首次写入限制激活 Root schema 7 / checkpoint schema 2，未写限制的旧 Root 仍按 5/6 读取和保存。检查点确认丢失时不发布新限制；合法重绑立即使旧范围门控失效，随后持久化解除原因。
- 五页强度 GUI 已接服务端政策快照和 canonical command：54 政策 / 14 动作，管理员权限、expected revision、请求 ID、连接上下文与超时校验。网络协议为 11 / 8 packets。原客户端显示/启动设置保留为功能页入口。真实客户端已验证读取、修改回执和五页截图，路径 `validation/7.17/r66-game-smoke/integrated-controls-dispatch/instance/validation-gui/`。
- `build-7.17-candidate-integration.log` 的完整 build / 55 项测试 PASS；后续语义校验单测与限制汇总 focused 测试 PASS。最新源码还增加了恢复后证据重验检查，需在后续整体验证中复核。
- Native 在建：以 ASM 方法入口/所有返回边及实际 defining loader 校验替代常量池字符串“证明”，修复错误已有钩子导致跳过重装的问题。Java 语义测试 PASS；新 Native DLL 尚待编译与实际 JVM 验证，不能将旧 DLL 的验证结果算给新源码。
- GUI 验证的三次未通过记录保留在 `integrated-controls`、`integrated-controls-fixed`、`integrated-controls-observed`；接入已验证的客户端测试分发入口后，`integrated-controls-dispatch/round-1.json` PASS，原世界未变。该运行验证 GUI 和前置基础，不表示 §4 全部完成。
- r66 的 363 个构建输入已校验并保存到 `validation/7.17/baseline-r66/source-r66.zip`；原成品保留。
- Root 终结计划入口接入显式 SubjectResolver，返回绑定、跨 Realm、冲突、旧记录未绑定或未决结果，保留映射来源和 acknowledged commit revision；不改历史主键、hash 或回执。完整 lineage/迁移 resolver 仍待完成。
- 计划执行使用最多 5 秒、绑定完整 Root 计划和执行线程的许可；步骤前后复验，durable effect entry 后再验；许可检查传入 lifecycle 内部。服务端计划拒绝异线程执行。此项不等于全部 Native/WriteSink 已证明。
- 客户端投影增加 64 MiB owned estimate 预算，统计旧视图、暂存及临时索引；超限保留已提交视图，失败/超时/断开释放暂存。已测试 65,536 条分片边界、重复包、超限后的重同步和释放；不把估算写成整个 JVM 的精确内存。
- focused 测试：执行许可、身份解析、实际 Root 文件回归及投影容量均 PASS。集成构建日志使用 `build-7.17-integration-*.log`，测试成品使用 `integration` 后缀，不递增小版本或宣告完整验收。
- 集成 build 与 53 项 SelfTest PASS（76 tasks）；世界副本两轮实际 Forge PASS，54.08 / 87.06 秒，退出码 0，原世界哈希未变。第二轮恢复 6 条 checkpoint 记录，重复 lifecycle 回调为 0。此证据验证本节新增实现，不能覆盖 §4 全部待办。
- [集成核验记录](../../../validation/7.17/integration-foundation-audit.json)、[构建日志](../../../build-7.17-integration-check.log)、[源码清单](../../../validation/7.17/integration-foundation-source.json)。测试 JAR SHA-256：`1ca5d6461dfabd71fc9f18c43d61ff8d320b6b4d5a5632cf79afa86ea78c627b`。
- §4 全部剩余工程仍在同一交付范围，验收台账不因上述局部用例而升级为整项 PASS。

## 1. 当前基线

| 项目 | 当前值 |
|---|---|
| Minecraft / Forge 构建 / Java | 1.20.1 / 47.4.22 / 17 |
| 实际客户端 | Forge 47.4.23，round 1/2 退出码 0 |
| 网络协议 | 10；旧协议 9 客户端/服务端不兼容 |
| Native DLL/wire | 未改 |
| Root / checkpoint / journal / effect / qualification | 5 或 6 / 1 / 3 / 1 / 1 或 2；持久格式未改 |
| 政策 | schema 3，54 keys / 14 actions |
| SelfTest | 52 PASS = 50 production JAR + 2 development outputs |
| 构建 | 75 tasks，67 executed / 8 up-to-date，1m 11s |
| 验收基线 | V2.6 B01–B23 + X24–X68；历史 X01–X23 等价未证 |

## 2. r66 本批实现

- 每个实际连接有独立 stream/context revision 和投影序列；OwnerState 同时发布 Realm、连接上下文与 Root Owner 授权版本，旧版本或冲突拒绝。
- ProjectionFrame 绑定 Realm/stream/context/revision/base/Root epoch；单片 128 marks，最多 512 片/65536 marks，字符串和数值验证有界。
- 分片收齐才一次提交客户端视图；旧包、冲突片、错误上下文和断档不覆盖已提交状态。缺片/断档触发双端节流 resync。
- APPEND/REMOVE/CLEAR 连续版本与精确作用域保留；Root 不认可的撤销请求改发当前快照，不从传入旧 mark 拼接新真值。
- 客户端排队回调检查当前活跃连接，断开重置镜像；晚到旧 player release 不删新连接登记。
- [r66 交接](../handoffs/7.17-VERSIONED-PROJECTION-HANDOFF.md)说明协议和界限；Root checkpoint/计划/义务等此前实现保留。

## 3. 实际证据

- 最终 52 项测试全部执行：乱序多片原子性、重复/冲突、旧包/clear/remove、Owner revision、断档/超时与 context reset；六包 ABI、统一客户端发布和现有 Root 回归通过。
- [round 1](../../../validation/7.17/r66-game-smoke/round-1.json) / [round 2](../../../validation/7.17/r66-game-smoke/round-2.json) 均 PASS，55.08 / 53.09 秒。每轮 Root 生成 129 个受控投影目标并多片发送，再注入旧/错 Realm/Owner/断档包，客户端拒绝并通过真实 resync 恢复。
- r65 checkpoint、r64 合并与 r63 回调/撤销验证继续通过；原世界基线哈希不变，原实例 JAR 未替换。
- r66 成品 3,843,493 字节 / 543 entries，SHA-256 `fd4e19ffcdb40791221bd574efb83173b439f43e737afc8141e45066ec1b9fa3`。
- [构建日志](../../../build-r66.log)、[成品清单](../../../BUILD-RESULT-r66.txt)、[源码清单](../../../validation/7.17/source-manifest-r66.json)；`build-r66.ps1` 可复跑。r65 及此前基线保留。

## 4. 完整 7.17 剩余工程

B/D：完整 identity/resolver/provenance、observer 证明、持久 Candidate 限制接纳/原因接力/解除、多层 overflow、真正公平执行和可证明恢复器、稳定 completion。

C/E：全资源域实际归属/释放/预算，WriteSink/Native lease/TOCTOU，double baseline/loader/semantic/dispatch/anti-AllReturn 及独立动作链。

F/G：其余表面、五页 GUI、required/optional 客户端参与与完成证据、大投影容量/资源预算、单机/专用服/故障/压力出口。

当前协议证明投影版本与连接隔离，不证明任意像素/第三方 renderer 完成，也不让客户端成为 Root writer。完整 7.17、FULL L5 与后续 L6/L6E 未宣告完成。

## 5. 文档链

[架构索引](../../plans/README.md) → L6 Master V2.4 → Baseline V1.4 → L6E-0 V1.3 →
[加强总纲 V2.6](../../plans/Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md) → P0-A/阶段规格。
台账 68 条：27 PARTIAL / 41 NOT_RUN，无整项 PASS。V2.6 仍为 Design Freeze Candidate。
