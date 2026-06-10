package com.cinescore.service;

import com.cinescore.dto.CommentDTO;
import com.cinescore.exception.InvalidInputException;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.exception.UnauthorizedOperationException;
import com.cinescore.model.Comment;
import com.cinescore.model.Review;
import com.cinescore.model.User;
import com.cinescore.repository.CommentRepository;
import com.cinescore.repository.ReactionRepository;
import com.cinescore.repository.ReviewRepository;
import com.cinescore.repository.UserRepository;
import jakarta.persistence.EntityManager;
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
@DisplayName("CommentService - Testes Unitários")
class CommentServiceTest {

    @Mock private CommentRepository  commentRepository;
    @Mock private ReactionRepository reactionRepository;
    @Mock private ReviewRepository   reviewRepository;
    @Mock private UserRepository     userRepository;
    @Mock private ModerationService  moderationService;
    @Mock private EntityManager      entityManager;

    @InjectMocks private CommentService commentService;

    private static final String USER_ID    = "uuid-lucas-001";
    private static final String REVIEW_ID  = "uuid-review-010";
    private static final String COMMENT_ID = "uuid-comment-100";

    private User    usuarioMock;
    private Review  reviewMock;
    private Comment commentMock;

    @BeforeEach
    void configurar() {
        usuarioMock = User.builder()
                .id(USER_ID).name("Lucas").email("lucas@test.com").role("USER").build();

        reviewMock = Review.builder()
                .id(REVIEW_ID).user(usuarioMock).movieId("550")
                .movieTitle("Fight Club").rating(4.5)
                .createdAt(LocalDateTime.now()).build();

        commentMock = Comment.builder()
                .id(COMMENT_ID).review(reviewMock).user(usuarioMock)
                .commentText("Ótimo filme!").createdAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("Deve adicionar comentário com sucesso quando texto é válido")
    void deveAdicionarComentarioComSucesso() {
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(reviewMock));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(commentRepository.save(any(Comment.class))).thenReturn(commentMock);

        CommentDTO resultado = commentService.adicionarComentario(REVIEW_ID, USER_ID, "Ótimo filme!");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getCommentText()).isEqualTo("Ótimo filme!");
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("Deve lançar InvalidInputException ao adicionar comentário vazio")
    void deveLancarExcecaoComComentarioVazio() {
        assertThatThrownBy(() -> commentService.adicionarComentario(REVIEW_ID, USER_ID, "  "))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("comentário");
    }

    @Test
    @DisplayName("Deve lançar InvalidInputException ao adicionar comentário nulo")
    void deveLancarExcecaoComComentarioNulo() {
        assertThatThrownBy(() -> commentService.adicionarComentario(REVIEW_ID, USER_ID, null))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("Deve lançar InvalidInputException quando comentário excede 500 caracteres")
    void deveLancarExcecaoQuandoComentarioExcedeLimiteDeCaracteres() {
        String textoLongo = "a".repeat(501);

        assertThatThrownBy(() -> commentService.adicionarComentario(REVIEW_ID, USER_ID, textoLongo))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("500 caracteres");
    }

    @Test
    @DisplayName("Deve aceitar comentário com exatamente 500 caracteres")
    void deveAceitarComentarioComExatamente500Caracteres() {
        String textoExato = "a".repeat(500);
        when(reviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(reviewMock));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        Comment commentComTextoExato = Comment.builder()
                .id("uuid-comment-101").review(reviewMock).user(usuarioMock)
                .commentText(textoExato).createdAt(LocalDateTime.now()).build();
        when(commentRepository.save(any(Comment.class))).thenReturn(commentComTextoExato);

        CommentDTO resultado = commentService.adicionarComentario(REVIEW_ID, USER_ID, textoExato);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getCommentText()).hasSize(500);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao comentar em review inexistente")
    void deveLancarExcecaoAoComentarEmReviewInexistente() {
        when(reviewRepository.findById("uuid-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.adicionarComentario("uuid-inexistente", USER_ID, "Texto"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Review");
    }

    @Test
    @DisplayName("Deve listar comentários de uma review em ordem cronológica")
    void deveListarComentariosDaReview() {
        when(commentRepository.findByReviewIdOrderByCreatedAtAsc(REVIEW_ID))
                .thenReturn(List.of(commentMock));

        List<CommentDTO> resultado = commentService.listarComentarios(REVIEW_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCommentText()).isEqualTo("Ótimo filme!");
    }

    @Test
    @DisplayName("Deve deletar comentário quando usuário é o autor")
    void deveDeletarComentarioProprio() {
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(commentMock));

        commentService.deletarComentario(COMMENT_ID, USER_ID);

        verify(commentRepository).deleteById(COMMENT_ID);
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedOperationException ao deletar comentário de outro usuário")
    void deveLancarExcecaoAoDeletarComentarioDeOutroUsuario() {
        when(commentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(commentMock));

        assertThatThrownBy(() -> commentService.deletarComentario(COMMENT_ID, "uuid-outro-099"))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao deletar comentário inexistente")
    void deveLancarExcecaoAoDeletarComentarioInexistente() {
        when(commentRepository.findById("uuid-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deletarComentario("uuid-inexistente", USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Comentário");
    }
}
