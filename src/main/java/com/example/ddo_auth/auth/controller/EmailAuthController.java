package com.example.ddo_auth.auth.controller;

import com.example.ddo_auth.auth.dto.EmailLoginRequest;
import com.example.ddo_auth.auth.dto.SignupRequest;
import com.example.ddo_auth.auth.dto.TokenResponse;
import com.example.ddo_auth.auth.service.AuthService;
import com.example.ddo_auth.auth.service.TokenService;
import com.example.ddo_auth.mail.service.EmailService;
import com.example.ddo_auth.mail.service.VerificationCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class EmailAuthController {

    private final AuthService authService;
    private final EmailService emailService;
    private final VerificationCodeService verificationCodeService;
    private final TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody EmailLoginRequest request) {
        TokenResponse tokens = authService.login(request);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Refresh token missing or malformed.");
        }

        String refreshToken = authorizationHeader.substring(7); // "Bearer " 이후 문자열 추출
        String newAccessToken = authService.reissueAccessToken(refreshToken);

        return ResponseEntity.ok(Map.of("access_token", newAccessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Refresh token missing or malformed.");
        }

        String refreshToken = authorizationHeader.substring(7);
        authService.logout(refreshToken);

        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    @PostMapping("/send-code")
    public ResponseEntity<Void> sendVerificationCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = verificationCodeService.createAndSaveCode(email);
        emailService.sendEmailCode(email, code);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");

        boolean result = verificationCodeService.verifyCode(email, code);
        if (!result) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("인증 실패");
        }

        return ResponseEntity.ok("인증 성공");
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody @Valid SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/reset-password/request")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = verificationCodeService.createAndSaveResetCode(email);
        emailService.sendEmailCode(email, code);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password/verify")
    public ResponseEntity<?> verifyResetCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");

        boolean verified = verificationCodeService.verifyResetCode(email, code);
        if (!verified) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("인증 실패");
        }
        return ResponseEntity.ok("인증 성공");
    }

    @PostMapping("/reset-password/confirm")
    public ResponseEntity<?> confirmNewPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String newPassword = body.get("newPassword");

        if (!verificationCodeService.isResetVerified(email)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이메일 인증이 필요합니다.");
        }

        authService.updatePassword(email, newPassword);
        return ResponseEntity.ok("비밀번호가 재설정되었습니다.");
    }


    @DeleteMapping("/withdraw")
    public ResponseEntity<?> withdrawUser(@RequestParam String email) {
        try {
            // 1. 리프레시 토큰 삭제
            tokenService.removeRefreshToken(email);

            // 2. 사용자 정보 삭제
            authService.deleteUser(email);

            return ResponseEntity.ok().body(Map.of("message", "회원 탈퇴가 완료되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "회원 탈퇴 중 오류가 발생했습니다."));
        }
    }
}
