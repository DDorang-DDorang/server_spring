package com.example.ddorang.team.dto;

import com.example.ddorang.team.entity.TeamMember;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TeamMemberResponse {
    
    private UUID id;
    private UUID userId;
    private String userName;
    private String userEmail;
    private String role;
    private LocalDateTime joinedAt;
    
    public static TeamMemberResponse from(TeamMember teamMember) {
        return TeamMemberResponse.builder()
                .id(teamMember.getId())
                .userId(teamMember.getUser().getUserId())
                .userName(teamMember.getUser().getName())
                .userEmail(teamMember.getUser().getEmail())
                .role(teamMember.getRole().name())
                .joinedAt(teamMember.getJoinedAt())
                .build();
    }
}