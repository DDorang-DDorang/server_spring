package com.example.ddo_auth.service;

import com.example.ddo_auth.domain.RefreshToken;
import com.example.ddo_auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {


    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    public void save(String email, String token) {
        String key = "refreshToken:" + email;
        redisTemplate.opsForValue().set(key, token, refreshTokenExpiration, TimeUnit.MILLISECONDS);
    }

// 토큰 탈취 위험을 방지하려 사용
//    public void update(String email, String newToken) {
//        RefreshToken refreshToken = refreshTokenRepository.findById(email)
//                .orElseThrow(() -> new IllegalArgumentException("리프레시 토큰이 존재하지 않습니다."));
//        refreshToken.updateToken(newToken);
//        refreshTokenRepository.save(refreshToken);
//    }


    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get("refreshToken:" + email);
    }

    public void deleteRefreshToken(String email) {
        redisTemplate.delete("refreshToken:" + email);
    }
}
