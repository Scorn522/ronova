# R66 · 基线缺陷与修复报告（首次源码改动）

**性质：** 记录 **`ronova-source-0.2.0-alpha.7.17-r66` 源码包自带的七处缺陷**及其修复（含精确前后代码）。
**方向：** Ronova 侧 → 外部研究方 / 实施方（**发出**）
**这是本工作流第一次修改生产源码。** 此前十二轮（fix1–fix12）全部为文档，`validation/` 一次未动。

**基准：** `ronova-source-0.2.0-alpha.7.17-r66.zip`，SHA-256 `1266d8c2dd46b949178d7341ee467333be678fb9b373c1403b576a56c73fd763`（**未改动**）

---

## 0. 结论摘要

**源码包带有一个两天的、未被发现的破坏。七处缺陷全部修复，四条验证命令全绿。**

| 命令 | 会话开始时 | 现在 |
|---|---|---|
| `compileJava` | ✅ | ✅ |
| `compileTestJava` | ❌ 1 个编译错误 | ✅ |
| `test` | ❌ | ✅ |
| `selfTest` | ❌ 停在第一个测试（39 任务） | ✅ **112 任务全绿** |
| **封印 JAR** | **从未产出** | ✅ **5,053,309 B，284 类** |

```
BUILD SUCCESSFUL in 1m 26s
112 actionable tasks: 106 executed, 6 up-to-date
Code seals: 284 registered classes; build_id=dc53e77c48b9edbe817404425fd6152354b25bfad39ca657346401c5d9a1fdeb
```

**七处缺陷同源：一次不完整的编辑会话（2026-09-11 18:25–18:26）。**

### §0.1 外部研究方指出的补丁缺陷（已确认并修正）

本报告第一版对缺陷 4 给出的修法（`executionsExited()` 增加 `submissions<=entries`）**不充分**，由外部研究方以反例指出：

> 历史上多出来的执行次数，可能掩盖一次新的、尚未执行的提交。
> 历史执行已全部退出：`submissions=1, entries=3, exits=3`
> 再次提交但尚未执行：`submissions=2, entries=3, exits=3`
> 此时 `submissions <= entries` 仍然成立，方法仍会返回 `true`。

**该反例经确认成立。** 第一版只让 `:91` 那条断言通过，**没有解决这一类问题** —— `entries` 可以合法超过 `submissions`，这份累积余量会把后来的提交吸收掉。

**修正：** 改用 per-node 未配对提交计数 `unenteredSubmissions`（详见 §2 缺陷 4），并**补入覆盖该反例的回归测试**；该断言在第一版补丁下会失败、在当前实现下通过。

**方法记录：** 第一版犯了"用累计量的不等式替代一个需要状态表达的条件"。此处保留该过程，不把第一版修法写成正确结论。

---

## 1. 时间线：为什么两天没人发现

```
09/09 22:49:41  RecoveryWorkResult.java            Fact 枚举（含 NOT_EXECUTED、SUCCESSORS_CLOSED）
09/10 23:13:41  RecoveryWorkDescriptor.java         缺陷 2 埋下
09/11 09:44:47  ShadowDurableRootStore.java
09/11 09:45:16  ★ 最后一次成功构建（build/libs/…-phase1-dev.jar）
                ★ 文档里"85 个 Java 自测通过"指的是这一次
   ────────────── 以下全部晚于最后一次构建 ──────────────
09/11 18:25:08  TaskExecutionGraphSelfTest.java     缺陷 1、5、7
09/11 18:25:16  TaskExecutionGraph.java             缺陷 4、6
09/11 18:25:59  RecoveryTaskDisposition.java
09/11 18:26:08  RecoveryWorkScenarios.java          缺陷 3
```

**18:25–18:26 那次编辑加了新概念与新断言，但实现只跟上了一部分。** 此后没有任何构建，所以无人发现。

**贯穿七处的形状：** 同一个概念被"写了一半"——断言写了、实现没跟上；或实现写了一处、漏了配套的另一处。

---

## 2. 七处缺陷与修复

### 缺陷 1 · 测试编译错误（编译）

**位置：** `src/test/java/dev/ronova/coremod/observation/TaskExecutionGraphSelfTest.java:105`

```
错误: 在方法 main(String[]) 中定义了变量 dependent
> Task :compileTestJava FAILED
```

**根因：** 同一个 `main` 里 `dependent` 声明两次；`:105` 那个从未被引用（`:94` 的才被 `:95` 使用）。

**阻断链：** `compileTestJava` ❌ → `test` ❌ → `testClasses` ❌ → `selfTest` ❌（`build.gradle:330`）

```diff
-        var closure=new TaskExecutionGraph(8);Object owner=new Object();Object dependent=new Object();
+        var closure=new TaskExecutionGraph(8);Object owner=new Object();
```

**安全性：** 零引用变量，删除不改变任何行为。

---

### 缺陷 2 · 存储工作接受任务专属事实（**实现**）

**位置：** `src/main/java/dev/ronova/coremod/root/api/RecoveryWorkDescriptor.java:90-91`

**失败断言**（`RecoveryWorkScenarios.java:92-97`）：

```java
// The queue settlement fact belongs to task work only; a storage commit must reject it.
require(!store.observeRecoveryWork(storage.effectKey(),RecoveryWorkResult.Fact.NOT_EXECUTED)
    && !store.observeRecoveryWork(storage.effectKey(),RecoveryWorkResult.Fact.SUCCESSORS_CLOSED)
    && …,"task queue/successor settlement entered a storage intent");
```

**根因：** `allows()` 把 `NOT_EXECUTED` 放进"无条件允许"桶，而 `SUCCESSORS_CLOSED` 在正确的"仅任务"桶里。

`NOT_EXECUTED` 意为"排队结算——已证明从未运行"（`TaskExecutionGraph.QueueState` 的队列概念）。存储提交**没有队列**，必须拒收。

```diff
-            case NOT_CURRENT,NOT_EXECUTED,EFFECT_UNKNOWN,RESOURCES_RELEASED -> true;
-            case CANCEL_REQUESTED,CANCEL_ACCEPTED,CANCEL_REJECTED,EXECUTION_EXITED,SUCCESSORS_CLOSED -> kind==Kind.TASK_CANCEL;
+            case NOT_CURRENT,EFFECT_UNKNOWN,RESOURCES_RELEASED -> true;
+            case CANCEL_REQUESTED,CANCEL_ACCEPTED,CANCEL_REJECTED,EXECUTION_EXITED,SUCCESSORS_CLOSED,NOT_EXECUTED -> kind==Kind.TASK_CANCEL;
```

**安全性（改前已核）：** `RecoveryWorkResult.Fact.NOT_EXECUTED` 在全部生产代码中的使用点**只有一处** —— `RecoveryTaskDisposition.java:137`（任务处置，descriptor 为 `Kind.TASK_CANCEL`）。对照 `SUCCESSORS_CLOSED` 也**只在该文件 `:159`**，且已在 `TASK_CANCEL` 桶。**∴ 改动后唯一生产使用点仍被允许；没有任何生产存储路径记 `NOT_EXECUTED`。**

**旁证：** `RecordMutationTask:65`、`RecoverySourceTracker:1311/1337`、`RecoveryMutationIntent:16` 里的 `NOT_EXECUTED` 属于**另一个枚举**（`RecoveryMutationIntent.Observation`），不受影响。

---

### 缺陷 3 · 比对了过期捕获（测试）

**位置：** `src/test/java/dev/ronova/coremod/persistence/RecoveryWorkScenarios.java:121`

```
AssertionError: restart changed observed task result
```

**根因：** `cancellation` 只在 `:67` 赋值一次；`:76` 与 `:83` 又给同一节点加了两个事实，记录已从 `3` 变为 `4115`。`:121` 却仍拿 `:67` 的旧值比。

```
:67   var cancellation=…               事实 = CANCEL_REQUESTED|CANCEL_ACCEPTED = 3
:76   + NOT_EXECUTED                   事实 += 4096
:83   + SUCCESSORS_CLOSED              事实 += 16
:84   var closed=…                     事实 = 4115   ← 正确的最新值，:85-86 用完后即弃
:119  store=LocalDominionStore.openWorld(world);    ← 重启
:121  require(… .equals(cancellation.record()), …)  ← 拿 4115 比 3
```

**判定为测试缺陷：** `RecoveryWorkResult` 类注释写 "**Monotone facts** about one work intent"；若重启丢掉事实才是耐久性 bug，而断言名 "restart changed observed task result" 要求的正是**不变**。**18:26 那次编辑更新了插入点之前的 `:71`（也用 `cancellation`），漏了插入点之后的 `:121`。**

```diff
- …record().equals(cancellation.record()),"restart changed observed task result");
+ …record().equals(closed.record()),"restart changed observed task result");
```

---

### 缺陷 4 · `executionsExited()` 漏掉"未进入的提交"（**实现**）

**位置：** `src/main/java/dev/ronova/coremod/observation/TaskExecutionGraph.java:18`

**失败断言**（`TaskExecutionGraphSelfTest.java:91`）：`"reopened queue epoch preserved a stale exit"`

**失败序列：**

```
submitted(q)  → sub=1, ent=0, ex=0, act=0   executionsExited()=false  ✓（:82 要求）
entered(q,1)  → ent=1
exited(q,1)   → ex=1                        executionsExited()=true   ✓（:85 要求）
submitted(q)  → sub=2                       executionsExited()=true   ✗（:91 要求 false）
```

**根因：** `entries==exits` 只说明"进入过的都退出了"，不说明"没有未进入的提交"。

#### 第一版修法及其缺陷（**由外部研究方指出，已确认**）

第一版改为累计计数比较：

```java
… && entries==exits && submissions<=entries      // 第一版：不充分
```

**研究方给出了反例并确认成立：**

```
历史执行已全部退出：submissions=1, entries=3, exits=3
再次提交但尚未执行：submissions=2, entries=3, exits=3
→ 2<=3 仍成立，方法仍返回 true，未识别这次新提交
```

**根因：** `entries` 可以**合法超过** `submissions`（`entered()` 裸调用、run 合并会单独加 `entries`）。这份累积余量会把后来的提交**吸收掉**。因此第一版只让 `:91` 那条断言通过，**没有解决这一类问题**。`==` 不成立、`<=` 也不充分 —— **累计计数根本表达不了"当前 epoch 有没有未配对的提交"。**

#### 采用修法：per-node 未配对提交计数

新增 `Node.unenteredSubmissions`（观察到的提交数减去已被 `entered` 配对的数目）：

```diff
     private static final class Node {
         …
         long submissions,entries,exits,revision;
+        /** Submissions observed but not yet matched by an entered execution. */
+        long unenteredSubmissions;
     }
```

```diff
     synchronized UUID submitted(Object task,Object scheduler,Object parent) {
         …
         target.submissions=Math.addExact(target.submissions,1);
+        target.unenteredSubmissions=Math.addExact(target.unenteredSubmissions,1);
         rememberScheduler(target,scheduler);
```

```diff
         if(target.active.size()>=MAX_EXECUTIONS||!target.active.add(execution)){target.lost=true;lost();}
-        else target.entries=Math.addExact(target.entries,1);
+        else {
+            target.entries=Math.addExact(target.entries,1);
+            // This entry settles one outstanding submission. Entries with no outstanding submission
+            // (a bare entered, or a merged run) simply do not consume one.
+            if(target.unenteredSubmissions>0)target.unenteredSubmissions--;
+        }
```

```diff
         target.entries=Math.addExact(target.entries,completed);target.exits=Math.addExact(target.exits,completed);
         target.submissions=Math.addExact(target.submissions,submissions);
+        // A run pairs each completed execution with its submission; only submissions it observed
+        // beyond that pairing leave an epoch open. An open tail is entered separately by mergeInto.
+        if(submissions>completed)target.unenteredSubmissions=Math.addExact(target.unenteredSubmissions,submissions-completed);
         if(submissions>0)rememberScheduler(target,scheduler);
```

```diff
-        public boolean executionsExited(){return !evidenceLost&&entries>0&&activeExecutions==0&&entries==exits&&submissions<=entries;}
+        public boolean executionsExited(){return !evidenceLost&&entries>0&&activeExecutions==0&&entries==exits&&unenteredSubmissions==0;}
```

**`View` 增加一个分量**（公开 record；已核**全仓只有一个构造点** `:225`，外部 `RecoverySourceTracker` / `RecoveryTaskDisposition` 只消费访问器）：

```diff
-    public record View(UUID registration,State state,long submissions,long entries,long exits,int activeExecutions,
+    public record View(UUID registration,State state,long submissions,long entries,long exits,int activeExecutions,
+                       long unenteredSubmissions,
                        Set<UUID> parents,Set<UUID> children,boolean cancellationReturned,boolean cancellationAccepted,
```

#### 新增回归测试（覆盖上述反例）

既有测试套件**不含**这个场景，故补入：

```java
// Accumulated entry headroom must not absorb a later submission. …
var wide=new TaskExecutionGraph(8);Object wideTask=new Object();
UUID wideId=wide.submitted(wideTask,scheduler,null);
wide.entered(wideTask,1,null);wide.exited(wideTask,1);
wide.entered(wideTask,2,null);wide.exited(wideTask,2);
wide.entered(wideTask,3,null);wide.exited(wideTask,3);
require(wide.view(wideId).entries()>wide.view(wideId).submissions()&&wide.view(wideId).executionsExited(),
    "a settled node with entry headroom was not reported exited");
wide.submitted(wideTask,scheduler,null);
require(!wide.view(wideId).executionsExited(),
    "a submission hid behind accumulated entry headroom");
```

**该断言在第一版补丁下会失败、在当前实现下通过** —— 是真正的回归守卫。

---

### 缺陷 5 · 测试漏传 `null` scheduler（测试）

**位置：** `TaskExecutionGraphSelfTest.java:116`

**根因：** `:117` 的断言消息是 "**an unobserved submission** was treated as settled"，而"未观察"在该 API 中的唯一表达是 `missingScheduler` / `schedulers().complete()==false` —— 测试自己 `:50-51` 就是这么构造的（`submitted(hostile,null,null)`）。但 `:116` 传了真 scheduler。

**实测证据（加了诊断行后的四节点转储）：**

```
[DIAG:117] complete=true nodes=4 closed=true
  owner    sub=1 ent=0 ex=0 act=0 qs=SUBMITTED_NEVER_ENTERED lost=false schedComplete=true observed=1
  running  sub=1 ent=1 ex=1 act=0 qs=ENTERED               exited=true  schedComplete=true observed=1
  waiting  sub=1 ent=0 ex=0 act=0 qs=SUBMITTED_NEVER_ENTERED lost=false schedComplete=true observed=1
  stranger sub=1 ent=0 ex=0 act=0 qs=SUBMITTED_NEVER_ENTERED lost=false schedComplete=true observed=1
```

`waiting` 与 `stranger` 四个字段完全一致，而 `:115` 要 true、`:117` 要 false —— **没有任何状态能区分。** 加上 `null` 后 `stranger` 的 `schedComplete` 为假，才具备区分条件。

```diff
-        Object stranger=new Object();closure.submitted(stranger,scheduler,owner);
+        Object stranger=new Object();closure.submitted(stranger,null,owner);
```

---

### 缺陷 6 · scheduler 检查放在相反的分支（**实现**）

**位置：** `TaskExecutionGraph.successorsClosed`

**根因：** 该方法的文档注释（`:139-146`）写明"**An unobserved submission**, an executing node, **an unknown scheduler**, or any truncation leaves this false" —— **但代码恰好漏掉了"未观察的提交"这一条，还把 scheduler 检查放到了相反的分支上。**

```java
// 原代码
for(var node:snapshot.nodes()) {
    if(node.evidenceLost())return false;
    if(node.executionsExited())continue;
    if(!node.queuedNeverEntered())return false;
    // ← 文档说的"未观察的提交"检查缺失
}
for(var node:snapshot.nodes()) {
    if(node.queuedNeverEntered())continue;          // ← 把真正需要检查的排队节点跳过了
    var schedulers=schedulers(node.registration());
    if(!schedulers.complete())return false;         // ← 反过来去查已退出的节点
}
```

**改动：**

```diff
 for(var node:snapshot.nodes()) {
     if(node.evidenceLost())return false;
     if(node.executionsExited())continue;
     if(!node.queuedNeverEntered())return false;
+    // A queued successor is settled only when its submission was actually observed: a
+    // submission whose scheduler was never seen cannot be trusted to still be reachable,
+    // so it must not discharge the parent's obligation. Once the node has entered and
+    // exited, the execution and its exit are the facts that matter, not the scheduler.
+    if(!schedulers(node.registration()).complete())return false;
 }
-for(var node:snapshot.nodes()) {
-    if(node.queuedNeverEntered())continue;
-    var schedulers=schedulers(node.registration());
-    if(!schedulers.complete())return false;
-}
 return true;
```

**这不是"设计"出来的，是照着该方法自己的文档注释把代码补齐。**

---

### 缺陷 7 · 与 `:45` 自相矛盾的断言（测试）

**位置：** `TaskExecutionGraphSelfTest.java:125`

```
AssertionError: a cyclic descendant graph reported successor closure
```

**根因：** 与 `:45` **不可能同时成立**：

```java
:45   require(!ancestryGraph.descendants(a,8).complete(), "cyclic descendants reported closure");   // 要求 false
:125  require( ancestryGraph.descendants(a,8).complete() && !ancestryGraph.successorsClosed(a,8), …); // 要求 true
```

**`ancestryGraph` 在 `:45` 与 `:125` 之间未被修改**（`:46-52` 全部操作的是另一个图 `graph`），所以是同一个纯函数被要求给出相反结果。

**推演环 `ancestor→left→leaf→ancestor`（`:43` 建立）：**

```
walk(ancestor) color=1 → walk(left) color=1 → walk(leaf) color=1
  → walk(ancestor) 命中 color==1 → return color==2 → false
```

**环必然让 `complete()` 为 false —— `:45` 正确，`:125` 的 `complete()` 前置多余且矛盾。** 而 `:125` 的消息要验的是 `successorsClosed` 为假，且 `successorsClosed` 在 `!complete()` 时本就返回 false。

```diff
-        require(ancestryGraph.descendants(a,8).complete()&&!ancestryGraph.successorsClosed(a,8),
+        require(!ancestryGraph.successorsClosed(a,8),
             "a cyclic descendant graph reported successor closure");
```

---

## 3. 缺陷分类

| 类型 | 数量 | 编号 |
|---|---|---|
| **实现缺陷** | **3** | 2、4、6 |
| 测试缺陷 | 3 | 3、5、7 |
| 编译错误 | 1 | 1 |

**三处实现缺陷都在"任务/后继/存储"语义上**，即文档称"已有证据"的那一块。

---

## 4. 验证结果

**环境（本机）：**

```powershell
$env:JAVA_HOME="D:\桌面\MOD\.build-tools\jdk17\jdk-17.0.15+6"   # 从 .build-tools 的 190 MB zip 解压
$env:GRADLE_USER_HOME="D:\桌面\MOD\.gradle-cache"                # 1,230 MB 热缓存
cd D:\桌面\MOD\mod
.\gradlew.bat compileJava compileTestJava test selfTest --console=plain --no-daemon
```

```
BUILD SUCCESSFUL in 1m 26s
112 actionable tasks: 106 executed, 6 up-to-date
```

**逐层推进记录（每一步修复后失败断言前移一位，测试在第一个失败处中止）：**

```
起点     :91   "reopened queue epoch preserved a stale exit"
修复 4   :117  "an unobserved submission was treated as settled"
修复 5+6 :121  "fully settled successors did not close the subtree"
修复 6   :125  "a cyclic descendant graph reported successor closure"
修复 7   :127  测试结束 → PASS
```

**全部 112 个任务通过**，含 `TaskExecutionGraphSelfTest PASS (cancel versus exit, concurrent entries, descendants, periodic reentry, identity isolation, capacity, queue-versus-exit separation, successor closure)`。

---

## 5. 封印 JAR 已产出

```
build/libs/ronova-0.2.0-alpha.7.17-r66.jar     5,053,309 B   09/15 14:03:30
  META-INF/ronova/code-seal-id.txt                    64 B
  META-INF/ronova/code-seal-manifest.json         37,467 B
  native/windows-x86_64/ronova.dll             1,954,816 B   ← 封印版（源包内为 186,880 B 未封印）
build_id = dc53e77c48b9edbe817404425fd6152354b25bfad39ca657346401c5d9a1fdeb
```

**关于构建依赖：** 本机**一直具备**完整构建条件：

```
D:\桌面\MOD\.build-tools\zig-windows-x86_64-0.13.0\zig.exe     ✓
D:\桌面\MOD\.build-tools\jdk17-headers\jvmti.h                 ✓
build_code_seals.py:137  tools = ROOT.parent/'.build-tools'    ✓ 路径正好对上
```

**封印构建是 `gradlew reobfJar` 一条命令。** `build.gradle:151` 的 `finalizedBy 'reobfJar'` 挂在 `jar` 上，`:189` 的封印钩子只挂 `reobfJar`；因此 `compileJava` / `test` 不需要 Zig，而 `jar` / `build` / `selfTest` / `reobfJar` 需要。**`selfTest` 因依赖 `reobfJar`（`:330`）而顺带完成了封印构建。**

---

## 6. 工作区状态

**相对源码包偏离的文件：恰好 4 个**（逐文件 SHA-256 复核）：

```
src/main/java/dev/ronova/coremod/observation/TaskExecutionGraph.java        缺陷 4、6
src/main/java/dev/ronova/coremod/root/api/RecoveryWorkDescriptor.java       缺陷 2
src/test/java/dev/ronova/coremod/observation/TaskExecutionGraphSelfTest.java 缺陷 1、5、7
src/test/java/dev/ronova/coremod/persistence/RecoveryWorkScenarios.java     缺陷 3
```

**没有其他文件被修改。** 无临时诊断残留（`DIAG` 命中 0）。`validation/` 未动（`acceptance-matrix.json` mtime 仍为 `09/11/2026 09:56:44`，84,707 B）。

**注意：** `ronova-source-...-r66.zip` **未改动**；五个文档检查器以它为 `--source-root`、**不读工作区源码**，故文档验证记录不受本轮影响。

---

## 7. 边界

- **L0 三条铁律与 UX1 不变。** 未修改依赖/Mod/Forge 文件、未针对、未删除对方文件、未启动外部进程。
- **未修改** 任何文档、`validation/` 台账、构建配置或资源（`build.gradle` 未动）。
- **未新增** `SubjectKey` / Root / D*·INV*·A*·R*·ID*·T* 编号。
- **本文档不是验收结论。** 七处修复只使既有自测转绿；**不产生任何 B/X 通过、不改 28 PARTIAL / 40 NOT_RUN、不关联 GAP-MAP-07、不改变 8 表面 S0/S1 现状**。
- **未做：** 未启动游戏、未做双客户端验证、未做专用服验收、未跑 `validation/` 中任何场景。
- **未复核：** 修复只保证既有断言通过；**不证明这些语义在真实运行中正确**。
