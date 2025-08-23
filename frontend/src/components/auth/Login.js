// components/auth/Login.js - MERGED VERSION WITH NAVIGATION
import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
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

  // Navigation hooks
  const navigate = useNavigate();
  const location = useLocation();

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
    if (result.success) {
      // Handle remember me functionality
      if (rememberMe) {
        localStorage.setItem('rememberMe', 'true');
        localStorage.setItem('lastEmail', formData.email);
      } else {
        localStorage.removeItem('rememberMe');
        localStorage.removeItem('lastEmail');
      }

      // Redirect based on user role
      const from = location.state?.from?.pathname || '/';
      navigate(from, { replace: true });
    } else {
      console.error('Login failed:', result.error);
    }
  };

  // Updated Google login handler with navigation
  const handleGoogleSuccess = async (user, token) => {
    console.log('✅ Google login successful:', user.email);
    
    // Optional: Handle remember me for Google login
    if (rememberMe) {
      localStorage.setItem('rememberMe', 'true');
      localStorage.setItem('lastEmail', user.email);
    }

    // Redirect based on role or previous location
    const from = location.state?.from?.pathname || '/';
    const isAdmin = user.roles?.includes('ADMIN');
    
    if (isAdmin) {
      navigate('/admin');
    } else {
      navigate(from, { replace: true });
    }
  };

  const handleGoogleError = (errorMessage) => {
    console.error('❌ Google login failed:', errorMessage);
  };

  // Load remembered email on component mount
  useEffect(() => {
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

        {/* FORM OPTIONS */}
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
              <div className="btn-icon"></div>
            </>
          )}
        </button>

        {/* DIVIDER */}
        <div className="divider">
          <span>or continue with</span>
        </div>

        {/* SOCIAL LOGIN */}
        <div className="social-login">
          <GoogleLoginButton 
            mode="login"
            onSuccess={handleGoogleSuccess}
            onError={handleGoogleError}
            disabled={loading}
          />
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
