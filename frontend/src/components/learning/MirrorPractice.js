import React, { useRef, useEffect, useState, useCallback } from 'react';
import { Pose } from '@mediapipe/pose';
import { Camera } from '@mediapipe/camera_utils';
import { useAuth } from '../../contexts/AuthContext';
import apiClient from '../../services/apiClient';
import '../../styles/learning/mirror.css';

const MirrorPractice = ({ lessonId, expectedSign, onResult }) => {
    const { user } = useAuth();
    const videoRef = useRef(null);
    const canvasRef = useRef(null);
    const [isActive, setIsActive] = useState(false);
    const [feedback, setFeedback] = useState('');
    const [confidenceScore, setConfidenceScore] = useState(0);
    const [sessionId, setSessionId] = useState(null);
    const [pose, setPose] = useState(null);
    const [camera, setCamera] = useState(null);

    const initializePose = useCallback(() => {
        const poseInstance = new Pose({
            locateFile: (file) => {
                return `https://cdn.jsdelivr.net/npm/@mediapipe/pose/${file}`;
            }
        });

        poseInstance.setOptions({
            modelComplexity: 0,
            smoothLandmarks: true,
            enableSegmentation: false,
            smoothSegmentation: false,
            minDetectionConfidence: 0.5,
            minTrackingConfidence: 0.5
        });

        poseInstance.onResults(onPoseResults);
        setPose(poseInstance);

        // Initialize camera
        if (videoRef.current) {
            const cameraInstance = new Camera(videoRef.current, {
                onFrame: async () => {
                    await poseInstance.send({ image: videoRef.current });
                },
                width: 640,
                height: 480
            });
            setCamera(cameraInstance);
            cameraInstance.start();
        }
    }, []);

    useEffect(() => {
        initializePose();
        return () => {
            if (camera) {
                camera.stop();
            }
        };
    }, [initializePose]);

    const onPoseResults = async (results) => {
        if (!canvasRef.current || !videoRef.current) return;

        const canvas = canvasRef.current;
        const ctx = canvas.getContext('2d');
        
        canvas.width = videoRef.current.videoWidth;
        canvas.height = videoRef.current.videoHeight;

        // Clear canvas
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        
        // Draw video frame
        ctx.drawImage(videoRef.current, 0, 0, canvas.width, canvas.height);

        // Draw pose landmarks if detected
        if (results.poseLandmarks) {
            drawPoseLandmarks(ctx, results.poseLandmarks);
            
            // Send pose data for analysis if active
            if (isActive) {
                await analyzePose(results.poseLandmarks);
            }
        }

        // Draw feedback overlay
        drawFeedbackOverlay(ctx);
    };

    const drawPoseLandmarks = (ctx, landmarks) => {
        // Draw key pose points
        const keyPoints = [11, 12, 13, 14, 15, 16, 19, 20]; // Arms and hands
        
        ctx.fillStyle = '#00FF00';
        ctx.strokeStyle = '#00FF00';
        ctx.lineWidth = 2;

        keyPoints.forEach(index => {
            if (landmarks[index]) {
                const x = landmarks[index].x * ctx.canvas.width;
                const y = landmarks[index].y * ctx.canvas.height;
                
                ctx.beginPath();
                ctx.arc(x, y, 4, 0, 2 * Math.PI);
                ctx.fill();
            }
        });

        // Draw connections between arm points
        const connections = [
            [11, 13], [13, 15], [15, 19], // Left arm
            [12, 14], [14, 16], [16, 20]  // Right arm
        ];

        connections.forEach(([start, end]) => {
            if (landmarks[start] && landmarks[end]) {
                const startX = landmarks[start].x * ctx.canvas.width;
                const startY = landmarks[start].y * ctx.canvas.height;
                const endX = landmarks[end].x * ctx.canvas.width;
                const endY = landmarks[end].y * ctx.canvas.height;

                ctx.beginPath();
                ctx.moveTo(startX, startY);
                ctx.lineTo(endX, endY);
                ctx.stroke();
            }
        });
    };

    const drawFeedbackOverlay = (ctx) => {
        if (!feedback && confidenceScore === 0) return;

        const canvas = ctx.canvas;
        
        // Draw semi-transparent background
        ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
        ctx.fillRect(10, 10, canvas.width - 20, 100);

        // Draw confidence bar
        ctx.fillStyle = confidenceScore > 70 ? '#4CAF50' : 
                       confidenceScore > 40 ? '#FF9800' : '#F44336';
        ctx.fillRect(20, 20, (canvas.width - 40) * (confidenceScore / 100), 20);

        // Draw confidence text
        ctx.fillStyle = '#FFFFFF';
        ctx.font = '16px Arial';
        ctx.fillText(`Confidence: ${confidenceScore.toFixed(1)}%`, 20, 60);
        
        // Draw feedback text
        ctx.font = '14px Arial';
        ctx.fillText(feedback || 'Practice your sign...', 20, 85);
    };

    const analyzePose = async (landmarks) => {
        try {
            const response = await apiClient.post('/api/learning/feedback/analyze', {
                lessonId: lessonId,
                expectedSign: expectedSign,
                poseLandmarks: landmarks
            });

            setFeedback(response.data.feedbackText || 'Keep practicing!');
            setConfidenceScore(response.data.confidenceScore * 100 || 0);
            
            if (onResult && response.data.confidenceScore > 0.7) {
                onResult(response.data.confidenceScore * 100);
            }
        } catch (error) {
            console.error('Error analyzing pose:', error);
            setFeedback('Analysis unavailable');
            setConfidenceScore(0);
        }
    };

    const startPractice = async () => {
        try {
            setIsActive(true);
            
            const response = await apiClient.post(`/api/learning/feedback/${lessonId}/session/start`, {
                expectedSign: expectedSign,
                userId: user?.id,
                startTime: new Date().toISOString()
            });
            
            setSessionId(response.data.sessionId);
            setFeedback('Practice session started! Begin making your sign.');
            console.log('Practice session started:', response.data);
            
        } catch (error) {
            console.error('Error starting practice:', error);
            setIsActive(false);
            setFeedback('Could not start session, but you can still practice!');
        }
    };

    const stopPractice = async () => {
        try {
            if (sessionId) {
                await apiClient.post(`/api/learning/feedback/${lessonId}/session/end`, {
                    sessionId: sessionId,
                    expectedSign: expectedSign,
                    endTime: new Date().toISOString()
                });
            }
            
            setIsActive(false);
            setSessionId(null);
            setFeedback('Practice session ended. Great job!');
            
        } catch (error) {
            console.error('Error ending practice:', error);
            // Still stop the session on frontend
            setIsActive(false);
            setSessionId(null);
            setFeedback('Session ended.');
        }
    };

    return (
        <div className="mirror-practice">
            <div className="mirror-header">
                <h2>Mirror Practice: {expectedSign}</h2>
                <div className="mirror-controls">
                    {!isActive ? (
                        <button className="btn-primary" onClick={startPractice}>
                            Start Practice
                        </button>
                    ) : (
                        <button className="btn-danger" onClick={stopPractice}>
                            Stop Practice
                        </button>
                    )}
                </div>
            </div>

            <div className="mirror-container">
                <div className="video-container">
                    <video
                        ref={videoRef}
                        className="mirror-video"
                        playsInline
                        muted
                        style={{ display: 'none' }}
                    />
                    <canvas
                        ref={canvasRef}
                        className="mirror-canvas"
                    />
                </div>

                <div className="mirror-instructions">
                    <h3>Instructions:</h3>
                    <ul>
                        <li>Position yourself so your upper body is visible</li>
                        <li>Make sure your hands are clearly in view</li>
                        <li>Practice the sign slowly and clearly</li>
                        <li>Green lines show your detected pose</li>
                        <li>Aim for 70%+ confidence for good accuracy</li>
                        <li>{isActive ? '🟢 Session Active' : '⚪ Session Inactive'}</li>
                    </ul>
                </div>
            </div>
        </div>
    );
};

export default MirrorPractice;
