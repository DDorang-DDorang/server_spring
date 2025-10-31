package com.example.ddorang.presentation.dto;

import com.example.ddorang.presentation.entity.VideoAnalysisJob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 사용자 대시보드용 작업 요약 DTO
 *
 * 역할:
 * 1. 사용자의 모든 분석 작업 목록 표시
 * 2. 각 작업의 상태 요약
 * 3. 완료된 작업의 결과 링크 제공
 */

// 사용자 대시보드 용 작업 요약 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoAnalysisJobSummary {

    private UUID jobId;

    private UUID presentationId;

    private String presentationTitle;

    private String originalFilename;

    private String status;              // "pending", "processing", "completed", "failed"

    // 진행률은 현재 미사용 (상태만 추적)

    private String message;             // 현재 상태 메시지

    private LocalDateTime createdAt;    // 작업 생성 시간


    private String errorMessage;        // 실패 시 오류 메시지

    private Boolean hasResult;          // 결과 조회 가능 여부

    /**
     * Entity + 상태 정보로부터 요약 생성
     */
    public static VideoAnalysisJobSummary from(VideoAnalysisJob job, Map<String, Object> statusInfo) {
        VideoAnalysisJobSummaryBuilder builder = VideoAnalysisJobSummary.builder()
            .jobId(job.getId())
            .presentationId(job.getPresentation().getId())
            .presentationTitle(job.getPresentation().getTitle())
            .originalFilename(job.getOriginalFilename())
            .createdAt(job.getCreatedAt())
            .errorMessage(job.getErrorMessage());

        // DB 기반 상태 정보 설정
        if (statusInfo != null) {
            String status = (String) statusInfo.get("status");
            String message = (String) statusInfo.get("message");
            builder.status(status)
                   .message(message)
                   .hasResult("completed".equals(status));
        } else {
            // 상태 정보가 없으면 DB 상태 사용
            builder.status(job.getStatus().toString().toLowerCase())
                   .message(job.getErrorMessage() != null ? "분석 실패" : "상태 정보 없음")
                   .hasResult(job.getStatus().toString().equals("COMPLETED"));
        }

        return builder.build();
    }

    /**
     * 진행 중인 작업인지 확인
     */
    public boolean isInProgress() {
        return "pending".equals(status) || "processing".equals(status);
    }

    /**
     * 완료된 작업인지 확인
     */
    public boolean isCompleted() {
        return "completed".equals(status);
    }

    /**
     * 실패한 작업인지 확인
     */
    public boolean isFailed() {
        return "failed".equals(status);
    }
}