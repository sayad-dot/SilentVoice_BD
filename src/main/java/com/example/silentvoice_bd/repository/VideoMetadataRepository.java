package com.example.silentvoice_bd.repository;

import com.example.silentvoice_bd.model.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoMetadataRepository extends JpaRepository<VideoMetadata, UUID> {

    /**
     * Find video metadata by video file ID
     */
    Optional<VideoMetadata> findByVideoFileId(UUID videoFileId);

    /**
     * Find all video metadata for a video file ID, ordered by creation date
     * (newest first)
     */
    List<VideoMetadata> findByVideoFileIdOrderByCreatedAtDesc(UUID videoFileId);

    /**
     * Find videos with duration greater than specified seconds (FIXED FIELD
     * NAME)
     */
    @Query("SELECT v FROM VideoMetadata v WHERE v.durationSeconds > :minDurationSeconds ORDER BY v.durationSeconds DESC")
    List<VideoMetadata> findByDurationGreaterThan(@Param("minDurationSeconds") Integer minDurationSeconds);

    /**
     * Find videos by resolution (width and height)
     */
    @Query("SELECT v FROM VideoMetadata v WHERE v.width = :width AND v.height = :height ORDER BY v.createdAt DESC")
    List<VideoMetadata> findByResolution(@Param("width") Integer width, @Param("height") Integer height);

    /**
     * Find videos by codec
     */
    List<VideoMetadata> findByVideoCodecOrderByCreatedAtDesc(String videoCodec);

    /**
     * Find videos by file format
     */
    List<VideoMetadata> findByFileFormatOrderByCreatedAtDesc(String fileFormat);

    /**
     * Find videos that have audio
     */
    @Query("SELECT v FROM VideoMetadata v WHERE v.hasAudio = true ORDER BY v.createdAt DESC")
    List<VideoMetadata> findVideosWithAudio();

    /**
     * Find videos within duration range
     */
    @Query("SELECT v FROM VideoMetadata v WHERE v.durationSeconds BETWEEN :minSeconds AND :maxSeconds ORDER BY v.durationSeconds ASC")
    List<VideoMetadata> findByDurationRange(
            @Param("minSeconds") Integer minSeconds,
            @Param("maxSeconds") Integer maxSeconds
    );

    /**
     * Count video metadata records for a video file
     */
    @Query("SELECT COUNT(v) FROM VideoMetadata v WHERE v.videoFileId = :videoFileId")
    Long countByVideoFileId(@Param("videoFileId") UUID videoFileId);

    /**
     * Check if video metadata exists for a video file
     */
    boolean existsByVideoFileId(UUID videoFileId);

    /**
     * Get average duration of all videos
     */
    @Query("SELECT AVG(v.durationSeconds) FROM VideoMetadata v")
    Double getAverageDuration();

    /**
     * Get video statistics
     */
    @Query("SELECT MIN(v.durationSeconds), MAX(v.durationSeconds), AVG(v.durationSeconds), COUNT(v) FROM VideoMetadata v")
    Object[] getVideoStatistics();

    /**
     * Find videos by bitrate range
     */
    @Query("SELECT v FROM VideoMetadata v WHERE v.bitrate BETWEEN :minBitrate AND :maxBitrate ORDER BY v.bitrate DESC")
    List<VideoMetadata> findByBitrateRange(
            @Param("minBitrate") Integer minBitrate,
            @Param("maxBitrate") Integer maxBitrate
    );

    /**
     * CRITICAL: Delete video metadata by video file ID (for cascade deletion)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM VideoMetadata v WHERE v.videoFileId = :videoFileId")
    void deleteByVideoFileId(@Param("videoFileId") UUID videoFileId);

    /**
     * Delete all video metadata records (for cleanup operations)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM VideoMetadata v")
    void deleteAll();

    /**
     * Find videos created within a date range
     */
    @Query("SELECT v FROM VideoMetadata v WHERE v.createdAt BETWEEN :startDate AND :endDate ORDER BY v.createdAt DESC")
    List<VideoMetadata> findByCreatedAtBetween(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate
    );

    /**
     * Find videos by multiple criteria (advanced search)
     */
    @Query("SELECT v FROM VideoMetadata v WHERE "
            + "(:minDuration IS NULL OR v.durationSeconds >= :minDuration) AND "
            + "(:maxDuration IS NULL OR v.durationSeconds <= :maxDuration) AND "
            + "(:codec IS NULL OR v.videoCodec = :codec) AND "
            + "(:format IS NULL OR v.fileFormat = :format) AND "
            + "(:hasAudio IS NULL OR v.hasAudio = :hasAudio) "
            + "ORDER BY v.createdAt DESC")
    List<VideoMetadata> findByMultipleCriteria(
            @Param("minDuration") Integer minDuration,
            @Param("maxDuration") Integer maxDuration,
            @Param("codec") String codec,
            @Param("format") String format,
            @Param("hasAudio") Boolean hasAudio
    );
}
