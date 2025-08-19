package com.example.ddorang.team.dto;

import com.example.ddorang.team.entity.Team;
import com.example.ddorang.team.entity.TeamMember;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class TeamResponse {
    
    private UUID id;
    private String name;
    private LocalDateTime createdAt;
    private int memberCount;
    private List<TeamMemberResponse> members;
    private String userRole;
    
    public static TeamResponse from(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .createdAt(team.getCreatedAt())
                .memberCount(team.getMembers() != null ? team.getMembers().size() : 0)
                .build();
    }
    
    public static TeamResponse from(Team team, TeamMember.Role userRole) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .createdAt(team.getCreatedAt())
                .memberCount(team.getMembers() != null ? team.getMembers().size() : 0)
                .userRole(userRole != null ? userRole.name() : null)
                .build();
    }
    
    public static TeamResponse fromWithMembers(Team team, List<TeamMemberResponse> members, TeamMember.Role userRole) {
        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .createdAt(team.getCreatedAt())
                .memberCount(members.size())
                .members(members)
                .userRole(userRole != null ? userRole.name() : null)
                .build();
    }
}