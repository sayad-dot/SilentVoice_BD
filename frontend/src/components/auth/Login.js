import React, { useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { getValidationErrors } from '../../utils/validation';
import AuthLayout from './AuthLayout';
import LoadingSpinner from '../common/LoadingSpinner';
import ErrorMessage from '../common/ErrorMessage';

const Login = ({ onSwitchToRegister }) => {
  const { login, loading, error, clearError } = useAuth();
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  });
  const [formErrors, setFormErrors] = useState({});

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

      subtitle="Sign in to access your dashboard"
    >
      <form onSubmit={handleSubmit} className="auth-form">
        {error && <ErrorMessage message={error} />}

        <div className="form-group">
          <label htmlFor="email">Email Address</label>
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
          <label htmlFor="password">Password</label>
          <input
            type="password"
            id="password"
            name="password"
            value={formData.password}
            onChange={handleInputChange}
            className={formErrors.password ? 'error' : ''}
            placeholder="Enter your password"
            disabled={loading}
          />
          {formErrors.password && <span className="error-text">{formErrors.password}</span>}
        </div>

        <button
          type="submit"
          className="auth-btn primary"
          disabled={loading}
        >
          {loading ? <LoadingSpinner size="small" /> : 'Sign In'}
        </button>
      </form>

      <div className="auth-footer">
        <p>
          Don't have an account?
          <button
            type="button"
            onClick={onSwitchToRegister}
            className="link-button"
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
