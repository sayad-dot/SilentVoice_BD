import React, { useState } from 'react';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import Login from './components/auth/Login';
import Register from './components/auth/Register';
import VideoUpload from './components/VideoUpload';
import LiveWebcamRecognition from './components/LiveWebcamRecognition';
import LessonHome from './components/learning/LessonHome';
import LessonViewer from './components/learning/LessonViewer';
import ChatbotWidget from './components/learning/ChatbotWidget';
import Header from './components/common/Header';
import LoadingSpinner from './components/common/LoadingSpinner';
import './App.css';

const AppContent = () => {
  const { user, loading } = useAuth();
  const [authView, setAuthView] = useState('login');
  const [currentView, setCurrentView] = useState('upload');
  const [selectedLessonId, setSelectedLessonId] = useState(null);
  
  // Global chatbot state
  const [isChatbotOpen, setIsChatbotOpen] = useState(false);
  const [chatbotMinimized, setChatbotMinimized] = useState(true);

  if (loading) {
    return (
      <div className="loading-container">
        <LoadingSpinner size="large" message="Loading..." />
      </div>
    );
  }

  if (!user) {
    return (
      <div className="App auth-layout">
        <div className="auth-container">
          <div className="auth-header">
            <h1>SilentVoice BD</h1>
            <p>AI-Powered Bangla Sign Language Recognition</p>
          </div>
          
          <div className="auth-tabs">
            <button 
              className={authView === 'login' ? 'active' : ''}
              onClick={() => setAuthView('login')}
            >
              Login
            </button>
            <button 
              className={authView === 'register' ? 'active' : ''}
              onClick={() => setAuthView('register')}
            >
              Register
            </button>
          </div>

          <div className="auth-content">
            {authView === 'login' ? (
              <Login onSwitchToRegister={() => setAuthView('register')} />
            ) : (
              <Register onSwitchToLogin={() => setAuthView('login')} />
            )}
          </div>
        </div>
      </div>
    );
  }

  // Get current page context for chatbot
  const getCurrentPageContext = () => {
    switch (currentView) {
      case 'upload':
        return {
          page: 'upload',
          title: 'Video Upload',
          description: 'Upload sign language videos for AI analysis',
          features: ['Video upload', 'AI analysis', 'Sign recognition', 'Progress tracking']
        };
      case 'live':
        return {
          page: 'live',
          title: 'Live Recognition',
          description: 'Real-time sign language recognition using webcam',
          features: ['Live webcam feed', 'Real-time analysis', 'Instant feedback', 'Practice mode']
        };
      case 'learning':
        return {
          page: 'learning',
          title: 'Learn Signs',
          description: 'Interactive sign language learning lessons',
          features: ['Video lessons', 'Practice exercises', 'Progress tracking', 'Mirror practice']
        };
      default:
        return {
          page: 'home',
          title: 'SilentVoice BD',
          description: 'AI-powered Bangla sign language learning platform',
          features: ['Sign recognition', 'Learning modules', 'Practice tools', 'Progress tracking']
        };
    }
  };

  const toggleChatbot = () => {
    if (!isChatbotOpen) {
      setIsChatbotOpen(true);
      setChatbotMinimized(false);
    } else {
      setIsChatbotOpen(false);
    }
  };

  return (
    <div className="App authenticated">
      <Header />
      
      <div className="app-navigation">
        <button 
          className={`nav-button ${currentView === 'upload' ? 'active' : ''}`}
          onClick={() => setCurrentView('upload')}
        >
          📤 Upload Video
        </button>
        <button 
          className={`nav-button ${currentView === 'live' ? 'active' : ''}`}
          onClick={() => setCurrentView('live')}
        >
          📹 Live Recognition
        </button>
        <button 
          className={`nav-button ${currentView === 'learning' ? 'active' : ''}`}
          onClick={() => setCurrentView('learning')}
        >
          📚 Learn Signs
        </button>
      </div>

      <div className="app-content">
        {currentView === 'upload' && <VideoUpload />}
        {currentView === 'live' && <LiveWebcamRecognition />}
        {currentView === 'learning' && selectedLessonId ? (
          <LessonViewer 
            lessonId={selectedLessonId}
            onBack={() => setSelectedLessonId(null)}
          />
        ) : currentView === 'learning' && (
          <LessonHome onSelectLesson={setSelectedLessonId} />
        )}
      </div>

      {/* Global AI Assistant */}
      {!isChatbotOpen && (
        <button 
          className="chatbot-toggle-button"
          onClick={toggleChatbot}
          title="Open AI Assistant"
        >
          🤖
        </button>
      )}

      {isChatbotOpen && (
        <ChatbotWidget
          lessonId={selectedLessonId}
          currentSign={null}
          pageContext={getCurrentPageContext()}
          isGlobal={true}
          isMinimized={chatbotMinimized}
          onClose={() => setIsChatbotOpen(false)}
          onMinimize={() => setChatbotMinimized(true)}
          onMaximize={() => setChatbotMinimized(false)}
        />
      )}
    </div>
  );
};

const App = () => {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
};

export default App;
