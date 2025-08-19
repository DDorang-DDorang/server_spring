package com.example.ddorang.common.util;

import com.example.ddorang.auth.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Spring Security 컨텍스트에서 인증된 사용자 정보를 가져오는 유틸리티 클래스
 */
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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }
        
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails)) {
            throw new IllegalStateException("올바르지 않은 인증 정보입니다.");
        }
        
        CustomUserDetails userDetails = (CustomUserDetails) principal;
        return userDetails.getUser().getUserId();
    }

    /**
     * 현재 인증된 사용자의 CustomUserDetails를 반환합니다.
     * 
     * @return CustomUserDetails 객체
     * @throws IllegalStateException 인증되지 않은 사용자이거나 인증 정보가 올바르지 않은 경우
     */
    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }
        
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails)) {
            throw new IllegalStateException("올바르지 않은 인증 정보입니다.");
        }
        
        return (CustomUserDetails) principal;
    }

    /**
     * 현재 인증된 사용자의 이메일을 반환합니다.
     * 
     * @return 인증된 사용자의 이메일
     * @throws IllegalStateException 인증되지 않은 사용자이거나 인증 정보가 올바르지 않은 경우
     */
    public static String getCurrentUserEmail() {
        return getCurrentUserDetails().getUser().getEmail();
    }

    /**
     * 현재 사용자가 인증되었는지 확인합니다.
     * 
     * @return 인증되었으면 true, 아니면 false
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
                && authentication.getPrincipal() instanceof CustomUserDetails;
    }
}