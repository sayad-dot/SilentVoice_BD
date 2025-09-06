import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation, useNavigate } from 'react-router-dom';
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
import AdminLayout from './components/admin/AdminLayout';
import AuditLogs from './components/admin/AuditLogs';
import SupportTickets from './components/admin/SupportTickets';
import FeatureFlags from './components/admin/FeatureFlags';
import UserManagement from './components/admin/UserManagement';
import ContentManagement from './components/admin/ContentManagement';
import './App.css';

// Component to require authentication
const RequireAuth = ({ children }) => {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="loading-container">
        <LoadingSpinner size="large" message="Loading..." />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
};

// Component to require admin role
const RequireAdmin = ({ children }) => {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return (
      <div className="loading-container">
        <LoadingSpinner size="large" message="Loading..." />
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (!user.roles?.includes('ADMIN')) {
    return <Navigate to="/" replace />;
  }

  return children;
};

// Auth pages component (Login/Register)
const AuthPages = () => {
  const [authView, setAuthView] = useState('login');
  const location = useLocation();
  
  // Determine which view to show based on the current path
  useEffect(() => {
    if (location.pathname === '/register') {
      setAuthView('register');
    } else {
      setAuthView('login');
    }
  }, [location.pathname]);

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
};

// Individual route components
const UploadPage = ({ isChatbotOpen, toggleChatbot, chatbotMinimized, setChatbotMinimized, setIsChatbotOpen }) => {
  const pageContext = {
    page: 'upload',
    title: 'Video Upload',
    description: 'Upload sign language videos for AI analysis',
    features: ['Video upload', 'AI analysis', 'Sign recognition', 'Progress tracking']
  };

  return (
    <div className="App authenticated">
      <Header />
      
      <div className="app-navigation">
        <NavigationButtons currentView="upload" />
      </div>

      <div className="app-content">
        <VideoUpload />
      </div>

      <ChatbotControls
        isChatbotOpen={isChatbotOpen}
        toggleChatbot={toggleChatbot}
        chatbotMinimized={chatbotMinimized}
        setChatbotMinimized={setChatbotMinimized}
        setIsChatbotOpen={setIsChatbotOpen}
        pageContext={pageContext}
        selectedLessonId={null}
      />
    </div>
  );
};

const LivePage = ({ isChatbotOpen, toggleChatbot, chatbotMinimized, setChatbotMinimized, setIsChatbotOpen }) => {
  const pageContext = {
    page: 'live',
    title: 'Live Recognition',
    description: 'Real-time sign language recognition using webcam',
    features: ['Live webcam feed', 'Real-time analysis', 'Instant feedback', 'Practice mode']
  };

  return (
    <div className="App authenticated">
      <Header />
      
      <div className="app-navigation">
        <NavigationButtons currentView="live" />
      </div>

      <div className="app-content">
        <LiveWebcamRecognition />
      </div>

      <ChatbotControls
        isChatbotOpen={isChatbotOpen}
        toggleChatbot={toggleChatbot}
        chatbotMinimized={chatbotMinimized}
        setChatbotMinimized={setChatbotMinimized}
        setIsChatbotOpen={setIsChatbotOpen}
        pageContext={pageContext}
        selectedLessonId={null}
      />
    </div>
  );
};

const LearningPage = ({ isChatbotOpen, toggleChatbot, chatbotMinimized, setChatbotMinimized, setIsChatbotOpen }) => {
  const [selectedLessonId, setSelectedLessonId] = useState(null);
  const navigate = useNavigate();
  const location = useLocation();
  
  // Parse lesson ID from URL if present
  useEffect(() => {
    const searchParams = new URLSearchParams(location.search);
    const lessonId = searchParams.get('lesson');
    if (lessonId) {
      setSelectedLessonId(lessonId);
    } else {
      setSelectedLessonId(null);
    }
  }, [location.search]);

  const handleSelectLesson = (lessonId) => {
    setSelectedLessonId(lessonId);
    navigate(`/learning?lesson=${lessonId}`);
  };

  const handleBackToHome = () => {
    setSelectedLessonId(null);
    navigate('/learning');
  };

  const pageContext = {
    page: 'learning',
    title: 'Learn Signs',
    description: 'Interactive sign language learning lessons',
    features: ['Video lessons', 'Practice exercises', 'Progress tracking', 'Mirror practice']
  };

  return (
    <div className="App authenticated">
      <Header />
      
      <div className="app-navigation">
        <NavigationButtons currentView="learning" />
      </div>

      <div className="app-content">
        {selectedLessonId ? (
          <LessonViewer 
            lessonId={selectedLessonId}
            onBack={handleBackToHome}
          />
        ) : (
          <LessonHome onSelectLesson={handleSelectLesson} />
        )}
      </div>

      <ChatbotControls
        isChatbotOpen={isChatbotOpen}
        toggleChatbot={toggleChatbot}
        chatbotMinimized={chatbotMinimized}
        setChatbotMinimized={setChatbotMinimized}
        setIsChatbotOpen={setIsChatbotOpen}
        pageContext={pageContext}
        selectedLessonId={selectedLessonId}
      />
    </div>
  );
};

// Navigation buttons component
const NavigationButtons = ({ currentView }) => {
  const navigate = useNavigate();

  return (
    <>
      <button 
        className={`nav-button ${currentView === 'upload' ? 'active' : ''}`}
        onClick={() => navigate('/')}
      >
        📤 Upload Video
      </button>
      <button 
        className={`nav-button ${currentView === 'live' ? 'active' : ''}`}
        onClick={() => navigate('/live')}
      >
        📹 Live Recognition
      </button>
      <button 
        className={`nav-button ${currentView === 'learning' ? 'active' : ''}`}
        onClick={() => navigate('/learning')}
      >
        📚 Learn Signs
      </button>
    </>
  );
};

// Chatbot controls component
const ChatbotControls = ({ 
  isChatbotOpen, 
  toggleChatbot, 
  chatbotMinimized, 
  setChatbotMinimized, 
  setIsChatbotOpen, 
  pageContext, 
  selectedLessonId 
}) => {
  return (
    <>
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
          pageContext={pageContext}
          isGlobal={true}
          isMinimized={chatbotMinimized}
          onClose={() => setIsChatbotOpen(false)}
          onMinimize={() => setChatbotMinimized(true)}
          onMaximize={() => setChatbotMinimized(false)}
        />
      )}
    </>
  );
};

// Main user dashboard wrapper
const UserDashboard = () => {
  const [isChatbotOpen, setIsChatbotOpen] = useState(false);
  const [chatbotMinimized, setChatbotMinimized] = useState(true);

  const toggleChatbot = () => {
    if (!isChatbotOpen) {
      setIsChatbotOpen(true);
      setChatbotMinimized(false);
    } else {
      setIsChatbotOpen(false);
    }
  };

  const chatbotProps = {
    isChatbotOpen,
    toggleChatbot,
    chatbotMinimized,
    setChatbotMinimized,
    setIsChatbotOpen
  };

  return (
    <Routes>
      <Route path="/" element={<UploadPage {...chatbotProps} />} />
      <Route path="/live" element={<LivePage {...chatbotProps} />} />
      <Route path="/learning" element={<LearningPage {...chatbotProps} />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

// Admin dashboard
const AdminDashboard = () => {
  const [adminTab, setAdminTab] = useState('audit');
  const location = useLocation();
  const navigate = useNavigate();

  // Update admin tab based on URL
  useEffect(() => {
    const path = location.pathname.split('/')[2]; // /admin/[tab]
    if (path) {
      setAdminTab(path);
    }
  }, [location.pathname]);

  const handleTabSelect = (key) => {
    setAdminTab(key);
    navigate(`/admin/${key}`);
  };

  return (
    <div className="App admin-app">
      <AdminLayout
        selectedKey={adminTab}
        onSelect={handleTabSelect}
      >
        <Routes>
          <Route path="/audit" element={<AuditLogs />} />
          <Route path="/tickets" element={<SupportTickets />} />
          <Route path="/flags" element={<FeatureFlags />} />
          <Route path="/users" element={<UserManagement />} />
          <Route path="/content" element={<ContentManagement />} />
          <Route path="/" element={<Navigate to="/admin/audit" replace />} />
          <Route path="*" element={<Navigate to="/admin/audit" replace />} />
        </Routes>
      </AdminLayout>
    </div>
  );
};

// Main App component with routing
const AppContent = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public routes */}
        <Route path="/login" element={<AuthPages />} />
        <Route path="/register" element={<AuthPages />} />

        {/* Protected user routes */}
        <Route
          path="/*"
          element={
            <RequireAuth>
              <UserDashboard />
            </RequireAuth>
          }
        />

        {/* Admin routes */}
        <Route
          path="/admin/*"
          element={
            <RequireAdmin>
              <AdminDashboard />
            </RequireAdmin>
          }
        />
      </Routes>
    </BrowserRouter>
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
