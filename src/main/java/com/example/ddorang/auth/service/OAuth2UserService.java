package com.example.ddorang.auth.service;

import com.example.ddorang.auth.entity.User;
import com.example.ddorang.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User processOAuth2User(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createNewUser(email, name, picture));

        return user;
    }

    private User createNewUser(String email, String name, String picture) {
        // OAuth 사용자를 위한 임의의 강력한 password 생성
        String randomPassword = "OAUTH_" + UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
        String encodedPassword = passwordEncoder.encode(randomPassword);
        
        User newUser = User.builder()
                .email(email)
                .name(name)
                .password(encodedPassword) // 암호화된 임의 password 설정
                .provider(User.Provider.GOOGLE)
                .build();

        return userRepository.save(newUser);
    }

    @Transactional
    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 구글 OAuth 사용자인지 확인
        if (user.getProvider() != User.Provider.GOOGLE) {
            throw new RuntimeException("구글 OAuth 사용자가 아닙니다.");
        }

        userRepository.delete(user);
    }
} 