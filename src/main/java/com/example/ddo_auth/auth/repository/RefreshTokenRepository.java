package com.example.ddo_auth.auth.repository;

import java.util.Optional;

public interface RefreshTokenRepository {
    void save(String email, String token);
    Optional<String> findByEmail(String email);
    void deleteByEmail(String email);
    boolean existsByEmailAndToken(String email, String token);
    boolean existsByEmail(String email);
}

