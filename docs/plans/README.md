# Ronova 规划入口 · MS 三书结构

**fix12 · 当前验收分工：** [新增接线正式口径](WIRING-ACCEPTANCE.md#decision) · [八方向/批次绑定](WIRING-ACCEPTANCE.md#direction-binding) · [原68项范围](VALIDATION-TRACEABILITY.md#acceptance-scope)。采用方案③，原19行判据不删不改，原台账不回填；旧轮记录仅对其各自版本有效。

**fix11当前修订：** [先复用具名接缝](R66-CAPABILITY-WIRING-PLAN.md#eight-existing-baseline) · [回执](RESTRUCTURE-REVIEW.md#review-fix11-baseline) · [逐节映射](RESTRUCTURE-MAP.md#fix11-baseline-merge)。写入/完成、复制索引与模块宿主补入已有基线；原fix10决策和批次、铁律/UX1及编号不变。

**fix10当前修订：** [八方向按职责融合](R66-CAPABILITY-WIRING-PLAN.md#eight-directions) · [八条回执与修正](RESTRUCTURE-REVIEW.md#review-fix10-eight) · [来源映射](RESTRUCTURE-MAP.md#fix10-eight-merge)。S3保留累积观察、A4复用双维状态；S4已有任务图失证被纳入基线。只改规划/核对，不新增Root、不重编号、不新增运行PASS。下列“当前”旧轮说明按各自fix号阅读。

**fix9历史修订：** [Windows 原生复跑闭合](RESTRUCTURE-REVIEW.md#review-fix9-windows) · [原始回执](../reference/R66-FIX8-WINDOWS-RERUN-RECEIPT.md)。只更新 fix8 文档核对工具的现场确认状态，不新建方向、批次、ID 或验收 PASS。

> **fix8修订：** [三处精度修正/极限基线](R66-CAPABILITY-WIRING-PLAN.md#verification-ceiling) · [回执](RESTRUCTURE-REVIEW.md#review-fix8-verification) · [核对器可移植性修复](RESTRUCTURE-REVIEW.md#review-fix8-portability)。不新建方向或批次；保留原九项决策与硬限制。

> **fix7 当前融合：** [同权限清点及八条加固](RESTRUCTURE-REVIEW.md#review-fix7-surface)接回[既有C13/C11/原生/持久化章节](R66-CAPABILITY-WIRING-PLAN.md#same-privilege-surface)；[原文映射](RESTRUCTURE-MAP.md#fix7-surface-merge)。来源判断和独立复核分开，未新增Root、编号、批次或PASS。

> **前轮fix6 V1.3融合：** [九项回执](RESTRUCTURE-REVIEW.md#review-fix6-v13) · [现有接线章节/批次](R66-CAPABILITY-WIRING-PLAN.md#v13-receipt) · [逐节映射](RESTRUCTURE-MAP.md#fix6-v13-merge)。V1.3是Ronova侧发出方向，不替换V1.2；不新建权威或不变量编号，不改源码/验收。

> **fix5 已融合记录：** [当前 r66 接线工作](R66-CAPABILITY-WIRING-PLAN.md#native-negotiation)继续沿用 fix4 顺序，补入逐项能力回读、ThreadEnd 清理与已有回退；[来源/重复/新增/推论收窄映射](RESTRUCTURE-MAP.md#fix5-addendum-merge)。三书框架与全部原文/ID 保留；代码和运行验收未修改。

更新：2026-09-14。输入基线：`ronova-docs-0.2.0-alpha.7.17-r66.zip`。

初轮是**规划结构重构**；fix4 增加点名的能力接线规划，fix5 将新收到的工作区增补融入其原生接线与首批工作，不再平行堆叠一份规范。没有改 Java、Native、资源、Gradle、依赖、Mod 或 Forge 文件；没有构建、启动游戏、执行功能测试或更新阶段验收结果。原源码包保持不变。

## 从哪里读

| 职责 | 主规划 | 骨架来源 |
|---|---|---|
| 能力、架构、线程、转换器、Native、阶段和验证 | [技能、架构与运行时](MS-SKILLS-PLAN.md) | `skills.zh.md` |
| 物品契约、输入、动作上下文、目标、结果、队列、同步与表现 | [击杀物品与动作执行](MS-MSITEM-PLAN.md) | `msitem.zh.md` |
| 规范实例、状态、服务端/客户端注册表、读侧、修复与防御验证 | [实体防御与存在一致性](MS-ENTITYDEFENCE-PLAN.md) | `entitydefence.zh.md` |

[不变量全集与全部编号索引](INVARIANT-INDEX.md) · [规划条款 ↔ B/X 验收规格](VALIDATION-TRACEABILITY.md) · [新旧章节映射、MS 缺口与不适用说明](RESTRUCTURE-MAP.md) · [审阅修订说明](RESTRUCTURE-REVIEW.md) · [文档完整性核对记录](RESTRUCTURE-CHECK.md) · [当前核对与历史记录索引](restructure/README.md) · [当前实现记录](../CURRENT-PROGRESS.md)。

三本书按 MS 原标题与顺序组织。Ronova 的 Root、身份、图、证书、54 项能力、14 个动作、8 类运行表面和所有阶段作为相应职责下的原条款展开，**不是改名后把整份旧稿塞进同一个附录**。

<a id="scope-hard-limits"></a>
## 当前不可越过的限制

| 限制 | 当前效力 |
|---|---|
| 不改依赖 / Mod / Forge 文件 | 不能以修改这些文件实现功能、优先级或对抗效果。 |
| 不针对 | 不能按第三方身份、私有类/字段/包名等编写特判，也不能用反射、MethodHandle、VarHandle 等绕过这一点。研究记录中的样本名称不因此成为运行时选择器。 |
| 不删对方文件 | 不以删除对方文件完成清除或压制。 |
| 不起外部进程 | 不启动外部程序、JVM、helper、attach/relaunch/recovery 子进程或常驻外部控制面。 |

这里按本次明确要求定界，不另加“无参数”“禁用 Agent/JVMTI/Native”“只能某一种后端”等永久禁令。项目自有配置、日志、状态持久化等原设计本轮未实施，也不因本轮界定改变其设计效力；其具体边界仍须据实际对象、文件类别与上述限制判断，不能把不删对方文件偷换成只不删 JAR。

<a id="scope-startup-parameters"></a>
### “无参数”的当前效力与前置能力选型

**“无参数”不是本次的硬约束，也不是额外的必选完成条件。** 四项限制仍只有上表所列内容。原稿将“不要求启动参数”保留为默认取向，见 [RULE 的 W5](MS-SKILLS-PLAN.md#orig-rule-l0247)；本轮不把它升级为铁律，也不反过来要求用户必须提供参数。

采用宿主原有启动流程提供参数、或采用进程内前置入口，属于可调整的方案选择；是否可用、实际启用了哪条路径，仍须由对应环境和实现证据说明。**允许参数不授权 Ronova 启动 helper、attach/relaunch 子进程，不授权修改受禁文件。**

[DS 加强 A 块](MS-SKILLS-PLAN.md#orig-ds-l1007)及 [RULE 加强 A 块的 A5](MS-SKILLS-PLAN.md#orig-rule-l0734)“程序化打开 Java 模块访问边界”、[RULE 的 I7](MS-SKILLS-PLAN.md#orig-rule-l0267)“进程内 attach 管道”，不再因为“全项目必须无参数”而被列成唯一必选手段。仅为满足无参数目标而设的必要性，按选中路线条件化；它们可以保留为无参数体验或所选前置链的实现选项。

**可选的是具体手段，不是所选功能实际依赖的权限或初始化条件。** 已选路线确实依赖 A5 或 I7 时，不能只改标签就删掉能力、取消替代接线或宣告前置链就绪。旧文“唯一前置入口”“A 块未就绪前不得执行 C15”是原路线的接续安排，不是禁止等价方案的新增铁律；也不授权为了等待替代方案继续启用外部进程。这里没有实施任何入口替换。

这里的 A5 只指对应来源的**加强 A 块**，不改变 [P0-A A5 — Root Writer Uniqueness](MS-SKILLS-PLAN.md#orig-p0a-l0152)。同名 ID 不能跨来源、跨章节混用。

<a id="scope-process"></a>
### 原进程条款如何处理

原文仍可见 `D13`、`PROC-INV-1`、`R7`、`A8`、`ID-9`、`T-16` 及相关测试中关于 bounded helper 的允许语句。**这些允许语句仅作为保真历史文本，不再授权起任何外部进程。**

同一段中的内部 Root、writer、durable store、身份、原子性与恢复语义并没有因此删除。重启恢复可以描述用户或现有宿主随后重新启动 Minecraft 时的状态恢复，不能据此由 Ronova 拉起新进程。进程内线程、JNI/JVMTI 与同 JVM 的转换/桥接能力不因“不起外部进程”一概排除。

涉及该边界的迁入段落有就地提示，逐段清单在 `restructure/scope-adjustments.json`；原句和原 ID 保持不变。

<a id="scope-governance"></a>
## 当前文档效力：按职责组织，不按旧版位次冻结

旧的“唯一规范顺序”“较高位冻结决策不可覆盖”“派生导读不能修改上游”等规则**不再具有不可修改的优先权**。它们也被迁移保留，但只说明原文当时的组织关系，不能否决本次结构或以后显式的项目设计修订。

三本新主规划是职责入口，不是新的不可更改排名；MS 三份资料提供骨架、术语和参考要求，不自动成为凌驾用户要求的额外铁律。

**内容保留与规则永远不可改是两回事。** 本轮没有以“可改”为理由删除 D*、INV*、A*、R*、ID-*、T-*、类型、依赖或验收条款。除本页明确的进程授权、文档治理效力、启动参数选型，以及 [fix4 点名的源码/JVMTI 判断更正](R66-CAPABILITY-WIRING-PLAN.md#affected-originals)外，技术条款逐段沿用原文字与原来源。更正不改原文块和原 ID，不替换实现，也不伪造验收结果。

今后修订可以明确改变旧设计，不必等待某份上游永久冻结文档先开放；同时应记录受影响原 ID 和前后关系，避免把某次设计选择伪写成曾经实现过的事实。这个追溯方法是工作约定，不是新增的不可修改禁令。

<a id="scope-p-layer"></a>
### DS 的 P 层与 C 块如何定级

原 [DS §15 的 P 层](MS-SKILLS-PLAN.md#orig-ds-l0932)、[§16.2 的对内判断](MS-SKILLS-PLAN.md#orig-ds-l0962)、[C 块](MS-SKILLS-PLAN.md#orig-ds-l1040)与[§18 实施顺序](MS-SKILLS-PLAN.md#orig-ds-l1126)中的“自己冻结的规范”，**现在解释为可显式修订的项目设计与验收选择，不是第五条铁律，也不能凭旧上游位次否决修订。** [BL 原冻结结论](MS-SKILLS-PLAN.md#orig-bl-l2927)与 [CF 原 Frozen Architecture Position](MS-SKILLS-PLAN.md#orig-cf-l0056)不恢复这种否决权。

就 **DS 的 C 块**而言，`C1 / C11 / C12 / C13 / C14 / C16 / C19` 的 P 层标签仅说明原设计对齐来源。不能仅因“`D91` 已冻结”把不同选型判成铁律违规或禁止讨论；也不能将解除冻结解释成七项全部删掉、代码问题自动修好，或无条件放行其中触及四项限制的行为。

**DS 的 C12（逐键矩阵 revision 2）和 C13（天花板管道）继续保留在当前规划中。** 它们与 FULL L5 / V2.5 的完成条件都是可修订的项目选择；本轮没有取消两项、替换完成条件或宣布达标。继续沿用旧完成口径时，仍须按该口径核对；改口径时要明确记下受影响 ID、替代条件及旧证据的适用范围，不能拿旧证据套用到新结论。

以上只对 DS 的对应条目定级，不将 [RULE 旧 C 块](MS-SKILLS-PLAN.md#orig-rule-l0767)的同号项混为一谈；例如 RULE 的 C12/C13 与 DS 的 C12/C13 不是同一组内容。原表、原编号与历史判断全部保留，具体对照见[本轮审阅说明](RESTRUCTURE-REVIEW.md#review-p4)。

<a id="scope-classification"></a>
### 旧红线、示例与历史判断的分层

旧稿的 W*、V*、L0/L1/P、S*、N*、C*、R-* 表格以及“已取代/已并入”说明全部保留。只有符合本次三条铁律与不起外部进程要求的内容具有相应硬边界效力；其他属于可修订的项目约定、适用前提、建议或历史裁决。

这不意味着一切旧约束自动取消，更不意味着可以静默删掉不变量。也不恢复那些已被后续原稿纠正的判断。例如“终结目标过滤 = 全局独占”在原稿里已有修正，两段同时保留并说明出处。

## 原文、引用与 ID 的阅读方式

每个迁入段落有原文件缩写、原始行范围、完整原标题路径、显式稳定锚点和映射链接。正文的 `本文`、`本节`、`§X`、`第 X 节` 仍属于**其原来源语境**，不是三本新书的重编号。

旧标题正文、条款、表格、代码/文本示例、ID 和历史状态保留；只有 Markdown 标题层级及引用链接进行了可逆调整。同一个 `D1` 或 `R1` 出现在不同旧文件中仍各自归属原文件，不跨来源合并，不重编号。

引用到已迁移原文时，使用新书显式锚点；跨多个段落的旧行号引用跳到映射文件中的范围索引。旧文件名保留为导航页，旧标题锚点也保留兼容跳转。未能唯一定位的原节号明列为未决，不编造章节或补写来源。

## 范围与完整性

初轮输入的 `docs/plans/` **15 份 Markdown** 全部纳入。另将原索引指向的 `docs/DESIGN-SPEC.md` **完整迁入**，以免遗漏已集中在合并导读中的加强内容；它的原路径改为导航页。共 **16 份原文来源**。

`.incoming-ms-mod-doc/` 三份 zh.md 原文与许可证不变。`.incoming-all-stages-v1.1/` 是输入包中的另一套待采纳快照，不是当前根 `docs/plans/`；它没有被混入本次有效规划，目录和文件全部保留。`archive/`、历史进度、历史实施记录、验证证据和当前进度不重新裁决。

逐文件原始字节快照位于 `../archive/plans-before-ms-restructure-r66/`。迁移不是只做备份：各原段落都已进入三本书，备份用于逐字节回溯核对。源清单、分段映射、原 ID 出现次数、引用变更和未决记录在 `restructure/`。


<a id="addendum-sources"></a>
### 工作区增补的当前效力

[增补 V1.2](Ronova_Enhancement_Addendum_V1.2.md)已融合到[现有接线规划](R66-CAPABILITY-WIRING-PLAN.md#native-negotiation)，不作为第二套优先级或新增完成条件；[V1.1](Ronova_Enhancement_Addendum_V1.1.md)继续为已被取代的对照稿。[交接说明](../reference/Ronova_Enhancement_Addendum_MERGE-NOTE.md)保留其来源性质，处置结果见[融合映射](RESTRUCTURE-MAP.md#fix5-addendum-merge)。三份原文在来源页完整保留，顶部横幅明确区分当前处置和历史说法；收到的 zip 另有[原字节归档](../archive/enhancement-addendum-handoff-r66.zip)。

新增输入引用的 D/B/C 等 ID 沿用各自来源语境，不是新定义；原 16 来源的不变量索引及 68 项 B/X 的人工关系表均不因这些重复引用扩编。`can_retransform_any_class` 的“只写不读/必然无影响”推论已显式收窄，`AddCapabilities` 的待确认项另有带来源的外部核对；没有静默替换历史原句。无参数仍非铁律，UX1 和四项限制不变。

### V1.3 发出稿的当前效力

[正文](Ronova_Enhancement_Addendum_V1.3.md)和[发出说明](../reference/Ronova_Enhancement_Addendum_V1.3_HANDOFF.md)是Ronova侧原提案，不是收到的外部V1.2勘误；原文保留，“尚未进入规划”只描述发出时。当前按[fix6回执](RESTRUCTURE-REVIEW.md#review-fix6-v13)融合：①⑦⑨接受，②③④⑤⑥修改，⑧本轮废弃。技术更正与设计选择明确列出，不能把保留原文当作反向恢复被否决语义的授权。项目/来源层次不变；不新增第三方兼容铁律。

## 实现与验证状态

当前实现只按 [原进度记录](../CURRENT-PROGRESS.md) 和实际产物/日志的适用范围理解。本轮没有复核这些运行事实，也没有修改其中的计数、日期或状态。

原文的历史成功/失败、已实现/未实现、设计冻结与源码候选字样照存，不是本轮的验收结论。`RESTRUCTURE-CHECK.md` 只记录文档恢复、ID 保留、链接与改动边界的可检查事实，不给 Mod、阶段、能力或成品宣告测试通过。

## 旧入口锚点兼容

下列旧标题只用于历史链接跳转，效力已按上文更新。

<a id="有效规划"></a>

[旧标题：有效规划](MS-SKILLS-PLAN.md#orig-index-l0001)。

<a id="当前优先工作"></a>

[旧标题：当前优先工作](MS-SKILLS-PLAN.md#orig-index-l0007)。

<a id="唯一规范顺序"></a>

[旧标题：唯一规范顺序](MS-SKILLS-PLAN.md#orig-index-l0027)。

<a id="后续阶段"></a>

[旧标题：后续阶段](MS-SKILLS-PLAN.md#orig-index-l0040)。

<a id="参考与历史"></a>

[旧标题：参考与历史](MS-SKILLS-PLAN.md#orig-index-l0054)。
