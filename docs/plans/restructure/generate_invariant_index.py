#!/usr/bin/env python3
"""Generate/check the derived, human-readable ID index. Documentation only.

Run from the extracted documentation root:
    python docs/plans/restructure/generate_invariant_index.py --write
    python docs/plans/restructure/generate_invariant_index.py --check
Python 3.9+; standard library only. Does not alter original source bodies.
"""
from __future__ import annotations
import argparse
from collections import Counter
import json
from pathlib import Path
import posixpath
import re
import sys
from verify_restructure import ID_PATTERN, ANCHOR

BOOK_LABELS = {
    'MS-SKILLS-PLAN.md': '技能书',
    'MS-MSITEM-PLAN.md': '物品书',
    'MS-ENTITYDEFENCE-PLAN.md': '防御书',
}


def natural_key(value: str) -> tuple:
    return tuple((1, int(p)) if p.isdigit() else (0, p) for p in re.split(r'(\d+)', value))


def cell(value: str) -> str:
    return value.replace('|', '&#124;').replace('\n', ' ')


def generate(root: Path) -> tuple[str, dict]:
    audit = root / 'docs/plans/restructure'
    load = lambda name: json.loads((audit / name).read_text(encoding='utf-8'))
    inventory = load('id-inventory.json')['sources']
    manifest = load('source-manifest.json')['sources']
    sections = load('section-map.json')['sections']
    section_by_anchor = {s['anchor']: s for s in sections}
    sources = {s['alias']: s for s in manifest}
    original_lines = {
        a: (root / s['archive']).read_text(encoding='utf-8').splitlines(keepends=True)
        for a, s in sources.items()
    }
    if set(inventory) != set(sources):
        raise ValueError('ID inventory and source manifest have different source sets')
    book_anchors = {
        b: set(ANCHOR.findall((root / ('docs/plans/' + b)).read_text(encoding='utf-8')))
        for b in BOOK_LABELS
    }
    # Reconstruct the expected locations from the preserved original text.
    for alias, entry in inventory.items():
        if entry['path'] != sources[alias]['path']:
            raise ValueError(alias + ': source path mismatch')
        expected_counts = Counter()
        expected_locations: dict[str, list] = {}
        for s in sorted((s for s in sections if s['alias'] == alias), key=lambda s: s['source_index']):
            body = ''.join(original_lines[alias][s['start_line'] - 1:s['end_line']])
            counts = Counter(ID_PATTERN.findall(body))
            expected_counts.update(counts)
            for identifier, count in counts.items():
                expected_locations.setdefault(identifier, []).append({
                    'anchor': s['anchor'],
                    'destination': s['destination'] + '#' + s['anchor'],
                    'count': count,
                })
        if dict(expected_counts) != entry['counts'] or expected_locations != entry['locations']:
            raise ValueError(alias + ': ID counts/locations differ from the preserved source')
        if sum(expected_counts.values()) != entry['occurrences'] or len(expected_counts) != entry['distinct_spellings']:
            raise ValueError(alias + ': ID summary mismatch')
        for locations in entry['locations'].values():
            for loc in locations:
                s = section_by_anchor[loc['anchor']]
                if s['alias'] != alias or loc['destination'] != s['destination'] + '#' + s['anchor']:
                    raise ValueError(alias + ': source/destination mismatch')
                if loc['anchor'] not in book_anchors[Path(s['destination']).name]:
                    raise ValueError('Missing destination anchor: ' + loc['anchor'])

    def section_at(alias: str, line: int) -> dict:
        return next(s for s in sections if s['alias'] == alias and s['start_line'] <= line <= s['end_line'])

    def location_link(s: dict, label: str | None = None) -> str:
        name = Path(s['destination']).name
        return '[' + cell(label or BOOK_LABELS[name]) + '](' + name + '#' + s['anchor'] + ')'

    def source_label(s: dict) -> str:
        return f"`{s['alias']}:L{s['start_line']}–L{s['end_line']}`"

    stats = {
        'source_count': len(sources),
        'scoped_id_rows': sum(len(s['counts']) for s in inventory.values()),
        'distinct_id_spellings_across_sources': len({k for s in inventory.values() for k in s['counts']}),
        'mapped_location_entries': sum(len(v) for s in inventory.values() for v in s['locations'].values()),
        'recorded_id_occurrences': sum(s['occurrences'] for s in inventory.values()),
    }
    out = [
        '# Ronova 跨文档不变量与条款 ID 索引', '',
        '更新：2026-09-14 · 内容基线 fix1，fix2 追加验收规格入口。**派生阅读入口，不是新增规范，也不是实现或验收结果。**', '',
        '[规划入口](README.md) · [B/X 验收规格对照](VALIDATION-TRACEABILITY.md) · [新旧章节映射](RESTRUCTURE-MAP.md) · [本轮审阅处理](RESTRUCTURE-REVIEW.md) · [机器清单](restructure/id-inventory.json)', '',
        '从 `id-inventory.json` 生成，结合 `section-map.json` 和保真快照校验来源、原行范围、每段次数与新书锚点。'
        '编号明细一行对应一个**来源内的原 ID 拼写**；同一行列出全部登记位置，不把首次命中当作唯一权威定义。', '',
        f"覆盖 **{stats['source_count']} 份原来源、{stats['scoped_id_rows']} 个来源内 ID 条目、{stats['mapped_location_entries']} 个位置条目、{stats['recorded_id_occurrences']:,} 次文本出现**。"
        f"跨来源共有 {stats['distinct_id_spellings_across_sources']} 种编号拼写；不跨来源合并、不重编号。", '',
        '**读法提醒：** `D*`、`INV*`、`A*`、`R*`、`ID-*`、`T-*` 是原清单的检索家族，不保证每次出现都是定义；也包含引用、测试项和加强项。'
        '同一来源内部亦可能复用编号，例如 DS 的 `A5` 既用于不变量表，也用于加强 A 块。必须结合原标题和上下文阅读。'
        '本索引不裁决同号异义，不覆盖原稿已登记的两张 `L6-INV-*` 表不一致问题。', '',
        '<a id="overview"></a>', '## 不变量全集：人读总入口', '',
        '按原 `DESIGN-SPEC.md` §4.2、§5 的阅读顺序恢复聚合导航。正文仍在三书唯一落点，索引不复制第二份规范。', '',
        '| 原阅读单元 | 新书正文 | 原来源 |', '|---|---|---|',
    ]
    quick = [
        (177, '§4.2：D1–D13 决策表'), (265, '§5：不变量全集'),
        (267, '§5.1：PROC-INV-1 / D13；helper 原授权须按当前进程边界阅读'),
        (283, '§5.2：L5-INV-1…9；保留原候选项说明'),
        (299, '§5.3：L6-INV-1…10；含同号两表不一致的原提醒'),
        (319, '§5.4：D33 修订'), (329, '§5.5：D53–D106 分组导读及 D1–D32 继承原则'),
        (364, '§5.6：CF 宪法级不变量与 R1–R7'),
        (385, '§5.7：P0-A A1–A8、宪法级不变量与特有规则'),
    ]
    for line, label in quick:
        s = section_at('DS', line)
        out.append(f'| {label} | {location_link(s)} | {source_label(s)} |')
    out += [
        '', '原文中的 helper 允许句只保真、不授权起进程，见[当前进程边界](README.md#scope-process)。'
        '“冻结”“宪法级”“必要条件”按[当前文档效力](README.md#scope-governance)阅读，不新增第五条铁律。', '',
        '<a id="named-invariants"></a>', '### 无编号宪法级不变量', '',
        '这些名称不在编号正则清单中，因此单独按原章节补导航，不伪造 ID。CF 和 P0-A 各有八个名称，分别保留；相似名称不合并。', '',
        '| 原来源 | 原名称（逐字保留） | 新书原章节 |', '|---|---|---|',
    ]
    named_count = 0
    for alias, line in [('CF', 826), ('P0A', 1261)]:
        s = section_at(alias, line)
        raw = ''.join(original_lines[alias][s['start_line'] - 1:s['end_line']])
        match = re.search(r'```text\s*\n(.*?)\n```', raw, re.S)
        if not match:
            raise ValueError(alias + ': constitutional-name block not found')
        names = [v for v in match.group(1).splitlines() if v.strip()]
        if len(names) != 8:
            raise ValueError(alias + ': expected original eight constitutional names')
        for name in names:
            out.append(f'| {source_label(s)} | `{cell(name)}` | {location_link(s)} |')
            named_count += 1
    stats['unnumbered_constitutional_name_rows'] = named_count
    individual_d = {k for s in inventory.values() for k in s['counts'] if re.fullmatch(r'D\d+', k)}
    missing_d = [f'D{i}' for i in range(1, 107) if f'D{i}' not in individual_d]
    stats['d1_d106_without_individually_recorded_spelling'] = missing_d
    out += [
        '', '<a id="range-notation"></a>', '### 范围写法不展开为虚构条文', '',
        '输入有 `D1–D32`、`D33–D106` 等范围写法。编号清单只登记实际出现的编号拼写，不据端点或省略号生成中间编号的定义。'
        '尤其 `D14–D31`、`D34–D52` 没有作为独立编号拼写被原清单登记；这不是本轮删项，也不证明相应设计内容不存在。', '',
        '| 原分组 | 原文入口 | 来源 |', '|---|---|---|',
    ]
    for alias, line, label in [('V26', 455, 'D1–D32 继承原则'), ('V26', 472, 'D33 修订（不代写 D34–D52）')]:
        s = section_at(alias, line)
        out.append(f'| {label} | {location_link(s)} | {source_label(s)} |')
    out += ['', '<a id="source-directory"></a>', '## 按来源查看全部编号', '',
            '各表中新书链接与来源行范围按相同顺序一一对应；`×n` 是该原段落内的文本出现次数，不是完成数量。', '',
            '| 来源 | 原文件 / 导航 | 编号条目 | 文本出现 |', '|---|---|---:|---:|']
    for s in manifest:
        alias = s['alias']
        nav = posixpath.relpath(s['path'], 'docs/plans')
        entry = inventory[alias]
        out.append(f"| [{alias}](#ids-{alias.lower()}) | [{Path(s['path']).name}]({nav}) | {len(entry['counts'])} | {entry['occurrences']} |")
    for source in manifest:
        alias = source['alias']
        entry = inventory[alias]
        out += ['', f'<a id="ids-{alias.lower()}"></a>', f'### {alias}', '',
                f"原来源：`{source['path']}`。"
                f"[来源映射](RESTRUCTURE-MAP.md#source-{alias.lower()}) · "
                f"[原字节快照]({posixpath.relpath(source['archive'], 'docs/plans')}) · "
                '[返回来源目录](#source-directory)。', '']
        if not entry['counts']:
            out += ['原编号清单在此来源下未登记上述 ID 家族；**不等于没有条款**。原文保留由文档恢复核对覆盖。']
            continue
        out += ['| 原 ID | 新书锚点（×本段次数） | 原来源行范围 | 总次数 |', '|---|---|---|---:|']
        for identifier in sorted(entry['counts'], key=natural_key):
            locations = sorted(entry['locations'][identifier], key=lambda loc: section_by_anchor[loc['anchor']]['start_line'])
            links, lines = [], []
            for loc in locations:
                s = section_by_anchor[loc['anchor']]
                links.append(location_link(s, BOOK_LABELS[Path(s['destination']).name] + f" · 原L{s['start_line']}") + f" ×{loc['count']}")
                lines.append(source_label(s))
            out.append(f"| `{cell(identifier)}` | {'；'.join(links)} | {'；'.join(lines)} | {entry['counts'][identifier]} |")
    out += ['', '<a id="reproduce"></a>', '## 重新生成与核对', '',
            '在完整文档根目录，或已经合并完整 r66 文档的项目根目录执行：', '',
            '```powershell', 'python docs/plans/restructure/generate_invariant_index.py --write',
            'python docs/plans/restructure/generate_invariant_index.py --check', '```', '',
            '`--write` 只重写本索引；`--check` 比较索引与机器清单、来源快照及新书锚点的一致性，不写文件。'
            '二者都不编译、不启动游戏、不确认任何源码功能或阶段完成。', '']
    return '\n'.join(out), stats


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[3])
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--write', action='store_true')
    group.add_argument('--check', action='store_true')
    args = parser.parse_args()
    try:
        body, stats = generate(args.root.resolve())
        target = args.root / 'docs/plans/INVARIANT-INDEX.md'
        expected = body.encode('utf-8')
        if args.write:
            target.write_bytes(expected)
        equal = target.is_file() and target.read_bytes() == expected
        report = {'kind': 'derived_document_index_only', 'statistics': stats,
                  'markdown_matches_generator': equal, 'errors': [] if equal else ['Index differs from generated output'],
                  'implementation_or_runtime_tests_executed': False}
        print(json.dumps(report, ensure_ascii=False, indent=2))
        return 0 if equal else 1
    except (OSError, ValueError, KeyError, TypeError, StopIteration) as exc:
        print('Index generation/check could not complete: ' + str(exc), file=sys.stderr)
        return 2


if __name__ == '__main__':
    raise SystemExit(main())
