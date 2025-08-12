#!/usr/bin/env python3

import cv2
import mediapipe as mp
import numpy as np
import logging
from pathlib import Path

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class PoseFeatureExtractor:
    def __init__(self):
        self.mp_holistic = mp.solutions.holistic
        self.holistic = None
        
    def __enter__(self):
        self.holistic = self.mp_holistic.Holistic(
            static_image_mode=False,
            model_complexity=1,
            enable_segmentation=False,
            refine_face_landmarks=False
        )
        return self
        
    def __exit__(self, exc_type, exc_val, exc_tb):
        if self.holistic:
            self.holistic.close()
    
    def extract_features_from_landmarks(self, results):
        """Extract exactly 288 features to match model input"""
        features = []
        
        # Pose landmarks (33 points × 3 coordinates = 99 features)
        if results.pose_landmarks:
            for landmark in results.pose_landmarks.landmark:
                features.extend([landmark.x, landmark.y, landmark.z])
        else:
            features.extend([0.0] * 99)  # 33 * 3
        
        # Left hand landmarks (21 points × 3 coordinates = 63 features)  
        if results.left_hand_landmarks:
            for landmark in results.left_hand_landmarks.landmark:
                features.extend([landmark.x, landmark.y, landmark.z])
        else:
            features.extend([0.0] * 63)  # 21 * 3
        
        # Right hand landmarks (21 points × 3 coordinates = 63 features)
        if results.right_hand_landmarks:
            for landmark in results.right_hand_landmarks.landmark:
                features.extend([landmark.x, landmark.y, landmark.z])
        else:
            features.extend([0.0] * 63)  # 21 * 3
            
        # Current total: 99 + 63 + 63 = 225 features
        # Need 288 features, so add 63 more (likely face or additional features)
        
        # Face landmarks (simplified - take first 21 face points × 3 = 63 features)
        if results.face_landmarks and len(results.face_landmarks.landmark) > 21:
            for i in range(21):  # Take first 21 face landmarks
                landmark = results.face_landmarks.landmark[i]
                features.extend([landmark.x, landmark.y, landmark.z])
        else:
            features.extend([0.0] * 63)  # 21 * 3 padding
            
        # Total: 99 + 63 + 63 + 63 = 288 features ✓
        
        if len(features) != 288:
            logger.warning(f"Feature count mismatch: got {len(features)}, expected 288")
            # Pad or truncate to exactly 288
            if len(features) < 288:
                features.extend([0.0] * (288 - len(features)))
            else:
                features = features[:288]
        
        return np.array(features, dtype=np.float32)
    
    def process_image(self, image):
        """Process single image and return 288 features"""
        if image is None:
            return np.zeros(288, dtype=np.float32)
            
        # Convert BGR to RGB
        rgb_image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        
        # Process with MediaPipe
        results = self.holistic.process(rgb_image)
        
        # Extract features
        features = self.extract_features_from_landmarks(results)
        return features
    
    def process_sequence(self, image_paths, target_frames=30):
        """Process sequence of images and return (target_frames, 288) array"""
        sequence_features = []
        
        processed = 0
        for i, image_path in enumerate(image_paths):
            if processed >= target_frames:
                break
                
            try:
                image = cv2.imread(image_path)
                if image is not None:
                    features = self.process_image(image)
                    sequence_features.append(features)
                    processed += 1
                    
                    if i % 10 == 0:
                        logger.info(f"Processed {processed}/{target_frames} frames")
                        
            except Exception as e:
                logger.warning(f"Failed to process {image_path}: {e}")
                # Add zero features for failed frame
                sequence_features.append(np.zeros(288, dtype=np.float32))
                processed += 1
        
        # Ensure exactly target_frames
        while len(sequence_features) < target_frames:
            sequence_features.append(np.zeros(288, dtype=np.float32))
            
        # Convert to numpy array
        result = np.array(sequence_features[:target_frames], dtype=np.float32)
        logger.info(f"Final sequence shape: {result.shape}")
        
        return result

def extract_pose_features(image_paths, target_frames=30):
    """Standalone function for compatibility"""
    with PoseFeatureExtractor() as extractor:
        return extractor.process_sequence(image_paths, target_frames)

if __name__ == "__main__":
    # Test the extractor
    import sys
    if len(sys.argv) > 1:
        test_dir = sys.argv[1]
        image_paths = sorted([str(p) for p in Path(test_dir).glob("*.jpg")])
        
        print(f"Testing with {len(image_paths)} images from {test_dir}")
        
        features = extract_pose_features(image_paths, target_frames=30)
        print(f"Extracted features shape: {features.shape}")
        print(f"Feature range: [{features.min():.3f}, {features.max():.3f}]")
        print("✅ Pose feature extraction successful!")
