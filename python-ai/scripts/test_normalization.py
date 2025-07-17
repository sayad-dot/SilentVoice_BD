#!/usr/bin/env python3
import numpy as np
import os
import sys

# Test normalization loading
script_dir = os.path.dirname(os.path.abspath(__file__))
data_dir = os.path.join(script_dir, '..', 'data')
means_path = os.path.join(data_dir, 'feature_means.npy')
stds_path = os.path.join(data_dir, 'feature_stds.npy')

print(f"Script dir: {script_dir}")
print(f"Data dir: {data_dir}")
print(f"Means exists: {os.path.exists(means_path)}")
print(f"Stds exists: {os.path.exists(stds_path)}")

if os.path.exists(means_path):
    means = np.load(means_path)
    print(f"Means shape: {means.shape}")
    print(f"Means sample: {means[:5]}")
else:
    print("ERROR: Means file not found!")
