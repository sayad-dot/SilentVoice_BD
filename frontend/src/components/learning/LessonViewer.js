import React, { useState, useEffect } from 'react';
import apiClient from '../../services/apiClient';
import MirrorPractice from './MirrorPractice';

import '../../styles/learning/lesson.css';

const LessonViewer = ({ lessonId, onBack }) => {
  const [lesson, setLesson] = useState(null);
  const [currentStep, setCurrentStep] = useState(0);
  const [startTime, setStartTime] = useState(null);
  const [showMirror, setShowMirror] = useState(false);
  const [totalAccuracy, setTotalAccuracy] = useState(0);
  const [stepCount, setStepCount] = useState(0);

  // BdSLW-60 video integration
  const [availableVideos, setAvailableVideos] = useState({});
  const [currentVideoIndex, setCurrentVideoIndex] = useState(0);
  const [videoLoading, setVideoLoading] = useState(false);

  useEffect(() => {
    fetchLesson();
    setStartTime(Date.now());
  }, [lessonId]);

  useEffect(() => {
    if (lesson) loadAvailableVideos();
  }, [lesson]);

  useEffect(() => {
    setCurrentVideoIndex(0);
  }, [currentStep]);

  /* ───────────────────────────
     Data-loading helpers
  ─────────────────────────── */
  const fetchLesson = async () => {
    try {
      const { data } = await apiClient.get(`/api/learning/lessons/${lessonId}`);
      setLesson(data);
    } catch (err) {
      console.error('Error fetching lesson:', err);
    }
  };

  const loadAvailableVideos = async () => {
    try {
      setVideoLoading(true);
      const signs = getLessonContent();
      const videoMap = {};

      for (const sign of signs) {
        try {
          const { data } = await apiClient.get(`/api/media/videos/${sign}`);
          videoMap[sign] = data;
        } catch (err) {
          console.error(`Error loading videos for ${sign}:`, err);
          videoMap[sign] = [];
        }
      }
      setAvailableVideos(videoMap);
    } finally {
      setVideoLoading(false);
    }
  };

  /* ───────────────────────────
     Lesson helpers
  ─────────────────────────── */
  const getLessonContent = () => {
    if (!lesson?.contentData) return [];
    const data = typeof lesson.contentData === 'string'
      ? JSON.parse(lesson.contentData)
      : lesson.contentData;

    switch (lesson.lessonType) {
      case 'ALPHABET': return data.letters || [];
      case 'WORD':     return data.words   || [];
      case 'PHRASE':   return data.phrases || [];
      default:         return [];
    }
  };

  const getCurrentContent = () => getLessonContent()[currentStep] || '';

  const getCurrentVideoUrl = () => {
    const sign = getCurrentContent();
    const vids = availableVideos[sign];
    if (vids?.length) {
      const file = vids[currentVideoIndex % vids.length];
      return `/api/media/video/${sign}/${file}`;
    }
    return null;
  };

  /* ───────────────────────────
     Navigation
  ─────────────────────────── */
  const nextVideo = () => {
    const vids = availableVideos[getCurrentContent()];
    if (vids?.length > 1) {
      setCurrentVideoIndex(i => (i + 1) % vids.length);
    }
  };

  const prevVideo = () => {
    const vids = availableVideos[getCurrentContent()];
    if (vids?.length > 1) {
      setCurrentVideoIndex(i => (i - 1 + vids.length) % vids.length);
    }
  };

  const nextStep = () => {
    const total = getLessonContent().length;
    if (currentStep < total - 1) setCurrentStep(s => s + 1);
    else completeLesson();
  };

  const prevStep = () => currentStep && setCurrentStep(s => s - 1);

  const completeLesson = async () => {
    const timeSpent = Math.floor((Date.now() - startTime) / 1000);
    const avgAccuracy = stepCount ? totalAccuracy / stepCount : 0;
    try {
      await apiClient.post(`/api/learning/lessons/${lessonId}/complete`, {
        accuracyScore: avgAccuracy,
        timeSpent
      });
      onBack();
    } catch (err) {
      console.error('Error completing lesson:', err);
    }
  };

  const handlePracticeResult = acc => {
    setTotalAccuracy(t => t + acc);
    setStepCount(c => c + 1);
  };

  /* ───────────────────────────
     Render
  ─────────────────────────── */
  if (!lesson) return <div className="lesson-loading">Loading lesson...</div>;

  const content      = getLessonContent();
  const sign         = getCurrentContent();
  const videoUrl     = getCurrentVideoUrl();

  return (
    <div className="lesson-viewer">
      {/* Header */}
      <div className="lesson-header">
        <button className="btn-back" onClick={onBack}>← Back to Lessons</button>
        <h1>{lesson.title}</h1>

        <div className="lesson-progress-bar">
          <div
            className="progress-fill"
            style={{ width: `${((currentStep + 1) / content.length) * 100}%` }}
          />
        </div>
        <span className="progress-text">{currentStep + 1} of {content.length}</span>
      </div>

      {/* Main content */}
      <div className="lesson-content">
        <div className="lesson-main">
          <div className="content-display">
            <h2>Learn: {sign}</h2>

            {/* Video area */}
            {videoLoading ? (
              <div className="demo-placeholder">
                <h3>Loading videos…</h3>
                <p>📹 Please wait while we load demonstration videos</p>
              </div>
            ) : videoUrl ? (
              <div className="demo-video">
                <video controls width="400" key={videoUrl}>
                  <source src={videoUrl} type="video/mp4" />
                  Your browser does not support the video tag.
                </video>
                {/* Video navigation */}
                {availableVideos[sign]?.length > 1 && (
                  <div className="video-navigation">
                    <button className="btn-secondary btn-small" onClick={prevVideo}>← Previous</button>
                    <span>Video {currentVideoIndex + 1} of {availableVideos[sign]?.length}</span>
                    <button className="btn-secondary btn-small" onClick={nextVideo}>Next →</button>
                  </div>
                )}
              </div>
            ) : (
              <div className="demo-placeholder">
                <h3>Practice: {sign}</h3>
                <p>📹 No demo video available</p>
                <div className="practice-tips">
                  <p>• Position your hands clearly in front of the camera</p>
                  <p>• Make slow, deliberate movements</p>
                  <p>• Ensure good lighting for better detection</p>
                  <p>• Use the Mirror Practice feature below for real-time feedback</p>
                </div>
              </div>
            )}

            <div className="lesson-instructions">
              <p>
                {videoUrl
                  ? 'Watch the demo video above and practise the sign.'
                  : 'Practise the sign using the instructions above.'}
              </p>
              <p>Use the Mirror Practice feature to get real-time feedback on your signing.</p>
            </div>
          </div>

          {/* Action buttons */}
          <div className="lesson-actions">
            <button
              className="btn-secondary"
              onClick={() => setShowMirror(m => !m)}
            >
              {showMirror ? 'Hide' : 'Show'} Mirror Practice
            </button>

            <div className="navigation-buttons">
              <button
                className="btn-outline"
                onClick={prevStep}
                disabled={!currentStep}
              >
                Previous
              </button>
              <button
                className="btn-primary"
                onClick={nextStep}
              >
                {currentStep === content.length - 1 ? 'Complete Lesson' : 'Next'}
              </button>
            </div>
          </div>
        </div>

        {/* Mirror practice */}
        {showMirror && (
          <div className="mirror-practice-container">
            <MirrorPractice
              lessonId={lesson.id}
              expectedSign={sign}
              onResult={handlePracticeResult}
            />
          </div>
        )}
      </div>
    </div>
  );
};

export default LessonViewer;
