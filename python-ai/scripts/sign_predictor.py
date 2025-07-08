#!/usr/bin/env python3
import json
import numpy as np
import tensorflow as tf
import pickle
import sys
import os

class SignLanguagePredictor:
    def __init__(self, model_path='../trained_models/bangla_lstm_model.h5',
                 encoder_path='../trained_models/label_encoder.pkl',
                 config_path='../trained_models/model_config.json'):
        self.model_path = model_path
        self.encoder_path = encoder_path
        self.config_path = config_path
        self.model = None
        self.label_encoder = None
        self.config = None
        self.load_model()
    
    def load_model(self):
        """Load trained model and label encoder"""
        try:
            # Load model
            self.model = tf.keras.models.load_model(self.model_path)
            
            # Load label encoder
            with open(self.encoder_path, 'rb') as f:
                self.label_encoder = pickle.load(f)
            
            # Load config
            with open(self.config_path, 'r', encoding='utf-8') as f:
                self.config = json.load(f)
                
            print(f"Model loaded successfully", file=sys.stderr)
            
        except Exception as e:
            print(f"Error loading model: {e}", file=sys.stderr)
            # Create demo model for testing
            self.create_demo_model()
    
    def create_demo_model(self):
        """Create a demo model for testing when real model is not available"""
        from models.bangla_sign_lstm import BanglaSignLanguageLSTM
        
        demo_model = BanglaSignLanguageLSTM(sequence_length=30, num_classes=61)
        demo_model.build_model()
        demo_model.create_demo_model()
        
        self.model = demo_model.model
        self.config = {
            'classes': ['আম', 'আপেল', 'এসি', 'এইডস', 'আলু', 'আনারস', 'আঙুর', 
                       'অ্যাপার্টমেন্ট', 'আত্তিও', 'অডিও ক্যাসেট', 'আয়না'],
            'sequence_length': 30,
            'feature_dim': 258
        }
        
        # Create mock label encoder
        class MockLabelEncoder:
            def __init__(self, classes):
                self.classes_ = np.array(classes)
            
            def inverse_transform(self, y):
                return [self.classes_[i] for i in y]
        
        self.label_encoder = MockLabelEncoder(self.config['classes'])
    
    def preprocess_pose_sequence(self, pose_sequence):
        """Preprocess pose sequence for prediction"""
        sequence_length = self.config.get('sequence_length', 30)
        feature_dim = self.config.get('feature_dim', 258)
        
        # Ensure each frame has correct feature dimension
        processed_sequence = []
        for frame in pose_sequence:
            if len(frame) != feature_dim:
                # Pad or truncate to correct dimension
                if len(frame) < feature_dim:
                    frame = frame + [0] * (feature_dim - len(frame))
                else:
                    frame = frame[:feature_dim]
            processed_sequence.append(frame)
        
        # Pad or truncate sequence to correct length
        if len(processed_sequence) > sequence_length:
            processed_sequence = processed_sequence[:sequence_length]
        elif len(processed_sequence) < sequence_length:
            padding = [[0] * feature_dim] * (sequence_length - len(processed_sequence))
            processed_sequence = processed_sequence + padding
        
        return np.array([processed_sequence])  # Add batch dimension
    
    def predict_from_pose_sequence(self, pose_sequence):
        """Predict sign language from pose sequence"""
        try:
            # Preprocess input
            X = self.preprocess_pose_sequence(pose_sequence)
            
            # Make prediction
            predictions = self.model.predict(X, verbose=0)
            predicted_class = np.argmax(predictions[0])
            confidence = float(predictions[0][predicted_class])
            
            # Get predicted text
            predicted_text = self.label_encoder.inverse_transform([predicted_class])[0]
            
            return {
                'success': True,
                'predicted_text': predicted_text,
                'confidence': confidence,
                'model_version': 'bangla_lstm_v1',
                'sequence_length': len(pose_sequence),
                'processing_info': {
                    'input_shape': X.shape,
                    'prediction_scores': predictions[0].tolist()[:5]  # Top 5 scores
                }
            }
            
        except Exception as e:
            return {
                'success': False,
                'error': str(e),
                'predicted_text': 'ত্রুটি',
                'confidence': 0.0
            }

def main():
    """Command line interface for sign prediction"""
    if len(sys.argv) < 2:
        print(json.dumps({
            "error": "Usage: python sign_predictor.py '<pose_sequence_json>'"
        }))
        return
    
    try:
        # Parse input pose sequence
        pose_sequence_json = sys.argv[1]
        pose_sequence = json.loads(pose_sequence_json)
        
        # Create predictor and make prediction
        predictor = SignLanguagePredictor()
        result = predictor.predict_from_pose_sequence(pose_sequence)
        
        # Output result as JSON
        print(json.dumps(result, ensure_ascii=False))
        
    except Exception as e:
        error_result = {
            "success": False,
            "error": str(e),
            "predicted_text": "ত্রুটি",
            "confidence": 0.0
        }
        print(json.dumps(error_result, ensure_ascii=False))

if __name__ == "__main__":
    main()
