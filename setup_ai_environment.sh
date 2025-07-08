#!/bin/bash

# SilentVoice_BD AI Environment Setup Script (Python 3.12 Compatible)
echo "🚀 Setting up AI environment for SilentVoice_BD..."

# Check if virtual environment exists
if [ ! -d "ai-env" ]; then
    echo "❌ Virtual environment 'ai-env' not found!"
    echo "Please create it first with: python3 -m venv ai-env"
    exit 1
fi

# Activate virtual environment
echo "📦 Activating virtual environment..."
source ai-env/bin/activate

# Verify activation
if [ "$VIRTUAL_ENV" = "" ]; then
    echo "❌ Failed to activate virtual environment"
    exit 1
fi

echo "✅ Virtual environment activated: $VIRTUAL_ENV"

# Set environment variables to suppress TensorFlow warnings
echo "🔧 Setting environment variables..."
export TF_CPP_MIN_LOG_LEVEL=2
export TF_ENABLE_ONEDNN_OPTS=0
export PYTHONUNBUFFERED=1

# Update pip
echo "⬆️ Updating pip..."
pip install --upgrade pip

# Install core packages with Python 3.12 compatible versions
echo "📚 Installing AI packages (Python 3.12 compatible)..."
pip install tensorflow-cpu==2.17.0
pip install mediapipe==0.10.14
pip install opencv-python==4.8.1.78
pip install numpy==1.24.3
pip install scikit-learn==1.3.2

# Verify installations
echo "🔍 Verifying installations..."
python -c "import tensorflow as tf; print(f'TensorFlow: {tf.__version__}')"
python -c "import mediapipe as mp; print(f'MediaPipe: {mp.__version__}')"
python -c "import cv2; print(f'OpenCV: {cv2.__version__}')"

echo "✅ Environment setup complete!"
echo ""
echo "📝 To use this environment:"
echo "   source ai-env/bin/activate"
echo "   export TF_CPP_MIN_LOG_LEVEL=2"
echo "   export TF_ENABLE_ONEDNN_OPTS=0"
echo ""
echo "🎯 Ready for AI processing!"
