# 重构资料与核对记录入口

**fix12当前资料：** [验收分工](../WIRING-ACCEPTANCE.md#decision) · [当前核对](../RESTRUCTURE-CHECK.md#review-fix12-check)。只正式化原19行判据和方向/批次桥接；原68项状态与历史核对记录不改。下方旧“当前”按轮次阅读。

**fix11当前资料：** [补遗回执](../RESTRUCTURE-REVIEW.md#review-fix11-baseline) · [既有接缝](../R66-CAPABILITY-WIRING-PLAN.md#eight-existing-baseline) · [源码依据](../../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#findings)。新来源登记在原融合JSON的`existing_baseline_supplement_handoffs`；下方旧“当前”条目仅属所标版本。

[fix10八条回执](../RESTRUCTURE-REVIEW.md#review-fix10-eight) · [八方向当前规划](../R66-CAPABILITY-WIRING-PLAN.md#eight-directions) · [原核验稿](../../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md) · [独立补读](../../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#findings)。`eight_direction_verification_handoffs`记录新增来源、分段和摘录；当前摘要仍只更新固定的`current-verification.json`。

当前文档修订：**fix12 · 新增接线验收分工（2026-09-15）；原方向/批次不变，fix11及更早记录保留版本/环境语境**。这里区分当前资料、当前核对快照和历史核对，不按文件名排序或修改时间猜测效力。

[规划入口](../README.md) · [fix9 Windows复跑闭合](../RESTRUCTURE-REVIEW.md#review-fix9-windows) · [原始Windows回执](../../reference/R66-FIX8-WINDOWS-RERUN-RECEIPT.md) · [fix8回执/工具修复](../RESTRUCTURE-REVIEW.md#review-fix8-verification) · [fix8逐节映射](../RESTRUCTURE-MAP.md#fix8-verification-merge) · [fix7清点回执](../RESTRUCTURE-REVIEW.md#review-fix7-surface) · [fix7逐节映射](../RESTRUCTURE-MAP.md#fix7-surface-merge) · [当前接线规划](../R66-CAPABILITY-WIRING-PLAN.md) · [fix6九项回执](../RESTRUCTURE-REVIEW.md#review-fix6-v13) · [fix6逐节融合](../RESTRUCTURE-MAP.md#fix6-v13-merge) · [fix5 逐节融合](../RESTRUCTURE-MAP.md#fix5-addendum-merge) · [收口审阅说明](../RESTRUCTURE-REVIEW.md#review-fix3) · [核对说明](../RESTRUCTURE-CHECK.md#review-fix3-check) · [台账接线与剩余证据](../VALIDATION-TRACEABILITY.md#ledger-bind)

## 现在看哪份

| 用途 | 入口及效力 |
|---|---|
| 新增来源与融合关系 | [enhancement-addendum-merge.json](enhancement-addendum-merge.json)：原三份输入、`followup_handoffs`内V1.3正文/发出说明、`same_privilege_handoffs`内同权限清点、`verification_ceiling_handoffs`内fix8四文件，以及`windows_rerun_receipts`内fix9现场回执的哈希与保留位置；原始 zip/正文均可恢复。Windows运行结论属于用户现场回报，不由本地Linux核对器认证。 |
| r66 静态接线依据 | [capability-wiring-baseline.json](capability-wiring-baseline.json)：源码哈希、54 键/14 动作和有限静态摘录核对；不是运行状态，人工设计关联不由清单证明。 |
| 当前文档核对 | [current-verification.json](current-verification.json)：固定名称的当前摘要，记录输入基线、所核文件哈希、原三个文档脚本及增补保真核对结果、改动边界与链接增量。仅对列明内容有效；未来内容有改动时须重跑。不是验收台账。 |
| 原文保真与迁移关系 | [source-manifest.json](source-manifest.json)、[section-map.json](section-map.json)、[id-inventory.json](id-inventory.json)、[reference-updates.json](reference-updates.json)：原来源/分段/ID/引用的追溯资料；“原始”不等于过期，也不因新摘要出现就替换它们。 |
| 当前适用性与候选映射 | [ms-coverage.json](ms-coverage.json)、[scope-adjustments.json](scope-adjustments.json)、[validation-traceability.json](validation-traceability.json)：来源分类、效力提示和设计关联。人工语义仍需审阅，机器一致性检查不证明覆盖。 |
| 交付清单 | [package-manifest.json](package-manifest.json)：相对原 r66 的累计文件差异；不代表本轮执行了源码功能。小补丁的基线另见交付说明。 |
| 输入溯源 | [input-archives.json](input-archives.json)、[input-file-hashes.json](input-file-hashes.json)：初轮输入记录，不描述用户所有后续工作区。 |
| 只读复核工具 | [verify_restructure.py](verify_restructure.py)、[generate_invariant_index.py](generate_invariant_index.py)、[generate_validation_traceability.py](generate_validation_traceability.py)。两个生成器的 `--check` 比较派生 Markdown，原文核对器直接运行；新增来源只读核对 [verify_addendum_merge.py](verify_addendum_merge.py)；可选 `--source-root` 比较实际 r66 源码，不需要 chmod 或生成运行日志。解析器辅助测试 [test_validation_traceability.py](test_validation_traceability.py) 不是游戏验收。 |

## 历史记录保留，不作为当前重新执行的结果

| 历史文件 | 适用轮次 / 阅读方法 |
|---|---|
| [verification.json](verification.json) | 初轮 MS 重构的文档保真核对，不是当前修订的执行结果。 |
| [link-audit.json](link-audit.json) | 初轮本地链接审计，保留原缺件语境。 |
| [review-verification.json](review-verification.json) | fix1 核对，不能覆盖或冒充后续修订的结果。 |
| [link-audit-fix2.json](link-audit-fix2.json) | fix2 链接审计，保留519处原问题的历史逐项清单；该文件的逐项清单是历史记录；当前增量基线是实际输入fix10，用同一解析器重算两侧再比较。 |
| [review-verification-fix2.json](review-verification-fix2.json) | fix2 文档核对；里面的台账缺件只描述当时收到的压缩包。 |
| [review-notes.json](review-notes.json) | fix1 的审阅解释记录，不是当前逐项状态台账。 |

以上文件本轮不改名、不移动、不覆盖、不删除。`RESTRUCTURE-CHECK.md` / `RESTRUCTURE-REVIEW.md` 按已标明轮次阅读；文件靠前的初轮记录不会因为位于首页就获得当前效力。

## 收敛方式

日常迭代只更新本入口、固定名称的 `current-verification.json` 和当前派生文档/清单；不默认再生成 `link-audit-fixN.json` / `review-verification-fixN.json` 成套副本。当前摘要的链接部分比较基线问题多重集，不重复拷贝全部旧缺件清单。需要发布级历史证据时，才显式归档并写清基线与适用范围；这项工作约定可以修订，不是新增冻结铁律。

旧 `verify_restructure.py --write` 会覆盖初轮 `verification.json`，因此**保留历史时不要使用该旧写入选项**；直接运行默认只读命令。两个派生生成器的 `--write` 分别重建索引 Markdown，不改台账。当前摘要只是检查输出与哈希的汇集，不改变检查程序的语义，不是新的验收门槛。

## 台账来源与证据层级

**据用户回报**，完整工作区有真实台账，68 个唯一 ID 与规格对上、28 PARTIAL / 40 NOT_RUN；[原命令和结果摘录](../RESTRUCTURE-REVIEW.md#review-fix3-ledger)说明来源。文档交付未收到原账本字节、哈希或逐项状态，不声称独立本地重跑；文档包缺件不等于工作区缺件。

当前文档核对、用户工作区台账回报、人工映射判断和运行证据是四种不同来源。台账集合/汇总已核不代表 fixture、artifact、断言或双客户端观察已绑定；`GAP-MAP-07` 总体仍开放，任何文档 `errors: []` 都不产生 Mod/阶段 PASS。

## V1.3资料的来源与脚本范围

V1.3两份输入为Ronova侧发出稿；[正文](../Ronova_Enhancement_Addendum_V1.3.md)与[交接说明](../../reference/Ronova_Enhancement_Addendum_V1.3_HANDOFF.md)不使用新外部原文块标记。`enhancement-addendum-merge.json`原fix5记录保留，`followup_handoffs`单独追踪fix6。沿用`verify_addendum_merge.py --check`同时核对两轮，`--source-root`可选只读比对原r66；不重用fix5的运行输出冒充新结果。判据仅为原文/分段/来源/目标一致性，接受/修改/废弃及成本选型仍是人工判断。

## 同权限清点的保留与核对范围

fix7原清点正文与zip按输入字节保留在reference/archive中；不会被改成V1.4或新总纲。来源分段、八条动作的原编号、限定源码检索/名单及独立复核记录在原融合JSON的`same_privilege_handoffs`；`verify_addendum_merge.py --check`一并核对，`--source-root`可额外复算所记录的源码检索/名单和摘录。不需要chmod、不执行构建、不读取第三方运行状态。旧V1.3仍严格核其九项回执，不能为容纳八项清点而放松旧检查。

当前摘要继续使用`current-verification.json`，本轮不新造link-audit-fix7或verification-fix7文件。源码词法吻合、名单和引用一致不证明加固方案完整/可编译/可隔离；283个已构建封印class未在本次源码包独立确认，保留为原稿报告值。

## fix8核验补遗与可移植性修复

当前只读工具为修复后的[verify_addendum_merge.py](verify_addendum_merge.py)及新增[test_addendum_portability.py](test_addendum_portability.py)。六处路径统一POSIX、按文本稳定排序、源码显式UTF-8；原fix7的来源hash/词法inventory不改。`verification_ceiling_handoffs`独立保存本轮四输入，前两份的pre-fix7时间关系也保留；不是将旧九条回执改成十条。

`--source-root`明确指向原r66源包解压根；`built_manifest_files_found`基准敏感，不能拿带build/validation的工作区来对原源包却要求忽略差异。核对器仍拒绝错基准。新增测试不需chmod、不起子进程、不修改文件或台账；未提供source-root时跳过源码相关测试并明确报告，不以跳过冒充已测。

本轮七项工具回归在Linux执行，包括PureWindowsPath格式适配与内存负例。**不是原生Windows运行**，也不是Mod/JVM/游戏测试。现行结果放固定[current-verification.json](current-verification.json)，不新增link-audit-fix8或verification-fix8副本。原审计快照全部保留原字节，旧“工具无错误”只对其当时环境负责。


## fix9 · Windows 原生复跑回执

[原始回执](../../reference/R66-FIX8-WINDOWS-RERUN-RECEIPT.md)与[输入ZIP](../../archive/fix8-windows-rerun-receipt-r66.zip)按收到字节保留。回执报告在原生 Windows 进程中，以与记录一致的 r66 源包 SHA-256 为基准执行 `verify_addendum_merge.py --check --source-root ...` 与 `test_addendum_portability.py --source-root ...`：前者 `errors: []`，后者 7/7、0跳过、0错误；其余三个文档检查器亦报告无错误。

本交付环境可独立确认**收到的回执/ZIP字节**及**本地现有 r66 源包 SHA-256 与回执所列基准相同**，但不会冒充重新执行那次 Windows 进程。因而 fix8 的“实际 Windows 复跑仍待用户环境确认”在当前文档状态中已由用户现场回执闭合；这只关闭文档工具的可移植性待确认项，不改变生产源码、`validation/`、68项状态、GAP-MAP-07证据侧，也不产生 Mod/阶段 PASS。


## fix10 · 八方向核验资料与检查范围

新增输入保持Ronova侧发出稿身份，原ZIP/正文逐字节保留；八个方向的标识只在该来源内使用。核对器扩展读取`eight_direction_verification_handoffs`，检查分段、目标、原源码摘录与报告哈希，不将“名字无命中”当语义证明，不校验成本承诺或把方向采纳写成运行完成。

使用原有四个文档核对命令；`--source-root`仍须为原r66源包根。本轮扩展在Linux运行，旧七项可移植性回归保留并复跑；fix9的Windows结果只属于它所列工具版本，不自动覆盖本轮新函数。没有重开已收到的fix8现场确认，也没有新Windows实测回执。

历史link-audit/verification字节不变；固定摘要更新当前输入/检查/负例/链接增量，原68项及GAP-MAP-07证据侧不改。核验正文中对A3等自评计数的歧义只在新回执解释，不回改来源。


## fix11 · 已有基线补遗与工具范围

只扩展原核对器，复用已有来源/分段/方向/摘录核对，另核本轮子系统枚举和限定Java词法引用；新分组保留完整原文。旧八方向回执、历史handoff分组、原ID/章节清单、审计快照与Windows现场回执均不改。

继续使用固定`current-verification.json`，不新增按轮次复制的审计JSON。源码目录清单是机械文件事实，不证明作者约28条契约计数、全部方法语义、运行触达或工量估计；工具无错误只说明所检查的一致性。`--source-root`仍是原r66源包根，不是后续工作区。原Windows回执不自动证明本轮新增检查分支在Windows执行过。


## fix12 · 新增判据效力与机器核对边界

[WIRING-ACCEPTANCE.md](../WIRING-ACCEPTANCE.md)是项目验收分工，不是新最高规范，也不是机器生成的覆盖证明。19行判据正文仍在原接线规划中；批次/方向绑定属于本轮可审阅的项目决定。生成器只给原68项派生索引加分工和反向链接，原validation-traceability.json不动。

当前文档检查会核对原文恢复、派生输出、增补来源和链接；不能证明新绑定语义完备、fixture已存在或工作已完成。仍使用固定current-verification.json和package-manifest.json，不追加一套逐轮审计JSON。不调用旧verify_restructure.py的--write，以免覆盖历史verification.json。
