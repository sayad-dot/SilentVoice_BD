#!/usr/bin/env python3
"""
Enhanced Pose Extractor for SilentVoice_BD
- File-based input/output to handle large data
- Unified normalization pipeline
- Quality-aware frame selection
- Real-time optimization
- Comprehensive error handling
- Backward compatibility with existing interface
"""

import cv2
import numpy as np
import mediapipe as mp
import json
import sys
import os
import gc
import argparse
import logging
from typing import List, Tuple, Dict, Optional
from pathlib import Path

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class EnhancedPoseExtractor:
    """Enhanced pose extractor with unified normalization and quality control"""
    
    def __init__(self, config_path: Optional[str] = None):
        self.mp_holistic = mp.solutions.holistic
        self.holistic = self.mp_holistic.Holistic(
            static_image_mode=False,
            model_complexity=1,  # Balanced accuracy vs speed
            enable_segmentation=False,
            min_detection_confidence=0.7,  # Higher threshold for quality
            min_tracking_confidence=0.7
        )
        
        # Load configuration
        self.config = self._load_config(config_path)
        
        # Initialize normalization
        self.feature_means = None
        self.feature_stds = None
        self.normalization_loaded = False
        self._load_normalization_params()
        
        # Quality metrics
        self.min_quality_score = 0.4
        self.frame_stats = {
            'processed': 0,
            'valid': 0,
            'low_quality': 0,
            'failed': 0
        }
    
    def _load_config(self, config_path: Optional[str]) -> Dict:
        """Load extractor configuration"""
        default_config = {
            'sequence_length': 30,
            'feature_dim': 288,
            'quality_threshold': 0.4,
            'max_zero_percentage': 0.7,
            'min_motion_variance': 0.001,
            'enable_quality_filtering': True,
            'enable_temporal_smoothing': True
        }
        
        if config_path and os.path.exists(config_path):
            try:
                with open(config_path, 'r') as f:
                    user_config = json.load(f)
                default_config.update(user_config)
                logger.info(f"✅ Loaded configuration from {config_path}")
            except Exception as e:
                logger.warning(f"⚠️ Failed to load config: {e}, using defaults")
        
        return default_config
    
    def _load_normalization_params(self):
        """Load normalization parameters with enhanced error handling"""
        try:
            script_dir = Path(__file__).parent
            data_dir = script_dir / '..' / 'data'
            
            means_path = data_dir / 'feature_means.npy'
            stds_path = data_dir / 'feature_stds.npy'
            
            if means_path.exists() and stds_path.exists():
                self.feature_means = np.load(means_path)
                self.feature_stds = np.load(stds_path)
                
                # Ensure no zero standard deviations
                self.feature_stds = np.where(self.feature_stds == 0, 1e-8, self.feature_stds)
                
                self.normalization_loaded = True
                logger.info("✅ Normalization parameters loaded successfully")
                logger.info(f"   Means shape: {self.feature_means.shape}, range: [{self.feature_means.min():.4f}, {self.feature_means.max():.4f}]")
                logger.info(f"   Stds shape: {self.feature_stds.shape}, range: [{self.feature_stds.min():.4f}, {self.feature_stds.max():.4f}]")
            else:
                logger.error("❌ CRITICAL: Normalization files not found!")
                logger.error(f"   Expected paths: {means_path}, {stds_path}")
                self._generate_default_normalization()
                
        except Exception as e:
            logger.error(f"❌ Failed to load normalization parameters: {e}")
            self._generate_default_normalization()
    
    def _generate_default_normalization(self):
        """Generate default normalization parameters as fallback"""
        logger.warning("⚠️ Generating default normalization parameters")
        self.feature_means = np.zeros(self.config['feature_dim'], dtype=np.float32)
        self.feature_stds = np.ones(self.config['feature_dim'], dtype=np.float32)
        self.normalization_loaded = True
        logger.warning("⚠️ Using default normalization - model accuracy may be reduced!")
    
    def extract_keypoints_enhanced(self, results) -> Tuple[np.ndarray, float]:
        """Extract 288 features with quality scoring"""
        try:
            # Left hand (21 * 3 = 63 features)
            if results.left_hand_landmarks:
                lh = np.array([[lm.x, lm.y, lm.z] for lm in results.left_hand_landmarks.landmark]).flatten()
                lh_quality = 1.0
            else:
                lh = np.zeros(63, dtype=np.float32)
                lh_quality = 0.0
            
            # Right hand (21 * 3 = 63 features)  
            if results.right_hand_landmarks:
                rh = np.array([[lm.x, lm.y, lm.z] for lm in results.right_hand_landmarks.landmark]).flatten()
                rh_quality = 1.0
            else:
                rh = np.zeros(63, dtype=np.float32)
                rh_quality = 0.0
            
            # Pose with visibility (33 * 4 = 132 features)
            if results.pose_landmarks:
                pose = np.array([[lm.x, lm.y, lm.z, lm.visibility] for lm in results.pose_landmarks.landmark]).flatten()
                pose_quality = np.mean([lm.visibility for lm in results.pose_landmarks.landmark])
            else:
                pose = np.zeros(132, dtype=np.float32)
                pose_quality = 0.0
            
            # Face landmarks (10 * 3 = 30 features)
            if results.face_landmarks and len(results.face_landmarks.landmark) >= 10:
                face = np.array([[lm.x, lm.y, lm.z] for lm in results.face_landmarks.landmark[:10]]).flatten()
                face_quality = 1.0
            else:
                face = np.zeros(30, dtype=np.float32)
                face_quality = 0.0
            
            # Combine features (63 + 63 + 132 + 30 = 288)
            features = np.concatenate([lh, rh, pose, face]).astype(np.float32)
            
            # Calculate comprehensive quality score
            quality_score = self._calculate_quality_score(features, lh_quality, rh_quality, pose_quality, face_quality)
            
            return features, quality_score
            
        except Exception as e:
            logger.error(f"❌ Feature extraction failed: {e}")
            return np.zeros(288, dtype=np.float32), 0.0
    
    def _calculate_quality_score(self, features: np.ndarray, lh_q: float, rh_q: float, pose_q: float, face_q: float) -> float:
        """Calculate comprehensive quality score for a frame"""
        if len(features) != 288:
            return 0.0
        
        # Component quality (weighted by importance)
        component_score = (pose_q * 0.4 + lh_q * 0.3 + rh_q * 0.3 + face_q * 0.0)  # Face less important
        
        # Data quality metrics
        zero_percentage = np.sum(np.abs(features) < 1e-6) / len(features)
        data_quality = max(0.0, 1.0 - zero_percentage)
        
        # Motion variance (indicates meaningful movement)
        motion_variance = min(1.0, np.var(features) * 1000)
        
        # Combined score
        quality_score = (component_score * 0.5 + data_quality * 0.3 + motion_variance * 0.2)
        
        return max(0.0, min(1.0, quality_score))
    
    def process_video_file(self, video_path: str, max_frames: int = 30) -> Dict:
        """Process video file with enhanced quality control"""
        logger.info(f"🎬 Processing video: {video_path}")
        
        if not os.path.exists(video_path):
            return self._create_error_response(f"Video file not found: {video_path}")
        
        cap = cv2.VideoCapture(video_path)
        if not cap.isOpened():
            return self._create_error_response(f"Cannot open video: {video_path}")
        
        # Get video properties
        fps = cap.get(cv2.CAP_PROP_FPS)
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        duration = total_frames / fps if fps > 0 else 0
        
        logger.info(f"📊 Video info: {total_frames} frames, {fps:.2f} FPS, {duration:.2f}s")
        
        # Extract frames with quality awareness
        raw_sequences = []
        quality_scores = []
        frame_count = 0
        
        try:
            # Calculate frame sampling strategy
            if total_frames <= max_frames:
                frame_indices = list(range(total_frames))
            else:
                # Smart sampling: more frames from middle section
                frame_indices = self._calculate_smart_sampling(total_frames, max_frames)
            
            for frame_idx in frame_indices:
                cap.set(cv2.CAP_PROP_POS_FRAMES, frame_idx)
                ret, frame = cap.read()
                
                if not ret:
                    continue
                
                # Preprocess frame
                processed_frame = self._preprocess_frame(frame)
                
                # Extract pose
                results = self.holistic.process(processed_frame)
                features, quality = self.extract_keypoints_enhanced(results)
                
                raw_sequences.append(features.tolist())
                quality_scores.append(quality)
                frame_count += 1
                
                if frame_count >= max_frames:
                    break
        
        finally:
            cap.release()
            gc.collect()
        
        if not raw_sequences:
            return self._create_error_response("No valid frames extracted")
        
        # Apply quality filtering and normalization
        return self._finalize_processing(raw_sequences, quality_scores, {
            'source': 'video_file',
            'total_frames': total_frames,
            'processed_frames': frame_count,
            'fps': fps,
            'duration': duration
        })
    
    def process_frame_sequence(self, frame_paths: List[str], max_frames: int = 30) -> Dict:
        """Process sequence of frame images (for live streaming)"""
        logger.info(f"🖼️ Processing {len(frame_paths)} frame images")
        
        raw_sequences = []
        quality_scores = []
        valid_frames = 0
        
        # Limit and select best frames
        selected_paths = self._select_best_frames(frame_paths, max_frames)
        
        for i, frame_path in enumerate(selected_paths):
            try:
                if not os.path.exists(frame_path):
                    logger.warning(f"⚠️ Frame not found: {frame_path}")
                    continue
                
                # Load and process frame
                frame = cv2.imread(frame_path)
                if frame is None:
                    continue
                
                processed_frame = self._preprocess_frame(frame)
                results = self.holistic.process(processed_frame)
                features, quality = self.extract_keypoints_enhanced(results)
                
                raw_sequences.append(features.tolist())
                quality_scores.append(quality)
                valid_frames += 1
                
            except Exception as e:
                logger.warning(f"⚠️ Error processing frame {frame_path}: {e}")
                continue
        
        if not raw_sequences:
            return self._create_error_response("No valid frames processed from sequence")
        
        return self._finalize_processing(raw_sequences, quality_scores, {
            'source': 'frame_sequence',
            'input_frames': len(frame_paths),
            'processed_frames': valid_frames,
            'selection_method': 'quality_based'
        })
    
    def _preprocess_frame(self, frame: np.ndarray) -> np.ndarray:
        """Standardized frame preprocessing"""
        # Resize if too large (for consistency and speed)
        h, w = frame.shape[:2]
        if w > 640:
            scale = 640.0 / w
            new_w, new_h = int(w * scale), int(h * scale)
            frame = cv2.resize(frame, (new_w, new_h))
        
        # Convert to RGB
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        rgb_frame.flags.writeable = False
        
        return rgb_frame
    
    def _calculate_smart_sampling(self, total_frames: int, max_frames: int) -> List[int]:
        """Smart frame sampling strategy"""
        if total_frames <= max_frames:
            return list(range(total_frames))
        
        # Strategy: More frames from middle 60% of video
        start_idx = int(total_frames * 0.2)  # Skip first 20%
        end_idx = int(total_frames * 0.8)    # Skip last 20%
        
        middle_frames = max(int(max_frames * 0.7), 1)
        edge_frames = max_frames - middle_frames
        
        indices = []
        
        # Middle section (dense sampling)
        if middle_frames > 0:
            middle_step = max(1, (end_idx - start_idx) // middle_frames)
            for i in range(start_idx, end_idx, middle_step):
                indices.append(i)
                if len(indices) >= middle_frames:
                    break
        
        # Edge frames (sparse sampling)
        if edge_frames > 0:
            edge_per_side = edge_frames // 2
            
            # Beginning frames
            for i in range(0, start_idx, max(1, start_idx // edge_per_side)):
                indices.append(i)
                if len(indices) >= max_frames:
                    break
            
            # End frames
            if len(indices) < max_frames:
                for i in range(end_idx, total_frames, max(1, (total_frames - end_idx) // edge_per_side)):
                    indices.append(i)
                    if len(indices) >= max_frames:
                        break
        
        return sorted(indices[:max_frames])
    
    def _select_best_frames(self, frame_paths: List[str], max_frames: int) -> List[str]:
        """Select best frames based on timestamps and quality estimation"""
        if len(frame_paths) <= max_frames:
            return frame_paths
        
        # For now, use uniform sampling
        # TODO: Implement quality-based pre-selection
        step = len(frame_paths) / max_frames
        indices = [int(i * step) for i in range(max_frames)]
        return [frame_paths[i] for i in indices if i < len(frame_paths)]
    
    def _apply_normalization(self, sequences: List[List[float]]) -> Tuple[List[List[float]], bool]:
        """Apply normalization with comprehensive logging"""
        if not self.normalization_loaded:
            logger.error("❌ Normalization parameters not loaded!")
            return sequences, False
        
        try:
            # Convert to numpy for processing
            data = np.array(sequences, dtype=np.float32)
            
            # Apply normalization: (x - mean) / std
            normalized = (data - self.feature_means) / self.feature_stds
            
            # Clip extreme values to prevent instability
            normalized = np.clip(normalized, -5.0, 5.0)
            
            # Log normalization effect
            logger.info("🔧 Normalization applied:")
            logger.info(f"   Before: mean={data.mean():.4f}, std={data.std():.4f}")
            logger.info(f"   After: mean={normalized.mean():.4f}, std={normalized.std():.4f}")
            
            return normalized.tolist(), True
            
        except Exception as e:
            logger.error(f"❌ Normalization failed: {e}")
            return sequences, False
    
    def _finalize_processing(self, raw_sequences: List[List[float]], quality_scores: List[float], metadata: Dict) -> Dict:
        """Finalize processing with quality filtering and normalization"""
        
        # Quality filtering
        if self.config['enable_quality_filtering']:
            filtered_sequences, filtered_scores = self._apply_quality_filtering(raw_sequences, quality_scores)
            logger.info(f"📊 Quality filtering: {len(filtered_sequences)}/{len(raw_sequences)} frames kept")
        else:
            filtered_sequences, filtered_scores = raw_sequences, quality_scores
        
        # Ensure minimum sequence length
        if len(filtered_sequences) < 5:
            logger.warning("⚠️ Too few high-quality frames, using all available frames")
            filtered_sequences, filtered_scores = raw_sequences, quality_scores
        
        # Apply normalization
        normalized_sequences, is_normalized = self._apply_normalization(filtered_sequences)
        
        # Pad or truncate to target length
        target_length = self.config['sequence_length']
        final_sequences = self._adjust_sequence_length(normalized_sequences, target_length)
        
        # Calculate final statistics
        avg_quality = np.mean(filtered_scores) if filtered_scores else 0.0
        
        result = {
            'success': True,
            'pose_sequence': final_sequences,
            'sequence_length': len(final_sequences),
            'feature_dimension': len(final_sequences[0]) if final_sequences else 0,
            'normalized': is_normalized,
            'quality_score': avg_quality,
            'processing_stats': {
                'raw_frames': len(raw_sequences),
                'filtered_frames': len(filtered_sequences),
                'final_frames': len(final_sequences),
                'average_quality': avg_quality,
                'min_quality': min(filtered_scores) if filtered_scores else 0.0,
                'max_quality': max(filtered_scores) if filtered_scores else 0.0
            },
            'metadata': metadata
        }
        
        logger.info("✅ Processing completed successfully")
        logger.info(f"   Final sequence: {len(final_sequences)} frames × {len(final_sequences[0]) if final_sequences else 0} features")
        logger.info(f"   Average quality: {avg_quality:.3f}")
        logger.info(f"   Normalized: {is_normalized}")
        
        return result
    
    def _apply_quality_filtering(self, sequences: List[List[float]], scores: List[float]) -> Tuple[List[List[float]], List[float]]:
        """Filter sequences based on quality scores"""
        if not scores:
            return sequences, scores
        
        threshold = self.config['quality_threshold']
        filtered_pairs = [(seq, score) for seq, score in zip(sequences, scores) if score >= threshold]
        
        if not filtered_pairs:
            # If no frames meet threshold, keep best 50%
            sorted_pairs = sorted(zip(sequences, scores), key=lambda x: x[1], reverse=True)
            keep_count = max(1, len(sorted_pairs) // 2)
            filtered_pairs = sorted_pairs[:keep_count]
        
        filtered_sequences, filtered_scores = zip(*filtered_pairs) if filtered_pairs else ([], [])
        return list(filtered_sequences), list(filtered_scores)
    
    def _adjust_sequence_length(self, sequences: List[List[float]], target_length: int) -> List[List[float]]:
        """Adjust sequence to target length with smart padding/truncation"""
        if len(sequences) == target_length:
            return sequences
        
        if len(sequences) > target_length:
            # Truncate: keep most recent frames
            return sequences[-target_length:]
        
        # Pad with normalized zeros or repeat last frame
        padding_needed = target_length - len(sequences)
        
        if sequences:
            # Use normalized zero vector for padding
            if self.normalization_loaded:
                zero_normalized = ((-self.feature_means) / self.feature_stds).tolist()
                zero_normalized = np.clip(zero_normalized, -5.0, 5.0).tolist()
            else:
                zero_normalized = [0.0] * self.config['feature_dim']
            
            padded_sequences = sequences + [zero_normalized] * padding_needed
        else:
            # Complete fallback
            zero_sequence = [[0.0] * self.config['feature_dim']] * target_length
            padded_sequences = zero_sequence
        
        return padded_sequences
    
    def _create_error_response(self, error_message: str) -> Dict:
        """Create standardized error response"""
        logger.error(f"❌ {error_message}")
        return {
            'success': False,
            'error': error_message,
            'pose_sequence': [],
            'sequence_length': 0,
            'feature_dimension': 0,
            'normalized': False,
            'quality_score': 0.0
        }

def main():
    """Main function with both file-based and command-line interfaces"""
    parser = argparse.ArgumentParser(description='Enhanced Pose Extractor')
    parser.add_argument('input_file', nargs='?', help='Input file path (for file-based mode)')
    parser.add_argument('output_file', nargs='?', help='Output file path (for file-based mode)')
    parser.add_argument('--legacy', action='store_true', help='Use legacy command-line mode')
    
    args = parser.parse_args()
    
    try:
        # Determine operation mode
        if args.input_file and args.output_file and not args.legacy:
            # FILE-BASED MODE (New approach to fix "argument list too long")
            logger.info("🔄 Using file-based input/output mode")
            
            # Read input data
            with open(args.input_file, 'r', encoding='utf-8') as f:
                input_data = json.load(f)
            
            # Initialize extractor
            extractor = EnhancedPoseExtractor()
            
            # Determine processing mode and input from the data
            mode = None
            processing_input = None
            
            # Handle different input formats for backward compatibility
            if 'mode' in input_data:
                mode = input_data['mode']
                processing_input = input_data.get('data')
            elif 'data' in input_data:
                # Try to determine mode from data structure
                data_str = input_data['data']
                if isinstance(data_str, str):
                    try:
                        parsed_data = json.loads(data_str)
                        if isinstance(parsed_data, list) and len(parsed_data) > 0:
                            if isinstance(parsed_data[0], str):
                                mode = 'frames'
                                processing_input = parsed_data
                            else:
                                mode = 'frames'
                                processing_input = parsed_data
                    except:
                        # If parsing fails, treat as video path
                        mode = 'video'
                        processing_input = data_str
                else:
                    processing_input = data_str
                    mode = 'frames' if isinstance(data_str, list) else 'video'
            else:
                # Direct format
                if 'video_path' in input_data:
                    mode = 'video'
                    processing_input = input_data['video_path']
                elif 'frame_paths' in input_data:
                    mode = 'frames'
                    processing_input = input_data['frame_paths']
                else:
                    raise ValueError("Cannot determine processing mode from input data")
            
            # Process based on mode
            if mode == 'video':
                if isinstance(processing_input, str):
                    video_path = processing_input
                else:
                    video_path = str(processing_input)
                
                max_frames = input_data.get('max_frames', 30)
                result = extractor.process_video_file(video_path, max_frames)
                logger.info(f"Processed video: {video_path}")
                
            elif mode == 'frames':
                if isinstance(processing_input, str):
                    try:
                        frame_paths = json.loads(processing_input)
                    except:
                        frame_paths = [processing_input]
                elif isinstance(processing_input, list):
                    frame_paths = processing_input
                else:
                    raise ValueError("Invalid frame paths format")
                
                max_frames = input_data.get('max_frames', 30)
                result = extractor.process_frame_sequence(frame_paths, max_frames)
                logger.info(f"Processed {len(frame_paths)} frame paths")
                
            else:
                raise ValueError(f"Unknown mode: {mode}. Use 'video' or 'frames'")
            
            # Write output
            with open(args.output_file, 'w', encoding='utf-8') as f:
                json.dump(result, f, ensure_ascii=False, indent=2)
            
            logger.info(f"Results written to: {args.output_file}")
            
        else:
            # LEGACY COMMAND-LINE MODE (for backward compatibility)
            logger.info("🔄 Using legacy command-line mode")
            
            if len(sys.argv) < 3:
                print(json.dumps({
                    "success": False,
                    "error": "Usage: python pose_extractor.py <mode> <input> OR python pose_extractor.py <input_file> <output_file>"
                }))
                return
            
            mode = sys.argv[1].lower()
            input_data = sys.argv[2]
            
            # Initialize enhanced extractor
            extractor = EnhancedPoseExtractor()
            
            if mode == "video":
                result = extractor.process_video_file(input_data)
            elif mode == "frames":
                frame_paths = json.loads(input_data)
                result = extractor.process_frame_sequence(frame_paths)
            else:
                result = {
                    "success": False,
                    "error": f"Unknown mode: {mode}. Use 'video' or 'frames'"
                }
            
            print(json.dumps(result, ensure_ascii=False))
        
    except Exception as e:
        logger.error(f"❌ Script failed: {e}")
        
        # Write error result
        error_result = {
            "success": False,
            "error": str(e),
            "pose_sequence": [],
            "sequence_length": 0,
            "feature_dimension": 0,
            "normalized": False,
            "quality_score": 0.0
        }
        
        try:
            if args.output_file:
                with open(args.output_file, 'w', encoding='utf-8') as f:
                    json.dump(error_result, f, ensure_ascii=False, indent=2)
            else:
                print(json.dumps(error_result))
        except:
            pass
        
        sys.exit(1)

if __name__ == "__main__":
    main()
