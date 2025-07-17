import json
import numpy as np
import sys
sys.path.append('.')
from pose_extractor import extract_pose_landmarks

def analyze_training_data():
    print("Analyzing training data...")
    with open('python-ai/data/training_data.json', 'r') as f:
        training_data = json.load(f)
    X = training_data['X']
    y = training_data['y']
    print(f"Training data samples: {len(X)}")
    for i, (sequence, label) in enumerate(zip(X[:3], y[:3])):
        landmarks = np.array(sequence)
        print(f"\nTraining Sample {i+1}:")
        print(f"  Shape: {landmarks.shape}")
        print(f"  Range: {landmarks.min():.6f} to {landmarks.max():.6f}")
        print(f"  Mean: {landmarks.mean():.6f}")
        print(f"  Std: {landmarks.std():.6f}")
        print(f"  Label: {label}")
        if np.isnan(landmarks).any():
            print(f"  ❌ Contains NaN values: {np.isnan(landmarks).sum()}")
        if np.isinf(landmarks).any():
            print(f"  ❌ Contains infinite values: {np.isinf(landmarks).sum()}")

def compare_with_inference(test_video_path):
    print(f"\nExtracting landmarks from test video: {test_video_path}")
    inference_landmarks = extract_pose_landmarks(test_video_path)
    inference_landmarks = np.array(inference_landmarks)
    print(f"Inference landmarks:")
    print(f"  Shape: {inference_landmarks.shape}")
    print(f"  Range: {inference_landmarks.min():.6f} to {inference_landmarks.max():.6f}")
    print(f"  Mean: {inference_landmarks.mean():.6f}")
    print(f"  Std: {inference_landmarks.std():.6f}")
    # Load training data for comparison
    with open('python-ai/data/training_data.json', 'r') as f:
        training_data = json.load(f)
    X = training_data['X']
    training_sample = np.array(X[0])
    print(f"\nComparison:")
    print(f"  Shape match: {training_sample.shape[1:] == inference_landmarks.shape[1:]}")
    print(f"  Range similarity: Training({training_sample.min():.3f}-{training_sample.max():.3f}) vs Inference({inference_landmarks.min():.3f}-{inference_landmarks.max():.3f})")
    print(f"  Scale similarity: Training std={training_sample.std():.3f} vs Inference std={inference_landmarks.std():.3f}")
    range_diff = abs(training_sample.max() - inference_landmarks.max())
    if range_diff > 0.5:
        print(f"  ❌ WARNING: Large range difference ({range_diff:.3f})")
    return training_sample, inference_landmarks

if __name__ == "__main__":
    analyze_training_data()
    # Replace with the actual path to your test video
    test_video = "/media/sayad/Ubuntu-Data/SilentVoice_BD/dataset/bdslw60/archive/balti/U1W219F_trial_4_L.mp4"
    compare_with_inference(test_video)
