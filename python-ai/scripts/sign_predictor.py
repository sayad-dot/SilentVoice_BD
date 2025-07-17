#!/usr/bin/env python3
import json
import numpy as np
import tensorflow as tf
import pickle
import sys
import os

class SignLanguagePredictor:
    def __init__(self,
                 model_path='/media/sayad/Ubuntu-Data/SilentVoice_BD/python-ai/trained_models/bangla_lstm_model.h5',
                 encoder_path='/media/sayad/Ubuntu-Data/SilentVoice_BD/python-ai/trained_models/label_encoder.pkl',
                 config_path='/media/sayad/Ubuntu-Data/SilentVoice_BD/python-ai/trained_modelsmodel_config.json'):
        self.model_path   = model_path
        self.encoder_path = encoder_path
        self.config_path  = config_path
        self.model         = None
        self.label_encoder = None
        self.config        = None
        self.feature_means = None
        self.feature_stds = None
        self.load_model()
        self.load_normalization_params()

    def load_normalization_params(self):
        """Load normalization parameters with absolute paths and debugging"""
        try:
            # Use absolute paths to ensure file loading
            script_dir = os.path.dirname(os.path.abspath(__file__))
            data_dir = os.path.join(script_dir, '..', 'data')
            
            means_path = os.path.join(data_dir, 'feature_means.npy')
            stds_path = os.path.join(data_dir, 'feature_stds.npy')
            
            print(f"🔍 PREDICTOR NORMALIZATION DEBUG:", file=sys.stderr)
            print(f"   Means path: {means_path}", file=sys.stderr)
            print(f"   Stds path: {stds_path}", file=sys.stderr)
            print(f"   Means exists: {os.path.exists(means_path)}", file=sys.stderr)
            print(f"   Stds exists: {os.path.exists(stds_path)}", file=sys.stderr)
            
            if os.path.exists(means_path) and os.path.exists(stds_path):
                self.feature_means = np.load(means_path)
                self.feature_stds = np.load(stds_path)
                print(f"✅ PREDICTOR: Loaded normalization params - means={len(self.feature_means)}, stds={len(self.feature_stds)}", file=sys.stderr)
                return
                
            # Try JSON fallback
            json_path = os.path.join(data_dir, 'normalization_params.json')
            if os.path.exists(json_path):
                with open(json_path, 'r') as f:
                    params = json.load(f)
                self.feature_means = np.array(params['feature_means'])
                self.feature_stds = np.array(params['feature_stds'])
                print(f"✅ PREDICTOR: Loaded normalization from JSON - means={len(self.feature_means)}, stds={len(self.feature_stds)}", file=sys.stderr)
                return
                
            print("❌ PREDICTOR: No normalization params found", file=sys.stderr)
            self.feature_means = None
            self.feature_stds = None
        except Exception as e:
            print(f"❌ PREDICTOR: Error loading normalization params: {e}", file=sys.stderr)
            self.feature_means = None
            self.feature_stds = None

    def load_model(self):
        try:
            if os.path.exists(self.model_path):
                self.model = tf.keras.models.load_model(self.model_path)
                print(f"✅ Loaded trained model from {self.model_path}", file=sys.stderr)
                print(f"✅ Model input shape: {self.model.input_shape}", file=sys.stderr)
            else:
                self.create_demo_model()

            if os.path.exists(self.encoder_path):
                with open(self.encoder_path, 'rb') as f:
                    self.label_encoder = pickle.load(f)
                print(f"✅ Loaded label encoder with {len(self.label_encoder.classes_)} classes", file=sys.stderr)
            else:
                self.create_demo_encoder()

            if os.path.exists(self.config_path):
                with open(self.config_path, 'r', encoding='utf-8') as f:
                    self.config = json.load(f)
                print(f"✅ Loaded model config: {self.config}", file=sys.stderr)
            else:
                self.create_demo_config()

            print("✅ Model loaded successfully", file=sys.stderr)
        except Exception as e:
            print(f"❌ Error loading model: {e}", file=sys.stderr)
            self.create_demo_model()

    def create_demo_model(self):
        from models.bangla_sign_lstm import BanglaSignLanguageLSTM
        demo = BanglaSignLanguageLSTM(sequence_length=30, num_classes=61)
        demo.build_model()
        demo.create_demo_model()
        self.model = demo.model
        print("✅ Created demo model", file=sys.stderr)

    def create_demo_encoder(self):
        class MockLE:
            def __init__(self, classes): self.classes_ = np.array(classes)
            def inverse_transform(self, y): return [self.classes_[i] for i in y]
        
        # Complete class list including দংশন
        demo_classes = [
            "অজানা", "অডিও ক্যাসেট", "অ্যাপার্টমেন্ট", "আঙুর", "আত্তিও", "আনারস", "আপেল", "আম",
            "আয়না", "আলু", "এইডস", "এসি", "কন্যা", "কেক", "ক্যাপসুল", "ক্রিম", "ক্লিপ", "চকলেট",
            "চশমা", "চা", "চাচা", "চাচি", "চাদর", "চাল", "চিকিৎসা", "চিনি", "চিপস", "চিরুনি",
            "চুরি", "চোখ উঠা", "জমজ", "জুতা", "টিউবলাইট", "টিভি", "টিশার্ট", "টুথপেস্ট", "টুপি",
            "ডাক্তার", "ডাল", "ডেঙ্গু", "তত্ত্ব", "দংশন", "দাদা", "দাদি", "দায়িত্ব", "দুর্বল",
            "দুলাভাই", "দেনাদার", "দেবর", "বউ", "বড়ই", "বাত", "বাবা", "বালতি", "বালু", "বিস্কুট",
            "বোতাম", "বোন", "ব্যান্ডেজ", "ভাই", "মা"
        ]
        self.label_encoder = MockLE(demo_classes)

    def create_demo_config(self):
        # CRITICAL: Use 288 features to match your model training
        self.config = {
            'sequence_length': 30, 
            'feature_dim': 288,  # CORRECT: Model expects 288 features
            'num_classes': 61
        }

    def get_normalized_zero_vector(self):
        """Get properly normalized zero vector for padding"""
        if self.feature_means is not None and self.feature_stds is not None:
            # Use actual normalization: (0 - mean) / std
            normalized_zeros = ((-self.feature_means) / self.feature_stds).tolist()
            print(f"🔧 Using normalized zeros (sample): {normalized_zeros[:5]}", file=sys.stderr)
            return normalized_zeros
        else:
            print(f"⚠️ Using raw zeros - normalization params not available", file=sys.stderr)
            return [0.0] * 288

    def preprocess(self, seq):
        """CORRECTED preprocessing with 288 feature compatibility"""
        sl = self.config['sequence_length']
        fd = self.config['feature_dim']  # Should be 288
        ps = []
        
        print(f"🔧 PREPROCESSING DEBUG:", file=sys.stderr)
        print(f"   Expected sequence length: {sl}", file=sys.stderr)
        print(f"   Expected feature dimension: {fd}", file=sys.stderr)
        print(f"   Input sequence length: {len(seq)}", file=sys.stderr)
        
        # Process each frame
        for i, f in enumerate(seq):
            original_len = len(f)
            if len(f) != fd:
                print(f"⚠️ Frame {i}: dimension mismatch - got {len(f)}, expected {fd}", file=sys.stderr)
                if len(f) > fd:
                    f = f[:fd]
                    print(f"   Truncated frame {i} from {original_len} to {len(f)}", file=sys.stderr)
                else:
                    padding_needed = fd - len(f)
                    f = f + [0.0] * padding_needed
                    print(f"   Padded frame {i} from {original_len} to {len(f)}", file=sys.stderr)
            ps.append(f)
        
        # Handle sequence length
        if len(ps) > sl:
            ps = ps[:sl]
            print(f"   Truncated sequence from {len(seq)} to {sl} frames", file=sys.stderr)
        elif len(ps) < sl:
            padding_frames = sl - len(ps)
            normalized_zero = self.get_normalized_zero_vector()
            ps.extend([normalized_zero] * padding_frames)
            print(f"   Padded sequence from {len(seq)} to {sl} frames", file=sys.stderr)
        
        result = np.array([ps], dtype=np.float32)
        print(f"✅ Final preprocessed shape: {result.shape}", file=sys.stderr)
        return result

    def predict_from_pose_sequence(self, pose_sequence):
        try:
            if not pose_sequence:
                return {
                    'success': False, 
                    'error': 'Empty pose sequence', 
                    'predicted_text': 'ত্রুটি', 
                    'confidence': 0.0
                }
            
            print(f"📊 PREDICTION INPUT: {len(pose_sequence)} frames, {len(pose_sequence[0]) if pose_sequence else 0} features per frame", file=sys.stderr)
            
            X = self.preprocess(pose_sequence)
            
            # Debug: Print shapes
            print(f"📊 Model input shape: {X.shape}", file=sys.stderr)
            print(f"📊 Model expects: {self.model.input_shape}", file=sys.stderr)
            
            preds = self.model.predict(X, verbose=0)[0]
            idx = int(np.argmax(preds))
            confidence = float(preds[idx])
            
            # Show top predictions for debugging
            top_predictions = sorted(enumerate(preds), key=lambda x: x[1], reverse=True)[:5]
            print(f"📊 Top 5 predictions:", file=sys.stderr)
            for i, (class_idx, conf) in enumerate(top_predictions):
                class_name = self.label_encoder.inverse_transform([class_idx])[0]
                print(f"   {i+1}. {class_name}: {conf:.6f} ({conf*100:.4f}%)", file=sys.stderr)
            
            predicted_text = self.label_encoder.inverse_transform([idx])[0]
            print(f"✅ FINAL PREDICTION: {predicted_text} (confidence: {confidence:.6f} = {confidence*100:.4f}%)", file=sys.stderr)
            
            return {
                'success': True,
                'predicted_text': predicted_text,
                'confidence': confidence,
                'model_version': 'bangla_lstm_v1'
            }
        except Exception as e:
            print(f"❌ Prediction error: {e}", file=sys.stderr)
            import traceback
            print(f"   Traceback: {traceback.format_exc()}", file=sys.stderr)
            return {
                'success': False, 
                'error': str(e), 
                'predicted_text': 'ত্রুটি', 
                'confidence': 0.0
            }

def main():
    if len(sys.argv) < 2:
        print(json.dumps({"error":"Usage: python sign_predictor.py '<pose_sequence_json>'"}))
        return
    seq = json.loads(sys.argv[1])
    pred = SignLanguagePredictor().predict_from_pose_sequence(seq)
    print(json.dumps(pred, ensure_ascii=False))

if __name__ == "__main__":
    main()
