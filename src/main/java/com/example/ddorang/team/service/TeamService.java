package com.example.ddorang.team.service;

import com.example.ddorang.auth.entity.User;
import com.example.ddorang.auth.repository.UserRepository;
import com.example.ddorang.team.dto.*;
import com.example.ddorang.team.entity.Team;
import com.example.ddorang.team.entity.TeamMember;
import com.example.ddorang.team.repository.TeamMemberRepository;
import com.example.ddorang.team.repository.TeamRepository;
import com.example.ddorang.presentation.entity.Topic;
import com.example.ddorang.presentation.entity.Presentation;
import com.example.ddorang.presentation.repository.TopicRepository;
import com.example.ddorang.presentation.repository.PresentationRepository;
import com.example.ddorang.presentation.repository.CommentRepository;
import com.example.ddorang.presentation.repository.VoiceAnalysisRepository;
import com.example.ddorang.presentation.repository.SttResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final InviteService inviteService;
    private final TopicRepository topicRepository;
    private final PresentationRepository presentationRepository;
    private final CommentRepository commentRepository;
    private final VoiceAnalysisRepository voiceAnalysisRepository;
    private final SttResultRepository sttResultRepository;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    

    public TeamResponse createTeam(UUID userId, TeamCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        Team team = Team.builder()
                .name(request.getName())
                .createdAt(LocalDateTime.now())
                .build();
        
        team = teamRepository.save(team);

        TeamMember owner = TeamMember.builder()
                .team(team)
                .user(user)
                .role(TeamMember.Role.OWNER)
                .joinedAt(LocalDateTime.now())
                .build();
        
        teamMemberRepository.save(owner);

        return TeamResponse.from(team, TeamMember.Role.OWNER);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getUserTeams(UUID userId) {
        List<Team> teams = teamRepository.findTeamsByUserId(userId);
        
        return teams.stream()
                .map(team -> {
                    TeamMember userMember = team.getMembers().stream()
                            .filter(member -> member.getUser().getUserId().equals(userId))
                            .findFirst()
                            .orElse(null);
                    return TeamResponse.from(team, userMember != null ? userMember.getRole() : null);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamById(UUID teamId, UUID userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        TeamMember member = teamMemberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new SecurityException("팀 멤버가 아닙니다"));

        List<TeamMemberResponse> members = teamMemberRepository.findByTeamOrderByJoinedAtAsc(team)
                .stream()
                .map(TeamMemberResponse::from)
                .toList();

        return TeamResponse.fromWithMembers(team, members, member.getRole());
    }

    public TeamInviteResponse createInvite(UUID teamId, UUID userId, CreateInviteRequest request) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        TeamMember member = teamMemberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new SecurityException("팀 멤버가 아닙니다"));

        if (member.getRole() != TeamMember.Role.OWNER) {
            throw new SecurityException("팀장만 초대링크를 생성할 수 있습니다");
        }

        String inviteCode = inviteService.createInvite(teamId);
        
        return TeamInviteResponse.builder()
                .inviteCode(inviteCode)
                .inviteUrl(baseUrl + "/api/teams/join/" + inviteCode)
                .teamName(team.getName())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
    }

    public TeamResponse joinTeamByInvite(String inviteCode, UUID userId) {
        UUID teamId = inviteService.getTeamIdByInviteCode(inviteCode);
        
        if (teamId == null) {
            throw new IllegalArgumentException("유효하지 않은 초대 코드입니다");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        if (teamMemberRepository.existsByTeamAndUser(team, user)) {
            throw new IllegalStateException("이미 팀의 멤버입니다");
        }

        TeamMember member = TeamMember.builder()
                .team(team)
                .user(user)
                .role(TeamMember.Role.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();

        teamMemberRepository.save(member);

        return TeamResponse.from(team, TeamMember.Role.MEMBER);
    }

    public void removeMember(UUID teamId, UUID targetUserId, UUID requestUserId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다"));

        User requestUser = userRepository.findById(requestUserId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다"));

        TeamMember requestMember = teamMemberRepository.findByTeamAndUser(team, requestUser)
                .orElseThrow(() -> new SecurityException("팀 멤버가 아닙니다"));

        TeamMember targetMember = teamMemberRepository.findByTeamAndUser(team, targetUser)
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자가 팀 멤버가 아닙니다"));

        if (requestMember.getRole() != TeamMember.Role.OWNER) {
            throw new SecurityException("팀장만 멤버를 제거할 수 있습니다");
        }

        if (targetMember.getRole() == TeamMember.Role.OWNER) {
            throw new SecurityException("소유자는 제거할 수 없습니다");
        }

        teamMemberRepository.delete(targetMember);
    }

    public void leaveTeam(UUID teamId, UUID userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        TeamMember member = teamMemberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new SecurityException("팀 멤버가 아닙니다"));

        if (member.getRole() == TeamMember.Role.OWNER) {
            long memberCount = teamMemberRepository.countByTeam(team);
            if (memberCount > 1) {
                throw new IllegalStateException("팀에 다른 멤버가 있을 때 소유자는 팀을 떠날 수 없습니다");
            } else {
                // 팀장이 혼자 남았을 때는 팀을 자동 삭제
                log.info("팀장이 혼자 남아 팀을 떠나므로 팀을 자동 삭제합니다 - 팀: {}, 사용자: {}", teamId, userId);
                deleteTeam(teamId, userId);
                return;
            }
        }

        teamMemberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public List<TeamInviteResponse> getTeamInvites(UUID teamId, UUID userId) {
        // Redis 기반 초대 시스템에서는 활성 초대 링크 목록 조회 기능 제거
        // 필요 시 별도의 관리 인터페이스로 구현 가능
        throw new UnsupportedOperationException("초대 링크 목록 조회는 지원되지 않습니다");
    }

    public TeamResponse updateTeam(UUID teamId, UUID userId, String newName) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        TeamMember member = teamMemberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new SecurityException("팀 멤버가 아닙니다"));

        if (member.getRole() != TeamMember.Role.OWNER) {
            throw new SecurityException("팀장만 팀 정보를 수정할 수 있습니다");
        }

        team.setName(newName);
        team = teamRepository.save(team);

        return TeamResponse.from(team, TeamMember.Role.OWNER);
    }

    public void deleteTeam(UUID teamId, UUID userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        TeamMember member = teamMemberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new SecurityException("팀 멤버가 아닙니다"));

        if (member.getRole() != TeamMember.Role.OWNER) {
            throw new SecurityException("팀 소유자만 팀을 삭제할 수 있습니다");
        }

        // 1. 팀의 모든 토픽 조회
        List<Topic> teamTopics = topicRepository.findByTeamIdOrderByTitle(team.getId());
        
        // 2. 각 토픽의 프레젠테이션들과 관련 데이터 삭제
        for (Topic topic : teamTopics) {
            List<Presentation> presentations = presentationRepository.findByTopicId(topic.getId());
            
            for (Presentation presentation : presentations) {
                // 2-1. AI 분석 결과 삭제
                voiceAnalysisRepository.findByPresentationId(presentation.getId())
                    .ifPresent(voiceAnalysisRepository::delete);
                sttResultRepository.findByPresentationId(presentation.getId())
                    .ifPresent(sttResultRepository::delete);
                
                // 2-2. 댓글 삭제 (CASCADE DELETE로 자동 삭제)
                // commentRepository는 Presentation 삭제 시 자동으로 삭제
                
                log.info("프레젠테이션 관련 데이터 삭제: {}", presentation.getId());
            }
            
            // 2-3. 프레젠테이션 삭제
            presentationRepository.deleteAll(presentations);
        }
        
        // 3. 토픽 삭제
        topicRepository.deleteAll(teamTopics);
        
        // 4. 팀 멤버 삭제
        teamMemberRepository.deleteAll(teamMemberRepository.findByTeamOrderByJoinedAtAsc(team));
        
        // 5. 팀 삭제
        teamRepository.delete(team);
        
        log.info("팀 및 관련 데이터 삭제 완료 - 팀: {}, 토픽: {}개, 삭제자: {}", 
                team.getName(), teamTopics.size(), userId);
    }

}