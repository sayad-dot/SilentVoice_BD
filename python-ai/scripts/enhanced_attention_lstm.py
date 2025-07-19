#!/usr/bin/env python3

import json
import numpy as np
import tensorflow as tf
from tensorflow.keras.models import Model
from tensorflow.keras.layers import *
from tensorflow.keras.callbacks import ModelCheckpoint, EarlyStopping, ReduceLROnPlateau
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

class AttentionBiLSTM:
    """Enhanced Attention-based Bidirectional LSTM for Bangla Sign Language Recognition"""
    
    def __init__(self, sequence_length=30, feature_dim=288, num_classes=61):
        self.sequence_length = sequence_length
        self.feature_dim = feature_dim
        self.num_classes = num_classes
        self.model = None
        self.label_encoder = None
        
    def build_attention_model(self, params):
        """Build attention-based bidirectional LSTM model"""
        
        # Input layer
        inputs = Input(shape=(self.sequence_length, self.feature_dim), name='pose_input')
        
        # Spatial feature extraction
        x = TimeDistributed(Dense(params['spatial_features'], activation='relu'), name='spatial_features')(inputs)
        x = TimeDistributed(BatchNormalization(), name='spatial_bn')(x)
        x = TimeDistributed(Dropout(params['dropout_rate']), name='spatial_dropout')(x)
        
        # Bidirectional LSTM layers
        lstm1 = Bidirectional(
            LSTM(params['lstm_units_1'], return_sequences=True, dropout=params['lstm_dropout']),
            name='bilstm_1'
        )(x)
        lstm1 = BatchNormalization(name='lstm1_bn')(lstm1)
        
        lstm2 = Bidirectional(
            LSTM(params['lstm_units_2'], return_sequences=True, dropout=params['lstm_dropout']),
            name='bilstm_2'
        )(lstm1)
        lstm2 = BatchNormalization(name='lstm2_bn')(lstm2)
        
        # Self-attention mechanism
        attention_input = Dense(params['attention_units'], activation='tanh', name='attention_dense')(lstm2)
        attention_weights = Dense(1, activation='softmax', name='attention_weights')(attention_input)
        attention_weights = Flatten(name='attention_flatten')(attention_weights)
        attention_weights = RepeatVector(params['lstm_units_2'] * 2, name='attention_repeat')(attention_weights)
        attention_weights = Permute([2, 1], name='attention_permute')(attention_weights)
        
        # Apply attention
        attended_features = Multiply(name='attention_multiply')([lstm2, attention_weights])
        attended_features = Lambda(lambda x: tf.reduce_sum(x, axis=1), name='attention_sum')(attended_features)
        
        # Classification head
        dense1 = Dense(params['dense_units_1'], activation='relu', name='dense1')(attended_features)
        dense1 = BatchNormalization(name='dense1_bn')(dense1)
        dense1 = Dropout(params['dropout_rate'], name='dense1_dropout')(dense1)
        
        dense2 = Dense(params['dense_units_2'], activation='relu', name='dense2')(dense1)
        dense2 = BatchNormalization(name='dense2_bn')(dense2)
        dense2 = Dropout(params['dropout_rate'], name='dense2_dropout')(dense2)
        
        # Output layer
        outputs = Dense(self.num_classes, activation='softmax', name='output')(dense2)
        
        # Create model
        model = Model(inputs=inputs, outputs=outputs, name='AttentionBiLSTM')
        
        return model
    
    def compile_model(self, model, learning_rate=0.001):
        """Compile model with optimizer and loss function"""
        optimizer = tf.keras.optimizers.Adam(
            learning_rate=learning_rate,
            beta_1=0.9,
            beta_2=0.999,
            epsilon=1e-7
        )
        
        model.compile(
            optimizer=optimizer,
            loss='categorical_crossentropy',
            metrics=['accuracy', 'top_k_categorical_accuracy']
        )
        
        return model

class EnhancedAttentionTrainer:
    """Trainer for attention-based Bangla sign language model"""
    
    def __init__(self, data_file='../data/training_data.json'):
        self.data_file = data_file
        self.model_builder = None
        self.X_train = None
        self.X_val = None
        self.y_train = None
        self.y_val = None
        self.label_encoder = None
        self.num_classes = 0
        self.best_params = None
        self.best_score = 0
        
    def load_data(self):
        """Load and prepare training data"""
        logger.info("Loading training data for attention model...")
        
        try:
            with open(self.data_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
        except FileNotFoundError:
            raise FileNotFoundError(f"Training data file not found: {self.data_file}")
        
        X = np.array(data['X'], dtype=np.float32)
        y = data['y']
        
        logger.info(f"Loaded {len(X)} samples with {len(set(y))} unique labels")
        logger.info(f"Data shape: {X.shape}")
        
        # Initialize model builder
        self.model_builder = AttentionBiLSTM(
            sequence_length=X.shape[1],
            feature_dim=X.shape[2],
            num_classes=len(set(y))
        )
        
        # Encode labels
        self.label_encoder = LabelEncoder()
        y_encoded = self.label_encoder.fit_transform(y)
        y_categorical = tf.keras.utils.to_categorical(y_encoded)
        self.num_classes = y_categorical.shape[1]
        
        # Update model builder
        self.model_builder.num_classes = self.num_classes
        
        # Split data
        self.X_train, self.X_val, self.y_train, self.y_val = train_test_split(
            X, y_categorical, test_size=0.2, random_state=42,
            stratify=y_encoded
        )
        
        logger.info(f"Training samples: {len(self.X_train)}")
        logger.info(f"Validation samples: {len(self.X_val)}")
        logger.info(f"Number of classes: {self.num_classes}")
        
        return True
    
    def objective(self, trial):
        """Objective function for hyperparameter optimization"""
        tf.keras.backend.clear_session()
        gc.collect()
        
        # Define hyperparameter search space for attention model
        params = {
            'spatial_features': trial.suggest_int('spatial_features', 128, 512, step=64),
            'lstm_units_1': trial.suggest_int('lstm_units_1', 64, 256, step=32),
            'lstm_units_2': trial.suggest_int('lstm_units_2', 32, 128, step=16),
            'attention_units': trial.suggest_int('attention_units', 32, 128, step=16),
            'dense_units_1': trial.suggest_int('dense_units_1', 128, 512, step=64),
            'dense_units_2': trial.suggest_int('dense_units_2', 64, 256, step=32),
            'dropout_rate': trial.suggest_float('dropout_rate', 0.2, 0.6, step=0.1),
            'lstm_dropout': trial.suggest_float('lstm_dropout', 0.1, 0.4, step=0.1),
            'learning_rate': trial.suggest_float('learning_rate', 1e-4, 1e-2, log=True),
            'batch_size': trial.suggest_categorical('batch_size', [16, 32, 64]),
            'epochs': trial.suggest_int('epochs', 25, 50, step=5)
        }
        
        logger.info(f"Attention Trial {trial.number}: Testing parameters")
        
        try:
            # Build and compile model
            model = self.model_builder.build_attention_model(params)
            model = self.model_builder.compile_model(model, params['learning_rate'])
            
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
                    patience=10,
                    restore_best_weights=True,
                    verbose=0
                ),
                ReduceLROnPlateau(
                    monitor='val_loss',
                    factor=0.5,
                    patience=5,
                    min_lr=1e-7,
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
            
            best_val_accuracy = max(history.history['val_accuracy'])
            
            logger.info(f"Attention Trial {trial.number}: Best validation accuracy = {best_val_accuracy:.4f}")
            
            return best_val_accuracy
            
        except Exception as e:
            logger.error(f"Attention Trial {trial.number} failed: {e}")
            return 0.0
    
    def optimize(self, n_trials=30, timeout=7200):
        """Optimize attention model hyperparameters"""
        logger.info(f"Starting attention model optimization with {n_trials} trials")
        
        # Load data
        self.load_data()
        
        # Create study
        study = optuna.create_study(
            direction='maximize',
            pruner=optuna.pruners.MedianPruner(n_startup_trials=5, n_warmup_steps=15)
        )
        
        # Optimize
        study.optimize(self.objective, n_trials=n_trials, timeout=timeout)
        
        # Save results
        self.best_params = study.best_params
        self.best_score = study.best_value
        
        logger.info(f"Attention model optimization completed!")
        logger.info(f"Best parameters: {self.best_params}")
        logger.info(f"Best validation accuracy: {self.best_score:.4f}")
        
        # Save best parameters
        os.makedirs('../trained_models', exist_ok=True)
        with open('../trained_models/best_attention_hyperparameters.json', 'w') as f:
            json.dump({
                'best_params': self.best_params,
                'best_score': self.best_score,
                'optimization_date': str(tf.timestamp()),
                'n_trials': n_trials,
                'model_type': 'attention_bilstm'
            }, f, indent=2)
        
        return study
    
    def train_final_model(self):
        """Train final attention model with best hyperparameters"""
        if self.best_params is None:
            logger.error("No best parameters found. Run optimization first.")
            return None
        
        logger.info("Training final attention model with best parameters...")
        
        # Build and compile model
        model = self.model_builder.build_attention_model(self.best_params)
        model = self.model_builder.compile_model(model, self.best_params['learning_rate'])
        
        # Print model summary
        logger.info("Attention Model Architecture:")
        model.summary()
        
        # Compute class weights
        y_train_labels = np.argmax(self.y_train, axis=1)
        class_weights = compute_class_weight(
            'balanced',
            classes=np.unique(y_train_labels),
            y=y_train_labels
        )
        class_weight_dict = dict(enumerate(class_weights))
        
        # Enhanced callbacks
        callbacks = [
            ModelCheckpoint(
                '../trained_models/attention_bangla_lstm_best.h5',
                monitor='val_accuracy',
                save_best_only=True,
                mode='max',
                verbose=1
            ),
            EarlyStopping(
                monitor='val_accuracy',
                patience=15,
                restore_best_weights=True,
                verbose=1
            ),
            ReduceLROnPlateau(
                monitor='val_loss',
                factor=0.5,
                patience=7,
                min_lr=1e-8,
                verbose=1
            )
        ]
        
        # Train final model
        history = model.fit(
            self.X_train, self.y_train,
            validation_data=(self.X_val, self.y_val),
            epochs=self.best_params['epochs'] + 15,  # Extended training
            batch_size=self.best_params['batch_size'],
            callbacks=callbacks,
            class_weight=class_weight_dict,
            verbose=1
        )
        
        # Save final model
        model.save('../trained_models/attention_bangla_lstm_final.h5')
        
        # Save label encoder
        with open('../trained_models/attention_label_encoder.pkl', 'wb') as f:
            pickle.dump(self.label_encoder, f)
        
        # Save configuration
        config = {
            'best_params': self.best_params,
            'best_score': self.best_score,
            'final_accuracy': max(history.history['val_accuracy']),
            'sequence_length': self.model_builder.sequence_length,
            'feature_dim': self.model_builder.feature_dim,
            'num_classes': self.num_classes,
            'classes': self.label_encoder.classes_.tolist(),
            'model_type': 'attention_bilstm'
        }
        
        with open('../trained_models/attention_model_config.json', 'w', encoding='utf-8') as f:
            json.dump(config, f, ensure_ascii=False, indent=2)
        
        logger.info("Final attention model training completed!")
        logger.info(f"Final validation accuracy: {max(history.history['val_accuracy']):.4f}")
        
        return model, history

def main():
    if len(sys.argv) < 2:
        print("Usage: python enhanced_attention_lstm.py <command>")
        print("Commands: optimize, train, both")
        return
    
    command = sys.argv[1]
    trainer = EnhancedAttentionTrainer()
    
    if command == "optimize":
        n_trials = int(sys.argv[2]) if len(sys.argv) > 2 else 30
        trainer.optimize(n_trials=n_trials)
    
    elif command == "train":
        # Load existing best parameters
        try:
            with open('../trained_models/best_attention_hyperparameters.json', 'r') as f:
                data = json.load(f)
                trainer.best_params = data['best_params']
                trainer.best_score = data['best_score']
            trainer.load_data()
            trainer.train_final_model()
        except FileNotFoundError:
            logger.error("No attention hyperparameters found. Run optimization first.")
    
    elif command == "both":
        n_trials = int(sys.argv[2]) if len(sys.argv) > 2 else 30
        trainer.optimize(n_trials=n_trials)
        trainer.train_final_model()
    
    else:
        logger.error(f"Unknown command: {command}")

if __name__ == "__main__":
    main()
