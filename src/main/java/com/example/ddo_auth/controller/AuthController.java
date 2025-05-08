package com.example.ddo_auth.controller;

import com.example.ddo_auth.dto.SignupRequest;
import com.example.ddo_auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        userService.register(request);
        return ResponseEntity.ok("회원가입 완료");
    }
}

