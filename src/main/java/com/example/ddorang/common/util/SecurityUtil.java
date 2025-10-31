package com.example.ddorang.common.util;

import com.example.ddorang.auth.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Spring Security 컨텍스트에서 인증된 사용자 정보를 가져오는 유틸리티 클래스
 */
@Slf4j
public final class SecurityUtil {

    private SecurityUtil() {
        // 유틸리티 클래스이므로 인스턴스 생성 방지
    }

    /**
     * 현재 인증된 사용자의 ID를 반환합니다.
     * 
     * @return 인증된 사용자의 UUID
     * @throws IllegalStateException 인증되지 않은 사용자이거나 인증 정보가 올바르지 않은 경우
     */
    public static UUID getCurrentUserId() {
        try {
            
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null) {
                throw new IllegalStateException("인증 정보가 null입니다.");
            }

            if (!authentication.isAuthenticated()) {
                throw new IllegalStateException("사용자가 인증되지 않았습니다.");
        }
        
        Object principal = authentication.getPrincipal();
            
            if (principal == null) {
                throw new IllegalStateException("Principal이 null입니다.");
            }
            
        if (!(principal instanceof CustomUserDetails)) {
                throw new IllegalStateException("Principal이 CustomUserDetails가 아닙니다. 실제 타입: " + principal.getClass().getName());
        }
        
        CustomUserDetails userDetails = (CustomUserDetails) principal;
            
            UUID userId = userDetails.getUser().getUserId();
            
            if (userId == null) {
                throw new IllegalStateException("사용자 ID가 null입니다.");
            }

            return userId;
        } catch (Exception e) {
            throw new IllegalStateException("사용자 ID를 가져오는 중 오류 발생: " + e.getMessage(), e);
        }
    }
}