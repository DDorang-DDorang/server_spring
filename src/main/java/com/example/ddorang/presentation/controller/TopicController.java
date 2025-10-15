package com.example.ddorang.presentation.controller;

import com.example.ddorang.common.ApiPaths;
import com.example.ddorang.presentation.entity.Topic;
import com.example.ddorang.presentation.entity.Presentation;
import com.example.ddorang.presentation.service.TopicService;
import com.example.ddorang.presentation.service.PresentationService;
import com.example.ddorang.presentation.dto.TopicResponse;
import com.example.ddorang.presentation.dto.CreateTopicRequest;
import com.example.ddorang.presentation.dto.UpdateTopicRequest;
import com.example.ddorang.presentation.dto.PresentationResponse;
import com.example.ddorang.auth.entity.User;
import com.example.ddorang.auth.service.AuthService;
import com.example.ddorang.team.entity.Team;
import com.example.ddorang.team.service.TeamService;
import com.example.ddorang.team.dto.TeamResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping(ApiPaths.ROOT)
@RequiredArgsConstructor
@Slf4j
public class TopicController {
    
    private final TopicService topicService;
    private final PresentationService presentationService;
    private final AuthService authService;
    private final TeamService teamService;
    
    // 사용자의 모든 토픽 조회
    @GetMapping("/topics")
    public ResponseEntity<List<TopicResponse>> getTopics(@RequestParam String userId) {
        log.info("토픽 목록 조회 요청 - 사용자: {}", userId);
        
        try {
            User user = getUserByIdentifier(userId);
            UUID userUuid = user.getUserId();
            
            List<Topic> privateTopics = topicService.getPrivateTopicsByUserId(userUuid);
            List<Topic> teamTopics = topicService.getTeamTopicsByUserId(userUuid);
            
            List<TopicResponse> response = new java.util.ArrayList<>();
            
            // 개인 토픽 추가
            privateTopics.forEach(topic -> {
                long presentationCount = presentationService.getPresentationsByTopicId(topic.getId()).size();
                response.add(TopicResponse.from(topic, presentationCount, false));
            });
            
            // 팀 토픽을 팀별로 그룹화하여 추가
            // 팀 ID별로 토픽을 맵에 저장
            Map<UUID, List<Topic>> teamTopicMap = new HashMap<>();
            teamTopics.forEach(topic -> {
                if (topic.getTeam() != null) {
                    teamTopicMap.computeIfAbsent(topic.getTeam().getId(), k -> new ArrayList<>()).add(topic);
                }
            });
            
            // 각 팀의 토픽들을 순서대로 추가 (팀별로 그룹화)
            teamTopicMap.values().forEach(teamTopicList -> {
                // 각 팀 내에서 토픽을 제목순으로 정렬
                teamTopicList.sort((t1, t2) -> t1.getTitle().compareTo(t2.getTitle()));
                
                teamTopicList.forEach(topic -> {
                    long presentationCount = presentationService.getPresentationsByTopicId(topic.getId()).size();
                    response.add(TopicResponse.from(topic, presentationCount, true));
                });
            });
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("토픽 목록 조회 실패 - 사용자: {}, 오류: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 특정 토픽 조회
    @GetMapping("/topics/{topicId}")
    public ResponseEntity<TopicResponse> getTopic(@PathVariable UUID topicId) {
        log.info("토픽 조회 요청 - ID: {}", topicId);
        
        Topic topic = topicService.getTopicById(topicId);
        long presentationCount = presentationService.getPresentationsByTopicId(topic.getId()).size();
        boolean isTeamTopic = topic.getTeam() != null;
        
        TopicResponse response = TopicResponse.from(topic, presentationCount, isTeamTopic);
        return ResponseEntity.ok(response);
    }
    
    // 특정 토픽의 프레젠테이션 목록 조회
    @GetMapping("/topics/{topicId}/presentations")
    public ResponseEntity<List<PresentationResponse>> getPresentations(@PathVariable UUID topicId) {
        log.info("프레젠테이션 목록 조회 요청 - 토픽: {}", topicId);
        
        List<Presentation> presentations = presentationService.getPresentationsByTopicId(topicId);
        List<PresentationResponse> response = presentations.stream()
                .map(PresentationResponse::from)
                .toList();
        
        return ResponseEntity.ok(response);
    }
    
    // 새 토픽 생성 (개인 또는 팀)
    @PostMapping("/topics")
    public ResponseEntity<TopicResponse> createTopic(@RequestBody CreateTopicRequest request) {
        log.info("토픽 생성 요청 - 제목: {}, 사용자: {}, 팀: {}", 
                request.getTitle(), request.getUserId(), request.getTeamId());
        
        try {
            User user = getUserByIdentifier(request.getUserId());
            
            Team team = null;
            boolean isTeamTopic = false;
            
            // 팀 토픽인 경우
            if (request.getTeamId() != null) {
                team = validateTeamAccess(request.getTeamId(), user.getUserId());
                isTeamTopic = true;
                log.info("팀 토픽 생성 - 팀: {}, 사용자: {}", team.getName(), user.getName());
            }
            
            // 토픽 생성 (개인 또는 팀)
            Topic topic = topicService.createTopic(request.getTitle(), user, team);
            
            // 응답 생성
            TopicResponse response = TopicResponse.from(topic, 0L, isTeamTopic);
            
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.error("토픽 생성 실패 - 권한 없음: {}", e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (IllegalArgumentException e) {
            log.error("토픽 생성 실패 - 잘못된 인수: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("토픽 생성 실패 - 사용자: {}, 오류: {}", request.getUserId(), e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // 토픽 수정
    @PutMapping("/topics/{topicId}")
    public ResponseEntity<TopicResponse> updateTopic(
            @PathVariable UUID topicId, 
            @RequestBody UpdateTopicRequest request) {
        log.info("토픽 수정 요청 - ID: {}, 새 제목: {}", topicId, request.getTitle());
        
        Topic topic = topicService.updateTopic(topicId, request.getTitle());
        long presentationCount = presentationService.getPresentationsByTopicId(topic.getId()).size();
        boolean isTeamTopic = topic.getTeam() != null;
        
        TopicResponse response = TopicResponse.from(topic, presentationCount, isTeamTopic);
        return ResponseEntity.ok(response);
    }
    
    // 토픽 삭제
    @DeleteMapping("/topics/{topicId}")
    public ResponseEntity<Void> deleteTopic(@PathVariable UUID topicId) {
        log.info("토픽 삭제 요청 - ID: {}", topicId);
        
        topicService.deleteTopic(topicId);
        
        return ResponseEntity.ok().build();
    }
    
    // UUID 또는 이메일로 사용자 조회하는 헬퍼 메서드
    private User getUserByIdentifier(String identifier) {
        try {
            // UUID로 파싱 시도
            UUID userId = UUID.fromString(identifier);
            return authService.getUserById(userId);
        } catch (IllegalArgumentException e) {
            // UUID가 아니면 email로 간주
            log.info("UUID가 아닌 식별자로 사용자 조회: {}", identifier);
            return authService.getUserByEmail(identifier);
        }
    }
    
    // 팀 접근 권한 검증 헬퍼 메서드  
    private Team validateTeamAccess(UUID teamId, UUID userId) {
        try {
            // TeamService의 getTeamById는 팀 멤버 권한도 함께 확인함
            TeamResponse teamResponse = teamService.getTeamById(teamId, userId);
            
            // TeamResponse에서 Team 엔티티로 변환
            return Team.builder()
                    .id(teamResponse.getId())
                    .name(teamResponse.getName())
                    .createdAt(teamResponse.getCreatedAt())
                    .build();
        } catch (SecurityException e) {
            throw new SecurityException("팀에 접근할 권한이 없습니다: " + e.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException("팀을 찾을 수 없습니다: " + e.getMessage());
        }
    }
} 