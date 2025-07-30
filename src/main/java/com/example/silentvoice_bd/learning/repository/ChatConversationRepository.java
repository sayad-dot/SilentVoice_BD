package com.example.silentvoice_bd.learning.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.silentvoice_bd.learning.model.ChatConversation;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    List<ChatConversation> findByUserIdAndLessonIdOrderByCreatedAtAsc(UUID userId, Long lessonId);

    List<ChatConversation> findByUserIdAndLessonIdOrderByCreatedAtDesc(UUID userId, Long lessonId);

    List<ChatConversation> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT cc FROM ChatConversation cc WHERE cc.userId = :userId AND cc.lessonId = :lessonId AND cc.createdAt >= :since ORDER BY cc.createdAt DESC")
    List<ChatConversation> findRecentConversation(
            @Param("userId") UUID userId,
            @Param("lessonId") Long lessonId,
            @Param("since") LocalDateTime since
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM ChatConversation cc WHERE cc.userId = :userId AND cc.lessonId = :lessonId")
    void deleteByUserIdAndLessonId(@Param("userId") UUID userId, @Param("lessonId") Long lessonId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ChatConversation cc WHERE cc.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(cc) FROM ChatConversation cc WHERE cc.userId = :userId AND cc.sender = 'USER'")
    Long countUserMessagesByUser(@Param("userId") UUID userId);

    @Query("SELECT cc FROM ChatConversation cc WHERE cc.lessonId = :lessonId AND cc.sender = 'USER' ORDER BY cc.createdAt DESC")
    List<ChatConversation> findUserQuestionsByLesson(@Param("lessonId") Long lessonId);
}
