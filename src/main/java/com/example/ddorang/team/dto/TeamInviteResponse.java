package com.example.ddorang.team.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeamInviteResponse {
    
    private String inviteCode;
    private String inviteUrl;
    private LocalDateTime expiresAt;
    private String teamName;
}