import apiClient from './apiClient';

class AudioService {
  constructor() {
    this.baseURL = process.env.REACT_APP_API_URL || 'http://localhost:8080';
  }

  async getAudioForPrediction(predictionId) {
    try {
      console.log('🔊 Fetching audio for prediction:', predictionId);
      
      const response = await apiClient.get(`/api/audio/prediction/${predictionId}`, {
        responseType: 'blob'
      });
      
      console.log('✅ Audio blob received, size:', response.data.size);
      return response.data;
    } catch (error) {
      console.error('❌ Error fetching audio:', error);
      if (error.response?.status === 404) {
        throw new Error('Audio not found for this prediction');
      }
      throw error;
    }
  }

  async generateAudio(text) {
    try {
      console.log('🎵 Generating audio for text:', text);
      
      const response = await apiClient.post('/api/audio/generate', {
        text: text
      });
      
      console.log('✅ Audio generation response:', response.data);
      return response.data;
    } catch (error) {
      console.error('❌ Error generating audio:', error);
      throw error;
    }
  }

  async deleteAudio(predictionId) {
    try {
      console.log('🗑️ Deleting audio for prediction:', predictionId);
      
      const response = await apiClient.delete(`/api/audio/prediction/${predictionId}`);
      
      console.log('✅ Audio deleted successfully');
      return response.data;
    } catch (error) {
      console.error('❌ Error deleting audio:', error);
      throw error;
    }
  }

  getAudioUrl(predictionId) {
    return `${this.baseURL}/api/audio/prediction/${predictionId}`;
  }
}

const audioService = new AudioService();
export default audioService;
