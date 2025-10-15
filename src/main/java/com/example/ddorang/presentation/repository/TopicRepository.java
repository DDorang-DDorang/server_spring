package com.example.ddorang.presentation.repository;

import com.example.ddorang.presentation.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {
    
    // 사용자의 모든 토픽 조회 (제목순 정렬)
    @Query("SELECT t FROM Topic t WHERE t.user.userId = :userId ORDER BY t.title")
    List<Topic> findByUserIdOrderByTitle(@Param("userId") UUID userId);
    
    // 팀의 모든 토픽 조회 (제목순 정렬)
    @Query("SELECT t FROM Topic t WHERE t.team.id = :teamId ORDER BY t.title")
    List<Topic> findByTeamIdOrderByTitle(@Param("teamId") UUID teamId);
    
    // 개인 토픽만 조회 (팀이 없는 토픽)
    @Query("SELECT t FROM Topic t WHERE t.user.userId = :userId AND t.team IS NULL ORDER BY t.title")
    List<Topic> findByUserIdAndTeamIsNullOrderByTitle(@Param("userId") UUID userId);
    
    // 팀 토픽만 조회 (사용자가 속한 팀들의 모든 토픽)
    @Query("SELECT DISTINCT t FROM Topic t " +
           "JOIN t.team team " +
           "JOIN team.members tm " +
           "WHERE tm.user.userId = :userId " +
           "ORDER BY t.title")
    List<Topic> findTeamTopicsByUserId(@Param("userId") UUID userId);
    
    // 토픽 제목으로 검색 (사용자별)
    @Query("SELECT t FROM Topic t WHERE t.user.userId = :userId AND LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY t.title")
    List<Topic> searchTopicsByKeyword(@Param("userId") UUID userId, @Param("keyword") String keyword);
    
    // 사용자별 토픽 개수 조회
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.user.userId = :userId")
    long countByUserId(@Param("userId") UUID userId);
    
    // 팀별 토픽 개수 조회
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.team.id = :teamId")
    long countByTeamId(@Param("teamId") UUID teamId);
} 