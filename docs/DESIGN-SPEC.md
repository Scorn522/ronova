# 迁移导航：DESIGN-SPEC.md

本文件的完整原文已按 MS 三书职责逐段迁移；这里仅保留旧路径和旧标题锚点，**不再承载并行规划正文或旧规范位次**。

[当前规划入口](plans/README.md) · [不变量全集与全部编号索引](plans/INVARIANT-INDEX.md) · [本文件完整映射](plans/RESTRUCTURE-MAP.md#source-ds) · [保真原始快照](archive/plans-before-ms-restructure-r66/DESIGN-SPEC.md)

原标题、条款、ID、示例及历史状态均在新书中；本页不宣告任何实现或验证结果。

<a id="ronova-设计规范--合并导读"></a>

**旧 L1–L18：** [Ronova 设计规范 · 合并导读](plans/MS-SKILLS-PLAN.md#orig-ds-l0001)

<a id="1-权威顺序与效力"></a>

**旧 L19–L32：** [1. 权威顺序与效力](plans/MS-SKILLS-PLAN.md#orig-ds-l0019)

<a id="11-冲突处理规则五条跨文档一致"></a>

**旧 L33–L43：** [1.1 冲突处理规则（五条，跨文档一致）](plans/MS-SKILLS-PLAN.md#orig-ds-l0033)

<a id="12-违反顺序的典型错误各处点名"></a>

**旧 L44–L52：** [1.2 违反顺序的典型错误（各处点名）](plans/MS-SKILLS-PLAN.md#orig-ds-l0044)

<a id="2-冻结词汇contextmd"></a>

**旧 L53–L56：** [2. 冻结词汇（`CONTEXT.md`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0053)

<a id="21-观察与可见性"></a>

**旧 L57–L64：** [2.1 观察与可见性](plans/MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0057)

<a id="22-判定与终结语义"></a>

**旧 L65–L74：** [2.2 判定与终结语义](plans/MS-MSITEM-PLAN.md#orig-ds-l0065)

<a id="23-权威与免疫"></a>

**旧 L75–L98：** [2.3 权威与免疫](plans/MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0075)

<a id="3-身份与修订合同cf-34"></a>

**旧 L99–L100：** [3. 身份与修订合同（`CF` §3–§4）](plans/MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0099)

<a id="31-身份成员与排除cf248-254"></a>

**旧 L101–L109：** [3.1 身份成员与排除（`CF:248-254`）](plans/MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0101)

<a id="32-realmidcf213-222"></a>

**旧 L110–L114：** [3.2 `RealmId`（`CF:213-222`）](plans/MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0110)

<a id="33-physicalincarnationidcf258-278-不是身份"></a>

**旧 L115–L124：** [3.3 `PhysicalIncarnationId`（`CF:258-278`）—— **不是身份**](plans/MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0115)

<a id="34-三个-revision-的规则cf284-302"></a>

**旧 L125–L133：** [3.4 三个 revision 的规则（`CF:284-302`）](plans/MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0125)

<a id="35-revisioncrossingpolicycf538-553-三模式封闭"></a>

**旧 L134–L143：** [3.5 `RevisionCrossingPolicy`（`CF:538-553`）—— 三模式，封闭](plans/MS-SKILLS-PLAN.md#orig-ds-l0134)

<a id="36-操作与完成标识cf325-369"></a>

**旧 L144–L153：** [3.6 操作与完成标识（`CF:325-369`）](plans/MS-MSITEM-PLAN.md#orig-ds-l0144)

<a id="37-25-冻结接口集cf1297-1334-p0-a-前必须冻结的-30-个名字"></a>

**旧 L154–L165：** [3.7 §25 冻结接口集（`CF:1297-1334`）—— P0-A 前必须冻结的 30 个名字](plans/MS-SKILLS-PLAN.md#orig-ds-l0154)

<a id="4-权威与状态所有权"></a>

**旧 L166–L167：** [4. 权威与状态所有权](plans/MS-SKILLS-PLAN.md#orig-ds-l0166)

<a id="41-internal-root-不变l612-14l6174-181v26174-181"></a>

**旧 L168–L176：** [4.1 Internal Root 不变（`L6:12-14`、`L6:174-181`、`V2.6:174-181`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0168)

<a id="42-d1d13最高权威冻结决策l645-70"></a>

**旧 L177–L196：** [4.2 D1–D13（最高权威冻结决策，`L6:45-70`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0177)

<a id="43-single-writer-不变量l6415-436"></a>

**旧 L197–L204：** [4.3 Single-Writer 不变量（`L6:415-436`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0197)

<a id="44-目标状态所有权l6393-411"></a>

**旧 L205–L226：** [4.4 目标状态所有权（`L6:393-411`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0205)

<a id="45-模块不做什么清单l6337-349"></a>

**旧 L227–L242：** [4.5 模块「不做什么」清单（`L6:337-349`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0227)

<a id="46-编译期依赖约束l6487-494-可静态检查"></a>

**旧 L243–L255：** [4.6 编译期依赖约束（`L6:487-494`）—— 可静态检查](plans/MS-SKILLS-PLAN.md#orig-ds-l0243)

<a id="47-presence-保证范围l6351-357关闭-h6"></a>

**旧 L256–L264：** [4.7 Presence 保证范围（`L6:351-357`，关闭 H6）](plans/MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0256)

<a id="5-不变量全集"></a>

**恢复的人读总入口：** [按原 §5 分组导览、查看全部编号与无编号宪法级名称](plans/INVARIANT-INDEX.md#overview)。

**旧 L265–L266：** [5. 不变量全集](plans/MS-SKILLS-PLAN.md#orig-ds-l0265)

<a id="51-跨阶段必要条件-proc-inv-1-d13l6543-545"></a>

**旧 L267–L282：** [5.1 跨阶段必要条件 `PROC-INV-1`（= `D13`，`L6:543-545`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0267)

<a id="52-full-l5-不变量l6504-541"></a>

**旧 L283–L298：** [5.2 FULL L5 不变量（`L6:504-541`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0283)

<a id="53-full-l6-internal-不变量l6861-873"></a>

**旧 L299–L318：** [5.3 FULL L6 Internal 不变量（`L6:861-873`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0299)

<a id="54-d33-修订final-write-sink-不得绝对化v26472-501"></a>

**旧 L319–L328：** [5.4 `D33` 修订：Final Write Sink 不得绝对化（`V2.6:472-501`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0319)

<a id="55-d53d106v26-强不变量全部继续有效"></a>

**旧 L329–L363：** [5.5 `D53`–`D106`（`V2.6` 强不变量，全部继续有效）](plans/MS-SKILLS-PLAN.md#orig-ds-l0329)

<a id="56-d1d32-与-d33d106-之外的两组宪法级不变量"></a>

**旧 L364–L384：** [5.6 `D1`–`D32` 与 `D33`–`D106` 之外的两组宪法级不变量](plans/MS-SKILLS-PLAN.md#orig-ds-l0364)

<a id="57-p0-a-核心不变量-a1a8p0a92-197"></a>

**旧 L385–L424：** [5.7 P0-A 核心不变量 `A1`–`A8`（`P0A:92-197`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0385)

<a id="58-baseline-v14-的阶段与门bl"></a>

**旧 L425–L460：** [5.8 Baseline V1.4 的阶段与门（`BL`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0425)

<a id="6-执行与宿主边界"></a>

**旧 L461–L462：** [6. 执行与宿主边界](plans/MS-SKILLS-PLAN.md#orig-ds-l0461)

<a id="61-进程边界r7cf193-207-d13proc-inv-1"></a>

**旧 L463–L487：** [6.1 进程边界：`R7`（`CF:193-207`）+ `D13`/`PROC-INV-1`](plans/MS-SKILLS-PLAN.md#orig-ds-l0463)

<a id="62-执行上下文cf508-536"></a>

**旧 L488–L503：** [6.2 执行上下文（`CF:508-536`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0488)

<a id="63-同步查询-vs-可延期工作bl2584cf1521-1523l6733"></a>

**旧 L504–L515：** [6.3 同步查询 vs 可延期工作（`BL:2584`、`CF:1521-1523`、`L6:733`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0504)

<a id="64-预算与资源l6722-748l61188-1200"></a>

**旧 L516–L536：** [6.4 预算与资源（`L6:722-748`、`L6:1188-1200`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0516)

<a id="65-宿主安全域d81d82d96"></a>

**旧 L537–L547：** [6.5 宿主安全域（`D81`/`D82`/`D96`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0537)

<a id="66-活性与降级cf858-903cf989-1007"></a>

**旧 L548–L560：** [6.6 活性与降级（`CF:858-903`、`CF:989-1007`）](plans/MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0548)

<a id="7-极限档的绝对安全边界与代价v26-44--224"></a>

**旧 L561–L562：** [7. 极限档的绝对安全边界与代价（`V2.6` §4.4 / §22.4）](plans/MS-SKILLS-PLAN.md#orig-ds-l0561)

<a id="71-绝对安全边界--13-条v26431-445"></a>

**旧 L563–L584：** [7.1 绝对安全边界 —— 13 条（`V2.6:431-445`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0563)

<a id="72-必须计的代价v264493288"></a>

**旧 L585–L604：** [7.2 必须计的代价（`V2.6:449`、`:3288`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0585)

<a id="73-明确不保证的v26419-427"></a>

**旧 L605–L616：** [7.3 明确不保证的（`V2.6:419-427`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0605)

<a id="74-禁止写死的架构上限v26404-415"></a>

**旧 L617–L623：** [7.4 禁止写死的架构上限（`V2.6:404-415`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0617)

<a id="75-资源与容量v26-122--146--20"></a>

**旧 L624–L647：** [7.5 资源与容量（`V2.6` §12.2 / §14.6 / §20）](plans/MS-SKILLS-PLAN.md#orig-ds-l0624)

<a id="76-极限档非免疫的明确声明"></a>

**旧 L648–L658：** [7.6 极限档"非免疫"的明确声明](plans/MS-SKILLS-PLAN.md#orig-ds-l0648)

<a id="8-客户端--服务端边界"></a>

**旧 L659–L676：** [8. 客户端 / 服务端边界](plans/MS-SKILLS-PLAN.md#orig-ds-l0659)

<a id="9-副作用合同cf-16--id-2"></a>

**旧 L677–L697：** [9. 副作用合同（`CF` §16 + `ID-2`）](plans/MS-MSITEM-PLAN.md#orig-ds-l0677)

<a id="10-强度模型v26"></a>

**旧 L698–L707：** [10. 强度模型（`V2.6`）](plans/MS-MSITEM-PLAN.md#orig-ds-l0698)

<a id="101-三档正式预设v2641-45600-604"></a>

**旧 L708–L720：** [10.1 三档正式预设（`V2.6:41-45`、`:600-604`）](plans/MS-MSITEM-PLAN.md#orig-ds-l0708)

<a id="102-两套不同的层级值集不要合并"></a>

**旧 L721–L729：** [10.2 两套**不同**的层级值集（不要合并）](plans/MS-MSITEM-PLAN.md#orig-ds-l0721)

<a id="103-五个策略字段与组合算子v26649-673765-774"></a>

**旧 L730–L768：** [10.3 五个策略字段与组合算子（`V2.6:649-673`、`:765-774`）](plans/MS-MSITEM-PLAN.md#orig-ds-l0730)

<a id="104-表面成熟度阶梯v261259-1267"></a>

**旧 L769–L786：** [10.4 表面成熟度阶梯（`V2.6:1259-1267`）](plans/MS-MSITEM-PLAN.md#orig-ds-l0769)

<a id="105-逐键矩阵与三档差异性d91v26877"></a>

**旧 L787–L795：** [10.5 逐键矩阵与三档差异性（`D91`、`V2.6:877`）](plans/MS-MSITEM-PLAN.md#orig-ds-l0787)

<a id="106-attackaction-的逐档许可v26895-910"></a>

**旧 L796–L810：** [10.6 `AttackAction` 的逐档许可（`V2.6:895-910`）](plans/MS-MSITEM-PLAN.md#orig-ds-l0796)

<a id="11-非法组合v262828-2885"></a>

**旧 L811–L843：** [11. 非法组合（`V2.6:2828-2885`）](plans/MS-SKILLS-PLAN.md#orig-ds-l0811)

<a id="12-完成判定的元原则"></a>

**旧 L844–L870：** [12. 完成判定的元原则](plans/MS-SKILLS-PLAN.md#orig-ds-l0844)

<a id="13-覆盖进度"></a>

**旧 L871–L889：** [13. 覆盖进度](plans/MS-SKILLS-PLAN.md#orig-ds-l0871)

<a id="14-已知的文档级问题如实记录不代为裁决"></a>

**旧 L890–L906：** [14. 已知的文档级问题（如实记录，不代为裁决）](plans/MS-SKILLS-PLAN.md#orig-ds-l0890)

<a id="附本导读不做什么"></a>

**旧 L907–L917：** [附：本导读不做什么](plans/MS-SKILLS-PLAN.md#orig-ds-l0907)

<a id="第二部分--加强方案提案非规范"></a>

**旧 L918–L931：** [第二部分 · 加强方案（**提案，非规范**）](plans/MS-SKILLS-PLAN.md#orig-ds-l0918)

<a id="15-三层规则源判定力完全不同"></a>

**旧 L932–L947：** [15. 三层规则源（判定力完全不同）](plans/MS-SKILLS-PLAN.md#orig-ds-l0932)

<a id="16-合规判定"></a>

**旧 L948–L949：** [16. 合规判定](plans/MS-SKILLS-PLAN.md#orig-ds-l0948)

<a id="161-对外l0-铁律--ux1"></a>

**旧 L950–L961：** [16.1 对外（L0 铁律 + UX1）](plans/MS-SKILLS-PLAN.md#orig-ds-l0950)

<a id="162-对内p-项目自有规范-本轮整合新增的维度"></a>

**旧 L962–L985：** [16.2 对内（P 项目自有规范）—— 本轮整合新增的维度](plans/MS-SKILLS-PLAN.md#orig-ds-l0962)

<a id="163-跨所有权-saveddata-写入--结论不违反-l0"></a>

**旧 L986–L1002：** [16.3 跨所有权 `SavedData` 写入 —— 结论：不违反 L0](plans/MS-SKILLS-PLAN.md#orig-ds-l0986)

<a id="17-与项目自有规范对齐后的加强块"></a>

**旧 L1003–L1006：** [17. 与项目自有规范对齐后的加强块](plans/MS-SKILLS-PLAN.md#orig-ds-l1003)

<a id="a-块--前置与入口服务-ux1--r7--proc-inv-1"></a>

**旧 L1007–L1020：** [A 块 —— 前置与入口（服务 `UX1` + `R7` + `PROC-INV-1`）](plans/MS-SKILLS-PLAN.md#orig-ds-l1007)

<a id="b-块--原生能力位扩展纯增量"></a>

**旧 L1021–L1039：** [B 块 —— 原生能力位扩展（纯增量）](plans/MS-SKILLS-PLAN.md#orig-ds-l1021)

<a id="c-块--收口项"></a>

**旧 L1040–L1065：** [C 块 —— 收口项](plans/MS-SKILLS-PLAN.md#orig-ds-l1040)

<a id="d-块--执行器与结算能力本体"></a>

**旧 L1066–L1071：** [D 块 —— 执行器与结算（能力本体）](plans/MS-MSITEM-PLAN.md#orig-ds-l1066)

<a id="e-块--图与证书"></a>

**旧 L1072–L1082：** [E 块 —— 图与证书](plans/MS-SKILLS-PLAN.md#orig-ds-l1072)

<a id="f-块--证明与策略收口"></a>

**旧 L1083–L1099：** [F 块 —— 证明与策略收口](plans/MS-SKILLS-PLAN.md#orig-ds-l1083)

<a id="g-块--原生强制下沉"></a>

**旧 L1100–L1106：** [G 块 —— 原生强制下沉](plans/MS-SKILLS-PLAN.md#orig-ds-l1100)

<a id="h-块--新维度"></a>

**旧 L1107–L1114：** [H 块 —— 新维度](plans/MS-SKILLS-PLAN.md#orig-ds-l1107)

<a id="i-块--ms-未禁止但本方案不建议优先做的"></a>

**旧 L1115–L1125：** [I 块 —— MS 未禁止、但本方案**不建议**优先做的](plans/MS-SKILLS-PLAN.md#orig-ds-l1115)

<a id="18-实施顺序"></a>

**旧 L1126–L1150：** [18. 实施顺序](plans/MS-SKILLS-PLAN.md#orig-ds-l1126)

<a id="19-验证全部后置"></a>

**旧 L1151–L1159：** [19. 验证：全部后置](plans/MS-SKILLS-PLAN.md#orig-ds-l1151)

<a id="20-完成判定"></a>

**旧 L1160–L1169：** [20. 完成判定](plans/MS-SKILLS-PLAN.md#orig-ds-l1160)
