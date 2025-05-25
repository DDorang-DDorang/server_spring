package com.example.ddo_auth.auth.service;

import com.example.ddo_auth.auth.repository.RefreshTokenRepository;
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
    private final RefreshTokenRepository refreshTokenRepository; // 인터페이스 사용

    public void saveRefreshToken(String refreshToken, String email) {
        refreshTokenRepository.save(email, refreshToken);
    }

    public void removeRefreshToken(String email) {
        refreshTokenRepository.deleteByEmail(email);
    }

    public String refreshAccessToken(String refreshToken, String email) {
        if (!refreshTokenRepository.existsByEmailAndToken(email, refreshToken)) {
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

            // 새로운 리프레시 토큰이 있다면 저장
            if (newRefreshToken != null) {
                refreshTokenRepository.save(email, newRefreshToken);
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
                return clientId.equals(aud);
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}