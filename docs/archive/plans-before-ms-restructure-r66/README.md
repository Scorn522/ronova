# 有效规划

这里集中当前仍有效的架构合同和后续实施规划。
文档版本中的日期、r50/r54 等历史状态不代表当前代码；实现情况只看
[当前进度](../CURRENT-PROGRESS.md)。

## 当前优先工作

- [**设计规范 + 加强方案（合并视图）**](../DESIGN-SPEC.md)：**唯一加强入口**。
  第一部分 = 五份规范 + `CONTEXT.md` 的跨文档合并导读（**派生、非规范**，冲突以原始为准）；
  第二部分 = 加强方案提案（§15 三层规则源 / §16 合规判定 / §17 C 块 / §18 顺序）。
  已取代 4 份旧加强稿（V1.0 / MS 规则内 V2.0 / 可选强化设计 V1.0 / 合规极限 V1.0）。
- [7.17 极限档全体系加强规划](Ronova_7.17_Extreme_Full_Plane_Enhancement_Plan_V1.0_Internal_Root.md)：
  极限档定义、全部 54 项高级能力和 14 个普通动作、九个加强方向及完成判断；目标、已接线和已验证分别记录。
  2026-09-10 第 8–9 节新增参考 Mod 研究转化的 M01–M14、三阶段落点和逐项正负验收。
- [7.17 通用恢复链处置实施规划](Ronova_7.17_Generic_Recovery_Chain_Implementation_Plan_V1.0_Internal_Root.md)：
  同一次终结中处理已证明的目标恢复记录、任务、重建入口和相关资源。
  通用实现，已开始编码，整链未完成；不针对任何 Boss。
  2026-09-10 第 12 节补齐研究采纳的阶段一具体合同，已有实现与新要求分别说明。
- [7.17 加强总纲 V2.6](Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md)：
  §16.6 的 B/D、C/E、F/G 剩余工程继续在同一 7.17 范围推进。
- [MS 规则内加强方案 V2.0](Ronova_MS_Rulebound_Enhancement_Plan_V2.0.md)：**已并入** ——
  加强部分整合进 [`docs/DESIGN-SPEC.md`](../DESIGN-SPEC.md) 第二部分（§15–§20）。本文件保留作对照。
- [MS 未满足条款可选强化设计 V1.0](Ronova_MS_Remediation_Spec_V1.0.md)：**已并入** ——
  S1–S6 整合进 `DESIGN-SPEC.md` 第二部分 §17 的 C 块。本文件保留作对照。

## 唯一规范顺序

| 顺序 | 有效文档 | 用途 |
|---|---|---|
| 1 | [L6 Architecture Master V2.4](Ronova_L6_Architecture_Master_Plan_V2.4_Internal_Root.md) | D1–D13、Internal Root、单 writer、原子 batch 和宿主边界 |
| 2 | [Baseline / All Enhancements V1.4](Ronova_7.14A_Baseline_All_Enhancements_Master_V1.4_Internal_Root.md) | 阶段路线及已内联的加强约束 |
| 3 | [L6E-0 Contract Freeze V1.3](Ronova_L6E-0_Contract_Freeze_V1.3_Internal_Root.md) | SubjectKey、revision、副作用、执行上下文与客户端边界 |
| 4 | [7.16+ Strength Master V2.6](Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md) | 7.17 起的强度、执行、候选、恢复、资源和完成合同 |
| 5 | [P0-A Implementation Spec V1.3](Ronova_L6E_P0-A_Truth_Authority_Implementation_Spec_V1.3_Internal_Root.md) | FULL L6 Internal 之后的 Truth Authority 实施规格 |

7.17 恢复链规划、L6/L6E 全量加强路线及其他阶段规格属于第 5 层，不增加另一套 Root、身份或规范顺序。
较高位文档的冻结决策优先；实质冲突登记为设计缺口，不按“版本号更新”覆盖。

## 后续阶段

完整入口：[L6／L6E 全量加强路线 V1.0](Ronova_L6_L6E_Full_Enhancement_Roadmap_V1.0_Internal_Root.md)
（2026-09-09，后续规划）。逐项列出 54 项能力、14 个动作、8 类运行表面的阶段加强、依赖与完成条件，
并集中整理候选待办、客户端资源、接口迁移和 P1 扩展；7.17 原工程仍在当前阶段完成。

| 阶段 | 规划入口 | 当前定位 |
|---|---|---|
| 7.17 / FULL L5 候选 | V2.6 §16.2、§16.6；L6 Master §11–§12 | 正在施工；完成和验证按各自适用范围判断 |
| L6 Internal，8.0–8.4 | L6 Master §8、§11；Baseline §25.8 | 后续逻辑 writer 收口，不建立 External Root |
| L6E P0-A | L6E-0 与 P0-A V1.3 | 复用 canonical identity/binding/ownership 合同 |
| L6E P0-B/C/D | Baseline §4；V2.6 §16.4 | 执行、收敛、完整性；完整运行闭包尚未完成 |
| 后续 P1 扩展 | 全量加强路线 §10；Baseline §16–§22 | 深层 Native、可选渲染边界、移动/装备/效果、诊断和恢复连续性，逐项保留适用范围 |

## 参考与历史

- [领域词汇](../../CONTEXT.md)仍有效。
- [7.14D 预算合同](../reference/Ronova_7.14D_FrameBudget_Telemetry_Implementation_Spec_V1.0_Internal_Root.md)
  是已实施阶段的参考：可延期工作与同步查询的边界继续适用。
- [历史归档](../archive/README.md)保留加强稿 V2.4/V2.5、旧交接、旧进度和验证记录。
  这些不是并行的当前规划。

验证节奏按用户最新要求：开发期间做改动相关编译和针对性测试，主链接线后集中完整构建与阶段验收；
代码、失败原因或范围未变，不因为多份历史文档重复提到同一检查就逐项反复运行。
