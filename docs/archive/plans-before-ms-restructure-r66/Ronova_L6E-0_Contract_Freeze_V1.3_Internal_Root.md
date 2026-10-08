# Ronova — L6E-0 Contract Freeze V1.3 — Internal Root

> 有效规划（2026-09-08 整理）：正文的历史版本、阶段和验证记录保留原义；当前实现见 [进度](../CURRENT-PROGRESS.md)。
> 规范顺序与后续实施入口见 [规划索引](README.md)，新功能不因规划登记而视为已实现。

> **2026-09-05 文档接合说明：**当前规范链见 [架构索引](README.md)：L6 Master V2.4 → Baseline V1.4 → 本文 V1.3 → [加强总纲 V2.6](Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md) → P0-A/阶段实现规格。V2.6 §9.5.1 复用本文 SubjectKey/Binding/Ownership，§12.6 复用 §16 Side-Effect 分类，§9.4.1 复用 §20–§21 客户端边界；不是另建 identity、Truth 或副作用分类。本文冻结合同不变，新增执行接合仍为设计候选，非 r53 已实现。

> **V1.3 Internal Root + Bootstrap/Restart Boundary Revision — 2026-08-29**
>
> 本 Contract Freeze 以 FULL L6 Internal 为前提：`InternalDominionRoot` 与 Minecraft 同 JVM，作为唯一逻辑 Authority writer；`DurableRootStore` 为 durable backend。
> 所有 External Root transport/session/Named Pipe 语义从 mandatory contract 删除；`FenceToken` 保留为内部 writer-generation 防 stale writer。
> 本 Contract 同时继承 Canonical D13/`PROC-INV-1`：L6E 允许 bounded bootstrap/attach/recovery/relaunch helper，但 Authority truth、durable Root mutation 与 continuing enforcement ownership 必须在当前有效 Minecraft JVM；helper/sidecar 不得成为 External Authority writer 或持续 combat plane。


> **Ronova sync:** 2026-08-30；项目身份与 canonical 包路径已对齐 r50 working source；冻结契约版本 V1.3。
> **Status:** CONTRACT FROZEN  
> **Scope:** Post-Baseline V1.4 / Pre-Implementation  
> **Basis:** Canonical L6 Master V2.4 Internal Root + Baseline V1.4 hardening closure（已内联；不存在第五份 Addendum）  
> **Not an implementation spec. 本文不声称当前 r50 已实现 L6E。**
>
> 本文的目的不是继续扩张功能，而是在进入 P0-A / P0-B / P0-C / P0-D Implementation Spec 之前，冻结所有跨层必须共享的语义、标识、版本、模式、失败和扩展契约。
>
> 冻结后，后续实现不得通过“局部方便”破坏这里定义的全局语义。

---

# 0. Non-Goals

L6E-0 **不实现**：

- InternalDominionRoot 本体（应由 FULL L6 Internal 阶段先完成）；
- Authority Relationship Graph 全量 Resolver；
- Execution Capability 真实签发；
- Authority AllReturn；
- Durable TerminalWatch；
- Native Authority Microkernel；
- Semantic Class Seal；
- Render Client Hardening；
- 全量 VITALS Owner Resolution；
- Physical Incarnation Reconstitution。

L6E-0 只冻结：

1. 标识语义；
2. revision / epoch 语义；
3. Root-compatible envelope；
4. mode / scope / policy 语义；
5. failure / fence / liveness 语义；
6. P0-A → P0-B 契约；
7. P0-B → P0-C completion / observation 契约；
8. Server / Client 边界；
9. forward-compatible extension 规则。

---

# 1. Frozen Architecture Position

```text
Canonical L6 Master V2.4 Internal Root
    ↓
V1.4 Architecture + Hardening Closure
    ↓
L6E-0 Contract Freeze V1.3   ← 本文
    ↓
P0-A Truth Authority Implementation Spec V1.3
    ↓
P0-B Execution Authority Implementation Spec
    ↓
P0-C Convergence Authority Implementation Spec

P0-D Integrity Authority = 从本 Contract 起横切 A/B/C，最终单独 Closure
```

**禁止**把 L6E-0 反向解释成对 Canonical L6 的替代。

---

# 2. Global Redlines

以下红线在 L6E-0 冻结：

## R1 — No Mod-ID Authority

不得出现：

```text
if modId == X → allow/deny/terminal/hardened
```

Mod / class / loader / transformer provenance 只能作为：

- evidence；
- incident attribution；
- selective fencing input。

不能直接成为 Authority。

---

## R2 — Non-Blocking ≠ Hookless

`NON_BLOCKING` 仅表示：

> 不依赖对 hostile mutation 的逐次主动拦截。

它仍可使用少量 Authority-critical seams：

- phase reconciliation；
- admission；
- presence publication；
- side-effect publication；
- raw observation；
- integrity verification。

---

## R3 — Hardened 必须是 Non-Blocking Core 的超集

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

## R4 — Server Authority ≠ Client Pixel Authority

FULL L6 Internal / L6E Server 强保证：

- server truth；
- logical identity；
- lifecycle；
- presence；
- server-owned projection。

不声称保证：

- 任意恶意客户端最终屏幕像素；
- 外部 overlay；
- 已失陷客户端进程。

客户端 Render Hardening 只能作为可选扩展。

---

## R5 — Unknown ≠ False ≠ Safe

任何未解析状态不得默认为安全。

统一使用显式状态：

```text
RESOLVED
PARTIALLY_RESOLVED
AMBIGUOUS
OPAQUE
STALE
INVALID
UNKNOWN
```

---

## R6 — Repair Success ≠ Trust Restored

任何 Integrity repair 后必须：

```text
repair
→ revalidate
→ PASS
→ ACTIVE
```

否则继续 FENCED。

---

## R7 — Internal Root Process-Role Boundary

FULL L6 Internal / L6E 的 “Internal” 约束 Authority 位置与外部进程角色；它不要求 child-process count=0：

```text
External Root/persistent Authority service/daemon  FORBIDDEN
Named Pipe/cross-process IPC as Root channel      FORBIDDEN
AutoAttach/helper JVM              ALLOWED_FOR_BOUNDED_BOOTSTRAP_ATTACH
unbounded/persistent external fallback            FORBIDDEN
in-process JNI/JVMTI               ALLOWED
same-JVM premain                   ALLOWED
degraded/fenced same-JVM fallback  ALLOWED
```

Native unavailable、Agent unavailable 或 Integrity proof failure 可以触发 bounded attach/recovery/relaunch helper；helper 不得持有 Authority truth 或持续 enforcement，必须在 handoff 后退出。若无法建立合规 in-JVM runtime，则进入显式 degraded/fenced result。

---

# 3. Canonical Identifier Model

## 3.1 RealmId

表示 Authority 所属逻辑 realm。

要求：

- 全局稳定；
- 不从当前 Java object identity 推导；
- 不与 Minecraft dimension id 混用；
- 可跨重启与 world/store lineage 持久化；不要求跨进程。

---

## 3.2 SubjectKey 与 SubjectIdentity

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

## 3.3 PhysicalIncarnationId

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

## 3.4 BindingRevision / OwnershipRevision

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

## 3.5 ProjectionRevision

表示当前本地 immutable Authority Projection 的版本。

要求：

- 单调；
- 与 Root CommitIndex 相关但不等同；
- 同一 operation 读取的 authority snapshot 必须明确绑定某一 ProjectionRevision；
- critical execution completion 必须证明自己没有错误跨越不允许的 projection revision。

`ProjectionRevision` 单调 per Realm projection lineage。它不是 Binding/Ownership revision 的替代物；每个 projection entry 必须同时携带其实际消费的 binding 与 ownership revision。

## 3.5.1 PolicyRevision

`PolicyRevision` 是 Root-authorized policy/mode bundle 的单调版本，作用域为 Realm policy lineage。全文只使用 `PolicyRevision`；旧称 `ModeRevision` 不再作为独立类型。

---

## 3.6 CriticalOperationId

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

## 3.7 CompletionContractId

所有 Authority-bearing completion 必须引用 CompletionContract。

示例：

```text
TERMINAL_COMMIT_V1
VITAL_WRITE_V1
ADMISSION_BIND_V1
PRESENCE_REVOKE_V1
```

CompletionContract 描述：

- 允许的 completion type；
- 必须成立的 postcondition；
- 允许的 side-effect scope；
- revision consistency requirements。

---

# 4. DominionEpoch / Fence / Sequence 分离

继续冻结：

```text
DominionEpoch = (generation, sequence)
```

以下概念严格分离：

```text
DominionEpoch
FenceToken
RootInstanceId
CommandSequence
commandId
commitIndex
realmId
bindingRevision
projectionRevision
incarnationSequence
```

不再要求 external session/holder/message transport identity。`FenceToken` 只用于同 JVM 内 Root writer generation 与 stale-instance rejection。

# 5. Root-Compatible Forward Envelope

L6E-0 冻结 canonical **in-process Root command / projection envelope**，不定义网络协议。

```text
AuthorityEnvelope
├ protocolVersion
├ schemaVersion
├ messageType
├ realmId
├ rootInstanceId?
├ fenceToken?
├ commandSequence?
├ subjectKey?
├ subjectMetadata?
├ incarnationId?
├ operationId?
├ completionContractId?
├ bindingRevision?
├ ownershipRevision?
├ projectionRevision?
├ dominionEpoch
├ expectedEpoch?
├ commandId?
├ commitIndex?
├ policyRevision?
├ flags
└ extensions[]
```

规则：

- canonical hash、field ordering、NFC、presence bits、extension criticality 继续保留；
- Root mutation envelope 必须带 `commandId + expectedEpoch + rootInstanceId + fenceToken`；
- `same commandId + same canonical payload` 幂等；
- `same commandId + different payload` → `COMMAND_ID_CONFLICT`；
- 不再存在 AXL6 session/HMAC/Named Pipe header；
- canonical Authority command/envelope 不得把 child process、external helper、OS shared-memory IPC 或本地网络 loopback 提升为 Root transport；Java↔Native Authority path 保持同 JVM JNI/只读 in-process projection；bootstrap helper 只负责 attach/relaunch handoff；
- realtime query 不创建 Root mutation envelope，只消费 `ProjectionView`。

## 5.1 Unknown Field Rule

未来新增 optional field 可安全忽略；未知 mandatory semantic → `UNSUPPORTED_SCHEMA`。extension payload 必须 immutable / defensive-copy / bounded。

## 5.2 Extension Namespace

```text
core.*
p0a.*
p0b.*
p0c.*
p0d.*
client.*
experimental.*
```

## 5.3 Dormant Field Rule

没有 writer/reader/activation rule 的未来能力不得预写空 core 字段。

# 6. P0-A → P0-B Contract

P0-B 不得自己发明 Truth。

P0-A 必须向 P0-B 提供一个不可变 authority snapshot：

```text
AuthoritySnapshot
├ subjectKey
├ subjectMetadata
├ activeIncarnation?
├ bindingRevision
├ ownershipBindings[StateDomainId → OwnershipBinding + OwnershipRevision]
├ projectionRevision
├ policyRevision
├ dominionEpoch
├ lifecycleTruth       ACTIVE | TERMINAL
├ resolutionState      RESOLVED | PARTIALLY_RESOLVED | AMBIGUOUS |
│                      OPAQUE | STALE | INVALID | UNKNOWN
├ policyMode
├ fenceState
└ relevant domain constraints
```

状态轴必须正交：

- `lifecycleTruth` 只表达 durable `ACTIVE | TERMINAL`；
- `resolutionState` 只表达本地是否足以解释 snapshot；
- `QUARANTINED` / `HELD` 属于 admission / presence disposition，不得伪装成第三种 lifecycle truth；
- `UNKNOWN` 不是 `ACTIVE`，也不能通过布尔默认值变成“可执行”。

P0-B 只允许基于这个 snapshot 做：

```text
ALLOW
DENY
STALE
FENCED
RETRY_REQUIRED
```

P0-B 不允许：

- 直接修改 Root Truth；
- 自增 BindingRevision；
- 自己创建 Tombstone；
- 根据本地猜测重写 Identity。

---

# 7. ExecutionAuthorityContext Contract

```text
ExecutionAuthorityContext
├ subjectKey
├ incarnationId
├ operationId
├ completionContractId
├ ownershipRevision
├ bindingRevision
├ projectionRevision
├ policyRevision
├ dominionEpoch
├ mode
├ fenceState
├ fenceScope
├ revisionCrossingPolicy
├ callSiteEvidence?
├ classRevision?
└ contextSequence
```

要求：

1. 一次 critical operation **只从同一个 immutable `ProjectionView` handle 构建一次** context；不得分别读取 revision、binding 与 policy 造成 tearing；
2. `subjectKey`、`incarnationId`、`operationId`、`completionContractId`、四类 revision、epoch、mode、fence state/scope、crossing policy、context sequence 为 native required fields；缺失即 `INVALID_CONTEXT`；
3. context 不可被 Domain 在执行途中 mutate，也不得以 extension 覆盖 required field；
4. completion 必须按下述 crossing policy 校验；违反时返回 `STALE_CONTEXT`，不得默认为成功或失败；
5. context 本身不是 Root Truth，只是绑定单一 ProjectionRevision 的执行快照。

## 7.1 Revision Crossing Policy

```text
EXACT_SNAPSHOT
    completion 时四类 revision、epoch 与 context 完全一致

ALLOW_NEWER_PROJECTION_SAME_AUTHORITY
    只允许 ProjectionRevision 前进；SubjectKey、Incarnation、BindingRevision、
    OwnershipRevision、PolicyRevision、Epoch 与 FenceState 必须不变

REVALIDATE_BEFORE_COMPLETION
    completion 前必须从一个新的 immutable ProjectionView 重建 context，
    并重新执行 Judgment；不能只替换 revision 数字
```

未声明 crossing policy 的 CompletionContract 等同于 `EXACT_SNAPSHOT`。

## 7.2 CompletionContract

```text
CompletionContract
├ id                       CompletionContractId
├ contractRevision         positive u64
├ allowedKinds             RETURN | THROW | ABORT | SUPPRESS | REWRITE
├ requiredPostconditions[]
├ sideEffectScope          SideEffectScope
├ revisionCrossingPolicy   RevisionCrossingPolicy
└ failureMapping           closed mapping to explicit AuthorityResult state
```

`allowedKinds` 是封闭集合；未列出的 completion 不合法。`failureMapping` 不得把 `UNKNOWN`、`STALE`、`FENCED` 映射为普通 `false`。

---

# 8. ResolverProvider SPI Contract

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

## 8.1 Relationship Edge Lifecycle

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

# 9. Pre-Admission Binding Contract

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

## 9.1 Admission Levels

- `HARD_PREBIND`：在物理对象进入世界容器前完成 `PREBOUND`；
- `PRE_PRESENCE_BIND`：对象可暂存于隔离容器，但在 tracking / publish 前完成 `PREBOUND`；
- `POST_ADMISSION_EMERGENCY_RECONCILE`：只用于无法命中前两类 seam 的既存对象，初始为 `EMERGENCY_RECONCILE`，不自动获得 full presence。

## 9.2 Forge Seam Mapping

| Forge 语义 seam | 最低要求 | Root/Projection 规则 |
|---|---|---|
| NBT / chunk load | `HARD_PREBIND` 优先 | 只用本地 immutable ProjectionView 判定；缺 binding 时 HELD |
| normal spawn / join | `PRE_PRESENCE_BIND` | publish/tracking 前必须 PREBOUND |
| command summon | `PRE_PRESENCE_BIND` | 不得因命令来源跳过 authority |
| dimension transfer | 原 SubjectKey + 新 Incarnation rebind | 旧 incarnation 在新 binding commit 后立即 stale |
| third-party direct add | emergency seam | 先隔离为 EMERGENCY_RECONCILE，再绑定或取消 |

## 9.3 Root Unavailable / Fenced

admission hot path 禁止同步执行 durable Root I/O，只允许消费当前 immutable `ProjectionView`：

- current binding 且未 fenced → 可 PREBOUND；
- projection `STALE / UNKNOWN` → HELD；
- durable TERMINAL → CANCELLED；
- Internal Root component 不可用或 durable store fenced → 不产生新 durable truth；
- Root 恢复后异步 refresh/reconcile，不在 Forge callback 内阻塞持久化。

# 10. Mode Contract

冻结三种主模式：

```text
OBSERVATIONAL
NON_BLOCKING
HARDENED
```

---

## 10.1 OBSERVATIONAL

允许：

- Root read；
- Raw Observation；
- evidence collection；
- diagnose。

默认不主动 enforcement。

---

## 10.2 NON_BLOCKING

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

## 10.3 HARDENED

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

# 11. Mode Scope

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

# 12. Mode Change Authority

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

# 13. Policy Constitution

普通 Policy / Overlay 不允许破坏：

```text
TERMINAL precedence
Epoch monotonicity
COMMIT_UNKNOWN semantics
Ownership revision ordering
Tombstone durability
Root writer uniqueness
Stale incarnation denial
Fence monotonic safety
```

这些统称：

```text
Constitutional Invariants
```

Policy Compiler 必须支持：

```text
Static Analysis
Conflict Detection
Unreachable Rule Detection
Invariant Validation
Dry Run
Signed Revision
```

---

# 14. Liveness Contract

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

## 14.1 Liveness States

```text
NORMAL
→ DEGRADED
→ STORM
→ CRITICAL_LIVENESS_UNPROVABLE
→ FENCED / FAIL_STOP
```

---

## 14.2 Liveness Safety Rule

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

## 14.3 CriticalInvariant Contract

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

# 15. DeviationAccumulator Contract

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

## 15.1 Required Operations

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

## 15.2 Exact Mode Semantics

| mode | update | consume/reset 语义 |
|---|---|---|
| `LATEST_STATE` | 保存最高 sequence 的完整 immutable observation | `consume` 返回当前值并清空；用于可由最终状态完整表达的偏差 |
| `EXTREMA_LATCH` | 保存窗口内 min/max 及首次越界证据 | 未成功 reconcile 前不得因值恢复正常而清除 extrema |
| `EDGE_LATCH` | 保存每种 security-relevant edge 的首次/末次 sequence 与计数 | edge 至少被一次成功 consume 才可清除 |
| `COUNT_RATE` | 按固定 monotonic time bucket 计数；不使用 wall-clock 回拨 | 返回窗口 counts/rate；过期 bucket 只能按 policy 窗口淘汰 |
| `BOUNDED_TRANSITION_LOG` | 按 sequence 保存转移记录 | 满容量后不静默覆盖 security evidence；进入 overflow 摘要并触发 degrade/fence policy |

## 15.3 Bounds and Overflow

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

# 16. Side-Effect Contract

SideEffect 分为：

```text
RONOVA_CONTROLLED
REVERSIBLE
COMPENSATABLE
EXTERNAL_OPAQUE
```

---

## 16.1 RONOVA_CONTROLLED

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

## 16.2 REVERSIBLE

具有确定 inverse operation。

允许 rollback，但必须：

- 有 revision；
- 可验证；
- 不产生二次不一致。

---

## 16.3 COMPENSATABLE

不保证精确回滚，只能进行已知补偿。

例如某些：

- 掉落；
- XP；
- scoreboard。

补偿失败必须产生 Incident。

---

## 16.4 EXTERNAL_OPAQUE

Ronova 不知道或无法证明其副作用语义。

处理：

```text
observe
→ incident
→ optional degrade/fence
```

不得盲目修改第三方系统。

---

# 17. Relationship Graph Trust Contract

Graph 是证据图，不是第二个 Root。

任何关系图更新都必须带：

```text
graphRevision
edgeRevision
provenance
validationState
```

本地缓存 Graph 可以重建。

Root 只持久化：

- 已确认的 Authority Binding；
- 必要 identity/ownership relationship；
- 不持久化所有临时 discovery noise。

---

# 18. Integrity Failure Contract

统一 failure：

```text
INTEGRITY_SUSPECT
COMPONENT_FENCED
REPAIRING
REVALIDATING
REPAIR_BUDGET_EXHAUSTED
STABLE_FENCED
GLOBAL_FAIL_STOP
```

禁止出现：

```text
repair failed
→ log warning
→ continue ACTIVE
```

---

# 19. COMMIT_UNKNOWN Contract

Internal Root 仍保留 `COMMIT_UNKNOWN`，但触发源改为本地 durable commit ambiguity：

```text
submit(command)
→ durable write 可能已经完成
→ JVM crash / I/O exception / receipt delivery interrupted
```

必须：

- 保留同一 `command_id`；
- restart 后查询 dedup/receipt store 或幂等重放同 envelope；
- 不允许本地猜 commit failed/succeeded；
- irreversible lifecycle transition 等 durable result 解析后再执行。

# 20. Server / Client Boundary Contract

Server 主线只负责：

```text
Truth
Identity
Lifecycle
Presence
Server-owned Projection
Projection Revocation
Network Anti-Replay
```

不在 Server Core 中预留或强依赖：

```text
GPU resource id
Visual capability
VAO/VBO lineage
Frame render token
Native render metadata
```

---

## 20.1 Client Extension Namespace

客户端扩展使用：

```text
client.visual.*
client.projection.*
client.integrity.*
experimental.render.*
```

Server Root 对这些字段：

- 默认 optional；
- 不参与核心 Authority；
- 不影响 Server FULL L6 Internal 判定。

---

# 21. Render Server Contract

Server 只保证：

```text
TERMINAL(X)
→ stop authoritative presence
→ revoke tracking
→ stop entity updates
→ revoke server-owned BossBar
→ revoke known server-owned projections
```

不保证：

```text
恶意客户端不继续画 X
```

---

# 22. Seam Contract

critical seam 不绑定具体方法名，而绑定语义：

```text
PRE_ENTITY_CONSUMPTION
PRE_PRESENCE_PUBLICATION
POST_CRITICAL_MUTATION_VERIFY
RAW_OBSERVATION_POINT
TERMINAL_PHYSICAL_RECONCILE
```

每个版本实现必须证明：

```text
Structural
→ Ordering
→ Behavioral
```

失败：

```text
SEAM_CONTRACT_UNSATISFIED
→ fence affected domain
```

---

# 23. Versioning Contract

所有关键 schema 必须支持：

```text
major
minor
feature flags
mandatory extensions
optional extensions
```

规则：

- major 不兼容；
- minor forward-compatible；
- mandatory extension 不理解 → fail closed；
- optional extension 不理解 → ignore safely；
- feature activation 必须显式。

---

# 24. Audit / Provenance Contract

Execution Provenance 记录：

```text
classOrigin
classLoaderIdentity
transformerLineage
callSite
classRevision
agentEvidence
transformationHistory
```

它只能用于：

- incident；
- diagnostics；
- selective fence proposal。

不能直接决定：

```text
Truth
Judgment
Tombstone
Ownership
```

---

# 25. L6E-0 Frozen Interface Set

进入 P0-A Implementation Spec 前，以下接口名称和语义必须冻结：

1. `RealmId`
2. `SubjectKey`
3. `SubjectIdentity`
4. `SubjectType`
5. `PhysicalIncarnationId`
6. `BindingRevision`
7. `OwnershipRevision`
8. `ProjectionRevision`
9. `PolicyRevision`
10. `DominionEpoch`
11. `CriticalOperationId`
12. `CompletionContractId`
13. `CompletionContract`
14. `RevisionCrossingPolicy`
15. `AuthorityExtension`
16. `AuthorityEnvelope`
17. `AuthorityResult<T>`
18. `AuthoritySnapshot`
19. `ProjectionView`
20. `ExecutionAuthorityContext`
21. `ResolverProvider`
22. `ResolverProviderState`
23. `AuthorityRelationshipEdge`
24. `PreAdmissionBindingState`
25. `DeviationAccumulator`
26. `CriticalInvariant`
27. `ModeSelector`
28. `PolicyOverlay`
29. `FenceState` / `FenceScope`
30. `SideEffectScope`

`AuthorityResult<T>` 是封闭结果：`RESOLVED(T) | PARTIALLY_RESOLVED(T,evidence) | NOT_FOUND(verifiedAtRevision) | AMBIGUOUS | OPAQUE | STALE | INVALID | UNKNOWN | FENCED`。`NOT_FOUND` 只表示在明确 revision 上验证不存在；Root/Projection 不可达或未查清必须是 `UNKNOWN`。公共 Authority API 不得用 `boolean`、`null` 或 `Optional.empty()` 承载这些状态。

`ProjectionView` 是一次读取获得的 immutable handle，至少同时固定 `ProjectionRevision`、`DominionEpoch`、`PolicyRevision`、fence state 与所查 entry；不得提供“先读 revision、再另查 entry”的撕裂式 API。

---

# 26. P0-A Implementation Spec Entry Criteria

只有下面全部满足，才进入 P0-A Spec：

- Identifier semantics frozen（§3.1–3.3）；
- revision semantics frozen（§3.4–3.5.1）；
- Root envelope / canonical hash / extension semantics frozen（§5）；
- `ExecutionAuthorityContext` requiredness、revision crossing 与 completion contract frozen（§7）；
- Resolver provider lifecycle、revoke、stale 与 hostile isolation frozen（§8）；
- Pre-Admission state machine、Forge seam mapping 与 Root-unavailable behavior frozen（§9）；
- `DeviationAccumulator` 五种模式、操作、bounds 与 overflow frozen（§15）；
- mode selector / conflict semantics frozen（§10–13）；
- liveness failure semantics frozen；
- relationship graph trust semantics frozen；
- side-effect semantics frozen；
- client/server boundary frozen；
- seam contract vocabulary frozen；
- forward-compatibility rule frozen；
- P0-D 最小横切契约 frozen：所有 P0-A schema/wire 对象必须支持结构校验、canonical fixture、fenced snapshot 和 tamper-negative fixture；P0-A 不实现完整 P0-D runtime。

本 V1.3 已满足上述文档级 entry criteria；实现是否满足只能由对应 contract tests 与 build/runtime evidence 证明。

---

# 27. P0-A First Implementation Scope

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

# 28. Acceptance Tests for L6E-0

L6E-0 本身虽不实现完整功能，但必须能通过 contract-level tests。

## T-01 — Stale Incarnation

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

## T-02 — Hardened Superset

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

## T-03 — Unknown Extension

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

## T-04 — COMMIT_UNKNOWN

Root durable commit 与 `CommitReceipt` 返回之间发生 I/O ambiguity / JVM crash：

要求：

- 不生成新 command_id；
- 同 command_id recover/retry；
- 不发生 irreversible local assumption。

---

## T-05 — Opaque Relationship

关键 owner 解析为 `OPAQUE`。

要求：

- 不猜写；
- Policy 决定 observe/quarantine/deny；
- Resolver 不能直接 Root mutation。

---

## T-06 — Liveness Overload

系统进入：

```text
CRITICAL_LIVENESS_UNPROVABLE
```

要求：

- non-critical work 可丢弃；
- critical safety 不静默失效；
- 无法维持时显式 FENCE/FAIL_STOP。

---

## T-07 — Client Boundary

无 Ronova Client 的普通客户端继续本地绘制假视觉。

要求：

- Server 不宣称其像素被清除；
- Server Truth/Presence 仍保持正确；
- 不因此把 Client Render 变成 FULL L6 必需条件。

---

## T-08 — Context Revision Crossing

构造同一 operation 的三种 crossing policy，分别改变 projection-only、binding revision 和 policy revision。要求只有 `ALLOW_NEWER_PROJECTION_SAME_AUTHORITY` 接受“仅 projection 前进且 authority 不变”；其余返回 `STALE_CONTEXT` 或执行完整 revalidation。

---

## T-09 — Resolver Revocation / Hostile Provider

ACTIVE provider 产出 evidence 后被 revoke，或 provider throw/timeout。要求 evidence 变为 STALE、provider 被隔离并产生 Incident；其他 provider 继续运行，既有 durable binding 不被静默删除也不得未经重验证继续强执行。

---

## T-10 — Pre-Admission Without Root

模拟 Root 不可达并覆盖 chunk load、normal spawn、command summon、dimension transfer 和 third-party direct add。要求 Forge hot path 不同步等待 durable I/O；current projection 可 PREBOUND，UNKNOWN/STALE 进入 HELD，TERMINAL 进入 CANCELLED。可延期工作超预算时只能记录积压并在后续 tick 继续；当前对象的同步 admission / presence / authority 结果不得因预算耗尽而顺延、替换或改答。只有既有 safety policy 明确要求时，才可产生 `cancel/fence`，不能把预算耗尽本身当作同步查询结果。

---

## T-11 — DeviationAccumulator Semantics

逐一验证五种 mode 的 update/peek/consume/reset、乱序去重、并发 consume 与 overflow。security edge 不因最终 snapshot 恢复而消失；任何 overflow 都有 counter/summary/degrade/fence 证据。

---

## T-12 — Canonical Envelope

对字段顺序、absent/empty、UTF-8 NFC、extension 排序、unknown required/optional、size/count 上限和 defensive copy 做 golden/negative fixture；同 commandId 不同 canonical hash 必须 `COMMAND_ID_CONFLICT`。

---

## T-13 — Immutable Projection View

并发 publish 新 projection 时，旧 `ProjectionView` 中 entry、epoch、policy、fence 与 revision 必须来自同一快照；禁止 torn read。

---

## T-14 — P0-D Minimum Crosscut

对每个 P0-A wire/schema fixture 执行 structural validation、canonical round-trip、single-bit tamper negative、stale root-instance/fence negative 与 immutable collection check。这里只验证接口可被完整性层覆盖，不实现完整 P0-D。

---

## T-15 — Lifecycle / Resolution Orthogonality

覆盖 `ACTIVE + UNKNOWN`、`TERMINAL + RESOLVED`、`ACTIVE + OPAQUE + HELD`。要求 lifecycle、resolution 与 admission 三轴互不代替，公共 API 不退化为 boolean/Optional。

---

## T-16 — Production Process Boundary

在真实 production bootstrap 与 JVM shutdown 之间执行 process-tree capture，并对构建产物做 process-launch inventory。允许 `ProcessBuilder`/Attach helper 或受控 relaunch，但每个路径必须被分类为 bootstrap/attach/recovery/relaunch，helper 不得持有 durable Authority、不得持续监控/攻击目标运行时，并在 handoff 后终止。Named Pipe/local-loopback/OS shared-memory 不得成为 canonical Root transport。

---

# 29. Freeze Decision Summary

L6E-0 最终冻结以下 9 个 Implementation Decisions：

## ID-1 — Liveness

证明重点是：

> **有界工作区间内保证收敛；超载时保证安全降级。**

不是声称无限负载下永远 Δ tick 收敛。

---

## ID-2 — Side Effects

默认：

```text
Ronova-controlled → suppress
known reversible → rollback
known compensatable → compensate
external opaque → incident
```

不构建全局副作用总拦截器。

---

## ID-3 — Relationship Graph

Graph 是：

> **validated evidence graph**

不是第二个 Root。

OPAQUE 不深入猜测。

---

## ID-4 — Mode Switching

Mode 切换：

> **Root-authorized Policy change**

而不是 Domain 自行升级。

HARDENED 永远是 NON_BLOCKING 的超集。

---

## ID-5 — Internal Root Envelope

提前为：

```text
SubjectKey + SubjectIdentity metadata
PhysicalIncarnationId
CriticalOperationId
CompletionContractId
BindingRevision + OwnershipRevision
ProjectionRevision + PolicyRevision
AuthorityExtension
```

通过 versioned extension 提供 forward compatibility；不预写无 writer/reader 的 dormant core 字段。

---

## ID-6 — Render Boundary

Server 主线只保留：

```text
Presence
Projection publication
Projection revocation
BossEvent / tracking cleanup
network anti-replay
```

VisualSubject / Render Firewall 等全部归可选 Client Extension。

---

## ID-7 — Four Shared Protocols

`ExecutionAuthorityContext`、Resolver provider lifecycle、Pre-Admission state machine 与 `DeviationAccumulator` 已具有 requiredness、transition、failure/overflow 和 contract tests，不再留给 P0-A 临场猜测。

---

## ID-8 — Integrity Crosscut

P0-D 从 P0-A schema 起横切：每个 wire/schema 对象都必须可做 structural validation、canonical fixture、fenced snapshot 与 tamper-negative test；完整 repair engine 仍由 P0-D closure 交付。

---

## ID-9 — Production Process Boundary

`Internal Root` 的生产定义包含严格的**角色边界**：Ronova 可使用 bounded helper/restart process 完成 bootstrap、attach、recovery 或 relaunch，但 active Minecraft JVM 始终是唯一 Authority/Truth/continuing-enforcement owner。Named Pipe/local loopback/OS shared-memory 不得成为 canonical Root transport；`AutoAttach` / `AttachHelper` 可作为合规 bootstrap path 保留。

---

# 30. Final Freeze Statement

从 L6E-0 起，Ronova 的工程目标正式冻结为：

> **Ronova 不以“拦住每一次 hostile write”为必要条件。**
>
> **Ronova 以 Root Truth、Logical Identity、Binding Revision、Execution Qualification、Independent Observation、Bounded Convergence、Explicit Failure 与 Integrity Proof 维持 Authority。**
>
> **当系统无法证明自身还能维持这些性质时，必须显式 Fence / Fail-Stop，而不是继续以 ACTIVE 状态运行。**

这份 Contract Freeze V1.3 的直接下游文档为：

> **`Ronova L6E P0-A Truth Authority Implementation Spec V1.3`**

并只先实现：

```text
SubjectKey + SubjectIdentity metadata
Physical Incarnation
Binding / Ownership Revision
Ownership Binding
Execution Lease
Root Projection Contract
Terminal-aware Rebinding
P0-D minimum structural/wire fixtures
```

而不是一次性实现全部 Hardened+。
