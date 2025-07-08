package com.example.silentvoice_bd.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.silentvoice_bd.ai.models.SignLanguagePrediction;

@Repository
public interface SignLanguagePredictionRepository extends JpaRepository<SignLanguagePrediction, UUID> {

    // Find predictions by video ID
    List<SignLanguagePrediction> findByVideoFileIdOrderByCreatedAtDesc(UUID videoFileId);

    // Find high confidence predictions
    List<SignLanguagePrediction> findByConfidenceScoreGreaterThanEqualOrderByCreatedAtDesc(BigDecimal minConfidence);

    // Custom query for high confidence predictions by video
    @Query("SELECT s FROM SignLanguagePrediction s WHERE s.videoFileId = :videoFileId AND s.confidenceScore >= :minConfidence ORDER BY s.createdAt DESC")
    List<SignLanguagePrediction> findHighConfidencePredictions(
            @Param("videoFileId") UUID videoFileId,
            @Param("minConfidence") BigDecimal minConfidence
    );

    // Count predictions by video
    @Query("SELECT COUNT(s) FROM SignLanguagePrediction s WHERE s.videoFileId = :videoFileId")
    Long countPredictionsByVideoId(@Param("videoFileId") UUID videoFileId);

    // Get latest prediction for video
    Optional<SignLanguagePrediction> findTopByVideoFileIdOrderByCreatedAtDesc(UUID videoFileId);

    // Get predictions by date range
    @Query("SELECT s FROM SignLanguagePrediction s WHERE s.createdAt BETWEEN :startDate AND :endDate ORDER BY s.createdAt DESC")
    List<SignLanguagePrediction> findPredictionsByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Get statistics by model version
    @Query("SELECT s.modelVersion, COUNT(s), AVG(s.confidenceScore) FROM SignLanguagePrediction s GROUP BY s.modelVersion")
    List<Object[]> getStatsByModelVersion();

    // Delete predictions by video ID
    @Modifying
    @Transactional
    @Query("DELETE FROM SignLanguagePrediction s WHERE s.videoFileId = :videoFileId")
    void deleteByVideoFileId(@Param("videoFileId") UUID videoFileId);

    // Find recent predictions with pagination
    @Query("SELECT s FROM SignLanguagePrediction s ORDER BY s.createdAt DESC")
    List<SignLanguagePrediction> findRecentPredictions(org.springframework.data.domain.Pageable pageable);

    // Get prediction accuracy stats
    @Query("SELECT AVG(s.confidenceScore), MIN(s.confidenceScore), MAX(s.confidenceScore), COUNT(s) FROM SignLanguagePrediction s WHERE s.createdAt >= :since")
    Object[] getAccuracyStats(@Param("since") LocalDateTime since);
}
