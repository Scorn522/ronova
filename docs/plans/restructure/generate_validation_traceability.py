#!/usr/bin/env python3
"""Generate/check documentation-only traceability; optionally inspect an explicitly described ledger read-only.

No imports of Mod code, no network, no child processes, no writes to validation/.
--check compares the derived Markdown to the checked source/association manifest.
An absent local acceptance-matrix.json is INPUT_MISSING for this invocation only.
A recorded user-workspace report is separate provenance, never a synthetic local ledger.
"""
from __future__ import annotations
import argparse
from collections import Counter, defaultdict
import hashlib
import json
import os
from pathlib import Path
import re
import sys
from urllib.parse import unquote

EXPECTED_IDS = [f'B{i:02d}' for i in range(1, 24)] + [f'X{i:02d}' for i in range(24, 69)]
KINDS = {'DESIGN_PARTIAL': '设计子范围', 'CONTEXT_ONLY': '仅邻近/逆向，非覆盖', 'NO_DIRECT_CASE': '未定位直接项'}
DATA_REL = 'docs/plans/restructure/validation-traceability.json'
OUTPUT_REL = 'docs/plans/VALIDATION-TRACEABILITY.md'


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def read_text(path: Path) -> str:
    return path.read_bytes().decode('utf-8')


def cell(text: str) -> str:
    return text.strip().replace('|', '&#124;').replace('\r', '').replace('\n', '<br>')


def slug(text: str) -> str:
    return re.sub(r'[^\w\-]', '', re.sub(r'\s+', '-', text.lower()), flags=re.UNICODE)


def relative(destination: str) -> str:
    path, sep, frag = destination.partition('#')
    rel = os.path.relpath(path, 'docs/plans').replace('\\', '/')
    return rel + (sep + frag if sep else '')


def requirement_anchor(key: str) -> str:
    return 'requirement-' + re.sub(r'[^a-z0-9]+', '-', key.lower()).strip('-')


def ms_anchor(key: str) -> str:
    return 'ms-check-' + key.lower().replace(':', '-')


def link(label: str, destination: str) -> str:
    return '[' + cell(label) + '](' + relative(destination) + ')'


def local_source(root: Path, rel: str) -> Path:
    """Manifest source paths must stay within the unpacked project."""
    path = (root / rel).resolve()
    if not path.is_relative_to(root.resolve()):
        raise ValueError('Source path escapes project: ' + rel)
    if not path.is_file():
        raise ValueError('Missing source: ' + rel)
    return path


def validate(root: Path, data: dict) -> dict:
    errors: list[str] = []
    def require(ok: bool, msg: str) -> None:
        if not ok:
            errors.append(msg)
    for rel, expected in data['input_hashes'].items():
        require(sha(local_source(root, rel).read_bytes()) == expected, 'Input hash differs: ' + rel)
    cases = data['cases']; requirements = data['requirements']; ms_checks = data['ms_checks']
    require([c['id'] for c in cases] == EXPECTED_IDS, 'Case set/order differs from original 68 definitions')
    req_by_key = {r['key']: r for r in requirements}
    require(len(req_by_key) == len(requirements), 'Duplicate scoped requirement key')
    require(len({m['key'] for m in ms_checks}) == len(ms_checks), 'Duplicate MS source-line key')
    require(len(ms_checks) == 28, 'MS source checks differ from six outcomes plus two 11-row matrices')
    require(not data['gap_map_07_state']['gap_closed'], 'Mapping alone cannot close GAP-MAP-07')
    sections = json.loads(read_text(root / 'docs/plans/restructure/section-map.json'))['sections']
    coverage = json.loads(read_text(root / 'docs/plans/restructure/ms-coverage.json'))['gaps']
    coverage_by_id = {g['id']: g for g in coverage}
    for obj in cases + requirements + ms_checks:
        ref = obj['source']; ident = obj.get('key', obj.get('id', '?'))
        if 'source_text' in ref:
            p = ref.get('snapshot_path', ref.get('path'))
            lines = read_text(local_source(root, p)).splitlines(keepends=True)
            text = ''.join(lines[ref['start_line'] - 1:ref['end_line']])
            require(text == ref['source_text'], 'Quoted source text differs: ' + ident)
            require(sha(text.encode('utf-8')) == ref['source_text_sha256'], 'Quote hash differs: ' + ident)
        if 'source_alias' in ref:
            sec = next((s for s in sections if s['alias'] == ref['source_alias'] and
                        s['start_line'] <= ref['start_line'] <= ref['end_line'] <= s['end_line']), None)
            require(sec is not None, 'Source range not in section map: ' + ident)
            if sec:
                require(ref['destination'] == sec['destination'] + '#' + sec['anchor'], 'Wrong new-book anchor: ' + ident)
                require(ref['source_path'] == sec['source'], 'Wrong source path: ' + ident)
        if 'coverage_id' in ref:
            require(coverage_by_id.get(ref['coverage_id']) == ref['coverage_original'], 'Gap source changed: ' + ident)
        dest, _, anchor = ref['destination'].partition('#')
        target = local_source(root, dest)
        if anchor.startswith(('orig-', 'gap-')):
            require(f'<a id="{anchor}"></a>' in read_text(target), 'Missing explicit target anchor: ' + ident)
        if 'case_ids' in obj:
            require(set(obj['case_ids']) <= set(EXPECTED_IDS), 'Unknown B/X relation: ' + ident)
            require(len(obj['case_ids']) == len(set(obj['case_ids'])), 'Duplicate B/X edge: ' + ident)
            require(obj['relation_kind'] in KINDS, 'Unrecognized relation kind: ' + ident)
        if 'requirement_keys' in obj:
            require(set(obj['requirement_keys']) <= set(req_by_key), 'Unknown scoped requirement: ' + ident)
    for r in requirements:
        ref = r['source']
        if 'source_alias' not in ref:
            continue
        require(r['key'].split(':', 1)[0] == ref['source_alias'], 'Requirement source alias mismatch: ' + r['key'])
        first = ref['source_text'].splitlines()[0]
        if first.startswith('|'):
            first_cell = first.split('|')[1].replace('*', '').replace('`', '').strip()
            require(bool(re.match(re.escape(r['id']) + r'(?:\s*/\s*PROC-INV-1)?$', first_cell)), 'Wrong original row ID: ' + r['key'])
        else:
            require(r['id'] == 'D33' and 'D33 修订' in first, 'Unrecognized numbered definition: ' + r['key'])
        if ':C-block:' in r['key']:
            require('C 块' in ref['section_title'], 'C identity points outside original C block: ' + r['key'])
        if ':D-block:' in r['key']:
            require('D 块' in ref['section_title'], 'D enhancement identity points outside original D block: ' + r['key'])
    for c in cases:
        if c['definition_format'] == 'table_row':
            m = re.fullmatch(r'\| ([BX]\d{2}) \| (.*?) \| (.*?) \|\s*', c['source']['source_text'])
            require(bool(m) and m.groups() == (c['id'], c['scenario'], c['expected']), 'Case row altered: ' + c['id'])
        else:
            original = c['source']['source_text'].splitlines(keepends=True)
            require(original[0].strip().lstrip('#').strip() == c['scenario'] and ''.join(original[1:]) == c['expected'], 'Case section altered: ' + c['id'])
    for m in ms_checks:
        src = m['source']['source_text'].strip()
        if m['key'].startswith('skills:'):
            expected = src.split('. ', 1)[1]
            require(m['scenario'] == expected == m['expected'], 'MS visible result text altered: ' + m['key'])
        else:
            cells = [x.strip() for x in src.strip('|').split('|')]
            require(cells == [m['scenario'], m['expected']], 'MS table row altered: ' + m['key'])
    case_by_id = {c['id']: c for c in cases}
    for e in data['explicit_original_id_edges']:
        require(e['requirement'] in req_by_key and e['case_id'] in case_by_id, 'Unknown explicit edge')
        require(e['quote'] in case_by_id[e['case_id']]['source']['source_text'], 'Explicit ID quote not in source')
        require(e['case_id'] in req_by_key[e['requirement']]['case_ids'], 'Explicit edge omitted from design relation')
    aggregate = data['ledger']['historical_aggregate']
    source_line = read_text(root / aggregate['source']).splitlines()[aggregate['source_line'] - 1]
    require('28 PARTIAL / 40 NOT_RUN / 0 完整 PASS' in source_line, 'Historical aggregate source line differs')
    require((aggregate['PARTIAL'], aggregate['NOT_RUN'], aggregate['PASS']) == (28, 40, 0), 'Historical aggregate altered')
    # Check the recorded report's internal consistency, not the external run's authenticity.
    reported = data['ledger']['workspace_observation']
    require(reported['provenance'] == 'USER_REPORTED_WORKSPACE_RUN', 'Workspace report provenance must stay explicit')
    require(not reported['independently_reproduced_in_this_delivery'], 'Do not promote a user report to an independent local ledger run')
    require(not reported['runtime_evidence_verified'], 'Ledger ID/count reconciliation is not evidence verification')
    require(not reported['individual_status_rows_provided_here'], 'This report supplies no individual status rows')
    require(reported['original_ledger_sha256_provided_here'] is None, 'No original ledger hash was supplied with this report')
    require(reported['ledger_path'] == data['ledger']['expected_path'], 'Recorded ledger path differs')
    require(reported['schema'] == {'records_pointer': '/cases', 'id_field': 'id', 'status_field': 'status'}, 'Recorded command schema differs from the user report')
    counts = reported['selected_original_status_counts']
    require(all(isinstance(k, str) and type(v) is int and v >= 0 for k, v in counts.items()), 'Recorded status counts must be nonnegative integers')
    require(sum(counts.values()) == reported['total_selected_rows'] == len(EXPECTED_IDS), 'Recorded row/count totals disagree')
    require(reported['exact_68_unique_ids'] and not any(reported[k] for k in ('duplicate_ids', 'missing_ids', 'extra_ids')), 'Recorded ID reconciliation is inconsistent')
    require(data['gap_map_07_state']['per_case_ledger_binding'] == 'USER_REPORTED_ID_SET_AND_AGGREGATES_MATCHED', 'Ledger subtask must retain its provenance-qualified scope')
    # CASE STATUS IS INTENTIONALLY ABSENT FROM THE STATIC MANIFEST.
    require(all('status' not in c for c in cases), 'Do not manufacture per-case status in the design manifest')
    require(set(data['scope_cautions']) == {'B23', 'X66'}, 'Known obsolete premises not recorded')
    return {'errors': errors, 'cases': len(cases), 'scoped_requirement_rows': len(requirements),
            'requirement_groups': dict(Counter(r['group'] for r in requirements)),
            'requirement_case_edges': sum(len(r['case_ids']) for r in requirements),
            'explicit_original_id_edges': len(data['explicit_original_id_edges']),
            'no_direct_case_rows': sum(not r['case_ids'] for r in requirements),
            'ms_source_checks': len(ms_checks), 'ms_case_edges': sum(len(m['case_ids']) for m in ms_checks),
            'ms_checks_without_direct_case': [m['key'] for m in ms_checks if not m['case_ids']],
            'source_hashes_checked': len(data['input_hashes']),
            'recorded_workspace_ledger_report': {
                'provenance': reported['provenance'],
                'source_record': reported['source_record'],
                'reported_exact_68_unique_ids': reported['exact_68_unique_ids'],
                'reported_status_counts': reported['selected_original_status_counts'],
                'independently_reproduced_in_this_delivery': False,
                'note': 'Recorded user report only; ledger_observation separately describes the local file actually inspected by this invocation.'},
            'mapping_semantics_automatically_proven': False,
            'runtime_evidence_verified': False, 'gap_map_07_closed': False}


def generate(data: dict) -> str:
    lines: list[str] = []
    def put(*values: str) -> None:
        lines.extend(values)
    put('# Ronova 规划条款 ↔ B/X 验收规格对照', '',
        '当前入口：2026-09-15 · fix12；原映射基线为fix3。**这是来源可追溯的设计映射，不是新验收台账，不是覆盖率报告。**', '',
        '[规划入口](README.md) · [不变量索引](INVARIANT-INDEX.md) · [缺口映射](RESTRUCTURE-MAP.md#gap-map-07) · [本轮审阅说明](RESTRUCTURE-REVIEW.md#review-fix3) · [记录效力与当前核对](restructure/README.md) · [映射数据](restructure/validation-traceability.json)', '',
        '<a id="acceptance-scope"></a>', '## 当前验收分工（fix12显式决策）', '',
        '**原68项只保留原需求及受影响回归职责；新增接线按[正式观察判据与绑定](WIRING-ACCEPTANCE.md#decision)验收。** 原[施工批次](R66-CAPABILITY-WIRING-PLAN.md#batches)、[19行判据原文](R66-CAPABILITY-WIRING-PLAN.md#validation)与[八方向绑定](WIRING-ACCEPTANCE.md#direction-binding)已互链；不扩编B/X，不把新增方向强贴到旧编号。', '',
        '这不是两边互相豁免：实际改动影响原要求时仍需原用例回归；同一证据允许分别绑定两边断言，状态不能互相代填。旧场景可能覆盖新工作的部分交界，但不能据来源/名称推定全部覆盖或全部无关。批次绑定是新的项目分工，不改变下方120行原人工映射与68项场景。', '',
        '当前仅决定验收口径，未提交新增运行证据；台账集合/汇总的用户回报和GAP-MAP-07证据侧未完成事实保持。没有新增PASS，也没有将未选中/未实现改成N/A。', '',
        '<a id="input-boundary"></a>', '## 输入边界与状态读法', '',
        '**当前事实（用户工作区核查回报）：** `validation/7.17/acceptance-matrix.json` 存在于用户完整工作区。用户已按 `/cases`、`id`、`status` 只读运行生成器，报告 68 个唯一 B/X ID、0 缺失、0 额外、0 重复，状态汇总为 28 PARTIAL / 40 NOT_RUN；并报告未写入 `validation/`。台账 ID 集合和状态汇总对接子项据该回报记为已核对，**不等于运行证据已绑定**。原命令、结果摘录与来源边界见[台账核查回报](RESTRUCTURE-REVIEW.md#review-fix3-ledger)。', '',
        '**fix2 的历史输入边界：** 当时收到的原 r66 文档包、MS 重构 fix1 文档包及源码包没有随包提供该文件。这个归档事实保留，**不得推广为项目或完整工作区没有账本**。本轮仍未收到原账本字节、SHA-256 或 68 行明细，不声称在此环境独立重跑用户的台账核查。', '',
        '[原进度](../CURRENT-PROGRESS.md#验收与下一步)记载 **28 PARTIAL / 40 NOT_RUN / 0 完整 PASS**；用户回报的状态计数与此前汇总一致。**本静态索引不收录逐项状态**，不从 28/40 推算哪个 B/X 是 PARTIAL 或 NOT_RUN，不把“未收录”写成新的验收状态。68 行原状态仍以适用工作区的真实账本为准。', '',
        '源规格、原 ID、`validation/`、原进度与历史证据不修改。本交付未包含的账本/附件不会被创建为空台账或假 fixture。恢复链正例、文档脚本成功、ID 对齐和局部结果都不能据此升级整项验收。', '',
        '本表的 **68 项**仅指 V26 验收编号；待采纳快照中的同号研究模块、DS/RULE 的加强编号和 L6 决策编号均不是同一命名空间。用完整来源与章节限定身份。', '',
        '<a id="relation-legend"></a>', '## 关联强度：映射不等于证明', '',
        '| 标记 | 含义 |', '|---|---|',
        '| 原文明引 | B/X 原句直接提到该原 ID；只说明引用关系，仍不代表已覆盖或通过。 |',
        '| 设计子范围 | 本轮根据两端原文建立的可审阅关联；未取得原台账对该映射的声明，仍需 fixture/断言/证据确认。 |',
        '| 仅邻近/逆向，非覆盖 | 可用于找实现入口，但语义方向或对象范围不同；不能当作覆盖边。 |',
        '| 未定位直接项 | 不强贴 B/X；保留尚需明确的子场景/结构/阶段核查。 |', '',
        '**这 120 行及其关联是人工设计判断。** `--check` 核对摘录、编号、关系集合及派生内容一致性，不能证明关联正确或覆盖完整；用户回报的台账 ID/计数一致也不能代替这项语义审阅。优先核查[未定位直接用例的 9 行](#manual-review-queue)，其余有链接的行同样不自动视为审阅通过。', '',
        '**尤其：ERASED 目标禁止重插 ≠ PROTECTED 实体缺表后的补回；Root 原子事务 ≠ ID/UUID 注册表原子修复；Root receipt ≠ 任意客户端像素已达标。** 下方每个 C/GAP 行写明剩余差距。', '',
        '原文明引只有下列已核对的引用边；其他边即使语义接近，也统一按本轮设计关联阅读：', '',
        '| 原 ID（限定来源） | 原 B/X | 原句证据 |', '|---|---|---|')
    for e in data['explicit_original_id_edges']:
        put(f"| [{e['requirement']}](#{requirement_anchor(e['requirement'])}) | [{e['case_id']}](#case-{e['case_id'].lower()}) | `{e['quote']}` |")
    put('', '<a id="scope-cautions"></a>', '## 两处旧前提的当前效力', '',
        '| 用例 | 保留与限制 |', '|---|---|',
        '| [B23](#case-b23) | 原 helper attach/relaunch 正例不适用当前[不起外部进程](README.md#scope-process)边界；原文和 ID 保留，当前 JVM 唯一 writer 子断言保留。需另取得新入口/失败 fallback/实际进程树证据，不能沿旧正例启动进程，也不能把整项直接抹成 N/A。 |',
        '| [X66](#case-x66) | 原五级规范顺序前提按[当前文档治理](README.md#scope-governance)失效；自包含编号、缺失历史透明登记、状态真实性要求仍保留。本轮索引脚本不回填该项状态。 |', '',
        'DS 的 P 层 C 项仍按 [fix1 定级](README.md#scope-p-layer)阅读；“无参数”不是额外硬限制，前置权限/初始化条件并未随之消失。其余原规格不在本轮改写。', '',
        '<a id="case-directory"></a>', '## 68 项验收规格：正向入口', '',
        '“场景 / 原题”来自原文；完整断言在新书唯一正文落点。右列只是关联导航。本静态表未收录逐项状态；用户的台账集合核查另见输入边界，不把正向列表完整误读成 68 项已覆盖。', '')
    reverse: dict[str, list] = defaultdict(list)
    msreverse: dict[str, list] = defaultdict(list)
    for r in data['requirements']:
        for cid in r['case_ids']:
            reverse[cid].append(r)
    for m in data['ms_checks']:
        for cid in m['case_ids']:
            msreverse[cid].append(m)
    for group, pred in [('B01–B23：基础验收', lambda cid: cid.startswith('B')), ('X24–X68：增量验收', lambda cid: cid.startswith('X'))]:
        put('### ' + group, '', '| 原编号 / 状态呈现范围 | 场景 / 原题 | 新书原规格与来源 | 条款与 MS 检查导航（非覆盖声明） |', '|---|---|---|---|')
        for c in data['cases']:
            if not pred(c['id']):
                continue
            s = c['source']; cid = c['id']
            rels = [f"[{r['key']}](#{requirement_anchor(r['key'])})" for r in reverse[cid]]
            rels += [f"[{m['key']}](#{ms_anchor(m['key'])})" for m in msreverse[cid]]
            scope = '<br>**旧前提见上节**' if cid in data['scope_cautions'] else ''
            put(f"| <a id=\"case-{cid.lower()}\"></a>**{cid}**<br>逐项状态未收录{scope} | {cell(c['scenario'])} | {link('V26 原文', s['destination'])}<br>`V26:L{s['start_line']}–L{s['end_line']}` | {' · '.join(rels) or '未强行添加关联；原规格仍保留'} |")
        put('')
    put('<a id="requirement-directory"></a>', '## 反向：原条款 / C 块 / 缺口 → 验收规格', '',
        '本节列 L6 明确决策行、V26 D33 及 D53–D106 明确条款、DS/RULE 两份加强 C 块、RULE 加强 D 块和 11 个登记缺口。**不是对全语料同号 ID 的语义合并或定义补全**；其他来源和全部文本出现仍看 [INVARIANT-INDEX](INVARIANT-INDEX.md)。`D1–D32` 的范围端点不自动变成独立条文；37 个既有编号空洞不新增内容、不重编号。', '',
        '**DS:C12/C13/C14 与 RULE:C12/C13/C14 分开列；RULE 加强 D1–D4 不等于 L6 决策 D1–D4。** 即使两份 C 行文字相近，也分别保留来源和边。', '')
    for group in dict.fromkeys(r['group'] for r in data['requirements']):
        put('### ' + group, '', '| 原编号（来源/章节限定） | 原文 / 设计主题 | 关联 B/X 与强度 | 关联依据及尚缺内容 |', '|---|---|---|---|')
        for r in data['requirements']:
            if r['group'] != group:
                continue
            s = r['source']
            source_label = f"{s['source_alias']}:L{s['start_line']}–L{s['end_line']}" if 'source_alias' in s else '登记缺口原文'
            c_links = ' · '.join(f'[{cid}](#case-{cid.lower()})' for cid in r['case_ids']) or '—'
            note = '<br>**效力：**' + cell(r['scope_note']) if r['scope_note'] else ''
            put(f"| <a id=\"{requirement_anchor(r['key'])}\"></a>`{r['key']}` | {link(source_label, s['destination'])}<br>{cell(r['title'])} | {c_links}<br>**{KINDS[r['relation_kind']]}** | {cell(r['basis'])}<br>**尚缺：**{cell(r['uncovered'])}{note} |")
        put('')
    put('<a id="manual-review-queue"></a>', '## 需人工复核的 9 行：当前未定位直接用例', '',
        '以下列表从现有 `case_ids` 为空的行生成，**不新增结论，不等于九项运行失败或九个必须新增的 B/X 用例**。阶段规则、构建一致性和文档交付项可能需要各自的审阅/产物记录；是否应新增、复用或不适用须人工明确理由。本轮没有完成这项语义审阅，也不自动将它们标为 N/A。', '',
        '| 来源限定原 ID | 现有主题 | 原映射已登记的待核内容 |', '|---|---|---|')
    for requirement in data['requirements']:
        if not requirement['case_ids']:
            put(f"| [{requirement['key']}](#{requirement_anchor(requirement['key'])}) | {cell(requirement['title'])} | {cell(requirement['uncovered'])} |")
    put('', '复核每条关联时要确认原来源/章节、对象与方向、触发和前置条件、断言实际能证明什么，以及适用产物和双端观察范围；没有证据的只保留候选或未定位状态，不靠编号相近认定覆盖。', '')
    put('<a id="ms-checks"></a>', '## MS 的 28 个检查条目 → 现有 B/X 候选子范围', '',
        '保留三份 zh.md 的原场景与预期；`skills:L47` 等只是来源行定位，**不是新测试 ID**。两组矩阵各自要求单人和至少两个客户端连接的专用服务器；分别记录产物/配置、目标身份、触发入口、服务器与每个客户端的观察/回执。当前没有绑定这些运行证据。', '',
        '“尚缺”是在来源基础上的对接建议，不是声称已有 fixture 或已完成能力。多部位实体没有强贴 B/X；实体独占条目仅在明确选择该策略时适用，不因此默认开启。', '')
    for group in dict.fromkeys(m['group'] for m in data['ms_checks']):
        put('### ' + group, '', '| 原来源 / 适用对象 | 原场景与预期（原文） | B/X 候选与条款 | 尚缺的观察 / 子场景 |', '|---|---|---|---|')
        for m in data['ms_checks']:
            if m['group'] != group:
                continue
            c_links = ' · '.join(f'[{cid}](#case-{cid.lower()})' for cid in m['case_ids']) or '未定位直接项'
            r_links = ' · '.join(f"[{key}](#{requirement_anchor(key)})" for key in m['requirement_keys'])
            put(f"| <a id=\"{ms_anchor(m['key'])}\"></a>{link(m['key'], m['source']['destination'])}<br>{cell(m['applicability'])} | {cell(m['scenario'])}<br>**预期：**{cell(m['expected'])} | {c_links}<br>{r_links}<br>**{KINDS[m['relation_kind']]}** | {cell(m['uncovered'])}<br>{cell(m['environment'])} |")
        put('')
    put('<a id="ledger-bind"></a>', '## 原台账接线：已做与未做', '',
        '| 子项 | 当前记录及证据来源 |', '|---|---|',
        '| 规格侧入口 | 68 项 B/X 规格、120 行 D/C/GAP、MS 28 项检查的来源与候选关系已经列出；不等于语义已审完。 |',
        '| 工作区台账 ID / 状态汇总 | **据用户实际运行回报已核对**：68 个唯一 ID、缺/多/重均为 0、28 PARTIAL / 40 NOT_RUN。只对该回报所指工作区和运行生效。 |',
        '| 本交付的独立台账复算 / 逐项状态呈现 | 没有收到原账本和明细；本地未独立复算，不从汇总分配 68 行状态。 |',
        '| artifact / fixture / 环境 / 断言 / receipt / 双客户端观察 | **未绑定、未复核，仍待运行时与证据工作。** ID 对齐不能关闭这一侧。 |',
        '| GAP-MAP-07 总体 | **仍开放；台账集合/汇总接线子项按用户回报已核，证据侧未闭合。** 不把“一半”换算成百分比，不新增原 GAP 编号。 |', '',
        '已知工作区账本结构由用户实际调用提供：记录集合 `/cases`、ID 字段 `id`、原状态字段 `status`。在相应完整工作区可只读复核（在项目根运行）：', '',
        '```powershell', 'python docs/plans/restructure/generate_validation_traceability.py --root . --check --ledger validation/7.17/acceptance-matrix.json --records-pointer /cases --id-field id --status-field status', '```', '',
        '这是用户已验证的 schema，不是为未提供文件猜字段；当前工作区有变化时，以该次真实读取为准。缺行、重复 ID、未知编号和原状态原样输出，不能由已有汇总覆盖新观察。', '',
        '上述四个参数只令标准输出 JSON 的 `ledger_observation` 给出本次实际文件的原状态、计数、ID 差异和 SHA-256；不将这些状态写入本页或 `validation/`。`recorded_workspace_ledger_report` 则是另列的用户回报摘要，**两种来源不互相替代**。未给 schema 的本地文件为 `PRESENT_UNPARSED`，本次环境缺文件为 `INPUT_MISSING`；都不是关于所有工作区的断言。', '',
        '<a id="reproduce"></a>', '## 可重复生成与核对', '',
        '在完整文档根目录或合并文档后的项目根目录运行（Python 标准库）：', '',
        '```powershell', 'python docs/plans/restructure/verify_restructure.py',
        'python docs/plans/restructure/generate_invariant_index.py --check',
        'python docs/plans/restructure/generate_validation_traceability.py --check', '```', '',
        '本生成器的 `--write` 仅重新生成本 Markdown。核对摘录、哈希、来源、编号、关系集合、MS 原行及生成一致性；记录回报的自洽检查也**不能认证外部运行、证明人工语义或验证运行证据**。不运行 Mod、不回填验收结果。', '',
        '账本观察与文档核对分别报告；未给本交付提供逐项状态不是文档核对失败，也不是验收成功。[当前核对与历史记录效力](restructure/README.md)区分本次结果和历轮证据；[收口核对记录](RESTRUCTURE-CHECK.md#review-fix3-check)不宣告 Mod 或阶段完成。', '')
    return '\n'.join(lines)


def resolve_pointer(data: object, pointer: str) -> object:
    if pointer == '':
        return data
    if not pointer.startswith('/'):
        raise ValueError('records-pointer must be empty or start with /')
    value = data
    for part in pointer.split('/')[1:]:
        if re.search(r'~(?![01])', part):
            raise ValueError('Invalid JSON Pointer escape')
        part = part.replace('~1', '/').replace('~0', '~')
        if isinstance(value, dict):
            if part not in value:
                raise ValueError('JSON Pointer key absent: ' + part)
            value = value[part]
        elif isinstance(value, list) and re.fullmatch(r'0|[1-9][0-9]*', part):
            try:
                value = value[int(part)]
            except IndexError as exc:
                raise ValueError('JSON Pointer index out of bounds: ' + part) from exc
        else:
            raise ValueError('JSON Pointer does not resolve to records')
    return value


def inspect_ledger(path: Path, pointer: str | None, id_field: str | None, status_field: str | None) -> dict:
    supplied_schema = any(v is not None for v in (pointer, id_field, status_field))
    if supplied_schema and any(v is None for v in (pointer, id_field, status_field)):
        raise ValueError('Provide records-pointer, id-field and status-field together; schema is never guessed')
    if not path.is_file():
        return {'state': 'INPUT_MISSING', 'path': str(path), 'per_case_status_read': False, 'runtime_evidence_verified': False}
    raw = path.read_bytes()
    base = {'path': str(path), 'sha256': sha(raw), 'runtime_evidence_verified': False}
    if not supplied_schema:
        return dict(base, state='PRESENT_UNPARSED', per_case_status_read=False, reason='Explicit ledger schema required')
    # BOM is accepted for user JSON files; original bytes/hashes are still retained.
    value = resolve_pointer(json.loads(raw.decode('utf-8-sig')), pointer)
    if isinstance(value, dict):
        records = list(value.items())
    elif isinstance(value, list):
        if id_field == '@key':
            raise ValueError('@key requires an object, not an array')
        records = [(None, row) for row in value]
    else:
        raise ValueError('Selected records must be a JSON array or object')
    if not id_field or not status_field:
        raise ValueError('ID and status field names cannot be empty')
    rows = []
    for object_key, row in records:
        if not isinstance(row, dict):
            raise ValueError('Each selected ledger row must be an object')
        if id_field != '@key' and id_field not in row:
            raise ValueError('Explicit ID field is absent: ' + id_field)
        cid = object_key if id_field == '@key' else row[id_field]
        if not isinstance(cid, str) or not cid:
            raise ValueError('Ledger ID must be a nonempty string; no coercion performed')
        if status_field not in row or not isinstance(row[status_field], str):
            raise ValueError('Explicit status field missing/not a string for ' + cid)
        rows.append({'id': cid, 'original_status': row[status_field], 'in_specification': cid in EXPECTED_IDS})
    counts = Counter(r['id'] for r in rows)
    duplicate_ids = sorted(k for k, n in counts.items() if n > 1)
    missing_ids = [cid for cid in EXPECTED_IDS if cid not in counts]
    extra_ids = sorted(set(counts) - set(EXPECTED_IDS))
    matched = [r for r in rows if r['in_specification']]
    return dict(base, state='READ_ONLY_OBSERVED', per_case_status_read=True,
                schema={'records_pointer': pointer, 'id_field': id_field, 'status_field': status_field},
                rows=rows, total_selected_rows=len(rows),
                selected_original_status_counts=dict(Counter(r['original_status'] for r in rows)),
                matched_original_status_counts=dict(Counter(r['original_status'] for r in matched)),
                duplicate_ids=duplicate_ids, missing_ids=missing_ids, extra_ids=extra_ids,
                exact_68_unique_ids=not (duplicate_ids or missing_ids or extra_ids),
                note='Original status observations only; source evidence/fixtures/artifacts were not verified; no ledger writes.')


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[3])
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument('--check', action='store_true')
    mode.add_argument('--write', action='store_true')
    parser.add_argument('--ledger', type=Path, help='Read-only ledger path; relative to --root')
    parser.add_argument('--records-pointer', help='Explicit RFC6901 pointer to the records collection; empty string selects root')
    parser.add_argument('--id-field', help='Explicit ID field; @key only for object-keyed records')
    parser.add_argument('--status-field', help='Explicit original status field')
    args = parser.parse_args()
    try:
        root = args.root.resolve()
        data = json.loads(read_text(root / DATA_REL))
        report = validate(root, data)
        expected = generate(data)
        output = root / OUTPUT_REL
        if args.write:
            if report['errors']:
                raise ValueError('Refusing to write derived Markdown with source verification errors')
            output.write_bytes(expected.encode('utf-8'))
        matches = output.is_file() and output.read_bytes() == expected.encode('utf-8')
        report['markdown_matches_generator'] = matches
        if (args.check or not args.write) and not matches:
            report['errors'].append('Derived Markdown differs from checked mapping manifest')
        ledger = args.ledger or Path(data['ledger']['expected_path'])
        if not ledger.is_absolute():
            ledger = root / ledger
        report['ledger_observation'] = inspect_ledger(ledger, args.records_pointer, args.id_field, args.status_field)
        report['kind'] = 'documentation_traceability_only'
        report['implementation_or_runtime_tests_executed'] = False
        print(json.dumps(report, ensure_ascii=False, indent=2))
        return 1 if report['errors'] else 0
    except (OSError, ValueError, KeyError, TypeError, UnicodeError) as exc:
        print(json.dumps({'kind': 'documentation_traceability_only', 'errors': [str(exc)], 'implementation_or_runtime_tests_executed': False}, ensure_ascii=False, indent=2), file=sys.stderr)
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
