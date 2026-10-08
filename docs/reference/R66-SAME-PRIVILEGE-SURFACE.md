# R66 · 同权限干预面清点（含 mixin 层）

**性质：** 对"同权限对手能从哪里干预 Ronova"的**只读静态清点**。不是新提案，不新增方向/编号/批次。
**方法：** 逐条穷尽检索 + 逐个名单对账。**所有锚点均可照行号复核。**
**未做：** 未编译、未启动 JVM/游戏、未改源码、未改 `validation/`、未回填任何 PASS。

---

## 0. 口径

**威胁模型：** 同一 JVM 内的同权限参与者（另一个 Mod / 另一个 agent）。**不含** JVM 完全失陷——按 `L6-INV-10` 的准确含义，那不属于 Internal Root 的隔离保证。

**"肘击"的四种形态：**

| 形态 | 是否需要改字节码 | 现有封印是否覆盖 |
|---|---|---|
| 改字节码（JVMTI / inst / 转换服务） | 是 | 对已登记类**覆盖**，对未登记类**不覆盖** |
| 调 public API | **否** | **完全不覆盖** |
| 伪造落盘状态 | **否** | **不覆盖**（无密钥） |
| 抢 Mixin 注入点 | 是（同类机制） | 对未登记目标**不覆盖** |

**核心结论：Ronova 在字节码完整性上投入巨大（283 类封印 + 23 类语义租约），但另外三种形态基本没有对应防护。**

---

## 1. 摘要

| 块 | 缺口 | 严重度依据 |
|---|---|---|
| **A. mixin 目标覆盖** | 24 个 mixin 目标中 **10 个不在语义租约内**；**6 个 `@Redirect` 里 5 个在这 10 个里**，且全部落在持久化线 | 持久化是核心资产；`@Redirect` 是最脆的注入类型 |
| **B. 决定侧未封印** | 封印 283 类里 `policy` 包**只有 2 个消费者类**；**天花板计算链上的生产者一个都没封** | 改一处即可让全部 54 键静默报告不可用 |
| **C. API 表面敞开** | 无 JPMS、无 SecurityManager；`public static` 注册表 71 个调用点；**三个破坏性方法零鉴权** | 不需要碰字节码，封印零覆盖 |
| **D. 落盘无密钥** | 校验只有无密钥 SHA-256；`Hmac`/`Mac`/`SecretKey`/`javax.crypto`/`Signature`/`salt` **全 0 命中** | 能写存档目录即可造一致伪造 |

---

## 2. A 块：mixin 层

### 2.1 现状（规模与注入类型）

| 指标 | 值 |
|---|---|
| mixin 类 / 全部类 | 31 / 378 |
| mixin 行数 / 全部行数 | **872 / 40,428（2.2%）** |
| `@Accessor` | 41 |
| `@Inject` | 26（`HEAD` 21 / `RETURN` 3 / `TAIL` 2） |
| `@Redirect` | **6** |
| `@Overwrite` | **0** |
| 去重后的 `@Mixin` 目标 | **24** |

**零 `@Overwrite` 是重要的正面事实**：没有整段替换原方法的写法，冲突面小得多。21/26 注入在 `HEAD`，也是最稳的注入点。

### 2.2 非 mixin 的肘击向量（回答"mixin 会被什么肘"）

Mixin 的注入代码**住在目标类的字节码里**。所以凡能在字节码落地前后改写目标类的机制都能肘掉它，**不需要碰 Mixin 本身**：

| 向量 | 位置 | 相对 Mixin |
|---|---|---|
| 另一个 Forge `ITransformationService`（优先级更晚） | ModLauncher 管线内 | **下游** |
| 另一个 JVMTI agent 的 `ClassFileLoadHook` | 类定义前最后一站 | **下游** |
| `RetransformClasses` / `RedefineClasses` | 对已加载类再来一次 | **下游** |
| `java.lang.instrument` 的 `ClassFileTransformer`（可重转换） | 底层即 JVMTI 回调 | **下游** |

**全部在下游。** 它们拿到的是**已经打上注入的字节**，可以删掉，而 Mixin 早已跑完。

### 2.3 现有防线及其边界

**防线一：代码封印 —— 护不到 mixin。**
283 个类 / 111 个前缀，**全部 `dev/ronova/...`，零个 Minecraft 类**。机制是 ClassFileLoadHook 里用 `CODE_IMAGES` 原始字节强制覆盖回去（`native/src/ronova_native.cpp:508-515`）。**mixin 注入在 MC 类里，MC 类没有镜像 → 还原不了。**

**防线二：语义租约 —— 护 MC 类，但是"检测"不是"还原"。**

`SEMANTIC_TARGETS`（`:304-321`）= **16 个 MC 类**；`TASK_TARGETS`（`:29-34`）= **7 个 JDK 并发类**。

`reassert_core_targets`（`:629-692`）对全部目标 `RetransformClasses` → 经 Java 桥重施转换 → 只有

```cpp
:678-679  const bool complete = retransformed && required != 0 && (verified & required) == required
              && g_semantic_cycle_callbacks == targets.size() && initial_changes == g_semantic_outside_changes;
```

全部满足才发**有时限的租约**（`g_last_success_tick` + `semantic_lease_current(now)`），失败计数导出给 Java（`values[7] = g_semantic_failures`）。

**能检测、能发租约、能记账；不能还原字节。**

### 2.4 缺口：10 个 mixin 目标不在租约内

24 个去重目标对账：**14 覆盖 / 10 未覆盖**。

| 未覆盖目标 | mixin | 领域 |
|---|---|---|
| `ListTag` | `ListTagWriteObservationMixin` | **持久化 / NBT** |
| `NbtIo` | `NbtCompressedWriteObservationMixin` | **持久化 / NBT** |
| `DimensionDataStorage` | `DimensionDataStorageRecoveryMixin` | **持久化** |
| `SavedData` | `SavedDataRecoverySaveMixin` | **持久化** |
| `Connection` | `ConnectionProtectionMixin` | 网络 |
| `ServerGamePacketListenerImpl` | `ServerConnectionProtectionMixin` | 网络 |
| `MinecraftServer` | `MinecraftServerRuntimeMixin` | 服务端 |
| `Minecraft` | `MinecraftClientRuntimeMixin` / `MinecraftScreenGuardMixin` | 客户端 |
| `MouseHandler` | `MouseHandlerInputMixin` | 输入 |
| `ItemRenderer` | `ItemRendererMixin` | 渲染 |

**6 个 `@Redirect` 的覆盖情况——只有 1 个被覆盖：**

```
CompoundTagWriteObservationMixin:11   → CompoundTag          ✅ 有租约
ListTagWriteObservationMixin:10       → ListTag              ❌
NbtCompressedWriteObservationMixin:11 → NbtIo                ❌
DimensionDataStorageRecoveryMixin:27  → DimensionDataStorage ❌
SavedDataRecoverySaveMixin:37         → SavedData            ❌
SavedDataRecoverySaveMixin:55         → SavedData            ❌
```

**`@Redirect` 依赖目标调用的精确签名，是最脆的注入类型；而它们 5/6 落在持久化线上——耐久性权威所在的那条线。这条线上没有租约。**

对照：`LivingEntity`、`Entity`、`ServerLevel`、`EntityType`、`CompoundTag` **有**租约。**所以不是设计上不管 mixin，是这张 16 类名单没跟上 mixin 的实际分布。**

### 2.5 加载单点与自校验缺失

- `ronova.mixins.json`：`"required": true` + `"injectors": { "defaultRequire": 1 }` —— **任何一个注入点失配，整个 mod 不加载**。这是可用性打击（DoS），不是逻辑绕过。
- `coremod/integrity` 包 240 行内，**零处** mixin 自校验（检索 `MixinEnvironment` / refmap / mixin integrity 均 0 命中）。**被干扰了不会有信号。**

### 2.6 残余边界（逻辑推理，未实测）

`:679` 的 `initial_changes == g_semantic_outside_changes` 只能捕捉 **Ronova 自己的 hook 观察到**的改动。若对手的 agent hook **排在 Ronova 之后**，它改完的字节才是最终定义的字节，而 Ronova 的 hook 已经跑过、看不到——此时租约可能照发。

**这取决于 agent 挂载顺序，静态代码无法判定。** 但它是这套设计的结构性边界：**检测能力 = 观察位置。**

---

## 3. B 块：决定侧未封印

封印名单（`native/build_code_seals.py:11-123`，111 个前缀 → `manifest.json` 载明 **283 个类**）里，`policy` 包**只有两个**：

```
dev/ronova/coremod/policy/RecoveryChainPolicy      已封印
dev/ronova/coremod/policy/TerminalActionPolicy     已封印
```

**天花板计算链上一个都没封：**

| 类 | 角色 | 状态 |
|---|---|---|
| `RuntimeCapabilityProjection` | **唯一的全局 ceiling 生产者**（26 行，只覆盖 6 键） | **未封印** |
| `StrengthPolicySnapshot` | **`effective` 在这里算出**（`:230-233`） | **未封印** |
| `ProfilePresetMatrix` | `EXTREME = STRICT_ALL` 在这里定义（`:147-148`） | **未封印** |
| `MechanismLevel` | 档位枚举 + `lowerOf` | **未封印** |
| `StrengthOptionKey` / `AttackActionLevel` / `AttackAction` / `AttackResolution` / `PolicyState` / `StrengthPolicyConfiguration` / `StrengthPolicyNbtCodec` / `StrengthProfile` | 整条链 | **未封印** |

**消费策略的两个类封了，生产策略的每一个类都没封。**

**后果（代码路径推理，未实机验证）：** 对手 `RetransformClasses` 进 `StrengthPolicySnapshot` 或 `RuntimeCapabilityProjection`，把 `supportedCeiling` 压成 `OFF`，则

`resolve:235-238` → `activationState() = UNAVAILABLE` → `resolveAttack:205-207` 对每个 required key 直接返回 `permitted = false`

**→ 全部 54 键作废，所有攻击动作被拒。一个封印类都没碰，封印机制不会还原（不在 `CODE_IMAGES` 里），也没有信号。且它是静默的：`UNAVAILABLE` 本来就是"平台不支持"的合法状态。**

### 附：天花板实证（供交叉核对）

穷尽递归检索全树：

- `withCapability` 真实授予站点**只有两个**：`RuntimeCapabilityProjection.java:13` 与 `:20`（全局，各 3 键）、`TerminalActionPolicy.java:40`（本地，5 键）——**最高都只给 `PROTECT_RONOVA`（rank 2）**
- `withCapability\([^)]*(RELATED_PATHS|STRICT_ALL)` → **零命中**

> **∴ `supportedCeiling` 对每个键、每条路径永远 ≤ rank 2；`effective = min(requested, ceiling, evidence) ≤ 2`，永远。**
> **∴ rank 3（`RELATED_PATHS`）与 rank 4（`STRICT_ALL`）从未被授予为天花板。**

而极限档请求的是 `filled(MechanismLevel.STRICT_ALL)`（`ProfilePresetMatrix:148`）——**54 键全 rank 4**。

同一批 8 个 `RecoveryChainPolicy.Step` 有两套并行路径：

| 路径 | 读的值 | 上限 | 生产调用点 |
|---|---|---|---|
| 全局发布 `effective`/`supportedCeiling` | `min(requested, ceiling, evidence)` | **2** | 诊断与 UI |
| **A** 通用 `resolve()`（`:48-59`） | `effectiveDepth`（受 ceiling 约束） | **2** → `executes` **恒 false** | **8** |
| **B** 四个 `registered*` 入口（`:75`/`:94`/`:107`/`:110`） | `requested`（**绕过 ceiling**） | **4** | **13** |

**执行到 rank 4，发布与通用路径停在 rank 2。**

### 附：一处绑定验证不一致

路径 B 的闸是 `observes && !pendingRestart && bindingVerified`。13 个调用点里 12 个传字面量 `true`——**其中 11 个是正当的**：四个兄弟审批路径都在方法开头检查了 `!bindingVerified`（`:789`、`:963`、`:998`、`:1027`），之后传 `true` 是已确立事实。

**但 `approveRecoverySourcePlan`（`:770-784`）是例外：它没有 `bindingVerified` 参数，也没有任何绑定检查，却在 `:779`/`:781` 传字面量 `true`。** 它的两条调用路径 `qualifyRecoverySource`（`:1153-1177`）与 `recoverySourceHeld`（`:1195-1199`）**全程没有绑定验证**，剩下的闸只有 authority 连续性、`evidence.fresh()`、policy revision 溯源（`:1164`）、effect journal 状态。

而 `:783` 据此决定拷贝深度上限：

```java
derived.executionPermitted() ? Integer.MAX_VALUE : 1
```

**即：极限档下，一条没有绑定验证的路径可以给出无上限拷贝深度的 recovery source plan。**

`:769` 的注释说它是"从**已提交**的 terminal command 派生出的**只读**条件许可"——也许在已提交前提下绑定验证确实不必要。**但五个审批路径里四个要求、一个自己断言，这个不一致需要有人确认是有意的。**

---

## 4. C 块：API 表面敞开

### 4.1 三个前提全部成立

```
module-info.java        无        → 没有 JPMS 封装
SecurityManager         0 命中    → JDK 17 已废弃
AccessController        0 命中
doPrivileged            0 命中
ReflectPermission       0 命中
```

**关键区别：调用一个 public 方法不是修改字节码。封印对它是零覆盖。**

### 4.2 入口

`ServerRuntimeRegistry`（`bootstrap` 包，`public final`）——`runtime(...)` 有 **71 个内部调用点**，是通用入口：

```java
:22   public static DominionServerRuntime runtime(MinecraftServer server)
```

返回的 `DominionServerRuntime`（293 行）继续暴露：

| 行 | 方法 | 暴露的东西 |
|---|---|---|
| `:156` | `commands()` | **`RootCommandBus`** |
| `:161` | `terminalPlans()` | `TerminalPlanExecutor` |
| `:200` | `durableStore()` | **`LocalDominionStore`** |
| `:219` | `integrateLifecycleOnce(uuid, id, body)` | **在生命期整合下跑调用方的 `Runnable`** |
| `:213` | `claimLifecycleIntegration(uuid, id)` | 认领生命期整合 |
| `:183` / `:189` / `:187` | `authorityState()` / `spawnAdmission()` / `authorityEnforcement()` | 权威运行时 |
| `:167` / `:163` | `authorityView()` / `recoverySources()` | — |

（`AuthorityBinder:76`/`:88` 已在用 `.authorityState().releaseObject(player)` / `.clearProjection(player)`，说明这些 accessor 是常规调用面。）

### 4.3 零鉴权的破坏性方法

| 方法 | 位置 | 实现 |
|---|---|---|
| `invalidatePasses()` | `SpawnAdmissionRuntime:334-337` | `passed.clear(); passes.clear();` —— **清空全部准入，无任何前置检查** |
| `clearIdentity(UUID)` | `SpawnAdmissionRuntime:449-460` | `classLineages.forget(uuid); … terminalWatch.clearIdentity(uuid);` —— **针对性摘掉某主体的血缘与终结观察** |
| `release(MinecraftServer)` | `ServerRuntimeRegistry:136-174` | 17 步清理，含 `authorityState().release()`、`terminalWatch().clear()`、`ProjectionCoordinator.release()`、`LocalDominionStore.release()` —— **整个拆掉权威** |

三个都是 `public`，都无鉴权。任何一个同 JVM 的 mod 拿到 `MinecraftServer` 引用（每个 mod 都有）即可调用。

**封印一个字节都不会动，`CODE_IMAGES` 里那些类完好无损。**

---

## 5. D 块：落盘状态无密钥

`DurableRootFileBackend`：

```
:27   /** World-local, checksummed and atomically replaced image for the 7.16 Root. */
:35   private static final int DIGEST_BYTES = 32;
:36   private static final int HEADER_BYTES = Integer.BYTES * 3 + DIGEST_BYTES;
:105  throw new DurableRootPersistenceException("durable Root magic mismatch");
:118  byte[] expectedDigest = new byte[DIGEST_BYTES];
:119  input.readFully(expectedDigest);
:122  if (!MessageDigest.isEqual(expectedDigest, digest(payload))) { … }
```

32 字节 SHA-256 + magic header + 原子替换 + 常量时间比较（`MessageDigest.isEqual`）——**工程上是干净的**。

**但没有密钥：**

```
Hmac              0 命中      Mac.getInstance    0
SecretKey         0           SecretKeySpec      0
javax.crypto      0           Signature          0
salt / Salt       0
MessageDigest     37          ← 只有它
```

原生侧链接的 `-lbcrypt` **只用于 SHA-256**：`storage_transaction.hpp:89` `BCryptOpenAlgorithmProvider(&algorithm, BCRYPT_SHA256_ALGORITHM, nullptr, 0)`。**不是 HMAC。**

**∴ 这套校验防意外损坏，不防蓄意伪造。** 同权限对手能写存档目录，就能重算 SHA-256，产出一份哈希一致的伪造镜像。

**天花板提醒：** 即使改成 HMAC，**若密钥也在同 JVM 内派生，对手能读代码就能读密钥派生**。这条只能提高门槛，不落在"防住"那一侧——与 `L6-INV-10` 的边界一致。

---

## 6. 共同的形状

四块缺口是**同一个形状的三次重复**：

| 投入巨大的一侧 | 没跟上的一侧 |
|---|---|
| 封印 283 个自有类 | **决定侧（policy 生产者）不在名单** |
| 语义租约 16 个 MC 类 | **持久化线（5/6 `@Redirect` 的目标）不在名单** |
| 字节码完整性做到极致 | **API 表面零最小化** |
| 落盘校验做得很干净 | **没有密钥** |

**每一次都是"名单/防护没跟上实际依赖分布"，而不是机制本身有缺陷。** 机制的质量很高；覆盖面的选取落后于依赖图。

---

## 7. 加固方向（按可做性排序）

| # | 动作 | 位置 | 备注 |
|---|---|---|---|
| **1** | 收 API 表面：`ServerRuntimeRegistry` 与 `DominionServerRuntime` 的 accessor 收成 package-private，只留一个带鉴权的门面 | `bootstrap` / `runtime` | **收益最大**；71 个调用点里绝大多数是包内调用 |
| **2** | 给三个破坏性方法加前置授权 | `SpawnAdmissionRuntime:334`/`:449`、`ServerRuntimeRegistry:136` | `RootExecutionFence.Lease` 是现成模式 |
| **3** | 把决定侧加进封印 `PREFIXES` | `native/build_code_seals.py:11-123` | 类很小，**离 512 类 / 4 MiB 上限余量很大**（现 283 类） |
| **4** | 持久化线补进 `SEMANTIC_TARGETS` | `ronova_native.cpp:304-321` | `ListTag` / `NbtIo` / `DimensionDataStorage` / `SavedData`。**位掩码有余量**：`LONG` 32 位，现用 `0x01`–`0x8000`（16 位），**还剩 15 位** |
| **5** | 网络/服务端补租约 | 同上 | `Connection` / `ServerGamePacketListenerImpl` / `MinecraftServer` |
| **6** | 客户端三个至少**登记为未保护** | 文档 | `Minecraft` / `MouseHandler` / `ItemRenderer` 不在租约里，别在文档里当成被保护的 |
| **7** | 落盘改 HMAC | `storage_transaction.hpp` + `DurableRootFileBackend` | 只能提高门槛，**有天花板** |
| **8** | 补 mixin 自校验 | `coremod/integrity` | 现在被干扰了没有信号 |

**第 1–5 条是真正可做的。第 7 条要清楚它的边界。**

---

## 8. 边界与口径

- **L0 三条铁律与 UX1 不变。** 本文只读源码，不修改任何依赖/Mod/Forge 文件、不针对、不删除对方文件、不起外部进程。
- 不新增 `SubjectKey` / Root / 平行 D*·INV*·A*·R*·ID*·T* 编号。
- **头文件/字段存在 ≠ 运行时可用；字面命中数 ≠ 实现状态；存在接线 ≠ 触达。**
- 本文的"后果"段落里，凡标**代码路径推理**的均**未实机验证**；事实段落均为静态读取 + 穷尽检索。
- **穷尽检索的边界：** "没有站点授予 rank 3/4" 由递归检索全树得出，覆盖 `src/main/java/dev/ronova` 全部深度。若 ceiling 由运行期字符串/反射构造（未发现此类路径），该结论需重核。
- **"检测能力 = 观察位置"** 一条取决于 agent 挂载顺序，本文未能判定，只标为结构性边界。
- **未做：** 未编译、未启动 JVM/游戏、未走任何实机攻击路径、未改 `validation/`、未回填 PASS。
