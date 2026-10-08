# 恢复与处置机制对照学习

日期：2026-09-10。范围：用户提供的 ArcaneVorte 0.5.6、永爱之刃 2.6 fix / 3.2、SuperSteve 6.9.1 原 JAR，以及对应 Java/Native 还原包。

本次完成输入清点、版本对应核对和重点机制静态阅读。没有启动 Mod、Agent、DLL 或 EXE，没有修改原游戏或存档。对 Arcane 及有疑义的方法使用本机 JDK `javap -c -p` 读取字节码；这不执行被分析类。

## 可以用于 7.17 的结论

这些样本共同说明：恢复源不限于实体 NBT，也可能是旧对象、静态字段快照、共享集合、后续任务和运行时代码。不能将“调用删除返回成功”“取消被接受”“查询返回已删除”视为同一件事。

值得吸收的是恢复路径和验证场景。按类名、包名、UUID 或字段批量处置的做法不能直接成为 Ronova 的归属或动作许可规则。

| 样本 | 本次读到的机制 | 直接对应的验收要求 |
|---|---|---|
| SuperSteve 6.9.1 | 静态实例、共享裸线程、原对象重插、复制容器后发布、查询和渲染补入 | 当前 holder、旧指针、最终发布和稳定窗口分别检查 |
| 永爱之刃 2.6 fix | 静态字段快照与回写、部分集合浅复制、定时撤销条目、Native 接口及 JVMTI 链操作伪代码 | 共享别名、迟到任务、原资格、覆盖失效和未知 Native 效果 |
| 永爱之刃 3.2 | UUID/类级集合、多处实体索引处置、隐藏实现与普通实现回退；存在带副作用的查询辅助方法 | 精确目标隔离、定义身份变化、独立结果查证；接口存在和生产接线分开 |
| ArcaneVorte 0.5.6 | 生成路径判别、线程局部临时放行缓存、延迟清理线程、UUID 防护/追踪集合、隐藏类替代字节码 | 许可的作用域和寿命、迟到清理、加载器身份、实际生效代码 |

## 版本和证据边界

- 8 个输入的完整路径、SHA-256 和条目计数保存在 [输入清单](../validation/7.17/reference-study/input-manifest.json)。文件名里的签名、Pro 或版本标签不作为能力证明。
- 永爱之刃两份 Java 还原包 README 中记录的原 JAR 哈希与本次实际计算值一致。这验证了包的来源声明，不能保证每个反编译方法都正确。
- 原 SuperSteve JAR 有 109 个 class；与上次还原包对应的 43 个非 class 文件逐字节一致。沿用 [上次学习报告](SUPERSTEVE-6.9.1-STUDY.md)，不重复算作新验收。
- Arcane 主包有 468 个 class；内嵌 Agent 有 60 个 class，60 个名字均在外包出现，其中 58 个原始字节不同。对 3 个重点类另外读取了内嵌版本；所检查的关键机制在两处均可见。字节不同可能来自常量池、编码宽度或调试信息，**不能直接推断行为不同**，更不能据此判定运行时加载了哪个版本。
- 本报告是重点机制学习，不宣称穷尽数千个依赖类或恢复了所有 Native 实现。

## 永爱之刃 2.6：快照并不等于独立副本

源码位置以还原包 `src/com/wzz/forever_love_sword/` 为根。

`util/FieldModificationManager.java:82–165` 枚举传入类集合的静态非 final 字段，通过 Unsafe 或反射读取，再登记到 `Map<Field,Object>`。`315–361` 的恢复操作重新复制快照值并回写字段；`modifyField` 中可见 Unsafe 写入。这是一条独立于实体工厂的内存恢复入口。

CFR 对 `deepCopy` 的还原失败，源码中的 `throw new IllegalStateException("Decompilation failed")` 是工具占位，不能当作原程序逻辑。对照 Vineflower 版本 `1297–1456` 及原 JAR 的 `javap`：部分 List 分支使用集合构造、`addAll`，Map 分支使用 `new HashMap(map)`。这些分支复制外层结构，元素引用仍可共享；其他对象存在直接返回原对象的分支。因此不能把方法名 `deepCopy` 理解为任意对象图完全独立复制。

对 7.17 的应用：测试“目标从当前容器移除，但早先快照仍引用目标并晚到回写”。处置必须绑定当前 holder 和版本，不能清空整个快照表，也不能修改一个共享元素后就宣称所有副本已独立处置。该机制本次只证明内存快照，未证明磁盘保存与重启恢复。

`list/DeathList.java:85–96` 在更换临时类条目时取消旧 Future，随后定时执行 `temporaryClasses.remove(entityClass)`。这里没有用回调所属的登记版本做条件删除。**可构造的竞争推论**是：旧回调已经运行时，`cancel(false)` 未必阻止它迟到删除新条目；本次未实际触发该竞争。Ronova 的计划应继续绑定稳定登记身份、版本和准确对象，并保留“取消结果”与“实际退出”的区别。

## 永爱之刃 3.2：不能继承 2.6 的能力结论

3.2 主包有 597 个 class、没有嵌入 JAR；2.6 主包有 3176 个 class 和 4 个嵌入 JAR，且包含大量依赖。数量不是强弱指标；仅凭版本号不能推导 Native 能力保留或增强。

`util/ForeverUtils.java:300–420,476–490` 可见死亡登记、服务器/客户端不同索引及 section 的处置路径；`list/DeathList.java:15–66` 分别保存 UUID、健康值与 Class 集合。它们帮助列举需要独立观察的表面，但 Class 级影响不能成为 Ronova 对单一来源目标的默认授权。

`util/HiddenInstanceFactory.java:11–44` 优先尝试隐藏定义，失败后回退到普通构造。测试应覆盖同一 API 在不同实际定义类下执行，不能把 API 名称或包装对象当成最终 executor 身份。

`core/hook/EventUtil.java:27–60` 的辅助方法中，健康值查询包含恢复玩家状态或处置玩家的动作，移除状态查询也会受防护与死亡集合覆盖。不过，在本次全部 Java 文本和主 JAR class 常量检索中，没有找到这两个具体方法名的调用接线；所读 `PluginTransformer` 的处理分支是 tooltip 注入。**结论限于接口内有这些逻辑，生产启用未证实**。它仍提示一个通用负例：如果接入具有副作用的 getter，observer 的查询可能改变被验证对象，需采用独立且语义已验证的观察路径。

## Arcane：临时放行、迟到清理和定义身份

以下证据来自原 JAR 与内嵌 Agent 的字节码，没有为附件运行自带 Agent。

- `Agent.EntitySpawn.SpawnValidator.shouldAllowSpawn` 读取实际对象类、反射 `getId`、调用栈类名/方法名，并保存 `ThreadLocal<ConcurrentHashMap<Integer,Boolean>>`。这里缓存键是实体整数 ID，**不是 identityHashCode**；反射失败时的 `-1` 会跳过相关缓存路径。
- 该方法的一条路径写入缓存后创建裸线程，休眠约 100ms 再按 ID 删除条目。它提供“线程局部状态被另一个清理线程持有”“旧清理遇到同 ID 新状态”的测试思路；后者是待验证竞争，不是已观测故障。
- 调用栈中出现某个类名或方法名，只是这份实现采用的判别条件，不足以证明 Ronova 所需的来源资格、定义加载器和因果关系。
- `Content.Item.VanSh.EntityTrackingManager` 分别登记目标 UUID→攻击者 UUID 与目标 UUID→实体 ID；`Help.PlayerProtectionManager` 保存防护 UUID，并提供状态恢复入口。两张表和世界对象应分别核对，关联键不是对象本身。
- `Core.CoreEntity.EntityBlackListHelper` 的增删包装会丢弃底层 Set 返回值并返回 true。**接口返回 true 不等于条目确实发生变化**，尤其重复调用或空参数路径。
- `Agent.HiddenClass.HiddenClassTransformer` 根据保护登记和栈内包名判别，存在生成替代 class 字节的分支。它只能证明转换器有该代码，不能证明 JVM 在某次调用接受了替代字节；诊断与执行需绑定实际定义及生效代码。

外包和内嵌副本的对照保存在 [内嵌类清单](../validation/7.17/reference-study/arcane-embedded-comparison.json)。局部 `javap` 归一化对照只用于导航，不是完整代码语义证明。

## Native：有函数体和只有接口必须分开

Native 补充包中，`wzzjvmti.dll/exported-functions.c:218–324` 的伪代码可见遍历指针链、改前驱/头指针以及插入节点的操作，对应导出的 remove/add JVMTI 接口。其底层辅助函数类型、布局和当前 JVM 兼容性未得到运行验证；不能推导为任意 JVM 都能成功移除观察器。

`JNI导航.tsv` 将 `restoreClass`、`deepClone`、`forceStopThread`、`retransformClasses` 等多个核心接口标为 `PACKED_NOT_RECOVERED`。对应 `wzzapi.dll` 和 `wzz_hook.dll` 的接口名、导出地址及 Java 包装不等于已恢复函数体。报告不据此宣称精确对象扫描、线程停止、恢复或原子写入已经实现或可靠。

对 7.17 的直接要求是：observer/Native 证明失效时及时撤销当前完成性；无法证明的 Native 效果继续未知。不能采用强停共享线程作为第三方任务处置捷径。

## 下一步通用测试清单

以下场景全部为 **DESIGNED / NOT_RUN**，不增加验收通过数。

| 场景 | 必须满足的结果 | 所属范围 |
|---|---|---|
| 快照含共享元素，目标删除后回写 | 检测迟到发布，旧资格不能授权写入；其他条目保留 | 阶段一 |
| 旧取消回调与同位置新登记交错 | 旧回调不能撤销或处置新登记；取消和退出分别证明 | 阶段一 |
| ID 复用与延迟缓存清理 | 许可绑定实际对象、原资格和观察版本，不串用缓存结果 | 阶段一 |
| 静态来源、共享裸线程、原对象重插 | 来源和执行义务分别结算，不停止共享线程 | 阶段一，复用 SuperSteve 场景 |
| 包装函数返回 true 但底层没有改变 | 独立查证结果，不能凭布尔值清偿义务 | 阶段一 |
| 同一 API 隐藏定义失败后回退普通定义 | 重新核验实际 executor 身份和代码证明 | 阶段一必要能力；全面对抗阶段二 |
| getter 返回伪装值或触发恢复 | 查询不构成循环自证；未知保持未决 | 阶段一必要观察；全面对抗阶段二 |
| 运行期间 observer/Native 证据失效 | 撤销当前完成性，保留历史收据与未知效果 | 阶段一必要能力；全面对抗阶段二 |
| 同类不同目标、同名不同加载器 | 不因类名、同类或 Mod ID 连带处置 | 阶段一 |
| 内存已空但保存、提交或读回未证明 | 继续显示恢复源待处理，不虚报重启成功 | 阶段一 |

完整客户端渲染与 GUI 验收仍留阶段三。本次没有取得外部保存、稳定完成或新候选 JAR 的运行证据，不替代当前阶段出口。

## 阅读材料

选取的原文及字节码位于 `validation/7.17/reference-study/`，原文路径与 SHA-256 见 [索引](../validation/7.17/reference-study/source-excerpts-index.json)。这里只保留需要复查的材料，保留原注释；没有把还原文件加入生产构建。
