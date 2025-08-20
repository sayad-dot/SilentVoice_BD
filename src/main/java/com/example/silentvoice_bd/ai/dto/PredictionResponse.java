package com.example.silentvoice_bd.ai.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PredictionResponse {

    private boolean success = true;

    @JsonProperty("predicted_text")
    private String predictedText;

    private double confidence;

    @JsonProperty("processing_time_ms")
    private Integer processingTimeMs;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("prediction_id")
    private UUID predictionId;

    @JsonProperty("video_id")
    private UUID videoId;

    private String error;

    @JsonProperty("processing_info")
    private Map<String, Object> processingInfo;

    // ✅ Translation fields
    @JsonProperty("bangla_translation")
    private String banglaTranslation;

    @JsonProperty("english_translation")
    private String englishTranslation;

    // Additional fields for debugging normalization issues
    @JsonProperty("normalization_applied")
    private Boolean normalizationApplied;

    @JsonProperty("data_statistics")
    private Map<String, Object> dataStatistics;

    // Constructors
    public PredictionResponse() {
    }

    public PredictionResponse(String predictedText, double confidence, Integer processingTimeMs, UUID predictionId) {
        this.predictedText = predictedText;
        this.confidence = confidence;
        this.processingTimeMs = processingTimeMs;
        this.predictionId = predictionId;

        // ✅ Automatically set translation when prediction is set
        this.banglaTranslation = getBanglaTranslation(predictedText);
        this.englishTranslation = getEnglishTranslation(predictedText);
    }

    // Static factory methods
    public static PredictionResponse error(String errorMessage) {
        PredictionResponse response = new PredictionResponse();
        response.setSuccess(false);
        response.setError(errorMessage);
        response.setPredictedText("ত্রুটি"); // "Error" in Bangla
        response.setConfidence(0.0);
        return response;
    }

    public static PredictionResponse success(String predictedText, double confidence,
            Integer processingTimeMs, UUID predictionId) {
        return new PredictionResponse(predictedText, confidence, processingTimeMs, predictionId);
    }

    // ✅ Translation methods
    private String getBanglaTranslation(String englishText) {
        if (englishText == null) {
            return "Translation not available";
        }

        Map<String, String> translations = new HashMap<>();

        // BDSLW60 English to Bangla mappings
        translations.put("dongson", "দংশন");
        translations.put("attio", "আত্তিও");
        translations.put("durbol", "দুর্বল");
        translations.put("denadar", "দেনাদার");
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

        // Medical/Health terms
        translations.put("capsule", "ক্যাপসুল");
        translations.put("chikissha", "চিকিৎসা");
        translations.put("doctor", "ডাক্তার");
        translations.put("aids", "এইডস");
        translations.put("dengue", "ডেঙ্গু");
        translations.put("baat", "বাত");
        translations.put("chokh utha", "চোখ ওঠা");

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

        // Special terms
        translations.put("apartment", "অ্যাপার্টমেন্ট");
        translations.put("audio cassette", "অডিও ক্যাসেট");
        translations.put("baandej", "ব্যান্ডেজ");
        translations.put("balu", "বালু");
        translations.put("ac", "এসি");
        translations.put("daeitto", "দৈত্য");
        translations.put("tattha", "তত্ত্ব");

        String lowerInput = englishText.trim().toLowerCase();
        return translations.getOrDefault(lowerInput, englishText); // Return original if no translation
    }

    private String getEnglishTranslation(String input) {
        if (input == null) {
            return "Translation not available";
        }

        Map<String, String> englishMeanings = new HashMap<>();

        // Family members
        englishMeanings.put("dongson", "Bite/Sting");
        englishMeanings.put("attio", "Attio");
        englishMeanings.put("durbol", "Weak");
        englishMeanings.put("denadar", "Debtor");
        englishMeanings.put("dada", "Grandfather/Elder Brother");
        englishMeanings.put("dadi", "Grandmother");
        englishMeanings.put("maa", "Mother");
        englishMeanings.put("baba", "Father");
        englishMeanings.put("bhai", "Brother");
        englishMeanings.put("bon", "Sister");
        englishMeanings.put("chacha", "Uncle");
        englishMeanings.put("chachi", "Aunt");
        englishMeanings.put("debor", "Brother-in-law");
        englishMeanings.put("dulavai", "Brother-in-law");
        englishMeanings.put("bou", "Sister-in-law");
        englishMeanings.put("konna", "Daughter");
        englishMeanings.put("jomoj", "Twins");

        // Fruits and Food
        englishMeanings.put("aam", "Mango");
        englishMeanings.put("anaros", "Pineapple");
        englishMeanings.put("angur", "Grapes");
        englishMeanings.put("aaple", "Apple");
        englishMeanings.put("boroi", "Jujube");
        englishMeanings.put("cha", "Tea");
        englishMeanings.put("dal", "Lentils");
        englishMeanings.put("chal", "Rice");
        englishMeanings.put("chini", "Sugar");
        englishMeanings.put("biscuts", "Biscuits");
        englishMeanings.put("chocolate", "Chocolate");
        englishMeanings.put("chips", "Chips");
        englishMeanings.put("cake", "Cake");
        englishMeanings.put("alu", "Potato");

        // Medical/Health
        englishMeanings.put("capsule", "Capsule");
        englishMeanings.put("chikissha", "Treatment");
        englishMeanings.put("doctor", "Doctor");
        englishMeanings.put("aids", "AIDS");
        englishMeanings.put("dengue", "Dengue");
        englishMeanings.put("baat", "Arthritis");
        englishMeanings.put("chokh utha", "Eye Infection");

        // Household items
        englishMeanings.put("ayna", "Mirror");
        englishMeanings.put("balti", "Bucket");
        englishMeanings.put("chadar", "Sheet");
        englishMeanings.put("chiruni", "Comb");
        englishMeanings.put("chosma", "Glasses");
        englishMeanings.put("churi", "Bangles");
        englishMeanings.put("clip", "Clip");
        englishMeanings.put("cream", "Cream");
        englishMeanings.put("juta", "Shoes");
        englishMeanings.put("bottam", "Button");
        englishMeanings.put("toothpaste", "Toothpaste");
        englishMeanings.put("tshirt", "T-shirt");
        englishMeanings.put("tubelight", "Tube Light");
        englishMeanings.put("tupi", "Cap");
        englishMeanings.put("tv", "TV");

        // Special terms
        englishMeanings.put("apartment", "Apartment");
        englishMeanings.put("audio cassette", "Audio Cassette");
        englishMeanings.put("baandej", "Bandage");
        englishMeanings.put("balu", "Sand");
        englishMeanings.put("ac", "AC");
        englishMeanings.put("daeitto", "Monster");
        englishMeanings.put("tattha", "Principle");

        String lowerInput = input.trim().toLowerCase();
        return englishMeanings.getOrDefault(lowerInput, "Translation not available");
    }

    // Helper methods for debugging
    public boolean isHighConfidence() {
        return confidence > 0.7;
    }

    public boolean isLowConfidence() {
        return confidence < 0.1;
    }

    public String getConfidenceLevel() {
        if (confidence > 0.8) {
            return "HIGH";
        }
        if (confidence > 0.5) {
            return "MEDIUM";
        }
        if (confidence > 0.2) {
            return "LOW";
        }
        return "VERY_LOW";
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getPredictedText() {
        return predictedText;
    }

    // ✅ Updated setter for predictedText to update translations
    public void setPredictedText(String predictedText) {
        this.predictedText = predictedText;
        this.banglaTranslation = getBanglaTranslation(predictedText);
        this.englishTranslation = getEnglishTranslation(predictedText);
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
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

    public UUID getPredictionId() {
        return predictionId;
    }

    public void setPredictionId(UUID predictionId) {
        this.predictionId = predictionId;
    }

    public UUID getVideoId() {
        return videoId;
    }

    public void setVideoId(UUID videoId) {
        this.videoId = videoId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Map<String, Object> getProcessingInfo() {
        return processingInfo;
    }

    public void setProcessingInfo(Map<String, Object> processingInfo) {
        this.processingInfo = processingInfo;
    }

    // ✅ Getters and setters for translations
    public String getBanglaTranslation() {
        return banglaTranslation;
    }

    public void setBanglaTranslation(String banglaTranslation) {
        this.banglaTranslation = banglaTranslation;
    }

    public String getEnglishTranslation() {
        return englishTranslation;
    }

    public void setEnglishTranslation(String englishTranslation) {
        this.englishTranslation = englishTranslation;
    }

    public Boolean getNormalizationApplied() {
        return normalizationApplied;
    }

    public void setNormalizationApplied(Boolean normalizationApplied) {
        this.normalizationApplied = normalizationApplied;
    }

    public Map<String, Object> getDataStatistics() {
        return dataStatistics;
    }

    public void setDataStatistics(Map<String, Object> dataStatistics) {
        this.dataStatistics = dataStatistics;
    }

    @Override
    public String toString() {
        return "PredictionResponse{"
                + "success=" + success
                + ", predictedText='" + predictedText + '\''
                + ", banglaTranslation='" + banglaTranslation + '\''
                + ", englishTranslation='" + englishTranslation + '\''
                + ", confidence=" + confidence
                + ", processingTimeMs=" + processingTimeMs
                + ", modelVersion='" + modelVersion + '\''
                + ", error='" + error + '\''
                + '}';
    }
}
