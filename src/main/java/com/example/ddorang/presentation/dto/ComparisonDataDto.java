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
        
        // 음성 분석 등급 (ABCDE)
        private String intensityGrade;   // 음성 강도 등급
        private String pitchGrade;       // 피치 등급
        private String wpmGrade;         // 말하기 속도 등급
        private String expressionGrade;  // 표정 등급
        private String expressionText;   // 표정 분석 텍스트
        private String emotionAnalysis;  // 감정 분석 결과 (JSON)
        private Float pronunciationScore; // 발음 정확성
    }
}