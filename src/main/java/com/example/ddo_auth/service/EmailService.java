package com.example.ddo_auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmailCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[또랑또랑] 이메일 인증 코드");
        message.setText("""
               
                아래 인증 코드를 입력해 주세요:

                ✔ 인증코드: %s

                인증코드는 3분 뒤 만료됩니다.
                """.formatted(code));
        mailSender.send(message);
    }
}
