import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import apiClient from '../../services/apiClient';
import '../../styles/learning/lesson.css';

const LessonHome = ({ onSelectLesson }) => {
    const { user } = useAuth();
    const [lessons, setLessons] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchLessons();
    }, []);

    const fetchLessons = async () => {
        try {
            const response = await apiClient.get('/api/learning/lessons');
            setLessons(response.data);
            setLoading(false);
        } catch (error) {
            console.error('Error fetching lessons:', error);
            setLoading(false);
        }
    };

    const startLesson = async (lessonId) => {
        try {
            await apiClient.post(`/api/learning/lessons/${lessonId}/start`);
            onSelectLesson(lessonId);
        } catch (error) {
            console.error('Error starting lesson:', error);
        }
    };

    const getStatusColor = (status) => {
        switch (status) {
            case 'COMPLETED': return '#4CAF50';
            case 'IN_PROGRESS': return '#FF9800';
            default: return '#9E9E9E';
        }
    };

    const getDifficultyLabel = (level) => {
        switch (level) {
            case 1: return 'Beginner';
            case 2: return 'Intermediate';
            case 3: return 'Advanced';
            default: return 'Unknown';
        }
    };

    if (loading) {
        return <div className="lesson-loading">Loading lessons...</div>;
    }

    return (
        <div className="lesson-home">
            <div className="lesson-header">
                <h1>Bangla Sign Language Lessons</h1>
                <p>Learn at your own pace with interactive lessons and AI feedback</p>
            </div>

            <div className="lesson-grid">
                {lessons.map((lesson) => (
                    <div key={lesson.id} className="lesson-card">
                        <div className="lesson-card-header">
                            <h3>{lesson.title}</h3>
                            <span 
                                className="lesson-status"
                                style={{ backgroundColor: getStatusColor(lesson.status) }}
                            >
                                {lesson.status.replace('_', ' ')}
                            </span>
                        </div>

                        <div className="lesson-card-content">
                            <p className="lesson-description">{lesson.description}</p>
                            
                            <div className="lesson-meta">
                                <div className="lesson-meta-item">
                                    <span className="meta-label">Difficulty:</span>
                                    <span className="meta-value">{getDifficultyLabel(lesson.difficultyLevel)}</span>
                                </div>
                                <div className="lesson-meta-item">
                                    <span className="meta-label">Duration:</span>
                                    <span className="meta-value">{lesson.estimatedDuration} min</span>
                                </div>
                                <div className="lesson-meta-item">
                                    <span className="meta-label">Type:</span>
                                    <span className="meta-value">{lesson.lessonType}</span>
                                </div>
                            </div>

                            {lesson.status === 'COMPLETED' && (
                                <div className="lesson-progress">
                                    <div className="progress-item">
                                        <span>Accuracy: {lesson.accuracyScore}%</span>
                                    </div>
                                    <div className="progress-item">
                                        <span>Attempts: {lesson.attempts}</span>
                                    </div>
                                    <div className="progress-item">
                                        <span>Time: {Math.floor(lesson.timeSpent / 60)}m {lesson.timeSpent % 60}s</span>
                                    </div>
                                </div>
                            )}
                        </div>

                        <div className="lesson-card-actions">
                            <button 
                                className="btn-primary"
                                onClick={() => startLesson(lesson.id)}
                            >
                                {lesson.status === 'COMPLETED' ? 'Practice Again' :
                                 lesson.status === 'IN_PROGRESS' ? 'Continue' : 'Start Lesson'}
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default LessonHome;
