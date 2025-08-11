#!/usr/bin/env python3
import json, numpy as np, pathlib
import os

# Source: per-video JSON files  
src = pathlib.Path(__file__).parent.parent / 'data' / 'per_video_json'
# Destination: sequence arrays for training
dst = pathlib.Path(__file__).parent.parent / 'data' / 'sequences'
dst.mkdir(exist_ok=True)

total_sequences = 0
total_videos = 0

print("🔄 Converting 9,307 per-video JSONs to training sequences...")

for json_file in src.glob('*.json'):
    try:
        # Parse filename: class_videoname.json
        parts = json_file.stem.split('_', 1)
        if len(parts) < 2:
            continue
        
        cls, vid = parts
        
        with open(json_file) as f:
            data = json.load(f)
        
        # Extract pose sequence (already processed and normalized)
        pose_seq = data.get('pose_sequence', [])
        if len(pose_seq) != 30:  # Should be exactly 30 frames
            continue
            
        # Convert to numpy array: shape (30, 288)
        seq_array = np.array(pose_seq, dtype=np.float32)
        
        # Create class directory
        seq_dir = dst / cls
        seq_dir.mkdir(exist_ok=True)
        
        # Save as single sequence file
        seq_file = seq_dir / f"{cls}_{vid}_0.npy"
        np.save(seq_file, seq_array)
        
        total_sequences += 1
        total_videos += 1
        
        if total_videos % 500 == 0:
            print(f"✅ Processed {total_videos} videos, created {total_sequences} sequences")
    
    except Exception as e:
        print(f"❌ Error processing {json_file.name}: {e}")

print(f"🎯 Conversion complete:")
print(f"   📹 Videos processed: {total_videos}")
print(f"   📊 Total sequences created: {total_sequences}")
print(f"   📁 Average sequences per class: ~{total_sequences // 60 if total_sequences > 0 else 0}")
