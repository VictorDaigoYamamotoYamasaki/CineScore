package com.cinescore.service;

import com.cinescore.dto.ReviewRequestDTO;
import com.cinescore.dto.ReviewResponseDTO;
import com.cinescore.dto.ReviewUpdateDTO;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.exception.UnauthorizedOperationException;
import com.cinescore.model.Review;
import com.cinescore.model.User;
import com.cinescore.repository.ReviewHistoryRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService - Testes Unitários")
class ReviewServiceTest {

    @Mock private ReviewRepository        reviewRepository;
    @Mock private ReviewHistoryRepository reviewHistoryRepository;
    @Mock private UserRepository          userRepository;
    @Mock private ModerationService       moderationService;

    @InjectMocks private ReviewService reviewService;

    private User usuarioMock;
    private Review reviewMock;

    @BeforeEach
    void configurar() {
        usuarioMock = User.builder()
                .id(1L).name("João").email("joao@test.com").role("USER").build();

        reviewMock = Review.builder()
                .id(10L).user(usuarioMock).movieId("550")
                .movieTitle("Fight Club").rating(4.5)
                .reviewText("Excelente filme").createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve criar review com sucesso quando dados válidos")
    void deveCriarReviewComSucesso() {
        ReviewRequestDTO dto = new ReviewRequestDTO();
        dto.setMovieId("550"); dto.setMovieTitle("Fight Club");
        dto.setRating(4.5); dto.setReviewText("Ótimo");

        when(userRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(reviewRepository.save(any(Review.class))).thenReturn(reviewMock);

        ReviewResponseDTO resultado = reviewService.criar(1L, dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getMovieTitle()).isEqualTo("Fight Club");
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao criar review com usuário inexistente")
    void deveLancarExcecaoAoCriarReviewComUsuarioInexistente() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.criar(99L, new ReviewRequestDTO()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuário");
    }

    @Test
    @DisplayName("Deve retornar review ao buscar por ID existente")
    void deveBuscarReviewPorIdComSucesso() {
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(reviewMock));

        ReviewResponseDTO resultado = reviewService.buscarPorId(10L);

        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getMovieTitle()).isEqualTo("Fight Club");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar review inexistente")
    void deveLancarExcecaoAoBuscarReviewInexistente() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Review");
    }

    @Test
    @DisplayName("Deve listar todas as reviews em ordem cronológica decrescente")
    void deveListarReviewsEmOrdemDecrescente() {
        Review reviewAntiga = Review.builder().id(1L).user(usuarioMock)
                .movieId("100").rating(3.0).createdAt(LocalDateTime.now().minusDays(2)).build();
        Review reviewRecente = Review.builder().id(2L).user(usuarioMock)
                .movieId("200").rating(5.0).createdAt(LocalDateTime.now()).build();

        when(reviewRepository.findAll()).thenReturn(List.of(reviewAntiga, reviewRecente));

        List<ReviewResponseDTO> resultado = reviewService.listarTodos();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Deve salvar histórico e atualizar review quando usuário é o dono")
    void deveAtualizarReviewERegistrarHistorico() {
        ReviewUpdateDTO dto = new ReviewUpdateDTO();
        dto.setRating(5.0); dto.setReviewText("Revisado");

        when(reviewRepository.findById(10L)).thenReturn(Optional.of(reviewMock));
        when(reviewRepository.save(any())).thenReturn(reviewMock);

        reviewService.atualizar(10L, 1L, dto);

        verify(reviewHistoryRepository).save(any());
        verify(reviewRepository).save(reviewMock);
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedOperationException ao atualizar review de outro usuário")
    void deveLancarExcecaoAoAtualizarReviewDeOutroUsuario() {
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(reviewMock));

        assertThatThrownBy(() -> reviewService.atualizar(10L, 99L, new ReviewUpdateDTO()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    @DisplayName("Deve deletar review quando usuário é o dono")
    void deveDeletarReviewComSucesso() {
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(reviewMock));

        reviewService.deletar(10L, 1L);

        verify(reviewRepository).deleteById(10L);
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedOperationException ao deletar review de outro usuário")
    void deveLancarExcecaoAoDeletarReviewDeOutroUsuario() {
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(reviewMock));

        assertThatThrownBy(() -> reviewService.deletar(10L, 99L))
                .isInstanceOf(UnauthorizedOperationException.class);
    }
}
