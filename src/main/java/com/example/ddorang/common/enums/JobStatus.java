package com.example.ddorang.common.enums;


// 비동기 작업의 상태를 나타내는 열거형
public enum JobStatus {
    PENDING("대기중"),     // Redis 큐에 들어간 상태
    PROCESSING("처리중"),  // FastAPI Worker가 처리 중인 상태
    COMPLETED("완료"),     // 성공적으로 완료된 상태
    FAILED("실패");        // 처리 중 오류가 발생한 상태

    private final String description;

    JobStatus(String description) {
        this.description = description;
    }

    // 작업이 진행 중인지 확인
    public boolean isInProgress() {
        return this == PENDING || this == PROCESSING;
    }

  // 작업이 완료되었는지 확인 (성공/실패 무관)
    public boolean isFinished() {
        return this == COMPLETED || this == FAILED;
    }
}