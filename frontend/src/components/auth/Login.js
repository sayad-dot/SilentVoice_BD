// components/auth/Login.js - UPDATED VERSION
import React, { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { getValidationErrors } from '../../utils/validation';
import AuthLayout from './AuthLayout';
import LoadingSpinner from '../common/LoadingSpinner';
import ErrorMessage from '../common/ErrorMessage';
import GoogleLoginButton from './GoogleLoginButton';
import { FaFacebookF } from 'react-icons/fa';

const Login = ({ onSwitchToRegister }) => {
  const { login, loginWithGoogle, loading, error, clearError } = useAuth();
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [formErrors, setFormErrors] = useState({});
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));

    // Clear specific field error when user starts typing
    if (formErrors[name]) {
      setFormErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }

    // Clear general error
    if (error) {
      clearError();
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validate form
    const errors = getValidationErrors(formData, true);
    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    // Clear previous errors
    setFormErrors({});
    
    // Attempt login
    const result = await login(formData.email, formData.password);
    if (!result.success) {
      // Error is handled by AuthContext
      console.error('Login failed:', result.error);
    } else {
      // Handle remember me functionality if needed
      if (rememberMe) {
        localStorage.setItem('rememberMe', 'true');
        localStorage.setItem('lastEmail', formData.email);
      } else {
        localStorage.removeItem('rememberMe');
        localStorage.removeItem('lastEmail');
      }
    }
  };

  // 🔥 UPDATED: Better Google login handlers
  const handleGoogleSuccess = async (user, token) => {
    console.log('✅ Google login successful:', user.email);
    // The loginWithGoogle is already called automatically by GoogleLoginButton
    // Just handle any additional success logic here if needed
    
    // Optional: Handle remember me for Google login
    if (rememberMe) {
      localStorage.setItem('rememberMe', 'true');
      localStorage.setItem('lastEmail', user.email);
    }
  };

  const handleGoogleError = (errorMessage) => {
    console.error('❌ Google login failed:', errorMessage);
    // Error display is handled by AuthContext through the error state
    // But you can add additional error handling here if needed
  };

  // Load remembered email on component mount
  React.useEffect(() => {
    const remembered = localStorage.getItem('rememberMe');
    const lastEmail = localStorage.getItem('lastEmail');
    
    if (remembered === 'true' && lastEmail) {
      setFormData(prev => ({ ...prev, email: lastEmail }));
      setRememberMe(true);
    }
  }, []);

  return (
    <AuthLayout 
      title="Welcome Back" 
      subtitle="Sign in to continue your sign language journey"
    >
      <form onSubmit={handleSubmit} className="auth-form">
        {error && <ErrorMessage message={error} />}
        
        {/* EMAIL */}
        <div className="form-group">
          <div className="input-container">
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleInputChange}
              className={`form-input ${formErrors.email ? 'error' : ''} ${formData.email ? 'filled' : ''}`}
              placeholder=" "
              required
            />
            <label className="form-label">Email Address</label>
            <div className="input-icon">📧</div>
          </div>
          {formErrors.email && <span className="error-text">{formErrors.email}</span>}
        </div>

        {/* PASSWORD */}
        <div className="form-group">
          <div className="input-container">
            <input
              type={showPassword ? "text" : "password"}
              name="password"
              value={formData.password}
              onChange={handleInputChange}
              className={`form-input ${formErrors.password ? 'error' : ''} ${formData.password ? 'filled' : ''}`}
              placeholder=" "
              required
            />
            <label className="form-label">Password</label>
            <div className="input-icon">🔒</div>
            <button
              type="button"
              className="password-toggle"
              onClick={() => setShowPassword(!showPassword)}
              aria-label="Toggle password visibility"
            >
              {showPassword ? '🙈' : '👁️'}
            </button>
          </div>
          {formErrors.password && <span className="error-text">{formErrors.password}</span>}
        </div>

        {/* FORM OPTIONS - Updated */}
        <div className="form-options">
          <label className="checkbox-container">
            <input 
              type="checkbox" 
              checked={rememberMe}
              onChange={(e) => setRememberMe(e.target.checked)}
            />
            <span className="checkmark"></span>
            Remember me
          </label>
          <button 
            type="button" 
            className="forgot-link"
            onClick={() => {
              // TODO: Implement forgot password functionality
              console.log('Forgot password clicked');
            }}
          >
            Forgot password?
          </button>
        </div>

        {/* SUBMIT BUTTON */}
        <button 
          type="submit" 
          className="auth-btn primary" 
          disabled={loading}
        >
          {loading ? (
            <LoadingSpinner size="small" />
          ) : (
            <>
              <span>Sign In</span>
              <div className="btn-icon">🚀</div>
            </>
          )}
        </button>

        {/* DIVIDER */}
        <div className="divider">
          <span>or continue with</span>
        </div>

        {/* SOCIAL LOGIN - Updated */}
        <div className="social-login">
          <GoogleLoginButton 
            mode="login"
            onSuccess={handleGoogleSuccess}
            onError={handleGoogleError}
            disabled={loading}
          />
          {/* 
          <button type="button" className="social-btn facebook" disabled>
            <FaFacebookF className="social-icon" />
          </button>
          */}
        </div>
      </form>

      {/* FOOTER */}
      <div className="auth-footer">
        <p>
          Don't have an account?{' '}
          <button 
            type="button"
            className="link-button" 
            onClick={onSwitchToRegister}
            disabled={loading}
          >
            Register here
          </button>
        </p>
      </div>
    </AuthLayout>
  );
};

export default Login;
