package com.cinescore.security;

import com.cinescore.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService - Testes Unitários")
class JwtServiceTest {

    private JwtService jwtService;
    private User       usuarioMock;

    @BeforeEach
    void configurar() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "cinescore-chave-secreta-jwt-2026-producao-longa-o-suficiente");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        usuarioMock = User.builder()
                .id(1L).name("Ana").email("ana@test.com")
                .passwordHash("hashed").role("USER").build();
    }

    @Test
    @DisplayName("Deve gerar token não nulo e não vazio")
    void deveGerarTokenValido() {
        String token = jwtService.generateToken(usuarioMock);

        assertThat(token).isNotNull().isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("Deve extrair username (e-mail) corretamente do token")
    void deveExtrairUsernameDoToken() {
        String token = jwtService.generateToken(usuarioMock);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("ana@test.com");
    }

    @Test
    @DisplayName("Deve validar token correto para o usuário correspondente")
    void deveValidarTokenCorreto() {
        String token = jwtService.generateToken(usuarioMock);

        boolean valido = jwtService.isTokenValid(token, usuarioMock);

        assertThat(valido).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false ao validar token com usuário diferente")
    void deveRetornarInvalidoParaUsuarioDiferente() {
        String token = jwtService.generateToken(usuarioMock);

        User outroUsuario = User.builder()
                .id(2L).name("Carlos").email("carlos@test.com")
                .passwordHash("hashed").role("USER").build();

        boolean valido = jwtService.isTokenValid(token, outroUsuario);

        assertThat(valido).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false para token com expiração no passado")
    void deveRetornarInvalidoParaTokenExpirado() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        String token = jwtService.generateToken(usuarioMock);

        boolean valido = jwtService.isTokenValid(token, usuarioMock);

        assertThat(valido).isFalse();
    }
}
