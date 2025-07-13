#!/usr/bin/env python3
import cv2
import numpy as np
import mediapipe as mp
import json
import sys
import os
import gc
from typing import List, Optional

class OptimizedMediaPipePoseExtractor:
    def __init__(self):
        self.mp_holistic = mp.solutions.holistic
        self.holistic = self.mp_holistic.Holistic(
            min_detection_confidence=0.5,
            min_tracking_confidence=0.5,
            model_complexity=0  # Fastest model for CPU
        )
        
        # Load normalization parameters if available
        self.feature_means = None
        self.feature_stds = None
        self.load_normalization_params()
    
    def load_normalization_params(self):
        """Load normalization parameters for inference"""
        try:
            # Try to load from numpy files first (faster)
            means_path = '../data/feature_means.npy'
            stds_path = '../data/feature_stds.npy'
            
            if os.path.exists(means_path) and os.path.exists(stds_path):
                self.feature_means = np.load(means_path)
                self.feature_stds = np.load(stds_path)
                print(f"✅ Loaded normalization params from numpy files", file=sys.stderr)
                return
            
            # Fallback to JSON file
            json_path = '../data/normalization_params.json'
            if os.path.exists(json_path):
                with open(json_path, 'r') as f:
                    params = json.load(f)
                self.feature_means = np.array(params['feature_means'])
                self.feature_stds = np.array(params['feature_stds'])
                print(f"✅ Loaded normalization params from JSON file", file=sys.stderr)
                return
            
            print("⚠️ No normalization parameters found. Using raw features.", file=sys.stderr)
            
        except Exception as e:
            print(f"⚠️ Error loading normalization params: {e}", file=sys.stderr)
            print("Using raw features without normalization.", file=sys.stderr)
    
    def normalize_sequence(self, sequence):
        """Apply normalization to a pose sequence"""
        if self.feature_means is None or self.feature_stds is None:
            print("⚠️ No normalization parameters available", file=sys.stderr)
            return sequence
        
        try:
            sequence = np.array(sequence)
            normalized_sequence = (sequence - self.feature_means) / self.feature_stds
            return normalized_sequence.tolist()
        except Exception as e:
            print(f"⚠️ Error normalizing sequence: {e}", file=sys.stderr)
            return sequence
    
    def extract_keypoints(self, results) -> np.ndarray:
        try:
            lh = np.array([[res.x, res.y, res.z] for res in results.left_hand_landmarks.landmark]).flatten() \
                if results.left_hand_landmarks else np.zeros(21*3)
            rh = np.array([[res.x, res.y, res.z] for res in results.right_hand_landmarks.landmark]).flatten() \
                if results.right_hand_landmarks else np.zeros(21*3)
            pose = np.array([[res.x, res.y, res.z, res.visibility] for res in results.pose_landmarks.landmark]).flatten() \
                if results.pose_landmarks else np.zeros(33*4)
            face = np.array([[res.x, res.y, res.z] for res in results.face_landmarks.landmark[:10]]).flatten() \
                if results.face_landmarks else np.zeros(10*3)
            return np.concatenate([lh, rh, pose, face])
        except Exception as e:
            print(f"Error extracting keypoints: {e}", file=sys.stderr)
            return np.zeros(288)  # Updated to 288 features
    
    def extract_pose_from_video_frames(self, frame_paths: List[str]) -> List[List[float]]:
        sequence = []
        processed_count = 0
        try:
            for i, frame_path in enumerate(frame_paths):
                if not os.path.exists(frame_path):
                    print(f"Frame not found: {frame_path}", file=sys.stderr)
                    continue
                frame = cv2.imread(frame_path)
                if frame is None:
                    print(f"Could not read frame: {frame_path}", file=sys.stderr)
                    continue
                height, width = frame.shape[:2]
                if width > 640:
                    scale = 640.0 / width
                    new_width = int(width * scale)
                    new_height = int(height * scale)
                    frame = cv2.resize(frame, (new_width, new_height))
                image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                image.flags.writeable = False
                results = self.holistic.process(image)
                keypoints = self.extract_keypoints(results)
                sequence.append(keypoints.tolist())
                processed_count += 1
                if processed_count % 10 == 0:
                    gc.collect()
                if len(frame_paths) > 20 and (i + 1) % 10 == 0:
                    print(f"Processed {i + 1}/{len(frame_paths)} frames", file=sys.stderr)
        except Exception as e:
            print(f"Error during pose extraction: {e}", file=sys.stderr)
            return []
        finally:
            gc.collect()
        
        print(f"Successfully processed {processed_count}/{len(frame_paths)} frames", file=sys.stderr)
        
        # *** APPLY NORMALIZATION HERE FOR INFERENCE ***
        normalized_sequence = self.normalize_sequence(sequence)
        return normalized_sequence
    
    def extract_pose_from_video_file(self, video_path: str, max_frames: int = 30) -> List[List[float]]:
        cap = cv2.VideoCapture(video_path)
        sequence = []
        frame_count = 0
        try:
            while cap.isOpened() and frame_count < max_frames:
                ret, frame = cap.read()
                if not ret:
                    break
                height, width = frame.shape[:2]
                if width > 640:
                    scale = 640.0 / width
                    new_width = int(width * scale)
                    new_height = int(height * scale)
                    frame = cv2.resize(frame, (new_width, new_height))
                image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                image.flags.writeable = False
                results = self.holistic.process(image)
                keypoints = self.extract_keypoints(results)
                sequence.append(keypoints.tolist())
                frame_count += 1
        except Exception as e:
            print(f"Error processing video: {e}", file=sys.stderr)
        finally:
            cap.release()
            gc.collect()
        
        # *** APPLY NORMALIZATION HERE FOR INFERENCE ***
        normalized_sequence = self.normalize_sequence(sequence)
        return normalized_sequence

# ---- WRAPPER FUNCTION FOR COMPATIBILITY ----
def extract_pose_landmarks(video_path, max_frames=30):
    extractor = OptimizedMediaPipePoseExtractor()
    return np.array(extractor.extract_pose_from_video_file(video_path, max_frames))

def main():
    if len(sys.argv) < 3:
        result = {"error": "Usage: python pose_extractor.py <mode> <input_path>", "success": False}
        print(json.dumps(result))
        return
    mode = sys.argv[1]
    input_path = sys.argv[2]
    extractor = OptimizedMediaPipePoseExtractor()
    try:
        if mode == 'frames':
            frame_paths = json.loads(input_path)
            if not isinstance(frame_paths, list):
                raise ValueError("Frame paths must be a list")
            if len(frame_paths) == 0:
                raise ValueError("No frame paths provided")
            sequence = extractor.extract_pose_from_video_frames(frame_paths)
        elif mode == 'video':
            max_frames = int(sys.argv[3]) if len(sys.argv) > 3 else 30
            sequence = extractor.extract_pose_from_video_file(input_path, max_frames)
        else:
            raise ValueError("Invalid mode. Use 'frames' or 'video'")
        result = {
            "success": True,
            "pose_sequence": sequence,
            "sequence_length": len(sequence),
            "feature_dimension": len(sequence[0]) if sequence else 0,
            "normalized": True,  # Flag to indicate normalized data
            "processing_info": {
                "mode": mode,
                "input_count": len(frame_paths) if mode == 'frames' else 1,
                "successful_extractions": len(sequence)
            }
        }
        print(json.dumps(result))
    except Exception as e:
        error_result = {
            "success": False,
            "error": str(e),
            "pose_sequence": [],
            "sequence_length": 0,
            "feature_dimension": 0
        }
        print(json.dumps(error_result))

if __name__ == "__main__":
    main()
