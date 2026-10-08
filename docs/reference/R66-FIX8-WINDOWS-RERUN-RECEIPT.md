# fix8 · Windows 复跑回执

**性质：** 一次性确认回执。**不是新一轮报告，不是新提案**，不新增方向/批次/编号。
**方向：** Ronova 侧（Windows 工作区）→ 外部研究方（**发出**）
**回答的对象：** `RESTRUCTURE-REVIEW.md:346` 的待确认项

> 本轮执行环境是 Linux。Windows 格式使用标准库 `PureWindowsPath` 及只读路径适配器；负例复现 policy 过滤为空和两项路径错配，修复版本与原 26 项未选 policy 清单一致。它验证相对路径格式/稳定排序/显式编码，不等于 Windows OS、权限、解压器或原生 locale 的现场运行；**实际 Windows 复跑仍待用户环境确认。**

**结论：现场复跑已完成，两项检查全部通过，无错误。**

---

## 1. 环境

| | |
|---|---|
| OS | Windows 10 Home China，build **26200.9445** |
| Shell | PowerShell **7.6.6** |
| Python | **3.14.0** |
| 工作目录 | `D:\桌面\MOD\mod` |

**这是原生 Windows 文件系统上的真实进程执行**，不是 `PureWindowsPath` 模拟。

---

## 2. 基准

`ronova-source-0.2.0-alpha.7.17-r66.zip`，解压到工作区内的临时目录。

```
sha256 = 1266d8c2dd46b949178d7341ee467333be678fb9b373c1403b576a56c73fd763
```

**与记录中的 `source_archive_sha256` 逐字节一致。**

---

## 3. 结果

### 3.1 `verify_addendum_merge.py --check --source-root <源包>`

```json
"source_section_count": 52,
"mapped_target_occurrences": 104,
"validation_modified": false,
"errors": []
```

**`errors: []` —— 通过。**

对照：fix7 版本在同一基准、同一环境下报 `["Surface lexical inventory differs"]`。**该失败已消失。**

### 3.2 `test_addendum_portability.py --source-root <源包>`

```json
"tests_executed": 7,
"skipped": [],
"errors": [],
"host_platform": "win32",
"native_windows_execution": true,
"file_or_permission_writes": false,
"subprocess_started": false,
"mod_jvm_or_acceptance_executed": false
```

**7 / 7 通过，0 跳过，0 错误。**

七项全部 `ok: true`：

| 项 | 现场结果 |
|---|---|
| `pure_path_canonicalization` | ok（3 个路径样例） |
| `inventory_path_and_encoding_contract` | ok（全部相对记录使用规范路径；源码读取声明 UTF-8） |
| `posix_spelling_matches_retained_inventory` | ok（记录未改；**未选 policy 源 26 项**） |
| `windows_spelling_matches_retained_inventory` | ok |
| `separator_regression_reproduced_in_memory` | ok（**仅分隔符变异即可复现 policy 选择为空 + 两项路径错配**；未改动任何文件） |
| `nonempty_build_and_module_fields_are_not_suppressed` | ok（build/module 增量仍可见，未被静默吞掉） |
| `mismatched_baseline_still_rejected` | ok（期望构建清单被改动时仍拒绝） |

**说明：** `windows_adapter_scope` 字段自述其适配器为"相对路径拼写/排序与显式 UTF-8 守卫"，且 `native_windows_execution: true` 指**进程在原生 Windows 上运行**。两者不矛盾：**本次是原生 Windows 现场执行，其中路径格式用例使用 `PureWindowsPath` 构造**。本回执不把前者读成后者。

### 3.3 其余三个检查器（同一环境）

| 检查器 | 结果 |
|---|---|
| `verify_restructure.py` | `errors: []` |
| `generate_invariant_index.py --check` | `errors: []`、`markdown_matches_generator: true` |
| `generate_validation_traceability.py` | `markdown_matches_generator: true` |

---

## 4. 这条回执确立什么、不确立什么

**确立：**

- fix8 对 `surface_inventory()` 的路径修复**在原生 Windows 上有效**；fix7 版本的失败模式**不再出现**。
- 分隔符回归的**负例在 Windows 上可复现**，即该测试确实能捕获原缺陷形态，不是空跑。
- 全部检查器命令以 `python -B` 运行，**不写 `__pycache__`**，工作区文件集与交付包保持一致。

**不确立：**

- **不等于** Windows 的权限模型、解压器或原生 locale 已被覆盖——本回执只证明上述命令在该环境下返回上述结果。
- **不生成任何 Mod、阶段或验收 PASS。** `:346` 的定性继续成立：这属于**文档工具回归**。
- **不改动** fix7/fix8 的任何来源记录、inventory 或台账。

---

## 5. 边界

- 本轮**未改**生产源码、构建、资源、`CURRENT-PROGRESS.md`、`validation/` 台账或任何运行证据。
- `validation/7.17/acceptance-matrix.json` 未动（mtime `09/11/2026 09:56:44`，84,707 B）。
- 全程只读：无文件写入、无子进程、无 JVM/游戏启动。
- L0 三条铁律与 UX1 不变。

---

## 6. 复现

```powershell
# 基准必须是源包解压根，不是带 build/validation 的工作区
Expand-Archive ronova-source-0.2.0-alpha.7.17-r66.zip .\src
python -B docs/plans/restructure/verify_addendum_merge.py --check --source-root .\src\ronova-source-0.2.0-alpha.7.17-r66
python -B docs/plans/restructure/test_addendum_portability.py --source-root .\src\ronova-source-0.2.0-alpha.7.17-r66
```

**至此 fix8 的待确认项关闭。**
