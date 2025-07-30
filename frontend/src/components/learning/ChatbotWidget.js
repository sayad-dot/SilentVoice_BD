import React, { useState, useEffect, useRef } from 'react';
import apiClient from '../../services/apiClient';
import { useAuth } from '../../contexts/AuthContext';
import '../../styles/learning/chatbot.css';

const ChatbotWidget = ({ lessonId, currentSign, onClose }) => {
    const { user } = useAuth();
    const [messages, setMessages] = useState([]);
    const [inputMessage, setInputMessage] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const messagesEndRef = useRef(null);

    useEffect(() => {
        // Add welcome message when component mounts
        setMessages([{
            role: 'assistant',
            content: "Hi! I'm your sign language learning assistant. I can help you with hand positions, sign meanings, or any questions about your lesson. How can I help you today?",
            timestamp: new Date()
        }]);
    }, [lessonId]);

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
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
                    lesson_title: 'Current Lesson',
                    current_sign: currentSign || 'Unknown Sign',
                    lesson_id: lessonId
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
                content: 'Im having trouble responding right now. Please try again in a moment.',
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
        setMessages([{
            role: 'assistant',
            content: "Conversation cleared! How can I help you with your lesson?",
            timestamp: new Date()
        }]);
    };

    const formatTime = (timestamp) => {
        return new Date(timestamp).toLocaleTimeString([], { 
            hour: '2-digit', minute: '2-digit' 
        });
    };

    const handleSuggestionClick = (suggestion) => {
        setInputMessage(suggestion);
    };

    return (
        <div className="chatbot-widget">
            <div className="chatbot-header">
                <h4>AI Learning Assistant</h4>
                <div className="chatbot-header-buttons">
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
                        title="Close chat"
                    >
                        ✕
                    </button>
                </div>
            </div>

            <div className="chatbot-messages">
                {messages.map((message, index) => (
                    <div 
                        key={index} 
                        className={`message ${message.role === 'user' ? 'user' : 'bot'}`}
                    >
                        <div className="message-content">
                            {message.content}
                        </div>
                        <div className="message-time">
                            {formatTime(message.timestamp)}
                        </div>
                    </div>
                ))}
                
                {isLoading && (
                    <div className="message bot">
                        <div className="message-content typing">
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

            <div className="chatbot-input">
                <div className="input-container">
                    <textarea
                        value={inputMessage}
                        onChange={(e) => setInputMessage(e.target.value)}
                        onKeyPress={handleKeyPress}
                        placeholder="Ask about hand positions, signs, or get help..."
                        rows={2}
                        disabled={isLoading}
                    />
                    <button 
                        className="send-button"
                        onClick={sendMessage}
                        disabled={!inputMessage.trim() || isLoading}
                    >
                        Send
                    </button>
                </div>
            </div>

            <div className="chatbot-suggestions">
                <button 
                    className="suggestion-chip"
                    onClick={() => handleSuggestionClick("How should I position my hands?")}
                >
                    Hand position help
                </button>
                <button 
                    className="suggestion-chip"
                    onClick={() => handleSuggestionClick("I'm having trouble with this sign")}
                >
                    Sign difficulty
                </button>
                <button 
                    className="suggestion-chip"
                    onClick={() => handleSuggestionClick("What does this sign mean?")}
                >
                    Sign meaning
                </button>
                <button 
                    className="suggestion-chip"
                    onClick={() => handleSuggestionClick("Can you give me practice tips?")}
                >
                    Practice tips
                </button>
            </div>
        </div>
    );
};

export default ChatbotWidget;
