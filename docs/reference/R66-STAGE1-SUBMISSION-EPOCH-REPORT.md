# Stage 1 变更报告：提交/纪元计数（§8 第 1+2 项）

**范围**：修复"未执行提交"被误判的两个方向。**不含**第 3 项（`closurePending` 重试），那是 Stage 2。

## 根因

`unenteredSubmissions` 是**计数**，由三条互不知情的路径维护
（单独 `submitted()` 递增 / `entered()` 递减 / 批次合并按 `submissions-completed` 估算）。
总数在原理上无法区分这两种历史：

| 历史 | 应有的判定 |
|---|---|
| `提交 → 进入 → 退出` | 已结清 |
| `进入 → 退出 → 再次提交` | **未结清**（新纪元未进入） |

两者产出同一对计数，所以补计数只能堵住一侧、在另一侧开洞。

## 改动

| 文件 | 改动 |
|---|---|
| `TaskExecutionGraph.java` | `View.unenteredSubmissions`(long) → **`View.submissionPending`(boolean)**；`Node` 增加 `lastEventSubmitted`；`submitted/entered/exited` 各自更新该位；`observedCompletedRun` 改签名为 `(entries, completed, submissions, lastEventSubmitted)`；新增 `registerOpenExecution`（只登记未闭合 token，不重复计 entry、不重写该位） |
| `TaskEventRun.java` | 追踪 `lastEventSubmitted`；`mergeInto` 现在把**它覆盖的全部 entry**（已完成对 + 未闭合尾 token）一次交给图，**不需要图从总数反推** |
| `TaskExecutionGraphSelfTest.java` | 新增 3 组断言（尾随提交 / 先提交后合并 / 对照） |

**判据 = 两个独立读数的并集**（任一成立即视为未结清）：

1. **顺序**：本节点最近一次 提交/进入/退出 事件是**提交**。总数答不了这个问题。
2. **盈余**：累计提交数 > 累计进入数。顺序答不了"第二次提交先于进入"。

单用任一个都不够：第 1 条漏掉"提交、提交、进入、退出"；第 2 条漏掉
"进入数本就多于提交数"（裸 `entered`、合并批次补成对 entry）时的一次新提交。
并集比原有实现严格更强 —— 已验证**不会**把任何既有 `executionsExited()==true` 断言翻假。

## 刻意未改的语义

周期自提交批次 `进入,提交,退出` 重复（既有测试第 56–58 行构造、第 67 行断言）
**保持判为已结清**。这是既有测试写明的归一化语义，我按原样保留、未擅自改动。
若认为它应改为"未结清"，那是**独立决定**，请单独提出。

## 验证

| 项 | 结果 |
|---|---|
| `compileJava compileTestJava test selfTest` | **BUILD SUCCESSFUL in 1m 26s**，112 任务 |
| `TaskExecutionGraphSelfTest` | PASS（… queue-versus-exit separation, **submission ordering**, successor closure） |
| 产物 | `ronova-0.2.0-alpha.7.17-r66.jar`，5,054,188 B，SHA-256 `D75F5F64AFFDBAB21D97731C507966642A617572D0BDCA0BAD68EA15DEC9DB75` |

**反向验证（两次定向变更，均被捕获）**：

1. 去掉顺序位（`submissionPending` 只留盈余）→ 套件失败于既有 headroom 断言
   `a submission hid behind accumulated entry headroom`。证明顺序位承重。
2. 保留判据、但让批次不再上报"末事件是提交" → 失败于**本次新增**的断言
   `a submission trailing the last exit was reported as already exited`。
   证明新用例有真实证据力，而非恒真。

两次变更均已还原，上表是还原后的最终结果。

## 未做 / 未决

- **第 3 项未处理**：`RecoveryTaskDisposition:59` 的跳过条件仍不含 `closurePending`，
  重试键仍是父节点 `View.revision`（`TaskExecutionGraph:226` 传 `node.revision`）→ 闩锁仍可能永不重试。**Stage 2。**
- **D-08 仍无回归测试**（后继闭合的闩锁路径需要整套 store 夹具）。
- 未启动游戏、未跑隔离专用服、未改 `validation/`、未回填 PASS、未覆盖历史失败。
