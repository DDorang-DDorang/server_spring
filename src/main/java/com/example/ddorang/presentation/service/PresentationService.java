package com.example.ddorang.presentation.service;

import com.example.ddorang.presentation.entity.Presentation;
import com.example.ddorang.presentation.entity.Topic;
import com.example.ddorang.presentation.repository.PresentationRepository;
import com.example.ddorang.presentation.repository.TopicRepository;
import com.example.ddorang.presentation.repository.VoiceAnalysisRepository;
import com.example.ddorang.presentation.repository.SttResultRepository;
import com.example.ddorang.common.service.FileStorageService;
import com.example.ddorang.presentation.service.FastApiService;
import com.example.ddorang.presentation.service.VoiceAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PresentationService {
    
    private final PresentationRepository presentationRepository;
    private final TopicRepository topicRepository;
    private final VoiceAnalysisRepository voiceAnalysisRepository;
    private final SttResultRepository sttResultRepository;
    private final FileStorageService fileStorageService;
    private final FastApiService fastApiService;
    private final VoiceAnalysisService voiceAnalysisService;
    
    // 특정 토픽의 프레젠테이션 목록 조회
    public List<Presentation> getPresentationsByTopicId(UUID topicId) {
        log.info("토픽 {}의 프레젠테이션 목록 조회", topicId);
        return presentationRepository.findByTopicId(topicId);
    }
    
    // 특정 프레젠테이션 조회
    public Presentation getPresentationById(UUID presentationId) {
        log.info("프레젠테이션 {} 조회", presentationId);
        return presentationRepository.findById(presentationId)
                .orElseThrow(() -> new RuntimeException("프레젠테이션을 찾을 수 없습니다."));
    }
    
    // 새 프레젠테이션 생성
    @Transactional
    public Presentation createPresentation(UUID topicId, String title, String script, Integer goalTime, MultipartFile videoFile) {
        log.info("새 프레젠테이션 생성: {} (토픽: {})", title, topicId);
        
        // 토픽 존재 확인
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("토픽을 찾을 수 없습니다."));
        
        // 비디오 파일 처리
        String videoUrl = null;
        if (videoFile != null && !videoFile.isEmpty()) {
            try {
                // 파일 저장 (사용자 ID와 토픽 ID를 projectId로 사용)
                String userId = topic.getUser() != null ? topic.getUser().getUserId().toString() : "anonymous";
                Long projectId = Long.valueOf(Math.abs(topicId.hashCode())); // UUID를 Long으로 변환
                
                FileStorageService.FileInfo fileInfo = fileStorageService.storeVideoFile(videoFile, userId, projectId);
                // relativePath에서 videos/ 부분을 제거하고 URL 생성
                String cleanPath = fileInfo.relativePath;
                if (cleanPath.startsWith("videos/")) {
                    cleanPath = cleanPath.substring("videos/".length());
                }
                videoUrl = "/api/files/videos/" + cleanPath;
                
                log.info("비디오 파일 저장 완료: {}", videoUrl);
                log.info("원본 relativePath: {}", fileInfo.relativePath);
                log.info("정리된 경로: {}", cleanPath);
            } catch (Exception e) {
                log.error("비디오 파일 저장 실패: {}", e.getMessage());
                throw new RuntimeException("비디오 파일 저장에 실패했습니다: " + e.getMessage());
            }
        }
        
        // 제목이 없으면 기본 제목 설정
        if (title == null || title.trim().isEmpty()) {
            title = "새 프레젠테이션 " + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
        
        Presentation presentation = Presentation.builder()
                .topic(topic)
                .title(title)
                .script(script != null ? script : "")
                .videoUrl(videoUrl)
                .goalTime(goalTime)
                .createdAt(LocalDateTime.now())
                .build();
        
        Presentation savedPresentation = presentationRepository.save(presentation);
        log.info("프레젠테이션 생성 완료: {}", savedPresentation.getId());

        // 비디오 파일이 있는 경우 FastAPI 분석 수행
        if (videoFile != null && !videoFile.isEmpty()) {
            Map<String, Object> analysisResult = null;
            try {
                log.info("FastAPI 분석 요청 시작");
                analysisResult = fastApiService.analyzeVideo(videoFile);
                log.info("FastAPI 분석 결과: {}", analysisResult);
                
                // 분석 결과 저장
                voiceAnalysisService.saveAnalysisResults(savedPresentation.getId(), analysisResult);
                log.info("분석 결과 저장 완료");
            } catch (Exception e) {
                log.error("FastAPI 분석 요청 실패: {}", e.getMessage());
                // 분석 실패는 프레젠테이션 생성을 막지 않음
            }
            
            // 목표시간이 있고 대본이 있는 경우 대본 최적화도 함께 실행
            if (goalTime != null && script != null && !script.trim().isEmpty() && analysisResult != null) {
                try {
                    log.info("목표시간이 설정되어 대본 최적화 시작: {}분", goalTime);
                    Integer goalTimeSeconds = goalTime * 60; // 분 → 초 변환
                    
                    // 영상 분석 결과에서 실제 영상 길이 추출
                    Integer currentDurationSeconds = fastApiService.extractDurationFromAnalysis(analysisResult);
                    log.info("추출된 영상 길이: {}초", currentDurationSeconds);
                    
                    Map<String, Object> optimizeResult = fastApiService.optimizeScript(
                        script, goalTimeSeconds, currentDurationSeconds);
                    
                    // 최적화된 대본으로 업데이트
                    String optimizedScript = (String) optimizeResult.get("optimized_script");
                    if (optimizedScript != null && !optimizedScript.trim().isEmpty()) {
                        savedPresentation.setScript(optimizedScript);
                        savedPresentation = presentationRepository.save(savedPresentation);
                        log.info("대본 최적화 완료 및 저장됨");
                    }
                } catch (Exception e) {
                    log.error("대본 최적화 실패: {}", e.getMessage());
                    // 대본 최적화 실패해도 프레젠테이션 생성은 계속
                }
            }
        }
        
        return savedPresentation;
    }
    
    // 프레젠테이션 수정
    @Transactional
    public Presentation updatePresentation(UUID presentationId, String title, String script, Integer goalTime) {
        log.info("프레젠테이션 {} 수정", presentationId);
        
        Presentation presentation = getPresentationById(presentationId);
        
        if (title != null && !title.trim().isEmpty()) {
            presentation.setTitle(title);
        }
        if (script != null) {
            presentation.setScript(script);
        }
        if (goalTime != null) {
            presentation.setGoalTime(goalTime);
        }
        
        Presentation savedPresentation = presentationRepository.save(presentation);
        log.info("프레젠테이션 수정 완료: {}", savedPresentation.getId());
        
        return savedPresentation;
    }
    
    // 비디오 파일 업데이트 (별도 업로드)
    @Transactional
    public Presentation updateVideoFile(UUID presentationId, MultipartFile videoFile) {
        log.info("프레젠테이션 {} 비디오 파일 업데이트", presentationId);
        
        Presentation presentation = getPresentationById(presentationId);
        
        if (videoFile != null && !videoFile.isEmpty()) {
            try {
                // 기존 파일 삭제 (필요시)
                if (presentation.getVideoUrl() != null) {
                    // TODO: 기존 파일 삭제 로직 구현
                }
                
                // 새 파일 저장
                String userId = presentation.getTopic().getUser() != null ? 
                    presentation.getTopic().getUser().getUserId().toString() : "anonymous";
                Long projectId = Long.valueOf(Math.abs(presentation.getTopic().getId().hashCode()));
                
                FileStorageService.FileInfo fileInfo = fileStorageService.storeVideoFile(videoFile, userId, projectId);
                // relativePath에서 videos/ 부분을 제거하고 URL 생성
                String cleanPath = fileInfo.relativePath;
                if (cleanPath.startsWith("videos/")) {
                    cleanPath = cleanPath.substring("videos/".length());
                }
                String videoUrl = "/api/files/videos/" + cleanPath;
                
                presentation.setVideoUrl(videoUrl);
                
                log.info("비디오 파일 업데이트 완료: {}", videoUrl);
                log.info("원본 relativePath: {}", fileInfo.relativePath);
                log.info("정리된 경로: {}", cleanPath);
            } catch (Exception e) {
                log.error("비디오 파일 업데이트 실패: {}", e.getMessage());
                throw new RuntimeException("비디오 파일 업데이트에 실패했습니다: " + e.getMessage());
            }
        }
        
        return presentationRepository.save(presentation);
    }
    
    // 프레젠테이션 삭제
    @Transactional
    public void deletePresentation(UUID presentationId) {
        log.info("프레젠테이션 {} 삭제", presentationId);
        
        Presentation presentation = getPresentationById(presentationId);
        
        // 관련된 VoiceAnalysis 데이터 삭제
        voiceAnalysisRepository.findByPresentationId(presentationId)
                .ifPresent(voiceAnalysis -> {
                    voiceAnalysisRepository.delete(voiceAnalysis);
                    log.info("VoiceAnalysis 삭제 완료: {}", presentationId);
                });
        
        // 관련된 SttResult 데이터 삭제
        sttResultRepository.findByPresentationId(presentationId)
                .ifPresent(sttResult -> {
                    sttResultRepository.delete(sttResult);
                    log.info("SttResult 삭제 완료: {}", presentationId);
                });
        
        // 비디오 파일 삭제 (필요시)
        if (presentation.getVideoUrl() != null) {
            // TODO: 파일 삭제 로직 구현
        }
        
        // 프레젠테이션 삭제
        presentationRepository.delete(presentation);
        log.info("프레젠테이션 및 관련 댓글 삭제 완료: {}", presentationId);
    }

    // 사용자의 모든 프레젠테이션 조회
    public List<Presentation> getPresentationsByUserId(UUID userId) {
        log.info("사용자 {}의 프레젠테이션 목록 조회", userId);
        return presentationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    // 프레젠테이션 검색
    public List<Presentation> searchPresentations(UUID topicId, String keyword) {
        log.info("토픽 {}에서 프레젠테이션 검색: {}", topicId, keyword);
        return presentationRepository.searchPresentationsByKeyword(topicId, keyword);
    }
    
    // 사용자의 프레젠테이션 검색
    public List<Presentation> searchUserPresentations(UUID userId, String keyword) {
        log.info("사용자 {}의 프레젠테이션 검색: {}", userId, keyword);
        return presentationRepository.searchUserPresentationsByKeyword(userId, keyword);
    }
} 