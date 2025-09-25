package com.example.ddorang.auth.security;

import com.example.ddorang.auth.entity.User;
import com.example.ddorang.auth.repository.UserRepository;
import com.example.ddorang.team.entity.TeamMember;
import com.example.ddorang.team.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;

    //유저 조회
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("해당 이메일을 가진 사용자를 찾을 수 없습니다."));
            
            // 사용자의 팀 멤버십 정보도 함께 로드
            List<TeamMember> teamMemberships = null;
            try {
                teamMemberships = teamMemberRepository.findByUserOrderByJoinedAtDesc(user);
                
                if (teamMemberships != null && !teamMemberships.isEmpty()) {
                    teamMemberships.forEach(tm -> {
                        log.info("팀 멤버십: 팀 {} ({}), 역할: {}",
                            tm.getTeam().getName(), tm.getTeam().getId(), tm.getRole());
                    });
                }
            } catch (Exception e) {
                teamMemberships = new ArrayList<>(); // 빈 리스트로 설정
            }
            
            CustomUserDetails userDetails = new CustomUserDetails(user, teamMemberships);
            
            return userDetails;
        } catch (Exception e) {
            log.error("사용자 정보 로드 중 오류 발생: {}", e.getMessage(), e);
            throw new UsernameNotFoundException("사용자 정보를 로드할 수 없습니다: " + e.getMessage());
        }
    }
}
