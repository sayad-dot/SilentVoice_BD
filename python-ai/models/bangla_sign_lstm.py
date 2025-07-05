import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Dropout, BatchNormalization
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.callbacks import EarlyStopping, ModelCheckpoint
import numpy as np
import json
import pickle

class BanglaSignLanguageLSTM:
    def __init__(self, sequence_length=30, num_classes=47):
        self.sequence_length = sequence_length
        self.num_classes = num_classes
        self.feature_dim = 258  # MediaPipe features: 21*3 + 21*3 + 33*4 + 10*3
        self.model = None
        self.class_labels = self._get_bangla_labels()
        
    def _get_bangla_labels(self):
        """Define Bangla sign language labels based on BdSL47 dataset"""
        return [
            # Bangla alphabet (39 characters)
            'অ', 'আ', 'ই', 'ঈ', 'উ', 'ঊ', 'ঋ', 'এ', 'ঐ', 'ও', 'ঔ',
            'ক', 'খ', 'গ', 'ঘ', 'ঙ', 'চ', 'ছ', 'জ', 'ঝ', 'ঞ',
            'ট', 'ঠ', 'ড', 'ঢ', 'ণ', 'ত', 'থ', 'দ', 'ধ', 'ন',
            'প', 'ফ', 'ব', 'ভ', 'ম', 'য', 'র', 'ল',
            'শ', 'ষ', 'স', 'হ', 'ড়', 'ঢ়', 'য়', 'ৎ',
            # Common words
            'হ্যালো'  # Hello
        ]
    
    def build_model(self):
        """Build LSTM model architecture"""
        self.model = Sequential([
            # First LSTM layer
            LSTM(128, return_sequences=True, 
                 input_shape=(self.sequence_length, self.feature_dim)),
            BatchNormalization(),
            Dropout(0.3),
            
            # Second LSTM layer  
            LSTM(64, return_sequences=True),
            BatchNormalization(),
            Dropout(0.3),
            
            # Third LSTM layer
            LSTM(32, return_sequences=False),
            BatchNormalization(),
            Dropout(0.2),
            
            # Dense layers
            Dense(64, activation='relu'),
            Dropout(0.2),
            Dense(32, activation='relu'),
            Dense(self.num_classes, activation='softmax')
        ])
        
        # Compile model
        self.model.compile(
            optimizer=Adam(learning_rate=0.001),
            loss='categorical_crossentropy',
            metrics=['accuracy']
        )
        
        return self.model
    
    def train_model(self, X_train, y_train, X_val, y_val, epochs=150):
        """Train the LSTM model"""
        if self.model is None:
            self.build_model()
        
        # Callbacks
        early_stopping = EarlyStopping(
            monitor='val_accuracy',
            patience=15,
            restore_best_weights=True
        )
        
        model_checkpoint = ModelCheckpoint(
            'python-ai/trained_models/best_bangla_sign_model.h5',
            monitor='val_accuracy',
            save_best_only=True,
            verbose=1
        )
        
        # Train model
        history = self.model.fit(
            X_train, y_train,
            validation_data=(X_val, y_val),
            epochs=epochs,
            batch_size=32,
            callbacks=[early_stopping, model_checkpoint],
            verbose=1
        )
        
        return history
    
    def predict(self, pose_sequence):
        """Predict sign language from pose sequence"""
        if self.model is None:
            raise ValueError("Model not loaded. Call load_model() first.")
        
        # Ensure sequence is correct length
        sequence = self._normalize_sequence_length(pose_sequence)
        
        # Reshape for prediction
        X = np.array(sequence).reshape(1, self.sequence_length, self.feature_dim)
        
        # Make prediction
        predictions = self.model.predict(X, verbose=0)
        
        # Get predicted class and confidence
        predicted_class = np.argmax(predictions[0])
        confidence = predictions[0][predicted_class]
        
        result = {
            "predicted_text": self.class_labels[predicted_class],
            "confidence": float(confidence),
            "all_predictions": predictions[0].tolist(),
            "class_index": int(predicted_class)
        }
        
        return result
    
    def _normalize_sequence_length(self, sequence):
        """Normalize sequence to required length"""
        if len(sequence) == self.sequence_length:
            return sequence
        elif len(sequence) > self.sequence_length:
            # Downsample
            step = len(sequence) / self.sequence_length
            return [sequence[int(i * step)] for i in range(self.sequence_length)]
        else:
            # Upsample by repeating last frame
            normalized = sequence.copy()
            while len(normalized) < self.sequence_length:
                normalized.append(sequence[-1] if sequence else [0] * self.feature_dim)
            return normalized
    
    def save_model(self, model_path):
        """Save trained model"""
        if self.model is not None:
            self.model.save(model_path)
            
            # Save class labels
            labels_path = model_path.replace('.h5', '_labels.json')
            with open(labels_path, 'w', encoding='utf-8') as f:
                json.dump(self.class_labels, f, ensure_ascii=False, indent=2)
    
    def load_model(self, model_path):
        """Load trained model"""
        self.model = tf.keras.models.load_model(model_path)
        
        # Load class labels
        labels_path = model_path.replace('.h5', '_labels.json')
        try:
            with open(labels_path, 'r', encoding='utf-8') as f:
                self.class_labels = json.load(f)
        except FileNotFoundError:
            print("Warning: Class labels file not found. Using default labels.")

# Create a simple demo model for testing
def create_demo_model():
    """Create and save a demo model for testing"""
    model = BanglaSignLanguageLSTM()
    model.build_model()
    
    # Create dummy training data
    X_dummy = np.random.random((100, 30, 258))
    y_dummy = tf.keras.utils.to_categorical(np.random.randint(0, 47, (100,)), 47)
    
    # Train for a few epochs just to have a working model
    model.model.fit(X_dummy, y_dummy, epochs=5, verbose=0)
    
    # Save the demo model
    model.save_model('python-ai/trained_models/demo_bangla_sign_model.h5')
    print("Demo model created and saved!")

if __name__ == "__main__":
    create_demo_model()
