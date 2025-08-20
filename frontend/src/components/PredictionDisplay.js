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

  // ✅ ADD THESE MISSING FUNCTIONS
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

  // Map English/Banglish class names to Bangla
  const getBanglaTranslation = (text) => {
    if (!text) return 'Translation not available';
    const translations = {
      // Direct BDSLW60 and common demo classes
      'dongson': 'দংশন',
      'attio': 'আত্তিও',
      'durbol': 'দুর্বল',
      'denadar': 'দেনাদার',
      'dada': 'দাদা',
      'dadi': 'দাদি',
      'maa': 'মা',
      'baba': 'বাবা',
      'bhai': 'ভাই',
      'bon': 'বোন',
      'chacha': 'চাচা',
      'chachi': 'চাচি',
      'aam': 'আম',
      'aaple': 'আপেল',
      'cha': 'চা',
      'chocolate': 'চকলেট',
      'cake': 'কেক',
      'tv': 'টিভি',
      'doctor': 'ডাক্তার',
      'aids': 'এইডস',
      'dengue': 'ডেঙ্গু',
      'capsule': 'ক্যাপসুল',
      'hello': 'হ্যালো',
      'dhonnobad': 'ধন্যবাদ'
      // ... add more as needed
    };

    // If input is already Bangla (for safety), just return
    if (/[\u0980-\u09FF]/.test(text)) return text;
    const lowerInput = text.trim().toLowerCase();
    return translations[lowerInput] || text;
  };

  // English meaning for Bangla and/or English class names
  const getEnglishMeaning = (text) => {
    const meanings = {
      'dongson': 'Bite/Sting',
      'attio': 'Attio',
      'durbol': 'Weak',
      'denadar': 'Debtor',
      'dada': 'Grandfather / Elder Brother',
      'dadi': 'Grandmother',
      'maa': 'Mother',
      'baba': 'Father',
      'bhai': 'Brother',
      'bon': 'Sister',
      'chacha': 'Uncle',
      'chachi': 'Aunt',
      'aam': 'Mango',
      'aaple': 'Apple',
      'cha': 'Tea',
      'chocolate': 'Chocolate',
      'cake': 'Cake',
      'tv': 'TV',
      'doctor': 'Doctor',
      'aids': 'AIDS',
      'dengue': 'Dengue',
      'capsule': 'Capsule',
      'hello': 'Hello',
      'dhonnobad': 'Thank you',
      // Sometimes backend may send Bangla directly—reverse map:
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
      'ধন্যবাদ': 'Thank you',
      'দুর্বল': 'Weak',
      'দেনাদার': 'Debtor',
      'আত্তিও': 'Attio',
      'দংশন': 'Bite/Sting',
      // ... (expand as needed)
    };
    return meanings[text] || 'Translation not available';
  };

  // The main display logic (Bangla always shown)
  let displayBangla;
  if ("banglaTranslation" in prediction && prediction.banglaTranslation) {
    displayBangla = prediction.banglaTranslation;
  } else if ("bangla_translation" in prediction && prediction.bangla_translation) {
    displayBangla = prediction.bangla_translation;
  } else {
    displayBangla = getBanglaTranslation(prediction.predictedText || prediction.predicted_text);
  }

  // English meaning logic (try backend, else look up)
  let displayEnglish;
  if ("englishTranslation" in prediction && prediction.englishTranslation) {
    displayEnglish = prediction.englishTranslation;
  } else if ("english_translation" in prediction && prediction.english_translation) {
    displayEnglish = prediction.english_translation;
  } else {
    displayEnglish = getEnglishMeaning(prediction.predictedText || prediction.predicted_text);
  }

  // Use unified field access for prediction fields (to handle both camelCase and snake_case)
  const confidence = prediction.confidenceScore !== undefined
    ? prediction.confidenceScore
    : prediction.confidence;
  const processingTime = prediction.processingTimeMs !== undefined
    ? prediction.processingTimeMs
    : prediction.processing_time_ms;
  const modelVersion = prediction.modelVersion || prediction.model_version;
  const predictionId = prediction.id || prediction.prediction_id;
  const createdAt = prediction.createdAt || prediction.created_at;

  return (
    <div className="prediction-display">
      <div className="prediction-header">
        <h2>✅ Sign Language Recognized!</h2>
      </div>

      <div className="prediction-main">
        <div className="predicted-text-card">
          <div className="bangla-text">
            {displayBangla}
          </div>
          <div className="english-translation">
            {displayEnglish}
          </div>
        </div>

        <div className="confidence-display">
          <div className="confidence-label">Confidence</div>
          <div
            className="confidence-score"
            style={{ backgroundColor: getConfidenceColor(confidence) }}
          >
            {(confidence * 100).toFixed(1)}%
          </div>
          <div className="confidence-text">
            {getConfidenceText(confidence)}
          </div>
        </div>
      </div>

      <div className="audio-section">
        <AudioPlayer
          predictionId={predictionId}
          text={displayBangla}
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
          onClick={() => navigator.clipboard.writeText(displayBangla)}
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
            <strong>Processing Time:</strong> {processingTime}ms
          </div>
          <div className="detail-item">
            <strong>Model Version:</strong> {modelVersion}
          </div>
          <div className="detail-item">
            <strong>Prediction ID:</strong> {predictionId}
          </div>
          <div className="detail-item">
            <strong>Video ID:</strong> {videoId}
          </div>
          <div className="detail-item">
            <strong>Timestamp:</strong> {createdAt ? new Date(createdAt).toLocaleString() : ''}
          </div>
        </div>
      )}
    </div>
  );
};

export default PredictionDisplay;
