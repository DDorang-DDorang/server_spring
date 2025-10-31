package com.example.ddorang.presentation.dto;

import com.example.ddorang.presentation.entity.PresentationFeedback;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresentationFeedbackResponse {
    
    private UUID id;
    private UUID presentationId;
    private String presentationTitle;
    
    // 자주 사용된 단어들
    private List<String> frequentWords;
    
    // 어색한 문장들
    private List<Map<String, String>> awkwardSentences;
    
    // 난이도 문제들
    private List<Map<String, String>> difficultyIssues;
    
    // 예측된 질문들
    private List<String> predictedQuestions;
    
    // Entity에서 DTO로 변환하는 정적 메서드
    public static PresentationFeedbackResponse from(PresentationFeedback feedback) {
        ObjectMapper objectMapper = new ObjectMapper();
        
        try {
            List<String> frequentWords = feedback.getFrequentWords() != null ?
                    objectMapper.readValue(feedback.getFrequentWords(), new TypeReference<List<String>>() {}) : null;
            
            List<Map<String, String>> awkwardSentences = feedback.getAwkwardSentences() != null ?
                    objectMapper.readValue(feedback.getAwkwardSentences(), new TypeReference<List<Map<String, String>>>() {}) : null;
            
            List<Map<String, String>> difficultyIssues = feedback.getDifficultyIssues() != null ?
                    objectMapper.readValue(feedback.getDifficultyIssues(), new TypeReference<List<Map<String, String>>>() {}) : null;
            
            List<String> predictedQuestions = feedback.getPredictedQuestions() != null ?
                    objectMapper.readValue(feedback.getPredictedQuestions(), new TypeReference<List<String>>() {}) : null;
            
            return PresentationFeedbackResponse.builder()
                    .id(feedback.getId())
                    .presentationId(feedback.getPresentation().getId())
                    .presentationTitle(feedback.getPresentation().getTitle())
                    .frequentWords(frequentWords)
                    .awkwardSentences(awkwardSentences)
                    .difficultyIssues(difficultyIssues)
                    .predictedQuestions(predictedQuestions)
                    .build();
                    
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 파싱 오류", e);
        }
    }
} 