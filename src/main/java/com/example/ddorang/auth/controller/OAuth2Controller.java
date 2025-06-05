package com.example.ddorang.auth.controller;

import com.example.ddorang.auth.entity.User;
import com.example.ddorang.auth.service.OAuth2UserService;
import com.example.ddorang.auth.service.TokenService;
import com.example.ddorang.common.ApiPaths;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping(ApiPaths.OAUTH)
public class OAuth2Controller {

    private final OAuth2AuthorizedClientService clientService;
    private final OAuth2UserService oauth2UserService;
    private final TokenService tokenService;

    private String bearer(HttpHeaders h) {
        String v = h.getFirst(HttpHeaders.AUTHORIZATION);
        if (v == null || !v.startsWith("Bearer "))
            throw new IllegalArgumentException("Refresh-Token 헤더가 없거나 형식이 잘못됐습니다.");
        return v.substring(7);
    }

    public OAuth2Controller(OAuth2AuthorizedClientService clientService, 
                          OAuth2UserService oauth2UserService,
                          TokenService tokenService) {
        this.clientService = clientService;
        this.oauth2UserService = oauth2UserService;
        this.tokenService = tokenService;
    }

    @GetMapping("/login/success")
    public void loginSuccess(OAuth2AuthenticationToken authentication,
                             HttpServletResponse response) throws IOException {
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName());

        User savedUser = oauth2UserService.processOAuth2User(authentication.getPrincipal());

        // 리프레시 토큰 관련 로그 추가
        System.out.println("=== OAuth2 로그인 성공 ===");
        System.out.println("사용자 이메일: " + savedUser.getEmail());
        System.out.println("액세스 토큰 존재: " + (client.getAccessToken() != null));
        System.out.println("리프레시 토큰 존재: " + (client.getRefreshToken() != null));
        
        if (client.getRefreshToken() != null) {
            System.out.println("리프레시 토큰 값: " + client.getRefreshToken().getTokenValue().substring(0, 20) + "...");
            tokenService.saveRefreshToken(client.getRefreshToken().getTokenValue(), savedUser.getEmail());
            System.out.println("리프레시 토큰 저장 완료");
        } else {
            System.out.println("경고: 리프레시 토큰이 없습니다!");
        }

        String accessToken = client.getAccessToken().getTokenValue();
        System.out.println("액세스 토큰: " + accessToken.substring(0, 20) + "...");

        // 사용자 정보도 함께 전달
        String redirectUrl = "http://localhost:3000/oauth2/callback/google" +
                "?token=" + accessToken +
                "&email=" + java.net.URLEncoder.encode(savedUser.getEmail(), "UTF-8") +
                "&name=" + java.net.URLEncoder.encode(savedUser.getName(), "UTF-8");

        System.out.println("리다이렉트 URL: " + redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    @PostMapping(ApiPaths.TOKEN_REFRESH)
    public ResponseEntity<?> refresh(@RequestHeader HttpHeaders headers) {
        String refresh = bearer(headers);            // Bearer 추출
        try {
            String newAccess = tokenService.refreshAccessToken(refresh);
            return ResponseEntity.ok(Map.of("access_token", newAccess));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Refresh failed");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshTokenByEmail(@RequestParam String email) {
        System.out.println("=== 토큰 재발급 API 호출 ===");
        System.out.println("요청 이메일: " + email);
        
        try {
            String newAccessToken = tokenService.refreshAccessTokenByEmail(email);
            System.out.println("토큰 재발급 성공, 응답 전송");
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } catch (RuntimeException e) {
            System.out.println("토큰 재발급 실패: " + e.getMessage());
            
            if (e.getMessage().contains("No refresh token found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "refresh_token_not_found", "message", e.getMessage()));
            } else if (e.getMessage().contains("Failed to refresh token")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "refresh_token_expired", "message", "리프레시 토큰이 만료되었거나 유효하지 않습니다."));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "refresh_failed", "message", e.getMessage()));
            }
        }
    }

    @PostMapping(ApiPaths.TOKEN_LOGOUT)
    public ResponseEntity<Void> logout(@RequestHeader HttpHeaders headers) {
        tokenService.removeRefreshToken(bearer(headers));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token.");
        }

        String token = authHeader.substring(7);
        boolean isValid = tokenService.validateAccessTokenWithGoogle(token);
        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token.");
        }

        return ResponseEntity.ok("Token is valid");
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<?> withdrawUser(@RequestParam String email) {
        try {
            // 1. 리프레시 토큰 삭제
            tokenService.removeRefreshToken(email);
            
            // 2. 사용자 정보 삭제
            oauth2UserService.deleteUser(email);
            
            return ResponseEntity.ok().body(Map.of("message", "회원 탈퇴가 완료되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "회원 탈퇴 중 오류가 발생했습니다."));
        }
    }
} 