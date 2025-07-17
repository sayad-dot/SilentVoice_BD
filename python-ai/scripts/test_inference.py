#!/usr/bin/env python3
import json
from sign_predictor import SignLanguagePredictor

# Initialize predictor
predictor = SignLanguagePredictor()

# Load training data
with open('../data/training_data.json', 'r') as f:
    data = json.load(f)

# Test on the first 5 training samples
print("Sample │ True Label       │ Predicted Text   │ Confidence")
print("───────┼──────────────────┼──────────────────┼──────────")
for idx in range(5):
    seq = data['X'][idx]
    true_label = data['y'][idx]
    result = predictor.predict_from_pose_sequence(seq)
    
    pred_text = result['predicted_text']
    confidence = result['confidence']
    
    print(f"{idx:>2}     │ {true_label:<16} │ {pred_text:<16} │ {confidence:.2%}")
