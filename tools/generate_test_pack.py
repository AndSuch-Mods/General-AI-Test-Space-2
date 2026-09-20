#!/usr/bin/env python3
"""Generate original synthetic artwork only; no external or paid inputs. Python 3 stdlib."""
import argparse
import json
from pathlib import Path
import struct
import zipfile
import zlib

BLOCKS = ('stone', 'dirt', 'cobblestone', 'bricks', 'sand', 'gravel', 'gold_block',
          'diamond_block', 'glass', 'oak_leaves', 'redstone_block', 'prismarine')

def png(size, index, material, frames):
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
    raw = bytearray()
    for y in range(size * frames):
        raw.append(0)
        for x in range(size):
            checker = ((x * 8 // size) + (y * 8 // size)) % 2
            if material == 'n':
                rgba = (128 + checker * 40, 128, 255, 40 + (y % size) * 215 // size)
            elif material == 's':
                # LabPBR _s: smoothness, F0/metal, SSS, emission; preserve all channels.
                rgba = (40 + checker * 200, 230 if index % 2 else 0,
                        160 if index % 3 == 0 else 0, 180 if index % 4 == 0 and checker else 0)
            else:
                rgba = ((index * 47 + 50 + checker * 60) % 256,
                        (index * 89 + 80 + (y // size) * 90) % 256,
                        (index * 131 + 100) % 256,
                        128 if BLOCKS[index] == 'glass' else (255 * checker if BLOCKS[index] == 'oak_leaves' else 255))
            raw.extend(rgba)
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', size, size * frames, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 6)) + chunk(b'IEND', b''))

def generate(destination, size):
    if destination.exists():
        raise FileExistsError(f'Refusing to overwrite {destination}')
    if size < 16 or size > 2048 or size & (size - 1):
        raise ValueError('size must be a power of two from 16 through 2048')
    destination.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(destination, 'w', zipfile.ZIP_DEFLATED) as z:
        def put(name, data):
            info = zipfile.ZipInfo(name, (2024, 8, 8, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, data if isinstance(data, bytes) else json.dumps(data, sort_keys=True))
        put('pack.mcmeta', {'pack': {'pack_format': 34, 'description': 'Pages of Atlas 1.21.1 synthetic paging + LabPBR test'}})
        for index, block in enumerate(BLOCKS):
            frames = 2 if block == 'prismarine' else 1
            texture = 'pagesofatlas_test:block/' + block
            put(f'assets/minecraft/models/block/{block}.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': texture}})
            for material in ('', 'n', 's'):
                name = f'assets/pagesofatlas_test/textures/block/{block}' + ('_' + material if material else '') + '.png'
                put(name, png(size, index, material, frames))
                if frames == 2:
                    put(name + '.mcmeta', {'animation': {'width': size, 'height': size, 'frametime': 8, 'frames': [0, 1], 'interpolate': True}})
        put('TEST_CONTENT.json', {'blocks': BLOCKS, 'sprite_size': size,
                                'page_cap': 2048, 'expected': 'With default size 1024, at least 3 physical block-atlas pages; vanilla sprites may require a fourth.'})

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=Path('build/test-packs/poa-synthetic-1.21.1-backport3.zip'))
    parser.add_argument('--size', type=int, default=1024)
    args = parser.parse_args()
    generate(args.output, args.size)
    print(args.output)
