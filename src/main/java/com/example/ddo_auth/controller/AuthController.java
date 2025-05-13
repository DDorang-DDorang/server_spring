package com.example.ddo_auth.controller;

import com.example.ddo_auth.dto.LoginRequest;
import com.example.ddo_auth.dto.TokenResponse;
import com.example.ddo_auth.service.AuthService;
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

}
