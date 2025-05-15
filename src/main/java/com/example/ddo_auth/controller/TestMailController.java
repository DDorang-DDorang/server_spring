package com.example.ddo_auth.controller;

import com.example.ddo_auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class TestMailController {

    private final EmailService emailService;

    @GetMapping("/mail")
    public ResponseEntity<String> testMail(@RequestParam String to) {
        String dummyCode = "123456"; // 임시 인증 코드
        emailService.sendEmailCode(to, dummyCode);
        return ResponseEntity.ok("이메일 발송 완료");
    }
}

