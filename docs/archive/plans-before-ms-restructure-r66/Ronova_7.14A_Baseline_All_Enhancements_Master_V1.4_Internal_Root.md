# Ronova — 以当前源码链为基准的全量加强总纲 V1.4 — Internal Root

> 有效规划（2026-09-08 整理）：正文的历史版本、阶段和验证记录保留原义；当前实现见 [进度](../CURRENT-PROGRESS.md)。
> 规范顺序与后续实施入口见 [规划索引](README.md)，新功能不因规划登记而视为已实现。

> **2026-09-02 implementation note：**当前 working source 已推进到 `0.2.0-alpha.7.17-r53` ProfilePresetMatrix source candidate；正文中的 r50/40-test 状态是 V1.4 冻结输入，当前事实见 `docs/CURRENT-PROGRESS.md` 与根目录 `docs/archive/handoffs/7.17-PROFILE-PRESET-MATRIX-HANDOFF.md`。

> **V1.4 Internal Root + Bootstrap/Restart Boundary Revision — 2026-08-29**
>
> 本版与 Canonical L6 V2.4 对齐：取消 External Root / 持续外部 Authority/attack plane / 跨进程 Root IPC 作为正式路线，FULL L6 改为 JVM 内 `InternalDominionRoot` 唯一逻辑 Authority writer，`DurableRootStore` 为唯一 durable backend。
> L6E P0-A/B/C/D 继续保留，但全部建立在 Internal Root 上。
> **r27 historical source reconciliation：**Authority/Truth 主线保持在 Minecraft JVM 内；Native-unavailable `DUAL_AGENT` 可由 `AutoAttach` 启动短命 helper JVM 做 Agent attach。V1.4 允许这类 bounded bootstrap/relaunch path；D13/`PROC-INV-1` 禁止的是 helper/sidecar 成为 durable Authority writer、持续战斗执行面或 External Root。该历史审计结论已由当前 r50 路线继承。


> 文档日期：2026-08-29  
> Ronova 同步：项目身份、包路径、命令、配置与 r50 working source 对齐；r12 构建证据仅作为 7.14A frozen historical evidence
> 当前 working source：r50，`mod_version=0.2.0-alpha.7.15-r50`，注册 40 个 standalone SelfTest（39 个重混淆生产 JAR + 1 个开发主输出）。冻结 7.14A historical artifact：`build/libs/ronova-0.2.0-alpha.7.13-r12.jar`，SHA-256 `8570321AC5C178A5B802B8B423988F931E148B49201840D247ABD878C3B0F3AD`
> 当前阶段：**r50 working source：7.14B/7.14C/7.14D/7.15 source implemented and locally verified；7.16 next；r12 仅保留为 7.14A frozen evidence**
> 文档性质：**总体架构与后续加强路线；文中明确标注的 7.14B/7.14C/7.14D/7.15 实现状态以 r50 本地证据为准，其余未标注实现的增强仍属于设计目标**
> 修订版本：**V1.4 — Internal Root + Bootstrap/Restart Boundary Closure**  
> V1.4 重点：保持 Internal Root 主线，区分 r12 frozen evidence 与 r50 working source，允许 bounded helper/relaunch 但禁止 External Authority role，并冻结 7.14D synchronous-vs-deferrable budget 边界。

---

## 0. 文档目的

这份文档把此前讨论过的 Ronova 加强方案全部重新整理，并以 7.14A frozen baseline、r27 历史源码事实与当前 r50 working source 为起点。

核心原则只有两个：

1. **不把未来 L6 / L6 Enhanced 的能力倒灌进 7.14A。**
2. **所有加强必须服从正式 L6 的架构方向，而不是靠不断堆 Mixin / Hook / Event / fallback 形成“看起来很强”的屎山。**

因此本文把内容分为三层：

```text
当前真实实现
    ↓
正式升级路线
    ↓
FULL L6 Internal 之后的 Enhanced 强化
```

任何一项未来能力，在没有对应 Implementation Spec 和源码实现前，都只能视为**设计目标**。

---

# 1. 7.14A 冻结基准、r27 历史输入与 r50 当前源码

## 1.1 7.14A frozen baseline

`docs/archive/handoffs/7.14A-IMPLEMENTATION-HANDOFF.md` 与 r12 冻结证据确认：

- per-server `RawObservationSource`；
- weak `TopologyGraph`；
- immutable level / UUID / related snapshots；
- server ownership checks；
- `shouldIndex` / `shouldJudge` 分离，未使用的 `shouldTrack` 兼容别名已删除；
- RAW / LEGACY 一次性选择；
- `ronova.714a.rawObservation.enabled`；
- `RONOVA_714A_RAW_OBSERVATION_ENABLED`；
- 默认仍为 `LEGACY / false`；
- construction、admission、repair、discovery、Judgment、terminal cleanup、identity clearing、telemetry、commands、action logs、shutdown 已迁移到选定 observation port；
- RAW 模式下 Terminal enforcement 不再能够让 Raw Observation forget 物理对象；
- LEGACY 模式仍保留 7.13 `TopologyIndex.forget` rollback 路径；
- per-server passive `TerminalWatchSet`；
- `/ronova diagnose_uuid <uuid>`；
- r12 冻结候选注册并通过 32 个 standalone tests；该结果只属于 7.14A historical evidence，不属于 r50 当前验证；
- RAW integrated-client smoke 与 paired performance gate 已通过，但证据属于迁移前候选，不能自动继承为 r50 exact artifact PASS；
- passive watch 已改为 O(1) 聚合指标、UUID-scoped snapshot 与 1,024 条有界诊断历史。

r12 冻结基准的语义版本仍然是：

```properties
mod_version=0.2.0-alpha.7.13
```

这不是 r50 当前源码版本，而是 7.14A historical artifact 的版本事实。r12 的完整 scripted semantic / rollback scenario matrix 未形成最终 `verification-report.json PASS`，因此旧 evidence 不能提升为 r50 当前发布证明。

```text
7.14A frozen baseline
        +
r12 32 项 standalone self-tests PASS（历史证据）
        +
迁移前 smoke / performance 仅作历史证据
        +
r12 exact artifact 未形成最终 scripted verification PASS
```

## 1.2 r50 当前 working source（源码事实）

当前 r50 的源码事实如下：

- `gradle.properties`：`mod_version=0.2.0-alpha.7.15-r50`，Forge `47.4.22`；
- `build.gradle` 注册 40 个 standalone SelfTest：39 个针对重混淆生产 JAR，1 个 `DurableRootStoreSelfTest` 针对开发主输出；
- 7.14B `AuthorityView` / `AuthorityBinder` / `LegacyAuthorityView` / `AuthoritySplitFlags` 已存在并接入；
- 7.14C `PresencePolicy` / `OwnerImmunity` / `PresenceSplitFlags` 与三路 quarantine view 已存在并接入；
- 7.14D budget/telemetry 与 7.15 DurableRootStore Shadow 已实现并接入，`mc_check` 与 `mc_build` 均通过，40/40 分层本地回归通过；
- 本 revision pack 未附 r50 exact-artifact `verification-report.json PASS`，不得把 r12 的 32/32 PASS 归给 r50。

### 1.2.1 当前运行时进程边界（r27 源码确认）

```text
NativeBridge ACTIVE
    -> RonovaDominion returns NATIVE_ACTIVE
    -> no AutoAttach call
    -> Ronova-created child process = 0 on this path

NativeBridge not ACTIVE
    + default DUAL_AGENT + regular mod JAR
    -> AutoAttach ProcessBuilder(java.exe ... AttachHelper ...)
    -> short-lived helper JVM
```

因此可以根据当前可见 Java 源码声明“External Dominion Root 不存在、Authority/Truth 主线在 Minecraft JVM 内”。r27 唯一由工作区 Java 源码确认的生产 child-process path 是 Java `AutoAttach`/`AttachHelper`，它被归类为 bounded bootstrap helper；只要不托管 durable Authority、不持续执行战斗/目标监控并在 handoff 后退出，就不违反 Internal Root。候选包报告声称两份 Native DLL 未发现 process creation、Named Pipe、Socket 或远程进程注入接口；该 Native/PE 结论本轮标为 DOCUMENT-ONLY，仍需对应输入和 exact-artifact 运行验证。

`PROC-INV-1` 冻结为角色边界：允许 bootstrap/attach/recovery/relaunch helper，但 exact packaged artifact 必须证明这些进程用途有界、无 durable Authority 状态、handoff 后终止；External Root、持续 external combat plane、独立 target-monitoring process 与 canonical cross-process Root channel 禁止。

---

## 1.3 7.14A 冻结范围明确不包含

当前 handoff 已明确以下内容在 7.14A 范围之外：

- 7.14B Authority split；
- 7.14C Presence / OwnerImmunity split；
- 7.14D budget seam；
- 7.16 durable TerminalWatch takeover；
- Internal Dominion Root；
- FULL L5 / FULL L6；
- VITALS Health Authority 正式实现；
- Return / Dispatch Authority；
- Native Render Firewall；
- L6 Enhanced 的 State Ownership / Anti-Rollback / Integrity Closure 等后期能力。
- `PROC-INV-1` 的 bounded-role/process-tree 闭环；当前条件式 `AutoAttach` fallback 可保留。

---

## 1.4 冻结基准中的 Terminal 行为仍然是有限 retry

r12 冻结基准中的 `JudgmentEngine` 仍保留：

```java
private static final int[] RETRY_DELAYS = {1, 2, 5, 10, 20, 40};
```

因此该冻结基准中的 `TerminalWatchSet` 只是**被动镜像旧 retry 行为**，不是 durable enforcement owner。

正确理解：

```text
7.14A
Raw Observation 不再忘记 Terminal 对象

但

Terminal physical enforcement
仍使用旧有限 retry
```

真正无限逻辑寿命的 Tombstone / durable watch takeover 属于后续阶段。

---

## 1.5 冻结基准中的 Passive TerminalWatch 诊断面状态

r12 冻结基准中的 completed history 无界增长与全量 snapshot 问题已经修复；该基准具备：

```text
TerminalWatchMetrics        O(1) 聚合指标
snapshot(UUID) / diagnose   按目标读取
diagnostic history          固定上限 1,024
```

该修复只改变诊断成本，不把 passive watch 升级成 7.16 durable TerminalWatch。r12 当时剩余的是 exact artifact 的 scripted semantic / rollback closure，而不是再次实现诊断历史修复；这段历史证据不适用于替代 r42 的当前验证。

---

# 2. 后续所有加强必须遵守的硬边界

## 2.1 Raw Observation 与 Presence 永久单向分离

```text
Physical Runtime
      ↓
Raw Observation
      ↓
Internal Judgment / Reconcile / TerminalWatch

Raw Observation
      ↓
Presence Policy
      ↓
External Visibility
```

禁止：

```text
Presence / Quarantine
      ↓
反向过滤 Raw Observation
```

内部事实层必须仍然能够看到被隐藏、隔离、Terminal 的物理对象。

---

## 2.2 单一 Authority 真值源

任何阶段都禁止出现多套最终真值：

```text
HealthRoot
TerminalRoot
NativeRoot
JavaStore
各自都能创造最终 truth
```

正式目标改为：

```text
FULL L5:
DurableRootStore
= authoritative durable writer

FULL L6 Internal:
InternalDominionRoot
= 唯一逻辑 Authority writer
        ↓
DurableRootStore
= 唯一 durable backend
```

Java Domain / Native / Client 只消费 projection 或执行已授权操作。Native 永远不是 durable writer。

## 2.3 强度不能来自 Hook 数量

允许：

```text
明确 Authority 边界
      ↓
最小必要 critical seam
      ↓
独立 Observation / Integrity 验证
```

禁止把：

```text
Mixin + Event + ASM + 10 个 fallback
```

本身当成强度来源。

新增拦截点必须服务于明确的：

- Authority；
- Observation；
- Enforcement；
- Integrity；
- Native / Render boundary。

---

## 2.4 不做逐 Mod / Boss 特判架构

禁止把主线设计成：

```java
if (uomWither) { ... }
if (bossA) { ... }
if (bossB) { ... }
```

也不以 Mod ID / package / class whitelist 作为最终 Authority。

正确方向是：

```text
Target
 ↓
Identity Discovery
 ↓
State Ownership Discovery
 ↓
Authority Relation
```

无法解析时明确报告 `UNRESOLVED_AUTHORITY_OWNER`，而不是偷偷 fallback 成普通 `setHealth()`。

---

## 2.5 Fail-static

Integrity / Root / proof 出问题时：

```text
已有 committed truth
→ 尽可能继续 enforce

新的 authoritative mutation
→ FENCE
```

禁止：

```text
“证明失败了，所以先关闭保护再修。”
```

---

## 2.6 每个阶段只迁移一个清晰 Authority 边界

当前路线必须保持可回滚窗口，不能把：

- Raw Observation；
- Authority split；
- Presence split；
- Budget；
- DurableRootStore；
- TerminalWatch takeover；
- Internal Root；

一次性全部塞进同一个版本。

---

# 3. MC 实际威胁模型

Ronova 的主威胁模型应冻结在 Minecraft Mod 对抗范围，而不是企业安全 / 内核安全。

```text
T0 普通玩家

T1 作弊客户端 / 纯客户端 Mod
   ├ forged packets
   ├ movement / attack prediction
   ├ UI / render modification
   └ client memory modification

T2 同服务器 JVM 内的对抗 Mod / CoreMod
   ├ Forge Event
   ├ Mixin
   ├ ASM
   ├ reflection
   └ direct state mutation

T3 Java Agent / Instrumentation / JVMTI
   ├ retransformation
   ├ method-body injection
   └ class-load interference

T4 in-process Native adversary
   ├ JNI
   ├ JVMTI native
   └ native hook
```

不作为主线设计目标：

- OS administrator；
- kernel；
- hypervisor；
- TPM / HSM；
- SGX / SEV 必选部署。

---

# 4. L6 Enhanced 最终四条 P0 大链

经过前面所有方案去重，真正的核心可以压缩为四条链：

```text
P0-A Truth Authority
Identity
→ State Ownership
→ Durable Truth
→ Anti-Rollback

P0-B Execution Authority
Ownership
→ Dispatch
→ Return / Completion

P0-C Convergence Authority
Raw Observation
→ VITALS / TERMINAL
→ Resource-bounded Reassertion

P0-D Integrity Authority
Proof
→ Scoped Fence
→ Bounded Repair
→ Revalidation
```

P1 才是：

```text
Native Boundary
Render Boundary
Movement / Equipment / Effect
Audit / Deception
广泛 Formal / Chaos 工具化与扩展矩阵
```

P0-D critical protocol 的最小 invariant model 与指定 failure fixtures 仍是对应 Implementation Spec 的 gate；只有超出关键状态机的广泛形式化、长时 chaos 与平台工具化属于 P1。

## 4.1 四条 P0 链不是并列开发

逻辑依赖必须冻结为：

```text
                    ┌──────────────────────────────┐
                    │ P0-D Integrity（横切轨道）    │
                    │ 从契约阶段开始伴随每一阶段     │
                    └──────────────┬───────────────┘
                                   │
P0-A Truth Authority
Identity / Binding / Ownership / Durable Truth
                 ↓
P0-B Execution Authority
ExecutionAuthorityContext
→ Dispatch
→ Return / Completion
                 ↓
P0-C Convergence Authority
Raw Observation
→ Domain Enforcement
→ bounded convergence
```

其中：

- **P0-A 是语义地基**：没有稳定 Identity / Ownership，B/C 没有可消费的权威对象；
- **P0-B 依赖 P0-A**：Dispatch / Return 必须消费同一 authoritative binding / projection revision；
- **P0-C 依赖 A+B**：知道“应该修什么”还不够，还必须知道“谁有权修、修复路径是否可信”；
- **P0-D 不是最后才补**：它从每个阶段的 seam / contract 定义开始同步建设，最后才形成完整自动修复闭环。

禁止实现顺序：

```text
先把 A/B/C 全写完
→ 最后再想 Integrity 怎么证明
```

因为那样会导致关键 seam 无法证明、需要回头重构。

## 4.2 推荐的 Enhanced 实现顺序

FULL L6 完成后，推荐：

```text
L6E-0 Contract Foundation
├ SubjectIdentity / BindingRevision
├ CriticalOperationId
├ CompletionContract
├ DeviationSemantics
└ IntegrityCriticalSeam registry

        ↓

L6E-1 P0-A
Identity Binding
→ State Ownership Binding
→ ExecutionLease
→ Anti-Rollback binding semantics

        ↓

L6E-2 P0-B + P0-D/Structural
Execution Authority Chain
→ Dispatch
→ Return
→ Structural / Semantic proof of critical seams

        ↓

L6E-3 P0-C + Resource Semantics
VITALS / TERMINAL convergence
→ phase-aware enforcement
→ deviation accumulation
→ bounded work / storm behavior

        ↓

L6E-4 P0-D Closure
Scoped Fence
→ bounded Repair
→ Revalidation
→ Stable Fenced terminal state
```

这表示 **P0-D 贯穿 A/B/C**，而不是一个独立的末尾功能包。

## 4.3 四链共同的不变量

```text
Truth 决定“什么是真的”
Execution 决定“如何允许执行”
Convergence 决定“物理世界如何回到真值”
Integrity 决定“前三条链是否仍值得信任”
```

任意一条链不得反向成为另一条链的第二 Authority：

- Integrity 只能 Fence / Repair / Revalidate，不能自行创造 Judgment；
- Convergence 只能执行 projection，不能修改 Root Truth；
- Dispatch / Return 不能自行修改 Ownership；
- Ownership 不以某次物理执行成功与否决定 Root Truth。

---

# 5. Authority Policy 加强

## 5.1 目标

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

## 5.2 原则

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

## 5.3 Root 驱动的确定性 Threat Posture / Policy Overlay

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

## 5.4 Minecraft Tag 只作为高效 selector，不作为 Authority 证据

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

# 6. Identity + State Ownership

这是后续最重要的增强之一。

## 6.1 Identity 不再等于 UUID

最终 Identity 必须把稳定相等键、描述 metadata 与物理 incarnation 分开：

```text
SubjectKey                    唯一 equality / lookup key
├ realmId
└ subjectId

SubjectIdentity               以 SubjectKey 为锚点的逻辑描述
├ subjectType
├ origin / provenance metadata
├ identity schema metadata
└ Root-issued binding reference

PhysicalIncarnationId         独立的物理宿主身份
└ 不参与 SubjectKey 相等
```

`originDescriptor`、schema version、dimension、坐标、runtime class 都不得进入 `SubjectKey` 相等性。当前源码中的 `SubjectIdentity` 是 loader-neutral observation descriptor，L6E 需要显式 migration；不得在新包中并行创建第二套 Identity。

实体自己提供的：

- UUID；
- NBT；
- parent UUID；
- capability data；

只能作为 evidence，不直接成为 Authority。

---

## 6.2 State Ownership 的正确定义

State Ownership 回答：

> **“这个状态最终由谁解释？”**

它不是说：

> “其他 Mod 物理上无法写这个字段。”

例如：

```text
Root Desired HP = 1

外部 Mod：
HP → 999999

Raw Observation：
HP = 999999

Ownership：
VITALS semantics owned by Health domain

→ deviation
→ bounded reassert → 1
```

外部写入没有获得最终解释权。

---

## 6.3 OwnershipBinding 与 ExecutionLease 分离

不能让“所有权租约到期”直接导致 Authority 消失。

正式拆成：

```text
OwnershipBinding
= Root 对语义所有权的持续绑定

ExecutionLease
= 当前哪个 executor instance 可以替它执行
```

例如：

```text
OwnershipBinding = HEALTH
ExecutionLease = HealthDomain-A
```

A 崩溃：

```text
OwnershipBinding 仍然成立
ExecutionLease A 失效
→ 可重新授予 B
```

TERMINAL 更不能因为 executor lease 到期而失去 Tombstone 语义。

---

## 6.4 Ownership 优先级

至少冻结：

```text
TERMINAL / Lifecycle > VITALS / Health
```

一旦有效 Tombstone 存在：

- HealthDomain 停止维持“活着时生命状态”；
- Lifecycle / Terminal 获得生命周期最终解释权。

---

# 7. Raw Observation 2.0

7.14A 已经打好了最重要的地基。

后续增强不是回头增加 semantic filter，而是增加事实获取效率和完整性。

## 7.1 Event + Raw Reconciliation

```text
Fast Events
   ↓
ObservationBus

同时

Low-frequency Raw Reconciliation
```

Event 只负责加速：

```text
事件没发生 ≠ 状态没变化
```

最终事实仍来自 Raw Observation。

---

## 7.2 Observation 消费者

未来可以由同一 Raw 层服务：

- Identity Resolver；
- State Ownership Resolver；
- VITALS invariant；
- TerminalWatch；
- Admission closure；
- Runtime Behavioral Proof；
- Resource storm detection。

禁止每个 Domain 自己建立第二套“事实世界”。

---

# 8. Enforcement Plane

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

## 8.3 Phase-aware Enforcement / Tick-Level Control

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

# 9. VITALS Enhanced — 通用改血加强

这是之前“改血加强”最终整理版。

## 9.1 当前设计基础

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

## 9.2 VITALS 不是一次 setHealth

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

## 9.3 至少覆盖的生命状态

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

## 9.4 VitalStateResolver

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

### 9.4.1 Resolver 必须是“通用发现 + 可插拔证据”，不是 Adapter 海

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

### 9.4.2 Resolver Provider 的可信边界

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

## 9.5 UNRESOLVED 失败语义

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

## 9.6 VITALS 与 Owner Protection

当前生命恢复逻辑位于独立的 `VitalsRecoveryModule`。接入 Health Authority 后，应替换该模块的 desired-vitals 来源并统一走：

```text
HealthProjectionResolver
```

优先规则：

1. 有效 `LOCKED` HealthDirective 优先；
2. 无 HealthDirective 时保留 Owner Protection 现有语义；
3. 过期 / 低 epoch / 断序不得覆盖；
4. Judgment 终态不能被 Health Authority 复活。

---

## 9.7 VITALS 铁律

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

# 10. TERMINAL + Admission Closure

## 10.1 TERMINAL 最终语义

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

## 10.2 Admission Closure

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

## 10.3 Pre-Admission Binding

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

### 10.3.1 Pre-Binding 的现实实现边界

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

### 10.3.2 Pre-Binding 作为 P0-A 独立难点

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

# 11. Constraint Fusion

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

# 12. Execution Scheduling + Feedback

这部分不是 AllReturn。

## 12.1 Scheduling

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

## 12.2 Feedback

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

## 12.3 Domain 不得自行升级 Authority

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

## 12.4 原子多目标 JudgmentBatch：原子的是“逻辑提交”，不是物理世界

此前讨论过 `JudgmentTransaction / 两阶段提交`。最终应收敛为：

```text
AREA action
→ discover N subjects
→ canonical JudgmentBatch
→ one command_id / batch_hash
→ Root all-or-nothing durable commit
→ durable `CommitReceipt`
→ per-subject asynchronous physical convergence
```

需要保证：

- 一个 batch 的逻辑 Judgment 要么全部进入 durable truth，要么全部不进入；
- `COMMIT_UNKNOWN` 必须查询 / 重发**同一个 batch command_id**；
- batch 内 subject 顺序与内容需要 canonicalization，保证幂等；
- projection 更新应为 batch / incremental publish，避免 N 次全量 rebuild。

**不承诺 Minecraft 物理对象的瞬时原子性：**

```text
Root atomic commit ✅
所有实体在同一个 CPU 指令 / tick 瞬间同时物理消失 ❌
```

物理 convergence 仍受 budget / scheduling 管理。

因此不需要把 Minecraft Entity Runtime 强行包装成分布式 2PC participant。

---

# 13. Execution Authority Chain — State Ownership / Dispatch / Return 统一模型

这是此前方案最重要的结构修正。

三者不再各自为政，而属于同一次关键执行的三个阶段：

```text
Authoritative Projection
          ↓
┌─────────────────────────────┐
│   Execution Authority Chain │
│                             │
│ ① Ownership                 │
│   谁拥有状态解释权           │
│          ↓                  │
│ ② Dispatch                  │
│   哪条执行路径有资格         │
│          ↓                  │
│ ③ Return / Completion       │
│   哪种结果有资格传播         │
└─────────────────────────────┘
          ↓
Physical Runtime
```

---

## 13.1 Authority Snapshot

三阶段应消费同一个权威快照，而不是反复查三套 projection。

概念结构：

```text
ExecutionAuthorityContext
├ subject
├ operation
├ authority epoch
├ projection revision
├ ownership binding
├ authorized dispatch class
└ completion contract
```

Java 可以携带上下文优化热路径，但 Native critical boundary 不得无条件相信 Java 自报 Authority。

---

## 13.2 Dispatch Authority

Dispatch 回答：

> **“这次 Authority-bearing 调用最终应该落到哪个实现 / 路径？”**

只控制**具有 Authority 意义的 critical seams**，不试图接管整个 JVM 的所有 invokevirtual / vtable dispatch。

目标场景例如：

- Health mutation；
- Lifecycle transition；
- Admission commit；
- critical protection decision；
- 其他会改变权威物理投影的关键路径。

模型：

```text
Call
 ↓
Invocation Identity
 ↓
Dispatch Resolution
 ↓
Authorized Target?
 ├ yes → execute
 └ no  → reject / fence
```

实现后端可以是：

```text
instrumented critical seam
internal MethodHandle route
Native critical primitive
```

但它们只是后端，不是多套 Authority。

---

## 13.3 Return Authority / AllReturn

Return Authority 回答：

> **“执行结束后，哪个返回值 / exception / completion semantics 才有资格传播给 caller？”**

模型：

```text
Authorized Invocation
      ↓
Execution
      ↓
Raw Completion
├ RETURN(value)
├ THROW(exception)
├ ABORT
└ other completion
      ↓
Return Authority
├ ACCEPT
├ REWRITE
├ SUPPRESS
└ FENCE
      ↓
Caller-visible Completion
```

这与“ExecutionReceipt 发回 Root”完全不同。

它是此前正式 L6 与多数加强稿中真正缺失的一条执行语义轴。

---

## 13.4 Return / Dispatch 的技术实现路线

Return Authority **不定义为接管 JVM 所有方法返回**。它的范围冻结为：

> **所有被声明为 Authority-bearing Critical Operation 的 completion semantics。**

即所谓 AllReturn，在 Ronova 中应理解为：

```text
All Authority-bearing Returns
```

而不是：

```text
All JVM Returns
```

### 13.4.1 首选后端：显式 Execution Gateway

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

### 13.4.2 外部 / Minecraft 无法重构路径：最小 instrumented seam

对于不能改造成 Gateway 的第三方 / Minecraft critical path，可以使用：

- caller-side critical seam；
- method-entry / method-exit instrumentation；
- ASM / Mixin；
- 必要的 Native critical primitive。

但只允许覆盖经过 `CriticalOperationId` 注册的路径，不做全 JVM 广谱 method-return rewriting。

### 13.4.3 Return Authority 与 Integrity 的边界

两者必须明确区分：

```text
Return Authority
= 定义“什么 completion 是合法的”

Integrity Structural/Semantic Proof
= 证明当前字节码 / dispatch seam
  确实执行并消费了这个 completion contract
```

例如：

```text
CompletionContract says DENY must propagate as false

Return Authority:
定义 false 才是权威 completion

Structural Proof:
证明 callsite 没绕过 ExecutionGateway

Semantic Proof:
证明 Gateway 返回的 false 真正支配 caller 后续分支

Behavioral Proof:
证明 forbidden mutation 最终没有发生
```

因此 Return Authority **不是 Integrity 的子模块**；Integrity 是对 Return/Dispatch 实现的证明与恢复层。

### 13.4.4 Dispatch 本身必须进入 Integrity Critical Set

任何：

```text
DispatchBinding
ExecutionGateway
CompletionContract consumer
Native critical dispatch bridge
```

都属于 P0-D 的关键 seam。

一旦这些 seam 的 Structural / Semantic proof 失败，应按影响范围进入 `FENCED_COMPONENT / EXECUTION_PLANE / GLOBAL`，不能继续把未证明的 Dispatch 当正常执行路径。

---

# 14. Integrity Closure

此前多个加强稿出现大量：

- SemanticAttestor；
- RuntimeSemanticGuard；
- InvariantMonitor；
- InterceptionIntegrityMonitor；
- BehavioralConsistencyChecker；
- ClassRepairEngine；
- AdversarialDetector；
- Native integrity；

最终必须去重为一个 Integrity Plane。

---

## 14.1 Integrity Plane

```text
Structural Proof
├ semantic bytecode fingerprint
└ CFG / critical-edge proof

Semantic Proof
└ 关键决策结果是否真正被消费

Behavioral Proof
└ Raw Observation 验证物理结果是否收敛

Native Integrity
└ critical native/projection validity

Recovery
└ scoped fence / repair / revalidation
```

---

## 14.2 状态机

```text
ACTIVE
  ↓ anomaly
SUSPECT
  ↓ confirmed
FENCED_COMPONENT
  ↓
REPAIRING
  ↓
REVALIDATING
  ├ PASS → ACTIVE
  └ FAIL
      ↓
REPAIR_BUDGET_EXHAUSTED
      ↓
STABLE_FENCED
```

---

## 14.3 Repair 必须有预算

```text
RepairBudget
├ maxAttemptsPerIncident
├ cooldown
├ maxCost
└ escalationScope
```

禁止无限：

```text
repair → fail → repair → fail → ...
```

否则攻击者可以把修复系统本身变成 DoS 放大器。

---

## 14.4 Fence Scope

```text
COMPONENT
DOMAIN
EXECUTION_PLANE
GLOBAL
```

普通 EquipmentDomain 故障不能直接全局 FENCE。

只有以下核心信任基础出问题才允许升级到 GLOBAL：

- Root trust；
- authoritative projection；
- identity core；
- Execution Authority Chain core；
- Native critical gate；
- writer fence / RootInstance validity。

---

## 14.5 Repair 成功不等于 Trust 恢复

必须：

```text
Repair
 ↓
Structural Attestation
 ↓
Semantic Attestation
 ↓
Behavioral Challenge
 ↓
Native / Projection consistency
 ↓
Trust lease renewed
 ↓
ACTIVE
```

---

# 15. Anti-Interception / 方法体注入对抗

此前讨论过：

- bytecode hash；
- CFG；
- JVMTI retransformation；
- method-body injection detection；
- multiple implementations；
- classloader isolation；
- CFI；
- honeypots；
- code polymorphism；
- native final gate。

最终主线只保留：

```text
关键 seam 的结构证明
+
关键 branch / return 的语义证明
+
行为闭环
+
有界重新注入 / 修复
```

---

## 15.1 不把字节码 hash 当唯一真理

允许不同 bytecode variant，只要能够证明相同关键语义。

重点不是：

```text
“一个 NOP 都不能变化”
```

而是：

```text
DENY decision 是否仍支配 forbidden mutation edge
Return result 是否仍被 caller 正确消费
```

---

## 15.2 Code Polymorphism 降级为可选 Anti-analysis

周期性变异 / 多个等价实现可以提高固定 pattern 注入成本，但它不是 Authority 强度本身。

因此不进入 P0 主线。

---

## 15.3 不采用的高风险做法

主线不采用：

- Java method 内存页硬 `mprotect`；
- 常驻 JVMTI SingleStep CFI；
- 高频全类自变异；
- SecurityManager 作为 Java 17 核心防线；
- 把 ClassLoader isolation 当成 JVMTI 防火墙。

---

# 16. Native Trust Boundary

## 16.1 Native 的职责

```text
Root
→ Truth

Java Domain
→ Enforcement Request

Native
→ validate / guard / critical primitive
```

Native 不得：

- 创建 VITALS Intent；
- 创建 Tombstone；
- 自己修改 authoritative projection；
- 成为第二 Root。

---

## 16.2 Java 不能直接把 Authority 结论喂给 Native

不要：

```text
Java：我是 HEALTH，这是合法 token
Native：好
```

更合理：

```text
Java Request
├ subject handle
├ operation id
└ revision
      ↓
Native immutable projection
      ↓
resolve subject / ownership / permission
```

原则：

> **Java 提供请求；Native 自己解析关键 Authority。**

---

## 16.3 Native callback Java

Critical Native decision 不能再依赖一个可被修改的 Java callback 作为最终 Authority 判断。

Java callback 只能用于：

- telemetry；
- scheduling；
- non-critical formatting。

---

## 16.4 审计分级

热路径：

```text
compact audit token
```

只有 SUSPECT 状态才升级为：

```text
full stack / argument evidence
```

禁止每次 JNI 都采集完整双向栈。

---

# 17. Render Boundary + Native Render Firewall

## 17.1 Server 与 Client 的能力边界

服务器可以决定：

- 逻辑存在；
- interaction validity；
- movement validity；
- damage validity；
- tracking / Presence；
- authoritative state。

但恶意客户端永远可以自己画：

- fake boss；
- fake entity；
- fake UI；
- fake HP bar。

所以：

```text
Server Authority 强保证
≠
恶意 Client 屏幕像素强保证
```

---

## 17.2 Optional Client Authority Extension

受控整合包环境可以增加：

```text
Render Projection
 ↓
Java RenderDomain
 ↓
Native Render Firewall
 ↓
glDrawElements / glDrawArrays / glMultiDraw*
 ↓
ALLOW / DROP
```

识别可使用：

- shader program；
- texture；
- VAO/VBO；
- render context；
- call fingerprint。

但无上下文的任意 GL 绘制无法保证精确识别。

Native Render Firewall 不是 FULL L6 Server 必需条件。

---

## 17.3 RenderDirective 而不是 RenderIntent

客户端只是 projection consumer，不应该产生新的 Authority intent。

```text
RenderDirective
├ subject
├ disposition
└ projectionRevision
```

可能 disposition：

```text
VISIBLE
HIDDEN
DROP_DRAW
```

Client Render 状态不能反向决定 Server Truth。

---

# 18. Durable Truth / Anti-Rollback / Causality

## 18.1 正式路线

```text
7.14 Separation & Semantic Split
 ↓
7.15 Java DurableRootStore shadow
 ↓
7.16 durable cutover / enforcement closure
 ↓
7.17 FULL L5 candidate
 ↓
8.x Internal Dominion Root consolidation
 ↓
FULL L6 Internal
 ↓
L6 Enhanced
```

FULL L6 不建立 External Authority process。8.x 的目标是把 `InternalDominionRoot` 收口为唯一逻辑 writer，并把 `DurableRootStore` 降为 durable backend。bounded attach/relaunch helper 可以存在，但不得保存/裁决 Authority truth、不得在 handoff 后持续执行战斗或目标监控。

## 18.2 Anti-Rollback

Internal Root 必须通过本地 durable store 的 checkpoint lineage 防旧 snapshot 静默回滚：

```text
checkpoint generation
commit index
previous checkpoint digest
snapshot lineage
monotonic generation / fence
```

如果 durable store 与 world 一同被完整复制/回滚，Internal Root **不能宣称具有外部独立故障域**；该限制属于明确能力边界，而不是隐藏假设。

## 18.3 Causal Ordering

Authority 顺序不得依赖 wall clock；继续使用：

```text
epoch
revision
commit_index
command_id
causal predecessor
fence_token
```

## 18.4 COMMIT_UNKNOWN

同进程也可能出现：durable write 已发生，但 JVM crash / I/O exception 导致 caller 未收到 receipt。

```text
command_id = X
 ↓
result ambiguous
 ↓
restart/recover X / idempotent resend X
```

禁止创建新的同义 command id。

## 18.5 Internal Root Recovery

不做 HA / replica consensus。主线只要求：

- atomic/checksummed checkpoint；
- command dedup + durable receipt；
- root instance/fence generation；
- crash-safe reopen；
- corrupt store → explicit FENCED / recovery mode；
- repair 后 revalidate 才 ACTIVE。

# 19. Resource Resilience

后来这项从“性能优化”升格为 P0，因为：

> **如果攻击者可以把 Ronova 自己的修复机制打到 TPS 崩溃，Authority 在实战上就等于失效。**

---

## 19.1 State-driven Enforcement

攻击者一 tick 改 health 10000 次：

错误：

```text
10000 writes
→ 10000 repair tasks
```

正确原则不是“无脑只保留最后一次”，而是：

> **Coalesce work, not security evidence.**

昂贵的 repair work 可以合并，但对语义重要的 transition / extrema / violation 不能静默丢失。

### 19.1.1 DeviationSemantics

每种 invariant 必须声明自己的 accumulation / coalescing 语义：

```text
LATEST_STATE
    最终状态敏感，可保留最新 observed state

EXTREMA_LATCH
    保留窗口内最危险的 min/max excursion

EDGE_LATCH
    某个关键边沿发生过一次就保持 pending，直到消费

COUNT_RATE
    不逐个执行 repair，但保留次数 / 速率

BOUNDED_TRANSITION_LOG
    对少量真正需要顺序语义的关键 transition
    使用有界序列，而不是无限 event queue
```

例如：

```text
EXACT_HEALTH = 100
→ repair 可基于 latest state

MIN_HEALTH >= 10
→ 必须 latch “曾经低于 10” 与最小 observed value

TERMINAL revive attempt
→ 不产生 1000 个 lifecycle task
→ 但 revive edge / count 不能消失

Admission violation
→ EDGE_LATCH
```

### 19.1.2 Raw Observation 不是“必然看到所有瞬态”的魔法

Raw Observation 是内部事实来源，但不能假设低频 snapshot 一定看到：

```text
100 → 1 → 100
```

这种发生在两个 observation point 之间的瞬态。

因此对**瞬时敏感 / transition-sensitive invariant**：

```text
critical mutation seam / phase seam
→ TransitionWitness
→ O(1) accumulator / latch
```

Raw reconciliation 则负责最终状态独立验证。

正确组合：

```text
Fast witness / edge latch
        +
Raw state reconciliation
        ↓
one bounded convergence job
```

同理：

```text
1000 revive attempts
+
1 Tombstoned Subject
=
1 unresolved terminal subject
+
revive_attempt_count / edge evidence
```

不是 1000 个昂贵 lifecycle job，也不是把 999 次攻击证据丢掉。

---

## 19.2 必须具备

```text
Coalescing
Budget
Backpressure
Priority
Dominance
Bounded Queue
Dedup
Incremental Projection
```

---

## 19.3 Resource Priority

资源紧张时推荐：

```text
TIER 0
Root / Fence / Projection validity
Execution Authority critical integrity

TIER 1
TERMINAL / Admission critical enforcement
Identity / Ownership enforcement

TIER 2
VITALS critical convergence
critical behavioral verification

TIER 3
Movement / Equipment / Effect
ordinary reconciliation

TIER 4
Audit / telemetry / statistics
forensics enrichment
```

不能为了修 health 把“health mutation path 是否已被劫持”的 Integrity 完全停掉。

---

## 19.4 Storm Mode

```text
NORMAL
 ↓ pressure
PRESSURE
 ↓ adversarial pattern
STORM
```

STORM：

- 更激进 coalescing；
- 降低低优先 audit 频率；
- 非关键 reconcile 降频；
- 关键 Integrity 继续；
- TERMINAL / Admission 不丢；
- storm 本身产生 adversarial signal。

---

# 20. Movement / Equipment / Effect Domain 加强

这些保留为 Enforcement Domain，不升级成独立 Root。

## 20.1 Movement

可表达：

- FREEZE；
- TELEPORT_BLOCK；
- knockback constraints；
- region / path constraint；
- flight / movement desired state。

统一走 Observation → Invariant → Reassert。

---

## 20.2 Equipment

可表达：

- FORCE_EQUIP；
- LOCK；
- DISARM；
- ITEM_BAN；
- DURABILITY_LOCK；
- ENCHANTMENT_FORCE；
- BIND。

必须使用 persistent constraint + projection，不做一次性 `setItemSlot()` 逻辑。

---

## 20.3 Effect

可表达：

- FORCE_EFFECT；
- REMOVE_EFFECT；
- EFFECT_IMMUNITY；
- CLEAR；
- level / duration constraint。

仍然遵守单一 Root Truth。

---

# 21. Deception / Honeypot

Fake Authority Projection 明确禁止，因为它会污染真值层。

但 MC Mod-vs-Mod 环境可以保留独立低成本 deception：

```text
deception/
├ HoneyEntity
├ HoneyConstraint
├ HoneyApiSurface
└ DeceptionSignal
```

硬边界：

```text
DECEPTION
❌ 不进入 DominionProjection
❌ 不成为 Tombstone
❌ 不改变 State Ownership
❌ 不产生 Judgment
```

它只能产生：

```text
DeceptionSignal
→ AdversarialDetector / Integrity
```

而且：

```text
honeypot touched
≠ attacker proven
```

只能作为证据之一。

---

# 22. Audit / Incident / Formal / Chaos

这些是验证与恢复设施，不是新的 Authority Plane。

## 22.1 Incident Correlation

之前 SOAR 概念收缩为：

```text
Integrity Signals
      ↓
Incident Correlation
      ↓
Approved Response
      ↓
Repair / Fence / Re-attest
```

Incident 系统不能自行创造新的 Judgment。

---

## 22.2 Audit

只需要结构化 Security Event Store：

```text
command
judgment
revision
integrity incident
repair
fence
execution failure
```

不需要企业级“数据湖”。

---

## 22.3 Formal Verification

Formal Verification 不进入运行时热路径。对 single-writer、`COMMIT_UNKNOWN`、JudgmentBatch 原子性、terminal/admission 不复活与 scoped fence 等关键协议，最小 invariant model / machine-checkable state exploration 是 **P0-D 设计输入与验收门槛**；广泛模型覆盖、长时 chaos 与工具平台建设属于 P1，不要求每个普通 Domain 各造一套形式模型。

至少应对以下关键状态机建立机器可检查模型（优先 TLA+ / Alloy）：

- Internal Root single-writer / fencing；
- `COMMIT_UNKNOWN` / idempotent resend；
- TERMINAL + Admission 的不可复活不变量；
- `OwnershipBinding / ExecutionLease` handover；
- Integrity scoped fence / RepairBudget / Stable Fenced；
- Constraint Fusion deterministic result；
- Anti-Rollback / commit-index ordering；
- `JudgmentBatch` all-or-nothing logical commit。

原则：

```text
Formal model
→ 证明架构状态机没有明显逻辑洞
→ Implementation Spec
→ Runtime Integrity 再证明实际实现没有偏离
```

Formal Proof 不替代 Runtime Integrity；Runtime Integrity 也不能替代设计阶段的状态机验证。

进入对应 P0 Implementation Spec 时，应把形式模型 / invariant list 列为验收物之一。

---

## 22.4 Chaos / Adversarial Validation

开发验证应覆盖：

- JVM crash；
- Internal Root component / JVM crash；
- durable store I/O failure / lock / partial-write fault；
- durable commit / receipt ambiguity；
- stale projection；
- corrupted projection；
- revive storm；
- state churn；
- projection churn；
- Integrity repair failure；
- Native gate unavailable。

目标不是“制造功能”，而是证明系统在故障和持续对抗下不会 Authority 漂移。

---

# 23. 支持性安全增强：保留但不升格为核心轴

## 23.1 ClassLoader / Module isolation

用途：

- 缩小普通 Java surface；
- 限制 accidental reflection；
- 改善内部模块边界。

不能宣称它能阻止 JVMTI / Instrumentation。

---

## 23.2 Supply-chain self-attestation

可以验证 Ronova 自身：

- JAR hash；
- Native DLL hash；
- Agent artifact hash；
- Internal Root schema / durable store artifact；
- SBOM / build provenance。

不要求所有第三方 Mod 必须进入可信白名单。

---

## 23.3 Code signing / Native RX pages

可以作为 hardening，但不能宣称同进程 Native 对手永远无法修改。

它们提高成本，不改变 Authority 模型。

---

## 23.4 Entity Physical State Snapshot / Rollback：仅限受约束恢复辅助

此前讨论过“实体状态快照与回滚”。它不应成为独立 Authority 机制。

原因：

- snapshot 很快可能陈旧；
- 合法游戏变化可能与旧 snapshot 冲突；
- 直接回滚完整 Entity/NBT 可能恢复已经失效的引用、装备、Capability 或生命周期；
- 不能用 physical snapshot 覆盖 Tombstone / Identity / Root Truth。

最终定位：

```text
Authoritative Desired State / Projection
= 真值

Physical State Checkpoint
= 可选诊断 / 可逆 Domain 的恢复证据
```

允许场景：

- Equipment / Effect 等明确可逆状态的局部 checkpoint；
- repair 前后 diff；
- forensic / behavioral validation；
- 某些 Domain 在 Root Desired State 不足以重建物理细节时的受限恢复。

禁止：

```text
Entity snapshot
→ 覆盖 Root Judgment
→ 清除 Tombstone
→ 恢复旧 Identity Binding
```

因此它属于辅助 Recovery，不进入 P0 Truth Authority。

---

# 24. 明确淘汰 / 不进入主线的旧加强

以下想法保留为研究资料，不进入主实现：

| 旧方案 | 处理 |
|---|---|
| TPM / HSM | 不属于 MC 主威胁模型 |
| 外部进程 / SGX / SEV 必选 | 不进入 FULL L6 Internal 要求 |
| kernel / hypervisor 防御 | 超出范围 |
| 第三方 Mod 全量签名白名单 | 违反通用 Authority 方向 |
| Fake Authority Projection | 污染真值层，禁止 |
| ML / RL 自动产生 Judgment | Authority 不可交给非确定模型 |
| 高频代码自变异 | 仅可选 anti-analysis |
| SecurityManager | Java 17 主线不依赖 |
| Java method page `mprotect` | HotSpot 兼容性风险过高 |
| 常驻 JVMTI SingleStep | 热路径性能不可接受 |
| 每次 JNI 完整 stack trace | 热路径开销过大 |
| 每 Boss 独立 Adapter | 改为 State Ownership Resolver |
| 无限堆 Hook / fallback | 与正式 L6 方向冲突 |
| EventPriority 决定 Authority | Event 只能是 seam / observation accelerator |
| 动态密钥 / 执行前字节码解密 / 执行后擦除 | 可作为 anti-analysis 研究项；对已能 runtime 注入的 T3/T4 对手不是 Authority 防线，不进入主线 |

---

# 25. 从 7.14A 冻结基准到当前 r50 的升级路线

这是本文最重要的实施约束。

## 25.1 7.14A — completed / frozen baseline

7.14A 已完成并冻结，冻结对象是 r12 及其对应的源码、验证工具和历史证据：

```text
Raw Observation / Presence semantic separation
D5 terminal no-forget in RAW mode
per-server observation
passive TerminalWatch mirror
rollback path
validation tooling
```

r12 的 32 项 standalone self-tests、迁移前 smoke 和性能结果只作为 frozen historical evidence；它们不构成 r50 exact-artifact 发布证明。7.14A 冻结范围不包含后续的 Authority split、Presence split、14D budget、DurableRootStore 或 Internal Root。

---

## 25.2 7.14B — Authority Split（r42 已实现并接入）

r50 已完成 Authority 边界拆分：

```text
AuthorityView / AuthorityBinder
LegacyAuthorityView / AuthoritySplitFlags
```

后续文档不得把 7.14B 写成尚未实现；LEGACY 只表示显式回退路径。

---

## 25.3 7.14C — Presence / OwnerImmunity Split（r42 已实现并接入）

r50 已完成：

```text
PresencePolicy
OwnerImmunity
ServerQuarantineView / ClientQuarantineView / LegacyQuarantineView
PresenceSplitFlags
```

`ProjectionCoordinator` 在同一 projection snapshot 上组装 presence policy；Raw Observation 继续不受 Presence 反向过滤。

---

## 25.4 7.14D — Budget / Telemetry Seam（r50 继承并通过本地回归）

r50 已完成 Frame / Work Budget 的统一 seam、structured telemetry seam 与 7.15 Java DurableRootStore Shadow，为后面的 Durable Cutover 奠基。`mc_check` 与 `mc_build` 均通过，40/40 分层 standalone SelfTest 全部通过；JAR SHA-256 为 `0e0a82422cb56cc622d18a1ddb0a4864efb861f8a567d0c296cb30e67560879d`。Windows Forge、GPU、process-tree 与独立 `verification-report.json PASS` 仍属于外部发布验收边界。

这一阶段已落地：

```text
有限预算
可测量
可回滚
```

仅**可延期持续工作**可在预算耗尽后顺延。`AuthorityView`、`PresencePolicy` / `QuarantineView` 与当前对象的 admission decision 属同步关键查询，14D 只能对它们做 telemetry/performance gate，不得因为 budget exhaustion 延迟、返回假值或临时改变可见性。

这一阶段不直接实现完整 STORM 自适应系统。实施细节与回滚边界见 `docs/reference/Ronova_7.14D_FrameBudget_Telemetry_Implementation_Spec_V1.0_Internal_Root.md`。

---

## 25.5 7.15 — Java DurableRootStore Shadow

正式路线进入 durable truth 迁移：

```text
现有 durable owner
      ↓ shadow
Java DurableRootStore
```

这一阶段仍不是 Internal Root。

重点是证明：

- 单写者；
- durability；
- replay；
- projection；
- rollback window。

---

## 25.6 7.16 — Durable Cutover / Enforcement Closure

这一阶段才适合 durable TerminalWatch takeover。

从：

```text
passive mirror + finite legacy retry
```

迁移到：

```text
Tombstone-driven durable logical watch
```

并继续保持 Lifecycle Integration ONCE / State Reassertion REPEATABLE。

---

## 25.7 7.17 — FULL L5 Candidate

冻结：

- Java durable authority；
- Native critical validation / integrity；
- persistent projection；
- full lifecycle semantics；
- recovery semantics；
- `PROC-INV-1`：枚举并验证所有 helper/relaunch path；只允许 bootstrap/attach/recovery/relaunch，禁止 external Authority/持续 combat plane，并验证 handoff/termination。

达到 FULL L5 候选后再准备 Internal Root cutover。

---

## 25.8 8.x — Internal Dominion Root / FULL L6 Internal

FULL L5 后进行内部 Root 收口。进入本阶段的前提是 `PROC-INV-1` 已在 7.17 通过；保留合规 bounded helper 不阻断 8.x，但任何 helper/sidecar 越界为 Authority writer 或持续 combat plane 都会阻断：

```text
8.0 Internal Root Shadow
    ↓
8.1 Exclusive Root Gateway
    ↓
8.2 Internal Authority Cutover
    ↓
8.3 Recovery / Presence Closure
    ↓
8.4 FULL L6 Internal Release Gate
```

### 25.8.1 Internal Root 收口目标

8.x 必须完成：

- `InternalDominionRoot` 成为唯一逻辑 Authority writer；
- `DurableRootStore` 仅作为 durable backend；
- Domain / Agent / Native 无 direct store mutation；
- command_id + canonical hash dedup；
- `COMMIT_UNKNOWN` 本地 recovery；
- RootInstanceId + FenceToken 防 stale writer；
- anti-rollback/checkpoint lineage；
- immutable projection publication；
- realtime hot path 零同步 durable I/O；
- 全生产生命周期无 External Authority writer/持续 external combat plane；覆盖 Native-unavailable helper 与可选 relaunch 的 role/handoff 验证。

8.x 继续预留 L6E 需要的 `SubjectKey` / revision / completion schema，但不提前实现 P0-A/P0-B 行为。

**明确限制：** FULL L6 Internal 与 Minecraft JVM 同故障域，不获得外部进程隔离。

## 25.9 FULL L6 Internal 之后 — L6 Enhanced

本文中以下高级能力只能在 FULL L6 Internal 之后正式落地。FULL L6 前允许提前写设计、contract fixture 与非生产 reference model，但不得把 L6E production behavior 倒灌进当前实现：

```text
Identity + State Ownership
Execution Authority Chain
VITALS State Ownership Resolver
Integrity Closure
Durable Anti-Rollback hardening
Advanced Resource Resilience
Native Render Firewall
Root HA
```

总纲负责确定最终方向，不代表它们现在就应该进入 7.14A。

---

# 26. 各增强轴的验收原则

## 26.1 Identity / Ownership

必须证明：

- UUID clone 不直接获得原身份；
- lineage 来自独立 observation / Root binding；
- dimension transfer 不丢失 logical identity；
- executor lease 失效不导致 OwnershipBinding 消失。

---

## 26.2 VITALS

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

## 26.3 TERMINAL

必须证明：

- Tombstone 逻辑寿命不由有限 retry 决定；
- physical object 重新出现仍被 Raw Observation 看见；
- Admission closure 能阻止合法重建；
- Root revoke 是恢复存在资格的唯一 Authority mutation。

---

## 26.4 Dispatch / Return

必须证明：

- unauthorized dispatch path 不能获得 Authority；
- critical return value 被覆盖时可以被发现 / 阻止传播；
- exception / abort / rewrite semantics 有确定模型；
- Dispatch implementation 本身处于 Integrity critical set。

---

## 26.5 Integrity

必须证明：

- 方法体 / critical seam 被改可检测；
- 修复失败不会无限循环；
- component failure 不无条件升级 global fence；
- repair 后必须 revalidate 才恢复 ACTIVE；
-旧 committed protection 在可信路径仍可继续。

---

## 26.6 Resource Resilience

必须做 adversarial workload：

- high-frequency health rewrite；
- revive storm；
- spawn storm；
- packet flood；
- projection churn；
- audit flood。

验收不只是 TPS：

> **有限预算下 Authority Truth 不漂移，关键约束最终仍可收敛。**

---

## 26.7 Production Process Boundary

必须同时证明：

- production Java/source/artifact 中所有 `ProcessBuilder`、`Runtime.exec`、Attach-helper/relaunch 入口都被完整枚举并绑定到明确的 bootstrap/attach/recovery/relaunch purpose；不得存在未分类或隐藏的 process-launch path；
- Native source 与 exact DLL import table 必须枚举任何 process creation；若未来 Native 参与 relaunch，只允许 bounded bootstrap/relaunch purpose。external injection、Named Pipe/cross-process shared memory/direct network 不得成为 Root Authority transport；
- Windows process tree 枚举 normal startup、runtime reassertion、shutdown、forced Native-unavailable 与可选 relaunch 的所有 Ronova-created process，并证明 helper 生命周期有界、handoff 后退出；
- Native unavailable 时允许 bounded attach/relaunch；若无法建立合规 in-JVM runtime，则进入 documented degraded/fenced；
- Gradle、PowerShell、Forge scenario runner 与 profiler 只属于构建/验证工具，永不从 production runtime 调用。

---

# 27. 最终整合架构

```text
                         Internal Dominion Root
                 Truth / Policy / Identity / Causality
                              │
                      Immutable Projection
                              │
              ┌───────────────┴────────────────┐
              │                                │
      Identity Binding                 Constraint Truth
              │                                │
      State Ownership Binding                  │
              │                                │
      Execution Lease                          │
              └───────────────┬────────────────┘
                              ↓
                   Execution Authority Chain
             ┌────────────────────────────────┐
             │ Ownership                      │
             │      ↓                         │
             │ Dispatch                       │
             │      ↓                         │
             │ Return / Completion            │
             └────────────────────────────────┘
                              ↓
                       Physical Runtime
                              ↓
                       Raw Observation
                              ↓
                    Invariant / Reconcile
                              ↓
                     Execution Feedback
                              ↓
                             Root

Enforcement Domains:
VITALS / TERMINAL / Admission / Movement / Equipment / Effect

Integrity 横跨全链：
Structural Proof
→ Semantic Proof
→ Behavioral Proof
→ Scoped Fence
→ Bounded Repair
→ Revalidation

全链底座：
Durable Anti-Rollback
Resource Resilience
Audit / Formal / Chaos

边界扩展：
Java ↔ Native Trust Boundary
Server ↔ Client Render Boundary
Optional Native Render Firewall
```

---

# 28. 最终优先级总结

## 当前状态与下一步

```text
7.15 Java DurableRootStore Shadow implementation candidate
已完成：mc_check、mc_build、40/40 分层 standalone SelfTest PASS
下一步：7.16 Java Durable Cutover / TerminalWatch takeover
```

当前不提前实现 P0-A、P0-B、P0-C、P0-D 或 Enhanced L6 runtime。

---

## 正式阶段路线

```text
7.14A
Raw Observation semantic split
      ↓
7.14B
Authority split
      ↓
7.14C
Presence / OwnerImmunity split
      ↓
7.14D
Budget seam
      ↓
7.15
Java DurableRootStore shadow
      ↓
7.16
Durable cutover + TerminalWatch takeover
      ↓
7.17
FULL L5 candidate
      ↓
8.x
Internal Dominion Root
      ↓
FULL L6
```

---

## FULL L6 Internal 后最重要的 Enhanced P0

```text
P0-A Truth Authority
Identity + State Ownership + Anti-Rollback

P0-B Execution Authority
Ownership + Dispatch + Return

P0-C Convergence Authority
Raw Observation + VITALS/TERMINAL + Resource-bounded Reassertion

P0-D Integrity Authority
Proof + Scoped Fence + Bounded Repair + Revalidation
```

真正的设计目标不是“功能多”，而是：

> **Root 决定事实；Identity 与 State Ownership 解释状态归属；Execution Authority Chain 决定关键调用如何执行、如何返回；Raw Observation 独立观察现实；Enforcement 在有限预算下持续让现实收敛到 Authority；Integrity 持续证明整条执行链仍然可信；即使重启、回滚、注入、状态风暴或客户端欺骗，也不能让 Authority 静默漂移。**

---

# 29. 本文冻结结论

1. 7.14A 已完成并冻结；当前 r50 已完成并接入 7.14B/7.14C/7.14D/7.15，7.16 是下一实现阶段。
2. 7.14D 的有限预算与遥测 seam、7.15 的 Java DurableRootStore Shadow 已作为 implementation candidate 落地；本轮以 `mc_check`、`mc_build` 与 40/40 分层 standalone SelfTest 完成收尾；不提前实现 7.16 之外的 P0 链。
3. Canonical L6 Master Plan 继续作为上位架构，不被后续加强稿覆盖。  
4. VITALS 与 TERMINAL 永久分离。  
5. Unknown Mod / Boss 的问题用 **State Ownership Discovery** 解决，不用逐 Mod Adapter 海。  
6. State Ownership / Dispatch / Return 统一为 **Execution Authority Chain**。  
7. Integrity 必须有 scoped fence、repair budget 和明确不可修复终点。  
8. Resource Resilience 是实战强度，不只是性能优化。  
9. Native 只做 critical validation / primitive，不成为第二 Root。  
10. Client Render 是独立信任边界，Native Render Firewall 只能作为受控客户端扩展。  
11. Anti-Rollback、Causality、Fencing 必须保证 durable truth 不静默倒退。  
12. 后续不再横向发明大量“大模块”，而应按四条 P0 大链向下写不变量、状态机、数据模型和 Implementation Spec。  
13. 当前 r50 的 `AutoAttach` / `AttachHelper` helper 从源码看是 bounded bootstrap path 候选；它无需在 7.17 前删除，但必须通过 `PROC-INV-1` 的用途、Authority-role、handoff 与 termination 验证。

# 30. V1.4 — Implementation Spec 前的缺口收口

对照此前所有讨论，以下曾经“缺失或隐含”的项目现已明确归档：

| 历史加强项 | 历史 V1.1 最终归属 |
|---|---|
| Entity State Snapshot & Rollback | §23.4：仅作为受限 Physical State Checkpoint / Recovery 辅助，不成为 Authority |
| Tick-Level Control | §8.3：收敛为 Phase-aware Enforcement / critical phase seam |
| 动态密钥与执行前解密 | §24：明确降级为 anti-analysis，不进入主线 |
| Minecraft Tag 高效过滤 | §5.4：Target selector / policy matching 优化，不作为 Authority proof |
| 原子多实体 JudgmentTransaction | §12.4：Root 逻辑 batch 原子提交；物理 convergence 不承诺瞬时原子 |
| Root 驱动全局动态策略 | §5.3：确定性、revisioned PolicyOverlay，不允许 ML/RL 或 Incident 系统直接创造 Judgment |
| P0 四链开发依赖 | §4.1~4.3：A → B → C，D 横切伴随 |
| Return / Dispatch 实现路线 | §13.4：ExecutionGateway 优先；无法改造成 Gateway 的非 Gateway critical seam 只作最小补充，由 Integrity 负责证明 |
| Resolver 可扩展性 | §9.4.1~9.4.2：ResolverProvider SPI 只提供 evidence，不拥有 Authority |
| Coalescing 状态丢失风险 | §19.1：Coalesce work, not security evidence；按 DeviationSemantics 累积 |
| Admission Pre-Binding 难度 | §10.3.1~10.3.2：Hard Prebind / Pre-Presence / Emergency Reconcile 分层 |
| FULL L6 Internal → Enhanced 迁移成本 | §25.8.1：8.x wire / projection 仅预留数据契约 |
| Formal Verification 定位 | §22.3：提升为 P0-D design input / acceptance gate |

## 30.1 进入 Implementation Spec 前仍需逐项冻结的四个协议

以下是冻结门，不是待无限扩写的愿望清单。`L6E-0 Contract Freeze V1.3 Internal Root` 已逐项给出可测试合同，下游 `P0-A V1.3 Internal Root` 因而可标记 `IMPLEMENTATION SPEC READY`：

1. **ExecutionAuthorityContext schema**
   - 字段 requiredness、wire type、作用域和不变量；
   - 哪些字段必须进入 Native validation，哪些仅为诊断 evidence；
   - ownership / binding / projection / policy revision 的一致性矩阵与允许跨越规则；
   - completion contract 的 RETURN / THROW / ABORT / SUPPRESS / REWRITE 表达、postcondition 与 side-effect scope。

2. **ResolverProvider SPI contract**
   - provider 注册、ACTIVE/SUSPENDED/REVOKED/FAILED 生命周期与 revision；
   - candidate evidence 格式；
   - validation / revoke / stale / provider disappearance 后的 revalidation 规则；
   - hostile provider 的隔离、审计和对既有 binding 的影响。

3. **DeviationAccumulator contract**
   - `LATEST_STATE / EXTREMA_LATCH / EDGE_LATCH / COUNT_RATE / BOUNDED_TRANSITION_LOG` 的状态结构、update/peek/consume/reset 精确定义；
   - 各 Domain 默认语义；
   - memory、overflow、eviction、counter saturation 与 evidence-loss failure 行为。

4. **Pre-Admission Binding state machine**
   - `DISCOVERED / PREBOUND / ACTIVE / HELD / CANCELLED / EMERGENCY_RECONCILE`；
   - 每个 transition 的 authority、revision 与失败结果；
   - Forge/Minecraft seam → HARD_PREBIND / PRE_PRESENCE_BIND / EMERGENCY_RECONCILE 对照表；
   - 哪些 seam 可真正 hard-gate，哪些只能 post-admission repair；
   - Root unavailable/fenced、stale projection、unknown schema 与 durable-I/O ambiguity 时的明确行为。

四项状态：**SATISFIED BY L6E-0 V1.3 Internal Root**（Execution context §7、Resolver §8、Pre-Admission §9、DeviationAccumulator §15，并由 T-08～T-15 验证合同）。这表示“可以开始按 P0-A 实施”，不表示对应 runtime 已经实现或通过游戏内验证。

## 30.2 类型复用与 Integrity 横切门

进入 P0-A 前还必须附一张唯一的迁移矩阵：

```text
现有 dev.ronova.coremod 类型
→ KEEP / EXTEND / REPLACE-IN-PLACE / ADD / DELETE
→ canonical package
→ schema migration
→ writer / reader
→ 删除旧 facade 的条件
```

该矩阵必须保证 `DominionRootGateway`、`DominionCommand`、`CommitReceipt`、`DominionEpoch` 与 Identity 只有一套 canonical truth path。兼容 facade 最多只读、必须有删除门，不能成为第二 writer。

P0-D 继续按 §4.1 横切 A/B/C：P0-A 在定义 Root/Projection/Binding seam 的同时，至少交付 structural validation、wire canonicalization 与 FENCED snapshot fixture；完整 repair/revalidation engine 仍留给后续 P0-D closure。

## 30.3 统一规范链（2026-09-05 文档接合）

当前规范顺序与 [架构索引](README.md) 及 [加强总纲 V2.6 §1.1](Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md) 一致：

```text
Canonical L6 Master V2.4 Internal Root
→ 本文 V1.4 Internal Root
→ L6E-0 Contract Freeze V1.3 Internal Root
→ 7.16+ JVM Extreme Strength Enhancement Master V2.6 Internal Root
→ P0-A Implementation Spec V1.3 Internal Root
```

其他阶段实现规格与 P0-A 同为下游实现层，不能覆盖上游冻结决策。V2.6 是明确纳入的强度增量，不是另找旧 `Post-V1.1 Hardening Addendum`；旧 hardening closure 仍已内联本文。V2.5/V2.4 加强稿只作历史，不与当前规范并行裁决；r53 源码状态及历史验证证据不因文档接合而改变。
