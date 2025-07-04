package com.example.silentvoice_bd.repository;

import com.example.silentvoice_bd.model.ExtractedFrame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExtractedFrameRepository extends JpaRepository<ExtractedFrame, UUID> {

    List<ExtractedFrame> findByVideoFileIdOrderByTimestampSeconds(UUID videoFileId);

    List<ExtractedFrame> findByVideoFileIdAndIsKeyframeTrue(UUID videoFileId);

    @Query("SELECT f FROM ExtractedFrame f WHERE f.videoFileId = :videoFileId AND f.timestampSeconds BETWEEN :startTime AND :endTime ORDER BY f.timestampSeconds")
    List<ExtractedFrame> findFramesInTimeRange(
        @Param("videoFileId") UUID videoFileId,
        @Param("startTime") BigDecimal startTime,
        @Param("endTime") BigDecimal endTime
    );

    @Query("SELECT COUNT(f) FROM ExtractedFrame f WHERE f.videoFileId = :videoFileId")
    Long countFramesByVideoId(@Param("videoFileId") UUID videoFileId);

    void deleteByVideoFileId(UUID videoFileId);
}

