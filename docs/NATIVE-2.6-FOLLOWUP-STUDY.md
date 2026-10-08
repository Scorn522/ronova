# 永爱之刃 2.6 Native 第二轮研究

日期：2026-09-10。研究对象为用户提供的 2.6 fix 原 JAR 与 Native 还原补充包；完整输入哈希沿用 [输入清单](../validation/7.17/reference-study/input-manifest.json)。本轮新增独立 PE 结构检查、原 JAR 字节码核对、Java 调用点索引和自建最小反例。

结论：可以继续确认 Java→Native 边界及未受保护模块的静态行为，但目前仍不能宣称还原了核心 `wzzapi.dll` / `wzz_hook.dll` 的业务函数体。本轮发现的几个“成功”信号不足以证明真实效果，对 Ronova 的结果查证和代码完整性验证有直接参考价值。

## 1. 核心 DLL 的限制已独立核实

用本轮自行编写的 [只读 PE 检查脚本](../validation/7.17/reference-study/native-followup/audit_pe.py)，直接读取 ZIP 内 11 个原生二进制，检查节表、命名导出、RVA 和文件字节范围，不加载 DLL。

| 二进制 | 命名导出数 | 入口无原始磁盘字节数 |
|---|---:|---:|
| wzzapi.dll | 149 | 149 |
| wzz_hook.dll | 4 | 4 |
| wzzjvmti.dll | 10 | 0 |
| wzzSecurityHook.dll | 9 | 0 |
| wzzWindowDetector.dll | 4 | 0 |
| wzzdx2d.dll / wzzdx3d.dll | 各 6 | 0 |
| wzzmachine.dll | 2 | 0 |
| wzznotification.dll | 14 | 0 |
| wzzswaphook.dll | 8 | 0 |
| wzz.exe | 0 | 0 |

全部 11 个文件的命名导出及 RVA 与补充包元数据一致，见 [独立结果](../validation/7.17/reference-study/native-followup/pe-export-audit.json) 和 [交叉核对](../validation/7.17/reference-study/native-followup/pe-audit-crosscheck.json)。

这说明两个核心库的导出入口不能按普通文件偏移直接读到函数体。并不表示这些函数运行时为空或没有效果，也不表示其他节中不存在封装载荷。其余文件有入口字节，也只代表可继续静态分析，不代表业务已经完整还原。

## 2. 恢复包装层会丢失失败信息

`ClassBytecodeRestorer.restoreClass(Class, boolean)` 取得原字节、检查 class 魔数后调用内部 `redefineClass`，接着返回 true。内部方法对反射调用异常只写日志，不向调用者返回失败或重新抛出；方法未初始化时也直接返回。

已经核对原 JAR [字节码](../validation/7.17/reference-study/native-followup/ClassBytecodeRestorer.javap.txt)：外层调用后紧接 `iconst_1; ireturn`；内层异常处理器记录日志后 `return`。因此，在已获得原字节、通过前置检查，但实际重定义调用失败且日志正常返回的路径上，外层仍可给出成功。

这是静态控制流结论，本轮没有加载 Native 来触发故障。它不能证明成功重定义曾发生，也不能据此判断用户游戏中发生过失败。

同类问题还包括：读取已加载类发生异常时返回空数组；读取当前字节失败时返回 null；修改判定遇到缺失字节返回 false。空结果与“全部已发现”、false 与“确认未修改”没有被明确区分。

对 Ronova：ACK、尝试执行、返回结果、效果未知和独立复验必须继续分开；不得通过日志正常或辅助函数正常返回清偿义务。

## 3. “语义相同”比较漏掉操作数

`BytecodeComparator` 的比较过程读取类名，按顺序比较方法指令数量、每条指令的 opcode 和节点类型。没有比较常量操作数、被调用方法的 owner/name/descriptor、字段引用、跳转目标等语义内容，也没有完整比较字段、方法签名及异常表。

原 JAR 的 [比较器字节码](../validation/7.17/reference-study/native-followup/BytecodeComparator.javap.txt) 与这段反编译逻辑一致。另一个边界是：解析异常后按字节长度差判断，差值小于 200 时返回未修改；这不是可靠的语义证明。

本轮编译两个同名自建类，只改变一个返回常量，然后用 JDK 反汇编：

| 版本 | 方法指令 | 实际返回含义 |
|---|---|---|
| before | `bipush 7; ireturn` | 返回 7 |
| after | `bipush 9; ireturn` | 返回 9 |

两个 class 文件等长，仅文件偏移 164 的一个字节不同；opcode 和节点类型相同。依据已核实的算法，它无法区分这一对不同语义的输入。见 [反例源文件、哈希与结果](../validation/7.17/reference-study/native-followup/operand-fixture/result.json)。这是编译、反汇编自建样本后的静态反例，没有执行附件比较器或目标 Mod。

对 Ronova：代码证明应覆盖真实操作数、控制流、异常处理和定义身份。仅凭方法形状、长度或 opcode 序列不能授予动作许可。

## 4. 批量 API 的计数与参数不能按名称解读

原 JAR [WzzNative 字节码](../validation/7.17/reference-study/native-followup/WzzNative.javap.txt) 确认：

- `restoreClasses(true, classes)` 在每轮迭代向 `forceRestoreClass(Object)` 传入整个数组，而非当前元素；丢弃 Native 布尔返回值。由于真实 Native 函数体未恢复，无法确定它是否有数组处理语义，不能仅凭 Java 参数形状断言功能一定失败。
- `restoreClasses(false, classes)` 对每个元素调用 `restoreClass`，同样丢弃返回值。
- `restorePackage` 在匹配类后调用 Native 并递增计数，未根据布尔结果区分成功。该数值更接近尝试数，不能用作已验证成功数。

本轮保存了 18 个与所选恢复、实例、任务及 JVMTI 接口匹配的 [Java 调用点](../validation/7.17/reference-study/native-followup/java-callsites.tsv)。它是静态文本索引，不是完整运行时调用图；反射、动态生成或 Native 间接调用可能不在其中。

## 5. JVMTI 路径的已知与未知

`Jvmti.java` 的 disable 包装调用 `WzzNative.forceSetAllCapabilities`；它本身不直接证明能力已禁用。静态初始化还会加载 `wzzjvmti` 并调用能力设置。ModMain 源码存在禁用和移除 JVMTI 的调用点，故不只是接口声明；具体执行条件和运行结果尚未验证。

可读的 `wzzjvmti.dll` 导出伪代码包含指针链遍历、前驱/头指针改写等操作，对应 remove/add 接口。但链表布局、辅助函数、安全同步以及各 JVM 版本行为仍有未解释部分。应继续通过只读静态调用关系和原始指令核实，不把一次返回 true 当成所有观察器已经失效或全部恢复。

`getAllInstancesOf`、`deepClone`、`forceStopThread`、`restoreClass`、`retransformClasses` 等核心导出仍属于未还原函数体的库。本轮没有获得这些函数的精确所有权、对象生命周期、同步或原子性证明。

## 6. 后续研究与阶段一接入

可继续静态追踪未受保护 DLL 的 JNI 引用生命周期、回调注册与异常传播，并核对 Java 包装和真实导出是否一致。两个核心库若要确认业务函数，需要进一步还原封装载荷或获得对应运行时函数体；现有跳转、空填充及导出声明不能替代。当前还没有准备或执行动态加载实验。

本轮对阶段一的具体补充是两个故障场景：重定义失败被包装吞掉但返回成功；代码仅改变操作数而保持指令形状。它们用于检验“未知结果不冒充成功”和“实际代码证据失效后撤销许可”。自建反例证明的是参考样本算法的局限，不是 Ronova 已通过上述验收。

第一阶段状态及验收通过数不变。本次没有生成候选 JAR，没有修改原 Mod 或原存档。
