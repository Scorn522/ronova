# Ronova 跨文档不变量与条款 ID 索引

更新：2026-09-14 · 内容基线 fix1，fix2 追加验收规格入口。**派生阅读入口，不是新增规范，也不是实现或验收结果。**

[规划入口](README.md) · [B/X 验收规格对照](VALIDATION-TRACEABILITY.md) · [新旧章节映射](RESTRUCTURE-MAP.md) · [本轮审阅处理](RESTRUCTURE-REVIEW.md) · [机器清单](restructure/id-inventory.json)

从 `id-inventory.json` 生成，结合 `section-map.json` 和保真快照校验来源、原行范围、每段次数与新书锚点。编号明细一行对应一个**来源内的原 ID 拼写**；同一行列出全部登记位置，不把首次命中当作唯一权威定义。

覆盖 **16 份原来源、424 个来源内 ID 条目、929 个位置条目、1,047 次文本出现**。跨来源共有 273 种编号拼写；不跨来源合并、不重编号。

**读法提醒：** `D*`、`INV*`、`A*`、`R*`、`ID-*`、`T-*` 是原清单的检索家族，不保证每次出现都是定义；也包含引用、测试项和加强项。同一来源内部亦可能复用编号，例如 DS 的 `A5` 既用于不变量表，也用于加强 A 块。必须结合原标题和上下文阅读。本索引不裁决同号异义，不覆盖原稿已登记的两张 `L6-INV-*` 表不一致问题。

<a id="overview"></a>
## 不变量全集：人读总入口

按原 `DESIGN-SPEC.md` §4.2、§5 的阅读顺序恢复聚合导航。正文仍在三书唯一落点，索引不复制第二份规范。

| 原阅读单元 | 新书正文 | 原来源 |
|---|---|---|
| §4.2：D1–D13 决策表 | [技能书](MS-SKILLS-PLAN.md#orig-ds-l0177) | `DS:L177–L196` |
| §5：不变量全集 | [技能书](MS-SKILLS-PLAN.md#orig-ds-l0265) | `DS:L265–L266` |
| §5.1：PROC-INV-1 / D13；helper 原授权须按当前进程边界阅读 | [技能书](MS-SKILLS-PLAN.md#orig-ds-l0267) | `DS:L267–L282` |
| §5.2：L5-INV-1…9；保留原候选项说明 | [技能书](MS-SKILLS-PLAN.md#orig-ds-l0283) | `DS:L283–L298` |
| §5.3：L6-INV-1…10；含同号两表不一致的原提醒 | [技能书](MS-SKILLS-PLAN.md#orig-ds-l0299) | `DS:L299–L318` |
| §5.4：D33 修订 | [技能书](MS-SKILLS-PLAN.md#orig-ds-l0319) | `DS:L319–L328` |
| §5.5：D53–D106 分组导读及 D1–D32 继承原则 | [技能书](MS-SKILLS-PLAN.md#orig-ds-l0329) | `DS:L329–L363` |
| §5.6：CF 宪法级不变量与 R1–R7 | [技能书](MS-SKILLS-PLAN.md#orig-ds-l0364) | `DS:L364–L384` |
| §5.7：P0-A A1–A8、宪法级不变量与特有规则 | [技能书](MS-SKILLS-PLAN.md#orig-ds-l0385) | `DS:L385–L424` |

原文中的 helper 允许句只保真、不授权起进程，见[当前进程边界](README.md#scope-process)。“冻结”“宪法级”“必要条件”按[当前文档效力](README.md#scope-governance)阅读，不新增第五条铁律。

<a id="named-invariants"></a>
### 无编号宪法级不变量

这些名称不在编号正则清单中，因此单独按原章节补导航，不伪造 ID。CF 和 P0-A 各有八个名称，分别保留；相似名称不合并。

| 原来源 | 原名称（逐字保留） | 新书原章节 |
|---|---|---|
| `CF:L824–L857` | `TERMINAL precedence` | [技能书](MS-SKILLS-PLAN.md#orig-cf-l0824) |
| `CF:L824–L857` | `Epoch monotonicity` | [技能书](MS-SKILLS-PLAN.md#orig-cf-l0824) |
| `CF:L824–L857` | `COMMIT_UNKNOWN semantics` | [技能书](MS-SKILLS-PLAN.md#orig-cf-l0824) |
| `CF:L824–L857` | `Ownership revision ordering` | [技能书](MS-SKILLS-PLAN.md#orig-cf-l0824) |
| `CF:L824–L857` | `Tombstone durability` | [技能书](MS-SKILLS-PLAN.md#orig-cf-l0824) |
| `CF:L824–L857` | `Root writer uniqueness` | [技能书](MS-SKILLS-PLAN.md#orig-cf-l0824) |
| `CF:L824–L857` | `Stale incarnation denial` | [技能书](MS-SKILLS-PLAN.md#orig-cf-l0824) |
| `CF:L824–L857` | `Fence monotonic safety` | [技能书](MS-SKILLS-PLAN.md#orig-cf-l0824) |
| `P0A:L1261–L1285` | `TERMINAL precedence` | [技能书](MS-SKILLS-PLAN.md#orig-p0a-l1261) |
| `P0A:L1261–L1285` | `Epoch monotonicity` | [技能书](MS-SKILLS-PLAN.md#orig-p0a-l1261) |
| `P0A:L1261–L1285` | `COMMIT_UNKNOWN semantics` | [技能书](MS-SKILLS-PLAN.md#orig-p0a-l1261) |
| `P0A:L1261–L1285` | `BindingRevision monotonicity` | [技能书](MS-SKILLS-PLAN.md#orig-p0a-l1261) |
| `P0A:L1261–L1285` | `Tombstone durability` | [技能书](MS-SKILLS-PLAN.md#orig-p0a-l1261) |
| `P0A:L1261–L1285` | `Root writer uniqueness` | [技能书](MS-SKILLS-PLAN.md#orig-p0a-l1261) |
| `P0A:L1261–L1285` | `Stale incarnation denial` | [技能书](MS-SKILLS-PLAN.md#orig-p0a-l1261) |
| `P0A:L1261–L1285` | `HARDENED superset rule` | [技能书](MS-SKILLS-PLAN.md#orig-p0a-l1261) |

<a id="range-notation"></a>
### 范围写法不展开为虚构条文

输入有 `D1–D32`、`D33–D106` 等范围写法。编号清单只登记实际出现的编号拼写，不据端点或省略号生成中间编号的定义。尤其 `D14–D31`、`D34–D52` 没有作为独立编号拼写被原清单登记；这不是本轮删项，也不证明相应设计内容不存在。

| 原分组 | 原文入口 | 来源 |
|---|---|---|
| D1–D32 继承原则 | [技能书](MS-SKILLS-PLAN.md#orig-v26-l0455) | `V26:L455–L471` |
| D33 修订（不代写 D34–D52） | [技能书](MS-SKILLS-PLAN.md#orig-v26-l0472) | `V26:L472–L502` |

<a id="source-directory"></a>
## 按来源查看全部编号

各表中新书链接与来源行范围按相同顺序一一对应；`×n` 是该原段落内的文本出现次数，不是完成数量。

| 来源 | 原文件 / 导航 | 编号条目 | 文本出现 |
|---|---|---:|---:|
| [INDEX](#ids-index) | [README.md](README.md) | 2 | 2 |
| [BL](#ids-bl) | [Ronova_7.14A_Baseline_All_Enhancements_Master_V1.4_Internal_Root.md](Ronova_7.14A_Baseline_All_Enhancements_Master_V1.4_Internal_Root.md) | 5 | 10 |
| [V26](#ids-v26) | [Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md](Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md) | 62 | 67 |
| [FULL](#ids-full) | [Ronova_7.17_Extreme_Full_Plane_Enhancement_Plan_V1.0_Internal_Root.md](Ronova_7.17_Extreme_Full_Plane_Enhancement_Plan_V1.0_Internal_Root.md) | 0 | 0 |
| [TIER](#ids-tier) | [Ronova_7.17_Extreme_Tier_Strength_Implementation_Plan_V1.0_Internal_Root.md](Ronova_7.17_Extreme_Tier_Strength_Implementation_Plan_V1.0_Internal_Root.md) | 5 | 5 |
| [REC](#ids-rec) | [Ronova_7.17_Generic_Recovery_Chain_Implementation_Plan_V1.0_Internal_Root.md](Ronova_7.17_Generic_Recovery_Chain_Implementation_Plan_V1.0_Internal_Root.md) | 0 | 0 |
| [MAX](#ids-max) | [Ronova_Compliant_Maximum_Strength_Plan_V1.0.md](Ronova_Compliant_Maximum_Strength_Plan_V1.0.md) | 10 | 10 |
| [CF](#ids-cf) | [Ronova_L6E-0_Contract_Freeze_V1.3_Internal_Root.md](Ronova_L6E-0_Contract_Freeze_V1.3_Internal_Root.md) | 34 | 34 |
| [P0A](#ids-p0a) | [Ronova_L6E_P0-A_Truth_Authority_Implementation_Spec_V1.3_Internal_Root.md](Ronova_L6E_P0-A_Truth_Authority_Implementation_Spec_V1.3_Internal_Root.md) | 13 | 17 |
| [L6](#ids-l6) | [Ronova_L6_Architecture_Master_Plan_V2.4_Internal_Root.md](Ronova_L6_Architecture_Master_Plan_V2.4_Internal_Root.md) | 35 | 256 |
| [ROAD](#ids-road) | [Ronova_L6_L6E_Full_Enhancement_Roadmap_V1.0_Internal_Root.md](Ronova_L6_L6E_Full_Enhancement_Roadmap_V1.0_Internal_Root.md) | 2 | 4 |
| [MSC](#ids-msc) | [Ronova_MS_Compliance_and_Enhancement_Plan_V1.0.md](Ronova_MS_Compliance_and_Enhancement_Plan_V1.0.md) | 86 | 113 |
| [MSR](#ids-msr) | [Ronova_MS_Remediation_Spec_V1.0.md](Ronova_MS_Remediation_Spec_V1.0.md) | 6 | 13 |
| [RULE](#ids-rule) | [Ronova_MS_Rulebound_Enhancement_Plan_V2.0.md](Ronova_MS_Rulebound_Enhancement_Plan_V2.0.md) | 76 | 269 |
| [NON](#ids-non) | [Ronova_NonViolating_Enhancement_Plan_V1.0.md](Ronova_NonViolating_Enhancement_Plan_V1.0.md) | 8 | 11 |
| [DS](#ids-ds) | [DESIGN-SPEC.md](../DESIGN-SPEC.md) | 80 | 236 |

<a id="ids-index"></a>
### INDEX

原来源：`docs/plans/README.md`。[来源映射](RESTRUCTURE-MAP.md#source-index) · [原字节快照](../archive/plans-before-ms-restructure-r66/README.md) · [返回来源目录](#source-directory)。

| 原 ID | 新书锚点（×本段次数） | 原来源行范围 | 总次数 |
|---|---|---|---:|
| `D1` | [技能书 · 原L27](MS-SKILLS-PLAN.md#orig-index-l0027) ×1 | `INDEX:L27–L39` | 1 |
| `D13` | [技能书 · 原L27](MS-SKILLS-PLAN.md#orig-index-l0027) ×1 | `INDEX:L27–L39` | 1 |

<a id="ids-bl"></a>
### BL

原来源：`docs/plans/Ronova_7.14A_Baseline_All_Enhancements_Master_V1.4_Internal_Root.md`。[来源映射](RESTRUCTURE-MAP.md#source-bl) · [原字节快照](../archive/plans-before-ms-restructure-r66/Ronova_7.14A_Baseline_All_Enhancements_Master_V1.4_Internal_Root.md) · [返回来源目录](#source-directory)。

| 原 ID | 新书锚点（×本段次数） | 原来源行范围 | 总次数 |
|---|---|---|---:|
| `D5` | [技能书 · 原L2527](MS-SKILLS-PLAN.md#orig-bl-l2527) ×1 | `BL:L2527–L2543` | 1 |
| `D13` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-bl-l0001) ×1 | `BL:L1–L24` | 1 |
| `PROC-INV-1` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-bl-l0001) ×1；[技能书 · 原L101](MS-SKILLS-PLAN.md#orig-bl-l0101) ×1；[技能书 · 原L121](MS-SKILLS-PLAN.md#orig-bl-l0121) ×1；[技能书 · 原L2632](MS-SKILLS-PLAN.md#orig-bl-l2632) ×1；[技能书 · 原L2647](MS-SKILLS-PLAN.md#orig-bl-l2647) ×1；[技能书 · 原L2927](MS-SKILLS-PLAN.md#orig-bl-l2927) ×1 | `BL:L1–L24`；`BL:L101–L120`；`BL:L121–L138`；`BL:L2632–L2646`；`BL:L2647–L2662`；`BL:L2927–L2942` | 6 |
| `T-08` | [技能书 · 原L2963](MS-SKILLS-PLAN.md#orig-bl-l2963) ×1 | `BL:L2963–L2992` | 1 |
| `T-15` | [技能书 · 原L2963](MS-SKILLS-PLAN.md#orig-bl-l2963) ×1 | `BL:L2963–L2992` | 1 |

<a id="ids-v26"></a>
### V26

原来源：`docs/plans/Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md`。[来源映射](RESTRUCTURE-MAP.md#source-v26) · [原字节快照](../archive/plans-before-ms-restructure-r66/Ronova_7.16_Plus_JVM_Extreme_Strength_Enhancement_Master_V2.6_Internal_Root.md) · [返回来源目录](#source-directory)。

| 原 ID | 新书锚点（×本段次数） | 原来源行范围 | 总次数 |
|---|---|---|---:|
| `D1` | [技能书 · 原L132](MS-SKILLS-PLAN.md#orig-v26-l0132) ×1；[技能书 · 原L455](MS-SKILLS-PLAN.md#orig-v26-l0455) ×1；[技能书 · 原L2895](MS-SKILLS-PLAN.md#orig-v26-l2895) ×1 | `V26:L132–L155`；`V26:L455–L471`；`V26:L2895–L2926` | 3 |
| `D4` | [防御书 · 原L1665](MS-ENTITYDEFENCE-PLAN.md#orig-v26-l1665) ×1 | `V26:L1665–L1684` | 1 |
| `D5` | [技能书 · 原L2895](MS-SKILLS-PLAN.md#orig-v26-l2895) ×2 | `V26:L2895–L2926` | 2 |
| `D9` | [防御书 · 原L1665](MS-ENTITYDEFENCE-PLAN.md#orig-v26-l1665) ×1 | `V26:L1665–L1684` | 1 |
| `D11` | [防御书 · 原L1665](MS-ENTITYDEFENCE-PLAN.md#orig-v26-l1665) ×1 | `V26:L1665–L1684` | 1 |
| `D13` | [技能书 · 原L132](MS-SKILLS-PLAN.md#orig-v26-l0132) ×1；[技能书 · 原L2895](MS-SKILLS-PLAN.md#orig-v26-l2895) ×1 | `V26:L132–L155`；`V26:L2895–L2926` | 2 |
| `D32` | [技能书 · 原L455](MS-SKILLS-PLAN.md#orig-v26-l0455) ×1 | `V26:L455–L471` | 1 |
| `D33` | [技能书 · 原L132](MS-SKILLS-PLAN.md#orig-v26-l0132) ×1；[技能书 · 原L472](MS-SKILLS-PLAN.md#orig-v26-l0472) ×1 | `V26:L132–L155`；`V26:L472–L502` | 2 |
| `D53` | [技能书 · 原L503](MS-SKILLS-PLAN.md#orig-v26-l0503) ×1 | `V26:L503–L520` | 1 |
| `D54` | [技能书 · 原L503](MS-SKILLS-PLAN.md#orig-v26-l0503) ×1 | `V26:L503–L520` | 1 |
| `D55` | [技能书 · 原L503](MS-SKILLS-PLAN.md#orig-v26-l0503) ×1 | `V26:L503–L520` | 1 |
| `D56` | [技能书 · 原L503](MS-SKILLS-PLAN.md#orig-v26-l0503) ×1 | `V26:L503–L520` | 1 |
| `D57` | [技能书 · 原L503](MS-SKILLS-PLAN.md#orig-v26-l0503) ×1 | `V26:L503–L520` | 1 |
| `D58` | [技能书 · 原L503](MS-SKILLS-PLAN.md#orig-v26-l0503) ×1 | `V26:L503–L520` | 1 |
| `D59` | [技能书 · 原L503](MS-SKILLS-PLAN.md#orig-v26-l0503) ×1 | `V26:L503–L520` | 1 |
| `D60` | [技能书 · 原L503](MS-SKILLS-PLAN.md#orig-v26-l0503) ×1 | `V26:L503–L520` | 1 |
| `D61` | [技能书 · 原L503](MS-SKILLS-PLAN.md#orig-v26-l0503) ×1 | `V26:L503–L520` | 1 |
| `D62` | [技能书 · 原L503](MS-SKILLS-PLAN.md#orig-v26-l0503) ×1 | `V26:L503–L520` | 1 |
| `D63` | [技能书 · 原L503](MS-SKILLS-PLAN.md#orig-v26-l0503) ×1 | `V26:L503–L520` | 1 |
| `D64` | [技能书 · 原L503](MS-SKILLS-PLAN.md#orig-v26-l0503) ×1 | `V26:L503–L520` | 1 |
| `D65` | [技能书 · 原L503](MS-SKILLS-PLAN.md#orig-v26-l0503) ×1 | `V26:L503–L520` | 1 |
| `D66` | [技能书 · 原L521](MS-SKILLS-PLAN.md#orig-v26-l0521) ×1 | `V26:L521–L538` | 1 |
| `D67` | [技能书 · 原L521](MS-SKILLS-PLAN.md#orig-v26-l0521) ×1 | `V26:L521–L538` | 1 |
| `D68` | [技能书 · 原L521](MS-SKILLS-PLAN.md#orig-v26-l0521) ×1 | `V26:L521–L538` | 1 |
| `D69` | [技能书 · 原L521](MS-SKILLS-PLAN.md#orig-v26-l0521) ×1 | `V26:L521–L538` | 1 |
| `D70` | [技能书 · 原L521](MS-SKILLS-PLAN.md#orig-v26-l0521) ×1 | `V26:L521–L538` | 1 |
| `D71` | [技能书 · 原L521](MS-SKILLS-PLAN.md#orig-v26-l0521) ×1 | `V26:L521–L538` | 1 |
| `D72` | [技能书 · 原L521](MS-SKILLS-PLAN.md#orig-v26-l0521) ×1 | `V26:L521–L538` | 1 |
| `D73` | [技能书 · 原L521](MS-SKILLS-PLAN.md#orig-v26-l0521) ×1 | `V26:L521–L538` | 1 |
| `D74` | [技能书 · 原L521](MS-SKILLS-PLAN.md#orig-v26-l0521) ×1 | `V26:L521–L538` | 1 |
| `D75` | [技能书 · 原L521](MS-SKILLS-PLAN.md#orig-v26-l0521) ×1 | `V26:L521–L538` | 1 |
| `D76` | [技能书 · 原L521](MS-SKILLS-PLAN.md#orig-v26-l0521) ×1 | `V26:L521–L538` | 1 |
| `D77` | [技能书 · 原L521](MS-SKILLS-PLAN.md#orig-v26-l0521) ×1 | `V26:L521–L538` | 1 |
| `D78` | [技能书 · 原L521](MS-SKILLS-PLAN.md#orig-v26-l0521) ×1 | `V26:L521–L538` | 1 |
| `D79` | [技能书 · 原L539](MS-SKILLS-PLAN.md#orig-v26-l0539) ×1 | `V26:L539–L551` | 1 |
| `D80` | [技能书 · 原L539](MS-SKILLS-PLAN.md#orig-v26-l0539) ×1 | `V26:L539–L551` | 1 |
| `D81` | [技能书 · 原L539](MS-SKILLS-PLAN.md#orig-v26-l0539) ×1 | `V26:L539–L551` | 1 |
| `D82` | [技能书 · 原L539](MS-SKILLS-PLAN.md#orig-v26-l0539) ×1 | `V26:L539–L551` | 1 |
| `D83` | [技能书 · 原L539](MS-SKILLS-PLAN.md#orig-v26-l0539) ×1 | `V26:L539–L551` | 1 |
| `D84` | [技能书 · 原L539](MS-SKILLS-PLAN.md#orig-v26-l0539) ×1 | `V26:L539–L551` | 1 |
| `D85` | [技能书 · 原L539](MS-SKILLS-PLAN.md#orig-v26-l0539) ×1 | `V26:L539–L551` | 1 |
| `D86` | [技能书 · 原L539](MS-SKILLS-PLAN.md#orig-v26-l0539) ×1 | `V26:L539–L551` | 1 |
| `D87` | [技能书 · 原L552](MS-SKILLS-PLAN.md#orig-v26-l0552) ×1 | `V26:L552–L570` | 1 |
| `D88` | [技能书 · 原L552](MS-SKILLS-PLAN.md#orig-v26-l0552) ×1 | `V26:L552–L570` | 1 |
| `D89` | [技能书 · 原L552](MS-SKILLS-PLAN.md#orig-v26-l0552) ×1 | `V26:L552–L570` | 1 |
| `D90` | [技能书 · 原L552](MS-SKILLS-PLAN.md#orig-v26-l0552) ×1 | `V26:L552–L570` | 1 |
| `D91` | [技能书 · 原L552](MS-SKILLS-PLAN.md#orig-v26-l0552) ×1 | `V26:L552–L570` | 1 |
| `D92` | [技能书 · 原L552](MS-SKILLS-PLAN.md#orig-v26-l0552) ×1 | `V26:L552–L570` | 1 |
| `D93` | [技能书 · 原L552](MS-SKILLS-PLAN.md#orig-v26-l0552) ×1 | `V26:L552–L570` | 1 |
| `D94` | [技能书 · 原L552](MS-SKILLS-PLAN.md#orig-v26-l0552) ×1 | `V26:L552–L570` | 1 |
| `D95` | [技能书 · 原L552](MS-SKILLS-PLAN.md#orig-v26-l0552) ×1 | `V26:L552–L570` | 1 |
| `D96` | [技能书 · 原L552](MS-SKILLS-PLAN.md#orig-v26-l0552) ×1 | `V26:L552–L570` | 1 |
| `D97` | [技能书 · 原L552](MS-SKILLS-PLAN.md#orig-v26-l0552) ×1 | `V26:L552–L570` | 1 |
| `D98` | [技能书 · 原L552](MS-SKILLS-PLAN.md#orig-v26-l0552) ×1 | `V26:L552–L570` | 1 |
| `D99` | [技能书 · 原L571](MS-SKILLS-PLAN.md#orig-v26-l0571) ×1 | `V26:L571–L583` | 1 |
| `D100` | [技能书 · 原L571](MS-SKILLS-PLAN.md#orig-v26-l0571) ×1 | `V26:L571–L583` | 1 |
| `D101` | [技能书 · 原L571](MS-SKILLS-PLAN.md#orig-v26-l0571) ×1 | `V26:L571–L583` | 1 |
| `D102` | [技能书 · 原L571](MS-SKILLS-PLAN.md#orig-v26-l0571) ×1 | `V26:L571–L583` | 1 |
| `D103` | [技能书 · 原L571](MS-SKILLS-PLAN.md#orig-v26-l0571) ×1 | `V26:L571–L583` | 1 |
| `D104` | [技能书 · 原L571](MS-SKILLS-PLAN.md#orig-v26-l0571) ×1 | `V26:L571–L583` | 1 |
| `D105` | [技能书 · 原L571](MS-SKILLS-PLAN.md#orig-v26-l0571) ×1 | `V26:L571–L583` | 1 |
| `D106` | [技能书 · 原L571](MS-SKILLS-PLAN.md#orig-v26-l0571) ×1 | `V26:L571–L583` | 1 |

<a id="ids-full"></a>
### FULL

原来源：`docs/plans/Ronova_7.17_Extreme_Full_Plane_Enhancement_Plan_V1.0_Internal_Root.md`。[来源映射](RESTRUCTURE-MAP.md#source-full) · [原字节快照](../archive/plans-before-ms-restructure-r66/Ronova_7.17_Extreme_Full_Plane_Enhancement_Plan_V1.0_Internal_Root.md) · [返回来源目录](#source-directory)。

原编号清单在此来源下未登记上述 ID 家族；**不等于没有条款**。原文保留由文档恢复核对覆盖。

<a id="ids-tier"></a>
### TIER

原来源：`docs/plans/Ronova_7.17_Extreme_Tier_Strength_Implementation_Plan_V1.0_Internal_Root.md`。[来源映射](RESTRUCTURE-MAP.md#source-tier) · [原字节快照](../archive/plans-before-ms-restructure-r66/Ronova_7.17_Extreme_Tier_Strength_Implementation_Plan_V1.0_Internal_Root.md) · [返回来源目录](#source-directory)。

| 原 ID | 新书锚点（×本段次数） | 原来源行范围 | 总次数 |
|---|---|---|---:|
| `D13` | [技能书 · 原L749](MS-SKILLS-PLAN.md#orig-tier-l0749) ×1 | `TIER:L749–L761` | 1 |
| `D60` | [技能书 · 原L815](MS-SKILLS-PLAN.md#orig-tier-l0815) ×1 | `TIER:L815–L833` | 1 |
| `D65` | [技能书 · 原L815](MS-SKILLS-PLAN.md#orig-tier-l0815) ×1 | `TIER:L815–L833` | 1 |
| `L5-INV-2` | [技能书 · 原L621](MS-SKILLS-PLAN.md#orig-tier-l0621) ×1 | `TIER:L621–L655` | 1 |
| `PROC-INV-1` | [技能书 · 原L749](MS-SKILLS-PLAN.md#orig-tier-l0749) ×1 | `TIER:L749–L761` | 1 |

<a id="ids-rec"></a>
### REC

原来源：`docs/plans/Ronova_7.17_Generic_Recovery_Chain_Implementation_Plan_V1.0_Internal_Root.md`。[来源映射](RESTRUCTURE-MAP.md#source-rec) · [原字节快照](../archive/plans-before-ms-restructure-r66/Ronova_7.17_Generic_Recovery_Chain_Implementation_Plan_V1.0_Internal_Root.md) · [返回来源目录](#source-directory)。

原编号清单在此来源下未登记上述 ID 家族；**不等于没有条款**。原文保留由文档恢复核对覆盖。

<a id="ids-max"></a>
### MAX

原来源：`docs/plans/Ronova_Compliant_Maximum_Strength_Plan_V1.0.md`。[来源映射](RESTRUCTURE-MAP.md#source-max) · [原字节快照](../archive/plans-before-ms-restructure-r66/Ronova_Compliant_Maximum_Strength_Plan_V1.0.md) · [返回来源目录](#source-directory)。

| 原 ID | 新书锚点（×本段次数） | 原来源行范围 | 总次数 |
|---|---|---|---:|
| `D1` | [技能书 · 原L157](MS-SKILLS-PLAN.md#orig-max-l0157) ×1 | `MAX:L157–L171` | 1 |
| `D2` | [技能书 · 原L157](MS-SKILLS-PLAN.md#orig-max-l0157) ×1 | `MAX:L157–L171` | 1 |
| `D3` | [技能书 · 原L157](MS-SKILLS-PLAN.md#orig-max-l0157) ×1 | `MAX:L157–L171` | 1 |
| `D4` | [技能书 · 原L157](MS-SKILLS-PLAN.md#orig-max-l0157) ×1 | `MAX:L157–L171` | 1 |
| `D5` | [技能书 · 原L157](MS-SKILLS-PLAN.md#orig-max-l0157) ×1 | `MAX:L157–L171` | 1 |
| `D6` | [技能书 · 原L157](MS-SKILLS-PLAN.md#orig-max-l0157) ×1 | `MAX:L157–L171` | 1 |
| `D7` | [技能书 · 原L157](MS-SKILLS-PLAN.md#orig-max-l0157) ×1 | `MAX:L157–L171` | 1 |
| `D8` | [技能书 · 原L157](MS-SKILLS-PLAN.md#orig-max-l0157) ×1 | `MAX:L157–L171` | 1 |
| `D9` | [技能书 · 原L157](MS-SKILLS-PLAN.md#orig-max-l0157) ×1 | `MAX:L157–L171` | 1 |
| `D10` | [技能书 · 原L157](MS-SKILLS-PLAN.md#orig-max-l0157) ×1 | `MAX:L157–L171` | 1 |

<a id="ids-cf"></a>
### CF

原来源：`docs/plans/Ronova_L6E-0_Contract_Freeze_V1.3_Internal_Root.md`。[来源映射](RESTRUCTURE-MAP.md#source-cf) · [原字节快照](../archive/plans-before-ms-restructure-r66/Ronova_L6E-0_Contract_Freeze_V1.3_Internal_Root.md) · [返回来源目录](#source-directory)。

| 原 ID | 新书锚点（×本段次数） | 原来源行范围 | 总次数 |
|---|---|---|---:|
| `D13` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-cf-l0001) ×1 | `CF:L1–L26` | 1 |
| `ID-1` | [防御书 · 原L1567](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1567) ×1 | `CF:L1567–L1576` | 1 |
| `ID-2` | [物品书 · 原L1577](MS-MSITEM-PLAN.md#orig-cf-l1577) ×1 | `CF:L1577–L1591` | 1 |
| `ID-3` | [技能书 · 原L1592](MS-SKILLS-PLAN.md#orig-cf-l1592) ×1 | `CF:L1592–L1603` | 1 |
| `ID-4` | [物品书 · 原L1604](MS-MSITEM-PLAN.md#orig-cf-l1604) ×1 | `CF:L1604–L1615` | 1 |
| `ID-5` | [技能书 · 原L1616](MS-SKILLS-PLAN.md#orig-cf-l1616) ×1 | `CF:L1616–L1633` | 1 |
| `ID-6` | [技能书 · 原L1634](MS-SKILLS-PLAN.md#orig-cf-l1634) ×1 | `CF:L1634–L1649` | 1 |
| `ID-7` | [技能书 · 原L1650](MS-SKILLS-PLAN.md#orig-cf-l1650) ×1 | `CF:L1650–L1655` | 1 |
| `ID-8` | [技能书 · 原L1656](MS-SKILLS-PLAN.md#orig-cf-l1656) ×1 | `CF:L1656–L1661` | 1 |
| `ID-9` | [技能书 · 原L1662](MS-SKILLS-PLAN.md#orig-cf-l1662) ×1 | `CF:L1662–L1667` | 1 |
| `PROC-INV-1` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-cf-l0001) ×1 | `CF:L1–L26` | 1 |
| `R1` | [技能书 · 原L82](MS-SKILLS-PLAN.md#orig-cf-l0082) ×1 | `CF:L82–L99` | 1 |
| `R2` | [技能书 · 原L100](MS-SKILLS-PLAN.md#orig-cf-l0100) ×1 | `CF:L100–L116` | 1 |
| `R3` | [物品书 · 原L117](MS-MSITEM-PLAN.md#orig-cf-l0117) ×1 | `CF:L117–L139` | 1 |
| `R4` | [技能书 · 原L140](MS-SKILLS-PLAN.md#orig-cf-l0140) ×1 | `CF:L140–L159` | 1 |
| `R5` | [技能书 · 原L160](MS-SKILLS-PLAN.md#orig-cf-l0160) ×1 | `CF:L160–L177` | 1 |
| `R6` | [技能书 · 原L178](MS-SKILLS-PLAN.md#orig-cf-l0178) ×1 | `CF:L178–L192` | 1 |
| `R7` | [技能书 · 原L193](MS-SKILLS-PLAN.md#orig-cf-l0193) ×1 | `CF:L193–L210` | 1 |
| `T-01` | [防御书 · 原L1404](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1404) ×1 | `CF:L1404–L1419` | 1 |
| `T-02` | [防御书 · 原L1420](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1420) ×1 | `CF:L1420–L1436` | 1 |
| `T-03` | [防御书 · 原L1437](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1437) ×1 | `CF:L1437–L1456` | 1 |
| `T-04` | [技能书 · 原L1457](MS-SKILLS-PLAN.md#orig-cf-l1457) ×1 | `CF:L1457–L1468` | 1 |
| `T-05` | [防御书 · 原L1469](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1469) ×1 | `CF:L1469–L1480` | 1 |
| `T-06` | [防御书 · 原L1481](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1481) ×1 | `CF:L1481–L1496` | 1 |
| `T-07` | [防御书 · 原L1497](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1497) ×1 | `CF:L1497–L1508` | 1 |
| `T-08` | [防御书 · 原L1509](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1509) ×1 | `CF:L1509–L1514` | 1 |
| `T-09` | [防御书 · 原L1515](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1515) ×1 | `CF:L1515–L1520` | 1 |
| `T-10` | [防御书 · 原L1521](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1521) ×1 | `CF:L1521–L1526` | 1 |
| `T-11` | [防御书 · 原L1527](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1527) ×1 | `CF:L1527–L1532` | 1 |
| `T-12` | [技能书 · 原L1533](MS-SKILLS-PLAN.md#orig-cf-l1533) ×1 | `CF:L1533–L1538` | 1 |
| `T-13` | [防御书 · 原L1539](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1539) ×1 | `CF:L1539–L1544` | 1 |
| `T-14` | [防御书 · 原L1545](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1545) ×1 | `CF:L1545–L1550` | 1 |
| `T-15` | [防御书 · 原L1551](MS-ENTITYDEFENCE-PLAN.md#orig-cf-l1551) ×1 | `CF:L1551–L1556` | 1 |
| `T-16` | [技能书 · 原L1557](MS-SKILLS-PLAN.md#orig-cf-l1557) ×1 | `CF:L1557–L1562` | 1 |

<a id="ids-p0a"></a>
### P0A

原来源：`docs/plans/Ronova_L6E_P0-A_Truth_Authority_Implementation_Spec_V1.3_Internal_Root.md`。[来源映射](RESTRUCTURE-MAP.md#source-p0a) · [原字节快照](../archive/plans-before-ms-restructure-r66/Ronova_L6E_P0-A_Truth_Authority_Implementation_Spec_V1.3_Internal_Root.md) · [返回来源目录](#source-directory)。

| 原 ID | 新书锚点（×本段次数） | 原来源行范围 | 总次数 |
|---|---|---|---:|
| `A1` | [防御书 · 原L94](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0094) ×1 | `P0A:L94–L104` | 1 |
| `A2` | [防御书 · 原L105](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0105) ×1 | `P0A:L105–L120` | 1 |
| `A3` | [防御书 · 原L121](MS-ENTITYDEFENCE-PLAN.md#orig-p0a-l0121) ×1 | `P0A:L121–L130` | 1 |
| `A4` | [物品书 · 原L131](MS-MSITEM-PLAN.md#orig-p0a-l0131) ×1 | `P0A:L131–L151` | 1 |
| `A5` | [技能书 · 原L152](MS-SKILLS-PLAN.md#orig-p0a-l0152) ×1 | `P0A:L152–L159` | 1 |
| `A6` | [技能书 · 原L160](MS-SKILLS-PLAN.md#orig-p0a-l0160) ×1 | `P0A:L160–L170` | 1 |
| `A7` | [技能书 · 原L171](MS-SKILLS-PLAN.md#orig-p0a-l0171) ×1 | `P0A:L171–L182` | 1 |
| `A8` | [技能书 · 原L183](MS-SKILLS-PLAN.md#orig-p0a-l0183) ×1 | `P0A:L183–L200` | 1 |
| `D1` | [技能书 · 原L26](MS-SKILLS-PLAN.md#orig-p0a-l0026) ×1 | `P0A:L26–L51` | 1 |
| `D6` | [技能书 · 原L903](MS-SKILLS-PLAN.md#orig-p0a-l0903) ×1 | `P0A:L903–L932` | 1 |
| `D9` | [技能书 · 原L903](MS-SKILLS-PLAN.md#orig-p0a-l0903) ×1 | `P0A:L903–L932` | 1 |
| `D13` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-p0a-l0001) ×1；[技能书 · 原L26](MS-SKILLS-PLAN.md#orig-p0a-l0026) ×1 | `P0A:L1–L25`；`P0A:L26–L51` | 2 |
| `PROC-INV-1` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-p0a-l0001) ×1；[技能书 · 原L183](MS-SKILLS-PLAN.md#orig-p0a-l0183) ×1；[技能书 · 原L2112](MS-SKILLS-PLAN.md#orig-p0a-l2112) ×1；[技能书 · 原L2139](MS-SKILLS-PLAN.md#orig-p0a-l2139) ×1 | `P0A:L1–L25`；`P0A:L183–L200`；`P0A:L2112–L2138`；`P0A:L2139–L2157` | 4 |

<a id="ids-l6"></a>
### L6

原来源：`docs/plans/Ronova_L6_Architecture_Master_Plan_V2.4_Internal_Root.md`。[来源映射](RESTRUCTURE-MAP.md#source-l6) · [原字节快照](../archive/plans-before-ms-restructure-r66/Ronova_L6_Architecture_Master_Plan_V2.4_Internal_Root.md) · [返回来源目录](#source-directory)。

| 原 ID | 新书锚点（×本段次数） | 原来源行范围 | 总次数 |
|---|---|---|---:|
| `D1` | [技能书 · 原L30](MS-SKILLS-PLAN.md#orig-l6-l0030) ×1；[技能书 · 原L45](MS-SKILLS-PLAN.md#orig-l6-l0045) ×1；[技能书 · 原L60](MS-SKILLS-PLAN.md#orig-l6-l0060) ×2；[技能书 · 原L142](MS-SKILLS-PLAN.md#orig-l6-l0142) ×1；[技能书 · 原L198](MS-SKILLS-PLAN.md#orig-l6-l0198) ×1；[技能书 · 原L206](MS-SKILLS-PLAN.md#orig-l6-l0206) ×1；[物品书 · 原L629](MS-MSITEM-PLAN.md#orig-l6-l0629) ×1；[技能书 · 原L960](MS-SKILLS-PLAN.md#orig-l6-l0960) ×1；[技能书 · 原L1238](MS-SKILLS-PLAN.md#orig-l6-l1238) ×1；[技能书 · 原L1293](MS-SKILLS-PLAN.md#orig-l6-l1293) ×2；[技能书 · 原L1443](MS-SKILLS-PLAN.md#orig-l6-l1443) ×1 | `L6:L30–L44`；`L6:L45–L59`；`L6:L60–L88`；`L6:L142–L147`；`L6:L198–L205`；`L6:L206–L334`；`L6:L629–L636`；`L6:L960–L981`；`L6:L1238–L1258`；`L6:L1293–L1323`；`L6:L1443–L1454` | 13 |
| `D2` | [技能书 · 原L45](MS-SKILLS-PLAN.md#orig-l6-l0045) ×1；[技能书 · 原L60](MS-SKILLS-PLAN.md#orig-l6-l0060) ×1；[技能书 · 原L155](MS-SKILLS-PLAN.md#orig-l6-l0155) ×1；[技能书 · 原L206](MS-SKILLS-PLAN.md#orig-l6-l0206) ×1；[技能书 · 原L500](MS-SKILLS-PLAN.md#orig-l6-l0500) ×1；[技能书 · 原L604](MS-SKILLS-PLAN.md#orig-l6-l0604) ×1；[物品书 · 原L663](MS-MSITEM-PLAN.md#orig-l6-l0663) ×1；[物品书 · 原L685](MS-MSITEM-PLAN.md#orig-l6-l0685) ×1；[技能书 · 原L960](MS-SKILLS-PLAN.md#orig-l6-l0960) ×1；[技能书 · 原L992](MS-SKILLS-PLAN.md#orig-l6-l0992) ×1；[技能书 · 原L1074](MS-SKILLS-PLAN.md#orig-l6-l1074) ×1；[技能书 · 原L1215](MS-SKILLS-PLAN.md#orig-l6-l1215) ×1；[技能书 · 原L1238](MS-SKILLS-PLAN.md#orig-l6-l1238) ×1；[技能书 · 原L1293](MS-SKILLS-PLAN.md#orig-l6-l1293) ×1；[技能书 · 原L1326](MS-SKILLS-PLAN.md#orig-l6-l1326) ×1 | `L6:L45–L59`；`L6:L60–L88`；`L6:L155–L161`；`L6:L206–L334`；`L6:L500–L546`；`L6:L604–L618`；`L6:L663–L684`；`L6:L685–L706`；`L6:L960–L981`；`L6:L992–L1006`；`L6:L1074–L1110`；`L6:L1215–L1237`；`L6:L1238–L1258`；`L6:L1293–L1323`；`L6:L1326–L1340` | 15 |
| `D3` | [技能书 · 原L45](MS-SKILLS-PLAN.md#orig-l6-l0045) ×1；[技能书 · 原L178](MS-SKILLS-PLAN.md#orig-l6-l0178) ×1；[技能书 · 原L206](MS-SKILLS-PLAN.md#orig-l6-l0206) ×2；[技能书 · 原L391](MS-SKILLS-PLAN.md#orig-l6-l0391) ×1；[技能书 · 原L498](MS-SKILLS-PLAN.md#orig-l6-l0498) ×1；[技能书 · 原L500](MS-SKILLS-PLAN.md#orig-l6-l0500) ×1；[技能书 · 原L604](MS-SKILLS-PLAN.md#orig-l6-l0604) ×1；[技能书 · 原L1007](MS-SKILLS-PLAN.md#orig-l6-l1007) ×1；[技能书 · 原L1119](MS-SKILLS-PLAN.md#orig-l6-l1119) ×1；[技能书 · 原L1215](MS-SKILLS-PLAN.md#orig-l6-l1215) ×1；[技能书 · 原L1238](MS-SKILLS-PLAN.md#orig-l6-l1238) ×1；[技能书 · 原L1293](MS-SKILLS-PLAN.md#orig-l6-l1293) ×1；[技能书 · 原L1326](MS-SKILLS-PLAN.md#orig-l6-l1326) ×1 | `L6:L45–L59`；`L6:L178–L183`；`L6:L206–L334`；`L6:L391–L414`；`L6:L498–L499`；`L6:L500–L546`；`L6:L604–L618`；`L6:L1007–L1019`；`L6:L1119–L1132`；`L6:L1215–L1237`；`L6:L1238–L1258`；`L6:L1293–L1323`；`L6:L1326–L1340` | 14 |
| `D4` | [技能书 · 原L45](MS-SKILLS-PLAN.md#orig-l6-l0045) ×1；[技能书 · 原L155](MS-SKILLS-PLAN.md#orig-l6-l0155) ×1；[技能书 · 原L170](MS-SKILLS-PLAN.md#orig-l6-l0170) ×1；[技能书 · 原L206](MS-SKILLS-PLAN.md#orig-l6-l0206) ×1；[技能书 · 原L391](MS-SKILLS-PLAN.md#orig-l6-l0391) ×1；[技能书 · 原L947](MS-SKILLS-PLAN.md#orig-l6-l0947) ×1；[技能书 · 原L1119](MS-SKILLS-PLAN.md#orig-l6-l1119) ×1；[技能书 · 原L1238](MS-SKILLS-PLAN.md#orig-l6-l1238) ×1；[技能书 · 原L1293](MS-SKILLS-PLAN.md#orig-l6-l1293) ×1 | `L6:L45–L59`；`L6:L155–L161`；`L6:L170–L177`；`L6:L206–L334`；`L6:L391–L414`；`L6:L947–L959`；`L6:L1119–L1132`；`L6:L1238–L1258`；`L6:L1293–L1323` | 9 |
| `D5` | [技能书 · 原L45](MS-SKILLS-PLAN.md#orig-l6-l0045) ×1；[技能书 · 原L142](MS-SKILLS-PLAN.md#orig-l6-l0142) ×1；[技能书 · 原L148](MS-SKILLS-PLAN.md#orig-l6-l0148) ×1；[技能书 · 原L206](MS-SKILLS-PLAN.md#orig-l6-l0206) ×2；[物品书 · 原L627](MS-MSITEM-PLAN.md#orig-l6-l0627) ×1；[物品书 · 原L629](MS-MSITEM-PLAN.md#orig-l6-l0629) ×1；[物品书 · 原L637](MS-MSITEM-PLAN.md#orig-l6-l0637) ×3；[技能书 · 原L960](MS-SKILLS-PLAN.md#orig-l6-l0960) ×3；[技能书 · 原L1074](MS-SKILLS-PLAN.md#orig-l6-l1074) ×1；[技能书 · 原L1119](MS-SKILLS-PLAN.md#orig-l6-l1119) ×1；[技能书 · 原L1148](MS-SKILLS-PLAN.md#orig-l6-l1148) ×1；[技能书 · 原L1293](MS-SKILLS-PLAN.md#orig-l6-l1293) ×1 | `L6:L45–L59`；`L6:L142–L147`；`L6:L148–L154`；`L6:L206–L334`；`L6:L627–L628`；`L6:L629–L636`；`L6:L637–L662`；`L6:L960–L981`；`L6:L1074–L1110`；`L6:L1119–L1132`；`L6:L1148–L1185`；`L6:L1293–L1323` | 17 |
| `D6` | [技能书 · 原L45](MS-SKILLS-PLAN.md#orig-l6-l0045) ×1；[技能书 · 原L60](MS-SKILLS-PLAN.md#orig-l6-l0060) ×1；[技能书 · 原L148](MS-SKILLS-PLAN.md#orig-l6-l0148) ×1；[技能书 · 原L206](MS-SKILLS-PLAN.md#orig-l6-l0206) ×3；[技能书 · 原L498](MS-SKILLS-PLAN.md#orig-l6-l0498) ×1；[技能书 · 原L500](MS-SKILLS-PLAN.md#orig-l6-l0500) ×1；[技能书 · 原L547](MS-SKILLS-PLAN.md#orig-l6-l0547) ×1；[技能书 · 原L604](MS-SKILLS-PLAN.md#orig-l6-l0604) ×1；[技能书 · 原L750](MS-SKILLS-PLAN.md#orig-l6-l0750) ×1；[技能书 · 原L960](MS-SKILLS-PLAN.md#orig-l6-l0960) ×1；[技能书 · 原L992](MS-SKILLS-PLAN.md#orig-l6-l0992) ×2；[技能书 · 原L1007](MS-SKILLS-PLAN.md#orig-l6-l1007) ×1；[技能书 · 原L1074](MS-SKILLS-PLAN.md#orig-l6-l1074) ×1；[技能书 · 原L1119](MS-SKILLS-PLAN.md#orig-l6-l1119) ×1；[技能书 · 原L1186](MS-SKILLS-PLAN.md#orig-l6-l1186) ×1；[技能书 · 原L1238](MS-SKILLS-PLAN.md#orig-l6-l1238) ×1；[技能书 · 原L1293](MS-SKILLS-PLAN.md#orig-l6-l1293) ×1；[技能书 · 原L1326](MS-SKILLS-PLAN.md#orig-l6-l1326) ×1 | `L6:L45–L59`；`L6:L60–L88`；`L6:L148–L154`；`L6:L206–L334`；`L6:L498–L499`；`L6:L500–L546`；`L6:L547–L603`；`L6:L604–L618`；`L6:L750–L764`；`L6:L960–L981`；`L6:L992–L1006`；`L6:L1007–L1019`；`L6:L1074–L1110`；`L6:L1119–L1132`；`L6:L1186–L1203`；`L6:L1238–L1258`；`L6:L1293–L1323`；`L6:L1326–L1340` | 21 |
| `D7` | [技能书 · 原L45](MS-SKILLS-PLAN.md#orig-l6-l0045) ×1；[技能书 · 原L60](MS-SKILLS-PLAN.md#orig-l6-l0060) ×1；[技能书 · 原L170](MS-SKILLS-PLAN.md#orig-l6-l0170) ×1；[技能书 · 原L206](MS-SKILLS-PLAN.md#orig-l6-l0206) ×1；[技能书 · 原L1007](MS-SKILLS-PLAN.md#orig-l6-l1007) ×1；[技能书 · 原L1119](MS-SKILLS-PLAN.md#orig-l6-l1119) ×1；[技能书 · 原L1293](MS-SKILLS-PLAN.md#orig-l6-l1293) ×1；[技能书 · 原L1455](MS-SKILLS-PLAN.md#orig-l6-l1455) ×1 | `L6:L45–L59`；`L6:L60–L88`；`L6:L170–L177`；`L6:L206–L334`；`L6:L1007–L1019`；`L6:L1119–L1132`；`L6:L1293–L1323`；`L6:L1455–L1468` | 8 |
| `D8` | [技能书 · 原L45](MS-SKILLS-PLAN.md#orig-l6-l0045) ×1；[技能书 · 原L60](MS-SKILLS-PLAN.md#orig-l6-l0060) ×1；[技能书 · 原L148](MS-SKILLS-PLAN.md#orig-l6-l0148) ×1；[技能书 · 原L500](MS-SKILLS-PLAN.md#orig-l6-l0500) ×1；[技能书 · 原L960](MS-SKILLS-PLAN.md#orig-l6-l0960) ×1；[技能书 · 原L1055](MS-SKILLS-PLAN.md#orig-l6-l1055) ×2；[技能书 · 原L1074](MS-SKILLS-PLAN.md#orig-l6-l1074) ×2；[技能书 · 原L1215](MS-SKILLS-PLAN.md#orig-l6-l1215) ×1；[技能书 · 原L1238](MS-SKILLS-PLAN.md#orig-l6-l1238) ×1；[技能书 · 原L1283](MS-SKILLS-PLAN.md#orig-l6-l1283) ×1；[技能书 · 原L1293](MS-SKILLS-PLAN.md#orig-l6-l1293) ×1 | `L6:L45–L59`；`L6:L60–L88`；`L6:L148–L154`；`L6:L500–L546`；`L6:L960–L981`；`L6:L1055–L1058`；`L6:L1074–L1110`；`L6:L1215–L1237`；`L6:L1238–L1258`；`L6:L1283–L1286`；`L6:L1293–L1323` | 13 |
| `D9` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-l6-l0001) ×1；[技能书 · 原L60](MS-SKILLS-PLAN.md#orig-l6-l0060) ×1；[技能书 · 原L750](MS-SKILLS-PLAN.md#orig-l6-l0750) ×1；[技能书 · 原L1074](MS-SKILLS-PLAN.md#orig-l6-l1074) ×1 | `L6:L1–L29`；`L6:L60–L88`；`L6:L750–L764`；`L6:L1074–L1110` | 4 |
| `D10` | [技能书 · 原L60](MS-SKILLS-PLAN.md#orig-l6-l0060) ×1；[技能书 · 原L391](MS-SKILLS-PLAN.md#orig-l6-l0391) ×1；[技能书 · 原L1074](MS-SKILLS-PLAN.md#orig-l6-l1074) ×1 | `L6:L60–L88`；`L6:L391–L414`；`L6:L1074–L1110` | 3 |
| `D11` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-l6-l0001) ×1；[技能书 · 原L60](MS-SKILLS-PLAN.md#orig-l6-l0060) ×1 | `L6:L1–L29`；`L6:L60–L88` | 2 |
| `D12` | [技能书 · 原L60](MS-SKILLS-PLAN.md#orig-l6-l0060) ×1 | `L6:L60–L88` | 1 |
| `D13` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-l6-l0001) ×3；[技能书 · 原L60](MS-SKILLS-PLAN.md#orig-l6-l0060) ×1；[技能书 · 原L500](MS-SKILLS-PLAN.md#orig-l6-l0500) ×2；[技能书 · 原L604](MS-SKILLS-PLAN.md#orig-l6-l0604) ×1；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-l6-l0767) ×1；[技能书 · 原L834](MS-SKILLS-PLAN.md#orig-l6-l0834) ×1；[技能书 · 原L1148](MS-SKILLS-PLAN.md#orig-l6-l1148) ×1；[技能书 · 原L1215](MS-SKILLS-PLAN.md#orig-l6-l1215) ×1；[技能书 · 原L1238](MS-SKILLS-PLAN.md#orig-l6-l1238) ×1；[技能书 · 原L1326](MS-SKILLS-PLAN.md#orig-l6-l1326) ×1；[技能书 · 原L1443](MS-SKILLS-PLAN.md#orig-l6-l1443) ×2；[技能书 · 原L1455](MS-SKILLS-PLAN.md#orig-l6-l1455) ×1 | `L6:L1–L29`；`L6:L60–L88`；`L6:L500–L546`；`L6:L604–L618`；`L6:L767–L790`；`L6:L834–L846`；`L6:L1148–L1185`；`L6:L1215–L1237`；`L6:L1238–L1258`；`L6:L1326–L1340`；`L6:L1443–L1454`；`L6:L1455–L1468` | 16 |
| `INV-6` | [技能书 · 原L1020](MS-SKILLS-PLAN.md#orig-l6-l1020) ×1；[技能书 · 原L1074](MS-SKILLS-PLAN.md#orig-l6-l1074) ×1；[技能书 · 原L1293](MS-SKILLS-PLAN.md#orig-l6-l1293) ×2；[技能书 · 原L1358](MS-SKILLS-PLAN.md#orig-l6-l1358) ×1；[技能书 · 原L1455](MS-SKILLS-PLAN.md#orig-l6-l1455) ×3 | `L6:L1020–L1041`；`L6:L1074–L1110`；`L6:L1293–L1323`；`L6:L1358–L1367`；`L6:L1455–L1468` | 8 |
| `L5-INV-1` | [技能书 · 原L500](MS-SKILLS-PLAN.md#orig-l6-l0500) ×2；[技能书 · 原L604](MS-SKILLS-PLAN.md#orig-l6-l0604) ×1；[技能书 · 原L1074](MS-SKILLS-PLAN.md#orig-l6-l1074) ×1；[技能书 · 原L1204](MS-SKILLS-PLAN.md#orig-l6-l1204) ×1；[技能书 · 原L1293](MS-SKILLS-PLAN.md#orig-l6-l1293) ×2；[技能书 · 原L1326](MS-SKILLS-PLAN.md#orig-l6-l1326) ×1；[技能书 · 原L1358](MS-SKILLS-PLAN.md#orig-l6-l1358) ×1；[技能书 · 原L1455](MS-SKILLS-PLAN.md#orig-l6-l1455) ×1 | `L6:L500–L546`；`L6:L604–L618`；`L6:L1074–L1110`；`L6:L1204–L1212`；`L6:L1293–L1323`；`L6:L1326–L1340`；`L6:L1358–L1367`；`L6:L1455–L1468` | 10 |
| `L5-INV-2` | [技能书 · 原L500](MS-SKILLS-PLAN.md#orig-l6-l0500) ×1；[技能书 · 原L604](MS-SKILLS-PLAN.md#orig-l6-l0604) ×1；[物品书 · 原L685](MS-MSITEM-PLAN.md#orig-l6-l0685) ×1；[技能书 · 原L1293](MS-SKILLS-PLAN.md#orig-l6-l1293) ×1；[技能书 · 原L1326](MS-SKILLS-PLAN.md#orig-l6-l1326) ×1 | `L6:L500–L546`；`L6:L604–L618`；`L6:L685–L706`；`L6:L1293–L1323`；`L6:L1326–L1340` | 5 |
| `L5-INV-3` | [技能书 · 原L500](MS-SKILLS-PLAN.md#orig-l6-l0500) ×1；[技能书 · 原L604](MS-SKILLS-PLAN.md#orig-l6-l0604) ×1；[技能书 · 原L1326](MS-SKILLS-PLAN.md#orig-l6-l1326) ×1 | `L6:L500–L546`；`L6:L604–L618`；`L6:L1326–L1340` | 3 |
| `L5-INV-4` | [技能书 · 原L184](MS-SKILLS-PLAN.md#orig-l6-l0184) ×1；[防御书 · 原L351](MS-ENTITYDEFENCE-PLAN.md#orig-l6-l0351) ×1；[技能书 · 原L500](MS-SKILLS-PLAN.md#orig-l6-l0500) ×1；[技能书 · 原L604](MS-SKILLS-PLAN.md#orig-l6-l0604) ×1；[技能书 · 原L1119](MS-SKILLS-PLAN.md#orig-l6-l1119) ×1；[技能书 · 原L1293](MS-SKILLS-PLAN.md#orig-l6-l1293) ×1；[技能书 · 原L1326](MS-SKILLS-PLAN.md#orig-l6-l1326) ×1 | `L6:L184–L195`；`L6:L351–L358`；`L6:L500–L546`；`L6:L604–L618`；`L6:L1119–L1132`；`L6:L1293–L1323`；`L6:L1326–L1340` | 7 |
| `L5-INV-5` | [技能书 · 原L500](MS-SKILLS-PLAN.md#orig-l6-l0500) ×1；[技能书 · 原L604](MS-SKILLS-PLAN.md#orig-l6-l0604) ×1；[技能书 · 原L1326](MS-SKILLS-PLAN.md#orig-l6-l1326) ×1 | `L6:L500–L546`；`L6:L604–L618`；`L6:L1326–L1340` | 3 |
| `L5-INV-6` | [技能书 · 原L500](MS-SKILLS-PLAN.md#orig-l6-l0500) ×2；[技能书 · 原L604](MS-SKILLS-PLAN.md#orig-l6-l0604) ×1；[技能书 · 原L1204](MS-SKILLS-PLAN.md#orig-l6-l1204) ×2；[技能书 · 原L1293](MS-SKILLS-PLAN.md#orig-l6-l1293) ×1；[技能书 · 原L1326](MS-SKILLS-PLAN.md#orig-l6-l1326) ×1；[技能书 · 原L1455](MS-SKILLS-PLAN.md#orig-l6-l1455) ×1 | `L6:L500–L546`；`L6:L604–L618`；`L6:L1204–L1212`；`L6:L1293–L1323`；`L6:L1326–L1340`；`L6:L1455–L1468` | 8 |
| `L5-INV-7` | [技能书 · 原L500](MS-SKILLS-PLAN.md#orig-l6-l0500) ×2；[技能书 · 原L604](MS-SKILLS-PLAN.md#orig-l6-l0604) ×1；[技能书 · 原L1204](MS-SKILLS-PLAN.md#orig-l6-l1204) ×1；[技能书 · 原L1326](MS-SKILLS-PLAN.md#orig-l6-l1326) ×1；[技能书 · 原L1455](MS-SKILLS-PLAN.md#orig-l6-l1455) ×1 | `L6:L500–L546`；`L6:L604–L618`；`L6:L1204–L1212`；`L6:L1326–L1340`；`L6:L1455–L1468` | 6 |
| `L5-INV-8` | [技能书 · 原L500](MS-SKILLS-PLAN.md#orig-l6-l0500) ×2；[技能书 · 原L604](MS-SKILLS-PLAN.md#orig-l6-l0604) ×1；[技能书 · 原L1204](MS-SKILLS-PLAN.md#orig-l6-l1204) ×1；[技能书 · 原L1326](MS-SKILLS-PLAN.md#orig-l6-l1326) ×1；[技能书 · 原L1455](MS-SKILLS-PLAN.md#orig-l6-l1455) ×1 | `L6:L500–L546`；`L6:L604–L618`；`L6:L1204–L1212`；`L6:L1326–L1340`；`L6:L1455–L1468` | 6 |
| `L5-INV-9` | [技能书 · 原L500](MS-SKILLS-PLAN.md#orig-l6-l0500) ×2；[技能书 · 原L604](MS-SKILLS-PLAN.md#orig-l6-l0604) ×1；[技能书 · 原L750](MS-SKILLS-PLAN.md#orig-l6-l0750) ×1；[技能书 · 原L1074](MS-SKILLS-PLAN.md#orig-l6-l1074) ×1；[技能书 · 原L1148](MS-SKILLS-PLAN.md#orig-l6-l1148) ×1；[技能书 · 原L1186](MS-SKILLS-PLAN.md#orig-l6-l1186) ×1；[技能书 · 原L1204](MS-SKILLS-PLAN.md#orig-l6-l1204) ×1；[技能书 · 原L1215](MS-SKILLS-PLAN.md#orig-l6-l1215) ×1；[技能书 · 原L1293](MS-SKILLS-PLAN.md#orig-l6-l1293) ×2；[技能书 · 原L1326](MS-SKILLS-PLAN.md#orig-l6-l1326) ×1；[技能书 · 原L1455](MS-SKILLS-PLAN.md#orig-l6-l1455) ×2 | `L6:L500–L546`；`L6:L604–L618`；`L6:L750–L764`；`L6:L1074–L1110`；`L6:L1148–L1185`；`L6:L1186–L1203`；`L6:L1204–L1212`；`L6:L1215–L1237`；`L6:L1293–L1323`；`L6:L1326–L1340`；`L6:L1455–L1468` | 14 |
| `L6-INV-1` | [技能书 · 原L859](MS-SKILLS-PLAN.md#orig-l6-l0859) ×1；[技能书 · 原L1341](MS-SKILLS-PLAN.md#orig-l6-l1341) ×1 | `L6:L859–L875`；`L6:L1341–L1357` | 2 |
| `L6-INV-2` | [技能书 · 原L859](MS-SKILLS-PLAN.md#orig-l6-l0859) ×1；[技能书 · 原L1341](MS-SKILLS-PLAN.md#orig-l6-l1341) ×1 | `L6:L859–L875`；`L6:L1341–L1357` | 2 |
| `L6-INV-3` | [技能书 · 原L859](MS-SKILLS-PLAN.md#orig-l6-l0859) ×1；[技能书 · 原L1293](MS-SKILLS-PLAN.md#orig-l6-l1293) ×2；[技能书 · 原L1341](MS-SKILLS-PLAN.md#orig-l6-l1341) ×1 | `L6:L859–L875`；`L6:L1293–L1323`；`L6:L1341–L1357` | 4 |
| `L6-INV-4` | [技能书 · 原L859](MS-SKILLS-PLAN.md#orig-l6-l0859) ×1；[技能书 · 原L1341](MS-SKILLS-PLAN.md#orig-l6-l1341) ×1 | `L6:L859–L875`；`L6:L1341–L1357` | 2 |
| `L6-INV-5` | [技能书 · 原L859](MS-SKILLS-PLAN.md#orig-l6-l0859) ×1；[技能书 · 原L1341](MS-SKILLS-PLAN.md#orig-l6-l1341) ×1 | `L6:L859–L875`；`L6:L1341–L1357` | 2 |
| `L6-INV-6` | [技能书 · 原L859](MS-SKILLS-PLAN.md#orig-l6-l0859) ×1；[技能书 · 原L1341](MS-SKILLS-PLAN.md#orig-l6-l1341) ×1 | `L6:L859–L875`；`L6:L1341–L1357` | 2 |
| `L6-INV-7` | [技能书 · 原L859](MS-SKILLS-PLAN.md#orig-l6-l0859) ×1；[技能书 · 原L1341](MS-SKILLS-PLAN.md#orig-l6-l1341) ×1 | `L6:L859–L875`；`L6:L1341–L1357` | 2 |
| `L6-INV-8` | [技能书 · 原L859](MS-SKILLS-PLAN.md#orig-l6-l0859) ×1；[技能书 · 原L1341](MS-SKILLS-PLAN.md#orig-l6-l1341) ×1 | `L6:L859–L875`；`L6:L1341–L1357` | 2 |
| `L6-INV-9` | [技能书 · 原L859](MS-SKILLS-PLAN.md#orig-l6-l0859) ×1；[技能书 · 原L1341](MS-SKILLS-PLAN.md#orig-l6-l1341) ×1 | `L6:L859–L875`；`L6:L1341–L1357` | 2 |
| `L6-INV-10` | [技能书 · 原L859](MS-SKILLS-PLAN.md#orig-l6-l0859) ×1；[技能书 · 原L1186](MS-SKILLS-PLAN.md#orig-l6-l1186) ×1；[技能书 · 原L1215](MS-SKILLS-PLAN.md#orig-l6-l1215) ×1；[技能书 · 原L1341](MS-SKILLS-PLAN.md#orig-l6-l1341) ×1；[技能书 · 原L1430](MS-SKILLS-PLAN.md#orig-l6-l1430) ×1 | `L6:L859–L875`；`L6:L1186–L1203`；`L6:L1215–L1237`；`L6:L1341–L1357`；`L6:L1430–L1442` | 5 |
| `PROC-INV-1` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-l6-l0001) ×2；[技能书 · 原L60](MS-SKILLS-PLAN.md#orig-l6-l0060) ×2；[技能书 · 原L206](MS-SKILLS-PLAN.md#orig-l6-l0206) ×1；[技能书 · 原L500](MS-SKILLS-PLAN.md#orig-l6-l0500) ×3；[技能书 · 原L604](MS-SKILLS-PLAN.md#orig-l6-l0604) ×1；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-l6-l0767) ×1；[技能书 · 原L859](MS-SKILLS-PLAN.md#orig-l6-l0859) ×1；[技能书 · 原L1074](MS-SKILLS-PLAN.md#orig-l6-l1074) ×1；[技能书 · 原L1204](MS-SKILLS-PLAN.md#orig-l6-l1204) ×3；[技能书 · 原L1293](MS-SKILLS-PLAN.md#orig-l6-l1293) ×1；[技能书 · 原L1326](MS-SKILLS-PLAN.md#orig-l6-l1326) ×1；[技能书 · 原L1341](MS-SKILLS-PLAN.md#orig-l6-l1341) ×1；[技能书 · 原L1358](MS-SKILLS-PLAN.md#orig-l6-l1358) ×1；[技能书 · 原L1430](MS-SKILLS-PLAN.md#orig-l6-l1430) ×1；[技能书 · 原L1443](MS-SKILLS-PLAN.md#orig-l6-l1443) ×2；[技能书 · 原L1455](MS-SKILLS-PLAN.md#orig-l6-l1455) ×2 | `L6:L1–L29`；`L6:L60–L88`；`L6:L206–L334`；`L6:L500–L546`；`L6:L604–L618`；`L6:L767–L790`；`L6:L859–L875`；`L6:L1074–L1110`；`L6:L1204–L1212`；`L6:L1293–L1323`；`L6:L1326–L1340`；`L6:L1341–L1357`；`L6:L1358–L1367`；`L6:L1430–L1442`；`L6:L1443–L1454`；`L6:L1455–L1468` | 24 |
| `R1` | [技能书 · 原L206](MS-SKILLS-PLAN.md#orig-l6-l0206) ×1 | `L6:L206–L334` | 1 |

<a id="ids-road"></a>
### ROAD

原来源：`docs/plans/Ronova_L6_L6E_Full_Enhancement_Roadmap_V1.0_Internal_Root.md`。[来源映射](RESTRUCTURE-MAP.md#source-road) · [原字节快照](../archive/plans-before-ms-restructure-r66/Ronova_L6_L6E_Full_Enhancement_Roadmap_V1.0_Internal_Root.md) · [返回来源目录](#source-directory)。

| 原 ID | 新书锚点（×本段次数） | 原来源行范围 | 总次数 |
|---|---|---|---:|
| `D1` | [技能书 · 原L26](MS-SKILLS-PLAN.md#orig-road-l0026) ×1 | `ROAD:L26–L42` | 1 |
| `D13` | [技能书 · 原L26](MS-SKILLS-PLAN.md#orig-road-l0026) ×1；[技能书 · 原L80](MS-SKILLS-PLAN.md#orig-road-l0080) ×1；[技能书 · 原L366](MS-SKILLS-PLAN.md#orig-road-l0366) ×1 | `ROAD:L26–L42`；`ROAD:L80–L90`；`ROAD:L366–L383` | 3 |

<a id="ids-msc"></a>
### MSC

原来源：`docs/plans/Ronova_MS_Compliance_and_Enhancement_Plan_V1.0.md`。[来源映射](RESTRUCTURE-MAP.md#source-msc) · [原字节快照](../archive/plans-before-ms-restructure-r66/Ronova_MS_Compliance_and_Enhancement_Plan_V1.0.md) · [返回来源目录](#source-directory)。

| 原 ID | 新书锚点（×本段次数） | 原来源行范围 | 总次数 |
|---|---|---|---:|
| `D8` | [技能书 · 原L212](MS-SKILLS-PLAN.md#orig-msc-l0212) ×1 | `MSC:L212–L242` | 1 |
| `D91` | [技能书 · 原L22](MS-SKILLS-PLAN.md#orig-msc-l0022) ×1 | `MSC:L22–L34` | 1 |
| `R4` | [技能书 · 原L306](MS-SKILLS-PLAN.md#orig-msc-l0306) ×1 | `MSC:L306–L318` | 1 |
| `R-A1` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-msc-l0001) ×1；[技能书 · 原L39](MS-SKILLS-PLAN.md#orig-msc-l0039) ×1 | `MSC:L1–L21`；`MSC:L39–L49` | 2 |
| `R-A2` | [技能书 · 原L39](MS-SKILLS-PLAN.md#orig-msc-l0039) ×1 | `MSC:L39–L49` | 1 |
| `R-A3` | [技能书 · 原L39](MS-SKILLS-PLAN.md#orig-msc-l0039) ×1 | `MSC:L39–L49` | 1 |
| `R-A4` | [技能书 · 原L39](MS-SKILLS-PLAN.md#orig-msc-l0039) ×1 | `MSC:L39–L49` | 1 |
| `R-A5` | [技能书 · 原L39](MS-SKILLS-PLAN.md#orig-msc-l0039) ×1；[技能书 · 原L193](MS-SKILLS-PLAN.md#orig-msc-l0193) ×1 | `MSC:L39–L49`；`MSC:L193–L209` | 2 |
| `R-A6` | [技能书 · 原L39](MS-SKILLS-PLAN.md#orig-msc-l0039) ×1 | `MSC:L39–L49` | 1 |
| `R-B1` | [物品书 · 原L50](MS-MSITEM-PLAN.md#orig-msc-l0050) ×1；[物品书 · 原L258](MS-MSITEM-PLAN.md#orig-msc-l0258) ×1 | `MSC:L50–L58`；`MSC:L258–L268` | 2 |
| `R-B2` | [物品书 · 原L50](MS-MSITEM-PLAN.md#orig-msc-l0050) ×1；[物品书 · 原L258](MS-MSITEM-PLAN.md#orig-msc-l0258) ×1 | `MSC:L50–L58`；`MSC:L258–L268` | 2 |
| `R-B3` | [物品书 · 原L50](MS-MSITEM-PLAN.md#orig-msc-l0050) ×1 | `MSC:L50–L58` | 1 |
| `R-B4` | [物品书 · 原L50](MS-MSITEM-PLAN.md#orig-msc-l0050) ×1 | `MSC:L50–L58` | 1 |
| `R-C1` | [物品书 · 原L59](MS-MSITEM-PLAN.md#orig-msc-l0059) ×1 | `MSC:L59–L68` | 1 |
| `R-C2` | [物品书 · 原L59](MS-MSITEM-PLAN.md#orig-msc-l0059) ×1 | `MSC:L59–L68` | 1 |
| `R-C3` | [物品书 · 原L59](MS-MSITEM-PLAN.md#orig-msc-l0059) ×1 | `MSC:L59–L68` | 1 |
| `R-C4` | [物品书 · 原L59](MS-MSITEM-PLAN.md#orig-msc-l0059) ×1 | `MSC:L59–L68` | 1 |
| `R-C5` | [物品书 · 原L59](MS-MSITEM-PLAN.md#orig-msc-l0059) ×1 | `MSC:L59–L68` | 1 |
| `R-D1` | [物品书 · 原L69](MS-MSITEM-PLAN.md#orig-msc-l0069) ×1 | `MSC:L69–L79` | 1 |
| `R-D2` | [物品书 · 原L69](MS-MSITEM-PLAN.md#orig-msc-l0069) ×1 | `MSC:L69–L79` | 1 |
| `R-D3` | [物品书 · 原L69](MS-MSITEM-PLAN.md#orig-msc-l0069) ×1 | `MSC:L69–L79` | 1 |
| `R-D4` | [物品书 · 原L69](MS-MSITEM-PLAN.md#orig-msc-l0069) ×1 | `MSC:L69–L79` | 1 |
| `R-D5` | [物品书 · 原L69](MS-MSITEM-PLAN.md#orig-msc-l0069) ×1；[技能书 · 原L360](MS-SKILLS-PLAN.md#orig-msc-l0360) ×1 | `MSC:L69–L79`；`MSC:L360–L374` | 2 |
| `R-D6` | [物品书 · 原L69](MS-MSITEM-PLAN.md#orig-msc-l0069) ×1 | `MSC:L69–L79` | 1 |
| `R-E1` | [技能书 · 原L80](MS-SKILLS-PLAN.md#orig-msc-l0080) ×1 | `MSC:L80–L88` | 1 |
| `R-E2` | [技能书 · 原L80](MS-SKILLS-PLAN.md#orig-msc-l0080) ×1 | `MSC:L80–L88` | 1 |
| `R-E3` | [技能书 · 原L80](MS-SKILLS-PLAN.md#orig-msc-l0080) ×1 | `MSC:L80–L88` | 1 |
| `R-E4` | [技能书 · 原L80](MS-SKILLS-PLAN.md#orig-msc-l0080) ×1 | `MSC:L80–L88` | 1 |
| `R-F1` | [技能书 · 原L89](MS-SKILLS-PLAN.md#orig-msc-l0089) ×1 | `MSC:L89–L96` | 1 |
| `R-F2` | [技能书 · 原L89](MS-SKILLS-PLAN.md#orig-msc-l0089) ×1 | `MSC:L89–L96` | 1 |
| `R-F3` | [技能书 · 原L89](MS-SKILLS-PLAN.md#orig-msc-l0089) ×1 | `MSC:L89–L96` | 1 |
| `R-G1` | [技能书 · 原L97](MS-SKILLS-PLAN.md#orig-msc-l0097) ×1；[技能书 · 原L193](MS-SKILLS-PLAN.md#orig-msc-l0193) ×1；[技能书 · 原L294](MS-SKILLS-PLAN.md#orig-msc-l0294) ×1 | `MSC:L97–L107`；`MSC:L193–L209`；`MSC:L294–L305` | 3 |
| `R-G2` | [技能书 · 原L97](MS-SKILLS-PLAN.md#orig-msc-l0097) ×1 | `MSC:L97–L107` | 1 |
| `R-G3` | [技能书 · 原L97](MS-SKILLS-PLAN.md#orig-msc-l0097) ×1；[技能书 · 原L294](MS-SKILLS-PLAN.md#orig-msc-l0294) ×1 | `MSC:L97–L107`；`MSC:L294–L305` | 2 |
| `R-G4` | [技能书 · 原L97](MS-SKILLS-PLAN.md#orig-msc-l0097) ×1 | `MSC:L97–L107` | 1 |
| `R-G5` | [技能书 · 原L97](MS-SKILLS-PLAN.md#orig-msc-l0097) ×1 | `MSC:L97–L107` | 1 |
| `R-G6` | [技能书 · 原L97](MS-SKILLS-PLAN.md#orig-msc-l0097) ×1；[技能书 · 原L294](MS-SKILLS-PLAN.md#orig-msc-l0294) ×1 | `MSC:L97–L107`；`MSC:L294–L305` | 2 |
| `R-H1` | [防御书 · 原L108](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0108) ×1；[防御书 · 原L269](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0269) ×1 | `MSC:L108–L118`；`MSC:L269–L279` | 2 |
| `R-H2` | [防御书 · 原L108](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0108) ×1；[技能书 · 原L193](MS-SKILLS-PLAN.md#orig-msc-l0193) ×1 | `MSC:L108–L118`；`MSC:L193–L209` | 2 |
| `R-H3` | [防御书 · 原L108](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0108) ×1 | `MSC:L108–L118` | 1 |
| `R-H4` | [防御书 · 原L108](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0108) ×1 | `MSC:L108–L118` | 1 |
| `R-I1` | [防御书 · 原L108](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0108) ×1；[防御书 · 原L269](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0269) ×1 | `MSC:L108–L118`；`MSC:L269–L279` | 2 |
| `R-I2` | [防御书 · 原L108](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0108) ×1 | `MSC:L108–L118` | 1 |
| `R-J1` | [防御书 · 原L119](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119) ×1；[防御书 · 原L280](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0280) ×1 | `MSC:L119–L131`；`MSC:L280–L293` | 2 |
| `R-J2` | [防御书 · 原L119](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119) ×1 | `MSC:L119–L131` | 1 |
| `R-J3` | [防御书 · 原L119](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119) ×1 | `MSC:L119–L131` | 1 |
| `R-J4` | [防御书 · 原L119](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119) ×1 | `MSC:L119–L131` | 1 |
| `R-J5` | [防御书 · 原L119](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119) ×1；[技能书 · 原L360](MS-SKILLS-PLAN.md#orig-msc-l0360) ×1 | `MSC:L119–L131`；`MSC:L360–L374` | 2 |
| `R-J6` | [防御书 · 原L119](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119) ×1 | `MSC:L119–L131` | 1 |
| `R-K1` | [防御书 · 原L119](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119) ×1 | `MSC:L119–L131` | 1 |
| `R-K2` | [防御书 · 原L119](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0119) ×1 | `MSC:L119–L131` | 1 |
| `R-L1` | [防御书 · 原L132](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0132) ×1；[防御书 · 原L280](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0280) ×1 | `MSC:L132–L144`；`MSC:L280–L293` | 2 |
| `R-L2` | [防御书 · 原L132](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0132) ×1 | `MSC:L132–L144` | 1 |
| `R-M1` | [防御书 · 原L132](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0132) ×1 | `MSC:L132–L144` | 1 |
| `R-M2` | [防御书 · 原L132](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0132) ×1 | `MSC:L132–L144` | 1 |
| `R-M3` | [防御书 · 原L132](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0132) ×1 | `MSC:L132–L144` | 1 |
| `R-M4` | [防御书 · 原L132](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0132) ×1；[技能书 · 原L193](MS-SKILLS-PLAN.md#orig-msc-l0193) ×1；[防御书 · 原L280](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0280) ×1 | `MSC:L132–L144`；`MSC:L193–L209`；`MSC:L280–L293` | 3 |
| `R-M5` | [防御书 · 原L132](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0132) ×1 | `MSC:L132–L144` | 1 |
| `R-M6` | [防御书 · 原L132](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0132) ×1 | `MSC:L132–L144` | 1 |
| `R-N1` | [防御书 · 原L145](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0145) ×1 | `MSC:L145–L152` | 1 |
| `R-N2` | [防御书 · 原L145](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0145) ×1 | `MSC:L145–L152` | 1 |
| `R-N3` | [防御书 · 原L145](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0145) ×1 | `MSC:L145–L152` | 1 |
| `R-O1` | [防御书 · 原L153](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0153) ×1；[技能书 · 原L193](MS-SKILLS-PLAN.md#orig-msc-l0193) ×1 | `MSC:L153–L160`；`MSC:L193–L209` | 2 |
| `R-O2` | [防御书 · 原L153](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0153) ×1 | `MSC:L153–L160` | 1 |
| `R-O3` | [防御书 · 原L153](MS-ENTITYDEFENCE-PLAN.md#orig-msc-l0153) ×1 | `MSC:L153–L160` | 1 |
| `R-P1` | [技能书 · 原L161](MS-SKILLS-PLAN.md#orig-msc-l0161) ×1 | `MSC:L161–L176` | 1 |
| `R-P2` | [技能书 · 原L161](MS-SKILLS-PLAN.md#orig-msc-l0161) ×1 | `MSC:L161–L176` | 1 |
| `R-P3` | [技能书 · 原L161](MS-SKILLS-PLAN.md#orig-msc-l0161) ×1 | `MSC:L161–L176` | 1 |
| `R-P4` | [技能书 · 原L161](MS-SKILLS-PLAN.md#orig-msc-l0161) ×1 | `MSC:L161–L176` | 1 |
| `R-P5` | [技能书 · 原L161](MS-SKILLS-PLAN.md#orig-msc-l0161) ×1；[技能书 · 原L306](MS-SKILLS-PLAN.md#orig-msc-l0306) ×1 | `MSC:L161–L176`；`MSC:L306–L318` | 2 |
| `R-P6` | [技能书 · 原L161](MS-SKILLS-PLAN.md#orig-msc-l0161) ×1 | `MSC:L161–L176` | 1 |
| `R-Q` | [技能书 · 原L306](MS-SKILLS-PLAN.md#orig-msc-l0306) ×1 | `MSC:L306–L318` | 1 |
| `R-Q1` | [技能书 · 原L161](MS-SKILLS-PLAN.md#orig-msc-l0161) ×1 | `MSC:L161–L176` | 1 |
| `R-Q2` | [技能书 · 原L161](MS-SKILLS-PLAN.md#orig-msc-l0161) ×1；[技能书 · 原L193](MS-SKILLS-PLAN.md#orig-msc-l0193) ×1；[技能书 · 原L360](MS-SKILLS-PLAN.md#orig-msc-l0360) ×1 | `MSC:L161–L176`；`MSC:L193–L209`；`MSC:L360–L374` | 3 |
| `R-Q3` | [技能书 · 原L161](MS-SKILLS-PLAN.md#orig-msc-l0161) ×1 | `MSC:L161–L176` | 1 |
| `R-Q4` | [技能书 · 原L161](MS-SKILLS-PLAN.md#orig-msc-l0161) ×1 | `MSC:L161–L176` | 1 |
| `R-Q5` | [技能书 · 原L161](MS-SKILLS-PLAN.md#orig-msc-l0161) ×1；[技能书 · 原L193](MS-SKILLS-PLAN.md#orig-msc-l0193) ×1 | `MSC:L161–L176`；`MSC:L193–L209` | 2 |
| `R-R1` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-msc-l0177) ×1 | `MSC:L177–L192` | 1 |
| `R-R2` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-msc-l0177) ×1 | `MSC:L177–L192` | 1 |
| `R-R3` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-msc-l0177) ×1；[技能书 · 原L306](MS-SKILLS-PLAN.md#orig-msc-l0306) ×1 | `MSC:L177–L192`；`MSC:L306–L318` | 2 |
| `R-R4` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-msc-l0177) ×1 | `MSC:L177–L192` | 1 |
| `R-R5` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-msc-l0177) ×1 | `MSC:L177–L192` | 1 |
| `R-S1` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-msc-l0177) ×1；[技能书 · 原L360](MS-SKILLS-PLAN.md#orig-msc-l0360) ×1 | `MSC:L177–L192`；`MSC:L360–L374` | 2 |
| `R-S2` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-msc-l0177) ×1；[技能书 · 原L193](MS-SKILLS-PLAN.md#orig-msc-l0193) ×1；[技能书 · 原L243](MS-SKILLS-PLAN.md#orig-msc-l0243) ×1 | `MSC:L177–L192`；`MSC:L193–L209`；`MSC:L243–L257` | 3 |
| `R-S3` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-msc-l0177) ×1 | `MSC:L177–L192` | 1 |
| `R-S4` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-msc-l0001) ×1；[技能书 · 原L177](MS-SKILLS-PLAN.md#orig-msc-l0177) ×1 | `MSC:L1–L21`；`MSC:L177–L192` | 2 |

<a id="ids-msr"></a>
### MSR

原来源：`docs/plans/Ronova_MS_Remediation_Spec_V1.0.md`。[来源映射](RESTRUCTURE-MAP.md#source-msr) · [原字节快照](../archive/plans-before-ms-restructure-r66/Ronova_MS_Remediation_Spec_V1.0.md) · [返回来源目录](#source-directory)。

| 原 ID | 新书锚点（×本段次数） | 原来源行范围 | 总次数 |
|---|---|---|---:|
| `R17` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-msr-l0001) ×1；[技能书 · 原L94](MS-SKILLS-PLAN.md#orig-msr-l0094) ×1 | `MSR:L1–L36`；`MSR:L94–L95` | 2 |
| `R22` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-msr-l0001) ×1；[技能书 · 原L37](MS-SKILLS-PLAN.md#orig-msr-l0037) ×1 | `MSR:L1–L36`；`MSR:L37–L38` | 2 |
| `R26` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-msr-l0001) ×1；[技能书 · 原L302](MS-SKILLS-PLAN.md#orig-msr-l0302) ×1 | `MSR:L1–L36`；`MSR:L302–L303` | 2 |
| `R31` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-msr-l0001) ×1；[技能书 · 原L234](MS-SKILLS-PLAN.md#orig-msr-l0234) ×1 | `MSR:L1–L36`；`MSR:L234–L235` | 2 |
| `R57` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-msr-l0001) ×1；[技能书 · 原L153](MS-SKILLS-PLAN.md#orig-msr-l0153) ×1 | `MSR:L1–L36`；`MSR:L153–L154` | 2 |
| `R58` | [技能书 · 原L1](MS-SKILLS-PLAN.md#orig-msr-l0001) ×2；[技能书 · 原L346](MS-SKILLS-PLAN.md#orig-msr-l0346) ×1 | `MSR:L1–L36`；`MSR:L346–L347` | 3 |

<a id="ids-rule"></a>
### RULE

原来源：`docs/plans/Ronova_MS_Rulebound_Enhancement_Plan_V2.0.md`。[来源映射](RESTRUCTURE-MAP.md#source-rule) · [原字节快照](../archive/plans-before-ms-restructure-r66/Ronova_MS_Rulebound_Enhancement_Plan_V2.0.md) · [返回来源目录](#source-directory)。

| 原 ID | 新书锚点（×本段次数） | 原来源行范围 | 总次数 |
|---|---|---|---:|
| `A1` | [技能书 · 原L734](MS-SKILLS-PLAN.md#orig-rule-l0734) ×3；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-rule-l0767) ×1；[技能书 · 原L845](MS-SKILLS-PLAN.md#orig-rule-l0845) ×1；[技能书 · 原L869](MS-SKILLS-PLAN.md#orig-rule-l0869) ×1；[技能书 · 原L926](MS-SKILLS-PLAN.md#orig-rule-l0926) ×1 | `RULE:L734–L747`；`RULE:L767–L797`；`RULE:L845–L868`；`RULE:L869–L911`；`RULE:L926–L948` | 7 |
| `A2` | [技能书 · 原L734](MS-SKILLS-PLAN.md#orig-rule-l0734) ×1 | `RULE:L734–L747` | 1 |
| `A3` | [技能书 · 原L734](MS-SKILLS-PLAN.md#orig-rule-l0734) ×1 | `RULE:L734–L747` | 1 |
| `A4` | [技能书 · 原L734](MS-SKILLS-PLAN.md#orig-rule-l0734) ×2；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-rule-l0767) ×1；[技能书 · 原L845](MS-SKILLS-PLAN.md#orig-rule-l0845) ×1；[技能书 · 原L869](MS-SKILLS-PLAN.md#orig-rule-l0869) ×1；[技能书 · 原L926](MS-SKILLS-PLAN.md#orig-rule-l0926) ×1 | `RULE:L734–L747`；`RULE:L767–L797`；`RULE:L845–L868`；`RULE:L869–L911`；`RULE:L926–L948` | 6 |
| `A5` | [技能书 · 原L734](MS-SKILLS-PLAN.md#orig-rule-l0734) ×1 | `RULE:L734–L747` | 1 |
| `D1` | [物品书 · 原L798](MS-MSITEM-PLAN.md#orig-rule-l0798) ×1 | `RULE:L798–L806` | 1 |
| `D2` | [物品书 · 原L798](MS-MSITEM-PLAN.md#orig-rule-l0798) ×1 | `RULE:L798–L806` | 1 |
| `D3` | [物品书 · 原L798](MS-MSITEM-PLAN.md#orig-rule-l0798) ×1 | `RULE:L798–L806` | 1 |
| `D4` | [物品书 · 原L798](MS-MSITEM-PLAN.md#orig-rule-l0798) ×1 | `RULE:L798–L806` | 1 |
| `R1` | [技能书 · 原L47](MS-SKILLS-PLAN.md#orig-rule-l0047) ×1；[技能书 · 原L98](MS-SKILLS-PLAN.md#orig-rule-l0098) ×4；[技能书 · 原L297](MS-SKILLS-PLAN.md#orig-rule-l0297) ×2；[技能书 · 原L345](MS-SKILLS-PLAN.md#orig-rule-l0345) ×1；[技能书 · 原L353](MS-SKILLS-PLAN.md#orig-rule-l0353) ×2；[技能书 · 原L559](MS-SKILLS-PLAN.md#orig-rule-l0559) ×5；[技能书 · 原L698](MS-SKILLS-PLAN.md#orig-rule-l0698) ×2；[技能书 · 原L716](MS-SKILLS-PLAN.md#orig-rule-l0716) ×1；[技能书 · 原L912](MS-SKILLS-PLAN.md#orig-rule-l0912) ×1；[技能书 · 原L917](MS-SKILLS-PLAN.md#orig-rule-l0917) ×1；[技能书 · 原L978](MS-SKILLS-PLAN.md#orig-rule-l0978) ×2；[技能书 · 原L1008](MS-SKILLS-PLAN.md#orig-rule-l1008) ×1 | `RULE:L47–L53`；`RULE:L98–L111`；`RULE:L297–L316`；`RULE:L345–L352`；`RULE:L353–L389`；`RULE:L559–L610`；`RULE:L698–L715`；`RULE:L716–L729`；`RULE:L912–L916`；`RULE:L917–L925`；`RULE:L978–L986`；`RULE:L1008–L1026` | 23 |
| `R2` | [技能书 · 原L98](MS-SKILLS-PLAN.md#orig-rule-l0098) ×1；[技能书 · 原L112](MS-SKILLS-PLAN.md#orig-rule-l0112) ×1；[技能书 · 原L353](MS-SKILLS-PLAN.md#orig-rule-l0353) ×2；[技能书 · 原L559](MS-SKILLS-PLAN.md#orig-rule-l0559) ×2；[技能书 · 原L698](MS-SKILLS-PLAN.md#orig-rule-l0698) ×1；[技能书 · 原L716](MS-SKILLS-PLAN.md#orig-rule-l0716) ×1；[技能书 · 原L917](MS-SKILLS-PLAN.md#orig-rule-l0917) ×1；[技能书 · 原L926](MS-SKILLS-PLAN.md#orig-rule-l0926) ×1；[技能书 · 原L978](MS-SKILLS-PLAN.md#orig-rule-l0978) ×1 | `RULE:L98–L111`；`RULE:L112–L132`；`RULE:L353–L389`；`RULE:L559–L610`；`RULE:L698–L715`；`RULE:L716–L729`；`RULE:L917–L925`；`RULE:L926–L948`；`RULE:L978–L986` | 11 |
| `R3` | [技能书 · 原L98](MS-SKILLS-PLAN.md#orig-rule-l0098) ×1；[技能书 · 原L297](MS-SKILLS-PLAN.md#orig-rule-l0297) ×1；[技能书 · 原L353](MS-SKILLS-PLAN.md#orig-rule-l0353) ×2；[技能书 · 原L698](MS-SKILLS-PLAN.md#orig-rule-l0698) ×2；[技能书 · 原L716](MS-SKILLS-PLAN.md#orig-rule-l0716) ×1；[技能书 · 原L978](MS-SKILLS-PLAN.md#orig-rule-l0978) ×1 | `RULE:L98–L111`；`RULE:L297–L316`；`RULE:L353–L389`；`RULE:L698–L715`；`RULE:L716–L729`；`RULE:L978–L986` | 8 |
| `R4` | [技能书 · 原L98](MS-SKILLS-PLAN.md#orig-rule-l0098) ×1；[技能书 · 原L1008](MS-SKILLS-PLAN.md#orig-rule-l1008) ×1 | `RULE:L98–L111`；`RULE:L1008–L1026` | 2 |
| `R5` | [物品书 · 原L133](MS-MSITEM-PLAN.md#orig-rule-l0133) ×1；[技能书 · 原L611](MS-SKILLS-PLAN.md#orig-rule-l0611) ×3；[技能书 · 原L683](MS-SKILLS-PLAN.md#orig-rule-l0683) ×1；[技能书 · 原L698](MS-SKILLS-PLAN.md#orig-rule-l0698) ×1；[技能书 · 原L716](MS-SKILLS-PLAN.md#orig-rule-l0716) ×1；[技能书 · 原L807](MS-SKILLS-PLAN.md#orig-rule-l0807) ×1；[技能书 · 原L1008](MS-SKILLS-PLAN.md#orig-rule-l1008) ×1 | `RULE:L133–L140`；`RULE:L611–L635`；`RULE:L683–L697`；`RULE:L698–L715`；`RULE:L716–L729`；`RULE:L807–L815`；`RULE:L1008–L1026` | 9 |
| `R6` | [物品书 · 原L133](MS-MSITEM-PLAN.md#orig-rule-l0133) ×1 | `RULE:L133–L140` | 1 |
| `R7` | [物品书 · 原L133](MS-MSITEM-PLAN.md#orig-rule-l0133) ×1 | `RULE:L133–L140` | 1 |
| `R8` | [技能书 · 原L141](MS-SKILLS-PLAN.md#orig-rule-l0141) ×1；[技能书 · 原L267](MS-SKILLS-PLAN.md#orig-rule-l0267) ×2；[技能书 · 原L652](MS-SKILLS-PLAN.md#orig-rule-l0652) ×1 | `RULE:L141–L158`；`RULE:L267–L296`；`RULE:L652–L682` | 4 |
| `R9` | [技能书 · 原L141](MS-SKILLS-PLAN.md#orig-rule-l0141) ×1；[技能书 · 原L267](MS-SKILLS-PLAN.md#orig-rule-l0267) ×1 | `RULE:L141–L158`；`RULE:L267–L296` | 2 |
| `R10` | [技能书 · 原L141](MS-SKILLS-PLAN.md#orig-rule-l0141) ×1；[技能书 · 原L267](MS-SKILLS-PLAN.md#orig-rule-l0267) ×3；[技能书 · 原L845](MS-SKILLS-PLAN.md#orig-rule-l0845) ×2 | `RULE:L141–L158`；`RULE:L267–L296`；`RULE:L845–L868` | 6 |
| `R11` | [技能书 · 原L141](MS-SKILLS-PLAN.md#orig-rule-l0141) ×1 | `RULE:L141–L158` | 1 |
| `R12` | [技能书 · 原L141](MS-SKILLS-PLAN.md#orig-rule-l0141) ×1；[技能书 · 原L816](MS-SKILLS-PLAN.md#orig-rule-l0816) ×1 | `RULE:L141–L158`；`RULE:L816–L827` | 2 |
| `R13` | [技能书 · 原L141](MS-SKILLS-PLAN.md#orig-rule-l0141) ×1；[技能书 · 原L267](MS-SKILLS-PLAN.md#orig-rule-l0267) ×1；[技能书 · 原L353](MS-SKILLS-PLAN.md#orig-rule-l0353) ×1；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-rule-l0767) ×1；[技能书 · 原L845](MS-SKILLS-PLAN.md#orig-rule-l0845) ×1 | `RULE:L141–L158`；`RULE:L267–L296`；`RULE:L353–L389`；`RULE:L767–L797`；`RULE:L845–L868` | 5 |
| `R14` | [技能书 · 原L141](MS-SKILLS-PLAN.md#orig-rule-l0141) ×1；[技能书 · 原L267](MS-SKILLS-PLAN.md#orig-rule-l0267) ×1；[技能书 · 原L716](MS-SKILLS-PLAN.md#orig-rule-l0716) ×1；[技能书 · 原L845](MS-SKILLS-PLAN.md#orig-rule-l0845) ×1 | `RULE:L141–L158`；`RULE:L267–L296`；`RULE:L716–L729`；`RULE:L845–L868` | 4 |
| `R15` | [技能书 · 原L141](MS-SKILLS-PLAN.md#orig-rule-l0141) ×1 | `RULE:L141–L158` | 1 |
| `R16` | [技能书 · 原L141](MS-SKILLS-PLAN.md#orig-rule-l0141) ×1 | `RULE:L141–L158` | 1 |
| `R17` | [技能书 · 原L66](MS-SKILLS-PLAN.md#orig-rule-l0066) ×1；[技能书 · 原L141](MS-SKILLS-PLAN.md#orig-rule-l0141) ×1；[技能书 · 原L317](MS-SKILLS-PLAN.md#orig-rule-l0317) ×2；[技能书 · 原L353](MS-SKILLS-PLAN.md#orig-rule-l0353) ×2；[技能书 · 原L476](MS-SKILLS-PLAN.md#orig-rule-l0476) ×3；[技能书 · 原L716](MS-SKILLS-PLAN.md#orig-rule-l0716) ×1 | `RULE:L66–L93`；`RULE:L141–L158`；`RULE:L317–L344`；`RULE:L353–L389`；`RULE:L476–L505`；`RULE:L716–L729` | 10 |
| `R18` | [技能书 · 原L141](MS-SKILLS-PLAN.md#orig-rule-l0141) ×1；[技能书 · 原L748](MS-SKILLS-PLAN.md#orig-rule-l0748) ×1 | `RULE:L141–L158`；`RULE:L748–L766` | 2 |
| `R19` | [技能书 · 原L141](MS-SKILLS-PLAN.md#orig-rule-l0141) ×1 | `RULE:L141–L158` | 1 |
| `R20` | [技能书 · 原L141](MS-SKILLS-PLAN.md#orig-rule-l0141) ×1 | `RULE:L141–L158` | 1 |
| `R21` | [技能书 · 原L112](MS-SKILLS-PLAN.md#orig-rule-l0112) ×1；[技能书 · 原L159](MS-SKILLS-PLAN.md#orig-rule-l0159) ×1；[技能书 · 原L267](MS-SKILLS-PLAN.md#orig-rule-l0267) ×1；[技能书 · 原L845](MS-SKILLS-PLAN.md#orig-rule-l0845) ×1 | `RULE:L112–L132`；`RULE:L159–L174`；`RULE:L267–L296`；`RULE:L845–L868` | 4 |
| `R22` | [技能书 · 原L66](MS-SKILLS-PLAN.md#orig-rule-l0066) ×1；[技能书 · 原L112](MS-SKILLS-PLAN.md#orig-rule-l0112) ×1；[技能书 · 原L159](MS-SKILLS-PLAN.md#orig-rule-l0159) ×1；[技能书 · 原L267](MS-SKILLS-PLAN.md#orig-rule-l0267) ×1；[技能书 · 原L317](MS-SKILLS-PLAN.md#orig-rule-l0317) ×2；[技能书 · 原L353](MS-SKILLS-PLAN.md#orig-rule-l0353) ×1；[技能书 · 原L390](MS-SKILLS-PLAN.md#orig-rule-l0390) ×1；[技能书 · 原L828](MS-SKILLS-PLAN.md#orig-rule-l0828) ×1；[技能书 · 原L845](MS-SKILLS-PLAN.md#orig-rule-l0845) ×1；[技能书 · 原L869](MS-SKILLS-PLAN.md#orig-rule-l0869) ×1 | `RULE:L66–L93`；`RULE:L112–L132`；`RULE:L159–L174`；`RULE:L267–L296`；`RULE:L317–L344`；`RULE:L353–L389`；`RULE:L390–L427`；`RULE:L828–L836`；`RULE:L845–L868`；`RULE:L869–L911` | 11 |
| `R23` | [技能书 · 原L159](MS-SKILLS-PLAN.md#orig-rule-l0159) ×1 | `RULE:L159–L174` | 1 |
| `R24` | [技能书 · 原L159](MS-SKILLS-PLAN.md#orig-rule-l0159) ×1；[技能书 · 原L716](MS-SKILLS-PLAN.md#orig-rule-l0716) ×2 | `RULE:L159–L174`；`RULE:L716–L729` | 3 |
| `R25` | [技能书 · 原L159](MS-SKILLS-PLAN.md#orig-rule-l0159) ×1；[技能书 · 原L716](MS-SKILLS-PLAN.md#orig-rule-l0716) ×1 | `RULE:L159–L174`；`RULE:L716–L729` | 2 |
| `R26` | [技能书 · 原L159](MS-SKILLS-PLAN.md#orig-rule-l0159) ×1；[技能书 · 原L317](MS-SKILLS-PLAN.md#orig-rule-l0317) ×2；[技能书 · 原L353](MS-SKILLS-PLAN.md#orig-rule-l0353) ×1；[技能书 · 原L436](MS-SKILLS-PLAN.md#orig-rule-l0436) ×1；[技能书 · 原L716](MS-SKILLS-PLAN.md#orig-rule-l0716) ×1；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-rule-l0767) ×1 | `RULE:L159–L174`；`RULE:L317–L344`；`RULE:L353–L389`；`RULE:L436–L441`；`RULE:L716–L729`；`RULE:L767–L797` | 7 |
| `R27` | [技能书 · 原L159](MS-SKILLS-PLAN.md#orig-rule-l0159) ×1 | `RULE:L159–L174` | 1 |
| `R28` | [技能书 · 原L159](MS-SKILLS-PLAN.md#orig-rule-l0159) ×1 | `RULE:L159–L174` | 1 |
| `R29` | [技能书 · 原L159](MS-SKILLS-PLAN.md#orig-rule-l0159) ×1 | `RULE:L159–L174` | 1 |
| `R30` | [技能书 · 原L159](MS-SKILLS-PLAN.md#orig-rule-l0159) ×1；[技能书 · 原L267](MS-SKILLS-PLAN.md#orig-rule-l0267) ×1；[技能书 · 原L845](MS-SKILLS-PLAN.md#orig-rule-l0845) ×1 | `RULE:L159–L174`；`RULE:L267–L296`；`RULE:L845–L868` | 3 |
| `R31` | [技能书 · 原L159](MS-SKILLS-PLAN.md#orig-rule-l0159) ×1；[技能书 · 原L317](MS-SKILLS-PLAN.md#orig-rule-l0317) ×2；[技能书 · 原L353](MS-SKILLS-PLAN.md#orig-rule-l0353) ×2；[技能书 · 原L428](MS-SKILLS-PLAN.md#orig-rule-l0428) ×1；[技能书 · 原L611](MS-SKILLS-PLAN.md#orig-rule-l0611) ×1；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-rule-l0767) ×2 | `RULE:L159–L174`；`RULE:L317–L344`；`RULE:L353–L389`；`RULE:L428–L435`；`RULE:L611–L635`；`RULE:L767–L797` | 9 |
| `R32` | [物品书 · 原L175](MS-MSITEM-PLAN.md#orig-rule-l0175) ×1；[物品书 · 原L550](MS-MSITEM-PLAN.md#orig-rule-l0550) ×1；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-rule-l0767) ×1；[物品书 · 原L798](MS-MSITEM-PLAN.md#orig-rule-l0798) ×1 | `RULE:L175–L187`；`RULE:L550–L558`；`RULE:L767–L797`；`RULE:L798–L806` | 4 |
| `R33` | [物品书 · 原L175](MS-MSITEM-PLAN.md#orig-rule-l0175) ×1；[物品书 · 原L798](MS-MSITEM-PLAN.md#orig-rule-l0798) ×1 | `RULE:L175–L187`；`RULE:L798–L806` | 2 |
| `R34` | [物品书 · 原L175](MS-MSITEM-PLAN.md#orig-rule-l0175) ×1 | `RULE:L175–L187` | 1 |
| `R35` | [物品书 · 原L175](MS-MSITEM-PLAN.md#orig-rule-l0175) ×1 | `RULE:L175–L187` | 1 |
| `R36` | [物品书 · 原L175](MS-MSITEM-PLAN.md#orig-rule-l0175) ×1；[技能书 · 原L748](MS-SKILLS-PLAN.md#orig-rule-l0748) ×1；[技能书 · 原L837](MS-SKILLS-PLAN.md#orig-rule-l0837) ×1 | `RULE:L175–L187`；`RULE:L748–L766`；`RULE:L837–L844` | 3 |
| `R37` | [物品书 · 原L175](MS-MSITEM-PLAN.md#orig-rule-l0175) ×1 | `RULE:L175–L187` | 1 |
| `R38` | [物品书 · 原L175](MS-MSITEM-PLAN.md#orig-rule-l0175) ×1 | `RULE:L175–L187` | 1 |
| `R39` | [物品书 · 原L175](MS-MSITEM-PLAN.md#orig-rule-l0175) ×1 | `RULE:L175–L187` | 1 |
| `R40` | [物品书 · 原L188](MS-MSITEM-PLAN.md#orig-rule-l0188) ×1 | `RULE:L188–L197` | 1 |
| `R41` | [物品书 · 原L188](MS-MSITEM-PLAN.md#orig-rule-l0188) ×1；[技能书 · 原L267](MS-SKILLS-PLAN.md#orig-rule-l0267) ×1；[技能书 · 原L845](MS-SKILLS-PLAN.md#orig-rule-l0845) ×1 | `RULE:L188–L197`；`RULE:L267–L296`；`RULE:L845–L868` | 3 |
| `R42` | [物品书 · 原L188](MS-MSITEM-PLAN.md#orig-rule-l0188) ×1；[技能书 · 原L636](MS-SKILLS-PLAN.md#orig-rule-l0636) ×2；[技能书 · 原L716](MS-SKILLS-PLAN.md#orig-rule-l0716) ×1；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-rule-l0767) ×1 | `RULE:L188–L197`；`RULE:L636–L651`；`RULE:L716–L729`；`RULE:L767–L797` | 5 |
| `R43` | [物品书 · 原L188](MS-MSITEM-PLAN.md#orig-rule-l0188) ×1 | `RULE:L188–L197` | 1 |
| `R44` | [物品书 · 原L188](MS-MSITEM-PLAN.md#orig-rule-l0188) ×1 | `RULE:L188–L197` | 1 |
| `R45` | [防御书 · 原L198](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0198) ×1 | `RULE:L198–L214` | 1 |
| `R46` | [防御书 · 原L198](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0198) ×1；[防御书 · 原L538](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0538) ×1；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-rule-l0767) ×1；[技能书 · 原L837](MS-SKILLS-PLAN.md#orig-rule-l0837) ×1 | `RULE:L198–L214`；`RULE:L538–L542`；`RULE:L767–L797`；`RULE:L837–L844` | 4 |
| `R47` | [防御书 · 原L198](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0198) ×1；[防御书 · 原L538](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0538) ×1；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-rule-l0767) ×1 | `RULE:L198–L214`；`RULE:L538–L542`；`RULE:L767–L797` | 3 |
| `R48` | [防御书 · 原L198](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0198) ×1 | `RULE:L198–L214` | 1 |
| `R49` | [防御书 · 原L198](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0198) ×1；[防御书 · 原L538](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0538) ×1；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-rule-l0767) ×1 | `RULE:L198–L214`；`RULE:L538–L542`；`RULE:L767–L797` | 3 |
| `R50` | [防御书 · 原L198](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0198) ×1；[防御书 · 原L538](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0538) ×1；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-rule-l0767) ×1 | `RULE:L198–L214`；`RULE:L538–L542`；`RULE:L767–L797` | 3 |
| `R51` | [防御书 · 原L198](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0198) ×1；[防御书 · 原L538](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0538) ×1；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-rule-l0767) ×1 | `RULE:L198–L214`；`RULE:L538–L542`；`RULE:L767–L797` | 3 |
| `R52` | [防御书 · 原L198](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0198) ×1；[防御书 · 原L512](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0512) ×1 | `RULE:L198–L214`；`RULE:L512–L537` | 2 |
| `R53` | [防御书 · 原L198](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0198) ×1；[技能书 · 原L716](MS-SKILLS-PLAN.md#orig-rule-l0716) ×1 | `RULE:L198–L214`；`RULE:L716–L729` | 2 |
| `R54` | [防御书 · 原L198](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0198) ×1 | `RULE:L198–L214` | 1 |
| `R55` | [防御书 · 原L198](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0198) ×1 | `RULE:L198–L214` | 1 |
| `R56` | [防御书 · 原L198](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0198) ×1 | `RULE:L198–L214` | 1 |
| `R57` | [技能书 · 原L66](MS-SKILLS-PLAN.md#orig-rule-l0066) ×1；[技能书 · 原L215](MS-SKILLS-PLAN.md#orig-rule-l0215) ×1；[技能书 · 原L317](MS-SKILLS-PLAN.md#orig-rule-l0317) ×2；[技能书 · 原L353](MS-SKILLS-PLAN.md#orig-rule-l0353) ×1；[技能书 · 原L442](MS-SKILLS-PLAN.md#orig-rule-l0442) ×1；[技能书 · 原L450](MS-SKILLS-PLAN.md#orig-rule-l0450) ×1；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-rule-l0767) ×1 | `RULE:L66–L93`；`RULE:L215–L223`；`RULE:L317–L344`；`RULE:L353–L389`；`RULE:L442–L449`；`RULE:L450–L475`；`RULE:L767–L797` | 8 |
| `R58` | [技能书 · 原L215](MS-SKILLS-PLAN.md#orig-rule-l0215) ×1；[技能书 · 原L317](MS-SKILLS-PLAN.md#orig-rule-l0317) ×4；[技能书 · 原L353](MS-SKILLS-PLAN.md#orig-rule-l0353) ×1；[技能书 · 原L442](MS-SKILLS-PLAN.md#orig-rule-l0442) ×1；[技能书 · 原L476](MS-SKILLS-PLAN.md#orig-rule-l0476) ×1；[技能书 · 原L506](MS-SKILLS-PLAN.md#orig-rule-l0506) ×1；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-rule-l0767) ×3；[技能书 · 原L816](MS-SKILLS-PLAN.md#orig-rule-l0816) ×1 | `RULE:L215–L223`；`RULE:L317–L344`；`RULE:L353–L389`；`RULE:L442–L449`；`RULE:L476–L505`；`RULE:L506–L511`；`RULE:L767–L797`；`RULE:L816–L827` | 13 |
| `R59` | [技能书 · 原L215](MS-SKILLS-PLAN.md#orig-rule-l0215) ×1 | `RULE:L215–L223` | 1 |
| `R59′` | [技能书 · 原L235](MS-SKILLS-PLAN.md#orig-rule-l0235) ×1；[技能书 · 原L353](MS-SKILLS-PLAN.md#orig-rule-l0353) ×1；[技能书 · 原L506](MS-SKILLS-PLAN.md#orig-rule-l0506) ×1；[技能书 · 原L734](MS-SKILLS-PLAN.md#orig-rule-l0734) ×2；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-rule-l0767) ×1；[技能书 · 原L869](MS-SKILLS-PLAN.md#orig-rule-l0869) ×1 | `RULE:L235–L246`；`RULE:L353–L389`；`RULE:L506–L511`；`RULE:L734–L747`；`RULE:L767–L797`；`RULE:L869–L911` | 7 |
| `R60` | [技能书 · 原L112](MS-SKILLS-PLAN.md#orig-rule-l0112) ×1；[技能书 · 原L215](MS-SKILLS-PLAN.md#orig-rule-l0215) ×1；[防御书 · 原L512](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0512) ×1；[技能书 · 原L926](MS-SKILLS-PLAN.md#orig-rule-l0926) ×1 | `RULE:L112–L132`；`RULE:L215–L223`；`RULE:L512–L537`；`RULE:L926–L948` | 4 |
| `R61` | [技能书 · 原L224](MS-SKILLS-PLAN.md#orig-rule-l0224) ×1；[技能书 · 原L816](MS-SKILLS-PLAN.md#orig-rule-l0816) ×1；[技能书 · 原L957](MS-SKILLS-PLAN.md#orig-rule-l0957) ×1 | `RULE:L224–L234`；`RULE:L816–L827`；`RULE:L957–L973` | 3 |
| `R62` | [技能书 · 原L224](MS-SKILLS-PLAN.md#orig-rule-l0224) ×1；[技能书 · 原L267](MS-SKILLS-PLAN.md#orig-rule-l0267) ×1；[物品书 · 原L550](MS-MSITEM-PLAN.md#orig-rule-l0550) ×1；[技能书 · 原L845](MS-SKILLS-PLAN.md#orig-rule-l0845) ×1；[技能书 · 原L957](MS-SKILLS-PLAN.md#orig-rule-l0957) ×2 | `RULE:L224–L234`；`RULE:L267–L296`；`RULE:L550–L558`；`RULE:L845–L868`；`RULE:L957–L973` | 6 |
| `R63` | [技能书 · 原L224](MS-SKILLS-PLAN.md#orig-rule-l0224) ×1；[防御书 · 原L512](MS-ENTITYDEFENCE-PLAN.md#orig-rule-l0512) ×1；[物品书 · 原L550](MS-MSITEM-PLAN.md#orig-rule-l0550) ×1；[技能书 · 原L698](MS-SKILLS-PLAN.md#orig-rule-l0698) ×1；[技能书 · 原L957](MS-SKILLS-PLAN.md#orig-rule-l0957) ×2 | `RULE:L224–L234`；`RULE:L512–L537`；`RULE:L550–L558`；`RULE:L698–L715`；`RULE:L957–L973` | 6 |
| `R64` | [技能书 · 原L224](MS-SKILLS-PLAN.md#orig-rule-l0224) ×1；[物品书 · 原L550](MS-MSITEM-PLAN.md#orig-rule-l0550) ×1；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-rule-l0767) ×1；[技能书 · 原L957](MS-SKILLS-PLAN.md#orig-rule-l0957) ×1 | `RULE:L224–L234`；`RULE:L550–L558`；`RULE:L767–L797`；`RULE:L957–L973` | 4 |
| `R65` | [技能书 · 原L224](MS-SKILLS-PLAN.md#orig-rule-l0224) ×1；[技能书 · 原L957](MS-SKILLS-PLAN.md#orig-rule-l0957) ×1 | `RULE:L224–L234`；`RULE:L957–L973` | 2 |
| `R66` | [技能书 · 原L47](MS-SKILLS-PLAN.md#orig-rule-l0047) ×1；[技能书 · 原L224](MS-SKILLS-PLAN.md#orig-rule-l0224) ×1；[技能书 · 原L345](MS-SKILLS-PLAN.md#orig-rule-l0345) ×1；[技能书 · 原L543](MS-SKILLS-PLAN.md#orig-rule-l0543) ×1；[技能书 · 原L767](MS-SKILLS-PLAN.md#orig-rule-l0767) ×1；[技能书 · 原L957](MS-SKILLS-PLAN.md#orig-rule-l0957) ×1；[技能书 · 原L1008](MS-SKILLS-PLAN.md#orig-rule-l1008) ×1 | `RULE:L47–L53`；`RULE:L224–L234`；`RULE:L345–L352`；`RULE:L543–L549`；`RULE:L767–L797`；`RULE:L957–L973`；`RULE:L1008–L1026` | 7 |

<a id="ids-non"></a>
### NON

原来源：`docs/plans/Ronova_NonViolating_Enhancement_Plan_V1.0.md`。[来源映射](RESTRUCTURE-MAP.md#source-non) · [原字节快照](../archive/plans-before-ms-restructure-r66/Ronova_NonViolating_Enhancement_Plan_V1.0.md) · [返回来源目录](#source-directory)。

| 原 ID | 新书锚点（×本段次数） | 原来源行范围 | 总次数 |
|---|---|---|---:|
| `A1` | [技能书 · 原L69](MS-SKILLS-PLAN.md#orig-non-l0069) ×1；[技能书 · 原L89](MS-SKILLS-PLAN.md#orig-non-l0089) ×1 | `NON:L69–L77`；`NON:L89–L101` | 2 |
| `A2` | [技能书 · 原L89](MS-SKILLS-PLAN.md#orig-non-l0089) ×1 | `NON:L89–L101` | 1 |
| `A3` | [技能书 · 原L38](MS-SKILLS-PLAN.md#orig-non-l0038) ×1；[技能书 · 原L89](MS-SKILLS-PLAN.md#orig-non-l0089) ×1；[技能书 · 原L276](MS-SKILLS-PLAN.md#orig-non-l0276) ×1 | `NON:L38–L56`；`NON:L89–L101`；`NON:L276–L296` | 3 |
| `A4` | [技能书 · 原L89](MS-SKILLS-PLAN.md#orig-non-l0089) ×1 | `NON:L89–L101` | 1 |
| `D1` | [物品书 · 原L145](MS-MSITEM-PLAN.md#orig-non-l0145) ×1 | `NON:L145–L152` | 1 |
| `D2` | [物品书 · 原L145](MS-MSITEM-PLAN.md#orig-non-l0145) ×1 | `NON:L145–L152` | 1 |
| `D3` | [物品书 · 原L145](MS-MSITEM-PLAN.md#orig-non-l0145) ×1 | `NON:L145–L152` | 1 |
| `D91` | [技能书 · 原L165](MS-SKILLS-PLAN.md#orig-non-l0165) ×1 | `NON:L165–L175` | 1 |

<a id="ids-ds"></a>
### DS

原来源：`docs/DESIGN-SPEC.md`。[来源映射](RESTRUCTURE-MAP.md#source-ds) · [原字节快照](../archive/plans-before-ms-restructure-r66/DESIGN-SPEC.md) · [返回来源目录](#source-directory)。

| 原 ID | 新书锚点（×本段次数） | 原来源行范围 | 总次数 |
|---|---|---|---:|
| `A1` | [技能书 · 原L385](MS-SKILLS-PLAN.md#orig-ds-l0385) ×3；[技能书 · 原L463](MS-SKILLS-PLAN.md#orig-ds-l0463) ×1；[技能书 · 原L871](MS-SKILLS-PLAN.md#orig-ds-l0871) ×1；[技能书 · 原L890](MS-SKILLS-PLAN.md#orig-ds-l0890) ×1；[技能书 · 原L932](MS-SKILLS-PLAN.md#orig-ds-l0932) ×1；[技能书 · 原L962](MS-SKILLS-PLAN.md#orig-ds-l0962) ×1；[技能书 · 原L1007](MS-SKILLS-PLAN.md#orig-ds-l1007) ×2；[技能书 · 原L1040](MS-SKILLS-PLAN.md#orig-ds-l1040) ×1 | `DS:L385–L424`；`DS:L463–L487`；`DS:L871–L889`；`DS:L890–L906`；`DS:L932–L947`；`DS:L962–L985`；`DS:L1007–L1020`；`DS:L1040–L1065` | 11 |
| `A2` | [技能书 · 原L385](MS-SKILLS-PLAN.md#orig-ds-l0385) ×1；[技能书 · 原L1007](MS-SKILLS-PLAN.md#orig-ds-l1007) ×1；[技能书 · 原L1107](MS-SKILLS-PLAN.md#orig-ds-l1107) ×1 | `DS:L385–L424`；`DS:L1007–L1020`；`DS:L1107–L1114` | 3 |
| `A3` | [技能书 · 原L385](MS-SKILLS-PLAN.md#orig-ds-l0385) ×1；[技能书 · 原L1007](MS-SKILLS-PLAN.md#orig-ds-l1007) ×1 | `DS:L385–L424`；`DS:L1007–L1020` | 2 |
| `A4` | [技能书 · 原L385](MS-SKILLS-PLAN.md#orig-ds-l0385) ×1；[技能书 · 原L463](MS-SKILLS-PLAN.md#orig-ds-l0463) ×1；[技能书 · 原L1007](MS-SKILLS-PLAN.md#orig-ds-l1007) ×2；[技能书 · 原L1040](MS-SKILLS-PLAN.md#orig-ds-l1040) ×1；[技能书 · 原L1107](MS-SKILLS-PLAN.md#orig-ds-l1107) ×1 | `DS:L385–L424`；`DS:L463–L487`；`DS:L1007–L1020`；`DS:L1040–L1065`；`DS:L1107–L1114` | 6 |
| `A5` | [技能书 · 原L385](MS-SKILLS-PLAN.md#orig-ds-l0385) ×1；[技能书 · 原L962](MS-SKILLS-PLAN.md#orig-ds-l0962) ×1；[技能书 · 原L1007](MS-SKILLS-PLAN.md#orig-ds-l1007) ×1；[技能书 · 原L1040](MS-SKILLS-PLAN.md#orig-ds-l1040) ×1；[技能书 · 原L1100](MS-SKILLS-PLAN.md#orig-ds-l1100) ×1；[技能书 · 原L1126](MS-SKILLS-PLAN.md#orig-ds-l1126) ×1；[技能书 · 原L1160](MS-SKILLS-PLAN.md#orig-ds-l1160) ×1 | `DS:L385–L424`；`DS:L962–L985`；`DS:L1007–L1020`；`DS:L1040–L1065`；`DS:L1100–L1106`；`DS:L1126–L1150`；`DS:L1160–L1169` | 7 |
| `A6` | [技能书 · 原L385](MS-SKILLS-PLAN.md#orig-ds-l0385) ×1；[技能书 · 原L962](MS-SKILLS-PLAN.md#orig-ds-l0962) ×1 | `DS:L385–L424`；`DS:L962–L985` | 2 |
| `A7` | [技能书 · 原L385](MS-SKILLS-PLAN.md#orig-ds-l0385) ×1；[技能书 · 原L962](MS-SKILLS-PLAN.md#orig-ds-l0962) ×1 | `DS:L385–L424`；`DS:L962–L985` | 2 |
| `A8` | [技能书 · 原L385](MS-SKILLS-PLAN.md#orig-ds-l0385) ×3；[技能书 · 原L871](MS-SKILLS-PLAN.md#orig-ds-l0871) ×1；[技能书 · 原L890](MS-SKILLS-PLAN.md#orig-ds-l0890) ×1；[技能书 · 原L932](MS-SKILLS-PLAN.md#orig-ds-l0932) ×1 | `DS:L385–L424`；`DS:L871–L889`；`DS:L890–L906`；`DS:L932–L947` | 6 |
| `D1` | [技能书 · 原L19](MS-SKILLS-PLAN.md#orig-ds-l0019) ×1；[技能书 · 原L177](MS-SKILLS-PLAN.md#orig-ds-l0177) ×3；[技能书 · 原L329](MS-SKILLS-PLAN.md#orig-ds-l0329) ×1；[技能书 · 原L364](MS-SKILLS-PLAN.md#orig-ds-l0364) ×1；[技能书 · 原L871](MS-SKILLS-PLAN.md#orig-ds-l0871) ×1；[技能书 · 原L932](MS-SKILLS-PLAN.md#orig-ds-l0932) ×1；[技能书 · 原L962](MS-SKILLS-PLAN.md#orig-ds-l0962) ×1 | `DS:L19–L32`；`DS:L177–L196`；`DS:L329–L363`；`DS:L364–L384`；`DS:L871–L889`；`DS:L932–L947`；`DS:L962–L985` | 9 |
| `D2` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-ds-l0177) ×1；[技能书 · 原L283](MS-SKILLS-PLAN.md#orig-ds-l0283) ×1 | `DS:L177–L196`；`DS:L283–L298` | 2 |
| `D3` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-ds-l0177) ×1；[技能书 · 原L283](MS-SKILLS-PLAN.md#orig-ds-l0283) ×1；[技能书 · 原L962](MS-SKILLS-PLAN.md#orig-ds-l0962) ×1；[技能书 · 原L1040](MS-SKILLS-PLAN.md#orig-ds-l1040) ×1；[技能书 · 原L1126](MS-SKILLS-PLAN.md#orig-ds-l1126) ×1；[技能书 · 原L1160](MS-SKILLS-PLAN.md#orig-ds-l1160) ×1 | `DS:L177–L196`；`DS:L283–L298`；`DS:L962–L985`；`DS:L1040–L1065`；`DS:L1126–L1150`；`DS:L1160–L1169` | 6 |
| `D4` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-ds-l0177) ×1 | `DS:L177–L196` | 1 |
| `D5` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-ds-l0177) ×1；[技能书 · 原L962](MS-SKILLS-PLAN.md#orig-ds-l0962) ×1 | `DS:L177–L196`；`DS:L962–L985` | 2 |
| `D6` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-ds-l0177) ×1；[技能书 · 原L283](MS-SKILLS-PLAN.md#orig-ds-l0283) ×1；[技能书 · 原L516](MS-SKILLS-PLAN.md#orig-ds-l0516) ×1；[技能书 · 原L962](MS-SKILLS-PLAN.md#orig-ds-l0962) ×1；[技能书 · 原L1040](MS-SKILLS-PLAN.md#orig-ds-l1040) ×1 | `DS:L177–L196`；`DS:L283–L298`；`DS:L516–L536`；`DS:L962–L985`；`DS:L1040–L1065` | 5 |
| `D7` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-ds-l0177) ×1；[技能书 · 原L962](MS-SKILLS-PLAN.md#orig-ds-l0962) ×1；[技能书 · 原L1040](MS-SKILLS-PLAN.md#orig-ds-l1040) ×1 | `DS:L177–L196`；`DS:L962–L985`；`DS:L1040–L1065` | 3 |
| `D8` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-ds-l0177) ×2 | `DS:L177–L196` | 2 |
| `D9` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-ds-l0177) ×2；[技能书 · 原L962](MS-SKILLS-PLAN.md#orig-ds-l0962) ×1；[技能书 · 原L1040](MS-SKILLS-PLAN.md#orig-ds-l1040) ×1 | `DS:L177–L196`；`DS:L962–L985`；`DS:L1040–L1065` | 4 |
| `D10` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-ds-l0177) ×1 | `DS:L177–L196` | 1 |
| `D11` | [防御书 · 原L101](MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0101) ×1；[技能书 · 原L177](MS-SKILLS-PLAN.md#orig-ds-l0177) ×1 | `DS:L101–L109`；`DS:L177–L196` | 2 |
| `D12` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-ds-l0177) ×1 | `DS:L177–L196` | 1 |
| `D13` | [技能书 · 原L19](MS-SKILLS-PLAN.md#orig-ds-l0019) ×1；[技能书 · 原L177](MS-SKILLS-PLAN.md#orig-ds-l0177) ×3；[技能书 · 原L267](MS-SKILLS-PLAN.md#orig-ds-l0267) ×2；[技能书 · 原L463](MS-SKILLS-PLAN.md#orig-ds-l0463) ×2；[技能书 · 原L871](MS-SKILLS-PLAN.md#orig-ds-l0871) ×1 | `DS:L19–L32`；`DS:L177–L196`；`DS:L267–L282`；`DS:L463–L487`；`DS:L871–L889` | 9 |
| `D32` | [技能书 · 原L329](MS-SKILLS-PLAN.md#orig-ds-l0329) ×1；[技能书 · 原L364](MS-SKILLS-PLAN.md#orig-ds-l0364) ×1 | `DS:L329–L363`；`DS:L364–L384` | 2 |
| `D33` | [技能书 · 原L319](MS-SKILLS-PLAN.md#orig-ds-l0319) ×1；[技能书 · 原L364](MS-SKILLS-PLAN.md#orig-ds-l0364) ×1；[技能书 · 原L871](MS-SKILLS-PLAN.md#orig-ds-l0871) ×1 | `DS:L319–L328`；`DS:L364–L384`；`DS:L871–L889` | 3 |
| `D53` | [技能书 · 原L329](MS-SKILLS-PLAN.md#orig-ds-l0329) ×2；[技能书 · 原L871](MS-SKILLS-PLAN.md#orig-ds-l0871) ×1 | `DS:L329–L363`；`DS:L871–L889` | 3 |
| `D55` | [技能书 · 原L844](MS-SKILLS-PLAN.md#orig-ds-l0844) ×1 | `DS:L844–L870` | 1 |
| `D62` | [技能书 · 原L617](MS-SKILLS-PLAN.md#orig-ds-l0617) ×1 | `DS:L617–L623` | 1 |
| `D65` | [技能书 · 原L329](MS-SKILLS-PLAN.md#orig-ds-l0329) ×1；[技能书 · 原L844](MS-SKILLS-PLAN.md#orig-ds-l0844) ×1 | `DS:L329–L363`；`DS:L844–L870` | 2 |
| `D66` | [技能书 · 原L329](MS-SKILLS-PLAN.md#orig-ds-l0329) ×1；[技能书 · 原L1072](MS-SKILLS-PLAN.md#orig-ds-l1072) ×1 | `DS:L329–L363`；`DS:L1072–L1082` | 2 |
| `D71` | [技能书 · 原L844](MS-SKILLS-PLAN.md#orig-ds-l0844) ×1 | `DS:L844–L870` | 1 |
| `D73` | [物品书 · 原L730](MS-MSITEM-PLAN.md#orig-ds-l0730) ×2 | `DS:L730–L768` | 2 |
| `D76` | [技能书 · 原L1107](MS-SKILLS-PLAN.md#orig-ds-l1107) ×1 | `DS:L1107–L1114` | 1 |
| `D77` | [技能书 · 原L1107](MS-SKILLS-PLAN.md#orig-ds-l1107) ×1 | `DS:L1107–L1114` | 1 |
| `D78` | [技能书 · 原L329](MS-SKILLS-PLAN.md#orig-ds-l0329) ×1；[技能书 · 原L844](MS-SKILLS-PLAN.md#orig-ds-l0844) ×1 | `DS:L329–L363`；`DS:L844–L870` | 2 |
| `D79` | [技能书 · 原L329](MS-SKILLS-PLAN.md#orig-ds-l0329) ×1 | `DS:L329–L363` | 1 |
| `D81` | [技能书 · 原L537](MS-SKILLS-PLAN.md#orig-ds-l0537) ×1 | `DS:L537–L547` | 1 |
| `D82` | [技能书 · 原L537](MS-SKILLS-PLAN.md#orig-ds-l0537) ×1；[技能书 · 原L844](MS-SKILLS-PLAN.md#orig-ds-l0844) ×1 | `DS:L537–L547`；`DS:L844–L870` | 2 |
| `D86` | [技能书 · 原L44](MS-SKILLS-PLAN.md#orig-ds-l0044) ×1；[技能书 · 原L329](MS-SKILLS-PLAN.md#orig-ds-l0329) ×1；[技能书 · 原L844](MS-SKILLS-PLAN.md#orig-ds-l0844) ×2 | `DS:L44–L52`；`DS:L329–L363`；`DS:L844–L870` | 4 |
| `D87` | [技能书 · 原L329](MS-SKILLS-PLAN.md#orig-ds-l0329) ×1 | `DS:L329–L363` | 1 |
| `D89` | [物品书 · 原L698](MS-MSITEM-PLAN.md#orig-ds-l0698) ×1 | `DS:L698–L707` | 1 |
| `D91` | [物品书 · 原L787](MS-MSITEM-PLAN.md#orig-ds-l0787) ×1；[技能书 · 原L962](MS-SKILLS-PLAN.md#orig-ds-l0962) ×1；[技能书 · 原L1040](MS-SKILLS-PLAN.md#orig-ds-l1040) ×1；[技能书 · 原L1126](MS-SKILLS-PLAN.md#orig-ds-l1126) ×1；[技能书 · 原L1160](MS-SKILLS-PLAN.md#orig-ds-l1160) ×1 | `DS:L787–L795`；`DS:L962–L985`；`DS:L1040–L1065`；`DS:L1126–L1150`；`DS:L1160–L1169` | 5 |
| `D92` | [技能书 · 原L659](MS-SKILLS-PLAN.md#orig-ds-l0659) ×1 | `DS:L659–L676` | 1 |
| `D94` | [技能书 · 原L844](MS-SKILLS-PLAN.md#orig-ds-l0844) ×1；[物品书 · 原L1066](MS-MSITEM-PLAN.md#orig-ds-l1066) ×1；[技能书 · 原L1083](MS-SKILLS-PLAN.md#orig-ds-l1083) ×1 | `DS:L844–L870`；`DS:L1066–L1071`；`DS:L1083–L1099` | 3 |
| `D96` | [技能书 · 原L537](MS-SKILLS-PLAN.md#orig-ds-l0537) ×1 | `DS:L537–L547` | 1 |
| `D98` | [技能书 · 原L44](MS-SKILLS-PLAN.md#orig-ds-l0044) ×2；[技能书 · 原L329](MS-SKILLS-PLAN.md#orig-ds-l0329) ×1；[技能书 · 原L844](MS-SKILLS-PLAN.md#orig-ds-l0844) ×2 | `DS:L44–L52`；`DS:L329–L363`；`DS:L844–L870` | 5 |
| `D99` | [技能书 · 原L329](MS-SKILLS-PLAN.md#orig-ds-l0329) ×1 | `DS:L329–L363` | 1 |
| `D100` | [防御书 · 原L75](MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0075) ×1 | `DS:L75–L98` | 1 |
| `D105` | [技能书 · 原L844](MS-SKILLS-PLAN.md#orig-ds-l0844) ×1 | `DS:L844–L870` | 1 |
| `D106` | [技能书 · 原L44](MS-SKILLS-PLAN.md#orig-ds-l0044) ×1；[技能书 · 原L329](MS-SKILLS-PLAN.md#orig-ds-l0329) ×2；[技能书 · 原L364](MS-SKILLS-PLAN.md#orig-ds-l0364) ×1；[技能书 · 原L844](MS-SKILLS-PLAN.md#orig-ds-l0844) ×1；[技能书 · 原L871](MS-SKILLS-PLAN.md#orig-ds-l0871) ×1；[技能书 · 原L932](MS-SKILLS-PLAN.md#orig-ds-l0932) ×1 | `DS:L44–L52`；`DS:L329–L363`；`DS:L364–L384`；`DS:L844–L870`；`DS:L871–L889`；`DS:L932–L947` | 7 |
| `ID-2` | [物品书 · 原L677](MS-MSITEM-PLAN.md#orig-ds-l0677) ×2 | `DS:L677–L697` | 2 |
| `ID-6` | [技能书 · 原L659](MS-SKILLS-PLAN.md#orig-ds-l0659) ×1 | `DS:L659–L676` | 1 |
| `ID-9` | [技能书 · 原L463](MS-SKILLS-PLAN.md#orig-ds-l0463) ×1 | `DS:L463–L487` | 1 |
| `L5-INV-1` | [技能书 · 原L283](MS-SKILLS-PLAN.md#orig-ds-l0283) ×1；[技能书 · 原L871](MS-SKILLS-PLAN.md#orig-ds-l0871) ×1 | `DS:L283–L298`；`DS:L871–L889` | 2 |
| `L5-INV-2` | [技能书 · 原L283](MS-SKILLS-PLAN.md#orig-ds-l0283) ×1 | `DS:L283–L298` | 1 |
| `L5-INV-3` | [技能书 · 原L283](MS-SKILLS-PLAN.md#orig-ds-l0283) ×1 | `DS:L283–L298` | 1 |
| `L5-INV-4` | [技能书 · 原L283](MS-SKILLS-PLAN.md#orig-ds-l0283) ×1；[技能书 · 原L962](MS-SKILLS-PLAN.md#orig-ds-l0962) ×1 | `DS:L283–L298`；`DS:L962–L985` | 2 |
| `L5-INV-5` | [技能书 · 原L283](MS-SKILLS-PLAN.md#orig-ds-l0283) ×1；[技能书 · 原L1107](MS-SKILLS-PLAN.md#orig-ds-l1107) ×1 | `DS:L283–L298`；`DS:L1107–L1114` | 2 |
| `L5-INV-6` | [技能书 · 原L283](MS-SKILLS-PLAN.md#orig-ds-l0283) ×2；[技能书 · 原L890](MS-SKILLS-PLAN.md#orig-ds-l0890) ×1 | `DS:L283–L298`；`DS:L890–L906` | 3 |
| `L5-INV-7` | [技能书 · 原L283](MS-SKILLS-PLAN.md#orig-ds-l0283) ×1 | `DS:L283–L298` | 1 |
| `L5-INV-8` | [技能书 · 原L283](MS-SKILLS-PLAN.md#orig-ds-l0283) ×1；[技能书 · 原L962](MS-SKILLS-PLAN.md#orig-ds-l0962) ×1；[技能书 · 原L1021](MS-SKILLS-PLAN.md#orig-ds-l1021) ×1；[技能书 · 原L1040](MS-SKILLS-PLAN.md#orig-ds-l1040) ×1；[技能书 · 原L1126](MS-SKILLS-PLAN.md#orig-ds-l1126) ×1；[技能书 · 原L1160](MS-SKILLS-PLAN.md#orig-ds-l1160) ×1 | `DS:L283–L298`；`DS:L962–L985`；`DS:L1021–L1039`；`DS:L1040–L1065`；`DS:L1126–L1150`；`DS:L1160–L1169` | 6 |
| `L5-INV-9` | [技能书 · 原L283](MS-SKILLS-PLAN.md#orig-ds-l0283) ×1；[技能书 · 原L962](MS-SKILLS-PLAN.md#orig-ds-l0962) ×1 | `DS:L283–L298`；`DS:L962–L985` | 2 |
| `L6-INV-1` | [技能书 · 原L299](MS-SKILLS-PLAN.md#orig-ds-l0299) ×1；[技能书 · 原L871](MS-SKILLS-PLAN.md#orig-ds-l0871) ×1；[技能书 · 原L962](MS-SKILLS-PLAN.md#orig-ds-l0962) ×1；[技能书 · 原L1040](MS-SKILLS-PLAN.md#orig-ds-l1040) ×1；[技能书 · 原L1126](MS-SKILLS-PLAN.md#orig-ds-l1126) ×1；[技能书 · 原L1160](MS-SKILLS-PLAN.md#orig-ds-l1160) ×1 | `DS:L299–L318`；`DS:L871–L889`；`DS:L962–L985`；`DS:L1040–L1065`；`DS:L1126–L1150`；`DS:L1160–L1169` | 6 |
| `L6-INV-2` | [技能书 · 原L299](MS-SKILLS-PLAN.md#orig-ds-l0299) ×1 | `DS:L299–L318` | 1 |
| `L6-INV-3` | [技能书 · 原L299](MS-SKILLS-PLAN.md#orig-ds-l0299) ×1；[技能书 · 原L962](MS-SKILLS-PLAN.md#orig-ds-l0962) ×1；[技能书 · 原L1040](MS-SKILLS-PLAN.md#orig-ds-l1040) ×1 | `DS:L299–L318`；`DS:L962–L985`；`DS:L1040–L1065` | 3 |
| `L6-INV-4` | [技能书 · 原L299](MS-SKILLS-PLAN.md#orig-ds-l0299) ×1 | `DS:L299–L318` | 1 |
| `L6-INV-5` | [技能书 · 原L299](MS-SKILLS-PLAN.md#orig-ds-l0299) ×1 | `DS:L299–L318` | 1 |
| `L6-INV-6` | [技能书 · 原L299](MS-SKILLS-PLAN.md#orig-ds-l0299) ×1 | `DS:L299–L318` | 1 |
| `L6-INV-7` | [技能书 · 原L299](MS-SKILLS-PLAN.md#orig-ds-l0299) ×1 | `DS:L299–L318` | 1 |
| `L6-INV-8` | [技能书 · 原L299](MS-SKILLS-PLAN.md#orig-ds-l0299) ×1 | `DS:L299–L318` | 1 |
| `L6-INV-9` | [技能书 · 原L299](MS-SKILLS-PLAN.md#orig-ds-l0299) ×1 | `DS:L299–L318` | 1 |
| `L6-INV-10` | [技能书 · 原L168](MS-SKILLS-PLAN.md#orig-ds-l0168) ×1；[技能书 · 原L299](MS-SKILLS-PLAN.md#orig-ds-l0299) ×2；[技能书 · 原L844](MS-SKILLS-PLAN.md#orig-ds-l0844) ×1；[技能书 · 原L890](MS-SKILLS-PLAN.md#orig-ds-l0890) ×1 | `DS:L168–L176`；`DS:L299–L318`；`DS:L844–L870`；`DS:L890–L906` | 5 |
| `PROC-INV-1` | [技能书 · 原L177](MS-SKILLS-PLAN.md#orig-ds-l0177) ×1；[技能书 · 原L267](MS-SKILLS-PLAN.md#orig-ds-l0267) ×1；[技能书 · 原L283](MS-SKILLS-PLAN.md#orig-ds-l0283) ×1；[技能书 · 原L385](MS-SKILLS-PLAN.md#orig-ds-l0385) ×2；[技能书 · 原L425](MS-SKILLS-PLAN.md#orig-ds-l0425) ×1；[技能书 · 原L463](MS-SKILLS-PLAN.md#orig-ds-l0463) ×2；[技能书 · 原L871](MS-SKILLS-PLAN.md#orig-ds-l0871) ×2；[技能书 · 原L932](MS-SKILLS-PLAN.md#orig-ds-l0932) ×1；[技能书 · 原L1007](MS-SKILLS-PLAN.md#orig-ds-l1007) ×1 | `DS:L177–L196`；`DS:L267–L282`；`DS:L283–L298`；`DS:L385–L424`；`DS:L425–L460`；`DS:L463–L487`；`DS:L871–L889`；`DS:L932–L947`；`DS:L1007–L1020` | 12 |
| `R1` | [技能书 · 原L364](MS-SKILLS-PLAN.md#orig-ds-l0364) ×2；[技能书 · 原L871](MS-SKILLS-PLAN.md#orig-ds-l0871) ×1；[技能书 · 原L932](MS-SKILLS-PLAN.md#orig-ds-l0932) ×1；[技能书 · 原L950](MS-SKILLS-PLAN.md#orig-ds-l0950) ×1；[技能书 · 原L962](MS-SKILLS-PLAN.md#orig-ds-l0962) ×1；[技能书 · 原L986](MS-SKILLS-PLAN.md#orig-ds-l0986) ×2；[技能书 · 原L1160](MS-SKILLS-PLAN.md#orig-ds-l1160) ×1 | `DS:L364–L384`；`DS:L871–L889`；`DS:L932–L947`；`DS:L950–L961`；`DS:L962–L985`；`DS:L986–L1002`；`DS:L1160–L1169` | 9 |
| `R2` | [技能书 · 原L364](MS-SKILLS-PLAN.md#orig-ds-l0364) ×1；[技能书 · 原L950](MS-SKILLS-PLAN.md#orig-ds-l0950) ×1；[技能书 · 原L986](MS-SKILLS-PLAN.md#orig-ds-l0986) ×1；[技能书 · 原L1160](MS-SKILLS-PLAN.md#orig-ds-l1160) ×1 | `DS:L364–L384`；`DS:L950–L961`；`DS:L986–L1002`；`DS:L1160–L1169` | 4 |
| `R3` | [技能书 · 原L364](MS-SKILLS-PLAN.md#orig-ds-l0364) ×1；[技能书 · 原L950](MS-SKILLS-PLAN.md#orig-ds-l0950) ×1；[技能书 · 原L1160](MS-SKILLS-PLAN.md#orig-ds-l1160) ×1 | `DS:L364–L384`；`DS:L950–L961`；`DS:L1160–L1169` | 3 |
| `R4` | [技能书 · 原L364](MS-SKILLS-PLAN.md#orig-ds-l0364) ×1；[技能书 · 原L659](MS-SKILLS-PLAN.md#orig-ds-l0659) ×1；[技能书 · 原L950](MS-SKILLS-PLAN.md#orig-ds-l0950) ×1；[技能书 · 原L1160](MS-SKILLS-PLAN.md#orig-ds-l1160) ×1 | `DS:L364–L384`；`DS:L659–L676`；`DS:L950–L961`；`DS:L1160–L1169` | 4 |
| `R5` | [防御书 · 原L75](MS-ENTITYDEFENCE-PLAN.md#orig-ds-l0075) ×1；[技能书 · 原L364](MS-SKILLS-PLAN.md#orig-ds-l0364) ×1 | `DS:L75–L98`；`DS:L364–L384` | 2 |
| `R6` | [技能书 · 原L364](MS-SKILLS-PLAN.md#orig-ds-l0364) ×1；[技能书 · 原L844](MS-SKILLS-PLAN.md#orig-ds-l0844) ×1 | `DS:L364–L384`；`DS:L844–L870` | 2 |
| `R7` | [技能书 · 原L364](MS-SKILLS-PLAN.md#orig-ds-l0364) ×2；[技能书 · 原L463](MS-SKILLS-PLAN.md#orig-ds-l0463) ×4；[技能书 · 原L871](MS-SKILLS-PLAN.md#orig-ds-l0871) ×1；[技能书 · 原L932](MS-SKILLS-PLAN.md#orig-ds-l0932) ×1；[技能书 · 原L1007](MS-SKILLS-PLAN.md#orig-ds-l1007) ×3 | `DS:L364–L384`；`DS:L463–L487`；`DS:L871–L889`；`DS:L932–L947`；`DS:L1007–L1020` | 11 |
| `T-07` | [技能书 · 原L659](MS-SKILLS-PLAN.md#orig-ds-l0659) ×1 | `DS:L659–L676` | 1 |
| `T-13` | [技能书 · 原L488](MS-SKILLS-PLAN.md#orig-ds-l0488) ×1 | `DS:L488–L503` | 1 |

<a id="reproduce"></a>
## 重新生成与核对

在完整文档根目录，或已经合并完整 r66 文档的项目根目录执行：

```powershell
python docs/plans/restructure/generate_invariant_index.py --write
python docs/plans/restructure/generate_invariant_index.py --check
```

`--write` 只重写本索引；`--check` 比较索引与机器清单、来源快照及新书锚点的一致性，不写文件。二者都不编译、不启动游戏、不确认任何源码功能或阶段完成。
