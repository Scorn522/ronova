#!/usr/bin/env python3
"""Documentation-helper tests only. Synthetic JSON is temporary and is never acceptance evidence."""
import json
from pathlib import Path
import tempfile
import unittest
from generate_validation_traceability import EXPECTED_IDS, inspect_ledger


class LedgerReaderTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.path = Path(self.tmp.name) / 'synthetic-reader-input.json'

    def tearDown(self):
        self.tmp.cleanup()

    def write(self, obj):
        self.path.write_bytes(json.dumps(obj).encode('utf-8'))

    def test_missing_is_not_not_run(self):
        r = inspect_ledger(self.path, None, None, None)
        self.assertEqual(r['state'], 'INPUT_MISSING')
        self.assertFalse(r['per_case_status_read'])
        self.assertNotIn('matched_original_status_counts', r)

    def test_existing_without_schema_stays_unparsed(self):
        self.write({'arbitrary': 'synthetic'})
        self.assertEqual(inspect_ledger(self.path, None, None, None)['state'], 'PRESENT_UNPARSED')

    def test_explicit_array_preserves_status_and_bytes(self):
        self.write({'items': [{'case': 'B01', 'state': 'SYNTHETIC_UNRECOGNIZED_STATUS'}]})
        before = self.path.read_bytes()
        r = inspect_ledger(self.path, '/items', 'case', 'state')
        self.assertEqual(r['rows'][0]['original_status'], 'SYNTHETIC_UNRECOGNIZED_STATUS')
        self.assertEqual(r['missing_ids'], EXPECTED_IDS[1:])
        self.assertFalse(r['exact_68_unique_ids'])
        self.assertEqual(before, self.path.read_bytes())

    def test_explicit_object_key_ids(self):
        self.write({'cases': {'X68': {'state': 'SYNTHETIC_ONLY'}}})
        r = inspect_ledger(self.path, '/cases', '@key', 'state')
        self.assertEqual(r['rows'][0]['id'], 'X68')
        self.assertFalse(r['runtime_evidence_verified'])

    def test_duplicate_and_unknown_ids_are_not_dropped(self):
        self.write([{'id': 'B01', 'state': 'SYNTHETIC_ONLY'}, {'id': 'B01', 'state': 'SYNTHETIC_OTHER'}, {'id': 'OTHER-SOURCE:B01', 'state': 'SYNTHETIC_ONLY'}])
        r = inspect_ledger(self.path, '', 'id', 'state')
        self.assertEqual(r['duplicate_ids'], ['B01'])
        self.assertEqual(r['extra_ids'], ['OTHER-SOURCE:B01'])
        self.assertEqual(len(r['rows']), 3)
        self.assertEqual(sum(r['matched_original_status_counts'].values()), 2)

    def test_partial_schema_is_rejected_even_if_file_missing(self):
        with self.assertRaises(ValueError):
            inspect_ledger(self.path, '', None, 'state')

    def test_wrong_field_is_not_guessed(self):
        self.write([{'id': 'B01', 'real_state': 'SYNTHETIC_ONLY'}])
        with self.assertRaises(ValueError):
            inspect_ledger(self.path, '', 'id', 'state')

    def test_key_identity_is_not_array_index(self):
        self.write([{'state': 'SYNTHETIC_ONLY'}])
        with self.assertRaises(ValueError):
            inspect_ledger(self.path, '', '@key', 'state')

    def test_json_pointer_escaping(self):
        self.write({'a/b~c': [{'id': 'X24', 'state': 'SYNTHETIC_ONLY'}]})
        self.assertEqual(inspect_ledger(self.path, '/a~1b~0c', 'id', 'state')['rows'][0]['id'], 'X24')

    def test_invalid_pointer_fails_explicitly(self):
        self.write({'items': []})
        for pointer in ['items', '/absent', '/bad~x', '/items/01', '/items/0']:
            with self.assertRaises(ValueError):
                inspect_ledger(self.path, pointer, 'id', 'state')


if __name__ == '__main__':
    unittest.main()
