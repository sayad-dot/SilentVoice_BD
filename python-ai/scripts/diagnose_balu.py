#!/usr/bin/env python3
import cv2
import numpy as np
import mediapipe as mp
from pose_extractor import OptimizedMediaPipePoseExtractor

VIDEO_PATH = '/media/sayad/Ubuntu-Data/SilentVoice_BD/dataset/bdslw60/archive/balu/U1W220F_trial_3_R.mp4'

# Match your extractor defaults
MIN_DETECTION_CONFIDENCE = 0.5
MIN_TRACKING_CONFIDENCE  = 0.5

def diagnose(video_path):
    cap = cv2.VideoCapture(video_path)
    extractor = OptimizedMediaPipePoseExtractor()
    mp_holistic = mp.solutions.holistic
    holistic = mp_holistic.Holistic(
        static_image_mode=False,
        min_detection_confidence=MIN_DETECTION_CONFIDENCE,
        min_tracking_confidence=MIN_TRACKING_CONFIDENCE
    )

    print("Frame │ Nonzero Feats │ Avg Feature │ Det Conf │ Track Conf")
    print("──────┼───────────────┼─────────────┼──────────┼───────────")
    frame_idx = 0

    while True:
        ret, frame = cap.read()
        if not ret or frame_idx >= 30:
            break

        # 1) Run MediaPipe Holistic inference to get a Results object
        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = holistic.process(rgb)

        # 2) Extract normalized keypoints from the Results
        try:
            keypoints = extractor.extract_keypoints(results)
        except Exception as e:
            print(f"Error extracting keypoints: {e}")
            keypoints = np.zeros(extractor.feature_means.shape, dtype=float)

        nonzero    = int(np.count_nonzero(keypoints))
        avg_value  = float(np.mean(keypoints)) if nonzero > 0 else 0.0
        det_conf   = getattr(results, 'pose_landmarks', None) and results.pose_landmarks.landmark[0].visibility or None
        track_conf = getattr(results, 'pose_world_landmarks', None) and results.pose_world_landmarks.landmark[0].visibility or None

        print(f"{frame_idx:>5} │ {nonzero:>13} │ {avg_value:>11.4f} │ "
              f"{det_conf!s:>8} │ {track_conf!s:>9}")

        frame_idx += 1

    holistic.close()
    cap.release()

if __name__ == "__main__":
    diagnose(VIDEO_PATH)
