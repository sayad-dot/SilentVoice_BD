import React, { createContext, useState, useContext, useEffect } from 'react';
import authService from '../services/authService';
import { getToken, removeToken } from '../utils/tokenStorage';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const initAuth = async () => {
      const token = getToken();
      if (token) {
        try {
          // Validate token by checking if it's expired
          const payload = JSON.parse(atob(token.split('.')[1]));
          const currentTime = Date.now() / 1000;

          if (payload.exp > currentTime) {
            // Token is valid, extract user info
            setUser({
              email: payload.sub,
              fullName: localStorage.getItem('userFullName') || payload.sub
            });
          } else {
            // Token expired
            removeToken();
            localStorage.removeItem('userFullName');
          }
        } catch (err) {
          console.error('Token validation error:', err);
          removeToken();
          localStorage.removeItem('userFullName');
        }
      }
      setLoading(false);
    };

    initAuth();
  }, []);

  const login = async (email, password) => {
    try {
      setError('');
      setLoading(true);

      const response = await authService.login(email, password);

      setUser({
        email: email,
        fullName: response.fullName
      });

      // Store user's full name for display
      localStorage.setItem('userFullName', response.fullName);

      return { success: true };
    } catch (err) {
      const errorMessage = err.response?.data?.message || err.message || 'Login failed';
      setError(errorMessage);
      return { success: false, error: errorMessage };
    } finally {
      setLoading(false);
    }
  };

  const register = async (userData) => {
    try {
      setError('');
      setLoading(true);

      await authService.register(userData);
      return { success: true };
    } catch (err) {
      const errorMessage = err.response?.data?.message || err.message || 'Registration failed';
      setError(errorMessage);
      return { success: false, error: errorMessage };
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    authService.logout();
    setUser(null);
    localStorage.removeItem('userFullName');
  };

  const clearError = () => setError('');

  const value = {
    user,
    loading,
    error,
    login,
    register,
    logout,
    clearError,
    isAuthenticated: !!user
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
