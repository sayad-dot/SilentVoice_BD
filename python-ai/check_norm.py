#!/usr/bin/env python3
"""
check_norm.py

Loads one frame from the “বালু” test video, extracts raw pose features
and applies your current normalization parameters, then prints mean and std
for both raw and normalized feature vectors to verify alignment.
"""

import os
import sys

# Ensure the script’s directory is on the Python path for local imports
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import cv2
import numpy as np
import mediapipe as mp
from pose_extractor import OptimizedMediaPipePoseExtractor

# Path to your test video
VIDEO_PATH = '/media/sayad/Ubuntu-Data/SilentVoice_BD/dataset/bdslw60/archive/balu/U1W220F_trial_3_R.mp4'

def main():
    # 1) Initialize extractor and MediaPipe Holistic
    extractor = OptimizedMediaPipePoseExtractor()
    holistic = mp.solutions.holistic.Holistic(
        static_image_mode=False,
        min_detection_confidence=0.5,
        min_tracking_confidence=0.5
    )

    # 2) Read the first frame of the video
    cap = cv2.VideoCapture(VIDEO_PATH)
    ret, frame = cap.read()
    cap.release()
    if not ret:
        print(f"Error: could not read frame from {VIDEO_PATH}")
        return

    # 3) Run Holistic to get landmarks
    rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    results = holistic.process(rgb)
    holistic.close()

    # 4) Extract raw features (288-dim vector)
    raw = extractor.extract_keypoints(results)

    # 5) Load current normalization parameters
    means = extractor.feature_means
    stds  = extractor.feature_stds

    # 6) Apply normalization: (x – mean) / std
    norm = (raw - means) / stds

    # 7) Print statistics
    print("=== Feature Statistics ===")
    print(f"Raw features:  mean = {raw.mean():.6f}, std = {raw.std():.6f}")
    print(f"Norm features: mean = {norm.mean():.6f}, std = {norm.std():.6f}")

if __name__ == "__main__":
    main()
