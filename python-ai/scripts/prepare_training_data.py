import cv2
import numpy as np
import json
import os
from pose_extractor import MediaPipePoseExtractor


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

        # Save training data
        training_data = {
            'X': X,
            'y': y,
            'labels': list(set(labels)),
            'num_samples': len(X),
            'num_classes': len(set(labels)),
        }

        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(training_data, f, ensure_ascii=False, indent=2)

        print(f"\nTraining data saved to {output_file}")
        print(f"Total samples: {len(X)}")
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
