# 交接索引 · Ronova 侧 → 外部研究方

**日期：** 2026-09-15
**性质：** 三份独立文档。**前两份写于 fix7 之前，不是对 fix7 的回应**（各自顶部有来源说明）；第三份是对 fix7 交付件中检查器脚本的缺陷报告。
**包：** `ronova-v13-verification-and-ceiling-report-handoff.zip`

---

## 包内文件（字节数为实测值）

| 文件 | 类型 | 针对 | 字节 |
|---|---|---|---|
| `R66-V13-VERIFICATION-ADDENDUM.md` | 核验补遗 | fix6 回执所引的五条源码依据 | 15,263 |
| `R66-EXTREME-CEILING-REVIEW.md` | 现状重审 | 极限档的请求侧 / 天花板 / 三条并行路径 | 12,621 |
| `R66-MERGE-VERIFIER-PORTABILITY.md` | 缺陷报告 | fix7 的 `verify_addendum_merge.py` | 5,550 |

---

## 各自一句话

**① 核验补遗** — fix6 的五条依据**全部成立**；本文只在三条上加精度：
- `TerminalOperationPlan` 的四个成员目前是**固定常量**（`reservedHeapBytes != 16384` 即抛）
- `CompletionTracker` **没有到期字段**，其"超时"是采样间隔陈旧界；挂钟范式在 `RecoverySourceEvidence.MAX_AGE_NANOS`
- **"恰好一个结算归属者"确实不存在**——全仓零 `settlement` 类型声明

**② 天花板重审** — 先量现状再谈加强：
- 极限档 = `filled(STRICT_ALL)` × 54，**请求侧已满**，抬请求是空操作
- 穷尽检索：**零个站点**把 `RELATED_PATHS`/`STRICT_ALL` 作为 ceiling 授予
- 同一批 8 个 `Step` 两套并行路径：通用 `resolve()` 的 `executes` 恒 false（8 调用点）；四个 `registered*` 绕过 ceiling（13 调用点）
- **一处措辞以 fix7 为准**：② 写"路径 B 可以执行到 rank 4"，fix7 收窄为"可**表达** rank 3/4，不等于广谱已获得 rank 4"

**③ 检查器缺陷** — `surface_inventory()` 三处 `str(p.relative_to(...))` 应为 `.as_posix()`：
- 导致**在 Windows 上必然报** `Surface lexical inventory differs`
- 三条独立证据确认：规范化后两项自动匹配；`policy_classes` 0 vs 28；28 − 2 = **26** 与记录值精确吻合
- **不是被检查内容的问题**；`:275`/`:276` 的语料集校正在 Windows 上通过

---

## 与 fix7 的关系

| 本包文档 | fix7 是否已覆盖 |
|---|---|
| ① | **未覆盖**——三条均不在 fix7 讨论范围内 |
| ② | **部分同向**——fix7 已独立到达"通用 8 处 / `registered*` 13 处、不得因全局 2 而全部削到 2"，与② §4 数字精确吻合；② 是量化版 |
| ③ | **fix7 自身缺陷**，需原作者处置 |

---

## 边界

- 三份均为**只读静态分析**。未编译、未启动 JVM/游戏、未改源码、未改 `validation/`、未回填 PASS。
- L0 三条铁律与 UX1 不变；不新增 `SubjectKey` / Root / 平行编号。
- ③ **不修改** fix7 交付件任何字节，只给修法。
