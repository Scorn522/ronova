# 7.14A P99 性能剖析

> 历史归档（2026-09-08 整理）：保留当时的设计、实现和验证口径，不代表当前源码状态。文中的“当前”“下一步”属于记录当时。
> 当前入口：[进度](../../../CURRENT-PROGRESS.md) · [有效规划](../../../plans/README.md)。

本工具只附加外部诊断，不修改候选 JAR，也不改变冻结的 76-action workload。

## 一次运行生成的证据

- `minecraft-profile.jfr`：Java Flight Recorder，`profile` 设置。
- `process-samples.csv`：每 250 ms 采集 Minecraft Java 进程 CPU、内存、线程和句柄。
- `jfr-summary.txt`：JFR 事件计数摘要。
- `jfr-events.json`：GC pause、CPU load、线程阻塞与执行采样。
- `profile-manifest.json`：进程、时间锚点、停止原因和文件 SHA-256。
- 原有 `workload-transcript.json` 与 `BASELINE-TICKS.csv`：仍由既有验收工具生成。

采集器在新的 integrated-server 启动日志出现后动态执行 `JFR.start`，在新的
`WINDOW_COMPLETE` 出现后执行 `JFR.stop`。因此 JFR 与 60 秒 warmup、300 秒
measurement 使用同一次服务器启动作为锚点。

## 调用

```powershell
pwsh -NoProfile -File .\validation\7.14a\Invoke-ProfiledPerformanceWorkload.ps1 `
  -Stage legacy `
  -EvidenceRoot <prepared-evidence-root>
```

先用 `-PlanOnly` 验证工具、路径和冻结 workload，不会启动或修改 Minecraft。

## 解释边界

JFR 与外部采样会产生少量观测开销，因此本轮用于定位 P99 驱动因素，不直接
替代无 profiler 的最终性能 gate。若定位并修复原因，最终仍需用原验收模式重跑。
