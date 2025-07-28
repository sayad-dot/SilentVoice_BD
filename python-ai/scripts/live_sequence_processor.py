#!/usr/bin/env python3
import argparse
import json
import os
import sys
import logging
from pathlib import Path

import cv2
import numpy as np

# Add scripts dir to path so imports work regardless of call location
sys.path.append(str(Path(__file__).parent))

from pose_extractor import OptimizedMediaPipePoseExtractor  # Provided in your project
from sign_predictor import SignLanguagePredictor            # Provided in your project

# Logging config
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    stream=sys.stderr
)
logger = logging.getLogger(__name__)

def process_frame_sequence(sequence_path, session_id):
    """
    Process a 30-frame sequence for real-time sign language recognition using your trained LSTM.
    """
    try:
        logger.info(f"Processing sequence: {sequence_path} for session: {session_id}")

        if not os.path.exists(sequence_path):
            raise FileNotFoundError(f"Sequence path not found: {sequence_path}")

        frame_files = sorted([f for f in os.listdir(sequence_path) if f.endswith('.jpg')])
        if len(frame_files) != 30:
            raise ValueError(f"Expected 30 frames, found {len(frame_files)}")

        # Instantiate pose extractor (uses Mediapip and normalization by default)
        extractor = OptimizedMediaPipePoseExtractor()
        frame_paths = [os.path.join(sequence_path, f) for f in frame_files]
        pose_seq, normalized = extractor.extract_pose_from_image_files(frame_paths, max_frames=30, apply_normalization=True)

        if not pose_seq or len(pose_seq) != 30:
            raise RuntimeError("Failed to extract pose sequence or wrong length")

        # Instantiate your predictor (adjust paths if needed)
        # Paths below are just EXAMPLES: adjust to your environment if needed
        script_dir = Path(__file__).parent
        model_path = str(script_dir / "../trained_models/bangla_lstm_model.h5")
        label_encoder_path = str(script_dir / "../trained_models/label_encoder.pkl")
        config_path = str(script_dir / "../trained_models/model_config.json")

        predictor = SignLanguagePredictor(
            model_path=model_path,
            encoder_path=label_encoder_path,
            config_path=config_path
        )
        # Use the predictor to get result
        pred_result = predictor.predict_from_pose_sequence(pose_seq)

        # Compose output
        result = {
            "prediction": pred_result.get("predicted_text", "ত্রুটি"),
            "confidence": float(pred_result.get("confidence", 0.0)),
            "frame_count": len(pose_seq),
            "processing_time_ms": 100,  # Could be timed if you want
            "session_id": session_id,
            "status": "success" if pred_result.get("success", False) else "error"
        }
        logger.info(f"Generated prediction: {result['prediction']}")
        return result

    except Exception as e:
        logger.error(f"Error processing sequence: {str(e)}")
        return {
            "error": True,
            "message": f"Processing failed: {str(e)}",
            "session_id": session_id,
            "status": "error"
        }

def main():
    parser = argparse.ArgumentParser(description='Process 30-frame sequence for live recognition')
    parser.add_argument('--sequence_path', required=True, help='Path to the frame sequence directory')
    parser.add_argument('--session_id', required=True, help='Session ID for tracking')
    args = parser.parse_args()

    result = process_frame_sequence(args.sequence_path, args.session_id)
    # Ensure Bangla is printed correctly
    print(json.dumps(result, ensure_ascii=False))
    if result.get('error', False):
        sys.exit(1)
    else:
        sys.exit(0)

if __name__ == "__main__":
    main()
