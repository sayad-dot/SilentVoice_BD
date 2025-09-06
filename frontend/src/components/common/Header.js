import React from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';

const Header = () => {
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    if (window.confirm('Are you sure you want to logout?')) {
      await logout();
      navigate('/login');
    }
  };

  return (
    <header style={{
      backgroundColor: '#4a90e2',
      padding: '1rem 2rem',
      boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
      position: 'sticky',
      top: 0,
      zIndex: 1000
    }}>
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        maxWidth: '1200px',
        margin: '0 auto'
      }}>
        {/* Left Side - Logo */}
        <h1 
          onClick={() => navigate('/')} 
          style={{ 
            cursor: 'pointer',
            color: 'white',
            margin: 0,
            fontSize: '1.5rem',
            fontWeight: 'bold'
          }}
        >
          🤟 SilentVoice BD
        </h1>

        {/* Right Side - Admin + User Info */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          {/* 🎯 ONLY ADMIN PANEL BUTTON - No other buttons */}
          {isAdmin && (
            <button 
              onClick={() => navigate('/admin')}
              style={{
                backgroundColor: '#dc3545',
                color: 'white',
                border: 'none',
                padding: '0.5rem 1rem',
                borderRadius: '5px',
                cursor: 'pointer',
                fontSize: '0.9rem',
                fontWeight: 'bold',
                boxShadow: '0 2px 5px rgba(220, 53, 69, 0.3)'
              }}
            >
              ⚙️ Admin Panel
            </button>
          )}

          {/* User Info */}
          {user && (
            <>
              <span style={{ 
                color: 'white', 
                fontSize: '0.9rem',
                fontWeight: '500'
              }}>
                Welcome, {user.fullName}!
              </span>
              <button 
                onClick={handleLogout}
                style={{
                  backgroundColor: 'rgba(255,255,255,0.2)',
                  color: 'white',
                  border: 'none',
                  padding: '0.5rem 1rem',
                  borderRadius: '5px',
                  cursor: 'pointer',
                  fontSize: '0.9rem'
                }}
              >
                Logout
              </button>
            </>
          )}
        </div>
      </div>
    </header>
  );
};

export default Header;
