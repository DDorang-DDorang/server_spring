package com.example.ddorang.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTopicRequest {
    private String title;
    private String userId; // UUID 또는 email String을 받을 수 있도록 String 타입 사용
    private UUID teamId;   // 팀 토픽 생성 시 사용 (optional)
} 