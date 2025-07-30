import React, { useState, useEffect } from 'react';
import apiClient from '../../services/apiClient';
import MirrorPractice from './MirrorPractice';
import ChatbotWidget from './ChatbotWidget';
import '../../styles/learning/lesson.css';

const LessonViewer = ({ lessonId, onBack }) => {
    const [lesson, setLesson] = useState(null);
    const [currentStep, setCurrentStep] = useState(0);
    const [startTime, setStartTime] = useState(null);
    const [showMirror, setShowMirror] = useState(false);
    const [showChatbot, setShowChatbot] = useState(false);
    const [totalAccuracy, setTotalAccuracy] = useState(0);
    const [stepCount, setStepCount] = useState(0);
    
    // New state for BdSLW-60 video integration
    const [availableVideos, setAvailableVideos] = useState({});
    const [currentVideoIndex, setCurrentVideoIndex] = useState(0);
    const [videoLoading, setVideoLoading] = useState(false);

    useEffect(() => {
        fetchLesson();
        setStartTime(Date.now());
    }, [lessonId]);

    // Load available videos when lesson changes
    useEffect(() => {
        if (lesson) {
            loadAvailableVideos();
        }
    }, [lesson]);

    // Reset video index when step changes
    useEffect(() => {
        setCurrentVideoIndex(0);
    }, [currentStep]);

    const fetchLesson = async () => {
        try {
            const response = await apiClient.get(`/api/learning/lessons/${lessonId}`);
            setLesson(response.data);
        } catch (error) {
            console.error('Error fetching lesson:', error);
        }
    };

    const loadAvailableVideos = async () => {
        try {
            setVideoLoading(true);
            const content = getLessonContent();
            const videoMap = {};
            
            for (const sign of content) {
                try {
                    const response = await apiClient.get(`/api/media/videos/${sign}`);
                    videoMap[sign] = response.data;
                } catch (error) {
                    console.error(`Error loading videos for sign ${sign}:`, error);
                    videoMap[sign] = []; // Empty array if no videos found
                }
            }
            
            setAvailableVideos(videoMap);
            setVideoLoading(false);
        } catch (error) {
            console.error('Error loading available videos:', error);
            setVideoLoading(false);
        }
    };

    const getLessonContent = () => {
        if (!lesson?.contentData) return [];
        
        const contentData = typeof lesson.contentData === 'string' 
            ? JSON.parse(lesson.contentData) 
            : lesson.contentData;
        
        switch (lesson.lessonType) {
            case 'ALPHABET':
                return contentData.letters || [];
            case 'WORD':
                return contentData.words || [];
            case 'PHRASE':
                return contentData.phrases || [];
            default:
                return [];
        }
    };

    const getCurrentContent = () => {
        const content = getLessonContent();
        return content[currentStep] || '';
    };

    const getDemoVideo = () => {
        if (!lesson?.contentData) return null;
        
        const contentData = typeof lesson.contentData === 'string' 
            ? JSON.parse(lesson.contentData) 
            : lesson.contentData;
            
        return contentData.demo_videos?.[currentStep] || null;
    };

    const getCurrentVideoUrl = () => {
        const currentContent = getCurrentContent();
        const videos = availableVideos[currentContent];
        
        if (videos && videos.length > 0) {
            const videoFile = videos[currentVideoIndex % videos.length];
            return `/api/media/video/${currentContent}/${videoFile}`;
        }
        
        return null;
    };

    const nextVideo = () => {
        const currentContent = getCurrentContent();
        const videos = availableVideos[currentContent];
        if (videos && videos.length > 1) {
            setCurrentVideoIndex((prev) => (prev + 1) % videos.length);
        }
    };

    const prevVideo = () => {
        const currentContent = getCurrentContent();
        const videos = availableVideos[currentContent];
        if (videos && videos.length > 1) {
            setCurrentVideoIndex((prev) => (prev - 1 + videos.length) % videos.length);
        }
    };

    const nextStep = () => {
        const content = getLessonContent();
        if (currentStep < content.length - 1) {
            setCurrentStep(currentStep + 1);
        } else {
            completeLesson();
        }
    };

    const prevStep = () => {
        if (currentStep > 0) {
            setCurrentStep(currentStep - 1);
        }
    };

    const completeLesson = async () => {
        const timeSpent = Math.floor((Date.now() - startTime) / 1000);
        const averageAccuracy = stepCount > 0 ? (totalAccuracy / stepCount) : 0;

        try {
            await apiClient.post(`/api/learning/lessons/${lessonId}/complete`, {
                accuracyScore: averageAccuracy,
                timeSpent: timeSpent
            });
            onBack();
        } catch (error) {
            console.error('Error completing lesson:', error);
        }
    };

    const handlePracticeResult = (accuracy) => {
        setTotalAccuracy(prev => prev + accuracy);
        setStepCount(prev => prev + 1);
    };

    if (!lesson) {
        return <div className="lesson-loading">Loading lesson...</div>;
    }

    const content = getLessonContent();
    const currentContent = getCurrentContent();
    const demoVideo = getDemoVideo();
    const currentVideoUrl = getCurrentVideoUrl();

    return (
        <div className="lesson-viewer">
            <div className="lesson-header">
                <button className="btn-back" onClick={onBack}>
                    ← Back to Lessons
                </button>
                <h1>{lesson.title}</h1>
                <div className="lesson-progress-bar">
                    <div 
                        className="progress-fill" 
                        style={{ width: `${((currentStep + 1) / content.length) * 100}%` }}
                    ></div>
                </div>
                <span className="progress-text">{currentStep + 1} of {content.length}</span>
            </div>

            <div className="lesson-content">
                <div className="lesson-main">
                    <div className="content-display">
                        <h2>Learn: {currentContent}</h2>
                        
                        {/* Updated video rendering section */}
                        {videoLoading ? (
                            <div className="demo-placeholder">
                                <div className="sign-demonstration">
                                    <h3>Loading videos...</h3>
                                    <p>📹 Please wait while we load demonstration videos</p>
                                </div>
                            </div>
                        ) : currentVideoUrl ? (
                            <div className="demo-video">
                                <video controls width="400" key={currentVideoUrl}>
                                    <source src={currentVideoUrl} type="video/mp4" />
                                    Your browser does not support the video tag.
                                </video>
                                <div className="video-controls">
                                    <p className="video-caption">Demo: {currentContent}</p>
                                    {availableVideos[currentContent]?.length > 1 && (
                                        <div className="video-navigation">
                                            <button onClick={prevVideo} className="btn-secondary btn-small">
                                                ← Previous
                                            </button>
                                            <span className="video-counter">
                                                Video {currentVideoIndex + 1} of {availableVideos[currentContent]?.length}
                                            </span>
                                            <button onClick={nextVideo} className="btn-secondary btn-small">
                                                Next →
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </div>
                        ) : demoVideo ? (
                            // Fallback to old demo video system
                            <div className="demo-video">
                                <video controls width="400">
                                    <source src={`/videos/lessons/${demoVideo}`} type="video/mp4" />
                                    Your browser does not support the video tag.
                                </video>
                                <div className="video-controls">
                                    <p className="video-caption">Demo: {currentContent}</p>
                                </div>
                            </div>
                        ) : (
                            <div className="demo-placeholder">
                                <div className="sign-demonstration">
                                    <h3>Practice: {currentContent}</h3>
                                    <p>📹 No demo video available</p>
                                    <div className="practice-tips">
                                        <p>• Position your hands clearly in front of the camera</p>
                                        <p>• Make slow, deliberate movements</p>
                                        <p>• Ensure good lighting for better detection</p>
                                        <p>• Use the Mirror Practice feature below for real-time feedback</p>
                                    </div>
                                </div>
                            </div>
                        )}

                        <div className="lesson-instructions">
                            <p>
                                {currentVideoUrl ? 
                                    "Watch the demo video above and practice the sign." : 
                                    "Practice the sign using the instructions above."
                                }
                            </p>
                            <p>Use the Mirror Practice feature to get real-time feedback on your signing.</p>
                        </div>
                    </div>

                    <div className="lesson-actions">
                        <button 
                            className="btn-secondary"
                            onClick={() => setShowMirror(!showMirror)}
                        >
                            {showMirror ? 'Hide' : 'Show'} Mirror Practice
                        </button>
                        
                        <button 
                            className="btn-secondary"
                            onClick={() => setShowChatbot(!showChatbot)}
                        >
                            {showChatbot ? 'Hide' : 'Show'} AI Helper
                        </button>

                        <div className="navigation-buttons">
                            <button 
                                className="btn-outline" 
                                onClick={prevStep}
                                disabled={currentStep === 0}
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

                {showMirror && (
                    <div className="mirror-practice-container">
                        <MirrorPractice 
                            lessonId={lesson.id}
                            expectedSign={currentContent}
                            onResult={handlePracticeResult}
                        />
                    </div>
                )}
            </div>

{showChatbot && (
    <ChatbotWidget 
        lessonId={lesson.id}
        currentSign={currentContent}  // Pass current sign
        onClose={() => setShowChatbot(false)}
    />
)}
        </div>
    );
};

export default LessonViewer;
