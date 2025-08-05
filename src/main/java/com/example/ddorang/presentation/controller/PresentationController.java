package com.example.ddorang.presentation.controller;

import com.example.ddorang.common.ApiPaths;
import com.example.ddorang.presentation.entity.Presentation;
import com.example.ddorang.presentation.service.PresentationService;
import com.example.ddorang.presentation.dto.PresentationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.ROOT)
@RequiredArgsConstructor
@Slf4j
public class PresentationController {
    
    private final PresentationService presentationService;
    private final ObjectMapper objectMapper;
    
    // 새 프레젠테이션 생성
    @PostMapping("/topics/{topicId}/presentations")
    public ResponseEntity<PresentationResponse> createPresentation(
            @PathVariable UUID topicId,
            @RequestParam("presentationData") String presentationDataJson,
            @RequestParam(value = "videoFile", required = false) MultipartFile videoFile) {
        
        log.info("프레젠테이션 생성 요청 - 토픽: {}", topicId);
        
        try {
            // JSON 파싱
            CreatePresentationRequest request = objectMapper.readValue(presentationDataJson, CreatePresentationRequest.class);
            
            // 프레젠테이션 생성
            Presentation presentation = presentationService.createPresentation(
                    topicId,
                    request.getTitle(),
                    request.getScript(),
                    request.getGoalTime(),
                    videoFile
            );
            
            PresentationResponse response = PresentationResponse.from(presentation);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("프레젠테이션 생성 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 특정 프레젠테이션 조회
    @GetMapping("/presentations/{presentationId}")
    public ResponseEntity<PresentationResponse> getPresentation(@PathVariable UUID presentationId) {
        log.info("프레젠테이션 조회 요청 - ID: {}", presentationId);
        
        try {
            Presentation presentation = presentationService.getPresentationById(presentationId);
            PresentationResponse response = PresentationResponse.from(presentation);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("프레젠테이션 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // 팀 프레젠테이션 조회 (권한 확인)
    @GetMapping("/presentations/{presentationId}/team")
    public ResponseEntity<PresentationResponse> getTeamPresentation(
            @PathVariable UUID presentationId,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("팀 프레젠테이션 조회 요청 - ID: {}, 사용자: {}", presentationId, userId);
        
        try {
            Presentation presentation = presentationService.getTeamPresentation(presentationId, userId);
            PresentationResponse response = PresentationResponse.from(presentation);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("팀 프레젠테이션 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 프레젠테이션 수정 (권한 확인)
    @PutMapping("/presentations/{presentationId}")
    public ResponseEntity<PresentationResponse> updatePresentation(
            @PathVariable UUID presentationId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody UpdatePresentationRequest request) {
        
        log.info("프레젠테이션 수정 요청 - ID: {}, 사용자: {}", presentationId, userId);
        
        try {
            // 권한 확인
            if (!presentationService.canModifyPresentation(presentationId, userId)) {
                log.error("프레젠테이션 수정 권한 없음 - ID: {}, 사용자: {}", presentationId, userId);
                return ResponseEntity.status(403).build();
            }
            
            Presentation presentation = presentationService.updatePresentation(
                    presentationId,
                    request.getTitle(),
                    request.getScript(),
                    request.getGoalTime()
            );
            
            PresentationResponse response = PresentationResponse.from(presentation);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("프레젠테이션 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 프레젠테이션 삭제 (권한 확인)
    @DeleteMapping("/presentations/{presentationId}")
    public ResponseEntity<Void> deletePresentation(
            @PathVariable UUID presentationId,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("프레젠테이션 삭제 요청 - ID: {}, 사용자: {}", presentationId, userId);
        
        try {
            // 권한 확인
            if (!presentationService.canModifyPresentation(presentationId, userId)) {
                log.error("프레젠테이션 삭제 권한 없음 - ID: {}, 사용자: {}", presentationId, userId);
                return ResponseEntity.status(403).build();
            }
            
            presentationService.deletePresentation(presentationId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("프레젠테이션 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 비디오 업로드 (별도 업로드)
    @PostMapping("/presentations/{presentationId}/video")
    public ResponseEntity<PresentationResponse> uploadVideo(
            @PathVariable UUID presentationId,
            @RequestParam("videoFile") MultipartFile videoFile) {
        
        log.info("비디오 업로드 요청 - 프레젠테이션: {}", presentationId);
        
        try {
            Presentation presentation = presentationService.updateVideoFile(presentationId, videoFile);
            PresentationResponse response = PresentationResponse.from(presentation);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("비디오 업로드 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 사용자의 모든 프레젠테이션 조회
    @GetMapping("/users/{userId}/presentations")
    public ResponseEntity<List<PresentationResponse>> getUserPresentations(@PathVariable UUID userId) {
        log.info("사용자 프레젠테이션 목록 조회 요청 - 사용자: {}", userId);
        
        try {
            List<Presentation> presentations = presentationService.getPresentationsByUserId(userId);
            List<PresentationResponse> response = presentations.stream()
                    .map(PresentationResponse::from)
                    .toList();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("사용자 프레젠테이션 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 프레젠테이션 검색
    @GetMapping("/topics/{topicId}/presentations/search")
    public ResponseEntity<List<PresentationResponse>> searchPresentations(
            @PathVariable UUID topicId,
            @RequestParam String keyword) {
        
        log.info("프레젠테이션 검색 요청 - 토픽: {}, 키워드: {}", topicId, keyword);
        
        try {
            List<Presentation> presentations = presentationService.searchPresentations(topicId, keyword);
            List<PresentationResponse> response = presentations.stream()
                    .map(PresentationResponse::from)
                    .toList();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("프레젠테이션 검색 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 팀의 모든 프레젠테이션 조회
    @GetMapping("/teams/{teamId}/presentations")
    public ResponseEntity<List<PresentationResponse>> getTeamPresentations(
            @PathVariable UUID teamId,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("팀 프레젠테이션 목록 조회 요청 - 팀: {}, 사용자: {}", teamId, userId);
        
        try {
            List<Presentation> presentations = presentationService.getTeamPresentations(teamId, userId);
            List<PresentationResponse> response = presentations.stream()
                    .map(PresentationResponse::from)
                    .toList();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("팀 프레젠테이션 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 팀 프레젠테이션 수정 (권한 확인)
    @PutMapping("/presentations/{presentationId}/team")
    public ResponseEntity<PresentationResponse> updateTeamPresentation(
            @PathVariable UUID presentationId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody UpdatePresentationRequest request) {
        
        log.info("팀 프레젠테이션 수정 요청 - ID: {}, 사용자: {}", presentationId, userId);
        
        try {
            Presentation presentation = presentationService.updateTeamPresentation(
                    presentationId,
                    userId,
                    request.getTitle(),
                    request.getScript(),
                    request.getGoalTime()
            );
            
            PresentationResponse response = PresentationResponse.from(presentation);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("팀 프레젠테이션 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // 팀 프레젠테이션 삭제 (권한 확인)
    @DeleteMapping("/presentations/{presentationId}/team")
    public ResponseEntity<Void> deleteTeamPresentation(
            @PathVariable UUID presentationId,
            @RequestHeader("X-User-Id") UUID userId) {
        
        log.info("팀 프레젠테이션 삭제 요청 - ID: {}, 사용자: {}", presentationId, userId);
        
        try {
            presentationService.deleteTeamPresentation(presentationId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("팀 프레젠테이션 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 내부 클래스들
    public static class CreatePresentationRequest {
        private String title;
        private String script;
        private Integer goalTime;
        private String type;
        private String originalFileName;
        private Integer duration;
        
        // getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getScript() { return script; }
        public void setScript(String script) { this.script = script; }
        public Integer getGoalTime() { return goalTime; }
        public void setGoalTime(Integer goalTime) { this.goalTime = goalTime; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getOriginalFileName() { return originalFileName; }
        public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
        public Integer getDuration() { return duration; }
        public void setDuration(Integer duration) { this.duration = duration; }
    }
    
    public static class UpdatePresentationRequest {
        private String title;
        private String script;
        private Integer goalTime;
        
        // getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getScript() { return script; }
        public void setScript(String script) { this.script = script; }
        public Integer getGoalTime() { return goalTime; }
        public void setGoalTime(Integer goalTime) { this.goalTime = goalTime; }
    }
} 