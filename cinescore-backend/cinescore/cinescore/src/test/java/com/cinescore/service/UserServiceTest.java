package com.cinescore.service;

import com.cinescore.dto.ProfileUpdateDTO;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
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

    private static final String USER_ID = "uuid-pedro-001";

    private User usuarioMock;

    @BeforeEach
    void configurar() {
        usuarioMock = User.builder()
                .id(USER_ID).name("Pedro").email("pedro@test.com")
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
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));

        UserResponseDTO resultado = userService.buscarPorId(USER_ID);

        assertThat(resultado.getId()).isEqualTo(USER_ID);
        assertThat(resultado.getName()).isEqualTo("Pedro");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar usuário inexistente")
    void deveLancarExcecaoAoBuscarUsuarioInexistente() {
        when(userRepository.findById("uuid-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.buscarPorId("uuid-inexistente"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuário");
    }

    @Test
    @DisplayName("Deve retornar lista de usuários ao buscar por nome")
    void deveBuscarUsuariosPorNome() {
        when(userRepository.findByNameContainingIgnoreCaseOrderByNameAsc("Pedro"))
                .thenReturn(List.of(usuarioMock));

        List<UserResponseDTO> resultado = userService.buscarPorNome("Pedro");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getName()).isEqualTo("Pedro");
    }

    @Test
    @DisplayName("Deve retornar lista vazia ao buscar por nome inexistente")
    void deveRetornarListaVaziaParaNomeInexistente() {
        when(userRepository.findByNameContainingIgnoreCaseOrderByNameAsc("Inexistente"))
                .thenReturn(List.of());

        List<UserResponseDTO> resultado = userService.buscarPorNome("Inexistente");

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve carregar UserDetails pelo e-mail com sucesso")
    void deveCarregarUserDetailsPorEmail() {
        when(userRepository.findByEmail("pedro@test.com")).thenReturn(Optional.of(usuarioMock));

        UserDetails resultado = userService.loadUserByUsername("pedro@test.com");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getUsername()).isEqualTo("pedro@test.com");
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException para e-mail desconhecido")
    void deveLancarExcecaoParaEmailDesconhecido() {
        when(userRepository.findByEmail("naoexiste@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("naoexiste@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Deve excluir conta e reviews quando deletarReviews=true")
    void deveDeletarContaComReviews() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));

        userService.deletarConta(USER_ID, true);

        verify(reviewRepository).deleteByUserId(USER_ID);
        verify(userRepository).deleteById(USER_ID);
    }

    @Test
    @DisplayName("Deve anonimizar dados pessoais quando deletarReviews=false")
    void deveAnonimizarContaSemDeletarReviews() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(userRepository.save(any())).thenReturn(usuarioMock);

        userService.deletarConta(USER_ID, false);

        verify(reviewRepository, never()).deleteByUserId(any());
        verify(userRepository).save(argThat(u ->
                u.getName().equals("Usuário Deletado") &&
                u.getEmail().contains("@removed.invalid")));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao deletar usuário inexistente")
    void deveLancarExcecaoAoDeletarUsuarioInexistente() {
        when(userRepository.existsById("uuid-inexistente")).thenReturn(false);

        assertThatThrownBy(() -> userService.deletar("uuid-inexistente"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve atualizar nome e e-mail do perfil com sucesso")
    void deveAtualizarPerfilComSucesso() {
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setName("Pedro Novo"); dto.setEmail("pedro.novo@test.com");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(userRepository.existsByEmailAndIdNot("pedro.novo@test.com", USER_ID)).thenReturn(false);
        when(userRepository.existsByNameIgnoreCaseAndIdNot("Pedro Novo", USER_ID)).thenReturn(false);
        when(userRepository.save(any())).thenReturn(usuarioMock);

        userService.atualizarPerfil(USER_ID, dto);

        verify(userRepository).save(argThat(u ->
                u.getName().equals("Pedro Novo") &&
                u.getEmail().equals("pedro.novo@test.com")));
    }

    @Test
    @DisplayName("Deve atualizar senha quando nova senha é informada")
    void deveAtualizarSenhaQuandoInformada() {
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setName("Pedro"); dto.setEmail("pedro@test.com");
        dto.setPassword("NovaSenha@123");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(userRepository.existsByEmailAndIdNot("pedro@test.com", USER_ID)).thenReturn(false);
        when(userRepository.existsByNameIgnoreCaseAndIdNot("Pedro", USER_ID)).thenReturn(false);
        when(passwordEncoder.encode("NovaSenha@123")).thenReturn("novo-hash");
        when(userRepository.save(any())).thenReturn(usuarioMock);

        userService.atualizarPerfil(USER_ID, dto);

        verify(passwordEncoder).encode("NovaSenha@123");
    }

    @Test
    @DisplayName("Deve lançar DuplicateResourceException ao atualizar com nome já em uso")
    void deveLancarExcecaoQuandoNomeJaExisteAoAtualizar() {
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setName("Nome Existente"); dto.setEmail("pedro@test.com");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(userRepository.existsByEmailAndIdNot("pedro@test.com", USER_ID)).thenReturn(false);
        when(userRepository.existsByNameIgnoreCaseAndIdNot("Nome Existente", USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> userService.atualizarPerfil(USER_ID, dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("nome");
    }

    @Test
    @DisplayName("Deve lançar DuplicateResourceException ao atualizar com e-mail já em uso")
    void deveLancarExcecaoQuandoEmailJaExisteAoAtualizar() {
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setName("Pedro"); dto.setEmail("existente@test.com");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(userRepository.existsByEmailAndIdNot("existente@test.com", USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> userService.atualizarPerfil(USER_ID, dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("e-mail");
    }
}
