# R66 · 第二批八方向核验（对源码现状的只读复核）

**性质：** 对**第二批八个加强方向**（S1–S4 / A1–A4）的**只读源码核验**。不是新提案，不新增方向/编号/批次。
**方向：** Ronova 侧 → 外部研究方（**发出**）
**方法：** 逐条对照代码，**先查是否已有**，再判缺口。**不以"名字没命中"推断"不存在"。**

**核验环境限制（须先声明）：** 本轮执行时 shell 不可用（`pwsh.exe` 无法启动，环境故障）。全部结论来自 `grep` / `read` 的**结构性检查**，**不含行数统计、不含编译、不含运行**。引用的行号均已逐条读取确认。

---

## 0. 结论摘要

**八条自评中六条准确，两条低估现状。** 这比第一批（九条里五处前提错误）明显更稳。

| # | 提案自评 | 核验判定 |
|---|---|---|
| **S1** 能力自证 / Challenge Probe | "部分有 proof，没有统一实测挑战" | ✅ **准确** |
| **S2** Mutation Write-Set 写集租约 | "没有完整统一形态" | ✅ **准确** |
| **S3** 独立结果见证器 | "存储链已有类似做法，可推广" | ✅ **准确，且范围更广** |
| **S4** 事件缺口传播 | "有预算、丢失计数思想，但没完全闭环" | ✅ **准确** |
| **A1** 运行时证据依赖图 | "老 L6E 有 Graph 概念，尚未真正运行化" | ✅ **准确** |
| **A2** Native ABI / Feature Negotiation | "现在只有 capability 协商的一部分" | ⚠️ **基本准确，但低估现状** |
| **A3** 通用 Shadow Enforcement | "DurableRoot 有 shadow，不是全机制通用" | ⚠️ **缺口准确，但低估已有结果模型** |
| **A4** 分能力熔断 / Degraded Mode | "有局部失证，没有统一降级状态机" | ❌ **低估最多** |

**需要修正自评的是 A4 与 A3；需要补现状的是 A2。其余五条自评可直接采用。**

---

## 1. 逐条核验

### S1 · 能力自证 / Challenge Probe —— 准确，原语齐备，缺统一层

**结论：** 提案的四个例子**逐个都能对上现有机制**，且**挑战-应答原语已经存在**。

**已存在：**

| 提案中的例子 | 现有机制 | 位置 |
|---|---|---|
| Retransform 能否真改 Ronova-owned 类 | `reassert_core_targets` 已做 retransform + 逐目标验证 | `ronova_native.cpp:629-692` |
| ClassFileLoadHook 事件是否真到达 | `record_semantic_proof` + `g_semantic_cycle_callbacks` 计数 | `:422`、`:336`、`:443`、`:679` |
| ThreadEnd 退出后是否真收到并清账 | `task_thread_end` 回调 + `native_task_exit` 成对释放 | `:106`、`:97`；绑定见 `:830`、`:921` |
| Native bridge receipt 一致性 | `NativeTransformBridge` | `coremod/agent/NativeTransformBridge.java` |

**挑战原语本身已存在** —— `coremod/integrity/CodeSealProbe.java`（10 行）：

```java
/** Stateless execution canary for the registered Native code-image guard. */
public static long evaluate(long challenge) {
    long mixed = challenge ^ 0x7170D15EA5E12345L;
    return ((mixed << 17) | (mixed >>> 47)) + 0x13579BDF2468ACE0L;
}
```

**它是一个挑战-应答金丝雀，只是用途是代码镜像守卫，不是能力获证。**

**缺口：** 统一层（探测→申请→实际获授→安装→**自发起无害挑战**→观察→签发）。

**建议采纳，并写清它是"提取统一层"而非"新建能力"** —— 四个样例机制都在，工作性质是把分散验证收敛成一条可签发的链。

---

### S2 · Mutation Write-Set 写集租约 —— 准确，是真缺口

**结论：** 自评准确。**没有声明式写集，也没有 declared-vs-actual 比对。**

**已存在：** `coremod/root/api/RecoveryMutationIntent.java`（82 行）——`Kind` 枚举（`STATIC_RECORD/STATIC_SNAPSHOT/LOADED_SNAPSHOT/LOADED_RECORD`）、`Observation` 位掩码、`Receipt`、`effectKey()`、`prepared()`。

**缺口：**

- 有 **write 是否落地** 的观察位，**没有"允许写什么"的声明**；
- 没有 `allowedSurfaces` / `allowedObjects` / `allowedOperations` 与执行后实际写入集的事后比对；
- 因此也**不可能**出现"未声明 mutation → fence 当前 plan"这条链路。

**这是本批里最实的一条新机制。** 且如提案所述：**不需要任何外部进程**，纯 plan 生成 + outlet 自检 + 事后比对。

---

### S3 · 独立结果见证器 —— 准确，而且比"存储链"更具体

**结论：** 自评"存储链已有类似做法"**正确**；但可推广的对象比提案点名的更精确——**已有现成模板**。

**已存在：** `RecoveryMutationIntent.Observation`（`:15-16`）：

```java
WRITE_OBSERVED(1), WRITE_UNVERIFIED(2), NOT_CURRENT(4), NOT_EXECUTED(8)
```

**这与提案要的四级阶梯 `REQUESTED / EXECUTED / OBSERVED / VERIFIED` 结构同构**，只是命名不同。而且提案最看重的那条纪律已经实现（`:74`）：

```java
/** An uncertain write is not cleared by a later observed bit. This covers memory only. */
```

落实在 `acknowledgedWrite`（`:75-81`）：

```java
return (mask & Observation.WRITE_OBSERVED.bit) != 0
    && (mask & Observation.WRITE_UNVERIFIED.bit) == 0;
```

**注意末句 "This covers memory only."** —— 这就是需要推广的边界。

**建议采纳，但把措辞从"设计四级结果模型"改为"把 `RecoveryMutationIntent.Observation` 提取为通用模式并推广"。** 已有实现即模板，工作量小于提案估计。

---

### S4 · 事件缺口传播 / Observation Gap Protocol —— 准确，是真缺口

**结论：** 自评准确。**只有单点丢弃计数，无序列连续性证据。**

**已存在：** `coremod/agent/BootstrapTaskHooks.java:78`

```java
public static long droppedEvents(){return DROPPED.get();}
```

**缺口（全树检索均无命中）：** `OBSERVATION_GAP`、`observationGap`、`SequenceGap`、`sourceGeneration`、`overflowGeneration`。

**提案的方向是对的：** 把"丢了 7 条"升级为"**事件连续性本身是证据**"，并支持 **scope-aware 撤销**（Native bind gap 只撤 JNI-native 相关 proof，不影响 entity lifecycle proof）。

**与已有设计衔接良好**：`g_semantic_outside_changes`（`ronova_native.cpp:335`）已在做同类判断——它记录"周期内被外部改动"的次数，并在 `:679` 用它决定租约是否成立。**S4 是把这个模式从"语义周期"推广到"事件流"。**

---

### A1 · 运行时证据依赖图 —— 准确，是真缺口

**结论：** 自评准确。**现有两个 Graph 都不是证据图。**

| 现有 | 性质 | 位置 |
|---|---|---|
| `TopologyGraph`（395 行） | **实体拓扑** —— 类注释原文："Per-server weak raw graph. **Terminal state is deliberately outside this type.**" | `coremod/observation/TopologyGraph.java:26-27` |
| `TaskExecutionGraph`（236 行） | **任务关系** | `coremod/observation/TaskExecutionGraph.java` |

**缺口：** 从 `JVMTI grant → ClassHook installed → CriticalClassWatch proof → protect.code_integrity → 执行器` 这样一条**可传播失效的 DAG**不存在。

**提案强调的 scope-aware revocation 是对的** —— 不能让 ClassLoader A 的证据失效把 ClassLoader B 一起打成 `UNAVAILABLE`。这与 C13 的 scope 化发布方向一致。

---

### A2 · Native ABI / Feature Negotiation —— 基本准确，但低估现状

**结论：** "只有 capability 协商的一部分"**不完全对** —— 已有一条**构建身份比对**。

**已存在：** `ronova_native.cpp:746`

```cpp
bool match=std::strcmp(text,CODE_SEAL_BUILD_ID)==0;env->ReleaseStringUTFChars(id,text);
```

Java 侧读 `/META-INF/ronova/code-seal-id.txt` 并传给 native 比对（`NativeBridge.java:48-50`）。`CODE_SEAL_BUILD_ID` 由 `native/build_code_seals.py:128` 生成。

**缺口（检索无命中）：** `NativeHandshake`、`abiVersion`、`featureMask`、`callbackTableVersion`、`structureSizes`。

**修正：** A2 不是"从零加协商"，而是"**在已有构建身份比对之上扩展 ABI / feature / structure-size 维度**"。尤其提案点出的场景是真实的：Java 认为 DLL 支持 `THREAD_END_V2` 而实际只有 `V1` 时，应当在握手处判为 `UNAVAILABLE / DEGRADED`，而不是在深处炸。

**且已有先例可循**：本项目的 `AddCapabilities` 教训（整组申请、无回读）正是同类问题的实例。

---

### A3 · 通用 Shadow Enforcement —— 缺口准确，但低估已有结果模型

**结论：** "只有 DurableRoot 有 shadow"**正确**；但**影子比对的结果模型已经比"有 shadow"成熟得多**。

**已存在：**

| 类 | 行数 | 性质 |
|---|---|---|
| `ShadowDurableRootStore` | 2,624 | 影子根 |
| `ShadowVerification` | 50 | **影子比对结果记录** |

`ShadowVerification`（`coremod/persistence/ShadowVerification.java`）带 **10 态判定词汇**：

```java
VERIFIED, MIRRORED, RECEIPT_MIRRORED, REALM_MISMATCH, ROLLBACK_REJECTED,
STATE_CONFLICT, RECEIPT_DRIFT, READ_ONLY, FAILPOINT, INVALID_INPUT
```

并有三个谓词（`:24-36`）：`accepted()` / `matches()` / `rejected()` —— **明确区分 `VERIFIED`（精确）与 `MIRRORED` / `RECEIPT_MIRRORED`（接受但非精确）**。

**修正：** A3 = "**把这个结果模型 + 比较流程推广到新机制**"，而不是"设计影子的判定方式"。判定词汇已经是现成的，而且已经处理了"接受但不完全一致"这个难点。

**提案的"DurableRoot 有 shadow，不是全机制通用"判断成立**，且提案指出的收益（让极限档更敢开：先 shadow 跑出误判率/覆盖率/未知率/成本，再 `MONITOR → PROTECT_RONOVA → STRICT_ALL`）是对的。

---

### A4 · 分能力熔断 / Degraded Mode —— **低估最多**

**结论：** "没有统一降级状态机"**与源码不符** —— **状态机已经存在，而且是 6 态；`DEGRADED` 也已存在。**

**已存在：** `coremod/policy/ActivationState.java`（11 行，6 值）

```java
INACTIVE, ACTIVE, PENDING_RESTART, UNAVAILABLE, REJECTED, FENCED
```

提案要的 5 个里 **3 个已存在**（`ACTIVE` / `FENCED` / `UNAVAILABLE`），且已有 **提案未提的 2 个**：`PENDING_RESTART`（暂不可用）、`REJECTED`（主动拒绝）。

**`DEGRADED` 也已存在**，只是在 `EvidenceState` 上 —— `coremod/policy/EvidenceState.java`（10 行，5 值）：

```java
UNOBSERVED, OBSERVED, VERIFIED, DEGRADED, FAILED
```

**真正的缺口只有两项：**

1. `DEGRADED` 是否需要在 `ActivationState` 上也有（当前只表达在 `EvidenceState`）
2. **`STALE` 没有命名状态** —— 当前"陈旧"是表达成 **ceiling 下降**（`evidenceCeilings(...)`）与 `RecoverySourceEvidence.fresh(now)` 的布尔判定，而非一个可报告的状态

**修正后的 A4 是"接现成的 6 态 + 加 1–2 个枚举值 + 接 per-capability 熔断"，不是新建机制。** 工时显著小于提案估计，收益（GUI 诊断可读）几乎立刻兑现。

---

## 2. 对优先级的意见

提案的排序是 `S1 → C13 → S2 → S3`。**我认为 S2 应与 C13 并行或提前，A4 应大幅前移。**

理由：

**① S2 不依赖 C13。** S2 是 plan 生成时声明 + outlet 执行时自检 + 事后比对，**纯本地闭环**；C13 是发布层。两者无先后依赖。

**② S2 反而为 C13 提供证据。** declared write-set 本身就是一条可引用的 capability 证据 —— 先做 S2，C13 的发布就有了实据可依赖。

**③ A4 成本极低。** 6 态已在、`DEGRADED` 已在，缺口是接线 + 1–2 个枚举值。放最后不合理。

**建议顺序：**

```
S1  Challenge Probe            （提取统一层，无新 JVMTI 能力）
 ↓
S2  Write-Set Lease            （真新机制；与 C13 并行）
 ↓
C13 当前接线完成                 （S2 为其提供证据）
 ↓
S3  Independent Witness        （用 RecoveryMutationIntent.Observation 当模板）
 ↓
A4  降级态接线                   （成本小，前移）
 ↓
S4  Observation Gap
 ↓
A1 / A2 / A3
```

---

## 3. 关于"不建议继续堆"的六项

提案明确不继续加：`MethodEntry` / `MethodExit` / 大量 `SingleStep` / 大量 `Breakpoint` / 全局 `Heap Walk` / 全量 stack sampling。

**同意，且理由比"厉害不厉害"更硬：** 这六项都是**"采得更多但不闭环"**的类型 —— 它们增加观察入口，却不改善 `观察 → 证据 → 授权 → 执行 → 独立结果 → 失效 → 重新建立` 这条链。当前瓶颈在**闭环**，不在**入口**。

**补充一点：** 本项目的 `TASK_TARGETS`（`ronova_native.cpp:29-34`，7 个 JDK 并发类）与 `SEMANTIC_TARGETS`（`:304-321`，16 个 MC 类）表明 —— **已有机制走的是"登记具体目标"而非"全量采样"的路线**。提案的判断与既有路线一致。

---

## 4. 边界与口径

- **L0 三条铁律与 UX1 不变。** 本文只读源码，不修改任何依赖/Mod/Forge 文件、不针对、不删除对方文件、不起外部进程。
- 不新增 `SubjectKey` / Root / 平行 D*·INV*·A*·R*·ID*·T* 编号。
- **未改**任何既有规划、九方向决策、54 键/14 动作接线、生产源码、构建、资源、`validation/` 台账或运行证据。
- **引用的行号均已逐条读取确认**；但如 §0 所述，本轮 **shell 不可用**，**无行数统计、无编译、无运行**。凡涉及"某类有多少行"的数字均**未在本轮复核**，标注为既往记录。
- **穷尽检索的边界：** "全树无命中"由 `grep` 得出，覆盖 `src/main/java/dev/ronova` 与 `native/src`。若某能力由运行期字符串/反射构造（本轮未发现），该结论需重核。
- **未做：** 未编译、未启动 JVM/游戏、未改 `validation/`、未回填任何 PASS。
