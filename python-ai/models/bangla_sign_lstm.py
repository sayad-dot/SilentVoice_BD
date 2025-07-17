#!/usr/bin/env python3
import numpy as np
import tensorflow as tf
from keras.models import Sequential, load_model
from keras.layers import LSTM, Dense, Dropout, BatchNormalization
import pickle
import json
import os

class BanglaSignLanguageLSTM:
    def __init__(self, sequence_length=30, num_classes=61):
        self.sequence_length = sequence_length
        self.num_classes = num_classes
        self.feature_dim = 288
        self.model = None
        self.label_encoder = None
        
    def build_model(self):
        """Build LSTM model architecture"""
        model = Sequential([
            LSTM(128, return_sequences=True, input_shape=(self.sequence_length, self.feature_dim)),
            Dropout(0.3),
            BatchNormalization(),
            
            LSTM(64, return_sequences=True),
            Dropout(0.3),
            BatchNormalization(),
            
            LSTM(32),
            Dropout(0.3),
            BatchNormalization(),
            
            Dense(64, activation='relu'),
            Dropout(0.2),
            
            Dense(self.num_classes, activation='softmax')
        ])
        
        model.compile(
            optimizer='adam',
            loss='categorical_crossentropy',
            metrics=['accuracy']
        )
        
        self.model = model
        return model
    
    def create_demo_model(self):
        """Create a demo model for testing when real model is not available"""
        if self.model is None:
            self.build_model()
        
        # Initialize with random weights for demo purposes
        dummy_input = np.random.random((1, self.sequence_length, self.feature_dim))
        _ = self.model.predict(dummy_input, verbose=0)
        
        # Save demo model
        os.makedirs('../trained_models', exist_ok=True)
        self.model.save('../trained_models/demo_bangla_sign_model.h5')
        
        return self.model
    
    def load_model(self, model_path):
        """Load trained model"""
        try:
            self.model = load_model(model_path)
            return True
        except Exception as e:
            print(f"Error loading model: {e}")
            return False
    
    def save_model(self, model_path):
        """Save trained model"""
        if self.model is not None:
            self.model.save(model_path)
            return True
        return False
    
    def predict(self, sequence):
        """Make prediction on pose sequence"""
        if self.model is None:
            raise ValueError("Model not loaded or built")
        
        # Ensure sequence has correct shape
        if len(sequence) != self.sequence_length:
            # Pad or truncate sequence
            if len(sequence) > self.sequence_length:
                sequence = sequence[:self.sequence_length]
            else:
                padding = [[0] * self.feature_dim] * (self.sequence_length - len(sequence))
                sequence = sequence + padding
        
        # Convert to numpy array and add batch dimension
        X = np.array([sequence])
        
        # Make prediction
        predictions = self.model.predict(X, verbose=0)
        predicted_class = np.argmax(predictions[0])
        confidence = float(predictions[0][predicted_class])
        
        return predicted_class, confidence, predictions[0]
