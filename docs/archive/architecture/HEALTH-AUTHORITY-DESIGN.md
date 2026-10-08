# Health Authority（同级改血）设计冻结草案

> 历史归档（2026-09-08 整理）：保留当时的设计、实现和验证口径，不代表当前源码状态。文中的“当前”“下一步”属于记录当时。
> 当前入口：[进度](../../CURRENT-PROGRESS.md) · [有效规划](../../plans/README.md)。

> 历史设计草案，非当前 canonical 规范。本文保留早期 External Root 等路线讨论；当前 Authority/Truth 路线以 `docs/plans/README.md` 与 `docs/CURRENT-PROGRESS.md` 为准。

## 目标

“同级改血”不是一次 `LivingEntity#setHealth` 调用，而是一条可恢复、可撤销、可同步、可由 Native/External Root 重建的权威状态链。它与 Judgment、Owner Protection 平级，但不得进入 `AuthorityGuard` 或 `JudgmentEngine`。

## 独立领域

建议在 R1/R2 建立：

```text
health-contract     不依赖 Minecraft 的指令、版本和快照
health-core         唯一决策与不可变 Projection
health-forge        服务端线程落地与 Minecraft 事件/Mixin 入口
health-native       L5 完整性检查与入口重断言
ronova-root/health   L6 最终事务真值与跨 JVM 恢复
```

不得把健康状态塞进 `AuthorityGuard`。后者只回答“这个玩家是否已被授权及其防护是否需要恢复”；Health Authority 只回答“这个 Subject 当前应呈现怎样的生命状态”。

## 最小 Contract

```java
record HealthDirective(
    SubjectIdentity subject,
    HealthMode mode,
    double currentHealth,
    double maximumHealth,
    boolean locked,
    Epoch epoch,
    long sequence
) {}
```

`HealthMode` 至少包含：

- `CURRENT`：只修改当前血量，完成一次权威提交后允许正常变化。
- `MAXIMUM`：修改最大血量，并把当前血量裁剪到合法范围。
- `LOCKED`：当前值和最大值成为持续期望状态，外部写入会被拒绝或纠正。
- `CLEAR`：撤销指令，恢复进入 Health Authority 前保存的基线投影。

所有数值必须拒绝 `NaN`、无限值和负数，并在 Contract 层使用固定上限/精度策略；不能让 Forge Adapter 各自决定取整或裁剪。

## Forge 落地入口

服务端线程只消费已经提交的不可变 Projection，并覆盖以下入口：

- `LivingEntity#setHealth`
- `LivingEntity#heal`
- `LivingEntity#hurt`、死亡与复活链
- `AttributeInstance` 最大生命值重算
- `SynchedEntityData` 的健康字段同步
- 玩家 Clone、重生、重登和跨维度对象替换
- NBT 读取完成后的首次投影

入口只查询 Core，不在 Mixin 内写业务规则。变更通过按 Subject 去重的 dirty queue 落地，另加低频分片校验；禁止每 tick 全世界扫描。

## 与现有玩家防护的冲突规则

当前 `AuthorityGuard.restoreVitals` 默认把已授权玩家恢复到最大血量。接入 Health Authority 时必须改为调用统一的 `HealthProjectionResolver`：

1. 同一 Authority generation 签发的 `LOCKED` 指令优先于默认满血恢复。
2. 没有有效 Health 指令时，保留当前 Owner Protection 的满血语义。
3. 过期、低 epoch 或断序指令不得改变玩家防护。
4. Judgment 终态不由 Health Authority 复活；只有先撤销 Judgment，再应用 Health 指令。

## L5 / L6 同级条件

- L5：Native 持有 health policy generation、epoch/sequence/hash，验证关键入口仍被 Hook；Java Projection 被清空后可由 Native 重新投影。
- L6：External Root 原子提交 `SET/CLEAR`，保存前值与审计记录；JVM 无权单方面创建、修改或撤销最终 HealthDirective；重启 JVM 后可恢复。

## 验收门槛

- 当前值、最大值、锁定和撤销四类行为均有 golden/状态机测试。
- 伤害、治疗、属性重算、NBT、Clone、重登、跨维度和直接同步字段写入都不会产生双真值。
- 指令乱序、重复、Root 断连、Native 降级和 JVM 重启均有故障注入。
- 未安装 Health Authority 时，现有 Judgment 与 Owner Protection 行为保持不变。

这份草案只冻结架构边界；具体首个用户入口（改当前值、改最大值或锁定）在开始功能实现前再确定。
