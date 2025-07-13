import React, { useState, useRef, useEffect } from 'react';
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

      // Try to get existing audio first
      let audioEndpoint = `/api/audio/prediction/${predictionId}`;
      let response = await fetch(audioEndpoint);
      
      if (!response.ok) {
        // If audio doesn't exist, generate it
        const generateResponse = await fetch('/api/audio/generate', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ text: text })
        });
        
        if (generateResponse.ok) {
          const result = await generateResponse.json();
          audioEndpoint = result.audioUrl;
          response = await fetch(audioEndpoint);
        } else {
          throw new Error('Failed to generate audio');
        }
      }
      
      if (!response.ok) {
        throw new Error('Failed to load audio');
      }
      
      const audioBlob = await response.blob();
      const url = URL.createObjectURL(audioBlob);
      
      setAudioUrl(url);
      setIsLoading(false);
      
      // Create and play audio
      const audio = new Audio(url);
      audioRef.current = audio;
      
      audio.volume = volume;
      audio.onended = () => {
        setIsPlaying(false);
        URL.revokeObjectURL(url);
        setAudioUrl(null);
      };
      
      audio.onerror = () => {
        setError('Failed to play audio');
        setIsPlaying(false);
        setIsLoading(false);
        URL.revokeObjectURL(url);
        setAudioUrl(null);
      };
      
      await audio.play();
      
    } catch (err) {
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
        <button 
          className={`play-button ${isPlaying ? 'playing' : ''} ${isLoading ? 'loading' : ''}`}
          onClick={isPlaying ? stopAudio : playAudio}
          disabled={!predictionId || isLoading}
        >
          {isLoading ? (
            <>
              <span className="loading-icon">⏳</span>
              <span>Loading...</span>
            </>
          ) : isPlaying ? (
            <>
              <span className="stop-icon">⏹️</span>
              <span>Stop</span>
            </>
          ) : (
            <>
              <span className="play-icon">🔊</span>
              <span>Play Audio</span>
            </>
          )}
        </button>

        <div className="volume-control">
          <span className="volume-icon">🔉</span>
          <input
            type="range"
            min="0"
            max="1"
            step="0.1"
            value={volume}
            onChange={handleVolumeChange}
            className="volume-slider"
            title={`Volume: ${Math.round(volume * 100)}%`}
          />
          <span className="volume-percentage">{Math.round(volume * 100)}%</span>
        </div>
      </div>

      <div className="audio-info">
        <span className="audio-text">"{text}"</span>
        {isPlaying && (
          <div className="playing-indicator">
            <span className="wave-animation">🎵</span>
            <span>Playing Bangla audio...</span>
          </div>
        )}
      </div>

      {error && (
        <div className="audio-error">
          <span>❌ {error}</span>
          <button onClick={() => setError(null)} className="dismiss-btn">
            Dismiss
          </button>
        </div>
      )}
    </div>
  );
};

export default AudioPlayer;
