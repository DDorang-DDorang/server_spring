package com.example.ddorang.settings.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountDeleteRequest {

    // LOCAL 사용자만 비밀번호 필요, GOOGLE 사용자는 null 가능
    private String password;
}