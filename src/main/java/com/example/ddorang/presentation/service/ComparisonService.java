package com.example.ddorang.presentation.service;

import com.example.ddorang.presentation.dto.ComparisonDataDto;
import com.example.ddorang.presentation.entity.Presentation;
import com.example.ddorang.presentation.entity.PresentationComparison;
import com.example.ddorang.presentation.entity.VoiceAnalysis;
import com.example.ddorang.presentation.repository.PresentationComparisonRepository;
import com.example.ddorang.presentation.repository.PresentationRepository;
import com.example.ddorang.presentation.repository.VoiceAnalysisRepository;
import com.example.ddorang.auth.entity.User;
import com.example.ddorang.auth.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComparisonService {
    
    private final PresentationComparisonRepository comparisonRepository;
    private final PresentationRepository presentationRepository;
    private final VoiceAnalysisRepository voiceAnalysisRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 두 발표를 비교하는 메인 메서드
     */
    @Transactional
    public PresentationComparison comparePresentations(UUID userId, UUID presentationId1, UUID presentationId2) {
        log.info("발표 비교 시작 - 사용자: {}, 발표1: {}, 발표2: {}", userId, presentationId1, presentationId2);
        
        // 1. 기존 비교 기록이 있는지 확인
        Optional<PresentationComparison> existingComparison = 
            comparisonRepository.findExistingComparison(userId, presentationId1, presentationId2);
            
        if (existingComparison.isPresent()) {
            log.info("기존 비교 기록 발견, 기존 데이터 반환");
            return existingComparison.get();
        }
        
        // 2. 발표 및 사용자 정보 조회
        User user = getUserById(userId);
        Presentation presentation1 = getPresentationById(presentationId1);
        Presentation presentation2 = getPresentationById(presentationId2);
        
        // 3. 권한 검증 - 두 발표 모두 해당 사용자의 것인지 확인
        validateUserOwnership(user, presentation1, presentation2);
        
        // 4. 음성 분석 데이터 조회
        VoiceAnalysis analysis1 = getVoiceAnalysis(presentationId1);
        VoiceAnalysis analysis2 = getVoiceAnalysis(presentationId2);
        
        // 5. 비교 데이터 생성
        ComparisonDataDto comparisonData = createComparisonData(analysis1, analysis2);
        
        // 6. 비교 요약 생성
        String comparisonSummary = generateComparisonSummary(presentation1, presentation2);
        
        // 7. 비교 결과 저장
        PresentationComparison comparison = PresentationComparison.builder()
                .user(user)
                .presentation1(presentation1)
                .presentation2(presentation2)
                .comparisonData(convertToJson(comparisonData))
                .comparisonSummary(comparisonSummary)
                .build();
        
        PresentationComparison savedComparison = comparisonRepository.save(comparison);
        log.info("발표 비교 완료, 결과 저장됨 - ID: {}", savedComparison.getId());
        
        return savedComparison;
    }
    
    /**
     * 두 음성 분석 데이터를 비교하여 ComparisonDataDto 생성
     */
    private ComparisonDataDto createComparisonData(VoiceAnalysis analysis1, VoiceAnalysis analysis2) {
        log.debug("비교 데이터 생성 시작");
        
        Presentation p1 = analysis1.getPresentation();
        Presentation p2 = analysis2.getPresentation();
        
        // 발표1의 메트릭스 생성
        ComparisonDataDto.PresentationMetrics metrics1 = ComparisonDataDto.PresentationMetrics.builder()
                .presentationId(p1.getId().toString())
                .title(p1.getTitle())
                .intensityDb(analysis1.getIntensityDb())
                .pitchAvg(analysis1.getPitchAvg())
                .wpmAvg(analysis1.getWpmAvg())
                // 미래 확장용 필드들은 현재 null
                .anxietyScore(null)
                .eyeContactScore(null)
                .pronunciationScore(null)
                .build();
                
        // 발표2의 메트릭스 생성
        ComparisonDataDto.PresentationMetrics metrics2 = ComparisonDataDto.PresentationMetrics.builder()
                .presentationId(p2.getId().toString())
                .title(p2.getTitle())
                .intensityDb(analysis2.getIntensityDb())
                .pitchAvg(analysis2.getPitchAvg())
                .wpmAvg(analysis2.getWpmAvg())
                .anxietyScore(null)
                .eyeContactScore(null)
                .pronunciationScore(null)
                .build();
        
        return ComparisonDataDto.builder()
                .presentation1(metrics1)
                .presentation2(metrics2)
                .build();
    }
    
    
    /**
     * 비교 요약 생성
     */
    private String generateComparisonSummary(Presentation p1, Presentation p2) {
        return String.format("'%s'와 '%s' 발표 비교가 완료되었습니다. " +
                           "육각형 그래프를 통해 두 발표의 수치를 시각적으로 비교해보세요.",
                           p1.getTitle(), p2.getTitle());
    }
    
    // === 유틸리티 메서드들 ===
    
    private User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
    }
    
    private Presentation getPresentationById(UUID presentationId) {
        return presentationRepository.findById(presentationId)
                .orElseThrow(() -> new RuntimeException("발표를 찾을 수 없습니다: " + presentationId));
    }
    
    private VoiceAnalysis getVoiceAnalysis(UUID presentationId) {
        return voiceAnalysisRepository.findByPresentationId(presentationId)
                .orElseThrow(() -> new RuntimeException("음성 분석 데이터를 찾을 수 없습니다: " + presentationId));
    }
    
    private void validateUserOwnership(User user, Presentation p1, Presentation p2) {
        UUID userId = user.getUserId();
        
        // 발표1의 소유자 확인
        if (!p1.getTopic().getUser().getUserId().equals(userId)) {
            throw new RuntimeException("발표에 대한 권한이 없습니다: " + p1.getId());
        }
        
        // 발표2의 소유자 확인
        if (!p2.getTopic().getUser().getUserId().equals(userId)) {
            throw new RuntimeException("발표에 대한 권한이 없습니다: " + p2.getId());
        }
    }
    
    private String convertToJson(ComparisonDataDto comparisonData) {
        try {
            return objectMapper.writeValueAsString(comparisonData);
        } catch (JsonProcessingException e) {
            log.error("JSON 변환 실패", e);
            throw new RuntimeException("비교 데이터 저장 중 오류가 발생했습니다", e);
        }
    }
    
    /**
     * 기존 비교 기록 조회
     */
    @Transactional(readOnly = true)
    public List<PresentationComparison> getUserComparisons(UUID userId) {
        return comparisonRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * 특정 발표와 관련된 모든 비교 기록 조회
     */
    @Transactional(readOnly = true)
    public List<PresentationComparison> getComparisonsInvolving(UUID userId, UUID presentationId) {
        return comparisonRepository.findComparisonsInvolving(userId, presentationId);
    }
}