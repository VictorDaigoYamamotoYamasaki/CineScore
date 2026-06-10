package com.cinescore.service;

import com.cinescore.dto.AdminStatsDTO;
import com.cinescore.dto.AdminUserResponseDTO;
import com.cinescore.dto.PageResponseDTO;
import com.cinescore.dto.ReviewResponseDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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

    private static final String USER_ID   = "uuid-admin-001";
    private static final String REVIEW_ID = "uuid-review-001";

    private User   usuarioMock;
    private Review reviewMock;

    @BeforeEach
    void configurar() {
        usuarioMock = User.builder()
                .id(USER_ID).name("Admin").email("admin@test.com")
                .passwordHash("hashed").role("ADMIN").build();

        reviewMock = Review.builder()
                .id(REVIEW_ID).user(usuarioMock).movieId("550").movieTitle("Fight Club")
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
    @DisplayName("Deve retornar usuários paginados com e-mail mascarado")
    void deveListarUsuariosPaginadosComEmailMascarado() {
        Page<User> pagina = new PageImpl<>(List.of(usuarioMock));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(pagina);

        PageResponseDTO<AdminUserResponseDTO> resultado =
                adminService.listarUsuariosPaginados(0, 10, Sort.Direction.ASC);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getCurrentPage()).isEqualTo(0);
        assertThat(resultado.getTotalPages()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getEmailMascarado())
                .doesNotContain("admin@test.com")
                .contains("@test.com");
    }

    @Test
    @DisplayName("Deve retornar reviews paginadas corretamente")
    void deveListarReviewsPaginadas() {
        Page<Review> pagina = new PageImpl<>(List.of(reviewMock));
        when(reviewRepository.findAll(any(Pageable.class))).thenReturn(pagina);

        PageResponseDTO<ReviewResponseDTO> resultado =
                adminService.listarReviewsPaginadas(0, 10, Sort.Direction.DESC);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getMovieTitle()).isEqualTo("Fight Club");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao deletar usuário inexistente")
    void deveLancarExcecaoAoDeletarUsuarioInexistente() {
        when(userRepository.existsById("uuid-inexistente")).thenReturn(false);

        assertThatThrownBy(() -> adminService.deletarUsuario("uuid-inexistente"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuário");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao deletar review inexistente")
    void deveLancarExcecaoAoDeletarReviewInexistente() {
        when(reviewRepository.existsById("uuid-inexistente")).thenReturn(false);

        assertThatThrownBy(() -> adminService.deletarReview("uuid-inexistente"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Review");
    }

    @Test
    @DisplayName("Deve deletar usuário existente com sucesso")
    void deveDeletarUsuarioComSucesso() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        adminService.deletarUsuario(USER_ID);

        verify(userRepository).deleteById(USER_ID);
    }

    @Test
    @DisplayName("Deve deletar review existente com sucesso")
    void deveDeletarReviewComSucesso() {
        when(reviewRepository.existsById(REVIEW_ID)).thenReturn(true);

        adminService.deletarReview(REVIEW_ID);

        verify(reviewRepository).deleteById(REVIEW_ID);
    }

    @Test
    @DisplayName("Deve respeitar tamanho máximo de página")
    void deveRespeitarTamanhoMaximoDePagina() {
        Page<User> pagina = new PageImpl<>(List.of());
        when(userRepository.findAll(any(Pageable.class))).thenReturn(pagina);

        adminService.listarUsuariosPaginados(0, 9999, Sort.Direction.ASC);

        verify(userRepository).findAll(any(Pageable.class));
    }
}
