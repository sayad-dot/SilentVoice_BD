#!/usr/bin/env python3
"""
Comprehensive Diagnostic Script for SilentVoice_BD Model Issues
- Tests all components of the sign language recognition pipeline
- Identifies common issues and provides solutions
- Validates model loading, pose extraction, and prediction consistency
"""

import os
import sys
import json
import numpy as np
import logging
import traceback
from pathlib import Path
import cv2
import tempfile
import pickle

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class SilentVoiceDiagnostic:
    """Comprehensive diagnostic tool for SilentVoice_BD system"""
    
    def __init__(self):
        self.script_dir = Path(__file__).parent
        self.base_dir = self.script_dir.parent if 'scripts' in str(self.script_dir) else self.script_dir
        self.issues = []
        self.warnings = []
        self.success_count = 0
        self.total_tests = 0
        
    def log_issue(self, issue):
        """Log an issue and print to console"""
        self.issues.append(issue)
        logger.error(f"❌ ISSUE: {issue}")
        
    def log_warning(self, warning):
        """Log a warning and print to console"""
        self.warnings.append(warning)
        logger.warning(f"⚠️ WARNING: {warning}")
        
    def log_success(self, message):
        """Log a success and print to console"""
        self.success_count += 1
        logger.info(f"✅ SUCCESS: {message}")
        
    def test_dependencies(self):
        """Test if all required dependencies are available"""
        logger.info("🔍 Testing Dependencies...")
        self.total_tests += 1
        
        required_packages = {
            'cv2': 'opencv-python',
            'numpy': 'numpy', 
            'tensorflow': 'tensorflow',
            'mediapipe': 'mediapipe',
            'sklearn': 'scikit-learn'
        }
        
        missing_packages = []
        
        for module_name, package_name in required_packages.items():
            try:
                __import__(module_name)
                logger.info(f"  ✓ {package_name} available")
            except ImportError:
                missing_packages.append(package_name)
                logger.error(f"  ✗ {package_name} missing")
                
        if missing_packages:
            self.log_issue(f"Missing packages: {', '.join(missing_packages)}")
            logger.info("Install missing packages with:")
            logger.info(f"pip install {' '.join(missing_packages)}")
            return False
        else:
            self.log_success("All required dependencies are available")
            return True
    
    def test_tensorflow(self):
        """Test TensorFlow installation and GPU availability"""
        logger.info("🔍 Testing TensorFlow...")
        self.total_tests += 1
        
        try:
            import tensorflow as tf
            logger.info(f"TensorFlow version: {tf.__version__}")
            
            # Test basic operations
            test_tensor = tf.constant([1.0, 2.0, 3.0])
            result = tf.reduce_sum(test_tensor)
            logger.info(f"TensorFlow basic operations: {result.numpy()}")
            
            # Check GPU availability
            gpus = tf.config.experimental.list_physical_devices('GPU')
            if gpus:
                logger.info(f"GPU devices available: {len(gpus)}")
                for gpu in gpus:
                    logger.info(f"  - {gpu}")
            else:
                self.log_warning("No GPU devices found - using CPU only")
            
            self.log_success("TensorFlow is working correctly")
            return True
            
        except Exception as e:
            self.log_issue(f"TensorFlow test failed: {e}")
            return False
    
    def find_models(self):
        """Find all available models in the system"""
        logger.info("🔍 Searching for Models...")
        self.total_tests += 1
        
        model_locations = [
            self.base_dir / 'models',
            self.base_dir / 'trained_models',
            self.script_dir / 'models',
            self.script_dir / 'trained_models'
        ]
        
        found_models = []
        
        for location in model_locations:
            if location.exists():
                for model_file in location.glob('*.h5'):
                    file_size = model_file.stat().st_size / (1024 * 1024)  # MB
                    found_models.append({
                        'path': str(model_file),
                        'name': model_file.name,
                        'size_mb': file_size,
                        'location': str(location)
                    })
                    logger.info(f"  Found: {model_file.name} ({file_size:.1f} MB)")
        
        if found_models:
            self.log_success(f"Found {len(found_models)} model files")
            return found_models
        else:
            self.log_issue("No model files (.h5) found in expected locations")
            logger.info("Expected model locations:")
            for location in model_locations:
                logger.info(f"  - {location}")
            return []
    
    def test_model_loading(self, model_path):
        """Test loading a specific model"""
        logger.info(f"🔍 Testing Model Loading: {model_path}")
        self.total_tests += 1
        
        try:
            import tensorflow as tf
            
            # Load model
            start_time = tf.timestamp()
            model = tf.keras.models.load_model(model_path)
            load_time = tf.timestamp() - start_time
            
            logger.info(f"Model loaded in {float(load_time):.2f} seconds")
            logger.info(f"Input shape: {model.input_shape}")
            logger.info(f"Output shape: {model.output_shape}")
            logger.info(f"Total parameters: {model.count_params():,}")
            
            # Test prediction with dummy data
            if len(model.input_shape) >= 3:
                # Assume it's (batch, sequence, features)
                dummy_input = np.random.random((1, model.input_shape[1], model.input_shape[2])).astype(np.float32)
            else:
                # Fallback
                dummy_input = np.random.random((1, 30, 288)).astype(np.float32)
            
            prediction = model.predict(dummy_input, verbose=0)
            logger.info(f"Test prediction shape: {prediction.shape}")
            
            self.log_success(f"Model {Path(model_path).name} loaded and tested successfully")
            return True, model
            
        except Exception as e:
            self.log_issue(f"Model loading failed: {e}")
            logger.error(f"Full error: {traceback.format_exc()}")
            return False, None
    
    def test_pose_extraction(self):
        """Test pose extraction functionality"""
        logger.info("🔍 Testing Pose Extraction...")
        self.total_tests += 1
        
        try:
            import mediapipe as mp
            import cv2
            
            # Create a test image
            test_image = np.zeros((480, 640, 3), dtype=np.uint8)
            cv2.rectangle(test_image, (100, 100), (500, 400), (128, 128, 128), -1)
            cv2.circle(test_image, (300, 200), 50, (255, 255, 255), -1)
            
            # Test MediaPipe Holistic
            mp_holistic = mp.solutions.holistic
            with mp_holistic.Holistic(
                static_image_mode=False,
                model_complexity=1,
                enable_segmentation=False,
                refine_face_landmarks=False
            ) as holistic:
                
                # Convert BGR to RGB
                rgb_image = cv2.cvtColor(test_image, cv2.COLOR_BGR2RGB)
                results = holistic.process(rgb_image)
                
                # Check results
                detections = []
                if results.pose_landmarks:
                    detections.append(f"pose ({len(results.pose_landmarks.landmark)} landmarks)")
                if results.left_hand_landmarks:
                    detections.append(f"left_hand ({len(results.left_hand_landmarks.landmark)} landmarks)")
                if results.right_hand_landmarks:
                    detections.append(f"right_hand ({len(results.right_hand_landmarks.landmark)} landmarks)")
                
                logger.info(f"MediaPipe detections: {detections if detections else 'None (expected for dummy image)'}")
                
            # Test feature extraction
            try:
                features = self.extract_test_features(results)
                logger.info(f"Extracted features shape: {features.shape}")
                logger.info(f"Feature range: [{features.min():.4f}, {features.max():.4f}]")
                
                if features.shape[0] == 288:
                    self.log_success("Pose extraction working correctly")
                    return True, features
                else:
                    self.log_issue(f"Unexpected feature dimension: {features.shape[0]} (expected 288)")
                    return False, features
                    
            except Exception as e:
                self.log_issue(f"Feature extraction failed: {e}")
                return False, None
                
        except Exception as e:
            self.log_issue(f"Pose extraction test failed: {e}")
            return False, None
    
    def extract_test_features(self, results):
        """Extract 288 features from MediaPipe results"""
        features = []
        
        # Left hand landmarks (21 * 3 = 63 features)
        if results.left_hand_landmarks:
            for landmark in results.left_hand_landmarks.landmark:
                features.extend([landmark.x, landmark.y, landmark.z])
        else:
            features.extend([0.0] * 63)
            
        # Right hand landmarks (21 * 3 = 63 features)
        if results.right_hand_landmarks:
            for landmark in results.right_hand_landmarks.landmark:
                features.extend([landmark.x, landmark.y, landmark.z])
        else:
            features.extend([0.0] * 63)
            
        # Pose landmarks (33 * 4 = 132 features with visibility)
        if results.pose_landmarks:
            for landmark in results.pose_landmarks.landmark:
                features.extend([landmark.x, landmark.y, landmark.z, landmark.visibility])
        else:
            features.extend([0.0] * 132)
            
        # Face landmarks (10 * 3 = 30 features)
        if results.face_landmarks and len(results.face_landmarks.landmark) >= 10:
            for i in range(10):
                landmark = results.face_landmarks.landmark[i]
                features.extend([landmark.x, landmark.y, landmark.z])
        else:
            features.extend([0.0] * 30)
            
        return np.array(features, dtype=np.float32)
    
    def test_normalization(self):
        """Test normalization parameters"""
        logger.info("🔍 Testing Normalization...")
        self.total_tests += 1
        
        normalization_locations = [
            self.base_dir / 'data',
            self.script_dir / 'data',
            self.script_dir.parent / 'data'
        ]
        
        found_normalization = False
        
        for location in normalization_locations:
            means_path = location / 'feature_means.npy'
            stds_path = location / 'feature_stds.npy'
            
            if means_path.exists() and stds_path.exists():
                try:
                    means = np.load(means_path)
                    stds = np.load(stds_path)
                    
                    logger.info(f"Found normalization at: {location}")
                    logger.info(f"Means shape: {means.shape}, range: [{means.min():.4f}, {means.max():.4f}]")
                    logger.info(f"Stds shape: {stds.shape}, range: [{stds.min():.4f}, {stds.max():.4f}]")
                    
                    # Check for problematic values
                    zero_stds = np.sum(stds == 0)
                    if zero_stds > 0:
                        self.log_warning(f"Found {zero_stds} features with zero standard deviation")
                    
                    self.log_success("Normalization parameters found and valid")
                    found_normalization = True
                    return True, means, stds
                    
                except Exception as e:
                    self.log_warning(f"Error loading normalization from {location}: {e}")
                    continue
        
        if not found_normalization:
            self.log_issue("Normalization parameters not found")
            logger.info("Expected locations:")
            for location in normalization_locations:
                logger.info(f"  - {location}/feature_means.npy")
                logger.info(f"  - {location}/feature_stds.npy")
            return False, None, None
    
    def test_class_mappings(self):
        """Test class mappings and label encoders"""
        logger.info("🔍 Testing Class Mappings...")
        self.total_tests += 1
        
        mapping_locations = [
            self.base_dir / 'models',
            self.base_dir / 'trained_models',
            self.base_dir / 'data',
            self.script_dir / 'models',
            self.script_dir / 'trained_models'
        ]
        
        found_mappings = []
        
        for location in mapping_locations:
            # JSON mappings
            for json_file in location.glob('*class_mappings.json'):
                try:
                    with open(json_file, 'r', encoding='utf-8') as f:
                        mappings = json.load(f)
                    found_mappings.append({
                        'path': str(json_file),
                        'type': 'json',
                        'classes': len(mappings.get('index_to_class', mappings)) if isinstance(mappings, dict) else len(mappings)
                    })
                    logger.info(f"JSON mappings: {json_file.name} ({found_mappings[-1]['classes']} classes)")
                except Exception as e:
                    self.log_warning(f"Error loading {json_file}: {e}")
                    
            # Pickle label encoders
            for pkl_file in location.glob('*label_encoder.pkl'):
                try:
                    with open(pkl_file, 'rb') as f:
                        encoder = pickle.load(f)
                    classes = len(encoder.classes_) if hasattr(encoder, 'classes_') else 0
                    found_mappings.append({
                        'path': str(pkl_file),
                        'type': 'pickle',
                        'classes': classes
                    })
                    logger.info(f"Pickle encoder: {pkl_file.name} ({classes} classes)")
                except Exception as e:
                    self.log_warning(f"Error loading {pkl_file}: {e}")
        
        if found_mappings:
            self.log_success(f"Found {len(found_mappings)} class mapping files")
            return True, found_mappings
        else:
            self.log_issue("No class mappings or label encoders found")
            return False, []
    
    def test_end_to_end_prediction(self, model, normalization_params=None):
        """Test end-to-end prediction pipeline"""
        logger.info("🔍 Testing End-to-End Prediction...")
        self.total_tests += 1
        
        try:
            # Generate test pose sequence
            test_sequence = np.random.random((30, 288)).astype(np.float32)
            
            # Apply normalization if available
            if normalization_params:
                means, stds = normalization_params
                test_sequence = (test_sequence - means) / stds
                test_sequence = np.clip(test_sequence, -5.0, 5.0)
                logger.info("Applied normalization to test sequence")
            
            # Reshape for model input
            model_input = test_sequence.reshape(1, 30, 288)
            
            # Make prediction
            prediction = model.predict(model_input, verbose=0)
            predicted_class = np.argmax(prediction[0])
            confidence = float(np.max(prediction[0]))
            
            logger.info(f"Prediction successful:")
            logger.info(f"  Input shape: {model_input.shape}")
            logger.info(f"  Output shape: {prediction.shape}")
            logger.info(f"  Predicted class: {predicted_class}")
            logger.info(f"  Confidence: {confidence:.4f}")
            
            # Check if prediction seems reasonable
            if 0 <= predicted_class < prediction.shape[1] and 0 <= confidence <= 1:
                self.log_success("End-to-end prediction working correctly")
                return True
            else:
                self.log_issue(f"Prediction values seem unreasonable: class={predicted_class}, conf={confidence}")
                return False
                
        except Exception as e:
            self.log_issue(f"End-to-end prediction failed: {e}")
            logger.error(f"Full error: {traceback.format_exc()}")
            return False
    
    def test_file_permissions(self):
        """Test file permissions and accessibility"""
        logger.info("🔍 Testing File Permissions...")
        self.total_tests += 1
        
        important_dirs = [
            self.base_dir / 'models',
            self.base_dir / 'trained_models',
            self.base_dir / 'data',
            self.script_dir
        ]
        
        permission_issues = []
        
        for dir_path in important_dirs:
            if dir_path.exists():
                # Test read permission
                if not os.access(dir_path, os.R_OK):
                    permission_issues.append(f"No read access to {dir_path}")
                    
                # Test write permission for data directories
                if 'data' in str(dir_path) and not os.access(dir_path, os.W_OK):
                    permission_issues.append(f"No write access to {dir_path}")
            else:
                self.log_warning(f"Directory does not exist: {dir_path}")
        
        if permission_issues:
            for issue in permission_issues:
                self.log_issue(issue)
            return False
        else:
            self.log_success("File permissions are adequate")
            return True
    
    def generate_diagnostic_report(self):
        """Generate a comprehensive diagnostic report"""
        logger.info("📊 Generating Diagnostic Report...")
        
        report = {
            'diagnostic_summary': {
                'total_tests': self.total_tests,
                'successful_tests': self.success_count,
                'issues_found': len(self.issues),
                'warnings_found': len(self.warnings),
                'success_rate': f"{(self.success_count/self.total_tests*100):.1f}%" if self.total_tests > 0 else "0%"
            },
            'issues': self.issues,
            'warnings': self.warnings,
            'recommendations': []
        }
        
        # Generate recommendations
        if not self.issues:
            report['recommendations'].append("✅ All tests passed! Your system appears to be working correctly.")
        else:
            report['recommendations'].append("❌ Issues detected. Please address the following:")
            
            if "Missing packages" in str(self.issues):
                report['recommendations'].append("• Install missing Python packages using pip")
                
            if "No model files" in str(self.issues):
                report['recommendations'].append("• Ensure model files (.h5) are in models/ or trained_models/ directories")
                
            if "Normalization parameters not found" in str(self.issues):
                report['recommendations'].append("• Run calculate_normalization.py to generate normalization parameters")
                
            if "class mappings" in str(self.issues).lower():
                report['recommendations'].append("• Ensure class mappings (JSON/pickle files) are available")
                
            if "permission" in str(self.issues).lower():
                report['recommendations'].append("• Fix file permissions using chmod/chown commands")
        
        return report
    
    def run_full_diagnostic(self):
        """Run all diagnostic tests"""
        logger.info("🚀 Starting SilentVoice_BD Diagnostic...")
        logger.info("=" * 50)
        
        # Test 1: Dependencies
        deps_ok = self.test_dependencies()
        
        # Test 2: TensorFlow
        if deps_ok:
            tf_ok = self.test_tensorflow()
        else:
            tf_ok = False
            
        # Test 3: Find models
        models = self.find_models()
        
        # Test 4: Test model loading
        working_model = None
        if models:
            for model_info in models[:3]:  # Test first 3 models
                success, model = self.test_model_loading(model_info['path'])
                if success:
                    working_model = model
                    break
        
        # Test 5: Pose extraction
        pose_ok, test_features = self.test_pose_extraction()
        
        # Test 6: Normalization
        norm_ok, means, stds = self.test_normalization()
        normalization_params = (means, stds) if norm_ok else None
        
        # Test 7: Class mappings
        mappings_ok, mappings = self.test_class_mappings()
        
        # Test 8: File permissions
        perms_ok = self.test_file_permissions()
        
        # Test 9: End-to-end prediction
        if working_model is not None:
            e2e_ok = self.test_end_to_end_prediction(working_model, normalization_params)
        
        # Generate report
        report = self.generate_diagnostic_report()
        
        # Print summary
        logger.info("=" * 50)
        logger.info("🏁 DIAGNOSTIC COMPLETE")
        logger.info(f"Tests run: {report['diagnostic_summary']['total_tests']}")
        logger.info(f"Success rate: {report['diagnostic_summary']['success_rate']}")
        logger.info(f"Issues: {len(report['issues'])}")
        logger.info(f"Warnings: {len(report['warnings'])}")
        
        if report['issues']:
            logger.info("\n❌ ISSUES FOUND:")
            for i, issue in enumerate(report['issues'], 1):
                logger.info(f"{i}. {issue}")
                
        if report['warnings']:
            logger.info("\n⚠️ WARNINGS:")
            for i, warning in enumerate(report['warnings'], 1):
                logger.info(f"{i}. {warning}")
        
        logger.info("\n💡 RECOMMENDATIONS:")
        for rec in report['recommendations']:
            logger.info(rec)
        
        # Save report
        report_path = self.script_dir / 'diagnostic_report.json'
        with open(report_path, 'w', encoding='utf-8') as f:
            json.dump(report, f, ensure_ascii=False, indent=2)
        
        logger.info(f"\n📋 Full report saved to: {report_path}")
        
        return report

def main():
    """Run the diagnostic"""
    print("🩺 SilentVoice_BD System Diagnostic Tool")
    print("This tool will test all components of your sign language recognition system")
    print()
    
    diagnostic = SilentVoiceDiagnostic()
    report = diagnostic.run_full_diagnostic()
    
    # Exit with appropriate code
    if report['diagnostic_summary']['issues_found'] == 0:
        print("\n🎉 System is healthy!")
        sys.exit(0)
    else:
        print(f"\n⚠️ Found {report['diagnostic_summary']['issues_found']} issues that need attention")
        sys.exit(1)

if __name__ == "__main__":
    main()