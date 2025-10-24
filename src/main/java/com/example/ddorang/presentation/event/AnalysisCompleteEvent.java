package com.example.ddorang.presentation.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * 영상 분석 완료 이벤트
 * 트랜잭션 커밋 후 알림 발송을 위해 사용
 */
@Getter
@RequiredArgsConstructor
public class AnalysisCompleteEvent {
    private final UUID jobId;
    private final UUID userId;
    private final String presentationTitle;
    private final UUID presentationId;
    private final boolean isSuccess;  // 성공/실패 구분
}