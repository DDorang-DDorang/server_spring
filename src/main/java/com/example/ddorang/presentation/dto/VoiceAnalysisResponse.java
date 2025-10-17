package com.example.ddorang.presentation.dto;

import com.example.ddorang.presentation.entity.VoiceAnalysis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceAnalysisResponse {
    
    private UUID id;
    private UUID presentationId;
    private String presentationTitle;
    
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
    
    // 표정 분석
    private String expressionGrade;
    private String expressionText;
    
    // Entity에서 DTO로 변환하는 정적 메서드
    public static VoiceAnalysisResponse from(VoiceAnalysis voiceAnalysis) {
        return VoiceAnalysisResponse.builder()
                .id(voiceAnalysis.getId())
                .presentationId(voiceAnalysis.getPresentation().getId())
                .presentationTitle(voiceAnalysis.getPresentation().getTitle())
                .intensityGrade(voiceAnalysis.getIntensityGrade())
                .intensityDb(voiceAnalysis.getIntensityDb())
                .intensityText(voiceAnalysis.getIntensityText())
                .pitchGrade(voiceAnalysis.getPitchGrade())
                .pitchAvg(voiceAnalysis.getPitchAvg())
                .pitchText(voiceAnalysis.getPitchText())
                .wpmGrade(voiceAnalysis.getWpmGrade())
                .wpmAvg(voiceAnalysis.getWpmAvg())
                .wpmComment(voiceAnalysis.getWpmComment())
                .expressionGrade(voiceAnalysis.getExpressionGrade())
                .expressionText(voiceAnalysis.getExpressionText())
                .build();
    }
} 