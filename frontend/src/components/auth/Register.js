import React, { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { getValidationErrors } from '../../utils/validation';
import AuthLayout from './AuthLayout';
import LoadingSpinner from '../common/LoadingSpinner';
import ErrorMessage from '../common/ErrorMessage';

const Register = ({ onSwitchToLogin, onRegistrationSuccess }) => {
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
    const errors = getValidationErrors(formData, false);
    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    // Clear previous errors
    setFormErrors({});

    // Attempt registration
    const result = await register(formData);
    if (result.success) {
      setSuccess(true);
      setTimeout(() => {
        onRegistrationSuccess();
      }, 2000);
    }
  };

  if (success) {
    return (
      <AuthLayout
        title="Registration Successful!"
        subtitle="Your account has been created successfully"
      >
        <div className="success-message">
          <div className="success-icon">✅</div>
          <h3>Welcome to SilentVoice_BD!</h3>
          <p>You can now sign in with your credentials.</p>
          <p>Redirecting to login...</p>
        </div>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout

      subtitle=" Start Using SilentVoice_BD"
    >
      <form onSubmit={handleSubmit} className="auth-form">
        {error && <ErrorMessage message={error} />}

        <div className="form-group">
          <label htmlFor="fullName">Full Name *</label>
          <input
            type="text"
            id="fullName"
            name="fullName"
            value={formData.fullName}
            onChange={handleInputChange}
            className={formErrors.fullName ? 'error' : ''}
            placeholder="Enter your full name"
            disabled={loading}
          />
          {formErrors.fullName && <span className="error-text">{formErrors.fullName}</span>}
        </div>

        <div className="form-group">
          <label htmlFor="email">Email Address *</label>
          <input
            type="email"
            id="email"
            name="email"
            value={formData.email}
            onChange={handleInputChange}
            className={formErrors.email ? 'error' : ''}
            placeholder="Enter your email"
            disabled={loading}
          />
          {formErrors.email && <span className="error-text">{formErrors.email}</span>}
        </div>

        <div className="form-group">
          <label htmlFor="password">Password *</label>
          <input
            type="password"
            id="password"
            name="password"
            value={formData.password}
            onChange={handleInputChange}
            className={formErrors.password ? 'error' : ''}
            placeholder="Choose a secure password (min 6 characters)"
            disabled={loading}
          />
          {formErrors.password && <span className="error-text">{formErrors.password}</span>}
        </div>

        <div className="form-group">
          <label htmlFor="phone">Phone Number (optional)</label>
          <input
            type="tel"
            id="phone"
            name="phone"
            value={formData.phone}
            onChange={handleInputChange}
            className={formErrors.phone ? 'error' : ''}
            placeholder="Enter your phone number"
            disabled={loading}
          />
          {formErrors.phone && <span className="error-text">{formErrors.phone}</span>}
        </div>

        <div className="form-group">
          <label htmlFor="dateOfBirth">Date of Birth (optional)</label>
          <input
            type="date"
            id="dateOfBirth"
            name="dateOfBirth"
            value={formData.dateOfBirth}
            onChange={handleInputChange}
            disabled={loading}
          />
        </div>

        <button
          type="submit"
          className="auth-btn primary"
          disabled={loading}
        >
          {loading ? <LoadingSpinner size="small" /> : 'Create Account'}
        </button>
      </form>

      <div className="auth-footer">
        <p>
          Already have an account?
          <button
            type="button"
            onClick={onSwitchToLogin}
            className="link-button"
            disabled={loading}
          >
            Sign in here
          </button>
        </p>
      </div>
    </AuthLayout>
  );
};

export default Register;
