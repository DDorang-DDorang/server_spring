package com.example.ddorang.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonDataDto {
    
    private PresentationMetrics presentation1;
    private PresentationMetrics presentation2;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PresentationMetrics {
        private String presentationId;   // 발표 ID
        private String title;            // 발표 제목
        private Float intensityDb;       // 음성 강도
        private Float pitchAvg;          // 평균 피치
        private Float wpmAvg;            // 분당 단어 수
        
        // 미래 확장을 위한 필드들 (현재는 null)
        private Float anxietyScore;      // 불안도 (미구현)
        private Float eyeContactScore;   // 시선처리 (미구현)
        private Float pronunciationScore; // 발음 정확성 (미구현)
    }
}