# 若娜瓦（Ronova）

> **旧版仓库同步：2026-10-08。** 本仓库对应普通 Ronova，来自本地旧版工程源码快照，与 [Ronova Pro](https://github.com/Scorn522/Ronova_pro) 分开维护。版本配置为 `0.2.0-alpha.7.17-r66`；当前开发范围与未完成事项见 [当前进度](docs/CURRENT-PROGRESS.md)。本次只同步源码、资源、Native 与构建文件，没有新构建、实机验证或发布可用性结论。下文已有检查结果保留其各自历史版本范围。

游戏存档、运行日志、缓存、第三方研究样本与历史分发压缩包留在本地，不作为仓库源码提交。

**fix12 · 当前验收分工：** [新增接线正式口径](docs/plans/WIRING-ACCEPTANCE.md#decision) · [八方向/批次绑定](docs/plans/WIRING-ACCEPTANCE.md#direction-binding) · [原68项范围](docs/plans/VALIDATION-TRACEABILITY.md#acceptance-scope)。采用方案③，原19行判据不删不改，原台账不回填；旧轮记录仅对其各自版本有效。

**fix11 · 当前入口：** [已有实现补遗回执](docs/plans/RESTRUCTURE-REVIEW.md#review-fix11-baseline) · [八方向复用接缝](docs/plans/R66-CAPABILITY-WIRING-PLAN.md#eight-existing-baseline) · [源码消费者依据](docs/reference/R66-EIGHT-DIRECTIONS-EXISTING-BASELINE-EVIDENCE.md#findings)。只补基线，不改fix10决策/批次、源码或验收。下列旧轮“当前”按其版本阅读。

**fix10 · 当前融合入口：** [八方向核验回执](docs/plans/RESTRUCTURE-REVIEW.md#review-fix10-eight) · [按职责融合的规划](docs/plans/R66-CAPABILITY-WIRING-PLAN.md#eight-directions) · [源码补读](docs/reference/R66-EIGHT-DIRECTIONS-REVIEW-EVIDENCE.md#findings)。S2与C13并行、A4前移；复用现有机制，不改源码或验收状态。下列fix9及更早记录只描述其当轮范围。

**fix9 · 历史回执入口：** [Windows 原生复跑闭合](docs/plans/RESTRUCTURE-REVIEW.md#review-fix9-windows) · [原始回执](docs/reference/R66-FIX8-WINDOWS-RERUN-RECEIPT.md) · [fix8 核验/天花板融合](docs/plans/R66-CAPABILITY-WIRING-PLAN.md#verification-ceiling)。本轮只闭合文档工具的 Windows 现场待确认项，生产源码和验收状态未改。

> **fix8 记录：** [核验/天花板融合](docs/plans/R66-CAPABILITY-WIRING-PLAN.md#verification-ceiling) · [回执与检查器修复](docs/plans/RESTRUCTURE-REVIEW.md#review-fix8-verification)。

> **r66 文档 fix7：** [同权限清点处理回执](docs/plans/RESTRUCTURE-REVIEW.md#review-fix7-surface)已融入[现有接线规划](docs/plans/R66-CAPABILITY-WIRING-PLAN.md#same-privilege-surface)；原八条加固接回原批次，不新增方向或验收状态。未改源码。

> **前轮 fix6：** [V1.3九项回执](docs/plans/RESTRUCTURE-REVIEW.md#review-fix6-v13)与[融合章节/批次](docs/plans/R66-CAPABILITY-WIRING-PLAN.md#v13-receipt)已更新；接受3项、修改5项、废弃1项本轮任务。保留原文与历史状态，未改源码。

> **前轮 fix5 记录：** [能力接线与 JVMTI 增补规划](docs/plans/R66-CAPABILITY-WIRING-PLAN.md#native-negotiation)已融入工作区增补包；[逐节融合与版本处置](docs/plans/RESTRUCTURE-MAP.md#fix5-addendum-merge)。先接通现有能力/事件/回退与诊断，再补执行和扩展；仅文档更新，下方原开发状态未改。

Minecraft Forge 1.20.1 Mod，Java 17。构建使用 Forge 47.4.22，
既有客户端验证环境为 47.4.23。客户端与服务端需要匹配的 Ronova 网络协议。

## 当前开发

**7.17 正在集成施工，尚未宣布完整完成。**
版本配置仍保留 r66，最新源码已推进到协议 14；
最新 `0.2.0-alpha.7.17-validation-12` JAR 已完成构建，85 个 Java 自测通过；封印选择器的 8 项检查已在前轮通过。
修复 tick 备用入口的重复维护和提前关闭观察；普通末尾回调的 copy→换 UUID→工厂拦截通过实机，无关 NBT 正常通过。
静态 Root 历史提示已在真实复制入口接续，准确对象绑定、错误快照/伪造 observer 拒绝和重启复验通过。
已加载 SavedData 的 Root 历史位置重查已接入生产维护，并通过真实保存重启后的新实例绑定验证。
登记终结状态修复、后台重复漂移恢复和完成边界已通过隔离专用服及重启复验。
第三方恢复任务、外部保存与游戏内整链验收仍未完成；旧 `integration-candidate` JAR 不代表最新源码。

- [当前进度](docs/CURRENT-PROGRESS.md)
- [全部有效规划](docs/plans/README.md)
- [不变量全集与编号索引](docs/plans/INVARIANT-INDEX.md) · [MS 重构审阅修订](docs/plans/RESTRUCTURE-REVIEW.md)
- [规划条款 ↔ B/X 验收规格对照](docs/plans/VALIDATION-TRACEABILITY.md)（设计映射；台账 ID/汇总据用户回报已核，运行证据待绑定）
- [重构记录入口：当前与历史](docs/plans/restructure/README.md)
- [通用恢复链处置规划](docs/plans/RESTRUCTURE-MAP.md#source-rec)
- [文档总入口](docs/README.md) · [历史归档](docs/archive/README.md)

## 操作与设计

- 左键：当前维度 64 格单目标射线；保留方块遮挡。
- 右键：按住格挡，松开后提交 64 格范围裁决。
- Shift＋右键：开始格挡时锁定该模式，松开后提交当前维度已加载目标裁决。
- `O`：运行时与强度设置入口。
- `/ronova clear_uuid <UUID>`、`/ronova clear_tombstones`：解除相应裁决，
  不会自动销毁第三方保存的恢复源；通用恢复链处置是正在规划的新功能。

Authority 与 durable Root 始终在当前 Minecraft JVM 内。
内部 Raw Observation 与对外 Presence 分开；物理对象消失不等于持久裁决消失。
生产逻辑不按第三方 Boss、Mod ID、类名或包名编写特判。

剑体和 Tooltip 使用 GLSL 着色器；Java 负责 Forge 渲染接入、参数与交互。
后续视觉效果继续优先使用着色器。

## 构建

在项目根目录、Java 17 环境执行：

```powershell
.\gradlew.bat build --no-daemon
```

开发集成候选可显式覆盖版本：

```powershell
.\gradlew.bat build '-Pmod_version=0.2.0-alpha.7.17-integration-candidate' --no-daemon
```

输出在 `build/libs/`。字体默认从 Adobe 官方固定版本获取；
离线时把 `SourceHanSerifCN-SemiBold.otf` 放到 `local-fonts/` 并传入 `-PronovaFontMode=local`。
构建和运行验证安排在当前大阶段完成后，历史记录不代替最新源码验证。
