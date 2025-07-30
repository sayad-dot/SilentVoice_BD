import React, { useState, useEffect } from 'react';
import apiClient from '../../services/apiClient';
import '../styles/learning/lesson.css';

const ProgressTracker = ({ userId }) => {
    const [stats, setStats] = useState(null);
    const [recentProgress, setRecentProgress] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchUserStats();
        fetchRecentProgress();
    }, [userId]);

    const fetchUserStats = async () => {
        try {
            const response = await apiClient.get('/api/learning/lessons/stats');
            setStats(response.data);
        } catch (error) {
            console.error('Error fetching user stats:', error);
        }
    };

    const fetchRecentProgress = async () => {
        try {
            const response = await apiClient.get('/api/learning/lessons/progress');
            setRecentProgress(response.data.slice(0, 5)); // Get last 5 lessons
            setLoading(false);
        } catch (error) {
            console.error('Error fetching recent progress:', error);
            setLoading(false);
        }
    };

    const formatTime = (seconds) => {
        const hours = Math.floor(seconds / 3600);
        const minutes = Math.floor((seconds % 3600) / 60);
        const secs = seconds % 60;

        if (hours > 0) {
            return `${hours}h ${minutes}m`;
        } else if (minutes > 0) {
            return `${minutes}m ${secs}s`;
        } else {
            return `${secs}s`;
        }
    };

    const getAccuracyColor = (accuracy) => {
        if (accuracy >= 80) return '#4CAF50';
        if (accuracy >= 60) return '#FF9800';
        return '#F44336';
    };

    if (loading) {
        return <div className="progress-loading">Loading your progress...</div>;
    }

    return (
        <div className="progress-tracker">
            <h2>Your Learning Progress</h2>
            
            {stats && (
                <div className="stats-overview">
                    <div className="stat-card">
                        <h3>{stats.completedLessons || 0}</h3>
                        <p>Lessons Completed</p>
                    </div>
                    <div className="stat-card">
                        <h3>{stats.averageAccuracy ? `${stats.averageAccuracy.toFixed(1)}%` : '0%'}</h3>
                        <p>Average Accuracy</p>
                    </div>
                    <div className="stat-card">
                        <h3>{formatTime(stats.totalTimeSpent || 0)}</h3>
                        <p>Total Practice Time</p>
                    </div>
                </div>
            )}

            <div className="recent-progress">
                <h3>Recent Lessons</h3>
                {recentProgress.length > 0 ? (
                    <div className="progress-list">
                        {recentProgress.map((progress, index) => (
                            <div key={index} className="progress-item">
                                <div className="progress-info">
                                    <h4>{progress.lessonTitle}</h4>
                                    <p className="progress-status">
                                        Status: <span className={`status-${progress.status.toLowerCase()}`}>
                                            {progress.status.replace('_', ' ')}
                                        </span>
                                    </p>
                                </div>
                                
                                {progress.status === 'COMPLETED' && (
                                    <div className="progress-metrics">
                                        <div className="metric">
                                            <span 
                                                className="accuracy-badge"
                                                style={{ backgroundColor: getAccuracyColor(progress.accuracyScore) }}
                                            >
                                                {progress.accuracyScore}%
                                            </span>
                                        </div>
                                        <div className="metric">
                                            <span>⏱️ {formatTime(progress.timeSpent)}</span>
                                        </div>
                                        <div className="metric">
                                            <span>🔄 {progress.attempts} attempts</span>
                                        </div>
                                    </div>
                                )}
                                
                                <div className="progress-date">
                                    {new Date(progress.updatedAt).toLocaleDateString()}
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <p className="no-progress">No lessons completed yet. Start your first lesson!</p>
                )}
            </div>
        </div>
    );
};

export default ProgressTracker;
