import os
import cv2

# Path to your dataset
DATASET_DIR = '/media/sayad/Ubuntu-Data/SilentVoice_BD/dataset/bdslw60/archive'
# Path where you want to save extracted frames
FRAMES_OUTPUT_DIR = '/media/sayad/Ubuntu-Data/SilentVoice_BD/uploads/frames'

# How many frames to extract per video (adjust as needed)
MAX_FRAMES_PER_VIDEO = 30

def extract_frames_from_video(video_path, output_dir, max_frames=30):
    os.makedirs(output_dir, exist_ok=True)
    cap = cv2.VideoCapture(video_path)
    frame_count = 0
    while cap.isOpened() and frame_count < max_frames:
        ret, frame = cap.read()
        if not ret:
            break
        frame_filename = os.path.join(output_dir, f'frame_{frame_count:04d}.jpg')
        cv2.imwrite(frame_filename, frame)
        frame_count += 1
    cap.release()

def main():
    for label in os.listdir(DATASET_DIR):
        label_dir = os.path.join(DATASET_DIR, label)
        if not os.path.isdir(label_dir):
            continue
        for video_file in os.listdir(label_dir):
            if not video_file.lower().endswith(('.mp4', '.avi', '.mov', '.mkv')):
                continue
            video_path = os.path.join(label_dir, video_file)
            video_id = os.path.splitext(video_file)[0]
            output_dir = os.path.join(FRAMES_OUTPUT_DIR, f'{label}_{video_id}')
            print(f'Extracting frames from {video_path} to {output_dir}')
            extract_frames_from_video(video_path, output_dir, MAX_FRAMES_PER_VIDEO)
    print('✅ Batch frame extraction complete!')

if __name__ == '__main__':
    main()
