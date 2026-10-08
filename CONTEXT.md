# Ronova

> 本表冻结架构与 L6E 设计术语，不表示所有目标类型或后续阶段能力已经实现；当前源码阶段以 [当前进度](docs/CURRENT-PROGRESS.md) 为准，后续规划见 [规划索引](docs/plans/README.md)。

Ronova separates durable judgment truth from the physical Minecraft objects through which that truth is observed and enforced. This glossary fixes the domain language used by the architecture plan and staged implementation specifications.

## Observation and visibility

**Raw Observation**:
Dominion's internal knowledge that a physical incarnation exists and how it is topologically related. It is never filtered by Judgment state, quarantine, Public Presence, or owner status.
_Avoid_: visible entities, active targets, unfiltered presence

**Public Presence**:
Whether a physical incarnation is exposed through Dominion-controlled external query, render, or tracking paths. It may hide quarantined objects but cannot change Raw Observation.
_Avoid_: observation, existence, loaded state

**Physical Incarnation**:
One concrete runtime entity object representing an identity or lineage at a particular time. Its disappearance does not erase durable Judgment truth, and a later incarnation may represent the same identity or lineage.
_Avoid_: tombstone, judgment record

## Judgment and terminal semantics

**Judgment Eligibility**:
Whether a raw-observed physical incarnation may enter a Judgment closure. Eligibility is a policy decision downstream of Raw Observation, not an indexing property.
_Avoid_: trackability, observability

**Tombstone**:
The authoritative durable fact that an identity or lineage has been judged terminal. Its logical lifetime ends only through an explicit clear operation, never because a physical incarnation disappears or retry attempts end.
_Avoid_: watch entry, retry record, weak reference

**Lifecycle Integration**:
The once-only incorporation of a Judgment into normal lifecycle effects such as death integration. It is distinct from repeatedly asserting terminal physical state.
_Avoid_: terminal retry, continuous enforcement

**State Reassertion**:
A repeatable and idempotent assertion that a physical incarnation still conforms to terminal truth. Repetition must not repeat once-only Lifecycle Integration effects.
_Avoid_: re-judgment, repeated death

**Terminal Watch**:
Runtime awareness of physical incarnations that match Tombstone truth so State Reassertion can remain bounded and resumable. A Terminal Watch never creates Judgment truth and never determines Public Presence.
_Avoid_: tombstone store, retry queue, judgment authority

## Authority and immunity

**SubjectKey**:
The sole stable logical key for equality, hashing, Root lookup, and durable foreign keys: `(RealmId, subjectId)`. Runtime class, dimension, position, provenance, and schema metadata never participate in SubjectKey equality.
_Avoid_: entity UUID alone, SubjectIdentity full-record equality, Java object identity

**SubjectIdentity**:
An immutable metadata descriptor anchored by a SubjectKey. It describes type, origin, schema, and provenance but is not a second logical key.
_Avoid_: identity key, physical incarnation, mutable entity snapshot

**BindingRevision**:
A Root-issued monotonic revision for `SubjectKey ↔ PhysicalIncarnationId`, comparable only within one SubjectKey.
_Avoid_: OwnershipRevision, ProjectionRevision, global sequence

**OwnershipRevision**:
A Root-issued monotonic revision for one `(SubjectKey, StateDomainId) ↔ AuthorityOwner` binding, comparable only within that pair.
_Avoid_: BindingRevision, policy version

**ProjectionRevision**:
The version of one immutable local projection lineage. It never replaces BindingRevision or OwnershipRevision and must be consumed through a single ProjectionView.
_Avoid_: RootCommitIndex, DominionEpoch, separately read cache version

**LifecycleTruth**:
The durable two-state fact `ACTIVE | TERMINAL`. Resolution uncertainty and admission quarantine are separate axes and never add lifecycle states.
_Avoid_: UNKNOWN lifecycle, QUARANTINED lifecycle, physical death flag

**ResolutionState**:
The explicit quality of a local authority resolution: resolved, partial, ambiguous, opaque, stale, invalid, or unknown. Unknown is never false or safe.
_Avoid_: boolean result, Optional.empty, lifecycle state

**ProjectionView**:
One immutable handle that fixes projection revision, epoch, policy revision, fence state, and entries together so a consumer cannot observe a torn authority snapshot.
_Avoid_: independent revision read, mutable cache, Root truth

**RootCommandId**:
The stable idempotency key for one Root command. The same id and canonical payload hash identify one retryable mutation; a different hash is a command conflict.
_Avoid_: message sequence, commit index, retry with a new id

**COMMIT_UNKNOWN**:
The unresolved result of durable-write or receipt-return ambiguity, including a Root timeout or JVM interruption. It permits only recovery or idempotent resend with the same command id and payload hash; it is neither committed nor rejected.
_Avoid_: failure, false, automatic retry with a new command

**Owner Immunity**:
The invariant that an owner or Dominion-self physical incarnation remains publicly present even when quarantine input says otherwise. The invariant belongs to Public Presence policy and does not depend on a caller-side shortcut.
_Avoid_: authority bypass, raw visibility
