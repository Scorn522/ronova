# R66 后继闭合变更审查件（recovery task closure change review）

本件是**送审件**：把我改了什么、为什么改、验证到什么程度、以及**我自己不确定的地方**
一次交清，供静态审查。审查结论请直接回在本件上，我负责验证你的结论（我这边能跑 Gradle，
约 1 分 30 秒一轮；你那边不能跑，所以"你判定、我验证"是当前唯一可行的分工）。

---

## 0. 本件性质与边界

**已验证（本机真实执行，非推断）**

- `compileJava`、`compileTestJava` 编译通过。
- `test` + `selfTest` 全量套件通过：**BUILD SUCCESSFUL in 1m 30s**，
  `112 actionable tasks: 105 executed, 7 up-to-date`。
- 密封 JAR 由本轮源码重建（`jar` → `finalizedBy reobfJar`），native DLL 随之重封。

**未验证（明确未做，不构成任何暗示）**

- ❌ 未启动游戏、未跑隔离专用服、未做实机验证。
- ❌ 未改 `validation/` 下任何台账，未回填任何 PASS，未覆盖任何历史失败。
  特别是 `validation/7.17/phase1-task-disposition/round-4.json` 原样保留。
- ❌ 未宣称任何验收项转 PASS，未宣称第一阶段有任何进展性完成。

**本轮结论的性质**

本轮**唯一的新增功能改动**是修复一个我自己在 09-11 那轮引入的缺陷。
不新增能力、不改写路径、不改 schema、不改 operation、不改 slot、不改 canonical 描述。

---

## 1. 送审变更集与基线

**基线**：`ronova-source-0.2.0-alpha.7.17-r66.zip`（618 个条目）。
逐条目 SHA-256 比对（非抽样，全量）：

| 结果 | 数量 |
|---|---|
| 与基线完全相同 | **613** |
| 改动 | **5** |
| 本地缺失 | **0** |

5 个改动文件全部集中在"第三方任务与资源结算"（块 3）的纵向切片内：

| # | 文件 | 来源轮次 | 说明 |
|---|---|---|---|
| 1 | `observation/TaskExecutionGraph.java` | 09-11 | 队列状态与后继闭合判据；上一版报告已送审，本轮未再改动 |
| 2 | `observation/RecoveryTaskDisposition.java` | **本轮** | **本件主角**：修复 D-08 |
| 3 | `root/api/RecoveryWorkDescriptor.java` | 09-11 | `NOT_EXECUTED` 归入 `TASK_CANCEL` 类别门 |
| 4 | `test/…/TaskExecutionGraphSelfTest.java` | 09-11 | 队列/退出分离与后继闭合断言 |
| 5 | `test/…/RecoveryWorkScenarios.java` | 09-11 | 两事实的持久语义断言 |

> 也就是说：**除本件第 2 项外，工作树与已交付源码包逐字节相同。**

---

## 2. 本轮新发现并修复的缺陷：D-08

### 2.1 该轮自述的意图

09-11 那轮的改动记录 `DEEPSEEK-CHANGELOG.md` 明确承诺了两条分支共用同一闭合判据：

> `:22` …抽出 `recordSuccessorsClosed` helper，**取消分支与正常退出分支共用**…
>
> `:31` 2. **正常退出分支**：`EXECUTION_EXITED` 已写入时评估
>      （`RecoveryTaskDisposition` 维护轮的既有 `work!=null` 路径）。

同文件 `:13` 同时承认：**"哪些已验证：无。所有新增断言尚未执行过。"**

> **注（为何此处转述而不就地标注）**：`DEEPSEEK-CHANGELOG.md` 是重构核对器的**冻结输入**——
> 它登记在 `docs/plans/restructure/input-file-hashes.json` 中且不属于 `source-manifest.json`
> 的导航来源，因此 `verify_restructure.py` 会把任何改动判为 `Undeclared input modifications`。
> 我实测确认了这一点（就地更正 → 核对器 exit 1），随后逐字节还原
> （SHA-256 与申报基线 `dc4aaea5…` 一致，核对器恢复 exit 0）。
> 所以该文档的原文一字未动，**:31 那句为假**这一更正记录在本件与
> `docs/plans/RESTRUCTURE-REVIEW.md` 的对应回执中。
> 换言之：**该轮自述的"两条分支共用"，在文档里仍然写着，但它不成立。**
> 本件下方的引文均照抄原文，未作修改。

### 2.2 实际行为

**"正常退出分支"从未生效过。** 它是一次死代码。

09-11 版本的守卫是：

```java
flush(work);
// A task that ran and exited carries its own closure proof on the normal path too; …
if(work.facts.contains(RecoveryWorkResult.Fact.EXECUTION_EXITED))
    recordSuccessorsClosed(work,view.registration());
```

`flush()` 会把它**成功落盘**的每个 fact 从集合里摘掉
（`RecoveryTaskDisposition:182`：`if(!store.observeRecoveryWork(work.key,fact))break;iterator.remove();`）。
因此紧接着的 `contains(EXECUTION_EXITED)`：

| 情形 | `flush` 结果 | `EXECUTION_EXITED` 是否仍在集合 | `:72` 守卫 | 是否写闭合 |
|---|---|---|---|---|
| **正常路径** | 成功 | **否**（已被 `iterator.remove()` 摘除） | **false** | **不写** |
| 落盘失败（降级） | 在更早的 fact 上 `break` | 是（枚举序在 3，未被遍历到） | true | 尝试写 |

也就是说，这个守卫恰好**只在持久化失败时才放行**——它退化成了一个重试入口，
而注释与 changelog 声称的"正常路径"永远不会发生。

**可加性说明（这是最刺眼的一点）**：`EnumSet` 按枚举序迭代，
`EXECUTION_EXITED` 的 ordinal 是 3（`CANCEL_REQUESTED(1),CANCEL_ACCEPTED(2),CANCEL_REJECTED(4),EXECUTION_EXITED(8)`）。
所以只有当它**前面的某个 fact** 落盘失败时，循环才 `break` 在它之前，它才留在集合里。
守卫的正确性因此依赖于一个与语义无关的副作用。

### 2.3 机理（三步，可独立核对）

1. `:68`（09-11 版 `:66`）把 `EXECUTION_EXITED` 放进 `work.facts`。
2. `:71` `flush(work)` 落盘成功 → `:182` `iterator.remove()` 把它**移出**集合。
3. `:77`（09-11 版 `:72`）读 `work.facts.contains(…)` → **false** → `recordSuccessorsClosed` 不被调用。

而且**后续轮次也不会补上**：`:63` 的门槛 `view.executionsExited() && !facts.contains(EXECUTION_EXITED)`
虽然会重新成立，但 `:67` 的 `!persisted.has(EXECUTION_EXITED)` 为 false（已落盘），
所以 fact 不会被重新加回，`:72` 永远保持 false。

### 2.4 影响面（以及它为何至今沉默）

我做了**全仓枚举**（不只按名 grep，枚举 `RecoveryWorkResult.Fact.` 的每一处使用）：

- `SUCCESSORS_CLOSED` 在 `src/main` 中**只有一个生产点**：`RecoveryTaskDisposition:175`。
- `SUCCESSORS_CLOSED` 在 `src/main` 中**零消费者**——没有任何代码读它。

所以 D-08 的功能影响面是：

| 维度 | 影响 |
|---|---|
| 事实缺失 | 每个"正常运行后退出"的任务都不会在持久镜像里留下 `SUCCESSORS_CLOSED` |
| 方向 | **fail-closed**（欠报，不会误报已闭合）——这是不幸中的万幸，也是它没被发现的原因 |
| 计数器 | `counters()` 的 `successors_closed`（`:189`）系统性少报 |
| 下游 | 任何将来把该 fact 当作"子树已可证明闭合"的消费者，都会永久看不到它 → 永不解除义务 |

**为什么既有测试抓不到**：

- `src/test` 对 `RecoveryTaskDisposition` **零引用**（枚举所有测试文件确认）。
- `RecoveryWorkScenarios:83` 的断言是 `store.observeRecoveryWork(plan.effectKey(), SUCCESSORS_CLOSED)`
  ——**测试自己把 fact 注入 store**，完全绕开了生产者。
  所以它验证的是"store 接受这个事实"，而不是"生产者会写这个事实"。

**结论：这个缺陷对现有套件是不可见的。** 这也正是它值得单独送审的原因。

### 2.5 修复

改为**闩锁式义务**（latch）：退出时记下"欠一份闭合证明"，
每个维护轮尝试清偿，**只有 store 真的收下该事实才清除闩锁**。

完整 diff（`git diff --no-index --unified=3`，基线为已交付源码包内的同名文件）：

```diff
@@ -19,6 +19,8 @@ final class RecoveryTaskDisposition implements AutoCloseable {
         final Runnable release;
         final EnumSet<RecoveryWorkResult.Fact> facts=EnumSet.noneOf(RecoveryWorkResult.Fact.class);
         boolean invoked;
+        /** An observed exit still owes its successor-closure proof; discharged on a later pass. */
+        boolean closurePending;
         long reviewedRevision=-1,nextReviewTick;
         int reviewDelay=1;
         Pending(Object task,RecoveryTaskPlan plan,Runnable release){this.task=new WeakReference<>(task);this.plan=plan;this.key=plan.effectKey();this.release=release;}
@@ -63,13 +65,16 @@ final class RecoveryTaskDisposition implements AutoCloseable {
                     // through NOT_EXECUTED instead, and never claims an exit it did not have.
                     var persisted=store.recoveryWorkResult(work.key);
                     if(persisted!=null&&!persisted.has(RecoveryWorkResult.Fact.EXECUTION_EXITED)) {
-                        work.facts.add(RecoveryWorkResult.Fact.EXECUTION_EXITED);exits++;
+                        work.facts.add(RecoveryWorkResult.Fact.EXECUTION_EXITED);exits++;work.closurePending=true;
                     }
                 }
                 flush(work);
                 // A task that ran and exited carries its own closure proof on the normal path too;
                 // descendants and the exited node itself must all be settled before this is recorded.
-                if(work.facts.contains(RecoveryWorkResult.Fact.EXECUTION_EXITED))recordSuccessorsClosed(work,view.registration());
+                // The obligation is latched rather than gated on the fact set: flush() removes the
+                // facts it persisted, so a completed flush no longer contains EXECUTION_EXITED and a
+                // set-membership test would silently restrict this call to the failed-persist path.
+                settleClosure(work,view.registration());
                 var prior=store.recoveryWorkResult(work.key);
                 if(prior!=null&&(prior.has(RecoveryWorkResult.Fact.CANCEL_REQUESTED)||prior.has(RecoveryWorkResult.Fact.EFFECT_UNKNOWN)))work.invoked=true;
                 if(work.invoked)continue;
@@ -136,29 +141,41 @@ final class RecoveryTaskDisposition implements AutoCloseable {
                             // separately; never mint EXECUTION_EXITED for a run that never happened.
                             work.facts.add(RecoveryWorkResult.Fact.NOT_EXECUTED);queuedSettled++;
                             lastStage="NATIVE_INVOKED_QUEUED_SETTLED";
-                            recordSuccessorsClosed(work,view.registration());
+                            work.closurePending=true;
                         }
                     }
                 }
             }
             flush(work);
+            settleClosure(work,view.registration());
         }
     }
+    /**
+     * Attempts a latched successor-closure proof and clears the latch only once the closure fact is
+     * recorded. A closure that is not yet provable stays latched: the graph revision produced when a
+     * descendant settles is what schedules the next attempt, so the obligation is never dropped.
+     */
+    private void settleClosure(Pending work,UUID registration) {
+        if(work!=null&&work.closurePending&&recordSuccessorsClosed(work,registration))work.closurePending=false;
+    }
     /**
      * Records successor closure for this task, but only against the exact same proof the queue
      * settlement uses: a complete observed subtree in which every descendant is settled. Facts are
      * flushed first because the store persists them one at a time by bit, so a partial write would
      * leave the intent half-recorded. Nothing here closes a resource or descendant obligation.
+     *
+     * @return true only when the store accepted the closure fact itself
      */
-    private void recordSuccessorsClosed(Pending work,UUID registration) {
-        if(work==null||registration==null)return;
-        if(!graph.successorsClosed(registration,64))return;
+    private boolean recordSuccessorsClosed(Pending work,UUID registration) {
+        if(work==null||registration==null)return false;
+        if(!graph.successorsClosed(registration,64))return false;
         for(var iterator=work.facts.iterator();iterator.hasNext();) {
             var fact=iterator.next();if(store.observeRecoveryWork(work.key,fact))iterator.remove();
         }
         if(store.observeRecoveryWork(work.key,RecoveryWorkResult.Fact.SUCCESSORS_CLOSED)) {
-            successorsClosed++;lastStage=lastStage+"_SUCCESSORS";
+            successorsClosed++;lastStage=lastStage+"_SUCCESSORS";return true;
         }
+        return false;
     }
```

**为什么不是"记住一个 boolean 就行"的极简改法**

有人会想：把 `:68` 的 `add` 返回值存成局部 `boolean`，然后 `if(exitedNow)`。这**不够**：
`successorsClosed()` 要求子树**全部**后代都已结清。父任务退出的那一刻，
子任务通常仍在执行中 → 判据为 false → 一次性尝试失败后**永不重试**，
等于把"死代码"换成"只在恰好没有后代时才生效的代码"，仍然是半个实现。

闩锁是让这个特性真正能成立的最小结构：**义务独立于事实集合存活**，
与这个类里其它义务（`invoked`、`release`）的处理方式一致。

**重试由什么触发（这是闩锁正确性的关键，请重点审）**

不是轮询，而是**图 revision 变化**：

- `:59` 的 `changed=work.reviewedRevision!=view.revision()` 放行本次处理；
- 后代结清时必然 `touch(target)` → revision 变化 → 下一轮放行 → `:77` 重试清偿。

所以"后代将来才结清"这件事本身就会把重试**调度**起来，不需要额外定时器。

### 2.6 我查证后**排除**的候选（不是缺陷，列出以免你重复劳动）

审计该文件时我一并怀疑了三处，逐一查证后排除：

| 候选 | 位置 | 查证结论 |
|---|---|---|
| 返回码映射把非 0/1/2/3 误记为 `CANCEL_REJECTED` | `:132-137` | **不是缺陷。** native 侧 `cancelRegisteredFuture0` 的返回码**恰为** {0,1,2,3}：`:1027 return accepted==JNI_TRUE?1:2;` 是 1/2 的**唯一**出口，且 `:1023-1026` 把任何不确定（异常、`context.uncertain`、未 commit、changes 不符、语义掩码不符）**强制**成 3。映射穷尽且正确。 |
| `entries>0` 门槛漏掉"排队未进入且没 intent"的任务 | `:63` | **不是缺陷**（但见 §4 第 5 条）。store 的 `observeRecoveryWork` 在无 intent 行时本就拒绝写入（`ShadowDurableRootStore:1147`），所以"无 intent 就不记退出"是既有分层的结果，不是本文件的选择。 |
| `successorsClosed()` 把 registration 自身也纳入判据 | `TaskExecutionGraph:185`（`walkChildren` 从自身起） | **不是缺陷。** 两个调用点都在"本节点已退出/已排队结清"之后调用，自身天然满足 `executionsExited()` 或 `queuedNeverEntered()`；纳入自身是保守方向。 |

---

## 3. 验证回执

**命令**

```
JAVA_HOME=D:\桌面\MOD\.build-tools\jdk17\jdk-17.0.15+6
GRADLE_USER_HOME=D:\桌面\MOD\.gradle-cache
gradlew compileJava compileTestJava test selfTest --console=plain
```

**结果**

| 项 | 值 |
|---|---|
| 退出状态 | **BUILD SUCCESSFUL in 1m 30s** |
| 任务 | `112 actionable tasks: 105 executed, 7 up-to-date` |
| 产物 | `build/libs/ronova-0.2.0-alpha.7.17-r66.jar` |
| 大小 / 时间 | 5,053,522 B / 09-15 14:11 |
| SHA-256 | `A08DD6D1730B35CE15207C60C2D81F2A841042881EB88719E968BB6C6B08F1F7` |
| `dev/ronova/*.class` | 735 |
| 内嵌 native | `native/windows-x86_64/ronova.dll` 1,955,328 B；`ronova_root.dll` 415,744 B |

相关自测通过行（节选，均为真实输出）：

```
TaskExecutionGraphSelfTest PASS (cancel versus exit, concurrent entries, descendants,
  periodic reentry, identity isolation, capacity, queue-versus-exit separation, successor closure)
EnforcementRuntimeSelfTest → PASS
DurableRootStoreSelfTest → PASS
```

> 记账更正：更早一份回执记录上一版 JAR 为"284 classes"。本轮实测 `dev/ronova/*.class` = **735**。
> 两者计数基准不同（前一数字的工具链未知），此处以可复现的实测值为准，差异记入待核对项。

---

## 4. 我自己不确定的点（请重点审这些，不要只审我自信的部分）

1. **闩锁的清偿时机是否够及时。**
   清偿只发生在本轮循环体走到 `:77` 或 `:150` 时。若某个任务同时满足
   "闩锁未清偿" 且 "图 revision 长期不变"，则本轮不会重试。
   我论证了"后代结清必然改 revision"，因此该组合在**后代将来会结清**的前提下不会僵死；
   但如果存在**后代永远不会结清、且 revision 不再变化**的路径，闩锁会永久挂起。
   我没有构造出这样的路径，但也没有证明它不存在。**请判定。**

2. **闩锁会不会与 `:80` 的 `continue` 相互作用。**
   `:80` 在 `work.invoked` 为真时结束本轮。`:77` 在它之前，所以闩锁仍会被尝试。
   但"已退出、未取消"的任务会走到 `:82` 之后并可能被**再次送入取消调用**——
   这是 09-11 轮既有行为，**不是本轮引入的**，我也没有改动它。
   我认为它无害（对已退出任务请求取消），但没有验证 native 侧对已运行完成任务的返回语义。
   **请判定这是否是独立缺陷。**

3. **本轮修复没有任何回归测试。**
   我**没有**为 D-08 写测试，这是本件最薄弱的环节，必须明说。
   原因是：生产者的可达性需要一整套 store 夹具（策略 → 判决 → terminal plan →
   `approveRecoveryTaskPlan` → `prepareRecoveryTask` → `acquireTaskContinuation` →
   真实的 `EnforcementRuntime` 证明），而 `src/test` 目前对该类**零引用**，不存在可复用的夹具。
   我评估了可达性并确认**测试是可行的**（native 调用在测试 JVM 中经
   `NativeBridge:199` 的 `LinkageError` 兜底返回 0，走到 `NATIVE_NOT_INVOKED; continue;`
   即可留下 `Pending`，下一轮就能触发 `:63` 的退出记账），
   但我选择先交本件而不是先花预算搭夹具。
   **请判定：这个缺陷是否值得我投入一轮搭夹具 + 补回归测试？**
   （我倾向于是——"没有测试"正是 D-08 得以存在的原因。）

4. **`observeRecoveryWork` 的"去重即成功"语义。**
   `ShadowDurableRootStore:1149` 在 `prior.evidenceRef().equals(evidence)` 时**直接返回 true**
   而不落盘。我的闩锁把这个 true 当作"已清偿"。对 `SUCCESSORS_CLOSED` 而言这是否安全
   （即：该 fact 的 evidenceRef 是否可能在**未真正记录该 fact** 的情况下与已有值相等）？
   我读了 `RecoveryWorkResult.observe` 但未穷尽证明。**请判定。**

5. **`:63` 的 `entries>0` 门槛与"无 intent"路径。**
   一个**已经运行并退出**、但从未有过 recovery-work intent 行的任务，
   永远不会被记 `EXECUTION_EXITED`（`:66` 的 `persisted!=null` 为 false）。
   我认为这是"无 intent 即无事实可依附"的既有分层结果、且方向安全（欠报），
   但它意味着 `execution_exits` 计数器的口径是"有 intent 的退出"。
   **请判定这个口径是否需要写进文档，或是否需要修正。**

6. **`settleClosure` 在 `:150` 的调用是否过度。**
   它使每个走到循环体末尾的轮次都多调一次 `graph.successorsClosed()`（内部会分配视图）。
   有 `work.closurePending` 短路，未闩锁时是单次 boolean 判断；
   但 `Pending` 为 null 的轮次也会调一次（我加了 `work!=null` 判断）。
   这是否违反本包的**预算/分配纪律**？我没有对应的预算测试覆盖。**请判定。**

---

## 5. 未覆盖与未决（继承自 09-11 轮，本轮未改变）

明确**未接线、非遗漏**：

- `RESOURCES_RELEASED` **无生产写入路径**。共享队列引用、线程池与其它主体资源的结算
  需要独立证明，**不得**由任务自身退出或取消推断。本轮未触碰。
- 委托／跨队列后继（`CompletableFuture` 同步后继无独立边界）未覆盖。
- 停服结算、共享队列引用结算未完成。
- `NOT_EXECUTED` 与 `SUCCESSORS_CLOSED` 只是该完成条件**可用的证据面**，
  **不构成**完成证据；Completion 的独立版本化语义尚未接入。
- 未关闭的验收项：`X65`（NOT_RUN）、`X63`/`X64`（PARTIAL）；`RECOVERY_TASKS` 真实证据未闭合。
- 实机待观测（09-11 轮提出，仍未做）：取消被接受的任务**不得**再出现 `entries>0`。
  该前提是 `NOT_EXECUTED` 正确性的依赖（`cancel(FALSE)` 的排除语义），
  本轮**无法**在单元层验证。

**本轮明确未宣称**：未宣称任务链已结清、未宣称资源已释放、
未宣称后继闭合已达生产口径、未宣称任何验收项转 PASS。

---

## 6. 对 2026-09-15 审查意见的逐条回复

### 6.0 根因：审查对象是 09-11 的冻结快照，不是当前工作树

该意见明确写着"**当前源码还没有包含上一份报告提出的 `unenteredSubmissions` 修复**"，
且依据是 `git status` / `git diff` 为空。这两点共同定位了问题：
**审查读的是 `validation/7.17/codex-review-deepseek-20260911-1/snapshot/`
（09-11 18:26 状态），而不是工作树。**

本机实测的判定依据：

| 证据 | 结果 |
|---|---|
| 该快照的 `TaskExecutionGraph.java` vs 工作树 | 418 行差异（`Compare-Object`） |
| 该快照的 `RecoveryTaskDisposition.java` vs 工作树（本轮修复前） | **逐字节相同** |
| 该快照包含的 java 文件数 | **4**（定向快照，非全树） |

`git status` 为空**不构成**工作树干净的证据：本项目这几处改动并未被 git 跟踪，
该命令看不到它们。

**这 6 项里有 5 项、以及全部 3 处测试不一致，在 09-11 那轮的后续修复中已经修掉**，
其中第 2 项是本轮（见 §2）修的。逐条如下。

### 6.1 逐条状态

| # | 优先级 | 意见 | 当前状态 | 证据（工作树真实行号） |
|---|---|---|---|---|
| 1 | 高 | 重新提交会继承"已退出"判断 | **已修**（09-11 后续轮） | `View.unenteredSubmissions` `:15`；`executionsExited()` `:34` 末尾 `&&unenteredSubmissions==0`；配对保留在 `observedCompletedRun` `:122`（`submissions>completed` 才留开放尾巴） |
| 2 | 高 | 保存成功后反而跳过子任务结束检查 | **本轮已修** | 见 §2.5；`settleClosure` `:158`，闩锁置位 `:68`/`:144` |
| 3 | 高 | 测试重复声明 `dependent`，阻断 `compileTestJava` | **已修**（09-11 后续轮） | 全文件 `dependent` 仅剩 2 处：声明 `:108`、使用 `:109`；`:105` 的重复声明已删 |
| 4 | 中 | 未执行节点绕过调度器完整性检查 | **已修**（09-11 后续轮） | `successorsClosed` `:173` 之后紧接 `:178` `if(!schedulers(...).complete())return false;` —— 排队节点**不再跳过**该检查。`submitted(task,null,null)` 经 `rememberScheduler` `:133` 置 `missingScheduler=true`，使 `complete()` `:145` 为 false ⇒ 返回 false。测试覆盖见 `:116` 的 `submitted(stranger,null,owner)` |
| 5 | 中 | 存储工作仍接受任务专属 `NOT_EXECUTED` | **已修**（09-11 后续轮） | `RecoveryWorkDescriptor:91`：`NOT_EXECUTED` 已从 `:90` 的无条件分支移入 `kind==Kind.TASK_CANCEL` |
| 6 | 中 | 普通 `test` 不执行这些独立自测；且它们经 `reobfJar` 触发封印 | **成立，仍然开放**（见 §6.2） | `build.gradle:325-333` 注册为独立 `JavaExec`，仅挂在 `selfTest` `:337-341`；`:330` `dependsOn …, tasks.named('reobfJar')` |

测试不一致三处：

| 位置 | 意见 | 当前状态 |
|---|---|---|
| `RecoveryWorkScenarios:121` | 仍与较早的 `cancellation.record()` 比较 | **已修** ⇒ `.equals(closed.record())` |
| `TaskExecutionGraphSelfTest:116` | 相同条件却要求相反判断 | **已修** ⇒ 该行现为 `"coverage loss did not revoke the queue reading"`；原条件已重写为 `submitted(stranger,null,owner)` |
| `TaskExecutionGraphSelfTest:125` | 环形 `complete()` 与 `:45` 自相矛盾 | **已修** ⇒ 该行现为 `closure.exited(running,1);`，矛盾的 `descendants(a,8).complete()&&` 已删除 |

### 6.2 第 6 项：接受，且它影响我的验证表述

这一项**我接受，且它部分修正了我此前的表述**：

- `test` 与 `selfTest` 是两条路径。**`test` 通过不能证明这些断言通过。**
  我本轮的验证命令是 `compileJava compileTestJava test selfTest`——
  **四者一起跑**，所以 §3 的回执成立；但单跑 `test` 确实不够，这一点此前没有写明。
- `:330` 的 `dependsOn reobfJar` 意味着跑这些自测**必然触发封印链**。
  所以本轮的 JAR/`build_id` 是我**主动**重建的产物（§3 已列其 SHA-256），
  不是"未触发封印"的产物。若验收要求"不触发封印地跑断言"，
  **当前构建脚本没有提供这样的执行路径**——这是构建配置的缺口，不是源码缺陷。
- 你提的"另设使用开发输出的执行路径"我认同方向，但**本轮不擅自改 `build.gradle`**：
  它同时决定 `packagedSelfTestClasspath` 与 `mappedRuntimeSelfTests` 的分组语义
  （`:314-333`），改错会把"对着打好的 JAR 验证"降级成"对着 class 目录验证"，
  反而削弱证据。**请你给出具体方案后再动。**

### 6.3 送审基线已刷新

为消除"审查对象过期"这一根因，已按**已交付源码包的同一 618 条目清单**
用当前工作树重建源码包（结构一致，逐条目替换为当前字节）：

| 项 | 值 |
|---|---|
| 文件 | `ronova-source-0.2.0-alpha.7.17-r66-code1.zip` |
| 条目 | 618（缺失 0） |
| 大小 | 3,770,422 B |
| SHA-256 | `CF68011245EC3555394065DD2482CCACEA0C5D67C04D57A75719CB78E7E82743` |

**请以该包为审查基线。** 它与工作树的偏离面就是 §1 的 5 个文件
（其中 `RecoveryTaskDisposition.java` 是本轮改动）。

---

## 7. D-09：批量合并丢弃事件先后顺序（**已确证，未修**）

2026-09-15 的审查意见第 2 项**成立**，且它命中的是我上一轮
`unenteredSubmissions` 补丁**没有堵住**的另一个洞。我按源码复核后确认如下。

### 7.1 反例成立

`TaskEventRun.append`（`:19-32`）接受三类事件：`kind=1` 进入（`:21`）、
`kind=2` 退出（`:24`）、`kind=3` 提交（`:27`）。

按「**进入 → 退出 → 再提交**」调用：

| 步 | 事件 | `append` 内的状态变化 | 是否被接受 |
|---|---|---|---|
| — | 构造，`running=true`, `execution=token0` | — | — |
| 1 | `kind=2, token=token0` | `token==execution` ✓ → `completed=1`, `running=false` | 接受 |
| 2 | `kind=3, token=0` | `:28` 只校验 `token==0` 与 scheduler → `submissions=1` | **接受** |

于是 `mergeInto`（`:34`）以 `completed=1, submissions=1` 调用
`TaskExecutionGraph.observedCompletedRun`，而后者在 `:122` 用
`if(submissions>completed)` 推导未执行数：**`1>1` 为假 ⇒ 不加**。

结果 `entries=1, exits=1, active=0, unenteredSubmissions=0` ⇒
`executionsExited()` 返回 **true**，但第 2 步那次提交发生在退出**之后**，
确实尚未执行。**判断错误。**

### 7.2 为什么加计数不够（我认为这是本项真正的难点）

同一对 `(completed=1, submissions=1)`、同样 `running=false`，来自两种不同历史：

| 历史 | 未执行提交数 |
|---|---|
| `提交 → 进入 → 退出` | 0 |
| `进入 → 退出 → 提交` | **1** |

**聚合二元组无法区分它们**——顺序信息在 `TaskEventRun` 内部**仍然存在**
（`running` 与各 `kind` 的接受条件），却在传给图时被压成两个总数而丢弃。
所以修法必须让 run 把自己的**净未执行数**传下去，而不是让图去猜。

### 7.3 修法形状（含一个报告未提到的次序陷阱）

1. `TaskEventRun` 增加 `long unentered`，与图内同构地增量维护：
   `kind==3` 时 `unentered++`；`kind==1` 时 `if(unentered>0)unentered--`。
2. `observedCompletedRun` 改收该净未执行数（替代 `submissions>completed` 推导），
   图侧改为 `target.unenteredSubmissions += unentered`。

**陷阱（我复核时发现，报告未涉及）**：`mergeInto` 当前先 `observedCompletedRun`
再于 `:35` 调 `graph.entered(...)`，而 `entered` 在 `:101` 会
`if(target.unenteredSubmissions>0)target.unenteredSubmissions--`。
对「构造时已 running、无 `kind=1` 事件」的 run，这个后置 `entered` 会把
**尾随的未执行提交错误地消费掉**（净额算成 0，仍然误判为已退出）。
正确次序是**先应用开场的 `entered`，再并入 run 的聚合**——
即 `unenteredSubmissions` 使这两步**不再可交换**。

因此本项**不是一行改动**，需要连同 `entered`／`observedCompletedRun` 的施加次序一并修正，
并配一个覆盖上述两种同聚合、异历史的反例测试。

### 7.4 可达性

**不是死代码**：`RecoveryTaskObserver:231` 构造 `TaskEventRun`，
`:184` 调 `value.mergeInto(graph)`，属服务器合并路径。

### 7.5 本轮处置

**本轮未修。** 理由是它需要次序语义重构 + 新的反例测试，
而本轮预算已用于 D-08 与其送审件；在预算末端抢改一个涉及
「已 receiver 验证事件」持续语义的核心类，风险高于收益。
**建议作为下一个独立工作单元**，并请审查者确认 §7.3 的次序判断是否正确。

---

## 8. 对 2026-09-15 第二轮（基于 code1 源码包）意见的回复：**三点全部成立**

这一轮审查者拿到了正确的源码包并**实际编译运行**了 `TaskExecutionGraphSelfTest`。
三点我逐条复核后**全部确认**。其中两点推翻了我自己的论证，记录如下。

### 8.1 我在此撤回两处自己的主张

1. **§4 不确定点第 1 条的错误回答。** 我当时论证"后代结清必然改 revision，
   因此闩锁不会僵死"。**这是错的。** `view(Node)`（`:226`）传的是
   `node.revision`——**节点自己的**版本；全局 `revision` 只在 `ancestry()`（`:235`）等处使用。
   子任务 `touch()` 只递增子任务自己的版本，**父节点的 `View.revision` 不变**。
   因此 `RecoveryTaskDisposition:59` 的 `!changed && …` 会照常 `continue`，
   `settleClosure` 永远到不了。审查者实测父节点版本在子任务退出前后都是 `8`，与此一致。
2. **§7.3 中"加计数不够"是对的，但我没有意识到我的补丁在另一侧开了新洞。**
   见 §8.2 第 2 条：那是**我的 `unenteredSubmissions` 补丁引入的回归**。

### 8.2 三点确认

| # | 意见 | 判定 | 机理 |
|---|---|---|---|
| 1 | 合并批次把"重新提交、尚未执行"误判为全部退出 | **成立**（即 §7 的 D-09，未修） | `TaskEventRun.append` 允许 `进入→退出→提交`，得 `completed=1,submissions=1`；`observedCompletedRun:122` 的 `submissions>completed` 为假 ⇒ 不计未执行 |
| 2 | 已完整执行的任务可能被**永久**当作尚未开始 | **成立，且是我不慎引入的回归** | 单独 `submitted()` 先使 `unentered=1`；随后 (进入,退出) 以 `completed=1,submissions=0` 合并，`:122` 只会**增加**该计数、**不会**用已完成的执行抵消先前那次等待提交。且 `mergeInto` 因 `running=false` 不再调 `entered()`，无人递减 ⇒ `unentered=1` 恒定 ⇒ `executionsExited()` 恒假。**补丁前该式为真**，故这是回归 |
| 3 | `closurePending` 可能永远得不到重试 | **成立** | 同 §8.1 第 1 条：`:59` 的跳过条件未考虑 `closurePending`，而重试键是**父节点**版本 |

**第 1 与第 2 点是同一个根因的两侧**：`unenteredSubmissions` 目前由
"单独 `submitted()` 递增 / `entered()` 递减 / 合并时按 `submissions-completed` 递增"
三条**互不知情**的路径维护，缺少一个**保序的净额来源**。
只补一侧必然在另一侧开洞——这正是第 1 点（欠计）与第 2 点（过计）同时出现的原因。

### 8.3 修正后的修法形状

**第 1+2 点必须一次性改，不能分两次：**

1. `TaskEventRun` 维护自己的 `unentered`（`kind==3` 递增；`kind==1` 时若非零递减），
   并把**净额**传给图；
2. `observedCompletedRun` 改为按该净额增减，**去掉 `submissions>completed` 的推导**；
3. 明确"合并批次能否抵消此前单独记录的等待提交"这一语义——
   第 2 点要求**能**（否则永久卡死），第 1 点要求**不能跨退出抵消**（否则漏计）。
   两者的分界是**次序**：批内退出之前的进入可抵消，退出之后的提交不可。
4. 施加次序：**先**开场 `entered`，**再**并入批次聚合（见 §7.3 的次序陷阱）。

**第 3 点（可独立、较小）**：把闩锁纳入 `:59` 的跳过条件，例如
`if(!changed&&(work.invoked&&work.facts.isEmpty()&&!work.closurePending||tick<work.nextReviewTick))continue;`
或改用全局 `revision` 作为重试键。**但它必须带一个"父版本不变、子任务结清"的测试**
——否则就是第二次"改了但没人能证明它生效"，即 D-08 的成因。

### 8.4 本轮处置：三点均**未修**

不修的理由一致：三处都落在"已 receiver 验证事件"的持续语义与维护轮调度上，
需要①保序净额重构 ②跨来源语义裁定 ③配套反例测试。
在预算末端对核心类连做三处语义改动而无法逐项验证，风险高于收益，
且会重演 D-08（未验证的改动 + 无覆盖）。**建议作为一个完整工作单元，
连同 §8.3 的四类测试一起提交。**

审查者"补上测试再提交修正后的源码包"的建议，我接受并作为下一单元的验收条件。
