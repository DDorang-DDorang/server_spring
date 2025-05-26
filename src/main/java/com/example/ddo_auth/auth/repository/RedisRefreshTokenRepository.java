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

    private static final String KEY_PREFIX     = "refreshToken:";   // email → RT
    private static final String REV_PREFIX     = "emailOf:";        // RT    → email

    @Override
    public void save(String email, String token) {
        redisTemplate.opsForValue()
                .set(KEY_PREFIX + email, token,
                        refreshTokenExpiration, TimeUnit.MILLISECONDS);
        saveMapping(token, email);
    }

    @Override
    public Optional<String> findByEmail(String email) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(KEY_PREFIX + email));
    }

    @Override
    public void deleteByEmail(String email) {
        findByEmail(email).ifPresent(this::deleteByToken);
        redisTemplate.delete(KEY_PREFIX + email);
    }

    @Override
    public void saveMapping(String token, String email) {
        redisTemplate.opsForValue()
                .set(REV_PREFIX + token, email,
                        refreshTokenExpiration, TimeUnit.MILLISECONDS);
    }

    @Override
    public void deleteByToken(String token) {
        redisTemplate.delete(REV_PREFIX + token);
    }

    @Override
    public Optional<String> findEmailByToken(String token) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(REV_PREFIX + token));
    }
}
