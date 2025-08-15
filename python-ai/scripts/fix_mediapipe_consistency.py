#!/usr/bin/env python3
# Save as: fix_mediapipe_consistency.py

import cv2
import mediapipe as mp
import numpy as np
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class FixedMediaPipePoseExtractor:
    """Fixed MediaPipe extractor with consistent configuration"""
    
    def __init__(self):
        self.mp_holistic = mp.solutions.holistic
        # Use exact same configuration as your training
        self.holistic = self.mp_holistic.Holistic(
            static_image_mode=False,
            model_complexity=1,  # Consistent complexity
            enable_segmentation=False,
            min_detection_confidence=0.7,  # Higher threshold
            min_tracking_confidence=0.7
        )
    
    def extract_consistent_features(self, image_path):
        """Extract features with consistent preprocessing"""
        
        # Load image
        image = cv2.imread(image_path)
        if image is None:
            return None
        
        # Consistent preprocessing
        h, w = image.shape[:2]
        if w > 640:  # Standardize input size
            scale = 640.0 / w
            new_w, new_h = int(w * scale), int(h * scale)
            image = cv2.resize(image, (new_w, new_h))
        
        # Convert BGR to RGB
        rgb_image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        rgb_image.flags.writeable = False
        
        # Process with MediaPipe
        results = self.holistic.process(rgb_image)
        
        # Extract exactly 288 features as in training
        features = self._extract_288_features(results)
        
        return features
    
    def _extract_288_features(self, results):
        """Extract exactly 288 features to match training"""
        
        features = []
        
        # Left hand (21 * 3 = 63 features)
        if results.left_hand_landmarks:
            for landmark in results.left_hand_landmarks.landmark:
                features.extend([landmark.x, landmark.y, landmark.z])
        else:
            features.extend([0.0] * 63)
        
        # Right hand (21 * 3 = 63 features)  
        if results.right_hand_landmarks:
            for landmark in results.right_hand_landmarks.landmark:
                features.extend([landmark.x, landmark.y, landmark.z])
        else:
            features.extend([0.0] * 63)
        
        # Pose (33 * 4 = 132 features)
        if results.pose_landmarks:
            for landmark in results.pose_landmarks.landmark:
                features.extend([landmark.x, landmark.y, landmark.z, landmark.visibility])
        else:
            features.extend([0.0] * 132)
        
        # Face (10 * 3 = 30 features)  
        if results.face_landmarks and len(results.face_landmarks.landmark) >= 10:
            for i in range(10):
                landmark = results.face_landmarks.landmark[i]
                features.extend([landmark.x, landmark.y, landmark.z])
        else:
            features.extend([0.0] * 30)
        
        # Ensure exactly 288 features
        if len(features) != 288:
            logger.warning(f"Feature count mismatch: {len(features)} != 288")
            if len(features) < 288:
                features.extend([0.0] * (288 - len(features)))
            else:
                features = features[:288]
        
        return np.array(features, dtype=np.float32)

def test_fixed_extraction(video_path):
    """Test the fixed extraction on your dongshon video"""
    
    extractor = FixedMediaPipePoseExtractor()
    
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        print(f"❌ Cannot open video: {video_path}")
        return
    
    sequences = []
    frame_count = 0
    
    # Extract exactly 30 frames
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    frame_indices = np.linspace(0, total_frames-1, 30, dtype=int)
    
    for frame_idx in frame_indices:
        cap.set(cv2.CAP_PROP_POS_FRAMES, frame_idx)
        ret, frame = cap.read()
        if not ret:
            break
        
        # Save frame temporarily
        temp_path = f'/tmp/temp_frame_{frame_count}.jpg'
        cv2.imwrite(temp_path, frame)
        
        # Extract features
        features = extractor.extract_consistent_features(temp_path)
        if features is not None:
            sequences.append(features.tolist())
        
        # Cleanup
        os.remove(temp_path)
        frame_count += 1
    
    cap.release()
    
    if len(sequences) < 30:
        # Pad with zeros if needed
        while len(sequences) < 30:
            sequences.append([0.0] * 288)
    
    sequences = sequences[:30]  # Ensure exactly 30 frames
    
    print(f"✅ Extracted {len(sequences)} sequences")
    print(f"   Feature range: [{np.array(sequences).min():.6f}, {np.array(sequences).max():.6f}]")
    
    return sequences

if __name__ == "__main__":
    dongshon_video = input("Enter path to dongshon video: ").strip()
    if os.path.exists(dongshon_video):
        sequences = test_fixed_extraction(dongshon_video)
        
        # Save for testing
        with open('fixed_dongshon_features.json', 'w') as f:
            json.dump({'sequences': sequences}, f)
        
        print("✅ Fixed features saved to 'fixed_dongshon_features.json'")
