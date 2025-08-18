package com.example.ddorang.presentation.repository;

import com.example.ddorang.presentation.entity.PresentationFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PresentationFeedbackRepository extends JpaRepository<PresentationFeedback, UUID> {
    
    // 특정 프레젠테이션의 피드백 조회
    @Query("SELECT pf FROM PresentationFeedback pf WHERE pf.presentation.id = :presentationId")
    Optional<PresentationFeedback> findByPresentationId(@Param("presentationId") UUID presentationId);
    
    // 사용자의 모든 피드백 조회
    @Query("SELECT pf FROM PresentationFeedback pf WHERE pf.presentation.topic.user.id = :userId")
    List<PresentationFeedback> findByUserId(@Param("userId") UUID userId);
    
    // 프레젠테이션 피드백 존재 여부 확인
    @Query("SELECT COUNT(pf) > 0 FROM PresentationFeedback pf WHERE pf.presentation.id = :presentationId")
    boolean existsByPresentationId(@Param("presentationId") UUID presentationId);
    
    // 특정 토픽의 모든 피드백 조회
    @Query("SELECT pf FROM PresentationFeedback pf WHERE pf.presentation.topic.id = :topicId")
    List<PresentationFeedback> findByTopicId(@Param("topicId") UUID topicId);
} 