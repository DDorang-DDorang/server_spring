package com.example.ddorang.auth.dto;

import java.util.UUID;

public class UserInfoResponse {
    private UUID userId;
    private String email;
    private String name;
    private String provider;
    
    public UserInfoResponse(UUID userId, String email, String name, String provider) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.provider = provider;
    }
    
    // getters and setters
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
} 