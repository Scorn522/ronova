"""Run at the stage boundary: python -m unittest discover -s native/tests."""
import io
from pathlib import Path
import sys
import unittest
import warnings
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from code_seal_inventory import MAX_CLASSES, MAX_TOTAL_BYTES, select_registered_images

IMAGE = b'\xca\xfe\xba\xbe\x00\x00\x00\x3d'


class CodeSealInventoryTests(unittest.TestCase):
    def select(self, entries, prefixes=('example/Required',)):
        buffer = io.BytesIO()
        with warnings.catch_warnings():
            warnings.simplefilter('ignore', UserWarning)
            with zipfile.ZipFile(buffer, 'w') as archive:
                for name, data in entries:
                    archive.writestr(name, data)
        with zipfile.ZipFile(buffer) as archive:
            return select_registered_images(archive, prefixes)

    def test_complete_family_includes_observation_nested_types(self):
        entries = [('example/Required$Observation$State.class', IMAGE),
            ('example/Required.class', IMAGE), ('example/Unrelated.class', IMAGE),
            ('example/Required$Observation.class', IMAGE)]
        images = self.select(entries)
        self.assertEqual(list(images), ['example/Required', 'example/Required$Observation',
            'example/Required$Observation$State'])

    def test_missing_top_level_is_not_satisfied_by_nested_class(self):
        with self.assertRaisesRegex(ValueError, 'Missing required'):
            self.select([('example/Required$Observation.class', IMAGE)])

    def test_missing_one_required_family_rejects_partial_inventory(self):
        with self.assertRaisesRegex(ValueError, 'Missing required.*Other'):
            self.select([('example/Required.class', IMAGE)], ('example/Required', 'example/Other'))

    def test_duplicate_class_is_not_silently_overwritten(self):
        with self.assertRaisesRegex(ValueError, 'Duplicate registered'):
            self.select([('example/Required.class', IMAGE), ('example/Required.class', IMAGE)])

    def test_class_budget_includes_nested_types(self):
        entries = [('example/Required.class', IMAGE)] + [
            (f'example/Required$Nested{i}.class', IMAGE) for i in range(MAX_CLASSES - 1)]
        self.assertEqual(len(self.select(entries)), MAX_CLASSES)
        with self.assertRaisesRegex(ValueError, 'exceeds its budget'):
            self.select(entries + [('example/Required$Overflow.class', IMAGE)])

    def test_byte_budget_has_no_truncation_fallback(self):
        with self.assertRaisesRegex(ValueError, 'exceeds its budget'):
            self.select([('example/Required.class', IMAGE + bytes(MAX_TOTAL_BYTES))])

    def test_invalid_image_cannot_be_sealed(self):
        with self.assertRaisesRegex(ValueError, 'Invalid registered class header'):
            self.select([('example/Required.class', b'not a class')])

    def test_empty_or_duplicate_prefix_configuration_rejected(self):
        for prefixes in [(), ('example/Required', 'example/Required')]:
            with self.assertRaisesRegex(ValueError, 'nonempty unique'):
                self.select([('example/Required.class', IMAGE)], prefixes)


if __name__ == '__main__':
    unittest.main()
