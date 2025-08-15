import sys
import os
import cv2
import mediapipe as mp
import numpy as np
import json
from typing import Dict, List, Tuple, Any
from flask import Flask, request, jsonify
from flask_cors import CORS
import logging


# Fix the relative import issue
sys.path.append(os.path.dirname(os.path.abspath(__file__)))


# Import the enhanced model predictor
try:
    from enhanced_model_predictor import get_predictor
    MODEL_AVAILABLE = True
    logging.info("✅ Enhanced model predictor imported successfully")
except ImportError as e:
    logging.warning(f"⚠️ Could not import enhanced_model_predictor: {e}")
    MODEL_AVAILABLE = False


# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class LessonFeedbackAnalyzer:
    def __init__(self):
        self.mp_pose = mp.solutions.pose
        self.pose = self.mp_pose.Pose(
            static_image_mode=False,
            model_complexity=1,
            smooth_landmarks=True,
            min_detection_confidence=0.5,
            min_tracking_confidence=0.5
        )
        
        # Get the trained model predictor
        if MODEL_AVAILABLE:
            self.predictor = get_predictor()
            if self.predictor.model is not None:
                logger.info("🚀 LessonFeedbackAnalyzer initialized with BDSLW60 trained model")
            else:
                logger.warning("⚠️ Model predictor created but no model loaded")
        else:
            self.predictor = None
            logger.warning("⚠️ LessonFeedbackAnalyzer initialized without trained model")
        
        # BDSLW60 vocabulary for fallback
        self.bdslw60_signs = {
            "আম": {"confidence_threshold": 0.7, "description": "Mango fruit sign"},
            "আপেল": {"confidence_threshold": 0.7, "description": "Apple fruit sign"},
            "এসি": {"confidence_threshold": 0.75, "description": "Air conditioner sign"},
            "এইডস": {"confidence_threshold": 0.8, "description": "AIDS disease sign"},
            "আলু": {"confidence_threshold": 0.7, "description": "Potato vegetable sign"},
            "আনারস": {"confidence_threshold": 0.7, "description": "Pineapple fruit sign"},
            "আঙুর": {"confidence_threshold": 0.7, "description": "Grapes fruit sign"},
            "অ্যাপার্টমেন্ট": {"confidence_threshold": 0.8, "description": "Apartment building sign"},
            "বাবা": {"confidence_threshold": 0.7, "description": "Father family sign"},
            "মা": {"confidence_threshold": 0.7, "description": "Mother family sign"},
            "ভাই": {"confidence_threshold": 0.7, "description": "Brother family sign"},
            "বোন": {"confidence_threshold": 0.7, "description": "Sister family sign"},
            "ডাক্তার": {"confidence_threshold": 0.75, "description": "Doctor profession sign"},
            "চা": {"confidence_threshold": 0.7, "description": "Tea drink sign"},
            "টিভি": {"confidence_threshold": 0.75, "description": "Television sign"},
        }


    def analyze_pose(self, pose_landmarks: List[Dict], expected_sign: str) -> Dict[str, Any]:
        """Analyze pose landmarks using the trained BDSLW60 LSTM model"""
        try:
            if not pose_landmarks:
                return self._create_error_response("No pose detected. Please ensure you are visible in the camera.")


            # Use trained model if available
            if self.predictor and MODEL_AVAILABLE and self.predictor.model is not None:
                return self._analyze_with_trained_model(pose_landmarks, expected_sign)
            else:
                return self._analyze_with_fallback(pose_landmarks, expected_sign)


        except Exception as e:
            logger.error(f"❌ Error in analyze_pose: {str(e)}")
            return self._create_error_response(f"Analysis error: {str(e)}")


    def _analyze_with_trained_model(self, pose_landmarks: List[Dict], expected_sign: str) -> Dict[str, Any]:
        """Analyze using your trained BDSLW60 LSTM model"""
        
        # Use the enhanced model predictor's debugging method
        return self.predictor.analyze_with_trained_model(pose_landmarks, expected_sign)

    def _analyze_with_fallback(self, pose_landmarks: List[Dict], expected_sign: str) -> Dict[str, Any]:
        """Fallback analysis when trained model is not available"""
        
        # Basic hand visibility check
        key_landmarks = self._extract_key_landmarks(pose_landmarks)
        
        if not self._hands_visible(key_landmarks):
            return self._create_error_response("Hands not clearly visible. Please position yourself so both hands are in view.")
        
        # Simple geometric confidence calculation
        confidence_score = self._calculate_fallback_confidence(key_landmarks, expected_sign)
        
        # Generate fallback feedback
        feedback_text = self._generate_fallback_feedback(confidence_score, expected_sign)
        improvement_tips = self._generate_fallback_tips(confidence_score, expected_sign)
        
        threshold = self.bdslw60_signs.get(expected_sign, {}).get("confidence_threshold", 0.7)
        is_correct = confidence_score >= (threshold * 100)


        return {
            "confidence_score": round(confidence_score, 2),
            "feedback_text": feedback_text,
            "is_correct": is_correct,
            "improvement_tips": improvement_tips,
            "predicted_sign": expected_sign if is_correct else None,
            "expected_sign": expected_sign,
            "model_status": "fallback"
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


    # Helper methods for fallback mode
    def _extract_key_landmarks(self, pose_landmarks: List[Dict]) -> Dict[str, Tuple[float, float, float]]:
        """Extract key landmarks for basic analysis"""
        key_points = {}
        landmark_indices = {
            "left_shoulder": 11, "right_shoulder": 12, "left_elbow": 13, "right_elbow": 14,
            "left_wrist": 15, "right_wrist": 16, "left_pinky": 17, "right_pinky": 18,
            "left_index": 19, "right_index": 20, "left_thumb": 21, "right_thumb": 22
        }


        for name, index in landmark_indices.items():
            if index < len(pose_landmarks):
                landmark = pose_landmarks[index]
                if isinstance(landmark, dict):
                    x = landmark.get('x', 0)
                    y = landmark.get('y', 0)
                    visibility = landmark.get('visibility', 1)
                else:
                    x = getattr(landmark, 'x', 0)
                    y = getattr(landmark, 'y', 0)
                    visibility = getattr(landmark, 'visibility', 1)
                key_points[name] = (x, y, visibility)
        return key_points


    def _hands_visible(self, landmarks: Dict[str, Tuple[float, float, float]]) -> bool:
        """Check if both hands are visible"""
        left_wrist = landmarks.get("left_wrist", (0, 0, 0))
        right_wrist = landmarks.get("right_wrist", (0, 0, 0))
        left_visible = len(left_wrist) > 2 and left_wrist[2] > 0.5
        right_visible = len(right_wrist) > 2 and right_wrist[2] > 0.5
        return left_visible and right_visible


    def _calculate_fallback_confidence(self, landmarks: Dict, expected_sign: str) -> float:
        """Basic confidence calculation for fallback mode"""
        visibility_scores = [landmark[2] for landmark in landmarks.values() if len(landmark) > 2]
        if not visibility_scores:
            return 0.0
        
        avg_visibility = sum(visibility_scores) / len(visibility_scores)
        base_confidence = avg_visibility * 80  # Max 80% for fallback mode
        
        if expected_sign in self.bdslw60_signs:
            base_confidence += 10
        
        return min(100.0, base_confidence)


    def _generate_fallback_feedback(self, confidence: float, expected_sign: str) -> str:
        """Generate feedback for fallback mode"""
        sign_desc = self.bdslw60_signs.get(expected_sign, {}).get("description", "BDSLW60 sign")
        if confidence >= 70:
            return f"Good visibility for '{expected_sign}' sign practice."
        elif confidence >= 50:
            return f"Fair attempt at '{expected_sign}' ({sign_desc}). Improve hand positioning."
        else:
            return f"Keep practicing '{expected_sign}' with better lighting and hand visibility."


    def _generate_fallback_tips(self, confidence: float, expected_sign: str) -> str:
        """Generate tips for fallback mode"""
        if confidence >= 70:
            return "Good hand visibility. The trained model would provide better feedback."
        else:
            return "Ensure both hands are clearly visible and well-lit. Check camera positioning."


    def _create_error_response(self, message: str) -> Dict[str, Any]:
        """Create standard error response"""
        return {
            "confidence_score": 0.0,
            "feedback_text": message,
            "is_correct": False,
            "improvement_tips": "Ensure good lighting and clear hand visibility.",
            "predicted_sign": None,
            "model_status": "error"
        }


    def reset_session(self):
        """Reset prediction session"""
        if self.predictor:
            self.predictor.reset_sequence()
        logger.info("🔄 Session reset completed")


# Flask API setup
app = Flask(__name__)
CORS(app, origins=["http://localhost:3000", "http://localhost:8080"])
analyzer = LessonFeedbackAnalyzer()


@app.route('/analyze_pose', methods=['POST'])
def analyze_pose():
    try:
        data = request.json
        if not data:
            return jsonify({
                "confidence_score": 0.0,
                "feedback_text": "No data received",
                "is_correct": False,
                "improvement_tips": "Please ensure pose data is being sent correctly."
            }), 400


        pose_landmarks = data.get('pose_landmarks', [])
        expected_sign = data.get('expected_sign', '')
        user_id = data.get('user_id', 'unknown')


        logger.info(f"🎯 Analyzing pose for user {user_id}, expected sign: {expected_sign}")
        logger.info(f"📊 Received {len(pose_landmarks)} landmarks")


        if not expected_sign:
            return jsonify({
                "confidence_score": 0.0,
                "feedback_text": "No expected sign provided",
                "is_correct": False,
                "improvement_tips": "Please specify which BDSLW60 sign you're practicing."
            }), 400


        result = analyzer.analyze_pose(pose_landmarks, expected_sign)
        
        # Log the analysis result
        logger.info(f"📊 Analysis result: {result.get('predicted_sign', 'None')} | "
                   f"Confidence: {result.get('confidence_score', 0):.2f}% | "
                   f"Correct: {result.get('is_correct', False)} | "
                   f"Status: {result.get('model_status', 'unknown')}")
        
        return jsonify(result)


    except Exception as e:
        logger.error(f"❌ Error in analyze_pose endpoint: {str(e)}")
        return jsonify({
            "confidence_score": 0.0,
            "feedback_text": f"Analysis error: {str(e)}",
            "is_correct": False,
            "improvement_tips": "Please try again with better lighting and positioning."
        }), 500


@app.route('/reset_session', methods=['POST'])
def reset_session():
    try:
        analyzer.reset_session()
        return jsonify({
            "status": "success",
            "message": "BDSLW60 model sequence reset successfully"
        })
    except Exception as e:
        return jsonify({
            "status": "error", 
            "message": f"Error resetting session: {str(e)}"
        }), 500


@app.route('/health', methods=['GET'])
def health_check():
    model_loaded = (analyzer.predictor and MODEL_AVAILABLE and analyzer.predictor.model is not None)
    model_status = "BDSLW60_loaded" if model_loaded else "fallback_mode"
    
    return jsonify({
        "status": "healthy",
        "service": "BDSLW60_enhanced_analyzer",
        "model_status": model_status,
        "model_loaded": model_loaded,
        "dataset": "BDSLW60",
        "total_classes": 60,
        "available_signs": list(analyzer.bdslw60_signs.keys())[:10]
    })


if __name__ == '__main__':
    logger.info("🚀 Starting BDSLW60 Enhanced Lesson Feedback Analyzer...")
    logger.info(f"📊 Model available: {MODEL_AVAILABLE}")
    if analyzer.predictor and analyzer.predictor.model is not None:
        logger.info("✅ Model successfully loaded!")
    else:
        logger.warning("⚠️ Model not loaded - running in fallback mode")
    logger.info(f"🎯 Dataset: BDSLW60 (60 classes)")
    logger.info(f"📋 Sample signs: {list(analyzer.bdslw60_signs.keys())[:5]}")
    app.run(host='0.0.0.0', port=5000, debug=True)
