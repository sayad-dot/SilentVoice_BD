// components/auth/GoogleLoginButton.js - FIXED VERSION
import React, { useEffect, useState, useRef } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import googleAuthService from '../../services/googleAuthService';

// Official Google Logo SVG (follows Google branding guidelines)
const GoogleIcon = () => (
  <svg
    width="20"
    height="20"
    viewBox="0 0 24 24"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
      fill="#4285F4"
    />
    <path
      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
      fill="#34A853"
    />
    <path
      d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
      fill="#FBBC05"
    />
    <path
      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
      fill="#EA4335"
    />
  </svg>
);

// Loading spinner component
const LoadingSpinner = () => (
  <div
    style={{
      width: '18px',
      height: '18px',
      border: '2px solid #ffffff40',
      borderTop: '2px solid #ffffff',
      borderRadius: '50%',
      animation: 'spin 1s linear infinite'
    }}
  />
);

const GoogleLoginButton = ({ mode = 'login', onSuccess, onError, disabled }) => {
  const { loginWithGoogle, registerWithGoogle } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const [isReady, setIsReady] = useState(false);
  const buttonRef = useRef(null);

  useEffect(() => {
    const loadGoogle = async () => {
      try {
        await googleAuthService.initialize();
        setIsReady(true);
      } catch (error) {
        console.error('Google load failed:', error);
        onError?.('Google Sign-In not available. Please try refreshing the page.');
      }
    };

    loadGoogle();
  }, [onError]);

  const handleClick = async (e) => {
    e.preventDefault();
    
    if (!isReady || isLoading || disabled) return;
    
    setIsLoading(true);

    try {
      const userData = await googleAuthService.signIn();
      const authMethod = mode === 'register' ? registerWithGoogle : loginWithGoogle;
      const result = await authMethod(userData);
      
      if (result.success) {
        onSuccess?.(userData, result.token);
      } else {
        throw new Error(result.error || 'Authentication failed');
      }
    } catch (error) {
      console.error('Google authentication error:', error);
      let message = 'Google Sign-In failed';
      
      if (error.message?.includes('popup_closed')) {
        message = 'Sign-In cancelled';
      } else if (error.message?.includes('popup_blocked')) {
        message = 'Popup blocked. Please allow popups for this site.';
      } else if (error.message?.includes('network')) {
        message = 'Network error. Please check your connection.';
      }
      
      onError?.(message);
    } finally {
      setIsLoading(false);
    }
  };

  const buttonText = () => {
    if (isLoading) return mode === 'register' ? 'Signing up...' : 'Signing in...';
    if (!isReady) return 'Loading...';
    return mode === 'register' ? 'Sign up with Google' : 'Sign in with Google';
  };

  return (
    <>
      <button
        ref={buttonRef}
        type="button"
        onClick={handleClick}
        disabled={!isReady || disabled || isLoading}
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: '12px',
          width: '100%',
          padding: '12px 16px',
          backgroundColor: '#fff',
          color: '#3c4043',
          border: '1px solid #dadce0',
          borderRadius: '8px',
          fontSize: '14px',
          fontWeight: '500',
          fontFamily: 'Roboto, -apple-system, BlinkMacSystemFont, sans-serif',
          cursor: isReady && !disabled && !isLoading ? 'pointer' : 'not-allowed',
          opacity: !isReady || disabled || isLoading ? 0.6 : 1,
          transition: 'all 0.2s ease',
          minHeight: '48px',
          boxShadow: '0 1px 2px 0 rgba(60,64,67,0.30), 0 1px 3px 1px rgba(60,64,67,0.15)'
        }}
        onMouseEnter={(e) => {
          if (isReady && !disabled && !isLoading) {
            e.target.style.backgroundColor = '#f8f9fa';
            e.target.style.boxShadow = '0 1px 3px 0 rgba(60,64,67,0.30), 0 4px 8px 3px rgba(60,64,67,0.15)';
          }
        }}
        onMouseLeave={(e) => {
          if (isReady && !disabled && !isLoading) {
            e.target.style.backgroundColor = '#fff';
            e.target.style.boxShadow = '0 1px 2px 0 rgba(60,64,67,0.30), 0 1px 3px 1px rgba(60,64,67,0.15)';
          }
        }}
      >
        {isLoading ? <LoadingSpinner /> : <GoogleIcon />}
        <span>{buttonText()}</span>
      </button>
      
      <style jsx="true" global="true">{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
        
        .google-auth-button:active {
          transform: translateY(1px) !important;
          box-shadow: 0 1px 1px 0 rgba(60,64,67,0.30) !important;
        }
      `}</style>
    </>
  );
};

export default GoogleLoginButton;
