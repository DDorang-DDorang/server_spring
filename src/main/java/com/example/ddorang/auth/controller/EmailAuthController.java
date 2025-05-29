package com.example.ddorang.auth.controller;

import com.example.ddorang.auth.dto.EmailLoginRequest;
import com.example.ddorang.auth.dto.SignupRequest;
import com.example.ddorang.auth.dto.TokenResponse;
import com.example.ddorang.auth.service.AuthService;
import com.example.ddorang.auth.service.TokenService;
import com.example.ddorang.common.ApiPaths;
import com.example.ddorang.mail.service.VerificationCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.AUTH)
public class EmailAuthController {

    private final AuthService authService;
    private final VerificationCodeService verificationCodeService;
    private final TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody EmailLoginRequest request) {
        TokenResponse tokens = authService.login(request);
        return ResponseEntity.ok(tokens);
    }

    private String bearer(HttpHeaders h) {
        String v = h.getFirst(HttpHeaders.AUTHORIZATION);
        if (v == null || !v.startsWith("Bearer "))
            throw new IllegalArgumentException("Refresh-Token 헤더가 없거나 형식이 잘못됐습니다.");
        return v.substring(7);
    }

    @PostMapping(ApiPaths.TOKEN_REFRESH)
    public ResponseEntity<?> refresh(@RequestHeader HttpHeaders headers) {
        String rt = bearer(headers);
        String newAT = authService.reissueAccessToken(rt);
        return ResponseEntity.ok(Map.of("access_token", newAT));
    }

    @PostMapping(ApiPaths.TOKEN_LOGOUT)
    public ResponseEntity<?> logout(@RequestHeader HttpHeaders headers) {
        authService.logout(bearer(headers));
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }


    @PostMapping("/email/code/signup")
    public ResponseEntity<Void> sendVerificationCode(@RequestBody Map<String, String> body) {
        authService.requestSignupCode(body.get("email"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/code/signup/verify")
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

    @PostMapping("/email/code/reset")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody Map<String, String> body) {
        authService.requestResetCode(body.get("email"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/code/reset/verify")
    public ResponseEntity<?> verifyResetCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");

        boolean verified = verificationCodeService.verifyResetCode(email, code);
        if (!verified) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("인증 실패");
        }
        return ResponseEntity.ok("인증 성공");
    }

    @PatchMapping("/password/reset")
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
