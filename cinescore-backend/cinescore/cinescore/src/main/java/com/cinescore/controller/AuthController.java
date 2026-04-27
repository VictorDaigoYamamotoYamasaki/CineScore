package com.cinescore.controller;

import com.cinescore.dto.LoginRequestDTO;
import com.cinescore.dto.LoginResponseDTO;
import com.cinescore.dto.UserRequestDTO;
import com.cinescore.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponseDTO> register(@Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.status(201).body(authService.register(dto));
    }
}
