# python-ai/learning/chatbot_adapter.py
import openai
import json
import logging
from typing import Dict, List, Any, Optional
from datetime import datetime
import os
from dataclasses import dataclass

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@dataclass
class ChatMessage:
    role: str  # 'system', 'user', 'assistant'
    content: str
    timestamp: Optional[datetime] = None

class ChatbotAdapter:
    """
    OpenAI GPT adapter for Bangla Sign Language learning assistance
    """
    
    def __init__(self, api_key: str = None):
        self.api_key = api_key or os.getenv('OPENAI_API_KEY')
        if not self.api_key:
            logger.warning("No OpenAI API key provided. Chatbot will use fallback responses.")
            self.openai_available = False
        else:
            openai.api_key = self.api_key
            self.openai_available = True
            logger.info("ChatbotAdapter initialized with OpenAI API")
        
        # System prompt for Bangla sign language learning
        self.system_prompt = self._create_system_prompt()
        
        # Fallback responses for when OpenAI is not available
        self.fallback_responses = self._load_fallback_responses()
        
        # Conversation memory (simple in-memory storage)
        self.conversation_history = {}
    
    def _create_system_prompt(self) -> str:
        """Create the system prompt for the AI assistant"""
        return """You are SignHelper, a friendly and knowledgeable Bangla sign language learning assistant. Your role is to help users learn Bangla sign language effectively.

Your guidelines:
- Be encouraging, patient, and supportive
- Provide specific, actionable feedback about hand positions and movements
- Use simple, clear language that's easy to understand
- Break down complex signs into smaller, manageable steps
- Share cultural context about Bangla sign language when relevant
- Keep responses concise (2-3 sentences maximum)
- Always maintain a positive and motivating tone
- If users are struggling, offer alternative practice methods
- Encourage regular practice and patience

You can help with:
- Hand positioning and movement corrections
- Sign explanations and meanings
- Practice tips and techniques
- Cultural insights about Bangla sign language
- Motivation and encouragement
- Breaking down difficult signs into steps

Remember: You're helping people learn a beautiful language that connects the deaf community in Bangladesh. Every learner is on their own journey, so be patient and supportive."""

    def _load_fallback_responses(self) -> Dict[str, List[str]]:
        """Load fallback responses for when OpenAI is not available"""
        return {
            "greeting": [
                "Hello! I'm here to help you learn Bangla sign language. What would you like to practice today?",
                "Welcome to your sign language learning session! How can I assist you?",
                "Hi there! Ready to learn some Bangla signs? I'm here to help!"
            ],
            "help": [
                "I'm here to help! Can you tell me which specific sign you're having trouble with?",
                "I'd be happy to assist you. What aspect of sign language learning would you like help with?",
                "Let me help you out! Are you working on a specific sign or do you have a general question?"
            ],
            "hand_position": [
                "For better hand positioning, make sure your fingers are clearly visible and movements are distinct. Practice slowly first!",
                "Focus on hand placement. Keep your hands in the camera's view and make deliberate movements.",
                "Remember to keep your hands steady and clearly visible. Good lighting helps too!"
            ],
            "encouragement": [
                "You're doing great! Keep practicing and you'll improve with each session.",
                "Don't worry about mistakes - they're part of learning! Keep up the good work.",
                "Every expert was once a beginner. You're making progress with each practice session!"
            ],
            "cultural": [
                "Bangla sign language is used by over 200,000 deaf individuals in Bangladesh. It has its own rich grammar and cultural expressions!",
                "Learning Bangla sign language helps build bridges in our community and supports inclusive communication.",
                "Sign language is a complete, natural language with its own unique grammar and cultural nuances."
            ],
            "practice_tips": [
                "Try practicing in front of a mirror to see your signs clearly.",
                "Practice regularly for short periods rather than long sessions.",
                "Start with basic signs and gradually work up to more complex ones."
            ],
            "default": [
                "I'm here to support your sign language learning! Feel free to ask about hand positions, sign meanings, or practice tips.",
                "I understand you're working on sign language! How can I help you improve your signing today?",
                "Let me know if you need help with any signs or have questions about your practice session."
            ]
        }
    
    def get_response(self, user_message: str, user_id: str = "default", 
                    lesson_context: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Get a response from the chatbot
        
        Args:
            user_message: The user's message
            user_id: Unique identifier for the user (for conversation history)
            lesson_context: Additional context about the current lesson
            
        Returns:
            Dict containing response and metadata
        """
        try:
            if self.openai_available:
                return self._get_openai_response(user_message, user_id, lesson_context)
            else:
                return self._get_fallback_response(user_message)
                
        except Exception as e:
            logger.error(f"Error getting chatbot response: {e}")
            return self._get_error_response(str(e))
    
    def _get_openai_response(self, user_message: str, user_id: str, 
                           lesson_context: Dict[str, Any] = None) -> Dict[str, Any]:
        """Get response from OpenAI GPT"""
        try:
            # Build conversation context
            messages = self._build_conversation_context(user_message, user_id, lesson_context)
            
            # Call OpenAI API
            response = openai.ChatCompletion.create(
                model="gpt-3.5-turbo",
                messages=messages,
                max_tokens=150,
                temperature=0.7,
                presence_penalty=0.1,
                frequency_penalty=0.1
            )
            
            assistant_message = response.choices[0].message.content.strip()
            
            # Update conversation history
            self._update_conversation_history(user_id, user_message, assistant_message)
            
            return {
                "content": assistant_message,
                "source": "openai",
                "timestamp": datetime.now(),
                "token_usage": response.usage.total_tokens if hasattr(response, 'usage') else 0
            }
            
        except openai.error.RateLimitError:
            logger.warning("OpenAI rate limit exceeded, using fallback")
            return self._get_fallback_response(user_message)
        except openai.error.AuthenticationError:
            logger.error("OpenAI authentication failed, check API key")
            return self._get_fallback_response(user_message)
        except Exception as e:
            logger.error(f"OpenAI API error: {e}")
            return self._get_fallback_response(user_message)
    
    def _build_conversation_context(self, user_message: str, user_id: str, 
                                  lesson_context: Dict[str, Any] = None) -> List[Dict[str, str]]:
        """Build conversation context for OpenAI"""
        messages = [{"role": "system", "content": self.system_prompt}]
        
        # Add lesson context if provided
        if lesson_context:
            context_message = f"Context: User is currently learning '{lesson_context.get('current_sign', 'unknown sign')}' in lesson '{lesson_context.get('lesson_title', 'Unknown Lesson')}'"
            messages.append({"role": "system", "content": context_message})
        
        # Add recent conversation history (last 5 exchanges)
        if user_id in self.conversation_history:
            recent_history = self.conversation_history[user_id][-10:]  # Last 10 messages (5 exchanges)
            for msg in recent_history:
                messages.append({
                    "role": msg.role,
                    "content": msg.content
                })
        
        # Add current user message
        messages.append({"role": "user", "content": user_message})
        
        return messages
    
    def _update_conversation_history(self, user_id: str, user_message: str, assistant_message: str):
        """Update conversation history for a user"""
        if user_id not in self.conversation_history:
            self.conversation_history[user_id] = []
        
        # Add user message
        self.conversation_history[user_id].append(
            ChatMessage("user", user_message, datetime.now())
        )
        
        # Add assistant message
        self.conversation_history[user_id].append(
            ChatMessage("assistant", assistant_message, datetime.now())
        )
        
        # Keep only last 20 messages (10 exchanges) per user
        if len(self.conversation_history[user_id]) > 20:
            self.conversation_history[user_id] = self.conversation_history[user_id][-20:]
    
    def _get_fallback_response(self, user_message: str) -> Dict[str, Any]:
        """Get fallback response when OpenAI is not available"""
        message_lower = user_message.lower()
        
        # Determine response category based on message content
        if any(word in message_lower for word in ['hello', 'hi', 'hey', 'start']):
            category = "greeting"
        elif any(word in message_lower for word in ['help', 'confused', "don't understand"]):
            category = "help"
        elif any(word in message_lower for word in ['hand', 'finger', 'position', 'placement']):
            category = "hand_position"
        elif any(word in message_lower for word in ['good', 'great', 'correct', 'right']):
            category = "encouragement"
        elif any(word in message_lower for word in ['culture', 'bangladesh', 'deaf', 'community']):
            category = "cultural"
        elif any(word in message_lower for word in ['practice', 'tip', 'advice', 'improve']):
            category = "practice_tips"
        else:
            category = "default"
        
        # Get random response from category
        import random
        responses = self.fallback_responses.get(category, self.fallback_responses["default"])
        selected_response = random.choice(responses)
        
        return {
            "content": selected_response,
            "source": "fallback",
            "timestamp": datetime.now(),
            "category": category
        }
    
    def _get_error_response(self, error_message: str) -> Dict[str, Any]:
        """Get error response"""
        return {
            "content": "I'm having trouble responding right now. Please try again in a moment, or feel free to continue practicing!",
            "source": "error",
            "timestamp": datetime.now(),
            "error": error_message
        }
    
    def clear_conversation_history(self, user_id: str):
        """Clear conversation history for a user"""
        if user_id in self.conversation_history:
            del self.conversation_history[user_id]
    
    def get_conversation_summary(self, user_id: str) -> Dict[str, Any]:
        """Get conversation summary for a user"""
        if user_id not in self.conversation_history:
            return {"message_count": 0, "last_interaction": None}
        
        history = self.conversation_history[user_id]
        user_messages = [msg for msg in history if msg.role == "user"]
        
        return {
            "message_count": len(user_messages),
            "total_exchanges": len(history) // 2,
            "last_interaction": history[-1].timestamp if history else None
        }

# Example Flask integration
from flask import Flask, request, jsonify
from flask_cors import CORS

def create_chatbot_service(api_key: str = None):
    """Create a Flask service for the chatbot"""
    app = Flask(__name__)
    CORS(app, origins=["http://localhost:3000", "http://localhost:8080"])
    
    chatbot = ChatbotAdapter(api_key)
    
    @app.route('/chat', methods=['POST'])
    def chat():
        try:
            data = request.json
            user_message = data.get('message', '')
            user_id = data.get('user_id', 'default')
            lesson_context = data.get('lesson_context', {})
            
            if not user_message:
                return jsonify({"error": "No message provided"}), 400
            
            response = chatbot.get_response(user_message, user_id, lesson_context)
            return jsonify(response)
            
        except Exception as e:
            logger.error(f"Chat endpoint error: {e}")
            return jsonify({"error": "Internal server error"}), 500
    
    @app.route('/chat/clear/<user_id>', methods=['POST'])
    def clear_chat(user_id):
        try:
            chatbot.clear_conversation_history(user_id)
            return jsonify({"message": "Conversation history cleared"})
        except Exception as e:
            return jsonify({"error": str(e)}), 500
    
    @app.route('/chat/summary/<user_id>', methods=['GET'])
    def chat_summary(user_id):
        try:
            summary = chatbot.get_conversation_summary(user_id)
            return jsonify(summary)
        except Exception as e:
            return jsonify({"error": str(e)}), 500
    
    @app.route('/health', methods=['GET'])
    def health():
        return jsonify({
            "status": "healthy",
            "openai_available": chatbot.openai_available,
            "service": "chatbot_adapter"
        })
    
    return app

# Example usage
if __name__ == "__main__":
    # Get API key from environment or use None for fallback mode
    api_key = os.getenv('OPENAI_API_KEY')
    
    # Create and run the service
    app = create_chatbot_service(api_key)
    app.run(host='0.0.0.0', port=5001, debug=True)
