package com.example.ddorang.auth.repository;

import java.util.Optional;

public interface RefreshTokenRepository {
    void save(String email, String token);
    Optional<String> findByEmail(String email);
    void deleteByEmail(String email);
    void     saveMapping(String token, String email);   // RT → email
    void     deleteByToken(String token);
    Optional<String> findEmailByToken(String token);
}

