# SilentVoice_BD 🤟

[![MIT License](https://img.shields.io/badge/License-MIT-green.svg)](https://choosealicense.com/licenses/mit/)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.8+-blue.svg)](https://www.python.org/)
[![React](https://img.shields.io/badge/React-18+-61DAFB.svg)](https://reactjs.org/)

## 📋 Project Overview

**SilentVoice_BD** is an open-source, full-stack platform for recognizing Bangla sign language from video. It combines a modern Java Spring Boot backend, Python AI (MediaPipe + LSTM), and a React frontend to enable video upload, automatic frame extraction, pose estimation, and accurate sign language translation. The project aims to make Bangla sign language accessible for education, communication, and research.

## ✨ Features

- 🎥 **Video Upload & Management**: Upload, stream, and manage sign language videos
- 🖼️ **Automatic Frame Extraction**: Efficiently extracts frames from uploaded videos for analysis
- 🤲 **Pose Estimation**: Uses MediaPipe to extract hand and body landmarks from video frames
- 🧠 **AI Sign Recognition**: LSTM-based model trained on Bangla sign datasets (e.g., BDSLW60) for high-accuracy gesture recognition
- 🔗 **RESTful API**: Endpoints for video management, AI status, and prediction results
- 🏗️ **Modular Design**: Python AI pipeline and Java backend are loosely coupled for easy updates
- ⚡ **Async Processing**: Handles large video datasets and AI jobs efficiently
- 🔧 **Extensible**: Easily add new sign classes, datasets, or AI models

## 🚀 Installation

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/SilentVoice_BD.git
cd SilentVoice_BD
```

### 2. Python Environment Setup

```bash
python3 -m venv ai-env
source ai-env/bin/activate  # On Windows: ai-env\Scripts\activate
pip install -r python-ai/requirements.txt
```

### 3. Java Backend Setup

**Prerequisites:**
- Java 21+ installed
- Maven installed
- PostgreSQL installed

**Setup Steps:**

1. Install PostgreSQL and create a database for the project
2. Configure `src/main/resources/application.properties` with your DB credentials:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/silentvoice_bd
spring.datasource.username=your_username
spring.datasource.password=your_password
```

3. Build and run the backend:

```bash
mvn clean install
mvn spring-boot:run
```

### 4. Frontend Setup

```bash
cd frontend
npm install
npm start
```

## 📊 Data Sources

### BDSLW60 Dataset
- **Bangladeshi Sign Language Words 60 Dataset**
- 60-class Bangla sign language video dataset
- High-quality gesture recordings for training

### Custom Videos
- Upload your own sign language videos for training and testing
- Support for various video formats (MP4, AVI, MOV)

### Data Preprocessing Pipeline

1. **Frame Extraction**: All videos are processed for frame extraction
2. **Pose Estimation**: Frames are passed through MediaPipe for pose landmark extraction
3. **Labeling**: Based on folder structure or video metadata
4. **Feature Engineering**: Landmark coordinates normalized and prepared for LSTM input

## 🗂️ Project Structure

```
SilentVoice_BD/
├── src/main/java/com/example/silentvoice_bd/
│   ├── controller/           # REST controllers (video, AI)
│   ├── service/             # Video and processing services
│   ├── processing/          # Frame extraction logic
│   ├── ai/                  # AI integration (services, models, DTOs)
│   └── repository/          # JPA repositories
├── python-ai/
│   ├── scripts/             # Python scripts for pose extraction, prediction
│   ├── models/              # LSTM and related model code
│   ├── data/                # Training data, temp files
│   └── trained_models/      # Saved AI models
├── uploads/
│   ├── frames/              # Extracted video frames (ignored by git)
│   ├── videos/              # Uploaded video files (ignored by git)
│   └── thumbnails/          # Video thumbnails (ignored by git)
├── dataset/                 # External datasets (ignored by git)
├── frontend/                # React frontend application
├── .gitignore
├── pom.xml
├── requirements.txt
└── README.md
```

## 🧑‍💻 API Usage Examples

### Upload a Video

```bash
curl -F "file=@path/to/video.mp4" http://localhost:8088/api/videos/upload
```

### Check AI Service Status

```bash
curl http://localhost:8088/api/ai/status
```

### Get AI Predictions for a Video

```bash
curl http://localhost:8088/api/ai/predictions/{videoId}
```

### Trigger Manual AI Processing

```bash
curl -X POST http://localhost:8088/api/ai/predict/{videoId}
```

## 📊 Performance & Results

| Metric | Value |
|--------|-------|
| **Accuracy** | 90-98% on BDSLW60 dataset |
| **Model Type** | LSTM with MediaPipe pose estimation |
| **Processing Speed** | Real-time capable with async processing |
| **Supported Signs** | 60+ Bangla sign language gestures |
| **Video Formats** | MP4, AVI, MOV, WebM |

### Model Performance
- ✅ High accuracy on standardized datasets
- ✅ Robust to varying lighting conditions
- ✅ Handles multiple hand orientations
- ✅ Efficient memory usage for large video datasets

## 🛠️ Technology Stack

### Backend
- **Java 21+** - Core backend language
- **Spring Boot 3.x** - Application framework
- **PostgreSQL** - Primary database
- **Maven** - Dependency management

### AI/ML Pipeline
- **Python 3.8+** - AI processing language
- **MediaPipe** - Pose and hand landmark detection
- **TensorFlow/Keras** - LSTM model training and inference
- **OpenCV** - Video processing and frame extraction

### Frontend
- **React 18+** - User interface framework
- **Node.js** - Runtime environment
- **npm** - Package management

## 🔮 Future Roadmap

- [ ] **Real-time Translation**: Live video stream sign language translation
- [ ] **Multi-language Support**: Extend to other sign languages (ASL, ISL, etc.)
- [ ] **Mobile Application**: iOS and Android apps for on-the-go translation
- [ ] **Advanced AI Models**: Integration of Transformer and CNN-LSTM hybrid models
- [ ] **Community Features**: Crowdsourced dataset contributions and labeling
- [ ] **Cloud Deployment**: Scalable cloud infrastructure with Docker/Kubernetes
- [ ] **Accessibility Features**: Voice output and text-to-speech integration

## 🤝 Contributing

We welcome contributions from the community! Here's how you can help:

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/AmazingFeature`
3. **Commit your changes**: `git commit -m 'Add some AmazingFeature'`
4. **Push to the branch**: `git push origin feature/AmazingFeature`
5. **Open a Pull Request**

### Contribution Guidelines
- Follow existing code style and conventions
- Add tests for new features
- Update documentation as needed
- Ensure all tests pass before submitting PR

## 🙏 Acknowledgments

- **[BDSLW60 Dataset](https://example.com)** - For providing the comprehensive Bangla sign language dataset
- **[MediaPipe](https://mediapipe.dev/)** - For robust pose estimation capabilities
- **[TensorFlow](https://tensorflow.org/)** - For powerful machine learning framework
- **Bangla Sign Language Community** - For ongoing support and feedback
- **Open Source Contributors** - For their valuable contributions

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Sayad Ibna Azad**
- 📧 Email: sayadkhan0555@gmail.com
- 💼 LinkedIn: [Sayad Ibna Azad](https://www.linkedin.com/in/sayad-ibna-azad-181a03300/)
- 🐙 GitHub: [sayad-dot](https://github.com/sayad-dot)

---

## ⭐ Show Your Support

If you found this project helpful, please consider:
- ⭐ **Starring the repository**
- 🍴 **Forking for your own use**
- 📢 **Sharing with others**
- 🐛 **Reporting issues**
- 💡 **Suggesting improvements**

**Made with ❤️ for the Bangla sign language community**

---

*If you use this project for research, education, or development, please cite or star the repository! Contributions and issues are always welcome.*