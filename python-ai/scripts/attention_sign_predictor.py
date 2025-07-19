#!/usr/bin/env python3

import json
import numpy as np
import tensorflow as tf
import pickle
import sys
import os

class AttentionSignLanguagePredictor:
    def __init__(self,
                 model_path='../trained_models/attention_bangla_lstm_final.h5',
                 encoder_path='../trained_models/attention_label_encoder.pkl',
                 config_path='../trained_models/attention_model_config.json'):
        self.model_path = model_path
        self.encoder_path = encoder_path
        self.config_path = config_path
        self.model = None
        self.label_encoder = None
        self.config = None
        self.load_model()
    
    def load_model(self):
        try:
            if os.path.exists(self.model_path):
                self.model = tf.keras.models.load_model(self.model_path)
                print(f"✅ Loaded attention model from {self.model_path}", file=sys.stderr)
            else:
                # Fallback to basic model
                fallback_path = '../trained_models/bangla_lstm_model.h5'
                if os.path.exists(fallback_path):
                    self.model = tf.keras.models.load_model(fallback_path)
                    print(f"⚠️ Fallback to basic model: {fallback_path}", file=sys.stderr)
                else:
                    raise FileNotFoundError("No model found")
            
            if os.path.exists(self.encoder_path):
                with open(self.encoder_path, 'rb') as f:
                    self.label_encoder = pickle.load(f)
                print(f"✅ Loaded attention label encoder", file=sys.stderr)
            else:
                # Fallback to basic encoder
                fallback_encoder = '../trained_models/label_encoder.pkl'
                if os.path.exists(fallback_encoder):
                    with open(fallback_encoder, 'rb') as f:
                        self.label_encoder = pickle.load(f)
                    print(f"⚠️ Fallback to basic encoder", file=sys.stderr)
            
            if os.path.exists(self.config_path):
                with open(self.config_path, 'r', encoding='utf-8') as f:
                    self.config = json.load(f)
                print(f"✅ Loaded attention model config", file=sys.stderr)
            else:
                self.config = {
                    'sequence_length': 30,
                    'feature_dim': 288,
                    'num_classes': 61
                }
                print(f"⚠️ Using default config", file=sys.stderr)
            
        except Exception as e:
            print(f"❌ Error loading attention model: {e}", file=sys.stderr)
            raise
    
    def predict_from_pose_sequence(self, pose_sequence):
        try:
            if not pose_sequence:
                return {
                    'success': False,
                    'error': 'Empty pose sequence',
                    'predicted_text': 'ত্রুটি',
                    'confidence': 0.0
                }
            
            # Preprocess sequence
            X = self.preprocess_sequence(pose_sequence)
            
            # Predict with attention model
            predictions = self.model.predict(X, verbose=0)[0]
            predicted_idx = int(np.argmax(predictions))
            confidence = float(predictions[predicted_idx])
            
            # Get predicted text
            predicted_text = self.label_encoder.inverse_transform([predicted_idx])[0]
            
            print(f"✅ Attention prediction: {predicted_text} (confidence: {confidence:.4f})", file=sys.stderr)
            
            return {
                'success': True,
                'predicted_text': predicted_text,
                'confidence': confidence,
                'model_version': 'attention_bangla_lstm_v1'
            }
            
        except Exception as e:
            print(f"❌ Attention prediction error: {e}", file=sys.stderr)
            return {
                'success': False,
                'error': str(e),
                'predicted_text': 'ত্রুটি',
                'confidence': 0.0
            }
    
    def preprocess_sequence(self, pose_sequence):
        """Preprocess pose sequence for attention model input"""
        sequence_length = self.config['sequence_length']
        feature_dim = self.config['feature_dim']
        
        processed_sequence = []
        for frame in pose_sequence:
            if len(frame) != feature_dim:
                if len(frame) > feature_dim:
                    frame = frame[:feature_dim]
                else:
                    frame = frame + [0.0] * (feature_dim - len(frame))
            processed_sequence.append(frame)
        
        # Adjust sequence length
        if len(processed_sequence) > sequence_length:
            processed_sequence = processed_sequence[:sequence_length]
        elif len(processed_sequence) < sequence_length:
            padding = [[0.0] * feature_dim] * (sequence_length - len(processed_sequence))
            processed_sequence.extend(padding)
        
        return np.array([processed_sequence], dtype=np.float32)

def main():
    if len(sys.argv) < 2:
        print(json.dumps({"error": "Usage: python attention_sign_predictor.py '<pose_sequence>'"}))
        return
    
    pose_sequence = json.loads(sys.argv[1])
    predictor = AttentionSignLanguagePredictor()
    result = predictor.predict_from_pose_sequence(pose_sequence)
    print(json.dumps(result, ensure_ascii=False))

if __name__ == "__main__":
    main()
