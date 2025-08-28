package com.example.ddorang.settings.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationSettingRequest {

    @NotNull(message = "알림 설정 값은 필수입니다.")
    private Boolean enabled;
}