import queue
import threading
import time
import cv2
import numpy as np
from collections import deque
import json

class FrameQueueManager:
    """
    Manages frame queues for real-time processing
    Handles frame buffering, preprocessing, and batch management
    """
    
    def __init__(self, max_queue_size=50, sequence_length=30):
        self.max_queue_size = max_queue_size
        self.sequence_length = sequence_length
        
        # Thread-safe queues for each session
        self.session_queues = {}
        self.session_sequences = {}
        self.session_locks = {}
        
        # Processing statistics
        self.stats = {
            'frames_processed': 0,
            'frames_dropped': 0,
            'active_sessions': 0
        }
        
        # Cleanup thread
        self.cleanup_thread = None
        self.cleanup_running = False
        self.start_cleanup_thread()
    
    def create_session(self, session_id):
        """Create a new session with its own queue and sequence buffer"""
        if session_id not in self.session_queues:
            self.session_queues[session_id] = queue.Queue(maxsize=self.max_queue_size)
            self.session_sequences[session_id] = deque(maxlen=self.sequence_length)
            self.session_locks[session_id] = threading.Lock()
            self.stats['active_sessions'] += 1
            print(f"Created session: {session_id}")
    
    def add_frame(self, session_id, frame_data, timestamp):
        """Add a frame to the session queue"""
        if session_id not in self.session_queues:
            self.create_session(session_id)
        
        frame_info = {
            'data': frame_data,
            'timestamp': timestamp,
            'session_id': session_id
        }
        
        try:
            # Add frame to queue (non-blocking)
            self.session_queues[session_id].put_nowait(frame_info)
            return True
        except queue.Full:
            # Queue is full, drop the frame
            self.stats['frames_dropped'] += 1
            print(f"Queue full for session {session_id}, dropping frame")
            return False
    
    def get_frame(self, session_id, timeout=1.0):
        """Get the next frame from session queue"""
        if session_id not in self.session_queues:
            return None
        
        try:
            frame_info = self.session_queues[session_id].get(timeout=timeout)
            self.stats['frames_processed'] += 1
            return frame_info
        except queue.Empty:
            return None
    
    def add_to_sequence(self, session_id, landmarks):
        """Add processed landmarks to the session sequence buffer"""
        if session_id not in self.session_sequences:
            self.create_session(session_id)
        
        with self.session_locks[session_id]:
            self.session_sequences[session_id].append(landmarks)
    
    def get_sequence(self, session_id):
        """Get the current sequence for prediction"""
        if session_id not in self.session_sequences:
            return None
        
        with self.session_locks[session_id]:
            if len(self.session_sequences[session_id]) >= self.sequence_length:
                # Return a copy of the sequence as numpy array
                return np.array(list(self.session_sequences[session_id]))
            else:
                return None
    
    def get_sequence_progress(self, session_id):
        """Get the current sequence progress (how many frames we have)"""
        if session_id not in self.session_sequences:
            return 0
        
        with self.session_locks[session_id]:
            return len(self.session_sequences[session_id])
    
    def clear_session(self, session_id):
        """Clear all data for a session"""
        if session_id in self.session_queues:
            # Clear the queue
            while not self.session_queues[session_id].empty():
                try:
                    self.session_queues[session_id].get_nowait()
                except queue.Empty:
                    break
            
            # Clear sequence
            with self.session_locks[session_id]:
                self.session_sequences[session_id].clear()
    
    def remove_session(self, session_id):
        """Completely remove a session"""
        if session_id in self.session_queues:
            self.clear_session(session_id)
            del self.session_queues[session_id]
            del self.session_sequences[session_id]
            del self.session_locks[session_id]
            self.stats['active_sessions'] -= 1
            print(f"Removed session: {session_id}")
    
    def get_queue_size(self, session_id):
        """Get current queue size for a session"""
        if session_id in self.session_queues:
            return self.session_queues[session_id].qsize()
        return 0
    
    def preprocess_frame(self, frame_data):
        """
        Preprocess frame data for pose extraction
        Convert base64 to opencv image
        """
        try:
            # Remove data URL prefix if present
            if ',' in frame_data:
                frame_data = frame_data.split(',')[1]
            
            # Decode base64 to bytes
            img_bytes = np.frombuffer(
                np.base64.b64decode(frame_data), 
                dtype=np.uint8
            )
            
            # Convert to opencv image
            image = cv2.imdecode(img_bytes, cv2.IMREAD_COLOR)
            
            if image is None:
                raise ValueError("Could not decode image")
            
            # Resize for consistent processing (optional optimization)
            image = cv2.resize(image, (640, 480))
            
            return image
            
        except Exception as e:
            print(f"Frame preprocessing error: {e}")
            return None
    
    def start_cleanup_thread(self):
        """Start background thread for session cleanup"""
        self.cleanup_running = True
        self.cleanup_thread = threading.Thread(target=self._cleanup_worker, daemon=True)
        self.cleanup_thread.start()
    
    def _cleanup_worker(self):
        """Background worker to clean up inactive sessions"""
        while self.cleanup_running:
            time.sleep(30)  # Check every 30 seconds
            
            # Find sessions with empty queues for extended periods
            sessions_to_remove = []
            for session_id in list(self.session_queues.keys()):
                if self.session_queues[session_id].empty():
                    # Mark for removal if consistently empty
                    # In a real implementation, you'd track last activity time
                    pass
            
            # Clean up old sessions (implement your logic here)
            # For now, we'll keep sessions until explicitly removed
    
    def stop_cleanup_thread(self):
        """Stop the cleanup thread"""
        self.cleanup_running = False
        if self.cleanup_thread:
            self.cleanup_thread.join()
    
    def get_stats(self):
        """Get processing statistics"""
        return self.stats.copy()
    
    def reset_stats(self):
        """Reset processing statistics"""
        self.stats = {
            'frames_processed': 0,
            'frames_dropped': 0,
            'active_sessions': self.stats['active_sessions']
        }

# Global instance for use across the application
frame_queue_manager = FrameQueueManager()

def get_frame_queue_manager():
    """Get the global frame queue manager instance"""
    return frame_queue_manager
