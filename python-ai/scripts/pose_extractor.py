import cv2
import numpy as np
import mediapipe as mp
import json
import sys
import os

class MediaPipePoseExtractor:
    def __init__(self):
        self.mp_holistic = mp.solutions.holistic
        self.mp_drawing = mp.solutions.drawing_utils
        self.holistic = self.mp_holistic.Holistic(
            min_detection_confidence=0.5,
            min_tracking_confidence=0.5
        )
    
    def extract_keypoints(self, results):
        """Extract keypoints from MediaPipe results"""
        # Hand landmarks (21 points each hand)
        lh = np.array([[res.x, res.y, res.z] for res in results.left_hand_landmarks.landmark]).flatten() if results.left_hand_landmarks else np.zeros(21*3)
        rh = np.array([[res.x, res.y, res.z] for res in results.right_hand_landmarks.landmark]).flatten() if results.right_hand_landmarks else np.zeros(21*3)
        
        # Pose landmarks (33 points)
        pose = np.array([[res.x, res.y, res.z, res.visibility] for res in results.pose_landmarks.landmark]).flatten() if results.pose_landmarks else np.zeros(33*4)
        
        # Face landmarks (468 points, but we'll use first 10 for efficiency)
        face = np.array([[res.x, res.y, res.z] for res in results.face_landmarks.landmark[:10]]).flatten() if results.face_landmarks else np.zeros(10*3)
        
        return np.concatenate([lh, rh, pose, face])
    
    def extract_pose_from_video_frames(self, frame_paths):
        """Extract pose sequence from list of frame image paths"""
        sequence = []
        
        for frame_path in frame_paths:
            if not os.path.exists(frame_path):
                continue
                
            # Read frame
            frame = cv2.imread(frame_path)
            if frame is None:
                continue
                
            # Convert BGR to RGB
            image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            image.flags.writeable = False
            
            # Make detection
            results = self.holistic.process(image)
            
            # Extract keypoints
            keypoints = self.extract_keypoints(results)
            sequence.append(keypoints.tolist())
        
        return sequence
    
    def extract_pose_from_video_file(self, video_path, max_frames=30):
        """Extract pose sequence from video file"""
        cap = cv2.VideoCapture(video_path)
        sequence = []
        frame_count = 0
        
        while cap.isOpened() and frame_count < max_frames:
            ret, frame = cap.read()
            if not ret:
                break
            
            # Convert BGR to RGB
            image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            image.flags.writeable = False
            
            # Make detection
            results = self.holistic.process(image)
            
            # Extract keypoints
            keypoints = self.extract_keypoints(results)
            sequence.append(keypoints.tolist())
            
            frame_count += 1
        
        cap.release()
        return sequence

def main():
    if len(sys.argv) < 3:
        print(json.dumps({"error": "Usage: python pose_extractor.py <mode> <input_path>"}))
        return
    
    mode = sys.argv[1]  # 'frames' or 'video'
    input_path = sys.argv[2]
    
    extractor = MediaPipePoseExtractor()
    
    try:
        if mode == 'frames':
            # Expect JSON list of frame paths
            frame_paths = json.loads(input_path)
            sequence = extractor.extract_pose_from_video_frames(frame_paths)
        elif mode == 'video':
            # Process video file directly
            sequence = extractor.extract_pose_from_video_file(input_path)
        else:
            print(json.dumps({"error": "Invalid mode. Use 'frames' or 'video'"}))
            return
        
        # Return pose sequence as JSON
        result = {
            "success": True,
            "pose_sequence": sequence,
            "sequence_length": len(sequence),
            "feature_dimension": len(sequence[0]) if sequence else 0
        }
        print(json.dumps(result))
        
    except Exception as e:
        error_result = {
            "success": False,
            "error": str(e)
        }
        print(json.dumps(error_result))

if __name__ == "__main__":
    main()
