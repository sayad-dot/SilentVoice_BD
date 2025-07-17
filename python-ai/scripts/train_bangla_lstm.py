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
import gc

# Configure TensorFlow for memory efficiency
tf.config.threading.set_inter_op_parallelism_threads(2)
tf.config.threading.set_intra_op_parallelism_threads(4)

class BanglaSignLSTMTrainer:
    def __init__(self, data_file='../data/training_data.json'):
        self.data_file = data_file
        self.model = None
        self.label_encoder = None
        self.sequence_length = 30
        self.feature_dim = 288
        
    def validate_sequence_data(self, X, max_samples_to_check=100):
        """Validate sequence data structure and report issues"""
        print("Validating sequence data...")
        
        issues_found = 0
        for i, sequence in enumerate(X[:max_samples_to_check]):
            if not isinstance(sequence, list):
                print(f"Warning: Sequence {i} is not a list: {type(sequence)}")
                issues_found += 1
                continue
                
            if len(sequence) == 0:
                print(f"Warning: Sequence {i} is empty")
                issues_found += 1
                continue
                
            # Check frame structure
            for j, frame in enumerate(sequence[:5]):  # Check first 5 frames
                if not isinstance(frame, list):
                    print(f"Warning: Sequence {i}, frame {j} is not a list: {type(frame)}")
                    issues_found += 1
                    break
                    
                if len(frame) != self.feature_dim:
                    print(f"Warning: Sequence {i}, frame {j} has {len(frame)} features (expected {self.feature_dim})")
                    issues_found += 1
                    break
        
        if issues_found == 0:
            print("✅ Data validation passed!")
        else:
            print(f"⚠️ Found {issues_found} data issues in first {max_samples_to_check} samples")
        
        return issues_found == 0
        
    def load_and_prepare_data(self):
        """Load training data and prepare for model training with memory optimization"""
        print("Loading training data...")
        
        try:
            with open(self.data_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
        except FileNotFoundError:
            raise FileNotFoundError(f"Training data file not found: {self.data_file}")
        except json.JSONDecodeError as e:
            raise ValueError(f"Invalid JSON in training data file: {e}")
        
        # *** NEW: Check if data is normalized ***
        if data.get('normalized', False):
            print("✅ Training data is normalized")
        else:
            print("⚠️ WARNING: Training data may not be normalized!")
            print("This could lead to poor model performance.")
        
        # Keep as lists initially - DO NOT convert to numpy array yet
        X = data['X']
        y = data['y']
        
        print(f"Loaded {len(X)} samples with {len(set(y))} unique labels")
        
        # Validate data structure
        if not self.validate_sequence_data(X):
            print("⚠️ Data validation failed, but continuing with processing...")
        
        # Get sequence length statistics
        sequence_lengths = [len(seq) for seq in X]
        max_seq_length = max(sequence_lengths)
        min_seq_length = min(sequence_lengths)
        avg_seq_length = sum(sequence_lengths) / len(sequence_lengths)
        
        print(f"Sequence lengths: {min_seq_length} to {max_seq_length} (avg: {avg_seq_length:.1f})")
        
        # Process sequences in batches to manage memory
        print("Processing sequences (padding/truncating)...")
        X_processed = []
        batch_size = 500  # Process in smaller batches
        
        for batch_start in range(0, len(X), batch_size):
            batch_end = min(batch_start + batch_size, len(X))
            batch_X = X[batch_start:batch_end]
            
            print(f"Processing batch {batch_start//batch_size + 1}/{(len(X) + batch_size - 1)//batch_size}")
            
            batch_processed = []
            for i, sequence in enumerate(batch_X):
                try:
                    if len(sequence) > self.sequence_length:
                        # Truncate
                        processed_seq = sequence[:self.sequence_length]
                    elif len(sequence) < self.sequence_length:
                        # Pad with zeros
                        padding_needed = self.sequence_length - len(sequence)
                        padding = [[0.0] * self.feature_dim] * padding_needed
                        processed_seq = sequence + padding
                    else:
                        processed_seq = sequence
                    
                    # Validate processed sequence
                    if len(processed_seq) != self.sequence_length:
                        raise ValueError(f"Processed sequence length mismatch: {len(processed_seq)} != {self.sequence_length}")
                    
                    # Ensure all frames have correct feature dimension
                    for frame_idx, frame in enumerate(processed_seq):
                        if len(frame) != self.feature_dim:
                            # Pad or truncate frame if needed
                            if len(frame) < self.feature_dim:
                                frame.extend([0.0] * (self.feature_dim - len(frame)))
                            elif len(frame) > self.feature_dim:
                                processed_seq[frame_idx] = frame[:self.feature_dim]
                    
                    batch_processed.append(processed_seq)
                    
                except Exception as e:
                    print(f"Error processing sequence {batch_start + i}: {e}")
                    # Create a zero-filled sequence as fallback
                    zero_sequence = [[0.0] * self.feature_dim] * self.sequence_length
                    batch_processed.append(zero_sequence)
            
            X_processed.extend(batch_processed)
            
            # Periodic garbage collection
            if batch_start % (batch_size * 5) == 0:
                gc.collect()
        
        print("Converting to numpy array...")
        try:
            X_processed = np.array(X_processed, dtype=np.float32)
        except Exception as e:
            print(f"Error converting to numpy array: {e}")
            print("Attempting to fix data structure...")
            
            # Fix any remaining issues
            fixed_X = []
            for seq in X_processed:
                if len(seq) == self.sequence_length:
                    fixed_seq = []
                    for frame in seq:
                        if len(frame) == self.feature_dim:
                            fixed_seq.append(frame)
                        else:
                            # Fix frame
                            fixed_frame = list(frame)[:self.feature_dim]
                            while len(fixed_frame) < self.feature_dim:
                                fixed_frame.append(0.0)
                            fixed_seq.append(fixed_frame)
                    fixed_X.append(fixed_seq)
                else:
                    # Create zero sequence
                    fixed_X.append([[0.0] * self.feature_dim] * self.sequence_length)
            
            X_processed = np.array(fixed_X, dtype=np.float32)
        
        # *** NEW: Log data statistics for verification ***
        print(f"Data statistics after loading:")
        print(f"  Mean: {X_processed.mean():.6f} (should be ~0 for normalized data)")
        print(f"  Std: {X_processed.std():.6f} (should be ~1 for normalized data)")
        print(f"  Range: {X_processed.min():.6f} to {X_processed.max():.6f}")
        
        # Encode labels
        print("Encoding labels...")
        self.label_encoder = LabelEncoder()
        y_encoded = self.label_encoder.fit_transform(y)
        y_categorical = tf.keras.utils.to_categorical(y_encoded)
        
        print(f"Final data shape: {X_processed.shape}")
        print(f"Labels shape: {y_categorical.shape}")
        print(f"Number of classes: {y_categorical.shape[1]}")
        
        # Validate final shapes
        expected_shape = (len(X), self.sequence_length, self.feature_dim)
        if X_processed.shape != expected_shape:
            raise ValueError(f"Final data shape mismatch: {X_processed.shape} != {expected_shape}")
        
        # Clear memory
        del X, data
        gc.collect()
        
        return X_processed, y_categorical, y_encoded
    
    def build_model(self, num_classes):
        """Build LSTM model architecture with memory optimization"""
        print("Building LSTM model...")
        
        model = Sequential([
            # *** FIXED: Use self.feature_dim instead of hardcoded 288 ***
            LSTM(64, return_sequences=True, input_shape=(self.sequence_length, self.feature_dim)),
            Dropout(0.3),
            BatchNormalization(),
            
            LSTM(32, return_sequences=True),
            Dropout(0.3),
            BatchNormalization(),
            
            LSTM(16),
            Dropout(0.3),
            BatchNormalization(),
            
            Dense(32, activation='relu'),
            Dropout(0.2),
            
            Dense(num_classes, activation='softmax')
        ])
        
        # Use a lower learning rate for better convergence
        optimizer = tf.keras.optimizers.Adam(learning_rate=0.001)
        
        model.compile(
            optimizer=optimizer,
            loss='categorical_crossentropy',
            metrics=['accuracy']
        )
        
        self.model = model
        return model
    
    def train_model(self, X, y, validation_split=0.15, epochs=30, batch_size=16):
        """Train the LSTM model with memory-efficient settings"""
        print("Starting model training...")
        
        # Split data with stratification
        X_train, X_val, y_train, y_val = train_test_split(
            X, y, test_size=validation_split, random_state=42, 
            stratify=np.argmax(y, axis=1)
        )
        
        print(f"Training samples: {len(X_train)}")
        print(f"Validation samples: {len(X_val)}")
        
        # Callbacks with adjusted patience
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
                patience=8,
                restore_best_weights=True,
                verbose=1
            ),
            ReduceLROnPlateau(
                monitor='val_loss',
                factor=0.5,
                patience=4,
                min_lr=1e-6,
                verbose=1
            )
        ]
        
        # Train model with memory-efficient batch size
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
            'feature_dim': self.feature_dim,
            'num_classes': len(self.label_encoder.classes_)
        }
        
        with open('../trained_models/model_config.json', 'w', encoding='utf-8') as f:
            json.dump(label_mapping, f, ensure_ascii=False, indent=2)
        print("Model config saved to ../trained_models/model_config.json")

def main():
    """Train Bangla sign language LSTM model"""
    print("🚀 Starting Bangla Sign Language LSTM Training")
    print("=" * 50)
    
    try:
        trainer = BanglaSignLSTMTrainer()
        
        # Load and prepare data
        print("\n📊 Loading and preparing data...")
        X, y, y_encoded = trainer.load_and_prepare_data()
        
        # Build model
        print("\n🏗️ Building model...")
        num_classes = y.shape[1]
        trainer.build_model(num_classes)
        
        # Print model summary
        print("\n📋 Model Architecture:")
        trainer.model.summary()
        
        # Train model with reduced epochs and batch size for memory efficiency
        print("\n🎯 Training model...")
        history = trainer.train_model(X, y, epochs=30, batch_size=16)
        
        # Save model and encoder
        print("\n💾 Saving model...")
        trainer.save_model_and_encoder()
        
        # Print final metrics
        print("\n✅ Training completed!")
        print("=" * 50)
        print(f"Final training accuracy: {history.history['accuracy'][-1]:.4f}")
        print(f"Final validation accuracy: {history.history['val_accuracy'][-1]:.4f}")
        
        print("\n📁 Files created:")
        print("- ../trained_models/bangla_lstm_model.h5")
        print("- ../trained_models/bangla_lstm_best.h5") 
        print("- ../trained_models/label_encoder.pkl")
        print("- ../trained_models/model_config.json")
        
        print("\n🎉 Your Bangla Sign Language model is ready for deployment!")
        
    except Exception as e:
        print(f"\n❌ Training failed with error: {e}")
        print("Please check your data file and try again.")
        raise

if __name__ == "__main__":
    main()
