package com.example.ddo_auth.controller;

import com.example.ddo_auth.dto.LoginRequest;
import com.example.ddo_auth.dto.SignupRequest;
import com.example.ddo_auth.dto.TokenResponse;
import com.example.ddo_auth.service.AuthService;
import com.example.ddo_auth.service.EmailService;
import com.example.ddo_auth.service.VerificationCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;
    private final VerificationCodeService verificationCodeService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
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
}
