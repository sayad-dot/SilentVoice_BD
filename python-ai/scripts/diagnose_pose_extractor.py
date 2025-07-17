#!/usr/bin/env python3
import sys
import os
import importlib.util
import numpy as np

def load_pose_extractor_safely():
    """Safely load the pose extractor module to avoid circular imports"""
    try:
        # Get the absolute path to pose_extractor.py
        current_dir = os.path.dirname(os.path.abspath(__file__))
        pose_extractor_path = os.path.join(current_dir, 'pose_extractor.py')
        
        if not os.path.exists(pose_extractor_path):
            print(f"❌ ERROR: pose_extractor.py not found at {pose_extractor_path}")
            return None
        
        # Load the module dynamically
        spec = importlib.util.spec_from_file_location("pose_extractor_module", pose_extractor_path)
        pose_module = importlib.util.module_from_spec(spec)
        
        # Execute the module
        spec.loader.exec_module(pose_module)
        
        # Check if the class exists
        if hasattr(pose_module, 'OptimizedMediaPipePoseExtractor'):
            print("✅ Successfully loaded OptimizedMediaPipePoseExtractor")
            return pose_module.OptimizedMediaPipePoseExtractor
        else:
            print("❌ ERROR: OptimizedMediaPipePoseExtractor class not found in pose_extractor.py")
            return None
            
    except Exception as e:
        print(f"❌ ERROR loading pose extractor: {str(e)}")
        import traceback
        traceback.print_exc()
        return None

def diagnose_pose_extraction(video_path, ExtractorClass):
    """Diagnose pose extraction for a single video"""
    print(f"Diagnosing pose extraction for: {video_path}")
    
    # Check if video file exists
    if not os.path.exists(video_path):
        print(f"❌ ERROR: Video file not found: {video_path}")
        return False
    
    try:
        # Initialize extractor
        extractor = ExtractorClass()
        
        # Check if normalization parameters are loaded
        if hasattr(extractor, 'feature_means') and hasattr(extractor, 'feature_stds'):
            if extractor.feature_means is not None and extractor.feature_stds is not None:
                print(f"✅ Normalization parameters loaded")
                print(f"   Means shape: {extractor.feature_means.shape}")
                print(f"   Stds shape: {extractor.feature_stds.shape}")
            else:
                print("⚠️ WARNING: Normalization parameters are None")
        else:
            print("❌ ERROR: Extractor missing normalization attributes")
            return False
        
        # Extract landmarks
        landmarks = extractor.extract_pose_from_video_file(video_path, max_frames=30)
        
        if not landmarks:
            print("❌ ERROR: No landmarks extracted")
            return False
            
        landmarks = np.array(landmarks)
        
        print(f"✓ Pose extraction completed successfully")
        print(f"✓ Output shape: {landmarks.shape}")
        print(f"✓ Data type: {landmarks.dtype}")
        print(f"✓ Value range: {landmarks.min():.6f} to {landmarks.max():.6f}")
        print(f"✓ Mean: {landmarks.mean():.6f}")
        print(f"✓ Std: {landmarks.std():.6f}")
        
        # Check for common issues
        if landmarks.shape[0] == 0:
            print("❌ ERROR: No frames extracted")
            return False
            
        if landmarks.shape[1] == 0:
            print("❌ ERROR: No landmarks per frame")
            return False
            
        if np.isnan(landmarks).any():
            nan_count = np.isnan(landmarks).sum()
            print(f"❌ WARNING: {nan_count} NaN values found")
            
        if np.isinf(landmarks).any():
            inf_count = np.isinf(landmarks).sum()
            print(f"❌ WARNING: {inf_count} infinite values found")
            
        # CORRECTED NORMALIZATION CHECK
        mean_val = landmarks.mean()
        std_val = landmarks.std()
        
        print(f"\n🔍 Normalization Analysis:")
        print(f"   Mean: {mean_val:.6f} (should be close to 0)")
        print(f"   Std:  {std_val:.6f} (should be close to 1)")
        
        # Check if data appears to be properly normalized
        mean_ok = abs(mean_val) < 0.5
        std_ok = abs(std_val - 1.0) < 0.5
        
        if not mean_ok or not std_ok:
            print(f"❌ WARNING: Data appears unnormalized!")
            print(f"   Expected: mean≈0, std≈1")
            print(f"   Actual: mean={mean_val:.3f}, std={std_val:.3f}")
            print(f"   Mean check: {'✅' if mean_ok else '❌'}")
            print(f"   Std check: {'✅' if std_ok else '❌'}")
            return False
        else:
            print(f"✅ Data is properly normalized")
            print(f"   Mean≈0: ✅ ({abs(mean_val):.3f} < 0.5)")
            print(f"   Std≈1: ✅ ({abs(std_val - 1.0):.3f} < 0.5)")
            return True
            
    except Exception as e:
        print(f"❌ ERROR in pose extraction: {str(e)}")
        import traceback
        traceback.print_exc()
        return False

def test_normalization_files():
    """Test if normalization files exist and are valid"""
    print("\n" + "="*60)
    print("TESTING NORMALIZATION FILES")
    print("="*60)
    
    files_to_check = [
        "../data/feature_means.npy",
        "../data/feature_stds.npy",
        "../data/normalization_params.json"
    ]
    
    all_files_ok = True
    
    for file_path in files_to_check:
        if os.path.exists(file_path):
            file_size = os.path.getsize(file_path)
            print(f"✅ {file_path} exists ({file_size} bytes)")
            
            # Try to load numpy files
            if file_path.endswith('.npy'):
                try:
                    data = np.load(file_path)
                    print(f"   Shape: {data.shape}, Type: {data.dtype}")
                except Exception as e:
                    print(f"   ❌ Error loading: {e}")
                    all_files_ok = False
        else:
            print(f"❌ {file_path} NOT FOUND")
            all_files_ok = False
    
    return all_files_ok

def comprehensive_diagnosis():
    """Run comprehensive diagnosis"""
    print("🚀 COMPREHENSIVE POSE EXTRACTION DIAGNOSIS")
    print("="*60)
    
    # Try to load the pose extractor
    ExtractorClass = load_pose_extractor_safely()
    
    if ExtractorClass is None:
        print("❌ CRITICAL: Cannot load pose extractor")
        print("\n🔧 IMMEDIATE FIXES NEEDED:")
        print("   1. Ensure pose_extractor.py exists and is syntactically correct")
        print("   2. Check that OptimizedMediaPipePoseExtractor class is defined")
        print("   3. Remove any circular import issues")
        return
    
    # Test normalization files
    files_ok = test_normalization_files()
    
    # Test videos
    test_videos = [
        "/media/sayad/Ubuntu-Data/archive/balti/U1W219F_trial_4_L.mp4",
        "/media/sayad/Ubuntu-Data/archive/chini/U8W47F_trial_3_R.mp4",
        "/media/sayad/Ubuntu-Data/hi.mp4"
    ]
    
    all_passed = True
    valid_videos = []
    
    for video in test_videos:
        if os.path.exists(video):
            valid_videos.append(video)
    
    if not valid_videos:
        print("❌ ERROR: No valid test videos found")
        return
    
    for i, video in enumerate(valid_videos, 1):
        print(f"\n📹 Test {i}/{len(valid_videos)}: {video.split('/')[-1]}")
        print("="*50)
        
        try:
            success = diagnose_pose_extraction(video, ExtractorClass)
            if not success:
                all_passed = False
        except Exception as e:
            print(f"❌ Test {i} failed: {str(e)}")
            all_passed = False
    
    # Final summary
    print("\n" + "="*60)
    print("FINAL DIAGNOSIS SUMMARY")
    print("="*60)
    
    if files_ok and all_passed:
        print("✅ ALL TESTS PASSED")
        print("✅ Pose extraction is working correctly")
        print("✅ Normalization is properly applied")
        print("\n🎯 If you're still getting 1.7% confidence:")
        print("   1. Restart your Spring Boot application")
        print("   2. Clear any model caches")
        print("   3. Verify the model files are recent")
    else:
        print("❌ ISSUES FOUND")
        if not files_ok:
            print("❌ Normalization files missing or corrupted")
        if not all_passed:
            print("❌ Pose extraction or normalization problems detected")
        
        print("\n🔧 RECOMMENDED FIXES:")
        print("   1. Restart your Spring Boot application")
        print("   2. Verify normalization files exist in ../data/")
        print("   3. Check that pose_extractor.py is updated")

if __name__ == "__main__":
    comprehensive_diagnosis()
