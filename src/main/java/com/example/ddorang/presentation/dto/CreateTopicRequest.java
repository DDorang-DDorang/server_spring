package com.example.ddorang.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTopicRequest {
    private String title;
    private String userId; // UUID 또는 email String을 받을 수 있도록 String 타입 사용
} 