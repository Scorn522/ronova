# Changelog

> 历史归档（2026-09-08 整理）：保留当时的设计、实现和验证口径，不代表当前源码状态。文中的“当前”“下一步”属于记录当时。
> 当前入口：[进度](../../CURRENT-PROGRESS.md) · [有效规划](../../plans/README.md)。

> 本文件是历史 release log，不定义当前 working source 或 canonical 架构路线；当前状态以 `docs/plans/README.md` 和 `docs/CURRENT-PROGRESS.md` 为准。

## 0.2.0-alpha.7.17-r66（versioned projection，2026-09-07）

- 网络协议升级为 10，投影绑定 Realm/连接 stream/context/Root epoch/连续版本；分片完整后一次发布，旧包不覆盖当前状态。
- Owner 授权版本阻止回滚；断档/缺片触发限流 resync，发布方从 Root 当前 marks 取数，断开清理旧镜像。
- 52 项分层回归和两轮 Forge 多片/旧包/错 Realm/Owner/resync 验证通过。[r66 交接](../handoffs/7.17-VERSIONED-PROJECTION-HANDOFF.md)说明仍未完成的参与范围与完整 7.17 工程。

## 0.2.0-alpha.7.17-r65（obligation checkpoint，2026-09-07）

- 有界义务/证据/coverage checkpoint 接入同一 Root writer；首次激活 schema 6，旧无检查点 Root 保留 schema 5。
- expected revision、ACK 重试与防遗忘检查不推进裁决代际；重启保留历史证据但要求重新验证。
- 51 项分层回归和两轮真实 Forge 保存恢复通过。[r65 交接](../handoffs/7.17-OBLIGATION-CHECKPOINT-HANDOFF.md)说明周期内未确认窗口及剩余完整 7.17 工程。

## 0.2.0-alpha.7.17-r64（obligation review，2026-09-07）

- 添加有界候选证据/义务诊断，按资格和结果合并；强/独立弱证据规则不把重复、过期或未验证来源升级为权限。
- 主体间轮转审查、固定 detail/样本上限、coverage gap 与资源释放；原 Root 未知效果通过分页恢复，不重放副作用。
- 51 项分层回归、最终两轮 Forge 合并/恢复/轮转验证通过。[r64 交接](../handoffs/7.17-OBLIGATION-REVIEW-HANDOFF.md)明确生产 attestation、持久 candidate 义务与完整 7.17 尚未完成。

## 0.2.0-alpha.7.17-r63（terminal operation plan，2026-09-07）

- 生产裁决接入 Root 从原回执批准的固定操作计划，贯通自有准入/存在登记、生命周期步骤和网络投递；每步及主要调用之间复验资格。
- 新绑定裁决的整个生命周期调用使用一次 OPAQUE bundle；与旧 public-death/claim 双向互斥，重规划和维护不能改名重发未知效果。
- 50 项分层测试与最终两轮真实 Forge 回调计数/撤销测试通过，完成状态保持 PARTIAL/FENCED。完整范围见 [r63 交接](../handoffs/7.17-TERMINAL-OPERATION-PLAN-HANDOFF.md)。

## 0.2.0-alpha.7.17-r62（runtime inventory，2026-09-07）

- 登记八类运行表面与 typed observation，当前四类 S1、四类 S0；失败/移除/替换保留历史证据但不伪报当前能力。
- 15 个资源域区分 owned accounting 与 host JMX 观测；构造/网络队列接入共享配额、取消/完成/诊断淘汰释放和恢复余量，未知项不按 0 处理。
- 新增 `/ronova diagnostics`；49 项分层回归与最终成品两轮真实 Forge 配额/释放/恢复验证通过。[r62 交接](../handoffs/7.17-RUNTIME-INVENTORY-HANDOFF.md)说明尚未完成的 C/D 与 7.17 工程。

## 0.2.0-alpha.7.17-r61（deferred authority，2026-09-07）

- 网络排队任务与构造观察捕获 Root 资格；Owner revoke/regrant、旧连接、政策变化与到期任务不能继承新资格。Realm 校验先于弱物理身份收据缓存。
- 准入 pass 绑定 nonce/完整向量/投影，普通与乘客 tick 复验；HELD 保留原始实体、NBT 和调度成员，显式 rebind 后恢复，不生成墓碑或终结副作用。
- 补齐生产静态 passenger refmap 与可恢复调度回归；47 项分层测试、最终 JAR 两轮真实 Forge channel/保存恢复通过。[r61 交接](../handoffs/7.17-DEFERRED-AUTHORITY-HANDOFF.md)列明未完成的 7.17 工程。

## 0.2.0-alpha.7.17-r60（subject binding，2026-09-07）

- 同一 Root 主体记录接入物理 incarnation、独立 Binding/Judgment/Admission 与域 Ownership revision；生产裁决原子绑定并终结，入口复验完整向量与物理 nonce。
- guarded rebind/reactivate/owner 更新与撤销通过既有 canonical command 提交；域更新不产生第二个死亡效果身份。
- 修复非玩家精确身份请求被扩大成全类型范围、保存后被误标为玩家的问题，保留旧命令/hash/墓碑原义。
- 46 项分层回归通过，最终成品两轮真实 Forge 原版实体/所有权/保存恢复验证通过；完整身份迁移与 7.17 剩余工程见 [r60 交接](../handoffs/7.17-SUBJECT-BINDING-HANDOFF.md)。

## 0.2.0-alpha.7.17-r59（qualification preconditions，2026-09-07）

- 生产 Judgment batch 显式捕获 Realm、政策版本和完整逐主体预期资格，写入 canonical hash 与 journal；任一冲突整批拒绝，重试不刷新预期值。
- 新 receipt 校验每个目标精确推进一次；保存、ACK 丢失与重启恢复保留原命令/哈希/回执，旧命令哈希保持原样。
- 46 项分层回归与实际 Forge 世界副本两轮受控资格/过期拒绝/恢复通过。完整 Binding/Ownership 和 7.17 剩余工作见 [r59 交接](../handoffs/7.17-QUALIFICATION-PRECONDITIONS-HANDOFF.md)。

## 0.2.0-alpha.7.17-r58（startup hotfix，2026-09-07）

- 修复 cutover 后旧 SavedData 镜像的 reason/哈希假冲突否决有效 Root；改为 canonical Root 优先、持久事实比较和单向镜像重建。
- 镜像旧字节有界备份；坏镜像不能成为第二权威，真实 Root 损坏/缺失仍拒绝。Root schema 5、协议 9 和历史回执保持。
- 新增非创建型运行时查询、失败构造缓存与资源清理；停服过程不重新启动失败的运行时。
- 完整 build 与 46 项分层测试通过（44 production JAR + 2 development outputs）。实际 Forge 47.4.23 世界副本两轮启动/保存/退出通过，原 Root 字节及原世界文件不变。
- 交接见 [r58 Handoff](../handoffs/7.17-ROOT-BOOTSTRAP-HOTFIX-HANDOFF.md)。本批不宣告完整 7.17。

## 0.2.0-alpha.7.17-r57（implementation candidate，2026-09-06）

- Root schema 5 / effect journal 1：现有 public-death 入口先提交 PREPARED，再以原 claim 实例进入一次 callback；正常返回仍保留 EFFECT_UNKNOWN，不伪造外部效果完成。
- 原 command/qualification/attempt 保真，clear 不删除未知效果；旧 unqualified/已 claim 数据保留原语义，现有 kill/discard/remove 等其他物理 lane 不因此获得 exactly-once 保证。
- 新 command/intent 安装工作图像前预检 64 MiB 格式、每未决效果 1024 字节增长余量、双图像/8 MiB 恢复空间和 owned quota；复用暂存文件不重复计费。实际写失败继续 UNKNOWN。
- 完整 build 与 45 项分层测试通过（43 production JAR + 2 development outputs），包含 6 个受控子 JVM halt、真实文件、延迟 callback 和容量模型/拒绝恢复。
- 归档并核对用户 r56 实机日志与两份旧 Root；修复无用 GbufferProjMat shader 声明。r57 游戏内、真实第三方链、Native 语义/GPU 验收仍待执行。
- 细节见 [r57 Handoff](../handoffs/7.17-EFFECT-JOURNAL-HANDOFF.md)，成品见 `BUILD-RESULT-r57.txt`。

## 0.2.0-alpha.7.17-r56（implementation candidate，2026-09-06）

- 分离已确认 Root 读取头与待恢复工作图像；持久化失败时不提前公开 snapshot/epoch/commit index/策略/Owner/Tombstone，后台投影重试不发布未确认变化。
- 新增 Realm-qualified SubjectKey 和每主体 TargetEpoch；终结、解除、再次终结原子持久化，解除后保留代际历史，原 receipt 不续租为当前资格。
- Root schema 4、journal format 3、qualification extension 1；旧 schema/command/hash/opaque claim 语义保留，历史资格基线 0 不签发新效果权限。
- 生产执行前复验原 receipt 与当前 TargetEpoch；新增稳定 EffectKey 类型，通用 effect journal 和完整 lease 尚未完成。
- 完整 build 与 43 项分层测试 PASS（41 production JAR + 2 development outputs）；真实 Forge/Native/GPU/多人/进程崩溃仍 UNVERIFIED。
- 建立 B01–B23 + X24–X68 新验收台账，历史等价未证；细节见 [r56 Handoff](../handoffs/7.17-SUBJECT-QUALIFICATION-HANDOFF.md)。

## 0.2.0-alpha.7.17-r55（implementation candidate，2026-09-06）

- 修复 lifecycle claim 持久化失败后删除内存记录/清 pending 的窗口；原子替换成功但 ACK 丢失时保留 intent，恢复不重新授予死亡入口资格。
- 新增 typed LifecycleClaimResult，区分首次授权、已有 claim/效果未知、持久化未知与拒绝；旧 runtime 入口复用该合同。
- claim 创建前先解析 pending Root 图像；effect 持久化不生成新 Judgment epoch 或 command commit index。旧 receipt 编码/hash、schema 3、协议 9 保持不变。
- `/ronova status` 新增 `lifecycle_unverified_claims`，不把 claim 数当成死亡效果完成数。
- 真实文件故障窗口、重启、pending Judgment 和并发 claim 回归通过；完整 build 成功，42 项分层测试通过（40 production JAR + 2 development outputs）。
- 尚无真实 Forge/Native/GPU/多人/进程终止验收；通用 effectKey、完整副作用恢复和独立 executor 仍未完成。详见 r55 Handoff。

## 0.2.0-alpha.7.17-r54（source candidate，2026-09-05）

- 新增同一 Root StrengthPolicy 内的 14 项 AttackActionPolicy；54 个高级键不变。普通许可、动作深度、相关高级键、运行/证据 ceiling 取交集；强力攻击不独立授予其他动作。
- schema 3 保存普通字段和 action matrix revision；schema 1/2 可迁移，旧 Custom 的新字段默认关闭。历史 ConfigureStrengthPolicy 命令保留原 schema/hash，不能用旧哈希验证新增动作权限。
- 普通项与高级项命令编辑进入 Custom，其余值固化；整份配置仍重启激活。
- Root 在新终结提交处检查 active terminalize；已提交回执重放不被配置关闭撤销。JudgmentExecutor 的物理动作入口必须通过 committed receipt membership gate。
- 保留 r53 TerminalLifecycle 物理链，避免只关 remove 却保留隔离引起不可见残留。其余十三项先保存与解析，独立执行接线尚未完成，详见 r54 Handoff。
- 补策略、NBT、Root 重启与提交门回归；注册 SelfTest 数量保持 42。完整 Forge 编译/JAR/实机验证 OPEN，不宣称全返回等 Extreme executor 已实现。

## 0.2.0-alpha.7.17-r53（source candidate）

- 2026-09-05 DOCUMENT-ONLY：加强总纲升为 V2.6，修订普通开关/高级政策权限映射、canonical identity 与资格 batch、副作用未知恢复、server/client completion 参与范围、统一规范链与历史验收缺件，增加 B01–B23 / X57–X68、7.17 分批路线及持久空间治理。V2.5/V2.4 原稿保留；源码、测试、构建脚本、版本与验证结果不变。详见 `docs/archive/reports/DOC-REVISION-V2.6.md`；不是 Extreme 实现或发布记录。

- 2026-09-05 信息同步：上传包已包含批处理 prepare/applyCommitted、DurableTerminalWatchSet 与禁止旧终态 forget 调用的测试修正。用户反馈似乎可生成 JAR；完整成功日志和最终生产 JAR 尚未提供，42 项分层验证及成品审计仍待证据。本次仅同步版本与文档，不修改生产 Java、现有测试或构建脚本。
- 将上传包的 `mod_version=0.2.0-alpha.7.17-r3` 统一回 `r53`；保留构建脚本现有的 JDK 配置。
- 当前测试分层为 40 production JAR + 2 development outputs；两个开发输出任务为 DurableRootStoreSelfTest 和 StrengthPolicyPersistenceSelfTest。

- fix2：第二次 Windows reobf 回执确认两个记录入口分别名为 `recordBatch` 与 `record`；修正 fix1 对方法名的错误合并，当前测试精确冻结 11 个方法中的 11 个映射调用。生产代码和 Mod 版本仍不变。
- fix1：根据 Windows reobf 构建证据，把 `InheritedMappedMemberSelfTest` 从历史总数 8 改为逐 durable mutation 方法核对当前 11 个已映射 `SavedData.setDirty` 调用；生产代码和 Mod 版本不变。
- fix1：测试资源位置构造改用 `ResourceLocation.fromNamespaceAndPath`，消除两条待删除 API 警告。

- 用 versioned `ProfilePresetMatrix` 替换 r52 的统一 plane 映射；54 个稳定 key 均由完整逐项表解析。兼容档当前为 12 项 `MONITOR`、42 项 `PROTECT_RONOVA`，强化档为逐项 `RELATED_PATHS`，极限档为逐项 `STRICT_ALL`。
- `StrengthResolutionReceipt` 增加 configured/active matrix revision、preset/requested、supported/evidence ceiling 与 mandatory-floor 标记；`effectiveLevel` 不得越过请求、运行时或证据上限。
- Host Safety 相关核心权限、重启恢复、防回滚与死循环保护具有不可降至关闭的最低安全级；普通 Custom 选项仍可关闭。
- 进入 Custom 时固化当前 54 项解析值，避免未编辑项随矩阵升版隐式漂移；整份配置仍只在重启边界整体激活。
- StrengthPolicy NBT 升至 schema 2，持久化 matrix revision；schema 1 策略与 command journal hash 可读取并在下一次保存时重写为当前编码，未知 revision fail closed。
- `/ronova status` 与 `/ronova strength status` 报告 configured/active matrix revision。
- `StrengthPolicySelfTest` 的逐项矩阵、ceiling、安全下限、Custom 固化和未知 revision 用例通过；相关 policy、Root command journal 与 NBT codec 生产源码通过 Java 17 `-Xlint:all -Werror` 离线编译。
- 新增 `StrengthPolicyPersistenceSelfTest`，总数升至 42（40 production JAR + 2 development outputs）；离线回归已验证 schema 2 往返、schema 1 迁移、严格 54 行与 journal tag/hash 重写。
- 本条不是 release record：用户反馈似乎可生成 JAR，完整成功日志、42 项分层结果、最终 JAR 审计和实机验证尚待证据。八类 Runtime Surface executor 与 CrossSurfaceOperationPlan 未由本版本交付。

## 0.2.0-alpha.7.16-r52（source candidate）

- 将 Java `DurableRootStore` 晋升为 sole durable writer；`DominionLedger` 退为 downgrade-only mirror，镜像失败不再影响 Root truth。
- 新增世界本地 `ronova/durable-root-v1.bin` 权威文件：64 MiB 有界 NBT payload、版本头、SHA-256、临时文件强制落盘和原子替换；不支持原子替换时 fail closed。cutover 后文件缺失、损坏或 checksum 不符禁止回退 SavedData，`SavedData#setDirty()` 不再被当作 durable commit 证明。
- command journal 升为紧凑 format 2：Root Snapshot 只在权威图像保存一次，每条 journal row 不再重复嵌入历史 Snapshot，避免随命令数和状态规模近似二次增长；format 1 可读并在保存时迁移。
- 新增动作级 `JudgmentBatch`：全部目标共享 command id 与 decision epoch，准备阶段无物理副作用，单次 Root commit 后才执行终态链并原子发布 projection。
- 新增 canonical command journal、payload hash、commit index、`COMMIT_UNKNOWN` 与原 command-id recovery；覆盖提交前失败、提交后 ACK 前失败、重复 replay 和 command-id payload conflict。
- 客户端有序动作现在以 `RealmId + player UUID + session + sequence` 确定性派生 ingress command id，动作类型进入不可变 command domain/canonical payload；相同序号改发另一动作会冲突拒绝，不能生成第二条命令。服务端在重新发现目标前按 ID 查询 Durable Root。已提交的 lost ACK 跨重启只返回原 receipt，未解决的模糊提交不缓存、不 ACK、不广播。
- 网络协议升至 9：Owner 状态携带 RealmId，动作请求必须回传同一 Realm；客户端断线时仅保留未确认 outbox，仅在同 Realm、同玩家上下文恢复原 session/sequence，上下文改变会清空旧输入，防止旧动作在同地址新世界或另一玩家身份下被当成新命令。
- 已授权但零目标的动作写入不推进 epoch 的 durable empty receipt，避免重启后因目标消失而把同一输入当作新动作。
- 用 tombstone-owned `DurableTerminalWatchSet` 取代 Judgment 的有限 retry 生命周期；HOT/WARM/COLD lane、弱引用、Admission incarnation 与 WorkBudget 已接线，只有 committed clear 才释放。
- Root commit 后若在物理终态完成前崩溃，TerminalWatch/Admission 会沿 committed completion 路径续作；durable lifecycle receipt 保证死亡/掉落整合最多一次。
- 新增 StrengthPolicy foundation：4 profile、54 stable key、6 mechanism level、configured/active/supported/effective/evidence/activation 分离、policy revision/hash 持久化和 coherent restart activation。
- 新增 `/ronova strength compatible|enhanced|extreme|custom`、`/ronova strength status`，并扩展 `/ronova status` 的 Durable Root/Strength 字段。
- 新增 versioned extension envelope/registry，以及 TargetEpoch、WriteSink、Completion extension seam；完整 executor 与传播仍按 V2.4 留到 7.17/L6E。
- standalone SelfTest 注册数增至 41。六项聚焦自测以及 Root、网络入口、客户端 outbox、终态接管和命令/状态相关生产源码的 Java 17 严格离线编译通过；durability 回归覆盖丢弃内存后从文件恢复 lost ACK、稳定 ingress replay、checksum corruption 与原子替换失败拒绝 cutover。
- 本条不是 release record：r52 Forge reobf JAR、41/41 exact-artifact、Windows Forge 与 Native/GPU/process-tree 验证尚未完成。

## 0.2.0-alpha.7.15-r50

- 修复全新世界无 Owner/Judgment mutation 时 `DominionLedger` 的随机 `RealmId` 未进入首次保存：无参构造现在立即标记 SavedData 待保存；新增无 mutation 的 Ledger + Shadow 双重启回归。
- 移除 RAW `TopologyGraph.staleBacklog()` 对全部 observation 的双重全表扫描；维护成本改为每 tick 最多发现 257 条、处理 256 条 queued stale reference，计时门在发现前启动。`TopologyGraphSelfTest` 新增 200,003 条 live reference、仅 3 条 stale 的稀疏压力回归和禁止 `Map.keySet/values/entrySet/forEach` 的字节码门禁。
- Admission 构造队列改为每个物理对象最多 8 次失败尝试；构造器与工厂的重复 callback 先经弱身份 `addBounded` 去重，共享同一组额度，每次入队最多回收 16 条旧弱引用。失败项继续轮转，不阻塞后续对象，耗尽后移入最多 1,024 条的 dead-letter 诊断区并从活动 backlog 移除。`/ronova status` 新增 pending、quarantined、total、latest UUID 与 retry limit 字段。
- `BoundedRetryQueueSelfTest` 新增永久失败 100 tick 探针、后续成功对象、活动 backlog 最终清空、dead-letter 尝试次数和有界 FIFO 淘汰回归。
- `build.gradle` 共注册 40 项 standalone SelfTest：39 项针对重混淆生产 JAR，`DurableRootStoreSelfTest` 1 项针对开发主输出；40/40 分层回归通过。`mc_check` 与 `mc_build` 均通过，Gradle 完成 63 个任务。
- 成品 `ronova-0.2.0-alpha.7.15-r50.jar` 大小为 3,460,001 字节，含 368 个 ZIP 条目、0 个重复项、19 个 Mixin；两个内嵌 Windows x64 DLL 与源码资源逐字节一致。SHA-256：`0e0a82422cb56cc622d18a1ddb0a4864efb861f8a567d0c296cb30e67560879d`。
- 7.16 Java Durable Cutover、durable TerminalWatch takeover、JudgmentBatch 与旧 finite retry 删除仍未开始。

## 0.2.0-alpha.7.15-r49

- 完成 7.15 Java `DurableRootStore` Shadow implementation candidate：`RealmId` 绑定、shadow state extraction、snapshot/state hash、NBT reload、legacy realm migration marker 与 crash failpoint recovery。
- 保持 `DominionLedger(SavedData)` 为唯一 authoritative writer；`LifecycleIntegrationReceipt` 由 Ledger 唯一创建，shadow 只镜像、验证，并拒绝同 epoch 的 receipt 删除或改写。
- 新增 `BoundedRetryQueueSelfTest`，覆盖快照轮转、预算耗尽后的 carry、后续 tick 恢复、并发 drain 串行化、输入校验与清理；standalone SelfTest 注册数增至 39。
- `mc_check` 与 `mc_build` 均通过：Gradle 完成 63 个任务，39/39 standalone SelfTest 全部通过。产物 SHA-256：`cc55bc2e017d9e96c6798df00592a3a0accbbde4d35748d49695551346b57e74`。
- 7.16 Java Durable Cutover、durable TerminalWatch takeover、JudgmentBatch 与旧 finite retry 删除仍未开始。

## 0.2.0-alpha.7.14b-r42

- 完成 Internal Root canonical 文档一致性收尾：明确区分 r27 历史源码审计输入与 r42 当前 working source，统一 7.14D 已完成本地 implementation candidate、7.15 为下一阶段的状态（该条为历史 r42 记录）。
- 修正 L6 Master、L6E-0、P0-A 与 7.14D Implementation Spec 中残留的旧当前态标签，同时保留历史 QA、审计与版本记录中的 r27/r39 事实。
- `mc_check` 通过：168 个 Java 文件、101 个依赖 JAR，无类型错误。
- `mc_build` 通过：Gradle 8.12.1 / JDK 17，61 个任务完成，38/38 standalone SelfTest 全部通过。产物 SHA-256：`f7144d7e06180e2cb27f100c2056f653dcfafcda36c9ef940f940ce19a4ea918`。
- 本候选不宣称 Windows Forge 实机、GPU、process-tree 或独立 `verification-report.json PASS`；7.15 DurableRootStore Shadow 仍是下一阶段。

## 0.2.0-alpha.7.14b-r39

- 完成 7.14D FrameBudget / WorkBudget 与结构化 telemetry seam：按服务器隔离 governor、稳定预算类别、不可变回执和同步查询计量。
- 将预算接入有限可延期工作，包括构造准入队列、有限终端 retry 与 RAW/LEGACY observation stale 维护；预算耗尽保留工作并通过 `deferred` / `carryIn` / `carryOut` 记录，不静默丢弃。
- 修复弱引用维护的有界转移、当前引用判断和一次性 `reaped` 状态，防止旧引用误删新观察、重复入队或跨 tick 丢失积压。
- 保持 `AuthorityView`、`PresencePolicy`、当前对象 admission 和同步发现查询的同步语义；`PresenceBudgetSeparationSelfTest` 验证预算耗尽不会改变答案。
- `mc_check` 通过；`mc_build` 完成重混淆、资源与 Mixin 处理，38/38 standalone SelfTest 全部通过。产物 SHA-256：`441476608d8348aa618890cff72edb2f7ab28455881532e892675fddc988e13f`。
- 本候选不宣称 Windows Forge 实机、GPU、process-tree 或独立 `verification-report.json PASS`；7.15 DurableRootStore Shadow 仍是下一阶段。

## 0.2.0-alpha.7.13

- Started the real L5 strength phase with R4/L5A Native semantic enforcement leases over the Windows Forge frozen alpha.7.12 baseline.
- Added independent Native bytecode evidence for the loaded `Entity`, `LivingEntity`, `Level`, `ServerLevel` and `ClientLevel` gates instead of treating a successful JVMTI retransformation request as proof of enforcement.
- Added bounded watchdog renewal, monotonic lease generations, dynamic required/verified masks and separate transformation, semantic-failure, successful/failed lease and age counters.
- Latched per-cycle failures so one successful duplicate from another ClassLoader cannot hide a failed copy of the same core target.
- Added a cold `NativeIntegrityBridge -> EnforcementRuntime` projection boundary. Server runtime polls at most once per 20 ticks; Authority, Judgment and Spawn hot paths never call semantic-status JNI.
- Added explicit `R4_L5A_SEMANTIC_NATIVE_ROOT`, degraded Native and Java fallback contract states without changing gameplay behavior during degradation.
- Made integrity-DLL builds deterministic by stripping random PDB/debug identity; two independent Windows x64 builds are byte-identical. The Native Root DLL is unchanged.
- All 27 packaged-JAR Java SelfTests, the Java/C++ Root fixture, double reobfuscation, deterministic packaging and external ABI parity gates pass. Protocol 8, Root snapshot schema, ranges, lineage scopes and five packet payloads are unchanged.
- This is a real integrity-strength increase but remains L5A, not FULL L5. Alpha.7.12 stays the live baseline until Windows Forge verifies lease renewal and gameplay equivalence.

## 0.2.0-alpha.7.12

- Completed R3.2 as a behavior-preserving component-boundary refactor over the Windows Forge accepted alpha.7.11.1 baseline; protocol 8, Root schema, ranges, lineage scopes and both Native DLLs are unchanged.
- Replaced process-global Spawn admission caches and construction queues with one `SpawnAdmissionRuntime` per composed server while retaining the `SpawnFirewall` compatibility API used by Mixins, events and commands.
- Split `ClientRuntime` into ordered action delivery, raw input/instant-mining targeting and lifecycle/surface recovery controllers. The former Mixin, Forge and packet-handler entry points remain stable.
- Split `RonovaNetwork` into protocol transport, five packet schemas, server action execution and quarantine synchronization services while preserving discriminator order and payloads.
- Extended deterministic packaging to prune stale nested classes when a top-level implementation is replaced, preventing removed alpha.7.11.1 packet/runtime classes from surviving in the JAR.
- Added client-outbox and R3.2 architecture regressions. All 25 packaged-JAR Java SelfTests, the Java/C++ Root fixture, double reobfuscation, deterministic packaging and external ABI parity gates pass.
- This remains R3 structure work, not FULL L5. Alpha.7.12 later passed its Windows Forge equivalence, state-reload and gameplay smoke tests and became the frozen live baseline for R4.

## 0.2.0-alpha.7.11.1

- Fixed the alpha.7.10 Windows Forge failure where client Presence and Authority reads used epoch `0:0` against an active integrated Native Root, producing more than 2.5 million counted Java fallbacks.
- Made Authority, Admission and Presence routine reads consume only the immutable Root-approved local projections. `JudgmentExecutor` remains the sole gameplay decision lane that crosses the Native boundary.
- Removed per-entity Admission JNI calls at construction, join, level, manager and first-tick gates. Native snapshot publication, coordination, reconciliation and explicit Judgment remain unchanged.
- Prevented no-op Root commands from republishing unchanged snapshots; a same-epoch content mismatch now fails closed instead of being presented as a harmless no-op.
- Stopped returning `null` from the `EntityType.create(Level)` factory hook. Natural spawns are rejected and counted at JOIN/LEVEL/manager boundaries without triggering `NaturalSpawner`'s repeated `Can't spawn entity of type` warning.
- Added `HotPathProjectionSelfTest` and expanded architecture/commit wiring regressions. All 23 Java SelfTests pass before packaging; protocol 8 and both Native DLLs remain unchanged.
- This is an R3.1 runtime repair, not FULL L5. Windows Forge must still confirm zero new `degraded_decisions`, zero `stale`, silent lineage suppression and save/re-entry.

## 0.2.0-alpha.7.11

- Completed R3.1 Modular Composition as a behavior-preserving structural refactor; PRIMARY 64, AREA 64, LOADED, Authority, tombstone, single-settlement, Native snapshot and protocol-8 semantics are unchanged.
- Added loader-neutral `DominionCommand`, `CommitReceipt` and `DominionRootGateway` contracts plus one `RootCommandBus` that serializes durable mutation and projection publication.
- Added `DominionCompositionRoot`, `ServerRuntimeRegistry` and an explicit per-server runtime graph. Concrete Local Root, Projection, Authority state and Capability modules are wired only at the composition boundary.
- Removed both reverse dependencies found in alpha.7.10: `DominionLedger` no longer calls `LocalDominionCore` while assembling snapshots, and `LocalDominionStore` no longer imports or publishes `ProjectionCoordinator` state.
- Split the 531-line `AuthorityGuard` implementation into a 205-line compatibility façade, per-server Authority state, an explicit Capability dispatcher and isolated Lifecycle, Vitals, negative-effect, Flight, Topology and Blade modules.
- Replaced capability `enum.ordinal()` masks with explicit stable IDs while preserving the exact alpha.7.10 owner mask `0x7ff` and Native payload.
- Added fail-closed Root publication recovery: a durable commit that cannot reach Native/Projection is counted, remains unavailable to gameplay and is retried before Native-to-Java reconciliation can overwrite it.
- Added 2 dedicated R3.1 regressions and updated earlier wiring gates. All 22 packaged-JAR Java SelfTests pass; both embedded DLLs, ASM support and Agent hook remain byte-identical to alpha.7.10.
- This is still an R3 foundation, not FULL L5. Critical lifecycle enforcement has not yet received independent Native attestation.

## 0.2.0-alpha.7.10

- Added the R3 Native Root foundation without changing the frozen PRIMARY 64, AREA 64, LOADED, Authority, tombstone, single-settlement or protocol-8 semantics.
- Added a canonical bounded `AXR3` snapshot format with deterministic FNV-1a/SHA-256 fingerprints and strict Java/Native validation.
- Added a Native-owned monotonic snapshot root that rejects rollback and same-epoch conflicts, tracks policy/admission generations, exports authoritative state and answers Authority, Judgment, Admission and Presence decisions.
- Cut `LocalDominionCore` over only after Native accepts a snapshot; unavailable or failed Native decisions use an explicit counted Java fallback.
- Added Native-to-Java projection acknowledgement and low-frequency reconciliation. Missing or drifted Java projections rebuild from the validated Native snapshot rather than silently becoming a second source of truth.
- Kept the original JVMTI integrity DLL byte-identical to alpha.7.9 and placed Root state in a separate DLL. Java loading, wire codec, JNI protocol, process coordination and decision fallback remain separate classes; Native domain state is separated from the JNI/Windows adapter.
- Added 2 Java regressions plus a pure C++ model test, including a Java-generated snapshot interoperability fixture and PE export-surface audit. All 20 packaged-JAR Java SelfTests and the Native model test pass offline.
- This release is an R3 foundation, not FULL L5: independent Native attestation of critical lifecycle enforcement remains a later gate.

## 0.2.0-alpha.7.9

- Completed R2 Projection Split without changing the frozen PRIMARY 64, AREA 64, LOADED current-dimension, Authority, tombstone or network-protocol-8 semantics.
- Added an atomic `ProjectionCoordinator` that derives AuthorityTopology, Capability, Admission and Presence projections from one immutable `DominionSnapshot` epoch; stale and conflicting publications cannot replace current truth.
- Added `LocalDominionStore` as the only R2 adapter around `DominionLedger`; gameplay hot paths consume projections while SavedData remains the local durable source.
- Split target discovery from terminal execution through `TargetDiscovery` and `JudgmentExecutor`, so selection cannot silently commit policy and execution cannot expand its target set.
- Moved owner/capability lookups, tombstone matching, login presence snapshots and status reporting onto the published projection bundle.
- Unified object-boundary and NBT-boundary Admission counters across projection rebuilds, fixing the `rejected=0` observability gap seen while lineage rejection was already active.
- Added atomic publication, rollback, metrics-continuity and bytecode-wiring regressions; all 18 standalone SelfTests pass on the packaged JAR.
- Kept the embedded Windows x64 DLL, ASM support JAR and Agent hook JAR byte-identical to alpha.7.8.

## 0.2.0-alpha.7.8

- Completed R1 Dominion Contract Extraction with loader-neutral Subject, Decision, Snapshot, Epoch and Capability contracts.
- Routed Authority, Judgment, Admission and Presence through the replaceable `LocalDominionCore` decision seam.
- Added monotonic `generation:sequence` persistence and `/ronova status` contract epoch reporting while preserving protocol 8 and alpha.7.7.2 gameplay semantics.
- Added contract state-machine and production-bytecode wiring regressions.

## 0.2.0-alpha.7.7.2

- Fixed the reload/login crash captured in `错误报告-2026-8-11_9.41.54.zip`: the offline compile stub modeled `FriendlyByteBuf.Writer` with a specialized `accept(FriendlyByteBuf, Object)` method, while Minecraft 1.20.1 inherits the erased `accept(Object, Object)` SAM from `BiConsumer`. The mismatch caused `AbstractMethodError` when a Presence snapshot was encoded after re-entering the world.
- Corrected both the handwritten network stub and `StubJarGenerator` so `FriendlyByteBuf.Writer` extends `BiConsumer<FriendlyByteBuf, T>` and `Reader` extends `Function<FriendlyByteBuf, T>`.
- Added `FriendlyByteBufLambdaAbiSelfTest`, which rejects the broken alpha.7.7.1 LambdaMetafactory descriptor and requires the runtime-compatible erased SAM plus the exact `writeMark` adaptation.
- Kept network protocol 8 and all alpha.7.7 R0 behavior unchanged; clients and servers should both run alpha.7.7.2.

## 0.2.0-alpha.7.7.1

- Fixed the integrated-server save crash captured in `错误报告-2026-8-11_9.22.46.zip`: the offline release remapped inherited `SavedData.setDirty()` call sites but left the `DominionLedger.save(CompoundTag)` override declaration under its development name, causing `AbstractMethodError` when Forge dispatched `SavedData.m_7176_`.
- Extended `OfflineReobfuscator` to remap application method declarations through the same exact parent hierarchy used for member references. The transformation now handles mapped abstract/virtual overrides generically instead of patching `DominionLedger` with a hard-coded SRG method.
- Expanded `InheritedMappedMemberSelfTest` to require exactly one `m_7176_(CompoundTag)` override, reject any leaked `save(CompoundTag)` declaration, retain all five mapped `setDirty` calls and preserve application-owned fields.
- Kept network protocol 8 and all alpha.7.7 R0 behavior unchanged; clients and servers should both run alpha.7.7.1.

## 0.2.0-alpha.7.7

- Completed the first R0 baseline-repair batch without adding L5/L6 feature code or importing MS-Overkill architecture.
- Made terminal revocation complete across `DominionLedger`, admission pass/reject caches, weak execution identity, loaded-object NBT hints, server quarantine objects and client presence projections. Serialized NBT is now a non-authoritative hint and cannot recreate a revoked judgment.
- Replaced attacker-only quarantine packets with protocol-8 presence operations: full snapshot on login/player replacement, append broadcast for new judgments, UUID removal and full clear for every tracked client.
- Unified both normal entity discovery and independent topology recovery on one exact spherical 64-block predicate; the AABB remains broad phase only.
- Froze `DominionLedger` as the sole server authorization source for R0. `/ronova give` and `/ronova bind` are permission-level-2 administrative grants; holding the blade, inventory ticks and action packets can only restore an existing grant.
- Coalesced routine inventory/player-tick repairs while retaining the intentional server-tick head/tail recovery pair and immediate violation repairs.
- Centralized both Forge network send directions behind one ABI-audited call site and kept all five packet registrations on the Forge 47.4.22 descriptors.
- Wired all thirteen standalone SelfTests into Gradle `check`, including new spherical-range and R0 structural boundary regressions.
- Bumped the network protocol to 8; clients and servers must both run alpha.7.7.

## 0.2.0-alpha.7.6

- Fixed the server-thread player-tick crash captured in `错误报告-2026-8-10_2.53.29.zip`: the compile-only `SimpleChannel.send` stub used `(Object, Object)` while Forge 47.4.22 requires `(PacketDistributor.PacketTarget, Object)`.
- Expanded `ForgeNetworkAbiSelfTest` to verify all five registrations, five packet handlers, two client-to-server sends, three server-to-client sends and all three packet-target constructions.
- Extended `ExternalAbiParityAudit` beyond return descriptors to reject unique external method argument-descriptor drift against the prior ForgeGradle-built artifact. The broken `SimpleChannel.send` shape is now detected generically before packaging.
- Kept network protocol 7 and all alpha.7 execution, admission, performance and logout behavior unchanged.

## 0.2.0-alpha.7.5

- Fixed the server-thread player-tick crash captured both without Pig2 in `错误报告-2026-8-10_2.38.58.zip` and with Pig2 in `错误报告-2026-8-10_2.41.53.zip`: `DominionLedger.setDirty()` retained its development name instead of inherited `SavedData.m_77762_()`.
- Made `OfflineReobfuscator` always load the hierarchy of the application classes it is transforming, in addition to optional Minecraft/Forge hierarchy roots. Application-owned subclasses can now resolve inherited mapped members without per-class exceptions.
- Added `InheritedMappedMemberSelfTest`, which verifies all five production ledger dirty markers use `m_77762_` and rejects any leaked `setDirty` call.
- Kept network protocol 7 and all alpha.7 execution, admission, performance and logout behavior unchanged.

## 0.2.0-alpha.7.4

- Fixed both in-world client crashes captured in `错误报告-2026-8-10_2.27.38.zip` and `错误报告-2026-8-10_2.29.27.zip`: the tick and raw-mouse paths both called `Window.getWindow()` under its official-development name instead of the runtime `m_85439_` name.
- Reworked `OfflineReobfuscator` eligibility around the complete class namespace present in the 1.20.1 TSRG mapping set, with hierarchy-aware fallback, instead of hardcoding only `net/minecraft/` owners. This also corrected both latent `InputConstants.isKeyDown` call sites.
- Added `MappedNamespaceSelfTest`, which verifies both the `net.minecraft.client.Minecraft` and `com.mojang.blaze3d.platform.Window` call sites in the production client runtime.
- Kept network protocol 7 and all alpha.7 execution, admission, performance and logout behavior unchanged.

## 0.2.0-alpha.7.3

- Fixed the integrated-server command-registration crash captured in `错误报告-2026-8-10_2.16.23.zip`: the compile-only Brigadier stub declared `CommandDispatcher.register` as returning `Object`, while Brigadier 1.0.18 returns `LiteralCommandNode`.
- Corrected the complete command-registration call descriptor and added `BrigadierAbiSelfTest` against the production `DominionCommands.class`.
- Added `ExternalAbiParityAudit`, which compares shared external method-return and field descriptors across the prior ForgeGradle-built alpha.6 artifact and each offline release. Its first run also caught and corrected the latent `NetworkEvent.Context.enqueueWork` return descriptor before packet handling could trigger another `NoSuchMethodError`.
- Expanded `ForgeNetworkAbiSelfTest` to verify both all five channel registrations and all five packet-handler work submissions.
- Kept network protocol 7 and all alpha.7 execution, admission, performance and logout behavior unchanged.

## 0.2.0-alpha.7.2

- Fixed the Forge 47.4.22 construct-stage crash captured in `错误报告-2026-8-10_2.05.14.zip`: the offline `SimpleChannel` compile stub declared `registerMessage` with a `void` return, but the real method returns `IndexedMessageCodec.MessageHandler`.
- Corrected the compile-only Forge network ABI shape and rebuilt `RonovaNetwork`, so all five packet registrations now resolve to the descriptor used by the real Forge runtime.
- Added `ForgeNetworkAbiSelfTest`, which inspects production bytecode and rejects any future packet-registration return-descriptor drift.
- Kept network protocol 7 and all alpha.7 execution, admission, performance and logout behavior unchanged.

## 0.2.0-alpha.7.1

- Fixed the startup crash captured in `错误报告-2026-8-10_1.55.04.zip`: offline compilation had emitted `@Mixin` into `RuntimeVisibleAnnotations`, while Mixin 0.8.5 requires the CLASS-retained annotation in `RuntimeInvisibleAnnotations` during PREPARE.
- Corrected the compile-only `@Mixin` annotation stub to match the official Mixin retention contract and rebuilt every changed mixin class.
- Added `MixinMetadataSelfTest`, which verifies all 18 configured mixins carry exactly one invisible `@Mixin` annotation and no visible copy.
- Kept network protocol 7 and all alpha.7 execution, admission, performance and logout behavior unchanged.

## 0.2.0-alpha.7

- Replaced scattered admission decisions with one `SpawnFirewall` used by construction, NBT deserialization, `EntityType` factories, Forge join events, `ServerLevel`, the persistent entity manager and first tick.
- Normalized every non-player execution to a cross-dimensional global registry-type/runtime-class lineage rule; players remain UUID-exact so `/ronova clear_uuid` can recover them without weakening unrelated tombstones.
- Routed Mixin and Agent removal interception through the same reason-aware policy, preserving dimension changes, normal logout and server shutdown while retaining live-session hostile-removal repair.
- Added a lower `Connection` disconnect boundary and explicit shutdown state without retaining stale connections or server instances.
- Made terminal retries raw-state-only after the first public death integration, preventing repeated death callbacks and loot emission.
- Replaced repeated closure walks, per-target admission-cache clears and queue-size scans with one batched identity closure, one cache invalidation per action, an atomic retry count and a fixed per-tick budget.
- Added pure client screen/overlay reentrancy guards and session unbinding so repair cannot recursively call `setScreen`, and logout does not carry authority into the next session.
- Corrected all new production member references against the Forge 47.4.22/1.20.1 mapping set, including inherited owners and exact network/NBT JVM descriptors.
- Bumped the network protocol to 7; clients and servers must both run alpha.7. The embedded native DLL is unchanged from alpha.6; the updated Java transformer and bootstrap hook are packaged around it.

## 0.2.0-alpha.6

- Stopped transforming singular entity return values in third-party helpers. Forge event carriers and internal identity methods can no longer be replaced with `null`; only collection/iterable query producers are filtered.
- Made first-execution state independent of entity persistent data through a weak identity set, with NBT retained only as a best-effort marker.
- Split terminal execution into failure-isolated stages so one hostile damage, death, loot, removal or callback override cannot abort the remaining stages.
- Added construction observation and a final persistent-entity-manager admission gate, backed by UUID, registry type and runtime-class lineage tombstones, for generic replacement entities.
- Added live-server owner disconnect protection while preserving normal client socket close and server shutdown behavior.
- Added monotonic-time client recovery for death screens, overlays, released input and missing local-player topology; recovery no longer depends on game ticks progressing.
- Added generic client control/renderer transformer gates without third-party identifiers, while preserving unrelated entity/event identity carriers.
- Rebuilt the embedded Windows x64 JVMTI DLL with the expanded generic entity, renderer and client-control candidate prefilter.
- Bumped the network protocol to 6; clients and servers must both run alpha.6.

## 0.2.0-alpha.5

- Added terminal quarantine gates for `revive`, positive-health restoration and direct live-state field writes while preserving the inverse protections for bound owners.
- Added producer-side filtering for entity-aware third-party query helpers, so a whole-method query replacement cannot re-inject an already quarantined object after the Minecraft return filter.
- Added generic custom-renderer entry gates and detached-proxy lineage matching for entity visuals rendered outside normal world topology and Forge render events.
- Reasserted terminal state when area/loaded modes encounter an already judged object, without re-running damage, death or loot processing.
- Broadened the native ClassFileLoadHook candidate prefilter by generic entity and renderer bytecode signatures; no third-party Mod ID, registry name or class name is used.
- Added transformer regressions for revive, external query producers, custom renderers, collection-shape preservation and optional real-class fixture checks.
- Kept network protocol 5; alpha.4 and alpha.5 can handshake, but both sides should use alpha.5 for matching visual quarantine behavior.

## 0.2.0-alpha.4

- Replaced linear durable-tombstone and client-mark scans with a shared identity/spatial lineage index.
- Replaced global topology scans with weak level, UUID and entity-ID indexes; judged objects are removed immediately.
- Limited construction topology retention to living combatants and multipart pieces while retaining normal-query attacks against all current combat objects.
- Replaced the strong quarantined-object set with a weak identity set to prevent executed entities from being retained indefinitely.
- Added per-connection-session request IDs, bounded receipt journals and exponential retry backoff so stalled/disconnected requests cannot execute twice.
- Added standalone regression and 65,536-record stress checks for lineage lookup, weak identity semantics and session-journal eviction.
- Bumped the network protocol to 5; clients and servers must both run alpha.4.

## 0.2.0-alpha.3

- Fixed repeated loot explosions by separating first death execution from terminal-state retries.
- A successful lethal `hurt` path no longer receives a second explicit `die` call.
- Tombstone admission rejection and delayed reassertion never invoke death or loot processing.
- Removed duplicate full actions from server item swing/use callbacks; acknowledged raw-input packets remain authoritative.
- Kept targeted `hurtEnemy` as a compatibility fallback, guarded by the persistent first-execution marker.
- Excluded dropped item stacks and experience orbs from combat queries, the weak topology index, and legacy tombstone admission checks.

## 0.2.0-alpha.2

- Replaced game-time input deduplication with GLFW edge capture plus ordered request acknowledgements and retry, preventing a stalled integrated-server tick from silently losing attacks.
- Added idempotent server receipts and structured `RONOVA_ACTION` timing logs.
- Added generic query quarantine and direct lifecycle-field write guards without third-party Mod IDs, registry names, equipment names, or entity-class hardcoding.
- Added head/tail server recovery and raw synchronized-health repair for protected owners.
- Added explicit player combat-kill packets while preserving the same finalization chain used for all other living entities.
- Added `/ronova clear_uuid <UUID>` for exact admission recovery while retaining `/ronova clear_tombstones` for a full reset.
- Removed the virtual creature render path and its forced render-buffer flush.
- Reduced integrity watchdog work to the core lifecycle pair, raised the default fallback interval to 30 seconds, and kept query targets in the eager startup pass only.
- Bumped the network protocol to 4; clients and servers must both run alpha.2.

## 0.2.0-alpha.1

- Replaced reference-specific acceptance ideas with generic object identity, lifecycle, and topology authority.
- Added an independent weak entity index populated at construction time, allowing judgment to include objects missing from normal world queries.
- Added direct entity finalization, admission rejection, durable identity/lineage tombstones, and bounded delayed reassertion.
- Added owner recovery for removal/death fields, level callbacks, entity manager UUID state, tick lists, and server player lists.
- Added EventBus-independent MinecraftServer tick and ServerLevel admission Mixins.
- Froze weapon semantics at 64-block single-target, 64-block area, and current-dimension loaded-entity modes.
- Added edge-triggered authoritative network input and newly-exposed-block mining requests.
- Added owner-state synchronization, result HUD, particles, and an experimental client visual echo.
- Expanded the runtime control screen with visual modes and live capability/range status.
- Kept embedded Windows x64 JNI/JVMTI loading automatic and cooperative; no launcher `-javaagent` argument is required.

## 0.1.0-alpha.2

- Fixed a client-start `VerifyError` caused by recomputing unrelated `Entity` stack-map frames during premain transformation.
- Preserved ModLauncher-provided frames and added only the explicit guard-branch frame.
- Delayed integrity retransformation until Forge load completion, preventing the watchdog from racing EventBus class loading.
- Added a regression fixture matching the failing `Entity.m_280447_` merge-frame shape.
- Embedded a private bootstrap-support JAR so premain no longer depends on Forge exposing ASM early.
- Added no-argument automatic Agent attachment through a short-lived external `jdk.attach` helper.
- Added a bootstrap-loaded hook bridge so attached Agent state is shared across ModLauncher class loaders.
- Added an embedded Windows x64 JNI/JVMTI DLL with a real ClassFileLoadHook and bounded target retransformation watchdog.
- Native startup is automatic from the Forge Mod; no launcher `-javaagent` argument is required on supported systems.
- Kept external Attach and Mixin as explicit fallback layers when native JVMTI capabilities are unavailable.

## 0.1.0-alpha.1

- Added Ronova Blade with ray, area, and loaded-dimension judgment modes.
- Added persistent identity and lineage tombstones.
- Added bounded retry enforcement and multipart/passenger closure.
- Added owner continuity across damage, death, removal, clone, and inventory paths.
- Added Mixin lifecycle gates and optional early/late Java Agent gates.
- Added runtime sovereignty screen with actual Agent status.
- Added commands, Chinese/English localization, tests, and dedicated-server smoke verification.
