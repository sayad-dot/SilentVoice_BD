const API_BASE_URL = 'http://localhost:8080/api';

class VideoService {
  uploadVideo(file, description = '', enableAI = true, onProgress = null) {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      const formData = new FormData();

      formData.append('file', file);
      if (description) formData.append('description', description);
      formData.append('enableAI', enableAI.toString());

      // Progress event
      if (onProgress && typeof onProgress === 'function') {
        xhr.upload.addEventListener('progress', (event) => {
          if (event.lengthComputable) {
            onProgress({
              loaded: event.loaded,
              total: event.total,
              progress: (event.loaded / event.total) * 100
            });
          }
        });
      }

      xhr.onload = function () {
        if (xhr.status >= 200 && xhr.status < 300) {
          try {
            const response = JSON.parse(xhr.responseText);
            resolve(response);
          } catch (e) {
            reject(new Error('Invalid JSON response'));
          }
        } else {
          reject(new Error(`Upload failed with status: ${xhr.status} - ${xhr.statusText}`));
        }
      };

      xhr.onerror = function () {
        reject(new Error('Network error during upload'));
      };

      xhr.ontimeout = function () {
        reject(new Error('Upload timed out'));
      };

      xhr.open('POST', `${API_BASE_URL}/videos/upload`);
      xhr.timeout = 300000; // 5 minutes

      xhr.send(formData);
    });
  }

  getVideoStreamUrl(videoId) {
    return `${API_BASE_URL}/videos/${videoId}/stream`;
  }

  async getVideoInfo(videoId) {
    const response = await fetch(`${API_BASE_URL}/videos/${videoId}/info`);
    if (!response.ok) {
      throw new Error('Failed to fetch video info');
    }
    return await response.json();
  }

  async deleteVideo(videoId) {
    const response = await fetch(`${API_BASE_URL}/videos/${videoId}`, {
      method: 'DELETE'
    });
    if (!response.ok) {
      throw new Error('Failed to delete video');
    }
    return await response.text();
  }

  async getAllVideos() {
    const response = await fetch(`${API_BASE_URL}/videos`);
    if (!response.ok) {
      throw new Error('Failed to fetch videos');
    }
    return await response.json();
  }

  getVideoDownloadUrl(videoId) {
    return `${API_BASE_URL}/videos/${videoId}`;
  }
}

export default new VideoService();
