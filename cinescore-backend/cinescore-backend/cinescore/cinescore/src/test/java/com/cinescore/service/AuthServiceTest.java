package com.cinescore.service;

import com.cinescore.dto.LoginRequestDTO;
import com.cinescore.dto.LoginResponseDTO;
import com.cinescore.dto.UserRequestDTO;
import com.cinescore.exception.DuplicateResourceException;
import com.cinescore.model.User;
import com.cinescore.repository.UserRepository;
import com.cinescore.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Testes Unitários")
class AuthServiceTest {

    @Mock private UserRepository        userRepository;
    @Mock private ModerationService     moderationService;
    @Mock private PasswordEncoder       passwordEncoder;
    @Mock private JwtService            jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    private User usuarioMock;

    @BeforeEach
    void configurar() {
        usuarioMock = User.builder()
                .id(1L).name("Maria").email("maria@test.com")
                .passwordHash("hashed").role("USER").build();
    }

    @Test
    @DisplayName("Deve registrar novo usuário com sucesso")
    void deveRegistrarUsuarioComSucesso() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setName("Maria"); dto.setEmail("maria@test.com"); dto.setPassword("Senha@123");

        when(userRepository.existsByEmail("maria@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(usuarioMock);
        when(jwtService.generateToken(any())).thenReturn("token.jwt.mock");

        LoginResponseDTO resultado = authService.register(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getToken()).isEqualTo("token.jwt.mock");
        assertThat(resultado.getEmail()).isEqualTo("maria@test.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar DuplicateResourceException ao registrar e-mail já existente")
    void deveLancarExcecaoAoRegistrarEmailDuplicado() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setName("Maria"); dto.setEmail("maria@test.com"); dto.setPassword("Senha@123");

        when(userRepository.existsByEmail("maria@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("e-mail");
    }

    @Test
    @DisplayName("Deve fazer login com sucesso e retornar token")
    void deveFazerLoginComSucesso() {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("maria@test.com"); dto.setPassword("Senha@123");

        when(userRepository.findByEmail("maria@test.com")).thenReturn(Optional.of(usuarioMock));
        when(jwtService.generateToken(any())).thenReturn("token.jwt.login");

        LoginResponseDTO resultado = authService.login(dto);

        assertThat(resultado.getToken()).isEqualTo("token.jwt.login");
        assertThat(resultado.getType()).isEqualTo("Bearer");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Deve lançar BadCredentialsException ao autenticar com credenciais inválidas")
    void deveLancarExcecaoComCredenciaisInvalidas() {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("maria@test.com"); dto.setPassword("senhaErrada");

        doThrow(new BadCredentialsException("Credenciais inválidas"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(BadCredentialsException.class);
    }
}
