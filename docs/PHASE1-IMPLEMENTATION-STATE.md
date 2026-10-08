# 第一阶段实施接续

更新：2026-09-11。第一阶段仍未完成，不使用子代理。

## 新增自动接线

- 任务初次审批保留新鲜见证要求；原 ACK 后使用准确的本次 observer 计划继续执行，复验任务/资格/加载器/来源及代码证明。
  Native 已调用后消费许可，原回执未 ACK 时也不允许重新进入；使用既有 enteredEffects，不新增持久数据库。
  无新任务事件、无待写事实时停止重复 Root 查询，其他等待最多每 20 tick 重查，图版本变化立即唤醒。
  [任务接续报告](../validation/7.17/phase1-task-continuation-report.md)和同 JAR 普通/周期任务八秒回归均已记录。
- 存储可从原内存处置的持久 WRITE_OBSERVED 接续，不刷新复制见证时间戳。
  Root 要求准确的本次运行 observer 计划、原登记/位置/版本/深度、现资格和当前绑定；
  原计划只由有界弱引用缓存保留，旧磁盘描述或复制出来的同内容计划不能取得新写权限。
  审批、持久 ACK 与最终 fence 分别复验；未知内存结果仍拒绝执行。
  `build-phase1-memory-continuation-3.log` 的 Root 回归通过，长等待/重启实机结果以自动链汇总为准。
- `RecoveryMutationIntent.acknowledgedWrite` 和 Root 原记录查询区分持久确认、回执未确认及带未知位的写入。
  已验证的移除保留来源和任务/保存义务，不再统一当作来源失联；原 Root 意图继续承载未结债务。
- `advanceDisposedStorage` 已从维护线程自动推进内存 ACK、图像准备、存储 ACK、事务提交、读回和该存储工作资源回执。
  初版自动链和同 JAR 重启查询通过；不同对象重插使旧状态失效，重插对象保留。
- 最终直接记录提交使用 `AtomicSnapshotRecord.guardSlot` 保持准确 ConcurrentHashMap 键，按容器锁→tracker→Root 顺序进入。
  `drainStorageCommits` 在 tracker 监视器外执行，回执写盘和独立读回在键锁外；共享预算轮转给予提交首轮机会。
- 存储执行复用 Root 的 `effectClaims/enteredEffects`，Native 最终入口消耗准确的一次性 claim；
  外部 Root fence、生命周期入口借用和重复进入被拒绝。异常返回先保留 UNKNOWN，不能重新变成 READY 重放。
- 任务图保留全部已观察调度器，并新增有界后继图快照；循环、截断、缺失调度器保持不完整，不把观察到退出当作全部资源闭合。

[自动链证据汇总](../validation/7.17/phase1-automatic-recovery-evidence.json)。
`phase1-memory-budget-phased` 第二轮长等待接续、第三轮原意图重启查询由同一 JAR 通过，
SHA-256 为 `8a28d6eb60e166ef30318e5ea6d3fb33c37ea2886106463fa343a3838f69355a`。
[本轮实现、回归及失败解释](../validation/7.17/phase1-memory-budget-report.md)。
`phase1-automatic-storage-gated` 第二轮自动处置和第三轮重启查询已由同一 JAR 通过。
SHA-256 为 `0221a564c02d734c97aaa011c0361518b91fe3474a2e4f2045e53e3229813856`。
本地相关检查：`build-phase1-automatic-storage.log`、`build-phase1-task-relations.log`、
`build-phase1-storage-entry-claim.log`、`build-phase1-gated-automatic-storage.log`。
实机范围以汇总中的各个哈希/轮次为准，尚不能拼成整个阶段通过。

## 当前生产接线

1. `SavedDataEncodedWrite` 从实际 FileOutputStream 的 FileDescriptor 取得保存前/关闭前身份，
   与真实压缩输出摘要、NBT 实际写入引用和保存代次关联；关闭后清除 holder/image/遍历引用。
2. `StorageImageTask` 必须同时匹配实际保存身份和摘要；同内容文件替换不能沿用候选。
3. `StoragePreparationResult` 驱动 ACK、不可由外部构造的 `StorageCommitRequest`、
   `StorageTransactionTask` 及短期 Root 最终许可。后台阶段只持有不可变输入，不访问世界。
4. Native `storage_transaction.hpp` 动态加载 TxF，CNG 验证基线和候选；私有事务写入、截断、
   刷新、事务内读回和身份复核后关闭文件句柄。最终提交 token 同时绑定准确请求和许可。
   限制两个 Native 事务；新准备会回滚清理已过期的旧 token，不能用超时假定完成。
5. 提交和独立读回分别记入原 Root 工作事实，保留其他记录/未知字段。未增加第二份权威数据库。
6. `queryStorageIntent` 从原意图和已加载标准 DimensionDataStorage 重新解析文件位置，
   复核缓存/holder、适配器版本、File ID 和图像；查询自行登记资源核算，不依赖先运行准备流程。
   查询不改原 attempt、不发放写权限、不重放操作，也不等于整链完成。

## 持久兼容

- Root 最大版本 14、EffectJournal 最大版本 5；描述 v4 含 `RecoveryStorageGeneration`：
  后端 `windows-ntfs-txf/v1`、保存登记 UUID 和代次。
- 同代原 key/attempt 保持；新保存代次使用独立 semantic slot。旧 v1-v3 描述不重写。
- 缺少新执行依据的旧工作可查询，不能经生产交付句柄取得新 Native 写许可。
- 旧 command/hash/ONCE 和未知效果保持原义；未来版本和伪降级拒绝写入。

## 已验证范围

[汇总证据](../validation/7.17/phase1-native-storage-commit-evidence.json)保存所有报告和失败原因。

| 范围 | 证据 |
|---|---|
| 实际保存句柄身份及既有保存回归 | `phase1-saved-data-save-trace/round-24.json` |
| 首个生产事务提交、完整图像读回和保留检查 | `phase1-native-storage-commit/round-2.json` |
| 私有准备后 clear、回滚及原图像不变 | `phase1-native-storage-clear/round-2.json` |
| 竞争替换被阻止、竞争文件保留、原事务完成 | `phase1-native-storage-conflict/round-2.json` |
| 同内容不同文件身份拒绝 | `phase1-native-storage-replace-serial/round-2.json` |
| 保存代次描述版生产提交 | `phase1-storage-generation-commit/round-2.json` |
| 停服后原意图独立查询，无重放 | `phase1-storage-generation-commit/round-4.json` |
| 提交前后证明复验版，同 JAR 提交及重启读回 | `phase1-storage-postcommit-proof/round-2.json`、`round-3.json` |

本地针对性构建/测试：`build-phase1-writer-handle.log`、`build-phase1-native-transaction-link.log`、
`build-phase1-storage-generation.log`、`build-phase1-storage-reference-release.log`、
`build-phase1-original-storage-query.log`、`build-phase1-original-storage-query-accounting.log`。
这些是分版本子范围证据，尚未执行最后的整阶段完整构建和统一版本验收。

保留失败：Native 初次链接缺少 bcrypt 库，构建入口补齐后通过；
`phase1-native-storage-replace/round-2.json` 在来源见证/Native 观察尚不可用时超时，未进入替换步骤，
独立串行套件已实际执行替换并通过；`phase1-storage-generation-commit/round-3.json`
重启查询不可用，查询入口独立登记资源核算后第四轮通过。
旧 `phase1-task-disposition/round-4.json` 超时仍需定位，不能被本次存储结果覆盖。

## 下一项实现与尚未关闭的边界

- 提交返回后的资格/观察证明复验已加入：失败保留 EFFECT_UNKNOWN，不凭 Native 返回成功发完整事实。
  `build-phase1-storage-postcommit-proof.log` 构建及 `phase1-storage-postcommit-proof/round-2.json` 正例通过；
  同版本第三轮重启查询通过，完整的证明失效故障注入仍需覆盖。
- 已有直接记录→存储的生产自动正例，仍需将原意图枚举、任务/后继及所有资源接入同一自动链。
- 当前自动准备限额为压缩 64KiB、解码 256KiB、4096 节点，较大图像与预算调整后的接续尚需完善。
  不支持/失败代次停止重试并保留未决，不能把这个临时限额当作阶段最终支持范围。
- 已接入原内存意图的长等待接续；预算故障夹具在内存移除后停止复制，等待八秒再释放存储预算。
  实机结果/失败保存在自动链汇总；原始 seed 实际被 GC、超过有界活跃计划容量及更广泛并发仍未验证。
  重启旧记录仍仅可查询；不能从缺少本次 observer 核验的旧描述推测写权限。
- 原子快照槽的已验证清空也保留义务，代码已接线，自动快照文件实机案例尚需补齐。
  当前最终键锁仅覆盖准确 ConcurrentHashMap 记录，其他容器/Native 并发语义不能据此宣称通过。
- 直接任务/存储结果目前保留在原 EffectJournal；部分事实不能自动清偿其 OPAQUE_EFFECT 义务。
  需要恢复链独立契约和明确的结果/资源结算规则，不能修改旧生命周期未知效果。
- 完整静态持有者保存、未加载来源重查、提交回执丢失/进程终止后的安全重试仍未闭合。
  queryStorageIntent 是已加载 SavedData 的只读生产查询，不是通用保存恢复器。
- 尚未完整验证：Native 各步骤故障、提交期间资格撤销、过期 token 清理、停服期间后台尚未退出时的资源结算。
  关闭 worker 的请求与实际线程退出仍必须分别确认；不能停止共享池或假报资源释放。
- 第三方委托、同步 CF/ForkJoin 后继、排队未执行、周期重排和迟到回调的整链结算继续实现。
- Completion 仍保留 20 tick 稳定窗口和最大 10 tick 采样间隔；必须按持久操作而非实体弱引用维持观察，
  新来源/任务/重插/代次/证据变化立即撤销当前完成性，并覆盖容量、预算、压力和公平性。

## 接续约定

始终使用隔离的自有专用服与通用夹具；夹具可建立场景和制造故障，不能伪造最终来源、任务、提交或完成证据。
不运行原第三方样本，不替换原游戏和原存档。不使用子代理。
完成四块后集中完整构建、旧回归、故障/重启/压力和稳定完成验收，再交付最终候选 JAR、哈希与完整报告。

[此前完整实施历史](PHASE1-IMPLEMENTATION-STATE-HISTORY-2026-09-10.md)
