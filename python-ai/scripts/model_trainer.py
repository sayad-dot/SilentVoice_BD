#!/usr/bin/env python3
"""
Model Training and Improvement Script for SilentVoice_BD
- Enhanced LSTM with Attention
- Data augmentation techniques
- Comprehensive normalization
- Model validation and testing
"""

import os
import sys
import json
import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
import logging
from pathlib import Path
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
import matplotlib.pyplot as plt
from typing import Dict, List, Tuple, Optional
import pickle
import pathlib

# Directory containing per-class .npy sequence files
data_dir = pathlib.Path(__file__).parent.parent / 'data' / 'sequences'

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class EnhancedModelTrainer:
    """Enhanced model trainer with attention mechanism and dynamic class discovery"""

    def __init__(self, config_path: Optional[str] = None):
        self.config = self._load_config(config_path)
        self.model = None
        self.scaler = StandardScaler()
        self.history = None

        # Model parameters
        self.sequence_length = self.config.get('sequence_length', 30)
        self.feature_dim = self.config.get('feature_dim', 288)
        self.lstm_units = self.config.get('lstm_units', 128)
        self.attention_units = self.config.get('attention_units', 64)
        self.dropout_rate = self.config.get('dropout_rate', 0.3)

        # Training parameters
        self.batch_size = self.config.get('batch_size', 32)
        self.epochs = self.config.get('epochs', 100)
        self.learning_rate = self.config.get('learning_rate', 0.001)
        self.patience = self.config.get('patience', 15)

        # Dynamically discover classes from sequences directory
        base = Path(data_dir)
        self.vocabulary = sorted([p.name for p in base.iterdir() if p.is_dir()])
        self.num_classes = len(self.vocabulary)

        # Create mappings
        self.class_to_index = {cls: idx for idx, cls in enumerate(self.vocabulary)}
        self.index_to_class = {idx: cls for cls, idx in self.class_to_index.items()}

    def _load_config(self, config_path: Optional[str]) -> Dict:
        """Load training configuration from JSON or defaults."""
        default = {
            'sequence_length': 30,
            'feature_dim': 288,
            'lstm_units': 128,
            'attention_units': 64,
            'dropout_rate': 0.3,
            'batch_size': 32,
            'epochs': 100,
            'learning_rate': 0.001,
            'patience': 15,
            'enable_attention': True,
            'validation_split': 0.2
        }
        if config_path and os.path.exists(config_path):
            try:
                with open(config_path) as f:
                    user_conf = json.load(f)
                default.update(user_conf)
                logger.info(f"Loaded config from {config_path}")
            except Exception as e:
                logger.warning(f"Failed to load config: {e}")
        return default

    def create_attention_lstm_model(self) -> keras.Model:
        """Build and compile the attention-based LSTM model."""
        inputs = layers.Input((self.sequence_length, self.feature_dim), name='pose_sequence')
        x = layers.BatchNormalization(name='norm')(inputs)
        x = layers.LSTM(self.lstm_units, return_sequences=True, name='lstm1')(x)
        x = layers.LSTM(self.lstm_units, return_sequences=True, name='lstm2')(x)

        if self.config.get('enable_attention', True):
            q = layers.Dense(self.attention_units, name='q')(x)
            k = layers.Dense(self.attention_units, name='k')(x)
            v = layers.Dense(self.attention_units, name='v')(x)
            score = layers.Dot(axes=[2,2], name='score')([q,k])
            weights = layers.Activation('softmax', name='attn_weights')(score)
            x = layers.Dot(axes=[2,1], name='attn_out')([weights,v])
            x = layers.GlobalAveragePooling1D(name='attn_pool')(x)
        else:
            x = layers.GlobalAveragePooling1D(name='gap')(x)

        x = layers.Dense(self.lstm_units, activation='relu', name='dense1')(x)
        x = layers.Dropout(self.dropout_rate)(x)
        x = layers.BatchNormalization()(x)
        x = layers.Dense(self.lstm_units//2, activation='relu', name='dense2')(x)
        x = layers.Dropout(self.dropout_rate)(x)

        outputs = layers.Dense(self.num_classes, activation='softmax', name='output')(x)
        model = keras.Model(inputs, outputs, name='enhanced_lstm_attn')

        optimizer = keras.optimizers.Adam(self.learning_rate)
        model.compile(optimizer, loss='categorical_crossentropy',
                      metrics=['accuracy', 'top_k_categorical_accuracy'])
        logger.info("Model built:")
        model.summary(print_fn=logger.info)
        return model

    def load_data(self, training_dir: str) -> Tuple[np.ndarray, np.ndarray]:
        """Load .npy sequences and labels."""
        seqs, labels = [], []
        logger.info(f"Loading data from {training_dir} for classes: {self.vocabulary}")
        base = Path(training_dir)
        for cls in self.vocabulary:
            cls_dir = base/cls
            if not cls_dir.exists():
                logger.warning(f"Missing class directory: {cls_dir}")
                continue
            for npy in cls_dir.glob("*.npy"):
                try:
                    arr = np.load(npy)
                    if arr.shape == (self.sequence_length, self.feature_dim):
                        seqs.append(arr)
                        labels.append(self.class_to_index[cls])
                    else:
                        logger.warning(f"Skipping {npy}: wrong shape {arr.shape}")
                except Exception as e:
                    logger.warning(f"Error loading {npy}: {e}")
        
        if not seqs:
            raise ValueError("No valid data found.")
        
        X = np.stack(seqs)
        y = np.array(labels)
        logger.info(f"Loaded {len(X)} sequences.")
        return X, y

    def preprocess_data(self, X: np.ndarray, y: np.ndarray
                       ) -> Tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
        """Normalize and split data."""
        # Check class distribution
        unique_classes, counts = np.unique(y, return_counts=True)
        min_samples = np.min(counts)
        classes_with_one_sample = unique_classes[counts == 1]
        
        logger.info(f"Class distribution: min={min_samples}, max={np.max(counts)}, mean={np.mean(counts):.1f}")
        
        if len(classes_with_one_sample) > 0:
            logger.warning(f"Found {len(classes_with_one_sample)} classes with only 1 sample: {[self.index_to_class[i] for i in classes_with_one_sample]}")
        
        # Reshape for normalization
        original_shape = X.shape
        flat = X.reshape(-1, self.feature_dim)
        
        # Fit and transform
        norm_flat = self.scaler.fit_transform(flat)
        X_norm = norm_flat.reshape(original_shape)
        
        # Save normalization parameters
        data_path = Path(__file__).parent.parent / 'data'
        data_path.mkdir(exist_ok=True)
        
        means_path = data_path / 'feature_means.npy'
        stds_path = data_path / 'feature_stds.npy'
        
        np.save(means_path, self.scaler.mean_)
        np.save(stds_path, self.scaler.scale_)
        logger.info(f"Saved normalization parameters to {data_path}")
        
        # Convert to categorical
        y_cat = keras.utils.to_categorical(y, self.num_classes)
        
        # Handle stratified split - if classes have too few samples, use regular split
        try:
            if min_samples >= 2:
                # Use stratified split when possible
                X_train, X_val, y_train, y_val = train_test_split(
                    X_norm, y_cat,
                    test_size=self.config['validation_split'],
                    random_state=42,
                    stratify=y
                )
                logger.info("Used stratified split")
            else:
                # Use regular split for classes with insufficient samples
                X_train, X_val, y_train, y_val = train_test_split(
                    X_norm, y_cat,
                    test_size=self.config['validation_split'],
                    random_state=42
                )
                logger.warning("Used regular split due to classes with insufficient samples")
                
        except ValueError as e:
            # Fallback to regular split if stratified fails
            logger.warning(f"Stratified split failed: {e}. Using regular split.")
            X_train, X_val, y_train, y_val = train_test_split(
                X_norm, y_cat,
                test_size=self.config['validation_split'],
                random_state=42
            )
        
        return X_train, X_val, y_train, y_val

    def train_model(self, X_train, X_val, y_train, y_val, out_path: str) -> keras.Model:
        """Train the model and return it."""
        model = self.create_attention_lstm_model()
        
        # Callbacks
        callbacks = [
            keras.callbacks.EarlyStopping(
                monitor='val_accuracy', 
                patience=self.patience, 
                restore_best_weights=True,
                verbose=1
            ),
            keras.callbacks.ModelCheckpoint(
                out_path, 
                monitor='val_accuracy',
                save_best_only=True,
                verbose=1
            ),
            keras.callbacks.ReduceLROnPlateau(
                monitor='val_loss',
                factor=0.5,
                patience=self.patience//2,
                min_lr=1e-6,
                verbose=1
            )
        ]
        
        # Train the model
        self.history = model.fit(
            X_train, y_train,
            validation_data=(X_val, y_val),
            epochs=self.epochs,
            batch_size=self.batch_size,
            callbacks=callbacks,
            verbose=1
        )
        
        # Save the model
        model.save(out_path)
        logger.info(f"Model saved to {out_path}")
        
        # Save class mappings
        mappings = {
            'class_to_index': self.class_to_index,
            'index_to_class': self.index_to_class,
            'vocabulary': self.vocabulary
        }
        
        mappings_path = Path(out_path).parent / 'class_mappings.json'
        with open(mappings_path, 'w') as f:
            json.dump(mappings, f, indent=2)
        logger.info(f"Class mappings saved to {mappings_path}")
        
        return model

    def evaluate_model(self, X, y) -> Dict:
        """Evaluate the model performance."""
        if self.model is None:
            raise ValueError("Model not trained yet. Call train_model first.")
            
        loss, acc, topk = self.model.evaluate(X, y, verbose=0)
        return {
            'loss': float(loss), 
            'accuracy': float(acc), 
            'top_k_accuracy': float(topk)
        }

    def predict(self, X) -> np.ndarray:
        """Make predictions on new data."""
        if self.model is None:
            raise ValueError("Model not trained yet. Call train_model first.")
        return self.model.predict(X)

    def save_training_history(self, save_path: str):
        """Save training history for analysis."""
        if self.history is not None:
            history_path = Path(save_path).parent / 'training_history.json'
            history_dict = {key: [float(val) for val in values] 
                          for key, values in self.history.history.items()}
            with open(history_path, 'w') as f:
                json.dump(history_dict, f, indent=2)
            logger.info(f"Training history saved to {history_path}")


def main():
    if len(sys.argv) != 3:
        print("Usage: python model_trainer.py <training_data_dir> <output_model.h5>")
        sys.exit(1)
    
    try:
        # Initialize trainer
        trainer = EnhancedModelTrainer()
        
        # Load and preprocess data
        X, y = trainer.load_data(sys.argv[1])
        X_train, X_val, y_train, y_val = trainer.preprocess_data(X, y)
        
        logger.info(f"Training set: {X_train.shape}, Validation set: {X_val.shape}")
        logger.info(f"Number of classes: {trainer.num_classes}")
        
        # Train model
        trainer.model = trainer.train_model(X_train, X_val, y_train, y_val, sys.argv[2])
        
        # Evaluate model
        eval_results = trainer.evaluate_model(X_val, y_val)
        print("\nValidation Results:")
        print(json.dumps(eval_results, indent=2))
        
        # Save training history
        trainer.save_training_history(sys.argv[2])
        
        logger.info("Training completed successfully!")
        
    except Exception as e:
        logger.error(f"Training failed: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()