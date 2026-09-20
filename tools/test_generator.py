import json
from pathlib import Path
import struct
import tempfile
import unittest
import zipfile
from generate_test_pack import BLOCKS, generate

class GeneratorTest(unittest.TestCase):
    def test_models_material_dimensions_animation_and_safe_rerun(self):
        with tempfile.TemporaryDirectory() as directory:
            pack = Path(directory) / 'pack.zip'
            generate(pack, 16)
            with zipfile.ZipFile(pack) as z:
                self.assertEqual(json.loads(z.read('pack.mcmeta'))['pack']['pack_format'], 34)
                self.assertIsNone(z.testzip())
                for block in BLOCKS:
                    model = json.loads(z.read(f'assets/minecraft/models/block/{block}.json'))
                    ns, texture = model['textures']['all'].split(':')
                    for suffix in ('', '_n', '_s'):
                        name = f'assets/{ns}/textures/{texture}{suffix}.png'
                        image = z.read(name)
                        self.assertEqual(struct.unpack('>II', image[16:24]), (16, 32 if block == 'prismarine' else 16))
                        if block == 'prismarine':
                            self.assertEqual(json.loads(z.read(name+'.mcmeta'))['animation']['frames'], [0, 1])
            with self.assertRaises(FileExistsError): generate(pack, 16)

if __name__ == '__main__': unittest.main()
