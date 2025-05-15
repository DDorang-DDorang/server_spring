package com.example.ddo_auth.service;

import com.example.ddo_auth.domain.User;
import com.example.ddo_auth.dto.LoginRequest;
import com.example.ddo_auth.dto.SignupRequest;
import com.example.ddo_auth.dto.TokenResponse;
import com.example.ddo_auth.jwt.JwtTokenProvider;
import com.example.ddo_auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final VerificationCodeService verificationCodeService;

    public void signup(SignupRequest request) {
        String email = request.getEmail();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        if (!verificationCodeService.isEmailVerified(email)) {
            throw new IllegalArgumentException("이메일 인증이 필요합니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .email(email)
                .password(encodedPassword)
                .name(request.getName())
                .provider(User.Provider.LOCAL)
                .build();

        userRepository.save(user);
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());
        refreshTokenService.save(user.getEmail(), refreshToken);

        return new TokenResponse(accessToken, refreshToken);
    }

    public String reissueAccessToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 리프레시 토큰입니다.");
        }

        String email = jwtTokenProvider.getUserEmailFromToken(refreshToken);

        // Redis에서 저장된 토큰과 일치 여부 확인
        String storedToken = refreshTokenService.getRefreshToken(email);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 리프레시 토큰입니다.");
        }

        return jwtTokenProvider.createAccessToken(email);
    }

    public void logout(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 리프레시 토큰입니다.");
        }

        String email = jwtTokenProvider.getUserEmailFromToken(refreshToken);
        refreshTokenService.deleteRefreshToken(email);
    }

    //비밀번호 재설정
    public void updatePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
