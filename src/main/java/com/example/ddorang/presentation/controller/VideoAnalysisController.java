package com.example.ddorang.presentation.controller;

import com.example.ddorang.common.service.AuthorizationService;
import com.example.ddorang.common.util.SecurityUtil;
import com.example.ddorang.common.ApiPaths;
import com.example.ddorang.presentation.service.FastApiService;
import com.example.ddorang.presentation.service.VoiceAnalysisService;
import com.example.ddorang.presentation.service.PresentationService;
import com.example.ddorang.presentation.dto.VoiceAnalysisResponse;
import com.example.ddorang.presentation.dto.SttResultResponse;
import com.example.ddorang.presentation.dto.PresentationFeedbackResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final AuthorizationService authorizationService;

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

            authorizationService.requireVideoAnalysisPermission(presentationId);

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
    @PreAuthorize("hasRole('USER')")
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
    @PreAuthorize("hasRole('USER')")
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
     * 프레젠테이션의 피드백 결과 조회
     */
    @GetMapping("/feedback/{presentationId}")
    public ResponseEntity<PresentationFeedbackResponse> getPresentationFeedback(@PathVariable UUID presentationId) {
        try {
            PresentationFeedbackResponse feedback = voiceAnalysisService.getPresentationFeedback(presentationId);

            if (feedback != null) {
                return ResponseEntity.ok(feedback);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("피드백 결과 조회 실패: presentationId={}", presentationId, e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 프레젠테이션의 모든 분석 결과 조회
     */
    @GetMapping("/results/{presentationId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getAllAnalysisResults(@PathVariable UUID presentationId) {
        try {
            VoiceAnalysisResponse voiceAnalysis = voiceAnalysisService.getVoiceAnalysis(presentationId);
            SttResultResponse sttResult = voiceAnalysisService.getSttResult(presentationId);
            PresentationFeedbackResponse feedback = voiceAnalysisService.getPresentationFeedback(presentationId);

            Map<String, Object> response = new HashMap<>();
            response.put("voiceAnalysis", voiceAnalysis);
            response.put("sttResult", sttResult);
            response.put("feedback", feedback);

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
    @PreAuthorize("hasRole('USER')")
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

}