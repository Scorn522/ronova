# 第一阶段实施接续

第一阶段仍未完成。本文件记录实现与验证接续；参考研究不改变阶段验收结论。不使用子代理。

## 最新：Native 文件事务能力实测（2026-09-10）

新增独立 ctypes 探针，仅操作新建的 workspace 测试文件。实测事务未提交视图隔离、6800 竞争
替换拒绝、提交、回滚以及文件 ID 一致性通过。
[结果](../validation/7.17/native-file-transaction-probes/bf5ccd7f85c448039cb9339c527bb836/result.json)
标记 production_evidence=false；没有把探针接为默认存储实现。

TxF 的未来可用性有微软明确警示；普通按路径替换也没有本项目所需的预期 File ID 比较参数。
后续需确定受支持的提交语义和能力不足出口，并接入生产 Root/Native 执行、独立读回与故障证据。
不得把探针通过算作 Native 最终门或第一阶段完成。

## 最新：Root 存储执行许可正例（2026-09-10）

StoragePreparationResult.acquireWrite 接入原 ACK、来源、新鲜见证和观察证明，并持有既有
RootExecutionFence 存储 lease。phase1-storage-permit-types 第二轮验证 clear 排斥、线程隔离、
关闭撤销和零效果事实，通过。[证据](../validation/7.17/phase1-storage-write-permit-evidence.json)。

首次 phase1-storage-write-permit 运行的 OBSERVATION_CHANGED 超时保留；固定类型提前加载后
通过。下一步是 Native 准确文件提交及其最终条件复验，而不是重复证明 ACK。本轮没有写外部文件，
完整停服/跨线程释放、提交故障、重启与稳定完成仍待完成。

## 最新：Root 存储意图 ACK 已获生产实机正例（2026-09-10）

phase1-storage-confirmed-intent 第二轮通过：GC 后候选仍有效，真实复制/工厂见证驱动持久
STORAGE_COMMIT 意图，重复查询保持原记录及 attempt，不产生效果事实。保存套件第二十三轮
替换和失效回归通过。[证据和检查点](../validation/7.17/phase1-root-storage-ack-evidence.json)。

修复点：写入返回前确认实际引用映射，保留保存代号/哈希结论；不在后续等待期间依赖临时对象。
已 ACK 的意图先按原 key 查询，历史查询不冒充新执行许可。之前的失败日志保留。

下一步仍为最终存储执行门与提交、重启重新解析原来源/意图、资源与稳定完成。生产流程仍由
夹具启动；本轮不是第一阶段收口。

## 最新：见证等待和候选失效诊断（2026-09-10）

本轮三个独立运行正例均未通过，日志保留：observation 轮已取得工厂见证，diagnostic 轮明确
OBSERVATION_UNAVAILABLE（策略允许），load-order 轮失败于等待期间候选失效。
入口新增 intentStatus；指纹初始化移到请求见证之前；夹具保留原见证，不延长其时间戳。
[证据](../validation/7.17/phase1-storage-intent-observation-evidence.json)。

下一步细分 storagePreparationCurrent 的失效原因，并完善正确的重新准备流程；不得绕过失效。
尚无生产存储 ACK 正例，Root 最终文件提交、重启和完成判定继续未闭合。

## 最新：条件性 Root 存储意图接线，运行正例失败（2026-09-10）

StoragePreparationResult.prepareIntent 内部构造描述，检查新见证、当前来源、候选和 Root ACK；
StorageAdapterFingerprint 是制品兼容指纹，不替代执行代码证明。Root 自测及构建通过。
phase1-owned-storage-intent 第一轮策略准备通过，第二轮因实际 copy/factory 未获验证见证失败；
未证明生产正例 ACK 或同意图重试。[证据](../validation/7.17/phase1-storage-root-intent-evidence.json)。

下一步从复制资格与 Native/边界观察诊断定位，不降低 observerVerified 条件。最终写入与重启
仍未实现，本轮失败不得算作预期负例通过。

## 最新：有来源归属的交付句柄（2026-09-10）

takeStoragePreparation 返回内部构造的 StoragePreparationResult。裸字节不承载权限；句柄
保留来源、保存观察和资源，value/current 重新检查来源，维护/close 清理已交付结果。
第二十二轮实机验证交付后保存使句柄失效并拒绝访问；原回归通过。
[证据](../validation/7.17/phase1-owned-storage-result-evidence.json)。

Root 存储批准接口本轮未接通，仍需写入资格证明、持久意图与最终门。句柄不等于写许可。

## 最新：真实编码前的替换验收（2026-09-10）

独立夹具 Mixin 仅注入故障，不注入证明：扫描后，实际编码前，由自有线程替换精确选中的 seed
引用为同 UUID 的无关对象。第二十一轮证明生产实际路径与扫描路径失配时拒绝准备。
[证据及第二十轮夹具失败解释](../validation/7.17/phase1-writer-replacement-evidence.json)。

生产 JAR 沿用 writer-references 检查点。该范围不等于编码中途任意变更、代码资格或最终门验收；
Root 存储提交、重启执行和稳定完成仍未闭合。

## 最新：写入调用中的引用路径（2026-09-10）

NbtWriteReferences 由 CompoundTag / ListTag 实际写入调用驱动，快照随 EncodedWrite 发布；
SourceTracker 用实际身份对应路径复验保存前位置。三个专项自测和专用服第十九轮通过。
[证据](../validation/7.17/phase1-writer-references-evidence.json)。

该观察尚未完成并发替换负例或编码代码资格证明；弱引用失效时不能批准准备，Root 最终提交和
重启执行仍待接通。不能把纯路径对应关系扩大成完整编码权限证明。

## 最新：摘要撤销与交付检查（2026-09-10）

SavedDataEncodedWrite 在写入/刷新/关闭异常时撤销同代号摘要；跨线程和迟到写入也撤销。
新自测覆盖吞异常、跨线程、迟到写入、新代号隔离、乱序关闭清理。SourceTracker 准备有效性
要求摘要仍存在。三个专项自测及专用服第十八轮回归通过。
[证据](../validation/7.17/phase1-encoded-write-revocation-evidence.json)。

新异常注入仅为自测证据；并发来源引用到编码证明、最终提交门、Root 存储执行与重启仍待完成。

## 最新：后台准备绑定真实保存字节（2026-09-10）

SavedDataEncodedWrite 在原压缩输出流计算有界摘要；SavedDataSaveTrace 按代号保存临时观察。
SourceTracker 准备入口要求当前摘要，StorageImageTask 的 Native 基线必须匹配才生成候选。
三个专项自测和专用服第十七轮通过，包含错误摘要无候选与旧保存回归。
[证据](../validation/7.17/phase1-encoded-write-binding-evidence.json)。

第十六轮只验证输出流摘要，第十七轮另验证后台基线匹配。初次构建的夹具类型转换编译错误已修正，
重试及最终构建通过；未运行失败构建产物。仍不能把摘要当作编码因果完整性或持久提交收据，
后续需并发来源证明、Root 意图与最终写入门、保存重启接续和稳定完成。

## 最新：已准备结果的资格撤销通过（2026-09-10）

新增 --storage-preparation-clear 独立运行选项，使用专用世界；第一轮设置策略，第二轮通过。
生产候选 READY 后 Root clear 成功，旧结果无法交付；重新资格获得新 epoch，旧 SourceKey
仍被拒绝，历史效果记录不变。[证据](../validation/7.17/phase1-storage-clear-evidence.json)。

这不是存储最终写入门验收，也不证明保存重启原意图接续。Root 批准接线尚缺合格的新因果见证
与冻结编码关系证明，不能用历史绑定或 READY 状态代替。运行中失效和 holder 替换仍待覆盖。

## 最新：已完成候选在保存变化后拒绝交付（2026-09-10）

StorageImageTask.READY 只表示线程已经结束且保留非空候选，不表示来源有效或允许提交。
专用服第十五轮确认 READY 后执行真实 SavedData 保存，旧代号候选无法转交，旧输出已丢弃，
后续新准备正常交付；专项自测及原保存/读回断言通过。
[本轮证据](../validation/7.17/phase1-storage-stale-handoff-evidence.json)。

下一步仍需资格撤销、holder 替换/卸载和运行中失效；不能把保存代号负例替代这些用例。
准备入口仍由夹具调用，Root 存储执行器、最终门、重启接续及稳定完成尚未闭合。

## 最新：生产后台基线读取与候选准备通过（2026-09-10）

来源跟踪器新增准备请求和结果接收入口，检查当前来源、保存代号和位置，并先预留工作/结果资源。
自有后台线程只接收路径、位置和限额，执行 Native 读取及候选生成；服务器线程复验后才能转交结果。
并发操作最多两个，每个跟踪器最多保留两个准备项。结果预留随结果转交，消费者关闭后才释放。

专项自测和隔离专用服第十三轮通过：取消不会中断线程或把活线程当作已退出，工作阻塞期间保留
资源，失败与取消后容量恢复，结果转交后预留不被任务关闭提前释放。实机覆盖生产请求、后台读取/
生成、服务器线程复验和结果关闭，旧保存/Native 回归保持。
[后台准备证据与检查点](../validation/7.17/phase1-storage-image-worker-evidence.json)。

这仍是准备入口，当前由夹具发起并接收；自动整链编排、Root 存储意图、最终提交和完成收据未接通。
工作预留在工作线程收尾时释放，结果交付另要求线程实际结束；五秒时限为协作式，阻塞 I/O 不会被
伪报退出。全部资源归属、编码资格证明、重启执行及稳定完成仍需继续，第一阶段保持未完成。

## 最新：生产 Native 基线捕获接入候选生成（2026-09-10）

基线字节、哈希和文件身份现在来自同一个 Native 读取过程；新增 OBSERVED_IMAGE 区分基线观测
与预期图像匹配。只有读取完整且取得身份时才交出冻结字节，预算中断不暴露部分基线，返回数组为副本。

隔离专用服第十二轮通过：去掉夹具 Files.readAllBytes 作为基线输入的步骤，由生产捕获结果驱动
已有引用位置、候选生成和保留验证，旧 Native 替换/退出回归保持。
[基线捕获证据及检查点](../validation/7.17/phase1-native-baseline-capture-evidence.json)。

阶段串联目前仍由夹具驱动；生产资源准入/异步调度、引用位置到冻结编码的资格证明，以及 Root
意图准备、最终提交门和收据尚未闭合。OBSERVED_IMAGE 不会替代匹配或完成判定。
第一阶段和整体验收状态保持未完成。

## 最新：生产位置驱动的冻结候选图像生成通过（2026-09-10）

新增字节级生成器，使用捕获位置删除 Compound 条目或 List 原始索引，调整对应列表长度；
未选中的原始 NBT 字节直接保留，不重放第三方保存器，也不重编码无关字段值。
生成前后均校验结构，并冻结输出字节、原图像哈希和预期图像哈希；损坏/额外 gzip 成员、重复键、
负长度、缺失/重叠位置和预算超限均不返回半成品。支持的 gzip 头元数据也保持。

专项自测及隔离专用服第十一轮通过：候选不再由夹具硬编码删除，生产生成器直接使用捕获位置；
保留校验、未知字段及 Native 读回回归保持，源文件未被候选生成过程改写。
[冻结图像证据与当前检查点](../validation/7.17/phase1-frozen-storage-image-evidence.json)。

全部 260 个必要类已纳入代码封印；类数上限从 256 调至 512，总字节上限仍为 4 MiB。
Native 按生成数组长度遍历，清单的 8 项缺失/重复/预算等测试通过，没有排除必需类。

**候选还不是获得 Root 授权的存储提交。** 基线字节仍由夹具读取，来源资格、输入引用位置与冻结
编码图像的证明尚未合并；Root 意图创建、受资源治理的异步执行、Native 最终提交与最终收据仍待完成。
第一阶段和 68 项整体验收状态保持未完成。

## 最新：保留校验使用生产捕获的写入输入位置（2026-09-10）

SavedData 真实写入入口现在按实际 holder 路由到来源观察器，在服务器维护线程上识别源对象引用
或已验证复制来源的位置；不按 NBT 内容/UUID 相同推断归属。只读取已登记的标准容器布局，
未知 backing 不执行额外容器回调。扫描设有节点/深度/位置上限、协作式时间检查及 holder 内轮换。
结果仅在相同成功写入代号下提供；预算不足、异常、循环等不发布部分位置。

专项自测和隔离专用服第十轮通过：保留验证从生产观察器取得 data/seed 位置，独立同 UUID NBT
未被连带；旧保存失败、覆盖 no-op、Native 同内容替换和句柄退出回归保持。
[生产引用位置证据与检查点](../validation/7.17/phase1-production-encoding-paths-evidence.json)。

**这些是写入输入的引用位置，尚不是冻结编码字节或存储操作权限证明。**
候选删除动作本身仍由夹具构造，工作线程编码捕获和大型图像覆盖仍未完成；下一步需把位置与
实际编码图像、Root 原意图、最终写入门和收据关联。第一阶段及 68 项整体验收状态不变。

## 最新：候选 NBT 图像的无关记录/未知字段保留验证通过（2026-09-10）

新增有界比较器，只允许移除给定的目标编码位置；其他已解码 NBT 值必须保持。
支持 Compound 键与 List 原始索引，拒绝目标仍在、无关修改、位置缺失/重复/重叠、循环和预算超限，
浮点 NaN 的位模式变化也不能混为相同。比较器不修改输入、不按 UUID 推断归属、不授予写许可。

专项自测与隔离专用服第九轮通过。实机从真实保存图像构造私有候选，目标位置删除后无关记录及
未知字段保留；损坏未知字段的候选被拒绝，规划没有改变磁盘文件。已有 Native 读回和退出回归保持。
[保留验证证据与检查点](../validation/7.17/phase1-nbt-preservation-evidence.json)。

**这是条件性的图像校验，不是存储链完成。** 删除位置仍由夹具提供，尚缺生产来源到编码位置的
证明、受预算约束的私有解码/预期编码图像生成、Root 存储执行器和 Native 最终提交门。
解码值相同也不等于原始编码字节全部保持；尚未写入 PRESERVATION_VERIFIED/READBACK_VERIFIED 收据。
本轮构建仅在 --offline 命令中跳过 ForgeGradle 启动 HEAD 预检查，未修改持久配置或 TLS 传输校验。
第一阶段及 68 项整体验收状态保持未完成。

## 最新：读回查询已绑定 Root 原存储意图（2026-09-10）

新增查询层从已 ACK 的原 STORAGE_COMMIT 描述取得预期图像哈希；读前后核对同一条 Root 记录。
ACK 缺失或任务意图不进入读回；查询中原记录不可用、返回哈希不符或缺少文件身份均不能作为匹配。
SourceTracker 的实时入口另核对来源位置、登记身份、来源版本、目标 epoch 和原 command；
查询本身不创建新意图，不授予写许可，也不追加 READBACK_VERIFIED 或完整完成收据。

DurableRootStoreSelfTest 通过：原意图在真实后端重开前后都按持久描述查询，原 attempt/效果不变；
现有 schema 迁移、旧 hash/ONCE、clear、ACK 与子 JVM 崩溃窗口回归保持。
[Root 查询证据与当前构建检查点](../validation/7.17/phase1-root-storage-query-evidence.json)。

这些 Root 查询测试使用模拟读回结果，不能替代生产存储链实机验收。实时来源入口尚未完成游戏
内联验；重启后把旧登记及保存上下文重新解析到当前 holder 也仍需实现。
第一阶段继续推进预期编码图像生成、受支持存储 executor、最终提交门、未知字段保留和最终收据。
本次没有新增整项 PASS，68 项验收状态保持 28 PARTIAL / 40 NOT_RUN。

## 最新：Windows 同句柄读回正例与替换/退出故障通过（2026-09-10）

Native 现在从同一内容句柄读取完整文件字节、卷标识和 128 位文件 ID，并核对读前后大小、
写入时间和路径当前指向的文件身份。正常图像可得到 OBSERVED_MATCH，错误哈希为 DIFFERENT_IMAGE。
读取不排斥其他使用者的写入/删除；路径替换或已观察到的内容变化保留未决。

隔离专用服第七轮和当前检查点第八轮通过。第八轮覆盖：相同内容的新文件替换被拒绝、恢复
原文件后身份恢复、预算中断不产出完整哈希、回调异常原样传播，以及各退出路径后独占重开文件。
旧 SavedData 序列化/写入异常和覆盖 save(File) 的 no-op 回归也保持通过。
[Native 读回证据、产物与源码哈希](../validation/7.17/phase1-native-readback-evidence.json)。

**关闭的是当前测试盘上的 Native 同句柄图像读回缺口，不是整链保存验收。**
预期编码哈希仍由夹具提供；尚未接入 Root 原存储意图的预期图像、最终写入门、强制持久提交和
未知字段保留证明。SourceTracker 的来源/资格复验及临时资源预留入口已编译，完整生产联验仍待完成。
同步文件读取也尚未成为带硬时限的异步存储任务。第一阶段和 68 项整体台账状态保持未完成。

## 历史：Java 读回缺少 Windows 文件身份（Native 接线前，2026-09-10）

保存 observer 记录实际写入入口的路径；新增读回工具只读这一指定文件，限制为 16 MiB、
16 KiB 分块并逐块核对预算，保留文件缺失、非普通文件、超限、中断和读取期间变化等状态。
SourceTracker 已增加当前来源/资格/绑定及保存代号的读前后核对入口；它不写 Root 完成事实。

**该轮 Java 路径未通过正向读回；后续 Native 证据见上方。** 当前 JDK 的 fileKey() 返回 null，首个正例测试因此失败；
现已明确分类为 FILE_IDENTITY_UNAVAILABLE。后续边界测试证明保持未决，不替代原正例要求。
独立专用服第六轮确认生产路径捕获、实际文件打开、身份未知不升级成功及旧保存故障回归。
实机预期文件哈希由夹具提供，尚非 Root 原意图下的预期序列化图像，不能据此登记 READBACK_VERIFIED。

[读回边界证据和缺口](../validation/7.17/phase1-readback-boundary-evidence.json)。
下一步需实现 Native 文件句柄身份及最终提交门、受资源治理的异步存储执行、Root 预期图像与
独立读回/未知字段保留证明。字节预算不等于阻塞文件 I/O 的硬时间上限。
该轮尚未完成 Windows 正向读回；它的失败证据保留，后续 Native 通过不改变第一阶段未完成结论。

## 最新：SavedData 实际保存边界与故障观察已接入（2026-09-10）

生产来源发现为实际 SavedData holder 启用有界保存观察，分别记录序列化返回、写入尝试、
写入返回、异常和外层返回。正常写入返回不代表持久提交、保存内容保留或独立读回。
新增实现纳入代码封印；没有改写 Root 语义、第三方保存行为或原版 dirty/异常处理。

- 专项自测通过：空记录、clean no-op、被吞 IOException、序列化异常、重叠调用及旧不可变快照。
- 隔离专用服第二轮及重启第三轮通过：来源由生产历史定位器绑定；实际文件写入失败仍保留失败，
  序列化异常原样传播，正常写入返回与 clean no-op 分离，旧 Root 意图不变。
- 夹具只建立历史定位提示和故障条件；保存事件来自实际生产 Mixin，不注入虚构保存回执。
- 该记录是诊断层最新已观察调用；使用时必须核对调用前后 generation。未覆盖的 save 重写不能
  沿用旧快照。尚未证明代码资格、准确路径、未知字段保留或磁盘持久性，不能写入 FILE_COMMITTED。

[构建、源码哈希与实机清单](../validation/7.17/phase1-saved-data-save-trace-evidence.json)。
**第一阶段仍未完成。** 下一步接 Root 原意图下的存储 executor、最终写入门、独立读取及保留性验证；
任务后继、资源结算、稳定完成和旧取消超时原因继续保留。68 项验收状态未变。

保存观察 API 进一步要求调用前快照与调用后快照属于同一 trace，且 generation 恰好增加 1。
不同 holder 的同代号、跨越多次保存、第三方覆盖 save(File) 后直接返回，均不能复用旧记录。
专项自测与重启专用服第四轮通过；真实写入异常、序列化异常和 clean no-op 回归保持。
[精确调用对应证据](../validation/7.17/phase1-save-call-generation-evidence.json)。
trace UUID 仅为诊断实例标识，不授予来源资格，也不成为第二份主体身份或权威数据库。

Native 产物差异已定位并修复：上轮两份 JAR 仅 DLL 的 PE 时间戳不同。
构建现在归零新生成、未签名且无校验和的 DLL 时间戳；其他字节不变，带签名/校验和则拒绝处理。
相同输入的 Native 重链与封装哈希一致，保护性测试和当前产物的重启保存故障第五轮均通过。
[构建可重复性与当前实机证据](../validation/7.17/phase1-native-reproducibility-evidence.json)。
这是构建证据修复，不新增整链验收通过项；外部存储 executor、最终提交和独立读回仍待实现。


## 最新：任务事件队列回归已在测试范围内修复（2026-09-10）

生产 observer 已接入有界、保序的连续任务事件合并；队列仍为 512 个槽位。
只合并相同任务、父任务、调度器与证明版本下的准确 token 配对；复制或来源变化切断合并。
10,000 次周期配对自测及七类 JDK 字节码检查通过。

- 原失败的 JDK 任务回归第六轮通过：进入/退出各 10 次，inbox_full=0。
- 裸 CallerRuns 第六轮及重启后的异步 worker 连续复制第七轮通过。
- 周期任务取消诊断版第五轮通过；第四轮零取消尝试超时的原因仍未解释，保留原失败。
- 上述结果分别绑定原产物哈希；不代表外部保存、资源释放或稳定整链完成。

证据见[任务事件合并清单](../validation/7.17/phase1-task-event-runs-evidence.json)。
**第一阶段仍未完成，68 项验收保持 28 PARTIAL / 40 NOT_RUN。**
下一步追查取消超时分支，继续后继处置、外部保存读回和稳定完成链。

已补充固定容量的任务候选拒绝原因计数、当前登记身份、证明年龄及队列延后次数；
诊断不保留任务/holder 引用，不放宽资格、证明时效或 Root 许可。
周期任务第六轮（原诊断产物）与第七轮（新增候选诊断产物）均通过，
见[候选诊断构建与实机清单](../validation/7.17/phase1-task-candidate-diagnostics-evidence.json)。
第四轮失败原因仍未查明；本轮没有新增整项 PASS，也未实现外部存储 executor。

## 历史施工：裸 CallerRuns 与修复前的任务队列回归（2026-09-10）

- 新增第七个完整 JDK 字节码适配器 `ThreadPoolExecutor$CallerRunsPolicy.rejectedExecution`，
  只包围真实 Runnable.run；Java bootstrap 与 Native 增加对应边界位 64。
  有局部变量访问能力时读取 CallerRuns 参数槽 1；无此能力时仍用经过核验的 Native 入栈接收者。
- 中间产物 `942702320c983843e05acd3c693ac4ed9941cac5d59ffe470fb16fae7a021ca3`
  通过 `phase1-caller-runs/round-4.json`（裸 Runnable、连续复制、异常退出、shutdown 拒绝）
  和 `round-5.json`（原 FutureTask 嵌套回归）。它不是阶段候选，也不证明裸任务取消或同步 CF 后继。
- **相关 JDK 回归仍失败**：`phase1-jdk-seven/round-1.json` 至 `round-5.json` 均超时。
  旧检查点在 `phase1-jdk-before-raw/round-1.json` 通过。不能用上述局部通过覆盖这项回归。
- 已增加有界原因计数及本地队列诊断。第五轮 checks=9、periods=4、work 全完成、周期已取消，
  但本地 inbox 丢失 3742 个事件；receiver/proof/capture 异常为 0。图中缺失退出事实并保持 UNKNOWN。
  主线程转储显示探测 worker 已空闲，问题落在观察记录承接而非任务体未执行。
- 已将 stale 维护与恢复观察改成轮换取得同一预算；单独这个改变没有解决超时，不能登记为验收通过。
  最新代码还包括诊断和 Native 槽位修正，其构建日志为 `build-phase1-task-capture-diagnostics.log`；
  不把早先的 raw 中间产物证据移用到最新代码。
- 下一步：实现有界、保序的重复任务事件批量承接/公平队列，保留真实 token 配对、来源复制事件和
  证明版本失效；不通过扩大无界队列、丢失后清零 UNKNOWN 或降低退出断言取得通过。
  修复后先跑原 JDK 回归，再回归 raw/Future CallerRuns；之后继续同步后继、保存及整链闭环。

调查入口：[原结果、对照和源码哈希](../validation/7.17/phase1-jdk-seven/investigation.json)。
当前所有本轮构建/专用服进程均已退出，未增加整项验收 PASS，goal 保持未完成。

2026-09-10 用户要求将可采纳的研究结果放进现有 Markdown，现已写入
[恢复链规划第 12 节](plans/Ronova_7.17_Generic_Recovery_Chain_Implementation_Plan_V1.0_Internal_Root.md)
和[极限加强规划第 8–9 节](plans/Ronova_7.17_Extreme_Full_Plane_Enhancement_Plan_V1.0_Internal_Root.md)。
M01–M14 覆盖通用加强、固定接入点、阶段与正负例；第一阶段优先补来源缓存/旧对象、完整任务后继、
最终提交、外部保存读回及资源/稳定完成。这些是必须接续的要求，不是新一轮生产通过记录。

2026-09-10 用户提供 SuperSteve 反编译包用于学习，静态分析见
[源码学习与 7.17 对照](SUPERSTEVE-6.9.1-STUDY.md)。重点补入已有验收范围的具体场景：
共享裸线程、旧对象重插、复制容器后迟到发布。它们尚未被现有 Future/NBT 证据覆盖；
此次学习未执行附件代码，未改变验收通过数。

同日继续对照 ArcaneVorte 0.5.6、永爱之刃 2.6 fix / 3.2 及 Native 还原包：
[机制对照与通用场景](RESTORATION-MODS-COMPARATIVE-STUDY.md)。新增具体场景包括静态快照共享引用、
旧回调误改新登记、实体 ID 临时许可与迟到清理、布尔返回值不代表实际效果、隐藏定义回退、
观察器覆盖失效。均为场景设计，尚未运行；Native 受保护接口不冒充已还原实现。
原文索引和静态字节码证据位于 `validation/7.17/reference-study/`，不进入生产构建。

Native 第二轮见 [独立结构检查与调用语义](NATIVE-2.6-FOLLOWUP-STUDY.md)：
独立核对 11 个 PE 的导出/RVA；核心两个库的 153 个命名导出仍无入口磁盘字节。
原 JAR 字节码确认参考样本的恢复包装会吞掉失败，比较器未比较操作数；
自建 `bipush 7` / `bipush 9` 反例已编译与反汇编，未执行附件代码。
新增场景仍须由 Ronova 自身验收，不增加通过数。

用户后续要求全面研究四个 Mod 版本，最新工作见 [全部 Mod 研究接续](ALL-MODS-RESEARCH.md)。
已完成全量字节码索引及重点行为专题；研究与文档采纳没有将第一阶段标成完成，
也未改变该 goal 状态。任务的即时运行状态不由本文件中的历史描述决定。

## 已验证的检查点

- 完整构建基线：validation-12，85 项自测。
- 新增 observer 实机快照：`validation/7.17/phase1-checkpoints/ronova-7.17-observer-checkpoint.jar`，
  SHA-256 `049acb5689cedbf094fba4d256fe3b95d05e8867e9681a083253bb2ed964063f`。
- JDK 任务生产捕获/合并：`validation/7.17/phase1-jdk-task-bridge/round-11.json`。
  Runnable、Future、周期任务、ForkJoin/异步 CompletableFuture 通过；最后 9 次进入/退出配对。
- 异步 NBT 来源和真实工厂：`validation/7.17/phase1-async-copy/round-4.json`。
  生产任务/来源关联、换 UUID 工厂拦截、无关复制放行、旧效果不改写通过。
- 最新源码另通过 `build-phase1-root-work-kind-tests.log`：Root schema 12 / effect schema 3、
  新任务/存储计划与意图、ACK 失败、许可隔离、旧语义、重启未知、clear 撤销及相关旧 Root 回归。
  后续普通 Future/周期任务取消已经实机验证，外部存储尚未验收。
- 新生产取消证据：`validation/7.17/phase1-task-disposition/round-2.json`（普通 Future）及
  `round-3.json`（周期任务）；取消接受后仍运行，不中断线程，实际退出后才写退出事实，
  共享池/无关条目保留，周期任务不重排。保留 JAR
  `validation/7.17/phase1-checkpoints/ronova-7.17-task-cancel-checkpoint.jar`，
  SHA-256 `7b53a92c386c672d55230c4fe414b1d5d94f66608f70613566cf5b4ca66d7658`。
- 连续 worker copy→copy：`validation/7.17/phase1-worker-chain/round-2.json` 通过，深度 2。

## 现有接线

- `BootstrapTaskHooks` 位于独立 bootstrap JAR，由 Native 注册任务上下文入口/退出方法。
  运行期 JVMTI 无局部变量读取能力时，使用实际已验证 JDK 调用边界建立 Native 身份上下文。
  活动方法在重转换后可能成为 obsolete method id；上下文核销使用配对 token 和边界角色。
- `JdkTaskBoundaryContract` 验证完整平台镜像，兼容 JVM 省略旧 Deprecated 属性。
  `ThreadPoolExecutor.runWorker` 只包围实际 `Runnable.run` 调用，保留原异常处理。
- `RecoveryTaskObserver` 捕获有界事件；待合并事件暂时保留对象，合并后图只保留弱引用。
  `RecoverySourceTracker.maintain` 在服务器线程合并，`RecoveryObservationScope` 路由 Realm。
- 异步复制使用已发布的源身份/资格票据；无票据的实际 caller 只用于后续发现，不回填本次证明。
  `TaskExecutionGraph` 保存执行、父子、取消事实和已观察来源使用；它不授予取消权限。
- `RecoveryTaskDisposition` 已接服务器维护：准确 FutureTask/ScheduledFutureTask、单个已证明来源、
  原资格和当前适配器证明→Root 意图 ACK→Native 取消→独立退出事实。它不关闭后继或资源义务。
- FutureTask.cancel 的 VarHandle CAS 改经 BootstrapTaskHooks.cancelState；Native 仅为明确的
  Root 调用检查 task/handle/lease，在 CAS 后释放 Root fence，非 Root 取消保留原行为。
  `cancelRegisteredFuture0` 非虚调用 FutureTask.cancel(false)，避开 ScheduledFutureTask 的独立队列锁。
- Root writer 已有 prepare/acquire/query/observe 任务及存储接口。结果事实只能累积，仍保持未知；
  **尚无外部存储 executor 或 Work 完成证明**。

## 下一批必须继续

以下继续作为同一个端到端阶段推进，并纳入恢复链规划第 12 节新增的具体约束：

1. 扩展实际任务处置：排队任务的原始资格、通用委托、ForkJoin/CompletableFuture 处置、
   子任务和迟到回调、队列/资源结算。普通 Future/周期任务运行中取消已经接线。
2. 完成最终写入门，覆盖 Root 资格撤销与 Native/登记 CAS 的共同提交边界。
   `RootExecutionFence` 新计划作用域已接入，但它本身不是 Native TOCTOU 保证。
3. 接实际 SavedData/受支持静态保存路径，分别证明序列化、提交、独立读回及其他记录/未知字段保留。
4. 原意图重启查询、准确资源释放及已知恢复链的稳定完成。不得把旧 OPAQUE 生命周期改成成功。
5. 扩展统一故障/压力夹具，再集中完整构建和阶段实机验收，交付最终候选 JAR/哈希与报告。

## 尚需处理的边界

- 同步 CompletableFuture 后继、广泛第三方包装/委托尚未覆盖。
- 连续 worker 复制已通过：捕获后发布暂存票据，服务器仍逐边复验；真实同步种子重观察刷新票据。
  `SourceUse.copyDepth` 保留同来源最大深度，任务计划按该深度选择政策。
- CallerRuns 中已登记 FutureTask 的同步 task-source 关联已补齐并通过实机，连续复制深度 2、
  实际换 UUID 工厂拒绝、无关任务隔离及原效果保持通过；同产物重启后的异步链回归通过。
  证据：`validation/7.17/phase1-caller-runs/evidence.json`，检查点 SHA-256
  `8bd5ebd46d3a14154e92f66742edf3deac4d5287622df4750bf4d7327b4d7d4f`。
- 尚需处理来源 detach 后旧任务保留种子的继续使用、CallerRuns 的裸 Runnable 等未登记边界，以及
  Task 来源深度在原意图 ACK 后变化时的重新规划；不能重用更弱的许可。
- 事件丢失、代码证明过期保留未决；仍需完整压力恢复与公平性出口。
- Task 图目前只回收无来源、无关系且已收集的无关节点。相关节点的退休必须等待 Root 义务闭合。
- Native 任务 token 索引已加弱引用队列回收，关闭期间迟到入队有界清理；仍需完整生命周期/压力验证。
- 实机工厂调用期间若 Native 证明失效会拒绝作证；正例等待能力恢复后触发，不得用 Mixin
  fallback 调用栈补充真正的 bootstrap 工厂见证。

## 执行约定

- 工作目录 `D:\桌面\MOD\mod`，无 Git 仓库。JDK17 位于 `%APPDATA%\.minecraft\runtime\java-runtime-gamma-snapshot`。
- Gradle 参数必须引用：`'-Pmod_version=0.2.0-alpha.7.17-phase1-dev'`；使用 `--offline`，
  `GRADLE_USER_HOME=D:\桌面\MOD\.gradle-cache`。不要覆盖默认 r66 或 validation-12。
- runner 拒绝覆盖已有轮次；同一 suite 不放不同 basename 的生产 JAR，以免加载重复 Mod。
  支持 `--jdk-tasks`，异步来源使用 `--static-history-locations --late-copy-observer --async-copy-observer`。
- 新 runner 标志：连续复制再加 `--async-copy-chain`（EXTREME）；任务处置为 `--task-disposition`，
  周期任务再加 `--task-disposition-scheduled`。新世界需先单独 `--prepare-state-policy` 再重启运行。
- 所有测试只使用隔离世界；原游戏与原存档保持不变。当前没有运行中的专用服或构建会话。
