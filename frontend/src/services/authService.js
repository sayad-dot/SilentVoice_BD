import apiClient from './apiClient';
import { setToken, removeToken } from '../utils/tokenStorage';

class AuthService {
  async login(email, password) {
    try {
      console.log('Attempting login to: /api/auth/login');
      const response = await apiClient.post('/api/auth/login', {
        email,
        password
      });
      console.log('Login response:', response.data);

      const { token, fullName } = response.data;

      // Store JWT token
      setToken(token);
      // Store user full name for UI display
      localStorage.setItem('userFullName', fullName);

      return { token, fullName };
    } catch (error) {
      console.error('Login error:', error);
      // Better error handling
      if (error.response) {
        const message = error.response.data || 'Login failed';
        throw new Error(typeof message === 'string' ? message : 'Invalid credentials');
      } else if (error.request) {
        throw new Error('Network error: Unable to reach server');
      } else {
        throw new Error('Login failed: ' + error.message);
      }
    }
  }

  async register(userData) {
    try {
      console.log('Attempting register to: /api/auth/register');
      const response = await apiClient.post('/api/auth/register', {
        fullName: userData.fullName,
        email: userData.email,
        password: userData.password,
        phone: userData.phone || null,
        dateOfBirth: userData.dateOfBirth || null
      });
      console.log('Registration successful:', response.data);
      return response.data;
    } catch (error) {
      console.error('Registration error:', error);
      // Better error handling
      if (error.response) {
        const message = error.response.data || 'Registration failed';
        throw new Error(typeof message === 'string' ? message : 'Registration failed');
      } else if (error.request) {
        throw new Error('Network error: Unable to reach server');
      } else {
        throw new Error('Registration failed: ' + error.message);
      }
    }
  }

  // NEW: Google Login
  async googleLogin(googleUserData) {
    try {
      console.log('🔐 Attempting Google login to: /api/auth/google/login');
      const response = await apiClient.post('/api/auth/google/login', {
        googleId: googleUserData.googleId,
        email: googleUserData.email,
        fullName: googleUserData.fullName,
        firstName: googleUserData.firstName,
        lastName: googleUserData.lastName,
        profilePicture: googleUserData.profilePicture,
        idToken: googleUserData.idToken
      });

      console.log('✅ Google login response:', response.data);

      const { token, fullName } = response.data;

      // Store JWT token
      setToken(token);
      // Store user full name for UI display
      localStorage.setItem('userFullName', fullName);
      // Store profile picture if available
      if (googleUserData.profilePicture) {
        localStorage.setItem('userProfilePicture', googleUserData.profilePicture);
      }

      return { token, fullName, profilePicture: googleUserData.profilePicture };
    } catch (error) {
      console.error('❌ Google login error:', error);
      // Better error handling
      if (error.response) {
        const message = error.response.data || 'Google login failed';
        if (error.response.status === 404) {
          throw new Error('Account not found. Please sign up with Google instead.');
        }
        throw new Error(typeof message === 'string' ? message : 'Google login failed');
      } else if (error.request) {
        throw new Error('Network error: Unable to reach server');
      } else {
        throw new Error('Google login failed: ' + error.message);
      }
    }
  }

  // NEW: Google Registration
  async googleRegister(googleUserData) {
    try {
      console.log('🔐 Attempting Google registration to: /api/auth/google/register');
      const response = await apiClient.post('/api/auth/google/register', {
        googleId: googleUserData.googleId,
        email: googleUserData.email,
        fullName: googleUserData.fullName,
        firstName: googleUserData.firstName,
        lastName: googleUserData.lastName,
        profilePicture: googleUserData.profilePicture,
        idToken: googleUserData.idToken
      });

      console.log('✅ Google registration response:', response.data);

      const { token, fullName } = response.data;

      // Store JWT token
      setToken(token);
      // Store user full name for UI display
      localStorage.setItem('userFullName', fullName);
      // Store profile picture if available
      if (googleUserData.profilePicture) {
        localStorage.setItem('userProfilePicture', googleUserData.profilePicture);
      }

      return { token, fullName, profilePicture: googleUserData.profilePicture };
    } catch (error) {
      console.error('❌ Google registration error:', error);
      // Better error handling
      if (error.response) {
        const message = error.response.data || 'Google registration failed';
        if (error.response.status === 409) {
          throw new Error('Email already registered. Please sign in with Google instead.');
        }
        throw new Error(typeof message === 'string' ? message : 'Google registration failed');
      } else if (error.request) {
        throw new Error('Network error: Unable to reach server');
      } else {
        throw new Error('Google registration failed: ' + error.message);
      }
    }
  }

  logout() {
    removeToken();
    localStorage.removeItem('userFullName');
    localStorage.removeItem('userProfilePicture');
  }

  getCurrentUser() {
    const token = localStorage.getItem('token');
    if (!token) return null;

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const currentTime = Date.now() / 1000;

      if (payload.exp > currentTime) {
        return {
          email: payload.sub,
          fullName: localStorage.getItem('userFullName') || payload.sub,
          profilePicture: localStorage.getItem('userProfilePicture') || null
        };
      } else {
        this.logout();
        return null;
      }
    } catch (error) {
      console.error('Token parsing error:', error);
      this.logout();
      return null;
    }
  }

  isAuthenticated() {
    return this.getCurrentUser() !== null;
  }
}

export default new AuthService();
