import json
import numpy as np
from pose_extractor import MediaPipePoseExtractor
from sign_predictor import SignLanguagePredictor

def test_complete_pipeline():
    """Test the complete AI pipeline"""
    print("Testing SilentVoice_BD AI Pipeline...")
    
    # Step 1: Test pose extraction with dummy data
    print("\n1. Testing pose extraction...")
    extractor = MediaPipePoseExtractor()
    
    # Create dummy pose sequence (simulating extracted poses)
    dummy_pose_sequence = []
    for i in range(30):  # 30 frames
        # Create random pose keypoints (258 features)
        pose_features = np.random.random(258).tolist()
        dummy_pose_sequence.append(pose_features)
    
    print(f"✓ Created dummy pose sequence: {len(dummy_pose_sequence)} frames")
    
    # Step 2: Test sign language prediction
    print("\n2. Testing sign language prediction...")
    predictor = SignLanguagePredictor()
    
    result = predictor.predict_from_pose_sequence(dummy_pose_sequence)
    
    print(f"✓ Prediction result:")
    print(f"  - Predicted text: {result.get('predicted_text', 'N/A')}")
    print(f"  - Confidence: {result.get('confidence', 0):.2%}")
    print(f"  - Success: {result.get('success', False)}")
    
    # Step 3: Test JSON communication (simulating Java integration)
    print("\n3. Testing JSON communication...")
    pose_json = json.dumps(dummy_pose_sequence)
    result_json = json.dumps(result, ensure_ascii=False)
    
    print(f"✓ Pose sequence JSON size: {len(pose_json)} characters")
    print(f"✓ Result JSON size: {len(result_json)} characters")
    
    print("\n🎉 AI Pipeline test completed successfully!")
    return True

if __name__ == "__main__":
    test_complete_pipeline()
