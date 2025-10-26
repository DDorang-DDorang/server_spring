package com.example.ddorang.presentation.service;

import com.example.ddorang.presentation.entity.VideoAnalysisJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class FastApiPollingService {

    private final VideoAnalysisService videoAnalysisService;
    private final VideoChunkService videoChunkService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${fastapi.base-url:http://localhost:8000}")
    private String fastApiUrl;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // 비동기 영상 분석 시작
    @Async
    public CompletableFuture<Void> startVideoAnalysis(VideoAnalysisJob job) {
        log.info("🎬 FastAPI 비동기 분석 시작: {} - {}", job.getId(), job.getPresentation().getTitle());
        log.debug("DEBUG: VideoAnalysisJob - videoPath: {}, presentationId: {}", job.getVideoPath(), job.getPresentation().getId());
        log.debug("DEBUG: VideoChunkService bean: {}", videoChunkService != null ? "OK" : "NULL");

        try {
            // FastAPI /stt 엔드포인트 호출
            log.debug("DEBUG: callFastApiStt() 호출 직전");
            String fastApiJobId = callFastApiStt(job);
            log.debug("DEBUG: callFastApiStt() 호출 직후 - 반환값: {}", fastApiJobId);

            if (fastApiJobId == null) {
                log.warn("⚠️ FastAPI 초기 호출 실패, 백그라운드 처리 대기 중: {}", job.getId());
                videoAnalysisService.updateJobStatus(job.getId(), "processing", "분석 서버 연결 중입니다. 잠시만 기다려주세요...");
                // 실패로 마킹하지 않고 processing 상태 유지하여 폴링 기회 제공
                return CompletableFuture.completedFuture(null);
            }

            // 상태를 processing으로 업데이트
            videoAnalysisService.updateJobStatus(job.getId(), "processing", "FastAPI에서 분석 중...");

            // 백그라운드에서 결과 폴링 시작
            pollFastApiResult(job.getId(), fastApiJobId);

        } catch (Exception e) {
            log.error("FastAPI 분석 시작 실패: {}", job.getId(), e);
            videoAnalysisService.markJobAsFailed(job.getId(), "분석 시작 실패: " + e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }


    // FastAPI /stt 엔드포인트 호출 (청크 업로드 방식)
    private String callFastApiStt(VideoAnalysisJob job) {
        log.debug("DEBUG: callFastApiStt() 메서드 진입");

        try {
            log.info("📹 FastAPI STT 호출 (청크 모드): {}", job.getVideoPath());

            // 비디오 파일 경로 처리
            String videoPath = job.getVideoPath();

            log.debug("DEBUG: 원본 videoPath: {}", videoPath);

            // 웹 URL을 실제 파일 경로로 변환
            // /api/files/videos/... → uploads/videos/...
            if (videoPath.startsWith("/api/files/videos/")) {
                String relativePath = videoPath.substring("/api/files/videos/".length());
                videoPath = uploadDir + "/videos/" + relativePath;
                log.debug("DEBUG: 웹 URL을 파일 경로로 변환: {}", videoPath);
            }

            // 상대 경로인 경우 절대 경로로 변환
            if (!videoPath.startsWith("/")) {
                videoPath = System.getProperty("user.dir") + "/" + videoPath;
                log.debug("DEBUG: 절대 경로로 변환: {}", videoPath);
            }

            File videoFile = new File(videoPath);
            log.debug("DEBUG: 파일 존재 여부: {}, 크기: {}MB",
                videoFile.exists(),
                videoFile.exists() ? videoFile.length() / (1024 * 1024) : 0);

            if (!videoFile.exists()) {
                log.error("❌ 비디오 파일 없음: {}", videoPath);
                return null;
            }

            // 메타데이터 구성
            Map<String, Object> metadata = new HashMap<>();
            String targetTime = job.getPresentation().getGoalTime() != null ?
                job.getPresentation().getGoalTime() + ":00" : "6:00";
            metadata.put("target_time", targetTime);
            log.debug("DEBUG: 메타데이터 구성 완료 - target_time: {}", targetTime);

            // VideoChunkService를 통해 청크 업로드
            log.debug("DEBUG: videoChunkService.uploadVideoInChunks() 호출 직전");
            log.debug("DEBUG: videoChunkService는 null? {}", videoChunkService == null);

            String fastApiJobId = videoChunkService.uploadVideoInChunks(videoFile, metadata);

            log.debug("DEBUG: videoChunkService.uploadVideoInChunks() 호출 완료 - 반환값: {}", fastApiJobId);
            log.info("✅ FastAPI 청크 업로드 성공 - job_id: {}", fastApiJobId);
            return fastApiJobId;

        } catch (Exception e) {
            log.error("❌ FastAPI /stt 청크 업로드 실패 - 예외 타입: {}, 메시지: {}",
                e.getClass().getSimpleName(), e.getMessage(), e);
        }

        log.debug("DEBUG: callFastApiStt() null 반환");
        return null;
    }


    // FastAPI 결과 폴링
    // 5초마다 /result/{job_id} 호출
    private void pollFastApiResult(java.util.UUID springJobId, String fastApiJobId) {
        log.info("FastAPI 결과 폴링 시작: {} → {}", springJobId, fastApiJobId);

        int maxAttempts = 240; // 최대 20분 (5초 × 240회)
        int attempts = 0;

        while (attempts < maxAttempts) {
            try {
                // FastAPI /result/{job_id} 호출
                ResponseEntity<Map> response = restTemplate.getForEntity(
                    fastApiUrl + "/result/" + fastApiJobId,
                    Map.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> result = response.getBody();
                    String status = (String) result.get("status");

                    log.debug("폴링 결과: {} - {} ({}회차)", springJobId, status, attempts + 1);

                    switch (status) {
                        case "processing":
                            // 계속 대기
                            break;

                        case "completed":
                            // 분석 완료
                            Map<String, Object> analysisResult = (Map<String, Object>) result.get("result");

                            log.info("FastAPI 분석 완료: {} → {}", springJobId, fastApiJobId);

                            // DB에 결과 저장 + 직접 웹소켓 알림 발행
                            videoAnalysisService.completeJob(springJobId, analysisResult);
                            return;

                        case "error":
                            // 분석 실패
                            String error = (String) result.get("error");
                            log.error("FastAPI 분석 실패: {} - {}", springJobId, error);

                            videoAnalysisService.markJobAsFailed(springJobId, "FastAPI 분석 오류: " + error);
                            return;

                        case "not_found":
                            log.warn("⚠FastAPI 작업 없음: {}", fastApiJobId);
                            videoAnalysisService.markJobAsFailed(springJobId, "FastAPI에서 작업을 찾을 수 없음");
                            return;

                        default:
                            log.warn(" 알 수 없는 상태: {} - {}", springJobId, status);
                    }
                }

                // 5초 대기
                Thread.sleep(5000);
                attempts++;

            } catch (InterruptedException e) {
                log.info("폴링 중단: {}", springJobId);
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("폴링 오류: {} ({}회차)", springJobId, attempts + 1, e);
                attempts++;

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        // 타임아웃 처리
        log.error("FastAPI 폴링 타임아웃: {} (20분 초과)", springJobId);
        videoAnalysisService.markJobAsFailed(springJobId, "FastAPI 응답 타임아웃 (20분 초과)");
    }

}