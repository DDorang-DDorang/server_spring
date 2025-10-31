package com.example.ddorang.presentation.repository;

import com.example.ddorang.presentation.entity.SttResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SttResultRepository extends JpaRepository<SttResult, UUID> {
    
    // 특정 프레젠테이션의 STT 결과 조회
    @Query("SELECT sr FROM SttResult sr WHERE sr.presentation.id = :presentationId")
    Optional<SttResult> findByPresentationId(@Param("presentationId") UUID presentationId);
    
    // 사용자의 모든 STT 결과 조회
    @Query("SELECT sr FROM SttResult sr WHERE sr.presentation.topic.user.id = :userId")
    List<SttResult> findByUserId(@Param("userId") UUID userId);
    
    // STT 결과 존재 여부 확인
    @Query("SELECT COUNT(sr) > 0 FROM SttResult sr WHERE sr.presentation.id = :presentationId")
    boolean existsByPresentationId(@Param("presentationId") UUID presentationId);
    
    // 특정 토픽의 모든 STT 결과 조회
    @Query("SELECT sr FROM SttResult sr WHERE sr.presentation.topic.id = :topicId")
    List<SttResult> findByTopicId(@Param("topicId") UUID topicId);
    
    // 발음 점수 범위별 조회
    @Query("SELECT sr FROM SttResult sr WHERE sr.pronunciationScore >= :minScore AND sr.pronunciationScore <= :maxScore")
    List<SttResult> findByPronunciationScoreRange(@Param("minScore") Float minScore, @Param("maxScore") Float maxScore);
    
    // 텍스트 검색
    @Query("SELECT sr FROM SttResult sr WHERE sr.transcription LIKE %:keyword%")
    List<SttResult> findByTranscriptionContaining(@Param("keyword") String keyword);
} 