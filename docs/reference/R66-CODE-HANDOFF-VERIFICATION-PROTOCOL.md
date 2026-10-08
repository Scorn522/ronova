# R66 代码交接与实机验证协议（v1）

**分工**：外部智能体写代码 → 本机（DeepSeek 侧）实机验证 → 回执。

本机有可用工具链，**完整验证一轮约 1 分 30 秒**。外部侧无法跑完整构建，
但**可以**单独编译并运行 standalone 自测（见 §5），请先自测再交付。

---

## 1. 基线：请对这个包打补丁，不要对别的

| 项 | 值 |
|---|---|
| 文件 | `ronova-source-0.2.0-alpha.7.17-r66-code1.zip` |
| 条目 | 618 |
| 大小 | 3,770,422 B |
| SHA-256 | `CF68011245EC3555394065DD2482CCACEA0C5D67C04D57A75719CB78E7E82743` |

它与**原始交付包**的偏离面恰为 5 个文件：

```
src/main/java/dev/ronova/coremod/observation/RecoveryTaskDisposition.java   ← 本轮修复目标之一
src/main/java/dev/ronova/coremod/observation/TaskExecutionGraph.java        ← 本轮修复目标之一
src/main/java/dev/ronova/coremod/root/api/RecoveryWorkDescriptor.java
src/test/java/dev/ronova/coremod/observation/TaskExecutionGraphSelfTest.java ← 请在此加用例
src/test/java/dev/ronova/coremod/persistence/RecoveryWorkScenarios.java
```

⚠️ **`validation/7.17/codex-review-deepseek-20260911-1/snapshot/` 是 09-11 18:26 的旧状态，
不要以它为基线。** 前两轮审查因此重复推导了已修好的缺陷。

---

## 2. 本轮要修的三件事（详见审查件 §8）

审查件：`R66-RECOVERY-TASK-CLOSURE-CHANGE-REVIEW.md`（§7 = D-09，§8 = 三点确认与修法形状）。

| # | 位置 | 问题 | 类型 |
|---|---|---|---|
| 1 | `TaskEventRun.append` + `TaskExecutionGraph:122` | 批次 `进入→退出→提交` 合成 `completed=1,submissions=1`，未执行提交漏计 ⇒ 误判已退出 | 欠计 |
| 2 | `TaskExecutionGraph:115` | 单独 `submitted()` 后，批次 `(进入,退出)` 不会抵消先前的等待提交 ⇒ `unentered` 恒为 1，**永久**判为未开始 | **过计（我引入的回归）** |
| 3 | `RecoveryTaskDisposition:59` | 跳过条件未含 `closurePending`，而重试键是**父**节点 `View.revision`（`TaskExecutionGraph:226` 传 `node.revision`）⇒ 闩锁永不被清偿 | 调度 |

**第 1 与第 2 是同一根因的两侧**，`unenteredSubmissions` 由三条互不知情的路径维护
（单独 `submitted()` 递增 / `entered()` 递减 / 合并按 `submissions-completed` 估算）。
**只补一侧必然在另一侧开洞 —— 请一次改完，不要分两次提交。**

裁定要点（§8.3）：**批内退出之前的进入可抵消先前的等待提交；退出之后的提交不可。**
分界是次序。且 `entered` 与批次聚合的**施加次序不可交换**（§7.3）。

---

## 3. 必须带的测试（验收条件，缺一不算完成）

加在**已有的** `src/test/java/dev/ronova/coremod/observation/TaskExecutionGraphSelfTest.java` 里
（它已在 `build.gradle:211` 注册，**无需改构建文件**）：

| # | 场景 | 期望 |
|---|---|---|
| T1 | 批内 `进入 → 退出 → 提交`（`completed=1,submissions=1`） | `unenteredSubmissions>=1`，`executionsExited()==false` |
| T2 | 单独 `submitted()` → 批次 `(进入,退出)` | `unenteredSubmissions==0`，`executionsExited()==true` |
| T3 | 对照组：独立 `提交 → 进入 → 退出` | `executionsExited()==true`（不得因修 T1/T2 而破坏） |
| T4 | 父已退出、子仍在执行 → 子退出后要求记 `SUCCESSORS_CLOSED` | 父 `View.revision` 不变的前提下，补记仍能发生（覆盖 `RecoveryTaskDisposition:59`） |
| T5 | `submitted(task, null, null)`（未知调度器） | `successorsClosed()` 为 `false`（保持既有语义） |

**T1/T2/T3 必须同时绿。** 只让 T1 绿而 T2 红的补丁等于把洞挪了位置。

---

## 4. 交付形式

**首选**：一个 zip，含**完整的修改后文件**（保持包内路径结构）。
理由：手工套 diff 易错，完整文件可直接覆盖并复算 diff。

```
ronova-source-0.2.0-alpha.7.17-r66-code1-patch1.zip
  src/main/java/...        ← 完整文件
  src/test/java/...        ← 完整文件
  PATCH-NOTES.md           ← 改了什么、为什么、自测结果
```

**`PATCH-NOTES.md` 必写**：①动了哪几个文件 ②不变量依据 ③**你自己不确定的点**
（这一项最有价值 —— 我上一轮标的"闩锁重试"不确定点，正是被审查者打中的那处）
④你已跑过什么、结果如何。

**不要动**：`validation/**`、`native/**`、`build.gradle`、`docs/**`、
`TaskExecutionGraphSelfTest` 以外的测试文件（如确需改，请在 NOTES 里单独说明理由）。
若你认为必须改 `build.gradle`，**先只给方案，不要直接改**。

---

## 5. 你那边能做的自测（请先做，省回合）

你有 JDK，standalone 自测是 `main()` 入口、不依赖 Minecraft：

```
javac -d <out> -cp <main-classes> src/test/java/dev/ronova/coremod/observation/TaskExecutionGraphSelfTest.java
java -cp <out>;<main-classes> dev.ronova.coremod.observation.TaskExecutionGraphSelfTest
```

**本机工具链（已就位，不要再索要）**：

| 项 | 路径 / 值 |
|---|---|
| JDK 17 | `D:\桌面\MOD\.build-tools\jdk17\jdk-17.0.15+6` |
| Gradle 缓存 | `D:\桌面\MOD\.gradle-cache`（约 1.2 GB，已热） |
| Zig 0.13.0 | `D:\桌面\MOD\.build-tools\zig-windows-x86_64-0.13.0\zig.exe` |
| JDK headers | `D:\桌面\MOD\.build-tools\jdk17-headers\jvmti.h` |

---

## 6. 我会跑什么（验证清单）

```
JAVA_HOME=D:\桌面\MOD\.build-tools\jdk17\jdk-17.0.15+6
GRADLE_USER_HOME=D:\桌面\MOD\.gradle-cache
gradlew compileJava compileTestJava test selfTest --console=plain
```

| 关卡 | 通过标准 |
|---|---|
| G1 | `compileJava` + `compileTestJava` 成功 |
| G2 | `test` 成功 |
| G3 | `selfTest` 全部 `PASS`，含新增的 T1–T5 |
| G4 | `BUILD SUCCESSFUL`，112 个任务 |
| G5 | 产物 JAR 重建成功（`reobfJar` 封印链会触发，这是预期的） |
| G6 | 相对 code1 基线的偏离面 = 你声明的文件集，**无未声明改动** |
| G7 | 我独立复核 diff：新增授权是否落在 policy/Root 层、"观察不授权"免责声明是否原样保留 |

**另**：我会对 T1–T5 做**反向验证** —— 把你的补丁撤掉后这些用例必须失败。
不会失败的测试不算证据（这正是 D-08 藏了两轮的原因）。

---

## 7. 我的验证能证明什么、不能证明什么

**能证明**：编译通过；T1–T5 及其它既有断言在本机通过；偏离面与声明一致；
封印产物可重建。

**不能证明**：未启动游戏、未跑隔离专用服；`cancel(FALSE)` 的排除语义仍未实机观测；
"失败测试在撤补丁后确实失败"只覆盖我实际跑的那几个用例；
**我无法验证意图**——我只能验证"你声称的行为是否有测试证明"。

我不会回填任何 PASS、不改 `validation/`、不覆盖历史失败。
