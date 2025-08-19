package com.example.ddorang.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamCreateRequest {
    
    @NotBlank(message = "팀 이름은 필수입니다")
    @Size(max = 100, message = "팀 이름은 100자 이하여야 합니다")
    private String name;
}