> **来源记录 · 写于 fix7 之前。** 本文基于 fix6 回执与核验补遗，不是对 fix7 的回应。fix7 已独立到达同向结论（其回执载明"通用 `resolve` 8 处、`registered*` 13 处，不得因全局 2 而全部削到 2"），与本文 §4 的两个调用点数**精确吻合**。**一处措辞以 fix7 为准**：本文 §4 写"路径 B 可以执行到 rank 4"，fix7 收窄为"可**表达** rank 3/4，不等于广谱已经获得 rank 4"——范围以 fix7 为准。方向为 Ronova 侧 → 外部研究方（**发出**）。

# R66 · 极限档天花板重审（基于 fix6 回执与核验补遗）

**性质：** 对"极限档加强"的**现状重审**。不是新提案，不新增方向/编号/批次。
**方法：** 先建现状模型，再谈加强。**逐条穷尽检索**，不以"名字没命中"推断"不存在"。
**依据：** fix6 回执（接受3/修改5/废弃1）+ `R66-V13-VERIFICATION-ADDENDUM.md` 三处精度修正。
**未做：** 未编译、未启动 JVM/游戏、未改源码、未改 `validation/`、未回填 PASS。

---

## 0. 为什么需要重审

前一轮九条提案里，**五处**栽在同一个错：**没先查机制是不是已经存在**。fix6 逐条去源码找出了现有机制（`WorkBudget`、`TerminalOperationPlan`、`CompletionTracker`、owner/lease/claim），核验补遗又收窄了三处。

所以这份重审换起手式：**先把"极限档现在到底解析成什么"量出来**，再谈加强。

结果是：**加强的真实目标和我原来想的不是一回事。**

---

## 1. 档位模型（实测）

### 1.1 两套词汇表，靠一个桥接方法相连

| 词汇表 | 文件 | 值 | 角色 |
|---|---|---|---|
| `AttackActionLevel` | `policy/AttackActionLevel.java`（15 行） | `FOLLOW_PROFILE, DISABLED, COMPATIBLE, ENHANCED, EXTREME` | **配置语法**。javadoc `:3` "Configuration syntax, independent of the advanced mechanism vocabulary" |
| `MechanismLevel` | `policy/MechanismLevel.java`（37 行） | `FOLLOW_PROFILE, OFF, MONITOR, PROTECT_RONOVA, RELATED_PATHS, STRICT_ALL` | **机制词汇表** |

桥接在 `AttackActionLevel.depth()`（`:7-14`）：

```
FOLLOW_PROFILE, DISABLED → OFF              (rank 0)
COMPATIBLE               → PROTECT_RONOVA   (rank 2)
ENHANCED                 → RELATED_PATHS    (rank 3)
EXTREME                  → STRICT_ALL       (rank 4)
```

> **更正我自己的一处记录。** 我之前记的是"Attack/Protection 档位集 = `FOLLOW_PROFILE/OFF/COMPATIBLE/ENHANCED/EXTREME`"。**两套词汇表确实存在**，但攻击侧的第二个值是 `DISABLED` 不是 `OFF`。

### 1.2 合成不是集合交，是按 rank 取小

> **更正我自己的第二处记录。** 我之前记的是"合成是集合交 `∩`，不是 `min(...)`"。**错了。**

`MechanismLevel.lowerOf`（`:28-32`）：

```java
public static MechanismLevel lowerOf(MechanismLevel requested, MechanismLevel ceiling) {
    MechanismLevel left = runnable(requested);
    MechanismLevel right = runnable(ceiling);
    return left.rank() <= right.rank() ? left : right;
}
```

`StrengthPolicySnapshot.resolve()`（`:230-233`）：

```java
MechanismLevel effective = MechanismLevel.lowerOf(
    MechanismLevel.lowerOf(activeRequested, supportedCeiling),
    evidenceCeiling
);
```

**纯 rank-min，三层。** 全局只有一个 `MechanismLevel` 枚举，没有第二套天花板类型。

---

## 2. 极限档请求什么

`ProfilePresetMatrix`（259 行）`revisionOne()`（`:96-154`）：

```java
:144  profiles.put(StrengthProfile.COMPATIBLE, immutable(compatible));
:145  profiles.put(StrengthProfile.ENHANCED,   immutable(filled(MechanismLevel.RELATED_PATHS)));
:147  profiles.put(StrengthProfile.EXTREME,    immutable(filled(MechanismLevel.STRICT_ALL)));
:149  profiles.put(StrengthProfile.CUSTOM,     immutable(filled(MechanismLevel.MONITOR)));
```

**只有 COMPATIBLE 是逐键手调的**（`:104-142`，约 20 处显式覆写）。ENHANCED / EXTREME / CUSTOM 都是 `filled(...)`——**54 个键填同一个值**。

而该类的 javadoc（`:8-14`）说这张表存在的意义是：

> "Keeping all 54 keys in one audited table prevents individual planes from **silently falling back to the old uniform r52 mapping**."

**即：建表是为了避免"统一映射"，但四个档里三个本身就是统一映射。** 逐平面差异化只在 COMPATIBLE 存在。

**结论：极限档已经把 `activeRequested` 拉到满档 `STRICT_ALL`（rank 4），对全部 54 个键。**
→ **任何"提高极限档请求强度"的加强都是空操作。**

`SAFETY_FLOORS`（`:156-168`）只覆盖 4 个键（`PROTECT_CORE_AUTHORITY`、`PROTECT_RESTART_RECOVERY`、`PROTECT_ROLLBACK`、`PROTECT_LOOP_GUARD` → `PROTECT_RONOVA`），**其余 50 键可被配置到 `PROTECT_RONOVA` 以下**。

---

## 3. 天花板实际在哪（决定性）

`StrengthPolicySnapshot:99`：

```java
this.supportedCeilings = levels(supportedCeilings, MechanismLevel.OFF);   // 默认 OFF
```

**全树递归检索 `withCapability`，只有两个真实授予站点：**

| 站点 | 范围 | 授予的最高 ceiling |
|---|---|---|
| `RuntimeCapabilityProjection.java:13` | 全局，3 键（`PROTECT_CODE_INTEGRITY`、`RUNTIME_CRITICAL_CLASS_WATCH`、`INTERCEPT_INSTRUMENTATION`） | `PROTECT_RONOVA`（rank 2） |
| `RuntimeCapabilityProjection.java:20` | 全局，3 键（`RUNTIME_JVMTI`、`RUNTIME_NATIVE`、`RUNTIME_BACKGROUND_INTEGRITY`） | `PROTECT_RONOVA`（rank 2） |
| `TerminalActionPolicy.java:40` | 本地作用域，5 键 | `PROTECT_RONOVA`（rank 2） |

`select-string 'withCapability\([^)]*(RELATED_PATHS|STRICT_ALL)'` 递归全树 → **零命中。**

> ### **∴ `supportedCeiling` 对每一个键、在每一条路径上，永远 ≤ `PROTECT_RONOVA`（rank 2）。**
> ### **∴ `effective = min(requested, supportedCeiling, evidenceCeiling) ≤ 2`，永远。**

**Rank 3（`RELATED_PATHS`）和 rank 4（`STRICT_ALL`）从未被授予为天花板。**

它们全树只以三种身份出现：

| 身份 | 出现处 |
|---|---|
| **请求** | `ProfilePresetMatrix:146/:148`、`AttackActionLevel:11-12`、`PolicyState:129-130` |
| **最低要求** | `RecoveryChainPolicy:11-24`（8 条通道）、`:33`、`:61` |
| **行为输入** | `StateFeedbackSchedule:23`——`horizon = requestedDepth==STRICT_ALL ? 0 : requestedDepth==RELATED_PATHS ? 100 : 20` |
| UI 文案 | `RonovaStrengthScreen:179` |

---

## 4. 三条路径的实际行为（核心发现）

同样 8 个 `RecoveryChainPolicy.Step`，有**两套并行路径**，都在生产使用：

### 路径 A — 通用 `resolve(policy, step)`（`:48-59`）

```java
:51  boolean observes = requested.rank() >= step.minimum().rank();
:55  boolean executes = resolution.permitted() && resolution.effectiveDepth().rank() >= step.minimum().rank();
```

`resolution` 来自 `resolveAttack()`（`StrengthPolicySnapshot:179-215`），其 `effective` 是 **ceiling 约束后**的值（`:202-210`）。

8 条通道的 `minimum` 是 `RELATED_PATHS`(3) 或 `STRICT_ALL`(4)。**而 `effectiveDepth ≤ 2`。**

> **∴ 路径 A 的 `executionPermitted()` 在每一个 Step 上永远是 `false`，理由串恒为 `RECOVERY_DEPTH_LIMITED_BY_DEPENDENCY`（`:58`）。**
> **外部调用点：8 个**（`TerminalPlanExecutor:77`、`RecoverySourceTracker:579/612/653` 等）。

### 路径 B — 四个 `registered*` 本地入口（`:75` / `:94` / `:107` / `:110`）

```java
:87-89   boolean observes  = requested.rank() >= minimum.rank();
         boolean permitted = observes && !policy.pendingRestart() && bindingVerified;
         return new Decision(step, requested, permitted ? requested : MechanismLevel.OFF, observes, permitted, ...)
```

**返回的是 `requested`，不是 `effectiveDepth`——绕过 ceiling。**

> **∴ 路径 B 可以在 rank 4 上执行**，只以 `bindingVerified` 与 `pendingRestart` 为闸，并以 reason 串自限定范围：
> `PERMITTED_FOR_THIS_STATIC_ATOMIC_SNAPSHOT_ONLY` / `PERMITTED_FOR_THIS_OBSERVED_NBT_SOURCE_ONLY` / `PERMITTED_FOR_THIS_REGISTERED_RECOVERY_WORK_ONLY`
> **外部调用点：13 个**（`EffectJournal:200/201/217/232`、`ShadowDurableRootStore:779/781/796/978/1006/1007/1034`、`RecoverySourceTracker:870` 等）。

### 断裂的形状

| | 读取的值 | 上限 | 生产调用点 |
|---|---|---|---|
| 全局发布 `effective` / `supportedCeiling` | `min(requested, ceiling, evidence)` | **rank 2** | 消费 `supportedCeilings()` / `effectiveLevel()` 的所有诊断与 UI |
| 路径 A 通用 `resolve()` | `effectiveDepth`（受 ceiling 约束） | **rank 2** → `executes` 恒 false | **8** |
| 路径 B 四个 `registered*` | `requested`（绕过 ceiling） | **rank 4** | **13** |

**执行能到 rank 4；发布与通用路径停在 rank 2。两者之间没有连接。**

这正是 C13 的准确形状，而且是**量化的、可复核的**——不再是"三层没贯通"这种定性描述。

---

## 5. 加强的真实目标

| 原以为的加强 | 实际 |
|---|---|
| 提高极限档请求强度 | **空操作**——`filled(STRICT_ALL)` 已是满档 |
| 救活 8 条恢复链通道 | **不必**——路径 B 的 13 个调用点已经在 rank 4 上执行 |
| "把功能接上" | **不是**——功能是活的，缺的是**发布与通用路径追赶不上执行** |

**真正的加强目标（按可验证性排序）：**

1. **让天花板能表达本地已达的档位。** 现状是"执行到 4、发布说 2"。最小改动是让路径 B 的 scoped 授予**同时**产生一条可发布的、带 scope 的 ceiling 记录——**不是**把全局 ceiling 抬到 4（那会变成对所有键的无据声明）。这是 C13 的本体。
2. **让路径 A 追上路径 B。** 同一批 8 个 Step，8 个调用点走 A、13 个走 B。需要明确：A 是应当废弃、应当改走 B、还是应当具备 B 的绕过语义。**这个决定没人做过**，而它决定那 8 个调用点的行为。
3. **补 `reason` 的消费者。** `RecoveryChainPolicy` 已经产出 12 种 reason 串，`resolveAttack` 产出 5 种。这些是现成的诊断素材，但目前没有统一消费者把它们变成可读状态。
4. **`TerminalOperationPlan` 的定值解锁**（核验补遗已提）：`reservedHeapBytes != 16384` 即抛（`:19`），四表面集精确相等（`:22-24`），工厂全硬编码（`:30-36`）。⑦ 要的逐计划切片要先解这个锁。

---

## 6. 对前九条的重新定位

| 方向 | 核验后状态 | 与本次重审的关系 |
|---|---|---|
| ① 观测成本模型 | 仍缺 `(surface, source)` 二级归集；`WorkBudget`/`FrameBudgetGovernor`/`HostResourceGovernor` 已在生产 | 与天花板无关，独立成立 |
| ② 对象生命期账本 | **前提被推翻**（`DurableTerminalWatchSet` 以投影为权威，弱引用只是三源候选之一） | 只剩"补回收观测" |
| ③ JIT 时机调度 | 先测；无数据不实现 | 与天花板无关 |
| ④ 类血缘执行器 | 键与策略声明存在；`ClassLineageResolver` 已两端接线 | 与天花板无关 |
| ⑤ 证书到期闸 | 无到期字段、无声明式依赖集；应沿 `RecoverySourceEvidence.MAX_AGE_NANOS` | 与天花板无关 |
| ⑥ 结算归属断言 | **"恰好一个结算归属者"确实不存在**；性质是新建而非扩展 | 是第 1 项加强的前置（谁有权授予 scoped ceiling） |
| ⑦ 跨表面预算分配 | 载体存在但是**固定常量** | 是第 4 项加强 |
| ⑧ 行号表信号 | 废弃 | —— |
| ⑨ 绕过面报告 | 接受 | **本次重审的天然载体**：本地 rank 4 执行 vs 发布 rank 2，本身就是一条必须写进报告的失效条件 |

**新增的第 10 项（本次重审的直接产物）：让已发布的 scope 反映本地实际执行档位。** 它是 C13 的可执行化，也是 ⑨ 的第一个真实填入项。

---

## 7. 边界与口径

- **L0 三条铁律与 UX1 不变。** 本文只读源码。
- 不新增 `SubjectKey` / Root / 平行 D*·INV*·A*·R*·ID*·T* 编号。
- **未改 fix6 任何回执结论，未改核验补遗任何结论。**
- 本文更正了**我自己**两处旧记录：攻击侧档位集第二值是 `DISABLED` 不是 `OFF`；合成是 rank-min 不是集合交。
- **穷尽检索的边界：** "没有站点授予 rank 3/4" 由递归 `select-string` 全树得出，覆盖 `src/main/java/dev/ronova` 全部深度。若 ceiling 由**运行期字符串/反射**构造（本文未发现此类路径），该结论需要重核。
- **未做：** 未编译、未启动 JVM/游戏、未走任何恢复链实机路径、未改 `validation/`、未回填 PASS。
