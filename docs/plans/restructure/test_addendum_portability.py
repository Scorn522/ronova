#!/usr/bin/env python3
"""Read-only documentation-verifier regressions, not Mod/JVM acceptance.

  python -B docs/plans/restructure/test_addendum_portability.py --source-root ORIGINAL_R66

Uses standard-library pure-path spelling adapters over real, read-only files.
No chmod, subprocess, source writes, generated ledger or native Windows claim.
Without --source-root only the path/syntax checks run; source tests are skipped.
"""
from __future__ import annotations
import argparse
import ast
import copy
import inspect
import json
from pathlib import Path, PurePosixPath, PureWindowsPath
import sys
sys.dont_write_bytecode = True
import verify_addendum_merge as verifier


class PathSpellingView:
    """File I/O stays on the host; relative path strings have the chosen flavor.

    This is deliberately NOT a Windows filesystem/permission/locale emulator.
    The strict text-read guard makes locale-dependent source reads fail in tests.
    Optional virtual names exercise manifest/module paths without writing files.
    """
    def __init__(self, path, root, windows=False, virtual_names=(), virtual=False):
        self.path = Path(path)
        self.root = Path(root)
        self.windows = windows
        self.virtual_names = tuple(virtual_names)
        self.virtual = virtual

    def view(self, path, virtual=False):
        return type(self)(path, self.root, self.windows, self.virtual_names, virtual)

    def __truediv__(self, other):
        return self.view(self.path / other)

    def __str__(self):
        return str(self.path)

    def __lt__(self, other):
        return self.relative_to(self.root) < other.relative_to(other.root)

    def resolve(self):
        return self.view(self.path.resolve(), self.virtual)

    def relative_to(self, other):
        base = other.path if isinstance(other, PathSpellingView) else other
        relative = self.path.relative_to(base).as_posix()
        return PureWindowsPath(relative) if self.windows else PurePosixPath(relative)

    def rglob(self, pattern):
        for path in self.path.rglob(pattern):
            yield self.view(path)
        if self.path == self.root:
            for name in self.virtual_names:
                if pattern == '*' or (pattern == 'module-info.java' and Path(name).name == pattern):
                    yield self.view(self.root / name, virtual=True)

    def glob(self, pattern):
        for path in self.path.glob(pattern):
            yield self.view(path)

    def is_file(self):
        return self.virtual or self.path.is_file()

    def read_bytes(self):
        if self.virtual:
            raise AssertionError('The inventory must not read a virtual build manifest')
        return self.path.read_bytes()

    def read_text(self, encoding=None, **kwargs):
        if encoding != 'utf-8':
            raise AssertionError('Source text must explicitly declare UTF-8')
        return self.path.read_text(encoding=encoding, **kwargs)

    @property
    def suffix(self):
        return self.path.suffix

    @property
    def name(self):
        return self.path.name

    @property
    def stem(self):
        return self.path.stem


class NativeSeparatorMutation(ast.NodeTransformer):
    """Reintroduce the reported separator mistake in memory, never on disk."""
    def visit_Call(self, node):
        node = self.generic_visit(node)
        if (isinstance(node.func, ast.Attribute) and node.func.attr == 'as_posix'
                and isinstance(node.func.value, ast.Call)
                and isinstance(node.func.value.func, ast.Attribute)
                and node.func.value.func.attr == 'relative_to'):
            return ast.copy_location(ast.Call(func=ast.Name(id='str', ctx=ast.Load()),
                                             args=[node.func.value], keywords=[]), node)
        return node


def run(root: Path, source_root: Path | None = None):
    root = root.resolve()
    results, errors, skipped = [], [], []

    def test(name, action):
        try:
            detail = action()
            results.append({'name': name, 'ok': True, 'detail': detail})
        except Exception as exc:
            errors.append(name + ': ' + str(exc))
            results.append({'name': name, 'ok': False, 'detail': str(exc)})

    def require(ok, msg):
        if not ok:
            raise AssertionError(msg)

    def pure_paths():
        names = ['src/main/java/dev/ronova/coremod/policy/StrengthPolicySnapshot.java',
                 'build/字母/manifest.json', 'nested/module-info.java']
        for name in names:
            require(PureWindowsPath(name).as_posix() == PurePosixPath(name).as_posix(), name)
        require(not str(PureWindowsPath(names[0])).startswith('src/main/java/'),
                'The Windows-spelling negative fixture is not effective')
        return {'path_examples':len(names),'pure_windows_not_native_windows':True}

    def syntax_contract():
        tree = ast.parse(inspect.getsource(verifier.surface_inventory))
        for node in ast.walk(tree):
            if not isinstance(node, ast.Call):
                continue
            if (isinstance(node.func, ast.Name) and node.func.id == 'str'
                    and node.args and isinstance(node.args[0], ast.Call)
                    and isinstance(node.args[0].func, ast.Attribute)
                    and node.args[0].func.attr == 'relative_to'):
                raise AssertionError('Platform-dependent relative path serialization remains')
            if isinstance(node.func, ast.Attribute) and node.func.attr == 'read_text':
                require(any(k.arg == 'encoding' and isinstance(k.value, ast.Constant)
                            and k.value.value == 'utf-8' for k in node.keywords),
                        'A source text read has no explicit UTF-8 encoding')
        return 'All relative records use canonical paths; source text reads declare UTF-8.'

    test('pure_path_canonicalization', pure_paths)
    test('inventory_path_and_encoding_contract', syntax_contract)
    if source_root is None:
        skipped.append('No --source-root: real source inventory, separator mutation and baseline rejection not executed.')
    else:
        sr = source_root.resolve()
        manifest = json.loads((root/'docs/plans/restructure/enhancement-addendum-merge.json').read_text(encoding='utf-8'))
        entry = manifest['same_privilege_handoffs'][0]
        expected = entry['source_evidence']['inventory']

        def compare(flavor):
            actual = verifier.surface_inventory(PathSpellingView(sr,sr,windows=flavor))
            require(actual == expected, 'Canonical inventory differs from retained fix7 record')
            require(len(actual['policy_top_level_sources_not_selected']) == 26,
                    'The reported policy filter regression is not covered')
            return {'record_unchanged':True,'unselected_policy_sources':26,
                    'source_reads_explicit_utf8':True,'path_flavor':'windows' if flavor else 'posix'}
        test('posix_spelling_matches_retained_inventory', lambda:compare(False))
        test('windows_spelling_matches_retained_inventory', lambda:compare(True))

        def mutation():
            tree = NativeSeparatorMutation().visit(ast.parse(inspect.getsource(verifier.surface_inventory)))
            ast.fix_missing_locations(tree)
            namespace = dict(vars(verifier))
            exec(compile(tree,'<separator-regression-mutant>','exec'),namespace)
            actual = namespace['surface_inventory'](PathSpellingView(sr,sr,windows=True))
            require(actual != expected, 'The negative mutation was not detected')
            require(actual['policy_top_level_sources_not_selected'] == [], 'Windows prefix filter did not fail as reported')
            require(actual['mixin_rows'] != expected['mixin_rows'], 'Mixin path mismatch not reproduced')
            require(actual['runtime_qualified_call_lines'] != expected['runtime_qualified_call_lines'], 'Runtime path mismatch not reproduced')
            return 'Separator-only mutation reproduces empty policy selection plus two path mismatches; no file was changed.'
        test('separator_regression_reproduced_in_memory',mutation)

        def nonempty_fields():
            names = ('build/字母/manifest.json','build/Z/registered_code_images.hpp','nested/module-info.java')
            actual = verifier.surface_inventory(PathSpellingView(sr,sr,windows=True,virtual_names=names))
            require(actual['built_manifest_files_found'] == sorted(names[:2]), 'Nonempty build paths not canonical/deterministic')
            require(actual['module_info_source_files'] == [names[2]], 'Nonempty module path not canonical')
            require(actual != expected, 'Build/module additions were silently ignored')
            return 'Virtual filenames only; build/module additions remain visible and do not match the original source snapshot.'
        test('nonempty_build_and_module_fields_are_not_suppressed',nonempty_fields)

        def wrong_record():
            altered = copy.deepcopy(manifest)
            altered['same_privilege_handoffs'][0]['source_evidence']['inventory']['built_manifest_files_found'] = ['build/unexpected/manifest.json']
            result = verifier.verify_same_privilege(root,altered,sr)
            require('Surface lexical inventory differs' in result['errors'], 'The verifier no longer rejects mismatched build inventory')
            return 'A changed expected build inventory is still rejected; no record or source was written.'
        test('mismatched_baseline_still_rejected',wrong_record)
    return {'kind':'documentation_verifier_portability_regression_only','tests':results,
            'tests_executed':len(results),'skipped':skipped,'errors':errors,
            'host_platform':sys.platform,'native_windows_execution':sys.platform=='win32',
            'windows_adapter_scope':'Relative spelling/sorting and explicit UTF-8 guard over host files; not Windows OS execution.',
            'file_or_permission_writes':False,'subprocess_started':False,
            'mod_jvm_or_acceptance_executed':False}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[3])
    parser.add_argument('--source-root',type=Path)
    args = parser.parse_args()
    try:
        report = run(args.root,args.source_root)
    except (OSError,ValueError,KeyError,TypeError) as exc:
        print(json.dumps({'kind':'documentation_verifier_portability_regression_only','errors':[str(exc)]},ensure_ascii=False,indent=2))
        return 2
    print(json.dumps(report,ensure_ascii=False,indent=2))
    return 1 if report['errors'] else 0


if __name__ == '__main__':
    raise SystemExit(main())
