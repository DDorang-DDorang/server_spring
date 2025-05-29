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