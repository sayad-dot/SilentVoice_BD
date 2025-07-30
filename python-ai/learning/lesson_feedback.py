# python-ai/learning/lesson_feedback.py
import cv2
import mediapipe as mp
import numpy as np
import json
from typing import Dict, List, Tuple, Any
from flask import Flask, request, jsonify
from flask_cors import CORS
import logging

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
        
        # Load sign language patterns (you would load actual trained models here)
        self.sign_patterns = self._load_sign_patterns()
        logger.info("LessonFeedbackAnalyzer initialized successfully")
    
    def _load_sign_patterns(self) -> Dict[str, Any]:
        """Load pre-trained sign language patterns"""
        # This would load your actual trained models
        # For now, returning mock patterns based on common Bangla signs
        return {
            "আ": {
                "hand_positions": [(0.5, 0.3), (0.6, 0.4)], 
                "confidence_threshold": 0.7,
                "description": "Open palm facing forward"
            },
            "ই": {
                "hand_positions": [(0.4, 0.25), (0.65, 0.35)], 
                "confidence_threshold": 0.75,
                "description": "Index finger pointing up"
            },
            "উ": {
                "hand_positions": [(0.45, 0.3), (0.6, 0.4)], 
                "confidence_threshold": 0.7,
                "description": "Closed fist with thumb up"
            },
            "নমস্কার": {
                "hand_positions": [(0.4, 0.2), (0.7, 0.3)], 
                "confidence_threshold": 0.75,
                "description": "Both palms together in greeting"
            },
            "ধন্যবাদ": {
                "hand_positions": [(0.35, 0.25), (0.65, 0.35)], 
                "confidence_threshold": 0.8,
                "description": "Hand to chest, then forward"
            },
            "শুভ সকাল": {
                "hand_positions": [(0.3, 0.2), (0.7, 0.3)], 
                "confidence_threshold": 0.75,
                "description": "Rising sun gesture"
            }
        }
    
    def analyze_pose(self, pose_landmarks: List[Dict], expected_sign: str) -> Dict[str, Any]:
        """
        Analyze pose landmarks against expected sign
        
        Args:
            pose_landmarks: List of pose landmark dictionaries from MediaPipe
            expected_sign: The sign the user is supposed to perform
            
        Returns:
            Dictionary with confidence score, feedback text, and correctness
        """
        try:
            if not pose_landmarks:
                return {
                    "confidence_score": 0.0,
                    "feedback_text": "No pose detected. Please ensure you are visible in the camera.",
                    "is_correct": False,
                    "improvement_tips": "Make sure you're well-lit and within camera view."
                }
            
            # Extract key hand and arm landmarks
            key_landmarks = self._extract_key_landmarks(pose_landmarks)
            
            # Check if hands are visible
            if not self._hands_visible(key_landmarks):
                return {
                    "confidence_score": 0.0,
                    "feedback_text": "Hands not clearly visible. Please position yourself so both hands are in view.",
                    "is_correct": False,
                    "improvement_tips": "Move closer to the camera and ensure good lighting."
                }
            
            # Calculate confidence based on expected sign
            confidence_score = self._calculate_confidence(key_landmarks, expected_sign)
            
            # Generate feedback text
            feedback_text = self._generate_feedback(confidence_score, expected_sign, key_landmarks)
            
            # Generate improvement tips
            improvement_tips = self._generate_improvement_tips(confidence_score, expected_sign, key_landmarks)
            
            # Determine if sign is correct
            threshold = self.sign_patterns.get(expected_sign, {}).get("confidence_threshold", 0.7)
            is_correct = confidence_score >= (threshold * 100)  # Convert to percentage
            
            return {
                "confidence_score": round(confidence_score, 2),
                "feedback_text": feedback_text,
                "is_correct": is_correct,
                "improvement_tips": improvement_tips
            }
            
        except Exception as e:
            logger.error(f"Error in analyze_pose: {str(e)}")
            return {
                "confidence_score": 0.0,
                "feedback_text": f"Error analyzing pose: {str(e)}",
                "is_correct": False,
                "improvement_tips": "Please try again with better lighting and positioning."
            }
    
    def _extract_key_landmarks(self, pose_landmarks: List[Dict]) -> Dict[str, Tuple[float, float]]:
        """Extract key landmarks for sign language analysis"""
        key_points = {}
        
        # MediaPipe pose landmark indices for upper body
        landmark_indices = {
            "left_shoulder": 11,
            "right_shoulder": 12,
            "left_elbow": 13,
            "right_elbow": 14,
            "left_wrist": 15,
            "right_wrist": 16,
            "left_pinky": 17,
            "right_pinky": 18,
            "left_index": 19,
            "right_index": 20,
            "left_thumb": 21,
            "right_thumb": 22
        }
        
        for name, index in landmark_indices.items():
            if index < len(pose_landmarks):
                landmark = pose_landmarks[index]
                # Handle both dictionary and object formats
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
        """Check if both hands are visible with good confidence"""
        left_wrist = landmarks.get("left_wrist", (0, 0, 0))
        right_wrist = landmarks.get("right_wrist", (0, 0, 0))
        
        # Check visibility (MediaPipe visibility threshold)
        left_visible = len(left_wrist) > 2 and left_wrist[2] > 0.5
        right_visible = len(right_wrist) > 2 and right_wrist[2] > 0.5
        
        return left_visible and right_visible
    
    def _calculate_confidence(self, landmarks: Dict[str, Tuple[float, float, float]], expected_sign: str) -> float:
        """Calculate confidence score for the detected pose"""
        if expected_sign not in self.sign_patterns:
            return 30.0  # Default low confidence for unknown signs
        
        pattern = self.sign_patterns[expected_sign]
        expected_positions = pattern["hand_positions"]
        
        # Extract current hand positions
        left_wrist = landmarks.get("left_wrist", (0, 0, 0))[:2]
        right_wrist = landmarks.get("right_wrist", (0, 0, 0))[:2]
        
        if len(expected_positions) >= 2:
            # Calculate distance from expected positions
            left_distance = self._euclidean_distance(left_wrist, expected_positions[0])
            right_distance = self._euclidean_distance(right_wrist, expected_positions[1])
            
            # Convert distance to confidence (closer = higher confidence)
            avg_distance = (left_distance + right_distance) / 2
            
            # Scale distance to confidence (0-100)
            # Closer positions (smaller distance) = higher confidence
            confidence = max(0, 100 - (avg_distance * 300))  # Scale factor of 300
            
            # Add bonus for hand visibility
            left_visibility = landmarks.get("left_wrist", (0, 0, 0))[2] if len(landmarks.get("left_wrist", (0, 0, 0))) > 2 else 0
            right_visibility = landmarks.get("right_wrist", (0, 0, 0))[2] if len(landmarks.get("right_wrist", (0, 0, 0))) > 2 else 0
            visibility_bonus = (left_visibility + right_visibility) * 10
            
            final_confidence = min(100, confidence + visibility_bonus)
            return max(0, final_confidence)
        
        return 50.0  # Default confidence
    
    def _euclidean_distance(self, point1: Tuple[float, float], point2: Tuple[float, float]) -> float:
        """Calculate Euclidean distance between two points"""
        return np.sqrt((point1[0] - point2[0])**2 + (point1[1] - point2[1])**2)
    
    def _generate_feedback(self, confidence: float, expected_sign: str, landmarks: Dict) -> str:
        """Generate human-readable feedback based on confidence and landmarks"""
        sign_desc = self.sign_patterns.get(expected_sign, {}).get("description", expected_sign)
        
        if confidence >= 85:
            return f"Excellent! Your {expected_sign} sign is very accurate. Perfect hand positioning!"
        elif confidence >= 70:
            return f"Great job! Your {expected_sign} sign is mostly correct. Keep practicing for consistency."
        elif confidence >= 50:
            return f"Good attempt! For {expected_sign} ({sign_desc}), try to be more precise with hand positioning."
        elif confidence >= 30:
            return f"Getting there! Remember, {expected_sign} requires: {sign_desc}. Check your hand positions."
        else:
            return f"Keep practicing! For {expected_sign}, focus on: {sign_desc}. Make sure both hands are clearly visible."
    
    def _generate_improvement_tips(self, confidence: float, expected_sign: str, landmarks: Dict) -> str:
        """Generate specific improvement tips"""
        if confidence >= 80:
            return "You're doing great! Try to hold the sign steady for 2-3 seconds."
        elif confidence >= 60:
            return "Almost there! Pay attention to finger positioning and hand orientation."
        elif confidence >= 40:
            return "Focus on hand placement. Make sure both hands are clearly visible and in the correct position."
        else:
            return "Start with basic hand positioning. Ensure good lighting and camera angle for better detection."

# Flask API setup
app = Flask(__name__)
CORS(app, origins=["http://localhost:3000", "http://localhost:8080"])  # Add your frontend URLs

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
        
        logger.info(f"Analyzing pose for user {user_id}, expected sign: {expected_sign}")
        
        if not expected_sign:
            return jsonify({
                "confidence_score": 0.0,
                "feedback_text": "No expected sign provided",
                "is_correct": False,
                "improvement_tips": "Please specify which sign you're practicing."
            }), 400
        
        result = analyzer.analyze_pose(pose_landmarks, expected_sign)
        return jsonify(result)
        
    except Exception as e:
        logger.error(f"Error in analyze_pose endpoint: {str(e)}")
        return jsonify({
            "confidence_score": 0.0,
            "feedback_text": f"Analysis error: {str(e)}",
            "is_correct": False,
            "improvement_tips": "Please try again with better lighting and positioning."
        }), 500

@app.route('/health', methods=['GET'])
def health_check():
    return jsonify({
        "status": "healthy",
        "service": "lesson_feedback_analyzer",
        "available_signs": list(analyzer.sign_patterns.keys())
    })

@app.route('/signs', methods=['GET'])
def get_available_signs():
    """Get list of available signs for training"""
    signs_info = {}
    for sign, pattern in analyzer.sign_patterns.items():
        signs_info[sign] = {
            "description": pattern.get("description", ""),
            "difficulty": "Easy" if pattern.get("confidence_threshold", 0.7) <= 0.7 else "Medium"
        }
    
    return jsonify({
        "available_signs": signs_info,
        "total_count": len(signs_info)
    })

if __name__ == '__main__':
    logger.info("Starting Lesson Feedback Analyzer service...")
    logger.info(f"Available signs: {list(analyzer.sign_patterns.keys())}")
    app.run(host='0.0.0.0', port=5000, debug=True)
