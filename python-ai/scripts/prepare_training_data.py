import cv2
import numpy as np
import json
import os
from pose_extractor import MediaPipePoseExtractor

class TrainingDataPreparer:
    def __init__(self):
        self.extractor = MediaPipePoseExtractor()
        self.labels = {
            # Map directory names to Bangla characters
            'a': 'অ', 'aa': 'আ', 'i': 'ই', 'ii': 'ঈ', 'u': 'উ', 'uu': 'ঊ',
            'e': 'এ', 'o': 'ও', 'k': 'ক', 'kh': 'খ', 'g': 'গ', 'gh': 'ঘ',
            'ch': 'চ', 'chh': 'ছ', 'j': 'জ', 'jh': 'ঝ', 't': 'ট', 'th': 'ঠ',
            'd': 'ড', 'dh': 'ঢ', 'n': 'ণ', 'p': 'প', 'ph': 'ফ', 'b': 'ব',
            'bh': 'ভ', 'm': 'ম', 'y': 'য', 'r': 'র', 'l': 'ল', 's': 'স',
            'sh': 'শ', 'h': 'হ', 'hello': 'হ্যালো'
        }
    
    def prepare_from_extracted_frames(self, frames_root_dir, output_file):
        """Prepare training data from extracted frames directory"""
        X, y, labels = [], [], []
        
        # Iterate through video directories
        for video_dir in os.listdir(frames_root_dir):
            video_path = os.path.join(frames_root_dir, video_dir)
            if not os.path.isdir(video_path):
                continue
            
            # Get frame files
            frame_files = sorted([f for f in os.listdir(video_path) if f.endswith('.jpg')])
            frame_paths = [os.path.join(video_path, f) for f in frame_files]
            
            if len(frame_paths) < 10:  # Skip videos with too few frames
                continue
            
            # Extract pose sequence
            try:
                pose_sequence = self.extractor.extract_pose_from_video_frames(frame_paths)
                if len(pose_sequence) > 0:
                    X.append(pose_sequence)
                    
                    # Try to infer label from directory name or use default
                    label = self._infer_label_from_path(video_dir)
                    y.append(label)
                    labels.append(label)
                    
                    print(f"Processed {video_dir}: {len(pose_sequence)} frames -> {label}")
                    
            except Exception as e:
                print(f"Error processing {video_dir}: {e}")
                continue
        
        # Save training data
        training_data = {
            'X': X,
            'y': y,
            'labels': list(set(labels)),
            'num_samples': len(X),
            'num_classes': len(set(labels))
        }
        
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(training_data, f, ensure_ascii=False, indent=2)
        
        print(f"\nTraining data saved to {output_file}")
        print(f"Total samples: {len(X)}")
        print(f"Unique labels: {len(set(labels))}")
        
        return training_data
    
    def _infer_label_from_path(self, path):
        """Infer label from file path"""
        path_lower = path.lower()
        for key, bangla_char in self.labels.items():
            if key in path_lower:
                return bangla_char
        return 'অজানা'  # Unknown

def main():
    """Prepare training data from your extracted frames"""
    preparer = TrainingDataPreparer()
    

    frames_dir = "/media/sayad/Ubuntu-Data/SilentVoice_BD/uploads/frames"

    output_file = "../data/training_data.json"
    
    if os.path.exists(frames_dir):
        preparer.prepare_from_extracted_frames(frames_dir, output_file)
    else:
        print(f"Frames directory not found: {frames_dir}")
        print("Please update the frames_dir path in the script")

if __name__ == "__main__":
    main()
