package com.example.ddorang.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresentationCreateRequest {
    
    @NotBlank(message = "프레젠테이션 제목은 필수입니다.")
    @Size(max = 255, message = "프레젠테이션 제목은 255자를 초과할 수 없습니다.")
    private String title;
    
    @Size(max = 1000, message = "프레젠테이션 설명은 1000자를 초과할 수 없습니다.")
    private String description;
    
    @NotBlank(message = "프레젠테이션 타입은 필수입니다.")
    private String type; // "recording" 또는 "upload"
    
    private String originalFileName;
    
    private Integer duration; // 비디오 길이 (초)
} 