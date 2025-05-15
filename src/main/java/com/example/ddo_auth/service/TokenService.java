package com.example.ddo_auth.service;

import jakarta.servlet.http.HttpServletResponse;
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
    private final RefreshTokenService refreshTokenService;

    public void saveRefreshToken(String refreshToken, String email) {
        refreshTokenService.save(email, refreshToken);
    }

    public void removeRefreshToken(String email) {
        refreshTokenService.deleteRefreshToken(email);
    }

    public String refreshAccessToken(String refreshToken, String email) {
        if (!refreshTokenService.validateRefreshToken(email, refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("grant_type", "refresh_token");
        params.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUri, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            Map body = response.getBody();
            String newAccessToken = (String) body.get("access_token");
            String newRefreshToken = (String) body.get("refresh_token");

            // 새로운 리프레시 토큰이 있다면 Redis 업데이트
            if (newRefreshToken != null) {
                refreshTokenService.save(email, newRefreshToken);
            }

            return newAccessToken;
        } else {
            throw new RuntimeException("Failed to refresh token: " + response.getStatusCode());
        }
    }

    public boolean validateAccessTokenWithGoogle(String accessToken) {
        String googleTokenInfoUrl = "https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=" + accessToken;

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(googleTokenInfoUrl, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> body = response.getBody();
                String aud = (String) body.get("aud");

                // 발급받은 Google Client ID와 일치하는지 확인
                return clientId.equals(aud);
            }
        } catch (Exception e) {
            // 유효하지 않거나 만료되었을 경우 예외 발생
            return false;
        }

        return false;
    }
}
