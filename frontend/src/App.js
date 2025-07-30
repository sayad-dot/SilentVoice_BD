import React, { useState } from 'react';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import Login from './components/auth/Login';
import Register from './components/auth/Register';
import VideoUpload from './components/VideoUpload';
import LiveWebcamRecognition from './components/LiveWebcamRecognition';
import LessonHome from './components/learning/LessonHome';
import LessonViewer from './components/learning/LessonViewer';
import Header from './components/common/Header';
import LoadingSpinner from './components/common/LoadingSpinner';
import './App.css';

const AppContent = () => {
  const { user, loading } = useAuth();
  const [authView, setAuthView] = useState('login'); // 'login' or 'register'
  const [currentView, setCurrentView] = useState('upload'); // 'upload', 'live', 'learning'
  const [selectedLessonId, setSelectedLessonId] = useState(null);

  if (loading) {
    return (
      <div className="app-loading">
        <LoadingSpinner size="large" message="Loading application..." />
      </div>
    );
  }

  if (!user) {
    // Show authentication interface
    return (
      <div className="App">
        {authView === 'login' ? (
          <Login
            onSwitchToRegister={() => setAuthView('register')}
          />
        ) : (
          <Register
            onSwitchToLogin={() => setAuthView('login')}
            onRegistrationSuccess={() => setAuthView('login')}
          />
        )}
      </div>
    );
  }

  // Handle lesson viewer navigation
  const handleSelectLesson = (lessonId) => {
    setSelectedLessonId(lessonId);
    setCurrentView('lesson-viewer');
  };

  const handleBackToLessons = () => {
    setSelectedLessonId(null);
    setCurrentView('learning');
  };

  // User is authenticated, show main application
  return (
    <div className="App authenticated">
      <Header />
      
      {/* Navigation Tabs */}
      <div className="app-navigation">
        <button 
          className={`nav-button ${currentView === 'upload' ? 'active' : ''}`}
          onClick={() => setCurrentView('upload')}
        >
          📁 Upload Video
        </button>
        <button 
          className={`nav-button ${currentView === 'live' ? 'active' : ''}`}
          onClick={() => setCurrentView('live')}
        >
          📹 Live Recognition
        </button>
        <button 
          className={`nav-button ${currentView === 'learning' || currentView === 'lesson-viewer' ? 'active' : ''}`}
          onClick={() => setCurrentView('learning')}
        >
          🎓 Learn Signs
        </button>
      </div>

      <main className="app-main">
        {currentView === 'upload' && <VideoUpload />}
        {currentView === 'live' && <LiveWebcamRecognition />}
        {currentView === 'learning' && (
          <LessonHome onSelectLesson={handleSelectLesson} />
        )}
        {currentView === 'lesson-viewer' && selectedLessonId && (
          <LessonViewer 
            lessonId={selectedLessonId} 
            onBack={handleBackToLessons}
          />
        )}
      </main>
    </div>
  );
};

function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}

export default App;
