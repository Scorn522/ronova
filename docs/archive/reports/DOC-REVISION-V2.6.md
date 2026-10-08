# Ronova V2.6 文档修订与交接

> 历史归档（2026-09-08 整理）：保留当时的设计、实现和验证口径，不代表当前源码状态。文中的“当前”“下一步”属于记录当时。
> 当前入口：[进度](../../CURRENT-PROGRESS.md) · [有效规划](../../plans/README.md)。

日期：2026-09-05。性质：DOCUMENT-ONLY。

当前加强实施入口为 [V2.6 完整主文档](../../plans/Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md)，规范顺序见 [架构索引](../../plans/README.md)。V2.5/V2.4 原稿保留为历史，不再并行解释当前实现合同。

## 已修订的设计

| 问题 | V2.6 的明确合同 |
|---|---|
| 普通开关与十五项高级政策不对应 | §6.7：十四项 typed 动作子字段及追踪别名，保留普通页逐项三档；与高级键取许可/深度交集，OFF 不可绕过；完整预设、Custom、同图持久化及迁移规则 |
| TargetEpoch 容易形成第二身份/代际 | §9.5.1–§9.5.2：复用 SubjectKey/Binding/Ownership；lineage 只作关系证据；资格 batch 整批原子提交，失败/未知不物理执行 |
| 第三方效果后崩溃可能重复执行 | §12.6、§15.7：稳定 effectKey、durable intent、EFFECT_UNKNOWN、查询/幂等/补偿/opaque 分类，跨 planRevision 去重，不把 Root COMMIT_UNKNOWN 当同一状态 |
| 客户端缺失或恶意时何时算清除 | §9.4.1：SERVER_RESULT 与指定客户端联合范围、参与 session/revision、required/optional、N/A 依据、断线与重连策略 |
| 不同 MD 各有一套优先级 | L6 Master → Baseline → L6E-0 → V2.6 → P0-A/阶段规格；同步四份上游/实现文档的链接与顺序，不改冻结不变量 |
| X01–X23 原文不在包内 | 明示缺件；新增自包含 B01–B23，绝不伪称找回旧用例或证明等价；历史收口需找回映射或维护者明确批准新基线 |
| 7.17 交付集合过大 | §16.6：A–G 内部批次，先合同/真实运行能力，再首条生产纵向链，最后完整候选验收 |
| 磁盘与去重状态可能无限增长 | §20.5：保留当前格式上限，提交前空间预留、实际失败分类、精确压缩、背压与恢复 reserve；不丢 Truth、不扩大 Tombstone |

新增 X57–X68 用例要求覆盖本轮接合；原 X24–X56 仍保留在主文档。B/X 编号是架构验收要求，不等于 Gradle SelfTest 数量。

## 实现状态不变

- 源码仍为 `0.2.0-alpha.7.17-r53`，54 个 StrengthOptionKey，policy schema 2，profile matrixRevision 1。
- schema 3 / AttackActionPolicy、完整 SubjectKey 迁移、effect recovery、参与范围与 storage reserve 均为后续设计，未在本轮编写源码。
- 保留现有 inherited dirty-marker、ProjectionWiring、RawObservation 与构建分层修复；未修改生产 Java、测试、DLL、Gradle 脚本或版本配置。
- 当前测试注册仍为 42 项：40 production JAR + 2 development outputs。最新完整成功日志和最终生产 JAR 的验收状态不因 MD 改动而改变。
- 本轮不运行 Gradle/Forge/GPU 测试，不宣称新运行 PASS，不生成新的生产 JAR。

## 文档检查与冻结边界

交付检查包含：新增/修改文档的相对文件链接、Markdown 表格列数与代码围栏、章节及验收编号、54-key 引用对照、历史 V2.5/V2.4 原稿保留，以及与上一份同步 ZIP 的非 MD 内容逐字节比较。

检查结果：12 份 MD 新增/修改，410 个既有非 MD 文件逐字节不变，未删除文件；历史 V2.5/V2.4 原稿逐字节不变。主文档含 23 项 B 基础用例、45 项 X24–X68 用例及完整 54-key 对照；新增链接、表格、围栏、章节/编号静态检查通过。这些数字不计作生产测试通过数。

这类静态检查不能证明 executor 有效或对第三方副作用实现 exactly-once。V2.6 保持 **Design Freeze Candidate**；新动作映射/迁移、基础用例与历史缺件处置需后续冻结评审。它已可作为下一轮细化与实现的工作稿，不把“文档完整”当成“代码已经写完”。

## 下一步

按 V2.6 §16.6 收口 r53 构建证据，然后实现普通许可与高级政策的同图解析、qualification batch 接合及 effect recovery 合同。各批只跑受影响的关键回归；新 executor 启用和候选出口再做相应成品/实机验证。
