# DeepSeek 本轮改动

状态：**未编译、未实机、未运行任何测试。** 本机命令执行器不可用（详见末尾"环境"）。
所有结论来自源码阅读与补丁本身的静态核查。**未更改任何 PASS 状态，未覆盖任何历史失败。**

## 目标和实际完成范围

- 对应四块中的哪些要求：**第三方任务与资源结算**（块 3）的两个纵向模块——
  (a)「排队且从未开始的任务」与「真实退出」分开结算；(b) 后继子树的独立闭合证明，
      覆盖取消分支与正常退出分支。
  共享资源隔离保持：`RESOURCES_RELEASED` 未接线，取消/退出都不充抵资源结算。
- 哪些仅实现：本轮全部改动。
- 哪些已验证：**无**。所有新增断言尚未执行过。
- 哪些仍未完成：共享队列引用与全部资源结算、停服结算、委托/跨队列后继；
  块 1（阶段故障/进程退出）、块 2（静态保存与未加载来源）、块 4（Completion 条件与稳定窗口）。

## 文件变化

| 文件 | 新增/修改 | 原问题 | 改动后的行为 |
|---|---|---|---|
| `src/main/java/dev/ronova/coremod/observation/TaskExecutionGraph.java` | 修改 | `View` 只能表达 `executionsExited()`（要求 `entries>0`），无法表达「已观察提交但从未进入执行」；`descendants()` 只给快照，后继闭合无独立判定 | 新增 `QueueState` 枚举、`View.queueState()` / `View.queuedNeverEntered()`、`successorsClosed(UUID,int)`；`executionsExited()` 语义未改 |
| `src/main/java/dev/ronova/coremod/observation/RecoveryTaskDisposition.java` | 修改 | 取消被接受后只写 `CANCEL_ACCEPTED`；从未开始的任务无结算事实；`SUCCESSORS_CLOSED` 全仓从未被写入过；后继闭合此前只可能在取消分支成立 | 取消返回后新鲜重读队列状态；仅「取消已接受 + 从未进入执行」时写 `NOT_EXECUTED`；抽出 `recordSuccessorsClosed` helper，**取消分支与正常退出分支共用**；计数器改用 `Map.ofEntries`，新增 `queued_not_executed` 与 `successors_closed` |
| `src/test/java/dev/ronova/coremod/observation/TaskExecutionGraphSelfTest.java` | 修改 | 未覆盖队列/退出分离与后继闭合 | 新增断言：排队未进入、进入后退出、周期重入不抹掉历史 entries、仅作父节点者不得算已提交、覆盖丢失撤销队列读数；后继闭合的叶子/运行中/已退出/排队/未观察/未知 registration/环 |
| `src/test/java/dev/ronova/coremod/persistence/RecoveryWorkScenarios.java` | 修改 | 未覆盖 `NOT_EXECUTED` / `SUCCESSORS_CLOSED` 的持久语义与类别限制 | 新增断言：两事实均可在 `TASK_CANCEL` 意图上持久化、与 `CANCEL_ACCEPTED` 共存、不冒领 `EXECUTION_EXITED` 或 `RESOURCES_RELEASED`、重复记录幂等、`STORAGE_COMMIT` 意图同时拒绝两者 |

### 后继结算的覆盖范围

`SUCCESSORS_CLOSED` 现由两条分支共用同一判据写入：

1. **取消分支**：取消被接受且任务从未进入执行（`NOT_EXECUTED`）时评估。
2. **正常退出分支**：`EXECUTION_EXITED` 已写入时评估（`RecoveryTaskDisposition` 维护轮的既有 `work!=null` 路径）。

判据本身只有一个实现（`TaskExecutionGraph.successorsClosed`），不存在两套语义。
仅当全部条件成立才为真：registration 已在本图观察过；子树遍历完整；`losses==0`；
每个后代要么 `executionsExited()`，要么 `queuedNeverEntered()`；
且已执行过的后代其全部已观察调度器完整（无缺失调度器）。

以下任一情况保持未决：后代从未被观察提交、后代正在执行、覆盖丢失、遍历被上限截断、registration 未知。

`NOT_EXECUTED` 与 `SUCCESSORS_CLOSED` 都不充抵 `EXECUTION_EXITED` 或 `RESOURCES_RELEASED`。
`losses!=0` 是保守的全局判据：一旦本图任何位置丢失证据，本轮不再写后继闭合（宁缺勿假）。

`RESOURCES_RELEASED` **本轮完全未接线**。取消或退出都**不得**充抵资源结算——
按规范，共享队列引用、线程池与其他主体资源的结算需要独立证明，不能由任务自身退出推断。
这一点是有意保留的未决，不是遗漏。

## 关键约束

- 持久 ACK、资格撤销、最终对象绑定如何保持：
  未改动任何 Root/EffectJournal 写入路径、fence、lease 或 ACK 顺序。
  `NOT_EXECUTED` 仍经既有 `LocalDominionStore.observeRecoveryWork` →
  `ShadowDurableRootStore.observeRecoveryWork` 写入，仍受
  `RecoveryWorkDescriptor.allows(fact)` 类别门与 `enteredEffects` 一次性许可约束。
- 任务退出/后继/资源如何分别证明：
  退出只有在 `entries>0 && activeExecutions==0 && entries==exits` 时才写 `EXECUTION_EXITED`（未改）；
  队列结清是新事实 `NOT_EXECUTED`；后继闭合是新事实 `SUCCESSORS_CLOSED`，由
  `successorsClosed()` 的完整子树判据单独提供证明。三者互不代替，
  `RESOURCES_RELEASED` 本轮完全未触碰，仍为独立未决。
- 保存保留、原意图重启和未知效果如何处理：
  本轮不触碰保存链。`EFFECT_UNKNOWN`、`OPAQUE` 生命周期与旧语义原样保留。
- 持久版本迁移和旧 hash/ONCE 影响：
  **无版本变更**。未新增 schema、未新增 operation、未改 slot、未改 canonical 描述，
  因此根镜像与 effect journal 的编码不受影响。`NOT_EXECUTED` 是 `RecoveryWorkResult.Fact`
  的既有位（4096），本轮只是首次在生产路径写入它。

## 依赖的正确性前提（已核实）

1. `FutureTask.cancel(FALSE)` 排除后续进入：`native/src/ronova_native.cpp:1021`
   以 `CallNonvirtualBooleanMethod(task, FutureTask, cancel, JNI_FALSE)` 调用基类
   `FutureTask.cancel(Z)`，注释明确 `false never interrupts the runner or any shared pool thread`。
2. 因此「取消已接受」之后在服务器线程上重新读取到 `entries==0 && activeExecutions==0`，
   即可证明该包装器从未运行，而不是「暂时还没运行」。
3. 重读发生在取消返回之后，不使用循环开头的快照——否则可能把取消前已进入执行的任务误判为未执行。
4. `TaskEventRun.mergeInto` 只提交成对完成数与开放尾部，不会制造 `entries>0` 的假象，
   因此批量压缩路径不会被误判成 `NOT_EXECUTED`。

## 验证

| 命令 | 退出码 | 日志文件 | 实际验证范围 |
|---|---|---|---|
| 未执行 | — | — | — |

- JAR 路径与完整 SHA-256：**未构建**。
- 实机使用的 JAR/夹具 SHA-256、suite/round：**未运行**。
- 未执行的测试及原因：全部。本机无可用命令执行器。
  计划执行：`runTaskExecutionGraphSelfTest`、`runDurableRootStoreSelfTest`
  （后者加载 `RecoveryWorkScenarios`），再按 `03-本机验证.md` 跑隔离专用服新 suite。
- 失败、修复解释及保留的原日志：本轮无新失败。历史失败原样保留，
  特别是 `validation/7.17/phase1-task-disposition/round-4.json` 及其同轮日志——
  该轮 graph 为 `state=EXECUTING, entries=1, activeExecutions=1`，属于**运行中**任务，
  不在本轮改动的语义范围内，本轮结果不能解释也不能覆盖它。

## 本轮修正的既有判断

上一轮只读核对的结论有两处经指出并复核后确认**错误**，在此更正：

1. **「进程退出/停服期间的过期事务无结算点」——错误。**
   `native/src/storage_transaction.hpp:55-62` 已有 `reap_expired`，在
   `prepareStorageTransaction0` 入口第 122 行调用：取过期槽、在注册表锁外回滚并释放，
   且不涉及 Root permit。槽位过期时间在 :136 设定为 5000 ms。
2. **「并发上限与 5 秒期限是缺陷」——错误。**
   `RecoverySourceTracker.java:1728` 的并发准备上限 2 与
   `StorageTransactionTask.java:65` 的读回期限是既有的有界设计，
   超时正确落到 `UNKNOWN`/`READBACK_CHANGED`，不应删除。已从缺口清单移除。

另更正口径：进度文档**不得**覆盖规范合同。先前的
「修正总纲 `:1585` 状态行」提议作废——规范文本保持原样，
只更新 `docs/CURRENT-PROGRESS.md` 与 `docs/PHASE1-IMPLEMENTATION-STATE.md` 的
当前状态叙述，且两者都必须继续声明第一阶段未完成。

## 待审查与未决

- 相关并发/生命周期风险：
  `graph.view(registration)` 在取消返回后重读，与执行线程的 `entered` 存在天然竞态；
  本轮依赖 `cancel(FALSE)` 的排除语义来保证正确性（前提 1/2）。
  该前提的实机验证仍需在专用服上观察：取消被接受的任务不得再出现 `entries>0`。
- 尚未关闭的验收项：`X65`（NOT_RUN）、`X63`/`X64`（PARTIAL）以及
  `RECOVERY_TASKS` 完成条件的真实证据仍未闭合。
  本轮新增的 `NOT_EXECUTED` 与 `SUCCESSORS_CLOSED` 只是该完成条件可用的证据面，
  **不构成** 完成证据；Completion 的独立版本化语义尚未接入。
- 本轮明确的剩余缺口（未接线，非遗漏）：
  `RESOURCES_RELEASED` 无生产写入路径——共享队列引用、线程池与其他主体资源的结算
  需要独立证明，不能由任务自身退出或取消推断；
  委托/跨队列后继（`CompletableFuture` 同步后继无独立边界）仍未覆盖。
- 本轮明确未宣称的能力：未宣称任务链已结算、未宣称资源已释放、未宣称后继已闭合到生产口径、
  未宣称任何验收项转 PASS、未宣称第一阶段有任何进展性完成。

## 环境

命令执行本会话三次均失败：`workspace-write` 报
`no sandbox backend is usable on this host`（`windows-acl-run: CreateProcessAsUserW failed (Win32 2)`）；
升到 `danger-full-access` 后报
`spawn \Users\Scorn\AppData\Local\Microsoft\WindowsApps\pwsh.exe ENOENT`（路径缺盘符）。
故 `gradlew`、`python`、隔离专用服、SHA-256 一律未运行。
