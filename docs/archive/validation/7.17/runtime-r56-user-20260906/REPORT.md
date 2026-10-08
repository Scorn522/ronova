# r56 用户实机日志对照（2026-09-06）

> 历史归档（2026-09-08 整理）：保留当时的设计、实现和验证口径，不代表当前源码状态。文中的“当前”“下一步”属于记录当时。
> 当前入口：[进度](../../../../CURRENT-PROGRESS.md) · [有效规划](../../../../plans/README.md)。

来源：用户提供的 Forge 47.4.23 / Minecraft 1.20.1 实例日志，运行时间约 21:59–22:01。原日志编码为 GB18030，归档保留原字节；哈希见 manifest.json。

## 已观察到

- 实例里的 Ronova JAR 为 r56，SHA-256 与本工作区成品相同：`0b5787e2d880e3a32b8b5febfc96ec621b4e5be4fa94d4ddacc3d6eef8024d02`。
- Java 为 Microsoft 17.0.15；Ronova 初始化报告 `native: ACTIVE/0x7f`、`NATIVE_ACTIVE`。这只证明该初始化状态，不是 CFG/全部 loader 的完整证明。
- 协议 9 双向握手通过；日志显示 Owner 绑定、一次 AREA 的 `candidates=1 judged=1 marks=1`，随后若干输入和 TerminalWatch 活动。
- 正常保存全部维度并退出，未看到 Ronova 异常或崩溃栈。不能仅据 entity 数量或 watch_completed 认定跨表面 CLEARED。

## 明确报错与警告

- 唯一 ERROR 来自 `fading_black_sun:rewards/sun_slayer` 函数引用不存在的 `generated_mod:creation_myth_disc`。没有修改第三方模组或世界数据。
- Ronova 的 `GbufferProjMat` 声明未参与 shader 计算，被编译器移除后产生 uniform 警告。r57 删除未使用的 GLSL/JSON 声明和 Java 设置调用，视觉复测仍需新一轮游戏日志。
- 纹理 mip-level 警告、原版 shader/sound 警告与 Netty debug 反射探测不等于 Ronova 崩溃。

## 存储只读检查

归档两份 Ronova Root 文件，通过 r57 的开发输出加载器只读检查：

| 文件 | 原 schema / journal | epoch | owners / tombstones / legacy claims |
|---|---|---|---|
| world-1-root.bin | 3 / 2 | 1:4 | 1 / 1 / 1 |
| world-2-root.bin | 4 / 3 | 1:2 | 1 / 1 / 1 |

两份均通过校验，加载到 r57 时需要格式升级；没有回写原世界或归档文件。第二份源路径是日志保存的 `新的世界 (1)`，第一份为实例中另一个旧世界；不把它们都说成本次运行新建的数据。

此为 r56 的 BASIC_USER_SMOKE_OBSERVED 和旧 Root 离线读取证据；不替代 r57 游戏内复测、完整世界升级/重启、第三方实际副作用、多人、Native 语义证明、GPU 或长时间压力验收。
