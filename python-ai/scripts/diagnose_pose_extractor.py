import sys
sys.path.append('.')

from pose_extractor import extract_pose_landmarks

import numpy as np

def diagnose_pose_extraction(video_path):
    print(f"Diagnosing pose extraction for: {video_path}")
    try:
        landmarks = extract_pose_landmarks(video_path, max_frames=30)
        print(f"✓ Pose extraction completed successfully")
        print(f"✓ Output shape: {landmarks.shape}")
        print(f"✓ Data type: {landmarks.dtype}")
        print(f"✓ Value range: {landmarks.min():.6f} to {landmarks.max():.6f}")
        print(f"✓ Mean: {landmarks.mean():.6f}")
        print(f"✓ Std: {landmarks.std():.6f}")
        if landmarks.shape[0] == 0:
            print("❌ ERROR: No frames extracted")
            return False
        if landmarks.shape[1] == 0:
            print("❌ ERROR: No landmarks extracted")
            return False
        if np.isnan(landmarks).any():
            nan_count = np.isnan(landmarks).sum()
            print(f"❌ WARNING: {nan_count} NaN values found")
        if np.isinf(landmarks).any():
            inf_count = np.isinf(landmarks).sum()
            print(f"❌ WARNING: {inf_count} infinite values found")
        if landmarks.min() < -2 or landmarks.max() > 2:
            print("❌ WARNING: Landmark values seem unnormalized")
        return True
    except Exception as e:
        print(f"❌ ERROR in pose extraction: {str(e)}")
        return False

test_videos = [
    "/media/sayad/Ubuntu-Data/archive/balti/U1W219F_trial_4_L.mp4",
    "/media/sayad/Ubuntu-Data/archive/chini/U8W47F_trial_3_R.mp4",
    "/media/sayad/Ubuntu-Data/hi.mp4"
]

for video in test_videos:
    print("="*50)
    diagnose_pose_extraction(video)
    print()
