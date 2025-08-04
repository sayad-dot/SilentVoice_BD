import React, { useState, useRef, useEffect } from 'react';
import audioService from '../services/audioService';
import './AudioPlayer.css';

const AudioPlayer = ({ predictionId, text }) => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [audioUrl, setAudioUrl] = useState(null);
  const [volume, setVolume] = useState(1.0);
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const audioRef = useRef(null);

  useEffect(() => {
    return () => {
      // Cleanup audio URL on component unmount
      if (audioUrl) {
        URL.revokeObjectURL(audioUrl);
      }
    };
  }, [audioUrl]);

  const playAudio = async () => {
    try {
      setError(null);
      setIsLoading(true);
      setIsPlaying(true);

      console.log('🔊 Starting audio playbook for prediction:', predictionId);

      let audioBlob = null;

      try {
        // Try to get existing audio first
        console.log('📡 Trying to get existing audio...');
        audioBlob = await audioService.getAudioForPrediction(predictionId);
        console.log('✅ Got existing audio blob');
      } catch (error) {
        console.log('⚠️ No existing audio, generating new one...');
        
        // If audio doesn't exist, generate it
        const generateResponse = await audioService.generateAudio(text);
        console.log('✅ Audio generation response:', generateResponse);

        if (generateResponse.audioUrl) {
          // Now try to get the generated audio
          const audioId = generateResponse.predictionId || predictionId;
          audioBlob = await audioService.getAudioForPrediction(audioId);
          console.log('✅ Got generated audio blob');
        } else {
          throw new Error('Failed to generate audio');
        }
      }

      if (!audioBlob) {
        throw new Error('Failed to get audio blob');
      }

      const url = URL.createObjectURL(audioBlob);
      setAudioUrl(url);
      setIsLoading(false);

      // Create and play audio
      const audio = new Audio(url);
      audioRef.current = audio;
      audio.volume = volume;

      audio.onended = () => {
        console.log('🔊 Audio playback ended');
        setIsPlaying(false);
        URL.revokeObjectURL(url);
        setAudioUrl(null);
      };

      audio.onerror = (e) => {
        console.error('❌ Audio playback error:', e);
        setError('Failed to play audio');
        setIsPlaying(false);
        setIsLoading(false);
        URL.revokeObjectURL(url);
        setAudioUrl(null);
      };

      await audio.play();
      console.log('🔊 Audio started playing');

    } catch (err) {
      console.error('❌ Error in playAudio:', err);
      setError('Error playing audio: ' + err.message);
      setIsPlaying(false);
      setIsLoading(false);
      if (audioUrl) {
        URL.revokeObjectURL(audioUrl);
        setAudioUrl(null);
      }
    }
  };

  const stopAudio = () => {
    if (audioRef.current) {
      audioRef.current.pause();
      audioRef.current.currentTime = 0;
      setIsPlaying(false);
      if (audioUrl) {
        URL.revokeObjectURL(audioUrl);
        setAudioUrl(null);
      }
    }
  };

  const handleVolumeChange = (event) => {
    const newVolume = parseFloat(event.target.value);
    setVolume(newVolume);
    if (audioRef.current) {
      audioRef.current.volume = newVolume;
    }
  };

  return (
    <div className="audio-player">
      <div className="audio-controls">
        {!isPlaying && !isLoading && (
          <button 
            onClick={playAudio}
            className="play-button"
            disabled={!predictionId || !text}
          >
            🔊 Play Audio
          </button>
        )}

        {isLoading && (
          <button className="loading-button" disabled>
            ⏳ Loading...
          </button>
        )}

        {isPlaying && !isLoading && (
          <button 
            onClick={stopAudio}
            className="stop-button"
          >
            ⏹️ Stop
          </button>
        )}

        <div className="volume-control">
          <label htmlFor="volume">🔊</label>
          <input
            id="volume"
            type="range"
            min="0"
            max="1"
            step="0.1"
            value={volume}
            onChange={handleVolumeChange}
            className="volume-slider"
          />
          <span className="volume-value">{Math.round(volume * 100)}%</span>
        </div>
      </div>

      {error && (
        <div className="audio-error">
          ❌ {error}
        </div>
      )}

      {text && (
        <div className="audio-text">
          📝 Text: "{text}"
        </div>
      )}
    </div>
  );
};

export default AudioPlayer;

