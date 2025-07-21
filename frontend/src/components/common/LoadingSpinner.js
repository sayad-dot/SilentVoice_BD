import React from 'react';

const LoadingSpinner = ({ size = 'medium', message = '' }) => {
  const sizeClass = `spinner-${size}`;

  return (
    <div className="loading-container">
      <div className={`spinner ${sizeClass}`}>
        <div className="spinner-circle"></div>
      </div>
      {message && <p className="loading-message">{message}</p>}
    </div>
  );
};

export default LoadingSpinner;
