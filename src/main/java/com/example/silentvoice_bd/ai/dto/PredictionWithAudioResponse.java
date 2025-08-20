package com.example.silentvoice_bd.ai.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PredictionWithAudioResponse {

    private UUID id;
    private UUID videoFileId;

    @JsonProperty("predicted_text")
    private String predictedText;

    @JsonProperty("confidence_score")
    private Double confidenceScore;

    @JsonProperty("processing_time_ms")
    private Integer processingTimeMs;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("audio_url")
    private String audioUrl;

    private boolean hasAudio;

    @JsonProperty("english_translation")
    private String englishTranslation;

    // Constructors
    public PredictionWithAudioResponse() {
    }

    public PredictionWithAudioResponse(UUID id, UUID videoFileId, String predictedText,
            Double confidenceScore, Integer processingTimeMs,
            String modelVersion, LocalDateTime createdAt) {
        this.id = id;
        this.videoFileId = videoFileId;
        this.predictedText = predictedText;
        this.confidenceScore = confidenceScore;
        this.processingTimeMs = processingTimeMs;
        this.modelVersion = modelVersion;
        this.createdAt = createdAt;
        this.englishTranslation = getTranslation(predictedText);
    }

    private String getTranslation(String inputText) {
        if (inputText == null) {
            return "Translation not available";
        }

        String input = inputText.trim().toLowerCase();

        // Direct mapping for your current predictions
        switch (input) {
            case "dongson":
                return "দংশন";
            case "attio":
                return "আত্তিও";
            case "durbol":
                return "দুর্বল";
            case "denadar":
                return "দেনাদার";
            case "dada":
                return "দাদা";
            case "dadi":
                return "দাদি";
            case "maa":
                return "মা";
            case "baba":
                return "বাবা";
            default:
                return "Translation not available";
        }
    }

    private Map<String, String> createBDSLW60TranslationMap() {
        Map<String, String> translations = new HashMap<>();

        // ========== ENGLISH TO BANGLA (Your model outputs English) ==========
        // Family members
        translations.put("dada", "দাদা");
        translations.put("dadi", "দাদি");
        translations.put("maa", "মা");
        translations.put("baba", "বাবা");
        translations.put("bhai", "ভাই");
        translations.put("bon", "বোন");
        translations.put("chacha", "চাচা");
        translations.put("chachi", "চাচি");
        translations.put("debor", "দেবর");
        translations.put("dulavai", "দুলাভাই");
        translations.put("bou", "বউ");
        translations.put("konna", "কন্যা");
        translations.put("jomoj", "জমজ");

        // Fruits and Food
        translations.put("aam", "আম");
        translations.put("anaros", "আনারস");
        translations.put("angur", "আঙুর");
        translations.put("aaple", "আপেল");
        translations.put("boroi", "বরই");
        translations.put("cha", "চা");
        translations.put("dal", "দাল");
        translations.put("chal", "চাল");
        translations.put("chini", "চিনি");
        translations.put("biscuts", "বিস্কুট");
        translations.put("chocolate", "চকলেট");
        translations.put("chips", "চিপস");
        translations.put("cake", "কেক");
        translations.put("alu", "আলু");

        // Medical/Health terms (from your database)
        translations.put("capsule", "ক্যাপসুল");
        translations.put("chikissha", "চিকিৎসা");
        translations.put("doctor", "ডাক্তার");
        translations.put("aids", "এইডস");
        translations.put("dengue", "ডেঙ্গু");
        translations.put("baat", "বাত");
        translations.put("chokh utha", "চোখ ওঠা");
        translations.put("durbol", "দুর্বল");        // ✅ From your database
        translations.put("dongson", "দংশন");        // ✅ From your database
        translations.put("denadar", "দেনাদার");     // ✅ From your database

        // Household items
        translations.put("ayna", "আয়না");
        translations.put("balti", "বালতি");
        translations.put("chadar", "চাদর");
        translations.put("chiruni", "চিরুনি");
        translations.put("chosma", "চশমা");
        translations.put("churi", "চুড়ি");
        translations.put("clip", "ক্লিপ");
        translations.put("cream", "ক্রিম");
        translations.put("juta", "জুতা");
        translations.put("bottam", "বোতাম");
        translations.put("toothpaste", "টুথপেস্ট");
        translations.put("tshirt", "টি-শার্ট");
        translations.put("tubelight", "টিউবলাইট");
        translations.put("tupi", "টুপি");
        translations.put("tv", "টিভি");

        // Special terms (from your database)
        translations.put("attio", "আত্তিও");         // ✅ From your database
        translations.put("apartment", "অ্যাপার্টমেন্ট");
        translations.put("audio cassette", "অডিও ক্যাসেট");
        translations.put("baandej", "ব্যান্ডেজ");
        translations.put("balu", "বালু");
        translations.put("ac", "এসি");
        translations.put("daeitto", "দৈত্য");
        translations.put("tattha", "তত্ত্ব");

        // ========== BANGLA TO ENGLISH (Reverse mapping) ==========
        translations.put("দাদা", "Grandfather / Elder Brother");
        translations.put("দাদি", "Grandmother");
        translations.put("মা", "Mother");
        translations.put("বাবা", "Father");
        translations.put("ভাই", "Brother");
        translations.put("বোন", "Sister");
        translations.put("চাচা", "Uncle");
        translations.put("চাচি", "Aunt");
        translations.put("দেবর", "Brother-in-law");
        translations.put("দুলাভাই", "Brother-in-law");
        translations.put("বউ", "Sister-in-law");
        translations.put("কন্যা", "Daughter");
        translations.put("জমজ", "Twins");

        translations.put("আম", "Mango");
        translations.put("আনারস", "Pineapple");
        translations.put("আঙুর", "Grapes");
        translations.put("আপেল", "Apple");
        translations.put("বরই", "Jujube");
        translations.put("চা", "Tea");
        translations.put("দাল", "Lentils");
        translations.put("চাল", "Rice");
        translations.put("চিনি", "Sugar");
        translations.put("বিস্কুট", "Biscuits");
        translations.put("চকলেট", "Chocolate");
        translations.put("চিপস", "Chips");
        translations.put("কেক", "Cake");
        translations.put("আলু", "Potato");

        translations.put("ক্যাপসুল", "Capsule");
        translations.put("চিকিৎসা", "Treatment");
        translations.put("ডাক্তার", "Doctor");
        translations.put("এইডস", "AIDS");
        translations.put("ডেঙ্গু", "Dengue");
        translations.put("বাত", "Arthritis");
        translations.put("চোখ ওঠা", "Eye Infection");
        translations.put("দুর্বল", "Weak");
        translations.put("দংশন", "Bite/Sting");
        translations.put("দেনাদার", "Debtor");

        translations.put("আয়না", "Mirror");
        translations.put("বালতি", "Bucket");
        translations.put("চাদর", "Sheet");
        translations.put("চিরুনি", "Comb");
        translations.put("চশমা", "Glasses");
        translations.put("চুড়ি", "Bangles");
        translations.put("ক্লিপ", "Clip");
        translations.put("ক্রিম", "Cream");
        translations.put("জুতা", "Shoes");
        translations.put("বোতাম", "Button");
        translations.put("টুথপেস্ট", "Toothpaste");
        translations.put("টি-শার্ট", "T-shirt");
        translations.put("টিউবলাইট", "Tube Light");
        translations.put("টুপি", "Cap");
        translations.put("টিভি", "TV");

        translations.put("আত্তিও", "Attio");
        translations.put("অ্যাপার্টমেন্ট", "Apartment");
        translations.put("অডিও ক্যাসেট", "Audio Cassette");
        translations.put("ব্যান্ডেজ", "Bandage");
        translations.put("বালু", "Sand");
        translations.put("এসি", "AC");
        translations.put("দৈত্য", "Monster");
        translations.put("তত্ত্ব", "Principle");

        return translations;
    }

    private String handleVariations(String input) {
        // Handle common variations and alternative spellings using HashMap
        // FIXED: Using HashMap instead of Map.of() to avoid 10 entry limit
        Map<String, String> variations = new HashMap<>();

        variations.put("mama", "মামা");
        variations.put("didi", "দিদি");
        variations.put("ami", "আমি");
        variations.put("tumi", "তুমি");
        variations.put("apni", "আপনি");
        variations.put("kemon", "কেমন");
        variations.put("bhalo", "ভালো");
        variations.put("kharap", "খারাপ");
        variations.put("dhonnobad", "ধন্যবাদ");
        variations.put("dukkhito", "দুঃখিত");
        variations.put("hello", "হ্যালো");
        variations.put("nam", "নাম");
        variations.put("kaj", "কাজ");
        variations.put("school", "স্কুল");
        variations.put("bari", "বাড়ি");

        return variations.getOrDefault(input, "Translation not available");
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getVideoFileId() {
        return videoFileId;
    }

    public void setVideoFileId(UUID videoFileId) {
        this.videoFileId = videoFileId;
    }

    public String getPredictedText() {
        return predictedText;
    }

    public void setPredictedText(String predictedText) {
        this.predictedText = predictedText;
        this.englishTranslation = getTranslation(predictedText);
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Integer getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Integer processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public boolean isHasAudio() {
        return hasAudio;
    }

    public void setHasAudio(boolean hasAudio) {
        this.hasAudio = hasAudio;
    }

    public String getEnglishTranslation() {
        return englishTranslation;
    }

    public void setEnglishTranslation(String englishTranslation) {
        this.englishTranslation = englishTranslation;
    }
}
