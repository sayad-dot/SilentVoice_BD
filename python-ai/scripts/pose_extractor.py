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
    
    def extract_keypoints(self, results) -> np.ndarray:
        """Extract optimized keypoints from MediaPipe results"""
        try:
            # Hand landmarks (21 points each hand, 3D)
            lh = np.array([[res.x, res.y, res.z] for res in results.left_hand_landmarks.landmark]).flatten() \
                if results.left_hand_landmarks else np.zeros(21*3)
            rh = np.array([[res.x, res.y, res.z] for res in results.right_hand_landmarks.landmark]).flatten() \
                if results.right_hand_landmarks else np.zeros(21*3)
            
            # Pose landmarks (33 points, 4D - including visibility)
            pose = np.array([[res.x, res.y, res.z, res.visibility] for res in results.pose_landmarks.landmark]).flatten() \
                if results.pose_landmarks else np.zeros(33*4)
            
            # Face landmarks (reduced to first 10 points for efficiency)
            face = np.array([[res.x, res.y, res.z] for res in results.face_landmarks.landmark[:10]]).flatten() \
                if results.face_landmarks else np.zeros(10*3)
            
            return np.concatenate([lh, rh, pose, face])
        
        except Exception as e:
            print(f"Error extracting keypoints: {e}", file=sys.stderr)
            return np.zeros(258)  # Default feature size
    
    def extract_pose_from_video_frames(self, frame_paths: List[str]) -> List[List[float]]:
        """Extract pose sequence from list of frame image paths with memory optimization"""
        sequence = []
        processed_count = 0
        
        try:
            for i, frame_path in enumerate(frame_paths):
                if not os.path.exists(frame_path):
                    print(f"Frame not found: {frame_path}", file=sys.stderr)
                    continue
                
                # Read and process frame
                frame = cv2.imread(frame_path)
                if frame is None:
                    print(f"Could not read frame: {frame_path}", file=sys.stderr)
                    continue
                
                # Resize frame for faster processing if too large
                height, width = frame.shape[:2]
                if width > 640:
                    scale = 640.0 / width
                    new_width = int(width * scale)
                    new_height = int(height * scale)
                    frame = cv2.resize(frame, (new_width, new_height))
                
                # Convert BGR to RGB
                image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                image.flags.writeable = False
                
                # Make detection
                results = self.holistic.process(image)
                
                # Extract keypoints
                keypoints = self.extract_keypoints(results)
                sequence.append(keypoints.tolist())
                
                processed_count += 1
                
                # Periodic garbage collection to prevent memory buildup
                if processed_count % 10 == 0:
                    gc.collect()
                
                # Progress indication for large batches
                if len(frame_paths) > 20 and (i + 1) % 10 == 0:
                    print(f"Processed {i + 1}/{len(frame_paths)} frames", file=sys.stderr)
        
        except Exception as e:
            print(f"Error during pose extraction: {e}", file=sys.stderr)
            return []
        
        finally:
            # Clean up
            gc.collect()
        
        print(f"Successfully processed {processed_count}/{len(frame_paths)} frames", file=sys.stderr)
        return sequence
    
    def extract_pose_from_video_file(self, video_path: str, max_frames: int = 30) -> List[List[float]]:
        """Extract pose sequence from video file"""
        cap = cv2.VideoCapture(video_path)
        sequence = []
        frame_count = 0
        
        try:
            while cap.isOpened() and frame_count < max_frames:
                ret, frame = cap.read()
                if not ret:
                    break
                
                # Resize for faster processing
                height, width = frame.shape[:2]
                if width > 640:
                    scale = 640.0 / width
                    new_width = int(width * scale)
                    new_height = int(height * scale)
                    frame = cv2.resize(frame, (new_width, new_height))
                
                # Convert BGR to RGB
                image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                image.flags.writeable = False
                
                # Make detection
                results = self.holistic.process(image)
                
                # Extract keypoints
                keypoints = self.extract_keypoints(results)
                sequence.append(keypoints.tolist())
                
                frame_count += 1
        
        except Exception as e:
            print(f"Error processing video: {e}", file=sys.stderr)
        
        finally:
            cap.release()
            gc.collect()
        
        return sequence

def main():
    """Main function for command line interface"""
    if len(sys.argv) < 3:
        result = {"error": "Usage: python pose_extractor.py <mode> <input_path>", "success": False}
        print(json.dumps(result))
        return
    
    mode = sys.argv[1]  # 'frames' or 'video'
    input_path = sys.argv[2]
    
    extractor = OptimizedMediaPipePoseExtractor()
    
    try:
        if mode == 'frames':
            # Expect JSON list of frame paths
            frame_paths = json.loads(input_path)
            
            if not isinstance(frame_paths, list):
                raise ValueError("Frame paths must be a list")
            
            if len(frame_paths) == 0:
                raise ValueError("No frame paths provided")
            
            sequence = extractor.extract_pose_from_video_frames(frame_paths)
            
        elif mode == 'video':
            # Process video file directly
            max_frames = int(sys.argv[3]) if len(sys.argv) > 3 else 30
            sequence = extractor.extract_pose_from_video_file(input_path, max_frames)
            
        else:
            raise ValueError("Invalid mode. Use 'frames' or 'video'")
        
        # Return pose sequence as JSON
        result = {
            "success": True,
            "pose_sequence": sequence,
            "sequence_length": len(sequence),
            "feature_dimension": len(sequence[0]) if sequence else 0,
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
