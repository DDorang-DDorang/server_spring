package com.example.ddorang.presentation.service;

import com.example.ddorang.presentation.entity.Presentation;
import com.example.ddorang.presentation.entity.VoiceAnalysis;
import com.example.ddorang.presentation.entity.SttResult;
import com.example.ddorang.presentation.entity.PresentationFeedback;
import com.example.ddorang.presentation.repository.VoiceAnalysisRepository;
import com.example.ddorang.presentation.repository.SttResultRepository;
import com.example.ddorang.presentation.repository.PresentationRepository;
import com.example.ddorang.presentation.repository.PresentationFeedbackRepository;
import com.example.ddorang.presentation.dto.VoiceAnalysisResponse;
import com.example.ddorang.presentation.dto.SttResultResponse;
import com.example.ddorang.presentation.dto.PresentationFeedbackResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VoiceAnalysisService {

    private final VoiceAnalysisRepository voiceAnalysisRepository;
    private final SttResultRepository sttResultRepository;
    private final PresentationRepository presentationRepository;
    private final PresentationFeedbackRepository presentationFeedbackRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * FastAPI 응답 데이터를 받아 VoiceAnalysis, SttResult, PresentationFeedback 저장
     */
    @Transactional
    public void saveAnalysisResults(UUID presentationId, Map<String, Object> fastApiResponse) {
        log.info("프레젠테이션 {}에 대한 분석 결과 저장", presentationId);

        Presentation presentation = presentationRepository.findById(presentationId)
                .orElseThrow(() -> new RuntimeException("프레젠테이션을 찾을 수 없습니다: " + presentationId));

        // VoiceAnalysis 저장
        saveVoiceAnalysis(presentation, fastApiResponse);

        // SttResult 저장
        saveSttResult(presentation, fastApiResponse);

        // PresentationFeedback 저장
        savePresentationFeedback(presentation, fastApiResponse);

        log.info("분석 결과 저장 완료: {}", presentationId);
    }

    private void saveVoiceAnalysis(Presentation presentation, Map<String, Object> response) {
        // 기존 분석 결과가 있으면 삭제
        voiceAnalysisRepository.findByPresentationId(presentation.getId())
                .ifPresent(voiceAnalysisRepository::delete);

        VoiceAnalysis voiceAnalysis = VoiceAnalysis.builder()
                .presentation(presentation)
                // 음성 강도 분석
                .intensityGrade(getStringValue(response, "intensity_grade"))
                .intensityDb(getFloatValue(response, "intensity_db"))
                .intensityText(getStringValue(response, "intensity_text"))
                // 피치 분석
                .pitchGrade(getStringValue(response, "pitch_grade"))
                .pitchAvg(getFloatValue(response, "pitch_avg"))
                .pitchText(getStringValue(response, "pitch_text"))
                // WPM 분석
                .wpmGrade(getStringValue(response, "wpm_grade"))
                .wpmAvg(getFloatValue(response, "wpm_avg"))
                .wpmComment(getStringValue(response, "wpm_comment"))
                .build();

        voiceAnalysisRepository.save(voiceAnalysis);
        log.info("VoiceAnalysis 저장 완료: {}", presentation.getId());
    }

    private void saveSttResult(Presentation presentation, Map<String, Object> response) {
        // 기존 STT 결과가 있으면 삭제
        sttResultRepository.findByPresentationId(presentation.getId())
                .ifPresent(sttResultRepository::delete);

        SttResult sttResult = SttResult.builder()
                .presentation(presentation)
                .transcription(getStringValue(response, "transcription"))
                .pronunciationScore(getFloatValue(response, "pronunciation_score"))
                .adjustedScript(getStringValue(response, "adjusted_script"))
                .correctedScript(getStringValue(response, "corrected_script"))
                .build();

        sttResultRepository.save(sttResult);
        log.info("SttResult 저장 완료: {}", presentation.getId());
    }

    private void savePresentationFeedback(Presentation presentation, Map<String, Object> response) {
        // 기존 피드백이 있으면 삭제
        presentationFeedbackRepository.findByPresentationId(presentation.getId())
                .ifPresent(presentationFeedbackRepository::delete);

        try {
            // feedback 객체 추출
            @SuppressWarnings("unchecked")
            Map<String, Object> feedback = (Map<String, Object>) response.get("feedback");
            
            if (feedback != null) {
                PresentationFeedback presentationFeedback = PresentationFeedback.builder()
                        .presentation(presentation)
                        .frequentWords(convertToJsonString(feedback.get("frequent_words")))
                        .awkwardSentences(convertToJsonString(feedback.get("awkward_sentences")))
                        .difficultyIssues(convertToJsonString(feedback.get("difficulty_issues")))
                        .predictedQuestions(convertToJsonString(response.get("predicted_questions")))
                        .build();

                presentationFeedbackRepository.save(presentationFeedback);
                log.info("PresentationFeedback 저장 완료: {}", presentation.getId());
            }
        } catch (Exception e) {
            log.error("피드백 저장 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 프레젠테이션의 음성 분석 결과 조회
     */
    public VoiceAnalysisResponse getVoiceAnalysis(UUID presentationId) {
        return voiceAnalysisRepository.findByPresentationId(presentationId)
                .map(VoiceAnalysisResponse::from)
                .orElse(null);
    }

    /**
     * 프레젠테이션의 STT 결과 조회
     */
    public SttResultResponse getSttResult(UUID presentationId) {
        return sttResultRepository.findByPresentationId(presentationId)
                .map(SttResultResponse::from)
                .orElse(null);
    }

    /**
     * 프레젠테이션의 피드백 결과 조회
     */
    public PresentationFeedbackResponse getPresentationFeedback(UUID presentationId) {
        return presentationFeedbackRepository.findByPresentationId(presentationId)
                .map(PresentationFeedbackResponse::from)
                .orElse(null);
    }

    /**
     * 사용자의 모든 음성 분석 결과 조회
     */
    public List<VoiceAnalysisResponse> getUserVoiceAnalyses(UUID userId) {
        return voiceAnalysisRepository.findByUserId(userId).stream()
                .map(VoiceAnalysisResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 모든 STT 결과 조회
     */
    public List<SttResultResponse> getUserSttResults(UUID userId) {
        return sttResultRepository.findByUserId(userId).stream()
                .map(SttResultResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 모든 피드백 결과 조회
     */
    public List<PresentationFeedbackResponse> getUserPresentationFeedbacks(UUID userId) {
        return presentationFeedbackRepository.findByUserId(userId).stream()
                .map(PresentationFeedbackResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 분석 결과 존재 여부 확인
     */
    public boolean hasAnalysisResults(UUID presentationId) {
        // 하나라도 분석 결과가 있으면 true 반환
        return voiceAnalysisRepository.existsByPresentationId(presentationId) ||
               sttResultRepository.existsByPresentationId(presentationId) ||
               presentationFeedbackRepository.existsByPresentationId(presentationId);
    }

    // 유틸리티 메서드들
    private String getStringValue(Map<String, Object> response, String key) {
        Object value = response.get(key);
        return value != null ? value.toString() : null;
    }

    private Float getFloatValue(Map<String, Object> response, String key) {
        Object value = response.get(key);
        if (value == null) return null;
        
        try {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Float 변환 실패: {} = {}", key, value);
            return null;
        }
    }

    private String convertToJsonString(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON 변환 실패: {}", e.getMessage());
            return null;
        }
    }
} 