package com.example.silentvoice_bd.learning.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.silentvoice_bd.learning.model.LessonProgress;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByUserIdAndLessonId(UUID userId, Long lessonId);

    List<LessonProgress> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    List<LessonProgress> findByUserIdAndStatus(UUID userId, String status);

    @Query("SELECT lp FROM LessonProgress lp WHERE lp.userId = :userId AND lp.status = 'COMPLETED'")
    List<LessonProgress> findCompletedLessonsByUser(@Param("userId") UUID userId);

    @Query("SELECT COUNT(lp) FROM LessonProgress lp WHERE lp.userId = :userId AND lp.status = 'COMPLETED'")
    Long countCompletedLessonsByUser(@Param("userId") UUID userId);

    @Query("SELECT AVG(lp.accuracyScore) FROM LessonProgress lp WHERE lp.userId = :userId AND lp.status = 'COMPLETED'")
    Double getAverageAccuracyByUser(@Param("userId") UUID userId);

    @Query("SELECT SUM(lp.timeSpent) FROM LessonProgress lp WHERE lp.userId = :userId")
    Long getTotalTimeSpentByUser(@Param("userId") UUID userId);

    @Query("SELECT lp FROM LessonProgress lp WHERE lp.lessonId = :lessonId ORDER BY lp.accuracyScore DESC")
    List<LessonProgress> findTopPerformersByLesson(@Param("lessonId") Long lessonId);
}
