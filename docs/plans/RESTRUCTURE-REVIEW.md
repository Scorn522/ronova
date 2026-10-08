# Ronova MS 重构 · 审阅修订说明（fix1–fix7）

**fix12当前决策：** [采用③及范围](#review-fix12-acceptance) · [验收分工](WIRING-ACCEPTANCE.md)。新增判据正式化，证据仍待实施后绑定；不改原运行状态。下方旧“当前”保留轮次语境。

**当前：fix11已有基线补遗。** [处理回执](#review-fix11-baseline) · [当前规划](R66-CAPABILITY-WIRING-PLAN.md#eight-existing-baseline)。fix10及更早节保留其各自轮次，不由本补遗改写决策或实现状态。

**当前回执：** [fix10八方向核验融合](#review-fix10-eight)。下方历史轮次原义保留；当前源码仍为r66。

**当前修订：fix8。** [核验/天花板与检查器回执](#review-fix8-verification)；以下旧轮次按各自日期阅读，不冒充本轮复跑。

> **当前审阅：** [fix7 同权限清点融合](#review-fix7-surface)；来源原稿、本轮核对和待实施设计分别阅读。

> **前轮新增审阅：** [fix6 V1.3 九项回执](#review-fix6-v13)；更早各轮记录按自己的基线阅读。

> **历史fix5：** [fix5 增补融合](#review-fix5)。以下旧轮次记录保留各自基线。

> **前轮规划增补：** [fix4 接线更新](#review-fix4)。fix3 的重构收口结论保留；本轮不重开结构迁移，只纠正点名技术判断与实施顺序。

结构收口记录：[fix3：事实来源、记录效力与人工审阅边界](#review-fix3)。历史：[fix2：根入口与验收规格对接](#review-fix2)；下列 fix1/fix2 记录保留其原时点，不覆盖为新运行结论。

下列 fix1 历史记录日期：2026-09-14。输入：`ronova-docs-0.2.0-alpha.7.17-r66-ms-restructured.zip`。当轮处理用户对重构结果的五项审阅意见。

[规划入口](README.md) · [不变量索引](INVARIANT-INDEX.md) · [迁移映射](RESTRUCTURE-MAP.md) · [核对记录](RESTRUCTURE-CHECK.md) · [fix1 历史机器核对](restructure/review-verification.json)

**仅修改文档解释、阅读入口和派生索引。** 没有编译、启动 Minecraft、执行源码功能/专用服/阶段验收；没有复核原文中的历史实现判断，没有宣布任何阶段完成。

原进程段落确有“进程效力调整”就地提示。本次没有删除或放宽“不起外部进程”；也不把这次文档核对当作源码已经满足边界的证据。

<a id="review-p1"></a>
## P1 · 恢复不变量的人读总入口

新增 [INVARIANT-INDEX.md](INVARIANT-INDEX.md)，由原 `id-inventory.json` 生成，并用 `section-map.json` 与原字节快照校验。首页恢复原 DS §4.2 / §5 的分组导览，随后按来源列出全量编号；不复制第二份竞争正文。

| 项目 | 本次索引记录 |
|---|---:|
| 原来源 | 16 份 |
| 来源内原 ID 拼写条目 | 424 行 |
| 新书位置条目 | 929 个 |
| 原编号文本出现 | 1,047 次 |
| 跨来源不同编号拼写 | 273 种 |
| 无编号宪法级名称 | CF 8 行、P0-A 8 行，按来源分别保留 |

**424 行不代表 424 条独立语义不变量。** 每行标明来源、原 ID、全部新书锚点、原始行范围和次数，既不跨文件合并，也不把同一文件中的同号异义强行归一。原 [DS §5.3](MS-SKILLS-PLAN.md#orig-ds-l0299) 已记录两张 L6-INV 表含义不一致，本次保留该提醒，不替来源裁决。

`D14–D31`、`D34–D52` 没有被原编号清单登记为独立拼写；原稿存在范围引用和分组原则。索引提供范围入口，不根据端点生成不存在的定义。CF / P0-A 的无编号宪法级名称从各自原章节提取，不编造编号。

新索引已接到 `docs/plans/README.md`、`docs/README.md`、三本主书开头和旧 `docs/DESIGN-SPEC.md` 的开头及 §5 兼容入口。重新生成或核对见索引末尾的命令；原 `id-inventory.json` 未改。

<a id="review-p2"></a>
## P2 · 主书体量：记录现状，不宣称解决

本轮**没有拆分、压缩或改写三本主书正文**。保持 MS 三书的 82 个二级/三级标题文字、级别、顺序，以及 1,047 个原文分段的唯一落点。只在书头增加索引/效力说明链接，在有关原段外补就地提示。

大文件仍然存在；本轮改善的是“从哪里读”和“同号条款如何找”，不是单文件大小。章节锚点不变，既有逐节映射继续可用。书体量的本次实际字节与行数见机器核对记录，未据此推算功能完成率。

<a id="review-p3"></a>
## P3 · 无参数不是额外硬约束

依据用户前一轮“唯一限制”的范围，本轮明确：**无参数不是硬约束，也不是额外必选完成条件；它保留为默认取向。** 原 [RULE 的 W5](MS-SKILLS-PLAN.md#orig-rule-l0247) 已将其写成取向而非违规判据。

A5 这里指 [DS 加强 A 块](MS-SKILLS-PLAN.md#orig-ds-l1007) / [RULE 加强 A 块](MS-SKILLS-PLAN.md#orig-rule-l0734)中的程序化模块开放；I7 指 [RULE 能力空间表](MS-SKILLS-PLAN.md#orig-rule-l0267)的进程内 attach 管道。**不是** [P0-A A5 — Root Writer Uniqueness](MS-SKILLS-PLAN.md#orig-p0a-l0152)。

为无参数目标而设的唯一必选性被明确条件化：可以改用满足四项限制的其他方案，但采用某条路线后仍必须处理该路线实际依赖的权限、初始化和替代接线。这里不判断何种入口已可运行，不将“可选”理解为可随意删除已依赖的能力。

[README 的当前口径](README.md#scope-startup-parameters)、映射的 `COND-06`、`ms-coverage.json` 和有关原段外提示已同步。允许宿主启动参数不意味着让 Ronova 起进程；也不意味着强制用户手工加参数。

<a id="review-p4"></a>
## P4 · DS 的 P 层改按可修订设计选择阅读

[README 的专门说明](README.md#scope-p-layer)明确：原“自己冻结的规范”不再是不可修改的优先权。下列对象全部指 **DS 的 C 块**，不是 RULE 或其他文档的同号项。

| 原 ID（DS C 块） | 原工作内容 | 本轮效力说明 |
|---|---|---|
| C1 | 判定实现与 writer 分工的收口 | 可修订项目设计；不因冻结字样成为额外铁律 |
| C11 | 14 个动作的执行器接线与调度收敛 | 保留原工作项；不据文档修订宣称接线完成 |
| C12 | 逐键矩阵 revision 2 | 保留原设计和与完成口径的关系；二者均可显式修订 |
| C13 | 天花板管道 / RuntimeCapabilityProjection | 保留原设计和与完成口径的关系；本轮不复核旧计数 |
| C14 | NBT 链语义实现与入口关系 | 可修订项目设计；原待核内容仍是待核内容 |
| C16 | 可达集合与内容驱动、非定向处置 | 具体方案可改；“不针对”等四项限制不能因此越过 |
| C19 | 扩展性、原子性、热路径 I/O 与发布关系核对 | 可修订项目设计与核对项；没有把“待核”改成完成 |

来源：[DS C 块原表](MS-SKILLS-PLAN.md#orig-ds-l1040)、[DS 对内判断](MS-SKILLS-PLAN.md#orig-ds-l0962)、[DS 实施顺序与完成关系](MS-SKILLS-PLAN.md#orig-ds-l1126)。七个原编号未改、未重排、未删除；原表外增加说明而不是覆盖表内原句。

**“不再永久冻结”不等于“自动取消当前完成条件”。** DS C12 / C13 及 FULL L5 / V2.5 的完成判定可以改，但本轮没有选择替代条件。继续使用旧完成口径时仍按旧口径核对；更改后必须明确新条件和旧证据能否复用，不能单凭解除冻结宣布达标。

这也是对“D91 已冻结”的处理：它仍有原文、原 ID 与来源，可以保留、修订或替换；**不能只凭冻结二字禁止修订，也不能把修订当作实现证据。**

<a id="review-p5"></a>
## P5 · 修正自有配置、日志与持久化的措辞

README 已改为：

> 项目自有配置、日志、状态持久化等原设计本轮未实施，也不因本轮界定改变其设计效力；其具体边界仍须据实际对象、文件类别与上述限制判断，不能把不删对方文件偷换成只不删 JAR。

这里的“本轮未实施”只描述文档修订没有进行实现，不取消设计，也不否认原版本中可能已有实现；本轮未对这些实现状态作判断。

<a id="review-integrity"></a>
## 文档核对与交付边界

原字节快照、`section-map.json`、`id-inventory.json`、原 MS 三份资料、历史进度和源码包保持不变。原文保留的历史成功/失败词语不视为本轮运行结果。

使用原 `verify_restructure.py` 复核原文恢复、原编号、MS 骨架和已登记落点；使用新增 `generate_invariant_index.py --check` 复核索引；另行解析 Markdown 本地链接。实际结果登记在 [review-verification.json](restructure/review-verification.json) 和 [RESTRUCTURE-CHECK.md](RESTRUCTURE-CHECK.md)，不对外部网址或游戏行为作结论。

完整修订文档包用于独立阅读；累计覆盖包可合并到已有原 r66 文档或上一轮 MS 重构文档的项目根目录。覆盖包不删除旧文件、不含源码替换；只有源码包而没有原文档时，先合并完整文档包。

---

<a id="review-fix2"></a>
## 2026-09-14 · fix2：根入口与验收规格对接

输入是 `ronova-docs-0.2.0-alpha.7.17-r66-ms-restructured-fix1.zip`。以上 fix1 处理记录原样保留；本节是后续补记，不把历史记录覆盖成新运行结论。

### ① 根入口补齐

根 `README.md` 增加 `INVARIANT-INDEX`、本审阅说明及 [VALIDATION-TRACEABILITY](VALIDATION-TRACEABILITY.md) 入口；`docs/README.md` 补审阅说明和验收规格对照；规划入口与派生不变量索引同时互链。从仓库根可直接进入，不必先猜到 `docs/plans/README.md`。

### ② 编号空洞不补造

保留 fix1 的范围读法和全部原 ID。`D14–D31` / `D34–D52` 的 37 个编号不新增定义，不用重新编号“填平”。新对照按来源与章节限定 ID；RULE 加强 D1–D4、L6 决策 D1–D4 分列。范围端点也不自动当成独立条文。

### ③ 规格侧双向映射已建立；台账与运行证据尚未接完

新 [VALIDATION-TRACEABILITY.md](VALIDATION-TRACEABILITY.md) 按 V26 原文列出全部 68 项 B01–B23 / X24–X68；按原来源限定的 120 行条款/缺口（L6 决策 13、V26 明确不变量 55、DS C 块 19、RULE C 块 18、RULE D 块 4、MS 缺口 11）反查原用例；三份 MS 的六项可见结果及两组各 11 项矩阵共 28 条逐项对接。

这些是**设计关系**，不是 120 项实现、68 项验收覆盖或 28 个新测试。每行分别标明原文明引、设计子范围、仅邻近/逆向关系或未定位直接项，并给出仍缺的观察/断言。没有把“清除目标不许重插”当作“保护目标规范实例能补回”，也没有把 Root 原子提交当作 ID/UUID 集合修复事务。多部位实体在原 B/X 中未找到直接一组断言，明确保留缺口。

**本轮有一个输入限制：原 r66 文档、fix1 文档与源码压缩包均未提供 `validation/7.17/acceptance-matrix.json`。** 原进度中的 `28 PARTIAL / 40 NOT_RUN / 0 完整 PASS` 只按来源转述，不拆成 68 行。没有逐项读取台账，没有恢复缺失附件，没有用样例 JSON、历史片段或总数推填状态。全部行状态标为“台账未读取”，不是新增 NOT_RUN，也不是改为 N/A。

`GAP-MAP-07` 增加本次规格对接入口，**仍不关闭**；实际 artifact / fixture / assertion / server-and-client observation 的证据连接尚未完成。映射本身不触发原台账状态更新。

### 旧前提在新入口中不复活

**B23** 原 helper attach/relaunch 正例在当前“不起外部进程”边界下不适用；ID 和原文保留，当前 JVM 唯一 writer 子断言仍须核查。不能沿旧例启动 helper，也不能把整项直接抹成 N/A。新入口、fallback 和实际进程树尚待真实证据。

**X66** 原五级规范优先顺序不恢复强制效力；编号自包含、历史缺件登记及状态真实性要求继续保留。没有因为文档脚本无差异就把 X66 或任何 B/X 项改成完成。

### 可重复核对与剩余范围

新映射数据在 [validation-traceability.json](restructure/validation-traceability.json)，生成/核对脚本为 [generate_validation_traceability.py](restructure/generate_validation_traceability.py)。`--check` 只核对原文摘录、原编号与来源、关系集合及生成内容；语义关联仍是可审阅的人工设计判断，不宣称机器已证明覆盖完整性。

脚本可以在明确真实账本 schema 后只读输出原状态、缺失/重复/额外 ID 及原文件哈希；不会启发式猜字段，不写 `validation/`。账本缺失与文档核对结果分开报告。附带的小型解析器测试仅使用临时合成输入，**不是游戏验收夹具或运行证据**。

三本主书未修改；原迁移块、保真快照、原 ID 清单与章节映射保持不变。本轮没有修改源码、构建、资源、Gradle、进度或运行证据；没有构建或启动游戏。实际核对结果见 [fix2 文档记录](RESTRUCTURE-CHECK.md#review-fix2-check)。


---

<a id="review-fix3"></a>
## 2026-09-14 · fix3 收口：事实来源、记录效力与人工审阅边界

输入：`ronova-docs-0.2.0-alpha.7.17-r66-ms-restructured-fix2.zip` 与本轮用户审阅回报。只修正当前阅读口径和记录入口，不重新重构主书、不变更技术条款或验收状态。

<a id="review-fix3-ledger"></a>
### ① 用户工作区的真实台账核查回报

**来源是用户在本轮消息中报告的实际工作区运行，不是本交付环境独立读取了该文件。** 用户报告工作区存在 `validation/7.17/acceptance-matrix.json`，使用以下命令（按原参数记录）：

```text
python generate_validation_traceability.py --root . \
  --ledger validation/7.17/acceptance-matrix.json \
  --records-pointer /cases --id-field id --status-field status
```

以下字段摘自用户提供的结果；不是本轮在本地生成的真实台账运行输出：

```json
{
  "total_selected_rows": 68,
  "selected_original_status_counts": { "PARTIAL": 28, "NOT_RUN": 40 },
  "duplicate_ids": [],
  "missing_ids": [],
  "extra_ids": [],
  "exact_68_unique_ids": true
}
```

用户还报告没有写入 `validation/`，文件写入时间仍为 `9/11 09:56:44`。这是带来源的用户观察；本轮没有原文件字节、原 SHA-256、68 行状态明细或前后哈希可独立核验。**不把修改时间未变单独提升为字节未变的证明**，也不从上述汇总造出逐项状态。

台账 ID 集合与状态汇总接线子项据这份回报记为已核对。fix2 的“随包未提供台账”作为当时输入事实保留；当前入口明确不将其推广为项目缺文件。`GAP-MAP-07` 总体仍开放：artifact / fixture / 断言 / receipt / 双客户端运行证据尚未绑定或复核。

`validation-traceability.json` 以 `USER_REPORTED_WORKSPACE_RUN` 单列回报；生成器输出将其与本次实际读取文件的 `ledger_observation` 分开。本轮文档检查可以核对已记录摘要的自洽性，**不能据此声称认证了用户那次外部运行**。

### ② 当前入口收敛；历史审计不互相覆盖

新增 [restructure/README.md](restructure/README.md)。当前文档核对使用固定文件名 `current-verification.json`，包含本轮三个脚本结果、文件边界与本地链接增量对照；以后更新该当前摘要，而不是每轮再新增一整套带 fix 编号的审计文件。

初轮、fix1、fix2 的五份审计 JSON 全部保留原字节，分别标明适用基线。当前摘要不是运行验收凭据，也不能跨基线替代历史证据；实际改变检查对象后应重新核对，而不是靠“最新”两个字延长有效期。

### ③ 人工语义边界移到读者可见位置

120 行条款/缺口、231 条关联、28 条 MS 检查及 64 条候选关联保持原数据不变。`--check` 检查数据与生成内容一致，不证明人工关联正确或覆盖完整。新入口将这一点放到关联图例之前，并从原数据生成 9 行 `case_ids` 为空的优先审阅列表。

该列表只是导航，不是本轮对 9 行作出了语义审阅结论；也不声称其余 111 行已经正确。没有强贴 B/X、新增测试 ID、把未定位项改成 N/A，或把台账 ID 对齐当成覆盖证明。

### 收口与交付范围

三本主书、原迁移块/ID、原章节映射、历史证据及 `validation/` 保持不变。本轮不跑 Mod、不构建、不改变阶段完成判定。文档重构交付与后续语义审阅/运行取证是不同工作，不能为了后者持续把重构判为未收口，也不能因重构收口而宣布验收完成。

只含 fix2 → fix3 变更的小补丁须覆盖到**已有 fix2 文档的项目根目录**；完整 fix3 文档包可独立阅读或合并到项目。均不含源码、构建或 `validation/` 替换。核对边界见[当前记录](restructure/README.md)与[收口核对](RESTRUCTURE-CHECK.md#review-fix3-check)。

<a id="review-fix4"></a>
## fix4：接线规划与 JVMTI 更正

本轮依据用户请求，把上一轮源码核对落成可交付规划。[增补主文](R66-CAPABILITY-WIRING-PLAN.md)列全 54 个键、14 个动作及五条 JVMTI 方向；[源码摘录](../reference/R66-CAPABILITY-AUDIT.md)按收到字节保留；[静态清单](restructure/capability-wiring-baseline.json)记录源码哈希、枚举/依赖提取、可识别摘录比较与就地注记位置。

三书原文块、全部旧 ID、82 个 MS 骨架标题和旧原始映射不改。主书增加导航及原块外更正，不再声称整本书字节未变。撤回错字段导致的 native bind 禁用判断；限定 prefixes/动态代码/当前帧/结构证据的实际范围；纠正“48 键永久不可用”“硬编码文案证明 13 个无执行器”“服务器整链不可达”的外推。

实施顺序先 DS:C13 与现有 DS:C11 局部执行/诊断贯通，再补 D 块真缺口，后推进原生证据与条件路线。未改最终 54 项极限目标，也不删除 B1；B1 受包装时机与客户端覆盖证据限制。范围限制、无参数选型、P 层可修订、局部能力不变成全局完成等口径继续适用。

本轮未重新做人审 120 行验收关系，未复算真实台账，未改其 68 个 ID/状态；用户工作区 28 PARTIAL / 40 NOT_RUN 的回报维持来源说明。GAP-MAP-07 证据侧仍开放。没有新增阶段 PASS 或新的运行完成结论。

核对入口只更新 [current-verification.json](restructure/current-verification.json)，历史两套 link-audit/三套 verification 保留原字节。本次是文档增补，不提供编译后的 JAR 或已加强源码。


<a id="review-fix5"></a>
## fix5：工作区加强增补融合

**输入与完成范围：** 三份输入已完整读取，V1.1 为已被 V1.2 取代的对照；V1.2 已融合，MERGE-NOTE 为交接来源，不作规范。全部原始字节保留，当前只维护一份[能力接线规划](R66-CAPABILITY-WIRING-PLAN.md)，详见[逐节融合表](RESTRUCTURE-MAP.md#fix5-addendum-merge)。

**实际融入：** 四个申请字段逐项诊断；GetPotentialCapabilities 错误与 GetCapabilities 回读；ThreadEnd 未尝试/失败/启用与清理状态；保留局部变量能力缺失时已有的认证 callsite 回退；整组申请失败与已有能力的区别。以上进入第一批的具体代码落点和关键批次观察要求，生产源码仍未修改。

**显式限定，非静默纠错：** any_class 一个申请语句并不证明 VM 不使用能力，亦不足以判定整项 B7 的实现状态；B7 保留待做/待证。ThreadEnd 缺诊断不等于 native_task_exit 正常释放不存在。V1.2 原文和交接说明的较强推论在来源页照存，现行采用范围在接线章节写明。

**外部核对与来源分层：** 按交接登记的待确认项，本轮查了 JDK 17 和版本化 OpenJDK 17u：所查实现的不可申请整组会在写集合前返回，不会撤销原先持有的能力；正确从 potential 读到 0 的可选位不进入请求，不能据此推“必然拖死核心”。原作者当时访问失败的事实保留，没有冒充用户机器的运行复现。DynamicCodeGenerated 本轮没有独立触发，继续沿用 fix4 有限观察范围。

**边界与历史：** 四项限制、无参数非铁律、UX1、原 16 来源全部条款与不变量不变。原源码/构建/资源、进度与 validation 未改，68 项规格、120 行人工关联和台账状态没有重判；GAP-MAP-07 不因这次来源融合关闭。文档一致性核对不产生 Mod 或阶段 PASS。

**记录卫生：** 固定更新 `current-verification.json`，新添的 `enhancement-addendum-merge.json` 用于本次三份新增来源及其持续融合关系，不是再复制一套历史 link-audit/verification。既有历史审计原字节保留。

<a id="review-fix6-v13"></a>
## fix6 · V1.3 九条方向回执（2026-09-14）

**来源方向保留：** 这是 Ronova 侧向研究方发出的九条提案，不是外部作者对 V1.2 的勘误。V1.1/V1.2/旧 MERGE-NOTE 原文和处置继续保留。本轮完整读取新正文566行与交接说明133行，并在原接线章节和批次内作出下列显式决策。接受/修改是规划选择，绝非源码已实现或台账状态。

| 原方向 | 回执 | 具体处理 | 前置是否成立 | 原计划落点/批次 |
|---|---|---|---|---|
| 1 · 观测成本模型 | **接受** | 保留六类，按(surface,source)加子账、每源过载动作与父总闸；不伪造外部事件频率上界。 | 无新硬前置；成本账与过载行为待实施。 | [第一批](R66-CAPABILITY-WIRING-PLAN.md#v13-cost-model) |
| 2 · 对象生命期账本 | **修改** | ObjectFree只补回收观察；不能发terminal判决或替掉活对象弱引用；组合能力与队列不足则回到原路线。 | ①+实际能力获授/事件启用+callback-safe缓冲均待证。 | [条件第四批](R66-CAPABILITY-WIRING-PLAN.md#v13-object-lifecycle) |
| 3 · JIT时机重转换调度 | **修改** | 只先测现有重转换；调度实现不进入必做，收益数据不成立即废弃。 | ①必要；实际频率/净收益数据缺失，执行前置未成立。 | [第一批仅测量，条件第四批复审](R66-CAPABILITY-WIRING-PLAN.md#v13-jit-scheduling) |
| 4 · 类血缘执行器 | **修改** | 共享只读解析与证据，但攻击/拦截各自授权/消费；补真实执行闭环，不据命中数推断全部间接路径。 | 职责边界已设计；共享解析/消费者/预算与结果未接通。 | [第二批](R66-CAPABILITY-WIRING-PLAN.md#v13-lineage) |
| 5 · 证书到期/依赖摘要 | **修改** | 现有CompletionTracker已有依赖版本和超时失效；扩展枚举依赖/原因与原子使用边界，而非从零造Authority。 | 无新硬依赖；现有失效检查可见，完整依赖/使用闸未证。 | [第二批](R66-CAPABILITY-WIRING-PLAN.md#v13-certificate) |
| 6 · 结算归属断言 | **修改** | 现有owner/lease/claim守卫存在；补结算端口覆盖、并发与恢复语义，不另起所有者。 | 现有Root计划基础可见；跨端口唯一归属未证，⑦须等此项。 | [第一批](R66-CAPABILITY-WIRING-PLAN.md#v13-settlement) |
| 7 · 跨表面预算分配 | **接受** | Root计划统一切片和父子预算，不重复扣减，不新造批准者。 | ①⑥为硬前置，均尚未运行验证。 | [第三批，①⑥之后](R66-CAPABILITY-WIRING-PLAN.md#v13-cross-surface-budget) |
| 8 · 行号表改写信号 | **废弃** | 没有独立样本/增量收益依据，本轮不建实现或默认研究任务；不是宣称已测无效。 | 独立样本和判别证据未具备；不安排执行。 | [不入批次](R66-CAPABILITY-WIRING-PLAN.md#v13-line-number-research) |
| 9 · 绕过面报告 | **接受** | 报告分析状态、条件集合、范围/版本和依据；保持L6-INV-10准确含义，不等同所有同权限对手必然绕过。 | 无新硬前置；报告链/协议消费者待实施。 | [第一批](R66-CAPABILITY-WIRING-PLAN.md#v13-bypass-report) |

**没有偷偷改来源结论：** ②的GC/终结混同、⑤⑥“从零”前提、⑦“无载体”的范围、④四个源码命中的组成，以及⑨同权限/完全失陷的区别都在对应规划段显式标明新增复核或设计修订。[源码摘录/官方依据](../reference/R66-V13-REVIEW-EVIDENCE.md)与原作者的头文件/工作区回报分开。

**前置登记而非伪造完成：** ①是根，⑥可并行，⑦依赖①⑥；②需①和实际VM能力，③还需正向成本证据。④先划清攻击/拦截职责，⑤复用原依赖失效链，⑨接现有报告生产者/消费者。⑧不进必做队列；③不给“技术可行”永久保留权。不存在新增B/X状态或“接受=ACTIVE”。

**规则口径：** L0三条铁律和UX1不变；MS其它规定、项目P层仍可显式修订而非不可覆盖的上游。新增预算/证据要求是本轮工程选型，不加第三方兼容铁律；不按特定Mod/作者/玩家硬编码，不把合法当前目标解析误判为“针对”。不新造SubjectKey、Root或平行D/INV/A/R/ID/T编号。无参数口径不变。

来源页只加清楚区分当前处置的顶部说明，不使用新的外部原文块标记；其后原文逐字节保留，输入ZIP原字节归档。完整逐节对照见[fix6映射](RESTRUCTURE-MAP.md#fix6-v13-merge)。只读核对脚本沿用并扩展`verify_addendum_merge.py`，当前摘要仍用固定文件，不堆一份新link-audit。

**未做：** 未编译、未启动JVM/游戏、未进行GC/JIT/隐藏类场景测试、未改源码和validation，未回填任何PASS。原120行B/X人工关系及其9行无直接用例保持；本轮九条技术回执不等于那张表已完成人审。GAP-MAP-07的台账集合/汇总仍据用户回报已核，artifact/fixture/断言/双客户端绑定继续开放。

<a id="review-fix7-surface"></a>
## fix7 · 同权限干预面清点融合（2026-09-15）

**收到并完整阅读：** `ronova-same-privilege-surface-handoff.zip` 中唯一 Markdown。它自述为只读静态清点，不是新提案；本轮不改成V1.4，不新增方向/不变量/批次。原文与压缩包原字节保留。

[原稿](../reference/R66-SAME-PRIVILEGE-SURFACE.md) · [独立复核证据](../reference/R66-SAME-PRIVILEGE-REVIEW-EVIDENCE.md) · [逐节映射](RESTRUCTURE-MAP.md#fix7-surface-merge) · [已融合规划](R66-CAPABILITY-WIRING-PLAN.md#same-privilege-surface)

### 八条加固建议：沿原编号处理，不新增工作线

| 原§7序号 | 原动作 | 处理 | 接回现有顺序 | 具体处理 |
|---:|---|---|---|---|
| 1 | 收API表面 | 修改后纳入 | 第一批敏感门面/合法调用迁移；第二批扩大范围 | 71处全部跨包，不能直接批量package-private；把查询、提交、生命周期拆开，减少暴露全权可变对象。 [当前落点](R66-CAPABILITY-WIRING-PLAN.md#surface-api) |
| 2 | 三个破坏性入口授权 | 接受并细化 | 第一批高影响变更处 | 覆盖公开转发器、真实发行实例和scope；release用合法生命周期权利，不强套terminal租约；Lease类型本身不等于授权。 [当前落点](R66-CAPABILITY-WIRING-PLAN.md#surface-api) |
| 3 | 决定侧进入封印 | 接受并细化 | 第一批所选producer依赖闭合 | 按实际依赖选自有类及内部类，并核真实构建数/字节/加载器；不凭283历史数或前缀登记声称隔离。 [当前落点](R66-CAPABILITY-WIRING-PLAN.md#surface-policy) |
| 4 | 持久化线补租约 | 接受并细化 | 第一批选中保存/NBT路径；第二批扩展 | 四个目标需变换、验证、native mask、消费者和失效链同时接；只填名单不算保护。 [当前落点](R66-CAPABILITY-WIRING-PLAN.md#surface-mixin) |
| 5 | 网络/服务端补租约 | 接受并细化 | 原第二批；第一批实际依赖者提前 | 覆盖Connection/ServerGamePacketListenerImpl/MinecraftServer的具体合同及侧别、时机；不添加新批次。 [当前落点](R66-CAPABILITY-WIRING-PLAN.md#surface-mixin) |
| 6 | 客户端登记未保护 | 修改用语后纳入 | 第一批报告 | 准确写未进入当前MC租约/逐注入未证明，不能把已有其他Java控制路径一并说成不存在。 [当前落点](R66-CAPABILITY-WIRING-PLAN.md#surface-mixin) |
| 7 | 落盘改HMAC | 保留条件工作 | 第一批报告现状；原第二批持久化工作按前提接入 | 须先明确key来源/保存/轮换/恢复、旧格式迁移、上下文与抗回滚合同；不保证同进程隔离、不删除该目标。 [当前落点](R66-CAPABILITY-WIRING-PLAN.md#surface-durable) |
| 8 | Mixin自校验 | 接受并细化 | 第一批差分/报告；第二批合同核对 | 区分启动失败与运行失证，复用已有失败信号，登记逐注入及观察位置限制，不靠静默require=0。 [当前落点](R66-CAPABILITY-WIRING-PLAN.md#surface-mixin) |

### 需要显式区别的来源判断

| 原稿主题 | 本轮处理 | 依据/范围 |
|---|---|---|
| 规模与名单 | 源码复算一致 | 378 个 Java 源文件/40,428 物理行；31 个 mixin 源文件/872 行；24 个去重目标；41 Accessor、26 Inject、6 Redirect、0 Overwrite。这里的源文件数不是编译后含内部类的 class 数。 |
| 14/10 与 5/6 | 保留数字，收窄含义 | 14 个目标名与 16 个 MC 原生名单重合，10 个不重合；6 个 Redirect 中 5 个的目标未列入。名单重合不证明该类每个 Mixin 注入都由语义合同验证，更不证明租约当前有效。 |
| 语义链不能还原 | 显式修正 | 没有 MC 原始镜像整体回滚，但 NativeTransformBridge → RonovaTransformer 已调用 repair/patch 并产生替换字节；SemanticGateVerifier 校验登记控制流。不能写成完全没有重施或修复，也不能扩大为任意 Mixin 可恢复。 |
| 全部机制在下游 | 保留残余风险，撤下绝对顺序 | 同权限后续转换可能改变已有注入；JVMTI 先按是否可重转换分组、再按环境创建顺序派发，重转换还区分初始字节与复用输出。ModLauncher 实际管线、agent 顺序和最终落地字节本轮未测，不采用“全部固定下游”的通用结论。 |
| policy 封印名单 | 源码前缀确认，构建数归来源 | 111 个前缀只有 RecoveryChainPolicy/TerminalActionPolicy 两个 policy 类；点名的决定侧生产者未选入。源码包没有构建后的封印 manifest，283 类是交接原稿所报，512/4 MiB 是选择器上限；本轮不证明实际封印镜像装载、覆盖或字节余量。 |
| ceiling 与执行 | 区分生产路径及范围 | 生产源码中 .withCapability 有 3 处显式调用行（全局两处、实体局部一处），所示授予最多 PROTECT_RONOVA；通用 RecoveryChainPolicy.resolve 有 8 处显式调用行，四类 registered* 有 13 处。局部 registered* 按请求及绑定条件计算，可表达 rank 3/4；不等于广谱已经获得 rank 4，也不将有限源码外推为未来所有运行路径永远如此。 |
| 局部固定五键 | 补上下文 | TerminalActionPolicy 有 5 个固定键，resolve 另并入所选动作 requiredKeys；不能把循环发布集合永远记为五键。 |
| 全 54 键/所有动作必废 | 代码路径推论未实测 | 决定侧完整性缺口成立，但“任一 ceiling OFF 改动必使所有局部动作失效”不能由通用解析推出；局部视图可重新赋值，恢复注册通道另读 requested。作用范围、具体改动和实际消费者都需验证，不能从名单缺项证明一次静默全废攻击。 |
| 71 个 API 调用包归属 | 与原稿不同 | 71 处显式 ServerRuntimeRegistry.runtime 调用分布在 8 个其他包，bootstrap 同包为 0。只去掉 public 会破坏这些源码调用；应先拆只读/提交/生命周期门面和迁移必要调用，不照抄“多数包内”。 |
| 破坏性 public 入口 | 接受为优先加固面 | invalidatePasses、clearIdentity、release 的方法体没有操作级授权前置；存在空值/同步等检查不等于授权。连同注册表的公开转发入口与运行时返回的可变对象一起审阅。普通 public 调用不改方法字节，封印不能替代请求授权；但可用反射/agent 等能力的同权限对手不因访问修饰符而得到隔离。 |
| 恢复源全程无绑定 | 收窄为边界信任待确认 | approveRecoverySourcePlan 确实没有显式 bindingVerified 参数，传 true；但 evidence.fresh() 同时要求 observerVerified，重建调用方有 verified/boundary/nativeCurrent/stillBound，维护路径也有资格、语义观察与采后 stillBound。缺口是公开审批边界是否可信接收证据/布尔值、是否在持久提交前复核发行来源与对象绑定；不是已有链完全没有检查，也未证明可利用。 |
| 落盘无密钥 | 当前实现确认，补限制 | 本轮 Java 文本检索未定位 HMAC/Mac/SecretKey/Signature 等，当前 Java 后端与 native Hash 的可见路径使用无密钥摘要。摘要一致不等于可信来源或不可回滚；源码可读不等于能推出运行期随机密钥，但如果同权限环境可取得密钥或改验证路径，HMAC 同样不提供隔离保证。 |
| 无 JPMS/无信号/最稳最脆 | 不将词法零命中变成全局结论 | 源码没有 module-info.java，未观察运行时模块图；integrity 包没有特定 Mixin 专项调用不代表语义校验失败和注入失败完全无信号。HEAD/Redirect 的稳定性排序仅为原作者判断，本轮只核对位置与合同缺口。 |

### 实施状态与原边界

这是将已发现覆盖/授权缺口落实到现有规划的文档修订，不是缺口已修。原 C13/C11/D 工作与V1.3①⑤⑥⑨仍在原章节；先保护当前执行链的真实依赖，再增加新机制。不得把仅有观察/名单扩大成Extreme完成，也不把注册局部执行的rank 3/4削成全局2。

本轮未改生产源码、构建、资源、原validation台账或任何运行证据；不新增PASS。三书的全部原文块、既有不变量编号与82个MS二/三级标题保持。120条验收候选关系及其中9条未定位直接用例不因本清点自动得到语义证明；GAP-MAP-07的证据绑定仍开放。

真实台账68唯一ID与28 PARTIAL/40 NOT_RUN仍是此前用户工作区回报，本轮没有读到原账本字节或独立实测。静态零命中、类名在名单、门面规划、HMAC设计或源码摘录核对均不代表运行强度已经提高。


<a id="review-fix8-verification"></a>
## fix8 · 核验补遗、极限天花板与检查器缺陷融合（2026-09-15）

**输入性质保留：** 四文件交接包中，核验补遗和天花板重审均写于fix7之前，第三份是fix7检查器缺陷报告，第四份是交接索引。三份报告不是V1.4，不新增方向/批次。来源原字节、独立复核与当前设计分开；旧fix6/fix7记录按当时范围阅读，本节不将其历史结论反写成今日运行状态。

[原交接索引](../reference/v13-verification-ceiling/README-INDEX.md) · [核验原稿](../reference/v13-verification-ceiling/R66-V13-VERIFICATION-ADDENDUM.md) · [天花板原稿](../reference/v13-verification-ceiling/R66-EXTREME-CEILING-REVIEW.md) · [工具缺陷原稿](../reference/v13-verification-ceiling/R66-MERGE-VERIFIER-PORTABILITY.md) · [独立源行与检索](../reference/R66-VERIFICATION-CEILING-REVIEW-EVIDENCE.md) · [当前规划](R66-CAPABILITY-WIRING-PLAN.md#verification-ceiling) · [逐节映射](RESTRUCTURE-MAP.md#fix8-verification-merge)

### 核验补遗：保留九方向原决策，只提高三处精度

| 原报告项目 | 当前采用与增量 | 归属/批次 |
|---|---|---|
| 六类预算已存在 | 接受；OBSERVATION本来就有，缺的是(surface, source)二级归集，不重建一级类别 | ①仍第一批 |
| TerminalOperationPlan四成员定值 | 接受；堆预留必须16384、required/optional集合及工厂合同均固定；先明确原Root发行权限、计划/合同revision、旧切片撤销/未决用量，再合法解锁。ownerIdentity是String，不假定已有RootOwner类型 | ⑦仍第三批、①⑥之后 |
| CompletionTracker没有证书到期/声明式期望集 | 接受；已有采样陈旧、稳定窗口和snapshot变化检查是基础，独立有效期与逐依赖期望版本匹配仍需新增。参考RecoverySourceEvidence使用点新鲜度样式，不沿用“观察超时=TTL” | ⑤仍第二批 |
| 恰好一个结算归属者不是现成断言 | 接受其工作性质修正；在已有owner/lease/claim及原子提交基础上**新增结算端口唯一性检查**。零类型名只是辅助，不是语义不存在的单独证明，不新造Root/SubjectKey | ⑥仍第一批 |
| 持久权威由弱引用轮询兜着的前提不成立 | 接受撤回该前提；不可变投影决定durable成员资格，弱引用/拓扑/世界索引提供物理候选；②只剩补回收观察，不能决定terminal或替换存活对象引用 | ②仍条件第四批 |

外部术语复核单列：原文“挂钟”保留，但当前代码使用`System.nanoTime()`差值。当前规划称其为同JVM经过时间新鲜度，不拿日历时间或跨重启绝对值续证。该补充来自[JDK 17官方规范](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/System.html#nanoTime())，不是声称原作者写了这个限定；同样不机械复制RecoverySourceEvidence的五秒常量。

### 天花板重审：接受量化结果，不接受从计数直接推出执行完成

| 原报告表述 | 本轮独立核对与采用范围 |
|---|---|
| 极限档请求已满 | 内置preset revision 1对54键filled(STRICT_ALL)成立；提高该内置请求是空操作。显式逐键/动作覆写、configured/active差别仍保留，不能泛化为任何工作区此刻active都为4 |
| 两个授予站点/表中三行 | 明确为全局2处+实体局部1处，共3处显式调用行；local固定5键还并入动作requiredKeys |
| 支持/通用effective永远≤2 | 只在原r66已定位生产赋值链中采用；不是对公开构造snapshot、同权限篡改、反射或后续版本的全称结论。默认OFF继续保留 |
| A执行位恒false、reason恒为深度受限 | 当前可见上限不足以满足各Step的3/4执行最低值，但reason**并不恒定**：观察未请求、未激活/失证、待重启、动作禁用等会返回其它分支 |
| 8处A与13处B | 显式调用数复算一致；**8处A均消费observationRequested，6处DIRECT_SOURCE、2处DERIVED_SOURCE**，不是8处执行许可消费者。B可表达注册范围3/4，不是实机已执行/成功证明，也不证明八种Step全覆盖 |
| A该废弃/改走B/绕过ceiling | 当前决定：保留A观察/通用职责，保留B注册执行职责；不把观察许可变写许可，不批量换函数。对选中的实际写端口接原发行者、scope/revision与结果链，缺写路径继续列C11/D工作 |
| 第10项 | 按交接整体“不新建方向”归入原DS:C13与⑨报告，不另编方向10或新批次。局部支持/证据可发布，但requested和一条布尔值不是授予证明 |
| reason计数是完整状态表 | 不采用；计数是词法素材，动态前缀/分支必须保留上下文，诊断同源而不成为批准者 |

[8个调用点邻接原行](../reference/R66-VERIFICATION-CEILING-REVIEW-EVIDENCE.md#consumer-summary)是本轮额外消费侧复核，不回写报告。真正第一批交付是已选scope的发行→发布→消费→失效→结果/诊断贯通，**不降级原广度目标，也不按ACTIVE计数冒充强度**。

<a id="review-fix8-portability"></a>
### 检查器缺陷：接受并已修脚本，不计为Mod加固

报告指出的两处输出路径、policy路径前缀过滤、groups键四处均已改为`.as_posix()`；同函数另两处build/module路径也统一，共六处。当前源码包中后两字段为空，但不应留下未来的分隔符差异。对路径列表增加按POSIX文本的稳定排序，源码文本读取显式UTF-8。没有按平台跳过检查、没有把反斜杠异常当成内容差异忽略，也没有重建/改写fix7原inventory来迁就检查器。

**基准仍为指定r66源包解压根，而不是带build/validation的工作区。** 原SHA-256：`1266d8c2dd46b949178d7341ee467333be678fb9b373c1403b576a56c73fd763`。built_manifest_files_found对基准敏感，存在额外构建文件时继续报告差异；本轮不把它排除掉。原报告“工作区选错基准”和“检查器路径缺陷”两种问题继续分开。

```powershell
python -B docs/plans/restructure/verify_addendum_merge.py --check --source-root .\src\ronova-source-0.2.0-alpha.7.17-r66
python -B docs/plans/restructure/test_addendum_portability.py --source-root .\src\ronova-source-0.2.0-alpha.7.17-r66
```

上面假定原源包已解压到`src`；`--root`可指定文档根。脚本默认只读，不需chmod、不启动JVM/游戏、不改台账。新增[可移植性回归](restructure/test_addendum_portability.py)含七项：POSIX/Windows纯路径规范、路径/编码契约、两种格式与原inventory匹配、内存中重新注入分隔符错误的负例、非空build/module字段、错基准仍拒绝。最后一项直接调用原核对函数；未改变任何来源记录。

**本轮执行环境是Linux。** Windows格式使用标准库PureWindowsPath及只读路径适配器；负例复现policy过滤为空和两项路径错配，修复版本与原26项未选policy清单一致。它验证相对路径格式/稳定排序/显式编码，不等于Windows OS、权限、解压器或原生locale的现场运行；实际Windows复跑仍待用户环境确认。七项均无错误属于文档工具回归，不生成任何Mod/阶段PASS。

### 本轮范围

生产源码、构建、资源、CURRENT-PROGRESS、validation台账和运行证据均未修改。四份输入共36,289字节、633物理行、43个分段（含来源前言）按原字节保留。旧16来源/1,047正文块/82个MS骨架标题及ID不重编号；旧同权限inventory及此前每轮来源记录保留，不为修工具修改被检材料。

台账68唯一ID、28 PARTIAL/40 NOT_RUN仍为用户此前回报，本轮未获原账本字节，不新增独立复算或PASS。120条人工关联、9条未定位直接用例和GAP-MAP-07证据侧均不因本轮融合关闭。九方向原接受3/修改5/废弃1与各批次位置不变，三份报告不另挂一套规划。


<a id="review-fix9-windows"></a>
## fix9 · fix8 Windows 原生复跑回执闭合（2026-09-15）

**性质：** 本轮输入是一次性现场确认回执，不是新报告、新提案、V1.4 或新的规划方向。fix8 [可移植性节](#review-fix8-portability)中“实际 Windows 复跑仍待用户环境确认”保留为当时历史事实；当前效力由本节更新。

[收到的原始回执](../reference/R66-FIX8-WINDOWS-RERUN-RECEIPT.md)与[原ZIP](../archive/fix8-windows-rerun-receipt-r66.zip)按输入字节保留。回执报告环境为 Windows 10 Home China build 26200.9445、PowerShell 7.6.6、Python 3.14.0，工作目录位于原生 Windows 文件系统；其所列 r66 源包 SHA-256 为 `1266d8c2dd46b949178d7341ee467333be678fb9b373c1403b576a56c73fd763`。本轮集成环境中现有 `ronova-source-0.2.0-alpha.7.17-r66.zip` 的 SHA-256 独立计算与该值一致；这只确认基准身份，不认证远端/用户机器的执行过程。

### 用户现场回报的结果

| 项 | 回执报告 |
|---|---|
| `verify_addendum_merge.py --check --source-root <源包>` | `errors: []`；52来源分段、104映射目标；`validation_modified: false` |
| `test_addendum_portability.py --source-root <源包>` | 7 / 7，0跳过，0错误；`host_platform: win32`、`native_windows_execution: true` |
| fix7同环境对照 | 原路径缺陷仍能产生 `Surface lexical inventory differs`；fix8修复后该失败消失 |
| 其余三个文档检查器 | 回执报告 `verify_restructure.py`、不变量生成器检查、验收映射生成器检查均无错误/派生一致 |
| 写入边界 | 回执声明无文件/权限写入、无子进程、无Mod JVM/游戏/验收执行；`validation/7.17/acceptance-matrix.json` 未改 |

这组结果**关闭 fix8 唯一剩余的“需要原生 Windows 现场复跑”文档工具待确认项**。同时保留三个边界：第一，当前交付环境没有再次启动 Windows 进程，以上运行结果的来源仍是用户提供的现场回执；第二，七项工具测试覆盖的是相对路径格式、排序、UTF-8、负例与错基准拒绝，不扩大成 Windows 权限模型、解压器或 locale 的完整覆盖；第三，文档工具回归不产生任何 Mod、阶段或 B/X 验收 PASS。

本轮不改能力规划、九方向决策、54键/14动作接线、生产源码、构建、资源、CURRENT-PROGRESS、原validation台账或运行证据。GAP-MAP-07 的证据绑定仍开放，120行人工语义映射的正确性也没有因本回执获得机器证明。


<a id="review-fix10-eight"></a>
## fix10 · 第二批八方向核验的融合回执

输入为Ronova侧→外部研究方的**发出核验稿**，不是新增提案。“第二批八方向”是来源称谓，不是更改当前实施批次号。原稿声明pwsh.exe启动失败，结论来自grep/read、无编译运行；本轮独立读取收到的r66源包，不复述成用户现场实测。原文与ZIP完整保留：[核验稿](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md)、[归档](../archive/second-batch-eight-directions-verification-r66.zip)。

**先修正上一轮自评：** A4已有六态ActivationState、五态EvidenceState、逐键map和解析器；A2已有构建身份和capability mask；A3已有成熟的ShadowVerification结果纪律；S3已有累积观察模板。不是从零重建这些机制。本轮八条都保留，但没有给“现有基础”“通用协议”“运行强度”画等号。

<a id="fix10-direction-receipts"></a>
### 八条按原标识处理

| 来源内标识 | 当前处理 | 真实增量与精度限定 | 原批次位置 |
|---|---|---|---|
| S1 Challenge Probe | 接受统一层，收窄“纯提取/四原语全齐” | 复用canary、自有镜像和生产验证；core targets是MC类，不等于owned dummy。所选scope仍需隔离fixture、结果绑定、超时清理和撤销。 | 第一批，和C13同源 |
| S2 Write-Set | 接受，和C13并行 | 附在原plan/lease；声明＋提交前检查＋实际/未知回执＋独立结果，不能把声明本身当ACTIVE证据，也不重造授权根。 | 第一批选中端口，第二批扩面 |
| S3 Independent Witness | 接受推广已有模板；修改“四级同构”判断 | Observation是可累积位，保留不确定性不被成功覆盖、memory-only及原格式；四层展示只是派生视图。 | 第一批所选结果，第二批扩面 |
| S4 Observation Gap | 接受推广；补读纠正“只有计数” | 已有RecoveryTaskObserver→graph.evidenceLost消费者、原因/队列诊断。补跨源序列/代际、依赖撤销与重新建基线。 | 第一批预留源合同，第二/三批扩展 |
| A1 Evidence Graph | 接受反向依赖索引 | 不复用实体/任务图充当权威；已有CompletionTracker依赖快照先保留。共享依赖失败不假装完全局部隔离，不预估DAG性能收益。 | 第二/三批，非第一批阻塞 |
| A2 ABI / Feature | 接受已有基础上扩展 | build identity＋既有CAP位保留；增加应用合同/feature版本与兼容接线，结构尺寸仅对真实二进制交换格式适用。 | 桥接变更前最小前置，第三批扩展 |
| A3 Generic Shadow | 接受模板推广，不重造结果词汇 | 保留accepted不等于matches、领域结果；新机制只读比较不发真实effect。无独立基准不能把分歧率说成误判/覆盖。 | 第三批/条件适配 |
| A4 分能力熔断 | 接受前移，修改“加1–2枚举即可” | 保持activation、evidence、freshness/reason维度；先接已有状态与实际故障/恢复闸，不机械增加enum，不承诺工时小一数量级。 | 第一批所选scope，与S3接近 |

对应八条正文在[当前规划入口](R66-CAPABILITY-WIRING-PLAN.md#eight-directions)。它们沿原标识附着到C13/C11/原生/结果职责，不登记为D*/INV*/A*/R*/ID-*/T-*新不变量。特别是“八方向A1”等不与DS加强A块、P0-A编号混用。

<a id="fix10-source-corrections"></a>
### 独立复核带来的四个关键收窄

**S3不是四级状态机。** `observations()`使用OR累积；WRITE_OBSERVED和NOT_EXECUTED可并存，WRITE_UNVERIFIED即使叠加observed仍不认可。原报告“四级同构”原句保留，但现行设计不采纳这种逐值映射。执行事实、重试失败和不确定性不是一个互斥状态变量。[源码模板](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#memory-observations)

**S4已有失证链。** 不止BootstrapTaskHooks一个计数器；RecoveryTaskObserver第173–189行会因安装失败、LOST、dropped、proof/changes异常使任务图失证，211–227行还有原因与inbox丢弃诊断。TaskExecutionGraph有EVIDENCE_LOST和依此拒绝执行退出的判断。需要推广，不应再排一遍“第一次把丢失接到撤销”。[消费者](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#task-loss-consumer)

**A2已有位掩码，A4已有消费者，但都不能过度推论。** NativeBridge合并bootstrap/CAP位，有capabilityMask访问器；构建ID校验在code-seal可选入口，Java目前不消费enable返回值，不等于全部Native协议一致。StrengthPolicySnapshot有按键activation/evidence及降级原因，但完整熔断仍要定义窗口、阈值、撤销、有限重试和并发恢复；不按新增两个enum排一个“几乎零工时”的任务。[桥接](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#bridge-build-and-mask) · [状态解析](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#policy-state-resolution)

**S1与S2的证据不要被说大。** reassert_core_targets处理MC语义目标，自有镜像另有路径；一段挑战成功只支撑该范围。写集声明是合同，不是已执行守卫的凭据；C13只消费由真实端口和结果支撑、版本仍有效的证明。方向接受不代表“纯提取”已经全部完成，也不抹去原Root/lease的已有范围防护。

### 采用的顺序与未采用的估工

采用S2与C13并行和A4前移；S3在所选动作上与它们形成第一批纵向交付，不等待所有键/出口全接完。A2涉及新桥接协议的必要前置提前，通用扩展仍后置；S4→A1/A3在原第二/三批展开。[正式批次](R66-CAPABILITY-WIRING-PLAN.md#eight-order)

不新增工程批次、最高规范或方向ID，不改变V1.3①⑦⑨接受、②③④⑤⑥修改、⑧本轮废弃的旧回执。没有新增大量MethodEntry/Exit、SingleStep、Breakpoint、全堆遍历或全量栈采样任务；“本轮不堆入口”是项目排期，不断言这些API在任何场景都没价值。原最终Extreme目标不降级为只读观察。

### 保留、核对与证据边界

原附件§0表格/汇总与聊天对A2/A3的分类不同；它们不用于重新统计“六条/两条”。原核验稿全字节保留；本轮补读放独立依据，设计选择放现行章节。脚本校验原文、分段、落点和源码摘录，**不证明人工判断、方案正确性、跨同权限安全隔离或运行性能**。

本轮没有Java/Native编译、JVM/游戏实测，不修改生产源码、构建/资源、台账或旧运行证据。fix9 Windows现场回执继续只证明当时工具版本；本轮扩展的核对器在当前Linux环境复跑，未冒称Windows再次运行。原68项台账仍只记录用户既有的28 PARTIAL/40 NOT_RUN回报；不产生PASS，GAP-MAP-07证据侧与原120行人工语义审阅仍开放。


<a id="review-fix11-baseline"></a>
## fix11 · 八方向已有实现补遗融合（2026-09-15）

**性质：补准已有基线，不重审方向、不新增工程批次。** 收到的[Ronova侧发出补遗](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md)共 **19,795 字节／263 物理行／19 分段**，连同[原ZIP](../archive/eight-directions-existing-baseline-supplement-r66.zip)按原字节保留。它不是外部新提案、不是fix10决策的替换稿。[逐节映射](RESTRUCTURE-MAP.md#fix11-baseline-merge) · [当前规划落点](R66-CAPABILITY-WIRING-PLAN.md#eight-existing-baseline)。

### 八方向采用回执：原决定和批次全部不变

| 方向 | 补遗提供的已有实现 | 融合后的真正增量／采用边界 |
|---|---|---|
| S1 | `RonovaTransformer`、bridge、probe及域合同 | 真实变换载体列为先读；统一挑战/获证层继续沿原第一批，不重写转换器。 |
| S2 | `WriteSinkId/Evidence/Extension`，NBT实际遍历、sequence及插桩 | 明确指定接缝并复用受支持NBT actual侧；本轮还查到`writtenPositionsMatch`已有实际/编码路径比对。所缺仍是声明式允许集合、出口约束及超界/未知传播，不是再建遍历。 |
| S3 | `CompletionContractId/Extension/Evaluation`、独立读回、`DeferredReceiptBuffer` | 指定完成接缝和“只重试回执”纪律；评价消费原观察且不自授Truth；non-CLEARED与原累积未知语义不被抹平。 |
| S4 | `TaskEventRun`及既有失证链 | fix10已经采用run压缩；补精确不跨断裂契约，跨源代际/依赖传播仍为增量。 |
| A1 | `CopyProvenanceIndex`及bySource、coverageLoss | 补现成反向索引和局部失证消费者；通用grant/proof/capability依赖图继续待接，不用实体/任务/复制图冒充。 |
| A2 | build identity、capabilityMask及未消费的enable结果 | 原协议扩展不变；把已有返回路径写入协商诊断，不称“完全没有feature基础”。 |
| A3 | `ShadowVerification`十态及三谓词 | 复用语义和适配模板，不强套持久根专用enum到所有表面。 |
| A4 | 唯一模块维护owner、隔离修复、计数诊断与FIFO重试 | 在现有服务端模块宿主接熔断，区分contract.Capability与54个强度键；跨域按原owner适配。有限false重试不等于任意异常都有界，正常返回不必然是成功。 |

### 与原稿分开记录的限定

**观察不授权继续有效，但不另立铁律。** 补遗§0的“新增授权语义归policy/Root、免责声明原样保留、授权引用观察结果”作为当前分层约定采用。S3是完成评价、A4是失证/熔断接线，不把两者改成新的许可发行者；观察包已有消费Root许可的适配也不应被误判为自行授予权限。原三条铁律与不起外部进程仍是此前规定的限制集合。

**具名接缝不等于生产就绪。** 两个Extension当前是版本codec接口，`begins in 7.17`说明扩展意图；本轮限定生产Java词法引用见[扫描](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#seam-references)。已存在抽象应复用，但构造Evidence/Evaluation对象或hash非空都不能替代真实发行、原Root计划及最终端口复核。

**已有actual侧不可扩大成全表面覆盖。** NBT快照受节点/深度/条目等限制，弱引用可失效；`complete=false`不能当空集合。现有保存位置比对需要保留，也不等于操作授权或持久完成。其他实体/追踪/Native表面的actual侧仍须按选中出口接入。

**宿主不是所有能力键的统一执行器。** `CapabilityRuntime`的标准表有六个repair模块，使用另一套稳定`contract.Capability`位。Native等功能不能机械路由到player.repair；映射和各域原owner需明确。`BoundedRetryQueue`在`engine.spawn`包内且异常分支不累计false重试次数，不能仅凭标题判定完整熔断已实现；`CapabilityContext.attempt`内部吞异常并计数，恢复闸不能只看外层正常返回。

这些限定来自[本轮原源码补读](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#findings)，不是暗改补遗。作者“约28个免责声明”保留为作者人工下界；本轮机械枚举不冒称穷举语义统计。原S1–S4/A1–A4、V1.3九项回执、54键、14动作和68项验收关联不重编号、不改状态。

### 工具和记录范围

沿用固定`current-verification.json`，不增一套link-audit-fix11或verification-fix11历史副本。原来源ZIP、原文、旧manifest handoff分组和原文快照保持；新分组`existing_baseline_supplement_handoffs`保留本轮来源/分段/摘录/子系统目录清单。核对器检查的是字节、位置、来源和派生一致性，不证明语义完整或工程工量。

本轮没有编译、启动JVM/游戏、修改生产源码、修改validation或新增验收PASS；Linux文档工具核对不冒充Windows现场复跑。fix9的Windows回执仍只适用于其所列工具版本。原120行人工语义关系和GAP-MAP-07运行证据绑定继续开放。


<a id="review-fix12-acceptance"></a>
## fix12 · 选择③：把施工与验收接起来

输入：完整fix11及本轮用户评审消息。用户明确指出施工批次/八方向没有验收桥，倾向原观察点作为新工作口径。本轮采用③，作为显式项目决定，而非冒充原文已有安排。

本地复核：fix11观察表实际为**19行**（含末行“V1.3 新增观察”）；54键表有39行通用逐键清点待办。两点均保留事实，不按18行删减，也不重写39行成已完成。原B/X与新增方向可能共享部分语义和测试，故不采用“来源不同即结构上完全无法覆盖”的绝对判断；不强贴新方向到旧用例。

落实在[验收分工](WIRING-ACCEPTANCE.md#decision)：原表19行获得新增工作的正式判据效力；批次和S1–S4/A1–A4有适用绑定，另给原B/X受影响回归线索、scope级完成规则及未填写的证据记录模板。原批次和方向未增加，原表行文未变，只添描述性锚点。

[原台账索引](VALIDATION-TRACEABILITY.md#acceptance-scope)由生成器加入反向入口，不手改生成结果；原JSON的68场景、120行候选关联、用户28/40回报保持原字节。选择③不免除原B/X回归，也不取消GAP-MAP-07的证据侧。

这是规范/分工层收口；实际artifact、fixture、断言、版本、scope、客户端观察仍待代码批次后绑定。本轮不创建空台账、不修改validation、源码或原进度，不声称Mod/方向/阶段PASS；39键待清点也未在本轮完成。三本MS主书、原ID与来源档案未修改。


<a id="review-code-round-1-closure"></a>
## 代码第 1 轮 · 分工改为「我写代码、外部智能体审代码」

输入：用户决定 `你改代码，我给他审查行不`。据此把此前「外部智能体改、我审」的方向反转为**我改代码并本机验证、外部智能体只做静态审查**。依据：外部环境无法运行 Gradle（`./gradlew` → 权限拒绝、退出码 126，且无审批通道），而审查是只读作业，其环境足够；本轮同时确认其静态分析有效（上一轮正是它给出 `submissions<=entries` 的反例，推翻了我的补丁）。

**本机验证能力已恢复并实测**：命令执行器不再报 `CreateProcessAsUserW failed` / `pwsh.exe ENOENT`。显式指定 `JAVA_HOME=D:\桌面\MOD\.build-tools\jdk17\jdk-17.0.15+6`、`GRADLE_USER_HOME=D:\桌面\MOD\.gradle-cache` 后，`compileJava compileTestJava test selfTest` 一轮约 **1 分 30 秒**。这推翻了 `DEEPSEEK-CHANGELOG.md` 原有的「未编译、未运行任何测试」状态。

**该更正未写回 `DEEPSEEK-CHANGELOG.md`，因为它是重构核对器的冻结输入。** 该文件登记在 `docs/plans/restructure/input-file-hashes.json` 内且**不属于** `source-manifest.json` 的导航来源，故 `verify_restructure.py` 判定任何改动为 `Undeclared input modifications`。我一度就地更正，核对器随即报错（exit 1）；随后**逐字节还原**并复核：文件 SHA-256 与申报基线 `dc4aaea5…` 一致，`verify_restructure.py` 恢复 exit 0 / `errors: []`。因此该轮的过期状态与被证伪的 `:31` 陈述，一律在本回执与送审件中更正，冻结输入保持原样。`verify_addendum_merge.py` 亦为 exit 0。

**本轮定位并修复缺陷 D-08（我自己 09-11 那轮引入）**：`RecoveryTaskDisposition` 的「正常退出分支」后继闭合从未生效。`flush()` 会 `iterator.remove()` 已落盘的 fact，紧随其后的 `contains(EXECUTION_EXITED)` 守卫在正常路径恒为 false，只在**落盘失败**时才放行；该轮 changelog 却声称「取消分支与正常退出分支共用」（`:22`）与「正常退出分支：`EXECUTION_EXITED` 已写入时评估」（`:31`）。改为**闩锁式义务**（退出记欠、每轮尝试清偿、store 收下方清闩锁），并把 `recordSuccessorsClosed` 改为返回布尔。

**该缺陷至今沉默的原因已查明并记为事实**：`SUCCESSORS_CLOSED` 在 `src/main` 只有一个生产点且**零消费者**，方向为 fail-closed（欠报）；`src/test` 对 `RecoveryTaskDisposition` **零引用**，而 `RecoveryWorkScenarios:83` 是测试自己把该 fact 注入 store，**绕开了生产者**。即：现有套件结构上无法发现它。本轮**未补**回归测试，此点已在送审件中列为最重要未决并请对方判定是否值得投入一轮搭夹具。

**送审件**：[R66-RECOVERY-TASK-CLOSURE-CHANGE-REVIEW.md](../reference/R66-RECOVERY-TASK-CLOSURE-CHANGE-REVIEW.md)，含完整 diff、机理、排除的候选（3 项已查证非缺陷）、验证回执与**我自陈的 6 个不确定点**。基线偏离面经**全量 618 条目逐条 SHA-256 比对**确认为 613 相同 / 5 改动 / 0 缺失，5 个改动文件全部落在块 3 纵切片内。

**本轮边界**：改了生产源码（1 文件）并跑了本机套件；未启动游戏、未跑隔离专用服、未做实机验证；未修改 `validation/`、未回填任何 PASS、未覆盖历史失败（`validation/7.17/phase1-task-disposition/round-4.json` 原样保留）；未声称任何验收项转 PASS。`RESOURCES_RELEASED` 仍完全未接线；`X63/X64/X65` 与 `RECOVERY_TASKS` 真实证据仍未闭合；GAP-MAP-07 与 39 键清点继续开放。
