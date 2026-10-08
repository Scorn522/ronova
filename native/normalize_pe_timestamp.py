"""Normalize only the timestamp of our newly linked, unsigned x64 DLL before packaging."""
from pathlib import Path
import struct
import sys


def normalized_timestamp(image: bytes) -> bytes:
    if len(image) < 64 or image[:2] != b'MZ':
        raise ValueError('not a PE image')
    pe = struct.unpack_from('<I', image, 60)[0]
    if pe < 64 or pe + 24 > len(image) or image[pe:pe + 4] != b'PE\0\0':
        raise ValueError('invalid PE header')
    machine, = struct.unpack_from('<H', image, pe + 4)
    optional_size, flags = struct.unpack_from('<HH', image, pe + 20)
    optional = pe + 24
    if machine != 0x8664 or not flags & 0x2000 or optional_size < 240 or optional + optional_size > len(image):
        raise ValueError('expected an x64 DLL with a complete optional header')
    if struct.unpack_from('<H', image, optional)[0] != 0x20b:
        raise ValueError('expected PE32+')
    checksum, = struct.unpack_from('<I', image, optional + 64)
    directories, = struct.unpack_from('<I', image, optional + 108)
    if directories < 5:
        raise ValueError('missing certificate directory')
    certificate = struct.unpack_from('<II', image, optional + 144)
    if checksum or certificate != (0, 0):
        raise ValueError('refusing a checksummed or signed image; normalize before signing')
    result = bytearray(image)
    result[pe + 8:pe + 12] = b'\0' * 4
    return bytes(result)


if __name__ == '__main__':
    path = Path(sys.argv[1])
    original = path.read_bytes()
    result = normalized_timestamp(original)
    if result != original:
        path.write_bytes(result)
