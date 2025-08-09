package com.example.ddorang.presentation.controller;

import com.example.ddorang.auth.util.SecurityUtil;
import com.example.ddorang.common.ApiPaths;
import com.example.ddorang.presentation.service.FastApiService;
import com.example.ddorang.presentation.service.VoiceAnalysisService;
import com.example.ddorang.presentation.service.PresentationService;
import com.example.ddorang.presentation.dto.VoiceAnalysisResponse;
import com.example.ddorang.presentation.dto.SttResultResponse;
import com.example.ddorang.presentation.entity.Presentation;
import com.example.ddorang.presentation.entity.Topic;
import com.example.ddorang.auth.entity.User;
import com.example.ddorang.auth.repository.UserRepository;
import com.example.ddorang.team.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(ApiPaths.ROOT+"/video-analysis")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class VideoAnalysisController {

    private final FastApiService fastApiService;
    private final VoiceAnalysisService voiceAnalysisService;
    private final PresentationService presentationService;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * 비디오 파일을 업로드하여 음성 분석 수행
     */
    @PostMapping("/analyze/{presentationId}")
    public ResponseEntity<Map<String, Object>> analyzeVideo(
            @PathVariable UUID presentationId,
            @RequestParam("videoFile") MultipartFile videoFile) {

        try {
            UUID userId = SecurityUtil.getCurrentUserId();
            log.info("비디오 분석 요청: presentationId={}, userId={}, fileName={}",
                    presentationId, userId, videoFile.getOriginalFilename());

            // 권한 검증
            if (!hasAnalysisPermission(presentationId, userId)) {
                log.error("비디오 분석 권한 없음 - presentationId: {}, userId: {}", presentationId, userId);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "해당 발표를 분석할 권한이 없습니다.");
                return ResponseEntity.status(403).body(errorResponse);
            }

            // FastAPI로 비디오 분석 요청
            Map<String, Object> analysisResult = fastApiService.analyzeVideo(videoFile);

            // 분석 결과를 DB에 저장
            voiceAnalysisService.saveAnalysisResults(presentationId, analysisResult);

            // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "비디오 분석이 완료되었습니다.");
            response.put("presentationId", presentationId);
            response.put("analysisResult", analysisResult);

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            log.error("인증 실패: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "인증되지 않은 사용자입니다.");
            return ResponseEntity.status(401).body(errorResponse);
        } catch (Exception e) {
            log.error("비디오 분석 실패: presentationId={}", presentationId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "비디오 분석 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("presentationId", presentationId);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 프레젠테이션의 음성 분석 결과 조회
     */
    @GetMapping("/voice-analysis/{presentationId}")
    public ResponseEntity<VoiceAnalysisResponse> getVoiceAnalysis(@PathVariable UUID presentationId) {
        try {
            VoiceAnalysisResponse voiceAnalysis = voiceAnalysisService.getVoiceAnalysis(presentationId);

            if (voiceAnalysis != null) {
                return ResponseEntity.ok(voiceAnalysis);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("음성 분석 결과 조회 실패: presentationId={}", presentationId, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 프레젠테이션의 STT 결과 조회
     */
    @GetMapping("/stt-result/{presentationId}")
    public ResponseEntity<SttResultResponse> getSttResult(@PathVariable UUID presentationId) {
        try {
            SttResultResponse sttResult = voiceAnalysisService.getSttResult(presentationId);

            if (sttResult != null) {
                return ResponseEntity.ok(sttResult);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("STT 결과 조회 실패: presentationId={}", presentationId, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 프레젠테이션의 모든 분석 결과 조회
     */
    @GetMapping("/results/{presentationId}")
    public ResponseEntity<Map<String, Object>> getAllAnalysisResults(@PathVariable UUID presentationId) {
        try {
            VoiceAnalysisResponse voiceAnalysis = voiceAnalysisService.getVoiceAnalysis(presentationId);
            SttResultResponse sttResult = voiceAnalysisService.getSttResult(presentationId);

            Map<String, Object> response = new HashMap<>();
            response.put("voiceAnalysis", voiceAnalysis);
            response.put("sttResult", sttResult);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("분석 결과 조회 실패: presentationId={}", presentationId, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 분석 결과 존재 여부 확인
     */
    @GetMapping("/has-results/{presentationId}")
    public ResponseEntity<Map<String, Object>> hasAnalysisResults(@PathVariable UUID presentationId) {
        try {
            boolean hasResults = voiceAnalysisService.hasAnalysisResults(presentationId);

            Map<String, Object> response = new HashMap<>();
            response.put("presentationId", presentationId);
            response.put("hasResults", hasResults);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("분석 결과 존재 여부 확인 실패: presentationId={}", presentationId, e);
            return ResponseEntity.status(500).build();
        }
    }

     // AI 분석 권한 검증

    private boolean hasAnalysisPermission(UUID presentationId, UUID userId) {
        try {
            // 1. 발표 조회
            Presentation presentation = presentationService.getPresentationById(presentationId);
            Topic topic = presentation.getTopic();
            
            // 2. 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            // 3. 개인 발표인 경우 - 소유자만 분석 가능
            if (topic.getTeam() == null) {
                return topic.getUser().getUserId().equals(userId);
            }
            
            // 4. 팀 발표인 경우 - 팀원 분석 가능
            return teamMemberRepository.existsByTeamAndUser(topic.getTeam(), user);
            
        } catch (Exception e) {
            log.error("권한 검증 실패: presentationId={}, userId={}", presentationId, userId, e);
            return false;
        }
    }
}