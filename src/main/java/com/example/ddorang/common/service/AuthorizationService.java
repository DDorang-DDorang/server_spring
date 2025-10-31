package com.example.ddorang.common.service;

import com.example.ddorang.auth.entity.User;
import com.example.ddorang.auth.repository.UserRepository;
import com.example.ddorang.common.util.SecurityUtil;
import com.example.ddorang.presentation.entity.Presentation;
import com.example.ddorang.presentation.entity.Topic;
import com.example.ddorang.presentation.repository.PresentationRepository;
import com.example.ddorang.team.entity.Team;
import com.example.ddorang.team.entity.TeamMember;
import com.example.ddorang.team.repository.TeamMemberRepository;
import com.example.ddorang.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 중앙화된 권한 검증 서비스
 * 복잡한 비즈니스 권한 로직을 한 곳에서 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final PresentationRepository presentationRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;

    /**
     * 프레젠테이션 수정 권한 확인 및 예외 발생
     * 권한이 없으면 AccessDeniedException 발생
     */
    public void requirePresentationModifyPermission(UUID presentationId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        if (!canModifyPresentation(presentationId, userId)) {
            throw new AccessDeniedException("프레젠테이션을 수정할 권한이 없습니다.");
        }
    }

    /**
     * 프레젠테이션 조회 권한 확인 및 예외 발생
     * 권한이 없으면 AccessDeniedException 발생
     */
    public void requirePresentationViewPermission(UUID presentationId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        if (!canViewPresentation(presentationId, userId)) {
            throw new AccessDeniedException("프레젠테이션을 조회할 권한이 없습니다.");
        }
    }

    /**
     * 팀 소유자 권한 확인 및 예외 발생
     * 권한이 없으면 AccessDeniedException 발생
     */
    public void requireTeamOwnerPermission(UUID teamId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        if (!isTeamOwner(teamId, userId)) {
            throw new AccessDeniedException("팀장만 수행할 수 있는 작업입니다.");
        }
    }

    /**
     * 팀 멤버 권한 확인 및 예외 발생
     * 권한이 없으면 AccessDeniedException 발생
     */
    public void requireTeamMemberPermission(UUID teamId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        if (!isTeamMember(teamId, userId)) {
            throw new AccessDeniedException("팀 멤버만 수행할 수 있는 작업입니다.");
        }
    }

    /**
     * 비디오 분석 권한 확인 및 예외 발생
     * 권한이 없으면 AccessDeniedException 발생
     */
    public void requireVideoAnalysisPermission(UUID presentationId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        if (!canAnalyzeVideo(presentationId, userId)) {
            throw new AccessDeniedException("해당 발표를 분석할 권한이 없습니다.");
        }
    }

    // ======= 권한 확인 메서드들 (boolean 반환) =======

    /**
     * 프레젠테이션 수정 권한 확인 (엄격한 권한)
     * 개인 발표: 소유자만
     * 팀 발표: 발표 작성자 또는 팀장만
     */
    public boolean canModifyPresentation(UUID presentationId, UUID userId) {
        try {
            Presentation presentation = presentationRepository.findById(presentationId)
                    .orElseThrow(() -> new RuntimeException("프레젠테이션을 찾을 수 없습니다"));
            
            Topic topic = presentation.getTopic();
            
            // 개인 발표인 경우 소유자만 수정 가능
            if (topic.getTeam() == null) {
                return topic.getUser().getUserId().equals(userId);
            }
            
            // 팀 발표인 경우 발표 작성자 또는 팀장만 수정 가능
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            // 발표 작성자인지 확인
            if (topic.getUser() != null && topic.getUser().getUserId().equals(userId)) {
                return true;
            }
            
            // 팀장인지 확인
            TeamMember member = teamMemberRepository.findByTeamAndUser(topic.getTeam(), user)
                    .orElse(null);
            
            return member != null && member.getRole() == TeamMember.Role.OWNER;
            
        } catch (Exception e) {
            log.error("프레젠테이션 수정 권한 확인 실패: presentationId={}, userId={}", 
                    presentationId, userId, e);
            return false;
        }
    }

    /**
     * 프레젠테이션 조회 권한 확인 (관대한 권한)
     * 개인 발표: 소유자만
     * 팀 발표: 팀 멤버 모두
     */
    public boolean canViewPresentation(UUID presentationId, UUID userId) {
        try {
            Presentation presentation = presentationRepository.findById(presentationId)
                    .orElseThrow(() -> new RuntimeException("프레젠테이션을 찾을 수 없습니다"));
            
            Topic topic = presentation.getTopic();
            
            // 개인 발표인 경우 소유자만 조회 가능
            if (topic.getTeam() == null) {
                return topic.getUser().getUserId().equals(userId);
            }
            
            // 팀 발표인 경우 팀 멤버 확인
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            return teamMemberRepository.existsByTeamAndUser(topic.getTeam(), user);
            
        } catch (Exception e) {
            log.error("프레젠테이션 조회 권한 확인 실패: presentationId={}, userId={}", 
                    presentationId, userId, e);
            return false;
        }
    }

    /**
     * 비디오 분석 권한 확인
     * 프레젠테이션 조회 권한과 동일
     */
    public boolean canAnalyzeVideo(UUID presentationId, UUID userId) {
        return canViewPresentation(presentationId, userId);
    }

    /**
     * 팀 소유자 권한 확인
     */
    public boolean isTeamOwner(UUID teamId, UUID userId) {
        try {
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new RuntimeException("팀을 찾을 수 없습니다"));
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            TeamMember member = teamMemberRepository.findByTeamAndUser(team, user)
                    .orElse(null);
            
            return member != null && member.getRole() == TeamMember.Role.OWNER;
            
        } catch (Exception e) {
            log.error("팀 소유자 권한 확인 실패: teamId={}, userId={}", teamId, userId, e);
            return false;
        }
    }

    /**
     * 팀 멤버 권한 확인
     */
    public boolean isTeamMember(UUID teamId, UUID userId) {
        try {
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new RuntimeException("팀을 찾을 수 없습니다"));
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            return teamMemberRepository.existsByTeamAndUser(team, user);
            
        } catch (Exception e) {
            log.error("팀 멤버 권한 확인 실패: teamId={}, userId={}", teamId, userId, e);
            return false;
        }
    }

    /**
     * 댓글 수정/삭제 권한 확인 - 댓글 작성자만
     */
    public void requireCommentOwnerPermission(UUID commentUserId) {
        UUID currentUserId = SecurityUtil.getCurrentUserId();
        if (!currentUserId.equals(commentUserId)) {
            throw new AccessDeniedException("댓글 작성자만 수정/삭제할 수 있습니다.");
        }
    }
}