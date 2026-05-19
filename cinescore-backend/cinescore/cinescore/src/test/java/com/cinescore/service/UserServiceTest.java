package com.cinescore.service;

import com.cinescore.dto.UserRequestDTO;
import com.cinescore.dto.UserResponseDTO;
import com.cinescore.exception.DuplicateResourceException;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.model.User;
import com.cinescore.repository.ReviewRepository;
import com.cinescore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - Testes Unitários")
class UserServiceTest {

    @Mock private UserRepository   userRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private PasswordEncoder  passwordEncoder;

    @InjectMocks private UserService userService;

    private User usuarioMock;

    @BeforeEach
    void configurar() {
        usuarioMock = User.builder()
                .id(1L).name("Pedro").email("pedro@test.com")
                .passwordHash("hashed").role("USER").build();
    }

    @Test
    @DisplayName("Deve criar usuário com sucesso quando e-mail não existe")
    void deveCriarUsuarioComSucesso() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setName("Pedro"); dto.setEmail("pedro@test.com"); dto.setPassword("Senha@123");

        when(userRepository.existsByEmail("pedro@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(usuarioMock);

        UserResponseDTO resultado = userService.criar(dto);

        assertThat(resultado.getName()).isEqualTo("Pedro");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar DuplicateResourceException ao criar com e-mail duplicado")
    void deveLancarExcecaoComEmailDuplicadoAoCriar() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setEmail("pedro@test.com");

        when(userRepository.existsByEmail("pedro@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.criar(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("e-mail");
    }

    @Test
    @DisplayName("Deve retornar usuário ao buscar por ID existente")
    void deveBuscarUsuarioPorIdComSucesso() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));

        UserResponseDTO resultado = userService.buscarPorId(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getName()).isEqualTo("Pedro");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar usuário inexistente")
    void deveLancarExcecaoAoBuscarUsuarioInexistente() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuário");
    }

    @Test
    @DisplayName("Deve excluir conta e reviews quando deletarReviews=true")
    void deveDeletarContaComReviews() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));

        userService.deletarConta(1L, true);

        verify(reviewRepository).deleteByUserId(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Deve anonimizar dados pessoais quando deletarReviews=false")
    void deveAnonimizarContaSemDeletarReviews() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(userRepository.save(any())).thenReturn(usuarioMock);

        userService.deletarConta(1L, false);

        verify(reviewRepository, never()).deleteByUserId(any());
        verify(userRepository).save(argThat(u ->
                u.getName().equals("Usuário Deletado") &&
                u.getEmail().contains("@removed.invalid")));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao deletar usuário inexistente")
    void deveLancarExcecaoAoDeletarUsuarioInexistente() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deletar(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
