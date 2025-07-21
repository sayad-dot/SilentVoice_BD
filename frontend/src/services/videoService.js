import { getToken } from '../utils/tokenStorage';

const API_BASE_URL = 'http://localhost:8080/api';

class VideoService {
  uploadVideo(file, description = '', enableAI = true, onProgress = null) {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      const formData = new FormData();

      // Prepare form data
      formData.append('file', file);
      if (description) formData.append('description', description);
      formData.append('enableAI', enableAI.toString());

      // CRITICAL: Open the request FIRST
      xhr.open('POST', `${API_BASE_URL}/videos/upload`);

      // CRITICAL: Set headers AFTER opening the request
      const token = getToken();
      if (token) {
        xhr.setRequestHeader('Authorization', `Bearer ${token}`);
      }

      // Set timeout
      xhr.timeout = 300000; // 5 minutes

      // Progress event handler
      if (onProgress && typeof onProgress === 'function') {
        xhr.upload.addEventListener('progress', (event) => {
          if (event.lengthComputable) {
            onProgress({
              loaded: event.loaded,
              total: event.total,
              progress: Math.round((event.loaded / event.total) * 100)
            });
          }
        });
      }

      // Success handler
      xhr.onload = function () {
        if (xhr.status >= 200 && xhr.status < 300) {
          try {
            const response = JSON.parse(xhr.responseText);
            resolve(response);
          } catch (e) {
            reject(new Error('Invalid JSON response from server'));
          }
        } else if (xhr.status === 401) {
          reject(new Error('Authentication required. Please login again.'));
        } else if (xhr.status === 413) {
          reject(new Error('File too large. Please choose a smaller video file.'));
        } else {
          let errorMessage = `Upload failed with status: ${xhr.status}`;
          try {
            const errorResponse = JSON.parse(xhr.responseText);
            errorMessage = errorResponse.message || errorMessage;
          } catch (e) {
            // Use default error message if parsing fails
          }
          reject(new Error(errorMessage));
        }
      };

      // Error handlers
      xhr.onerror = function () {
        reject(new Error('Network error occurred during upload. Please check your connection.'));
      };

      xhr.ontimeout = function () {
        reject(new Error('Upload timed out. Please try again with a smaller file.'));
      };

      xhr.onabort = function () {
        reject(new Error('Upload was cancelled.'));
      };

      // Send the request
      xhr.send(formData);
    });
  }

  // Helper method to make authenticated fetch requests
  async makeAuthenticatedRequest(url, options = {}) {
    const token = getToken();

    const defaultHeaders = {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` })
    };

    const requestOptions = {
      ...options,
      headers: {
        ...defaultHeaders,
        ...options.headers
      }
    };

    const response = await fetch(`${API_BASE_URL}${url}`, requestOptions);

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Request failed (${response.status}): ${errorText}`);
    }

    // Check if response has content
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return await response.json();
    } else {
      return await response.text();
    }
  }

  // Get video information
  async getVideoInfo(videoId) {
    return await this.makeAuthenticatedRequest(`/videos/${videoId}/info`);
  }

  // Delete video
  async deleteVideo(videoId) {
    return await this.makeAuthenticatedRequest(`/videos/${videoId}`, {
      method: 'DELETE'
    });
  }

  // Get all videos
  async getAllVideos() {
    return await this.makeAuthenticatedRequest('/videos');
  }

  // Get video status
  async getVideoStatus(videoId) {
    return await this.makeAuthenticatedRequest(`/videos/${videoId}/status`);
  }

  // Get latest prediction
  async getLatestPrediction(videoId) {
    try {
      return await this.makeAuthenticatedRequest(`/ai/predictions/${videoId}/latest`);
    } catch (error) {
      if (error.message.includes('404')) {
        return null; // No prediction found yet
      }
      throw error;
    }
  }

  // Get video stream URL
  getVideoStreamUrl(videoId) {
    const token = getToken();
    const tokenParam = token ? `?token=${encodeURIComponent(token)}` : '';
    return `${API_BASE_URL}/videos/${videoId}/stream${tokenParam}`;
  }

  // Get video download URL
  getVideoDownloadUrl(videoId) {
    const token = getToken();
    const tokenParam = token ? `?token=${encodeURIComponent(token)}` : '';
    return `${API_BASE_URL}/videos/${videoId}${tokenParam}`;
  }

  // Get video thumbnail URL (if needed)
  getVideoThumbnailUrl(videoId) {
    const token = getToken();
    const tokenParam = token ? `?token=${encodeURIComponent(token)}` : '';
    return `${API_BASE_URL}/videos/${videoId}/thumbnail${tokenParam}`;
  }
}

export default new VideoService();
