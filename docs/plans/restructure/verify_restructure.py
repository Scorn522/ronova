#!/usr/bin/env python3
"""Verify documentation migration integrity only; never builds or runs Ronova.

Usage from the extracted documentation or an r66 repository overlay:
    python docs/plans/restructure/verify_restructure.py
    python docs/plans/restructure/verify_restructure.py --root /path/to/root --write
Python 3.9+; standard library only. --write emits verification.json.
"""
from __future__ import annotations
import argparse
from collections import Counter, defaultdict
import hashlib
import html
import json
from pathlib import Path
import re
import sys
import unicodedata
from urllib.parse import unquote, urlsplit

BLOCK = re.compile(r'<!-- RONOVA-ORIGINAL-BEGIN ([A-Za-z0-9_-]+) -->\n(.*?)\n<!-- RONOVA-ORIGINAL-END \1 -->', re.S)
ID_PATTERN = re.compile(r'(?<![A-Za-z0-9_])(?:[A-Z][A-Z0-9]*-)*INV(?:-[A-Za-z0-9]+)+|(?<![A-Za-z0-9_])(?:D\d+(?:-[A-Za-z0-9]+)*|A\d+(?:-[A-Za-z0-9]+)*|R\d+(?:[′\u2032]|-[A-Za-z0-9]+)*|R-[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*|ID-[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*|T-[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)(?![A-Za-z0-9_])')
ANCHOR = re.compile(r'<a\s+(?:[^>]*?\s)?id=["\']([^"\']+)["\'][^>]*>')
BOOK_NAMES = {'MS-SKILLS-PLAN.md', 'MS-MSITEM-PLAN.md', 'MS-ENTITYDEFENCE-PLAN.md'}


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def text(path: Path) -> str:
    return path.read_bytes().decode('utf-8')


def slug(title: str) -> str:
    value = html.unescape(re.sub(r'<[^>]*>', '', title)).lower()
    value = ''.join(c for c in value if c in '-_' or c.isspace() or unicodedata.category(c)[0] in 'LN')
    return value.replace(' ', '-')


def headings(body: str):
    fence = None
    found = []
    for line_number, line in enumerate(body.splitlines(), 1):
        fm = re.match(r'^\s{0,3}(`{3,}|~{3,})', line)
        if fm:
            marker = fm.group(1)
            if fence is None:
                fence = (marker[0], len(marker))
            elif marker[0] == fence[0] and len(marker) >= fence[1]:
                fence = None
            continue
        if fence:
            continue
        match = re.match(r'^(#{1,6})\s+(.+?)(?:\s+#+)?\s*$', line)
        if match:
            found.append((line_number, len(match.group(1)), match.group(2)))
    return found


def restore(payload: str, edits: list) -> str:
    """Invert documented Markdown-only edits; reject undocumented edit kinds."""
    parts = []
    rendered_cursor = 0
    original_end = 0
    delta = 0
    for edit in sorted(edits, key=lambda x: x['start']):
        start, end, old, new = edit['start'], edit['end'], edit['old'], edit['new']
        if start < original_end or end - start != len(old):
            raise ValueError('Overlapping or invalid original edit range')
        kind = edit['kind']
        if kind == 'heading-depth':
            valid = bool(re.fullmatch(r'#{1,6}', old)) and new == '####'
        elif kind == 'reference-wrapper':
            valid = new == '[' + old + '](' + str(edit['target']) + ')'
        elif kind == 'link-target':
            valid = new == edit['target'] and bool(new)
        else:
            valid = False
        if not valid:
            raise ValueError('Nonpermitted content edit: ' + kind)
        rendered_start = start + delta
        rendered_end = rendered_start + len(new)
        if payload[rendered_start:rendered_end] != new:
            raise ValueError('Rendered edit text differs from the recorded Markdown edit')
        parts.extend((payload[rendered_cursor:rendered_start], old))
        rendered_cursor = rendered_end
        original_end = end
        delta += len(new) - len(old)
    parts.append(payload[rendered_cursor:])
    recovered = ''.join(parts)
    for edit in edits:
        if recovered[edit['start']:edit['end']] != edit['old']:
            raise ValueError('Recovered edit text differs from the original')
    return recovered


def verify(root: Path) -> dict:
    root = root.resolve()
    audit = root / 'docs/plans/restructure'
    load = lambda name: json.loads(text(audit / name))
    manifest = load('source-manifest.json')
    sections = load('section-map.json')['sections']
    inventory = load('id-inventory.json')['sources']
    coverage = load('ms-coverage.json')['headings']
    input_hashes = load('input-file-hashes.json')
    references = load('reference-updates.json')
    errors, checks = [], {}

    def require(condition: bool, message: str):
        if not condition:
            errors.append(message)

    payloads, book_texts = {}, {}
    for book in sorted({u['destination'] for u in sections}):
        body = text(root / book)
        book_texts[book] = body
        matches = list(BLOCK.finditer(body))
        require(len(matches) == body.count('<!-- RONOVA-ORIGINAL-BEGIN '), book + ': unmatched start marker')
        require(len(matches) == body.count('<!-- RONOVA-ORIGINAL-END '), book + ': unmatched end marker')
        for match in matches:
            anchor, payload = match.group(1), match.group(2)
            require(anchor not in payloads, 'Duplicate source body: ' + anchor)
            payloads[anchor] = (book, payload)
    require(set(payloads) == {s['anchor'] for s in sections}, 'Source-body set differs from section-map.json')
    checks['source_body_count'] = len(payloads)

    restored_by_source = defaultdict(list)
    body_edit_counts = Counter()
    for unit in sections:
        anchor = unit['anchor']
        if anchor not in payloads:
            continue
        book, payload = payloads[anchor]
        require(book == unit['destination'], anchor + ': wrong destination')
        require(sha(payload.encode('utf-8')) == unit['rendered_sha256'], anchor + ': rendered body hash changed')
        try:
            recovered = restore(payload, unit['edits'])
        except (ValueError, KeyError, TypeError) as exc:
            errors.append(anchor + ': ' + str(exc))
            continue
        require(sha(recovered.encode('utf-8')) == unit['raw_sha256'], anchor + ': original body hash differs')
        require(len(recovered.splitlines()) == unit['end_line'] - unit['start_line'] + 1, anchor + ': line partition changed')
        body_edit_counts.update(e['kind'] for e in unit['edits'])
        restored_by_source[unit['alias']].append((unit['source_index'], unit, recovered))
    checks['reversible_markdown_edits'] = dict(body_edit_counts)

    source_results = []
    for source in manifest['sources']:
        alias = source['alias']
        seq = sorted(restored_by_source[alias], key=lambda x: x[0])
        require(len(seq) == source['sections'], alias + ': missing source section')
        require([v[0] for v in seq] == list(range(source['sections'])), alias + ': invalid source ordering')
        line = 1
        for _, unit, recovered in seq:
            require(unit['start_line'] == line, unit['anchor'] + ': gap/overlap in original lines')
            line = unit['end_line'] + 1
        recovered_text = ''.join(v[2] for v in seq)
        recovered_bytes = recovered_text.encode('utf-8')
        archived_bytes = (root / source['archive']).read_bytes()
        equal = recovered_bytes == archived_bytes and sha(recovered_bytes) == source['sha256']
        require(equal, alias + ': recovered source is not byte-identical to the original snapshot')
        require(sha(archived_bytes) == input_hashes[source['path']], alias + ': archive differs from input file hash')
        require(len(recovered_bytes) == source['bytes'], alias + ': original byte length differs')
        require(len(recovered_text.splitlines()) == source['lines'], alias + ': original line count differs')
        id_counts = Counter(ID_PATTERN.findall(recovered_text))
        require(dict(id_counts) == inventory[alias]['counts'], alias + ': ID spellings/counts differ')
        source_results.append({'alias': alias, 'source': source['path'], 'sections': len(seq), 'lines': source['lines'], 'bytes': len(recovered_bytes), 'sha256': sha(recovered_bytes), 'byte_identical_to_input': equal, 'id_occurrences': sum(id_counts.values())})
    checks['original_sources'] = source_results
    checks['original_lines'] = sum(x['lines'] for x in source_results)
    checks['original_bytes'] = sum(x['bytes'] for x in source_results)
    checks['requested_id_family_occurrences'] = sum(x['id_occurrences'] for x in source_results)

    ms_results = []
    for source in manifest['ms_sources']:
        raw = (root / source['path']).read_bytes()
        require(sha(raw) == source['sha256'], 'MS framework source changed: ' + source['path'])
        original_heads = [(level, title) for _, level, title in headings(raw.decode('utf-8')) if level in (2, 3)]
        mapped = [entry for entry in coverage if entry['source'] == source['path']]
        require([x['heading'] for x in mapped] == [t for _, t in original_heads], 'MS coverage heading sequence differs: ' + source['path'])
        book = mapped[0]['destination'].split('#')[0]
        output_heads = [(level, title) for _, level, title in headings(book_texts[book]) if level in (2, 3) and title != '章节导航']
        require(output_heads == original_heads, book + ': MS H2/H3 framework/order differs')
        for entry in mapped:
            fragment = entry['destination'].split('#', 1)[1]
            require(fragment in ANCHOR.findall(book_texts[book]), 'Missing MS heading anchor: ' + fragment)
        ms_results.append({'path': source['path'], 'headings': len(original_heads), 'sha256': sha(raw), 'heading_sequence_identical': original_heads == output_heads})
    checks['ms_frameworks'] = ms_results
    checks['ms_framework_headings'] = sum(x['headings'] for x in ms_results)

    anchor_cache = {}
    duplicate_anchors = []
    for path in (root / 'docs/plans').glob('*.md'):
        anchors = ANCHOR.findall(text(path))
        duplicates = [a for a, n in Counter(anchors).items() if n > 1]
        duplicate_anchors += [{'file': path.relative_to(root).as_posix(), 'anchor': a} for a in duplicates]
    require(not duplicate_anchors, 'Duplicate explicit anchors exist')
    checks['duplicate_explicit_anchors'] = duplicate_anchors

    def target_exists(current: str, target: str) -> bool:
        parsed = urlsplit(target)
        if parsed.scheme or parsed.netloc:
            return True
        path = (root / current).parent.joinpath(unquote(parsed.path)).resolve() if parsed.path else root / current
        if not path.exists():
            return False
        if not parsed.fragment:
            return True
        if path.suffix.lower() != '.md':
            return True
        if path not in anchor_cache:
            body = text(path)
            anchors = set(ANCHOR.findall(body))
            counts = Counter()
            for _, _, title in headings(body):
                base = slug(title)
                occurrence = counts[base]
                counts[base] += 1
                anchors.add(base + (f'-{occurrence}' if occurrence else ''))
            anchor_cache[path] = anchors
        return unquote(parsed.fragment) in anchor_cache[path]

    generated_targets = []
    for unit in sections:
        for edit in unit['edits']:
            if edit.get('target'):
                generated_targets.append((unit['destination'], edit['target']))
    for item in references['current_document_updates']:
        for edit in item['edits']:
            if edit.get('target'):
                generated_targets.append((item['path'], edit['target']))
    invalid_targets = sorted(set((p, t) for p, t in generated_targets if not target_exists(p, t)))
    require(not invalid_targets, 'New canonical reference targets are missing')
    checks['canonical_reference_occurrences'] = len(generated_targets)
    checks['canonical_reference_unique_targets_by_origin'] = len(set(generated_targets))
    checks['invalid_canonical_references'] = [{'source': p, 'target': t} for p, t in invalid_targets]

    navigation = {source['path'] for source in manifest['sources']}
    external = {item['path'] for item in references['current_document_updates']} | {'docs/README.md'}
    permitted_changes = navigation | external
    changed, unchanged, missing = [], [], []
    for rel, original_hash in input_hashes.items():
        path = root / rel
        if not path.is_file():
            missing.append(rel)
        elif sha(path.read_bytes()) == original_hash:
            unchanged.append(rel)
        else:
            changed.append(rel)
    require(not missing, 'Input files removed: ' + ', '.join(missing))
    unexpected = sorted(set(changed) - permitted_changes)
    require(not unexpected, 'Undeclared input modifications: ' + ', '.join(unexpected))
    protected = [p for p in input_hashes if any(part.startswith('.') for part in Path(p).parts) or 'archive' in Path(p).parts or 'validation' in Path(p).parts or 'history' in Path(p).name.lower()]
    require(all(p in unchanged for p in protected), 'Reference, history, archive or evidence inputs changed')
    checks['input_file_boundary'] = {'original_files': len(input_hashes), 'unchanged_files': len(unchanged), 'changed_files': changed, 'removed_files': missing, 'unexpected_changes': unexpected, 'protected_reference_history_evidence_files': len(protected)}
    checks['unresolved_original_text_reference_count'] = sum(len(u['unresolved_references']) for u in sections)
    checks['unresolved_original_link_fragment_count'] = len(references['unresolved_link_fragments'])
    return {'kind': 'document_integrity_only', 'scope': 'Source-body reconstruction, unchanged IDs, MS section skeleton, canonical links, and input-file change boundaries. Not implementation or runtime evidence.', 'errors': errors, 'checks': checks, 'implementation_or_runtime_tests_executed': False}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[3])
    parser.add_argument('--write', action='store_true', help='Write the document-integrity record; never alter source bodies.')
    args = parser.parse_args()
    try:
        report = verify(args.root)
    except (OSError, ValueError, KeyError, TypeError) as exc:
        print('Document integrity verification could not complete: ' + str(exc), file=sys.stderr)
        return 2
    if args.write:
        output = args.root / 'docs/plans/restructure/verification.json'
        output.write_bytes((json.dumps(report, ensure_ascii=False, indent=2) + '\n').encode('utf-8'))
    brief = {'kind': report['kind'], 'original_sources': len(report['checks']['original_sources']), 'original_lines': report['checks']['original_lines'], 'source_bodies': report['checks']['source_body_count'], 'ms_headings': report['checks']['ms_framework_headings'], 'id_occurrences': report['checks']['requested_id_family_occurrences'], 'canonical_reference_occurrences': report['checks']['canonical_reference_occurrences'], 'errors': report['errors'], 'implementation_or_runtime_tests_executed': False}
    print(json.dumps(brief, ensure_ascii=False, indent=2))
    return 1 if report['errors'] else 0


if __name__ == '__main__':
    raise SystemExit(main())
