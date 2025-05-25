package com.example.ddo_auth.mail.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmailCode(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(to);
            helper.setSubject("[또랑또랑] 이메일 인증 코드");

            String html = """
                <div style="font-family: Arial, sans-serif; padding: 20px; border: 1px solid #eee;">
                  <h2 style="color: #5A67D8;">또랑또랑 이메일 인증</h2>
                  <p>안녕하세요! 아래 인증 코드를 입력해 주세요:</p>
                  <div style="font-size: 24px; font-weight: bold; color: #2D3748; margin: 16px 0;">
                    %s
                  </div>
                  <p style="font-size: 12px; color: gray;">이 코드는 3분 뒤 만료됩니다.</p>
                </div>
                """.formatted(code);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("메일 전송 실패", e);
        }
    }

}
