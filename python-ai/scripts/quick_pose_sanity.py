#!/usr/bin/env python3
import sys, os, numpy as np
from pose_extractor import OptimizedMediaPipePoseExtractor

def summarize_seq(seq):
    seq = np.array(seq)
    if seq.ndim != 2:
        print(f"[WARN] Expected 2D (T,288) but got shape {seq.shape}")
        return
    nonzero_per_frame = np.count_nonzero(seq, axis=1)
    pct_nonzero_frames = (nonzero_per_frame > 0).mean() * 100.0
    avg_nonzero = nonzero_per_frame.mean()
    print(f"Total frames: {seq.shape[0]}")
    print(f"Feature dim: {seq.shape[1]}")
    print(f"Frames w/ ANY nonzero: {pct_nonzero_frames:.1f}%")
    print(f"Avg nonzero features per frame: {avg_nonzero:.1f} / 288")
    # blocks
    lh = seq[:, 0:63]
    rh = seq[:, 63:126]
    pose = seq[:, 126:258]
    face = seq[:, 258:288]
    for name, block in [("LH", lh), ("RH", rh), ("POSE", pose), ("FACE", face)]:
        nz = np.count_nonzero(block)
        total = block.size
        print(f"  {name}: {nz}/{total} nonzero ({100*nz/total:.1f}%)")
    print(f"Global min/max: {seq.min():.4f}/{seq.max():.4f}")
    print(f"Mean: {seq.mean():.4f}, Std: {seq.std():.4f}")

def main():
    if len(sys.argv) < 2:
        print("Usage: python quick_pose_sanity.py <video_path>")
        return
    video_path = sys.argv[1]
    if not os.path.exists(video_path):
        print(f"Video not found: {video_path}")
        return
    extractor = OptimizedMediaPipePoseExtractor()
    print(f"\nRunning on: {video_path}")
    print("\n=== Raw Features (no normalization) ===")
    seq = extractor.extract_pose_from_video_file(video_path, max_frames=30, apply_normalization=False)
    summarize_seq(seq)
    print("\n=== After Normalization (if params loaded) ===")
    seq_norm = extractor.extract_pose_from_video_file(video_path, max_frames=30, apply_normalization=True)
    summarize_seq(seq_norm)

if __name__ == "__main__":
    main()
