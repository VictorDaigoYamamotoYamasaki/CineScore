package com.cinescore.service;

import com.cinescore.dto.ReviewRequestDTO;
import com.cinescore.dto.ReviewResponseDTO;
import com.cinescore.dto.ReviewUpdateDTO;
import com.cinescore.exception.ContentModerationException;
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

import java.time.LocalDate;
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

    private static final String USER_ID   = "uuid-joao-001";
    private static final String REVIEW_ID = "uuid-review-010";

    private User   usuarioMock;
    private Review reviewMock;

    @BeforeEach
    void configurar() {
        usuarioMock = User.builder()
                .id(USER_ID).name("João").email("joao@test.com").role("USER").build();

        reviewMock = Review.builder()
                .id(REVIEW_ID).user(usuarioMock).movieId("550")
                .movieTitle("Fight Club").rating(4.5)
                .reviewText("Excelente filme")
                .watchedAt(LocalDate.of(2026, 6, 1))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve criar review com sucesso quando dados são válidos")
    void deveCriarReviewComSucesso() {
        ReviewRequestDTO dto = new ReviewRequestDTO();
        dto.setMovieId("550"); dto.setMovieTitle("Fight Club");
        dto.setRating(4.5); dto.setReviewText("Ótimo");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(reviewRepository.save(any(Review.class))).thenReturn(reviewMock);

        ReviewResponseDTO resultado = reviewService.criar(USER_ID, dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getMovieTitle()).isEqualTo("Fight Club");
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Deve usar data de hoje quando watchedAt não é informado")
    void deveUsarDataDeHojeQuandoWatchedAtNaoInformado() {
        ReviewRequestDTO dto = new ReviewRequestDTO();
        dto.setMovieId("550"); dto.setRating(4.0);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review saved = inv.getArgument(0);
            assertThat(saved.getWatchedAt()).isNotNull();
            assertThat(saved.getWatchedAt()).isEqualTo(LocalDate.now());
            return reviewMock;
        });

        reviewService.criar(USER_ID, dto);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Deve usar watchedAt fornecido quando informado")
    void deveUsarWatchedAtFornecidoQuandoInformado() {
        LocalDate dataAssistido = LocalDate.of(2026, 6, 1);
        ReviewRequestDTO dto = new ReviewRequestDTO();
        dto.setMovieId("550"); dto.setRating(4.0);
        dto.setWatchedAt(dataAssistido);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review saved = inv.getArgument(0);
            assertThat(saved.getWatchedAt()).isEqualTo(dataAssistido);
            return reviewMock;
        });

        reviewService.criar(USER_ID, dto);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException com usuário inexistente")
    void deveLancarExcecaoAoCriarComUsuarioInexistente() {
        when(userRepository.findById("uuid-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.criar("uuid-inexistente", new ReviewRequestDTO()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuário");
    }

    @Test
    @DisplayName("Deve lançar ContentModerationException quando texto é ofensivo")
    void deveLancarExcecaoQuandoTextoEOfensivo() {
        ReviewRequestDTO dto = new ReviewRequestDTO();
        dto.setMovieId("550"); dto.setRating(3.0);
        dto.setReviewText("conteudo ofensivo");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        doThrow(new ContentModerationException("review"))
                .when(moderationService).verificar("conteudo ofensivo", "review");

        assertThatThrownBy(() -> reviewService.criar(USER_ID, dto))
                .isInstanceOf(ContentModerationException.class);
    }

    @Test
    @DisplayName("Deve retornar review ao buscar por ID existente")
    void deveBuscarReviewPorIdComSucesso() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(reviewMock));

        ReviewResponseDTO resultado = reviewService.buscarPorId(REVIEW_ID);

        assertThat(resultado.getId()).isEqualTo(REVIEW_ID);
        assertThat(resultado.getMovieTitle()).isEqualTo("Fight Club");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar review inexistente")
    void deveLancarExcecaoAoBuscarReviewInexistente() {
        when(reviewRepository.findById("uuid-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.buscarPorId("uuid-inexistente"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Review");
    }

    @Test
    @DisplayName("Deve listar reviews por filme")
    void deveListarReviewsPorFilme() {
        when(reviewRepository.findByMovieIdOrderByCreatedAtDesc("550"))
                .thenReturn(List.of(reviewMock));

        List<ReviewResponseDTO> resultado = reviewService.listarPorFilme("550");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getMovieId()).isEqualTo("550");
    }

    @Test
    @DisplayName("Deve listar reviews por usuário")
    void deveListarReviewsPorUsuario() {
        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(reviewMock));

        List<ReviewResponseDTO> resultado = reviewService.listarPorUsuario(USER_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Deve salvar histórico e atualizar review quando usuário é o dono")
    void deveAtualizarReviewERegistrarHistorico() {
        ReviewUpdateDTO dto = new ReviewUpdateDTO();
        dto.setRating(5.0); dto.setReviewText("Revisado");

        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(reviewMock));
        when(reviewRepository.save(any())).thenReturn(reviewMock);

        reviewService.atualizar(REVIEW_ID, USER_ID, dto);

        verify(reviewHistoryRepository).save(any());
        verify(reviewRepository).save(reviewMock);
    }

    @Test
    @DisplayName("Deve atualizar watchedAt junto com rating e texto")
    void deveAtualizarWatchedAtNaEdicao() {
        LocalDate novaData = LocalDate.of(2026, 6, 5);
        ReviewUpdateDTO dto = new ReviewUpdateDTO();
        dto.setRating(5.0); dto.setReviewText("Revisado"); dto.setWatchedAt(novaData);

        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(reviewMock));
        when(reviewRepository.save(any())).thenReturn(reviewMock);

        reviewService.atualizar(REVIEW_ID, USER_ID, dto);

        assertThat(reviewMock.getWatchedAt()).isEqualTo(novaData);
        verify(reviewRepository).save(reviewMock);
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedOperationException ao atualizar review de outro usuário")
    void deveLancarExcecaoAoAtualizarReviewDeOutroUsuario() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(reviewMock));

        assertThatThrownBy(() -> reviewService.atualizar(REVIEW_ID, "uuid-outro-099", new ReviewUpdateDTO()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    @DisplayName("Deve atualizar data assistido com sucesso")
    void deveAtualizarDataAssistidoComSucesso() {
        LocalDate novaData = LocalDate.of(2026, 6, 3);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(reviewMock));
        when(reviewRepository.save(any())).thenReturn(reviewMock);

        reviewService.atualizarDataAssistido(REVIEW_ID, USER_ID, novaData);

        assertThat(reviewMock.getWatchedAt()).isEqualTo(novaData);
        verify(reviewRepository).save(reviewMock);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao atualizar data de review inexistente")
    void deveLancarExcecaoAoAtualizarDataDeReviewInexistente() {
        when(reviewRepository.findById("uuid-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.atualizarDataAssistido("uuid-inexistente", USER_ID, LocalDate.now()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedOperationException ao atualizar data de review de outro usuário")
    void deveLancarExcecaoAoAtualizarDataDeReviewDeOutroUsuario() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(reviewMock));

        assertThatThrownBy(() -> reviewService.atualizarDataAssistido(REVIEW_ID, "uuid-outro-099", LocalDate.now()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    @DisplayName("Deve deletar review quando usuário é o dono")
    void deveDeletarReviewComSucesso() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(reviewMock));

        reviewService.deletar(REVIEW_ID, USER_ID);

        verify(reviewRepository).deleteById(REVIEW_ID);
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedOperationException ao deletar review de outro usuário")
    void deveLancarExcecaoAoDeletarReviewDeOutroUsuario() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(reviewMock));

        assertThatThrownBy(() -> reviewService.deletar(REVIEW_ID, "uuid-outro-099"))
                .isInstanceOf(UnauthorizedOperationException.class);
    }
}
