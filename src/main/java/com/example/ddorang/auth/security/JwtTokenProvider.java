package com.example.ddorang.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    private Key key;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private final CustomUserDetailsService userDetailsService;

    @PostConstruct
    protected void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String userEmail, UUID userId) {
        return createToken(userEmail, userId, accessTokenExpiration);
    }

    public String createRefreshToken(String userEmail, UUID userId) {
        return createToken(userEmail, userId, refreshTokenExpiration);
    }

    private String createToken(String userEmail, UUID userId, long validityInMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMillis);

        JwtBuilder builder = Jwts.builder()
                .setSubject(userEmail)
                .setIssuedAt(now)
                .setExpiration(expiry);
        
        // userId를 필수로 포함
        if (userId != null) {
            builder.claim("userId", userId.toString());
        } else {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        
        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUserEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 이후 문자열 반환
        }
        return null;
    }

    public Authentication getAuthentication(String token) {
        try {
            log.info("JWT 토큰 인증 처리 시작");
            
            // JWT 토큰에서 이메일과 userId 추출
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();
            
            String email = claims.getSubject();
            String userIdStr = claims.get("userId", String.class);
            
            if (email == null) {
                throw new IllegalArgumentException("JWT 토큰에 이메일이 없습니다.");
            }
            
            if (userIdStr == null) {
                throw new IllegalArgumentException("JWT 토큰에 userId가 없습니다.");
            }
            
            UUID userId = UUID.fromString(userIdStr);

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // CustomUserDetails인지 확인
            if (!(userDetails instanceof com.example.ddorang.auth.security.CustomUserDetails)) {
                throw new IllegalStateException("CustomUserDetails가 아닙니다: " + userDetails.getClass().getName());
            }
            
            // userId가 일치하는지 확인
            com.example.ddorang.auth.security.CustomUserDetails customUserDetails = 
                (com.example.ddorang.auth.security.CustomUserDetails) userDetails;

            if (!userId.equals(customUserDetails.getUser().getUserId())) {
                throw new IllegalStateException("JWT 토큰의 userId와 사용자 정보의 userId가 일치하지 않습니다. " +
                    "JWT userId: " + userId + ", User userId: " + customUserDetails.getUser().getUserId());
            }

            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
            
            return auth;
            
        } catch (Exception e) {
            throw new RuntimeException("JWT 토큰 인증 처리 중 오류 발생: " + e.getMessage(), e);
        }
    }
}