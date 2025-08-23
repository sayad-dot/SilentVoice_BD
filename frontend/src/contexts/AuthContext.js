import React, { createContext, useState, useContext, useEffect } from 'react';
import authService from '../services/authService';
import googleAuthService from '../services/googleAuthService';
import { getToken, removeToken } from '../utils/tokenStorage';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

// Helper function to decode JWT and extract roles
const parseJwtPayload = (token) => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload;
  } catch (error) {
    console.error('Error parsing JWT:', error);
    return null;
  }
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
          const payload = parseJwtPayload(token);
          const currentTime = Date.now() / 1000;
          
          if (payload && payload.exp > currentTime) {
            // Token is valid, extract user info including roles
            setUser({
              email: payload.sub,
              fullName: localStorage.getItem('userFullName') || payload.sub,
              profilePicture: localStorage.getItem('userProfilePicture') || null,
              roles: payload.roles || [] // Extract roles from JWT
            });
          } else {
            // Token expired
            removeToken();
            localStorage.removeItem('userFullName');
            localStorage.removeItem('userProfilePicture');
          }
        } catch (err) {
          console.error('Token validation error:', err);
          removeToken();
          localStorage.removeItem('userFullName');
          localStorage.removeItem('userProfilePicture');
        }
      }
      setLoading(false);
    };

    initAuth();
  }, []);

  // Helper function to update user from JWT token
  const updateUserFromToken = (token, additionalData = {}) => {
    const payload = parseJwtPayload(token);
    if (payload) {
      setUser({
        email: payload.sub,
        roles: payload.roles || [], // Extract roles from JWT
        ...additionalData
      });
    }
  };

  // Traditional login
  const login = async (email, password) => {
    try {
      setError('');
      setLoading(true);

      const response = await authService.login(email, password);
      
      // Update user with roles from JWT
      updateUserFromToken(response.token, {
        fullName: response.fullName,
        profilePicture: response.profilePicture || null
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

  // Google Login
  const loginWithGoogle = async (googleUserData) => {
    try {
      setError('');
      setLoading(true);

      console.log('🔐 Processing Google login in AuthContext...');
      const response = await authService.googleLogin(googleUserData);

      // Update user with roles from JWT
      updateUserFromToken(response.token, {
        email: googleUserData.email,
        fullName: response.fullName,
        profilePicture: response.profilePicture || null
      });

      console.log('✅ Google login successful in AuthContext');
      return { success: true };
    } catch (err) {
      const errorMessage = err.message || 'Google login failed';
      setError(errorMessage);
      console.error('❌ Google login failed in AuthContext:', errorMessage);
      return { success: false, error: errorMessage };
    } finally {
      setLoading(false);
    }
  };

  // Traditional registration
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

  // Google Registration
  const registerWithGoogle = async (googleUserData) => {
    try {
      setError('');
      setLoading(true);

      console.log('🔐 Processing Google registration in AuthContext...');
      const response = await authService.googleRegister(googleUserData);

      // Update user with roles from JWT
      updateUserFromToken(response.token, {
        email: googleUserData.email,
        fullName: response.fullName,
        profilePicture: response.profilePicture || null
      });

      console.log('✅ Google registration successful in AuthContext');
      return { success: true };
    } catch (err) {
      const errorMessage = err.message || 'Google registration failed';
      setError(errorMessage);
      console.error('❌ Google registration failed in AuthContext:', errorMessage);
      return { success: false, error: errorMessage };
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    try {
      // Sign out from Google if signed in
      await googleAuthService.signOut();
    } catch (error) {
      console.warn('Google sign out error:', error);
    }

    authService.logout();
    setUser(null);
    localStorage.removeItem('userFullName');
    localStorage.removeItem('userProfilePicture');
  };

  const clearError = () => setError('');

  const value = {
    user,
    loading,
    error,
    login,
    loginWithGoogle,
    register,
    registerWithGoogle,
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


