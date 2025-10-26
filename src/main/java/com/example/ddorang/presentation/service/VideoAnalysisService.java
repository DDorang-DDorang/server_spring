package com.example.ddorang.presentation.service;

import com.example.ddorang.common.enums.JobStatus;
import com.example.ddorang.common.service.NotificationService;
import com.example.ddorang.presentation.entity.VideoAnalysisJob;
import com.example.ddorang.presentation.event.AnalysisCompleteEvent;
import com.example.ddorang.presentation.repository.VideoAnalysisJobRepository;
import com.example.ddorang.presentation.service.VoiceAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoAnalysisService {

    private final VideoAnalysisJobRepository videoAnalysisJobRepository;
    private final NotificationService notificationService;
    private final VoiceAnalysisService voiceAnalysisService;
    private final ApplicationEventPublisher eventPublisher;

    // 메모리에 결과 임시 저장 (TTL 캐시)
    private final Map<UUID, CacheEntry> resultCache = new ConcurrentHashMap<>();

    // 캐시 엔트리 클래스
    private static class CacheEntry {
        private final Map<String, Object> data;
        private final LocalDateTime expireTime;

        public CacheEntry(Map<String, Object> data) {
            this.data = data;
            this.expireTime = LocalDateTime.now().plusDays(7); // 7일 후 만료
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expireTime);
        }

        public Map<String, Object> getData() {
            return data;
        }
    }

    // 작업 초기 상태 설정
    public void initializeJob(VideoAnalysisJob job) {
        try {
            log.info("작업 초기화: {}", job.getId());

            // DB에 이미 저장되어 있으므로 별도 처리 불필요
            log.info("작업 초기화 완료: {}", job.getId());

        } catch (Exception e) {
            log.error("작업 초기화 실패: {}", job.getId(), e);
            throw new RuntimeException("작업 초기화에 실패했습니다", e);
        }
    }

    // 작업 상태 업데이트
    public void updateJobStatus(UUID jobId, String status, String message) {
        try {
            VideoAnalysisJob job = videoAnalysisJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 작업: " + jobId));

            // 상태 업데이트
            JobStatus newStatus = JobStatus.valueOf(status.toUpperCase());
            job.setStatus(newStatus);

            if (JobStatus.FAILED.equals(newStatus)) {
                job.setErrorMessage(message);
            }

            videoAnalysisJobRepository.save(job);
            log.debug("상태 업데이트: {} - {}", jobId, message);

        } catch (Exception e) {
            log.error("상태 업데이트 실패: {}", jobId, e);
        }
    }

    // 작업 완료 처리 - 이벤트 발행
    @Transactional
    public void completeJob(UUID jobId, Map<String, Object> analysisResult) {
        try {
            log.info("작업 완료 처리 시작: {}", jobId);

            // 분석 결과를 메모리 캐시에 저장 (7일간 보관)
            resultCache.put(jobId, new CacheEntry(analysisResult));

            // 만료된 캐시 엔트리 정리
            cleanupExpiredCache();

            // 상태를 'COMPLETED'로 업데이트
            VideoAnalysisJob job = videoAnalysisJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 작업: " + jobId));

            job.setStatus(JobStatus.COMPLETED);
            videoAnalysisJobRepository.save(job);
            // 분석 결과를 DB에 저장 (VoiceAnalysis, SttResult, PresentationFeedback)
            try {
                UUID presentationId = job.getPresentation().getId();
                voiceAnalysisService.saveAnalysisResults(presentationId, analysisResult);
                log.info("분석 결과 DB 저장 완료: {}", presentationId);
            } catch (Exception dbError) {
                log.error("분석 결과 DB 저장 실패: {}", jobId, dbError);
                // DB 저장 실패해도 작업은 완료로 처리 (메모리 캐시는 있음)
            }

            // 알림에 필요한 정보 추출 (트랜잭션 내에서 Lazy Loading)
            UUID userId = job.getPresentation().getTopic().getUser().getUserId();
            String presentationTitle = job.getPresentation().getTitle();
            UUID presentationId = job.getPresentation().getId();

            // 이벤트 발행 (트랜잭션 커밋 후 알림 발송됨)
            eventPublisher.publishEvent(new AnalysisCompleteEvent(
                jobId, userId, presentationTitle, presentationId, true
            ));

            log.info("작업 완료 처리 성공: {}", jobId);

        } catch (Exception e) {
            log.error("작업 완료 처리 실패: {}", jobId, e);
            markJobAsFailed(jobId, "결과 저장 중 오류: " + e.getMessage());
        }
    }

    // 작업 실패 처리 - 이벤트 발행
    @Transactional
    public void markJobAsFailed(UUID jobId, String errorMessage) {
        try {
            log.error("작업 실패 처리: {} - {}", jobId, errorMessage);

            VideoAnalysisJob job = videoAnalysisJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 작업: " + jobId));

            job.markAsFailed(errorMessage);
            videoAnalysisJobRepository.save(job);

            // 알림에 필요한 정보 추출 (트랜잭션 내에서 Lazy Loading)
            UUID userId = job.getPresentation().getTopic().getUser().getUserId();
            String presentationTitle = job.getPresentation().getTitle();
            UUID presentationId = job.getPresentation().getId();

            // 실패 이벤트 발행 (트랜잭션 커밋 후 알림 발송됨)
            eventPublisher.publishEvent(new AnalysisCompleteEvent(
                jobId, userId, presentationTitle, presentationId, false
            ));

        } catch (Exception e) {
            log.error("실패 처리 중 추가 오류: {}", jobId, e);
        }
    }

    // 상태 조회 (사용자 폴링용)
    public Map<String, Object> getJobStatus(UUID jobId) {
        try {
            VideoAnalysisJob job = videoAnalysisJobRepository.findById(jobId).orElse(null);

            if (job == null) {
                return null;
            }

            Map<String, Object> status = new HashMap<>();
            status.put("presentationId", job.getPresentation().getId().toString());
            status.put("status", job.getStatus().toString().toLowerCase());
            status.put("message", getStatusMessage(job));
            status.put("createdAt", job.getCreatedAt().toString());

            return status;

        } catch (Exception e) {
            log.error("상태 조회 실패: {}", jobId, e);
            return null;
        }
    }

    // 결과 조회
    public Map<String, Object> getJobResult(UUID jobId) {
        try {
            CacheEntry entry = resultCache.get(jobId);

            if (entry == null) {
                return null;
            }

            // 만료된 캐시 확인
            if (entry.isExpired()) {
                resultCache.remove(jobId);
                log.debug("만료된 캐시 제거: {}", jobId);
                return null;
            }

            return entry.getData();
        } catch (Exception e) {
            log.error("결과 조회 실패: {}", jobId, e);
            return null;
        }
    }

    // 만료된 캐시 엔트리 정리
    private void cleanupExpiredCache() {
        resultCache.entrySet().removeIf(entry -> {
            if (entry.getValue().isExpired()) {
                log.debug("만료된 캐시 엔트리 제거: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }

    // === Private 헬퍼 메서드들 ===

    // 상태별 메세지 생성
    private String getStatusMessage(VideoAnalysisJob job) {
        return switch (job.getStatus()) {
            case PENDING -> "분석을 준비하고 있습니다...";
            case PROCESSING -> "FastAPI에서 분석 중...";
            case COMPLETED -> "분석이 완료되었습니다.";
            case FAILED -> job.getErrorMessage() != null ?
                "분석 중 오류가 발생했습니다: " + job.getErrorMessage() :
                "분석 중 오류가 발생했습니다";
        };
    }

    // 캐시 통계 조회 (모니터링용)
    public int getCacheSize() {
        cleanupExpiredCache(); // 정리 후 사이즈 반환
        return resultCache.size();
    }

    // 캐시 전체 정리 (필요시 사용)
    public void clearAllCache() {
        resultCache.clear();
        log.info("전체 캐시 정리 완료");
    }
}