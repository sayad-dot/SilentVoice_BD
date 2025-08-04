package com.example.silentvoice_bd.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.silentvoice_bd.ai.models.SignLanguagePrediction;

@Repository
public interface SignLanguagePredictionRepository extends JpaRepository<SignLanguagePrediction, UUID> {

    /**
     * Find all predictions for a specific video file ID, ordered by creation
     * date (newest first) This matches the method call in
     * AIProcessingService.java
     */
    @Query("SELECT p FROM SignLanguagePrediction p WHERE p.videoFileId = :videoFileId ORDER BY p.createdAt DESC")
    List<SignLanguagePrediction> findByVideoFileIdOrderByCreatedAtDesc(@Param("videoFileId") UUID videoFileId);

    /**
     * Find high confidence predictions for a video file This matches the method
     * call in AIProcessingService.java
     */
    @Query("SELECT p FROM SignLanguagePrediction p WHERE p.videoFileId = :videoFileId AND p.confidenceScore >= :minConfidence ORDER BY p.createdAt DESC")
    List<SignLanguagePrediction> findHighConfidencePredictions(
            @Param("videoFileId") UUID videoFileId,
            @Param("minConfidence") BigDecimal minConfidence
    );

    /**
     * Count predictions for a video file This matches the method call in
     * AIProcessingService.java
     */
    @Query("SELECT COUNT(p) FROM SignLanguagePrediction p WHERE p.videoFileId = :videoFileId")
    Long countPredictionsByVideoId(@Param("videoFileId") UUID videoFileId);

    /**
     * Get accuracy statistics for a time period This matches the method call in
     * AIProcessingService.java
     */
    @Query("SELECT AVG(p.confidenceScore), MIN(p.confidenceScore), MAX(p.confidenceScore), COUNT(p) FROM SignLanguagePrediction p WHERE p.createdAt >= :since")
    Object[] getAccuracyStats(@Param("since") LocalDateTime since);

    /**
     * Get statistics by model version This matches the method call in
     * AIProcessingService.java
     */
    @Query("SELECT p.modelVersion, COUNT(p), AVG(p.confidenceScore) FROM SignLanguagePrediction p GROUP BY p.modelVersion ORDER BY COUNT(p) DESC")
    List<Object[]> getStatsByModelVersion();

    /**
     * Delete all predictions for a video file (useful when deleting a video)
     * This matches the method call in AIProcessingService.java
     */
    void deleteByVideoFileId(UUID videoFileId);

    /**
     * Check if predictions exist for a video file
     */
    boolean existsByVideoFileId(UUID videoFileId);

    /**
     * Find the latest prediction for a video file
     */
    @Query("SELECT p FROM SignLanguagePrediction p WHERE p.videoFileId = :videoFileId ORDER BY p.createdAt DESC LIMIT 1")
    Optional<SignLanguagePrediction> findLatestByVideoFileId(@Param("videoFileId") UUID videoFileId);

    /**
     * Find predictions by confidence score range
     */
    @Query("SELECT p FROM SignLanguagePrediction p WHERE p.confidenceScore >= :minConfidence AND p.confidenceScore <= :maxConfidence ORDER BY p.createdAt DESC")
    List<SignLanguagePrediction> findByConfidenceScoreBetween(
            @Param("minConfidence") BigDecimal minConfidence,
            @Param("maxConfidence") BigDecimal maxConfidence
    );

    /**
     * Find predictions by model version
     */
    List<SignLanguagePrediction> findByModelVersionOrderByCreatedAtDesc(String modelVersion);

    /**
     * Find predictions with processing time greater than specified milliseconds
     */
    @Query("SELECT p FROM SignLanguagePrediction p WHERE p.processingTimeMs > :processingTime ORDER BY p.processingTimeMs DESC")
    List<SignLanguagePrediction> findByProcessingTimeGreaterThan(@Param("processingTime") Integer processingTime);
}
