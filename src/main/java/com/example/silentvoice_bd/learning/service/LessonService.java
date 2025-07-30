package com.example.silentvoice_bd.learning.service;

import com.example.silentvoice_bd.learning.dto.*;
import com.example.silentvoice_bd.learning.model.*;
import com.example.silentvoice_bd.learning.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class LessonService {

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonProgressRepository progressRepository;

    public List<LessonResponse> getAllLessonsWithProgress(UUID userId) {
        List<Lesson> lessons = lessonRepository.findAllByOrderByDifficultyLevelAsc();

        return lessons.stream().map(lesson -> {
            LessonProgress progress = progressRepository.findByUserIdAndLessonId(userId, lesson.getId())
                    .orElse(null);

            return mapToLessonResponse(lesson, progress);
        }).collect(Collectors.toList());
    }

    public LessonResponse getLessonWithProgress(Long lessonId, UUID userId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found with id: " + lessonId));

        LessonProgress progress = progressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElse(null);

        return mapToLessonResponse(lesson, progress);
    }

    public void startLesson(Long lessonId, UUID userId) {
        // Verify lesson exists
        lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found with id: " + lessonId));

        LessonProgress progress = progressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElse(new LessonProgress());

        if (progress.getId() == null) {
            progress.setUserId(userId);
            progress.setLessonId(lessonId);
            progress.setCreatedAt(LocalDateTime.now());
        }

        progress.setStatus("IN_PROGRESS");
        progress.setUpdatedAt(LocalDateTime.now());
        progressRepository.save(progress);
    }

    public void completeLesson(Long lessonId, UUID userId, Double accuracyScore, Integer timeSpent) {
        LessonProgress progress = progressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson progress not found. Please start the lesson first."));

        progress.setStatus("COMPLETED");
        progress.setAccuracyScore(accuracyScore);
        progress.setTimeSpent(timeSpent);
        progress.setAttempts(progress.getAttempts() + 1);
        progress.setCompletedAt(LocalDateTime.now());
        progress.setUpdatedAt(LocalDateTime.now());

        progressRepository.save(progress);
    }

    public List<LessonProgressResponse> getUserProgress(UUID userId) {
        List<LessonProgress> progressList = progressRepository.findByUserIdOrderByUpdatedAtDesc(userId);

        return progressList.stream().map(progress -> {
            Lesson lesson = lessonRepository.findById(progress.getLessonId()).orElse(null);

            return mapToLessonProgressResponse(progress, lesson);
        }).collect(Collectors.toList());
    }

    public List<LessonResponse> getLessonsByType(String lessonType, UUID userId) {
        List<Lesson> lessons = lessonRepository.findByLessonTypeOrderByDifficultyLevelAsc(lessonType);

        return lessons.stream().map(lesson -> {
            LessonProgress progress = progressRepository.findByUserIdAndLessonId(userId, lesson.getId())
                    .orElse(null);

            return mapToLessonResponse(lesson, progress);
        }).collect(Collectors.toList());
    }

    public UserLearningStatsResponse getUserLearningStats(UUID userId) {
        Long completedLessons = progressRepository.countCompletedLessonsByUser(userId);
        Double averageAccuracy = progressRepository.getAverageAccuracyByUser(userId);
        Long totalTimeSpent = progressRepository.getTotalTimeSpentByUser(userId);

        UserLearningStatsResponse stats = new UserLearningStatsResponse();
        stats.setCompletedLessons(completedLessons != null ? completedLessons : 0L);
        stats.setAverageAccuracy(averageAccuracy != null ? averageAccuracy : 0.0);
        stats.setTotalTimeSpent(totalTimeSpent != null ? totalTimeSpent : 0L);

        return stats;
    }

    private LessonResponse mapToLessonResponse(Lesson lesson, LessonProgress progress) {
        LessonResponse response = new LessonResponse();
        response.setId(lesson.getId());
        response.setTitle(lesson.getTitle());
        response.setDescription(lesson.getDescription());
        response.setLessonType(lesson.getLessonType());
        response.setDifficultyLevel(lesson.getDifficultyLevel());
        response.setEstimatedDuration(lesson.getEstimatedDuration());
        response.setContentData(lesson.getContentData());
        response.setCreatedAt(lesson.getCreatedAt());
        response.setUpdatedAt(lesson.getUpdatedAt());

        if (progress != null) {
            response.setStatus(progress.getStatus());
            response.setAccuracyScore(progress.getAccuracyScore());
            response.setAttempts(progress.getAttempts());
            response.setTimeSpent(progress.getTimeSpent());
            response.setCompletedAt(progress.getCompletedAt());
        } else {
            response.setStatus("NOT_STARTED");
            response.setAccuracyScore(0.0);
            response.setAttempts(0);
            response.setTimeSpent(0);
        }

        return response;
    }

    private LessonProgressResponse mapToLessonProgressResponse(LessonProgress progress, Lesson lesson) {
        LessonProgressResponse response = new LessonProgressResponse();
        response.setLessonId(progress.getLessonId());
        response.setLessonTitle(lesson != null ? lesson.getTitle() : "Unknown Lesson");
        response.setStatus(progress.getStatus());
        response.setAccuracyScore(progress.getAccuracyScore());
        response.setAttempts(progress.getAttempts());
        response.setTimeSpent(progress.getTimeSpent());
        response.setCompletedAt(progress.getCompletedAt());
        response.setCreatedAt(progress.getCreatedAt());
        response.setUpdatedAt(progress.getUpdatedAt());

        return response;
    }
}
