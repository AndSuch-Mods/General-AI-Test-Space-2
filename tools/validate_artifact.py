#!/usr/bin/env python3
"""Validate the remapped package without loading the game. Python 3 stdlib."""
import hashlib
import json
from pathlib import Path
import struct
import zipfile

def validate(path):
    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        assert len(names) == len(set(names)), 'duplicate JAR entries'
        assert z.testzip() is None, 'corrupt JAR'
        metadata = json.loads(z.read('fabric.mod.json'))
        assert metadata['environment'] == 'client'
        assert metadata['depends']['minecraft'] == '1.21.1'
        assert metadata['depends']['sodium'] == '0.6.13+mc1.21.1'
        assert metadata['depends']['iris'] == '1.8.8+mc1.21.1'
        assert metadata['depends']['java'] == '>=21'
        assert '${' not in metadata['version']
        assert b'Copyright (c) 2026 mattmcbeardface' in z.read('LICENSE')
        assert not any(n.startswith(('net/minecraft/', '.reference/', 'assets/minecraft/')) for n in names)
        config = json.loads(z.read('pagesofatlas.client.mixins.json'))
        assert config['required'] and config['injectors']['defaultRequire'] == 1
        refmap = json.loads(z.read(config['refmap']))
        assert 'named:intermediary' in refmap['data']
        assert 'method_47663' in json.dumps(refmap), 'missing stitch remapping'
        for mixin in config['client']:
            assert config['package'].replace('.', '/') + '/' + mixin + '.class' in names
        classes = [n for n in names if n.endswith('.class')]
        for name in classes:
            data = z.read(name)
            magic, minor, major = struct.unpack('>IHH', data[:8])
            assert magic == 0xcafebabe and major == 65 and minor == 0, (name, major, minor)
            assert b'net/minecraft/client/renderer/texture/' not in data, f'unremapped reference in {name}'
        result = {'jar': path.name, 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
                  'version': metadata['version'], 'java_class_major': 65, 'classes': len(classes),
                  'required_mixins': len(config['client']), 'checks': 'zip, metadata, MIT license, class version, resource/refmap presence, named-reference scan',
                  'runtime_rendering_tested': False}
        path.with_suffix('.jar.sha256').write_text(result['sha256'] + '  ' + path.name + '\n', encoding='utf-8')
        report = Path('build/reports/package-validation.json')
        report.parent.mkdir(parents=True, exist_ok=True)
        report.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
        print(json.dumps(result, indent=2))

if __name__ == '__main__':
    jars = [p for p in Path('build/libs').glob('*.jar') if not p.name.endswith('-sources.jar')]
    assert len(jars) == 1, f'Expected one production JAR, got {jars}'
    validate(jars[0])
