import React from 'react';

const ProgressBar = ({ progress, fileName, status = 'uploading' }) => {
  const getStatusIcon = () => {
    switch (status) {
      case 'uploading':
        return '⏫';
      case 'success':
        return '✅';
      case 'error':
        return '❌';
      default:
        return '⏫';
    }
  };

  const getStatusText = () => {
    switch (status) {
      case 'uploading':
        return `Uploading... ${Math.round(progress)}%`;
      case 'success':
        return 'Upload completed successfully!';
      case 'error':
        return 'Upload failed. Please try again.';
      default:
        return `Uploading... ${Math.round(progress)}%`;
    }
  };

  return (
    <div className="progress-container">
      <div className="progress-header">
        <div className="file-info">
          <span className="status-icon">{getStatusIcon()}</span>
          <span className="file-name">{fileName}</span>
        </div>
        <span className="progress-text">{getStatusText()}</span>
      </div>

      <div className="progress-bar">
        <div
          className={`progress-fill ${status}`}
          style={{ width: `${progress}%` }}
        />
      </div>
    </div>
  );
};

export default ProgressBar;
