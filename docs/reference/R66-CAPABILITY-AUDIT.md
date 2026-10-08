# Ronova r66：能力接线只读核对摘录

核对日期：2026-09-14。源码基线：用户上传 `ronova-source-0.2.0-alpha.7.17-r66.zip`；文档基线：fix3。

**范围：静态源码和文档检查，未编译、未运行游戏、未测用户 JVM 的实际 capabilities，不是运行验收或 PASS。没有修改源压缩包、规划或 validation。**

源码包 SHA-256：`1266d8c2dd46b949178d7341ee467333be678fb9b373c1403b576a56c73fd763`。

## 计数与限定结论

- StrengthOptionKey 枚举：54 个。
- RuntimeCapabilityProjection 明确发布的键：6 个；另外 48 个没有从此全局投影获得支持值。这不是所有执行器的可达性统计。
- 发布值只见 PROTECT_RONOVA/OFF；ACTIVE 还以 proof 为前提，不能说实测六个都 ACTIVE。
- AttackAction 枚举：14 个（包含 STRONG_ATTACK 聚合控制）。
- native initialize 中的能力申请字段：4 个。这里是条件复制 potential 值，**不是四项均已获授**。
- TerminalActionPolicy 另有 scoped.withCapability 调用，映射 REWRITE_LIFE / DIRECT_CLEAR / CLEAR_ENTITY_RECORDS 三个动作的实体局部步骤。
- RecoveryChainPolicy 还存在 registeredAtomicSnapshot / registeredNbtOutlet / registeredTaskDisposition / registeredStorageCommit 的局部通道。不能仅由全局键 OFF 推导这些通道不可达。
- TerminalLifecycle.enforce 服务器路径在取得有效终结 qualification/revisions 后委派 TerminalPlanExecutor；后者通过 opaque port 回调 runPlannedIntegration。旧 integrateDeathOnce 中服务器支路与当前整个服务器终结链不是同一回事。

全局投影的六个键：`INTERCEPT_INSTRUMENTATION, PROTECT_CODE_INTEGRITY, RUNTIME_BACKGROUND_INTEGRITY, RUNTIME_CRITICAL_CLASS_WATCH, RUNTIME_JVMTI, RUNTIME_NATIVE`。

申请字段：`can_retransform_classes, can_retransform_any_class, can_generate_all_class_hook_events, can_access_local_variables`。

下列段落保持源文件行号；结论为静态阅读判断，不代表已触达或功能成功。

<a id="policy"></a>
## 全局策略和缺省值

`src/main/java/dev/ronova/coremod/policy/StrengthPolicySnapshot.java`

### L93–L114

```text
93:         this.configuredMatrixRevision = configuredMatrixRevision;
94:         this.activeMatrixRevision = activeMatrixRevision;
95:         this.configuredProfile = Objects.requireNonNull(configuredProfile, "configuredProfile");
96:         this.activeProfile = Objects.requireNonNull(activeProfile, "activeProfile");
97:         this.configuredLevels = levels(configuredLevels, MechanismLevel.FOLLOW_PROFILE);
98:         this.activeLevels = levels(activeLevels, MechanismLevel.FOLLOW_PROFILE);
99:         this.supportedCeilings = levels(supportedCeilings, MechanismLevel.OFF);
100:         this.evidenceStates = evidence(evidenceStates);
101:         this.evidenceCeilings = evidenceCeilings == null
102:             ? evidenceCeilings(this.supportedCeilings, this.evidenceStates)
103:             : levels(evidenceCeilings, MechanismLevel.OFF);
104:         this.runtimeStates = activation(runtimeStates);
105:         this.configuredActions = Objects.requireNonNull(configuredActions, "configuredActions");
106:         this.activeActions = Objects.requireNonNull(activeActions, "activeActions");
107:     }
108: 
109:     public static StrengthPolicySnapshot defaults() {
110:         return new StrengthPolicySnapshot(
111:             PolicyRevision.INITIAL,
112:             StrengthProfile.COMPATIBLE,
113:             StrengthProfile.COMPATIBLE,
114:             Map.of(), Map.of(), Map.of(), Map.of(), Map.of()
```

### L125–L138

```text
125:     /** Separates the saved request from the policy image loaded by this JVM. */
126:     public static StrengthPolicySnapshot fromConfigurations(
127:         StrengthPolicyConfiguration configured,
128:         StrengthPolicyConfiguration active
129:     ) {
130:         Objects.requireNonNull(configured, "configured");
131:         Objects.requireNonNull(active, "active");
132:         return new StrengthPolicySnapshot(
133:             configured.revision(),
134:             configured.profileMatrixRevision(), active.profileMatrixRevision(),
135:             configured.profile(), active.profile(),
136:             configured.levels(), active.levels(),
137:             Map.of(), Map.of(), Map.of(), Map.of(), configured.attacks(), active.attacks()
138:         );
```

### L179–L214

```text
179:     public AttackResolution resolveAttack(
180:         java.util.Set<AttackAction> actions,
181:         java.util.Set<StrengthOptionKey> additionalKeys
182:     ) {
183:         Objects.requireNonNull(actions, "actions");
184:         Objects.requireNonNull(additionalKeys, "additionalKeys");
185:         if (actions.isEmpty() || actions.equals(java.util.Set.of(AttackAction.STRONG_ATTACK))) {
186:             return new AttackResolution(MechanismLevel.OFF, MechanismLevel.OFF, false,
187:                 "NO_EXECUTABLE_ACTION");
188:         }
189:         java.util.EnumSet<StrengthOptionKey> required =
190:             java.util.EnumSet.noneOf(StrengthOptionKey.class);
191:         MechanismLevel requested = MechanismLevel.STRICT_ALL;
192:         for (AttackAction action : actions) {
193:             MechanismLevel depth = activeActions.resolve(activeProfile, action).depth();
194:             if (depth == MechanismLevel.OFF) {
195:                 return new AttackResolution(MechanismLevel.OFF, MechanismLevel.OFF, false,
196:                     "ACTION_DISABLED:" + action.canonical());
197:             }
198:             requested = MechanismLevel.lowerOf(requested, depth);
199:             required.addAll(action.requiredKeys());
200:         }
201:         required.addAll(additionalKeys);
202:         MechanismLevel effective = requested;
203:         for (StrengthOptionKey key : required) {
204:             StrengthResolutionReceipt receipt = resolve(key);
205:             if (receipt.activationState() != ActivationState.ACTIVE) {
206:                 return new AttackResolution(requested, MechanismLevel.OFF, false,
207:                     receipt.activationState() + ":" + key.canonical());
208:             }
209:             effective = MechanismLevel.lowerOf(effective, receipt.effectiveLevel());
210:         }
211:         boolean permitted = effective.rank() >= MechanismLevel.PROTECT_RONOVA.rank();
212:         return new AttackResolution(requested, effective, permitted,
213:             permitted ? effective == requested ? "PERMITTED" : "LIMITED_BY_DEPENDENCY"
214:                 : "DEPENDENCY_MONITOR_ONLY");
```

### L217–L246

```text
217:     public StrengthResolutionReceipt resolve(StrengthOptionKey option) {
218:         Objects.requireNonNull(option, "option");
219:         MechanismLevel configured = configuredLevels.get(option);
220:         ProfilePresetMatrix.Resolution activeResolution = ProfilePresetMatrix.resolve(
221:             activeMatrixRevision, activeProfile, option, activeLevels.get(option)
222:         );
223:         MechanismLevel activeRequested = activeResolution.requestedLevel();
224:         MechanismLevel supportedCeiling = MechanismLevel.runnable(
225:             supportedCeilings.get(option)
226:         );
227:         MechanismLevel evidenceCeiling = MechanismLevel.runnable(
228:             evidenceCeilings.get(option)
229:         );
230:         MechanismLevel effective = MechanismLevel.lowerOf(
231:             MechanismLevel.lowerOf(activeRequested, supportedCeiling),
232:             evidenceCeiling
233:         );
234:         EvidenceState evidence = evidenceStates.get(option);
235:         ActivationState activation = activationState(
236:             option, activeRequested, supportedCeiling,
237:             evidenceCeiling, evidence
238:         );
239:         return new StrengthResolutionReceipt(
240:             revision, configuredMatrixRevision, activeMatrixRevision,
241:             option, configured, activeResolution.presetLevel(), activeRequested,
242:             supportedCeiling, evidenceCeiling, effective, evidence, activation,
243:             activeResolution.minimumFloorApplied(),
244:             reason(
245:                 activeRequested, effective, supportedCeiling, evidenceCeiling,
246:                 evidence, activation, activeResolution.minimumFloorApplied()
```

### L358–L392

```text
358: 
359:     private ActivationState activationState(
360:         StrengthOptionKey option,
361:         MechanismLevel activeRequested,
362:         MechanismLevel supportedCeiling,
363:         MechanismLevel evidenceCeiling,
364:         EvidenceState evidence
365:     ) {
366:         // One saved profile becomes one coherent runtime image. Hot activation
367:         // of only part of a profile would split attack/protection/runtime truth.
368:         // Compare configuration syntax as well as resolved strength: changing
369:         // FOLLOW_PROFILE to an equal explicit level is still a new durable image.
370:         if (configuredMatrixRevision != activeMatrixRevision
371:             || configuredProfile != activeProfile
372:             || !configuredLevels.equals(activeLevels)
373:             || !configuredActions.equals(activeActions)) {
374:             return ActivationState.PENDING_RESTART;
375:         }
376:         ActivationState explicit = runtimeStates.get(option);
377:         if (explicit == ActivationState.FENCED || explicit == ActivationState.REJECTED) {
378:             return explicit;
379:         }
380:         if (activeRequested == MechanismLevel.OFF) {
381:             return ActivationState.INACTIVE;
382:         }
383:         if (supportedCeiling == MechanismLevel.OFF
384:             || evidenceCeiling == MechanismLevel.OFF
385:             || evidence == EvidenceState.UNOBSERVED) {
386:             return ActivationState.UNAVAILABLE;
387:         }
388:         if (evidence == EvidenceState.FAILED) {
389:             return ActivationState.FENCED;
390:         }
391:         return explicit == ActivationState.UNAVAILABLE
392:             ? ActivationState.UNAVAILABLE : ActivationState.ACTIVE;
```

<a id="projection"></a>
## 全局投影及生产入口

`src/main/java/dev/ronova/coremod/policy/RuntimeCapabilityProjection.java`

### L1–L26

```text
1: package dev.ronova.coremod.policy;
2: 
3: import dev.ronova.coremod.integrity.*;
4: import java.util.Set;
5: 
6: /** Connects measured support to policy presentation without granting new attack permissions. */
7: public final class RuntimeCapabilityProjection {
8:     private RuntimeCapabilityProjection() {}
9:     public static StrengthPolicySnapshot apply(StrengthPolicySnapshot policy,EnforcementProof enforcement,CodeIntegrityProof code) {
10:         var result=policy;
11:         for(var key:Set.of(StrengthOptionKey.PROTECT_CODE_INTEGRITY,StrengthOptionKey.RUNTIME_CRITICAL_CLASS_WATCH,
12:             StrengthOptionKey.INTERCEPT_INSTRUMENTATION)) {
13:             result=result.withCapability(key,code.supported()?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,
14:                 code.current()?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,
15:                 code.current()?EvidenceState.VERIFIED:code.supported()?EvidenceState.FAILED:EvidenceState.UNOBSERVED,
16:                 code.current()?ActivationState.ACTIVE:code.supported()?ActivationState.FENCED:ActivationState.UNAVAILABLE);
17:         }
18:         for(var key:Set.of(StrengthOptionKey.RUNTIME_JVMTI,StrengthOptionKey.RUNTIME_NATIVE,StrengthOptionKey.RUNTIME_BACKGROUND_INTEGRITY)) {
19:             boolean verified=enforcement.current();
20:             result=result.withCapability(key,enforcement.supported()?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,
21:                 verified?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,verified?EvidenceState.VERIFIED:EvidenceState.UNOBSERVED,
22:                 verified?ActivationState.ACTIVE:ActivationState.UNAVAILABLE);
23:         }
24:         return result;
25:     }
26: }
```

<a id="runtime"></a>
## 运行时策略读取

`src/main/java/dev/ronova/coremod/runtime/DominionServerRuntime.java`

### L202–L210

```text
202:     /**
203:      * Coherent configured/active strength view. A configured change remains
204:      * PENDING_RESTART until the durable Root is loaded by the next JVM.
205:      */
206:     public StrengthPolicySnapshot strengthPolicy() {
207:         return durableStore == null
208:             ? StrengthPolicySnapshot.defaults()
209:             : dev.ronova.coremod.policy.RuntimeCapabilityProjection.apply(durableStore.strengthPolicySnapshot(),enforcement.published(),enforcement.codeIntegrity());
210:     }
```

<a id="actions"></a>
## 14 个动作的定义

`src/main/java/dev/ronova/coremod/policy/AttackAction.java`

### L5–L29

```text
5: /** Ordinary attack controls; these do not add advanced StrengthOptionKeys. */
6: public enum AttackAction {
7:     STRONG_ATTACK("strongAttack", "强力攻击"),
8:     REWRITE_LIFE("rewriteLife", "改写生命", StrengthOptionKey.ATTACK_STATE_SOURCE),
9:     TERMINALIZE("terminalize", "强制终结", StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER,
10:         StrengthOptionKey.ATTACK_REGISTRATION_SOURCE),
11:     DIRECT_CLEAR("directClear", "直接清除", StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER),
12:     CLEAR_ENTITY_RECORDS("clearEntityRecords", "实体记录清除",
13:         StrengthOptionKey.ATTACK_STATE_SOURCE, StrengthOptionKey.ATTACK_REGISTRATION_SOURCE),
14:     SUPPRESS_RESPAWN("suppressRespawn", "重生压制", StrengthOptionKey.ATTACK_REGISTRATION_SOURCE),
15:     SUPPRESS_REBUILD("suppressRebuild", "重建压制", StrengthOptionKey.ATTACK_RECOVERY_SOURCE,
16:         StrengthOptionKey.ATTACK_TARGET_CLASS_LINEAGE),
17:     SUPPRESS_ADMISSION("suppressAdmission", "生成压制", StrengthOptionKey.ATTACK_REGISTRATION_SOURCE,
18:         StrengthOptionKey.ATTACK_NETWORK_RECONSTRUCTION, StrengthOptionKey.ATTACK_SAVE_RECONSTRUCTION),
19:     ATTACK_RENDER("attackRender", "渲染攻击", StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER),
20:     CLEAR_TRACKING("clearTracking", "追踪清除", StrengthOptionKey.ATTACK_NETWORK_RECONSTRUCTION,
21:         StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER),
22:     CLEAR_HUD("clearHud", "血条/HUD 清除", StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER),
23:     STRIP_CODE_DEFENSE("stripCodeDefense", "代码防御剥离",
24:         StrengthOptionKey.ATTACK_RUNTIME_PATCH_DISABLEMENT, StrengthOptionKey.ATTACK_DYNAMIC_CODE,
25:         StrengthOptionKey.ATTACK_TARGET_CLASS_LINEAGE),
26:     ATTACK_CALL_CHAIN("attackCallChain", "调用链攻击", StrengthOptionKey.ATTACK_LOGIC_SOURCE,
27:         StrengthOptionKey.ATTACK_RECOVERY_SOURCE, StrengthOptionKey.ATTACK_TASK_CHAIN,
28:         StrengthOptionKey.ATTACK_CALLBACK_CHAIN, StrengthOptionKey.ATTACK_DISPATCH),
29:     ALL_RETURN("allReturn", "全返回", StrengthOptionKey.ATTACK_DISPATCH);
```

<a id="terminal-policy"></a>
## 实体局部动作策略

`src/main/java/dev/ronova/coremod/policy/TerminalActionPolicy.java`

### L1–L50

```text
1: package dev.ronova.coremod.policy;
2: 
3: import java.util.*;
4: 
5: /** Fixed permissions for the currently implemented entity-local terminal executors. */
6: public final class TerminalActionPolicy {
7:     public enum Step {
8:         LIFE(0x8063,Set.of(AttackAction.REWRITE_LIFE)),
9:         REMOVAL(0x3,Set.of(AttackAction.DIRECT_CLEAR)),
10:         REMOVED_RECORDS(0x1f89,Set.of(AttackAction.CLEAR_ENTITY_RECORDS)),
11:         BYPASS_RECORDS(0x1f89,Set.of(AttackAction.CLEAR_ENTITY_RECORDS,AttackAction.DIRECT_CLEAR));
12:         private final int mask;
13:         private final Set<AttackAction> actions;
14:         Step(int mask,Set<AttackAction> actions){this.mask=mask;this.actions=Set.copyOf(actions);}
15:         public Set<AttackAction> actions(){return actions;}
16:         public int mask(){return mask;}
17:     }
18:     private static final Set<StrengthOptionKey> KEYS=Set.of(StrengthOptionKey.ATTACK_STATE_SOURCE,
19:         StrengthOptionKey.RUNTIME_NATIVE,StrengthOptionKey.RUNTIME_JVMTI,
20:         StrengthOptionKey.RUNTIME_CRITICAL_CLASS_WATCH,StrengthOptionKey.PROTECT_CODE_INTEGRITY);
21:     private TerminalActionPolicy(){}
22:     public static MechanismLevel requestedDepth(StrengthPolicySnapshot policy,Step step) {
23:         var keys=EnumSet.copyOf(KEYS);var depth=MechanismLevel.STRICT_ALL;
24:         for(var action:step.actions()) {
25:             depth=MechanismLevel.lowerOf(depth,policy.activeActions().resolve(policy.activeProfile(),action).depth());
26:             keys.addAll(action.requiredKeys());
27:         }
28:         for(var key:keys)depth=MechanismLevel.lowerOf(depth,ProfilePresetMatrix.resolve(policy.activeMatrixRevision(),
29:             policy.activeProfile(),key,policy.activeLevels().get(key)).requestedLevel());
30:         return depth;
31:     }
32:     public static AttackResolution resolve(StrengthPolicySnapshot policy,Step step,boolean boundaryVerified) {
33:         Objects.requireNonNull(policy);Objects.requireNonNull(step);
34:         if(policy.pendingRestart())return new AttackResolution(MechanismLevel.OFF,MechanismLevel.OFF,false,"PENDING_RESTART");
35:         // Only this registered executor receives the measured boundary capability.
36:         // Do not advertise these broad option keys as supported for other surfaces.
37:         var keys=EnumSet.copyOf(KEYS);
38:         step.actions().forEach(action->keys.addAll(action.requiredKeys()));
39:         var scoped=policy;
40:         for(var key:keys)scoped=scoped.withCapability(key,MechanismLevel.PROTECT_RONOVA,
41:             boundaryVerified?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,
42:             boundaryVerified?EvidenceState.VERIFIED:EvidenceState.UNOBSERVED,
43:             boundaryVerified?ActivationState.ACTIVE:ActivationState.UNAVAILABLE);
44:         var result=scoped.resolveAttack(step.actions(),KEYS);
45:         if(!result.permitted())return result;
46:         // These executors act on one registered entity and its exact index references.
47:         // Selecting Extreme does not claim producer, native or arbitrary resource coverage.
48:         return new AttackResolution(result.requestedDepth(),MechanismLevel.PROTECT_RONOVA,true,"REGISTERED_ENTITY_ONLY");
49:     }
50: }
```

<a id="lifecycle"></a>
## 服务器委派与旧生命周期支路

`src/main/java/dev/ronova/coremod/engine/TerminalLifecycle.java`

### L44–L119

```text
44:     public static void enforce(
45:         Entity entity,
46:         boolean processDeathOnce,
47:         AuthorityView authorityView
48:     ) {
49:         if (entity != null && entity.level() instanceof ServerLevel level) {
50:             var runtime = ServerRuntimeRegistry.getIfPresent(level.getServer());
51:             if (runtime != null && runtime.durableStore() != null) {
52:                 var current = runtime.durableStore().subjectQualification(new dev.ronova.coremod.contract.SubjectKey(runtime.durableStore().realmId(), entity.getUUID()));
53:                 // A changed/unjudged UUID must not fall through into the legacy physical writer.
54:                 if (current == null || !current.terminal()) return;
55:                 if (current.revisions() != null) {
56:                     runtime.terminalPlans().execute(entity, authorityView); return;
57:                 }
58:             }
59:             // Server mutations always need a current Root plan. Missing runtime or
60:             // legacy unbound qualifications cannot re-enable the old unrestricted writer.
61:             return;
62:         }
63:         enforceLegacyBody(entity, processDeathOnce, authorityView, false, () -> true);
64:     }
65: 
66:     static void runPlannedIntegration(Entity entity, AuthorityView view, java.util.function.BooleanSupplier current) {
67:         enforceLegacyBody(entity, true, view, true, current);
68:     }
69: 
70:     private static void enforceLegacyBody(Entity entity, boolean processDeathOnce, AuthorityView authorityView, boolean planned,
71:         java.util.function.BooleanSupplier current) {
72:         if (entity == null || authorityView == null || authorityView.isProtected(entity)) {
73:             return;
74:         }
75:         if (!current.getAsBoolean()) return;
76:         BaselineTelemetry.recordTerminalInvocation(entity);
77:         boolean firstExecution = JudgmentState.mark(entity);
78:         if (firstExecution && !planned) {
79:             prepare(entity, current);
80:         }
81:         if (entity instanceof LivingEntity living) {
82:             if (!current.getAsBoolean()) return;
83:             if (firstExecution && !planned) {
84:                 clearEffects(living);
85:             }
86:             if (processDeathOnce && firstExecution) {
87:                 if (!current.getAsBoolean()) return;
88:                 if (planned) {
89:                     try { invokePublicDeath(living, living.damageSources().fellOutOfWorld(), current, false); }
90:                     catch (RuntimeException failed) { /* The encompassing Root effect remains unknown. */ }
91:                 } else integrateDeathOnce(living, entity);
92:             }
93:             if (!current.getAsBoolean()) return;
94:             if (!planned) writeLivingTerminal(living, current);
95:         }
96:         if (firstExecution) {
97:             // The planned path has one death entry and one removal entry. kill() can
98:             // dispatch hurt/die again; discard() can dispatch remove again.
99:             if (!planned) {
100:                 if (!current.getAsBoolean()) return;
101:                 invokeKill(entity);
102:                 if (!current.getAsBoolean()) return;
103:                 invokeDiscard(entity);
104:             }
105:             if (!current.getAsBoolean()) return;
106:             invokeRemove(entity);
107:         }
108:         if (!current.getAsBoolean()) return;
109:         if (!planned) writeEntityTerminal(entity, current);
110:     }
111: 
112:     /**
113:      * Completes a Root-committed terminal transition.  The durable lifecycle
114:      * claim is written before public death integration. Recovery never replays
115:      * that opaque death entry for an existing or uncertain claim. This does not
116:      * prove its effect completed, nor cover the other legacy physical lanes.
117:      */
118:     public static void completeCommitted(Entity entity, AuthorityView authorityView) {
119:         enforce(entity, true, authorityView);
```

### L150–L169

```text
150:     private static void integrateDeathOnce(LivingEntity living, Entity entity) {
151:         DamageSource source;
152:         try {
153:             source = living.damageSources().fellOutOfWorld();
154:         } catch (Throwable ignored) {
155:             return;
156:         }
157:         if (entity.level() instanceof ServerLevel level) {
158:             try {
159:                 DominionServerRuntime runtime = ServerRuntimeRegistry.runtime(level.getServer());
160:                 runtime.integrateLifecycleOnce(entity.getUUID(), physicalIdentity(entity),
161:                     () -> invokePublicDeath(living, source, () -> true));
162:             } catch (Throwable ignored) {
163:                 // No acknowledged intent/entry means no callback; raw terminal maintenance continues.
164:             }
165:         } else {
166:             invokePublicDeath(living, source, () -> true);
167:         }
168:     }
169: 
```

<a id="executor"></a>
## 服务器执行器端口

`src/main/java/dev/ronova/coremod/engine/TerminalPlanExecutor.java`

### L36–L56

```text
36:     public boolean execute(Entity entity, AuthorityView view) {
37:         if (entity == null || view == null || runtime.durableStore() == null) return false;
38:         var identity=readIdentity(entity);
39:         if(identity==null){countFenced();return false;}
40:         var level=identity.level();
41:         var store = runtime.durableStore();
42:         TerminalOperationPlan plan = store.approveTerminalPlan(identity.subject(), identity.nonce());
43:         if (plan == null) { countFenced(); return false; }
44:         int initialEntityId=identity.entityId();
45:         runtime.spawnAdmission().rememberClassLineage(entity,plan);
46:         Participation participation=participation(plan,level.getServer());
47:         var resource = runtime.resources().reserve(Map.of(HostResourceDomain.JAVA_HEAP, plan.reservedHeapBytes()), HostResourceGovernor.Priority.RECOVERY);
48:         if (!resource.granted()) { countFenced(); return false; }
49:         try (var ignored = resource.reservation();
50:              var observations = new dev.ronova.coremod.observation.RecoveryObservationScope(runtime.recoverySources(), level.getServer())) {
51:             OperationReport report = runner.run(plan, new OperationPlanRunner.Ports() {
52:                 @Override public boolean current(TerminalOperationPlan value) {
53:                     var current=readIdentity(entity);
54:                     return current!=null&&current.level()==level&&current.subject().equals(value.subjectKey().subjectId())
55:                         &&current.nonce().equals(value.physicalNonce())&&store.validateTerminalPlan(value)
56:                         && runtime.projection().snapshot().epoch().compareTo(store.epoch()) >= 0;
```

### L68–L130

```text
68:                             return new OperationReport.StepReceipt(step, runtime.spawnAdmission().plannedFencePresent(entity)
69:                                 ? OperationReport.Status.CONVERGED : OperationReport.Status.UNAVAILABLE, "server-owned-admission-fence");
70:                         }
71:                         case SERVER_PRESENCE -> {
72:                             QuarantineView.register(level.getServer(), entity);
73:                             return new OperationReport.StepReceipt(step, QuarantineView.registered(level.getServer(), entity)
74:                                 ? OperationReport.Status.CONVERGED : OperationReport.Status.UNAVAILABLE, "server-owned-presence-registry");
75:                         }
76:                         case SOURCE_QUALIFICATION -> {
77:                             var policy = dev.ronova.coremod.policy.RecoveryChainPolicy.resolve(runtime.strengthPolicy(),
78:                                 dev.ronova.coremod.policy.RecoveryChainPolicy.Step.DIRECT_SOURCE);
79:                             if (!policy.observationRequested()) return new OperationReport.StepReceipt(step,
80:                                 OperationReport.Status.CONVERGED, "source-discovery-outside-active-depth;base-admission-retained");
81:                             runtime.recoverySources().watch(level, value);
82:                             runtime.recoverySources().pulse(runtime.budget(BudgetCategory.OBSERVATION));
83:                             return new OperationReport.StepReceipt(step, OperationReport.Status.DISPATCHED,
84:                                 "source-observation-and-qualified-outlet;record-task-save-disposition-separate");
85:                         }
86:                         case TERMINAL_STATE -> { return stateActions(entity,value,permitted); }
87:                         case WORLD_RECORDS -> { return observedStep(value,step,TerminalActionPolicy.Step.REMOVED_RECORDS.mask(),permitted,
88:                             gate -> {
89:                                 boolean removed=gate.getAsBoolean()&&TerminalStateSink.removalObserved(entity);
90:                                 var action=removed?TerminalActionPolicy.Step.REMOVED_RECORDS:TerminalActionPolicy.Step.BYPASS_RECORDS;
91:                                 var decision=TerminalActionPolicy.resolve(runtime.strengthPolicy(),action,gate.getAsBoolean());
92:                                 if(!decision.permitted())return new OperationReport.StepReceipt(step,OperationReport.Status.UNAVAILABLE,decision.reason());
93:                                 if(!indices.prepare(entity,value,initialEntityId))return new OperationReport.StepReceipt(step,OperationReport.Status.UNAVAILABLE,"world-index-budget-unavailable");
94:                                 return indices.repair(entity,value,()->gate.getAsBoolean()
95:                                     &&(!removed||TerminalStateSink.removalObserved(entity)));
96:                             }); }
97:                         case NETWORK_PROJECTION -> {
98:                             JudgmentMark mark = runtime.projection().admission().mark(value.subjectKey().subjectId());
99:                             if (mark == null) return new OperationReport.StepReceipt(step, OperationReport.Status.UNAVAILABLE, "missing-current-root-mark");
100:                             synchronized (TerminalPlanExecutor.this) {
101:                                 if (!dispatched.contains(value.causalityKey())) {
102:                                     dev.ronova.coremod.network.QuarantineSyncService.broadcastMarks(level.getServer(), List.of(mark));
103:                                     if (dispatched.size() >= 128) dispatched.remove(dispatched.iterator().next());
104:                                     dispatched.add(value.causalityKey());
105:                                 }
106:                             }
107:                             if(participation==null)return new OperationReport.StepReceipt(step,OperationReport.Status.DISPATCHED,"client-view-contract-budget-unavailable");
108:                             var tracker=participation.tracker();var status=tracker.sample(member->dev.ronova.coremod.network.QuarantineSyncService
109:                                 .confirmsParticipation(level.getServer(),tracker.contract(),member),Integer.toUnsignedLong(level.getServer().getTickCount()));
110:                             return new OperationReport.StepReceipt(step,status.requiredViewsStable()?OperationReport.Status.CONVERGED:OperationReport.Status.DISPATCHED,
111:                                 "client-view-contract="+tracker.contract().id()+";required-missing="+status.missingRequired().size()
112:                                     +";optional-missing="+status.missingOptional().size()+";deadline="+status.deadlineExceeded()+";pixels-unproven");
113:                         }
114:                         default -> throw new IllegalArgumentException("opaque step cannot use controlled port");
115:                     }
116:                 }
117:                 @Override public EffectClaim prepare(TerminalOperationPlan value) {
118:                     return store.prepareTerminalEffect(value, identity.subject() + "#" + initialEntityId + "@" + identity.nonce());
119:                 }
120:                 @Override public boolean enter(EffectClaim claim) { return store.enterEffect(claim); }
121:                 @Override public void opaque(TerminalOperationPlan value) { TerminalLifecycle.runPlannedIntegration(entity, view, () -> current(value)); }
122:                 @Override public void opaque(TerminalOperationPlan value, java.util.function.BooleanSupplier permitted) {
123:                     TerminalLifecycle.runPlannedIntegration(entity, view, permitted);
124:                 }
125:                 @Override public void finishUnknown(EffectClaim claim) { store.finishEffectUnknown(claim); }
126:             });
127:             if (!report.state().equals("REENTRANT")) {
128:                 boolean approved = store.acceptOperationReport(report);
129:                 remember(report, approved);
130:                 if (approved) completion.track(entity, plan);
```

### L151–L190

```text
151:     private OperationReport.StepReceipt stateActions(Entity entity,TerminalOperationPlan plan,
152:         java.util.function.BooleanSupplier permitted) {
153:         // Separate observations and write scopes keep unavailable health storage from vetoing removal.
154:         var life=stateAction(entity,plan,TerminalActionPolicy.Step.LIFE,permitted);
155:         var removal=stateAction(entity,plan,TerminalActionPolicy.Step.REMOVAL,permitted);
156:         var status=life.status()==OperationReport.Status.FENCED||removal.status()==OperationReport.Status.FENCED
157:             ?OperationReport.Status.FENCED
158:             :life.status()==OperationReport.Status.UNAVAILABLE||removal.status()==OperationReport.Status.UNAVAILABLE
159:                 ?OperationReport.Status.UNAVAILABLE
160:                 :life.status()==OperationReport.Status.CONVERGED&&removal.status()==OperationReport.Status.CONVERGED
161:                     ?OperationReport.Status.CONVERGED:OperationReport.Status.DISPATCHED;
162:         if(status==OperationReport.Status.CONVERGED||status==OperationReport.Status.DISPATCHED) {
163:             // Separate successful writes do not prove their combined postcondition is still present.
164:             status=OperationReport.Status.DISPATCHED;
165:             try(var observation=runtime.enforcement().beginObservation(0x8063)) {
166:                 if(observation!=null&&permitted.getAsBoolean()&&TerminalStateSink.matches(entity)
167:                     &&permitted.getAsBoolean()&&observation.finish())status=OperationReport.Status.CONVERGED;
168:             } catch(RuntimeException|LinkageError unavailable) {status=OperationReport.Status.DISPATCHED;}
169:         }
170:         String evidence="life="+life.status()+":"+life.evidence()+";removal="+removal.status()+":"+removal.evidence();
171:         return new OperationReport.StepReceipt(TerminalOperationPlan.Step.TERMINAL_STATE,status,
172:             evidence.substring(0,Math.min(256,evidence.length())));
173:     }
174:     private OperationReport.StepReceipt stateAction(Entity entity,TerminalOperationPlan plan,TerminalActionPolicy.Step action,
175:         java.util.function.BooleanSupplier permitted) {
176:         var result=writeStateAction(entity,plan,action,permitted);
177:         if(result.status()==OperationReport.Status.DISPATCHED||result.status()==OperationReport.Status.CONVERGED)
178:             stateFeedback.follow(entity,plan,action);
179:         return result;
180:     }
181:     private OperationReport.StepReceipt writeStateAction(Entity entity,TerminalOperationPlan plan,TerminalActionPolicy.Step action,
182:         java.util.function.BooleanSupplier permitted) {
183:         var policy=runtime.strengthPolicy();
184:         // Check requested settings first; actual boundary evidence is acquired below before any write.
185:         var requested=TerminalActionPolicy.resolve(policy,action,true);
186:         if(!requested.permitted())return new OperationReport.StepReceipt(TerminalOperationPlan.Step.TERMINAL_STATE,
187:             OperationReport.Status.UNAVAILABLE,requested.reason());
188:         return observedStep(plan,TerminalOperationPlan.Step.TERMINAL_STATE,action.mask(),permitted,
189:             gate->TerminalStateSink.reassert(entity,plan,registeredStateLevel(entity,plan),gate,
190:                 action==TerminalActionPolicy.Step.LIFE,action==TerminalActionPolicy.Step.REMOVAL));
```

<a id="recovery"></a>
## 恢复链通用门与局部通道

`src/main/java/dev/ronova/coremod/policy/RecoveryChainPolicy.java`

### L48–L67

```text
48:     public static Decision resolve(StrengthPolicySnapshot policy, Step step) {
49:         Objects.requireNonNull(policy); Objects.requireNonNull(step);
50:         MechanismLevel requested = requested(policy, step);
51:         boolean observes = requested.rank() >= step.minimum().rank();
52:         if (!observes) return new Decision(step, requested, MechanismLevel.OFF, false, false,
53:             "OUTSIDE_ACTIVE_RECOVERY_DEPTH");
54:         var resolution = policy.resolveAttack(step.actions(), step.keys());
55:         boolean executes = resolution.permitted() && resolution.effectiveDepth().rank() >= step.minimum().rank();
56:         return new Decision(step, requested, resolution.effectiveDepth(), true, executes,
57:             executes ? "POLICY_PERMITTED_REQUIRES_CURRENT_SOURCE_AND_WRITE_LEASE"
58:                 : resolution.permitted() ? "RECOVERY_DEPTH_LIMITED_BY_DEPENDENCY" : resolution.reason());
59:     }
60:     private static MechanismLevel requested(StrengthPolicySnapshot policy, Step step) {
61:         MechanismLevel requested = MechanismLevel.STRICT_ALL;
62:         for (AttackAction action : step.actions()) requested = MechanismLevel.lowerOf(requested,
63:             policy.activeActions().resolve(policy.activeProfile(), action).depth());
64:         for (StrengthOptionKey key : step.keys()) requested = MechanismLevel.lowerOf(requested,
65:             ProfilePresetMatrix.resolve(policy.activeMatrixRevision(), policy.activeProfile(), key,
66:                 policy.activeLevels().get(key)).requestedLevel());
67:         return requested;
```

### L74–L121

```text
74:     /** Exact static AtomicReference invalidation; this does not raise the general record or save ceiling. */
75:     public static Decision registeredAtomicSnapshot(StrengthPolicySnapshot policy, int copyDepth, boolean bindingVerified) {
76:         if (copyDepth < 0) throw new IllegalArgumentException("invalid copy depth");
77:         Step step = Step.RECORD_DISPOSAL;
78:         var requested = requested(policy, step);
79:         var minimum = step.minimum();
80:         if (copyDepth > 1) {
81:             requested = MechanismLevel.lowerOf(requested, requested(policy, Step.DERIVED_SOURCE));
82:             minimum = Step.DERIVED_SOURCE.minimum();
83:         }
84:         for (var key : NBT_GATE_KEYS) requested = MechanismLevel.lowerOf(requested,
85:             ProfilePresetMatrix.resolve(policy.activeMatrixRevision(), policy.activeProfile(), key,
86:                 policy.activeLevels().get(key)).requestedLevel());
87:         boolean observes = requested.rank() >= minimum.rank();
88:         boolean permitted = observes && !policy.pendingRestart() && bindingVerified;
89:         return new Decision(step, requested, permitted ? requested : MechanismLevel.OFF, observes, permitted,
90:             !observes ? "OUTSIDE_ACTIVE_RECOVERY_DEPTH" : policy.pendingRestart() ? "PENDING_RESTART"
91:                 : !bindingVerified ? "STATIC_ATOMIC_BINDING_UNVERIFIED" : "PERMITTED_FOR_THIS_STATIC_ATOMIC_SNAPSHOT_ONLY");
92:     }
93:     /** Scopes capability evidence to the registered NBT outlet; it is never published as a global key ceiling. */
94:     public static Decision registeredNbtOutlet(StrengthPolicySnapshot policy, int copyDepth, boolean boundaryVerified) {
95:         Step step = copyDepth > 1 ? Step.DERIVED_SOURCE : Step.REBUILD_OUTLET;
96:         if (copyDepth < 0) throw new IllegalArgumentException("invalid copy depth");
97:         var requested = requested(policy, step);
98:         for (var key : NBT_GATE_KEYS) requested = MechanismLevel.lowerOf(requested,
99:             ProfilePresetMatrix.resolve(policy.activeMatrixRevision(), policy.activeProfile(), key,
100:                 policy.activeLevels().get(key)).requestedLevel());
101:         boolean observes = requested.rank() >= step.minimum().rank();
102:         boolean permitted = observes && !policy.pendingRestart() && boundaryVerified;
103:         return new Decision(step, requested, permitted ? requested : MechanismLevel.OFF, observes, permitted,
104:             !observes ? "OUTSIDE_ACTIVE_RECOVERY_DEPTH" : policy.pendingRestart() ? "PENDING_RESTART"
105:                 : !boundaryVerified ? "REGISTERED_NBT_BOUNDARY_UNVERIFIED" : "PERMITTED_FOR_THIS_OBSERVED_NBT_SOURCE_ONLY");
106:     }
107:     public static Decision registeredTaskDisposition(StrengthPolicySnapshot policy,int copyDepth,boolean bindingVerified) {
108:         return registeredWork(policy,copyDepth>1?Step.DESCENDANT_TASK:Step.DIRECT_TASK,copyDepth,bindingVerified);
109:     }
110:     public static Decision registeredStorageCommit(StrengthPolicySnapshot policy,int copyDepth,boolean bindingVerified) {
111:         return registeredWork(policy,copyDepth>1?Step.RELOAD_RECONSTRUCTION:Step.SAVED_RECORD_DISPOSAL,copyDepth,bindingVerified);
112:     }
113:     private static Decision registeredWork(StrengthPolicySnapshot policy,Step step,int copyDepth,boolean bindingVerified) {
114:         if(copyDepth<0)throw new IllegalArgumentException("invalid copy depth");
115:         var requested=requested(policy,step);
116:         for(var key:NBT_GATE_KEYS)requested=MechanismLevel.lowerOf(requested,
117:             ProfilePresetMatrix.resolve(policy.activeMatrixRevision(),policy.activeProfile(),key,policy.activeLevels().get(key)).requestedLevel());
118:         boolean observes=requested.rank()>=step.minimum().rank(),permitted=observes&&!policy.pendingRestart()&&bindingVerified;
119:         return new Decision(step,requested,permitted?requested:MechanismLevel.OFF,observes,permitted,
120:             !observes?"OUTSIDE_ACTIVE_RECOVERY_DEPTH":policy.pendingRestart()?"PENDING_RESTART":!bindingVerified?"RECOVERY_WORK_BINDING_UNVERIFIED":"PERMITTED_FOR_THIS_REGISTERED_RECOVERY_WORK_ONLY");
121:     }
```

<a id="native"></a>
## 原生能力申请与语义证明

`native/src/ronova_native.cpp`

### L415–L420

```text
415: bool has_semantic_proof(JNIEnv* env, jstring name, jbyteArray bytes, jobject loader) {
416:     if (g_verify_method == nullptr || name == nullptr || bytes == nullptr) return false;
417:     const jboolean verified = env->CallStaticBooleanMethod(g_transform_bridge, g_verify_method, name, bytes, loader);
418:     if (env->ExceptionCheck()) { env->ExceptionClear(); return false; }
419:     return verified == JNI_TRUE;
420: }
```

### L803–L847

```text
803:     g_transform_bridge = static_cast<jclass>(env->NewGlobalRef(local_bridge));
804:     env->DeleteLocalRef(local_bridge);
805:     g_transform_method = env->GetStaticMethodID(
806:         g_transform_bridge, "transform", "(Ljava/lang/String;[B)[B"
807:     );
808:     if (g_transform_method == nullptr) {
809:         env->ExceptionClear();
810:         return mask;
811:     }
812:     g_verify_method = env->GetStaticMethodID(
813:         g_transform_bridge, "verify", "(Ljava/lang/String;[BLjava/lang/ClassLoader;)Z"
814:     );
815:     if (g_verify_method == nullptr) { env->ExceptionClear(); return mask; }
816: 
817:     jvmtiCapabilities potential{};
818:     jvmtiCapabilities requested{};
819:     g_jvmti->GetPotentialCapabilities(&potential);
820:     requested.can_retransform_classes = potential.can_retransform_classes;
821:     requested.can_retransform_any_class = potential.can_retransform_any_class;
822:     requested.can_generate_all_class_hook_events = potential.can_generate_all_class_hook_events;
823:     requested.can_access_local_variables = potential.can_access_local_variables;
824:     if (g_jvmti->AddCapabilities(&requested) == JVMTI_ERROR_NONE && requested.can_retransform_classes) {
825:         mask |= CAP_RETRANSFORM;
826:     }
827: 
828:     jvmtiEventCallbacks callbacks{};
829:     callbacks.ClassFileLoadHook = &class_file_load_hook;
830:     callbacks.ThreadEnd = &task_thread_end;
831: #ifdef RONOVA_CODE_SEALS
832:     callbacks.ClassPrepare = &code_class_prepared;
833: #endif
834:     if (g_jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks)) == JVMTI_ERROR_NONE &&
835:         g_jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, nullptr) == JVMTI_ERROR_NONE) {
836:         mask |= CAP_HOOK;
837:         g_jvmti->SetEventNotificationMode(JVMTI_ENABLE,JVMTI_EVENT_THREAD_END,nullptr);
838:     }
839:     if ((mask & CAP_HOOK) != 0 && (mask & CAP_RETRANSFORM) != 0) {
840:         // One eager pass establishes query/lifecycle filters. The watchdog is
841:         // only a low-frequency fallback; ordinary retransforms are handled by
842:         // ClassFileLoadHook as they happen.
843:         retransform_targets(env, true);
844:         reassert_core_targets(env);
845:         mask |= CAP_SEMANTIC_LEASE;
846:     }
847:     return mask;
```

<a id="b1"></a>
## B1 的来源限定：已经是 prefix 路线

`docs/plans/MS-SKILLS-PLAN.md`

### L14478–L14490

```text
14478: | **I-10** | 载荷伪装 | MS 无语 | 与自有可验证性取向冲突，属取舍 |
14479: 
14480: **I 块的定位**：这些是「MS 未禁止、且能提升强度」的空间。它们**不是**本方案的优先级 —
14481: 优先级由 [§6](MS-SKILLS-PLAN.md#orig-rule-l0869) 的依赖顺序决定；I 块的价值在于**取消了一条没有依据的自我限制**。
14482: 
14483: 特别地，**B1 与 I-2 是同一技术路线的两端**：B1 用官方 JVMTI（`SetNativeMethodPrefixes`）
14484: 拿到「纯 Java Hook Native 函数」，I-2 用 [`skills:324`](../../.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md#实现一个简单-hook) 明许的 `Unsafe` 路线拿到更低层。
14485: 两者都应在实现「失败即拒绝」的前提下推进。
14486: 
14487: ---
14488: 
14489: 
14490: <!-- RONOVA-ORIGINAL-END orig-rule-l0845 -->
```

### L14533–L14546

```text
14533: **现状**（`ronova_native.cpp:817-823`，申请 4 项）：
14534: `can_retransform_classes` / `can_retransform_any_class` / `can_generate_all_class_hook_events` / `can_access_local_variables`
14535: 
14536: **待加**（全部经 JDK 17 `jvmti.h` 核实存在）：
14537: 
14538: | ID | 能力位 | 行 | 用途 | 打开的能力 |
14539: |---|---|---|---|---|
14540: | **B1** | `can_set_native_method_prefix` | `:706` | `SetNativeMethodPrefix`（`:1417`）/ `SetNativeMethodPrefixes`（`:1421`） | **T0-② 完整形态**：原生方法层接管，覆盖多 classloader。直接命中 `intercept.jni_native` |
14541: | **B2** | `can_get_bytecodes` | `:673` | `GetBytecodes`（`:1426`） | **不变量级证明的基础**（F1） |
14542: | **B3** | `can_get_constant_pool` | `:705` | 校验常量池引用目标 | `attack.dispatch`、`intercept.unsafe_reflection` |
14543: | **B4** | `can_generate_compiled_method_load_events` | `:697` | JIT 编译/回退事件 | **关闭 `attack.dispatch` 的 JIT 缺口** |
14544: | **B5** | `can_tag_objects` | `:670` | 跨事件关联同一对象，不依赖弱引用 | `protect.entity_record`、E1 候选节点标识 |
14545: | **B6** | `can_get_source_file_name` / `can_get_line_numbers` | `:681` / `:682` | 诊断归因 | `runtime.anomalous_call_log` |
14546: 
```

<a id="diagnostics"></a>
## 补充：动作诊断硬编码不能当成执行器清点

`DominionCommands.reportStrength` 把 TERMINALIZE 以外的动作统一打印为未接线。该诊断没有查询 TerminalActionPolicy 的局部步骤，故不能用它证明全项目只有一个动作有执行接线。

### `src/main/java/dev/ronova/coremod/command/DominionCommands.java`

```text
265:     private static int reportStrength(CommandSourceStack source) {
266:         var policy = ServerRuntimeRegistry.runtime(source.getServer()).strengthPolicy();
267:         source.sendSuccess(() -> Component.literal(
268:             "strength_revision=" + policy.revision().value()
269:                 + ", configured_matrix=" + policy.configuredMatrixRevision()
270:                 + ", active_matrix=" + policy.activeMatrixRevision()
271:                 + ", configured=" + policy.configuredProfile()
272:                 + ", active=" + policy.activeProfile()
273:                 + ", pending_restart=" + policy.pendingRestart()
274:         ), false);
275:         for (AttackAction action : AttackAction.values()) {
276:             var resolution = policy.resolveAttack(java.util.Set.of(action), java.util.Set.of());
277:             source.sendSuccess(() -> Component.literal(
278:                 action.displayName() + ": 保存=" + policy.configuredActions().levels().get(action)
279:                     + ", 当前=" + policy.activeActions().resolve(policy.activeProfile(), action)
280:                     + ", 接线=" + (action == AttackAction.TERMINALIZE
281:                         ? "新终结提交门" : "未接线（仅保存请求）")
282:                     + ", 高级解析=" + resolution.reason()
283:             ), false);
284:         }
285:         source.sendSuccess(() -> Component.literal(
286:             "关闭终结不会解除已有裁决。其余普通项尚不改变旧终态物理链。"
287:         ), false);
288:         return policy.pendingRestart() ? 0 : 1;
```

### `src/main/java/dev/ronova/coremod/policy/StrengthPolicyConfiguration.java`

```text
84:     /** Additional permission gate for existing baseline lanes, not a capability claim. */
85:     public boolean permitsBaselineAction(AttackAction action) {
86:         Objects.requireNonNull(action, "action");
87:         // Never use this legacy gate as an evidence-free authorization for a
88:         // future code/return/render executor. Those require resolveAttack.
89:         if (action != AttackAction.TERMINALIZE) {
90:             return false;
91:         }
92:         if (attacks.resolve(profile, action).depth() == MechanismLevel.OFF) return false;
93:         for (StrengthOptionKey key : action.requiredKeys()) {
94:             if (ProfilePresetMatrix.resolve(profileMatrixRevision, profile, key, levels.get(key))
95:                 .requestedLevel().rank() < MechanismLevel.PROTECT_RONOVA.rank()) return false;
96:         }
97:         return true;
98:     }
```

### `src/main/java/dev/ronova/coremod/persistence/ShadowDurableRootStore.java`

```text
1237:     public synchronized EffectClaim prepareTerminalEffect(dev.ronova.coremod.root.api.TerminalOperationPlan plan, String physicalIdentity) {
1238:         if (!validateTerminalPlan(plan) || physicalIdentity == null || physicalIdentity.length() > 256)
1239:             return new EffectClaim(EffectClaim.Status.REJECTED, null);
1240:         EffectKey key = plan.effectKey();
1241:         if (effectJournal.get(key) != null) return recoverEffect(key);
1242:         EffectKey old = new EffectKey(plan.commandId(), plan.qualification().targetEpoch(), EffectRecord.PUBLIC_DEATH, EffectRecord.ONCE_SLOT);
1243:         if (effectJournal.get(old) != null) return new EffectClaim(EffectClaim.Status.LEGACY_CLAIM_UNKNOWN, effectJournal.get(old));
1244:         if (lifecycleReceipts.containsKey(plan.subjectKey().subjectId())) return new EffectClaim(EffectClaim.Status.LEGACY_CLAIM_UNKNOWN, null);
1245:         // A retained terminal qualification still supports independent, separately permitted actions.
1246:         // The ordinary terminalize switch only gates new public lifecycle attempts here.
1247:         if (!activeStrengthPolicyConfiguration.permitsBaselineAction(dev.ronova.coremod.policy.AttackAction.TERMINALIZE))
1248:             return new EffectClaim(EffectClaim.Status.REJECTED, null);
1249:         var authority = commandJournal.latestQualification(plan.qualification().targetEpoch());
1250:         if (authority == null || !authority.qualification().equals(plan.qualification())) return new EffectClaim(EffectClaim.Status.REJECTED, null);
1251:         return persistEffectIntent(EffectRecord.terminalLifecycle(key, plan.authorityHash(), plan.authorityCommitIndex(),
1252:             plan.activePolicyRevision(), plan.planRevision()), physicalIdentity);
```

### `src/main/java/dev/ronova/coremod/client/RonovaStrengthScreen.java`

```text
169:         });
170:         return text.append("\n完成状态以服务端观察为准；客户端回执仅确认投影数据。").toString();
171:     }
172:     private static String actionCapability(StrengthControl.Action action) {
173:         if(action.effective()==MechanismLevel.PROTECT_RONOVA&&java.util.Set.of(AttackAction.REWRITE_LIFE,
174:             AttackAction.DIRECT_CLEAR,AttackAction.CLEAR_ENTITY_RECORDS).contains(action.key()))return "实体局部路径";
175:         return level(action.effective());
176:     }
177:     private static String profile(StrengthProfile p){return switch(p){case COMPATIBLE->"兼容";case ENHANCED->"强化";case EXTREME->"极限代码对抗";case CUSTOM->"自定义";};}
```

### `native/src/ronova_native.cpp`

```text
779: 
780: JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
781:     g_vm = vm;
782:     void* environment = nullptr;
783:     if (vm->GetEnv(&environment, JVMTI_VERSION_1_2) == JNI_OK) {
784:         g_jvmti = static_cast<jvmtiEnv*>(environment);
785:     }
786:     return JNI_VERSION_1_8;
787: }
788: 
789: extern "C" JNIEXPORT jint JNICALL
790: Java_dev_ronova_coremod_nativebridge_NativeBridge_bootstrap(JNIEnv* env, jclass owner) {
791:     if (g_jvmti == nullptr) return 0;
792:     jint mask = CAP_JVMTI;
793:     jobject loader=nullptr;
794:     if(g_jvmti->GetClassLoader(owner,&loader)==JVMTI_ERROR_NONE&&loader!=nullptr) {
795:         g_root_loader=env->NewGlobalRef(loader);env->DeleteLocalRef(loader);
796:     }
797: 
798:     jclass local_bridge = env->FindClass("dev/ronova/coremod/agent/NativeTransformBridge");
799:     if (local_bridge == nullptr) {
```

原 native/ 树文本搜索：找到 GetPotentialCapabilities 一处；未找到 GetCapabilities、Agent_OnLoad、Agent_OnAttach 的实现/调用。JNI_OnLoad 不等于 JVMTI Agent_OnLoad。这里不据静态文本推定用户 JVM 的实际能力集。
