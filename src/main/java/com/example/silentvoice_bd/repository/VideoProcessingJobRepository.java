package com.example.silentvoice_bd.repository;

import com.example.silentvoice_bd.model.VideoProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoProcessingJobRepository extends JpaRepository<VideoProcessingJob, UUID> {

    List<VideoProcessingJob> findByVideoFileIdOrderByCreatedAtDesc(UUID videoFileId);

    List<VideoProcessingJob> findByStatusOrderByCreatedAtAsc(VideoProcessingJob.ProcessingStatus status);

    Optional<VideoProcessingJob> findByVideoFileIdAndJobType(UUID videoFileId, VideoProcessingJob.JobType jobType);

    @Query("SELECT v FROM VideoProcessingJob v WHERE v.status = :status AND v.jobType = :jobType ORDER BY v.createdAt ASC")
    List<VideoProcessingJob> findPendingJobsByType(
        @Param("status") VideoProcessingJob.ProcessingStatus status,
        @Param("jobType") VideoProcessingJob.JobType jobType
    );

    @Query("SELECT COUNT(v) FROM VideoProcessingJob v WHERE v.videoFileId = :videoFileId AND v.status = 'PROCESSING'")
    Long countActiveJobsForVideo(@Param("videoFileId") UUID videoFileId);
}
