# MS 领域知识简报（豆包之刃专用版）

来源：改编自 `.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/`（Apache-2.0）。
改编目的：把它变成一份**给编码 AI 读的施工参考**。

## 这份文档改了什么

| 处理 | 内容 | 原因 |
|---|---|---|
| **保留** | MS 定义、能力边界、三种执行模式、扇形选取、线程与生命周期、防御链五层、故障定位表 | 纯 Java 能做到，是这个 mod 的核心 |
| **砍掉** | 自定义转换器/Mixin 注入、重转换与重定义、JNA/JNI/JVMTI Native 层、启动顺序、Native 打包 | 本项目明确不走这条路。留着只会诱导 AI 去写用不上的东西 |
| **改写** | 完成标准 | 原文"带 Native 的秒杀 Mod"标准不适用；改成纯 Java 的分档标准 |
| **新增** | API 名称对照表、纯 Java 的能力边界说明 | 原文档**不给具体 API 签名**，这是 AI 最容易臆造的地方 |

**给 AI 的读法**：第 1–7 节是设计知识，第 8 节是防臆造导航，第 9 节是故障定位。
先读第 8 节，再读别的。

---

## 1. MS 是什么

我的世界秒杀（简称 MS），是 Minecraft 的一个小众圈子。
**它现在的定义不是"把伤害数值改大"**，而是一条**可配置的实体状态转换链**。

十几年前随便 `setHealth(0)` 就算秒杀。现在一个合格的 MS Mod 至少要：

1. 把能力建模成明确的状态转换，而不是单次伤害
2. 逻辑服务端独占目标选取、世界状态、战利品与最终结算
3. 正常死亡与运行时清除**分开实现**
4. 有独立开关、目标过滤与故障日志
5. 有可验证的实机证据

### 圈内硬规矩（违反即不算 MS Mod）

1. **不修改文件** —— 包括依赖文件、其他 Mod 的 JAR、游戏本体
2. **不针对** —— 不用反射、MethodHandle、VarHandle 或对方的类去打特定第三方实现；
   不按别人的 modid、类名、包名写特判
3. **不删除**他人 Mod 文件

---

## 2. 三种执行模式（核心概念，必须先分清）

### 模式一：标准死亡

适合"伤害再高也应该视为战斗"的目标，保留 Minecraft 生态行为。

- 走 `LivingEntity.hurt` / `kill` 或实体自身公开死亡 API
- 使用来源清楚的 `DamageSource`，以便兼容战利品、统计、进度、死亡事件和归因
- 让 Forge 的伤害/死亡事件保持**可观察、可取消**
- **不要**在同一目标上既手动掉战利品又触发正常死亡 —— 会重复掉落
- 对凋灵、末影龙、模组 Boss、多部位实体要单独测试奖励、阶段和部位注册表

### 模式二：实体清除

目标是"从当前世界运行态中抹除实体"，而不是普通死亡。
**这是高耦合机制**，必须有独立开关、目标过滤和故障日志。

按顺序执行：

1. 选择并复制**稳定快照**；不要在迭代实体集合时修改该集合
2. 过滤 `Player`、受保护实体、无效实体和不允许的 `ItemEntity`
3. 在逻辑服务器线程将目标放入**串行队列**
4. 按策略在清除前生成额外掉落、播放死亡音效或记录审计信息
5. **先**把实体标为 `ERASED`，让后续注入立即返回一致状态
6. 调用实体移除生命周期，停止其被玩家追踪
7. 清理服务端：可见查询、按 ID/UUID 索引、tick 列表、ChunkMap 追踪记录、section 存储
8. 清理客户端：实体存储、渲染实体集合、客户端 tick 列表、部位实体表
9. 多部位实体移除全部 `PartEntity` 引用；末影龙等有专用战斗控制器的更新 Boss 战状态
10. 把位置、速度、回调和可更新标志固定为已清除状态，**阻止后续回调把实体重新挂回世界**

> **最关键的一句话**：不要以为调用了 `remove` 就是完整清除。
> 1.20.1 的实体可能同时存在于实体查询、tick 集合、分区存储、网络追踪和客户端渲染结构里。
> 清除链必须覆盖**所有会再次观察到该实体的索引**。

### 模式三：混合

常用策略：普通生物走正常死亡，对特定异常实体、重复生成物或允许的掉落物走清除链。
写成**单独的 `KillPolicy`**，不要在每个触发回调里散落 `instanceof`。

```java
KillMode chooseMode(Entity target, KillContext context) {
    if (target instanceof Player || protection.isProtected(target)) {
        return KillMode.IGNORE;
    }
    if (target instanceof ItemEntity && !context.eraseItems()) {
        return KillMode.IGNORE;
    }
    if (context.mustErase(target)) {
        return KillMode.ERASE;
    }
    return KillMode.NORMAL_DEATH;
}
```

---

## 3. 能力边界：七维表

写任何 Item、事件或注入之前，先把这七项写成表：

| 维度 | 必须明确 |
|---|---|
| 触发 | 左键 / `hurtEnemy` / 蓄力释放 / 挥动 / 按键包 / 命令 / 定时技能 |
| 目标 | 是否排除玩家、施法者、已清除实体、受保护实体、掉落物、盔甲架、宠物、Boss、多部位实体 |
| 范围 | 单体 / 球形 / 扇形 / 射线 / 全维度；给出半径、角度、长度和**上限** |
| 结果 | 正常死亡 / 直接清除 / 仅客户端视觉清除 / 保留掉落 / 复制掉落 / 禁止重生 |
| 防御 | 谁可进入受保护状态；由持有物品、实体标签、白名单还是能力模块决定 |
| 同步 | 服务端与客户端各自做什么；是否需要显式网络包、追踪回调、重载区补偿 |
| 开关 | 高风险行为放在模块/配置开关后，并给出默认目标范围与掉落策略 |

**不许把"玩家不可杀"和"任意实体不可杀"写成同一个条件。**
玩家、普通生物、物品实体、多部位 Boss 的清理与恢复路径各不相同。

---

## 4. 扇形与范围选取

用**服务端朝向 + 余弦比较**，不要依赖客户端 hit result。

```text
maxDistanceSqr = length * length
look = (-sin(yaw), cos(yaw))
offset = (target.x - actor.x, target.z - actor.z)
directionDot = dot(normalize(offset), look)
target 在扇形内，当 directionDot >= cos(halfAngle)
```

实现要点：

- 用**距离平方**，避免每个目标调用 `sqrt`
- `halfAngle >= 180` 时直接视为全方向
- 水平距离为零时单独处理，避免归一化除零
- 限定目标必须在同一 `Level`，排除施法者自身
- 候选结果先收集到 `Set` / `List` **快照**，再进入执行队列
- 明确"半角"还是"总开角"；不要让配置字段名 `radius` 同时表示角度和距离
- **客户端扫描只服务视觉一致性；服务器扫描才决定世界状态**

---

## 5. 线程、逻辑边与生命周期

### 逻辑服务端

会改变服务器实体管理器、掉落、追踪、战斗状态或区块数据的动作，
必须提交到 `MinecraftServer.execute` 或等价主线程调度器。
**不要**从渲染线程、网络线程、异步线程直接修改 `ServerLevel` 内部集合。

串行消费清除任务：

```java
enqueue(target, mode);
server.execute(() -> {
    while ((task = queue.poll()) != null) {
        executor.apply(task);
    }
});
```

真正执行前**再次检查**实体仍在目标世界、未被清除、未受保护 ——
目标从快照到执行之间可能已死亡、切维度或被卸载。

### 客户端

客户端负责立即可见的渲染、局部音效、镜头效果和客户端实体索引一致性。
**不要让仅客户端清除成为唯一的游戏规则来源。**

- 客户端专属类、`Minecraft.getInstance()`、渲染器、UI 放在独立类或明确的 `Dist.CLIENT` 边界
- 不要让专用服务器的类加载触及客户端类型
- 客户端清除逻辑要容忍实体已不存在、世界已切换、当前维度不一致

### 追踪与重载

实体会在 `onTrackingStart`、`onTrackingEnd`、`onTickingStart`、`onTickingEnd`、
`onDestroyed` 等回调中重新进入或离开客户端可见范围。为两种状态都注入规则：

- `PROTECTED`：阻止会结束 ticking、结束追踪或销毁实体的回调
- `ERASED`：阻止会开始 ticking 或开始追踪的回调

**不要用单一 `isRemoved()` 注入代替追踪层治理。**

---

## 6. 状态语义

| 状态 | 语义 | 主要效果 |
|---|---|---|
| `PROTECTED` | 实体必须留在世界且持续可用 | 取消伤害/死亡/移除，修复生命和同步数据，保留追踪与 ticking |
| `ERASED` | 实体必须从逻辑、追踪、渲染和后续查询中消失 | 停止 tick/追踪，标记 removed/invisible/dead，清理实体索引和多部位引用 |

状态键用 **UUID 或实体 ID + 维度**，并在实体卸载、维度切换、世界关闭时清理过期条目。
**不要只依赖 Java 对象引用长期保存状态。**

---

## 7. 防御链五层

把防御实现成五层，而不是只取消一个 Forge 事件。

### 第 1 层：统一分类

提供唯一的 `ProtectionPolicy.isProtected(Object)` 入口，识别：

- 持有指定物品的玩家、生物或盔甲架
- 指定的自定义实体
- 被保护的物品实体
- 持久化白名单中的玩家

白名单存游戏目录下的 JSON，加载失败回退空集合；
写入时创建父目录并用 UTF-8。把"当前持有物品"和"永久白名单"设计成可分别开关的两种来源。

### 第 2 层：Forge 事件

在 `LivingAttackEvent`、`LivingHurtEvent`、`LivingDeathEvent` 中取消受保护实体的事件。

### 第 3 层：实体与生物方法

用注入覆盖下列入口，保证来自其他 Mod 的直接调用也无法绕过防御：

| 类别 | 受保护实体应表现为 | 已清除实体应表现为 |
|---|---|---|
| `getHealth` / `getMaxHealth` | 固定有效生命值 | 0 |
| `hurt` | 返回 `false` | 按策略拒绝后续操作 |
| `isAlive` / `isDeadOrDying` | 活着且未死亡 | 不活着/死亡中 |
| `die` / `tickDeath` | 取消 | 不再执行普通 tick |
| `remove` / `setRemoved` / 世界移除 | 取消 | 保持已移除 |
| `isInvisible` / `shouldRender` | 可见、可渲染 | 不可见、不可渲染 |
| 移动、推挤、速度、位置写入 | 按设计拦截或修复 | 冻结到失效位置，速度归零 |

只在确实需要时取消方法。**有返回值的方法必须显式写入回调返回值；`void` 方法设置返回值没有意义。**

### 第 4 层：`SynchedEntityData`

**只拦截伤害方法不足以防止网络同步把客户端血量写成 0。**
对 `LivingEntity.DATA_HEALTH_ID`：

- 读取时为受保护实体返回固定生命值
- 写入时取消会把生命值降为非法值的同步更新
- 对箭矢数量、燃烧、姿态等需要稳定显示的数据按功能单独处理

**不要把所有同步字段一律冻结**，否则会破坏正常装备、位置和动画同步。

### 第 5 层：周期修复

在服务器 tick 或实体常规 tick 中重新应用受保护状态：
恢复生命、清空死亡计时、保证实体可更新/已加入世界。
周期修复用于对抗其他 Mod 的延迟修改，**不是替代事件和注入层**。
修复逻辑必须幂等，避免每 tick 反复重建集合、刷网络包或重复注册实体。

---

## 8. API 名称对照（**防臆造导航，不是权威**）

原 MS 文档只给概念名，不给签名。AI 最容易在这里编造 API。
下面这份是**导航用的常见名**，用来帮你缩小查找范围。

> ⚠️ **使用规则（必须遵守）**：
> 1. 这张表**不是签名表**，只给类名/方法名，不给完整 descriptor。
> 2. 每个签名你都必须自己用 IDE 补全、`javap`、反编译产物或官方文档**核对一次**。
> 3. **核对不了的，写进"我不确定的地方"清单**，不要猜一个看起来合理的。
> 4. 表中任何一项与你的实际依赖冲突时，**以依赖为准，这张表作废**。

### 实体与生命值

| 概念 | 1.20.1 官方名（Mojang mappings） |
|---|---|
| 实体基类 | `net.minecraft.world.entity.Entity` |
| 生物基类 | `net.minecraft.world.entity.LivingEntity` |
| 玩家 | `net.minecraft.world.entity.player.Player` |
| 服务端玩家 | `net.minecraft.server.level.ServerPlayer` |
| 物品实体 | `net.minecraft.world.entity.item.ItemEntity` |
| 生命值 | `LivingEntity#getHealth` / `#setHealth` / `#getMaxHealth` |
| 伤害 | `Entity#hurt(DamageSource, float)` |
| 死亡 | `LivingEntity#die(DamageSource)`、`#kill()` |
| 移除 | `Entity#remove(RemovalReason)`、`#setRemoved`、`#isRemoved` |
| 存活判断 | `Entity#isAlive`、`LivingEntity#isDeadOrDying` |
| 同步数据 | `Entity#getEntityData`、`SynchedEntityData` |
| 生命值同步键 | `LivingEntity#DATA_HEALTH_ID` |

### 服务端结构（清除链要碰的地方）

| 概念 | 1.20.1 官方名 |
|---|---|
| 服务端世界 | `net.minecraft.server.level.ServerLevel` |
| 服务端主线程调度 | `MinecraftServer#execute` |
| 实体查询 | `ServerLevel#getEntities`、`#getEntity`、`#getAllEntities` |
| 追踪 | `ChunkMap$TrackedEntity`、`ServerEntity`、`ChunkMap#broadcast*` |
| tick 列表 | `LevelEntityGetter`、`EntityTickList`、`ServerLevel#entityManager` |
| 分区存储 | `EntitySectionStorage`、`EntitySection`、`ClassInstanceMultiMap` |
| 持久实体 | `PersistentEntitySectionManager` |
| 客户端实体存储 | `ClientLevel#getEntities`、客户端 `EntityTickList` |
| 多部位 | `PartEntity`、`EnderDragonPart` |

### Forge 事件（1.20.1 / Forge 47.x）

| 用途 | 事件类（`net.minecraftforge.event.entity.living` 等） |
|---|---|
| 伤害前 | `LivingAttackEvent` |
| 受伤 | `LivingHurtEvent` |
| 实际伤害 | `LivingDamageEvent` |
| 死亡 | `LivingDeathEvent` |
| 实体加入世界 | `EntityJoinLevelEvent` |
| 实体离开世界 | `EntityLeaveLevelEvent` |
| 玩家 tick | `PlayerTickEvent` |
| 服务器 tick | `ServerTickEvent` |

### 注册与工程

| 用途 | 1.20.1 / Forge 47.x |
|---|---|
| 物品注册 | `DeferredRegister<Item>` + `ForgeRegistries.ITEMS` |
| 创造模式标签页 | `CreativeModeTab`、`BuildCreativeModeTabContentsEvent` |
| 事件总线（游戏） | `MinecraftForge.EVENT_BUS` |
| 事件总线（Mod 生命周期） | `FMLJavaModLoadingContext` / Mod 事件总线 |
| Mod 元数据 | `src/main/resources/META-INF/mods.toml` |
| 构建版本 | Forge **47.4.22** |

---

## 9. 故障定位表

| 现象 | 优先检查 |
|---|---|
| 实体只在一个客户端消失 | 是否只执行了客户端清除；服务器追踪和实体管理器是否仍保留对象 |
| 清除后实体又回来 | `ERASED` 状态是否在回调、tick、`unsetRemoved`、追踪开始路径上被重新加入 |
| 受保护目标仍死亡 | 是否只取消了 Forge 事件；检查直接 `hurt`、`die`、`tickDeath` 与同步生命值写入 |
| 客户端仍显示 0 血或死亡界面 | 检查 `SynchedEntityData` 读写拦截以及客户端死亡 UI |
| 末影龙部位残留或 Boss 条不消失 | 检查多部位表、Boss 战控制器和 server/client 两侧的部位索引 |
| 注入没有效果 | 核对目标类是否已加载、方法名、descriptor、注入点位置 |
| 专用服务器崩溃 | 检查 common 代码是否引用了 `Minecraft.getInstance()`、渲染器或其他客户端类 |
| 全维度扫描卡服 | 是否有上限；是否每 tick 无界遍历所有实体 |

---

## 10. 验证矩阵

改完实体状态、注册表、追踪、同步后，至少覆盖：

| 场景 | 必须验证 |
|---|---|
| 单机 | 正常触发、无崩溃、无残留实体 |
| 专用服务器 + 多客户端 | 服务端权威、各客户端观察一致、无幽灵实体 |
| 普通生物 / 玩家 / Boss / 多部位 / 掉落物 / 盔甲架 / 宠物 | 每类分别验证，不能只测僵尸 |
| 边界 | 目标已死亡 / 已卸载 / 重复触发 / 跨维度 / 区块卸载后重载 / 重连 |
| 生命周期 | 被清除后不再 tick、不再被追踪、查询不到、不占索引 |
| 防御 | 受保护实体挡住全部入口；保护解除后恢复正常 |
| 性能 | 全维度扫描有上限；给出 p95 tick 开销数字 |

---

## 11. 完成标准（本项目适用版）

原文档的标准是"带 Native 的秒杀 Mod"，本项目**不走 Native**，所以标准改为：

### 第一档：能打

- 左键能可靠杀死普通原版生物
- 服务端与客户端一致，无幽灵实体
- 专用服务器不崩

### 第二档：打得干净

- 标准死亡与运行时清除是两条独立流程
- 清除后实体不再 tick、不被追踪、查询不到、重载不复活
- 受保护实体挡得住全部入口
- 玩家、宠物、村民在默认配置下不会被误杀

### 第三档：打得稳

- 多部位 Boss（末影龙、凋灵）部位与 Boss 条正确处理
- 重复触发、重连、跨维度、区块重载全部正确
- 有审计日志、有故障定位手段、有性能数字

**本项目目标定在第二档，做到位。** 第三档作为后续目标。
不要因为够不到原生/JVMTI 那一档就降低第二档的标准，也不要硬凑假实现。

---

## 附：原文中被砍掉的部分（留个索引，不做）

如果你后来确实需要那一档，原文有完整记载，但**必须先跟我确认**：

- 自定义注解驱动转换器、`@Overload`/注入声明、转换器注册与顺序
- 重转换与重定义（`RetransformClasses` / `RedefineClasses`）、批量与去重
- JNA/JNI/JVMTI Native 层、命令桥接契约、启动顺序、DLL 打包
- `ImmediateWindowProvider` / `ITransformationService` / Java agent 优先级
- HotSpot 内联实现与 `MethodHandle` 取 VM 方法的做法

原文位置：`.incoming-ms-mod-doc/Minecraft-MS-Mod-Doc-main/base/minecraft-ms-1.20.1-skills.zh.md`
第 273–412 行。**这些内容超出本项目已定路线，不要主动去看、不要据它设计。**
