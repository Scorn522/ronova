# Ronova MS 未满足条款 —— 可选强化设计 V1.0

> **已并入（2026-09-12）**：S1–S6 已整合进 [`docs/DESIGN-SPEC.md`](../DESIGN-SPEC.md)
> 第二部分 §17 的 **C 块**（C1–C19），并按设计规范重新锚定。
> **本文件保留作对照，不再作为加强入口。**

日期：2026-09-12。性质：**设计规格**。未修改任何生产代码。

> **命名说明（2026-09-12 用户口径）**：原题「整改实施规格」隐含「这些是违规」。
> 按 `Ronova_MS_Rulebound_Enhancement_Plan_V2.0.md` §0.4 的分层，本文件覆盖的 6 条属 **L1 自定项**
> ——MS 的建议，**不是义务，不违反铁律**。故改述为「**可选强化设计**」。
> 文件内容（目标形态、代码落点、判据）不变，只是**判定力从"必须"降为"建议"**。

本文给出 [`Ronova_MS_Rulebound_Enhancement_Plan_V2.0.md`](Ronova_MS_Rulebound_Enhancement_Plan_V2.0.md)
§5 C 块中 **C1 / C2 / C3 / C4 / C6** 五项的具体落地设计——即 V2.0 §4.0「L1 自定项明细」里
Ronova 与 MS 建议不同的 6 处（含 R58）。

**要不要做由 Ronova 自己决定**；V2.0 的建议是**全部采纳**，理由见每条的「为什么值得做」。

| 本文项 | V2.0 缺口 | 规则 | MS 措辞 | 建议 |
|---|---|---|---|---|
| **S1** | N1 | `R22` = `skills:358` | 「**不能**成为业务规则的第二份实现」 | 采纳（3/4 已是死代码，删除代价最小） |
| **S2** | N6 | `R17` = `skills:340-346` | 「使用单工作线程和阻塞队列」/「去重」/「单批失败时**输出类名、阶段和异常**」 | 采纳（直接决定能否扛住对手的转换器重写） |
| **S3** | N5 | `R57` = `msitem:313-323` | 「**每一个**注入或转换声明都**必须**写清以下契约」（七字段） | 采纳（`descriptor` 缺失就是静默失效面） |
| **S4** | N2 | `R31` = `skills:411` | 「**不要**将 DLL 文件名、资源路径和 Gradle 拷贝路径拆成三份手写常量」 | 采纳（低风险，无行为变化） |
| **S5** | N3 | `R26` = `skills:380-382` | 「检查 Windows x64 **和 Java 17** 前提」/「**先调用 ping**」 | 采纳（低风险）；亦可不做 |
| **S6** | N4 | `R58` = `msitem:322` | 「**不能**静默假装能力仍完整」 | 采纳（与 S2 同源；自称的能力必须是真的） |

**边界**：本文只做这 6 项。不引入新编号体系（沿用 `S`/`C`/`R`/`N`），不改 `validation/7.17/acceptance-matrix.json`，
不声称任何 PASS。验证按 V2.0 §8 全部后置。

**强制项另行**：`UX1`（无外部）是用户明示约束，与铁律同等强制，其落地项是 V2.0 的 **C15**，
不在本文范围（本文只覆盖 L1 自定项）。

---

## S1 —— 判定收回 Java（R22 / `skills:358`）

### S1.1 违规事实

`skills:358` 全文要求：「Native 层应是小型、受约束的执行后端，**不能成为业务规则的第二份实现**。
秒杀策略、防御分类和物品规则**仍放在 Java 层**。」

现状是**两份实现 + 三重冗余**：

| # | 事实 | 位置 |
|---|---|---|
| 1 | 原生侧独立判定内核 | `native/src/native_root.cpp:353-410` `ronova::root::Decision decide(const Request&)`，按 `domain`(0..3) 与 `facts` 位域决定 accepted/scope/reason |
| 2 | Java 侧镜像实现 | `src/main/java/dev/ronova/coremod/core/LocalDominionDecisionSource.java:14-64`，复刻四个域与全部 reason 词表 |
| 3 | native 优先 + Java 兜底 | `nativebridge/NativeDominionDecisionSource.java:16-23` |
| 4 | Java 二次裁决 native | `core/LocalDominionCore.java:26-31` 重算 scope 上限与 `committed` |
| 5 | 入参位域由 Java 拼 | `nativebridge/NativeRootBridge.java:166-184` |
| 6 | **4 个域中 3 个不走 native** | Authority→`AuthorityView:214-217`；Admission→`SpawnAdmissionRuntime:228-231`/`:297-299`；Presence→`PresencePolicy:48-69`。唯一走 native 的是 domain 1（`JudgmentExecutor:96-100` 是全仓库唯一 `new DominionRequest` 站点） |

⇒ 采用 V2.0 C1 的 **(c) 方案：删除原生判定内核**。理由：它已是 3/4 死代码，
删除的代价最小，且是唯一能同时满足「判定在 Java」与「Native 是受约束后端」两句话的方案。

### S1.2 目标形态

| 层 | 职责（改后） |
|---|---|
| `ronova_root.dll` | **持久状态 + 集合成员查询 + 原子对账**。保留 `install` / `exportSnapshot` / `status` / `acknowledge` / `matches`（隔离集合匹配）。**不含任何 `accepted`/`reason` 决策** |
| `NativeRootBridge` | 只暴露状态类窄命令；命令集收敛到 `skills:363-370` 的表（`ping` / `init` / `retransformClasses` / `redefineClassesBatch` / `restore...` 形态） |
| Java | **唯一判定方**。`LocalDominionDecisionSource` 或其继任者是全部四个域的唯一入口 |

### S1.3 步骤

1. **删除** `native_root.cpp:353-410` 的 `decide()` 与 `native_root_jni.cpp:87-111` 的 `decide0` 导出；
   同步删除 `native_root.hpp` 中 `Request`/`Decision` 的决策语义（只保留对账所需的字段）。
2. **保留** `native_root.cpp:291-306 matches()` 与 `:318-351 install/exportSnapshot`——它们是状态查询与对账，
   不是判定，符合 `skills:358`。
3. **改写** `NativeDominionDecisionSource`：从「native 判定 → Java 兜底」改为
   「**向 native 取状态事实** → Java 判定」，或直接删除该类，让 `LocalDominionDecisionSource` 成为唯一实现
   （`core/LocalDominionCore.java:10-11` 的默认源本来就是它）。
4. **处理** `core/LocalDominionCore.java:26-31`：
   - 若第 3 步删除了可插拔源 → 连带删除二次裁决；
   - 若保留可插拔源（测试需要）→ 保留 scope 上限钳制，但**改写注释**，明确它是
     「对不可信来源的**安全钳制**」，并把 `decision.scope()` 的来源标注为非权威。
     **不得让它读起来像"第二份判定"。**
5. **删除** `NativeRootBridge.java:166-184` 的 `facts()` 组装（不再有消费者），
   连同 `:186-206` 的 reason 码表。
6. **删除** `NativeSnapshotCodec` 中仅服务判定的分支（`:105`、`:127-145`）。

### S1.4 判据

- 全仓库检索 reason 词表（`world-result` / `authority-subject` / `terminal-object` / `tombstone-match` /
  `cached-final-pass` / `final-pass` / `provisional-pass` / `quarantined` / `present`）——**每个词只出现一处**。
- `decide0` JNI 导出不存在；`ronova_root.dll` 的导出表里无判定入口。
- 四个域全部经由同一个 Java 入口；`grep -c "new DominionRequest"` 的行为不再决定路由。
- `/ronova status` 显示的判定来源为 Java，且不再出现 "native-first" 语义。

---

## S2 —— 重转换/重定义纪律（R17 / `skills:340-346`）

### S2.1 违规事实

`skills:340-346` 的五条要求逐条对照 `agent/RonovaAgent.java:117-133 reassertTargets()`
（由 `:99-115` 的 `Ronova-Integrity-Reassert` 线程按 `policy.reassertMillis()` 周期调用）：

| MS 要求 | 现状 |
|---|---|
| 「使用单工作线程和阻塞队列」 | ❌ 无队列：watchdog 线程直接调 `value.retransformClasses(targets)`（`:128`） |
| 「去重同一个类的重复请求」 | ❌ 每轮全量重扫 `getAllLoadedClasses()` 并按 `isTarget` 过滤（`:122-125`） |
| 「批量执行重定义，例如每批最多 32 个类」 | ❌ 把全部命中类一次传入（`:126-128`） |
| 「单批失败时输出类名、阶段和异常」 | ❌ `catch (Throwable ignored)`（`:129-131`），**零日志** |
| 「必要时降级为逐个处理」 | ❌ 无 |

原生侧（`ronova_native.cpp:622-623`、`:588`、`:667`、`:226`）同样**无批量上限**；
但原生侧**已有**失败记账与 lease 扣留（`:686-688`、`:694-706`）——**姿态更好，S2 应把 Java 对齐到它**。

> 注：`skills:345` 的「例如每批最多 32 个类」措辞是举例；但「单工作线程和阻塞队列」「去重」
> 「单批失败时输出类名、阶段和异常」是硬性措辞。32 作为实现常量采纳，便于与原生侧一致。

### S2.2 目标形态：`RedefinitionQueue`

新增 `src/main/java/dev/ronova/coremod/agent/RedefinitionQueue.java`：

```
单 daemon worker 线程 "Ronova-Redefinition-Worker"
  ├─ BlockingQueue<Class<?>> queue（有界，容量 4096；满时丢弃并记 RONOVA_REDEFINITION_OVERFLOW）
  ├─ Set<Class<?>> pending（ConcurrentHashMap.newKeySet()，去重用）
  ├─ 每批 drainTo(buffer, 32)
  ├─ 批成功 → 从 pending 移除该批
  └─ 批失败 → 记 RONOVA_RETRANSFORM_BATCH_FAILED batch=<n> phase=<...> classes=[...] cause=<...>
              → 逐个 retransformClasses(new Class[]{c})
              → 单个失败记 RONOVA_RETRANSFORM_CLASS_FAILED class=<internalName> cause=<...>
```

落点与约束：

1. `skills:343`「在**初始化完成后**延迟启动 worker」——worker 线程在 `RonovaAgent` 安装完成的回调里启动，
   不在 `premain`/`agentmain` 的静态初始化里。
2. 生产方：把 `reassertTargets` 的扫描结果 **投递**给队列，而不是直接 retransform。
   扫描仍在 watchdog 里（可保留周期），但队列负责去重 ⇒ 未变化的类不会被重复重转换。
3. `skills:334`「每个转换器返回 `null` 表示未修改」——队列只投递 `isTarget` 命中的类，
   不改变 `RonovaTransformer` 的返回值语义。
4. 原生侧：在 `retransform_targets` 等四处调用点前**切批**（每批 ≤32），
   逐批调用 `RetransformClasses`；单批失败沿用已有的 `g_failed_leases` 记账
   （`:686-688`）并补 `RONOVA_NATIVE_RETRANSFORM_BATCH_FAILED` 日志。
5. **不得**在 `ClassFileLoadHook` 内调用队列（`skills:340`）。
   现有实现已满足（`ronova_native.cpp:717-718` 有明确注释，hook 只 `SetEvent`），**S2 不得破坏它**。

### S2.3 判据

- 重转换路径上不存在 `catch (Throwable ignored)`；每次批量失败必有一行含**类名 + 阶段 + 异常**的日志。
- 单次 `retransformClasses` 调用的参数长度 ≤32（加断言或自检）。
- 同一类在一个扫描周期内被请求两次时，只发生一次重转换（自检可计数）。
- `skills:340` 保持成立：`ClassFileLoadHook` 内仍只有 `request_integrity_refresh()`。

---

## S3 —— 七字段注入契约（R57 / `msitem:313-323`）

### S3.1 违规事实

`msitem:313` 措辞是「每一个注入或转换声明都**必须**写清以下契约」，七字段为：
目标（所属类 + 精确方法/字段 + **JVM descriptor** + 映射策略）/ 位置 / 逻辑侧 / 条件 / 顺序 /
**失败策略** / 验证。

现状：31 个 `@Mixin` 类（`ronova.mixins.json:7-34` 26 个 common + `:35-41` 5 个 client），
注解计数 `@Inject` 26 / `@Redirect` 6 / `@Accessor` 41 / `@Unique` 3 / `@Shadow` 4 / `@ModifyVariable` 0。
抽样 7 处（`ServerLevelAdmissionMixin`、`PersistentEntityAdmissionMixin`、`EntityTypeNbtAdmissionMixin`、
`MinecraftScreenGuardMixin`、`ItemRendererMixin`、`SavedDataRecoverySaveMixin`、`EntityProtectionMixin`）
**最多命中 4/7**：

| 字段 | 现状 |
|---|---|
| 目标类 + 方法 | ✅ |
| **JVM descriptor** | ⚠️ 多数只写方法名：`"addEntity"`、`"startTracking"`、`"startTicking"`、`"setScreen"`、`"forceSetScreen"`、`"setOverlay"`、`"<init>"`、`"remove"` 等 |
| 位置 | ✅ |
| **逻辑侧** | ❌ 无 `@OnlyIn` / `Dist` / `FMLEnvironment.dist`；侧别只由 `ronova.mixins.json` 两个数组表达 |
| 条件 | ✅ |
| **顺序** | ❌ mixin 包内 `priority` 零命中 |
| **失败策略** | ⚠️ 仅容器级：`ronova.mixins.json:2 "required": true` + `:42-44 "defaultRequire": 1` |
| **验证** | ❌ 只有 ASM 转换器有（`SemanticGateVerifier`），Mixin 侧无 |

**先肯定**：`required: true` + `defaultRequire: 1` 是「失配即失败」的选择，方向与 `msitem:322` 一致；
`@Overwrite` 零处、`@Inject` 全用 HEAD/RETURN、`@Redirect` 全带精确 INVOKE 目标——
`skills:289` / `msitem:325` 的「优先小范围注入」**完全达标**。缺的是**逐站点声明**。

### S3.2 目标形态：可编译期检查的注解契约

新增 `src/main/java/dev/ronova/coremod/mixin/InjectionContract.java`：

```java
@Retention(RUNTIME)
@Target(METHOD)
public @interface InjectionContract {
    String owner();          // 目标类全限定名（开发名）
    String method();         // 精确方法名
    String descriptor();     // 精确 JVM descriptor —— 无默认值，必填
    Position position();     // HEAD | RETURN | INVOKE | FIELD_READ | FIELD_WRITE
    Side side();             // COMMON | CLIENT | SERVER
    int order() default 1000;
    OnMismatch onMismatch(); // FAIL_FAST | DISABLE_CAPABILITY | SAFE_DEGRADE
    String verification();   // 自检类名或用例 id —— 无默认值，必填
}
```

落地要点：

1. **为 26 个 `@Inject` + 6 个 `@Redirect` 的每个回调方法补注解**。
   逐站点补 `descriptor` ——这是 `skills:315`「不能只用方法名定位」的硬要求，
   也是本次整改**工作量最大**的一项。
   已正确写全 descriptor 的样例可作模板：
   `PersistentEntityAdmissionMixin.java:17-21` 的
   `"addEntity(Lnet/minecraft/world/level/entity/EntityAccess;Z)Z"`、
   `EntityTypeNbtAdmissionMixin.java:17-21` 的
   `"create(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"`。
2. **顺序**：对确实需要先后关系的站点补 `priority`（并按 `msitem:321` 写明与其他 Hook/事件/转换器的关系）。
   不需要的全部保持默认值，并在自检中显式记录「默认即无要求」。
3. **逻辑侧**：5 个 client mixin 显式标 `Side.CLIENT`，其余 `Side.COMMON`；
   与 `ronova.mixins.json` 的两个数组做一致性自检（不一致即失败）。
4. **失败策略**：默认填 `FAIL_FAST`（与现有 `required: true` / `defaultRequire: 1` 一致），
   个别确实要降级的填 `SAFE_DEGRADE`，并让该值**流入 S6 的能力降级上报**。
5. **验证**：指向已存在的自检，或新建最小用例；不允许留空。
6. 新增 `src/test/java/dev/ronova/coremod/mixin/InjectionContractSelfTest.java`：
   - 读 `ronova.mixins.json` 拿全部 mixin 类名；
   - 反射枚举每个类的方法，找出带 `@Inject`/`@Redirect` 的方法；
   - 断言每个都带完整的 `@InjectionContract`，且：
     `descriptor` 非空、`verification` 指向真实存在的类/用例、
     `side` 与配置数组一致、同 owner+method 的 `order` 不冲突。

### S3.3 判据

- `InjectionContractSelfTest` 通过：**任一声明缺任一字段即失败**。
- 每个 `@Inject`/`@Redirect` 站点都有精确 descriptor（不再有只写方法名的站点）。
- 逻辑侧自检与 `ronova.mixins.json` 的两个数组一致。
- 每个站点的 `onMismatch` 在 `/ronova status` 的降级清单中可追溯（与 S6 联动）。

---

## S4 —— DLL 名 / 资源路径 / 拷贝路径单一来源（R31 / `skills:411`）

### S4.1 违规事实

`skills:411`：「**不要**将 DLL 文件名、资源路径和 Gradle 拷贝路径拆成三份手写常量；
让构建任务从**一个来源**生成并校验。」

现状正是三处手写：

| # | 位置 | 内容 |
|---|---|---|
| 1 | `nativebridge/NativeBridge.java:14-16` | `"/native/windows-x86_64/ronova.dll"`、`"/native/windows-x86_64/ronova_root.dll"` |
| 2 | `native/build-native.ps1:8` | `src/main/resources/native/windows-x86_64/ronova.dll` |
| 3 | `native/build-native-root.ps1:8` | `src/main/resources/native/windows-x86_64/ronova_root.dll` |

同一违规还有**第二个面**（V2.0 的 N13）：目标类清单 / SRG 字段名 / JDK 任务表 / 客户端入口名
在 C++ 与 Java 各写一份：

| 规则集 | 原生 | Java |
|---|---|---|
| 17 个语义目标 | `ronova_native.cpp:304-321 SEMANTIC_TARGETS` | `agent/RonovaTransformer.java:24-34` |
| 候选类字符串 | `ronova_native.cpp:458-488` | `agent/RonovaTransformer.java:68-85`、`:473-486`、`:489-500` |
| SRG / 字段名 | 同上 | `agent/RawAccessorContract.java`、`agent/NbtCopyContract.java` |
| JDK 任务表 | `ronova_native.cpp:29-34` + `:56-61` | `agent/JdkTaskBoundaryContract.java:13-24` |
| 客户端入口名 | `ronova_native.cpp:474-480` | `agent/RonovaTransformer.java:29-34`、`:474-486` |

### S4.2 目标形态：`native/native-artifacts.json` 单一来源

```json
{
  "artifacts": [
    { "id": "ronova",      "dll": "ronova.dll",      "resource": "/native/windows-x86_64/ronova.dll",
      "sources": ["src/ronova_native.cpp"], "sealed": true },
    { "id": "ronova_root", "dll": "ronova_root.dll", "resource": "/native/windows-x86_64/ronova_root.dll",
      "sources": ["src/native_root.cpp", "src/native_root_jni.cpp"], "sealed": false }
  ],
  "targets": {
    "semantic": ["net/minecraft/world/entity/Entity", "..."],
    "jdkTasks": ["java/util/concurrent/FutureTask", "..."],
    "clientEntrypoints": ["net/minecraft/client/Minecraft"]
  }
}
```

生成与校验：

1. **Java 侧**：Gradle 任务从 JSON 生成 `NativeArtifacts`（常量类）到
   `build/generated/sources/ronova/java/...`，加入 `main` source set。
   `NativeBridge` 改引用生成常量，删掉 `:14-16` 手写字面量。
2. **C++ 侧**：同一任务生成 `build/generated/native/targets.generated.hpp`，
   由 `ronova_native.cpp` include，替换 `SEMANTIC_TARGETS` / `TASK_TARGETS` / 入口名字面量。
   `build-native.ps1` / `build-native-root.ps1` 加 `-I` 指向该目录。
3. **PowerShell 侧**：两个脚本用 `ConvertFrom-Json` 读同一 JSON 取 `dll` 与输出路径，
   删除脚本内的字面量。
4. **构建期校验**（`skills:411` 的「校验」）：新任务在 `jar` 之后断言——
   每个 artifact 的 `resource` 在 JAR 内存在，且其 SHA-256 与 `sources` 构建产物一致；
   不一致即构建失败。
5. **顺带**：`build-native-root.ps1` 目前缺 PE 时间戳归一化（`build-native.ps1:19` 有），
   两脚本走同一段归一化逻辑（V2.0 N10）。

### S4.3 判据

- 改名任一 DLL 只需编辑 **1 个文件**。
- 三处（Java 常量 / PS 脚本 / JAR 内路径）不一致时**构建失败**，而不是运行时静默。
- 目标类清单 / JDK 任务表在 C++ 与 Java 中来自同一生成源（`grep` 原字面量只命中生成物与 JSON）。

---

## S5 —— 启动前提检查与 `ping`（R26 / `skills:380-382`）

### S5.1 违规事实

`skills:378-387` 的启动顺序第 1 步是「检查 Windows x64 **和 Java 17** 前提」，
第 3 步是「加载 DLL，并**先调用 `ping`** 验证 JNI/JVMTI 绑定」。

现状 `NativeBridge.load`（`nativebridge/NativeBridge.java:31-81`）：

| MS 步骤 | 现状 |
|---|---|
| Windows x64 检查 | ✅ `:35-39`（`os.name` 含 win 且 `os.arch` 为 amd64/x86_64，否则 `UNSUPPORTED_PLATFORM`） |
| **Java 17 检查** | ❌ 完全没有 |
| 提取 DLL | ✅ `:42`、`:54` 经 `extract`（`:123-147`，按 SHA-256 内容校验） |
| **先 `ping`** | ❌ `:43` 直接 `bootstrap()` 返回能力位，无独立 `ping` 步 |
| 幂等 | ✅ `synchronized` + `state != NOT_ATTEMPTED` 早退（`:31-34`），符合 `skills:389` |

`skills:363-366` 的命令表把 `ping` 定义为「无参数 → 返回版本或可用状态」——这正是当前缺失的那一步。

### S5.2 步骤

1. **Java 17 前提检查**：在 `:35` 之前加运行时版本判定。
   - `java.specification.version == "17"` → 继续；
   - `< 17` → `State.UNSUPPORTED_RUNTIME`（新枚举值），不尝试加载；
   - `> 17` → 默认同样 `UNSUPPORTED_RUNTIME`，除非命中显式 allow-list。
     **理由**：`skills:324` 要求「在其他 JVM、JDK 更新、其他架构或布局探测失败时**必须立即拒绝执行**」；
     同理，JNI/JVMTI 的假设在非 17 上未经验证，按 MS 的取向应当拒绝而非静默降级。
2. **独立 `ping`**：新增 JNI 导出 `ping()`，返回版本 + 可用状态
   （`jvmti.h` 版本、构建 id、支持的能力掩码）。在 `System.load` **之后**、`bootstrap()` **之前**调用；
   `ping` 无应答 ⇒ `State.LOAD_FAILED`（或更精确的新状态），**不得**继续 bootstrap。
3. **顺序对齐 `skills:378-387`**：平台+版本检查 → 提取 → `load` → **`ping`** → `bootstrap`/初始化
   native agent 与 class-file hook → 打开 Java 模块访问边界 → 建 Java 端注册表/映射 →
   注册转换器并驱动重转换 → 再注册 Forge 内容。
   当前 `:43-61` 已是这个骨架，只缺 `ping` 与版本门槛。
4. **幂等**：`ping` 与 `bootstrap` 都必须在重复调用下无副作用（`skills:389`）。

### S5.3 判据

- 非 Java 17 运行时上，`/ronova status` 显示明确的前提拒绝状态，**不是**静默的 `JVMTI_LIMITED`。
- `ping` 在调用顺序上先于 `bootstrap`，其结果在 `/ronova status` 可见。
- 重复 `load` 不产生第二次 JVMTI hook 安装（现有幂等保持）。

---

## S6 —— 失败策略显式化（R58 / `msitem:322`）

### S6.1 违规事实

`msitem:322`：「目标失配时应失败、禁用能力还是安全降级；**不能静默假装能力仍完整**。」

| 位置 | 现状 |
|---|---|
| `agent/BootstrapHooks.java:64-65`、`:109-110`、`:216-218`、`:227-228`、`:244-245`、`:255-261` | 全部 `catch → false`（fail-open），**无声明、无诊断** |
| `nativebridge/NativeBridge.java:77-79` | `catch (Throwable ignored) { state = LOAD_FAILED; }` —— **丢弃原因** |
| `agent/RonovaAgent.java:129-131` | `catch (Throwable ignored)`（与 S2 同源） |
| Mixin 容器级 | ✅ `ronova.mixins.json:2 "required": true` + `:42-44 "defaultRequire": 1` —— 方向正确 |

`BootstrapHooks` 的 fail-open 本身是 `msitem:322` 允许的**安全降级**，
但它当前是**隐含行为**；顶层 `LOAD_FAILED` 丢弃原因，使得「能力为什么不完整」不可追溯。

### S6.2 步骤

1. **声明**：在 `BootstrapHooks` 类 javadoc 写明——
   全部 8 个 volatile 谓词槽的缺省与异常语义均为 **SAFE_DEGRADE（fail-open）**，
   并说明为什么（防注入回调把宿主拖崩优先于保护强度）。
2. **可观测**：每个谓词的 `catch` 分支改为**一次性**诊断
   （`RONOVA_BOOTSTRAP_PREDICATE_FAILED hook=<名称> cause=<异常类:消息>`），
   用 `AtomicBoolean` 去重，避免每 tick 刷日志（`skills:271` 的幂等取向）。
3. **保留原因**：`NativeBridge.java:77-79` 把 throwable 存进字段，
   经 `state()` / `/ronova status` 暴露 `loadFailureCause`。
4. **全量分类**：扫描主源码中所有 `catch (Throwable ignored)` / `catch (Exception ignored)`，逐处归类为
   - `FAIL_FAST`（能力不可用必须让调用方知道）、
   - `DISABLE_CAPABILITY`（关闭该能力并上报）、
   - `SAFE_DEGRADE`（继续但记诊断）；
   非能力路径（如 shutdown 清理）可保持静默，但**必须**加一行注释说明为何静默。
5. **汇总上报**：`/ronova status` 增加「降级清单」，列出当前处于降级的能力与原因；
   S3 的每个 `onMismatch` 值汇入同一清单（`msitem:322` 的「不静默」在用户可见层闭环）。

### S6.3 判据

- 能力相关路径上不再有**未注释**的 `catch (... ignored)`。
- `/ronova status` 能逐一列出降级能力及其原因；能力完整时不显示降级项。
- Mixin 失配（`defaultRequire: 1` 触发）在降级清单中可见，而不是只在启动日志里一闪。

---

## 实施顺序与依赖

```
S4（单一来源）──┐
                ├──► 可先做：只动构建与常量，无行为变化
S5（前提+ping）─┘
S6（失败策略）──── 依赖 S2 的日志通道；与 S3 的 onMismatch 汇流
S2（重转换队列）── 独立
S3（七字段契约）── 独立，工作量最大（26 + 6 个站点的 descriptor）
S1（判定收回）─── 结构性，动 C++ 导出与 Java 装配；建议 S2/S3 之后再做，避免同时改两条链路
```

**建议**：`S4 → S5 → S6 → S2 → S3 → S1`。
S4/S5 无行为风险且立即消除两条明文违规；S1 改动面最大，放最后以免与其他项的调试相互干扰。

---

## 不做什么

- 不改 `validation/7.17/acceptance-matrix.json`。
- 不声称任何 PASS；本规格的判据是**实现完成度**判据，不是游戏内验收。
- 不引入新的阶段/能力编号；落地时并入 V2.0 的 `C1`–`C4` / `C6` 与既有 `CF*` 体系。
- 不在本轮做 V2.0 §8 的三项后置验证。

---

*本规格为设计文档，未修改工作区任何既有文件。*
