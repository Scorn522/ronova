# Ronova — L6E P0-A Truth Authority Implementation Spec V1.3 — Internal Root

> 有效规划（2026-09-08 整理）：正文的历史版本、阶段和验证记录保留原义；当前实现见 [进度](../CURRENT-PROGRESS.md)。
> 规范顺序与后续实施入口见 [规划索引](README.md)，新功能不因规划登记而视为已实现。

> **V1.3 Internal Root + Bootstrap/Restart Boundary Revision — 2026-08-29**
>
> Target 改为 `FULL L6 Internal → L6E`。P0-A 不再假设 External Root / 跨进程 Root；它以已经 authoritative 的 JVM 内 `InternalDominionRoot` 为前提，持久化由 `DurableRootStore` backend 完成。

> **Process-boundary prerequisite:** Canonical D13/`PROC-INV-1` 允许 bounded bootstrap/attach/recovery/relaunch helper。P0-A 的 Subject/Truth/Ownership/durable mutation 与 continuing enforcement ownership 必须留在当前有效 Minecraft JVM；helper 不得成为 durable Authority writer、持续 combat plane 或独立 target-monitoring process。r27 历史审计确认、并由 r50 保留的 `AutoAttach` / `AttachHelper` 可作为 bootstrap path。


> **Ronova sync:** 2026-08-30；项目身份与 canonical 包路径已对齐 r50 working source；实施规格版本 V1.3。
> **Status:** IMPLEMENTATION SPEC READY  
> **Stage:** Post-Baseline V1.4 / Post-L6E-0 Contract Freeze V1.3  
> **Scope:** P0-A — Truth Authority  
> **Target:** FULL L6 Internal → L6E transition foundation  
> **说明：**本文是后续实施规格；当前 r50 已完成 7.14B/7.14C/7.14D/7.15 DurableRootStore Shadow implementation candidate，不声称已实现 P0-A。
>
> 本文只规定 P0-A Truth Authority 的工程实现边界、包结构、核心接口、状态机、数据模型、持久化与投影契约、迁移策略和验收标准。
>
> 本文不实现 P0-B Execution Authority、P0-C Convergence Authority、P0-D Integrity Authority 的完整逻辑，但必须为它们提供稳定、前向兼容的接口。

---

# 0. 上位依据

实现必须遵循以下优先级：

```text
Canonical L6 Master Plan V2.4 Internal Root
    ↓
7.14A Baseline + All Enhancements Master V1.4 Internal Root
    ↓
L6E-0 Contract Freeze V1.3
    ↓
7.16+ JVM Extreme Strength Enhancement Master V2.6 Internal Root
    ↓
本文：P0-A Truth Authority Implementation Spec V1.3
```

2026-09-05 将 [加强总纲 V2.6](Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md) 明确纳入合同增量层，顺序与 [架构索引](README.md) 一致。Baseline V1.4 仍已内联 hardening closure，不存在必须另找的旧 `Post-V1.2 Hardening Addendum`。V2.6 的普通动作许可、TargetEpoch/batch 接合、副作用未知恢复和完成参与范围约束相关新增实现，但不降低本文上游 SubjectKey、单 writer、原子 batch、server/client 和 side-effect 合同。r53 状态及历史证据不变；历史 V2.5/V2.4 不再并行裁决。

若本文与 L6E-0 Contract Freeze 冲突：

> **以 L6E-0 Contract Freeze 为准。**

若本文与 Canonical L6 D1–D13 冲突：

> **以 Canonical L6 冻结决策为准。**

## 0.1 V1.3 Closure Map

| 复核问题 | V1.3 处理 |
|---|---|
| 与当前源码重复类型/包 | §3 明确 KEEP/EXTEND/MIGRATE；只使用 `dev.ronova.coremod` canonical path |
| 身份把 metadata 放进 equality | §4 拆 `SubjectKey` 与 metadata-only `SubjectIdentity` |
| incarnation 缺 Realm | §5 直接包含完整 SubjectKey |
| lifecycle/unknown/quarantine 混为一轴 | §10 拆 LifecycleTruth / ResolutionState / AdmissionDisposition |
| Root 协议弱于 Canonical L6 | §13–16 复用 InternalRoot instance/fence、command hash、封闭 submit/recovery result |
| projection tearing / boolean Optional | §19–22 使用单一 immutable ProjectionView + AuthorityResult |
| 四协议只留名称 | §24、§26 实现 L6E-0 V1.3 已冻结的 admission/provider 合同；context/accumulator 由共享 contract 消费 |
| P0-D 被推迟到最后 | §37 与 deliverables 纳入最小 structural/wire/fenced/tamper fixtures |
| Root-side 交付漏列 | §58 显式列 schema、handler、dedup/recovery、migration/rollback tooling |

---

# 1. P0-A 的唯一职责

P0-A 只回答五个问题：

1. **这个 Logical Subject 是谁？**
2. **当前哪个 Physical Incarnation 与它绑定？**
3. **哪个状态 Owner 在语义上拥有某项 Authority？**
4. **当前 Binding / Projection / Epoch 的 revision 是多少？**
5. **Root Truth 当前是什么？**

P0-A 不负责：

- 决定具体 Java 方法是否可以执行；
- 拦截 hostile mutation；
- 修复 Minecraft 物理状态；
- 做 AllReturn；
- 做 renderer suppression；
- 直接扫描所有第三方对象；
- 自己执行 TERMINAL physical sink。

---

# 2. P0-A 核心不变量

以下不变量必须成为代码级断言、测试和 Root schema 的共同基础。

## A1 — Subject Identity Stability

```text
Logical Subject Identity
≠ Java Object Identity
```

物理 Entity 重建、reload、dimension transfer、manager replacement 不得自动改变 `SubjectKey`。`SubjectIdentity` metadata 可以随新证据更新，但不参与 equality/hash/Root lookup。

---

## A2 — Incarnation Non-Inheritance

新的 PhysicalIncarnation：

```text
不得自动继承旧 Incarnation 的 execution authority。
```

旧 incarnation 一旦 stale：

```text
永远不能仅靠 UUID 相同恢复 Authority。
```

---

## A3 — Binding Monotonicity

```text
BindingRevision(new) > BindingRevision(old)
```

旧 binding 不得覆盖新 binding。

---

## A4 — Tombstone Dominance

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

## A5 — Root Writer Uniqueness

FULL L6 Internal / L6E 中：

> `InternalDominionRoot` 是唯一逻辑 Authority writer；`DurableRootStore` 是唯一 durable backend。

Minecraft Domain、Agent、Native、Projection 不得直接推进 authoritative epoch，也不得绕过 Root 修改 durable truth。

## A6 — Projection Is Not Truth

```text
Projection
= immutable local view of Root Truth
```

Projection stale、损坏、延迟，不得反向改写 Root。

---

## A7 — Graph Is Not Root

Authority Relationship Graph：

```text
= evidence / validated relationship cache
```

不是第二个 Root。

---

## A8 — Production Process Boundary (`PROC-INV-1`)

P0-A 的 truth service、projection publisher、durable mutation 与 continuing enforcement 必须留在当前有效 Minecraft JVM 内；bootstrap/recovery 可以使用 bounded helper/relaunch：

```text
bounded AutoAttach / helper JVM = allowed for bootstrap/attach/relaunch
External Root / persistent Authority service/daemon = forbidden
helper as durable Truth/Ownership writer = forbidden
helper as continuing combat/target-monitoring plane = forbidden
Named Pipe / local loopback / OS shared-memory as canonical Root channel = forbidden
JNI / JVMTI / same-JVM premain / in-process threads = allowed
failed bootstrap after allowed recovery = DEGRADED or FENCED
```

P0-A 可以依赖 canonical bounded bootstrap/attach/relaunch helper，但不得把 `ProcessBuilder`、Attach API、service/daemon 或其他外部进程路径用于 Subject/Truth/Ownership durable mutation 或持续 enforcement。r27 历史审计确认、并由当前 r50 保留的 `AutoAttach` / `AttachHelper` 可作为 bootstrap fallback。所有 process-launch site 必须在证据中分类并验证 handoff/termination。

---

# 3. 推荐包结构

P0-A 必须在当前命名空间 `dev.ronova.coremod` 内演进，禁止另建 `com.<project>.ronova.authority.p0a` 平行体系。当前源码复用/迁移矩阵：

| 当前源码类型 | 动作 | V1.3 目标 |
|---|---|---|
| `contract.SubjectIdentity` | **MIGRATE** | 拆成稳定 `SubjectKey` 与 metadata-only `SubjectIdentity`；旧 record 只作迁移输入，不长期并存 |
| `contract.DominionEpoch` | **KEEP** | 保持 `(generation,sequence)`，补齐 wire/schema 使用 |
| `root.api.DominionRootGateway` | **EXTEND** | 在现有 port 上增加 versioned envelope、idempotency 与 recovery；不新建第二个 Gateway |
| `root.api.DominionCommand` | **EXTEND/REVISE** | 使用 SubjectKey、expectedEpoch、command envelope；不新建 `RootWriteCommand` 平行层级 |
| `root.api.CommitReceipt` | **EXTEND/REVISE** | 增加 commandId、canonical hash、commitIndex、before/after epoch；保留唯一 durable receipt |
| 现有 `projection.*` | **ADAPT** | 通过单一 immutable `ProjectionView` 发布/读取；不再建第二个投影真相 |

目标职责树：

```text
dev.ronova.coremod
├── contract/
│   ├── RealmId.java
│   ├── SubjectKey.java
│   ├── SubjectIdentity.java              # metadata descriptor keyed by SubjectKey
│   ├── SubjectType.java
│   ├── PhysicalIncarnationId.java
│   ├── DominionEpoch.java                # reuse current type
│   ├── AuthorityResult.java
│   ├── AuthorityExtension.java
│   ├── AuthorityEnvelope.java
│   ├── AuthoritySnapshot.java
│   ├── CriticalOperationId.java
│   ├── CompletionContractId.java
│   ├── CompletionContract.java
│   └── RevisionCrossingPolicy.java
│
├── binding/
│   ├── BindingRevision.java
│   ├── IncarnationBinding.java
│   ├── OwnershipRevision.java
│   ├── OwnershipBinding.java
│   ├── ExecutionLease.java
│   ├── BindingState.java
│   └── BindingTransitionValidator.java
│
├── relationship/
│   ├── AuthorityRelationshipEdge.java
│   ├── RelationshipType.java
│   ├── RelationshipValidationState.java
│   ├── RelationshipEvidence.java
│   ├── RelationshipGraphSnapshot.java
│   ├── ResolverProvider.java
│   ├── ResolverProviderState.java
│   └── ResolverRegistry.java
│
├── root/api/                              # extend current canonical API
│   ├── DominionRootGateway.java
│   ├── DominionCommand.java
│   ├── RootCommandEnvelope.java
│   ├── RootSubmitResult.java
│   ├── RootRecoveryResult.java
│   ├── RootCommandId.java
│   ├── RootCommitIndex.java
│   └── CommitReceipt.java
├── root/wire/
│   ├── RootEnvelopeCodec.java
│   └── RootSchemaV1.java
├── root/migration/
│   └── P0ASchemaMigration.java
│
├── projection/
│   ├── DominionProjection.java
│   ├── ProjectionRevision.java
│   ├── SubjectProjectionEntry.java
│   ├── ProjectionPublisher.java
│   └── ProjectionView.java
│
├── policy/
│   ├── AuthorityMode.java
│   ├── ModeSelector.java
│   ├── PolicyOverlay.java
│   ├── PolicyRevision.java
│   ├── ConstitutionalInvariant.java
│   └── PolicyResolver.java
│
├── admission/
│   ├── PreAdmissionBindingState.java
│   ├── AdmissionDisposition.java
│   └── PreAdmissionCoordinator.java
│
├── lifecycle/
│   ├── LifecycleTruth.java
│   ├── ResolutionState.java
│   ├── TombstoneState.java
│   └── TombstoneRecord.java
│
├── service/
│   ├── TruthAuthorityService.java
│   ├── IdentityBindingService.java
│   ├── OwnershipService.java
│   ├── IncarnationService.java
│   ├── ProjectionService.java
│   └── PolicyAuthorityService.java
│
└── integrity/fixtures/                    # P0-D minimum crosscut only
    ├── CanonicalEnvelopeFixtures.java
    ├── FencedSnapshotFixtures.java
    └── TamperNegativeFixtures.java
```

路径可随现有模块边界微调，但 canonical 类型只能有一份；任何适配器都必须薄且只做转换，不能持有第二套 truth。

---

# 4. SubjectKey / SubjectIdentity

## 4.1 数据结构

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

## 4.2 SubjectType

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

## 4.3 Identity Resolution

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

# 5. PhysicalIncarnationId

## 5.1 目的

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

## 5.2 推荐结构

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

## 5.3 Incarnation 状态

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

# 6. Incarnation Binding

## 6.1 核心记录

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

## 6.2 Bind 规则

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

## 6.3 Rebind

Rebind 必须：

1. Root-authorized；
2. 产生新 BindingRevision；
3. 旧 incarnation 进入 SUPERSEDED/STALE；
4. Projection 原子发布；
5. P0-B/P0-C 观察到 revision 变化后才使用新 binding。

---

# 7. BindingRevision / OwnershipRevision

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

# 8. OwnershipBinding

## 8.1 目的

Ownership 表示：

> 哪个逻辑 Owner 对某项状态具有 Authority 语义。

---

## 8.2 第一版 Scope

P0-A V1.3 只实现：

```text
SUBJECT_SELF
KNOWN_MANAGER
KNOWN_ROOT_OWNED_STATE
```

不要马上支持任意第三方 Manager graph。

---

## 8.3 数据结构

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

## 8.4 OwnershipState

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

# 9. ExecutionLease

ExecutionLease 不等于 Ownership。`ExecutionHolderId` 只标识 JVM 内当前获准执行该 Domain 的 executor instance，不表示网络 session、外部进程或第二个 Root。

```java
public record ExecutionLease(
        ExecutionHolderId holderId,
        SubjectKey subjectKey,
        StateDomainId stateDomain,
        LeaseSequence leaseSequence,
        LeaseDeadline deadline,
        BindingRevision bindingRevision,
        OwnershipRevision ownershipRevision,
        RootInstanceId rootInstanceId,
        FenceToken fenceToken
) {}
```

第一版可以只提供接口和数据模型，不必须实现完整租约续期。

规则：

- lease 过期 ≠ ownership 消失；
- lease/root-instance stale → P0-B execution denied；
- TERMINAL truth 不受 lease 生命周期影响。

---

# 10. LifecycleTruth / ResolutionState / AdmissionDisposition

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

# 11. TombstoneRecord

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

# 12. DominionEpoch

继续采用：

```text
DominionEpoch = (generation, sequence)
```

建议：

```java
public record DominionEpoch(
        long generation,
        long sequence
) implements Comparable<DominionEpoch> {}
```

比较：

```text
generation first
then sequence
```

禁止与：

- commitIndex；
- commandSequence；
- fenceToken；

混用。

---

# 13. Root Gateway

本节扩展当前 `dev.ronova.coremod.root.api`，目标为 JVM 内 `InternalDominionRoot`，不创建网络 transport。

## 13.1 Canonical Interface

```java
public interface DominionRootGateway {
    AuthorityResult<RootReadView> readSubject(SubjectKey subjectKey);
    RootSubmitResult submit(RootCommandEnvelope envelope);
    RootRecoveryResult recover(RootCommandId commandId, CanonicalPayloadHash hash);
    AuthorityResult<RootHead> head();
}
```

```java
public record RootCommandEnvelope(
        RootCommandId commandId,
        RealmId realmId,
        RootInstanceId rootInstanceId,
        FenceToken fenceToken,
        long commandSequence,
        DominionEpoch expectedEpoch,
        CanonicalPayloadHash canonicalPayloadHash,
        DominionCommand command
) {}
```

要求：

- Root API 仅允许 `InternalDominionRoot` implementation 推进 epoch；
- Store writer API 不暴露给 Domain；
- stale root instance / fence / expected epoch → explicit `REJECTED/FENCED`；
- realtime projection read 不通过此 mutation API。

## 13.2 DominionCommand

第一版继续使用 canonical sealed `DominionCommand`，包括：

```text
CREATE_SUBJECT
BIND_INCARNATION
REBIND_INCARNATION
BIND_OWNERSHIP
REVOKE_OWNERSHIP
TERMINALIZE
CLEAR_TOMBSTONE
SET_POLICY_MODE
```

## 13.3 Closed Submit / Recovery Results

```text
RootSubmitResult
├ COMMITTED(CommitReceipt)
├ REJECTED(reason, observedEpoch?, observedFence?)
└ COMMIT_UNKNOWN(commandId, canonicalPayloadHash)

RootRecoveryResult
├ COMMITTED(CommitReceipt)
├ NOT_COMMITTED(commandId, canonicalPayloadHash)
└ STILL_UNKNOWN(commandId, canonicalPayloadHash)
```

`CommitReceipt` 至少包含 commandId、canonical hash、commitIndex、before/after epoch、durable state digest 与 projection publication input。

# 14. RootCommandId 与幂等

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

Canonical payload hash 采用 L6E-0 §5 的稳定编码；不得用 Java serialization、Map 迭代顺序或对象 identity 计算。

---

# 15. COMMIT_UNKNOWN

Internal Root 调用也可能在 durable write 与 receipt 返回之间发生 JVM crash / I/O ambiguity。

```text
submit(command)
→ result ambiguous
→ COMMIT_UNKNOWN(commandId, canonicalPayloadHash)
```

调用方只能：

```text
recover(same commandId, same hash)
or
resend(same RootCommandEnvelope)
```

禁止 new commandId retry。不可逆 physical transition 必须等 durable resolved state。

# 16. AuthorityEnvelope

canonical envelope 完全复用 L6E-0 V1.3 Internal Root §5，不在 P0-A 定义弱化版本：

```java
public record AuthorityEnvelope(
        ProtocolVersion protocolVersion,
        SchemaVersion schemaVersion,
        AuthorityMessageType messageType,
        RealmId realmId,
        SubjectKey subjectKey,
        SubjectIdentity subjectMetadata,
        PhysicalIncarnationId incarnationId,
        CriticalOperationId operationId,
        CompletionContractId completionContractId,
        BindingRevision bindingRevision,
        OwnershipRevision ownershipRevision,
        ProjectionRevision projectionRevision,
        DominionEpoch dominionEpoch,
        DominionEpoch expectedEpoch,
        RootCommandId commandId,
        RootCommitIndex commitIndex,
        PolicyRevision policyRevision,
        long flags,
        List<AuthorityExtension> extensions
) {}
```

实际 canonical 编码/持久化/fixture 时：

- message-specific requiredness 严格采用 L6E-0 表；缺失即 `INVALID_ENVELOPE`；
- optional 字段必须显式 presence，absent 与 empty 不同；
- `extensions` 按 `(namespace,name,version)` 唯一且 canonical sort；payload defensive-copy；
- 单 extension 64 KiB、每 envelope 64 个、总 payload 256 KiB；
- mandatory unknown extension fail closed；
- optional unknown extension safe ignore；
- dormant future semantic 只能通过 versioned extension 协商，不能塞进无 reader/writer 的空 core 字段。

---

# 17. Projection Model

## 17.1 DominionProjection

```text
DominionProjection
├ projectionRevision
├ dominionEpoch
├ commitIndex
├ subjects[]
├ policyRevision
└ integrityMetadata
```

Projection 必须：

- immutable；
- atomic swap；
- readers lock-free 或低锁；
- 不被 Domain 原地 mutate。

---

## 17.2 SubjectProjectionEntry

```java
public record SubjectProjectionEntry(
        SubjectKey subjectKey,
        SubjectIdentity subjectMetadata,
        LifecycleTruth lifecycleTruth,
        ResolutionState resolutionState,
        PhysicalIncarnationId activeIncarnation,
        BindingRevision bindingRevision,
        List<OwnershipBinding> ownershipBindings,
        AuthorityMode mode,
        PolicyRevision policyRevision,
        FenceState fenceState
) {}
```

第一版不要把完整 Relationship Graph 全塞进 hot projection。

---

# 18. Projection Publication

推荐：

```text
Root Commit
    ↓
Projection Build
    ↓
Validate
    ↓
Publish new immutable revision
    ↓
Atomic swap
```

必须满足 Canonical L6 D6：

> 一个 Judgment / Root batch 不得导致 N 次全量 projection rebuild。

并满足 V2.4 D9：`1 JudgmentBatch = 1 commandId = 1 atomic Root transaction = 1 DominionEpoch transition = 1 commitIndex = 1 durable receipt = ≤1 projection publication`。

应支持：

- batch build；
- incremental update；
- bounded publication cost。

---

# 19. ProjectionView

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

# 20. AuthoritySnapshot

P0-A 向 P0-B 输出：

```java
public record AuthoritySnapshot(
        SubjectKey subjectKey,
        SubjectIdentity subjectMetadata,
        PhysicalIncarnationId activeIncarnation,
        BindingRevision bindingRevision,
        Map<StateDomainId, OwnershipBinding> ownershipBindings,
        ProjectionRevision projectionRevision,
        PolicyRevision policyRevision,
        DominionEpoch epoch,
        LifecycleTruth lifecycleTruth,
        ResolutionState resolutionState,
        AuthorityMode mode,
        FenceState fenceState
) {}
```

要求：

- immutable；
- 由一个 `ProjectionView` 原子构造，并在单 operation 内复用；
- ownership map 与 metadata 必须 defensive-copy / immutable；
- snapshot 不允许反写 P0-A。

---

# 21. TruthAuthorityService

```java
public interface TruthAuthorityService {
    AuthorityResult<AuthoritySnapshot> resolve(ProjectionView view, SubjectKey subjectKey);
    AuthorityResult<AuthoritySnapshot> resolveByIncarnation(
            ProjectionView view,
            PhysicalIncarnationId incarnation
    );
    AuthorityResult<LifecycleTruth> lifecycle(ProjectionView view, SubjectKey subjectKey);
}
```

刻意不提供 `boolean isTerminal()`：`UNKNOWN ≠ false`。调用方必须穷尽处理 `AuthorityResult` 的 `RESOLVED/STALE/UNKNOWN/FENCED/...` 状态。

---

# 22. IdentityBindingService

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

# 23. Incarnation Discovery

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

# 24. Pre-Admission Binding

直接实现 L6E-0 V1.3 §9 的状态机：

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

# 25. Terminal-aware Binding

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

# 26. ResolverProvider SPI

## 26.1 接口

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

## 26.2 ResolverResult

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

# 27. AuthorityRelationshipEdge

建议：

```java
public record AuthorityRelationshipEdge(
        UUID edgeId,
        SubjectKey subjectKey,
        String sourceNode,
        String targetNode,
        RelationshipType type,
        RelationshipValidationState validationState,
        long edgeRevision,
        BindingRevision bindingRevision,
        ResolverProviderId providerId,
        ResolverProviderRevision providerRevision,
        Provenance provenance,
        double confidence
) {}
```

---

# 28. Relationship Validation State

```text
DISCOVERED
EVIDENCE_ACCUMULATED
VALIDATED
BOUND
AMBIGUOUS
OPAQUE
STALE
INVALID
```

只有：

```text
VALIDATED / BOUND 且 provider evidence 未 stale
```

可被 OwnershipService 使用。

---

# 29. Graph Persistence Policy

Root 不持久化所有临时 graph。

Root 只持久化：

- SubjectKey + SubjectIdentity metadata；
- durable OwnershipBinding；
- durable Incarnation binding revision；
- lifecycle/tombstone；
- policy revision。

本地 Graph 可：

```text
rebuild
evict
revalidate
```

避免把 discovery noise 变成 Root 垃圾。

---

# 30. Policy Model

## 30.1 AuthorityMode

```text
OBSERVATIONAL
NON_BLOCKING
HARDENED
```

---

## 30.2 ModeSelector

```text
ModeSelector(realmId?, dimensionId?, subjectClass?, subjectKey?, stateDomainId?)
```

`stateDomainId` 与主体定位维度正交，不是继承链末端。

---

## 30.3 PolicyOverlay

```java
public record PolicyOverlay(
        PolicyRevision revision,
        ModeSelector selector,
        AuthorityMode mode,
        Map<PolicyParameterId, PolicyValue> parameters
) {}
```

---

# 31. Policy Resolution

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

# 32. Constitutional Invariants

第一版冻结：

```text
TERMINAL precedence
Epoch monotonicity
COMMIT_UNKNOWN semantics
BindingRevision monotonicity
Tombstone durability
Root writer uniqueness
Stale incarnation denial
HARDENED superset rule
```

PolicyCompiler 发现冲突：

```text
POLICY_REJECTED
```

而不是 runtime 临时猜优先级。

---

# 33. Mode 切换

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

# 34. P0-A 与 Non-Blocking Dominion

P0-A 必须保证即使关闭 active mutation interception：

```text
Truth
Identity
Binding
Ownership
Lifecycle
Projection
```

仍完整工作。

这意味着 P0-A 不能依赖：

- setHealth hook 成功；
- hurt hook 成功；
- remove hook 成功；
- AllReturn 成功。

---

# 35. P0-A 与 P0-B 的边界

P0-B 可以问：

```text
subject?
incarnation?
binding revision?
ownership?
mode?
fence?
```

P0-A 返回 snapshot。

P0-B 不能：

```text
直接修改 SubjectKey / SubjectIdentity metadata
重写 BindingRevision
清 Tombstone
```

---

# 36. P0-A 与 P0-C 的边界

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

# 37. P0-A 与 P0-D 的边界

P0-A 不实现完整 P0-D，但从第一天交付以下最小横切面：

- 所有 wire/schema value object 的 structural validator；
- canonical envelope golden round-trip fixture；
- single-bit tamper / stale root-instance / stale fence negative fixtures；
- immutable collection 与 defensive-copy tests；
- 可构造的 `FENCED` ProjectionView/AuthoritySnapshot fixture；
- 每个 critical command/result/receipt 的 schema revision 与 provenance slot。

P0-D 负责验证：

- Projection 是否可信；
- class/seam 是否可信；
- Native/Java reader 是否可信；
- Root command boundary / durable recovery path 是否完整。

P0-A 遇到：

```text
P0-D says FENCED
```

必须向 P0-B/P0-C 输出：

```text
FENCED snapshot
```

不能静默继续 ACTIVE。

---

# 38. Failure Model

查询/解析 API 统一返回封闭 `AuthorityResult<T>`：

```text
RESOLVED(T)
PARTIALLY_RESOLVED(T, evidence)
NOT_FOUND(verifiedAtRevision)
AMBIGUOUS(details)
OPAQUE(details)
STALE(observedRevision)
INVALID(reason)
UNKNOWN(reason)
FENCED(fenceState)
```

`NOT_FOUND` 只允许在指定 Root/Projection revision 上确认不存在时返回；没有查到、Root 不可达或 projection 未覆盖都必须是 `UNKNOWN`。

Root mutation 使用 §13.3 的 `RootSubmitResult/RootRecoveryResult`；transition 使用 `AuthorityResult<BindingOutcome>`；schema/policy error 作为封闭 reason code（`UNSUPPORTED_SCHEMA | INVALID_TRANSITION | POLICY_REJECTED | ROOT_UNAVAILABLE | COMMAND_ID_CONFLICT`）承载，不能把不同层级塞进一个随意 string/enum 大桶。

避免：

```text
null / false / Optional.empty() / generic Exception
```

承担核心 Authority 语义。

---

# 39. Fail-Closed / Fail-Open 策略

## Root unavailable / fenced

对 critical subject：

```text
NO NEW DURABLE MUTATION
```

Internal Root component / durable store 若 FENCED、无法打开或状态无法验证：

- 已缓存 Projection 只可按 declared age/fence policy 继续读；
- authority-sensitive admission/execution 遇到 STALE/UNKNOWN 必须 HELD/FENCED；
- 不能自造 binding revision；
- 不能清 Tombstone；
- 恢复后必须先重建 Root head / projection，再恢复 ACTIVE。

## Unknown schema

mandatory semantic unknown：

```text
FAIL CLOSED
```

optional extension unknown：

```text
SAFE IGNORE
```

---

# 40. Cache 设计

允许缓存：

- SubjectKey → SubjectIdentity metadata lookup；
- incarnation → subject；
- immutable projection entry；
- validated relationship edge。

禁止缓存：

- 可被当作 durable truth 的 mutable local object；
- 无 revision 的 ownership result；
- 无 expiry/revision 的 resolver guess。

---

# 41. 并发模型

P0-A 的 hot read：

```text
projection snapshot read
```

应尽量 lock-free / read-mostly。

Root mutation：

```text
serialized by Root
```

本地只负责：

```text
publish immutable revision
```

避免：

```text
多个 Domain 同时 mutate shared HashMap truth
```

---

# 42. Java 内存模型要求

所有 Projection root reference：

```text
volatile / VarHandle release-acquire / AtomicReference
```

必须保证：

```text
新 Projection 内容完全构造
→ 再 publish root reference
```

禁止 partial publication。

---

# 43. Native Shared Projection 预留

P0-A V1.3 只冻结接口：

```text
ProjectionBackend
├ JAVA_IMMUTABLE
└ NATIVE_SHARED_READONLY
```

当前实现可仍使用 Java immutable projection。

切换 Native backend 时：

> P0-B/P0-C consumer API 不变。

这里的 `NATIVE_SHARED_READONLY` 仅指 active Minecraft JVM 内 Java↔Native 的只读投影；不得借此把 OS shared-memory、Named Pipe、local-loopback 或 helper JVM 提升为 Authority truth channel。bounded bootstrap/relaunch 不改变这一点。

---

# 44. Migration from FULL L6 Internal

迁移顺序：

```text
1. Extend existing root/api envelope and receipt
2. Migrate current SubjectIdentity → SubjectKey + metadata
3. Introduce BindingRevision / OwnershipRevision / PhysicalIncarnationId
4. Add immutable ProjectionView and dual-read validation
5. Add P0-D minimum fixtures
6. Implement P0-A schema inside the already-authoritative InternalDominionRoot
7. Shadow compare old projection and P0-A projection
8. Cut over consumers through one cutover state machine
9. Remove legacy adapters only after invariant/runtime proof
```

FULL L6 Internal 的 `InternalDominionRoot` 在进入 P0-A 前已经是唯一逻辑 Authority writer；P0-A 只是升级 schema/identity/binding/ownership contract，不再次迁移 Root。

# 45. Migration from 7.14A Frozen Baseline / r50 Current Line

7.14A 冻结基线不直接实现本文；r27 仅作为历史源码审计输入，当前 r50 working source 作为现行迁移输入，两者都不改变本文的 P0-A 设计范围。

未来迁移时必须保持：

- Raw Observation separation 不回退；
- Terminal 不让 Raw forget；
- existing reliable hooks 先保留；
- P0-A 先以 shadow/parallel read 方式验证。

本节只规定从当前实现继承的可靠不变量和证据，不构成跳过 FULL L6 的实施路线。7.14A 不直接启动 Internal L6 Root consolidation、Execution Lease 或 L6E admission cutover。

禁止：

```text
一次性把 current local runtime 全替换
```

---

# 46. Telemetry

P0-A 必须提供 O(1) 聚合指标：

```text
subjectCount
activeIncarnationCount
staleIncarnationCount
terminalSubjectCount
bindingConflictCount
opaqueRelationshipCount
projectionRevision
projectionAge
rootCommitIndex
commitUnknownCount
```

不要在 hot path：

```text
snapshot entire history
```

---

# 47. Audit

关键 Root mutation 记录：

```text
commandId
subject
oldRevision
newRevision
operation
epoch
commitIndex
result
```

Audit 不得成为 Root Truth 本体。

Audit 写失败：

- 不应自动回滚已 durable commit；
- 但必须产生 Integrity incident。

---

# 48. Security / Integrity Notes

P0-A 不把 off-heap/native/encrypted bytes/hidden class 当成绝对可信边界。

Internal Root 路线的 durable trust boundary 仍位于 Minecraft JVM 内：

```text
InternalDominionRoot
      ↓
DurableRootStore
```

它提供架构 single-writer、revision/fence、anti-rollback、dedup/recovery 与 fail-static，但**不提供 JVM 外进程隔离**。若对手完全控制同一 JVM/JVMTI/native address space，P0-D Integrity/repair 只能提高持续篡改成本，不能把其描述成物理不可达 Root。

进程角色边界必须显式可审计：P0-A 可触发 bounded helper/relaunch，但 helper 不得承载 Subject/Truth/Ownership state 或 continuing enforcement。Native/Agent 不可用时可以 attach/relaunch；若仍无法建立合规 active JVM，则转为 `DEGRADED` / `FENCED` 并留下 reason code。

# 49. Unit Tests

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

## U-01 SubjectKey Stability

同 Subject 两个不同 Java object：

```text
SubjectKey must remain same even when metadata differs
incarnation must differ
```

## U-02 Incarnation Monotonicity

新 incarnation sequence 必须单调。

## U-03 Stale Rejection

旧 incarnation 在 rebind 后：

```text
resolve → STALE
```

## U-04 Tombstone Dominance

TERMINAL Subject 新 incarnation：

```text
bind → TERMINAL_REJECT
```

## U-05 Binding Revision

旧 revision 更新：

```text
INVALID_TRANSITION
```

## U-06 Root Command Idempotency

同 commandId / same payload：

```text
same semantic result
```

## U-07 Command Conflict

同 commandId / different payload：

```text
COMMAND_ID_CONFLICT
```

## U-08 Projection Atomicity

Reader 不得看到 half-built projection。

## U-09 Unknown Optional Extension

安全忽略。

## U-10 Unknown Mandatory Extension

`UNSUPPORTED_SCHEMA`

## U-11 Lifecycle / Resolution Orthogonality

`ACTIVE + UNKNOWN`、`TERMINAL + RESOLVED`、`ACTIVE + OPAQUE + HELD` 均可表达；任何路径不得退化成 boolean/Optional。

## U-12 Revision Scope

BindingRevision 只按 SubjectKey 比较；OwnershipRevision 只按 `(SubjectKey,StateDomainId)` 比较；跨 scope 比较必须拒绝。

## U-13 Canonical Wire / Defensive Copy

覆盖字段 tag、presence bits、NFC、extension sort/limits、同 commandId 不同 hash、payload defensive copy 与 golden round-trip。

## U-14 Immutable ProjectionView

并发 publish 下，一个 view 的 revision/epoch/policy/fence/entry 必须同源，不能 torn read。

## U-15 Provider Lifecycle

provider revoke/disappear/throw/timeout 后 evidence stale 且隔离；其他 provider 不受影响。

## U-16 Pre-Admission State Machine

覆盖全部合法/非法 transition、Root unavailable、hold budget 与五类 Forge seam。

## U-17 Production Process Boundary

对 production bootstrap 做 process-tree capture，并对生产源码、依赖与打包产物做 forbidden API/import scan。必须证明：

```text
all process-launch sites enumerated and role-bounded
AutoAttach / AttachHelper allowed only for bootstrap/attach/relaunch handoff
no external Authority writer / persistent combat plane
no Named Pipe / local loopback / OS shared-memory IPC
Native unavailable → same-JVM DEGRADED/FENCED
```

构建、测试、profiling、Forge runner 与普通 Minecraft client/server 进程继续在报告中单独标记；production helper/relaunch 必须显式标记 purpose、lifetime 与 Authority-role。

---

# 50. Integration Tests

## I-01 Entity Rebuild

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

## I-02 Terminal Rebuild Attempt

```text
Subject X terminal
→ new Entity same UUID
```

要求：

```text
no ACTIVE binding
```

---

## I-03 Projection Lag

Root revision N+1 已提交，本地仍 N：

要求：

- snapshot 带明确 ProjectionRevision；
- P0-B 可识别 stale；
- 本地不得假装 N+1 已生效。

---

## I-04 Root Receipt Ambiguity

要求进入：

```text
COMMIT_UNKNOWN
```

并使用同 commandId 解析。

---

## I-05 Mode Change

```text
NON_BLOCKING → HARDENED
```

要求：

- 产生新 PolicyRevision；
- Projection 更新；
- Non-Blocking core 仍启用。

---

## I-06 Relationship Opaque

Resolver 找到未知 holder：

```text
OPAQUE
```

要求：

- 不自动写；
- 不自动 BOUND；
- Policy 决定后续。

## I-07 Root Schema / Recovery

真实 Root handler 覆盖 expectedEpoch、stale root-instance/fence、dedup store、COMMIT_UNKNOWN recovery、COMMAND_ID_CONFLICT 与 durable receipt replay。

## I-08 P0-D Minimum Crosscut

所有 P0-A schema fixture 通过 structural/canonical checks；single-bit tamper、stale root-instance/fence 与 mutable collection fixture 必须 FAIL/FENCED。

---

# 51. Chaos Tests

## C-01 Reordered Projection Arrival

模拟旧 projection 晚到：

```text
revision 12
after 13
```

要求：

```text
12 rejected
```

---

## C-02 Duplicate Root Response

重复 `CommitReceipt` / recovery result：

```text
must remain idempotent
```

---

## C-03 JVM Restart

重启后：

- BindingRevision 不重置；
- Tombstone 保留；
- SubjectKey 与 durable metadata 保留；
- stale incarnation 不复活。

---

## C-04 Partial Relationship Cache Loss

本地 Graph 丢失：

```text
Root Truth must remain unchanged
```

Graph 可重建。

---

## C-05 Internal Root / Store Temporary Unavailable

要求：

- no new durable truth；
- current read-only projection 可按 age/fence policy 继续；STALE/UNKNOWN admission 必须 HELD；
- critical mutation 不得本地自授权。

---

# 52. Performance Gates

P0-A 第一版必须设可复现预算。归档固定硬件/OS/JVM/参数，warmup 60 s、measurement 300 s；与同环境冻结基线比较：

```text
Projection lookup:
O(1) expected; p95 ≤ 2 × baseline lookup p95 and allocation = 0 B/op after warmup

Incarnation → Subject lookup:
O(1) expected; p95 ≤ 2 × baseline lookup p95

Subject → Projection entry:
O(1) expected; no lock/contention regression in 1/4/16 reader threads

Root mutation:
not on per-tick hot path; durable I/O ambiguity must return/recover COMMIT_UNKNOWN

Relationship graph discovery:
bounded / asynchronous / budgeted

server tick p50/p95/p99:
≤ frozen baseline +5% / +5% / +10%

1k / 10k / 100k subjects:
command+commit+projection p95(100k) ≤ max(8 × p95(1k), p95(1k) + 10 ms)
```

必须归档 raw samples、异常值规则、projection allocation、Root submit/commit 与 batch size。任何同步 durable Root I/O 出现在 Minecraft tick/admission hot path 都直接 FAIL，不以平均值掩盖。

禁止：

- 每 tick 全量扫描所有 Subject；
- 每 operation rebuild 全 projection；
- 每 action deep-copy 全 graph/history。

---

# 53. Persistence Rules

Root durable：

```text
SubjectKey + SubjectIdentity metadata
Tombstone
IncarnationBinding
BindingRevision
OwnershipRevision
OwnershipBinding
PolicyRevision
DominionEpoch
CommitIndex
RootCommandId + CanonicalPayloadHash dedup record
CommitReceipt / unresolved recovery record
```

Local ephemeral：

```text
Java object reference
resolver transient evidence
relationship discovery cache
diagnostic history
hot-path metrics
```

---

# 54. Data Retention

对 ephemeral history：

```text
bounded
ring-buffered
or sampled
```

避免：

```text
unbounded completed history
```

所有诊断历史都必须声明 bound/eviction/overflow telemetry；当前 `PassiveTerminalWatchSet` 已是 1,024 条有界历史，本节保留这一约束而不是重复旧的无界增长描述。

---

# 55. Logging Rules

核心状态转换：

```text
INFO / structured audit
```

高频 read：

```text
no per-read logging
```

Resolver noise：

```text
sampled / debug
```

Storm 下：

```text
rate-limited
```

---

# 56. Feature Flags

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

# 57. Backward Compatibility

只有 call-site inventory 证明仍有旧 API 消费者时，才允许临时 facade：

```text
LegacyEntityAuthorityFacade
```

映射到：

```text
SubjectKey + SubjectIdentity metadata
AuthorityResult<AuthoritySnapshot>
```

禁止长期维护两套 truth。

facade 必须 read-only、不得折叠 Unknown、不得持久化，且带 owner/deletion milestone。若 inventory 为 0，不创建它。Migration 完成后若仍短暂存在：

```text
legacy facade = read adapter
```

而不是第二个 writer。

---

# 58. P0-A V1.3 Deliverables

工程实现至少交付：

1. `RealmId`、`SubjectKey`、metadata-only `SubjectIdentity`、`SubjectType`
2. `PhysicalIncarnationId`、`IncarnationBinding`、`BindingRevision`
3. `StateDomainId`、`AuthorityOwnerKey`、`OwnershipBinding`、`OwnershipRevision`
4. `ExecutionLease`、`ExecutionHolderId`、`LeaseSequence`、`LeaseDeadline`
5. reuse 的 `DominionEpoch`、`FenceState/FenceToken`、RootInstanceId / writer-fence identifiers
6. `RootCommandId`、`RootCommitIndex`、`RootHead`、`RootReadView`、`CanonicalPayloadHash`、`RootCommandEnvelope`
7. extended sealed `DominionCommand`、`RootSubmitResult`、`RootRecoveryResult`、`CommitReceipt`
8. `AuthorityExtension`、`AuthorityEnvelope`、canonical codec/hasher 与 schema migration
9. extended `DominionRootGateway` + Root-side handlers、dedup/recovery store、schema tables、migration/rollback tooling
10. `DominionProjection`、`SubjectProjectionEntry`、immutable `ProjectionView`、atomic publisher/builder
11. `AuthorityResult<T>`、`AuthoritySnapshot`、`CriticalOperationId`、`CompletionContractId`、`CompletionContract`、`RevisionCrossingPolicy`、`TruthAuthorityService`
12. `BindingOutcome`、`IdentityBindingService`
13. `LifecycleTruth`、`ResolutionState`、`TombstoneRecord`、`PreAdmissionBindingState`、`AdmissionDisposition`、`IncarnationCandidate`
14. `PolicyRevision`、`ModeSelector`、`PolicyOverlay`、`AuthorityMode`
15. `ResolverProvider`、`ResolverProviderId`、`ResolverProviderRevision`、`ResolverProviderState`、`ResolverInput/Result`、`AuthorityRelationshipEdge` SPI 骨架及 lifecycle/revoke/stale isolation
16. P0-D minimum structural/canonical/fenced/tamper fixtures
17. model/property、wire/contract、integration/chaos 三类 suites 与三层执行入口
18. cutover state machine、仅在 inventory 需要时的 read-only migration facade
19. O(1) metrics、audit、operating-envelope benchmark
20. `PROC-INV-1` 的 process-launch inventory、production process-tree、helper handoff/relaunch 与 Authority-role fixtures

---

# 59. P0-A V1.3 明确不交付

以下留给后续：

- 全量 arbitrary Manager graph；
- Native shared-memory backend；
- 把 helper JVM、companion process 或其他跨进程 Root/IPC 路径用作 Authority；bounded bootstrap/attach/recovery/relaunch helper 仍受 `PROC-INV-1` 允许，但不得成为 Root；
- Execution Capability；
- AllReturn；
- CompletionReceipt enforcement；
- Physical Reconstitution；
- Side-Effect compensation engine；
- Semantic Class Seal；
- Render Client Agent；
- Native Render Firewall；
- T4 same-process native absolute protection。

---

# 60. P0-A Exit Criteria

只有全部通过，P0-A 才算完成：

## Identity

- SubjectKey 是唯一 equality/hash/Root key，metadata 不参与相等；
- 当前 SubjectIdentity 已迁移/适配，无平行 logical identity；
- Subject 与 Java object 解耦；
- incarnation 可替换；
- stale incarnation 不继承 Authority。

## Binding

- BindingRevision 单调；
- OwnershipRevision 按 domain 单调且不与 BindingRevision 混用；
- rebind 原子；
- old binding 不可回滚。

## Lifecycle

- Tombstone durable；
- TERMINAL dominance 有测试证明；
- new incarnation 无法绕 Tombstone。

## Root

- 复用现有 root/api，Root-side schema/handler/dedup/recovery/migration 均交付；
- Internal Root instance/fence/command-sequence 与 canonical P0-A envelope 绑定；
- command idempotency；
- COMMIT_UNKNOWN 及同 commandId/hash recovery；
- same id/different hash conflict；
- anti-replay / monotonic epoch。

## Projection

- immutable；
- atomic swap；
- ProjectionView 单 handle、revision/entry/epoch/policy/fence 无 tearing；
- no N rebuild per batch。

## Policy

- Mode Root-authorized；
- HARDENED 是 NON_BLOCKING 超集；
- Constitutional Invariants 不可被 overlay 破坏。

## Relationship

- Resolver 不自授权；
- OPAQUE 不猜写；
- Graph 不成为第二个 Root。

## Admission / Result Semantics

- 五类 Forge seam 有映射；hot path 零同步 durable I/O；Root boundary 只消费本地 immutable projection；
- Root unavailable 时 UNKNOWN/STALE 不放行；
- lifecycle/resolution/admission 三轴正交；
- Authority API 无 boolean/Optional/null 承载 Unknown。

## P0-D Minimum Crosscut

- structural/canonical/fenced/tamper fixtures 完整；
- stale root-instance/fence 与 payload tamper negative 全 FAIL；
- 本项不冒充完整 P0-D runtime 已完成。

## Performance

- hot lookup O(1) expected；
- no unbounded history；
- no hot full snapshot copy。

## Process Boundary

- P0-A production path 的所有 helper/relaunch 都有明确 purpose、bounded lifetime 与 handoff；
- `AutoAttach` / `AttachHelper` 若存在，仅用于 bootstrap/attach，不进入 Truth/Ownership durable dependency graph；
- external Authority writer / persistent combat plane 数为零；
- Native/Agent 不可用时只产生同 JVM `DEGRADED` / `FENCED`；
- process-tree、static forbidden-call/import scan 与打包产物检查均通过。

---

# 61. 下一阶段接口交接

P0-A 完成后，向 P0-B 提供：

```text
AuthoritySnapshot
ProjectionView
AuthorityResult<T>
ExecutionAuthorityContext input data
CriticalOperationId
CompletionContractId
BindingRevision
OwnershipRevision
ProjectionRevision
PolicyRevision
DominionEpoch
AuthorityMode
FenceState
```

P0-B 不再直接读取：

```text
raw mutable entity fields
local ad-hoc authority maps
legacy SavedData truth
```

作为最终 Authority。

---

# 62. Final Implementation Rule

P0-A 的代码实现必须持续满足：

> **Truth only flows one way：Root → Projection → Consumer。**

以及：

> **Physical Minecraft state、Resolver evidence、Java object identity、第三方 Manager field 都只能成为观察或绑定证据，不能反向偷偷升级成 durable Authority Truth。**

P0-A V1.3 的最终目标不是“一次性解决所有强力 Mod”，而是先把：

```text
Identity
Incarnation
Binding
Ownership
Revision
Root Truth
Projection
Policy
Process Boundary
```

这九个地基彻底锁死。

只有这九个地基稳定后，P0-B 的 Execution Capability、Dispatch、AllReturn，P0-C 的 Convergence/Reconstitution，以及 P0-D 的 Integrity Proof 才有可靠的 Authority 基础。
