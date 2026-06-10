package com.cinescore.service;

import com.cinescore.dto.ResetPasswordDTO;
import com.cinescore.exception.InvalidInputException;
import com.cinescore.model.PasswordResetToken;
import com.cinescore.model.User;
import com.cinescore.repository.PasswordResetTokenRepository;
import com.cinescore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService - Testes Unitários")
class PasswordResetServiceTest {

    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private UserRepository               userRepository;
    @Mock private EmailService                 emailService;
    @Mock private PasswordEncoder              passwordEncoder;

    @InjectMocks private PasswordResetService passwordResetService;

    private static final String USER_ID = "uuid-beatriz-001";

    private User              usuarioMock;
    private PasswordResetToken tokenValido;
    private PasswordResetToken tokenExpirado;
    private PasswordResetToken tokenUsado;

    @BeforeEach
    void configurar() {
        usuarioMock = User.builder()
                .id(USER_ID).name("Beatriz").email("beatriz@test.com")
                .passwordHash("hashed").role("USER").build();

        tokenValido = PasswordResetToken.builder()
                .id("uuid-token-001").user(usuarioMock).token("token-valido-uuid")
                .expiresAt(LocalDateTime.now().plusHours(1)).used(false).build();

        tokenExpirado = PasswordResetToken.builder()
                .id("uuid-token-002").user(usuarioMock).token("token-expirado")
                .expiresAt(LocalDateTime.now().minusMinutes(1)).used(false).build();

        tokenUsado = PasswordResetToken.builder()
                .id("uuid-token-003").user(usuarioMock).token("token-usado")
                .expiresAt(LocalDateTime.now().plusHours(1)).used(true).build();
    }

    @Test
    @DisplayName("Deve enviar e-mail e salvar token quando e-mail existe")
    void deveSolicitarResetQuandoEmailExiste() {
        when(userRepository.findByEmail("beatriz@test.com")).thenReturn(Optional.of(usuarioMock));
        when(tokenRepository.saveAndFlush(any())).thenReturn(tokenValido);

        passwordResetService.solicitarResetSenha("beatriz@test.com");

        verify(tokenRepository).deleteByUserId(USER_ID);
        verify(tokenRepository).saveAndFlush(any(PasswordResetToken.class));
        verify(emailService).enviarEmailResetSenha(eq("beatriz@test.com"), anyString());
    }

    @Test
    @DisplayName("Deve retornar silenciosamente quando e-mail não existe (segurança)")
    void deveRetornarSilenciosamenteQuandoEmailNaoExiste() {
        when(userRepository.findByEmail("naoexiste@test.com")).thenReturn(Optional.empty());

        assertThatNoException()
                .isThrownBy(() -> passwordResetService.solicitarResetSenha("naoexiste@test.com"));

        verify(emailService, never()).enviarEmailResetSenha(any(), any());
        verify(tokenRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Deve redefinir senha com token válido")
    void deveRedefinirSenhaComTokenValido() {
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setToken("token-valido-uuid");
        dto.setNewPassword("NovaSenha@123");

        when(tokenRepository.findByToken("token-valido-uuid")).thenReturn(Optional.of(tokenValido));
        when(passwordEncoder.encode("NovaSenha@123")).thenReturn("novo-hash");
        when(userRepository.save(any())).thenReturn(usuarioMock);
        when(tokenRepository.save(any())).thenReturn(tokenValido);

        passwordResetService.redefinirSenha(dto);

        verify(passwordEncoder).encode("NovaSenha@123");
        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("novo-hash")));
        verify(tokenRepository).save(argThat(PasswordResetToken::isUsed));
    }

    @Test
    @DisplayName("Deve lançar InvalidInputException com token inexistente")
    void deveLancarExcecaoComTokenInvalido() {
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setToken("token-nao-existe");
        dto.setNewPassword("Senha@123");

        when(tokenRepository.findByToken("token-nao-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.redefinirSenha(dto))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("inválido");
    }

    @Test
    @DisplayName("Deve lançar InvalidInputException com token expirado")
    void deveLancarExcecaoComTokenExpirado() {
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setToken("token-expirado");
        dto.setNewPassword("Senha@123");

        when(tokenRepository.findByToken("token-expirado")).thenReturn(Optional.of(tokenExpirado));

        assertThatThrownBy(() -> passwordResetService.redefinirSenha(dto))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    @DisplayName("Deve lançar InvalidInputException com token já utilizado")
    void deveLancarExcecaoComTokenJaUsado() {
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setToken("token-usado");
        dto.setNewPassword("Senha@123");

        when(tokenRepository.findByToken("token-usado")).thenReturn(Optional.of(tokenUsado));

        assertThatThrownBy(() -> passwordResetService.redefinirSenha(dto))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("utilizado");
    }

    @Test
    @DisplayName("Deve invalidar tokens anteriores antes de criar o novo")
    void deveInvalidarTokensAnterioresAntesDeNovoCriar() {
        when(userRepository.findByEmail("beatriz@test.com")).thenReturn(Optional.of(usuarioMock));
        when(tokenRepository.saveAndFlush(any())).thenReturn(tokenValido);

        passwordResetService.solicitarResetSenha("beatriz@test.com");

        var inOrder = inOrder(tokenRepository);
        inOrder.verify(tokenRepository).deleteByUserId(USER_ID);
        inOrder.verify(tokenRepository).saveAndFlush(any());
    }

    @Test
    @DisplayName("Deve marcar token como usado após redefinição bem-sucedida")
    void deveMarcarTokenComoUsadoAposReset() {
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setToken("token-valido-uuid");
        dto.setNewPassword("NovaSenha@123");

        when(tokenRepository.findByToken("token-valido-uuid")).thenReturn(Optional.of(tokenValido));
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any())).thenReturn(usuarioMock);
        when(tokenRepository.save(any())).thenReturn(tokenValido);

        passwordResetService.redefinirSenha(dto);

        assertThat(tokenValido.isUsed()).isTrue();
    }
}
