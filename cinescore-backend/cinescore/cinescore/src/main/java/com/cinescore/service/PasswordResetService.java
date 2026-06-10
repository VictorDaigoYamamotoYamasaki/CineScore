package com.cinescore.service;

import com.cinescore.dto.ResetPasswordDTO;
import com.cinescore.exception.InvalidInputException;
import com.cinescore.model.PasswordResetToken;
import com.cinescore.model.User;
import com.cinescore.repository.PasswordResetTokenRepository;
import com.cinescore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int TOKEN_VALIDADE_HORAS = 1;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository               userRepository;
    private final EmailService                 emailService;
    private final PasswordEncoder              passwordEncoder;

    @Transactional
    public void solicitarResetSenha(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            invalidarTokensAnteriores(user.getId());
            String token = salvarNovoToken(user);
            emailService.enviarEmailResetSenha(email, token);
        });
    }

    @Transactional
    public void redefinirSenha(ResetPasswordDTO dto) {
        PasswordResetToken token = buscarTokenValidoOuLancar(dto.getToken());
        atualizarSenha(token.getUser(), dto.getNewPassword());
        marcarTokenComoUsado(token);
    }


    private void invalidarTokensAnteriores(String userId) {
        tokenRepository.deleteByUserId(userId);
    }

    private String salvarNovoToken(User user) {
        String tokenValue = UUID.randomUUID().toString();

        tokenRepository.saveAndFlush(PasswordResetToken.builder()
                .user(user)
                .token(tokenValue)
                .expiresAt(LocalDateTime.now().plusHours(TOKEN_VALIDADE_HORAS))
                .used(false)
                .build());

        return tokenValue;
    }

    private PasswordResetToken buscarTokenValidoOuLancar(String tokenValue) {
        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new InvalidInputException("token", "inválido ou inexistente"));

        if (token.isExpired()) {
            throw new InvalidInputException("token", "expirado — solicite um novo link");
        }
        if (token.isUsed()) {
            throw new InvalidInputException("token", "já foi utilizado");
        }

        return token;
    }

    private void atualizarSenha(User user, String novaSenha) {
        user.setPasswordHash(passwordEncoder.encode(novaSenha));
        userRepository.save(user);
    }

    private void marcarTokenComoUsado(PasswordResetToken token) {
        token.setUsed(true);
        tokenRepository.save(token);
    }
}
