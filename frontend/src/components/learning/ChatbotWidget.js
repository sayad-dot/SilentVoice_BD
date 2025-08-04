import React, { useState, useEffect, useRef } from 'react';
import apiClient from '../../services/apiClient';
import { useAuth } from '../../contexts/AuthContext';
import '../../styles/learning/chatbot.css';

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
  const messagesEndRef = useRef(null);

  useEffect(() => {
    // Add context-aware welcome message
    const welcomeMessage = getWelcomeMessage();
    setMessages([{
      role: 'assistant',
      content: welcomeMessage,
      timestamp: new Date()
    }]);
  }, [pageContext, lessonId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const getWelcomeMessage = () => {
    if (!isGlobal) {
      return "Hi! I'm your sign language learning assistant. I can help you with hand positions, sign meanings, or any questions about your lesson. How can I help you today?";
    }

    switch (pageContext?.page) {
      case 'upload':
        return "Hi! I'm your AI assistant. I can help you with video uploads, explain how AI analysis works, or answer any questions about sign language recognition. What would you like to know?";
      case 'live':
        return "Hello! I can help you with live recognition features, webcam setup, real-time feedback, or any sign language questions. How can I assist you?";
      case 'learning':
        return "Hi there! I'm here to help with your sign language learning journey. I can explain lessons, help with practice techniques, or answer any questions about Bangla sign language. What can I help you with?";
      default:
        return "Hello! I'm your AI assistant for SilentVoice BD. I can help you navigate the app, learn about features, practice sign language, or answer any questions. How can I help you today?";
    }
  };

  const getSuggestions = () => {
    const baseSuggestions = ["How do I use this app?", "Tell me about Bangla sign language"];
    
    switch (pageContext?.page) {
      case 'upload':
        return [
          "How do I upload a video?",
          "What video formats are supported?",
          "How does AI analysis work?",
          "Why is my upload taking long?"
        ];
      case 'live':
        return [
          "How to start live recognition?",
          "Camera not working?",
          "How to improve accuracy?",
          "Practice tips for webcam"
        ];
      case 'learning':
        return [
          "How to start a lesson?",
          "Practice techniques",
          "How to improve my signs?",
          "Cultural context of signs"
        ];
      default:
        return [
          "Show me around the app",
          "How to get started?",
          "Best practices for learning",
          "Technical support"
        ];
    }
  };

  const sendMessage = async () => {
    if (!inputMessage.trim()) return;

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

      const assistantMessage = {
        role: 'assistant',
        content: response.data.content,
        timestamp: new Date()
      };

      setMessages(prev => [...prev, assistantMessage]);
    } catch (error) {
      console.error('Chat error:', error);
      const errorMessage = {
        role: 'assistant',
        content: 'I\'m having trouble responding right now. Please try again in a moment.',
        timestamp: new Date()
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
    setMessages([{
      role: 'assistant',
      content: "Conversation cleared! " + welcomeMessage,
      timestamp: new Date()
    }]);
  };

  const formatTime = (timestamp) => {
    return new Date(timestamp).toLocaleTimeString([], { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };

  const handleSuggestionClick = (suggestion) => {
    setInputMessage(suggestion);
  };

  if (isMinimized) {
    return (
      <div className="chatbot-minimized" onClick={onMaximize}>
        <div className="chatbot-minimized-content">
          <span className="chatbot-icon">🤖</span>
          <span className="chatbot-title">AI Assistant</span>
          {isLoading && <span className="typing-dot">●</span>}
        </div>
      </div>
    );
  }

  return (
    <div className={`chatbot-widget ${isGlobal ? 'global' : ''}`}>
      <div className="chatbot-header">
        <h4>🤖 AI Assistant</h4>
        <div className="chatbot-header-buttons">
          {isGlobal && (
            <button 
              className="btn-icon" 
              onClick={onMinimize}
              title="Minimize"
            >
              ➖
            </button>
          )}
          <button 
            className="btn-icon" 
            onClick={clearConversation}
            title="Clear conversation"
          >
            🗑️
          </button>
          <button 
            className="btn-icon" 
            onClick={onClose}
            title="Close"
          >
            ✕
          </button>
        </div>
      </div>

      <div className="chatbot-messages">
        {messages.map((message, index) => (
          <div key={index} className={`message ${message.role === 'user' ? 'user' : 'bot'}`}>
            <div className="message-content">
              {message.content}
            </div>
            <div className="message-time">
              {formatTime(message.timestamp)}
            </div>
          </div>
        ))}
        
        {isLoading && (
          <div className="message bot typing">
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

      {/* Context-aware suggestions */}
      <div className="chatbot-suggestions">
        {getSuggestions().map((suggestion, index) => (
          <button
            key={index}
            className="suggestion-chip"
            onClick={() => handleSuggestionClick(suggestion)}
          >
            {suggestion}
          </button>
        ))}
      </div>

      <div className="chatbot-input">
        <div className="input-container">
          <textarea
            value={inputMessage}
            onChange={(e) => setInputMessage(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder={`Ask me about ${pageContext?.title || 'anything'}...`}
            disabled={isLoading}
            rows="1"
          />
          <button
            className="send-button"
            onClick={sendMessage}
            disabled={isLoading || !inputMessage.trim()}
          >
            📤
          </button>
        </div>
      </div>
    </div>
  );
};

export default ChatbotWidget;
