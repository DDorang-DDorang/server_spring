package com.example.ddorang.settings.controller;

import com.example.ddorang.auth.entity.User;
import com.example.ddorang.auth.security.JwtTokenProvider;
import com.example.ddorang.settings.dto.NameUpdateRequest;
import com.example.ddorang.settings.dto.PasswordChangeRequest;
import com.example.ddorang.settings.dto.ProfileImageUpdateRequest;
import com.example.ddorang.settings.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;
    private final JwtTokenProvider jwtTokenProvider;

    // 프로필 이미지 수정 (LOCAL + GOOGLE 둘 다 가능)
    @PatchMapping("/profile-image")
    public ResponseEntity<?> updateProfileImage(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Valid ProfileImageUpdateRequest request) {
        try {
            String email = extractEmailFromToken(authHeader);
            settingsService.updateProfileImage(email, request.getProfileImage());
            return ResponseEntity.ok(Map.of("message", "프로필 이미지가 수정되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "프로필 이미지 수정 중 오류가 발생했습니다."));
        }
    }

    // 이름 수정 (LOCAL만 가능)
    @PatchMapping("/name")
    public ResponseEntity<?> updateName(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Valid NameUpdateRequest request) {
        try {
            String email = extractEmailFromToken(authHeader);
            settingsService.updateName(email, request.getName());
            return ResponseEntity.ok(Map.of("message", "이름이 수정되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "이름 수정 중 오류가 발생했습니다."));
        }
    }

    // 비밀번호 변경 (LOCAL만 가능)
    @PatchMapping("/password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Valid PasswordChangeRequest request) {
        try {
            String email = extractEmailFromToken(authHeader);
            settingsService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "비밀번호 변경 중 오류가 발생했습니다."));
        }
    }

    private String extractEmailFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("인증 토큰이 필요합니다.");
        }

        String token = authHeader.substring(7);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        return jwtTokenProvider.getUserEmailFromToken(token);
    }
}