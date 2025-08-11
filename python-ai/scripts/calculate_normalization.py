#!/usr/bin/env python3
"""
Normalization Parameter Calculator for SilentVoice_BD
- Extract features from training data
- Calculate proper normalization parameters
- Ensure training/inference consistency
"""

import numpy as np
import json
import os
import sys
from pathlib import Path
import cv2
import logging
from enhanced_pose_extractor import EnhancedPoseExtractor

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class NormalizationCalculator:
    """Calculate normalization parameters from training data"""
    
    def __init__(self):
        self.pose_extractor = EnhancedPoseExtractor()
        self.all_features = []
        self.all_quality_scores = []
        
    def process_training_videos(self, video_dir: str, classes: list = None):
        """Process training videos and extract features"""
        video_path = Path(video_dir)
        
        if not video_path.exists():
            raise FileNotFoundError(f"Video directory not found: {video_dir}")
        
        if classes is None:
            # Use default Bangla vocabulary
            classes = [
                'দাদা', 'দাদি', 'মা', 'বাবা', 'ভাই', 'বোন',
                'আম', 'আপেল', 'চা', 'পানি', 'খাবার',
                'ভালো', 'খারাপ', 'হ্যালো', 'ধন্যবাদ', 'দুঃখিত',
                'আমি', 'তুমি', 'আপনি', 'কি', 'কেমন',
                'নাম', 'কাজ', 'স্কুল', 'বাড়ি', 'রাস্তা',
                'সকাল', 'দুপুর', 'রাত', 'এখন', 'পরে'
            ]
        
        total_processed = 0
        valid_sequences = 0
        
        for class_name in classes:
            class_dir = video_path / class_name
            if not class_dir.exists():
                logger.warning(f"Class directory not found: {class_dir}")
                continue
            
            logger.info(f"Processing class: {class_name}")
            
            # Find video files
            video_extensions = ['.mp4', '.avi', '.mov', '.mkv', '.webm']
            video_files = []
            for ext in video_extensions:
                video_files.extend(class_dir.glob(f'*{ext}'))
            
            class_processed = 0
            for video_file in video_files:
                try:
                    logger.info(f"  Processing: {video_file.name}")
                    result = self.pose_extractor.process_video_file(str(video_file))
                    
                    if result['success'] and len(result['pose_sequence']) > 0:
                        self.all_features.extend(result['pose_sequence'])
                        
                        # Store quality scores if available
                        if 'quality_score' in result:
                            self.all_quality_scores.extend([result['quality_score']] * len(result['pose_sequence']))
                        
                        valid_sequences += len(result['pose_sequence'])
                        class_processed += 1
                        logger.info(f"    ✅ Extracted {len(result['pose_sequence'])} sequences")
                    else:
                        logger.warning(f"    ❌ Failed to process: {result.get('error', 'Unknown error')}")
                    
                    total_processed += 1
                    
                except Exception as e:
                    logger.error(f"    💥 Error processing {video_file}: {e}")
                    continue
            
            logger.info(f"Class '{class_name}': {class_processed}/{len(video_files)} videos processed")
        
        logger.info(f"📊 Total processing summary:")
        logger.info(f"   Videos processed: {total_processed}")
        logger.info(f"   Valid sequences: {valid_sequences}")
        logger.info(f"   Features per sequence: {len(self.all_features[0]) if self.all_features else 0}")
        
        return valid_sequences > 0
    
    def process_existing_features(self, features_dir: str):
        """Process pre-extracted feature files"""
        features_path = Path(features_dir)
        
        if not features_path.exists():
            raise FileNotFoundError(f"Features directory not found: {features_dir}")
        
        # Look for JSON files with extracted features
        json_files = list(features_path.glob('*.json'))
        npy_files = list(features_path.glob('*.npy'))
        
        total_sequences = 0
        
        # Process JSON files
        for json_file in json_files:
            try:
                with open(json_file, 'r') as f:
                    data = json.load(f)
                
                if isinstance(data, dict) and data.get('success') and 'pose_sequence' in data:
                    # Single result file
                    self.all_features.extend(data['pose_sequence'])
                    total_sequences += len(data['pose_sequence'])
                elif isinstance(data, list):
                    # List of sequences
                    self.all_features.extend(data)
                    total_sequences += len(data)
                
                logger.info(f"✅ Loaded features from {json_file.name}")
                
            except Exception as e:
                logger.warning(f"⚠️ Failed to load {json_file}: {e}")
        
        # Process NPY files
        for npy_file in npy_files:
            try:
                data = np.load(npy_file)
                
                if len(data.shape) == 2:
                    # Single sequence
                    self.all_features.append(data.tolist())
                    total_sequences += 1
                elif len(data.shape) == 3:
                    # Multiple sequences
                    for sequence in data:
                        self.all_features.append(sequence.tolist())
                    total_sequences += data.shape[0]
                
                logger.info(f"✅ Loaded features from {npy_file.name}")
                
            except Exception as e:
                logger.warning(f"⚠️ Failed to load {npy_file}: {e}")
        
        logger.info(f"📊 Loaded {total_sequences} sequences from {len(json_files + npy_files)} files")
        return total_sequences > 0
    
    def calculate_normalization_parameters(self):
        """Calculate normalization parameters from collected features"""
        if not self.all_features:
            raise ValueError("No features available for normalization calculation")
        
        logger.info("🔧 Calculating normalization parameters...")
        
        # Convert to numpy array
        features_array = np.array(self.all_features, dtype=np.float32)
        logger.info(f"Feature array shape: {features_array.shape}")
        
        # Validate feature dimension
        expected_dim = 288  # MediaPipe holistic features
        if features_array.shape[1] != expected_dim:
            logger.warning(f"⚠️ Unexpected feature dimension: {features_array.shape[1]} (expected {expected_dim})")
        
        # Calculate statistics
        feature_means = np.mean(features_array, axis=0)
        feature_stds = np.std(features_array, axis=0)
        
        # Handle zero standard deviations
        zero_std_count = np.sum(feature_stds == 0)
        if zero_std_count > 0:
            logger.warning(f"⚠️ Found {zero_std_count} features with zero standard deviation")
            feature_stds = np.where(feature_stds == 0, 1e-8, feature_stds)
        
        # Calculate quality metrics
        stats = {
            'num_sequences': len(self.all_features),
            'feature_dimension': features_array.shape[1],
            'mean_range': [float(feature_means.min()), float(feature_means.max())],
            'std_range': [float(feature_stds.min()), float(feature_stds.max())],
            'zero_std_features': int(zero_std_count),
            'data_range': [float(features_array.min()), float(features_array.max())],
            'mean_magnitude': float(np.mean(np.abs(feature_means))),
            'std_magnitude': float(np.mean(feature_stds))
        }
        
        logger.info("📊 Normalization statistics:")
        logger.info(f"   Sequences processed: {stats['num_sequences']}")
        logger.info(f"   Feature dimension: {stats['feature_dimension']}")
        logger.info(f"   Mean range: [{stats['mean_range'][0]:.4f}, {stats['mean_range'][1]:.4f}]")
        logger.info(f"   Std range: [{stats['std_range'][0]:.4f}, {stats['std_range'][1]:.4f}]")
        logger.info(f"   Data range: [{stats['data_range'][0]:.4f}, {stats['data_range'][1]:.4f}]")
        
        if self.all_quality_scores:
            avg_quality = np.mean(self.all_quality_scores)
            logger.info(f"   Average quality score: {avg_quality:.3f}")
            stats['average_quality'] = float(avg_quality)
        
        return feature_means, feature_stds, stats
    
    def save_normalization_parameters(self, feature_means, feature_stds, stats, output_dir: str):
        """Save normalization parameters to files"""
        output_path = Path(output_dir)
        output_path.mkdir(exist_ok=True)
        
        # Save as numpy arrays (for Python)
        np.save(output_path / 'feature_means.npy', feature_means.astype(np.float32))
        np.save(output_path / 'feature_stds.npy', feature_stds.astype(np.float32))
        
        # Save as JSON (for verification)
        json_data = {
            'feature_means': feature_means.tolist(),
            'feature_stds': feature_stds.tolist(),
            'statistics': stats,
            'generated_at': str(np.datetime64('now')),
            'version': 'enhanced_v1'
        }
        
        with open(output_path / 'normalization_params.json', 'w') as f:
            json.dump(json_data, f, indent=2)
        
        # Save statistics summary
        with open(output_path / 'normalization_stats.txt', 'w') as f:
            f.write("SilentVoice_BD Normalization Parameters\n")
            f.write("=" * 40 + "\n\n")
            f.write(f"Generated at: {json_data['generated_at']}\n")
            f.write(f"Version: {json_data['version']}\n\n")
            f.write("Statistics:\n")
            for key, value in stats.items():
                f.write(f"  {key}: {value}\n")
        
        logger.info(f"✅ Normalization parameters saved to: {output_path}")
        logger.info(f"   📁 feature_means.npy")
        logger.info(f"   📁 feature_stds.npy") 
        logger.info(f"   📁 normalization_params.json")
        logger.info(f"   📁 normalization_stats.txt")
        
        return output_path
    
    def validate_normalization(self, sample_data: np.ndarray, feature_means, feature_stds):
        """Validate normalization on sample data"""
        logger.info("🔍 Validating normalization...")
        
        # Apply normalization
        normalized_data = (sample_data - feature_means) / feature_stds
        
        # Check normalized statistics
        norm_mean = np.mean(normalized_data, axis=0)
        norm_std = np.std(normalized_data, axis=0)
        
        # Validation metrics
        mean_close_to_zero = np.allclose(norm_mean, 0, atol=0.1)
        std_close_to_one = np.allclose(norm_std, 1, atol=0.1)
        
        logger.info(f"✅ Validation results:")
        logger.info(f"   Mean close to 0: {mean_close_to_zero} (max deviation: {np.max(np.abs(norm_mean)):.4f})")
        logger.info(f"   Std close to 1: {std_close_to_one} (max deviation: {np.max(np.abs(norm_std - 1)):.4f})")
        
        if mean_close_to_zero and std_close_to_one:
            logger.info("🎉 Normalization validation PASSED!")
            return True
        else:
            logger.warning("⚠️ Normalization validation FAILED - check data quality")
            return False

def main():
    """Main normalization calculation script"""
    if len(sys.argv) < 3:
        print("""
Usage: python calculate_normalization.py <mode> <input_path> [output_path]

Modes:
  videos - Process training videos to extract features and calculate normalization
  features - Use pre-extracted features to calculate normalization

Examples:
  python calculate_normalization.py videos /path/to/training/videos ../data
  python calculate_normalization.py features /path/to/extracted/features ../data
        """)
        return
    
    mode = sys.argv[1].lower()
    input_path = sys.argv[2]
    output_path = sys.argv[3] if len(sys.argv) > 3 else "../data"
    
    try:
        calculator = NormalizationCalculator()
        
        # Process input data
        if mode == "videos":
            logger.info(f"🎬 Processing training videos from: {input_path}")
            success = calculator.process_training_videos(input_path)
        elif mode == "features":
            logger.info(f"📊 Processing extracted features from: {input_path}")
            success = calculator.process_existing_features(input_path)
        else:
            raise ValueError(f"Unknown mode: {mode}")
        
        if not success:
            raise ValueError("No valid data found for processing")
        
        # Calculate normalization parameters
        feature_means, feature_stds, stats = calculator.calculate_normalization_parameters()
        
        # Save parameters
        output_dir = calculator.save_normalization_parameters(
            feature_means, feature_stds, stats, output_path
        )
        
        # Validate with sample data
        if len(calculator.all_features) > 10:
            sample_indices = np.random.choice(len(calculator.all_features), 10, replace=False)
            sample_data = np.array([calculator.all_features[i] for i in sample_indices])
            calculator.validate_normalization(sample_data, feature_means, feature_stds)
        
        print(json.dumps({
            'success': True,
            'message': 'Normalization parameters calculated successfully',
            'output_directory': str(output_dir),
            'statistics': stats
        }, indent=2))
        
    except Exception as e:
        logger.error(f"💥 Normalization calculation failed: {e}")
        print(json.dumps({
            'success': False,
            'error': str(e)
        }))

if __name__ == "__main__":
    main()