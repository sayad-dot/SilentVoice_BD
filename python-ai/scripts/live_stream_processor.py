import sys
import json
import argparse
import cv2
import numpy as np
from pathlib import Path
import tensorflow as tf
from tensorflow import keras
import pickle

# Add the parent directory to the path
sys.path.append(str(Path(__file__).parent))

# Import your existing modules
from pose_extractor import extract_pose_landmarks
from sign_predictor import SignPredictor

class LiveStreamProcessor:
    def __init__(self):
        self.model_path = "../trained_models/hypertuned_bangla_lstm_best.h5"
        self.label_encoder_path = "../trained_models/hypertuned_label_encoder.pkl"
        self.model_config_path = "../trained_models/hypertuned_model_config.json"
        
        # Load model and encoder
        self.load_model_and_encoder()
        
        # Initialize predictor
        self.predictor = SignPredictor(
            model_path=self.model_path,
            label_encoder_path=self.label_encoder_path,
            config_path=self.model_config_path
        )
        
        # Frame sequence for LSTM (30 frames as per your training)
        self.sequence_length = 30
        self.frame_sequences = {}  # Store sequences per session
        
    def load_model_and_encoder(self):
        """Load the trained model and label encoder"""
        try:
            self.model = keras.models.load_model(self.model_path)
            
            with open(self.label_encoder_path, 'rb') as f:
                self.label_encoder = pickle.load(f)
                
            with open(self.model_config_path, 'r') as f:
                self.config = json.load(f)
                
        except Exception as e:
            raise Exception(f"Failed to load model: {str(e)}")

    def process_single_frame(self, image_path, session_id):
        """Process a single frame and return prediction"""
        try:
            # Read image
            image = cv2.imread(image_path)
            if image is None:
                return {"error": True, "message": "Could not read image"}
            
            # Extract pose landmarks using your existing method
            landmarks = extract_pose_landmarks(image)
            
            if landmarks is None:
                return {
                    "prediction": "No pose detected",
                    "confidence": 0.0,
                    "error": False,
                    "session_id": session_id
                }
            
            # Initialize session sequence if not exists
            if session_id not in self.frame_sequences:
                self.frame_sequences[session_id] = []
            
            # Add current frame landmarks to sequence
            self.frame_sequences[session_id].append(landmarks)
            
            # Keep only last 30 frames (sequence_length)
            if len(self.frame_sequences[session_id]) > self.sequence_length:
                self.frame_sequences[session_id] = self.frame_sequences[session_id][-self.sequence_length:]
            
            # If we have enough frames, make prediction
            if len(self.frame_sequences[session_id]) >= self.sequence_length:
                sequence = np.array(self.frame_sequences[session_id][-self.sequence_length:])
                
                # Reshape for model input
                sequence = sequence.reshape(1, self.sequence_length, -1)
                
                # Make prediction using your existing method
                prediction = self.predictor.predict(sequence)
                
                return {
                    "prediction": prediction["predicted_sign"],
                    "confidence": float(prediction["confidence"]),
                    "error": False,
                    "session_id": session_id,
                    "frame_count": len(self.frame_sequences[session_id])
                }
            else:
                # Not enough frames yet
                return {
                    "prediction": "Building sequence...",
                    "confidence": 0.0,
                    "error": False,
                    "session_id": session_id,
                    "frame_count": len(self.frame_sequences[session_id])
                }
                
        except Exception as e:
            return {
                "error": True,
                "message": f"Processing failed: {str(e)}",
                "session_id": session_id
            }

    def cleanup_session(self, session_id):
        """Clean up session data"""
        if session_id in self.frame_sequences:
            del self.frame_sequences[session_id]

def main():
    parser = argparse.ArgumentParser(description='Live Stream Frame Processor')
    parser.add_argument('--image_path', required=True, help='Path to the image file')
    parser.add_argument('--session_id', required=True, help='Session ID')
    
    args = parser.parse_args()
    
    try:
        processor = LiveStreamProcessor()
        result = processor.process_single_frame(args.image_path, args.session_id)
        print(json.dumps(result))
        
    except Exception as e:
        error_result = {
            "error": True,
            "message": str(e),
            "session_id": args.session_id
        }
        print(json.dumps(error_result))

if __name__ == "__main__":
    main()
