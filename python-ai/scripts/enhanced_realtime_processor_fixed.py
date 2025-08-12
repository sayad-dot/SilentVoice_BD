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

# Import components
try:
    from pose_feature_extractor import PoseFeatureExtractor
except ImportError:
    logging.error("❌ pose_feature_extractor.py not found!")
    sys.exit(1)

# Set TensorFlow logging
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

logging.basicConfig(level=logging.INFO, stream=sys.stderr)
logger = logging.getLogger(__name__)

class FixedBDSLW60Processor:
    def __init__(self):
        self.pose_extractor = None
        self.class_mappings = {}
        self.model = None
        
    def initialize(self):
        """Initialize with correct BDSLW60 mappings"""
        try:
            # Initialize pose extractor
            self.pose_extractor = PoseFeatureExtractor()
            logger.info("✅ Pose extractor initialized")
            
            # Load correct class mappings
            self.load_bdslw60_mappings()
            
            # Load working model
            self.load_working_model()
            
            return True
            
        except Exception as e:
            logger.error(f"❌ Initialization failed: {e}")
            return False
    
    def load_bdslw60_mappings(self):
        """Load correct BDSLW60 class mappings"""
        script_dir = Path(__file__).parent
        
        # Try to load existing mappings
        mapping_paths = [
            script_dir / "../models/bdslw60_class_mappings.json",
            script_dir / "../models/class_mappings.json",
            script_dir / "../trained_models/class_mappings.json"
        ]
        
        for mapping_path in mapping_paths:
            if mapping_path.exists():
                try:
                    with open(mapping_path, 'r', encoding='utf-8') as f:
                        loaded_mappings = json.load(f)
                    
                    # Extract the index_to_class mapping
                    if 'index_to_class' in loaded_mappings:
                        self.class_mappings = loaded_mappings
                        logger.info(f"✅ Loaded BDSLW60 mappings from: {mapping_path}")
                        logger.info(f"Total classes: {len(self.class_mappings.get('index_to_class', {}))}")
                        return
                    else:
                        self.class_mappings = {'index_to_class': loaded_mappings}
                        logger.info(f"✅ Loaded basic mappings from: {mapping_path}")
                        return
                        
                except Exception as e:
                    logger.warning(f"⚠️ Failed to load mappings from {mapping_path}: {e}")
                    continue
        
        # Create default BDSLW60 mappings if none found
        logger.warning("⚠️ No existing mappings found, creating default BDSLW60 mappings")
        self.create_default_bdslw60_mappings()
    
    def create_default_bdslw60_mappings(self):
        """Create default BDSLW60 mappings"""
        bdslw60_classes = [
            "aam", "aaple", "ac", "aids", "alu", "anaros", "angur", "apartment",
            "attio", "audio cassette", "ayna", "baandej", "baat", "baba",
            "balti", "balu", "bhai", "biscuts", "bon", "boroi", "bottam",
            "bou", "cake", "capsule", "cha", "chacha", "chachi", "chadar",
            "chal", "chikissha", "chini", "chips", "chiruni", "chocolate",
            "chokh utha", "chosma", "churi", "clip", "cream", "dada",
            "dadi", "daeitto", "dal", "debor", "denadar", "dengue",
            "doctor", "dongson", "dulavai", "durbol", "jomoj", "juta",
            "konna", "maa", "tattha", "toothpaste", "tshirt", "tubelight",
            "tupi", "tv"
        ]
        
        bangla_translations = {
            "aam": "আম", "aaple": "আপেল", "ac": "এসি", "aids": "এইডস", "alu": "আলু",
            "anaros": "আনারস", "angur": "আঙুর", "apartment": "অ্যাপার্টমেন্ট", "attio": "আত্তিও",
            "audio cassette": "অডিও ক্যাসেট", "ayna": "আয়না", "baandej": "ব্যান্ডেজ",
            "baat": "বাত", "baba": "বাবা", "balti": "বালতি", "balu": "বালু", "bhai": "ভাই",
            "biscuts": "বিস্কুট", "bon": "বোন", "boroi": "বরই", "bottam": "বোতাম", "bou": "বউ",
            "cake": "কেক", "capsule": "ক্যাপসুল", "cha": "চা", "chacha": "চাচা", "chachi": "চাচি",
            "chadar": "চাদর", "chal": "চাল", "chikissha": "চিকিৎসা", "chini": "চিনি",
            "chips": "চিপস", "chiruni": "চিরুনি", "chocolate": "চকলেট", "chokh utha": "চোখ ওঠা",
            "chosma": "চশমা", "churi": "চুড়ি", "clip": "ক্লিপ", "cream": "ক্রিম", "dada": "দাদা",
            "dadi": "দাদি", "daeitto": "দৈত্য", "dal": "দাল", "debor": "দেবর", "denadar": "দেনাদার",
            "dengue": "ডেঙ্গু", "doctor": "ডাক্তার", "dongson": "দংশন", "dulavai": "দুলাভাই",
            "durbol": "দুর্বল", "jomoj": "জমজ", "juta": "জুতা", "konna": "কন্যা", "maa": "মা",
            "tattha": "তত্ত্ব", "toothpaste": "টুথপেস্ট", "tshirt": "টি-শার্ট",
            "tubelight": "টিউবলাইট", "tupi": "টুপি", "tv": "টিভি"
        }
        
        # Create comprehensive mappings
        index_to_class = {i: class_name for i, class_name in enumerate(bdslw60_classes)}
        
        self.class_mappings = {
            "index_to_class": index_to_class,
            "english_to_bangla": bangla_translations,
            "total_classes": len(bdslw60_classes),
            "dataset": "BDSLW60"
        }
        
        logger.info(f"✅ Created default BDSLW60 mappings for {len(bdslw60_classes)} classes")
    
    def load_working_model(self):
        """Load a working TensorFlow model"""
        script_dir = Path(__file__).parent
        model_paths = [
            str(script_dir / "../trained_models/bangla_lstm_model.h5"),
            str(script_dir / "../trained_models/bangla_lstm_best.h5"),
            str(script_dir / "../models/bangla_lstm_enhanced.h5")
        ]
        
        for model_path in model_paths:
            if os.path.exists(model_path):
                try:
                    import tensorflow as tf
                    logger.info(f"🤖 Loading model: {model_path}")
                    
                    self.model = tf.keras.models.load_model(model_path)
                    
                    # Test with correct input shape
                    test_input = np.random.random((1, 30, 288)).astype(np.float32)
                    test_prediction = self.model.predict(test_input, verbose=0)
                    
                    logger.info(f"✅ Model loaded successfully: {model_path}")
                    logger.info(f"Input shape: {self.model.input_shape}")
                    logger.info(f"Output shape: {self.model.output_shape}")
                    logger.info(f"Model outputs {test_prediction.shape[1]} classes")
                    
                    return model_path
                    
                except Exception as e:
                    logger.warning(f"❌ Failed to load model {model_path}: {str(e)[:100]}...")
                    continue
        
        logger.error("❌ No working model found!")
        return None
    
    def predict_sign(self, pose_sequence):
        """Make prediction with correct BDSLW60 class mapping"""
        try:
            if self.model is None:
                raise ValueError("No model loaded")
            
            # Ensure correct input shape
            if len(pose_sequence.shape) == 2:
                model_input = pose_sequence.reshape(1, pose_sequence.shape[0], pose_sequence.shape[1])
            else:
                model_input = pose_sequence
                
            logger.info(f"🔮 Model input shape: {model_input.shape}")
            
            # Make prediction
            start_time = time.time()
            prediction = self.model.predict(model_input, verbose=0)
            prediction_time = time.time() - start_time
            
            # Get predicted class and confidence
            predicted_class_idx = np.argmax(prediction[0])
            confidence = float(np.max(prediction[0]))
            
            logger.info(f"✅ Raw prediction: class {predicted_class_idx}, confidence {confidence:.3f}")
            
            # Map to correct BDSLW60 class
            predicted_text = self.map_class_to_bdslw60_text(predicted_class_idx)
            
            logger.info(f"🎯 BDSLW60 mapping: class {predicted_class_idx} -> '{predicted_text}'")
            
            return {
                "success": True,
                "predicted_text": predicted_text,
                "confidence": confidence,
                "predicted_class": int(predicted_class_idx),
                "processing_time_ms": int(prediction_time * 1000),
                "model_type": "BDSLW60_lstm"
            }
            
        except Exception as e:
            logger.error(f"❌ Prediction failed: {e}")
            return {
                "success": False,
                "predicted_text": "ভবিষ্যৎবাণী ব্যর্থ",
                "confidence": 0.0,
                "error": str(e)
            }
    
    def map_class_to_bdslw60_text(self, class_idx):
        """Map class index to correct BDSLW60 text"""
        try:
            index_to_class = self.class_mappings.get('index_to_class', {})
            english_to_bangla = self.class_mappings.get('english_to_bangla', {})
            
            # Get English class name
            english_class = None
            if str(class_idx) in index_to_class:
                english_class = index_to_class[str(class_idx)]
            elif class_idx in index_to_class:
                english_class = index_to_class[class_idx]
            
            if english_class:
                # Get Bangla translation
                bangla_text = english_to_bangla.get(english_class, english_class)
                logger.info(f"✅ Mapped: {class_idx} -> {english_class} -> {bangla_text}")
                return bangla_text
            else:
                logger.warning(f"⚠️ No mapping found for class index: {class_idx}")
                return f"অজানা_ক্লাস_{class_idx}"
                
        except Exception as e:
            logger.error(f"❌ Class mapping failed: {e}")
            return f"ত্রুটি_{class_idx}"
    
    def process_sequence(self, sequence_path, session_id):
        """Process sequence with correct BDSLW60 mappings"""
        try:
            logger.info(f"🎬 Processing BDSLW60 sequence for session: {session_id}")
            
            if not os.path.exists(sequence_path):
                raise FileNotFoundError(f"Sequence path not found: {sequence_path}")
            
            # Get frame files
            frame_files = sorted([f for f in os.listdir(sequence_path) if f.endswith('.jpg')])
            if len(frame_files) == 0:
                raise ValueError("No frame files found")
                
            logger.info(f"📹 Found {len(frame_files)} frame files")
            
            # Extract pose features
            image_paths = [os.path.join(sequence_path, f) for f in frame_files]
            
            logger.info("🎯 Extracting pose features...")
            with self.pose_extractor as extractor:
                pose_sequence = extractor.process_sequence(image_paths, target_frames=30)
            
            logger.info(f"✅ Pose sequence extracted: {pose_sequence.shape}")
            
            # Make prediction with BDSLW60 mapping
            logger.info("🔮 Making BDSLW60 prediction...")
            pred_result = self.predict_sign(pose_sequence)
            
            # Format result
            result = {
                "prediction": pred_result.get("predicted_text", "ত্রুটি"),
                "confidence": float(pred_result.get("confidence", 0.0)),
                "frame_count": pose_sequence.shape[0],
                "processing_time_ms": pred_result.get("processing_time_ms", 100),
                "session_id": session_id,
                "status": "success" if pred_result.get("success", False) else "error",
                "model_version": "BDSLW60_lstm",
                "quality_score": min(1.0, float(pred_result.get("confidence", 0.0)) + 0.1),
                "predicted_class": pred_result.get("predicted_class", -1),
                "dataset": "BDSLW60"
            }
            
            logger.info(f"🎯 Final BDSLW60 result: '{result['prediction']}' ({result['confidence']:.2f})")
            return result
            
        except Exception as e:
            logger.error(f"❌ BDSLW60 processing failed: {e}")
            logger.error(f"Traceback: {traceback.format_exc()}")
            
            return {
                "error": True,
                "message": f"BDSLW60 processing failed: {str(e)}",
                "session_id": session_id,
                "status": "error",
                "prediction": "ত্রুটি",
                "confidence": 0.0
            }

def process_bdslw60_sequence(sequence_path, session_id):
    """Main processing function with correct BDSLW60 mappings"""
    processor = FixedBDSLW60Processor()
    
    if not processor.initialize():
        return {
            "error": True,
            "message": "Failed to initialize BDSLW60 processor",
            "session_id": session_id
        }
    
    return processor.process_sequence(sequence_path, session_id)

def main():
    parser = argparse.ArgumentParser(description='Fixed BDSLW60 real-time processor')
    parser.add_argument('--sequence_path', required=True, help='Path to frame sequence directory')
    parser.add_argument('--session_id', required=True, help='Session ID for tracking')
    
    args = parser.parse_args()
    
    logger.info(f"🚀 Starting fixed BDSLW60 processor...")
    logger.info(f"Sequence path: {args.sequence_path}")
    logger.info(f"Session ID: {args.session_id}")
    
    result = process_bdslw60_sequence(args.sequence_path, args.session_id)
    
    # Output JSON result
    print(json.dumps(result, ensure_ascii=False))
    
    if result.get('error', False):
        sys.exit(1)
    else:
        sys.exit(0)

if __name__ == "__main__":
    main()
