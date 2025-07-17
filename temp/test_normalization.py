#!/usr/bin/env python3
import json
import numpy as np
import sys,os
pose_extractor_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'python-ai', 'scripts'))
if pose_extractor_dir not in sys.path:
    sys.path.insert(0, pose_extractor_dir)
from pose_extractor import OptimizedMediaPipePoseExtractor

# 1. Load training-data normalization stats
data = json.load(open('python-ai/data/training_data.json'))
X_train = np.array(data['X'], dtype=np.float32)
print("TRAIN mean/std:", X_train.mean(), X_train.std())

# 2. Extract and normalize a sequence from a real video
video_path = 'dataset/bdslw60/archive/aam/U10W37F_trial_5_L.mp4'
extractor = OptimizedMediaPipePoseExtractor()
sequence = extractor.extract_pose_from_video_file(video_path, 30)

if len(sequence) == 0:
    print("❌ Empty sequence returned - pose extraction failed")
    sys.exit(1)

S = np.array(sequence, dtype=np.float32)
print("INFER mean/std:", S.mean(), S.std())

# 3. Check feature dimension
feature_dim = S.shape[1]
print("Feature dimension:", feature_dim)
