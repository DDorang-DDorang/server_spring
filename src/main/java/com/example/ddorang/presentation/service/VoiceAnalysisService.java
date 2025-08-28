package com.example.ddorang.presentation.service;

import com.example.ddorang.presentation.entity.Presentation;
import com.example.ddorang.presentation.entity.VoiceAnalysis;
import com.example.ddorang.presentation.entity.SttResult;
import com.example.ddorang.presentation.repository.VoiceAnalysisRepository;
import com.example.ddorang.presentation.repository.SttResultRepository;
import com.example.ddorang.presentation.repository.PresentationRepository;
import com.example.ddorang.presentation.dto.VoiceAnalysisResponse;
import com.example.ddorang.presentation.dto.SttResultResponse;
import com.example.ddorang.common.service.NotificationService;
import com.example.ddorang.auth.entity.User;
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
    private final NotificationService notificationService;

    /**
     * FastAPI 응답 데이터를 받아 VoiceAnalysis와 SttResult 저장
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

        log.info("분석 결과 저장 완료: {}", presentationId);
        
        // AI 분석 완료 알림 발송
        User owner = presentation.getTopic().getUser();
        if (owner != null) {
            notificationService.sendAnalysisCompleteNotification(
                owner.getUserId(), 
                presentation.getTitle(), 
                presentationId
            );
        }
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
                .build();

        sttResultRepository.save(sttResult);
        log.info("SttResult 저장 완료: {}", presentation.getId());
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
     * 분석 결과 존재 여부 확인
     */
    public boolean hasAnalysisResults(UUID presentationId) {
        return voiceAnalysisRepository.existsByPresentationId(presentationId) &&
               sttResultRepository.existsByPresentationId(presentationId);
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
} 