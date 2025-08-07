// services/googleAuthService.js - FIXED VERSION
import { initializeGoogleAuth } from '../utils/googleAuth';

const GOOGLE_CLIENT_ID = process.env.REACT_APP_GOOGLE_CLIENT_ID;

class GoogleAuthService {
  constructor() {
    this.google = null;
    this.isInitialized = false;
  }

  async initialize() {
    if (this.isInitialized && this.google) {
      return this.google;
    }

    try {
      console.log('🔐 Loading Google Identity Services...');
      this.google = await initializeGoogleAuth();
      this.isInitialized = true;
      return this.google;
    } catch (error) {
      console.error('❌ Google init failed:', error);
      throw error;
    }
  }

  async signIn() {
    await this.initialize();
    
    return new Promise((resolve, reject) => {
      try {
        // 🔥 CRITICAL FIX: Disable FedCM to avoid CORS issues
        this.google.accounts.id.initialize({
          client_id: GOOGLE_CLIENT_ID,
          callback: (response) => {
            try {
              const userData = this.parseCredential(response.credential);
              resolve(userData);
            } catch (error) {
              reject(error);
            }
          },
          // ⭐ This is the key fix for CORS/FedCM 403 errors
          use_fedcm_for_prompt: false,
          cancel_on_tap_outside: false
        });

        // Use renderButton instead of prompt() to avoid FedCM
        // Create a temporary hidden container
        const tempContainer = document.createElement('div');
        tempContainer.id = 'temp-google-signin-' + Date.now();
        tempContainer.style.position = 'absolute';
        tempContainer.style.top = '-9999px';
        tempContainer.style.left = '-9999px';
        tempContainer.style.width = '1px';
        tempContainer.style.height = '1px';
        tempContainer.style.overflow = 'hidden';
        document.body.appendChild(tempContainer);
        
        this.google.accounts.id.renderButton(tempContainer, {
          type: 'standard',
          theme: 'outline',
          size: 'large',
          text: 'signin_with',
          shape: 'rectangular'
        });
        
        // Automatically trigger the button click
        setTimeout(() => {
          const button = tempContainer.querySelector('div[role="button"]');
          if (button) {
            button.click();
          } else {
            // Fallback: use One Tap if renderButton fails
            this.google.accounts.id.prompt((notification) => {
              if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
                reject(new Error('Google Sign-In was not displayed or skipped'));
              }
            });
          }
          
          // Clean up temp container after 5 seconds
          setTimeout(() => {
            if (document.body.contains(tempContainer)) {
              document.body.removeChild(tempContainer);
            }
          }, 5000);
        }, 100);
        
      } catch (error) {
        reject(error);
      }
    });
  }

  async signOut() {
    if (this.google && this.google.accounts) {
      try {
        this.google.accounts.id.disableAutoSelect();
        console.log('Google sign-out completed');
      } catch (error) {
        console.warn('Google sign-out error:', error);
      }
    }
  }

  parseCredential(credential) {
    try {
      const base64Url = credential.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map(function (c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
          })
          .join('')
      );

      const tokenData = JSON.parse(jsonPayload);
      
      return {
        googleId: tokenData.sub,
        email: tokenData.email,
        fullName: tokenData.name,
        firstName: tokenData.given_name,
        lastName: tokenData.family_name,
        profilePicture: tokenData.picture,
        idToken: credential
      };
    } catch (error) {
      throw new Error('Invalid token format');
    }
  }
}

const googleAuthService = new GoogleAuthService();
export default googleAuthService;
