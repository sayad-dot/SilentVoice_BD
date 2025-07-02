import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/videos';

const videoService = {
  // Upload a video file
  uploadVideo: async (file, description = '', onUploadProgress = null) => {
    const formData = new FormData();
    formData.append('file', file);
    if (description) {
      formData.append('description', description);
    }

    const config = {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      // Set timeout to 5 minutes for large uploads
      timeout: 300000,
    };

    if (onUploadProgress) {
      config.onUploadProgress = onUploadProgress;
    }

    try {
      const response = await axios.post(`${API_BASE_URL}/upload`, formData, config);
      return response.data;
    } catch (error) {
      console.error('Upload error:', error);
      if (error.code === 'ECONNABORTED') {
        throw 'Upload timed out. Please try again with a smaller file or check your network connection.';
      } else if (!error.response) {
        throw 'Network error. Please check your internet connection and try again.';
      } else {
        throw error.response?.data || error.message;
      }
    }
  },

  // Get all uploaded videos
  getAllVideos: async () => {
    try {
      const response = await axios.get(API_BASE_URL);
      return response.data;
    } catch (error) {
      throw error.response?.data || error.message;
    }
  },

  // Get video info
  getVideoInfo: async (videoId) => {
    try {
      const response = await axios.get(`${API_BASE_URL}/${videoId}/info`);
      return response.data;
    } catch (error) {
      throw error.response?.data || error.message;
    }
  },

  // Delete a video
  deleteVideo: async (videoId) => {
    try {
      const response = await axios.delete(`${API_BASE_URL}/${videoId}`);
      return response.data;
    } catch (error) {
      throw error.response?.data || error.message;
    }
  },

  // Get video stream URL
  getVideoStreamUrl: (videoId) => {
    return `${API_BASE_URL}/${videoId}/stream`;
  },

  // Get video download URL
  getVideoDownloadUrl: (videoId) => {
    return `${API_BASE_URL}/${videoId}`;
  }
};

export default videoService;
