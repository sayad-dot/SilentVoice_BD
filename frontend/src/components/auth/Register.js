import React, { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { getValidationErrors } from '../../utils/validation';
import AuthLayout from './AuthLayout';
import LoadingSpinner from '../common/LoadingSpinner';
import ErrorMessage from '../common/ErrorMessage';
import { FaGoogle, FaFacebookF } from 'react-icons/fa';

const Register = ({ onSwitchToLogin }) => {
  const { register, loading, error, clearError } = useAuth();
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    phone: '',
    dateOfBirth: ''
  });
  const [formErrors, setFormErrors] = useState({});
  const [success, setSuccess] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));

    if (formErrors[name]) setFormErrors(prev => ({ ...prev, [name]: '' }));
    if (error) clearError();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errors = getValidationErrors(formData, false);
    if (Object.keys(errors).length) return setFormErrors(errors);

    setFormErrors({});
    const result = await register(formData);
    if (result.success) {
      setSuccess(true);
      setTimeout(() => onSwitchToLogin && onSwitchToLogin(), 2_000);
    }
  };

  if (success) {
    return (
      <AuthLayout title="Welcome to SilentVoice_BD!" subtitle="Your account has been created successfully">
        <div className="success-message">
          <div className="success-icon">🎉</div>
          <h3>Registration Successful!</h3>
          <p>You can now sign in with your credentials.</p>
          <p>Redirecting to login...</p>
          <div className="success-animation">
            <div className="pulse-ring"></div>
            <div className="pulse-ring"></div>
            <div className="pulse-ring"></div>
          </div>
        </div>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout title="Create Account" >
      <form onSubmit={handleSubmit} className="auth-form">
        {error && <ErrorMessage message={error} />}

        {/* FULL NAME */}
        <div className="form-group">
          <div className="input-container">
            <input
              type="text"
              name="fullName"
              value={formData.fullName}
              onChange={handleInputChange}
              className={`form-input ${formErrors.fullName ? 'error' : ''} ${formData.fullName ? 'filled' : ''}`}
              placeholder=" "
              required
            />
            <label className="form-label">Full Name *</label>
            <div className="input-icon">👤</div>
          </div>
          {formErrors.fullName && <span className="error-text">{formErrors.fullName}</span>}
        </div>

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
            <label className="form-label">Email Address *</label>
            <div className="input-icon">📧</div>
          </div>
          {formErrors.email && <span className="error-text">{formErrors.email}</span>}
        </div>

        {/* PASSWORD */}
        <div className="form-group">
          <div className="input-container">
            <input
              type={showPassword ? 'text' : 'password'}
              name="password"
              value={formData.password}
              onChange={handleInputChange}
              className={`form-input ${formErrors.password ? 'error' : ''} ${formData.password ? 'filled' : ''}`}
              placeholder=" "
              required
            />
            <label className="form-label">Password *</label>
            <div className="input-icon">🔒</div>
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

        {/* PHONE (stacked) */}
        <div className="form-group">
         
          <div className="input-container simple-input">
            <input
              type="tel"
              name="phone"
              value={formData.phone}
              onChange={handleInputChange}
              className="form-input phone-input"
              placeholder="e.g. +880 123 456 7890"
            />
          </div>
          {formErrors.phone && <span className="error-text">{formErrors.phone}</span>}
        </div>

        {/* DATE OF BIRTH (stacked) */}
        <div className="form-group">
          
          <div className="input-container simple-input">
            <input
              type="date"
              name="dateOfBirth"
              value={formData.dateOfBirth}
              onChange={handleInputChange}
              className="form-input date-input"
            />
          </div>
          {formErrors.dateOfBirth && <span className="error-text">{formErrors.dateOfBirth}</span>}
        </div>

        <button type="submit" className="auth-btn primary" disabled={loading}>
          {loading ? <LoadingSpinner size="small" /> : <> <span>Create Account</span><div className="btn-icon">✨</div> </>}
        </button>

        <div className="divider"><span>or continue with</span></div>

        <div className="social-login">
          <button type="button" className="social-btn google"><FaGoogle className="social-icon" /></button>
          <button type="button" className="social-btn facebook"><FaFacebookF className="social-icon" /></button>
        </div>
      </form>

      <div className="auth-footer">
        <p>
          Already have an account?{' '}
          <button className="link-button" onClick={onSwitchToLogin} disabled={loading}>
            Sign in here
          </button>
        </p>
      </div>
    </AuthLayout>
  );
};

export default Register;

