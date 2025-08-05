package com.example.ddorang.team.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InviteService {

    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String INVITE_KEY_PREFIX = "team:invite:";
    private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration INVITE_TTL = Duration.ofHours(24);

    public String createInvite(UUID teamId) {
        String inviteCode = generateUniqueInviteCode(teamId);
        
        log.info("초대 코드 생성: {} -> 팀 ID: {}, TTL: {}시간", inviteCode, teamId, INVITE_TTL.toHours());
        return inviteCode;
    }

    public UUID getTeamIdByInviteCode(String inviteCode) {
        // 입력 검증
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            throw new IllegalArgumentException("초대 코드가 필요합니다");
        }
        
        if (inviteCode.length() != 8) {
            throw new IllegalArgumentException("잘못된 초대 코드 형식입니다");
        }
        
        String key = INVITE_KEY_PREFIX + inviteCode;
        String teamIdStr = redisTemplate.opsForValue().get(key);
        
        if (teamIdStr == null) {
            log.warn("유효하지 않거나 만료된 초대 코드: {}", inviteCode);
            return null;
        }
        
        try {
            return UUID.fromString(teamIdStr);
        } catch (IllegalArgumentException e) {
            log.error("잘못된 팀 ID 형식 in Redis - 초대 코드: {}, 값: {}", inviteCode, teamIdStr);
            deleteInvite(inviteCode);
            return null;
        }
    }

    public boolean isValidInvite(String inviteCode) {
        String key = INVITE_KEY_PREFIX + inviteCode;
        return redisTemplate.hasKey(key);
    }

    public void deleteInvite(String inviteCode) {
        String key = INVITE_KEY_PREFIX + inviteCode;
        Boolean deleted = redisTemplate.delete(key);
        
        if (Boolean.TRUE.equals(deleted)) {
            log.info("초대 코드 삭제: {}", inviteCode);
        } else {
            log.warn("존재하지 않는 초대 코드 삭제 시도: {}", inviteCode);
        }
    }

    public Duration getRemainingTTL(String inviteCode) {
        String key = INVITE_KEY_PREFIX + inviteCode;
        Long ttlSeconds = redisTemplate.getExpire(key);
        
        if (ttlSeconds == null || ttlSeconds <= 0) {
            return Duration.ZERO;
        }
        
        return Duration.ofSeconds(ttlSeconds);
    }

    private String generateUniqueInviteCode(UUID teamId) {
        int maxRetries = 10;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            String inviteCode = generateInviteCode();
            String key = INVITE_KEY_PREFIX + inviteCode;
            
            // 바로 실제 값으로 설정 - 1번의 원자적 연산으로 끝!
            Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, teamId.toString(), INVITE_TTL);
                
            if (Boolean.TRUE.equals(success)) {
                log.debug("유니크한 초대 코드 생성 성공: {} (시도 횟수: {})", inviteCode, attempt + 1);
                return inviteCode; // 성공! 실제 키도 이미 설정됨
            }
            
            log.debug("초대 코드 중복 발생: {} (시도 횟수: {})", inviteCode, attempt + 1);
            // 실패하면 다른 코드로 재시도
        }
        
        throw new RuntimeException("초대 코드 생성 실패: 최대 재시도 횟수(" + maxRetries + ") 초과");
    }

    private String generateInviteCode() {
        StringBuilder code = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            code.append(INVITE_CODE_CHARS.charAt(RANDOM.nextInt(INVITE_CODE_CHARS.length())));
        }
        return code.toString();
    }
}