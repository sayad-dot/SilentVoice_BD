import React, { useState, useEffect, useRef } from 'react';
import apiClient from '../../services/apiClient';
import { useAuth } from '../../contexts/AuthContext';
import '../../styles/learning/chatbot.css';
import { 
  FaComments, 
  FaTimes, 
  FaMinus, 
  FaExpand, 
  FaPaperPlane,
  FaRobot,
  FaUser,
  FaSpinner
} from 'react-icons/fa';

const ChatbotWidget = ({ 
  lessonId, 
  currentSign, 
  pageContext, 
  isGlobal = false, 
  isMinimized = false, 
  onClose, 
  onMinimize, 
  onMaximize 
}) => {
  const { user } = useAuth();
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isExpanded, setIsExpanded] = useState(!isMinimized);
  const [isVisible, setIsVisible] = useState(true);
  const [typingMessage, setTypingMessage] = useState('');
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => {
    // Add context-aware welcome message with typing animation
    const welcomeMessage = getWelcomeMessage();
    typeMessage(welcomeMessage);
  }, [pageContext, lessonId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const typeMessage = (text, isWelcome = true) => {
    if (isWelcome) {
      setMessages([]);
      setTypingMessage('');
    }

    let index = 0;
    const typing = setInterval(() => {
      setTypingMessage(prev => text.substring(0, index + 1));
      index++;

      if (index >= text.length) {
        clearInterval(typing);
        setTimeout(() => {
          const fullMessage = {
            role: 'assistant',
            content: text,
            timestamp: new Date(),
            animated: true
          };
          setMessages(prev => isWelcome ? [fullMessage] : [...prev, fullMessage]);
          setTypingMessage('');
        }, 500);
      }
    }, 30);
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const getWelcomeMessage = () => {
    if (!isGlobal) {
      return "👋 Hi there! I'm your AI-powered sign language assistant. I'm here to help you learn Bangla sign language more effectively. Ask me about hand positions, sign meanings, or any questions about your lesson!";
    }

    switch (pageContext?.page) {
      case 'upload':
        return "🎥 Welcome to the Upload section! I can help you with video uploads, explain how our AI analysis works, troubleshoot issues, or answer questions about sign language recognition. What would you like to know?";
      case 'live':
        return "📹 Hello! I'm here to assist with live recognition features. I can help with webcam setup, lighting optimization, real-time feedback tips, or any sign language practice questions. How can I help?";
      case 'learning':
        return "📚 Welcome to your learning journey! I can explain lessons, provide practice techniques, help with hand positioning, share cultural context about Bangla sign language, or motivate you through challenges. What can I assist you with today?";
      default:
        return "🤖 Hello! I'm your SilentVoice BD AI assistant. I can help you navigate the app, learn about features, practice sign language, troubleshoot issues, or provide learning tips. I'm here to make your sign language journey smooth and enjoyable!";
    }
  };

  const getSuggestions = () => {
    const baseSuggestions = [
      "How do I get started?", 
      "Tell me about Bangla sign language culture",
      "I'm having trouble with a sign",
      "How can I improve my accuracy?"
    ];

    switch (pageContext?.page) {
      case 'upload':
        return [
          "How do I upload a video?",
          "What video formats are supported?",
          "Why is my upload taking so long?",
          "How does AI analysis work?"
        ];
      case 'live':
        return [
          "How to start live recognition?",
          "My camera isn't working properly",
          "Tips to improve recognition accuracy",
          "Best lighting for webcam practice"
        ];
      case 'learning':
        return [
          "How do I start a lesson?",
          "Practice techniques for beginners",
          "How to improve my hand positions?",
          "Cultural significance of signs"
        ];
      default:
        return [
          "Give me a tour of the app",
          "Best practices for learning",
          "Technical support",
          "Motivation and encouragement"
        ];
    }
  };

  const sendMessage = async () => {
    if (!inputMessage.trim() || isLoading) return;

    const userMessage = {
      role: 'user',
      content: inputMessage.trim(),
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMessage]);
    setInputMessage('');
    setIsLoading(true);

    try {
      const response = await apiClient.post('/api/learning/chat', {
        message: inputMessage.trim(),
        user_id: user?.id || 'anonymous',
        lesson_context: {
          lesson_title: pageContext?.title || 'Current Session',
          current_sign: currentSign || 'General Help',
          lesson_id: lessonId,
          page_context: pageContext?.page || 'general',
          page_features: pageContext?.features || [],
          is_global: isGlobal
        }
      });

      // Simulate typing for AI response
      setTimeout(() => {
        const assistantMessage = {
          role: 'assistant',
          content: response.data.content,
          timestamp: new Date()
        };
        setMessages(prev => [...prev, assistantMessage]);
      }, 800);

    } catch (error) {
      console.error('Chat error:', error);
      const errorMessage = {
        role: 'assistant',
        content: 'I\'m experiencing some technical difficulties right now. Please try again in a moment, or feel free to continue with your learning! 😊',
        timestamp: new Date(),
        isError: true
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const clearConversation = () => {
    const welcomeMessage = getWelcomeMessage();
    setMessages([]);
    typeMessage("✨ Conversation cleared! " + welcomeMessage);
  };

  const formatTime = (timestamp) => {
    return new Date(timestamp).toLocaleTimeString([], { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };

  const handleSuggestionClick = (suggestion) => {
    setInputMessage(suggestion);
    inputRef.current?.focus();
  };

  const toggleExpanded = () => {
    if (isExpanded) {
      setIsExpanded(false);
      onMinimize?.();
    } else {
      setIsExpanded(true);
      onMaximize?.();
    }
  };

  const closeWidget = () => {
    setIsVisible(false);
    onClose?.();
  };

  if (!isVisible) {
    return (
      <div className="chatbot-launcher" onClick={() => setIsVisible(true)}>
        <div className="launcher-icon">
          <FaComments />
        </div>
        <div className="launcher-tooltip">Ask AI Assistant</div>
      </div>
    );
  }

  if (!isExpanded) {
    return (
      <div className="chatbot-minimized" onClick={toggleExpanded}>
        <div className="minimized-avatar">
          <FaRobot />
        </div>
        <div className="minimized-text">AI Assistant</div>
        <div className="minimized-status">
          {isLoading && <FaSpinner className="spinning" />}
        </div>
      </div>
    );
  }

  return (
    <div className="chatbot-widget">
      <div className="chatbot-header">
        <div className="header-left">
          <div className="bot-avatar">
            <FaRobot />
          </div>
          <div className="header-info">
            <h4>AI Assistant</h4>
            <span className="status">Online • Ready to help</span>
          </div>
        </div>
        <div className="header-actions">
          <button className="btn-icon" onClick={clearConversation} title="Clear chat">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
            </svg>
          </button>
          <button className="btn-icon" onClick={toggleExpanded} title="Minimize">
            <FaMinus />
          </button>
          <button className="btn-icon" onClick={closeWidget} title="Close">
            <FaTimes />
          </button>
        </div>
      </div>

      <div className="chatbot-messages">
        {messages.map((message, index) => (
          <div key={index} className={`message ${message.role} ${message.animated ? 'animated' : ''} ${message.isError ? 'error' : ''}`}>
            <div className="message-avatar">
              {message.role === 'user' ? <FaUser /> : <FaRobot />}
            </div>
            <div className="message-content">
              <div className="message-text">{message.content}</div>
              <div className="message-time">{formatTime(message.timestamp)}</div>
            </div>
          </div>
        ))}

        {typingMessage && (
          <div className="message assistant typing">
            <div className="message-avatar">
              <FaRobot />
            </div>
            <div className="message-content">
              <div className="message-text">{typingMessage}<span className="cursor">|</span></div>
            </div>
          </div>
        )}

        {isLoading && !typingMessage && (
          <div className="message assistant loading">
            <div className="message-avatar">
              <FaRobot />
            </div>
            <div className="message-content">
              <div className="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      <div className="chatbot-suggestions">
        {getSuggestions().map((suggestion, index) => (
          <button 
            key={index} 
            className="suggestion-chip"
            onClick={() => handleSuggestionClick(suggestion)}
            disabled={isLoading}
          >
            {suggestion}
          </button>
        ))}
      </div>

      <div className="chatbot-input">
        <div className="input-container">
          <textarea
            ref={inputRef}
            value={inputMessage}
            onChange={(e) => setInputMessage(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Type your message here..."
            rows="1"
            disabled={isLoading}
            className="message-input"
          />
          <button 
            className="send-button"
            onClick={sendMessage}
            disabled={!inputMessage.trim() || isLoading}
          >
            {isLoading ? <FaSpinner className="spinning" /> : <FaPaperPlane />}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ChatbotWidget;