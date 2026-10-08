# Ronova 7.17 极限档强度实施规划 V1.0 — Internal Root

更新：2026-09-11。状态：**规划，尚未开始实施。全文分两部。**

| 部 | 范围 | 性质 |
|---|---|---|
| **第一部 P0–P4** | 逐键矩阵、天花板管道、逐键举证、表面成熟度、路径合并 | **兑现**：让极限档等于规范已写明的样子 |
| **第二部 Q0–Q5** | 候选一等公民 → MutationTrace → ResultGraph → Certificate → 证明主线 → 深度解锁 | **加强**：把 V2.6 §4.1 定义的 EXTREME 范围真正建起来 |

> **修订说明**
> - 初版（同日）把首要工作定为"建天花板管道 + 自拟深度语义"。经复核 V2.6 §6.6，
>   **该假设被推翻**——三档语义由规范逐键定义，且"Extreme 全部 STRICT_ALL"被 V2.6:776
>   明文认定为"不是最终矩阵"。已重构为以 **G0 逐键矩阵**为首要目标。
> - 第二版补入 **第二部**：依据 V2.6:90"完整 ResultGraph、MutationTrace、
>   ExistenceTerminationCertificate 生产 schema 与 executor **从 7.17 起实现**"，
>   该主线为 7.17 已排定、代码为零的正题。第一部是它的前置，不是它的替代。

本文属[规范顺序](README.md)第 5 层（与 7.17 通用恢复链处置规划同级），
**不增加第二套 Root、身份、动作开关或验收编号**，不修改任何高位冻结决策。
与 [L6 Architecture Master V2.4](Ronova_L6_Architecture_Master_Plan_V2.4_Internal_Root.md)、
[L6E-0 契约冻结 V1.3](Ronova_L6E-0_Contract_Freeze_V1.3_Internal_Root.md)、
[7.16+ 强度总纲 V2.6](Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md)
冲突时，以高位文档为准，并把冲突登记为设计缺口，不按"版本号更新"覆盖。

---

## 0. 本文定位

**要解决的问题：极限档（EXTREME）的声明强度与运行期实际强度之间存在系统性落差，
而落差的根因不是"天花板太低"，是"请求侧根本没有携带规范的逐键语义"。**

本文只谈**如何让极限档真正变强**，不谈如何把它的**声明数字**写得更高。

### 0.1 术语约定

| 术语 | 含义 | 载体 |
|---|---|---|
| **请求等级** `requested` | 配置请求的深度 | `ProfilePresetMatrix` |
| **支持天花板** `supportedCeiling` | 本构建**能证明自己做到**的深度 | `StrengthPolicySnapshot.supportedCeilings` |
| **证据天花板** `evidenceCeiling` | 当前**证据链有效**的深度，恒 ≤ 支持天花板 | `StrengthPolicySnapshot.evidenceCeilings` |
| **有效深度** `effective` | `min(requested, supportedCeiling, evidenceCeiling)`，**运行期真正使用的值** | `StrengthPolicySnapshot.java:230-233` |
| **逐键矩阵** | 54 键 × 3 预设各自的**语义声明**（非统一枚举值） | V2.6 §6.6，本文 G0 |
| **举证**（attestation） | 由**实际运行观察**产生的、可复核的能力证据 | 本文 P1 新增 |

---

## 1. 问题陈述（实测，全部带行号）

### 1.1 配置层看似拉满

- EXTREME 预设：54 个键全部 `STRICT_ALL`（`ProfilePresetMatrix.java:147-148`）；
  14 个动作全部 `EXTREME` → `depth() = STRICT_ALL`（`AttackActionLevel.java:12`）。
- 运行期：`effective = lowerOf(lowerOf(requested, supportedCeiling), evidenceCeiling)`
  （`StrengthPolicySnapshot.java:230-233`）；`lowerOf` 即按 rank 取小（`MechanismLevel.java:28-32`）。
- 等级 rank：`OFF=0 / MONITOR=1 / PROTECT_RONOVA=2 / RELATED_PATHS=3 / STRICT_ALL=4`
  （`MechanismLevel.java:17-26`）。

### 1.2 天花板默认 OFF，且只有 6 个键会被抬升

- `supportedCeilings` 默认 `OFF`（`StrengthPolicySnapshot.java:99`）；
  快照由 `fromConfigurations` 构造，天花板传 `Map.of()`（`ShadowDurableRootStore.java:344`、`:137`）。
- `withCapability` 全项目**只有 2 个调用者**，且**只传 `PROTECT_RONOVA` 或 `OFF`**：
  - `RuntimeCapabilityProjection.java:11-17` → 3 键（`PROTECT_CODE_INTEGRITY`、
    `RUNTIME_CRITICAL_CLASS_WATCH`、`INTERCEPT_INSTRUMENTATION`）
  - `RuntimeCapabilityProjection.java:18-23` → 3 键（`RUNTIME_JVMTI`、`RUNTIME_NATIVE`、
    `RUNTIME_BACKGROUND_INTEGRITY`）
  - `TerminalActionPolicy.java:40` → 5 键，**调用内作用域、不发布**
- **没有任何代码路径把天花板设为 `RELATED_PATHS` 或 `STRICT_ALL`。**

### 1.3 后果 A：48 个键恒不可用

`supportedCeiling == OFF` ⇒ `activationState = UNAVAILABLE`（`:383-387`）；
`activationState != ACTIVE` ⇒ `resolveAttack` 直接拒绝（`:205-208`）。

| 平面 | 已有天花板的键 / 总数 |
|---|---|
| attack | **0 / 15** |
| intercept | 1 / 10 |
| protect | 1 / 17 |
| runtime | 4 / 12 |
| **合计** | **6 / 54** |

**这不是"能力弱"，是"新执行器一上线就会被拒绝"。** 天花板同时是一道封住扩张的闸门。

### 1.4 后果 B：等级高低在运行期只有 2 处消费点

把 54 键 + 14 动作的全部消费点扫完，只有两处按**等级大小**分支，其余全部是二值阈值
`rank() >= PROTECT_RONOVA.rank()`：

| 位置 | 语义 |
|---|---|
| `runtime/StateFeedbackSchedule.java:23` | `horizon = STRICT_ALL ? 0 : RELATED_PATHS ? 100 : 20`；`0` = 持续反馈永不结束 |
| `policy/RecoveryChainPolicy.java` `Step.minimum` | 8 个步骤中 5 个要求 `RELATED_PATHS`，**3 个要求 `STRICT_ALL`** |

### 1.5 后果 C：3 个"极限档才该解锁"的步骤在 `resolve()` 路径恒不可执行

```java
boolean executes = resolution.permitted()
    && resolution.effectiveDepth().rank() >= step.minimum().rank();   // RecoveryChainPolicy.java:55
```

`effectiveDepth ≤ PROTECT_RONOVA(2) < STRICT_ALL(4)` ⇒ **`executes` 恒为 `false`**，
reason 恒为 `RECOVERY_DEPTH_LIMITED_BY_DEPENDENCY`。
涉及：`DERIVED_SOURCE`、`DESCENDANT_TASK`、`RELOAD_RECONSTRUCTION`。

### 1.6 后果 D：两套权限路径并存

| 路径 | 判据 | 结果 |
|---|---|---|
| `RecoveryChainPolicy.resolve(step)` / `StrengthPolicySnapshot.resolveAttack` | 证据天花板 `effective` | 上述 3 步骤恒不可执行 |
| `RecoveryChainPolicy.registered*()` | **预设 `requestedLevel()`，不看天花板** | 只要 `!pendingRestart && bindingVerified` 即放行 |

`registered*` 是生产恢复链实际走的路径（`ShadowDurableRootStore` 5 处、`EffectJournal` 3 处、
`RecoverySourceTracker` 1 处）。⇒ **"强度必须由证据决定"对生产恢复链不成立。**

同类第三门：`StrengthPolicyConfiguration.permitsBaselineAction`（`:85-98`）
对非 `TERMINALIZE` 直接 `return false`，对 `TERMINALIZE` 只看预设 `requestedLevel`。

### 1.7 后果 E：表面成熟度框架被写死在下两档

`SurfaceRegistry.Maturity` 定义了 7 级（`S0_ABSENT → S6_COMPLETE`，`SurfaceRegistry.java:7`），
但实现只能产出 S0/S1：

```java
rows.add(new Report(surface, registered ? Maturity.S1_OBSERVE : Maturity.S0_ABSENT,
                            observed   ? Maturity.S1_OBSERVE : Maturity.S0_ABSENT, ...));   // :47-48
```

`S2_ATTRIBUTE` 至 `S6_COMPLETE` **声明了、从未产生过**。
且 `RuntimeDiagnostics` 只注册 6 个表面采样器，缺 `PRESENTATION` 与 `INPUT`。

### 1.8 后果 F（**首要发现**）：极限档从未按规范实现——请求侧被压成了统一映射

V2.6 §6.6 标题为"V2.5 **逐能力** ProfilePresetMatrix"，下含五张逐键 × 三档语义表：

| 小节 | 内容 |
|---|---|
| §6.6.1 | 玩家可见攻击模块（14 动作 × 三档） |
| §6.6.2 | 十五项高级攻击政策（15 键 × 三档） |
| §6.6.3 | 拦截机制矩阵（10 键 × 三档） |
| §6.6.4 | 穿透后防护矩阵（17 键 × 三档） |
| §6.6.5 | 运行时支撑矩阵（12 键 × 三档） |
| §6.6.6 | 矩阵正确性要求 |

而 V2.6:776 明文规定：

> `CUSTOM` 直接使用玩家逐项选择；三个正式预设**必须按下表逐键解析**。
> r52 实现的"Compatible 对运行时统一 MONITOR、其他平面统一 PROTECT_RONOVA；
> **Enhanced 全部 RELATED_PATHS；Extreme 全部 STRICT_ALL**"
> **仅是 7.16 policy foundation，不是 V2.5 最终矩阵**。
> 矩阵进入生产代码时必须有独立 revision、迁移规则和 exact-artifact 测试。

V2.6:2767 的演进口径表记录当前状态为"**仅稳定 key/seam；r52 统一 plane 映射保持事实口径**"，
目标列才是"逐键矩阵、revision、迁移、解析 receipt"。
V2.6:2885 把"以 V2.5 为理由把 r52 的占位接口、**统一 mapping** 或 source-level 编译通过
写成 exact-artifact 已完成"列为**禁止项**。

**而当前代码实现的正是那个统一映射**：

```java
profiles.put(StrengthProfile.ENHANCED, immutable(filled(MechanismLevel.RELATED_PATHS)));  // ProfilePresetMatrix.java:145-146
profiles.put(StrengthProfile.EXTREME,  immutable(filled(MechanismLevel.STRICT_ALL)));      // ProfilePresetMatrix.java:147-148
profiles.put(StrengthProfile.CUSTOM,   immutable(filled(MechanismLevel.MONITOR)));         // :149-150
```

`filled(...)` 把**同一个枚举值**铺满 54 个键。⇒ **"极限档"在实现上是一个统一枚举值，
而不是 V2.6 §6.6 定义的那套逐键机制。**

#### 规范定义的"极限"是什么

§6.6 三档的递进关系是 **直接 → 已证明相关 → 候选全部**，
且极限档**额外承担两项义务**。逐键抽样（原文摘要）：

| 键 | 极限档语义 |
|---|---|
| `ATTACK_NON_ENTITY` | 所有**候选**非实体结果**先限制资格**，证明后执行完整清除/失能/恢复闭环 |
| `ATTACK_TASK_CHAIN` | 约束**候选**线程、executor、timer、future 和重建任务并**验证最终结果** |
| `ATTACK_REGISTRATION_SOURCE` | 覆盖候选注册、重注册、loader 与动态 producer，**旧 epoch 永不重新合法** |
| `INTERCEPT_MIXIN` | 对当前 Judgment **候选结果路径全部严格**；**无真实前置门不得伪报 DENIED** |
| `INTERCEPT_JVMTI` | 在能力可证范围内严格 gate/repair/reverify；**不可观测部分必须报告 UNOBSERVED** |
| `ATTACK_SOURCE_TRACE` | 候选跨表面 MutationTrace；**证据去重、容量汇总和独立性规则全部启用** |

⇒ 极限档不是"等级数字"，而是三件东西的合取：

1. **纳入范围**：候选（尚只被证明"相关"、未被证明"等价"）全部纳入；
2. **前置限制机制**：先施加**可逆**限制（hold），不是先裁决；
3. **报告义务**：不可观测的部分必须如实报告 `UNOBSERVED`，不得伪报 `DENIED`。

#### 该机制的零件已存在，只是未与档位语义连接

| 规范要求 | 已存在的实现 |
|---|---|
| "先限制资格"（可逆 hold，非裁决） | `obligation/CandidateRestrictions`——"Root 派生可逆准入 hold reason（**不是裁决**）+ reconcile + requestRelease" |
| "不可观测必须报告" | `ResolutionState` 的 unknown 轴、`EvidenceState.UNOBSERVED`、各处 `UNOBSERVED` reason |
| "证明后处置" | `RecoveryWorkDescriptor` + `RecoveryWorkFact` 体系、`RecoveryChainPolicy` 的 `registered*` 许可 |
| "旧 epoch 永不重新合法" | `TargetEpoch` + `RootCommandJournal.latestQualification` |

这些均已实现并接线，**但没有任何一处读取"当前键处于哪一档"**——
因为档位在实现上只是一个枚举值，不携带逐键语义。

### 1.9 结论

**极限档没有"到头"；它没有按规范实现。**

"极限档不强"由三个独立缺口叠加而成，缺一不可：

| # | 缺口 | 性质 |
|---|---|---|
| 1 | **请求侧**：逐键矩阵被压成统一映射（§1.8） | **合同缺口**（违反 V2.6 §6.6） |
| 2 | **支持侧**：只有 6/54 键有天花板（§1.2–1.3） | 测量缺口 |
| 3 | **执行侧**：只有 2 处按档位大小分支（§1.4） | 语义缺口 |

⇒ 只补 2 与 3 不够（初版方案即犯此错），**必须先补 1**：
一旦请求侧携带逐键语义，"执行侧该按什么区分档位"就不再需要发明，而是照 §6.6 逐键落实。

**架构不需要推翻**：Root 唯一 writer、逐键 key、天花板机制（V2.6:770-773 明文规定
`∩ supportedCeiling ∩ evidenceCeiling ∩ restart/activation state → effectiveLevel`）、
候选限制、证据体系，**都是规范要求且已就位的**。需要推翻的只是
**"极限档 = 全部 STRICT_ALL"这个实现假设**。

---

## 2. 目标与非目标

### 2.1 目标

| ID | 目标 | 完成判据 |
|---|---|---|
| **G0** | **实施 V2.6 §6.6 的逐键矩阵**（首要目标） | 54 键 + 14 动作在三个预设下各自携带**逐键语义声明**（纳入范围 / 前置限制 / 报告义务），并具备独立 revision、迁移规则与 exact-artifact 测试 |
| **G1** | 建立"能力测量 → 天花板"管道 | 天花板由注册的举证生产者提供，而非硬编码调用点；至少 6 个既有键经新管道产出**行为不变**的结果 |
| **G2** | 让已存在执行器的键可被举证 | 有天花板的键从 6 个提升，且每个 ACTIVE 都由**实际行为观察**支撑 |
| **G3** | 让每键执行按**其声明的档位语义**工作 | 逐键落实 §6.6 三档语义；3 个 `STRICT_ALL` 步骤因此获得合法天花板 |
| **G4** | 合并两套权限路径 | `registered*` 与 `resolve()` 使用同一判据，或在合同中显式写明分叉理由 |
| **G5** | 修正报告矛盾 | `/ronova strength status` 的"接线"列与"高级解析"列不再对同一行给出互相矛盾的信息 |

**顺序约束**：G0 是 G1–G3 的前提。

### 2.2 非目标（明确不做）

1. **不把"全部 STRICT_ALL"继续当作极限档的实现**——V2.6:776 认定它是
   "7.16 policy foundation，不是最终矩阵"，V2.6:2885 禁止把它写成已完成。
2. **不自行发明通用深度语义**（初版方案的 P3 即犯此错）——三档语义由 §6.6 逐键定义，
   实施者的职责是落实，不是重新设计。
3. **不降低任何档位的声明目标**：不把 3 个恢复链步骤的 `minimum` 从 `STRICT_ALL` 降到
   `RELATED_PATHS`——属"因难以实现而把极限目标降级"，规范明文禁止。
4. 不建立外部持续 Authority、Named Pipe、跨进程 Root 或第二份权威数据库。
5. 不新增第 55 个高级键、第 15 个普通动作或第 9 类运行表面。
6. 不声称任何验收项转 PASS；本文不修改 `validation/7.17/acceptance-matrix.json`。
7. 不把 `SurfaceRegistry.Maturity` 按"适配器已注册"直接给高分（见 §6.2）。
8. **不修改 `ProfilePresetMatrix` revision 1 的既有字节语义**——revision 2 以新增方式进入
   （见 P0），revision 1 保留可解析与可回滚。

---

## 3. 阶段划分

### P0 —— 矩阵 revision 2：把逐键语义实施出来（**合同缺口，最高优先**）

**入口条件**：无。可立即开始。
**为什么最先做**：V2.6:776 要求"三个正式预设必须按下表逐键解析"，
而当前 `ENHANCED` / `EXTREME` / `CUSTOM` 是 `filled(...)` 统一映射（§1.8）。
在此缺口闭合前，任何执行侧工作都缺少"该按什么区分档位"的依据。

**交付物**

| 产物 | 内容 |
|---|---|
| `ProfilePresetMatrix` revision 2 | 54 键 × 3 预设的**逐键声明**，按 V2.6 §6.6.2–6.6.5 四张表逐行落实；14 动作按 §6.6.1 |
| 逐键语义声明模型 | 每个 (键, 预设) 声明三件事：**纳入范围**（直接 / 已证明相关 / 候选全部）、**是否前置可逆限制**、**不可观测时的报告义务** |
| revision 与迁移 | 独立 revision 号；schema 升级先验证旧 policy/journal，再由 Root 提交完整新配置图并等待重启；旧 receipt 保留原 payload/hash 语义（照 V2.6:927 口径） |
| exact-artifact 测试 | 逐键生成可复现的 `requestedLevel` 与声明（对应 X51 口径） |
| 回滚路径 | revision 1 仍可解析；revision 2 可整体回退 |

**硬约束**

1. **逐键语义必须来自 §6.6 原文**，不得由实施者自拟；
2. `FOLLOW_PROFILE` 只引用所配置的矩阵 revision；改单项后转 Custom，
   其他项保持最近一次显式解析值，**禁止隐式漂移**（V2.6:879）；
3. 不得改动 r53 已有 schema 2 的字节或哈希（V2.6:927）；
4. 不得因"某一键难以实现"而把它在极限列的声明降为兼容列语义。

**验收**：矩阵逐键可复现；每键的极限档声明能在 §6.6 中找到对应原文；
revision 1 → 2 → 1 往返不丢语义。

**明确不做**：P0 不改变任何键的 `effective`，不提升天花板。

---

### P1 —— 天花板管道（**执行侧一切的前置**）

**入口条件**：P0 完成（矩阵已携带逐键语义，才知道该为什么举证）。

**为什么必需**：`withCapability` 目前只有 2 个硬编码调用点。
任何键在获得天花板之前，其执行器会被 `resolveAttack` 直接拒绝（§1.3）。
**不先建管道，P2 之后的所有工作都无法生效。**

**交付物**

| 产物 | 内容 |
|---|---|
| `policy/CapabilityAttestation.java` | 举证生产者接口：`StrengthOptionKey key()`、`Attestation observe(证据上下文)`；返回值含 `supportedCeiling / evidenceCeiling / EvidenceState / ActivationState / 证据引用` |
| `policy/AttestationRegistry.java` | 按键注册生产者；构造期冻结（不引入 `extension/` 的信封格式，避免把未接线组件带入生产路径） |
| `RuntimeCapabilityProjection` 改造 | 从"2 个硬编码 `withCapability` 块"改为"遍历注册生产者"；**先迁移既有 6 键，行为与现在完全一致** |
| 自测 | 迁移前后同一配置下 `resolve()` 的 12 项字段逐一致（回归基线） |

**验收**：6 个既有键迁移后 `resolve()` 输出**逐字段相同**；新增生产者只增不减；
无生产者注册的键仍为 `OFF`。

---

### P2 —— 逐键举证与执行接线（收益最大）

**入口条件**：P1 完成且回归一致。
**核心原则**：**不写新的强制代码**，只为已存在的执行器补"可复核的行为证据"，
并让其执行按 P0 声明的档位语义工作。

**证据合格线**（三条全满足才算）

1. 证据来自**本运行时实际发生的行为**，不是源码存在、接口返回或配置值；
2. 证据**可复核**：能在 `round-N.json` / 诊断输出中定位到具体计数或回执；
3. 证据**可失效**：相关代码 / loader / 版本变化后必须撤销。

**不合格证据（明确拒绝）**：类名存在、字段被赋值、接口返回 `true`、单次成功、
夹具预置数据、UI 显示为 ACTIVE——与项目既有红线一致。

**分批**（按"执行器完整度"排序）

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

#### P2-B 防护平面（当前 1 / 17）

| 键 | 已有执行器 | 建议的可观察证据 |
|---|---|---|
| `protect.life` / `protect.removal` | `AuthorityEnforcement.shouldProtectRemoval` + `LifecycleContinuityModule` | 受保护 Owner 的移除被实际拦下并完成一次 repair |
| `protect.death` | `LivingEntityProtectionMixin` 的 `die` 门 + `TerminalLifecycle` | 死亡事件被实际取消，且**未**重放外部回调 |
| `protect.entity_record` | `TopologyContinuityModule` | 实际修复过 UUID/tick/section 索引缺口 |
| `protect.identity_respawn` | `RegisteredOwnerIdentity` + `AuthorityBinder` | 跨重生/换维度后资格被实际恢复 |
| `protect.anti_interception` | `SemanticGateVerifier` | 与 `attack.runtime_patch_disablement` 共用实现但**必须分开计量** |
| `protect.anti_all_return` | `RawAccessorContract.repair` | 自检方法体实际被重写并复验 |
| `protect.call` | `PlanExecutionLease` + `OperationPlanRunner` | 旧上下文写入被实际拒绝 |
| `protect.input_communication` | `ClientActionController` 发件箱 | 同会话内重发与幂等回执实际生效 |
| `protect.state_recovery` | `TerminalStateSink` | 异常值被实际拒绝传播 |
| `protect.restart_recovery` | durable store 重建 | 重启后原意图被实际重新解析 |
| `protect.rollback` | Native 事务回滚（**不需要 Root 许可**） | 实际发生过一次 rollback 且原文件不变 |
| `protect.loop_guard` | `ClientRecoveryGuard` / `CapabilityRuntime` 重入闸 | 实际拦下过一次递归 |

#### P2-C 拦截平面（当前 1 / 10）

| 键 | 已有执行器 | 建议的可观察证据 |
|---|---|---|
| `intercept.mixin` | 31 个 mixin + `MixinMetadataSelfTest` | 实际注入点存在且语义门通过 |
| `intercept.asm` | `RonovaTransformer` + `SemanticGateVerifier` | 控制流证明通过（非仅常量池字符串） |
| `intercept.jvmti` | `NativeBridge` `CAP_JVMTI` + `ClassFileLoadHook` | 实际收到过 hook 回调且完成证明 |
| `intercept.unsafe_reflection` | `RawAccessorContract` + `RawAccessorDispatch` | 登记写入位置的对象/槽/版本被实际核对 |
| `intercept.classloader` | `g_root_loader` 作用域 + `ClassLineageResolver` | 实际区分过同名类/新副本 |
| `intercept.jni_native` | 语义租约（`CAP_SEMANTIC_LEASE`） | 租约实际当前（`required != 0 && verified == required && 未过期 && 连续失败 0`） |
| `intercept.modlauncher` | Native eager retransform | 实际完成过自有变换 |

`intercept.thread_task`、`intercept.all_return`、`intercept.instrumentation` 另行处理
（`instrumentation` 已由 `RuntimeCapabilityProjection` 覆盖）。

#### P2-D 运行时平面（当前 4 / 12，补 6）

`runtime.java_agent`、`runtime.native_loader`、`runtime.render_guard`、
`runtime.dynamic_code_watch`、`runtime.thread_continuity`、`runtime.anomalous_call_log`。

**P2 验收**：每个晋升 ACTIVE 的键必须在诊断输出中给出**证据引用**；
证据不足者**保持 `UNAVAILABLE`**，不得为凑数放行。
本阶段**不提升任何键高于 `PROTECT_RONOVA`**——高于该档的合法性由 P0 的矩阵声明
与后续范围实施决定，不在本阶段声明。

---

### P3 —— 接通表面成熟度

**入口条件**：P2 至少完成一批。

1. 让 8 个表面适配器实际产出 `S2_ATTRIBUTE` 及以上，而非恒 S0/S1；
2. 补 `PRESENTATION` 与 `INPUT` 两个采样器；
3. 建立**表面成熟度 → 能力键天花板**的显式映射表，作为 P2 之外的第二个举证来源。

**硬约束**：成熟度必须由**该表面实际完成的动作等级**决定，不得由"适配器已注册"决定。
`S6_COMPLETE` 只在既有 `CompletionTracker` 的真正完成判定成立时给出。
依据：Roadmap:250-264——"S1–S6 表示能力成熟度，不是版本号；
**只有相应 S6 成立，才可声明该表面对该操作 FULL**"。

---

### P4 —— 权限路径合并与报告修正

**入口条件**：P3 完成或明确中止。

1. `RecoveryChainPolicy.registered*()` 改用 `effectiveLevel()`，
   或在合同中显式写明"为何配置型许可与证据型许可分叉"，并让诊断分别展示两者；
2. `StrengthPolicyConfiguration.permitsBaselineAction` 的 `TERMINALIZE` 特例必须给出明文理由，
   不得以"非 TERMINALIZE 一律 false"隐式表达；
3. `/ronova strength status` 的"接线"列与"高级解析"列合并为**单一结论列**：
   每行只输出一个可判定状态（ACTIVE / FENCED / UNAVAILABLE / PENDING_RESTART 之一）+ 证据引用；
4. 三处静默降级（`ronova_root.dll` 加载失败、`armLateGuard` 反射失败、任务桥提取失败）
   必须在 `/ronova status` 中显式可见。

---

## 4. 阶段依赖与顺序

```
P0 逐键矩阵（合同缺口）
      │
      ▼
P1 天花板管道 ──► P2 逐键举证与执行接线（A/B/C/D 可并行）──► P3 表面成熟度
                                                                  │
                                                                  ▼
                                                          P4 路径合并与报告

G5 报告修正：可随时插入，建议最先（最便宜，直接关系诊断可信度）
```

**建议实际顺序**：`G5 修正 → P0 矩阵 → P1 管道 → P2-A → P2-B/C/D → P3 → P4`。

---

## 5. 验收与证据要求

1. 每个晋升 ACTIVE 的键，验收时必须给出：**观察到的行为**、**证据引用**、**失效条件**。
2. 不得新增 PASS 声明；不得修改 `acceptance-matrix.json` 的任何 `status`。
3. 不得把不同 JAR 哈希下的子范围结果拼成某一版本的完整通过。
4. 保留全部失败记录；证据不足时保持 `UNAVAILABLE` 并记录原因。
5. 矩阵 revision 2 进入生产必须同时具备：独立 revision、迁移规则、exact-artifact 测试
   （V2.6:776 明文要求）。
6. 实施期间按既有要求：做改动相关编译与针对性测试，主链接线后集中完整构建与阶段验收；
   代码、失败原因或范围未变，不重复运行相同检查。
7. 继续使用隔离的自有专用服与通用夹具；不运行原第三方样本，不替换原游戏与原存档。

---

## 6. 不采纳的实现方式（红线）

### 6.1 四类禁止的"增强"

| 禁止 | 为什么 |
|---|---|
| 把"全部 STRICT_ALL"当作极限档实现交付 | V2.6:776 认定其为 7.16 policy foundation；V2.6:2885 明文禁止写成已完成 |
| 自拟一套通用"深度语义"替代 §6.6 逐键定义 | 规范已逐键定义；自拟即制造第二套语义 |
| 给 `withCapability` 传高等级而不建立证据 | 即"接口返回即证据"，规范明文禁止 |
| 把 3 个步骤的 `minimum` 降到 `PROTECT_RONOVA` | 即"因难以实现而把极限目标降级"，规范明文禁止 |

### 6.2 特别提示：举证退化的两种形态

1. **自我报告式退化**：举证生产者直接读自己的配置或缓存，而不是观察行为。
   防线：证据必须能指向一次具体的运行时事件（计数、回执、被拦截对象）。
2. **笼统覆盖式退化**：一个生产者给一整片平面发放天花板。
   防线：举证**按键注册**，一个键一份证据；
   `attack.runtime_patch_disablement` 与 `protect.anti_interception` 共用实现但**分开计量**。

### 6.3 明确不在本文范围

- Native WriteSink / lease 强制下沉（Native 层架构决策，另议）
- `ronova_root.dll` 未纳入封印信任链（构建期信任链缺口，另议）
- 三层拦截的交叠精简（拦截层架构决策，另议）
- 12 个无执行器的普通动作的具体实现（属能力实现，P2 只负责举证与按档接线）

---

## 7. 完成判定

本文**不定义新的"极限档已达成"结论**。完成判定仅针对本文六个目标：

| 目标 | 完成判据 |
|---|---|
| G0 | 54 键 + 14 动作在三个预设下各有**可追溯至 §6.6 原文**的逐键声明；具备独立 revision、迁移规则、exact-artifact 测试；revision 1↔2 往返不丢语义 |
| G1 | 天花板全部由注册的举证生产者提供；无硬编码 `withCapability` 调用点 |
| G2 | 有天花板的键数量提升，且**每一个都附行为证据与失效条件**；证据不足者保持 UNAVAILABLE |
| G3 | 每键执行实际读取其档位声明；3 个 `STRICT_ALL` 步骤获得**合法**天花板而非被绕过 |
| G4 | 权限判据合一，或分叉有明文合同且诊断分别展示 |
| G5 | status 命令每行只输出一个可判定状态；三处静默降级在 status 中可见 |

**任何一项未达成，其余项不得据此宣布极限档已完成。**
本文不改变 `docs/CURRENT-PROGRESS.md` 的既有结论：完整 7.17、FULL L5 / L6 / L6E 仍未宣告完成。

---
---

# 第二部：加强路线（第一部 P0–P4 完成之后）

> 第一部是**兑现**：让极限档等于规范已经写明的样子。
> 第二部才是**加强**：把 §4.1 定义的 EXTREME 范围真正建起来。
> 依据：V2.6:90——"完整 ResultGraph、MutationTrace、ExistenceTerminationCertificate
> 生产 schema 与 executor **从 7.17 起实现**"。

## 8. 第一部完成时的状态与暴露出的新缺口

### 8.1 完成状态

| 第一部阶段 | 完成后 |
|---|---|
| P0 矩阵 revision 2 | 请求侧携带逐键语义 |
| P1 天花板管道 | 举证生产者注册表就位 |
| P2 逐键举证 | 有执行器的键在 `PROTECT_RONOVA` 深度 ACTIVE |
| P3 表面成熟度 | S0–S6 可产出 |
| P4 路径合并 | 判据统一、报告不矛盾 |

**结果：极限档成为一个诚实但"广而浅"的档位。** 它不再名不副实，
但**仍不等于 §4.1 定义的 EXTREME**。

### 8.2 暴露出的新缺口

P0 完成后，矩阵的极限列声明了"候选全部纳入 / 先限制资格 / 证明后处置"，
**但没有执行载体**——因为 `REGISTERED_ENTITY_ONLY` 是点，而候选路径是图。

三档范围的定义（V2.6:365-396）就是这条缺口的直接来源：

| 档 | 规范范围（原文） | 结构 |
|---|---|---|
| COMPATIBLE | Ronova 自身 + r51 已有稳定实体/生命周期/准入路径 | 自身 |
| ENHANCED | Ronova + 当前目标 + 当前 Judgment + **已经明确识别的直接结果相关路径** | 点 + 已知边 |
| **EXTREME** | 当前 Judgment → **所有可能改变其最终结果的同 JVM 候选路径** → 已证明源 / 链 / 代码 / dispatch / task / callback / rebuild / publish → 最终完成与状态出口 | **图** |

旁证：V2.6:404-411（§4.2）禁止把"最多 N 个**结果图节点**""最多 N 层逻辑链深度"
"最多 N 个 loader 副本"写成固定架构限制——**规范直接以"结果图节点"描述 EXTREME 的容量策略**。

### 8.3 第二部的六个阶段与单向依赖

```
Q0 候选一等公民 ──► Q1 MutationTrace ──► Q2 ResultGraph ──► Q3 Certificate
                                                    │
                                                    ▼
                                          Q4 证明主线（Lease / Manifest）
                                                    │
                                                    ▼
                                          Q5 深度解锁
```

**不可跳步的三条理由**

1. **候选必须先于图**：先写图会得到一张空图；
2. **图必须先于证书**：证书判据原文是"在某一证据 revision / 会话 / policy / **result graph** 下
   完成条件已满足"（V2.6:354）——没有图，证书无从判定；
3. **证书必须先于深度提升**：没有完成判据，"更深的范围"无法验收。

---

## 9. Q0 —— 候选一等公民

**入口**：第一部 P0 完成（矩阵已声明"候选全部"这一档，需要实体承载）。

**依据**：V2.6:1170——`MutationTrace`"**可提高 Candidate → PROVEN_RELATED 的置信度**"。
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

## 10. Q1 —— MutationTrace 生产实现

**入口**：Q0 完成。

**规范管线**（V2.6:1158-1164 原文）

```text
发现关键状态/结果变化
→ 记录 before / after
→ 关联 caller / task / callback / manager / dispatch / loader / native source
→ 生成有界 MutationTrace evidence
→ 将证据提供给 ResultGraph 和后续 attack/intercept/protection
```

**规范的三条硬约束**（V2.6:1166-1171 原文）

- **不是 Authority**；
- **不直接创造敌对关系**；
- **不能因为字符串、包名或同父类就独立判定目标归属**。

**已有零件**

| 零件 | 可作为 |
|---|---|
| `observation/CopyProvenanceIndex` | "只在真实复制操作之后被调用"的复制来源 |
| `observation/RecoveryBoundaryProof` | "真实已登记调用者帧；直接调用公开观察者不算边界事件" |
| `observation/RecoveryObservationScope` | "观察当前执行的服务器通道，**不做全局 Realm 猜测**" |
| `observation/TaskEventRun` | 同一任务的连续、已由接收者验证的事件 |
| `observation/NbtWriteReferences` | "真实写入器遍历" |

**验收**：一次真实状态变化能被逆推到 caller/task/callback/manager/dispatch/loader/native 来源之一，
且**该逆推可复核、可失效**；凭字符串/包名/同父类的判定一律判为不合格证据。

---

## 11. Q2 —— ResultGraph 生产实现

**入口**：Q1 完成。

**规范权威边界**（V2.6:343-350 原文）

ResultGraph：

- 是从 Raw Observation、provenance、dispatch、task、state、network、save 等证据派生的**结果关系图**；
- 可限制 lease、证明 closure、定位攻击面；
- **不能创造 Judgment**；
- **不能单独撤销 Tombstone**；
- **不能自己推进 TargetEpoch**；
- **重启后必须依据当前 Root Truth 和可信 evidence 重新建立**。

**容量策略**（V2.6:413-415 原文）

> 实现可以因内存和 CPU 使用**有界窗口**、分页、分片、LRU、backlog，但：
> **资源预算限制每个窗口处理多少，不限制最终应该处理哪些。**

⇒ 必须支持**分片 + 持久索引 + 冷热分离**，且**禁止语义截断**。
与 L6 Master §6.4 的 tombstone 容量策略同构：**只做物理表示优化，语义零变化**。

**交付物**

1. 图的 schema 与持久化表示（含分片与冷热段）；
2. 图的 executor（在预算内推进、可续接）；
3. 与 `obligation/CandidateRestrictions` 的接线（候选限制挂在图节点上）；
4. 与 `RootCommandBus` 的边界：图**只读** Root Truth，**不写**。

**验收**：容量压力下命中语义逐条不变（等价于 L5-INV-2 的 compaction 等价性判据）；
图重建不改变任何 Judgment / Tombstone / TargetEpoch。

---

## 12. Q3 —— ExistenceTerminationCertificate

**入口**：Q2 完成。

**规范边界**（V2.6:352-357 原文）

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

## 13. Q4 —— 证明主线

**入口**：Q2 完成（可与 Q3 并行）。

**依据**：L6 Master §11.1（7.17 FULL L5 Candidate 判据）——
"CFG `SemanticAttestor`、`NativeGateBridge`、`LoaderProofRegistry` all-copies、
`EnforcementLease` FENCED（经 RootCommandBus）、repair 链、restart/recovery verification"。

| 组件 | 规范要求 | 现状 |
|---|---|---|
| `AttestationManifest` | L6:262——"**语义不变量清单（非整类 hash）**" | 无。现有 CodeSeal 是**逐类 SHA-256**，是规范明确要求之外的**更弱形式** |
| `LoaderProofRegistry` | L6:263——"多 ClassLoader **all-copies** 证明" | **零代码** |
| `EnforcementLease` | L6:264——"proof→lease 状态（ACTIVE/**PROVISIONAL**/**FENCED**）；经 `RootCommandBus` 生效，**不直写 store**" | `RootExecutionFence.Lease` + `EnforcementRuntime` 部分对应 |
| `WriteSink` | 规范自承"完整 Native lease / WriteSink 尚未实现" | 3 个占位接口（`WriteSinkId` / `WriteSinkEvidence` / `WriteSinkExtension`） |

**这一阶段的意义**：**只有"更深的范围"有了证明形式，天花板才可能合法地超过 `PROTECT_RONOVA`。**
在此之前，任何超过该档的声明都是虚报。

**验收**：`AttestationManifest` 以**不变量**而非类哈希为单位；
all-copies 覆盖多 ClassLoader；lease 的 `FENCED` 经 `RootCommandBus` 生效且**不直写 store**。

---

## 14. Q5 —— 深度解锁

**入口**：Q3 与 Q4 均完成。

**这是唯一可以让矩阵的极限列声明为"已实现"的时点。**

| 解锁条件 | 对应阶段 |
|---|---|
| 候选有实体与置信度模型 | Q0 |
| 候选提升有据（MutationTrace） | Q1 |
| 候选关系有载体（ResultGraph） | Q2 |
| 完成有判据（Certificate，九项齐备） | Q3 |
| 更深范围有证明形式（Manifest / LoaderProof / Lease） | Q4 |

**验收**：同一目标在 ENHANCED 与 EXTREME 下**实际纳入的对象集合不同**，
差异可枚举、可解释、可复核，且每一份纳入都有独立证据；
**仅枚举值不同不算完成**。

---

## 15. 第二部的红线

1. **不要先写 ResultGraph 再想候选从哪来**——会得到空图；
2. **不要用 LRU 截断图的语义**——V2.6:404-411 禁止把"最多 N 个结果图节点"
   写成固定架构限制；预算只限制每窗口处理量；
3. **不要把 Certificate 当作 `denySpawn()` 的 Authority**——V2.6:355 明文禁止；
4. **不要把 MutationTrace 当 Authority**，或凭字符串/包名/同父类判定归属——V2.6:1168-1171 明文禁止；
5. **不要让 ResultGraph 写 Root**——V2.6:347-349 明文禁止（不创造 Judgment /
   不单独撤销 Tombstone / 不自己推进 TargetEpoch）；
6. **不要在 Q4 之前声明高于 `PROTECT_RONOVA` 的天花板**；
7. **不得因容量压力做语义截断**——与 L6 §6.4 tombstone 容量策略同构，只做物理表示优化。

---

## 16. 明确超出本文范围（需上层决策或平台能力）

以下不在第一部/第二部授权内；实施者**不得自行启动**：

| 项 | 阻塞原因 | 解冻条件 |
|---|---|---|
| **JIT / native dispatch 覆盖** | `ATTACK_DISPATCH` 极限列要求"覆盖候选解释/**JIT**/native dispatch 与最终调用目标"，而原生 JVMTI **仅申请 4 项能力**（`can_retransform_classes` / `_any_class` / `can_generate_all_class_hook_events` / `can_access_local_variables`），**无 compiled-method-load、无 method-entry** | 扩原生 JVMTI 能力或改用其他平台机制；需上层决策 |
| **跨进程 / 跨 JVM Root** | D13 与 `PROC-INV-1` 冻结：无 Named Pipe、无 HMAC、无跨进程 replay 协议 | 需第 1 层规范解冻 |
| **对同权限 Native 免疫** | 规范自承"不宣称对同权限 Native 绝对免疫"（§6.6.3 `INTERCEPT_JNI_NATIVE` 极限列） | 需上层决策 |
| **非 Windows 平台** | 存储后端限定 Windows 本地 NTFS + TxF | 需另立后端规格 |

---

## 17. 第二部完成判定

| 阶段 | 完成判据 |
|---|---|
| Q0 | 候选登记与置信度模型就位；提升需独立证据（沿用 `ObligationRegistry` 独立性规则） |
| Q1 | 一次真实变化可逆推至 caller/task/callback/manager/dispatch/loader/native 来源之一，且可复核、可失效 |
| Q2 | 图的容量压力下命中语义逐条不变；图不写 Root；重建不改 Judgment / Tombstone / TargetEpoch |
| Q3 | 九项条件各有生产者；证书持久化并带全部 revision；不成为 Admission Authority |
| Q4 | Manifest 以不变量为单位；all-copies 覆盖多 loader；lease FENCED 经 `RootCommandBus` 生效 |
| Q5 | ENHANCED 与 EXTREME 实际纳入集合不同且每份纳入有独立证据 |

**Q5 达成之前，矩阵的极限列不得声明为已实现。**
本文两部均不改变 `docs/CURRENT-PROGRESS.md` 的既有结论。

---

## 附：本文引用的实测证据索引

### 代码

| 事实 | 位置 |
|---|---|
| EXTREME = 54 键全 `STRICT_ALL`（统一映射） | `ProfilePresetMatrix.java:145-150` |
| 等级 rank 表 | `MechanismLevel.java:17-26` |
| `lowerOf` = 按 rank 取小 | `MechanismLevel.java:28-32` |
| `effective = min(请求, 支持, 证据)` | `StrengthPolicySnapshot.java:230-233` |
| `permitted = effective >= PROTECT_RONOVA` | `StrengthPolicySnapshot.java:211` |
| 天花板默认 `OFF` | `StrengthPolicySnapshot.java:99` |
| 快照构造传 `Map.of()` | `StrengthPolicySnapshot.java:137`、`ShadowDurableRootStore.java:344` |
| `OFF` 天花板 ⇒ `UNAVAILABLE` | `StrengthPolicySnapshot.java:383-387` |
| 非 ACTIVE ⇒ 拒绝 | `StrengthPolicySnapshot.java:205-208` |
| `withCapability` 仅 2 个调用者 | `RuntimeCapabilityProjection.java:11-23`、`TerminalActionPolicy.java:40` |
| 等级大小唯一 2 处消费点 | `StateFeedbackSchedule.java:23`、`RecoveryChainPolicy.java:11-24,55` |
| `registered*` 用预设不看天花板 | `RecoveryChainPolicy.java:113-121`、`:84-86` |
| `TERMINALIZE` 第三门 | `StrengthPolicyConfiguration.java:85-98` |
| `Maturity` 只能产出 S0/S1 | `SurfaceRegistry.java:7,47-48` |
| 表面采样器缺 2 个 | `surface/RuntimeDiagnostics.java:16-56` |
| 已有逐步骤边界举证范例 | `network/StrengthControlService.java:38-45` |

### 规范

| 事实 | 位置 |
|---|---|
| "三个正式预设必须按下表逐键解析"；统一映射"不是 V2.5 最终矩阵" | V2.6:776 |
| §6.6 逐键矩阵五张表 | V2.6:778 / :800 / :820 / :835 / :857；正确性要求 :874 |
| 演进口径：当前"仅稳定 key/seam；r52 统一 plane 映射保持事实口径" | V2.6:2767 |
| 禁止把统一 mapping 写成 exact-artifact 已完成 | V2.6:2885 |
| `FOLLOW_PROFILE` 禁止隐式漂移 | V2.6:879 |
| 矩阵升级的迁移与 schema 口径 | V2.6:927 |
| X51 验收口径（全 54 键 × 四档 + ceiling 缺失 + 矩阵升版） | V2.6:3074 |
| `effectiveLevel` 的合取公式 | V2.6:770-773 |
| S1–S6 为能力成熟度；只有 S6 成立才可声明该表面 FULL | Roadmap:250-264 |

### 规范（第二部）

| 事实 | 位置 |
|---|---|
| ResultGraph / Certificate 是**派生证据**，不是第二 Truth | V2.6:87、D60（:514） |
| **"完整 ResultGraph、MutationTrace、ExistenceTerminationCertificate 生产 schema 与 executor 从 7.17 起实现"** | **V2.6:90** |
| 7.16 不因 V2.2 返工成完整 ResultGraph/MutationTrace/Certificate 阶段 | V2.6:519（D65） |
| ResultGraph 权威边界（六条，含四条禁令与"重启后重建"） | V2.6:343-350 |
| ExistenceTerminationCertificate 权威边界（四条） | V2.6:352-357 |
| **三档目标范围定义**（COMPATIBLE / ENHANCED / **EXTREME = 所有候选路径**） | V2.6:365-400 |
| **Extreme 不设置的限制**："资源预算限制每个窗口处理多少，不限制最终应该处理哪些" | V2.6:404-415 |
| Extreme 允许牺牲的东西（第三方兼容性） | V2.6:417-427 |
| Extreme 绝对安全边界（13 项禁止 + 形式化表述） | V2.6:429-449 |
| **MutationTrace 五段管线**与三条硬约束 | V2.6:1154-1171 |
| `AttestationManifest` = "语义不变量清单（**非整类 hash**）" | L6:262 |
| `LoaderProofRegistry` = "多 ClassLoader **all-copies** 证明" | L6:263 |
| `EnforcementLease` = proof→lease（ACTIVE/PROVISIONAL/**FENCED**），经 `RootCommandBus`，**不直写 store** | L6:264、L6:346 |
| 7.17 FULL L5 Candidate 判据（含 SemanticAttestor / NativeGateBridge / LoaderProofRegistry / EnforcementLease） | L6:1092 |
| 极限关键 evidence 失效必须 **fail-closed**；极限可牺牲兼容性，不可牺牲持久化正确性 | V2.6:469-470 |
