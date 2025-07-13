import React, { useState } from 'react';
import AudioPlayer from './AudioPlayer';
import './PredictionDisplay.css';

const PredictionDisplay = ({ prediction, onReset, videoId }) => {
  const [showDetails, setShowDetails] = useState(false);

  console.log('PredictionDisplay received prediction:', prediction);

  if (!prediction) {
    console.log('No prediction data available');
    return null;
  }

  const getConfidenceColor = (confidence) => {
    if (confidence >= 0.8) return '#4CAF50';
    if (confidence >= 0.6) return '#FF9800';
    return '#F44336';
  };

  const getConfidenceText = (confidence) => {
    if (confidence >= 0.8) return 'High';
    if (confidence >= 0.6) return 'Medium';
    return 'Low';
  };

  const getEnglishTranslation = (banglaText) => {
    const translations = {
      'দাদা': 'Grandfather / Elder Brother',
      'দাদি': 'Grandmother',
      'মা': 'Mother',
      'বাবা': 'Father',
      'ভাই': 'Brother',
      'বোন': 'Sister',
      'আম': 'Mango',
      'আপেল': 'Apple',
      'চা': 'Tea',
      'হ্যালো': 'Hello',
      'ধন্যবাদ': 'Thank you'
    };
    return translations[banglaText] || 'Translation not available';
  };

  return (
    <div className="prediction-display">
      <div className="prediction-header">
        <h2>✅ Sign Language Recognized!</h2>
      </div>

      <div className="prediction-main">
        <div className="predicted-text-card">
          <div className="bangla-text">
            {prediction.predictedText}
          </div>
          <div className="english-translation">
            {getEnglishTranslation(prediction.predictedText)}
          </div>
        </div>

        <div className="confidence-display">
          <div className="confidence-label">Confidence</div>
          <div
            className="confidence-score"
            style={{ backgroundColor: getConfidenceColor(prediction.confidenceScore) }}
          >
            {(prediction.confidenceScore * 100).toFixed(1)}%
          </div>
          <div className="confidence-text">
            {getConfidenceText(prediction.confidenceScore)}
          </div>
        </div>
      </div>

      <div className="audio-section">
        <AudioPlayer
          predictionId={prediction.id}
          text={prediction.predictedText}
        />
      </div>

      <div className="action-buttons">
        <button
          className="details-button"
          onClick={() => setShowDetails(!showDetails)}
        >
          {showDetails ? 'Hide Details' : 'Show Details'}
        </button>

        <button
          className="copy-button"
          onClick={() => navigator.clipboard.writeText(prediction.predictedText)}
        >
          📋 Copy Text
        </button>

        <button
          className="new-upload-button"
          onClick={onReset}
        >
          📹 Upload New Video
        </button>
      </div>

      {showDetails && (
        <div className="prediction-details">
          <h4>Processing Details</h4>
          <div className="detail-item">
            <strong>Processing Time:</strong> {prediction.processingTimeMs}ms
          </div>
          <div className="detail-item">
            <strong>Model Version:</strong> {prediction.modelVersion}
          </div>
          <div className="detail-item">
            <strong>Prediction ID:</strong> {prediction.id}
          </div>
          <div className="detail-item">
            <strong>Video ID:</strong> {videoId}
          </div>
          <div className="detail-item">
            <strong>Timestamp:</strong> {new Date(prediction.createdAt).toLocaleString()}
          </div>
        </div>
      )}
    </div>
  );
};

export default PredictionDisplay;
