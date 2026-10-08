# Ronova 击杀物品与动作执行规划

**fix11已有基线入口（原文块外）：** [S2写入接缝/实际路径比对](R66-CAPABILITY-WIRING-PLAN.md#eight-s2)与[S3完成接缝/延迟回执](R66-CAPABILITY-WIRING-PLAN.md#eight-s3)补足实现起点；观察不自授许可，回执重发不重做变更。原方向和批次不改。

**fix10职责入口（原文块外）：** [S2原计划写集](R66-CAPABILITY-WIRING-PLAN.md#eight-s2)、[S3累积事实/独立结果](R66-CAPABILITY-WIRING-PLAN.md#eight-s3)与[A3影子比较](R66-CAPABILITY-WIRING-PLAN.md#eight-a3)接回动作/结算；声明不等于执行证据，观察位不改名成四态流程。

**fix8就地精度入口（原文块外）：** [⑥新增结算端口唯一性检查](R66-CAPABILITY-WIRING-PLAN.md#v13-settlement)建立在既有owner/lease/claim上，不将基础守卫当作该断言已实现；[八处通用观察调用](R66-CAPABILITY-WIRING-PLAN.md#recovery-consumers)不自动获得写许可。

> **fix7 同权限清点职责入口：** 受权提交/生命周期门面、真实结算处检查与双路径scope已融入[既有工作章节](R66-CAPABILITY-WIRING-PLAN.md#same-privilege-surface)；[处理回执](RESTRUCTURE-REVIEW.md#review-fix7-surface)。仅规划补足，旧原文块和ID不改。

> **fix6 V1.3职责入口：** ④类血缘与⑥结算归属复用既有动作/计划；攻击与拦截不合并授权。 [逐条回执](RESTRUCTURE-REVIEW.md#review-fix6-v13) · [当前融合规划](R66-CAPABILITY-WIRING-PLAN.md#v13-receipt)。
> 当前接线增补：[r66 能力发布、动作执行与 JVMTI 规划](R66-CAPABILITY-WIRING-PLAN.md)。下方旧块保真保留；就地更正只作用于注明的历史判断，不代表源码已修改。
> 结构来源：`.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md`。重构日期：2026-09-14。
> 本书按 MS 原有二级、三级标题顺序组织；下方“迁入原文”来自 Ronova 输入包。MS 示例、Ronova 原条款和本次编排说明分别标注。
> **只有文档迁移与明确标注的规划增补，没有实现改动或运行验收。** 原文中的已实现、未实现、成功/失败、日期、源码行号和历史版本都属于原记录，不是本轮确认。
> 当前限制与文档效力见 [规划入口](README.md)；旧文件/节号/行号与新位置见 [完整迁移映射](RESTRUCTURE-MAP.md)。
> 人读入口：[不变量全集与全部编号索引](INVARIANT-INDEX.md)；[启动参数选型](README.md#scope-startup-parameters)；[P 层与完成口径](README.md#scope-p-layer)。
> **旧语境约定：** 迁入段落的“本文/本节/第 X 节/§X”继续指其原来源的章节；可解析引用已转为新位置链接。相同 ID 在不同原来源中不合并、不重编号。

本书收纳 **177 个原文分段**。这些分段保留标题文字、正文、表格、示例和全部原 ID；仅调整标题层级及引用链接。

## 章节导航
- [先定义击杀契约](#ms-msitem-l24)
- [总体架构](#ms-msitem-l43)
- [建立职责边界](#ms-msitem-l89)
- [物品层只承担入口与表现](#ms-msitem-l112)
- [列出全部击杀入口](#ms-msitem-l126)
- [用统一动作上下文消除重复](#ms-msitem-l142)
  - [去重与幂等](#ms-msitem-l168)
- [目标选择与快照](#ms-msitem-l180)
- [统一目标、结果和防御策略](#ms-msitem-l201)
- [正常死亡、运行时清除与混合执行](#ms-msitem-l224)
  - [正常死亡](#ms-msitem-l226)
  - [运行时清除](#ms-msitem-l238)
  - [混合模式](#ms-msitem-l255)
- [服务端队列与线程边界](#ms-msitem-l261)
- [Forge 事件、Mixin 与自定义转换器](#ms-msitem-l286)
  - [事件层](#ms-msitem-l290)
  - [Hook 分类](#ms-msitem-l301)
- [服务端与客户端一致性](#ms-msitem-l329)
- [客户端表现也是物品的一部分](#ms-msitem-l345)
- [从零实现顺序](#ms-msitem-l358)
- [验证矩阵](#ms-msitem-l372)
- [故障定位](#ms-msitem-l392)
- [完成标准](#ms-msitem-l406)

---

<a id="ms-msitem-l24"></a>

## 先定义击杀契约

**框架来源：** [MS msitem，原 L24–L42](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#先定义击杀契约)。

**编排说明（非原条款）：** MS 八维击杀合同由原能力/普通动作/三档配置承接。物品堆叠、主副手与成本返还等尚未集中成完整细则的部分单列文档缺口，不虚构默认值。

<a id="orig-full-l0017"></a>

**迁入原文｜FULL｜旧 L17–L35** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-full-l0017)

原标题路径：Ronova 7.17 极限档全体系加强规划 V1.0 → 1. 用户要求与目标

<!-- RONOVA-ORIGINAL-BEGIN orig-full-l0017 -->
#### 1. 用户要求与目标

用户要求将极限档的所有强度提高到非常极限的位置，并把从 ECA、黑日、otto、Deadload、馒头学习的机制做得更强。
范围为攻击 15 项、拦截 10 项、防护 17 项、运行时 12 项，共 54 项高级能力，以及它们驱动的 14 个普通动作。

本规划的目标是让全部 54 项具备实际的极限执行、观察、恢复及结果反馈路径。
不是只将预设写为 STRICT_ALL；目前预设已经如此，缺口主要在真实实现和覆盖范围。
不能因某项难以实现而把极限档的最终目标改为兼容档或仅观察，也不能将尚未支持的路径显示为已实现。
单项未完成时继续保留该项工程与待办，不因此冻结其他已经获准的独立步骤。

极限提高的方向包括：更深的因果追踪、多来源状态处置、持续反馈、事件驱动恢复、精确资源释放、
跨实例/连接/保存周期的失效处理，以及在干扰与压力下仍能前进的调度。
“几个数量级”是用户提出的提升目标；没有同条件结果数据前不标注已达到某个倍率。

用户明确要求：**memoryRemove 只分析，不集成到 Ronova**。
生产机制不加入上述模组的名称、包名、类名或文件名特判。

规范顺序遵循本目录 [README](README.md)；复用现有 Internal Root、身份、效果槽、普通动作开关和恢复链合同。


<!-- RONOVA-ORIGINAL-END orig-full-l0017 -->

<a id="orig-full-l0036"></a>

**迁入原文｜FULL｜旧 L36–L48** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-full-l0036)

原标题路径：Ronova 7.17 极限档全体系加强规划 V1.0 → 1. 用户要求与目标 → 1.1 极限档的定义

<!-- RONOVA-ORIGINAL-BEGIN orig-full-l0036 -->
#### 1.1 极限档的定义

极限档让攻击、防护、拦截和运行时都达到最大实际覆盖：

- **攻击**：处理目标本体，同时追踪维持它的状态、恢复记录、任务和新实例，持续维持处理结果。
- **防护**：Owner 的生命、身份、世界索引、连接与显示遭到干扰后，仍能发现缺口并恢复。
- **拦截**：关键调用被取消、提前返回、替换或绕过后，仍有独立执行路径。
- **运行时**：核心代码、维护线程和资源出现故障后，能够恢复工作，并在持续干扰下继续推进。

极限档生效后，应立即启用全部符合当前 Root 资格、普通动作权限和实际可用能力的路径，
并持续发现后继来源。待执行工作按预算公平推进，容量不足进入可续接待办。
加强同时覆盖全部 54 项高级能力和 14 个普通动作；某一路径未完成或暂时失效，不冻结其他独立且获准的步骤。


<!-- RONOVA-ORIGINAL-END orig-full-l0036 -->

<a id="orig-tier-l0223"></a>

**迁入原文｜TIER｜旧 L223–L224** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-tier-l0223)

原标题路径：Ronova 7.17 极限档强度实施规划 V1.0 — Internal Root → 2. 目标与非目标

<!-- RONOVA-ORIGINAL-BEGIN orig-tier-l0223 -->
#### 2. 目标与非目标


<!-- RONOVA-ORIGINAL-END orig-tier-l0223 -->

<a id="orig-tier-l0225"></a>

**迁入原文｜TIER｜旧 L225–L237** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-tier-l0225)

原标题路径：Ronova 7.17 极限档强度实施规划 V1.0 — Internal Root → 2. 目标与非目标 → 2.1 目标

<!-- RONOVA-ORIGINAL-BEGIN orig-tier-l0225 -->
#### 2.1 目标

| ID | 目标 | 完成判据 |
|---|---|---|
| **G0** | **实施 [V2.6 §6.6](MS-MSITEM-PLAN.md#orig-v26-l0761) 的逐键矩阵**（首要目标） | 54 键 + 14 动作在三个预设下各自携带**逐键语义声明**（纳入范围 / 前置限制 / 报告义务），并具备独立 revision、迁移规则与 exact-artifact 测试 |
| **G1** | 建立"能力测量 → 天花板"管道 | 天花板由注册的举证生产者提供，而非硬编码调用点；至少 6 个既有键经新管道产出**行为不变**的结果 |
| **G2** | 让已存在执行器的键可被举证 | 有天花板的键从 6 个提升，且每个 ACTIVE 都由**实际行为观察**支撑 |
| **G3** | 让每键执行按**其声明的档位语义**工作 | 逐键落实 [§6.6](MS-MSITEM-PLAN.md#orig-v26-l0761) 三档语义；3 个 `STRICT_ALL` 步骤因此获得合法天花板 |
| **G4** | 合并两套权限路径 | `registered*` 与 `resolve()` 使用同一判据，或在合同中显式写明分叉理由 |
| **G5** | 修正报告矛盾 | `/ronova strength status` 的"接线"列与"高级解析"列不再对同一行给出互相矛盾的信息 |

**顺序约束**：G0 是 G1–G3 的前提。


<!-- RONOVA-ORIGINAL-END orig-tier-l0225 -->

<a id="orig-road-l0011"></a>

**迁入原文｜ROAD｜旧 L11–L25** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-road-l0011)

原标题路径：Ronova L6／L6E 全量加强路线 V1.0 — Internal Root → 1. 范围、规范与阅读方式 → 1.1 极限档的连续加强目标

<!-- RONOVA-ORIGINAL-BEGIN orig-road-l0011 -->
#### 1.1 极限档的连续加强目标

极限档在各阶段继续加深攻击、防护、拦截和运行时的实际覆盖：
对目标同时处理本体、维持状态的写源、恢复记录、任务、后继实例及关联显示；
对 Owner 同时维护生命、身份、世界存在关系、输入、连接与显示；
关键执行或恢复路径失效后，其他独立且获准的路径继续推进，并恢复失效部分。

极限档生效后启用当前资格、动作权限和实际能力共同允许的路径，持续发现后继来源。
共享路径不能仅因共享而自动排除；具体处置级别仍由实际归属、当前许可和执行证据决定。
容量和时间预算用于分片、背压与持续调度，待办及其适用范围必须能续接。

三档继续使用同一事实模型：兼容、强化、极限扩大的是适用范围、实际执行深度和恢复能力。
54 个高级键及普通动作可单独配置；请求深度、实际支持、激活状态和完成状态分别报告。
当前极限预设已有 `STRICT_ALL`，未来加强需要落实每项背后的生产路径。


<!-- RONOVA-ORIGINAL-END orig-road-l0011 -->

<a id="orig-msc-l0069"></a>

**迁入原文｜MSC｜旧 L69–L79** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-msc-l0069)

原标题路径：Ronova × MS 要求 合规与加强方案 V1.0 → 1. 要求清单与现状判定 → 1.4 目标与结果策略

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-msc-l0069 -->
#### 1.4 目标与结果策略

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-D1 | 八维契约：物品/触发/目标/选取/结果/服务端/客户端/防御 | [`msitem:28-37`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#先定义击杀契约) | 分散在 `StrengthOptionKey`/`AttackAction`/README 操作表 | ⚠️ 未集中成契约表 |
| R-D2 | 目标选取用**稳定快照**，迭代期间不改集合 | [`skills:37`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#秒杀是什么)、[`msitem:412`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#完成标准) | `TargetDiscovery` + 观察快照 | ✅ |
| R-D3 | 执行前重新验证（仍在目标世界/未清除/未受保护） | [`skills:202`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#逻辑服务器) | 先 `observe` 后 `canJudge`，全部 `prepare` 后一次 `commit` | ✅ |
| R-D4 | 三种执行模式：标准死亡 / 实体清除 / 混合 | [`skills:112-162`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#秒杀的三种执行模式) | `TerminalLifecycle`（标准）/`TerminalIndexRepair`（清除）/组合 | ✅ |
| R-D5 | 标准死亡保留归因、事件可观察可取消、**不重复掉落** | [`skills:120`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#1-标准死亡) | 4 道防腐线（`mark` 首门、planned 路径不执行 `kill/discard`、唯一死亡入口、`LifecycleIntegrationReceipt` 持久预留） | ✅ **强于要求** |
| R-D6 | "走标准死亡"与"从查询/追踪/渲染消失"是**两件事** | [`msitem:39`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#先定义击杀契约) | `TerminalLifecycle` 与 `TerminalIndexRepair` 分离，后者自述"绝不调用实体生命周期方法" | ✅ |


<!-- RONOVA-ORIGINAL-END orig-msc-l0069 -->

<a id="ms-msitem-l43"></a>

## 总体架构

**框架来源：** [MS msitem，原 L43–L88](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#总体架构)。

**编排说明（非原条款）：** 动作状态与持久实体状态是不同轴。保留原七态视图提案和 LifecycleTruth / ResolutionState / AdmissionDisposition，不重编号或把多个轴强塞进一个枚举。

**文档缺口：** [GAP-MAP-01 · 动作七态与实体持久状态的正交映射](RESTRUCTURE-MAP.md#gap-map-01)。

<a id="orig-bl-l1542"></a>

**迁入原文｜BL｜旧 L1542–L1572** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l1542)

原标题路径：13. Execution Authority Chain — State Ownership / Dispatch / Return 统一模型 → 13.4 Return / Dispatch 的技术实现路线 → 13.4.1 首选后端：显式 Execution Gateway

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l1542 -->
#### 13.4.1 首选后端：显式 Execution Gateway

Ronova 自己可控制的关键路径优先设计成：

```text
ExecutionGateway.invoke(context, CriticalOperation)
       ↓
Dispatch Binding
       ↓
implementation
       ↓
Raw Completion
       ↓
CompletionContract
       ↓
AuthorityCompletion
```

概念类型：

```text
AuthorityCompletion
├ RETURN(value)
├ THROW(exception)
├ ABORT
├ SUPPRESS
└ REWRITE(value/exception)
```

这样 Return Authority 首先是**显式执行协议**，而不是另一个零散 method-exit Hook。


<!-- RONOVA-ORIGINAL-END orig-bl-l1542 -->

<a id="orig-v26-l0675"></a>

**迁入原文｜V26｜旧 L675–L760** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0675)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 6. 强度模型 → 6.5 Execution State 与 Attack Outcome

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0675 -->
#### 6.5 Execution State 与 Attack Outcome

策略是否可用、当前是否允许执行、攻击取得了什么结果必须分别记录。Authority 的提交状态继续引用 canonical command/receipt，不并入下列枚举，不新增 Authority 状态机。

```text
executionState:
ACTIVE
QUARANTINED
FENCED

attackOutcome:
APPLIED
PARTIAL
CLEARED
FAILED

fenceScope:
NONE
TARGET
OPERATION
PLANE
ROOT
REALM
```

| 字段 | 含义 | 不能推导的结论 |
|---|---|---|
| executionState=ACTIVE | 当前范围允许按有效 policy、lease 与预算继续执行 | 不代表攻击已成功，也不代表所有 Extreme 能力已完成 |
| executionState=QUARANTINED | 相关结果资格已隔离，等待查证、修复或恢复 | 不代表敌方任务已停止或实体已移除 |
| executionState=FENCED | 当前 scope 的授权或执行可信性不满足条件，按政策停止承认新的相关结果 | 不代表 CLEARED，也不自动等同 ROOT/REALM 失证 |
| attackOutcome=APPLIED | 已有证据确认请求中的局部动作生效；清除请求仍可能等待最终验收 | 调用过执行器、发过请求、返回过成功码均不足以成立 |
| attackOutcome=PARTIAL | 清除或其他请求合同仍有未满足条件、结果漂移或未闭合关键义务 | 不回滚已经提交的 Judgment，不得展示完成 |
| attackOutcome=CLEARED | 清除请求已满足 [§9.4](MS-MSITEM-PLAN.md#orig-v26-l1506) 的全部条件且当前证据有效 | 不适用于所有非清除能力，不是 Admission Truth |
| attackOutcome=FAILED | 本次执行尝试按其合同被确定为失败 | 不等同 durable commit 失败，不自动清除 Tombstone 或后续恢复义务 |

请求刚接收、尚无效果证据时，attackOutcome 可以尚未赋值，沿用请求进度显示“执行中/待确认”；禁止为了填枚举提前写 APPLIED。清除的局部动作确认后可以短暂为 APPLIED；完成评估发现任何必要条件尚未满足时即为 PARTIAL，不能长期用 APPLIED 掩盖未闭合条件。非清除请求按自身 CompletionContract 判断局部或最终效果，不伪造 CLEARED。

V2.4 冻结独立的完成性字段：

```text
completionContractId / completionContractRevision
completionSatisfied: true | false
```

其中：

- `completionSatisfied=false` 表示当前 CompletionContract 仍有条件、稳定窗口、重验证或必要结果边未满足；
- `completionSatisfied=true` 表示该合同定义的最终成功条件已经由当前 revision 的效果证据满足；
- 对没有 `CLEARED` 语义的非清除 CriticalOperation，只有 `attackOutcome=APPLIED AND completionSatisfied=true` 才能显示“攻击已完成”；
- `APPLIED` 未携带有效 CompletionContract，或 `completionSatisfied` 尚未建立时，只能显示“动作已生效/待完成”；
- `completionSatisfied` 是由 Root 接受的完成证明，不是 Judgment、Tombstone、TargetEpoch 或 Admission Truth；必要证据失效后必须推进 revision 并重新评估。

禁止另用 `isTerminalOutcome` 表示上述含义；`TERMINAL` 保留给目标存在性终结语义，不能与一次 CriticalOperation 的完成性混用。

每份运行时结果至少关联：

```text
attackId / criticalOperationId
authorityReceiptRef / commandId
judgmentRevision / lineageId / targetEpoch
executionState
fenceScope / affectedScope
attackOutcome / outcomeRevision
completionContractId / completionContractRevision / completionSatisfied
resultGraphRevision / obligationCoverageRevision
outcomeEvidenceRefs[]
currentCertificateRef / failureReason
```

affectedScope 必须标明真实受影响的目标、操作或平面，不能只给一个 OPERATION 枚举就把所有目标准入一并停掉。fields 是同一结果报告的不同维度，不对应新的 writer。

典型组合：

| 实际情况 | executionState | fenceScope | attackOutcome | completionSatisfied | GUI 文案 |
|---|---|---|---|---|---|
| 局部移除已确认，最终条件仍待检查 | ACTIVE | NONE | APPLIED | false | 动作已生效，待验收 |
| 非清除操作的局部效果成立，但合同仍有未闭合结果边 | ACTIVE | NONE | APPLIED | false | 动作已生效，待完成 |
| 非清除操作的全部合同条件与重验证通过 | ACTIVE | NONE | APPLIED | true | 攻击已完成；当前完成证明有效 |
| Ronova 停止授权，manager 仍每 20 tick 重建目标 | FENCED | TARGET 或相关 OPERATION | PARTIAL | false | 部分生效；恢复链未控制，相关结果已隔离 |
| 恢复链与全部出口受控，九项条件及稳定窗口通过 | ACTIVE | NONE | CLEARED | true | 已清除；当前完成证明有效 |
| 本次尝试确定失败，已提交 Judgment 仍保留 | 按实际报告 | 按实际报告 | FAILED | false | 本次执行失败；权威提交状态另列 |

executionState 与 attackOutcome 两个状态维度正交；completionSatisfied 则把当前 outcome 对照指定 CompletionContract 判断是否最终完成。三者的组合都必须有证据支撑。若 fence 原因使同一目标当前证书或完成合同的必要条件失证，应撤销其当前有效性、将 completionSatisfied 置为 false，并按合同重新评估 outcome；不能继续组合成“当前已清除/已完成 + 关键结果无法证明”。无关目标或操作被 fence 不自动撤销其他有效证书。

旧字段 clearanceState 仅可作为清除请求 attackOutcome 的只读兼容展示，不再拥有独立决策逻辑。策略的 activationState/evidenceState、结果的 executionState/attackOutcome 和 Authority 的 commit 状态禁止互相覆盖。


<!-- RONOVA-ORIGINAL-END orig-v26-l0675 -->

<a id="orig-p0a-l0634"></a>

**迁入原文｜P0A｜旧 L634–L649** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0634)

原标题路径：10. LifecycleTruth / ResolutionState / AdmissionDisposition

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0634 -->
#### 10. LifecycleTruth / ResolutionState / AdmissionDisposition

三个状态轴严格分开：

```text
LifecycleTruth       = ACTIVE | TERMINAL
ResolutionState      = RESOLVED | PARTIALLY_RESOLVED | AMBIGUOUS |
                       OPAQUE | STALE | INVALID | UNKNOWN
AdmissionDisposition = PREBOUND | ACTIVE | HELD | CANCELLED |
                       EMERGENCY_RECONCILE
```

不要把所有 Minecraft death 状态直接映射成 TERMINAL。`QUARANTINED/HELD` 不是 lifecycle truth；`UNKNOWN` 也不是 `ACTIVE` 或 `false`。任何公共接口必须返回显式 `AuthorityResult<T>`，不能用 boolean/Optional 折叠这些状态。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0634 -->

<a id="orig-msc-l0050"></a>

**迁入原文｜MSC｜旧 L50–L58** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-msc-l0050)

原标题路径：Ronova × MS 要求 合规与加强方案 V1.0 → 1. 要求清单与现状判定 → 1.2 状态模型

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-msc-l0050 -->
#### 1.2 状态模型

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-B1 | 动作与实体状态建模为**显式状态**：`READY`/`DISPATCHED`/`QUEUED`/`NORMAL_DEATH`/`ERASED`/`PROTECTED`/`IGNORED` | [`msitem:75-87`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#总体架构) | 有 `OperationReport.Status`、`EffectRecord.State`、`JudgmentState`，但**无统一七态模型** | ⚠️ 部分 |
| R-B2 | `ERASED` 与 `PROTECTED` **互斥**，冲突时确定优先级（通常先拒绝处理受保护对象） | [`msitem:87`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#总体架构) | 有 `OwnerImmunity`（免疫不变量），语义相近但未明文化为互斥优先级 | ⚠️ 部分 |
| R-B3 | 状态键 = UUID 或实体 ID + 维度；**不用 Java 对象引用作长期身份** | [`skills:110`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#总体架构) | `SubjectKey = (RealmId, subjectId)`，明文"运行时类/维度/位置/来源不参与相等" | ✅ 强于要求 |
| R-B4 | 卸载/切维度/世界关闭时清理过期条目 | [`skills:110`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#总体架构) | `RetiredWorldAdmission`（"弱物理世界退休"）、`ServerRuntimeRegistry.release` | ✅ |


<!-- RONOVA-ORIGINAL-END orig-msc-l0050 -->

<a id="orig-msc-l0258"></a>

**迁入原文｜MSC｜旧 L258–L268** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-msc-l0258)

原标题路径：Ronova × MS 要求 合规与加强方案 V1.0 → 3. 阶段设计 → M2 —— 显式状态模型（R-B1、R-B2）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-msc-l0258 -->
#### M2 —— 显式状态模型（R-B1、R-B2）

**入口**：M1 完成（先知道现状才能定义目标态）。

| 交付物 | 内容 |
|---|---|
| 七态模型 | `READY`/`DISPATCHED`/`QUEUED`/`NORMAL_DEATH`/`ERASED`/`PROTECTED`/`IGNORED`，映射到现有 `OperationReport.Status` / `EffectRecord.State` / `JudgmentState`，**不新增第二套真值** |
| 互斥与优先级 | `ERASED` 与 `PROTECTED` 互斥；冲突时**先拒绝处理受保护对象**；与现有 `OwnerImmunity` 对齐 |

**验收**：同一目标不会同时处于两态；冲突场景有确定结果且可复核。


<!-- RONOVA-ORIGINAL-END orig-msc-l0258 -->

> **fix7 就地说明：** 入口的访问修饰符不替代原Root的变更授权；[API/生命周期门面](R66-CAPABILITY-WIRING-PLAN.md#surface-api)与原动作权限分别接线。

<a id="ms-msitem-l89"></a>

## 建立职责边界

**框架来源：** [MS msitem，原 L89–L111](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#建立职责边界)。

**编排说明（非原条款）：** 物品、动作、目标、策略、状态、服务端、客户端和 Hook 仅是职责分类；原类名、包路径和来源版本记录不随本文改代码。

<a id="orig-rec-l0162"></a>

**迁入原文｜REC｜旧 L162–L179** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0162)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 4. 源码落点与责任

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0162 -->
#### 4. 源码落点与责任

下列是实施落点，新增类型尚未表示已经存在。

| 现有位置 | 本功能在其中承担的工作 |
|---|---|
| `engine/spawn/SpawnAdmissionRuntime` 与通用准入 seam | 上报本次重建输入、实际拒绝结果与上下文；同步 gate 不做 durable I/O |
| `observation` / `surface` / 现有 resolver | 有界来源关系发现、复制/调度证据、访问语义与 observer 有效性 |
| `runtime` 的 Candidate/Obligation 协调 | 合并同目标同来源的重复尝试，持久承接待处置范围，公平安排后续探查 |
| `root/api/TerminalOperationPlan`、现有 Store | 从原已提交裁决批准恢复源步骤、固定 slot、验证当前资格和记录执行进度 |
| `engine/TerminalPlanExecutor` / `OperationPlanRunner` | 编排来源限制、身体与记录收敛、已知源处置、效果查询和最终观察 |
| 已登记的 Data/Temporal/Registry executor | 在精确位置执行记录处理、任务失效或注册边处理；不持有另一份 Judgment |
| `persistence/EffectJournal` | 扩展注册的来源处置语义和恢复能力，保存 intent/未知/确认结果 |
| `TerminalCompletionMonitor` / GUI 投影 | 展示已知来源、仍未决范围、持久化状态与当前完成缺项 |

优先把实现放到职责明确的组件，经现有组合根接入。
`SpawnFirewall`、`AuthorityGuard` 等 façade 不吸收来源扫描和写入实现。


<!-- RONOVA-ORIGINAL-END orig-rec-l0162 -->

<a id="ms-msitem-l112"></a>

## 物品层只承担入口与表现

**框架来源：** [MS msitem，原 L112–L125](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#物品层只承担入口与表现)。

**编排说明（非原条款）：** 入口与表现不应吞并全部执行职责。原规划对 ItemPolicy 和成本有概括，但没有覆盖 MS 的完整物品契约细节；见文档缺口。

**文档缺口：** [GAP-DOC-01 · 物品自身的完整契约](RESTRUCTURE-MAP.md#gap-doc-01)。

**关联原条款：** [MSC · 1.4 目标与结果策略](MS-MSITEM-PLAN.md#orig-msc-l0069)；[NON · C 块 —— 合规整改（减法，先做）](MS-SKILLS-PLAN.md#orig-non-l0129)；[RULE · 闸门 4 —— 结算唯一性闸](MS-MSITEM-PLAN.md#orig-rule-l0175)。

本节点没有被单独搬入的原文段落；它保留 MS 的独立职责入口。上列关联条款或下级章节不等于本节点所有细节已经覆盖；适用性与缺口见映射表。

<a id="ms-msitem-l126"></a>

## 列出全部击杀入口

**框架来源：** [MS msitem，原 L126–L141](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#列出全部击杀入口)。

**编排说明（非原条款）：** 14 个普通动作、15 个高级攻击键及多入口结算关系迁入此处；迁移不新增键，不宣称未接线入口已可执行。

<a id="orig-v26-l0778"></a>

**迁入原文｜V26｜旧 L778–L799** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0778)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 6. 强度模型 → 6.6 V2.5 逐能力 ProfilePresetMatrix → 6.6.1 玩家可见攻击模块

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0778 -->
#### 6.6.1 玩家可见攻击模块

| GUI 能力 | 兼容 | 强化 | 极限代码对抗 |
|---|---|---|---|
| 强力攻击 | 当前目标的生命、死亡、清除基础闭环 | 同时收敛相关状态、记录、追踪与直接恢复链 | 对当前 Judgment 的全部候选/已证明结果表面执行完整闭环 |
| 改写生命 | 当前/最大生命及原版同步 | 加入额外生命状态、能力附件与已识别写回源 | 控制候选状态源、最终写入点、回滚与重建出口 |
| 强制终结 | 提交 Judgment、Tombstone 与同 lineage Admission | 约束已识别恢复/注册源并推进 TargetEpoch | 资格事务原子提交，旧 epoch 的实体、任务、包、存档和投影全部失效 |
| 直接清除 | 通过稳定标准生命周期完成移除 | 绕过已证明会吞掉结果的死亡表现并同步实体记录 | 执行 Existence Termination 九项条件、稳定窗口和当前证书重验 |
| 实体记录清除 | 使用标准 manager/index API 清除当前记录 | 同步 ID/UUID、section、chunk、tick、tracking 相关记录 | 覆盖全部已观察写入点与重插入口，未闭合即 PARTIAL |
| 重生压制 | 拒绝同 UUID/同 lineage 的旧资格目标 | 控制相关重生 manager、任务和 callback | 对全部候选重建来源执行 epoch/lineage 准入闭环 |
| 重建压制 | 只监控对象替换与新 UUID | 压制已证明属于目标的对象/UUID/类型替换 | 按 ResultGraph、producer 和最终结果识别，不依赖单一 UUID/类名 |
| 生成压制 | Tombstone 对应的基础 Admission gate | 覆盖已识别注册器、工厂与生成入口 | 覆盖候选 registration/network/save/dynamic 入口并验证拒绝结果 |
| 渲染攻击 | 清除原版实体可见性 | 控制已识别 renderer、client presence 与镜像 | 把 Presentation 作为结果表面闭合，但不让客户端成为 Truth |
| 追踪清除 | 标准服务端 tracking 移除 | 同步已识别服务端/客户端 presence 与包路径 | 控制网络重建、缓存投影和重订阅；旧 epoch 投影不得复活 |
| 血条 / HUD 清除 | 原版 BossBar/HUD | 已识别自定义 HUD、overlay 与同步源 | 闭合 Presentation/Network 最终显示结果；无法观察时真实标记缺口 |
| 代码防御剥离 | 监控目标对 hurt/die/remove 等直接覆盖 | 处理已证明相关的防御方法、hook 与 dispatch | 对候选结果链先隔离资格，对已证明代码源执行精确失能与重验 |
| 调用链攻击 | 监控关键直接调用 | 控制已证明相关的 task/callback/manager 链 | 追踪并约束全部候选调用、分派、恢复和最终出口，防循环重建 |
| 全返回 | 默认关闭主动方法失能；只保留合同观察 | 对已证明相关关键方法使用返回/完成失能策略 | 全返回 + dispatch + final result takeover 联合，必须有 completion proof |
| 攻击来源追踪 | 追踪受保护 sink 的直接来源 | 追踪相关任务、回调、loader、网络与存档来源 | 覆盖八类 Runtime Surface 的候选因果链，并受有界证据预算约束 |

这些行是玩家语义，不创建额外 `StrengthOptionKey`；独立动作许可由 [§6.7](MS-MSITEM-PLAN.md#orig-v26-l0884) 的 typed 子配置表达。十五项高级政策决定允许动作的作用深度，不能代替普通开关授予动作权限。r53 的 54-key/schema-2 模型尚未实现此子配置，不能宣称普通页所有独立开关已经落地。


<!-- RONOVA-ORIGINAL-END orig-v26-l0778 -->

<a id="orig-v26-l0884"></a>

**迁入原文｜V26｜旧 L884–L930** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0884)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 6. 强度模型 → 6.7 普通开关到执行权限的唯一映射（V2.6）

**规范关系调整：** 本段的旧位次/冻结不可覆盖规则仅保留作来源记录，不再阻止显式修订；技术不变量本轮未据此删除。参见 [当前文档效力](README.md#scope-governance)。

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0884 -->
#### 6.7 普通开关到执行权限的唯一映射（V2.6）

保留现有 54 个 `StrengthOptionKey` 和十五项高级攻击政策。普通页独立可关的动作由同一 `StrengthPolicy` 内的 `AttackActionPolicy` 子记录表达，绝不在客户端或 executor 保存第二份策略。

- `AttackActionPolicy` 保存下表十四个字段；每个配置值为 `FOLLOW_PROFILE / DISABLED / COMPATIBLE / ENHANCED / EXTREME`，与普通页“跟随预设/关闭/兼容/强化/极限”一致。解析得到 `actionAllowed + requestedActionDepth`，不是只有布尔开关，也不是新的高级能力枚举。
- `actionMatrixRevision=1` 固定下表默认许可；`policyRevision/hash` 同时覆盖普通字段、54-key 图及两类矩阵 revision。配置图/激活图继续整体分离，重启时整体生效。
- 攻击来源追踪直接编辑现有 `ATTACK_SOURCE_TRACE`，普通页与高级页只是同一字段的两个视图，不增设同名布尔值。
- 顶部“攻击强度”是批量编辑入口，不是独立存储字段。应用整套预设明确重置普通许可与高级图；之后改单项进入 Custom，其余项固化为当时解析值。不能因为从其他页返回而重新套预设。

下表“键”省略共同前缀 `ATTACK_`；多键表示按实际 step 需要组合校验，不是任意一个开启就全部授权。“开”表示许可开启且请求本列档位的动作深度，“关”表示 DISABLED；各深度语义见 [§6.6.1](MS-MSITEM-PLAN.md#orig-v26-l0778)。普通子项可单独选档，高级 policy 不足时不自动抬高依赖，只如实降低可执行深度。

| GUI / 子字段 | 兼容许可 | 强化许可 | 极限许可 | 相关高级键 | 可授权动作及关闭语义 |
|---|---|---|---|---|---|
| 强力攻击 `strongAttack` | 开 | 开 | 开 | 各 step 对应键 | 增强编排方式；自身不授予改血、终结、直接清除或代码改写；关时保留其他独立开启项 |
| 改写生命 `rewriteLife` | 开 | 开 | 开 | STATE_SOURCE | 当前/最大/附加生命的攻击性写入；关时不得以状态源攻击改血 |
| 强制终结 `terminalize` | 开 | 开 | 开 | FINAL_RESULT_TAKEOVER、REGISTRATION_SOURCE | 新的持久 TERMINAL/资格撤销；关时不创建此类新判决，但不撤销已提交 Tombstone |
| 直接清除 `directClear` | 关 | 开 | 开 | FINAL_RESULT_TAKEOVER | 绕过普通死亡表现的移除；关时可以普通死亡后正常移除，不能偷偷改走 bypass |
| 实体记录清除 `clearEntityRecords` | 开 | 开 | 开 | STATE_SOURCE、REGISTRATION_SOURCE | 主动额外索引/容器清理；关时不禁止标准移除必需的记录一致性维护 |
| 重生压制 `suppressRespawn` | 开 | 开 | 开 | REGISTRATION_SOURCE；主动处置恢复器另需 RECOVERY_SOURCE | 同主体准入校验及获许可的恢复源压制；关时已有 Tombstone 拒绝旧资格仍有效 |
| 重建压制 `suppressRebuild` | 关 | 开 | 开 | RECOVERY_SOURCE、TARGET_CLASS_LINEAGE | 对新身体/UUID/producer 的主动压制；必须先解析身份与证明范围 |
| 生成压制 `suppressAdmission` | 开 | 开 | 开 | REGISTRATION_SOURCE、NETWORK_RECONSTRUCTION、SAVE_RECONSTRUCTION | 扩展生成/网络/存档入口压制；关时不关闭已提交 Truth 的基础 Admission |
| 渲染攻击 `attackRender` | 开 | 开 | 开 | FINAL_RESULT_TAKEOVER；第三方非实体显示另需 NON_ENTITY | 原版可见性收敛及获许可的 renderer/投影失能；关时仍允许标准移除包使原版模型消失 |
| 追踪清除 `clearTracking` | 开 | 开 | 开 | NETWORK_RECONSTRUCTION、FINAL_RESULT_TAKEOVER | 额外 tracking/cache/重订阅处置；关时不截断正常移除协议 |
| 血条/HUD 清除 `clearHud` | 开 | 开 | 开 | FINAL_RESULT_TAKEOVER；第三方逻辑 HUD 另需 NON_ENTITY，额外同步链另需 NETWORK_RECONSTRUCTION | 原版 BossBar 收敛及获许可的 HUD/overlay 处置；不会因此授权任意客户端内存修改 |
| 代码防御剥离 `stripCodeDefense` | 关 | 开 | 开 | RUNTIME_PATCH_DISABLEMENT、DYNAMIC_CODE、TARGET_CLASS_LINEAGE | 精确防御代码/补丁生产者失能；关时只观察相关代码，不借别名进行同效果改写 |
| 调用链攻击 `attackCallChain` | 关 | 开 | 开 | LOGIC_SOURCE、RECOVERY_SOURCE、TASK_CHAIN、CALLBACK_CHAIN、DISPATCH | 主动取消/替换/失能相关调用链；关时保留观察，不授权这种破坏性执行 |
| 全返回 `allReturn` | 关 | 开 | 开 | DISPATCH；改字节码时另需 DYNAMIC_CODE 或 RUNTIME_PATCH_DISABLEMENT | 空执行、固定返回或等价完成抑制；关时任何实现路径均不得做同等主动失能 |

每种 executor 注册 `actionRequirements[]`、`keyRequirements[]`、`effectClass`、依赖、预期副作用与 CompletionContract。一个动作同时具有代码剥离、调用链破坏、返回失能效果，就必须同时满足相应普通许可；不能只改操作名字绕过 OFF。高级键的 MONITOR 仅允许观察，自身不能把普通许可“开”变成可执行。

上表明确列出的“另需”按 step 的真实效果判断：标准实体/BossBar 收敛不冒充第三方非实体攻击；一旦进入后者必须增加 NON_ENTITY 等必要键，不能以“基础路径”命名逃避。Compatible 中这些高级键为 MONITOR 时，既有标准闭环可继续，扩展攻击只能观察。具体生产操作注册表必须给出所用分支和强度差异，并在 X57 中逐分支测试。

最终执行是以下条件的**交集**：当前 Root 授权、普通动作许可及 requestedActionDepth、全部所需高级键的 effective policy、当前归属证据、运行时支持、lease 与宿主预算。实际动作深度不得高于普通子项和任一必要高级键的许可；强力攻击及最终结果接管不是 OR 总开关，不得覆盖其中任何一项。

冲突固定按以下规则处理：

1. 所需普通许可为 DISABLED：拒绝该 step，报告 `ACTION_DISABLED`；不得自动升档或开启依赖。
2. 许可为开但所需高级政策、组件或证据不足：报告具体 dependency/ceiling 缺口，可选择不越权的其他路径；否则保持真实 PARTIAL/不可用。
3. `allReturn=DISABLED + ATTACK_DISPATCH=STRICT_ALL`：可观察、验证分派或执行其他已许可动作，不能全返回；拦截页与防护页的另两种全返回不受该攻击许可关闭影响。
4. `directClear=DISABLED + FINAL_RESULT_TAKEOVER=STRICT_ALL`：只可用仍获许可的生命周期路径完成目标；不能用“最终接管”绕过死亡表现开关。
5. 关闭能力只约束新 step；撤销已有 Judgment 必须另走显式 Authority command。已提交义务不被删掉；需要被关闭能力才能续作的义务保持 pending，基础安全 gate 继续维持，不继续调用已禁用执行器。
6. 候选限制、单 writer、防回滚、基础 Admission 与资源安全不是攻击许可副作用，不可通过普通 OFF 关闭；UI 必须说明“关闭主动加强，不解除已有裁决”，不能显示成完整禁用 Truth。

**迁移与证据：** 本子配置预留给后续 policy schema 3，不改 r53 已有 schema 2 字节或哈希。schema-2 升级先验证旧 policy/journal，再由 Root 提交完整的新配置图并等待重启；旧 receipt 保留原 payload/hash 语义。预设配置依据上述 revision-1 默认值生成许可；旧 Custom 无法证明对应许可时，新增十四项默认 DISABLED，并显示待确认，不从 STRICT_ALL 猜出“用户同意全返回”。既有安全下限仍独立有效。schema-3 缺字段、重复字段、未知 action/matrix revision 拒绝激活；禁止旧二进制静默忽略子字段。旧 Custom 迁移到新执行系统前必须展示差异，不宣称行为完全等价。

本轮仅冻结这份迁移目标；r53 原有 54-key matrixRevision=1、schema=2 和全部源码保持不变。


<!-- RONOVA-ORIGINAL-END orig-v26-l0884 -->

<a id="orig-full-l0146"></a>

**迁入原文｜FULL｜旧 L146–L159** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-full-l0146)

原标题路径：Ronova 7.17 极限档全体系加强规划 V1.0 → 3. 普通动作同样进入极限范围

<!-- RONOVA-ORIGINAL-BEGIN orig-full-l0146 -->
#### 3. 普通动作同样进入极限范围

14 个普通动作继续逐项适用其固定依赖，不增加一个可以绕过其余开关的总动作。

| 动作组 | 极限实现方向 |
|---|---|
| strongAttack | 编排已经分别获准的动作，维持原因和效果身份 |
| rewriteLife | 多来源生命模型、精确写入、反弹反馈及维护写源处置 |
| terminalize | 原 Root ONCE 语义、死亡归因和未知结果恢复 |
| directClear / clearEntityRecords | 独立字段与精确容器处置，重复状态修复不重复外部生命周期 |
| suppressRespawn / suppressRebuild / suppressAdmission | 原身份、新实例、producer、后继任务和保存/网络重建分别处理 |
| attackRender / clearTracking / clearHud | 处理目标归属的真实显示和追踪资源，清理后独立观察 |
| stripCodeDefense / attackCallChain / allReturn | 作用范围来自真实关系、固定依赖和当前许可；恢复反弹后继续处置相关路径 |


<!-- RONOVA-ORIGINAL-END orig-full-l0146 -->

<a id="orig-rec-l0115"></a>

**迁入原文｜REC｜旧 L115–L136** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0115)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 3. 接入现有操作与开关

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0115 -->
#### 3. 接入现有操作与开关

不新增第十五个普通动作、第五十五个高级键或新页面。
入口沿用“重生压制”“重建压制”“调用链攻击”等现有选项。

| 处置效果 | 必需普通许可 | 必需高级键，除普通项固有依赖外 |
|---|---|---|
| 同主体恢复源处置 | `SUPPRESS_RESPAWN` | `ATTACK_RECOVERY_SOURCE`；改记录另需 `ATTACK_STATE_SOURCE` |
| 新身体、换 UUID 或 producer 重建处置 | `SUPPRESS_REBUILD` | 固有 `ATTACK_RECOVERY_SOURCE`、`ATTACK_TARGET_CLASS_LINEAGE` |
| SavedData/NBT 持久恢复记录处置 | 按上两项实际效果选择；两者都有则都需要 | `ATTACK_STATE_SOURCE`、`ATTACK_SAVE_RECONSTRUCTION` |
| 主动取消任务、断开订阅或恢复调用链 | `ATTACK_CALL_CHAIN`，并满足相应重生/重建许可 | 沿用该动作的全部逻辑源、恢复源、任务、回调和分派依赖 |
| 额外注册/重建准入处置 | `SUPPRESS_ADMISSION` 及实际涉及的重生/重建许可 | 沿用该动作全部固有依赖 |
| 代码失能或返回抑制 | 另需 `STRIP_CODE_DEFENSE` / `ALL_RETURN`，按实际效果取交集 | 相应动态代码、补丁和分派依赖；不是记录清理的隐式权力 |

每种生产 executor 固定声明自己的全部 action/key requirements，调用方不能删减依赖。
执行深度取当前 Root 许可、普通动作深度、所有高级 effective policy、来源证明、
运行能力、lease 和资源预算的交集。任何 OFF 或 MONITOR 都不能被“强力攻击”绕过。

兼容档保持原基础闭环与观察；强化档处理已经证明的目标记录和直接恢复链；
极限档继续纳入结果相关候选并追踪后继，但不把候选评分当成永久处置权限。
本功能不自动改动玩家的预设，也不把 configured 值当作 active 值。


<!-- RONOVA-ORIGINAL-END orig-rec-l0115 -->

<a id="orig-road-l0211"></a>

**迁入原文｜ROAD｜旧 L211–L239** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-road-l0211)

原标题路径：Ronova L6／L6E 全量加强路线 V1.0 — Internal Root → 5. 全部 14 个普通动作

**规范关系调整：** 本段的旧位次/冻结不可覆盖规则仅保留作来源记录，不再阻止显式修订；技术不变量本轮未据此删除。参见 [当前文档效力](README.md#scope-governance)。

<!-- RONOVA-ORIGINAL-BEGIN orig-road-l0211 -->
#### 5. 全部 14 个普通动作

动作字段继续使用 `FOLLOW_PROFILE / DISABLED / COMPATIBLE / ENHANCED / EXTREME`。
下表沿用 Strength Master [V2.6 §6.7](MS-MSITEM-PLAN.md#orig-v26-l0884) 的默认许可；“开”表示请求该档深度，实际执行取所有必要条件的交集。
固定高级依赖来自现有 `AttackAction.requiredKeys()`；表内“另需”来自实际步骤效果的追加要求。
动作以同一份 StrengthPolicy 保存，不在客户端或执行器中复制策略。

| 动作 | 默认许可（兼容／强化／极限） | 固定高级依赖及追加要求 | 执行范围与后续加强 | 关闭语义与副作用 |
|---|---|---|---|---|
| `strongAttack` | 开／开／开 | 各实际 step 的依赖；本字段不增加独立高级键 | 编排已分别开启且获准的动作，接合 Root 计划、原因、步骤和结果 | 关闭仅停用该编排方式；不关闭其他独立项，也不自动开启子动作 |
| `rewriteLife` | 开／开／开 | `attack.state_source` | 当前、最大、附加和实际存活来源的获准改写；A/B/C 扩展状态归属、联写与反弹处理 | 关闭后不得借状态源改血；重复状态修复不调用死亡/掉落回调 |
| `terminalize` | 开／开／开 | `attack.final_result_takeover`、`attack.registration_source` | 创建新的持久 TERMINAL/资格撤销；L6 原子提交，A/C 接合当前身份和代际 | 关闭不撤销已提交 Tombstone；不可逆生命周期保留原 ONCE 与未知效果身份 |
| `directClear` | 关／开／开 | `attack.final_result_takeover` | 绕过普通死亡表现的获准移除；B/C 维护独立字段、入口与结果 | 关闭可保留正常死亡后的移除，不能通过“最终接管”调用同等 bypass |
| `clearEntityRecords` | 开／开／开 | `attack.state_source`、`attack.registration_source` | 额外索引、容器和精确条目的主动清理；A/C 覆盖当前归属和重插来源 | 关闭不禁止标准移除必要的一致性维护；清理同一条目不重复外部生命周期 |
| `suppressRespawn` | 开／开／开 | `attack.registration_source`；主动处置恢复器另需 `attack.recovery_source` | 同主体准入与获准恢复源压制；A/C 关联实例和合法重新激活 | 关闭停止主动加强，已有 Tombstone 对旧资格的基础拒绝继续有效 |
| `suppressRebuild` | 关／开／开 | `attack.recovery_source`、`attack.target_class_lineage` | 已证明的新身体、新 UUID、producer 和后继链主动压制；A/B/C/D 联动 | 必须解析具体因果与身份；关闭不授权用其他名称处置同一重建链 |
| `suppressAdmission` | 开／开／开 | `attack.registration_source`、`attack.network_reconstruction`、`attack.save_reconstruction` | 扩展生成、网络与保存入口的压制；L6/A/C 统一版本和资格事务 | 关闭不关闭既有权威 truth 的基础 Admission，也不自动解除原裁决 |
| `attackRender` | 开／开／开 | `attack.final_result_takeover`；第三方非实体显示另需 `attack.non_entity` | 原版可见性及获准 renderer/投影处置；C/D 接合资源，P1 可选深层绘制边界 | 关闭仍可发送标准移除包；只对实际可归属客户端范围声明结果 |
| `clearTracking` | 开／开／开 | `attack.network_reconstruction`、`attack.final_result_takeover` | 额外 tracking、缓存和重订阅处置；B/C 验证当前连接与迟到数据 | 关闭不截断正常移除协议；旧 ACK 与重复包不产生新的完成结果 |
| `clearHud` | 开／开／开 | `attack.final_result_takeover`；第三方逻辑显示另需 `attack.non_entity`，额外同步链另需 `attack.network_reconstruction` | BossBar、HUD、overlay 的获准处置；B/C/D 关联来源、同步和资源 | 关闭不阻止标准协议收敛；不授予任意客户端内存修改权限 |
| `stripCodeDefense` | 关／开／开 | `attack.runtime_patch_disablement`、`attack.dynamic_code`、`attack.target_class_lineage` | 具体防御代码、补丁 producer 失能；B/D 覆盖动态重现与当前语义 | 关闭只保留相应攻击观察，不通过别名继续同效果代码改写 |
| `attackCallChain` | 关／开／开 | `attack.logic_source`、`attack.recovery_source`、`attack.task_chain`、`attack.callback_chain`、`attack.dispatch` | 取消、替换或失能相关调用链；B/C/D 覆盖后继、回调和提交出口 | 关闭保留观察，不授权主动破坏；正在运行和已结束分别记录 |
| `allReturn` | 关／开／开 | `attack.dispatch`；改字节码时另需 `attack.dynamic_code` 或 `attack.runtime_patch_disablement` | 空执行、固定返回或等价完成抑制；B/D 按实际返回合同执行 | 关闭禁止所有同等主动失能路径；防护与拦截侧的另两种全返回独立适用 |

各 executor 注册动作要求、高级键要求、预期效果和 CompletionContract。
同一步骤同时包含代码剥离、调用链破坏和返回失能时，必须满足全部对应动作许可。
MONITOR 仅提供观察；高级键处于 STRICT_ALL 也不能覆盖普通项的 DISABLED。
关闭主动能力不自动撤销已提交判断；需要该能力才能继续的工作保留 pending，停止调用被关闭执行器。


<!-- RONOVA-ORIGINAL-END orig-road-l0211 -->

<a id="orig-rule-l0550"></a>

**迁入原文｜RULE｜旧 L550–L558** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rule-l0550)

原标题路径：Ronova MS 规则内加强方案 V2.0 → 4. 缺口清单（按 §0.4 分层） → N11 —— 调度收敛（R32/R64）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

**r66 接线规划更正（fix4，非迁入原文）：** D 块旧判断已收窄：命令中的“未接线”是硬编码诊断，不是全执行器枚举；服务器当前路径委派 `TerminalPlanExecutor` 并回调 `runPlannedIntegration`。保留动作接线工作，不恢复旧无计划路径；其余完成条件生产者仍须逐项核查。 [当前更正](R66-CAPABILITY-WIRING-PLAN.md#action-wiring)。

<!-- RONOVA-ORIGINAL-BEGIN orig-rule-l0550 -->
#### N11 —— 调度收敛（R32/R64）

`coremod/command/DominionCommands.java:280-281` 对 14 个动作中的 13 个显示
`"未接线（仅保存请求）"`；`TerminalActionPolicy.java:48` 把结果钳制在 `REGISTERED_ENTITY_ONLY`。
[`msitem:411`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#完成标准) 的完成标准要求「所有击杀入口都收敛到一个可去重、可审计、服务端权威的动作调度器」。

> 这一条**不是违规**（未接线不等于违约），而是 [`msitem:406-418`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#完成标准) 完成标准意义上的缺口，
> 且它同时限制了 R62/R63 的可达性。


<!-- RONOVA-ORIGINAL-END orig-rule-l0550 -->

<a id="ms-msitem-l142"></a>

## 用统一动作上下文消除重复

**框架来源：** [MS msitem，原 L142–L167](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#用统一动作上下文消除重复)。

**编排说明（非原条款）：** 承接 CriticalOperationId、上下文、输入与快照；MS ActionContext 是组织参考，不替换原 RootCommandId 等稳定身份。

**文档缺口：** [GAP-DOC-03 · MS 动作上下文到 Ronova 类型的逐字段映射](RESTRUCTURE-MAP.md#gap-doc-03)。

<a id="orig-cf-l0325"></a>

**迁入原文｜CF｜旧 L325–L350** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0325)

原标题路径：3. Canonical Identifier Model → 3.6 CriticalOperationId

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0325 -->
#### 3.6 CriticalOperationId

所有 Authority-sensitive operation 必须使用稳定的操作类型标识。

示例类别：

```text
VITAL_WRITE
TERMINALIZE
ADMISSION_BIND
PRESENCE_PUBLISH
PRESENCE_REVOKE
INCARNATION_REBIND
EQUIPMENT_ENFORCE
MOVEMENT_ENFORCE
EFFECT_ENFORCE
SIDE_EFFECT_PUBLISH
```

要求：

- 不使用方法名作为唯一语义；
- OperationId 表示 Authority 意图，不表示 Java 实现细节。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0325 -->

<a id="orig-msc-l0059"></a>

**迁入原文｜MSC｜旧 L59–L68** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-msc-l0059)

原标题路径：Ronova × MS 要求 合规与加强方案 V1.0 → 1. 要求清单与现状判定 → 1.3 统一动作调度

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-msc-l0059 -->
#### 1.3 统一动作调度

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-C1 | 全部入口汇入**同一** `ActionDispatcher` | [`msitem:128`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#列出全部击杀入口)、`:140` | `JudgmentEngine` 三条入口统一收敛到 `judgeRootsReport` | ✅ |
| R-C2 | 只有**一个结算所有者**；其余路径只报告输入/补上下文/取消重复/播表现 | [`msitem:140`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#列出全部击杀入口) | `onEntitySwing` 故意空实现（注释：避免跨 tick 重复裁决） | ✅ |
| R-C3 | `ActionContext` 含动作 ID/手/槽位/不可变快照；不用 `ItemStack` 引用作延迟身份 | [`msitem:124`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#物品层只承担入口与表现) | `ActionRequest(realm, session, seq, action)` + `SessionJournal` | ✅ |
| R-C4 | 去重与幂等 | [`msitem:168-179`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#去重与幂等) | `RootCommandId` + canonical payload hash 幂等 | ✅ |
| R-C5 | 服务端**重新验证**后才执行 | [`msitem:35`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#先定义击杀契约) | `ServerActionService` 校验 + `RuntimeCoordinator` | ✅ |


<!-- RONOVA-ORIGINAL-END orig-msc-l0059 -->

<a id="orig-ds-l0144"></a>

**迁入原文｜DS｜旧 L144–L153** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0144)

原标题路径：Ronova 设计规范 · 合并导读 → 3. 身份与修订合同（`CF` §3–§4） → 3.6 操作与完成标识（`CF:325-369`）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0144 -->
#### 3.6 操作与完成标识（[`CF:325-369`](RESTRUCTURE-MAP.md#range-cf-325-369)）

- **`CriticalOperationId`**：所有 Authority-sensitive operation 必须使用**稳定的操作类型标识**；
  **不使用方法名作为唯一语义**。类别：VITAL_WRITE / TERMINALIZE / ADMISSION_BIND / PRESENCE_PUBLISH /
  PRESENCE_REVOKE / INCARNATION_REBIND / EQUIPMENT_ENFORCE / MOVEMENT_ENFORCE / EFFECT_ENFORCE / SIDE_EFFECT_PUBLISH。
- **`CompletionContractId`**：所有 Authority-bearing completion **必须引用 CompletionContract**
  （TERMINAL_COMMIT_V1 / VITAL_WRITE_V1 / ADMISSION_BIND_V1 / PRESENCE_REVOKE_V1…）。
- **`CompletionContract`**（[`CF:558-568`](MS-SKILLS-PLAN.md#orig-cf-l0555)）：`allowedKinds` 是**封闭集合**，未列出的 completion 不合法；
  `failureMapping` **不得把 `UNKNOWN`/`STALE`/`FENCED` 映射为普通 `false`**。


<!-- RONOVA-ORIGINAL-END orig-ds-l0144 -->

<a id="ms-msitem-l168"></a>

### 去重与幂等

**框架来源：** [MS msitem，原 L168–L179](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#去重与幂等)。

**编排说明（非原条款）：** 保持动作/目标去重、效果身份、ONCE、COMMIT_UNKNOWN 和跨版本重复防护的不同作用域。

<a id="orig-v26-l2182"></a>

**迁入原文｜V26｜旧 L2182–L2223** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l2182)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 12. 运行时与宿主安全 → 12.6 副作用未知与跨版本去重（V2.6）

**规范关系调整：** 本段的旧位次/冻结不可覆盖规则仅保留作来源记录，不再阻止显式修订；技术不变量本轮未据此删除。参见 [当前文档效力](README.md#scope-governance)。

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l2182 -->
#### 12.6 副作用未知与跨版本去重（V2.6）

Root 事务原子性不覆盖第三方 callback、掉落、外部 registry mutation 等物理副作用。发生“副作用已产生，step receipt 尚未持久化就崩溃”时，不能用本地缺 receipt 推断动作未执行，也不能承诺任意第三方 exactly-once。

复用 [L6E-0 §16](MS-MSITEM-PLAN.md#orig-cf-l1011) 的 `RONOVA_CONTROLLED / REVERSIBLE / COMPENSATABLE / EXTERNAL_OPAQUE` 分类。每个 typed step 另声明恢复能力；该能力是证据，不从动作名称推测：

| 恢复能力 | 必须证明 | 效果未知后的处理 |
|---|---|---|
| `IDEMPOTENT` | 同 effectKey、同语义重复执行的全部副作用幂等；不仅最终字段值相同 | 复验授权及目标状态后可有界重放原效果 |
| `QUERYABLE` | 能按 effectKey 查询已应用/确实未应用；查询覆盖延迟 callback 和旧执行 attempt | 查明已应用则补收据；证明确未应用且旧 attempt 已失效后才能续作；否则仍未知 |
| `COMPENSATABLE` | 补偿对象、版本、范围及补偿自身的幂等/查证合同完整 | 不能因“支持补偿”就盲目重放；先查证效果，再由 Root 批准补偿；补偿失败保持 incident/pending |
| `OPAQUE` | 缺少上述可证能力或副作用语义未知 | 不盲目重做、不猜测回滚；最小必要 OPERATION fence、保留 obligation 与诊断 |

`REVERSIBLE` 的反向动作必须证明当前 revision 和原效果归属；已经发生其他写入时不能套旧 inverse。`RONOVA_CONTROLLED` 也需真实的 publication dedup，不能因“是自己的代码”跳过证明。复合 step 含任一不可控非幂等副作用时，不能整体标 IDEMPOTENT。

最低执行记录（仅由既有 Root/Store writer 接受与持久化）：

```text
effectKey = (RealmId, canonicalCommandId, SubjectKey, qualifiedTargetEpoch,
             semanticOperationId, semanticSlotId)
effectPayloadHash / sideEffectClass / recoveryCapability
planId / planRevision / stepId / attemptId
expectedRevisions / authorityReceiptRef / leaseRef
effectState = PREPARED | EFFECT_UNKNOWN | EFFECT_CONFIRMED | NO_EFFECT_CONFIRMED
effectEvidenceRef / observedRevision / compensationRef
```

`semanticSlotId` 来自 canonical 计划的稳定语义位置，不能用随机重试 ID 或 planRevision 充当新效果身份。相同 command 的重规划沿用同一 effectKey；同 key 不同 payloadHash 拒绝，不能覆盖旧记录。确有新的、已授权物理效果时须显式分配新语义 slot 并记录与旧效果的关系；单纯为了绕过去重不得分配新 slot。持续 state reassertion 与 ONCE lifecycle integration 继续分开，前者采用已证明幂等的 reassert 合同，不把新的掉落混入重复修复。

执行顺序固定为：

1. 确认原 Authority command 已提交；通过同一 writer 持久化 effectKey、payloadHash 与 PREPARED 意图。该意图不额外改变 Judgment/TargetEpoch，不能由 Surface 写独立 journal 真值。
2. PREPARED 获持久 ACK 后，执行器在真实提交门复验当前资格/许可/lease；只允许一个持有当前 attempt lease 的执行者。没有 durable intent ACK 不调用物理副作用。
3. 在 Root 事务之外执行物理动作；成功返回仍需检查效果及副作用证据，再持久化 EFFECT_CONFIRMED，或在可证明没有效果时记录 NO_EFFECT_CONFIRMED。
4. 调用超时、返回/落盘中断、崩溃后只剩 PREPARED 均保守解释为 EFFECT_UNKNOWN。抛出异常本身不证明没有副作用。
5. 对 EFFECT_UNKNOWN 按上表解析。只查询到“暂时不存在”但旧 callback 仍可能完成时，不是 NO_EFFECT_CONFIRMED；无真实 gate 能撤销旧 attempt 的路径继续保持未知。
6. 资格已变或动作已关闭时不重放旧效果，先解析历史效果是否存在，再针对当前 Truth 制定获授权的补偿/收敛计划；旧 epoch 永不因恢复而续租。

`EFFECT_UNKNOWN` 是 step 的执行事实，**不是**新的 Root commit 状态，也不是 `attackOutcome` 的新增枚举。Root COMMIT_UNKNOWN 用原 commandId/receipt 协议处理；确认 Root 已提交仍可能存在 EFFECT_UNKNOWN。必要 step 未解析时 completionSatisfied=false；有已知局部效果可报告 PARTIAL，无任何效果证据时不得虚报 APPLIED。

这些记录可以压缩，但未解决 effectKey、原 payload 语义、去重与恢复所需证据不得被 TTL/LRU 抹掉。容量治理按 [§20.5](MS-SKILLS-PLAN.md#orig-v26-l3202)；幂等/查询能力缺失是可观测的执行上限，不是缩小既有 Judgment 或宣称成功的理由。


<!-- RONOVA-ORIGINAL-END orig-v26-l2182 -->

<a id="orig-rec-l0240"></a>

**迁入原文｜REC｜旧 L240–L279** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0240)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 7. 证据、效果身份与恢复

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0240 -->
#### 7. 证据、效果身份与恢复

来源证据至少携带：

```text
SubjectKey / physical incarnation / TargetEpoch
authorityReceiptRef / active and configured policy revisions
source instance / defining loader / observer revision
container or scheduler identity / exact entry or task reference
record or snapshot revision / actual reconstruction event refs
relationship state / affected scope / write-sink revision
recovery capability / bounded evidence refs
```

运行时引用为受预算约束的弱引用或短期句柄，不进入 durable truth。
持久 locator 保存可复验的存储和目标语义位置；重启后必须重新解析实际 holder，
不能把旧 JVM 的 identity hash、指针、类名或 loader 名当作当前授权。

来源处置沿用原 `EffectKey` 结构，但注册独立的来源操作语义与稳定 slot。
slot 绑定规范来源位置和目标记录，不能使用 planRevision、随机重试 ID 或临时对象地址。
相同 key 不同 payload 冲突拒绝；来源真实替换须明确建立新关系与新语义位置。

当前 `EffectJournal` 已登记 OPAQUE 死亡/生命周期语义，以及 Root 自己的来源资格回执。
来源资格回执只确认重建限制，不表示外部记录、任务或持久文件已处置。
外部处置仍须补齐其专用语义的编码、解码、校验、恢复和旧版本拒绝规则，再允许落盘。
恢复能力按实际证明选择：IDEMPOTENT、QUERYABLE、COMPENSATABLE 或 OPAQUE。
复合步骤含任一未知回调时，不能整体标成幂等。

```text
原 Authority receipt 已确认
→ 同一 Root writer 持久接纳来源义务及 PREPARED intent
→ ACK 后在最终提交门复验资格、许可与当前 attempt
→ Root 事务外执行物理动作
→ 查询完整效果与迟到后继
→ 持久记录已确认结果，或保留 EFFECT_UNKNOWN
```

只有能够证明“确未执行且旧 attempt 已失效”时，才可补做未知操作。
恢复源记录清理不把旧死亡/掉落的 EFFECT_UNKNOWN 改成已完成，也不补发掉落、奖励或成就。


<!-- RONOVA-ORIGINAL-END orig-rec-l0240 -->

<a id="orig-cf-l1011"></a>

**迁入原文｜CF｜旧 L1011–L1023** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1011)

原标题路径：16. Side-Effect Contract

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1011 -->
#### 16. Side-Effect Contract

SideEffect 分为：

```text
RONOVA_CONTROLLED
REVERSIBLE
COMPENSATABLE
EXTERNAL_OPAQUE
```

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1011 -->

<a id="orig-cf-l1024"></a>

**迁入原文｜CF｜旧 L1024–L1042** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1024)

原标题路径：16. Side-Effect Contract → 16.1 RONOVA_CONTROLLED

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1024 -->
#### 16.1 RONOVA_CONTROLLED

Ronova 自己拥有 publication path。

允许：

```text
suppress before publish
```

例如：

- Ronova packet；
- server-owned projection；
- Ronova BossEvent；
- Ronova presence publication。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1024 -->

<a id="orig-cf-l1043"></a>

**迁入原文｜CF｜旧 L1043–L1054** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1043)

原标题路径：16. Side-Effect Contract → 16.2 REVERSIBLE

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1043 -->
#### 16.2 REVERSIBLE

具有确定 inverse operation。

允许 rollback，但必须：

- 有 revision；
- 可验证；
- 不产生二次不一致。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1043 -->

<a id="orig-cf-l1055"></a>

**迁入原文｜CF｜旧 L1055–L1068** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1055)

原标题路径：16. Side-Effect Contract → 16.3 COMPENSATABLE

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1055 -->
#### 16.3 COMPENSATABLE

不保证精确回滚，只能进行已知补偿。

例如某些：

- 掉落；
- XP；
- scoreboard。

补偿失败必须产生 Incident。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1055 -->

<a id="orig-cf-l1069"></a>

**迁入原文｜CF｜旧 L1069–L1084** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1069)

原标题路径：16. Side-Effect Contract → 16.4 EXTERNAL_OPAQUE

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1069 -->
#### 16.4 EXTERNAL_OPAQUE

Ronova 不知道或无法证明其副作用语义。

处理：

```text
observe
→ incident
→ optional degrade/fence
```

不得盲目修改第三方系统。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1069 -->

<a id="orig-cf-l1577"></a>

**迁入原文｜CF｜旧 L1577–L1591** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1577)

原标题路径：29. Freeze Decision Summary → ID-2 — Side Effects

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1577 -->
#### ID-2 — Side Effects

默认：

```text
Ronova-controlled → suppress
known reversible → rollback
known compensatable → compensate
external opaque → incident
```

不构建全局副作用总拦截器。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1577 -->

<a id="orig-p0a-l0775"></a>

**迁入原文｜P0A｜旧 L775–L798** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0775)

原标题路径：14. RootCommandId 与幂等

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0775 -->
#### 14. RootCommandId 与幂等

所有 durable mutation：

```text
RootCommandId
```

必须唯一且可重试。

规则：

```text
same commandId + same payload
→ 返回同一 durable receipt，或继续 recovery

same commandId + different payload
→ COMMAND_ID_CONFLICT
```

Canonical payload hash 采用 [L6E-0 §5](MS-SKILLS-PLAN.md#orig-cf-l0398) 的稳定编码；不得用 Java serialization、Map 迭代顺序或对象 identity 计算。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0775 -->

<a id="orig-road-l0350"></a>

**迁入原文｜ROAD｜旧 L350–L356** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-road-l0350)

原标题路径：Ronova L6／L6E 全量加强路线 V1.0 — Internal Root → 9. 接口衔接、效果与迁移 → 9.3 效果与未知结果

<!-- RONOVA-ORIGINAL-BEGIN orig-road-l0350 -->
#### 9.3 效果与未知结果

生命周期集成保持 ONCE；状态修复采用可重复、幂等的对应步骤。
命令、计划、步骤和外部效果分别保留各自稳定身份，换实现版本不能补发死亡、掉落或奖励。
取消请求不等于任务结束；方法返回不等于保存、原生提交或客户端完成。
不可确定的外部效果保持 UNKNOWN，通过原身份与适用恢复协议处理。


<!-- RONOVA-ORIGINAL-END orig-road-l0350 -->

<a id="orig-rule-l0175"></a>

**迁入原文｜RULE｜旧 L175–L187** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rule-l0175)

原标题路径：Ronova MS 规则内加强方案 V2.0 → 1. 规则基（L0 铁律 + L1 参考库） → 闸门 4 —— 结算唯一性闸

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-rule-l0175 -->
#### 闸门 4 —— 结算唯一性闸

| ID | 规则 | 依据 |
|---|---|---|
| **R32** | 必须选择一个**结算所有者**；只有统一调度器可把动作推进到执行状态 | [`msitem:140`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#列出全部击杀入口)、[`msitem:299`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#事件层) |
| **R33** | Forge 事件、物品覆写、标准 Sponge Mixin 和自定义转换器都是入口或契约维持层，**不能各自成为第二个击杀执行器** | [`msitem:288`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#forge-事件mixin-与自定义转换器) |
| **R34** | 一次动作对一个目标只产生一个最终状态转换 | [`msitem:259`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#混合模式) |
| **R35** | 至少按 `actionId` 与目标稳定身份去重；同一 tick 不得为同一目标建多次任务 | [`msitem:168-178`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#去重与幂等) |
| **R36** | 队列必须有每 tick 总预算、每发起者预算；世界关闭/维度切换/实体卸载/玩家离开时丢弃无效任务；单任务失败隔离并记录上下文；不保留长期强引用 | [`msitem:275-282`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#服务端队列与线程边界) |
| **R37** | 开始持续使用不能直接提交无限任务 | [`msitem:135`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#列出全部击杀入口) |
| **R38** | 真正执行前再次验证：物品仍在允许的发起者手中、目标仍在同一维度、目标不在冲突状态、动作未超时、冷却与资源仍有效 | [`msitem:284`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#服务端队列与线程边界) |
| **R39** | 不得重复结算：战利品只结算一次；冷却/耐久/资源成本只在设计结果真正发生时提交 | [`msitem:234`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#正常死亡) |


<!-- RONOVA-ORIGINAL-END orig-rule-l0175 -->

<a id="orig-ds-l0677"></a>

**迁入原文｜DS｜旧 L677–L697** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0677)

原标题路径：Ronova 设计规范 · 合并导读 → 9. 副作用合同（`CF` §16 + `ID-2`）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0677 -->
#### 9. 副作用合同（`CF` [§16](MS-MSITEM-PLAN.md#orig-cf-l1011) + `ID-2`）

四类，**封闭**：

| 类别 | 处置 | 约束 |
|---|---|---|
| `RONOVA_CONTROLLED` | **suppress before publish** | 只限 Ronova 自有发布路径（自有包、服务端自有投影、自有 BossEvent、自有 presence 发布） |
| `REVERSIBLE` | rollback | **必须**：有 revision；可验证；**不产生二次不一致** |
| `COMPENSATABLE` | compensate | 不保证精确回滚，只做已知补偿；**补偿失败必须产生 Incident** |
| `EXTERNAL_OPAQUE` | observe → incident → 可选 degrade/fence | **不得盲目修改第三方系统** |

- **默认策略**（`ID-2`，[`CF:1577-1588`](MS-MSITEM-PLAN.md#orig-cf-l1577)）：Ronova-controlled → suppress / known reversible → rollback /
  known compensatable → compensate / external opaque → incident。
- **强制**：**不构建全局副作用总拦截器**；`sideEffectScope` 按 CompletionContract 逐合同限定。
- **压缩规则**（[`CF:947-963`](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0935)）：**"Coalesce work, not security evidence."** ——
  允许「10000 次写 → 1 个 repair job」；**不允许**「非法 death transition → 因最终 snapshot 恢复而丢证据」。
- **持久化归属**（[`CF:1098-1104`](MS-SKILLS-PLAN.md#orig-cf-l1085)）：本地缓存 Graph **可重建**；Root **只持久化**已确认的
  Authority Binding 与必要的 identity/ownership relationship；**不持久化所有临时 discovery noise**。

---


<!-- RONOVA-ORIGINAL-END orig-ds-l0677 -->

<a id="ms-msitem-l180"></a>

## 目标选择与快照

**框架来源：** [MS msitem，原 L180–L200](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#目标选择与快照)。

**编排说明（非原条款）：** 候选收集、稳定快照、Candidate-first、恢复源关系及执行前复验组织在此；有来源证据与最终处置级别仍是不同问题。

**文档缺口：** [GAP-DOC-02 · 扇形与范围几何的边界条件](RESTRUCTURE-MAP.md#gap-doc-02)。

<a id="orig-v26-l0258"></a>

**迁入原文｜V26｜旧 L258–L294** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0258)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 3. 正式术语 → 3.4 Candidate Attack Surface / 候选攻击面

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0258 -->
#### 3.4 Candidate Attack Surface / 候选攻击面

V2.2 定义，V2.3 按 [§9.2](MS-MSITEM-PLAN.md#orig-v26-l1327) 细化准入：

> **任何存在合理证据表明它可能改变当前 Judgment 最终结果的路径，都先进入 Candidate Attack Surface。**

候选证据可以来自：

- TERMINAL → PRESENT 漂移；
- 状态写入相关性；
- recovery / rebuild 时序；
- callback / Future / task 后继；
- registry/provider 重新发布；
- network/save 重建；
- dispatch / CallSite 改绑；
- dynamic class / transformer / runtime patch；
- native callback / bind；
- HUD / Presence 重建。

候选路径以 CANDIDATE 开始，归属判定有三种结果：

```text
CANDIDATE
    ↓
PROVEN_RELATED
PROVEN_UNRELATED
UNRESOLVED
```

规则：

- `PROVEN_RELATED`：进入正式结果图，由当前 Root 政策授权相应攻击强度；
- `PROVEN_UNRELATED`：只解除本候选原因建立的临时限制，不撤销其他 Judgment、Tombstone 或仍有效的隔离；
- `UNRESOLVED`：不从攻击域删除，继续保持有界追踪与可证结果出口限制；细节容量不足时由范围级 obligation 汇总承接。

Candidate Admission 是候选证据准入，不是 Entity Admission，也不直接给实体生成、复活或写入 durable truth 授权。准入门槛只决定何时建立候选及其临时处置；没有达到门槛不等于证明无关，原有受保护写入点仍按 Root 政策独立验证授权。


<!-- RONOVA-ORIGINAL-END orig-v26-l0258 -->

<a id="orig-v26-l1275"></a>

**迁入原文｜V26｜旧 L1275–L1276** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1275)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 9. 攻击平面 → 9.1 三档攻击范围

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1275 -->
#### 9.1 三档攻击范围


<!-- RONOVA-ORIGINAL-END orig-v26-l1275 -->

<a id="orig-v26-l1277"></a>

**迁入原文｜V26｜旧 L1277–L1287** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1277)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 9. 攻击平面 → 9.1 三档攻击范围 → Compatible

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1277 -->
#### Compatible

```text
当前目标实体
→ 标准 health / death / remove
→ 基础 Tombstone / Admission
→ 基础 tracking / render
```

不主动深挖第三方复杂恢复链。


<!-- RONOVA-ORIGINAL-END orig-v26-l1277 -->

<a id="orig-v26-l1288"></a>

**迁入原文｜V26｜旧 L1288–L1299** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1288)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 9. 攻击平面 → 9.1 三档攻击范围 → Enhanced

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1288 -->
#### Enhanced

```text
当前目标
→ 已识别状态源
→ 已登记 revive / factory / task
→ tracking / Presence / HUD
→ 已知代码防御 / direct recovery chain
```

仍尽量做目标级、实例级处置。


<!-- RONOVA-ORIGINAL-END orig-v26-l1288 -->

<a id="orig-v26-l1300"></a>

**迁入原文｜V26｜旧 L1300–L1326** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1300)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 9. 攻击平面 → 9.1 三档攻击范围 → Extreme

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1300 -->
#### Extreme

```text
当前 Judgment
    ↓
Candidate Attack Surface
    ↓
目标实体 / 非实体载体
    ↔ 状态源 / 逻辑源 / 恢复源
    ↔ manager / registry / provider / controller
    ↔ factory / clone / deserialize / transfer
    ↔ task / Future / callback / event
    ↔ network / save reconstruction
    ↔ renderer / tracking / HUD / Presence
    ↔ defense code / helper / parent / listener
    ↔ generated / hidden / proxy / lambda
    ↔ runtime patch / transformer
    ↔ MethodHandle / CallSite / dispatch
    ↔ native callback / bind
    ↓
final write / completion / admission / publish
```

Extreme 的终点不是“把当前实体删掉”，而是：

> 当前 Judgment 相关的有效结果不再能被未经授权地建立、恢复、重建或发布。


<!-- RONOVA-ORIGINAL-END orig-v26-l1300 -->

<a id="orig-v26-l1327"></a>

**迁入原文｜V26｜旧 L1327–L1361** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1327)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 9. 攻击平面 → 9.2 Candidate-first 执行

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1327 -->
#### 9.2 Candidate-first 执行

```text
发现可能影响当前 Judgment 的证据
→ 校验结果范围、授权和证据有效性
→ 满足 Candidate Admission Rule
→ CandidateResultEdge
→ 在平台可控处执行已授权的临时结果限制
→ 有界追踪归属
```

结果：

```text
PROVEN_RELATED
→ 纳入 ResultGraph
→ 按当前 Root policy 授权攻击/失能/压制

PROVEN_UNRELATED
→ 释放本候选原因建立的临时限制

UNRESOLVED
→ obligation 由细节或范围汇总继续承载
→ 已建立的结果出口限制继续有效
→ 后续预算窗口继续证明
```

如果平台只能观察、连结果出口也无法控制：

```text
不得宣称攻击成功
→ 对应 TARGET/OPERATION scope FENCED
→ 尝试替代结果路径 / 修复 / 后续重验
```


<!-- RONOVA-ORIGINAL-END orig-v26-l1327 -->

<a id="orig-v26-l1362"></a>

**迁入原文｜V26｜旧 L1362–L1396** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1362)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 9. 攻击平面 → 9.2 Candidate-first 执行 → 9.2.1 Candidate Admission Rule

**限制分层调整：** 无参数或其他项目自定禁令不能自动升格为本次铁律；原句保留，适用性见 [当前限制分层](README.md#scope-classification)。

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1362 -->
#### 9.2.1 Candidate Admission Rule

准入条件冻结为：

```text
结果范围能够绑定当前 Judgment 或其受保护操作
AND 证据属于当前有效观察上下文
AND 不是已证明授权有效且结果合规的正常操作
AND (
    至少一条有效强证据
    OR 至少两条独立弱证据
)
→ CANDIDATE
```

“当前上下文”至少绑定 realm、Judgment revision、policy/rule revision、受保护结果范围及当前 TargetEpoch。lineage 尚未查明时，允许先绑定已知目标操作或结果出口，不要求先证明具体来源身份。旧 epoch 提交仍可作为新观察事件：证据绑定当前上下文，另行记录提交者携带的旧 epoch，不能混为过期证据丢弃。

| 强证据 | 成立条件 |
|---|---|
| S1 直接触达受保护 WriteSink | 已观测到无有效授权、授权无法验证或与当前 Judgment 冲突的写入/完成尝试；合法合规写入不因触达就被隔离 |
| S2 直接产生 TERMINAL → PRESENT | 可信状态观察确认当前被终结目标重新有效存在；来源未明时先对受影响结果范围建候选，不把任意邻近 caller 判成原因 |
| S3 旧 TargetEpoch 仍尝试提交 | 当前 gate/trace 观测到失效资格用于写入、准入、恢复或发布 |
| S4 直接重建当前 lineage | 重建参数、身份血缘或准入证据指向当前被禁止恢复的目标 |
| S5 直接替换关键 dispatch/completion | 实际绑定、完成或发布被替换，且当前授权无效、无法验证或结果冲突 |

| 弱证据族 | 必须关联的观察 |
|---|---|
| W1 时间关联 | 在有界观察窗口内紧邻同一受保护结果的漂移 |
| W2 状态依赖 | 持续读写目标关键状态，但尚未证明直接导致冲突结果 |
| W3 任务/回调关系 | 与已确认 recovery source 存在可观察的 task/callback 关系，而非仅使用同一个线程池 |
| W4 恢复链重复关系 | 在多个可区分的恢复事件中重复出现同一结果关系；重复日志不算新事件 |
| W5 来源关联 | 可验证的 producer、loader、dispatch 或调用来源与已确认攻击链关联；包名、Mod ID 或自报名称不足以单独成立 |

强弱分类表示候选证据强度，不等同因果关系已被完整证明，也不直接授予永久处置权。可信观察必须来自登记并受完整性检查的观测入口；对方自报“我是恢复源”、伪造类名或提供任意日志，不能自行满足条件。


<!-- RONOVA-ORIGINAL-END orig-v26-l1362 -->

<a id="orig-v26-l1397"></a>

**迁入原文｜V26｜旧 L1397–L1412** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1397)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 9. 攻击平面 → 9.2 Candidate-first 执行 → 9.2.2 独立性、有效期与合法操作

**规范关系调整：** 本段的旧位次/冻结不可覆盖规则仅保留作来源记录，不再阻止显式修订；技术不变量本轮未据此删除。参见 [当前文档效力](README.md#scope-governance)。

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1397 -->
#### 9.2.2 独立性、有效期与合法操作

两条独立弱证据必须同时满足：

1. 属于不同证据族，关联同一当前 Judgment 的结果范围；
2. 有可区分、可检查的原始观察依据；一种证据不能只是另一种证据的复制或字段派生；
3. 同一 event 被两个 listener 记录、同一日志转发两次、同一观察拆成多个字段，只记一次；
4. 未跨用已失效的 policy、TargetEpoch、观察窗口或来源证明；
5. 独立性尚未建立时按一条证据处理，不能凑数准入。

观察窗口、原始事件标识、证据族与去重规则由版本化 policy 决定，并进入回执。窗口到期只表示需要补充新证据，不把已经建立的 obligation 自动变成 PROVEN_UNRELATED。

单条弱证据保留在有界观察层，不自动授权临时隔离，更不能升级高副作用处置；未达门槛不意味着从逻辑攻击域永久排除。若观察暴露真实受保护结果缺口或观测丢失，按 [§14.6](MS-SKILLS-PLAN.md#orig-v26-l2420) 保留该范围的覆盖缺口，禁止利用样本过期来声明闭合。

已证明授权有效且效果符合当前 Judgment 的修复、状态同步和合法重新激活，按原合同执行，不因使用共享 manager 或 protected sink 自动成为受攻击候选。授权无效的写入仍由原 WriteSink/Admission gate 直接处理，不必等待候选规则凑齐证据；Candidate 规则不能反过来降低已有 gate 的拒绝条件。


<!-- RONOVA-ORIGINAL-END orig-v26-l1397 -->

<a id="orig-v26-l1413"></a>

**迁入原文｜V26｜旧 L1413–L1424** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1413)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 9. 攻击平面 → 9.2 Candidate-first 执行 → 9.2.3 候选处置权限表

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1413 -->
#### 9.2.3 候选处置权限表

| 状态 | 可授权动作 | 禁止自动获得的权力 |
|---|---|---|
| 单条弱证据/观察中 | 有界记录、去重、关联及追加观察 | 仅凭弱相关性封禁共享组件、改写类或裁决整个 Mod 敌对 |
| CANDIDATE | 在真实可控入口执行目标/操作范围内临时、可撤销的结果限制，继续追踪 | 永久改写、类级失能、高副作用共享组件改写、创建 Judgment |
| UNRESOLVED | 维持上述临时限制，合并义务、按预算继续；必要时最小 scope fence | 因时间长、次数多或分数高而自动转成已证明敌对 |
| PROVEN_RELATED | 由当前 Root policy 根据因果证据、影响范围、能力与恢复基线授权攻击 | 绕过 lease、宿主边界、单 writer，或把同类无关业务自动判为敌对 |
| PROVEN_UNRELATED | 撤销仅由该候选建立且不再需要的限制 | 撤销其他候选的有效限制、已提交 Tombstone 或旧 epoch 撤销事实 |

临时限制必须记录建立原因、affectedScope、policy revision、可撤销动作和重验条件。失去追踪预算时允许汇总承接，不能使限制静默消失；证据表明无关后也不能永久保留无依据的候选限制。释放须检查同一范围是否仍有其他有效限制原因。


<!-- RONOVA-ORIGINAL-END orig-v26-l1413 -->

<a id="orig-v26-l1425"></a>

**迁入原文｜V26｜旧 L1425–L1441** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1425)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 9. 攻击平面 → 9.2 Candidate-first 执行 → 9.2.4 准入与处置回执

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1425 -->
#### 9.2.4 准入与处置回执

现有候选/证据回执至少扩展：

```text
scopeRef / judgmentRevision / targetEpoch
policyRevision / admissionRuleRevision
observedEventRefs[] / observationWindow
strongEvidenceKinds[] / independentWeakFamilies[]
authorizationCheck / deduplicationBasis
candidateDecision / decisionReason
allowedTemporaryActions[] / actualAppliedActions[]
restrictionReasons[] / obligationRef
```

各数组和字段载荷有界，重复证据引用规范事件或义务，不复制完整堆栈。实际动作未执行成功时不能写成已限制或已 DENIED；仍需按真实结果记录 PARTIAL/FENCED。准入回执只说明证据与政策判断，不成为 Judgment、Tombstone 或资格代际的新写入者。


<!-- RONOVA-ORIGINAL-END orig-v26-l1425 -->

<a id="orig-v26-l1442"></a>

**迁入原文｜V26｜旧 L1442–L1487** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1442)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 9. 攻击平面 → 9.2 Candidate-first 执行 → 9.2.5 Candidate 临时限制释放防抖

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1442 -->
#### 9.2.5 Candidate 临时限制释放防抖

V2.4 将候选证据结论与限制执行状态分离。`PROVEN_UNRELATED` 一旦由有效证据成立就立即成为当前 candidateDecision，不得为了防抖伪装成仍未证明；防抖只作用于该候选原因建立的临时、可撤销限制。

```text
restrictionReleaseState:
HELD
RELEASE_PENDING
RELEASED
```

自动释放流程：

```text
candidateDecision = PROVEN_UNRELATED
→ 枚举同一 affectedScope 的全部 restrictionReasons
→ 仅本候选原因已消失且不存在其他有效原因
→ restrictionReleaseState = RELEASE_PENDING
→ 进入有界观察 / 重验证窗口
→ 确认没有新的有效漂移或重新关联证据
→ RELEASED
```

释放窗口必须满足：

- 由版本化 policy 给出固定上限、重验证条件和最晚释放时刻；
- 相同日志、重复事件、过期证据、自报严重度或单纯 sourceIdentity 轮换不能刷新整个窗口；
- 新出现的一条有效强证据或满足独立性规则的弱证据组合，可以取消本次 pending release，但必须生成新的 decision/restriction revision；
- 其他 Judgment、Tombstone、旧 TargetEpoch 撤销事实或其他候选原因建立的限制继续独立有效；
- 预算压力、观测暂缺或对方持续制造噪声不能让无依据限制无限保留；达到硬截止时，除非已经建立新的有效限制原因，否则必须释放本候选原因建立的限制；
- 释放交接必须在实际 gate 上连续完成，不得出现旧限制先撤销、新状态后发布的无保护窗口。

回执至少增加：

```text
restrictionReleaseState / releaseRevision
releaseRequestedAt / releaseNotBefore / releaseDeadline
releaseRevalidationRefs[]
remainingRestrictionReasons[]
releaseCancelledReason / releasedActions[]
```

操作者通过 canonical Root command 明确执行的 `clear_uuid`、`clear_tombstones` 或后续等价“解除裁决”不属于 Candidate 自动释放，不受本防抖窗口偷偷延迟。它们必须按自身 Authority 合同立即提交，并在界面明确提示：删除 Ronova 的 Tombstone/Admission 限制后，拥有外部 SavedData、manager、factory 或任务重建源的目标可能重新出现。

本状态机不成为新 Truth writer，不改变 PROVEN_UNRELATED 的证据结论，也不把“释放等待中”显示成攻击成功。


<!-- RONOVA-ORIGINAL-END orig-v26-l1442 -->

<a id="orig-tier-l0562"></a>

**迁入原文｜TIER｜旧 L562–L585** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-tier-l0562)

原标题路径：第二部：加强路线（第一部 P0–P4 完成之后） → 9. Q0 —— 候选一等公民

<!-- RONOVA-ORIGINAL-BEGIN orig-tier-l0562 -->
#### 9. Q0 —— 候选一等公民

**入口**：第一部 P0 完成（矩阵已声明"候选全部"这一档，需要实体承载）。

**依据**：[V2.6:1170](MS-SKILLS-PLAN.md#orig-v26-l1154)——`MutationTrace`"**可提高 Candidate → PROVEN_RELATED 的置信度**"。
⇒ **Candidate 先于图存在**，不是图的产物。

**已有零件**

| 零件 | 现状 |
|---|---|
| `obligation/CandidateEvidence` | "当前上下文观察，8 个 family（3 strong）" |
| `obligation/CandidateRestrictions` | "Root 派生**可逆**准入 hold reason（不是裁决）+ reconcile + requestRelease" |
| `obligation/ObligationRegistry` | 证据合并需 **strong family 或两个独立弱 family** 且 eventRef/basisRef 都不同；含 **revalidation barrier** 防 A→B→A 复用延迟观察 |

**交付物**：统一的候选登记实体与其置信度模型
（`CANDIDATE → PROVEN_RELATED` 的提升规则、独立性与去重规则），
并把现有零散候选来源接入该模型。

**验收**：候选可被登记、提升、限制、释放；**提升必须有独立证据**（沿用 `ObligationRegistry`
既有的"strong family 或两个独立弱 family"规则，不得放宽）。

---


<!-- RONOVA-ORIGINAL-END orig-tier-l0562 -->

<a id="orig-rec-l0064"></a>

**迁入原文｜REC｜旧 L64–L90** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0064)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 2. 通用识别依据

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0064 -->
#### 2. 通用识别依据

从已经存在的 Raw Observation 和 Minecraft/Forge 通用语义入口观察实际行为：
NBT/快照复制与反序列化、工厂构造、注册/重插、调度与回调、保存与重新加载。
观察本身不调用第三方死亡、恢复或存档回调来“试试看”。

目标关系至少需要连成一条可核验的路径：

```text
当前 Root SubjectKey / TargetEpoch
    ← 实际重建参数或新身体的已证明来源
    ← 实际被消费的快照、记录或任务
    ← 对应 holder / container / scheduler / persistence owner
```

发现与永久处置分开：

- 一条有效强证据或两条独立弱证据只按 [V2.6 §9.2](MS-MSITEM-PLAN.md#orig-v26-l1327) 建立 Candidate。
- 来源正式进入 PROVEN_RELATED，必须证明实际数据流、调用/调度因果或注册关系。
- UUID、类名、继承关系、时间接近、同一线程池、同一 manager 或重复日志，
  都不能单独授权清理整个来源。
- 新 UUID、新对象、包装对象、不同 loader 的同名类均重新解析；
  保留 SubjectKey 与物理 incarnation 的区分，不复制旧身体的执行资格。
- provider 只交付不可变证据和访问位置描述；不交付可执行写闭包、Root handle 或自授权标记。
- 不扫描整个 JVM heap；有界追踪从实际入口取得的对象关系，已知容器用游标继续，
  未知结构保留缺口，不因无法读取而猜测其内容。


<!-- RONOVA-ORIGINAL-END orig-rec-l0064 -->

<a id="orig-rec-l0091"></a>

**迁入原文｜REC｜旧 L91–L114** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0091)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 2. 通用识别依据 → 2.1 记录到重建输入的证明

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0091 -->
#### 2.1 记录到重建输入的证明

SavedData/缓存记录路径的首个实现目标是：

```text
已观察的存储 owner
→ 某个容器中精确的目标条目
→ 条目实际持有的快照
→ 本次复制/派生出的 NBT
→ EntityType 反序列化或其他登记的重建入口
```

复制关系以本次真实对象关系和入口事件建立，不能仅比较两份 NBT 的 UUID 相同。
若一个记录包含多个目标，必须能定位当前目标的子记录；不能删除整个复合记录。
一次普通读取不构成恢复源证明，实际进入重建入口并与当前终结事实冲突才建立相应因果证据。

当前首个容器观察器限定完整实体快照：原始快照自身的 UUID 必须对应当前目标，
从而排除同条目中的其他实体快照；之后仍需实际复制与工厂入口证明。
缺少主体标识的局部快照仍需专用关系解析，不能借容器 key 自动取得来源资格。

通用容器适配只处理已注册、可验证语义的容器和访问方式。
自定义 `Map.get/remove`、iterator、getter、save 方法可能带任意回调，
不能把它们当作无副作用的原始内存访问。


<!-- RONOVA-ORIGINAL-END orig-rec-l0091 -->

<a id="orig-rec-l0389"></a>

**迁入原文｜REC｜旧 L389–L406** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0389)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 12. 参考 Mod 研究转入第一阶段 → 12.1 来源身份、缓存和再发布

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0389 -->
#### 12.1 来源身份、缓存和再发布

对应 M01–M03。发现结果必须区分当前 holder、当前条目、代理/原值缓存、复制后继及最终发布位置。
一个字段变为 null，只能作为该位置的观察事实；不能删除仍在使用的旧副本、任务关系或原值缓存义务。
同一浅复制图的共享元素单独记录关系和引用归属，不能因为外层 Map 已删除就清偿其他路径。

- runtime hash 只分桶，实际引用身份必须再次比较；稳定登记身份不能由 identity hash、
  UUID、字段名或类名拼接替代。SubjectKey 不变，物理 incarnation、定义 loader、世界和来源版本分别复验。
- 已经从旧 holder 派生的任务/快照在 holder 脱离后继续归于原因果链；
  同一个对象在新位置重新被观察时建立新的来源绑定，旧许可不能授权新绑定。
- 扩展真实消费与发布观察到无 NBT 的旧实体再使用、构造结果、索引副本和注册替换；
  不调用未知 getter、equals/hashCode、iterator 或 save 来探测副作用，也不以放行一次重建换取证据。
- 弱引用回收、访问失败、元数据过期和未观察行为分别保留状态。
  引用消失不是任务退出或持久存储已更新；无法证明边已终结时保留对应缺口。

首个成套正例应同时包含原对象再发布、原值缓存、浅复制后继以及同容器无关对象；
最终关系由生产 observer 取得。反例覆盖实际 hash 碰撞、同名不同 loader、空槽与诱饵 UUID。


<!-- RONOVA-ORIGINAL-END orig-rec-l0389 -->

<a id="orig-cf-l0572"></a>

**迁入原文｜CF｜旧 L572–L612** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0572)

原标题路径：8. ResolverProvider SPI Contract

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0572 -->
#### 8. ResolverProvider SPI Contract

ResolverProvider 只负责 discovery / evidence，不持有 Authority。

```text
ResolverProvider
├ providerId
├ providerRevision
├ lifecycleState
└ resolve(immutable input)
    → CandidateNode
    → RelationshipEvidence
    → Provenance
    → Confidence
```

provider lifecycle 冻结为：

```text
REGISTERED → ACTIVE ↔ SUSPENDED
ACTIVE | SUSPENDED → REVOKED
REGISTERED | ACTIVE | SUSPENDED → FAILED
```

- 只有 `ACTIVE` provider 的输出可以进入 validation；
- provider 输出必须是不可变 data/evidence，不得携带可执行 write closure、`WritePlan` 或隐式 Root handle；
- provider disappearance、revision change、`SUSPENDED`、`REVOKED` 或 `FAILED` 会把该 provider 未重验证的 evidence 标为 `STALE`；
- 已 durable commit 的 binding 不被 provider 消失直接删除，但在下一次 authority-sensitive use 前必须按 Policy 重验证或 fence；
- hostile/throwing/timeout provider 必须按 providerId 隔离、记 Incident，并以 `FAILED` 退出本轮；不得继承调用方 Authority，也不得拖垮其他 provider。

明确禁止：

```text
Resolver → Root mutation
Resolver → Judgment authorization
Resolver → Tombstone creation
Resolver → Mode escalation
```

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0572 -->

<a id="orig-cf-l0613"></a>

**迁入原文｜CF｜旧 L613–L640** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0613)

原标题路径：8. ResolverProvider SPI Contract → 8.1 Relationship Edge Lifecycle

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0613 -->
#### 8.1 Relationship Edge Lifecycle

```text
DISCOVERED
→ EVIDENCE_ACCUMULATED
→ VALIDATED
→ BOUND
```

任何边都必须带：

```text
edgeId
source
target
relationType
provenance
confidence
validationState
bindingRevision
providerId
providerRevision
```

`DISCOVERED` / `EVIDENCE_ACCUMULATED` 不得直接参与强 mutation。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0613 -->

<a id="orig-p0a-l1096"></a>

**迁入原文｜P0A｜旧 L1096–L1097** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1096)

原标题路径：26. ResolverProvider SPI

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1096 -->
#### 26. ResolverProvider SPI


<!-- RONOVA-ORIGINAL-END orig-p0a-l1096 -->

<a id="orig-p0a-l1098"></a>

**迁入原文｜P0A｜旧 L1098–L1115** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1098)

原标题路径：26. ResolverProvider SPI → 26.1 接口

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1098 -->
#### 26.1 接口

```java
public interface ResolverProvider {
    ResolverProviderId providerId();
    ResolverProviderRevision revision();
    ResolverProviderState state();
    ResolverResult inspect(
            SubjectKey subjectKey,
            ResolverInput input
    );
}
```

provider lifecycle 为 `REGISTERED → ACTIVE ↔ SUSPENDED → REVOKED/FAILED`。只有 ACTIVE 输出可进入 validation；revoked/changed/disappeared provider 的未重验证 evidence 变 STALE。throw/timeout provider 按 providerId 隔离并记 Incident，不得拖垮 pipeline 或继承 Authority。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l1098 -->

<a id="orig-p0a-l1116"></a>

**迁入原文｜P0A｜旧 L1116–L1138** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1116)

原标题路径：26. ResolverProvider SPI → 26.2 ResolverResult

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1116 -->
#### 26.2 ResolverResult

```text
candidates[]
evidence[]
provenance
confidence
providerId
providerRevision
```

结果必须是 immutable data，不得携带 `WritePlan`、可执行 closure 或 Root handle。

Resolver 绝对不能：

- submit `DominionCommand` / call `DominionRootGateway`；
- terminalize；
- set mode；
- mutate target；
- 自己判定 BOUND。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l1116 -->

<a id="orig-road-l0267"></a>

**迁入原文｜ROAD｜旧 L267–L283** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-road-l0267)

原标题路径：Ronova L6／L6E 全量加强路线 V1.0 — Internal Root → 7. 候选、持续待办与跨表面结果

<!-- RONOVA-ORIGINAL-BEGIN orig-road-l0267 -->
#### 7. 候选、持续待办与跨表面结果

| 加强项 | L6 编排 | L6E 深化与完成条件 |
|---|---|---|
| 候选准入与独立证据 | Root 根据当前上下文与冻结规则批准适用处置 | B/C/D 分别接合任务、重建和动态来源；证据去重、有效期、独立性与合法授权都参与判断，分数不直接授予破坏性权限 |
| 持久候选与原因保留 | 保存规范候选历史、适用主体与原因，保持当前临时许可独立 | 重启或版本变化后重新证明实际对象；历史候选不自动变成当前写入许可，也不因缓存回收丢失原因 |
| 临时限制与原因接力 | Root 批准精确限制和新旧原因的交接 | 原因变化、汇总升级或恢复时遵循原限制接力合同，不能在交接空隙放行旧资格 |
| 义务归并与多层溢出 | 使用现有 canonical key 归并重复工作，保留明细与范围汇总 | 同键合并不改变含义，不同来源/目标洪泛仍保留范围待办；不能用扩大 Judgment 语义代替容量管理 |
| 反诱导与公平调度 | Root 资格决定可做什么，调度器决定何时取得预算 | 来源归一、重复贡献封顶、探索预算及主体/来源/明细轮转；持续压力下低频目标仍能推进 |
| 自动解除与防抖 | 按当前资格执行 RELEASE_PENDING、硬截止和回执合同 | 跨任务、动态来源、重启及汇总限制使用同一规则；显式操作者命令不被自动防抖拖延，旧 epoch 不被重新放行 |
| 跨表面 OperationPlan | Root 是唯一批准者，统一原因、步骤、依赖、lease 和结果 | 独立步骤可分别推进；处理任务/网络/显示/资源间冲突和恢复顺序，避免循环修复 |
| 执行、结果与完成 | 执行状态、局部效果、当前结果和历史证书分别投影 | APPLIED 是否最终完成由 CompletionContract 决定；required 表面、未决来源和有效稳定窗口共同决定当前完成 |
| 重启与合法重新激活 | 保存原命令、效果、义务和资格事务，恢复当前投影 | B/C/D 重新解析任务、记录、代码与资源；旧实例证据退出，未知外部效果仍使用原身份解析 |

容量不足、观察不可用或部分修复失败时，记录缺口所在的主体、来源、操作与版本。
已登记限制和已知终态不能通过丢弃缓存自动解除；最后一份可信结果与当前未知状态需要分别保留。


<!-- RONOVA-ORIGINAL-END orig-road-l0267 -->

<a id="ms-msitem-l201"></a>

## 统一目标、结果和防御策略

**框架来源：** [MS msitem，原 L201–L223](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#统一目标结果和防御策略)。

**编排说明（非原条款）：** 承接预设、逐键矩阵、目标范围、Owner Protection、结果许可与明确解除。MS 中保守排除玩家或未知 Boss 的示例不成为 Ronova 的新全局禁令。

<a id="orig-bl-l0518"></a>

**迁入原文｜BL｜旧 L518–L519** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0518)

原标题路径：5. Authority Policy 加强

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0518 -->
#### 5. Authority Policy 加强


<!-- RONOVA-ORIGINAL-END orig-bl-l0518 -->

<a id="orig-bl-l0520"></a>

**迁入原文｜BL｜旧 L520–L550** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0520)

原标题路径：5. Authority Policy 加强 → 5.1 目标

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0520 -->
#### 5.1 目标

从简单：

```text
protected / not protected
```

提升为细粒度 operation authority：

```text
Subject Attributes
      ↓
Deterministic Policy
      ↓
Allowed Operations
```

可能的 operation：

- VITALS；
- TERMINAL；
- Admission；
- Movement；
- Equipment；
- Effect；
- owner-protection mutation；
- critical administrative mutation。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l0520 -->

<a id="orig-bl-l0551"></a>

**迁入原文｜BL｜旧 L551–L570** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0551)

原标题路径：5. Authority Policy 加强 → 5.2 原则

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0551 -->
#### 5.2 原则

Policy 必须：

- deterministic；
- versioned；
- auditable；
- rollback-able；
- 不依赖机器学习直接生成 Judgment。

AI / anomaly model 最多输出：

```text
Recommendation / Risk Signal
```

最终 Authority 仍由 Root policy 决定。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l0551 -->

<a id="orig-bl-l0571"></a>

**迁入原文｜BL｜旧 L571–L605** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0571)

原标题路径：5. Authority Policy 加强 → 5.3 Root 驱动的确定性 Threat Posture / Policy Overlay

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0571 -->
#### 5.3 Root 驱动的确定性 Threat Posture / Policy Overlay

保留此前“全局动态策略下发”的合理部分，但不引入非确定性自治 Authority。

模型：

```text
Integrity / Storm / Incident Signals
             ↓
Incident Correlation
             ↓
Deterministic Root Policy
             ↓
PolicyOverlay(revision)
             ↓
Immutable Projection
```

`PolicyOverlay` 可以调整：

- enforcement budget；
- Integrity challenge cadence；
- unresolved target 的 Admission 处理模式；
- rate limit / storm threshold；
- 非关键 Domain 的执行频率；
- 诊断与 evidence capture 强度。

它**不得绕过正常 Authority mutation 路径**直接把：

```text
VITALS failure → TERMINAL
```

只有 Root 中预先定义、确定、可审计的 policy rule 明确授权时，才能通过正常 Judgment / mutation command 产生新事实。


<!-- RONOVA-ORIGINAL-END orig-bl-l0571 -->

<a id="orig-bl-l0606"></a>

**迁入原文｜BL｜旧 L606–L633** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0606)

原标题路径：5. Authority Policy 加强 → 5.4 Minecraft Tag 只作为高效 selector，不作为 Authority 证据

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0606 -->
#### 5.4 Minecraft Tag 只作为高效 selector，不作为 Authority 证据

允许利用原版 / datapack tag 做：

```text
TargetSelectorIndex
Tag membership
→ 快速候选过滤 / policy matching
```

适合：

- 大范围攻击候选过滤；
- policy scope；
- workload reduction。

但 Tag：

```text
❌ 不是 Identity proof
❌ 不是 State Ownership proof
❌ 不能单独证明目标具有某项 Authority
```

Tag membership 只属于**选择与分类输入**。最终 Judgment、Ownership、Admission 仍由 Root Authority 语义决定。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l0606 -->

<a id="orig-bl-l1257"></a>

**迁入原文｜BL｜旧 L1257–L1285** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l1257)

原标题路径：11. Constraint Fusion

**规范关系调整：** 本段的旧位次/冻结不可覆盖规则仅保留作来源记录，不再阻止显式修订；技术不变量本轮未据此删除。参见 [当前文档效力](README.md#scope-governance)。

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l1257 -->
#### 11. Constraint Fusion

多个 Domain / Intent 不能互相反复覆盖。

```text
All Constraints
      ↓
ConstraintFusionEngine
      ↓
normalize
      ↓
priority / hard-soft
      ↓
conflict detection
      ↓
FusedConstraintSet
```

用途：

- 防止 Domain 互打；
- 防止无限修复循环；
- 防止 conflict storm；
- 让执行顺序显式化。

TERMINAL 的优先语义不应由普通 solver 临时猜测，而由 Authority 层明确冻结。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l1257 -->

<a id="orig-v26-l0295"></a>

**迁入原文｜V26｜旧 L295–L324** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0295)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 3. 正式术语 → 3.5 证明决定处置级别，不决定范围

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0295 -->
#### 3.5 证明决定处置级别，不决定范围

禁止：

```text
无法立刻证明
→ 当成无关
→ 从范围里删掉
```

V2.2 改为：

```text
存在满足候选准入规则的合理证据
→ 先纳入候选攻击面
→ 限制其对当前 Judgment 的结果资格
→ 再证明

证明相关
→ 升级正式攻击/失能/压制

证明无关
→ 释放

长期未决
→ 保留 obligation
→ 有界继续追踪
→ 必要时 fence 对应结果范围
```


<!-- RONOVA-ORIGINAL-END orig-v26-l0295 -->

<a id="orig-v26-l0363"></a>

**迁入原文｜V26｜旧 L363–L364** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0363)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 4. 目标、非目标与宿主安全边界 → 4.1 三档目标范围

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0363 -->
#### 4.1 三档目标范围


<!-- RONOVA-ORIGINAL-END orig-v26-l0363 -->

<a id="orig-v26-l0365"></a>

**迁入原文｜V26｜旧 L365–L374** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0365)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 4. 目标、非目标与宿主安全边界 → 4.1 三档目标范围 → COMPATIBLE / 兼容

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0365 -->
#### COMPATIBLE / 兼容

```text
Ronova 自身
+
r51 已有稳定实体/生命周期/准入路径
```

目标：普通整合包可用，优先避免干扰无关第三方功能。


<!-- RONOVA-ORIGINAL-END orig-v26-l0365 -->

<a id="orig-v26-l0375"></a>

**迁入原文｜V26｜旧 L375–L388** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0375)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 4. 目标、非目标与宿主安全边界 → 4.1 三档目标范围 → ENHANCED / 强化

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0375 -->
#### ENHANCED / 强化

```text
Ronova
+
当前目标
+
当前 Judgment
+
已经明确识别的直接结果相关路径
```

目标：强度优先，但仍尽量做目标级、边级、实例级精确处置。


<!-- RONOVA-ORIGINAL-END orig-v26-l0375 -->

<a id="orig-v26-l0389"></a>

**迁入原文｜V26｜旧 L389–L401** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0389)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 4. 目标、非目标与宿主安全边界 → 4.1 三档目标范围 → EXTREME / 极限代码对抗

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0389 -->
#### EXTREME / 极限代码对抗

```text
当前 Judgment
→ 所有可能改变其最终结果的同 JVM 候选路径
→ 已证明源 / 链 / 代码 / dispatch / task / callback / rebuild / publish
→ 最终完成与状态出口
```

目标：

> **胜负结果优先，第三方兼容性不是保护目标。**


<!-- RONOVA-ORIGINAL-END orig-v26-l0389 -->

<a id="orig-v26-l0584"></a>

**迁入原文｜V26｜旧 L584–L585** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0584)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 6. 强度模型

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0584 -->
#### 6. 强度模型


<!-- RONOVA-ORIGINAL-END orig-v26-l0584 -->

<a id="orig-v26-l0586"></a>

**迁入原文｜V26｜旧 L586–L605** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0586)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 6. 强度模型 → 6.1 顶层预设

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0586 -->
#### 6.1 顶层预设

```text
强度预设：

○ 兼容
○ 强化
● 极限代码对抗
○ 自定义
```

内部枚举仍为：

```text
COMPATIBLE
ENHANCED
EXTREME
CUSTOM
```


<!-- RONOVA-ORIGINAL-END orig-v26-l0586 -->

<a id="orig-v26-l0606"></a>

**迁入原文｜V26｜旧 L606–L613** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0606)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 6. 强度模型 → 6.2 三档正式范围

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0606 -->
#### 6.2 三档正式范围

| 档位 | 默认范围 | 兼容性政策 | 失败政策 |
|---|---|---|---|
| COMPATIBLE | Ronova 核心 + 稳定实体路径 | 优先兼容 | 非关键路径记录/降级，Authority 不失真 |
| ENHANCED | Ronova + 当前目标 + 已识别直接结果链 | 强度优先、尽量精确 | 对相关对象/类/调用隔离、修复或对象级 fence |
| EXTREME | 当前 Judgment 的全部候选/已证明同 JVM 结果攻击面 | 不保护敌对 Mod 兼容性 | 先限制未知结果资格；无法闭合时按最小必要 scope fence |


<!-- RONOVA-ORIGINAL-END orig-v26-l0606 -->

<a id="orig-v26-l0614"></a>

**迁入原文｜V26｜旧 L614–L644** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0614)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 6. 强度模型 → 6.3 子项档位

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0614 -->
#### 6.3 子项档位

攻击与防护：

```text
跟随预设
关闭
兼容
强化
极限
```

拦截机制：

```text
跟随预设
关闭
监控
保护若娜瓦
加强相关路径
全部严格
```

映射：

```text
兼容 → 保护若娜瓦
强化 → 加强相关路径
极限 → 全部严格
```


<!-- RONOVA-ORIGINAL-END orig-v26-l0614 -->

<a id="orig-v26-l0645"></a>

**迁入原文｜V26｜旧 L645–L674** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0645)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 6. 强度模型 → 6.4 配置值与有效值

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0645 -->
#### 6.4 配置值与有效值

继续区分：

```text
configuredLevel
supportedCeiling
effectiveLevel
evidenceState
activationState
```

禁止：

```text
GUI 显示 EXTREME
后台实际只跑 COMPATIBLE
```

若关键能力缺失：

```text
PENDING_RESTART
REJECTED
FENCED
UNAVAILABLE
```

必须真实展示。


<!-- RONOVA-ORIGINAL-END orig-v26-l0645 -->

<a id="orig-v26-l0761"></a>

**迁入原文｜V26｜旧 L761–L777** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0761)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 6. 强度模型 → 6.6 V2.5 逐能力 ProfilePresetMatrix

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0761 -->
#### 6.6 V2.5 逐能力 ProfilePresetMatrix

V2.5 冻结版本化解析合同：

```text
ProfilePresetMatrix(profile, optionKey, matrixRevision)
    → requestedLevel

requestedLevel
    ∩ supportedCeiling
    ∩ evidenceCeiling
    ∩ restart/activation state
    → effectiveLevel
```

`CUSTOM` 直接使用玩家逐项选择；三个正式预设必须按下表逐键解析。r52 实现的“Compatible 对运行时统一 MONITOR、其他平面统一 PROTECT_RONOVA；Enhanced 全部 RELATED_PATHS；Extreme 全部 STRICT_ALL”仅是 7.16 policy foundation，不是 V2.5 最终矩阵。矩阵进入生产代码时必须有独立 revision、迁移规则和 exact-artifact 测试。


<!-- RONOVA-ORIGINAL-END orig-v26-l0761 -->

<a id="orig-v26-l0800"></a>

**迁入原文｜V26｜旧 L800–L819** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0800)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 6. 强度模型 → 6.6 V2.5 逐能力 ProfilePresetMatrix → 6.6.2 十五项高级攻击政策

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0800 -->
#### 6.6.2 十五项高级攻击政策

| StrengthOptionKey | 玩家名称 | 兼容 | 强化 | 极限代码对抗 |
|---|---|---|---|---|
| `ATTACK_NON_ENTITY` | 非实体攻击 | 监控与当前目标直接关联的非实体结果 | 控制已证明相关的 HUD、registry object、projection 与逻辑对象 | 所有候选非实体结果先限制资格，证明后执行完整清除/失能/恢复闭环 |
| `ATTACK_LOGIC_SOURCE` | 逻辑源攻击 | 仅记录直接 manager/source | 隔离并处理已证明相关逻辑源 | 候选源进入 ResultGraph；临时限制与证明后高副作用处置分离 |
| `ATTACK_RECOVERY_SOURCE` | 恢复源攻击 | 监控同 lineage 的直接恢复 | 控制已识别复活、重建与自修复源 | 覆盖 task/network/save/dynamic/native 候选恢复源并持续重验 |
| `ATTACK_STATE_SOURCE` | 状态源攻击 | 收敛当前目标直接状态写入 | 处理相关 capability、map、field 与同步写回 | 控制候选状态源、WriteSink、回滚和最终结果出口 |
| `ATTACK_REGISTRATION_SOURCE` | 注册源攻击 | 基础 Admission 与同 lineage 注册检查 | 控制相关 registry/factory/listener 注册 | 覆盖候选注册、重注册、loader 与动态 producer，旧 epoch 永不重新合法 |
| `ATTACK_TASK_CHAIN` | 任务链攻击 | 观察直接 server task | 取消/隔离已证明相关队列与 scheduler 任务 | 约束候选线程、executor、timer、future 和重建任务并验证最终结果 |
| `ATTACK_CALLBACK_CHAIN` | 回调链攻击 | 观察直接 listener/callback | 控制已证明相关回调和订阅 | 约束候选 event/listener/lambda/native callback 与再注册链 |
| `ATTACK_NETWORK_RECONSTRUCTION` | 网络重建攻击 | 拒绝旧 epoch/stale 基础包 | 控制相关 channel、packet handler 与同步缓存 | 绑定身份、nonce、epoch、revision，覆盖候选重发、镜像和客户端重建 |
| `ATTACK_SAVE_RECONSTRUCTION` | 存档重建攻击 | Tombstone/Realm 基础加载门 | 控制相关 SavedData/NBT/缓存恢复源 | 跨重启验证 durable lineage、旧 epoch 和候选 save reconstruction 出口 |
| `ATTACK_DYNAMIC_CODE` | 动态代码攻击 | 监控 runtime compiler、proxy、lambda、hidden class | 处理已证明相关的生成类/动态补丁 | 候选动态 producer、loader、dispatch、result outlet 全链覆盖；无证明不做永久类级破坏 |
| `ATTACK_DISPATCH` | 分派攻击 | 验证 Ronova 关键 dispatch | 控制已证明相关 method handle、reflection、invoke target | 覆盖候选解释/JIT/native dispatch 与最终调用目标，失败则最小 scope fence |
| `ATTACK_RUNTIME_PATCH_DISABLEMENT` | 运行时补丁失能 | 保护 Ronova 自身关键方法不被补丁替换 | 失能已证明相关的 Mixin/ASM/Instrumentation 补丁 | 对候选先隔离结果，对已证明 patch producer/transform/dispatch 执行精确失能与重验 |
| `ATTACK_TARGET_CLASS_LINEAGE` | 目标类血缘压制 | 精确类与当前 lineage | 相关父子类、代理、包装与工厂产物 | 按 producer/result 识别所有候选 class lineage，不以混淆名作为安全边界 |
| `ATTACK_FINAL_RESULT_TAKEOVER` | 最终结果接管 | 验证当前实体最终状态 | 接管已证明相关状态、记录、tracking 与显示出口 | 对八表面当前 Judgment 最终结果统一完成仲裁；缺口即 PARTIAL/FENCED |
| `ATTACK_SOURCE_TRACE` | 攻击来源追踪 | 直接 protected sink provenance | 相关 task/callback/loader/network/save provenance | 候选跨表面 MutationTrace；证据去重、容量汇总和独立性规则全部启用 |


<!-- RONOVA-ORIGINAL-END orig-v26-l0800 -->

<a id="orig-v26-l1127"></a>

**迁入原文｜V26｜旧 L1127–L1153** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1127)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 8. 选项、政策键与阶段 → 8.1 V2.2 攻击政策（V2.3/V2.4/V2.5 沿用）

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1127 -->
#### 8.1 V2.2 攻击政策（V2.3/V2.4/V2.5 沿用）

V2.1 十四项继续保留：

```text
attack.non_entity
attack.logic_source
attack.recovery_source
attack.state_source
attack.registration_source
attack.task_chain
attack.callback_chain
attack.network_reconstruction
attack.save_reconstruction
attack.dynamic_code
attack.dispatch
attack.runtime_patch_disablement
attack.target_class_lineage
attack.final_result_takeover
```

V2.2 新增、V2.3/V2.4/V2.5 继续保留：

```text
attack.source_trace
```


<!-- RONOVA-ORIGINAL-END orig-v26-l1127 -->

<a id="orig-v26-l1488"></a>

**迁入原文｜V26｜旧 L1488–L1505** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1488)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 9. 攻击平面 → 9.3 Extreme 不因共享路径自动降级

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1488 -->
#### 9.3 Extreme 不因共享路径自动降级

如果某个 shared helper / parent / global manager：

- 已被证明承担当前目标的恢复、防御或结果发布；
- 且没有更低副作用的等价执行方式；

则 Extreme：

> **不因为它同时服务其他第三方功能而自动退回 Enhanced。**

但必须：

- 记录 impact scope；
- 保留恢复基线；
- 不越过宿主安全边界；
- 不把“同 manager”当成无证据的整个 Mod 敌对证明。


<!-- RONOVA-ORIGINAL-END orig-v26-l1488 -->

<a id="orig-full-l0072"></a>

**迁入原文｜FULL｜旧 L72–L91** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-full-l0072)

原标题路径：Ronova 7.17 极限档全体系加强规划 V1.0 → 2. 全部高级能力的极限目标 → 2.1 攻击：15 项

<!-- RONOVA-ORIGINAL-BEGIN orig-full-l0072 -->
#### 2.1 攻击：15 项

| 高级项 | 极限目标 |
|---|---|
| attack.non_entity | 处理与当前目标具有实际关系的非实体显示、任务、资源及其他已登记表面，分别确认其结果 |
| attack.logic_source | 追踪维持目标结果的真实状态读写和 producer，覆盖字段、容器、方法、句柄及回调关系 |
| attack.recovery_source | 同时覆盖 SavedData、静态管理器、快照、维护任务及后继恢复源；原身体消失后继续处理 |
| attack.state_source | 建立真实生命/状态模型，区分显示值、镜像、计算值和维护写源；精确写入并持续检查反弹 |
| attack.registration_source | 覆盖注册、重插和索引替换路径，按实际主体/实例处理，不能靠全类误杀代替关系解析 |
| attack.task_chain | 识别实际调度关系，处置目标任务及后继；已运行、已取消和结果仍可能提交分别处理 |
| attack.callback_chain | 追踪订阅、移除、重新订阅及迟到回调；终端提交点核对原资格，避免清理后回流 |
| attack.network_reconstruction | 服务端发布到客户端重建的每段关系都带当前版本；旧连接、旧图像及迟到消息不能复活旧状态 |
| attack.save_reconstruction | 处置实际持久记录并独立读取确认；重启后复验原来源，内存缺失或 setDirty 返回不代替保存结果 |
| attack.dynamic_code | 跟踪结果相关的动态定义、重定义、重转换及新 loader 副本，使失效代码证据及时退出并重新建立 |
| attack.dispatch | 为普通调用、回调、函数对象、句柄和已登记原生出口建立独立执行路径及当前证据 |
| attack.runtime_patch_disablement | 识别并处置实际阻止当前目标执行的补丁；保留其他功能，后续补丁重现时继续处理 |
| attack.target_class_lineage | 以实际 Class、loader、incarnation 和 Root 范围解析血缘；覆盖换 UUID/新实例而不复用旧身体的资格 |
| attack.final_result_takeover | 从状态、索引、恢复源、追踪与显示共同确认结果；持续反弹触发相应修复，不重放死亡/掉落 |
| attack.source_trace | 形成可续接的目标、来源、调用、任务和结果关系；容量不足进入明确范围待办，不静默忘记 |


<!-- RONOVA-ORIGINAL-END orig-full-l0072 -->

<a id="orig-tier-l0034"></a>

**迁入原文｜TIER｜旧 L34–L46** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-tier-l0034)

原标题路径：Ronova 7.17 极限档强度实施规划 V1.0 — Internal Root → 0. 本文定位 → 0.1 术语约定

<!-- RONOVA-ORIGINAL-BEGIN orig-tier-l0034 -->
#### 0.1 术语约定

| 术语 | 含义 | 载体 |
|---|---|---|
| **请求等级** `requested` | 配置请求的深度 | `ProfilePresetMatrix` |
| **支持天花板** `supportedCeiling` | 本构建**能证明自己做到**的深度 | `StrengthPolicySnapshot.supportedCeilings` |
| **证据天花板** `evidenceCeiling` | 当前**证据链有效**的深度，恒 ≤ 支持天花板 | `StrengthPolicySnapshot.evidenceCeilings` |
| **有效深度** `effective` | `min(requested, supportedCeiling, evidenceCeiling)`，**运行期真正使用的值** | `StrengthPolicySnapshot.java:230-233` |
| **逐键矩阵** | 54 键 × 3 预设各自的**语义声明**（非统一枚举值） | [V2.6 §6.6](MS-MSITEM-PLAN.md#orig-v26-l0761)，本文 G0 |
| **举证**（attestation） | 由**实际运行观察**产生的、可复核的能力证据 | 本文 P1 新增 |

---


<!-- RONOVA-ORIGINAL-END orig-tier-l0034 -->

<a id="orig-tier-l0257"></a>

**迁入原文｜TIER｜旧 L257–L288** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-tier-l0257)

原标题路径：Ronova 7.17 极限档强度实施规划 V1.0 — Internal Root → 3. 阶段划分 → P0 —— 矩阵 revision 2：把逐键语义实施出来（**合同缺口，最高优先**）

<!-- RONOVA-ORIGINAL-BEGIN orig-tier-l0257 -->
#### P0 —— 矩阵 revision 2：把逐键语义实施出来（**合同缺口，最高优先**）

**入口条件**：无。可立即开始。
**为什么最先做**：[V2.6:776](MS-MSITEM-PLAN.md#orig-v26-l0761) 要求"三个正式预设必须按下表逐键解析"，
而当前 `ENHANCED` / `EXTREME` / `CUSTOM` 是 `filled(...)` 统一映射（[§1.8](MS-SKILLS-PLAN.md#orig-tier-l0132)）。
在此缺口闭合前，任何执行侧工作都缺少"该按什么区分档位"的依据。

**交付物**

| 产物 | 内容 |
|---|---|
| `ProfilePresetMatrix` revision 2 | 54 键 × 3 预设的**逐键声明**，按 [V2.6 §6.6.2](MS-MSITEM-PLAN.md#orig-v26-l0800)–6.6.5 四张表逐行落实；14 动作按 [§6.6.1](MS-MSITEM-PLAN.md#orig-v26-l0778) |
| 逐键语义声明模型 | 每个 (键, 预设) 声明三件事：**纳入范围**（直接 / 已证明相关 / 候选全部）、**是否前置可逆限制**、**不可观测时的报告义务** |
| revision 与迁移 | 独立 revision 号；schema 升级先验证旧 policy/journal，再由 Root 提交完整新配置图并等待重启；旧 receipt 保留原 payload/hash 语义（照 [V2.6:927](MS-MSITEM-PLAN.md#orig-v26-l0884) 口径） |
| exact-artifact 测试 | 逐键生成可复现的 `requestedLevel` 与声明（对应 X51 口径） |
| 回滚路径 | revision 1 仍可解析；revision 2 可整体回退 |

**硬约束**

1. **逐键语义必须来自 [§6.6](MS-MSITEM-PLAN.md#orig-v26-l0761) 原文**，不得由实施者自拟；
2. `FOLLOW_PROFILE` 只引用所配置的矩阵 revision；改单项后转 Custom，
   其他项保持最近一次显式解析值，**禁止隐式漂移**（[V2.6:879](MS-MSITEM-PLAN.md#orig-v26-l0874)）；
3. 不得改动 r53 已有 schema 2 的字节或哈希（[V2.6:927](MS-MSITEM-PLAN.md#orig-v26-l0884)）；
4. 不得因"某一键难以实现"而把它在极限列的声明降为兼容列语义。

**验收**：矩阵逐键可复现；每键的极限档声明能在 [§6.6](MS-MSITEM-PLAN.md#orig-v26-l0761) 中找到对应原文；
revision 1 → 2 → 1 往返不丢语义。

**明确不做**：P0 不改变任何键的 `effective`，不提升天花板。

---


<!-- RONOVA-ORIGINAL-END orig-tier-l0257 -->

<a id="orig-tier-l0328"></a>

**迁入原文｜TIER｜旧 L328–L349** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-tier-l0328)

原标题路径：Ronova 7.17 极限档强度实施规划 V1.0 — Internal Root → 3. 阶段划分 → P2 —— 逐键举证与执行接线（收益最大） → P2-A 攻击平面（当前 0 / 15）

<!-- RONOVA-ORIGINAL-BEGIN orig-tier-l0328 -->
#### P2-A 攻击平面（当前 0 / 15）

| 键 | 已有执行器 | 建议的可观察证据 | 完整度 |
|---|---|---|---|
| `attack.registration_source` | `SpawnFirewall` 4 道边界 + `SpawnAdmissionRuntime` 计数 | 本运行时实际拒绝过 admission，且拒绝对象与 tombstone 匹配 | 高 |
| `attack.final_result_takeover` | `TerminalPlanExecutor` + `TerminalCompletionMonitor` | 完成判定实际产出过 `VERIFIED` 条件且经 Root 回执确认 | 高 |
| `attack.state_source` | `TerminalStateSink` + `TerminalStateFeedback` | 已登记字段写入后**实际观察到漂移并被重写** | 高 |
| `attack.network_reconstruction` | `ProjectionCoordinator` + 回执 + `DeliveryState` | 实际拒绝过陈旧/错 Realm 投影，且客户端回过 ACK | 高 |
| `attack.dynamic_code` | **Native CodeSeal + `ClassPrepare`** | 封印类实际加载并通过字节校验；有 `g_code_replacements` 计数 | 高（易被忽视） |
| `attack.runtime_patch_disablement` | **`SemanticGateVerifier` + `RawAccessorContract` 修复** | 实际检出被改成空返回/错字段的门并重写方法体 | 高（易被忽视） |
| `attack.recovery_source` | `RecoverySourceTracker` + `Fact` 体系 | 实际写入过 `MEMORY_CHANGED` / `READBACK_VERIFIED` / `PRESERVATION_VERIFIED` | 中 |
| `attack.save_reconstruction` | `SavedDataSaveTrace` / `EncodedWrite` / `NbtStoragePreservation` | 实际匹配过保存代次 + 摘要 + 文件身份 | 中 |
| `attack.task_chain` | `TaskExecutionGraph` + `RecoveryTaskDisposition` | 实际写入过 `EXECUTION_EXITED`（任务域 `RESOURCES_RELEASED` 无写入点） | 中 |
| `attack.logic_source` | `CopyProvenanceIndex` + `StaticSnapshotLocator` | 实际 `RESOLVED` 且 `evidenceVerified()` 的复制来源 | 中 |
| `attack.target_class_lineage` | `ClassLineageResolver` | 实际解析出 anchor 且资格当前 | 中 |
| `attack.callback_chain` | `TaskEventRun` + JDK 任务边界 | 实际观察到回调边界事件 | 低 |
| `attack.dispatch` | `OperationPlanRunner` 端口 | 独立执行路径实际被走到（非 fallback） | 低 |
| `attack.non_entity` | 追踪 / HUD 路径 | 尚无独立证据链 | 低 |
| `attack.source_trace` | `RecoveryChainPolicy.snapshot` / `policyCounters` | 目前只是策略计数，**不足以作为来源追踪证据** | 低 |

**建议**：先做前 6 项；后 4 项需先补执行器。


<!-- RONOVA-ORIGINAL-END orig-tier-l0328 -->

<a id="orig-rec-l0137"></a>

**迁入原文｜REC｜旧 L137–L161** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0137)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 3. 接入现有操作与开关 → 3.1 三档恢复链强度

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0137 -->
#### 3.1 三档恢复链强度

2026-09-08 用户明确要求本功能也按三档加强；沿用 Compatible / Enhanced / Extreme，
不另外增加一套互不相干的总开关。每个步骤按自己的普通许可和高级键取交集。

| 档位 | 来源范围 | 实际处置目标 | 完成要求 |
|---|---|---|---|
| Compatible（兼容） | 当前实体、标准保存/准入入口的基础观察 | 保持实体清除与旧资格拒绝；不主动遍历复杂第三方恢复容器 | 明示恢复链未深挖；身体消失不显示整链完成 |
| Enhanced（强化） | 已证明属于目标的直接记录、快照、工厂与任务 | 精确处置目标条目、直接恢复任务和已登记重建入口；保留共享容器其他条目 | 查询真实效果并检查适用保存结果；失去来源/写入证明时暂停对应步骤 |
| Extreme（极限） | 与当前裁决结果相关的派生快照、后继任务、换 UUID、新载体与跨保存重建链 | 持续发现并处置已证明的后继来源；旧 epoch 的迟到写入和重建在出口失效 | 新增来源使完成证据失效；所有适用义务、资源释放、保存及稳定观察闭合后才完成 |

三档选择不同生产范围，不用重复次数或扫描半径冒充增强。
强化档的直接任务也必须具备 `ATTACK_CALL_CHAIN` 的完整依赖；
极限档的后继任务另需 `SUPPRESS_REBUILD`。
派生源追踪、后继任务和跨保存重建的步骤最低要求 `STRICT_ALL`；
直接来源、精确记录和直接任务步骤最低要求 `RELATED_PATHS`。
高档仍不允许根据 Candidate 分数、同名类、同一管理器或 UUID 相同直接永久删除。

`Custom` 逐步骤取显式动作与逐键深度的交集；关闭一个依赖只使相关步骤不可执行，
不能靠全局 Extreme 或“强力攻击”重新打开。能力缺失、证据失效和待重启分别展示。
`RecoveryChainPolicy` 固定各步骤依赖，`RecoverySourceTracker` 使用 active 政策决定主动发现范围；
运行诊断分别发布 requested/effective 深度，尚无执行证据的步骤不得显示已完成。

阶段集中验证须覆盖三档差异、Custom 单项关闭、待重启、能力不足、政策切换和旧许可失效。


<!-- RONOVA-ORIGINAL-END orig-rec-l0137 -->

<a id="orig-rec-l0280"></a>

**迁入原文｜REC｜旧 L280–L292** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0280)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 8. 明确解除与新生效范围

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0280 -->
#### 8. 明确解除与新生效范围

`clear_uuid` / `clear_tombstones` 继续表示显式解除 Ronova 裁决与限制，
不改成“清除敌方恢复链”命令，不偷偷延迟解除。

- 若来源处置尚未执行，clear 后旧 lease 失效，不继续破坏目标记录。
- 若来源处置已经发生，clear 不自动还原被处理的记录、订阅或任务，
  不重放死亡效果。需要还原时必须有单独、可证明且获授权的补偿合同。
- 原来源已确实移除时，clear 不应凭空重新产生该恢复记录。
- 有其他未处理来源时，明确报告缺口；clear 后该来源仍可能生成新对象。
- 合法新资格和新召唤按当前 Root policy 判断；不留下基于旧类名的隐形永久封禁。
- 已追踪旧 epoch 的任务不能因 clear 或合法重新激活恢复其旧资格。


<!-- RONOVA-ORIGINAL-END orig-rec-l0280 -->

<a id="orig-cf-l0117"></a>

**迁入原文｜CF｜旧 L117–L139** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0117)

原标题路径：2. Global Redlines → R3 — Hardened 必须是 Non-Blocking Core 的超集

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0117 -->
#### R3 — Hardened 必须是 Non-Blocking Core 的超集

```text
HARDENED
=
NON_BLOCKING_CORE
+
EXECUTION_AUTHORITY
+
INTEGRITY_HARDENING
```

进入 Hardened 不能关闭：

- Raw Observation；
- Convergence；
- Admission；
- Presence；
- Root Truth；
- Reconstitution。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0117 -->

<a id="orig-cf-l0319"></a>

**迁入原文｜CF｜旧 L319–L324** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0319)

原标题路径：3. Canonical Identifier Model → 3.5.1 PolicyRevision

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0319 -->
#### 3.5.1 PolicyRevision

`PolicyRevision` 是 Root-authorized policy/mode bundle 的单调版本，作用域为 Realm policy lineage。全文只使用 `PolicyRevision`；旧称 `ModeRevision` 不再作为独立类型。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0319 -->

<a id="orig-cf-l0692"></a>

**迁入原文｜CF｜旧 L692–L703** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0692)

原标题路径：10. Mode Contract

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0692 -->
#### 10. Mode Contract

冻结三种主模式：

```text
OBSERVATIONAL
NON_BLOCKING
HARDENED
```

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0692 -->

<a id="orig-cf-l0704"></a>

**迁入原文｜CF｜旧 L704–L716** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0704)

原标题路径：10. Mode Contract → 10.1 OBSERVATIONAL

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0704 -->
#### 10.1 OBSERVATIONAL

允许：

- Root read；
- Raw Observation；
- evidence collection；
- diagnose。

默认不主动 enforcement。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0704 -->

<a id="orig-cf-l0717"></a>

**迁入原文｜CF｜旧 L717–L743** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0717)

原标题路径：10. Mode Contract → 10.2 NON_BLOCKING

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0717 -->
#### 10.2 NON_BLOCKING

必须启用：

```text
Root Truth
Identity / Binding
Raw Observation
Phase Reconciliation
Admission
Presence
Convergence
Liveness
Integrity baseline
```

不要求启用：

```text
mutation deny
field-write interception
AllReturn
revocable dispatch
```

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0717 -->

<a id="orig-cf-l0744"></a>

**迁入原文｜CF｜旧 L744–L760** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0744)

原标题路径：10. Mode Contract → 10.3 HARDENED

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0744 -->
#### 10.3 HARDENED

必须包含 NON_BLOCKING 全部能力，再增加：

```text
Execution Capability
Dispatch Authority
Authority AllReturn
Transition Witness hardening
Critical Reentrancy Firewall
Semantic Class Seal
Critical Edge Proof
Native Integrity
```

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0744 -->

<a id="orig-cf-l0761"></a>

**迁入原文｜CF｜旧 L761–L794** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0761)

原标题路径：11. Mode Scope

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0761 -->
#### 11. Mode Scope

模式 selector 维度冻结为：

```text
GLOBAL
REALM
DIMENSION
SUBJECT_CLASS
SUBJECT
DOMAIN
```

`DOMAIN` 与主体定位层级正交，不能被错误地放进一条继承链。一次 mode resolve 使用完整 selector：

```text
ModeSelector
├ realmId?
├ dimensionId?
├ subjectClass?
├ subjectKey?
└ stateDomainId?
```

确定性解析规则：

1. 先在同一 `stateDomainId` 分区内按 `GLOBAL → REALM → DIMENSION → SUBJECT_CLASS → SUBJECT` 选择最具体匹配项；
2. 没有 domain-specific 规则时才回退到 `stateDomainId = *` 的通用分区；
3. 同一 specificity 出现两个互不相同的有效值是 `POLICY_CONFLICT`，不得按加载顺序取最后一个；
4. 更具体 selector 可以收紧，但不得违反 Constitutional Invariants；
5. 解析结果必须携带 `PolicyRevision` 和命中的 rule id，供 audit 与 completion revalidation 使用。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0761 -->

<a id="orig-cf-l0795"></a>

**迁入原文｜CF｜旧 L795–L823** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0795)

原标题路径：12. Mode Change Authority

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0795 -->
#### 12. Mode Change Authority

Mode 切换必须是：

```text
Evidence
→ Incident / Policy Evaluation
→ Root-authorized PolicyOverlay
→ PolicyRevision
→ Projection publish
```

禁止：

```text
Domain 自己看到危险
→ 本地直接 HARDENED
```

Domain 可以：

```text
PROPOSE_ESCALATION
```

但不能自行 author mode truth。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0795 -->

<a id="orig-cf-l1604"></a>

**迁入原文｜CF｜旧 L1604–L1615** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1604)

原标题路径：29. Freeze Decision Summary → ID-4 — Mode Switching

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1604 -->
#### ID-4 — Mode Switching

Mode 切换：

> **Root-authorized Policy change**

而不是 Domain 自行升级。

HARDENED 永远是 NON_BLOCKING 的超集。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1604 -->

<a id="orig-p0a-l1209"></a>

**迁入原文｜P0A｜旧 L1209–L1210** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1209)

原标题路径：30. Policy Model

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1209 -->
#### 30. Policy Model


<!-- RONOVA-ORIGINAL-END orig-p0a-l1209 -->

<a id="orig-p0a-l1211"></a>

**迁入原文｜P0A｜旧 L1211–L1220** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1211)

原标题路径：30. Policy Model → 30.1 AuthorityMode

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1211 -->
#### 30.1 AuthorityMode

```text
OBSERVATIONAL
NON_BLOCKING
HARDENED
```

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l1211 -->

<a id="orig-p0a-l1221"></a>

**迁入原文｜P0A｜旧 L1221–L1230** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1221)

原标题路径：30. Policy Model → 30.2 ModeSelector

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1221 -->
#### 30.2 ModeSelector

```text
ModeSelector(realmId?, dimensionId?, subjectClass?, subjectKey?, stateDomainId?)
```

`stateDomainId` 与主体定位维度正交，不是继承链末端。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l1221 -->

<a id="orig-p0a-l1231"></a>

**迁入原文｜P0A｜旧 L1231–L1243** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1231)

原标题路径：30. Policy Model → 30.3 PolicyOverlay

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1231 -->
#### 30.3 PolicyOverlay

```java
public record PolicyOverlay(
        PolicyRevision revision,
        ModeSelector selector,
        AuthorityMode mode,
        Map<PolicyParameterId, PolicyValue> parameters
) {}
```

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l1231 -->

<a id="orig-p0a-l1244"></a>

**迁入原文｜P0A｜旧 L1244–L1260** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1244)

原标题路径：31. Policy Resolution

**规范关系调整：** 本段的旧位次/冻结不可覆盖规则仅保留作来源记录，不再阻止显式修订；技术不变量本轮未据此删除。参见 [当前文档效力](README.md#scope-governance)。

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1244 -->
#### 31. Policy Resolution

deterministic resolution：

```text
在同一 StateDomain 分区：GLOBAL → REALM → DIMENSION → SUBJECT_CLASS → SUBJECT
无 domain-specific match：回退到 domain=* 分区
```

同一 specificity 有两个不同有效值即 `POLICY_CONFLICT`，不得按加载顺序覆盖。更具体规则可以收紧，但：

```text
不得违反 Constitutional Invariants
```

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l1244 -->

<a id="orig-p0a-l1286"></a>

**迁入原文｜P0A｜旧 L1286–L1312** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1286)

原标题路径：33. Mode 切换

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1286 -->
#### 33. Mode 切换

Domain 只能：

```text
propose escalation
```

真正切换：

```text
Evidence
→ Policy Evaluation
→ Root command
→ new PolicyRevision
→ projection publish
```

禁止本地：

```text
if suspicious:
    localMode = HARDENED
```

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l1286 -->

<a id="orig-p0a-l2066"></a>

**迁入原文｜P0A｜旧 L2066–L2084** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l2066)

原标题路径：56. Feature Flags

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l2066 -->
#### 56. Feature Flags

P0-A 核心只能使用一个一致 cutover state：

```text
OFF → SHADOW → VERIFY → ACTIVE
                      ↘ FENCED
```

- `OFF`：只走已证明的 FULL L6 baseline；
- `SHADOW`：构建 P0-A 结果但不驱动 Authority；
- `VERIFY`：逐 invariant 对比，分歧显式 incident；
- `ACTIVE`：SubjectKey/binding/ownership/projection 作为一个原子 bundle 切换；
- `FENCED`：无法证明 bundle 一致时停止 authority-sensitive progression。

`p0a.relationship.experimental` 可单独控制非 authoritative discovery telemetry，但不能改变 truth/binding/ownership。禁止 identity、incarnation、ownership、projection 四个独立布尔开关形成半启用状态；任何 flag 不得绕过 Constitutional Invariants，production ACTIVE 后也不得被普通客户端 config 关闭。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l2066 -->

<a id="orig-road-l0137"></a>

**迁入原文｜ROAD｜旧 L137–L156** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-road-l0137)

原标题路径：Ronova L6／L6E 全量加强路线 V1.0 — Internal Root → 4. 全部 54 项高级能力的阶段对照 → 4.1 攻击：15 项

<!-- RONOVA-ORIGINAL-BEGIN orig-road-l0137 -->
#### 4.1 攻击：15 项

| 能力键 | 7.17 承接基础 | L6 加强 | L6E 深化（阶段） | 依赖 | 完成条件 |
|---|---|---|---|---|---|
| `attack.non_entity` | 非实体表面的观察、归属和独立步骤 | Root 将非实体对象纳入同一目标计划与结果投影 | B/C/D：处理相关显示、任务、记录和资源及其 producer 后继 | 非实体归属；客户端能力；资源版本 | 当前操作必需的非实体结果分别完成，缺失表面明确保留待办 |
| `attack.logic_source` | 实际读写、调用与 producer 来源观察 | Root 批准逻辑来源的适用范围与处置步骤 | A/B/D：关联字段、容器、方法、函数对象和句柄；变化后重建关系 | 当前绑定；调用证据；代码版本 | 维持目标结果的已纳入逻辑来源受到对应处置，晚到结果不能绕过当前资格 |
| `attack.recovery_source` | 通用恢复链发现、资格、记录与任务处置基础 | Root 统一恢复源资格、限制及跨表面计划 | A/B/C/D：覆盖静态管理器、独立快照、重建器及跨 epoch/重启后继 | 记录身份；任务因果；保存与重建入口 | 恢复记录、任务、产物和适用持久出口逐项完成处置与复验 |
| `attack.state_source` | 独立状态写入、生命模型、外部维护源与反弹反馈基础 | Root 统一状态域意图、写入范围和修复依据 | A/B/C：联动真实生命、计算值、镜像、附加生命和外部写源 | State Ownership；WriteSink；模型与槽位版本 | 实际生命/状态满足意图，持续反弹被处理且镜像未掩盖真实结果 |
| `attack.registration_source` | 精确实体索引、准入与重插入口 | Root 同步提交资格撤销、TargetEpoch 和 Admission revision | A/C/D：覆盖工厂、注册表、重注册及 loader 血缘产物 | 实例绑定；注册归属；当前代际 | 旧资格无法重新准入，新产物按真实血缘重新解析 |
| `attack.task_chain` | 自有及目标相关任务的归属、取消与提交出口基础 | Root 批准任务范围和上下文，保留跨步骤原因 | B/C/D：递归后继、计时器、线程与登记 Native callback；跨重启恢复 | 调度关系；任务状态；提交点资格 | 取消、结束和提交分别确认，旧任务及后继无迟到回流 |
| `attack.callback_chain` | 订阅、回调与迟到入口观察 | Root 将回调限制与当前操作资格关联 | B/D：覆盖 lambda、委托、重订阅和桥接，关系变化后重验 | 订阅身份；调用上下文；代码版本 | 相关回调不能恢复旧结果，无关订阅保持独立 |
| `attack.network_reconstruction` | 身份、连接 nonce、epoch/revision 与发送/确认事务 | Root 统一网络发布资格和投影版本 | B/C：重发、镜像、重连和跨保存后的客户端重建 | 当前连接；投影版本；成员参与要求 | 旧包、旧镜像和旧 ACK 不复活旧状态，当前必需参与者完成相应合同 |
| `attack.save_reconstruction` | 持久来源处置、保存查证和再次加载基础 | Root 编排来源义务、保存结果与重启恢复 | A/C：跨记录、快照、检查点及重新激活的重建链 | 外部记录归属；保存完成证据；原效果身份 | 独立读取和重启复验确认适用来源结果，内存缺失不替代保存结论 |
| `attack.dynamic_code` | 动态类、producer、loader 和变换证据 | Root 为当前代码范围批准计划与 lease | B/D：生成类、隐藏类、代理、lambda 及登记 JIT/Native 后继 | 定义 loader；实际类身份；变换周期 | 必需代码副本的语义与执行均有效，变化立即触发失效和重评 |
| `attack.dispatch` | 独立调用链、上下文与分派观察 | Root 批准操作上下文和分派权限 | B/D：Gateway、回调、函数对象、句柄与登记原生出口的独立执行 | ExecutionAuthorityContext；CompletionContract | 调用实际到达获准执行路径，返回或异常满足对应后置条件 |
| `attack.runtime_patch_disablement` | 防御代码、补丁来源与作用范围识别 | Root 将补丁处置绑定目标、操作及版本 | B/D：处置实际阻止执行的补丁与 producer，重现后继续处理 | 补丁关系；代码权限；修复依据 | 当前阻断路径失能且获准语义有效，无关功能不随整包关闭 |
| `attack.target_class_lineage` | 实际 Class/loader 关系，避免仅凭类名授权 | Root 统一目标身份、实例和类血缘的适用资格 | A/C/D：跨 loader、重定义、新 UUID 和新 incarnation 的血缘绑定 | IdentityBinding；producer 因果；类版本 | 后继能精确归入当前主体，合法无关新实体不继承旧裁决 |
| `attack.final_result_takeover` | 独立动作、九项完成观察与当前结果投影 | Root 接受完成证据并编排跨表面结果 | B/C/D：控制状态、索引、恢复源、显示和后继结果，反弹后重新收敛 | 全部实际效果所需动作许可；必需表面 | 适用完成合同和稳定窗口成立，重试不重放生命周期效果 |
| `attack.source_trace` | 有界来源、调用、任务和复制关系及缺口记录 | 来源图作为 Root 证据输入，保留原因与版本 | A/B/D：动态 producer、跨线程、句柄和 Native 出口的可续接因果 | 独立证据；来源版本；义务归并 | 从目标到来源、执行和结果可追溯；溢出或关系中断显式保留 |


<!-- RONOVA-ORIGINAL-END orig-road-l0137 -->

<a id="orig-rule-l0133"></a>

**迁入原文｜RULE｜旧 L133–L140** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rule-l0133)

原标题路径：Ronova MS 规则内加强方案 V2.0 → 1. 规则基（L0 铁律 + L1 参考库） → 闸门 1 —— 目标判定闸

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-rule-l0133 -->
#### 闸门 1 —— 目标判定闸

| ID | 规则 | 依据 |
|---|---|---|
| **R5** | 目标分类不能依赖其他 Mod 的包名、类名、文件或私有实现；应使用通用实体类别、公开状态、标签、权限和自己的状态服务 | [`msitem:41`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#先定义击杀契约) |
| **R6** | 未分类的新生物、Boss 或多部位对象：保守地 `IGNORE`，直到完成兼容性测试 | [`msitem:218`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#统一目标结果和防御策略) |
| **R7** | 多部位实体的部位 ≠ 根实体；命中部位必须明确「转根 / 只作用部位 / 拒绝」；范围扫描不得把根与全部部位重复列入同一动作 | [`msitem:220`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#统一目标结果和防御策略) |


<!-- RONOVA-ORIGINAL-END orig-rule-l0133 -->

<a id="orig-ds-l0698"></a>

**迁入原文｜DS｜旧 L698–L707** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0698)

原标题路径：Ronova 设计规范 · 合并导读 → 10. 强度模型（`V2.6`）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0698 -->
#### 10. 强度模型（`V2.6`）

**三个平面**（`D89`）：Attack / Interception / Protection。**不得创造第四平面**或第二 Judgment 状态机。

| 目录 | 数量 | 构成 |
|---|---|---|
| `StrengthOptionKey` | **54** | 15 `attack.*` + 10 `intercept.*` + 17 `protect.*` + 12 `runtime.*` |
| `AttackAction` | **14** | strongAttack / rewriteLife / terminalize / directClear / clearEntityRecords / suppressRespawn / suppressRebuild / suppressAdmission / attackRender / clearTracking / clearHud / stripCodeDefense / attackCallChain / allReturn |
| `RuntimeSurface` | **8** | DATA / NETWORK / PRESENTATION / INPUT / EVENT / TEMPORAL / REGISTRY / RESOURCE |


<!-- RONOVA-ORIGINAL-END orig-ds-l0698 -->

<a id="orig-ds-l0708"></a>

**迁入原文｜DS｜旧 L708–L720** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0708)

原标题路径：Ronova 设计规范 · 合并导读 → 10. 强度模型（`V2.6`） → 10.1 三档正式预设（`V2.6:41-45`、`:600-604`）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0708 -->
#### 10.1 三档正式预设（[`V2.6:41-45`](MS-SKILLS-PLAN.md#orig-v26-l0037)、`:600-604`）

`COMPATIBLE 兼容` / `ENHANCED 强化` / `EXTREME 极限代码对抗`，加 `CUSTOM`。
EXTREME「**不再被定义成"兼容档的更高数值版本"**」（[`V2.6:47`](MS-SKILLS-PLAN.md#orig-v26-l0037)）。

| 档 | 范围 | 兼容策略 | 失败策略 |
|---|---|---|---|
| COMPATIBLE | Ronova 核心 + 稳定实体路径 | 优先兼容 | 非关键路径记录/降级，**Authority 不失真** |
| ENHANCED | Ronova + 当前目标 + 已识别直接结果链 | 强度优先、尽量精确 | 对相关对象/类/调用隔离、修复或对象级 fence |
| EXTREME | 当前 Judgment 的全部候选 / 已证明同 JVM 结果攻击面 | **不保护敌对 Mod 兼容性** | 先限制未知结果资格；无法闭合时按**最小必要 scope** fence |

[`V2.6:400`](MS-MSITEM-PLAN.md#orig-v26-l0389)：「**胜负结果优先，第三方兼容性不是保护目标。**」


<!-- RONOVA-ORIGINAL-END orig-ds-l0708 -->

<a id="orig-ds-l0721"></a>

**迁入原文｜DS｜旧 L721–L729** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0721)

原标题路径：Ronova 设计规范 · 合并导读 → 10. 强度模型（`V2.6`） → 10.2 两套**不同**的层级值集（不要合并）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0721 -->
#### 10.2 两套**不同**的层级值集（不要合并）

| 用于 | 取值（升序） |
|---|---|
| **Attack / Protection 子项** | `跟随预设 / 关闭 / 兼容 / 强化 / 极限`（FOLLOW_PROFILE / OFF / COMPATIBLE / ENHANCED / EXTREME） |
| **拦截机制子项** | `跟随预设 / 关闭 / 监控 / 保护若娜瓦 / 加强相关路径 / 全部严格`（FOLLOW_PROFILE / OFF / MONITOR / PROTECT_RONOVA / RELATED_PATHS / STRICT_ALL） |

预设→机制映射（[`V2.6:640-642`](MS-MSITEM-PLAN.md#orig-v26-l0614)）：兼容 → 保护若娜瓦；强化 → 加强相关路径；极限 → 全部严格。


<!-- RONOVA-ORIGINAL-END orig-ds-l0721 -->

<a id="orig-ds-l0730"></a>

**迁入原文｜DS｜旧 L730–L768** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0730)

原标题路径：Ronova 设计规范 · 合并导读 → 10. 强度模型（`V2.6`） → 10.3 五个策略字段与组合算子（`V2.6:649-673`、`:765-774`）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0730 -->
#### 10.3 五个策略字段与组合算子（[`V2.6:649-673`](MS-MSITEM-PLAN.md#orig-v26-l0645)、`:765-774`）

```
configuredLevel
supportedCeiling
effectiveLevel
evidenceState
activationState
```

组合**不是 `min(...)`，而是集合交集 `∩`**：

```
ProfilePresetMatrix(profile, optionKey, matrixRevision) → requestedLevel

requestedLevel ∩ supportedCeiling ∩ evidenceCeiling ∩ restart/activation state
    → effectiveLevel
```

等价推论（[`V2.6:878`](MS-MSITEM-PLAN.md#orig-v26-l0874)）：「`effectiveLevel` **永远不高于**运行时 ceiling 与证据 ceiling；
缺少 Native/JVMTI、需要重启或专用服务端不支持客户端模块时**必须真实降级并显示原因**。」

**四个正交状态轴**（[`V2.6:677-698`](MS-MSITEM-PLAN.md#orig-v26-l0675)、`:757-759`，`D73`）：

```
executionState:  ACTIVE | QUARANTINED | FENCED
attackOutcome:   APPLIED | PARTIAL | CLEARED | FAILED
fenceScope:      NONE | TARGET | OPERATION | PLANE | ROOT | REALM
```

- 「策略的 `activationState`/`evidenceState`、结果的 `executionState`/`attackOutcome`
  和 Authority 的 commit 状态**禁止互相覆盖**」；
- 「**FENCED 不能推导攻击成功**」（`D73`）；
- 旧字段 `clearanceState` 降级为只读兼容展示，**不再拥有独立决策逻辑**。

**完成声明**（[`V2.6:714-727`](MS-MSITEM-PLAN.md#orig-v26-l0675)）：非清除 CriticalOperation **只有 `attackOutcome=APPLIED AND
completionSatisfied=true`** 才能显示"攻击已完成"；**禁止**用 `isTerminalOutcome` 表达该含义
（`TERMINAL` 保留给目标存在性终结语义）。


<!-- RONOVA-ORIGINAL-END orig-ds-l0730 -->

<a id="orig-ds-l0769"></a>

**迁入原文｜DS｜旧 L769–L786** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0769)

原标题路径：Ronova 设计规范 · 合并导读 → 10. 强度模型（`V2.6`） → 10.4 表面成熟度阶梯（`V2.6:1259-1267`）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0769 -->
#### 10.4 表面成熟度阶梯（[`V2.6:1259-1267`](MS-SKILLS-PLAN.md#orig-v26-l1255)）

`S0 — Absent` / `S1 — Observe` / `S2 — Attribute` / `S3 — Enforce` / `S4 — Repair` /
`S5 — Recover` / `S6 — Complete`（源码枚举名 `S0_ABSENT`…`S6_COMPLETE`）。

| 级别 | 必须具备 | 可声明 |
|---|---|---|
| S0 | 无可靠 observer | `UNAVAILABLE` |
| S1 | 有界观察与 revision | `MONITOR`；**不能声称拦截/恢复** |
| S2 | 来源、上下文与证据独立性可验证 | 可进入 Candidate；不能自动高副作用处置 |
| S3 | 有真实 gate/executor 与 denial/effect receipt | 按实际报告 DENIED/APPLIED；**穿透不得伪报成功** |
| S4 | 能恢复受保护结果并重验 | 可报告恢复动作；未闭合仍为 PARTIAL |
| S5 | 重启、epoch、任务、网络、保存链可恢复 | 可承接跨时序 obligation |
| S6 | CompletionContract、稳定窗口与证据失效重评完成 | 才可声明该 Surface 对相应操作 `FULL` |

[`V2.6:1269`](MS-SKILLS-PLAN.md#orig-v26-l1255)：「EXTREME 要求当前操作所依赖的**所有** Surface 达到其 CompletionContract 指定成熟度；
一个表面只有 S1，**不得**因为其他表面 S6 就把整项能力显示 FULL。」


<!-- RONOVA-ORIGINAL-END orig-ds-l0769 -->

<a id="orig-ds-l0787"></a>

**迁入原文｜DS｜旧 L787–L795** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0787)

原标题路径：Ronova 设计规范 · 合并导读 → 10. 强度模型（`V2.6`） → 10.5 逐键矩阵与三档差异性（`D91`、`V2.6:877`）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0787 -->
#### 10.5 逐键矩阵与三档差异性（`D91`、[`V2.6:877`](MS-MSITEM-PLAN.md#orig-v26-l0874)）

- 三个正式预设**从 7.17 起**由 `ProfilePresetMatrix(profile, StrengthOptionKey)` 逐键解析；
  r52 的按-plane 统一映射「**仅是 7.16 policy foundation，不是 V2.5 最终矩阵**」；
- [`V2.6:877`](MS-MSITEM-PLAN.md#orig-v26-l0874)：**同一 `StrengthOptionKey` 的三档必须命中不同生产策略，
  或明确声明相同并给出原因；不得三个档位共用一个 executor 参数却显示三档**；
- [`V2.6:925`](MS-MSITEM-PLAN.md#orig-v26-l0884)：Host Safety、Loop Guard、single-writer、防回滚、基础 Admission 与资源安全
  **不是攻击许可的副作用，不可通过普通 OFF 关闭**；UI 必须说明"关闭主动加强，不解除已有裁决"。


<!-- RONOVA-ORIGINAL-END orig-ds-l0787 -->

<a id="orig-ds-l0796"></a>

**迁入原文｜DS｜旧 L796–L810** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0796)

原标题路径：Ronova 设计规范 · 合并导读 → 10. 强度模型（`V2.6`） → 10.6 `AttackAction` 的逐档许可（`V2.6:895-910`）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0796 -->
#### 10.6 `AttackAction` 的逐档许可（[`V2.6:895-910`](MS-MSITEM-PLAN.md#orig-v26-l0884)）

五个动作在 **COMPATIBLE 档为「关」**，ENHANCED / EXTREME 为「开」：
`directClear` · `suppressRebuild` · `stripCodeDefense` · `attackCallChain` · `allReturn`。
其余九个三档皆「开」。每个动作另有**所需高级键**的约束（例如 `allReturn` 需 `ATTACK_DISPATCH`；
改字节码时另需 `ATTACK_DYNAMIC_CODE` 或 `ATTACK_RUNTIME_PATCH_DISABLEMENT`）。

**六条冲突规则**（[`V2.6:920-925`](MS-MSITEM-PLAN.md#orig-v26-l0884)）中最关键的两条：

- 所需普通许可为 DISABLED → **拒绝该 step，报告 `ACTION_DISABLED`；不得自动升档或开启依赖**；
- 关闭能力**只约束新 step**；撤销已有 Judgment **必须另走显式 Authority command**，
  已提交义务不被删掉。

---


<!-- RONOVA-ORIGINAL-END orig-ds-l0796 -->

<a id="ms-msitem-l224"></a>

## 正常死亡、运行时清除与混合执行

**框架来源：** [MS msitem，原 L224–L225](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#正常死亡运行时清除与混合执行)。

**编排说明（非原条款）：** 既有 VITALS、TERMINAL、实体清除与恢复链条款保留；按 MS 正常死亡/运行态清除/混合模式区分职责，不篡改原结果语义。

<a id="orig-bl-l0874"></a>

**迁入原文｜BL｜旧 L874–L877** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0874)

原标题路径：9. VITALS Enhanced — 通用改血加强

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0874 -->
#### 9. VITALS Enhanced — 通用改血加强

这是之前“改血加强”最终整理版。


<!-- RONOVA-ORIGINAL-END orig-bl-l0874 -->

<a id="orig-bl-l0878"></a>

**迁入原文｜BL｜旧 L878–L902** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0878)

原标题路径：9. VITALS Enhanced — 通用改血加强 → 9.1 当前设计基础

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0878 -->
#### 9.1 当前设计基础

现有 `docs/archive/architecture/HEALTH-AUTHORITY-DESIGN.md` 已冻结：

```text
health-contract
health-core
health-forge
health-native
ronova-root/health
```

Health Authority 与 Judgment、Owner Protection 平级，但不得塞进 `AuthorityGuard` 或 `JudgmentEngine`。

最小模式：

```text
CURRENT
MAXIMUM
LOCKED
CLEAR
```

---


<!-- RONOVA-ORIGINAL-END orig-bl-l0878 -->

<a id="orig-bl-l0903"></a>

**迁入原文｜BL｜旧 L903–L928** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0903)

原标题路径：9. VITALS Enhanced — 通用改血加强 → 9.2 VITALS 不是一次 setHealth

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0903 -->
#### 9.2 VITALS 不是一次 setHealth

完整模型：

```text
VitalAuthorityIntent
        ↓
VitalStateResolver
        ↓
State Ownership Graph
        ↓
VitalDesiredState
        ↓
Raw Vital Observation
        ↓
Invariant
        ↓
Vital Enforcement
        ↓
Verify
        ↓
Reassert when needed
```

---


<!-- RONOVA-ORIGINAL-END orig-bl-l0903 -->

<a id="orig-bl-l0929"></a>

**迁入原文｜BL｜旧 L929–L951** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0929)

原标题路径：9. VITALS Enhanced — 通用改血加强 → 9.3 至少覆盖的生命状态

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0929 -->
#### 9.3 至少覆盖的生命状态

不只：

```text
health
```

还要考虑：

```text
maxHealth
absorption
attribute-backed state
death/lifecycle-related state
revive-related backing state
related authoritative manager/controller
```

但不得把这些直接混成 TERMINAL。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l0929 -->

<a id="orig-bl-l0952"></a>

**迁入原文｜BL｜旧 L952–L992** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0952)

原标题路径：9. VITALS Enhanced — 通用改血加强 → 9.4 VitalStateResolver

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0952 -->
#### 9.4 VitalStateResolver

目标不是识别某个 Boss 名字，而是找真正的生命状态 owner：

```text
Target
 ↓
Entity-local health
 ↓
Attribute-backed state
 ↓
Capability / Attachment
 ↓
Manager
 ↓
Controller
 ↓
Related authoritative holder
```

只有能够建立：

```text
Target ↔ State Owner
```

的明确关系，才允许声称已解析生命 Authority。

失败状态：

```text
RESOLVED
AMBIGUOUS
UNRESOLVED
STALE
```

`UNRESOLVED` 时绝不能假装 VITALS 成功。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l0952 -->

<a id="orig-bl-l0993"></a>

**迁入原文｜BL｜旧 L993–L1040** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0993)

原标题路径：9. VITALS Enhanced — 通用改血加强 → 9.4 VitalStateResolver → 9.4.1 Resolver 必须是“通用发现 + 可插拔证据”，不是 Adapter 海

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0993 -->
#### 9.4.1 Resolver 必须是“通用发现 + 可插拔证据”，不是 Adapter 海

推荐结构：

```text
VitalStateResolver
      ↓
ResolverPipeline
├ BuiltinLivingStateResolver
├ AttributeStateResolver
├ Capability / Attachment Resolver
├ Relationship / Manager Resolver
└ External ResolverProvider SPI
      ↓
OwnershipCandidate[]
      ↓
Independent Validation
      ↓
Ownership Binding decision
```

`ResolverProvider` 只允许输出**候选关系与访问证据**，例如：

```text
subject
candidate state holder
state kind
read/write access plan
provenance
confidence / validation evidence
```

第三方 Resolver **不得直接写入 Root**，也不得声明：

```text
“我是插件，所以这个对象就是最终 owner”
```

最终 State Ownership 仍由 Ronova 的 Authority / Root 逻辑确认。

这样可以允许：

- Ronova 自身提供通用 resolver；
- 整合包 / 兼容模块补充特殊数据结构解析；
- 第三方模组主动提供标准化 resolver；

但不会把“插件注册”本身变成新的 Authority。


<!-- RONOVA-ORIGINAL-END orig-bl-l0993 -->

<a id="orig-bl-l1041"></a>

**迁入原文｜BL｜旧 L1041–L1064** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l1041)

原标题路径：9. VITALS Enhanced — 通用改血加强 → 9.4 VitalStateResolver → 9.4.2 Resolver Provider 的可信边界

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l1041 -->
#### 9.4.2 Resolver Provider 的可信边界

在 T2/T3 对抗环境下，恶意 Mod 也可能尝试注册假的 Resolver，因此 provider 输出必须被当成：

```text
Discovery Evidence
```

而不是：

```text
Trusted Truth
```

至少应验证：

- candidate 与 subject 是否存在稳定关系；
- candidate 的读写是否真实影响目标生命语义；
- relation 是否能被 Raw Observation / behavioral probe 独立确认；
- provider revision / provenance 是否可审计；
- provider 失效后是否能回到 `AMBIGUOUS / UNRESOLVED`，而不是保留陈旧 binding。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l1041 -->

<a id="orig-bl-l1065"></a>

**迁入原文｜BL｜旧 L1065–L1088** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l1065)

原标题路径：9. VITALS Enhanced — 通用改血加强 → 9.5 UNRESOLVED 失败语义

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l1065 -->
#### 9.5 UNRESOLVED 失败语义

Root Policy 决定：

```text
OBSERVE_ONLY
DENY_VITALS
HOLD_ADMISSION
ISOLATE
REQUEST_REEVALUATION
```

原则：

```text
Resolver 找不到血在哪
≠
自动 TERMINAL
```

TERMINAL 必须来自独立 Judgment。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l1065 -->

<a id="orig-bl-l1106"></a>

**迁入原文｜BL｜旧 L1106–L1122** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l1106)

原标题路径：9. VITALS Enhanced — 通用改血加强 → 9.7 VITALS 铁律

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l1106 -->
#### 9.7 VITALS 铁律

```text
VITALS = 改变生命状态
TERMINAL = 改变存在资格
```

即使：

```text
health = 0
```

也不能自动生成 Tombstone。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l1106 -->

<a id="orig-rule-l0798"></a>

**迁入原文｜RULE｜旧 L798–L806** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rule-l0798)

原标题路径：Ronova MS 规则内加强方案 V2.0 → 5. 加强块 → D 块 —— 执行器与结算（能力本体）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

**r66 接线规划更正（fix4，非迁入原文）：** D 块旧判断已收窄：命令中的“未接线”是硬编码诊断，不是全执行器枚举；服务器当前路径委派 `TerminalPlanExecutor` 并回调 `runPlannedIntegration`。保留动作接线工作，不恢复旧无计划路径；其余完成条件生产者仍须逐项核查。 [当前更正](R66-CAPABILITY-WIRING-PLAN.md#action-wiring)。

<!-- RONOVA-ORIGINAL-BEGIN orig-rule-l0798 -->
#### D 块 —— 执行器与结算（能力本体）

| ID | 项 | 现状 |
|---|---|---|
| **D1** | 13 个未接线动作的执行器 | `DominionCommands.java:280-281` |
| **D2** | `CompletionTracker` 补生产者 | 9 项条件中 6 项零引用 |
| **D3** | `TerminalLifecycle` 服务器分支 | `TerminalLifecycle.java:49-62` 每条路径都 return，整体不可达 |
| **D4** | 单一结算所有者的显式化 | R32/R33：事件层、注入层不得各自结算 |


<!-- RONOVA-ORIGINAL-END orig-rule-l0798 -->

<a id="orig-non-l0145"></a>

**迁入原文｜NON｜旧 L145–L152** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-non-l0145)

原标题路径：Ronova 无违规加强方案 V1.0 → 3. 加强块（按实现顺序） → D 块 —— 执行器补齐（能力本体）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

**r66 接线规划更正（fix4，非迁入原文）：** D 块旧判断已收窄：命令中的“未接线”是硬编码诊断，不是全执行器枚举；服务器当前路径委派 `TerminalPlanExecutor` 并回调 `runPlannedIntegration`。保留动作接线工作，不恢复旧无计划路径；其余完成条件生产者仍须逐项核查。 [当前更正](R66-CAPABILITY-WIRING-PLAN.md#action-wiring)。

<!-- RONOVA-ORIGINAL-BEGIN orig-non-l0145 -->
#### D 块 —— 执行器补齐（能力本体）

| ID | 项 | 现状 | 依据 |
|---|---|---|---|
| **D1** | 12 个普通动作的执行器 | 14 个动作**只有 1 个接线**（`DominionCommands.java:280-281` 硬编码） | 项目自身 |
| **D2** | `CompletionTracker` 补 6 项生产者 | 9 项条件中 6 项**零引用**（`WORLD_RECORDS` / `OLD_EPOCH_REJECTED` / `RECOVERY_TASKS` / `REBUILD_ENTRYPOINTS` / `TRACKING` / `SERVER_PRESENCE`） | [`skills:139-140`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#2-实体清除) 九项条件；[V2.6:352-357](MS-SKILLS-PLAN.md#orig-v26-l0341) |
| **D3** | `TerminalLifecycle` 服务器分支 | 整体不可达（`TerminalLifecycle.java:49-62` 每条路径都 return） | 项目自身 |


<!-- RONOVA-ORIGINAL-END orig-non-l0145 -->

<a id="orig-ds-l1066"></a>

**迁入原文｜DS｜旧 L1066–L1071** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l1066)

原标题路径：第二部分 · 加强方案（**提案，非规范**） → 17. 与项目自有规范对齐后的加强块 → D 块 —— 执行器与结算（能力本体）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

**r66 接线规划更正（fix4，非迁入原文）：** D 块旧判断已收窄：命令中的“未接线”是硬编码诊断，不是全执行器枚举；服务器当前路径委派 `TerminalPlanExecutor` 并回调 `runPlannedIntegration`。保留动作接线工作，不恢复旧无计划路径；其余完成条件生产者仍须逐项核查。 [当前更正](R66-CAPABILITY-WIRING-PLAN.md#action-wiring)。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l1066 -->
#### D 块 —— 执行器与结算（能力本体）

`DominionCommands.java:280-281` 对 14 个动作中的 13 个硬编码「未接线」（= C11）；
`CompletionTracker` 9 项条件中 6 项零引用；`TerminalLifecycle.java:49-62` 服务器分支整体不可达。
**注**：`D94` 要求能力按实际状态报告——**当前的"未接线"显示是合规的**，缺口是未接线本身。


<!-- RONOVA-ORIGINAL-END orig-ds-l1066 -->

<a id="ms-msitem-l226"></a>

### 正常死亡

**框架来源：** [MS msitem，原 L226–L237](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#正常死亡)。

**编排说明（非原条款）：** 常规生态效果仅结算一次。原生命状态处理不等于普通伤害，因此本节以关联原条款明确适用语义，不强行搬运不相干内容充数。

**关联原条款：** [MSC · 1.4 目标与结果策略](MS-MSITEM-PLAN.md#orig-msc-l0069)；[BL · 10.1 TERMINAL 最终语义](MS-MSITEM-PLAN.md#orig-bl-l1125)；[CF · 16. Side-Effect Contract](MS-MSITEM-PLAN.md#orig-cf-l1011)。

本节点没有被单独搬入的原文段落；它保留 MS 的独立职责入口。上列关联条款或下级章节不等于本节点所有细节已经覆盖；适用性与缺口见映射表。

<a id="ms-msitem-l238"></a>

### 运行时清除

**框架来源：** [MS msitem，原 L238–L254](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#运行时清除)。

**编排说明（非原条款）：** 承接存在性终结、生成/重建压制、恢复记录、任务、资源和完成证书；仅视觉隐藏不替代服务端结果。

<a id="orig-bl-l1123"></a>

**迁入原文｜BL｜旧 L1123–L1124** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l1123)

原标题路径：10. TERMINAL + Admission Closure

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l1123 -->
#### 10. TERMINAL + Admission Closure


<!-- RONOVA-ORIGINAL-END orig-bl-l1123 -->

<a id="orig-bl-l1125"></a>

**迁入原文｜BL｜旧 L1125–L1146** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l1125)

原标题路径：10. TERMINAL + Admission Closure → 10.1 TERMINAL 最终语义

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l1125 -->
#### 10.1 TERMINAL 最终语义

```text
Judgment
  ↓
Durable Commit
  ↓
Tombstone
  ↓
Lifecycle Integration ONCE
  ↓
State Reassertion REPEATABLE + IDEMPOTENT
  ↓
TerminalWatch
  ↓
Raw Observation
```

物理删除失败不等于 Judgment 失效。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l1125 -->

<a id="orig-bl-l1147"></a>

**迁入原文｜BL｜旧 L1147–L1171** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l1147)

原标题路径：10. TERMINAL + Admission Closure → 10.2 Admission Closure

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l1147 -->
#### 10.2 Admission Closure

Admission 负责堵住：

- spawn；
- factory；
- NBT load；
- reconstruction；
- dimension transfer；
- reinsertion；
- command summon；
- 其他明确的 creation/admission seam。

关系：

```text
TERMINAL
= 已经存在的对象不再具有存在资格

Admission
= 它也不能重新合法进入
```

---


<!-- RONOVA-ORIGINAL-END orig-bl-l1147 -->

<a id="orig-bl-l1172"></a>

**迁入原文｜BL｜旧 L1172–L1205** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l1172)

原标题路径：10. TERMINAL + Admission Closure → 10.3 Pre-Admission Binding

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l1172 -->
#### 10.3 Pre-Admission Binding

不能留下：

```text
Admission ALLOW
→ entity add
→ 过一会儿才建立 ownership
```

最终应是：

```text
Candidate
 ↓
Admission Evaluation
 ↓
Pre-Admission Binding
├ logical identity
├ initial ownership
├ constraint revision
└ visibility disposition
 ↓
Physical Admission
 ↓
Binding Activation
 ↓
Presence Publish
```

在必要 Authority Binding 激活之前，对象不得进入正常可交互 Presence。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l1172 -->

<a id="orig-bl-l1206"></a>

**迁入原文｜BL｜旧 L1206–L1241** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l1206)

原标题路径：10. TERMINAL + Admission Closure → 10.3 Pre-Admission Binding → 10.3.1 Pre-Binding 的现实实现边界

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l1206 -->
#### 10.3.1 Pre-Binding 的现实实现边界

不能把目标写成：

> “Minecraft 中任何 Entity 在对象构造前都必须同步执行 durable Root mutation 并完成绑定。”

这既不符合 Minecraft 生命周期，也违反 L6 “实时查询 hot path 只读本地 immutable projection”的方向。

应分成三层：

```text
A. HARD_PREBIND
已知 Subject / 已有 Tombstone / 已有强 Admission policy
→ 只读本地 immutable projection
→ admission 前完成判定与 binding

B. PRE_PRESENCE_BIND
对象可以已经构造、甚至进入内部 staging
→ 但在 Authority binding 激活前
→ 不进入正常可见 / 可交互 Presence

C. POST_ADMISSION_EMERGENCY_RECONCILE
第三方路径绕过标准 admission seam
→ Raw Observation 发现
→ 立即执行隔离 / deny / reassert fallback
```

关键规则：

1. **Pre-Binding 热路径不得同步执行 durable Root I/O。**
2. 已存在的 authoritative rule 必须通过本地 projection 快速判断。
3. 普通新实体如果没有任何现存 Authority requirement，不要求每次 spawn 都产生 Root mutation。
4. `UNRESOLVED` 的新实体是否 HOLD，由 Root Policy 决定；默认不能为了兼容性把所有未知对象一刀切拒绝。
5. “Physical object 已构造”与“已进入正常 Presence”不是同一个概念。
6. 对确实无法抢到 pre-admission seam 的第三方直接插入路径，必须有 Raw Observation + emergency reconcile，但不能虚假宣称绝对无窗口。


<!-- RONOVA-ORIGINAL-END orig-bl-l1206 -->

<a id="orig-bl-l1242"></a>

**迁入原文｜BL｜旧 L1242–L1256** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l1242)

原标题路径：10. TERMINAL + Admission Closure → 10.3 Pre-Admission Binding → 10.3.2 Pre-Binding 作为 P0-A 独立难点

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l1242 -->
#### 10.3.2 Pre-Binding 作为 P0-A 独立难点

后续 Implementation Spec 应把它单独列为 workstream，并验证至少：

- chunk/NBT load；
- normal spawn；
- command summon；
- dimension transfer；
- reconstruction / reinsert；
- hostile direct add path；
- unresolved identity；
- Root unavailable / stale projection。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l1242 -->

<a id="orig-v26-l0325"></a>

**迁入原文｜V26｜旧 L325–L340** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0325)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 3. 正式术语 → 3.6 Existence Termination / 存在性终结

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0325 -->
#### 3.6 Existence Termination / 存在性终结

Extreme 清除内部语义继续为 `Existence Termination`。

它不等于：

```text
entity.remove()
```

而是：

> 撤销当前 Judgment 所辖目标继续存在、恢复、重建和重新发布的有效资格。

`CLEARED`、`PARTIAL` 属于 attackOutcome；`FENCED` 属于 executionState，并由 fenceScope 指明范围。它们均不替代 Judgment Truth，完整定义见 [§6.5](MS-MSITEM-PLAN.md#orig-v26-l0675)。


<!-- RONOVA-ORIGINAL-END orig-v26-l0325 -->

<a id="orig-v26-l1273"></a>

**迁入原文｜V26｜旧 L1273–L1274** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1273)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 9. 攻击平面

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1273 -->
#### 9. 攻击平面


<!-- RONOVA-ORIGINAL-END orig-v26-l1273 -->

<a id="orig-v26-l1506"></a>

**迁入原文｜V26｜旧 L1506–L1545** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1506)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 9. 攻击平面 → 9.4 极限清除 / Existence Termination

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1506 -->
#### 9.4 极限清除 / Existence Termination

九项完成条件继续保留：

1. 当前身体不能作为有效目标执行；
2. 世界实体记录不能恢复旧资格；
3. 旧 TargetEpoch 不可重插；
4. 恢复任务失效；
5. 重建入口失效；
6. save 不能恢复旧资格；
7. tracking 不能重新发布；
8. Presence/HUD 不能重新建立；
9. 稳定窗口内无未决关键义务和漂移。

只有按 [§9.4.1](MS-MSITEM-PLAN.md#orig-v26-l1546) 预先确定的合同适用范围，九项条件均获满足或具有合法 N/A 证明，且不存在 required 缺口，才能报告：

```text
attackOutcome = CLEARED
```

否则：

```text
PARTIAL
```

证据/执行能力失效：

```text
attackOutcome = PARTIAL
executionState = FENCED
fenceScope = 最小必要范围
```

CLEARED 还要求当前 Judgment 已确定提交、必需证据 revision 有效，且不存在尚未受控的关键范围汇总缺口。仅降低事件数量、清空样本、完成一次压缩、取消 Future、停止 Ronova 授权或通过短暂无漂移窗口，都不能独立满足完成条件。

若 manager 仍然每 20 tick 把目标恢复为 PRESENT，即使 Ronova 已拒绝相关授权，attackOutcome 仍为 PARTIAL。只有恢复链被实际控制、最终 state 收敛、九项条件全部满足并通过稳定窗口，才能转为 CLEARED。

完成后出现新恢复边、目标再次 PRESENT、关键证据失效或汇总缺口重新打开时：保留原回执的历史事实，推进 outcomeRevision，撤销证书的当前有效性，当前结果回到 PARTIAL 并重新收敛；不因此撤销既有 Judgment。非关键、无关范围的变化不触发全局回退。


<!-- RONOVA-ORIGINAL-END orig-v26-l1506 -->

<a id="orig-v26-l1569"></a>

**迁入原文｜V26｜旧 L1569–L1587** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1569)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 9. 攻击平面 → 9.4.2 通用恢复链处置（2026-09-08 规划增量）

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1569 -->
#### 9.4.2 通用恢复链处置（2026-09-08 规划增量）

实体终结的生产纵向链新增明确目标：在同一已提交 command、主体资格和 Root 批准计划下，
处理已证明属于该目标的恢复记录、快照、任务、回调、重建入口及其相关资源。
实施细节见 [7.17 通用恢复链处置规划 V1.0](RESTRUCTURE-MAP.md#source-rec)。

此功能按实际对象、数据消费和调度因果识别来源，不含第三方 Boss、Mod ID、包名、类名或文件名特判。
现有 Tombstone/Admission 负责拒绝旧资格，新增来源处置负责让已证明的恢复路径实际失效；
仅实体消失、入口拒绝或 `Future.cancel()` 成功，不等于来源和全部后继已完成。

来源处置服从 [§6.7](MS-MSITEM-PLAN.md#orig-v26-l0884) 普通动作与高级键的权限交集、[§9.2](MS-MSITEM-PLAN.md#orig-v26-l1327) 候选/证明区分、
[§12.6](MS-MSITEM-PLAN.md#orig-v26-l2182) 稳定 effectKey 和效果未知恢复、[§13](MS-SKILLS-PLAN.md#orig-v26-l2224) 最终提交门以及 [§20](MS-SKILLS-PLAN.md#orig-v26-l3104) 的预算与持久化边界。
内存记录移除与保存成功分别证明；不得清空共享容器、删除整个 SavedData 或离线破坏第三方文件。
明确 `clear_uuid` / `clear_tombstones` 仍按原义解除裁决，不自动重建已经处置的外部记录或重放旧副作用。

本项属于 7.17 B/D、C/E 的优先实施链，F/G 消费其状态和完成证据。
当前状态为规划，尚未完成生产接线，不增加攻击按钮或 Authority writer，
不以本段文档新增宣告恢复链、7.17、FULL L5 或 L6E 已完成。


<!-- RONOVA-ORIGINAL-END orig-v26-l1569 -->

<a id="orig-v26-l1697"></a>

**迁入原文｜V26｜旧 L1697–L1717** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1697)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 9. 攻击平面 → 9.6 全权威结果闭包

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1697 -->
#### 9.6 全权威结果闭包

Extreme：

> **目标结果图内，不允许未经当前 Judgment 授权的成功完成形成有效结果。**

覆盖：

- normal return；
- fixed/early return；
- throw/catch/finally；
- timeout/cancel；
- callback/Future；
- scheduled continuation；
- event；
- packet response；
- reflection/proxy；
- MethodHandle/CallSite；
- native callback；
- 延迟到下一 tick / 另一线程的恢复后继。


<!-- RONOVA-ORIGINAL-END orig-v26-l1697 -->

<a id="orig-tier-l0656"></a>

**迁入原文｜TIER｜旧 L656–L691** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-tier-l0656)

原标题路径：第二部：加强路线（第一部 P0–P4 完成之后） → 12. Q3 —— ExistenceTerminationCertificate

<!-- RONOVA-ORIGINAL-BEGIN orig-tier-l0656 -->
#### 12. Q3 —— ExistenceTerminationCertificate

**入口**：Q2 完成。

**规范边界**（[V2.6:352-357](MS-SKILLS-PLAN.md#orig-v26-l0341) 原文）

ExistenceTerminationCertificate：

- 只是"在某一证据 revision / 会话 / policy / result graph 下完成条件已满足"的证明；
- **不能直接成为 `denySpawn()` 的 Authority**；
- Admission 必须继续依据 Root Truth + Tombstone + TargetEpoch + policy；
- **Certificate 失效只代表"完成证明失效"，不自动回滚已提交 Judgment**。

**已有零件与缺口（可量化）**

`runtime/CompletionTracker` 的 9 项条件即规范所称"Existence Termination 九项条件"：

| 条件 | 生产者 |
|---|---|
| `BODY_DISABLED` | ✅ 有 |
| `SAVE_QUALIFICATION` | ✅ 有 |
| `NO_PENDING_WORK` | ✅ 有 |
| `WORLD_RECORDS` | ❌ **零引用** |
| `OLD_EPOCH_REJECTED` | ❌ **零引用** |
| `RECOVERY_TASKS` | ❌ **零引用** |
| `REBUILD_ENTRYPOINTS` | ❌ **零引用** |
| `TRACKING` | ❌ **零引用** |
| `SERVER_PRESENCE` | ❌ **零引用** |

⇒ **本阶段的实际工作 = 为那 6 项补生产者**，并把 `CompletionEvaluation` 持久化为证书。

**验收**：九项条件各自有可复核的生产者；证书带证据 revision / 会话 / policy / 图 revision；
证书失效不触碰已提交 Judgment；证书**不**被任何 Admission 路径直接消费。

---


<!-- RONOVA-ORIGINAL-END orig-tier-l0656 -->

<a id="orig-rec-l0045"></a>

**迁入原文｜REC｜旧 L45–L63** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0045)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 1. 要解决的行为

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0045 -->
#### 1. 要解决的行为

某些目标的实体只是身体，持续存在的 manager、SavedData 记录、快照、任务或回调
还保存着“缺失时重建它”的状态。仅移除身体会留下这条恢复路径；
仅用 Tombstone 拒绝重新加入，也不能证明恢复源已经消失。

本功能要求区分三个结果：

| 结果 | 必须实际发生什么 |
|---|---|
| 身体已清除 | 当前物理对象及适用世界记录已按原终结合同收敛 |
| 重建被阻止 | 旧资格在真实构造、准入或发布入口被拒绝；来源可能仍在尝试 |
| 已知恢复链已处置 | 已确认来源中的目标记录、任务和重建边已处置，并有当前效果证据 |

“同时”指同一 command、目标资格和执行计划内的协同处理：
已知恢复入口尽可能在死亡/移除回调前限制，记录与身体一起收敛；
回调中才暴露的新来源继续由当前 gate 约束并进入后续预算窗口。
不承诺任意长度的恢复链在同一 tick 内瞬时全部清除，也不为发现来源而放行一次复活。


<!-- RONOVA-ORIGINAL-END orig-rec-l0045 -->

<a id="orig-rec-l0196"></a>

**迁入原文｜REC｜旧 L196–L197** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0196)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 6. 数据、任务和资源的真实处置

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0196 -->
#### 6. 数据、任务和资源的真实处置


<!-- RONOVA-ORIGINAL-END orig-rec-l0196 -->

<a id="orig-rec-l0198"></a>

**迁入原文｜REC｜旧 L198–L211** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0198)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 6. 数据、任务和资源的真实处置 → 6.1 精确恢复记录

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0198 -->
#### 6.1 精确恢复记录

执行前重新确认存储 owner、容器、key、当前 value 的对象身份及版本，
并确认该条目实际被本次恢复路径消费。优先撤销目标子记录的恢复资格；
只有记录语义可验证且无其他合法用途时，才执行精确移除。

写入必须使用登记的 WriteSink 或等价原子提交门。
“在 server thread 上”不自动证明没有后台线程竞争；
没有共同临界区、CAS、版本化更新或可证明单 writer 的位置，不允许先检查再盲写。
持有锁时不调用未知外部方法，锁竞争采用有界退出并保留待办。

不清空共享 Map、不删除整个 SavedData、不离线搜索并删改第三方存档文件。
文件名、字段名和包名只可用于诊断/访问位置，不成为目标归属规则。


<!-- RONOVA-ORIGINAL-END orig-rec-l0198 -->

<a id="orig-rec-l0381"></a>

**迁入原文｜REC｜旧 L381–L388** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0381)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 12. 参考 Mod 研究转入第一阶段

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0381 -->
#### 12. 参考 Mod 研究转入第一阶段

研究来源、M01–M14 总清单及跨阶段验收统一见
[极限档全体系加强规划](RESTRUCTURE-MAP.md#source-full)第 8–9 节。
本节把其中恢复链必需的内容转为可实施要求，沿用既有三档、14 个普通动作、54 个高级键、
Internal Root 和 EffectJournal，不增加另一份权威状态或新的“强力总开关”。
条目均为新增设计/验收要求，现有子范围通过保留，尚无新增整链通过结论。


<!-- RONOVA-ORIGINAL-END orig-rec-l0381 -->

<a id="orig-max-l0226"></a>

**迁入原文｜MAX｜旧 L226–L227** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-max-l0226)

原标题路径：Ronova 合规极限加强方案 V1.0 → 5. L5 执行深度：候选图 + 跨表面追踪 + 完成证书

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-max-l0226 -->
#### 5. L5 执行深度：候选图 + 跨表面追踪 + 完成证书


<!-- RONOVA-ORIGINAL-END orig-max-l0226 -->

<a id="orig-max-l0228"></a>

**迁入原文｜MAX｜旧 L228–L238** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-max-l0228)

原标题路径：Ronova 合规极限加强方案 V1.0 → 5. L5 执行深度：候选图 + 跨表面追踪 + 完成证书 → 5.1 现状

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-max-l0228 -->
#### 5.1 现状

`TerminalActionPolicy` 的天花板是 `REGISTERED_ENTITY_ONLY`（单个已登记实体 + 精确索引引用）。
而 [V2.6 §4.1](MS-MSITEM-PLAN.md#orig-v26-l0363) 定义的 EXTREME 范围是：

> 当前 Judgment → **所有可能改变其最终结果的同 JVM 候选路径** → 已证明源 / 链 / 代码 /
> dispatch / task / callback / rebuild / publish → 最终完成与状态出口

**点是点，图是图。** 且 [V2.6:90](MS-SKILLS-PLAN.md#orig-v26-l0037) 明文"完整 ResultGraph、MutationTrace、
ExistenceTerminationCertificate 生产 schema 与 executor **从 7.17 起实现**"。


<!-- RONOVA-ORIGINAL-END orig-max-l0228 -->

<a id="orig-max-l0239"></a>

**迁入原文｜MAX｜旧 L239–L247** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-max-l0239)

原标题路径：Ronova 合规极限加强方案 V1.0 → 5. L5 执行深度：候选图 + 跨表面追踪 + 完成证书 → 5.2 交付物

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-max-l0239 -->
#### 5.2 交付物

| 交付物 | 内容 | 依据 |
|---|---|---|
| 候选一等公民 | 统一候选登记 + 置信度模型（`CANDIDATE → PROVEN_RELATED`）；复用 `obligation/CandidateEvidence`（8 family / 3 strong）与 `CandidateRestrictions` | [V2.6:1170](MS-SKILLS-PLAN.md#orig-v26-l1154) |
| `MutationTrace` | 五段管线：状态变化 → before/after → 关联 caller/task/callback/manager/dispatch/loader/native → 有界证据 → 供图使用 | [V2.6:1158-1171](MS-SKILLS-PLAN.md#orig-v26-l1154) |
| `ResultGraph` | 候选关系图；分片 + 持久索引 + 冷热分离；**语义零截断**（预算只限每窗口处理量） | [V2.6:343-350](MS-SKILLS-PLAN.md#orig-v26-l0341)、:404-415 |
| `ExistenceTerminationCertificate` | **九项条件补齐**（现有 3 项有生产者，6 项零引用：`WORLD_RECORDS`/`OLD_EPOCH_REJECTED`/`RECOVERY_TASKS`/`REBUILD_ENTRYPOINTS`/`TRACKING`/`SERVER_PRESENCE`） | `CompletionTracker` + [V2.6:352-357](MS-SKILLS-PLAN.md#orig-v26-l0341) |


<!-- RONOVA-ORIGINAL-END orig-max-l0239 -->

<a id="orig-max-l0248"></a>

**迁入原文｜MAX｜旧 L248–L262** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-max-l0248)

原标题路径：Ronova 合规极限加强方案 V1.0 → 5. L5 执行深度：候选图 + 跨表面追踪 + 完成证书 → 5.3 样本反哺：必须能击败的具体恢复路径

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-max-l0248 -->
#### 5.3 样本反哺：必须能击败的具体恢复路径

SuperSteve 6.9.1/6.9.13f 与永爱 2.6 揭示了**真实的恢复链形态**，图必须能承载它们：

| 恢复路径 | 样本依据 | 图要求 |
|---|---|---|
| 静态并发表按 UUID 保留服务端/客户端实例 | SuperSteve `api/SSUtil.java:217-218` | 候选节点跨端绑定 |
| 两条裸线程每 ~10ms 重复维护 | `power/SSThread.java:34-114` | 任务域候选 + 共享线程不整体停 |
| 复制 section/UUID/ID/active/tracking 后**替换字段** | `api/SSUtil.java:398-450` | 容器级候选 + 迟到发布检测 |
| 同 UUID 新实例进入 → 重挂旧实例、移除新实例 | `entity/SuperSteveEntity.java:583-613` | lineage 节点 + 旧指针复用检测 |
| 隐藏类反射构造实体 | `init/SSModEntities.java:43-56` | loader/定义类血缘边 |
| 内存 Java 编译再定义隐藏类 | `power/HotCplr.java:55-81` | 动态代码节点 |
| 对象类指针改写 + 无构造分配 + 清反射缓存 | `api/PLZBase.java:129-146` | 字段级候选 |
| Native 解密后 `DefineClass` | himitsu PE | 原生来源边 |


<!-- RONOVA-ORIGINAL-END orig-max-l0248 -->

<a id="orig-max-l0263"></a>

**迁入原文｜MAX｜旧 L263–L269** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-max-l0263)

原标题路径：Ronova 合规极限加强方案 V1.0 → 5. L5 执行深度：候选图 + 跨表面追踪 + 完成证书 → 5.4 合规性论证

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-max-l0263 -->
#### 5.4 合规性论证

`ResultGraph` 与 `CandidateRestrictions` 只作用于**与当前 Judgment 有实际关系**的路径，
判定依据是**证据**（复制来源、调用栈帧、任务关系），不是名称（V2 不触碰）。
规范亦明确其边界：**不能创造 Judgment、不能单独撤销 Tombstone、不能自己推进 TargetEpoch**
（[V2.6:347-349](MS-SKILLS-PLAN.md#orig-v26-l0341)）——即它不扩大 Authority，只扩大**可证范围**。


<!-- RONOVA-ORIGINAL-END orig-max-l0263 -->

<a id="orig-p0a-l0131"></a>

**迁入原文｜P0A｜旧 L131–L151** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0131)

原标题路径：2. P0-A 核心不变量 → A4 — Tombstone Dominance

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0131 -->
#### A4 — Tombstone Dominance

若 Root Truth：

```text
Subject X = TERMINAL
```

则：

```text
new incarnation
rebind
reload
reconstruct
```

都不能使 Subject 自动恢复 ACTIVE。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0131 -->

<a id="orig-p0a-l0650"></a>

**迁入原文｜P0A｜旧 L650–L674** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0650)

原标题路径：11. TombstoneRecord

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0650 -->
#### 11. TombstoneRecord

建议：

```java
public record TombstoneRecord(
        SubjectKey subjectKey,
        DominionEpoch epoch,
        RootCommitIndex commitIndex,
        String reasonCode,
        long createdAtSequence,
        BindingRevision bindingRevision
) {}
```

要求：

- durable；
- monotonic；
- 不因 Entity 不存在而删除；
- ClearTombstone 只能显式 Root command；
- 普通 rebind 不得清 Tombstone。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0650 -->

<a id="orig-p0a-l1070"></a>

**迁入原文｜P0A｜旧 L1070–L1095** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1070)

原标题路径：25. Terminal-aware Binding

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1070 -->
#### 25. Terminal-aware Binding

若：

```text
Root lifecycle = TERMINAL
```

新发现 incarnation：

```text
不得 ACTIVE bind
```

结果：

```text
TERMINAL_REJECT(CANCELLED)
+ AuthoritySnapshot(lifecycleTruth=TERMINAL)
+ P0-C reconciliation request
```

P0-A 只负责 truth / binding 结论，真正 physical removal 由 P0-C 执行。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l1070 -->

<a id="orig-l6-l0627"></a>

**迁入原文｜L6｜旧 L627–L628** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-l6-l0627)

原标题路径：Ronova L6 Architecture Master Plan V2.4 — Internal Root → 6. Tombstone / Terminal Enforcement 重设计（V2 修订：C2 语义/存储分离、D5 终结拆分）

<!-- RONOVA-ORIGINAL-BEGIN orig-l6-l0627 -->
#### 6. Tombstone / Terminal Enforcement 重设计（V2 修订：C2 语义/存储分离、D5 终结拆分）


<!-- RONOVA-ORIGINAL-END orig-l6-l0627 -->

<a id="orig-l6-l0629"></a>

**迁入原文｜L6｜旧 L629–L636** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-l6-l0629)

原标题路径：Ronova L6 Architecture Master Plan V2.4 — Internal Root → 6. Tombstone / Terminal Enforcement 重设计（V2 修订：C2 语义/存储分离、D5 终结拆分） → 6.1 问题回顾（CONFIRMED）

<!-- RONOVA-ORIGINAL-BEGIN orig-l6-l0629 -->
#### 6.1 问题回顾（CONFIRMED）

7.13 把两件本应独立的事耦合在一起：
- **"避免重复 death/loot"** 由 `TerminalLifecycle` firstExecution 门保证（正确，`:36,41`）。
- **"以后无需再 physical detach"** 被错误地绑到 `JudgmentEngine` 的 6 次有限 retry（`RETRY_DELAYS`，`:28,171`）——用完就放弃。

同时权威身份用弱引用（`JudgmentState.EXECUTED = WeakIdentitySet<Entity>`，`:16`），reload 重建实体后丢失，只能靠 `QuarantineView.matchesDetachedLineage` 兜底。此外 `TerminalLifecycle.enforce` 主动 `TopologyIndex.forget(entity)`（`:28`）——terminal 层反向抹除 raw 观测认知，违反 D1/D5。


<!-- RONOVA-ORIGINAL-END orig-l6-l0629 -->

<a id="orig-l6-l0637"></a>

**迁入原文｜L6｜旧 L637–L662** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-l6-l0637)

原标题路径：Ronova L6 Architecture Master Plan V2.4 — Internal Root → 6. Tombstone / Terminal Enforcement 重设计（V2 修订：C2 语义/存储分离、D5 终结拆分） → 6.2 新模型：无限逻辑寿命 + 有界物理工作（V2 按 D5 术语拆分）

<!-- RONOVA-ORIGINAL-BEGIN orig-l6-l0637 -->
#### 6.2 新模型：无限逻辑寿命 + 有界物理工作（V2 按 D5 术语拆分）

将"逻辑判决"与"物理执行"彻底分离，物理执行再拆两个语义：

```
Committed Tombstone (durable, UUID+lineage, 无限期, 语义不压缩)
│
├── Lifecycle Integration ── ONCE（幂等一次）
│     ├── 生命周期事件衔接（death callback 等）
│     ├── loot / effect 集成
│     └── 记 LifecycleIntegrationReceipt{judgment_id, first_physical_identity, integrated_epoch}
│
└── State Reassertion ── REPEATABLE / IDEMPOTENT（无限次幂等）
      ├── removal state（discard / remove）
      ├── entity-manager detach
      ├── tracking detach
      ├── raw terminal 字段断言（health/removalReason/callback）
      └── quarantine/presence 投影更新
```

**Terminal 与 Raw Observation 的关系（D5，V2 明确）**：State Reassertion 的目标是"物理 incarnation 不重新恢复为有效状态"，它需要**持续看见**该对象——因此 terminal 层**不得**调用任何"让 raw 观测遗忘对象"的操作。现有 `TopologyIndex.forget(entity)`（`TerminalLifecycle.java:28`）与 `ExecutionTargetPolicy.shouldTrack` 的 `!isJudged` 条件（`:31`）均在迁移中移除（[§10.2](MS-SKILLS-PLAN.md#orig-l6-l0960)）。已判决对象在 raw 观测中的可见性与普通对象相同；它对**外部**不可见由 presence 负责，与 raw 无关。

**逻辑寿命判据**：`TerminalWatchSet` 的成员**不以重试次数结束**，而以 **authoritative tombstone 是否仍存在**决定。tombstone 只在 owner 显式 `ClearTombstone`/`ClearTombstones` 命令下消失。

**物理 incarnation 处理**：对每个物理对象用弱引用 + identity 记录。对象连续 `K` 个 raw-observation 周期确实不存在 → 物理 watch 转 COLD 休眠（不保留强引用），但 **lineage/identity tombstone 永久保留**。一旦 Admission 或 raw observation 再次发现该 lineage 的物理 incarnation，立即重新升为 HOT。（COLD→HOT 的 raw 再发现路径因 D5 的 forget 移除而保持可用——这正是 V1 的自相矛盾处，V2 已闭合。）


<!-- RONOVA-ORIGINAL-END orig-l6-l0637 -->

<a id="orig-l6-l0685"></a>

**迁入原文｜L6｜旧 L685–L706** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-l6-l0685)

原标题路径：Ronova L6 Architecture Master Plan V2.4 — Internal Root → 6. Tombstone / Terminal Enforcement 重设计（V2 修订：C2 语义/存储分离、D5 终结拆分） → 6.4 对象重建 / reload 的权威性 + 容量策略（V2 重写，关闭 C2）

<!-- RONOVA-ORIGINAL-BEGIN orig-l6-l0685 -->
#### 6.4 对象重建 / reload 的权威性 + 容量策略（V2 重写，关闭 C2）

| 场景 | 7.13 行为（CONFIRMED） | 新设计 |
|---|---|---|
| chunk reload 重建实体 | `JudgmentState` 弱身份丢失，靠 lineage 兜底 | `TombstoneStore` UUID+lineage 权威匹配，Admission 阶段即拦截 |
| world/dimension reload | tombstone SavedData 恢复（已正确） | 保持；`TerminalWatchSet` 从 `TombstoneStore` 重建 |
| 对象 UUID 变化重生 | **判决时**按 scope 决定（IDENTITY/LOCAL_LINEAGE/GLOBAL_LINEAGE，`LineageIndex` KEEP） | 保持——scope 是**判决时语义**，不是事后容量手段 |
| 容量超限 | **FIFO 静默丢弃最老（问题）** | **物理表示优化，语义零变化**（下述） |

**容量策略（V2，替代 V1 的"lineage 压缩"）**：

V1 曾提出"长期未命中的精确记录压缩为 class-lineage 规则"。**该方案废弃**（Review C2）：源码证明它会同时扩大命中集合（`LineageIndex.matches` 的 GLOBAL 分支只按 type 命中，`LineageIndex.java:74-76`）并破坏撤销（`clearTombstone(uuid)` 以 UUID 为键，`DominionLedger.java:216-228`，折叠后静默 no-op）——即语义压缩，违反 D2。

V2 的容量控制**只发生在物理表示层**，Judgment 匹配语义零变化：

- **允许**：紧凑二进制表示（复用 AXR3 经验）、分段存储（hot segment 内存驻留 + cold segment 磁盘按需加载）、UUID 索引 + 空间索引分离、checkpoint 合并、数据库层 compaction。
- **强制等价**：任何物理优化前后，(a) 对任意查询的命中结果逐条相同（不扩大、不缩小）；(b) `ClearTombstone(uuid)` 对任意记录可命中并使其永久消失；(c) IDENTITY/精确 judgment 的 UUID 语义不变。该等价性是 L5-INV-2 的 PASS 判据之一，纳入 [§12.1](MS-SKILLS-PLAN.md#orig-l6-l1148) 测试矩阵（compaction 语义等价测试）。
- **内存驻留集**：常驻内存的只需 admission 判定所需的索引（UUID 集 + lineage 空间索引），全量记录体可冷存——索引本身不改变命中语义，只改变查找路径。
- 逻辑判决数量本身**不设上限**。存储增长的工程压力由表示层吸收，不由语义层吸收。

---


<!-- RONOVA-ORIGINAL-END orig-l6-l0685 -->

<a id="orig-ds-l0065"></a>

**迁入原文｜DS｜旧 L65–L74** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0065)

原标题路径：Ronova 设计规范 · 合并导读 → 2. 冻结词汇（`CONTEXT.md`） → 2.2 判定与终结语义

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0065 -->
#### 2.2 判定与终结语义

| 术语 | 定义 | 禁止混用 |
|---|---|---|
| **Judgment Eligibility** | raw-observed 化身是否**可以**进入 Judgment 关闭。是 Raw Observation **下游**的策略决定，**不是索引属性** | trackability / observability |
| **Tombstone** | 「身份/lineage 已被判定终结」的权威持久事实。**逻辑寿命只通过显式 clear 结束**，绝不仅因化身消失或重试用尽结束 | watch entry / retry record / weak reference |
| **Lifecycle Integration** | 把一次 Judgment **一次性**并入正常生命周期效果（如死亡整合） | terminal retry / continuous enforcement |
| **State Reassertion** | 可重复且幂等地断言化身仍符合终结真相。**重复不得重复一次性 Lifecycle Integration 效果** | re-judgment / repeated death |
| **Terminal Watch** | 对匹配 Tombstone 真相的化身的运行时感知，使 State Reassertion **有界且可恢复**。**永不创造** Judgment 真相，**永不决定** Public Presence | tombstone store / retry queue / judgment authority |


<!-- RONOVA-ORIGINAL-END orig-ds-l0065 -->

<a id="ms-msitem-l255"></a>

### 混合模式

**框架来源：** [MS msitem，原 L255–L260](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#混合模式)。

**编排说明（非原条款）：** 共享结果策略与一次性效果合同，而不是每个入口各试一遍死亡和清除。现有跨文档技术差异保留，不在重构时暗中重写。

**关联原条款：** [MSC · 1.4 目标与结果策略](MS-MSITEM-PLAN.md#orig-msc-l0069)；[CF · 16. Side-Effect Contract](MS-MSITEM-PLAN.md#orig-cf-l1011)。

本节点没有被单独搬入的原文段落；它保留 MS 的独立职责入口。上列关联条款或下级章节不等于本节点所有细节已经覆盖；适用性与缺口见映射表。

<a id="ms-msitem-l261"></a>

## 服务端队列与线程边界

**框架来源：** [MS msitem，原 L261–L285](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#服务端队列与线程边界)。

**编排说明（非原条款）：** 全部实际执行、快照后复验、调度预算和回调任务按原合同承接。段落中的源码落点和测试记录是原文件当时陈述。

<a id="orig-bl-l0794"></a>

**迁入原文｜BL｜旧 L794–L835** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0794)

原标题路径：8. Enforcement Plane

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0794 -->
#### 8. Enforcement Plane

所有持续能力统一为：

```text
Immutable Projection
      ↓
Desired State
      ↓
State Observation
      ↓
Invariant Evaluation
      ↓
Deviation
      ↓
Bounded Reassertion
```

共享组件可以包括：

```text
DesiredStateView
StateObserver
InvariantEvaluator
ReassertionScheduler
EnforcementBudget
```

Domain：

- Health；
- Lifecycle；
- Movement；
- Equipment；
- Effect。

硬规则：

> **Domain 只执行 projection，不能自己创造 Judgment / Root Truth。**

---


<!-- RONOVA-ORIGINAL-END orig-bl-l0794 -->

<a id="orig-bl-l0836"></a>

**迁入原文｜BL｜旧 L836–L873** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0836)

原标题路径：8. Enforcement Plane → 8.3 Phase-aware Enforcement / Tick-Level Control

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0836 -->
#### 8.3 Phase-aware Enforcement / Tick-Level Control

此前讨论过的 Tick-Level Control 不应升级成“每个实体每 tick 做一次完整检查”，而应收敛为：

> **只有对瞬时安全敏感的 invariant，才在明确的执行阶段设置 critical phase seam。**

可表达的 phase 示例：

```text
PRE_ENTITY_TICK
PRE_AI
PRE_INTERACTION
PRE_CRITICAL_MUTATION
POST_CRITICAL_MUTATION
END_TICK_RECONCILE
```

不是所有 Domain 都需要所有 phase。

例如：

- `EXACT_HEALTH` 一般可由 observation + convergence 处理；
- “health 绝不能跨过死亡阈值”属于 transition-sensitive invariant，可能需要更早的 critical mutation / lifecycle seam；
- TERMINAL Admission 应优先在 admission / lifecycle seam 判定，而不是等 end-tick 才发现。

Phase seam 本身必须进入 Integrity Critical Set：

```text
critical phase seam
→ Structural Proof
→ Semantic Proof
→ Behavioral Proof
```

因此 Tick-Level Control 是 **Execution / Convergence 的时序实现细节**，不是新 Authority Plane。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l0836 -->

<a id="orig-bl-l1286"></a>

**迁入原文｜BL｜旧 L1286–L1289** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l1286)

原标题路径：12. Execution Scheduling + Feedback

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l1286 -->
#### 12. Execution Scheduling + Feedback

这部分不是 AllReturn。


<!-- RONOVA-ORIGINAL-END orig-bl-l1286 -->

<a id="orig-bl-l1290"></a>

**迁入原文｜BL｜旧 L1290–L1310** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l1290)

原标题路径：12. Execution Scheduling + Feedback → 12.1 Scheduling

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l1290 -->
#### 12.1 Scheduling

Root / projection 可以决定：

```text
subject
constraint
domain
priority
budget
check cadence
```

形成：

```text
EnforcementSchedule
```

---


<!-- RONOVA-ORIGINAL-END orig-bl-l1290 -->

<a id="orig-bl-l1311"></a>

**迁入原文｜BL｜旧 L1311–L1346** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l1311)

原标题路径：12. Execution Scheduling + Feedback → 12.2 Feedback

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l1311 -->
#### 12.2 Feedback

Domain 执行后产生：

```text
ExecutionReceipt
├ SUCCESS
├ PARTIAL
├ FAILED
└ CONFLICT
```

但不能每 tick 每个 SUCCESS 都同步执行 durable Root mutation/I/O。

正确：

```text
Local receipts
 ↓
coalesce / aggregate
 ↓
state transition / anomaly / summary
 ↓
Root
```

高优先级事件：

- FAILED；
- CONFLICT；
- repeated PARTIAL；
- ownership violation；
- integrity incident。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l1311 -->

<a id="orig-bl-l1347"></a>

**迁入原文｜BL｜旧 L1347–L1365** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l1347)

原标题路径：12. Execution Scheduling + Feedback → 12.3 Domain 不得自行升级 Authority

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l1347 -->
#### 12.3 Domain 不得自行升级 Authority

禁止：

```text
HealthDomain 修不动
→ 自己切 TERMINAL
```

只能：

```text
FAILED evidence
→ Root Policy
→ 新的明确 Judgment（如被授权）
```

---


<!-- RONOVA-ORIGINAL-END orig-bl-l1347 -->

<a id="orig-v26-l2134"></a>

**迁入原文｜V26｜旧 L2134–L2181** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l2134)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 12. 运行时与宿主安全 → 12.5 Cross-surface Operation Plan

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l2134 -->
#### 12.5 Cross-surface Operation Plan

同一结果可能同时经过多个表面。例如目标被 Root 判定 TERMINAL 后，Temporal 任务尝试重建，Registry 重新登记，Data 写回状态，Network 重发投影，Presentation 又显示 HUD。V2.5 禁止五个 executor 各自“看见就修”，必须先由 Root 根据同一 command/revision 生成一份跨表面计划。

最低 schema：

```text
CrossSurfaceOperationPlan {
  planId / planRevision
  canonicalCommandId / authorityReceiptRef
  RealmId / ownerIdentity
  lineageId / targetEpoch / judgmentRevision
  operationType / completionContractRef
  requiredSurfaces[] / optionalSurfaces[]
  orderedSteps[]
  perStepLease / idempotencyKey / retryPolicy
  resourceReservations[]
  rollbackOrCompensationPolicy
  completionDependencies[]
  currentOutcomeRevision
}
```

编排规则：

1. Root 只批准计划和资格变化；Surface executor 只执行自己的 typed step 并返回 receipt。
2. 同一 `planId + stepId + revision` 是调度去重身份；副作用还必须使用 [§12.6](MS-MSITEM-PLAN.md#orig-v26-l2182) 的稳定 effectKey，跨 planRevision/restart 不得重复非幂等改写。Root COMMIT_UNKNOWN 与 step 效果未知分别解析。
3. 默认顺序优先阻断未来资格与重建入口，再收敛 Data/Registry，随后处理 Network/Presentation，最后进行跨表面 completion；实际顺序由 operation contract 指定，不能写死为所有操作相同。
4. 任何 step 发现 epoch、policy、dispatch、evidence 或 loader revision 变化，必须失效旧 lease 并回 Root 重解析，不能在表面内部续租。
5. 两个表面提出冲突动作时，Root 按 Authority Truth、当前 Judgment、最小必要 scope、宿主安全和 completion dependency 仲裁；“最后一个 callback 赢”不是合法策略。
6. repair/recovery 产生的新 observation 带 causality token；同 plan 的预期反馈不得重新生成无限攻击任务。真正的新漂移使用新 revision 进入调度。
7. required surface 未达到合同成熟度时，计划只能是 PARTIAL/FENCED/UNAVAILABLE；optional surface 缺失是否阻止完成由 CompletionContract 明确，不能由 GUI 猜测。
8. 客户端 step 只返回 projection/input receipt；任何客户端成功都不能替代服务端 Root receipt。

典型清除计划：

| 阶段 | 主要 Surface | 成功证据 |
|---|---|---|
| 资格提交 | Root（非 Surface） | 原子 Judgment/TargetEpoch/revoke/Admission receipt |
| 阻断重建 | Temporal、Registry、Network | 旧 epoch task/registration/packet 的拒绝或隔离回执 |
| 状态与记录收敛 | Data、Registry | 状态、ID/UUID、section、tracking 等 required sink 的效果证据 |
| 非实体与显示收敛 | Resource、Presentation、Network | HUD/presence/resource projection 不再表达旧结果 |
| 稳定与重验 | 全部 required Surface | CompletionContract 条件、稳定窗口、current evidence revision |

CrossSurfaceOperationPlan、step receipt 和 outcome projection 都是派生执行事实，不是新的 durable Truth writer。持久化只保存重启所需的 canonical command、未决 obligation、最小计划恢复信息和权威 receipt；不得把全部 raw event 或运行时对象图无界保存。

---


<!-- RONOVA-ORIGINAL-END orig-v26-l2134 -->

<a id="orig-v26-l2268"></a>

**迁入原文｜V26｜旧 L2268–L2285** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l2268)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 13. Evidence / Lease / TOCTOU → 13.3 三次复验 + 真实提交门

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l2268 -->
#### 13.3 三次复验 + 真实提交门

```text
签发前
提交前
完成后
```

只是 evidence check，不是天然原子性。

执行器还必须提供：

- 临界区；
- 原子版本判定；
- versioned commit；
- CAS / equivalent gate；
- 或其他能够证明“检查到提交之间没有未经授权插入”的协议。


<!-- RONOVA-ORIGINAL-END orig-v26-l2268 -->

<a id="orig-rec-l0180"></a>

**迁入原文｜REC｜旧 L180–L195** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0180)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 5. 同一 Root 下的执行顺序

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0180 -->
#### 5. 同一 Root 下的执行顺序

1. 用原 canonical command/receipt 确认终结已提交，读取当前完整主体资格和政策。
2. 保持旧资格拒绝；对已证明来源批准目标范围的限制和待处理义务。
3. 在生命周期回调前处理已经具备有效许可与安全执行门的已知恢复入口。
4. 按原 ONCE 合同执行生命周期，独立完成身体状态和世界记录收敛。
5. 处理已证明的恢复条目、任务与注册边；新增发现合并回同一目标的待办。
6. 查询各步骤实际效果，确认保存状态，收敛 tracking/Presence 等适用出口。
7. 在同一完成合同下观察稳定窗口；新增恢复边使当前完成证明失效并继续处理。

未知来源不阻止已获许可的身体清除，也不能因为身体已消失而丢弃。
资格提交与物理执行仍分开；Root 的事务临界区不调用第三方对象或回调。

终结计划增加来源步骤时升其内部合同版本，不借改名重放旧生命周期 effect。
旧 command、历史 hash、Tombstone 和 ONCE slot 保持原义。


<!-- RONOVA-ORIGINAL-END orig-rec-l0180 -->

<a id="orig-rec-l0223"></a>

**迁入原文｜REC｜旧 L223–L232** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0223)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 6. 数据、任务和资源的真实处置 → 6.3 延迟任务与回调

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0223 -->
#### 6.3 延迟任务与回调

至少区分排队、运行、取消请求/接受、实际退出、后继未结和结果未知，具体按[第 12.2 节](MS-MSITEM-PLAN.md#orig-rec-l0407)结算。
只取消已证明绑定当前目标的任务/订阅，禁止停止共享 executor、线程池或整条事件总线。
`Future.cancel()` 返回 true、移除 listener 或任务不再排队，都不是全部后继失效的证明。

已运行任务和迟到 callback 的最终写入/重建出口必须验证原资格。
发现子任务、重新订阅、替换 manager 或新 producer 时推进证据版本并继续处理，
不能把“父任务已取消”直接转成整个恢复链完成。


<!-- RONOVA-ORIGINAL-END orig-rec-l0223 -->

<a id="orig-rec-l0407"></a>

**迁入原文｜REC｜旧 L407–L425** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0407)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 12. 参考 Mod 研究转入第一阶段 → 12.2 任务链按对象和执行轮次结算

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0407 -->
#### 12.2 任务链按对象和执行轮次结算

对应 M04、M10–M11。继续扩展现有 `RecoveryTaskObserver` / `TaskExecutionGraph` /
`RecoveryTaskDisposition`，保留已验证的 Future/周期任务取消路径。
任务节点与执行轮次分开：同一个周期任务再运行不是旧退出事实的重放；
parent 退出只结束该次执行，已入队、同步内联或晚生成的后继分别结算。

必须覆盖 executor→服务器队列→裸线程→再次入队、CallerRuns、同步 CompletableFuture、
ForkJoin 和第三方实际委托。共享线程只是承载者，不自动成为可取消的目标。
只对已登记且准确绑定的任务使用取消/失效入口；未知封装保留未决和后续发现位置。

工作线程仅捕获有界事件和已发布的来源票据，不访问世界对象；服务器维护线程重新核对资格、
观察版本、关系深度与来源绑定。ACK 后发现更深来源时重新规划，不能继续使用较弱旧许可。
上下文进入/退出使用实际配对 token；重入、异常、同步后继和线程复用不能串用参数或来源。

记录排队、运行、取消请求、取消接受、真实退出、后继未结、结果未知和资源未释放事实，
沿既有结果类型/义务语义扩展，不以一个完成 boolean 代替。
取消不打断共享 worker；中断被忽略、超时、Future.isDone 或弱 token 回收均不补记实际退出。


<!-- RONOVA-ORIGINAL-END orig-rec-l0407 -->

<a id="orig-rec-l0426"></a>

**迁入原文｜REC｜旧 L426–L440** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0426)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 12. 参考 Mod 研究转入第一阶段 → 12.3 ACK、最终提交与旧资格失效

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0426 -->
#### 12.3 ACK、最终提交与旧资格失效

对应 M05、M08。复用已有 `RecoveryTaskPlan` / `RecoveryStoragePlan`、Root 写入门和结果查询。
计划固定原资格、规范来源位置、稳定登记身份、预期对象版本、适配器证明和全部动作依赖。
运行时对象引用和本机指针不落盘；恢复查询不得把旧引用或旧 loader 名直接变成新许可。

持久 ACK 后在实际提交门复验，并按登记位置使用 CAS、共同临界区或已证明的 Native 提交语义。
Root 持久事务内不调用未知第三方代码；取得 Root 观察快照也不等于整个物理操作已经原子化。
最终 gate 前已线性化的 clear、世界卸载、holder 替换、代码证明失效都必须拒绝旧操作。
如果实际提交已先完成，则保留该效果和未知回执，clear 不伪造“从未发生”或补发生命周期。

禁止通过自有包装捕获异常后返回成功；返回正常只能更新调用事实。
实际变更、独立结果和持久记录分别进入原 intent，回执丢失只查询或安全重交原结果。
第三方取消、任务内最终发布、内存记录写入和外部存储提交是不同门，不因一个 CAS 通过而全部签证。


<!-- RONOVA-ORIGINAL-END orig-rec-l0426 -->

<a id="orig-l6-l0663"></a>

**迁入原文｜L6｜旧 L663–L684** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-l6-l0663)

原标题路径：Ronova L6 Architecture Master Plan V2.4 — Internal Root → 6. Tombstone / Terminal Enforcement 重设计（V2 修订：C2 语义/存储分离、D5 终结拆分） → 6.3 调度状态机与预算（保留 V1）

<!-- RONOVA-ORIGINAL-BEGIN orig-l6-l0663 -->
#### 6.3 调度状态机与预算（保留 V1）

```
        readd / revive / reinsert 检测
   ┌──────────────────────────────────────┐
   ▼                                        │
 [HOT] ──物理已 detach 且短期未复现──▶ [WARM] ──连续 raw-absent──▶ [COLD]
   ▲                                        │                          │
   └────────────── raw 再现 ────────────────┴──────────────────────────┘

HOT :  每 tick 或极短 backoff（1 tick）        —— 刚检测到复活/重插
WARM:  指数 backoff 2/5/10/20 tick            —— 最近 detach、未重新注册
COLD:  低频确认（如每 100+ tick），无强引用    —— 连续 raw-absent
```

**预算规则**：
- `TerminalWatchSet` 每 tick 消耗受 `TerminalBudget` 限制（初始建议 ≤ 0.5 ms/tick，实机基准后调整）。
- 超预算按优先级（HOT > WARM > COLD）分帧处理，剩余顺延下一 tick，**绝不在一个 tick 内无界循环**。
- COLD 成员用弱引用；GC 回收后从 raw 观测再发现时重建，不常驻内存。

**防重复掉落**：同一 committed judgment 的后续 reassertion **不再调用 Lifecycle Integration**（查 `LifecycleIntegrationReceipt`），但 removal/detach/字段断言可无限幂等重放。Admission 阶段刚出现即被 tombstone 拒绝的新 incarnation **不生成第二次 death/loot**。**"正常对象不误入 watch"的可测口径（L3）**分开统计：`live/resolvable watch count == 当前命中 authoritative tombstone 且仍可解析的物理 incarnation 数`；COLD/tombstone-backed records 单独计数，不得混入 live watch 等式。误入率由 D2 的命中集合零扩大约束。


<!-- RONOVA-ORIGINAL-END orig-l6-l0663 -->

<a id="orig-road-l0341"></a>

**迁入原文｜ROAD｜旧 L341–L349** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-road-l0341)

原标题路径：Ronova L6／L6E 全量加强路线 V1.0 — Internal Root → 9. 接口衔接、效果与迁移 → 9.2 写入与完成的当前性

<!-- RONOVA-ORIGINAL-BEGIN orig-road-l0341 -->
#### 9.2 写入与完成的当前性

一次关键操作使用一致的身份、实例、BindingRevision、OwnershipRevision、PolicyRevision、ProjectionRevision 和 epoch。
最终写入位置核对当前绑定；写后独立观察。Java 排他范围和 Native 观察不能代替尚未实现的完整 Native lease / WriteSink。
外部并发变化、槽位替换或代码证据丢失按实际范围产生 STALE/FENCED/PARTIAL 等结果，再取得当前上下文处理。

CompletionContract 沿用 EXACT_SNAPSHOT、ALLOW_NEWER_PROJECTION_SAME_AUTHORITY 或 REVALIDATE_BEFORE_COMPLETION；
未声明时采用冻结合同的 EXACT_SNAPSHOT。重新验证需要重建上下文与判断，不能只替换版本数字。


<!-- RONOVA-ORIGINAL-END orig-road-l0341 -->

<a id="ms-msitem-l286"></a>

## Forge 事件、Mixin 与自定义转换器

**框架来源：** [MS msitem，原 L286–L289](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#forge-事件mixin-与自定义转换器)。

**编排说明（非原条款）：** 事件、Mixin、自定义转换器属于动作入口或合同维护层；运行时转换器细则集中在技能书，并保留本书关联入口。

**关联原条款：** [MSR · S3 —— 七字段注入契约（R57 / `msitem:313-323`）](MS-SKILLS-PLAN.md#orig-msr-l0153)；[MSC · 1.13 转换器与 Native](MS-SKILLS-PLAN.md#orig-msc-l0161)。

本节点没有被单独搬入的原文段落；它保留 MS 的独立职责入口。上列关联条款或下级章节不等于本节点所有细节已经覆盖；适用性与缺口见映射表。

<a id="ms-msitem-l290"></a>

### 事件层

**框架来源：** [MS msitem，原 L290–L300](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#事件层)。

**编排说明（非原条款）：** 单一结算所有者与事件观察/取消的区别保留。没有把被触发的每个事件当成第二个执行器。

**关联原条款：** [MSC · 1.3 统一动作调度](MS-MSITEM-PLAN.md#orig-msc-l0059)；[MSC · 1.7 防御链五层](MS-SKILLS-PLAN.md#orig-msc-l0097)。

本节点没有被单独搬入的原文段落；它保留 MS 的独立职责入口。上列关联条款或下级章节不等于本节点所有细节已经覆盖；适用性与缺口见映射表。

<a id="ms-msitem-l301"></a>

### Hook 分类

**框架来源：** [MS msitem，原 L301–L328](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#hook-分类)。

**编排说明（非原条款）：** MS 七字段 Hook 契约与原 S3/R57/R58 对照；已加载目标、失败策略及客户端边界均不靠一个目录名视为闭合。

**关联原条款：** [MSR · S3 —— 七字段注入契约（R57 / `msitem:313-323`）](MS-SKILLS-PLAN.md#orig-msr-l0153)；[RULE · 闸门 7 —— 声明完整性闸](MS-SKILLS-PLAN.md#orig-rule-l0215)。

本节点没有被单独搬入的原文段落；它保留 MS 的独立职责入口。上列关联条款或下级章节不等于本节点所有细节已经覆盖；适用性与缺口见映射表。

<a id="ms-msitem-l329"></a>

## 服务端与客户端一致性

**框架来源：** [MS msitem，原 L329–L344](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#服务端与客户端一致性)。

**编排说明（非原条款）：** 服务端结果与客户端投影/显示区分不变；原 Completion 参与范围、服务端权威与客户端扩展合同全部保留。

<a id="orig-v26-l1546"></a>

**迁入原文｜V26｜旧 L1546–L1568** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1546)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 9. 攻击平面 → 9.4.1 Completion 参与范围与客户端边界（V2.6）

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1546 -->
#### 9.4.1 Completion 参与范围与客户端边界（V2.6）

九项条件不能被执行器随意删减，也不要求专用服务端证明任意恶意客户端的像素。复用 [L6E-0 §20](MS-SKILLS-PLAN.md#orig-cf-l1149)–[§21](MS-SKILLS-PLAN.md#orig-v26-l3220) 的边界，每个 CompletionContract 在执行前固定：

| 字段 | 语义 |
|---|---|
| `completionScope` | `SERVER_RESULT` 或 `SERVER_WITH_CLIENT_PROJECTION`；后者是在前者之外增加指定客户端投影证明，不是客户端参与 Truth 投票 |
| `participantSetRevision` | Root 接受的逻辑参与集合版本；人数与诊断样本有界，客户端不能自行修改集合 |
| `participants[]` | 客户端身份、连接 session、Realm、binding 与目标 epoch；服务器投影亦声明所有权与结果范围 |
| `required/optional` | 每个参与者的每项条件分别声明；required 不等于“当前已经连上且报告成功” |
| `deadline / disconnectPolicy` | 有界等候策略；超时不得直接视作成功，不持有无界线程/队列 |
| `applicabilityReason / evidenceRef` | N/A 的客观范围依据及当前版本；仅缺 executor/evidence 不构成 N/A |

- `SERVER_RESULT` 必须证明所有适用服务端状态、记录、任务、Admission、tracking 及服务端拥有的 BossBar/Presence 发布条件；不声称远程任意 overlay 消失。其 UI 标注“服务端结果已清除”，不显示无范围的“所有客户端均清除”。
- `SERVER_WITH_CLIENT_PROJECTION` 还必须验证预先指定的 required 客户端投影。客户端 receipt 是有信任等级的证据，单个自报 ACK 不证明像素不可篡改；缺少可验证 observer 时只能 PARTIAL/UNOBSERVED。
- 专用服务端没有渲染实现：仅该客户端本地条件可按 SERVER_RESULT 预设范围记 N/A；服务端 tracking/BossBar 条件不能因此 N/A。无客户端在线不妨碍一个有效 SERVER_RESULT 合同，但不能使原来 required 的客户端条件消失。
- required 客户端中途断线/超时：原联合合同仍未完成，保留 pending/真实 PARTIAL；optional 客户端缺失如实报告，不阻塞未依赖它的服务器合同。不得等待任意客户端而 fence 正常 Server Root。
- 经授权改变参与范围必须生成新 contract/participant revision，记录旧合同未闭合原因并按新范围重验；不得修改旧 receipt、伪造旧合同完成或靠移除失败客户端把旧结果涂成绿色。
- 新 session 或重连客户端不能复用旧 session receipt。先接受当前 Root epoch/revision 的投影，旧缓存与包不得恢复资格。新联合合同按新参与集独立验收。
- 不受控或无法观察的远程/同进程显示路径若是该合同 required，必须暴露缺口；若不在所声明服务端保证范围内，则明确列作范围外，不宣称已攻击成功。

证书绑定 completionScope、contractRevision、participantSetRevision 和各 required 证据；任一关键证据失效仍适用 [§6.5](MS-MSITEM-PLAN.md#orig-v26-l0675)/[§9.4](MS-MSITEM-PLAN.md#orig-v26-l1506) 的当前完成撤销规则。客户端像素不是服务器 Authority Truth，缩减显示保证也不能缩减已建立的服务端 Judgment。


<!-- RONOVA-ORIGINAL-END orig-v26-l1546 -->

<a id="orig-rule-l0188"></a>

**迁入原文｜RULE｜旧 L188–L197** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rule-l0188)

原标题路径：Ronova MS 规则内加强方案 V2.0 → 1. 规则基（L0 铁律 + L1 参考库） → 闸门 5 —— 客户端/服务端边界闸

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-rule-l0188 -->
#### 闸门 5 —— 客户端/服务端边界闸

| ID | 规则 | 依据 |
|---|---|---|
| **R40** | 服务端不接受「客户端指定要清除的任意实体列表」；客户端不能因收到本地特效包就修改实体、战利品或注册表 | [`msitem:341`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#服务端与客户端一致性) |
| **R41** | 视觉层不能修复游戏规则；隐藏实体、改 HUD 或改物品栏文字不能替代服务端结算、清除或保护 | [`msitem:356`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#客户端表现也是物品的一部分) |
| **R42** | 客户端专属类、渲染器、UI 必须在明确的客户端边界；公共/服务端初始化不能触及它们，否则专用服务器可能类加载失败崩溃 | [`msitem:343`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#服务端与客户端一致性)、[`skills:210-211`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#客户端) |
| **R43** | 不允许从渲染线程、网络处理线程、异步线程或 Native 回调线程直接修改服务端实体集合 | [`skills:189`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#逻辑服务器)、[`entitydefence:130`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复时机)、[`msitem:282`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#服务端队列与线程边界) |
| **R44** | 不在实体集合迭代期间修改集合 | [`skills:131`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#2-实体清除)、[`msitem:195`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#目标选择与快照)、[`entitydefence:149`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#分区存储) |


<!-- RONOVA-ORIGINAL-END orig-rule-l0188 -->

<a id="ms-msitem-l345"></a>

## 客户端表现也是物品的一部分

**框架来源：** [MS msitem，原 L345–L357](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#客户端表现也是物品的一部分)。

**编排说明（非原条款）：** 迁入五页 GUI、Tooltip、普通功能与状态展示原条款；图形界面不赋予任何新攻击权限，也不改变实现证据。

<a id="orig-v26-l0931"></a>

**迁入原文｜V26｜旧 L931–L932** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0931)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 7. GUI 正式结构

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0931 -->
#### 7. GUI 正式结构


<!-- RONOVA-ORIGINAL-END orig-v26-l0931 -->

<a id="orig-v26-l0933"></a>

**迁入原文｜V26｜旧 L933–L953** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0933)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 7. GUI 正式结构 → 7.1 顶层

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0933 -->
#### 7.1 顶层

```text
若娜瓦 · 强度设置

强度预设：
兼容 | 强化 | 极限代码对抗 | 自定义

[攻击] [拦截] [防护] [运行时] [功能]
```

“极限代码对抗” Tooltip：

> 面向高强度同 JVM Mod 对抗。优先确保若娜瓦的最终判决成立，不保证敌对 Mod 的兼容性。不会以破坏存档、用户文件、操作系统或无界消耗资源作为强度来源。

不得显示：

```text
最多支持 1~2 个同级 Mod
```


<!-- RONOVA-ORIGINAL-END orig-v26-l0933 -->

<a id="orig-v26-l0954"></a>

**迁入原文｜V26｜旧 L954–L1017** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0954)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 7. GUI 正式结构 → 7.2 攻击页

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0954 -->
#### 7.2 攻击页

普通页：

```text
【攻击】

攻击强度                    [跟随预设]

强力攻击                    [跟随预设]
改写生命                    [跟随预设]
强制终结                    [跟随预设]
直接清除 / 极限清除          [跟随预设]

实体记录清除                [跟随预设]
重生压制                    [跟随预设]
重建压制                    [跟随预设]
生成压制                    [跟随预设]

渲染攻击                    [跟随预设]
追踪清除                    [跟随预设]
血条 / HUD 清除             [跟随预设]

代码防御剥离                [跟随预设]
调用链攻击                  [跟随预设]
全返回                      [跟随预设]

攻击来源追踪                [跟随预设]

[高级攻击...]
```

高级攻击：

```text
非实体攻击
逻辑源攻击
恢复源攻击
状态源攻击
注册源攻击
任务链攻击
回调链攻击
网络重建攻击
存档重建攻击
动态代码攻击
分派攻击
运行时补丁失能
目标类血缘压制
最终结果接管
```

这样保留 V2.1 的十四项，同时加入第十五项：

```text
攻击来源追踪
attack.source_trace
```

玩家 Tooltip：

> 当目标状态、恢复、注册或执行结果被其他代码改变时，追踪实际来源，为后续拦截、调用链攻击和恢复源攻击提供证据。

V2.3/V2.4/V2.5/V2.6 不新增第十六项高级攻击政策。普通页的十四项动作许可及追踪别名严格按 [§6.7](MS-MSITEM-PLAN.md#orig-v26-l0884)，不再留给 GUI 自行猜测映射。候选准入、压缩、调度、原子资格事务、完成性、宿主安全和 Runtime Surface 属于共用合同；普通界面至少分开显示“攻击结果”“是否完成”和“执行/隔离状态”，并标明服务端/指定客户端范围，高级诊断才显示 surface、scope、resource domain 与 revision。


<!-- RONOVA-ORIGINAL-END orig-v26-l0954 -->

<a id="orig-v26-l1018"></a>

**迁入原文｜V26｜旧 L1018–L1053** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1018)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 7. GUI 正式结构 → 7.3 拦截页

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1018 -->
#### 7.3 拦截页

```text
【拦截】

Mixin                       [跟随预设]
ASM / Javassist             [跟随预设]
Instrumentation             [跟随预设]
JVMTI                       [跟随预设]
Unsafe / 反射               [跟随预设]
ClassLoader                 [跟随预设]
JNI / JNA / Native          [跟随预设]
ModLauncher                 [跟随预设]
线程与任务                  [跟随预设]
全返回拦截                  [跟随预设]
```

“全部严格”不代表“声称什么都拦得住”。

规则：

```text
有真实前置门
→ DENY + VERIFY_DENIAL

已经穿透
→ QUARANTINE_RESULT_AUTHORIZATION
→ REPAIR
→ REVERIFY

只有观察能力
→ 不能写 DENIED
→ 限制 Ronova 对该结果的授权
→ 按最小 scope FENCE
```


<!-- RONOVA-ORIGINAL-END orig-v26-l1018 -->

<a id="orig-v26-l1054"></a>

**迁入原文｜V26｜旧 L1054–L1080** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1054)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 7. GUI 正式结构 → 7.4 防护页

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1054 -->
#### 7.4 防护页

```text
【防护】

生命保护
死亡保护
清除保护
实体记录保护
身份与重生保护
显示保护

反拦截
反全返回
按键与通信保护
调用保护

代码防篡改
核心权限保护
自动修复
状态恢复
重启恢复

防状态回滚
死循环保护
```


<!-- RONOVA-ORIGINAL-END orig-v26-l1054 -->

<a id="orig-v26-l1081"></a>

**迁入原文｜V26｜旧 L1081–L1104** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1081)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 7. GUI 正式结构 → 7.5 运行时页

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1081 -->
#### 7.5 运行时页

```text
【运行时】

Java Agent
Instrumentation
JVMTI
Native
本地库加载
渲染保护

关键类监视
动态代码监视
Native 模块监视
线程连续性
后台完整性检查
异常调用记录

兼容模式
性能限制
修复速度
```


<!-- RONOVA-ORIGINAL-END orig-v26-l1081 -->

<a id="orig-v26-l1105"></a>

**迁入原文｜V26｜旧 L1105–L1108** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1105)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 7. GUI 正式结构 → 7.6 功能页

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1105 -->
#### 7.6 功能页

功能与强度正交，不因 Extreme 自动改变普通游戏功能。


<!-- RONOVA-ORIGINAL-END orig-v26-l1105 -->

<a id="orig-v26-l1109"></a>

**迁入原文｜V26｜旧 L1109–L1124** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1109)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 7. GUI 正式结构 → 7.7 V2.5 界面收敛规则

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1109 -->
#### 7.7 V2.5 界面收敛规则

八类 Runtime Surface 不增加八个玩家页面，也不把 Data/Temporal/ResultGraph/Lease 等内部名词塞进普通 Tooltip。普通界面继续只显示“攻击 / 拦截 / 防护 / 运行时 / 功能”和玩家可理解的能力名；Mixin、ASM、Instrumentation、JVMTI、Unsafe、ClassLoader、JNI/Native 等机制名可以保留在拦截/运行时页。

普通状态只回答三件事：

```text
当前选择：兼容 / 强化 / 极限 / 自定义
当前生效：已生效 / 等待重启 / 能力不足 / 部分生效 / 已隔离
原因：缺少哪一类可读能力，例如“缺少底层转换支持”或“客户端显示链未验证”
```

`Runtime Surface`、S0–S6、revision、lease、plan step 与 evidence refs 只进入“高级诊断/导出报告”。GUI 不得为了显得强而显示内部枚举，也不得把内部缺口隐藏成一句“极限已开启”。

---


<!-- RONOVA-ORIGINAL-END orig-v26-l1109 -->

<a id="ms-msitem-l358"></a>

## 从零实现顺序

**框架来源：** [MS msitem，原 L358–L371](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#从零实现顺序)。

**编排说明（非原条款）：** 接续既有恢复链和分阶段实施安排。MS 从零顺序不是把已在施工的 Ronova 退回最初步骤。

<a id="orig-rec-l0317"></a>

**迁入原文｜REC｜旧 L317–L347** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0317)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 10. 实施顺序与阶段验证

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0317 -->
#### 10. 实施顺序与阶段验证

本工作作为 7.17 B/D、C/E 的同一条生产链实现，F/G 消费其状态和阶段证据。

1. 通用观察与来源关系：先接真实快照/重建输入、任务和容器关系，接入 Candidate/Obligation。
2. 精确记录与持久恢复：补 Root 来源计划、注册 effect 语义、实际 Data sink 和查询式恢复。
3. 任务、注册与资源：补取消/后继/迟到写入的资格门，以及可证明归属的资源释放。
4. 终结编排与完成观察：与身体、索引、网络同一计划执行，接入 GUI 和当前完成缺项。

按用户最新要求，施工期间运行改动相关的编译和针对性测试；
**主链接线后集中完整构建、回归和实机验收**。代码、失败原因或验收范围未变，不重复同范围检查。
下表为待运行的功能场景，复用现有 B/X 合同，不把它们计作已经通过的测试。

| 场景 | 关键断言 | 关联合同 |
|---|---|---|
| 多种通用 manager / Map / 快照布局 | 按实际关系定位目标；生产代码不含第三方标识特判 | B03、B11、X59 |
| 同 UUID、新 UUID、新对象与不同 loader | 不继承旧执行资格，不因同名/同类误伤 | B03、B13、X59 |
| 同容器有其他目标、重复字段或诱饵 UUID | 只处置有因果证明的目标记录，保留无关数据 | B11、X31–X33 |
| 身体清除时同步重建、下一 tick 重建 | 现有 gate 先拒绝旧资格，新增来源被承接 | B02、B08、X38 |
| 入队、已运行、取消中、迟到 callback、再订阅 | 不用 cancel 返回值冒充后继全部失效 | B07、B08、X50、X54 |
| 内存清理后保存、退出、重开 | 持久记录效果可查；仅 dirty 不视为完成 | B10、X63–X65 |
| intent/写入/保存回执前后失败 | 原 effectKey 恢复；不重复未知副作用 | X63–X65 |
| 检查与写入之间发生容器替换、资格/政策变化 | 真实提交门拒绝旧 lease；不使用旧对象位置继续写 | B17、X45 |
| 清理前后执行 clear，随后合法重新召唤 | clear 立即按原义生效；不恢复旧任务，也无隐形类封禁 | X48、X53 |
| 共用票据、线程池和 listener manager | 不卸载无关资源，释放结果与归属可核对 | B11、X46 |
| 未观察来源、无 executor、opaque 保存或 Native 缺失 | 状态如实保持未决，不签发虚假完成 | B16、X56 |
| 多目标洪泛、资源不足与停服 | 有界继续、无饥饿/循环修复；清理不创建 runtime | X34–X44、X46、X54 |

至少使用相互独立的数据布局与调度方式构造通用测试来源。
用户提供的实际 Boss 可作为额外现象复现样本，但不成为生产依赖或唯一通过标准。


<!-- RONOVA-ORIGINAL-END orig-rec-l0317 -->

<a id="ms-msitem-l372"></a>

## 验证矩阵

**框架来源：** [MS msitem，原 L372–L391](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#验证矩阵)。

**编排说明（非原条款）：** 动作、生命周期与清除验收按原测试原号保留；MS 11 项验证矩阵仍是来源检查清单，不是此次执行结果。

<a id="orig-bl-l2714"></a>

**迁入原文｜BL｜旧 L2714–L2735** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l2714)

原标题路径：26. 各增强轴的验收原则 → 26.2 VITALS

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l2714 -->
#### 26.2 VITALS

必须覆盖：

- CURRENT；
- MAXIMUM；
- LOCKED；
- CLEAR；
- heal；
- hurt；
- attribute recalculation；
- NBT；
- Clone；
- relog；
- dimension replacement；
- external state rewrite；
- unresolved state owner。

且 VITALS 永远不能偷偷产生 Tombstone。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l2714 -->

<a id="orig-bl-l2736"></a>

**迁入原文｜BL｜旧 L2736–L2746** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l2736)

原标题路径：26. 各增强轴的验收原则 → 26.3 TERMINAL

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l2736 -->
#### 26.3 TERMINAL

必须证明：

- Tombstone 逻辑寿命不由有限 retry 决定；
- physical object 重新出现仍被 Raw Observation 看见；
- Admission closure 能阻止合法重建；
- Root revoke 是恢复存在资格的唯一 Authority mutation。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l2736 -->

<a id="orig-v26-l0874"></a>

**迁入原文｜V26｜旧 L874–L883** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l0874)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 6. 强度模型 → 6.6 V2.5 逐能力 ProfilePresetMatrix → 6.6.6 矩阵正确性要求

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l0874 -->
#### 6.6.6 矩阵正确性要求

1. GUI 选择预设时生成一份完整、带 revision 的逐键解析回执；不能只保存 profile 名称后在各模块自行猜测。
2. 同一 `StrengthOptionKey` 的 Compatible、Enhanced、Extreme 必须命中不同生产策略或明确声明相同且给出原因；不得三个档位共用一个 executor 参数却显示三档。
3. `effectiveLevel` 永远不高于运行时 ceiling 与证据 ceiling；缺少 Native/JVMTI、需要重启或专用服务端不支持客户端模块时必须真实降级并显示原因。
4. `FOLLOW_PROFILE` 只引用所配置的矩阵 revision；玩家改单项后转为 Custom，其他项继续保持最近一次显式解析值，禁止隐式漂移。
5. Host Safety、Loop Guard、single-writer、durable recovery 与版本校验不属于可关闭强度项；任何档位都必须保持。

---


<!-- RONOVA-ORIGINAL-END orig-v26-l0874 -->

<a id="orig-rec-l0472"></a>

**迁入原文｜REC｜旧 L472–L487** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0472)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 12. 参考 Mod 研究转入第一阶段 → 12.6 同一批次的出口和证据

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0472 -->
#### 12.6 同一批次的出口和证据

本阶段以来源发现 → 独立 Root 意图 → 准确记录/任务处置 → 实际保存与读回 →
原意图重启接续 → 资源结算 → 稳定完成为一次大批次交付，不把新增基础类当作阶段完成。

| 已有承接基础 | 本轮必须补齐的验收 | 完成所需证据 |
|---|---|---|
| 静态/已加载来源、工作线程 copy 链 | 缓存别名、原对象无 NBT 重插、旧 holder 后继、不同 loader/身份碰撞 | 生产关系见证；无关对象保留；新边立即打断稳定窗 |
| Future/周期任务实际取消与退出 | 排队原资格、包装、同步/跨队列后继、周期执行轮次和迟到提交 | 每个相关执行及必要后继有退出/失效结果；共享池仍正常工作 |
| Root task/storage 意图及迁移 | 最终提交撤销竞争、外部保存 executor、分阶段失败和原意图重开 | 原 ACK/attempt 连续；存储独立读回；旧未知生命周期不改写 |
| 预算、checkpoint、完成观察基础 | 多目标容量外续接、事件丢失恢复、资源实退、无空转与完整稳定窗口 | 预算及最长等待有记录；正例真正完成；负例明确未决 |

这些场景并入原 B/D、C/E、F/G 和 68 项台账的适用子范围，不额外制造“已通过”计数。
夹具可以预置数据，但不能代替最终 observer 的来源归属、任务退出、保存与完成见证。
阶段一必需的代码/Native 能力证明随主链交付；全面代码对抗和完整 GUI/客户端验收仍分别在阶段二、三。
任一必要路径缺失、正例不能稳定收敛或相关回归失败未解释，第一阶段继续保持未完成。

<!-- RONOVA-ORIGINAL-END orig-rec-l0472 -->

<a id="orig-p0a-l1812"></a>

**迁入原文｜P0A｜旧 L1812–L1826** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1812)

原标题路径：50. Integration Tests → I-02 Terminal Rebuild Attempt

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1812 -->
#### I-02 Terminal Rebuild Attempt

```text
Subject X terminal
→ new Entity same UUID
```

要求：

```text
no ACTIVE binding
```

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l1812 -->

<a id="ms-msitem-l392"></a>

## 故障定位

**框架来源：** [MS msitem，原 L392–L405](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#故障定位)。

**编排说明（非原条款）：** 保留与动作重复、目标快照、战利品、同步、客户端类加载及表现不一致有关的故障定位入口；完整 Ronova 逐症状手册仍是文档待办。

**关联原条款：** [MSC · M1 —— 可见验收闭环（C8、R-S2/S3/S4）](MS-SKILLS-PLAN.md#orig-msc-l0243)；[MSC · 1.14 构建与验收](MS-SKILLS-PLAN.md#orig-msc-l0177)。

本节点没有被单独搬入的原文段落；它保留 MS 的独立职责入口。上列关联条款或下级章节不等于本节点所有细节已经覆盖；适用性与缺口见映射表。

<a id="ms-msitem-l406"></a>

## 完成标准

**框架来源：** [MS msitem，原 L406–L418](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#完成标准)。

**编排说明（非原条款）：** 一次动作的目标结果、服务端/客户端职责及范围证据都须分别说明；本次只迁移设计与历史记录。

<a id="orig-rec-l0293"></a>

**迁入原文｜REC｜旧 L293–L316** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rec-l0293)

原标题路径：Ronova 7.17 通用恢复链处置实施规划 V1.0 → 9. 预算、退出与完成条件

<!-- RONOVA-ORIGINAL-BEGIN orig-rec-l0293 -->
#### 9. 预算、退出与完成条件

证据、locator、图边、任务句柄、扫描游标、意图和诊断均接入现有资源记账。
重复恢复尝试合并成同一义务与饱和计数；不同来源洪泛转入有界范围汇总。
容量满会延后探查并保留覆盖缺口，不将其标成“无恢复源”。
按主体和来源公平轮转，保留现有 Authority/恢复预算。

暂停、clear、失败和停服先撤销相应入口和旧许可，再释放已证明不被在途调用使用的句柄与 reservation。
未退出任务及其占用资源继续单独记账，不因取消或观察引用丢失提前释放。
退出只查询已有 runtime，不创建新实例；未决 durable intent 由下次恢复承接。

本功能只能为现有九项 Completion 条件提供对应证据：
恢复任务、重建入口、保存恢复，以及关联记录和资源的未决工作。
身体、网络、tracking、Presence 和其他必要条件仍独立评估。

“已知恢复链已处置”要求所有已纳入该范围的来源均有当前有效的处置与后继证据，
没有必需的效果未知、持久化未知或覆盖缺口，并通过声明的稳定窗口。
这不等于证明整个 JVM 不存在未观察到的任意恢复机制；
相关必要面不可观察时保持 PARTIAL/UNOBSERVED。
未来出现新边时保留历史收据，撤销当前完成性并重新收敛。

普通界面显示“身体已清除”“重建已阻止”“恢复源仍待处理”
或“已知恢复链已处置”，附简短缺项；内部 revision、slot 和源码位置只进入高级诊断。


<!-- RONOVA-ORIGINAL-END orig-rec-l0293 -->
