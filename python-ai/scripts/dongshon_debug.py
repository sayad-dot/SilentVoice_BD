import cv2
import json
import numpy as np
import mediapipe as mp

def extract_pose_debug(video_path, output_json):
    """Extract pose landmarks frame by frame for debugging"""
    
    mp_pose = mp.solutions.pose
    cap = cv2.VideoCapture(video_path)
    
    pose_landmarks_list = []
    frame_metadata = []
    
    with mp_pose.Pose(
        static_image_mode=False, 
        min_detection_confidence=0.5, 
        min_tracking_confidence=0.5
    ) as pose:
        
        frame_num = 0
        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                break
                
            # Convert BGR to RGB
            image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            image.flags.writeable = False
            
            # Extract pose
            results = pose.process(image)
            
            frame_data = {
                'frame_number': frame_num,
                'has_pose': results.pose_landmarks is not None
            }
            
            if results.pose_landmarks:
                landmarks = results.pose_landmarks.landmark
                landmarks_array = [[lm.x, lm.y, lm.z, lm.visibility] for lm in landmarks]
                pose_landmarks_list.append(landmarks_array)
                frame_data['landmark_count'] = len(landmarks_array)
            else:
                pose_landmarks_list.append(None)
                frame_data['landmark_count'] = 0
                
            frame_metadata.append(frame_data)
            frame_num += 1
    
    cap.release()
    
    # Save extracted data
    debug_data = {
        'pose_landmarks': pose_landmarks_list,
        'frame_metadata': frame_metadata,
        'total_frames': frame_num
    }
    
    with open(output_json, 'w') as f:
        json.dump(debug_data, f, indent=2)
    
    print(f"Extracted {frame_num} frames, saved to {output_json}")
    return debug_data

# Run extraction
video_path = "/media/sayad/Ubuntu-Data/archive/dongson/U1W359F_trial_2_L.mp4"
current_data = extract_pose_debug(video_path, "dongshon_pose_current.json")
