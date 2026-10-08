# 迁移导航：Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md

本文件的完整原文已按 MS 三书职责逐段迁移；这里仅保留旧路径和旧标题锚点，**不再承载并行规划正文或旧规范位次**。

[当前规划入口](README.md) · [本文件完整映射](RESTRUCTURE-MAP.md#source-v26) · [保真原始快照](../archive/plans-before-ms-restructure-r66/Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md)

原标题、条款、ID、示例及历史状态均在新书中；本页不宣告任何实现或验证结果。

<a id="ronova-716-jvm-极限强度加强总纲-v26internal-root"></a>

**旧 L1–L36：** [Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root）](MS-SKILLS-PLAN.md#orig-v26-l0001)

<a id="0-最终结论"></a>

**旧 L37–L129：** [0. 最终结论](MS-SKILLS-PLAN.md#orig-v26-l0037)

<a id="1-文档权威与版本关系"></a>

**旧 L130–L131：** [1. 文档权威与版本关系](MS-SKILLS-PLAN.md#orig-v26-l0130)

<a id="11-历史版本"></a>

**旧 L132–L155：** [1.1 历史版本](MS-SKILLS-PLAN.md#orig-v26-l0132)

<a id="12-不回写已完成阶段"></a>

**旧 L156–L173：** [1.2 不回写已完成阶段](MS-SKILLS-PLAN.md#orig-v26-l0156)

<a id="13-internal-root-不变"></a>

**旧 L174–L184：** [1.3 Internal Root 不变](MS-SKILLS-PLAN.md#orig-v26-l0174)

<a id="2-r51--715-冻结基线"></a>

**旧 L185–L211：** [2. r51 / 7.15 冻结基线](MS-SKILLS-PLAN.md#orig-v26-l0185)

<a id="3-正式术语"></a>

**旧 L212–L213：** [3. 正式术语](MS-SKILLS-PLAN.md#orig-v26-l0212)

<a id="31-攻击--拦截--防护"></a>

**旧 L214–L228：** [3.1 攻击 / 拦截 / 防护](MS-SKILLS-PLAN.md#orig-v26-l0214)

<a id="32-三种全返回"></a>

**旧 L229–L238：** [3.2 三种全返回](MS-SKILLS-PLAN.md#orig-v26-l0229)

<a id="33-极限代码对抗"></a>

**旧 L239–L257：** [3.3 极限代码对抗](MS-SKILLS-PLAN.md#orig-v26-l0239)

<a id="34-candidate-attack-surface--候选攻击面"></a>

**旧 L258–L294：** [3.4 Candidate Attack Surface / 候选攻击面](MS-MSITEM-PLAN.md#orig-v26-l0258)

<a id="35-证明决定处置级别不决定范围"></a>

**旧 L295–L324：** [3.5 证明决定处置级别，不决定范围](MS-MSITEM-PLAN.md#orig-v26-l0295)

<a id="36-existence-termination--存在性终结"></a>

**旧 L325–L340：** [3.6 Existence Termination / 存在性终结](MS-MSITEM-PLAN.md#orig-v26-l0325)

<a id="37-resultgraph--certificate-的权威边界"></a>

**旧 L341–L360：** [3.7 ResultGraph / Certificate 的权威边界](MS-SKILLS-PLAN.md#orig-v26-l0341)

<a id="4-目标非目标与宿主安全边界"></a>

**旧 L361–L362：** [4. 目标、非目标与宿主安全边界](MS-SKILLS-PLAN.md#orig-v26-l0361)

<a id="41-三档目标范围"></a>

**旧 L363–L364：** [4.1 三档目标范围](MS-MSITEM-PLAN.md#orig-v26-l0363)

<a id="compatible--兼容"></a>

**旧 L365–L374：** [COMPATIBLE / 兼容](MS-MSITEM-PLAN.md#orig-v26-l0365)

<a id="enhanced--强化"></a>

**旧 L375–L388：** [ENHANCED / 强化](MS-MSITEM-PLAN.md#orig-v26-l0375)

<a id="extreme--极限代码对抗"></a>

**旧 L389–L401：** [EXTREME / 极限代码对抗](MS-MSITEM-PLAN.md#orig-v26-l0389)

<a id="42-extreme-不设置的限制"></a>

**旧 L402–L416：** [4.2 Extreme 不设置的限制](MS-SKILLS-PLAN.md#orig-v26-l0402)

<a id="43-extreme-允许牺牲的东西"></a>

**旧 L417–L428：** [4.3 Extreme 允许牺牲的东西](MS-SKILLS-PLAN.md#orig-v26-l0417)

<a id="44-extreme-绝对安全边界"></a>

**旧 L429–L452：** [4.4 Extreme 绝对安全边界](MS-SKILLS-PLAN.md#orig-v26-l0429)

<a id="5-核心不变量"></a>

**旧 L453–L454：** [5. 核心不变量](MS-SKILLS-PLAN.md#orig-v26-l0453)

<a id="51-继承不变量-d1d32"></a>

**旧 L455–L471：** [5.1 继承不变量 D1–D32](MS-SKILLS-PLAN.md#orig-v26-l0455)

<a id="52-d33-修订final-write-sink-不得绝对化"></a>

**旧 L472–L502：** [5.2 D33 修订：Final Write Sink 不得绝对化](MS-SKILLS-PLAN.md#orig-v26-l0472)

<a id="53-v22-新增强不变量继续有效"></a>

**旧 L503–L520：** [5.3 V2.2 新增强不变量（继续有效）](MS-SKILLS-PLAN.md#orig-v26-l0503)

<a id="54-v23-执行合同不变量"></a>

**旧 L521–L538：** [5.4 V2.3 执行合同不变量](MS-SKILLS-PLAN.md#orig-v26-l0521)

<a id="55-v24-design-freeze-不变量"></a>

**旧 L539–L551：** [5.5 V2.4 Design Freeze 不变量](MS-SKILLS-PLAN.md#orig-v26-l0539)

<a id="56-v25-runtime-surface-不变量"></a>

**旧 L552–L570：** [5.6 V2.5 Runtime Surface 不变量](MS-SKILLS-PLAN.md#orig-v26-l0552)

<a id="57-v26-接合合同不变量"></a>

**旧 L571–L583：** [5.7 V2.6 接合合同不变量](MS-SKILLS-PLAN.md#orig-v26-l0571)

<a id="6-强度模型"></a>

**旧 L584–L585：** [6. 强度模型](MS-MSITEM-PLAN.md#orig-v26-l0584)

<a id="61-顶层预设"></a>

**旧 L586–L605：** [6.1 顶层预设](MS-MSITEM-PLAN.md#orig-v26-l0586)

<a id="62-三档正式范围"></a>

**旧 L606–L613：** [6.2 三档正式范围](MS-MSITEM-PLAN.md#orig-v26-l0606)

<a id="63-子项档位"></a>

**旧 L614–L644：** [6.3 子项档位](MS-MSITEM-PLAN.md#orig-v26-l0614)

<a id="64-配置值与有效值"></a>

**旧 L645–L674：** [6.4 配置值与有效值](MS-MSITEM-PLAN.md#orig-v26-l0645)

<a id="65-execution-state-与-attack-outcome"></a>

**旧 L675–L760：** [6.5 Execution State 与 Attack Outcome](MS-MSITEM-PLAN.md#orig-v26-l0675)

<a id="66-v25-逐能力-profilepresetmatrix"></a>

**旧 L761–L777：** [6.6 V2.5 逐能力 ProfilePresetMatrix](MS-MSITEM-PLAN.md#orig-v26-l0761)

<a id="661-玩家可见攻击模块"></a>

**旧 L778–L799：** [6.6.1 玩家可见攻击模块](MS-MSITEM-PLAN.md#orig-v26-l0778)

<a id="662-十五项高级攻击政策"></a>

**旧 L800–L819：** [6.6.2 十五项高级攻击政策](MS-MSITEM-PLAN.md#orig-v26-l0800)

<a id="663-拦截机制矩阵"></a>

**旧 L820–L834：** [6.6.3 拦截机制矩阵](MS-SKILLS-PLAN.md#orig-v26-l0820)

<a id="664-穿透后防护矩阵"></a>

**旧 L835–L856：** [6.6.4 穿透后防护矩阵](MS-SKILLS-PLAN.md#orig-v26-l0835)

<a id="665-运行时支撑矩阵"></a>

**旧 L857–L873：** [6.6.5 运行时支撑矩阵](MS-SKILLS-PLAN.md#orig-v26-l0857)

<a id="666-矩阵正确性要求"></a>

**旧 L874–L883：** [6.6.6 矩阵正确性要求](MS-MSITEM-PLAN.md#orig-v26-l0874)

<a id="67-普通开关到执行权限的唯一映射v26"></a>

**旧 L884–L930：** [6.7 普通开关到执行权限的唯一映射（V2.6）](MS-MSITEM-PLAN.md#orig-v26-l0884)

<a id="7-gui-正式结构"></a>

**旧 L931–L932：** [7. GUI 正式结构](MS-MSITEM-PLAN.md#orig-v26-l0931)

<a id="71-顶层"></a>

**旧 L933–L953：** [7.1 顶层](MS-MSITEM-PLAN.md#orig-v26-l0933)

<a id="72-攻击页"></a>

**旧 L954–L1017：** [7.2 攻击页](MS-MSITEM-PLAN.md#orig-v26-l0954)

<a id="73-拦截页"></a>

**旧 L1018–L1053：** [7.3 拦截页](MS-MSITEM-PLAN.md#orig-v26-l1018)

<a id="74-防护页"></a>

**旧 L1054–L1080：** [7.4 防护页](MS-MSITEM-PLAN.md#orig-v26-l1054)

<a id="75-运行时页"></a>

**旧 L1081–L1104：** [7.5 运行时页](MS-MSITEM-PLAN.md#orig-v26-l1081)

<a id="76-功能页"></a>

**旧 L1105–L1108：** [7.6 功能页](MS-MSITEM-PLAN.md#orig-v26-l1105)

<a id="77-v25-界面收敛规则"></a>

**旧 L1109–L1124：** [7.7 V2.5 界面收敛规则](MS-MSITEM-PLAN.md#orig-v26-l1109)

<a id="8-选项政策键与阶段"></a>

**旧 L1125–L1126：** [8. 选项、政策键与阶段](MS-SKILLS-PLAN.md#orig-v26-l1125)

<a id="81-v22-攻击政策v23v24v25-沿用"></a>

**旧 L1127–L1153：** [8.1 V2.2 攻击政策（V2.3/V2.4/V2.5 沿用）](MS-MSITEM-PLAN.md#orig-v26-l1127)

<a id="82-攻击来源追踪--mutationtrace"></a>

**旧 L1154–L1172：** [8.2 攻击来源追踪 / MutationTrace](MS-SKILLS-PLAN.md#orig-v26-l1154)

<a id="83-716-scope-lock"></a>

**旧 L1173–L1199：** [8.3 7.16 Scope Lock](MS-SKILLS-PLAN.md#orig-v26-l1173)

<a id="84-四项执行合同的阶段归属"></a>

**旧 L1200–L1210：** [8.4 四项执行合同的阶段归属](MS-SKILLS-PLAN.md#orig-v26-l1200)

<a id="85-v24-收口合同的阶段归属"></a>

**旧 L1211–L1221：** [8.5 V2.4 收口合同的阶段归属](MS-SKILLS-PLAN.md#orig-v26-l1211)

<a id="86-v25-runtime-surface-taxonomy"></a>

**旧 L1222–L1254：** [8.6 V2.5 Runtime Surface Taxonomy](MS-SKILLS-PLAN.md#orig-v26-l1222)

<a id="861-广度与深度等高"></a>

**旧 L1255–L1272：** [8.6.1 广度与深度等高](MS-SKILLS-PLAN.md#orig-v26-l1255)

<a id="9-攻击平面"></a>

**旧 L1273–L1274：** [9. 攻击平面](MS-MSITEM-PLAN.md#orig-v26-l1273)

<a id="91-三档攻击范围"></a>

**旧 L1275–L1276：** [9.1 三档攻击范围](MS-MSITEM-PLAN.md#orig-v26-l1275)

<a id="compatible"></a>

**旧 L1277–L1287：** [Compatible](MS-MSITEM-PLAN.md#orig-v26-l1277)

<a id="enhanced"></a>

**旧 L1288–L1299：** [Enhanced](MS-MSITEM-PLAN.md#orig-v26-l1288)

<a id="extreme"></a>

**旧 L1300–L1326：** [Extreme](MS-MSITEM-PLAN.md#orig-v26-l1300)

<a id="92-candidate-first-执行"></a>

**旧 L1327–L1361：** [9.2 Candidate-first 执行](MS-MSITEM-PLAN.md#orig-v26-l1327)

<a id="921-candidate-admission-rule"></a>

**旧 L1362–L1396：** [9.2.1 Candidate Admission Rule](MS-MSITEM-PLAN.md#orig-v26-l1362)

<a id="922-独立性有效期与合法操作"></a>

**旧 L1397–L1412：** [9.2.2 独立性、有效期与合法操作](MS-MSITEM-PLAN.md#orig-v26-l1397)

<a id="923-候选处置权限表"></a>

**旧 L1413–L1424：** [9.2.3 候选处置权限表](MS-MSITEM-PLAN.md#orig-v26-l1413)

<a id="924-准入与处置回执"></a>

**旧 L1425–L1441：** [9.2.4 准入与处置回执](MS-MSITEM-PLAN.md#orig-v26-l1425)

<a id="925-candidate-临时限制释放防抖"></a>

**旧 L1442–L1487：** [9.2.5 Candidate 临时限制释放防抖](MS-MSITEM-PLAN.md#orig-v26-l1442)

<a id="93-extreme-不因共享路径自动降级"></a>

**旧 L1488–L1505：** [9.3 Extreme 不因共享路径自动降级](MS-MSITEM-PLAN.md#orig-v26-l1488)

<a id="94-极限清除--existence-termination"></a>

**旧 L1506–L1545：** [9.4 极限清除 / Existence Termination](MS-MSITEM-PLAN.md#orig-v26-l1506)

<a id="941-completion-参与范围与客户端边界v26"></a>

**旧 L1546–L1568：** [9.4.1 Completion 参与范围与客户端边界（V2.6）](MS-MSITEM-PLAN.md#orig-v26-l1546)

<a id="942-通用恢复链处置2026-09-08-规划增量"></a>

**旧 L1569–L1587：** [9.4.2 通用恢复链处置（2026-09-08 规划增量）](MS-MSITEM-PLAN.md#orig-v26-l1569)

<a id="95-targetepoch-资格代际"></a>

**旧 L1588–L1664：** [9.5 TargetEpoch 资格代际](MS-SKILLS-PLAN.md#orig-v26-l1588)

<a id="951-canonical-identity-与-revision-对照"></a>

**旧 L1665–L1684：** [9.5.1 Canonical identity 与 revision 对照](MS-ENTITYDEFENCE-PLAN.md#orig-v26-l1665)

<a id="952-多目标-qualificationbatch-原子性"></a>

**旧 L1685–L1696：** [9.5.2 多目标 QualificationBatch 原子性](MS-SKILLS-PLAN.md#orig-v26-l1685)

<a id="96-全权威结果闭包"></a>

**旧 L1697–L1717：** [9.6 全权威结果闭包](MS-MSITEM-PLAN.md#orig-v26-l1697)

<a id="97-代码防御剥离"></a>

**旧 L1718–L1732：** [9.7 代码防御剥离](MS-SKILLS-PLAN.md#orig-v26-l1718)

<a id="98-攻击来源追踪"></a>

**旧 L1733–L1761：** [9.8 攻击来源追踪](MS-SKILLS-PLAN.md#orig-v26-l1733)

<a id="10-拦截平面"></a>

**旧 L1762–L1763：** [10. 拦截平面](MS-SKILLS-PLAN.md#orig-v26-l1762)

<a id="101-统一机制"></a>

**旧 L1764–L1788：** [10.1 统一机制](MS-SKILLS-PLAN.md#orig-v26-l1764)

<a id="102-extreme-全部严格"></a>

**旧 L1789–L1820：** [10.2 Extreme “全部严格”](MS-SKILLS-PLAN.md#orig-v26-l1789)

<a id="103-全部严格不是全-jvm-随机停机"></a>

**旧 L1821–L1836：** [10.3 “全部严格”不是全 JVM 随机停机](MS-SKILLS-PLAN.md#orig-v26-l1821)

<a id="11-防护平面"></a>

**旧 L1837–L1838：** [11. 防护平面](MS-SKILLS-PLAN.md#orig-v26-l1837)

<a id="111-反拦截"></a>

**旧 L1839–L1862：** [11.1 反拦截](MS-SKILLS-PLAN.md#orig-v26-l1839)

<a id="112-反全返回四层"></a>

**旧 L1863–L1884：** [11.2 反全返回四层](MS-SKILLS-PLAN.md#orig-v26-l1863)

<a id="113-fence-scope-escalation"></a>

**旧 L1885–L1898：** [11.3 Fence Scope Escalation](MS-SKILLS-PLAN.md#orig-v26-l1885)

<a id="target_fenced"></a>

**旧 L1899–L1902：** [TARGET_FENCED](MS-SKILLS-PLAN.md#orig-v26-l1899)

<a id="operation_fenced"></a>

**旧 L1903–L1906：** [OPERATION_FENCED](MS-SKILLS-PLAN.md#orig-v26-l1903)

<a id="plane_fenced"></a>

**旧 L1907–L1910：** [PLANE_FENCED](MS-SKILLS-PLAN.md#orig-v26-l1907)

<a id="root_fenced"></a>

**旧 L1911–L1921：** [ROOT_FENCED](MS-SKILLS-PLAN.md#orig-v26-l1911)

<a id="realm_fenced"></a>

**旧 L1922–L1929：** [REALM_FENCED](MS-SKILLS-PLAN.md#orig-v26-l1922)

<a id="114-fence-不是放弃攻击"></a>

**旧 L1930–L1950：** [11.4 FENCE 不是“放弃攻击”](MS-SKILLS-PLAN.md#orig-v26-l1930)

<a id="115-隔离恢复与攻击结果的独立更新"></a>

**旧 L1951–L1973：** [11.5 隔离、恢复与攻击结果的独立更新](MS-SKILLS-PLAN.md#orig-v26-l1951)

<a id="12-运行时与宿主安全"></a>

**旧 L1974–L1975：** [12. 运行时与宿主安全](MS-SKILLS-PLAN.md#orig-v26-l1974)

<a id="121-runtime-capability-ceiling"></a>

**旧 L1976–L1993：** [12.1 Runtime Capability Ceiling](MS-SKILLS-PLAN.md#orig-v26-l1976)

<a id="122-host-safety-governor"></a>

**旧 L1994–L2063：** [12.2 Host Safety Governor](MS-SKILLS-PLAN.md#orig-v26-l1994)

<a id="123-不设-mod-数量硬上限"></a>

**旧 L2064–L2084：** [12.3 不设 Mod 数量硬上限](MS-SKILLS-PLAN.md#orig-v26-l2064)

<a id="124-candidate-反诱导与公平调度"></a>

**旧 L2085–L2133：** [12.4 Candidate 反诱导与公平调度](MS-SKILLS-PLAN.md#orig-v26-l2085)

<a id="125-cross-surface-operation-plan"></a>

**旧 L2134–L2181：** [12.5 Cross-surface Operation Plan](MS-MSITEM-PLAN.md#orig-v26-l2134)

<a id="126-副作用未知与跨版本去重v26"></a>

**旧 L2182–L2223：** [12.6 副作用未知与跨版本去重（V2.6）](MS-MSITEM-PLAN.md#orig-v26-l2182)

<a id="13-evidence--lease--toctou"></a>

**旧 L2224–L2225：** [13. Evidence / Lease / TOCTOU](MS-SKILLS-PLAN.md#orig-v26-l2224)

<a id="131-evidence"></a>

**旧 L2226–L2243：** [13.1 Evidence](MS-SKILLS-PLAN.md#orig-v26-l2226)

<a id="132-executionlease"></a>

**旧 L2244–L2267：** [13.2 ExecutionLease](MS-SKILLS-PLAN.md#orig-v26-l2244)

<a id="133-三次复验--真实提交门"></a>

**旧 L2268–L2285：** [13.3 三次复验 + 真实提交门](MS-MSITEM-PLAN.md#orig-v26-l2268)

<a id="134-qualification-transition-commit-gate"></a>

**旧 L2286–L2307：** [13.4 Qualification Transition Commit Gate](MS-SKILLS-PLAN.md#orig-v26-l2286)

<a id="14-resultgraph-与-candidategraph"></a>

**旧 L2308–L2309：** [14. ResultGraph 与 CandidateGraph](MS-SKILLS-PLAN.md#orig-v26-l2308)

<a id="141-candidategraph"></a>

**旧 L2310–L2322：** [14.1 CandidateGraph](MS-SKILLS-PLAN.md#orig-v26-l2310)

<a id="142-resultgraph"></a>

**旧 L2323–L2334：** [14.2 ResultGraph](MS-SKILLS-PLAN.md#orig-v26-l2323)

<a id="143-graph-revision"></a>

**旧 L2335–L2347：** [14.3 Graph Revision](MS-SKILLS-PLAN.md#orig-v26-l2335)

<a id="144-图预算耗尽"></a>

**旧 L2348–L2367：** [14.4 图预算耗尽](MS-SKILLS-PLAN.md#orig-v26-l2348)

<a id="145-obligation-compaction--规范义务归并"></a>

**旧 L2368–L2419：** [14.5 Obligation Compaction / 规范义务归并](MS-SKILLS-PLAN.md#orig-v26-l2368)

<a id="146-不同-key-洪泛与范围汇总"></a>

**旧 L2420–L2462：** [14.6 不同 key 洪泛与范围汇总](MS-SKILLS-PLAN.md#orig-v26-l2420)

<a id="147-压缩淘汰与限制交接"></a>

**旧 L2463–L2478：** [14.7 压缩、淘汰与限制交接](MS-SKILLS-PLAN.md#orig-v26-l2463)

<a id="148-义务解除与证据权限"></a>

**旧 L2479–L2492：** [14.8 义务解除与证据权限](MS-SKILLS-PLAN.md#orig-v26-l2479)

<a id="15-authority持久化与恢复"></a>

**旧 L2493–L2494：** [15. Authority、持久化与恢复](MS-SKILLS-PLAN.md#orig-v26-l2493)

<a id="151-权威对象"></a>

**旧 L2495–L2504：** [15.1 权威对象](MS-SKILLS-PLAN.md#orig-v26-l2495)

<a id="152-resultgraph--certificate-不写-truth"></a>

**旧 L2505–L2526：** [15.2 ResultGraph / Certificate 不写 Truth](MS-SKILLS-PLAN.md#orig-v26-l2505)

<a id="153-restart"></a>

**旧 L2527–L2540：** [15.3 Restart](MS-SKILLS-PLAN.md#orig-v26-l2527)

<a id="154-v23-未决范围与重启恢复"></a>

**旧 L2541–L2550：** [15.4 V2.3 未决范围与重启恢复](MS-SKILLS-PLAN.md#orig-v26-l2541)

<a id="155-qualification-transaction-崩溃恢复"></a>

**旧 L2551–L2575：** [15.5 Qualification Transaction 崩溃恢复](MS-SKILLS-PLAN.md#orig-v26-l2551)

<a id="156-runtime-surface-持久化边界"></a>

**旧 L2576–L2602：** [15.6 Runtime Surface 持久化边界](MS-SKILLS-PLAN.md#orig-v26-l2576)

<a id="157-执行效果恢复次序v26"></a>

**旧 L2603–L2612：** [15.7 执行效果恢复次序（V2.6）](MS-SKILLS-PLAN.md#orig-v26-l2603)

<a id="16-分阶段实施路线"></a>

**旧 L2613–L2614：** [16. 分阶段实施路线](MS-SKILLS-PLAN.md#orig-v26-l2613)

<a id="161-716--不返工原则"></a>

**旧 L2615–L2649：** [16.1 7.16 — 不返工原则](MS-SKILLS-PLAN.md#orig-v26-l2615)

<a id="162-717--enforcement-foundation"></a>

**旧 L2650–L2702：** [16.2 7.17 — Enforcement Foundation](MS-SKILLS-PLAN.md#orig-v26-l2650)

<a id="163-l6--internal-dominion-root"></a>

**旧 L2703–L2716：** [16.3 L6 — Internal Dominion Root](MS-SKILLS-PLAN.md#orig-v26-l2703)

<a id="164-l6e--extreme-closure"></a>

**旧 L2717–L2718：** [16.4 L6E — Extreme Closure](MS-SKILLS-PLAN.md#orig-v26-l2717)

<a id="p0-a"></a>

**旧 L2719–L2724：** [P0-A](MS-SKILLS-PLAN.md#orig-v26-l2719)

<a id="p0-b"></a>

**旧 L2725–L2737：** [P0-B](MS-SKILLS-PLAN.md#orig-v26-l2725)

<a id="p0-c"></a>

**旧 L2738–L2749：** [P0-C](MS-SKILLS-PLAN.md#orig-v26-l2738)

<a id="p0-d"></a>

**旧 L2750–L2762：** [P0-D](MS-SKILLS-PLAN.md#orig-v26-l2750)

<a id="165-v25-runtime-surface-阶段归属"></a>

**旧 L2763–L2779：** [16.5 V2.5 Runtime Surface 阶段归属](MS-SKILLS-PLAN.md#orig-v26-l2763)

<a id="166-717-可交付批次与出口v26"></a>

**旧 L2780–L2795：** [16.6 7.17 可交付批次与出口（V2.6）](MS-SKILLS-PLAN.md#orig-v26-l2780)

<a id="17-能力阶段映射"></a>

**旧 L2796–L2827：** [17. 能力—阶段映射](MS-SKILLS-PLAN.md#orig-v26-l2796)

<a id="18-依赖与非法组合"></a>

**旧 L2828–L2888：** [18. 依赖与非法组合](MS-SKILLS-PLAN.md#orig-v26-l2828)

<a id="19-验收体系"></a>

**旧 L2889–L2890：** [19. 验收体系](MS-SKILLS-PLAN.md#orig-v26-l2889)

<a id="191-extreme-code-duel-必测"></a>

**旧 L2891–L2894：** [19.1 Extreme Code Duel 必测](MS-SKILLS-PLAN.md#orig-v26-l2891)

<a id="1911-基础验收自包含表与历史缺件"></a>

**旧 L2895–L2926：** [19.1.1 基础验收自包含表与历史缺件](MS-SKILLS-PLAN.md#orig-v26-l2895)

<a id="x24--candidate-default-in"></a>

**旧 L2927–L2935：** [X24 — Candidate Default-in](MS-SKILLS-PLAN.md#orig-v26-l2927)

<a id="x25--proof-controls-severity"></a>

**旧 L2936–L2953：** [X25 — Proof Controls Severity](MS-SKILLS-PLAN.md#orig-v26-l2936)

<a id="x26--fence-scope"></a>

**旧 L2954–L2969：** [X26 — Fence Scope](MS-SKILLS-PLAN.md#orig-v26-l2954)

<a id="x27--legal-reactivation"></a>

**旧 L2970–L2980：** [X27 — Legal Reactivation](MS-SKILLS-PLAN.md#orig-v26-l2970)

<a id="x28--budget-persistence"></a>

**旧 L2981–L2991：** [X28 — Budget Persistence](MS-SKILLS-PLAN.md#orig-v26-l2981)

<a id="x29--multi-opponent-scale"></a>

**旧 L2992–L3000：** [X29 — Multi-opponent Scale](MS-SKILLS-PLAN.md#orig-v26-l2992)

<a id="x30--host-safety"></a>

**旧 L3001–L3012：** [X30 — Host Safety](MS-SKILLS-PLAN.md#orig-v26-l3001)

<a id="192-completion-gate"></a>

**旧 L3013–L3035：** [19.2 Completion Gate](MS-SKILLS-PLAN.md#orig-v26-l3013)

<a id="193-v23-四项执行合同验收"></a>

**旧 L3036–L3056：** [19.3 V2.3 四项执行合同验收](MS-SKILLS-PLAN.md#orig-v26-l3036)

<a id="194-v24-design-freeze-收口验收"></a>

**旧 L3057–L3067：** [19.4 V2.4 Design Freeze 收口验收](MS-SKILLS-PLAN.md#orig-v26-l3057)

<a id="195-v25-runtime-surface-与逐能力矩阵验收"></a>

**旧 L3068–L3084：** [19.5 V2.5 Runtime Surface 与逐能力矩阵验收](MS-SKILLS-PLAN.md#orig-v26-l3068)

<a id="196-v26-合同修订验收"></a>

**旧 L3085–L3103：** [19.6 V2.6 合同修订验收](MS-SKILLS-PLAN.md#orig-v26-l3085)

<a id="20-性能与资源约束"></a>

**旧 L3104–L3105：** [20. 性能与资源约束](MS-SKILLS-PLAN.md#orig-v26-l3104)

<a id="201-原则"></a>

**旧 L3106–L3128：** [20.1 原则](MS-SKILLS-PLAN.md#orig-v26-l3106)

<a id="202-最关键规则"></a>

**旧 L3129–L3134：** [20.2 最关键规则](MS-SKILLS-PLAN.md#orig-v26-l3129)

<a id="203-诊断"></a>

**旧 L3135–L3182：** [20.3 诊断](MS-SKILLS-PLAN.md#orig-v26-l3135)

<a id="204-全资源域安全验收"></a>

**旧 L3183–L3201：** [20.4 全资源域安全验收](MS-SKILLS-PLAN.md#orig-v26-l3183)

<a id="205-持久存储容量与恢复保留v26"></a>

**旧 L3202–L3219：** [20.5 持久存储容量与恢复保留（V2.6）](MS-SKILLS-PLAN.md#orig-v26-l3202)

<a id="21-与典型极限代码对抗能力对照"></a>

**旧 L3220–L3253：** [21. 与典型极限代码对抗能力对照](MS-SKILLS-PLAN.md#orig-v26-l3220)

<a id="22-jvm-与宿主硬边界"></a>

**旧 L3254–L3255：** [22. JVM 与宿主硬边界](MS-SKILLS-PLAN.md#orig-v26-l3254)

<a id="221-同权限-nativejvmti"></a>

**旧 L3256–L3269：** [22.1 同权限 Native/JVMTI](MS-SKILLS-PLAN.md#orig-v26-l3256)

<a id="222-jvm-死亡"></a>

**旧 L3270–L3281：** [22.2 JVM 死亡](MS-SKILLS-PLAN.md#orig-v26-l3270)

<a id="223-os--文件--无关进程"></a>

**旧 L3282–L3285：** [22.3 OS / 文件 / 无关进程](MS-SKILLS-PLAN.md#orig-v26-l3282)

<a id="224-极限代码对抗的正式边界"></a>

**旧 L3286–L3291：** [22.4 极限代码对抗的正式边界](MS-SKILLS-PLAN.md#orig-v26-l3286)

<a id="23-交付物"></a>

**旧 L3292–L3293：** [23. 交付物](MS-SKILLS-PLAN.md#orig-v26-l3292)

<a id="231-716"></a>

**旧 L3294–L3324：** [23.1 7.16](MS-SKILLS-PLAN.md#orig-v26-l3294)

<a id="232-717"></a>

**旧 L3325–L3371：** [23.2 7.17](MS-SKILLS-PLAN.md#orig-v26-l3325)

<a id="233-l6l6e"></a>

**旧 L3372–L3390：** [23.3 L6/L6E](MS-SKILLS-PLAN.md#orig-v26-l3372)

<a id="24-最终完成判定"></a>

**旧 L3391–L3460：** [24. 最终完成判定](MS-SKILLS-PLAN.md#orig-v26-l3391)
