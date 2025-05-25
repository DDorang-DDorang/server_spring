package com.example.ddo_auth.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenRepository implements RefreshTokenRepository {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    private static final String KEY_PREFIX = "refreshToken:";

    @Override
    public void save(String email, String token) {
        String key = KEY_PREFIX + email;
        redisTemplate.opsForValue().set(key, token, refreshTokenExpiration, TimeUnit.MILLISECONDS);
    }

    @Override
    public Optional<String> findByEmail(String email) {
        String key = KEY_PREFIX + email;
        String token = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(token);
    }

    @Override
    public void deleteByEmail(String email) {
        String key = KEY_PREFIX + email;
        redisTemplate.delete(key);
    }

    @Override
    public boolean existsByEmailAndToken(String email, String token) {
        return findByEmail(email)
                .map(storedToken -> storedToken.equals(token))
                .orElse(false);
    }

    @Override
    public boolean existsByEmail(String email) {
        String key = KEY_PREFIX + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}

