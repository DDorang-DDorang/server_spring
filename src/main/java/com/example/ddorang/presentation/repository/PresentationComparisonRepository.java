package com.example.ddorang.presentation.repository;

import com.example.ddorang.presentation.entity.Presentation;
import com.example.ddorang.presentation.entity.PresentationComparison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PresentationComparisonRepository extends JpaRepository<PresentationComparison, UUID> {
    
    /**
     * 특정 사용자의 모든 비교 기록 조회
     */
    List<PresentationComparison> findByUserUserIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * 두 발표 간의 기존 비교 기록이 있는지 확인 (순서 무관)
     * presentation1=A, presentation2=B 또는 presentation1=B, presentation2=A 모두 찾기
     */
    @Query("SELECT pc FROM PresentationComparison pc WHERE pc.user.userId = :userId AND " +
           "((pc.presentation1.id = :presentationId1 AND pc.presentation2.id = :presentationId2) OR " +
           "(pc.presentation1.id = :presentationId2 AND pc.presentation2.id = :presentationId1))")
    Optional<PresentationComparison> findExistingComparison(
            @Param("userId") UUID userId,
            @Param("presentationId1") UUID presentationId1,
            @Param("presentationId2") UUID presentationId2
    );
    
    /**
     * 특정 발표가 포함된 모든 비교 기록 조회
     */
    @Query("SELECT pc FROM PresentationComparison pc WHERE " +
           "(pc.presentation1.id = :presentationId OR pc.presentation2.id = :presentationId) " +
           "AND pc.user.userId = :userId")
    List<PresentationComparison> findComparisonsInvolving(
            @Param("userId") UUID userId, 
            @Param("presentationId") UUID presentationId
    );
    
    /**
     * 특정 발표가 포함된 모든 비교 기록 삭제
     */
    @Modifying
    @Query("DELETE FROM PresentationComparison pc WHERE " +
           "pc.presentation1 = :presentation OR pc.presentation2 = :presentation")
    void deleteByPresentation1OrPresentation2(@Param("presentation") Presentation presentation);
}