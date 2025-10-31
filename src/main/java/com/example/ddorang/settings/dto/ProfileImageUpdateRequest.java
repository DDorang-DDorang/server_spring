package com.example.ddorang.settings.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileImageUpdateRequest {

    @NotBlank(message = "프로필 이미지 URL은 필수입니다.")
    private String profileImage;
}