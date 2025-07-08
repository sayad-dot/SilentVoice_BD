#!/usr/bin/env python3
import json
import numpy as np
import tensorflow as tf
from keras.models import Sequential
from keras.layers import LSTM, Dense, Dropout, BatchNormalization
from keras.callbacks import ModelCheckpoint, EarlyStopping, ReduceLROnPlateau
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import os
import pickle

class BanglaSignLSTMTrainer:
    def __init__(self, data_file='../data/training_data.json'):
        self.data_file = data_file
        self.model = None
        self.label_encoder = None
        self.sequence_length = 30
        self.feature_dim = 258
        
    def load_and_prepare_data(self):
        """Load training data and prepare for model training"""
        print("Loading training data...")
        
        with open(self.data_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        X = np.array(data['X'])
        y = np.array(data['y'])
        
        print(f"Loaded {len(X)} samples with {len(set(y))} unique labels")
        
        # Normalize sequence lengths
        max_seq_length = max(len(seq) for seq in X)
        min_seq_length = min(len(seq) for seq in X)
        
        print(f"Sequence lengths: {min_seq_length} to {max_seq_length}")
        
        # Pad or truncate sequences to fixed length
        X_processed = []
        for sequence in X:
            if len(sequence) > self.sequence_length:
                # Truncate
                X_processed.append(sequence[:self.sequence_length])
            elif len(sequence) < self.sequence_length:
                # Pad with zeros
                padding = [[0] * self.feature_dim] * (self.sequence_length - len(sequence))
                X_processed.append(sequence + padding)
            else:
                X_processed.append(sequence)
        
        X_processed = np.array(X_processed)
        
        # Encode labels
        self.label_encoder = LabelEncoder()
        y_encoded = self.label_encoder.fit_transform(y)
        y_categorical = tf.keras.utils.to_categorical(y_encoded)
        
        print(f"Data shape: {X_processed.shape}")
        print(f"Labels shape: {y_categorical.shape}")
        print(f"Number of classes: {y_categorical.shape[1]}")
        
        return X_processed, y_categorical, y_encoded
    
    def build_model(self, num_classes):
        """Build LSTM model architecture"""
        print("Building LSTM model...")
        
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
            
            Dense(num_classes, activation='softmax')
        ])
        
        model.compile(
            optimizer='adam',
            loss='categorical_crossentropy',
            metrics=['accuracy']
        )
        
        self.model = model
        return model
    
    def train_model(self, X, y, validation_split=0.2, epochs=50, batch_size=32):
        """Train the LSTM model"""
        print("Starting model training...")
        
        # Split data
        X_train, X_val, y_train, y_val = train_test_split(
            X, y, test_size=validation_split, random_state=42, stratify=np.argmax(y, axis=1)
        )
        
        print(f"Training samples: {len(X_train)}")
        print(f"Validation samples: {len(X_val)}")
        
        # Callbacks
        callbacks = [
            ModelCheckpoint(
                '../trained_models/bangla_lstm_best.h5',
                monitor='val_accuracy',
                save_best_only=True,
                mode='max',
                verbose=1
            ),
            EarlyStopping(
                monitor='val_loss',
                patience=10,
                restore_best_weights=True,
                verbose=1
            ),
            ReduceLROnPlateau(
                monitor='val_loss',
                factor=0.5,
                patience=5,
                min_lr=1e-7,
                verbose=1
            )
        ]
        
        # Train model
        history = self.model.fit(
            X_train, y_train,
            validation_data=(X_val, y_val),
            epochs=epochs,
            batch_size=batch_size,
            callbacks=callbacks,
            verbose=1
        )
        
        return history
    
    def save_model_and_encoder(self):
        """Save trained model and label encoder"""
        os.makedirs('../trained_models', exist_ok=True)
        
        # Save model
        self.model.save('../trained_models/bangla_lstm_model.h5')
        print("Model saved to ../trained_models/bangla_lstm_model.h5")
        
        # Save label encoder
        with open('../trained_models/label_encoder.pkl', 'wb') as f:
            pickle.dump(self.label_encoder, f)
        print("Label encoder saved to ../trained_models/label_encoder.pkl")
        
        # Save label mapping
        label_mapping = {
            'classes': self.label_encoder.classes_.tolist(),
            'sequence_length': self.sequence_length,
            'feature_dim': self.feature_dim
        }
        
        with open('../trained_models/model_config.json', 'w', encoding='utf-8') as f:
            json.dump(label_mapping, f, ensure_ascii=False, indent=2)
        print("Model config saved to ../trained_models/model_config.json")

def main():
    """Train Bangla sign language LSTM model"""
    trainer = BanglaSignLSTMTrainer()
    
    # Load and prepare data
    X, y, y_encoded = trainer.load_and_prepare_data()
    
    # Build model
    num_classes = y.shape[1]
    trainer.build_model(num_classes)
    
    # Print model summary
    print("\nModel Architecture:")
    trainer.model.summary()
    
    # Train model
    history = trainer.train_model(X, y, epochs=50, batch_size=32)
    
    # Save model and encoder
    trainer.save_model_and_encoder()
    
    # Print final metrics
    print("\nTraining completed!")
    print(f"Final training accuracy: {history.history['accuracy'][-1]:.4f}")
    print(f"Final validation accuracy: {history.history['val_accuracy'][-1]:.4f}")
    
    print("\nFiles created:")
    print("- ../trained_models/bangla_lstm_model.h5")
    print("- ../trained_models/bangla_lstm_best.h5") 
    print("- ../trained_models/label_encoder.pkl")
    print("- ../trained_models/model_config.json")

if __name__ == "__main__":
    main()
