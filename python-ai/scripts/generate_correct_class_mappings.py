#!/usr/bin/env python3

import json
import os
from pathlib import Path

def generate_bdslw60_class_mappings():
    """Generate correct class mappings from your training data structure"""
    
    # Your actual class names from training data
    actual_classes = [
        "aam", "aaple", "ac", "aids", "alu", "anaros", "angur", "apartment",
        "attio", "audio cassette", "ayna", "baandej", "baat", "baba",
        "balti", "balu", "bhai", "biscuts", "bon", "boroi", "bottam",
        "bou", "cake", "capsule", "cha", "chacha", "chachi", "chadar",
        "chal", "chikissha", "chini", "chips", "chiruni", "chocolate",
        "chokh utha", "chosma", "churi", "clip", "cream", "dada",
        "dadi", "daeitto", "dal", "debor", "denadar", "dengue",
        "doctor", "dongson", "dulavai", "durbol", "jomoj", "juta",
        "konna", "maa", "tattha", "toothpaste", "tshirt", "tubelight",
        "tupi", "tv"
    ]
    
    # Create class index to name mapping
    class_mappings = {}
    for i, class_name in enumerate(actual_classes):
        class_mappings[i] = class_name
        class_mappings[str(i)] = class_name  # Both int and string keys
    
    # Create reverse mapping (name to index)
    reverse_mappings = {name: i for i, name in enumerate(actual_classes)}
    
    # Enhanced mappings with Bangla translations
    bangla_translations = {
        "aam": "আম",
        "aaple": "আপেল", 
        "ac": "এসি",
        "aids": "এইডস",
        "alu": "আলু",
        "anaros": "আনারস",
        "angur": "আঙুর",
        "apartment": "অ্যাপার্টমেন্ট",
        "attio": "আত্তিও",
        "audio cassette": "অডিও ক্যাসেট",
        "ayna": "আয়না",
        "baandej": "ব্যান্ডেজ",
        "baat": "বাত",
        "baba": "বাবা",
        "balti": "বালতি",
        "balu": "বালু",
        "bhai": "ভাই",
        "biscuts": "বিস্কুট",
        "bon": "বোন",
        "boroi": "বরই",
        "bottam": "বোতাম",
        "bou": "বউ",
        "cake": "কেক",
        "capsule": "ক্যাপসুল",
        "cha": "চা",
        "chacha": "চাচা",
        "chachi": "চাচি",
        "chadar": "চাদর",
        "chal": "চাল",
        "chikissha": "চিকিৎসা",
        "chini": "চিনি",
        "chips": "চিপস",
        "chiruni": "চিরুনি",
        "chocolate": "চকলেট",
        "chokh utha": "চোখ ওঠা",
        "chosma": "চশমা",
        "churi": "চুড়ি",
        "clip": "ক্লিপ",
        "cream": "ক্রিম",
        "dada": "দাদা",
        "dadi": "দাদি",
        "daeitto": "দৈত্য",
        "dal": "দাল",
        "debor": "দেবর",
        "denadar": "দেনাদার",
        "dengue": "ডেঙ্গু",
        "doctor": "ডাক্তার",
        "dongson": "দংশন",
        "dulavai": "দুলাভাই",
        "durbol": "দুর্বল",
        "jomoj": "জমজ",
        "juta": "জুতা",
        "konna": "কন্যা",
        "maa": "মা",
        "tattha": "তত্ত্ব",
        "toothpaste": "টুথপেস্ট",
        "tshirt": "টি-শার্ট",
        "tubelight": "টিউবলাইট",
        "tupi": "টুপি",
        "tv": "টিভি"
    }
    
    # Create comprehensive mapping structure
    comprehensive_mappings = {
        "class_to_index": reverse_mappings,
        "index_to_class": class_mappings,
        "english_to_bangla": bangla_translations,
        "bangla_to_english": {v: k for k, v in bangla_translations.items()},
        "total_classes": len(actual_classes),
        "dataset": "BDSLW60",
        "model_type": "bangla_lstm"
    }
    
    return comprehensive_mappings

def save_class_mappings():
    """Save mappings to multiple locations"""
    mappings = generate_bdslw60_class_mappings()
    
    script_dir = Path(__file__).parent
    save_locations = [
        script_dir / "../models/class_mappings.json",
        script_dir / "../trained_models/class_mappings.json",
        script_dir / "../data/class_mappings.json",
        script_dir / "../models/bdslw60_class_mappings.json"
    ]
    
    for save_path in save_locations:
        save_path.parent.mkdir(parents=True, exist_ok=True)
        
        try:
            with open(save_path, 'w', encoding='utf-8') as f:
                json.dump(mappings, f, ensure_ascii=False, indent=2)
            print(f"✅ Saved mappings to: {save_path}")
        except Exception as e:
            print(f"❌ Failed to save to {save_path}: {e}")
    
    # Print sample mappings for verification
    print("\n📋 Sample Class Mappings:")
    for i in range(min(10, len(mappings['index_to_class']))):
        english = mappings['index_to_class'][i]
        bangla = mappings['english_to_bangla'][english]
        print(f"  {i}: {english} -> {bangla}")
    
    print(f"\n✅ Total classes: {mappings['total_classes']}")
    return mappings

if __name__ == "__main__":
    print("🔧 Generating correct BDSLW60 class mappings...")
    mappings = save_class_mappings()
    print("✅ Class mappings generation complete!")
