import React, { useState } from 'react';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import Login from './components/auth/Login';
import Register from './components/auth/Register';
import VideoUpload from './components/VideoUpload';
import LiveWebcamRecognition from './components/LiveWebcamRecognition';
import Header from './components/common/Header';
import LoadingSpinner from './components/common/LoadingSpinner';
import './App.css';

const AppContent = () => {
  const { user, loading } = useAuth();
  const [authView, setAuthView] = useState('login'); // 'login' or 'register'
  const [currentView, setCurrentView] = useState('upload'); // 'upload' or 'live'

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
      </div>

      <main className="app-main">
        {currentView === 'upload' ? (
          <VideoUpload />
        ) : (
          <LiveWebcamRecognition />
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
