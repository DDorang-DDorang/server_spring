package com.example.ddorang.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.Authentication;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = jwtTokenProvider.resolveToken(request);
            
            if (token != null) {
                
                if (jwtTokenProvider.validateToken(token)) {
                    
                    try {
                        Authentication auth = jwtTokenProvider.getAuthentication(token);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        
                        // Principal이 CustomUserDetails인지 확인
                        if (auth.getPrincipal() instanceof com.example.ddorang.auth.security.CustomUserDetails) {
                            com.example.ddorang.auth.security.CustomUserDetails userDetails = 
                                (com.example.ddorang.auth.security.CustomUserDetails) auth.getPrincipal();
                        } else {
                            log.warn("Principal이 CustomUserDetails가 아님: {}", auth.getPrincipal().getClass().getName());
                        }
                    } catch (Exception e) {
                        log.error("JWT 인증 처리 실패: {}", e.getMessage(), e);
                        // 인증 실패 시에도 필터 체인은 계속 진행
                    }
                } else {
                    log.warn("JWT 토큰 유효성 검증 실패");
                }
            } else {
                log.debug("JWT 토큰 없음");
            }
        } catch (Exception e) {
            log.error("JWT 필터 처리 중 오류 발생: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}
