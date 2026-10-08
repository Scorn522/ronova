> **fix5 来源保留页（2026-09-14）：交接原说明；融合处置已记录，不是新规范。** 当前实施入口是[能力接线规划的原生申请章节](../plans/R66-CAPABILITY-WIRING-PLAN.md#native-negotiation)，逐节处置见[融合映射](../plans/RESTRUCTURE-MAP.md#fix5-addendum-merge)。
>
> 下方是收到的原文，保留其当时的结论、编号与“待确认”状态；它们不冒充本轮重新执行的结果。`can_retransform_any_class` 字面计数的推论收窄及 `AddCapabilities` 的外部核对见当前入口。**V1.1 的旧机制误读不恢复效力，V1.2 的重复部分不再另算一批工作。** 本页原文之外的横幅才是融合状态说明。

<a id="handoff-original"></a>
<!-- RONOVA-HANDOFF-ORIGINAL-BEGIN MERGE -->
# 融合说明：Ronova 加强方案增补 V1.1 / V1.2

日期：2026-09-14。**本文件是转交说明，不是规范、不是实现记录。**

## 1. 这两份是什么、为什么外部流程没见过

| 文件 | 位置 | 大小 |
|---|---|---|
| `Ronova_Enhancement_Addendum_V1.1.md` | `docs/plans/` | 202 行 |
| `Ronova_Enhancement_Addendum_V1.2.md` | `docs/plans/` | 281 行 |

**来源**：这两份由会话内的助手在 **fix3 之后直接写入工作区**，
**不在任何覆盖包内**，也不在 `docs/plans/README.md` 的索引里。
外部流程是从它收到的包构建的，因此**从未见过这两个文件**。

**版本关系**：V1.1 → 已被 V1.2 取代（V1.1 顶部已有取代横幅）。
**V1.2 自包含**，不需要先读 V1.1。

## 2. 与现有规划的重复度：约 95% 已被覆盖

`docs/plans/R66-CAPABILITY-WIRING-PLAN.md`（fix4）已经覆盖了 V1.2 的绝大部分内容，
且处理更细。**重复部分不需要再融**：

| V1.2 章节 | 现状 |
|---|---|
| §1.1 `SetNativeMethodPrefixes` 不是按 classloader | 已被 wiring plan `#prefix` 覆盖 |
| §1.2 `NativeMethodBind` 不只是观测 | 已被 `#native-bind` 覆盖 |
| §1.3 `DynamicCodeGenerated` 收窄 | 已被 `#dynamic-code` 覆盖 |
| §1.4 局部通道存在且故意不发布 | 已被 `#source-corrections` + `#capability-pipeline` 覆盖 |
| §1.5 14 动作数字来自硬编码文案 | 已被 `#diagnostics` + `#action-wiring` 覆盖 |
| §1.6 `TerminalLifecycle` 守卫≠不可达 | 已被 `#lifecycle` 覆盖 |
| §3 修正后的 C13（三层不贯通） | 已被 `#capability-pipeline` 覆盖 |
| §4 五条方向重定位 | 已被 `#jvmti-directions` 覆盖 |
| §5 批次表 | 已被 `#batches` 覆盖 |

## 3. 需要融合的内容：四条（唯一未被覆盖的）

### 3.1 `can_retransform_any_class` **只写不读** —— 全部出现只有一处

```
native/src/ronova_native.cpp:821
    requested.can_retransform_any_class = potential.can_retransform_any_class;
```

全原生源码中该字段**仅此一次出现**，**从未被读取、验证或使用**。

**含义**：
- `B7`（用它在外部 transform 后强制重转换）**确实未实现**；
- 它的缺失**不会使任何现有路径失效** —— 没有任何代码依赖它；
- 因此它是"申请了但闲置"的能力位，不属于当前能力天花板的一部分。

**建议落点**：wiring plan 的 `#native-negotiation` 一节，作为逐项能力清单的一行。

### 3.2 `ThreadEnd` 事件启用返回值未检，也无对应 mask 位

```
ronova_native.cpp:834-835    SetEventCallbacks(...) == JVMTI_ERROR_NONE &&
                             SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, nullptr) == JVMTI_ERROR_NONE
                                 → mask |= CAP_HOOK                                    ← 有检查
ronova_native.cpp:837        SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_END, nullptr);
                                                                                       ← 无检查、无 mask 位
```

`ThreadEnd` 绑定 `task_thread_end`（`:830`），属任务观察链：
`:956-981` 的接收者身份核验依赖 `g_task_depth` / `g_task_frames`。

**含义**：若该事件未启用，**任务线程退出时的清理不会执行，且没有任何信号**。
与本项目"能力按实际报告"的取向相反（`D94` 一类要求）。

**建议落点**：wiring plan `#native-negotiation` 的逐项记录表，与 §3.1 同处。

### 3.3 `GetPotentialCapabilities` 返回值未检 + 无 `GetCapabilities()` 回读

```
ronova_native.cpp:819   g_jvmti->GetPotentialCapabilities(&potential);   ← 返回值未检
:820-823                requested.x = potential.x;   （四个条件申请字段）
:824                    if (AddCapabilities(&requested) == JVMTI_ERROR_NONE && requested.can_retransform_classes)
                            mask |= CAP_RETRANSFORM;
```

**三点事实**：
1. `GetPotentialCapabilities` 的返回值未检查；
2. **全文件无 `GetCapabilities()`** —— 实际**获授**集从未被读取；
3. 四个申请字段最终**只塌缩成两个 mask 位**（`CAP_RETRANSFORM`、`CAP_HOOK`），
   失败时不记录是哪一项导致的。

**注**：wiring plan `#native-negotiation` 已经给出了正确的目标形状
（`GetPhase + GetPotentialCapabilities` → `AddCapabilities` → **`GetCapabilities` 回读** → …）。
本条的价值是**点明"当前缺的正是回读这一步"**，可作为该节的现状对照。

### 3.4 待确认项：`AddCapabilities` 是否整体失败语义

若任一项不可加则全部不加，则**一项不可用会静默带走其余三项**，
包括核心的 `can_retransform_classes`，`mask` 直接丢失 `CAP_RETRANSFORM`，
`:839-846` 的急切重转换与 `CAP_SEMANTIC_LEASE` 全部跳过。

**本机无法访问 JVMTI 规范原文**（GitHub 与 Oracle 均不可达，DNS 假 IP），
**故标为待确认，不得当作结论**。有规范访问权限的一方应核实后再定级。

## 4. 建议处置

| 对象 | 建议 |
|---|---|
| §3.1 / §3.2 / §3.3 | 融入 `R66-CAPABILITY-WIRING-PLAN.md` 的 `#native-negotiation` |
| §3.4 | 记为已登记的待确认项，不写成结论 |
| `Ronova_Enhancement_Addendum_V1.2.md` | 其余内容判为**已并入**；可归档或加"已并入"横幅 |
| `Ronova_Enhancement_Addendum_V1.1.md` | **已被 V1.2 取代**，可归档或删除 |
| `docs/plans/README.md` | 若保留 V1.2，需决定是否加入索引；若不保留则无需改动 |

## 5. 边界与证据口径（不变）

- 硬限制仍是四项：不改依赖/Mod/Forge 文件、不针对、不删对方文件、**不起外部进程**。
  无参数为可选取向，不是第五条铁律。
- 本文全部结论来自**静态源码读取**，未编译、未启动游戏、未修改 `validation/`、未新增 PASS。
- 源码行号对应本工作区当前 r66 源码；两份增补文件中的 `jvmti.h` 行号取自
  `D:\桌面\MOD\.build-tools\jdk17-headers\jvmti.h`。
- **头文件里存在 ≠ 运行时支持**；`GetCapabilities` 回读才是实际获授读取口。

<!-- RONOVA-HANDOFF-ORIGINAL-END MERGE -->
