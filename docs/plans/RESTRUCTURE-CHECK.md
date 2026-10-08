# Ronova MS 骨架重构 · 文档核对记录

**fix12当前核对：** [本轮范围](#review-fix12-check) · [固定摘要](restructure/current-verification.json)。判据正式化不等于运行通过；历史结果只对原版本有效。

**当前核对：fix11已有基线补遗。** [本轮范围](#review-fix11-check) · [固定摘要](restructure/current-verification.json)。下方早期各轮记录只对它们所列输入/环境有效，不自动覆盖当前结果。

**当前核对：** [fix10八方向融合](#fix10-eight-check)；历史记录按其轮次和环境阅读。

**当前核对：fix8。** [本轮范围与结果](#review-fix8-check) · [固定摘要](restructure/current-verification.json)。较早章节仅为对应历史轮次。

> **当前核对入口：** [fix7 同权限清点融合](#fix7-surface-check)；实际结果见固定 [current-verification.json](restructure/current-verification.json)，旧轮次不冒充本轮。

> **前轮核对入口：** [fix6 V1.3融合](#fix6-v13-check)；前轮数据保留其原基线，不作为本轮运行结果。

> **历史fix5核对：** [fix5 增补融合](#review-fix5-check)。旧轮次计数保持原基线，不作为本轮重新执行的结果。

> **前轮核对：** [fix4 接线文档核对](#review-fix4-check)。当前完整结果以 [current-verification.json](restructure/current-verification.json) 所列基线与范围为准；历史结果不自动成为 fix5 结果。

**结构收口记录：** [fix3 收口核对](#review-fix3-check) · [记录效力索引](restructure/README.md)。第 1–8 节为各轮历史核对，不是本轮重新执行的运行结论。

日期：2026-09-14。基线：`ronova-docs-0.2.0-alpha.7.17-r66.zip`。

本记录只描述文档迁移和文件核对。**没有编译 Ronova，没有启动 Minecraft，没有运行功能、专用服或阶段验收，没有据此修改当前实现状态。** 原文中已有的历史状态字样属于原记录，并非本轮结果。

[规划入口](README.md) · [不变量索引](INVARIANT-INDEX.md) · [审阅修订说明](RESTRUCTURE-REVIEW.md) · [章节映射、缺口与适用性](RESTRUCTURE-MAP.md) · [机器可读核对记录](restructure/verification.json) · [本地链接审计](restructure/link-audit.json)

## 1. 实际核对结果

| 项目 | 实际记录 |
|---|---|
| 纳入来源 | 15 份原 `docs/plans/` Markdown，加完整 `docs/DESIGN-SPEC.md`，共 16 份 |
| 原文规模 | 17,752 行、968,757 UTF-8 字节 |
| 唯一正文落点 | 1,047 个原文分段，每段恰有一处主正文 |
| MS 骨架 | skills 35、msitem 23、entitydefence 24，共 82 个二级/三级标题；标题文字、层级和顺序一致 |
| 原文恢复 | 对新增引用包装及标题标记改动逐项逆向恢复，16/16 份与输入 SHA-256 和原始字节一致 |
| 指定 ID 家族清单 | 1,047 次原文出现；按原来源核对拼写、次数与位置，不跨文档合并 |
| 原文引用改动 | 1,368 处引用链接或包装调整；标签原文保留 |
| 当前其他文档 | 7 份同步引用/入口，不重写历史进度 |
| 新规范落点引用 | 1,432 次引用，路径及章节锚点均可定位 |
| 文本引用待定位 | 0 条；这只是定位结果，不代表来源的论证已经复核 |
| 显式锚点重复 | 0 处 |
| 代码/游戏运行验收 | 未执行 |

ID 清单统计 D*、包含前缀的 INV*、A*、R*、ID-*、T-* 的实际文本出现；不把 `D34–D52` 这样的范围凭空展开成并不存在的条文。未落入该检索表达式的其他 ID、条款和示例同样由逐字节恢复覆盖。

## 2. 逐来源恢复记录

“相同”只指逆向恢复后的文档字节与输入一致。完整原文件名、SHA-256、行区间和新位置见源清单与迁移映射。

| 原来源缩写 | 分段 | 原文行数 | 指定 ID 出现 | 恢复与输入字节 |
|---|---:|---:|---:|---|
| INDEX | 5 | 63 | 2 | 相同 |
| BL | 144 | 3,022 | 10 | 相同 |
| V26 | 171 | 3,460 | 67 | 相同 |
| FULL | 22 | 337 | 0 | 相同 |
| TIER | 53 | 833 | 5 | 相同 |
| REC | 25 | 487 | 0 | 相同 |
| MAX | 38 | 381 | 10 | 相同 |
| CF | 95 | 1,695 | 34 | 相同 |
| P0A | 139 | 2,298 | 17 | 相同 |
| L6 | 100 | 1,468 | 256 | 相同 |
| ROAD | 35 | 429 | 4 | 相同 |
| MSC | 30 | 374 | 113 | 相同 |
| MSR | 28 | 414 | 13 | 相同 |
| RULE | 63 | 1,026 | 269 | 相同 |
| NON | 19 | 296 | 11 | 相同 |
| DS | 80 | 1,169 | 236 | 相同 |

快照不是迁移的替代品：上述每个正文段落已经放入三本主规划；快照仅供复原比对。标题文字不改，Markdown 标题级别降为四级；引用的可见标签不改，目标迁入新书稳定锚点。所有可逆编辑写入 `section-map.json`。

## 3. 文件改动边界

原文档包 243 个文件中，220 个保持逐字节不变，23 个 Markdown 文件改为新入口或更新引用；原文件没有删除。

其中 211 个参考资料、隐藏输入、历史快照与验证证据文件保持原字节。三份 MS zh.md、许可证、`.incoming-all-stages-v1.1/` 与其他历史材料均未重写。

源码包里的 Java、Native、资源、Gradle 与构建脚本没有修改；新提供的 Python 文件只用于文档一致性核对，不属于 Ronova 运行时功能。

旧 helper 允许句和旧规范顺序没有从原文中删除：它们按新入口的明确说明调整效力。文档重排没有把实现中的外部进程能力改掉，也没有宣称源码已符合新限制；这是后续代码对照的独立事项。

## 4. 本地链接核对与输入缺件

对 Markdown 真正的链接/图片节点解析后检查本地文件与章节锚点；代码围栏里的示例不当成链接，外部网站未访问。已归档原文保留历史相对链接语境，不以其新归档目录误报迁移损坏。

**本轮未引入新的本地缺失路径或缺失锚点。** 输入原本就有 **519 次缺失文件引用，涉及 368 个不同目标路径**；输出保留同样的 519 次。主要分布在待采纳快照、研究/历史记录与验证证据引用，其中包括输入包未带的附件。

这些原缺件均记录在 `link-audit.json`，没有创建假证据或空占位文件“消除”它们，也没有宣称整个输入包所有历史链接原本有效。新规划及更新后的跨文档落点另经可定位性核对；结果不替代运行验收。

## 5. 可重复核对

在完整解压后的文档根目录，或已合并原 r66 文档的项目根目录运行：

```powershell
python docs/plans/restructure/verify_restructure.py
```

该脚本仅依赖 Python 标准库，只读文档、源清单和快照，逆向恢复原文并检查 ID、MS 骨架、链接目标与原文件改动边界。退出码非零表示核对遇到差异或不能完成，不代表游戏功能成功或失败。

追加 `--write` 会重写 `docs/plans/restructure/verification.json`，仍不会构建或运行 Mod。CommonMark 全包链接扫描在本次环境另行执行，完整记录在 `link-audit.json`；标准库脚本重点复核登记的规范落点引用，不声称复现网络访问或全部渲染器行为。

## 6. 交付内容及使用

完整文档包保留原文档包的未改文件及新增规划/核对资料，可单独阅读。覆盖包只含变更与新增文件，路径从项目根目录起算：应覆盖到**已有 r66 文档的项目根目录**，不是再放进一个 `docs/` 子目录；只有源码包而没有原文档时，使用完整文档包合并。

本轮登记 11 个文档细则/对接缺口、4 个明确不适用事项和6 个有条件适用事项。这些是来源比对记录，不是新增功能的实现宣告。

## 7. 2026-09-14 审阅修订 fix1

新增人读索引与可重复生成脚本：16 份来源，424 个来源内编号条目、929 个位置条目、1,047 次出现；另列 CF / P0-A 各 8 个无编号宪法级名称。这里不是独立语义不变量的去重计数。

明确无参数为默认取向而非第五条硬约束；明确 DS P 层与 C 块的可修订效力，但不自动删除 C12 / C13 或更换完成口径；修正 README 中自有配置、日志和状态持久化的措辞。三本主书没有拆分，只在原文分段外增加入口与说明。

原迁入正文、16 份保真快照、原编号清单和原章节映射未改。重跑原文档核对与派生索引核对，并复核新增本地链接；本次输入差异、实际字节/行数、链接结果和未执行的运行验收范围见 [review-verification.json](restructure/review-verification.json)。

索引命令（只读核对）：

```powershell
python docs/plans/restructure/generate_invariant_index.py --check
```

`package-manifest.json` 已按累计交付内容更新；原文件未删除。上述记录都只针对文档，不为任何 Mod 功能或开发阶段宣告测试通过。

## 8. 2026-09-14 · 审阅修订 fix2

<a id="review-fix2-check"></a>

本节记录 fix1 之后的入口和验收规格对接；第 1–7 节继续作为此前文档核对记录保留。没有改写原运行证据。

### 新增对照的范围

[验收规格双向映射](VALIDATION-TRACEABILITY.md)列出 68 项 B/X 原规格、120 行按来源与章节限定的条款/缺口、231 条条款到用例的关联，以及 MS 六项可见结果与两组各 11 项矩阵共 28 条原文检查；MS 条目到用例的关联为 64 条。这些都是设计关联和阅读入口，**不是实现计数、验收覆盖率或新增通过结果**。

四条原文明引与本轮设计子范围/仅邻近关系分开；九行没有定位直接用例，不强行分配。多部位实体的 MS 原条目未强贴 B/X。B23 原 helper 场景、X66 原五级顺序分别标注旧前提不适用，其他原断言及编号保留。

### 实际执行的文档核对

| 核对 | 结果与边界 |
|---|---|
| `verify_restructure.py` | `errors: []`；16 原来源 / 17,752 行 / 1,047 段 / 82 标题 / 1,432 个登记引用；原文恢复与原 ID 保留 |
| `generate_invariant_index.py --check` | `errors: []`；生成内容一致；424 行编号条目、929 个位置、1,047 次原文出现保持 |
| `generate_validation_traceability.py --check` | `errors: []`；68 项集合、原摘录、来源/原章节/限定 ID、映射目标和生成内容一致；人工关联语义未被自动证明 |
| 新增账本读取器的小型回归 | 10 个文档工具测试，失败 0、异常 0；只读临时合成 JSON，**不是 Mod/阶段/验收 fixture** |
| 本地链接 | 8,720 处，8,201 可定位；原 519 处缺件等量保留，新增问题 0（不访问外部网址） |
| 原 `acceptance-matrix.json` | 三个输入压缩包均未提供；报告 `INPUT_MISSING`，逐项状态未读取，运行证据未绑定 |
| 代码、Forge、游戏及阶段验收 | 未执行 |

相对 fix1：**9 个文件修改、6 个新增、0 个删除**；累计覆盖包仍从原 r66 文档起算。

三本主书逐字节未改；原迁入正文、16 份原字节快照、原 `id-inventory.json`、`section-map.json` 和来源清单保持不变。原文档包的 211 个受保护输入逐字节保持，原 `validation/` 文件、当前进度和历史资料均未修改。

完整本地链接审计、输入差异和本轮实际文件清单分别在 [link-audit-fix2.json](restructure/link-audit-fix2.json) 与 [review-verification-fix2.json](restructure/review-verification-fix2.json)。外部网址没有访问；保真快照保留原相对链接语境。原缺失引用继续记录，不创建空文件伪造证据。

### 可重复命令

```powershell
python docs/plans/restructure/verify_restructure.py
python docs/plans/restructure/generate_invariant_index.py --check
python docs/plans/restructure/generate_validation_traceability.py --check
python -B docs/plans/restructure/test_validation_traceability.py
```

新生成器的 `--write` 只重建 `VALIDATION-TRACEABILITY.md`。没有给 68 项账本写新状态，也没有声明 `GAP-MAP-07` 已关闭。允许依据真实账本 schema 只读输出观察，不会猜字段或修改 `validation/`。


## 9. 2026-09-14 · fix3 收口核对

<a id="review-fix3-check"></a>

本轮基线：`ronova-docs-0.2.0-alpha.7.17-r66-ms-restructured-fix2.zip`。用户工作区台账结果作为**用户回报**另列；原账本未随本轮消息提供，不声称本地独立读取或认证那次运行。

### 本轮核对对象与结果

| 对象 | 本轮记录 |
|---|---|
| `verify_restructure.py` | `errors: []`；16 源 / 17,752 行 / 1,047 段 / 82 个 MS 标题 / 1,432 个登记引用。 |
| `generate_invariant_index.py --check` | `errors: []`，派生内容与生成器一致。索引文件与 fix2 原字节相同。 |
| `generate_validation_traceability.py --check` | `errors: []`，派生内容一致；新增回报记录检查只核对自洽性，不认证外部台账运行。 |
| 主书与设计关系 | 三本主书逐字节未改；68 项规格、120 行条款、231 条关联、28 条 MS 检查与 64 条候选关联的数据未改。9 行无直接用例仍为 9 行，不补造关系。 |
| 原资料和证据 | 211 个受保护原输入、16 份保真快照、原 ID/章节清单、原进度、`validation/` 及源码包不变。五份历史审计 JSON 原字节保留。 |
| 本地链接 | 使用与 fix2 相同的 CommonMark/table 节点口径复算；比较缺件多重集，原 519 次缺件等量保留，新增问题 0。具体总数与方法见当前摘要。 |
| 变更与交付 | 本节不重复抄录易过期的文件计数；当前摘要记录相对 fix2 的新增/修改列表、文件哈希与排除项。没有删除原文件。 |
| Mod / 阶段验收 | 未编译、未启动游戏、未执行功能/阶段验收。辅助解析器临时输入测试本轮也未重跑；其历史结果不提升为本轮结果。 |

当前机器摘要使用固定名称 [current-verification.json](restructure/current-verification.json)，包含本轮命令输出、核对对象哈希、基线和链接增量，不再复制一整套带 fix3 后缀的审计文件。自身及包含自身哈希的交付清单不参与摘要哈希，避免循环自引用；完整交付的内容变化由清单另行记录。

**GAP-MAP-07 的台账 ID 集合/汇总接线子项据用户回报已核，运行证据绑定仍未完成，总缺口仍开放。** 文档重构收口不等于人工语义关联已审完，也不对任何原 B/X 或阶段宣告验收成功。

<a id="review-fix4-check"></a>
## fix4：接线规划更新核对范围

本轮以 fix3 为输入，改动是文档新增及原文块外说明。三本主书全文件字节发生变化，但 1,047 个 RONOVA-ORIGINAL 原文块须与 fix3 完全相同；82 个 MS 二/三级骨架标题、来源快照、原 ID 清单和规范引用映射继续由原核对器验证。

复跑原文核对器和两个派生索引 `--check`；未对其使用 `--write` 覆盖旧核对记录。额外比较 54 键/14 动作提取与源码，比较可识别的编号源码摘录，检查本地链接差异及保护文件哈希。结果和命令记录在固定名称的 `current-verification.json`；这些都是文档/静态核对，不证明语义映射、实际能力、运行触达或游戏完成。

本轮未改 `validation/`、原进度、历史/归档、MS 来源、源码或构建文件。现有台账只有用户工作区回报，未收到原始台账字节；不把汇总生成逐项状态。本轮不执行 Mod 编译、游戏测试或阶段验收。

交付：完整 fix4 文档 ZIP + 相对原 r66 的累计文档覆盖包。累计覆盖包可应用在原 r66、fix1、fix2 或 fix3 文档根目录；若工作区已自行改过同名文档，应比较后合并，不覆盖真实源码/台账。覆盖包条目从 `docs/`、`README.md` 等项目根相对路径开始，不再额外套一层 `docs/`。

### fix4 本轮实际结果

三项命令的 `errors` 均为空；两个派生 Markdown 与其生成器一致。原文恢复为 16 源、17,752 行、1,047 段、82 个 MS 标题、1,047 次指定 ID 家族出现及 1,432 处登记规范引用。

本地链接共 8,894 处，8,375 可定位；原 519 处问题多重集等量保留，新增问题 0。211 个受保护原输入与原字节一致；三书仅导航/原块外文字改变，1,047 个原文块与 fix3 一致。14 组共 671 行可识别编号源码摘录与 r66 源文件逐行一致；54 键/14 动作的枚举与依赖提取完整，不是运行状态统计。

相对 fix3：12 个修改、3 个新增、0 删除。原不变量索引、验收关系 JSON/Markdown、当前进度与两套 link-audit/三套 verification 均保持原字节。源码 618 文件与原 r66 压缩包一致；未编译或运行验收。


<a id="review-fix5-check"></a>
## fix5 增补融合：当前文档核对

2026-09-14；输入为 fix4 完整文档包、原 r66 源码包与本次加强增补交接包。当前机读结果固定为 [current-verification.json](restructure/current-verification.json)，不新增一套按轮次命名的 link-audit / review-verification。

| 核查项 | 本轮结果 |
|---|---|
| 原三个文档脚本 | 原文重构、编号索引和 68 项规格映射均 `errors: []`；两个派生页与各自生成器一致 |
| 新来源保真核对 | 三份原文 **39,404 字节 / 802 物理行 / 52 个来源分段**完整保留；对应目标可定位；没有把重复引用作为新不变量 |
| 本轮原生静态比对 | 扫描 6 个 native 源文件，7 个摘录范围与 r66 相符；词法命中数不冒充调用图或运行获授 |
| 三书框架与原文 | **82 个 MS 标题、1,047 个原文块、16 来源 / 17,752 行、1,432 处登记引用**保持；全部原文可逐字节恢复 |
| 三书改动 | 仅技能书 Native 章节外加一处导航说明；其余两本逐字节不变；所有迁入原文块内容与顺序不变 |
| 受保护输入 | **211 个**保持原字节；原进度、原 validation 内容及历史核对未修改 |
| 本地链接 | 9,057 处，8,538 可定位；原 **519** 处缺件问题多重集保持，新增问题 **0** |
| 相对 fix4 | **11 改 + 7 增 + 0 删**；生产源码、构建与资源没有替换件 |

只读复核命令（从项目或文档根目录执行；`-B` 避免生成字节码缓存）：

```text
python -B docs/plans/restructure/verify_restructure.py
python -B docs/plans/restructure/generate_invariant_index.py --check
python -B docs/plans/restructure/generate_validation_traceability.py --check
python -B docs/plans/restructure/verify_addendum_merge.py --check
```

新来源核对另支持 `--source-root <原r66源码根目录>`，只读比较原生字节哈希、词法记录和摘录。本轮已经用收到的 r66 源码运行该模式；文档包单独运行默认命令时没有实际源码，输出应明确标为未重读，而不是伪造比对结果。

**证据边界：** 新脚本不联网、不启动 JVM、不修改台账或权限；只验证来源保留和目标定位，不证明 52 条融合关系的语义、54 项能力的实现、120 行映射的覆盖、任何 JVMTI 获授或 ThreadEnd 清理效果。AddCapabilities 的外部核对来源另见接线章节；用户现场仍未复现。旧解析器辅助测试本轮未重跑。

交付为**完整文档包**与**相对原 r66 的累计覆盖包**；已核对原 r66、初轮 MS 重构、fix1、fix2、fix3、fix4 的已提供基线应用覆盖后均还原相同文件集合与字节。覆盖包根目录直接含 `docs/`，用于项目根目录；本地额外改动仍需比较合并。SHA/文件清单在 [package-manifest.json](restructure/package-manifest.json)。

本轮没有修改源码/构建/资源/运行状态，没有编译、启动游戏或执行 Mod 阶段验收，也没有增加任何 PASS。已存在的 `GAP-MAP-07` 台账用户回报与待补证据状态原样保留。

<a id="fix6-v13-check"></a>
## fix6 · V1.3九方向融合核对（2026-09-14）

**本轮输入：** 完整fix5、原r66源码快照、`ronova-enhancement-addendum-v1.3-handoff.zip`。新正文与发出说明共2份、41,319字节、699物理行、28个标题分段；Ronova侧发出稿身份保持，不套外部原文块标记。V1.1/V1.2/MERGE原记录不变。接受①⑦⑨、修改②③④⑤⑥、废弃⑧本轮任务是人工设计回执，不是运行证据。

| 检查 | 本轮范围与结果 |
|---|---|
| 原文/ID/框架 | 原16来源、17,752行、1,047块、82个MS标题恢复一致；原ID不重编号 |
| 原三个脚本 | 原文核对、编号索引检查、验收映射检查均`errors: []`；派生内容一致 |
| 增补脚本 | 同一个`verify_addendum_merge.py --check`兼查fix5与V1.3原文、分段、落点及回执结构；可选`--source-root`只读对照原r66 |
| 新源码摘录 | 14段；6组词法搜索。源码哈希/范围见融合清单，不把零命中当调用图证明 |
| 核对器反例 | 内存副本中的重复方向、缺失目标、前缀变动、分段哈希变动均被拒绝；实际原文未改 |
| 文档链接 | 9,183处，8,664可定位；历史519处问题多重集保持，新增0 |
| 改动边界 | 15改＋4增＋0删（相对fix5）；三书新增文字全在原文块外；旧资料与validation无替换 |
| 原受保护输入 | 211个逐字节不变；源码包618个文件与输入一致，未修改源码 |
| 覆盖包 | 相对原r66的累计差异，不含validation或生产源文件替换；七种文档基线均能复原完整fix6 |

本轮未重跑`test_validation_traceability.py`，不将先前沙箱chmod问题冒充已修；新增核对器反例仅是文档脚本测试。未编译、未启动游戏/JVM、未实测新事件/GC/JIT/隐藏类、未更改台账或新增PASS。原68项与120行人工映射保持，GAP-MAP-07证据绑定仍开放。当前结果写入固定`current-verification.json`，没有新增一套link-audit/verification轮次文件。

<a id="fix7-surface-check"></a>
## fix7 · 同权限干预面清点融合核对（2026-09-15）

**本轮输入：** 完整fix6、原r66源码快照及`ronova-same-privilege-surface-handoff.zip`。交接包只有一份只读清点，18,288字节、322物理行、21个标题分段；原文和ZIP均按字节保留。八条动作沿原编号接回现有批次，不新增不变量、Root、SubjectKey或第二套工作线。当前结果固定写入[current-verification.json](restructure/current-verification.json)，不是验收记录。

| 检查 | 本轮范围与结果 |
|---|---|
| 四个文档核对脚本 | 原文重构、编号索引、验收映射及扩展后的增补核对均`errors: []`；两个派生页与生成器一致；本轮增补检查已带`--source-root`只读对照原r66 |
| 原文/ID/MS框架 | 16来源、17,752行、1,047原文块、82个MS标题、1,047次指定ID家族出现与1,432处登记引用保持；三本书全部新增文字在原文块外 |
| 新清点的来源保真 | 1份原文/21个分段/8条原编号动作；来源哈希、范围、目标和独立复核摘录一致，不证明人工处置语义正确 |
| 只读源码复核 | 387个限定文件哈希、6组词法搜索、24段摘录；复算31个Mixin源文件/24个目标及14重合、10不重合；71处显式runtime调用全部在其他包。283个已构建封印class仍只是原稿报告值 |
| 核对器反例 | 内存副本中的7类缺项/重复/假完成/分段/落点/新批次/词法数据变动均被拒绝；没有修改实际来源或验收记录 |
| 文档链接 | 9,285处，8,766可定位；相对fix6的519处原问题多重集相同，新增0 |
| 改动边界 | 15改＋3增＋0删（相对fix6）；新增为原输入ZIP、原清点正文和独立复核依据。更新现有固定摘要/融合清单，不新增一套轮次审计文件 |
| 保留范围 | 211个受保护原输入、原进度、已有归档/历史和验收资料不变；618个源码文件与原输入一致，未改生产源码 |
| 覆盖包 | 相对原r66的累计差异；原r66、初轮MS重构、fix1至fix6共八种已提供文档基线均能还原完整fix7的文件集合与字节 |

只读检查命令沿用上一轮；`verify_addendum_merge.py --check --source-root <原r66源码根目录>`现在还复核同权限清点的源码名单/范围。只提供文档包时可不传源码目录，脚本必须报告未重读，不能冒充验证过用户运行环境。旧V1.3的九项回执要求没有为容纳本轮八条而放松。

**证据边界：** Mixin目标名进入名单不等于逐注入合同验证或当前获授；公开API存在、SHA/MAC方案、封印选择与源码计数不证明可利用性、隔离能力或实际强度。本轮没有编译、启动游戏/JVM、实测agent顺序/新JVMTI能力或写入validation，也未新增PASS。`test_validation_traceability.py`本轮未重跑，前述7个反例仅是文档核对器测试。68项规格/120行候选关系、37个编号空洞及GAP-MAP-07待绑运行证据状态保持；用户工作区真实台账的68唯一ID和28/40汇总继续明确为先前回报，不伪装成本轮读取。

<a id="review-fix8-check"></a>
## fix8 · 核验补遗/天花板融合与文档核对工具回归（2026-09-15）

输入为完整fix7、原r66源码包及本轮四文件交接包。前两份报告的pre-fix7来源、第三份对fix7工具的缺陷定位均按原文保留。当前结果汇总到[current-verification.json](restructure/current-verification.json)，不覆盖初轮/历史verification或link-audit，也不替换运行台账。

### 已执行的文档侧核对

| 核查 | 本轮结果/范围 |
|---|---|
| 四个文档核对脚本 | verify_restructure默认只读；两个索引生成器--check；verify_addendum_merge --check --source-root均errors: []，不是Mod/阶段验收 |
| 旧规划保真 | 16来源、17,752原行、1,047原文块、82个MS二/三级标题、1,432登记引用保持；ID不重编号 |
| 新输入保真 | 4文件、36,289字节、633物理行、43分段（含来源前言），输入zip和正文都按字节核对 |
| 有限源码核对 | 增补核对器只读原r66：新报告记录378个Java文件、5组词法查询、34段原行。8个A调用/13个B调用是词法计数；A均读取观察位的判断附邻接原行，不宣称已跑恢复链 |
| 工具可移植性 | 7项回归无错误：POSIX/Windows纯路径格式与原inventory一致、内存反向注入分隔符错误能够复现、非空build/module路径规范、错基准继续拒绝；不需chmod/子进程，不写源文件 |
| 新输入核对器负例 | 内存中缺交接声明、改输入大小、坏目标锚点、坏摘录4项都被拒绝；未修改磁盘原文/记录 |
| 原同权限inventory | 原fix7数据未改；修的是检查器，未重造被检数据来消除差异 |
| 本地链接 | 9,433处本地链接，8,914可定位；519处原问题多重集与fix7一致，新增0，未消除/隐藏旧缺件 |
| 相对fix7文件变化 | 15改＋7增＋0删；新增源码只有文档核对测试，不是生产Java/C++ |
| 保护边界 | 211受保护原输入、618原源码文件原字节未变；生产源码、构建、资源、CURRENT-PROGRESS和validation均未改；旧来源manifest条目内容保持 |
| 累计覆盖模拟 | 原r66、初轮MS重构及fix1–fix7共9种未自行改动的文档基线；覆盖后文件集合/字节与完整fix8一致 |

**原生Windows复跑未执行。** 七项使用Linux宿主与PureWindowsPath/只读格式适配器，覆盖相对路径拼写、稳定排序、显式UTF-8和记录比较，不证明Windows OS、权限或解压工具行为。原报告提供的是用户Windows上的旧版失败证据；两者不可合写为“Windows已验证”。新增Python工具被测试，不是声称本轮没有执行任何代码。

**基准约束：** --source-root指向原源码ZIP解压根，而非已有build/validation的工作区；built_manifest_files_found仍保留对输入差异的敏感性。缺少真实源码参数时记录“未读取”，不能把纯文档核对写成源码独立复算。

本轮没有编译、启动JVM/游戏、做Mod功能或性能验证；未复跑旧test_validation_traceability.py，不改其环境限制报告。没有回填任何阶段PASS。68唯一ID与28 PARTIAL/40 NOT_RUN继续归于用户此前回报；人工120行关联与GAP-MAP-07证据绑定均未关闭。


<a id="fix9-windows-check"></a>
## fix9 · Windows 复跑回执融合核对（2026-09-15）

**输入：** 完整fix8、原r66源码包与 `ronova-fix8-windows-rerun-receipt.zip`。回执只闭合fix8文档工具的原生Windows待确认项；不修改生产源码、构建、资源、当前进度或validation状态。

| 核查 | fix9结果 |
|---|---|
| `verify_restructure.py` | `errors: []`；16来源 / 17,752行 / 1,047原文块 / 82个MS标题 / 1,432处登记规范引用保持 |
| `generate_invariant_index.py --check` | `errors: []`，派生Markdown与生成器一致 |
| `generate_validation_traceability.py --check` | `errors: []`，派生Markdown与生成器一致 |
| `verify_addendum_merge.py --check --source-root <r66>` | `errors: []`；原52分段/104目标保持；新增Windows回执原文 5,032字节 / 126物理行逐字节保留 |
| 本地 `test_addendum_portability.py --source-root <r66>` | Linux环境7/7、0跳过、0错误；用于确认fix9没有破坏fix8工具修复，不冒充Windows复跑 |
| 用户Windows现场回执 | 报告原生Windows 7/7、0跳过、0错误；fix7同环境失败形态可复现、fix8修复后消失。来源是用户现场回执，不由本地Linux认证 |
| 源包身份 | 本地现有r66源码ZIP SHA-256与回执所列 `1266d8c2...73fd763` 精确一致 |
| 本地链接 | 9,451处，8,932可定位；fix8原519处问题多重集等量保留，新增0 |
| 相对fix8 | 11改 + 2增 + 0删（固定摘要与交付清单计入最终修改数）；三本MS主书逐字节未改 |
| 受保护输入/源码 | 211个受保护原输入保持；618个源码文件与原r66源码包一致 |

收到的回执和ZIP按字节保留；本地可独立匹配回执所列r66源包SHA-256，但不冒充重新执行用户的Windows进程。原16来源、1,047原文块、82个MS标题、原ID和既有人工映射继续由既有检查器核对。

**运行边界：** 本轮本地复跑仅为文档/静态核对；Windows 7/7结果属于用户现场回报。不会因为回执把任何 B/X、阶段或Mod标成PASS，GAP-MAP-07证据侧仍开放。


<a id="fix10-eight-check"></a>
## fix10 · 八方向核验融合的文档核对

基线是完整fix9、原r66源码以及`ronova-second-batch-eight-directions-verification.zip`。本轮只更改规划、来源接线和文档核对工具；不编译Java/Native、不启动JVM/游戏、不改验收状态。

| 核查 | 本轮记录 |
|---|---|
| 新来源 | 1份核验稿，13,814字节/263物理行/14分段；原ZIP、原正文逐字节保留 |
| 来源独立复核 | 26段源码摘录，逐段行号/原文件hash/正文hash核对；S4已有任务图失证、A2已有CAP掩码、S3位累积与A4现有解析器分别记录 |
| 四个文档核对器 | `verify_restructure.py`、`generate_invariant_index.py --check`、`generate_validation_traceability.py --check`、`verify_addendum_merge.py --check --source-root ...`均为`errors: []` |
| 原文迁移 | 16来源/17,752行/1,047个原文块/82个MS二级三级标题/1,432处登记引用保持，原ID不重编号 |
| 工具回归 | 原7项可移植性回归复跑无错误、无跳过；新核验接线的7项内存负例均被拒绝，不修改被测来源文件 |
| 保留边界 | 三本主书新增文字均在原文块外；211个受保护原输入和618个原源码文件保持；原索引/120行人工关系/68项规格及旧增补来源记录未改 |
| 本地链接 | 9553处，9034可定位；与实际fix9用同一解析器对比，原519处缺件问题多重集保持，新增0 |
| 相对fix9 | 15改＋3增＋0删；源码/资源/构建/validation不在替换清单 |

检查器仅确认来源保真、映射/链接、摘录与派生内容一致，不证明八条语义正确、工时、能力可用或验收覆盖。新函数的负例分别覆盖原ZIP hash、分段hash、缺失锚点、改写摘录、改号、假运行声明、源码文件hash。

fix9保留的Windows现场回执仍只适用当时工具/基线。本轮扩展在当前Linux环境执行，不冒称Windows原生再次运行，也不推翻已收到的旧回执。真实台账集合/汇总继续引用用户回报；未收取逐项证据、未做artifact/fixture/断言/双客户端绑定，GAP-MAP-07总体继续开放，无新增PASS。

[当前核对摘要](restructure/current-verification.json) · [八条融合回执](RESTRUCTURE-REVIEW.md#review-fix10-eight) · [原核验来源](../reference/R66-SECOND-BATCH-EIGHT-DIRECTIONS-VERIFICATION.md) · [独立源码复核](../reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#findings)


<a id="review-fix11-check"></a>
## fix11 · 八方向已有基线补遗核对（2026-09-15）

本轮只把Ronova侧已有实现补遗并入原八方向章节、原批次的代码落点与观察要求，并扩展文档核对器。来源原文完整保留，[处理回执](RESTRUCTURE-REVIEW.md#review-fix11-baseline)、[映射](RESTRUCTURE-MAP.md#fix11-baseline-merge)与[独立源码依据](../reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#findings)分开；没有重写fix10八条决定。

### 输入与保真

新输入`ronova-eight-directions-existing-baseline-supplement.zip`内只有一份Markdown；**19,795字节、263物理行、19分段**，ZIP和正文按原字节保留。新增源码依据含**38段、29个文件**的原行摘录；另对七个已声明子系统做机械目录/哈希/首段说明枚举，共124个Java文件记录。目录计数不是逐方法审完或作者“约28条免责声明”的独立证明。

原16来源、17,752行、1,047个迁入原文块、82个MS二三级标题和原ID清单保持。三书只在原文块外增加入口，原文块内容和顺序相同。原来源快照、历史/归档/验收输入、旧handoff分组、54键/14动作静态基线、原120行关系、两个派生索引和真实台账状态均不改。源码ZIP与618个解压源文件逐字节比对；它们不在文档覆盖包的替换范围。

### 实际执行范围

四个文档检查命令：原文保真核对、两个派生生成器`--check`、增补核对`--check --source-root`，最终结果记录在固定摘要。保留默认只读模式，不用旧`verify_restructure.py --write`覆盖历史核对JSON。旧七项可移植性回归在Linux复跑；新分组另做八项内存负例：缺补遗、缺旧报告、错来源哈希、错目标锚点、改方向编号、伪运行声明、错子系统hash、错词法行，均应被拒绝。POSIX与PureWindowsPath拼写作内存比较，不是原生Windows运行。

新函数复用原八方向来源保真检查，不替换旧fix10分组；新增枚举按`.as_posix()`路径排序、显式UTF-8，保证不再次用宿主分隔符作为比较身份。原fix8 Windows确认仍由fix9收到的现场回执闭合，但不自动认证本轮新函数在Windows运行过。

运行示例（项目根目录；源码参数必须是原r66源包解压根，不是修改后工作区）：

```text
python -B docs/plans/restructure/verify_restructure.py --root .
python -B docs/plans/restructure/generate_invariant_index.py --root . --check
python -B docs/plans/restructure/generate_validation_traceability.py --root . --check
python -B docs/plans/restructure/verify_addendum_merge.py --root . --check --source-root <原r66源码根>
python -B docs/plans/restructure/test_addendum_portability.py --root . --source-root <原r66源码根>
```

### 交付与未做事项

本地链接用相同CommonMark/表格解析器重新扫描fix10与fix11，忽略原文快照的旧相对路径语境，按source/target/resolved/reason多重集比较；外站不访问。当前摘要记录确切计数、是否新增问题，不用消音或补假文件消除旧缺件。

完整包和累计覆盖包逐基线做文件集合/字节比对；覆盖包相对原r66，只覆盖项目根的文档/只读核对资料，不提供`src/`、`native/`或`validation/`替换件。本地自行改过同名文档应比较合并，不把受控压缩包兼容性扩大成任意工作区无损覆盖。

**未编译、未启动JVM/游戏、未测试运行强度/同权限攻击、未改生产源码或验收状态、未新增Mod/阶段PASS。** 文档无错误不证明映射语义完整；120行人工关系与GAP-MAP-07证据绑定继续开放。确切最终输出、改动边界和文件hash见[current-verification.json](restructure/current-verification.json)。


<a id="review-fix12-check"></a>
## fix12 · 验收分工文档核对

输入：完整fix11文档ZIP。改动仅为新增验收分工页、原19行观察点的稳定锚点/效力说明、双向导航和派生索引生成器的说明段；不改运行代码或台账。三本MS主书及原观察行的断言文字保持。

本轮实际检查结果在固定[current-verification.json](restructure/current-verification.json)。检查器只证实文档保真与一致性；新增人工绑定的语义正确、场景覆盖与运行效果不是机器已证明的事实。文档基线与所声明测试产物/fixture并未因此完成绑定。

| 实际核对 | 结果与限制 |
|---|---|
| 四个文档核对器 | 均为 `errors: []`；两个派生索引与生成器一致，增补核对使用原r66源包解压根只读比较。 |
| 生成器解析回归 | 原10项单元测试无失败；它们验证台账解析边界，不证明验收关系语义。 |
| 原可移植性回归 | 7项、0跳过、0错误；Linux宿主，不冒充本轮Windows原生复跑。 |
| 原观察表与54键表 | 19行原断言内容/顺序保持，仅增加导航锚点；54键表原文保持，其中39行清点待办未改。 |
| 新绑定结构 | 原六个批次位置、八方向均有观察点入口；19行均有分工入口。Markdown表格按实际解析核对，无空行拆表。语义仍是项目决策，不是机器证明。 |
| 原重构与受保护输入 | 16来源、17,752行、1,047原文块、82个MS标题保持；三本主书逐字节未改；211个受保护原输入与618个原源码文件保持。 |
| 本地链接 | 9,815处，9,296可定位；原519处问题多重集等量保留，新增0。外部网页未重新访问。 |
| 相对fix11 | 12改、1增、0删；新增文件仅为 `WIRING-ACCEPTANCE.md`，不增加生产或validation文件。 |
| 打包 | 完整包与累计覆盖包均按文件字节核对；原r66、初轮MS重构、fix1至fix11共13种文档基线应用累计覆盖后等同完整交付。 |


原68项JSON及120行关联、原进度、受保护历史/来源保持原字节。原台账的68唯一ID/28 PARTIAL/40 NOT_RUN仍是用户回报，未独立复跑真实台账。GAP-MAP-07证据侧、原人工映射审阅与39键清点继续开放。本轮没有编译、启动JVM/游戏、原生Windows复跑、修改validation或产生Mod/阶段PASS。
