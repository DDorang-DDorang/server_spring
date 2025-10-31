package com.example.ddorang.presentation.repository;

import com.example.ddorang.presentation.entity.Presentation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PresentationRepository extends JpaRepository<Presentation, UUID> {
    
    // 특정 토픽의 프레젠테이션 목록 조회
    @Query("SELECT p FROM Presentation p WHERE p.topic.id = :topicId ORDER BY p.createdAt DESC")
    List<Presentation> findByTopicId(@Param("topicId") UUID topicId);
    
    // 사용자의 모든 프레젠테이션 조회 (토픽을 통해)
    @Query("SELECT p FROM Presentation p JOIN p.topic t WHERE t.user.userId = :userId ORDER BY p.createdAt DESC")
    List<Presentation> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
    
    // 특정 토픽의 특정 프레젠테이션 조회
    @Query("SELECT p FROM Presentation p WHERE p.id = :presentationId AND p.topic.id = :topicId")
    Optional<Presentation> findByIdAndTopicId(@Param("presentationId") UUID presentationId, @Param("topicId") UUID topicId);
    
    // 토픽별 프레젠테이션 개수 조회
    @Query("SELECT COUNT(p) FROM Presentation p WHERE p.topic.id = :topicId")
    long countByTopicId(@Param("topicId") UUID topicId);
    
    // 사용자별 프레젠테이션 개수 조회
    @Query("SELECT COUNT(p) FROM Presentation p JOIN p.topic t WHERE t.user.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);
    
    // 프레젠테이션 제목으로 검색 (특정 토픽 내)
    @Query("SELECT p FROM Presentation p WHERE p.topic.id = :topicId AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.createdAt DESC")
    List<Presentation> searchPresentationsByKeyword(@Param("topicId") UUID topicId, @Param("keyword") String keyword);
    
    // 사용자의 모든 프레젠테이션에서 검색
    @Query("SELECT p FROM Presentation p JOIN p.topic t WHERE t.user.userId = :userId AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.createdAt DESC")
    List<Presentation> searchUserPresentationsByKeyword(@Param("userId") UUID userId, @Param("keyword") String keyword);
    
    // 팀의 모든 프레젠테이션 조회
    @Query("SELECT p FROM Presentation p JOIN p.topic t WHERE t.team.id = :teamId ORDER BY p.createdAt DESC")
    List<Presentation> findByTeamIdOrderByCreatedAtDesc(@Param("teamId") UUID teamId);
    
    // 팀의 프레젠테이션에서 검색
    @Query("SELECT p FROM Presentation p JOIN p.topic t WHERE t.team.id = :teamId AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.createdAt DESC")
    List<Presentation> searchTeamPresentationsByKeyword(@Param("teamId") UUID teamId, @Param("keyword") String keyword);
} 