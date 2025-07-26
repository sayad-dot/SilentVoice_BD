import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

class WebSocketService {
  constructor() {
    this.stompClient = null;
    this.connected = false;
    this.sessionId = null;
    this.subscriptions = new Map();
    this.connectionPromise = null;
  }

  connect() {
    if (this.connectionPromise) {
      return this.connectionPromise;
    }

    if (this.connected && this.stompClient) {
      return Promise.resolve(this.stompClient);
    }

    this.connectionPromise = new Promise((resolve, reject) => {
      console.log('🔌 Attempting WebSocket connection...');
      
      const socket = new SockJS('/ws');
      this.stompClient = Stomp.over(() => socket);
      
      // Increased timeouts for stability
      this.stompClient.heartbeat.outgoing = 30000; // 30 seconds
      this.stompClient.heartbeat.incoming = 30000; // 30 seconds
      this.stompClient.reconnectDelay = 10000; // 10 seconds
      
      this.stompClient.debug = (str) => {
        console.log('📡 STOMP:', str);
      };
      
      // Increased connection timeout to 20 seconds
      const connectTimeout = setTimeout(() => {
        console.error('⏰ WebSocket connection timeout after 20 seconds');
        reject(new Error('Connection timeout after 20 seconds'));
      }, 20000);
      
      this.stompClient.connect(
        {}, 
        (frame) => {
          clearTimeout(connectTimeout);
          console.log('✅ WebSocket connected successfully!', frame);
          this.connected = true;
          this.connectionPromise = null;
          resolve(this.stompClient);
        },
        (error) => {
          clearTimeout(connectTimeout);
          console.error('❌ WebSocket connection failed:', error);
          this.connected = false;
          this.connectionPromise = null;
          reject(new Error(`WebSocket connection failed: ${error}`));
        }
      );
    });

    return this.connectionPromise;
  }

  disconnect() {
    if (this.stompClient && this.connected) {
      console.log('🔌 Disconnecting WebSocket...');
      
      this.subscriptions.forEach((subscription, key) => {
        console.log(`🔇 Unsubscribing from ${key}`);
        subscription.unsubscribe();
      });
      this.subscriptions.clear();
      
      this.stompClient.disconnect(() => {
        console.log('✅ WebSocket disconnected successfully');
      });
      
      this.connected = false;
      this.sessionId = null;
      this.connectionPromise = null;
    }
  }

  startLiveSession(userId) {
    return new Promise((resolve, reject) => {
      if (!this.connected) {
        reject(new Error('WebSocket not connected'));
        return;
      }

      console.log('🎬 Starting live session for user:', userId);

      // Increased session timeout to 20 seconds
      const sessionTimeout = setTimeout(() => {
        reject(new Error('Session start timeout after 20 seconds'));
      }, 20000);

      const subscription = this.stompClient.subscribe('/topic/session-status', (message) => {
        try {
          const response = JSON.parse(message.body);
          console.log('📨 Session status received:', response);
          
          if (response.status === 'started' && !response.error) {
            clearTimeout(sessionTimeout);
            this.sessionId = response.sessionId;
            console.log('✅ Live session started with ID:', this.sessionId);
            resolve(response);
          } else if (response.error) {
            clearTimeout(sessionTimeout);
            reject(new Error(response.message || 'Session start failed'));
          }
        } catch (error) {
          clearTimeout(sessionTimeout);
          console.error('❌ Error parsing session status:', error);
          reject(error);
        }
      });

      this.subscriptions.set('session-status', subscription);

      try {
        this.stompClient.send('/app/start-live-session', {}, JSON.stringify({
          userId: userId
        }));
        console.log('📤 Start session request sent');
      } catch (error) {
        clearTimeout(sessionTimeout);
        console.error('❌ Error sending start session request:', error);
        reject(error);
      }
    });
  }

  subscribeToPredictions(callback) {
    if (!this.connected || !this.sessionId) {
      throw new Error('Session not started');
    }

    console.log('🎯 Subscribing to predictions for session:', this.sessionId);

    const subscription = this.stompClient.subscribe('/user/queue/predictions', (message) => {
      try {
        const prediction = JSON.parse(message.body);
        console.log('🔮 Prediction received:', prediction);
        callback(prediction);
      } catch (error) {
        console.error('❌ Error parsing prediction:', error);
      }
    });

    this.subscriptions.set('predictions', subscription);
    return subscription;
  }

  subscribeToErrors(callback) {
    if (!this.connected || !this.sessionId) {
      throw new Error('Session not started');
    }

    console.log('⚠️ Subscribing to errors for session:', this.sessionId);

    const subscription = this.stompClient.subscribe('/user/queue/errors', (message) => {
      try {
        const error = JSON.parse(message.body);
        console.error('🚨 Error received:', error);
        callback(error);
      } catch (parseError) {
        console.error('❌ Error parsing error message:', parseError);
      }
    });

    this.subscriptions.set('errors', subscription);
    return subscription;
  }

  sendFrame(frameData) {
    if (!this.connected || !this.sessionId) {
      throw new Error('Session not started');
    }

    const frameMessage = {
      sessionId: this.sessionId,
      frameData: frameData,
      timestamp: Date.now()
    };

    try {
      this.stompClient.send('/app/live-frame', {}, JSON.stringify(frameMessage));
    } catch (error) {
      console.error('❌ Error sending frame:', error);
      throw error;
    }
  }

  isConnected() {
    return this.connected;
  }

  getSessionId() {
    return this.sessionId;
  }
}

const webSocketService = new WebSocketService();
export default webSocketService;
