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

    private User   usuarioMock;
    private Review reviewMock;
    private Comment commentMock;

    @BeforeEach
    void configurar() {
        usuarioMock = User.builder()
                .id(1L).name("Lucas").email("lucas@test.com").role("USER").build();

        reviewMock = Review.builder()
                .id(10L).user(usuarioMock).movieId("550")
                .movieTitle("Fight Club").rating(4.5)
                .createdAt(LocalDateTime.now()).build();

        commentMock = Comment.builder()
                .id(100L).review(reviewMock).user(usuarioMock)
                .commentText("Ótimo filme!").createdAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("Deve adicionar comentário com sucesso quando texto é válido")
    void deveAdicionarComentarioComSucesso() {
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(reviewMock));
        when(userRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(commentRepository.save(any(Comment.class))).thenReturn(commentMock);

        CommentDTO resultado = commentService.adicionarComentario(10L, 1L, "Ótimo filme!");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getCommentText()).isEqualTo("Ótimo filme!");
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("Deve lançar InvalidInputException ao adicionar comentário vazio")
    void deveLancarExcecaoComComentarioVazio() {
        assertThatThrownBy(() -> commentService.adicionarComentario(10L, 1L, "  "))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("texto");
    }

    @Test
    @DisplayName("Deve lançar InvalidInputException ao adicionar comentário nulo")
    void deveLancarExcecaoComComentarioNulo() {
        assertThatThrownBy(() -> commentService.adicionarComentario(10L, 1L, null))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao comentar em review inexistente")
    void deveLancarExcecaoAoComentarEmReviewInexistente() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.adicionarComentario(99L, 1L, "Texto"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Review");
    }

    @Test
    @DisplayName("Deve listar comentários de uma review em ordem cronológica")
    void deveListarComentariosDaReview() {
        when(commentRepository.findByReviewIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(commentMock));

        List<CommentDTO> resultado = commentService.listarComentarios(10L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCommentText()).isEqualTo("Ótimo filme!");
    }

    @Test
    @DisplayName("Deve deletar comentário quando usuário é o autor")
    void deveDeletarComentarioProprio() {
        when(commentRepository.findById(100L)).thenReturn(Optional.of(commentMock));

        commentService.deletarComentario(100L, 1L);

        verify(commentRepository).deleteById(100L);
    }

    @Test
    @DisplayName("Deve lançar UnauthorizedOperationException ao deletar comentário de outro usuário")
    void deveLancarExcecaoAoDeletarComentarioDeOutroUsuario() {
        when(commentRepository.findById(100L)).thenReturn(Optional.of(commentMock));

        assertThatThrownBy(() -> commentService.deletarComentario(100L, 99L))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao deletar comentário inexistente")
    void deveLancarExcecaoAoDeletarComentarioInexistente() {
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deletarComentario(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Comentário");
    }
}
