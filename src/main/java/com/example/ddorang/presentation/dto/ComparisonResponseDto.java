package com.example.ddorang.presentation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparisonResponseDto {
    
    private UUID id;
    
    @JsonProperty("presentation1")
    private PresentationInfo presentation1;
    
    @JsonProperty("presentation2")
    private PresentationInfo presentation2;
    
    @JsonProperty("comparisonData")
    private ComparisonDataDto comparisonData;
    
    @JsonProperty("comparisonSummary")
    private String comparisonSummary;
    
    @JsonProperty("strengths_comparison")
    private String strengthsComparison;
    
    @JsonProperty("improvement_suggestions")
    private String improvementSuggestions;
    
    @JsonProperty("overall_feedback")
    private String overallFeedback;
    
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PresentationInfo {
        private UUID id;
        private String title;
        private LocalDateTime createdAt;
    }
}

