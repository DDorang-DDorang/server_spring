package com.example.ddorang.settings.service;

import com.example.ddorang.auth.entity.User;
import com.example.ddorang.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SettingsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 프로필 이미지 수정 (LOCAL + GOOGLE 둘 다 가능)
    public void updateProfileImage(String email, String profileImage) {
        User user = getUserByEmail(email);
        user.setProfileImage(profileImage);
        userRepository.save(user);
    }

    // 이름 수정 (LOCAL만 가능)
    public void updateName(String email, String name) {
        User user = getUserByEmail(email);
        
        // LOCAL 사용자만 이름 수정 가능
        if (user.getProvider() != User.Provider.LOCAL) {
            throw new IllegalArgumentException("OAuth 사용자는 이름을 수정할 수 없습니다. 연동된 계정에서 수정해주세요.");
        }
        
        user.setName(name);
        userRepository.save(user);
    }

    // 비밀번호 변경 (LOCAL만 가능)
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = getUserByEmail(email);
        
        // LOCAL 사용자만 비밀번호 변경 가능
        if (user.getProvider() != User.Provider.LOCAL) {
            throw new IllegalArgumentException("OAuth 사용자는 비밀번호를 변경할 수 없습니다. 연동된 계정에서 수정해주세요.");
        }
        
        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        
        // 새 비밀번호와 현재 비밀번호가 같은지 확인
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }
        
        // 새 비밀번호 암호화 후 저장
        String encodedNewPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedNewPassword);
        userRepository.save(user);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }
}