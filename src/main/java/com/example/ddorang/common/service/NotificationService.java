package com.example.ddorang.common.service;

import com.example.ddorang.common.entity.Notification;
import com.example.ddorang.common.repository.NotificationRepository;
import com.example.ddorang.team.entity.Team;
import com.example.ddorang.team.entity.TeamMember;
import com.example.ddorang.team.repository.TeamMemberRepository;
import com.example.ddorang.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;

    /**
     * 팀 발표에 댓글이 달렸을 때 팀원들에게 알림 발송
     * @param teamId 팀 ID
     * @param commenterName 댓글 작성자 이름
     * @param presentationTitle 발표 제목
     * @param commentId 댓글 ID
     */
    public void sendCommentNotification(UUID teamId, String commenterName, String presentationTitle, UUID commentId) {
        try {
            // 팀 조회
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new RuntimeException("팀을 찾을 수 없습니다: " + teamId));
            
            // 팀원들 조회
            List<TeamMember> teamMembers = teamMemberRepository.findByTeamOrderByJoinedAtAsc(team);
            
            String title = "새 댓글이 달렸습니다";
            String message = String.format("%s님이 '%s' 발표에 댓글을 남겼습니다.", commenterName, presentationTitle);
            
            // 각 팀원에게 알림 생성
            for (TeamMember member : teamMembers) {
                Notification notification = Notification.builder()
                        .userId(member.getUser().getUserId())
                        .type(Notification.NotificationType.COMMENT)
                        .title(title)
                        .message(message)
                        .relatedId(commentId)
                        .build();
                
                notificationRepository.save(notification);
                log.info("댓글 알림 발송 - 수신자: {}, 발표: {}", member.getUser().getEmail(), presentationTitle);
            }
        } catch (Exception e) {
            log.error("댓글 알림 발송 실패 - 팀ID: {}, 댓글ID: {}", teamId, commentId, e);
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
            String title = "AI 분석이 완료되었습니다";
            String message = String.format("'%s' 발표의 AI 분석이 완료되었습니다. 결과를 확인해보세요!", presentationTitle);
            
            Notification notification = Notification.builder()
                    .userId(userId)
                    .type(Notification.NotificationType.AI_ANALYSIS_COMPLETE)
                    .title(title)
                    .message(message)
                    .relatedId(presentationId)
                    .build();
            
            notificationRepository.save(notification);
            log.info("AI 분석 완료 알림 발송 - 수신자 ID: {}, 발표: {}", userId, presentationTitle);
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
}