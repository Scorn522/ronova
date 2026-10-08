# Ronova 当前进度

更新：2026-09-11。**第一阶段未完成；完整 7.17、FULL L5 / L6 / L6E 未宣告完成。**

## 当前结论

生产存储链已经从“只有意图和许可”推进到真实 NTFS 事务提交、独立读回，
并在停服重启后通过原意图查询。此处仅证明列明的存储子范围，任务、资源和完成义务没有因此自动闭合。

新增生产自动链正例：原 Root 移除回执确认后，维护线程自动准备、提交、读回并登记该存储工作的资源事实，
夹具没有调用存储执行器。重插不同记录会使旧处置状态失效。
[自动链及失败记录](../validation/7.17/phase1-automatic-recovery-evidence.json)保存每个版本的具体证据。

| 四块工作 | 已有证据 | 必须继续 |
|---|---|---|
| Native 最终提交 | 实际保存流的文件句柄身份；来源和 ACK 驱动的私有事务准备、最终许可、提交及独立读回通过专用服；clear 回滚、竞争替换隔离及同内容不同文件身份拒绝通过 | 提交期间完整资格/代码竞争、阶段故障、进程退出、过期事务回收及统一版本回归 |
| 外部保存与重启 | 保存代次进入 Root 14 / EffectJournal 5；旧 hash/ONCE 保留及同代重试、跨代隔离通过本地测试；原生产意图重新定位已加载 SavedData 并独立读回通过新进程 | 静态持有者保存、未加载来源、丢失提交回执的查证和安全重试、生产自动枚举与完整语义恢复 |
| 第三方任务与资源 | Future/周期任务在 ACK 后延迟八秒、无新复制时仍能取消并观察真实退出；一次性许可和共享池隔离通过；存储 worker 退出后的局部资源释放通过 | 后继/通用委托、未开始任务、共享队列引用、完整资源和停服结算；旧超时最初阻塞点缺少证据 |
| 自动整链与稳定完成 | 真实内存移除到 SavedData 存储处置已有生产自动正例；已区分已验证移除与来源失联；重插使旧状态失效 | 补齐任务、全资源和静态存储分支；独立恢复链契约、压力公平性及真正稳定完成 |

## 可检查的证据

- [任务原意图接续、一次性许可及历史超时分析](../validation/7.17/phase1-task-continuation-report.md)。
- [任务接续检查点 JAR](../validation/7.17/phase1-checkpoints/ronova-7.17-task-continuation-checkpoint.jar)：
  SHA-256 `a36e855a8fe90cc3e9e576d68f8a1d3aacbc2ac32f59883e3c6805c3a6d46355`。
  [任务与存储同版本检查结果](../validation/7.17/phase1-task-continuation-evidence.json)分开列出各子范围。
- [生产提交、边界故障、迁移和重启证据汇总](../validation/7.17/phase1-native-storage-commit-evidence.json)。
- [预算等待八秒、停止新复制后的生产接续正例](../validation/7.17/phase1-memory-budget-phased/round-2.json)。
- [本轮同 JAR 原意图重启查询](../validation/7.17/phase1-memory-budget-phased/round-3.json)。
- [原内存意图接续说明及失败解释](../validation/7.17/phase1-memory-budget-report.md)。
- [本轮检查点 JAR](../validation/7.17/phase1-checkpoints/ronova-7.17-memory-budget-continuation-checkpoint.jar)：
  SHA-256 `8a28d6eb60e166ef30318e5ea6d3fb33c37ea2886106463fa343a3838f69355a`。
- [此前自动内存／存储处置正例](../validation/7.17/phase1-automatic-storage-gated/round-2.json)。
- [同版本原意图重启读回正例](../validation/7.17/phase1-automatic-storage-gated/round-3.json)。
- [自动处置与重启检查点 JAR](../validation/7.17/phase1-checkpoints/ronova-7.17-gated-automatic-storage-checkpoint.jar)：
  SHA-256 `0221a564c02d734c97aaa011c0361518b91fe3474a2e4f2045e53e3229813856`。

检查点不是阶段最终候选。各报告包含各自产物和夹具哈希，不能把不同版本的子范围结果拼成某一个 JAR 的完整通过。

首个提交后端限定 Windows 本地 NTFS，并要求运行时 TxF 能力可用。
不支持的文件类型、存储语义、路径或能力保持未决，不回退普通覆盖，不操作任意第三方文件。

## 验收与下一步

[68 项验收台账](../validation/7.17/acceptance-matrix.json)：**28 PARTIAL / 40 NOT_RUN / 0 完整 PASS**。
本次补入真实存储证据，尚没有任何完整验收项满足全部出口。
已知恢复链处置不得消除既有死亡、掉落等 OPAQUE 未知效果。

提交后证明复验和内存/存储自动编排已有正例；原内存意图的长等待接续已接线，实机结果见自动链汇总。
下一步仍须补齐第三方任务/资源、静态保存、容量与稳定完成。
保留全部失败日志；重启查询首轮资源核算入口遗漏已修复，替换用例首轮未取得来源见证的超时不算替换负例通过。

## 接续资料

- [当前实施接续](PHASE1-IMPLEMENTATION-STATE.md)。
- [此前完整进度与研究加强项历史](CURRENT-PROGRESS-HISTORY-2026-09-10.md)。
- [此前实施、失败与约定历史](PHASE1-IMPLEMENTATION-STATE-HISTORY-2026-09-10.md)。

继续作为一个阶段实施，不使用子代理；原游戏、原存档和原第三方 Mod 样本不运行或替换。
只在实现、失败原因或验收范围变化后重复相关验证。任一必要路径缺失，第一阶段保持未完成。
