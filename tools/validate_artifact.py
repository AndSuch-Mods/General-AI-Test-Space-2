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
        assert metadata['depends']['sodium'] == '0.8.12+mc1.21.1'
        assert metadata['depends']['iris'] == '1.8.14-beta.1+mc1.21.1'
        assert metadata['depends']['java'] == '>=21'
        assert metadata['depends']['fabricloader'] == '>=0.18.4'
        assert metadata['depends']['fabric-api'] == '0.116.17+1.21.1'
        assert set(metadata['depends']) == {'minecraft', 'java', 'fabricloader', 'fabric-api', 'sodium', 'iris'}
        assert metadata['version'] == '0.3.15-backport.3+1.21.1'
        assert '${' not in metadata['version']
        assert b'Copyright (c) 2026 mattmcbeardface' in z.read('LICENSE')
        assert not any(n.startswith(('net/minecraft/', '.reference/', 'assets/minecraft/')) for n in names)
        config = json.loads(z.read('pagesofatlas.client.mixins.json'))
        assert config['required'] and config['injectors']['defaultRequire'] == 1
        # Loom 1.16 statically remaps annotation selectors instead of generating a refmap.
        assert b'Fabric-Loom-Mixin-Remap-Type: static' in z.read('META-INF/MANIFEST.MF')
        selectors = {'SpriteLoaderMixin': 'method_47663', 'TextureAtlasMixin': 'method_45848',
                     'ShaderInstanceMixin': 'method_34586', 'IrisExtendedShaderMixin': 'method_34586',
                     'IrisFallbackShaderMixin': 'method_34586', 'TextureUtilMixin': 'Lnet/minecraft/class_1011$class_1013;'}
        for mixin, selector in selectors.items():
            assert selector.encode() in z.read('com/pagesofatlas/mixin/' + mixin + '.class'), f'unmapped selector in {mixin}'
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
                  'required_mixins': len(config['client']), 'checks': 'zip, metadata, MIT license, class version, resources, static mixin selectors, named-reference scan',
                  'runtime_rendering_tested': False}
        path.with_suffix('.jar.sha256').write_text(result['sha256'] + '  ' + path.name + '\n', encoding='utf-8')
        report = Path('build/reports/package-validation.json')
        report.parent.mkdir(parents=True, exist_ok=True)
        report.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
        print(json.dumps(result, indent=2))

if __name__ == '__main__':
    jars = [p for p in Path('build/libs').glob('pages-of-atlas-0.3.15-backport.3+1.21.1.jar')]
    assert len(jars) == 1, f'Expected one production JAR, got {jars}'
    validate(jars[0])
