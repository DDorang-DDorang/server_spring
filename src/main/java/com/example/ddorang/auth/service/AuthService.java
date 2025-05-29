package com.example.ddorang.auth.service;

import com.example.ddorang.auth.entity.User;
import com.example.ddorang.auth.dto.EmailLoginRequest;
import com.example.ddorang.auth.dto.SignupRequest;
import com.example.ddorang.auth.dto.TokenResponse;
import com.example.ddorang.auth.security.JwtTokenProvider;
import com.example.ddorang.mail.service.EmailService;
import com.example.ddorang.mail.service.VerificationCodeService;
import com.example.ddorang.auth.repository.UserRepository;
import com.example.ddorang.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationCodeService verificationCodeService;
    private final EmailService emailService;


    //회원가입 시 중복회원이면 이메일 전송 x
    public void requestSignupCode(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        String code = verificationCodeService.createAndSaveCode(email);
        emailService.sendEmailCode(email, code);
    }

    //비밀번호 재설정 시 존재하지 않는 이메일이면 코드 전송x
    public void requestResetCode(String email) {
        if (userRepository.findByEmail(email).isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 이메일입니다.");
        }
        String code = verificationCodeService.createAndSaveResetCode(email);
        emailService.sendEmailCode(email, code);
    }


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

    public TokenResponse login(EmailLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());
        refreshTokenRepository.save(user.getEmail(), refreshToken);

        return new TokenResponse(accessToken, refreshToken);
    }

    public String reissueAccessToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 리프레시 토큰입니다.");
        }

        String email = jwtTokenProvider.getUserEmailFromToken(refreshToken);

        // Redis에서 저장된 토큰과 일치 여부 확인
        String storedToken = refreshTokenRepository.findByEmail(email)
                .orElse(null); // getRefreshToken → findByEmail().orElse(null)

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
        refreshTokenRepository.deleteByEmail(email); // deleteRefreshToken → deleteByEmail
    }

    //비밀번호 재설정
    public void updatePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void deleteUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        // 일반 회원인지 확인
        if (user.getProvider() != User.Provider.LOCAL) {
            throw new IllegalArgumentException("일반 회원이 아닙니다.");
        }

        // 사용자 삭제
        userRepository.delete(user);

        // 리프레시 토큰 삭제
        refreshTokenRepository.deleteByEmail(email);
    }
}