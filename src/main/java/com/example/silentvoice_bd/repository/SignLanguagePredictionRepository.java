package com.example.silentvoice_bd.repository;

import com.example.silentvoice_bd.ai.models.SignLanguagePrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SignLanguagePredictionRepository extends JpaRepository<SignLanguagePrediction, UUID> {

    List<SignLanguagePrediction> findByVideoFileIdOrderByCreatedAtDesc(UUID videoFileId);

    List<SignLanguagePrediction> findByConfidenceScoreGreaterThanOrderByCreatedAtDesc(BigDecimal minConfidence);

    @Query("SELECT s FROM SignLanguagePrediction s WHERE s.videoFileId = :videoFileId AND s.confidenceScore >= :minConfidence ORDER BY s.createdAt DESC")
    List<SignLanguagePrediction> findHighConfidencePredictions(
        @Param("videoFileId") UUID videoFileId,
        @Param("minConfidence") BigDecimal minConfidence
    );

    @Query("SELECT COUNT(s) FROM SignLanguagePrediction s WHERE s.videoFileId = :videoFileId")
    Long countPredictionsByVideoId(@Param("videoFileId") UUID videoFileId);

    Optional<SignLanguagePrediction> findTopByVideoFileIdOrderByCreatedAtDesc(UUID videoFileId);

    void deleteByVideoFileId(UUID videoFileId);
}
