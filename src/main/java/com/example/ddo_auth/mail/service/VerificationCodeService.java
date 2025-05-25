package com.example.ddo_auth.mail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    private static final String PREFIX = "emailCode:";
    private static final Duration TTL = Duration.ofMinutes(3);
    private static final String VERIFIED_PREFIX = "verified:";
    private static final String RESET_PREFIX = "resetCode:";
    private static final String RESET_VERIFIED_PREFIX = "resetVerified:";

    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom random = new SecureRandom();

    public String createAndSaveCode(String email) {
        String code = "%06d".formatted(random.nextInt(1_000_000));
        redisTemplate.opsForValue().set(PREFIX + email, code, TTL);
        return code;
    }


    // 인증 성공 시 인증 여부 저장
    public boolean verifyCode(String email, String inputCode) {
        String key = PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);
        if (storedCode != null && storedCode.equals(inputCode)) {
            redisTemplate.delete(key);
            redisTemplate.opsForValue().set(VERIFIED_PREFIX + email, "true", Duration.ofMinutes(10));
            return true;
        }
        return false;
    }

    // 회원가입 전 인증 여부 확인
    public boolean isEmailVerified(String email) {
        String value = redisTemplate.opsForValue().get(VERIFIED_PREFIX + email);
        return "true".equals(value);
    }

    //비밀번호 재설정
    public String createAndSaveResetCode(String email) {
        String code = "%06d".formatted(random.nextInt(1_000_000));
        redisTemplate.opsForValue().set(RESET_PREFIX + email, code, Duration.ofMinutes(3));
        return code;
    }

    public boolean verifyResetCode(String email, String inputCode) {
        String stored = redisTemplate.opsForValue().get(RESET_PREFIX + email);
        if (stored != null && stored.equals(inputCode)) {
            redisTemplate.delete(RESET_PREFIX + email);
            redisTemplate.opsForValue().set(RESET_VERIFIED_PREFIX + email, "true", Duration.ofMinutes(10));
            return true;
        }
        return false;
    }

    public boolean isResetVerified(String email) {
        return "true".equals(redisTemplate.opsForValue().get(RESET_VERIFIED_PREFIX + email));
    }


}
