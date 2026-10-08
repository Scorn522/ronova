# Ronova 实体防御与存在一致性规划

**fix11已有基线入口（原文块外）：** [复制来源的反向索引与局部失证](R66-CAPABILITY-WIRING-PLAN.md#eight-a1)、[已有模块维护宿主](R66-CAPABILITY-WIRING-PLAN.md#eight-a4)纳入复用清单；不把实体/复制关系当新Truth或全能力依赖图。原方向/ID不改。

**fix10职责入口（原文块外）：** [S3读取范围/不确定性](R66-CAPABILITY-WIRING-PLAN.md#eight-s3)、[S4已有失证链推广](R66-CAPABILITY-WIRING-PLAN.md#eight-s4)、[A1按依赖scope撤销](R66-CAPABILITY-WIRING-PLAN.md#eight-a1)复用原身份、注册表与Root；不把实体/任务图变成新的权威图，不把内存观察当全表面完成。

**fix8就地精度入口（原文块外）：** [②只补对象回收观察](R66-CAPABILITY-WIRING-PLAN.md#v13-object-lifecycle)；持久成员资格来自投影而不是弱引用缺席，[⑤采样陈旧与证书到期分开](R66-CAPABILITY-WIRING-PLAN.md#v13-certificate)。旧原文与不变量编号保留。

> **fix7 同权限清点职责入口：** 准入/身份清理授权、恢复源证据绑定与客户端未入租约的状态已融入[既有工作章节](R66-CAPABILITY-WIRING-PLAN.md#same-privilege-surface)；[处理回执](RESTRUCTURE-REVIEW.md#review-fix7-surface)。仅规划补足，旧原文块和ID不改。

> **fix6 V1.3职责入口：** ②对象回收仅为观察，保留弱引用与持久裁决；不把GC当terminal或新身份源。 [逐条回执](RESTRUCTURE-REVIEW.md#review-fix6-v13) · [当前融合规划](R66-CAPABILITY-WIRING-PLAN.md#v13-receipt)。
> 当前接线增补：[r66 能力发布、动作执行与 JVMTI 规划](R66-CAPABILITY-WIRING-PLAN.md)。下方旧块保真保留；就地更正只作用于注明的历史判断，不代表源码已修改。
> 结构来源：`.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md`。重构日期：2026-09-14。
> 本书按 MS 原有二级、三级标题顺序组织；下方“迁入原文”来自 Ronova 输入包。MS 示例、Ronova 原条款和本次编排说明分别标注。
> **只有文档迁移与明确标注的规划增补，没有实现改动或运行验收。** 原文中的已实现、未实现、成功/失败、日期、源码行号和历史版本都属于原记录，不是本轮确认。
> 当前限制与文档效力见 [规划入口](README.md)；旧文件/节号/行号与新位置见 [完整迁移映射](RESTRUCTURE-MAP.md)。
> 人读入口：[不变量全集与全部编号索引](INVARIANT-INDEX.md)；[启动参数选型](README.md#scope-startup-parameters)；[P 层与完成口径](README.md#scope-p-layer)。
> **旧语境约定：** 迁入段落的“本文/本节/第 X 节/§X”继续指其原来源的章节；可解析引用已转为新位置链接。相同 ID 在不同原来源中不合并、不重编号。

本书收纳 **139 个原文分段**。这些分段保留标题文字、正文、表格、示例和全部原 ID；仅调整标题层级及引用链接。

## 章节导航
- [先定义防御契约](#ms-entitydefence-l14)
  - [划定策略范围](#ms-entitydefence-l25)
- [为什么只覆写实体方法不够](#ms-entitydefence-l42)
- [保留一个规范实例](#ms-entitydefence-l58)
  - [规范实例规则](#ms-entitydefence-l62)
  - [修复前的状态不变量](#ms-entitydefence-l72)
- [通过正常生命周期初始化](#ms-entitydefence-l88)
- [构建幂等修复过程](#ms-entitydefence-l103)
  - [修复时机](#ms-entitydefence-l117)
- [修复服务端注册表](#ms-entitydefence-l132)
  - [分区存储](#ms-entitydefence-l145)
  - [ID 与 UUID 查询](#ms-entitydefence-l151)
  - [Tick 集合](#ms-entitydefence-l157)
  - [持久管理与生命周期](#ms-entitydefence-l168)
  - [玩家追踪](#ms-entitydefence-l174)
- [修复客户端注册表](#ms-entitydefence-l184)
- [防御读取路径](#ms-entitydefence-l199)
- [可选：实体列表独占防御](#ms-entitydefence-l215)
- [实体状态也必须防御](#ms-entitydefence-l238)
- [从零实现顺序](#ms-entitydefence-l254)
- [内部集合规则](#ms-entitydefence-l268)
- [验证矩阵](#ms-entitydefence-l282)
- [故障定位](#ms-entitydefence-l300)
- [完成标准](#ms-entitydefence-l314)

---

<a id="ms-entitydefence-l14"></a>

## 先定义防御契约

**框架来源：** [MS entitydefence，原 L14–L24](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#先定义防御契约)。

**编排说明（非原条款）：** MS 的对象状态与注册表成员关系双层合同，承接 Ronova 原保护、Ownership 与存在连续性要求；不把攻击清除误记为防御补回。

**关联原条款：** [MSC · 1.8 规范实例与状态不变量](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0108)；[CF · 17. Relationship Graph Trust Contract](MS-SKILLS-PLAN.md#orig-cf-l1085)。

本节点没有被单独搬入的原文段落；它保留 MS 的独立职责入口。上列关联条款或下级章节不等于本节点所有细节已经覆盖；适用性与缺口见映射表。

> **fix7 就地说明：** 目标类名在租约名单不等于每个注入受保护；[合同覆盖及未证明范围](R66-CAPABILITY-WIRING-PLAN.md#surface-mixin)仍需依实际侧别和消费路径核对。

<a id="ms-entitydefence-l25"></a>

### 划定策略范围

**框架来源：** [MS entitydefence，原 L25–L41](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#划定策略范围)。

**编排说明（非原条款）：** 按原 Subject、绑定与保护范围适用。MS 示例的“一世界一个实体”不能无条件扩大为整个 Ronova 只能保护一个对象。

**关联原条款：** [CF · 3.1 RealmId](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0213)；[P0A · 6.1 核心记录](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0462)；[MSC · 1.8 规范实例与状态不变量](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0108)。

本节点没有被单独搬入的原文段落；它保留 MS 的独立职责入口。上列关联条款或下级章节不等于本节点所有细节已经覆盖；适用性与缺口见映射表。

<a id="ms-entitydefence-l42"></a>

## 为什么只覆写实体方法不够

**框架来源：** [MS entitydefence，原 L42–L57](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#为什么只覆写实体方法不够)。

**编排说明（非原条款）：** Raw Observation、Presence、对象状态、查询、tick、追踪和渲染职责分别可定位；不以单个 remove/hurt Hook 代表全部表面。

<a id="orig-bl-l0750"></a>

**迁入原文｜BL｜旧 L750–L755** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0750)

原标题路径：7. Raw Observation 2.0

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0750 -->
#### 7. Raw Observation 2.0

7.14A 已经打好了最重要的地基。

后续增强不是回头增加 semantic filter，而是增加事实获取效率和完整性。


<!-- RONOVA-ORIGINAL-END orig-bl-l0750 -->

<a id="orig-bl-l0756"></a>

**迁入原文｜BL｜旧 L756–L777** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0756)

原标题路径：7. Raw Observation 2.0 → 7.1 Event + Raw Reconciliation

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0756 -->
#### 7.1 Event + Raw Reconciliation

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


<!-- RONOVA-ORIGINAL-END orig-bl-l0756 -->

<a id="orig-bl-l0778"></a>

**迁入原文｜BL｜旧 L778–L793** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0778)

原标题路径：7. Raw Observation 2.0 → 7.2 Observation 消费者

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0778 -->
#### 7.2 Observation 消费者

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


<!-- RONOVA-ORIGINAL-END orig-bl-l0778 -->

<a id="ms-entitydefence-l58"></a>

## 保留一个规范实例

**框架来源：** [MS entitydefence，原 L58–L61](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#保留一个规范实例)。

**编排说明（非原条款）：** 规范当前实例与 durable SubjectKey 是不同层次。既有 identity/binding/ownership 合同迁入；MS 规范引用的全部修复规则仍需按 SubjectKey/BindingRevision 对接。

**文档缺口：** [GAP-MAP-02 · 规范当前实例和修复前七项物理状态](RESTRUCTURE-MAP.md#gap-map-02)。

<a id="orig-bl-l0634"></a>

**迁入原文｜BL｜旧 L634–L637** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0634)

原标题路径：6. Identity + State Ownership

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0634 -->
#### 6. Identity + State Ownership

这是后续最重要的增强之一。


<!-- RONOVA-ORIGINAL-END orig-bl-l0634 -->

<a id="orig-bl-l0670"></a>

**迁入原文｜BL｜旧 L670–L701** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0670)

原标题路径：6. Identity + State Ownership → 6.2 State Ownership 的正确定义

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0670 -->
#### 6.2 State Ownership 的正确定义

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


<!-- RONOVA-ORIGINAL-END orig-bl-l0670 -->

<a id="orig-bl-l0735"></a>

**迁入原文｜BL｜旧 L735–L749** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0735)

原标题路径：6. Identity + State Ownership → 6.4 Ownership 优先级

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0735 -->
#### 6.4 Ownership 优先级

至少冻结：

```text
TERMINAL / Lifecycle > VITALS / Health
```

一旦有效 Tombstone 存在：

- HealthDomain 停止维持“活着时生命状态”；
- Lifecycle / Terminal 获得生命周期最终解释权。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l0735 -->

<a id="orig-cf-l0211"></a>

**迁入原文｜CF｜旧 L211–L212** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0211)

原标题路径：3. Canonical Identifier Model

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0211 -->
#### 3. Canonical Identifier Model


<!-- RONOVA-ORIGINAL-END orig-cf-l0211 -->

<a id="orig-p0a-l0546"></a>

**迁入原文｜P0A｜旧 L546–L547** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0546)

原标题路径：8. OwnershipBinding

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0546 -->
#### 8. OwnershipBinding


<!-- RONOVA-ORIGINAL-END orig-p0a-l0546 -->

<a id="orig-p0a-l0548"></a>

**迁入原文｜P0A｜旧 L548–L555** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0548)

原标题路径：8. OwnershipBinding → 8.1 目的

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0548 -->
#### 8.1 目的

Ownership 表示：

> 哪个逻辑 Owner 对某项状态具有 Authority 语义。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0548 -->

<a id="orig-p0a-l0556"></a>

**迁入原文｜P0A｜旧 L556–L569** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0556)

原标题路径：8. OwnershipBinding → 8.2 第一版 Scope

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0556 -->
#### 8.2 第一版 Scope

P0-A V1.3 只实现：

```text
SUBJECT_SELF
KNOWN_MANAGER
KNOWN_ROOT_OWNED_STATE
```

不要马上支持任意第三方 Manager graph。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0556 -->

<a id="orig-p0a-l0570"></a>

**迁入原文｜P0A｜旧 L570–L584** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0570)

原标题路径：8. OwnershipBinding → 8.3 数据结构

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0570 -->
#### 8.3 数据结构

```java
public record OwnershipBinding(
        SubjectKey subjectKey,
        StateDomainId stateDomain,
        AuthorityOwnerKey ownerKey,
        OwnershipRevision revision,
        OwnershipState state,
        RelationshipValidationState validationState
) {}
```

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0570 -->

<a id="orig-p0a-l0585"></a>

**迁入原文｜P0A｜旧 L585–L605** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0585)

原标题路径：8. OwnershipBinding → 8.4 OwnershipState

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0585 -->
#### 8.4 OwnershipState

```text
UNBOUND
PROVISIONAL
BOUND
SUSPENDED
STALE
REVOKED
```

只有：

```text
BOUND
```

可参与强 Authority 语义。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0585 -->

<a id="orig-msc-l0108"></a>

**迁入原文｜MSC｜旧 L108–L118** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-msc-l0108)

原标题路径：Ronova × MS 要求 合规与加强方案 V1.0 → 1. 要求清单与现状判定 → 1.8 规范实例与状态不变量

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-msc-l0108 -->
#### 1.8 规范实例与状态不变量

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-H1 | 唯一**规范实例**作恢复来源 | [`entitydefence:58-60`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#保留一个规范实例) | 有 `PhysicalIncarnationTracker`/`AdmissionPass`，但语义不同 | ⚠️ |
| R-H2 | 5 条规则（首占 / 构造与客户端同步不得替换 / **每个注册表接收同一对象** / 有意替换走显式生命周期 / 引用缺失不得造占位） | [`entitydefence:62-68`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#规范实例规则) | 无对应规则；Ronova 允许"later incarnation may represent the same identity" | ❌ **C6** |
| R-H3 | 通过正常生命周期初始化；内部集合修改**只用于修复** | [`entitydefence:88-90`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#通过正常生命周期初始化) | `TerminalIndexRepair` 只做摘除；补回走 `TopologyContinuityModule` | ✅ |
| R-H4 | 不能把 A 发为规范却把字段相同的 B 注册进世界 | [`entitydefence:101`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#通过正常生命周期初始化) | `bindingRevision` + `PhysicalIncarnationId.nonce` 可检测 | ⚠️ 未明文化 |
| R-I1 | 修复前状态不变量 7 项 | [`entitydefence:72-86`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复前的状态不变量) | `TerminalStateSink` 只断言已登记 raw 字段 | ⚠️ **缺**旧坐标/旧旋转、包围盒、摔落 |
| R-I2 | 注册表修复只能恢复成员关系，不能让矛盾状态变安全 | [`entitydefence:86`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复前的状态不变量) | `TerminalIndexRepair` 不碰生命周期方法 → 一致 | ✅ |


<!-- RONOVA-ORIGINAL-END orig-msc-l0108 -->

<a id="orig-msc-l0269"></a>

**迁入原文｜MSC｜旧 L269–L279** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-msc-l0269)

原标题路径：Ronova × MS 要求 合规与加强方案 V1.0 → 3. 阶段设计 → M3 —— 规范实例与状态不变量（C6、R-H1~H4、R-I1）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-msc-l0269 -->
#### M3 —— 规范实例与状态不变量（C6、R-H1~H4、R-I1）

**入口**：M2 完成。

| 交付物 | 内容 |
|---|---|
| 规范实例契约 | 5 条规则；与 `SubjectKey`/`bindingRevision`/`PhysicalIncarnationId` 对齐；明确 Ronova 的"新 incarnation 代表同一身份"与 MS"不得新建字段相同对象"的调和规则 |
| 状态不变量 7 项 | 补齐旧坐标/旧旋转、包围盒、摔落状态；作为修复前置检查 |

**验收**：每条不变量有独立探测；违反时不进入注册表修复。


<!-- RONOVA-ORIGINAL-END orig-msc-l0269 -->

<a id="orig-rule-l0538"></a>

**迁入原文｜RULE｜旧 L538–L542** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rule-l0538)

原标题路径：Ronova MS 规则内加强方案 V2.0 → 4. 缺口清单（按 §0.4 分层） → N9 —— 规范实例与修复契约（R46/R47/R49/R50/R51）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-rule-l0538 -->
#### N9 —— 规范实例与修复契约（R46/R47/R49/R50/R51）

[`entitydefence:62-128`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#规范实例规则)、`:201-213`。重点是**注册表写入 hook 与读取 hook 作为修复触发点**——
Ronova 目前以周期修复为主。


<!-- RONOVA-ORIGINAL-END orig-rule-l0538 -->

<a id="orig-ds-l0099"></a>

**迁入原文｜DS｜旧 L99–L100** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0099)

原标题路径：Ronova 设计规范 · 合并导读 → 3. 身份与修订合同（`CF` §3–§4）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0099 -->
#### 3. 身份与修订合同（`CF` [§3](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0211)–[§4](MS-SKILLS-PLAN.md#orig-cf-l0373)）


<!-- RONOVA-ORIGINAL-END orig-ds-l0099 -->

<a id="ms-entitydefence-l62"></a>

### 规范实例规则

**框架来源：** [MS entitydefence，原 L62–L71](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#规范实例规则)。

**编排说明（非原条款）：** 保留 A*、ID-*、SubjectIdentity、PhysicalIncarnationId、各 revision 与显式 rebind 原文；同名 ID 不跨来源合并。

<a id="orig-bl-l0638"></a>

**迁入原文｜BL｜旧 L638–L669** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0638)

原标题路径：6. Identity + State Ownership → 6.1 Identity 不再等于 UUID

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0638 -->
#### 6.1 Identity 不再等于 UUID

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


<!-- RONOVA-ORIGINAL-END orig-bl-l0638 -->

<a id="orig-v26-l1665"></a>

**迁入原文｜V26｜旧 L1665–L1684** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-v26-l1665)

原标题路径：Ronova 7.16+ JVM 极限强度加强总纲 V2.6（Internal Root） → 9. 攻击平面 → 9.5 TargetEpoch 资格代际 → 9.5.1 Canonical identity 与 revision 对照

<!-- RONOVA-ORIGINAL-BEGIN orig-v26-l1665 -->
#### 9.5.1 Canonical identity 与 revision 对照

| 概念 | Canonical scope / 来源 | 禁止混用 |
|---|---|---|
| `SubjectKey` | `(RealmId, subjectId)`，唯一逻辑身份与 durable FK | UUID、类名、dimension、producer 不直接替代身份 |
| `targetIdentity` | 本稿旧描述字段；规范编码仅保存 `SubjectKey`，其他内容归 metadata | 不保留第二个独立可写 target identity |
| `lineageId` | Realm-qualified 的关系/来源索引，带 resolver/evidence revision，解析为已绑定 SubjectKey 集合 | lineage 命中不自动授权整组主体；不充当第二 Tombstone 主键 |
| `PhysicalIncarnationId` | 复用 L6E-0 的 SubjectKey-qualified 物理实例标识 | 新对象、同 UUID 不继承旧资格 |
| `BindingRevision` | Root 发放，per SubjectKey，实际 binding 改变时递增 | 不等于 TargetEpoch，不因普通终结无条件伪造 rebind |
| `TargetEpoch` | Root 发放，per SubjectKey 的资格代际，保存在同一主体权威记录的版本化扩展 | 不用 lineage graph 本地计数，不与 DominionEpoch/BindingRevision 数值比较 |
| `OwnershipRevision` | Root 发放，per SubjectKey × StateDomainId | 不能用 TargetEpoch 代替域 owner 校验 |
| `JudgmentRevision / AdmissionTokenRevision` | 同一 SubjectKey 内对应判决与准入资格的版本 | 不用客户端或 executor 本地版本签发资格 |
| `DominionEpoch / CommitIndex` | Realm 全局事务/持久序列，沿用 L6 Master D4/D9 | 不因 batch 有 N 个主体推进 N 次 |

改变资格的 rebind 必须在同一事务同时推进该主体的 BindingRevision 与 TargetEpoch；普通终结/解除只推进实际改变的域版本，不强制让所有计数相等。metadata 更新本身不推进资格。恢复照抄已提交版本，不重新分配 epoch；计数溢出拒绝提交并诊断，不绕回 0。

resolver 先把候选 incarnation/lineage 解析到 SubjectKey，再按主体逐项校验授权与受保护状态。关联收集、骑乘或相同 producer 不能跳过 `canJudge` 等授权检查；ProvenRelated 也不能替代主体权限。解析未决可按候选规则临时限制相关出口，但不得靠猜 SubjectKey 创建持久资格或把不确定映射当成迁移成功。

r53 的 SubjectIdentity 等迁移输入按 L6 Master D11 演进，必须有 schema/映射来源/冲突测试。历史 Tombstone、解除目标和命中集合保持原语义；同一输入映射冲突时 fence 对应未决操作，不能随机分配新身份、合并主体或扩大 Tombstone 范围。


<!-- RONOVA-ORIGINAL-END orig-v26-l1665 -->

<a id="orig-cf-l0213"></a>

**迁入原文｜CF｜旧 L213–L225** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0213)

原标题路径：3. Canonical Identifier Model → 3.1 RealmId

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0213 -->
#### 3.1 RealmId

表示 Authority 所属逻辑 realm。

要求：

- 全局稳定；
- 不从当前 Java object identity 推导；
- 不与 Minecraft dimension id 混用；
- 可跨重启与 world/store lineage 持久化；不要求跨进程。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0213 -->

<a id="orig-cf-l0226"></a>

**迁入原文｜CF｜旧 L226–L257** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0226)

原标题路径：3. Canonical Identifier Model → 3.2 SubjectKey 与 SubjectIdentity

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0226 -->
#### 3.2 SubjectKey 与 SubjectIdentity

`SubjectKey` 是全系统唯一允许用作 equality、hash key、Root lookup 与 durable foreign key 的逻辑主体键：

```text
SubjectKey
├ realmId       RealmId
└ subjectId     UUID / 128-bit stable id
```

`SubjectIdentity` 是以 `SubjectKey` 为锚点的不可变描述，不是第二个相等键：

```text
SubjectIdentity
├ key                    SubjectKey
├ subjectType            SubjectType
└ metadata
    ├ originDescriptor?
    ├ identitySchemaVersion
    └ provenance?
```

冻结语义：

- identity equality / hash / lookup **只使用 `SubjectKey`**；
- `originDescriptor`、schema version、dimension、坐标、runtime class 与 provenance 都是 metadata/evidence，不参与身份相等；
- Minecraft UUID 可作为 `subjectId` 来源证据，但只有 Root binding 才确立完整 `SubjectKey`；
- identity 一经 durable binding，不因 metadata、物理 incarnation、reload 或 dimension transfer 改变；
- 当前源码 `dev.ronova.coremod.contract.SubjectIdentity` 必须迁移/适配到该模型，不允许在新包并行维护第二套 logical identity。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0226 -->

<a id="orig-cf-l0258"></a>

**迁入原文｜CF｜旧 L258–L281** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0258)

原标题路径：3. Canonical Identifier Model → 3.3 PhysicalIncarnationId

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0258 -->
#### 3.3 PhysicalIncarnationId

表示一个具体物理宿主实例。

要求：

- 同一 Subject 可有多个 incarnation 历史；
- 当前时刻只能有符合 Policy 的 active incarnation；
- incarnation 不继承旧 incarnation 的 execution authority；
- stale incarnation 永远不能靠同 UUID 自动恢复 Authority。

最小语义：

```text
PhysicalIncarnationId
├ subjectKey
├ incarnationSequence
└ incarnationNonce / generation
```

`PhysicalIncarnationId` 必须 realm-qualified；任何只接收 incarnation 的 lookup 都能从其中取得完整 `SubjectKey`，不得依赖调用方另传 Realm。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0258 -->

<a id="orig-cf-l0282"></a>

**迁入原文｜CF｜旧 L282–L305** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0282)

原标题路径：3. Canonical Identifier Model → 3.4 BindingRevision / OwnershipRevision

**规范关系调整：** 本段的旧位次/冻结不可覆盖规则仅保留作来源记录，不再阻止显式修订；技术不变量本轮未据此删除。参见 [当前文档效力](README.md#scope-governance)。

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0282 -->
#### 3.4 BindingRevision / OwnershipRevision

两类 revision 不得混用：

```text
BindingRevision
= SubjectKey ↔ PhysicalIncarnationId 的 Root-issued revision
= 单调 per SubjectKey

OwnershipRevision
= SubjectKey × StateDomainId ↔ Authority Owner 的 Root-issued revision
= 单调 per (SubjectKey, StateDomainId)
```

要求：

- revision 只在自身 scope 内比较；不同 Subject 或不同 Domain 的数字大小没有语义；
- 旧 revision 不能覆盖同一 scope 的新 revision；
- stale binding 不参与 authoritative execution；
- incarnation rebind 必须产生新 `BindingRevision`；ownership rebind/revoke 必须产生新 `OwnershipRevision`；
- TERMINAL 后出现的新 incarnation 不能通过旧 binding 恢复。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0282 -->

<a id="orig-p0a-l0094"></a>

**迁入原文｜P0A｜旧 L94–L104** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0094)

原标题路径：2. P0-A 核心不变量 → A1 — Subject Identity Stability

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0094 -->
#### A1 — Subject Identity Stability

```text
Logical Subject Identity
≠ Java Object Identity
```

物理 Entity 重建、reload、dimension transfer、manager replacement 不得自动改变 `SubjectKey`。`SubjectIdentity` metadata 可以随新证据更新，但不参与 equality/hash/Root lookup。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0094 -->

<a id="orig-p0a-l0105"></a>

**迁入原文｜P0A｜旧 L105–L120** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0105)

原标题路径：2. P0-A 核心不变量 → A2 — Incarnation Non-Inheritance

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0105 -->
#### A2 — Incarnation Non-Inheritance

新的 PhysicalIncarnation：

```text
不得自动继承旧 Incarnation 的 execution authority。
```

旧 incarnation 一旦 stale：

```text
永远不能仅靠 UUID 相同恢复 Authority。
```

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0105 -->

<a id="orig-p0a-l0121"></a>

**迁入原文｜P0A｜旧 L121–L130** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0121)

原标题路径：2. P0-A 核心不变量 → A3 — Binding Monotonicity

**规范关系调整：** 本段的旧位次/冻结不可覆盖规则仅保留作来源记录，不再阻止显式修订；技术不变量本轮未据此删除。参见 [当前文档效力](README.md#scope-governance)。

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0121 -->
#### A3 — Binding Monotonicity

```text
BindingRevision(new) > BindingRevision(old)
```

旧 binding 不得覆盖新 binding。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0121 -->

<a id="orig-p0a-l0312"></a>

**迁入原文｜P0A｜旧 L312–L313** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0312)

原标题路径：4. SubjectKey / SubjectIdentity

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0312 -->
#### 4. SubjectKey / SubjectIdentity


<!-- RONOVA-ORIGINAL-END orig-p0a-l0312 -->

<a id="orig-p0a-l0314"></a>

**迁入原文｜P0A｜旧 L314–L343** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0314)

原标题路径：4. SubjectKey / SubjectIdentity → 4.1 数据结构

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0314 -->
#### 4.1 数据结构

唯一 logical equality key：

```java
public record SubjectKey(
        RealmId realmId,
        UUID subjectId
) {}

public record SubjectIdentity(
        SubjectKey key,
        SubjectType subjectType,
        String originDescriptor,
        int identitySchemaVersion,
        Provenance provenance
) {}
```

冻结规则：

- 所有 `equals/hashCode`、Map key、Root primary/foreign key 与 command target **只使用 `SubjectKey`**；
- `SubjectIdentity` 是 immutable metadata descriptor；`originDescriptor`、schema version、registry/runtime class、dimension/position 和 provenance 不参与逻辑身份相等；
- 当前源码 record 中的这些观察字段必须通过 migration adapter 转成 metadata，不允许继续依赖 record 全字段 equality；
- `subjectId` 可以来自 Minecraft UUID，但：

> 不能仅凭 `entity.getUUID()` 自动宣告完整 Logical Subject Identity。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0314 -->

<a id="orig-p0a-l0344"></a>

**迁入原文｜P0A｜旧 L344–L366** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0344)

原标题路径：4. SubjectKey / SubjectIdentity → 4.2 SubjectType

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0344 -->
#### 4.2 SubjectType

第一版只需：

```text
ENTITY
PLAYER
NON_ENTITY_LOGICAL
```

不要在 P0-A V1.3 一次性扩展：

```text
BLOCK_ENTITY
REGION
WORLD
RENDER_OBJECT
```

这些留给后续 scope extension。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0344 -->

<a id="orig-p0a-l0367"></a>

**迁入原文｜P0A｜旧 L367–L394** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0367)

原标题路径：4. SubjectKey / SubjectIdentity → 4.3 Identity Resolution

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0367 -->
#### 4.3 Identity Resolution

第一版：

```text
Minecraft Entity
    ↓
IdentityCandidate
    ↓
Realm + UUID candidate + Type metadata
    ↓
Root lookup
    ↓
FOUND / NEW / CONFLICT
```

结果：

```text
IDENTITY_BOUND
IDENTITY_NEW_PENDING
IDENTITY_CONFLICT
IDENTITY_STALE
IDENTITY_TERMINAL
```

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0367 -->

<a id="orig-p0a-l0395"></a>

**迁入原文｜P0A｜旧 L395–L396** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0395)

原标题路径：5. PhysicalIncarnationId

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0395 -->
#### 5. PhysicalIncarnationId


<!-- RONOVA-ORIGINAL-END orig-p0a-l0395 -->

<a id="orig-p0a-l0397"></a>

**迁入原文｜P0A｜旧 L397–L414** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0397)

原标题路径：5. PhysicalIncarnationId → 5.1 目的

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0397 -->
#### 5.1 目的

把：

```text
Logical Subject
```

与：

```text
具体 Java object
```

彻底分开。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0397 -->

<a id="orig-p0a-l0415"></a>

**迁入原文｜P0A｜旧 L415–L433** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0415)

原标题路径：5. PhysicalIncarnationId → 5.2 推荐结构

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0415 -->
#### 5.2 推荐结构

```java
public record PhysicalIncarnationId(
        SubjectKey subjectKey,
        long incarnationSequence,
        long generationNonce
) {}
```

要求：

- `incarnationSequence` 单 Subject 单调；
- `generationNonce` 避免旧对象伪造新序号；
- `subjectKey` 使 incarnation realm-qualified，任何 lookup 都不得依赖调用方另传 Realm；
- 不使用 `System.identityHashCode()` 作为唯一 incarnation。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0415 -->

<a id="orig-p0a-l0460"></a>

**迁入原文｜P0A｜旧 L460–L461** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0460)

原标题路径：6. Incarnation Binding

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0460 -->
#### 6. Incarnation Binding


<!-- RONOVA-ORIGINAL-END orig-p0a-l0460 -->

<a id="orig-p0a-l0462"></a>

**迁入原文｜P0A｜旧 L462–L478** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0462)

原标题路径：6. Incarnation Binding → 6.1 核心记录

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0462 -->
#### 6.1 核心记录

```java
public record IncarnationBinding(
        SubjectKey subjectKey,
        PhysicalIncarnationId incarnation,
        BindingRevision revision,
        BindingState state,
        DominionEpoch committedEpoch,
        RootCommitIndex commitIndex
) {}
```

这是 Root/projection 中的 binding fact；`ProjectionRevision` 属于承载它的 `ProjectionView`，不得写进 durable binding 并形成第二个版本轴。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0462 -->

<a id="orig-p0a-l0479"></a>

**迁入原文｜P0A｜旧 L479–L497** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0479)

原标题路径：6. Incarnation Binding → 6.2 Bind 规则

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0479 -->
#### 6.2 Bind 规则

允许：

```text
UNBOUND → ACTIVE
ACTIVE(old) → ACTIVE(new) + SUPERSEDED(old)
```

禁止：

```text
STALE(old) → ACTIVE
TERMINAL subject → ACTIVE(new)
old BindingRevision → overwrite new
```

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0479 -->

<a id="orig-p0a-l0498"></a>

**迁入原文｜P0A｜旧 L498–L509** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0498)

原标题路径：6. Incarnation Binding → 6.3 Rebind

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0498 -->
#### 6.3 Rebind

Rebind 必须：

1. Root-authorized；
2. 产生新 BindingRevision；
3. 旧 incarnation 进入 SUPERSEDED/STALE；
4. Projection 原子发布；
5. P0-B/P0-C 观察到 revision 变化后才使用新 binding。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0498 -->

<a id="orig-p0a-l0510"></a>

**迁入原文｜P0A｜旧 L510–L545** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0510)

原标题路径：7. BindingRevision / OwnershipRevision

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0510 -->
#### 7. BindingRevision / OwnershipRevision

推荐：

```java
public record BindingRevision(long value)
        implements Comparable<BindingRevision> {}

public record OwnershipRevision(long value)
        implements Comparable<OwnershipRevision> {}
```

规则：

```text
0 = invalid/uninitialized
1..N = valid monotonic revision
```

比较只能：

```text
newer / equal / stale
```

禁止：

- wrap；
- reset；
- 复用旧 revision；
- 因 JVM restart 从 1 重新开始。

Revision scope 必须显式：`BindingRevision` 单调 per `SubjectKey`；`OwnershipRevision` 单调 per `(SubjectKey, StateDomainId)`。不同 scope 的数字不可比较，也不得用 `BindingRevision` 代替 ownership revision。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0510 -->

<a id="orig-p0a-l1002"></a>

**迁入原文｜P0A｜旧 L1002–L1024** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1002)

原标题路径：22. IdentityBindingService

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1002 -->
#### 22. IdentityBindingService

```java
public interface IdentityBindingService {
    AuthorityResult<BindingOutcome> discover(IncarnationCandidate candidate);
    AuthorityResult<BindingOutcome> bind(
            SubjectKey subjectKey,
            PhysicalIncarnationId incarnation
    );
    AuthorityResult<BindingOutcome> rebind(
            SubjectKey subjectKey,
            PhysicalIncarnationId newIncarnation
    );
    AuthorityResult<BindingOutcome> invalidate(
            PhysicalIncarnationId incarnation
    );
}
```

`BindingOutcome` 是封闭 data result（`BOUND | REBOUND | SUPERSEDED | TERMINAL_REJECT | HELD | NO_CHANGE`）并携带 resulting revisions/receipt；不是未定义占位符。Minecraft/Forge 类型只允许出现在 `*.minecraft` adapter，core API 使用 loader-neutral `IncarnationCandidate`，禁止 `Object` 与无边界反射扫描。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l1002 -->

<a id="orig-p0a-l1025"></a>

**迁入原文｜P0A｜旧 L1025–L1048** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1025)

原标题路径：23. Incarnation Discovery

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1025 -->
#### 23. Incarnation Discovery

第一版建议：

```text
Entity enters known lifecycle seam
    ↓
create IncarnationCandidate
    ↓
Identity lookup
    ↓
Root/Projection lookup
    ↓
Bind or quarantine
```

不要：

```text
定时扫描整个 JVM heap
```

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l1025 -->

<a id="orig-l6-l0359"></a>

**迁入原文｜L6｜旧 L359–L362** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-l6-l0359)

原标题路径：Ronova L6 Architecture Master Plan V2.4 — Internal Root → 2. 新模块树（V2 修订：C1/H2/H5/H7/M1 相关条目） → 2.3 关键模块职责再定义 → 2.3.2 "Dominion 自身实体"的身份来源（V2 新增，关闭 M1）

<!-- RONOVA-ORIGINAL-BEGIN orig-l6-l0359 -->
#### 2.3.2 "Dominion 自身实体"的身份来源（V2 新增，关闭 M1）

`OwnerImmunity` 的两类免疫对象各有明确身份来源：**owner** 来自 durable `OwnerRegistry`（UUID 集）；**Dominion 自身实体** 由无状态谓词判定——实体注册类型属于 Dominion 自身 registry namespace（即本 mod 自己注册的 EntityType）。该判定只引用自身 namespace，不涉及任何第三方标识，不违反红线 3。状态所有权见 [§3.2](MS-SKILLS-PLAN.md#orig-l6-l0391)。


<!-- RONOVA-ORIGINAL-END orig-l6-l0359 -->

<a id="orig-l6-l0619"></a>

**迁入原文｜L6｜旧 L619–L626** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-l6-l0619)

原标题路径：Ronova L6 Architecture Master Plan V2.4 — Internal Root → 5. FULL L5 架构与不变量（V2 修订：D3 writer 固定、M2 gate 消歧、H7 判据修正、D6 性能不变量） → 5.4 RealmId 与 store 绑定（V2 新增）

<!-- RONOVA-ORIGINAL-BEGIN orig-l6-l0619 -->
#### 5.4 RealmId 与 store 绑定（V2 新增）

`DurableRootStore` 自 7.15 起绑定 `RealmId`。在 Internal Root 路线中它用于 migration/shadow 对账、store 与 world lineage 关联以及 clone/fork 诊断；不再承担网络 session 语义。

`RealmId` **不自动解决 clone/fork ownership**。存档文件夹被复制后，两份分叉存档可能携带相同 `RealmId`；`fence_token` 只裁决 writer qualification / stale writer，不能单独判断哪一个分叉应继承原 Internal Root authoritative lineage。因此不得再以 `realm_id + fence` 宣称 clone ownership 已解决。无法唯一证明 realm ownership 时，mutation 必须 **FENCED / BLOCKED**；不得让两个 clone 共用可写 authoritative state、不得按“谁先连上”静默选 owner、不得产生两个可同时写入的 writer。具体 clone/fork arbitration mechanism 留到 L6 detailed engineering specification；本 Master Plan 只冻结上述 fail-closed 不变量。

---


<!-- RONOVA-ORIGINAL-END orig-l6-l0619 -->

<a id="orig-road-l0093"></a>

**迁入原文｜ROAD｜旧 L93–L101** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-road-l0093)

原标题路径：Ronova L6／L6E 全量加强路线 V1.0 — Internal Root → 3. L6E 四条加强链 → 3.1 P0-A：身份、状态归属与权威

<!-- RONOVA-ORIGINAL-BEGIN orig-road-l0093 -->
#### 3.1 P0-A：身份、状态归属与权威

- 以稳定 SubjectKey 表达目标身份，将元数据、实际对象与 incarnation 分开；通过显式迁移承接旧身份。
- 为状态域建立 OwnershipBinding 与版本；外部字段、记录、管理器和 Resolver 输出提供绑定证据，由 Root 作权威决定。
- 统一 ResolverProvider 的注册、版本、撤销、失效及冲突状态；多来源证据不能因相同 UUID 或相同数值直接合并。
- 原子提交策略、绑定、归属、目标代际及适用撤销；合法重新激活由明确 Authority 命令推进。
- 向执行层提供一致的不可变 AuthoritySnapshot；生命周期真值、解析状态、准入处置和 fence 分别表达。
- 接合崩溃恢复、未知命令回放和 revision 传播；该阶段交付执行、WriteSink、Lease 与 Completion 所需的权威合同。


<!-- RONOVA-ORIGINAL-END orig-road-l0093 -->

<a id="orig-ds-l0075"></a>

**迁入原文｜DS｜旧 L75–L98** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0075)

原标题路径：Ronova 设计规范 · 合并导读 → 2. 冻结词汇（`CONTEXT.md`） → 2.3 权威与免疫

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0075 -->
#### 2.3 权威与免疫

| 术语 | 定义 | 禁止混用 |
|---|---|---|
| **SubjectKey** | 相等、哈希、Root 查找与持久外键的**唯一稳定逻辑键**：`(RealmId, subjectId)`。运行时类、维度、位置、来源、schema 元数据**永不参与**相等 | entity UUID alone / 整记录相等 / Java 对象同一性 |
| **SubjectIdentity** | 由 SubjectKey 锚定的不可变元数据描述符。**不是第二个逻辑键** | identity key / physical incarnation / 可变快照 |
| **BindingRevision** | Root 签发的 `SubjectKey ↔ PhysicalIncarnationId` **单调**修订号，**仅同一 SubjectKey 内可比** | OwnershipRevision / ProjectionRevision / 全局序号 |
| **OwnershipRevision** | Root 签发的 `(SubjectKey, StateDomainId) ↔ AuthorityOwner` **单调**修订号，**仅该对之内可比** | BindingRevision / policy version |
| **ProjectionRevision** | 一条不可变本地投影谱系的版本。**永不替代**前两者，**必须**经单个 ProjectionView 消费 | RootCommitIndex / DominionEpoch / 单独读的缓存版本 |
| **PolicyRevision** | Root 授权的 policy/mode bundle 的单调版本（Realm policy lineage）。**旧称 `ModeRevision` 不再作为独立类型**（[`CF:319-321`](MS-MSITEM-PLAN.md#orig-cf-l0319)） | mode version 独立类型 |
| **LifecycleTruth** | 持久两态 `ACTIVE \| TERMINAL`。Resolution 不确定性与 admission quarantine 是**另外的轴**，**永不新增**生命周期状态 | UNKNOWN lifecycle / QUARANTINED lifecycle |
| **ResolutionState** | 本地权威解析的显式质量：resolved / partial / ambiguous / opaque / stale / invalid / unknown。**Unknown 永不等于 false 或 safe** | boolean result / Optional.empty |
| **ProjectionView** | 不可变句柄，把 projection revision、epoch、policy revision、fence state 与 entries **一起固定**，**不得提供「先读 revision 再另查 entry」的撕裂式 API**（[`CF:1334`](MS-SKILLS-PLAN.md#orig-cf-l1297)） | independent revision read / mutable cache |
| **RootCommandId** | 一条 Root 命令的稳定幂等键。**同 id + 同 canonical payload hash** = 一次可重试 mutation；**不同 hash = 命令冲突** | message sequence / commit index / 换新 id 重试 |
| **COMMIT_UNKNOWN** | 持久写入或回执返回歧义的未决结果。**只允许**用**同一 command id 与 payload hash** 恢复或幂等重发；既非已提交也非已拒绝 | failure / false / 换新命令自动重试 |
| **Owner Immunity** | 「owner 或 Dominion 自身化身在 quarantine 说反话时**仍保持公开存在**」这一不变量。属 Public Presence 策略，**不依赖调用方侧捷径** | authority bypass / raw visibility |
| **DominionEpoch** | `(generation, sequence)`。与 FenceToken、RootInstanceId、CommandSequence、commandId、commitIndex、realmId、各 revision、incarnationSequence **严格分离**（[`CF:383-394`](MS-SKILLS-PLAN.md#orig-cf-l0373)） | 单一 sequence 兼职 |
| **AuthorityResult\<T\>** | 封闭结果类型：`RESOLVED(T) / PARTIALLY_RESOLVED(T,evidence) / NOT_FOUND(verifiedAtRevision) / AMBIGUOUS / OPAQUE / STALE / INVALID / UNKNOWN / FENCED`。**不得用 `boolean` / `null` / `Optional.empty()` 承载**（[`CF:1332`](MS-SKILLS-PLAN.md#orig-cf-l1297)） | boolean result |

**三条词汇层硬约束**：身份只有一个键（`D100`）；三类 revision 各有 scope、不可互替；
`unknown` **永不**等价于 `false` 或 `safe`（[`CF:160-174`](MS-SKILLS-PLAN.md#orig-cf-l0160) R5）。

---


<!-- RONOVA-ORIGINAL-END orig-ds-l0075 -->

<a id="orig-ds-l0101"></a>

**迁入原文｜DS｜旧 L101–L109** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0101)

原标题路径：Ronova 设计规范 · 合并导读 → 3. 身份与修订合同（`CF` §3–§4） → 3.1 身份成员与排除（`CF:248-254`）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0101 -->
#### 3.1 身份成员与排除（[`CF:248-254`](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0226)）

- **只用 `SubjectKey`** 做 equality / hash / lookup；
- **不参与身份相等**：`originDescriptor`、schema version、dimension、坐标、runtime class、provenance；
- Minecraft UUID **只作为 `subjectId` 的来源证据**；**只有 Root binding 才确立完整 `SubjectKey`**；
- identity 一经 durable binding，**不因 metadata、物理 incarnation、reload 或 dimension transfer 改变**；
- **迁移强制**：现有 `dev.ronova.coremod.contract.SubjectIdentity` 必须迁移/适配，
  **不允许在新包并行维护第二套 logical identity**（与 `D11`、[`L6:69`](MS-SKILLS-PLAN.md#orig-l6-l0060) 一致）。


<!-- RONOVA-ORIGINAL-END orig-ds-l0101 -->

<a id="orig-ds-l0110"></a>

**迁入原文｜DS｜旧 L110–L114** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0110)

原标题路径：Ronova 设计规范 · 合并导读 → 3. 身份与修订合同（`CF` §3–§4） → 3.2 `RealmId`（`CF:213-222`）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0110 -->
#### 3.2 `RealmId`（[`CF:213-222`](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0213)）

全局稳定；**不从 Java object identity 推导**；**不与 Minecraft dimension id 混用**；
可跨重启与 world/store lineage 持久化；**不要求跨进程**。


<!-- RONOVA-ORIGINAL-END orig-ds-l0110 -->

<a id="orig-ds-l0115"></a>

**迁入原文｜DS｜旧 L115–L124** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0115)

原标题路径：Ronova 设计规范 · 合并导读 → 3. 身份与修订合同（`CF` §3–§4） → 3.3 `PhysicalIncarnationId`（`CF:258-278`）—— **不是身份**

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0115 -->
#### 3.3 `PhysicalIncarnationId`（[`CF:258-278`](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0258)）—— **不是身份**

| 规则 | 内容 |
|---|---|
| 构成 | `subjectKey + incarnationSequence + incarnationNonce/generation` |
| 多副本 | 同一 Subject 可有多个 incarnation 历史；**当前时刻只能有一个符合 Policy 的 active incarnation** |
| 权限 | incarnation **不继承**旧 incarnation 的 execution authority |
| 复活 | **stale incarnation 永远不能靠同 UUID 自动恢复 Authority** |
| 限定 | **必须 realm-qualified**；任何只接收 incarnation 的 lookup 都能从其中取得完整 `SubjectKey`，**不得依赖调用方另传 Realm** |


<!-- RONOVA-ORIGINAL-END orig-ds-l0115 -->

<a id="orig-ds-l0125"></a>

**迁入原文｜DS｜旧 L125–L133** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0125)

原标题路径：Ronova 设计规范 · 合并导读 → 3. 身份与修订合同（`CF` §3–§4） → 3.4 三个 revision 的规则（`CF:284-302`）

**规范关系调整：** 本段的旧位次/冻结不可覆盖规则仅保留作来源记录，不再阻止显式修订；技术不变量本轮未据此删除。参见 [当前文档效力](README.md#scope-governance)。

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0125 -->
#### 3.4 三个 revision 的规则（[`CF:284-302`](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0282)）

- 两类 revision **不得混用**；
- **只在自身 scope 内比较**；不同 Subject 或不同 Domain 的数字大小**没有语义**；
- **旧 revision 不能覆盖同一 scope 的新 revision**；
- stale binding **不参与** authoritative execution；
- incarnation rebind **必须产生新 `BindingRevision`**；ownership rebind/revoke **必须产生新 `OwnershipRevision`**；
- **TERMINAL 后出现的新 incarnation 不能通过旧 binding 恢复**。


<!-- RONOVA-ORIGINAL-END orig-ds-l0125 -->

<a id="ms-entitydefence-l72"></a>

### 修复前的状态不变量

**框架来源：** [MS entitydefence，原 L72–L87](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复前的状态不变量)。

**编排说明（非原条款）：** 修复前有效状态与生命周期状态分离保留。MS 七项物理状态检查在原规划有缺口登记，不因迁移视为完整实现。

**关联原条款：** [RULE · 闸门 6 —— 状态与修复闸](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0198)；[MSC · 1.8 规范实例与状态不变量](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0108)。

<a id="orig-cf-l0907"></a>

**迁入原文｜CF｜旧 L907–L934** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0907)

原标题路径：14. Liveness Contract → 14.3 CriticalInvariant Contract

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0907 -->
#### 14.3 CriticalInvariant Contract

```text
CriticalInvariant
├ invariantId
├ domain
├ priority
├ deadlineClass
├ maxOutstandingWork
├ convergenceCostClass
├ overloadPolicy
└ failureEscalation
```

deadline 可以是语义 deadline：

```text
BEFORE_PUBLICATION
BEFORE_NEXT_CONSUMPTION
WITHIN_ONE_TICK
BOUNDED_TICKS(N)
BEST_EFFORT
```

优先使用语义 deadline，不硬编码所有场景为 tick 数。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0907 -->

<a id="orig-p0a-l0434"></a>

**迁入原文｜P0A｜旧 L434–L459** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0434)

原标题路径：5. PhysicalIncarnationId → 5.3 Incarnation 状态

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0434 -->
#### 5.3 Incarnation 状态

本节只描述物理 binding 状态，不承载 durable lifecycle truth：

```text
DISCOVERED | PENDING_BIND | ACTIVE | SUPERSEDED | STALE | RELEASED
```

状态转换：

```text
DISCOVERED
    ↓
PENDING_BIND
    ↓
ACTIVE
    ↓
SUPERSEDED
    ↓
STALE
    ↓
RELEASED
```

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0434 -->

<a id="ms-entitydefence-l88"></a>

## 通过正常生命周期初始化

**框架来源：** [MS entitydefence，原 L88–L102](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#通过正常生命周期初始化)。

**编排说明（非原条款）：** MS 要求首次正常生成与异常修复分开；原规范实例/生命周期汇总是对应来源，未出现独立长章节不等于可以凭空造新实体。

**关联原条款：** [MSC · 1.8 规范实例与状态不变量](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0108)；[MSC · M3 —— 规范实例与状态不变量（C6、R-H1~H4、R-I1）](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0269)。

本节点没有被单独搬入的原文段落；它保留 MS 的独立职责入口。上列关联条款或下级章节不等于本节点所有细节已经覆盖；适用性与缺口见映射表。

<a id="ms-entitydefence-l103"></a>

## 构建幂等修复过程

**框架来源：** [MS entitydefence，原 L103–L116](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#构建幂等修复过程)。

**编排说明（非原条款）：** 承接幂等、重入、偏差累计、预算及修复后信任恢复。写回成功不等于验证或信任恢复。

<a id="orig-bl-l2074"></a>

**迁入原文｜BL｜旧 L2074–L2090** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l2074)

原标题路径：19. Resource Resilience → 19.1 State-driven Enforcement

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l2074 -->
#### 19.1 State-driven Enforcement

攻击者一 tick 改 health 10000 次：

错误：

```text
10000 writes
→ 10000 repair tasks
```

正确原则不是“无脑只保留最后一次”，而是：

> **Coalesce work, not security evidence.**

昂贵的 repair work 可以合并，但对语义重要的 transition / extrema / violation 不能静默丢失。


<!-- RONOVA-ORIGINAL-END orig-bl-l2074 -->

<a id="orig-bl-l2091"></a>

**迁入原文｜BL｜旧 L2091–L2129** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l2091)

原标题路径：19. Resource Resilience → 19.1 State-driven Enforcement → 19.1.1 DeviationSemantics

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l2091 -->
#### 19.1.1 DeviationSemantics

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


<!-- RONOVA-ORIGINAL-END orig-bl-l2091 -->

<a id="orig-bl-l2458"></a>

**迁入原文｜BL｜旧 L2458–L2498** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l2458)

原标题路径：23. 支持性安全增强：保留但不升格为核心轴 → 23.4 Entity Physical State Snapshot / Rollback：仅限受约束恢复辅助

**规范关系调整：** 本段的旧位次/冻结不可覆盖规则仅保留作来源记录，不再阻止显式修订；技术不变量本轮未据此删除。参见 [当前文档效力](README.md#scope-governance)。

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l2458 -->
#### 23.4 Entity Physical State Snapshot / Rollback：仅限受约束恢复辅助

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


<!-- RONOVA-ORIGINAL-END orig-bl-l2458 -->

<a id="orig-cf-l0858"></a>

**迁入原文｜CF｜旧 L858–L875** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0858)

原标题路径：14. Liveness Contract

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0858 -->
#### 14. Liveness Contract

L6E 不承诺：

> 任意负载下永远在 Δ tick 内完成所有 convergence。

冻结成：

```text
Within Declared Operating Envelope
→ bounded convergence guarantee

Outside Envelope
→ explicit degradation / fence / fail-stop
```

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0858 -->

<a id="orig-cf-l0876"></a>

**迁入原文｜CF｜旧 L876–L887** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0876)

原标题路径：14. Liveness Contract → 14.1 Liveness States

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0876 -->
#### 14.1 Liveness States

```text
NORMAL
→ DEGRADED
→ STORM
→ CRITICAL_LIVENESS_UNPROVABLE
→ FENCED / FAIL_STOP
```

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0876 -->

<a id="orig-cf-l0888"></a>

**迁入原文｜CF｜旧 L888–L906** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0888)

原标题路径：14. Liveness Contract → 14.2 Liveness Safety Rule

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0888 -->
#### 14.2 Liveness Safety Rule

预算不足时可以停止：

- audit；
- diagnostics；
- low-priority reconciliation；
- non-critical projection maintenance。

不能静默停止：

- Root / fence processing；
- Tombstone awareness；
- Identity safety；
- Admission safety；
- critical Presence revoke。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0888 -->

<a id="orig-cf-l0935"></a>

**迁入原文｜CF｜旧 L935–L964** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0935)

原标题路径：15. DeviationAccumulator Contract

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0935 -->
#### 15. DeviationAccumulator Contract

冻结语义：

```text
LATEST_STATE
EXTREMA_LATCH
EDGE_LATCH
COUNT_RATE
BOUNDED_TRANSITION_LOG
```

核心规则：

> **Coalesce work, not security evidence.**

允许：

```text
10000 writes
→ 1 repair job
```

不允许：

```text
非法 death transition
→ 因最终 snapshot 恢复而丢证据
```


<!-- RONOVA-ORIGINAL-END orig-cf-l0935 -->

<a id="orig-cf-l0965"></a>

**迁入原文｜CF｜旧 L965–L978** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0965)

原标题路径：15. DeviationAccumulator Contract → 15.1 Required Operations

**规范关系调整：** 本段的旧位次/冻结不可覆盖规则仅保留作来源记录，不再阻止显式修订；技术不变量本轮未据此删除。参见 [当前文档效力](README.md#scope-governance)。

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0965 -->
#### 15.1 Required Operations

```text
update(observation, sequence, monotonicTime)
peek()       → immutable summary; does not consume
consume()    → atomic summary-and-reset for consumable modes
reset(reason, authorizedRevision)
```

- `update` 必须按 domain-local sequence 去重；旧 sequence 不得覆盖新证据；
- `peek` 与 `consume` 返回 accumulator revision、first/last sequence、overflow state；
- `reset` 只能由 successful reconciliation、explicit policy reset 或 lifecycle archive 触发并记 audit；
- 并发 `consume` 必须 linearizable，同一证据不能被消费两次。


<!-- RONOVA-ORIGINAL-END orig-cf-l0965 -->

<a id="orig-cf-l0979"></a>

**迁入原文｜CF｜旧 L979–L988** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0979)

原标题路径：15. DeviationAccumulator Contract → 15.2 Exact Mode Semantics

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0979 -->
#### 15.2 Exact Mode Semantics

| mode | update | consume/reset 语义 |
|---|---|---|
| `LATEST_STATE` | 保存最高 sequence 的完整 immutable observation | `consume` 返回当前值并清空；用于可由最终状态完整表达的偏差 |
| `EXTREMA_LATCH` | 保存窗口内 min/max 及首次越界证据 | 未成功 reconcile 前不得因值恢复正常而清除 extrema |
| `EDGE_LATCH` | 保存每种 security-relevant edge 的首次/末次 sequence 与计数 | edge 至少被一次成功 consume 才可清除 |
| `COUNT_RATE` | 按固定 monotonic time bucket 计数；不使用 wall-clock 回拨 | 返回窗口 counts/rate；过期 bucket 只能按 policy 窗口淘汰 |
| `BOUNDED_TRANSITION_LOG` | 按 sequence 保存转移记录 | 满容量后不静默覆盖 security evidence；进入 overflow 摘要并触发 degrade/fence policy |


<!-- RONOVA-ORIGINAL-END orig-cf-l0979 -->

<a id="orig-cf-l0989"></a>

**迁入原文｜CF｜旧 L989–L1010** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0989)

原标题路径：15. DeviationAccumulator Contract → 15.3 Bounds and Overflow

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0989 -->
#### 15.3 Bounds and Overflow

每个 accumulator 必须声明 `maxEntries`、`maxBytes`、`maxAge` 和 `overflowPolicy`。允许的 overflow 结果只有：

```text
COALESCE_WITH_SUMMARY
DROP_NON_CRITICAL_WITH_COUNTER
DEGRADE_DOMAIN
FENCE_DOMAIN
```

禁止无计数覆盖、无界增长和把 overflow 解释为“无偏差”。最低 domain 默认值：

所有计数器采用 saturating arithmetic；达到上限时置 `counterSaturated=true` 并进入声明的 overflow policy，禁止 long wrap 成负数或零。

- lifecycle / terminal / binding / ownership：`EDGE_LATCH` 或 `BOUNDED_TRANSITION_LOG`，overflow 至少 `FENCE_DOMAIN`；
- vitals extrema：`EXTREMA_LATCH`，overflow 至少 `DEGRADE_DOMAIN`；
- ordinary projection drift：`LATEST_STATE`；
- rate telemetry：`COUNT_RATE`，只允许丢弃过期 bucket，不得影响独立的 security latch。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l0989 -->

<a id="orig-cf-l1567"></a>

**迁入原文｜CF｜旧 L1567–L1576** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1567)

原标题路径：29. Freeze Decision Summary → ID-1 — Liveness

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1567 -->
#### ID-1 — Liveness

证明重点是：

> **有界工作区间内保证收敛；超载时保证安全降级。**

不是声称无限负载下永远 Δ tick 收敛。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1567 -->

<a id="orig-p0a-l1362"></a>

**迁入原文｜P0A｜旧 L1362–L1384** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1362)

原标题路径：36. P0-A 与 P0-C 的边界

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1362 -->
#### 36. P0-A 与 P0-C 的边界

P0-C 可以消费：

```text
Desired lifecycle truth
Active incarnation
Ownership bindings
Mode
Projection revision
```

P0-C 负责：

- physical reassert；
- admission enforcement；
- terminal sink；
- reconstitution。

P0-A 不执行 Minecraft physical mutation。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l1362 -->

<a id="orig-road-l0111"></a>

**迁入原文｜ROAD｜旧 L111–L120** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-road-l0111)

原标题路径：Ronova L6／L6E 全量加强路线 V1.0 — Internal Root → 3. L6E 四条加强链 → 3.3 P0-C：多来源状态、恢复链与持续结果

<!-- RONOVA-ORIGINAL-BEGIN orig-road-l0111 -->
#### 3.3 P0-C：多来源状态、恢复链与持续结果

- 建立真实生命模型，覆盖登记字段、同步数据、附加生命、计算状态和实体外维护源；联写后独立检查反弹。
- 将来源追踪扩展到 SavedData、静态管理器、独立快照、非 NBT producer、新 UUID、新身体及任务后继。
- 按具体记录和当前绑定处置恢复源，检查正在运行的任务和晚到结果；相关 chunk ticket、索引与资源分别释放。
- 实际确认持久保存结果，重启后重新解析对象和来源，继续未完成义务；内存删除与保存完成分别记录。
- 攻击侧完成状态、索引、恢复出口、追踪和显示的适用结果；防护侧恢复 Owner 生命、生命周期、世界关系、连接和显示。
- 处理合法重生、换维度、资格解除和重新激活；新 incarnation 使用当前资格，旧身体与旧 epoch 后继不能借恢复回流。
- 维持稳定完成窗口，来源变化、证据失效或再次反弹时重新评估；死亡、掉落和奖励仍按原效果身份处理。


<!-- RONOVA-ORIGINAL-END orig-road-l0111 -->

<a id="orig-rule-l0198"></a>

**迁入原文｜RULE｜旧 L198–L214** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rule-l0198)

原标题路径：Ronova MS 规则内加强方案 V2.0 → 1. 规则基（L0 铁律 + L1 参考库） → 闸门 6 —— 状态与修复闸

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-rule-l0198 -->
#### 闸门 6 —— 状态与修复闸

| ID | 规则 | 依据 |
|---|---|---|
| **R45** | `ERASED` 与 `PROTECTED` 不能同时成立；冲突时必须有确定优先级 | [`msitem:87`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md#总体架构) |
| **R46** | 规范实例 5 条规则：首个有效实例占据规范引用 / 普通构造与客户端同步不能替换 / 每个被修复的注册表接收**同一个**对象 / 有意替换走显式生命周期 / 规范引用缺失时不造占位对象 | [`entitydefence:62-68`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#规范实例规则) |
| **R47** | 修复前 7 项状态不变量：运行时 ID 与 UUID、实体类型、坐标/方块/区块坐标、包围盒、旧坐标与旧旋转、已加入/已移除/可更新标记、生命与死亡等瞬态状态 | [`entitydefence:72-86`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复前的状态不变量) |
| **R48** | 修复过程必须幂等；重入时只做维持不变量所需的最小修正 | [`entitydefence:113`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#构建幂等修复过程)、[`entitydefence:115`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#构建幂等修复过程) |
| **R49** | 修复点至少有六类：初次构造/正常生成、实体自身 tick、独立服务端 tick、**注册表写入 hook**、**注册表读取 hook**、客户端 Level 与渲染 hook | [`entitydefence:119-128`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复时机) |
| **R50** | 读取路径必须认识规范实体，关闭「被移除后、下次修复前」的时间窗 | [`entitydefence:201-211`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#防御读取路径) |
| **R51** | 构造查询结果时按对象身份或规范运行时 ID 去重 | [`entitydefence:213`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#防御读取路径) |
| **R52** | 全局列表独占过滤必须使用**独立、明确的开关**，不能藏在基础伤害或死亡防御里；启用前必须记录 5 条后果 | [`entitydefence:40`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#划定策略范围)、[`entitydefence:228-236`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#可选实体列表独占防御) |
| **R53** | 只在确实需要时取消方法；有返回值的方法必须显式写回调返回值；`void` 方法设置返回值没有意义 | [`skills:255`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#3-实体与生物方法层) |
| **R54** | 不冻结所有同步字段（会破坏装备、位置和动画同步）；只保护必须稳定的字段 | [`skills:265`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#4-synchedentitydata-层)、[`entitydefence:250`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#实体状态也必须防御) |
| **R55** | 周期修复必须幂等，避免每 tick 反复重建集合、刷网络包或重复注册实体 | [`skills:271`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#5-周期修复层) |
| **R56** | 防御是五层而非一层：统一分类、前置阻断、状态视图修正、同步阻断、周期修复 | [`skills:224`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#防御链) |


<!-- RONOVA-ORIGINAL-END orig-rule-l0198 -->

<a id="orig-ds-l0548"></a>

**迁入原文｜DS｜旧 L548–L560** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0548)

原标题路径：Ronova 设计规范 · 合并导读 → 6. 执行与宿主边界 → 6.6 活性与降级（`CF:858-903`、`CF:989-1007`）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0548 -->
#### 6.6 活性与降级（[`CF:858-903`](RESTRUCTURE-MAP.md#range-cf-858-903)、[`CF:989-1007`](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0989)）

- 状态：`NORMAL → DEGRADED → STORM → CRITICAL_LIVENESS_UNPROVABLE → FENCED / FAIL_STOP`；
- **可以**停：audit、diagnostics、低优先 reconciliation、非关键投影维护；
- **必须不停**：Root / fence 处理、Tombstone awareness、Identity safety、Admission safety、critical Presence revoke；
- 每个 accumulator 必须声明 `maxEntries` / `maxBytes` / `maxAge` / `overflowPolicy`；
  允许的溢出结果**只有** `COALESCE_WITH_SUMMARY` / `DROP_NON_CRITICAL_WITH_COUNTER` /
  `DEGRADE_DOMAIN` / `FENCE_DOMAIN`；
  **禁止**无计数覆盖、无界增长和把 overflow 解释为「无偏差」；
  计数器用 **saturating arithmetic** + `counterSaturated=true`，**禁止 long wrap 成负数或零**。

---


<!-- RONOVA-ORIGINAL-END orig-ds-l0548 -->

<a id="ms-entitydefence-l117"></a>

### 修复时机

**框架来源：** [MS entitydefence，原 L117–L131](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复时机)。

**编排说明（非原条款）：** 自实体 tick、独立服务端 tick、注册表读写与客户端时机分别保留；原要求里尚未闭合的双侧时序由缺口清单承接。

**关联原条款：** [MSC · 1.10 读取路径兜底与修复时机](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0132)。

<a id="orig-bl-l2130"></a>

**迁入原文｜BL｜旧 L2130–L2175** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l2130)

原标题路径：19. Resource Resilience → 19.1 State-driven Enforcement → 19.1.2 Raw Observation 不是“必然看到所有瞬态”的魔法

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l2130 -->
#### 19.1.2 Raw Observation 不是“必然看到所有瞬态”的魔法

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


<!-- RONOVA-ORIGINAL-END orig-bl-l2130 -->

<a id="ms-entitydefence-l132"></a>

## 修复服务端注册表

**框架来源：** [MS entitydefence，原 L132–L144](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复服务端注册表)。

**编排说明（非原条款）：** 原服务端/客户端表族汇总逐字保留；细分的分区、ID/UUID、tick、生命周期及追踪节点通过下列关联条款进入，不拆坏原表格。

<a id="orig-msc-l0119"></a>

**迁入原文｜MSC｜旧 L119–L131** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-msc-l0119)

原标题路径：Ronova × MS 要求 合规与加强方案 V1.0 → 1. 要求清单与现状判定 → 1.9 服务端与客户端注册表

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-msc-l0119 -->
#### 1.9 服务端与客户端注册表

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-J1 | 6 张服务端表**分别**可修 | [`entitydefence:136-143`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复服务端注册表) | accessor 齐（`EntityLookup`/`EntityTickList`/`EntitySection*`/`ClassInstanceMultiMap`/`PersistentEntityManager`）；`TerminalIndexRepair` 按 `Indexes` 分组 | ⚠️ **`ChunkMap` 追踪器独立修复未见** |
| R-J2 | 分区存储的完整视图与按类视图都可见；不改正在遍历的集合 | [`entitydefence:147-149`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#分区存储) | `RegisteredIntIndexSlots`（"视图是瞬态的，绝不跨修改保留迭代器"） | ✅ 强于要求 |
| R-J3 | **ID 与 UUID 修复是一个事务** | [`entitydefence:153`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#id-与-uuid-查询) | `TerminalIndexRepair` 有 `Key`/`Group`，但未见"事务"语义明文 | ⚠️ |
| R-J4 | 陈旧同键对象须经显式切换替换，不能共存 | [`entitydefence:155`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#id-与-uuid-查询) | `ConflictingPhysicalBinding` 检测 + `bindingRevision` | ✅ |
| R-J5 | 恢复真实缺失，**不无限重放生命周期回调**；执行前检查"缺失→存在"转换 | [`entitydefence:172`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#持久管理与生命周期) | `LifecycleIntegrationReceipt` ONCE + `TerminalLifecycle` planned 路径不重放 | ✅ **强于要求** |
| R-J6 | 追踪器只在缺失或陈旧时创建/替换；只刷新该追踪的玩家 | [`entitydefence:176-182`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#玩家追踪) | `AuthorityRuntimeState` anchor/connection 弱绑定 + 追踪门 | ⚠️ 部分 |
| R-K1 | 6 条客户端路径修复 | [`entitydefence:188-195`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复客户端注册表) | `ClientRecoveryController`（含补回客户端实体表）+ `ClientLevelAccessor` | ⚠️ 部分 |
| R-K2 | 客户端收敛到服务端权威；**不建纯客户端重复对象** | [`entitydefence:197`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复客户端注册表) | 无纯客户端实体创建 | ✅ |


<!-- RONOVA-ORIGINAL-END orig-msc-l0119 -->

<a id="orig-msc-l0280"></a>

**迁入原文｜MSC｜旧 L280–L293** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-msc-l0280)

原标题路径：Ronova × MS 要求 合规与加强方案 V1.0 → 3. 阶段设计 → M4 —— 注册表独立修复与读写窗口（C7、R-J1~J6、R-L1/L2、R-M4/M5/M6）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-msc-l0280 -->
#### M4 —— 注册表独立修复与读写窗口（C7、R-J1~J6、R-L1/L2、R-M4/M5/M6）

**入口**：M3 完成。

| 交付物 | 内容 |
|---|---|
| 逐表可修 | 六张服务端表分别验证可恢复（含 **`ChunkMap` 追踪器独立修复**） |
| ID/UUID 事务 | 两者作为一个事务提交，避免单侧恢复 |
| 读写 hook | 注册表**写入 hook** 与**读取 hook** 作为修复触发点 |
| 读侧兜底 | 7 个查询族：对 `PROTECTED` 保证"一定看得见"，对 `ERASED` 保证"一定看不见" |
| 去重 | 结果构造按对象身份/规范运行时 ID 去重 |

**验收**：[`entitydefence:286-298`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#验证矩阵) 的 11 项逐项通过；重复修复不产生重复实体/条目/追踪器/tick/渲染。


<!-- RONOVA-ORIGINAL-END orig-msc-l0280 -->

<a id="ms-entitydefence-l145"></a>

### 分区存储

**框架来源：** [MS entitydefence，原 L145–L150](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#分区存储)。

**编排说明（非原条款）：** 对应完整实例视图、按类视图与迭代安全；原 R-J2 / 内部集合汇总保留为引用落点。

**关联原条款：** [MSC · 1.9 服务端与客户端注册表](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119)；[MSC · 1.11 幂等与内部集合](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0145)。

本节点没有被单独搬入的原文段落；它保留 MS 的独立职责入口。上列关联条款或下级章节不等于本节点所有细节已经覆盖；适用性与缺口见映射表。

<a id="ms-entitydefence-l151"></a>

### ID 与 UUID 查询

**框架来源：** [MS entitydefence，原 L151–L156](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#id-与-uuid-查询)。

**编排说明（非原条款）：** MS 的 ID/UUID 成员关系事务不等同于 Root 的逻辑 JudgmentBatch 原子提交。原 R-J3 的独立缺口不可用 D9 已有来冒充关闭。

**文档缺口：** [GAP-MAP-03 · ID/UUID 原子修复及 active/passive 保持](RESTRUCTURE-MAP.md#gap-map-03)。

**关联原条款：** [MSC · 1.9 服务端与客户端注册表](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119)；[P0A · 6.1 核心记录](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0462)。

本节点没有被单独搬入的原文段落；它保留 MS 的独立职责入口。上列关联条款或下级章节不等于本节点所有细节已经覆盖；适用性与缺口见映射表。

<a id="ms-entitydefence-l157"></a>

### Tick 集合

**框架来源：** [MS entitydefence，原 L157–L167](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#tick-集合)。

**编排说明（非原条款）：** 对应 active/passive 与当前迭代快照。原规划只有汇总的部分必须明确为待细化，而不是把每 tick 维护当成交换语义已验证。

**关联原条款：** [MSC · 1.11 幂等与内部集合](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0145)；[MSC · M4 —— 注册表独立修复与读写窗口（C7、R-J1~J6、R-L1/L2、R-M4/M5/M6）](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0280)。

本节点没有被单独搬入的原文段落；它保留 MS 的独立职责入口。上列关联条款或下级章节不等于本节点所有细节已经覆盖；适用性与缺口见映射表。

<a id="ms-entitydefence-l168"></a>

### 持久管理与生命周期

**框架来源：** [MS entitydefence，原 L168–L173](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#持久管理与生命周期)。

**编排说明（非原条款）：** 承接准入、绑定与追踪/tick 生命周期规则；初始发布、异常补回和有意替换须保留原义。

<a id="orig-cf-l0641"></a>

**迁入原文｜CF｜旧 L641–L665** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0641)

原标题路径：9. Pre-Admission Binding Contract

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0641 -->
#### 9. Pre-Admission Binding Contract

冻结 admission 状态机：

```text
DISCOVERED
→ PREBOUND
→ ACTIVE

DISCOVERED | PREBOUND → HELD
DISCOVERED | PREBOUND | HELD → CANCELLED
ACTIVE → EMERGENCY_RECONCILE → ACTIVE | HELD | CANCELLED
```

`ACTIVE` 只表示允许进入本地 presence path，不改写 durable lifecycle truth。

| transition | 唯一授权者 / 输入 | revision 要求 | 失败结果 |
|---|---|---|---|
| `DISCOVERED → PREBOUND` | P0-A Binding coordinator + current ProjectionView | subject/incarnation、BindingRevision、Epoch、FenceState 必须同一 view 且 current | `HELD` / `CANCELLED` |
| `PREBOUND → ACTIVE` | Admission/Presence gate | 与 PREBOUND 相同的 revisions；若 projection 前进必须重验证 | `HELD` |
| `DISCOVERED/PREBOUND → HELD` | explicit UNKNOWN/STALE/FENCED/policy result | 保存 observed revisions 与 reason；不自增 Root revision | `HELD` |
| `* → CANCELLED` | durable TERMINAL 或 bounded policy decision | 不创造 lifecycle truth；记录触发它的 projection/policy revision | `CANCELLED` |
| `ACTIVE → EMERGENCY_RECONCILE` | 检测到 bypass/stale binding 的 seam adapter | 立即撤销 full presence qualification | `HELD` / `CANCELLED` |
| `EMERGENCY_RECONCILE → ACTIVE` | P0-A 在新 immutable view 上完成 rebind/revalidation | 必须使用新 current BindingRevision/OwnershipRevision | 否则保持 `HELD` |


<!-- RONOVA-ORIGINAL-END orig-cf-l0641 -->

<a id="orig-cf-l0666"></a>

**迁入原文｜CF｜旧 L666–L671** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0666)

原标题路径：9. Pre-Admission Binding Contract → 9.1 Admission Levels

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0666 -->
#### 9.1 Admission Levels

- `HARD_PREBIND`：在物理对象进入世界容器前完成 `PREBOUND`；
- `PRE_PRESENCE_BIND`：对象可暂存于隔离容器，但在 tracking / publish 前完成 `PREBOUND`；
- `POST_ADMISSION_EMERGENCY_RECONCILE`：只用于无法命中前两类 seam 的既存对象，初始为 `EMERGENCY_RECONCILE`，不自动获得 full presence。


<!-- RONOVA-ORIGINAL-END orig-cf-l0666 -->

<a id="orig-cf-l0672"></a>

**迁入原文｜CF｜旧 L672–L681** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0672)

原标题路径：9. Pre-Admission Binding Contract → 9.2 Forge Seam Mapping

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0672 -->
#### 9.2 Forge Seam Mapping

| Forge 语义 seam | 最低要求 | Root/Projection 规则 |
|---|---|---|
| NBT / chunk load | `HARD_PREBIND` 优先 | 只用本地 immutable ProjectionView 判定；缺 binding 时 HELD |
| normal spawn / join | `PRE_PRESENCE_BIND` | publish/tracking 前必须 PREBOUND |
| command summon | `PRE_PRESENCE_BIND` | 不得因命令来源跳过 authority |
| dimension transfer | 原 SubjectKey + 新 Incarnation rebind | 旧 incarnation 在新 binding commit 后立即 stale |
| third-party direct add | emergency seam | 先隔离为 EMERGENCY_RECONCILE，再绑定或取消 |


<!-- RONOVA-ORIGINAL-END orig-cf-l0672 -->

<a id="orig-cf-l0682"></a>

**迁入原文｜CF｜旧 L682–L691** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l0682)

原标题路径：9. Pre-Admission Binding Contract → 9.3 Root Unavailable / Fenced

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l0682 -->
#### 9.3 Root Unavailable / Fenced

admission hot path 禁止同步执行 durable Root I/O，只允许消费当前 immutable `ProjectionView`：

- current binding 且未 fenced → 可 PREBOUND；
- projection `STALE / UNKNOWN` → HELD；
- durable TERMINAL → CANCELLED；
- Internal Root component 不可用或 durable store fenced → 不产生新 durable truth；
- Root 恢复后异步 refresh/reconcile，不在 Forge callback 内阻塞持久化。


<!-- RONOVA-ORIGINAL-END orig-cf-l0682 -->

<a id="orig-p0a-l1049"></a>

**迁入原文｜P0A｜旧 L1049–L1069** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1049)

原标题路径：24. Pre-Admission Binding

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1049 -->
#### 24. Pre-Admission Binding

直接实现 [L6E-0 V1.3 §9](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l0641) 的状态机：

```text
DISCOVERED → PREBOUND → ACTIVE
DISCOVERED | PREBOUND → HELD
DISCOVERED | PREBOUND | HELD → CANCELLED
ACTIVE → EMERGENCY_RECONCILE → ACTIVE | HELD | CANCELLED
```

Forge seam 映射必须覆盖：NBT/chunk load、normal spawn、command summon、dimension transfer、third-party direct add。实现规则：

- hot path 只读一个 `ProjectionView`，禁止同步 durable Root I/O；
- current binding 可 PREBOUND；STALE/UNKNOWN/缺 binding 必须 HELD；TERMINAL 必须 CANCELLED；
- emergency path 先隔离再异步 reconcile，不能先 publish 后补票；
- hold budget 超时按 Policy cancel/fence，并产生可聚合 telemetry；
- P0-A 输出 binding/admission result，P0-C 执行物理移除或 presence sink。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l1049 -->

<a id="ms-entitydefence-l174"></a>

### 玩家追踪

**框架来源：** [MS entitydefence，原 L174–L183](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#玩家追踪)。

**编排说明（非原条款）：** 对应缺失/陈旧追踪器替换、同一规范对象与应追踪玩家集合。MS 的独立追踪修复要求在旧稿里仍有缺口记录。

**文档缺口：** [GAP-MAP-04 · 服务端追踪与客户端六路径独立修复](RESTRUCTURE-MAP.md#gap-map-04)。

**关联原条款：** [MSC · 1.9 服务端与客户端注册表](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119)；[MSC · M4 —— 注册表独立修复与读写窗口（C7、R-J1~J6、R-L1/L2、R-M4/M5/M6）](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0280)。

本节点没有被单独搬入的原文段落；它保留 MS 的独立职责入口。上列关联条款或下级章节不等于本节点所有细节已经覆盖；适用性与缺口见映射表。

<a id="ms-entitydefence-l184"></a>

## 修复客户端注册表

**框架来源：** [MS entitydefence，原 L184–L198](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复客户端注册表)。

**编排说明（非原条款）：** 承接客户端存储、tick、查询及渲染与服务端的收敛；原客户端扩展不等于已能控制所有客户端像素路径。

**关联原条款：** [MSC · 1.9 服务端与客户端注册表](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119)。

<a id="orig-bl-l1939"></a>

**迁入原文｜BL｜旧 L1939–L1968** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l1939)

原标题路径：17. Render Boundary + Native Render Firewall → 17.2 Optional Client Authority Extension

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l1939 -->
#### 17.2 Optional Client Authority Extension

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


<!-- RONOVA-ORIGINAL-END orig-bl-l1939 -->

<a id="ms-entitydefence-l199"></a>

## 防御读取路径

**框架来源：** [MS entitydefence，原 L199–L214](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#防御读取路径)。

**编排说明（非原条款）：** 保留 Raw Observation / Public Presence 单向边界，并登记 PROTECTED 读兜底与 ERASED 过滤方向不同；原七族查询缺口不被重构掩盖。

**文档缺口：** [GAP-MAP-05 · 七族读取兜底与六类修复触发点](RESTRUCTURE-MAP.md#gap-map-05)。

<a id="orig-bl-l0181"></a>

**迁入原文｜BL｜旧 L181–L208** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l0181)

原标题路径：2. 后续所有加强必须遵守的硬边界 → 2.1 Raw Observation 与 Presence 永久单向分离

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l0181 -->
#### 2.1 Raw Observation 与 Presence 永久单向分离

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


<!-- RONOVA-ORIGINAL-END orig-bl-l0181 -->

<a id="orig-p0a-l0933"></a>

**迁入原文｜P0A｜旧 L933–L954** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l0933)

原标题路径：19. ProjectionView

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l0933 -->
#### 19. ProjectionView

```java
public interface ProjectionView {
    ProjectionRevision revision();
    DominionEpoch epoch();
    RootCommitIndex commitIndex();
    PolicyRevision policyRevision();
    FenceState fenceState();
    AuthorityResult<SubjectProjectionEntry> find(SubjectKey subjectKey);
    AuthorityResult<SubjectProjectionEntry> findByIncarnation(PhysicalIncarnationId incarnation);
}

public interface ProjectionService {
    ProjectionView currentView();
}
```

`currentView()` 一次取得 immutable handle；entry、epoch、policy、fence 与 revision 都从同一 handle 读取。禁止先调用 `revision()`、再从另一个可变引用查 entry 的 torn-read API。P0-B/P0-C 必须只通过该只读 handle 消费。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l0933 -->

<a id="orig-l6-l0351"></a>

**迁入原文｜L6｜旧 L351–L358** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-l6-l0351)

原标题路径：Ronova L6 Architecture Master Plan V2.4 — Internal Root → 2. 新模块树（V2 修订：C1/H2/H5/H7/M1 相关条目） → 2.3 关键模块职责再定义 → 2.3.1 Presence 保证范围（V2 新增，关闭 H6）

<!-- RONOVA-ORIGINAL-BEGIN orig-l6-l0351 -->
#### 2.3.1 Presence 保证范围（V2 新增，关闭 H6）

删除 `patchExternal*`（针对任意第三方类的字节码改写）后，presence 的强制面收窄。V2 显式声明边界：

- **强保证范围**：Dominion 自身控制的 Minecraft / Forge 通用 observation、query、tracking、rendering sink——即 Dominion 注入自身核心 gate 的入口、Mixin seam、Forge 通用查询/同步/渲染路径。L5-INV-4 的 `public=false` 判据按此口径验收。
- **不保证范围**：第三方自行创建并维护、绕过 Minecraft/Forge 通用入口的私有 collection、自定义 manager、独立 cache、直接对象引用读取。对这些路径**不通过**第三方 Mod ID / class name / entity name / bytecode patch 建立特殊适配（红线 3）。
- 该收窄是红线 3（禁止第三方特判）与需求 4（禁止堆砌）的必然结果，不是缺陷；文档与测试口径一致按此书写，避免"测试按窄口径通过、文档按宽口径宣称"。


<!-- RONOVA-ORIGINAL-END orig-l6-l0351 -->

<a id="orig-l6-l0363"></a>

**迁入原文｜L6｜旧 L363–L368** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-l6-l0363)

原标题路径：Ronova L6 Architecture Master Plan V2.4 — Internal Root → 2. 新模块树（V2 修订：C1/H2/H5/H7/M1 相关条目） → 2.4 QuarantineView 拆分（保留 V1，依据 CONFIRMED `QuarantineView.java:76-114`）

<!-- RONOVA-ORIGINAL-BEGIN orig-l6-l0363 -->
#### 2.4 QuarantineView 拆分（保留 V1，依据 CONFIRMED `QuarantineView.java:76-114`）

现 `QuarantineView.matches` 在单类里承载 server 路径（`SERVER_OBJECTS` raw + admission projection）与 client 路径（`CLIENT_MARKS` lineage + `CLIENT_PRESENCE`），两套状态与逻辑完全不同。拆为 `ServerQuarantineView` 与 `ClientQuarantineView`，共享 `presence.PresencePolicy.decide` 作为最终裁决点。【V2】拆分后两路对保护对象的判定一律改调 `AuthorityView`（纯查询）；immunity 不再依赖此处短路，而由 `PresencePolicy` 自身保证（[§1.5](MS-SKILLS-PLAN.md#orig-l6-l0184)、H7）。

---


<!-- RONOVA-ORIGINAL-END orig-l6-l0363 -->

<a id="orig-l6-l1389"></a>

**迁入原文｜L6｜旧 L1389–L1397** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-l6-l1389)

原标题路径：最终清单 → 6. Raw Observation / Presence dependency DAG（摘要）

<!-- RONOVA-ORIGINAL-BEGIN orig-l6-l1389 -->
#### 6. Raw Observation / Presence dependency DAG（摘要）

```
RawObservationSource ──▶ TerminalWatch / JudgmentEngine / AdmissionGate   （raw 直连，不经 presence）
ProjectionBundle ──▶ AuthorityView ──▶ JudgmentEngine / PresencePolicy    （保护判定走纯查询）
PresencePolicy(+OwnerImmunity) ──▶ 外部 query/render/tracking sink        （仅对外；无任何边指向 raw/discovery）
编译期：observation ⊬ presence；presence/discovery ⊬ AuthorityBinder；persistence ⊬ integrity
```


<!-- RONOVA-ORIGINAL-END orig-l6-l1389 -->

<a id="orig-msc-l0132"></a>

**迁入原文｜MSC｜旧 L132–L144** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-msc-l0132)

原标题路径：Ronova × MS 要求 合规与加强方案 V1.0 → 1. 要求清单与现状判定 → 1.10 读取路径兜底与修复时机

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-msc-l0132 -->
#### 1.10 读取路径兜底与修复时机

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-L1 | 7 个查询族在底层陈旧时返回规范实体 | [`entitydefence:203-211`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#防御读取路径) | presence 投影过滤（**方向相反**：让读看不见被隔离对象） | ⚠️ 对 ERASED ✅ / 对 PROTECTED ❌ |
| R-L2 | 构造结果时按对象身份或规范运行时 ID **去重** | [`entitydefence:213`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#防御读取路径) | `TopologyIndex.related` 用 `IdentityHashMap` Set | ✅ |
| R-M1 | 修复时机：初次构造与正常生成 | [`entitydefence:123`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复时机) | `SpawnAdmissionRuntime.observeConstruction` | ✅ |
| R-M2 | 受保护实体自身 tick | [`entitydefence:124`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复时机) | `DominionEvents.onPlayerTick` | ✅ |
| R-M3 | 独立服务端 tick | [`entitydefence:125`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复时机) | `RuntimeCoordinator.serverTickHead` | ✅ |
| R-M4 | **注册表写入 hook** | [`entitydefence:126`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复时机) | 无 | ❌ **C7** |
| R-M5 | **注册表读取 hook** | [`entitydefence:127`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复时机) | 无（只靠 presence 投影） | ❌ **C7** |
| R-M6 | 客户端 Level 与渲染 hook | [`entitydefence:128`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#修复时机) | `MinecraftScreenGuardMixin` + `RenderLivingEvent` | ⚠️ 部分 |


<!-- RONOVA-ORIGINAL-END orig-msc-l0132 -->

<a id="orig-ds-l0057"></a>

**迁入原文｜DS｜旧 L57–L64** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0057)

原标题路径：Ronova 设计规范 · 合并导读 → 2. 冻结词汇（`CONTEXT.md`） → 2.1 观察与可见性

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0057 -->
#### 2.1 观察与可见性

| 术语 | 定义 | 禁止混用 |
|---|---|---|
| **Raw Observation** | Dominion 内部「物理化身存在及其拓扑关系」的知识。**永不**被 Judgment 状态、quarantine、Public Presence 或 owner 状态过滤 | visible entities / active targets / unfiltered presence |
| **Public Presence** | 物理化身是否经 Dominion 控制的外部查询、渲染或追踪路径暴露。**可隐藏**被隔离对象，**不能改变 Raw Observation** | observation / existence / loaded state |
| **Physical Incarnation** | 某身份/lineage 在某一时刻的一个具体运行时实体对象。其消失**不擦除**持久 Judgment 真相 | tombstone / judgment record |


<!-- RONOVA-ORIGINAL-END orig-ds-l0057 -->

<a id="orig-ds-l0256"></a>

**迁入原文｜DS｜旧 L256–L264** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-ds-l0256)

原标题路径：Ronova 设计规范 · 合并导读 → 4. 权威与状态所有权 → 4.7 Presence 保证范围（`L6:351-357`，关闭 H6）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-ds-l0256 -->
#### 4.7 Presence 保证范围（[`L6:351-357`](MS-ENTITYDEFENCE-PLAN.md#orig-l6-l0351)，关闭 H6）

**强保证**：Dominion 控制的 MC/Forge 通用 observation/query/tracking/rendering sink。
**不保证**：第三方自行创建维护、绕过 MC/Forge 通用入口的私有 collection、自定义 manager、
独立 cache、直接对象引用读取。
**且不得**对这些路径通过第三方 Mod ID / class name / entity name / bytecode patch 建立特殊适配（[`L6:355`](MS-ENTITYDEFENCE-PLAN.md#orig-l6-l0351) 红线 3）。

---


<!-- RONOVA-ORIGINAL-END orig-ds-l0256 -->

<a id="ms-entitydefence-l215"></a>

## 可选：实体列表独占防御

**框架来源：** [MS entitydefence，原 L215–L237](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#可选实体列表独占防御)。

**编排说明（非原条款）：** 该章是可选能力，不因“MS 有”自动开启，也不能把 Ronova 自身终结目标过滤误称为全局独占。旧稿相反判断同时保留并标记来源/时点。

**文档缺口：** [GAP-MAP-06 · 独占列表、终结过滤与瞬态抑制的边界](RESTRUCTURE-MAP.md#gap-map-06)。

<a id="orig-msc-l0153"></a>

**迁入原文｜MSC｜旧 L153–L160** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-msc-l0153)

原标题路径：Ronova × MS 要求 合规与加强方案 V1.0 → 1. 要求清单与现状判定 → 1.12 独占列表防御

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-msc-l0153 -->
#### 1.12 独占列表防御

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-O1 | 独占过滤必须是**独立、明确的开关**，不藏在基础防御里 | [`entitydefence:236`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#可选实体列表独占防御) | **presence 隔离默认开启**，无独立开关 | ❌ **C3** |
| R-O2 | 启用前记录 5 条后果 | [`entitydefence:228-234`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#可选实体列表独占防御) | 无记录 | ❌ **C3** |
| R-O3 | 默认值是"完整列表 + 自我修复" | [`entitydefence:236`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#可选实体列表独占防御) | 默认是过滤 | ❌ **C3** |


<!-- RONOVA-ORIGINAL-END orig-msc-l0153 -->

<a id="orig-rule-l0512"></a>

**迁入原文｜RULE｜旧 L512–L537** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-rule-l0512)

原标题路径：Ronova MS 规则内加强方案 V2.0 → 4. 缺口清单（按 §0.4 分层） → N8 —— 独占过滤：开关归属待确认 + 5 条后果未记录（R52）

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-rule-l0512 -->
#### N8 —— 独占过滤：开关归属待确认 + 5 条后果未记录（R52）

[`entitydefence:40`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#划定策略范围) 与 `:236` 两处强调「不要把保护一个实体和让全局查询不再返回其他实体混成一项策略」，
并要求**独立、明确的开关**，且启用前记录 5 条后果（`:228-234`）。

已核实的事实（本轮修正：原判「缺独立开关」不准确）：

- Ronova **没有**对全局实体列表做独占过滤。最接近的两种行为都不是独占：
  - `agent/BootstrapHooks.java:115-153 filterQuarantinedResult` 只剔除 **Ronova 自己的终结目标**，
    这是 R63（`say @e` 中必须没有）**要求**的行为，不涉及其他 Mod 的实体；
  - `agent/BootstrapHooks.java:95-100 shouldSuppressRender` 的第二分支是
    `!safeEntity(entity) && shouldSuppressExternalVisual()`，而
    `RonovaDominion.java:37` 安装的是 `Entity.class::isInstance` ⇒ 该分支只对**根本不是 Entity 的参数**生效，
    **不是**"抑制其他 Mod 的实体"。
    其开关由 `client/ClientRuntime.java:27` 安装为
    `RECOVERY::isAuthorityRecoveryActive`——**仅客户端、仅在 Authority 恢复期间**的瞬态抑制。
- `presence/PresenceSplitFlags.java` 是一个独立、有 `Source` 溯源的显式开关
  （`PROPERTY = "ronova.714c.presenceSplit.enabled"`、`ENVIRONMENT = "RONOVA_714C_PRESENCE_SPLIT_ENABLED"`，
  `:32-73` 解析，默认 `true`），但它切的是 **7.14C 的服务端/客户端判定分半**
  （见 `engine/QuarantineView.java:16-26`），**不是**独占列表过滤。两者不要混为一谈。
  该开关只能通过 JVM 参数或环境变量切换，即需要启动配置——与 R60 的取向不符。
- [`entitydefence:228-234`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#可选实体列表独占防御) 的 5 条后果**未在任何地方记录**。

⇒ N8 的整改是**确认 + 声明**，不是新建开关：确认是否存在任何形式的列表收窄，
并为已存在的瞬态抑制记录后果与开关语义。条目见 [§5](MS-SKILLS-PLAN.md#orig-rule-l0730) 的 C8。


<!-- RONOVA-ORIGINAL-END orig-rule-l0512 -->

<a id="ms-entitydefence-l238"></a>

## 实体状态也必须防御

**框架来源：** [MS entitydefence，原 L238–L253](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#实体状态也必须防御)。

**编排说明（非原条款）：** 状态防御与成员关系防御分别定位；生命、移除、运动、接触、装备/效果及可见性的项目扩展继续保留。

<a id="orig-bl-l1089"></a>

**迁入原文｜BL｜旧 L1089–L1105** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l1089)

原标题路径：9. VITALS Enhanced — 通用改血加强 → 9.6 VITALS 与 Owner Protection

**规范关系调整：** 本段的旧位次/冻结不可覆盖规则仅保留作来源记录，不再阻止显式修订；技术不变量本轮未据此删除。参见 [当前文档效力](README.md#scope-governance)。

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l1089 -->
#### 9.6 VITALS 与 Owner Protection

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


<!-- RONOVA-ORIGINAL-END orig-bl-l1089 -->

<a id="orig-bl-l2242"></a>

**迁入原文｜BL｜旧 L2242–L2245** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l2242)

原标题路径：20. Movement / Equipment / Effect Domain 加强

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l2242 -->
#### 20. Movement / Equipment / Effect Domain 加强

这些保留为 Enforcement Domain，不升级成独立 Root。


<!-- RONOVA-ORIGINAL-END orig-bl-l2242 -->

<a id="orig-bl-l2246"></a>

**迁入原文｜BL｜旧 L2246–L2259** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l2246)

原标题路径：20. Movement / Equipment / Effect Domain 加强 → 20.1 Movement

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l2246 -->
#### 20.1 Movement

可表达：

- FREEZE；
- TELEPORT_BLOCK；
- knockback constraints；
- region / path constraint；
- flight / movement desired state。

统一走 Observation → Invariant → Reassert。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l2246 -->

<a id="orig-bl-l2260"></a>

**迁入原文｜BL｜旧 L2260–L2275** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l2260)

原标题路径：20. Movement / Equipment / Effect Domain 加强 → 20.2 Equipment

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l2260 -->
#### 20.2 Equipment

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


<!-- RONOVA-ORIGINAL-END orig-bl-l2260 -->

<a id="orig-bl-l2276"></a>

**迁入原文｜BL｜旧 L2276–L2289** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l2276)

原标题路径：20. Movement / Equipment / Effect Domain 加强 → 20.3 Effect

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l2276 -->
#### 20.3 Effect

可表达：

- FORCE_EFFECT；
- REMOVE_EFFECT；
- EFFECT_IMMUNITY；
- CLEAR；
- level / duration constraint。

仍然遵守单一 Root Truth。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l2276 -->

<a id="ms-entitydefence-l254"></a>

## 从零实现顺序

**框架来源：** [MS entitydefence，原 L254–L267](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#从零实现顺序)。

**编排说明（非原条款）：** MS 从零顺序与 Ronova 既有阶段并列说明职责，不重新宣布前置步骤完成或覆盖原依赖。

<a id="orig-cf-l1362"></a>

**迁入原文｜CF｜旧 L1362–L1399** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1362)

原标题路径：27. P0-A First Implementation Scope

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1362 -->
#### 27. P0-A First Implementation Scope

P0-A 第一版只实现：

```text
Subject
↔ Physical Incarnation
↔ Known Authoritative Owner
```

以及：

```text
Identity
Binding
Revision
Ownership
Terminal-aware rebind rules
Projection publication
```

**不在第一版实现全量 Relationship Graph。**

后续再逐步扩展：

```text
Manager
Capability
Controller
BackingStore
PhaseState
Opaque / Related Holder
```

避免 Resolver 系统在 Identity 模型稳定前膨胀。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1362 -->

<a id="ms-entitydefence-l268"></a>

## 内部集合规则

**框架来源：** [MS entitydefence，原 L268–L281](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#内部集合规则)。

**编排说明（非原条款）：** 保留快照、身份匹配、交换、回调与客户端/服务端分离的原条款；不可只以幂等词汇代替每一张表的集合契约。

<a id="orig-msc-l0145"></a>

**迁入原文｜MSC｜旧 L145–L152** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-msc-l0145)

原标题路径：Ronova × MS 要求 合规与加强方案 V1.0 → 1. 要求清单与现状判定 → 1.11 幂等与内部集合

**原稿定位说明：** 原“已并入/已取代/派生导读”等状态也随文保留；迁移不把该稿恢复成另一份并行最高规范，更不刷新其实现判断。

<!-- RONOVA-ORIGINAL-BEGIN orig-msc-l0145 -->
#### 1.11 幂等与内部集合

| ID | 要求 | 出处 | Ronova 现状 | 判定 |
|---|---|---|---|---|
| R-N1 | 修复幂等：结构正确时不新建/不重复/不重发/不重复回调 | [`entitydefence:113`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#构建幂等修复过程) | `TerminalStateSink` 幂等 + 写后全量复验 | ✅ |
| R-N2 | 重入保护 | [`entitydefence:115`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#构建幂等修复过程) | `CapabilityRuntime` ThreadLocal `repairing` + `ClientRecoveryGuard` | ✅ |
| R-N3 | 内部集合规则 9 条（按 ID/UUID/身份匹配；迭代敏感集合先快照；保留 active/passive 语义；条目缺失不新建替代对象；不重复添加按类成员关系；无真实状态切换不重放回调；追踪器缺失或指向错误才重建；客户端服务端分别验证；按类别记录修复） | [`entitydefence:272-280`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#内部集合规则) | 前 8 条基本满足；**第 9 条"按类别记录修复"部分满足**（`policyCounters` 只记策略计数） | ⚠️ |


<!-- RONOVA-ORIGINAL-END orig-msc-l0145 -->

<a id="ms-entitydefence-l282"></a>

## 验证矩阵

**框架来源：** [MS entitydefence，原 L282–L299](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#验证矩阵)。

**编排说明（非原条款）：** 原 T-*、U-*、I-* 等测试逐条保留，MS 11 项注册表破坏/修复矩阵作为待对接的验证范围；本轮未运行这些测试。

<a id="orig-bl-l2703"></a>

**迁入原文｜BL｜旧 L2703–L2713** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-bl-l2703)

原标题路径：26. 各增强轴的验收原则 → 26.1 Identity / Ownership

<!-- RONOVA-ORIGINAL-BEGIN orig-bl-l2703 -->
#### 26.1 Identity / Ownership

必须证明：

- UUID clone 不直接获得原身份；
- lineage 来自独立 observation / Root binding；
- dimension transfer 不丢失 logical identity；
- executor lease 失效不导致 OwnershipBinding 消失。

---


<!-- RONOVA-ORIGINAL-END orig-bl-l2703 -->

<a id="orig-cf-l1400"></a>

**迁入原文｜CF｜旧 L1400–L1403** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1400)

原标题路径：28. Acceptance Tests for L6E-0

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1400 -->
#### 28. Acceptance Tests for L6E-0

L6E-0 本身虽不实现完整功能，但必须能通过 contract-level tests。


<!-- RONOVA-ORIGINAL-END orig-cf-l1400 -->

<a id="orig-cf-l1404"></a>

**迁入原文｜CF｜旧 L1404–L1419** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1404)

原标题路径：28. Acceptance Tests for L6E-0 → T-01 — Stale Incarnation

**规范关系调整：** 本段的旧位次/冻结不可覆盖规则仅保留作来源记录，不再阻止显式修订；技术不变量本轮未据此删除。参见 [当前文档效力](README.md#scope-governance)。

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1404 -->
#### T-01 — Stale Incarnation

```text
Subject X
Incarnation 37 active
→ bind 38
→ 37 becomes stale
```

要求：

- 37 不能继续获得 Authority；
- 旧 BindingRevision 不能覆盖新 Revision。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1404 -->

<a id="orig-cf-l1420"></a>

**迁入原文｜CF｜旧 L1420–L1436** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1420)

原标题路径：28. Acceptance Tests for L6E-0 → T-02 — Hardened Superset

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1420 -->
#### T-02 — Hardened Superset

切换：

```text
NON_BLOCKING → HARDENED
```

要求：

- Raw Observation 不关闭；
- Convergence 不关闭；
- Admission 不关闭；
- Presence 不关闭。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1420 -->

<a id="orig-cf-l1437"></a>

**迁入原文｜CF｜旧 L1437–L1456** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1437)

原标题路径：28. Acceptance Tests for L6E-0 → T-03 — Unknown Extension

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1437 -->
#### T-03 — Unknown Extension

旧 runtime 收到：

```text
optional unknown extension
```

要求安全忽略。

收到：

```text
mandatory unknown extension
```

要求 fail closed / unsupported schema。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1437 -->

<a id="orig-cf-l1469"></a>

**迁入原文｜CF｜旧 L1469–L1480** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1469)

原标题路径：28. Acceptance Tests for L6E-0 → T-05 — Opaque Relationship

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1469 -->
#### T-05 — Opaque Relationship

关键 owner 解析为 `OPAQUE`。

要求：

- 不猜写；
- Policy 决定 observe/quarantine/deny；
- Resolver 不能直接 Root mutation。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1469 -->

<a id="orig-cf-l1481"></a>

**迁入原文｜CF｜旧 L1481–L1496** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1481)

原标题路径：28. Acceptance Tests for L6E-0 → T-06 — Liveness Overload

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1481 -->
#### T-06 — Liveness Overload

系统进入：

```text
CRITICAL_LIVENESS_UNPROVABLE
```

要求：

- non-critical work 可丢弃；
- critical safety 不静默失效；
- 无法维持时显式 FENCE/FAIL_STOP。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1481 -->

<a id="orig-cf-l1497"></a>

**迁入原文｜CF｜旧 L1497–L1508** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1497)

原标题路径：28. Acceptance Tests for L6E-0 → T-07 — Client Boundary

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1497 -->
#### T-07 — Client Boundary

无 Ronova Client 的普通客户端继续本地绘制假视觉。

要求：

- Server 不宣称其像素被清除；
- Server Truth/Presence 仍保持正确；
- 不因此把 Client Render 变成 FULL L6 必需条件。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1497 -->

<a id="orig-cf-l1509"></a>

**迁入原文｜CF｜旧 L1509–L1514** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1509)

原标题路径：28. Acceptance Tests for L6E-0 → T-08 — Context Revision Crossing

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1509 -->
#### T-08 — Context Revision Crossing

构造同一 operation 的三种 crossing policy，分别改变 projection-only、binding revision 和 policy revision。要求只有 `ALLOW_NEWER_PROJECTION_SAME_AUTHORITY` 接受“仅 projection 前进且 authority 不变”；其余返回 `STALE_CONTEXT` 或执行完整 revalidation。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1509 -->

<a id="orig-cf-l1515"></a>

**迁入原文｜CF｜旧 L1515–L1520** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1515)

原标题路径：28. Acceptance Tests for L6E-0 → T-09 — Resolver Revocation / Hostile Provider

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1515 -->
#### T-09 — Resolver Revocation / Hostile Provider

ACTIVE provider 产出 evidence 后被 revoke，或 provider throw/timeout。要求 evidence 变为 STALE、provider 被隔离并产生 Incident；其他 provider 继续运行，既有 durable binding 不被静默删除也不得未经重验证继续强执行。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1515 -->

<a id="orig-cf-l1521"></a>

**迁入原文｜CF｜旧 L1521–L1526** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1521)

原标题路径：28. Acceptance Tests for L6E-0 → T-10 — Pre-Admission Without Root

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1521 -->
#### T-10 — Pre-Admission Without Root

模拟 Root 不可达并覆盖 chunk load、normal spawn、command summon、dimension transfer 和 third-party direct add。要求 Forge hot path 不同步等待 durable I/O；current projection 可 PREBOUND，UNKNOWN/STALE 进入 HELD，TERMINAL 进入 CANCELLED。可延期工作超预算时只能记录积压并在后续 tick 继续；当前对象的同步 admission / presence / authority 结果不得因预算耗尽而顺延、替换或改答。只有既有 safety policy 明确要求时，才可产生 `cancel/fence`，不能把预算耗尽本身当作同步查询结果。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1521 -->

<a id="orig-cf-l1527"></a>

**迁入原文｜CF｜旧 L1527–L1532** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1527)

原标题路径：28. Acceptance Tests for L6E-0 → T-11 — DeviationAccumulator Semantics

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1527 -->
#### T-11 — DeviationAccumulator Semantics

逐一验证五种 mode 的 update/peek/consume/reset、乱序去重、并发 consume 与 overflow。security edge 不因最终 snapshot 恢复而消失；任何 overflow 都有 counter/summary/degrade/fence 证据。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1527 -->

<a id="orig-cf-l1539"></a>

**迁入原文｜CF｜旧 L1539–L1544** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1539)

原标题路径：28. Acceptance Tests for L6E-0 → T-13 — Immutable Projection View

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1539 -->
#### T-13 — Immutable Projection View

并发 publish 新 projection 时，旧 `ProjectionView` 中 entry、epoch、policy、fence 与 revision 必须来自同一快照；禁止 torn read。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1539 -->

<a id="orig-cf-l1545"></a>

**迁入原文｜CF｜旧 L1545–L1550** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1545)

原标题路径：28. Acceptance Tests for L6E-0 → T-14 — P0-D Minimum Crosscut

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1545 -->
#### T-14 — P0-D Minimum Crosscut

对每个 P0-A wire/schema fixture 执行 structural validation、canonical round-trip、single-bit tamper negative、stale root-instance/fence negative 与 immutable collection check。这里只验证接口可被完整性层覆盖，不实现完整 P0-D。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1545 -->

<a id="orig-cf-l1551"></a>

**迁入原文｜CF｜旧 L1551–L1556** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-cf-l1551)

原标题路径：28. Acceptance Tests for L6E-0 → T-15 — Lifecycle / Resolution Orthogonality

<!-- RONOVA-ORIGINAL-BEGIN orig-cf-l1551 -->
#### T-15 — Lifecycle / Resolution Orthogonality

覆盖 `ACTIVE + UNKNOWN`、`TERMINAL + RESOLVED`、`ACTIVE + OPAQUE + HELD`。要求 lifecycle、resolution 与 admission 三轴互不代替，公共 API 不退化为 boolean/Optional。

---


<!-- RONOVA-ORIGINAL-END orig-cf-l1551 -->

<a id="orig-p0a-l1673"></a>

**迁入原文｜P0A｜旧 L1673–L1688** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1673)

原标题路径：49. Unit Tests

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1673 -->
#### 49. Unit Tests

测试按稳定 invariant id 维护，不按旧版本“测试数量”冻结：

```text
Suite A — model/property
Suite B — wire/contract
Suite C — integration/chaos

Level 1（每次变更） = compile + affected invariant tests
Level 2（阶段 tag） = 当前阶段 gate + shared core regression
Level 3（release）   = 全部 suites + server/client/runtime + performance evidence
```

同一 invariant 只保留一个 canonical fixture，可被 unit、wire 和 integration harness 复用，减少重复手工测试。至少包括：


<!-- RONOVA-ORIGINAL-END orig-p0a-l1673 -->

<a id="orig-p0a-l1689"></a>

**迁入原文｜P0A｜旧 L1689–L1697** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1689)

原标题路径：49. Unit Tests → U-01 SubjectKey Stability

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1689 -->
#### U-01 SubjectKey Stability

同 Subject 两个不同 Java object：

```text
SubjectKey must remain same even when metadata differs
incarnation must differ
```


<!-- RONOVA-ORIGINAL-END orig-p0a-l1689 -->

<a id="orig-p0a-l1698"></a>

**迁入原文｜P0A｜旧 L1698–L1701** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1698)

原标题路径：49. Unit Tests → U-02 Incarnation Monotonicity

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1698 -->
#### U-02 Incarnation Monotonicity

新 incarnation sequence 必须单调。


<!-- RONOVA-ORIGINAL-END orig-p0a-l1698 -->

<a id="orig-p0a-l1702"></a>

**迁入原文｜P0A｜旧 L1702–L1709** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1702)

原标题路径：49. Unit Tests → U-03 Stale Rejection

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1702 -->
#### U-03 Stale Rejection

旧 incarnation 在 rebind 后：

```text
resolve → STALE
```


<!-- RONOVA-ORIGINAL-END orig-p0a-l1702 -->

<a id="orig-p0a-l1710"></a>

**迁入原文｜P0A｜旧 L1710–L1717** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1710)

原标题路径：49. Unit Tests → U-04 Tombstone Dominance

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1710 -->
#### U-04 Tombstone Dominance

TERMINAL Subject 新 incarnation：

```text
bind → TERMINAL_REJECT
```


<!-- RONOVA-ORIGINAL-END orig-p0a-l1710 -->

<a id="orig-p0a-l1718"></a>

**迁入原文｜P0A｜旧 L1718–L1725** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1718)

原标题路径：49. Unit Tests → U-05 Binding Revision

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1718 -->
#### U-05 Binding Revision

旧 revision 更新：

```text
INVALID_TRANSITION
```


<!-- RONOVA-ORIGINAL-END orig-p0a-l1718 -->

<a id="orig-p0a-l1726"></a>

**迁入原文｜P0A｜旧 L1726–L1733** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1726)

原标题路径：49. Unit Tests → U-06 Root Command Idempotency

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1726 -->
#### U-06 Root Command Idempotency

同 commandId / same payload：

```text
same semantic result
```


<!-- RONOVA-ORIGINAL-END orig-p0a-l1726 -->

<a id="orig-p0a-l1734"></a>

**迁入原文｜P0A｜旧 L1734–L1741** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1734)

原标题路径：49. Unit Tests → U-07 Command Conflict

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1734 -->
#### U-07 Command Conflict

同 commandId / different payload：

```text
COMMAND_ID_CONFLICT
```


<!-- RONOVA-ORIGINAL-END orig-p0a-l1734 -->

<a id="orig-p0a-l1742"></a>

**迁入原文｜P0A｜旧 L1742–L1745** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1742)

原标题路径：49. Unit Tests → U-08 Projection Atomicity

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1742 -->
#### U-08 Projection Atomicity

Reader 不得看到 half-built projection。


<!-- RONOVA-ORIGINAL-END orig-p0a-l1742 -->

<a id="orig-p0a-l1746"></a>

**迁入原文｜P0A｜旧 L1746–L1749** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1746)

原标题路径：49. Unit Tests → U-09 Unknown Optional Extension

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1746 -->
#### U-09 Unknown Optional Extension

安全忽略。


<!-- RONOVA-ORIGINAL-END orig-p0a-l1746 -->

<a id="orig-p0a-l1750"></a>

**迁入原文｜P0A｜旧 L1750–L1753** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1750)

原标题路径：49. Unit Tests → U-10 Unknown Mandatory Extension

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1750 -->
#### U-10 Unknown Mandatory Extension

`UNSUPPORTED_SCHEMA`


<!-- RONOVA-ORIGINAL-END orig-p0a-l1750 -->

<a id="orig-p0a-l1754"></a>

**迁入原文｜P0A｜旧 L1754–L1757** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1754)

原标题路径：49. Unit Tests → U-11 Lifecycle / Resolution Orthogonality

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1754 -->
#### U-11 Lifecycle / Resolution Orthogonality

`ACTIVE + UNKNOWN`、`TERMINAL + RESOLVED`、`ACTIVE + OPAQUE + HELD` 均可表达；任何路径不得退化成 boolean/Optional。


<!-- RONOVA-ORIGINAL-END orig-p0a-l1754 -->

<a id="orig-p0a-l1758"></a>

**迁入原文｜P0A｜旧 L1758–L1761** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1758)

原标题路径：49. Unit Tests → U-12 Revision Scope

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1758 -->
#### U-12 Revision Scope

BindingRevision 只按 SubjectKey 比较；OwnershipRevision 只按 `(SubjectKey,StateDomainId)` 比较；跨 scope 比较必须拒绝。


<!-- RONOVA-ORIGINAL-END orig-p0a-l1758 -->

<a id="orig-p0a-l1762"></a>

**迁入原文｜P0A｜旧 L1762–L1765** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1762)

原标题路径：49. Unit Tests → U-13 Canonical Wire / Defensive Copy

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1762 -->
#### U-13 Canonical Wire / Defensive Copy

覆盖字段 tag、presence bits、NFC、extension sort/limits、同 commandId 不同 hash、payload defensive copy 与 golden round-trip。


<!-- RONOVA-ORIGINAL-END orig-p0a-l1762 -->

<a id="orig-p0a-l1766"></a>

**迁入原文｜P0A｜旧 L1766–L1769** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1766)

原标题路径：49. Unit Tests → U-14 Immutable ProjectionView

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1766 -->
#### U-14 Immutable ProjectionView

并发 publish 下，一个 view 的 revision/epoch/policy/fence/entry 必须同源，不能 torn read。


<!-- RONOVA-ORIGINAL-END orig-p0a-l1766 -->

<a id="orig-p0a-l1770"></a>

**迁入原文｜P0A｜旧 L1770–L1773** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1770)

原标题路径：49. Unit Tests → U-15 Provider Lifecycle

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1770 -->
#### U-15 Provider Lifecycle

provider revoke/disappear/throw/timeout 后 evidence stale 且隔离；其他 provider 不受影响。


<!-- RONOVA-ORIGINAL-END orig-p0a-l1770 -->

<a id="orig-p0a-l1774"></a>

**迁入原文｜P0A｜旧 L1774–L1777** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1774)

原标题路径：49. Unit Tests → U-16 Pre-Admission State Machine

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1774 -->
#### U-16 Pre-Admission State Machine

覆盖全部合法/非法 transition、Root unavailable、hold budget 与五类 Forge seam。


<!-- RONOVA-ORIGINAL-END orig-p0a-l1774 -->

<a id="orig-p0a-l1794"></a>

**迁入原文｜P0A｜旧 L1794–L1795** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1794)

原标题路径：50. Integration Tests

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1794 -->
#### 50. Integration Tests


<!-- RONOVA-ORIGINAL-END orig-p0a-l1794 -->

<a id="orig-p0a-l1796"></a>

**迁入原文｜P0A｜旧 L1796–L1811** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1796)

原标题路径：50. Integration Tests → I-01 Entity Rebuild

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1796 -->
#### I-01 Entity Rebuild

```text
Entity A active
→ destroy A
→ create B same UUID
```

要求：

- B 新 incarnation；
- 不自动继承旧 execution authority；
- Root binding 决定是否 active。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l1796 -->

<a id="orig-p0a-l1827"></a>

**迁入原文｜P0A｜旧 L1827–L1838** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1827)

原标题路径：50. Integration Tests → I-03 Projection Lag

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1827 -->
#### I-03 Projection Lag

Root revision N+1 已提交，本地仍 N：

要求：

- snapshot 带明确 ProjectionRevision；
- P0-B 可识别 stale；
- 本地不得假装 N+1 已生效。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l1827 -->

<a id="orig-p0a-l1839"></a>

**迁入原文｜P0A｜旧 L1839–L1850** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1839)

原标题路径：50. Integration Tests → I-04 Root Receipt Ambiguity

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1839 -->
#### I-04 Root Receipt Ambiguity

要求进入：

```text
COMMIT_UNKNOWN
```

并使用同 commandId 解析。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l1839 -->

<a id="orig-p0a-l1851"></a>

**迁入原文｜P0A｜旧 L1851–L1864** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1851)

原标题路径：50. Integration Tests → I-05 Mode Change

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1851 -->
#### I-05 Mode Change

```text
NON_BLOCKING → HARDENED
```

要求：

- 产生新 PolicyRevision；
- Projection 更新；
- Non-Blocking core 仍启用。

---


<!-- RONOVA-ORIGINAL-END orig-p0a-l1851 -->

<a id="orig-p0a-l1865"></a>

**迁入原文｜P0A｜旧 L1865–L1878** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l1865)

原标题路径：50. Integration Tests → I-06 Relationship Opaque

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l1865 -->
#### I-06 Relationship Opaque

Resolver 找到未知 holder：

```text
OPAQUE
```

要求：

- 不自动写；
- 不自动 BOUND；
- Policy 决定后续。


<!-- RONOVA-ORIGINAL-END orig-p0a-l1865 -->

<a id="ms-entitydefence-l300"></a>

## 故障定位

**框架来源：** [MS entitydefence，原 L300–L313](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#故障定位)。

**编排说明（非原条款）：** MS 的查询缺失、停 tick、不可见、重复实体、区块切换及迭代崩溃九类症状需要 Ronova 具体排查入口；旧规划主要登记了手册待办。

**关联原条款：** [MSC · M1 —— 可见验收闭环（C8、R-S2/S3/S4）](MS-SKILLS-PLAN.md#orig-msc-l0243)；[MSC · 1.14 构建与验收](MS-SKILLS-PLAN.md#orig-msc-l0177)。

本节点没有被单独搬入的原文段落；它保留 MS 的独立职责入口。上列关联条款或下级章节不等于本节点所有细节已经覆盖；适用性与缺口见映射表。

<a id="ms-entitydefence-l314"></a>

## 完成标准

**框架来源：** [MS entitydefence，原 L314–L325](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md#完成标准)。

**编排说明（非原条款）：** 保留原身份、绑定、投影、策略、关系、准入与性能退出条件。外部 helper 的旧条件只作为历史对照，当前不得起外部进程。

<a id="orig-p0a-l2158"></a>

**迁入原文｜P0A｜旧 L2158–L2161** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l2158)

原标题路径：60. P0-A Exit Criteria

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l2158 -->
#### 60. P0-A Exit Criteria

只有全部通过，P0-A 才算完成：


<!-- RONOVA-ORIGINAL-END orig-p0a-l2158 -->

<a id="orig-p0a-l2162"></a>

**迁入原文｜P0A｜旧 L2162–L2169** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l2162)

原标题路径：60. P0-A Exit Criteria → Identity

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l2162 -->
#### Identity

- SubjectKey 是唯一 equality/hash/Root key，metadata 不参与相等；
- 当前 SubjectIdentity 已迁移/适配，无平行 logical identity；
- Subject 与 Java object 解耦；
- incarnation 可替换；
- stale incarnation 不继承 Authority。


<!-- RONOVA-ORIGINAL-END orig-p0a-l2162 -->

<a id="orig-p0a-l2170"></a>

**迁入原文｜P0A｜旧 L2170–L2176** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l2170)

原标题路径：60. P0-A Exit Criteria → Binding

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l2170 -->
#### Binding

- BindingRevision 单调；
- OwnershipRevision 按 domain 单调且不与 BindingRevision 混用；
- rebind 原子；
- old binding 不可回滚。


<!-- RONOVA-ORIGINAL-END orig-p0a-l2170 -->

<a id="orig-p0a-l2177"></a>

**迁入原文｜P0A｜旧 L2177–L2182** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l2177)

原标题路径：60. P0-A Exit Criteria → Lifecycle

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l2177 -->
#### Lifecycle

- Tombstone durable；
- TERMINAL dominance 有测试证明；
- new incarnation 无法绕 Tombstone。


<!-- RONOVA-ORIGINAL-END orig-p0a-l2177 -->

<a id="orig-p0a-l2183"></a>

**迁入原文｜P0A｜旧 L2183–L2191** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l2183)

原标题路径：60. P0-A Exit Criteria → Root

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l2183 -->
#### Root

- 复用现有 root/api，Root-side schema/handler/dedup/recovery/migration 均交付；
- Internal Root instance/fence/command-sequence 与 canonical P0-A envelope 绑定；
- command idempotency；
- COMMIT_UNKNOWN 及同 commandId/hash recovery；
- same id/different hash conflict；
- anti-replay / monotonic epoch。


<!-- RONOVA-ORIGINAL-END orig-p0a-l2183 -->

<a id="orig-p0a-l2192"></a>

**迁入原文｜P0A｜旧 L2192–L2198** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l2192)

原标题路径：60. P0-A Exit Criteria → Projection

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l2192 -->
#### Projection

- immutable；
- atomic swap；
- ProjectionView 单 handle、revision/entry/epoch/policy/fence 无 tearing；
- no N rebuild per batch。


<!-- RONOVA-ORIGINAL-END orig-p0a-l2192 -->

<a id="orig-p0a-l2199"></a>

**迁入原文｜P0A｜旧 L2199–L2204** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l2199)

原标题路径：60. P0-A Exit Criteria → Policy

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l2199 -->
#### Policy

- Mode Root-authorized；
- HARDENED 是 NON_BLOCKING 超集；
- Constitutional Invariants 不可被 overlay 破坏。


<!-- RONOVA-ORIGINAL-END orig-p0a-l2199 -->

<a id="orig-p0a-l2205"></a>

**迁入原文｜P0A｜旧 L2205–L2210** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l2205)

原标题路径：60. P0-A Exit Criteria → Relationship

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l2205 -->
#### Relationship

- Resolver 不自授权；
- OPAQUE 不猜写；
- Graph 不成为第二个 Root。


<!-- RONOVA-ORIGINAL-END orig-p0a-l2205 -->

<a id="orig-p0a-l2211"></a>

**迁入原文｜P0A｜旧 L2211–L2217** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l2211)

原标题路径：60. P0-A Exit Criteria → Admission / Result Semantics

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l2211 -->
#### Admission / Result Semantics

- 五类 Forge seam 有映射；hot path 零同步 durable I/O；Root boundary 只消费本地 immutable projection；
- Root unavailable 时 UNKNOWN/STALE 不放行；
- lifecycle/resolution/admission 三轴正交；
- Authority API 无 boolean/Optional/null 承载 Unknown。


<!-- RONOVA-ORIGINAL-END orig-p0a-l2211 -->

<a id="orig-p0a-l2218"></a>

**迁入原文｜P0A｜旧 L2218–L2223** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l2218)

原标题路径：60. P0-A Exit Criteria → P0-D Minimum Crosscut

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l2218 -->
#### P0-D Minimum Crosscut

- structural/canonical/fenced/tamper fixtures 完整；
- stale root-instance/fence 与 payload tamper negative 全 FAIL；
- 本项不冒充完整 P0-D runtime 已完成。


<!-- RONOVA-ORIGINAL-END orig-p0a-l2218 -->

<a id="orig-p0a-l2224"></a>

**迁入原文｜P0A｜旧 L2224–L2229** · [原章节映射](RESTRUCTURE-MAP.md#map-orig-p0a-l2224)

原标题路径：60. P0-A Exit Criteria → Performance

<!-- RONOVA-ORIGINAL-BEGIN orig-p0a-l2224 -->
#### Performance

- hot lookup O(1) expected；
- no unbounded history；
- no hot full snapshot copy。


<!-- RONOVA-ORIGINAL-END orig-p0a-l2224 -->
