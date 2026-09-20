#!/usr/bin/env python3
"""Fetch checksum-pinned public shader inputs for local compiler checks, never redistribution."""
import hashlib
import io
import json
from pathlib import Path
import urllib.request
import zipfile

root = Path('build/shader-inputs')
root.mkdir(parents=True, exist_ok=True)
for source in json.loads(Path('SHADERS.json').read_text()):
    cache = root / (source['name'] + '.download')
    data = cache.read_bytes() if cache.exists() else urllib.request.urlopen(source['url'], timeout=60).read()
    assert hashlib.sha256(data).hexdigest() == source['sha256'], source['name']
    cache.write_bytes(data)
    for entry in source['files']:
        content = data
        if source['name'] == 'bsl':
            with zipfile.ZipFile(io.BytesIO(data)) as z:
                content = z.read(entry['source'])
        (root / entry['fixture']).write_bytes(content)
    print(source['name'], source['reference'], 'verified')
