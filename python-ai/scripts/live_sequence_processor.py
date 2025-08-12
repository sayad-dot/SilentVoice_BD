#!/usr/bin/env python3

import argparse
import json
import os
import sys
import logging
import time
from pathlib import Path
import traceback
import numpy as np

# Add scripts dir to path
sys.path.append(str(Path(__file__).parent))

# Import our fixed pose extractor
try:
    from pose_feature_extractor import PoseFeatureExtractor
except ImportError:
    logger.error("❌ pose_feature_extractor.py not found!")
    sys.exit(1)

# Set TensorFlow logging
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

logging.basicConfig(level=logging.INFO, stream=sys.stderr)
logger = logging.getLogger(__name__)

def find_working_model():
    """Find a model that can actually be loaded and used"""
    script_dir = Path(__file__).parent
    
    # Try models in order of preference
    model_candidates = [
        ("trained_models/bangla_lstm_model.h5", "basic_lstm"),
        ("trained_models/bangla_lstm_best.h5", "best_lstm"), 
        ("models/bangla_lstm_enhanced.h5", "enhanced_lstm"),
    ]
    
    for model_file, model_type in model_candidates:
        model_path = str(script_dir / f"../{model_file}")
        
        if not os.path.exists(model_path):
            continue
            
        try:
            import tensorflow as tf
            logger.info(f"🧪 Testing model: {model_path}")
            
            # Load model
            model = tf.keras.models.load_model(model_path)
            
            # Test with correct input shape (30, 288)
            test_input = np.random.random((1, 30, 288)).astype(np.float32)
            prediction = model.predict(test_input, verbose=0)
            
            logger.info(f"✅ Working model found: {model_path}")
            logger.info(f"Input shape: {model.input_shape}")
            logger.info(f"Output shape: {model.output_shape}")
            logger.info(f"Test prediction shape: {prediction.shape}")
            
            return model_path, model_type, model
            
        except Exception as e:
            logger.warning(f"❌ Model {model_path} failed: {str(e)[:100]}...")
            continue
    
    logger.error("❌ No working model found!")
    return None, None, None

def load_class_mappings():
    """Load class mappings for prediction interpretation"""
    script_dir = Path(__file__).parent
    mapping_paths = [
        script_dir / "../models/class_mappings.json",
        script_dir / "../trained_models/class_mappings.json",
        script_dir / "../data/class_mappings.json"
    ]
    
    for mapping_path in mapping_paths:
        if mapping_path.exists():
            try:
                with open(mapping_path, 'r', encoding='utf-8') as f:
                    mappings = json.load(f)
                logger.info(f"✅ Loaded class mappings from: {mapping_path}")
                return mappings
            except Exception as e:
                logger.warning(f"⚠️ Failed to load mappings from {mapping_path}: {e}")
                continue
    
    logger.warning("⚠️ No class mappings found, using class indices")
    return {}

def predict_sign(pose_sequence, model, class_mappings):
    """Make prediction using the model"""
    try:
        # Ensure correct input shape
        if len(pose_sequence.shape) == 2:
            # Add batch dimension: (frames, features) -> (1, frames, features)
            model_input = pose_sequence.reshape(1, pose_sequence.shape[0], pose_sequence.shape[1])
        else:
            model_input = pose_sequence
            
        logger.info(f"🔮 Model input shape: {model_input.shape}")
        
        # Make prediction
        start_time = time.time()
        prediction = model.predict(model_input, verbose=0)
        prediction_time = time.time() - start_time
        
        # Get predicted class and confidence
        predicted_class_idx = np.argmax(prediction[0])
        confidence = float(np.max(prediction[0]))
        
        logger.info(f"✅ Prediction completed in {prediction_time:.3f}s")
        logger.info(f"Predicted class index: {predicted_class_idx}, confidence: {confidence:.3f}")
        
        # Map to class name
        predicted_text = "অজানা"  # "Unknown" in Bangla
        
        if class_mappings:
            # Try different mapping formats
            if str(predicted_class_idx) in class_mappings:
                predicted_text = class_mappings[str(predicted_class_idx)]
            elif predicted_class_idx in class_mappings:
                predicted_text = class_mappings[predicted_class_idx]
            else:
                # Reverse mapping if needed
                reverse_mapping = {v: k for k, v in class_mappings.items() if isinstance(v, int)}
                predicted_text = reverse_mapping.get(predicted_class_idx, f"ক্লাস_{predicted_class_idx}")
        else:
            predicted_text = f"ক্লাস_{predicted_class_idx}"
        
        return {
            "success": True,
            "predicted_text": predicted_text,
            "confidence": confidence,
            "predicted_class": int(predicted_class_idx),
            "processing_time_ms": int(prediction_time * 1000)
        }
        
    except Exception as e:
        logger.error(f"❌ Prediction failed: {e}")
        return {
            "success": False,
            "predicted_text": "ভবিষ্যৎবাণী ব্যর্থ",  # "Prediction failed"
            "confidence": 0.0,
            "error": str(e)
        }

def process_live_sequence(sequence_path, session_id):
    """Process live sequence with correct feature extraction"""
    try:
        logger.info(f"🎬 Processing live sequence for session: {session_id}")
        logger.info(f"Sequence path: {sequence_path}")
        
        # Check sequence path
        if not os.path.exists(sequence_path):
            raise FileNotFoundError(f"Sequence path not found: {sequence_path}")
        
        # Get frame files
        frame_files = sorted([f for f in os.listdir(sequence_path) if f.endswith('.jpg')])
        if len(frame_files) == 0:
            raise ValueError("No frame files found in sequence directory")
            
        logger.info(f"📹 Found {len(frame_files)} frame files")
        
        # Extract pose features with correct dimensions
        image_paths = [os.path.join(sequence_path, f) for f in frame_files]
        
        logger.info("🎯 Extracting pose features...")
        with PoseFeatureExtractor() as extractor:
            pose_sequence = extractor.process_sequence(image_paths, target_frames=30)
        
        logger.info(f"✅ Pose sequence extracted: {pose_sequence.shape}")
        
        # Find working model
        logger.info("🤖 Finding working model...")
        model_path, model_type, model = find_working_model()
        
        if model is None:
            # Return without model prediction
            return {
                "prediction": "মডেল উপলব্ধ নয়",  # "Model not available"
                "confidence": 0.0,
                "frame_count": pose_sequence.shape[0],
                "processing_time_ms": 100,
                "session_id": session_id,
                "status": "success_no_model",
                "model_version": "none",
                "quality_score": 0.5
            }
        
        # Load class mappings
        class_mappings = load_class_mappings()
        
        # Make prediction
        logger.info("🔮 Making prediction...")
        pred_result = predict_sign(pose_sequence, model, class_mappings)
        
        # Format result
        result = {
            "prediction": pred_result.get("predicted_text", "ত্রুটি"),
            "confidence": float(pred_result.get("confidence", 0.0)),
            "frame_count": pose_sequence.shape[0],
            "processing_time_ms": pred_result.get("processing_time_ms", 100),
            "session_id": session_id,
            "status": "success" if pred_result.get("success", False) else "error",
            "model_version": model_type,
            "quality_score": min(1.0, float(pred_result.get("confidence", 0.0)) + 0.2),
            "predicted_class": pred_result.get("predicted_class", -1)
        }
        
        logger.info(f"🎯 Final result: '{result['prediction']}' ({result['confidence']:.2f})")
        return result
        
    except Exception as e:
        logger.error(f"❌ Live sequence processing failed: {e}")
        logger.error(f"Full traceback: {traceback.format_exc()}")
        return {
            "error": True,
            "message": f"Processing failed: {str(e)}",
            "session_id": session_id,
            "status": "error",
            "prediction": "ত্রুটি",  # "Error"
            "confidence": 0.0
        }

def main():
    parser = argparse.ArgumentParser(description='Process live sequence for sign recognition')
    parser.add_argument('--sequence_path', required=True, help='Path to frame sequence directory')
    parser.add_argument('--session_id', required=True, help='Session ID for tracking')
    
    args = parser.parse_args()
    
    logger.info(f"🚀 Starting live sequence processor...")
    logger.info(f"Sequence path: {args.sequence_path}")
    logger.info(f"Session ID: {args.session_id}")
    
    result = process_live_sequence(args.sequence_path, args.session_id)
    
    # Output JSON result
    print(json.dumps(result, ensure_ascii=False))
    
    if result.get('error', False):
        sys.exit(1)
    else:
        sys.exit(0)

if __name__ == "__main__":
    main()
