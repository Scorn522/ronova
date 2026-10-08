import struct
import unittest
from normalize_pe_timestamp import normalized_timestamp


class NormalizeTimestampTest(unittest.TestCase):
    def image(self):
        data = bytearray(512)
        data[:2] = b'MZ'
        struct.pack_into('<I', data, 60, 64)
        data[64:68] = b'PE\0\0'
        struct.pack_into('<H', data, 68, 0x8664)
        struct.pack_into('<I', data, 72, 1234567)
        struct.pack_into('<HH', data, 84, 240, 0x2000)
        struct.pack_into('<H', data, 88, 0x20b)
        struct.pack_into('<I', data, 196, 16)
        data[400:412] = b'code-payload'
        return data

    def test_only_timestamp_changes_and_idempotent(self):
        original = bytes(self.image())
        result = normalized_timestamp(original)
        self.assertEqual(result[:72], original[:72])
        self.assertEqual(result[76:], original[76:])
        self.assertEqual(result[72:76], b'\0' * 4)
        self.assertEqual(normalized_timestamp(result), result)

    def test_reject_signed_and_checksummed(self):
        for offset in (152, 232, 236):
            data = self.image()
            struct.pack_into('<I', data, offset, 1)
            with self.subTest(offset=offset), self.assertRaises(ValueError):
                normalized_timestamp(bytes(data))

    def test_reject_truncated_or_wrong_format(self):
        for size in (0, 63, 80, 327):
            with self.subTest(size=size), self.assertRaises(ValueError):
                normalized_timestamp(bytes(self.image()[:size]))
        for offset in (0, 64, 68, 85, 88):
            data = self.image()
            data[offset] ^= 0xff
            with self.subTest(offset=offset), self.assertRaises(ValueError):
                normalized_timestamp(bytes(data))


if __name__ == '__main__':
    unittest.main()
