#!/usr/bin/env python3
"""
Real-time Processing Manager for SilentVoice_BD
- Optimized for live streaming and mirror practice
- Frame buffer management
- Quality-based frame selection
- WebSocket integration support
"""

import cv2
import numpy as np
import json
import sys
import os
import time
import threading
from queue import Queue, Empty
from typing import List, Dict, Optional, Tuple
from pathlib import Path
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class RealTimeProcessor:
    """Real-time sign language processing for live streams"""
    
    def __init__(self, config_path: Optional[str] = None):
        self.config = self._load_config(config_path)
        
        # Buffer settings
        self.buffer_size = self.config.get('buffer_size', 30)
        self.min_frames_for_prediction = self.config.get('min_frames_for_prediction', 15)
        self.prediction_interval = self.config.get('prediction_interval_ms', 1000)  # 1 second
        
        # Frame buffers
        self.frame_buffer = []
        self.pose_buffer = []
        self.quality_scores = []
        
        # Processing state
        self.is_processing = False
        self.last_prediction_time = 0
        self.frame_counter = 0
        
        # Initialize pose extractor
        from enhanced_pose_extractor import EnhancedPoseExtractor
        self.pose_extractor = EnhancedPoseExtractor(config_path)
        
        # Statistics
        self.stats = {
            'frames_processed': 0,
            'predictions_made': 0,
            'average_fps': 0.0,
            'average_confidence': 0.0,
            'last_prediction_time': 0
        }
    
    def _load_config(self, config_path: Optional[str]) -> Dict:
        """Load real-time processing configuration"""
        default_config = {
            'buffer_size': 30,
            'min_frames_for_prediction': 15,
            'prediction_interval_ms': 1000,
            'quality_threshold': 0.4,
            'enable_frame_skipping': True,
            'max_processing_time_ms': 500,
            'enable_quality_filtering': True
        }
        
        if config_path and os.path.exists(config_path):
            try:
                with open(config_path, 'r') as f:
                    user_config = json.load(f)
                default_config.update(user_config)
            except Exception as e:
                logger.warning(f"Failed to load config: {e}")
        
        return default_config
    
    def process_frame(self, frame_data: np.ndarray) -> Dict:
        """Process a single frame for real-time analysis"""
        try:
            start_time = time.time()
            self.frame_counter += 1
            
            # Convert frame to pose data
            pose_result = self._extract_pose_from_frame(frame_data)
            
            if not pose_result['success']:
                return {
                    'success': False,
                    'error': 'Pose extraction failed',
                    'frame_id': self.frame_counter
                }
            
            # Add to buffer
            pose_features = pose_result['features']
            quality_score = pose_result['quality_score']
            
            self._add_to_buffer(pose_features, quality_score)
            
            # Check if we should make a prediction
            current_time = time.time() * 1000  # Convert to milliseconds
            time_since_last = current_time - self.last_prediction_time
            
            should_predict = (
                len(self.pose_buffer) >= self.min_frames_for_prediction and
                time_since_last >= self.prediction_interval
            )
            
            result = {
                'success': True,
                'frame_id': self.frame_counter,
                'buffer_size': len(self.pose_buffer),
                'quality_score': quality_score,
                'processing_time_ms': (time.time() - start_time) * 1000,
                'prediction_ready': should_predict
            }
            
            # Make prediction if ready
            if should_predict:
                prediction_result = self._make_prediction()
                result['prediction'] = prediction_result
                self.last_prediction_time = current_time
                self.stats['predictions_made'] += 1
            
            self.stats['frames_processed'] += 1
            return result
            
        except Exception as e:
            logger.error(f"Frame processing failed: {e}")
            return {
                'success': False,
                'error': str(e),
                'frame_id': self.frame_counter
            }
    
    def _extract_pose_from_frame(self, frame: np.ndarray) -> Dict:
        """Extract pose features from a single frame"""
        try:
            # Save frame temporarily for pose extraction
            temp_frame_path = f"/tmp/temp_frame_{self.frame_counter}.jpg"
            cv2.imwrite(temp_frame_path, frame)
            
            # Extract poses using the enhanced extractor
            result = self.pose_extractor.process_frame_sequence([temp_frame_path], max_frames=1)
            
            # Clean up temporary file
            if os.path.exists(temp_frame_path):
                os.remove(temp_frame_path)
            
            if result['success'] and len(result['pose_sequence']) > 0:
                return {
                    'success': True,
                    'features': result['pose_sequence'][0],
                    'quality_score': result.get('quality_score', 0.5)
                }
            else:
                return {
                    'success': False,
                    'error': result.get('error', 'Unknown pose extraction error')
                }
                
        except Exception as e:
            return {
                'success': False,
                'error': f"Pose extraction failed: {str(e)}"
            }
    
    def _add_to_buffer(self, pose_features: List[float], quality_score: float):
        """Add pose features to the processing buffer"""
        self.pose_buffer.append(pose_features)
        self.quality_scores.append(quality_score)
        
        # Maintain buffer size
        if len(self.pose_buffer) > self.buffer_size:
            self.pose_buffer.pop(0)
            self.quality_scores.pop(0)
    
    def _make_prediction(self) -> Dict:
        """Make a prediction using current buffer contents"""
        try:
            if len(self.pose_buffer) < self.min_frames_for_prediction:
                return {
                    'success': False,
                    'error': 'Insufficient frames for prediction'
                }
            
            # Apply quality filtering if enabled
            if self.config.get('enable_quality_filtering', True):
                filtered_sequence = self._apply_quality_filtering()
            else:
                filtered_sequence = self.pose_buffer.copy()
            
            # Use enhanced sign predictor
            from enhanced_sign_predictor import EnhancedSignLanguagePredictor
            
            script_dir = Path(__file__).parent
            model_path = script_dir / '..' / 'models' / 'bangla_lstm_model.h5'
            
            predictor = EnhancedSignLanguagePredictor(str(model_path))
            result = predictor.predict(filtered_sequence)
            
            # Update statistics
            if result.get('success', False):
                confidence = result.get('confidence', 0.0)
                if self.stats['average_confidence'] == 0:
                    self.stats['average_confidence'] = confidence
                else:
                    # Exponential moving average
                    alpha = 0.1
                    self.stats['average_confidence'] = (
                        alpha * confidence + (1 - alpha) * self.stats['average_confidence']
                    )
            
            return result
            
        except Exception as e:
            logger.error(f"Prediction failed: {e}")
            return {
                'success': False,
                'error': f"Prediction failed: {str(e)}"
            }
    
    def _apply_quality_filtering(self) -> List[List[float]]:
        """Apply quality filtering to current buffer"""
        if not self.quality_scores:
            return self.pose_buffer.copy()
        
        threshold = self.config.get('quality_threshold', 0.4)
        
        # Create pairs of (pose, quality) and filter
        paired_data = list(zip(self.pose_buffer, self.quality_scores))
        filtered_pairs = [
            (pose, quality) for pose, quality in paired_data 
            if quality >= threshold
        ]
        
        # If too few frames pass filter, keep best ones
        if len(filtered_pairs) < self.min_frames_for_prediction:
            # Sort by quality and take top frames
            paired_data.sort(key=lambda x: x[1], reverse=True)
            filtered_pairs = paired_data[:self.min_frames_for_prediction]
        
        # Extract just the pose data
        return [pose for pose, _ in filtered_pairs]
    
    def reset_buffer(self):
        """Reset the processing buffer"""
        self.pose_buffer.clear()
        self.quality_scores.clear()
        self.last_prediction_time = 0
        logger.info("Buffer reset completed")
    
    def get_stats(self) -> Dict:
        """Get processing statistics"""
        current_time = time.time()
        if self.stats['frames_processed'] > 0:
            # Calculate approximate FPS
            elapsed_time = current_time - (self.stats.get('start_time', current_time))
            if elapsed_time > 0:
                self.stats['average_fps'] = self.stats['frames_processed'] / elapsed_time
        
        return self.stats.copy()
    
    def process_video_stream(self, video_source: int = 0) -> None:
        """Process video stream from camera (for testing)"""
        cap = cv2.VideoCapture(video_source)
        if not cap.isOpened():
            logger.error(f"Cannot open video source: {video_source}")
            return
        
        logger.info(f"Starting video stream processing from source: {video_source}")
        self.stats['start_time'] = time.time()
        
        try:
            while True:
                ret, frame = cap.read()
                if not ret:
                    break
                
                # Process frame
                result = self.process_frame(frame)
                
                # Display results (for testing)
                if result.get('prediction'):
                    pred = result['prediction']
                    if pred.get('success'):
                        text = f"Prediction: {pred.get('predicted_text', 'Unknown')}"
                        confidence = pred.get('confidence', 0.0)
                        cv2.putText(frame, text, (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 
                                  1, (0, 255, 0), 2)
                        cv2.putText(frame, f"Confidence: {confidence:.3f}", (10, 70), 
                                  cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
                
                # Show frame
                cv2.imshow('SilentVoice_BD Real-time Processing', frame)
                
                # Break on 'q' key
                if cv2.waitKey(1) & 0xFF == ord('q'):
                    break
                    
        except KeyboardInterrupt:
            logger.info("Stream processing interrupted by user")
        finally:
            cap.release()
            cv2.destroyAllWindows()
            logger.info("Video stream processing completed")

class StreamingAPI:
    """API for real-time streaming integration"""
    
    def __init__(self, config_path: Optional[str] = None):
        self.processor = RealTimeProcessor(config_path)
        self.active_sessions = {}
    
    def create_session(self, session_id: str) -> Dict:
        """Create a new processing session"""
        if session_id in self.active_sessions:
            return {'success': False, 'error': 'Session already exists'}
        
        self.active_sessions[session_id] = {
            'processor': RealTimeProcessor(),
            'created_at': time.time(),
            'frame_count': 0
        }
        
        return {
            'success': True,
            'session_id': session_id,
            'message': 'Session created successfully'
        }
    
    def process_frame_data(self, session_id: str, frame_data: str) -> Dict:
        """Process base64 encoded frame data"""
        if session_id not in self.active_sessions:
            return {'success': False, 'error': 'Session not found'}
        
        try:
            import base64
            
            # Decode frame data
            frame_bytes = base64.b64decode(frame_data)
            frame_array = np.frombuffer(frame_bytes, dtype=np.uint8)
            frame = cv2.imdecode(frame_array, cv2.IMREAD_COLOR)
            
            if frame is None:
                return {'success': False, 'error': 'Invalid frame data'}
            
            # Process frame
            session = self.active_sessions[session_id]
            result = session['processor'].process_frame(frame)
            session['frame_count'] += 1
            
            return result
            
        except Exception as e:
            return {'success': False, 'error': f'Frame processing failed: {str(e)}'}
    
    def get_session_stats(self, session_id: str) -> Dict:
        """Get statistics for a session"""
        if session_id not in self.active_sessions:
            return {'success': False, 'error': 'Session not found'}
        
        session = self.active_sessions[session_id]
        stats = session['processor'].get_stats()
        stats['session_frame_count'] = session['frame_count']
        stats['session_duration'] = time.time() - session['created_at']
        
        return {'success': True, 'stats': stats}
    
    def close_session(self, session_id: str) -> Dict:
        """Close a processing session"""
        if session_id not in self.active_sessions:
            return {'success': False, 'error': 'Session not found'}
        
        del self.active_sessions[session_id]
        return {'success': True, 'message': 'Session closed successfully'}

def main():
    """Command-line interface for real-time processing"""
    if len(sys.argv) < 2:
        print(json.dumps({
            'success': False,
            'error': 'Usage: python realtime_processor.py <mode> [args]'
        }))
        return
    
    mode = sys.argv[1].lower()
    
    try:
        if mode == 'stream':
            # Live video stream processing
            video_source = int(sys.argv[2]) if len(sys.argv) > 2 else 0
            processor = RealTimeProcessor()
            processor.process_video_stream(video_source)
            
        elif mode == 'frame':
            # Single frame processing (for API integration)
            if len(sys.argv) < 3:
                print(json.dumps({
                    'success': False,
                    'error': 'Frame data required'
                }))
                return
            
            frame_data = sys.argv[2]
            processor = RealTimeProcessor()
            
            # Decode and process frame
            import base64
            frame_bytes = base64.b64decode(frame_data)
            frame_array = np.frombuffer(frame_bytes, dtype=np.uint8)
            frame = cv2.imdecode(frame_array, cv2.IMREAD_COLOR)
            
            if frame is None:
                print(json.dumps({
                    'success': False,
                    'error': 'Invalid frame data'
                }))
                return
            
            result = processor.process_frame(frame)
            print(json.dumps(result, ensure_ascii=False))
            
        elif mode == 'test':
            # Test mode - process sample frames
            processor = RealTimeProcessor()
            
            # Create sample frames for testing
            for i in range(5):
                # Create a dummy frame
                test_frame = np.random.randint(0, 255, (480, 640, 3), dtype=np.uint8)
                result = processor.process_frame(test_frame)
                print(f"Frame {i+1}: {json.dumps(result, ensure_ascii=False)}")
                time.sleep(0.1)  # Simulate real-time delay
        
        else:
            print(json.dumps({
                'success': False,
                'error': f'Unknown mode: {mode}'
            }))
    
    except Exception as e:
        print(json.dumps({
            'success': False,
            'error': f'Processing failed: {str(e)}'
        }))

if __name__ == "__main__":
    main()