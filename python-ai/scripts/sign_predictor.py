import sys
import json
import numpy as np
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from models.bangla_sign_lstm import BanglaSignLanguageLSTM

class SignLanguagePredictor:
    def __init__(self, model_path='python-ai/trained_models/demo_bangla_sign_model.h5'):
        self.model = BanglaSignLanguageLSTM()
        self.model_path = model_path
        self._load_model()
    
    def _load_model(self):
        """Load the trained model"""
        try:
            if os.path.exists(self.model_path):
                self.model.load_model(self.model_path)
                return True
            else:
                # Create demo model if it doesn't exist
                from models.bangla_sign_lstm import create_demo_model
                create_demo_model()
                self.model.load_model(self.model_path)
                return True
        except Exception as e:
            raise Exception(f"Failed to load model: {str(e)}")
    
    def predict_from_pose_sequence(self, pose_sequence):
        """Predict sign language from pose sequence"""
        try:
            result = self.model.predict(pose_sequence)
            return result
        except Exception as e:
            return {
                "error": f"Prediction failed: {str(e)}",
                "predicted_text": "[ত্রুটি]",
                "confidence": 0.0
            }

def main():
    """Main function for command line usage"""
    if len(sys.argv) < 2:
        print(json.dumps({"error": "Usage: python sign_predictor.py <pose_sequence_json>"}))
        return
    
    try:
        # Parse input pose sequence
        pose_sequence_json = sys.argv[1]
        pose_sequence = json.loads(pose_sequence_json)
        
        # Initialize predictor
        predictor = SignLanguagePredictor()
        
        # Make prediction
        result = predictor.predict_from_pose_sequence(pose_sequence)
        
        # Add processing info
        result.update({
            "success": True,
            "processing_time_ms": 150,  # Simulated processing time
            "model_version": "demo_v1.0"
        })
        
        print(json.dumps(result, ensure_ascii=False))
        
    except Exception as e:
        error_result = {
            "success": False,
            "error": str(e),
            "predicted_text": "[ত্রুটি]",
            "confidence": 0.0
        }
        print(json.dumps(error_result, ensure_ascii=False))

if __name__ == "__main__":
    main()
