package com.example.ddorang.common.repository;

import com.example.ddorang.common.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // 사용자별 알림 조회 (최신순)
    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // 사용자의 읽지 않은 알림 조회
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);

    // 사용자의 읽지 않은 알림 개수
    long countByUserIdAndIsReadFalse(UUID userId);

    // 특정 사용자의 모든 알림을 읽음 처리
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId")
    void markAllAsReadByUserId(@Param("userId") UUID userId);

    // 특정 알림을 읽음 처리
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.notificationId = :notificationId AND n.userId = :userId")
    void markAsReadByIdAndUserId(@Param("notificationId") UUID notificationId, @Param("userId") UUID userId);

    // 사용자의 알림 개수 조회
    long countByUserId(UUID userId);

    // 사용자의 오래된 알림 삭제 (15개 초과시)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId AND n.notificationId IN " +
           "(SELECT sub.notificationId FROM Notification sub WHERE sub.userId = :userId " +
           "ORDER BY sub.createdAt ASC LIMIT :deleteCount)")
    void deleteOldestNotifications(@Param("userId") UUID userId, @Param("deleteCount") int deleteCount);
}