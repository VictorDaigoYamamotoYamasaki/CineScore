package com.cinescore.service;

import com.cinescore.dto.FavoriteMovieDTO;
import com.cinescore.dto.FavoriteMovieRequestDTO;
import com.cinescore.dto.ProfileDTO;
import com.cinescore.exception.InvalidInputException;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.model.FavoriteMovie;
import com.cinescore.model.Review;
import com.cinescore.model.User;
import com.cinescore.repository.FavoriteMovieRepository;
import com.cinescore.repository.FollowerRepository;
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
@DisplayName("ProfileService - Testes Unitários")
class ProfileServiceTest {

    @Mock private UserRepository          userRepository;
    @Mock private ReviewRepository        reviewRepository;
    @Mock private FavoriteMovieRepository favoriteMovieRepository;
    @Mock private FollowerRepository      followerRepository;

    @InjectMocks private ProfileService profileService;

    private static final String USER_ID   = "uuid-camila-001";
    private static final String OTHER_ID  = "uuid-outro-002";

    private User   usuarioMock;
    private Review reviewMock;

    @BeforeEach
    void configurar() {
        usuarioMock = User.builder()
                .id(USER_ID).name("Camila").email("camila@test.com")
                .passwordHash("hashed").role("USER").build();

        reviewMock = Review.builder()
                .id("uuid-review-001").user(usuarioMock).movieId("278")
                .movieTitle("Um Sonho de Liberdade").rating(5.0)
                .watchedAt(LocalDate.now()).createdAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("Deve buscar perfil com dados completos")
    void deveBuscarPerfilComSucesso() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(followerRepository.countByFollowedId(USER_ID)).thenReturn(10L);
        when(followerRepository.countByFollowerId(USER_ID)).thenReturn(5L);
        when(followerRepository.existsByFollowerIdAndFollowedId(any(), any())).thenReturn(false);
        when(favoriteMovieRepository.findByUserIdOrderByPosition(USER_ID)).thenReturn(List.of());
        when(reviewRepository.findByUserIdOrderByWatchedAtDescIdDesc(USER_ID)).thenReturn(List.of(reviewMock));

        ProfileDTO perfil = profileService.buscarPerfil(USER_ID, null);

        assertThat(perfil.getName()).isEqualTo("Camila");
        assertThat(perfil.getFollowerCount()).isEqualTo(10L);
        assertThat(perfil.getFollowingCount()).isEqualTo(5L);
        assertThat(perfil.getFavorites()).hasSize(5);
        assertThat(perfil.getReviews()).hasSize(1);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException para userId inexistente")
    void deveLancarExcecaoParaUserIdInexistente() {
        when(userRepository.findById("uuid-invalido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.buscarPerfil("uuid-invalido", null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuário");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar perfil por id inexistente")
    void deveLancarExcecaoParaIdInexistente() {
        when(userRepository.findById("uuid-invalido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.buscarPerfilPorId("uuid-invalido", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Deve retornar isFollowing=true quando viewer já segue o perfil")
    void deveRetornarIsFollowingQuandoJaSegue() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(followerRepository.countByFollowedId(USER_ID)).thenReturn(1L);
        when(followerRepository.countByFollowerId(USER_ID)).thenReturn(0L);
        when(followerRepository.existsByFollowerIdAndFollowedId(OTHER_ID, USER_ID)).thenReturn(true);
        when(favoriteMovieRepository.findByUserIdOrderByPosition(USER_ID)).thenReturn(List.of());
        when(reviewRepository.findByUserIdOrderByWatchedAtDescIdDesc(USER_ID)).thenReturn(List.of());

        ProfileDTO perfil = profileService.buscarPerfil(USER_ID, OTHER_ID);

        assertThat(perfil.isFollowing()).isTrue();
    }

    @Test
    @DisplayName("Deve retornar isFollowing=false quando viewer é o mesmo usuário")
    void deveRetornarFalseQuandoViewerEOMesmoUsuario() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(followerRepository.countByFollowedId(USER_ID)).thenReturn(0L);
        when(followerRepository.countByFollowerId(USER_ID)).thenReturn(0L);
        when(favoriteMovieRepository.findByUserIdOrderByPosition(USER_ID)).thenReturn(List.of());
        when(reviewRepository.findByUserIdOrderByWatchedAtDescIdDesc(USER_ID)).thenReturn(List.of());

        ProfileDTO perfil = profileService.buscarPerfil(USER_ID, USER_ID);

        assertThat(perfil.isFollowing()).isFalse();
    }

    @Test
    @DisplayName("Deve salvar filme favorito na posição correta")
    void deveSalvarFilmeFavoritoComSucesso() {
        FavoriteMovieRequestDTO req = new FavoriteMovieRequestDTO();
        req.setMovieId("278"); req.setTitle("Um Sonho de Liberdade"); req.setPosition(1);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(favoriteMovieRepository.findByUserIdAndPosition(USER_ID, 1)).thenReturn(Optional.empty());
        when(favoriteMovieRepository.findByUserIdOrderByPosition(USER_ID)).thenReturn(List.of());

        FavoriteMovieDTO resultado = profileService.salvarFavorito(USER_ID, 1, req);

        assertThat(resultado.getPosition()).isEqualTo(1);
        assertThat(resultado.getMovieId()).isEqualTo("278");
        verify(favoriteMovieRepository).save(any(FavoriteMovie.class));
    }

    @Test
    @DisplayName("Deve lançar InvalidInputException para posição fora do intervalo 1-5")
    void deveLancarExcecaoParaPosicaoInvalida() {
        FavoriteMovieRequestDTO req = new FavoriteMovieRequestDTO();
        req.setMovieId("278"); req.setPosition(6);

        assertThatThrownBy(() -> profileService.salvarFavorito(USER_ID, 6, req))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("posição");

        assertThatThrownBy(() -> profileService.salvarFavorito(USER_ID, 0, req))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("Deve remover favorito existente")
    void deveRemoverFavoritoExistente() {
        FavoriteMovie favoritoExistente = FavoriteMovie.builder()
                .id("uuid-fav-001").user(usuarioMock).movieId("278").position(2).build();
        when(favoriteMovieRepository.findByUserIdAndPosition(USER_ID, 2))
                .thenReturn(Optional.of(favoritoExistente));

        profileService.removerFavorito(USER_ID, 2);

        verify(favoriteMovieRepository).delete(favoritoExistente);
    }

    @Test
    @DisplayName("Deve substituir favorito quando já existe filme na mesma posição")
    void deveSubstituirFavoritoNaMesmaPosicao() {
        FavoriteMovie favoritoExistente = FavoriteMovie.builder()
                .id("uuid-fav-001").user(usuarioMock).movieId("550").position(1).build();

        FavoriteMovieRequestDTO req = new FavoriteMovieRequestDTO();
        req.setMovieId("278"); req.setTitle("Um Sonho de Liberdade"); req.setPosition(1);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(favoriteMovieRepository.findByUserIdAndPosition(USER_ID, 1))
                .thenReturn(Optional.of(favoritoExistente));
        when(favoriteMovieRepository.findByUserIdOrderByPosition(USER_ID)).thenReturn(List.of());

        profileService.salvarFavorito(USER_ID, 1, req);

        verify(favoriteMovieRepository).delete(favoritoExistente);
        verify(favoriteMovieRepository).save(any(FavoriteMovie.class));
    }
}
