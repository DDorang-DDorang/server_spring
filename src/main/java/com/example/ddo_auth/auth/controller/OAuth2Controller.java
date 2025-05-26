package com.example.ddo_auth.auth.controller;

import com.example.ddo_auth.auth.entity.User;
import com.example.ddo_auth.auth.service.OAuth2UserService;
import com.example.ddo_auth.auth.service.TokenService;
import com.example.ddo_auth.common.ApiPaths;
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

        if (client.getRefreshToken() != null) {
            tokenService.saveRefreshToken(client.getRefreshToken().getTokenValue(), savedUser.getEmail());
        }

        String accessToken = client.getAccessToken().getTokenValue();

        String redirectUrl = "http://localhost:3000/oauth2/callback/google" +
                "?token=" + accessToken;

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