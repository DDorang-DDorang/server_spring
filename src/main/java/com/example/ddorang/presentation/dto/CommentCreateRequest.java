package com.example.ddorang.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreateRequest {
    
    @NotBlank(message = "댓글 내용은 필수입니다.")
    private String content;
    
    @Min(value = 0, message = "동영상 시간은 0초 이상이어야 합니다.")
    private Integer timestamp;
    
    private UUID parentCommentId;
}