package com.example.ddorang.presentation.repository;

import com.example.ddorang.common.enums.JobStatus;
import com.example.ddorang.presentation.entity.VideoAnalysisJob;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoAnalysisJobRepository extends JpaRepository<VideoAnalysisJob, UUID> {

    // 특정 발표의 모든 작업을 최신 순으로 조회
    @Query("SELECT vaj FROM VideoAnalysisJob vaj WHERE vaj.presentation.id = :presentationId " +
           "ORDER BY vaj.createdAt DESC")
    List<VideoAnalysisJob> findByPresentationIdOrderByCreatedAtDesc(@Param("presentationId") UUID presentationId);

    // 특정 발표의 진행 중인 작업 조회 (중복 작업 방지용)
    @Query("SELECT vaj FROM VideoAnalysisJob vaj WHERE vaj.presentation.id = :presentationId " +
           "AND vaj.status IN (com.example.ddorang.common.enums.JobStatus.PENDING, com.example.ddorang.common.enums.JobStatus.PROCESSING)")
    Optional<VideoAnalysisJob> findActiveJobByPresentationId(@Param("presentationId") UUID presentationId);

   // 특정 발표의 가장 최근 완료된 작업 조회
    @Query("SELECT vaj FROM VideoAnalysisJob vaj WHERE vaj.presentation.id = :presentationId " +
           "AND vaj.status = com.example.ddorang.common.enums.JobStatus.COMPLETED ORDER BY vaj.createdAt DESC")
    Optional<VideoAnalysisJob> findLatestCompletedJobByPresentationId(@Param("presentationId") UUID presentationId);

    // 상태별 작업 조회 (생성 시간 순)
    List<VideoAnalysisJob> findByStatusOrderByCreatedAt(JobStatus status);

   // 사용자의 모든 작업 조회 (최신 순)
    @Query("SELECT vaj FROM VideoAnalysisJob vaj WHERE vaj.presentation.topic.user.userId = :userId " +
           "ORDER BY vaj.createdAt DESC")
    List<VideoAnalysisJob> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    // 장시간 처리 중인 작업 조회 (데드락 감지용)
    @Query("SELECT vaj FROM VideoAnalysisJob vaj WHERE vaj.status = com.example.ddorang.common.enums.JobStatus.PROCESSING " +
           "AND vaj.createdAt < :cutoffTime")
    List<VideoAnalysisJob> findStuckJobs(@Param("cutoffTime") LocalDateTime cutoffTime);


   // 사용자의 진행 중인 작업 수 조회
    @Query("SELECT COUNT(vaj) FROM VideoAnalysisJob vaj WHERE vaj.presentation.topic.user.userId = :userId " +
           "AND vaj.status IN (com.example.ddorang.common.enums.JobStatus.PENDING, com.example.ddorang.common.enums.JobStatus.PROCESSING)")
    long countActiveJobsByUserId(@Param("userId") UUID userId);

    // 특정 기간 이전에 생성된 완료/실패 작업 조회
    @Query("SELECT vaj FROM VideoAnalysisJob vaj WHERE vaj.createdAt < :cutoffTime " +
           "AND vaj.status IN (com.example.ddorang.common.enums.JobStatus.COMPLETED, com.example.ddorang.common.enums.JobStatus.FAILED)")
    List<VideoAnalysisJob> findOldFinishedJobs(@Param("cutoffTime") LocalDateTime cutoffTime);

    // 연관 엔티티를 미리 로딩하여 조회 (LazyInitializationException 방지)
    @EntityGraph(attributePaths = {"presentation", "presentation.topic", "presentation.topic.user"})
    @Override
    Optional<VideoAnalysisJob> findById(UUID id);

}