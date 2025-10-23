package com.example.ddorang.presentation.service;

import com.example.ddorang.presentation.entity.Presentation;
import com.example.ddorang.presentation.entity.PresentationComparison;
import com.example.ddorang.presentation.entity.Topic;
import com.example.ddorang.presentation.repository.*;
import com.example.ddorang.presentation.repository.PresentationRepository;
import com.example.ddorang.presentation.repository.TopicRepository;
import com.example.ddorang.presentation.repository.VoiceAnalysisRepository;
import com.example.ddorang.presentation.repository.SttResultRepository;
import com.example.ddorang.presentation.repository.VideoAnalysisJobRepository;
import com.example.ddorang.presentation.entity.VideoAnalysisJob;
import com.example.ddorang.common.service.FileStorageService;
import com.example.ddorang.presentation.service.FastApiService;
import com.example.ddorang.presentation.service.VoiceAnalysisService;
import com.example.ddorang.team.entity.Team;
import com.example.ddorang.team.entity.TeamMember;
import com.example.ddorang.team.repository.TeamRepository;
import com.example.ddorang.team.repository.TeamMemberRepository;
import com.example.ddorang.auth.entity.User;
import com.example.ddorang.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PresentationService {
    
    private final PresentationRepository presentationRepository;
    private final TopicRepository topicRepository;
    private final VoiceAnalysisRepository voiceAnalysisRepository;
    private final SttResultRepository sttResultRepository;
    private final PresentationFeedbackRepository presentationFeedbackRepository;
    private final PresentationComparisonRepository presentationComparisonRepository;
    private final FileStorageService fileStorageService;
    private final FastApiService fastApiService;
    private final VoiceAnalysisService voiceAnalysisService;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final VideoAnalysisJobRepository videoAnalysisJobRepository;
    
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
        
        try {
        // 토픽 존재 확인
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("토픽을 찾을 수 없습니다."));
        
            // 토픽당 발표 개수 제한 확인 (최대 2개)
            long presentationCount = presentationRepository.countByTopicId(topicId);
            if (presentationCount >= 2) {
                log.warn("토픽 {}에 이미 {}개의 발표가 있습니다. 최대 2개까지만 생성 가능합니다.", topicId, presentationCount);
                throw new RuntimeException("토픽당 최대 2개의 발표만 생성할 수 있습니다. 새로운 발표를 추가하려면 기존 발표를 삭제해주세요.");
            }
            log.info("토픽 {}의 현재 발표 개수: {} (최대 2개)", topicId, presentationCount);
        
            // 현재 인증 상태 확인
            try {
                org.springframework.security.core.Authentication auth = 
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                
                if (auth != null) {
                    log.info("현재 인증 정보 - 인증됨: {}, Principal 타입: {}, Principal: {}", 
                        auth.isAuthenticated(), 
                        auth.getPrincipal() != null ? auth.getPrincipal().getClass().getName() : "null",
                        auth.getPrincipal() != null ? auth.getPrincipal().toString() : "null");
                } else {
                    log.error("현재 인증 정보가 null입니다.");
                }
            } catch (Exception e) {
                log.error("인증 정보 확인 중 오류: {}", e.getMessage(), e);
            }
            
            // 현재 사용자 ID 가져오기
            UUID currentUserId = null;
            try {
                log.info("SecurityUtil.getCurrentUserId() 호출 시작");
                currentUserId = com.example.ddorang.common.util.SecurityUtil.getCurrentUserId();
                log.info("SecurityUtil.getCurrentUserId() 성공: {}", currentUserId);
            } catch (Exception e) {
                log.error("SecurityUtil.getCurrentUserId() 실패: {}", e.getMessage(), e);
                throw new RuntimeException("사용자 인증 정보를 가져올 수 없습니다: " + e.getMessage());
            }
            
            // 권한 검증
            if (topic.getTeam() != null) {
                // 팀 토픽인 경우 팀 멤버 권한 확인
                log.info("팀 토픽 권한 확인 - 팀: {}, 팀 ID: {}", topic.getTeam().getName(), topic.getTeam().getId());
                
                // CustomUserDetails에서 팀 멤버십 정보 확인
                try {
                    com.example.ddorang.auth.security.CustomUserDetails userDetails = 
                        (com.example.ddorang.auth.security.CustomUserDetails) 
                        org.springframework.security.core.context.SecurityContextHolder.getContext()
                            .getAuthentication().getPrincipal();
                    
                    log.info("CustomUserDetails 로드 성공 - 사용자: {}", userDetails.getUser().getName());
                    log.info("팀 멤버십 개수: {}", userDetails.getTeamMemberships() != null ? userDetails.getTeamMemberships().size() : 0);
                    
                    if (userDetails.getTeamMemberships() != null) {
                        userDetails.getTeamMemberships().forEach(tm -> {
                            log.info("팀 멤버십: 팀 {} ({}), 역할: {}", 
                                tm.getTeam().getName(), tm.getTeam().getId(), tm.getRole());
                        });
                    }
                    
                    boolean isTeamMember = userDetails.isMemberOfTeam(topic.getTeam().getId());
                    log.info("팀 멤버 권한 확인 결과 (CustomUserDetails): {}", isTeamMember);
                    
                    if (!isTeamMember) {
                        // 더 자세한 디버깅 정보
                        log.error("팀 멤버 권한 확인 실패 - 팀: {}, 사용자: {}, 사용자 ID: {}", 
                            topic.getTeam().getName(), userDetails.getUser().getName(), currentUserId);
                        
                        // 팀 멤버 목록 확인
                        List<TeamMember> teamMembers = teamMemberRepository.findByTeamOrderByJoinedAtAsc(topic.getTeam());
                        log.info("팀 멤버 목록: {}", teamMembers.stream()
                            .map(tm -> tm.getUser().getName() + "(" + tm.getUser().getUserId() + ")")
                            .collect(Collectors.joining(", ")));
                
                throw new RuntimeException("팀 멤버만 프레젠테이션을 생성할 수 있습니다");
            }
            
            log.info("팀 토픽 프레젠테이션 생성 권한 확인 완료 - 팀: {}, 사용자: {}", 
                            topic.getTeam().getName(), userDetails.getUser().getName());
                } catch (Exception e) {
                    log.error("CustomUserDetails에서 권한 확인 중 오류 발생: {}", e.getMessage(), e);
                    throw new RuntimeException("사용자 권한 정보를 확인할 수 없습니다: " + e.getMessage());
                }
        } else {
                // 개인 토픽인 경우 소유자 권한 확인
                log.info("개인 토픽 권한 확인 - 토픽 소유자: {}, 현재 사용자: {}", 
                    topic.getUser().getUserId(), currentUserId);
            
            if (!topic.getUser().getUserId().equals(currentUserId)) {
                throw new RuntimeException("본인의 토픽에만 프레젠테이션을 생성할 수 있습니다");
            }
            
            log.info("개인 토픽 프레젠테이션 생성 권한 확인 완료 - 사용자: {}", currentUserId);
        }
        
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

            return savedPresentation;
            
                } catch (Exception e) {
            log.error("프레젠테이션 생성 중 오류 발생: {}", e.getMessage(), e);
            throw e; // 원본 예외를 그대로 던짐
        }
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
        
        // 1. 관련된 PresentationFeedback 데이터 삭제
        presentationFeedbackRepository.findByPresentationId(presentationId)
                .ifPresent(feedback -> {
                    presentationFeedbackRepository.delete(feedback);
                    log.info("PresentationFeedback 삭제 완료: {}", presentationId);
                });
        
        // 2. 관련된 VoiceAnalysis 데이터 삭제
        voiceAnalysisRepository.findByPresentationId(presentationId)
                .ifPresent(voiceAnalysis -> {
                    voiceAnalysisRepository.delete(voiceAnalysis);
                    log.info("VoiceAnalysis 삭제 완료: {}", presentationId);
                });
        
        // 3. 관련된 SttResult 데이터 삭제
        sttResultRepository.findByPresentationId(presentationId)
                .ifPresent(sttResult -> {
                    sttResultRepository.delete(sttResult);
                    log.info("SttResult 삭제 완료: {}", presentationId);
                });
        
        // 4. 관련된 PresentationComparison 데이터 삭제
        try {
            // 먼저 개별 삭제 시도
            List<PresentationComparison> comparisons = presentationComparisonRepository.findComparisonsInvolving(
                presentation.getTopic().getUser().getUserId(), presentationId);
            if (!comparisons.isEmpty()) {
                for (PresentationComparison comparison : comparisons) {
                    presentationComparisonRepository.delete(comparison);
                }
                log.info("PresentationComparison 삭제 완료: {} ({}개 삭제)", presentationId, comparisons.size());
            }
        } catch (Exception e) {
            log.warn("개별 삭제 실패, 배치 삭제 시도: {}", e.getMessage());
            // 개별 삭제가 실패하면 배치 삭제 시도
            try {
                presentationComparisonRepository.deleteByPresentation1OrPresentation2(presentation);
                log.info("PresentationComparison 배치 삭제 완료: {}", presentationId);
            } catch (Exception batchException) {
                log.error("PresentationComparison 삭제 실패: {}", batchException.getMessage());
                throw new RuntimeException("발표 비교 데이터 삭제 중 오류가 발생했습니다: " + batchException.getMessage());
            }
        }
        
        // 5. 비디오 파일 삭제 (필요시)
        if (presentation.getVideoUrl() != null) {
            // TODO: 파일 삭제 로직 구현
        }
        
        // 6. 프레젠테이션 삭제
        presentationRepository.delete(presentation);
        log.info("프레젠테이션 및 관련 데이터 삭제 완료: {}", presentationId);
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

    // 팀 프레젠테이션 조회 (팀원만 접근 가능)
    public Presentation getTeamPresentation(UUID presentationId, UUID userId) {
        log.info("팀 프레젠테이션 {} 조회 요청 - 사용자: {}", presentationId, userId);
        
        Presentation presentation = getPresentationById(presentationId);
        Topic topic = presentation.getTopic();
        
        // 토픽이 팀에 속한 경우에만 팀원 권한 확인
        if (topic.getTeam() != null) {
            try {
                // CustomUserDetails에서 팀 멤버십 확인
                org.springframework.security.core.Authentication auth = 
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                
                if (auth != null && auth.getPrincipal() instanceof com.example.ddorang.auth.security.CustomUserDetails) {
                    com.example.ddorang.auth.security.CustomUserDetails userDetails = 
                        (com.example.ddorang.auth.security.CustomUserDetails) auth.getPrincipal();
                    
                    boolean isTeamMember = userDetails.isMemberOfTeam(topic.getTeam().getId());
                    log.info("팀 멤버십 확인 결과 (CustomUserDetails): {}", isTeamMember);
                    
                    if (!isTeamMember) {
                        throw new RuntimeException("팀 멤버만 접근할 수 있습니다");
                    }
                } else {
                    // CustomUserDetails를 사용할 수 없는 경우 기존 방식 사용
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            boolean isTeamMember = teamMemberRepository.existsByTeamAndUser(topic.getTeam(), user);
            if (!isTeamMember) {
                        throw new RuntimeException("팀 멤버만 접근할 수 있습니다");
                    }
                }
            } catch (Exception e) {
                log.error("팀 멤버십 확인 실패: {}", e.getMessage(), e);
                throw new RuntimeException("팀 멤버만 접근할 수 있습니다");
            }
        } else {
            // 개인 발표인 경우 소유자만 접근 가능
            if (!topic.getUser().getUserId().equals(userId)) {
                throw new RuntimeException("본인의 발표만 조회할 수 있습니다");
            }
        }
        
        return presentation;
    }

    // 팀의 모든 프레젠테이션 조회
    public List<Presentation> getTeamPresentations(UUID teamId, UUID userId) {
        log.info("팀 {}의 프레젠테이션 목록 조회 - 사용자: {}", teamId, userId);
        
        try {
            // CustomUserDetails에서 팀 멤버십 확인
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            
            if (auth != null && auth.getPrincipal() instanceof com.example.ddorang.auth.security.CustomUserDetails) {
                com.example.ddorang.auth.security.CustomUserDetails userDetails = 
                    (com.example.ddorang.auth.security.CustomUserDetails) auth.getPrincipal();
                
                boolean isTeamMember = userDetails.isMemberOfTeam(teamId);
                log.info("팀 멤버십 확인 결과 (CustomUserDetails): {}", isTeamMember);
                
                if (!isTeamMember) {
                    throw new RuntimeException("팀 멤버만 접근할 수 있습니다");
                }
            } else {
                // CustomUserDetails를 사용할 수 없는 경우 기존 방식 사용
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("팀을 찾을 수 없습니다"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        
        // 팀 멤버 권한 확인
        if (!teamMemberRepository.existsByTeamAndUser(team, user)) {
            throw new RuntimeException("팀 멤버만 접근할 수 있습니다");
                }
        }
        
        return presentationRepository.findByTeamIdOrderByCreatedAtDesc(teamId);
        } catch (Exception e) {
            log.error("팀 프레젠테이션 조회 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    // 프레젠테이션 권한 확인 (수정/삭제 시 사용)
    public boolean hasAccessToPresentation(UUID presentationId, UUID userId) {
        try {
            Presentation presentation = getPresentationById(presentationId);
            Topic topic = presentation.getTopic();
            
            // 개인 발표인 경우 소유자 확인
            if (topic.getTeam() == null) {
                return topic.getUser().getUserId().equals(userId);
            }
            
            // 팀 발표인 경우 팀원 확인
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            return teamMemberRepository.existsByTeamAndUser(topic.getTeam(), user);
        } catch (Exception e) {
            log.error("프레젠테이션 권한 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    // 프레젠테이션 수정 권한 확인 (더 엄격한 권한)
    public boolean canModifyPresentation(UUID presentationId, UUID userId) {
        try {
            Presentation presentation = getPresentationById(presentationId);
            Topic topic = presentation.getTopic();
            
            // 개인 발표인 경우 소유자만 수정 가능
            if (topic.getTeam() == null) {
                return topic.getUser().getUserId().equals(userId);
            }
            
            // 팀 발표인 경우 발표 작성자 또는 팀 관리자만 수정 가능
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            
            // 발표 작성자인지 확인 (Topic의 user 필드를 통해)
            if (topic.getUser() != null && topic.getUser().getUserId().equals(userId)) {
                return true;
            }
            
            // 팀장인지 확인
            TeamMember member = teamMemberRepository.findByTeamAndUser(topic.getTeam(), user)
                    .orElse(null);
            
            return member != null && member.getRole() == TeamMember.Role.OWNER;
        } catch (Exception e) {
            log.error("프레젠테이션 수정 권한 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    // 팀 프레젠테이션 수정 (권한 확인 포함)
    @Transactional
    public Presentation updateTeamPresentation(UUID presentationId, UUID userId, String title, String script, Integer goalTime) {
        log.info("팀 프레젠테이션 {} 수정 요청 - 사용자: {}", presentationId, userId);
        
        // 권한 확인
        if (!canModifyPresentation(presentationId, userId)) {
            throw new RuntimeException("프레젠테이션 수정 권한이 없습니다");
        }
        
        // 기존 updatePresentation 로직 재사용
        return updatePresentation(presentationId, title, script, goalTime);
    }

    // 팀 프레젠테이션 삭제 (권한 확인 포함)
    @Transactional
    public void deleteTeamPresentation(UUID presentationId, UUID userId) {
        log.info("팀 프레젠테이션 {} 삭제 요청 - 사용자: {}", presentationId, userId);
        
        // 권한 확인
        if (!canModifyPresentation(presentationId, userId)) {
            throw new RuntimeException("프레젠테이션 삭제 권한이 없습니다");
        }
        
        // 기존 deletePresentation 로직 재사용
        deletePresentation(presentationId);
    }
    /**
     * 프레젠테이션의 목표시간 조회
     */
    public Integer getGoalTime(UUID presentationId) {
        log.info("프레젠테이션 {}의 목표시간 조회", presentationId);
        
        Presentation presentation = presentationRepository.findById(presentationId)
                .orElseThrow(() -> new RuntimeException("프레젠테이션을 찾을 수 없습니다: " + presentationId));
        
        return presentation.getGoalTime();
    }
    // 비동기 영상 분석 관련 메서드들
    @Transactional
    public VideoAnalysisJob createVideoAnalysisJob(Presentation presentation, String originalFilename, Long fileSize) {
        log.info("비동기 영상 분석 작업 생성 - 프레젠테이션: {}", presentation.getId());

        // 이미 진행 중인 작업이 있는지 확인
        videoAnalysisJobRepository.findActiveJobByPresentationId(presentation.getId())
            .ifPresent(existingJob -> {
                log.warn("진행 중인 분석 작업 존재: {}", existingJob.getId());
                throw new RuntimeException("이미 진행 중인 영상 분석 작업이 있습니다. 기존 작업: " + existingJob.getId());
            });

        // VideoAnalysisJob 생성
        VideoAnalysisJob job = VideoAnalysisJob.builder()
            .presentation(presentation)
            .videoPath(presentation.getVideoUrl())
            .originalFilename(originalFilename)
            .fileSize(fileSize)
            .build();

        // DB에 저장
        VideoAnalysisJob savedJob = videoAnalysisJobRepository.save(job);
        log.info("영상 분석 작업 생성 완료 - ID: {}", savedJob.getId());

        return savedJob;
    }

    // 사용자의 모든 영상 분석 작업 조회
    public List<VideoAnalysisJob> getUserVideoAnalysisJobs(UUID userId) {
        log.info("사용자 {}의 영상 분석 작업 목록 조회", userId);
        return videoAnalysisJobRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // 특정 프레젠테이션의 분석 작업 조회
    public List<VideoAnalysisJob> getPresentationAnalysisJobs(UUID presentationId) {
        log.info("프레젠테이션 {}의 분석 작업 목록 조회", presentationId);
        return videoAnalysisJobRepository.findByPresentationIdOrderByCreatedAtDesc(presentationId);
    }

    // 진행 중인 작업 수 조회
    public long getActiveJobCount(UUID userId) {
        long count = videoAnalysisJobRepository.countActiveJobsByUserId(userId);
        log.debug("사용자 {}의 진행 중인 작업 수: {}", userId, count);
        return count;
    }

    // 가장 최근 완료된 분석 결과 조회
    public VideoAnalysisJob getLatestCompletedAnalysis(UUID presentationId) {
        return videoAnalysisJobRepository.findLatestCompletedJobByPresentationId(presentationId)
            .orElse(null);
    }
} 