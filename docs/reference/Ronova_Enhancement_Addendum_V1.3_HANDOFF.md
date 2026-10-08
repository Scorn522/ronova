> **来源记录 · 原交接方向为 Ronova 侧 → 外部研究方。** 以下为发出说明原文，当前处置见[fix6 九项回执](../plans/RESTRUCTURE-REVIEW.md#review-fix6-v13)。引用的工作区时间、台账和头文件读数仍是原作者回报，不冒充本交付环境复跑。

# 交接说明：Ronova Enhancement Addendum V1.3

**日期：** 2026-09-14
**方向：** Ronova 侧 → 外部研究方（**发出**，不是收到）
**正文：** `docs/plans/Ronova_Enhancement_Addendum_V1.3.md`

---

## 1. 这是什么

V1.3 是 **V1.1 / V1.2 之外的增量**，含 **9 条新方向**。

**它不是 V1.1/V1.2 的勘误，也不替换其中的条目。**

| 版本 | 作者 | 内容性质 |
|---|---|---|
| V1.1 | 外部研究方 | 追加建议（已被 V1.2 取代） |
| V1.2 | 外部研究方 | 追加建议（自包含） |
| **V1.3** | **Ronova 侧** | **尚未进入规划的新方向** |

前两份是"**已存在接线的缺口**"；V1.3 是"**还没被想到的方向**"。两者互补，不重叠。

**V1.3 全文由 Ronova 侧撰写，因此不含 `RONOVA-HANDOFF-ORIGINAL-*` 块。** 这是有意的：那对块用来保存**收到的外部原文**，V1.3 没有收到任何东西，加块会是假标记。

---

## 2. 九条一览

| # | 方向 | 前置 | 批次 | 备注 |
|---|---|---|---|---|
| ① | 观测成本模型 | 无（**根**） | 1 | ②③⑦ 的硬前置 |
| ② | 对象生命期账本（`can_tag_objects` + `OBJECT_FREE`） | ① | 4 | 取代 6 处 `WeakReference` |
| ③ | JIT 时机感知的重转换调度（`CompiledMethodLoad`） | ① | 4 | **主动给了废弃出口** |
| ④ | `ATTACK_TARGET_CLASS_LINEAGE` 执行器 | 与 `intercept.classloader` 定边界 | 2 | 键存在、书引用、无执行器 |
| ⑤ | 证书到期闸 + 依赖集哈希 | 无 | 2 | 给 `D75` 补可执行语义 |
| ⑥ | 结算归属断言 | 无 | 1 | ⑦ 的门 |
| ⑦ | 跨表面预算分配器 | ① + ⑥ | 3 | `D95` 的落地 |
| ⑧ | `LineNumberTable` 作为改写信号 | 无（研究） | 研究 | **假设，未验证，可整体废弃** |
| ⑨ | 把 `L6-INV-10` 绕过面写进能力报告 | 无 | 1 | 成本最低 |

---

## 3. 规则边界（请先接受这一条，否则下面每条都会被误判）

只有**两层**构成绑定共识：

- **L0 — MS 铁律**：`skills:62-64`（不修改文件 / 不针对 / 不删除对方文件）、`skills:398`（Forge 文件同理）
- **UX1 — 不启动外部进程**（用户明确追加）

**MS 文档里除上述之外的"必须/不得"文字，不构成违规判据，只作参考。** 本项目自订的 P 层规范（三本书 + 全部 D*/INV*/A*/R*/ID-*/T-* 条目）**自行决定、可以修改**——只要改完自洽，且不声称未实现的能力。

**所以每条方向只需要过两道闸：**

1. 触碰 L0 或 UX1？
2. 会让文档声称一个没有执行器、没有证据的能力？

**不需要**过"第三方兼容"闸。极限档没有兼容性可言。

> 附：`UX1` **推翻**了四份文档里允许 `AutoAttach`/`AttachHelper` 的文字——`L6:15`/`L6:20`/`L6:874`、`BL:135`/`BL:2941`、`CF:193-207`/`CF:1662-1664`(ID-9)、`V2.6:181`。那些文字作为历史保留，**授权一律作废**。相关位置已有就地提示（如 `MS-SKILLS-PLAN.md:2851`）。

---

## 4. 证据口径

V1.3 里每个 `文件:行号` 锚点**都是本轮实际读取并复核过的**，可直接反驳。

**JDK 17 头文件**（`D:\桌面\MOD\.build-tools\jdk17-headers\jvmti.h`，复核行号）：

```
:670  can_tag_objects
:682  can_get_line_numbers
:697  can_generate_compiled_method_load_events
:700  can_generate_native_method_bind_events
:702  can_generate_object_free_events
:703  can_force_early_return
:706  can_set_native_method_prefix
```

**原生源码中确认为 0 命中**（`native/src/*.cpp`、`*.hpp`）：

```
can_tag_objects = 0    OBJECT_FREE = 0           SetTag( = 0       GetTag( = 0
COMPILED_METHOD_LOAD = 0    CompiledMethodLoad = 0
DYNAMIC_CODE_GENERATED = 0  DynamicCodeGenerated = 0
```

**其它已复核锚点：**

- `BudgetCategory.java:5-10` — 恰好 6 类，**无表面维度、无事件源维度**
- `budget` 包内 `CrossSurface|crossSurface` = **0 命中**
- `DurableTerminalWatchSet.java` — `WeakReference` 6 处使用点（`:92`/`:117`/`:285`/`:434` 字段声明/`:449`/`:459`）+ `:14` import
- `StrengthOptionKey.java` — **54 个键**（分面 15/10/17/12），第 55 个正则命中是 `:71` 的构造器声明
- `StrengthOptionKey.java:20` — `ATTACK_TARGET_CLASS_LINEAGE("attack.target_class_lineage", PlaneId.ATTACK, true)`
- `StrengthOptionKey.java:60` — `RUNTIME_DYNAMIC_CODE_WATCH(..., true)`（**已声明但不在 6 个已发布键内**）
- `RuntimeCapabilityProjection.java:9-25` — 全局生产者只产出 **6 个键**
- `MS-SKILLS-PLAN.md:4483` / `:5569` / `:12626` — `target_class_lineage` 三处引用
- `MS-SKILLS-PLAN.md:4379` — `D75` 原文
- `MS-SKILLS-PLAN.md:4429` — `D95` 原文

**证据纪律（沿用不变）：**

- 头文件里有字段 **≠** 运行时可获得
- 字面计数 **≠** 实现状态
- **存在接线 ≠ 触达 ≠ 结果生效 ≠ 原口径验收完成**
- 外部来源的结论 **≠** 本机实际 VM 行为
- 没有证据不发表能力；没有执行器 + 证据 + 修复 + 测试，不声称 `ACTIVE`

---

## 5. 我想要的回执

**逐条给三选一：接受 / 修改 / 废弃**，并说明每条的前置是否成立。

**特别请求：**

- **③ 和 ⑧ 我主动给了废弃出口。** 请不要因为"技术上可行"而保留它们。③ 的价值完全取决于重转换的实际频率；⑧ 的假阳性风险可能高到让它没有价值。
- 若你判断某条的**成本高于收益**，直接否掉。

---

## 6. 我不要求你做的事

- **不要**去"点亮"那 48 个没有全局生产者的键。其中多数有**故意不发表**的本地通道（`RecoveryChainPolicy.java:93`、`TerminalActionPolicy.java:35-36`），全部发表是**倒退**。
- **不要**主张任何未经实测的数字。
- **不要**为第三方兼容折中。
- **不要**新建与既有编号平行的身份/权威/规范体系。
- **不要**触碰 L0 与 UX1。

---

## 7. 不会改变的东西

`validation/7.17/acceptance-matrix.json` **全程未动**（mtime `09/11/2026 09:56:44`，84,707 B）。本轮全部结论来自**静态源码读取**，未编译、未启动游戏、未新增 PASS。
