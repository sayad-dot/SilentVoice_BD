🚀 Project Overview
SilentVoice_BD is an open-source, full-stack platform for recognizing Bangla sign language from video. It combines a modern Java Spring Boot backend, Python AI (MediaPipe + LSTM), and a React frontend to enable video upload, automatic frame extraction, pose estimation, and accurate sign language translation. The project aims to make Bangla sign language accessible for education, communication, and research.

✨ Features
Video Upload & Management: Upload, stream, and manage sign language videos.

Automatic Frame Extraction: Efficiently extracts frames from uploaded videos for analysis.

Pose Estimation: Uses MediaPipe to extract hand and body landmarks from video frames.

AI Sign Recognition: LSTM-based model trained on Bangla sign datasets (e.g., BDSLW60) for high-accuracy gesture recognition.

RESTful API: Endpoints for video management, AI status, and prediction results.

Modular Design: Python AI pipeline and Java backend are loosely coupled for easy updates.

Async Processing: Handles large video datasets and AI jobs efficiently.

Extensible: Easily add new sign classes, datasets, or AI models.

📦 Installation

1. Clone the Repository
   bash
   git clone https://github.com/yourusername/SilentVoice_BD.git
   cd SilentVoice_BD
2. Python Environment Setup
   bash
   python3 -m venv ai-env
   source ai-env/bin/activate
   pip install -r python-ai/requirements.txt
3. Java Backend Setup
   Ensure you have Java 21+ and Maven installed.

Install PostgreSQL and create a database for the project.

Configure src/main/resources/application.properties with your DB credentials.

bash
mvn clean install
mvn spring-boot:run 4. Frontend Setup (if included)
bash
cd frontend
npm install
npm start
📂 Data Sources
BDSLW60: Bangladeshi Sign Language Words 60 Dataset
60-class Bangla sign language video dataset.

Custom Videos: Upload your own sign language videos for training and testing.

Data Preprocessing:

All videos are processed for frame extraction.

Frames are passed through MediaPipe for pose landmark extraction.

Labeling is based on folder structure or video metadata.

🗂️ Code Structure
text
SilentVoice_BD/
├── src/main/java/com/example/silentvoice_bd/
│ ├── controller/ # REST controllers (video, AI)
│ ├── service/ # Video and processing services
│ ├── processing/ # Frame extraction logic
│ ├── ai/ # AI integration (services, models, DTOs)
│ └── repository/ # JPA repositories
├── python-ai/
│ ├── scripts/ # Python scripts for pose extraction, prediction
│ ├── models/ # LSTM and related model code
│ ├── data/ # Training data, temp files
│ └── trained_models/ # Saved AI models
├── uploads/frames/ # Extracted video frames (ignored by git)
├── uploads/videos/ # Uploaded video files (ignored by git)
├── uploads/thumbnails/ # Video thumbnails (ignored by git)
├── dataset/ # External datasets (ignored by git)
├── .gitignore
├── pom.xml
├── requirements.txt
└── README.md
🧑‍💻 Example Usage
Upload a Video
bash
curl -F "file=@path/to/video.mp4" http://localhost:8088/api/videos/upload
Check AI Status
bash
curl http://localhost:8088/api/ai/status
Get AI Predictions
bash
curl http://localhost:8088/api/ai/predictions/{videoId}
Manual AI Processing
bash
curl -X POST http://localhost:8088/api/ai/predict/{videoId}
📊 Results & Evaluation
Accuracy: Achieves 90–98% accuracy for Bangla sign recognition using the BDSLW60 dataset and LSTM model.

Performance: Handles large datasets and concurrent video uploads with async processing.

Extensibility: Easily supports new signs, datasets, or improved AI models.

🔮 Future Work
Real-time sign language translation for live video streams

Support for additional sign languages

Mobile app integration

Advanced AI models (Transformers, CNN-LSTM hybrids)

Community dataset contributions and crowdsourced labeling

🙏 Acknowledgments
BDSLW60 Dataset

MediaPipe

TensorFlow

All open-source contributors and the Bangla sign language community

📜 License
This project is licensed under the MIT License.
See LICENSE for details.

👤 Author
SilentVoice_BD
Developed by -Sayad Ibna Azad
Contact: sayadkhan0555@gmail.com

If you use this project for research, education, or development, please cite or star the repository! Contributions and issues are welcome.
