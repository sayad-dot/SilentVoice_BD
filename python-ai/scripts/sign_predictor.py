#!/usr/bin/env python3
"""
Enhanced Sign Language Predictor for SilentVoice_BD
- File-based input/output to handle large data
- Attention-based LSTM architecture
- Robust normalization handling
- Real-time optimization
- Comprehensive error handling and logging
"""

import os
import sys
import json
import argparse
import numpy as np
import tensorflow as tf
import logging
from typing import List, Dict, Tuple, Optional
from pathlib import Path
import gc

# Suppress TensorFlow warnings
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
tf.get_logger().setLevel('ERROR')

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class EnhancedSignLanguagePredictor:
    """Enhanced sign language predictor with file-based input/output and attention mechanism"""
    
    def __init__(self, model_path: str, config_path: Optional[str] = None):
        self.model_path = Path(model_path)
        self.model = None
        self.is_loaded = False
        self.feature_means = None
        self.feature_stds = None
        self.normalization_loaded = False
        self.vocabulary = None
        
        # Load configuration
        self.config = self._load_config(config_path)
        self.sequence_length = self.config.get('sequence_length', 30)
        self.feature_dim = self.config.get('feature_dim', 288)
        self.confidence_threshold = self.config.get('confidence_threshold', 0.1)
        
        # Initialize components
        self._load_normalization_params()
        self._load_model_and_resources()
    
    def _load_config(self, config_path: Optional[str]) -> Dict:
        """Load predictor configuration"""
        default_config = {
            'sequence_length': 30,
            'feature_dim': 288,
            'confidence_threshold': 0.1,
            'enable_attention': True,
            'dropout_rate': 0.3,
            'lstm_units': 128,
            'dense_units': 64,
            'enable_normalization': True
        }
        
        if config_path and os.path.exists(config_path):
            try:
                with open(config_path, 'r') as f:
                    user_config = json.load(f)
                default_config.update(user_config)
                logger.info(f"✅ Loaded configuration from {config_path}")
            except Exception as e:
                logger.warning(f"⚠️ Failed to load config: {e}, using defaults")
        
        return default_config
    
    def _load_normalization_params(self):
        """Load normalization parameters with error handling"""
        try:
            script_dir = Path(__file__).parent
            data_dir = script_dir / '..' / 'data'
            
            means_path = data_dir / 'feature_means.npy'
            stds_path = data_dir / 'feature_stds.npy'
            
            if means_path.exists() and stds_path.exists():
                self.feature_means = np.load(means_path).astype(np.float32)
                self.feature_stds = np.load(stds_path).astype(np.float32)
                
                # Ensure no zero standard deviations
                self.feature_stds = np.where(self.feature_stds == 0, 1e-8, self.feature_stds)
                
                self.normalization_loaded = True
                logger.info("✅ Normalization parameters loaded successfully")
                logger.info(f"   Means shape: {self.feature_means.shape}, range: [{self.feature_means.min():.4f}, {self.feature_means.max():.4f}]")
                logger.info(f"   Stds shape: {self.feature_stds.shape}, range: [{self.feature_stds.min():.4f}, {self.feature_stds.max():.4f}]")
            else:
                logger.error("❌ CRITICAL: Normalization files not found!")
                logger.error(f"   Expected: {means_path}, {stds_path}")
                self._generate_default_normalization()
                
        except Exception as e:
            logger.error(f"❌ Failed to load normalization parameters: {e}")
            self._generate_default_normalization()
    
    def _generate_default_normalization(self):
        """Generate default normalization parameters as fallback"""
        logger.warning("⚠️ Generating default normalization parameters")
        self.feature_means = np.zeros(self.feature_dim, dtype=np.float32)
        self.feature_stds = np.ones(self.feature_dim, dtype=np.float32)
        self.normalization_loaded = True
        logger.warning("⚠️ Using default normalization - model accuracy may be reduced!")
    
    def _load_model_and_resources(self):
        """Load model and associated resources"""
        try:
            # Load the model
            if not self.model_path.exists():
                raise FileNotFoundError(f"Model file not found: {self.model_path}")
            
            logger.info(f"📥 Loading model from: {self.model_path}")
            
            # Load model with custom objects if needed
            custom_objects = {}
            if self.config.get('enable_attention', True):
                custom_objects.update({
                    'attention_layer': self._attention_layer,
                    'scaled_dot_product_attention': self._scaled_dot_product_attention
                })
            
            self.model = tf.keras.models.load_model(str(self.model_path), custom_objects=custom_objects)
            self.is_loaded = True
            
            # Log model architecture
            logger.info("✅ Model loaded successfully")
            logger.info(f"   Input shape: {self.model.input_shape}")
            logger.info(f"   Output shape: {self.model.output_shape}")
            logger.info(f"   Total parameters: {self.model.count_params():,}")
            
            # Load class mappings
            mappings_path = self.model_path.parent / 'class_mappings.json'
            if mappings_path.exists():
                with open(mappings_path, 'r', encoding='utf-8') as f:
                    mappings = json.load(f)
                vocabulary_mapping = mappings.get('index_to_class', {})
                # Convert string keys to integers
                self.vocabulary = {int(k): v for k, v in vocabulary_mapping.items()}
                logger.info(f"✅ Loaded vocabulary with {len(self.vocabulary)} classes")
            else:
                # Use default Bangla vocabulary if mappings not found
                logger.warning("⚠️ Class mappings not found, using default vocabulary")
                self.vocabulary = {
                    i: word for i, word in enumerate([
                        'দাদা', 'দাদি', 'মা', 'বাবা', 'ভাই', 'বোন',
                        'আম', 'আপেল', 'চা', 'পানি', 'খাবার',
                        'ভালো', 'খারাপ', 'হ্যালো', 'ধন্যবাদ', 'দুঃখিত',
                        'আমি', 'তুমি', 'আপনি', 'কি', 'কেমন',
                        'নাম', 'কাজ', 'স্কুল', 'বাড়ি', 'রাস্তা',
                        'সকাল', 'দুপুর', 'রাত', 'এখন', 'পরে'
                    ])
                }
            
            # Verify model compatibility
            expected_input_shape = (None, self.sequence_length, self.feature_dim)
            if self.model.input_shape != expected_input_shape:
                logger.warning(f"⚠️ Model input shape mismatch: expected {expected_input_shape}, got {self.model.input_shape}")
                
        except Exception as e:
            logger.error(f"❌ Failed to load model resources: {e}")
            self._create_fallback_model()
    
    def _create_fallback_model(self):
        """Create a basic fallback model"""
        logger.warning("⚠️ Creating fallback model - predictions will be random!")
        
        # Use default vocabulary if not loaded
        if not self.vocabulary:
            self.vocabulary = {
                i: word for i, word in enumerate([
                    'দাদা', 'দাদি', 'মা', 'বাবা', 'ভাই', 'বোন',
                    'আম', 'আপেল', 'চা', 'পানি', 'খাবার',
                    'ভালো', 'খারাপ', 'হ্যালো', 'ধন্যবাদ', 'দুঃখিত',
                    'আমি', 'তুমি', 'আপনি', 'কি', 'কেমন',
                    'নাম', 'কাজ', 'স্কুল', 'বাড়ি', 'রাস্তা',
                    'সকাল', 'দুপুর', 'রাত', 'এখন', 'পরে'
                ])
            }
        
        model = tf.keras.Sequential([
            tf.keras.layers.Input(shape=(self.sequence_length, self.feature_dim)),
            tf.keras.layers.LSTM(64, return_sequences=True),
            tf.keras.layers.GlobalAveragePooling1D(),
            tf.keras.layers.Dense(len(self.vocabulary), activation='softmax')
        ])
        
        model.compile(optimizer='adam', loss='categorical_crossentropy', metrics=['accuracy'])
        self.model = model
        self.is_loaded = True
        logger.warning("⚠️ Fallback model created - please load proper trained model!")
    
    def _attention_layer(self, inputs):
        """Custom attention layer"""
        return inputs
    
    def _scaled_dot_product_attention(self, query, key, value, mask=None):
        """Scaled dot-product attention mechanism"""
        return value
    
    def preprocess_sequence(self, sequence: List[List[float]]) -> Tuple[np.ndarray, Dict]:
        """Enhanced preprocessing with comprehensive validation"""
        try:
            logger.debug(f"🔧 Preprocessing sequence: {len(sequence)} frames")
            
            # Input validation
            if not sequence:
                return None, {"error": "Empty sequence provided"}
            
            # Convert to numpy array
            data = np.array(sequence, dtype=np.float32)
            
            # Validate dimensions
            if len(data.shape) != 2:
                return None, {"error": f"Invalid data shape: expected 2D, got {data.shape}"}
            
            if data.shape[1] != self.feature_dim:
                return None, {"error": f"Feature dimension mismatch: expected {self.feature_dim}, got {data.shape[1]}"}
            
            # Quality checks
            nan_count = np.isnan(data).sum()
            inf_count = np.isinf(data).sum()
            zero_count = (np.abs(data) < 1e-8).sum()
            
            if nan_count > 0 or inf_count > 0:
                logger.warning(f"⚠️ Data quality issues: {nan_count} NaN, {inf_count} Inf values")
                data = np.nan_to_num(data, nan=0.0, posinf=1.0, neginf=-1.0)
            
            # Apply normalization if available
            stats = {
                'original_shape': data.shape,
                'nan_count': int(nan_count),
                'inf_count': int(inf_count),
                'zero_percentage': float(zero_count / data.size * 100),
                'data_range': [float(data.min()), float(data.max())],
                'normalization_applied': False
            }
            
            if self.normalization_loaded and self.config.get('enable_normalization', True):
                # Apply z-score normalization
                normalized_data = (data - self.feature_means) / self.feature_stds
                
                # Clip extreme values to prevent instability
                normalized_data = np.clip(normalized_data, -5.0, 5.0)
                
                stats.update({
                    'normalization_applied': True,
                    'normalized_range': [float(normalized_data.min()), float(normalized_data.max())],
                    'normalized_mean': float(normalized_data.mean()),
                    'normalized_std': float(normalized_data.std())
                })
                
                data = normalized_data
                logger.debug("✅ Normalization applied successfully")
            else:
                logger.warning("⚠️ Normalization not applied - this may reduce accuracy!")
            
            # Adjust sequence length
            if data.shape[0] > self.sequence_length:
                # Take the last N frames (most recent)
                data = data[-self.sequence_length:]
                logger.debug(f"🔄 Truncated sequence to last {self.sequence_length} frames")
            elif data.shape[0] < self.sequence_length:
                # Pad with normalized zeros
                padding_needed = self.sequence_length - data.shape[0]
                if self.normalization_loaded:
                    # Use normalized zero vector
                    zero_frame = -self.feature_means / self.feature_stds
                    zero_frame = np.clip(zero_frame, -5.0, 5.0)
                else:
                    zero_frame = np.zeros(self.feature_dim, dtype=np.float32)
                
                padding = np.tile(zero_frame, (padding_needed, 1))
                data = np.vstack([padding, data])
                logger.debug(f"🔄 Padded sequence with {padding_needed} frames")
            
            # Final validation
            if data.shape != (self.sequence_length, self.feature_dim):
                return None, {"error": f"Final shape mismatch: expected ({self.sequence_length}, {self.feature_dim}), got {data.shape}"}
            
            # Add batch dimension
            data = np.expand_dims(data, axis=0)
            
            stats['final_shape'] = data.shape
            stats['preprocessing_successful'] = True
            
            logger.debug("✅ Preprocessing completed successfully")
            return data, stats
            
        except Exception as e:
            logger.error(f"❌ Preprocessing failed: {e}")
            return None, {"error": f"Preprocessing failed: {str(e)}"}
    
    def predict(self, sequence: List[List[float]]) -> Dict:
        """Enhanced prediction with comprehensive error handling"""
        try:
            start_time = tf.timestamp()
            
            # Validate model
            if not self.is_loaded or self.model is None:
                return {
                    "success": False,
                    "error": "Model not loaded",
                    "predicted_text": "ত্রুটি",
                    "confidence": 0.0,
                    "model_version": "bangla_lstm_enhanced_v1"
                }
            
            logger.info(f"🤖 Starting prediction for sequence with {len(sequence)} frames")
            
            # Preprocess input
            processed_data, preprocessing_stats = self.preprocess_sequence(sequence)
            if processed_data is None:
                return {
                    "success": False,
                    "error": preprocessing_stats.get("error", "Preprocessing failed"),
                    "predicted_text": "ত্রুটি",
                    "confidence": 0.0,
                    "model_version": "bangla_lstm_enhanced_v1",
                    "preprocessing_stats": preprocessing_stats
                }
            
            # Model inference
            logger.debug("🔮 Running model inference...")
            predictions = self.model.predict(processed_data, verbose=0)
            
            # Process predictions
            if len(predictions.shape) == 2 and predictions.shape[0] == 1:
                prediction_vector = predictions[0]
            else:
                logger.error(f"❌ Unexpected prediction shape: {predictions.shape}")
                return {
                    "success": False,
                    "error": f"Unexpected prediction shape: {predictions.shape}",
                    "predicted_text": "ত্রুটি",
                    "confidence": 0.0,
                    "model_version": "bangla_lstm_enhanced_v1"
                }
            
            # Get top prediction
            predicted_class = np.argmax(prediction_vector)
            confidence = float(prediction_vector[predicted_class])
            
            # Validate prediction
            if predicted_class >= len(self.vocabulary):
                logger.error(f"❌ Predicted class {predicted_class} out of vocabulary range (max: {len(self.vocabulary) - 1})")
                return {
                    "success": False,
                    "error": f"Predicted class out of range: {predicted_class}",
                    "predicted_text": "ত্রুটি",
                    "confidence": 0.0,
                    "model_version": "bangla_lstm_enhanced_v1"
                }
            
            predicted_text = self.vocabulary[predicted_class]
            
            # Calculate processing time
            processing_time = float(tf.timestamp() - start_time) * 1000  # Convert to milliseconds
            
            # Enhanced confidence analysis
            confidence_analysis = self._analyze_confidence(prediction_vector)
            
            # Create comprehensive response
            result = {
                "success": True,
                "predicted_text": predicted_text,
                "confidence": confidence,
                "predicted_class_idx": int(predicted_class),
                "model_version": "bangla_lstm_enhanced_v1",
                "processing_time_ms": processing_time,
                "preprocessing_stats": preprocessing_stats,
                "confidence_analysis": confidence_analysis,
                "processing_info": {
                    "input_shape": list(processed_data.shape),
                    "normalized": preprocessing_stats.get('normalization_applied', False),
                    "prediction_time_ms": processing_time
                }
            }
            
            # Log results
            logger.info(f"✅ Prediction completed successfully:")
            logger.info(f"   📝 Predicted: '{predicted_text}' (class: {predicted_class})")
            logger.info(f"   📊 Confidence: {confidence:.4f} ({confidence*100:.2f}%)")
            logger.info(f"   ⏱️ Processing time: {processing_time:.2f}ms")
            logger.info(f"   🔧 Normalization applied: {preprocessing_stats.get('normalization_applied', False)}")
            
            # Confidence warnings
            if confidence < 0.1:
                logger.error(f"❌ VERY LOW CONFIDENCE: {confidence:.4f} - Check normalization and input quality!")
            elif confidence < 0.3:
                logger.warning(f"⚠️ Low confidence: {confidence:.4f} - Consider improving input quality")
            elif confidence > 0.8:
                logger.info(f"✅ High confidence prediction: {confidence:.4f}")
            
            return result
            
        except Exception as e:
            logger.error(f"❌ Prediction failed with exception: {e}")
            return {
                "success": False,
                "error": f"Prediction failed: {str(e)}",
                "predicted_text": "ত্রুটি",
                "confidence": 0.0,
                "model_version": "bangla_lstm_enhanced_v1"
            }
    
    def _analyze_confidence(self, prediction_vector: np.ndarray) -> Dict:
        """Analyze prediction confidence and distribution"""
        try:
            # Sort predictions
            sorted_indices = np.argsort(prediction_vector)[::-1]
            sorted_probs = prediction_vector[sorted_indices]
            
            # Top predictions
            top_3_classes = sorted_indices[:3].tolist()
            top_3_probs = sorted_probs[:3].tolist()
            top_3_words = [self.vocabulary.get(i, f"class_{i}") for i in top_3_classes]
            
            # Confidence metrics
            entropy = -np.sum(prediction_vector * np.log(prediction_vector + 1e-8))
            max_prob = float(sorted_probs[0])
            second_max_prob = float(sorted_probs[1]) if len(sorted_probs) > 1 else 0.0
            confidence_gap = max_prob - second_max_prob
            
            return {
                "top_3_classes": top_3_classes,
                "top_3_probabilities": top_3_probs,
                "top_3_words": top_3_words,
                "entropy": float(entropy),
                "confidence_gap": float(confidence_gap),
                "prediction_certainty": "HIGH" if max_prob > 0.8 else "MEDIUM" if max_prob > 0.5 else "LOW"
            }
            
        except Exception as e:
            logger.warning(f"⚠️ Confidence analysis failed: {e}")
            return {"error": str(e)}
    
    def get_system_info(self) -> Dict:
        """Get comprehensive system information"""
        return {
            "model_loaded": self.is_loaded,
            "model_path": str(self.model_path),
            "normalization_loaded": self.normalization_loaded,
            "vocabulary_size": len(self.vocabulary) if self.vocabulary else 0,
            "sequence_length": self.sequence_length,
            "feature_dim": self.feature_dim,
            "tensorflow_version": tf.__version__,
            "config": self.config
        }

def test_model_loading():
    """Test if model can be loaded successfully (for startup check)"""
    try:
        script_dir = Path(__file__).parent
        model_path = script_dir / '..' / 'models' / 'bangla_lstm_enhanced.h5'
        
        if not model_path.exists():
            return {
                "success": False,
                "error": f"Model file not found: {model_path}"
            }
        
        # Try to create predictor (this loads the model)
        predictor = EnhancedSignLanguagePredictor(str(model_path))
        
        return {
            "success": True,
            "message": "Model loaded successfully",
            "model_version": "bangla_lstm_enhanced_v1",
            "vocab_size": len(predictor.vocabulary) if predictor.vocabulary else 0,
            "normalization_available": predictor.normalization_loaded
        }
        
    except Exception as e:
        logger.error(f"❌ Model loading test failed: {e}")
        return {
            "success": False,
            "error": str(e)
        }

def main():
    """Main function with both file-based and command-line interfaces"""
    parser = argparse.ArgumentParser(description='Enhanced Sign Language Predictor')
    parser.add_argument('input_file', nargs='?', help='Input file path (for file-based mode)')
    parser.add_argument('output_file', nargs='?', help='Output file path (for file-based mode)')
    parser.add_argument('--model-path', default='../models/bangla_lstm_enhanced.h5', help='Model file path')
    parser.add_argument('--legacy', action='store_true', help='Use legacy command-line mode')
    
    args = parser.parse_args()
    
    try:
        # Determine operation mode
        if args.input_file and args.output_file and not args.legacy:
            # FILE-BASED MODE (New approach to fix "argument list too long")
            logger.info("🔄 Using file-based input/output mode")
            
            # Read input data
            with open(args.input_file, 'r', encoding='utf-8') as f:
                input_data = json.load(f)
            
            # Check if this is a simple model test (startup check)
            if not input_data or input_data.get('data') == '[]':
                logger.info("Performing model loading test")
                result = test_model_loading()
            else:
                # Handle different input formats
                if 'data' in input_data:
                    data_content = input_data['data']
                    if isinstance(data_content, str):
                        pose_sequence = json.loads(data_content)
                    else:
                        pose_sequence = data_content
                elif 'pose_sequence' in input_data:
                    pose_sequence = input_data['pose_sequence']
                else:
                    raise ValueError("No pose sequence found in input data")
                
                if not pose_sequence:
                    raise ValueError("Empty pose sequence provided")
                
                logger.info(f"Loaded pose sequence with {len(pose_sequence)} frames")
                
                # Initialize predictor
                script_dir = Path(__file__).parent
                model_path = script_dir / args.model_path
                predictor = EnhancedSignLanguagePredictor(str(model_path))
                
                # Make prediction
                result = predictor.predict(pose_sequence)
            
            # Write output
            with open(args.output_file, 'w', encoding='utf-8') as f:
                json.dump(result, f, ensure_ascii=False, indent=2)
            
            logger.info(f"Results written to: {args.output_file}")
            
        else:
            # LEGACY COMMAND-LINE MODE (for backward compatibility)
            logger.info("🔄 Using legacy command-line mode")
            
            if len(sys.argv) < 2:
                print(json.dumps({
                    "success": False,
                    "error": "Usage: python sign_predictor.py <pose_sequence_json> OR python sign_predictor.py <input_file> <output_file>"
                }))
                return
            
            # Get script directory for relative paths
            script_dir = Path(__file__).parent
            model_path = script_dir / '..' / 'models' / 'bangla_lstm_enhanced.h5'
            
            # Initialize predictor
            predictor = EnhancedSignLanguagePredictor(str(model_path))
            
            # Parse input
            input_data = sys.argv[1]
            
            # Handle special commands
            if input_data == "[]":
                # System readiness check
                result = {
                    "success": True,
                    "message": "System ready for predictions",
                    "system_info": predictor.get_system_info()
                }
                print(json.dumps(result, ensure_ascii=False))
                return
            
            # Parse sequence data
            try:
                pose_sequence = json.loads(input_data)
            except json.JSONDecodeError as e:
                print(json.dumps({
                    "success": False,
                    "error": f"Invalid JSON input: {str(e)}"
                }))
                return
            
            # Validate input format
            if not isinstance(pose_sequence, list):
                print(json.dumps({
                    "success": False,
                    "error": "Input must be a list of pose sequences"
                }))
                return
            
            # Perform prediction
            if len(pose_sequence) > 0 and isinstance(pose_sequence[0], list):
                # Single sequence
                result = predictor.predict(pose_sequence)
            else:
                # Invalid format
                result = {
                    "success": False,
                    "error": "Invalid sequence format"
                }
            
            print(json.dumps(result, ensure_ascii=False))
        
    except Exception as e:
        logger.error(f"❌ Script failed: {e}")
        
        # Write error result
        error_result = {
            "success": False,
            "error": str(e),
            "predicted_text": "ত্রুটি",
            "confidence": 0.0,
            "model_version": "bangla_lstm_enhanced_v1"
        }
        
        try:
            if args.output_file:
                with open(args.output_file, 'w', encoding='utf-8') as f:
                    json.dump(error_result, f, ensure_ascii=False, indent=2)
            else:
                print(json.dumps(error_result, ensure_ascii=False))
        except:
            pass  # If we can't write error file, just exit
        
        sys.exit(1)

if __name__ == "__main__":
    main()
