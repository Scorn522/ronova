# Ronova r66 能力接线与 JVMTI 增补规划

**fix12 · 验收分工已决定：** [新增接线验收口径](WIRING-ACCEPTANCE.md#decision) · [八方向绑定](WIRING-ACCEPTANCE.md#direction-binding) · [原 B01–B23／X24–X68 范围](VALIDATION-TRACEABILITY.md#acceptance-scope)。本页原19行观察点成为新增工作的正式判据；原文不变，批次不增加，运行结果未填。下列旧轮“当前”只描述其当轮。

**fix11 · 已有基线补遗：** [复用接缝与分层纪律](#eight-existing-baseline) · [补遗回执](RESTRUCTURE-REVIEW.md#review-fix11-baseline)。不新增方向/批次，不改fix10八条决策；新增内容为已有实现与真正增量的限定，不是实现完成。

修订：**fix10 · 第二批八方向核验融合，2026-09-15**。基础：原 r66 源码 + 完整 fix9 + 本轮一文件核验交接包。**生产源码仍为原 r66；本轮只更新规划、来源接线与文档核对工具，不是 Mod 能力实现更新。**

[规划入口](README.md) · [技能/运行时主书](MS-SKILLS-PLAN.md) · [动作主书](MS-MSITEM-PLAN.md) · [实体防御主书](MS-ENTITYDEFENCE-PLAN.md) · [原始源码核对摘录](../reference/R66-CAPABILITY-AUDIT.md) · [fix4 静态清单](restructure/capability-wiring-baseline.json) · [本次融合与原文来源](restructure/enhancement-addendum-merge.json)

本增补挂在 MS 三书职责之下，不再创建一份覆盖所有项目规则的“最高规范”。**只显式纠正本页点名的历史判断，并更新这条接线工作流的实施顺序。** 原 D*、INV*、A*、R*、ID-*、T-*、B/C/D 块 ID、原文块和映射不删、不改、不重编号。原稿中曾声称“完整达到”的句子仍可供追溯，但不能在有就地更正时当作当前实现状态。

<a id="v13-receipt"></a>
**V1.3 当前回执：接受①⑦⑨；修改②③④⑤⑥；废弃⑧的本轮任务。** [逐条理由、前置和落点](RESTRUCTURE-REVIEW.md#review-fix6-v13)。①⑥⑨并入原第一批，④⑤并入第二批，⑦接在①⑥之后；②为条件第四批，③只先做既有请求成本测量。接受的是规划，不是运行验收。V1.3不是V1.2勘误，二者互补；以下修订是本轮显式设计选择/新增复核，不反写原作者结论。

<a id="same-privilege-surface"></a>
**fix7 当前融合：** 新输入是同一 JVM 同权限干预面的只读清点，**不新增方向、批次、Root 或不变量编号**。原 A/B/C/D 四块、§7 八条动作按原序在[回执](RESTRUCTURE-REVIEW.md#review-fix7-surface)逐项登记；结论层级分为交接原稿、本轮源码/外部复核、待实施设计。以下加固接回 C13、C11/D、V1.3⑤⑥⑨及原生合同，不创建第二套总纲。原八条编号只在该来源内有意义。

**重要收窄：** 14 个 Mixin 目标名在 MC 租约名单不等于 14 个目标内所有注入都受保护；Native 已有 ASM 重施/校验，不是完全不能修复。71 个显式 runtime 调用全部跨出 bootstrap 包，不能直接批量改 package-private。恢复源 evidence.fresh 已检查 observerVerified，部分调用方已有绑定核验；要补的是入口可信证据与提交边界，不抹去既有检查。[本轮复核及原行](../reference/R66-SAME-PRIVILEGE-REVIEW-EVIDENCE.md#findings)

<a id="verification-ceiling"></a>
**fix8 当前融合：** [三份报告与原交接索引](../reference/v13-verification-ceiling/README-INDEX.md)完整保留。前两份写于 fix7 之前，第三份针对 fix7 检查器；不改写成 V1.4、九方向的新回执或新批次。⑤区分采样陈旧与证书到期，⑥明确新增结算端口唯一性检查，⑦补固定载体解锁前提；C13补请求/天花板/消费者量化基线。源码独立复核发现八处通用调用均读取观察位，不能因为执行位受限而整体换成写入口。[本轮回执](RESTRUCTURE-REVIEW.md#review-fix8-verification) · [独立源码证据](../reference/R66-VERIFICATION-CEILING-REVIEW-EVIDENCE.md#consumer-summary)。检查器路径修复见[工具回执](RESTRUCTURE-REVIEW.md#review-fix8-portability)，不计入运行强度。

<a id="eight-directions"></a>
**fix10 当前融合：** [Ronova侧八方向只读核验](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md)沿 S1–S4/A1–A4 原标识融入下方职责章节；“第二批”只是该组方向的来源称谓，不新增第七个工程批次，也不与V1.3九方向或DS/RULE A项混号。[逐项回执](RESTRUCTURE-REVIEW.md#review-fix10-eight) · [独立源码补读](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#findings)。S2与C13并行、A4提前；S3保留累积观察而非四态改名。源码还显示S4已有任务图失证消费者、A2已有capability mask、A3已有结果模型。全部是接下来的工作，不是新增运行状态。

## 阅读导航

[八方向回执](#eight-directions) · [S1挑战统一层](#eight-s1) · [S2写集](#eight-s2) · [S3独立见证](#eight-s3) · [S4连续性](#eight-s4) · [A1依赖索引](#eight-a1) · [A2协议协商](#eight-a2) · [A3影子推广](#eight-a3) · [A4状态/熔断](#eight-a4)

[fix8核验融合](#verification-ceiling) · [极限基线](#extreme-ceiling-baseline) · [观察/执行对接](#recovery-consumers) · [同权限清点融合](#same-privilege-surface) · [决定侧](#surface-policy) · [API 与授权](#surface-api) · [Mixin/租约](#surface-mixin) · [恢复源绑定](#surface-binding) · [持久认证](#surface-durable) · [V1.3回执](#v13-receipt) · [成本账](#v13-cost-model) · [生命期观察](#v13-object-lifecycle) · [类血缘](#v13-lineage) · [证书](#v13-certificate) · [结算归属](#v13-settlement) · [跨表面预算](#v13-cross-surface-budget) · [范围](#scope) · [源码事实纠正](#source-corrections) · [DS:C13 发布管道](#capability-pipeline) · [54 键清点](#key-inventory) · [DS:C11 / D 块动作](#action-wiring) · [原生能力申请](#native-negotiation) · [逐项现状](#native-current-state) · [已有任务回退](#native-local-fallback) · [ThreadEnd 清理](#native-thread-end) · [整组申请语义](#native-add-capabilities) · [五条 JVMTI 方向](#jvmti-directions) · [实施批次](#batches) · [关键验证与台账](#validation) · [旧条款映射](#affected-originals) · [依据](#sources)

<a id="scope"></a>
## 范围与状态

硬限制继续沿用[当前四项边界](README.md#scope-hard-limits)：不改依赖/Mod/Forge 文件、不针对、不删对方文件、不起外部进程。无参数仍为可选取向，不成为第五条铁律；允许宿主原有启动流程提供参数，并不授权 Ronova 拉起 helper 或新 JVM。Native/JVMTI/进程内线程没有被一概禁止；同样不能用“官方 API”作为绕过上述边界的理由。

现有全部 54 项的极限目标与原 FULL L5/L6/L6E 工作项继续保留，**没有把未接通项从计划删掉，也没有把 Extreme 的最终目标改成仅观察或注册实体局部模式。** 先交付能证明的一部分，是实施顺序，不是最终上限。未完成的广度继续作为工程缺口；局部成功不能抬成广谱成功。

本页将证据分为三层：**源码摘录/枚举提取**、**外部规范和实现说明**、**待实施的设计选择**。三者均不等于用户 JVM 的现场验证。本轮不改 `CURRENT-PROGRESS.md`、源码、构建、资源、原台账或旧运行证据；不回填任何验收状态。

<a id="source-corrections"></a>
## r66 源码事实：哪些结论应当收窄

| 原判断 | 本轮采用的精确表述 | 直接依据 |
|---|---|---|
| 54 键只有 6 能 ACTIVE，其余 48 永久不可用 | `RuntimeCapabilityProjection.apply` 明列 6 个键，支持上限仅见 PROTECT_RONOVA/OFF；实际 ACTIVE 取决于 proof。此统计不覆盖所有局部路径 | [projection](../reference/R66-CAPABILITY-AUDIT.md#projection) |
| `withCapability` 只有两个生产调用位置 | 该类里有两个；`TerminalActionPolicy.resolve` 另有注册实体局部调用。不能以全局 OFF 推断局部不可达 | [terminal-policy](../reference/R66-CAPABILITY-AUDIT.md#terminal-policy) |
| 14 动作只接线 1 个，因此另造 13 执行器 | `DominionCommands` 的二分文案是硬编码，非执行器注册清点；三种实体局部动作有路径；STRONG_ATTACK 是聚合开关 | [diagnostics](../reference/R66-CAPABILITY-AUDIT.md#diagnostics)、[actions](../reference/R66-CAPABILITY-AUDIT.md#actions) |
| `TerminalLifecycle` 服务器整体不可达 | 当前入口在有效 qualification/revision 条件下委派计划执行器，后者通过执行端口回调计划内整合；旧内部服务器支路不等于当前全链 | [lifecycle](../reference/R66-CAPABILITY-AUDIT.md#lifecycle)、[executor](../reference/R66-CAPABILITY-AUDIT.md#executor) |
| 四个申请字段即四项已获授能力 | 它们按 potential 条件复制；摘录没有检查 GetPotentialCapabilities 返回值，原生树中未定位 GetCapabilities 回读。未测实际获授集合 | [native](../reference/R66-CAPABILITY-AUDIT.md#native) |

这里没有反向宣称“那 48 项都已做完”“全部局部通道必然执行成功”。**存在接线 ≠ 触达 ≠ 结果生效 ≠ 原口径验收完成。**

<a id="capability-pipeline"></a>
## DS:C13：把全局发布、局部能力与执行诊断贯通

这是当前优先工作，与[DS 原 C11/C12/C13](MS-SKILLS-PLAN.md#orig-ds-l1040)共同实施。RULE:C13 是规则清单单一来源，不要与 DS:C13 天花板管道混用。

**保留缺省 OFF。** 缺少支持证据时不应默认可执行；修复对象是支持证据的生产、传递、有效范围和消费，不是把常量改为 STRICT_ALL。保留 configured request、active request、supported ceiling、evidence ceiling、runtime state 之间的区别；重启待生效不等于已执行。

### 拟采用的数据流（尚未实现）

```text
现有 proof / 已登记 executor 或 outlet 的边界证明
    → 来源与版本、作用范围明确的能力记录
    → 全局能力视图 或 对本次对象/出口的局部视图
    → 同一动作计划和权限解析
    → 原执行器消费
    → 结果/失效回执
    → 命令、配置界面和诊断读取同一事实
```

这只是支持/证据投影，不创建第二份 Judgment、Admission、TargetEpoch 或 durable Truth。实例身份与 revision 以现有权威来源为准，不让能力记录重新裁决目标。

**候选记录字段**（拟用字段名，不声称源码已有类型）：`key`、生产者/执行器标识、能力范围、支持级别、证据级别/状态、策略与对象/类版本、失效原因、执行端口和结果出处。范围区分“通用路径”“已登记实体”“已登记 NBT 出口”“已登记任务/存储”以及客户端特定出口。发布与解析必须保留范围，不能取所有局部支持的最大值冒充全局支持。

同一线程/操作捕获一份与当前 policy revision 匹配的不可变能力视图；执行前按原合同复核对象/出口资格。证明丢失、类替换、世界/连接切换、策略更新或 worker 失效时，撤销对应证据，而不是静默保留历史 ACTIVE。不同范围不能互相扩大权限；诊断也不能因外层全局 OFF 抹掉已经成立的局部能力。

### 先复用，再扩展

| 现有入口 | 首批改动目标 | 不得据此宣称 |
|---|---|---|
| `RuntimeCapabilityProjection.apply` | 让发布来源和消费者可逐键追踪；按原 proof 条件更新，不统一填充 | 六键现场全 ACTIVE、六键全 Extreme |
| `TerminalActionPolicy.resolve` | 保留 LIFE/REMOVAL/REMOVED_RECORDS/BYPASS_RECORDS 的 exact entity 范围，接统一诊断 | 三个相关广谱键对所有表面可用 |
| `RecoveryChainPolicy.registeredAtomicSnapshot` | 将真实 snapshot 绑定与失效原因接入局部诊断 | 任意静态缓存/全部保存源均覆盖 |
| `registeredNbtOutlet` | 将 outlet、copyDepth、boundary 状态和策略深度呈现清楚 | 网络/保存重建全覆盖 |
| `registeredTaskDisposition` / `registeredStorageCommit` | 复用已登记工作资格；对照实际 executor 和结果生产者 | 第三方任务/外部保存整链已经验收 |
| `DominionCommands` 的“未接线”二分文案 | 由能力/执行器事实给出“未实现／局部可用／待重启／失证”等真实说明 | 文案修改本身提升执行强度 |

这些状态词是显示建议，不要求新增一套状态机。GUI 继续使用原人读结构，不再堆一页 JVM 术语；详细 producer/scope/revision 放诊断展开中。

**DS:C12 逐键三档仍是独立工作。** C13 接上局部能力既不自动完成 profile matrix revision 2，也不授权把 ACTIVE 等同于 STRICT_ALL。C12 与 C13 可以同批修改，但需分别说明真正变化的生产策略。

<a id="extreme-ceiling-baseline"></a>
### 极限档量化基线：请求已满，发布与范围尚未贯通

**来源结论与采用范围分开：** 原报告的 `filled(STRICT_ALL)`、rank-min、三处显式授予和 8/13 调用计数可对照当前源包；“执行已到4”“恒为某个reason”“每条路径永远≤2”等外推不作为运行事实。以下是在**原 r66 可见生产赋值路径及内置 preset revision 1**范围内的静态结论，不涵盖另行构造的 snapshot、反射/运行期修改或后续实现。[预设](../reference/R66-VERIFICATION-CEILING-REVIEW-EVIDENCE.md#preset) · [解析](../reference/R66-VERIFICATION-CEILING-REVIEW-EVIDENCE.md#snapshot) · [恢复策略](../reference/R66-VERIFICATION-CEILING-REVIEW-EVIDENCE.md#recovery-policy)

| 层/路径 | 当前源码事实 | 工程含义 |
|---|---|---|
| 请求侧 | 内置 EXTREME 对54键 `filled(STRICT_ALL)`；ENHANCED填RELATED_PATHS，CUSTOM填MONITOR；显式逐键/动作覆写与pending状态另算 | 再提高这个内置极限预设是空操作，不等于每个自定义工作区的active request都为4；C12真实逐键策略仍保留 |
| 机制合成 | `MechanismLevel.lowerOf` 按rank取小，effective取请求、支持、证据三者最小 | 两套配置词汇的桥接不是新天花板类型；攻击侧关闭值是DISABLED，不是OFF |
| 显式支持发布 | 全局2处 `.withCapability` + 实体局部1处；表达OFF/PROTECT_RONOVA，未定位3/4授予 | 原稿“两处”表实际列三处；实体局部有5固定键，并另并入动作requiredKeys，不能把集合永久记为五键 |
| A：通用恢复解析 | 八种Step最低需求为3或4，当前可见支持最高2不足以令其执行位成立 | 执行位不成立不等于观察位不成立；reason还受禁用、失证、未激活、pending等分支影响，不恒为RECOVERY_DEPTH_LIMITED_BY_DEPENDENCY |
| B：四类registered入口 | 按请求/动作依赖、NBT门键、copyDepth、重启待生效、注册绑定条件产生仅对应对象/出口的局部决定；外部显式调用13处 | 可以**表达**3/4的局部许可，不是现场完成证明；不能把13行当作8种Step全覆盖，也不能抬成54键全局4 |

C13的增量是让**真实发行、范围/版本/失效明确的局部支持与证据**进入同源发布/诊断，仍保留通用支持。不得拿`requested`本身当已证的ceiling，也不得因全局2把原registered范围一律削到2。局部对象变更、原登记解除、版本或边界证明失效时，其投影必须一起撤销；只有观测或许可而没有执行/结果，报告必须分层显示。

原报告§6的“第10项”在本轮**归并到已有DS:C13与V1.3⑨**，不登记新方向或不变量。它是当前第一批的可执行描述，不是新批次，也不取消原最终Extreme目标。

<a id="recovery-consumers"></a>
### 通用观察与注册执行的对接决定（待实施）

**独立消费侧复核：8个通用调用全部读取 `observationRequested()`，其中6处DIRECT_SOURCE、2处DERIVED_SOURCE。** 它们不是8处读取执行许可的死入口。原稿只计了调用，未区分消费的record成员；因此“不必救活所有恢复工作”可以保留为不重复造轮子，但“8条全活/无需补缺”不成立。[逐调用原行](../reference/R66-VERIFICATION-CEILING-REVIEW-EVIDENCE.md#consumer-summary)

**本轮设计决定：保留A的通用/观察职责，保留B的注册执行职责，不批量替换、删除或给A加入无范围的ceiling绕过。** 第一批逐个记录这8处观察调用的Step、读取成员、下游实际写端口及结果生产者；对本批选中的注册写路径，由原发行者提供同一scope/revision的局部能力记录，B消费者和诊断读同一记录。只有已建立注册对象/出口资格、必要证明及提交边界的写端口才接scoped执行解析；不能让观察调用自己构造true或借其它scope获得写权。其余真实缺失写路径继续进入C11/D的第二批，不凭计数宣布已完成。

`DIRECT_SOURCE`在A中用来控制发现/观察，四类B并不直接返回该Step；不能由“共用八个枚举”推出一一可替换。若以后统一函数签名，仍须显式区分观察请求、执行许可和结果证据，兼容保持观察链，不以隐式回退混淆权限。

reason接入同源诊断时保留结构上下文（key、Step、scope、policy/证据版本、原reason），分别显示观察未请求、待重启、失证/未激活、深度受限、绑定失败、局部许可和结果未知。它是结果的说明，不因字符串存在就产生授权；动态reason前缀与组合不能按“12/5种”当完整状态契约。

<a id="surface-policy"></a>
### 同权限清点 B：决定侧的保护与能力发布必须同批闭合

**来源事实：** 111 个封印前缀中 policy 只有两个消费者；`RuntimeCapabilityProjection`、`StrengthPolicySnapshot`、`ProfilePresetMatrix`、`MechanismLevel` 与动作/档位依赖未被前缀选择。原稿的283个封印class来自其构建清单，本轮只读源码包未提供该构建清单，不能把283写成独立复测。[依据](../reference/R66-SAME-PRIVILEGE-REVIEW-EVIDENCE.md#findings)

**并入第一批的设计：** 对当前选中能力追踪 producer→snapshot→preset/level/requiredKeys→consumer→diagnostic 的实际依赖，补决定侧必需自有类及内部类的封印选择；同时保护接收和发布证明的入口。先登记覆盖欠账，再逐依赖闭合；不能只封消费者，也不能无差别把全部第三方或 MC 类当作 Ronova 的原字节镜像。构建阶段按实际选择重新核对 class 数、聚合字节、构建身份和加载器；保留512类/4 MiB现有上限，源类小和位数余量都不证明构建一定装得下。不修改依赖/Mod/Forge 文件。

投影失证必须在可信观察仍可用时撤销相应 scope 的证据，诊断尽量区分真实不支持、未观察、失证/生产链异常；诊断若也受干预则承认未知，不把一个 UNAVAILABLE 文案当作防护证明。封印自身回调之后的改写、发布器/API 数据被合法方法修改等风险进入⑨报告；新增前缀只证明选择，不证明恢复或防同权限隔离。

**rank 对账：** r66 三处显式 .withCapability 生产调用所示上限为2；恢复通用 resolve 与 registered* 是不同范围的路径。后者允许满足请求、版本与绑定的注册范围表达3/4，不得因全局2而全部削到2，也不得把局部4提升为54键全局4。首批诊断同时报告通用与局部范围。原稿“全部动作静默作废”是未实测外推，不能在当前规格当确定结果。C12/C13最终极限目标不变。

<a id="surface-binding"></a>
### 同权限清点 B 附：恢复源证据的发行与提交边界

`approveRecoverySourcePlan` 传字面 true 的不一致保留为待确认工作，但 `RecoverySourceEvidence.fresh()` 同时检查 observerVerified；重建调用方还有 evidenceVerified/boundary/nativeCurrent/stillBound，维护路径有资格、语义观察及采后 stillBound。因此不是“全程无绑定”。当前公开可构造的 record/布尔值也不自动证明它来自受信观察者。[原审批、fresh 与调用方](../reference/R66-SAME-PRIVILEGE-REVIEW-EVIDENCE.md#source-excerpts)

**并入第一批所选恢复路径、第二批扩大覆盖：** 明确条件只读 plan、持久 source qualification、实际出口决策三种权限，追到原观察者的发行/登记记录，并在原 Root 审批及持久提交边界复核确切 source/对象/版本/authority/epoch及仍然有效的边界证明；消费者不得自己用 true 代替发行证明。若保持由已提交 command 派生的只读许可语义，显式说明它信任何种输入、为何成立，以及怎样限制未受信的直接入口；若该前提不能验证，就拒绝该次许可/失效对应scope，而不是取消正常恢复链或提高全局ceiling。

采后检查不能自动替代持久写入前的可信性；已有持久 journal 的正确历史不因本轮设计被改写。`Integer.MAX_VALUE` 在现行 plan 中是策略的复制深度上限，**不代表取消宿主成本、递归/任务/时间预算**。不凭这一常量宣告资源漏洞已复现，也不因审批名为只读就忽略后续持久资格的效力。

<a id="v13-certificate"></a>
### V1.3 ⑤｜修改：复用现有失效链，补依赖快照与使用闸

**来源与增量分开：** [原稿方向⑤](Ronova_Enhancement_Addendum_V1.3.md#方向-⑤证书到期闸--依赖集哈希落在-d75-上)提出把 [V26:D75](MS-SKILLS-PLAN.md#orig-v26-l0521)变成依赖集可判定的机制。本轮只读发现 `CompletionTracker.Scope` 已绑定 command/authority/qualification/active+configured policy/contract，`Evidence` 已带 `dependencyRevision`，`sample()` 与 `current()` 已处理依赖变化、authority 失效、缺证和采样间隔陈旧（`MAX_SAMPLE_GAP=10` tick）；**不能再说整个实现只有原则或时间戳**。[源码依据](../reference/R66-V13-REVIEW-EVIDENCE.md#completion)

**fix8精度修正：** `CompletionTracker`没有独立证书到期字段；每Condition的`dependencyRevision`与整快照比较不等于声明式的期望依赖版本集及逐项匹配。已有稳定窗口/沉默失效必须保留，但⑤的到期闸与依赖合同仍需新增。参考现有 `RecoverySourceEvidence.MAX_AGE_NANOS/fresh()` 在 `ShadowDurableRootStore` 使用点复核的样式；后者用 `System.nanoTime()` 差值，是**经过时间新鲜度**，不是日历挂钟。原报告“挂钟”用语在原文保留，当前采用此独立规范核对后的术语。[原码与时间依据](../reference/R66-VERIFICATION-CEILING-REVIEW-EVIDENCE.md#evidence-age)

**当前设计选择：** 在原 Scope/Evidence/revision 链上新增独立有效期与显式期望依赖清单，再列出可解释摘要，不另造证书 Authority。有效期由所选合同和发行上下文定义，不机械复制其它证据的五秒常量；时间域、起算时刻、期限、续期条件、跨JVM失效必须明确，不能用采样次数或反复采样自动延长旧证书。发行前、实际使用前与最终写边界复核同一份资格/期望版本；到期或缺依赖则撤销该scope的完成声明。最小集合按所选结果列出：当前资格与 TargetEpoch/物理 incarnation、策略/合同 revision、所用执行端口与每条必要证据的版本、观察有效期；class/loader、配置和文件/外部状态仅在该结果确实依赖且可合法读取时加入。缺项记未知，不把“未观察”编码成零值后称哈希一致。不得为此修改依赖/Mod/Forge 文件。

同一依赖集合使用规范顺序、类型/长度编码及明确算法版本生成摘要，同时保留可逐项比较的依赖清单；摘要不具备签名或防同权限伪造的效果。事件驱动脏标记用于减少重复采样；每次实际使用前仍比较不可变快照的版本与当前资格，变化就重采/撤销。最终写入与该资格复核共享现有 lease/提交边界，不能把“先算哈希、之后任意写入”当作解决竞态。无法稳定取到所需快照时拒绝该次完成声明，不静默沿用历史 CLEARED。

**前置：** 原版本/沉默失效基础存在；独立有效期、声明式期望版本集、最终使用边界和漂移归因仍待实施/测试，不标为就绪。**关键观察：** 单依赖变化、只变顺序、采样中换版本、观察沉默及同 command 恢复分别核对；失效必须指出哪个已枚举依赖变化。原历史结果保留而不能冒充当前完成。放入第二批。

<a id="v13-bypass-report"></a>
### V1.3 ⑨｜接受：能力报告携带具体失效条件

沿用 [L6:L6-INV-10](MS-SKILLS-PLAN.md#orig-l6-l0859)的准确口径：**JVM 完全失陷不属于 Internal Root 的隔离保证**。同权限参与者可能干预观测、代码或发布链，应列出具体条件；不把“同权限存在”一概写成“每项能力已经被绕过”。报告范围仍为 key + scope，不借报告字段新造权限。

**拟采用报告结构：** 复用当前能力记录，增加“分析状态＋已知条件集合＋适用范围/版本＋依据”四部分；`UNANALYZED` 与 `ANALYZED` 分开，条件可以有多项，不能用单选枚举把多个绕过面挤成一个。`ANALYZED` 且集合为空只表示在声明范围/版本内无已知项，不表示 SAFE 或不可绕过。旧消费者默认显示“未分析”，不能用空串或默认 NONE_KNOWN 蒙混。

原生/字节码相关报告明确列入：证明发布器、执行入口或同 JVM 权威状态被同权限代码篡改时，当前报告不构成隔离保证。其他 key 根据所选路径具体填写；自检验证分析状态、来源和作用范围，不靠复制一句免责声明。GUI 保持简明摘要，具体条件进入诊断展开；报告导出/协议消费者同源更新。

**前置：** 无新增硬前置，可与第一批 C13 同做；这是字段、生产者、消费者和检查的待实施工作，不是已添加一个字段，更不是“纯文档就完成运行接线”。**关键观察：** 未分析与已分析空集合可区分；旧版本/失证时报告同步失效；所有已发布原生相关 scope 都能定位限制条款和具体条件。

<a id="eight-existing-baseline"></a>
### 八方向补遗｜先复用具名接缝，观察与授权继续分层

输入为[Ronova侧发出的已有实现清单](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md)，不是新方向稿。原文逐字节保留；[独立消费者补读](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#findings)与来源断言分开。原S1–S4/A1–A4、fix10回执和批次不变，原先未逐一列名不再被读成这些类不存在。

**当前分层纪律：** 新的许可发行/变更批准仍归原policy/Root；观察类的非授权声明保持原样，Root消费其结果而不把类本身升级为许可。S3可在原完成合同内评价证据，但不创造第二Truth；A4可撤销/限制证据依赖的可用性，不成为另一个授权者。`observation`包中消费已授Root计划的执行适配也不因包名而被误判为自行授权。补遗§0所称“三条硬约束”按这套现行设计纪律采用，**不新增L0铁律或不可修改的上游冻结层级**；原三条铁律、UX1和不起外部进程不变。

**六条复用要求：** 不向现有观察类塞入授权；不重建受支持NBT writer遍历；连续性沿用`TaskEventRun`的不跨断裂纪律；影子结果复用`accepted/matches/rejected`而不混淆；写边界使用`WriteSinkId/Evidence/Extension`；完成使用`CompletionContractId/Extension/Evaluation`，服务端模块沿`CapabilityRuntime`唯一owner接线。具名API/注释存在不等于生产路径完成，受支持表面以实际消费者为界。

**实施前先读四组：** `RonovaTransformer`及域合同（S1）；`CopyProvenanceIndex`及tracker的loss消费者（A1）；`WriteSinkEvidence/Extension`连同真实NBT路径比对（S2）；`CapabilityRuntime/ServerCapabilityModule`连同诊断/重试语义（A4）。[逐行依据](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#inventory)登记了实际目录，不用猜测名字的零命中替代子系统核对。

<a id="eight-s1"></a>
### 八方向 S1｜复用挑战原语与生产验证，补能力获证统一层

**fix11已有载体补足：** `NativeTransformBridge`只是入口，实际委派`RonovaTransformer`；已有域合同/`SemanticGateVerifier`与`CodeSealProbe`分别提供变换、验证和镜像挑战。先读真实变换分派及所选域合同，统一层只增加缺失的挑战上下文、行为观察与发行接线，不另写一套转换器。原生产目标重施仍不等于隔离测试类挑战已经实现。[真实变换器](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#actual-transformer) · [桥接委派](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#transform-bridge)。

**现状修正：** `CodeSealProbe.evaluate` 已是自有代码镜像的挑战原语；`reassert_registered_code` 和 `reassert_core_targets`、NativeTransformBridge、任务正常exit/ThreadEnd清理已有验证片段。[源码](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#probe)。后二者不能被直接叫成四种自有dummy挑战已经齐备：core targets是Minecraft语义目标，ThreadEnd清理存在也不等于该事件现场触达。[范围](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#semantic-targets)

**第一批增量：** 按本批选中的 key/scope 将“探测→实际获授→安装/启用→受控挑战→观察→签发/撤销”接回原 proof 生产与C13；先复用现有校验/回读，不重写镜像守卫，不新增JVMTI能力。需要行为挑战的接口先定义隔离的Ronova自有fixture、预期事件/结果、预算、超时与清理，禁止用外部进程或无关第三方对象试刀。只有真实必要依赖才进入该键的获证条件，不要求54键全部挑战完才开放一个已证明的局部动作。

挑战记录复用原scope与版本，至少携带本次挑战关联值、注册实例/加载器、代码/桥接代际、观察来源、时间范围和失败原因；不得用固定回显、上次成功或API返回零代替当前行为。生产守卫周期不必重复开新线程/重转换一遍；设计合并与重入抑制，保留ThreadEnd与正常exit配对。具备探测但没有可隔离挑战时记清该层未证，不能全局置ACTIVE。

**签发边界：** 自有测试类成功只证明其样本范围，不自动证明MC/LWJGL或所有加载器可修改；能力基本自证与本次业务结果是两种证据。最终提交仍复核现行lease/revision；同权限攻击可同时影响探针和观察者，本机制不是新的可信Root或不可伪造隔离。S1不是已经做完的“纯提取”，还需要适配、隔离、结果绑定与失效接线。

<a id="eight-a4"></a>
### 八方向 A4｜复用双维状态和解析器，前移分能力熔断接线

**fix11现成宿主与适配范围：** `ServerCapabilityModule`、`CapabilityRuntime`、`CapabilityContext`、`CapabilityDiagnostics`已经给服务端修复模块提供单一tick/repair owner、逐模块过滤与失败计数。先在该宿主内接所选模块的窗口/阈值/撤销/恢复，不另开维护循环。它使用`contract.Capability`稳定位，当前standard登记六个repair模块；**不是全部54个policy.CapabilityKey或Native/客户端执行器**。以明确的模块→key/scope映射接C13/双维状态，其他域沿各自原owner适配，不把它们硬塞进player.repair。[宿主](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#module-runtime) · [能力位域](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#capability-bits)。

`engine.spawn.BoundedRetryQueue`已有FIFO、有限false重试及有界死信，可评估受控提取/适配，但它包内可见，且异常分支将原entry回队并重抛，并不递增同一个failedAttempts。**不能据类注释宣称所有异常已受有限重试约束**。新增熔断必须明确异常、false和未知副作用的分类；先查旧effect/原receipt，不自动重做不确定mutation。`CapabilityContext.attempt`会内部捕获并记失败，外层repair正常返回不代表成功，恢复判断应消费真实范围结果。[返回false与抛异常的区别](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#retry-false-vs-exception) · [内部计数](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#module-context)。

**已有而必须保留：** `ActivationState` 的 INACTIVE/ACTIVE/PENDING_RESTART/UNAVAILABLE/REJECTED/FENCED、`EvidenceState` 的 UNOBSERVED/OBSERVED/VERIFIED/DEGRADED/FAILED，以及 `StrengthPolicySnapshot` 的逐键映射、解析和 `active with degraded evidence` 原因。[枚举及真实消费者](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#policy-state-resolution)。PENDING_RESTART是“保存的配置等待重启生效”，不是所有暂不可用的总称；DEGRADED不自动降低数值ceiling，调用方必须提供实际证据上限。

**第一批采用的设计选择：** 不先给两个enum重复追加DEGRADED，也不把STALE硬塞进ActivationState。首先保持“配置/启用结果”“证据质量”“新鲜度/失效原因”“有效范围与上限”分层，在现有receipt/诊断路径明确表达陈旧原因和受影响scope；仍需独立STALE枚举时再显式定义维度、序列化及所有消费者的迁移，不按“加1–2值即可”估工。

为本批选中key/scope接通失败来源→可用证据上限/已有activation→执行闸→诊断→有限恢复探测。定义可恢复故障、重复失败窗口、冷却/退避、最大重试、同一代际并发发布和恢复所需的重新验证；硬性完整性或授权失效不等待次数阈值。失败时保留原configured request，不静默改预设；只有低档观察仍有有效证据且策略允许时才保留那部分，不能把MONITOR当一切失败的默认可用结果。必要前提完全未知时拒绝该范围的执行，不把整个Mod或无关键一起关掉；共享桥接/观察源失效影响所有真正依赖它的scope。

熔断恢复不清除已发生动作的未知副作用，不改历史验收状态；S3/S4提供结果与连续性原因，未完成S4时可先接当前生产者已有的明确失败信号。GUI收益来自消费者接线而不是枚举变长。既有状态表达降低重建成本，但阈值、重试、迁移和竞态尚未实现，不承诺低一个数量级的工时。

<a id="eight-a1"></a>
### 八方向 A1｜把依赖失效推广为有界的反向索引

**fix11已有关系索引补足：** 不只有TopologyGraph/TaskExecutionGraph；`CopyProvenanceIndex`已有真实对象复制关系、`bySource`反向索引、冲突/容量损失回调及按source退役，tracker将loss接到targetEpoch的discoveryGap。后续设计先复用身份、歧义、预算/释放和局部失证纪律，不按“关系索引与传播从零”排工。[索引/失证](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#copy-index-resolution-loss) · [接收端](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#copy-loss-consumer)。

它仍不是grant→installed→proof→capability→executor通用可传播失效图。复制关系、弱引用生命周期和source retirement不自动等于能力依赖语义；基于读过的模型选择复用内核或适配，不把所有依赖强塞进NBT复制图，也不把`resolve()`提升成许可。原各域图不被替换。

`TopologyGraph`是实体拓扑，`TaskExecutionGraph`是任务关系，不改造成第二个权威图；`CompletionTracker`已会比较依赖快照/revision、处理采样缺口与陈旧。[现有依赖模板](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#completion-dependencies)。八方向A1补的是多生产者→proof→key/scope→执行出口的通用依赖反向索引，不是从零发明任何依赖检查。

在C13/S4的scope与版本合同稳定后接原第二/第三批：复用现有事实标识和Root资格，登记依赖的来源代际、消费者版本、撤销和回收关系；受控传播失效，缺依赖/未知代际不继续发布有效证明。不得由图节点批准Judgment、签第二份lease或延续旧TargetEpoch。共享VM/bridge依赖失败可以合理撤销多scope；仅加载器A的局部依赖失效不牵连独立B。

图规模、扇出、遍历和未完成撤销都使用既有预算；拒绝循环依赖造成“互相证明有效”，先撤销再在一致快照上重建，旧发布回调不得复活已撤销代际。对54键不预先声称DAG比扫描快；先选清晰可审的索引与有界全量复核兜底，测成本后再选增量算法。它不是新Root，也不阻塞第一批的小范围直接失效接线。

<a id="key-inventory"></a>
## 54 键静态清点

下面逐项来自 r66 `StrengthOptionKey.java`，与 `RuntimeCapabilityProjection.java` 的显式集合对照。不是 54 项验收状态表；“否”仅表示这个全局发布器没有列出，不是“功能不存在”。源码路径和哈希见[静态清单](restructure/capability-wiring-baseline.json)。

| 原枚举键 | 配置键 | 通用投影中显式列出 | 首轮工作口径 |
|---|---|---|---|
| `ATTACK_NON_ENTITY` | `attack.non_entity` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `ATTACK_LOGIC_SOURCE` | `attack.logic_source` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `ATTACK_RECOVERY_SOURCE` | `attack.recovery_source` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `ATTACK_STATE_SOURCE` | `attack.state_source` | 否（仅此投影） | 优先接实体局部视图；不得把注册实体证据发布为所有表面支持 |
| `ATTACK_REGISTRATION_SOURCE` | `attack.registration_source` | 否（仅此投影） | 优先接实体局部视图；不得把注册实体证据发布为所有表面支持 |
| `ATTACK_TASK_CHAIN` | `attack.task_chain` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `ATTACK_CALLBACK_CHAIN` | `attack.callback_chain` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `ATTACK_NETWORK_RECONSTRUCTION` | `attack.network_reconstruction` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `ATTACK_SAVE_RECONSTRUCTION` | `attack.save_reconstruction` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `ATTACK_DYNAMIC_CODE` | `attack.dynamic_code` | 否（仅此投影） | 区分类定义、JIT、VM stub 和第三方原生；不能由 VM 事件推全覆盖 |
| `ATTACK_DISPATCH` | `attack.dispatch` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `ATTACK_RUNTIME_PATCH_DISABLEMENT` | `attack.runtime_patch_disablement` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `ATTACK_TARGET_CLASS_LINEAGE` | `attack.target_class_lineage` | 否（仅此投影） | 接结构引用候选与生命周期；引用不独立授权归属 |
| `ATTACK_FINAL_RESULT_TAKEOVER` | `attack.final_result_takeover` | 否（仅此投影） | 优先接实体局部视图；不得把注册实体证据发布为所有表面支持 |
| `ATTACK_SOURCE_TRACE` | `attack.source_trace` | 否（仅此投影） | 接结构引用候选与生命周期；引用不独立授权归属 |
| `INTERCEPT_MIXIN` | `intercept.mixin` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `INTERCEPT_ASM` | `intercept.asm` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `INTERCEPT_INSTRUMENTATION` | `intercept.instrumentation` | 是（静态） | 审计原 proof 的来源、时效、撤销与范围；不是自动升级 STRICT_ALL |
| `INTERCEPT_JVMTI` | `intercept.jvmti` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `INTERCEPT_UNSAFE_REFLECTION` | `intercept.unsafe_reflection` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `INTERCEPT_CLASSLOADER` | `intercept.classloader` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `INTERCEPT_JNI_NATIVE` | `intercept.jni_native` | 否（仅此投影） | 接 NativeMethodBind 的观测／控制分层；分别举证 |
| `INTERCEPT_MODLAUNCHER` | `intercept.modlauncher` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `INTERCEPT_THREAD_TASK` | `intercept.thread_task` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `INTERCEPT_ALL_RETURN` | `intercept.all_return` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `PROTECT_LIFE` | `protect.life` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `PROTECT_DEATH` | `protect.death` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `PROTECT_REMOVAL` | `protect.removal` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `PROTECT_ENTITY_RECORD` | `protect.entity_record` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `PROTECT_IDENTITY_RESPAWN` | `protect.identity_respawn` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `PROTECT_DISPLAY` | `protect.display` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `PROTECT_ANTI_INTERCEPTION` | `protect.anti_interception` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `PROTECT_ANTI_ALL_RETURN` | `protect.anti_all_return` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `PROTECT_INPUT_COMMUNICATION` | `protect.input_communication` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `PROTECT_CALL` | `protect.call` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `PROTECT_CODE_INTEGRITY` | `protect.code_integrity` | 是（静态） | 审计原 proof 的来源、时效、撤销与范围；不是自动升级 STRICT_ALL |
| `PROTECT_CORE_AUTHORITY` | `protect.core_authority` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `PROTECT_AUTO_REPAIR` | `protect.auto_repair` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `PROTECT_STATE_RECOVERY` | `protect.state_recovery` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `PROTECT_RESTART_RECOVERY` | `protect.restart_recovery` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `PROTECT_ROLLBACK` | `protect.rollback` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `PROTECT_LOOP_GUARD` | `protect.loop_guard` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `RUNTIME_JAVA_AGENT` | `runtime.java_agent` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `RUNTIME_INSTRUMENTATION` | `runtime.instrumentation` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `RUNTIME_JVMTI` | `runtime.jvmti` | 是（静态） | 审计原 proof 的来源、时效、撤销与范围；不是自动升级 STRICT_ALL |
| `RUNTIME_NATIVE` | `runtime.native` | 是（静态） | 审计原 proof 的来源、时效、撤销与范围；不是自动升级 STRICT_ALL |
| `RUNTIME_NATIVE_LOADER` | `runtime.native_loader` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `RUNTIME_RENDER_GUARD` | `runtime.render_guard` | 否（仅此投影） | B1 条件路线；不改变 FULL L6 Server 的原有范围 |
| `RUNTIME_CRITICAL_CLASS_WATCH` | `runtime.critical_class_watch` | 是（静态） | 审计原 proof 的来源、时效、撤销与范围；不是自动升级 STRICT_ALL |
| `RUNTIME_DYNAMIC_CODE_WATCH` | `runtime.dynamic_code_watch` | 否（仅此投影） | 区分类定义、JIT、VM stub 和第三方原生；不能由 VM 事件推全覆盖 |
| `RUNTIME_NATIVE_MODULE_WATCH` | `runtime.native_module_watch` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `RUNTIME_THREAD_CONTINUITY` | `runtime.thread_continuity` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |
| `RUNTIME_BACKGROUND_INTEGRITY` | `runtime.background_integrity` | 是（静态） | 审计原 proof 的来源、时效、撤销与范围；不是自动升级 STRICT_ALL |
| `RUNTIME_ANOMALOUS_CALL_LOG` | `runtime.anomalous_call_log` | 否（仅此投影） | 逐键补生产者／消费路径清点；保留原目标，不推断局部路径不存在 |

<a id="action-wiring"></a>
## DS:C11 与 D 块：动作、执行与结算一起接

先沿“入口 → 请求快照 → policy → qualification/lease → plan → executor → 结果”清点真实路径，再决定补写什么。已有局部路径保留，硬编码诊断纠正；没有源码/运行证据的部分明确待追踪，不能从一个 grep 的零命中推为永久缺失。

当前服务器链按[源码摘录](../reference/R66-CAPABILITY-AUDIT.md#lifecycle)理解为：

```text
TerminalLifecycle.enforce(server)
  → 有效 terminal qualification / revisions
  → runtime.terminalPlans().execute(...)
  → OperationPlanRunner 的执行端口
  → TerminalLifecycle.runPlannedIntegration(...)
```

新增动作继续进入现有服务器权威调度链，不为了“打通”恢复旧的无计划写入，不让事件层/注入层分别重复结算。代码里找到方法不是验证调用发生；后续记录必须指明实际提交点、消费点及结果写入点。

| 原动作 ID／显示名 | 原依赖键（源码逐字提取） | 静态所见 | 本轮规划的后续接线 |
|---|---|---|---|
| `STRONG_ATTACK` / 强力攻击 | 无（聚合开关） | 聚合控制；单独选择返回 `NO_EXECUTABLE_ACTION` | 组合动作共享上下文，不新增一个空执行器凑数 |
| `REWRITE_LIFE` / 改写生命 | `ATTACK_STATE_SOURCE` | `TerminalActionPolicy.LIFE` 的注册实体局部步骤 | 保留局部证明与原生命约束，追到结果回读 |
| `TERMINALIZE` / 强制终结 | `ATTACK_FINAL_RESULT_TAKEOVER`<br>`ATTACK_REGISTRATION_SOURCE` | 旧基线提交门允许；新服务器计划链在源码中存在 | 核对 qualification、revision、计划提交和结果结算，不以入口调用代替完成 |
| `DIRECT_CLEAR` / 直接清除 | `ATTACK_FINAL_RESULT_TAKEOVER` | `REMOVAL`／`BYPASS_RECORDS` 的局部动作 | 去重计划与 exact entity/index scope；不是广谱直接清除已完成 |
| `CLEAR_ENTITY_RECORDS` / 实体记录清除 | `ATTACK_STATE_SOURCE`<br>`ATTACK_REGISTRATION_SOURCE` | `REMOVED_RECORDS`／`BYPASS_RECORDS` 的局部动作 | 确认注册表、UUID/ID 与 tracking 的具体动作范围 |
| `SUPPRESS_RESPAWN` / 重生压制 | `ATTACK_REGISTRATION_SOURCE` | 本轮未建立独立动作的完整调用链 | 追 Admission/epoch 消费点，区别同 UUID 与合法解除 |
| `SUPPRESS_REBUILD` / 重建压制 | `ATTACK_RECOVERY_SOURCE`<br>`ATTACK_TARGET_CLASS_LINEAGE` | 恢复链有相关局部出口，但不等同该动作完整接通 | 跨 UUID 与源/派生证据范围分别核对 |
| `SUPPRESS_ADMISSION` / 生成压制 | `ATTACK_REGISTRATION_SOURCE`<br>`ATTACK_NETWORK_RECONSTRUCTION`<br>`ATTACK_SAVE_RECONSTRUCTION` | 恢复链存在 NBT 局部出口；网络/保存全链未由此证明 | 接当前拒入资格，合法生成与无关保存仍正常 |
| `ATTACK_RENDER` / 渲染攻击 | `ATTACK_FINAL_RESULT_TAKEOVER` | 本轮未建立独立动作的完整调用链 | 沿表现 surface 接结果；专用服务端不加载客户端类 |
| `CLEAR_TRACKING` / 追踪清除 | `ATTACK_NETWORK_RECONSTRUCTION`<br>`ATTACK_FINAL_RESULT_TAKEOVER` | 本轮未建立独立动作的完整调用链 | 追服务端与客户端 exact tracking 收敛，不以不可见代替完成 |
| `CLEAR_HUD` / 血条/HUD 清除 | `ATTACK_FINAL_RESULT_TAKEOVER` | 本轮未建立独立动作的完整调用链 | 记录 HUD/bar 出口结果；不让 UI 成为第二裁决源 |
| `STRIP_CODE_DEFENSE` / 代码防御剥离 | `ATTACK_RUNTIME_PATCH_DISABLEMENT`<br>`ATTACK_DYNAMIC_CODE`<br>`ATTACK_TARGET_CLASS_LINEAGE` | 本轮未建立独立动作的完整调用链 | 先归属和补丁/dispatch/result 出口；不靠能力位或观察完结 |
| `ATTACK_CALL_CHAIN` / 调用链攻击 | `ATTACK_LOGIC_SOURCE`<br>`ATTACK_RECOVERY_SOURCE`<br>`ATTACK_TASK_CHAIN`<br>`ATTACK_CALLBACK_CHAIN`<br>`ATTACK_DISPATCH` | 任务/存储局部处置可相关，不能合并成动作达标 | 沿 source→task/callback→dispatch→result 逐段接线 |
| `ALL_RETURN` / 全返回 | `ATTACK_DISPATCH` | 本轮未证明普通开关到最终结果的完整链 | 保留现有字节码路线；帧级提前返回只作条件补充 |

### D 块逐项延续

| 原来源/ID | 当前接续范围 | 不能沿用的误读 |
|---|---|---|
| RULE 加强 D1；DS D 块；DS:C11 | 基于上面的 14 项清点补执行与调度；统计时区分聚合项和独立动作 | “命令显示 13 个未接线，因此必须新建 13 执行器” |
| RULE 加强 D2；DS D 块 | 逐项查 CompletionTracker 的条件、生产者、消费点及结果定义；无生产者的再补 | 原“9 条/6 条零引用”未在本轮完整复算，不能当作新基线的独立证明 |
| RULE 加强 D3；DS D 块 | 对现有计划委派、opaque port、runPlannedIntegration 做路径和去重核对 | “服务器整体无路，因此绕过 plan 修旧支路” |
| RULE 加强 D4 | 明确单一结算所有者；多入口共用 command/context 与原幂等键 | 把回调返回、APPLIED 或实体不可见直接当完整清除 |

原 D 系列不变量和 RULE 加强 D 块不是同一编号空间。上述 D1–D4 只指[原 RULE D 块](MS-MSITEM-PLAN.md#orig-rule-l0798)，不重定义 L6:D1–D4。

<a id="v13-lineage"></a>
### V1.3 ④｜修改：共享解析证据，攻击与拦截保留分工

本轮复核四处 Java 命中实际在 `StrengthOptionKey`、`AttackAction` 的两个依赖声明、`ProfilePresetMatrix`，不含三本书。它支持“键与策略声明存在”，**不能单凭四个命中证明没有任何间接执行**；本轮未定位该键专属的完整解析→执行→结果闭环，不因此发布支持。[源码/检索范围](../reference/R66-V13-REVIEW-EVIDENCE.md#lineage-actions)

**当前设计选择：** 共用只读的 class/defining loader/实际注册产物证据解析器，但**不合并两个能力的授权或执行开关**。`ATTACK_TARGET_CLASS_LINEAGE` 给当前目标操作提供可验证的逻辑承载/来源关系，必须由实际攻击动作及其结果端口消费；`INTERCEPT_CLASSLOADER` 决定类定义/装载边界的拦截，不能用拦截触达证明攻击生效。二者共享同一事实与失效版本，保留攻击/拦截两页分工。

从当前 SubjectKey/资格对应的精确对象出发解析，每一跳记录对象/类身份、定义 loader、代次与证据；包名、父类、loader 相同及新增常量池引用都不能单独授予归属。遇到隐藏类/匿名类、无法枚举的中间跳或已失效引用时显式保留未知，不以父加载器树代替完整来源链。只沿当前结果需要的已证关系展开，跳数/节点/字节上限由①记账，截断时保留未决范围而非假称完整。该预算实现是执行阶段前置，不要求现在捏造上限数字。

[MAX 加强表的 D10](MS-SKILLS-PLAN.md#orig-max-l0157)场景是必要验收样例，不是唯一目标白名单；不能按具体 Mod、作者、包名或玩家身份硬编码选择器。“不针对”不等于禁止处理当前合法选择的具体游戏对象。解析只读并不能单独证明后续执行合规，执行端口仍过原资格/结果边界，不改受禁文件、不起外部进程。

**前置：** 与 classloader 的职责边界在本页已设计划分；共享解析、消费者及结果证明尚未接通。**关键观察：** 对不同匿名/隐藏类生成路径产出可追溯链、同名不同 loader 不合并、证据过期撤销、攻击与拦截开关分别生效。只有解析图而没有消费与结果不算完成。放入第二批。

<a id="v13-settlement"></a>
### V1.3 ⑥｜修改：在既有守卫上新增结算端口唯一性检查

`TerminalOperationPlan` 已含 `ownerIdentity`、`commandId`、`planRevision`、`effectKey`，`PlanExecutionLease` 已检查线程/计划与有效期；`OperationPlanRunner` 已检查重入、Root claim、effect key 和最后写入权限。因此原稿“断言现在不存在”不能扩展成整个结算没有运行时守卫。[现有计划](../reference/R66-V13-REVIEW-EVIDENCE.md#operation-plan) · [租约](../reference/R66-V13-REVIEW-EVIDENCE.md#execution-lease) · [执行器](../reference/R66-V13-REVIEW-EVIDENCE.md#operation-runner)。技能书的“单一最终转换”是转换链要求，不能直接充当业务结算唯一性的证明。

**fix8精度修正：** owner/lease/claim是基础，**不等于“恰好一个结算归属者”的现成实现**。现有Root写令牌排他、每域单OwnershipBinding与一次性effect claim均保留；本轮所读端口未定位这个独立断言，应写为**新增检查**，而非只扩展既有断言覆盖。零个`settlement`类型声明只是辅助词法证据，不能单独证明全仓无等价逻辑。`ownerIdentity`是受构造器检查的String，源码未见`RootOwner`类型；不据此另建权威身份。[计划/lease/fence/claim原行](../reference/R66-VERIFICATION-CEILING-REVIEW-EVIDENCE.md#operation-plan)

**当前设计选择：** 复用现有Root发行的计划归属、计划代次、effect key与claim身份，在各个结果提交端口前**新增**当前计划恰有一个有效结算归属的检查；不同步骤各有执行端口，不等于每步另立一个 Authority。新加入的表面只能消费同一计划下的资格/预算切片，不得领取第二个结算所有者。

缺 owner、冲突 owner、过期代次或不匹配 claim 时返回既有 FENCED/拒绝回执并记录原因，不靠可被关闭的 Java `assert`，不以崩溃宿主替代 scoped failure。并发竞争的领取/复核与实际提交必须沿现有权威串行化或原子边界完成；只检查一个普通 Map 的当前大小不够。`COMMIT_UNKNOWN` 查询/恢复沿用原 command id 和 effect key；同一 claim 重入是恢复或去重，不自动误判第二位 owner；未知副作用不能靠换 ID 重放。

**前置：** 原计划/租约/claim基础可见，结算端口唯一性断言本身及跨端口覆盖/诊断均待实现；①不是它的硬前置，但⑦必须等其选中路径的归属检查与①成本账均有运行证据。**关键观察：** 无归属、双归属、并发竞争、过期 lease、同 command 恢复和跨表面重入均有明确回执，正常受限路径不误报。归属唯一不证明结果正确。放入第一批。

<a id="eight-s2"></a>
### 八方向 S2｜原计划与租约附带声明式写集，和C13并行推进

**fix11具名接缝与实际侧基线：** 写集依附原plan/lease，并通过`WriteSinkId`、`WriteSinkEvidence`、`WriteSinkExtension`对接最终写边界；沿`VersionedExtensionCodec`的版本/注册合同扩展所需载荷，不并列再造写边界身份。接缝的“production … begins in 7.17”是阶段意图，不是生产已完成；构造record/非空hash也不证明发行来源，真实出口仍复核原Root批准、对象、epoch/revision和当前lease。[接缝](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#sink-evidence) · [codec](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#extension-codec)。

`NbtWriteReferences.Snapshot(complete,entries)`、`StorageMutationSequence`和已插桩存储观察已有，且`RecoverySourceTracker.writtenPositionsMatch`已经按真实对象/复制来源，把writer实际路径与encoding.positions比对。**不重建这一遍历或位置比对**；缺的是允许表面/对象/操作的声明及Root下最终出口的统一约束、越界/未知回执和失证链。既有比对是保存位置/来源一致性，不是通用授权写集。[实际遍历](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#writer-traversal) · [现有比对消费者](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#writer-path-comparison)。

该Snapshot仅覆盖受支持、有界NBT遍历，entries主要保留compound对象弱引用与路径；不代表实体记录、追踪或Native任意写均已被观察。`complete=false`、引用已失效、插桩缺失/版本不稳是未知，不是空写集通过；序列戳从不锁住其他缓存或授予写权。其他表面继续用原执行端口补actual侧，不因复用NBT组件而抬高全局覆盖。

现有 `RecoveryMutationIntent`、`TerminalOperationPlan`、`PlanExecutionLease`、EffectKey/claim已有对象、资格、版本和槽位约束，不能说此前任意写入都无限制。[原计划与租约](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#existing-plan-lease)。待补的是跨选中mutation outlet的统一声明、实际写入回执及差异处理，而非另造Root、SubjectKey或独立批准者。

**第一批选定动作先落地、第二批扩覆盖：** 声明随原plan生成并绑定到原lease的plan/contract revision，描述目标现有身份/epoch、具体对象或登记集合、surface/outlet、允许的操作及有效期/取消条件。`WriteSetLease`作为方向名保留；具体实现优先扩原计划载荷与lease校验，不创建绕过旧授权的另一种可自行构造通行证。原有攻击/拦截/防护开关分别授权，不借共享写集把权限合并。

真实写端口在提交前核对声明和现行Root资格，执行后记录attempt、确知的写入及未知/部分写入范围，按同一operation/plan/epoch比较declared与observed actual。无声明端口尝试应在可检查边界拒绝；观察到声明外写入或无法确认结果时限制本plan/相关能力范围、保留未决effect，不能用“检查失败”抹掉已写入事实，也不能盲目回滚到旧世界状态。正常传递的目标关系清理须纳入声明的合法关系范围，不硬编码第三方名称。

**C13不构成开发先后阻塞：** 写集合同和端口自检可独立实施，C13并行定义其证据消费接口。但声明本身只是“允许写什么”的合同，不是“已经会写且被守住”的证明。C13引用的应是声明绑定＋真实端口执行/拒绝＋结果见证/覆盖范围＋现行版本，而不是manifest非空即签发ACTIVE。只接了N个端口就只发布这N个端口的可证明范围；未观察的副作用/出口记未知，不宣称能发现任意JVM内存写。

<a id="eight-s3"></a>
### 八方向 S3｜推广已有累积观察与独立读回，不新造四级状态机

**fix11完成接缝与回执纪律补足：** `CompletionContractId`/`CompletionContractExtension`/`CompletionEvaluation`是应优先对接的既有完成抽象；保持Id的non-CLEARED范围、原OperationReport/EffectRecord与Truth分离。读回结果由原Root/policy完成合同消费，评价仍为证据，不把`completionSatisfied`字段自行构造成功当作可信认证，更不替掉CLEARED语义。[既有完成接缝](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#completion-id) · [评价载体](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#completion-evaluation)。

`DeferredReceiptBuffer`已有“先reserve结果保留、执行后complete、flush只重试发布”的纪律；tracker在原Root mutation intent准备后、实际写前留票，异常/关闭保留原未知意图。跨域推广沿此模式和原累积观察位，不把失败发布变成重做mutation。该类当前包内可见，适配/提取不得顺手公开授权面；独立读回仍不变成自己的持久提交或归属凭证。[缓冲器](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#deferred-receipts) · [实际调用顺序](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#reserve-and-mutate) · [只发布结果](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#publish-receipts)。

`RecoveryMutationIntent.Observation`的WRITE_OBSERVED/WRITE_UNVERIFIED/NOT_CURRENT/NOT_EXECUTED是**可按位累积的事实**，不是REQUESTED→EXECUTED→OBSERVED→VERIFIED四个互斥阶段。`observations()`按OR追加；`acknowledgedWrite()`要求observed且没有unverified。[完整模板](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#memory-observations)。不确定写不能被后来的成功覆盖，后来的拒绝重试也不能抹掉早先写入；`This covers memory only`及`task-and-save-unproven`必须保留。

**第一批覆盖所选动作、第二批推广：** 提取的是事实累积和“执行/观察/认可分离”的纪律，保持原位值、evidenceRef格式和旧恢复语义，不直接把原enum改名迁移。执行器可以产生尝试与执行回执，但最终结果另由登记的观察路径读回目标状态、版本及所选完成合同；witness绑定同一plan/effectKey、目标epoch、读取表面/时刻/来源及不确定性。已有内存观察也不能自动升格为保存完成、任务退出或双客户端可见。

四层展示（请求、执行、观察、验证）可以作为**派生视图**，不能替换保留失败/未执行/陈旧/未知的底层模型。第二读取路径不只复制执行器返回值或同一陈旧缓存；静态独立不等于同权限安全隔离，范围不足就报告仅该范围观察成立。并发换版本、重插/重建、迟到receipt以及先不确定后成功的组合继续不被写成最终成功。S3为S2和C13提供更强结果证据，但不要求全部表面推广完成才交付一个已有可独立读回的局部动作。

<a id="eight-a3"></a>
### 八方向 A3｜推广既有Shadow比较纪律，保持领域结果与副作用分离

**fix11影子基线补足：** `ShadowVerification.Status`已有十态，且accepted只包括VERIFIED/MIRRORED/RECEIPT_MIRRORED，matches仅VERIFIED。复用这套精确/非精确接受的纪律和适配模板；持久根专用realm/hash/receipt字段不机械变成所有Native/实体能力的唯一enum。泛化影子仍须不发许可、不产生真实effect，不在无oracle时承诺误判率。[原结果模型](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#shadow-results)。

已有 `ShadowDurableRootStore` 和 `ShadowVerification`，后者10种状态并有accepted/matches/rejected：只有VERIFIED代表精确匹配，MIRRORED/RECEIPT_MIRRORED是接受但非精确。[源码](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#shadow-result)。通用影子机制复用这种区分，不为已有持久影子重造结果模型，也不把REALM_MISMATCH等专属名称无条件复制给原生绑定或动作执行。

接原第三批/条件扩展：对选中的纯判定入口使用同一scope/revision的只读输入快照，记录影子决策、实际决策、可观察结果和未知原因；影子侧不修改目标、不替换native绑定、不签真实写lease、不提交effect、不产生第二个权威状态。采集本身的回调/日志/内存成本记入①，不声称零副作用；若接口无法隔离出无控制副作用的评估路径，就先不做该接口的shadow，不拿真实执行后补偿冒充dry-run。

当前结果模型作为模板，定义各表面的领域适配并保留旧序列化/判断含义。没有独立或人工标注基准时，只报告差异、未知和成本，不把分歧率当误判率、不凭“几次无差异”自动升级STRICT_ALL。正式启用仍经C13的范围/证据和原配置策略；shadow任务失败只影响依赖它的新增路径，不把既有结果改成失败或成功。

<a id="surface-api"></a>
### 同权限清点 C：收敛 API 表面，授权落在真实变更处

**复核事实：** 71 处显式 `ServerRuntimeRegistry.runtime(...)` 调用属于8个其他包，bootstrap同包为0。缺少 module-info.java 只描述源码，不证明宿主运行时没有任何模块封装。`invalidatePasses()`、`clearIdentity(UUID)`、`release(server)` 方法体没有操作级授权；公开转发器和 runtime 返回的可变管理对象仍可绕到它们。字节码封印不能防一个正常 public 调用造成的状态变更。[调用分布与方法体](../reference/R66-SAME-PRIVILEGE-REVIEW-EVIDENCE.md#runtime-calls)

**第一批先处理高影响变更入口：** 拆只读诊断视图、受权请求提交、生命周期清理三个职责；不要继续通过门面直接返回全权 runtime/store/command bus。为既有跨包调用迁移受限入口，再缩可见性；仅改修饰符既不能构成同权限安全边界，也会使当前调用失去访问。类/包名、栈字符串或“来自本Mod”的自报字段不是独立授权。维持攻击、拦截、防护各自开关及范围，不为收门面合并权限。

**原§7动作2的授权设计：** 每次清准入/摘身份/关闭runtime核对正确的server-runtime实例、原Root批准的操作范围及当前revision/epoch/状态，复用原租约/计划模式并在实际变更处检查；不得只接受任意 `RootExecutionFence.Lease` 类型对象。Fence可公开构造，正确模式还必须绑定真实发行实例及当前活动租约，不能把另一个Fence自签的Lease认成权限。

`release` 属于宿主生命周期，需能认证合法关闭且幂等的生命周期通道；不强套一个普通实体terminal租约导致停服或异常清理死锁。保留停止接收、排空/回收、落盘等原顺序和错误汇集；`getIfPresent` 的诊断/清理不应初始化新runtime。审阅 `integrateLifecycleOnce(..., Runnable)` 及其claim入口，防止未经批准的任意回调借合法生命周期scope执行。第一批修这三类高影响路径，剩余accessor按消费者逐步迁移，不把71处大重构变成每项接线的全局前置。

**残余边界：** 门面、实例资格与封印是减少未经授权入口和可用绕行的加固，不保证抵挡已可读写/替换相同信任域关键代码和状态的任意对手；这与L6-INV-10一致，但不将“同权限存在”当作取消防护的理由。验证用自有通用夹具，不按特定Mod身份特判。

<a id="surface-durable"></a>
### 同权限清点 D：持久镜像认证与密钥治理（条件接入原持久化工作）

当前可见后端使用无密钥SHA-256，magic/长度/容量、原子替换与digest比较的已有作用保留；它们验证格式/损坏，不证明授权来源。原生bcrypt当前Hash路径也未提供HMAC。此结论限定收到的源码与路径，不从几个关键词推断整个宿主或第三方库。[存储摘录](../reference/R66-SAME-PRIVILEGE-REVIEW-EVIDENCE.md#durable-checksum)

**原§7动作7保留为条件工作，不删除、不伪装已实施：** 在原Root自有持久化链评估带秘密密钥的认证封装，明确认证范围、版本、世界/Root上下文和原commit/epoch、格式迁移、密钥获取与保存/轮换/丢失恢复策略。不能只替换digest函数，不能从旧无密钥镜像现签一个MAC就把其历史真实性抬为已证明，不能因密钥丢失静默接受任意镜像或重置既有终结/身份真相；失败状态与人工恢复策略应明示。

认证不自动抗旧有效镜像回放；回滚仍走原epoch/提交与恢复合同，不新造第二真相源。运行期随机密钥不能仅凭读源码推导，但同权限参与者若能取得密钥/替换验证器，就不能宣称隔离。没有不可篡改密钥根的证据时，仅说明具体威胁范围和残余风险。密钥和镜像限于获准的项目自有资源，不修改依赖/Mod/Forge文件、不删对方文件、不启动外部服务/进程。Native保持校验/密码等原语职责，不复制业务裁决。

第一批先给当前持久证据标清“checksum非认证”并进入⑨报告；在第二批原持久化变更中，密钥生命周期与迁移设计成立后接认证实现。它不被偷加为全部Server能力的新必需闸，亦不从Extreme目标清单移除。拟验证范围见下表；本轮未选择具体key provider、未提供新的运行保证。

<a id="native-negotiation"></a>
## 原生能力申请：最先补的是可验证接线

**fix5 融合来源：** [增补 V1.2 §2](Ronova_Enhancement_Addendum_V1.2.md#handoff-original)与[交接说明 §3](../reference/Ronova_Enhancement_Addendum_MERGE-NOTE.md#handoff-original)。重复的五条 JVMTI 方向、C13 三层接线与实施顺序继续共用本页原章节，不另立一套规划。新增静态摘录见[原生申请与任务清理证据](../reference/R66-NATIVE-NEGOTIATION-EVIDENCE.md)，逐节处置见[融合映射](RESTRUCTURE-MAP.md#fix5-addendum-merge)。

[当前 r66 原生入口](../reference/R66-CAPABILITY-AUDIT.md#native)只条件申请四个字段：`can_retransform_classes`、`can_retransform_any_class`、`can_generate_all_class_hook_events`、`can_access_local_variables`。本轮未测 actual capabilities，也未新增 `Agent_OnLoad`。

<a id="native-current-state"></a>
### 四个申请字段、回调与回退：当前事实分开记录

以下行号指原 r66 的 `native/src/ronova_native.cpp`，不是新增实现。`CAP_*` 为 Ronova 自定义 mask，不等同 JVMTI capability 集合；回调安装、事件启用与功能证据也不是同一件事。

| 对象 | r66 静态现状 | 本轮采纳的接续工作（待实施） |
|---|---|---|
| `GetPotentialCapabilities` | L819 未检查返回值，随即读取 `potential` | 保留错误码与阶段；探测失败不解释为“已确认不支持”，也不继续将该输出当有效能力集合 |
| `can_retransform_classes` | L820 从 potential 复制；L824–825 以整组申请成功且 requested 此位为真设置 `CAP_RETRANSFORM`；未回读实际持有集 | 独立记录核心重转换的请求/获授/错误，保留首次 ClassFileLoadHook 启用前的次序前提 |
| `can_retransform_any_class` | L821 **一个申请语句、同一行两次字段拼写**（读 potential、写 requested）；无后续独立读取、获授验证或分位诊断 | 纳入逐项获授清单；不能凭这一次赋值宣称 B7 已完成，亦不能凭字面引用数证明 VM 内部不消费它；详见下节 |
| `can_generate_all_class_hook_events` | L822 从 potential 复制；未有该字段的独立获授记录 | 与回调表安装、ClassFileLoadHook 的 enable 返回值分开记录，不以 `CAP_HOOK` 替代此能力获授证明 |
| `can_access_local_variables` | L823 条件申请；L966–975 的接收者核验已有 `MUST_POSSESS_CAPABILITY` 专用回退 | **保留已有回退**，把当前选中路线及其前提接进 C13 诊断；不是把四项全部变成硬依赖 |
| `GetCapabilities` | 所核原生源码树无该调用；实际持有集合未独立回读 | 申请前后读取实际集合；读取失败单独记录为未知，不能把 requested 或旧 mask 当 granted |
| `SetEventCallbacks` / ClassFileLoadHook | L828–835 安装整表并检查 hook 启用；两者都成功才在 L836 设置 `CAP_HOOK` | 拆开记录两次 API 的结果，保留所有既有槽位与条件编译分支 |
| `ThreadEnd` | L830 有 `task_thread_end` 槽位；L837 启用结果被忽略，且只在前述 hook 分支成功时尝试；无独立公开状态 | 增加事件级启用/清理状态与失败原因。未尝试、尝试失败、已启用但未观察到退出，不能合成一个成功位 |

上述四个申请字段没有四份独立的支持回执。`CAP_RETRANSFORM` 反映一个组合条件，`CAP_HOOK` 反映安装/启用分支；二者不能解释所有单项状态。也不能反推成“AddCapabilities 一失败，ClassFileLoadHook 就必然没有安装”：后者的尝试在源码中另有控制流。

<a id="native-any-class"></a>
### `can_retransform_any_class` 与 B7：保留问题，不扩大静态结论

**交接原说法**是“只写不读、申请后闲置；B7 未实现；缺少该能力不会影响任何现有路径”。本轮将可直接核实的部分采用为：**Ronova 没有对该位做后续独立回读、诊断或专用闭环证明**。右侧 `potential.can_retransform_any_class` 本身就是一次读取，因此“零读取”只能指没有后续的获授读取，不能按字面成立。

**外部核对补充（非交接原结论）：** 该位扩展的是 `RetransformClasses` 对可修改类的使用范围，并依赖 `can_retransform_classes`；VM 可在 API 内部使用能力状态，不需要 Ronova 再写一处同名字段读取。[JDK 17 RetransformClasses 可选特性](https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html#RetransformClasses)。原 r66 已有多处 `IsModifiableClass` / `RetransformClasses` 调用，见[对应摘录](../reference/R66-NATIVE-NEGOTIATION-EVIDENCE.md#retransform-paths)。

因此 **RULE/DS 加强 B7 继续列为待实施/待证目标，不因这次融合宣告完成**；但其完整实现状态不能单凭一次字段检索裁决。保留“外部转换后检测 → 对合适的类重转换 → 结果核验”的工程目标，逐入口核对已有能力后再补缺口；不删除这项申请，也不承诺拿不到它时所有既有路径必然不受影响。每个实际类的可修改性、API 结果与目标效果仍需分别验证。这是对交接推论的**显式收窄**，原句可在保留版本中追溯。

<a id="native-local-fallback"></a>
### 局部变量能力缺失：已有回退不能被重构掉

V1.2 §2.3 的正面发现一并融入，即使 MERGE-NOTE 的“四条”没有单列它。r66 对 `GetLocalObject` / `GetLocalInstance` 返回 `JVMTI_ERROR_MUST_POSSESS_CAPABILITY` 且 `g_task_depth > 0` 的情况，使用经既有边界捕获的最内层任务帧；**不是对任意错误一律放行，也不是任意捕获对象均可信**。

当前谓词保留原样的五个核验条件：`!g_task_cycle`、已验证位覆盖当前 `bit`、`captured.changes == g_task_changes`、`captured.boundary_bit == bit`、`IsSameObject(captured.receiver, expected)`。外围还有候选方法/loader 核验、进入深度条件与“不得让更深父任务冒充最内层任务”的 `break`。捕获、正常退出和线程退出清理须一起看，不能只摘一个 `IsSameObject` 作为通用身份凭据。[L87–108、L942–980 摘录](../reference/R66-NATIVE-NEGOTIATION-EVIDENCE.md#task-fallback)。

**首批拟交付：** 将“局部变量读取”与“已认证 callsite 捕获回退”作为同一受限任务功能的不同实现路线报告；记录当前路线与失效原因，不新增全局权限，不把回退成功解释为 `can_access_local_variables` 已获授。能力缺失而现有回退成立时继续保留该局部路径；回退前提不成立则拒绝该次身份资格，不能自动提升全局 key。

<a id="native-thread-end"></a>
### `ThreadEnd`：补启用与清理证据，不制造新能力位

`task_thread_end` 在 L106–108 释放当前线程剩余的 weak global refs 并清空帧；L97–105 的 `native_task_exit` 另有正常配对释放。**缺少 ThreadEnd 的启用结果记录，不等于所有正常退出清理都不存在**。这里登记的风险是：若该事件路径未建立，剩余帧的线程退出兜底无法由该事件保证，且当前观察接口未报告这个缺口；本轮未复现泄漏或运行故障。

[JDK 17 ThreadEnd](https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html#ThreadEnd)不要求新增一个 `can_generate_thread_end_events`。拟新增的是 **Ronova 的事件/清理状态回执**，不是虚构 JVMTI 字段。`taskObserverState0` 当前只有 bridge/verified/generation/changes/cycle 五项；接续时按实际 bridge 协议演进，把 enable error、清理可用性与触达证据接给同一消费者，不能仅在 native 内加一个没人读取的 bool。

首批应保留 `native_task_exit` 正常释放、`task_thread_end` 剩余帧清理，以及共享回调装配；分别处理注册失败、hook 分支未进入、ThreadEnd 启用失败。缺少退出清理前提时，不继续无依据接受依赖该前提的新捕获；可以采用经验证的等价清理路线，否则只撤销受影响的任务 scope/证据，不一并停掉不依赖它的其他功能。具体替代路线仍是待设计项，不把日志打印等同于资源已经回收。

<a id="native-add-capabilities"></a>
### `AddCapabilities` 的整组失败：补外部核对，保留现场未知

**来源状态：** V1.2 §2.1/附录、MERGE-NOTE §3.4 因当时无法访问规范而标为待确认；这个历史事实不改。本轮另查 JDK 17 规范和带版本标签的 OpenJDK 17u 实现，不能把新核对写成原作者当时已经确认。

**外部核对结论：** JDK 17 规定不可申请的请求会返回 `JVMTI_ERROR_NOT_AVAILABLE`，原有能力保留；所查 OpenJDK `jdk-17.0.16+8` 的 `add_capabilities` 在更新集合前先检查整组 desired 是否落在 potential 内，超出时立即返回。对这条不可用错误路径，**本次请求没有部分新增；此前已有的能力也不会被清掉**。[JDK 17 AddCapabilities](https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html#AddCapabilities) · [版本标签实现 L225–255](https://github.com/openjdk/jdk17u/blob/jdk-17.0.16%2B8/src/hotspot/share/prims/jvmtiManageCapabilities.cpp#L225-L255)。这是所查实现的结果，不替用户当前 Temurin/HotSpot 二进制做现场验证。

**对 r66 的条件性影响：** 请求位本来来自 `potential`；一个能力被正确探测为 `0` 时，对应请求也为 `0`，**不能据此说“可选项不支持就必然拖死另外三项”**。但是一旦整次 Add 调用返回错误，L824–825 确实不设置 `CAP_RETRANSFORM`，L839–845 的组合分支也会跳过。原代码未区分“探测失败”“未请求”“申请失败”“已有能力但本次申请失败”；本轮没有证明用户环境发生过其中任何一种。

**首批拟交付：** 按有依赖关系的核心能力组与可选组组织请求，不盲目把相依位拆散；记录每次 API error、请求集合及前后获授集。核心重转换前提先处理，可选申请失败不删除已确认的核心能力。发生整组错误时应保留整组结果，必要时重新探测并有限拆分诊断，不能从一个错误码猜出是哪一位失败。回读也失败时明确标记未知，不沿用 requested 冒充 granted；未知前提不发布支持。这一故障隔离设计尚未实现，也不要求额外外部进程。

<a id="native-record-contract"></a>
### 首批记录链：申请、事件、路径、结果各归各位

拟按每项能力/事件分别记录：

```text
GetPhase + GetPotentialCapabilities（检查返回值）
  → GetCapabilities（读取已有集合；错误单列）
  → AddCapabilities（核心/可选依赖组；记录请求和各次错误）
  → GetCapabilities（回读实际持有集合，不猜部分结果）
  → SetEventCallbacks / SetEventNotificationMode 或具体执行入口
  → 所选局部路线 + 回退/退出清理前提
  → 真实触达与功能结果证据
  → 对对应 key + scope 发布；失证时撤销受影响范围
```

`GetCapabilities` 是实际获授读取口；类重转换能力必须在首次启用 ClassFileLoadHook 前处理相应前提。[JDK 17 能力管理](https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html#capability)。上述逐层 receipt 与发布方式是 Ronova 的拟实施设计，不是 JVMTI 自动提供的业务完成证明。

候选记录字段为 `phase`、`potential_query_error`、`requested_set`、`add_error`、`granted_before/after`、`granted_query_error`、`callback_registration_error`、逐事件的尝试/启用结果、所选回退路线、清理前提、作用范围与失效原因；这些是诊断数据设计，**不是本轮新增源码类型，也不是要把全部黑话放进 GUI**。某阶段未走到时记“未尝试”，不能填写成功零值。

注意 [SetEventCallbacks](https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html#SetEventCallbacks) 设置整张回调表：新增 native bind/动态代码回调时必须保留原 ClassFileLoadHook、ThreadEnd 及条件启用的 ClassPrepare，不可用另一份局部表覆盖掉已注册槽位。此处需在共享装配点合并，不另建竞争注册路径。

事件回调的日志/分析可进入有界队列，**但同步决策不能伪装成可延期执行**：native bind 的 `new_address_ptr` 只能在当前绑定时按已有有效快照决定；异步分析不得宣称事后阻止了已完成绑定。每个 API 的 phase/callback/thread 条件逐项满足，不写“只要在任何回调里都能用”。

“失败即拒绝”在本接线方案中指：拒绝把未获授/无效证据的路径标成已具备、拒绝缺少前提的执行请求；不自动把当前 Minecraft 进程或所有无关 Mod 停掉，也不回退到外部 helper。既有宿主与运行预算合同继续适用。

<a id="eight-a2"></a>
### 八方向 A2｜以现有build identity与capability mask扩展应用协议

**fix11已有协商补足：** 构建身份和capabilityMask均保留；`NativeBridge.load`的`enableCodeSeals0(id)`返回值在该调用点未消费，应接到原协商/诊断链，不外推成所有封印证明都缺失。所缺为ABI及版本化feature/callback合同；不把既有mask当不存在，也不以新协商绕过build id。`structureSizes`仅用于真正跨边界的数据布局，不要求凭空引入结构或第二业务实现。[调用位置与位合并](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#native-negotiation-existing)。

`NativeBridge`已有bootstrap返回的capability位、Root/完整性掩码合并和`capabilityMask()`；code-seal路径还读取构建ID，Native与`CODE_SEAL_BUILD_ID`比较。[桥接基础](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#bridge-build-and-mask)。这不是从零，没有`featureMask`同名变量不等于没有位掩码；当前build ID检查只覆盖其code-seal入口，Java未消费该enable返回值，也不是全桥协议兼容证明。

**实施拆分：** 第一批在变更任何桥接方法/回调格式前先确定该变化的最小合同版本与不兼容处理；其余可在原第三批扩展，不能把必要ABI前置机械排到最后。复用现有bit定义，区分静态声明支持、VM实际获授、事件已启用和已验证可用，避免给同一位重定义含义。候选应用层协商内容为合同/ABI版本、构建身份、feature及其版本、回调/记录编码、所需可选功能与兼容模式。

`structureSizes`只针对真的跨边界传输的二进制记录/原生结构，有则连同长度、字段宽度和布局版本校验；纯JNI参数不凭空要求Java对象的C结构布局。JNI接口版本只表示JNIAPI要求，不替代Ronova自有协议。[外部界定](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#external-notes)。旧库缺新握手入口须明确未知/不兼容，新功能不开；已有有效且独立的旧路径按已声明兼容合同保留，共享根协议不兼容则影响其所有依赖。不得静默绕过既有code-seal匹配或从远处启动helper更新DLL。

<a id="eight-s4"></a>
### 八方向 S4｜推广已有任务图失证链，补跨源连续性与重建

**fix11连续性基线补足：** `TaskEventRun`已经写明同任务、父/调度器/proof代际不跨越，只有精确配对exit累加完成；`RecoveryTaskObserver`负责先验证再合并。fix10已经纳入该压缩器，本轮只补源码入口和精确语义。通用跨源sequence/sourceGeneration/overflowGeneration沿此“不跨断裂”纪律扩展，不另造并行连续性概念，也不把局部run当跨源完整证明。[TaskEventRun](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#task-event-run)。

**独立补读修正：** 不只有一个dropped计数。`RecoveryTaskObserver.merge`在LOST变化、事件dropped变化、proof不足或changes不符时调用`graph.evidenceLost()`；已有inbox_full等丢失原因、局部队列诊断与顺序屏障。`TaskExecutionGraph`记录失证并禁止据此认定执行已退出。[消费者及队列](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#task-loss-consumer) · [失效动作](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#task-graph-invalidation)。因此S4是推广已有局部处理，补通用事件源/代际/序列协议，不是把一个裸计数器第一次接上撤销。

在第一批S1/A4所用源合同中预留连续性信息，第二批接通所选源，第三批随NativeBind/新源扩展：定义producer/source代际、同一排序域的sequence、丢弃/溢出代际与观察截止位置；序号在可能丢弃的有界队列之前分配，明确并发域、重排、重复和计数饱和的处理。现有TaskEventRun压缩连续事件时保留可校验区间与原changes/drop上下文，不能因合并或跨线程到达次序制造假缺口，也不能跨copy屏障重排。

缺口回执撤销真正依赖该源的proof/scope，接A4和后续A1；未知共享源范围不可假装精确只影响一个实体。溢出通知本身可能丢失，使用可读取的代际/丢弃状态和原有陈旧检查兜底。新一段序列连续不自动修复此前缺失事实：先按该表面重读基线/核对未决效果、建立新的有效观察边界，再签发新代际；不可消除旧S3未知回执。

序列证明仅覆盖已进入其计数域的事件，不保证检测被源头压制、回调尚未启用或记录器同时被篡改的事件。心跳/S1挑战与版本检查可以补沉默诊断，但不冒充完整观察。预算丢样仍是缺口，不能写成“主动降采样所以证明不断”；真正不需要逐事件完整性的采样型证明另写其语义，不能借名扩权。

<a id="surface-mixin"></a>
### 同权限清点 A：按注入合同补覆盖，不只追加类名

**已复算：** 24个去重Mixin目标名中，14个在16类MC原生名单、10个不在；6个Redirect中5个位于未登记的持久化目标。31个Mixin源类与7个JDK任务目标是不同清单。**在名单不等于每个注入已验证，也不等于本轮现场租约有效。** MC无自有原始镜像不代表无修复：现有桥会重施登记ASM合同；但没有完整原始回滚，不保证任意注入恢复。[24目标/31Mixin表](../reference/R66-SAME-PRIVILEGE-REVIEW-EVIDENCE.md#mixin-inventory)

| 原§7动作 | 接回原批次的范围 | 一项合同必须同时具备的环节 |
|---|---|---|
| 4 持久化线 | 第一批选中保存/NBT路径先闭合 `ListTag`、`NbtIo`、`DimensionDataStorage`、`SavedData`；其余第二批继续 | 真实方法/descriptor和字段/调用语义→合法patch/重施→Java verifier→native target/mask→证明发布及消费者→失效/结果观察 |
| 5 网络/服务端 | 第二批的实际网络/生命周期路径覆盖 `Connection`、`ServerGamePacketListenerImpl`、`MinecraftServer`；选中路径若第一批依赖就先补 | 合同关联所选connection/server实例与版本；普通网络调用及合法停服不误拒；共同入口不成为第二Root |
| 6 客户端状态登记 | 第一批报告明确 `Minecraft`、`MouseHandler`、`ItemRenderer` 未进入当前MC租约名单；后续随原客户端路径补 | “未纳入该租约/逐注入未证明”不等于完全没有其他保护；专用服不强载客户端类、不等待客户端完成才发server证明 |
| 8 Mixin自校验 | 第一批形成名单差分及失效入口，第二批随所选合同补行为核对 | 启动应用失败、注入存在但控制流被改、报告链/观察点失证分别记录；只读class资源或扫描常量池字符串不能证明运行方法有效 |

扩名单时同改 `RonovaTransformer.isTarget/shouldInspect`、目标合同的patch与 `SemanticGateVerifier`、原生目标/required/verified映射及消费者版本；**只向 SEMANTIC_TARGETS 填名字会出现无法变换或无法验证的目标，不能据此发租约**。签名、加载器副本、客户端/服务端加载阶段、变更代际及重复回调逐项对账。保留原CompoundTag等已登记合同，不将其余合同删除换成一张名录。

位掩码扩展先核对Windows LONG、符号位、Java传递及每个消费掩码；原稿“还剩15位”只能作保留符号位的容量提示，不当作已完成协议设计。封印类数/字节、重转换时间、事件频率继续进入①成本账，按现有宿主预算处理，不靠无界重转换循环维持租约。

`required/defaultRequire` 的现有加载失败与运行期自检要分开：必需注入未满足时不得通过改成require=0来静默伪装可用；本Mod尚未初始化的失败不能承诺由尚未运行的自检修复。Native已有失败计数/Java桥有失败原因，不能说“所有干预完全无信号”；未知观察位置和排在后续的转换风险进入⑨报告，不把hook返回的提议当作最终字节证明。JDK17与Mixin0.8.5的有限外部依据见[规范核对](../reference/R66-SAME-PRIVILEGE-REVIEW-EVIDENCE.md#external)。

<a id="v13-cost-model"></a>
### V1.3 ①｜接受：先做观测成本账，不冒充调度器

本轮确认预算枚举仍为六类，`OBSERVATION`一级预算（200µs/256）已经存在并被使用，新增目标不是重建观测类别；budget 包未定位 RuntimeSurface/事件源二级归集；已有 `WorkBudget`、`FrameBudgetGovernor` 与宿主资源预算继续复用。[源码依据](../reference/R66-V13-REVIEW-EVIDENCE.md#budget-categories)。原稿“回调最贵”“DISCOVERY MAX 没有上界”等不能当性能实测：MAX 是计数维度的原常量，时间和宿主预算另有约束。

**当前设计选择：** 保留六类一级预算，OBSERVATION（必要时 MAINTENANCE）增加 `(surface, source)` 归集，source 是受控注册标识，不接受任意对象/字符串制造无限维度。每源独立声明过载动作，表面提供汇总/公平份额；全体观测还有父预算总闸，子账不能各自扩张父总额。未知表面以明确未解析桶归集，不凭猜测归因。

每行记录配置允许的接收率/时间窗口、实测触发率、回调入口与后续分析成本、重入/线程/phase 限制、有界缓冲容量、丢弃/采样/关闭动作和测量证据。**外部事件频率不是我们可保证的上限**；未测成本记未知，不编造毫秒或频率数字。构建/自检拒绝缺少成本契约的新源，成本实测不充分时不发表已满足上线条件。

过载三选一仍按原稿执行：丢弃观测、降采样、关闭事件源，但只能作用于允许丢失的观察记录；相关丢失计数、未知窗口与证据撤销必须可见。同步 native bind 决定、最终授权、必须完成的资源清理不能被偷偷排队/丢弃后宣称成功；关闭事件源前先安排其依赖路径的收敛，不能边继续接收新 tag/捕获边丢清理事实。队列、计数、预算扣减本身也计成本。

**前置：** 依赖图上的根；规格选型成立，二级账、过载行为和测量仍未实现/实测。②与③的新事件源启用，以及⑦分配器必须消费此账，不让①反过来依赖⑦。**关键观察：** 缺成本行拒绝、超载动作计数、总额与子账守恒、未知/失证区分；不是“已做全局资源调度”。放入第一批。

<a id="v13-object-lifecycle"></a>
### V1.3 ②｜修改：对象回收事件只补观测，不决定持久终结

**外部核对与原稿分开：** ObjectFree 仅报告被打 tag 且已被 GC 释放的对象，回调只有 tag，没有可供重新访问的对象；不得调用 JNI，仅可调用事件允许的 JVMTI 函数。[JDK 17 ObjectFree](https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html#ObjectFree)。弱引用在弱可达时清除，可配 ReferenceQueue；它与对象实际回收不是同一事件。[WeakReference](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ref/WeakReference.html)。因此本轮**撤回采用**原提议“此刻进入 terminal 判定”以及“在回调返回前证明业务终态”的语义，不改原稿字节。

r66 的弱引用还用于取得仍活着的 incarnation；`DurableTerminalWatchSet` 同时读取 projection、JudgmentMark 和 SubjectIdentity，持久成员资格由不可变投影的tombstone/matches判定；弱引用只是活体候选之一，持久条目可由空弱引用建立。原稿②“持久死亡权威由弱引用轮询兜着”的前提撤回采用，剩余方向仅为补物理回收观察。transient缺席清理与durable成员资格分开，不提升弱观察为权威。物理对象被回收不能撤销/新建 Judgment、清掉 tombstone、推出重建源已清或颁发 CLEARED。[实际使用](../reference/R66-V13-REVIEW-EVIDENCE.md#watch-maintain)

**当前设计选择：** `can_tag_objects` 与 `can_generate_object_free_events` 分别探测、申请、回读，再与事件装配/启用及①成本合同一起形成这一观察路线的组合前置；任一不成立，就不宣称 ObjectFree 路线可用，保留弱引用/拓扑观察的当前功能。tag-only 只能如实报告部分机制获授，不给业务完整支持。

tag 为 environment 生命周期内不重复使用的非零账本索引，索引绑定既有 SubjectKey/Realm/incarnation 代次但不参与授权，不是第二身份。维护有界索引表、登记/退订、重复/过期事件和耗尽行为；到界不静默复用编号。回调只按预分配/有界结构记录标量事实，由合法线程消费并撤销已释放 incarnation 的**观察证据**、安排现有核对；这不是在回调执行完整业务。

六处 weak 使用逐项保留职责：L92/L117/L285 的存活对象绑定更新仍需弱引用；L434 保留非强持有字段；L449 的 null 占位不对空对象打 tag；L459 的 transient incarnation 按当次资格登记。对象换代须另绑 tag，不让旧回收事件伤及新实例；先解绑或对象已回收的路径只清理自有索引/计数，不能回调里对已释放对象调用 SetTag。environment 内 tag 独立，但不能据此声称其它同权限代码无法干预本进程。

**前置：** ①成本行、实际获授与 callback-safe 队列均待实现/实测，第四批条件启用。**关键观察：** 分别记录弱引用清除、VM 事件观察、队列消费与业务重新评估；只在声明的宿主调度/负载条件下验证收到事件后的处理延迟，不承诺 GC 发生时刻或绝对顺序；协商失败、事件溢出、世界退出和换代仍可收敛，索引不无界增长。不绑定语义不相关的 `RUNTIME_DYNAMIC_CODE_WATCH` 来“点亮空键”。

<a id="v13-jit-scheduling"></a>
### V1.3 ③｜修改：先测既有重转换成本，不承诺新增 JIT 调度器

原稿主动提供废弃出口，且本轮没有重转换频率/停顿数据，**当前不把调度器列为必做实现**。第一批只给现有重转换请求记次数、原因、已有耗时与重复合并量，纳入①成本账；只有证据表明“可延期的重复重转换”成本足以覆盖新增观察/维护费用时，才显式复审第四批的调度候选。低频或净收益不成立就废弃这个优化，不保留“技术可行所以必做”。这不取消既有 B4 的 JIT 观察方向。

**外部核对：** CompiledMethodLoad 可对同一方法的多个编译版本发事件，另有卸载事件；晚启用需考虑当前状态补发与信息不全。[JDK 17](https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html#CompiledMethodLoad)。未收到事件、视图被淘汰或条目过期都应记未知，不反推“未编译”。原稿“解释执行重转换几乎免费”不作为成本结论。

若成本证据支持重开：共享既有 SetEventCallbacks 装配，按 method/class 版本与 code 地址生命周期维护有界视图，并处理卸载、同方法多版本和漏事件；不只用一个方法布尔位。维护用途的可延期请求采用单调时间截止并记录合并，不用负载相关次数冒充时间保证。紧急的完整性/资格修复不能因“已 JIT”延后；到截止、视图未知或观测失效时回到原有效执行路径，不吞请求、不新造转换语义。

**前置：** ①为必要条件；成本收益数据本轮没有，故执行前置**未成立**。没有性能数字、没有已开启事件、更没有省时结论。关键复审材料是现有请求测量、净成本对照、延期截止实际执行及未知视图行为；不把这个研究门槛变成阻塞其它强度工作的总门。

<a id="v13-cross-surface-budget"></a>
### V1.3 ⑦｜接受：在原计划上统一分配与结算预算

沿用 [V26:D95](MS-SKILLS-PLAN.md#orig-v26-l0552)。budget包未定位按表面分配，但`root/api/TerminalOperationPlan`已有required/optional surfaces、reservedHeapBytes与`String ownerIdentity`，不能说所有计划载体都不存在；也不能称已存在类型化RootOwner。[源码依据](../reference/R66-V13-REVIEW-EVIDENCE.md#operation-plan)。新增内容是该计划的多表面费用视图与额度发放/回收闭环，不另造批准者。

**fix8先决工作：固定载体不是可变切片。** 当前构造器拒绝`reservedHeapBytes != 16384`，required固定为REGISTRY/TEMPORAL/DATA，optional固定为NETWORK/PRESENTATION；工厂、步骤顺序、completion/recovery合同也有定值校验。必须先沿原Root计划发行链明确：谁能选额度/表面、采用哪个计划/策略/合同revision、如何验证父预算可用，以及换代/取消时旧切片如何停止新消费并结清已发生/未知用量。不能只删构造器校验或暴露可写字段就宣布完成。[完整41行](../reference/R66-VERIFICATION-CEILING-REVIEW-EVIDENCE.md#operation-plan)

新合同保持原计划身份与提交边界；需要改变固定合同就显式登记可验证的合同版本，旧计划恢复/去重仍按原版本解释。旧租约到期只撤销继续写入，不能为返还预算强行释放可能暂停中的writer；未决消费不得当作未使用份额再次发放。不新造Root、SubjectKey或一套独立编号。这些是⑦第三批的前置设计，当前源码未解锁。

**当前设计选择：** ①成本账按现有 protected result/command/plan revision 汇总；⑥确认同一结算归属后，现有 Root-approved plan 发出预算切片。各表面可消费切片并上报实际用量，不能另从父池重复扣账；记录预留、已消费、未使用返还、已取消及未决项，重试按原计划/幂等键复用，不无限二次领额度。

在相同计划版本、输入快照与可用预算下使用明确稳定顺序；关键权威/修复保留份额，队列有界且延迟/拒绝有回执，不能固定优先级永久饿死未决范围。额度不足时明确削减允许缩减的工作或排队；必要结果条件没满足就保持未决，不用削减预算宣布完成。表面只能回到协调器报告或请求下一步，不能 A→B→A 同步互调形成修复循环；沿现有因果/重入守卫扩展检查。

**前置：** ①成本契约、⑥选中路径新增归属检查，以及上述固定计划合同的合法解锁/代次撤销均为前置，均尚未运行验证；第三批在它们有证据后接入。**关键观察：** 两表面竞争可验证局部不超扣，但不是八表面覆盖；再核对三表面循环、重试、取消、动态预算变化、关键保留份额与公平性。相同快照重复分配一致不等于所有异步调度轨迹相同，也不宣称全局最优。极限档不新增第三方兼容闸；预算维护的是执行真实性与不自耗尽，不削去原目标能力。

<a id="jvmti-directions"></a>
## 五条 JVMTI 方向：保留价值，纠正覆盖范围

本节沿用讨论里的“新 1–5”作为说明标签，不新增或重编号原 B/D 不变量。新 4 并入 B1；新 5 并入 B2/B3 的组合分析，不重复计算工作量。

<a id="native-bind"></a>
### 新 1：NativeMethodBind → 原生绑定证据与受证控制

正确字段是 `can_generate_native_method_bind_events`。事件覆盖 Java native 方法绑定（包括 RegisterNatives），可经 `new_address_ptr` 重定向；不是每次 C/C++ 函数调用。[JDK 17 NativeMethodBind](https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html#NativeMethodBind)。原“can_generate_native_method_bindings 不存在，所以真实事件已移除/不得申请”的推论撤回。

**拟实施：** 为启用后实际捕获的绑定建立 method 身份、loader/class 版本、原地址、线程/阶段与采样出处；早期阶段不能调用的解析操作延期，不冒用失效引用。JNI 的[RegisterNatives 契约](https://docs.oracle.com/en/java/javase/17/docs/specs/jni/functions.html#registernatives)仅是绑定事实，不是跨 Mod 归属凭证。

先发布可证明的“绑定观察”，再为已验证签名、ABI、归属和恢复路径的绑定提供同步控制。不能一收到事件就把整个 `intercept.jni_native` 标为完整。未捕获的既有绑定、解绑/重绑生命周期、原入口回退与控制后的实际结果均保留为明确缺口；不得按第三方名称决定控制对象。

<a id="dynamic-code"></a>
### 新 2：DynamicCodeGenerated → 有限的 VM 代码观察

该事件报告 VM 动态生成的原生组件；Java JIT 方法另看 CompiledMethodLoad。它没有控制能力位，不能据此观测任意 defineClass/Unsafe 写内存或第三方生成代码。[JDK 17 DynamicCodeGenerated](https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html#DynamicCodeGenerated)。

**拟实施：** 将此路归类为 VM 代码区段观察，与类定义/转换、JIT 方法及第三方原生代码分栏；保留每栏已观察/未知的边界。不把 name/address/length 单独当归属证明，不把 VM 正常 stub 当作目标动态代码。

`attack.dynamic_code` 的 Extreme 仍须实现与目标对应的 dispatch/result outlet 控制；本事件不会自动填平这个要求。原 [V26 must-reject](MS-SKILLS-PLAN.md#orig-v26-l2828)“只有观察却宣称完成”继续适用。现有 B4 的 JIT 观察也不能单独等同于 dispatch 控制已完成。

<a id="early-return"></a>
### 新 3：ForceEarlyReturn* → 条件化的当前 Java 帧补充

它操作当前线程或已挂起线程的当前 Java 帧，不能泛用于 native 帧，且跳过 finally；不是永久或批量 AllReturn。[JDK 17 Force Early Return](https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html#ForceEarlyReturn)。

OpenJDK 17u 的 `can_force_early_return` 在 onload 能力集合；`can_generate_native_method_bind_events` 在 always 集合。这是实现来源的分类，不是本机获授结果。[OpenJDK 17u capability implementation](https://raw.githubusercontent.com/openjdk/jdk17u/master/src/hotspot/share/prims/jvmtiManageCapabilities.cpp)。当前只有 JNI_OnLoad 的 r66 入口不能自动宣称已处在 Agent_OnLoad 阶段。

**拟实施：** 先选宿主已有的早期加载或其他实际获授路线；不能满足前提则该帧级路径保持不可用，不能通过外部 attach 子进程补救。采用挂起路线时，另外核对所需能力与线程恢复责任。只针对已证明属于当前操作的精确帧和匹配返回类型，记录清理/锁风险与结果；框架同进程不代表可无差别强退任意线程。

价值定位是补充已经执行中的旧版本帧，而不是替代现有字节码路线。标准重转换后活动旧帧仍运行旧版本，新调用使用新定义。[Instrumentation.retransformClasses](https://docs.oracle.com/en/java/javase/17/docs/api/java.instrument/java/lang/instrument/Instrumentation.html#retransformClasses(java.lang.Class...))。上述路线尚未实现或运行验证。

<a id="prefix"></a>
### 新 4 / B1：Native prefixes → 条件化 Java 包装入口

前缀支持 Java 包装方法 + 改名前缀 native 方法的解析回退；复数接口是同一 JVMTI environment 中的多组前缀，不是 classloader 参数。包装结构须先建立。[JDK 17 SetNativeMethodPrefixes](https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html#SetNativeMethodPrefixes)。

**拟实施：** 优先检查目标加载时机、包装存在性、前缀冲突、解析/重绑、原方法恢复和具体图形出口覆盖。涉及增加/改名方法的包装不能假定已加载类可以用标准重转换任意补入；参照 [Instrumentation 的重转换限制](https://docs.oracle.com/en/java/javase/17/docs/api/java.instrument/java/lang/instrument/Instrumentation.html#retransformClasses(java.lang.Class...))。

“不需要新增专用 Hook DLL”仅为待验证实现选型；当前 Ronova 已有 Native 后端，不将此路线包装成零 Native 依赖。B1 仍值得推进，但不能只把 capability 设为 1 就称 Render Firewall 完整。与 [BL 原 Native Render Firewall](MS-ENTITYDEFENCE-PLAN.md#orig-bl-l1939) 的范围一起审阅：它原本是可选 P1 客户端扩展，不提升成 FULL L6 Server 的必需项。

<a id="structure-evidence"></a>
### 新 5 / B2+B3：字节码与常量池 → 结构引用证据

`GetConstantPool` 与 `GetBytecodes` 的索引对应会被中间重转换/重定义破坏，原始布局也可能不同。[JDK 17 GetConstantPool](https://docs.oracle.com/en/java/javase/17/docs/specs/jvmti.html#GetConstantPool)。类身份还需名称与定义它的 loader，不能只看符号名称。[JVMS 17 §5.3](https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-5.html#jvms-5.3)。

**拟实施：** 将一组采样绑定到可核对的 class/method 身份和版本，若在窗口中变更则重采/失效，不把两代数据混用。对方法所用的结构引用做规范化比较，保留采样来源；不是只对原始常量池 bytes 求差。

新增 Methodref 不能单独证明恶意/归属；原池已有引用也可能被新路径复用，因而“不新增常量池项”不证明未修改。把证据送入现有 Candidate/MutationTrace，与真实变更和实际调用结果合并审阅；不允许名字、包名、字符串或同父类单独授权高副作用操作。E1/E2 的工作是证据分析/生命周期接线，而不是新增一套目标真相。

<a id="v13-line-number-research"></a>
### V1.3 ⑧｜废弃：本轮不建立行号表检测任务

原稿明确这是未经验证的假设，没有独立标注样本或判别力/误报率，允许整体废弃。**本轮决定：不进入实现批次、不申请能力位、不作为 Candidate 归属或改写判据，也不保留一个默认必做研究项。** 原文和拒绝理由保留在来源映射中，没有删除任何既存不变量或 B2/B3 工作。

[JVMS 17 §4.7.12](https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.7.12)将 LineNumberTable 定义为可选属性；仅凭缺失/异常形态没有本任务需要的实测判别依据。这支持“不直接采用”，**不证明该方向在所有样本上绝对无效**。本轮未收集样本、未计算误报率，不伪造成本高于收益的量化结论；当前没有足够收益依据来分配实现或样本建设工作。

以后只有提交与行号特征独立标注的数据、增量判别力和总成本证据，才能作为新的显式提案重审，不能因原稿仍在仓库就自动恢复。它也不能代替 B2/B3 的结构证据、真实变更或版本核对。**前置：** 研究输入未具备，当前不安排执行。

<a id="batches"></a>
## 当前实施顺序：端到端交付，不按 ACTIVE 数量冲进度

以下批次是本工作流的设计排序，**尚未实施**。没有要求必须完成所有批次才交付一项已有能力的接线，也不取消原 A/C/D/E/F/G/H 工作。

| 批次 | 工程范围 | 批次交付内容 | 明确不算完成的情况 |
|---|---|---|---|
| 第一批 | DS:C13 基础 + DS:C11 已存在局部链 + 原生协商/事件/回退接线；V1.3 ①⑥⑨；八方向S1、S2与C13并行、所选S3/A4 | 现有六键 proof、实体/NBT/任务/存储局部资格、执行器与诊断读同一事实；补四位申请的回读/失败隔离、ThreadEnd 状态与已有回退的诊断；修掉命令硬编码误报；补观测成本账、在既有守卫上新增结算端口唯一性检查/诊断及失效条件报告；保留8处观察消费，接局部scope发布与reason消费者；同权限清点补高影响API授权、所选决定侧封印/持久化合同、恢复证据发行信任核对及未保护范围报告；接所选scope的挑战、写集/实际回执与独立观察、已有状态/熔断，桥接变化先定A2最小合同 | 改默认 OFF；只多几个 ACTIVE；全局投影覆盖局部 scope；宣称整项 Extreme |
| 第二批 | DS:C11 + RULE D1–D4 / DS D 块真正缺失路径；C12 可并行；V1.3 ④⑤；S2/S3扩面、S4连续性与A1小范围依赖接线 | 逐动作补缺；计划单一路由；完成条件有真实生产者；三档对应真实策略；类血缘消费者；证书在现有版本链上新增独立有效期、声明式期望依赖及使用闸；扩余下API迁移、网络/服务端合同及Mixin自校验；持久认证在密钥/迁移前提成立后接入 | 重写已有新终结链；把局部回调当全链；按旧“13 个”机械造类 |
| 第三批 | 新 1 + B2/B3/新 5；①⑥有证后接 V1.3 ⑦；A1通用索引、A2协议扩展、A3只读影子及S4新源适配 | 原生绑定和结构引用证据进入具体能力，scope 与结果分层清楚；先定义固定计划额度/表面的发行权限、合同revision和旧切片撤销，再统一切片并关联原归属和completion | 获授/观测即拦截成功；常量池变化即归属 |
| 条件第四批 | V1.3 ②；③仅在测量证据支持后重审 | ObjectFree只补对象观察，保留弱引用和业务裁决；JIT调度无净收益则废弃 | GC等于terminal；未收到JIT事件等于未编译；无实测先造调度器 |
| 条件批次 | B1/新 4 | 先证明包装时机和解析，再决定客户端 Firewall 覆盖与依赖 | “prefix = 任意纯 Java Native Hook”；未经验证删除原依赖 |
| 后续专项 | 新 3；新 2 的有限观察用途 | 前者解决能力取得/帧适用；后者只发布实测 VM 代码观察范围 | 强退等于全返回；VM 事件等于任意动态代码攻击 |

第一批可先选一组注册实体动作，贯通请求、证据、实际执行与结果回读，再扩展恢复链。**本批次选择不取消其余能力项的最终目标。** 同权限清点的八条加固是现有路径的依赖/覆盖补足，不增加新批次。高影响API授权和实际选中路径的完整性先接，非相关路径的封印/合同扩展继续保留欠账；不将 C1/其他大面积重构偷塞进每个能力发布补丁，但遇到当前选中路径的真实前提就必须解决，不靠标签“可选”跳过它。

**批次依赖不循环：** ①只建成本/过载账，不要求⑦先完成；⑥在现有守卫上新增结算端口唯一性检查，不依赖跨表面调度；⑦消费①⑥。④的证据遍历和⑤的重采开销同样登记成本，但不使它们变成①的前置。③的成本测量不等于启用新JIT事件。⑧不在任何默认工作队列；既有 B4、动态代码/dispatch目标未取消。

<a id="eight-order"></a>
**八方向的排期校正：** 同意S2与C13并行、A4前移，不采用“先完成所有能力获证，再做所有写集”的串行总闸。第一批以一个真实动作范围为纵向切片：

```text
S1 复用现有验证、补所选scope的挑战/证据合同
  ├─ S2 原plan/lease写集与端口回执 ─┐
  └─ C13 同源scope发布/消费者 ─────┤ 并行定义接口，不互等全面完成
                                ↓
              S3 所选结果独立观察 + A4 状态/熔断诊断
                                ↓
        S4 连续性扩展 → A1反向索引 / A3影子适配
A2：变更桥接前先做该功能最小协议前置，其余扩展接第三批
```

S2可先验证本地合同，但C13不能从声明本身制造执行证明；S3可以复用已有观察与S2一同交付，不人为等到所有C13完工才开始。A4在已有逐键维度上接明确故障，不等全图/全源事件协议完成。原第一批工作已包含端到端结果；本次不是增加八个必须逐个停下来验收的新阶段。仍仅在关键代码批次验证，不新增方向编号、最高规范、Root或B/X编号，未改变V1.3九项回执及其原有位置。

### 首批代码落点（待实施）

| 源文件/模块 | 预定工作 | 与现有实现的关系 |
|---|---|---|
| `StrengthPolicySnapshot` / `RuntimeCapabilityProjection` | 引入可追溯的 producer/scope 视图；默认 OFF 不变 | 扩展投影，不重造权威状态机 |
| `TerminalActionPolicy` / `RecoveryChainPolicy` | 将局部边界事实暴露给共用诊断，保留逐对象/出口权限 | 复用已有局部通道 |
| `DominionCommands` / 强度 UI | 去除固定“terminalize 以外未接线”的推断；显示请求、范围与原因 | 文案来自真实 registry/receipt，而非另一份硬编码表 |
| `TerminalPlanExecutor` / `OperationPlanRunner` / `TerminalLifecycle` | 补选中动作端到端证据，核对同一 command 的去重和结算 | 不恢复旧非计划服务器写路径 |
| `ronova_native.cpp` / Native bridge | 逐项申请/前后获授/整组错误；ThreadEnd enable 与清理状态；保留认证 callsite 回退和配对释放；合并回调装配，范围化支持回执 | 不因新增回调覆盖现有能力，不引入子进程 |

新增落点均为待实施：`WorkBudget/FrameBudgetGovernor` 扩展二级账；`CompletionTracker`新增独立到期/期望依赖检查并保留原采样陈旧闸；`TerminalOperationPlan/PlanExecutionLease/OperationPlanRunner`新增选中结算端口唯一性检查，第三批再解锁合法的逐计划切片合同；原能力报告生产者与消费者携带失效条件。没有指定一套平行 Root、SubjectKey 或不变量编号。

同权限清点另涉及 `ServerRuntimeRegistry`、`DominionServerRuntime`、`SpawnAdmissionRuntime`、`ShadowDurableRootStore`、`RecoverySourceEvidence`、`build_code_seals.py`、三侧目标合同/验证器及 `DurableRootFileBackend/storage_transaction`：具体前置与范围在上面对应小节，均为待实施。

八方向新增接线落点仍是待实施：S1复用`CodeSealProbe`/Native验证与原proof发行；S2复用原plan/lease/outlet；S3从`RecoveryMutationIntent`与独立读回抽取纪律；A4扩展现有snapshot/receipt消费者而非先改enum；S4沿`RecoveryTaskObserver`/任务图失证和原成本账扩展；A1不替实体/任务图；A2复用`NativeBridge`与位定义；A3复用`ShadowVerification`比较纪律。

**fix11首批接缝落点补足（原批次不变）：** S1先读真实`RonovaTransformer`分派；S2在原plan/lease上关联`WriteSinkId/Evidence/Extension`并复用NBT实际遍历/位置比对；S3消费`DeferredReceiptBuffer`与独立读回，接`CompletionContractId/Extension/Evaluation`；A4在已覆盖模块的`CapabilityRuntime`内接熔断并显式映射稳定位与强度键。S4沿`TaskEventRun`与旧失证消费者扩展；A1先复核`CopyProvenanceIndex`反向表及loss接收端；A2/A3沿原合同扩展。此清单指实现前要复用的组件，不宣称接缝已生产注册，也不推算减少几倍工时。

这张表是实施任务，不是本 zip 已改文件清单。本轮交付没有这些源文件的替换件。

<a id="validation"></a>
## 新增接线正式验收判据：仍只在关键批次核对

**fix12效力调整（采用方案③）：** 下表原19行“需要确认的实际事实”是本接线工作流新增工作的正式验收判据，不再仅作未绑定的观察建议。条目身份采用“本页稳定锚点＋原行名＋适用文档版本”，不新编B/X或方向编号。原行文一字不改；一行含多个断言，不等于一个布尔勾选或一个测试。

执行时按[批次分工](WIRING-ACCEPTANCE.md#batch-binding)和[八方向绑定](WIRING-ACCEPTANCE.md#direction-binding)选中适用断言，用[证据与结论规则](WIRING-ACCEPTANCE.md#evidence-rules)判定所声明范围。原B/X负责原要求及受影响回归，不替代新增断言，也不被新增验收豁免。只在关键接线批次集中核对；本轮只修改文档/生成器，未运行以下场景，不创建任何通过记录。

| 批次观察点 | 需要确认的实际事实 |
|---|---|
| <a id="observe-request-policy"></a>请求与策略 | 保存配置不立即冒充active；内置极限已请求4不证明支持4；逐键/动作覆写、待重启、禁用、失证理由与执行一致 |
| <a id="observe-generic-observation"></a>通用观察消费 | 八处原调用保留observationRequested语义；执行位受限不错误关闭观察；写许可只能来自注册scope的发行/验证，不从观察位转换 |
| <a id="observe-expiry-sampling"></a>独立到期与采样陈旧 | 分别观察采样未中断但证书已到期、证书未到期但采样沉默、依赖版本错配、续期和最终提交竞态；两种闸不能互相替代 |
| <a id="observe-plan-slices"></a>计划切片换代 | 固定合同解锁有发行资格和revision；旧切片撤销、不双领/双还、未知消费保留、暂停writer不强制放锁 |
| <a id="observe-local-scope"></a>局部范围 | 正确实体/出口可以走已有链；换对象、换 epoch/revision、无关来源不能复用资格；全局状态不被局部成功抬升 |
| <a id="observe-execution-settlement"></a>执行与结算 | 真正进入目标执行器、结果有生产者；重复入口不双结算；旧服务器死分支不被错误恢复 |
| <a id="observe-native-prerequisites"></a>原生前置 | 实际获授、回调装配、事件启用、触达和控制结果分开记录；缺能力不启动外部 helper |
| <a id="observe-capability-failure"></a>申请故障隔离 | potential 查询失败不消费未知输出；可选位为 0 不被误写为申请失败；核心/可选请求的错误与前后获授各有记录，不能虚构单项失败原因 |
| <a id="observe-task-fallback"></a>已有任务回退 | 局部变量能力缺失时，只在原认证捕获条件成立的 exact task scope 保持路径；版本/对象/边界失配和其他错误不可借回退放行 |
| <a id="observe-thread-cleanup"></a>线程退出清理 | 分别核对正常 exit 与 ThreadEnd 剩余帧清理；事件未尝试/启用失败能被消费者看见；无清理前提不继续积累依赖它的捕获 |
| <a id="observe-host-side"></a>宿主/侧边界 | 原生事件风暴有成本/过载回执；专用服务端无客户端类加载；不按第三方身份特判。合法未选中路径作为作用范围检查，不新增“第三方必须兼容”的 Extreme 完成闸 |
| <a id="observe-same-privilege-api"></a>同权限API | 无授权、错server/runtime、错误scope、过期/异发行Fence租约、公开转发绕行被拒；合法shutdown/恢复/幂等清理不死锁、不初始化新runtime |
| <a id="observe-decision-paths"></a>决定侧与双路径 | 所选producer/preset/consumer依赖未登记或失证有回执；全局2与注册范围3/4分别呈现；不将本轮词法计数作为真实获授/攻击结果 |
| <a id="observe-recovery-trust"></a>恢复源信任 | 自报observerVerified、陈旧/错对象/错来源记录、提交中换版本等由真实发行与原Root边界复核；已有合法绑定路径仍能执行，复制深度不取消预算 |
| <a id="observe-mixin-contract"></a>Mixin合同 | 每个选中方法的注入/patch/verify/native mask/消费者同版本；分别观察未列入、未载入、失配、重施失败、观察位置未知；专用服与客户端合同不混 |
| <a id="observe-durable-auth"></a>持久认证（选用时） | 旧格式迁移、key丢失/轮换/错key、镜像截断/改写/回放及原子替换失败；保留原truth/recovery合同；digest或MAC有效不自动等于新鲜或可信提交 |
| <a id="observe-eight-directions"></a>八方向关键观察 | 挑战缺回调/旧nonce/旧代际/清理失败；写集越界、未知写及声明无执行；累积不确定性不被成功清除；状态/证据/新鲜度不串层；队列合并与gap代际、重读基线、共享依赖撤销；旧Native版本不误开新功能；shadow不产生真实effect且无oracle不报误判率。按所选代码切片核对，不本轮回填状态 |
| <a id="observe-baseline-reuse"></a>已有基线复用 | 写集Snapshot不完整/弱引用失效不当空集；既有保存路径比对不代替授权；完成Evaluation不凭构造晋升Truth；回执重发不重做mutation；copy source失证作用域不误当全局DAG；模块位与54键不混用、内部捕获不报恢复成功、重试异常路径仍有界；实际变更后的关键批次核对，本轮未运行 |
| <a id="observe-v13-observations"></a>V1.3 新增观察 | 成本账守恒与过载、无/双owner和同command恢复、依赖变更/采样竞态、隐藏类证据消费、跨表面竞争/循环、tag迟到/溢出/回退、报告失效条件；按本页对应条目在关键代码批次核对，不逐条制造开发停顿 |

双向入口：[新增工作与批次验收分工](WIRING-ACCEPTANCE.md) ↔ [原68项B/X规格与人工关系表](VALIDATION-TRACEABILITY.md#acceptance-scope)。新增判据按所选scope独立给出结论；原要求按原B/X另行给出结论。一个真实测试产物可以供两边使用，但必须分别匹配断言、版本、范围，不能传递或代填状态。原120行人工关联保持不变，未在本轮补做语义审阅。

[54键逐项收敛规则](WIRING-ACCEPTANCE.md#key-readiness)明确：39行通用“逐键清点”仍是待办，不是可被整体验收的既成实现。先完成所选键/范围的producer→consumer→outlet→结果→失效路径登记，再依据本表形成实际证据；未选范围保留欠账，不能抬升整项Extreme或整批完成。

关于真实台账继续保留 fix3 的来源区别：**用户工作区回报** 68 唯一 ID 精确一致，28 PARTIAL / 40 NOT_RUN；本交付没有收到原台账字节、逐项状态和运行证据，未独立复算。GAP-MAP-07 的台账集合/汇总子项按该回报已核，证据绑定未完成，整个缺口仍开放。

<a id="affected-originals"></a>
## 受影响原条款与 MS 归属

| 原来源/段落 | 本次处理 | MS 主归属 |
|---|---|---|
| [DS:C11/C12/C13](MS-SKILLS-PLAN.md#orig-ds-l1040) | C13 计数结论收窄、C11 清点纠正、C12 保留；不改变 ID | skills 总体架构/实现清单；msitem 动作与结果 |
| [RULE B 块](MS-SKILLS-PLAN.md#orig-rule-l0748)、[NON B 块](MS-SKILLS-PLAN.md#orig-non-l0102)、[DS B 块](MS-SKILLS-PLAN.md#orig-ds-l1021) | 撤回错字段推论；限定 prefixes；B2/B3 组合证据不等同归属 | skills Native 层 |
| [NON T0 判断](MS-SKILLS-PLAN.md#orig-non-l0057) | prefixes 的“完整拿到”不作为实现事实 | skills Native 层/完成标准 |
| [RULE D 块](MS-MSITEM-PLAN.md#orig-rule-l0798)、[DS D 块](MS-MSITEM-PLAN.md#orig-ds-l1066) | 纠正旧硬编码诊断与服务器不可达外推；接真实 plan/executor | msitem 统一上下文、混合执行、服务端队列 |
| [DS 实施顺序](MS-SKILLS-PLAN.md#orig-ds-l1126) | 本工作流改按端到端批次推进；未取消其他块 | skills 实现清单 |
| [V26 must-reject](MS-SKILLS-PLAN.md#orig-v26-l2828) | 不变；只观察不冒充动态代码/全返回完成 | skills 完成标准；msitem 结果 |
| entitydefence 规范实例与注册表 | 不新增第二对象/身份真相；局部能力不可冒充通用修复 | [实体防御主书](MS-ENTITYDEFENCE-PLAN.md) |

逐段就地提示清单见[静态清单的 original_body_external_annotations](restructure/capability-wiring-baseline.json)及[更新映射](RESTRUCTURE-MAP.md#fix4-wiring-supplement)。旧原文块原字节保留；新说明不能反向改写历史运行结果。

<a id="sources"></a>
## 来源、核对范围与版本说明

**fix11输入与处置：** [已有实现补遗](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-SUPPLEMENT.md)与[原ZIP](../archive/eight-directions-existing-baseline-supplement-r66.zip)原字节保留；[逐节映射](RESTRUCTURE-MAP.md#fix11-baseline-merge)、[源码消费者依据](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#findings)。本轮补已有基线、不改fix10决策/原批次/ID；作者计数和本轮机械枚举分开，类注释、静态路径与运行证明分开，没有外部研究填补或新的现场测试。

**fix10 输入与处置：** [八方向原核验稿](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md)与[ZIP](../archive/second-batch-eight-directions-verification-r66.zip)原字节保留，性质仍是Ronova侧发出的只读复核，不包装成新提案或新工程批次；[逐节映射](RESTRUCTURE-MAP.md#fix10-eight-merge)、[来源差异与独立源码依据](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#findings)。聊天与附件对A3的评价细节不同，以逐项来源分列，不偷改原文。除外部补充中明示的两处接口界定，本轮设计基于所给源码/报告；没有新的用户现场结果。

**fix7 新输入与处置：** [同权限清点原文](../reference/R66-SAME-PRIVILEGE-SURFACE.md)完整保留，原zip在[只读归档](../archive/same-privilege-surface-handoff-r66.zip)。A/B/C/D与八条动作按原来源顺序在[逐节映射](RESTRUCTURE-MAP.md#fix7-surface-merge)登记，不继承为新最高规范；本轮源码/外部复核与设计选择分列在[依据](../reference/R66-SAME-PRIVILEGE-REVIEW-EVIDENCE.md)。未提交新的运行证据。

**fix6 新输入与处置（保留当轮语境）：** V1.3 正文/发出说明完整读完，保持 Ronova 侧发出稿身份；[原文](Ronova_Enhancement_Addendum_V1.3.md)和[发出说明](../reference/Ronova_Enhancement_Addendum_V1.3_HANDOFF.md)不套外部原文块标记，收到的 zip 在[原字节归档](../archive/enhancement-addendum-v1.3-handoff-r66.zip)。逐节处理与拒绝/条件项见[fix6 映射](RESTRUCTURE-MAP.md#fix6-v13-merge)。本轮另读预算、watch set、completion、计划/租约/runner、键与动作；来源和外部依据均在[复核摘录](../reference/R66-V13-REVIEW-EVIDENCE.md)中分层，头文件存在、词法零命中、用户台账回报不混成实际获授/执行证据。

**fix5 新输入（保留）：** 本次增补交接包的 V1.1、V1.2、MERGE-NOTE 已完整读取；原 zip 保存在[只读来源归档](../archive/enhancement-addendum-handoff-r66.zip)，三份原文在来源保留页逐字节保留。重复内容映射到现有章节，新事实及推论收窄落在[原生申请](#native-negotiation)。本轮另读 r66 原生初始化、任务捕获/退出及四处重转换调用；范围与哈希见[本次证据](../reference/R66-NATIVE-NEGOTIATION-EVIDENCE.md)和[融合清单](restructure/enhancement-addendum-merge.json)。

**用户文件与 fix4 静态读取：** 原 r66 源码压缩包 SHA-256 为 `1266d8c2dd46b949178d7341ee467333be678fb9b373c1403b576a56c73fd763`。上文代码结论依[源码核对摘录](../reference/R66-CAPABILITY-AUDIT.md)，本轮对可识别的编号源码摘录进行了逐行比较，并另提取 54 键、14 动作与显式投影集合；逐源哈希/比较范围见[静态清单](restructure/capability-wiring-baseline.json)。摘录中的“未修改规划”是其产生时的事实，不否认本轮已经修改文档。

**外部技术依据：** 本页在各机制处链接 JDK 17 JVMTI、JNI、Instrumentation、JVMS 官方规范及 OpenJDK 17u 的实现文件，查阅日期为 2026-09-14。17u 的 `master` 为可变分支，仅支撑所述实现分类，不当作用户 Temurin 二进制的精确构建证明。实际 VM 支持仍由探测/获授/行为结果确定。

**设计判断：** producer/scope 接线字段、批次顺序、补证安排与动作后续工作为拟实施设计；不声称由脚本证明正确，不改变 `validation/`。当前复核摘要在 [current-verification.json](restructure/current-verification.json)，历史核对保留各自基线。[记录效力入口](restructure/README.md)。
