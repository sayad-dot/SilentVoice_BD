#!/usr/bin/env python3
import json, numpy as np, pathlib, sys

data_dir = pathlib.Path(__file__).parent.parent / 'data'
train_dir = data_dir / 'training'
train_dir.mkdir(exist_ok=True)

for json_file in data_dir.glob('features_*.json'):
    class_name = json_file.stem.replace('features_', '')
    class_dir = train_dir / class_name
    class_dir.mkdir(exist_ok=True)
    with open(json_file, 'r') as f:
        data = json.load(f)
    seqs = data.get('pose_sequence', [])
    # Save each sequence as a separate .npy
    for i, seq in enumerate(seqs):
        np.save(class_dir / f'{class_name}_{i}.npy', np.array(seq, dtype=np.float32))
    print(f'→ Converted {len(seqs)} sequences for class "{class_name}"')
