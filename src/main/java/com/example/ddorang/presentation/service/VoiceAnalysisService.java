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
import java.util.Optional;
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

        try {
        Presentation presentation = presentationRepository.findById(presentationId)
                .orElseThrow(() -> new RuntimeException("프레젠테이션을 찾을 수 없습니다: " + presentationId));

            log.info("프레젠테이션 조회 성공: {}", presentation.getTitle());

            // FastAPI 응답 구조 확인 및 처리
            Map<String, Object> analysisResult;
            
            // result 객체가 있는 경우 (새로운 구조)
            if (fastApiResponse.containsKey("result")) {
                analysisResult = (Map<String, Object>) fastApiResponse.get("result");
                log.info("새로운 FastAPI 응답 구조 사용 (result 객체 포함)");
            } 
            // result 객체가 없는 경우 (기존 구조 - 직접 분석 결과)
            else {
                analysisResult = fastApiResponse;
                log.info("기존 FastAPI 응답 구조 사용 (직접 분석 결과)");
            }
            
            if (analysisResult == null || analysisResult.isEmpty()) {
                log.error("분석 결과 데이터가 비어있습니다: {}", fastApiResponse);
                throw new RuntimeException("분석 결과 데이터가 올바르지 않습니다.");
            }

            log.info("분석 결과 데이터 추출 성공: {}", analysisResult.keySet());

            // 실제 DB에 분석 결과 저장
            saveVoiceAnalysis(presentation, analysisResult);
            saveSttResult(presentation, analysisResult);
            savePresentationFeedback(presentation, analysisResult);

            // 알림은 VideoAnalysisService의 이벤트 리스너를 통해 자동으로 발송됨
        } catch (Exception e) {
            log.error("분석 결과 저장 중 오류 발생: {}", presentationId, e);
            throw e;
        }
    }

    private void saveVoiceAnalysis(Presentation presentation, Map<String, Object> response) {
        try {
            log.info("VoiceAnalysis 저장 시작 - 프레젠테이션: {}", presentation.getId());
            
        // 기존 분석 결과가 있으면 삭제
        voiceAnalysisRepository.findByPresentationId(presentation.getId())
                .ifPresent(voiceAnalysisRepository::delete);

            log.info("기존 VoiceAnalysis 삭제 완료");

            // 표정 등급과 텍스트 계산
            String expressionGrade = calculateExpressionGrade(response);
            String expressionText = generateExpressionText(response);
            
            log.info("표정 분석 결과 - 등급: {}, 텍스트: {}", expressionGrade, expressionText);

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
                // 표정 분석 (감정 분석 기반)
                .expressionGrade(expressionGrade)
                .expressionText(expressionText)
                    // 감정 분석 추가
                    .emotionAnalysis(convertToJsonString(response.get("emotion_analysis")))
                .build();

            log.info("VoiceAnalysis 객체 생성 완료");

        voiceAnalysisRepository.save(voiceAnalysis);
        log.info("VoiceAnalysis 저장 완료: {}", presentation.getId());
        } catch (Exception e) {
            log.error("VoiceAnalysis 저장 실패: {}", presentation.getId(), e);
            throw e;
        }
    }

    private void saveSttResult(Presentation presentation, Map<String, Object> response) {
        try {
            log.info("SttResult 저장 시작 - 프레젠테이션: {}", presentation.getId());
            
        // 기존 STT 결과가 있으면 삭제
        sttResultRepository.findByPresentationId(presentation.getId())
                .ifPresent(sttResultRepository::delete);

            log.info("기존 SttResult 삭제 완료");

        SttResult sttResult = SttResult.builder()
                .presentation(presentation)
                .transcription(getStringValue(response, "transcription"))
                .pronunciationScore(getFloatValue(response, "pronunciation_score"))
                    .adjustedScript(getStringValue(response, "adjusted_script")) // FastAPI에서 제공하지 않을 수 있음
                    .correctedScript(getStringValue(response, "corrected_transcription")) // corrected_transcription으로 변경
                .build();

            log.info("SttResult 객체 생성 완료");

        sttResultRepository.save(sttResult);
        log.info("SttResult 저장 완료: {}", presentation.getId());
        } catch (Exception e) {
            log.error("SttResult 저장 실패: {}", presentation.getId(), e);
            throw e;
        }
    }

    private void savePresentationFeedback(Presentation presentation, Map<String, Object> response) {
        try {
            log.info("PresentationFeedback 저장 시작 - 프레젠테이션: {}", presentation.getId());
            
        // 기존 피드백이 있으면 삭제
        presentationFeedbackRepository.findByPresentationId(presentation.getId())
                .ifPresent(presentationFeedbackRepository::delete);

            log.info("기존 PresentationFeedback 삭제 완료");

            // feedback 객체 추출
            @SuppressWarnings("unchecked")
            Map<String, Object> feedback = (Map<String, Object>) response.get("feedback");
            
            if (feedback != null) {
                log.info("feedback 객체 발견: {}", feedback.keySet());
                
                PresentationFeedback presentationFeedback = PresentationFeedback.builder()
                        .presentation(presentation)
                        .frequentWords(convertToJsonString(feedback.get("frequent_words")))
                        .awkwardSentences(convertToJsonString(feedback.get("awkward_sentences")))
                        .difficultyIssues(convertToJsonString(feedback.get("difficulty_issues")))
                        .predictedQuestions(convertToJsonString(response.get("predicted_questions")))
                        .build();

                log.info("PresentationFeedback 객체 생성 완료");

                presentationFeedbackRepository.save(presentationFeedback);
                log.info("PresentationFeedback 저장 완료: {}", presentation.getId());
            } else {
                log.warn("feedback 객체가 없습니다. 기본 피드백 생성");
                
                // 기본 피드백 생성
                PresentationFeedback presentationFeedback = PresentationFeedback.builder()
                        .presentation(presentation)
                        .frequentWords("[]")
                        .awkwardSentences("[]")
                        .difficultyIssues("[]")
                        .predictedQuestions(convertToJsonString(response.get("predicted_questions")))
                        .build();

                presentationFeedbackRepository.save(presentationFeedback);
                log.info("기본 PresentationFeedback 저장 완료: {}", presentation.getId());
            }
        } catch (Exception e) {
            log.error("PresentationFeedback 저장 실패: {}", presentation.getId(), e);
            throw e;
        }
    }

    /**
     * 감정 분석 기반 표정 등급 계산
     */
    private String calculateExpressionGrade(Map<String, Object> response) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> emotionAnalysis = (Map<String, Object>) response.get("emotion_analysis");

            if (emotionAnalysis == null) {
                return "C"; // 기본값
            }

            double positive = getDoubleValue(emotionAnalysis, "positive");
            double neutral = getDoubleValue(emotionAnalysis, "neutral");
            double negative = getDoubleValue(emotionAnalysis, "negative");

            log.info("감정 분석 결과 - 긍정: {}%, 중립: {}%, 부정: {}%", positive, neutral, negative);

            // ✅ 긍정 - 부정 차이 계산
            double balance = positive - negative; // +면 긍정적, -면 부정적

            // ✅ 표정 등급 계산
            if (balance >= 10) {
                return "A"; // 매우 긍정적
            } else if (balance >= 5) {
                return "B"; // 긍정적
            } else if (balance > -5) {
                return "C"; // 무표정
            } else if (balance > -10) {
                return "D"; // 부정적
            } else {
                return "E"; // 매우 부정적
            }

        } catch (Exception e) {
            log.error("표정 등급 계산 실패: {}", e.getMessage());
            return "C";
        }
    }
    
    /**
     * 감정 분석 기반 표정 텍스트 생성
     */
    private String generateExpressionText(Map<String, Object> response) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> emotionAnalysis = (Map<String, Object>) response.get("emotion_analysis");

            if (emotionAnalysis == null) {
                return "표정 분석 데이터를 사용할 수 없습니다.";
            }

            double positive = getDoubleValue(emotionAnalysis, "positive");
            double neutral = getDoubleValue(emotionAnalysis, "neutral");
            double negative = getDoubleValue(emotionAnalysis, "negative");

            // ✅ 긍정 - 부정 밸런스 계산
            double balance = positive - negative; // +면 긍정적, -면 부정적

            // ✅ 텍스트 생성
            if (balance >= 10) {
                return "매우 밝고 긍정적인 표정을 유지했습니다. 발표에 자신감이 잘 드러납니다.";
            } else if (balance >= 5) {
                return "긍정적이고 자연스러운 표정을 보였습니다. 청중에게 좋은 인상을 주었습니다.";
            } else if (balance >= -5) {
                return "표정 변화가 적습니다. 조금 더 미소를 지어보면 좋습니다.";
            } else if (balance >= -10) {
                return "다소 부정적인 표정이 보였습니다. 긴장을 풀고 편안한 표정을 연습해보세요.";
            } else {
                return "부정적인 표정이 많이 나타났습니다. 발표 전 마음의 준비가 필요합니다.";
            }

        } catch (Exception e) {
            log.error("표정 텍스트 생성 실패: {}", e.getMessage());
            return "표정 분석 결과를 처리할 수 없습니다.";
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

    private Double getDoubleValue(Map<String, Object> response, String key) {
        Object value = response.get(key);
        if (value == null) return 0.0;
        
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Double 변환 실패: {} = {}", key, value);
            return 0.0;
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