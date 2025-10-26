package com.example.ddorang.common.service;

import com.example.ddorang.auth.entity.User;
import com.example.ddorang.auth.repository.UserRepository;
import com.example.ddorang.common.entity.Notification;
import com.example.ddorang.common.repository.NotificationRepository;
import com.example.ddorang.presentation.event.AnalysisCompleteEvent;
import com.example.ddorang.team.entity.Team;
import com.example.ddorang.team.entity.TeamMember;
import com.example.ddorang.team.repository.TeamMemberRepository;
import com.example.ddorang.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 팀 발표에 댓글이 달렸을 때 팀원들에게 알림 발송
     * @param teamId 팀 ID
     * @param commenterId 댓글 작성자 ID
     * @param commenterName 댓글 작성자 이름
     * @param presentationTitle 발표 제목
     * @param commentId 댓글 ID
     */
    public void sendCommentNotification(UUID teamId, UUID commenterId, String commenterName, String presentationTitle, UUID commentId) {
        try {
            // 댓글 작성자가 팀원인지 먼저 확인
            User commenter = userRepository.findById(commenterId)
                    .orElseThrow(() -> new RuntimeException("댓글 작성자를 찾을 수 없습니다: " + commenterId));
            
            // 팀 조회
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new RuntimeException("팀을 찾을 수 없습니다: " + teamId));
            
            // 댓글 작성자가 팀원인지 확인
            boolean isTeamMember = teamMemberRepository.existsByTeamAndUser(team, commenter);
            if (!isTeamMember) {
                log.warn("팀원이 아닌 사용자의 댓글에 대해 알림 발송 요청이 있었습니다 - 팀ID: {}, 사용자ID: {}", teamId, commenterId);
                return; // 팀원이 아니면 알림 발송 중단
            }
            
            // 팀원들 조회
            List<TeamMember> teamMembers = teamMemberRepository.findByTeamOrderByJoinedAtAsc(team);
            
            String title = "새 댓글이 달렸습니다";
            String message = String.format("%s님이 '%s' 발표에 댓글을 남겼습니다.", commenterName, presentationTitle);
            
            // 각 팀원에게 알림 생성 (자기 자신 제외, 알림 설정 확인)
            for (TeamMember member : teamMembers) {
                User memberUser = member.getUser();
                
                // 자기 자신에게는 알림 발송하지 않음
                if (memberUser.getUserId().equals(commenterId)) {
                    continue;
                }
                
                // 알림 설정이 꺼져있으면 알림 발송하지 않음
                if (!memberUser.getNotificationEnabled()) {
                    log.info("알림 설정이 꺼진 사용자에게 알림 발송 건너뜀 - 사용자: {}", memberUser.getEmail());
                    continue;
                }
                
                Notification notification = Notification.builder()
                        .userId(memberUser.getUserId())
                        .type(Notification.NotificationType.COMMENT)
                        .title(title)
                        .message(message)
                        .relatedId(commentId)
                        .build();
                
                Notification savedNotification = notificationRepository.save(notification);
                log.info("댓글 알림 발송 - 수신자: {}, 발표: {}", memberUser.getEmail(), presentationTitle);
                
                // 실시간 알림 발송
                sendRealtimeNotification(memberUser.getUserId(), savedNotification);
                
                // 사용자별 알림 15개 제한 처리
                cleanupUserNotifications(memberUser.getUserId());
            }
        } catch (Exception e) {
            log.error("댓글 알림 발송 실패 - 팀ID: {}, 댓글작성자ID: {}, 댓글ID: {}", teamId, commenterId, commentId, e);
        }
    }

    /**
     * AI 분석이 완료되었을 때 발표자에게 알림 발송
     * @param userId 발표자 ID
     * @param presentationTitle 발표 제목
     * @param presentationId 발표 ID
     */
    public void sendAnalysisCompleteNotification(UUID userId, String presentationTitle, UUID presentationId) {
        try {
            // 사용자의 알림 설정 확인
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
            
            if (!user.getNotificationEnabled()) {
                log.info("알림 설정이 꺼진 사용자에게 AI 분석 완료 알림 발송 건너뜀 - 사용자ID: {}", userId);
                return;
            }
            
            String title = "AI 분석이 완료되었습니다";
            String message = String.format("'%s' 발표의 AI 분석이 완료되었습니다. 결과를 확인해보세요!", presentationTitle);
            
            Notification notification = Notification.builder()
                    .userId(userId)
                    .type(Notification.NotificationType.AI_ANALYSIS_COMPLETE)
                    .title(title)
                    .message(message)
                    .relatedId(presentationId)
                    .build();
            
            Notification savedNotification = notificationRepository.save(notification);
            log.info("AI 분석 완료 알림 발송 - 수신자 ID: {}, 발표: {}", userId, presentationTitle);
            
            // 실시간 알림 발송
            sendRealtimeNotification(userId, savedNotification);
            
            // 사용자별 알림 15개 제한 처리
            cleanupUserNotifications(userId);
        } catch (Exception e) {
            log.error("AI 분석 완료 알림 발송 실패 - 사용자ID: {}, 발표ID: {}", userId, presentationId, e);
        }
    }

    /**
     * 사용자의 알림을 읽음 처리
     * @param notificationId 알림 ID
     * @param userId 사용자 ID
     */
    public void markAsRead(UUID notificationId, UUID userId) {
        notificationRepository.markAsReadByIdAndUserId(notificationId, userId);
        log.info("알림 읽음 처리 - 알림ID: {}, 사용자ID: {}", notificationId, userId);
    }

    /**
     * 사용자의 모든 알림을 읽음 처리
     * @param userId 사용자 ID
     */
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        log.info("모든 알림 읽음 처리 - 사용자ID: {}", userId);
    }

    /**
     * 사용자별 알림 15개 제한 처리 (오래된 알림 삭제)
     * @param userId 사용자 ID
     */
    @Transactional
    protected void cleanupUserNotifications(UUID userId) {
        try {
            long notificationCount = notificationRepository.countByUserId(userId);
            
            if (notificationCount > 15) {
                int deleteCount = (int)(notificationCount - 15);
                notificationRepository.deleteOldestNotifications(userId, deleteCount);
                log.info("알림 정리 완료 - 사용자ID: {}, 삭제된 알림: {}개", userId, deleteCount);
            }
        } catch (Exception e) {
            log.error("알림 정리 중 오류 발생 - 사용자ID: {}", userId, e);
        }
    }

    /**
     * 실시간 알림 발솠
     * @param userId 사용자 ID
     * @param notification 알림 객체
     */
    private void sendRealtimeNotification(UUID userId, Notification notification) {
        try {
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("id", notification.getNotificationId());
            notificationData.put("type", notification.getType().name());
            notificationData.put("title", notification.getTitle());
            notificationData.put("message", notification.getMessage());
            notificationData.put("relatedId", notification.getRelatedId());
            notificationData.put("isRead", notification.getIsRead());
            notificationData.put("createdAt", notification.getCreatedAt());

            // 특정 사용자에게만 알림 발송
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notificationData
            );

            log.info("실시간 알림 발송 완료 - 사용자ID: {}, 알림타입: {}", userId, notification.getType());
        } catch (Exception e) {
            log.error("실시간 알림 발송 실패 - 사용자ID: {}, 알림ID: {}", userId, notification.getNotificationId(), e);
        }
    }

    /**
     * 영상 분석 완료 이벤트 리스너
     * 트랜잭션 커밋 후 비동기로 알림 발송
     *
     * @param event 분석 완료 이벤트
     */
    @Async
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAnalysisCompleteEvent(AnalysisCompleteEvent event) {
        try {
            log.info("영상 분석 완료 이벤트 수신 - jobId: {}, 성공여부: {}", event.getJobId(), event.isSuccess());

            // 기존 알림 발송 메서드 호출
            sendAnalysisCompleteNotification(
                event.getUserId(),
                event.getPresentationTitle(),
                event.getPresentationId()
            );

            log.info("영상 분석 알림 발송 완료 - jobId: {}", event.getJobId());

        } catch (Exception e) {
            // 알림 실패해도 DB는 이미 저장됨 (트랜잭션 커밋 후 실행)
            log.error("영상 분석 알림 발송 실패 (DB는 이미 저장됨) - jobId: {}", event.getJobId(), e);
        }
    }
}