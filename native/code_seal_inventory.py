"""Bounded, complete selection of build-defined code images from a production JAR."""
from collections import Counter

# Phase-one storage adds required top-level/nested images beyond 256. Native uses the generated array length.
# Keep a finite count ceiling and the unchanged 4 MiB aggregate image limit; never omit required families.
MAX_CLASSES = 512
MAX_TOTAL_BYTES = 4 * 1024 * 1024


def select_registered_images(source, prefixes):
    prefixes = tuple(prefixes)
    if not prefixes or len(set(prefixes)) != len(prefixes):
        raise ValueError("Code seals require a nonempty unique prefix inventory")
    entries = sorted((entry for entry in source.infolist()
        if entry.filename.endswith('.class') and any(
            entry.filename == prefix + '.class' or entry.filename.startswith(prefix + '$')
            for prefix in prefixes)), key=lambda entry: entry.filename[:-6])
    names = Counter(entry.filename for entry in entries)
    duplicates = sorted(name for name, count in names.items() if count != 1)
    if duplicates:
        raise ValueError("Duplicate registered class entries: " + ', '.join(duplicates))
    missing = sorted(prefix for prefix in prefixes if prefix + '.class' not in names)
    if missing:
        raise ValueError("Missing required registered classes: " + ', '.join(missing))
    total = sum(entry.file_size for entry in entries)
    if not 1 <= len(entries) <= MAX_CLASSES or not 0 < total <= MAX_TOTAL_BYTES:
        raise ValueError(f"Code seal inventory exceeds its budget: {len(entries)}/{MAX_CLASSES} classes, "
            f"{total}/{MAX_TOTAL_BYTES} bytes")
    images = {}
    used = 0
    for entry in entries:
        # Bound decompression too; a metadata estimate must not authorize an unbounded read.
        with source.open(entry) as stream:
            image = stream.read(MAX_TOTAL_BYTES - used + 1)
        used += len(image)
        if used > MAX_TOTAL_BYTES or len(image) != entry.file_size:
            raise ValueError("Code seal image size changed or exceeded budget: " + entry.filename)
        if len(image) < 8 or image[:4] != b'\xca\xfe\xba\xbe':
            raise ValueError("Invalid registered class header: " + entry.filename)
        images[entry.filename[:-6]] = image
    return images
