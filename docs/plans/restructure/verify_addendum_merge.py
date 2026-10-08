#!/usr/bin/env python3
"""Read-only preservation check for fix5 and declared followup handoff merges (Python 3.9+).

From the documentation/project root:
  python -B docs/plans/restructure/verify_addendum_merge.py --check
  python -B docs/plans/restructure/verify_addendum_merge.py --check --source-root PATH

The optional source root must be the supplied r66 source snapshot. No source,
ledger, permissions, generated file or verification record is modified. This
checks preservation and traceability, not the semantics of a design relation,
JVM capability grants, game behavior, or acceptance status.
"""
from __future__ import annotations
import argparse
from collections import Counter, defaultdict
import hashlib
import json
import re
from pathlib import Path
import sys
from urllib.parse import unquote, urlsplit
from zipfile import BadZipFile, ZipFile

from verify_restructure import headings, slug, ANCHOR


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def inside(root: Path, relative: str) -> Path:
    path = (root / relative).resolve()
    try:
        path.relative_to(root)
    except ValueError:
        raise ValueError('Path escapes the supplied root: ' + relative)
    return path



def verify_followups(root: Path, manifest: dict, source_root: Path | None) -> dict:
    """Preserve outgoing/incoming provenance separately; no design verdicts are inferred."""
    errors = []
    results = []
    def require(ok, msg):
        if not ok:
            errors.append(msg)
    anchor_cache = {}
    def target_ok(target):
        u = urlsplit(target)
        require(not u.scheme and not u.netloc, 'Unexpected external mapped target: ' + target)
        path = inside(root, unquote(u.path))
        if not path.is_file():
            errors.append('Missing mapped followup target: ' + target)
            return
        if u.fragment:
            if path not in anchor_cache:
                txt = path.read_text(encoding='utf-8')
                ids = set(ANCHOR.findall(txt)); seen = Counter()
                for _, _, title in headings(txt):
                    base = slug(title); n = seen[base]; seen[base] += 1
                    ids.add(base + ('-' + str(n) if n else ''))
                anchor_cache[path] = ids
            require(unquote(u.fragment) in anchor_cache[path], 'Missing followup anchor: ' + target)
    for entry in manifest.get('followup_handoffs', []):
        ar = entry['input_archive']; ap = inside(root, ar['preserved_path'])
        require(sha(ap.read_bytes()) == ar['sha256'], 'Followup input archive hash differs')
        grouped = defaultdict(list)
        for row in entry['section_mapping']:
            grouped[row['source_alias']].append(row)
        preserved = []
        with ZipFile(ap) as z:
            members = [i.filename for i in z.infolist() if not i.is_dir()]
            require(len(members) == len(set(members)), 'Duplicate followup ZIP names')
            require(set(members) == {a['archive_member'] for a in entry['sources']}, 'Followup source member set differs')
            for src in entry['sources']:
                raw = z.read(src['archive_member']); alias = src['alias']
                require(sha(raw) == src['input_sha256'] and len(raw) == src['input_bytes'], alias + ': original hash/size differs')
                require(inside(root, src['destination']).read_bytes() == src['prefix'].encode('utf-8') + raw,
                        alias + ': retained source or declared prefix differs')
                require('<!-- RONOVA-HANDOFF-ORIGINAL-' not in src['prefix'], alias + ': false external-original marker in prefix')
                txt = raw.decode('utf-8'); lines = txt.splitlines(keepends=True); rows = grouped[alias]
                require(len(lines) == src['input_lines'], alias + ': physical lines differ')
                require([(a['start_line'], a['heading_level'], a['source_heading']) for a in rows] == headings(txt), alias + ': heading partition differs')
                prev = 1; pieces = []
                for row in rows:
                    first, last = row['start_line'], row['end_line']
                    require(first == prev and first <= last <= len(lines), alias + ': gap/overlap in partition')
                    chunk = ''.join(lines[first - 1:last]).encode('utf-8'); pieces.append(chunk); prev = last + 1
                    require(sha(chunk) == row['original_segment_sha256'], alias + ': segment hash differs')
                    target_ok(row['retained_source']); target_ok(row['current_destination'])
                require(b''.join(pieces) == raw, alias + ': sections do not recover full input')
                preserved.append({'alias': alias, 'bytes': len(raw), 'lines': len(lines), 'sections': len(rows),
                                  'retained_body_byte_identical': inside(root, src['destination']).read_bytes() == src['prefix'].encode('utf-8') + raw})
        require(set(grouped) == {a['alias'] for a in entry['sources']}, 'Followup source aliases differ')
        decisions = entry['decisions']
        require(Counter(d['source_direction'] for d in decisions) == Counter(range(1, 10)), 'V1.3 decisions must contain each original direction once')
        require(all(d['decision'] in {'接受','修改','废弃'} for d in decisions), 'Unknown V1.3 decision label')
        for d in decisions:
            target_ok(d['current_destination'])
            require(not d['implementation_completed'] and not d['runtime_evidence_supplied'], 'V1.3 receipt is not runtime evidence')
        ev = entry['source_evidence']; human = inside(root, 'docs/reference/R66-V13-REVIEW-EVIDENCE.md').read_text(encoding='utf-8')
        for ex in ev['excerpts']:
            txt = ex['text']; rows = txt.split('\n')
            require(sha(txt.encode('utf-8')) == ex['normalized_text_sha256'], 'Followup excerpt hash differs: ' + ex['anchor'])
            require(len(rows) == ex['end_line'] - ex['start_line'] + 1, 'Followup excerpt range differs: ' + ex['anchor'])
            require('\n'.join(f'{n:4d}  {l}' for n,l in enumerate(rows, ex['start_line'])) in human,
                    'Human followup excerpt differs: ' + ex['anchor'])
        source_check = {'performed': False, 'reason': 'No --source-root; no claim of re-reading user source.'}
        if source_root is not None:
            sr = source_root.resolve(); texts = {}
            for files in ev['groups'].values():
                for rel, h in files.items():
                    raw = inside(sr, rel).read_bytes()
                    require(sha(raw) == h, 'V1.3 source snapshot differs: ' + rel)
                    texts[rel] = raw.decode('utf-8')
            import re
            for q in ev['searches']:
                got = [{'path': rel, 'line': n, 'text': line} for rel in sorted(ev['groups'][q['group']])
                       for n, line in enumerate(texts[rel].splitlines(), 1) if re.search(q['pattern'], line)]
                require(got == q['matches'] and len(got) == q['matching_lines'], 'Followup lexical search differs: ' + q['pattern'])
            for ex in ev['excerpts']:
                path = inside(sr, ex['path']); raw = path.read_bytes()
                require(sha(raw) == ex['file_sha256'], 'Followup source excerpt file differs: ' + ex['anchor'])
                got = '\n'.join(raw.decode('utf-8').splitlines()[ex['start_line'] - 1:ex['end_line']])
                require(got == ex['text'], 'Followup source excerpt differs: ' + ex['anchor'])
            source_check = {'performed': True, 'hashed_files': len(texts), 'searches': len(ev['searches']),
                            'excerpts': len(ev['excerpts']), 'semantic_call_graph_or_runtime_proven': False}
        results.append({'revision': entry['revision'], 'origin': entry['origin'], 'sources': preserved,
                        'sections': len(entry['section_mapping']), 'decisions_recorded': dict(Counter(d['decision'] for d in decisions)),
                        'source_check': source_check, 'semantic_decisions_machine_proven': False})
    return {'entries': results, 'errors': errors}

def surface_inventory(source_root: Path) -> dict:
    """Reproduce a lexical r66 inventory; never infer per-injection proofs or runtime status."""
    # Canonical record paths and ordering are platform-independent; content is UTF-8.
    # Do not suppress built artifacts: the input must still match the declared source ZIP.
    import re, ast, collections
    S = source_root
    J = 'src/main/java/dev/ronova/coremod/'
    def txt(relative):
        return inside(S, relative).read_bytes().decode('utf-8')
    java=sorted((S/'src/main/java/dev/ronova').rglob('*.java'), key=lambda p:p.relative_to(S).as_posix())
    native=sorted((p for p in (S/'native').rglob('*') if p.is_file() and p.suffix in {'.cpp','.hpp','.h','.cc','.cxx','.c'}), key=lambda p:p.relative_to(S).as_posix())
    other=[S/'native/build_code_seals.py',S/'native/code_seal_inventory.py',S/'src/main/resources/ronova.mixins.json']
    groups={name:{p.relative_to(S).as_posix():sha(p.read_bytes()) for p in ps} for name,ps in [('java',java),('native',native),('configuration',other)]}
    nt=txt('native/src/ronova_native.cpp')
    semantic=re.findall(r'RONOVA_SEMANTIC_TARGET\("([^"]+)"',nt)
    tasks=re.findall(r'"([^"]+)"',re.search(r'TASK_TARGETS\[\]\s*=\s*\{(.*?)\};',nt,re.S)[1])
    prefixes=next(ast.literal_eval(n.value) for n in ast.parse(txt('native/build_code_seals.py')).body if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='PREFIXES' for t in n.targets))
    config=json.loads(txt('src/main/resources/ronova.mixins.json'))
    rows=[]
    for p in sorted((S/(J+'mixin')).glob('*.java'), key=lambda p:p.relative_to(S).as_posix()):
     t=p.read_text(encoding="utf-8"); match=re.search(r'@Mixin\((.*?)\)',t,re.S)
     if not match: raise ValueError(p)
     arg=match[1]; imports={x.rsplit('.',1)[-1]:x for x in re.findall(r'^import\s+([\w.]+);',t,re.M)}
     token=re.search(r'([\w.]+)\.class',arg)
     if token:
      name=token[1];parts=name.split('.');owner=imports.get(parts[0]);
      if owner is None:
       wild=re.findall(r'^import\s+(net\.minecraft[\w.]*)\.\*;',t,re.M)
       if len(wild)!=1: raise ValueError('Unresolved import: '+name)
       owner=wild[0]+'.'+parts[0]
      target=owner.replace('.','/')+('' if len(parts)==1 else '$'+'$'.join(parts[1:]))
     else: target=re.search(r'"([^"]+)"',arg)[1].replace('.','/')
     counts={k:len(re.findall('@'+k+r'\b',t)) for k in ['Accessor','Inject','Redirect','Overwrite']}
     rows.append({'mixin':p.stem,'path':p.relative_to(S).as_posix(),'target':target,'in_native_mc_target_list':target in semantic,'side':'client' if p.stem in config['client'] else 'common','physical_lines':len(t.splitlines()),'annotations':counts,'redirect_lines':[i for i,line in enumerate(t.splitlines(),1) if '@Redirect' in line]})
    assert set(r['mixin'] for r in rows)==set(config['mixins']+config['client'])
    runtime=[]
    for p in java:
     t=p.read_text(encoding="utf-8");pkg=re.search(r'^package\s+([^;]+);',t,re.M)[1]
     for i,l in enumerate(t.splitlines(),1):
      if 'ServerRuntimeRegistry.runtime(' in l: runtime.append({'path':p.relative_to(S).as_posix(),'line':i,'package':pkg,'text':l})
    targets=sorted(set(r['target'] for r in rows));uncovered=sorted(set(targets)-set(semantic))
    policy_path=J+'policy/'
    policy_classes=[p.stem for p in java if p.relative_to(S).as_posix().startswith(policy_path)]
    metrics={'scope':'Lexical source inventory; annotation-name membership is not per-injection semantic proof or runtime lease.',
     'java_source_files':len(java),'java_physical_lines':sum(len(p.read_text(encoding="utf-8").splitlines()) for p in java),
     'mixin_sources':len(rows),'mixin_physical_lines':sum(r['physical_lines'] for r in rows),'mixin_targets':len(targets),'native_mc_targets':semantic,'native_jdk_task_targets':tasks,
     'targets_in_mc_list':len(set(targets)&set(semantic)),'targets_not_in_mc_list':uncovered,'annotation_counts':{k:sum(r['annotations'][k] for r in rows) for k in ['Accessor','Inject','Redirect','Overwrite']},
     'redirects_on_targets_not_in_mc_list':sum(r['annotations']['Redirect'] for r in rows if not r['in_native_mc_target_list']),
     'mixin_rows':rows,'runtime_qualified_call_lines':runtime,'runtime_call_package_counts':dict(sorted(collections.Counter(r['package'] for r in runtime).items())),
     'calls_in_registry_package':sum(r['package']=='dev.ronova.coremod.bootstrap' for r in runtime),
     'seal_prefixes':prefixes,'seal_prefix_count':len(prefixes),'policy_prefixes':[p for p in prefixes if '/policy/' in p],
     'policy_top_level_sources_not_selected':[x for x in policy_classes if J.replace('src/main/java/','')+'policy/'+x not in prefixes],
     'built_manifest_files_found':sorted(p.relative_to(S).as_posix() for p in S.rglob('*') if p.is_file() and p.name in {'manifest.json','code-seal-manifest.json','registered_code_images.hpp'}),
     'built_seal_class_count':None,'reported_built_seal_class_count':283,'built_count_origin':'User handoff; no built seal manifest supplied in the source ZIP.',
     'module_info_source_files':sorted(p.relative_to(S).as_posix() for p in S.rglob('module-info.java')),'runtime_module_graph_observed':False}
    return metrics

def verify_same_privilege(root: Path, manifest: dict, source_root: Path | None) -> dict:
    """Keep a read-only inventory distinct from proposal decisions and acceptance evidence."""
    import re
    errors, entries = [], []
    def require(ok, message):
        if not ok:
            errors.append(message)
    cache = {}
    def target_ok(target):
        u = urlsplit(target)
        if u.scheme or u.netloc:
            errors.append('Unexpected external surface mapping: ' + target)
            return
        path = inside(root, unquote(u.path))
        if not path.is_file():
            errors.append('Missing surface target: ' + target)
            return
        if u.fragment:
            if path not in cache:
                text = path.read_text(encoding='utf-8'); seen = Counter()
                ids = set(ANCHOR.findall(text))
                for _, _, title in headings(text):
                    base = slug(title); n = seen[base]; seen[base] += 1
                    ids.add(base + ('-' + str(n) if n else ''))
                cache[path] = ids
            require(unquote(u.fragment) in cache[path], 'Missing surface anchor: ' + target)
    if manifest.get('current_revision') == 'fix7-same-privilege-surface':
        require(len(manifest.get('same_privilege_handoffs', [])) == 1, 'fix7 requires its declared surface handoff')
    for entry in manifest.get('same_privilege_handoffs', []):
        ar = entry['input_archive']; path = inside(root, ar['preserved_path'])
        require(sha(path.read_bytes()) == ar['sha256'], 'Surface archive hash differs')
        require(len(path.read_bytes()) == ar['bytes'], 'Surface archive size differs')
        grouped = defaultdict(list)
        for row in entry['section_mapping']:
            grouped[row['source_alias']].append(row)
        source_results = []
        with ZipFile(path) as z:
            members = [i.filename for i in z.infolist() if not i.is_dir()]
            require(len(members) == len(set(members)), 'Duplicate surface ZIP names')
            require(set(members) == {a['archive_member'] for a in entry['sources']}, 'Surface member set differs')
            for src in entry['sources']:
                raw = z.read(src['archive_member']); alias = src['alias']
                require(sha(raw) == src['input_sha256'] and len(raw) == src['input_bytes'], alias + ': input differs')
                require(inside(root, src['destination']).read_bytes() == raw, alias + ': original was modified')
                text = raw.decode('utf-8'); lines = text.splitlines(keepends=True); rows = grouped[alias]
                require(len(lines) == src['input_lines'], alias + ': line count differs')
                require([(a['start_line'], a['heading_level'], a['source_heading']) for a in rows] == headings(text), alias + ': heading partition differs')
                prev, pieces = 1, []
                for row in rows:
                    first, last = row['start_line'], row['end_line']
                    require(first == prev and first <= last <= len(lines), alias + ': partition gap or overlap')
                    chunk = ''.join(lines[first-1:last]).encode('utf-8'); prev = last + 1; pieces.append(chunk)
                    require(sha(chunk) == row['original_segment_sha256'], alias + ': section hash differs')
                    target_ok(row['retained_source']); target_ok(row['current_destination'])
                require(b''.join(pieces) == raw, alias + ': source does not recover exactly')
                source_results.append({'alias': alias, 'bytes': len(raw), 'lines': len(lines), 'sections': len(rows), 'byte_identical': inside(root,src['destination']).read_bytes() == raw})
        require(set(grouped) == {s['alias'] for s in entry['sources']}, 'Surface aliases differ')
        require(Counter(d['source_number'] for d in entry['actions']) == Counter(range(1,9)), 'Surface actions must preserve original numbers 1 through 8 exactly once')
        for d in entry['actions']:
            target_ok(d['current_destination'])
            require(d['implementation_completed'] is False and d['runtime_evidence_supplied'] is False, 'Surface actions are not implementation/runtime evidence')
        require(entry['new_invariant_ids'] == [] and entry['new_roadmap_batches'] == [], 'Surface must not introduce new invariant IDs or batches')
        require(entry['production_source_modified'] is False and entry['validation_modified'] is False, 'Surface handoff is documentation only')
        ev = entry['source_evidence']; human = inside(root,entry['human_evidence_path']).read_text(encoding='utf-8')
        for ex in ev['excerpts']:
            rows = ex['text'].split('\n')
            require(sha(ex['text'].encode()) == ex['normalized_text_sha256'], 'Surface excerpt hash differs: ' + ex['anchor'])
            require(len(rows) == ex['end_line'] - ex['start_line'] + 1, 'Surface excerpt line range differs: ' + ex['anchor'])
            require('\n'.join(f'{n:4d}  {l}' for n,l in enumerate(rows,ex['start_line'])) in human, 'Human surface excerpt differs: ' + ex['anchor'])
        # The readable inventory table is derived from the recorded lexical data, not a protection claim.
        inv = ev['inventory']
        for target in sorted({r['target'] for r in inv['mixin_rows']}):
            rows = [r for r in inv['mixin_rows'] if r['target'] == target]
            expected = '| `' + target + '` | ' + ' / '.join('`'+r['mixin']+'`' for r in rows) + ' | ' + ','.join(sorted({r['side'] for r in rows})) + ' | ' + ('是' if target in inv['native_mc_targets'] else '否') + ' | ' + str(sum(r['annotations']['Redirect'] for r in rows)) + ' |'
            require(expected in human, 'Human surface target table differs: ' + target)
        for package, count in inv['runtime_call_package_counts'].items():
            require(f'| `{package}` | {count} |' in human, 'Human caller package count differs: ' + package)
        check = {'performed': False, 'reason': 'No --source-root; recorded facts not independently re-read.'}
        if source_root is not None:
            sr = source_root.resolve(); texts = {}
            for files in ev['groups'].values():
                for relative, h in files.items():
                    raw = inside(sr, relative).read_bytes()
                    require(sha(raw) == h, 'Surface source hash differs: ' + relative)
                    texts[relative] = raw.decode('utf-8')
            actual_java = {p.relative_to(sr).as_posix() for p in (sr/'src/main/java/dev/ronova').rglob('*.java')}
            actual_native = {p.relative_to(sr).as_posix() for p in (sr/'native').rglob('*') if p.is_file() and p.suffix in {'.cpp','.hpp','.h','.cc','.cxx','.c'}}
            require(actual_java == set(ev['groups']['java']), 'Surface Java corpus differs')
            require(actual_native == set(ev['groups']['native']), 'Surface native corpus differs')
            for q in ev['searches']:
                got = [{'path': rel, 'line': n, 'text': line} for rel in sorted(ev['groups'][q['group']]) for n,line in enumerate(texts[rel].splitlines(),1) if re.search(q['pattern'],line)]
                require(got == q['matches'] and len(got) == q['matching_lines'], 'Surface lexical search differs: ' + q['pattern'])
            for ex in ev['excerpts']:
                raw = inside(sr,ex['path']).read_bytes()
                require(sha(raw) == ex['file_sha256'], 'Surface excerpt file hash differs: ' + ex['anchor'])
                got = '\n'.join(raw.decode().splitlines()[ex['start_line']-1:ex['end_line']])
                require(got == ex['text'], 'Surface source excerpt differs: ' + ex['anchor'])
            require(surface_inventory(sr) == inv, 'Surface lexical inventory differs')
            check = {'performed': True, 'hashed_files': len(texts), 'lexical_searches':len(ev['searches']), 'excerpt_ranges':len(ev['excerpts']), 'mixin_inventory_recomputed':True, 'runtime_call_packages_recomputed':True, 'seal_prefixes_recomputed':True, 'semantic_or_runtime_proof':False}
        entries.append({'revision':entry['revision'], 'sources':source_results, 'source_sections':len(entry['section_mapping']), 'original_actions':len(entry['actions']), 'source_check':check, 'built_seal_class_count_independently_confirmed':False, 'semantic_findings_or_design_machine_proven':False})
    return {'entries':entries, 'errors':errors}

def verify_verification_ceiling_reports(root: Path, manifest: dict, source_root: Path | None) -> dict:
    """Preserve pre-fix7 reports and fix7 tool defect separately; no runtime verdicts."""
    import re
    errors, entries, anchor_cache = [], [], {}

    def require(ok, message):
        if not ok:
            errors.append(message)

    def target_ok(target):
        u = urlsplit(target)
        if u.scheme or u.netloc:
            errors.append('Unexpected external verification-report target: ' + target)
            return
        path = inside(root, unquote(u.path))
        if not path.is_file():
            errors.append('Missing verification-report target: ' + target)
            return
        if u.fragment:
            if path not in anchor_cache:
                txt = path.read_text(encoding='utf-8'); counts = Counter()
                anchors = set(ANCHOR.findall(txt))
                for _, _, title in headings(txt):
                    key = slug(title); n = counts[key]; counts[key] += 1
                    anchors.add(key + ('-' + str(n) if n else ''))
                anchor_cache[path] = anchors
            require(unquote(u.fragment) in anchor_cache[path], 'Missing verification-report anchor: ' + target)

    reports = manifest.get('verification_ceiling_handoffs', [])
    if manifest.get('current_revision') == 'fix8-verification-ceiling-portability':
        require(len(reports) == 1, 'fix8 requires its verification/ceiling/portability report handoff')
        require(len(manifest.get('same_privilege_handoffs', [])) == 1, 'fix8 retains the fix7 surface handoff')
        require(len(manifest.get('followup_handoffs', [])) == 1, 'fix8 retains the original V1.3 nine-direction handoff')
    for entry in reports:
        ar = entry['input_archive']; ap = inside(root, ar['preserved_path']); raw_archive = ap.read_bytes()
        require(sha(raw_archive) == ar['sha256'] and len(raw_archive) == ar['bytes'], 'Verification-report archive hash/size differs')
        grouped = defaultdict(list)
        for row in entry['section_mapping']:
            grouped[row['source_alias']].append(row)
        sources = []
        with ZipFile(ap) as z:
            members = [i.filename for i in z.infolist() if not i.is_dir()]
            require(len(members) == len(set(members)), 'Duplicate verification-report ZIP names')
            require(set(members) == {s['archive_member'] for s in entry['sources']}, 'Verification-report member set differs')
            for src in entry['sources']:
                raw = z.read(src['archive_member']); alias = src['alias']
                require(sha(raw) == src['input_sha256'] and len(raw) == src['input_bytes'], alias + ': original hash/size differs')
                require(inside(root, src['destination']).read_bytes() == raw, alias + ': retained report bytes differ')
                txt = raw.decode('utf-8'); lines = txt.splitlines(keepends=True); hs = headings(txt)
                require(bool(hs), alias + ': no report headings')
                expected = ([(1, 0, '来源前言')] if hs and hs[0][0] != 1 else []) + hs
                rows = grouped[alias]
                require(len(lines) == src['input_lines'], alias + ': physical lines differ')
                require([(r['start_line'], r['heading_level'], r['source_heading']) for r in rows] == expected, alias + ': partition differs')
                previous = 1; pieces = []
                for row in rows:
                    first, last = row['start_line'], row['end_line']
                    require(first == previous and first <= last <= len(lines), alias + ': report section gap/overlap')
                    chunk = ''.join(lines[first - 1:last]).encode('utf-8'); pieces.append(chunk); previous = last + 1
                    require(sha(chunk) == row['original_segment_sha256'], alias + ': section hash differs')
                    target_ok(row['retained_source']); target_ok(row['current_destination'])
                require(b''.join(pieces) == raw, alias + ': sections do not recover full report')
                sources.append({'alias':alias, 'bytes':len(raw), 'lines':len(lines), 'sections':len(rows), 'byte_identical':inside(root,src['destination']).read_bytes()==raw})
        require(set(grouped) == {s['alias'] for s in entry['sources']}, 'Verification-report source aliases differ')
        require(not entry['new_invariant_ids'] and not entry['new_roadmap_batches'], 'Report merge cannot silently create parallel IDs/batches')
        require(not entry['production_source_modified'] and not entry['validation_modified'], 'Report merge is not production implementation/acceptance')
        ev = entry['source_evidence']; human = inside(root,entry['human_evidence_path']).read_text(encoding='utf-8')
        for ex in ev['excerpts']:
            ls = ex['text'].split('\n')
            require(sha(ex['text'].encode('utf-8')) == ex['normalized_text_sha256'], 'Verification-report excerpt hash differs: ' + ex['anchor'])
            require(len(ls) == ex['end_line'] - ex['start_line'] + 1, 'Verification-report excerpt lines differ: ' + ex['anchor'])
            require('\n'.join(f'{n:4d}  {line}' for n,line in enumerate(ls,ex['start_line'])) in human, 'Verification-report human excerpt differs: ' + ex['anchor'])
        check = {'performed':False,'reason':'No --source-root; recorded source excerpts not independently re-read in this invocation.'}
        if source_root is not None:
            sr = source_root.resolve(); texts = {}
            actual = {p.relative_to(sr).as_posix() for p in (sr/'src/main/java/dev/ronova').rglob('*.java')}
            require(actual == set(ev['groups']['java']), 'Verification-report Java corpus differs from original r66 source ZIP')
            for rel,h in ev['groups']['java'].items():
                raw = inside(sr,rel).read_bytes(); require(sha(raw) == h, 'Verification-report source hash differs: ' + rel)
                texts[rel] = raw.decode('utf-8')
            for q in ev['searches']:
                got = [{'path':rel,'line':n,'text':line} for rel in sorted(ev['groups'][q['group']])
                       for n,line in enumerate(texts[rel].splitlines(),1) if re.search(q['pattern'],line)]
                require(got == q['matches'] and len(got) == q['matching_lines'], 'Verification-report lexical search differs: ' + q['name'])
            for ex in ev['excerpts']:
                raw = inside(sr,ex['path']).read_bytes()
                require(sha(raw) == ex['file_sha256'], 'Verification-report excerpt file differs: ' + ex['anchor'])
                got = '\n'.join(raw.decode('utf-8').splitlines()[ex['start_line']-1:ex['end_line']])
                require(got == ex['text'], 'Verification-report source excerpt differs: ' + ex['anchor'])
            check = {'performed':True,'java_source_files':len(texts),'lexical_searches':len(ev['searches']),'excerpt_ranges':len(ev['excerpts']), 'semantic_call_graph_or_runtime_proven':False}
        entries.append({'revision':entry['revision'],'sources':sources,'source_sections':len(entry['section_mapping']),'source_check':check, 'semantic_report_conclusions_machine_proven':False})
    return {'entries':entries,'errors':errors}



def verify_windows_rerun_receipts(root: Path, manifest: dict) -> dict:
    """Preserve user-provided Windows rerun receipts without treating them as local execution evidence."""
    errors = []
    entries = []
    def require(condition: bool, message: str) -> None:
        if not condition:
            errors.append(message)
    for entry in manifest.get('windows_rerun_receipts', []):
        arc = entry['input_archive']
        ap = inside(root, arc['preserved_path'])
        raw_archive = ap.read_bytes()
        require(sha(raw_archive) == arc['sha256'] and len(raw_archive) == arc['bytes'],
                'Windows rerun receipt archive hash/size differs')
        src = entry['source']
        with ZipFile(ap) as z:
            members = [i.filename for i in z.infolist() if not i.is_dir()]
            require(members == [src['archive_member']], 'Windows rerun receipt ZIP member set/order differs')
            raw = z.read(src['archive_member'])
        require(sha(raw) == src['input_sha256'] and len(raw) == src['input_bytes'],
                'Windows rerun receipt source hash/size differs')
        require(len(raw.decode('utf-8').splitlines(keepends=True)) == src['input_lines'],
                'Windows rerun receipt physical line count differs')
        require(inside(root, src['destination']).read_bytes() == raw,
                'Windows rerun receipt retained source bytes differ')
        require(entry['reported_source_archive_sha256'] == manifest['source_archive']['sha256'],
                'Windows rerun receipt reports a different r66 source archive identity')
        target = entry['current_destination']
        u = urlsplit(target)
        require(not u.scheme and not u.netloc, 'Unexpected external Windows rerun target: ' + target)
        tp = inside(root, unquote(u.path))
        require(tp.is_file(), 'Windows rerun target missing: ' + target)
        if tp.is_file() and u.fragment:
            text = tp.read_text(encoding='utf-8')
            ids = set(ANCHOR.findall(text))
            counts = Counter()
            for _, _, title in headings(text):
                base = slug(title); n = counts[base]; counts[base] += 1
                ids.add(base + ('-' + str(n) if n else ''))
            require(unquote(u.fragment) in ids, 'Windows rerun target anchor missing: ' + target)
        require(entry['closes_fix8_documentation_windows_rerun_pending'],
                'Windows rerun receipt must explicitly state its documentation-only closure')
        require(not entry['production_source_modified'] and not entry['validation_modified']
                and not entry['mod_or_acceptance_pass_created'],
                'Windows rerun receipt cannot silently claim production/validation/PASS changes')
        entries.append({
            'revision': entry['revision'],
            'source_bytes': len(raw),
            'source_lines': len(raw.decode('utf-8').splitlines(keepends=True)),
            'byte_identical': inside(root, src['destination']).read_bytes() == raw,
            'reported_native_windows_execution': bool(entry['reported_environment']['native_windows_execution']),
            'reported_portability_tests_executed': entry['reported_results']['portability_tests_executed'],
            'reported_portability_errors': entry['reported_results']['portability_errors'],
            'local_windows_execution_reperformed_by_this_script': False,
            'mod_or_acceptance_pass_created': False,
        })
    return {'entries': entries, 'errors': errors}

def verify_eight_direction_reports(root: Path, manifest: dict, source_root: Path | None) -> dict:
    """Check the outgoing report and exact excerpts; never infer runtime or design success."""
    errors, entries, anchor_cache = [], [], {}
    def require(ok, message):
        if not ok:
            errors.append('Eight-direction: ' + message)

    def target_ok(target):
        u = urlsplit(target)
        if u.scheme or u.netloc:
            errors.append('Eight-direction: unexpected external mapped target: ' + target)
            return
        path = inside(root, unquote(u.path))
        if not path.is_file():
            errors.append('Eight-direction: missing target: ' + target)
            return
        if u.fragment:
            if path not in anchor_cache:
                txt = path.read_text(encoding='utf-8'); counts = Counter()
                ids = set(ANCHOR.findall(txt))
                for _, _, title in headings(txt):
                    key = slug(title); n = counts[key]; counts[key] += 1
                    ids.add(key + ('-' + str(n) if n else ''))
                anchor_cache[path] = ids
            require(unquote(u.fragment) in anchor_cache[path], 'missing anchor: ' + target)

    reports = manifest.get('eight_direction_verification_handoffs', [])
    if manifest.get('current_revision') == 'fix10-eight-directions-verification':
        require(len(reports) == 1, 'fix10 requires its eight-direction report')
        for key in ('followup_handoffs', 'same_privilege_handoffs',
                    'verification_ceiling_handoffs', 'windows_rerun_receipts'):
            require(len(manifest.get(key, [])) == 1, 'previous handoff must remain: ' + key)
    for entry in reports:
        ar = entry['input_archive']; ap = inside(root, ar['preserved_path'])
        archive = ap.read_bytes()
        require(sha(archive) == ar['sha256'] and len(archive) == ar['bytes'], 'archive hash/size differs')
        grouped = defaultdict(list)
        for row in entry['section_mapping']:
            grouped[row['source_alias']].append(row)
        preserved = []
        with ZipFile(ap) as z:
            members = [i.filename for i in z.infolist() if not i.is_dir()]
            require(len(members) == len(set(members)), 'duplicate ZIP member names')
            require(set(members) == {s['archive_member'] for s in entry['sources']}, 'source member set differs')
            for src in entry['sources']:
                raw = z.read(src['archive_member']); alias = src['alias']
                require(sha(raw) == src['input_sha256'] and len(raw) == src['input_bytes'], alias + ': source hash/size differs')
                require(inside(root, src['destination']).read_bytes() == raw, alias + ': retained source bytes differ')
                text = raw.decode('utf-8'); lines = text.splitlines(keepends=True); rows = grouped[alias]
                require(len(lines) == src['input_lines'], alias + ': physical line count differs')
                require([(r['start_line'], r['heading_level'], r['source_heading']) for r in rows] == headings(text),
                        alias + ': heading partition differs')
                previous = 1; chunks = []
                for row in rows:
                    first, last = row['start_line'], row['end_line']
                    require(first == previous and first <= last <= len(lines), alias + ': gap/overlap in partition')
                    part = ''.join(lines[first - 1:last]).encode('utf-8'); chunks.append(part); previous = last + 1
                    require(sha(part) == row['original_segment_sha256'], alias + ': section hash differs')
                    target_ok(row['retained_source']); target_ok(row['current_destination'])
                require(b''.join(chunks) == raw, alias + ': sections do not recover the full source')
                preserved.append({'alias': alias, 'bytes': len(raw), 'lines': len(lines), 'sections': len(rows),
                                  'byte_identical': inside(root, src['destination']).read_bytes() == raw})
        require(set(grouped) == {s['alias'] for s in entry['sources']}, 'source aliases differ')
        require([d['id'] for d in entry['directions']] == ['S1', 'S2', 'S3', 'S4', 'A1', 'A2', 'A3', 'A4'],
                'source-local direction IDs/order differ')
        for direction in entry['directions']:
            target_ok(direction['current_destination'])
        for flag in ('new_invariant_ids', 'new_roadmap_batches', 'production_source_modified',
                     'validation_modified', 'semantic_mapping_machine_proven', 'local_runtime_tests_performed'):
            require(entry[flag] is False, 'unexpected implementation/proof claim: ' + flag)
        ev = entry['source_evidence']; human = inside(root, entry['human_evidence_path']).read_text(encoding='utf-8')
        for ex in ev['excerpts']:
            ls = ex['text'].split('\n')
            require(sha(ex['text'].encode('utf-8')) == ex['normalized_text_sha256'], 'excerpt digest differs: ' + ex['anchor'])
            require(len(ls) == ex['end_line'] - ex['start_line'] + 1, 'excerpt range differs: ' + ex['anchor'])
            require('\n'.join(f'{n:4d}  {line}' for n, line in enumerate(ls, ex['start_line'])) in human,
                    'human excerpt differs: ' + ex['anchor'])
            target_ok(entry['human_evidence_path'] + '#' + ex['anchor'])
        check = {'performed': False, 'reason': 'No --source-root; recorded excerpts are not independently re-read.'}
        if source_root is not None:
            sr = source_root.resolve(); source_files = set()
            for ex in ev['excerpts']:
                b = inside(sr, ex['path']).read_bytes(); source_files.add(ex['path'])
                require(sha(b) == ex['file_sha256'], 'source file hash differs: ' + ex['anchor'])
                got = '\n'.join(b.decode('utf-8').splitlines()[ex['start_line'] - 1:ex['end_line']])
                require(got == ex['text'], 'source excerpt differs: ' + ex['anchor'])
            check = {'performed': True, 'source_files_read': len(source_files), 'excerpt_ranges': len(ev['excerpts']),
                     'semantic_absence_or_runtime_behavior_proven': False}
        entries.append({'revision': entry['revision'], 'sources': preserved,
                        'source_sections': len(entry['section_mapping']), 'direction_rows': len(entry['directions']),
                        'source_check': check, 'report_conclusions_machine_proven': False,
                        'mod_or_acceptance_pass_created': False})
    return {'entries': entries, 'errors': errors}


def verify_existing_baseline_supplements(root: Path, manifest: dict, source_root: Path | None) -> dict:
    """Reuse exact-source checks; add bounded inventories, not semantic proofs."""
    errors = []
    reports = manifest.get('existing_baseline_supplement_handoffs', [])
    def require(ok, message):
        if not ok:
            errors.append('Existing-baseline: ' + message)
    if manifest.get('current_revision') == 'fix11-eight-directions-existing-baseline':
        require(len(reports) == 1, 'fix11 requires its outgoing supplement')
        for key in ('followup_handoffs', 'same_privilege_handoffs',
                    'verification_ceiling_handoffs', 'windows_rerun_receipts',
                    'eight_direction_verification_handoffs'):
            require(len(manifest.get(key, [])) == 1, 'previous handoff must remain: ' + key)
    if not reports:
        return {'entries': [], 'errors': errors}
    # The supplement preserves the same eight local IDs. Do not replace or mutate
    # the original fix10 report group in the caller's manifest.
    view = dict(manifest)
    view['eight_direction_verification_handoffs'] = reports
    view['current_revision'] = 'existing-baseline-source-check'
    result = verify_eight_direction_reports(root, view, source_root)
    errors.extend(result['errors'])
    expected_tokens = ['WriteSinkId', 'WriteSinkEvidence', 'WriteSinkExtension',
                       'CompletionContractId', 'CompletionContractExtension', 'CompletionEvaluation']
    for entry, summary in zip(reports, result['entries']):
        inventories = entry['subsystem_inventory']
        directories = [x['directory'] for x in inventories]
        require(len(directories) == len(set(directories)) and bool(directories), 'duplicate/empty subsystem list')
        for inventory in inventories:
            directory = inventory['directory']
            inside(root, directory)  # Confinement only; documentation need not contain production files.
            rows = inventory['files']
            paths = [row['path'] for row in rows]
            require(paths == sorted(set(paths)), 'unsorted/duplicate inventory paths: ' + directory)
            for row in rows:
                require(row['path'].startswith(directory + '/') and row['path'].endswith('.java'),
                        'inventory path outside declared scope: ' + row['path'])
                require(bool(re.fullmatch('[0-9a-f]{64}', row['sha256'])), 'invalid file digest: ' + row['path'])
            if source_root is not None:
                base = inside(source_root.resolve(), directory)
                files = base.rglob('*.java') if inventory['recursive'] else base.glob('*.java')
                actual = []
                for file in sorted(files, key=lambda item: item.relative_to(source_root.resolve()).as_posix()):
                    raw = file.read_bytes(); text = raw.decode('utf-8')
                    doc = re.search(r'/\*\*.*?\*/', text, re.S)
                    actual.append({'path': file.relative_to(source_root.resolve()).as_posix(),
                                   'sha256': sha(raw), 'lines': len(text.splitlines()),
                                   'first_javadoc': re.sub(r'\s+', ' ', doc.group()).strip() if doc else None})
                require(actual == rows, 'source subsystem inventory differs: ' + directory)
        scan = entry['scoped_reference_scan']
        require(scan['directory'] == 'src/main/java' and scan['recursive'] is True,
                'reference scan scope differs')
        require(scan['tokens'] == expected_tokens, 'reference scan token set differs')
        require(scan['semantic_absence_proven'] is False, 'scan must not claim semantic absence')
        if source_root is not None:
            actual = []
            for file in sorted(inside(source_root.resolve(), scan['directory']).rglob('*.java'),
                               key=lambda item: item.relative_to(source_root.resolve()).as_posix()):
                for line_number, line in enumerate(file.read_text(encoding='utf-8').splitlines(), 1):
                    if any(re.search(r'\b' + re.escape(token) + r'\b', line) for token in expected_tokens):
                        actual.append({'path': file.relative_to(source_root.resolve()).as_posix(),
                                       'line': line_number, 'text': line})
            require(actual == scan['hits'], 'scoped source references differ')
        summary.update({'subsystem_inventories': len(inventories),
                        'inventory_files': sum(len(x['files']) for x in inventories),
                        'scoped_reference_lines': len(scan['hits']),
                        'inventories_compared_to_source': source_root is not None,
                        'old_fix10_decisions_changed': False,
                        'semantic_or_runtime_completeness_proven': False})
    return {'entries': result['entries'], 'errors': errors}


def verify(root: Path, source_root: Path | None = None) -> dict:
    root = root.resolve()
    manifest = json.loads((root / 'docs/plans/restructure/enhancement-addendum-merge.json').read_text(encoding='utf-8'))
    errors = []
    def require(condition: bool, message: str) -> None:
        if not condition:
            errors.append(message)

    arcinfo = manifest['input_archive']
    arcpath = inside(root, arcinfo['preserved_path'])
    raw_archive = arcpath.read_bytes()
    require(sha(raw_archive) == arcinfo['sha256'], 'Input archive hash differs')
    grouped = defaultdict(list)
    for section in manifest['section_mapping']:
        grouped[section['source_alias']].append(section)
    preserved = []
    targets = []
    with ZipFile(arcpath) as z:
        members = [i.filename for i in z.infolist() if not i.is_dir()]
        require(len(members) == len(set(members)), 'Duplicate ZIP member names')
        require(set(members) == {s['archive_member'] for s in manifest['sources']}, 'Source member set differs')
        for source in manifest['sources']:
            alias = source['alias']
            raw = z.read(source['archive_member'])
            require(sha(raw) == source['input_sha256'], alias + ': source hash mismatch')
            require(len(raw) == source['input_bytes'], alias + ': source byte length differs')
            body = inside(root, source['destination']).read_bytes()
            begin = source['begin_marker'].encode('utf-8')
            end = source['end_marker'].encode('utf-8')
            require(body.count(begin) == 1 and body.count(end) == 1, alias + ': invalid wrapper markers')
            start = body.find(begin) + len(begin)
            stop = body.find(end, start)
            require(stop >= start and body[start:stop] == raw, alias + ': retained body is not byte-identical')
            text = raw.decode('utf-8')
            lines = text.splitlines(keepends=True)
            require(len(lines) == source['input_lines'], alias + ': source physical line count differs')
            seq = grouped[alias]
            expected_heads = headings(text)
            require([(s['start_line'], s['heading_level'], s['source_heading']) for s in seq] == expected_heads,
                    alias + ': source heading partition differs')
            previous = 1
            pieces = []
            for section in seq:
                first, last = section['start_line'], section['end_line']
                require(first == previous and first <= last <= len(lines), alias + ': gap/overlap in section partition')
                piece = ''.join(lines[first - 1:last]).encode('utf-8')
                require(sha(piece) == section['original_segment_sha256'], alias + ': source section hash differs')
                previous = last + 1
                pieces.append(piece)
                targets.extend([section['retained_source'], section['current_destination']])
            require(b''.join(pieces) == raw, alias + ': source sections do not reconstruct the original')
            preserved.append({'alias': alias, 'original_bytes': len(raw), 'original_lines': len(lines),
                              'original_sha256': sha(raw), 'sections': len(seq),
                              'retained_body_byte_identical': stop >= start and body[start:stop] == raw})
    require(set(grouped) == {s['alias'] for s in manifest['sources']}, 'Unknown source aliases in section mapping')

    anchors = {}
    for target in sorted(set(targets)):
        url = urlsplit(target)
        require(not url.scheme and not url.netloc, 'Unexpected external current target: ' + target)
        path = inside(root, unquote(url.path))
        if not path.is_file():
            errors.append('Mapped target is missing: ' + target)
            continue
        if url.fragment:
            if path not in anchors:
                text = path.read_text(encoding='utf-8')
                ids = set(ANCHOR.findall(text))
                counts = Counter()
                for _, _, title in headings(text):
                    base = slug(title)
                    occurrence = counts[base]
                    counts[base] += 1
                    ids.add(base + ('-' + str(occurrence) if occurrence else ''))
                anchors[path] = ids
            require(unquote(url.fragment) in anchors[path], 'Mapped anchor is missing: ' + target)

    evidence = (root / 'docs/reference/R66-NATIVE-NEGOTIATION-EVIDENCE.md').read_text(encoding='utf-8')
    for excerpt in manifest['native_excerpts']:
        raw_lines = excerpt['text'].split('\n')
        require(sha(excerpt['text'].encode()) == excerpt['normalized_text_sha256'], 'Recorded excerpt hash differs')
        require(len(raw_lines) == excerpt['end_line'] - excerpt['start_line'] + 1, 'Recorded excerpt line count differs')
        rendered = '\n'.join(f'{i:4d}  {line}' for i, line in enumerate(raw_lines, excerpt['start_line']))
        require(rendered in evidence, 'Human source excerpt differs: ' + excerpt['anchor'])

    source_check = {'performed': False, 'reason': 'No --source-root provided; recorded source facts not independently re-read in this invocation.'}
    if source_root is not None:
        source_root = source_root.resolve()
        files = manifest['native_scan']['files']
        texts = {}
        for rel, expected_hash in files.items():
            raw = inside(source_root, rel).read_bytes()
            require(sha(raw) == expected_hash, 'r66 source snapshot differs: ' + rel)
            texts[rel] = raw.decode('utf-8')
        actual_set = {p.relative_to(source_root).as_posix() for p in (source_root / 'native').rglob('*')
                      if p.is_file() and p.suffix.lower() in {'.cpp', '.hpp', '.h', '.c', '.cc', '.cxx'}}
        require(actual_set == set(files), 'Native source scan file set differs from recorded r66')
        for symbol, recorded in manifest['native_scan']['queries'].items():
            matches = [{'path': rel, 'line': n, 'count': line.count(symbol), 'text': line}
                       for rel in sorted(texts) for n, line in enumerate(texts[rel].splitlines(), 1) if symbol in line]
            require(matches == recorded['matches'], 'Native text search differs: ' + symbol)
            require(len(matches) == recorded['matching_lines'] and sum(x['count'] for x in matches) == recorded['text_occurrences'],
                    'Recorded native counts differ: ' + symbol)
        for excerpt in manifest['native_excerpts']:
            part = '\n'.join(texts[excerpt['source']].splitlines()[excerpt['start_line'] - 1:excerpt['end_line']])
            require(part == excerpt['text'], 'Source excerpt differs from supplied r66: ' + excerpt['anchor'])
        source_check = {'performed': True, 'native_source_files': len(files), 'excerpt_ranges': len(manifest['native_excerpts']),
                        'method': 'Exact r66 hashes, lexical searches, and line excerpts; not a semantic call graph or runtime test.'}
    followups = verify_followups(root, manifest, source_root)
    errors.extend(followups['errors'])
    surfaces = verify_same_privilege(root, manifest, source_root)
    errors.extend(surfaces['errors'])
    reports = verify_verification_ceiling_reports(root, manifest, source_root)
    errors.extend(reports['errors'])
    windows_receipts = verify_windows_rerun_receipts(root, manifest)
    errors.extend(windows_receipts['errors'])
    eight_reports = verify_eight_direction_reports(root, manifest, source_root)
    errors.extend(eight_reports['errors'])
    baseline_reports = verify_existing_baseline_supplements(root, manifest, source_root)
    errors.extend(baseline_reports['errors'])
    return {'kind': 'documentation_addendum_preservation_only', 'revision': manifest.get('current_revision', manifest['revision']), 'sources': preserved,
            'followup_handoffs': followups['entries'],
            'same_privilege_handoffs': surfaces['entries'],
            'verification_ceiling_handoffs': reports['entries'],
            'windows_rerun_receipts': windows_receipts['entries'],
            'eight_direction_verification_handoffs': eight_reports['entries'],
            'existing_baseline_supplement_handoffs': baseline_reports['entries'],
            'source_section_count': len(manifest['section_mapping']), 'mapped_target_occurrences': len(targets),
            'native_source_check': source_check, 'external_references_rechecked_by_this_script': False,
            'semantic_mapping_machine_proven': False, 'implementation_or_runtime_tests_executed': False,
            'validation_modified': False, 'errors': errors}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[3])
    parser.add_argument('--check', action='store_true', help='Explicit read-only mode (also the default).')
    parser.add_argument('--source-root', type=Path, help='Optional supplied r66 source snapshot, read-only.')
    args = parser.parse_args()
    try:
        report = verify(args.root, args.source_root)
    except (OSError, ValueError, KeyError, TypeError, BadZipFile) as exc:
        print(json.dumps({'kind': 'documentation_addendum_preservation_only', 'errors': [str(exc)]}, ensure_ascii=False, indent=2))
        return 2
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 1 if report['errors'] else 0


if __name__ == '__main__':
    raise SystemExit(main())
