#!/usr/bin/env python3
"""
Pose extraction utility for SilentVoice_BD.

Supports two invocation modes (command-line):

  1) VIDEO MODE  (current CLI contract)
     python pose_extractor.py video /path/to/video.mp4

  2) FRAMES MODE (used by Spring backend via PythonAIIntegrationService)
     python pose_extractor.py frames '["/path/f1.jpg","/path/f2.jpg",...]'

In both cases we return JSON to stdout in the form:

{
  "success": true,
  "pose_sequence": [[...288 floats...], ... up to max_frames ...],
  "sequence_length": <int>,
  "feature_dimension": 288,
  "normalized": true|false,
  "error": <optional string>
}
"""

import cv2
import numpy as np
import mediapipe as mp
import json
import sys
import os
import gc
from typing import List, Tuple


class OptimizedMediaPipePoseExtractor:
    def __init__(self, skip_normalization_loading=False,
                 min_detection_confidence=0.5,
                 min_tracking_confidence=0.5,
                 model_complexity=0):
        self.mp_holistic = mp.solutions.holistic
        self.holistic = self.mp_holistic.Holistic(
            min_detection_confidence=min_detection_confidence,
            min_tracking_confidence=min_tracking_confidence,
            model_complexity=model_complexity
        )
        self.feature_means = None
        self.feature_stds = None
        self.normalization_loaded = False
        if not skip_normalization_loading:
            self.load_normalization_params()

    def load_normalization_params(self):
        """Load normalization parameters with ABSOLUTE paths and comprehensive debugging"""
        try:
            # CRITICAL FIX: Use absolute paths instead of relative paths
            script_dir = os.path.dirname(os.path.abspath(__file__))
            data_dir = os.path.join(script_dir, '..', 'data')
            
            means_path = os.path.join(data_dir, 'feature_means.npy')
            stds_path = os.path.join(data_dir, 'feature_stds.npy')
            
            # ENHANCED DEBUGGING: Show detailed path information
            print(f"🔍 NORMALIZATION DEBUG INFO:", file=sys.stderr)
            print(f"   Script file: {__file__}", file=sys.stderr)
            print(f"   Script directory: {script_dir}", file=sys.stderr)
            print(f"   Data directory: {data_dir}", file=sys.stderr)
            print(f"   Current working directory: {os.getcwd()}", file=sys.stderr)
            print(f"   Means path: {means_path}", file=sys.stderr)
            print(f"   Stds path: {stds_path}", file=sys.stderr)
            print(f"   Means file exists: {os.path.exists(means_path)}", file=sys.stderr)
            print(f"   Stds file exists: {os.path.exists(stds_path)}", file=sys.stderr)
            
            if os.path.exists(means_path) and os.path.exists(stds_path):
                self.feature_means = np.load(means_path)
                self.feature_stds = np.load(stds_path)
                self.normalization_loaded = True
                
                # DETAILED DEBUGGING: Show normalization parameter info
                print(f"✅ NORMALIZATION LOADED SUCCESSFULLY:", file=sys.stderr)
                print(f"   Means array shape: {self.feature_means.shape}", file=sys.stderr)
                print(f"   Stds array shape: {self.feature_stds.shape}", file=sys.stderr)
                print(f"   Means sample (first 5): {self.feature_means[:5]}", file=sys.stderr)
                print(f"   Stds sample (first 5): {self.feature_stds[:5]}", file=sys.stderr)
                print(f"   Means range: [{np.min(self.feature_means):.4f}, {np.max(self.feature_means):.4f}]", file=sys.stderr)
                print(f"   Stds range: [{np.min(self.feature_stds):.4f}, {np.max(self.feature_stds):.4f}]", file=sys.stderr)
                return
            
            # Try JSON fallback with absolute path
            json_path = os.path.join(data_dir, 'normalization_params.json')
            print(f"   Trying JSON fallback: {json_path}", file=sys.stderr)
            print(f"   JSON file exists: {os.path.exists(json_path)}", file=sys.stderr)
            
            if os.path.exists(json_path):
                with open(json_path, 'r') as f:
                    params = json.load(f)
                self.feature_means = np.array(params['feature_means'])
                self.feature_stds = np.array(params['feature_stds'])
                self.normalization_loaded = True
                
                print(f"✅ NORMALIZATION LOADED FROM JSON:", file=sys.stderr)
                print(f"   Loaded from: {json_path}", file=sys.stderr)
                print(f"   Means length: {len(self.feature_means)}", file=sys.stderr)
                print(f"   Stds length: {len(self.feature_stds)}", file=sys.stderr)
                return
            
            # If we reach here, no normalization files found
            print(f"❌ NO NORMALIZATION FILES FOUND:", file=sys.stderr)
            print(f"   Checked paths:", file=sys.stderr)
            print(f"     - {means_path}", file=sys.stderr)
            print(f"     - {stds_path}", file=sys.stderr)
            print(f"     - {json_path}", file=sys.stderr)
            print(f"⚠️ Will use RAW features without normalization", file=sys.stderr)
            self.normalization_loaded = False
            
        except Exception as e:
            print(f"❌ CRITICAL ERROR loading normalization params: {e}", file=sys.stderr)
            print(f"   Exception type: {type(e).__name__}", file=sys.stderr)
            import traceback
            print(f"   Traceback: {traceback.format_exc()}", file=sys.stderr)
            self.normalization_loaded = False

    def normalize_sequence(self, sequence, apply_normalization=True) -> Tuple[List, bool]:
        """Normalize feature sequence with enhanced debugging and status return"""
        if not apply_normalization:
            print(f"⚠️ Normalization DISABLED by parameter", file=sys.stderr)
            return sequence, False
            
        if self.feature_means is None or self.feature_stds is None or not self.normalization_loaded:
            print(f"❌ NORMALIZATION SKIPPED: Parameters not loaded", file=sys.stderr)
            print(f"   feature_means is None: {self.feature_means is None}", file=sys.stderr)
            print(f"   feature_stds is None: {self.feature_stds is None}", file=sys.stderr)
            print(f"   normalization_loaded: {self.normalization_loaded}", file=sys.stderr)
            return sequence, False
        
        if len(sequence) == 0:
            print(f"⚠️ Empty sequence provided for normalization", file=sys.stderr)
            return sequence, False
        
        # Enhanced debugging for normalization process
        seq = np.array(sequence, dtype=np.float32)
        print(f"🔧 APPLYING NORMALIZATION:", file=sys.stderr)
        print(f"   Input sequence shape: {seq.shape}", file=sys.stderr)
        print(f"   Feature dimension: {seq.shape[1] if len(seq.shape) > 1 else 'N/A'}", file=sys.stderr)
        print(f"   Normalization params dimension: {len(self.feature_means)}", file=sys.stderr)
        
        # Check for dimension mismatch
        if len(seq.shape) > 1 and seq.shape[1] != len(self.feature_means):
            print(f"❌ DIMENSION MISMATCH:", file=sys.stderr)
            print(f"   Sequence features: {seq.shape[1]}", file=sys.stderr)
            print(f"   Normalization params: {len(self.feature_means)}", file=sys.stderr)
            return sequence, False
        
        # Apply normalization
        try:
            normalized = ((seq - self.feature_means) / self.feature_stds)
            
            # Debug the normalization effect
            print(f"✅ NORMALIZATION APPLIED SUCCESSFULLY:", file=sys.stderr)
            print(f"   Before - mean: {np.mean(seq):.4f}, std: {np.std(seq):.4f}", file=sys.stderr)
            print(f"   After - mean: {np.mean(normalized):.4f}, std: {np.std(normalized):.4f}", file=sys.stderr)
            print(f"   Sample values before: {seq.flatten()[:5]}", file=sys.stderr)
            print(f"   Sample values after: {normalized.flatten()[:5]}", file=sys.stderr)
            
            return normalized.tolist(), True
            
        except Exception as e:
            print(f"❌ ERROR during normalization: {e}", file=sys.stderr)
            return sequence, False
    
    def extract_keypoints(self, results) -> np.ndarray:
        """Extract pose keypoints EXACTLY as used during training - 288 features with debugging"""
        try:
            # Extract left hand landmarks (21 * 3 = 63 features)
            lh = (np.array([[lm.x, lm.y, lm.z] for lm in results.left_hand_landmarks.landmark]).flatten()
                  if results.left_hand_landmarks else np.zeros(21 * 3, dtype=np.float32))
            
            # Extract right hand landmarks (21 * 3 = 63 features)
            rh = (np.array([[lm.x, lm.y, lm.z] for lm in results.right_hand_landmarks.landmark]).flatten()
                  if results.right_hand_landmarks else np.zeros(21 * 3, dtype=np.float32))
            
            # CRITICAL: Use pose WITH visibility (33 * 4 = 132 features) - MUST match training
            pose = (np.array([[lm.x, lm.y, lm.z, lm.visibility] for lm in results.pose_landmarks.landmark]).flatten()
                    if results.pose_landmarks else np.zeros(33 * 4, dtype=np.float32))
            
            # Extract face landmarks (10 * 3 = 30 features)
            face = (np.array([[lm.x, lm.y, lm.z] for lm in results.face_landmarks.landmark[:10]]).flatten()
                    if (results.face_landmarks and len(results.face_landmarks.landmark) >= 10)
                    else np.zeros(10 * 3, dtype=np.float32))
            
            # Enhanced debugging for feature extraction
            print(f"🔍 FEATURE EXTRACTION DEBUG:", file=sys.stderr)
            print(f"   Left hand detected: {results.left_hand_landmarks is not None}, features: {len(lh)}", file=sys.stderr)
            print(f"   Right hand detected: {results.right_hand_landmarks is not None}, features: {len(rh)}", file=sys.stderr)
            print(f"   Pose detected: {results.pose_landmarks is not None}, features: {len(pose)}", file=sys.stderr)
            print(f"   Face detected: {results.face_landmarks is not None}, features: {len(face)}", file=sys.stderr)
            
            # Total: 63 + 63 + 132 + 30 = 288 features (matches training exactly)
            features = np.concatenate([lh, rh, pose, face]).astype(np.float32)
            
            print(f"   Total features before validation: {len(features)}", file=sys.stderr)
            print(f"   Feature value range: [{np.min(features):.4f}, {np.max(features):.4f}]", file=sys.stderr)
            print(f"   Non-zero features: {np.count_nonzero(features)}/{len(features)} ({100*np.count_nonzero(features)/len(features):.1f}%)", file=sys.stderr)
            
            # Ensure exactly 288 features
            if len(features) != 288:
                print(f"⚠️ FEATURE DIMENSION MISMATCH: got {len(features)}, expected 288", file=sys.stderr)
                if len(features) > 288:
                    features = features[:288]
                    print(f"   Truncated to 288 features", file=sys.stderr)
                else:
                    padding_size = 288 - len(features)
                    features = np.pad(features, (0, padding_size), 'constant', constant_values=0.0)
                    print(f"   Padded with {padding_size} zeros", file=sys.stderr)
            
            print(f"✅ Final feature vector: {len(features)} features", file=sys.stderr)
            return features
            
        except Exception as e:
            print(f"❌ CRITICAL ERROR in feature extraction: {e}", file=sys.stderr)
            import traceback
            print(f"   Traceback: {traceback.format_exc()}", file=sys.stderr)
            return np.zeros(288, dtype=np.float32)

    def extract_pose_from_video_frames(self, frame_paths: List[str], max_frames: int = 30, 
                                       apply_normalization: bool = True) -> Tuple[List[List[float]], bool]:
        """Extract pose from pre-extracted video frames - TRAINING COMPATIBLE VERSION"""
        seq = []
        count = 0
        
        print(f"🎬 PROCESSING VIDEO FRAMES:", file=sys.stderr)
        print(f"   Total frames to process: {len(frame_paths)}", file=sys.stderr)
        print(f"   Max frames limit: {max_frames}", file=sys.stderr)
        print(f"   Apply normalization: {apply_normalization}", file=sys.stderr)
        
        try:
            for fp in frame_paths[:max_frames]:
                print(f"   Processing frame {count + 1}: {fp}", file=sys.stderr)
                img_bgr = cv2.imread(fp)
                if img_bgr is None:
                    print(f"   ❌ Could not read frame: {fp}", file=sys.stderr)
                    continue
                    
                # Use same preprocessing as training
                h, w = img_bgr.shape[:2]
                print(f"   Original size: {w}x{h}", file=sys.stderr)
                if w > 640:
                    scale = 640.0 / w
                    img_bgr = cv2.resize(img_bgr, (int(w*scale), int(h*scale)))
                    print(f"   Resized to: {int(w*scale)}x{int(h*scale)}", file=sys.stderr)
                    
                img = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)
                img.flags.writeable = False
                results = self.holistic.process(img)
                kp = self.extract_keypoints(results)
                seq.append(kp.tolist())
                count += 1
                
        except Exception as e:
            print(f"❌ ERROR processing frames: {e}", file=sys.stderr)
        finally:
            gc.collect()
        
        print(f"✅ Extracted {len(seq)} frames successfully", file=sys.stderr)
        return self.normalize_sequence(seq, apply_normalization)

    def extract_pose_from_video_file(self, video_path: str, max_frames: int = 30,
                                   apply_normalization: bool = True) -> Tuple[List[List[float]], bool]:
        """Extract pose features from video file with normalization status"""
        cap = cv2.VideoCapture(video_path)
        seq = []
        count = 0
        
        if not cap.isOpened():
            print(f"❌ Could not open video: {video_path}", file=sys.stderr)
            return [], False
        
        print(f"🎬 PROCESSING VIDEO FILE: {video_path}", file=sys.stderr)
        
        try:
            while cap.isOpened() and count < max_frames:
                ret, frame = cap.read()
                if not ret:
                    break

                # Use same preprocessing as training
                h, w = frame.shape[:2]
                if w > 640:
                    scale = 640.0 / w
                    frame = cv2.resize(frame, (int(w * scale), int(h * scale)))

                # Convert BGR to RGB for MediaPipe
                img = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                img.flags.writeable = False
                
                # Process frame with MediaPipe
                results = self.holistic.process(img)
                kp = self.extract_keypoints(results)
                seq.append(kp.tolist())
                count += 1
                
        except Exception as e:
            print(f"❌ Error processing video: {e}", file=sys.stderr)
        finally:
            cap.release()
            gc.collect()
        
        print(f"✅ Extracted {len(seq)} frames from video", file=sys.stderr)
        return self.normalize_sequence(seq, apply_normalization)

    def extract_pose_from_image_files(self, frame_paths: List[str], max_frames: int = 30,
                                    apply_normalization: bool = True) -> Tuple[List[List[float]], bool]:
        """Process individual pre-extracted image frames with normalization status"""
        seq = []
        count = 0
        
        try:
            for fp in frame_paths[:max_frames]:
                img_bgr = cv2.imread(fp)
                if img_bgr is None:
                    print(f"⚠️ Could not read frame: {fp}", file=sys.stderr)
                    continue

                # Use same preprocessing as training
                h, w = img_bgr.shape[:2]
                if w > 640:
                    scale = 640.0 / w
                    img_bgr = cv2.resize(img_bgr, (int(w * scale), int(h * scale)))

                # Convert BGR to RGB for MediaPipe
                img = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)
                img.flags.writeable = False
                
                # Process frame with MediaPipe
                results = self.holistic.process(img)
                kp = self.extract_keypoints(results)
                seq.append(kp.tolist())
                count += 1
                
                if count >= max_frames:
                    break
                    
        except Exception as e:
            print(f"❌ Error processing frames: {e}", file=sys.stderr)
        finally:
            gc.collect()
        
        print(f"✅ Extracted {len(seq)} frames from image files", file=sys.stderr)
        return self.normalize_sequence(seq, apply_normalization)


# -----------------------------
# Simple wrapper (legacy)
# -----------------------------
def extract_pose_landmarks(video_path, max_frames=30):
    """Legacy wrapper function for backward compatibility"""
    extractor = OptimizedMediaPipePoseExtractor()
    seq, _ = extractor.extract_pose_from_video_file(video_path, max_frames, apply_normalization=True)
    return np.array(seq)


# -----------------------------
# Command-line entry point
# -----------------------------
def main():
    """Main entry point with corrected normalization status reporting"""
    if len(sys.argv) < 3:
        print(json.dumps({
            "success": False,
            "error": "Usage: python pose_extractor.py <video|frames> <path_or_json>"
        }))
        return

    mode = sys.argv[1].strip().lower()
    payload = sys.argv[2]

    extractor = OptimizedMediaPipePoseExtractor()

    try:
        if mode == "video":
            video_path = payload
            if not os.path.exists(video_path):
                print(json.dumps({
                    "success": False, 
                    "error": f"Video not found: {video_path}"
                }))
                return
            
            seq, is_normalized = extractor.extract_pose_from_video_file(
                video_path, max_frames=30, apply_normalization=True
            )
            # CRITICAL: Training-compatible output format with normalization status
            print(json.dumps({
                "sequence": seq,
                "normalized": is_normalized
            }))

        elif mode == "frames":
            # payload is a JSON list of frame image paths
            try:
                frame_paths = json.loads(payload)
            except json.JSONDecodeError as e:
                print(json.dumps({
                    "success": False, 
                    "error": f"Invalid frame paths JSON: {e}"
                }))
                return

            if not isinstance(frame_paths, list) or not frame_paths:
                print(json.dumps({
                    "success": False, 
                    "error": "Empty frame list"
                }))
                return

            seq, is_normalized = extractor.extract_pose_from_image_files(
                frame_paths, max_frames=30, apply_normalization=True
            )

            # Prepare output with current format for Spring backend
            feature_dim = len(seq[0]) if seq else 0
            success = bool(seq)
            
            out = {
                "success": success,
                "pose_sequence": seq,
                "sequence_length": len(seq),
                "feature_dimension": feature_dim,
                "normalized": is_normalized
            }
            
            if not success:
                out["error"] = "Failed to extract pose sequence"
            
            print(json.dumps(out, ensure_ascii=False))

        else:
            print(json.dumps({
                "success": False, 
                "error": f"Unknown mode '{mode}'. Use 'video' or 'frames'"
            }))
            return

    except Exception as e:
        print(json.dumps({
            "success": False, 
            "error": f"Unexpected error: {str(e)}"
        }))


if __name__ == "__main__":
    main()
