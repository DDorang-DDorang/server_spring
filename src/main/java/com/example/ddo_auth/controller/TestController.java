package com.example.ddo_auth.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/secure")
    public ResponseEntity<String> secure() {
        return ResponseEntity.ok("인증 성공! JWT 필터 정상 작동 중");
    }
}
