package com.cinescore.service;

import com.cinescore.dto.AdminStatsDTO;
import com.cinescore.dto.PageResponseDTO;
import com.cinescore.dto.ReviewResponseDTO;
import com.cinescore.dto.UserResponseDTO;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.model.Review;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService - Testes Unitários")
class AdminServiceTest {

    @Mock private UserRepository   userRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private MovieService     movieService;

    @InjectMocks private AdminService adminService;

    private User   usuarioMock;
    private Review reviewMock;

    @BeforeEach
    void configurar() {
        usuarioMock = User.builder()
                .id(1L).name("Admin").email("admin@test.com").role("ADMIN").build();

        reviewMock = Review.builder()
                .id(1L).user(usuarioMock).movieId("550").movieTitle("Fight Club")
                .rating(5.0).createdAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("Deve retornar estatísticas corretas da plataforma")
    void deveBuscarEstatisticasCorretamente() {
        when(userRepository.count()).thenReturn(42L);
        when(reviewRepository.count()).thenReturn(150L);

        AdminStatsDTO stats = adminService.buscarEstatisticas();

        assertThat(stats.getTotalUsuarios()).isEqualTo(42L);
        assertThat(stats.getTotalReviews()).isEqualTo(150L);
    }

    @Test
    @DisplayName("Deve retornar usuários paginados corretamente")
    void deveListarUsuariosPaginados() {
        when(userRepository.findAll()).thenReturn(List.of(usuarioMock));

        PageResponseDTO<UserResponseDTO> resultado = adminService.listarUsuariosPaginados(0, 10);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getCurrentPage()).isEqualTo(0);
        assertThat(resultado.getTotalPages()).isEqualTo(1);
        assertThat(resultado.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve calcular página corretamente com múltiplos registros")
    void deveCalcularPaginacaoCorretamente() {
        List<User> usuarios = List.of(
                usuarioMock,
                User.builder().id(2L).name("B").email("b@test.com").role("USER").build(),
                User.builder().id(3L).name("C").email("c@test.com").role("USER").build()
        );
        when(userRepository.findAll()).thenReturn(usuarios);

        PageResponseDTO<UserResponseDTO> pagina1 = adminService.listarUsuariosPaginados(0, 2);
        PageResponseDTO<UserResponseDTO> pagina2 = adminService.listarUsuariosPaginados(1, 2);

        assertThat(pagina1.getContent()).hasSize(2);
        assertThat(pagina2.getContent()).hasSize(1);
        assertThat(pagina1.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao deletar usuário inexistente")
    void deveLancarExcecaoAoDeletarUsuarioInexistente() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> adminService.deletarUsuario(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuário");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao deletar review inexistente")
    void deveLancarExcecaoAoDeletarReviewInexistente() {
        when(reviewRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> adminService.deletarReview(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Review");
    }

    @Test
    @DisplayName("Deve deletar usuário existente com sucesso")
    void deveDeletarUsuarioComSucesso() {
        when(userRepository.existsById(1L)).thenReturn(true);

        adminService.deletarUsuario(1L);

        verify(userRepository).deleteById(1L);
    }
}
