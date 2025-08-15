import tensorflow as tf
import numpy as np
import json
import logging
from pathlib import Path
import os
from typing import Dict, List, Any

logger = logging.getLogger(__name__)

class EnhancedModelPredictor:
    def __init__(self, model_filename='bangla_lstm_enhanced.h5'):
        self.model = None
        self.sequence_length = 30
        self.feature_dim = 288  # From your BDSLW60 config
        self.num_classes = 60   # From your BDSLW60 config
        self.current_sequence = []
        
        # Real BDSLW60 class mappings
        self.english_to_bangla = {
            "aam": "আম", "aaple": "আপেল", "ac": "এসি", "aids": "এইডস", "alu": "আলু",
            "anaros": "আনারস", "angur": "আঙুর", "apartment": "অ্যাপার্টমেন্ট", "attio": "আত্তিও",
            "audio cassette": "অডিও ক্যাসেট", "ayna": "আয়না", "baandej": "ব্যান্ডেজ", "baat": "বাত",
            "baba": "বাবা", "balti": "বালতি", "balu": "বালু", "bhai": "ভাই", "biscuits": "বিস্কুট",
            "bon": "বোন", "boroi": "বড়ই", "button": "বোতাম", "bou": "বউ", "cake": "কেক",
            "capsule": "ক্যাপসুল", "cha": "চা", "chacha": "চাচা", "chachi": "চাচি", "chadar": "চাদর",
            "chal": "চাল", "chikissha": "চিকিৎসা", "chini": "চিনি", "chips": "চিপস", "chiruni": "চিরুনি",
            "chocolate": "চকলেট", "chokh utha": "চোখ উঠা", "chosma": "চশমা", "churi": "চুরি",
            "clip": "ক্লিপ", "cream": "ক্রিম", "dada": "দাদা", "dadi": "দাদি", "daitto": "দায়িত্ব",
            "dal": "দাল", "debor": "দেবর", "denadar": "দেনাদার", "dengue": "ডেঙ্গু", "doctor": "ডাক্তার",
            "dongson": "দংশন", "dulavai": "দুলাভাই", "durbol": "দুর্বল", "jomoj": "জমজ", "juta": "জুতা",
            "konna": "কন্যা", "maa": "মা", "tattha": "তত্ত্ব", "toothpaste": "টুথপেস্ট",
            "tshirt": "টিশার্ট", "tubelight": "টিউবলাইট", "tupi": "টুপি", "tv": "টিভি"
        }
        
        self.bangla_to_english = {v: k for k, v in self.english_to_bangla.items()}
        
        # Class to index mapping from your training
        self.class_to_index = {
            "aam": 0, "aaple": 1, "ac": 2, "aids": 3, "alu": 4, "anaros": 5, "angur": 6,
            "apartment": 7, "attio": 8, "audio cassette": 9, "ayna": 10, "baandej": 11,
            "baat": 12, "baba": 13, "balti": 14, "balu": 15, "bhai": 16, "biscuits": 17,
            "bon": 18, "boroi": 19, "button": 20, "bou": 21, "cake": 22, "capsule": 23,
            "cha": 24, "chacha": 25, "chachi": 26, "chadar": 27, "chal": 28, "chikissha": 29,
            "chini": 30, "chips": 31, "chiruni": 32, "chocolate": 33, "chokh utha": 34,
            "chosma": 35, "churi": 36, "clip": 37, "cream": 38, "dada": 39, "dadi": 40,
            "daitto": 41, "dal": 42, "debor": 43, "denadar": 44, "dengue": 45, "doctor": 46,
            "dongson": 47, "dulavai": 48, "durbol": 49, "jomoj": 50, "juta": 51, "konna": 52,
            "maa": 53, "tattha": 54, "toothpaste": 55, "tshirt": 56, "tubelight": 57, "tupi": 58, "tv": 59
        }
        
        self.index_to_class = {v: k for k, v in self.class_to_index.items()}
        
        # Load the actual trained model
        self.load_model(model_filename)
    
    def load_model(self, model_filename):
        """Load your actual trained LSTM model with corrected paths"""
        try:
            # Get the current file's directory (learning/)
            current_dir = os.path.dirname(os.path.abspath(__file__))
            # Go up one level to python-ai/
            parent_dir = os.path.dirname(current_dir)
            
            # Define possible paths based on your actual structure
            possible_paths = [
                # Direct path from learning/ to models/
                os.path.join(parent_dir, 'models', model_filename),
                # Direct path from learning/ to trained_models/
                os.path.join(parent_dir, 'trained_models', model_filename),
                # Fallback paths
                os.path.join(current_dir, '..', 'models', model_filename),
                os.path.join(current_dir, '..', 'trained_models', model_filename),
                model_filename
            ]
            
            model_loaded = False
            logger.info(f"🔍 Looking for model: {model_filename}")
            
            for path in possible_paths:
                abs_path = os.path.abspath(path)
                logger.info(f"🔍 Checking path: {abs_path}")
                
                if os.path.exists(abs_path):
                    try:
                        logger.info(f"🎯 Found model file at: {abs_path}")
                        self.model = tf.keras.models.load_model(abs_path)
                        logger.info("✅ Successfully loaded BDSLW60 trained model!")
                        logger.info(f"📊 Model input shape: {self.model.input_shape}")
                        logger.info(f"📊 Model output shape: {self.model.output_shape}")
                        model_loaded = True
                        break
                    except Exception as e:
                        logger.error(f"❌ Error loading model from {abs_path}: {e}")
                        continue
                else:
                    logger.debug(f"❌ Model not found at: {abs_path}")
            
            if not model_loaded:
                logger.warning("⚠️ Trained model not found, using fallback mode")
                logger.info(f"🔍 Searched paths:")
                for path in possible_paths:
                    logger.info(f"   - {os.path.abspath(path)} (exists: {os.path.exists(path)})")
                self.model = None
                
        except Exception as e:
            logger.error(f"❌ Error during model loading: {e}")
            self.model = None
    
    def extract_features_from_landmarks(self, landmarks):
        """Extract features matching your training data format"""
        features = []
        
        # Extract all 33 pose landmarks (matching MediaPipe format)
        for i in range(33):
            if i < len(landmarks):
                landmark = landmarks[i]
                if isinstance(landmark, dict):
                    x = landmark.get('x', 0)
                    y = landmark.get('y', 0)
                    z = landmark.get('z', 0)
                    visibility = landmark.get('visibility', 0)
                else:
                    x = getattr(landmark, 'x', 0)
                    y = getattr(landmark, 'y', 0)
                    z = getattr(landmark, 'z', 0)
                    visibility = getattr(landmark, 'visibility', 0)
                
                # Add normalized features (x, y, z, visibility for each landmark)
                features.extend([float(x), float(y), float(z), float(visibility)])
            else:
                # Pad missing landmarks with zeros
                features.extend([0.0, 0.0, 0.0, 0.0])
        
        # Ensure we have exactly feature_dim features
        while len(features) < self.feature_dim:
            features.append(0.0)
        
        return np.array(features[:self.feature_dim], dtype=np.float32)
    
    def add_frame_to_sequence(self, landmarks):
        """Add current frame landmarks to the sequence buffer"""
        features = self.extract_features_from_landmarks(landmarks)
        self.current_sequence.append(features)
        
        # Maintain sequence length
        if len(self.current_sequence) > self.sequence_length:
            self.current_sequence.pop(0)
        
        # Debug logging
        print(f"🔍 DEBUG: Frame added to sequence. Current length: {len(self.current_sequence)}/{self.sequence_length}")
    
    def predict_sign(self):
        """Make prediction using your trained LSTM model"""
        if self.model is None:
            logger.debug("❌ No model available for prediction")
            return None, 0.0
        
        if len(self.current_sequence) < self.sequence_length:
            logger.debug(f"❌ Insufficient sequence length: {len(self.current_sequence)}/{self.sequence_length}")
            print(f"🔍 DEBUG: Need {self.sequence_length - len(self.current_sequence)} more frames")
            return None, 0.0
        
        try:
            # Prepare input for model (batch_size, sequence_length, feature_dim)
            sequence_array = np.array(self.current_sequence[-self.sequence_length:])
            sequence_array = np.expand_dims(sequence_array, axis=0)
            
            logger.debug(f"🔍 Input shape for prediction: {sequence_array.shape}")
            print(f"🔍 DEBUG: Making prediction with sequence shape: {sequence_array.shape}")
            
            # Make prediction using your trained model
            predictions = self.model.predict(sequence_array, verbose=0)
            
            # Get predicted class and confidence
            predicted_class_idx = np.argmax(predictions[0])
            confidence = float(np.max(predictions)) * 100
            
            # Map class index to English name, then to Bangla
            if predicted_class_idx in self.index_to_class:
                english_name = self.index_to_class[predicted_class_idx]
                bangla_name = self.english_to_bangla.get(english_name, english_name)
                
                logger.info(f"🎯 Model prediction: {english_name} → {bangla_name} (confidence: {confidence:.2f}%)")
                print(f"🔍 DEBUG: Model prediction: {english_name} → {bangla_name} (confidence: {confidence:.2f}%)")
                return bangla_name, confidence
            else:
                logger.warning(f"⚠️ Unknown class index: {predicted_class_idx}")
                print(f"🔍 DEBUG: Unknown class index: {predicted_class_idx}")
                return None, 0.0
                
        except Exception as e:
            logger.error(f"❌ Error during prediction: {e}")
            print(f"🔍 DEBUG: Error during prediction: {e}")
            return None, 0.0
    
    def reset_sequence(self):
        """Reset the current sequence buffer"""
        self.current_sequence = []
        logger.info("🔄 Sequence buffer reset")
        print("🔍 DEBUG: Sequence buffer reset")

    def analyze_with_trained_model(self, pose_landmarks: List[Dict], expected_sign: str) -> Dict[str, Any]:
        """Analyze using your trained BDSLW60 LSTM model with enhanced debugging"""
        
        # Add current frame to model's sequence buffer
        self.add_frame_to_sequence(pose_landmarks)
        
        # DEBUG: Log sequence length
        current_seq_len = len(self.current_sequence)
        required_seq_len = self.sequence_length
        print(f"🔍 DEBUG: Sequence buffer: {current_seq_len}/{required_seq_len} frames")
        
        # Get prediction from trained model
        predicted_sign, model_confidence = self.predict_sign()
        
        print(f"🔍 DEBUG: Model prediction: {predicted_sign}, confidence: {model_confidence}")
        
        if predicted_sign is None:
            return {
                "confidence_score": 0.0,
                "feedback_text": f"Building sequence... {current_seq_len}/{required_seq_len} frames collected. Continue signing steadily.",
                "is_correct": False,
                "improvement_tips": "Keep signing consistently for better recognition.",
                "predicted_sign": None,
                "expected_sign": expected_sign,
                "model_status": "building_sequence"
            }
        
        # Check if prediction matches expected sign
        is_correct = predicted_sign.strip() == expected_sign.strip()
        
        # Generate enhanced feedback based on model prediction
        feedback_text = self._generate_model_feedback(
            model_confidence, predicted_sign, expected_sign, is_correct
        )
        
        improvement_tips = self._generate_model_tips(
            model_confidence, predicted_sign, expected_sign, is_correct
        )
        
        # Confidence scoring based on model prediction and correctness
        if is_correct:
            confidence_score = max(model_confidence, 70.0)  # Boost correct predictions
        else:
            confidence_score = min(model_confidence * 0.7, 60.0)  # Reduce incorrect predictions

        return {
            "confidence_score": round(confidence_score, 2),
            "feedback_text": feedback_text,
            "is_correct": is_correct,
            "improvement_tips": improvement_tips,
            "predicted_sign": predicted_sign,
            "expected_sign": expected_sign,
            "model_confidence": round(model_confidence, 2),
            "model_status": "active"
        }

    def _generate_model_feedback(self, confidence: float, predicted: str, expected: str, is_correct: bool) -> str:
        """Generate feedback based on trained model predictions"""
        if is_correct:
            if confidence >= 85:
                return f"🎉 Excellent! Perfect '{expected}' sign detected with {confidence:.1f}% confidence!"
            elif confidence >= 70:
                return f"✅ Great job! Good '{expected}' sign recognized with {confidence:.1f}% confidence."
            else:
                return f"👍 Correct '{expected}' sign detected. Try to be more precise for higher confidence."
        else:
            if predicted and predicted != expected:
                return f"🔄 Model detected '{predicted}' but you're practicing '{expected}'. Check your hand positioning."
            else:
                return f"🤔 Sign not clearly recognized. Please ensure clear visibility for '{expected}'."

    def _generate_model_tips(self, confidence: float, predicted: str, expected: str, is_correct: bool) -> str:
        """Generate improvement tips based on model analysis"""
        if is_correct and confidence >= 80:
            return "Perfect execution! Try holding the sign steady for 2-3 seconds to build consistency."
        elif is_correct:
            return "Good sign recognition! Focus on smoother movements and clearer hand positioning."
        elif predicted and predicted != expected:
            return f"The model sees '{predicted}' instead of '{expected}'. Review the lesson video and adjust your hand position."
        else:
            return "Ensure both hands are clearly visible and make deliberate, steady movements matching the lesson demonstration."


# Global predictor instance
global_predictor = None


def get_predictor():
    """Get or create global predictor instance"""
    global global_predictor
    if global_predictor is None:
        global_predictor = EnhancedModelPredictor()
    return global_predictor
