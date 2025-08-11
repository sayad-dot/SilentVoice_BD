#!/usr/bin/env python3
import json, numpy as np, pathlib

src = pathlib.Path(__file__).parent.parent / 'data' / 'training'
dst = pathlib.Path(__file__).parent.parent / 'data' / 'sequences'
dst.mkdir(exist_ok=True)

for cls in src.iterdir():
    if not cls.is_dir(): continue
    frames = sorted(cls.glob('*.npy'))
    class_seq_dir = dst / cls.name
    class_seq_dir.mkdir(exist_ok=True)
    arrs = [np.load(f) for f in frames]
    # Slide window of 30
    for i in range(len(arrs) - 29):
        seq = np.stack(arrs[i:i+30], axis=0)
        np.save(class_seq_dir / f'{cls.name}_{i}.npy', seq)
    print(f'{cls.name}: {len(arrs)-29} sequences')
