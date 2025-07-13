import cv2
import numpy as np
import json
import os

from pose_extractor import OptimizedMediaPipePoseExtractor as MediaPipePoseExtractor


class TrainingDataPreparer:
    def __init__(self):
        self.extractor = MediaPipePoseExtractor()

        # Full mapping for BDSLW60 (folder name -> Bangla word)
        self.labels = {
            'aam': 'আম',
            'aaple': 'আপেল',
            'ac': 'এসি',
            'aids': 'এইডস',
            'alu': 'আলু',
            'anaros': 'আনারস',
            'angur': 'আঙুর',
            'apartment': 'অ্যাপার্টমেন্ট',
            'attio': 'আত্তিও',
            'audio cassette': 'অডিও ক্যাসেট',
            'ayna': 'আয়না',
            'baandej': 'ব্যান্ডেজ',
            'baat': 'বাত',
            'baba': 'বাবা',
            'balti': 'বালতি',
            'balu': 'বালু',
            'bhai': 'ভাই',
            'biscuts': 'বিস্কুট',
            'bon': 'বোন',
            'boroi': 'বড়ই',
            'bottam': 'বোতাম',
            'bou': 'বউ',
            'cake': 'কেক',
            'capsule': 'ক্যাপসুল',
            'cha': 'চা',
            'chacha': 'চাচা',
            'chachi': 'চাচি',
            'chadar': 'চাদর',
            'chal': 'চাল',
            'chikissha': 'চিকিৎসা',
            'chini': 'চিনি',
            'chips': 'চিপস',
            'chiruni': 'চিরুনি',
            'chocolate': 'চকলেট',
            'chokh utha': 'চোখ উঠা',
            'chosma': 'চশমা',
            'churi': 'চুরি',
            'clip': 'ক্লিপ',
            'cream': 'ক্রিম',
            'dada': 'দাদা',
            'dadi': 'দাদি',
            'daeitto': 'দায়িত্ব',
            'dal': 'ডাল',
            'debor': 'দেবর',
            'denadar': 'দেনাদার',
            'dengue': 'ডেঙ্গু',
            'doctor': 'ডাক্তার',
            'dongson': 'দংশন',
            'dulavai': 'দুলাভাই',
            'durbol': 'দুর্বল',
            'jomoj': 'জমজ',
            'juta': 'জুতা',
            'konna': 'কন্যা',
            'maa': 'মা',
            'tattha': 'তত্ত্ব',
            'toothpaste': 'টুথপেস্ট',
            'tshirt': 'টিশার্ট',
            'tubelight': 'টিউবলাইট',
            'tupi': 'টুপি',
            'tv': 'টিভি',
        }

    def normalize_features(self, X):
        """Apply standardization normalization to features"""
        print("Applying feature normalization...")
        
        # Convert to numpy array for easier manipulation
        X_array = np.array(X)
        print(f"Original data shape: {X_array.shape}")
        
        # Flatten all frames to compute global statistics
        X_flat = X_array.reshape(-1, X_array.shape[-1])  # (total_frames, features)
        print(f"Flattened shape for stats: {X_flat.shape}")
        
        # Compute mean and std for each feature
        feature_means = X_flat.mean(axis=0)
        feature_stds = X_flat.std(axis=0) + 1e-8  # Add small value to avoid division by zero
        
        print(f"Feature means range: {feature_means.min():.6f} to {feature_means.max():.6f}")
        print(f"Feature stds range: {feature_stds.min():.6f} to {feature_stds.max():.6f}")
        
        # Apply normalization to all sequences
        X_normalized = []
        for sequence in X:
            sequence = np.array(sequence)
            sequence_normalized = (sequence - feature_means) / feature_stds
            X_normalized.append(sequence_normalized.tolist())
        
        # Save normalization parameters
        normalization_params = {
            'feature_means': feature_means.tolist(),
            'feature_stds': feature_stds.tolist(),
            'feature_dim': len(feature_means)
        }
        
        # Save to multiple locations for easy access
        with open('../data/normalization_params.json', 'w') as f:
            json.dump(normalization_params, f, indent=2)
        
        # Also save as numpy arrays for faster loading
        np.save('../data/feature_means.npy', feature_means)
        np.save('../data/feature_stds.npy', feature_stds)
        
        print("✅ Normalization parameters saved to:")
        print("  - ../data/normalization_params.json")
        print("  - ../data/feature_means.npy")
        print("  - ../data/feature_stds.npy")
        
        # Verify normalization
        X_normalized_array = np.array(X_normalized)
        X_normalized_flat = X_normalized_array.reshape(-1, X_normalized_array.shape[-1])
        
        print(f"After normalization:")
        print(f"  Mean: {X_normalized_flat.mean():.6f} (should be ~0)")
        print(f"  Std: {X_normalized_flat.std():.6f} (should be ~1)")
        print(f"  Range: {X_normalized_flat.min():.6f} to {X_normalized_flat.max():.6f}")
        
        return X_normalized

    def prepare_from_extracted_frames(self, frames_root_dir, output_file):
        X, y, labels = [], [], []
        label_counts = {}

        for video_dir in os.listdir(frames_root_dir):
            video_path = os.path.join(frames_root_dir, video_dir)
            if not os.path.isdir(video_path):
                continue

            # Get frame files
            frame_files = sorted(
                [f for f in os.listdir(video_path) if f.endswith('.jpg')]
            )
            frame_paths = [os.path.join(video_path, f) for f in frame_files]

            if len(frame_paths) < 10:
                continue

            # Extract pose sequence
            try:
                pose_sequence = self.extractor.extract_pose_from_video_frames(
                    frame_paths
                )
                if len(pose_sequence) > 0:
                    # Improved label extraction: use folder name before first underscore
                    main_word = video_dir.split('_')[0].lower()
                    label = self.labels.get(main_word, 'অজানা')
                    X.append(pose_sequence)
                    y.append(label)
                    labels.append(label)
                    label_counts[label] = label_counts.get(label, 0) + 1
                    print(
                        f"Processed {video_dir}: {len(pose_sequence)} frames -> {label}"
                    )
            except Exception as e:
                print(f"Error processing {video_dir}: {e}")
                continue

        # *** APPLY NORMALIZATION HERE ***
        print(f"\nApplying normalization to {len(X)} sequences...")
        X_normalized = self.normalize_features(X)

        # Save training data with normalized features
        training_data = {
            'X': X_normalized,
            'y': y,
            'labels': list(set(labels)),
            'num_samples': len(X_normalized),
            'num_classes': len(set(labels)),
            'normalized': True,  # Flag to indicate data is normalized
            'feature_dim': 288
        }

        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(training_data, f, ensure_ascii=False, indent=2)

        print(f"\n✅ Normalized training data saved to {output_file}")
        print(f"Total samples: {len(X_normalized)}")
        print(f"Unique labels: {len(set(labels))}")
        print("Sample count per label:")
        for label, count in label_counts.items():
            print(f" {label}: {count}")

        return training_data


def main():
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
