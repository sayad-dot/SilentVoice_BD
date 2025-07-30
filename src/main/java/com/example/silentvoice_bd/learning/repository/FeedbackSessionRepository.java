package com.example.silentvoice_bd.learning.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.silentvoice_bd.learning.model.FeedbackSession;

@Repository
public interface FeedbackSessionRepository extends JpaRepository<FeedbackSession, Long> {

    List<FeedbackSession> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<FeedbackSession> findByUserIdAndLessonIdOrderByCreatedAtDesc(UUID userId, Long lessonId);

    List<FeedbackSession> findByLessonIdOrderByCreatedAtDesc(Long lessonId);

    @Query("SELECT fs FROM FeedbackSession fs WHERE fs.userId = :userId AND fs.createdAt >= :since ORDER BY fs.createdAt DESC")
    List<FeedbackSession> findRecentSessionsByUser(
            @Param("userId") UUID userId,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT AVG(fs.confidenceScore) FROM FeedbackSession fs WHERE fs.userId = :userId AND fs.lessonId = :lessonId")
    Double getAverageConfidenceByUserAndLesson(@Param("userId") UUID userId, @Param("lessonId") Long lessonId);

    @Query("SELECT COUNT(fs) FROM FeedbackSession fs WHERE fs.userId = :userId AND fs.confidenceScore >= :minConfidence")
    Long countSuccessfulSessionsByUser(@Param("userId") UUID userId, @Param("minConfidence") Double minConfidence);

    @Query("SELECT fs FROM FeedbackSession fs WHERE fs.sessionId = :sessionId ORDER BY fs.createdAt ASC")
    List<FeedbackSession> findBySessionId(@Param("sessionId") String sessionId);

    @Query("SELECT MAX(fs.confidenceScore) FROM FeedbackSession fs WHERE fs.userId = :userId AND fs.lessonId = :lessonId")
    Double getMaxConfidenceByUserAndLesson(@Param("userId") UUID userId, @Param("lessonId") Long lessonId);
}
