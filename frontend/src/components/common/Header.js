import React from 'react';
import { useAuth } from '../../contexts/AuthContext';

const Header = () => {
  const { user, logout } = useAuth();

  return (
    <header className="app-header">
      <div className="header-content">
        <div className="header-left">
          <div className="logo">
            <span className="logo-icon">🎯</span>
            <span className="logo-text">SilentVoice_BD</span>
          </div>
        </div>

        <div className="header-right">
          {user && (
            <div className="user-info">
              <div className="user-details">
                <span className="welcome-text">Welcome, </span>
                <span className="user-name">{user.fullName}</span>
              </div>
              <button
                onClick={logout}
                className="logout-btn"
                title="Logout"
              >
                🚪 Logout
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

export default Header;
