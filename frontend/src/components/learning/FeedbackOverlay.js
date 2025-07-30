import React from 'react';
import '../styles/learning/mirror.css';

const FeedbackOverlay = ({ 
    confidenceScore, 
    feedbackText, 
    isCorrect, 
    improvementTips, 
    isVisible 
}) => {
    if (!isVisible) return null;

    const getConfidenceColor = (score) => {
        if (score >= 80) return '#4CAF50';
        if (score >= 60) return '#FF9800';
        if (score >= 40) return '#FFC107';
        return '#F44336';
    };

    const getConfidenceLabel = (score) => {
        if (score >= 80) return 'Excellent';
        if (score >= 60) return 'Good';
        if (score >= 40) return 'Fair';
        return 'Keep Trying';
    };

    return (
        <div className="feedback-overlay">
            <div className="feedback-header">
                <div className="confidence-section">
                    <div 
                        className="confidence-circle"
                        style={{ 
                            background: `conic-gradient(${getConfidenceColor(confidenceScore)} ${confidenceScore * 3.6}deg, #e0e0e0 0deg)`
                        }}
                    >
                        <div className="confidence-inner">
                            <span className="confidence-number">{Math.round(confidenceScore)}%</span>
                            <span className="confidence-label">{getConfidenceLabel(confidenceScore)}</span>
                        </div>
                    </div>
                </div>

                <div className="status-indicator">
                    <div className={`status-icon ${isCorrect ? 'correct' : 'incorrect'}`}>
                        {isCorrect ? '✓' : '!'}
                    </div>
                </div>
            </div>

            <div className="feedback-content">
                <div className="feedback-text">
                    <p>{feedbackText}</p>
                </div>

                {improvementTips && (
                    <div className="improvement-tips">
                        <h4>💡 Tips for improvement:</h4>
                        <p>{improvementTips}</p>
                    </div>
                )}

                <div className="confidence-bar">
                    <div className="bar-background">
                        <div 
                            className="bar-fill"
                            style={{ 
                                width: `${confidenceScore}%`,
                                backgroundColor: getConfidenceColor(confidenceScore)
                            }}
                        />
                    </div>
                </div>
            </div>
        </div>
    );
};

export default FeedbackOverlay;
