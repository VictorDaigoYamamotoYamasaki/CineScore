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
                .id("uuid-ana-001").name("Ana").email("ana@test.com")
                .passwordHash("hashed").role("USER").build();
    }

    @Test
    @DisplayName("Deve gerar token não nulo e com 3 partes (header.payload.signature)")
    void deveGerarTokenValido() {
        String token = jwtService.generateToken(usuarioMock);

        assertThat(token).isNotNull().isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Deve extrair username (e-mail) corretamente do token")
    void deveExtrairUsernameDoToken() {
        String token = jwtService.generateToken(usuarioMock);

        assertThat(jwtService.extractUsername(token)).isEqualTo("ana@test.com");
    }

    @Test
    @DisplayName("Deve validar token correto para o usuário correspondente")
    void deveValidarTokenCorreto() {
        String token = jwtService.generateToken(usuarioMock);

        assertThat(jwtService.isTokenValid(token, usuarioMock)).isTrue();
    }

    @Test
    @DisplayName("Deve retornar false ao validar token com usuário de e-mail diferente")
    void deveRetornarInvalidoParaUsuarioDiferente() {
        String token = jwtService.generateToken(usuarioMock);

        User outroUsuario = User.builder()
                .id("uuid-carlos-002").name("Carlos").email("carlos@test.com")
                .passwordHash("hashed").role("USER").build();

        assertThat(jwtService.isTokenValid(token, outroUsuario)).isFalse();
    }

    @Test
    @DisplayName("Deve retornar false para token com expiração no passado")
    void deveRetornarInvalidoParaTokenExpirado() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        String token = jwtService.generateToken(usuarioMock);

        assertThat(jwtService.isTokenValid(token, usuarioMock)).isFalse();
    }

    @Test
    @DisplayName("Deve incluir role no token como claim")
    void deveIncluirRoleNoToken() {
        User admin = User.builder()
                .id("uuid-admin-001").name("Admin").email("admin@test.com")
                .passwordHash("hashed").role("ADMIN").build();

        String token = jwtService.generateToken(admin);
        String roleExtraida = jwtService.extractClaim(token,
                claims -> claims.get("role", String.class));

        assertThat(roleExtraida).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Deve lançar exceção ao parsear token malformado")
    void deveLancarExcecaoParaTokenMalformado() {
        assertThatThrownBy(() -> jwtService.extractUsername("token.invalido.aqui"))
                .isInstanceOf(Exception.class);
    }
}
