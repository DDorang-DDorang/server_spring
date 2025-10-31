package com.example.ddorang.presentation.repository;

import com.example.ddorang.presentation.entity.VoiceAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoiceAnalysisRepository extends JpaRepository<VoiceAnalysis, UUID> {
    
    // 특정 프레젠테이션의 음성 분석 결과 조회
    @Query("SELECT va FROM VoiceAnalysis va WHERE va.presentation.id = :presentationId")
    Optional<VoiceAnalysis> findByPresentationId(@Param("presentationId") UUID presentationId);
    
    // 사용자의 모든 음성 분석 결과 조회
    @Query("SELECT va FROM VoiceAnalysis va WHERE va.presentation.topic.user.id = :userId")
    List<VoiceAnalysis> findByUserId(@Param("userId") UUID userId);
    
    // 프레젠테이션 분석 결과 존재 여부 확인
    @Query("SELECT COUNT(va) > 0 FROM VoiceAnalysis va WHERE va.presentation.id = :presentationId")
    boolean existsByPresentationId(@Param("presentationId") UUID presentationId);
    
    // 특정 토픽의 모든 음성 분석 결과 조회
    @Query("SELECT va FROM VoiceAnalysis va WHERE va.presentation.topic.id = :topicId")
    List<VoiceAnalysis> findByTopicId(@Param("topicId") UUID topicId);
    
    // 음성 강도 등급별 조회
    @Query("SELECT va FROM VoiceAnalysis va WHERE va.intensityGrade = :grade")
    List<VoiceAnalysis> findByIntensityGrade(@Param("grade") String grade);
    
    // 피치 등급별 조회
    @Query("SELECT va FROM VoiceAnalysis va WHERE va.pitchGrade = :grade")
    List<VoiceAnalysis> findByPitchGrade(@Param("grade") String grade);
    
    // WPM 등급별 조회
    @Query("SELECT va FROM VoiceAnalysis va WHERE va.wpmGrade = :grade")
    List<VoiceAnalysis> findByWpmGrade(@Param("grade") String grade);
} 