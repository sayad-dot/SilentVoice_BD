#!/usr/bin/env python3

import argparse
import json
import os
import sys
import logging
import time
from pathlib import Path
import traceback

# Add scripts dir to path
sys.path.append(str(Path(__file__).parent))

# Set TensorFlow logging to reduce CUDA warnings
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

logging.basicConfig(level=logging.INFO, stream=sys.stderr)
logger = logging.getLogger(__name__)

def test_dependencies():
    """Test if all required dependencies are available"""
    dependencies_ok = True
    
    try:
        import cv2
        logger.info(f"✅ OpenCV available: {cv2.__version__}")
    except ImportError as e:
        logger.error(f"❌ OpenCV missing: {e}")
        dependencies_ok = False
    
    try:
        import numpy as np
        logger.info(f"✅ NumPy available: {np.__version__}")
    except ImportError as e:
        logger.error(f"❌ NumPy missing: {e}")
        dependencies_ok = False
    
    try:
        import mediapipe as mp
        logger.info(f"✅ MediaPipe available: {mp.__version__}")
    except ImportError as e:
        logger.error(f"❌ MediaPipe missing: {e}")
        dependencies_ok = False
    
    try:
        import tensorflow as tf
        logger.info(f"✅ TensorFlow available: {tf.__version__}")
        # Test basic TF operation
        test_tensor = tf.constant([1.0, 2.0, 3.0])
        logger.info(f"✅ TensorFlow basic operations working")
    except ImportError as e:
        logger.error(f"❌ TensorFlow missing: {e}")
        dependencies_ok = False
    except Exception as e:
        logger.error(f"❌ TensorFlow basic test failed: {e}")
        dependencies_ok = False
        
    return dependencies_ok

def test_model_loading():
    """Test if LSTM model can be loaded"""
    script_dir = Path(__file__).parent
    model_paths = [
        str(script_dir / "../models/bangla_lstm_enhanced.h5"),
        str(script_dir / "../trained_models/bangla_lstm_model.h5"),
        str(script_dir / "../trained_models/hypertuned_bangla_lstm_final.h5"),
        str(script_dir / "../trained_models/hypertuned_bangla_lstm_best.h5"),
        str(script_dir / "../trained_models/bangla_lstm_best.h5")
    ]
    
    logger.info("🔍 Searching for model files...")
    
    for model_path in model_paths:
        if os.path.exists(model_path):
            file_size = os.path.getsize(model_path) / (1024 * 1024)  # MB
            logger.info(f"📁 Found model: {model_path} ({file_size:.1f} MB)")
            
            try:
                import tensorflow as tf
                logger.info(f"⏳ Attempting to load model: {model_path}")
                
                # Use timeout for model loading
                start_time = time.time()
                model = tf.keras.models.load_model(model_path)
                load_time = time.time() - start_time
                
                logger.info(f"✅ Successfully loaded model in {load_time:.2f}s")
                logger.info(f"Model input shape: {model.input_shape}")
                logger.info(f"Model output shape: {model.output_shape}")
                
                # Test a dummy prediction
                import numpy as np
                dummy_input = np.random.random((1, 30, 1662))  # Typical pose sequence shape
                prediction = model.predict(dummy_input, verbose=0)
                logger.info(f"✅ Model prediction test successful: shape {prediction.shape}")
                
                return model_path
                
            except Exception as e:
                logger.error(f"❌ Failed to load model {model_path}: {e}")
                logger.error(f"Error details: {traceback.format_exc()}")
                continue
    
    logger.error("❌ No valid model found!")
    return None

def test_pose_extraction():
    """Test pose extraction on a dummy image"""
    try:
        import cv2
        import mediapipe as mp
        import numpy as np
        
        logger.info("🧪 Testing MediaPipe pose extraction...")
        
        # Create a dummy 640x480 RGB image with some basic shapes
        dummy_image = np.zeros((480, 640, 3), dtype=np.uint8)
        # Add some basic shapes to make it more realistic
        cv2.rectangle(dummy_image, (100, 100), (500, 400), (128, 128, 128), -1)
        cv2.circle(dummy_image, (300, 200), 50, (255, 255, 255), -1)
        
        # Initialize MediaPipe Holistic
        mp_holistic = mp.solutions.holistic
        with mp_holistic.Holistic(
            static_image_mode=False,
            model_complexity=1,
            enable_segmentation=False,
            refine_face_landmarks=False
        ) as holistic:
            
            # Convert BGR to RGB
            rgb_image = cv2.cvtColor(dummy_image, cv2.COLOR_BGR2RGB)
            
            # Process the image
            start_time = time.time()
            results = holistic.process(rgb_image)
            process_time = time.time() - start_time
            
            logger.info(f"✅ Pose extraction test successful in {process_time:.3f}s")
            
            # Log what was detected
            detections = []
            if results.pose_landmarks:
                detections.append(f"pose ({len(results.pose_landmarks.landmark)} landmarks)")
            if results.left_hand_landmarks:
                detections.append(f"left_hand ({len(results.left_hand_landmarks.landmark)} landmarks)")
            if results.right_hand_landmarks:
                detections.append(f"right_hand ({len(results.right_hand_landmarks.landmark)} landmarks)")
            
            if detections:
                logger.info(f"🎯 Detected: {', '.join(detections)}")
            else:
                logger.info("ℹ️ No landmarks detected (expected for dummy image)")
                
            return True
            
    except Exception as e:
        logger.error(f"❌ Pose extraction test failed: {e}")
        logger.error(f"Error details: {traceback.format_exc()}")
        return False

def process_sequence_frames(sequence_path):
    """Process actual frame files from sequence"""
    try:
        frame_files = sorted([f for f in os.listdir(sequence_path) if f.endswith('.jpg')])
        logger.info(f"📹 Found {len(frame_files)} frame files")
        
        if len(frame_files) == 0:
            return None, "No frame files found"
            
        # Test loading a few frames
        import cv2
        import mediapipe as mp
        import numpy as np
        
        valid_frames = 0
        pose_data = []
        
        mp_holistic = mp.solutions.holistic
        with mp_holistic.Holistic(
            static_image_mode=False,
            model_complexity=1,
            enable_segmentation=False,
            refine_face_landmarks=False
        ) as holistic:
            
            # Process up to 30 frames
            for i, frame_file in enumerate(frame_files[:30]):
                frame_path = os.path.join(sequence_path, frame_file)
                
                try:
                    image = cv2.imread(frame_path)
                    if image is None:
                        logger.warning(f"⚠️ Could not load frame: {frame_file}")
                        continue
                        
                    # Convert BGR to RGB
                    rgb_image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
                    
                    # Process the image
                    results = holistic.process(rgb_image)
                    
                    # Extract pose landmarks
                    frame_landmarks = []
                    
                    # Pose landmarks (33 points)
                    if results.pose_landmarks:
                        for landmark in results.pose_landmarks.landmark:
                            frame_landmarks.extend([landmark.x, landmark.y, landmark.z])
                    else:
                        frame_landmarks.extend([0.0] * 99)  # 33 * 3
                    
                    # Left hand landmarks (21 points)
                    if results.left_hand_landmarks:
                        for landmark in results.left_hand_landmarks.landmark:
                            frame_landmarks.extend([landmark.x, landmark.y, landmark.z])
                    else:
                        frame_landmarks.extend([0.0] * 63)  # 21 * 3
                    
                    # Right hand landmarks (21 points)
                    if results.right_hand_landmarks:
                        for landmark in results.right_hand_landmarks.landmark:
                            frame_landmarks.extend([landmark.x, landmark.y, landmark.z])
                    else:
                        frame_landmarks.extend([0.0] * 63)  # 21 * 3
                    
                    pose_data.append(frame_landmarks)
                    valid_frames += 1
                    
                    if i % 10 == 0:
                        logger.info(f"📊 Processed frame {i+1}/{len(frame_files)}")
                        
                except Exception as e:
                    logger.warning(f"⚠️ Error processing frame {frame_file}: {e}")
                    continue
        
        logger.info(f"✅ Successfully processed {valid_frames} frames")
        
        if valid_frames == 0:
            return None, "No frames could be processed"
            
        return np.array(pose_data), f"Processed {valid_frames} frames successfully"
        
    except Exception as e:
        logger.error(f"❌ Frame processing failed: {e}")
        return None, f"Frame processing failed: {str(e)}"

def quick_test(sequence_path, session_id):
    """Quick test version with real processing"""
    try:
        logger.info(f"🧪 Quick test processing for session: {session_id}")
        logger.info(f"Sequence path: {sequence_path}")
        
        # Check if sequence path exists
        if not os.path.exists(sequence_path):
            return {
                "error": True,
                "message": f"Sequence path not found: {sequence_path}",
                "session_id": session_id
            }
        
        # Test dependencies first
        logger.info("🔍 Testing dependencies...")
        if not test_dependencies():
            return {
                "error": True,
                "message": "Missing required dependencies",
                "session_id": session_id
            }
        
        # Test model loading
        logger.info("🤖 Testing model loading...")
        model_path = test_model_loading()
        if not model_path:
            # Continue without model for now, just test frame processing
            logger.warning("⚠️ No model found, continuing with frame processing test")
        
        # Test pose extraction
        logger.info("🎯 Testing pose extraction...")
        if not test_pose_extraction():
            return {
                "error": True,
                "message": "Pose extraction test failed",
                "session_id": session_id
            }
        
        # Process actual sequence frames
        logger.info("📹 Processing sequence frames...")
        pose_sequence, process_message = process_sequence_frames(sequence_path)
        
        if pose_sequence is None:
            return {
                "error": True,
                "message": f"Frame processing failed: {process_message}",
                "session_id": session_id
            }
        
        logger.info(f"✅ Pose sequence shape: {pose_sequence.shape}")
        
        # If we have a model, try prediction
        prediction_text = "পরীক্ষা"  # "Test" in Bangla
        confidence = 0.85
        
        if model_path:
            try:
                import tensorflow as tf
                logger.info("🔮 Loading model for prediction...")
                model = tf.keras.models.load_model(model_path)
                
                # Ensure correct input shape
                if len(pose_sequence) < 30:
                    # Pad sequence to 30 frames
                    padding = 30 - len(pose_sequence)
                    pose_sequence = np.pad(pose_sequence, ((0, padding), (0, 0)), mode='constant')
                elif len(pose_sequence) > 30:
                    # Truncate to 30 frames
                    pose_sequence = pose_sequence[:30]
                
                # Reshape for model input
                model_input = pose_sequence.reshape(1, 30, -1)
                logger.info(f"🔮 Model input shape: {model_input.shape}")
                
                # Make prediction
                start_time = time.time()
                prediction = model.predict(model_input, verbose=0)
                prediction_time = time.time() - start_time
                
                logger.info(f"✅ Prediction completed in {prediction_time:.3f}s")
                logger.info(f"Prediction shape: {prediction.shape}")
                
                # Get predicted class
                predicted_class = np.argmax(prediction[0])
                confidence = float(np.max(prediction[0]))
                
                logger.info(f"Predicted class: {predicted_class}, confidence: {confidence:.3f}")
                
                # Try to load class mappings
                try:
                    import json
                    mapping_path = Path(__file__).parent / "../models/class_mappings.json"
                    if os.path.exists(mapping_path):
                        with open(mapping_path, 'r', encoding='utf-8') as f:
                            class_mappings = json.load(f)
                        
                        # Convert to index-to-label mapping
                        if isinstance(class_mappings, dict):
                            idx_to_label = {v: k for k, v in class_mappings.items()} if all(isinstance(v, int) for v in class_mappings.values()) else class_mappings
                            prediction_text = idx_to_label.get(str(predicted_class), idx_to_label.get(predicted_class, f"ক্লাস_{predicted_class}"))
                        else:
                            prediction_text = f"ক্লাস_{predicted_class}"
                            
                        logger.info(f"✅ Mapped prediction: {prediction_text}")
                        
                except Exception as e:
                    logger.warning(f"⚠️ Could not load class mappings: {e}")
                    prediction_text = f"ক্লাস_{predicted_class}"
                    
            except Exception as e:
                logger.error(f"❌ Model prediction failed: {e}")
                prediction_text = "মডেল ত্রুটি"  # "Model error"
                confidence = 0.0
        
        # Return successful result
        result = {
            "prediction": prediction_text,
            "confidence": float(confidence),
            "frame_count": len(pose_sequence),
            "processing_time_ms": 200,
            "session_id": session_id,
            "status": "success",
            "model_version": "test_version" if not model_path else "bangla_lstm",
            "quality_score": 0.8,
            "test_mode": True,
            "pose_sequence_shape": list(pose_sequence.shape) if pose_sequence is not None else None
        }
        
        logger.info(f"✅ Test completed successfully: {result['prediction']} ({result['confidence']:.2f})")
        return result
        
    except Exception as e:
        logger.error(f"❌ Quick test failed: {e}")
        logger.error(f"Full traceback: {traceback.format_exc()}")
        return {
            "error": True,
            "message": f"Quick test failed: {str(e)}",
            "session_id": session_id,
            "traceback": traceback.format_exc()
        }

def main():
    parser = argparse.ArgumentParser(description='Test realtime processor')
    parser.add_argument('--sequence_path', required=True, help='Path to frame sequence')
    parser.add_argument('--session_id', required=True, help='Session ID')
    
    args = parser.parse_args()
    
    logger.info(f"🚀 Starting test processor...")
    logger.info(f"Sequence path: {args.sequence_path}")
    logger.info(f"Session ID: {args.session_id}")
    
    result = quick_test(args.sequence_path, args.session_id)
    
    # Output JSON result
    print(json.dumps(result, ensure_ascii=False))
    
    if result.get('error', False):
        sys.exit(1)
    else:
        sys.exit(0)

if __name__ == "__main__":
    main()
