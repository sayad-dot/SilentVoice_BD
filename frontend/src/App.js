import React, { useState } from 'react';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import Login from './components/auth/Login';
import Register from './components/auth/Register';
import VideoUpload from './components/VideoUpload';
import Header from './components/common/Header';
import LoadingSpinner from './components/common/LoadingSpinner';
import './App.css';

const AppContent = () => {
  const { user, loading } = useAuth();
  const [authView, setAuthView] = useState('login'); // 'login' or 'register'

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
      <main className="app-main">
        <VideoUpload />
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
