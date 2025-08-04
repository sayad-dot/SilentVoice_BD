import apiClient from './apiClient';
import { setToken, removeToken } from '../utils/tokenStorage';

class AuthService {
  async login(email, password) {
    try {
      console.log('Attempting login to: /api/auth/login');
      
      const response = await apiClient.post('/api/auth/login', { // FIXED: Changed from '/auth/login'
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
      
      const response = await apiClient.post('/api/auth/register', { // FIXED: Changed from '/auth/register'
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

  logout() {
    removeToken();
    localStorage.removeItem('userFullName');
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
          fullName: localStorage.getItem('userFullName') || payload.sub
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

