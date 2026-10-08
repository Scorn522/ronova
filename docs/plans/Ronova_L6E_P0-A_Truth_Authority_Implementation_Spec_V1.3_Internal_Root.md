# 迁移导航：Ronova_L6E_P0-A_Truth_Authority_Implementation_Spec_V1.3_Internal_Root.md

本文件的完整原文已按 MS 三书职责逐段迁移；这里仅保留旧路径和旧标题锚点，**不再承载并行规划正文或旧规范位次**。

[当前规划入口](README.md) · [本文件完整映射](RESTRUCTURE-MAP.md#source-p0a) · [保真原始快照](../archive/plans-before-ms-restructure-r66/Ronova_L6E_P0-A_Truth_Authority_Implementation_Spec_V1.3_Internal_Root.md)

原标题、条款、ID、示例及历史状态均在新书中；本页不宣告任何实现或验证结果。

<a id="ronova--l6e-p0-a-truth-authority-implementation-spec-v13--internal-root"></a>

**旧 L1–L25：** [Ronova — L6E P0-A Truth Authority Implementation Spec V1.3 — Internal Root](MS-SKILLS-PLAN.md#orig-p0a-l0001)

<a id="0-上位依据"></a>

**旧 L26–L51：** [0. 上位依据](MS-SKILLS-PLAN.md#orig-p0a-l0026)

<a id="01-v13-closure-map"></a>

**旧 L52–L67：** [0.1 V1.3 Closure Map](MS-SKILLS-PLAN.md#orig-p0a-l0052)

<a id="1-p0-a-的唯一职责"></a>

**旧 L68–L89：** [1. P0-A 的唯一职责](MS-SKILLS-PLAN.md#orig-p0a-l0068)

<a id="2-p0-a-核心不变量"></a>

**旧 L90–L93：** [2. P0-A 核心不变量](MS-SKILLS-PLAN.md#orig-p0a-l0090)

<a id="a1--subject-identity-stability"></a>

**旧 L94–L104：** [A1 — Subject Identity Stability](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0094)

<a id="a2--incarnation-non-inheritance"></a>

**旧 L105–L120：** [A2 — Incarnation Non-Inheritance](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0105)

<a id="a3--binding-monotonicity"></a>

**旧 L121–L130：** [A3 — Binding Monotonicity](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0121)

<a id="a4--tombstone-dominance"></a>

**旧 L131–L151：** [A4 — Tombstone Dominance](MS-MSITEM-PLAN.md#orig-p0a-l0131)

<a id="a5--root-writer-uniqueness"></a>

**旧 L152–L159：** [A5 — Root Writer Uniqueness](MS-SKILLS-PLAN.md#orig-p0a-l0152)

<a id="a6--projection-is-not-truth"></a>

**旧 L160–L170：** [A6 — Projection Is Not Truth](MS-SKILLS-PLAN.md#orig-p0a-l0160)

<a id="a7--graph-is-not-root"></a>

**旧 L171–L182：** [A7 — Graph Is Not Root](MS-SKILLS-PLAN.md#orig-p0a-l0171)

<a id="a8--production-process-boundary-proc-inv-1"></a>

**旧 L183–L200：** [A8 — Production Process Boundary (`PROC-INV-1`)](MS-SKILLS-PLAN.md#orig-p0a-l0183)

<a id="3-推荐包结构"></a>

**旧 L201–L311：** [3. 推荐包结构](MS-SKILLS-PLAN.md#orig-p0a-l0201)

<a id="4-subjectkey--subjectidentity"></a>

**旧 L312–L313：** [4. SubjectKey / SubjectIdentity](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0312)

<a id="41-数据结构"></a>

**旧 L314–L343：** [4.1 数据结构](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0314)

<a id="42-subjecttype"></a>

**旧 L344–L366：** [4.2 SubjectType](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0344)

<a id="43-identity-resolution"></a>

**旧 L367–L394：** [4.3 Identity Resolution](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0367)

<a id="5-physicalincarnationid"></a>

**旧 L395–L396：** [5. PhysicalIncarnationId](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0395)

<a id="51-目的"></a>

**旧 L397–L414：** [5.1 目的](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0397)

<a id="52-推荐结构"></a>

**旧 L415–L433：** [5.2 推荐结构](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0415)

<a id="53-incarnation-状态"></a>

**旧 L434–L459：** [5.3 Incarnation 状态](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0434)

<a id="6-incarnation-binding"></a>

**旧 L460–L461：** [6. Incarnation Binding](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0460)

<a id="61-核心记录"></a>

**旧 L462–L478：** [6.1 核心记录](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0462)

<a id="62-bind-规则"></a>

**旧 L479–L497：** [6.2 Bind 规则](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0479)

<a id="63-rebind"></a>

**旧 L498–L509：** [6.3 Rebind](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0498)

<a id="7-bindingrevision--ownershiprevision"></a>

**旧 L510–L545：** [7. BindingRevision / OwnershipRevision](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0510)

<a id="8-ownershipbinding"></a>

**旧 L546–L547：** [8. OwnershipBinding](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0546)

<a id="81-目的"></a>

**旧 L548–L555：** [8.1 目的](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0548)

<a id="82-第一版-scope"></a>

**旧 L556–L569：** [8.2 第一版 Scope](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0556)

<a id="83-数据结构"></a>

**旧 L570–L584：** [8.3 数据结构](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0570)

<a id="84-ownershipstate"></a>

**旧 L585–L605：** [8.4 OwnershipState](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0585)

<a id="9-executionlease"></a>

**旧 L606–L633：** [9. ExecutionLease](MS-SKILLS-PLAN.md#orig-p0a-l0606)

<a id="10-lifecycletruth--resolutionstate--admissiondisposition"></a>

**旧 L634–L649：** [10. LifecycleTruth / ResolutionState / AdmissionDisposition](MS-MSITEM-PLAN.md#orig-p0a-l0634)

<a id="11-tombstonerecord"></a>

**旧 L650–L674：** [11. TombstoneRecord](MS-MSITEM-PLAN.md#orig-p0a-l0650)

<a id="12-dominionepoch"></a>

**旧 L675–L708：** [12. DominionEpoch](MS-SKILLS-PLAN.md#orig-p0a-l0675)

<a id="13-root-gateway"></a>

**旧 L709–L712：** [13. Root Gateway](MS-SKILLS-PLAN.md#orig-p0a-l0709)

<a id="131-canonical-interface"></a>

**旧 L713–L743：** [13.1 Canonical Interface](MS-SKILLS-PLAN.md#orig-p0a-l0713)

<a id="132-dominioncommand"></a>

**旧 L744–L758：** [13.2 DominionCommand](MS-SKILLS-PLAN.md#orig-p0a-l0744)

<a id="133-closed-submit--recovery-results"></a>

**旧 L759–L774：** [13.3 Closed Submit / Recovery Results](MS-SKILLS-PLAN.md#orig-p0a-l0759)

<a id="14-rootcommandid-与幂等"></a>

**旧 L775–L798：** [14. RootCommandId 与幂等](MS-MSITEM-PLAN.md#orig-p0a-l0775)

<a id="15-commit_unknown"></a>

**旧 L799–L818：** [15. COMMIT_UNKNOWN](MS-SKILLS-PLAN.md#orig-p0a-l0799)

<a id="16-authorityenvelope"></a>

**旧 L819–L858：** [16. AuthorityEnvelope](MS-SKILLS-PLAN.md#orig-p0a-l0819)

<a id="17-projection-model"></a>

**旧 L859–L860：** [17. Projection Model](MS-SKILLS-PLAN.md#orig-p0a-l0859)

<a id="171-dominionprojection"></a>

**旧 L861–L881：** [17.1 DominionProjection](MS-SKILLS-PLAN.md#orig-p0a-l0861)

<a id="172-subjectprojectionentry"></a>

**旧 L882–L902：** [17.2 SubjectProjectionEntry](MS-SKILLS-PLAN.md#orig-p0a-l0882)

<a id="18-projection-publication"></a>

**旧 L903–L932：** [18. Projection Publication](MS-SKILLS-PLAN.md#orig-p0a-l0903)

<a id="19-projectionview"></a>

**旧 L933–L954：** [19. ProjectionView](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0933)

<a id="20-authoritysnapshot"></a>

**旧 L955–L984：** [20. AuthoritySnapshot](MS-SKILLS-PLAN.md#orig-p0a-l0955)

<a id="21-truthauthorityservice"></a>

**旧 L985–L1001：** [21. TruthAuthorityService](MS-SKILLS-PLAN.md#orig-p0a-l0985)

<a id="22-identitybindingservice"></a>

**旧 L1002–L1024：** [22. IdentityBindingService](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1002)

<a id="23-incarnation-discovery"></a>

**旧 L1025–L1048：** [23. Incarnation Discovery](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1025)

<a id="24-pre-admission-binding"></a>

**旧 L1049–L1069：** [24. Pre-Admission Binding](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1049)

<a id="25-terminal-aware-binding"></a>

**旧 L1070–L1095：** [25. Terminal-aware Binding](MS-MSITEM-PLAN.md#orig-p0a-l1070)

<a id="26-resolverprovider-spi"></a>

**旧 L1096–L1097：** [26. ResolverProvider SPI](MS-MSITEM-PLAN.md#orig-p0a-l1096)

<a id="261-接口"></a>

**旧 L1098–L1115：** [26.1 接口](MS-MSITEM-PLAN.md#orig-p0a-l1098)

<a id="262-resolverresult"></a>

**旧 L1116–L1138：** [26.2 ResolverResult](MS-MSITEM-PLAN.md#orig-p0a-l1116)

<a id="27-authorityrelationshipedge"></a>

**旧 L1139–L1161：** [27. AuthorityRelationshipEdge](MS-SKILLS-PLAN.md#orig-p0a-l1139)

<a id="28-relationship-validation-state"></a>

**旧 L1162–L1184：** [28. Relationship Validation State](MS-SKILLS-PLAN.md#orig-p0a-l1162)

<a id="29-graph-persistence-policy"></a>

**旧 L1185–L1208：** [29. Graph Persistence Policy](MS-SKILLS-PLAN.md#orig-p0a-l1185)

<a id="30-policy-model"></a>

**旧 L1209–L1210：** [30. Policy Model](MS-MSITEM-PLAN.md#orig-p0a-l1209)

<a id="301-authoritymode"></a>

**旧 L1211–L1220：** [30.1 AuthorityMode](MS-MSITEM-PLAN.md#orig-p0a-l1211)

<a id="302-modeselector"></a>

**旧 L1221–L1230：** [30.2 ModeSelector](MS-MSITEM-PLAN.md#orig-p0a-l1221)

<a id="303-policyoverlay"></a>

**旧 L1231–L1243：** [30.3 PolicyOverlay](MS-MSITEM-PLAN.md#orig-p0a-l1231)

<a id="31-policy-resolution"></a>

**旧 L1244–L1260：** [31. Policy Resolution](MS-MSITEM-PLAN.md#orig-p0a-l1244)

<a id="32-constitutional-invariants"></a>

**旧 L1261–L1285：** [32. Constitutional Invariants](MS-SKILLS-PLAN.md#orig-p0a-l1261)

<a id="33-mode-切换"></a>

**旧 L1286–L1312：** [33. Mode 切换](MS-MSITEM-PLAN.md#orig-p0a-l1286)

<a id="34-p0-a-与-non-blocking-dominion"></a>

**旧 L1313–L1336：** [34. P0-A 与 Non-Blocking Dominion](MS-SKILLS-PLAN.md#orig-p0a-l1313)

<a id="35-p0-a-与-p0-b-的边界"></a>

**旧 L1337–L1361：** [35. P0-A 与 P0-B 的边界](MS-SKILLS-PLAN.md#orig-p0a-l1337)

<a id="36-p0-a-与-p0-c-的边界"></a>

**旧 L1362–L1384：** [36. P0-A 与 P0-C 的边界](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1362)

<a id="37-p0-a-与-p0-d-的边界"></a>

**旧 L1385–L1418：** [37. P0-A 与 P0-D 的边界](MS-SKILLS-PLAN.md#orig-p0a-l1385)

<a id="38-failure-model"></a>

**旧 L1419–L1448：** [38. Failure Model](MS-SKILLS-PLAN.md#orig-p0a-l1419)

<a id="39-fail-closed--fail-open-策略"></a>

**旧 L1449–L1450：** [39. Fail-Closed / Fail-Open 策略](MS-SKILLS-PLAN.md#orig-p0a-l1449)

<a id="root-unavailable--fenced"></a>

**旧 L1451–L1466：** [Root unavailable / fenced](MS-SKILLS-PLAN.md#orig-p0a-l1451)

<a id="unknown-schema"></a>

**旧 L1467–L1482：** [Unknown schema](MS-SKILLS-PLAN.md#orig-p0a-l1467)

<a id="40-cache-设计"></a>

**旧 L1483–L1499：** [40. Cache 设计](MS-SKILLS-PLAN.md#orig-p0a-l1483)

<a id="41-并发模型"></a>

**旧 L1500–L1529：** [41. 并发模型](MS-SKILLS-PLAN.md#orig-p0a-l1500)

<a id="42-java-内存模型要求"></a>

**旧 L1530–L1548：** [42. Java 内存模型要求](MS-SKILLS-PLAN.md#orig-p0a-l1530)

<a id="43-native-shared-projection-预留"></a>

**旧 L1549–L1568：** [43. Native Shared Projection 预留](MS-SKILLS-PLAN.md#orig-p0a-l1549)

<a id="44-migration-from-full-l6-internal"></a>

**旧 L1569–L1586：** [44. Migration from FULL L6 Internal](MS-SKILLS-PLAN.md#orig-p0a-l1569)

<a id="45-migration-from-714a-frozen-baseline--r50-current-line"></a>

**旧 L1587–L1607：** [45. Migration from 7.14A Frozen Baseline / r50 Current Line](MS-SKILLS-PLAN.md#orig-p0a-l1587)

<a id="46-telemetry"></a>

**旧 L1608–L1632：** [46. Telemetry](MS-SKILLS-PLAN.md#orig-p0a-l1608)

<a id="47-audit"></a>

**旧 L1633–L1656：** [47. Audit](MS-SKILLS-PLAN.md#orig-p0a-l1633)

<a id="48-security--integrity-notes"></a>

**旧 L1657–L1672：** [48. Security / Integrity Notes](MS-SKILLS-PLAN.md#orig-p0a-l1657)

<a id="49-unit-tests"></a>

**旧 L1673–L1688：** [49. Unit Tests](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1673)

<a id="u-01-subjectkey-stability"></a>

**旧 L1689–L1697：** [U-01 SubjectKey Stability](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1689)

<a id="u-02-incarnation-monotonicity"></a>

**旧 L1698–L1701：** [U-02 Incarnation Monotonicity](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1698)

<a id="u-03-stale-rejection"></a>

**旧 L1702–L1709：** [U-03 Stale Rejection](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1702)

<a id="u-04-tombstone-dominance"></a>

**旧 L1710–L1717：** [U-04 Tombstone Dominance](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1710)

<a id="u-05-binding-revision"></a>

**旧 L1718–L1725：** [U-05 Binding Revision](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1718)

<a id="u-06-root-command-idempotency"></a>

**旧 L1726–L1733：** [U-06 Root Command Idempotency](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1726)

<a id="u-07-command-conflict"></a>

**旧 L1734–L1741：** [U-07 Command Conflict](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1734)

<a id="u-08-projection-atomicity"></a>

**旧 L1742–L1745：** [U-08 Projection Atomicity](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1742)

<a id="u-09-unknown-optional-extension"></a>

**旧 L1746–L1749：** [U-09 Unknown Optional Extension](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1746)

<a id="u-10-unknown-mandatory-extension"></a>

**旧 L1750–L1753：** [U-10 Unknown Mandatory Extension](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1750)

<a id="u-11-lifecycle--resolution-orthogonality"></a>

**旧 L1754–L1757：** [U-11 Lifecycle / Resolution Orthogonality](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1754)

<a id="u-12-revision-scope"></a>

**旧 L1758–L1761：** [U-12 Revision Scope](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1758)

<a id="u-13-canonical-wire--defensive-copy"></a>

**旧 L1762–L1765：** [U-13 Canonical Wire / Defensive Copy](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1762)

<a id="u-14-immutable-projectionview"></a>

**旧 L1766–L1769：** [U-14 Immutable ProjectionView](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1766)

<a id="u-15-provider-lifecycle"></a>

**旧 L1770–L1773：** [U-15 Provider Lifecycle](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1770)

<a id="u-16-pre-admission-state-machine"></a>

**旧 L1774–L1777：** [U-16 Pre-Admission State Machine](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1774)

<a id="u-17-production-process-boundary"></a>

**旧 L1778–L1793：** [U-17 Production Process Boundary](MS-SKILLS-PLAN.md#orig-p0a-l1778)

<a id="50-integration-tests"></a>

**旧 L1794–L1795：** [50. Integration Tests](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1794)

<a id="i-01-entity-rebuild"></a>

**旧 L1796–L1811：** [I-01 Entity Rebuild](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1796)

<a id="i-02-terminal-rebuild-attempt"></a>

**旧 L1812–L1826：** [I-02 Terminal Rebuild Attempt](MS-MSITEM-PLAN.md#orig-p0a-l1812)

<a id="i-03-projection-lag"></a>

**旧 L1827–L1838：** [I-03 Projection Lag](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1827)

<a id="i-04-root-receipt-ambiguity"></a>

**旧 L1839–L1850：** [I-04 Root Receipt Ambiguity](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1839)

<a id="i-05-mode-change"></a>

**旧 L1851–L1864：** [I-05 Mode Change](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1851)

<a id="i-06-relationship-opaque"></a>

**旧 L1865–L1878：** [I-06 Relationship Opaque](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l1865)

<a id="i-07-root-schema--recovery"></a>

**旧 L1879–L1882：** [I-07 Root Schema / Recovery](MS-SKILLS-PLAN.md#orig-p0a-l1879)

<a id="i-08-p0-d-minimum-crosscut"></a>

**旧 L1883–L1888：** [I-08 P0-D Minimum Crosscut](MS-SKILLS-PLAN.md#orig-p0a-l1883)

<a id="51-chaos-tests"></a>

**旧 L1889–L1890：** [51. Chaos Tests](MS-SKILLS-PLAN.md#orig-p0a-l1889)

<a id="c-01-reordered-projection-arrival"></a>

**旧 L1891–L1907：** [C-01 Reordered Projection Arrival](MS-SKILLS-PLAN.md#orig-p0a-l1891)

<a id="c-02-duplicate-root-response"></a>

**旧 L1908–L1917：** [C-02 Duplicate Root Response](MS-SKILLS-PLAN.md#orig-p0a-l1908)

<a id="c-03-jvm-restart"></a>

**旧 L1918–L1928：** [C-03 JVM Restart](MS-SKILLS-PLAN.md#orig-p0a-l1918)

<a id="c-04-partial-relationship-cache-loss"></a>

**旧 L1929–L1940：** [C-04 Partial Relationship Cache Loss](MS-SKILLS-PLAN.md#orig-p0a-l1929)

<a id="c-05-internal-root--store-temporary-unavailable"></a>

**旧 L1941–L1950：** [C-05 Internal Root / Store Temporary Unavailable](MS-SKILLS-PLAN.md#orig-p0a-l1941)

<a id="52-performance-gates"></a>

**旧 L1951–L1987：** [52. Performance Gates](MS-SKILLS-PLAN.md#orig-p0a-l1951)

<a id="53-persistence-rules"></a>

**旧 L1988–L2017：** [53. Persistence Rules](MS-SKILLS-PLAN.md#orig-p0a-l1988)

<a id="54-data-retention"></a>

**旧 L2018–L2037：** [54. Data Retention](MS-SKILLS-PLAN.md#orig-p0a-l2018)

<a id="55-logging-rules"></a>

**旧 L2038–L2065：** [55. Logging Rules](MS-SKILLS-PLAN.md#orig-p0a-l2038)

<a id="56-feature-flags"></a>

**旧 L2066–L2084：** [56. Feature Flags](MS-MSITEM-PLAN.md#orig-p0a-l2066)

<a id="57-backward-compatibility"></a>

**旧 L2085–L2111：** [57. Backward Compatibility](MS-SKILLS-PLAN.md#orig-p0a-l2085)

<a id="58-p0-a-v13-deliverables"></a>

**旧 L2112–L2138：** [58. P0-A V1.3 Deliverables](MS-SKILLS-PLAN.md#orig-p0a-l2112)

<a id="59-p0-a-v13-明确不交付"></a>

**旧 L2139–L2157：** [59. P0-A V1.3 明确不交付](MS-SKILLS-PLAN.md#orig-p0a-l2139)

<a id="60-p0-a-exit-criteria"></a>

**旧 L2158–L2161：** [60. P0-A Exit Criteria](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2158)

<a id="identity"></a>

**旧 L2162–L2169：** [Identity](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2162)

<a id="binding"></a>

**旧 L2170–L2176：** [Binding](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2170)

<a id="lifecycle"></a>

**旧 L2177–L2182：** [Lifecycle](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2177)

<a id="root"></a>

**旧 L2183–L2191：** [Root](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2183)

<a id="projection"></a>

**旧 L2192–L2198：** [Projection](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2192)

<a id="policy"></a>

**旧 L2199–L2204：** [Policy](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2199)

<a id="relationship"></a>

**旧 L2205–L2210：** [Relationship](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2205)

<a id="admission--result-semantics"></a>

**旧 L2211–L2217：** [Admission / Result Semantics](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2211)

<a id="p0-d-minimum-crosscut"></a>

**旧 L2218–L2223：** [P0-D Minimum Crosscut](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2218)

<a id="performance"></a>

**旧 L2224–L2229：** [Performance](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l2224)

<a id="process-boundary"></a>

**旧 L2230–L2239：** [Process Boundary](MS-SKILLS-PLAN.md#orig-p0a-l2230)

<a id="61-下一阶段接口交接"></a>

**旧 L2240–L2271：** [61. 下一阶段接口交接](MS-SKILLS-PLAN.md#orig-p0a-l2240)

<a id="62-final-implementation-rule"></a>

**旧 L2272–L2298：** [62. Final Implementation Rule](MS-SKILLS-PLAN.md#orig-p0a-l2272)
