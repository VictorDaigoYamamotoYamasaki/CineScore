package com.cinescore.controller;

import com.cinescore.dto.ForgotPasswordDTO;
import com.cinescore.dto.LoginRequestDTO;
import com.cinescore.dto.ResetPasswordDTO;
import com.cinescore.dto.LoginResponseDTO;
import com.cinescore.dto.UserRequestDTO;
import com.cinescore.service.AuthService;
import com.cinescore.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService         authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponseDTO> register(@Valid @RequestBody UserRequestDTO dto) {
        return ResponseEntity.status(201).body(authService.register(dto));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> esqueceuSenha(@Valid @RequestBody ForgotPasswordDTO dto) {
        passwordResetService.solicitarResetSenha(dto.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody ResetPasswordDTO dto) {
        passwordResetService.redefinirSenha(dto);
        return ResponseEntity.ok().build();
    }
}