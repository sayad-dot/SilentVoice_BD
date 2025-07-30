package com.example.silentvoice_bd.learning.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.silentvoice_bd.learning.model.Lesson;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findAllByOrderByDifficultyLevelAsc();

    List<Lesson> findByLessonTypeOrderByDifficultyLevelAsc(String lessonType);

    @Query("SELECT l FROM Lesson l WHERE l.difficultyLevel <= :maxLevel ORDER BY l.difficultyLevel ASC")
    List<Lesson> findByMaxDifficultyLevel(Integer maxLevel);

    Optional<Lesson> findByTitleIgnoreCase(String title);

    @Query("SELECT COUNT(l) FROM Lesson l WHERE l.lessonType = :lessonType")
    Long countByLessonType(String lessonType);

    @Query("SELECT AVG(l.estimatedDuration) FROM Lesson l WHERE l.lessonType = :lessonType")
    Double getAverageEstimatedDurationByType(String lessonType);
}
