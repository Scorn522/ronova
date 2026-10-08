# Ronova MS 骨架重构映射

**fix12当前桥接：** [施工与验收分工映射](#fix12-acceptance-binding)。原19行和原来源映射保留，不扩编编号。

**fix11当前新增：** [已有基线逐节映射](#fix11-baseline-merge)；原MS三书映射与后续来源编号不改。

**当前增补映射：** [fix10八方向来源与落点](#fix10-eight-merge)。原16来源与旧编号映射保持。

**当前追加：fix8。** [核验补遗/天花板/检查器逐节映射](#fix8-verification-merge)，不新增规划编号。

> **当前融合映射：** [fix7 同权限清点](#fix7-surface-merge)；原16源迁移关系、此前增补来源与原编号不变。

> **历史增补：** [fix5 工作区增补融合](#fix5-addendum-merge) · [fix4 接线更新映射](#fix4-wiring-supplement)。原分段映射不变，来源原文与原 ID 保留。

日期：2026-09-14。本表只记录来源、归属、缺口与适用性；不记录本轮运行成功结论。

[规划入口](README.md) · [不变量索引](INVARIANT-INDEX.md) · [审阅修订](RESTRUCTURE-REVIEW.md) · [技能书](MS-SKILLS-PLAN.md) · [击杀物品书](MS-MSITEM-PLAN.md) · [实体防御书](MS-ENTITYDEFENCE-PLAN.md) · [文档核对](RESTRUCTURE-CHECK.md)

## 1. 范围与迁移方法

根 `docs/plans/` 15 份文件加原索引指向的 `docs/DESIGN-SPEC.md`，共 16 份。按代码围栏外的标题分段，保留每段全部正文，不拆表格、列表或示例。MS 三份原资料的 82 个二级/三级标题原样、原序成为三本书的章节骨架。

原文的 D*、INV*、A*、R*、ID-*、T-* 与其他编号不重编号。下面新出现的 GAP/NA/COND 只是映射记录标签，不是替换原不变量。原段落各有唯一内容落点；其他职责只添加链接，不复制一份竞争正文。

`docs/DESIGN-SPEC.md` 原来含已合并加强块，故完整迁入而非仅改入口。原 `.incoming-all-stages-v1.1/`、参考资料、历史记录和证据不擅自合并。源码包不修改。

## 2. 治理与进程边界的效力调整

[唯一硬边界](README.md#scope-hard-limits)是三条铁律与不起外部进程。[旧位次与不可覆盖规则](README.md#scope-governance)失去不可修改优先权；原句仍在各迁入位置。[旧 helper 允许语句](README.md#scope-process)保真保留但不授权起进程。其他工程合同本轮不暗改。

逐段提示清单：[scope-adjustments.json](restructure/scope-adjustments.json)。旧技术规则可显式修订，不等于本轮已删除或放开所有旧限制。

2026-09-14 审阅修订补充：[无参数与前置选型](README.md#scope-startup-parameters)、[DS 的 P 层 / C 块定级](README.md#scope-p-layer)、[跨文档不变量人读入口](INVARIANT-INDEX.md)。本次只补解释层、导航与派生索引，原章节映射、原 ID 和正文落点不变；逐项处理见[审阅说明](RESTRUCTURE-REVIEW.md)。

## 3. MS 有而原规划没有充分展开的内容

以下以输入规划文本为判断范围，不以关键词“未命中”推断源码不存在。明确区分文档细则缺少、已有提案但对接不足、已有独立规格；本轮只登记缺口，不虚构补齐或实现状态。

<a id="gap-doc-01"></a>

### GAP-DOC-01 · 物品自身的完整契约

**性质：** 原规划缺少完整细则。**归属：** [物品层只承担入口与表现](MS-MSITEM-PLAN.md#ms-msitem-l112).

**MS 来源：** [msitem:L24–L41](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#先定义击杀契约)；[msitem:L112–L124](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#物品层只承担入口与表现)。

**原规划依据：** [MSC:L69–L78](MS-MSITEM-PLAN.md#orig-msc-l0069)；[NON:L129–L144](MS-SKILLS-PLAN.md#orig-non-l0129)；[RULE:L175–L187](MS-MSITEM-PLAN.md#orig-rule-l0175)。

原文有八维契约、ItemPolicy、执行前手持复验与成本一次提交，但没有主手/副手、堆叠/修复/附魔、取消或失败的成本返还、槽位变更逐项决策表。

**待补内容：** 在本节补一张物品契约表，保留既有动作和配置 ID；未获来源支持的默认值留待明确，不自动设计新功能。

<a id="gap-doc-02"></a>

### GAP-DOC-02 · 扇形与范围几何的边界条件

**性质：** 原规划缺少完整细则。**归属：** [目标选择与快照](MS-MSITEM-PLAN.md#ms-msitem-l180).

**MS 来源：** [skills:L163–L183](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#扇形与范围选取)；[msitem:L180–L199](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#目标选择与快照)。

**原规划依据：** [NON:L129–L144](MS-SKILLS-PLAN.md#orig-non-l0129)；[V26:L1275–L1326](RESTRUCTURE-MAP.md#range-v26-1275-1326)。

旧 C11 提到补扇形选择器，现有范围条款未集中给出半角/总开角、水平重合零向量、180 度边界、射线遮挡、垂直高度与快照后复验的完整对照。

**待补内容：** 补选择器契约；不改变现有 64 格等数值，也不据关键词检索断言代码缺失。

<a id="gap-doc-03"></a>

### GAP-DOC-03 · MS 动作上下文到 Ronova 类型的逐字段映射

**性质：** 原规划缺少字段级对照。**归属：** [用统一动作上下文消除重复](MS-MSITEM-PLAN.md#ms-msitem-l142).

**MS 来源：** [msitem:L142–L179](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#用统一动作上下文消除重复)。

**原规划依据：** [MSC:L59–L68](MS-MSITEM-PLAN.md#orig-msc-l0059)；[NON:L129–L144](MS-SKILLS-PLAN.md#orig-non-l0129)；[CF:L325–L371](RESTRUCTURE-MAP.md#range-cf-325-371)。

原 R-C3/R-C4 已肯定部分调度/幂等设计，另有 C11 要补 budget 与 candidates；没有把 actor、item、world、trigger、request、candidates、budget 逐字段映射到现有类型、执行前复验者和短期去重淘汰规则。

**待补内容：** 补类型/字段/所有者/有效期/复验位置表；不新建第二套 RootCommandId 或 SubjectKey。

<a id="gap-doc-04"></a>

### GAP-DOC-04 · 三份 MS 的逐症状故障定位手册

**性质：** 原规划仅登记待办。**归属：** [故障定位](MS-SKILLS-PLAN.md#ms-skills-l464).

**MS 来源：** [skills:L464–L476](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#故障定位)；[msitem:L392–L404](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#故障定位)；[entitydefence:L300–L312](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#故障定位)。

**原规划依据：** [MSC:L177–L191](MS-SKILLS-PLAN.md#orig-msc-l0177)；[MSC:L243–L257](MS-SKILLS-PLAN.md#orig-msc-l0243)。

旧 R-S4 / M1 写明待建故障定位手册，原 15 份规划没有展开成 MS 三张表的完整“现象→Ronova 检查入口→日志字段→处置”对照；泛化 Telemetry/Audit 不能替代逐症状手册。

**待补内容：** 按三本书各自故障定位章节补充，不把原来源示例当成已观察到的 Ronova 故障。

<a id="gap-map-01"></a>

### GAP-MAP-01 · 动作七态与实体持久状态的正交映射

**性质：** 已有提案，跨模型对接未闭合。**归属：** [总体架构](MS-MSITEM-PLAN.md#ms-msitem-l43).

**MS 来源：** [msitem:L75–L87](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#总体架构)。

**原规划依据：** [MSC:L50–L58](MS-MSITEM-PLAN.md#orig-msc-l0050)；[MSC:L258–L268](MS-MSITEM-PLAN.md#orig-msc-l0258)；[P0A:L634–L649](MS-MSITEM-PLAN.md#orig-p0a-l0634)。

MS 七态与 LifecycleTruth / ResolutionState / AdmissionDisposition 的作用域不同。旧 M2 已要求映射而不新增真值，不能以“三态/两态不是七态”直接判已有身份合同错误。

**待补内容：** 补动作视图、保护优先级与生命周期状态转换对照；保留原 A*、INV*。

<a id="gap-map-02"></a>

### GAP-MAP-02 · 规范当前实例和修复前七项物理状态

**性质：** 已有缺口登记，实例级细化不足。**归属：** [保留一个规范实例](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l58).

**MS 来源：** [entitydefence:L58–L101](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#保留一个规范实例)。

**原规划依据：** [MSC:L108–L118](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0108)；[MSC:L269–L279](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0269)；[RULE:L538–L542](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0538)。

既有 SubjectKey/binding/rebind 合同很完整，但旧规划仍登记当前规范实例五条规则、A/B 同键对象替换与修复前七项状态的对接缺口；长期身份不等于当前注册实例。

**待补内容：** 按 SubjectKey 与当前 BindingRevision 细化，不把全项目缩成一个规范对象。

<a id="gap-map-03"></a>

### GAP-MAP-03 · ID/UUID 原子修复及 active/passive 保持

**性质：** 已有缺口登记，集合事务待细化。**归属：** [ID 与 UUID 查询](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l151).

**MS 来源：** [entitydefence:L145–L172](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#分区存储)。

**原规划依据：** [MSC:L119–L131](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119)；[MSC:L145–L152](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0145)。

R-J3 明确记录 ID 与 UUID 成员修复事务未明文化；active/passive 仅在内部集合总规则中汇总。Root JudgmentBatch 的逻辑原子性不是实体注册表事务已经实现的证明。

**待补内容：** 展开缺失→存在、陈旧对象替换、迭代安全和两表失败语义。

<a id="gap-map-04"></a>

### GAP-MAP-04 · 服务端追踪与客户端六路径独立修复

**性质：** 已有缺口登记，独立修复矩阵未闭合。**归属：** [玩家追踪](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l174).

**MS 来源：** [entitydefence:L132–L197](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复服务端注册表)。

**原规划依据：** [MSC:L119–L131](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119)；[MSC:L280–L293](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0280)。

旧 R-J1/R-J6/R-K1 对 ChunkMap 追踪器和客户端路径标为部分/缺口；不能把 ERASED 摘除或客户端显示恢复当成 PROTECTED 各表独立补回已完成。

**待补内容：** 将每表恢复来源、同实例条件、缺失/陈旧判定、追踪玩家与客户端收敛分别列出。

<a id="gap-map-05"></a>

### GAP-MAP-05 · 七族读取兜底与六类修复触发点

**性质：** 已有缺口登记，时序覆盖未闭合。**归属：** [防御读取路径](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l199).

**MS 来源：** [entitydefence:L103–L130](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#构建幂等修复过程)；[entitydefence:L199–L213](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#防御读取路径)；[entitydefence:L238–L252](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#实体状态也必须防御)。

**原规划依据：** [MSC:L97–L107](MS-SKILLS-PLAN.md#orig-msc-l0097)；[MSC:L132–L144](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0132)；[RULE:L538–L542](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0538)。

旧 R-L1/R-M4/R-M5/R-G6 已登记读写 Hook 与 tick 后修复等差距；原读侧终结过滤方向与受保护存在兜底不同。

**待补内容：** 逐查询族列读取间隙、写 Hook、独立 tick 与双侧时序，不让 Public Presence 反向污染 Raw Observation。

<a id="gap-map-06"></a>

### GAP-MAP-06 · 独占列表、终结过滤与瞬态抑制的边界

**性质：** 原稿判断有修正，需保留差异。**归属：** [可选：实体列表独占防御](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l215).

**MS 来源：** [entitydefence:L25–L40](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#划定策略范围)；[entitydefence:L215–L236](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#可选实体列表独占防御)。

**原规划依据：** [MSC:L153–L160](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0153)；[RULE:L512–L537](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0512)。

旧 R-O/C3 曾把已有过滤判为独占；后续 N8 明确纠正两者不同。两份原文同时保留，不能采纳较早结论冒充当前实现。五项副作用与显式策略说明仍是待细化内容。

**待补内容：** 先声明选定路径、保护对象、过滤对象与开关含义；本轮不自动启用独占。

<a id="gap-map-07"></a>

### GAP-MAP-07 · 结果可见性、双客户端矩阵与原验收账本对接

**性质：** 已有来源清单，逐项落点未闭合。**归属：** [构建、打包与验证](MS-SKILLS-PLAN.md#ms-skills-l436).

**MS 来源：** [skills:L43–L54](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#秒杀是什么)；[msitem:L372–L390](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#验证矩阵)；[entitydefence:L282–L298](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#验证矩阵)。

**原规划依据：** [MSC:L177–L191](MS-SKILLS-PLAN.md#orig-msc-l0177)；[MSC:L243–L257](MS-SKILLS-PLAN.md#orig-msc-l0243)；[RULE:L224–L234](MS-SKILLS-PLAN.md#orig-rule-l0224)。

旧 R-S2/S3 和 M1 已要求六项可见结果及两组各 11 项矩阵，但来源规划未完整逐项绑定到 Ronova 的场景、动作/不变量 ID 与证据入口；状态不能由重构补成成功。

**待补内容：** 做适用目标/触发路径/原验收 ID/证据缺项表；保留未执行、部分覆盖与范围限制。

**fix2 对接记录（不关闭本缺口）：** [验收规格双向映射](VALIDATION-TRACEABILITY.md#ms-checks)已列 68 项 B/X 规格、限定来源的 D/C/GAP 关系及三份 MS 的 28 个来源检查条目；设计子范围与仅邻近/逆向关系分开。输入包未提供 `validation/7.17/acceptance-matrix.json`，尚未读取逐项状态或绑定实际 artifact / fixture / 两个客户端的运行证据；原进度汇总不能替代原台账。原条款、验收状态及本缺口待补范围不改。

**fix3 当前补记（来源：用户工作区核查回报）：** 用户已对真实 `validation/7.17/acceptance-matrix.json` 只读核对，68 个唯一 B/X ID、缺失/额外/重复均为 0，汇总为 28 PARTIAL / 40 NOT_RUN，并报告未写入台账。**台账 ID 集合与汇总接线子项据回报已核；原文中的“输入包未提供”只描述 fix2 当时收到的压缩包，不表示完整工作区没有文件。** 本交付未独立读取该账本，未取得 68 行明细或原文件哈希，不新增逐项状态。artifact / fixture / 断言 / 双客户端证据绑定仍未完成，人工映射仍需语义审阅，**本缺口总体保持开放**。见[已做与未做](VALIDATION-TRACEABILITY.md#ledger-bind)及[原命令与回报摘录](RESTRUCTURE-REVIEW.md#review-fix3-ledger)。

### 已有独立规格，不能误报为“完全没有”

| 主题 | MS 来源 | 原规划 | 本轮判断范围 |
|---|---|---|---|
| 七字段 Hook 契约 | [msitem:L313–L327](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#hook-分类) | [MSR:L153–L233](RESTRUCTURE-MAP.md#range-msr-153-233) | 旧 S3 已有目标形态与判据；不能写成完全没有设计，源码落实未在本轮复核。 |
| 重转换与重定义队列 | [skills:L338–L348](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#重转换与重定义) | [MSR:L94–L152](RESTRUCTURE-MAP.md#range-msr-94-152) | 旧 S2 已有 RedefinitionQueue 与判据；迁移保留，不重复发明队列。 |
| Native 桥接/规则定位 | [skills:L350–L374](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#jnajnijvmti-native-层) | [MSR:L37–L93](RESTRUCTURE-MAP.md#range-msr-37-93) | 旧 S1 及 Native 合同已覆盖；是否采用某一 Java/Native 分工属于可修订项目设计。 |
| 原生路径单一来源 | [skills:L404–L411](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#native-实现规则) | [MSR:L234–L301](RESTRUCTURE-MAP.md#range-msr-234-301) | 旧 S4 已有单一来源文件提案；本轮不修改构建。 |
| 启动前提与 ping | [skills:L376–L389](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#启动顺序对于modmain优先级且对平台有限制的秒杀mod) | [MSR:L302–L345](RESTRUCTURE-MAP.md#range-msr-302-345) | 旧 S5 已有步骤；没有补做启动或平台验证。 |
| 失败策略显式化 | [msitem:L319–L327](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#hook-分类) | [MSR:L346–L387](RESTRUCTURE-MAP.md#range-msr-346-387) | 旧 S6 已有步骤与判据；不把提案写成已接线。 |
| 原生探针与打包收口 | [skills:L436–L462](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#构建打包与验证) | [RULE:L543–L549](MS-SKILLS-PLAN.md#orig-rule-l0543) | 旧 N10 / C10 已登记；保持原记录而不是宣布 Native 构建通过。 |

## 4. 明确不适用与条件适用

### 明确不适用

| 标签 | 性质 | 来源章节/分支 | 处理 |
|---|---|---|---|
| NA-01 | 不作为工程规范 | skills / 秒杀是什么 | 圈内评价、人物评价、T0/T1 社会性排名及“完美击败”口号，不作为 Ronova 的新技术等级事实。保留 MS 原文件及该章节入口。 |
| NA-02 | 平台范围不适用 | skills、msitem / 开篇版本范围 | Fabric、Paper、Spigot、NeoForge 1.21+ 及其他版本 API 不替换 Forge 1.20.1 的当前条款。不是删除 Ronova 能力，而是不混用生命周期假设。 |
| NA-03 | 违反本次明确边界 | skills / 对于非MoMain优先级 | 任何以修改依赖/Mod/Forge 文件来获得更早入口的方式不纳入可实施路线；原 MS 也把修改 Forge 文件记为违规。 |
| NA-04 | 违反本次明确边界 | 原 L6 / Baseline / CF / P0-A 的 helper 与外部重启分支 | 旧 D13、PROC-INV-1、R7、A8、ID-9、T-16 等允许 bounded helper 的文字原样保留，但其中“起外部进程”的授权失效；重启恢复只能描述用户/现有宿主后来重启后的恢复语义，不能据此启动进程。 |

### 条件适用：不能误删整章

| 标签 | 来源章节 | 适用方式 |
|---|---|---|
| COND-01 | msitem / LivingEntity 范围 | 仅适用于该 MS 击杀物品职责切面，不能把 Ronova 已有非生物、投射物、掉落物、方块实体、非实体资源等条款删除或缩成 LivingEntity-only。原条款全部迁入各职责章。 |
| COND-02 | entitydefence / 一个规范实例 | 适用于一个被保护逻辑对象的当前有效实例；不能推导为全 JVM / 全服务端只能有一个受保护 Subject。显式 rebind、不同 Subject 和客户端对应实例的原合同保留。 |
| COND-03 | entitydefence / 可选实体列表独占防御 | 是可选策略，不是基础防御完成的强制前提。保留独立章节、原缺口和后果声明，不自动打开、不因不用就判铁律违规。 |
| COND-04 | skills / 血量与死亡界面可见结果 | 玩家死亡界面只对存在对应客户端/玩家场景的用例适用；一般非玩家实体没有这类 UI。不能用单个不适用格子宣布整个能力通过或整章不适用。 |
| COND-05 | skills / 平台 Native 启动与最小 Hook 示例 | Windows x64、Java 17、特定 JDK 布局、零参数 getter、批次示例均按原适用前提处理；不导出“所有方案必须如此”或“同 JVM Native 被禁止”。 |
| COND-06 | skills / Premain 与启动参数示例 | “无参数”不是本次硬约束或额外必选完成条件，保留为默认取向而非违规判据；允许参数不等于要求参数。DS / RULE 加强 A5 与 RULE I7 的必要性按选中路线确定，不自动取消其功能依赖；旧“唯一前置入口”不排除等价方案。没有整体禁止 Agent、Instrumentation、JNI 或 JVMTI，但任何入口均不得起外部进程或改受禁文件。 见[启动参数选型](README.md#scope-startup-parameters)。 |

## 5. MS 逐章骨架覆盖与反向索引

“迁入段数”只是原文归属计数，**不是功能完成率**。0 表示没有单独切出的原段，不表示该 MS 条款被宣布不适用；可能是保留在另一章的整张原表，通过关联链接承接。

| MS 来源原章 | 新位置 | 直接迁入段数 | 关联原段 | 文档缺口/适用性 |
|---|---|---:|---|---|
| skills · 秒杀是什么（L14） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l14) | 0 | [V26:L214](MS-SKILLS-PLAN.md#orig-v26-l0214)；[V26:L325](MS-MSITEM-PLAN.md#orig-v26-l0325) | 按来源适用；实现未复核 |
| skills · 先确定能力边界（L68） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l68) | 113 | — | 按来源适用；实现未复核 |
| skills · 总体架构（L84） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l84) | 244 | — | 按来源适用；实现未复核 |
| skills · 秒杀的三种执行模式（L112） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l112) | 0 | [BL:L874](MS-MSITEM-PLAN.md#orig-bl-l0874)；[REC:L1](MS-SKILLS-PLAN.md#orig-rec-l0001) | 按来源适用；实现未复核 |
| skills · 1. 标准死亡（L114） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l114) | 0 | [MSC:L69](MS-MSITEM-PLAN.md#orig-msc-l0069)；[BL:L1125](MS-MSITEM-PLAN.md#orig-bl-l1125) | 按来源适用；实现未复核 |
| skills · 2. 实体清除（L125） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l125) | 0 | [BL:L1123](MS-MSITEM-PLAN.md#orig-bl-l1123)；[V26:L1506](MS-MSITEM-PLAN.md#orig-v26-l1506) | 按来源适用；实现未复核 |
| skills · 3. 混合模式（L144） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l144) | 0 | [MSC:L69](MS-MSITEM-PLAN.md#orig-msc-l0069)；[CF:L1011](MS-MSITEM-PLAN.md#orig-cf-l1011) | 按来源适用；实现未复核 |
| skills · 扇形与范围选取（L163） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l163) | 0 | [NON:L129](MS-SKILLS-PLAN.md#orig-non-l0129)；[V26:L1275](MS-MSITEM-PLAN.md#orig-v26-l1275) | 按来源适用；实现未复核 |
| skills · 线程、逻辑边与生命周期（L185） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l185) | 1 | [P0A:L1500](MS-SKILLS-PLAN.md#orig-p0a-l1500) | 按来源适用；实现未复核 |
| skills · 逻辑服务器（L187） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l187) | 3 | — | 按来源适用；实现未复核 |
| skills · 客户端（L204） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l204) | 11 | — | 按来源适用；实现未复核 |
| skills · 追踪和重载（L214） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l214) | 1 | — | 按来源适用；实现未复核 |
| skills · 防御链（L222） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l222) | 27 | — | 按来源适用；实现未复核 |
| skills · 1. 统一分类（L226） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l226) | 0 | [MSC:L97](MS-SKILLS-PLAN.md#orig-msc-l0097)；[CF:L140](MS-SKILLS-PLAN.md#orig-cf-l0140) | 按来源适用；实现未复核 |
| skills · 2. Forge 事件层（L237） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l237) | 0 | [MSC:L97](MS-SKILLS-PLAN.md#orig-msc-l0097) | 按来源适用；实现未复核 |
| skills · 3. 实体与生物方法层（L241） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l241) | 0 | [MSC:L97](MS-SKILLS-PLAN.md#orig-msc-l0097)；[BL:L2242](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l2242) | 按来源适用；实现未复核 |
| skills · 4. `SynchedEntityData` 层（L257） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l257) | 0 | [MSC:L97](MS-SKILLS-PLAN.md#orig-msc-l0097)；[RULE:L198](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0198) | 按来源适用；实现未复核 |
| skills · 5. 周期修复层（L267） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l267) | 1 | — | 按来源适用；实现未复核 |
| skills · 自定义Mixin或转换器（L273） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l273) | 44 | — | 按来源适用；实现未复核 |
| skills · 注入声明（L281） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l281) | 9 | — | 按来源适用；实现未复核 |
| skills · 实现一个简单 Hook（L309） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l309) | 3 | — | 按来源适用；实现未复核 |
| skills · 转换器注册与顺序（L326） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l326) | 0 | [MSC:L161](MS-SKILLS-PLAN.md#orig-msc-l0161)；[MSR:L94](MS-SKILLS-PLAN.md#orig-msr-l0094) | 按来源适用；实现未复核 |
| skills · 重转换与重定义（L338） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l338) | 6 | — | 按来源适用；实现未复核 |
| skills · JNA/JNI/JVMTI Native 层（L350） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l350) | 42 | — | 按来源适用；实现未复核 |
| skills · 命令桥接契约（L360） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l360) | 1 | [MSC:L161](MS-SKILLS-PLAN.md#orig-msc-l0161) | 按来源适用；实现未复核 |
| skills · 启动顺序(对于ModMain优先级且对平台有限制的秒杀Mod)（L376） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l376) | 4 | — | 按来源适用；实现未复核 |
| skills · 对于非MoMain优先级（L391） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l391) | 7 | — | 按来源适用；实现未复核 |
| skills · Native 实现规则（L404） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l404) | 5 | — | 按来源适用；实现未复核 |
| skills · 新增功能的实现清单（L413） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l413) | 90 | — | 按来源适用；实现未复核 |
| skills · 新武器或主动技能（L415） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l415) | 0 | [TIER:L255](MS-SKILLS-PLAN.md#orig-tier-l0255)；[REC:L317](MS-MSITEM-PLAN.md#orig-rec-l0317) | 按来源适用；实现未复核 |
| skills · 新防御规则（L427） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l427) | 0 | [MSC:L269](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0269)；[MSC:L280](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0280)；[MSC:L294](MS-SKILLS-PLAN.md#orig-msc-l0294) | 按来源适用；实现未复核 |
| skills · 构建、打包与验证（L436） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l436) | 81 | — | [GAP-MAP-07](#gap-map-07) |
| skills · 故障定位（L464） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l464) | 21 | — | [GAP-DOC-04](#gap-doc-04) |
| skills · 完成标准(以一个标准的，带Native的秒杀Mod举例)（L478） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l478) | 17 | — | 按来源适用；实现未复核 |
| skills · 总而言之（L484） | [MS-SKILLS-PLAN.md](MS-SKILLS-PLAN.md#ms-skills-l484) | 0 | [V26:L3391](MS-SKILLS-PLAN.md#orig-v26-l3391) | 按来源适用；实现未复核 |
| msitem · 先定义击杀契约（L24） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l24) | 6 | — | 按来源适用；实现未复核 |
| msitem · 总体架构（L43） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l43) | 5 | — | [GAP-MAP-01](#gap-map-01) |
| msitem · 建立职责边界（L89） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l89) | 1 | — | 按来源适用；实现未复核 |
| msitem · 物品层只承担入口与表现（L112） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l112) | 0 | [MSC:L69](MS-MSITEM-PLAN.md#orig-msc-l0069)；[NON:L129](MS-SKILLS-PLAN.md#orig-non-l0129)；[RULE:L175](MS-MSITEM-PLAN.md#orig-rule-l0175) | [GAP-DOC-01](#gap-doc-01) |
| msitem · 列出全部击杀入口（L126） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l126) | 6 | — | 按来源适用；实现未复核 |
| msitem · 用统一动作上下文消除重复（L142） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l142) | 3 | — | [GAP-DOC-03](#gap-doc-03) |
| msitem · 去重与幂等（L168） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l168) | 12 | — | 按来源适用；实现未复核 |
| msitem · 目标选择与快照（L180） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l180) | 21 | — | [GAP-DOC-02](#gap-doc-02) |
| msitem · 统一目标、结果和防御策略（L201） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l201) | 51 | — | 按来源适用；实现未复核 |
| msitem · 正常死亡、运行时清除与混合执行（L224） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l224) | 12 | — | 按来源适用；实现未复核 |
| msitem · 正常死亡（L226） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l226) | 0 | [MSC:L69](MS-MSITEM-PLAN.md#orig-msc-l0069)；[BL:L1125](MS-MSITEM-PLAN.md#orig-bl-l1125)；[CF:L1011](MS-MSITEM-PLAN.md#orig-cf-l1011) | 按来源适用；实现未复核 |
| msitem · 运行时清除（L238） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l238) | 29 | — | 按来源适用；实现未复核 |
| msitem · 混合模式（L255） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l255) | 0 | [MSC:L69](MS-MSITEM-PLAN.md#orig-msc-l0069)；[CF:L1011](MS-MSITEM-PLAN.md#orig-cf-l1011) | 按来源适用；实现未复核 |
| msitem · 服务端队列与线程边界（L261） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l261) | 14 | — | 按来源适用；实现未复核 |
| msitem · Forge 事件、Mixin 与自定义转换器（L286） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l286) | 0 | [MSR:L153](MS-SKILLS-PLAN.md#orig-msr-l0153)；[MSC:L161](MS-SKILLS-PLAN.md#orig-msc-l0161) | 按来源适用；实现未复核 |
| msitem · 事件层（L290） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l290) | 0 | [MSC:L59](MS-MSITEM-PLAN.md#orig-msc-l0059)；[MSC:L97](MS-SKILLS-PLAN.md#orig-msc-l0097) | 按来源适用；实现未复核 |
| msitem · Hook 分类（L301） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l301) | 0 | [MSR:L153](MS-SKILLS-PLAN.md#orig-msr-l0153)；[RULE:L215](MS-SKILLS-PLAN.md#orig-rule-l0215) | 按来源适用；实现未复核 |
| msitem · 服务端与客户端一致性（L329） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l329) | 2 | — | 按来源适用；实现未复核 |
| msitem · 客户端表现也是物品的一部分（L345） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l345) | 8 | — | 按来源适用；实现未复核 |
| msitem · 从零实现顺序（L358） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l358) | 1 | — | 按来源适用；实现未复核 |
| msitem · 验证矩阵（L372） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l372) | 5 | — | 按来源适用；实现未复核 |
| msitem · 故障定位（L392） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l392) | 0 | [MSC:L243](MS-SKILLS-PLAN.md#orig-msc-l0243)；[MSC:L177](MS-SKILLS-PLAN.md#orig-msc-l0177) | 按来源适用；实现未复核 |
| msitem · 完成标准（L406） | [MS-MSITEM-PLAN.md](MS-MSITEM-PLAN.md#ms-msitem-l406) | 1 | — | 按来源适用；实现未复核 |
| entitydefence · 先定义防御契约（L14） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l14) | 0 | [MSC:L108](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0108)；[CF:L1085](MS-SKILLS-PLAN.md#orig-cf-l1085) | 按来源适用；实现未复核 |
| entitydefence · 划定策略范围（L25） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l25) | 0 | [CF:L213](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0213)；[P0A:L462](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0462)；[MSC:L108](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0108) | 按来源适用；实现未复核 |
| entitydefence · 为什么只覆写实体方法不够（L42） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l42) | 3 | — | 按来源适用；实现未复核 |
| entitydefence · 保留一个规范实例（L58） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l58) | 13 | — | [GAP-MAP-02](#gap-map-02) |
| entitydefence · 规范实例规则（L62） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l62) | 31 | — | 按来源适用；实现未复核 |
| entitydefence · 修复前的状态不变量（L72） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l72) | 2 | [RULE:L198](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0198)；[MSC:L108](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0108) | 按来源适用；实现未复核 |
| entitydefence · 通过正常生命周期初始化（L88） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l88) | 0 | [MSC:L108](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0108)；[MSC:L269](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0269) | 按来源适用；实现未复核 |
| entitydefence · 构建幂等修复过程（L103） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l103) | 15 | — | 按来源适用；实现未复核 |
| entitydefence · 修复时机（L117） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l117) | 1 | [MSC:L132](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0132) | 按来源适用；实现未复核 |
| entitydefence · 修复服务端注册表（L132） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l132) | 2 | — | 按来源适用；实现未复核 |
| entitydefence · 分区存储（L145） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l145) | 0 | [MSC:L119](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119)；[MSC:L145](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0145) | 按来源适用；实现未复核 |
| entitydefence · ID 与 UUID 查询（L151） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l151) | 0 | [MSC:L119](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119)；[P0A:L462](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0462) | [GAP-MAP-03](#gap-map-03) |
| entitydefence · Tick 集合（L157） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l157) | 0 | [MSC:L145](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0145)；[MSC:L280](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0280) | 按来源适用；实现未复核 |
| entitydefence · 持久管理与生命周期（L168） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l168) | 5 | — | 按来源适用；实现未复核 |
| entitydefence · 玩家追踪（L174） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l174) | 0 | [MSC:L119](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119)；[MSC:L280](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0280) | [GAP-MAP-04](#gap-map-04) |
| entitydefence · 修复客户端注册表（L184） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l184) | 1 | [MSC:L119](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119) | 按来源适用；实现未复核 |
| entitydefence · 防御读取路径（L199） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l199) | 8 | — | [GAP-MAP-05](#gap-map-05) |
| entitydefence · 可选：实体列表独占防御（L215） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l215) | 2 | — | [GAP-MAP-06](#gap-map-06) |
| entitydefence · 实体状态也必须防御（L238） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l238) | 5 | — | 按来源适用；实现未复核 |
| entitydefence · 从零实现顺序（L254） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l254) | 1 | — | 按来源适用；实现未复核 |
| entitydefence · 内部集合规则（L268） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l268) | 1 | — | 按来源适用；实现未复核 |
| entitydefence · 验证矩阵（L282） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l282) | 38 | — | 按来源适用；实现未复核 |
| entitydefence · 故障定位（L300） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l300) | 0 | [MSC:L243](MS-SKILLS-PLAN.md#orig-msc-l0243)；[MSC:L177](MS-SKILLS-PLAN.md#orig-msc-l0177) | 按来源适用；实现未复核 |
| entitydefence · 完成标准（L314） | [MS-ENTITYDEFENCE-PLAN.md](MS-ENTITYDEFENCE-PLAN.md#ms-entitydefence-l314) | 11 | — | 按来源适用；实现未复核 |

## 6. 原文件总表

| 原缩写 | 原路径 | 原字节数 | 原 SHA-256 | 原段数 | 全量章节映射 |
|---|---|---:|---|---:|---|
| INDEX | `docs/plans/README.md` | 5010 | `aa6d2bf1a0835722823d1e8314210e8504701afa9c5bd4b39517405927f802a1` | 5 | [查看](#source-index) |
| BL | `docs/plans/Ronova_7.14A_Baseline_All_Enhancements_Master_V1.4_Internal_Root.md` | 73679 | `cac45dcc62e38bb90a26661464272801462ee0b3482b894bedc55d8d419fe523` | 144 | [查看](#source-bl) |
| V26 | `docs/plans/Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md` | 207866 | `905ff0b353fce4764805ae1075259a2dba77904f9468e88768ccb05998efcf37` | 171 | [查看](#source-v26) |
| FULL | `docs/plans/Ronova_7.17_Extreme_Full_Plane_Enhancement_Plan_V1.0_Internal_Root.md` | 36800 | `62d45c727873273dfec65a39e6c846311898eefe8b5c10f268a203af7421b90a` | 22 | [查看](#source-full) |
| TIER | `docs/plans/Ronova_7.17_Extreme_Tier_Strength_Implementation_Plan_V1.0_Internal_Root.md` | 46583 | `028e5849c3741f7f29abb191dce6eecb9f8b8631614584329fd50df2fc522855` | 53 | [查看](#source-tier) |
| REC | `docs/plans/Ronova_7.17_Generic_Recovery_Chain_Implementation_Plan_V1.0_Internal_Root.md` | 35947 | `85c993b68fb0cdd4647483fb9da937ce9b2cd95f10a7d9a6a54e6f8d1bfa62a1` | 25 | [查看](#source-rec) |
| MAX | `docs/plans/Ronova_Compliant_Maximum_Strength_Plan_V1.0.md` | 22184 | `00af477883318e2ce5500178c4dc3d48867e980bba773bffa184d26b4617005f` | 38 | [查看](#source-max) |
| CF | `docs/plans/Ronova_L6E-0_Contract_Freeze_V1.3_Internal_Root.md` | 42431 | `b09cbc535fbc0d6ec1f8d72dc90a5f9e67fa13a49b57c6d3b65975c4811d9d2b` | 95 | [查看](#source-cf) |
| P0A | `docs/plans/Ronova_L6E_P0-A_Truth_Authority_Implementation_Spec_V1.3_Internal_Root.md` | 55825 | `a0a70e129f1c053bba8f6b6aec1f1947590fe51d5703550e53a137c96a0149ca` | 139 | [查看](#source-p0a) |
| L6 | `docs/plans/Ronova_L6_Architecture_Master_Plan_V2.4_Internal_Root.md` | 134413 | `16100e2fc2779ee2cb29b784d3201c4f76e0a0cbd58fbe3da720c070d7edf278` | 100 | [查看](#source-l6) |
| ROAD | `docs/plans/Ronova_L6_L6E_Full_Enhancement_Roadmap_V1.0_Internal_Root.md` | 56571 | `6a4c2cf0e620f3eb2646f958ae6b318c4471db2f5e8a638fd75079c50fd830a4` | 35 | [查看](#source-road) |
| MSC | `docs/plans/Ronova_MS_Compliance_and_Enhancement_Plan_V1.0.md` | 28438 | `8f190feed151a5928c0dc35f4f9e968ad3f7cf751768cd875d8488223100c409` | 30 | [查看](#source-msc) |
| MSR | `docs/plans/Ronova_MS_Remediation_Spec_V1.0.md` | 24760 | `d855674727e223ae56f01c4649c558ff97a6d2afed695cac8a83ce72e23e7e57` | 28 | [查看](#source-msr) |
| RULE | `docs/plans/Ronova_MS_Rulebound_Enhancement_Plan_V2.0.md` | 86398 | `caa8968235fa8a2e3b04503a1c080c136feaada93311a8485cb0243b7629d4d1` | 63 | [查看](#source-rule) |
| NON | `docs/plans/Ronova_NonViolating_Enhancement_Plan_V1.0.md` | 20772 | `43a2de15b78ca16375dca65cd1f6d05735a1672d1337dd346d5c2ffd9cfd9c29` | 19 | [查看](#source-non) |
| DS | `docs/DESIGN-SPEC.md` | 91080 | `9d0768c351aa3a1d4bda7865c93303def851cfd7370afdc37442bcf544dda9e7` | 80 | [查看](#source-ds) |

## 7. 全量旧章节 → 新章节映射

所有旧标题均列出；“旧行范围”包括该标题后的正文，直到下一个代码围栏外标题之前。原标题路径保存在机器映射的 `ancestors` 中，新书段首也完整显示。旧行号始终对原始快照，不是新书行号。

<a id="source-index"></a>

### INDEX · README.md

原路径：`docs/plans/README.md`。 [原始快照](../archive/plans-before-ms-restructure-r66/README.md)。

| 映射锚点 | 旧标题（原编号不变） | 旧行范围 | 新 MS 章节 | 新正文落点 |
|---|---|---|---|---|
| <a id="map-orig-index-l0001"></a>INDEX:1 | 有效规划 | L1–L6 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-index-l0001) |
| <a id="map-orig-index-l0007"></a>INDEX:2 | 当前优先工作 | L7–L26 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-index-l0007) |
| <a id="map-orig-index-l0027"></a>INDEX:3 | 唯一规范顺序 | L27–L39 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-index-l0027) |
| <a id="map-orig-index-l0040"></a>INDEX:4 | 后续阶段 | L40–L53 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-index-l0040) |
| <a id="map-orig-index-l0054"></a>INDEX:5 | 参考与历史 | L54–L63 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-index-l0054) |

<a id="source-bl"></a>

### BL · Ronova_7.14A_Baseline_All_Enhancements_Master_V1.4_Internal_Root.md

原路径：`docs/plans/Ronova_7.14A_Baseline_All_Enhancements_Master_V1.4_Internal_Root.md`。 [原始快照](../archive/plans-before-ms-restructure-r66/Ronova_7.14A_Baseline_All_Enhancements_Master_V1.4_Internal_Root.md)。

| 映射锚点 | 旧标题（原编号不变） | 旧行范围 | 新 MS 章节 | 新正文落点 |
|---|---|---|---|---|
| <a id="map-orig-bl-l0001"></a>BL:1 | Ronova — 以当前源码链为基准的全量加强总纲 V1.4 — Internal Root | L1–L24 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0001) |
| <a id="map-orig-bl-l0025"></a>BL:2 | 0. 文档目的 | L25–L47 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0025) |
| <a id="map-orig-bl-l0048"></a>BL:3 | 1. 7.14A 冻结基准、r27 历史输入与 r50 当前源码 | L48–L49 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0048) |
| <a id="map-orig-bl-l0050"></a>BL:4 | 1.1 7.14A frozen baseline | L50–L89 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0050) |
| <a id="map-orig-bl-l0090"></a>BL:5 | 1.2 r50 当前 working source（源码事实） | L90–L100 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0090) |
| <a id="map-orig-bl-l0101"></a>BL:6 | 1.2.1 当前运行时进程边界（r27 源码确认） | L101–L120 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0101) |
| <a id="map-orig-bl-l0121"></a>BL:7 | 1.3 7.14A 冻结范围明确不包含 | L121–L138 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0121) |
| <a id="map-orig-bl-l0139"></a>BL:8 | 1.4 冻结基准中的 Terminal 行为仍然是有限 retry | L139–L164 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0139) |
| <a id="map-orig-bl-l0165"></a>BL:9 | 1.5 冻结基准中的 Passive TerminalWatch 诊断面状态 | L165–L178 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0165) |
| <a id="map-orig-bl-l0179"></a>BL:10 | 2. 后续所有加强必须遵守的硬边界 | L179–L180 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0179) |
| <a id="map-orig-bl-l0181"></a>BL:11 | 2.1 Raw Observation 与 Presence 永久单向分离 | L181–L208 | entitydefence / 防御读取路径 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l0181) |
| <a id="map-orig-bl-l0209"></a>BL:12 | 2.2 单一 Authority 真值源 | L209–L237 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0209) |
| <a id="map-orig-bl-l0238"></a>BL:13 | 2.3 强度不能来自 Hook 数量 | L238–L267 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0238) |
| <a id="map-orig-bl-l0268"></a>BL:14 | 2.4 不做逐 Mod / Boss 特判架构 | L268–L295 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0268) |
| <a id="map-orig-bl-l0296"></a>BL:15 | 2.5 Fail-static | L296–L315 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0296) |
| <a id="map-orig-bl-l0316"></a>BL:16 | 2.6 每个阶段只迁移一个清晰 Authority 边界 | L316–L331 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0316) |
| <a id="map-orig-bl-l0332"></a>BL:17 | 3. MC 实际威胁模型 | L332–L372 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0332) |
| <a id="map-orig-bl-l0373"></a>BL:18 | 4. L6 Enhanced 最终四条 P0 大链 | L373–L412 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0373) |
| <a id="map-orig-bl-l0413"></a>BL:19 | 4.1 四条 P0 链不是并列开发 | L413–L452 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0413) |
| <a id="map-orig-bl-l0453"></a>BL:20 | 4.2 推荐的 Enhanced 实现顺序 | L453–L499 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0453) |
| <a id="map-orig-bl-l0500"></a>BL:21 | 4.3 四链共同的不变量 | L500–L517 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0500) |
| <a id="map-orig-bl-l0518"></a>BL:22 | 5. Authority Policy 加强 | L518–L519 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-bl-l0518) |
| <a id="map-orig-bl-l0520"></a>BL:23 | 5.1 目标 | L520–L550 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-bl-l0520) |
| <a id="map-orig-bl-l0551"></a>BL:24 | 5.2 原则 | L551–L570 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-bl-l0551) |
| <a id="map-orig-bl-l0571"></a>BL:25 | 5.3 Root 驱动的确定性 Threat Posture / Policy Overlay | L571–L605 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-bl-l0571) |
| <a id="map-orig-bl-l0606"></a>BL:26 | 5.4 Minecraft Tag 只作为高效 selector，不作为 Authority 证据 | L606–L633 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-bl-l0606) |
| <a id="map-orig-bl-l0634"></a>BL:27 | 6. Identity + State Ownership | L634–L637 | entitydefence / 保留一个规范实例 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l0634) |
| <a id="map-orig-bl-l0638"></a>BL:28 | 6.1 Identity 不再等于 UUID | L638–L669 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l0638) |
| <a id="map-orig-bl-l0670"></a>BL:29 | 6.2 State Ownership 的正确定义 | L670–L701 | entitydefence / 保留一个规范实例 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l0670) |
| <a id="map-orig-bl-l0702"></a>BL:30 | 6.3 OwnershipBinding 与 ExecutionLease 分离 | L702–L734 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l0702) |
| <a id="map-orig-bl-l0735"></a>BL:31 | 6.4 Ownership 优先级 | L735–L749 | entitydefence / 保留一个规范实例 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l0735) |
| <a id="map-orig-bl-l0750"></a>BL:32 | 7. Raw Observation 2.0 | L750–L755 | entitydefence / 为什么只覆写实体方法不够 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l0750) |
| <a id="map-orig-bl-l0756"></a>BL:33 | 7.1 Event + Raw Reconciliation | L756–L777 | entitydefence / 为什么只覆写实体方法不够 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l0756) |
| <a id="map-orig-bl-l0778"></a>BL:34 | 7.2 Observation 消费者 | L778–L793 | entitydefence / 为什么只覆写实体方法不够 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l0778) |
| <a id="map-orig-bl-l0794"></a>BL:35 | 8. Enforcement Plane | L794–L835 | msitem / 服务端队列与线程边界 | [原文](MS-MSITEM-PLAN.md#orig-bl-l0794) |
| <a id="map-orig-bl-l0836"></a>BL:36 | 8.3 Phase-aware Enforcement / Tick-Level Control | L836–L873 | msitem / 服务端队列与线程边界 | [原文](MS-MSITEM-PLAN.md#orig-bl-l0836) |
| <a id="map-orig-bl-l0874"></a>BL:37 | 9. VITALS Enhanced — 通用改血加强 | L874–L877 | msitem / 正常死亡、运行时清除与混合执行 | [原文](MS-MSITEM-PLAN.md#orig-bl-l0874) |
| <a id="map-orig-bl-l0878"></a>BL:38 | 9.1 当前设计基础 | L878–L902 | msitem / 正常死亡、运行时清除与混合执行 | [原文](MS-MSITEM-PLAN.md#orig-bl-l0878) |
| <a id="map-orig-bl-l0903"></a>BL:39 | 9.2 VITALS 不是一次 setHealth | L903–L928 | msitem / 正常死亡、运行时清除与混合执行 | [原文](MS-MSITEM-PLAN.md#orig-bl-l0903) |
| <a id="map-orig-bl-l0929"></a>BL:40 | 9.3 至少覆盖的生命状态 | L929–L951 | msitem / 正常死亡、运行时清除与混合执行 | [原文](MS-MSITEM-PLAN.md#orig-bl-l0929) |
| <a id="map-orig-bl-l0952"></a>BL:41 | 9.4 VitalStateResolver | L952–L992 | msitem / 正常死亡、运行时清除与混合执行 | [原文](MS-MSITEM-PLAN.md#orig-bl-l0952) |
| <a id="map-orig-bl-l0993"></a>BL:42 | 9.4.1 Resolver 必须是“通用发现 + 可插拔证据”，不是 Adapter 海 | L993–L1040 | msitem / 正常死亡、运行时清除与混合执行 | [原文](MS-MSITEM-PLAN.md#orig-bl-l0993) |
| <a id="map-orig-bl-l1041"></a>BL:43 | 9.4.2 Resolver Provider 的可信边界 | L1041–L1064 | msitem / 正常死亡、运行时清除与混合执行 | [原文](MS-MSITEM-PLAN.md#orig-bl-l1041) |
| <a id="map-orig-bl-l1065"></a>BL:44 | 9.5 UNRESOLVED 失败语义 | L1065–L1088 | msitem / 正常死亡、运行时清除与混合执行 | [原文](MS-MSITEM-PLAN.md#orig-bl-l1065) |
| <a id="map-orig-bl-l1089"></a>BL:45 | 9.6 VITALS 与 Owner Protection | L1089–L1105 | entitydefence / 实体状态也必须防御 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l1089) |
| <a id="map-orig-bl-l1106"></a>BL:46 | 9.7 VITALS 铁律 | L1106–L1122 | msitem / 正常死亡、运行时清除与混合执行 | [原文](MS-MSITEM-PLAN.md#orig-bl-l1106) |
| <a id="map-orig-bl-l1123"></a>BL:47 | 10. TERMINAL + Admission Closure | L1123–L1124 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-bl-l1123) |
| <a id="map-orig-bl-l1125"></a>BL:48 | 10.1 TERMINAL 最终语义 | L1125–L1146 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-bl-l1125) |
| <a id="map-orig-bl-l1147"></a>BL:49 | 10.2 Admission Closure | L1147–L1171 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-bl-l1147) |
| <a id="map-orig-bl-l1172"></a>BL:50 | 10.3 Pre-Admission Binding | L1172–L1205 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-bl-l1172) |
| <a id="map-orig-bl-l1206"></a>BL:51 | 10.3.1 Pre-Binding 的现实实现边界 | L1206–L1241 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-bl-l1206) |
| <a id="map-orig-bl-l1242"></a>BL:52 | 10.3.2 Pre-Binding 作为 P0-A 独立难点 | L1242–L1256 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-bl-l1242) |
| <a id="map-orig-bl-l1257"></a>BL:53 | 11. Constraint Fusion | L1257–L1285 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-bl-l1257) |
| <a id="map-orig-bl-l1286"></a>BL:54 | 12. Execution Scheduling + Feedback | L1286–L1289 | msitem / 服务端队列与线程边界 | [原文](MS-MSITEM-PLAN.md#orig-bl-l1286) |
| <a id="map-orig-bl-l1290"></a>BL:55 | 12.1 Scheduling | L1290–L1310 | msitem / 服务端队列与线程边界 | [原文](MS-MSITEM-PLAN.md#orig-bl-l1290) |
| <a id="map-orig-bl-l1311"></a>BL:56 | 12.2 Feedback | L1311–L1346 | msitem / 服务端队列与线程边界 | [原文](MS-MSITEM-PLAN.md#orig-bl-l1311) |
| <a id="map-orig-bl-l1347"></a>BL:57 | 12.3 Domain 不得自行升级 Authority | L1347–L1365 | msitem / 服务端队列与线程边界 | [原文](MS-MSITEM-PLAN.md#orig-bl-l1347) |
| <a id="map-orig-bl-l1366"></a>BL:58 | 12.4 原子多目标 JudgmentBatch：原子的是“逻辑提交”，不是物理世界 | L1366–L1399 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1366) |
| <a id="map-orig-bl-l1400"></a>BL:59 | 13. Execution Authority Chain — State Ownership / Dispatch / Return 统一模型 | L1400–L1426 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1400) |
| <a id="map-orig-bl-l1427"></a>BL:60 | 13.1 Authority Snapshot | L1427–L1447 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1427) |
| <a id="map-orig-bl-l1448"></a>BL:61 | 13.2 Dispatch Authority | L1448–L1489 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1448) |
| <a id="map-orig-bl-l1490"></a>BL:62 | 13.3 Return Authority / AllReturn | L1490–L1523 | skills / 实现一个简单 Hook | [原文](MS-SKILLS-PLAN.md#orig-bl-l1490) |
| <a id="map-orig-bl-l1524"></a>BL:63 | 13.4 Return / Dispatch 的技术实现路线 | L1524–L1541 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1524) |
| <a id="map-orig-bl-l1542"></a>BL:64 | 13.4.1 首选后端：显式 Execution Gateway | L1542–L1572 | msitem / 总体架构 | [原文](MS-MSITEM-PLAN.md#orig-bl-l1542) |
| <a id="map-orig-bl-l1573"></a>BL:65 | 13.4.2 外部 / Minecraft 无法重构路径：最小 instrumented seam | L1573–L1583 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1573) |
| <a id="map-orig-bl-l1584"></a>BL:66 | 13.4.3 Return Authority 与 Integrity 的边界 | L1584–L1616 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1584) |
| <a id="map-orig-bl-l1617"></a>BL:67 | 13.4.4 Dispatch 本身必须进入 Integrity Critical Set | L1617–L1633 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1617) |
| <a id="map-orig-bl-l1634"></a>BL:68 | 14. Integrity Closure | L1634–L1650 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1634) |
| <a id="map-orig-bl-l1651"></a>BL:69 | 14.1 Integrity Plane | L1651–L1672 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1651) |
| <a id="map-orig-bl-l1673"></a>BL:70 | 14.2 状态机 | L1673–L1694 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1673) |
| <a id="map-orig-bl-l1695"></a>BL:71 | 14.3 Repair 必须有预算 | L1695–L1714 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1695) |
| <a id="map-orig-bl-l1715"></a>BL:72 | 14.4 Fence Scope | L1715–L1736 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1715) |
| <a id="map-orig-bl-l1737"></a>BL:73 | 14.5 Repair 成功不等于 Trust 恢复 | L1737–L1758 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1737) |
| <a id="map-orig-bl-l1759"></a>BL:74 | 15. Anti-Interception / 方法体注入对抗 | L1759–L1787 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1759) |
| <a id="map-orig-bl-l1788"></a>BL:75 | 15.1 不把字节码 hash 当唯一真理 | L1788–L1806 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1788) |
| <a id="map-orig-bl-l1807"></a>BL:76 | 15.2 Code Polymorphism 降级为可选 Anti-analysis | L1807–L1814 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1807) |
| <a id="map-orig-bl-l1815"></a>BL:77 | 15.3 不采用的高风险做法 | L1815–L1826 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1815) |
| <a id="map-orig-bl-l1827"></a>BL:78 | 16. Native Trust Boundary | L1827–L1828 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1827) |
| <a id="map-orig-bl-l1829"></a>BL:79 | 16.1 Native 的职责 | L1829–L1850 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1829) |
| <a id="map-orig-bl-l1851"></a>BL:80 | 16.2 Java 不能直接把 Authority 结论喂给 Native | L1851–L1878 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1851) |
| <a id="map-orig-bl-l1879"></a>BL:81 | 16.3 Native callback Java | L1879–L1890 | skills / 命令桥接契约 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1879) |
| <a id="map-orig-bl-l1891"></a>BL:82 | 16.4 审计分级 | L1891–L1908 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1891) |
| <a id="map-orig-bl-l1909"></a>BL:83 | 17. Render Boundary + Native Render Firewall | L1909–L1910 | skills / 客户端 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1909) |
| <a id="map-orig-bl-l1911"></a>BL:84 | 17.1 Server 与 Client 的能力边界 | L1911–L1938 | skills / 客户端 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1911) |
| <a id="map-orig-bl-l1939"></a>BL:85 | 17.2 Optional Client Authority Extension | L1939–L1968 | entitydefence / 修复客户端注册表 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l1939) |
| <a id="map-orig-bl-l1969"></a>BL:86 | 17.3 RenderDirective 而不是 RenderIntent | L1969–L1991 | skills / 客户端 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1969) |
| <a id="map-orig-bl-l1992"></a>BL:87 | 18. Durable Truth / Anti-Rollback / Causality | L1992–L1993 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1992) |
| <a id="map-orig-bl-l1994"></a>BL:88 | 18.1 正式路线 | L1994–L2013 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l1994) |
| <a id="map-orig-bl-l2014"></a>BL:89 | 18.2 Anti-Rollback | L2014–L2027 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2014) |
| <a id="map-orig-bl-l2028"></a>BL:90 | 18.3 Causal Ordering | L2028–L2040 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2028) |
| <a id="map-orig-bl-l2041"></a>BL:91 | 18.4 COMMIT_UNKNOWN | L2041–L2054 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2041) |
| <a id="map-orig-bl-l2055"></a>BL:92 | 18.5 Internal Root Recovery | L2055–L2065 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2055) |
| <a id="map-orig-bl-l2066"></a>BL:93 | 19. Resource Resilience | L2066–L2073 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2066) |
| <a id="map-orig-bl-l2074"></a>BL:94 | 19.1 State-driven Enforcement | L2074–L2090 | entitydefence / 构建幂等修复过程 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l2074) |
| <a id="map-orig-bl-l2091"></a>BL:95 | 19.1.1 DeviationSemantics | L2091–L2129 | entitydefence / 构建幂等修复过程 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l2091) |
| <a id="map-orig-bl-l2130"></a>BL:96 | 19.1.2 Raw Observation 不是“必然看到所有瞬态”的魔法 | L2130–L2175 | entitydefence / 修复时机 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l2130) |
| <a id="map-orig-bl-l2176"></a>BL:97 | 19.2 必须具备 | L2176–L2190 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2176) |
| <a id="map-orig-bl-l2191"></a>BL:98 | 19.3 Resource Priority | L2191–L2220 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2191) |
| <a id="map-orig-bl-l2221"></a>BL:99 | 19.4 Storm Mode | L2221–L2241 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2221) |
| <a id="map-orig-bl-l2242"></a>BL:100 | 20. Movement / Equipment / Effect Domain 加强 | L2242–L2245 | entitydefence / 实体状态也必须防御 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l2242) |
| <a id="map-orig-bl-l2246"></a>BL:101 | 20.1 Movement | L2246–L2259 | entitydefence / 实体状态也必须防御 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l2246) |
| <a id="map-orig-bl-l2260"></a>BL:102 | 20.2 Equipment | L2260–L2275 | entitydefence / 实体状态也必须防御 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l2260) |
| <a id="map-orig-bl-l2276"></a>BL:103 | 20.3 Effect | L2276–L2289 | entitydefence / 实体状态也必须防御 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l2276) |
| <a id="map-orig-bl-l2290"></a>BL:104 | 21. Deception / Honeypot | L2290–L2331 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2290) |
| <a id="map-orig-bl-l2332"></a>BL:105 | 22. Audit / Incident / Formal / Chaos | L2332–L2335 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2332) |
| <a id="map-orig-bl-l2336"></a>BL:106 | 22.1 Incident Correlation | L2336–L2353 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2336) |
| <a id="map-orig-bl-l2354"></a>BL:107 | 22.2 Audit | L2354–L2371 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2354) |
| <a id="map-orig-bl-l2372"></a>BL:108 | 22.3 Formal Verification | L2372–L2401 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2372) |
| <a id="map-orig-bl-l2402"></a>BL:109 | 22.4 Chaos / Adversarial Validation | L2402–L2421 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2402) |
| <a id="map-orig-bl-l2422"></a>BL:110 | 23. 支持性安全增强：保留但不升格为核心轴 | L2422–L2423 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2422) |
| <a id="map-orig-bl-l2424"></a>BL:111 | 23.1 ClassLoader / Module isolation | L2424–L2435 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2424) |
| <a id="map-orig-bl-l2436"></a>BL:112 | 23.2 Supply-chain self-attestation | L2436–L2449 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2436) |
| <a id="map-orig-bl-l2450"></a>BL:113 | 23.3 Code signing / Native RX pages | L2450–L2457 | skills / Native 实现规则 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2450) |
| <a id="map-orig-bl-l2458"></a>BL:114 | 23.4 Entity Physical State Snapshot / Rollback：仅限受约束恢复辅助 | L2458–L2498 | entitydefence / 构建幂等修复过程 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l2458) |
| <a id="map-orig-bl-l2499"></a>BL:115 | 24. 明确淘汰 / 不进入主线的旧加强 | L2499–L2522 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2499) |
| <a id="map-orig-bl-l2523"></a>BL:116 | 25. 从 7.14A 冻结基准到当前 r50 的升级路线 | L2523–L2526 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2523) |
| <a id="map-orig-bl-l2527"></a>BL:117 | 25.1 7.14A — completed / frozen baseline | L2527–L2543 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2527) |
| <a id="map-orig-bl-l2544"></a>BL:118 | 25.2 7.14B — Authority Split（r42 已实现并接入） | L2544–L2556 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2544) |
| <a id="map-orig-bl-l2557"></a>BL:119 | 25.3 7.14C — Presence / OwnerImmunity Split（r42 已实现并接入） | L2557–L2571 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2557) |
| <a id="map-orig-bl-l2572"></a>BL:120 | 25.4 7.14D — Budget / Telemetry Seam（r50 继承并通过本地回归） | L2572–L2589 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2572) |
| <a id="map-orig-bl-l2590"></a>BL:121 | 25.5 7.15 — Java DurableRootStore Shadow | L2590–L2611 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2590) |
| <a id="map-orig-bl-l2612"></a>BL:122 | 25.6 7.16 — Durable Cutover / Enforcement Closure | L2612–L2631 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2612) |
| <a id="map-orig-bl-l2632"></a>BL:123 | 25.7 7.17 — FULL L5 Candidate | L2632–L2646 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2632) |
| <a id="map-orig-bl-l2647"></a>BL:124 | 25.8 8.x — Internal Dominion Root / FULL L6 Internal | L2647–L2662 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2647) |
| <a id="map-orig-bl-l2663"></a>BL:125 | 25.8.1 Internal Root 收口目标 | L2663–L2681 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2663) |
| <a id="map-orig-bl-l2682"></a>BL:126 | 25.9 FULL L6 Internal 之后 — L6 Enhanced | L2682–L2700 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2682) |
| <a id="map-orig-bl-l2701"></a>BL:127 | 26. 各增强轴的验收原则 | L2701–L2702 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2701) |
| <a id="map-orig-bl-l2703"></a>BL:128 | 26.1 Identity / Ownership | L2703–L2713 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l2703) |
| <a id="map-orig-bl-l2714"></a>BL:129 | 26.2 VITALS | L2714–L2735 | msitem / 验证矩阵 | [原文](MS-MSITEM-PLAN.md#orig-bl-l2714) |
| <a id="map-orig-bl-l2736"></a>BL:130 | 26.3 TERMINAL | L2736–L2746 | msitem / 验证矩阵 | [原文](MS-MSITEM-PLAN.md#orig-bl-l2736) |
| <a id="map-orig-bl-l2747"></a>BL:131 | 26.4 Dispatch / Return | L2747–L2757 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2747) |
| <a id="map-orig-bl-l2758"></a>BL:132 | 26.5 Integrity | L2758–L2769 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2758) |
| <a id="map-orig-bl-l2770"></a>BL:133 | 26.6 Resource Resilience | L2770–L2786 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2770) |
| <a id="map-orig-bl-l2787"></a>BL:134 | 26.7 Production Process Boundary | L2787–L2798 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2787) |
| <a id="map-orig-bl-l2799"></a>BL:135 | 27. 最终整合架构 | L2799–L2858 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2799) |
| <a id="map-orig-bl-l2859"></a>BL:136 | 28. 最终优先级总结 | L2859–L2860 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2859) |
| <a id="map-orig-bl-l2861"></a>BL:137 | 当前状态与下一步 | L2861–L2872 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2861) |
| <a id="map-orig-bl-l2873"></a>BL:138 | 正式阶段路线 | L2873–L2904 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2873) |
| <a id="map-orig-bl-l2905"></a>BL:139 | FULL L6 Internal 后最重要的 Enhanced P0 | L2905–L2926 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2905) |
| <a id="map-orig-bl-l2927"></a>BL:140 | 29. 本文冻结结论 | L2927–L2942 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2927) |
| <a id="map-orig-bl-l2943"></a>BL:141 | 30. V1.4 — Implementation Spec 前的缺口收口 | L2943–L2962 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2943) |
| <a id="map-orig-bl-l2963"></a>BL:142 | 30.1 进入 Implementation Spec 前仍需逐项冻结的四个协议 | L2963–L2992 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2963) |
| <a id="map-orig-bl-l2993"></a>BL:143 | 30.2 类型复用与 Integrity 横切门 | L2993–L3009 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-bl-l2993) |
| <a id="map-orig-bl-l3010"></a>BL:144 | 30.3 统一规范链（2026-09-05 文档接合） | L3010–L3022 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-bl-l3010) |

<a id="source-v26"></a>

### V26 · Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md

原路径：`docs/plans/Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md`。 [原始快照](../archive/plans-before-ms-restructure-r66/Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md)。

| 映射锚点 | 旧标题（原编号不变） | 旧行范围 | 新 MS 章节 | 新正文落点 |
|---|---|---|---|---|
| <a id="map-orig-v26-l0001"></a>V26:1 | Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） | L1–L36 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0001) |
| <a id="map-orig-v26-l0037"></a>V26:2 | 0. 最终结论 | L37–L129 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0037) |
| <a id="map-orig-v26-l0130"></a>V26:3 | 1. 文档权威与版本关系 | L130–L131 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0130) |
| <a id="map-orig-v26-l0132"></a>V26:4 | 1.1 历史版本 | L132–L155 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0132) |
| <a id="map-orig-v26-l0156"></a>V26:5 | 1.2 不回写已完成阶段 | L156–L173 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0156) |
| <a id="map-orig-v26-l0174"></a>V26:6 | 1.3 Internal Root 不变 | L174–L184 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0174) |
| <a id="map-orig-v26-l0185"></a>V26:7 | 2. r51 / 7.15 冻结基线 | L185–L211 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0185) |
| <a id="map-orig-v26-l0212"></a>V26:8 | 3. 正式术语 | L212–L213 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0212) |
| <a id="map-orig-v26-l0214"></a>V26:9 | 3.1 攻击 / 拦截 / 防护 | L214–L228 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0214) |
| <a id="map-orig-v26-l0229"></a>V26:10 | 3.2 三种全返回 | L229–L238 | skills / 实现一个简单 Hook | [原文](MS-SKILLS-PLAN.md#orig-v26-l0229) |
| <a id="map-orig-v26-l0239"></a>V26:11 | 3.3 极限代码对抗 | L239–L257 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0239) |
| <a id="map-orig-v26-l0258"></a>V26:12 | 3.4 Candidate Attack Surface / 候选攻击面 | L258–L294 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0258) |
| <a id="map-orig-v26-l0295"></a>V26:13 | 3.5 证明决定处置级别，不决定范围 | L295–L324 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0295) |
| <a id="map-orig-v26-l0325"></a>V26:14 | 3.6 Existence Termination / 存在性终结 | L325–L340 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0325) |
| <a id="map-orig-v26-l0341"></a>V26:15 | 3.7 ResultGraph / Certificate 的权威边界 | L341–L360 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0341) |
| <a id="map-orig-v26-l0361"></a>V26:16 | 4. 目标、非目标与宿主安全边界 | L361–L362 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0361) |
| <a id="map-orig-v26-l0363"></a>V26:17 | 4.1 三档目标范围 | L363–L364 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0363) |
| <a id="map-orig-v26-l0365"></a>V26:18 | COMPATIBLE / 兼容 | L365–L374 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0365) |
| <a id="map-orig-v26-l0375"></a>V26:19 | ENHANCED / 强化 | L375–L388 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0375) |
| <a id="map-orig-v26-l0389"></a>V26:20 | EXTREME / 极限代码对抗 | L389–L401 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0389) |
| <a id="map-orig-v26-l0402"></a>V26:21 | 4.2 Extreme 不设置的限制 | L402–L416 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0402) |
| <a id="map-orig-v26-l0417"></a>V26:22 | 4.3 Extreme 允许牺牲的东西 | L417–L428 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0417) |
| <a id="map-orig-v26-l0429"></a>V26:23 | 4.4 Extreme 绝对安全边界 | L429–L452 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0429) |
| <a id="map-orig-v26-l0453"></a>V26:24 | 5. 核心不变量 | L453–L454 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0453) |
| <a id="map-orig-v26-l0455"></a>V26:25 | 5.1 继承不变量 D1–D32 | L455–L471 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0455) |
| <a id="map-orig-v26-l0472"></a>V26:26 | 5.2 D33 修订：Final Write Sink 不得绝对化 | L472–L502 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0472) |
| <a id="map-orig-v26-l0503"></a>V26:27 | 5.3 V2.2 新增强不变量（继续有效） | L503–L520 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0503) |
| <a id="map-orig-v26-l0521"></a>V26:28 | 5.4 V2.3 执行合同不变量 | L521–L538 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0521) |
| <a id="map-orig-v26-l0539"></a>V26:29 | 5.5 V2.4 Design Freeze 不变量 | L539–L551 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0539) |
| <a id="map-orig-v26-l0552"></a>V26:30 | 5.6 V2.5 Runtime Surface 不变量 | L552–L570 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0552) |
| <a id="map-orig-v26-l0571"></a>V26:31 | 5.7 V2.6 接合合同不变量 | L571–L583 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0571) |
| <a id="map-orig-v26-l0584"></a>V26:32 | 6. 强度模型 | L584–L585 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0584) |
| <a id="map-orig-v26-l0586"></a>V26:33 | 6.1 顶层预设 | L586–L605 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0586) |
| <a id="map-orig-v26-l0606"></a>V26:34 | 6.2 三档正式范围 | L606–L613 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0606) |
| <a id="map-orig-v26-l0614"></a>V26:35 | 6.3 子项档位 | L614–L644 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0614) |
| <a id="map-orig-v26-l0645"></a>V26:36 | 6.4 配置值与有效值 | L645–L674 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0645) |
| <a id="map-orig-v26-l0675"></a>V26:37 | 6.5 Execution State 与 Attack Outcome | L675–L760 | msitem / 总体架构 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0675) |
| <a id="map-orig-v26-l0761"></a>V26:38 | 6.6 V2.5 逐能力 ProfilePresetMatrix | L761–L777 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0761) |
| <a id="map-orig-v26-l0778"></a>V26:39 | 6.6.1 玩家可见攻击模块 | L778–L799 | msitem / 列出全部击杀入口 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0778) |
| <a id="map-orig-v26-l0800"></a>V26:40 | 6.6.2 十五项高级攻击政策 | L800–L819 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0800) |
| <a id="map-orig-v26-l0820"></a>V26:41 | 6.6.3 拦截机制矩阵 | L820–L834 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0820) |
| <a id="map-orig-v26-l0835"></a>V26:42 | 6.6.4 穿透后防护矩阵 | L835–L856 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0835) |
| <a id="map-orig-v26-l0857"></a>V26:43 | 6.6.5 运行时支撑矩阵 | L857–L873 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-v26-l0857) |
| <a id="map-orig-v26-l0874"></a>V26:44 | 6.6.6 矩阵正确性要求 | L874–L883 | msitem / 验证矩阵 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0874) |
| <a id="map-orig-v26-l0884"></a>V26:45 | 6.7 普通开关到执行权限的唯一映射（V2.6） | L884–L930 | msitem / 列出全部击杀入口 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0884) |
| <a id="map-orig-v26-l0931"></a>V26:46 | 7. GUI 正式结构 | L931–L932 | msitem / 客户端表现也是物品的一部分 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0931) |
| <a id="map-orig-v26-l0933"></a>V26:47 | 7.1 顶层 | L933–L953 | msitem / 客户端表现也是物品的一部分 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0933) |
| <a id="map-orig-v26-l0954"></a>V26:48 | 7.2 攻击页 | L954–L1017 | msitem / 客户端表现也是物品的一部分 | [原文](MS-MSITEM-PLAN.md#orig-v26-l0954) |
| <a id="map-orig-v26-l1018"></a>V26:49 | 7.3 拦截页 | L1018–L1053 | msitem / 客户端表现也是物品的一部分 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1018) |
| <a id="map-orig-v26-l1054"></a>V26:50 | 7.4 防护页 | L1054–L1080 | msitem / 客户端表现也是物品的一部分 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1054) |
| <a id="map-orig-v26-l1081"></a>V26:51 | 7.5 运行时页 | L1081–L1104 | msitem / 客户端表现也是物品的一部分 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1081) |
| <a id="map-orig-v26-l1105"></a>V26:52 | 7.6 功能页 | L1105–L1108 | msitem / 客户端表现也是物品的一部分 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1105) |
| <a id="map-orig-v26-l1109"></a>V26:53 | 7.7 V2.5 界面收敛规则 | L1109–L1124 | msitem / 客户端表现也是物品的一部分 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1109) |
| <a id="map-orig-v26-l1125"></a>V26:54 | 8. 选项、政策键与阶段 | L1125–L1126 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1125) |
| <a id="map-orig-v26-l1127"></a>V26:55 | 8.1 V2.2 攻击政策（V2.3/V2.4/V2.5 沿用） | L1127–L1153 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1127) |
| <a id="map-orig-v26-l1154"></a>V26:56 | 8.2 攻击来源追踪 / MutationTrace | L1154–L1172 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1154) |
| <a id="map-orig-v26-l1173"></a>V26:57 | 8.3 7.16 Scope Lock | L1173–L1199 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1173) |
| <a id="map-orig-v26-l1200"></a>V26:58 | 8.4 四项执行合同的阶段归属 | L1200–L1210 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1200) |
| <a id="map-orig-v26-l1211"></a>V26:59 | 8.5 V2.4 收口合同的阶段归属 | L1211–L1221 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1211) |
| <a id="map-orig-v26-l1222"></a>V26:60 | 8.6 V2.5 Runtime Surface Taxonomy | L1222–L1254 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1222) |
| <a id="map-orig-v26-l1255"></a>V26:61 | 8.6.1 广度与深度等高 | L1255–L1272 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1255) |
| <a id="map-orig-v26-l1273"></a>V26:62 | 9. 攻击平面 | L1273–L1274 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1273) |
| <a id="map-orig-v26-l1275"></a>V26:63 | 9.1 三档攻击范围 | L1275–L1276 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1275) |
| <a id="map-orig-v26-l1277"></a>V26:64 | Compatible | L1277–L1287 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1277) |
| <a id="map-orig-v26-l1288"></a>V26:65 | Enhanced | L1288–L1299 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1288) |
| <a id="map-orig-v26-l1300"></a>V26:66 | Extreme | L1300–L1326 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1300) |
| <a id="map-orig-v26-l1327"></a>V26:67 | 9.2 Candidate-first 执行 | L1327–L1361 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1327) |
| <a id="map-orig-v26-l1362"></a>V26:68 | 9.2.1 Candidate Admission Rule | L1362–L1396 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1362) |
| <a id="map-orig-v26-l1397"></a>V26:69 | 9.2.2 独立性、有效期与合法操作 | L1397–L1412 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1397) |
| <a id="map-orig-v26-l1413"></a>V26:70 | 9.2.3 候选处置权限表 | L1413–L1424 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1413) |
| <a id="map-orig-v26-l1425"></a>V26:71 | 9.2.4 准入与处置回执 | L1425–L1441 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1425) |
| <a id="map-orig-v26-l1442"></a>V26:72 | 9.2.5 Candidate 临时限制释放防抖 | L1442–L1487 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1442) |
| <a id="map-orig-v26-l1488"></a>V26:73 | 9.3 Extreme 不因共享路径自动降级 | L1488–L1505 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1488) |
| <a id="map-orig-v26-l1506"></a>V26:74 | 9.4 极限清除 / Existence Termination | L1506–L1545 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1506) |
| <a id="map-orig-v26-l1546"></a>V26:75 | 9.4.1 Completion 参与范围与客户端边界（V2.6） | L1546–L1568 | msitem / 服务端与客户端一致性 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1546) |
| <a id="map-orig-v26-l1569"></a>V26:76 | 9.4.2 通用恢复链处置（2026-09-08 规划增量） | L1569–L1587 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1569) |
| <a id="map-orig-v26-l1588"></a>V26:77 | 9.5 TargetEpoch 资格代际 | L1588–L1664 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1588) |
| <a id="map-orig-v26-l1665"></a>V26:78 | 9.5.1 Canonical identity 与 revision 对照 | L1665–L1684 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-v26-l1665) |
| <a id="map-orig-v26-l1685"></a>V26:79 | 9.5.2 多目标 QualificationBatch 原子性 | L1685–L1696 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1685) |
| <a id="map-orig-v26-l1697"></a>V26:80 | 9.6 全权威结果闭包 | L1697–L1717 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-v26-l1697) |
| <a id="map-orig-v26-l1718"></a>V26:81 | 9.7 代码防御剥离 | L1718–L1732 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1718) |
| <a id="map-orig-v26-l1733"></a>V26:82 | 9.8 攻击来源追踪 | L1733–L1761 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1733) |
| <a id="map-orig-v26-l1762"></a>V26:83 | 10. 拦截平面 | L1762–L1763 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1762) |
| <a id="map-orig-v26-l1764"></a>V26:84 | 10.1 统一机制 | L1764–L1788 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1764) |
| <a id="map-orig-v26-l1789"></a>V26:85 | 10.2 Extreme “全部严格” | L1789–L1820 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1789) |
| <a id="map-orig-v26-l1821"></a>V26:86 | 10.3 “全部严格”不是全 JVM 随机停机 | L1821–L1836 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1821) |
| <a id="map-orig-v26-l1837"></a>V26:87 | 11. 防护平面 | L1837–L1838 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1837) |
| <a id="map-orig-v26-l1839"></a>V26:88 | 11.1 反拦截 | L1839–L1862 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1839) |
| <a id="map-orig-v26-l1863"></a>V26:89 | 11.2 反全返回四层 | L1863–L1884 | skills / 实现一个简单 Hook | [原文](MS-SKILLS-PLAN.md#orig-v26-l1863) |
| <a id="map-orig-v26-l1885"></a>V26:90 | 11.3 Fence Scope Escalation | L1885–L1898 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1885) |
| <a id="map-orig-v26-l1899"></a>V26:91 | TARGET_FENCED | L1899–L1902 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1899) |
| <a id="map-orig-v26-l1903"></a>V26:92 | OPERATION_FENCED | L1903–L1906 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1903) |
| <a id="map-orig-v26-l1907"></a>V26:93 | PLANE_FENCED | L1907–L1910 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1907) |
| <a id="map-orig-v26-l1911"></a>V26:94 | ROOT_FENCED | L1911–L1921 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1911) |
| <a id="map-orig-v26-l1922"></a>V26:95 | REALM_FENCED | L1922–L1929 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1922) |
| <a id="map-orig-v26-l1930"></a>V26:96 | 11.4 FENCE 不是“放弃攻击” | L1930–L1950 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1930) |
| <a id="map-orig-v26-l1951"></a>V26:97 | 11.5 隔离、恢复与攻击结果的独立更新 | L1951–L1973 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1951) |
| <a id="map-orig-v26-l1974"></a>V26:98 | 12. 运行时与宿主安全 | L1974–L1975 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1974) |
| <a id="map-orig-v26-l1976"></a>V26:99 | 12.1 Runtime Capability Ceiling | L1976–L1993 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1976) |
| <a id="map-orig-v26-l1994"></a>V26:100 | 12.2 Host Safety Governor | L1994–L2063 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l1994) |
| <a id="map-orig-v26-l2064"></a>V26:101 | 12.3 不设 Mod 数量硬上限 | L2064–L2084 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2064) |
| <a id="map-orig-v26-l2085"></a>V26:102 | 12.4 Candidate 反诱导与公平调度 | L2085–L2133 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2085) |
| <a id="map-orig-v26-l2134"></a>V26:103 | 12.5 Cross-surface Operation Plan | L2134–L2181 | msitem / 服务端队列与线程边界 | [原文](MS-MSITEM-PLAN.md#orig-v26-l2134) |
| <a id="map-orig-v26-l2182"></a>V26:104 | 12.6 副作用未知与跨版本去重（V2.6） | L2182–L2223 | msitem / 去重与幂等 | [原文](MS-MSITEM-PLAN.md#orig-v26-l2182) |
| <a id="map-orig-v26-l2224"></a>V26:105 | 13. Evidence / Lease / TOCTOU | L2224–L2225 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2224) |
| <a id="map-orig-v26-l2226"></a>V26:106 | 13.1 Evidence | L2226–L2243 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2226) |
| <a id="map-orig-v26-l2244"></a>V26:107 | 13.2 ExecutionLease | L2244–L2267 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2244) |
| <a id="map-orig-v26-l2268"></a>V26:108 | 13.3 三次复验 + 真实提交门 | L2268–L2285 | msitem / 服务端队列与线程边界 | [原文](MS-MSITEM-PLAN.md#orig-v26-l2268) |
| <a id="map-orig-v26-l2286"></a>V26:109 | 13.4 Qualification Transition Commit Gate | L2286–L2307 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2286) |
| <a id="map-orig-v26-l2308"></a>V26:110 | 14. ResultGraph 与 CandidateGraph | L2308–L2309 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2308) |
| <a id="map-orig-v26-l2310"></a>V26:111 | 14.1 CandidateGraph | L2310–L2322 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2310) |
| <a id="map-orig-v26-l2323"></a>V26:112 | 14.2 ResultGraph | L2323–L2334 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2323) |
| <a id="map-orig-v26-l2335"></a>V26:113 | 14.3 Graph Revision | L2335–L2347 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2335) |
| <a id="map-orig-v26-l2348"></a>V26:114 | 14.4 图预算耗尽 | L2348–L2367 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2348) |
| <a id="map-orig-v26-l2368"></a>V26:115 | 14.5 Obligation Compaction / 规范义务归并 | L2368–L2419 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2368) |
| <a id="map-orig-v26-l2420"></a>V26:116 | 14.6 不同 key 洪泛与范围汇总 | L2420–L2462 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2420) |
| <a id="map-orig-v26-l2463"></a>V26:117 | 14.7 压缩、淘汰与限制交接 | L2463–L2478 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2463) |
| <a id="map-orig-v26-l2479"></a>V26:118 | 14.8 义务解除与证据权限 | L2479–L2492 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2479) |
| <a id="map-orig-v26-l2493"></a>V26:119 | 15. Authority、持久化与恢复 | L2493–L2494 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2493) |
| <a id="map-orig-v26-l2495"></a>V26:120 | 15.1 权威对象 | L2495–L2504 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2495) |
| <a id="map-orig-v26-l2505"></a>V26:121 | 15.2 ResultGraph / Certificate 不写 Truth | L2505–L2526 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2505) |
| <a id="map-orig-v26-l2527"></a>V26:122 | 15.3 Restart | L2527–L2540 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2527) |
| <a id="map-orig-v26-l2541"></a>V26:123 | 15.4 V2.3 未决范围与重启恢复 | L2541–L2550 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2541) |
| <a id="map-orig-v26-l2551"></a>V26:124 | 15.5 Qualification Transaction 崩溃恢复 | L2551–L2575 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2551) |
| <a id="map-orig-v26-l2576"></a>V26:125 | 15.6 Runtime Surface 持久化边界 | L2576–L2602 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2576) |
| <a id="map-orig-v26-l2603"></a>V26:126 | 15.7 执行效果恢复次序（V2.6） | L2603–L2612 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2603) |
| <a id="map-orig-v26-l2613"></a>V26:127 | 16. 分阶段实施路线 | L2613–L2614 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2613) |
| <a id="map-orig-v26-l2615"></a>V26:128 | 16.1 7.16 — 不返工原则 | L2615–L2649 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2615) |
| <a id="map-orig-v26-l2650"></a>V26:129 | 16.2 7.17 — Enforcement Foundation | L2650–L2702 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2650) |
| <a id="map-orig-v26-l2703"></a>V26:130 | 16.3 L6 — Internal Dominion Root | L2703–L2716 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2703) |
| <a id="map-orig-v26-l2717"></a>V26:131 | 16.4 L6E — Extreme Closure | L2717–L2718 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2717) |
| <a id="map-orig-v26-l2719"></a>V26:132 | P0-A | L2719–L2724 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2719) |
| <a id="map-orig-v26-l2725"></a>V26:133 | P0-B | L2725–L2737 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2725) |
| <a id="map-orig-v26-l2738"></a>V26:134 | P0-C | L2738–L2749 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2738) |
| <a id="map-orig-v26-l2750"></a>V26:135 | P0-D | L2750–L2762 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2750) |
| <a id="map-orig-v26-l2763"></a>V26:136 | 16.5 V2.5 Runtime Surface 阶段归属 | L2763–L2779 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2763) |
| <a id="map-orig-v26-l2780"></a>V26:137 | 16.6 7.17 可交付批次与出口（V2.6） | L2780–L2795 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2780) |
| <a id="map-orig-v26-l2796"></a>V26:138 | 17. 能力—阶段映射 | L2796–L2827 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2796) |
| <a id="map-orig-v26-l2828"></a>V26:139 | 18. 依赖与非法组合 | L2828–L2888 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2828) |
| <a id="map-orig-v26-l2889"></a>V26:140 | 19. 验收体系 | L2889–L2890 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2889) |
| <a id="map-orig-v26-l2891"></a>V26:141 | 19.1 Extreme Code Duel 必测 | L2891–L2894 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2891) |
| <a id="map-orig-v26-l2895"></a>V26:142 | 19.1.1 基础验收自包含表与历史缺件 | L2895–L2926 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2895) |
| <a id="map-orig-v26-l2927"></a>V26:143 | X24 — Candidate Default-in | L2927–L2935 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2927) |
| <a id="map-orig-v26-l2936"></a>V26:144 | X25 — Proof Controls Severity | L2936–L2953 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2936) |
| <a id="map-orig-v26-l2954"></a>V26:145 | X26 — Fence Scope | L2954–L2969 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2954) |
| <a id="map-orig-v26-l2970"></a>V26:146 | X27 — Legal Reactivation | L2970–L2980 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2970) |
| <a id="map-orig-v26-l2981"></a>V26:147 | X28 — Budget Persistence | L2981–L2991 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2981) |
| <a id="map-orig-v26-l2992"></a>V26:148 | X29 — Multi-opponent Scale | L2992–L3000 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-v26-l2992) |
| <a id="map-orig-v26-l3001"></a>V26:149 | X30 — Host Safety | L3001–L3012 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3001) |
| <a id="map-orig-v26-l3013"></a>V26:150 | 19.2 Completion Gate | L3013–L3035 | skills / 完成标准(以一个标准的，带Native的秒杀Mod举例) | [原文](MS-SKILLS-PLAN.md#orig-v26-l3013) |
| <a id="map-orig-v26-l3036"></a>V26:151 | 19.3 V2.3 四项执行合同验收 | L3036–L3056 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3036) |
| <a id="map-orig-v26-l3057"></a>V26:152 | 19.4 V2.4 Design Freeze 收口验收 | L3057–L3067 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3057) |
| <a id="map-orig-v26-l3068"></a>V26:153 | 19.5 V2.5 Runtime Surface 与逐能力矩阵验收 | L3068–L3084 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3068) |
| <a id="map-orig-v26-l3085"></a>V26:154 | 19.6 V2.6 合同修订验收 | L3085–L3103 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3085) |
| <a id="map-orig-v26-l3104"></a>V26:155 | 20. 性能与资源约束 | L3104–L3105 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3104) |
| <a id="map-orig-v26-l3106"></a>V26:156 | 20.1 原则 | L3106–L3128 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3106) |
| <a id="map-orig-v26-l3129"></a>V26:157 | 20.2 最关键规则 | L3129–L3134 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3129) |
| <a id="map-orig-v26-l3135"></a>V26:158 | 20.3 诊断 | L3135–L3182 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3135) |
| <a id="map-orig-v26-l3183"></a>V26:159 | 20.4 全资源域安全验收 | L3183–L3201 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3183) |
| <a id="map-orig-v26-l3202"></a>V26:160 | 20.5 持久存储容量与恢复保留（V2.6） | L3202–L3219 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3202) |
| <a id="map-orig-v26-l3220"></a>V26:161 | 21. 与典型极限代码对抗能力对照 | L3220–L3253 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3220) |
| <a id="map-orig-v26-l3254"></a>V26:162 | 22. JVM 与宿主硬边界 | L3254–L3255 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3254) |
| <a id="map-orig-v26-l3256"></a>V26:163 | 22.1 同权限 Native/JVMTI | L3256–L3269 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3256) |
| <a id="map-orig-v26-l3270"></a>V26:164 | 22.2 JVM 死亡 | L3270–L3281 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3270) |
| <a id="map-orig-v26-l3282"></a>V26:165 | 22.3 OS / 文件 / 无关进程 | L3282–L3285 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3282) |
| <a id="map-orig-v26-l3286"></a>V26:166 | 22.4 极限代码对抗的正式边界 | L3286–L3291 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3286) |
| <a id="map-orig-v26-l3292"></a>V26:167 | 23. 交付物 | L3292–L3293 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3292) |
| <a id="map-orig-v26-l3294"></a>V26:168 | 23.1 7.16 | L3294–L3324 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3294) |
| <a id="map-orig-v26-l3325"></a>V26:169 | 23.2 7.17 | L3325–L3371 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3325) |
| <a id="map-orig-v26-l3372"></a>V26:170 | 23.3 L6/L6E | L3372–L3390 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-v26-l3372) |
| <a id="map-orig-v26-l3391"></a>V26:171 | 24. 最终完成判定 | L3391–L3460 | skills / 完成标准(以一个标准的，带Native的秒杀Mod举例) | [原文](MS-SKILLS-PLAN.md#orig-v26-l3391) |

<a id="source-full"></a>

### FULL · Ronova_7.17_Extreme_Full_Plane_Enhancement_Plan_V1.0_Internal_Root.md

原路径：`docs/plans/Ronova_7.17_Extreme_Full_Plane_Enhancement_Plan_V1.0_Internal_Root.md`。 [原始快照](../archive/plans-before-ms-restructure-r66/Ronova_7.17_Extreme_Full_Plane_Enhancement_Plan_V1.0_Internal_Root.md)。

| 映射锚点 | 旧标题（原编号不变） | 旧行范围 | 新 MS 章节 | 新正文落点 |
|---|---|---|---|---|
| <a id="map-orig-full-l0001"></a>FULL:1 | Ronova 7.17 极限档全体系加强规划 V1.0 | L1–L16 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-full-l0001) |
| <a id="map-orig-full-l0017"></a>FULL:2 | 1. 用户要求与目标 | L17–L35 | msitem / 先定义击杀契约 | [原文](MS-MSITEM-PLAN.md#orig-full-l0017) |
| <a id="map-orig-full-l0036"></a>FULL:3 | 1.1 极限档的定义 | L36–L48 | msitem / 先定义击杀契约 | [原文](MS-MSITEM-PLAN.md#orig-full-l0036) |
| <a id="map-orig-full-l0049"></a>FULL:4 | 1.2 当前能力与加强空间 | L49–L62 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-full-l0049) |
| <a id="map-orig-full-l0063"></a>FULL:5 | 1.3 后续 L6／L6E 加强入口 | L63–L69 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-full-l0063) |
| <a id="map-orig-full-l0070"></a>FULL:6 | 2. 全部高级能力的极限目标 | L70–L71 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-full-l0070) |
| <a id="map-orig-full-l0072"></a>FULL:7 | 2.1 攻击：15 项 | L72–L91 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-full-l0072) |
| <a id="map-orig-full-l0092"></a>FULL:8 | 2.2 拦截：10 项 | L92–L106 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-full-l0092) |
| <a id="map-orig-full-l0107"></a>FULL:9 | 2.3 防护：17 项 | L107–L128 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-full-l0107) |
| <a id="map-orig-full-l0129"></a>FULL:10 | 2.4 运行时：12 项 | L129–L145 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-full-l0129) |
| <a id="map-orig-full-l0146"></a>FULL:11 | 3. 普通动作同样进入极限范围 | L146–L159 | msitem / 列出全部击杀入口 | [原文](MS-MSITEM-PLAN.md#orig-full-l0146) |
| <a id="map-orig-full-l0160"></a>FULL:12 | 4. 这次已接入的源码 | L160–L188 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-full-l0160) |
| <a id="map-orig-full-l0189"></a>FULL:13 | 5. 进一步加强路径 | L189–L210 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-full-l0189) |
| <a id="map-orig-full-l0211"></a>FULL:14 | 6. 实现与文档约定 | L211–L221 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-full-l0211) |
| <a id="map-orig-full-l0222"></a>FULL:15 | 7. 完成与验证 | L222–L241 | skills / 完成标准(以一个标准的，带Native的秒杀Mod举例) | [原文](MS-SKILLS-PLAN.md#orig-full-l0222) |
| <a id="map-orig-full-l0242"></a>FULL:16 | 8. 参考 Mod 研究转入加强规划 | L242–L243 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-full-l0242) |
| <a id="map-orig-full-l0244"></a>FULL:17 | 8.1 研究依据与采纳边界 | L244–L266 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-full-l0244) |
| <a id="map-orig-full-l0267"></a>FULL:18 | 8.2 必须落实的 14 项加强 | L267–L289 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-full-l0267) |
| <a id="map-orig-full-l0290"></a>FULL:19 | 8.3 不采纳的实现方式 | L290–L299 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-full-l0290) |
| <a id="map-orig-full-l0300"></a>FULL:20 | 9. 研究加强项的阶段与验收 | L300–L301 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-full-l0300) |
| <a id="map-orig-full-l0302"></a>FULL:21 | 9.1 阶段安排 | L302–L309 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-full-l0302) |
| <a id="map-orig-full-l0310"></a>FULL:22 | 9.2 必须覆盖的正负例 | L310–L337 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-full-l0310) |

<a id="source-tier"></a>

### TIER · Ronova_7.17_Extreme_Tier_Strength_Implementation_Plan_V1.0_Internal_Root.md

原路径：`docs/plans/Ronova_7.17_Extreme_Tier_Strength_Implementation_Plan_V1.0_Internal_Root.md`。 [原始快照](../archive/plans-before-ms-restructure-r66/Ronova_7.17_Extreme_Tier_Strength_Implementation_Plan_V1.0_Internal_Root.md)。

| 映射锚点 | 旧标题（原编号不变） | 旧行范围 | 新 MS 章节 | 新正文落点 |
|---|---|---|---|---|
| <a id="map-orig-tier-l0001"></a>TIER:1 | Ronova 7.17 极限档强度实施规划 V1.0 — Internal Root | L1–L26 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0001) |
| <a id="map-orig-tier-l0027"></a>TIER:2 | 0. 本文定位 | L27–L33 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0027) |
| <a id="map-orig-tier-l0034"></a>TIER:3 | 0.1 术语约定 | L34–L46 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-tier-l0034) |
| <a id="map-orig-tier-l0047"></a>TIER:4 | 1. 问题陈述（实测，全部带行号） | L47–L48 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0047) |
| <a id="map-orig-tier-l0049"></a>TIER:5 | 1.1 配置层看似拉满 | L49–L57 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0049) |
| <a id="map-orig-tier-l0058"></a>TIER:6 | 1.2 天花板默认 OFF，且只有 6 个键会被抬升 | L58–L69 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0058) |
| <a id="map-orig-tier-l0070"></a>TIER:7 | 1.3 后果 A：48 个键恒不可用 | L70–L84 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0070) |
| <a id="map-orig-tier-l0085"></a>TIER:8 | 1.4 后果 B：等级高低在运行期只有 2 处消费点 | L85–L94 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0085) |
| <a id="map-orig-tier-l0095"></a>TIER:9 | 1.5 后果 C：3 个"极限档才该解锁"的步骤在 `resolve()` 路径恒不可执行 | L95–L105 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0095) |
| <a id="map-orig-tier-l0106"></a>TIER:10 | 1.6 后果 D：两套权限路径并存 | L106–L118 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0106) |
| <a id="map-orig-tier-l0119"></a>TIER:11 | 1.7 后果 E：表面成熟度框架被写死在下两档 | L119–L131 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0119) |
| <a id="map-orig-tier-l0132"></a>TIER:12 | 1.8 后果 F（**首要发现**）：极限档从未按规范实现——请求侧被压成了统一映射 | L132–L168 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0132) |
| <a id="map-orig-tier-l0169"></a>TIER:13 | 规范定义的"极限"是什么 | L169–L188 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0169) |
| <a id="map-orig-tier-l0189"></a>TIER:14 | 该机制的零件已存在，只是未与档位语义连接 | L189–L200 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0189) |
| <a id="map-orig-tier-l0201"></a>TIER:15 | 1.9 结论 | L201–L222 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0201) |
| <a id="map-orig-tier-l0223"></a>TIER:16 | 2. 目标与非目标 | L223–L224 | msitem / 先定义击杀契约 | [原文](MS-MSITEM-PLAN.md#orig-tier-l0223) |
| <a id="map-orig-tier-l0225"></a>TIER:17 | 2.1 目标 | L225–L237 | msitem / 先定义击杀契约 | [原文](MS-MSITEM-PLAN.md#orig-tier-l0225) |
| <a id="map-orig-tier-l0238"></a>TIER:18 | 2.2 非目标（明确不做） | L238–L254 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0238) |
| <a id="map-orig-tier-l0255"></a>TIER:19 | 3. 阶段划分 | L255–L256 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0255) |
| <a id="map-orig-tier-l0257"></a>TIER:20 | P0 —— 矩阵 revision 2：把逐键语义实施出来（**合同缺口，最高优先**） | L257–L288 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-tier-l0257) |
| <a id="map-orig-tier-l0289"></a>TIER:21 | P1 —— 天花板管道（**执行侧一切的前置**） | L289–L310 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0289) |
| <a id="map-orig-tier-l0311"></a>TIER:22 | P2 —— 逐键举证与执行接线（收益最大） | L311–L327 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0311) |
| <a id="map-orig-tier-l0328"></a>TIER:23 | P2-A 攻击平面（当前 0 / 15） | L328–L349 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-tier-l0328) |
| <a id="map-orig-tier-l0350"></a>TIER:24 | P2-B 防护平面（当前 1 / 17） | L350–L366 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0350) |
| <a id="map-orig-tier-l0367"></a>TIER:25 | P2-C 拦截平面（当前 1 / 10） | L367–L381 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0367) |
| <a id="map-orig-tier-l0382"></a>TIER:26 | P2-D 运行时平面（当前 4 / 12，补 6） | L382–L393 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0382) |
| <a id="map-orig-tier-l0394"></a>TIER:27 | P3 —— 接通表面成熟度 | L394–L408 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0394) |
| <a id="map-orig-tier-l0409"></a>TIER:28 | P4 —— 权限路径合并与报告修正 | L409–L423 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0409) |
| <a id="map-orig-tier-l0424"></a>TIER:29 | 4. 阶段依赖与顺序 | L424–L441 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0424) |
| <a id="map-orig-tier-l0442"></a>TIER:30 | 5. 验收与证据要求 | L442–L455 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0442) |
| <a id="map-orig-tier-l0456"></a>TIER:31 | 6. 不采纳的实现方式（红线） | L456–L457 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0456) |
| <a id="map-orig-tier-l0458"></a>TIER:32 | 6.1 四类禁止的"增强" | L458–L466 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0458) |
| <a id="map-orig-tier-l0467"></a>TIER:33 | 6.2 特别提示：举证退化的两种形态 | L467–L474 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0467) |
| <a id="map-orig-tier-l0475"></a>TIER:34 | 6.3 明确不在本文范围 | L475–L483 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0475) |
| <a id="map-orig-tier-l0484"></a>TIER:35 | 7. 完成判定 | L484–L502 | skills / 完成标准(以一个标准的，带Native的秒杀Mod举例) | [原文](MS-SKILLS-PLAN.md#orig-tier-l0484) |
| <a id="map-orig-tier-l0503"></a>TIER:36 | 第二部：加强路线（第一部 P0–P4 完成之后） | L503–L509 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0503) |
| <a id="map-orig-tier-l0510"></a>TIER:37 | 8. 第一部完成时的状态与暴露出的新缺口 | L510–L511 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0510) |
| <a id="map-orig-tier-l0512"></a>TIER:38 | 8.1 完成状态 | L512–L524 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0512) |
| <a id="map-orig-tier-l0525"></a>TIER:39 | 8.2 暴露出的新缺口 | L525–L540 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0525) |
| <a id="map-orig-tier-l0541"></a>TIER:40 | 8.3 第二部的六个阶段与单向依赖 | L541–L561 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0541) |
| <a id="map-orig-tier-l0562"></a>TIER:41 | 9. Q0 —— 候选一等公民 | L562–L585 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-tier-l0562) |
| <a id="map-orig-tier-l0586"></a>TIER:42 | 10. Q1 —— MutationTrace 生产实现 | L586–L620 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0586) |
| <a id="map-orig-tier-l0621"></a>TIER:43 | 11. Q2 —— ResultGraph 生产实现 | L621–L655 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0621) |
| <a id="map-orig-tier-l0656"></a>TIER:44 | 12. Q3 —— ExistenceTerminationCertificate | L656–L691 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-tier-l0656) |
| <a id="map-orig-tier-l0692"></a>TIER:45 | 13. Q4 —— 证明主线 | L692–L714 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0692) |
| <a id="map-orig-tier-l0715"></a>TIER:46 | 14. Q5 —— 深度解锁 | L715–L734 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0715) |
| <a id="map-orig-tier-l0735"></a>TIER:47 | 15. 第二部的红线 | L735–L748 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0735) |
| <a id="map-orig-tier-l0749"></a>TIER:48 | 16. 明确超出本文范围（需上层决策或平台能力） | L749–L761 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0749) |
| <a id="map-orig-tier-l0762"></a>TIER:49 | 17. 第二部完成判定 | L762–L777 | skills / 完成标准(以一个标准的，带Native的秒杀Mod举例) | [原文](MS-SKILLS-PLAN.md#orig-tier-l0762) |
| <a id="map-orig-tier-l0778"></a>TIER:50 | 附：本文引用的实测证据索引 | L778–L779 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0778) |
| <a id="map-orig-tier-l0780"></a>TIER:51 | 代码 | L780–L800 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0780) |
| <a id="map-orig-tier-l0801"></a>TIER:52 | 规范 | L801–L814 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0801) |
| <a id="map-orig-tier-l0815"></a>TIER:53 | 规范（第二部） | L815–L833 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-tier-l0815) |

<a id="source-rec"></a>

### REC · Ronova_7.17_Generic_Recovery_Chain_Implementation_Plan_V1.0_Internal_Root.md

原路径：`docs/plans/Ronova_7.17_Generic_Recovery_Chain_Implementation_Plan_V1.0_Internal_Root.md`。 [原始快照](../archive/plans-before-ms-restructure-r66/Ronova_7.17_Generic_Recovery_Chain_Implementation_Plan_V1.0_Internal_Root.md)。

| 映射锚点 | 旧标题（原编号不变） | 旧行范围 | 新 MS 章节 | 新正文落点 |
|---|---|---|---|---|
| <a id="map-orig-rec-l0001"></a>REC:1 | Ronova 7.17 通用恢复链处置实施规划 V1.0 | L1–L44 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-rec-l0001) |
| <a id="map-orig-rec-l0045"></a>REC:2 | 1. 要解决的行为 | L45–L63 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0045) |
| <a id="map-orig-rec-l0064"></a>REC:3 | 2. 通用识别依据 | L64–L90 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0064) |
| <a id="map-orig-rec-l0091"></a>REC:4 | 2.1 记录到重建输入的证明 | L91–L114 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0091) |
| <a id="map-orig-rec-l0115"></a>REC:5 | 3. 接入现有操作与开关 | L115–L136 | msitem / 列出全部击杀入口 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0115) |
| <a id="map-orig-rec-l0137"></a>REC:6 | 3.1 三档恢复链强度 | L137–L161 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0137) |
| <a id="map-orig-rec-l0162"></a>REC:7 | 4. 源码落点与责任 | L162–L179 | msitem / 建立职责边界 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0162) |
| <a id="map-orig-rec-l0180"></a>REC:8 | 5. 同一 Root 下的执行顺序 | L180–L195 | msitem / 服务端队列与线程边界 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0180) |
| <a id="map-orig-rec-l0196"></a>REC:9 | 6. 数据、任务和资源的真实处置 | L196–L197 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0196) |
| <a id="map-orig-rec-l0198"></a>REC:10 | 6.1 精确恢复记录 | L198–L211 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0198) |
| <a id="map-orig-rec-l0212"></a>REC:11 | 6.2 持久保存 | L212–L222 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-rec-l0212) |
| <a id="map-orig-rec-l0223"></a>REC:12 | 6.3 延迟任务与回调 | L223–L232 | msitem / 服务端队列与线程边界 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0223) |
| <a id="map-orig-rec-l0233"></a>REC:13 | 6.4 区块票据及其他资源 | L233–L239 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-rec-l0233) |
| <a id="map-orig-rec-l0240"></a>REC:14 | 7. 证据、效果身份与恢复 | L240–L279 | msitem / 去重与幂等 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0240) |
| <a id="map-orig-rec-l0280"></a>REC:15 | 8. 明确解除与新生效范围 | L280–L292 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0280) |
| <a id="map-orig-rec-l0293"></a>REC:16 | 9. 预算、退出与完成条件 | L293–L316 | msitem / 完成标准 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0293) |
| <a id="map-orig-rec-l0317"></a>REC:17 | 10. 实施顺序与阶段验证 | L317–L347 | msitem / 从零实现顺序 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0317) |
| <a id="map-orig-rec-l0348"></a>REC:18 | 11. 已识别的设计边界 | L348–L380 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rec-l0348) |
| <a id="map-orig-rec-l0381"></a>REC:19 | 12. 参考 Mod 研究转入第一阶段 | L381–L388 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0381) |
| <a id="map-orig-rec-l0389"></a>REC:20 | 12.1 来源身份、缓存和再发布 | L389–L406 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0389) |
| <a id="map-orig-rec-l0407"></a>REC:21 | 12.2 任务链按对象和执行轮次结算 | L407–L425 | msitem / 服务端队列与线程边界 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0407) |
| <a id="map-orig-rec-l0426"></a>REC:22 | 12.3 ACK、最终提交与旧资格失效 | L426–L440 | msitem / 服务端队列与线程边界 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0426) |
| <a id="map-orig-rec-l0441"></a>REC:23 | 12.4 精确记录、保存与跨重启 | L441–L456 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-rec-l0441) |
| <a id="map-orig-rec-l0457"></a>REC:24 | 12.5 资源、诊断和预算是完成前置条件 | L457–L471 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-rec-l0457) |
| <a id="map-orig-rec-l0472"></a>REC:25 | 12.6 同一批次的出口和证据 | L472–L487 | msitem / 验证矩阵 | [原文](MS-MSITEM-PLAN.md#orig-rec-l0472) |

<a id="source-max"></a>

### MAX · Ronova_Compliant_Maximum_Strength_Plan_V1.0.md

原路径：`docs/plans/Ronova_Compliant_Maximum_Strength_Plan_V1.0.md`。 [原始快照](../archive/plans-before-ms-restructure-r66/Ronova_Compliant_Maximum_Strength_Plan_V1.0.md)。

| 映射锚点 | 旧标题（原编号不变） | 旧行范围 | 新 MS 章节 | 新正文落点 |
|---|---|---|---|---|
| <a id="map-orig-max-l0001"></a>MAX:1 | Ronova 合规极限加强方案 V1.0 | L1–L8 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-max-l0001) |
| <a id="map-orig-max-l0009"></a>MAX:2 | 0. 本方案的判断 | L9–L29 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-max-l0009) |
| <a id="map-orig-max-l0030"></a>MAX:3 | 0.1 合规红线（本方案全程遵守） | L30–L47 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-max-l0030) |
| <a id="map-orig-max-l0048"></a>MAX:4 | 1. L1 前置层：抢到 Forge 与 Mixin 之前（替代被删的 AutoAttach，且更强） | L48–L49 | skills / 对于非MoMain优先级 | [原文](MS-SKILLS-PLAN.md#orig-max-l0048) |
| <a id="map-orig-max-l0050"></a>MAX:5 | 1.1 为什么这一层最划算 | L50–L60 | skills / 对于非MoMain优先级 | [原文](MS-SKILLS-PLAN.md#orig-max-l0050) |
| <a id="map-orig-max-l0061"></a>MAX:6 | 1.2 交付物 | L61–L71 | skills / 对于非MoMain优先级 | [原文](MS-SKILLS-PLAN.md#orig-max-l0061) |
| <a id="map-orig-max-l0072"></a>MAX:7 | 1.3 合规性论证 | L72–L88 | skills / 对于非MoMain优先级 | [原文](MS-SKILLS-PLAN.md#orig-max-l0072) |
| <a id="map-orig-max-l0089"></a>MAX:8 | 1.4 验收 | L89–L97 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-max-l0089) |
| <a id="map-orig-max-l0098"></a>MAX:9 | 2. L2 原生深化：扩充 JVMTI 能力位，覆盖 JIT 与字节码层 | L98–L99 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-max-l0098) |
| <a id="map-orig-max-l0100"></a>MAX:10 | 2.1 现状与缺口 | L100–L113 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-max-l0100) |
| <a id="map-orig-max-l0114"></a>MAX:11 | 2.2 交付物 | L114–L124 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-max-l0114) |
| <a id="map-orig-max-l0125"></a>MAX:12 | 2.3 为什么这条最"狠"而完全合规 | L125–L132 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-max-l0125) |
| <a id="map-orig-max-l0133"></a>MAX:13 | 2.4 合规性论证 | L133–L141 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-max-l0133) |
| <a id="map-orig-max-l0142"></a>MAX:14 | 2.5 验收 | L142–L149 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-max-l0142) |
| <a id="map-orig-max-l0150"></a>MAX:15 | 3. L3 反制面：把五个样本的手法变成检测项（纯防御） | L150–L151 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-max-l0150) |
| <a id="map-orig-max-l0152"></a>MAX:16 | 3.1 这一层是"用样本的强度反哺防御" | L152–L156 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-max-l0152) |
| <a id="map-orig-max-l0157"></a>MAX:17 | 3.2 检测项清单 | L157–L171 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-max-l0157) |
| <a id="map-orig-max-l0172"></a>MAX:18 | 3.3 合规性论证 | L172–L178 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-max-l0172) |
| <a id="map-orig-max-l0179"></a>MAX:19 | 3.4 验收 | L179–L187 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-max-l0179) |
| <a id="map-orig-max-l0188"></a>MAX:20 | 4. L4 证明强度：从"逐类哈希"升级为"不变量级证明" | L188–L189 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-max-l0188) |
| <a id="map-orig-max-l0190"></a>MAX:21 | 4.1 现状与规范要求 | L190–L202 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-max-l0190) |
| <a id="map-orig-max-l0203"></a>MAX:22 | 4.2 交付物 | L203–L211 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-max-l0203) |
| <a id="map-orig-max-l0212"></a>MAX:23 | 4.3 合规性论证 | L212–L217 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-max-l0212) |
| <a id="map-orig-max-l0218"></a>MAX:24 | 4.4 验收 | L218–L225 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-max-l0218) |
| <a id="map-orig-max-l0226"></a>MAX:25 | 5. L5 执行深度：候选图 + 跨表面追踪 + 完成证书 | L226–L227 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-max-l0226) |
| <a id="map-orig-max-l0228"></a>MAX:26 | 5.1 现状 | L228–L238 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-max-l0228) |
| <a id="map-orig-max-l0239"></a>MAX:27 | 5.2 交付物 | L239–L247 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-max-l0239) |
| <a id="map-orig-max-l0248"></a>MAX:28 | 5.3 样本反哺：必须能击败的具体恢复路径 | L248–L262 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-max-l0248) |
| <a id="map-orig-max-l0263"></a>MAX:29 | 5.4 合规性论证 | L263–L269 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-max-l0263) |
| <a id="map-orig-max-l0270"></a>MAX:30 | 5.5 验收 | L270–L276 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-max-l0270) |
| <a id="map-orig-max-l0277"></a>MAX:31 | 6. L6 原生强制：让强制不再回调 Java | L277–L278 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-max-l0277) |
| <a id="map-orig-max-l0279"></a>MAX:32 | 6.1 现状 | L279–L286 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-max-l0279) |
| <a id="map-orig-max-l0287"></a>MAX:33 | 6.2 交付物 | L287–L294 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-max-l0287) |
| <a id="map-orig-max-l0295"></a>MAX:34 | 6.3 合规性论证 | L295–L303 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-max-l0295) |
| <a id="map-orig-max-l0304"></a>MAX:35 | 7. 明确不采纳（尽管样本在用） | L304–L322 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-max-l0304) |
| <a id="map-orig-max-l0323"></a>MAX:36 | 8. 阶段与依赖 | L323–L351 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-max-l0323) |
| <a id="map-orig-max-l0352"></a>MAX:37 | 9. 与既有三份规划的关系 | L352–L364 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-max-l0352) |
| <a id="map-orig-max-l0365"></a>MAX:38 | 10. 完成判定 | L365–L381 | skills / 完成标准(以一个标准的，带Native的秒杀Mod举例) | [原文](MS-SKILLS-PLAN.md#orig-max-l0365) |

<a id="source-cf"></a>

### CF · Ronova_L6E-0_Contract_Freeze_V1.3_Internal_Root.md

原路径：`docs/plans/Ronova_L6E-0_Contract_Freeze_V1.3_Internal_Root.md`。 [原始快照](../archive/plans-before-ms-restructure-r66/Ronova_L6E-0_Contract_Freeze_V1.3_Internal_Root.md)。

| 映射锚点 | 旧标题（原编号不变） | 旧行范围 | 新 MS 章节 | 新正文落点 |
|---|---|---|---|---|
| <a id="map-orig-cf-l0001"></a>CF:1 | Ronova — L6E-0 Contract Freeze V1.3 — Internal Root | L1–L26 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0001) |
| <a id="map-orig-cf-l0027"></a>CF:2 | 0. Non-Goals | L27–L55 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0027) |
| <a id="map-orig-cf-l0056"></a>CF:3 | 1. Frozen Architecture Position | L56–L77 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0056) |
| <a id="map-orig-cf-l0078"></a>CF:4 | 2. Global Redlines | L78–L81 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0078) |
| <a id="map-orig-cf-l0082"></a>CF:5 | R1 — No Mod-ID Authority | L82–L99 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0082) |
| <a id="map-orig-cf-l0100"></a>CF:6 | R2 — Non-Blocking ≠ Hookless | L100–L116 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0100) |
| <a id="map-orig-cf-l0117"></a>CF:7 | R3 — Hardened 必须是 Non-Blocking Core 的超集 | L117–L139 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-cf-l0117) |
| <a id="map-orig-cf-l0140"></a>CF:8 | R4 — Server Authority ≠ Client Pixel Authority | L140–L159 | skills / 客户端 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0140) |
| <a id="map-orig-cf-l0160"></a>CF:9 | R5 — Unknown ≠ False ≠ Safe | L160–L177 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0160) |
| <a id="map-orig-cf-l0178"></a>CF:10 | R6 — Repair Success ≠ Trust Restored | L178–L192 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0178) |
| <a id="map-orig-cf-l0193"></a>CF:11 | R7 — Internal Root Process-Role Boundary | L193–L210 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0193) |
| <a id="map-orig-cf-l0211"></a>CF:12 | 3. Canonical Identifier Model | L211–L212 | entitydefence / 保留一个规范实例 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0211) |
| <a id="map-orig-cf-l0213"></a>CF:13 | 3.1 RealmId | L213–L225 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0213) |
| <a id="map-orig-cf-l0226"></a>CF:14 | 3.2 SubjectKey 与 SubjectIdentity | L226–L257 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0226) |
| <a id="map-orig-cf-l0258"></a>CF:15 | 3.3 PhysicalIncarnationId | L258–L281 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0258) |
| <a id="map-orig-cf-l0282"></a>CF:16 | 3.4 BindingRevision / OwnershipRevision | L282–L305 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0282) |
| <a id="map-orig-cf-l0306"></a>CF:17 | 3.5 ProjectionRevision | L306–L318 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0306) |
| <a id="map-orig-cf-l0319"></a>CF:18 | 3.5.1 PolicyRevision | L319–L324 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-cf-l0319) |
| <a id="map-orig-cf-l0325"></a>CF:19 | 3.6 CriticalOperationId | L325–L350 | msitem / 用统一动作上下文消除重复 | [原文](MS-MSITEM-PLAN.md#orig-cf-l0325) |
| <a id="map-orig-cf-l0351"></a>CF:20 | 3.7 CompletionContractId | L351–L372 | skills / 完成标准(以一个标准的，带Native的秒杀Mod举例) | [原文](MS-SKILLS-PLAN.md#orig-cf-l0351) |
| <a id="map-orig-cf-l0373"></a>CF:21 | 4. DominionEpoch / Fence / Sequence 分离 | L373–L397 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0373) |
| <a id="map-orig-cf-l0398"></a>CF:22 | 5. Root-Compatible Forward Envelope | L398–L437 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0398) |
| <a id="map-orig-cf-l0438"></a>CF:23 | 5.1 Unknown Field Rule | L438–L441 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0438) |
| <a id="map-orig-cf-l0442"></a>CF:24 | 5.2 Extension Namespace | L442–L453 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0442) |
| <a id="map-orig-cf-l0454"></a>CF:25 | 5.3 Dormant Field Rule | L454–L457 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0454) |
| <a id="map-orig-cf-l0458"></a>CF:26 | 6. P0-A → P0-B Contract | L458–L507 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0458) |
| <a id="map-orig-cf-l0508"></a>CF:27 | 7. ExecutionAuthorityContext Contract | L508–L537 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0508) |
| <a id="map-orig-cf-l0538"></a>CF:28 | 7.1 Revision Crossing Policy | L538–L554 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0538) |
| <a id="map-orig-cf-l0555"></a>CF:29 | 7.2 CompletionContract | L555–L571 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0555) |
| <a id="map-orig-cf-l0572"></a>CF:30 | 8. ResolverProvider SPI Contract | L572–L612 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-cf-l0572) |
| <a id="map-orig-cf-l0613"></a>CF:31 | 8.1 Relationship Edge Lifecycle | L613–L640 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-cf-l0613) |
| <a id="map-orig-cf-l0641"></a>CF:32 | 9. Pre-Admission Binding Contract | L641–L665 | entitydefence / 持久管理与生命周期 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0641) |
| <a id="map-orig-cf-l0666"></a>CF:33 | 9.1 Admission Levels | L666–L671 | entitydefence / 持久管理与生命周期 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0666) |
| <a id="map-orig-cf-l0672"></a>CF:34 | 9.2 Forge Seam Mapping | L672–L681 | entitydefence / 持久管理与生命周期 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0672) |
| <a id="map-orig-cf-l0682"></a>CF:35 | 9.3 Root Unavailable / Fenced | L682–L691 | entitydefence / 持久管理与生命周期 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0682) |
| <a id="map-orig-cf-l0692"></a>CF:36 | 10. Mode Contract | L692–L703 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-cf-l0692) |
| <a id="map-orig-cf-l0704"></a>CF:37 | 10.1 OBSERVATIONAL | L704–L716 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-cf-l0704) |
| <a id="map-orig-cf-l0717"></a>CF:38 | 10.2 NON_BLOCKING | L717–L743 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-cf-l0717) |
| <a id="map-orig-cf-l0744"></a>CF:39 | 10.3 HARDENED | L744–L760 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-cf-l0744) |
| <a id="map-orig-cf-l0761"></a>CF:40 | 11. Mode Scope | L761–L794 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-cf-l0761) |
| <a id="map-orig-cf-l0795"></a>CF:41 | 12. Mode Change Authority | L795–L823 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-cf-l0795) |
| <a id="map-orig-cf-l0824"></a>CF:42 | 13. Policy Constitution | L824–L857 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l0824) |
| <a id="map-orig-cf-l0858"></a>CF:43 | 14. Liveness Contract | L858–L875 | entitydefence / 构建幂等修复过程 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0858) |
| <a id="map-orig-cf-l0876"></a>CF:44 | 14.1 Liveness States | L876–L887 | entitydefence / 构建幂等修复过程 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0876) |
| <a id="map-orig-cf-l0888"></a>CF:45 | 14.2 Liveness Safety Rule | L888–L906 | entitydefence / 构建幂等修复过程 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0888) |
| <a id="map-orig-cf-l0907"></a>CF:46 | 14.3 CriticalInvariant Contract | L907–L934 | entitydefence / 修复前的状态不变量 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0907) |
| <a id="map-orig-cf-l0935"></a>CF:47 | 15. DeviationAccumulator Contract | L935–L964 | entitydefence / 构建幂等修复过程 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0935) |
| <a id="map-orig-cf-l0965"></a>CF:48 | 15.1 Required Operations | L965–L978 | entitydefence / 构建幂等修复过程 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0965) |
| <a id="map-orig-cf-l0979"></a>CF:49 | 15.2 Exact Mode Semantics | L979–L988 | entitydefence / 构建幂等修复过程 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0979) |
| <a id="map-orig-cf-l0989"></a>CF:50 | 15.3 Bounds and Overflow | L989–L1010 | entitydefence / 构建幂等修复过程 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0989) |
| <a id="map-orig-cf-l1011"></a>CF:51 | 16. Side-Effect Contract | L1011–L1023 | msitem / 去重与幂等 | [原文](MS-MSITEM-PLAN.md#orig-cf-l1011) |
| <a id="map-orig-cf-l1024"></a>CF:52 | 16.1 RONOVA_CONTROLLED | L1024–L1042 | msitem / 去重与幂等 | [原文](MS-MSITEM-PLAN.md#orig-cf-l1024) |
| <a id="map-orig-cf-l1043"></a>CF:53 | 16.2 REVERSIBLE | L1043–L1054 | msitem / 去重与幂等 | [原文](MS-MSITEM-PLAN.md#orig-cf-l1043) |
| <a id="map-orig-cf-l1055"></a>CF:54 | 16.3 COMPENSATABLE | L1055–L1068 | msitem / 去重与幂等 | [原文](MS-MSITEM-PLAN.md#orig-cf-l1055) |
| <a id="map-orig-cf-l1069"></a>CF:55 | 16.4 EXTERNAL_OPAQUE | L1069–L1084 | msitem / 去重与幂等 | [原文](MS-MSITEM-PLAN.md#orig-cf-l1069) |
| <a id="map-orig-cf-l1085"></a>CF:56 | 17. Relationship Graph Trust Contract | L1085–L1107 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1085) |
| <a id="map-orig-cf-l1108"></a>CF:57 | 18. Integrity Failure Contract | L1108–L1131 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1108) |
| <a id="map-orig-cf-l1132"></a>CF:58 | 19. COMMIT_UNKNOWN Contract | L1132–L1148 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1132) |
| <a id="map-orig-cf-l1149"></a>CF:59 | 20. Server / Client Boundary Contract | L1149–L1174 | skills / 客户端 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1149) |
| <a id="map-orig-cf-l1175"></a>CF:60 | 20.1 Client Extension Namespace | L1175–L1193 | skills / 客户端 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1175) |
| <a id="map-orig-cf-l1194"></a>CF:61 | 21. Render Server Contract | L1194–L1214 | skills / 客户端 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1194) |
| <a id="map-orig-cf-l1215"></a>CF:62 | 22. Seam Contract | L1215–L1243 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1215) |
| <a id="map-orig-cf-l1244"></a>CF:63 | 23. Versioning Contract | L1244–L1265 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1244) |
| <a id="map-orig-cf-l1266"></a>CF:64 | 24. Audit / Provenance Contract | L1266–L1296 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1266) |
| <a id="map-orig-cf-l1297"></a>CF:65 | 25. L6E-0 Frozen Interface Set | L1297–L1337 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1297) |
| <a id="map-orig-cf-l1338"></a>CF:66 | 26. P0-A Implementation Spec Entry Criteria | L1338–L1361 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1338) |
| <a id="map-orig-cf-l1362"></a>CF:67 | 27. P0-A First Implementation Scope | L1362–L1399 | entitydefence / 从零实现顺序 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1362) |
| <a id="map-orig-cf-l1400"></a>CF:68 | 28. Acceptance Tests for L6E-0 | L1400–L1403 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1400) |
| <a id="map-orig-cf-l1404"></a>CF:69 | T-01 — Stale Incarnation | L1404–L1419 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1404) |
| <a id="map-orig-cf-l1420"></a>CF:70 | T-02 — Hardened Superset | L1420–L1436 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1420) |
| <a id="map-orig-cf-l1437"></a>CF:71 | T-03 — Unknown Extension | L1437–L1456 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1437) |
| <a id="map-orig-cf-l1457"></a>CF:72 | T-04 — COMMIT_UNKNOWN | L1457–L1468 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1457) |
| <a id="map-orig-cf-l1469"></a>CF:73 | T-05 — Opaque Relationship | L1469–L1480 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1469) |
| <a id="map-orig-cf-l1481"></a>CF:74 | T-06 — Liveness Overload | L1481–L1496 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1481) |
| <a id="map-orig-cf-l1497"></a>CF:75 | T-07 — Client Boundary | L1497–L1508 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1497) |
| <a id="map-orig-cf-l1509"></a>CF:76 | T-08 — Context Revision Crossing | L1509–L1514 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1509) |
| <a id="map-orig-cf-l1515"></a>CF:77 | T-09 — Resolver Revocation / Hostile Provider | L1515–L1520 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1515) |
| <a id="map-orig-cf-l1521"></a>CF:78 | T-10 — Pre-Admission Without Root | L1521–L1526 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1521) |
| <a id="map-orig-cf-l1527"></a>CF:79 | T-11 — DeviationAccumulator Semantics | L1527–L1532 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1527) |
| <a id="map-orig-cf-l1533"></a>CF:80 | T-12 — Canonical Envelope | L1533–L1538 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1533) |
| <a id="map-orig-cf-l1539"></a>CF:81 | T-13 — Immutable Projection View | L1539–L1544 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1539) |
| <a id="map-orig-cf-l1545"></a>CF:82 | T-14 — P0-D Minimum Crosscut | L1545–L1550 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1545) |
| <a id="map-orig-cf-l1551"></a>CF:83 | T-15 — Lifecycle / Resolution Orthogonality | L1551–L1556 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1551) |
| <a id="map-orig-cf-l1557"></a>CF:84 | T-16 — Production Process Boundary | L1557–L1562 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1557) |
| <a id="map-orig-cf-l1563"></a>CF:85 | 29. Freeze Decision Summary | L1563–L1566 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1563) |
| <a id="map-orig-cf-l1567"></a>CF:86 | ID-1 — Liveness | L1567–L1576 | entitydefence / 构建幂等修复过程 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1567) |
| <a id="map-orig-cf-l1577"></a>CF:87 | ID-2 — Side Effects | L1577–L1591 | msitem / 去重与幂等 | [原文](MS-MSITEM-PLAN.md#orig-cf-l1577) |
| <a id="map-orig-cf-l1592"></a>CF:88 | ID-3 — Relationship Graph | L1592–L1603 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1592) |
| <a id="map-orig-cf-l1604"></a>CF:89 | ID-4 — Mode Switching | L1604–L1615 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-cf-l1604) |
| <a id="map-orig-cf-l1616"></a>CF:90 | ID-5 — Internal Root Envelope | L1616–L1633 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1616) |
| <a id="map-orig-cf-l1634"></a>CF:91 | ID-6 — Render Boundary | L1634–L1649 | skills / 客户端 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1634) |
| <a id="map-orig-cf-l1650"></a>CF:92 | ID-7 — Four Shared Protocols | L1650–L1655 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1650) |
| <a id="map-orig-cf-l1656"></a>CF:93 | ID-8 — Integrity Crosscut | L1656–L1661 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1656) |
| <a id="map-orig-cf-l1662"></a>CF:94 | ID-9 — Production Process Boundary | L1662–L1667 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1662) |
| <a id="map-orig-cf-l1668"></a>CF:95 | 30. Final Freeze Statement | L1668–L1695 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-cf-l1668) |

<a id="source-p0a"></a>

### P0A · Ronova_L6E_P0-A_Truth_Authority_Implementation_Spec_V1.3_Internal_Root.md

原路径：`docs/plans/Ronova_L6E_P0-A_Truth_Authority_Implementation_Spec_V1.3_Internal_Root.md`。 [原始快照](../archive/plans-before-ms-restructure-r66/Ronova_L6E_P0-A_Truth_Authority_Implementation_Spec_V1.3_Internal_Root.md)。

| 映射锚点 | 旧标题（原编号不变） | 旧行范围 | 新 MS 章节 | 新正文落点 |
|---|---|---|---|---|
| <a id="map-orig-p0a-l0001"></a>P0A:1 | Ronova — L6E P0-A Truth Authority Implementation Spec V1.3 — Internal Root | L1–L25 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0001) |
| <a id="map-orig-p0a-l0026"></a>P0A:2 | 0. 上位依据 | L26–L51 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0026) |
| <a id="map-orig-p0a-l0052"></a>P0A:3 | 0.1 V1.3 Closure Map | L52–L67 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0052) |
| <a id="map-orig-p0a-l0068"></a>P0A:4 | 1. P0-A 的唯一职责 | L68–L89 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0068) |
| <a id="map-orig-p0a-l0090"></a>P0A:5 | 2. P0-A 核心不变量 | L90–L93 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0090) |
| <a id="map-orig-p0a-l0094"></a>P0A:6 | A1 — Subject Identity Stability | L94–L104 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0094) |
| <a id="map-orig-p0a-l0105"></a>P0A:7 | A2 — Incarnation Non-Inheritance | L105–L120 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0105) |
| <a id="map-orig-p0a-l0121"></a>P0A:8 | A3 — Binding Monotonicity | L121–L130 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0121) |
| <a id="map-orig-p0a-l0131"></a>P0A:9 | A4 — Tombstone Dominance | L131–L151 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-p0a-l0131) |
| <a id="map-orig-p0a-l0152"></a>P0A:10 | A5 — Root Writer Uniqueness | L152–L159 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0152) |
| <a id="map-orig-p0a-l0160"></a>P0A:11 | A6 — Projection Is Not Truth | L160–L170 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0160) |
| <a id="map-orig-p0a-l0171"></a>P0A:12 | A7 — Graph Is Not Root | L171–L182 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0171) |
| <a id="map-orig-p0a-l0183"></a>P0A:13 | A8 — Production Process Boundary (`PROC-INV-1`) | L183–L200 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0183) |
| <a id="map-orig-p0a-l0201"></a>P0A:14 | 3. 推荐包结构 | L201–L311 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0201) |
| <a id="map-orig-p0a-l0312"></a>P0A:15 | 4. SubjectKey / SubjectIdentity | L312–L313 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0312) |
| <a id="map-orig-p0a-l0314"></a>P0A:16 | 4.1 数据结构 | L314–L343 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0314) |
| <a id="map-orig-p0a-l0344"></a>P0A:17 | 4.2 SubjectType | L344–L366 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0344) |
| <a id="map-orig-p0a-l0367"></a>P0A:18 | 4.3 Identity Resolution | L367–L394 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0367) |
| <a id="map-orig-p0a-l0395"></a>P0A:19 | 5. PhysicalIncarnationId | L395–L396 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0395) |
| <a id="map-orig-p0a-l0397"></a>P0A:20 | 5.1 目的 | L397–L414 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0397) |
| <a id="map-orig-p0a-l0415"></a>P0A:21 | 5.2 推荐结构 | L415–L433 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0415) |
| <a id="map-orig-p0a-l0434"></a>P0A:22 | 5.3 Incarnation 状态 | L434–L459 | entitydefence / 修复前的状态不变量 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0434) |
| <a id="map-orig-p0a-l0460"></a>P0A:23 | 6. Incarnation Binding | L460–L461 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0460) |
| <a id="map-orig-p0a-l0462"></a>P0A:24 | 6.1 核心记录 | L462–L478 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0462) |
| <a id="map-orig-p0a-l0479"></a>P0A:25 | 6.2 Bind 规则 | L479–L497 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0479) |
| <a id="map-orig-p0a-l0498"></a>P0A:26 | 6.3 Rebind | L498–L509 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0498) |
| <a id="map-orig-p0a-l0510"></a>P0A:27 | 7. BindingRevision / OwnershipRevision | L510–L545 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0510) |
| <a id="map-orig-p0a-l0546"></a>P0A:28 | 8. OwnershipBinding | L546–L547 | entitydefence / 保留一个规范实例 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0546) |
| <a id="map-orig-p0a-l0548"></a>P0A:29 | 8.1 目的 | L548–L555 | entitydefence / 保留一个规范实例 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0548) |
| <a id="map-orig-p0a-l0556"></a>P0A:30 | 8.2 第一版 Scope | L556–L569 | entitydefence / 保留一个规范实例 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0556) |
| <a id="map-orig-p0a-l0570"></a>P0A:31 | 8.3 数据结构 | L570–L584 | entitydefence / 保留一个规范实例 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0570) |
| <a id="map-orig-p0a-l0585"></a>P0A:32 | 8.4 OwnershipState | L585–L605 | entitydefence / 保留一个规范实例 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0585) |
| <a id="map-orig-p0a-l0606"></a>P0A:33 | 9. ExecutionLease | L606–L633 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0606) |
| <a id="map-orig-p0a-l0634"></a>P0A:34 | 10. LifecycleTruth / ResolutionState / AdmissionDisposition | L634–L649 | msitem / 总体架构 | [原文](MS-MSITEM-PLAN.md#orig-p0a-l0634) |
| <a id="map-orig-p0a-l0650"></a>P0A:35 | 11. TombstoneRecord | L650–L674 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-p0a-l0650) |
| <a id="map-orig-p0a-l0675"></a>P0A:36 | 12. DominionEpoch | L675–L708 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0675) |
| <a id="map-orig-p0a-l0709"></a>P0A:37 | 13. Root Gateway | L709–L712 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0709) |
| <a id="map-orig-p0a-l0713"></a>P0A:38 | 13.1 Canonical Interface | L713–L743 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0713) |
| <a id="map-orig-p0a-l0744"></a>P0A:39 | 13.2 DominionCommand | L744–L758 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0744) |
| <a id="map-orig-p0a-l0759"></a>P0A:40 | 13.3 Closed Submit / Recovery Results | L759–L774 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0759) |
| <a id="map-orig-p0a-l0775"></a>P0A:41 | 14. RootCommandId 与幂等 | L775–L798 | msitem / 去重与幂等 | [原文](MS-MSITEM-PLAN.md#orig-p0a-l0775) |
| <a id="map-orig-p0a-l0799"></a>P0A:42 | 15. COMMIT_UNKNOWN | L799–L818 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0799) |
| <a id="map-orig-p0a-l0819"></a>P0A:43 | 16. AuthorityEnvelope | L819–L858 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0819) |
| <a id="map-orig-p0a-l0859"></a>P0A:44 | 17. Projection Model | L859–L860 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0859) |
| <a id="map-orig-p0a-l0861"></a>P0A:45 | 17.1 DominionProjection | L861–L881 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0861) |
| <a id="map-orig-p0a-l0882"></a>P0A:46 | 17.2 SubjectProjectionEntry | L882–L902 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0882) |
| <a id="map-orig-p0a-l0903"></a>P0A:47 | 18. Projection Publication | L903–L932 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0903) |
| <a id="map-orig-p0a-l0933"></a>P0A:48 | 19. ProjectionView | L933–L954 | entitydefence / 防御读取路径 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0933) |
| <a id="map-orig-p0a-l0955"></a>P0A:49 | 20. AuthoritySnapshot | L955–L984 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0955) |
| <a id="map-orig-p0a-l0985"></a>P0A:50 | 21. TruthAuthorityService | L985–L1001 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l0985) |
| <a id="map-orig-p0a-l1002"></a>P0A:51 | 22. IdentityBindingService | L1002–L1024 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1002) |
| <a id="map-orig-p0a-l1025"></a>P0A:52 | 23. Incarnation Discovery | L1025–L1048 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1025) |
| <a id="map-orig-p0a-l1049"></a>P0A:53 | 24. Pre-Admission Binding | L1049–L1069 | entitydefence / 持久管理与生命周期 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1049) |
| <a id="map-orig-p0a-l1070"></a>P0A:54 | 25. Terminal-aware Binding | L1070–L1095 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-p0a-l1070) |
| <a id="map-orig-p0a-l1096"></a>P0A:55 | 26. ResolverProvider SPI | L1096–L1097 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-p0a-l1096) |
| <a id="map-orig-p0a-l1098"></a>P0A:56 | 26.1 接口 | L1098–L1115 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-p0a-l1098) |
| <a id="map-orig-p0a-l1116"></a>P0A:57 | 26.2 ResolverResult | L1116–L1138 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-p0a-l1116) |
| <a id="map-orig-p0a-l1139"></a>P0A:58 | 27. AuthorityRelationshipEdge | L1139–L1161 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1139) |
| <a id="map-orig-p0a-l1162"></a>P0A:59 | 28. Relationship Validation State | L1162–L1184 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1162) |
| <a id="map-orig-p0a-l1185"></a>P0A:60 | 29. Graph Persistence Policy | L1185–L1208 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1185) |
| <a id="map-orig-p0a-l1209"></a>P0A:61 | 30. Policy Model | L1209–L1210 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-p0a-l1209) |
| <a id="map-orig-p0a-l1211"></a>P0A:62 | 30.1 AuthorityMode | L1211–L1220 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-p0a-l1211) |
| <a id="map-orig-p0a-l1221"></a>P0A:63 | 30.2 ModeSelector | L1221–L1230 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-p0a-l1221) |
| <a id="map-orig-p0a-l1231"></a>P0A:64 | 30.3 PolicyOverlay | L1231–L1243 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-p0a-l1231) |
| <a id="map-orig-p0a-l1244"></a>P0A:65 | 31. Policy Resolution | L1244–L1260 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-p0a-l1244) |
| <a id="map-orig-p0a-l1261"></a>P0A:66 | 32. Constitutional Invariants | L1261–L1285 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1261) |
| <a id="map-orig-p0a-l1286"></a>P0A:67 | 33. Mode 切换 | L1286–L1312 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-p0a-l1286) |
| <a id="map-orig-p0a-l1313"></a>P0A:68 | 34. P0-A 与 Non-Blocking Dominion | L1313–L1336 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1313) |
| <a id="map-orig-p0a-l1337"></a>P0A:69 | 35. P0-A 与 P0-B 的边界 | L1337–L1361 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1337) |
| <a id="map-orig-p0a-l1362"></a>P0A:70 | 36. P0-A 与 P0-C 的边界 | L1362–L1384 | entitydefence / 构建幂等修复过程 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1362) |
| <a id="map-orig-p0a-l1385"></a>P0A:71 | 37. P0-A 与 P0-D 的边界 | L1385–L1418 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1385) |
| <a id="map-orig-p0a-l1419"></a>P0A:72 | 38. Failure Model | L1419–L1448 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1419) |
| <a id="map-orig-p0a-l1449"></a>P0A:73 | 39. Fail-Closed / Fail-Open 策略 | L1449–L1450 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1449) |
| <a id="map-orig-p0a-l1451"></a>P0A:74 | Root unavailable / fenced | L1451–L1466 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1451) |
| <a id="map-orig-p0a-l1467"></a>P0A:75 | Unknown schema | L1467–L1482 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1467) |
| <a id="map-orig-p0a-l1483"></a>P0A:76 | 40. Cache 设计 | L1483–L1499 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1483) |
| <a id="map-orig-p0a-l1500"></a>P0A:77 | 41. 并发模型 | L1500–L1529 | skills / 逻辑服务器 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1500) |
| <a id="map-orig-p0a-l1530"></a>P0A:78 | 42. Java 内存模型要求 | L1530–L1548 | skills / 逻辑服务器 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1530) |
| <a id="map-orig-p0a-l1549"></a>P0A:79 | 43. Native Shared Projection 预留 | L1549–L1568 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1549) |
| <a id="map-orig-p0a-l1569"></a>P0A:80 | 44. Migration from FULL L6 Internal | L1569–L1586 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1569) |
| <a id="map-orig-p0a-l1587"></a>P0A:81 | 45. Migration from 7.14A Frozen Baseline / r50 Current Line | L1587–L1607 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1587) |
| <a id="map-orig-p0a-l1608"></a>P0A:82 | 46. Telemetry | L1608–L1632 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1608) |
| <a id="map-orig-p0a-l1633"></a>P0A:83 | 47. Audit | L1633–L1656 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1633) |
| <a id="map-orig-p0a-l1657"></a>P0A:84 | 48. Security / Integrity Notes | L1657–L1672 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1657) |
| <a id="map-orig-p0a-l1673"></a>P0A:85 | 49. Unit Tests | L1673–L1688 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1673) |
| <a id="map-orig-p0a-l1689"></a>P0A:86 | U-01 SubjectKey Stability | L1689–L1697 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1689) |
| <a id="map-orig-p0a-l1698"></a>P0A:87 | U-02 Incarnation Monotonicity | L1698–L1701 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1698) |
| <a id="map-orig-p0a-l1702"></a>P0A:88 | U-03 Stale Rejection | L1702–L1709 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1702) |
| <a id="map-orig-p0a-l1710"></a>P0A:89 | U-04 Tombstone Dominance | L1710–L1717 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1710) |
| <a id="map-orig-p0a-l1718"></a>P0A:90 | U-05 Binding Revision | L1718–L1725 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1718) |
| <a id="map-orig-p0a-l1726"></a>P0A:91 | U-06 Root Command Idempotency | L1726–L1733 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1726) |
| <a id="map-orig-p0a-l1734"></a>P0A:92 | U-07 Command Conflict | L1734–L1741 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1734) |
| <a id="map-orig-p0a-l1742"></a>P0A:93 | U-08 Projection Atomicity | L1742–L1745 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1742) |
| <a id="map-orig-p0a-l1746"></a>P0A:94 | U-09 Unknown Optional Extension | L1746–L1749 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1746) |
| <a id="map-orig-p0a-l1750"></a>P0A:95 | U-10 Unknown Mandatory Extension | L1750–L1753 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1750) |
| <a id="map-orig-p0a-l1754"></a>P0A:96 | U-11 Lifecycle / Resolution Orthogonality | L1754–L1757 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1754) |
| <a id="map-orig-p0a-l1758"></a>P0A:97 | U-12 Revision Scope | L1758–L1761 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1758) |
| <a id="map-orig-p0a-l1762"></a>P0A:98 | U-13 Canonical Wire / Defensive Copy | L1762–L1765 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1762) |
| <a id="map-orig-p0a-l1766"></a>P0A:99 | U-14 Immutable ProjectionView | L1766–L1769 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1766) |
| <a id="map-orig-p0a-l1770"></a>P0A:100 | U-15 Provider Lifecycle | L1770–L1773 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1770) |
| <a id="map-orig-p0a-l1774"></a>P0A:101 | U-16 Pre-Admission State Machine | L1774–L1777 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1774) |
| <a id="map-orig-p0a-l1778"></a>P0A:102 | U-17 Production Process Boundary | L1778–L1793 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1778) |
| <a id="map-orig-p0a-l1794"></a>P0A:103 | 50. Integration Tests | L1794–L1795 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1794) |
| <a id="map-orig-p0a-l1796"></a>P0A:104 | I-01 Entity Rebuild | L1796–L1811 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1796) |
| <a id="map-orig-p0a-l1812"></a>P0A:105 | I-02 Terminal Rebuild Attempt | L1812–L1826 | msitem / 验证矩阵 | [原文](MS-MSITEM-PLAN.md#orig-p0a-l1812) |
| <a id="map-orig-p0a-l1827"></a>P0A:106 | I-03 Projection Lag | L1827–L1838 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1827) |
| <a id="map-orig-p0a-l1839"></a>P0A:107 | I-04 Root Receipt Ambiguity | L1839–L1850 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1839) |
| <a id="map-orig-p0a-l1851"></a>P0A:108 | I-05 Mode Change | L1851–L1864 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1851) |
| <a id="map-orig-p0a-l1865"></a>P0A:109 | I-06 Relationship Opaque | L1865–L1878 | entitydefence / 验证矩阵 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1865) |
| <a id="map-orig-p0a-l1879"></a>P0A:110 | I-07 Root Schema / Recovery | L1879–L1882 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1879) |
| <a id="map-orig-p0a-l1883"></a>P0A:111 | I-08 P0-D Minimum Crosscut | L1883–L1888 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1883) |
| <a id="map-orig-p0a-l1889"></a>P0A:112 | 51. Chaos Tests | L1889–L1890 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1889) |
| <a id="map-orig-p0a-l1891"></a>P0A:113 | C-01 Reordered Projection Arrival | L1891–L1907 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1891) |
| <a id="map-orig-p0a-l1908"></a>P0A:114 | C-02 Duplicate Root Response | L1908–L1917 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1908) |
| <a id="map-orig-p0a-l1918"></a>P0A:115 | C-03 JVM Restart | L1918–L1928 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1918) |
| <a id="map-orig-p0a-l1929"></a>P0A:116 | C-04 Partial Relationship Cache Loss | L1929–L1940 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1929) |
| <a id="map-orig-p0a-l1941"></a>P0A:117 | C-05 Internal Root / Store Temporary Unavailable | L1941–L1950 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1941) |
| <a id="map-orig-p0a-l1951"></a>P0A:118 | 52. Performance Gates | L1951–L1987 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1951) |
| <a id="map-orig-p0a-l1988"></a>P0A:119 | 53. Persistence Rules | L1988–L2017 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l1988) |
| <a id="map-orig-p0a-l2018"></a>P0A:120 | 54. Data Retention | L2018–L2037 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l2018) |
| <a id="map-orig-p0a-l2038"></a>P0A:121 | 55. Logging Rules | L2038–L2065 | skills / 故障定位 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l2038) |
| <a id="map-orig-p0a-l2066"></a>P0A:122 | 56. Feature Flags | L2066–L2084 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-p0a-l2066) |
| <a id="map-orig-p0a-l2085"></a>P0A:123 | 57. Backward Compatibility | L2085–L2111 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l2085) |
| <a id="map-orig-p0a-l2112"></a>P0A:124 | 58. P0-A V1.3 Deliverables | L2112–L2138 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l2112) |
| <a id="map-orig-p0a-l2139"></a>P0A:125 | 59. P0-A V1.3 明确不交付 | L2139–L2157 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l2139) |
| <a id="map-orig-p0a-l2158"></a>P0A:126 | 60. P0-A Exit Criteria | L2158–L2161 | entitydefence / 完成标准 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2158) |
| <a id="map-orig-p0a-l2162"></a>P0A:127 | Identity | L2162–L2169 | entitydefence / 完成标准 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2162) |
| <a id="map-orig-p0a-l2170"></a>P0A:128 | Binding | L2170–L2176 | entitydefence / 完成标准 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2170) |
| <a id="map-orig-p0a-l2177"></a>P0A:129 | Lifecycle | L2177–L2182 | entitydefence / 完成标准 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2177) |
| <a id="map-orig-p0a-l2183"></a>P0A:130 | Root | L2183–L2191 | entitydefence / 完成标准 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2183) |
| <a id="map-orig-p0a-l2192"></a>P0A:131 | Projection | L2192–L2198 | entitydefence / 完成标准 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2192) |
| <a id="map-orig-p0a-l2199"></a>P0A:132 | Policy | L2199–L2204 | entitydefence / 完成标准 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2199) |
| <a id="map-orig-p0a-l2205"></a>P0A:133 | Relationship | L2205–L2210 | entitydefence / 完成标准 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2205) |
| <a id="map-orig-p0a-l2211"></a>P0A:134 | Admission / Result Semantics | L2211–L2217 | entitydefence / 完成标准 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2211) |
| <a id="map-orig-p0a-l2218"></a>P0A:135 | P0-D Minimum Crosscut | L2218–L2223 | entitydefence / 完成标准 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2218) |
| <a id="map-orig-p0a-l2224"></a>P0A:136 | Performance | L2224–L2229 | entitydefence / 完成标准 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2224) |
| <a id="map-orig-p0a-l2230"></a>P0A:137 | Process Boundary | L2230–L2239 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l2230) |
| <a id="map-orig-p0a-l2240"></a>P0A:138 | 61. 下一阶段接口交接 | L2240–L2271 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l2240) |
| <a id="map-orig-p0a-l2272"></a>P0A:139 | 62. Final Implementation Rule | L2272–L2298 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-p0a-l2272) |

<a id="source-l6"></a>

### L6 · Ronova_L6_Architecture_Master_Plan_V2.4_Internal_Root.md

原路径：`docs/plans/Ronova_L6_Architecture_Master_Plan_V2.4_Internal_Root.md`。 [原始快照](../archive/plans-before-ms-restructure-r66/Ronova_L6_Architecture_Master_Plan_V2.4_Internal_Root.md)。

| 映射锚点 | 旧标题（原编号不变） | 旧行范围 | 新 MS 章节 | 新正文落点 |
|---|---|---|---|---|
| <a id="map-orig-l6-l0001"></a>L6:1 | Ronova L6 Architecture Master Plan V2.4 — Internal Root | L1–L29 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0001) |
| <a id="map-orig-l6-l0030"></a>L6:2 | 0. 阅读约定 | L30–L44 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0030) |
| <a id="map-orig-l6-l0045"></a>L6:3 | 0.1 V2 冻结决策（本版新增，优先级高于正文其余描述） | L45–L59 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0045) |
| <a id="map-orig-l6-l0060"></a>L6:4 | 0.2 V2.4 跨规格闭合决策（优先级与 D1–D8 相同） | L60–L88 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0060) |
| <a id="map-orig-l6-l0089"></a>L6:5 | 1. 7.13 架构审计（V2 修订：C1 误判更正、H7 表述更正，其余保留） | L89–L90 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0089) |
| <a id="map-orig-l6-l0091"></a>L6:6 | 1.1 运行环境（源码 CONFIRMED；实机数据为迁移前继承证据） | L91–L101 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0091) |
| <a id="map-orig-l6-l0102"></a>L6:7 | 1.2 包结构与分层现状（CONFIRMED，保留 V1） | L102–L132 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0102) |
| <a id="map-orig-l6-l0133"></a>L6:8 | 1.3 六大架构轴的现状评估（V2 修订处以【V2】标注） | L133–L134 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0133) |
| <a id="map-orig-l6-l0135"></a>L6:9 | 1.3.1 Authority（授权） | L135–L141 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0135) |
| <a id="map-orig-l6-l0142"></a>L6:10 | 1.3.2 Observation（观测） | L142–L147 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0142) |
| <a id="map-orig-l6-l0148"></a>L6:11 | 1.3.3 Enforcement（执行） | L148–L154 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0148) |
| <a id="map-orig-l6-l0155"></a>L6:12 | 1.3.4 Persistence（持久化） | L155–L161 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0155) |
| <a id="map-orig-l6-l0162"></a>L6:13 | 1.3.5 Integrity（完整性） | L162–L169 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0162) |
| <a id="map-orig-l6-l0170"></a>L6:14 | 1.3.6 Root（根权威） | L170–L177 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0170) |
| <a id="map-orig-l6-l0178"></a>L6:15 | 1.4 三条"权力倒流"（CONFIRMED，保留 V1） | L178–L183 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0178) |
| <a id="map-orig-l6-l0184"></a>L6:16 | 1.5 需求逐条对照现状（V2 修订 H7） | L184–L195 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0184) |
| <a id="map-orig-l6-l0196"></a>L6:17 | 2. 新模块树（V2 修订：C1/H2/H5/H7/M1 相关条目） | L196–L197 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0196) |
| <a id="map-orig-l6-l0198"></a>L6:18 | 2.1 设计原则 | L198–L205 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0198) |
| <a id="map-orig-l6-l0206"></a>L6:19 | 2.2 目标包树（FULL L6 Internal 收敛形态） | L206–L334 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0206) |
| <a id="map-orig-l6-l0335"></a>L6:20 | 2.3 关键模块职责再定义 | L335–L350 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0335) |
| <a id="map-orig-l6-l0351"></a>L6:21 | 2.3.1 Presence 保证范围（V2 新增，关闭 H6） | L351–L358 | entitydefence / 防御读取路径 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-l6-l0351) |
| <a id="map-orig-l6-l0359"></a>L6:22 | 2.3.2 "Dominion 自身实体"的身份来源（V2 新增，关闭 M1） | L359–L362 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-l6-l0359) |
| <a id="map-orig-l6-l0363"></a>L6:23 | 2.4 QuarantineView 拆分（保留 V1，依据 CONFIRMED `QuarantineView.java:76-114`） | L363–L368 | entitydefence / 防御读取路径 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-l6-l0363) |
| <a id="map-orig-l6-l0369"></a>L6:24 | 3. State Ownership（状态所有权） | L369–L372 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0369) |
| <a id="map-orig-l6-l0373"></a>L6:25 | 3.1 7.13 现状（CONFIRMED，保留 V1） | L373–L390 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0373) |
| <a id="map-orig-l6-l0391"></a>L6:26 | 3.2 目标状态所有权（FULL L5 / L6，V2 修订 D3/D4/M1） | L391–L414 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0391) |
| <a id="map-orig-l6-l0415"></a>L6:27 | 3.3 Single-Writer 不变量（贯穿 L5→L6，V2.4 Internal Root） | L415–L437 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0415) |
| <a id="map-orig-l6-l0438"></a>L6:28 | 4. Dependency DAG（V2 修订：H5 分层修正、C1 方向修正） | L438–L439 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0438) |
| <a id="map-orig-l6-l0440"></a>L6:29 | 4.1 分层规则（V2 修订） | L440–L456 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0440) |
| <a id="map-orig-l6-l0457"></a>L6:30 | 4.2 运行时数据流 DAG（FULL L6 Internal Root 目标） | L457–L486 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0457) |
| <a id="map-orig-l6-l0487"></a>L6:31 | 4.3 编译期依赖（关键约束，V2 增补） | L487–L497 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0487) |
| <a id="map-orig-l6-l0498"></a>L6:32 | 5. FULL L5 架构与不变量（V2 修订：D3 writer 固定、M2 gate 消歧、H7 判据修正、D6 性能不变量） | L498–L499 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0498) |
| <a id="map-orig-l6-l0500"></a>L6:33 | 5.1 FULL L5 的完成条件（严格定义） | L500–L546 | skills / 完成标准(以一个标准的，带Native的秒杀Mod举例) | [原文](MS-SKILLS-PLAN.md#orig-l6-l0500) |
| <a id="map-orig-l6-l0547"></a>L6:34 | 5.2 FULL L5 数据流（V2 修订） | L547–L603 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0547) |
| <a id="map-orig-l6-l0604"></a>L6:35 | 5.3 FULL L5 不变量总表 | L604–L618 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0604) |
| <a id="map-orig-l6-l0619"></a>L6:36 | 5.4 RealmId 与 store 绑定（V2 新增） | L619–L626 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-l6-l0619) |
| <a id="map-orig-l6-l0627"></a>L6:37 | 6. Tombstone / Terminal Enforcement 重设计（V2 修订：C2 语义/存储分离、D5 终结拆分） | L627–L628 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-l6-l0627) |
| <a id="map-orig-l6-l0629"></a>L6:38 | 6.1 问题回顾（CONFIRMED） | L629–L636 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-l6-l0629) |
| <a id="map-orig-l6-l0637"></a>L6:39 | 6.2 新模型：无限逻辑寿命 + 有界物理工作（V2 按 D5 术语拆分） | L637–L662 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-l6-l0637) |
| <a id="map-orig-l6-l0663"></a>L6:40 | 6.3 调度状态机与预算（保留 V1） | L663–L684 | msitem / 服务端队列与线程边界 | [原文](MS-MSITEM-PLAN.md#orig-l6-l0663) |
| <a id="map-orig-l6-l0685"></a>L6:41 | 6.4 对象重建 / reload 的权威性 + 容量策略（V2 重写，关闭 C2） | L685–L706 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-l6-l0685) |
| <a id="map-orig-l6-l0707"></a>L6:42 | 7. Bounded Budget / Backoff / Storm Control（V2 增补 §7.5） | L707–L708 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0707) |
| <a id="map-orig-l6-l0709"></a>L6:43 | 7.1 7.13 现状（CONFIRMED，保留 V1） | L709–L719 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0709) |
| <a id="map-orig-l6-l0720"></a>L6:44 | 7.2 统一预算框架（V2 增 presence/commit 行） | L720–L736 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0720) |
| <a id="map-orig-l6-l0737"></a>L6:45 | 7.3 各工作的具体策略（保留 V1） | L737–L743 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0737) |
| <a id="map-orig-l6-l0744"></a>L6:46 | 7.4 Storm Control（保留 V1） | L744–L749 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0744) |
| <a id="map-orig-l6-l0750"></a>L6:47 | 7.5 Judgment 提交扩展性（V2 新增，关闭 C3/D6） | L750–L764 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0750) |
| <a id="map-orig-l6-l0765"></a>L6:48 | 8. L6 架构（FULL L5 完成后；V2.4 Internal Root） | L765–L766 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0765) |
| <a id="map-orig-l6-l0767"></a>L6:49 | 8.1 L6 目标：把 Root 收口为 JVM 内唯一逻辑 Authority writer | L767–L790 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0767) |
| <a id="map-orig-l6-l0791"></a>L6:50 | 8.2 Internal Root 架构 | L791–L819 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0791) |
| <a id="map-orig-l6-l0820"></a>L6:51 | 8.3 Epoch / Fence / Root Instance / Command 语义 | L820–L833 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0820) |
| <a id="map-orig-l6-l0834"></a>L6:52 | 8.4 In-Process Root Command Boundary | L834–L846 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0834) |
| <a id="map-orig-l6-l0847"></a>L6:53 | 8.5 Recovery / Stale Writer 防护 | L847–L858 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0847) |
| <a id="map-orig-l6-l0859"></a>L6:54 | 8.6 FULL L6 Internal 不变量 | L859–L875 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0859) |
| <a id="map-orig-l6-l0876"></a>L6:55 | 8.7 Judgment Commit Ordering | L876–L905 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0876) |
| <a id="map-orig-l6-l0906"></a>L6:56 | 9. Java / Native / Internal Root 职责划分（V2.4） | L906–L907 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0906) |
| <a id="map-orig-l6-l0908"></a>L6:57 | 9.1 三层职责边界 | L908–L916 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0908) |
| <a id="map-orig-l6-l0917"></a>L6:58 | 9.2 能力层归属 | L917–L927 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0917) |
| <a id="map-orig-l6-l0928"></a>L6:59 | 9.3 边界契约 | L928–L940 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0928) |
| <a id="map-orig-l6-l0941"></a>L6:60 | 10. 现有代码迁移方案（KEEP / REFACTOR / SPLIT / MERGE / DELETE） | L941–L942 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0941) |
| <a id="map-orig-l6-l0943"></a>L6:61 | 10.0 默认规则（V2 新增，关闭 M5） | L943–L946 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0943) |
| <a id="map-orig-l6-l0947"></a>L6:62 | 10.1 contract / core | L947–L959 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0947) |
| <a id="map-orig-l6-l0960"></a>L6:63 | 10.2 engine（判定/执行/观测，V2 修订 C1/D5 相关行） | L960–L981 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0960) |
| <a id="map-orig-l6-l0982"></a>L6:64 | 10.3 capability（保留 V1） | L982–L991 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0982) |
| <a id="map-orig-l6-l0992"></a>L6:65 | 10.4 persistence / projection（V2 修订） | L992–L1006 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-l6-l0992) |
| <a id="map-orig-l6-l1007"></a>L6:66 | 10.5 root / nativebridge / wire（V2 增 JudgmentBatch） | L1007–L1019 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1007) |
| <a id="map-orig-l6-l1020"></a>L6:67 | 10.6 agent / mixin / event（保留 V1 裁决，范围声明见 §2.3.1） | L1020–L1041 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1020) |
| <a id="map-orig-l6-l1042"></a>L6:68 | 10.7 迁移动作汇总（V2 更新） | L1042–L1054 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1042) |
| <a id="map-orig-l6-l1055"></a>L6:69 | 11. 分阶段版本路线（V2 重排，关闭 H4/D8/M3） | L1055–L1058 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1055) |
| <a id="map-orig-l6-l1059"></a>L6:70 | 11.0 7.13 Baseline Freeze（V2 新增，7.14 前置，关闭 M3） | L1059–L1073 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1059) |
| <a id="map-orig-l6-l1074"></a>L6:71 | 11.1 阶段详表（V2 重排） | L1074–L1110 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1074) |
| <a id="map-orig-l6-l1111"></a>L6:72 | 11.2 可逆窗口与回滚 | L1111–L1118 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1111) |
| <a id="map-orig-l6-l1119"></a>L6:73 | 11.3 阶段↔Review 对账 | L1119–L1132 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1119) |
| <a id="map-orig-l6-l1133"></a>L6:74 | 11.4 与 deep-research-report.md 的差异（保留 V1 结论） | L1133–L1145 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1133) |
| <a id="map-orig-l6-l1146"></a>L6:75 | 12. 测试矩阵与性能标准（V2 修订） | L1146–L1147 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1146) |
| <a id="map-orig-l6-l1148"></a>L6:76 | 12.1 测试矩阵（release gate） | L1148–L1185 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1148) |
| <a id="map-orig-l6-l1186"></a>L6:77 | 12.2 性能预算（V2 修订） | L1186–L1203 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1186) |
| <a id="map-orig-l6-l1204"></a>L6:78 | 12.3 PASS / FAIL Gate 判定（V2 消歧，M2） | L1204–L1212 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1204) |
| <a id="map-orig-l6-l1213"></a>L6:79 | 13. 风险与明确拒绝的设计（V2 修订） | L1213–L1214 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1213) |
| <a id="map-orig-l6-l1215"></a>L6:80 | 13.1 风险与缓解（V2 增补三行，余保留 V1） | L1215–L1237 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1215) |
| <a id="map-orig-l6-l1238"></a>L6:81 | 13.2 明确拒绝的设计（Anti-goals，V2 修订） | L1238–L1258 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1238) |
| <a id="map-orig-l6-l1259"></a>L6:82 | 13.3 待确认输入（影响 L6 规格冻结，保留 V1） | L1259–L1276 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1259) |
| <a id="map-orig-l6-l1277"></a>L6:83 | 14. 结论 | L1277–L1278 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1277) |
| <a id="map-orig-l6-l1279"></a>L6:84 | 14.1 核心判断（CONFIRMED，保留 V1） | L1279–L1282 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1279) |
| <a id="map-orig-l6-l1283"></a>L6:85 | 14.2 V2 相对 V1 的修订原则 | L1283–L1286 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1283) |
| <a id="map-orig-l6-l1287"></a>L6:86 | 14.3 本轮交付范围 | L1287–L1292 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1287) |
| <a id="map-orig-l6-l1293"></a>L6:87 | Architecture Review Resolution Table | L1293–L1323 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1293) |
| <a id="map-orig-l6-l1324"></a>L6:88 | 最终清单 | L1324–L1325 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1324) |
| <a id="map-orig-l6-l1326"></a>L6:89 | 1. FULL L5 不变量清单 | L1326–L1340 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1326) |
| <a id="map-orig-l6-l1341"></a>L6:90 | 2. FULL L6 Internal 不变量清单 | L1341–L1357 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1341) |
| <a id="map-orig-l6-l1358"></a>L6:91 | 3. 7.14 → 7.17 路线 | L1358–L1367 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1358) |
| <a id="map-orig-l6-l1368"></a>L6:92 | 4. 8.0 → 8.4 路线 | L1368–L1379 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1368) |
| <a id="map-orig-l6-l1380"></a>L6:93 | 5. Single Writer ownership matrix | L1380–L1388 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1380) |
| <a id="map-orig-l6-l1389"></a>L6:94 | 6. Raw Observation / Presence dependency DAG（摘要） | L1389–L1397 | entitydefence / 防御读取路径 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-l6-l1389) |
| <a id="map-orig-l6-l1398"></a>L6:95 | 7. Epoch / Fence / RootInstance / Command semantic table | L1398–L1409 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1398) |
| <a id="map-orig-l6-l1410"></a>L6:96 | 8. Tombstone lifetime / clear semantics | L1410–L1416 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1410) |
| <a id="map-orig-l6-l1417"></a>L6:97 | 9. Judgment commit ordering | L1417–L1429 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1417) |
| <a id="map-orig-l6-l1430"></a>L6:98 | 10. Performance gates | L1430–L1442 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1430) |
| <a id="map-orig-l6-l1443"></a>L6:99 | 11. Freeze readiness verdict | L1443–L1454 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1443) |
| <a id="map-orig-l6-l1455"></a>L6:100 | Closure Fix Resolution | L1455–L1468 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-l6-l1455) |

<a id="source-road"></a>

### ROAD · Ronova_L6_L6E_Full_Enhancement_Roadmap_V1.0_Internal_Root.md

原路径：`docs/plans/Ronova_L6_L6E_Full_Enhancement_Roadmap_V1.0_Internal_Root.md`。 [原始快照](../archive/plans-before-ms-restructure-r66/Ronova_L6_L6E_Full_Enhancement_Roadmap_V1.0_Internal_Root.md)。

| 映射锚点 | 旧标题（原编号不变） | 旧行范围 | 新 MS 章节 | 新正文落点 |
|---|---|---|---|---|
| <a id="map-orig-road-l0001"></a>ROAD:1 | Ronova L6／L6E 全量加强路线 V1.0 — Internal Root | L1–L8 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-road-l0001) |
| <a id="map-orig-road-l0009"></a>ROAD:2 | 1. 范围、规范与阅读方式 | L9–L10 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-road-l0009) |
| <a id="map-orig-road-l0011"></a>ROAD:3 | 1.1 极限档的连续加强目标 | L11–L25 | msitem / 先定义击杀契约 | [原文](MS-MSITEM-PLAN.md#orig-road-l0011) |
| <a id="map-orig-road-l0026"></a>ROAD:4 | 1.2 规范来源 | L26–L42 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-road-l0026) |
| <a id="map-orig-road-l0043"></a>ROAD:5 | 1.3 阶段和状态约定 | L43–L60 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-road-l0043) |
| <a id="map-orig-road-l0061"></a>ROAD:6 | 2. L6 Internal：8.0–8.4 的完整加强 | L61–L70 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-road-l0061) |
| <a id="map-orig-road-l0071"></a>ROAD:7 | 2.1 Root 统一负责的内容 | L71–L79 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-road-l0071) |
| <a id="map-orig-road-l0080"></a>ROAD:8 | 2.2 身份、恢复和进程边界 | L80–L90 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-road-l0080) |
| <a id="map-orig-road-l0091"></a>ROAD:9 | 3. L6E 四条加强链 | L91–L92 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-road-l0091) |
| <a id="map-orig-road-l0093"></a>ROAD:10 | 3.1 P0-A：身份、状态归属与权威 | L93–L101 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-road-l0093) |
| <a id="map-orig-road-l0102"></a>ROAD:11 | 3.2 P0-B：独立执行、任务、调用与返回 | L102–L110 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-road-l0102) |
| <a id="map-orig-road-l0111"></a>ROAD:12 | 3.3 P0-C：多来源状态、恢复链与持续结果 | L111–L120 | entitydefence / 构建幂等修复过程 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-road-l0111) |
| <a id="map-orig-road-l0121"></a>ROAD:13 | 3.4 P0-D：完整性、代码对抗与恢复 | L121–L130 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-road-l0121) |
| <a id="map-orig-road-l0131"></a>ROAD:14 | 4. 全部 54 项高级能力的阶段对照 | L131–L136 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-road-l0131) |
| <a id="map-orig-road-l0137"></a>ROAD:15 | 4.1 攻击：15 项 | L137–L156 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-road-l0137) |
| <a id="map-orig-road-l0157"></a>ROAD:16 | 4.2 拦截：10 项 | L157–L171 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-road-l0157) |
| <a id="map-orig-road-l0172"></a>ROAD:17 | 4.3 防护：17 项 | L172–L193 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-road-l0172) |
| <a id="map-orig-road-l0194"></a>ROAD:18 | 4.4 运行时：12 项 | L194–L210 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-road-l0194) |
| <a id="map-orig-road-l0211"></a>ROAD:19 | 5. 全部 14 个普通动作 | L211–L239 | msitem / 列出全部击杀入口 | [原文](MS-MSITEM-PLAN.md#orig-road-l0211) |
| <a id="map-orig-road-l0240"></a>ROAD:20 | 6. 全部 8 类运行表面 | L240–L266 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-road-l0240) |
| <a id="map-orig-road-l0267"></a>ROAD:21 | 7. 候选、持续待办与跨表面结果 | L267–L283 | msitem / 目标选择与快照 | [原文](MS-MSITEM-PLAN.md#orig-road-l0267) |
| <a id="map-orig-road-l0284"></a>ROAD:22 | 8. 客户端、全资源域与运行连续性 | L284–L285 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-road-l0284) |
| <a id="map-orig-road-l0286"></a>ROAD:23 | 8.1 客户端参与和显示 | L286–L295 | skills / 客户端 | [原文](MS-SKILLS-PLAN.md#orig-road-l0286) |
| <a id="map-orig-road-l0296"></a>ROAD:24 | 8.2 资源范围 | L296–L317 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-road-l0296) |
| <a id="map-orig-road-l0318"></a>ROAD:25 | 8.3 持续执行与自恢复 | L318–L324 | skills / 5. 周期修复层 | [原文](MS-SKILLS-PLAN.md#orig-road-l0318) |
| <a id="map-orig-road-l0325"></a>ROAD:26 | 9. 接口衔接、效果与迁移 | L325–L326 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-road-l0325) |
| <a id="map-orig-road-l0327"></a>ROAD:27 | 9.1 唯一接口关系 | L327–L340 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-road-l0327) |
| <a id="map-orig-road-l0341"></a>ROAD:28 | 9.2 写入与完成的当前性 | L341–L349 | msitem / 服务端队列与线程边界 | [原文](MS-MSITEM-PLAN.md#orig-road-l0341) |
| <a id="map-orig-road-l0350"></a>ROAD:29 | 9.3 效果与未知结果 | L350–L356 | msitem / 去重与幂等 | [原文](MS-MSITEM-PLAN.md#orig-road-l0350) |
| <a id="map-orig-road-l0357"></a>ROAD:30 | 9.4 迁移与兼容要求 | L357–L365 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-road-l0357) |
| <a id="map-orig-road-l0366"></a>ROAD:31 | 10. 后续 P1 扩展 | L366–L383 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-road-l0366) |
| <a id="map-orig-road-l0384"></a>ROAD:32 | 11. 完成判断与场景 | L384–L385 | skills / 完成标准(以一个标准的，带Native的秒杀Mod举例) | [原文](MS-SKILLS-PLAN.md#orig-road-l0384) |
| <a id="map-orig-road-l0386"></a>ROAD:33 | 11.1 各阶段当前记录 | L386–L400 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-road-l0386) |
| <a id="map-orig-road-l0401"></a>ROAD:34 | 11.2 对应场景与预期结果 | L401–L417 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-road-l0401) |
| <a id="map-orig-road-l0418"></a>ROAD:35 | 11.3 完成规则与验证节奏 | L418–L429 | skills / 完成标准(以一个标准的，带Native的秒杀Mod举例) | [原文](MS-SKILLS-PLAN.md#orig-road-l0418) |

<a id="source-msc"></a>

### MSC · Ronova_MS_Compliance_and_Enhancement_Plan_V1.0.md

原路径：`docs/plans/Ronova_MS_Compliance_and_Enhancement_Plan_V1.0.md`。 [原始快照](../archive/plans-before-ms-restructure-r66/Ronova_MS_Compliance_and_Enhancement_Plan_V1.0.md)。

| 映射锚点 | 旧标题（原编号不变） | 旧行范围 | 新 MS 章节 | 新正文落点 |
|---|---|---|---|---|
| <a id="map-orig-msc-l0001"></a>MSC:1 | Ronova × MS 要求 合规与加强方案 V1.0 | L1–L21 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0001) |
| <a id="map-orig-msc-l0022"></a>MSC:2 | 0. 本方案的定位 | L22–L34 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0022) |
| <a id="map-orig-msc-l0035"></a>MSC:3 | 1. 要求清单与现状判定 | L35–L38 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0035) |
| <a id="map-orig-msc-l0039"></a>MSC:4 | 1.1 纪律与边界 | L39–L49 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0039) |
| <a id="map-orig-msc-l0050"></a>MSC:5 | 1.2 状态模型 | L50–L58 | msitem / 总体架构 | [原文](MS-MSITEM-PLAN.md#orig-msc-l0050) |
| <a id="map-orig-msc-l0059"></a>MSC:6 | 1.3 统一动作调度 | L59–L68 | msitem / 用统一动作上下文消除重复 | [原文](MS-MSITEM-PLAN.md#orig-msc-l0059) |
| <a id="map-orig-msc-l0069"></a>MSC:7 | 1.4 目标与结果策略 | L69–L79 | msitem / 先定义击杀契约 | [原文](MS-MSITEM-PLAN.md#orig-msc-l0069) |
| <a id="map-orig-msc-l0080"></a>MSC:8 | 1.5 线程与生命周期 | L80–L88 | skills / 线程、逻辑边与生命周期 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0080) |
| <a id="map-orig-msc-l0089"></a>MSC:9 | 1.6 追踪回调双向规则 | L89–L96 | skills / 追踪和重载 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0089) |
| <a id="map-orig-msc-l0097"></a>MSC:10 | 1.7 防御链五层 | L97–L107 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0097) |
| <a id="map-orig-msc-l0108"></a>MSC:11 | 1.8 规范实例与状态不变量 | L108–L118 | entitydefence / 保留一个规范实例 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0108) |
| <a id="map-orig-msc-l0119"></a>MSC:12 | 1.9 服务端与客户端注册表 | L119–L131 | entitydefence / 修复服务端注册表 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119) |
| <a id="map-orig-msc-l0132"></a>MSC:13 | 1.10 读取路径兜底与修复时机 | L132–L144 | entitydefence / 防御读取路径 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0132) |
| <a id="map-orig-msc-l0145"></a>MSC:14 | 1.11 幂等与内部集合 | L145–L152 | entitydefence / 内部集合规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0145) |
| <a id="map-orig-msc-l0153"></a>MSC:15 | 1.12 独占列表防御 | L153–L160 | entitydefence / 可选：实体列表独占防御 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0153) |
| <a id="map-orig-msc-l0161"></a>MSC:16 | 1.13 转换器与 Native | L161–L176 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0161) |
| <a id="map-orig-msc-l0177"></a>MSC:17 | 1.14 构建与验收 | L177–L192 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0177) |
| <a id="map-orig-msc-l0193"></a>MSC:18 | 2. 合规缺口（C1–C8，必须先处理） | L193–L209 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0193) |
| <a id="map-orig-msc-l0210"></a>MSC:19 | 3. 阶段设计 | L210–L211 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0210) |
| <a id="map-orig-msc-l0212"></a>MSC:20 | M0 —— 合规整改（C1、C3、C4） | L212–L242 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0212) |
| <a id="map-orig-msc-l0243"></a>MSC:21 | M1 —— 可见验收闭环（C8、R-S2/S3/S4） | L243–L257 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0243) |
| <a id="map-orig-msc-l0258"></a>MSC:22 | M2 —— 显式状态模型（R-B1、R-B2） | L258–L268 | msitem / 总体架构 | [原文](MS-MSITEM-PLAN.md#orig-msc-l0258) |
| <a id="map-orig-msc-l0269"></a>MSC:23 | M3 —— 规范实例与状态不变量（C6、R-H1~H4、R-I1） | L269–L279 | entitydefence / 保留一个规范实例 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0269) |
| <a id="map-orig-msc-l0280"></a>MSC:24 | M4 —— 注册表独立修复与读写窗口（C7、R-J1~J6、R-L1/L2、R-M4/M5/M6） | L280–L293 | entitydefence / 修复服务端注册表 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0280) |
| <a id="map-orig-msc-l0294"></a>MSC:25 | M5 —— 防御链补齐（C5、R-G1、R-G3、R-G6） | L294–L305 | skills / 防御链 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0294) |
| <a id="map-orig-msc-l0306"></a>MSC:26 | M6 —— 转换器、Native 与构建收口（C2、R-P5、R-Q、R-R3/R4） | L306–L318 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0306) |
| <a id="map-orig-msc-l0319"></a>MSC:27 | 4. 阶段依赖 | L319–L335 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0319) |
| <a id="map-orig-msc-l0336"></a>MSC:28 | 5. 验收与证据要求 | L336–L346 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0336) |
| <a id="map-orig-msc-l0347"></a>MSC:29 | 6. 红线 | L347–L359 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0347) |
| <a id="map-orig-msc-l0360"></a>MSC:30 | 7. 与 Ronova 内部规范的关系 | L360–L374 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-msc-l0360) |

<a id="source-msr"></a>

### MSR · Ronova_MS_Remediation_Spec_V1.0.md

原路径：`docs/plans/Ronova_MS_Remediation_Spec_V1.0.md`。 [原始快照](../archive/plans-before-ms-restructure-r66/Ronova_MS_Remediation_Spec_V1.0.md)。

| 映射锚点 | 旧标题（原编号不变） | 旧行范围 | 新 MS 章节 | 新正文落点 |
|---|---|---|---|---|
| <a id="map-orig-msr-l0001"></a>MSR:1 | Ronova MS 未满足条款 —— 可选强化设计 V1.0 | L1–L36 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0001) |
| <a id="map-orig-msr-l0037"></a>MSR:2 | S1 —— 判定收回 Java（R22 / `skills:358`） | L37–L38 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0037) |
| <a id="map-orig-msr-l0039"></a>MSR:3 | S1.1 违规事实 | L39–L57 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0039) |
| <a id="map-orig-msr-l0058"></a>MSR:4 | S1.2 目标形态 | L58–L65 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0058) |
| <a id="map-orig-msr-l0066"></a>MSR:5 | S1.3 步骤 | L66–L83 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0066) |
| <a id="map-orig-msr-l0084"></a>MSR:6 | S1.4 判据 | L84–L93 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0084) |
| <a id="map-orig-msr-l0094"></a>MSR:7 | S2 —— 重转换/重定义纪律（R17 / `skills:340-346`） | L94–L95 | skills / 重转换与重定义 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0094) |
| <a id="map-orig-msr-l0096"></a>MSR:8 | S2.1 违规事实 | L96–L114 | skills / 重转换与重定义 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0096) |
| <a id="map-orig-msr-l0115"></a>MSR:9 | S2.2 目标形态：`RedefinitionQueue` | L115–L143 | skills / 重转换与重定义 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0115) |
| <a id="map-orig-msr-l0144"></a>MSR:10 | S2.3 判据 | L144–L152 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0144) |
| <a id="map-orig-msr-l0153"></a>MSR:11 | S3 —— 七字段注入契约（R57 / `msitem:313-323`） | L153–L154 | skills / 注入声明 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0153) |
| <a id="map-orig-msr-l0155"></a>MSR:12 | S3.1 违规事实 | L155–L181 | skills / 注入声明 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0155) |
| <a id="map-orig-msr-l0182"></a>MSR:13 | S3.2 目标形态：可编译期检查的注解契约 | L182–L224 | skills / 注入声明 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0182) |
| <a id="map-orig-msr-l0225"></a>MSR:14 | S3.3 判据 | L225–L233 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0225) |
| <a id="map-orig-msr-l0234"></a>MSR:15 | S4 —— DLL 名 / 资源路径 / 拷贝路径单一来源（R31 / `skills:411`） | L234–L235 | skills / Native 实现规则 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0234) |
| <a id="map-orig-msr-l0236"></a>MSR:16 | S4.1 违规事实 | L236–L259 | skills / Native 实现规则 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0236) |
| <a id="map-orig-msr-l0260"></a>MSR:17 | S4.2 目标形态：`native/native-artifacts.json` 单一来源 | L260–L293 | skills / Native 实现规则 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0260) |
| <a id="map-orig-msr-l0294"></a>MSR:18 | S4.3 判据 | L294–L301 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0294) |
| <a id="map-orig-msr-l0302"></a>MSR:19 | S5 —— 启动前提检查与 `ping`（R26 / `skills:380-382`） | L302–L303 | skills / 启动顺序(对于ModMain优先级且对平台有限制的秒杀Mod) | [原文](MS-SKILLS-PLAN.md#orig-msr-l0302) |
| <a id="map-orig-msr-l0304"></a>MSR:20 | S5.1 违规事实 | L304–L320 | skills / 启动顺序(对于ModMain优先级且对平台有限制的秒杀Mod) | [原文](MS-SKILLS-PLAN.md#orig-msr-l0304) |
| <a id="map-orig-msr-l0321"></a>MSR:21 | S5.2 步骤 | L321–L337 | skills / 启动顺序(对于ModMain优先级且对平台有限制的秒杀Mod) | [原文](MS-SKILLS-PLAN.md#orig-msr-l0321) |
| <a id="map-orig-msr-l0338"></a>MSR:22 | S5.3 判据 | L338–L345 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0338) |
| <a id="map-orig-msr-l0346"></a>MSR:23 | S6 —— 失败策略显式化（R58 / `msitem:322`） | L346–L347 | skills / 注入声明 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0346) |
| <a id="map-orig-msr-l0348"></a>MSR:24 | S6.1 违规事实 | L348–L361 | skills / 注入声明 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0348) |
| <a id="map-orig-msr-l0362"></a>MSR:25 | S6.2 步骤 | L362–L379 | skills / 注入声明 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0362) |
| <a id="map-orig-msr-l0380"></a>MSR:26 | S6.3 判据 | L380–L387 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0380) |
| <a id="map-orig-msr-l0388"></a>MSR:27 | 实施顺序与依赖 | L388–L404 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0388) |
| <a id="map-orig-msr-l0405"></a>MSR:28 | 不做什么 | L405–L414 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-msr-l0405) |

<a id="source-rule"></a>

### RULE · Ronova_MS_Rulebound_Enhancement_Plan_V2.0.md

原路径：`docs/plans/Ronova_MS_Rulebound_Enhancement_Plan_V2.0.md`。 [原始快照](../archive/plans-before-ms-restructure-r66/Ronova_MS_Rulebound_Enhancement_Plan_V2.0.md)。

| 映射锚点 | 旧标题（原编号不变） | 旧行范围 | 新 MS 章节 | 新正文落点 |
|---|---|---|---|---|
| <a id="map-orig-rule-l0001"></a>RULE:1 | Ronova MS 规则内加强方案 V2.0 | L1–L18 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0001) |
| <a id="map-orig-rule-l0019"></a>RULE:2 | 0. 规则来源声明 | L19–L20 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0019) |
| <a id="map-orig-rule-l0021"></a>RULE:3 | 0.1 唯一规则来源 | L21–L33 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0021) |
| <a id="map-orig-rule-l0034"></a>RULE:4 | 0.2 明确不采用的规则来源 | L34–L46 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0034) |
| <a id="map-orig-rule-l0047"></a>RULE:5 | 0.2.1 唯一的例外：用户明示约束（`UX` 编号） | L47–L53 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0047) |
| <a id="map-orig-rule-l0054"></a>RULE:6 | 0.3 MS 文档本身的性质 | L54–L65 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0054) |
| <a id="map-orig-rule-l0066"></a>RULE:7 | 0.4 判定分层（2026-09-12 用户确立） | L66–L93 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0066) |
| <a id="map-orig-rule-l0094"></a>RULE:8 | 1. 规则基（L0 铁律 + L1 参考库） | L94–L97 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0094) |
| <a id="map-orig-rule-l0098"></a>RULE:9 | 闸门 0 —— MS 身份闸（三条铁律 + 一条推论） | L98–L111 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0098) |
| <a id="map-orig-rule-l0112"></a>RULE:10 | 闸门 0 附加 —— 用户明示约束 `UX1`（非 MS 来源，同等强制） | L112–L132 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0112) |
| <a id="map-orig-rule-l0133"></a>RULE:11 | 闸门 1 —— 目标判定闸 | L133–L140 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-rule-l0133) |
| <a id="map-orig-rule-l0141"></a>RULE:12 | 闸门 2 —— 改写纪律闸 | L141–L158 | skills / 重转换与重定义 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0141) |
| <a id="map-orig-rule-l0159"></a>RULE:13 | 闸门 3 —— 原生层定位闸 | L159–L174 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0159) |
| <a id="map-orig-rule-l0175"></a>RULE:14 | 闸门 4 —— 结算唯一性闸 | L175–L187 | msitem / 去重与幂等 | [原文](MS-MSITEM-PLAN.md#orig-rule-l0175) |
| <a id="map-orig-rule-l0188"></a>RULE:15 | 闸门 5 —— 客户端/服务端边界闸 | L188–L197 | msitem / 服务端与客户端一致性 | [原文](MS-MSITEM-PLAN.md#orig-rule-l0188) |
| <a id="map-orig-rule-l0198"></a>RULE:16 | 闸门 6 —— 状态与修复闸 | L198–L214 | entitydefence / 构建幂等修复过程 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0198) |
| <a id="map-orig-rule-l0215"></a>RULE:17 | 闸门 7 —— 声明完整性闸 | L215–L223 | skills / 注入声明 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0215) |
| <a id="map-orig-rule-l0224"></a>RULE:18 | 闸门 8 —— 完成标准闸 | L224–L234 | skills / 完成标准(以一个标准的，带Native的秒杀Mod举例) | [原文](MS-SKILLS-PLAN.md#orig-rule-l0224) |
| <a id="map-orig-rule-l0235"></a>RULE:19 | 1.1 MS 文档内部的一处张力（已裁决） | L235–L246 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0235) |
| <a id="map-orig-rule-l0247"></a>RULE:20 | 2. V1.0 红线的逐条重裁 | L247–L266 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0247) |
| <a id="map-orig-rule-l0267"></a>RULE:21 | 3. 重新纳入的能力空间 | L267–L296 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0267) |
| <a id="map-orig-rule-l0297"></a>RULE:22 | 3.1 参考样本的 MS 合规判定 | L297–L316 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0297) |
| <a id="map-orig-rule-l0317"></a>RULE:23 | 3.1.1 同一批样本对 L1 条款（R17/R22/R26/R31/R57/R58）的对照 | L317–L344 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0317) |
| <a id="map-orig-rule-l0345"></a>RULE:24 | 4. 缺口清单（按 §0.4 分层） | L345–L352 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0345) |
| <a id="map-orig-rule-l0353"></a>RULE:25 | 4.0 判定速览（直接回答「有没有违规」） | L353–L389 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0353) |
| <a id="map-orig-rule-l0390"></a>RULE:26 | N1 —— Native 是业务规则的第二份实现（R22）——比初判更严重 | L390–L427 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0390) |
| <a id="map-orig-rule-l0428"></a>RULE:27 | N2 —— DLL 名 / 资源路径 / 拷贝路径三处手写常量（R31） | L428–L435 | skills / Native 实现规则 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0428) |
| <a id="map-orig-rule-l0436"></a>RULE:28 | N3 —— 启动顺序缺 `ping` 与 Java 17 前提检查（R26） | L436–L441 | skills / 启动顺序(对于ModMain优先级且对平台有限制的秒杀Mod) | [原文](MS-SKILLS-PLAN.md#orig-rule-l0436) |
| <a id="map-orig-rule-l0442"></a>RULE:29 | N4 —— 失败策略未声明（R57/R58） | L442–L449 | skills / 注入声明 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0442) |
| <a id="map-orig-rule-l0450"></a>RULE:30 | N5 —— 七字段注入契约（R57）——抽样 7 处，最多命中 4/7 | L450–L475 | skills / 注入声明 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0450) |
| <a id="map-orig-rule-l0476"></a>RULE:31 | N6 —— 重转换/重定义纪律（R17）——已验证缺口 | L476–L505 | skills / 重转换与重定义 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0476) |
| <a id="map-orig-rule-l0506"></a>RULE:32 | N7 —— Mixin 不得是唯一实现（R59′） | L506–L511 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0506) |
| <a id="map-orig-rule-l0512"></a>RULE:33 | N8 —— 独占过滤：开关归属待确认 + 5 条后果未记录（R52） | L512–L537 | entitydefence / 可选：实体列表独占防御 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0512) |
| <a id="map-orig-rule-l0538"></a>RULE:34 | N9 —— 规范实例与修复契约（R46/R47/R49/R50/R51） | L538–L542 | entitydefence / 保留一个规范实例 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0538) |
| <a id="map-orig-rule-l0543"></a>RULE:35 | N10 —— 原生构建与自映像探针（R66） | L543–L549 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0543) |
| <a id="map-orig-rule-l0550"></a>RULE:36 | N11 —— 调度收敛（R32/R64） | L550–L558 | msitem / 列出全部击杀入口 | [原文](MS-MSITEM-PLAN.md#orig-rule-l0550) |
| <a id="map-orig-rule-l0559"></a>RULE:37 | N12 —— R1 边界：恢复链会改写按名字选中的 SavedData 文件（**已结论：不违反铁律**） | L559–L610 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0559) |
| <a id="map-orig-rule-l0611"></a>RULE:38 | N13 —— 同一规则集在两处维护 + 原生名称启发式 | L611–L635 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0611) |
| <a id="map-orig-rule-l0636"></a>RULE:39 | N14 —— 客户端边界的一处未加保护的公共→客户端调用（R42，当前不可达） | L636–L651 | skills / 客户端 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0636) |
| <a id="map-orig-rule-l0652"></a>RULE:40 | N15 —— 改写的可达集合包含 Forge / ModLauncher / LWJGL / com.mojang（R8 点名的类） | L652–L682 | skills / 重转换与重定义 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0652) |
| <a id="map-orig-rule-l0683"></a>RULE:41 | N16 —— 急切重转换的选取启发式按通用英文子串命中第三方类 | L683–L697 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0683) |
| <a id="map-orig-rule-l0698"></a>RULE:42 | 4.1 对其他 Mod 的影响面（合规口径） | L698–L715 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0698) |
| <a id="map-orig-rule-l0716"></a>RULE:43 | 4.2 已满足的项（同一口径下核对，避免只列缺口） | L716–L729 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0716) |
| <a id="map-orig-rule-l0730"></a>RULE:44 | 5. 加强块 | L730–L733 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0730) |
| <a id="map-orig-rule-l0734"></a>RULE:45 | A 块 —— 前置与入口 | L734–L747 | skills / 对于非MoMain优先级 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0734) |
| <a id="map-orig-rule-l0748"></a>RULE:46 | B 块 —— 原生能力位扩展（纯增量，性价比最高） | L748–L766 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0748) |
| <a id="map-orig-rule-l0767"></a>RULE:47 | C 块 —— 收口项（C15 是强制；C1–C14、C16–C18 为 L1 自定） | L767–L797 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0767) |
| <a id="map-orig-rule-l0798"></a>RULE:48 | D 块 —— 执行器与结算（能力本体） | L798–L806 | msitem / 正常死亡、运行时清除与混合执行 | [原文](MS-MSITEM-PLAN.md#orig-rule-l0798) |
| <a id="map-orig-rule-l0807"></a>RULE:49 | E 块 —— 图与证书 | L807–L815 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0807) |
| <a id="map-orig-rule-l0816"></a>RULE:50 | F 块 —— 证明与策略收口 | L816–L827 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0816) |
| <a id="map-orig-rule-l0828"></a>RULE:51 | G 块 —— 原生强制下沉 | L828–L836 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0828) |
| <a id="map-orig-rule-l0837"></a>RULE:52 | H 块 —— 新维度 | L837–L844 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0837) |
| <a id="map-orig-rule-l0845"></a>RULE:53 | I 块 —— 本轮新纳入的高强度手段（V1.0 明确不采纳的翻案） | L845–L868 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0845) |
| <a id="map-orig-rule-l0869"></a>RULE:54 | 6. 实施顺序 | L869–L911 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0869) |
| <a id="map-orig-rule-l0912"></a>RULE:55 | 7. MS 文档之外的约束与裁决 | L912–L916 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0912) |
| <a id="map-orig-rule-l0917"></a>RULE:56 | N12 —— 已结论：不违反铁律（记录备查） | L917–L925 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0917) |
| <a id="map-orig-rule-l0926"></a>RULE:57 | UX1 —— 「无外部」（不起子进程）—— **已确认，强制约束** | L926–L948 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0926) |
| <a id="map-orig-rule-l0949"></a>RULE:58 | U2 —— 自有的可验证性取向（不是裁决项，是优先级说明） | L949–L956 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0949) |
| <a id="map-orig-rule-l0957"></a>RULE:59 | 8. 验证：全部后置 | L957–L973 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-rule-l0957) |
| <a id="map-orig-rule-l0974"></a>RULE:60 | 9. 完成判定 | L974–L977 | skills / 完成标准(以一个标准的，带Native的秒杀Mod举例) | [原文](MS-SKILLS-PLAN.md#orig-rule-l0974) |
| <a id="map-orig-rule-l0978"></a>RULE:61 | 9.1 强制项（L0 铁律 + UX 用户约束）——不达成即不合格 | L978–L986 | skills / 完成标准(以一个标准的，带Native的秒杀Mod举例) | [原文](MS-SKILLS-PLAN.md#orig-rule-l0978) |
| <a id="map-orig-rule-l0987"></a>RULE:62 | 9.2 自定项（L1）——由 Ronova 自己决定，达成了才写进判据 | L987–L1007 | skills / 完成标准(以一个标准的，带Native的秒杀Mod举例) | [原文](MS-SKILLS-PLAN.md#orig-rule-l0987) |
| <a id="map-orig-rule-l1008"></a>RULE:63 | 附：与 V1.0 的差异速查 | L1008–L1026 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-rule-l1008) |

<a id="source-non"></a>

### NON · Ronova_NonViolating_Enhancement_Plan_V1.0.md

原路径：`docs/plans/Ronova_NonViolating_Enhancement_Plan_V1.0.md`。 [原始快照](../archive/plans-before-ms-restructure-r66/Ronova_NonViolating_Enhancement_Plan_V1.0.md)。

| 映射锚点 | 旧标题（原编号不变） | 旧行范围 | 新 MS 章节 | 新正文落点 |
|---|---|---|---|---|
| <a id="map-orig-non-l0001"></a>NON:1 | Ronova 无违规加强方案 V1.0 | L1–L24 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-non-l0001) |
| <a id="map-orig-non-l0025"></a>NON:2 | 1. 红线（可判定） | L25–L37 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-non-l0025) |
| <a id="map-orig-non-l0038"></a>NON:3 | 1.1 W8 的边界（本轮实测修正） | L38–L56 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-non-l0038) |
| <a id="map-orig-non-l0057"></a>NON:4 | 2. 可达上限：T0 三件套能到哪 | L57–L68 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-non-l0057) |
| <a id="map-orig-non-l0069"></a>NON:5 | 2.1 比 T0 更强的四个方向 | L69–L77 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-non-l0069) |
| <a id="map-orig-non-l0078"></a>NON:6 | 2.2 一条硬边界 | L78–L86 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-non-l0078) |
| <a id="map-orig-non-l0087"></a>NON:7 | 3. 加强块（按实现顺序） | L87–L88 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-non-l0087) |
| <a id="map-orig-non-l0089"></a>NON:8 | A 块 —— 前置与入口 | L89–L101 | skills / 对于非MoMain优先级 | [原文](MS-SKILLS-PLAN.md#orig-non-l0089) |
| <a id="map-orig-non-l0102"></a>NON:9 | B 块 —— 原生能力扩展 | L102–L128 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-non-l0102) |
| <a id="map-orig-non-l0129"></a>NON:10 | C 块 —— 合规整改（减法，先做） | L129–L144 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-non-l0129) |
| <a id="map-orig-non-l0145"></a>NON:11 | D 块 —— 执行器补齐（能力本体） | L145–L152 | msitem / 正常死亡、运行时清除与混合执行 | [原文](MS-MSITEM-PLAN.md#orig-non-l0145) |
| <a id="map-orig-non-l0153"></a>NON:12 | E 块 —— 图与证书（极限档本体） | L153–L164 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-non-l0153) |
| <a id="map-orig-non-l0165"></a>NON:13 | F 块 —— 证明与策略收口 | L165–L175 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-non-l0165) |
| <a id="map-orig-non-l0176"></a>NON:14 | G 块 —— 原生强制 | L176–L184 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-non-l0176) |
| <a id="map-orig-non-l0185"></a>NON:15 | H 块 —— 新维度（样本与文档补读所得） | L185–L195 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-non-l0185) |
| <a id="map-orig-non-l0196"></a>NON:16 | 4. 明确不采纳 | L196–L216 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-non-l0196) |
| <a id="map-orig-non-l0217"></a>NON:17 | 5. 实施顺序与依赖 | L217–L253 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-non-l0217) |
| <a id="map-orig-non-l0254"></a>NON:18 | 6. 验证：全部后置 | L254–L275 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-non-l0254) |
| <a id="map-orig-non-l0276"></a>NON:19 | 7. 完成判定 | L276–L296 | skills / 完成标准(以一个标准的，带Native的秒杀Mod举例) | [原文](MS-SKILLS-PLAN.md#orig-non-l0276) |

<a id="source-ds"></a>

### DS · DESIGN-SPEC.md

原路径：`docs/DESIGN-SPEC.md`。 [原始快照](../archive/plans-before-ms-restructure-r66/DESIGN-SPEC.md)。

| 映射锚点 | 旧标题（原编号不变） | 旧行范围 | 新 MS 章节 | 新正文落点 |
|---|---|---|---|---|
| <a id="map-orig-ds-l0001"></a>DS:1 | Ronova 设计规范 · 合并导读 | L1–L18 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0001) |
| <a id="map-orig-ds-l0019"></a>DS:2 | 1. 权威顺序与效力 | L19–L32 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0019) |
| <a id="map-orig-ds-l0033"></a>DS:3 | 1.1 冲突处理规则（五条，跨文档一致） | L33–L43 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0033) |
| <a id="map-orig-ds-l0044"></a>DS:4 | 1.2 违反顺序的典型错误（各处点名） | L44–L52 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0044) |
| <a id="map-orig-ds-l0053"></a>DS:5 | 2. 冻结词汇（`CONTEXT.md`） | L53–L56 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0053) |
| <a id="map-orig-ds-l0057"></a>DS:6 | 2.1 观察与可见性 | L57–L64 | entitydefence / 防御读取路径 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0057) |
| <a id="map-orig-ds-l0065"></a>DS:7 | 2.2 判定与终结语义 | L65–L74 | msitem / 运行时清除 | [原文](MS-MSITEM-PLAN.md#orig-ds-l0065) |
| <a id="map-orig-ds-l0075"></a>DS:8 | 2.3 权威与免疫 | L75–L98 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0075) |
| <a id="map-orig-ds-l0099"></a>DS:9 | 3. 身份与修订合同（`CF` §3–§4） | L99–L100 | entitydefence / 保留一个规范实例 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0099) |
| <a id="map-orig-ds-l0101"></a>DS:10 | 3.1 身份成员与排除（`CF:248-254`） | L101–L109 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0101) |
| <a id="map-orig-ds-l0110"></a>DS:11 | 3.2 `RealmId`（`CF:213-222`） | L110–L114 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0110) |
| <a id="map-orig-ds-l0115"></a>DS:12 | 3.3 `PhysicalIncarnationId`（`CF:258-278`）—— **不是身份** | L115–L124 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0115) |
| <a id="map-orig-ds-l0125"></a>DS:13 | 3.4 三个 revision 的规则（`CF:284-302`） | L125–L133 | entitydefence / 规范实例规则 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0125) |
| <a id="map-orig-ds-l0134"></a>DS:14 | 3.5 `RevisionCrossingPolicy`（`CF:538-553`）—— 三模式，封闭 | L134–L143 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0134) |
| <a id="map-orig-ds-l0144"></a>DS:15 | 3.6 操作与完成标识（`CF:325-369`） | L144–L153 | msitem / 用统一动作上下文消除重复 | [原文](MS-MSITEM-PLAN.md#orig-ds-l0144) |
| <a id="map-orig-ds-l0154"></a>DS:16 | 3.7 §25 冻结接口集（`CF:1297-1334`）—— P0-A 前必须冻结的 30 个名字 | L154–L165 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0154) |
| <a id="map-orig-ds-l0166"></a>DS:17 | 4. 权威与状态所有权 | L166–L167 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0166) |
| <a id="map-orig-ds-l0168"></a>DS:18 | 4.1 Internal Root 不变（`L6:12-14`、`L6:174-181`、`V2.6:174-181`） | L168–L176 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0168) |
| <a id="map-orig-ds-l0177"></a>DS:19 | 4.2 D1–D13（最高权威冻结决策，`L6:45-70`） | L177–L196 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0177) |
| <a id="map-orig-ds-l0197"></a>DS:20 | 4.3 Single-Writer 不变量（`L6:415-436`） | L197–L204 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0197) |
| <a id="map-orig-ds-l0205"></a>DS:21 | 4.4 目标状态所有权（`L6:393-411`） | L205–L226 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0205) |
| <a id="map-orig-ds-l0227"></a>DS:22 | 4.5 模块「不做什么」清单（`L6:337-349`） | L227–L242 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0227) |
| <a id="map-orig-ds-l0243"></a>DS:23 | 4.6 编译期依赖约束（`L6:487-494`）—— 可静态检查 | L243–L255 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0243) |
| <a id="map-orig-ds-l0256"></a>DS:24 | 4.7 Presence 保证范围（`L6:351-357`，关闭 H6） | L256–L264 | entitydefence / 防御读取路径 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0256) |
| <a id="map-orig-ds-l0265"></a>DS:25 | 5. 不变量全集 | L265–L266 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0265) |
| <a id="map-orig-ds-l0267"></a>DS:26 | 5.1 跨阶段必要条件 `PROC-INV-1`（= `D13`，`L6:543-545`） | L267–L282 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0267) |
| <a id="map-orig-ds-l0283"></a>DS:27 | 5.2 FULL L5 不变量（`L6:504-541`） | L283–L298 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0283) |
| <a id="map-orig-ds-l0299"></a>DS:28 | 5.3 FULL L6 Internal 不变量（`L6:861-873`） | L299–L318 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0299) |
| <a id="map-orig-ds-l0319"></a>DS:29 | 5.4 `D33` 修订：Final Write Sink 不得绝对化（`V2.6:472-501`） | L319–L328 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0319) |
| <a id="map-orig-ds-l0329"></a>DS:30 | 5.5 `D53`–`D106`（`V2.6` 强不变量，全部继续有效） | L329–L363 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0329) |
| <a id="map-orig-ds-l0364"></a>DS:31 | 5.6 `D1`–`D32` 与 `D33`–`D106` 之外的两组宪法级不变量 | L364–L384 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0364) |
| <a id="map-orig-ds-l0385"></a>DS:32 | 5.7 P0-A 核心不变量 `A1`–`A8`（`P0A:92-197`） | L385–L424 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0385) |
| <a id="map-orig-ds-l0425"></a>DS:33 | 5.8 Baseline V1.4 的阶段与门（`BL`） | L425–L460 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0425) |
| <a id="map-orig-ds-l0461"></a>DS:34 | 6. 执行与宿主边界 | L461–L462 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0461) |
| <a id="map-orig-ds-l0463"></a>DS:35 | 6.1 进程边界：`R7`（`CF:193-207`）+ `D13`/`PROC-INV-1` | L463–L487 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0463) |
| <a id="map-orig-ds-l0488"></a>DS:36 | 6.2 执行上下文（`CF:508-536`） | L488–L503 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0488) |
| <a id="map-orig-ds-l0504"></a>DS:37 | 6.3 同步查询 vs 可延期工作（`BL:2584`、`CF:1521-1523`、`L6:733`） | L504–L515 | skills / 逻辑服务器 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0504) |
| <a id="map-orig-ds-l0516"></a>DS:38 | 6.4 预算与资源（`L6:722-748`、`L6:1188-1200`） | L516–L536 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0516) |
| <a id="map-orig-ds-l0537"></a>DS:39 | 6.5 宿主安全域（`D81`/`D82`/`D96`） | L537–L547 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0537) |
| <a id="map-orig-ds-l0548"></a>DS:40 | 6.6 活性与降级（`CF:858-903`、`CF:989-1007`） | L548–L560 | entitydefence / 构建幂等修复过程 | [原文](MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0548) |
| <a id="map-orig-ds-l0561"></a>DS:41 | 7. 极限档的绝对安全边界与代价（`V2.6` §4.4 / §22.4） | L561–L562 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0561) |
| <a id="map-orig-ds-l0563"></a>DS:42 | 7.1 绝对安全边界 —— 13 条（`V2.6:431-445`） | L563–L584 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0563) |
| <a id="map-orig-ds-l0585"></a>DS:43 | 7.2 必须计的代价（`V2.6:449`、`:3288`） | L585–L604 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0585) |
| <a id="map-orig-ds-l0605"></a>DS:44 | 7.3 明确不保证的（`V2.6:419-427`） | L605–L616 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0605) |
| <a id="map-orig-ds-l0617"></a>DS:45 | 7.4 禁止写死的架构上限（`V2.6:404-415`） | L617–L623 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0617) |
| <a id="map-orig-ds-l0624"></a>DS:46 | 7.5 资源与容量（`V2.6` §12.2 / §14.6 / §20） | L624–L647 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0624) |
| <a id="map-orig-ds-l0648"></a>DS:47 | 7.6 极限档"非免疫"的明确声明 | L648–L658 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0648) |
| <a id="map-orig-ds-l0659"></a>DS:48 | 8. 客户端 / 服务端边界 | L659–L676 | skills / 客户端 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0659) |
| <a id="map-orig-ds-l0677"></a>DS:49 | 9. 副作用合同（`CF` §16 + `ID-2`） | L677–L697 | msitem / 去重与幂等 | [原文](MS-MSITEM-PLAN.md#orig-ds-l0677) |
| <a id="map-orig-ds-l0698"></a>DS:50 | 10. 强度模型（`V2.6`） | L698–L707 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-ds-l0698) |
| <a id="map-orig-ds-l0708"></a>DS:51 | 10.1 三档正式预设（`V2.6:41-45`、`:600-604`） | L708–L720 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-ds-l0708) |
| <a id="map-orig-ds-l0721"></a>DS:52 | 10.2 两套**不同**的层级值集（不要合并） | L721–L729 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-ds-l0721) |
| <a id="map-orig-ds-l0730"></a>DS:53 | 10.3 五个策略字段与组合算子（`V2.6:649-673`、`:765-774`） | L730–L768 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-ds-l0730) |
| <a id="map-orig-ds-l0769"></a>DS:54 | 10.4 表面成熟度阶梯（`V2.6:1259-1267`） | L769–L786 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-ds-l0769) |
| <a id="map-orig-ds-l0787"></a>DS:55 | 10.5 逐键矩阵与三档差异性（`D91`、`V2.6:877`） | L787–L795 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-ds-l0787) |
| <a id="map-orig-ds-l0796"></a>DS:56 | 10.6 `AttackAction` 的逐档许可（`V2.6:895-910`） | L796–L810 | msitem / 统一目标、结果和防御策略 | [原文](MS-MSITEM-PLAN.md#orig-ds-l0796) |
| <a id="map-orig-ds-l0811"></a>DS:57 | 11. 非法组合（`V2.6:2828-2885`） | L811–L843 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0811) |
| <a id="map-orig-ds-l0844"></a>DS:58 | 12. 完成判定的元原则 | L844–L870 | skills / 完成标准(以一个标准的，带Native的秒杀Mod举例) | [原文](MS-SKILLS-PLAN.md#orig-ds-l0844) |
| <a id="map-orig-ds-l0871"></a>DS:59 | 13. 覆盖进度 | L871–L889 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0871) |
| <a id="map-orig-ds-l0890"></a>DS:60 | 14. 已知的文档级问题（如实记录，不代为裁决） | L890–L906 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0890) |
| <a id="map-orig-ds-l0907"></a>DS:61 | 附：本导读不做什么 | L907–L917 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0907) |
| <a id="map-orig-ds-l0918"></a>DS:62 | 第二部分 · 加强方案（**提案，非规范**） | L918–L931 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0918) |
| <a id="map-orig-ds-l0932"></a>DS:63 | 15. 三层规则源（判定力完全不同） | L932–L947 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0932) |
| <a id="map-orig-ds-l0948"></a>DS:64 | 16. 合规判定 | L948–L949 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0948) |
| <a id="map-orig-ds-l0950"></a>DS:65 | 16.1 对外（L0 铁律 + UX1） | L950–L961 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0950) |
| <a id="map-orig-ds-l0962"></a>DS:66 | 16.2 对内（P 项目自有规范）—— 本轮整合新增的维度 | L962–L985 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0962) |
| <a id="map-orig-ds-l0986"></a>DS:67 | 16.3 跨所有权 `SavedData` 写入 —— 结论：不违反 L0 | L986–L1002 | skills / 先确定能力边界 | [原文](MS-SKILLS-PLAN.md#orig-ds-l0986) |
| <a id="map-orig-ds-l1003"></a>DS:68 | 17. 与项目自有规范对齐后的加强块 | L1003–L1006 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-ds-l1003) |
| <a id="map-orig-ds-l1007"></a>DS:69 | A 块 —— 前置与入口（服务 `UX1` + `R7` + `PROC-INV-1`） | L1007–L1020 | skills / 对于非MoMain优先级 | [原文](MS-SKILLS-PLAN.md#orig-ds-l1007) |
| <a id="map-orig-ds-l1021"></a>DS:70 | B 块 —— 原生能力位扩展（纯增量） | L1021–L1039 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-ds-l1021) |
| <a id="map-orig-ds-l1040"></a>DS:71 | C 块 —— 收口项 | L1040–L1065 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-ds-l1040) |
| <a id="map-orig-ds-l1066"></a>DS:72 | D 块 —— 执行器与结算（能力本体） | L1066–L1071 | msitem / 正常死亡、运行时清除与混合执行 | [原文](MS-MSITEM-PLAN.md#orig-ds-l1066) |
| <a id="map-orig-ds-l1072"></a>DS:73 | E 块 —— 图与证书 | L1072–L1082 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l1072) |
| <a id="map-orig-ds-l1083"></a>DS:74 | F 块 —— 证明与策略收口 | L1083–L1099 | skills / 自定义Mixin或转换器 | [原文](MS-SKILLS-PLAN.md#orig-ds-l1083) |
| <a id="map-orig-ds-l1100"></a>DS:75 | G 块 —— 原生强制下沉 | L1100–L1106 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-ds-l1100) |
| <a id="map-orig-ds-l1107"></a>DS:76 | H 块 —— 新维度 | L1107–L1114 | skills / 总体架构 | [原文](MS-SKILLS-PLAN.md#orig-ds-l1107) |
| <a id="map-orig-ds-l1115"></a>DS:77 | I 块 —— MS 未禁止、但本方案**不建议**优先做的 | L1115–L1125 | skills / JNA/JNI/JVMTI Native 层 | [原文](MS-SKILLS-PLAN.md#orig-ds-l1115) |
| <a id="map-orig-ds-l1126"></a>DS:78 | 18. 实施顺序 | L1126–L1150 | skills / 新增功能的实现清单 | [原文](MS-SKILLS-PLAN.md#orig-ds-l1126) |
| <a id="map-orig-ds-l1151"></a>DS:79 | 19. 验证：全部后置 | L1151–L1159 | skills / 构建、打包与验证 | [原文](MS-SKILLS-PLAN.md#orig-ds-l1151) |
| <a id="map-orig-ds-l1160"></a>DS:80 | 20. 完成判定 | L1160–L1169 | skills / 完成标准(以一个标准的，带Native的秒杀Mod举例) | [原文](MS-SKILLS-PLAN.md#orig-ds-l1160) |

## 8. 旧行号跨段引用索引

这些原引用跨越了多个迁入段落；保留原行号作为来源定位，逐一链接新落点，避免只跳到第一段而隐去余下依据。

| 范围锚点 | 原来源范围 | 新落点（原序） |
|---|---|---|
| <a id="range-cf-78-207"></a>range-cf-78-207 | CF:L78–L207 | [2. Global Redlines](MS-SKILLS-PLAN.md#orig-cf-l0078)；[R1 — No Mod-ID Authority](MS-SKILLS-PLAN.md#orig-cf-l0082)；[R2 — Non-Blocking ≠ Hookless](MS-SKILLS-PLAN.md#orig-cf-l0100)；[R3 — Hardened 必须是 Non-Blocking Core 的超集](MS-MSITEM-PLAN.md#orig-cf-l0117)；[R4 — Server Authority ≠ Client Pixel Authority](MS-SKILLS-PLAN.md#orig-cf-l0140)；[R5 — Unknown ≠ False ≠ Safe](MS-SKILLS-PLAN.md#orig-cf-l0160)；[R6 — Repair Success ≠ Trust Restored](MS-SKILLS-PLAN.md#orig-cf-l0178)；[R7 — Internal Root Process-Role Boundary](MS-SKILLS-PLAN.md#orig-cf-l0193) |
| <a id="range-cf-325-369"></a>range-cf-325-369 | CF:L325–L369 | [3.6 CriticalOperationId](MS-MSITEM-PLAN.md#orig-cf-l0325)；[3.7 CompletionContractId](MS-SKILLS-PLAN.md#orig-cf-l0351) |
| <a id="range-cf-325-371"></a>range-cf-325-371 | CF:L325–L371 | [3.6 CriticalOperationId](MS-MSITEM-PLAN.md#orig-cf-l0325)；[3.7 CompletionContractId](MS-SKILLS-PLAN.md#orig-cf-l0351) |
| <a id="range-cf-858-903"></a>range-cf-858-903 | CF:L858–L903 | [14. Liveness Contract](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0858)；[14.1 Liveness States](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0876)；[14.2 Liveness Safety Rule](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0888) |
| <a id="range-l6-45-70"></a>range-l6-45-70 | L6:L45–L70 | [0.1 V2 冻结决策（本版新增，优先级高于正文其余描述）](MS-SKILLS-PLAN.md#orig-l6-l0045)；[0.2 V2.4 跨规格闭合决策（优先级与 D1–D8 相同）](MS-SKILLS-PLAN.md#orig-l6-l0060) |
| <a id="range-l6-174-181"></a>range-l6-174-181 | L6:L174–L181 | [1.3.6 Root（根权威）](MS-SKILLS-PLAN.md#orig-l6-l0170)；[1.4 三条"权力倒流"（CONFIRMED，保留 V1）](MS-SKILLS-PLAN.md#orig-l6-l0178) |
| <a id="range-l6-722-748"></a>range-l6-722-748 | L6:L722–L748 | [7.2 统一预算框架（V2 增 presence/commit 行）](MS-SKILLS-PLAN.md#orig-l6-l0720)；[7.3 各工作的具体策略（保留 V1）](MS-SKILLS-PLAN.md#orig-l6-l0737)；[7.4 Storm Control（保留 V1）](MS-SKILLS-PLAN.md#orig-l6-l0744) |
| <a id="range-msr-37-93"></a>range-msr-37-93 | MSR:L37–L93 | [S1 —— 判定收回 Java（R22 / `skills:358`）](MS-SKILLS-PLAN.md#orig-msr-l0037)；[S1.1 违规事实](MS-SKILLS-PLAN.md#orig-msr-l0039)；[S1.2 目标形态](MS-SKILLS-PLAN.md#orig-msr-l0058)；[S1.3 步骤](MS-SKILLS-PLAN.md#orig-msr-l0066)；[S1.4 判据](MS-SKILLS-PLAN.md#orig-msr-l0084) |
| <a id="range-msr-94-152"></a>range-msr-94-152 | MSR:L94–L152 | [S2 —— 重转换/重定义纪律（R17 / `skills:340-346`）](MS-SKILLS-PLAN.md#orig-msr-l0094)；[S2.1 违规事实](MS-SKILLS-PLAN.md#orig-msr-l0096)；[S2.2 目标形态：`RedefinitionQueue`](MS-SKILLS-PLAN.md#orig-msr-l0115)；[S2.3 判据](MS-SKILLS-PLAN.md#orig-msr-l0144) |
| <a id="range-msr-153-233"></a>range-msr-153-233 | MSR:L153–L233 | [S3 —— 七字段注入契约（R57 / `msitem:313-323`）](MS-SKILLS-PLAN.md#orig-msr-l0153)；[S3.1 违规事实](MS-SKILLS-PLAN.md#orig-msr-l0155)；[S3.2 目标形态：可编译期检查的注解契约](MS-SKILLS-PLAN.md#orig-msr-l0182)；[S3.3 判据](MS-SKILLS-PLAN.md#orig-msr-l0225) |
| <a id="range-msr-234-301"></a>range-msr-234-301 | MSR:L234–L301 | [S4 —— DLL 名 / 资源路径 / 拷贝路径单一来源（R31 / `skills:411`）](MS-SKILLS-PLAN.md#orig-msr-l0234)；[S4.1 违规事实](MS-SKILLS-PLAN.md#orig-msr-l0236)；[S4.2 目标形态：`native/native-artifacts.json` 单一来源](MS-SKILLS-PLAN.md#orig-msr-l0260)；[S4.3 判据](MS-SKILLS-PLAN.md#orig-msr-l0294) |
| <a id="range-msr-302-345"></a>range-msr-302-345 | MSR:L302–L345 | [S5 —— 启动前提检查与 `ping`（R26 / `skills:380-382`）](MS-SKILLS-PLAN.md#orig-msr-l0302)；[S5.1 违规事实](MS-SKILLS-PLAN.md#orig-msr-l0304)；[S5.2 步骤](MS-SKILLS-PLAN.md#orig-msr-l0321)；[S5.3 判据](MS-SKILLS-PLAN.md#orig-msr-l0338) |
| <a id="range-msr-346-387"></a>range-msr-346-387 | MSR:L346–L387 | [S6 —— 失败策略显式化（R58 / `msitem:322`）](MS-SKILLS-PLAN.md#orig-msr-l0346)；[S6.1 违规事实](MS-SKILLS-PLAN.md#orig-msr-l0348)；[S6.2 步骤](MS-SKILLS-PLAN.md#orig-msr-l0362)；[S6.3 判据](MS-SKILLS-PLAN.md#orig-msr-l0380) |
| <a id="range-p0a-92-197"></a>range-p0a-92-197 | P0A:L92–L197 | [2. P0-A 核心不变量](MS-SKILLS-PLAN.md#orig-p0a-l0090)；[A1 — Subject Identity Stability](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0094)；[A2 — Incarnation Non-Inheritance](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0105)；[A3 — Binding Monotonicity](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0121)；[A4 — Tombstone Dominance](MS-MSITEM-PLAN.md#orig-p0a-l0131)；[A5 — Root Writer Uniqueness](MS-SKILLS-PLAN.md#orig-p0a-l0152)；[A6 — Projection Is Not Truth](MS-SKILLS-PLAN.md#orig-p0a-l0160)；[A7 — Graph Is Not Root](MS-SKILLS-PLAN.md#orig-p0a-l0171)；[A8 — Production Process Boundary (`PROC-INV-1`)](MS-SKILLS-PLAN.md#orig-p0a-l0183) |
| <a id="range-p0a-94-197"></a>range-p0a-94-197 | P0A:L94–L197 | [A1 — Subject Identity Stability](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0094)；[A2 — Incarnation Non-Inheritance](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0105)；[A3 — Binding Monotonicity](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0121)；[A4 — Tombstone Dominance](MS-MSITEM-PLAN.md#orig-p0a-l0131)；[A5 — Root Writer Uniqueness](MS-SKILLS-PLAN.md#orig-p0a-l0152)；[A6 — Projection Is Not Truth](MS-SKILLS-PLAN.md#orig-p0a-l0160)；[A7 — Graph Is Not Root](MS-SKILLS-PLAN.md#orig-p0a-l0171)；[A8 — Production Process Boundary (`PROC-INV-1`)](MS-SKILLS-PLAN.md#orig-p0a-l0183) |
| <a id="range-v26-365-396"></a>range-v26-365-396 | V26:L365–L396 | [COMPATIBLE / 兼容](MS-MSITEM-PLAN.md#orig-v26-l0365)；[ENHANCED / 强化](MS-MSITEM-PLAN.md#orig-v26-l0375)；[EXTREME / 极限代码对抗](MS-MSITEM-PLAN.md#orig-v26-l0389) |
| <a id="range-v26-365-400"></a>range-v26-365-400 | V26:L365–L400 | [COMPATIBLE / 兼容](MS-MSITEM-PLAN.md#orig-v26-l0365)；[ENHANCED / 强化](MS-MSITEM-PLAN.md#orig-v26-l0375)；[EXTREME / 极限代码对抗](MS-MSITEM-PLAN.md#orig-v26-l0389) |
| <a id="range-v26-1275-1326"></a>range-v26-1275-1326 | V26:L1275–L1326 | [9.1 三档攻击范围](MS-MSITEM-PLAN.md#orig-v26-l1275)；[Compatible](MS-MSITEM-PLAN.md#orig-v26-l1277)；[Enhanced](MS-MSITEM-PLAN.md#orig-v26-l1288)；[Extreme](MS-MSITEM-PLAN.md#orig-v26-l1300) |

<a id="unresolved-references"></a>

## 9. 原引用的未决与输入缺件

未能从原文件标题/行号唯一定位的引用不猜改、不删除；下面逐项列出。代码围栏中的示例和历史文件路径原样保存，不为了“清零断链”伪造类、产物或不存在的文档。输入包原有的缺失文件链接另见 `restructure/link-audit.json`。

| 来源 | 原引用 | 原因 |
|---|---|---|
| — | 无未决原节号记录 | 这不是运行验收结果。 |

## 10. 保真与边界核对文件

[source-manifest.json](restructure/source-manifest.json) 保存原文件哈希；[section-map.json](restructure/section-map.json) 保存每段起止、目标、原/新哈希与可逆标记变更；[id-inventory.json](restructure/id-inventory.json) 按原文件列 ID 次数与落点；[reference-updates.json](restructure/reference-updates.json) 记录引用同步；[verification.json](restructure/verification.json) 记录文档核对事实。

复核程序只读取这些文档和原始快照，不编译或启动 Mod：`python docs/plans/restructure/verify_restructure.py`。它的结果不是能力/阶段/成品验收。

## 11. 原引用语义的保留边界

引用定位核对检查的是“旧文件、旧节号或旧行号是否仍能找到迁移后的原段落”，不是重新认证来源对那一节内容的解释。原文若采用过时别名、上下文简写或本身引用了不完全对应的节号，本轮不靠猜测改写原编号或补出不存在的原文。来源上下文、旧原文快照与可逆引用变更表共同保留其可追溯性。

输入包已有的缺失证据/附件链接单独列入 `restructure/link-audit.json`，没有用空文件或虚构运行记录补洞，也不以这些缺失项推断功能成功或失败。


**记录效力提示（fix3）：** 本页原有 `verification.json` / `link-audit.json` 引用属于初轮迁移核对，不代表后续修订的当前结果。[当前与历史记录索引](restructure/README.md)给出每份记录的适用基线；不按文件名或修改时间推导效力。

<a id="fix4-wiring-supplement"></a>
## fix4：能力接线增补归属

[当前增补规划](R66-CAPABILITY-WIRING-PLAN.md)挂接三书，不引入第四套骨架。本轮未关闭原 MS 缺口、未变更不适用分类或运行状态；原 1,047 段映射不变。

| 原文块落点 | 更正主题 | 新规划位置 |
|---|---|---|
| [orig-non-l0069](MS-SKILLS-PLAN.md#orig-non-l0069) | prefix | [增补](R66-CAPABILITY-WIRING-PLAN.md#prefix) |
| [orig-rule-l0816](MS-SKILLS-PLAN.md#orig-rule-l0816) | source-corrections | [增补](R66-CAPABILITY-WIRING-PLAN.md#source-corrections) |
| [orig-non-l0165](MS-SKILLS-PLAN.md#orig-non-l0165) | source-corrections | [增补](R66-CAPABILITY-WIRING-PLAN.md#source-corrections) |
| [orig-max-l0114](MS-SKILLS-PLAN.md#orig-max-l0114) | native-bind | [增补](R66-CAPABILITY-WIRING-PLAN.md#native-bind) |
| [orig-rule-l0748](MS-SKILLS-PLAN.md#orig-rule-l0748) | native-bind / prefix | [增补](R66-CAPABILITY-WIRING-PLAN.md#native-bind) · [增补](R66-CAPABILITY-WIRING-PLAN.md#prefix) |
| [orig-rule-l0845](MS-SKILLS-PLAN.md#orig-rule-l0845) | prefix | [增补](R66-CAPABILITY-WIRING-PLAN.md#prefix) |
| [orig-non-l0057](MS-SKILLS-PLAN.md#orig-non-l0057) | prefix | [增补](R66-CAPABILITY-WIRING-PLAN.md#prefix) |
| [orig-non-l0102](MS-SKILLS-PLAN.md#orig-non-l0102) | native-bind / prefix | [增补](R66-CAPABILITY-WIRING-PLAN.md#native-bind) · [增补](R66-CAPABILITY-WIRING-PLAN.md#prefix) |
| [orig-ds-l1021](MS-SKILLS-PLAN.md#orig-ds-l1021) | native-bind / prefix | [增补](R66-CAPABILITY-WIRING-PLAN.md#native-bind) · [增补](R66-CAPABILITY-WIRING-PLAN.md#prefix) |
| [orig-ds-l1040](MS-SKILLS-PLAN.md#orig-ds-l1040) | source-corrections | [增补](R66-CAPABILITY-WIRING-PLAN.md#source-corrections) |
| [orig-ds-l1126](MS-SKILLS-PLAN.md#orig-ds-l1126) | batches | [增补](R66-CAPABILITY-WIRING-PLAN.md#batches) |
| [orig-rule-l0957](MS-SKILLS-PLAN.md#orig-rule-l0957) | action-wiring | [增补](R66-CAPABILITY-WIRING-PLAN.md#action-wiring) |
| [orig-rule-l0550](MS-MSITEM-PLAN.md#orig-rule-l0550) | action-wiring | [增补](R66-CAPABILITY-WIRING-PLAN.md#action-wiring) |
| [orig-rule-l0798](MS-MSITEM-PLAN.md#orig-rule-l0798) | action-wiring | [增补](R66-CAPABILITY-WIRING-PLAN.md#action-wiring) |
| [orig-non-l0145](MS-MSITEM-PLAN.md#orig-non-l0145) | action-wiring | [增补](R66-CAPABILITY-WIRING-PLAN.md#action-wiring) |
| [orig-ds-l1066](MS-MSITEM-PLAN.md#orig-ds-l1066) | action-wiring | [增补](R66-CAPABILITY-WIRING-PLAN.md#action-wiring) |


<a id="fix5-addendum-merge"></a>
## fix5：工作区增补完整读取与融合映射

输入为 `ronova-enhancement-addendum-handoff.zip` 中的三份 Markdown：V1.1、V1.2、MERGE-NOTE，分别为 **292 / 393 / 117 个物理行**，共 **39,404 字节**。交接表中的 202/281 行是原说明，不替代收到文件的实际计数。三份原始字节在[来源 zip](../archive/enhancement-addendum-handoff-r66.zip)及来源页原文块中保留；前置横幅负责说明当前效力，不改写原结论。

**融合方法：** 重复内容指向 fix4 既有章节；新事实直接充实[原生申请](R66-CAPABILITY-WIRING-PLAN.md#native-negotiation)、[首批实施](R66-CAPABILITY-WIRING-PLAN.md#batches)与[关键观察点](R66-CAPABILITY-WIRING-PLAN.md#validation)，不是把原包放进目录就宣称融合。

| 新输入内容 | 处置与现行落点 | 证据边界 |
|---|---|---|
| MERGE §3.1 / V1.2 §2.2：any_class 单项未回读 | [逐项清单与 B7](R66-CAPABILITY-WIRING-PLAN.md#native-any-class)；保留 B7 待做/待证 | 一个语句、两次拼写；显式收窄“无 VM 消费/对所有现路径无影响”的外推 |
| MERGE §3.2 / V1.2 §2.4：ThreadEnd 启用无诊断 | [事件与清理状态](R66-CAPABILITY-WIRING-PLAN.md#native-thread-end) | 正常 exit 已有；这里只登记退出兜底与可观测性缺口，不声称已复现泄漏 |
| MERGE §3.3 / V1.2 §2.1：potential 错误未检、无 granted 回读 | [现状表](R66-CAPABILITY-WIRING-PLAN.md#native-current-state)与[首批记录链](R66-CAPABILITY-WIRING-PLAN.md#native-record-contract) | 申请、回调、事件和功能结果不得混成两个 mask 的成功结论 |
| MERGE §3.4 / V1.2 附录：整组失败待确认 | [带来源的外部核对](R66-CAPABILITY-WIRING-PLAN.md#native-add-capabilities) | 只确认所查 HotSpot 错误路径；不猜用户现场发生过失败，不说已有能力被撤销 |
| V1.2 §2.3：局部变量缺失已有回退 | [保留认证捕获回退](R66-CAPABILITY-WIRING-PLAN.md#native-local-fallback) | 一并融入；不是新增全局权限或默认放行 |
| 五条 JVMTI 方向、C13/C11/服务器委派、批次主体 | 复用 fix4 的现有落点，详见下表 | 不重复计算工作量，不恢复 V1.1 已撤回判断 |

下表按**来源 + 原标题 + 原行范围**逐节登记；相同 B/C/D 编号不跨来源归一。它是人工融合处置，核对脚本只证明来源保留、分段连续和目标存在，**不证明语义关联完整或正确**。原 16 来源迁移表及 ID 清单不扩写，不创建新的 B/X 验收号。

| 来源 / 原行范围 | 原标题 | 当前落点 | 处置 |
|---|---|---|---|
| ADD11 · L1–L30 | [Ronova 加强方案 · 增补 V1.1](Ronova_Enhancement_Addendum_V1.1.md#ronova-加强方案--增补-v11) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#scope) | 已被取代，原文保留 |
| ADD11 · L31–L32 | [A. 撤回：`can_generate_native_method_bind_events` **存在**](Ronova_Enhancement_Addendum_V1.1.md#a-撤回can_generate_native_method_bind_events-存在) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-bind) | 已被取代，原文保留 |
| ADD11 · L33–L41 | [A.1 我此前的错误结论](Ronova_Enhancement_Addendum_V1.1.md#a1-我此前的错误结论) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-bind) | 已被取代，原文保留 |
| ADD11 · L42–L50 | [A.2 实测证据](Ronova_Enhancement_Addendum_V1.1.md#a2-实测证据) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-bind) | 已被取代，原文保留 |
| ADD11 · L51–L58 | [A.3 错在哪](Ronova_Enhancement_Addendum_V1.1.md#a3-错在哪) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-bind) | 已被取代，原文保留 |
| ADD11 · L59–L65 | [A.4 为什么这条撤回重要](Ronova_Enhancement_Addendum_V1.1.md#a4-为什么这条撤回重要) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-bind) | 已被取代，原文保留 |
| ADD11 · L66–L75 | [B. 五条新加强方向](Ronova_Enhancement_Addendum_V1.1.md#b-五条新加强方向) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#scope) | 已被取代，原文保留 |
| ADD11 · L76–L94 | [B.1 `NativeMethodBind` —— 补 `intercept.jni_native` 的观测缺口](Ronova_Enhancement_Addendum_V1.1.md#b1-nativemethodbind--补-interceptjni_native-的观测缺口) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-bind) | 已被取代，原文保留 |
| ADD11 · L95–L115 | [B.2 `DynamicCodeGenerated` —— 直接命中 `attack.dynamic_code`](Ronova_Enhancement_Addendum_V1.1.md#b2-dynamiccodegenerated--直接命中-attackdynamic_code) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#dynamic-code) | 已被取代，原文保留 |
| ADD11 · L116–L131 | [B.3 `ForceEarlyReturn*` —— 不依赖字节码所有权的定向提前返回](Ronova_Enhancement_Addendum_V1.1.md#b3-forceearlyreturn--不依赖字节码所有权的定向提前返回) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#early-return) | 已被取代，原文保留 |
| ADD11 · L132–L152 | [B.4 `SetNativeMethodPrefixes` —— 可能让 Render Firewall 不需要额外 DLL](Ronova_Enhancement_Addendum_V1.1.md#b4-setnativemethodprefixes--可能让-render-firewall-不需要额外-dll) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#prefix) | 已被取代，原文保留 |
| ADD11 · L153–L175 | [B.5 `GetConstantPool` + `GetBytecodes` → 常量池级归属证据](Ronova_Enhancement_Addendum_V1.1.md#b5-getconstantpool--getbytecodes--常量池级归属证据) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#structure-evidence) | 已被取代，原文保留 |
| ADD11 · L176–L177 | [C. `C13`（天花板管道）的准确表述 —— 修正我自己的判断](Ronova_Enhancement_Addendum_V1.1.md#c-c13天花板管道的准确表述--修正我自己的判断) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#capability-pipeline) | 已被取代，原文保留 |
| ADD11 · L178–L183 | [C.1 我先前的说法不准确](Ronova_Enhancement_Addendum_V1.1.md#c1-我先前的说法不准确) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#capability-pipeline) | 已被取代，原文保留 |
| ADD11 · L184–L226 | [C.2 实际机制（逐行核实）](Ronova_Enhancement_Addendum_V1.1.md#c2-实际机制逐行核实) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#capability-pipeline) | 已被取代，原文保留 |
| ADD11 · L227–L245 | [C.3 它是故意的 —— 代码里写着原因](Ronova_Enhancement_Addendum_V1.1.md#c3-它是故意的--代码里写着原因) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#capability-pipeline) | 已被取代，原文保留 |
| ADD11 · L246–L256 | [C.4 那真正的缺口是什么](Ronova_Enhancement_Addendum_V1.1.md#c4-那真正的缺口是什么) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#capability-pipeline) | 已被取代，原文保留 |
| ADD11 · L257–L272 | [C.5 所以 C13 的正确表述](Ronova_Enhancement_Addendum_V1.1.md#c5-所以-c13-的正确表述) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#capability-pipeline) | 已被取代，原文保留 |
| ADD11 · L273–L285 | [D. 建议顺序](Ronova_Enhancement_Addendum_V1.1.md#d-建议顺序) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#batches) | 已被取代，原文保留 |
| ADD11 · L286–L292 | [附：本增补不做什么](Ronova_Enhancement_Addendum_V1.1.md#附本增补不做什么) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#scope) | 已被取代，原文保留 |
| ADD12 · L1–L16 | [Ronova 加强方案 · 增补 V1.2](Ronova_Enhancement_Addendum_V1.2.md#ronova-加强方案--增补-v12) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#scope) | 来源/效力保留 |
| ADD12 · L17–L32 | [0. 本版修正摘要](Ronova_Enhancement_Addendum_V1.2.md#0-本版修正摘要) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#scope) | 来源/效力保留 |
| ADD12 · L33–L34 | [1. 逐条修正与复核](Ronova_Enhancement_Addendum_V1.2.md#1-逐条修正与复核) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#scope) | 来源/效力保留 |
| ADD12 · L35–L59 | [1.1 `SetNativeMethodPrefixes` 不是"按 classloader"](Ronova_Enhancement_Addendum_V1.2.md#11-setnativemethodprefixes-不是按-classloader) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#prefix) | 已覆盖，复用原章节 |
| ADD12 · L60–L87 | [1.2 `NativeMethodBind` 不只是观测](Ronova_Enhancement_Addendum_V1.2.md#12-nativemethodbind-不只是观测) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-bind) | 已覆盖，复用原章节 |
| ADD12 · L88–L107 | [1.3 `DynamicCodeGenerated` —— 我的覆盖范围说法被夸大](Ronova_Enhancement_Addendum_V1.2.md#13-dynamiccodegenerated--我的覆盖范围说法被夸大) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#dynamic-code) | 已覆盖，复用原章节 |
| ADD12 · L108–L140 | [1.4 「48 个键无生产者」过宽 —— 局部通道存在且是故意的](Ronova_Enhancement_Addendum_V1.2.md#14-48-个键无生产者过宽--局部通道存在且是故意的) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#source-corrections) | 已覆盖，复用原章节 |
| ADD12 · L141–L170 | [1.5 「14 个动作只接线 1 个」来自硬编码文案](Ronova_Enhancement_Addendum_V1.2.md#15-14-个动作只接线-1-个来自硬编码文案) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#action-wiring) | 已覆盖，复用原章节 |
| ADD12 · L171–L202 | [1.6 `TerminalLifecycle` 服务器分支：我把"守卫"读成了"不可达"](Ronova_Enhancement_Addendum_V1.2.md#16-terminallifecycle-服务器分支我把守卫读成了不可达) | [当前章节](../reference/R66-CAPABILITY-AUDIT.md#lifecycle) | 已覆盖，复用原章节 |
| ADD12 · L203–L204 | [2. 本轮新核实到的四项（双方此前都没提）](Ronova_Enhancement_Addendum_V1.2.md#2-本轮新核实到的四项双方此前都没提) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-negotiation) | 本轮融入 |
| ADD12 · L205–L247 | [2.1 能力申请链缺少回读 —— 四种失败原因在运行期不可区分](Ronova_Enhancement_Addendum_V1.2.md#21-能力申请链缺少回读--四种失败原因在运行期不可区分) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-add-capabilities) | 融入并显式限定推论 |
| ADD12 · L248–L255 | [2.2 `can_retransform_any_class` 只写不读](Ronova_Enhancement_Addendum_V1.2.md#22-can_retransform_any_class-只写不读) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-any-class) | 融入并显式限定推论 |
| ADD12 · L256–L279 | [2.3 `can_access_local_variables` 缺失已有设计好的回退](Ronova_Enhancement_Addendum_V1.2.md#23-can_access_local_variables-缺失已有设计好的回退) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-local-fallback) | 本轮融入 |
| ADD12 · L280–L297 | [2.4 `ThreadEnd` 事件启用返回值未检](Ronova_Enhancement_Addendum_V1.2.md#24-threadend-事件启用返回值未检) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-thread-end) | 本轮融入 |
| ADD12 · L298–L299 | [3. 修正后的 C13](Ronova_Enhancement_Addendum_V1.2.md#3-修正后的-c13) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#capability-pipeline) | 已覆盖，复用原章节 |
| ADD12 · L300–L311 | [3.1 瓶颈的正确描述：三层没有贯通](Ronova_Enhancement_Addendum_V1.2.md#31-瓶颈的正确描述三层没有贯通) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#capability-pipeline) | 已覆盖，复用原章节 |
| ADD12 · L312–L316 | [3.2 第一批交付目标](Ronova_Enhancement_Addendum_V1.2.md#32-第一批交付目标) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#capability-pipeline) | 已覆盖，复用原章节 |
| ADD12 · L317–L331 | [3.3 成功标准（采用外部核对提出的版本，比我原来的好）](Ronova_Enhancement_Addendum_V1.2.md#33-成功标准采用外部核对提出的版本比我原来的好) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#capability-pipeline) | 已覆盖，复用原章节 |
| ADD12 · L332–L360 | [4. 五条方向的修正后定位](Ronova_Enhancement_Addendum_V1.2.md#4-五条方向的修正后定位) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#jvmti-directions) | 已覆盖，复用原章节 |
| ADD12 · L361–L377 | [5. 修正后的实施顺序](Ronova_Enhancement_Addendum_V1.2.md#5-修正后的实施顺序) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#batches) | 首批范围补充 |
| ADD12 · L378–L387 | [6. 本增补不做什么](Ronova_Enhancement_Addendum_V1.2.md#6-本增补不做什么) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#scope) | 来源/效力保留 |
| ADD12 · L388–L393 | [附：本版未解决的两项](Ronova_Enhancement_Addendum_V1.2.md#附本版未解决的两项) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-add-capabilities) | 外部核对，未现场验证 |
| MERGE · L1–L4 | [融合说明：Ronova 加强方案增补 V1.1 / V1.2](../reference/Ronova_Enhancement_Addendum_MERGE-NOTE.md#融合说明ronova-加强方案增补-v11--v12) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#scope) | 交接处置已登记 |
| MERGE · L5–L18 | [1. 这两份是什么、为什么外部流程没见过](../reference/Ronova_Enhancement_Addendum_MERGE-NOTE.md#1-这两份是什么为什么外部流程没见过) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#scope) | 交接处置已登记 |
| MERGE · L19–L35 | [2. 与现有规划的重复度：约 95% 已被覆盖](../reference/Ronova_Enhancement_Addendum_MERGE-NOTE.md#2-与现有规划的重复度约-95-已被覆盖) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#jvmti-directions) | 已覆盖，复用原章节 |
| MERGE · L36–L37 | [3. 需要融合的内容：四条（唯一未被覆盖的）](../reference/Ronova_Enhancement_Addendum_MERGE-NOTE.md#3-需要融合的内容四条唯一未被覆盖的) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-negotiation) | 本轮融入 |
| MERGE · L38–L53 | [3.1 `can_retransform_any_class` **只写不读** —— 全部出现只有一处](../reference/Ronova_Enhancement_Addendum_MERGE-NOTE.md#31-can_retransform_any_class-只写不读--全部出现只有一处) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-any-class) | 融入并显式限定推论 |
| MERGE · L54–L71 | [3.2 `ThreadEnd` 事件启用返回值未检，也无对应 mask 位](../reference/Ronova_Enhancement_Addendum_MERGE-NOTE.md#32-threadend-事件启用返回值未检也无对应-mask-位) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-thread-end) | 本轮融入 |
| MERGE · L72–L90 | [3.3 `GetPotentialCapabilities` 返回值未检 + 无 `GetCapabilities()` 回读](../reference/Ronova_Enhancement_Addendum_MERGE-NOTE.md#33-getpotentialcapabilities-返回值未检--无-getcapabilities-回读) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-current-state) | 本轮融入 |
| MERGE · L91–L99 | [3.4 待确认项：`AddCapabilities` 是否整体失败语义](../reference/Ronova_Enhancement_Addendum_MERGE-NOTE.md#34-待确认项addcapabilities-是否整体失败语义) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#native-add-capabilities) | 融入并显式限定推论 |
| MERGE · L100–L109 | [4. 建议处置](../reference/Ronova_Enhancement_Addendum_MERGE-NOTE.md#4-建议处置) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#scope) | 交接处置已登记 |
| MERGE · L110–L117 | [5. 边界与证据口径（不变）](../reference/Ronova_Enhancement_Addendum_MERGE-NOTE.md#5-边界与证据口径不变) | [当前章节](R66-CAPABILITY-WIRING-PLAN.md#scope) | 交接处置已登记 |

<a id="fix6-v13-merge"></a>
## fix6 · V1.3 发出稿逐节融合映射

本表单独记录Ronova侧发出稿，不更改旧16来源映射或V1.1/V1.2处置。新输入2文件、41,319字节、699物理行均保留；原方向①–⑨是来源标签，不新增不变量编号。接受①⑦⑨、修改②③④⑤⑥、废弃⑧本轮任务；③调度仍须成本证据，不默认为必做。主实施顺序合并到[现有批次](R66-CAPABILITY-WIRING-PLAN.md#batches)。

| 原来源/行范围 | 原章节 | 原文保留位置 | 当前融合/回执位置 | 处置 |
|---|---|---|---|---|
| ADD13 L1–L9 | Ronova Enhancement Addendum V1.3 — 九条新方向 | [来源](Ronova_Enhancement_Addendum_V1.3.md#ronova-enhancement-addendum-v13--九条新方向) | [当前](RESTRUCTURE-REVIEW.md#review-fix6-v13) | 来源语境/回执，不替换V1.2 |
| ADD13 L10–L11 | 0. 先说边界（请先读这一节，它约束下面每一条） | [来源](Ronova_Enhancement_Addendum_V1.3.md#0-先说边界请先读这一节它约束下面每一条) | [当前](README.md#scope-hard-limits) | 来源语境/回执，不替换V1.2 |
| ADD13 L12–L21 | 0.1 规则来源只有三层，强度不同 | [来源](Ronova_Enhancement_Addendum_V1.3.md#01-规则来源只有三层强度不同) | [当前](README.md#scope-hard-limits) | 来源语境/回执，不替换V1.2 |
| ADD13 L22–L28 | 0.2 因此，本文每条方向只需要过两道闸 | [来源](Ronova_Enhancement_Addendum_V1.3.md#02-因此本文每条方向只需要过两道闸) | [当前](RESTRUCTURE-REVIEW.md#review-fix6-v13) | 来源语境/回执，不替换V1.2 |
| ADD13 L29–L48 | 0.3 本文的证据纪律 | [来源](Ronova_Enhancement_Addendum_V1.3.md#03-本文的证据纪律) | [当前](../reference/R66-V13-REVIEW-EVIDENCE.md) | 来源语境/回执，不替换V1.2 |
| ADD13 L49–L68 | 1. 为什么是这九条 | [来源](Ronova_Enhancement_Addendum_V1.3.md#1-为什么是这九条) | [当前](RESTRUCTURE-REVIEW.md#review-fix6-v13) | 来源语境/回执，不替换V1.2 |
| ADD13 L69–L74 | 2. 九条方向 | [来源](Ronova_Enhancement_Addendum_V1.3.md#2-九条方向) | [当前](R66-CAPABILITY-WIRING-PLAN.md#batches) | 来源语境/回执，不替换V1.2 |
| ADD13 L75–L122 | 方向 ①｜观测成本模型（**前置项**，建议最先做） | [来源](Ronova_Enhancement_Addendum_V1.3.md#方向-①观测成本模型前置项建议最先做) | [当前](R66-CAPABILITY-WIRING-PLAN.md#v13-cost-model) | 接受：保留六类，按(surface,source)加子账、每源过载动作与父总闸；不伪造外部事件频率上界。 |
| ADD13 L123–L180 | 方向 ②｜对象生命期账本（用 `can_tag_objects` + `OBJECT_FREE` 取代 6 处 `WeakReference`） | [来源](Ronova_Enhancement_Addendum_V1.3.md#方向-②对象生命期账本用-can_tag_objects--object_free-取代-6-处-weakreference) | [当前](R66-CAPABILITY-WIRING-PLAN.md#v13-object-lifecycle) | 修改：ObjectFree只补回收观察；不能发terminal判决或替掉活对象弱引用；组合能力与队列不足则回到原路线。 |
| ADD13 L181–L226 | 方向 ③｜JIT 时机感知的重转换调度（`CompiledMethodLoad`） | [来源](Ronova_Enhancement_Addendum_V1.3.md#方向-③jit-时机感知的重转换调度compiledmethodload) | [当前](R66-CAPABILITY-WIRING-PLAN.md#v13-jit-scheduling) | 修改：只先测现有重转换；调度实现不进入必做，收益数据不成立即废弃。 |
| ADD13 L227–L278 | 方向 ④｜`ATTACK_TARGET_CLASS_LINEAGE` 的执行器 | [来源](Ronova_Enhancement_Addendum_V1.3.md#方向-④attack_target_class_lineage-的执行器) | [当前](R66-CAPABILITY-WIRING-PLAN.md#v13-lineage) | 修改：共享只读解析与证据，但攻击/拦截各自授权/消费；补真实执行闭环，不据命中数推断全部间接路径。 |
| ADD13 L279–L321 | 方向 ⑤｜证书到期闸 + 依赖集哈希（落在 `D75` 上） | [来源](Ronova_Enhancement_Addendum_V1.3.md#方向-⑤证书到期闸--依赖集哈希落在-d75-上) | [当前](R66-CAPABILITY-WIRING-PLAN.md#v13-certificate) | 修改：现有CompletionTracker已有依赖版本和超时失效；扩展枚举依赖/原因与原子使用边界，而非从零造Authority。 |
| ADD13 L322–L363 | 方向 ⑥｜结算归属断言 | [来源](Ronova_Enhancement_Addendum_V1.3.md#方向-⑥结算归属断言) | [当前](R66-CAPABILITY-WIRING-PLAN.md#v13-settlement) | 修改：现有owner/lease/claim守卫存在；补结算端口覆盖、并发与恢复语义，不另起所有者。 |
| ADD13 L364–L407 | 方向 ⑦｜跨表面预算分配器（落在 `D95` 上） | [来源](Ronova_Enhancement_Addendum_V1.3.md#方向-⑦跨表面预算分配器落在-d95-上) | [当前](R66-CAPABILITY-WIRING-PLAN.md#v13-cross-surface-budget) | 接受：Root计划统一切片和父子预算，不重复扣减，不新造批准者。 |
| ADD13 L408–L454 | 方向 ⑧｜`LineNumberTable` 作为定向改写信号（**假设，未验证**） | [来源](Ronova_Enhancement_Addendum_V1.3.md#方向-⑧linenumbertable-作为定向改写信号假设未验证) | [当前](R66-CAPABILITY-WIRING-PLAN.md#v13-line-number-research) | 废弃：没有独立样本/增量收益依据，本轮不建实现或默认研究任务；不是宣称已测无效。 |
| ADD13 L455–L491 | 方向 ⑨｜把 `L6-INV-10` 的"同权限对手可绕过"写进能力报告 | [来源](Ronova_Enhancement_Addendum_V1.3.md#方向-⑨把-l6-inv-10-的同权限对手可绕过写进能力报告) | [当前](R66-CAPABILITY-WIRING-PLAN.md#v13-bypass-report) | 接受：报告分析状态、条件集合、范围/版本和依据；保持L6-INV-10准确含义，不等同所有同权限对手必然绕过。 |
| ADD13 L492–L520 | 3. 依赖顺序与批次 | [来源](Ronova_Enhancement_Addendum_V1.3.md#3-依赖顺序与批次) | [当前](R66-CAPABILITY-WIRING-PLAN.md#batches) | 来源语境/回执，不替换V1.2 |
| ADD13 L521–L530 | 4. 我不要求你做的事（边界澄清） | [来源](Ronova_Enhancement_Addendum_V1.3.md#4-我不要求你做的事边界澄清) | [当前](README.md#scope-hard-limits) | 来源语境/回执，不替换V1.2 |
| ADD13 L531–L550 | 5. 防复活索引：V1.1/V1.2 中已撤销或需修正的条目 | [来源](Ronova_Enhancement_Addendum_V1.3.md#5-防复活索引v11v12-中已撤销或需修正的条目) | [当前](R66-CAPABILITY-WIRING-PLAN.md#source-corrections) | 来源语境/回执，不替换V1.2 |
| ADD13 L551–L566 | 6. 相关文档位置 | [来源](Ronova_Enhancement_Addendum_V1.3.md#6-相关文档位置) | [当前](RESTRUCTURE-REVIEW.md#review-fix6-v13) | 来源语境/回执，不替换V1.2 |
| HAND13 L1–L8 | 交接说明：Ronova Enhancement Addendum V1.3 | [来源](../reference/Ronova_Enhancement_Addendum_V1.3_HANDOFF.md#交接说明ronova-enhancement-addendum-v13) | [当前](RESTRUCTURE-REVIEW.md#review-fix6-v13) | 来源语境/回执，不替换V1.2 |
| HAND13 L9–L26 | 1. 这是什么 | [来源](../reference/Ronova_Enhancement_Addendum_V1.3_HANDOFF.md#1-这是什么) | [当前](RESTRUCTURE-REVIEW.md#review-fix6-v13) | 来源语境/回执，不替换V1.2 |
| HAND13 L27–L42 | 2. 九条一览 | [来源](../reference/Ronova_Enhancement_Addendum_V1.3_HANDOFF.md#2-九条一览) | [当前](R66-CAPABILITY-WIRING-PLAN.md#batches) | 来源语境/回执，不替换V1.2 |
| HAND13 L43–L62 | 3. 规则边界（请先接受这一条，否则下面每条都会被误判） | [来源](../reference/Ronova_Enhancement_Addendum_V1.3_HANDOFF.md#3-规则边界请先接受这一条否则下面每条都会被误判) | [当前](README.md#scope-hard-limits) | 来源语境/回执，不替换V1.2 |
| HAND13 L63–L109 | 4. 证据口径 | [来源](../reference/Ronova_Enhancement_Addendum_V1.3_HANDOFF.md#4-证据口径) | [当前](../reference/R66-V13-REVIEW-EVIDENCE.md) | 来源语境/回执，不替换V1.2 |
| HAND13 L110–L120 | 5. 我想要的回执 | [来源](../reference/Ronova_Enhancement_Addendum_V1.3_HANDOFF.md#5-我想要的回执) | [当前](RESTRUCTURE-REVIEW.md#review-fix6-v13) | 来源语境/回执，不替换V1.2 |
| HAND13 L121–L130 | 6. 我不要求你做的事 | [来源](../reference/Ronova_Enhancement_Addendum_V1.3_HANDOFF.md#6-我不要求你做的事) | [当前](RESTRUCTURE-REVIEW.md#review-fix6-v13) | 来源语境/回执，不替换V1.2 |
| HAND13 L131–L133 | 7. 不会改变的东西 | [来源](../reference/Ronova_Enhancement_Addendum_V1.3_HANDOFF.md#7-不会改变的东西) | [当前](RESTRUCTURE-REVIEW.md#review-fix6-v13) | 来源语境/回执，不替换V1.2 |

**仍未绑定到B/X：** 新观察点需在真实实现后由人审关联原用例，不凭关键词强配，不增删原台账。MS归属仍为技能书运行时/预算/完成、物品书动作/结算、实体防御书身份/生命期；不适用的是把⑧直接作为实现要求、把GC当终结、把两键合成一个授权以及把外部进程旧授权复活，不是删除原来源段落。

<a id="fix7-surface-merge"></a>
## fix7 · 同权限清点的逐节映射

输入1个Markdown，**18,288字节/322物理行/21分段**。以下只使用来源别名，不重定义原A/B/C/D或§7序号。[处置回执](RESTRUCTURE-REVIEW.md#review-fix7-surface)；[复核与区别](../reference/R66-SAME-PRIVILEGE-REVIEW-EVIDENCE.md#findings)。原稿不是新规划规范。

| 来源原标题 | 原行范围 | 原文保留位置 | 当前章节落点 |
|---|---:|---|---|
| R66 · 同权限干预面清点（含 mixin 层） | 1–8 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#r66--同权限干预面清点含-mixin-层) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#same-privilege-surface) |
| 0. 口径 | 9–25 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#0-口径) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#same-privilege-surface) |
| 1. 摘要 | 26–36 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#1-摘要) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#same-privilege-surface) |
| 2. A 块：mixin 层 | 37–38 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#2-a-块mixin-层) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#surface-mixin) |
| 2.1 现状（规模与注入类型） | 39–52 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#21-现状规模与注入类型) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#surface-mixin) |
| 2.2 非 mixin 的肘击向量（回答"mixin 会被什么肘"） | 53–65 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#22-非-mixin-的肘击向量回答mixin-会被什么肘) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#surface-mixin) |
| 2.3 现有防线及其边界 | 66–85 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#23-现有防线及其边界) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#surface-mixin) |
| 2.4 缺口：10 个 mixin 目标不在租约内 | 86–117 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#24-缺口10-个-mixin-目标不在租约内) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#surface-mixin) |
| 2.5 加载单点与自校验缺失 | 118–122 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#25-加载单点与自校验缺失) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#surface-mixin) |
| 2.6 残余边界（逻辑推理，未实测） | 123–130 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#26-残余边界逻辑推理未实测) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#surface-mixin) |
| 3. B 块：决定侧未封印 | 131–157 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#3-b-块决定侧未封印) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#surface-policy) |
| 附：天花板实证（供交叉核对） | 158–179 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#附天花板实证供交叉核对) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#surface-policy) |
| 附：一处绑定验证不一致 | 180–197 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#附一处绑定验证不一致) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#surface-binding) |
| 4. C 块：API 表面敞开 | 198–199 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#4-c-块api-表面敞开) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#surface-api) |
| 4.1 三个前提全部成立 | 200–211 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#41-三个前提全部成立) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#surface-api) |
| 4.2 入口 | 212–233 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#42-入口) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#surface-api) |
| 4.3 零鉴权的破坏性方法 | 234–247 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#43-零鉴权的破坏性方法) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#surface-api) |
| 5. D 块：落盘状态无密钥 | 248–281 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#5-d-块落盘状态无密钥) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#surface-durable) |
| 6. 共同的形状 | 282–296 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#6-共同的形状) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#same-privilege-surface) |
| 7. 加固方向（按可做性排序） | 297–313 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#7-加固方向按可做性排序) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#same-privilege-surface) |
| 8. 边界与口径 | 314–322 | [原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md#8-边界与口径) | [融入原工作流](R66-CAPABILITY-WIRING-PLAN.md#same-privilege-surface) |

这里的“融入”指明来源段落的当前解释位置，不代表把每个推论都当已证实事实。八条动作不建立新B/X项；原四块与共同形状/边界均保留，13组复核说明仅是审阅记录，不是不变量ID。新增缺口是具体覆盖与信任边界工作，未自动关闭任何原GAP。

<a id="fix8-verification-merge"></a>
## fix8 · 核验补遗、极限天花板与检查器缺陷来源映射

四文件共36,289字节、633物理行；43分段含两段标题前来源说明。前两份写于fix7之前，第三份报告fix7工具缺陷。所有原文保持输入字节，原稿中的强表述按当前回执收窄而不涂改原稿。

[处理回执](RESTRUCTURE-REVIEW.md#review-fix8-verification) · [融合规划](R66-CAPABILITY-WIRING-PLAN.md#verification-ceiling) · [来源与摘录JSON](restructure/enhancement-addendum-merge.json)

| 来源 | 原章节/行 | 原文保留 | 当前落点 |
|---|---|---|---|
| V13-CHECK | 来源前言 · 1–2 | [原文](../reference/v13-verification-ceiling/R66-V13-VERIFICATION-ADDENDUM.md) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-verification) |
| V13-CHECK | R66 · V1.3 核验补遗（对 fix6 回执的独立源码复核） · 3–11 | [原文](../reference/v13-verification-ceiling/R66-V13-VERIFICATION-ADDENDUM.md#r66--v13-核验补遗对-fix6-回执的独立源码复核) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-verification) |
| V13-CHECK | 0. 这份补遗想解决什么 · 12–27 | [原文](../reference/v13-verification-ceiling/R66-V13-VERIFICATION-ADDENDUM.md#0-这份补遗想解决什么) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-verification) |
| V13-CHECK | 1. 逐条核验 · 28–29 | [原文](../reference/v13-verification-ceiling/R66-V13-VERIFICATION-ADDENDUM.md#1-逐条核验) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-verification) |
| V13-CHECK | 1.1 现有预算机制 — 确认，且已在生产使用 · 30–47 | [原文](../reference/v13-verification-ceiling/R66-V13-VERIFICATION-ADDENDUM.md#11-现有预算机制--确认且已在生产使用) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#v13-cost-model) |
| V13-CHECK | 1.2 `TerminalOperationPlan` 四个成员 — 存在，但是**固定常量** · 48–73 | [原文](../reference/v13-verification-ceiling/R66-V13-VERIFICATION-ADDENDUM.md#12-terminaloperationplan-四个成员--存在但是固定常量) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#v13-cross-surface-budget) |
| V13-CHECK | 1.3 `CompletionTracker` — 确认，但"超时"不是"到期" · 74–112 | [原文](../reference/v13-verification-ceiling/R66-V13-VERIFICATION-ADDENDUM.md#13-completiontracker--确认但超时不是到期) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#v13-certificate) |
| V13-CHECK | 1.4 owner/lease/claim 守卫 — 存在；但"恰好一个结算归属者"**不存在** · 113–141 | [原文](../reference/v13-verification-ceiling/R66-V13-VERIFICATION-ADDENDUM.md#14-ownerleaseclaim-守卫--存在但恰好一个结算归属者不存在) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#v13-settlement) |
| V13-CHECK | 1.5 `DurableTerminalWatchSet` — 确认；**原稿 ② 的前提被推翻** · 142–186 | [原文](../reference/v13-verification-ceiling/R66-V13-VERIFICATION-ADDENDUM.md#15-durableterminalwatchset--确认原稿-②-的前提被推翻) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#v13-object-lifecycle) |
| V13-CHECK | 2. 三条请回给研究方的精度修正 · 187–201 | [原文](../reference/v13-verification-ceiling/R66-V13-VERIFICATION-ADDENDUM.md#2-三条请回给研究方的精度修正) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-verification) |
| V13-CHECK | 3. 核验后九条的状态 · 202–219 | [原文](../reference/v13-verification-ceiling/R66-V13-VERIFICATION-ADDENDUM.md#3-核验后九条的状态) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-verification) |
| V13-CHECK | 4. 边界与口径 · 220–226 | [原文](../reference/v13-verification-ceiling/R66-V13-VERIFICATION-ADDENDUM.md#4-边界与口径) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-verification) |
| EXTREME-CHECK | 来源前言 · 1–2 | [原文](../reference/v13-verification-ceiling/R66-EXTREME-CEILING-REVIEW.md) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#extreme-ceiling-baseline) |
| EXTREME-CHECK | R66 · 极限档天花板重审（基于 fix6 回执与核验补遗） · 3–11 | [原文](../reference/v13-verification-ceiling/R66-EXTREME-CEILING-REVIEW.md#r66--极限档天花板重审基于-fix6-回执与核验补遗) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#extreme-ceiling-baseline) |
| EXTREME-CHECK | 0. 为什么需要重审 · 12–21 | [原文](../reference/v13-verification-ceiling/R66-EXTREME-CEILING-REVIEW.md#0-为什么需要重审) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#extreme-ceiling-baseline) |
| EXTREME-CHECK | 1. 档位模型（实测） · 22–23 | [原文](../reference/v13-verification-ceiling/R66-EXTREME-CEILING-REVIEW.md#1-档位模型实测) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#extreme-ceiling-baseline) |
| EXTREME-CHECK | 1.1 两套词汇表，靠一个桥接方法相连 · 24–41 | [原文](../reference/v13-verification-ceiling/R66-EXTREME-CEILING-REVIEW.md#11-两套词汇表靠一个桥接方法相连) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#extreme-ceiling-baseline) |
| EXTREME-CHECK | 1.2 合成不是集合交，是按 rank 取小 · 42–68 | [原文](../reference/v13-verification-ceiling/R66-EXTREME-CEILING-REVIEW.md#12-合成不是集合交是按-rank-取小) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#extreme-ceiling-baseline) |
| EXTREME-CHECK | 2. 极限档请求什么 · 69–94 | [原文](../reference/v13-verification-ceiling/R66-EXTREME-CEILING-REVIEW.md#2-极限档请求什么) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#extreme-ceiling-baseline) |
| EXTREME-CHECK | 3. 天花板实际在哪（决定性） · 95–128 | [原文](../reference/v13-verification-ceiling/R66-EXTREME-CEILING-REVIEW.md#3-天花板实际在哪决定性) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#extreme-ceiling-baseline) |
| EXTREME-CHECK | 4. 三条路径的实际行为（核心发现） · 129–132 | [原文](../reference/v13-verification-ceiling/R66-EXTREME-CEILING-REVIEW.md#4-三条路径的实际行为核心发现) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#extreme-ceiling-baseline) |
| EXTREME-CHECK | 路径 A — 通用 `resolve(policy, step)`（`:48-59`） · 133–146 | [原文](../reference/v13-verification-ceiling/R66-EXTREME-CEILING-REVIEW.md#路径-a--通用-resolvepolicy-step48-59) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#extreme-ceiling-baseline) |
| EXTREME-CHECK | 路径 B — 四个 `registered*` 本地入口（`:75` / `:94` / `:107` / `:110`） · 147–160 | [原文](../reference/v13-verification-ceiling/R66-EXTREME-CEILING-REVIEW.md#路径-b--四个-registered-本地入口75--94--107--110) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#extreme-ceiling-baseline) |
| EXTREME-CHECK | 断裂的形状 · 161–174 | [原文](../reference/v13-verification-ceiling/R66-EXTREME-CEILING-REVIEW.md#断裂的形状) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#extreme-ceiling-baseline) |
| EXTREME-CHECK | 5. 加强的真实目标 · 175–191 | [原文](../reference/v13-verification-ceiling/R66-EXTREME-CEILING-REVIEW.md#5-加强的真实目标) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#extreme-ceiling-baseline) |
| EXTREME-CHECK | 6. 对前九条的重新定位 · 192–209 | [原文](../reference/v13-verification-ceiling/R66-EXTREME-CEILING-REVIEW.md#6-对前九条的重新定位) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#extreme-ceiling-baseline) |
| EXTREME-CHECK | 7. 边界与口径 · 210–217 | [原文](../reference/v13-verification-ceiling/R66-EXTREME-CEILING-REVIEW.md#7-边界与口径) | [采用范围](R66-CAPABILITY-WIRING-PLAN.md#extreme-ceiling-baseline) |
| PORTABILITY | R66 · `verify_addendum_merge.py` 的 Windows 可移植性缺陷 · 1–9 | [原文](../reference/v13-verification-ceiling/R66-MERGE-VERIFIER-PORTABILITY.md#r66--verify_addendum_mergepy-的-windows-可移植性缺陷) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-portability) |
| PORTABILITY | 1. 结论 · 10–19 | [原文](../reference/v13-verification-ceiling/R66-MERGE-VERIFIER-PORTABILITY.md#1-结论) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-portability) |
| PORTABILITY | 2. 缺陷位置 · 20–43 | [原文](../reference/v13-verification-ceiling/R66-MERGE-VERIFIER-PORTABILITY.md#2-缺陷位置) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-portability) |
| PORTABILITY | 3. 证据链（三条独立确认） · 44–45 | [原文](../reference/v13-verification-ceiling/R66-MERGE-VERIFIER-PORTABILITY.md#3-证据链三条独立确认) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-portability) |
| PORTABILITY | 3.1 路径规范化后两项自动匹配 · 46–56 | [原文](../reference/v13-verification-ceiling/R66-MERGE-VERIFIER-PORTABILITY.md#31-路径规范化后两项自动匹配) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-portability) |
| PORTABILITY | 3.2 `policy_classes` 的 0 vs 28 · 57–67 | [原文](../reference/v13-verification-ceiling/R66-MERGE-VERIFIER-PORTABILITY.md#32-policy_classes-的-0-vs-28) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-portability) |
| PORTABILITY | 3.3 与记录值精确吻合 · 68–78 | [原文](../reference/v13-verification-ceiling/R66-MERGE-VERIFIER-PORTABILITY.md#33-与记录值精确吻合) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-portability) |
| PORTABILITY | 4. 复现 · 79–97 | [原文](../reference/v13-verification-ceiling/R66-MERGE-VERIFIER-PORTABILITY.md#4-复现) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-portability) |
| PORTABILITY | 5. 附：基准选择（我本轮犯过的错，一并记录） · 98–110 | [原文](../reference/v13-verification-ceiling/R66-MERGE-VERIFIER-PORTABILITY.md#5-附基准选择我本轮犯过的错一并记录) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-portability) |
| PORTABILITY | 6. 修法 · 111–130 | [原文](../reference/v13-verification-ceiling/R66-MERGE-VERIFIER-PORTABILITY.md#6-修法) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-portability) |
| PORTABILITY | 7. 边界 · 131–137 | [原文](../reference/v13-verification-ceiling/R66-MERGE-VERIFIER-PORTABILITY.md#7-边界) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-portability) |
| V13-CHECK-HANDOFF | 交接索引 · Ronova 侧 → 外部研究方 · 1–8 | [原文](../reference/v13-verification-ceiling/README-INDEX.md#交接索引--ronova-侧--外部研究方) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-verification) |
| V13-CHECK-HANDOFF | 包内文件（字节数为实测值） · 9–18 | [原文](../reference/v13-verification-ceiling/README-INDEX.md#包内文件字节数为实测值) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-verification) |
| V13-CHECK-HANDOFF | 各自一句话 · 19–38 | [原文](../reference/v13-verification-ceiling/README-INDEX.md#各自一句话) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-verification) |
| V13-CHECK-HANDOFF | 与 fix7 的关系 · 39–48 | [原文](../reference/v13-verification-ceiling/README-INDEX.md#与-fix7-的关系) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-verification) |
| V13-CHECK-HANDOFF | 边界 · 49–53 | [原文](../reference/v13-verification-ceiling/README-INDEX.md#边界) | [采用范围](RESTRUCTURE-REVIEW.md#review-fix8-verification) |

本轮补足的是精度、消费侧事实和工具缺陷；不新造GAP编号、不将任何人工关联自动改为已覆盖。“第10项”并入DS:C13/⑨；A观察消费不能被计数误读成死执行路径。


<a id="fix9-windows-rerun"></a>
## fix9 · Windows 原生复跑回执归属

本输入只回答 fix8 的现场待确认项，**不进入三书正文迁移、不新增不变量/方向/批次，也不改变任何 B/X 状态**。原始 Markdown 与 ZIP 分别保留在 reference/archive；当前效力写入审阅与固定核对摘要。

| 输入 | 保留位置 | 当前解释位置 | 处置 |
|---|---|---|---|
| `R66-FIX8-WINDOWS-RERUN-RECEIPT.md` | [原文](../reference/R66-FIX8-WINDOWS-RERUN-RECEIPT.md) | [fix9 Windows闭合](RESTRUCTURE-REVIEW.md#review-fix9-windows) | 用户现场回执；闭合fix8文档工具Windows待确认项，不生成Mod/阶段PASS |
| `ronova-fix8-windows-rerun-receipt.zip` | [原ZIP](../archive/fix8-windows-rerun-receipt-r66.zip) | [记录入口](restructure/README.md#fix9--windows-原生复跑回执) | 原字节归档；不作为新的规划来源 |


<a id="fix10-eight-merge"></a>
## fix10 · 第二批八方向核验的逐节映射

输入是Ronova侧发出的只读核验，不是新提案；原[正文](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md)与[zip](../archive/second-batch-eight-directions-verification-r66.zip)完整保留。S1–S4/A1–A4只在该来源内使用，不并入原不变量编号空间。原核验结论、本轮独立源码补读、当前设计分别位于原文、[依据](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#findings)、[回执](RESTRUCTURE-REVIEW.md#review-fix10-eight)。

| 原章节（原行） | 原文保留 | 当前落点 |
|---|---|---|
| R66 · 第二批八方向核验（对源码现状的只读复核）（1–10） | [来源](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md#r66--第二批八方向核验对源码现状的只读复核) | [融合](RESTRUCTURE-REVIEW.md#review-fix10-eight) |
| 0. 结论摘要（11–29） | [来源](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md#0-结论摘要) | [融合](RESTRUCTURE-REVIEW.md#review-fix10-eight) |
| 1. 逐条核验（30–31） | [来源](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md#1-逐条核验) | [融合](RESTRUCTURE-REVIEW.md#review-fix10-eight) |
| S1 · 能力自证 / Challenge Probe —— 准确，原语齐备，缺统一层（32–62） | [来源](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md#s1--能力自证--challenge-probe--准确原语齐备缺统一层) | [融合](R66-CAPABILITY-WIRING-PLAN.md#eight-s1) |
| S2 · Mutation Write-Set 写集租约 —— 准确，是真缺口（63–78） | [来源](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md#s2--mutation-write-set-写集租约--准确是真缺口) | [融合](R66-CAPABILITY-WIRING-PLAN.md#eight-s2) |
| S3 · 独立结果见证器 —— 准确，而且比"存储链"更具体（79–107） | [来源](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md#s3--独立结果见证器--准确而且比存储链更具体) | [融合](R66-CAPABILITY-WIRING-PLAN.md#eight-s3) |
| S4 · 事件缺口传播 / Observation Gap Protocol —— 准确，是真缺口（108–125） | [来源](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md#s4--事件缺口传播--observation-gap-protocol--准确是真缺口) | [融合](R66-CAPABILITY-WIRING-PLAN.md#eight-s4) |
| A1 · 运行时证据依赖图 —— 准确，是真缺口（126–140） | [来源](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md#a1--运行时证据依赖图--准确是真缺口) | [融合](R66-CAPABILITY-WIRING-PLAN.md#eight-a1) |
| A2 · Native ABI / Feature Negotiation —— 基本准确，但低估现状（141–160） | [来源](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md#a2--native-abi--feature-negotiation--基本准确但低估现状) | [融合](R66-CAPABILITY-WIRING-PLAN.md#eight-a2) |
| A3 · 通用 Shadow Enforcement —— 缺口准确，但低估已有结果模型（161–186） | [来源](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md#a3--通用-shadow-enforcement--缺口准确但低估已有结果模型) | [融合](R66-CAPABILITY-WIRING-PLAN.md#eight-a3) |
| A4 · 分能力熔断 / Degraded Mode —— **低估最多**（187–213） | [来源](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md#a4--分能力熔断--degraded-mode--低估最多) | [融合](R66-CAPABILITY-WIRING-PLAN.md#eight-a4) |
| 2. 对优先级的意见（214–245） | [来源](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md#2-对优先级的意见) | [融合](R66-CAPABILITY-WIRING-PLAN.md#eight-order) |
| 3. 关于"不建议继续堆"的六项（246–255） | [来源](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md#3-关于不建议继续堆的六项) | [融合](RESTRUCTURE-REVIEW.md#review-fix10-eight) |
| 4. 边界与口径（256–263） | [来源](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md#4-边界与口径) | [融合](R66-CAPABILITY-WIRING-PLAN.md#scope) |

未在本轮关闭新的MS缺口或验收项。S4已有任务图局部失证，不将其归为从零；S3的观察位不是四级状态机；S2声明不独自证明执行；A4不按机械新增枚举估工。S1/S2/C13/S3/A4落第一批所选切片，S4/A1/A3按既有批次扩展，A2最小协议前置随桥接变化提前。上述是可审阅设计安排，不是机器语义证明。


<a id="fix11-baseline-merge"></a>
## fix11 · 八方向已有基线补遗逐节映射

[原始输入](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md)与[ZIP](../archive/eight-directions-existing-baseline-supplement-r66.zip)逐字节保留；19,795字节、263物理行、19个分段。补充已有实现，不新增方向/批次，fix10原决策保留；[采用边界](RESTRUCTURE-REVIEW.md#review-fix11-baseline)与[消费者核对](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#findings)不回写来源。

| 原章节（原行范围） | 原文保留 | 当前规划／说明落点 |
|---|---|---|
| R66 · 第二批八方向补遗：已有实现清单（1–10） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#r66--第二批八方向补遗已有实现清单) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-existing-baseline) |
| 0. 最重要的发现：一条成文的架构纪律（11–40） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#0-最重要的发现一条成文的架构纪律) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-existing-baseline) |
| 对八方向的直接含义（41–54） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#对八方向的直接含义) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-existing-baseline) |
| 0.5 第二项关键发现：**S2 与 S3 已有为 7.17 预留的接缝**（55–58） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#05-第二项关键发现s2-与-s3-已有为-717-预留的接缝) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-existing-baseline) |
| 写入侧（对应 S2）（59–69） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#写入侧对应-s2) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-existing-baseline) |
| 完成侧（对应 S3）（70–79） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#完成侧对应-s3) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-existing-baseline) |
| 这两组发现的含义（80–91） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#这两组发现的含义) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-existing-baseline) |
| 1. 逐方向的已有实现清单（92–93） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#1-逐方向的已有实现清单) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-existing-baseline) |
| S2 · Write-Set —— **已有"实际写入"的完整一侧，缺"声明"侧与比对**（94–115） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#s2--write-set--已有实际写入的完整一侧缺声明侧与比对) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-s2) |
| S3 · Independent Witness —— **已有"独立观察"的完整一侧，且全部声明"不是回执"**（116–134） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#s3--independent-witness--已有独立观察的完整一侧且全部声明不是回执) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-s3) |
| S4 · Observation Gap —— **已有连续性概念与失证传播，缺跨源序列/代际**（135–151） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#s4--observation-gap--已有连续性概念与失证传播缺跨源序列代际) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-s4) |
| A1 · 证据依赖图 —— **已有关系索引一族，缺可传播撤销的 DAG**（152–167） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#a1--证据依赖图--已有关系索引一族缺可传播撤销的-dag) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-a1) |
| S1 · Challenge Probe —— 原语齐备，载体引用需订正（168–184） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#s1--challenge-probe--原语齐备载体引用需订正) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-s1) |
| A2 · Native ABI —— 已有构建身份与 capability 位（185–197） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#a2--native-abi--已有构建身份与-capability-位) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-a2) |
| A3 · Generic Shadow —— 缺口准确，**结果模型比前稿描述更成熟**（198–208） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#a3--generic-shadow--缺口准确结果模型比前稿描述更成熟) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-a3) |
| A4 · 分能力熔断 —— 状态词汇已有，**但熔断机制确实没有**（209–237） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#a4--分能力熔断--状态词汇已有但熔断机制确实没有) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-a4) |
| 2. 给实施者的六条不要（238–246） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#2-给实施者的六条不要) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-existing-baseline) |
| 3. 给实施者的四条先读（247–255） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#3-给实施者的四条先读) | [落点](R66-CAPABILITY-WIRING-PLAN.md#eight-existing-baseline) |
| 4. 边界与口径（256–263） | [来源](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md#4-边界与口径) | [落点](RESTRUCTURE-REVIEW.md#review-fix11-baseline) |

**缺口／不适用的处理：** 本轮不创建新的GAP编号；“全写集实际侧已完整”“模块宿主覆盖全部54键”“类注释即7.17生产完成”“所有异常已有有限重试”等外推不适用，保留来源措辞并由回执限定。原MS明确不适用表与原缺口表不改。


<a id="fix12-acceptance-binding"></a>
## fix12 · 施工、观察判据与原台账的桥

本轮没有外部ZIP新增来源，没有原文迁移；输入为fix11原文与本轮用户评审的方案③，以下是显式项目决策。原16来源与全部后续handoff映射保持原样。

| 旧位置/问题 | 新位置与处理 | 没有改变的边界 |
|---|---|---|
| 接线规划的19行批次观察点 | [原表](R66-CAPABILITY-WIRING-PLAN.md#validation)原行文保持，增加描述性锚点并明确正式判据效力 | 未编新B/X，未生成19个测试或运行结果。 |
| 批次与八方向仅有施工顺序 | [批次](WIRING-ACCEPTANCE.md#batch-binding)与[方向绑定](WIRING-ACCEPTANCE.md#direction-binding)连接具体观察行 | 原批次/方向不改，绑定是可审阅的人工分工。 |
| 原68项索引缺反向入口 | [当前分工](VALIDATION-TRACEABILITY.md#acceptance-scope)经生成器生成 | 原68场景/120行人工关联/台账状态未改；受影响回归仍适用。 |
| 39键还只写逐键清点 | [施工准备与完成范围](WIRING-ACCEPTANCE.md#key-readiness)明确producer/consumer/outlet/结果/失效/判据的收敛要求 | 未重新审计或补造执行器，不缩减54键目标。 |
| 观察要求没有交付记录身份 | [证据规则](WIRING-ACCEPTANCE.md#evidence-rules)与[模板](WIRING-ACCEPTANCE.md#record-template)使用原锚点＋版本＋scope | 未建空台账，未回填GAP-MAP-07或原PASS。 |
