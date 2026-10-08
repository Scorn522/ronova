# Ronova Build Verification

> 历史归档（2026-09-08 整理）：保留当时的设计、实现和验证口径，不代表当前源码状态。文中的“当前”“下一步”属于记录当时。
> 当前入口：[进度](../../CURRENT-PROGRESS.md) · [有效规划](../../plans/README.md)。

## 当前 r66 / 2026-09-07

- 完整构建 PASS：75 tasks，67 executed / 8 up-to-date，1m 11s；52 项分层测试全部执行（50 production JAR + 2 development outputs）。
- 网络协议 10 的 Realm/stream/context/revision、分片原子发布、Owner 版本、断档/缺片 resync 回归通过；实际 Forge 两轮注入旧/错包并恢复完整视图。
- JAR 3,843,493 字节 / 543 entries；SHA-256 `fd4e19ffcdb40791221bd574efb83173b439f43e737afc8141e45066ec1b9fa3`；Root 格式与 Native DLL 未改。
- [交接](../handoffs/7.17-VERSIONED-PROJECTION-HANDOFF.md)、[日志](../../../build-r66.log)、[成品清单](../../../BUILD-RESULT-r66.txt)。完整客户端参与和 7.17 出口仍未完成。

## 历史 r65 / 2026-09-07

- 完整构建 PASS：74 tasks，67 executed / 7 up-to-date，1m 35s；51 项分层测试全部执行（49 production JAR + 2 development outputs）。
- 同一 Root schema-6 checkpoint、ACK 丢失/重试、冲突、防遗忘、恢复重验和损坏拒绝通过；最终 Forge 两轮验证 6 条旧记录恢复、计数和引用保留。
- JAR 3,828,857 字节 / 538 entries；SHA-256 `2a96d75affec2f724705b57d5895a436bbc44526ec4c601150096e8e3f082fbc`。
- [交接](../handoffs/7.17-OBLIGATION-CHECKPOINT-HANDOFF.md)、[日志](../../../build-r65.log)、[成品清单](../../../BUILD-RESULT-r65.txt)。完整限制接力和 observer 证明仍未完成。

## 历史 r64 / 2026-09-07

- 完整 build PASS：74 tasks，68 executed / 6 up-to-date，2m 17s；51 项分层测试全部执行（49 production JAR + 2 development outputs）。
- 候选门槛、证据独立性、过期/上下文、100000 次合并、主体公平轮转、overflow 和 durable-ref 去重通过。
- 最终 Forge round 4/5 验证 Root 效果恢复、1000 次合并不增 detail、7/9 个主体轮转；独立 fixture 修复玩家尚未加入的计时假设，生产 JAR 不变。
- JAR 3,817,767 字节 / 534 entries；SHA-256 `db9ae7b229a01674bb552b0cf23d5f6321760d3ed504245254cf58d62891446f`。
- [交接](../handoffs/7.17-OBLIGATION-REVIEW-HANDOFF.md)、[日志](../../../build-r64.log)、[清单](../../../BUILD-RESULT-r64.txt)。完整 observer 证明与持久义务/限制接力尚未完成。

## 历史 r63 / 2026-09-07

- 完整 build PASS：73 tasks，65 executed / 8 up-to-date，1m 36s；50 项分层测试全部执行（48 production JAR + 2 development outputs）。
- Root 派生计划、固定顺序、步骤复验、重入抑制、opaque bundle/旧语义互斥、重规划和保存恢复回归通过。
- 最终 Forge round 2/3 各观察到 4 次首次生命周期调用、0 次重复调用；回调中撤销阻断剩余阶段，报告保持 PARTIAL/EFFECT_UNKNOWN 或 FENCED。
- JAR 3,794,604 字节 / 520 entries；SHA-256 `60f08b75d2ae12216b8f2beef0c7a3c11eb53d4680ac190cb78d1cb6fabe3ee3`。
- [交接](../handoffs/7.17-TERMINAL-OPERATION-PLAN-HANDOFF.md)、[日志](../../../build-r63.log)、[成品清单](../../../BUILD-RESULT-r63.txt)。通用恢复器、完成证明和完整 7.17 仍未完成。

## 历史 r62 / 2026-09-07

- 完整 build PASS：72 tasks，66 executed / 6 up-to-date，1m 21s；49 项分层测试全部执行（47 production JAR + 2 development outputs）。
- 原子多域配额、恢复余量、host/owned 分离、observer 失败/替换和队列释放回归通过；实际 Forge 两轮验证生产队列限流、Root 继续提交、释放恢复和诊断命令。
- 实际报告四类 S1 / 四类 S0；15 个资源域中只有已接线队列的 heap 用量为 PARTIAL_ESTIMATE，其余 owned 域仍为 UNOBSERVED。
- JAR 3,770,626 字节 / 509 entries，SHA-256 `72848dd398bde4936a667de6195a57358cd95bb2f15134c7bcb9283d8d7b9714`。
- [交接与限制](../handoffs/7.17-RUNTIME-INVENTORY-HANDOFF.md)、[构建日志](../../../build-r62.log)、[成品清单](../../../BUILD-RESULT-r62.txt)。完整 7.17 未完成。

## 历史 r61 / 2026-09-07

- 最终 `build validationFixtureJar` PASS：70 tasks，63 executed / 7 up-to-date，1m 4s；47 项分层测试全部执行（45 production JAR + 2 development outputs）。
- 新增 acknowledged authority/Owner ABA、单次排队任务、连接上下文、缓存投影与 raw HELD/rebind 接线验证；实际文件 ACK 丢失与原 journal 恢复通过。
- 最终 JAR 在实际 Forge round 4/5 完成真实 channel 重复/错 Realm 请求与保存重开；受控实体重绑定后均推进 20 ticks，原世界不变。
- 成品 3,730,810 字节 / 484 entries；SHA-256 `4fe35a4d96eb6c3ff57f20e2f4789e3eeea59f1f8c63183e0c94c12399da6e9c`。
- [交接](../handoffs/7.17-DEFERRED-AUTHORITY-HANDOFF.md)保留最初静态 refmap 和暂停调度的失败证据；[日志](../../../build-r61.log)、[清单](../../../BUILD-RESULT-r61.txt)。完整 7.17 尚未完成。

## 历史 r60 / 2026-09-07

- 最终 `build validationFixtureJar` PASS：69 tasks，63 executed / 6 up-to-date，52 秒；46 项分层 SelfTest 全部通过。
- 新增物理绑定、独立版本向量、域所有权/revoke、guarded rebind/reactivate、批量拒绝、ACK 恢复与 effect 身份保留验证。
- 实机发现并修复非玩家 IDENTITY 被扩大为 GLOBAL_LINEAGE，以及 scope 被误用为 player 元数据的问题；失败候选记录保留。
- 最终成品在 `r60-game-smoke/metadata` round 5/6 完成实际原版实体生产裁决、所有权更新和保存重开；每轮退出码 0，原世界不变。
- JAR 3,718,786 字节 / 478 entries；SHA-256 `1cad1d3df4b3cfff7f71e4f8e1cb47307236b01a00131ad6127f8c0f390cc586`。
- [交接与范围限制](../handoffs/7.17-SUBJECT-BINDING-HANDOFF.md)、[构建日志](../../../build-r60.log)、[成品清单](../../../BUILD-RESULT-r60.txt)。完整 7.17 未完成。

## 历史 r59 / 2026-09-07

- `build validationFixtureJar` PASS：69 tasks，61 executed / 8 up-to-date，53 秒；46 项分层测试全部执行并通过。
- 新增 whole-batch 预期资格、Realm/政策/单主体冲突、ACK 丢失、原命令恢复与字段篡改验证；历史数据与副作用回归继续通过。
- 最终成品在实际 Forge 47.4.23 的世界副本连续两轮提交受控资格命令、拒绝过期请求、保存退出；第二轮恢复原命令通过。原世界本轮 33 文件哈希不变。
- JAR 3,684,571 字节 / 465 entries；SHA-256 `fe7bc56cb9fd95befeda0420b8cfd21132a8df6b38758f7866e8e0725a12bd2b`。
- [交接与剩余项](../handoffs/7.17-QUALIFICATION-PRECONDITIONS-HANDOFF.md)、[构建日志](../../../build-r59.log)、[成品清单](../../../BUILD-RESULT-r59.txt)。完整 Binding/Ownership 与全 7.17 出口未完成。

## 历史 r58 / 2026-09-07

- JDK 17.0.15 / Gradle 8.8 / Forge 47.4.22 完整 build PASS：69 tasks，61 executed / 8 up-to-date。
- 46 项分层 SelfTest PASS：44 production JAR + 2 development outputs。
- 实际事故 Root/Ledger/shadow 的完整启动与两次保存重开通过；坏镜像、真实 Root 损坏/缺失、失败构造及退出清理均覆盖。
- 最终 JAR `3,679,752` 字节 / 464 entries，SHA-256 `ce24012fb99eed5d3fab4e2b6ea9fb98e7377a2fc1af6658e14e3ce41163cef4`；无测试/fixture 类、基础桩或重复项，DLL parity PASS。
- Forge 47.4.23 实际客户端 round 2、3 均 PASS，使用同一最终 JAR；原世界和 canonical Root 字节不变。范围为副本启动/保存/退出/重开，不代表全部 7.17 能力验收。
- 证据：`build-r58-focused.log`、`build-r58.log`、`BUILD-RESULT-r58.txt`、`validation/7.17/r58-game-smoke`。
- [r58 热修复交接](../handoffs/7.17-ROOT-BOOTSTRAP-HOTFIX-HANDOFF.md)记录 r57 漏掉的组合启动验证及本次补齐。

## 历史 r57 / 2026-09-06

- JDK 17.0.15 / Gradle 8.8 / Forge 47.4.22 完整 build 成功，68 tasks（61 executed / 7 up-to-date），1m 20s。
- 45 项分层 SelfTest 全通过：43 production JAR + 2 development outputs。新增 EffectContract、StorageCapacity；实际文件/6 个子 JVM halt/延迟 callback 归入 DurableRootStore。
- `build/libs/ronova-0.2.0-alpha.7.17-r57.jar`：3,668,789 字节，461 条目，无重复、测试类或基础桩，两个 DLL 与原资源一致。
- SHA-256：`9f3b2d950a74fbc8f806b435075a5999a1a39a6ea0bdedaa350666cd59f87d5a`。
- Root schema 5 / journal 3 / effect 1 / qualification 1；policy 3 / protocol 9。`build-r57.log` 是最终完整成品证据，`build-r57-focused.log` 为此前聚焦回归。
- `inspectDurableRoot` 对两份用户 Root 副本只读验证通过，原 schema 3/4；没有修改原世界。日志见 `validation/7.17/runtime-r56-user-20260906/r57-readonly-root-probe.log`。
- r56 基础实机观察已有证据，r57 游戏内复测仍 UNVERIFIED；6 个受控进程中止不等于真实 Minecraft 第三方掉落/Native/GPU 崩溃全验收。
- [r57 Handoff](../handoffs/7.17-EFFECT-JOURNAL-HANDOFF.md)记录实现范围和剩余项；复跑入口 `build-r57.ps1`。

## 历史 r56 / 2026-09-06

- JDK 17.0.15 / Gradle 8.8 / Forge 47.4.22 完整 build 成功，66 tasks（60 executed / 6 up-to-date）。
- 43 项分层 SelfTest 全通过：41 production JAR + 2 development outputs。
- 覆盖真实文件 ACK 失败时的读取/发布隔离、多目标资格提交、clear/re-terminalization、原 receipt 恢复、旧 schema 迁移及跨 Realm/篡改拒绝。
- `build/libs/ronova-0.2.0-alpha.7.17-r56.jar`：3,641,496 字节，449 条目，无重复，两个 DLL 与资源一致，基础桩及 SelfTest 类排除检查通过。
- SHA-256：`0b5787e2d880e3a32b8b5febfc96ec621b4e5be4fa94d4ddacc3d6eef8024d02`。
- 证据：`build-r56-focused.log`、`build-r56.log`、`BUILD-RESULT-r56.txt`；复跑入口 `build-r56.ps1`。
- Root schema 4 / journal 3 / qualification extension 1，policy schema 3 / protocol 9。完整 scope、迁移及未验收项见 [r56 Handoff](../handoffs/7.17-SUBJECT-QUALIFICATION-HANDOFF.md)。
- Forge/Native/GPU/多人/真实进程崩溃仍 UNVERIFIED；43 项测试不能作为 68 条 B/X 整体 PASS。

## 历史 r55 / 2026-09-06

- JDK 17.0.15 / Gradle 8.8 / Forge 47.4.22 完整 `build` 成功，65 tasks（57 executed / 8 up-to-date）。
- 42 项分层 SelfTest 全部执行通过：40 production JAR + 2 development outputs；不是 42 项成品类验证。
- 新增 lifecycle intent 的真实文件写入前/原子替换后 ACK 失败、重启、未决 Judgment 与并发领取回归；归入已有 DurableRootStoreSelfTest。
- 成品 `build/libs/ronova-0.2.0-alpha.7.17-r55.jar`：3,625,396 字节，444 ZIP entries，无重复项，ZIP 校验通过；两个内嵌 DLL 与源码资源一致，基础编译桩包扫描通过。
- SHA-256：`3c28ccf354c14db4acd38c6388b85e06435f4bb22ac5cf75ae43e74b48367dd1`。
- 原始证据：`build-r55-focused.log`、`build-r55.log`、`BUILD-RESULT-r55.txt`。Windows Forge/Native/GPU/多人/真实进程终止仍 UNVERIFIED。
- 此前本机 r54 的 build 和 42 项分层测试也已成功，证据保留于 `build-r54.log`、`BUILD-RESULT-r54.txt`。下面的受阻说明属于 2026-09-05 历史环境。

## 历史 r54 / 2026-09-05

- 版本 `0.2.0-alpha.7.17-r54`，policy schema 3，14 actions + 54 keys，注册 42 项测试（40 production JAR + 2 development outputs）。
- Java 17 独立 `StrengthPolicySelfTest` PASS；NBT stub 下 `StrengthPolicyPersistenceSelfTest` PASS。
- 当前 persistence/policy/root-api/contract/codec/receipt/LineageIndex 源码的离线 Root 回归 PASS；其中使用 NBT stubs 与 r51 未改依赖，不能把结构扫描输出算作当前 runtime wiring 或成品 PASS。
- 已新增动作重启门、历史 receipt 保留、动作哈希篡改和 failed/COMMIT_UNKNOWN 零物理回调回归。当前生产 JAR 上的 ProjectionWiringSelfTest 新断言待运行。
- 完整 ForgeGradle：BLOCKED，wrapper 下载 Gradle 8.8 时 Network is unreachable；现有最小 ABI stubs 不足以编译完整游戏层。未产生或宣称新生产 JAR。
- 本批以 [r54 Handoff](../handoffs/7.17-ATTACK-ACTION-POLICY-HANDOFF.md) 和 [当前进度](../../CURRENT-PROGRESS.md) 为准。下方全部是历史记录，尤其 document-only 声明只属于此前 MD 批次。

> 2026-09-05 V2.6 文档修订：仅进行 MD 引用、合同编号、配置键对照、文档链及源码未变检查；没有执行新的 Gradle/Forge/GPU 测试。B01–B23、X57–X68 是待实施/验收要求，不计入当前 42 个注册 SelfTest，也不改变下列历史通过记录。详情见 `docs/archive/reports/DOC-REVISION-V2.6.md`。

> 本文件保留各历史版本的构建记录；当前 working source 与阶段状态以 [Internal Root canonical 文档链](../../plans/README.md)、[当前进度](../../CURRENT-PROGRESS.md) 和 [r54 实施交接](../handoffs/7.17-ATTACK-ACTION-POLICY-HANDOFF.md) 为准。

## 历史 r53 7.17 ProfilePresetMatrix source-level implementation candidate（2026-09-05 信息同步）

- 当前源码版本：`0.2.0-alpha.7.17-r53`；Minecraft `1.20.1`；Forge `47.4.22`；协议 `9`。
- `build.gradle` 注册 42 个 standalone SelfTest：40 个预期针对重混淆生产 JAR，2 个（`DurableRootStoreSelfTest`、`StrengthPolicyPersistenceSelfTest`）针对开发主输出。
- `StrengthPolicySelfTest` 已通过，覆盖 54-key versioned matrix、12/42 兼容档分布、强化/极限逐项表、Custom 固化、安全下限、supported/evidence ceiling、pending restart、未知 revision 与 TargetEpoch/extension 既有合同。
- `StrengthPolicyPersistenceSelfTest` 已通过，覆盖 schema 2 往返、schema 1 迁移、严格 54 行、重复 key/未知 revision 拒绝，以及旧 journal tag/hash 的当前编码重写。
- policy、Root command journal 与 NBT codec 相关生产源码已使用 Java 17、`-Xlint:all -Werror` 和最小 compile-only NBT ABI stub 严格编译通过。
- 用户提供的 Windows/JDK 17/Gradle 8.8 构建已完成生产源码编译、Mixin/refmap 处理、`jar`、`reobfJar`、validation fixture 重混淆和测试源码编译；`DurableRootStoreSelfTest` 的完整迁移/恢复用例通过。
- 同一构建在前 19 项 SelfTest 通过后停在 `InheritedMappedMemberSelfTest`：重混淆成品中检测到 11 个均已映射的 `SavedData.setDirty` 调用，而历史断言仍写死为 8。由于该测试会先拒绝任何残留开发名，失败证明是合法持久化调用清单扩展后测试期望过时，不是生产调用未重混淆。
- fix1 将该门改成逐方法精确核对 11 个调用，并把测试中的两个待删除 `ResourceLocation` 构造器替换为官方工厂；第二次 Windows 构建进一步给出了成品方法清单，确认批量入口名为 `recordBatch`、单项入口名为 `record`，而非两个都叫 `record`。
- fix2 按第二次成品回执冻结准确清单：构造首存、Realm 迁移、owner bind/unbind、`recordBatch`、`record`、command append、durable mirror、lifecycle claim、全量/单项 clear，共 11 个调用、11 个方法名；生产代码仍未改变。
- 2026-09-05 信息同步：上传包已包含批处理 prepare/applyCommitted、DurableTerminalWatchSet 与禁止旧终态 forget 调用的测试修正。用户反馈似乎可生成 JAR；完整成功日志和最终生产 JAR 尚未提供，42 项分层验证及成品审计仍待证据。本次仅同步版本与文档，不修改生产 Java、现有测试或构建脚本。
- 已有 fix2 日志中的 InheritedMappedMemberSelfTest 已 PASS，随后停在旧 ProjectionWiringSelfTest；最新源码已更新该调用边，但本包没有最新完整成功日志。
- 当前构建脚本保留上传版本：`options.release = 17`，没有显式 Java toolchain 声明。构建使用 JDK 17；本次未恢复工具链声明，以免改变用户当前可用环境。
- 开发输出测试为 DurableRootStoreSelfTest 与 StrengthPolicyPersistenceSelfTest；42 项全部通过也不得表述为“42 项成品 JAR 验证”。
- r52 与 r51 的历史源码/JAR 证据都不能冒充 r53 证明；Windows Forge、真实旧世界迁移、Native/JVMTI/Agent、GPU、process-tree 与独立发布报告仍为 OPEN。

## 历史 r52 7.16 source-level implementation candidate（2026-09-01）

- 当时源码版本：`0.2.0-alpha.7.16-r52`；Minecraft `1.20.1`；Forge `47.4.22`；协议 `9`。
- `build.gradle` 注册 41 个 standalone SelfTest：40 个预期针对重混淆生产 JAR，1 个 `DurableRootStoreSelfTest` 针对开发主输出。
- 聚焦验证已通过：`DurableRootStoreSelfTest`、`RootCommandBusSelfTest`、`SessionJournalSelfTest`、`ClientActionControllerSelfTest`、`TerminalWatchSetSelfTest`、`StrengthPolicySelfTest`。
- Root、网络入口与终态接管相关生产源码离线严格编译通过；命令/状态 overlay 另行严格编译通过；均使用 Java 17、`-Xlint:all -Werror` 与最小 compile-only ABI stubs。
- 已验证源码范围：Durable Root sole-writer cutover、64 MiB 有界/校验/强制落盘/原子替换的世界本地权威文件、Snapshot 单份存储的紧凑 command journal 与旧格式迁移、丢弃内存后的 lost-ACK recovery、稳定 ingress command id、动作 command-domain 冲突、协议 9 Realm handshake、同 Realm/玩家上下文 outbox 恢复与上下文改变清空、目标发现前 ID-only receipt recovery、durable empty receipt、未决命令不 ACK、文件损坏与替换失败 fence、原子 JudgmentBatch、两侧 `COMMIT_UNKNOWN`、durable TerminalWatch、Admission incarnation、Root 已提交后的 once-only lifecycle completion、TargetEpoch/WriteSink/Completion extension seam、54-key StrengthPolicy、策略迁移/重启激活与 policy hash tamper fence。
- **未生成 r52 JAR**；本环境没有可用 ForgeGradle distribution/dependency cache，因此没有 `clean build`、reobf、41/41 exact-artifact、ZIP/Mixin/DLL parity 或 SHA-256 结果。
- r51 JAR SHA-256 `12624f81392363d757208cbfe873593d152fbbc58b03d1eb2d8e4dac04a09801` 只作为 frozen input baseline，不是 r52 产物证明。
- Windows Forge、GPU、Native/JVMTI/Agent、process-tree 与独立发布报告全部仍为 OPEN。

## 当前 r50 7.15 DurableRootStore Shadow implementation candidate（2026-08-30）

- 当前源码版本：`0.2.0-alpha.7.15-r50`；Minecraft `1.20.1`；Forge `47.4.22`；协议 `8`。
- 当前工作区产物：`build/libs/ronova-0.2.0-alpha.7.15-r50.jar`，大小 `3,460,001` 字节。
- 当前工作区产物 SHA-256：`0e0a82422cb56cc622d18a1ddb0a4864efb861f8a567d0c296cb30e67560879d`。
- `build.gradle` 注册 40 个 standalone SelfTest：39 个针对重混淆生产 JAR，1 个 `DurableRootStoreSelfTest` 针对开发主输出；`mc_check` 与 `mc_build` 均通过，Gradle 完成 63 个任务，40/40 分层 SelfTest 全部通过。
- 已验证范围包含 7.14B Authority Split、7.14C Presence/OwnerImmunity Split、7.14D budget/telemetry、全新世界 `RealmId` 首存、7.15 RealmId-bound shadow、lifecycle receipt single-writer、同 epoch 回执 anti-rollback、migration、failpoint recovery、有限 Admission retry + dead-letter 与 RAW/LEGACY observation 边界。
- `TopologyGraphSelfTest` 覆盖 300 条 stale 引用跨 tick 清理，以及 200,003 条 live reference、仅 3 条 stale 的稀疏负载；字节码门禁拒绝维护链通过 `Map.keySet/values/entrySet/forEach` 全表遍历。
- `BoundedRetryQueueSelfTest` 覆盖默认 8 次/1,024 条策略、构造器/工厂双回调在入队前经 `WeakIdentitySet.addBounded` 去重、快照轮转、预算耗尽 carry、并发 drain、永久失败 100 tick 后活动 backlog 清空及 dead-letter 有界保留；入队热路径每次最多回收 16 条旧弱引用。
- JAR 含 368 个 ZIP 条目、0 个重复项、19 个 Mixin；`ronova.dll` 与 `ronova_root.dll` 均与源码资源逐字节一致。
- 本地构建不等于 Windows Forge、GPU、process-tree 或独立 `verification-report.json PASS`；这些仍是发布级验收边界。

## 历史 r27 working source（2026-08-29）

- 当时源码版本：`0.2.0-alpha.7.14b-r27`；Minecraft `1.20.1`；Forge `47.4.22`；协议 `8`。
- 当时工作区产物：`build/libs/ronova-0.2.0-alpha.7.14b-r27.jar`。
- 当时工作区产物 SHA-256：`5260390a4d6e14e0731cc34f347bdcf71983abf80e9ba996d1489e49ea582e52`。
- `build.gradle` 当时注册 34 个 standalone SelfTest；该记录不等同于新的 Windows Forge、GPU、process-tree 或 `verification-report.json PASS`。
- 当时 7.14B Authority Split 与 7.14C Presence/OwnerImmunity Split 已有源码落点并接入；7.14D budget/telemetry 尚未实现。

## 冻结历史 7.14A 候选（r12，2026-08-28）

- 该版本公共身份为 `ronova`：Mod ID、Java 根包、资源命名空间、Mixin/refmap、配置键、命令、网络通道、Agent 路径、JNI 类路径、PE 导出名和归档命名已经统一。
- r12 冻结候选文件为 `build/libs/ronova-0.2.0-alpha.7.13-r12.jar`，SHA-256 为 `8570321AC5C178A5B802B8B423988F931E148B49201840D247ABD878C3B0F3AD`。
- 剑模型使用内置实体物品模型、基础像素画面和遮罩驱动的全亮 Shader；物品装饰器和单次 Tooltip 动画帧已经接入。
- Tooltip 眼睛图集为五个严格的 `512x256` 单元，整体为 `2560x256` RGBA；Shader 使用半纹素内缩采样，避免帧间串色。
- 本轮 `clean build` 成功，Gradle 8.12.1 / ForgeGradle 构建完成，32 个已注册 standalone Java tests 全部通过；生产包包含模型、贴图、字体、Shader、Mixin/refmap 和 Windows x64 DLL 资源。
- 该 r12 冻结候选继续保留 7.13 语义版本；`-r10` 只是更早阶段的构建标识。独立 validation fixture 由 `validationFixtureJar` 输出到 `build/libs/ronova-7.14a-fixture-1.0.0.jar`，历史 SHA-256 为 `CF511EE5909B027DE49AAF276BC85527C3A93AE6F35AB1ACDB7F74838672CA05`。
- 该 r12 exact-artifact 的 Windows Forge、GPU 和 scripted semantic/rollback 验证仍未闭环；下方旧阶段的 JAR 哈希只描述各自阶段，不代表当前 r50。详细对照见 `docs/CURRENT-PROGRESS.md`。

## Alpha.7.13 R4 / L5A Semantic Enforcement Lease

- The Windows Forge frozen alpha.7.12 behavior remains the input baseline while the JVMTI integrity DLL gains independent semantic evidence and renewable leases for five loaded core classes.
- Native verification requires removal/revive, damage/death/health and quarantined-query markers in transformed class bytes. Success and failure masks are reset per cycle; any failed duplicate ClassLoader copy is latched for that renewal.
- Java consumes the Native state through one isolated proof source and a per-server immutable projection polled at most once per 20 ticks. Judgment, Authority and Spawn hot paths contain no semantic-status JNI dependency.
- All 27 Java SelfTests pass against the final packaged, reobfuscated candidate JAR. The pure C++ Root model accepts the Java-generated `AXR3` fixture unchanged.
- Two R4 reobfuscation passes each rewrite 47 mapped references and produce byte-identical output; the idempotence pass rewrites zero. Two packages are byte-identical.
- Two stripped Windows x64 Native integrity builds are byte-identical and retain the three loader/watchdog exports plus the new semantic-status export.
- `ExternalAbiParityAudit` passes 642 common method names, 672 common argument shapes and 72 common fields against alpha.7.12.
- ZIP, Java 17, Manifest/mod version, 18-Mixin metadata, protocol-8 five-packet ABI, nested Hook parity, stale-class pruning, Native PE exports and compile-stub exclusion pass.
- Integrity DLL SHA-256: `66c9a2a55cfb611aee046fcdc25525944ad9adcf1c29da75065136ae67a23b7b`; Root DLL remains `f481818e13ef1ba33a46b3dc4b6d4514949084e0014b226b43755f5e5691afe0`.
- Packaged JAR SHA-256: `32ff5b64e44e196e73d70ebf5ac9d9558710b6a8a547c57c8cba58e3cc4031f4`.
- Protocol 8 is unchanged. Alpha.7.13 is L5A, not FULL L5, and still requires Windows Forge lease-renewal and gameplay-equivalence validation.

## Alpha.7.12 R3.2 Component Boundaries

- The Windows Forge accepted alpha.7.11.1 behavior is frozen while Spawn admission, client runtime and network responsibilities move behind stable compatibility facades.
- Spawn admission state is now composed per server; client action/input/recovery state and channel/packet/action/quarantine network responsibilities are independently owned.
- `R32ArchitectureSelfTest` prevents the facades from reacquiring implementation dependencies. `ClientActionControllerSelfTest` freezes ordered delivery, receipt advancement and new-session sequencing.
- All 25 Java SelfTests pass against the final packaged, reobfuscated candidate JAR. The pure C++ Root model also accepts the Java-generated `AXR3` fixture.
- Two independent R3.2 reobfuscation passes each rewrote 266 mapped references and produced byte-identical output; an idempotence pass rewrote zero. Two independent packages are byte-identical.
- `ExternalAbiParityAudit` passes 640 common method names, 669 common argument shapes and 72 common fields against alpha.7.11.1.
- ZIP integrity, Java 17 class version, Manifest/mod version, 18-Mixin metadata, five protocol-8 packet registrations, nested Hook parity, stale nested-class pruning and compile-only class exclusion pass.
- Packaged JAR SHA-256: `2ae9b34d7c0ad58ea8f38aa0b0ec3691bba157a122fcb210a7f5d021dc6ada3b`.
- Integrity DLL SHA-256 remains `828e3796ef33a551fc8ef4cdd457d83309ab4d539ec1dc7e62876cc65f5c66c5`; Root DLL remains `f481818e13ef1ba33a46b3dc4b6d4514949084e0014b226b43755f5e5691afe0`.
- Protocol 8 is unchanged. Alpha.7.12 subsequently passed its Windows Forge equivalence, persistence and gameplay smoke tests and is the frozen live input baseline for alpha.7.13; it is not FULL L5.

## Alpha.7.11.1 R3.1 Hot-Path Projection Repair

- Authority, Admission and Presence routine reads are now local immutable projection decisions; only explicit Judgment retains a Native gameplay decision call.
- No-op Root receipts return without publication and fail closed if their content conflicts with the current same-epoch projection.
- The EntityType factory Mixin observes results but never returns intentional `null`; JOIN/LEVEL/manager/first-tick gates retain silent counted rejection.
- `HotPathProjectionSelfTest` plus expanded wiring and architecture gates make these boundaries executable regressions.
- All 23 Java SelfTests pass against the final packaged, reobfuscated candidate JAR. The pure C++ Root model also passes with the Java-generated `AXR3` fixture.
- Two independent hotfix reobfuscation passes each rewrote 70 mapped references and produced byte-identical output; an idempotence pass rewrote zero. Two independent packages are byte-identical.
- `ExternalAbiParityAudit` passes 642 common method names, 671 common argument shapes and 72 common fields against alpha.7.11. ZIP, Manifest/mod version and compile-only class exclusion audits pass.
- Protocol 8 is unchanged. Integrity DLL SHA-256 remains `828e3796ef33a551fc8ef4cdd457d83309ab4d539ec1dc7e62876cc65f5c66c5`; Root DLL remains `f481818e13ef1ba33a46b3dc4b6d4514949084e0014b226b43755f5e5691afe0`.
- Packaged JAR SHA-256: `cd514827faff24a9977a6a8d15f32195fd0ad25234cd91ca43bd12a4f12356c1`.
- Windows Forge runtime validation remains open and this build is not FULL L5.

## Alpha.7.11 R3.1 Modular Composition

- Java 17 compilation passed for the new Root API, composition root, per-server runtime, command bus, snapshot assembler, stable capability IDs and six capability modules.
- `AuthorityGuard` was reduced from 531 to 205 lines; lifecycle, vitals, harmful-effect cleanse, flight, topology and blade recovery now execute through an explicit module list.
- `R31ArchitectureSelfTest` rejects any restored `DominionLedger → LocalDominionCore` or `LocalDominionStore → ProjectionCoordinator` call, verifies the unified Root commit/publish lane, prevents Mixin implementation leakage back into the Authority façade and freezes the owner mask at `0x7ff`.
- `RootCommandBusSelfTest` verifies initialization, commit receipt, epoch equality, atomic projection publication and a clean pending-publication state.
- All 22 Java SelfTests pass against the final packaged, reobfuscated JAR. The prior pure C++ Native Root model and unchanged binaries retain the alpha.7.10 proof boundary.
- Two independent reobfuscation passes each rewrote 359 mapped member references and produced byte-identical trees; a pass over the reobfuscated tree rewrote zero references.
- Two independent deterministic packaging passes produced byte-identical JARs. `ExternalAbiParityAudit` passes 634 common method names, 662 common argument shapes and 72 common fields against alpha.7.10.
- ZIP integrity, Java 17 class version, Manifest/mod version, 18-Mixin metadata, five protocol-8 packet registrations, nested hook parity and compile-only class exclusion pass.
- Packaged JAR SHA-256: `7be484ef961b873fd9f0816c91fc44a1639a789c637adc9a07d4648b8e4c407a`.
- Integrity DLL SHA-256: `828e3796ef33a551fc8ef4cdd457d83309ab4d539ec1dc7e62876cc65f5c66c5` (unchanged). Root DLL SHA-256: `f481818e13ef1ba33a46b3dc4b6d4514949084e0014b226b43755f5e5691afe0` (unchanged).
- Network protocol remains 8. This offline gate does not claim Windows Forge runtime success or FULL L5 lifecycle attestation.

## Alpha.7.10 R3 Native Root foundation

- Java 17 compilation completed for the R3 loader, codec, JNI adapter, process root, decision source, projection reconciliation, runtime coordination and status changes.
- The complete class overlay rewrote 445 mapped member references on both independent passes; the two reobfuscated trees are byte-identical.
- `NativeSnapshotCodecSelfTest` and `native_root_model_test` share a Java-generated `AXR3` fixture. C++ independently accepts it, reproduces the FNV-1a fingerprint and exports identical bytes.
- The pure C++ test also covers monotonic install/current/stale/conflict outcomes, Authority capabilities, all four decision domains, projection acknowledgement, drift detection and policy/admission generation changes under `-Wall -Wextra -Werror -pedantic`.
- The Windows x64 Root DLL is built from the same pure model plus a thin JNI/Windows adapter. Its PE export table contains exactly the 10 expected `NativeRootBridge` entry points.
- The original `ronova.dll` remains the alpha.7.9 integrity binary; Root state is packaged separately as `ronova_root.dll`.
- All 20 standalone Java SelfTests pass against the packaged, reobfuscated JAR. `ExternalAbiParityAudit` passes 599 common method names, 625 common argument shapes and 71 common fields against alpha.7.9.
- Deterministic packaging produced byte-identical JARs from both reobfuscation trees. Compile-only Minecraft, Forge, Mixin and build-support classes are absent.
- Packaged JAR SHA-256: `7c72031d0b810c3cb0ac38611603273688fd6a155f6187daf6b5e7d04c62597c`.
- Integrity DLL SHA-256: `828e3796ef33a551fc8ef4cdd457d83309ab4d539ec1dc7e62876cc65f5c66c5` (unchanged). Root DLL SHA-256: `f481818e13ef1ba33a46b3dc4b6d4514949084e0014b226b43755f5e5691afe0`.
- Network protocol remains 8. This offline gate does not claim Windows Forge runtime success or FULL L5 lifecycle attestation.

## Alpha.7.7.2 R0 historical verification

## Alpha.7.7.2 hotfix checks completed in this workspace

- The alpha.7.7.1 Forge 47.4.22 run bound the owner, completed two explicit saves, shut down cleanly and started the same world again. This confirms that the alpha.7.7 `DominionLedger.save` packaging defect is repaired.
- `错误报告-2026-8-11_9.41.54.zip` then exposed a separate login-snapshot ABI defect: the compile stub declared `FriendlyByteBuf.Writer.accept(FriendlyByteBuf,Object)`, while the runtime interface inherits `accept(Object,Object)` from `BiConsumer`.
- The handwritten stub and `StubJarGenerator` now preserve the real `Writer extends BiConsumer` and `Reader extends Function` hierarchy. The new regression fails against alpha.7.7.1 and passes against alpha.7.7.2.
- The complete R0 class overlay changed 419 mapped members on pass one and zero on pass two; both output trees are byte-identical. Compared with alpha.7.7.1, only the Manifest, `mods.toml` and `RonovaNetwork$QuarantineSync.class` differ.

## Alpha.7.7.2 hotfix output

- `dist/ronova-0.2.0-alpha.7.7.2.jar`
- SHA-256: `9b014c872d7b085dda413317b5e1f511a22b4b47012e61524eb2da596ee5ece6`
- Network protocol: `8`; client and server must both use alpha.7.7.2.

## Alpha.7.7 R0 checks retained

- Compiled all twelve changed/new production source units with Java 17 against the retained Forge 47.4.22 ABI evidence; no new production compile error remained.
- Compiled and ran all fourteen standalone SelfTests against the reobfuscated production JAR; all passed.
- `SphericalRangeSelfTest` verifies the inclusive 64-block sphere and rejects points that only lie inside the AABB broad phase.
- `R0BoundarySelfTest` verifies that compatibility item binding cannot mutate durable ownership, every network action validates existing ownership, terminal revocation reaches `JudgmentState`, and client presence implements append/replace/remove/clear.
- `ForgeNetworkAbiSelfTest` verifies five registrations, five enqueued handlers and the centralized Forge 47.4.22 `SimpleChannel.send(PacketTarget,Object)` descriptor.
- `FriendlyByteBufLambdaAbiSelfTest` requires the erased `(Object,Object)V` Writer SAM and exact `writeMark(FriendlyByteBuf,JudgmentMark)` adaptation.
- Every standalone SelfTest is now registered under Gradle `selfTest`, and `check` depends on that aggregate task.

## Alpha.7.7.2 historical open verification gates

- At the alpha.7.7.2 gate, Gradle 8.8 and ForgeGradle repositories were unavailable, so a fresh `./gradlew clean check build` had not run for that historical artifact.
- Alpha.7.7.2 has not yet repeated the Forge reload/login-snapshot smoke test; dedicated server, multiplayer, cross-dimension, revocation and clean single-target action gates also remain open.
- Do not mark R0 complete or claim FULL L5/L6 from these source/bytecode checks. The earlier alpha.7.6 evidence below remains historical baseline evidence only.

## Alpha.7.6 historical baseline

- All 27 changed source files / 46 generated classes compiled with Java 17 against compile-only Forge 47.4.22 / Minecraft 1.20.1 ABI stubs derived from the previous verified artifact. No stub class is packaged.
- The changed classes were reobfuscated with the exact 1.20.1 `mappings.tsrg`; 492 mapped member references were rewritten across every mapped class namespace and exact application-to-Minecraft inheritance edge. A second pass changed zero references.
- Network/NBT bytecode descriptors were compared with the previous production artifact and corrected to the real `FriendlyByteBuf.Reader`, `FriendlyByteBuf.Writer`, `ByteBuf` return and `ListTag.add(Object)` descriptors.
- The `错误报告-2026-8-10_2.05.14.zip` runtime reached Forge `CONSTRUCT` and exposed one remaining compile-stub drift: `SimpleChannel.registerMessage` had been compiled with a `void` return although Forge 47.4.22 returns `IndexedMessageCodec.MessageHandler`. All five final call sites now use the real return descriptor.
- The `错误报告-2026-8-10_2.16.23.zip` runtime fully initialized alpha.7.2, reported native JVMTI `ACTIVE/0x7`, armed runtime reassertion and reached integrated-server command registration. It exposed `CommandDispatcher.register` returning `Object` in offline bytecode instead of Brigadier's `LiteralCommandNode`.
- `BrigadierAbiSelfTest`: passed for the root `/ronova` registration by inspecting the production `DominionCommands.class` and requiring the real Brigadier 1.0.18 return descriptor.
- The `错误报告-2026-8-10_2.53.29.zip` alpha.7.5 runtime passed the repaired ledger bind and then exposed `SimpleChannel.send(Object,Object)`, while Forge 47.4.22 requires `SimpleChannel.send(PacketDistributor.PacketTarget,Object)`.
- `ForgeNetworkAbiSelfTest`: passed all five packet registrations, all five `NetworkEvent.Context.enqueueWork` calls, two client-to-server sends, three server-to-client sends and all three packet-target constructions. The exact Forge 47.4.22 descriptors are required for every call.
- `ExternalAbiParityAudit`: passed 560 shared external method names, 584 shared argument shapes and 67 external fields against the prior ForgeGradle-built alpha.6 artifact. In addition to return and field drift, it now rejects unique argument-descriptor drift; running the enhanced audit against alpha.7.5 independently identifies the broken `SimpleChannel.send` descriptor.
- The `错误报告-2026-8-10_2.27.38.zip` and `错误报告-2026-8-10_2.29.27.zip` runtimes entered a single-player world, completed protocol-7 negotiation and logged the player in. Their tick and raw-mouse paths then exposed `com.mojang.blaze3d.platform.Window.getWindow` retaining its development name because the old offline eligibility check only admitted `net/minecraft/` owners.
- `MappedNamespaceSelfTest`: passed both complete window lookup chains and both key-state calls. The production bytecode contains runtime names `m_85439_` and `m_84830_`, with no remaining `getWindow` or `isKeyDown` development-name references.
- The `错误报告-2026-8-10_2.38.58.zip` runtime reached a world with only Ronova installed; `错误报告-2026-8-10_2.41.53.zip` reproduced the same failure with Pig2 present. Both independently exposed `DominionLedger.setDirty()` retaining its development name when javac emitted the application subclass as the symbolic invocation owner.
- `InheritedMappedMemberSelfTest`: passed all five production ledger dirty markers as inherited runtime name `m_77762_`. It also verifies all seven `DominionLedger.owners` accesses retain their application-defined name, preventing unsafe global same-name fallback from rewriting project fields.
- The production refmap contains matching `mappings` and `data.searge` sections for all old and new boundaries, including NBT/factory creation, persistent-manager admission, first tick, raw connection disconnect and server shutdown.
- `MixinMetadataSelfTest`: passed for all 18 configured mixins. Every class carries CLASS-retained `@Mixin` metadata in `RuntimeInvisibleAnnotations`; the supplied alpha.7.1 runtime log also confirms Mixin PREPARE progressed past the previous missing-annotation failure.
- `AgentTransformerSelfTest`: passed for reason-aware removal, revive/health gates, direct lifecycle writes, collection filtering, singular carrier preservation, client controls, render suppression, idempotence and stack-frame preservation.
- `RuntimeIndexSelfTest`: passed identity/global/local semantics, replacement cleanup, weak identity and 65,536-record stress. The final hotfix run completed 20,000 spatial misses in 29,002 microseconds.
- `ExecutionScopePolicySelfTest`: passed; every non-player action normalizes to global lineage and every player action remains UUID-exact.
- `ClientAuthorityStateSelfTest`, `ClientRecoveryGuardSelfTest` and `SessionJournalSelfTest`: passed.
- JAR ZIP integrity, Java 17 class version, Manifest, `mods.toml`, protocol 7, all 18 Mixin classes and the nested bootstrap hook were checked.
- The nested `hook-support.jar` `BootstrapHooks.class` is byte-identical to the outer updated class.
- Compile-only Minecraft, Forge, Mixin and build-support classes are absent from the production JAR.
- Embedded DLL format remains Windows x86-64 PE32+ and is unchanged from alpha.6. Resource and JAR SHA-256: `828e3796ef33a551fc8ef4cdd457d83309ab4d539ec1dc7e62876cc65f5c66c5`.

## Alpha.7.6 historical build output

- `dist/ronova-0.2.0-alpha.7.6.jar`
- SHA-256: `5a8ab5bec77c06d3b1ccad9da68c6b88b90f708ee1cc49d60df006d91e292560`
- Network protocol: `7`; client and server must both use alpha.7.6.

## Runtime validation boundary

Forge's dependency repository was unavailable in this isolated build environment, so alpha.7.6 was compiled and reobfuscated with extracted Forge 47.4.22 ABI/mapping evidence instead of running ForgeGradle itself. The supplied alpha.7.5 run verifies in a real Forge process that the prior inherited `SavedData.setDirty` defect is repaired: owner binding advanced to its next operation, `sendOwnerState`, before the bad compile-stub descriptor failed resolution. Alpha.7.6 corrects all three server-to-client `send` call sites and audits the complete network surface, but no complete alpha.7.6 play session was launched here; an actual run is still required before claiming sustained gameplay or every cross-Mod, GPU and JVMTI timing race in that installation.
