# python-ai/learning/confidence_calculator.py
import numpy as np
import json
from typing import Dict, List, Tuple, Any
import logging

logger = logging.getLogger(__name__)

class ConfidenceCalculator:
    """
    Advanced confidence calculator for sign language pose analysis
    """
    
    def __init__(self):
        self.hand_landmarks = {
            'left_wrist': 15, 'right_wrist': 16,
            'left_pinky': 17, 'right_pinky': 18,
            'left_index': 19, 'right_index': 20,
            'left_thumb': 21, 'right_thumb': 22
        }
        
        self.body_landmarks = {
            'left_shoulder': 11, 'right_shoulder': 12,
            'left_elbow': 13, 'right_elbow': 14
        }
        
    def calculate_advanced_confidence(self, current_landmarks: List[Dict], 
                                    expected_sign: str, 
                                    reference_patterns: Dict[str, Any]) -> Dict[str, float]:
        """
        Calculate confidence using multiple metrics
        
        Returns:
            Dict with different confidence metrics
        """
        try:
            if not current_landmarks or expected_sign not in reference_patterns:
                return self._zero_confidence()
            
            # Extract normalized landmarks
            normalized_landmarks = self._normalize_landmarks(current_landmarks)
            expected_pattern = reference_patterns[expected_sign]
            
            # Calculate multiple confidence metrics
            position_confidence = self._calculate_position_confidence(
                normalized_landmarks, expected_pattern
            )
            
            stability_confidence = self._calculate_stability_confidence(
                normalized_landmarks
            )
            
            visibility_confidence = self._calculate_visibility_confidence(
                normalized_landmarks
            )
            
            symmetry_confidence = self._calculate_symmetry_confidence(
                normalized_landmarks, expected_sign
            )
            
            # Weighted combination
            overall_confidence = (
                position_confidence * 0.4 +
                stability_confidence * 0.2 +
                visibility_confidence * 0.3 +
                symmetry_confidence * 0.1
            )
            
            return {
                'overall': round(overall_confidence, 2),
                'position': round(position_confidence, 2),
                'stability': round(stability_confidence, 2),
                'visibility': round(visibility_confidence, 2),
                'symmetry': round(symmetry_confidence, 2)
            }
            
        except Exception as e:
            logger.error(f"Error in advanced confidence calculation: {e}")
            return self._zero_confidence()
    
    def _normalize_landmarks(self, landmarks: List[Dict]) -> Dict[str, Tuple[float, float, float]]:
        """Normalize landmarks relative to shoulder width"""
        normalized = {}
        
        # Get shoulder positions for normalization
        left_shoulder = self._get_landmark_position(landmarks, 11)
        right_shoulder = self._get_landmark_position(landmarks, 12)
        
        if not left_shoulder or not right_shoulder:
            return {}
        
        # Calculate shoulder width for normalization
        shoulder_width = abs(right_shoulder[0] - left_shoulder[0])
        shoulder_center = (
            (left_shoulder[0] + right_shoulder[0]) / 2,
            (left_shoulder[1] + right_shoulder[1]) / 2
        )
        
        # Normalize all landmarks
        all_landmarks = {**self.hand_landmarks, **self.body_landmarks}
        
        for name, index in all_landmarks.items():
            pos = self._get_landmark_position(landmarks, index)
            if pos:
                # Normalize relative to shoulder center and width
                norm_x = (pos[0] - shoulder_center[0]) / shoulder_width if shoulder_width > 0 else 0
                norm_y = (pos[1] - shoulder_center[1]) / shoulder_width if shoulder_width > 0 else 0
                visibility = pos[2] if len(pos) > 2 else 1.0
                
                normalized[name] = (norm_x, norm_y, visibility)
        
        return normalized
    
    def _get_landmark_position(self, landmarks: List[Dict], index: int) -> Tuple[float, float, float]:
        """Extract landmark position by index"""
        if index >= len(landmarks):
            return None
        
        landmark = landmarks[index]
        if isinstance(landmark, dict):
            x = landmark.get('x', 0)
            y = landmark.get('y', 0)
            visibility = landmark.get('visibility', 1)
        else:
            x = getattr(landmark, 'x', 0)
            y = getattr(landmark, 'y', 0)
            visibility = getattr(landmark, 'visibility', 1)
        
        return (x, y, visibility)
    
    def _calculate_position_confidence(self, landmarks: Dict, expected_pattern: Dict) -> float:
        """Calculate confidence based on hand positions"""
        if 'hand_positions' not in expected_pattern:
            return 50.0
        
        expected_positions = expected_pattern['hand_positions']
        if len(expected_positions) < 2:
            return 50.0
        
        left_wrist = landmarks.get('left_wrist', (0, 0, 0))[:2]
        right_wrist = landmarks.get('right_wrist', (0, 0, 0))[:2]
        
        # Calculate distances
        left_dist = self._euclidean_distance(left_wrist, expected_positions[0])
        right_dist = self._euclidean_distance(right_wrist, expected_positions[1])
        
        # Convert to confidence (lower distance = higher confidence)
        avg_distance = (left_dist + right_dist) / 2
        confidence = max(0, 100 - (avg_distance * 200))  # Scale factor
        
        return min(100, confidence)
    
    def _calculate_stability_confidence(self, landmarks: Dict) -> float:
        """Calculate confidence based on pose stability"""
        # Check if key landmarks are present and stable
        key_points = ['left_wrist', 'right_wrist', 'left_elbow', 'right_elbow']
        present_points = sum(1 for point in key_points if point in landmarks)
        
        if present_points == 0:
            return 0.0
        
        stability_score = (present_points / len(key_points)) * 100
        
        # Check visibility scores
        visibility_scores = []
        for point in key_points:
            if point in landmarks and len(landmarks[point]) > 2:
                visibility_scores.append(landmarks[point][2])
        
        if visibility_scores:
            avg_visibility = np.mean(visibility_scores) * 100
            stability_score = (stability_score + avg_visibility) / 2
        
        return min(100, stability_score)
    
    def _calculate_visibility_confidence(self, landmarks: Dict) -> float:
        """Calculate confidence based on landmark visibility"""
        if not landmarks:
            return 0.0
        
        visibility_scores = []
        for landmark_data in landmarks.values():
            if len(landmark_data) > 2:
                visibility_scores.append(landmark_data[2])
        
        if not visibility_scores:
            return 50.0  # Default if no visibility data
        
        avg_visibility = np.mean(visibility_scores)
        return min(100, avg_visibility * 100)
    
    def _calculate_symmetry_confidence(self, landmarks: Dict, expected_sign: str) -> float:
        """Calculate confidence based on hand symmetry (for symmetric signs)"""
        # Signs that should be symmetric
        symmetric_signs = ['নমস্কার', 'ধন্যবাদ']
        
        if expected_sign not in symmetric_signs:
            return 100.0  # Full confidence for non-symmetric signs
        
        left_wrist = landmarks.get('left_wrist', (0, 0, 0))[:2]
        right_wrist = landmarks.get('right_wrist', (0, 0, 0))[:2]
        
        # For symmetric signs, hands should be at similar heights
        height_diff = abs(left_wrist[1] - right_wrist[1])
        
        # Convert to confidence (smaller difference = higher confidence)
        symmetry_confidence = max(0, 100 - (height_diff * 500))  # Scale factor
        
        return min(100, symmetry_confidence)
    
    def _euclidean_distance(self, point1: Tuple[float, float], point2: Tuple[float, float]) -> float:
        """Calculate Euclidean distance between two points"""
        return np.sqrt((point1[0] - point2[0])**2 + (point1[1] - point2[1])**2)
    
    def _zero_confidence(self) -> Dict[str, float]:
        """Return zero confidence for all metrics"""
        return {
            'overall': 0.0,
            'position': 0.0,
            'stability': 0.0,
            'visibility': 0.0,
            'symmetry': 0.0
        }
    
    def generate_detailed_feedback(self, confidence_metrics: Dict[str, float], 
                                 expected_sign: str) -> List[str]:
        """Generate detailed feedback based on confidence metrics"""
        feedback = []
        
        if confidence_metrics['visibility'] < 50:
            feedback.append("Improve lighting and make sure both hands are clearly visible")
        
        if confidence_metrics['position'] < 60:
            feedback.append(f"Adjust hand positions for the {expected_sign} sign")
        
        if confidence_metrics['stability'] < 70:
            feedback.append("Hold the sign more steadily")
        
        if confidence_metrics['symmetry'] < 80 and expected_sign in ['নমস্কার', 'ধন্যবাদ']:
            feedback.append("Keep both hands at the same level for this symmetric sign")
        
        if not feedback:
            feedback.append("Great job! Your sign is well-formed")
        
        return feedback

# Example usage and testing
if __name__ == "__main__":
    calculator = ConfidenceCalculator()
    
    # Test with sample data
    sample_landmarks = [{'x': 0.5, 'y': 0.3, 'visibility': 0.9} for _ in range(33)]
    sample_patterns = {
        'আ': {'hand_positions': [(0.5, 0.3), (0.6, 0.4)]}
    }
    
    confidence = calculator.calculate_advanced_confidence(
        sample_landmarks, 'আ', sample_patterns
    )
    
    print("Sample confidence calculation:", confidence)
