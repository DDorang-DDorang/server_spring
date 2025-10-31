package com.example.ddorang.auth.security;

import com.example.ddorang.auth.entity.User;
import com.example.ddorang.team.entity.TeamMember;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final List<TeamMember> teamMemberships; // 팀 멤버십 정보 추가

    public CustomUserDetails(User user, List<TeamMember> teamMemberships) {
        this.user = user;
        this.teamMemberships = teamMemberships;
    }

    // 기존 생성자와의 호환성을 위한 오버로드
    public CustomUserDetails(User user) {
        this.user = user;
        this.teamMemberships = null; // 팀 멤버십 정보가 없는 경우
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(() -> "ROLE_USER");
    }

    @Override
    public String getPassword() {
        return user.getPassword(); // 암호화된 비밀번호
    }

    @Override
    public String getUsername() {
        return user.getEmail(); // 식별자
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // 특정 팀의 멤버인지 확인하는 헬퍼 메서드
    public boolean isMemberOfTeam(UUID teamId) {
        if (teamMemberships == null || teamMemberships.isEmpty()) {
            return false;
        }
        
        try {
            return teamMemberships.stream()
                    .anyMatch(tm -> tm != null && tm.getTeam() != null && tm.getTeam().getId() != null && 
                                   tm.getTeam().getId().equals(teamId));
        } catch (Exception e) {
            // 예외 발생 시 false 반환
            return false;
        }
    }
}
