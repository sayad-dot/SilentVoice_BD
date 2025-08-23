// components/common/Header.js - MERGED VERSION WITH NAVIGATION
import React from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';

const Header = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    if (window.confirm('Are you sure you want to logout?')) {
      await logout();
      navigate('/login');
    }
  };

  const goToAdmin = () => {
    navigate('/admin');
  };

  const goToHome = () => {
    navigate('/');
  };

  return (
    <header className="app-header">
      <div className="header-content">
        <div className="header-left">
          <div className="logo" onClick={goToHome} style={{ cursor: 'pointer' }}>
            <span className="logo-icon">🎯</span>
            <span className="logo-text">SilentVoice BD</span>
          </div>
        </div>

        <div className="header-right">
          {user && (
            <div className="user-info">
              <div className="user-details">
                <span className="welcome-text">Welcome, </span>
                <span className="user-name">{user.fullName}</span>
              </div>
              
              <div className="header-actions">
                {user.roles?.includes('ADMIN') && (
                  <button onClick={goToAdmin} className="admin-btn">
                    ⚙️ Admin Panel
                  </button>
                )}
                <button
                  onClick={handleLogout}
                  className="logout-btn"
                  title="Logout"
                >
                  🚪 Logout
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

export default Header;
