# Create: python-ai/scripts/test_pose_detection.py
import cv2
import mediapipe as mp
import numpy as np

def test_basic_pose_detection(video_path):
    mp_pose = mp.solutions.pose
    pose = mp_pose.Pose(
        static_image_mode=False,
        model_complexity=1,
        enable_segmentation=False,
        min_detection_confidence=0.5,
        min_tracking_confidence=0.5
    )
    
    cap = cv2.VideoCapture(video_path)
    frame_count = 0
    successful_detections = 0
    
    while cap.read()[0] and frame_count < 30:  # Test first 30 frames
        ret, frame = cap.read()
        if not ret:
            break
            
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = pose.process(rgb_frame)
        
        if results.pose_landmarks:
            successful_detections += 1
            print(f"Frame {frame_count}: Pose detected - {len(results.pose_landmarks.landmark)} landmarks")
        else:
            print(f"Frame {frame_count}: NO pose detected")
            
        frame_count += 1
    
    cap.release()
    detection_rate = (successful_detections / frame_count) * 100
    print(f"\nDetection Summary:")
    print(f"Total frames tested: {frame_count}")
    print(f"Successful detections: {successful_detections}")
    print(f"Detection rate: {detection_rate:.2f}%")
    
    return detection_rate > 70  # Should detect pose in at least 70% of frames

# Test with one of your videos
if __name__ == "__main__":
    video_path = "/media/sayad/Ubuntu-Data/SilentVoice_BD/dataset/bdslw60/archive/balu/U1W220F_trial_0_R.mp4"  # Replace with actual path
    success = test_basic_pose_detection(video_path)
    print(f"Basic pose detection test: {'PASSED' if success else 'FAILED'}")
