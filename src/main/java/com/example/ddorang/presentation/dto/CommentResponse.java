package com.example.ddorang.presentation.dto;

import com.example.ddorang.presentation.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    
    private UUID id;
    private String content;
    private Integer timestamp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID presentationId;
    private UUID userId;
    private String userName;
    private UUID parentCommentId;
    private List<CommentResponse> replies;
    private long replyCount;
    
    // Entity에서 DTO로 변환하는 정적 메서드
    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .timestamp(comment.getTimestamp())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .presentationId(comment.getPresentation().getId())
                .userId(comment.getUser().getUserId())
                .userName(comment.getUser().getName())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .replies(comment.getReplies().stream()
                        .map(CommentResponse::from)
                        .collect(Collectors.toList()))
                .replyCount(comment.getReplies().size())
                .build();
    }
    
    // 대댓글 없이 변환하는 메서드 (무한 재귀 방지)
    public static CommentResponse fromWithoutReplies(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .timestamp(comment.getTimestamp())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .presentationId(comment.getPresentation().getId())
                .userId(comment.getUser().getUserId())
                .userName(comment.getUser().getName())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .replyCount(comment.getReplies().size())
                .build();
    }
}