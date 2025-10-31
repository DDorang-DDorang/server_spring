package com.example.ddorang.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


// 비동기 영상 분석 요청 응답 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoAnalysisResponse {

    private UUID jobId;          // 작업 추적용 고유 ID

    private UUID presentationId; // 분석 대상 프레젠테이션

    private String status;       // "pending", "processing", "completed", "failed"

    private String message;      // 사용자에게 보여줄 메시지


}