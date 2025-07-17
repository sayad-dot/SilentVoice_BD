#!/usr/bin/env python3
import sys
import os
import tempfile
import subprocess
from pathlib import Path

def install_required_packages():
    """Install required packages if not available"""
    required_packages = ['gtts', 'pydub']
    
    for package in required_packages:
        try:
            __import__(package)
        except ImportError:
            print(f"Installing {package}...")
            subprocess.check_call([sys.executable, '-m', 'pip', 'install', package])

def text_to_speech_bangla(text, output_file):
    """Convert Bangla text to speech and save as WAV file"""
    try:
        # Ensure required packages are installed before importing
        install_required_packages()
        from gtts import gTTS
        from pydub import AudioSegment
        
        # Create gTTS object for Bangla
        tts = gTTS(text=text, lang='bn', slow=False)
        
        # Save to temporary MP3 file first
        with tempfile.NamedTemporaryFile(suffix='.mp3', delete=False) as temp_mp3:
            tts.save(temp_mp3.name)
            
            # Convert MP3 to WAV for better compatibility
            audio = AudioSegment.from_mp3(temp_mp3.name)
            audio.export(output_file, format="wav")
            
            # Clean up temporary file
            os.unlink(temp_mp3.name)
        
        print(f"✅ Audio saved to: {output_file}")
        return True
        
    except Exception as e:
        print(f"❌ Error generating speech: {e}")
        return False

def validate_bangla_text(text):
    """Validate if text contains Bangla characters"""
    # Check if text contains Bangla Unicode characters
    bangla_range = range(0x0980, 0x09FF)  # Bangla Unicode range
    has_bangla = any(ord(char) in bangla_range for char in text)
    
    if not has_bangla:
        print(f"⚠️  Warning: Text '{text}' may not contain Bangla characters")
    
    return True

def main():
    if len(sys.argv) != 3:
        print("Usage: python text_to_speech.py '<bangla_text>' '<output_file>'")
        print("Example: python text_to_speech.py 'দাদা' 'output.wav'")
        sys.exit(1)
    
    bangla_text = sys.argv[1]
    output_file = sys.argv[2]
    
    print(f"🔊 Converting to speech: {bangla_text}")
    
    # Validate input
    if not validate_bangla_text(bangla_text):
        sys.exit(1)
    
    # Ensure output directory exists
    output_path = Path(output_file)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    
    # Install required packages
    install_required_packages()
    
    # Generate speech
    success = text_to_speech_bangla(bangla_text, output_file)
    
    if success:
        print("🎉 Text-to-speech conversion successful!")
        # Verify file was created
        if os.path.exists(output_file):
            file_size = os.path.getsize(output_file)
            print(f"📁 File size: {file_size} bytes")
            sys.exit(0)
        else:
            print("❌ Output file was not created")
            sys.exit(1)
    else:
        print("💥 Text-to-speech conversion failed!")
        sys.exit(1)

if __name__ == "__main__":
    main()
