# Ronova 新增接线验收分工与绑定

修订：**fix12 · 2026-09-15**。这是对当前接线工作流验收口径的显式项目决策，不是新的最高规范、实现报告或运行台账。不增加Root、不增加B/X或方向编号；原不变量、原文块与历史状态不改。输入为fix11文档与本轮用户评审，以下“采用”表示设计决定，不表示已有代码满足条件。

[规划入口](README.md) · [施工与批次](R66-CAPABILITY-WIRING-PLAN.md#batches) · [19行判据原文](R66-CAPABILITY-WIRING-PLAN.md#validation) · [原68项规格与台账边界](VALIDATION-TRACEABILITY.md#acceptance-scope) · [本轮决策记录](RESTRUCTURE-REVIEW.md#review-fix12-acceptance)

<a id="decision"></a>
## 决策：采用③，原台账与新增判据分工、不得互相豁免

**S1–S4／A1–A4及本接线规划中的新增工作，以原“批次观察点”表作为正式验收判据；原B01–B23／X24–X68继续负责它们原有需求范围和受影响回归。** 本轮不扩编、重编号、改写或补造原台账。

核对fix11实际文件，观察表是**19行**，不是评审消息概括的18行；末尾还有“V1.3 新增观察”。19行原断言全部保留，不为凑行数合并或遗漏。行名加描述性导航锚点，属于定位方式，不是另造一批验收编号。

旧用例与新方向可以有语义交集；**来源不同本身不证明完全不能复用。** 例如原X47讨论完成合同，X51讨论54键与天花板。然而已有用例不能仅凭名称接近，就被宣称覆盖挑战nonce、声明写集、跨源gap等新增断言。本文件给出的是适用分工，不把“无新增B/X”偷换成“新增工作不用验收”。

| 判定对象 | 正式依据 | 结论的边界 |
|---|---|---|
| 原7.17需求、原B/X场景、改动影响的旧行为 | [原68项规格与真实台账](VALIDATION-TRACEABILITY.md#case-directory) | 按原场景和当前适用性判断；不得因为代码是新接线而免做受影响回归。 |
| 新增接线中选定的方向、能力键、表面与scope | [原19行正式判据](R66-CAPABILITY-WIRING-PLAN.md#validation)及本文件的绑定/证据规则 | 可以判定选定交付范围是否完成；不能自动更改原B/X状态，也不等于54键全覆盖。 |
| 同时涉及两类要求的一次变更 | 两边适用断言的并集 | 同一产物允许复用，分别写出匹配的断言与结论；不能用一边通过抵消另一边失败或缺证。 |
| 完整7.17或完整Extreme声明 | 原适用完成要求＋新增工作全部承诺范围 | 局部slice完成、文档检查无错误、已登记但未执行，均不能替代整体验收。条件研究未选用可说明排期，但不得冒充已交付能力。 |

**本轮只建立可据以判定的口径，没有任何运行验收结论。** 后续只在已有关键代码批次集中验证；一个场景可同时观察多个判据，不新增19个必停阶段，也不要求全54键先清点完才开始一个已明确的slice。

<a id="batch-binding"></a>
## 原批次 ↔ 适用观察点

这是基于fix11批次职责的人工验收分工，不改变原顺序。下表是选取入口，不是排他白名单：实际改动触及其他行时一并纳入；明确未触及的行记录不适用理由。条件工作保持原前提，不能从表中有位置反推必须立即实施。

| 原批次 | 主要适用观察点 | 本次能够作出的结论粒度 |
|---|---|---|
| 第一批 | [请求与策略](R66-CAPABILITY-WIRING-PLAN.md#observe-request-policy)、[通用观察消费](R66-CAPABILITY-WIRING-PLAN.md#observe-generic-observation)、[局部范围](R66-CAPABILITY-WIRING-PLAN.md#observe-local-scope)、[执行与结算](R66-CAPABILITY-WIRING-PLAN.md#observe-execution-settlement)、[原生前置](R66-CAPABILITY-WIRING-PLAN.md#observe-native-prerequisites)、[申请故障隔离](R66-CAPABILITY-WIRING-PLAN.md#observe-capability-failure)、[已有任务回退](R66-CAPABILITY-WIRING-PLAN.md#observe-task-fallback)、[线程退出清理](R66-CAPABILITY-WIRING-PLAN.md#observe-thread-cleanup)、[宿主/侧边界](R66-CAPABILITY-WIRING-PLAN.md#observe-host-side)、[同权限API](R66-CAPABILITY-WIRING-PLAN.md#observe-same-privilege-api)、[决定侧与双路径](R66-CAPABILITY-WIRING-PLAN.md#observe-decision-paths)、[恢复源信任](R66-CAPABILITY-WIRING-PLAN.md#observe-recovery-trust)、[Mixin合同](R66-CAPABILITY-WIRING-PLAN.md#observe-mixin-contract)、[八方向关键观察](R66-CAPABILITY-WIRING-PLAN.md#observe-eight-directions)、[已有基线复用](R66-CAPABILITY-WIRING-PLAN.md#observe-baseline-reuse)、[V1.3 新增观察](R66-CAPABILITY-WIRING-PLAN.md#observe-v13-observations) | 所选实体/NBT/任务/存储等scope的端到端接线；S1/S2/C13/S3/A4及实际依赖的A2前置，不外推完整方向或54键。 |
| 第二批 | [请求与策略](R66-CAPABILITY-WIRING-PLAN.md#observe-request-policy)、[独立到期与采样陈旧](R66-CAPABILITY-WIRING-PLAN.md#observe-expiry-sampling)、[局部范围](R66-CAPABILITY-WIRING-PLAN.md#observe-local-scope)、[执行与结算](R66-CAPABILITY-WIRING-PLAN.md#observe-execution-settlement)、[同权限API](R66-CAPABILITY-WIRING-PLAN.md#observe-same-privilege-api)、[恢复源信任](R66-CAPABILITY-WIRING-PLAN.md#observe-recovery-trust)、[Mixin合同](R66-CAPABILITY-WIRING-PLAN.md#observe-mixin-contract)、[持久认证（选用时）](R66-CAPABILITY-WIRING-PLAN.md#observe-durable-auth)、[八方向关键观察](R66-CAPABILITY-WIRING-PLAN.md#observe-eight-directions)、[已有基线复用](R66-CAPABILITY-WIRING-PLAN.md#observe-baseline-reuse)、[V1.3 新增观察](R66-CAPABILITY-WIRING-PLAN.md#observe-v13-observations) | 实际扩展的动作/类血缘/证书及S2/S3/S4/A1范围；选用持久认证时其前置和断言同时适用。 |
| 第三批 | [原生前置](R66-CAPABILITY-WIRING-PLAN.md#observe-native-prerequisites)、[申请故障隔离](R66-CAPABILITY-WIRING-PLAN.md#observe-capability-failure)、[计划切片换代](R66-CAPABILITY-WIRING-PLAN.md#observe-plan-slices)、[宿主/侧边界](R66-CAPABILITY-WIRING-PLAN.md#observe-host-side)、[局部范围](R66-CAPABILITY-WIRING-PLAN.md#observe-local-scope)、[执行与结算](R66-CAPABILITY-WIRING-PLAN.md#observe-execution-settlement)、[八方向关键观察](R66-CAPABILITY-WIRING-PLAN.md#observe-eight-directions)、[已有基线复用](R66-CAPABILITY-WIRING-PLAN.md#observe-baseline-reuse)、[V1.3 新增观察](R66-CAPABILITY-WIRING-PLAN.md#observe-v13-observations) | 绑定/结构引用证据、跨表面预算及A1/A2/A3/S4新增范围；观察成功不宣称控制成功。 |
| 条件第四批 | [原生前置](R66-CAPABILITY-WIRING-PLAN.md#observe-native-prerequisites)、[申请故障隔离](R66-CAPABILITY-WIRING-PLAN.md#observe-capability-failure)、[宿主/侧边界](R66-CAPABILITY-WIRING-PLAN.md#observe-host-side)、[八方向关键观察](R66-CAPABILITY-WIRING-PLAN.md#observe-eight-directions)、[V1.3 新增观察](R66-CAPABILITY-WIRING-PLAN.md#observe-v13-observations) | 选用对象生命期观察时按原前置验证；JIT成本测量只结测量工作，不替代调度器实证。 |
| 条件批次（B1/新4） | [原生前置](R66-CAPABILITY-WIRING-PLAN.md#observe-native-prerequisites)、[Mixin合同](R66-CAPABILITY-WIRING-PLAN.md#observe-mixin-contract)、[宿主/侧边界](R66-CAPABILITY-WIRING-PLAN.md#observe-host-side)、[局部范围](R66-CAPABILITY-WIRING-PLAN.md#observe-local-scope)、[八方向关键观察](R66-CAPABILITY-WIRING-PLAN.md#observe-eight-directions) | 实际包装时机、解析及选中图形入口，不由prefix可用推出完整Render Firewall。 |
| 后续专项（新3/新2） | [原生前置](R66-CAPABILITY-WIRING-PLAN.md#observe-native-prerequisites)、[申请故障隔离](R66-CAPABILITY-WIRING-PLAN.md#observe-capability-failure)、[宿主/侧边界](R66-CAPABILITY-WIRING-PLAN.md#observe-host-side)、[局部范围](R66-CAPABILITY-WIRING-PLAN.md#observe-local-scope)、[八方向关键观察](R66-CAPABILITY-WIRING-PLAN.md#observe-eight-directions) | 已选帧条件或VM代码观测范围，不外推批量全返回或任意动态代码覆盖。 |

<a id="direction-binding"></a>
## S1–S4／A1–A4 ↔ 原判据与批次

方向标识仅在这组八方向中使用，不与DS/RULE同名A项合并。每行“应绑定的特有事实”从原方向正文与原观察行抽取，用于识别应提交哪些证据；**不能以这份摘要替代原行内其余适用断言**。下表没有给方向新增状态。

| 原方向／施工正文 | 原批次位置 | 原观察点入口 | 应绑定的特有事实与不允许的替代 |
|---|---|---|---|
| <a id="direction-s1"></a>[S1](R66-CAPABILITY-WIRING-PLAN.md#eight-s1) | 第一批所选scope；扩面随功能批次 | [原生前置](R66-CAPABILITY-WIRING-PLAN.md#observe-native-prerequisites)、[决定侧与双路径](R66-CAPABILITY-WIRING-PLAN.md#observe-decision-paths)、[八方向关键观察](R66-CAPABILITY-WIRING-PLAN.md#observe-eight-directions)、[已有基线复用](R66-CAPABILITY-WIRING-PLAN.md#observe-baseline-reuse) | 挑战与实际回调/结果关联；旧nonce、旧代际、缺回调、清理失败不能签发成功。获授不等于触达，金丝雀成功不等于对任意目标有效。 |
| <a id="direction-s2"></a>[S2](R66-CAPABILITY-WIRING-PLAN.md#eight-s2) | 第一批与C13并行；第二批扩面 | [局部范围](R66-CAPABILITY-WIRING-PLAN.md#observe-local-scope)、[执行与结算](R66-CAPABILITY-WIRING-PLAN.md#observe-execution-settlement)、[恢复源信任](R66-CAPABILITY-WIRING-PLAN.md#observe-recovery-trust)、[八方向关键观察](R66-CAPABILITY-WIRING-PLAN.md#observe-eight-directions)、[已有基线复用](R66-CAPABILITY-WIRING-PLAN.md#observe-baseline-reuse) | 原plan/lease上的声明集合、真实出口和actual/unknown回执；越界/未知/不完整snapshot不能通过。NBT位置比对保留，但不替代授权写集。 |
| <a id="direction-s3"></a>[S3](R66-CAPABILITY-WIRING-PLAN.md#eight-s3) | 第一批所选结果；第二批扩面 | [执行与结算](R66-CAPABILITY-WIRING-PLAN.md#observe-execution-settlement)、[八方向关键观察](R66-CAPABILITY-WIRING-PLAN.md#observe-eight-directions)、[已有基线复用](R66-CAPABILITY-WIRING-PLAN.md#observe-baseline-reuse) | 独立结果与原计划/版本关联；WRITE_UNVERIFIED不被后续观察抹掉；回执重发不重复mutation；CompletionEvaluation不因可构造而晋升Truth。 |
| <a id="direction-s4"></a>[S4](R66-CAPABILITY-WIRING-PLAN.md#eight-s4) | 第二批连续性；第三批新源适配 | [通用观察消费](R66-CAPABILITY-WIRING-PLAN.md#observe-generic-observation)、[线程退出清理](R66-CAPABILITY-WIRING-PLAN.md#observe-thread-cleanup)、[宿主/侧边界](R66-CAPABILITY-WIRING-PLAN.md#observe-host-side)、[八方向关键观察](R66-CAPABILITY-WIRING-PLAN.md#observe-eight-directions)、[已有基线复用](R66-CAPABILITY-WIRING-PLAN.md#observe-baseline-reuse) | 保留原任务图失证，绑定丢失/合并、source代际、实际撤销范围和重建基线；新窗口连续不代表旧缺口已补齐。 |
| <a id="direction-a1"></a>[A1](R66-CAPABILITY-WIRING-PLAN.md#eight-a1) | 第二批小范围；第三批通用索引 | [局部范围](R66-CAPABILITY-WIRING-PLAN.md#observe-local-scope)、[决定侧与双路径](R66-CAPABILITY-WIRING-PLAN.md#observe-decision-paths)、[八方向关键观察](R66-CAPABILITY-WIRING-PLAN.md#observe-eight-directions)、[已有基线复用](R66-CAPABILITY-WIRING-PLAN.md#observe-baseline-reuse) | 依赖反向撤销及共享依赖对全部受影响scope的传播；实体/任务图、复制来源索引不冒充已完成通用证据DAG，也不新建Root。 |
| <a id="direction-a2"></a>[A2](R66-CAPABILITY-WIRING-PLAN.md#eight-a2) | 桥接变化前的最小合同；第三批扩展 | [原生前置](R66-CAPABILITY-WIRING-PLAN.md#observe-native-prerequisites)、[申请故障隔离](R66-CAPABILITY-WIRING-PLAN.md#observe-capability-failure)、[已有任务回退](R66-CAPABILITY-WIRING-PLAN.md#observe-task-fallback)、[八方向关键观察](R66-CAPABILITY-WIRING-PLAN.md#observe-eight-directions)、[已有基线复用](R66-CAPABILITY-WIRING-PLAN.md#observe-baseline-reuse) | 构建身份与旧CAP基础保留，实际协商新增ABI/feature语义；旧桥不误开新功能，不以构建ID匹配代替接口协议匹配。 |
| <a id="direction-a3"></a>[A3](R66-CAPABILITY-WIRING-PLAN.md#eight-a3) | 第三批只读影子适配 | [执行与结算](R66-CAPABILITY-WIRING-PLAN.md#observe-execution-settlement)、[宿主/侧边界](R66-CAPABILITY-WIRING-PLAN.md#observe-host-side)、[八方向关键观察](R66-CAPABILITY-WIRING-PLAN.md#observe-eight-directions)、[已有基线复用](R66-CAPABILITY-WIRING-PLAN.md#observe-baseline-reuse) | shadow无真实effect；accepted不等于matches；无oracle不报告误判率或覆盖率，不由shadow结果单独签发真实控制能力。 |
| <a id="direction-a4"></a>[A4](R66-CAPABILITY-WIRING-PLAN.md#eight-a4) | 第一批所选能力；扩面随模块 | [请求与策略](R66-CAPABILITY-WIRING-PLAN.md#observe-request-policy)、[局部范围](R66-CAPABILITY-WIRING-PLAN.md#observe-local-scope)、[决定侧与双路径](R66-CAPABILITY-WIRING-PLAN.md#observe-decision-paths)、[八方向关键观察](R66-CAPABILITY-WIRING-PLAN.md#observe-eight-directions)、[已有基线复用](R66-CAPABILITY-WIRING-PLAN.md#observe-baseline-reuse) | 现有activation/evidence/新鲜度不串层；故障只撤销实际受影响范围；模块位/54键映射明确，内部捕获不误报成功，异常重试和恢复条件仍有界。 |

<a id="bx-regression"></a>
## 与原B/X的桥：受影响回归线索，不另造覆盖声明

下面只是从原场景中选出的**可审阅回归线索**，不是重算那120行人工映射，也不是断言这些用例已经完成。实施者先读原规格，实际用例是否覆盖所改行为，由具体fixture与断言决定；语义不符就不强贴。其余原要求同样不得因没有列在这里而失效。

| 新工作交界 | 原场景入口 | 能复用的交界与仍需独立补证的部分 |
|---|---|---|
| C13/A4的配置、上限与失证 | [X51](VALIDATION-TRACEABILITY.md#case-x51)、[X56](VALIDATION-TRACEABILITY.md#case-x56) | 原54键预设/ceiling/待重启及缺少某表面证明的场景可作回归；新增熔断触发、恢复和具体依赖传播仍须明确断言。 |
| S1/A2的原生前置与回退 | [B16](VALIDATION-TRACEABILITY.md#case-b16) | 复用不可用/未观察/Java回退交界，不推定已测试新挑战nonce、ABI字段与旧bridge组合。 |
| S2/A1的scope、版本与共享依赖 | [B17](VALIDATION-TRACEABILITY.md#case-b17)、[X26](VALIDATION-TRACEABILITY.md#case-x26)、[X43](VALIDATION-TRACEABILITY.md#case-x43) | 原租约版本与scope场景可约束回归；声明-实际写集差异和证据索引撤销范围需新断言。 |
| S3/A3的结果与不确定性 | [X40](VALIDATION-TRACEABILITY.md#case-x40)、[X47](VALIDATION-TRACEABILITY.md#case-x47) | 原仅调用/局部效果/完成合同/证据失效的区别继续适用；新的独立见证路径、影子无effect仍需明证。 |
| S3的去重与延迟回执 | [B18](VALIDATION-TRACEABILITY.md#case-b18)、[B20](VALIDATION-TRACEABILITY.md#case-b20)、[X63](VALIDATION-TRACEABILITY.md#case-x63)、[X64](VALIDATION-TRACEABILITY.md#case-x64) | 可复用重复、ACK丢失和effect重放场景；须证明复用的DeferredReceiptBuffer不会重做变更，而非只证明队列有条目。 |
| S4/A1的缺证与重新验证 | [X39](VALIDATION-TRACEABILITY.md#case-x39)、[X56](VALIDATION-TRACEABILITY.md#case-x56) | 原证据丢失后不能保留完整成功结论；新跨源序列/gap代际/补读窗口须单独绑定。 |

同一次执行可以同时指向本页观察点和原用例，但**只能复用证据，不能复制通过结论**。本轮未读原台账逐项明细、未运行上述场景，28/40仍只是已记录的用户工作区汇总回报。`GAP-MAP-07`的运行证据绑定和120行人工语义审阅仍开放。

<a id="key-readiness"></a>
## 54键的施工准备与完成范围

[原54键表](R66-CAPABILITY-WIRING-PLAN.md#key-inventory)维持原状：**39行仍写“逐键补生产者／消费路径清点”**。这是未完成的工程拆解，不是观察表正式化后就消失的欠账。此处没有重新审计源码，也没有把39行补写成虚构执行器。

所选键进入实现和验收时，必须能追到下面这条实际链；可以在同一关键代码批次中边确认边接线，不增加一个全54键前置冻结阶段。

| 需要记录的实际对象 | 作用 |
|---|---|
| 原枚举/配置键、动作（适用时）、server/client、对象/出口scope及目标档位 | 定义本次声明的范围；只有局部支持时不能写“全局Extreme完成”。 |
| 请求/生效策略、producer、consumer、原plan/lease与真实outlet | 区分仅观察、已登记执行和实际写入；缺失位置必须明确写待接，而非留空当不存在。 |
| 独立结果来源、失证/撤销/恢复消费者及其版本 | 防止只验证入口、不验证结果，或旧证据继续发布ACTIVE。 |
| 本页正式观察点锚点、scope特有的正/负场景、受影响原B/X或不适用理由 | 把施工项与本项目的两类验收口径接上。 |
| 真实代码/产物基线、运行/测试出处、断言结果与未覆盖范围 | 形成可复核的完成声明，而不是用清点文档或日志关键词当效果证明。 |

查明路径只表示“清点完成”；补上路径只表示“实现已改”；有断言证据并经审阅才允许说“所声明范围验收成立”。本轮不替它们生成状态值，不新增运行enum。未选中、待清点、缺前置或缺证的键保留在余项中；不能因为本批次排除了它们就写54键完成。

<a id="evidence-rules"></a>
## 证据绑定与结论规则

**规范层可以现在确定，证据层必须在实际代码和测试存在后绑定。** 19行是正式验收判据集合，不是19个现成fixture；把标题升格不能假装运行绑定已经完成。

每个选中的断言须记录：文档版本与行锚点、方向/键/scope、代码及产物基线、可复现刺激或fixture、应观察事实、实际观察、产物/日志位置、执行环境与来源、判断者及剩余范围。具体场景和阈值在该slice运行前按所选实现确定，不预填虚假文件名/哈希、不等运行结束后按结果放宽门槛。涉及客户端结果时，明确参与客户端、session及原计划要求的独立观察；服务器日志不能替代所要求的客户端事实。

一行有多个适用事实时逐项回答；例如“挑战缺回调”和“旧nonce”不是互为替代。正例证明确实可执行，适用的反例证明确实拒绝、失证或保留未知。条件不触发、测试未运行、缺原始证据，都不算该断言成立。

| 记录中出现的情况 | 允许写出的结论 |
|---|---|
| 适用断言均有当前scope证据，未决/冲突已按原合同处理，受影响原要求回归无未解决失败 | 所声明slice的新增工作验收成立；不是其他scope、整方向或全部54键完成。 |
| 断言成立一部分、缺负例/原始产物/观察位置，或状态自报没有对应证据 | 明写缺证或部分范围已核，不得笼统写“完成”。 |
| 出现越界写、错scope放行、伪造成功、重复effect、旧证据冒充新证据等违反适用断言的事实 | 明确记录失败和影响；不以其他成功次数抵消，不抹去未知写或COMMIT_UNKNOWN。 |
| 某观察点对这次改动确实不适用 | 记录对象/路线/环境理由及审阅；只是该slice不适用，不删除项目原要求。未实现、没环境或未选排期不等于完成或永久N/A。 |
| 仅文档脚本、ID一致、链接可定位 | 只报告文档一致性，不给Mod、方向、批次或原B/X赋PASS。 |

共享scope/原语变更可能影响旧scope，需按实际依赖选回归；不能把第一批小slice当作免回归许可。方向整体完成还需其全部承诺范围均有证据，或有显式批准的范围修订；不得用“尚未选中”无限缩小方向定义。

<a id="record-template"></a>
## 关键代码批次交付记录模板（未填写、不是台账）

下面模板用于后续真实批次报告，不在本轮创建空运行记录、不写入`validation/`。使用已有报告/证据归档位置即可；无需再建一套机器状态库。锚点是原判据定位，不是新验收编号。

```markdown
# 批次与选定交付范围
文档基线：对应版本或文件哈希
代码/产物基线：实际commit/产物路径与哈希
方向/原能力键/动作：按原标识填写
scope/服务端或客户端/适用档位：明确本次实际范围
producer → consumer → plan/lease → outlet → 结果 → 失效：逐一指向实际实现

## 适用性与断言记录
原行名/锚点：R66-CAPABILITY-WIRING-PLAN.md#observe-...
行内具体断言：原意及本scope可核对条件
fixture/刺激/运行前预期：实际可复现内容
实际观察/原始证据位置/环境/来源：不从汇总反推
结论/审阅：成立、失败或缺证的具体事实与范围
不适用项及理由：逐项说明；没做不算不适用

## 原要求回归
相关B/X：原编号与具体断言，或经审阅的不适用理由
同一次证据复用范围：分别列本判据/原B/X，不代填状态

## 剩余工作
未覆盖scope、仍待清点键、缺前置、缺证与后续批次：如实保留
允许的完成声明：只针对本次有证据的范围
```

<a id="current-evidence"></a>
## 当前实际状态

本轮采用方案③并补双向入口、批次/方向分工，**没有运行上述验收、没有原台账逐项新事实、没有生产代码改动**。39个待清点键、原人工语义映射和运行证据绑定继续开放。原三本MS主书及其全部历史原文/ID保留；当前四项硬限制与UX1未改。这个决策是可显式修订的项目约定，不再新增不可覆盖的冻结规则。
