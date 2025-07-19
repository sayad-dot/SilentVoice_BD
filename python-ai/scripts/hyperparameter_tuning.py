#!/usr/bin/env python3

import json
import numpy as np
import tensorflow as tf
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense, Dropout, BatchNormalization, Bidirectional, Attention
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.utils.class_weight import compute_class_weight
import pickle
import os
import sys
import optuna
from optuna.integration import TFKerasPruningCallback
import gc
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class BanglaSignHyperparameterTuner:
    def __init__(self, data_file='../data/training_data.json'):
        self.data_file = data_file
        self.best_params = None
        self.best_score = 0
        self.X_train = None
        self.X_val = None
        self.y_train = None
        self.y_val = None
        self.label_encoder = None
        self.num_classes = 0
        
    def load_data(self):
        """Load and prepare training data"""
        logger.info("Loading training data...")
        
        try:
            with open(self.data_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
        except FileNotFoundError:
            raise FileNotFoundError(f"Training data file not found: {self.data_file}")
        
        X = np.array(data['X'], dtype=np.float32)
        y = data['y']
        
        logger.info(f"Loaded {len(X)} samples with {len(set(y))} unique labels")
        logger.info(f"Data shape: {X.shape}")
        
        # Encode labels
        self.label_encoder = LabelEncoder()
        y_encoded = self.label_encoder.fit_transform(y)
        y_categorical = tf.keras.utils.to_categorical(y_encoded)
        self.num_classes = y_categorical.shape[1]
        
        # Split data
        self.X_train, self.X_val, self.y_train, self.y_val = train_test_split(
            X, y_categorical, test_size=0.2, random_state=42,
            stratify=y_encoded
        )
        
        logger.info(f"Training samples: {len(self.X_train)}")
        logger.info(f"Validation samples: {len(self.X_val)}")
        logger.info(f"Number of classes: {self.num_classes}")
        
        return True
    
    def create_model(self, params):
        """Create model with given hyperparameters"""
        model = Sequential()
        
        # First LSTM layer
        model.add(LSTM(
            units=params['lstm_units_1'],
            return_sequences=True,
            input_shape=(self.X_train.shape[1], self.X_train.shape[2])
        ))
        model.add(Dropout(params['dropout_rate']))
        model.add(BatchNormalization())
        
        # Second LSTM layer
        model.add(LSTM(
            units=params['lstm_units_2'],
            return_sequences=False
        ))
        model.add(Dropout(params['dropout_rate']))
        model.add(BatchNormalization())
        
        # Dense layers
        model.add(Dense(params['dense_units'], activation='relu'))
        model.add(Dropout(params['dropout_rate']))
        model.add(BatchNormalization())
        
        # Output layer
        model.add(Dense(self.num_classes, activation='softmax'))
        
        # Compile model
        optimizer = tf.keras.optimizers.Adam(learning_rate=params['learning_rate'])
        model.compile(
            optimizer=optimizer,
            loss='categorical_crossentropy',
            metrics=['accuracy', 'top_k_categorical_accuracy']
        )
        
        return model
    
    def objective(self, trial):
        """Objective function for Optuna optimization"""
        # Clear session to avoid memory issues
        tf.keras.backend.clear_session()
        gc.collect()
        
        # Define hyperparameter search space
        params = {
            'lstm_units_1': trial.suggest_int('lstm_units_1', 64, 256, step=32),
            'lstm_units_2': trial.suggest_int('lstm_units_2', 32, 128, step=16),
            'dense_units': trial.suggest_int('dense_units', 64, 256, step=32),
            'dropout_rate': trial.suggest_float('dropout_rate', 0.2, 0.6, step=0.1),
            'learning_rate': trial.suggest_float('learning_rate', 1e-4, 1e-2, log=True),
            'batch_size': trial.suggest_categorical('batch_size', [16, 32, 64]),
            'epochs': trial.suggest_int('epochs', 20, 50, step=5)
        }
        
        logger.info(f"Trial {trial.number}: Testing parameters {params}")
        
        try:
            # Create model
            model = self.create_model(params)
            
            # Compute class weights
            y_train_labels = np.argmax(self.y_train, axis=1)
            class_weights = compute_class_weight(
                'balanced',
                classes=np.unique(y_train_labels),
                y=y_train_labels
            )
            class_weight_dict = dict(enumerate(class_weights))
            
            # Callbacks
            callbacks = [
                EarlyStopping(
                    monitor='val_accuracy',
                    patience=8,
                    restore_best_weights=True,
                    verbose=0
                ),
                ReduceLROnPlateau(
                    monitor='val_loss',
                    factor=0.5,
                    patience=4,
                    min_lr=1e-6,
                    verbose=0
                ),
                TFKerasPruningCallback(trial, 'val_accuracy')
            ]
            
            # Train model
            history = model.fit(
                self.X_train, self.y_train,
                validation_data=(self.X_val, self.y_val),
                epochs=params['epochs'],
                batch_size=params['batch_size'],
                callbacks=callbacks,
                class_weight=class_weight_dict,
                verbose=0
            )
            
            # Get best validation accuracy
            best_val_accuracy = max(history.history['val_accuracy'])
            
            logger.info(f"Trial {trial.number}: Best validation accuracy = {best_val_accuracy:.4f}")
            
            return best_val_accuracy
            
        except Exception as e:
            logger.error(f"Trial {trial.number} failed: {e}")
            return 0.0
    
    def optimize(self, n_trials=50, timeout=7200):  # 2 hours timeout
        """Run hyperparameter optimization"""
        logger.info(f"Starting hyperparameter optimization with {n_trials} trials")
        
        # Load data
        self.load_data()
        
        # Create study
        study = optuna.create_study(
            direction='maximize',
            pruner=optuna.pruners.MedianPruner(n_startup_trials=5, n_warmup_steps=10)
        )
        
        # Optimize
        study.optimize(self.objective, n_trials=n_trials, timeout=timeout)
        
        # Save results
        self.best_params = study.best_params
        self.best_score = study.best_value
        
        logger.info(f"Optimization completed!")
        logger.info(f"Best parameters: {self.best_params}")
        logger.info(f"Best validation accuracy: {self.best_score:.4f}")
        
        # Save best parameters
        os.makedirs('../trained_models', exist_ok=True)
        with open('../trained_models/best_hyperparameters.json', 'w') as f:
            json.dump({
                'best_params': self.best_params,
                'best_score': self.best_score,
                'optimization_date': str(tf.timestamp()),
                'n_trials': n_trials
            }, f, indent=2)
        
        return study
    
    def train_best_model(self):
        """Train final model with best hyperparameters"""
        if self.best_params is None:
            logger.error("No best parameters found. Run optimization first.")
            return None
        
        logger.info("Training final model with best parameters...")
        
        # Create model with best parameters
        model = self.create_model(self.best_params)
        
        # Compute class weights
        y_train_labels = np.argmax(self.y_train, axis=1)
        class_weights = compute_class_weight(
            'balanced',
            classes=np.unique(y_train_labels),
            y=y_train_labels
        )
        class_weight_dict = dict(enumerate(class_weights))
        
        # Enhanced callbacks for final training
        callbacks = [
            tf.keras.callbacks.ModelCheckpoint(
                '../trained_models/hypertuned_bangla_lstm_best.h5',
                monitor='val_accuracy',
                save_best_only=True,
                mode='max',
                verbose=1
            ),
            EarlyStopping(
                monitor='val_accuracy',
                patience=12,
                restore_best_weights=True,
                verbose=1
            ),
            ReduceLROnPlateau(
                monitor='val_loss',
                factor=0.5,
                patience=6,
                min_lr=1e-7,
                verbose=1
            )
        ]
        
        # Train final model
        history = model.fit(
            self.X_train, self.y_train,
            validation_data=(self.X_val, self.y_val),
            epochs=self.best_params['epochs'] + 10,  # Train a bit longer
            batch_size=self.best_params['batch_size'],
            callbacks=callbacks,
            class_weight=class_weight_dict,
            verbose=1
        )
        
        # Save final model
        model.save('../trained_models/hypertuned_bangla_lstm_final.h5')
        
        # Save label encoder
        with open('../trained_models/hypertuned_label_encoder.pkl', 'wb') as f:
            pickle.dump(self.label_encoder, f)
        
        # Save configuration
        config = {
            'best_params': self.best_params,
            'best_score': self.best_score,
            'final_accuracy': max(history.history['val_accuracy']),
            'sequence_length': self.X_train.shape[1],
            'feature_dim': self.X_train.shape[2],
            'num_classes': self.num_classes,
            'classes': self.label_encoder.classes_.tolist()
        }
        
        with open('../trained_models/hypertuned_model_config.json', 'w', encoding='utf-8') as f:
            json.dump(config, f, ensure_ascii=False, indent=2)
        
        logger.info("Final model training completed!")
        logger.info(f"Final validation accuracy: {max(history.history['val_accuracy']):.4f}")
        
        return model, history

def main():
    if len(sys.argv) < 2:
        print("Usage: python hyperparameter_tuning.py <command>")
        print("Commands: optimize, train, both")
        return
    
    command = sys.argv[1]
    tuner = BanglaSignHyperparameterTuner()
    
    if command == "optimize":
        n_trials = int(sys.argv[2]) if len(sys.argv) > 2 else 50
        tuner.optimize(n_trials=n_trials)
    
    elif command == "train":
        # Load existing best parameters
        try:
            with open('../trained_models/best_hyperparameters.json', 'r') as f:
                data = json.load(f)
                tuner.best_params = data['best_params']
                tuner.best_score = data['best_score']
            tuner.load_data()
            tuner.train_best_model()
        except FileNotFoundError:
            logger.error("No hyperparameters found. Run optimization first.")
    
    elif command == "both":
        n_trials = int(sys.argv[2]) if len(sys.argv) > 2 else 50
        tuner.optimize(n_trials=n_trials)
        tuner.train_best_model()
    
    else:
        logger.error(f"Unknown command: {command}")

if __name__ == "__main__":
    main()
