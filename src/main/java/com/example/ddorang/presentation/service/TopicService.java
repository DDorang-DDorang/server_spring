package com.example.ddorang.presentation.service;

import com.example.ddorang.presentation.entity.Topic;
import com.example.ddorang.presentation.entity.Presentation;
import com.example.ddorang.presentation.repository.TopicRepository;
import com.example.ddorang.presentation.repository.PresentationRepository;
import com.example.ddorang.auth.entity.User;
import com.example.ddorang.team.entity.Team;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TopicService {
    
    private final TopicRepository topicRepository;
    private final PresentationRepository presentationRepository;
    
    // 사용자의 모든 토픽(프로젝트) 조회
    public List<Topic> getTopicsByUserId(UUID userId) {
        log.info("사용자 {}의 토픽 목록 조회", userId);
        return topicRepository.findByUserIdOrderByTitle(userId);
    }
    
    // 팀의 모든 토픽(프로젝트) 조회
    public List<Topic> getTopicsByTeamId(UUID teamId) {
        log.info("팀 {}의 토픽 목록 조회", teamId);
        return topicRepository.findByTeamIdOrderByTitle(teamId);
    }
    
    // 특정 토픽 조회
    public Topic getTopicById(UUID topicId) {
        log.info("토픽 {} 조회", topicId);
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("토픽을 찾을 수 없습니다."));
    }
    
    // 새 토픽(프로젝트) 생성
    @Transactional
    public Topic createTopic(String title, User user, Team team) {
        log.info("새 토픽 생성: {} (사용자: {})", title, user != null ? user.getUserId() : "팀");
        
        Topic topic = Topic.builder()
                .title(title)
                .user(user)
                .team(team)
                .build();
        
        Topic savedTopic = topicRepository.save(topic);
        log.info("토픽 생성 완료: {}", savedTopic.getId());
        
        return savedTopic;
    }
    
    // 토픽 수정
    @Transactional
    public Topic updateTopic(UUID topicId, String title) {
        log.info("토픽 {} 수정", topicId);
        
        Topic topic = getTopicById(topicId);
        topic.setTitle(title);
        
        Topic savedTopic = topicRepository.save(topic);
        log.info("토픽 수정 완료: {}", savedTopic.getId());
        
        return savedTopic;
    }
    
    // 토픽 삭제
    @Transactional
    public void deleteTopic(UUID topicId) {
        log.info("토픽 {} 삭제", topicId);
        
        Topic topic = getTopicById(topicId);
        
        // 토픽에 속한 프레젠테이션들 먼저 삭제
        List<Presentation> presentations = presentationRepository.findByTopicId(topicId);
        for (Presentation presentation : presentations) {
            // 비디오 파일 삭제 (필요시)
            if (presentation.getVideoUrl() != null) {
                // TODO: 파일 삭제 로직 구현
                log.info("프레젠테이션 {} 비디오 파일 삭제 예정: {}", presentation.getId(), presentation.getVideoUrl());
            }
            presentationRepository.delete(presentation);
            log.info("프레젠테이션 {} 삭제 완료", presentation.getId());
        }
        
        // 토픽 삭제
        topicRepository.delete(topic);
        
        log.info("토픽 삭제 완료: {} (삭제된 프레젠테이션: {}개)", topicId, presentations.size());
    }
    
    // 특정 토픽의 프레젠테이션 목록 조회
    public List<Presentation> getPresentationsByTopicId(UUID topicId) {
        log.info("토픽 {}의 프레젠테이션 목록 조회", topicId);
        return presentationRepository.findByTopicId(topicId);
    }
    
    // 개인 토픽만 조회
    public List<Topic> getPrivateTopicsByUserId(UUID userId) {
        log.info("사용자 {}의 개인 토픽 목록 조회", userId);
        return topicRepository.findByUserIdAndTeamIsNullOrderByTitle(userId);
    }
    
    // 팀 토픽만 조회 (사용자가 속한 팀들)
    public List<Topic> getTeamTopicsByUserId(UUID userId) {
        log.info("사용자 {}의 팀 토픽 목록 조회", userId);
        return topicRepository.findTeamTopicsByUserId(userId);
    }
} 