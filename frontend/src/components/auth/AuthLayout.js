import React from 'react';
import '../../styles/auth.css';

const AuthLayout = ({ children, title, subtitle }) => {
  return (
    <div className="auth-container">
      {/* Animated Background */}
      <div className="auth-background">
        <div className="gradient-orb orb-1"></div>
        <div className="gradient-orb orb-2"></div>
        <div className="gradient-orb orb-3"></div>
        <div className="floating-particles">
          {[...Array(20)].map((_, i) => (
            <div key={i} className={`particle particle-${i % 5}`}></div>
          ))}
        </div>
      </div>

      {/* Main Content */}
      <div className="auth-content">
        <div className="auth-card">
          {/* Header Section */}
          <div className="auth-header">
            <div className="auth-logo">
              <div className="logo-icon">
                🤟
              </div>
              <h1>SilentVoice_BD</h1>
            </div>
            <div className="auth-subtitle">
              <h2>{title}</h2>
              <p>{subtitle}</p>
            </div>
          </div>

          {/* Form Content */}
          <div className="auth-form-container">
            {children}
          </div>

          {/* Footer */}
          <div className="auth-footer">
            <div className="brand-tagline">
              <span>🎯 AI-Powered Bangla Sign Language Recognition</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AuthLayout;
