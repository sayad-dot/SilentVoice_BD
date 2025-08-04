import React, { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { getValidationErrors } from '../../utils/validation';
import AuthLayout from './AuthLayout';
import LoadingSpinner from '../common/LoadingSpinner';
import ErrorMessage from '../common/ErrorMessage';
import { FaGoogle, FaFacebookF } from 'react-icons/fa';
const Login = ({ onSwitchToRegister }) => {
  const { login, loading, error, clearError } = useAuth();
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [formErrors, setFormErrors] = useState({});
  const [showPassword, setShowPassword] = useState(false);

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
    }
  };

  return (
    <AuthLayout 
      title="Welcome Back" 
      subtitle="Sign in to continue your sign language journey"
    >
      <form onSubmit={handleSubmit} className="auth-form">
        {error && <ErrorMessage message={error} />}
        
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
            <div className="input-icon">
              📧
            </div>
          </div>
          {formErrors.email && <span className="error-text">{formErrors.email}</span>}
        </div>

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
            <div className="input-icon">
              🔒
            </div>
            <button
              type="button"
              className="password-toggle"
              onClick={() => setShowPassword(!showPassword)}
            >
              {showPassword ? '🙈' : '👁️'}
            </button>
          </div>
          {formErrors.password && <span className="error-text">{formErrors.password}</span>}
        </div>

        <div className="form-options">
          <label className="checkbox-container">
            <input type="checkbox" />
            <span className="checkmark"></span>
            Remember me
          </label>
          <a href="#" className="forgot-link">Forgot password?</a>
        </div>

        <button 
          type="submit" 
          className="auth-btn primary" 
          disabled={loading}
        >
          {loading ? <LoadingSpinner size="small" /> : (
            <>
              <span>Sign In</span>
              <div className="btn-icon">🚀</div>
            </>
          )}
        </button>

        <div className="divider">
          <span>or continue with</span>
        </div>

<div className="social-login">
  <button type="button" className="social-btn google">
    <FaGoogle className="social-icon" />
  </button>
  <button type="button" className="social-btn facebook">
    <FaFacebookF className="social-icon" />
  </button>
</div>
      </form>

      <div className="auth-footer">
        <p>
          Don't have an account?{' '}
          <button 
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
