# Ronova 7.16+ JVM 极限强度加强总纲 V2.5（Internal Root）

> 历史归档（2026-09-08 整理）：保留当时的设计、实现和验证口径，不代表当前源码状态。文中的“当前”“下一步”属于记录当时。
> 当前入口：[进度](../../CURRENT-PROGRESS.md) · [有效规划](../../plans/README.md)。

> 文档性质：7.16 起的强度系统、极限代码对抗、结果相关攻击面、执行对抗、自动恢复与 JVM 闭环总纲  
> 状态：Design Freeze Candidate  
> 冻结实现基线：0.2.0-alpha.7.15-r51  
> 基线 JAR SHA-256：12624f81392363d757208cbfe873593d152fbbc58b03d1eb2d8e4dac04a09801  
> 当前源码承载版本：0.2.0-alpha.7.17-r53（ProfilePresetMatrix source-level implementation candidate；不是新的 exact-artifact 冻结基线）  
> 生效范围：7.16、7.17、FULL L5、L6、L6E  
> 上游架构：Ronova L6 Architecture Master Plan V2.4 Internal Root  
> 继承文档：Ronova 7.16+ JVM 极限强度加强总纲 V2.4，保留 V2.3 / V2.2 / V2.1 / V2.0 / V1.0 历史关系  
> 日期：2026-09-02

V2.5 沿用 V2.4 的冻结基线声明，不回写 r51 / 7.15 的实现状态，也不把 r52 源码候选伪装成新的 exact-artifact 冻结基线，更不要求为了本次升版返工 7.16 Durable Cutover。  
本版完整继承 V2.4 的攻击深度、十五项攻击政策、三类全返回、候选准入、义务压缩、执行/结果分离、反诱导调度和宿主安全；在此之上把 Data、Network、Presentation、Input、Event、Temporal、Registry、Resource 八类运行表面统一纳入同一 Internal Root，并冻结逐能力三档启用矩阵、跨表面操作计划、客户端/服务端边界和真实能力报告。文档升版不代表这些新增广度已经进入 r52 生产实现。

**2026-09-02 实现状态补记：** r53 已把 §6.6 的 54-key、revisioned `ProfilePresetMatrix`、逐项解析回执、supported/evidence 双 ceiling、安全下限、schema-1 读取迁移和 coherent restart 边界落入源码与聚焦测试；这只完成 7.17 矩阵基础，不代表八类 Runtime Surface adapter、CrossSurfaceOperationPlan、AllReturn、anti-AllReturn 或 Extreme executor 已经实现。

---

## 0. 最终结论

Ronova 的三个正式强度预设继续为：

```text
COMPATIBLE  兼容
ENHANCED    强化
EXTREME     极限代码对抗
```

其中 **EXTREME / 极限代码对抗** 不再被定义成“兼容档的更高数值版本”。

它的正式定位是：

> **面向 Minecraft / Forge / 当前 JVM 内的极限代码对抗。创作者通过各自 Mod 的状态、生命周期、代码执行、恢复、拦截、分派、动态代码、Native/JVMTI 证据和最终结果控制进行对抗。Ronova Extreme 的第一目标是让当前 Judgment 成为最终有效结果，而不是保证敌对 Mod 在对抗后仍保持正常功能。**

EXTREME：

- 不设置敌对 Mod 数量上限；
- 不设置固定目标槽位上限；
- 不以 Mod ID、类名、实体类型或单一机制作为攻击边界；
- 不因第三方 Mod 兼容性主动降低已确认结果链的攻击强度；
- 不因为一个路径暂时无法完成归属证明就把它从攻击域删除；
- 资源预算限制**执行速率**，不缩小**逻辑攻击域**；
- 唯一不可突破的强制边界是宿主安全、Authority 正确性和 JVM 可持续运行。

V2.2 冻结的九项政策继续有效：

1. **Extreme Code Duel Positioning**  
   EXTREME 正式定义为“极限代码对抗”，兼容性不再是该档位的首要目标。

2. **No Mod-count Cap**  
   不写入“1–2 个 Mod”等数量限制。少量同级 Mod 只可作为常见测试场景，不能成为架构上限。

3. **Candidate Surface Default-in**  
   任何有合理证据可能改变当前 Judgment 最终结果的同 JVM 路径，先进入候选攻击面，再做归属证明。

4. **Proof Controls Severity, Not Scope**  
   证明决定能否做永久、类级或高副作用处置；不决定候选路径是否进入结果控制范围。

5. **Host-Safety Boundary**  
   Extreme 可以牺牲敌对 Mod 兼容性，但不能靠 OOM、无限线程、无限扫描、无限 retransform、故意 JVM 崩溃、破坏用户文件/存档或 OS 来取得强度。

6. **Fence Scope Escalation**  
   FENCE 必须按 TARGET → OPERATION → PLANE → ROOT/REALM 分级，不允许一个未知类或单个目标把 Ronova 自己骗进全 Root 停摆。

7. **TargetEpoch Qualification Generation**  
   TargetEpoch 是资格代际，不只在终结时推进。任何让旧资格失效的 Authority transition 都推进 epoch；合法重新激活也必须使用新 epoch，旧任务永不复活。

8. **Derived Evidence Boundary**  
   ResultGraph 与 ExistenceTerminationCertificate 只是派生证据，不是第二 Truth，不得直接取代 Judgment、Tombstone、TargetEpoch 或 Admission Authority。

9. **7.16 Scope Lock**  
   7.16 只冻结政策键、版本化扩展缝和已有必要合同；完整 ResultGraph、MutationTrace、ExistenceTerminationCertificate 生产 schema 与 executor 从 7.17 起实现，不因 V2.2 把 7.16 重新扩成 JVM 大改造。

V2.3 在这九项政策下冻结四项执行合同：

| 合同 | 正文位置 | 必须解决的问题 |
|---|---|---|
| Candidate Admission Rule | §9.2 | 一条有效强证据或两条独立弱证据准入；候选只授权临时结果隔离与追踪 |
| Obligation Compaction | §14.5–§14.8 | 重复事件归并，不断变化的新来源转为有界范围汇总；记录可压缩，未解决事实不消失 |
| Execution / AttackOutcome Separation | §6.5、§9.4、§11.5 | FENCED 与 APPLIED/PARTIAL/CLEARED/FAILED 分开，当前完成证明失效不能继续显示成功 |
| Candidate Anti-induction Scheduling | §12.4 | 按真实结果影响和有效证据调度，抑制刷量，并保留公平处理与探索预算 |

四项合同只细化现有攻防执行，不新增攻击按钮，不创造第二 Authority，不追加 7.16 生产实现义务。

V2.4 进一步冻结五项收口合同：

| 合同 | 正文位置 | 必须解决的问题 |
|---|---|---|
| Qualification Transition Atomicity | §9.5、§13.4、§15.5 | Judgment、TargetEpoch、旧代际撤销、Admission revision 与 receipt 同一权威事务提交 |
| Full Host Resource Safety | §12.2、§20.4 | heap、direct/native、metaspace、code cache、线程、类加载、句柄与可观测 GPU 资源全部受预算约束 |
| Completion Finality | §6.5 | 非清除 CriticalOperation 明确区分局部 APPLIED 与合同最终完成 |
| Candidate Restriction Release Hysteresis | §9.2.5 | 证据结论与限制释放状态分离，防止自动限制反复开关 |
| Version Truthfulness | §2 | V2.2/V2.3/V2.4 新设计均不得伪装成 r51 已实现 |

五项收口不缩小 Extreme 攻击域。宿主预算只限制 Ronova 自身执行和分配速率；敌方制造的全局资源压力不能成为删除攻击面、撤销 Judgment 或伪造完成的依据。

V2.5 在 V2.4 深度不变的前提下冻结六项广度合同：

| 合同 | 正文位置 | 必须解决的问题 |
|---|---|---|
| Runtime Surface Taxonomy | §8.6 | Data、Network、Presentation、Input、Event、Temporal、Registry、Resource 统一映射到现有攻/拦/防能力，不创造第四执行平面 |
| Breadth–Depth Parity | §8.6、§12.5 | 每个表面都必须走观察、归属、验证、政策、执行、修复、恢复、完成闭环，不能只“看见了”就算具备 Extreme |
| Per-key ProfilePresetMatrix | §6.6 | 三档强度按每个能力键真实启用，而不是把整页粗暴映射成同一个等级 |
| Single-root Cross-surface Plan | §12.5、§15 | 多表面冲突由一份 Root 批准的 operation plan 与 receipt 编排，禁止各表面互相修复和产生第二 Truth |
| Client / Server Boundary | §8.6、§16.5 | Presentation 与 Input 只作显示投影和请求入口；服务端 Truth、专用服务端类加载和网络重放边界必须真实成立 |
| Surface Truthfulness | §6.6、§19.5 | 缺少 observer、executor、repair 或 completion proof 时显示真实 UNOBSERVED/UNAVAILABLE/PARTIAL/FENCED，不得伪报 ACTIVE/FULL |

V2.5 不增加 GUI 顶层页、不增加新的攻击按钮，也不修改现有十五项攻击政策键和五十四项稳定 `StrengthOptionKey`。新增表面是现有能力的覆盖维度与执行合同，不是给玩家看的 JVM 黑话开关。

---

## 1. 文档权威与版本关系

### 1.1 历史版本

- V1.0：第一次冻结“攻击 / 拦截 / 防护 / 运行时 / 功能”五页结构；
- V2.0：加入稳定窗口、TargetEpoch、WriteSink、运行时补丁防护、JIT/dispatch evidence 和恢复内核；
- V2.1：扩大结果相关攻击面，加入源/链/代码攻击、存在性终结和四层反全返回；
- **V2.2：把 EXTREME 正式改成极限代码对抗，并修正范围缩窄、FENCE 自伤、D33 绝对化、TargetEpoch 重激活及 7.16 scope 问题。**
- **V2.3：冻结候选准入与处置权限、义务归并和容量溢出汇总、执行/结果正交状态、反诱导公平调度及其验收。**
- **V2.4：冻结资格代际原子提交、JVM 全资源域宿主安全、Completion 最终性、候选限制释放防抖与最终验收。**
- **V2.5：在 V2.4 深度上冻结八类 Runtime Surface、逐能力三档矩阵、跨表面单 Root 编排、客户端/服务端边界和广度验收。**

解释优先级：

1. 已经进入 r51 成品并通过验证的 7.15 不变量；
2. Ronova L6 Architecture Master Plan V2.4 Internal Root；
3. 本 V2.5；
4. V2.4 / V2.3 / V2.2 / V2.1 / V2.0 / V1.0 中未与 V2.5 冲突的部分，较新版本优先；
5. 后续阶段实现规格；
6. UI 文案和示例。

### 1.2 不回写已完成阶段

```text
r51 / 7.15 frozen baseline
    ↓
7.16 Durable Cutover + Strength Policy Foundation
    ↓
7.17 JVM Enforcement Closure / FULL L5 Candidate
    ↓
L6 Internal Dominion Root
    ↓
L6E Execution / Reconstitution / Semantic Closure
```

7.14A/B/C/D、7.15 和已经接近完成的 7.16 实现不因本总纲重新编号或推翻。

V2.4、V2.3、V2.2 原稿与更早历史稿分别保留。V2.5 的完整合同集中定义在本文件的对应正文小节；阶段表与验收引用这些定义，不另建冲突的状态枚举、攻击键或执行规则。

### 1.3 Internal Root 不变

- Authority 不移出当前游戏进程；
- 7.16：DurableRootStore 是 sole durable writer；
- FULL L6：InternalDominionRoot 是 sole logical Authority writer；
- Agent、Instrumentation、JVMTI、Native、恢复内核只提供证据、门控、执行和修复；
- 它们不能成为第二 Judgment writer；
- 临时 attach/relaunch helper 可以存在，但必须有界、非权威、完成任务后退出。

---

## 2. r51 / 7.15 冻结基线

r51 仍只被视为后续强度系统承载基线，不得把 V2.2 / V2.3 / V2.4 / V2.5 的新增设计写成 r51 已实现。r52 是 7.16 源码级实现候选，r53 是当前逐键矩阵源码候选；除非另有成品哈希和 exact-artifact 验收，不得替换本节的 r51 冻结基线口径。

已有基础至少包括：

- Raw Observation / Presence 分离；
- 身份、拓扑、状态观察；
- Judgment / Terminal / Tombstone / Admission；
- command / epoch / commit index / receipt / replay；
- DurableRootStore shadow；
- RealmId；
- bounded worker；
- Java Agent / Transformer / JVMTI / Native 入口；
- 生命、死亡、清除、tracking、render 的部分生产路径。

仍缺：

- FULL L5 级语义证明；
- 真正统一的三档拦截；
- ResultGraph 生产闭包；
- 完整 anti-AllReturn；
- 动态代码 / dispatch / Native evidence 闭包；
- FULL Extreme 的存在性终结和最终结果控制。

---

## 3. 正式术语

### 3.1 攻击 / 拦截 / 防护

```text
攻击
= Ronova 主动让目标或其结果链发生什么

拦截
= 对方准备修改状态、代码、任务、分派、Native 路径或结果时，
  Ronova 在生效前阻止什么

防护
= 对方已经打进来、拦截漏过或结果已被偷换后，
  Ronova 如何检测、恢复、替代执行与重新取得有效结果
```

### 3.2 三种全返回

| 名称 | 页面 | 含义 |
|---|---|---|
| 全返回 | 攻击 | 接管目标结果图中的完成权限，使未经当前 Judgment 授权的成功结果不能成立 |
| 全返回拦截 | 拦截 | 阻止别人把 Ronova 的关键执行改成早退、固定返回、空执行或伪完成 |
| 反全返回 | 防护 | 对 bytes、dispatch、completion、state 四层联合验证；穿透后修复、替代执行并重验证 |

三者不得共用一个布尔值、一个 executor 或一个 receipt。

### 3.3 极限代码对抗

极限代码对抗不是“伤害值对拼”。

它比较的是：

```text
谁能建立最终 Judgment
谁能保护自己的执行
谁能让对方的恢复链失效
谁能阻止旧任务、旧包、旧状态重新生效
谁能发现代码/dispatch/Native 结果被偷换
谁能在被打穿后恢复
谁能让最终状态持续收敛
```

EXTREME 不限制创作者数量或 Mod 数量。  
人数/Mod 数量只能影响资源压力和测试规模，不能改变攻击域定义。

### 3.4 Candidate Attack Surface / 候选攻击面

V2.2 定义，V2.3 按 §9.2 细化准入：

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

### 3.5 证明决定处置级别，不决定范围

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

### 3.6 Existence Termination / 存在性终结

Extreme 清除内部语义继续为 `Existence Termination`。

它不等于：

```text
entity.remove()
```

而是：

> 撤销当前 Judgment 所辖目标继续存在、恢复、重建和重新发布的有效资格。

`CLEARED`、`PARTIAL` 属于 attackOutcome；`FENCED` 属于 executionState，并由 fenceScope 指明范围。它们均不替代 Judgment Truth，完整定义见 §6.5。

### 3.7 ResultGraph / Certificate 的权威边界

ResultGraph：

- 是从 Raw Observation、provenance、dispatch、task、state、network、save 等证据派生的结果关系图；
- 可限制 lease、证明 closure、定位攻击面；
- **不能创造 Judgment**；
- **不能单独撤销 Tombstone**；
- **不能自己推进 TargetEpoch**；
- 重启后必须依据当前 Root Truth 和可信 evidence 重新建立。

ExistenceTerminationCertificate：

- 只是“在某一证据 revision / 会话 / policy / result graph 下完成条件已满足”的证明；
- 不能直接成为 `denySpawn()` 的 Authority；
- Admission 必须继续依据 Root Truth + Tombstone + TargetEpoch + policy；
- Certificate 失效只代表“完成证明失效”，不自动回滚已提交 Judgment。

---

## 4. 目标、非目标与宿主安全边界

### 4.1 三档目标范围

#### COMPATIBLE / 兼容

```text
Ronova 自身
+
r51 已有稳定实体/生命周期/准入路径
```

目标：普通整合包可用，优先避免干扰无关第三方功能。

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

#### EXTREME / 极限代码对抗

```text
当前 Judgment
→ 所有可能改变其最终结果的同 JVM 候选路径
→ 已证明源 / 链 / 代码 / dispatch / task / callback / rebuild / publish
→ 最终完成与状态出口
```

目标：

> **胜负结果优先，第三方兼容性不是保护目标。**

### 4.2 Extreme 不设置的限制

不允许把以下写成固定架构限制：

- 最多 1 个、2 个或 N 个敌对 Mod；
- 最多 N 个结果图节点；
- 最多 N 层逻辑链深度；
- 最多 N 个 loader 副本；
- 最多 N 个任务/回调来源；
- “只攻击明确注册过的某几个 Mod”。

实现可以因内存和 CPU 使用**有界窗口**、分页、分片、LRU、backlog，但：

> **资源预算限制每个窗口处理多少，不限制最终应该处理哪些。**

### 4.3 Extreme 允许牺牲的东西

已证明属于当前敌对结果链、且相应处置获得当前 Root 政策授权后，EXTREME 不保证：

- 对方 Mod 的功能完整性；
- 对方 Mod 的热补丁仍能工作；
- 共享 helper / manager 在对抗后仍完整服务所有第三方功能；
- 对方自己的恢复器、listener、runtime patch、generated class 继续保持原语义；
- 对方 Mod 在当前会话内能够恢复到“正常玩法状态”。

“高度疑似”只在满足 Candidate Admission Rule 后授权临时、可撤销的结果限制和进一步追踪，不自动授权永久、类级或高副作用改写。已证明的共享恢复路径继续适用 §9.3，不能借第三方兼容性退回弱执行。

### 4.4 Extreme 绝对安全边界

EXTREME 不允许通过以下方式取得胜负：

- 故意制造 OOM；
- 无限创建线程；
- 无限类生成；
- 无限 retransform/redefine；
- 无限每 tick JVM 全扫描；
- 故意死循环吃满 CPU；
- 破坏 JVM/native 内存来求强度；
- 删除第三方文件；
- 损坏世界存档或区块文件；
- 修改、删除用户文件；
- 操纵无关进程；
- OS / 内核破坏；
- 故意蓝屏、死机、崩溃宿主电脑。

形式化：

> **EXTREME 不计第三方兼容性代价，但必须计宿主稳定性、Authority 正确性、用户数据安全和 JVM 可持续运行的代价。**

---

## 5. 核心不变量

### 5.1 继承不变量 D1–D32

以下原则继续有效：

- Raw Observation 与 Presence 分离；
- Judgment 与存储分离；
- 单 writer；
- command idempotency；
- COMMIT_UNKNOWN 原 command id 恢复；
- Tombstone / Admission / Receipt 不回退；
- 所有 worker 有界；
- Native/JVMTI 不是 Truth；
- 修复后重验证；
- 新机制没证明前不能删旧可靠机制；
- 极限关键 evidence 失效必须 fail-closed；
- 极限可以牺牲兼容性，不能牺牲持久化正确性。

### 5.2 D33 修订：Final Write Sink 不得绝对化

旧式表述：

```text
所有 Authority-bearing mutation 最终必须经过登记 WriteSink
```

V2.2 改为：

> **所有已纳入 Ronova 仲裁能力范围的 Authority-bearing mutation，必须经过登记 WriteSink 或等价的已证明最终提交门。**

如果某个相关 mutation：

- 能观察但不能前置阻止；
- 能穿透当前 gate；
- 能由未知 Native/JVMTI 直接修改；
- 无法证明是否经过当前 sink；

则：

```text
不得把该写入视为合法结果
不得宣布该结果面闭环
撤销对应 coverage proof
隔离相应结果授权
按 scope 进入 PARTIAL / FENCED
```

这比“假装所有写入都必经 Ronova”优先级更高。

### 5.3 V2.2 新增强不变量（继续有效）

| 编号 | 要求 |
|---|---|
| D53 | EXTREME 候选攻击域采用 default-in：合理怀疑可改变 Judgment 结果的路径先纳入候选面 |
| D54 | 归属证明决定处置级别，不决定候选路径是否进入结果控制范围 |
| D55 | 资源预算耗尽只延后执行，不得删除已建立 obligation 或缩小攻击域 |
| D56 | FENCE 必须有 scope；TARGET/OPERATION/PLANE 的问题不得无条件升级 ROOT/REALM |
| D57 | ROOT/REALM FENCE 只在 Authority writer、epoch、durable truth、root ownership 等核心可信性失证时升级 |
| D58 | TargetEpoch 是资格代际；终结、合法重新激活、重新绑定等使旧资格失效的 Authority transition 都推进 epoch |
| D59 | 后续合法重新激活必须使用新 TargetEpoch；任何旧实体、旧任务、旧包、旧 callback 都不能恢复资格 |
| D60 | ResultGraph 是派生证据，不是 Authority writer |
| D61 | ExistenceTerminationCertificate 是证明，不是 Admission/Judgment Truth |
| D62 | EXTREME 不设置敌对 Mod 数量上限 |
| D63 | EXTREME 可以破坏敌对 Mod 兼容性，但不能通过无界资源消耗、用户数据/存档/OS 破坏来增强 |
| D64 | “无法前置 veto”时不得产生虚假的 DENIED 证明；只能报告真实隔离/修复/fence 动作 |
| D65 | 7.16 不因 V2.2 返工成完整 ResultGraph/MutationTrace/Certificate 实现阶段 |

### 5.4 V2.3 执行合同不变量

| 编号 | 要求 |
|---|---|
| D66 | 候选准入要求当前结果范围内一条有效强证据或两条独立弱证据；合法且结果合规的授权操作不因触达 sink 被自动隔离 |
| D67 | Candidate/Unresolved 只允许临时结果限制与追踪；confidence、重复次数或调度优先级不能直接授予高副作用处置 |
| D68 | 同一事件的重复日志、派生字段和过期证据不能伪装成两条独立弱证据 |
| D69 | Raw event、代表样本、字符串载荷、细粒度 obligation 及汇总记录均有容量与字节预算 |
| D70 | 重复事件按规范义务归并；不同来源和 key 持续增长时由有界范围汇总承接未决事实 |
| D71 | 细节淘汰前必须完成未决范围与结果限制的接力；压缩、LRU、TTL、计数或样本变化不能独立解除 obligation |
| D72 | 汇总记录与证据摘要不证明其中全部来源敌对，也不证明结果闭合；覆盖缺口未解决时不得 CLEARED |
| D73 | Authority commit、executionState、attackOutcome、fenceScope 及策略激活状态相互分离，FENCED 不能推导攻击成功 |
| D74 | APPLIED 必须有局部效果证据；FAILED 不等同 Judgment 提交失败，COMMIT_UNKNOWN 始终以原 command id 查证 |
| D75 | 当前证书的关键证据失效或目标再次漂移时，当前结果重新评估；历史 CLEARED 不得冒充当前完成 |
| D76 | sourcePressure 主要驱动限流与合并，重复次数贡献封顶；刷量和更换来源身份不能自动获得更多预算 |
| D77 | 在宿主有最低执行机会时保留 Authority/repair 预算、目标间公平处理及有界探索；固定优先级不能永久搁置已有未决范围 |
| D78 | 四项合同的完整 schema、执行器和新增验收归 7.17/L6E；不新增攻击项、不返工 7.16、不增设 Truth writer |

### 5.5 V2.4 Design Freeze 不变量

| 编号 | 要求 |
|---|---|
| D79 | 任何改变目标资格代际的 Authority transition，必须把 Judgment transition、TargetEpoch 分配、旧代际撤销、Admission token revision 与权威 receipt 作为同一 canonical Root command 的单一 durable transaction 提交 |
| D80 | 资格事务只能整体可见或整体不可见；projection 只能在 durable commit 后发布，崩溃恢复与 COMMIT_UNKNOWN 必须使用原 command id，禁止用第二命令补推进 epoch |
| D81 | Host Safety 必须覆盖 heap、direct/mapped buffer、metaspace、code cache、线程与栈、class/loader、JNI/JNA/JVMTI native allocation、文件/平台句柄及可观测 GPU/render allocation |
| D82 | Ronova-owned usage 与 whole-host pressure 必须分开；敌方制造的全局压力只能触发背压、分片和紧急预算，不能删除攻击面、撤销 Judgment 或伪造成功 |
| D83 | 非清除 CriticalOperation 的 APPLIED 是否为最终成功，必须由 CompletionContract 通过 completionSatisfied 明确声明；未声明时 APPLIED 只表示局部效果 |
| D84 | Candidate 的证据结论与临时限制释放状态相互分离；PROVEN_UNRELATED 可以立即成立，而自动限制必须经有界、可审计、不可无限延长的释放重验证窗口 |
| D85 | Candidate 自动释放防抖不得改变明确的操作者 Authority command 语义；显式解除裁决仍按对应 canonical command 立即提交并如实提示可能恢复的外部重建源 |
| D86 | V2.4 不新增攻击按钮、不缩小 Extreme 攻击域、不返工 7.16 为全 JVM 实现；7.16 只承接已有 cutover 所需的原子事务与版本化扩展合同，完整生产执行和资源域 governor 从 7.17/L6E 实现 |

### 5.6 V2.5 Runtime Surface 不变量

| 编号 | 要求 |
|---|---|
| D87 | InternalDominionRoot 仍是唯一 logical Authority writer；Data、Network、Presentation、Input、Event、Temporal、Registry、Resource 只能作为 observer、evidence adapter、executor、repair/recovery adapter 或 projection，不能各自保存一份可竞争 Truth |
| D88 | 每个宣称受支持的 Runtime Surface 必须实现可检查的 Observe → Attribute → Verify → Policy → Enforce → Repair → Recover → Completion 链，并为每一阶段提供 typed input、typed output、revision 与失败语义 |
| D89 | Runtime Surface 必须映射到 Attack / Interception / Protection 三个既有平面；不得创造第四平面、第二 Judgment 状态机或绕过 Root API 的“表面权威” |
| D90 | V2.5 不增加顶层攻击 key 或 GUI 按钮；十五项攻击政策与五十四项稳定 StrengthOptionKey 保持兼容，表面覆盖通过映射、evidence 和 executor binding 表达 |
| D91 | 三个正式预设从 7.17 起由 versioned ProfilePresetMatrix(profile, StrengthOptionKey) 逐键解析；r52 按 plane 的统一映射只是 7.16 基础实现，不得被描述成 V2.5 已交付矩阵 |
| D92 | 服务端 Root Truth 与客户端投影严格分离；Presentation 只显示 accepted projection，Input 只生成带身份、epoch 与 nonce 的请求；专用服务端不得加载客户端渲染实现 |
| D93 | 任一跨表面操作至少绑定 RealmId、player/owner identity、target lineage、TargetEpoch、source identity、canonical command id 和 relevant revision；旧包、旧事件、旧任务、旧投影不能跨 epoch 恢复资格 |
| D94 | observer、executor、repair、recovery 或 completion proof 任一缺失时，能力必须按实际报告 UNOBSERVED、UNAVAILABLE、PENDING_RESTART、PARTIAL 或 FENCED；不得由“Agent 已加载”“看到调用”或“发出补丁”推导 ACTIVE/FULL |
| D95 | 多表面同时作用于同一 protected result 时，由一份 Root-approved CrossSurfaceOperationPlan 统一决定顺序、幂等键、lease、预算和 completion；表面之间不得互相调用形成循环修复、重复提交或结果振荡 |
| D96 | Host Safety Governor、Loop Guard、single-writer、bounded queue 和资源硬预算在所有预设中始终启用；EXTREME 只提高覆盖、证据和处置能力，不能关闭宿主安全来换强度 |
| D97 | Runtime Surface 仍受当前 Minecraft / Forge / 同 JVM 边界约束；不得把 OS、无关进程、用户文件、磁盘破坏、凭据窃取或无界资源攻击纳入攻防能力 |
| D98 | 7.16 Scope Lock 继续成立；r52 只承载已有 StrengthPolicy/option seam 与 cutover 基础，八表面生产闭环、逐键预设矩阵和跨表面验收从 7.17 开始并在 L6/L6E 收口 |

---

## 6. 强度模型

### 6.1 顶层预设

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

### 6.2 三档正式范围

| 档位 | 默认范围 | 兼容性政策 | 失败政策 |
|---|---|---|---|
| COMPATIBLE | Ronova 核心 + 稳定实体路径 | 优先兼容 | 非关键路径记录/降级，Authority 不失真 |
| ENHANCED | Ronova + 当前目标 + 已识别直接结果链 | 强度优先、尽量精确 | 对相关对象/类/调用隔离、修复或对象级 fence |
| EXTREME | 当前 Judgment 的全部候选/已证明同 JVM 结果攻击面 | 不保护敌对 Mod 兼容性 | 先限制未知结果资格；无法闭合时按最小必要 scope fence |

### 6.3 子项档位

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

### 6.4 配置值与有效值

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

### 6.5 Execution State 与 Attack Outcome

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
| attackOutcome=CLEARED | 清除请求已满足 §9.4 的全部条件且当前证据有效 | 不适用于所有非清除能力，不是 Admission Truth |
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

### 6.6 V2.5 逐能力 ProfilePresetMatrix

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

这些行是玩家语义，不创建额外 `StrengthOptionKey`。它们由十五项攻击政策、既有 Tombstone/Admission/TargetEpoch/WriteSink 合同和下列高级矩阵共同实现。

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

#### 6.6.3 拦截机制矩阵

| StrengthOptionKey | GUI 名称 | 兼容 | 强化 | 极限代码对抗 |
|---|---|---|---|---|
| `INTERCEPT_MIXIN` | Mixin | 保护若娜瓦核心类与关键 Hook | 拦截已证明触达目标结果链的 Mixin | 对当前 Judgment 候选结果路径全部严格；无真实前置门不得伪报 DENIED |
| `INTERCEPT_ASM` | ASM / Javassist | 保护若娜瓦关键字节码 | 控制相关 transformer 与修改目标 | 严格验证候选字节码生产、转换、重转换及结果；穿透后隔离、修复、重验 |
| `INTERCEPT_INSTRUMENTATION` | Instrumentation | 监控并保护 Ronova class retransformation | 控制相关 transformer/redefine | 严格处理候选 transform/redefine/retransform，受 rate/resource governor 约束 |
| `INTERCEPT_JVMTI` | JVMTI | 监控可见 JVMTI 影响并保护关键类 | 对相关 class/native event 建立证据与恢复 | 在能力可证范围内严格 gate/repair/reverify；不可观测部分必须报告 UNOBSERVED |
| `INTERCEPT_UNSAFE_REFLECTION` | Unsafe / 反射 | 保护 Ronova 关键 field/method/handle | 控制相关内存写入和反射分派 | 严格处理候选写入/调用结果；不全局禁用 JVM 合法 Unsafe/反射使用 |
| `INTERCEPT_CLASSLOADER` | ClassLoader | 保护 Ronova loader identity 与关键资源 | 控制相关 loader、defineClass 与资源替换 | 严格验证候选 loader/hidden/generated lineage，专用服务端保持类加载边界 |
| `INTERCEPT_JNI_NATIVE` | JNI / JNA / Native | 监控并保护 Ronova native bridge | 控制已证明相关 native module/callback | 在同 JVM 可观测范围严格证据、门控与恢复；不宣称对同权限 Native 绝对免疫 |
| `INTERCEPT_MODLAUNCHER` | ModLauncher | 保护 Ronova 启动链和核心转换顺序 | 控制相关 launch service/transformer | 严格验证当前结果相关启动/加载链；启动期变化按需 PENDING_RESTART |
| `INTERCEPT_THREAD_TASK` | 线程与任务 | 保护 Ronova worker、Root commit 与关键任务 | 控制相关 executor/task/callback | 严格约束候选恢复/重建任务，始终保留 bounded worker 与 loop guard |
| `INTERCEPT_ALL_RETURN` | 全返回拦截 | 检测 Ronova 关键方法被直接/固定返回 | 保护相关调用链与完成合同 | bytes + dispatch + completion + final state 四层联合；穿透后转入反全返回恢复 |

#### 6.6.4 穿透后防护矩阵

| StrengthOptionKey | GUI 名称 | 兼容 | 强化 | 极限代码对抗 |
|---|---|---|---|---|
| `PROTECT_LIFE` | 生命保护 | 恢复当前/最大生命与原版同步 | 收敛相关能力附件和写回源 | 从候选状态源到最终显示/网络结果持续收敛 |
| `PROTECT_DEATH` | 死亡保护 | 阻止非 Authority 死亡结果 | 修复相关死亡标志、回调与记录 | 对候选 death/terminal/write sink 全链验证与恢复 |
| `PROTECT_REMOVAL` | 清除保护 | 恢复非法 remove/discard | 修复相关 manager/index/tracking | 跨实体记录、registry、network、presentation 和 epoch 完整恢复 |
| `PROTECT_ENTITY_RECORD` | 实体记录保护 | 核心 ID/UUID/section 记录 | 加入 chunk/tick/tracking 相关记录 | 所有候选记录写入点、重插与 projection 持续验证 |
| `PROTECT_IDENTITY_RESPAWN` | 身份与重生保护 | 当前 identity/lineage 绑定 | 相关重生/替身/UUID 变化 | Realm/lineage/TargetEpoch 原子资格与跨重启恢复 |
| `PROTECT_DISPLAY` | 显示保护 | 原版模型、BossBar 与基础 presence | 相关 renderer/HUD/network projection | Presentation 只接受 Root projection，旧/伪投影不能冒充 Truth |
| `PROTECT_ANTI_INTERCEPTION` | 反拦截 | 保证关键请求、输入、通信能到达 Root | 恢复相关 hook/dispatch/task 连续性 | 对候选阻断点做来源追踪、替代路径、修复和最终收件证明 |
| `PROTECT_ANTI_ALL_RETURN` | 反全返回 | 检测关键方法空执行/固定返回 | bytes、调用目标与完成结果联合恢复 | bytes + dispatch + completion + final state 四层持续重验 |
| `PROTECT_INPUT_COMMUNICATION` | 按键与通信保护 | 认证输入与请求收件 | 相关 keybind/channel/packet continuity | nonce/epoch/revision、防重放与服务端授权全链；客户端不成为 Authority |
| `PROTECT_CALL` | 调用保护 | 验证关键调用目标 | 相关 reflection/handle/dispatch 恢复 | 候选解释/JIT/native 调用目标与 completion result 联合验证 |
| `PROTECT_CODE_INTEGRITY` | 代码防篡改 | 关键类 bytes/semantic baseline | 相关 loader/transformer/dispatch attestation | all-loader、dynamic、JIT/native evidence 与 repair/reverify 闭环 |
| `PROTECT_CORE_AUTHORITY` | 核心权限保护 | Root owner、writer、Realm 与 epoch | 相关 lease/gate/receipt/projection | single-writer、qualification transaction、durable recovery 全部严格 |
| `PROTECT_AUTO_REPAIR` | 自动修复 | 修复 Ronova 核心可验证漂移 | 修复相关字节码、状态和 dispatch | 按 Root plan 跨表面修复；禁止循环修复、无界重转换和伪成功 |
| `PROTECT_STATE_RECOVERY` | 状态恢复 | 从当前 Authority Truth 收敛核心状态 | 相关记录/tracking/identity 恢复 | Data/Registry/Network/Presentation 跨表面恢复并完成重验 |
| `PROTECT_RESTART_RECOVERY` | 重启恢复 | 恢复 durable Truth 与基础 gate | 重建相关 obligation/evidence/projection | 新 JVM 重建 Root、epoch、跨表面计划和当前完成证明，不复活旧资格 |
| `PROTECT_ROLLBACK` | 防状态回滚 | 拒绝旧 revision/epoch 状态 | 控制相关 save/network/cache 回滚 | 所有候选表面以 command/revision/epoch 防回滚，COMMIT_UNKNOWN 幂等解析 |
| `PROTECT_LOOP_GUARD` | 死循环保护 | 始终启用有界重试与队列 | 加入来源配额、dead-letter 和公平调度 | 全表面循环/振荡/洪泛保护始终启用；Extreme 不得关闭宿主安全 |

#### 6.6.5 运行时支撑矩阵

| StrengthOptionKey | GUI 名称 | 兼容 | 强化 | 极限代码对抗 |
|---|---|---|---|---|
| `RUNTIME_JAVA_AGENT` | Java Agent | 监控/保护 Ronova 关键类 | 为相关链提供 transform、attestation 与 repair | 为候选结果链提供有界 all-loader 证据与执行；能力不足真实降 ceiling |
| `RUNTIME_INSTRUMENTATION` | Instrumentation | 关键类 inventory 与变更观察 | 相关 redefine/retransform 控制 | 候选动态代码闭环，受重转换速率、metaspace/code cache 预算约束 |
| `RUNTIME_JVMTI` | JVMTI | 可用性与事件监控 | 相关 class/thread/native 证据 | 在平台支持范围启用严格证据/修复；不可观察项不伪报 FULL |
| `RUNTIME_NATIVE` | Native | 关键 bridge 健康与最小保护 | 相关 module/callback/dispatch 证据 | 同 JVM native/JIT 结果验证与恢复，明确同权限边界 |
| `RUNTIME_NATIVE_LOADER` | 本地库加载 | 仅加载已签名/匹配的 Ronova 组件 | 记录相关模块与加载来源 | 严格 inventory/identity/restart contract；不全局破坏第三方本地库 |
| `RUNTIME_RENDER_GUARD` | 渲染保护 | Tooltip/模型基础状态隔离 | 相关 shader/buffer/presence 恢复 | Presentation 完成证明与 GPU/render 资源预算联合 |
| `RUNTIME_CRITICAL_CLASS_WATCH` | 关键类监视 | Ronova 核心类 | Ronova + 已证明相关目标类 | 当前 Judgment 候选 producer/lineage；有界 inventory，非无限扫描 |
| `RUNTIME_DYNAMIC_CODE_WATCH` | 动态代码监视 | runtime compiler/proxy 基础观察 | 相关 generated/hidden/lambda 来源 | 候选动态 producer、loader、dispatch 与 outlet 跨表面追踪 |
| `RUNTIME_NATIVE_MODULE_WATCH` | Native 模块监视 | Ronova 模块与可见加载事件 | 相关模块/callback | 可观测同 JVM 模块、callback、allocation 证据；未知部分标 UNOBSERVED |
| `RUNTIME_THREAD_CONTINUITY` | 线程连续性 | Root/commit/repair 关键 worker | 相关 task/executor 连续性 | 候选任务链控制、公平调度和资源保留预算，禁止无限线程 |
| `RUNTIME_BACKGROUND_INTEGRITY` | 后台完整性检查 | 低频核心基线检查 | 事件驱动 + 相关路径增量检查 | 八表面增量重验与抽样，预算耗尽保留 obligation 而非全表扫描 |
| `RUNTIME_ANOMALOUS_CALL_LOG` | 异常调用记录 | 有界记录关键直接异常 | 相关来源、task、loader、dispatch 证据 | 跨表面 MutationTrace、去重、汇总和诊断；日志从不独立成为敌对证明 |

#### 6.6.6 矩阵正确性要求

1. GUI 选择预设时生成一份完整、带 revision 的逐键解析回执；不能只保存 profile 名称后在各模块自行猜测。
2. 同一 `StrengthOptionKey` 的 Compatible、Enhanced、Extreme 必须命中不同生产策略或明确声明相同且给出原因；不得三个档位共用一个 executor 参数却显示三档。
3. `effectiveLevel` 永远不高于运行时 ceiling 与证据 ceiling；缺少 Native/JVMTI、需要重启或专用服务端不支持客户端模块时必须真实降级并显示原因。
4. `FOLLOW_PRESET` 只引用当前矩阵 revision；玩家改单项后转为 Custom，其他项继续保持最近一次显式解析值，禁止隐式漂移。
5. Host Safety、Loop Guard、single-writer、durable recovery 与版本校验不属于可关闭强度项；任何档位都必须保持。

---

## 7. GUI 正式结构

### 7.1 顶层

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

### 7.2 攻击页

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

V2.3/V2.4/V2.5 不新增第十六项攻击。候选准入、压缩、调度、原子资格事务、完成性、宿主安全和 Runtime Surface 属于十五项能力共用的执行合同；普通界面至少分开显示“攻击结果”“是否完成”和“执行/隔离状态”，高级诊断才显示 surface、scope、resource domain 与 revision。

### 7.3 拦截页

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

### 7.4 防护页

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

### 7.5 运行时页

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

### 7.6 功能页

功能与强度正交，不因 Extreme 自动改变普通游戏功能。

### 7.7 V2.5 界面收敛规则

八类 Runtime Surface 不增加八个玩家页面，也不把 Data/Temporal/ResultGraph/Lease 等内部名词塞进普通 Tooltip。普通界面继续只显示“攻击 / 拦截 / 防护 / 运行时 / 功能”和玩家可理解的能力名；Mixin、ASM、Instrumentation、JVMTI、Unsafe、ClassLoader、JNI/Native 等机制名可以保留在拦截/运行时页。

普通状态只回答三件事：

```text
当前选择：兼容 / 强化 / 极限 / 自定义
当前生效：已生效 / 等待重启 / 能力不足 / 部分生效 / 已隔离
原因：缺少哪一类可读能力，例如“缺少底层转换支持”或“客户端显示链未验证”
```

`Runtime Surface`、S0–S6、revision、lease、plan step 与 evidence refs 只进入“高级诊断/导出报告”。GUI 不得为了显得强而显示内部枚举，也不得把内部缺口隐藏成一句“极限已开启”。

---

## 8. 选项、政策键与阶段

### 8.1 V2.2 攻击政策（V2.3/V2.4/V2.5 沿用）

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

### 8.2 攻击来源追踪 / MutationTrace

`attack.source_trace` 的最低职责：

```text
发现关键状态/结果变化
→ 记录 before / after
→ 关联 caller / task / callback / manager / dispatch / loader / native source
→ 生成有界 MutationTrace evidence
→ 将证据提供给 ResultGraph 和后续 attack/intercept/protection
```

MutationTrace：

- 不是 Authority；
- 不直接创造敌对关系；
- 可提高 Candidate → PROVEN_RELATED 的置信度；
- 不能因为字符串、包名或同父类就独立判定目标归属。

### 8.3 7.16 Scope Lock

7.16 必须冻结：

- `StrengthProfile`
- `PlaneId`
- `StrengthOptionKey`
- `MechanismLevel`
- `StrengthPolicySnapshot`
- `StrengthResolutionReceipt`
- 现有 cutover 必需的 `TargetEpoch / WriteSink / Completion` 扩展接口
- V2.2 新攻击 key
- 一个 versioned extension envelope / registry seam

7.16 **不要求**为了 V2.2 新增：

- 完整 ResultNode / ResultEdge durable schema；
- 完整 MutationTrace graph；
- ExistenceTerminationCertificate 生产实现；
- 十五项攻击 executor；
- JIT/native full evidence；
- Extreme 完整 GUI ACTIVE 状态。

这些从 7.17 起实现。

V2.3 不再增加 7.16 的攻击 key、完整图类型或迁移负担。V2.4 同样不增加攻击 key；资格事务只扩展已有 canonical command/receipt seam，候选规则版本、obligation 汇总、执行/结果/完成状态、资源域和释放参数继续由 versioned extension seam 承接。V2.5 的八类 Runtime Surface 也只复用同一 versioned extension registry 与现有五十四项 key；逐键矩阵和表面适配器从 7.17 实现，不回写 r52 为已完成。完整生产 schema 与行为测试按下表进入 7.17/L6E。

### 8.4 四项执行合同的阶段归属

| 合同 | 7.17 必须实现的基础 | L6E 完整闭合 | canonical 定义 |
|---|---|---|---|
| Candidate Admission Rule | 上下文绑定、证据去重与独立性、合法授权过滤、临时处置权限 | P0-B 结果链归属；P0-C 重建来源；P0-D 动态/Native 证据 | §9.2 |
| Obligation Compaction | 有界事件缓存、规范 key、去重归并、不同 key 溢出汇总、无放行窗口的接力 | P0-B 任务/回调；P0-C 跨 epoch 与重启；P0-D 动态来源洪泛 | §14.5–§14.8 |
| Execution / AttackOutcome | 状态分离、结果回执投影、scope、局部效果证据和证书失效 | P0-B 完成合同；P0-C 存在性终结；P0-D 穿透后恢复 | §6.5、§9.4、§11.5 |
| Candidate Anti-induction Scheduling | 来源归一、限流、影响优先、重复贡献封顶、公平与探索预算 | P0-B 多链；P0-C 多目标；P0-D 高压动态/Native 对抗 | §12.4 |

四项共用现有 policy resolver、结果图、Host Safety Governor 和 Root API；不得复制四套攻击事务、另建 Truth 表或新增可绕过依赖的空开关。阶段完成必须有对应生产路径与 §19.3 验收，合同存在不代表能力 ACTIVE。

### 8.5 V2.4 收口合同的阶段归属

| 合同 | 7.16 边界 | 7.17 必须实现的基础 | L6E 完整闭合 | canonical 定义 |
|---|---|---|---|---|
| Qualification Transition Atomicity | 已有 cutover 命令不得分裂提交；X45 覆盖实际已有 transition | TargetEpoch propagation、receipt/gate 发布与崩溃恢复 | 全 lineage/task/packet/save/reactivation/解除路径 | §9.5、§13.4、§15.5 |
| Full Host Resource Safety | 不新增全资源生产 governor；保留扩展缝和已有安全预算 | HostResourceDomain inventory、owned/host 分离、软硬预算、基础 X46 | Agent/JVMTI/Native/renderer 资源域完整记账与 soak | §12.2、§20.4 |
| Completion Finality | Completion extension seam | completionSatisfied receipt、状态更新和 GUI 投影 | 全 CriticalOperation、恢复后重验证和证据失效闭环 | §6.5 |
| Candidate Restriction Release Hysteresis | 不追加生产实现 | 自动释放状态、硬截止、重验证和回执 | 跨任务/动态来源/重启/汇总限制的完整释放 | §9.2.5 |

这些合同仍由同一 Root API、policy resolver、single writer 和 Host Safety Governor 编排。不得为 V2.4 再建资格 writer、完成 Truth、资源 Authority 或释放 Authority；阶段完成必须通过 §19.4 的对应验收。

### 8.6 V2.5 Runtime Surface Taxonomy

Runtime Surface 是“当前 Judgment 的结果可以被观察、改变、重建、投影或恢复的运行表面”。它不是 Mod 分类、包名白名单或新的 Authority。一个具体来源可以同时属于多个表面；归类用于选择 adapter、evidence 与 executor，不用于直接判敌。

| Surface | 覆盖对象 | 主要攻击键 | 主要拦截/防护键 | Extreme 完成条件 |
|---|---|---|---|---|
| Data | field、capability、map、NBT、SavedData、缓存、派生状态 | `ATTACK_STATE_SOURCE`、`ATTACK_RECOVERY_SOURCE`、`ATTACK_SAVE_RECONSTRUCTION`、`ATTACK_FINAL_RESULT_TAKEOVER` | Unsafe/反射、ASM、Instrumentation；生命/记录/回滚/状态恢复 | Authority state、所有已纳入候选的写回源和持久恢复出口收敛到同一 revision；缺口为 PARTIAL |
| Network | packet、channel、handler、mirror、sync cache、重发/重放 | `ATTACK_NETWORK_RECONSTRUCTION`、`ATTACK_CALLBACK_CHAIN`、`ATTACK_FINAL_RESULT_TAKEOVER`、`ATTACK_SOURCE_TRACE` | 线程任务、ClassLoader、JNI/Native；输入通信/反拦截/回滚 | 请求与投影绑定身份、nonce、epoch、revision；旧包/镜像不能恢复资格，服务端 receipt 可验证 |
| Presentation | renderer、model、BossBar、HUD、overlay、client presence、tooltip | `ATTACK_NON_ENTITY`、`ATTACK_FINAL_RESULT_TAKEOVER`、`ATTACK_CALLBACK_CHAIN` | Mixin/ASM、ClassLoader；显示保护、代码完整性、Render Guard | 客户端仅显示 Root 接受投影；旧/伪 presence、HUD 或 renderer 不再展示已终结结果，且不影响服务端 Truth |
| Input | keybind、mouse、GUI action、command request、client gesture | `ATTACK_CALLBACK_CHAIN`、`ATTACK_DISPATCH`、`ATTACK_SOURCE_TRACE` | Mixin、线程任务、全返回；反拦截、输入通信、调用保护 | 合法请求有收件/拒绝 receipt，取消、吞输入、重放或替换调用不能伪造 Authority command |
| Event | Forge bus、listener、hook、callback、lambda、订阅/反订阅 | `ATTACK_CALLBACK_CHAIN`、`ATTACK_LOGIC_SOURCE`、`ATTACK_SOURCE_TRACE` | Mixin、ASM、Instrumentation；反拦截、调用保护、自动修复 | 相关事件因果边可归属，敌方回调不能恢复旧结果；合法无关 listener 不因注册在同一总线被误杀 |
| Temporal | tick、scheduler、executor、thread、timer、future、延迟重建 | `ATTACK_TASK_CHAIN`、`ATTACK_RECOVERY_SOURCE`、`ATTACK_CALLBACK_CHAIN` | 线程任务、JVMTI、Unsafe/反射；循环保护、反拦截、状态恢复 | 旧 epoch task 永不提交，候选恢复任务有界隔离/取消/重验，公平与应急预算持续前进 |
| Registry | registry、factory、EntityType、ID/UUID index、listener registry、loader lineage | `ATTACK_REGISTRATION_SOURCE`、`ATTACK_TARGET_CLASS_LINEAGE`、`ATTACK_LOGIC_SOURCE` | ClassLoader、ModLauncher、ASM；实体记录、身份重生、核心权限 | 注册/重注册/工厂产物绑定 lineage 与 epoch，候选重插入口受 Admission 与 completion 验证 |
| Resource | class bytes、resource、shader、generated class、native module、handle、buffer | `ATTACK_DYNAMIC_CODE`、`ATTACK_RUNTIME_PATCH_DISABLEMENT`、`ATTACK_NON_ENTITY`、`ATTACK_DISPATCH` | ASM、Instrumentation、JVMTI、ClassLoader、JNI/Native；代码完整性、自动修复、显示保护 | 相关资源 identity、producer、loader/dispatch/outlet 可验证；repair 后重验，所有分配受 HostResourceDomain 预算 |

Host Safety 不是第九个可攻击表面，而是八个表面共用且不可关闭的执行边界。Realm、Judgment、TargetEpoch、Tombstone、Admission 和 canonical receipt 也不是 Data Surface 中的一般可写状态；它们仍只由 Root/Store single writer 改变。

每个 Surface adapter 必须采用统一 typed pipeline：

```text
Observation
  → AttributionEvidence
  → VerificationDecision
  → RootPolicyRequest
  → Root-approved OperationPlan
  → EnforcementReceipt
  → Repair/RecoveryReceipt
  → CompletionEvidence
  → Root-accepted OutcomeProjection
```

阶段失败必须保留最后一个可信 receipt，并按实际输出 `UNOBSERVED`、`UNAVAILABLE`、`QUARANTINED`、`FENCED`、`PARTIAL` 或 `FAILED`。任何 adapter 都不得直接写 Judgment、推进 TargetEpoch、签发 CLEARED 或把本地 cache 变成权威状态。

#### 8.6.1 广度与深度等高

“纳入表面”不等于完成。V2.5 对每个 Surface 采用统一成熟度：

| 成熟度 | 必须具备 | 可声明状态 |
|---|---|---|
| S0 — Absent | 无可靠 observer | `UNAVAILABLE` |
| S1 — Observe | 有界观察与 revision | `MONITOR`，不能声称拦截/恢复 |
| S2 — Attribute | 来源、上下文和证据独立性可验证 | 可进入 Candidate，不能自动高副作用处置 |
| S3 — Enforce | 有真实 gate/executor 与 denial/effect receipt | 按实际报告 DENIED/APPLIED；穿透不得伪报成功 |
| S4 — Repair | 能恢复受保护结果并重验 | 可报告恢复动作，未闭合仍为 PARTIAL |
| S5 — Recover | 重启、epoch、任务、网络、保存链可恢复 | 可承接跨时序 obligation |
| S6 — Complete | CompletionContract、稳定窗口和证据失效重评完成 | 才可声明该 Surface 对相应操作 `FULL` |

EXTREME 要求当前操作所依赖的所有 Surface 达到其 CompletionContract 指定成熟度；一个表面只有 S1，不得因为其他表面 S6 就把整项能力显示 FULL。Compatible/Enhanced 也使用同一成熟度事实，只是候选范围和处置政策不同。

---

## 9. 攻击平面

### 9.1 三档攻击范围

#### Compatible

```text
当前目标实体
→ 标准 health / death / remove
→ 基础 Tombstone / Admission
→ 基础 tracking / render
```

不主动深挖第三方复杂恢复链。

#### Enhanced

```text
当前目标
→ 已识别状态源
→ 已登记 revive / factory / task
→ tracking / Presence / HUD
→ 已知代码防御 / direct recovery chain
```

仍尽量做目标级、实例级处置。

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

### 9.2 Candidate-first 执行

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

#### 9.2.2 独立性、有效期与合法操作

两条独立弱证据必须同时满足：

1. 属于不同证据族，关联同一当前 Judgment 的结果范围；
2. 有可区分、可检查的原始观察依据；一种证据不能只是另一种证据的复制或字段派生；
3. 同一 event 被两个 listener 记录、同一日志转发两次、同一观察拆成多个字段，只记一次；
4. 未跨用已失效的 policy、TargetEpoch、观察窗口或来源证明；
5. 独立性尚未建立时按一条证据处理，不能凑数准入。

观察窗口、原始事件标识、证据族与去重规则由版本化 policy 决定，并进入回执。窗口到期只表示需要补充新证据，不把已经建立的 obligation 自动变成 PROVEN_UNRELATED。

单条弱证据保留在有界观察层，不自动授权临时隔离，更不能升级高副作用处置；未达门槛不意味着从逻辑攻击域永久排除。若观察暴露真实受保护结果缺口或观测丢失，按 §14.6 保留该范围的覆盖缺口，禁止利用样本过期来声明闭合。

已证明授权有效且效果符合当前 Judgment 的修复、状态同步和合法重新激活，按原合同执行，不因使用共享 manager 或 protected sink 自动成为受攻击候选。授权无效的写入仍由原 WriteSink/Admission gate 直接处理，不必等待候选规则凑齐证据；Candidate 规则不能反过来降低已有 gate 的拒绝条件。

#### 9.2.3 候选处置权限表

| 状态 | 可授权动作 | 禁止自动获得的权力 |
|---|---|---|
| 单条弱证据/观察中 | 有界记录、去重、关联及追加观察 | 仅凭弱相关性封禁共享组件、改写类或裁决整个 Mod 敌对 |
| CANDIDATE | 在真实可控入口执行目标/操作范围内临时、可撤销的结果限制，继续追踪 | 永久改写、类级失能、高副作用共享组件改写、创建 Judgment |
| UNRESOLVED | 维持上述临时限制，合并义务、按预算继续；必要时最小 scope fence | 因时间长、次数多或分数高而自动转成已证明敌对 |
| PROVEN_RELATED | 由当前 Root policy 根据因果证据、影响范围、能力与恢复基线授权攻击 | 绕过 lease、宿主边界、单 writer，或把同类无关业务自动判为敌对 |
| PROVEN_UNRELATED | 撤销仅由该候选建立且不再需要的限制 | 撤销其他候选的有效限制、已提交 Tombstone 或旧 epoch 撤销事实 |

临时限制必须记录建立原因、affectedScope、policy revision、可撤销动作和重验条件。失去追踪预算时允许汇总承接，不能使限制静默消失；证据表明无关后也不能永久保留无依据的候选限制。释放须检查同一范围是否仍有其他有效限制原因。

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

### 9.3 Extreme 不因共享路径自动降级

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

### 9.4 极限清除 / Existence Termination

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

只有全部成立：

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

### 9.5 TargetEpoch 资格代际

V2.2 冻结：

```text
TargetEpoch
= 目标有效资格的 generation
```

推进场景至少包括：

```text
终结
合法重新激活
Authority 重新绑定新 incarnation
恢复后废弃旧 admission token
发生需要撤销旧任务/旧包资格的 authoritative transition
```

示例：

```text
epoch 12
→ TERMINAL
→ epoch 13
→ 所有 <=12 的 task/packet/entity stale

后来唯一 Authority 明确允许重新激活
→ epoch 14
→ 只允许基于 14 的新 admission
→ epoch 12/13 永远不会因为“复活被允许”重新合法
```

V2.4 冻结资格代际的原子提交合同。任何上述推进都必须由一个 canonical `QualificationTransitionCommand` 表达，至少绑定：

```text
commandId / realmId
lineageId / targetIdentity
expectedJudgmentRevision
expectedTargetEpoch
transitionKind
nextJudgment / nextTargetEpoch
revokeThroughEpoch
nextAdmissionTokenRevision
policyRevision
```

同一事务必须一次提交：

```text
Judgment transition
TargetEpoch N → N+1
revoke <= N
Admission token revision
durable transition receipt
```

禁止：

```text
先发布 Judgment，稍后异步推进 TargetEpoch
先推进 TargetEpoch，再补写 Judgment/Tombstone
先开放新 Admission，再撤销旧 token
崩溃恢复时另建 command 再推进一次 epoch
projection 先看到新资格，durable truth 仍停留在旧资格
```

事务只能整体可见或整体不可见：

- 提交前崩溃：不发布任何新 Judgment、epoch 或 admission 资格；
- durable commit 已完成但回执未知：以原 `commandId` 查询或幂等重放，返回同一 transition receipt；
- expected revision / epoch 冲突：整个命令失败，不允许留下半个 transition；
- durable commit 成功后，才允许向 Projection、Admission gate、任务/包 gate 和客户端状态发布新 revision；
- 发布过程中断时，恢复方从 durable receipt 重建全部投影，不能反向猜测或生成第二 Truth。

7.16 的 DurableRootStore sole-writer 与后续 InternalDominionRoot 都必须服从同一合同；阶段变化只替换实现后端，不改变原子可见性和幂等语义。

### 9.6 全权威结果闭包

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

### 9.7 代码防御剥离

判断标准不是方法名，而是效果：

> **只要某条执行边已经被证明会把当前 Judgment 要求 TERMINAL 的目标重新建立为 PRESENT，或维持与 Judgment 冲突的有效结果，它就是防御/恢复图的一部分。**

Extreme 可：

- 撤销相关结果资格；
- 失能已证明的恢复补丁；
- 改写可安全恢复的目标执行边；
- 控制 dispatch 或 final result outlet；
- 无法安全改类时控制 admission / completion / write sink；
- 无法建立任何结果控制时按 scope FENCE。

### 9.8 攻击来源追踪

Extreme 的来源追踪范围可以覆盖：

```text
health / maxHealth / removed / dead
Entity records
Admission
Presence
Tombstone / TargetEpoch 的异常关联
dispatch target
runtime patch
task / callback
network/save reconstruction
```

但来源追踪本身不做“看见谁就杀谁”。

它输出：

```text
MutationTrace
→ source provenance
→ candidate result edge
→ 后续攻击/拦截/防护
```

---

## 10. 拦截平面

### 10.1 统一机制

结果相关操作至少归一化为：

```text
STATE_WRITE
CLASS_DEFINE
CLASS_REDEFINE
CLASS_RETRANSFORM
TRANSFORMER_REGISTER
REGISTRATION_REBIND
DISPATCH_REBIND
NATIVE_LOAD
NATIVE_BIND
TASK_SCHEDULE
CALLBACK_REGISTER
COMPLETION_PUBLISH
FACTORY_RECONSTRUCT
PACKET_RECONSTRUCT
SAVE_RECONSTRUCT
ENTITY_ADMIT
PRESENCE_PUBLISH
DURABLE_WRITE
```

### 10.2 Extreme “全部严格”

Extreme 对当前 Authority / Judgment / Candidate & Proven Result Surface：

```text
未知变换
未知 redefine/retransform
未知 loader copy
未知 generated/hidden code
未知 protected-state write
未知 critical dispatch
未知 result callback
未知 native bind/callback
```

采用：

```text
可前置拒绝
→ DENY

已经发生
→ QUARANTINE_RESULT_AUTHORIZATION
→ REPAIR
→ REVERIFY

只能观察
→ 不能伪造 DENIED
→ 停止 Ronova 对该结果的授权
→ FENCE 最小必要 scope
```

### 10.3 “全部严格”不是全 JVM 随机停机

“全部严格”表示：

> 当前对抗的关键结果路径不给未知执行默认有效资格。

不表示：

- 随机停止无关 Mod；
- 每 tick 扫所有类；
- 任意未知线程都杀；
- 对无关业务做无差别空返回；
- 把无法识别的东西直接升级 ROOT FENCE。

---

## 11. 防护平面

### 11.1 反拦截

防：

```text
input 被吞
event 被 cancel
packet 被吞/乱序
callback/listener 被移除
Hook 被替换
task 不执行
dispatch 被截断
completion 永不发布
```

Extreme：

- 多入口；
- receipt；
- execution continuity；
- alternate execution；
- final state validation；
- 失败按最小 scope fence。

### 11.2 反全返回四层

```text
方法 bytes
→ 最终 dispatch
→ 真实 completion
→ 最终 state
```

任何必要层失败：

```text
ANTI_ALL_RETURN_FAILURE
→ revoke affected lease
→ quarantine old result authorization
→ repair bytes/dispatch
→ repair state/continuation
→ bounded alternate execution
→ reverify all required layers
→ VERIFIED or FENCED(scope)
```

### 11.3 Fence Scope Escalation

正式 scope：

```text
TARGET_FENCED
OPERATION_FENCED
PLANE_FENCED
ROOT_FENCED
REALM_FENCED
```

升级规则：

#### TARGET_FENCED

仅某个目标或 lineage 的结果资格无法证明。

#### OPERATION_FENCED

某一个 CriticalOperation（例如 admission / dispatch / completion）失证。

#### PLANE_FENCED

某个完整执行平面（例如 Native evidence plane、runtime transform plane）在当前 JVM 失去可信性。

#### ROOT_FENCED

仅当以下核心条件之一失证时允许：

- 唯一 logical writer；
- root ownership / fence generation；
- authoritative epoch；
- durable truth 可解释性；
- command / commit correctness；
- Root 自身执行语义无法建立可信恢复。

#### REALM_FENCED

RealmId / durable lineage / store identity 本身冲突或无法解析。

原则：

> **一个敌方 hidden class、一个目标、一次未知 Native callback，不得自动把 Ronova 自己升级 ROOT/REALM FENCE。**

### 11.4 FENCE 不是“放弃攻击”

FENCE 表示：

> 当前 scope 不能安全地承认新的结果。

它不是：

```text
算了不管了
```

而是：

```text
保持 obligation
停止无证授权
继续有界 evidence / repair / alternate path
恢复条件满足后重新验证
```

### 11.5 隔离、恢复与攻击结果的独立更新

executionState 和 attackOutcome 只按 §6.5 解释。一次 fence 更新不能自动写入 CLEARED，也不能把 Authority receipt 改成失败；恢复 executor 返回成功同样不能直接把 PARTIAL 改成 CLEARED。

穿透后恢复流程：

```text
记录实际 state 与受影响结果范围
→ 隔离该范围无证结果授权，更新 executionState/fenceScope
→ 当前完成证明若失效，更新 attackOutcome=PARTIAL
→ 保持对应 obligation 或范围汇总
→ 按预算 repair / alternate execution
→ 验证真实 completion、最终 state 与全部必要出口
→ 条件恢复后解除相应执行限制
→ 清除请求另行完成九项条件和稳定窗口，才可 CLEARED
```

解除一个候选的限制前检查该 scope 的其他限制原因；某条来源被证明无关，不能顺带解除同一目标的其他恢复链隔离。Fence scope 按实际依赖范围决定，不机械地逐级扩成全局停摆。

COMMIT_UNKNOWN 必须以原 command id 查询/重放，不因 outcome 或 fence 改变而另建 command 重复提交。有限重试结束可以将本次尝试标为 FAILED，但不能删除其尚未解决的结果义务；后续恢复继续服从原有权威事实。

---

## 12. 运行时与宿主安全

### 12.1 Runtime Capability Ceiling

按 operation 报告：

```text
observe
prevent
repair
verify
```

禁止：

```text
Agent loaded
→ 推导“全部能拦”
```

### 12.2 Host Safety Governor

Extreme 必须有独立宿主安全 governor，至少约束：

- worker 数量；
- task queue；
- evidence queue；
- class/retransform rate；
- repair rate；
- JIT/native verification rate；
- per-source flood；
- backlog age；
- memory cap；
- CPU time budget。

`memory cap` 不得只解释成 Java heap。V2.4 冻结以下 `HostResourceDomain`：

| 资源域 | 最低约束 |
|---|---|
| JAVA_HEAP | Ronova-owned retained bytes、临时分配速率、GC 压力与软/硬预算 |
| DIRECT_BUFFER / MAPPED_BUFFER | direct/mapped buffer 的申请、容量、生命周期和未释放量 |
| METASPACE | 新增类、重定义生成物、元数据增长与回收趋势 |
| CODE_CACHE | JIT/生成代码占用、编译/重转换压力及接近满载状态 |
| THREAD / THREAD_STACK | worker 数、临时线程、线程栈预算、阻塞与失控线程增长 |
| CLASS / CLASSLOADER | loaded/generated class 数、loader 数、loader churn 与不可回收增长 |
| JNI_JNA_NATIVE | Ronova 经 JNI/JNA/Native bridge 申请的 native allocation 与释放回执 |
| JVMTI_OWNED | JVMTI 分配、tag/table/buffer 与对应释放责任 |
| FILE_DESCRIPTOR / PLATFORM_HANDLE | 文件、映射、进程内平台句柄和图形/事件对象的打开与关闭 |
| GPU_UPLOAD / RENDER_BUFFER | Ronova 可观测、可归属的纹理上传、VBO/SSBO/FBO、staging/render buffer 占用 |

每个资源域至少报告：

```text
supported / observed / unobserved
ownedUsage / ownedPeak / allocationRate
hostPressure
softBudget / hardBudget
throttleState / emergencyState
lastReleaseProgress
```

归属与压力必须分开：

- `ownedUsage` 是 Ronova 执行器、Agent、JVMTI、Native bridge、渲染器和诊断结构直接创建或能够可靠归属的资源；
- `hostPressure` 是整个 JVM/进程/驱动可观察到的剩余容量或危险趋势，不能自动归因于某个敌方来源；
- 敌方制造全局资源压力时，governor 可以减少新分配、降低并发、分片、延后或切换低分配替代路径，但不能据此删除攻击面、撤销 Judgment、释放旧 TargetEpoch、清空 obligation 或报告 CLEARED；
- 必须为 Authority command、durable receipt/recovery、必要 gate、最小 repair/reverify 保留独立应急预算；
- 资源域不可观测时必须报告 `UNOBSERVED`，并对 Ronova-owned allocation 使用保守硬上限；禁止把“无法测量”当成“无限可用”或“已经安全”；
- GPU/平台资源只对当前平台实际可观测与可归属的部分作强声明，不能用单一 Java heap 指标替代。

任何 Extreme executor 均不得通过耗尽 heap、direct/native memory、metaspace、code cache、线程栈、类加载容量、平台句柄或 GPU/render allocation 来获得对抗优势。

但语义必须是：

```text
预算耗尽
→ 暂停/分片/延后执行
→ obligation 仍存在
→ 下一预算窗口继续
```

禁止：

```text
预算耗尽
→ 把结果链从攻击域删除
```

达到 hard budget 时必须停止新的同类 Ronova-owned 分配并进入真实节流/替代路径；已经建立的权威事实和结果义务继续保留。若最小安全执行也无法维持，按实际受影响 scope 报告 FENCED/PARTIAL，不允许故意继续分配直至 JVM、游戏进程、显卡驱动或宿主系统崩溃。

### 12.3 不设 Mod 数量硬上限

如果同时有多个同级对抗 Mod：

```text
全部可进入 Candidate/Proven Result Surface
↓
按预算分页处理
↓
未处理部分保持 pending / quarantine / obligation
↓
后续继续
```

不允许：

```text
超过 2 个
→ 后面的不管
```

### 12.4 Candidate 反诱导与公平调度

调度器复用 Host Safety Governor，不新增 Authority。候选、规范 obligation 和范围汇总可保存以下有界调度信息：

```text
confidence
causalStrength
sourcePressure
repeatCount
protectedImpact
lastUsefulEvidence
firstPendingAt / lastServicedAt
```

confidence 与 causalStrength 由可验证证据更新，不信任来源自报。它们用于安排查证顺序，不能替代 Candidate Admission Rule，也不能直接把候选升级为 PROVEN_RELATED 或永久处置授权。

基础优先顺序：

```text
已观察到正在恢复目标 / TERMINAL → PRESENT
    > 正在改变关键状态
    > 未授权或授权不明的 protected sink/completion 触达
    > 同 lineage 的已关联恢复行为重复出现
    > 只有弱相关性的待查证噪声
```

已证明合法合规的操作不因活跃度高就被排入攻击候选。protectedImpact 必须来自真实结果观察；缺少直接状态证据时只能按已有证据等级安排，不能由对方填一个“高影响”字段取得高优先级。

| 调度因素 | 正式作用 | 禁止行为 |
|---|---|---|
| protectedImpact / causalStrength | 优先处理当前真实冲突结果和可验证的关键因果边 | 用类名、日志标签或自报严重度替代真实影响 |
| lastUsefulEvidence | 新增有用证据或实际查证进展可提高处理价值 | 重复发送同一证据就刷新“最近有效进展” |
| repeatCount | 合并重复语义，有限提升已确认持续冲突的处理紧迫性，贡献封顶 | 次数无限增大、权重无限上升或自动变成归属证明 |
| sourcePressure | 触发来源/结果范围配额、合并、背压与更粗汇总 | 洪泛越多分配预算越多 |
| 等待时间 | 在预算内作有界补偿，保证已有未决范围获得查证机会 | 永远只处理新来的高优先任务 |

预算分配必须同时具备：

1. 独立保留 Authority、commit/receipt、必要 repair/reverify 和 durable recovery 的最低执行预算；
2. 对真实结果漂移优先，但紧急处理本身也有总量与单来源上限；
3. 在目标/结果范围之间公平分片，同一来源不能耗尽全部攻击与证据预算；
4. 对低置信度未决候选和范围汇总保留非零、有界的探索预算；
5. 来源配额结合可验证 producer 与受保护结果范围，轮换 task、类名或 sourceIdentity 不能重置全部配额；
6. 来源归一无法完成时，进入已有未知来源预算组或范围汇总，不为每个新 ID 创建无限队列。

同源归组只用于预算和记录管理，不能成为“这一组全部敌对”的裁决依据。范围汇总必须有后续探查游标和服务记录；不能只永久保存一个 pending 标记却从不安排查证。

在待处理集合有限且 JVM 持续提供最低执行机会时，公平与探索机制必须避免已有未决范围永久饥饿。持续超载时不承诺每条 raw event 都能逐条处理或在固定时间内完成；必须保留有界范围约束、真实报告 pending/coverage gap，并持续对这些范围安排代表性查证。预算耗尽不能伪造 PROVEN_UNRELATED、DENIED 或 CLEARED。

### 12.5 Cross-surface Operation Plan

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
2. 同一 `planId + stepId + revision` 必须幂等；重复事件、重放和 COMMIT_UNKNOWN 不得产生第二次非幂等改写。
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

## 13. Evidence / Lease / TOCTOU

### 13.1 Evidence

继续维护：

- artifact baseline；
- sealed runtime baseline；
- loader inventory；
- dispatch revision；
- JIT/native revision；
- WriteSink evidence；
- ResultGraph revision；
- MutationTrace evidence；
- unresolved obligations。

V2.3 还需绑定 candidateAdmissionRuleRevision、独立证据依据、obligationCoverageRevision、范围汇总缺口、实际限制动作及 outcomeRevision。候选分数、事件数量和压力指标仍只是诊断或调度输入，不成为 Authority 投票。

V2.4 还需绑定 qualificationTransitionReceipt、expected/next TargetEpoch、Admission token revision、CompletionContract revision、completionSatisfied revision、restrictionRelease revision，以及各 HostResourceDomain 的 owned accounting/observability revision。V2.5 再绑定 profileMatrixRevision、surfaceRegistryRevision、requiredSurfaceSet、surfaceMaturityRevision、crossSurfacePlanId/stepRevision 与 causality token。资源压力、表面成熟度与释放状态同样只是执行和安全证据，不能成为 Judgment 投票或第二 Authority。

### 13.2 ExecutionLease

高强度操作必须绑定短期 lease。

lease 至少绑定：

```text
realm
root/epoch
policy revision
critical operation
target identity
lineage
target epoch
runtime seal
result graph revision
dispatch/native/sink revision
authorized result edges
obligation coverage revision / affectedScope
expiry
```

新增候选限制、汇总接力或覆盖缺口影响本次结果时，lease 在提交处必须复验。不能只检查已证明 ResultGraph 的 revision，忽略图外尚未证明但已经限制结果资格的候选范围。

### 13.3 三次复验 + 真实提交门

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

### 13.4 Qualification Transition Commit Gate

§13.3 的执行器 CAS 不能替代 §9.5 的 Authority 原子事务。凡 CriticalOperation 会终结、重新激活、重新绑定目标或使旧资格失效，必须先获得已经 durable commit 的 qualification transition receipt，再进入可能调用第三方代码的物理执行阶段。

```text
准备 evidence / expected revisions
→ canonical Root 原子提交 QualificationTransitionCommand
→ 获得 transitionReceipt
→ 发布新 gate revision
→ 签发绑定该 receipt 的 ExecutionLease
→ 执行 remove / rewrite / dispatch / repair
→ 完成后复验
```

Root transaction 内禁止调用目标实体、敌方 manager、第三方 callback 或可能重入 Root 的外部代码。事务只写内部权威状态和 receipt；物理攻击与投影发布在提交后执行，并由已经生效的新 TargetEpoch/Admission gate 覆盖。

因此，即使目标在 `remove()` 调用栈内同步创建新 UUID 身体，或者外部 SavedData manager 在下一 tick/重启后恢复身体，新对象也只能面对已经提交的新 epoch 与撤销事实，不存在“Judgment 已终结但旧资格仍可趁窗口提交”的状态。

若 transition commit 为 `COMMIT_UNKNOWN`，不得继续签发新的非幂等攻击 lease；先以原 command id 解析提交结果。已经提交但投影未完整发布时，恢复流程必须从 receipt 补建 gate，而不是回滚 durable transition。

---

## 14. ResultGraph 与 CandidateGraph

### 14.1 CandidateGraph

候选图用于：

- 收集疑似相关边；
- 做有界追踪；
- 暂时限制结果资格；
- 等待来源证明。

CandidateGraph 不能成为 Truth。

准入、证据独立性和处置权限唯一采用 §9.2。未决候选可以由细粒度记录或范围汇总表示；释放样本和图节点不等同解除结果限制。

### 14.2 ResultGraph

只有 `PROVEN_RELATED` 的节点/边进入正式 ResultGraph。

ResultGraph 用于：

- attack；
- interception；
- protection；
- completion contract；
- existence termination proof。

### 14.3 Graph Revision

新增关键边：

```text
resultGraphRevision++
→ affected lease stale
→ affected completion proof stale
→ revalidate
```

新增候选义务、范围汇总、覆盖缺口或限制交接涉及的变化推进相应 evidence/obligationCoverageRevision，同样使受影响 lease 和当前完成证明需要重验；不要求未证明的边先伪装成 ResultGraph 的已证明边。

### 14.4 图预算耗尽

图预算耗尽时：

```text
保持 unresolved obligation
保持对应结果出口限制
延后图扩展
```

禁止：

```text
淘汰候选
→ 当成无关
→ 宣布 CLEARED
```

V2.3 将“保留 obligation”明确为保留未解决的语义约束与受影响范围，不要求永久保留每条 raw event、每个 Java 对象或每个来源名字。

### 14.5 Obligation Compaction / 规范义务归并

细粒度 canonical key：

```text
ObligationKey
  = realm
  + lineage
  + targetEpoch
  + sourceIdentity
  + edgeKind
  + protectedResult
```

| 字段 | 约束 |
|---|---|
| realm / lineage / targetEpoch | 绑定当前受保护范围与资格代际，不能跨目标、跨代际混并后放行 |
| sourceIdentity | 优先引用可验证的 producer/注册关系/调用来源身份；不是任意自报字符串，也不永久强引用每个 task/Class 对象 |
| edgeKind | 区分写入、恢复、注册、调度、回调、重建、分派、发布等语义 |
| protectedResult | 指向受当前 Judgment 约束的具体结果或出口，不能只写“这个 Mod” |

Judgment、policy 与证据 revision 作为记录的上下文绑定一起保存。只有 key 与约束语义、上下文兼容时才能归并；不兼容记录须分开或转成显式待重验的范围汇总，禁止把旧证明拼成当前授权。

lineage/source 尚未知时使用已知结果范围的候选记录或汇总，不为每个未知字符串发放永久身份。更换 task 对象、类名或自报 ID 不自动意味着获得一份新的独立预算。

每条规范义务保存有界聚合信息：

```text
key / contextBinding
status / restrictionReasons
count / countSaturated
firstSeen / lastSeen
latestEvidenceDigest
representativeSamples[]
pressureScore
lastUsefulEvidence
restrictionRef / coverageRevision
```

例如同一语义的 100000 次重复提交，可保存为一条 unresolved obligation、计数 100000 和少量代表样本。计数采用有界、可饱和的表示；样本同时限制数量与总字节，字符串、堆栈、来源链和摘要计算也受预算约束。

处理流：

```text
Raw event → bounded ring buffer
重复语义 → merge/coalesce 到已有 ObligationKey
证据细节 → digest + 有界代表样本
结果义务 → 保留状态、限制原因、上下文和未闭合事实
```

digest 与 count 用于查重、摘要和调度，不是因果证明、授权凭证或完成证明。仅增加重复计数不能提升归属状态，也不能用“摘要相同”直接认为不同上下文已解决。

### 14.6 不同 key 洪泛与范围汇总

规范 key 归并只能压缩重复事件。对方持续产生不同 sourceIdentity、task、callback、hidden class、lineage 或结果边时，细粒度 key 数量仍可能增长，因此必须同时具备容量溢出汇总。

```text
可用细节预算内
  → 细粒度 ObligationKey

细节 key / 字节预算耗尽
  → 在有界范围汇总中登记受影响结果集合与未闭合事实
  → 接力维持临时结果限制 / coverage gap
  → 释放被覆盖的细节记录
  → 以后按预算选择代表性来源重新展开
```

范围汇总至少保存：

```text
affectedScope / protectedResultSelector
contextBinding / coverageRevision
unresolvedCoverageGap
restrictionRef / restrictionReasons
aggregateCount / countSaturated
firstSeen / lastUsefulEvidence
boundedRepresentativeSamples[]
nextProbeCursor / resolutionPredicates
```

汇总范围必须以可验证的目标、操作或受保护结果出口表示，不能把所有子 key 放进一个无限增长的集合，也不能依据一个样本就认定整个范围全部敌对。只保留“此范围仍有未解决来源/覆盖缺口”及其临时结果约束。

细粒度表、范围汇总表、样本和索引都必须有独立字节上限。汇总表也满时，使用预留的有界应急覆盖标记和更粗但仍有实际依据的结果范围；新输入合并计数或被背压，不再无限分配对象。scope selector 采用受控的有限表达，不携带全部事件、目标或来源清单。

更粗汇总表示降低记录精度，不表示扩大敌对裁决或永久失能权限。按实际受影响的 TARGET/OPERATION/PLANE 暂停无证结果授权；只有 §11.3 的 Authority 条件失证才能升级 ROOT/REALM，不能因为记录容量满就 Root 停摆。

未进入细节表的输入仍由原受保护 gate 验证授权。没有可控制出口时，必须如实标为覆盖缺口及 PARTIAL/FENCED，不能把范围标记当成真正拦截成功。

```text
记录精度允许下降
未解决事实不能消失
约束不能因压缩自动变弱
攻击成功不能由容量变化推导
```

### 14.7 压缩、淘汰与限制交接

细节记录转成范围汇总时必须依次完成：

1. 计算覆盖原未决事实的 affectedScope 与仍需保留的结果限制；
2. 在预留预算内建立或更新汇总，登记覆盖缺口与限制原因；
3. 通过实际门控同步或版本化协议确认新旧限制连续有效；
4. 推进相关 coverage revision，使受影响 lease 和当前完成证明失效并待重验；
5. 之后才释放已被汇总覆盖的细节、样本与索引。

并发提交不能在“细节先删、汇总后建”的空窗获得资格。交接失败时保留原限制，或使用已有应急结果 gate/范围标记继续拒绝无证授权；无法保证控制则按最小 scope FENCED，禁止报告交接成功。

Raw ring buffer 可以循环覆盖历史事件，但若覆盖造成当前关键证据缺失，要留下有界覆盖缺口，不能把“没有事件记录”解释成“没有攻击”。LRU/TTL 只能释放由其他记录有效承接的义务细节或非必要样本，不能直接删除未决语义义务。

汇总之后仍需调度查证。达到预算、样本全部过期、计数归零、压力下降或最近无漂移，都不能独立清除 unresolvedCoverageGap。不能通过无限落盘日志或无限扩张索引把内存问题转移到另一种资源。

### 14.8 义务解除与证据权限

解除义务必须有当前上下文中的可验证依据，例如：

- 完整受影响关系已被证明无关，且没有其他有效限制原因；
- 对应恢复/写入/完成出口已经实际受控，并满足该 obligation 的 resolutionPredicates；
- 唯一 Authority 已作出合法后续资格转换，同时证明旧资格仍被拒绝、新资格按新 epoch 验证。

一个代表样本无关，不足以清除整个范围汇总；销毁一个来源对象，不足以证明其任务/回调后继全部失效。只有范围级覆盖证明或逐步查证已覆盖整个汇总范围时，才能解除该范围的缺口。

Obligation、CoverageGap、CandidateGraph 与汇总记录均是派生证据。它们可以依据既有 Root policy 使必要前提失证、限制 lease 和阻止虚假完成声明，不能自己提交 Tombstone、推进 TargetEpoch 或生成永久实体黑名单。临时 gate 的策略执行仍受 canonical Root API 与单 writer 约束。

---

## 15. Authority、持久化与恢复

### 15.1 权威对象

```text
StrengthConfigRequest
→ StrengthPolicySnapshot
→ StrengthResolutionReceipt
```

运行时诊断不是 Truth。

### 15.2 ResultGraph / Certificate 不写 Truth

禁止：

```text
ResultGraph says related
→ 自己提交 Tombstone

Certificate says CLEARED
→ 自己 deny spawn
```

必须：

```text
Root Truth
→ Judgment / Tombstone / TargetEpoch
→ Admission policy
```

图和证书只提供证据。

### 15.3 Restart

重启：

```text
durable truth
→ restore Realm/Root/Tombstone/TargetEpoch
→ rebuild policy
→ rebuild candidate/result evidence
→ revalidate old certificate scope
```

旧会话证书不能直接充当新 JVM 的当前完成证明。

### 15.4 V2.3 未决范围与重启恢复

从 7.17 起，必要的 pending 范围摘要、策略/证据 revision 与恢复进度可经现有受控持久化通道保存；它们由当前阶段唯一 writer 管理，不成为第二份 Judgment 或 Admission Truth。不要求保存完整 raw 事件或完整堆对象图。

恢复先依据 durable truth 建立 Tombstone、当前 TargetEpoch、policy 与受保护结果 gate，再重建候选和结果证据。旧会话缺口摘要只提示“这些范围需要重验”，不直接永久判定其中全部来源敌对；缺口摘要缺失也不能推定当前已完成。

压缩接力期间崩溃时，新会话在相关受保护结果重新开放前必须验证覆盖与资格；未完成验证保持相应 pending/隔离状态。只有当前完成条件和稳定窗口重新通过，才能生成当前 CLEARED，旧会话的成功回执只保留为历史。

本节属于 7.17/L6E 合同，不要求给 7.16 追加完整图、压缩日志或证书生产 schema。

### 15.5 Qualification Transaction 崩溃恢复

V2.4 要求 durable recovery 以资格事务 receipt 为边界，不得分别猜测 Judgment、TargetEpoch 与 Admission 的最新值。

恢复时至少验证：

```text
commandId 唯一且可重放
transition receipt 与 durable commit index 一致
nextJudgment / nextTargetEpoch / revokeThroughEpoch 一致
Admission token revision 与 nextTargetEpoch 一致
projection revision 不领先于 durable truth
旧 epoch 不因投影缺失、回执重放或合法 reactivation 重新有效
```

处理规则：

- 存在完整 durable transition receipt：整体恢复新资格状态，再补建投影和 gate；
- 只有未提交 intent / journal prepare：不得发布其中任何资格变化；
- 回执状态未知：以原 command id 解析，不生成替代 transition；
- Judgment、epoch、撤销范围或 Admission revision 出现不可解释的分裂：按 Authority 核心失证处理，进入最小必要 ROOT/REALM FENCE，禁止随机选择一侧继续；
- 恢复完成前，旧 task、packet、save、entity 与 callback 不能因缓存为空获得默认授权。

同一规则适用于终结、合法重新激活、重新绑定和显式解除裁决，确保重启不能把一次原子 transition 拆成两段。

### 15.6 Runtime Surface 持久化边界

V2.5 允许持久化的只是重建执行所需的最小事实：

```text
surfaceRegistryRevision / profileMatrixRevision
pendingSurfaceScopeSummary[]
crossSurfacePlanResumeToken[]
lastAcceptedStepReceiptRef[]
completionDependencyRevision
```

禁止持久化 Surface 本地 Judgment、独立 TargetEpoch、客户端“最终状态”、完整 raw event 流、运行时对象引用或无界 plan history。重启顺序必须是：

```text
恢复 Root durable truth
→ 恢复 policy/matrix revision
→ 建立旧 epoch gate
→ 重建 required Surface capability/maturity
→ 以原 command/plan id 恢复未决 step
→ 重验 completion 与 projection
```

某 Surface 在新 JVM 中缺失、版本变化或降到较低 maturity 时，相关计划按实际进入 PARTIAL/FENCED/UNAVAILABLE；不得使用旧会话的 Surface receipt 伪造当前 FULL。合法新 epoch 只由新的 QualificationTransition receipt 建立，Surface 恢复不能自行“修正”权威代际。

---

## 16. 分阶段实施路线

### 16.1 7.16 — 不返工原则

7.16 第一职责仍是：

```text
DurableRootStore sole-writer cutover
Tombstone / Admission takeover
JudgmentBatch
receipt / replay / COMMIT_UNKNOWN
StrengthPolicy foundation
```

V2.2 对 7.16 只追加：

- 新攻击 `StrengthOptionKey` 预留；
- EXTREME 显示名“极限代码对抗”；
- versioned extension seam；
- supported/effective/unavailable 状态；
- 后续 ResultGraph/MutationTrace/Certificate 的接口占位（若当前 schema 已方便容纳）。

不要求为了 V2.2 现在完成：

- CandidateGraph executor；
- MutationTrace；
- 十五项攻击生产实现；
- Full ResultGraph schema；
- ExistenceTerminationCertificate；
- JIT/native full closure。

V2.3 不再扩大上述职责。Candidate Admission Rule、Obligation Compaction、AttackOutcome/Execution 分离和反诱导调度的完整 schema、实现、GUI 结果投影与新增对抗测试全部从 7.17 起安排；7.16 继续原有 cutover 和扩展接口工作。

V2.4 同样不把 7.16 扩成全 JVM Extreme。它只要求：凡 7.16 cutover 已经提交会改变资格代际的 canonical command，不得把 Judgment、TargetEpoch、撤销事实、Admission revision 和 receipt 分成多个可独立发布的写入；尚未进入 7.16 生产范围的完整 TargetEpoch propagation、全资源域 governor、Completion 最终性投影和 Candidate 释放防抖仍按 7.17/L6E 路线实现。

V2.5 继续锁定这一出口。7.16/r52 只保留五十四项稳定 key、versioned extension registry 和现有 policy resolution seam；不要求新增八套 production adapter、逐键 ProfilePresetMatrix、CrossSurfaceOperationPlan 或跨客户端/服务端完成证明。实现可以预留 typed 接口，但不得把占位类、统一 plane 映射或测试桩报告成 Runtime Surface FULL。

### 16.2 7.17 — Enforcement Foundation

建立：

- Original / Sealed Baseline；
- SemanticAttestor；
- LoaderProof；
- Final Guard；
- runtime compiler defense；
- dynamic dispatch；
- WriteSink evidence；
- ExecutionLease；
- TargetEpoch propagation；
- Candidate/ResultGraph 基础；
- MutationTrace 基础；
- host-safety governor；
- fence scope；
- anti-AllReturn 基础。

V2.3 在本阶段冻结并实现四项共同基础：

- §9.2 的候选准入、独立证据判定、合法操作过滤及临时处置权限；
- §14.5–§14.8 的规范义务、不同 key 溢出汇总、无放行窗口的限制接力与解除条件；
- §6.5 的执行/结果正交状态及 §11.5 的恢复后结果更新；
- §12.4 的来源归组、压力限流、公平分片、探索预算与防刷量调度；
- X31–X44 中基础层的正向、对抗、故障与恢复用例。

V2.4 在本阶段追加四项生产基础：

- QualificationTransitionCommand 的原子 durable commit、receipt、projection publish 与崩溃恢复；
- `HostResourceDomain` inventory、Ronova-owned accounting、host-pressure 分离、软硬预算和不可观测状态；
- `completionSatisfied` 的 CompletionReceipt、运行时更新、证据失效和 GUI 投影；
- Candidate 临时限制的 `restrictionReleaseState`、硬截止、重验证、取消和显式 Authority command 分流；
- X45–X48 中属于 7.17 能力面的正向、并发、故障、压力与恢复用例。

V2.5 在本阶段追加广度基础：

- versioned `ProfilePresetMatrix`、逐键解析回执、迁移和 effective ceiling；
- 八类 Runtime Surface registry 与 S0–S6 maturity report；
- Data、Temporal、Registry、Network 的基础 observer/attribution/enforcement adapter；
- Presentation/Input 的服务端 Truth 边界、认证请求与只读投影；
- Event/Resource 的基础 provenance、loader/producer 绑定和有界证据；
- `CrossSurfaceOperationPlan`、step lease、幂等回执、因果 token 与冲突仲裁；
- X49–X56 中属于 7.17 基础能力的运行时用例。

至少 Compatible / Enhanced 正式闭环。

Extreme：

> 只对已经真正具备 executor + evidence + repair + test 的选项显示 ACTIVE。

尚未完成的动态、重建或 Native 适配继续标记其真实状态；共同基础通过不能当成全部十五项 Extreme 已交付。任一相关结果缺口未闭合时不签发 CLEARED。

### 16.3 L6 — Internal Dominion Root

InternalDominionRoot：

- 唯一 logical writer；
- 统一编排 policy / Judgment / TargetEpoch / lease；
- Candidate/ResultGraph 只作为 evidence 输入；
- Attack / Intercept / Protection executor 只能请求 Root API；
- Root 决定何时推进 TargetEpoch；
- Root 以单一 qualification transaction 同时提交 Judgment、TargetEpoch、撤销范围、Admission revision 与 receipt；
- Root 决定 fence scope 的权威升级。

V2.3 四项合同、V2.4 收口合同和 V2.5 八类 Runtime Surface 的报告、限制、资源预算与调度通过同一 Root policy 编排。候选分数、汇总记录、outcome/completion 投影、释放状态、Surface cache 及宿主 governor 都不能成为第二 Truth，不能自己推进 TargetEpoch 或复制 command 提交。Root 统一批准 CrossSurfaceOperationPlan，Surface executor 只返回 typed receipt。

### 16.4 L6E — Extreme Closure

#### P0-A

Truth / Policy / TargetEpoch / Completion / WriteSink / Lease 合同，以及 QualificationTransition 的原子提交、幂等回放和崩溃恢复。

同时承接 V2.3 四项合同与 V2.4 completion/release/resource evidence 的上下文绑定、派生证据边界和 execution/outcome 报告语义，不新增 writer。

#### P0-B

- 全返回；
- 全返回拦截；
- 反全返回；
- 逻辑源 / 任务链 / 回调链；
- 调用链；
- dispatch；
- final result takeover；
- source trace。

完成任务/回调/分派/来源追踪上的候选准入、独立证据、临时结果限制、重复义务合并与公平调度；APPLIED/PARTIAL/FAILED 必须由真实完成和效果证据区分，所有非清除 CriticalOperation 必须有可重验证的 completionSatisfied 最终性。

#### P0-C

- durable lineage；
- 重生 / 重建；
- state / registration / recovery source；
- network/save reconstruction；
- Presence/HUD；
- Existence Termination；
- 合法 reactivation epoch。

完成跨 lineage/TargetEpoch 的义务绑定、范围汇总、重建入口控制、当前 CLEARED 与历史证书分离，以及重启/合法重新激活后的 qualification transaction 重放与重新验证；Candidate 自动释放防抖不能放行旧 epoch 或延迟明确的操作者 Authority command。

#### P0-D

- semantic seal；
- dynamic code；
- runtime patch disablement；
- all-loader；
- JIT/native evidence；
- recovery kernel；
- repair/reverify；
- Extreme 全登记能力 ACTIVE 或明确最小 scope FENCED。

完成动态类、loader、Native 来源不断变化时的容量溢出汇总与反诱导调度；穿透、限制交接失败和证据丢失时保持真实 PARTIAL/FENCED，修复后再验收当前结果。Agent/JVMTI/Native/renderer 必须纳入完整 HostResourceDomain 记账。八类 Runtime Surface 必须达到各 CriticalOperation 所需的 S6 完成条件，跨表面计划无循环修复、无第二 writer、无客户端越权。X31–X56 必须完成相应成品运行时与故障恢复验证。

### 16.5 V2.5 Runtime Surface 阶段归属

| Surface / 合同 | 7.16 | 7.17 | L6 | L6E |
|---|---|---|---|---|
| ProfilePresetMatrix | 仅稳定 key/seam；r52 统一 plane 映射保持事实口径 | 逐键矩阵、revision、迁移、解析 receipt | Root authoritative policy snapshot | 全 key、全 ceiling、exact-artifact 验收 |
| Data | 复用现有状态/持久化基础 | 状态源、rollback、save 基础 | Root plan 与 durable truth 边界 | 全写入源/恢复出口 S6 |
| Temporal | 复用 bounded worker/重试基础 | task/callback provenance 与 epoch gate | Root lease/调度 | scheduler/thread/native callback S6 |
| Registry | 复用 entity/admission 入口 | registration/lineage observer 与 gate | Root qualification 编排 | factory/loader/re-registration S6 |
| Network | 保持现有服务端请求边界 | identity/nonce/epoch/revision 与 receipt | Root-approved sync/projection | 重发、镜像、跨重启重建 S6 |
| Presentation | 不扩张专用服务端依赖 | 只读 projection、HUD/presence 基础 | Root outcome projection | renderer/resource/network 联合 S6 |
| Input | 保持现有键位/命令入口 | authenticated request/receipt、反吞输入基础 | Root command ingress | callback/dispatch/network 联合 S6 |
| Event | versioned adapter seam | listener/callback attribution 基础 | Root candidate policy | 动态订阅/lambda/native event S6 |
| Resource | capability report/seam | class/resource/loader producer 基础 | Root repair plan | dynamic/JIT/native/render resource S6 |
| Cross-surface plan | 无 production 义务 | plan/step/lease/receipt 基础 | Root 唯一批准者 | 所有 required surface completion 与恢复闭合 |

---

## 17. 能力—阶段映射

| 能力 | r51 | 7.16 | 7.17 | L6 | L6E |
|---|---:|---:|---:|---:|---:|
| Durable writer | Shadow | Sole writer | Harden | Root backend | Full |
| StrengthPolicy | 无正式模型 | 基础/持久化、plane 统一映射 | 逐键真实三档 | Root 持有 | Full |
| ProfilePresetMatrix | 无 | stable key/seam | versioned matrix + receipt | Root authoritative snapshot | 全 key/ceiling/exact-artifact Full |
| Extreme Code Duel 定位 | 无 | policy/UI 冻结 | runtime profile | Root 编排 | Full |
| TargetEpoch | 无 | 现有 cutover 所需接口 | propagation 基础 | Root authoritative generation | P0-C Full |
| Qualification transition atomicity | 无正式合同 | 已有资格命令不得分裂提交 | durable command/receipt/publish 基础 | Root single transaction | 全路径与崩溃恢复 |
| Candidate Surface | 无 | extension seam | 基础 | Root policy | L6E Full |
| ResultGraph | 无 | 不强制完整实现 | 初版 | Root evidence input | Full |
| MutationTrace | 无 | key/seam | 初版 | Root evidence input | P0-B/P0-D |
| Existence Termination | 无正式证明 | predicate seam | 基础 | Root certificate orchestration | P0-C/P0-D |
| Fence Scope | 零散 | contract 可兼容加入 | 正式 | Root authority | Full |
| 全返回三件套 | 无/零散 | contract key | 基础 | Root lease | P0-B/P0-D |
| dynamic/JIT/native | 部分入口 | capability report | evidence | Root lease | P0-D |
| host-safety governor | 14D/预算基础 | 保持/扩展缝 | 全资源域 inventory 与 owned/host 分离 | Root-aware | Agent/JVMTI/Native/renderer Full |
| Candidate Admission / 处置权限 | 无本合同完成证明 | 不追加生产实现 | 规则、去重、合法过滤、临时 gate | Root policy 授权 | P0-B/P0-C/P0-D |
| Obligation Compaction / 溢出汇总 | 无本合同完成证明 | 不追加生产实现 | 归并、字节上限、汇总接力 | Root policy / evidence | P0-B/P0-C/P0-D |
| Execution / AttackOutcome | 无本合同完成证明 | 保持既有扩展缝 | 正交报告、回执与 GUI 投影 | 与 Authority commit 分离 | P0-B/P0-C/P0-D |
| Completion finality | 无正式字段 | extension seam | completionSatisfied receipt/GUI | Root 接受完成证明 | 全 CriticalOperation/reverify |
| Candidate 反诱导调度 | 无本合同完成证明 | 保持既有预算 | 压力限流、公平、探索预算 | Root-aware，非 Truth | P0-B/P0-C/P0-D |
| Candidate restriction release | 无防抖合同 | 不追加生产实现 | RELEASE_PENDING/硬截止/回执 | Root policy 执行 | 跨重启/动态来源 Full |
| Runtime Surface registry | 无 | versioned extension seam | 八类 registry + S0–S6 report | Root policy binding | 全表面 maturity Full |
| Data / Temporal / Registry | 零散路径 | 保持现有基础 | observer/attribution/gate 基础 | Root operation plan | source/recovery/rebuild S6 |
| Network / Input / Presentation | 基础请求/渲染 | 不扩大边界 | identity/epoch/receipt/projection 基础 | Root ingress/outcome projection | reconstruction/display S6 |
| Event / Resource | 零散 hook/class 入口 | capability seam | listener/producer/loader evidence | Root candidate/repair plan | dynamic/JIT/native/resource S6 |
| CrossSurfaceOperationPlan | 无 | 不追加生产实现 | plan/step/lease/idempotency 基础 | Root sole approver | completion/conflict/restart Full |

---

## 18. 依赖与非法组合

必须拒绝：

- EXTREME 显示 ACTIVE，但关键 executor 不存在；
- 极限清除没有 TargetEpoch / Admission / stability / recovery closure；
- 反全返回 Extreme 没有 final state evidence；
- 全返回 Extreme 没有 final result takeover；
- 动态代码 Extreme 只有观察，没有 dispatch/result outlet 控制，却显示已完成；
- 全部严格只能观察却声称 DENIED；
- budget overflow 后删除 obligation；
- 单个 TARGET 失证直接 ROOT FENCE；
- 合法 reactivation 复用旧 TargetEpoch；
- ResultGraph/Certificate 直接作为 Judgment/Admission Truth；
- 7.16 为了 V2.2/V2.3/V2.4/V2.5 重做完整 ResultGraph / MutationTrace / Extreme executor；
- 通过 heap/direct/native/metaspace/code cache/线程栈/句柄/GPU 资源耗尽、无限扫描、文件/存档/OS 破坏增强 Extreme。

V2.3 另外必须拒绝：

- 两份重复日志、同一事件的派生字段或过期跨 epoch 证据凑成两条独立弱证据；
- 合法合规修复仅因触达 protected sink 就被自动攻击或隔离；
- Candidate/Unresolved 仅因分数高、等待久或次数多而直接获得永久/类级/高副作用改写权限；
- 只做重复 key 合并，却允许不同 sourceIdentity 或汇总记录无限增长；
- 先删细节限制、后建立汇总，造成并发提交放行窗口；
- LRU、TTL、样本过期、摘要相同或计数变化直接解除未决义务；
- 一个代表来源被证明无关，就解除整个汇总范围及其他仍有效的限制；
- FENCED、QUARANTINED、调用过执行器或恢复函数返回成功直接映射为 CLEARED；
- 当前证书已经失效，UI 仍显示历史 CLEARED 作为当前结果；
- 通过更换来源身份重置配额，或让 sourcePressure/repeatCount 无界提高优先级；
- 没有 Authority/repair 保留预算，或只有严格优先级而无公平与探索机制；
- 以新增四项合同为理由要求 7.16 完成全套实现。

V2.4 另外必须拒绝：

- Judgment transition、TargetEpoch 推进、旧代际撤销、Admission revision 与 receipt 分开提交或分开发布；
- `COMMIT_UNKNOWN` 时新建第二 command 补写 epoch，或在 durable commit 前发布新 projection/gate；
- qualification transaction 内调用第三方代码，允许同步重建回调重入 Root；
- 非清除操作仅凭 APPLIED 就显示最终完成，没有有效 CompletionContract 与 `completionSatisfied=true`；
- 用 `isTerminalOutcome` 混淆 CriticalOperation 完成和实体 TERMINAL；
- Candidate 已 PROVEN_UNRELATED 后篡改证据结论来实现防抖，或让重复日志/身份轮换无限延长自动释放窗口；
- Candidate 释放防抖偷偷延迟明确的操作者 `clear_uuid`/解除裁决 Authority command；
- 只监控 Java heap，却宣称 direct/native/metaspace/code cache/线程/类加载/句柄/GPU 资源安全完整；
- 把不可观测资源视为无限可用，或把敌方制造的 host-wide pressure 当成归属证明、删除攻击面、清空 obligation、撤销 Judgment；
- 通过触发 OOM、Direct buffer memory、metaspace/code cache 枯竭、线程/句柄泄漏、驱动崩溃来冒充攻击成功。

V2.5 另外必须拒绝：

- Data/Network/Presentation 等任一 Surface 保存可与 Root 竞争的 Judgment、epoch、Admission 或“表面真相”；
- 为八类表面新增八套互不兼容的攻击 key、GUI 顶层页或 Authority 状态机；
- 三个预设仍只按 plane 统一映射，却宣称已实现逐能力矩阵；
- Surface 只有 observer 就显示 FULL，只有发出 executor 请求就显示 DENIED/APPLIED，repair 返回成功就显示完成；
- 客户端 HUD、renderer、keybind 或 packet cache 直接提交 Judgment、推进 TargetEpoch 或签发 CLEARED；
- 专用服务端加载客户端渲染类、shader 或只在客户端存在的 API 后再用异常降级掩盖边界错误；
- 各 Surface executor 彼此直接调用、循环修复、重复提交 canonical command 或以最后回调覆盖 Root 仲裁；
- 跨表面计划不绑定 RealmId、lineage、TargetEpoch、commandId、revision 或 idempotency key；
- 一个表面完成就忽略 required surface 缺口，把跨表面结果显示为 FULL/CLEARED；
- 通过扩大全局 Mixin/Unsafe/ClassLoader/JNI 禁用范围来替代来源归属、最小 scope 和 Host Safety；
- 以 V2.5 为理由把 r52 的占位接口、统一 mapping 或 source-level 编译通过写成 exact-artifact 已完成。

---

## 19. 验收体系

### 19.1 Extreme Code Duel 必测

V2.1 原 X01–X23 与 V2.2 的 X24–X30 全部继承。以下先保留 V2.2 用例，V2.3 的四项合同用例见 §19.3，V2.4 的 Design Freeze 收口用例见 §19.4，V2.5 的 Runtime Surface 与逐能力矩阵用例见 §19.5；这里列的是验收要求，不是已运行通过的报告。

#### X24 — Candidate Default-in

未知恢复源第一次导致漂移：

```text
必须先进入 candidate
不能因“证据不足”直接从范围消失
```

#### X25 — Proof Controls Severity

同一 shared manager：

- 目标相关边有证据；
- 无关业务无证据。

要求：

```text
目标相关结果必须严格失能
无关路径不能仅因同类/同 manager 自动判成敌对
```

但如果唯一可行执行路径会让 shared manager 整体失效：

> Extreme 不因兼容性退回弱执行，但必须记录 impact scope 和恢复基线。

#### X26 — Fence Scope

单个 hidden class 失证：

```text
TARGET/OPERATION fence
```

不能自动：

```text
ROOT/REALM fence
```

除非 Authority core 同时失证。

#### X27 — Legal Reactivation

```text
epoch 20 TERMINAL
→ epoch 21 revoked
→ Authority 合法重新激活
→ epoch 22
```

旧 20/21 task/packet/entity 永远不能重新获得资格。

#### X28 — Budget Persistence

攻击面洪泛导致本窗口预算耗尽：

```text
pending obligation 必须保留
下一窗口继续
```

不能通过丢弃 backlog 声称 CLEARED。

#### X29 — Multi-opponent Scale

同时出现多个独立同级结果链：

- 不检查“是否超过 Mod 数量上限”；
- 按预算分片；
- 所有已建立 obligation 保留；
- 不能因为数量超过测试推荐值忽略后续目标。

#### X30 — Host Safety

高压对抗下：

- 无无限线程；
- 无无限 queue；
- 不以 heap/direct/native/metaspace/code cache/线程栈/句柄/GPU 资源耗尽作为策略；
- 无无限 retransform；
- 不损坏存档/用户文件；
- JVM 可持续运行；
- 详细资源域与归属验收继续执行 X46 和 §20.4。

### 19.2 Completion Gate

宣布 Extreme 完成必须同时满足：

- 无 silent downgrade；
- 无第二 writer；
- 资格代际 transition 原子提交且崩溃可重放；
- Candidate 未决义务不会被静默删除；
- Proven result edges 全部有 policy；
- TargetEpoch 正确撤销旧资格并支持合法新代际；
- Fence scope 最小化；
- ResultGraph/Certificate 没有变成 Truth；
- Resource budget 只节流、不缩域；
- 无 Mod 数量上限；
- Host Safety 测试通过；
- 所有 V2.1 关键闭环继续通过。

V2.3 出口还要求：候选准入与临时处置权限可验证；重复和不同 key 洪泛下数据结构总字节有界；压缩不放行、不丢失未解决事实；执行隔离与攻击结果不混写；公平探索与核心恢复预算实际推进；X31–X44 对应层级通过。

V2.4 出口还要求：Judgment/TargetEpoch/撤销/Admission/receipt 无分裂提交窗口；非清除 CriticalOperation 的最终完成可机器判定；Candidate 自动释放防抖有硬截止且不篡改显式 Authority command；全部可用宿主资源域具备 owned accounting、host-pressure 分离和真实节流；X45–X48 对应层级通过。

V2.5 出口还要求：三个预设由逐键 ProfilePresetMatrix 真实解析；八类 Runtime Surface 不产生第二 Truth；每个 required surface 按 S0–S6 报告真实成熟度；客户端/专用服务端边界成立；跨表面计划幂等、无循环修复并由 Root 唯一批准；X49–X56 对应层级通过。

### 19.3 V2.3 四项执行合同验收

| 编号 | 场景 | 必须证明的结果 |
|---|---|---|
| X31 | 分别输入一条有效强证据、两条独立弱证据、单条弱证据，以及合法合规 sink 写入 | 前两者进入 Candidate；单弱只观察；合法合规操作不被自动隔离；原 gate 对无权写入的拒绝不等待候选凑数 |
| X32 | 同一事件被两个 listener 记录、日志转发、同源派生字段、过期/跨 epoch 证据 | 不虚增独立证据数量，不错误准入或升级；观测到的新一轮旧 epoch 提交仍按当前上下文处理 |
| X33 | Candidate 分数很高或长期未决，试图改写共享类；随后仅一项原因证明无关 | 候选只获临时结果限制；高副作用需正式证明与 Root policy；只解除已消失的限制原因 |
| X34 | 同一语义重复提交 100000 次并继续增长 | 归并到规范 obligation；计数可饱和、样本和总载荷有界；重复次数不直接变成归属或完成证明 |
| X35 | 持续轮换 sourceIdentity/task/callback/hidden class/lineage，并填满细节表和汇总表 | 转成有界范围汇总及预留覆盖标记；键、样本、索引和总字节均不无限增长；未处理部分保持约束和待查证事实，不自动 Root fence |
| X36 | 细节向汇总交接时并发提交、容量耗尽或交接中断 | 新旧结果限制连续，缺口不能获得授权；交接失败保留原限制或进入真实最小 scope FENCED，不伪报成功 |
| X37 | LRU/TTL、原始缓存覆盖、样本过期、低压力、计数变化或 digest 相同 | 不把未决义务变成无关/完成；关键证据丢失使 coverage proof 失效，不能签发 CLEARED |
| X38 | Ronova 已停止授权，但敌方 manager 每 20 tick 继续重建实体 | executionState=FENCED，最小 TARGET/OPERATION scope，attackOutcome=PARTIAL；Raw Observation 仍报告 PRESENT，UI 不显示攻击成功 |
| X39 | 曾经 CLEARED，随后新恢复边、真实漂移或必要证据失效 | 历史回执保留，当前证书失效并推进 outcomeRevision，当前结果重新评估为 PARTIAL；无关 scope 的有效证书不被一并抹掉 |
| X40 | 仅调用执行器、确认局部效果、确定执行失败、以及 COMMIT_UNKNOWN 各自出现 | 仅调用不产生 APPLIED；局部效果不等于 CLEARED；FAILED 不回滚 Judgment；未知提交以原 command id 查证，无第二 writer 或重复非幂等提交 |
| X41 | 高频伪候选、重复高严重度自报、重复证据刷新、轮换身份抢配额 | 优先级依据真实影响；重复贡献封顶，压力触发限流/合并，身份轮换不刷新全额配额，lastUsefulEvidence 不被无效重复刷新 |
| X42 | 多目标、持续高优先输入与低置信度未决范围共存 | Authority/repair 保留预算实际前进；按范围公平服务并保留探索，汇总有探查进度；有限集合无永久饥饿，持续超载如实报告 pending 而非虚假完成 |
| X43 | 同来源但不同目标、结果出口、policy/epoch 上下文被请求合并；仅一个代表样本证明无关 | 不混并冲突约束或拼接旧证明；一个样本不能解除整个汇总范围；合法新 epoch 不使旧任务恢复资格 |
| X44 | 压缩或结果更新期间崩溃，重启时旧证书存在但缺口摘要不完整 | 先恢复 Root Truth 与必要 gate，再重建当前证据；缺日志不表示已完成，旧证书不直接放行，重新满足条件后才 CLEARED |

7.17 验证四项基础合同；P0-B/P0-C/P0-D 分别把同一合同应用于执行链、重建与动态/Native 来源。不能以测试桩返回 FENCED 代替实际状态、限制动作、内存界限和恢复后完成证明。

### 19.4 V2.4 Design Freeze 收口验收

| 编号 | 场景 | 必须证明的结果 |
|---|---|---|
| X45 | 对终结、合法重新激活、重新绑定和解除裁决分别在 qualification transaction 的 prepare、durable write、receipt、projection publish 前后注入崩溃；同时让旧 epoch task 在 Judgment transition 附近并发提交，并模拟 COMMIT_UNKNOWN 重放 | Judgment、TargetEpoch、revokeThroughEpoch、Admission revision 与 receipt 只能整体可见或整体不可见；旧 task 不存在抢占窗口；恢复使用原 command id，不重复推进 epoch、不产生第二 writer、不发布半个 transition |
| X46 | 分别及组合施压 heap、direct/mapped buffer、metaspace、code cache、线程/栈、class/loader、JNI/JNA/JVMTI allocation、文件/平台句柄与可观测 GPU/render buffer；再由敌方制造不可归属的 host-wide 压力 | Ronova-owned usage 有界并在硬预算前节流/替代；全局压力不被伪造为归属证明，不删除攻击面或 obligation；Authority/recovery 保留预算前进；压力解除后恢复；不可观测域显示 UNOBSERVED；无 OOM、无限资源增长、进程/驱动崩溃或用户数据损坏 |
| X47 | 非清除 CriticalOperation 依次出现“仅调用”“局部效果成立”“全部 CompletionContract 条件满足”“完成证据随后失效” | 仅调用不产生 APPLIED；局部效果为 APPLIED + completionSatisfied=false；最终成功为 APPLIED + completionSatisfied=true；证据失效推进 revision 并撤销当前完成性；GUI 不把局部效果显示成最终完成，且不滥用 CLEARED/TERMINAL |
| X48 | Candidate 被证明无关后进入自动释放窗口；期间重复同一日志、轮换 sourceIdentity、出现一条新有效强证据、存在其他 restriction reason，并另行执行明确的操作者解除命令 | PROVEN_UNRELATED 立即记录且与 restrictionReleaseState 分离；重复/轮换不无限刷新窗口；新有效证据以新 revision 取消 pending release；其他原因继续有效；无新原因时硬截止释放；显式 canonical Authority command 不被防抖偷偷延迟并提示外部重建源可能恢复 |

X45 在任何实际提交资格 transition 的阶段都是前置门；7.16 只需覆盖其 cutover 已有命令范围。X46–X48 的完整生产验证从 7.17 推进至 L6E。四项测试均不得用 mock 状态、仅日志输出或强制 JVM 崩溃冒充通过。

### 19.5 V2.5 Runtime Surface 与逐能力矩阵验收

| 编号 | 场景 | 必须证明的结果 |
|---|---|---|
| X49 | 为八类 Surface 分别伪造本地“完成状态”，并尝试由 adapter/cache/projection 直接改 Judgment、TargetEpoch、Admission 或 durable receipt；同时让两个 Surface 对同一目标提交冲突修复 | 只有 Internal Root/Store 能改变 Authority Truth；Surface 写入被拒绝并产生诊断；冲突由同一 CrossSurfaceOperationPlan 仲裁，无第二 writer、无最后 callback 赢、无重复 canonical command |
| X50 | TERMINAL 目标先由 Temporal task 重建，再经 Registry 注册、Data 写回、Network 同步、Presentation 显示；在每一步插入重放、重复回调和顺序变化 | 同一 plan/command/epoch 下各 step 幂等且按依赖闭合；旧 epoch 在任一表面都不能恢复资格；required surface 有缺口时保持 PARTIAL/FENCED，全部完成与稳定重验后才 CLEARED |
| X51 | 对全部五十四项 key 分别选择 Compatible、Enhanced、Extreme 和 Custom；再模拟 ceiling 缺失、PENDING_RESTART、矩阵升版与旧配置迁移 | ProfilePresetMatrix 逐键生成可复现 requestedLevel 和带 revision 的解析 receipt；三档命中各自真实策略；effective 不越过 supported/evidence ceiling；旧配置迁移确定且 GUI 不虚报 ACTIVE |
| X52 | 在单机、逻辑服务端、专用服务端和独立客户端分别加载；客户端篡改 HUD/input/cache、重放旧 packet，服务端缺少所有渲染类 | 专用服务端不加载客户端实现；客户端只能发送认证请求或显示 accepted projection；旧 nonce/epoch/revision 被拒绝；客户端成功不能代替 Root receipt，服务端 Truth 不受本地显示篡改影响 |
| X53 | Candidate 被合法证明无关、目标被明确解除裁决并使用新 epoch 合法重激活；同时保留旧 task、event、packet、save 和 Presentation cache | 明确 Authority command 一次提交新资格；Surface restriction 按同一 revision 释放/重建；旧 epoch 的全部表面对象仍非法，合法新 epoch 不被旧限制误伤，状态无 ON/OFF 抖动 |
| X54 | Data repair 触发 Event、Event 安排 Temporal task、Temporal 更新 Network、Network 刷新 Presentation；再让 repair 结果本身被 observer 重复观察 | causality token 抑制同 plan 的预期反馈；真正新漂移使用新 outcome revision；无循环修复、无限任务、重复非幂等写入或结果振荡；Loop Guard 与 Host Safety 保持开启 |
| X55 | EXTREME 下同时施压八类 Surface 的事件、类、任务、包、缓存、HUD、registry 和 native/render resource，并尝试通过配置关闭安全限制 | Surface 覆盖不因压力从逻辑域删除，执行按预算分片；Host Safety、Loop Guard、single-writer 与硬预算不可关闭；Authority/recovery 保留预算前进；无 OOM、无限线程/类/句柄/缓冲或宿主崩溃 |
| X56 | 逐一移除某 Surface 的 observer、executor、repair、recovery 或 completion proof，并让其他 Surface 完全成功 | maturity 准确降到 S0–S5；运行时报告 UNOBSERVED/UNAVAILABLE/PARTIAL/FENCED 等真实状态；一个表面成功不掩盖 required surface 缺口，不从 Agent/JVMTI/Native 已加载推导 FULL |

X49、X51、X52 是 7.17 广度基础前置门；X50、X53、X54 在 L6 Root 编排形成后完成；X55、X56 随各 Surface executor 推进并在 L6E exact-artifact、真实 Forge/专用服务端和故障恢复环境中收口。任何测试不得用仅构造枚举、mock receipt 或日志字符串替代真实 producer、gate、repair、重启与结果观察。

---

## 20. 性能与资源约束

### 20.1 原则

Extreme 可以更重，但：

```text
更高预算 ≠ 无预算
```

采用：

- event-driven；
- incremental graph；
- bounded worker；
- per-source quota；
- backlog；
- quarantine；
- dead-letter；
- age/timeout；
- repair rate limit；
- class/JIT/native evidence rate limit。

V2.3/V2.4/V2.5 要求预算覆盖整条记录链、八类 Surface adapter/plan/receipt 和 §12.2 的全部宿主资源域：raw event、字符串与堆栈载荷、候选条目、规范 key、代表样本、汇总、索引、重试游标、surface cache、plan step、生成类、native/JVMTI allocation、direct/mapped buffer、线程、句柄与可观测渲染资源。不能只限制工作器数量却留下无限 map、无限来源队列、无限摘要历史、无限 Surface plan 或 JVM 非堆泄漏。具体容量、字节预算和速率进入版本化政策与运行时报告，不作为敌对 Mod 数量上限。

### 20.2 最关键规则

> **预算只决定“什么时候处理”，不决定“它是不是攻击面”。**

对应执行要求是：重复事件按语义合并，不同 key 超限时转成有界范围汇总；容量不足降低记录精度，不能降低既有结果约束或伪造已解决。调度优先级服从真实影响、公平分片与探索预算，不能被攻击事件数量单独决定。

### 20.3 诊断

至少报告：

```text
strength_profile
candidate_edge_count
proven_edge_count
unresolved_obligations
mutation_trace_count
target_fence_count
operation_fence_count
plane_fence_count
root_fence_state
worker_backlog
oldest_obligation_age
host_safety_throttle
retransform_rate
repair_rate
memory_budget_state
host_resource_domain_states[]
owned_resource_usage / owned_resource_peak / owned_allocation_rate
host_pressure_state / unobserved_resource_domains[]
direct_mapped_buffer_bytes / metaspace_bytes / code_cache_state
thread_count / generated_class_count / classloader_count
native_jvmti_owned_bytes / open_handle_count
gpu_render_owned_bytes（可观测时）
candidate_admission_reason / evidence_dedup_count
independent_weak_family_count
obligation_detail_bytes / obligation_summary_bytes
obligation_coalesced_count / aggregate_count_saturated
unresolved_coverage_gap / obligation_coverage_revision
restriction_handoff_failure
attack_outcome / outcome_revision / certificate_current_valid
execution_state / fence_scope
source_throttle / useful_evidence_age
exploration_budget_used / pending_scope_last_serviced
profile_matrix_revision / requested_effective_key_diff
runtime_surface_states[] / surface_maturity_levels[]
required_surface_gaps[] / unobserved_surfaces[]
cross_surface_plan_count / active_plan_steps / oldest_plan_age
plan_retry_count / idempotent_replay_count / causality_suppressed_count
surface_conflict_count / root_arbitration_count
client_projection_revision / authenticated_input_receipt_state
```

诊断指标的标签、来源维度和历史缓存也必须有界，不能为每个新 task/class/sourceIdentity 永久创建一组指标。指标用于解释真实状态，不参与 Authority 投票，也不能代替验收。

### 20.4 全资源域安全验收

Host Safety 不能只靠 `Runtime.freeMemory()`、一次 heap snapshot、捕获 `OutOfMemoryError` 或主动 `System.gc()` 证明。每个启用的 Extreme executor 必须声明它可能消耗的 `HostResourceDomain`、归属计数器、软/硬预算、释放路径和不可观测项。

验收至少覆盖：

1. 分别对 heap、direct/mapped buffer、metaspace、code cache、线程/栈、class/loader、native/JVMTI allocation、文件/平台句柄和可观测 GPU/render buffer 施压；
2. 在软预算前正常运行，在硬预算前发生可观察的节流、合并、分片或替代执行，不以 OOM、进程终止或驱动崩溃作为控制机制；
3. 重复加载、retransform、生成 hidden class、attach/detach、native buffer 与渲染资源创建/销毁后，owned usage 能回落或明确进入有界 quarantine/dead-letter；
4. 敌方制造 host-wide 压力时，Ronova 不把压力自动归因成敌对证明，也不因压力删除已有攻击域和 obligation；
5. Authority、durable commit/recovery、必要 gate 与最小 repair/reverify 的保留预算在压力下实际取得进展；
6. 压力解除后 governor 能恢复，不永久卡在 throttle，不用重启掩盖资源泄漏；
7. 不可观测资源域真实显示 `UNOBSERVED`，相应 executor 的 supportedCeiling/effectiveLevel 不得伪装成已具备完整安全证明；
8. 组合压力和长时间 soak 下无无限线程、无限类/loader、无限句柄、无限 native/direct allocation、用户文件/存档损坏或 JVM/宿主崩溃。

资源安全测试证明的是“Ronova 不用摧毁宿主来赢”，不是保证敌方代码无法自行崩溃进程，也不是降低 Extreme 对当前 JVM 内结果路径的逻辑覆盖。

---

## 21. 与典型极限代码对抗能力对照

| 对抗能力 | Ronova V2.5 设计对应 |
|---|---|
| 超高伤害 / 改血 | State / source convergence |
| remove / discard | Existence Termination |
| 列表清 | Entity records + WriteSink + TargetEpoch |
| 重生黑名单 | lineage + TargetEpoch + Admission |
| 全返回 | Completion Authority |
| 反全返回 | bytes + dispatch + completion + state |
| 运行时热补丁 | runtime patch defense / disablement |
| hidden/generated | Candidate/ResultGraph + producer/dispatch/outlet |
| manager/task 复活 | recovery/task/callback attack |
| 代码自恢复 | sealed baseline + repair/reverify |
| 纯渲染/非实体 | non-entity + Presence/result outlet |
| 修改来源追踪 | MutationTrace |
| 多个同级 Mod 同时对抗 | 无数量硬上限，按预算分片 |
| 高压资源对抗 | Host Safety Governor，节流但不缩小攻击域 |
| 假线索/重复日志诱导 | Candidate Admission Rule、证据独立性与临时处置权限 |
| 重复事件/新来源洪泛 | 规范义务归并 + 不同 key 溢出范围汇总 + 连续结果限制 |
| 隔离被误当成功 | Execution / AttackOutcome 分离，当前完成证明独立重验 |
| 高优先噪声抢占预算 | 真实影响优先、压力限流、来源配额、公平与探索预算 |
| 状态/存档/缓存恢复 | Data Surface + state/save/recovery source + rollback |
| 包重放/镜像重建 | Network Surface + identity/nonce/epoch/revision + receipt |
| HUD/渲染假存在 | Presentation Surface + non-entity/final result takeover |
| 吞按键/替换请求 | Input Surface + anti-interception + authenticated ingress |
| 事件/监听器恢复 | Event Surface + callback/source trace + 合法 listener 过滤 |
| 延迟任务/线程重建 | Temporal Surface + task/recovery chain + bounded loop guard |
| 注册器/工厂重插 | Registry Surface + registration/class lineage + Admission |
| 动态类/资源/native 模块 | Resource Surface + dynamic/patch/dispatch + HostResourceDomain |
| 多表面循环修复 | Root-approved CrossSurfaceOperationPlan + causality token + 幂等 step |

---

## 22. JVM 与宿主硬边界

### 22.1 同权限 Native/JVMTI

如果对方完全控制同地址空间，不能宣称数学绝对不可篡改。

Ronova 可以：

- 多来源 evidence；
- fence；
- repair；
- restart；
- durable truth recovery。

不能虚构物理绝对免疫。

### 22.2 JVM 死亡

当前 JVM 已 halt / native crash / OS kill：

> 原 JVM 不能继续自救。

只能依赖：

- 已提交 durable truth；
- clean restart；
- 新 JVM 重建 Root 与 evidence。

### 22.3 OS / 文件 / 无关进程

不属于 Extreme 攻击面。

### 22.4 极限代码对抗的正式边界

> **Ronova Extreme 可以不计敌对 Mod 的兼容性和功能存活代价，但不能不计 Minecraft/JVM 宿主、用户数据和操作系统的安全代价。**

---

## 23. 交付物

### 23.1 7.16

只要求：

- sole-writer cutover；
- StrengthPolicy 基础；
- 三档 + Custom；
- “极限代码对抗”UI 定位；
- V2.2 StrengthOptionKey；
- TargetEpoch / WriteSink / Completion 的必要 extension seam；
- 已有资格变更 command 的 Judgment/TargetEpoch/revoke/Admission/receipt 原子提交；
- supported/effective/evidence/activation；
- migration / receipt / pending restart；
- exact-artifact。

不要求：

- Full ResultGraph；
- MutationTrace production；
- Extreme 十五项 executor；
- ExistenceTerminationCertificate production；
- Full JIT/native closure。

V2.3 不追加候选准入、义务压缩、结果投影或反诱导调度的完整实现要求；这些属于后续交付，不影响 7.16 原有出口。

V2.4 不追加全资源域 governor、Completion 最终性 GUI、Candidate 自动释放防抖或全 TargetEpoch propagation 的生产实现；只要求 7.16 已经存在的资格变更不得产生分裂权威状态，并为后续字段保留版本化扩展缝。

V2.5 不追加八类 Runtime Surface production adapter、逐键 ProfilePresetMatrix 或 CrossSurfaceOperationPlan 的 7.16 交付义务。r52 只需保持五十四项 key、扩展缝、配置持久化和当前 plane mapping 的真实状态；不得把占位接口标成 FULL。

### 23.2 7.17

交付：

- double baseline；
- semantic / loader proof；
- Final Guard；
- runtime compiler defense；
- Candidate/ResultGraph base；
- MutationTrace base；
- fence scope；
- TargetEpoch propagation；
- dynamic dispatch；
- WriteSink evidence；
- ExecutionLease；
- host-safety governor；
- anti-AllReturn base；
- Compatible / Enhanced full candidate。

V2.3 追加的本阶段交付：

- Candidate Admission Rule 与强/弱证据独立性、合法授权过滤、候选处置权限；
- ObligationKey、重复归并、不同 key 溢出汇总、限制连续接力与明确解除条件；
- executionState / attackOutcome / fenceScope 的独立报告与 GUI 投影；
- 来源压力限流、重复贡献封顶、公平分片、探索预算和有界诊断；
- X31–X44 的基础合同与适用运行时用例证据。

V2.4 追加的本阶段交付：

- QualificationTransitionCommand 原子 durable commit、receipt、projection publish 与 failpoint recovery；
- HostResourceDomain inventory、owned/host 分离、软硬预算、应急保留预算与 UNOBSERVED 报告；
- CompletionReceipt 的 `completionSatisfied`、证据失效重评与 GUI 最终性；
- Candidate `restrictionReleaseState`、释放硬截止、重验证、取消和显式命令分流；
- X45–X48 的基础层与当前运行时可用资源域证据。

V2.5 追加的本阶段交付：

- versioned ProfilePresetMatrix、逐键解析/迁移/回执和真实 effective ceiling；
- 八类 Runtime Surface registry、typed adapter contract 与 S0–S6 maturity report；
- Data/Temporal/Registry/Network 基础结果链；
- Presentation/Input 的 server-truth、authenticated ingress 与只读 projection 边界；
- Event/Resource 的 provenance、producer/loader 绑定与有界证据；
- CrossSurfaceOperationPlan、幂等 step、causality token、冲突仲裁和基础恢复；
- X49–X56 的 7.17 适用层与真实单机/专用服务端证据。

### 23.3 L6/L6E

最终交付：

- InternalDominionRoot sole logical writer；
- fifteen attack policies production；
- three AllReturn capabilities；
- Candidate → Proven incremental closure；
- Existence Termination；
- legal TargetEpoch reactivation；
- source/chain/dynamic/dispatch/native closure；
- scoped fence；
- repair/reverify/restart；
- Extreme Code Duel evidence report。

L6E 完成 V2.3 四项合同、V2.4 收口合同和 V2.5 八类 Runtime Surface 在任务/回调、状态/重建、网络/显示、动态代码/Native、跨 epoch、资源压力与重启场景中的生产覆盖，并提交 X31–X56 的相应成品证据。所有 required surface 达到合同成熟度、跨表面计划闭合，才能宣称对应 Extreme 能力 FULL；基础合同通过不能代替最终 Existence Termination 与四层反全返回验收。

---

## 24. 最终完成判定

只有以下全部成立，才能宣布 Ronova **Extreme Code Duel / 极限代码对抗**完成：

1. Compatible / Enhanced / Extreme 使用真实不同 production policy；
2. EXTREME 不设置敌对 Mod 数量硬上限；
3. Candidate Attack Surface 采用 default-in；
4. 归属证明只决定处置强度，不把未决路径从范围删除；
5. 已证明结果链不因第三方兼容性降低 Extreme 强度；
6. 未决路径在预算耗尽后仍保留 obligation；
7. 极限清除满足 Existence Termination 全部完成条件；
8. 三种全返回分别完成；
9. anti-AllReturn 四层联合验证完成；
10. Dynamic / hidden / proxy / lambda / runtime patch / dispatch / native callback 均进入相应证据与结果控制；
11. TargetEpoch 既能终结撤销旧资格，也能在合法重新激活时推进新资格代际；
12. 旧 epoch 的实体、任务、包、回调、save、Presence 永不因新激活重新合法；
13. Final Write Sink 只对真实可仲裁范围宣称 gate；无法证明覆盖就撤销完成证明；
14. FENCE 按 TARGET → OPERATION → PLANE → ROOT/REALM 最小化升级；
15. 单个未知目标路径不能自动把整个 Root 自己锁死；
16. ResultGraph / CandidateGraph / MutationTrace / Certificate 从未成为第二 Truth；
17. Root / Store single-writer 不变量保持；
18. 7.16 没有因 V2.2/V2.3/V2.4/V2.5 被强行返工成完整 JVM Extreme；
19. Attack source trace 能为状态/恢复/执行异常提供真实 provenance evidence；
20. Extreme 不通过无限线程、无限扫描、无限 retransform、heap/direct/native/metaspace/code cache/线程栈/句柄/GPU 资源耗尽或 JVM/驱动崩溃取得强度；
21. Extreme 不删除/破坏第三方文件、用户文件、世界存档，不操作 OS/无关进程；
22. Host Safety Governor 能在高压对抗下节流且不缩小攻击域；
23. 多个同级结果链并存时按资源预算分片，而不是按 Mod 数量截断；
24. 当前 JVM 不可恢复时可从 durable truth 干净重启；
25. GUI 不显示虚假 ACTIVE，不静默降级；
26. 同权限 Native/JVMTI 和 JVM 死亡边界明确；
27. Authority 始终在当前 Minecraft JVM 内，Agent/JVMTI/Native/repair kernel 从未成为第二 writer；
28. Candidate 准入满足有效强证据或独立弱证据规则，合法合规操作不因触达 protected sink 自伤；
29. Candidate/Unresolved 仅获得临时结果限制与追踪权限，高分或高频不自动授权高副作用改写；
30. 重复和不同 key 洪泛下，细节、汇总、索引、样本与诊断总资源有界，未解决范围被持续承接；
31. 细节压缩与限制交接无放行窗口，LRU/TTL/计数/摘要不独立解除义务或产生 CLEARED；
32. Execution、AttackOutcome、FenceScope 与 Authority commit 分离，APPLIED 有效果证据，FENCED 不冒充攻击成功；
33. 当前完成证明失效时重新评估结果，历史成功不被当成当前 CLEARED；
34. sourcePressure 不能换取无界优先级，轮换身份不能重置全部配额，核心恢复、公平处理与探索预算实际生效；
35. 四项执行合同在 7.17/L6E 的相应生产路径和 X31–X44 验收中成立。
36. 所有资格代际 transition 将 Judgment、TargetEpoch、旧代际撤销、Admission revision 与 receipt 作为同一 canonical Root durable transaction 提交；
37. 资格事务在并发、崩溃和 COMMIT_UNKNOWN 下整体可见或整体不可见，以原 command id 幂等恢复且不存在旧 epoch 抢占窗口；
38. 非清除 CriticalOperation 只有 APPLIED + 有效 `completionSatisfied=true` 才显示最终完成，完成证据失效后当前状态可重评；
39. Candidate 的 PROVEN_UNRELATED 与临时限制释放状态分离，自动释放有硬截止、不可被噪声无限刷新，且不延迟明确的操作者 Authority command；
40. Host Safety 覆盖 heap、direct/mapped、metaspace、code cache、线程/栈、class/loader、JNI/JNA/JVMTI、句柄和可观测 GPU/render 资源；
41. Ronova-owned usage 与 whole-host pressure 分离，敌方压力不成为归属证明，也不删除攻击面、Judgment 或 obligation；
42. 不可观测资源域真实显示 UNOBSERVED，软硬预算、应急保留预算、释放进度和恢复状态可验证；
43. V2.4 收口合同在对应生产路径和 X45–X48 验收中成立。
44. 三个正式预设由 versioned ProfilePresetMatrix 对五十四项稳定 key 逐项解析，Custom、迁移、ceiling 与回执可复现；
45. Data、Network、Presentation、Input、Event、Temporal、Registry、Resource 全部映射到既有 Attack/Interception/Protection，不产生第四平面或新攻击按钮；
46. 八类 Surface 都按 Observe → Attribute → Verify → Policy → Enforce → Repair → Recover → Completion 提供 typed 生产链，并真实报告 S0–S6；
47. Internal Root/Store 始终是唯一 Authority writer，Surface cache、adapter、plan、客户端投影和 completion evidence 从未直接改变 Judgment、TargetEpoch 或 Admission；
48. Presentation/Input 保持客户端投影/请求入口语义，专用服务端类加载边界和服务端 Truth 成立，旧 packet/cache/projection 不跨 epoch 复活；
49. CrossSurfaceOperationPlan 由 Root 唯一批准，step 幂等、revision/lease/causality 完整，无循环修复、重复 command 或最后 callback 赢；
50. 任一 required surface 缺少 observer、executor、repair、recovery 或 completion proof 时不显示 FULL/CLEARED，其他表面成功不能掩盖缺口；
51. V2.5 广度合同在对应生产路径和 X49–X56 的 exact-artifact、单机、专用服务端、故障恢复与压力验收中成立。

最终定位：

> **COMPATIBLE 是普通整合包兼容档；ENHANCED 是高强度但仍尽量精确的强化档；EXTREME 是 Minecraft / Forge / 当前 JVM 内的极限代码对抗档。三档由逐能力矩阵真实选择生产政策。Extreme 不限制敌对 Mod 数量，不以第三方兼容性作为强度上限；任何可能改变当前 Judgment 最终结果的同 JVM 路径先进入候选攻击面，再由证据决定处置级别。Data、Network、Presentation、Input、Event、Temporal、Registry、Resource 八类表面由同一 Root 跨表面编排；资格代际以原子 Root transaction 提交，预算只节流、不缩域，FENCE 只按最小必要范围升级。唯一不可牺牲的是 Ronova 的 Authority 正确性、JVM/宿主全资源域稳定、用户数据安全和正常 durable recovery。**

V2.3 将这一定位落实为四条执行要求：候选按可检查的证据准入且权限受限；义务可压缩汇总但不能凭容量不足消失；隔离状态不代表攻击成功；对方制造的工作量不能单独决定 Ronova 的预算分配。V2.4 再冻结资格代际原子提交、JVM 全资源域安全、非清除完成最终性和 Candidate 自动释放防抖。V2.5 把新的运行广度拉到同一深度，加入逐能力三档矩阵、八表面 S0–S6 闭环、单 Root 跨表面计划和客户端/服务端边界。完整实现与验收从 7.17 推进到 L6E，7.16 Scope Lock 保持不变；在 X49–X56、矩阵迁移和阶段口径通过冻结评审前，本文件保持 Design Freeze Candidate。
