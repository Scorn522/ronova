# Ronova MS 规则内加强方案 V2.0

> **已并入（2026-09-12）**：本方案的加强部分已整合进
> [`docs/DESIGN-SPEC.md`](../DESIGN-SPEC.md) **第二部分 · 加强方案**（§15–§20），
> 并在整合时修正了两处错误：
> ① §5 F 块引用的 `AttestationManifest` / `LoaderProofRegistry` / `EnforcementLease`
> 在五份规范中**均不存在**（见 DESIGN-SPEC §14 #5）；
> ② 判定分层从两层（L0/L1）扩展为三层（+ **P 项目自有规范**），
> 因为设计规范合并后发现 Ronova 可以「既不违铁律、又违反自己冻结的规范」。
> **本文件保留作对照，不再作为加强入口。**

日期：2026-09-12。性质：**设计文档**。未修改任何生产代码，未运行任何样本。

本版**取代** [`Ronova_NonViolating_Enhancement_Plan_V1.0.md`](Ronova_NonViolating_Enhancement_Plan_V1.0.md)。
V1.0 的 W1–W8 红线有六条不是 MS 文档的规则，本版按「规则来源唯一化」重写。

---

## 0. 规则来源声明

### 0.1 唯一规则来源

本方案的全部红线与合规判据**只来自 MS 三份文档**，均为 ZH 原文：

| 简称 | 文件 |
|---|---|
| `skills` | `.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md`（485 行） |
| `msitem` | `.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/item/minecraft-ms-msitem.zh.md`（418 行） |
| `entitydefence` | `.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/entity/minecraft-ms-entitydefence.zh.md`（325 行） |

引用格式为 `文档:行号`。三份文档的 EN 版是译文（标题计数 ZH/EN 一致：36/36、24/24、25/25），
不作为独立依据；仅当 ZH 表述有歧义时回查 EN。

### 0.2 明确不采用的规则来源

| 来源 | 为什么不作为规则 |
|---|---|
| `Ronova_7.16_Plus_..._V2.6_Internal_Root.md:433-445` 的 13 条绝对禁止 | 项目自有条款，不是 MS 文档 |
| `V2.6:400`「第三方兼容性不是保护目标」以外的兼容性顾虑 | 同上；且与本方案的判据无关 |
| `docs/CURRENT-PROGRESS.md`、`docs/PHASE1-IMPLEMENTATION-STATE.md` 的节奏纪律 | 项目文档纪律，不是 MS 规则（**§8 仍按用户要求保留验证后置**，但那是用户指令，不是判据） |
| V1.0 的 W1–W8 中无 MS 依据的部分 | 见 §2 逐条裁决 |
| AI 或既有文档自加的「安全保守」倾向 | 不是判据 |

**规则基只回答「违不违规」；它不回答「强不强」。** 本方案的目标是在「不违规」的补集里取最大强度，
因此任何被 MS 未禁止的手段都必须重新评估，不能因为「看起来危险」而预先排除。

### 0.2.1 唯一的例外：用户明示约束（`UX` 编号）

用户已明确要求把 **「无外部」** 作为强制约束加入（2026-09-12）。它**没有 MS 文档依据**，
但**与 MS 规则同等强制**，因此单独用 `UX` 编号，不与 `R1`–`R66` 混编——避免被误读为 MS 要求。

见 §1 闸门 0 的 `UX1`。凡与 `UX` 冲突的加强项一律排除，即使它完全符合 MS 规则。

### 0.3 MS 文档本身的性质

MS 三份文档不是纯禁止清单，而是**三层规范**，本方案三层都用：

| 层 | 性质 | 例 |
|---|---|---|
| **硬禁止** | 违反即不算 MS Mod | `skills:62-66` 三条铁律；`skills:398`「修改文件是一种违规行为」 |
| **强制架构契约** | 文档用「才算完整 / 才算完成」表述 | `skills:478-483`；`msitem:406-418`；`entitydefence:314-325` |
| **取向与默认** | 用「优先 / 建议 / 更安全的默认值 / 不会用」表述 | `skills:289`、`skills:396`、`entitydefence:236` |

取向层不是违规判据，但**偏离取向必须明文声明**（`msitem:322`「不能静默假装能力仍完整」）。

### 0.4 判定分层（2026-09-12 用户确立）

用户明确的判定口径：**「只要铁律不违反，剩下的自己说的算，铁律是共识。」**

⇒ 全部条款按**是否属于共识铁律**分成两层，判定力完全不同：

| 层 | 内容 | 判定力 |
|---|---|---|
| **L0 —— 铁律（共识）** | `skills:62-64` 三条 + `skills:398` 的推论（Forge 文件） | **强制**。违反即 `skills:66`「不算个秒杀 Mod」 |
| **L1 —— MS 其余全部文本** | 架构契约、完成标准、实现顺序、七字段契约、重转换纪律、启动顺序、失败策略、独占过滤开关… | **自定**。**不是违规判据**。MS 的「必须 / 不要」措辞在这里是**建议强度**，采纳与否由 Ronova 自己决定 |
| **UX —— 用户明示约束** | `UX1` 无外部 | **强制**（非 MS 来源，效力等同于 L0） |

**这个分层推翻了本文早先的一处框架错误**：此前把 L1 的「必须 / 不要」当成违规判据，
列出「5 处明文未满足」。按 0.4 的口径，那 5 处是**自定项**，不是违规。
V2.0 §4.0 与 §5 C 块已按此重述；`Ronova_MS_Remediation_Spec_V1.0.md` 的 S1–S6
从「整改」改述为**可选强化设计**。

**L1 仍然有用的三个理由**（不因为「自定」就作废）：

1. **它是免费的设计库**。MS 的七字段注入契约、重转换批处理纪律、启动顺序、
   独占过滤的 5 条后果——这些都是踩过坑的人写的清单，直接抄成本最低；
2. **它决定对抗强度**。L1 里的 R17（重转换纪律）、R22（判定收口）、R57（注入契约）
   直接关系到能不能扛住别人的转换器删除与重写；
3. **一旦采纳，就要自洽**。自己定的规则必须自己守住并如实呈现——否则退回
   L0 之外的另一种失分（对抗中对手会先发现你自称的能力是假的）。

---

## 1. 规则基（L0 铁律 + L1 参考库）

按「一份加强项要过几道闸」组织。**闸门 0 是 L0（强制）；闸门 1–8 是 L1（自定参考，见 §0.4）。**

### 闸门 0 —— MS 身份闸（三条铁律 + 一条推论）

| ID | 规则 | 依据 |
|---|---|---|
| **R1** | 不修改文件，包括依赖文件、Mod 文件 | `skills:62`，违反即 `skills:66` |
| **R2** | 不针对：不得用反射 / `MethodHandler` / `VarHandler` 或直接使用对方的类 | `skills:63` |
| **R3** | 不删对方 Mod 文件 | `skills:64` |
| **R4** | 不修改 Forge 文件（MS 明文称之为违规行为） | `skills:398` |

**R1 的正向边界（MS 自己要求写文件的地方）**：
`skills:381` 要求从 Jar 固定资源路径把 DLL 提取到游戏目录私有位置；
`skills:235` 要求白名单 JSON 写入游戏目录。
⇒ **写自有文件不是 R1 违规**；R1 覆盖的是第三方文件、依赖文件、Mod 文件、Forge 文件。

#### 闸门 0 附加 —— 用户明示约束 `UX1`（非 MS 来源，同等强制）

| ID | 规则 | 判定方法 | 来源 |
|---|---|---|---|
| **UX1** | **无外部**：不创建任何外部进程（含 helper JVM、attach 辅助进程、外部工具） | 进程树中无本 mod 派生的子进程；主源码中 `ProcessBuilder` / `Runtime.exec` / `ProcessHandle` 派生调用为 0 | 用户 2026-09-12 明确要求；**MS 三份文档零覆盖** |

**当前唯一的违反点**：`coremod/agent/AutoAttach.java:26-33` 用
`new ProcessBuilder(java.exe, "--add-modules", "jdk.attach", "-cp", <自身 jar>, AttachHelper, <自身 pid>, <自身 jar>)`
派生子 JVM，由 `AttachHelper.java:13-21` 执行 `VirtualMachine.attach(pid).loadAgent(jar).detach()`。

全主源码检索 `ProcessBuilder|Runtime\.getRuntime\(\)\.exec|ProcessHandle` → **仅此一处**；
其余 `.start()` 全部是 `Thread.start()`（`BaselineTelemetry.java:318`、`RonovaAgent.java:114`、
`RecordMutationTask.java:38`、`StorageImageTask.java:87`、`StorageTransactionTask.java:28,77`）。

⇒ 它**确实是自指向**的（`ProcessHandle.current().pid()` + 自身 JAR），不触 R2；
它存在的目的是避免要求 `jdk.attach.allowAttachSelf`（服务 R60 的取向）。
但按 `UX1` 它是必须移除的项，见 §5 的 **C15**。

> 注意与 `R21`/`R22` 区分：`UX1` 约束的是**进程模型**，不是 Native 层的定位。
> 移除 `AutoAttach` 不影响 `NativeBridge` 在进程内加载两个 DLL（`UX1` 允许）。

### 闸门 1 —— 目标判定闸

| ID | 规则 | 依据 |
|---|---|---|
| **R5** | 目标分类不能依赖其他 Mod 的包名、类名、文件或私有实现；应使用通用实体类别、公开状态、标签、权限和自己的状态服务 | `msitem:41` |
| **R6** | 未分类的新生物、Boss 或多部位对象：保守地 `IGNORE`，直到完成兼容性测试 | `msitem:218` |
| **R7** | 多部位实体的部位 ≠ 根实体；命中部位必须明确「转根 / 只作用部位 / 拒绝」；范围扫描不得把根与全部部位重复列入同一动作 | `msitem:220` |

### 闸门 2 —— 改写纪律闸

| ID | 规则 | 依据 |
|---|---|---|
| **R8** | 不对 JDK、Forge、LWJGL 或游戏核心类做全局「全部方法直接返回」式改写；只改写有明确目标、可验证 descriptor 和回滚策略的入口 | `skills:348` |
| **R9** | `Unsafe` 内联改写**被明确承认为合法技术**，条件是：仅用于已验证的 JDK 17 x64 布局；其他 JVM / JDK 更新 / 其他架构 / 布局探测失败时立即拒绝执行；不得用它**宽泛**改写 JDK、Forge、LWJGL 或游戏核心类 | `skills:324` |
| **R10** | 内联 HotSpot 实现必须**先探测并验证**假定的 JDK 布局、指针编码、代码地址和代码长度 | `skills:318` |
| **R11** | 替换代码必须放得进原 code 数组；剩余部分用 `NOP` 填充，保证新 `return` 之后不继续执行旧指令 | `skills:319` |
| **R12** | 必须明确继承策略：已加载子类的覆盖方法需枚举并修改；之后加载的子类需转换器 + 重转换 | `skills:320` |
| **R13** | 必须预置回滚（保存原始字节码或声明重启/重定义恢复路径）；未检测已有补丁时绝不能对同一 Hook 重复应用 | `skills:322` |
| **R14** | 只用小范围 HEAD/RETURN 注入、字段访问修正或窄重定向；大范围覆写仅在能说明兼容边界与回退方案时使用 | `skills:289`、`msitem:325` |
| **R15** | 转换器注册表必须有顺序、去重、日志、回滚；`null` = 未修改；保存最终输出字节码供后续 redefine | `skills:330-334`、`msitem:327` |
| **R16** | 目标类已加载时注册转换器不生效，必须走受控的重转换/重定义路径 | `skills:336` |
| **R17** | 不得在类加载回调中直接递归重定义当前类；单工作线程 + 阻塞队列；同类的重复请求去重；**批量每批最多 32 个类**；单批失败输出类名/阶段/异常并降级为逐个处理 | `skills:340-346` |
| **R18** | `ClassFileLoadHook` 中避免触发会再次加载大量类的 Java 回调 | `skills:408` |
| **R19** | Mixin / Accessor / 反射 / 转换器的工作范围必须很窄，并针对准确的 Forge 版本和映射测试 | `entitydefence:270` |
| **R20** | 每个注入类只承担一个小责任；客户端类拆分单独注入定义 | `skills:283`、`skills:288` |

### 闸门 3 —— 原生层定位闸

| ID | 规则 | 依据 |
|---|---|---|
| **R21** | 只在 Forge 事件、访问转换器或普通 Java agent 无法安全完成时才启用 Native 层；合法用途限定为：已加载类的重转换/重定义、自定义 class-file hook **/** 隐藏类 **/** 类恢复、模块与 JVM 线程上下文相关的底层能力 | `skills:352-356` |
| **R22** | **Native 层应是小型、受约束的执行后端，不能成为业务规则的第二份实现。秒杀策略、防御分类和物品规则仍放在 Java 层** | `skills:358` |
| **R23** | 通过单一桥接类暴露窄命令，不把 native 指针或内部状态泄漏到游戏代码；命令集限定在 `ping` / `init` / `retransformClasses` / `redefineClassesBatch` / `restore...` 形态 | `skills:362-370` |
| **R24** | 桥接若使用共享 `command/args/result` 槽位，一次只允许一个请求在飞行；用锁或请求序列号保证并发不互相覆盖 | `skills:372` |
| **R25** | 不要让 Java 在无限自旋中等待 native 返回；设置超时、清理槽位、恢复中断状态、输出命令名与异常上下文 | `skills:374` |
| **R26** | 启动顺序：检查 Windows x64 **和 Java 17** 前提 → 提取 DLL → **先 `ping` 验证 JNI/JVMTI 绑定** → 初始化 native agent 与 class-file hook → 打开 Java 模块访问边界 → 建 Java 端注册表/映射/模块 → 注册转换器并驱动重转换 → 再注册 Forge 内容 | `skills:378-387` |
| **R27** | 所有启动步骤必须幂等，防止静态初始化、构造器重入或重复加载导致多次安装 JVMTI hook | `skills:389` |
| **R28** | 按 JNI/JVMTI 规范管理局部/全局引用、异常与 UTF 字符串；不跨线程保留失效 `JNIEnv*` | `skills:406` |
| **R29** | 不在任意 native 工作线程直接调用需要 JVM safepoint 或游戏主线程的 Java 逻辑 | `skills:407` |
| **R30** | 把 Windows API、JVM 内部头文件和 Capstone 等依赖隔离在 Native 工程；Java 层只看到桥接命令 | `skills:410` |
| **R31** | 不把 DLL 文件名、资源路径和构建拷贝路径拆成三份手写常量；由构建任务从单一来源生成并校验 | `skills:411` |

### 闸门 4 —— 结算唯一性闸

| ID | 规则 | 依据 |
|---|---|---|
| **R32** | 必须选择一个**结算所有者**；只有统一调度器可把动作推进到执行状态 | `msitem:140`、`msitem:299` |
| **R33** | Forge 事件、物品覆写、标准 Sponge Mixin 和自定义转换器都是入口或契约维持层，**不能各自成为第二个击杀执行器** | `msitem:288` |
| **R34** | 一次动作对一个目标只产生一个最终状态转换 | `msitem:259` |
| **R35** | 至少按 `actionId` 与目标稳定身份去重；同一 tick 不得为同一目标建多次任务 | `msitem:168-178` |
| **R36** | 队列必须有每 tick 总预算、每发起者预算；世界关闭/维度切换/实体卸载/玩家离开时丢弃无效任务；单任务失败隔离并记录上下文；不保留长期强引用 | `msitem:275-282` |
| **R37** | 开始持续使用不能直接提交无限任务 | `msitem:135` |
| **R38** | 真正执行前再次验证：物品仍在允许的发起者手中、目标仍在同一维度、目标不在冲突状态、动作未超时、冷却与资源仍有效 | `msitem:284` |
| **R39** | 不得重复结算：战利品只结算一次；冷却/耐久/资源成本只在设计结果真正发生时提交 | `msitem:234` |

### 闸门 5 —— 客户端/服务端边界闸

| ID | 规则 | 依据 |
|---|---|---|
| **R40** | 服务端不接受「客户端指定要清除的任意实体列表」；客户端不能因收到本地特效包就修改实体、战利品或注册表 | `msitem:341` |
| **R41** | 视觉层不能修复游戏规则；隐藏实体、改 HUD 或改物品栏文字不能替代服务端结算、清除或保护 | `msitem:356` |
| **R42** | 客户端专属类、渲染器、UI 必须在明确的客户端边界；公共/服务端初始化不能触及它们，否则专用服务器可能类加载失败崩溃 | `msitem:343`、`skills:210-211` |
| **R43** | 不允许从渲染线程、网络处理线程、异步线程或 Native 回调线程直接修改服务端实体集合 | `skills:189`、`entitydefence:130`、`msitem:282` |
| **R44** | 不在实体集合迭代期间修改集合 | `skills:131`、`msitem:195`、`entitydefence:149` |

### 闸门 6 —— 状态与修复闸

| ID | 规则 | 依据 |
|---|---|---|
| **R45** | `ERASED` 与 `PROTECTED` 不能同时成立；冲突时必须有确定优先级 | `msitem:87` |
| **R46** | 规范实例 5 条规则：首个有效实例占据规范引用 / 普通构造与客户端同步不能替换 / 每个被修复的注册表接收**同一个**对象 / 有意替换走显式生命周期 / 规范引用缺失时不造占位对象 | `entitydefence:62-68` |
| **R47** | 修复前 7 项状态不变量：运行时 ID 与 UUID、实体类型、坐标/方块/区块坐标、包围盒、旧坐标与旧旋转、已加入/已移除/可更新标记、生命与死亡等瞬态状态 | `entitydefence:72-86` |
| **R48** | 修复过程必须幂等；重入时只做维持不变量所需的最小修正 | `entitydefence:113`、`entitydefence:115` |
| **R49** | 修复点至少有六类：初次构造/正常生成、实体自身 tick、独立服务端 tick、**注册表写入 hook**、**注册表读取 hook**、客户端 Level 与渲染 hook | `entitydefence:119-128` |
| **R50** | 读取路径必须认识规范实体，关闭「被移除后、下次修复前」的时间窗 | `entitydefence:201-211` |
| **R51** | 构造查询结果时按对象身份或规范运行时 ID 去重 | `entitydefence:213` |
| **R52** | 全局列表独占过滤必须使用**独立、明确的开关**，不能藏在基础伤害或死亡防御里；启用前必须记录 5 条后果 | `entitydefence:40`、`entitydefence:228-236` |
| **R53** | 只在确实需要时取消方法；有返回值的方法必须显式写回调返回值；`void` 方法设置返回值没有意义 | `skills:255` |
| **R54** | 不冻结所有同步字段（会破坏装备、位置和动画同步）；只保护必须稳定的字段 | `skills:265`、`entitydefence:250` |
| **R55** | 周期修复必须幂等，避免每 tick 反复重建集合、刷网络包或重复注册实体 | `skills:271` |
| **R56** | 防御是五层而非一层：统一分类、前置阻断、状态视图修正、同步阻断、周期修复 | `skills:224` |

### 闸门 7 —— 声明完整性闸

| ID | 规则 | 依据 |
|---|---|---|
| **R57** | 每个注入/转换声明必须写清七字段：目标（类+精确方法/字段+JVM descriptor+映射策略）、位置、逻辑侧、条件、顺序、**失败策略**、验证 | `msitem:313-323` |
| **R58** | 失败策略必须明确「失败 / 禁用能力 / 安全降级」之一；**不能静默假装能力仍完整** | `msitem:322` |
| **R59** | 禁用 Forge 提供的 Mixin 的两条理由（可被对手直接删除；无继承链修改、只能改正常加载的类、改过一次无法二次修改）——**这是强度评价，不是禁止** | `skills:277-279` |
| **R60** | Java Premain 是绝对优先级，但需要修改启动参数，「正常对抗中不会用该优先级」；只有 Boot Layer 能用的更高优先级形式需要修改 Forge 文件，属违规 | `skills:396`、`skills:398` |

### 闸门 8 —— 完成标准闸

| ID | 规则 | 依据 |
|---|---|---|
| **R61** | 完成标准三句：Native 层工作正常；Java 层工作正常；**字节码不会被别人修改到** | `skills:478-483` |
| **R62** | 完美击败标准：血量显示全部为 0（含 data health / getHealth / 后台数据）；显示死亡界面且不闪烁、不缺内容；GUI 血量显示为 0 | `skills:45-49` |
| **R63** | 实体击败标准：`say @e` 中没有；`forge entity list` 中消失；F3 的 E 计数中消失 | `skills:51-54` |
| **R64** | MS 击杀物品完成标准九条（收敛到一个可去重、可审计、服务端权威的动作调度器；目标/结果/物品/防御规则集中在策略服务；一次动作只走一个结果；客户端只负责输入与表现；每个 Hook 有精确目标、逻辑侧、取消语义、失败策略和验证用例等） | `msitem:406-418` |
| **R65** | 受保护实体完成标准八条（规范实例所有权与替换契约；状态/ID/UUID/类型/位置/区块/包围盒一致；服务端六类注册表可分别恢复；客户端收敛到服务端权威；读侧兜底关闭时间窗；重复修复不产生重复；独占过滤明确有限已记录已测试；读者能用自己的代码实现） | `entitydefence:314-325` |
| **R66** | Native 工程应提供 CTest 或等价可执行探针，覆盖 JNI/JVMTI 桥接、类恢复策略和**自映像保护** | `skills:452` |

### 1.1 MS 文档内部的一处张力（已裁决）

`skills:277-279` 说「不使用 Forge 提供的 Mixin」，而 `msitem:288` 把「标准 Sponge Mixin」列为可接受的层之一。

裁决：**不是禁止，是强度评价。** `skills:279` 给的两条理由都是「Mixin 在对抗中弱」：
转换器服务可被删掉、无继承链修改、改过一次无法二次修改。

⇒ 由此得到一条可判定推论，本方案称为 **R59′**：
**Mixin 层不得是任何关键能力的唯一实现。** 关键能力至少要有第二条不依赖 Mixin 转换服务的通道。

---

## 2. V1.0 红线的逐条重裁

| V1.0 | 内容 | 重裁 | 理由 |
|---|---|---|---|
| **W1** | 不修改任何文件 | **保留，收窄** | 正据 `skills:62`、`skills:398`；MS 只覆盖第三方/依赖/Mod/Forge 文件。写自有文件（`skills:381`、`entitydefence:235`）是 MS 要求，不算违规 |
| **W2** | 不针对（不按第三方包名/类名/Mod ID/私有实现判定） | **保留，精确化** | 正据 `msitem:41`。原文还包含「**文件**」与「**私有实现**」两种依赖，V1.0 漏了 |
| **W3** | 不删对方 Mod 文件 | **保留** | `skills:64` |
| **W4** | **无外部**：不创建任何外部进程 | **删除**（随后由用户明示以 `UX1` 重新加入，见闸门 0 附加） | **MS 三份文档零处提及进程**。查遍 `skills` / `msitem` / `entitydefence` 无任何关于子进程、helper JVM、attach 的条款。此规则无 MS 依据，因此不能编在 `R` 系列里 |
| **W5** | **无参数**：不要求任何启动参数 | **降级为取向** | `skills:396` 原文是「正常对抗中不会用该优先级」，是取向不是禁止。保留为默认选择，不作为违规判据 |
| **W6** | 不对 JDK/Forge/LWJGL/核心类做全局全返回式改写 | **保留** | `skills:348` 原文 |
| **W7** | 不破坏宿主稳定性 / Authority 正确性 / 用户数据安全 / JVM 可持续运行 | **删除为红线，改为「资源有界」闸** | 原文出自 `V2.6:449`，非 MS。MS 的等价条款是可判定的：`msitem:275-282`（每 tick 预算、每发起者预算、丢弃无效任务、失败隔离）、`skills:345`（每批 ≤32）、`msitem:135`（不能提交无限任务） |
| **W8** | 不篡改既有 JDK 类的行为 | **删除** | MS 无此禁。且 **MS 自身取向与 W8 相反**：`skills:384` 把「打开 Java 模块访问边界」列为启动顺序第 5 步，`skills:396` 又否定 Premain（需改启动参数）⇒ MS 的顺序隐含要求在**无启动参数**的前提下程序化打开模块边界，而这正是 W8 禁止的手段 |

**结论**：W1 / W2 / W3 / W6 有 MS 正据，保留；W4 / W7 / W8 删除；W5 降级。
**其中 W4 随后由用户明示以 `UX1` 重新加入**——不是作为 MS 规则，而是作为加在规则基之上的用户约束
（见闸门 0 附加与 §7）。所以它被删除的是「MS 依据」，不是「效力」。
V1.0 §4「明确不采纳」表中，有 7 项的理由是完全或部分依赖被删红线的，必须翻案（见 §3）。

---

## 3. 重新纳入的能力空间

下表把 V1.0「明确不采纳」的每一项按 MS 口径重判。**判据是 MS 文本，加上用户明示的 `UX1`。**

| # | 手段 | V1.0 结论 | MS 口径 | 约束 |
|---|---|---|---|---|
| **I1** | 改写既有 JDK 类（`InstrumentationImpl` 链、`sun.instrument` 插桩） | 不采纳（W8） | **允许**。MS 只禁「**全局**全返回式改写」与「**宽泛**改写核心类」 | 精确目标 + 可验证 descriptor + 回滚（R8/R13/R14） |
| **I2** | 借 `Unsafe` / `ReflectionFactory` / 私有字段取得包访问 | 不采纳（W8） | **明确允许**。`skills:324` 主动描述了这条技术路线并给出三条准入条件 | JDK 17 x64 已验证布局；其他 JVM/JDK 更新/其他架构/探测失败 ⇒ 立即拒绝；不得宽泛改写核心类（R9/R10） |
| **I3** | 对象头 / klass 元数据改写 | 未评估 | **允许**（MS 无语）。`skills:30-35` 把「熟悉 JDK 内存布局」列为 T1 门槛 | 精确 + 可验证 + 可回滚；受 R8「不得宽泛改写核心类」约束 |
| **I4** | 图形栈 hook（DXGI / OpenGL） | 未评估 | **允许**（MS 无语） | 只服务 R62 的显示侧判据；不得成为 R41 意义上的「视觉层替代服务端」 |
| **I5** | 隐藏类 / 运行时内存编译 | 未评估 | **明确允许**。`skills:355` 把「处理自定义 class-file hook、隐藏类或类恢复」列为 Native 层合法用途 | R21/R22：仍是受约束后端，不承载业务规则 |
| **I6** | engine 函数级 inline hook（含 JIT 产物） | 不采纳（"跨 JDK 极脆弱"） | **允许**（MS 无语）。脆弱是工程代价，不是违规 | 仍受 R10 探测-验证纪律；必须实现「失败即拒绝」 |
| **I7** | 手搓 `\\.\pipe\javatool<N>` 直连 attach listener | 不采纳 | **允许**（MS 无语） | 依赖 JDK 内部 ⇒ 同样受 R10 探测纪律；**进程内**，不触 UX1 |
| **I8** | ~~起外部 helper 进程~~ | 不采纳（W4） | MS 无语 | **【已排除】触 `UX1`「无外部」**（用户 2026-09-12 明确要求）。见下表 |
| **I9** | 运行时解密载荷后 `DefineClass` | 不采纳 | **允许**。`skills:354-356` 明文覆盖 | R30：依赖隔离仍在 Native 工程内 |
| **I10** | 载荷伪装（DLL 改名 `.secret`、jar 加密） | 不采纳 | **允许**（MS 无语） | **只与 Ronova 自有的可验证性取向冲突，不与 MS 冲突**。采纳与否是取舍，不是合规问题 |
| **I11** | 定义自有类进 JDK 包（T0-① 完整形态） | 半可达 | 技术受限：`java.*` 是 JDK 保留包名（**平台事实，非文档规则**）；取得 `sun.*` 包访问需 I2 | 与 I2 同条件 |

**仍然排除的**：

| 手段 | 依据 |
|---|---|
| 改写他人 JAR / 文件（`JarPatcher` 类手法） | `skills:62`、`skills:64` |
| 按第三方名称判定目标 | `msitem:41`、`skills:63` |
| 全局全返回式改写 JDK / Forge / LWJGL / 核心类 | `skills:348` |
| 修改 Forge 文件以取得 Boot Layer 优先级 | `skills:398` |
| **起外部 helper 进程 / 任何子进程** | **`UX1`（用户明示约束，非 MS 依据）** |

---

## 3.1 参考样本的 MS 合规判定

同一把尺子量五个参考样本。**结论：只有 ArcaneVorte 违反 MS 铁律；其余四个都不违反。**

| 样本 | MS 判定 | 关键证据 |
|---|---|---|
| **ArcaneVorte 0.5.6（含内嵌 Agent）** | **❌ 违反 R1（`skills:62` 不能修改文件，包括依赖文件、Mod 文件）** | `ARCANE.md:17`：`JarPatcher.patchAllMods` 注册 shutdown hook、准备临时目录、**扫描 `mods/` JAR 并按条件改写**，随后调用 `MixinPatcher`；`backupAndReplaceJar` 先复制备份、写临时 JAR，**再删除原路径并 move 临时文件**。`ARCANE.md:23` 证据指向 `AntiHiddenClass/JarPatcher.java` 与 `MixinPatcher.java`。⇒ 按 `skills:66`，这属于「不算个秒杀 Mod」的一档 |
| **永爱之刃 2.6 fix（+ 4 个 bridge、10 个 DLL）** | **✅ 未发现违反任何 MS 铁律** | 未发现磁盘 JAR 改写。native 包中只有 `wzzjvmti.dll` 是 JVMTI，`wzzdx2d`/`wzzdx3d` 是图形栈 hook、`wzzswaphook`/`wzzSecurityHook`/`wzzWindowDetector` 等——**MS 对图形栈 hook 与多 DLL 布局零表述**。手搓 `\\.\pipe\javatool<N>`、`Can-Set-Native-Method-Prefix: true`（即 `skills:59` 点名的 JNCT/JLTN）、`-javaagent` Premain 全部不在 MS 禁止面内（Premain 仅是 `skills:396` 的取向问题）。`ServiceLoaderPriorityAgent.reorderResources` 重排 ServiceLoader 资源序——MS 无禁，但属跨 Mod footprint，应记录 |
| **永爱之刃 3.2** | **✅ 未发现违反** | 无内嵌 JAR、无 native 声明。`HertaEntity` 把 remove/setter 覆写为空操作、`alive` 返回 true、健康值返回固定大值——这正是 `skills:243-253` 的防御方法表要求的形态。`HiddenInstanceFactory` 用隐藏类 ⇒ `skills:355` 明文合法 |
| **SuperSteve 6.9.13f** | **✅ 未发现违反**（一处 `skills:324` 条件待核） | 使用 `sun.instrument.InstrumentationImpl`、`defineClassInPackage`、`sun.misc.Unsafe`、`ReflectionFactory`、运行期生成类（`SSTransformerImpl` 被引用但零 class 文件）。**`skills:324` 明确承认这条 `Unsafe` 内联路线**，`skills:384` 还要求程序化打开模块边界 ⇒ 手段本身不违规。**待核**：`skills:324` 的「其他 JVM / JDK 更新 / 其他架构 / 布局探测失败时必须立即拒绝执行」是否实现 |
| **himitsu / Secret 4.1** | **✅ 未发现违反** | `himitsu.secret` 是改了扩展名的合法 x64 PE（MZ、`PE\0\0`@0x100、Machine 0x8664、6 节、熵 6.421）；`himitsu.jar` 是 `HSP2` 魔数全加密（熵 8.000）；PE 字符串含 `jvm.dll` / `GetEnv(JVMTI)` / `SetEventCallbacks` / `ClassFileLoadHook` / `DefineClass` / `bcrypt.dll` / `BCryptDecrypt` / `HimitsuBootstrap` / `HimitsuEarlyGuard`。**MS 三份文档对载荷加密与文件改名零表述** ⇒ 不构成违规 |

**对 Ronova 的直接含义**：

1. V1.0 §4 用「他违规所以我不用」的理由排除 himitsu 与 SuperSteve 的技术，**是错的**——他们不违规。
   V2.0 §3 已按 MS 口径翻案（I2 / I9 / I10）。
2. ArcaneVorte 的 `JarPatcher` 是**唯一**可确证的样本违规，Ronova **绝不能**走这条路（R1 / R3）。
3. 样本合规 ≠ 样本安全。SuperSteve 的 `JCplrMsg` 在服务端定义并执行网络消息中的隐藏类且未发现发送者权限检查
   （`README.md:37`）——MS 无相应条款，但这是真实攻击面；**采纳技术不等于采纳其安全姿态**。

### 3.1.1 同一批样本对 L1 条款（R17/R22/R26/R31/R57/R58）的对照

上表判的是 L0 铁律。L1 是 MS 的工程建议，样本**不是**照 MS 文档写的，
因此多数条款**根本无从判定**——研究档案的定位是行为链，不是工程纪律，且 `README.md:67` 明载
「运行时实际加载与竞争：**未加载原样本**」。

| 条款 | 永爱 2.6 | 永爱 3.2 | ArcaneVorte | SuperSteve | himitsu |
|---|---|---|---|---|---|
| **R58** 不能静默假装能力完整 | **❌ 违反** | 无证据 | 无证据 | 无证据 | 无法判定（载荷加密） |
| **R17** 重转换纪律 | ⚠️ 失败上报不满足（与 R58 同一处代码） | 无证据 | 无证据 | 无证据 | 无法判定 |
| **R57** 七字段契约 | ⚠️ descriptor 缺失时回落到名称匹配 | 无证据 | 无证据 | 无证据 | 无法判定 |
| **R26** Java 17 + ping | 无证据 | 无证据 | 有平台判别，未见 Java 17 与 ping | 无证据 | 无法判定 |
| **R22** Native 非第二实现 | **无法判定**（`wzzapi.dll` 149 导出、`wzz_hook.dll` 4 导出**无磁盘字节**，`NATIVE.md:17-18` 明载「受保护业务函数体…仍缺少」） | N/A（0 native 声明） | N/A（0 native 声明） | N/A（0 native 声明） | **无法判定**（`himitsu.jar` 熵 8.000 全加密） |
| **R31** DLL 名/路径单一来源 | 无法判定（无构建脚本，只有还原产物） | 同 | 同 | 同 | 同 |

**唯一可确证的 Tier 2 违规是永爱 2.6 的 R58**：

- `FLS26.md:29`：「`ClassBytecodeRestorer` 的内层重定义包装**吞掉异常，外层可能仍返回 true**；
  `BytecodeComparator` 只比较指令类型/opcode，遗漏操作数。……**不能将它的修复计数或"未修改"当成可信终态。**」
- `VM-VALIDATION.md:36` 复核为「**错误重定义仍报成功**」。

这正是 `msitem:322` 禁止的「静默假装能力仍完整」，且同一处代码也命中 `skills:346`
「单批失败时输出类名、阶段和异常」。

⇒ **样本只能当技术参照，不能当合规参照。** Ronova 的 S1–S6 没有可抄的作业。

---

## 4. 缺口清单（按 §0.4 分层）

> **本节标题改写说明**：原题「MS 口径下的真实缺口」隐含「这些都是违规」。
> 按 §0.4 的分层，只有 L0 铁律才算违规；其余是**自定项**。本节按层重述。

**编号**：`N1`–`N16` = 缺口（本节）；`R1`–`R66` = MS 文本条款（§1）；
`A`–`I` = 加强块（§5）；`UX1` / `U2` = MS 文档之外的约束与取向（§7）。四套编号互不重叠。

### 4.0 判定速览（直接回答「有没有违规」）

**L0 答案：没有确证的铁律违规。** 唯一的口径疑点已在本节给出结论（见 N12）。

| 层 | 内容 | 结论 |
|---|---|---|
| **L0 —— 铁律（共识，强制）** | R1 不改文件（含依赖 / Mod / Forge）/ R2 不针对 / R3 不删文件 | **R1 ✅ 干净**（只写自有产物；跨所有权 SavedData 写入的结论见 N12）<br>**R2 ✅ 干净**（全仓库无按第三方包名 / 类名 / Mod ID / 私有实现的判定路径）<br>**R3 ✅ 干净**（native 侧 `DeleteFile\|MoveFileEx\|ReplaceFile\|RemoveDirectory` = 0 命中） |
| **UX —— 用户约束（强制）** | `UX1` 无外部 | ❌ **一处违反**：`AutoAttach` / `AttachHelper`（唯一子进程派生点）→ C15 |
| **L1 —— MS 其余文本（自定，不是违规）** | 下面「自定项明细」的 7 条 | 与 MS 建议不同，**但按 §0.4 由 Ronova 自己决定** |

**L1 自定项明细**（原表把这一列标为「违规」，本版改述）：

| 规则 | MS 措辞 | 现状 | 自定建议 |
|---|---|---|---|
| **R22** `skills:358` | 「**不能**成为业务规则的第二份实现」 | `native_root.cpp:353-410 decide()` + `LocalDominionDecisionSource` 两份实现；且 4 个 native 域中 3 个在生产路径上是死代码 | **建议采纳 MS**：3/4 已死，删除代价最小（见 S1） |
| **R17** `skills:340-346` | 「使用单工作线程和阻塞队列」/「去重」/「单批失败时**输出类名、阶段和异常**」 | 无队列、无去重、`catch (Throwable ignored)` 零日志（`RonovaAgent.java:122-131`） | **建议采纳**：这条直接决定能否扛住对手的转换器重写（见 S2） |
| **R57** `msitem:313-323` | 「**每一个**注入或转换声明都**必须**写清以下契约」（七字段） | 抽样最多 4/7；逻辑侧 / 顺序 / 逐站点失败策略 / 验证全缺 | **建议采纳**：`descriptor` 缺失就是 `skills:315` 的静默失效面（见 S3） |
| **R31** `skills:411` | 「**不要**将 DLL 文件名、资源路径和 Gradle 拷贝路径拆成三份手写常量」 | 正是三处手写（`NativeBridge.java:14-16`、两个 `.ps1:8`） | 低风险、可随时做（见 S4） |
| **R26** `skills:380-382` | 「检查 Windows x64 **和 Java 17** 前提」/「**先调用 ping**」 | 只有 OS/arch 检查，无 Java 17 检查，无独立 ping | 低风险；亦可不做（见 S5） |
| **R58** `msitem:322` | 「**不能**静默假装能力仍完整」 | 容器级已有 `required:true` / `defaultRequire:1`（方向正确），但 `BootstrapHooks` 全域 fail-open 未声明，顶层 `catch (Throwable ignored)` 丢原因 | **建议采纳**：与 R17 同源，且对手会先发现你自称的能力是假的（见 S6） |
| **R13** `skills:322` | 「预先设计回滚。打补丁前保存原始字节码，或明确用重启/重定义作为恢复方式」 | 未见到原始字节码保存或显式回滚声明 | 随 S2 一并处理 |

**不是违规的**（避免误判）：

| 项 | 为什么不是 |
|---|---|
| N7 Mixin 是唯一实现 | `skills:277-279` 陈述的是**理由**（"不使用 Forge Mixin 的原因为…"），不是禁止；`msitem:288` 反而把标准 Sponge Mixin 列为可接受的层。它是强度评价 ⇒ R59′ 是本方案自定的推论，不是 MS 规则 |
| N8 独占过滤 | 独立开关**已存在**（`PresenceSplitFlags`，只是切的是另一件事）；缺的是归属核实与 5 条后果记录 |
| N9 规范实例 / 修复点 | `entitydefence` 的设计指引，Ronova 有自己的等价模型 |
| N10 原生探针 | `skills:452` 是「应提供」，属工程完善 |
| N11 13 个动作未接线 | 完成度问题；未接线 ≠ 违约 |
| N13 规则集两份 | 与 N1 / R31 同源，不单独构成新违规 |
| N14 未加保护的客户端调用 | **当前不可达**（无调用者），是潜伏风险而非既成违规 |
| N15 可达集合含 Forge/LWJGL | **形式合规**（精确 descriptor + 小范围），只有回滚声明缺失是实的 |
| N16 急切选取启发式 | 名称只用于**选取**、不用于**判定** ⇒ 不违反 `msitem:41` |
| **内存中改写其他 Mod 的类** | **不是违规**：`skills:243` 明确要求「保证来自其他 Mod 的直接调用也无法绕过防御」 |

### N1 —— Native 是业务规则的第二份实现（R22）——比初判更严重

**现状**：同一份判定存在两份实现，**且四个 native 判定域中只有一个真正在生产路径上走到 native**。

两份实现：

- 原生：`native/src/native_root.cpp:353-410` `ronova::root::Decision decide(const Request&)`——
  按 `domain`(0..3) 与 `facts` 位域决定 accepted / scope / reason。
- Java：`core/LocalDominionDecisionSource.java:14-64`，逐一对应四个 native 域，
  并复刻同一套 reason 词表（`world-result` / `authority-subject` / `terminal-object` /
  `tombstone-match` / `cached-final-pass` / `final-pass` / `provisional-pass` / `quarantined` / `present`），
  与 `NativeRootBridge.java:186-206` 的 reason 码 6..14 一一对应。
- 装配：`nativebridge/NativeDominionRoot.java:21` 装入 `NativeDominionDecisionSource.INSTANCE`；
  `NativeDominionDecisionSource.java:16-23` 是 **native 优先 + Java 兜底**。
  默认源是 Java（`core/LocalDominionCore.java:10-11`）。

四个域的实际路由（这是新增的、比"两份实现"更关键的事实）：

| native 域 | 生产路径 | 证据 |
|---|---|---|
| 0 Authority | **不走 native**，从 projection 解析 | `authority/AuthorityView.java:214-217` → `projection/AuthorityTopologyProjection.java:42-51`；客户端兜底本地计算 `AuthorityView.java:226-237` |
| 1 Judgment | **唯一走 native 的域** | 全仓库唯一的 `new DominionRequest` 站点：`engine/JudgmentExecutor.java:96-100` |
| 2 Admission | **不走 native** | `engine/spawn/SpawnAdmissionRuntime.java:228-231`、`:297-299` 调纯 Java 的 `view.decide(...)`；`projection/AdmissionProjection.java:77-105` 逐字复刻 `native_root.cpp:396-407` 的分支顺序 |
| 3 Presence | **不走 native** | `presence/PresencePolicy.java:48-69`、`projection/PresenceProjection.java:16-24` 对应 `native_root.cpp:409` |

⇒ 结论：**native 判定内核在生产的 4 个域里有 3 个是死代码，剩下 1 个还与 Java 判据重复。**
`skills:358` 要求判定留在 Java、Native 只做受约束执行后端；现状是"Java 已经是唯一实际判定方，
但 native 里仍保留了一份并行的判定实现，且 Java 还会二次裁决 native 的答复"。

Java 二次裁决 native 的证据（即使 native 参与，Java 仍重算）：

- `core/LocalDominionCore.java:26-31`：
  `DominionDecision.LineageScope scope = decision.scope().ordinal() > ceiling.ordinal() ? ceiling : decision.scope();`
  且 `decision.committed() && judgment.actorAuthorized() && !judgment.alreadyTerminal()` 再算一遍。
- 反向：native 丢弃 Java 提供的 scope/radius——`native_root_jni.cpp:93` 的匿名 `jint, jdouble` 形参。

`facts` 位域由 Java 组装：`nativebridge/NativeRootBridge.java:166-184`。

### N2 —— DLL 名 / 资源路径 / 拷贝路径三处手写常量（R31）

- `src/main/java/dev/ronova/coremod/nativebridge/NativeBridge.java:14-16`
- `native/build-native.ps1:8`
- `native/build-native-root.ps1:8`

`skills:411` 明文禁止这种拆分，要求构建任务从单一来源生成并校验。

### N3 —— 启动顺序缺 `ping` 与 Java 17 前提检查（R26）

`NativeBridge.load` 只检查了 `os.name` 与 `os.arch`（`NativeBridge.java:35-39`），
没有 Java 17 前提检查；`bootstrap()`（`:43`，`native` 侧 `ronova_native.cpp:790`）直接返回能力位，
没有 `skills:382` 要求的独立 `ping` 验证步。

### N4 —— 失败策略未声明（R57/R58）

`BootstrapHooks` 全域 fail-open：所有判定回调都 `catch → false`
（`agent/BootstrapHooks.java:64-65`、`:109-110`、`:216-218`、`:227-228`、`:244-245`、`:260-261`）。
「安全降级」本身是 `msitem:322` 允许的三选一，但**必须被声明**；当前是隐含行为，
且顶层 `NativeBridge.java:77-79` 用 `catch (Throwable ignored) { state = LOAD_FAILED; }` 丢弃原因，
`skills:462` / `msitem:322` 意义上的「不静默」未达成。

### N5 —— 七字段注入契约（R57）——抽样 7 处，最多命中 4/7

31 个 `@Mixin` 类（`ronova.mixins.json:7-34` 26 个 common + `:35-41` 5 个 client），
注解计数：`@Inject` 26、`@Redirect` 6、`@Accessor` 41、`@Unique` 3、`@Shadow` 4、`@ModifyVariable` 0。

抽样 7 处（`ServerLevelAdmissionMixin`、`PersistentEntityAdmissionMixin`、
`EntityTypeNbtAdmissionMixin`、`MinecraftScreenGuardMixin`、`ItemRendererMixin`、
`SavedDataRecoverySaveMixin`、`EntityProtectionMixin`）的字段覆盖：

| 七字段 | 覆盖情况 |
|---|---|
| 目标类 + 方法 | ✅ 全部 |
| **JVM descriptor** | ⚠️ 部分：多数 `@Inject` 只写方法名（`"addEntity"`、`"startTracking"`、`"setScreen"`），少数写全 descriptor |
| 位置 | ✅ 全部（HEAD / RETURN / 精确 INVOKE 目标） |
| **逻辑侧** | ❌ 无 `@OnlyIn`、无 `Dist`、无 `FMLEnvironment.dist`；侧别只由 `ronova.mixins.json` 的两个数组表达 |
| 条件 | ✅ 全部（都调统一策略） |
| **顺序** | ❌ 无 `priority`；mixin 包内 `priority` 零命中 |
| **失败策略** | ⚠️ **只在容器级**：`ronova.mixins.json:2 "required": true` + `:42-44 "defaultRequire": 1`；逐站点无声明；关键字集（`require` / `expect` / `allow` / `optional` / `failHard`）在 mixin 包内仅 3 命中，且全是 `remap = false` |
| **验证** | ❌ 只有 ASM 转换器有（`agent/SemanticGateVerifier.java:26-57`，经 `NativeTransformBridge.java:17`），Mixin 侧无 |

**公平地说**：`required: true` + `defaultRequire: 1` 是「失配即失败」的选择，
方向与 `msitem:322`「不能静默假装能力仍完整」一致。缺的是**逐站点声明**与其余三字段（逻辑侧 / 顺序 / 验证）。

这解释了 `skills:307` 的警告为何危险：`"setScreen"` 这类**只写方法名**的定位一旦遇到重载或映射漂移，
失配是从容器级 `required` 才暴露，定位不到具体站点。

### N6 —— 重转换/重定义纪律（R17）——已验证缺口

`skills:340-346` 逐条对照 `coremod/agent/RonovaAgent.java:117-133` `reassertTargets()`
（由 `:99-115` 的 `Ronova-Integrity-Reassert` watchdog 线程按 `policy.reassertMillis()` 周期调用）：

| R17 子条款 | 现状 |
|---|---|
| 单工作线程 + 阻塞队列 | **无**：watchdog 线程直接调用（`:100-110`） |
| 同类重复请求去重 | **无**：每轮全量重扫 `value.getAllLoadedClasses()` 并按 `RonovaTransformer.isTarget` 过滤（`:122-125`） |
| 每批最多 32 个类 | **无**：把全部命中类一次传给 `value.retransformClasses(targets)`（`:126-128`） |
| 单批失败输出类名 / 阶段 / 异常 | **无**：`catch (Throwable ignored)`（`:129-131`），且无任何日志 |
| 必要时降级为逐个处理 | **无** |

同一路径也让 `msitem:275-282`（有界预算）值得复核：每轮扫描规模随 JVM 已加载类数增长。

**原生侧同样不合格，但降级姿态明显更好**：

- 无批量上限：`ronova_native.cpp:622-623` `RetransformClasses(targets.size(), targets.data())`，
  另有 `:588`、`:667`、`:226` 同类调用，均无 size clamp。
- 无去重：每轮重扫 `GetLoadedClasses`（`:571-586`、`:644`）。
- 只有时间节流：`:719` 最小周期 1000ms；`g_refresh_pending` 合并（`:279-284`）。
- **失败会记账并扣留 lease**：`:686-688` 累加 `g_failed_leases` / `g_consecutive_lease_failures`，
  由 `:694-706 semantic_lease_current(...)` 要求 `g_consecutive_lease_failures==0` 才续租；
  封印周期另有 `RONOVA_CODE_SEAL_REJECT` / `RONOVA_CODE_SEAL_RETRY` 日志（`:230-234`、`:241-249`）。

⇒ 整改方向应**把 Java 侧对齐到原生侧已有的降级记账姿态**，而不是反过来。

> 这一条与 N4 同源：`catch (Throwable ignored)` 同时违反 R58 的「不能静默」
> 与 R17 的「单批失败必须输出类名、阶段和异常」。

### N7 —— Mixin 不得是唯一实现（R59′）

Ronova 有三层拦截（Mixin 31 类 / Java Agent / Native）。按 `skills:279`，
Mixin 转换服务可被对手直接删除。⇒ 每个**关键**能力必须点名它不依赖 Mixin 的第二通道，
并在 `/ronova status` 中如实显示当前生效层（与 R58 一致）。

### N8 —— 独占过滤：开关归属待确认 + 5 条后果未记录（R52）

`entitydefence:40` 与 `:236` 两处强调「不要把保护一个实体和让全局查询不再返回其他实体混成一项策略」，
并要求**独立、明确的开关**，且启用前记录 5 条后果（`:228-234`）。

已核实的事实（本轮修正：原判「缺独立开关」不准确）：

- Ronova **没有**对全局实体列表做独占过滤。最接近的两种行为都不是独占：
  - `agent/BootstrapHooks.java:115-153 filterQuarantinedResult` 只剔除 **Ronova 自己的终结目标**，
    这是 R63（`say @e` 中必须没有）**要求**的行为，不涉及其他 Mod 的实体；
  - `agent/BootstrapHooks.java:95-100 shouldSuppressRender` 的第二分支是
    `!safeEntity(entity) && shouldSuppressExternalVisual()`，而
    `RonovaDominion.java:37` 安装的是 `Entity.class::isInstance` ⇒ 该分支只对**根本不是 Entity 的参数**生效，
    **不是**"抑制其他 Mod 的实体"。
    其开关由 `client/ClientRuntime.java:27` 安装为
    `RECOVERY::isAuthorityRecoveryActive`——**仅客户端、仅在 Authority 恢复期间**的瞬态抑制。
- `presence/PresenceSplitFlags.java` 是一个独立、有 `Source` 溯源的显式开关
  （`PROPERTY = "ronova.714c.presenceSplit.enabled"`、`ENVIRONMENT = "RONOVA_714C_PRESENCE_SPLIT_ENABLED"`，
  `:32-73` 解析，默认 `true`），但它切的是 **7.14C 的服务端/客户端判定分半**
  （见 `engine/QuarantineView.java:16-26`），**不是**独占列表过滤。两者不要混为一谈。
  该开关只能通过 JVM 参数或环境变量切换，即需要启动配置——与 R60 的取向不符。
- `entitydefence:228-234` 的 5 条后果**未在任何地方记录**。

⇒ N8 的整改是**确认 + 声明**，不是新建开关：确认是否存在任何形式的列表收窄，
并为已存在的瞬态抑制记录后果与开关语义。条目见 §5 的 C8。

### N9 —— 规范实例与修复契约（R46/R47/R49/R50/R51）

`entitydefence:62-128`、`:201-213`。重点是**注册表写入 hook 与读取 hook 作为修复触发点**——
Ronova 目前以周期修复为主。

### N10 —— 原生构建与自映像探针（R66）

`skills:447-452` 要求 Native 工程提供 CTest 或等价可执行探针，覆盖桥接、类恢复策略、**自映像保护**。
现状：`native/build-native.ps1` / `build-native-root.ps1` 是独立 PowerShell + Zig 脚本，未接入 Gradle；
`native/test/native_root_model_test.cpp` 存在但无构建/运行入口；
`build-native-root.ps1` 未做 PE 时间戳归一化（`build-native.ps1:19` 有，root 脚本没有）⇒ 可复现性不对称。

### N11 —— 调度收敛（R32/R64）

`coremod/command/DominionCommands.java:280-281` 对 14 个动作中的 13 个显示
`"未接线（仅保存请求）"`；`TerminalActionPolicy.java:48` 把结果钳制在 `REGISTERED_ENTITY_ONLY`。
`msitem:411` 的完成标准要求「所有击杀入口都收敛到一个可去重、可审计、服务端权威的动作调度器」。

> 这一条**不是违规**（未接线不等于违约），而是 `msitem:406-418` 完成标准意义上的缺口，
> 且它同时限制了 R62/R63 的可达性。

### N12 —— R1 边界：恢复链会改写按名字选中的 SavedData 文件（**已结论：不违反铁律**）

**这是唯一需要解释 R1 口径的行为。按 §0.4 的分层给出结论：不构成铁律违规。**

链路（全部经源码核实）：

1. 拦截是**名字无关**的：`mixin/SavedDataRecoverySaveMixin.java:50-60` 重定向
   `Lnet/minecraft/nbt/NbtIo;writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/io/File;)V`，
   捕获 Minecraft / Forge / **其他 Mod** 传进来的任意 `File`；
   落到 `observation/SavedDataSaveTrace.java:37,58`。
2. 该路径随后被**原生完整重写**：`observation/RecoverySourceTracker.java:161` →
   `observation/StorageTransactionTask.java:33` → `native/src/storage_transaction.hpp:139`
   （`GENERIC_READ|GENERIC_WRITE` 打开）、`:149` `WriteFile`、`:151` `SetEndOfFile` + `FlushFileBuffers`，
   以 KTM 事务提交（`:137`）。
3. 路径范围**不限定在 Ronova 自有目录**：`mixin/DimensionDataStorageRecoveryMixin.java:16-22`
   `folder.resolve(name+".dat").normalize()`，只有字符集过滤（拒绝空 / >512 / `/` `\` `:` / 控制字符）
   与父目录一致性检查，**没有 `ronova` 前缀要求**；存储名来自任意已注册的 `SavedData`
   （`RecoverySourceTracker.java:1043`、`:1778`）。
4. 另有对任意绝对路径的**只读**访问：`observation/RecoveryFileReadback.java:81`、
   `ronova_native.cpp:1112,1144`。
5. **不存在**对外部文件的删除 / 改名 / JAR 改写：
   native 侧检索 `DeleteFile|MoveFileEx|ReplaceFile|RemoveDirectory|SetFileInformationByHandle` → 0 命中。

两种读法：

| 读法 | 结论 |
|---|---|
| **字面读法**：`skills:62`「不能修改文件，包括依赖文件、Mod 文件」 | 若某个 `.dat` 属于其他 Mod 的 `SavedData`，写入即构成 R1 违规 |
| **目的读法**：三条铁律（`skills:62-64`）整体语境是"不要动对方的 Mod 安装 / 不要针对"，且 MS 自身要求 Mod 写文件（`skills:381`、`skills:235`） | 运行期世界存档不是"依赖文件/Mod 文件"，且该路径**名字无关**（不触 R2），属合法恢复行为 |

**结论（按 §0.4 铁律口径）：不违反 R1。** 三条理由是决定性的：

1. **枚举范围**。`skills:62` 明确枚举的是「依赖文件、Mod 文件」——即 Mod 的**分发产物**
   （`mods/` 下的 JAR 及其依赖）。世界存档里的 `SavedData` 是**运行期数据**，不是任何人的分发产物。
   与之配套的 `skills:64`「不能删对方 Mod 文件」同样指向分发产物。三条铁律的整体语境是
   「不要动对方的 Mod 安装」——这是共识所在。
2. **MS 自己要求写世界持久化**。`skills:137` 要求清理「服务器实体管理器中的可见查询、
   按 ID/UUID 索引、tick 列表、ChunkMap 追踪记录和 section 存储」；
   `entitydefence:141` 要求修复「持久实体分区管理器」并明确其失效后果是
   「区块切换和管理器回调可能丢弃实体」。**这些都在世界存档里。**
   若把存档写入算作 R1 违规，MS 自己的清除链与修复链就无法成立——该读法自相矛盾。
3. **不触 R2**。拦截按 `NbtIo.writeCompressed` 的调用点触发，按存储名解析路径，
   **不依赖任何第三方包名 / 类名 / Mod ID**；对同一次写入，无论作者是谁都一视同仁。

**唯一的自定项**（不属铁律）：是否要给"改写第三方 `SavedData`"加一个显式开关并记录后果。
这不是合规要求，是 Ronova 自己要不要做的问题（与 `entitydefence:228-236` 的取向一致）。
若要，实现要点是：

- **(a) 限定范围**：只写 Ronova 自己注册的 `SavedData`，其他 Mod 的只观察不改写 —— **能力显著缩小**；
- **(b) 显式开关**：保留能力但默认关闭 + 状态如实显示；
- **(c) 保留现状**：不做额外开关，但记录该行为的存在与后果。

### N13 —— 同一规则集在两处维护 + 原生名称启发式

**问题一：同一套字面量在 C++ 与 Java 各写一份。**

| 规则集 | 原生 | Java |
|---|---|---|
| 17 个语义目标 MC 类 | `ronova_native.cpp:304-321` `SEMANTIC_TARGETS`，经 `:368-370 is_core_target` | `agent/RonovaTransformer.java:24-34` |
| 候选类字符串（`net/minecraft/world/entity/`、`client/Minecraft`、`KeyMapping`、`MouseHandler`、`KeyboardHandler`、`gui/screens/Screen`、`Overlay`） | `ronova_native.cpp:458-488 is_candidate` | `agent/RonovaTransformer.java:68-85 shouldInspect`、`:473-486`、`:489-500` |
| SRG / 字段名（`f_146795_`、`f_20890_`、`f_20919_`、`removalReason`、`deathTime`、`isAddedToWorld`） | 同上 `:458-488` | `agent/RawAccessorContract.java`、`agent/NbtCopyContract.java` |
| JDK 任务目标（FutureTask、`ScheduledThreadPoolExecutor$ScheduledFutureTask`、ForkJoinTask、ThreadPoolExecutor、ScheduledThreadPoolExecutor、ForkJoinPool、`CallerRunsPolicy`） | `ronova_native.cpp:29-34 TASK_TARGETS` + `:56-61` 位映射 | `agent/JdkTaskBoundaryContract.java:13-24` |
| 客户端入口点名称 | `ronova_native.cpp:474-480` | `agent/RonovaTransformer.java:29-34`、`:474-486` |

⇒ 与 R31 同构（`skills:411` 的"单一来源"精神），且**同一规则改一处漏一处会导致语义证明与实际改写不一致**。
唯一的真委派是 `has_semantic_proof`（`:415-420` → `agent/NativeTransformBridge.java:9-21` → `SemanticGateVerifier`），
`target_bit`（`:354-359`）纯机械，`registered_future_kind`（`:983-994`）是精确 JDK 身份检查。

**问题二：`:388-402` 的归属启发式用通用英文子串判定"是不是我们的类"**——
`strstr(signature,"Entity") || "Renderer" || "Client" || "Screen" || "Input" || "Mouse" ||
"Keyboard" || "Attack" || "Combat" || "Execution" || "Lifecycle" || "Control" || "Guard" || "Hook" || "Mixin"`。

`"Client"` / `"Screen"` / `"Control"` / `"Guard"` / `"Hook"` / `"Mixin"` 这些词**同样出现在其他 Mod 的类名里**。
`:382` 已用 `Ldev/ronova/coremod/` 前缀排除自有命名空间，说明设计上是命名空间感知的；
但该启发式本身无法限制到任何特定 Mod。**当前不构成 R5 违规的证据**（R5 禁的是"依赖其他 Mod 的名称"，
而不是"可能误命中"），但它是 R5 的**风险面**，应在 C13 中一并收口为显式清单。

### N14 —— 客户端边界的一处未加保护的公共→客户端调用（R42，当前不可达）

`network/RonovaNetwork.java:3` 导入 `dev.ronova.coremod.client.ClientRuntime`，
`:20-22 public static void request(RonovaAction action) { ClientRuntime.queueAction(action); }`
**无 `Dist` / `DistExecutor` 保护**；而 `client/ClientRuntime.java:25-28` 的静态初始化会执行
`AgentHooks.install*`，`:9` 引用 `net.minecraft.client.Minecraft`。

**当前不可达**：全树检索 `RonovaNetwork.request` 无任何调用者。
但它是 R42（`msitem:343`：公共/服务端初始化不能触及客户端类型）的**潜伏违规**——
一旦有人接上这条调用，专用服务器会因类加载失败崩溃。

对照：其余同类调用都正确加了保护——
`client/RonovaClient.java:35,91` 与 `client/tooltip/RonovaTooltipEvents.java:20` 用 `value = Dist.CLIENT`；
`content/RonovaBladeItem.java:41,48`、`network/RonovaPackets.java:69,100,129`、
`network/StrengthControlPackets.java:46` 用 `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)`。

### N15 —— 改写的可达集合包含 Forge / ModLauncher / LWJGL / com.mojang（R8 点名的类）

Java 侧与原生侧用的是同一个"外部"定义——**既不是原版 MC、也不是 Ronova 自己**：

- `agent/RonovaTransformer.java:119-126`：
  `if (!className.startsWith("net/minecraft/") && !className.startsWith("dev/ronova/coremod/"))`
  ⇒ `patchExternalFieldWrites` / `patchExternalQueryCollections` / `patchExternalControlEntrypoints` /
  `patchExternalControlGuards` / `patchExternalRendererEntries` 全部对**其他 Mod 的类**生效。
- 原生侧同义：`ronova_native.cpp:465-466`
  `external = name 不以 "net/minecraft/" 开头 && 不以 "dev/ronova/coremod/" 开头`。
- 唯一的硬排除只有 `java/`、`jdk/`、`sun/`（`RonovaTransformer.java:70-72`、`ronova_native.cpp:461-463`）。

⇒ 可达集合**同时包含** `net/minecraftforge/**`（Forge 本身）、`cpw/mods/**`（ModLauncher）、
`org/lwjgl/**`（LWJGL）、`com/mojang/**`、`io/netty/**`。
而 `skills:348` 点名保护的正包括 **Forge** 与 **LWJGL**。

**是否已构成违规，取决于两点**：

1. `skills:348` 禁的是「**全局"全部方法直接返回"式**改写」，并要求「只改写有明确目标、
   可验证 descriptor 和回滚策略的入口」。Ronova 的改写**形式上是精确的**——
   `patchExternalControlEntrypoints`（`:293` 只选 void 返回）、`patchExternalControlGuards`（`:308` 只选 boolean）、
   `patchExternalRendererEntries`（`:326` 只选 void）、字段写入只替换具名 PUTFIELD（`BootstrapHooks.java:177-204`），
   都带 descriptor 门。**形式上合规。**
2. 但 `skills:322` 要求「预先设计回滚。打补丁前保存原始字节码，或明确用重启/重定义作为恢复方式」——
   **当前未见到原始字节码保存或显式回滚声明**。这一条是实的。

> **待运行时确认**：Forge / LWJGL 的类**实际是否命中** `shouldInspect` 的内容过滤。
> 过滤条件是字节码里出现 `net/minecraft/world/entity/`、`f_146795_`、`removalReason`、`deathTime` 等，
> Forge 内部类有命中的可能。本方案不据此断言已违规，但要求把可达集合**显式收窄到
> 「原版 MC + 其他 Mod」并排除 Forge/ModLauncher/LWJGL**，或给出命中后的处置契约。

### N16 —— 急切重转换的选取启发式按通用英文子串命中第三方类

`ronova_native.cpp:388-402 is_loaded_candidate_signature`（`include_external=true` 时）用
`strstr(signature, "Entity" | "Renderer" | "Client" | "Screen" | "Input" | "Mouse" | "Keyboard" |
"Attack" | "Combat" | "Execution" | "Lifecycle" | "Control" | "Guard" | "Hook" | "Mixin")`
挑选**已加载**的类做一轮急切重转换。

`:385-387` 的注释声明「字节码预筛仍做最终决定，名称只用于给已加载的生命周期/查询/客户端控制辅助类一次急切处理」——
**所以它不是判定，只是选取**。但：

- 这些词是通用英文词，**任何 Mod 的类名里出现 `Client` / `Screen` / `Control` / `Guard` / `Hook` / `Mixin`
  都会被选中并急切重转换**，构成对其他 Mod 的无差别打扰面。
- 这是 Ronova 中**最接近「按名称判定第三方」的一处**。它**不违反** `msitem:41`
  （该条约束的是**目标分类**，不是转换候选选取），但它是 R5 的风险面，应改为显式清单（与 N13 合并处理）。

### 4.1 对其他 Mod 的影响面（合规口径）

| 通道 | 是否触及其他 Mod | MS 判定 |
|---|---|---|
| **改写对方 `.jar` / 磁盘文件** | ❌ 无 | ✅ 不违反 R1/R3（native 检索 `DeleteFile\|MoveFileEx\|ReplaceFile` = 0 命中） |
| **按第三方包名 / 类名 / Mod ID / 私有实现判定目标** | ❌ 无 | ✅ 不违反 R2 / `msitem:41`（`QuarantineView.java:11-14` 明示「contains no knowledge of another mod's names」） |
| **删对方文件** | ❌ 无 | ✅ 不违反 R3 |
| **在内存中改写对方类的字节码** | ✅ **有**（N15） | ⚠️ **MS 明确要求**：`skills:243`「使用注入覆盖下列入口，**保证来自其他 Mod 的直接调用也无法绕过防御**」。形式精确 ⇒ 不违反 `skills:348`；但缺回滚声明（`skills:322`） |
| **急切重转换选取命中对方类名** | ✅ 有（N16） | ⚠️ 名称仅用于选取、不用于判定 ⇒ 不违反 `msitem:41`，但是 R5 风险面 |
| **跨所有权改写对方 `SavedData`** | ✅ 有（N12） | ❓ **待裁决**：R1 的字面 vs 目的读法 |
| **把对方实体从全局列表/渲染中剔除** | ❌ 无 | ✅ 已核实：`filterQuarantinedResult` 只剔 Ronova 自己的终结目标（R63 要求）；`shouldSuppressRender` 第二分支只对非 Entity 参数生效 |
| **进程外干涉对方** | ❌ 无 | ✅ 不违反 UX1 |

**结论**：对外**没有**"针对 / 改文件 / 删文件"这三类 MS 铁律行为；
唯一的铁律歧义是 **N12（跨所有权 SavedData 写入）**。
内存中改写对方字节码是**MS 主动要求**的防御手段，不是违规。
真正要收口的是 **N15 的可达集合**与 **N16 的选取启发式**。

### 4.2 已满足的项（同一口径下核对，避免只列缺口）
| 规则 | 现状 |
|---|---|
| **R14**（只用小范围注入 / 大范围覆写需说明兼容边界） | **完全满足**：`coremod/mixin/` 下 31 个 `@Mixin` 类，**`@Overwrite` 零处**；全部使用 `@Inject`(HEAD/RETURN，按需 `cancellable`)、精确 INVOKE 目标的 `@Redirect`、以及 `@Accessor`（41 处） |
| **R53**（`void` 不写返回值 / 有返回值必须显式写） | **完全满足**：14 处 void 注入一律只 `cancel()`；全仓库仅 5 处 `setReturnValue`，全部配对 `CallbackInfoReturnable`；ASM 侧同规则（`RonovaTransformer.java:357-367/382-395/397-411/427-436` 走 `RETURN`，`:369-380/438-448` 走 `ICONST_0`+`IRETURN`，`:293/308/326` 有返回类型门）；`JdkTaskBoundaryContract.java:112-133` 两种都显式处理。检索「在 void 方法上设返回值」→ **0 命中** |
| **R17 前半**（不得在类加载回调中递归重定义） | **完全满足**：`ClassFileLoadHook`（`ronova_native.cpp:523-529`）与 `ClassPrepare`（`:251-266`）只调 `request_integrity_refresh()`（`:279-284`：置位 + `SetEvent`），真正重转换在 watchdog 线程 `:713-729`，并有注释 `:717-718 // Never retransform recursively inside a class hook`；重入保护 `:534-537`/`:563`；Java 侧 transform 链不含 `Instrumentation`（`NativeTransformBridge.java:24-36` → `RonovaTransformer.java:88-136` 纯 ASM） |
| **R24 / R25**（桥接单请求在飞 / 不得无限自旋） | **满足**：桥接**不用共享槽位**，逐调用传参（`native_root_jni.cpp:87-111`、`:50-68`、`:120-142`），R24 前提不成立；Java 侧无自旋、无 `while` 等待（`coremod/nativebridge/*.java` 检索 `timeout\|await\|sleep\|while (` → 0 命中）；有界等待只在原生（`storage_transaction.hpp:136,141,147`）与专用 worker（`StorageTransactionTask.java:65`，5s 上限）。存储事务另有并发上限 `storage_transaction.hpp:123 ERROR_BUSY` + `:158-160` SRW 锁 |
| **R2 / R5**（不针对、不按第三方名称判定） | 满足：全仓库无按第三方包名 / Mod ID 判定目标的**判定路径**；唯一的类名字符串使用在 `observation/RecoveryBoundaryProof.java:41`，属栈帧诊断格式化。**例外风险见 N13 问题二**（原生归属启发式） |
| **R1 / R3**（自有产物写入 + 不删文件） | 自有写入全部合规且可审计：DLL 提取 `NativeBridge.java:134-145`（SHA-256 内容校验）、agent jar 临时文件 `RonovaAgent.java:57-68` + `deleteOnExit`、配置 `StartupPolicy.java:15,51-59`、Root 状态 `DurableRootFileBackend.java:191-207,276-279`、镜像 `SavedDataMirrorFiles.java:19,43-69`、诊断 `JdkTaskBoundaryContract.java:49-55`、基线归档 `BaselineArchiveWriter.java:53-65`。**无任何删除 / 改名 / JAR 改写**（native 侧 `DeleteFile\|MoveFileEx\|ReplaceFile\|RemoveDirectory\|SetFileInformationByHandle` → 0 命中）。**跨所有权写入见 N12** |
| **R26 前段**（Windows x64 前提检查） | 满足：`NativeBridge.java:35-39` |
| **R42**（客户端边界） | 基本满足：所有 `import net.minecraft.client.*` 都落在 `coremod/client/**` 或 `ronova.mixins.json:35-41` 的 5 个 client mixin 内；`engine/`、`bootstrap/`、`persistence/`、`observation/`、`root/`、`capability/`、`terminal/` 无一引用客户端类型。**一处潜伏例外见 N14** |

---

## 5. 加强块

编排沿用 A–H 字母以便与 V1.0 对照；新增 **I 块**承载 §3 重新纳入的手段。

### A 块 —— 前置与入口

| ID | 项 | 依据 | 备注 |
|---|---|---|---|
| **A1** | 注册 4 个 ModLauncher 服务：`ILaunchHandlerService` / `ITransformationService` / `ILaunchPluginService` / `ITransformerDiscoveryService` | `skills:395` 明确推荐 `ITransformationService` | 抢在 Forge 与 Mixin 之前 |
| **A2** | ServiceLoader 返回顺序重排 | 参考样本机制 | **限制**：只调整自有条目的相对序；不隐藏、不伪造任何第三方条目；每次重排记日志 |
| **A3** | JNI `DefineClass`(loader=null) 替代「解压 jar + `AddToBootstrapClassLoaderSearch`」 | 现状 `NativeBridge.java:45` | 消掉一次磁盘写入；引导层可见性相同 |
| **A4** | `ImmediateWindowProvider`（Forge 47.4.11+，Ronova 在 47.4.22） | `skills:394`：优先级「极其高」且 Mod File 能正常使用 | 与 A1 并列的第二条前置通道，同时满足 R59′ |
| **A5** | 程序化打开 Java 模块访问边界 | `skills:384` 列为启动顺序第 5 步 | 在无启动参数前提下只能走 I2 手段；这是 MS 自身顺序隐含的要求 |

> **A 块的附加职责（因 `UX1`）**：A1 / A4 / I7 是删除 `AutoAttach` 之后**唯一**的前置入口，
> 且必须是**进程内**的。A 块未就绪前不得执行 C15（见 §6 顺序图）。
> `skills:389` 要求这一整套启动步骤幂等；R59′ 要求前置能力不依赖 Mixin 转换服务。

### B 块 —— 原生能力位扩展（纯增量，性价比最高）

现状：`native/src/ronova_native.cpp:817-823` 只申请 4 项
（`can_retransform_classes` / `can_retransform_any_class` / `can_generate_all_class_hook_events` / `can_access_local_variables`）。

| ID | 能力位 | `jvmti.h` 行 | 打开的能力 |
|---|---|---|---|
| **B1** | `can_set_native_method_prefix` | `:706`，配套 `SetNativeMethodPrefix`(`:1417`) / **`SetNativeMethodPrefixes`(`:1421`)** | **T0-② 完整形态，且覆盖多 classloader**。这就是 `skills:59` 点名的「JNCT/JLTN：不用 Native 的 Native」 |
| **B2** | `can_get_bytecodes` | `:673`（`GetBytecodes` `:1426`） | 语义不变量级证明的基础（F1） |
| **B3** | `can_get_constant_pool` | `:705` | 校验常量池引用目标 |
| **B4** | `can_generate_compiled_method_load_events` | `:697` | 观察 JIT 编译/回退事件，关闭 `attack.dispatch` 的 JIT 缺口 |
| **B5** | `can_tag_objects` | `:670` | 跨事件关联同一对象，不依赖弱引用 |
| **B6** | `can_get_source_file_name` / `can_get_line_numbers` | `:681` / `:682` | 诊断归因 |
| **B7** | `can_retransform_any_class`（已申请，启用其未用部分） | `:708` | 在外部 transform 之后强制重转换 = T0-③ 的等效形态 |

> **不得申请**：`can_generate_native_method_bindings` —— JDK 17 的 `jvmti.h` 中不存在（JDK 8 有，9 已移除）。

约束：新增事件回调一律走有界队列（R18/R36），不在回调内做重活。

### C 块 —— 收口项（C15 是强制；C1–C14、C16–C18 为 L1 自定）

> **分层说明（§0.4）**：C1–C14、C16–C18 对应的是 **L1 自定项**——MS 的建议，不是义务。
> 本方案的**建议是全部采纳**（理由见各自「自定建议」列：低风险、或直接决定对抗强度），
> 但 Ronova 有权不做或改做，只需自洽。
> **C15 例外**：它对应 `UX1`（用户明示约束），是强制的。
>
> **C1 / C2 / C3 / C4 / C6 的逐条实施规格**（含目标形态、代码落点与判据）见
> [`Ronova_MS_Remediation_Spec_V1.0.md`](Ronova_MS_Remediation_Spec_V1.0.md)（S1–S6）。

| ID | 对应缺口 | 动作 |
|---|---|---|
| **C1** | N1 | **判定收回 Java**：`skills:358` 要求策略/分类/物品规则留在 Java 层。三选一：<br>(a) `native_root` 收窄为状态保持 + 重放/对账后端，`decide()` 整体移出；<br>(b) 保留 native 判定但**取消 Java 并行实现**，并把 native 定位在 `skills:362-370` 的窄命令集内；<br>(c) 彻底删除 native 判定内核（4 个域中已有 3 个不走 native）。<br>**三者都必须消除「同一判定两份实现」；并同时决定 `LocalDominionCore:26-31` 的 Java 二次裁决是否保留** |
| **C2** | N2 | 由构建任务从单一来源生成 DLL 名 / 资源路径 / 拷贝路径并校验（R31） |
| **C3** | N3 | 补 Java 17 前提检查与独立 `ping` 步（R26），失败时如实置降级态 |
| **C4** | N4 | 为每个注入点声明失败策略；顶层 `catch (Throwable ignored)` 改为记录原因并上报（R58） |
| **C5** | N5 | 31 个 Mixin 逐条补七字段：**逻辑侧**（`Dist` / 拆文件）、**顺序**（`priority`）、**逐站点失败策略**、**验证引用**；并把只写方法名的定位补成精确 descriptor（`"setScreen"` / `"addEntity"` / `"startTracking"` / `"startTicking"` 等，R57 + `skills:307`） |
| **C6** | N6 | 重写 `agent/RonovaAgent.java:117-133` 的 `reassertTargets()`：单 worker + 阻塞队列、同类去重、每批 ≤32、单批失败记录类名/阶段/异常并降级逐个处理。**姿态对齐原生侧已有的 lease 记账**（`ronova_native.cpp:686-688`、`:694-706`），并把原生侧同样缺失的批量上限（`:622-623`、`:588`、`:667`、`:226`）一并补上 |
| **C7** | N7 | 为每个关键能力点名第二通道（非 Mixin），并在状态命令中如实显示生效层（R59′、R58） |
| **C8** | N8 | 确认是否存在任何形式的列表收窄（已核实：**目前没有**对全局实体列表的独占过滤）；为客户端瞬态外部抑制（`ClientRuntime.java:27` → `RECOVERY::isAuthorityRecoveryActive`）记录 `entitydefence:228-234` 的 5 条后果与开关语义；把 `PresenceSplitFlags` 与独占过滤两件事在文档中分开 |
| **C9** | N9 | 规范实例 5 规则、7 不变量、六类修复点（含注册表读写 hook）与读侧兜底对齐（R46/R47/R49/R50/R51） |
| **C10** | N10 | 原生构建接入 Gradle 包装任务 + 提供等价可执行探针；消除 root 脚本的时间戳归一化缺口（R66） |
| **C11** | N11 | 14 个动作的执行器接线与调度收敛（R32/R64） |
| **C12** | N12 | **不阻塞**：N12 已结论为不违反 L0 铁律，E 块可照常推进。本项只剩自定选择——要不要给跨所有权 `SavedData` 写入加显式开关（N12 的 (a)/(b)/(c)） |
| **C13** | N13 | 把原生与 Java 各写一份的目标清单 / 字段名 / JDK 任务表 / 客户端入口名收敛为**单一来源**（构建期生成或运行期注入，R31 精神）；`:388-402` 的通用子串归属启发式改为显式清单 |
| **C14** | N14 | 删除 `network/RonovaNetwork.java:20-22` 这条未加保护的公共→客户端调用，或补 `DistExecutor` 保护（R42） |
| **C15** | **UX1** | **删除 `coremod/agent/AutoAttach.java` + `coremod/agent/AttachHelper.java`**（唯一的子进程派生点）。前置能力由 **A1**（ModLauncher 服务）+ **A4**（`ImmediateWindowProvider`）+ **I7**（进程内 attach 管道）承接；同时把 `StartupPolicy.ProtectionMode.DUAL_AGENT` 的默认值与语义按新入口重写，并在状态中如实显示当前生效的前置层（R58）。**保留 `NativeBridge` 进程内加载两个 DLL——`UX1` 只约束进程模型** |
| **C16** | N15 | 把改写可达集合**显式收窄**：排除 `net/minecraftforge/`、`cpw/mods/`、`org/lwjgl/`、`com/mojang/`、`io/netty/`（`skills:348` 点名保护 Forge 与 LWJGL），或给出命中后的处置契约。同步给 `RonovaTransformer` 与 `ronova_native.cpp:465-466` 两处（与 C13 合并为单一来源） |
| **C17** | N15 附 / R13 | 落实 `skills:322` 的回滚：补丁前保存原始字节码，或明文声明以重启/重定义作为恢复方式；并满足 `skills:335`「保存最终输出字节码，供后续 redefine 使用」（B2 `GetBytecodes` 是其技术基础） |
| **C18** | N16 | 把 `ronova_native.cpp:388-402` 的通用英文子串选取改为**显式清单**（与 C13 合并），避免无差别急切重转换第三方类 |

### D 块 —— 执行器与结算（能力本体）

| ID | 项 | 现状 |
|---|---|---|
| **D1** | 13 个未接线动作的执行器 | `DominionCommands.java:280-281` |
| **D2** | `CompletionTracker` 补生产者 | 9 项条件中 6 项零引用 |
| **D3** | `TerminalLifecycle` 服务器分支 | `TerminalLifecycle.java:49-62` 每条路径都 return，整体不可达 |
| **D4** | 单一结算所有者的显式化 | R32/R33：事件层、注入层不得各自结算 |

### E 块 —— 图与证书

| ID | 项 | 关键约束 |
|---|---|---|
| **E1** | 候选一等公民 + 置信度模型 | 复用 `obligation/CandidateEvidence`；提升须独立证据 |
| **E2** | `MutationTrace` | 不得凭字符串 / 包名 / 同父类判归属（R5） |
| **E3** | `ResultGraph` | 不能创造 Judgment、不能单独撤销 Tombstone、不能自己推进 TargetEpoch；容量语义零截断 |
| **E4** | `ExistenceTerminationCertificate` | 不能成为 `denySpawn()` 的 Authority；失效不自动回滚已提交 Judgment |

### F 块 —— 证明与策略收口

| ID | 项 | 依据 |
|---|---|---|
| **F1** | `AttestationManifest`：语义不变量清单（非整类 hash） | 基础是 B2 |
| **F2** | `LoaderProofRegistry`：多 ClassLoader all-copies 证明 | R12（继承/多副本） |
| **F3** | `EnforcementLease`：proof → `ACTIVE` / `PROVISIONAL` / `FENCED` | R58 的如实呈现 |
| **F4** | 逐键矩阵 revision 2 | 现 `ProfilePresetMatrix:145-150` 只实现三种统一填充 |
| **F5** | 天花板管道 | `supportedCeilings` 默认 `OFF`（`StrengthPolicySnapshot.java:99`），`withCapability` 只有 2 个调用点 |
| **F6** | 两套权限路径合一 | `registered*` 用预设 `requestedLevel`，`resolve()` 用 `effectiveLevel` |
| **F7** | **自映像保护的可验证化** | R61：「字节码不会被别人修改到」是 MS 完成标准；CodeSeal 是其基础 |

### G 块 —— 原生强制下沉

| ID | 项 | 说明 |
|---|---|---|
| **G-1** | `WriteSink` / lease 下沉到原生 | 现状每次强制都回调 Java（`ronova_native.cpp:135` `CallBooleanMethod(lease, valid, plan)`） |

> 与 C1 的关系：C1 决定「判定在哪」，G-1 决定「执行在哪」。
> 二者合起来才让原生层回到 R22 的「小型受约束执行后端」定位。

### H 块 —— 新维度

| ID | 项 | 依据 |
|---|---|---|
| **H1** | COLD 休眠窗口的事件驱动唤醒（退避即攻击窗口） | Native 已有 `ClassPrepare` 触发刷新的先例 |
| **H2** | 候选 flood 的配额分配：按证据强度分配，保证高置信候选的最低配额 | R36（有界预算） |
| **H3** | 按需重建 incarnation | R46 第 4 条「有意替换必须走显式生命周期」 |

### I 块 —— 本轮新纳入的高强度手段（V1.0 明确不采纳的翻案）

| ID | 项 | MS 依据 | 准入条件（全部可判定） |
|---|---|---|---|
| **I-1** | 精确 JDK 类改写（非全局） | `skills:348` 只禁「全局全返回式」 | 精确目标 + 可验证 descriptor + 回滚 R13 + 小范围注入 R14 |
| **I-2** | `Unsafe` / `ReflectionFactory` 包访问 | `skills:324` 明文允许 | ① 仅已验证的 JDK 17 x64 布局；② 其他 JVM / JDK 更新 / 其他架构 / 布局探测失败 ⇒ **立即拒绝执行**；③ 不得宽泛改写 JDK、Forge、LWJGL、游戏核心类 |
| **I-3** | 对象头 / klass 元数据精确改写 | MS 无语 | 同 I-2 的 ①②③；另需可回滚 |
| **I-4** | 图形栈 hook（DXGI / OpenGL） | MS 无语 | 只服务 R62 的显示判据；不得违反 R41 |
| **I-5** | 隐藏类 / 运行时内存编译 | `skills:355` 明文列为合法用途 | 仍受 R21/R22：不承载业务规则 |
| **I-6** | engine 函数级 inline hook | MS 无语 | R10 探测-验证；失败即拒绝 |
| **I-7** | attach 管道直连（`\\.\pipe\javatool<N>` 形态） | MS 无语 | R10；作为 A1/A4 之外的可选第三条前置通道 |
| **I-8** | 外部 helper 进程 | MS 无语 | **已排除**：触 UX1。见 §3 排除表与 §5 的 C15 |
| **I-9** | 运行时解密载荷 + `DefineClass` | `skills:354-356` | R30 依赖隔离 |
| **I-10** | 载荷伪装 | MS 无语 | 与自有可验证性取向冲突，属取舍 |

**I 块的定位**：这些是「MS 未禁止、且能提升强度」的空间。它们**不是**本方案的优先级 —
优先级由 §6 的依赖顺序决定；I 块的价值在于**取消了一条没有依据的自我限制**。

特别地，**B1 与 I-2 是同一技术路线的两端**：B1 用官方 JVMTI（`SetNativeMethodPrefixes`）
拿到「纯 Java Hook Native 函数」，I-2 用 `skills:324` 明许的 `Unsafe` 路线拿到更低层。
两者都应在实现「失败即拒绝」的前提下推进。

---

## 6. 实施顺序

```
强制（UX1，与 L0 铁律同级）
  └─ C15 删除 AutoAttach / AttachHelper —— **A 块未就绪前不得执行**
        │
        ▼
C 块（L1 自定；V2.0 建议全做，但由 Ronova 自己决定）
  ├─ C1 判定收回 Java（N1，R22）—— 改动面最大，放最后以免与其他项调试互相干扰
  ├─ C2/C3/C4 单一来源 + 启动前提 + 失败策略
  ├─ C5/C6/C7 契约与纪律
  └─ C13/C14 规则集单一来源 + 客户端边界收口
        │
        ▼
A 块（前置与入口：A1/A4 双通道）──► B 块（原生能力位）
        │                              │
        │                              └── B1 = T0-② 完整形态
        │
        └── C15 移除 AutoAttach 后，A 块是被删入口的唯一替代
        ▼
D 块（执行器与结算收敛）──► F 块（证明）──► E 块（图与证书）
        │
        ▼
G 块（原生强制下沉）──► H 块（新维度）

I 块（新纳入手段）与 B 块并行推进，逐项按准入条件实现「探测 → 验证 → 失败即拒绝」
```

**顺序理由**：

1. **C15 是唯一的强制项**——它对应 `UX1`，与铁律同级；且必须先有 A 块替代，否则留下入口真空；
2. **C1 放最后**——它是改动面最大的一项（动 C++ 导出与 Java 装配），且只是 L1 建议；
   早做会与其他项的调试相互干扰；
3. **C2/C3/C4 先做**——都是小改动、无行为风险，收益立即可见；
4. **A 块**——R59′（关键能力需非 Mixin 通道）与 C15 都依赖它；
5. **B 块**——纯增量，B1 直接对应 `skills:59` 点名的技术；
6. **D → F → E**——D 是能力本体，F 的 F1 依赖 B2，E 最重；
7. **G、H 最后**——工程量大且触及核心状态机。

**注**：原「N12 裁决最先」已删除——N12 已结论为不违反铁律（§4 N12），不再阻塞任何块。

---

## 7. MS 文档之外的约束与裁决

> 编号：`UX1` = 用户明示约束（强制，无 MS 依据）；`U2` = 自有取向。
> **本节的裁决项已清空**：N12 的 R1 口径问题已在 §4 的 N12 得出结论（不违反铁律）。

### N12 —— 已结论：不违反铁律（记录备查）

按 §0.4 的 L0 口径，跨所有权 `SavedData` 写入**不构成 R1 违规**。三条决定性理由
（枚举范围 / MS 自己要求写世界持久化 / 不触 R2）见 §4 的 N12。

因此 **E 块（恢复链）不受合规阻碍**，可以按原计划推进。
剩下的只是自定项：要不要给该行为加显式开关（N12 的 (a)/(b)/(c)），
**不属合规要求**，不阻塞任何块。

### UX1 —— 「无外部」（不起子进程）—— **已确认，强制约束**

**2026-09-12 用户明确要求加入。** 它没有 MS 文档依据，但**与 L0 铁律同等强制**，
因此用 `UX` 编号单独列出，不被误读为 MS 要求。规则定义与判定方法见 §1 闸门 0 附加。

**为什么它值得单列**：MS 三份文档对进程模型**零表述**。可复核判据：在三份 ZH 原文上检索
`进程|process|Process|子进程|helper|attach|Attach` —— **0 命中**。
所以在纯 MS 口径下 `AutoAttach` 不违规；是用户把「无外部」加到了规则基之上。

**唯一的违反点与其代价**：

- `coremod/agent/AutoAttach.java:26-33` 派生子 JVM +
  `AttachHelper.java:13-21` 的 `VirtualMachine.attach(pid).loadAgent(jar).detach()`。
  全主源码检索确认**这是唯一一处**（其余 `.start()` 全是 `Thread.start()`）。
- 它是**自指向**的（`ProcessHandle.current().pid()` + 自身 JAR），不触 R2；
  存在的目的是避免要求 `jdk.attach.allowAttachSelf`，即服务 R60 的取向。
- 移除后**必须由 A 块补上前置能力**，否则会留下入口真空。替代路径有三条，
  全部是进程内的：**A1**（4 个 ModLauncher 服务）、**A4**（`ImmediateWindowProvider`，
  `skills:394` 称其优先级「极其高」且 Mod File 可正常使用）、**I7**（进程内 attach 管道）。
- 落地顺序上 **A 块未就绪前不得执行 C15**（见 §6）。

⇒ 动作项：**§5 的 C15**。同时 `I8`（外部 helper 进程）已从 §3 的可选空间移入排除表。

### U2 —— 自有的可验证性取向（不是裁决项，是优先级说明）

Ronova 的差异化资产是 CodeSeal + 证据链 + Root 单一 writer。
I-10（载荷伪装）、I-9（加密载荷）与这套取向冲突，但**不与 MS 冲突**。
本方案不把它们列为「不采纳」，只列为「与自有取向冲突，优先级最低」。

---

## 8. 验证：全部后置

**本方案不含验证项，也不建议在任何实现块完成前启动验证。** 依据用户明确指令，不引用项目文档纪律。

以下三项此前被误列为「立即」，全部后移：

| 项 | 为什么后置 |
|---|---|
| 肉眼验收清单（R62/R63 三+三条） | D 块 13 个执行器未接线时测不出结果（N11） |
| 对抗性验证夹具 | 覆盖度取决于 E 块是否建成 |
| 合规自证静态审计 | 应在 §1 规则基对应的实现全部落地后做一次性审计 |

**启动验证的门槛**：D 块（执行器）与 E 块（图与证书）接线完成。
届时 R61/R62/R63/R64/R65/R66 的判据可一次性对照，不需要分批制造游戏 PASS。

---

## 9. 完成判定

本方案不定义新的「加强已完成」结论。判据按 §0.4 分层——**强制项与自定项分开**。

### 9.1 强制项（L0 铁律 + UX 用户约束）——不达成即不合格

| 项 | 判据 |
|---|---|
| **L0 · R1** | 写操作清单可审计：仅写自有产物（DLL 提取、agent 临时文件、配置、Root 状态、镜像、诊断、基线归档）；不改第三方 / 依赖 / Mod / Forge 文件。跨所有权 `SavedData` 写入按 N12 结论不属 R1 |
| **L0 · R2** | 全仓库无按第三方包名 / 类名 / Mod ID / 私有实现判定目标的路径 |
| **L0 · R3** | 无任何删除 / 改名 / JAR 改写（native 侧 `DeleteFile\|MoveFileEx\|ReplaceFile\|RemoveDirectory\|SetFileInformationByHandle` 保持 0 命中） |
| **UX · UX1** | 主源码中 `ProcessBuilder` / `Runtime.exec` / `ProcessHandle` 派生调用为 **0**；`AutoAttach` 与 `AttachHelper` 已删除且入口真空已由 A 块补齐；运行期进程树中无本 mod 派生的子进程 |

### 9.2 自定项（L1）——由 Ronova 自己决定，达成了才写进判据

以下判据**只在 Ronova 决定采纳对应 L1 条款时才生效**。不采纳不算失败，但必须自洽
（不得声称拥有未实现的能力）。

| 块 | 判据（仅当采纳时） |
|---|---|
| C（C1–C14、C16–C18） | 每项按 §5 各自的「自定建议」；若采纳：每个注入点有七字段契约与逐站点失败策略；同一规则集只有单一来源；改写可达集合已收窄且回滚已声明；无 `catch (Throwable ignored)` 形式的静默降级 |
| D | 14 个动作各有执行器；`CompletionTracker` 9 项各有生产者；`TerminalLifecycle` 服务器分支可达；结算所有者唯一 |
| E | 同目标在 ENHANCED 与 EXTREME 下实际纳入集合不同且逐条有证据 |
| F | 不变量在**实际生效字节码**上校验；lease `FENCED` 只封新 mutation，不撤既有保护；自映像保护可验证 |
| G | 原生持有 lease 且强制**不回调 Java**；判定逻辑在 Java（若采纳 C1） |
| H | 三项各自有契约与可复核行为记录 |
| I | 每一项都有「探测 → 验证 → 失败即拒绝」的实现与拒绝路径 |
| A / B | 前置通道在无启动参数下生效；ServiceLoader 重排**未减少**任何第三方条目；未申请到的 JVMTI 能力位如实显示不可用 |

**任何已声明采纳的项未达成，不得据此宣称加强已完成。**
未采纳的 L1 项不影响合规结论——但**它们影响强度**（见 §0.4 的三个理由）。

---

## 附：与 V1.0 的差异速查

| 维度 | V1.0 | V2.0 |
|---|---|---|
| 规则来源 | MS 文档 + V2.6 红线 + 自加 W 线 | **MS 三份文档 + 用户明示的 `UX1`** |
| **判定分层** | 无分层，W 线一律当红线 | **§0.4：L0 铁律（共识，强制）/ L1 MS 其余文本（自定，非违规）/ UX 用户约束（强制）** |
| 红线数 | W1–W8 | L0 = R1–R4；L1 参考库 = R5–R66；UX1 |
| 无效红线 | W4 / W7 / W8 | W7 / W8 删除；W5 降级为取向 |
| 高风险手段 | 11 项明确不采纳 | **9 项翻案为允许**（I1–I7、I9–I11）；I8 因 UX1 排除；另有 4 项因 L0 正据排除 |
| 无外部（W4） | 自加红线，随即被 V2.0 判为无依据 | **由用户明示重新加入，编号 UX1**；落地为 §5 的 C15 |
| N1（原生判定） | 「需用户裁决」 | **降为 L1 自定项**（4 个 native 域中 3 个已是死代码，建议按 S1 收口） |
| N12（跨所有权 SavedData） | 未识别 | **已结论：不违反 L0 铁律**，不阻塞 E 块 |
| 待裁决项 | 4 项 | **0 项** |
| 实施顺序 | C → A → B → D → F → E → G → H | 同序；C15（UX1）为强制首项，C1 为建议首项 |
| 验证 | 后置（引项目文档） | 后置（引用户指令） |

---

*本方案为设计文档，未修改工作区任何既有文件。*
