#!/usr/bin/env python3
"""
Test end-to-end model prediction on a single video.

Usage:
    python test_model_prediction.py <video_path> [--no-norm] [--topk 5]

- <video_path>: absolute or relative path to an MP4.
- --no-norm: disable normalization when extracting pose (debug only).
- --topk N: show top N classes (default 5).
"""

import os
import sys
import argparse
import numpy as np

# Local imports (assumes running from python-ai/scripts/)
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
BASE_DIR = os.path.abspath(os.path.join(SCRIPT_DIR, ".."))
sys.path.insert(0, SCRIPT_DIR)      # for local scripts
sys.path.insert(0, BASE_DIR)        # for python-ai root

# Imports from your project
from pose_extractor import OptimizedMediaPipePoseExtractor
from sign_predictor import SignLanguagePredictor

def get_class_names(label_encoder):
    """Return class names as a numpy array of strings."""
    if hasattr(label_encoder, "classes_"):
        return np.array(label_encoder.classes_)
    # Fallback (MockLE in demo mode)
    if hasattr(label_encoder, "__dict__") and "classes" in label_encoder.__dict__:
        return np.array(label_encoder.classes)
    # Last resort
    return np.array([str(i) for i in range(999)])

def run_prediction(video_path, apply_normalization=True, topk=5):
    # 1. Extract pose
    extractor = OptimizedMediaPipePoseExtractor()
    seq = extractor.extract_pose_from_video_file(
        video_path,
        max_frames=30,
        apply_normalization=apply_normalization
    )

    seq = np.array(seq)
    if seq.ndim != 2 or seq.shape[1] != 288:
        print(f"[WARN] Unexpected pose shape {seq.shape}; attempting to coerce.")
    pose_list = seq.tolist()  # SignLanguagePredictor expects list of lists

    # 2. Load model + label encoder
    predictor = SignLanguagePredictor()

    # 3. Run model
    X = predictor.preprocess(pose_list)  # (1, 30, 288)
    print(f"\nModel input shape: {X.shape}")
    preds = predictor.model.predict(X, verbose=0)[0]  # (num_classes,)
    preds = np.array(preds, dtype=np.float32)

    # 4. Class decoding
    class_names = get_class_names(predictor.label_encoder)
    num_classes = preds.shape[0]
    if class_names.shape[0] != num_classes:
        print(f"[WARN] label_encoder classes ({class_names.shape[0]}) "
              f"!= model outputs ({num_classes})")
        # truncate/pad for safety
        if class_names.shape[0] > num_classes:
            class_names = class_names[:num_classes]
        else:
            pad = np.array([f"class_{i}" for i in range(class_names.shape[0], num_classes)])
            class_names = np.concatenate([class_names, pad])

    # 5. Sort top-k
    k = min(topk, num_classes)
    top_idx = np.argsort(preds)[::-1][:k]
    top_probs = preds[top_idx]
    top_labels = class_names[top_idx]

    # 6. Print raw + summary
    print("\n=== RAW SOFTMAX VECTOR ===")
    print(preds)

    print("\n=== TOP {} PREDICTIONS ===".format(k))
    for rank, (lbl, prob) in enumerate(zip(top_labels, top_probs), start=1):
        print(f"{rank:>2}. {lbl}  ({prob*100:.2f}%)")

    argmax_idx = int(np.argmax(preds))
    argmax_label = class_names[argmax_idx]
    print("\nArgmax class index:", argmax_idx)
    print("Argmax class label:", argmax_label)
    print("Argmax confidence:  {:.2f}%".format(preds[argmax_idx]*100))

    return {
        "preds": preds,
        "top_idx": top_idx,
        "top_labels": top_labels,
        "top_probs": top_probs,
        "argmax_idx": argmax_idx,
        "argmax_label": argmax_label,
    }

def parse_args():
    ap = argparse.ArgumentParser()
    ap.add_argument("video_path", help="Path to MP4 video.")
    ap.add_argument("--no-norm", action="store_true", help="Disable normalization during pose extraction.")
    ap.add_argument("--topk", type=int, default=5, help="Show top-k classes.")
    return ap.parse_args()

def main():
    args = parse_args()
    video_path = args.video_path
    if not os.path.exists(video_path):
        print(f"[ERROR] Video not found: {video_path}")
        sys.exit(1)

    print(f"Video: {video_path}")
    print(f"Normalization during extraction: {not args.no_norm}")

    run_prediction(
        video_path,
        apply_normalization=(not args.no_norm),
        topk=args.topk
    )

if __name__ == "__main__":
    main()
