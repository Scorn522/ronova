# R66 · `verify_addendum_merge.py` 的 Windows 可移植性缺陷

**性质：** 对 fix7 交付件中 **检查器脚本**的缺陷报告。**不涉及任何源码、规划或结论的改动**。
**方向：** Ronova 侧（Windows 工作区）→ 外部研究方（**发出**）
**对象：** `docs/plans/restructure/verify_addendum_merge.py`（fix7 版，29,962 B）
**未做：** 未修改该脚本（保持交付原字节）；未改源码、构建、`validation/`；未回填任何 PASS。

---

## 1. 结论

**该脚本在 Windows 上必然报 `Surface lexical inventory differs`，与工作区内容是否正确无关。**

根因是 `surface_inventory()` 内部三处路径比较使用了 `str(p.relative_to(...))`，在 Windows 上产生**反斜杠**；而同一文件里其它所有路径比较都使用 `.as_posix()`。

**这是检查器的可移植性缺陷，不是被检查内容的缺陷。**

---

## 2. 缺陷位置

`surface_inventory(source_root)`（脚本 `:133-186`）内：

| 行 | 代码 | 后果 |
|---|---|---|
| `:164` | `'path':str(p.relative_to(S))` | `mixin_rows[].path` 写成 `src\main\java\...` |
| `:170` | `'path':str(p.relative_to(S))` | `runtime_qualified_call_lines[].path` 同上 |
| `:173` | `[p.stem for p in java if str(p.relative_to(S)).startswith(policy_path)]` | **`startswith('src/main/java/...')` 恒为假 → `policy_classes` 为空** |

`:173` 是**唯一会造成逻辑后果**的一处：它不产生路径文本，而是**让过滤条件失效**。

对照——同一文件里正确写法有三处：

```
:143  {str(p.relative_to(S)): ...}                    ← 同缺陷（但仅作 groups 键，另有用 as_posix 的校验兜住）
:273  actual_java = {p.relative_to(sr).as_posix() ...}   ← 正确
:275  require(actual_java == set(ev['groups']['java']))  ← 因此这一项通过
```

**所以同一份语料，`:275` 的校验通过，`:285` 的校验失败——差异只在分隔符写法。**

---

## 3. 证据链（三条独立确认）

### 3.1 路径规范化后两项自动匹配

把重算结果里所有字符串字段的 `\` 归一为 `/` 后：

| 字段 | 规范化前 | 规范化后 |
|---|---|---|
| `mixin_rows` | 不一致 | **一致** |
| `runtime_qualified_call_lines` | 不一致 | **一致** |

→ 这两项**纯粹是分隔符伪差异**，记录用 `/`、重算用 `\`。

### 3.2 `policy_classes` 的 0 vs 28

在同一份源包上直接比较两种写法：

```
policy_classes via str(p.relative_to(S)).startswith(...)      →  0 个
policy_classes via p.relative_to(S).as_posix().startswith(...) → 28 个
```

→ 确认 `:173` 的过滤在 Windows 上恒不命中。

### 3.3 与记录值精确吻合

- 工作区实际有 **28** 个 policy 顶层类
- 其中 **2** 个已在封印前缀里：`RecoveryChainPolicy`、`TerminalActionPolicy`
- 28 − 2 = **26**
- 记录值 `policy_top_level_sources_not_selected` **恰好 26 项**，且逐项与 `as_posix()` 版本一致

→ **记录值本身是对的**，是重算侧在 Windows 上算不出来。

---

## 4. 复现

```powershell
# 基准必须是源包，不是工作区（见 §5）
Expand-Archive ronova-source-0.2.0-alpha.7.17-r66.zip .\src
python docs/plans/restructure/verify_addendum_merge.py --source-root .\src\ronova-source-0.2.0-alpha.7.17-r66
```

输出尾部：

```json
"validation_modified": false,
"errors": ["Surface lexical inventory differs"]
```

**在 POSIX 环境下同一命令不报此错**——因此该缺陷在原作者环境中不可见。

---

## 5. 附：基准选择（我本轮犯过的错，一并记录）

`--source-root` 应指向**源包解压目录**，不是工作区。

- 记录载明 `source_archive_name = ronova-source-0.2.0-alpha.7.17-r66.zip`、`source_archive_sha256 = 1266d8c2dd46b949178d7341ee467333be678fb9b373c1403b576a56c73fd763`
- 工作区该 zip 的 SHA-256 **与记录完全一致**

若误用工作区作基准，会额外多出一项差异：`built_manifest_files_found` 记录为 `[]`，而工作区因含 `build/` 与 `validation/` 会返回 16 项。改用源包后该项**自动消失**。

**这一项是操作者选错基准，不是脚本缺陷。**

---

## 6. 修法

三处 `str(...)` 改 `.as_posix()`：

```diff
- 'path':str(p.relative_to(S))
+ 'path':p.relative_to(S).as_posix()
```

```diff
- policy_classes=[p.stem for p in java if str(p.relative_to(S)).startswith(policy_path)]
+ policy_classes=[p.stem for p in java if p.relative_to(S).as_posix().startswith(policy_path)]
```

建议同时把 `:143` 的 groups 键也改 `.as_posix()`，使全文件的路径约定统一（当前 `:143` 与 `:273` 两种写法并存）。

**注意：** 修改后重算的 `mixin_rows[].path` 等字段会变成 `/` 形式，与现有记录一致，**无需重建记录**。若重建，则 `built_manifest_files_found` 会随基准不同而变，建议在记录里显式标注该字段对基准敏感。

---

## 7. 边界

- 本报告**不修改** fix7 交付件的任何字节；脚本保持原样，缺陷留待原作者处置。
- 本报告**不声称**被检查的文档树有缺陷——恰恰相反：经三条独立证据确认，**唯一失败项是检查器自身在 Windows 上的分隔符处理**。
- `:275`/`:276` 的语料集校验（Java / native）在 Windows 上**通过**，说明语料本身与记录一致。
- **未做：** 未改源码、构建、资源、`validation/` 台账；未回填 PASS；未在任何环境下实机验证修复（本报告只给修法，未施加）。
- L0 三条铁律与 UX1 不变。
