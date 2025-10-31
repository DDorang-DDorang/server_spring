package com.example.ddorang.presentation.dto;

import com.example.ddorang.presentation.entity.Topic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicResponse {
    private UUID id;
    private String title;
    private UUID userId;
    private UUID teamId;
    private Boolean isTeamTopic;
    private Long presentationCount;
    private String createdAt;
    
    public static TopicResponse from(Topic topic, Long presentationCount, Boolean isTeamTopic) {
        return TopicResponse.builder()
                .id(topic.getId())
                .title(topic.getTitle())
                .userId(topic.getUser() != null ? topic.getUser().getUserId() : null)
                .teamId(topic.getTeam() != null ? topic.getTeam().getId() : null)  // 팀 ID 설정
                .isTeamTopic(isTeamTopic)
                .presentationCount(presentationCount)
                .createdAt(topic.getId().toString()) // 임시로 ID를 사용, 실제로는 createdAt 필드 필요
                .build();
    }
} 