package com.example.ddorang.team.controller;

import com.example.ddorang.common.util.SecurityUtil;
import com.example.ddorang.common.ApiPaths;
import com.example.ddorang.team.dto.*;
import com.example.ddorang.team.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.ROOT + "/teams")
@RequiredArgsConstructor
@Slf4j
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
            @Valid @RequestBody TeamCreateRequest request) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("팀 생성 요청 - 사용자: {}, 팀명: {}", userId, request.getName());
        
        try {
            TeamResponse response = teamService.createTeam(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.error("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<TeamResponse>> getUserTeams() {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("사용자 팀 목록 조회 - 사용자: {}", userId);
        
        try {
            List<TeamResponse> teams = teamService.getUserTeams(userId);
            return ResponseEntity.ok(teams);
        } catch (IllegalStateException e) {
            log.error("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponse> getTeam(
            @PathVariable UUID teamId) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("팀 상세 조회 - 팀: {}, 사용자: {}", teamId, userId);
        
        try {
            TeamResponse response = teamService.getTeamById(teamId, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.error("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/{teamId}/invites")
    public ResponseEntity<TeamInviteResponse> createInvite(
            @PathVariable UUID teamId,
            @Valid @RequestBody CreateInviteRequest request) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("초대링크 생성 - 팀: {}, 사용자: {}", teamId, userId);
        
        try {
            TeamInviteResponse response = teamService.createInvite(teamId, userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.error("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping("/{teamId}/invites")
    public ResponseEntity<List<TeamInviteResponse>> getTeamInvites(
            @PathVariable UUID teamId) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("팀 초대링크 조회 - 팀: {}, 사용자: {}", teamId, userId);
        
        try {
            List<TeamInviteResponse> invites = teamService.getTeamInvites(teamId, userId);
            return ResponseEntity.ok(invites);
        } catch (IllegalStateException e) {
            log.error("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/join/{inviteCode}")
    public ResponseEntity<TeamResponse> joinTeam(
            @PathVariable String inviteCode) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("팀 참가 요청 - 초대코드: {}, 사용자: {}", inviteCode, userId);
        
        try {
            TeamResponse response = teamService.joinTeamByInvite(inviteCode, userId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.error("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }



    @DeleteMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID teamId,
            @PathVariable UUID memberId) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("팀 멤버 제거 - 팀: {}, 대상: {}, 요청자: {}", teamId, memberId, userId);
        
        try {
            teamService.removeMember(teamId, memberId, userId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.error("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/{teamId}/leave")
    public ResponseEntity<Void> leaveTeam(
            @PathVariable UUID teamId) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("팀 떠나기 - 팀: {}, 사용자: {}", teamId, userId);
        
        try {
            teamService.leaveTeam(teamId, userId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.error("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    @PutMapping("/{teamId}")
    public ResponseEntity<TeamResponse> updateTeam(
            @PathVariable UUID teamId,
            @Valid @RequestBody TeamUpdateRequest request) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("팀 정보 수정 요청 - 팀: {}, 사용자: {}, 새 이름: {}", teamId, userId, request.getName());
        
        try {
            TeamResponse response = teamService.updateTeam(teamId, userId, request.getName());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            log.error("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable UUID teamId) {
        
        UUID userId = SecurityUtil.getCurrentUserId();
        log.info("팀 삭제 요청 - 팀: {}, 사용자: {}", teamId, userId);
        
        try {
            teamService.deleteTeam(teamId, userId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            log.error("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }
}