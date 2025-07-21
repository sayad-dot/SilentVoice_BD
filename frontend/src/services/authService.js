import apiClient from './apiClient';
import { setToken, removeToken } from '../utils/tokenStorage';

class AuthService {
  async login(email, password) {
    try {
      const response = await apiClient.post('/auth/login', {
        email,
        password
      });

      const { token, fullName } = response.data;

      // Store JWT token
      setToken(token);

      return { token, fullName };
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  }

  async register(userData) {
    try {
      const response = await apiClient.post('/auth/register', {
        fullName: userData.fullName,
        email: userData.email,
        password: userData.password,
        phone: userData.phone || null,
        dateOfBirth: userData.dateOfBirth || null
      });

      return response.data;
    } catch (error) {
      console.error('Registration error:', error);
      throw error;
    }
  }

  logout() {
    removeToken();
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
      this.logout();
      return null;
    }
  }
}

export default new AuthService();
