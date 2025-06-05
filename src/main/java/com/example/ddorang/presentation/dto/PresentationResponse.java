package com.example.ddorang.presentation.dto;

import com.example.ddorang.presentation.entity.Presentation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresentationResponse {
    
    private UUID id;
    private String title;
    private String script;
    private String videoUrl;
    private String goalTime;
    private LocalDateTime createdAt;
    private UUID topicId;
    private String topicTitle;
    
    // Entity에서 DTO로 변환하는 정적 메서드
    public static PresentationResponse from(Presentation presentation) {
        return PresentationResponse.builder()
                .id(presentation.getId ())
                .title(presentation.getTitle())
                .script(presentation.getScript())
                .videoUrl(presentation.getVideoUrl())
                .goalTime(presentation.getGoalTime())
                .createdAt(presentation.getCreatedAt())
                .topicId(presentation.getTopic().getId())
                .topicTitle(presentation.getTopic().getTitle())
                .build();
    }
} 