package com.example.ddorang.auth.service;

import com.example.ddorang.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TokenService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String tokenUri;

    private final RestTemplate restTemplate = new RestTemplate();
    private final RefreshTokenRepository refreshTokenRepository;

    public void saveRefreshToken(String refreshToken, String email) {
        refreshTokenRepository.save(email, refreshToken);
        refreshTokenRepository.saveMapping(refreshToken, email);
    }

    public void removeRefreshToken(String refreshToken) {
        String email = refreshTokenRepository.findEmailByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Unknown refresh token"));

        refreshTokenRepository.deleteByEmail(email);
        refreshTokenRepository.deleteByToken(refreshToken);
    }

    public String refreshAccessToken(String refreshToken) {
        String email = refreshTokenRepository.findEmailByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id",     clientId);
        params.add("client_secret", clientSecret);
        params.add("grant_type",    "refresh_token");
        params.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(params, headers);
        ResponseEntity<Map> resp = restTemplate.postForEntity(tokenUri, req, Map.class);

        if (resp.getStatusCode() != HttpStatus.OK)
            throw new RuntimeException("Failed to refresh token: " + resp.getStatusCode());

        Map body = resp.getBody();
        String newAT  = (String) body.get("access_token");
        String newRT  = (String) body.get("refresh_token");

        if (newRT != null) {
            saveRefreshToken(newRT, email);
        }
        return newAT;
    }

    public String refreshAccessTokenByEmail(String email) {
        System.out.println("=== 이메일로 토큰 재발급 시도 ===");
        System.out.println("요청된 이메일: " + email);
        
        String refreshToken = refreshTokenRepository.findByEmail(email)
                .orElseThrow(() -> {
                    System.out.println("에러: 이메일 " + email + "에 대한 리프레시 토큰을 찾을 수 없습니다.");
                    return new RuntimeException("No refresh token found for email: " + email);
                });

        System.out.println("리프레시 토큰 발견: " + refreshToken.substring(0, 20) + "...");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id",     clientId);
        params.add("client_secret", clientSecret);
        params.add("grant_type",    "refresh_token");
        params.add("refresh_token", refreshToken);

        System.out.println("Google OAuth2 토큰 재발급 요청 중...");
        System.out.println("Client ID: " + clientId);
        System.out.println("Token URI: " + tokenUri);

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(params, headers);
        ResponseEntity<Map> resp = restTemplate.postForEntity(tokenUri, req, Map.class);

        System.out.println("Google 응답 상태: " + resp.getStatusCode());

        if (resp.getStatusCode() != HttpStatus.OK) {
            System.out.println("에러: Google 토큰 재발급 실패 - " + resp.getStatusCode());
            throw new RuntimeException("Failed to refresh token: " + resp.getStatusCode());
        }

        Map body = resp.getBody();
        System.out.println("Google 응답 본문: " + body);
        
        String newAT  = (String) body.get("access_token");
        String newRT  = (String) body.get("refresh_token");

        System.out.println("새 액세스 토큰: " + (newAT != null ? newAT.substring(0, 20) + "..." : "null"));
        System.out.println("새 리프레시 토큰: " + (newRT != null ? newRT.substring(0, 20) + "..." : "없음"));

        // 새 리프레시 토큰이 있으면 저장
        if (newRT != null) {
            System.out.println("새 리프레시 토큰 저장 중...");
            saveRefreshToken(newRT, email);
            System.out.println("새 리프레시 토큰 저장 완료");
        }
        
        System.out.println("토큰 재발급 성공");
        return newAT;
    }

    public boolean validateAccessTokenWithGoogle(String accessToken) {
        String googleTokenInfoUrl = "https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=" + accessToken;

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(googleTokenInfoUrl, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                String aud = (String) body.get("aud");
                return clientId.equals(aud);
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}