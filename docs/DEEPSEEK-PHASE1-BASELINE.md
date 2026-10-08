# DeepSeek 第一阶段实施基线（只读侦察结论）

建立：2026-09-11。用途：环境恢复后直接开工的接续清单。
状态：**本文件只记录阅读结论，不含任何实现改动；第一阶段仍未完成。**
本文件是只读侦察记录；实施进展以 `DEEPSEEK-CHANGELOG.md` 为准，两者冲突时后者优先。

## 0.1 实施进展

- 块 3 第一个纵向模块（排队任务与真实退出分开结算）已实施，见 `DEEPSEEK-CHANGELOG.md`。
  状态：**未编译、未实机、未运行测试**。第 3 节的缺口 3 已由此轮改动处理，其余缺口仍未闭合。

## 0. 本会话能力限制

命令执行器不可用：所有 `pwsh` 调用返回
`sandbox mode "workspace-write" is requested but no sandbox backend is usable on this host`
（`windows-acl-run: CreateProcessAsUserW failed (Win32 2)`）。
升级到 `danger-full-access` 后错误变为 `spawn \Users\Scorn\AppData\Local\Microsoft\WindowsApps\pwsh.exe ENOENT`
（路径缺盘符）。因此**未编译、未运行测试、未启动专用服、未计算任何 SHA-256**。
源码与文档未作任何修改。环境修复后须重新验证本文件全部结论。

## 1. 阅读基线

交接包实际位于 `D:\桌面\MOD\DeepSeek交接-7.17-20260911\`，不在工程目录内。
已按 `02-范围与代码导航.md` 顺序读完 1–8 项，并沿实际调用关系读了：
`RuntimeCoordinator`、`RecoverySourceTracker`、`RecoveryObservationScope`、`RecoveryTaskDisposition`、
`TaskExecutionGraph`、`JdkTaskBoundaryContract`、`BootstrapTaskHooks`、`StorageTransactionTask`、
`CompletionTracker`、`TerminalCompletionMonitor`、`TerminalOperationPlan`、`LocalDominionStore`、
`ShadowDurableRootStore`、`EffectJournal`、`native/src/storage_transaction.hpp`、
`build.gradle`、`gradle.properties`。

独立复核：`validation/7.17/acceptance-matrix.json` 共 68 例，
**28 PARTIAL / 40 NOT_RUN / 0 PASS**，无 PASS 无其他状态，与当前文档一致。
四块直接相关：X63 PARTIAL、X64 PARTIAL、X65 NOT_RUN、X66 PARTIAL、X67 PARTIAL、X68 PARTIAL。

## 2. 已实现（有生产调用点）

| 块 | 生产调用点 |
|---|---|
| Native 最终提交 | `RuntimeCoordinator.java:53,63` → `RecoverySourceTracker.drainStorageCommits` → `StoragePreparationResult.commitTransaction`；`NativeBridge.prepareStorageTransaction0/finishStorageTransaction0` |
| 保存与重启 | `RecoverySourceTracker.advanceDisposedStorage`（:1584-1648）自动推进内存 ACK→图像→存储 ACK→事务→读回→资源事实；`queryStorageIntent`（:1752+）独立只读查询 |
| 任务与资源 | `RecoveryTaskDisposition.maintain`（经 `RecoverySourceTracker.maintain` :1421 调用）；`recoveryTaskContinuationCurrent`/`acquireTaskContinuation`/`retireTaskContinuation` 一次性许可 |
| 整链稳定 | `TerminalCompletionMonitor.sample`（`RuntimeCoordinator.java:119`）；`CompletionTracker.STABLE_TICKS=20`、`MAX_SAMPLE_GAP=10`（:9） |
| 权限交集 | `policy/RecoveryChainPolicy.java` + `ATTACK_RECOVERY_SOURCE/ATTACK_STATE_SOURCE/ATTACK_SAVE_RECONSTRUCTION/ATTACK_TARGET_CLASS_LINEAGE` 均在 main 源码 |
| 持久兼容 | `ShadowDurableRootStore.SCHEMA=14`（:59）；`EffectJournal.decode` 上限 5（:161）；描述 v4 用 `RecoveryStorageGeneration.WINDOWS_NTFS_TXF` |

## 3. 缺生产接线（逐条含源码位置）

1. **静态 manager 来源无法进入保存链。**
   静态来源构造器 `RecoverySourceTracker.java:452` 显式 `saveTrace = null`；
   `advanceDisposedStorage` 在 :1604 要求 `source.encoding != null`，
   而 `encoding` 只能由 `saveTrace` 派生 → 静态来源永远停在 `WAITING_OBSERVED_SAVE`。
   `queryStorageIntent` 在 :1763 硬性要求 `RecoverySnapshotPlan.StorageScope.LOADED_SAVED_DATA`。
   对应未关闭项：完整静态持有者保存、未加载来源重查。

2. **Completion 九项中 6 项无任何生产生产者。**
   全仓（main+test）只有 3 处 `Condition.` 写入，都在 `TerminalCompletionMonitor`：
   `BODY_DISABLED`(:89)、`SAVE_QUALIFICATION`(:97)、`NO_PENDING_WORK`(:105)。
   `WORLD_RECORDS`、`OLD_EPOCH_REJECTED`、`RECOVERY_TASKS`、`REBUILD_ENTRYPOINTS`、
   `TRACKING`、`SERVER_PRESENCE` **0 处引用**（仅出现在 `CompletionTrackerSelfTest` 与 7.14a 夹具）。
   → 按自身逻辑当前不可能产生完整证书。这是诚实缺口，不是缺陷伪装。

3. **排队且从未开始的任务不会结清执行事实。**
   `TaskExecutionGraph.View.executionsExited()`（:16）要求 `entries>0`；
   提交但未进入的任务 `entries==0`，`RecoveryTaskDisposition.java:61` 永远不会写入
   `RecoveryWorkResult.Fact.EXECUTION_EXITED`。`submissions` 字段已存在（:12），可作结清依据。

4. ~~进程退出/停服期间的过期事务无结算点。~~ **本条已作废（原判断错误）。**
   `native/src/storage_transaction.hpp:55-62` 已实现 `reap_expired`，在
   `prepareStorageTransaction0` 入口第 122 行调用：取过期槽后在注册表锁外回滚并释放，
   不涉及 Root permit；槽位过期时间在 :136 设为 5000 ms。
   `StorageTransactionTask.close()`（:79-83）在 `!done()` 时直接返回是配合该回收的延迟关闭，
   `RecoverySourceTracker.reviewStoragePreparations`（:1800-1812）在后续维护轮复查直到
   `resourcesReleased()`。

5. ~~读回固定 5 秒内部期限~~ **与并发上限 2 一并从缺口清单移除。**
   `StorageTransactionTask.java:65` 的读回期限与
   `RecoverySourceTracker.java:1728` 的并发准备上限 2 是既有的有界设计；
   超时正确落 `UNKNOWN`/`READBACK_CHANGED`，不回落成功。**不应删除，也不应算作缺陷。**

6. **CompletableFuture 同步后继无独立边界。**
   `JdkTaskBoundaryContract.TARGETS`（:13-24）覆盖 `FutureTask`、`ScheduledFutureTask`、
   `ForkJoinTask.doExec/fork`、`ThreadPoolExecutor.execute/runWorker`、
   `ScheduledThreadPoolExecutor.delayedExecute/reExecutePeriodic`、`ForkJoinPool.externalSubmit`、
   `CallerRunsPolicy.rejectedExecution`；无 `CompletableFuture` 条目。
   同步 `thenApply` 类后继不经上述任何入口。

## 4. 文档与代码不一致（具体位置）

| 位置 | 问题 |
|---|---|
| `docs/plans/Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md:1585` | 该行写“尚未完成生产接线”。**规范文本不得被进度文档覆盖**，因此不改规范；只更新 `CURRENT-PROGRESS.md` / `PHASE1-IMPLEMENTATION-STATE.md` 的当前状态叙述，并注明规范日期 |
| `docs/plans/Ronova_7.17_Generic_Recovery_Chain_Implementation_Plan_V1.0_Internal_Root.md:3,21-37` | 状态日期 2026-09-10 及历史增量（“尚未构建/运行验证”）已被 `CURRENT-PROGRESS.md` / `PHASE1-IMPLEMENTATION-STATE.md`（2026-09-11，含自动链正例）覆盖 |
| `02-范围与代码导航.md:19-24` | “四块剩余工程”口径已被 `CURRENT-PROGRESS.md:14-19` 覆盖 |
| `02-范围与代码导航.md:32,35` | 源码入口表缺 `observation/RecoveryObservationScope.java`（`RecoverySourceTracker.java:555` 调 `installHook`，复制/任务观察实际安装点）与 `policy/RecoveryChainPolicy.java` |

已核对一致、无需改动：`validationFixtureJar` 确在 `build.gradle:154`；
`runDurableRootStoreSelfTest` 由 `build.gradle:325` 的 `run${simpleName}` 生成；
产物名 `ronova-<mod_version>.jar`（`build.gradle:11,9`）；`gradle.properties:15` 默认 r66 确为旧默认值。

## 5. 开工顺序（环境恢复后）

1. 先按 `03-本机验证.md` 跑一次未受影响的基线 selfTest，确认工具链与 JAR 可构建。
2. 块 3：排队未开始任务与后继结算（`TaskExecutionGraph` + `RecoveryTaskDisposition`），改动面最小。
3. 块 2：静态来源保存路径与未加载来源查询（`RecoverySourceTracker` 静态分支）。
4. 块 1：阶段故障、进程退出、过期 token 回收（`StorageTransactionTask`/`StorageImageTask`）。
5. 块 4：为缺项 Completion 条件接真实证据源 + 20 tick 稳定窗口验收。
6. 集中完整构建、旧回归、隔离专用服实机验收；再更新两个当前文档、验收台账与生成模板，
   填写 `DEEPSEEK-CHANGELOG.md`。

## 6. 硬约束（沿用，不得放宽）

不使用子代理；不运行原第三方样本；不替换原游戏/原 mods/原存档；
只在工作区内隔离专用服运行自有通用夹具；夹具可预置数据但不得提供来源归属、
任务退出、保存、读回或完成证据；不重写已有 Native TxF、保存身份捕获、
任务原意图接续与一次性许可基础设施；未知来源与 OPAQUE 效果保持未决；
不得提高阈值、删除失败用例或伪造 PASS。
