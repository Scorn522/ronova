# Ronova 规划条款 ↔ B/X 验收规格对照

当前入口：2026-09-15 · fix12；原映射基线为fix3。**这是来源可追溯的设计映射，不是新验收台账，不是覆盖率报告。**

[规划入口](README.md) · [不变量索引](INVARIANT-INDEX.md) · [缺口映射](RESTRUCTURE-MAP.md#gap-map-07) · [本轮审阅说明](RESTRUCTURE-REVIEW.md#review-fix3) · [记录效力与当前核对](restructure/README.md) · [映射数据](restructure/validation-traceability.json)

<a id="acceptance-scope"></a>
## 当前验收分工（fix12显式决策）

**原68项只保留原需求及受影响回归职责；新增接线按[正式观察判据与绑定](WIRING-ACCEPTANCE.md#decision)验收。** 原[施工批次](R66-CAPABILITY-WIRING-PLAN.md#batches)、[19行判据原文](R66-CAPABILITY-WIRING-PLAN.md#validation)与[八方向绑定](WIRING-ACCEPTANCE.md#direction-binding)已互链；不扩编B/X，不把新增方向强贴到旧编号。

这不是两边互相豁免：实际改动影响原要求时仍需原用例回归；同一证据允许分别绑定两边断言，状态不能互相代填。旧场景可能覆盖新工作的部分交界，但不能据来源/名称推定全部覆盖或全部无关。批次绑定是新的项目分工，不改变下方120行原人工映射与68项场景。

当前仅决定验收口径，未提交新增运行证据；台账集合/汇总的用户回报和GAP-MAP-07证据侧未完成事实保持。没有新增PASS，也没有将未选中/未实现改成N/A。

<a id="input-boundary"></a>
## 输入边界与状态读法

**当前事实（用户工作区核查回报）：** `validation/7.17/acceptance-matrix.json` 存在于用户完整工作区。用户已按 `/cases`、`id`、`status` 只读运行生成器，报告 68 个唯一 B/X ID、0 缺失、0 额外、0 重复，状态汇总为 28 PARTIAL / 40 NOT_RUN；并报告未写入 `validation/`。台账 ID 集合和状态汇总对接子项据该回报记为已核对，**不等于运行证据已绑定**。原命令、结果摘录与来源边界见[台账核查回报](RESTRUCTURE-REVIEW.md#review-fix3-ledger)。

**fix2 的历史输入边界：** 当时收到的原 r66 文档包、MS 重构 fix1 文档包及源码包没有随包提供该文件。这个归档事实保留，**不得推广为项目或完整工作区没有账本**。本轮仍未收到原账本字节、SHA-256 或 68 行明细，不声称在此环境独立重跑用户的台账核查。

[原进度](../CURRENT-PROGRESS.md#验收与下一步)记载 **28 PARTIAL / 40 NOT_RUN / 0 完整 PASS**；用户回报的状态计数与此前汇总一致。**本静态索引不收录逐项状态**，不从 28/40 推算哪个 B/X 是 PARTIAL 或 NOT_RUN，不把“未收录”写成新的验收状态。68 行原状态仍以适用工作区的真实账本为准。

源规格、原 ID、`validation/`、原进度与历史证据不修改。本交付未包含的账本/附件不会被创建为空台账或假 fixture。恢复链正例、文档脚本成功、ID 对齐和局部结果都不能据此升级整项验收。

本表的 **68 项**仅指 V26 验收编号；待采纳快照中的同号研究模块、DS/RULE 的加强编号和 L6 决策编号均不是同一命名空间。用完整来源与章节限定身份。

<a id="relation-legend"></a>
## 关联强度：映射不等于证明

| 标记 | 含义 |
|---|---|
| 原文明引 | B/X 原句直接提到该原 ID；只说明引用关系，仍不代表已覆盖或通过。 |
| 设计子范围 | 本轮根据两端原文建立的可审阅关联；未取得原台账对该映射的声明，仍需 fixture/断言/证据确认。 |
| 仅邻近/逆向，非覆盖 | 可用于找实现入口，但语义方向或对象范围不同；不能当作覆盖边。 |
| 未定位直接项 | 不强贴 B/X；保留尚需明确的子场景/结构/阶段核查。 |

**这 120 行及其关联是人工设计判断。** `--check` 核对摘录、编号、关系集合及派生内容一致性，不能证明关联正确或覆盖完整；用户回报的台账 ID/计数一致也不能代替这项语义审阅。优先核查[未定位直接用例的 9 行](#manual-review-queue)，其余有链接的行同样不自动视为审阅通过。

**尤其：ERASED 目标禁止重插 ≠ PROTECTED 实体缺表后的补回；Root 原子事务 ≠ ID/UUID 注册表原子修复；Root receipt ≠ 任意客户端像素已达标。** 下方每个 C/GAP 行写明剩余差距。

原文明引只有下列已核对的引用边；其他边即使语义接近，也统一按本轮设计关联阅读：

| 原 ID（限定来源） | 原 B/X | 原句证据 |
|---|---|---|
| [L6:decisions:D1](#requirement-l6-decisions-d1) | [B21](#case-b21) | `L6 Master D1/D5` |
| [L6:decisions:D5](#requirement-l6-decisions-d5) | [B21](#case-b21) | `L6 Master D1/D5` |
| [L6:decisions:D5](#requirement-l6-decisions-d5) | [B20](#case-b20) | `L6 Master D5` |
| [L6:decisions:D13](#requirement-l6-decisions-d13) | [B23](#case-b23) | `L6 Master D13` |

<a id="scope-cautions"></a>
## 两处旧前提的当前效力

| 用例 | 保留与限制 |
|---|---|
| [B23](#case-b23) | 原 helper attach/relaunch 正例不适用当前[不起外部进程](README.md#scope-process)边界；原文和 ID 保留，当前 JVM 唯一 writer 子断言保留。需另取得新入口/失败 fallback/实际进程树证据，不能沿旧正例启动进程，也不能把整项直接抹成 N/A。 |
| [X66](#case-x66) | 原五级规范顺序前提按[当前文档治理](README.md#scope-governance)失效；自包含编号、缺失历史透明登记、状态真实性要求仍保留。本轮索引脚本不回填该项状态。 |

DS 的 P 层 C 项仍按 [fix1 定级](README.md#scope-p-layer)阅读；“无参数”不是额外硬限制，前置权限/初始化条件并未随之消失。其余原规格不在本轮改写。

<a id="case-directory"></a>
## 68 项验收规格：正向入口

“场景 / 原题”来自原文；完整断言在新书唯一正文落点。右列只是关联导航。本静态表未收录逐项状态；用户的台账集合核查另见输入边界，不把正向列表完整误读成 68 项已覆盖。

### B01–B23：基础验收

| 原编号 / 状态呈现范围 | 场景 / 原题 | 新书原规格与来源 | 条款与 MS 检查导航（非覆盖声明） |
|---|---|---|---|
| <a id="case-b01"></a>**B01**<br>逐项状态未收录 | 同一当前目标分别执行改血与终结；只调用执行器但不产生效果 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2901–L2901` | [V26:invariants:D74](#requirement-v26-invariants-d74) · [DS:C-block:C11](#requirement-ds-c-block-c11) · [RULE:C-block:C11](#requirement-rule-c-block-c11) · [RULE:D-block:D1](#requirement-rule-d-block-d1) · [RULE:D-block:D2](#requirement-rule-d-block-d2) · [RULE:D-block:D3](#requirement-rule-d-block-d3) · [GAP-DOC-01](#requirement-gap-doc-01) · [GAP-MAP-01](#requirement-gap-map-01) · [GAP-MAP-07](#requirement-gap-map-07) · [skills:L47](#ms-check-skills-l47) · [skills:L48](#ms-check-skills-l48) · [skills:L49](#ms-check-skills-l49) · [msitem:L378](#ms-check-msitem-l378) |
| <a id="case-b02"></a>**B02**<br>逐项状态未收录 | 原版移除后让实体重新加入 ID/UUID/section/tick 索引 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2902–L2902` | [L6:decisions:D1](#requirement-l6-decisions-d1) · [DS:C-block:C9](#requirement-ds-c-block-c9) · [RULE:C-block:C9](#requirement-rule-c-block-c9) · [GAP-MAP-02](#requirement-gap-map-02) · [GAP-MAP-03](#requirement-gap-map-03) · [GAP-MAP-04](#requirement-gap-map-04) · [GAP-MAP-05](#requirement-gap-map-05) · [GAP-MAP-07](#requirement-gap-map-07) · [skills:L52](#ms-check-skills-l52) · [skills:L53](#ms-check-skills-l53) · [skills:L54](#ms-check-skills-l54) · [msitem:L385](#ms-check-msitem-l385) · [entitydefence:L288](#ms-check-entitydefence-l288) · [entitydefence:L289](#ms-check-entitydefence-l289) · [entitydefence:L290](#ms-check-entitydefence-l290) · [entitydefence:L291](#ms-check-entitydefence-l291) · [entitydefence:L295](#ms-check-entitydefence-l295) |
| <a id="case-b03"></a>**B03**<br>逐项状态未收录 | 同 UUID、换 UUID、换对象分别重建；另给一个不相关对象 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2903–L2903` | [DS:C-block:C9](#requirement-ds-c-block-c9) · [RULE:C-block:C9](#requirement-rule-c-block-c9) · [GAP-DOC-02](#requirement-gap-doc-02) · [GAP-MAP-02](#requirement-gap-map-02) · [msitem:L380](#ms-check-msitem-l380) · [msitem:L381](#ms-check-msitem-l381) · [entitydefence:L298](#ms-check-entitydefence-l298) |
| <a id="case-b04"></a>**B04**<br>逐项状态未收录 | 对同一已证明关键方法分别施加空返回、固定返回、正常返回 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2904–L2904` | [V26:invariants:D99](#requirement-v26-invariants-d99) |
| <a id="case-b05"></a>**B05**<br>逐项状态未收录 | 保持字节码摘要不变但改变已登记 dispatch 目标 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2905–L2905` | [DS:C-block:C5](#requirement-ds-c-block-c5) · [DS:C-block:C17](#requirement-ds-c-block-c17) · [RULE:C-block:C5](#requirement-rule-c-block-c5) · [RULE:C-block:C17](#requirement-rule-c-block-c17) · [msitem:L388](#ms-check-msitem-l388) |
| <a id="case-b06"></a>**B06**<br>逐项状态未收录 | 对 Ronova 关键方法施加全返回且让攻击全返回保持关闭 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2906–L2906` | [DS:C-block:C7](#requirement-ds-c-block-c7) · [RULE:C-block:C7](#requirement-rule-c-block-c7) · [entitydefence:L294](#ms-check-entitydefence-l294) |
| <a id="case-b07"></a>**B07**<br>逐项状态未收录 | 延迟 Future/task 在旧 epoch 被撤销后仍试图写入 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2907–L2907` | [msitem:L381](#ms-check-msitem-l381) |
| <a id="case-b08"></a>**B08**<br>逐项状态未收录 | callback/listener 被移除后又由 manager 重新注册 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2908–L2908` | [V26:invariants:D75](#requirement-v26-invariants-d75) |
| <a id="case-b09"></a>**B09**<br>逐项状态未收录 | 重放旧包、不同 session 包及相同 commandId 的冲突 payload | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2909–L2909` | [L6:decisions:D4](#requirement-l6-decisions-d4) · [V26:invariants:D93](#requirement-v26-invariants-d93) |
| <a id="case-b10"></a>**B10**<br>逐项状态未收录 | 保存后重启、旧 NBT 重建、损坏 Realm 及迁移身份冲突 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2910–L2910` | [L6:decisions:D2](#requirement-l6-decisions-d2) · [DS:C-block:C14](#requirement-ds-c-block-c14) · [RULE:C-block:C12](#requirement-rule-c-block-c12) · [entitydefence:L298](#ms-check-entitydefence-l298) |
| <a id="case-b11"></a>**B11**<br>逐项状态未收录 | 同 shared manager 有目标相关边和无关边 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2911–L2911` | [DS:C-block:C16](#requirement-ds-c-block-c16) · [DS:C-block:C18](#requirement-ds-c-block-c18) · [RULE:C-block:C16](#requirement-rule-c-block-c16) · [RULE:C-block:C18](#requirement-rule-c-block-c18) · [msitem:L382](#ms-check-msitem-l382) |
| <a id="case-b12"></a>**B12**<br>逐项状态未收录 | 独立 HUD/非实体 registry 继续发布目标，实体已经消失 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2912–L2912` | [RULE:D-block:D2](#requirement-rule-d-block-d2) · [GAP-MAP-04](#requirement-gap-map-04) · [GAP-MAP-07](#requirement-gap-map-07) · [skills:L49](#ms-check-skills-l49) · [skills:L54](#ms-check-skills-l54) · [msitem:L385](#ms-check-msitem-l385) · [entitydefence:L293](#ms-check-entitydefence-l293) |
| <a id="case-b13"></a>**B13**<br>逐项状态未收录 | 父类/helper/proxy/不同 loader 产生同形类；另有无关同名类 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2913–L2913` | [DS:C-block:C16](#requirement-ds-c-block-c16) · [DS:C-block:C18](#requirement-ds-c-block-c18) · [RULE:C-block:C13](#requirement-rule-c-block-c13) · [RULE:C-block:C16](#requirement-rule-c-block-c16) · [RULE:C-block:C18](#requirement-rule-c-block-c18) |
| <a id="case-b14"></a>**B14**<br>逐项状态未收录 | runtime compiler/动态补丁重新生成已处理的方法 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2914–L2914` | [msitem:L388](#ms-check-msitem-l388) |
| <a id="case-b15"></a>**B15**<br>逐项状态未收录 | 同 key 的 class/resource 被替换，或 baseline 与运行时分歧 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2915–L2915` | [DS:C-block:C5](#requirement-ds-c-block-c5) · [DS:C-block:C10](#requirement-ds-c-block-c10) · [DS:C-block:C17](#requirement-ds-c-block-c17) · [RULE:C-block:C5](#requirement-rule-c-block-c5) · [RULE:C-block:C10](#requirement-rule-c-block-c10) · [RULE:C-block:C13](#requirement-rule-c-block-c13) · [RULE:C-block:C17](#requirement-rule-c-block-c17) · [msitem:L388](#ms-check-msitem-l388) |
| <a id="case-b16"></a>**B16**<br>逐项状态未收录 | Native/JVMTI 不可用、未观察路径和 Java fallback 分别出现 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2916–L2916` | [V26:invariants:D94](#requirement-v26-invariants-d94) · [DS:C-block:C3](#requirement-ds-c-block-c3) · [DS:C-block:C7](#requirement-ds-c-block-c7) · [DS:C-block:C10](#requirement-ds-c-block-c10) · [DS:C-block:C13](#requirement-ds-c-block-c13) · [RULE:C-block:C3](#requirement-rule-c-block-c3) · [RULE:C-block:C7](#requirement-rule-c-block-c7) · [RULE:C-block:C10](#requirement-rule-c-block-c10) · [GAP-MAP-07](#requirement-gap-map-07) · [msitem:L388](#ms-check-msitem-l388) |
| <a id="case-b17"></a>**B17**<br>逐项状态未收录 | lease 签发后更换政策、binding、loader 或 sink revision | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2917–L2917` | [V26:invariants:D33](#requirement-v26-invariants-d33) · [msitem:L381](#ms-check-msitem-l381) |
| <a id="case-b18"></a>**B18**<br>逐项状态未收录 | 相同输入重发；Root 已写入但 ACK 丢失 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2918–L2918` | [L6:decisions:D7](#requirement-l6-decisions-d7) · [DS:C-block:C11](#requirement-ds-c-block-c11) · [RULE:C-block:C11](#requirement-rule-c-block-c11) · [RULE:D-block:D1](#requirement-rule-d-block-d1) · [RULE:D-block:D4](#requirement-rule-d-block-d4) · [GAP-DOC-01](#requirement-gap-doc-01) · [GAP-DOC-03](#requirement-gap-doc-03) · [msitem:L378](#ms-check-msitem-l378) · [msitem:L379](#ms-check-msitem-l379) |
| <a id="case-b19"></a>**B19**<br>逐项状态未收录 | 一个多目标动作中一目标并发变更 expected revision | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2919–L2919` | [L6:decisions:D7](#requirement-l6-decisions-d7) · [L6:decisions:D9](#requirement-l6-decisions-d9) · [DS:C-block:C19](#requirement-ds-c-block-c19) · [GAP-DOC-03](#requirement-gap-doc-03) · [GAP-MAP-03](#requirement-gap-map-03) |
| <a id="case-b20"></a>**B20**<br>逐项状态未收录 | ONCE 死亡/掉落 claim 与 REPEATABLE 状态断言重复执行 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2920–L2920` | [L6:decisions:D5](#requirement-l6-decisions-d5) · [DS:C-block:C11](#requirement-ds-c-block-c11) · [RULE:C-block:C11](#requirement-rule-c-block-c11) · [RULE:D-block:D1](#requirement-rule-d-block-d1) · [RULE:D-block:D3](#requirement-rule-d-block-d3) · [RULE:D-block:D4](#requirement-rule-d-block-d4) · [GAP-DOC-01](#requirement-gap-doc-01) · [GAP-MAP-01](#requirement-gap-map-01) · [msitem:L378](#ms-check-msitem-l378) · [msitem:L379](#ms-check-msitem-l379) · [msitem:L384](#ms-check-msitem-l384) |
| <a id="case-b21"></a>**B21**<br>逐项状态未收录 | Quarantine/Presence 屏蔽目标后再观察实体 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2921–L2921` | [L6:decisions:D1](#requirement-l6-decisions-d1) · [L6:decisions:D5](#requirement-l6-decisions-d5) · [DS:C-block:C8](#requirement-ds-c-block-c8) · [RULE:C-block:C8](#requirement-rule-c-block-c8) · [GAP-MAP-05](#requirement-gap-map-05) · [GAP-MAP-06](#requirement-gap-map-06) · [GAP-MAP-07](#requirement-gap-map-07) · [skills:L52](#ms-check-skills-l52) · [skills:L53](#ms-check-skills-l53) · [entitydefence:L295](#ms-check-entitydefence-l295) · [entitydefence:L296](#ms-check-entitydefence-l296) |
| <a id="case-b22"></a>**B22**<br>逐项状态未收录 | 受保护主体与可攻击载体建立关联，再攻击载体 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2922–L2922` | [DS:C-block:C11](#requirement-ds-c-block-c11) · [RULE:C-block:C11](#requirement-rule-c-block-c11) · [RULE:D-block:D1](#requirement-rule-d-block-d1) · [GAP-DOC-02](#requirement-gap-doc-02) · [GAP-DOC-03](#requirement-gap-doc-03) · [msitem:L380](#ms-check-msitem-l380) · [msitem:L382](#ms-check-msitem-l382) |
| <a id="case-b23"></a>**B23**<br>逐项状态未收录<br>**旧前提见上节** | bounded helper 完成 attach/relaunch；同时观察 writer 身份 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2895)<br>`V26:L2923–L2923` | [L6:decisions:D3](#requirement-l6-decisions-d3) · [L6:decisions:D13](#requirement-l6-decisions-d13) · [V26:invariants:D97](#requirement-v26-invariants-d97) · [DS:C-block:C1](#requirement-ds-c-block-c1) · [DS:C-block:C15](#requirement-ds-c-block-c15) · [RULE:C-block:C1](#requirement-rule-c-block-c1) · [RULE:C-block:C15](#requirement-rule-c-block-c15) |

### X24–X68：增量验收

| 原编号 / 状态呈现范围 | 场景 / 原题 | 新书原规格与来源 | 条款与 MS 检查导航（非覆盖声明） |
|---|---|---|---|
| <a id="case-x24"></a>**X24**<br>逐项状态未收录 | X24 — Candidate Default-in | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2927)<br>`V26:L2927–L2935` | [V26:invariants:D53](#requirement-v26-invariants-d53) |
| <a id="case-x25"></a>**X25**<br>逐项状态未收录 | X25 — Proof Controls Severity | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2936)<br>`V26:L2936–L2953` | [V26:invariants:D54](#requirement-v26-invariants-d54) · [DS:C-block:C16](#requirement-ds-c-block-c16) · [RULE:C-block:C16](#requirement-rule-c-block-c16) |
| <a id="case-x26"></a>**X26**<br>逐项状态未收录 | X26 — Fence Scope | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2954)<br>`V26:L2954–L2969` | [V26:invariants:D33](#requirement-v26-invariants-d33) · [V26:invariants:D56](#requirement-v26-invariants-d56) · [V26:invariants:D57](#requirement-v26-invariants-d57) |
| <a id="case-x27"></a>**X27**<br>逐项状态未收录 | X27 — Legal Reactivation | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2970)<br>`V26:L2970–L2980` | [V26:invariants:D58](#requirement-v26-invariants-d58) · [V26:invariants:D59](#requirement-v26-invariants-d59) |
| <a id="case-x28"></a>**X28**<br>逐项状态未收录 | X28 — Budget Persistence | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2981)<br>`V26:L2981–L2991` | [V26:invariants:D55](#requirement-v26-invariants-d55) |
| <a id="case-x29"></a>**X29**<br>逐项状态未收录 | X29 — Multi-opponent Scale | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l2992)<br>`V26:L2992–L3000` | [V26:invariants:D62](#requirement-v26-invariants-d62) |
| <a id="case-x30"></a>**X30**<br>逐项状态未收录 | X30 — Host Safety | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3001)<br>`V26:L3001–L3012` | [V26:invariants:D63](#requirement-v26-invariants-d63) · [V26:invariants:D97](#requirement-v26-invariants-d97) · [DS:C-block:C6](#requirement-ds-c-block-c6) · [RULE:C-block:C6](#requirement-rule-c-block-c6) |
| <a id="case-x31"></a>**X31**<br>逐项状态未收录 | 分别输入一条有效强证据、两条独立弱证据、单条弱证据，以及合法合规 sink 写入 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3036)<br>`V26:L3040–L3040` | [V26:invariants:D53](#requirement-v26-invariants-d53) · [V26:invariants:D66](#requirement-v26-invariants-d66) |
| <a id="case-x32"></a>**X32**<br>逐项状态未收录 | 同一事件被两个 listener 记录、日志转发、同源派生字段、过期/跨 epoch 证据 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3036)<br>`V26:L3041–L3041` | [V26:invariants:D68](#requirement-v26-invariants-d68) |
| <a id="case-x33"></a>**X33**<br>逐项状态未收录 | Candidate 分数很高或长期未决，试图改写共享类；随后仅一项原因证明无关 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3036)<br>`V26:L3042–L3042` | [V26:invariants:D67](#requirement-v26-invariants-d67) |
| <a id="case-x34"></a>**X34**<br>逐项状态未收录 | 同一语义重复提交 100000 次并继续增长 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3036)<br>`V26:L3043–L3043` | [V26:invariants:D69](#requirement-v26-invariants-d69) · [V26:invariants:D70](#requirement-v26-invariants-d70) |
| <a id="case-x35"></a>**X35**<br>逐项状态未收录 | 持续轮换 sourceIdentity/task/callback/hidden class/lineage，并填满细节表和汇总表 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3036)<br>`V26:L3044–L3044` | [V26:invariants:D55](#requirement-v26-invariants-d55) · [V26:invariants:D69](#requirement-v26-invariants-d69) · [V26:invariants:D70](#requirement-v26-invariants-d70) · [V26:invariants:D72](#requirement-v26-invariants-d72) |
| <a id="case-x36"></a>**X36**<br>逐项状态未收录 | 细节向汇总交接时并发提交、容量耗尽或交接中断 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3036)<br>`V26:L3045–L3045` | [V26:invariants:D71](#requirement-v26-invariants-d71) |
| <a id="case-x37"></a>**X37**<br>逐项状态未收录 | LRU/TTL、原始缓存覆盖、样本过期、低压力、计数变化或 digest 相同 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3036)<br>`V26:L3046–L3046` | [V26:invariants:D71](#requirement-v26-invariants-d71) · [V26:invariants:D72](#requirement-v26-invariants-d72) |
| <a id="case-x38"></a>**X38**<br>逐项状态未收录 | Ronova 已停止授权，但敌方 manager 每 20 tick 继续重建实体 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3036)<br>`V26:L3047–L3047` | [V26:invariants:D64](#requirement-v26-invariants-d64) · [V26:invariants:D73](#requirement-v26-invariants-d73) · [GAP-MAP-01](#requirement-gap-map-01) |
| <a id="case-x39"></a>**X39**<br>逐项状态未收录 | 曾经 CLEARED，随后新恢复边、真实漂移或必要证据失效 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3036)<br>`V26:L3048–L3048` | [V26:invariants:D61](#requirement-v26-invariants-d61) · [V26:invariants:D75](#requirement-v26-invariants-d75) |
| <a id="case-x40"></a>**X40**<br>逐项状态未收录 | 仅调用执行器、确认局部效果、确定执行失败、以及 COMMIT_UNKNOWN 各自出现 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3036)<br>`V26:L3049–L3049` | [L6:decisions:D7](#requirement-l6-decisions-d7) · [V26:invariants:D73](#requirement-v26-invariants-d73) · [V26:invariants:D74](#requirement-v26-invariants-d74) · [RULE:D-block:D2](#requirement-rule-d-block-d2) |
| <a id="case-x41"></a>**X41**<br>逐项状态未收录 | 高频伪候选、重复高严重度自报、重复证据刷新、轮换身份抢配额 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3036)<br>`V26:L3050–L3050` | [V26:invariants:D76](#requirement-v26-invariants-d76) |
| <a id="case-x42"></a>**X42**<br>逐项状态未收录 | 多目标、持续高优先输入与低置信度未决范围共存 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3036)<br>`V26:L3051–L3051` | [V26:invariants:D77](#requirement-v26-invariants-d77) |
| <a id="case-x43"></a>**X43**<br>逐项状态未收录 | 同来源但不同目标、结果出口、policy/epoch 上下文被请求合并；仅一个代表样本证明无关 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3036)<br>`V26:L3052–L3052` | [V26:invariants:D70](#requirement-v26-invariants-d70) |
| <a id="case-x44"></a>**X44**<br>逐项状态未收录 | 压缩或结果更新期间崩溃，重启时旧证书存在但缺口摘要不完整 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3036)<br>`V26:L3053–L3053` | [V26:invariants:D75](#requirement-v26-invariants-d75) |
| <a id="case-x45"></a>**X45**<br>逐项状态未收录 | 对终结、合法重新激活、重新绑定和解除裁决分别在 qualification transaction 的 prepare、durable write、receipt、projection publish 前后注入崩溃；同时让旧 epoch task 在 Judgment transition 附近并发提交，并模拟 COMMIT_UNKNOWN 重放 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3057)<br>`V26:L3061–L3061` | [V26:invariants:D58](#requirement-v26-invariants-d58) · [V26:invariants:D79](#requirement-v26-invariants-d79) · [V26:invariants:D80](#requirement-v26-invariants-d80) |
| <a id="case-x46"></a>**X46**<br>逐项状态未收录 | 分别及组合施压 heap、direct/mapped buffer、metaspace、code cache、线程/栈、class/loader、JNI/JNA/JVMTI allocation、文件/平台句柄与可观测 GPU/render buffer；再由敌方制造不可归属的 host-wide 压力 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3057)<br>`V26:L3062–L3062` | [V26:invariants:D63](#requirement-v26-invariants-d63) · [V26:invariants:D81](#requirement-v26-invariants-d81) · [V26:invariants:D82](#requirement-v26-invariants-d82) · [DS:C-block:C6](#requirement-ds-c-block-c6) · [RULE:C-block:C6](#requirement-rule-c-block-c6) |
| <a id="case-x47"></a>**X47**<br>逐项状态未收录 | 非清除 CriticalOperation 依次出现“仅调用”“局部效果成立”“全部 CompletionContract 条件满足”“完成证据随后失效” | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3057)<br>`V26:L3063–L3063` | [V26:invariants:D83](#requirement-v26-invariants-d83) · [RULE:D-block:D2](#requirement-rule-d-block-d2) · [GAP-MAP-01](#requirement-gap-map-01) · [GAP-MAP-07](#requirement-gap-map-07) · [skills:L47](#ms-check-skills-l47) · [skills:L49](#ms-check-skills-l49) |
| <a id="case-x48"></a>**X48**<br>逐项状态未收录 | Candidate 被证明无关后进入自动释放窗口；期间重复同一日志、轮换 sourceIdentity、出现一条新有效强证据、存在其他 restriction reason，并另行执行明确的操作者解除命令 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3057)<br>`V26:L3064–L3064` | [V26:invariants:D84](#requirement-v26-invariants-d84) · [V26:invariants:D85](#requirement-v26-invariants-d85) |
| <a id="case-x49"></a>**X49**<br>逐项状态未收录 | 为八类 Surface 分别伪造本地“完成状态”，并尝试由 adapter/cache/projection 直接改 Judgment、TargetEpoch、Admission 或 durable receipt；同时让两个 Surface 对同一目标提交冲突修复 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3068)<br>`V26:L3072–L3072` | [L6:decisions:D3](#requirement-l6-decisions-d3) · [V26:invariants:D60](#requirement-v26-invariants-d60) · [V26:invariants:D87](#requirement-v26-invariants-d87) · [V26:invariants:D89](#requirement-v26-invariants-d89) · [V26:invariants:D95](#requirement-v26-invariants-d95) · [DS:C-block:C1](#requirement-ds-c-block-c1) · [RULE:C-block:C1](#requirement-rule-c-block-c1) |
| <a id="case-x50"></a>**X50**<br>逐项状态未收录 | TERMINAL 目标先由 Temporal task 重建，再经 Registry 注册、Data 写回、Network 同步、Presentation 显示；在每一步插入重放、重复回调和顺序变化 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3068)<br>`V26:L3073–L3073` | [V26:invariants:D88](#requirement-v26-invariants-d88) · [V26:invariants:D93](#requirement-v26-invariants-d93) · [V26:invariants:D95](#requirement-v26-invariants-d95) · [msitem:L385](#ms-check-msitem-l385) |
| <a id="case-x51"></a>**X51**<br>逐项状态未收录 | 对全部五十四项 key 分别选择 Compatible、Enhanced、Extreme 和 Custom；再模拟 ceiling 缺失、PENDING_RESTART、矩阵升版与旧配置迁移 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3068)<br>`V26:L3074–L3074` | [V26:invariants:D90](#requirement-v26-invariants-d90) · [V26:invariants:D91](#requirement-v26-invariants-d91) · [DS:C-block:C12](#requirement-ds-c-block-c12) · [DS:C-block:C13](#requirement-ds-c-block-c13) · [GAP-MAP-07](#requirement-gap-map-07) |
| <a id="case-x52"></a>**X52**<br>逐项状态未收录 | 在单机、逻辑服务端、专用服务端和独立客户端分别加载；客户端篡改 HUD/input/cache、重放旧 packet，服务端缺少所有渲染类 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3068)<br>`V26:L3075–L3075` | [V26:invariants:D92](#requirement-v26-invariants-d92) · [DS:C-block:C5](#requirement-ds-c-block-c5) · [RULE:C-block:C5](#requirement-rule-c-block-c5) · [RULE:C-block:C14](#requirement-rule-c-block-c14) · [GAP-MAP-04](#requirement-gap-map-04) · [GAP-MAP-07](#requirement-gap-map-07) · [skills:L48](#ms-check-skills-l48) · [skills:L54](#ms-check-skills-l54) · [msitem:L385](#ms-check-msitem-l385) · [msitem:L386](#ms-check-msitem-l386) · [msitem:L387](#ms-check-msitem-l387) · [entitydefence:L292](#ms-check-entitydefence-l292) · [entitydefence:L293](#ms-check-entitydefence-l293) · [entitydefence:L298](#ms-check-entitydefence-l298) |
| <a id="case-x53"></a>**X53**<br>逐项状态未收录 | Candidate 被合法证明无关、目标被明确解除裁决并使用新 epoch 合法重激活；同时保留旧 task、event、packet、save 和 Presentation cache | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3068)<br>`V26:L3076–L3076` | [V26:invariants:D59](#requirement-v26-invariants-d59) · [V26:invariants:D85](#requirement-v26-invariants-d85) · [V26:invariants:D93](#requirement-v26-invariants-d93) |
| <a id="case-x54"></a>**X54**<br>逐项状态未收录 | Data repair 触发 Event、Event 安排 Temporal task、Temporal 更新 Network、Network 刷新 Presentation；再让 repair 结果本身被 observer 重复观察 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3068)<br>`V26:L3077–L3077` | [V26:invariants:D95](#requirement-v26-invariants-d95) · [entitydefence:L297](#ms-check-entitydefence-l297) |
| <a id="case-x55"></a>**X55**<br>逐项状态未收录 | EXTREME 下同时施压八类 Surface 的事件、类、任务、包、缓存、HUD、registry 和 native/render resource，并尝试通过配置关闭安全限制 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3068)<br>`V26:L3078–L3078` | [V26:invariants:D96](#requirement-v26-invariants-d96) |
| <a id="case-x56"></a>**X56**<br>逐项状态未收录 | 逐一移除某 Surface 的 observer、executor、repair、recovery 或 completion proof，并让其他 Surface 完全成功 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3068)<br>`V26:L3079–L3079` | [V26:invariants:D33](#requirement-v26-invariants-d33) · [V26:invariants:D64](#requirement-v26-invariants-d64) · [V26:invariants:D88](#requirement-v26-invariants-d88) · [V26:invariants:D94](#requirement-v26-invariants-d94) · [DS:C-block:C4](#requirement-ds-c-block-c4) · [DS:C-block:C7](#requirement-ds-c-block-c7) · [DS:C-block:C13](#requirement-ds-c-block-c13) · [RULE:C-block:C4](#requirement-rule-c-block-c4) · [RULE:C-block:C7](#requirement-rule-c-block-c7) · [GAP-MAP-07](#requirement-gap-map-07) |
| <a id="case-x57"></a>**X57**<br>逐项状态未收录 | 普通许可 OFF、高级键 STRICT_ALL、多个复合动作和全部三种全返回组合；切预设后改单项 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3085)<br>`V26:L3089–L3089` | [V26:invariants:D89](#requirement-v26-invariants-d89) · [V26:invariants:D90](#requirement-v26-invariants-d90) · [V26:invariants:D99](#requirement-v26-invariants-d99) · [RULE:C-block:C12](#requirement-rule-c-block-c12) |
| <a id="case-x58"></a>**X58**<br>逐项状态未收录 | schema-2 三档/Custom 升级到新子配置，缺字段/重复字段/未知版本/旧 journal，配置与激活之间重启 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3085)<br>`V26:L3090–L3090` | [V26:invariants:D99](#requirement-v26-invariants-d99) |
| <a id="case-x59"></a>**X59**<br>逐项状态未收录 | 相同 Subject 多 incarnation；lineage 多主体；重绑、终结、metadata 更新与身份迁移冲突 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3085)<br>`V26:L3091–L3091` | [L6:decisions:D4](#requirement-l6-decisions-d4) · [L6:decisions:D11](#requirement-l6-decisions-d11) · [V26:invariants:D93](#requirement-v26-invariants-d93) · [V26:invariants:D100](#requirement-v26-invariants-d100) · [DS:C-block:C9](#requirement-ds-c-block-c9) · [DS:C-block:C14](#requirement-ds-c-block-c14) · [RULE:C-block:C9](#requirement-rule-c-block-c9) · [GAP-MAP-02](#requirement-gap-map-02) · [GAP-MAP-05](#requirement-gap-map-05) · [msitem:L381](#ms-check-msitem-l381) · [entitydefence:L288](#ms-check-entitydefence-l288) · [entitydefence:L289](#ms-check-entitydefence-l289) · [entitydefence:L297](#ms-check-entitydefence-l297) · [entitydefence:L298](#ms-check-entitydefence-l298) |
| <a id="case-x60"></a>**X60**<br>逐项状态未收录 | N 目标资格 batch，逐 prepare/commit/publish 断点，单目标冲突、原 ID 重放、零目标动作 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3085)<br>`V26:L3092–L3092` | [L6:decisions:D6](#requirement-l6-decisions-d6) · [L6:decisions:D9](#requirement-l6-decisions-d9) · [V26:invariants:D80](#requirement-v26-invariants-d80) · [V26:invariants:D101](#requirement-v26-invariants-d101) · [DS:C-block:C19](#requirement-ds-c-block-c19) · [GAP-DOC-03](#requirement-gap-doc-03) · [GAP-MAP-03](#requirement-gap-map-03) |
| <a id="case-x61"></a>**X61**<br>逐项状态未收录 | 专用服务端无客户端、只服务器 HUD、可验证客户端与恶意自报客户端 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3085)<br>`V26:L3093–L3093` | [V26:invariants:D61](#requirement-v26-invariants-d61) · [V26:invariants:D102](#requirement-v26-invariants-d102) · [GAP-MAP-04](#requirement-gap-map-04) · [GAP-MAP-07](#requirement-gap-map-07) · [skills:L48](#ms-check-skills-l48) · [skills:L49](#ms-check-skills-l49) · [entitydefence:L292](#ms-check-entitydefence-l292) · [entitydefence:L293](#ms-check-entitydefence-l293) |
| <a id="case-x62"></a>**X62**<br>逐项状态未收录 | required/optional 客户端断线、超时、换 session、授权改参与集 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3085)<br>`V26:L3094–L3094` | [V26:invariants:D102](#requirement-v26-invariants-d102) · [GAP-MAP-04](#requirement-gap-map-04) · [GAP-MAP-07](#requirement-gap-map-07) · [skills:L48](#ms-check-skills-l48) · [entitydefence:L292](#ms-check-entitydefence-l292) · [entitydefence:L298](#ms-check-entitydefence-l298) |
| <a id="case-x63"></a>**X63**<br>逐项状态未收录 | durable intent 前、intent ACK 后、第三方效果后、step receipt 落盘/ACK 前后分别注入失败；另让 callback 延迟完成 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3085)<br>`V26:L3095–L3095` | [L6:decisions:D7](#requirement-l6-decisions-d7) · [V26:invariants:D103](#requirement-v26-invariants-d103) |
| <a id="case-x64"></a>**X64**<br>逐项状态未收录 | 同 command 的重试、重启与 planRevision/stepId 改变；尝试相同 effectKey 更换 payload/随机新 slot | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3085)<br>`V26:L3096–L3096` | [V26:invariants:D104](#requirement-v26-invariants-d104) |
| <a id="case-x65"></a>**X65**<br>逐项状态未收录 | 幂等、可查询、可补偿、opaque 四种 step；补偿途中失败与资格随后变化 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3085)<br>`V26:L3097–L3097` | [V26:invariants:D104](#requirement-v26-invariants-d104) |
| <a id="case-x66"></a>**X66**<br>逐项状态未收录<br>**旧前提见上节** | 扫描当前文档入口、优先级、历史引用及所有验收编号 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3085)<br>`V26:L3098–L3098` | [L6:decisions:D12](#requirement-l6-decisions-d12) · [V26:invariants:D106](#requirement-v26-invariants-d106) |
| <a id="case-x67"></a>**X67**<br>逐项状态未收录 | 当前 store 编码上限、软额度、磁盘满、原子替换失败、compaction 新旧共存空间不足、保留空间被外部耗尽 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3085)<br>`V26:L3099–L3099` | [V26:invariants:D105](#requirement-v26-invariants-d105) · [DS:C-block:C19](#requirement-ds-c-block-c19) |
| <a id="case-x68"></a>**X68**<br>逐项状态未收录 | 长时间 command/effect/dedup 增长；detail/summary/cold segment 均接近容量；旧 ID 重放与精确解除 | [V26 原文](MS-SKILLS-PLAN.md#orig-v26-l3085)<br>`V26:L3100–L3100` | [L6:decisions:D2](#requirement-l6-decisions-d2) · [V26:invariants:D105](#requirement-v26-invariants-d105) |

<a id="requirement-directory"></a>
## 反向：原条款 / C 块 / 缺口 → 验收规格

本节列 L6 明确决策行、V26 D33 及 D53–D106 明确条款、DS/RULE 两份加强 C 块、RULE 加强 D 块和 11 个登记缺口。**不是对全语料同号 ID 的语义合并或定义补全**；其他来源和全部文本出现仍看 [INVARIANT-INDEX](INVARIANT-INDEX.md)。`D1–D32` 的范围端点不自动变成独立条文；37 个既有编号空洞不新增内容、不重编号。

**DS:C12/C13/C14 与 RULE:C12/C13/C14 分开列；RULE 加强 D1–D4 不等于 L6 决策 D1–D4。** 即使两份 C 行文字相近，也分别保留来源和边。

### L6 决策

| 原编号（来源/章节限定） | 原文 / 设计主题 | 关联 B/X 与强度 | 关联依据及尚缺内容 |
|---|---|---|---|
| <a id="requirement-l6-decisions-d1"></a>`L6:decisions:D1` | [L6:L51–L51](MS-SKILLS-PLAN.md#orig-l6-l0045)<br>内部 Raw 与外部 Presence 分离 | [B02](#case-b02) · [B21](#case-b21)<br>**设计子范围** | B02 检查 Raw 可见；B21 明引 L6 Master D1/D5。<br>**尚缺：**还需各内部发现/读取入口的实际覆盖，不能只凭查询样例。 |
| <a id="requirement-l6-decisions-d2"></a>`L6:decisions:D2` | [L6:L52–L52](MS-SKILLS-PLAN.md#orig-l6-l0045)<br>精确 Judgment 与物理表示分离 | [B10](#case-b10) · [X68](#case-x68)<br>**设计子范围** | B10 恢复原 Truth，X68 要求精确压缩、解除及历史去重。<br>**尚缺：**存储表示等价的全命中集合与迁移负例仍需证据。 |
| <a id="requirement-l6-decisions-d3"></a>`L6:decisions:D3` | [L6:L53–L53](MS-SKILLS-PLAN.md#orig-l6-l0045)<br>唯一 Java / Internal Root writer | [B23](#case-b23) · [X49](#case-x49)<br>**设计子范围** | B23 保留当前 JVM writer 断言；X49 禁止表面直接写 Truth。<br>**尚缺：**B23 旧 helper 前提已失效；还需原生/Java 职责及全部写入路径核查。 |
| <a id="requirement-l6-decisions-d4"></a>`L6:decisions:D4` | [L6:L54–L54](MS-SKILLS-PLAN.md#orig-l6-l0045)<br>epoch 与身份/命令维度分离 | [B09](#case-b09) · [X59](#case-x59)<br>**设计子范围** | B09 覆盖重放上下文，X59 区分 TargetEpoch、binding/ownership 与全局 epoch。<br>**尚缺：**字段定义、序列和相等性不能仅由这两项推导。 |
| <a id="requirement-l6-decisions-d5"></a>`L6:decisions:D5` | [L6:L55–L55](MS-SKILLS-PLAN.md#orig-l6-l0045)<br>ONCE / REPEATABLE 与 Raw 保留 | [B20](#case-b20) · [B21](#case-b21)<br>**设计子范围** | B20、B21 均明引 L6 Master D5。<br>**尚缺：**仍需真实死亡/掉落、未知副作用与 Raw 路径的全子范围。 |
| <a id="requirement-l6-decisions-d6"></a>`L6:decisions:D6` | [L6:L56–L56](MS-SKILLS-PLAN.md#orig-l6-l0045)<br>批量提交扩展性 | [X60](#case-x60)<br>**设计子范围** | X60 要求非空 batch 一次提交/至多一次发布，与 D6 的发布次数相关。<br>**尚缺：**X60 并未完整测量 N×历史规模复杂度，另需性能与结构证据。 |
| <a id="requirement-l6-decisions-d7"></a>`L6:decisions:D7` | [L6:L57–L57](MS-SKILLS-PLAN.md#orig-l6-l0045)<br>durable receipt 后执行与未知恢复 | [B18](#case-b18) · [B19](#case-b19) · [X40](#case-x40) · [X63](#case-x63)<br>**设计子范围** | B18/19、X40/63 覆盖 receipt、未知提交与副作用时序。<br>**尚缺：**查询热路径零同步 durable I/O 仍需独立探针/调用路径核查。 |
| <a id="requirement-l6-decisions-d8"></a>`L6:decisions:D8` | [L6:L58–L58](MS-SKILLS-PLAN.md#orig-l6-l0045)<br>先证明替代链再移除旧机制 | —<br>**未定位直接项** | 属于阶段接续规则，本轮未找到针对删除顺序的直接 B/X 断言。<br>**尚缺：**保留阶段/替代链证据要求，不强贴一个结果测试。 |
| <a id="requirement-l6-decisions-d9"></a>`L6:decisions:D9` | [L6:L66–L66](MS-SKILLS-PLAN.md#orig-l6-l0060)<br>一个 batch 一个原子事务 | [B19](#case-b19) · [X60](#case-x60)<br>**设计子范围** | B19 检查冲突全批拒绝；X60 明确一次 epoch/commit/receipt/发布。<br>**尚缺：**需真实断点与同产物恢复证据。 |
| <a id="requirement-l6-decisions-d10"></a>`L6:decisions:D10` | [L6:L67–L67](MS-SKILLS-PLAN.md#orig-l6-l0060)<br>分阶段 receipt writer 迁移 | —<br>**未定位直接项** | 属于 7.15 shadow / 7.16 cutover 的阶段职责。<br>**尚缺：**不得用 X49 的最终唯一 writer 结果替代各迁移阶段证据。 |
| <a id="requirement-l6-decisions-d11"></a>`L6:decisions:D11` | [L6:L68–L68](MS-SKILLS-PLAN.md#orig-l6-l0060)<br>canonical API 与 SubjectKey 演进 | [X59](#case-x59)<br>**设计子范围** | X59 可检查身份/迁移冲突这一子范围。<br>**尚缺：**canonical 包归属与重复 DTO 排除并非 X59 直接断言。 |
| <a id="requirement-l6-decisions-d12"></a>`L6:decisions:D12` | [L6:L69–L69](MS-SKILLS-PLAN.md#orig-l6-l0060)<br>保留不变量覆盖而非旧测试数量 | [X66](#case-x66)<br>**设计子范围** | X66 的编号/历史缺件检查可作为追溯入口。<br>**尚缺：**测试等价与删除废弃结构测试须单独证明；五级顺序旧前提失效。 |
| <a id="requirement-l6-decisions-d13"></a>`L6:decisions:D13` | [L6:L70–L70](MS-SKILLS-PLAN.md#orig-l6-l0060)<br>进程边界与单 writer | [B23](#case-b23)<br>**设计子范围** | B23 明引 L6 Master D13，但原 helper 场景在现边界下不适用。<br>**尚缺：**需要无子进程的实际路径/进程树、失败 fallback 与 writer 证据；本轮没有新验收。<br>**效力：**helper 原授权无效；见 README#scope-process。 |

### V26 不变量

| 原编号（来源/章节限定） | 原文 / 设计主题 | 关联 B/X 与强度 | 关联依据及尚缺内容 |
|---|---|---|---|
| <a id="requirement-v26-invariants-d33"></a>`V26:invariants:D33` | [V26:L472–L502](MS-SKILLS-PLAN.md#orig-v26-l0472)<br>最终提交门不得绝对化 | [B17](#case-b17) · [X26](#case-x26) · [X56](#case-x56)<br>**设计子范围** | B17 检查提交门，X26/56 检查失证与范围降级；是设计子范围关联。<br>**尚缺：**不能由已登记门的成功推出所有未知写入均被前置拦截。 |
| <a id="requirement-v26-invariants-d53"></a>`V26:invariants:D53` | [V26:L507–L507](MS-SKILLS-PLAN.md#orig-v26-l0503)<br>候选 default-in | [X24](#case-x24) · [X31](#case-x31)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“候选 default-in”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d54"></a>`V26:invariants:D54` | [V26:L508–L508](MS-SKILLS-PLAN.md#orig-v26-l0503)<br>证明控制处置级别 | [X25](#case-x25)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“证明控制处置级别”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d55"></a>`V26:invariants:D55` | [V26:L509–L509](MS-SKILLS-PLAN.md#orig-v26-l0503)<br>预算不丢义务 | [X28](#case-x28) · [X35](#case-x35)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“预算不丢义务”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d56"></a>`V26:invariants:D56` | [V26:L510–L510](MS-SKILLS-PLAN.md#orig-v26-l0503)<br>最小 fence scope | [X26](#case-x26)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“最小 fence scope”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d57"></a>`V26:invariants:D57` | [V26:L511–L511](MS-SKILLS-PLAN.md#orig-v26-l0503)<br>Root fence 升级条件 | [X26](#case-x26)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“Root fence 升级条件”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d58"></a>`V26:invariants:D58` | [V26:L512–L512](MS-SKILLS-PLAN.md#orig-v26-l0503)<br>资格代际推进 | [X27](#case-x27) · [X45](#case-x45)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“资格代际推进”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d59"></a>`V26:invariants:D59` | [V26:L513–L513](MS-SKILLS-PLAN.md#orig-v26-l0503)<br>合法新代际不复活旧资格 | [X27](#case-x27) · [X53](#case-x53)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“合法新代际不复活旧资格”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d60"></a>`V26:invariants:D60` | [V26:L514–L514](MS-SKILLS-PLAN.md#orig-v26-l0503)<br>ResultGraph 不是 writer | [X49](#case-x49)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“ResultGraph 不是 writer”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**X49 只给出表面写入负例，ResultGraph 本身仍需专门负例。 |
| <a id="requirement-v26-invariants-d61"></a>`V26:invariants:D61` | [V26:L515–L515](MS-SKILLS-PLAN.md#orig-v26-l0503)<br>证书不是 Truth | [X39](#case-x39) · [X61](#case-x61)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“证书不是 Truth”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**X39/61 不等于证书不得写 Admission/Judgment 的完整负例。 |
| <a id="requirement-v26-invariants-d62"></a>`V26:invariants:D62` | [V26:L516–L516](MS-SKILLS-PLAN.md#orig-v26-l0503)<br>无敌对 Mod 数量上限 | [X29](#case-x29)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“无敌对 Mod 数量上限”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d63"></a>`V26:invariants:D63` | [V26:L517–L517](MS-SKILLS-PLAN.md#orig-v26-l0503)<br>宿主可持续性 | [X30](#case-x30) · [X46](#case-x46)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“宿主可持续性”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d64"></a>`V26:invariants:D64` | [V26:L518–L518](MS-SKILLS-PLAN.md#orig-v26-l0503)<br>不伪报 DENIED | [X38](#case-x38) · [X56](#case-x56)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“不伪报 DENIED”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**仍需真正不能前置 veto 的路径与真实返回证明，不能仅构造状态枚举。 |
| <a id="requirement-v26-invariants-d65"></a>`V26:invariants:D65` | [V26:L519–L519](MS-SKILLS-PLAN.md#orig-v26-l0503)<br>7.16 阶段边界 | —<br>**未定位直接项** | 阶段/范围约定，本轮不强行分配无直接断言的 B/X 项。<br>**尚缺：**阶段交付/变更记录需单独核查，不以其他测试的成功代替。 |
| <a id="requirement-v26-invariants-d66"></a>`V26:invariants:D66` | [V26:L525–L525](MS-SKILLS-PLAN.md#orig-v26-l0521)<br>一强或两独立弱证据 | [X31](#case-x31)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“一强或两独立弱证据”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d67"></a>`V26:invariants:D67` | [V26:L526–L526](MS-SKILLS-PLAN.md#orig-v26-l0521)<br>候选只得临时处置 | [X33](#case-x33)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“候选只得临时处置”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d68"></a>`V26:invariants:D68` | [V26:L527–L527](MS-SKILLS-PLAN.md#orig-v26-l0521)<br>证据独立性 | [X32](#case-x32)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“证据独立性”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d69"></a>`V26:invariants:D69` | [V26:L528–L528](MS-SKILLS-PLAN.md#orig-v26-l0521)<br>结构与载荷有界 | [X34](#case-x34) · [X35](#case-x35)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“结构与载荷有界”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d70"></a>`V26:invariants:D70` | [V26:L529–L529](MS-SKILLS-PLAN.md#orig-v26-l0521)<br>规范义务与范围汇总 | [X34](#case-x34) · [X35](#case-x35) · [X43](#case-x43)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“规范义务与范围汇总”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。 X43 的不同目标/出口/版本混并负例可检查规范义务归并边界。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d71"></a>`V26:invariants:D71` | [V26:L530–L530](MS-SKILLS-PLAN.md#orig-v26-l0521)<br>限制接力不丢义务 | [X36](#case-x36) · [X37](#case-x37)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“限制接力不丢义务”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d72"></a>`V26:invariants:D72` | [V26:L531–L531](MS-SKILLS-PLAN.md#orig-v26-l0521)<br>摘要不等于闭环 | [X35](#case-x35) · [X37](#case-x37)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“摘要不等于闭环”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d73"></a>`V26:invariants:D73` | [V26:L532–L532](MS-SKILLS-PLAN.md#orig-v26-l0521)<br>提交/执行/结果分离 | [X38](#case-x38) · [X40](#case-x40)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“提交/执行/结果分离”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d74"></a>`V26:invariants:D74` | [V26:L533–L533](MS-SKILLS-PLAN.md#orig-v26-l0521)<br>局部效果与未知提交 | [B01](#case-b01) · [X40](#case-x40)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“局部效果与未知提交”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d75"></a>`V26:invariants:D75` | [V26:L534–L534](MS-SKILLS-PLAN.md#orig-v26-l0521)<br>证书失效与重评 | [B08](#case-b08) · [X39](#case-x39) · [X44](#case-x44)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“证书失效与重评”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。 B08 的 manager 重新注册可作为再次漂移的子场景，但尚需当前证书失效观察。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d76"></a>`V26:invariants:D76` | [V26:L535–L535](MS-SKILLS-PLAN.md#orig-v26-l0521)<br>压力与配额公平 | [X41](#case-x41)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“压力与配额公平”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d77"></a>`V26:invariants:D77` | [V26:L536–L536](MS-SKILLS-PLAN.md#orig-v26-l0521)<br>保留预算与探索 | [X42](#case-x42)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“保留预算与探索”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d78"></a>`V26:invariants:D78` | [V26:L537–L537](MS-SKILLS-PLAN.md#orig-v26-l0521)<br>合同实现阶段划分 | —<br>**未定位直接项** | 阶段/范围约定，本轮不强行分配无直接断言的 B/X 项。<br>**尚缺：**阶段交付/变更记录需单独核查，不以其他测试的成功代替。 |
| <a id="requirement-v26-invariants-d79"></a>`V26:invariants:D79` | [V26:L543–L543](MS-SKILLS-PLAN.md#orig-v26-l0539)<br>资格 transition 单事务 | [X45](#case-x45)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“资格 transition 单事务”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d80"></a>`V26:invariants:D80` | [V26:L544–L544](MS-SKILLS-PLAN.md#orig-v26-l0539)<br>整体可见与原命令恢复 | [X45](#case-x45) · [X60](#case-x60)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“整体可见与原命令恢复”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d81"></a>`V26:invariants:D81` | [V26:L545–L545](MS-SKILLS-PLAN.md#orig-v26-l0539)<br>全资源域安全 | [X46](#case-x46)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“全资源域安全”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d82"></a>`V26:invariants:D82` | [V26:L546–L546](MS-SKILLS-PLAN.md#orig-v26-l0539)<br>owned 与 host pressure 分离 | [X46](#case-x46)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“owned 与 host pressure 分离”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d83"></a>`V26:invariants:D83` | [V26:L547–L547](MS-SKILLS-PLAN.md#orig-v26-l0539)<br>非清除最终完成 | [X47](#case-x47)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“非清除最终完成”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d84"></a>`V26:invariants:D84` | [V26:L548–L548](MS-SKILLS-PLAN.md#orig-v26-l0539)<br>候选释放防抖 | [X48](#case-x48)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“候选释放防抖”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d85"></a>`V26:invariants:D85` | [V26:L549–L549](MS-SKILLS-PLAN.md#orig-v26-l0539)<br>显式解除语义 | [X48](#case-x48) · [X53](#case-x53)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“显式解除语义”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d86"></a>`V26:invariants:D86` | [V26:L550–L550](MS-SKILLS-PLAN.md#orig-v26-l0539)<br>V2.4 阶段边界 | —<br>**未定位直接项** | 阶段/范围约定，本轮不强行分配无直接断言的 B/X 项。<br>**尚缺：**阶段交付/变更记录需单独核查，不以其他测试的成功代替。 |
| <a id="requirement-v26-invariants-d87"></a>`V26:invariants:D87` | [V26:L556–L556](MS-SKILLS-PLAN.md#orig-v26-l0552)<br>八表面无第二 writer | [X49](#case-x49)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“八表面无第二 writer”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d88"></a>`V26:invariants:D88` | [V26:L557–L557](MS-SKILLS-PLAN.md#orig-v26-l0552)<br>八阶段表面能力链 | [X50](#case-x50) · [X56](#case-x56)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“八阶段表面能力链”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**每个 typed 输入输出、revision 和阶段均需实际适配器证据。 |
| <a id="requirement-v26-invariants-d89"></a>`V26:invariants:D89` | [V26:L558–L558](MS-SKILLS-PLAN.md#orig-v26-l0552)<br>三个平面映射 | [X49](#case-x49) · [X57](#case-x57)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“三个平面映射”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**不得据 X49/57 宣称没有第四平面或第二状态机；需接口/结构核查。 |
| <a id="requirement-v26-invariants-d90"></a>`V26:invariants:D90` | [V26:L559–L559](MS-SKILLS-PLAN.md#orig-v26-l0552)<br>稳定 key / GUI 范围 | [X51](#case-x51) · [X57](#case-x57)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“稳定 key / GUI 范围”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**X51/57 只覆盖配置子范围，GUI 顶层按钮与兼容枚举仍需另查。 |
| <a id="requirement-v26-invariants-d91"></a>`V26:invariants:D91` | [V26:L560–L560](MS-SKILLS-PLAN.md#orig-v26-l0552)<br>逐键三档矩阵 | [X51](#case-x51)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“逐键三档矩阵”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d92"></a>`V26:invariants:D92` | [V26:L561–L561](MS-SKILLS-PLAN.md#orig-v26-l0552)<br>客户端与服务端分离 | [X52](#case-x52)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“客户端与服务端分离”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d93"></a>`V26:invariants:D93` | [V26:L562–L562](MS-SKILLS-PLAN.md#orig-v26-l0552)<br>跨表面身份与版本 | [B09](#case-b09) · [X50](#case-x50) · [X53](#case-x53) · [X59](#case-x59)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“跨表面身份与版本”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d94"></a>`V26:invariants:D94` | [V26:L563–L563](MS-SKILLS-PLAN.md#orig-v26-l0552)<br>能力按实际报告 | [B16](#case-b16) · [X56](#case-x56)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“能力按实际报告”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d95"></a>`V26:invariants:D95` | [V26:L564–L564](MS-SKILLS-PLAN.md#orig-v26-l0552)<br>同一计划与循环抑制 | [X49](#case-x49) · [X50](#case-x50) · [X54](#case-x54)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“同一计划与循环抑制”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d96"></a>`V26:invariants:D96` | [V26:L565–L565](MS-SKILLS-PLAN.md#orig-v26-l0552)<br>所有预设安全保留 | [X55](#case-x55)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“所有预设安全保留”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d97"></a>`V26:invariants:D97` | [V26:L566–L566](MS-SKILLS-PLAN.md#orig-v26-l0552)<br>同 JVM 与用户数据边界 | [B23](#case-b23) · [X30](#case-x30)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“同 JVM 与用户数据边界”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**B23 的 helper 前提失效；全部受禁文件类别及无外部进程的证据未绑定。 |
| <a id="requirement-v26-invariants-d98"></a>`V26:invariants:D98` | [V26:L567–L567](MS-SKILLS-PLAN.md#orig-v26-l0552)<br>八表面阶段划分 | —<br>**未定位直接项** | 阶段/范围约定，本轮不强行分配无直接断言的 B/X 项。<br>**尚缺：**阶段交付/变更记录需单独核查，不以其他测试的成功代替。 |
| <a id="requirement-v26-invariants-d99"></a>`V26:invariants:D99` | [V26:L575–L575](MS-SKILLS-PLAN.md#orig-v26-l0571)<br>普通许可与高级策略交集 | [B04](#case-b04) · [X57](#case-x57) · [X58](#case-x58)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“普通许可与高级策略交集”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。 B04 的攻击 OFF 不失能断言还可检查许可交集的子范围。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d100"></a>`V26:invariants:D100` | [V26:L576–L576](MS-SKILLS-PLAN.md#orig-v26-l0571)<br>唯一 SubjectKey 与版本 scope | [X59](#case-x59)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“唯一 SubjectKey 与版本 scope”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d101"></a>`V26:invariants:D101` | [V26:L577–L577](MS-SKILLS-PLAN.md#orig-v26-l0571)<br>资格 batch 原子性 | [X60](#case-x60)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“资格 batch 原子性”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d102"></a>`V26:invariants:D102` | [V26:L578–L578](MS-SKILLS-PLAN.md#orig-v26-l0571)<br>completion 范围预声明 | [X61](#case-x61) · [X62](#case-x62)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“completion 范围预声明”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d103"></a>`V26:invariants:D103` | [V26:L579–L579](MS-SKILLS-PLAN.md#orig-v26-l0571)<br>Root / step unknown 分离 | [X63](#case-x63)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“Root / step unknown 分离”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d104"></a>`V26:invariants:D104` | [V26:L580–L580](MS-SKILLS-PLAN.md#orig-v26-l0571)<br>稳定 effectKey 与有证据恢复 | [X64](#case-x64) · [X65](#case-x65)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“稳定 effectKey 与有证据恢复”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d105"></a>`V26:invariants:D105` | [V26:L581–L581](MS-SKILLS-PLAN.md#orig-v26-l0571)<br>有限存储与精确历史 | [X67](#case-x67) · [X68](#case-x68)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“有限存储与精确历史”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**实际 fixture、正反例、故障断点、产物/配置/环境及运行证据尚未逐项绑定。 |
| <a id="requirement-v26-invariants-d106"></a>`V26:invariants:D106` | [V26:L582–L582](MS-SKILLS-PLAN.md#orig-v26-l0571)<br>规范入口与历史缺件 | [X66](#case-x66)<br>**设计子范围** | 原 D 条款与所列用例的场景/断言在“规范入口与历史缺件”上对应；本轮新增设计关联，不声称原文已逐 ID 声明覆盖。<br>**尚缺：**旧唯一顺序按 README#scope-governance 失效；编号/历史缺件检查仍保留。<br>**效力：**旧优先级冻结不恢复。 |

### DS 加强 C 块

| 原编号（来源/章节限定） | 原文 / 设计主题 | 关联 B/X 与强度 | 关联依据及尚缺内容 |
|---|---|---|---|
| <a id="requirement-ds-c-block-c1"></a>`DS:C-block:C1` | [DS:L1046–L1046](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>唯一判定与 writer | [B23](#case-b23) · [X49](#case-x49)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**仍需裁决三选一路线及 Java/Native 重复判定结构；B23 旧 helper 前提失效。<br>**效力：**DS 的 P 标签按 README#scope-p-layer 解释，不是永久冻结。 |
| <a id="requirement-ds-c-block-c2"></a>`DS:C-block:C2` | [DS:L1047–L1047](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>构建命名单一来源 | —<br>**未定位直接项** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**没有直接 B/X 断言；须构建产物/资源路径一致性检查。 |
| <a id="requirement-ds-c-block-c3"></a>`DS:C-block:C3` | [DS:L1048–L1048](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>Java 17 前提与独立 ping | [B16](#case-b16)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**B16 只支持能力降级子范围，前提检查和独立 ping 需专用探针。 |
| <a id="requirement-ds-c-block-c4"></a>`DS:C-block:C4` | [DS:L1049–L1049](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>注入失败策略与诊断 | [X56](#case-x56)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**X56 不枚举全部注入点，需逐站点原因/失败处置证据。 |
| <a id="requirement-ds-c-block-c5"></a>`DS:C-block:C5` | [DS:L1050–L1050](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>Mixin 七字段契约 | [B05](#case-b05) · [B15](#case-b15) · [X52](#case-x52)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**B/X 只覆盖运行时/逻辑侧子范围，七字段与精确 descriptor 仍需契约审查。 |
| <a id="requirement-ds-c-block-c6"></a>`DS:C-block:C6` | [DS:L1051–L1051](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>重转换 worker、去重与预算 | [X30](#case-x30) · [X46](#case-x46)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**资源压力不证明单 worker、每批上限及逐类失败回退，需要专项断言。 |
| <a id="requirement-ds-c-block-c7"></a>`DS:C-block:C7` | [DS:L1052–L1052](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>关键能力第二通道 | [B06](#case-b06) · [B16](#case-b16) · [X56](#case-x56)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**须真的停用第一通道并记录生效层，不能由加载/状态显示反推已接线。 |
| <a id="requirement-ds-c-block-c8"></a>`DS:C-block:C8` | [DS:L1053–L1053](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>列表收窄与 Raw/Presence 分离 | [B21](#case-b21)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**B21 不是受保护读取兜底或独占过滤五项副作用测试；见 MS 对照。 |
| <a id="requirement-ds-c-block-c9"></a>`DS:C-block:C9` | [DS:L1054–L1054](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>规范实例与修复 | [B02](#case-b02) · [B03](#case-b03) · [X59](#case-x59)<br>**仅邻近/逆向，非覆盖** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**B02 是终结后禁止重插，方向相反，仅邻近入口；须补 PROTECTED 正向恢复矩阵。 |
| <a id="requirement-ds-c-block-c10"></a>`DS:C-block:C10` | [DS:L1055–L1055](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>原生构建与可执行探针 | [B15](#case-b15) · [B16](#case-b16)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**尚缺 Gradle 包装、同产物 hash、自映像/桥接探针证据，不能用 B16 顶替。 |
| <a id="requirement-ds-c-block-c11"></a>`DS:C-block:C11` | [DS:L1056–L1056](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>14 动作接线与统一调度 | [B01](#case-b01) · [B18](#case-b18) · [B20](#case-b20) · [B22](#case-b22)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**现有例未逐一枚举 14 动作；需动作×入口×去重×结算子表。<br>**效力：**DS 的 P 标签按 README#scope-p-layer 解释，不是永久冻结。 |
| <a id="requirement-ds-c-block-c12"></a>`DS:C-block:C12` | [DS:L1057–L1057](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>逐键矩阵 revision 2 | [X51](#case-x51)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**需 54 key×三档/Custom 的实际 requested/effective 策略证据；P 层可修订。<br>**效力：**DS 的 P 标签按 README#scope-p-layer 解释，不是永久冻结。 |
| <a id="requirement-ds-c-block-c13"></a>`DS:C-block:C13` | [DS:L1058–L1058](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>真实 ceiling 管道 | [B16](#case-b16) · [X51](#case-x51) · [X56](#case-x56)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**逐 key 的 supported/evidence ceiling 与 RuntimeCapabilityProjection 仍需证据。<br>**效力：**DS 的 P 标签按 README#scope-p-layer 解释，不是永久冻结。 |
| <a id="requirement-ds-c-block-c14"></a>`DS:C-block:C14` | [DS:L1059–L1059](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>NBT 一实现两门 | [B10](#case-b10) · [X59](#case-x59)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**B10/X59 只支持恢复/身份子范围，不证明编码实现已去重。<br>**效力：**DS 的 P 标签按 README#scope-p-layer 解释，不是永久冻结。 |
| <a id="requirement-ds-c-block-c15"></a>`DS:C-block:C15` | [DS:L1060–L1060](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>不起外部进程的前置链 | [B23](#case-b23)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**原 helper 正例不适用；须替代接线、失败 fallback 和实际无子进程证据，不放宽 UX1。 |
| <a id="requirement-ds-c-block-c16"></a>`DS:C-block:C16` | [DS:L1061–L1061](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>改写集合及不针对边界 | [B11](#case-b11) · [B13](#case-b13) · [X25](#case-x25)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**需明确内容驱动选择与受禁文件类别；shared manager/同名类负例不证明完整边界。<br>**效力：**DS 的 P 标签按 README#scope-p-layer 解释，不是永久冻结。 |
| <a id="requirement-ds-c-block-c17"></a>`DS:C-block:C17` | [DS:L1062–L1062](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>补丁回滚与 baseline | [B05](#case-b05) · [B15](#case-b15)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**需保存原始/最终字节或明确宿主重启恢复契约；Ronova 不得启动 relaunch。 |
| <a id="requirement-ds-c-block-c18"></a>`DS:C-block:C18` | [DS:L1063–L1063](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>归属启发式收敛 | [B11](#case-b11) · [B13](#case-b13)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**需显式清单的来源与选择器检查；不把内容相似当成归属证明。 |
| <a id="requirement-ds-c-block-c19"></a>`DS:C-block:C19` | [DS:L1064–L1064](MS-SKILLS-PLAN.md#orig-ds-l1040)<br>批量性能、原子性与发布时序 | [B19](#case-b19) · [X60](#case-x60) · [X67](#case-x67)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**扩展性与热路径零同步 I/O 并未被这些用例完整覆盖，仍需专项探针。<br>**效力：**DS 的 P 标签按 README#scope-p-layer 解释，不是永久冻结。 |

### RULE 加强 C 块

| 原编号（来源/章节限定） | 原文 / 设计主题 | 关联 B/X 与强度 | 关联依据及尚缺内容 |
|---|---|---|---|
| <a id="requirement-rule-c-block-c1"></a>`RULE:C-block:C1` | [RULE:L779–L779](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>唯一判定与 writer | [B23](#case-b23) · [X49](#case-x49)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**仍需裁决三选一路线及 Java/Native 重复判定结构；B23 旧 helper 前提失效。 |
| <a id="requirement-rule-c-block-c2"></a>`RULE:C-block:C2` | [RULE:L780–L780](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>构建命名单一来源 | —<br>**未定位直接项** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**没有直接 B/X 断言；须构建产物/资源路径一致性检查。 |
| <a id="requirement-rule-c-block-c3"></a>`RULE:C-block:C3` | [RULE:L781–L781](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>Java 17 前提与独立 ping | [B16](#case-b16)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**B16 只支持能力降级子范围，前提检查和独立 ping 需专用探针。 |
| <a id="requirement-rule-c-block-c4"></a>`RULE:C-block:C4` | [RULE:L782–L782](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>注入失败策略与诊断 | [X56](#case-x56)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**X56 不枚举全部注入点，需逐站点原因/失败处置证据。 |
| <a id="requirement-rule-c-block-c5"></a>`RULE:C-block:C5` | [RULE:L783–L783](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>Mixin 七字段契约 | [B05](#case-b05) · [B15](#case-b15) · [X52](#case-x52)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**B/X 只覆盖运行时/逻辑侧子范围，七字段与精确 descriptor 仍需契约审查。 |
| <a id="requirement-rule-c-block-c6"></a>`RULE:C-block:C6` | [RULE:L784–L784](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>重转换 worker、去重与预算 | [X30](#case-x30) · [X46](#case-x46)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**资源压力不证明单 worker、每批上限及逐类失败回退，需要专项断言。 |
| <a id="requirement-rule-c-block-c7"></a>`RULE:C-block:C7` | [RULE:L785–L785](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>关键能力第二通道 | [B06](#case-b06) · [B16](#case-b16) · [X56](#case-x56)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**须真的停用第一通道并记录生效层，不能由加载/状态显示反推已接线。 |
| <a id="requirement-rule-c-block-c8"></a>`RULE:C-block:C8` | [RULE:L786–L786](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>列表收窄与 Raw/Presence 分离 | [B21](#case-b21)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**B21 不是受保护读取兜底或独占过滤五项副作用测试；见 MS 对照。 |
| <a id="requirement-rule-c-block-c9"></a>`RULE:C-block:C9` | [RULE:L787–L787](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>规范实例与修复 | [B02](#case-b02) · [B03](#case-b03) · [X59](#case-x59)<br>**仅邻近/逆向，非覆盖** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**B02 是终结后禁止重插，方向相反，仅邻近入口；须补 PROTECTED 正向恢复矩阵。 |
| <a id="requirement-rule-c-block-c10"></a>`RULE:C-block:C10` | [RULE:L788–L788](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>原生构建与可执行探针 | [B15](#case-b15) · [B16](#case-b16)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**尚缺 Gradle 包装、同产物 hash、自映像/桥接探针证据，不能用 B16 顶替。 |
| <a id="requirement-rule-c-block-c11"></a>`RULE:C-block:C11` | [RULE:L789–L789](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>14 动作接线与统一调度 | [B01](#case-b01) · [B18](#case-b18) · [B20](#case-b20) · [B22](#case-b22)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**现有例未逐一枚举 14 动作；需动作×入口×去重×结算子表。 |
| <a id="requirement-rule-c-block-c12"></a>`RULE:C-block:C12` | [RULE:L790–L790](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>跨所有权 SavedData 显式开关选择 | [B10](#case-b10) · [X57](#case-x57)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**原文标为不阻塞的自定选择；不是 DS 的逐键矩阵 C12。B/X 只为相邻许可/持久化子范围。 |
| <a id="requirement-rule-c-block-c13"></a>`RULE:C-block:C13` | [RULE:L791–L791](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>Java/Native 目标清单单一来源 | [B13](#case-b13) · [B15](#case-b15)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**不是 DS 的 ceiling C13；需清单同源与构建/注入一致性检查。 |
| <a id="requirement-rule-c-block-c14"></a>`RULE:C-block:C14` | [RULE:L792–L792](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>公共到客户端调用保护 | [X52](#case-x52)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**不是 DS 的 NBT C14；需专用服真实类加载/初始化负例。 |
| <a id="requirement-rule-c-block-c15"></a>`RULE:C-block:C15` | [RULE:L793–L793](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>不起外部进程的前置链 | [B23](#case-b23)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**原 helper 正例不适用；须替代接线、失败 fallback 和实际无子进程证据，不放宽 UX1。 |
| <a id="requirement-rule-c-block-c16"></a>`RULE:C-block:C16` | [RULE:L794–L794](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>改写集合及不针对边界 | [B11](#case-b11) · [B13](#case-b13) · [X25](#case-x25)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**需明确内容驱动选择与受禁文件类别；shared manager/同名类负例不证明完整边界。 |
| <a id="requirement-rule-c-block-c17"></a>`RULE:C-block:C17` | [RULE:L795–L795](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>补丁回滚与 baseline | [B05](#case-b05) · [B15](#case-b15)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**需保存原始/最终字节或明确宿主重启恢复契约；Ronova 不得启动 relaunch。 |
| <a id="requirement-rule-c-block-c18"></a>`RULE:C-block:C18` | [RULE:L796–L796](MS-SKILLS-PLAN.md#orig-rule-l0767)<br>归属启发式收敛 | [B11](#case-b11) · [B13](#case-b13)<br>**设计子范围** | 本轮把该 C 行的具体职责与相邻 B/X 场景对照；不从同号推导同义，也不声称已有证据覆盖。<br>**尚缺：**需显式清单的来源与选择器检查；不把内容相似当成归属证明。 |

### RULE 加强 D 块

| 原编号（来源/章节限定） | 原文 / 设计主题 | 关联 B/X 与强度 | 关联依据及尚缺内容 |
|---|---|---|---|
| <a id="requirement-rule-d-block-d1"></a>`RULE:D-block:D1` | [RULE:L802–L802](MS-MSITEM-PLAN.md#orig-rule-l0798)<br>13 个未接线动作 | [B01](#case-b01) · [B18](#case-b18) · [B20](#case-b20) · [B22](#case-b22)<br>**设计子范围** | 与执行/结算用例的设计子范围关联；不是同号 L6 不变量。<br>**尚缺：**需逐动作执行/结算证据。 |
| <a id="requirement-rule-d-block-d2"></a>`RULE:D-block:D2` | [RULE:L803–L803](MS-MSITEM-PLAN.md#orig-rule-l0798)<br>CompletionTracker 生产者 | [B01](#case-b01) · [B12](#case-b12) · [X40](#case-x40) · [X47](#case-x47)<br>**设计子范围** | 与执行/结算用例的设计子范围关联；不是同号 L6 不变量。<br>**尚缺：**需九条件及全部生产者的覆盖。 |
| <a id="requirement-rule-d-block-d3"></a>`RULE:D-block:D3` | [RULE:L804–L804](MS-MSITEM-PLAN.md#orig-rule-l0798)<br>TerminalLifecycle 服务端分支 | [B01](#case-b01) · [B20](#case-b20)<br>**设计子范围** | 与执行/结算用例的设计子范围关联；不是同号 L6 不变量。<br>**尚缺：**需真实服务端可达性及副作用一次性证据。 |
| <a id="requirement-rule-d-block-d4"></a>`RULE:D-block:D4` | [RULE:L805–L805](MS-MSITEM-PLAN.md#orig-rule-l0798)<br>单一结算所有者 | [B18](#case-b18) · [B20](#case-b20)<br>**设计子范围** | 与执行/结算用例的设计子范围关联；不是同号 L6 不变量。<br>**尚缺：**需多入口重叠与资源结算对照。 |

### MS 缺口

| 原编号（来源/章节限定） | 原文 / 设计主题 | 关联 B/X 与强度 | 关联依据及尚缺内容 |
|---|---|---|---|
| <a id="requirement-gap-doc-01"></a>`GAP-DOC-01` | [登记缺口原文](RESTRUCTURE-MAP.md#gap-doc-01)<br>物品自身的完整契约 | [B01](#case-b01) · [B18](#case-b18) · [B20](#case-b20)<br>**设计子范围** | 采用已登记缺口的原始范围对照相邻 B/X；这是本轮候选落点，不是原台账中的覆盖声明。<br>**尚缺：**物品槽位/堆叠/修复/附魔/成本返还没有逐项 B/X 断言。 |
| <a id="requirement-gap-doc-02"></a>`GAP-DOC-02` | [登记缺口原文](RESTRUCTURE-MAP.md#gap-doc-02)<br>扇形与范围几何的边界条件 | [B03](#case-b03) · [B22](#case-b22)<br>**设计子范围** | 采用已登记缺口的原始范围对照相邻 B/X；这是本轮候选落点，不是原台账中的覆盖声明。<br>**尚缺：**扇形角度、零向量、遮挡、高度、快照复验需要新增子场景；不能由身份负例代替。 |
| <a id="requirement-gap-doc-03"></a>`GAP-DOC-03` | [登记缺口原文](RESTRUCTURE-MAP.md#gap-doc-03)<br>MS 动作上下文到 Ronova 类型的逐字段映射 | [B18](#case-b18) · [B19](#case-b19) · [B22](#case-b22) · [X60](#case-x60)<br>**设计子范围** | 采用已登记缺口的原始范围对照相邻 B/X；这是本轮候选落点，不是原台账中的覆盖声明。<br>**尚缺：**actor/item/world/trigger/budget/candidates 字段所有者与过期规则尚未逐字段覆盖。 |
| <a id="requirement-gap-doc-04"></a>`GAP-DOC-04` | [登记缺口原文](RESTRUCTURE-MAP.md#gap-doc-04)<br>三份 MS 的逐症状故障定位手册 | —<br>**未定位直接项** | 采用已登记缺口的原始范围对照相邻 B/X；这是本轮候选落点，不是原台账中的覆盖声明。<br>**尚缺：**故障定位手册不是一个 B/X 运行结果；现象到日志/检查入口尚需编写。 |
| <a id="requirement-gap-map-01"></a>`GAP-MAP-01` | [登记缺口原文](RESTRUCTURE-MAP.md#gap-map-01)<br>动作七态与实体持久状态的正交映射 | [B01](#case-b01) · [B20](#case-b20) · [X38](#case-x38) · [X47](#case-x47)<br>**设计子范围** | 采用已登记缺口的原始范围对照相邻 B/X；这是本轮候选落点，不是原台账中的覆盖声明。<br>**尚缺：**动作七态与持久状态正交表尚需设计；不能用终态测试证明全状态图。 |
| <a id="requirement-gap-map-02"></a>`GAP-MAP-02` | [登记缺口原文](RESTRUCTURE-MAP.md#gap-map-02)<br>规范当前实例和修复前七项物理状态 | [B02](#case-b02) · [B03](#case-b03) · [X59](#case-x59)<br>**仅邻近/逆向，非覆盖** | 采用已登记缺口的原始范围对照相邻 B/X；这是本轮候选落点，不是原台账中的覆盖声明。<br>**尚缺：**仅邻近/逆向关联；B02 的 ERASED 不重插不是 PROTECTED A/B 实例恢复。 |
| <a id="requirement-gap-map-03"></a>`GAP-MAP-03` | [登记缺口原文](RESTRUCTURE-MAP.md#gap-map-03)<br>ID/UUID 原子修复及 active/passive 保持 | [B02](#case-b02) · [B19](#case-b19) · [X60](#case-x60)<br>**仅邻近/逆向，非覆盖** | 采用已登记缺口的原始范围对照相邻 B/X；这是本轮候选落点，不是原台账中的覆盖声明。<br>**尚缺：**B19/X60 是 Root 逻辑事务，不是 ID/UUID 集合原子修复证据；active/passive 仍需正向场景。 |
| <a id="requirement-gap-map-04"></a>`GAP-MAP-04` | [登记缺口原文](RESTRUCTURE-MAP.md#gap-map-04)<br>服务端追踪与客户端六路径独立修复 | [B02](#case-b02) · [B12](#case-b12) · [X52](#case-x52) · [X61](#case-x61) · [X62](#case-x62)<br>**仅邻近/逆向，非覆盖** | 采用已登记缺口的原始范围对照相邻 B/X；这是本轮候选落点，不是原台账中的覆盖声明。<br>**尚缺：**清除/联合显示不等于受保护各表独立补回；须两客户端追踪/查询/渲染正向矩阵。 |
| <a id="requirement-gap-map-05"></a>`GAP-MAP-05` | [登记缺口原文](RESTRUCTURE-MAP.md#gap-map-05)<br>七族读取兜底与六类修复触发点 | [B02](#case-b02) · [B21](#case-b21) · [X59](#case-x59)<br>**仅邻近/逆向，非覆盖** | 采用已登记缺口的原始范围对照相邻 B/X；这是本轮候选落点，不是原台账中的覆盖声明。<br>**尚缺：**Raw/Presence 分离不等于七族读侧兜底与六类修复触发全覆盖。 |
| <a id="requirement-gap-map-06"></a>`GAP-MAP-06` | [登记缺口原文](RESTRUCTURE-MAP.md#gap-map-06)<br>独占列表、终结过滤与瞬态抑制的边界 | [B21](#case-b21)<br>**设计子范围** | 采用已登记缺口的原始范围对照相邻 B/X；这是本轮候选落点，不是原台账中的覆盖声明。<br>**尚缺：**仅 Raw/Presence 分离关联；不自动启用独占，不默认通用实体查询负例覆盖副作用。 |
| <a id="requirement-gap-map-07"></a>`GAP-MAP-07` | [登记缺口原文](RESTRUCTURE-MAP.md#gap-map-07)<br>结果可见性、双客户端矩阵与原验收账本对接 | [B01](#case-b01) · [B02](#case-b02) · [B12](#case-b12) · [B16](#case-b16) · [B21](#case-b21) · [X47](#case-x47) · [X51](#case-x51) · [X52](#case-x52) · [X56](#case-x56) · [X61](#case-x61) · [X62](#case-x62)<br>**设计子范围** | 采用已登记缺口的原始范围对照相邻 B/X；这是本轮候选落点，不是原台账中的覆盖声明。<br>**尚缺：**28 个 MS 来源检查条目已逐项建立设计候选落点；台账原文件、两个客户端实际观察及逐项证据未取得。 |

<a id="manual-review-queue"></a>
## 需人工复核的 9 行：当前未定位直接用例

以下列表从现有 `case_ids` 为空的行生成，**不新增结论，不等于九项运行失败或九个必须新增的 B/X 用例**。阶段规则、构建一致性和文档交付项可能需要各自的审阅/产物记录；是否应新增、复用或不适用须人工明确理由。本轮没有完成这项语义审阅，也不自动将它们标为 N/A。

| 来源限定原 ID | 现有主题 | 原映射已登记的待核内容 |
|---|---|---|
| [L6:decisions:D8](#requirement-l6-decisions-d8) | 先证明替代链再移除旧机制 | 保留阶段/替代链证据要求，不强贴一个结果测试。 |
| [L6:decisions:D10](#requirement-l6-decisions-d10) | 分阶段 receipt writer 迁移 | 不得用 X49 的最终唯一 writer 结果替代各迁移阶段证据。 |
| [V26:invariants:D65](#requirement-v26-invariants-d65) | 7.16 阶段边界 | 阶段交付/变更记录需单独核查，不以其他测试的成功代替。 |
| [V26:invariants:D78](#requirement-v26-invariants-d78) | 合同实现阶段划分 | 阶段交付/变更记录需单独核查，不以其他测试的成功代替。 |
| [V26:invariants:D86](#requirement-v26-invariants-d86) | V2.4 阶段边界 | 阶段交付/变更记录需单独核查，不以其他测试的成功代替。 |
| [V26:invariants:D98](#requirement-v26-invariants-d98) | 八表面阶段划分 | 阶段交付/变更记录需单独核查，不以其他测试的成功代替。 |
| [DS:C-block:C2](#requirement-ds-c-block-c2) | 构建命名单一来源 | 没有直接 B/X 断言；须构建产物/资源路径一致性检查。 |
| [RULE:C-block:C2](#requirement-rule-c-block-c2) | 构建命名单一来源 | 没有直接 B/X 断言；须构建产物/资源路径一致性检查。 |
| [GAP-DOC-04](#requirement-gap-doc-04) | 三份 MS 的逐症状故障定位手册 | 故障定位手册不是一个 B/X 运行结果；现象到日志/检查入口尚需编写。 |

复核每条关联时要确认原来源/章节、对象与方向、触发和前置条件、断言实际能证明什么，以及适用产物和双端观察范围；没有证据的只保留候选或未定位状态，不靠编号相近认定覆盖。

<a id="ms-checks"></a>
## MS 的 28 个检查条目 → 现有 B/X 候选子范围

保留三份 zh.md 的原场景与预期；`skills:L47` 等只是来源行定位，**不是新测试 ID**。两组矩阵各自要求单人和至少两个客户端连接的专用服务器；分别记录产物/配置、目标身份、触发入口、服务器与每个客户端的观察/回执。当前没有绑定这些运行证据。

“尚缺”是在来源基础上的对接建议，不是声称已有 fixture 或已完成能力。多部位实体没有强贴 B/X；实体独占条目仅在明确选择该策略时适用，不因此默认开启。

### MS 可见结果

| 原来源 / 适用对象 | 原场景与预期（原文） | B/X 候选与条款 | 尚缺的观察 / 子场景 |
|---|---|---|---|
| <a id="ms-check-skills-l47"></a>[skills:L47](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#秒杀是什么)<br>玩家或具相应生命数据显示的目标 | 血量显示全部为0，包括但不限于data health、gethealth、后台数据显示<br>**预期：**血量显示全部为0，包括但不限于data health、gethealth、后台数据显示 | [B01](#case-b01) · [X47](#case-x47)<br>[GAP-MAP-07](#requirement-gap-map-07)<br>**设计子范围** | 内部生命后置证据不等于三处可见显示全为 0；需要同期原始观察。<br>来源结果口径；执行端/目标类型须明确，不能把可见结果等同内部证据。 |
| <a id="ms-check-skills-l48"></a>[skills:L48](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#秒杀是什么)<br>具死亡界面的玩家目标 | 显示死亡界面，且不会闪烁、缺内容<br>**预期：**显示死亡界面，且不会闪烁、缺内容 | [B01](#case-b01) · [X52](#case-x52) · [X61](#case-x61) · [X62](#case-x62)<br>[GAP-MAP-07](#requirement-gap-map-07)<br>**设计子范围** | 需实际死亡界面、无闪烁/缺内容；普通实体无玩家死亡界面，适用性须明确。<br>来源结果口径；执行端/目标类型须明确，不能把可见结果等同内部证据。 |
| <a id="ms-check-skills-l49"></a>[skills:L49](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#秒杀是什么)<br>具相应 GUI 血量显示的目标 | Gui里的血量显示为0<br>**预期：**Gui里的血量显示为0 | [B01](#case-b01) · [B12](#case-b12) · [X47](#case-x47) · [X61](#case-x61)<br>[GAP-MAP-07](#requirement-gap-map-07)<br>**设计子范围** | 需指定 GUI 与观察端，不能由 Root receipt 推导像素为 0。<br>来源结果口径；执行端/目标类型须明确，不能把可见结果等同内部证据。 |
| <a id="ms-check-skills-l52"></a>[skills:L52](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#秒杀是什么)<br>实体目标与选定查询端 | say @e 中没有<br>**预期：**say @e 中没有 | [B02](#case-b02) · [B21](#case-b21)<br>[GAP-MAP-07](#requirement-gap-map-07)<br>**设计子范围** | 需真实 say @e 观察；不要把抑制外部查询误当内部 Raw 消失。<br>来源结果口径；执行端/目标类型须明确，不能把可见结果等同内部证据。 |
| <a id="ms-check-skills-l53"></a>[skills:L53](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#秒杀是什么)<br>实体目标与命令查询端 | forge entity list 中消失<br>**预期：**forge entity list 中消失 | [B02](#case-b02) · [B21](#case-b21)<br>[GAP-MAP-07](#requirement-gap-map-07)<br>**设计子范围** | 需实际 forge entity list 查询结果与身份对照。<br>来源结果口径；执行端/目标类型须明确，不能把可见结果等同内部证据。 |
| <a id="ms-check-skills-l54"></a>[skills:L54](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#秒杀是什么)<br>客户端可观察实体计数 | F3 中的 E 中消失<br>**预期：**F3 中的 E 中消失 | [B02](#case-b02) · [B12](#case-b12) · [X52](#case-x52)<br>[GAP-MAP-07](#requirement-gap-map-07)<br>**设计子范围** | 需选定客户端 F3 E 和实体查询/渲染同期观测，计数变化不替代身份命中。<br>来源结果口径；执行端/目标类型须明确，不能把可见结果等同内部证据。 |

### MS msitem 验证矩阵

| 原来源 / 适用对象 | 原场景与预期（原文） | B/X 候选与条款 | 尚缺的观察 / 子场景 |
|---|---|---|---|
| <a id="ms-check-msitem-l378"></a>[msitem:L378](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#验证矩阵)<br>主手普通目标 | 主手单体攻击普通生物<br>**预期：**只产生一次动作、一次结果和一次资源结算。 | [B01](#case-b01) · [B18](#case-b18) · [B20](#case-b20)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C11](#requirement-ds-c-block-c11)<br>**设计子范围** | 一次动作/结果/资源结算需要同一 action 与实际资源观察。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-msitem-l379"></a>[msitem:L379](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#验证矩阵)<br>多入口重叠的同一目标 | 左键、命中后与挥击路径重叠<br>**预期：**去重后同一目标只执行一次。 | [B18](#case-b18) · [B20](#case-b20)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C11](#requirement-ds-c-block-c11)<br>**设计子范围** | Root command 去重不自动证明左键/命中/挥击入口已共用请求键。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-msitem-l380"></a>[msitem:L380](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#验证矩阵)<br>范围候选含重复/发起者 | 范围动作包含重复候选或与发起者重叠<br>**预期：**候选被去重；发起者和排除目标不被处理。 | [B03](#case-b03) · [B22](#case-b22)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C11](#requirement-ds-c-block-c11)<br>**设计子范围** | 候选集合去重和排除策略须实测，绑定负例只是相关入口。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-msitem-l381"></a>[msitem:L381](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#验证矩阵)<br>快照后状态/维度/加载性改变 | 目标在快照后死亡、换维度、卸载或已清除<br>**预期：**队列执行前丢弃任务，不抛出异常。 | [B03](#case-b03) · [B07](#case-b07) · [B17](#case-b17) · [X59](#case-x59)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C11](#requirement-ds-c-block-c11)<br>**设计子范围** | 需要排队到执行间的物理状态负例，不能只检查 lease revision。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-msitem-l382"></a>[msitem:L382](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#验证矩阵)<br>玩家/受保护主体/未知 Boss | 玩家、权限保护对象、已保护对象和未知 Boss<br>**预期：**按策略忽略或保护，不因默认分支被误处理。 | [B11](#case-b11) · [B22](#case-b22)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C11](#requirement-ds-c-block-c11)<br>**设计子范围** | 逐目标授权及默认分支负例；不按第三方身份做特判。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-msitem-l383"></a>[msitem:L383](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#验证矩阵)<br>多部位实体 | 多部位实体<br>**预期：**根实体/部位策略明确；没有重复任务、残留部位或错误 Boss 状态。 | 未定位直接项<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C11](#requirement-ds-c-block-c11)<br>**设计子范围** | 当前 B/X 未明确给出根/部位、残留部位与 Boss 状态的一组断言。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-msitem-l384"></a>[msitem:L384](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#验证矩阵)<br>正常死亡目标 | 正常死亡目标<br>**预期：**事件、归因、战利品、统计和进度按策略只发生一次。 | [B20](#case-b20)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C11](#requirement-ds-c-block-c11)<br>**设计子范围** | 需逐项事件、归因、战利品、统计、进度与 ONCE 收据。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-msitem-l385"></a>[msitem:L385](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#验证矩阵)<br>清除目标 | 清除目标<br>**预期：**服务端查询、tick、追踪、客户端查询和渲染都收敛为不存在。 | [B02](#case-b02) · [B12](#case-b12) · [X50](#case-x50) · [X52](#case-x52)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C11](#requirement-ds-c-block-c11)<br>**设计子范围** | 需服务端查询/tick/追踪和客户端查询/渲染同时闭合。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-msitem-l386"></a>[msitem:L386](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#验证矩阵)<br>只发请求/特效的独立客户端 | 客户端只发请求或只播放特效<br>**预期：**不会独立改变实体、战利品或服务器注册表。 | [X52](#case-x52)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C11](#requirement-ds-c-block-c11)<br>**设计子范围** | 需客户端无服务器状态/掉落/注册表写入负例。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-msitem-l387"></a>[msitem:L387](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#验证矩阵)<br>专用服务端启动 | 专用服务器启动<br>**预期：**公共初始化不加载任何客户端专属类。 | [X52](#case-x52)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C11](#requirement-ds-c-block-c11)<br>**设计子范围** | 必须以真实专用服类加载验证，不是构造 side 枚举。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-msitem-l388"></a>[msitem:L388](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#验证矩阵)<br>打包 JAR 与已加载目标类 | 打包后的 JAR 与已加载目标类<br>**预期：**Hook 目标、映射、转换器顺序和资源均按预期加载。 | [B05](#case-b05) · [B14](#case-b14) · [B15](#case-b15) · [B16](#case-b16)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C11](#requirement-ds-c-block-c11)<br>**设计子范围** | 需相同 artifact 的 mappings/顺序/资源/实际 class bytes 证据。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |

### MS entitydefence 验证矩阵

| 原来源 / 适用对象 | 原场景与预期（原文） | B/X 候选与条款 | 尚缺的观察 / 子场景 |
|---|---|---|---|
| <a id="ms-check-entitydefence-l288"></a>[entitydefence:L288](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#验证矩阵)<br>受保护实体 ID 表 | 在开发探针中把受保护实体从 ID 查询表移除。<br>**预期：**ID 查询通过兜底或下一次修复返回规范实体。 | [B02](#case-b02) · [X59](#case-x59)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C9](#requirement-ds-c-block-c9) · [GAP-MAP-02](#requirement-gap-map-02) · [GAP-MAP-04](#requirement-gap-map-04) · [GAP-MAP-03](#requirement-gap-map-03)<br>**仅邻近/逆向，非覆盖** | B02 是清除方向；须新增同主体 PROTECTED 规范实例返回的正向子场景。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-entitydefence-l289"></a>[entitydefence:L289](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#验证矩阵)<br>受保护实体 UUID 表 | 把它从 UUID 查询表移除。<br>**预期：**UUID 查询返回同一个对象。 | [B02](#case-b02) · [X59](#case-x59)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C9](#requirement-ds-c-block-c9) · [GAP-MAP-02](#requirement-gap-map-02) · [GAP-MAP-04](#requirement-gap-map-04) · [GAP-MAP-03](#requirement-gap-map-03)<br>**仅邻近/逆向，非覆盖** | 相同对象断言及 ID/UUID 同时一致不能由终结不重插替代。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-entitydefence-l290"></a>[entitydefence:L290](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#验证矩阵)<br>受保护实体分区 | 把它从分区存储移除。<br>**预期：**范围和按类查询重新看到它，且不重复。 | [B02](#case-b02)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C9](#requirement-ds-c-block-c9) · [GAP-MAP-02](#requirement-gap-map-02) · [GAP-MAP-04](#requirement-gap-map-04) · [GAP-MAP-03](#requirement-gap-map-03)<br>**仅邻近/逆向，非覆盖** | 须范围/按类查询恢复且无重复的正向子场景。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-entitydefence-l291"></a>[entitydefence:L291](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#验证矩阵)<br>受保护实体 active tick | 把它从 active tick 集合移除。<br>**预期：**下一次修复恢复 tick，状态修复继续执行。 | [B02](#case-b02)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C9](#requirement-ds-c-block-c9) · [GAP-MAP-02](#requirement-gap-map-02) · [GAP-MAP-04](#requirement-gap-map-04) · [GAP-MAP-03](#requirement-gap-map-03)<br>**仅邻近/逆向，非覆盖** | 须下一次修复恢复 tick、保持 active/passive 语义。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-entitydefence-l292"></a>[entitydefence:L292](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#验证矩阵)<br>服务端追踪器与两个观察客户端 | 移除服务端追踪器。<br>**预期：**追踪修复后，应观察它的客户端收到或保留该实体。 | [X52](#case-x52) · [X61](#case-x61) · [X62](#case-x62)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C9](#requirement-ds-c-block-c9) · [GAP-MAP-02](#requirement-gap-map-02) · [GAP-MAP-04](#requirement-gap-map-04)<br>**仅邻近/逆向，非覆盖** | 联合完成范围不等于追踪器恢复，需玩家跟踪集合与两个客户端观测。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-entitydefence-l293"></a>[entitydefence:L293](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#验证矩阵)<br>客户端查询/渲染枚举 | 把它从客户端查询或渲染枚举移除。<br>**预期：**客户端恢复一个可见的规范实体。 | [B12](#case-b12) · [X52](#case-x52) · [X61](#case-x61)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C9](#requirement-ds-c-block-c9) · [GAP-MAP-02](#requirement-gap-map-02) · [GAP-MAP-04](#requirement-gap-map-04)<br>**仅邻近/逆向，非覆盖** | B12 是清除方向；需客户端重建同一规范实例正向子场景。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-entitydefence-l294"></a>[entitydefence:L294](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#验证矩阵)<br>受保护实体伤害/击退/投射物/死亡/移除 | 触发伤害、击退、投射物、死亡和移除路径。<br>**预期：**状态仍受保护，注册表成员关系有效。 | [B06](#case-b06)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C9](#requirement-ds-c-block-c9) · [GAP-MAP-02](#requirement-gap-map-02) · [GAP-MAP-04](#requirement-gap-map-04)<br>**仅邻近/逆向，非覆盖** | 反全返回不足以覆盖五条物理路径，须独立路径断言。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-entitydefence-l295"></a>[entitydefence:L295](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#验证矩阵)<br>修复间隙的六类枚举/查询 | 在修复间隙查询 ID、UUID、全部实体、类型、分区和 AABB。<br>**预期：**读取兜底返回一致结果。 | [B02](#case-b02) · [B21](#case-b21)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C9](#requirement-ds-c-block-c9) · [GAP-MAP-02](#requirement-gap-map-02) · [GAP-MAP-04](#requirement-gap-map-04) · [GAP-MAP-05](#requirement-gap-map-05)<br>**仅邻近/逆向，非覆盖** | Raw/Presence 分离不证明读取兜底，须 ID/UUID/全实体/类型/分区/AABB 逐族测试。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-entitydefence-l296"></a>[entitydefence:L296](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#验证矩阵)<br>仅选定并启用独占策略时 | 启用独占列表过滤。<br>**预期：**每条选定路径都按文档过滤，未受影响系统也有明确测试。 | [B21](#case-b21)<br>[GAP-MAP-07](#requirement-gap-map-07) · [GAP-MAP-06](#requirement-gap-map-06) · [DS:C-block:C8](#requirement-ds-c-block-c8)<br>**仅邻近/逆向，非覆盖** | 条件适用，不能默认启用；需每条选定路径及未受影响系统副作用检查。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-entitydefence-l297"></a>[entitydefence:L297](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#验证矩阵)<br>重复执行规范实例修复 | 重复运行修复。<br>**预期：**不出现重复实体、列表条目、追踪器、命令目标、tick 或渲染。 | [X54](#case-x54) · [X59](#case-x59)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C9](#requirement-ds-c-block-c9) · [GAP-MAP-02](#requirement-gap-map-02) · [GAP-MAP-04](#requirement-gap-map-04)<br>**仅邻近/逆向，非覆盖** | 循环修复抑制不等于列表/追踪器/tick/命令/渲染无重复，须逐表计数。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |
| <a id="ms-check-entitydefence-l298"></a>[entitydefence:L298](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#验证矩阵)<br>重连与区块卸载/重载 | 重连并卸载/重载区块。<br>**预期：**服务端和客户端再次收敛为一个规范实体。 | [B03](#case-b03) · [B10](#case-b10) · [X52](#case-x52) · [X59](#case-x59) · [X62](#case-x62)<br>[GAP-MAP-07](#requirement-gap-map-07) · [DS:C-block:C9](#requirement-ds-c-block-c9) · [GAP-MAP-02](#requirement-gap-map-02) · [GAP-MAP-04](#requirement-gap-map-04)<br>**仅邻近/逆向，非覆盖** | 需服务端和两个客户端重新收敛为同一规范实体的联合观察。<br>单人 + 至少两个客户端连接的专用服务器；每种环境分开记录，不互相替代。 |

<a id="ledger-bind"></a>
## 原台账接线：已做与未做

| 子项 | 当前记录及证据来源 |
|---|---|
| 规格侧入口 | 68 项 B/X 规格、120 行 D/C/GAP、MS 28 项检查的来源与候选关系已经列出；不等于语义已审完。 |
| 工作区台账 ID / 状态汇总 | **据用户实际运行回报已核对**：68 个唯一 ID、缺/多/重均为 0、28 PARTIAL / 40 NOT_RUN。只对该回报所指工作区和运行生效。 |
| 本交付的独立台账复算 / 逐项状态呈现 | 没有收到原账本和明细；本地未独立复算，不从汇总分配 68 行状态。 |
| artifact / fixture / 环境 / 断言 / receipt / 双客户端观察 | **未绑定、未复核，仍待运行时与证据工作。** ID 对齐不能关闭这一侧。 |
| GAP-MAP-07 总体 | **仍开放；台账集合/汇总接线子项按用户回报已核，证据侧未闭合。** 不把“一半”换算成百分比，不新增原 GAP 编号。 |

已知工作区账本结构由用户实际调用提供：记录集合 `/cases`、ID 字段 `id`、原状态字段 `status`。在相应完整工作区可只读复核（在项目根运行）：

```powershell
python docs/plans/restructure/generate_validation_traceability.py --root . --check --ledger validation/7.17/acceptance-matrix.json --records-pointer /cases --id-field id --status-field status
```

这是用户已验证的 schema，不是为未提供文件猜字段；当前工作区有变化时，以该次真实读取为准。缺行、重复 ID、未知编号和原状态原样输出，不能由已有汇总覆盖新观察。

上述四个参数只令标准输出 JSON 的 `ledger_observation` 给出本次实际文件的原状态、计数、ID 差异和 SHA-256；不将这些状态写入本页或 `validation/`。`recorded_workspace_ledger_report` 则是另列的用户回报摘要，**两种来源不互相替代**。未给 schema 的本地文件为 `PRESENT_UNPARSED`，本次环境缺文件为 `INPUT_MISSING`；都不是关于所有工作区的断言。

<a id="reproduce"></a>
## 可重复生成与核对

在完整文档根目录或合并文档后的项目根目录运行（Python 标准库）：

```powershell
python docs/plans/restructure/verify_restructure.py
python docs/plans/restructure/generate_invariant_index.py --check
python docs/plans/restructure/generate_validation_traceability.py --check
```

本生成器的 `--write` 仅重新生成本 Markdown。核对摘录、哈希、来源、编号、关系集合、MS 原行及生成一致性；记录回报的自洽检查也**不能认证外部运行、证明人工语义或验证运行证据**。不运行 Mod、不回填验收结果。

账本观察与文档核对分别报告；未给本交付提供逐项状态不是文档核对失败，也不是验收成功。[当前核对与历史记录效力](restructure/README.md)区分本次结果和历轮证据；[收口核对记录](RESTRUCTURE-CHECK.md#review-fix3-check)不宣告 Mod 或阶段完成。
