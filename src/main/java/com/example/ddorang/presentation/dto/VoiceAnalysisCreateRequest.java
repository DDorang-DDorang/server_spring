package com.example.ddorang.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceAnalysisCreateRequest {
    
    private UUID presentationId;
    
    // 음성 강도 분석
    private String intensityGrade;
    private Float intensityDb;
    private String intensityText;
    
    // 피치 분석
    private String pitchGrade;
    private Float pitchAvg;
    private String pitchText;
    
    // WPM 분석
    private String wpmGrade;
    private Float wpmAvg;
    private String wpmComment;
} 