package com.example.ddorang.team.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateInviteRequest {
    // Redis 기반 초대 시스템으로 변경: 24시간 고정 TTL, 사용 횟수 제한 없음
}