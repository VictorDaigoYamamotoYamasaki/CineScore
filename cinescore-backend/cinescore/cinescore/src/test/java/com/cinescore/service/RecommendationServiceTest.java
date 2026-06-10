package com.cinescore.service;

import com.cinescore.dto.MovieDTO;
import com.cinescore.dto.RecommendationDTO;
import com.cinescore.model.FavoriteMovie;
import com.cinescore.model.Review;
import com.cinescore.model.User;
import com.cinescore.repository.FavoriteMovieRepository;
import com.cinescore.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService - Testes Unitários")
class RecommendationServiceTest {

    @Mock private FavoriteMovieRepository favoriteMovieRepository;
    @Mock private ReviewRepository        reviewRepository;
    @Mock private RestTemplate            restTemplate;

    @InjectMocks private RecommendationService recommendationService;

    private static final String USER_ID = "uuid-user-001";

    private User          usuarioMock;
    private FavoriteMovie favoritoMock;

    @BeforeEach
    void configurar() {
        ReflectionTestUtils.setField(recommendationService, "apiKey",   "test-key");
        ReflectionTestUtils.setField(recommendationService, "baseUrl",  "https://api.themoviedb.org/3");
        ReflectionTestUtils.setField(recommendationService, "imageUrl", "https://image.tmdb.org/t/p/w500");

        usuarioMock = User.builder()
                .id(USER_ID).name("Felipe").email("felipe@test.com").role("USER").build();

        favoritoMock = FavoriteMovie.builder()
                .id("uuid-fav-001").user(usuarioMock).movieId("550").position(1).build();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando usuário não tem favoritos")
    void deveRetornarListaVaziaParaUsuarioSemFavoritos() {
        when(favoriteMovieRepository.findByUserIdOrderByPosition(USER_ID))
                .thenReturn(Collections.emptyList());

        List<RecommendationDTO> resultado = recommendationService.recomendar(USER_ID, Set.of());

        assertThat(resultado).isEmpty();
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando TMDB não retorna gêneros")
    void deveRetornarListaVaziaQuandoTmdbNaoRetornaGeneros() {
        when(favoriteMovieRepository.findByUserIdOrderByPosition(USER_ID))
                .thenReturn(List.of(favoritoMock));

        MovieDTO filmeVazio = new MovieDTO();
        filmeVazio.setId(550L);
        filmeVazio.setTitle("Fight Club");
        filmeVazio.setGenres(null);

        when(restTemplate.getForObject(anyString(), eq(MovieDTO.class), any(), any()))
                .thenReturn(filmeVazio);
        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Collections.emptyList());

        List<RecommendationDTO> resultado = recommendationService.recomendar(USER_ID, Set.of());

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando TMDB lança exceção para o favorito")
    void deveRetornarListaVaziaQuandoTmdbFalha() {
        when(favoriteMovieRepository.findByUserIdOrderByPosition(USER_ID))
                .thenReturn(List.of(favoritoMock));

        when(restTemplate.getForObject(anyString(), eq(MovieDTO.class), any(), any()))
                .thenThrow(new RuntimeException("TMDB offline"));
        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Collections.emptyList());

        List<RecommendationDTO> resultado = recommendationService.recomendar(USER_ID, Set.of());

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve excluir IDs passados no excludeIds do resultado")
    void deveExcluirFilmesDoExcludeIds() {
        FavoriteMovie fav = FavoriteMovie.builder()
                .id("uuid-fav-002").user(usuarioMock).movieId("278").position(1).build();

        when(favoriteMovieRepository.findByUserIdOrderByPosition(USER_ID))
                .thenReturn(List.of(fav));

        MovieDTO filmeComGenero = filmeComGenero(278L, "Drama");
        when(restTemplate.getForObject(anyString(), eq(MovieDTO.class), any(), any()))
                .thenReturn(filmeComGenero);
        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Collections.emptyList());

        // discover retorna um filme mas ele está no excludeIds
        RecommendationDTO.TmdbDiscoverResponse discoverResp = new RecommendationDTO.TmdbDiscoverResponse();
        RecommendationDTO.TmdbDiscoverItem item = new RecommendationDTO.TmdbDiscoverItem();
        item.setId(12345L);
        item.setTitle("Filme Excluído");
        discoverResp.setResults(List.of(item));

        when(restTemplate.getForObject(anyString(), eq(RecommendationDTO.TmdbDiscoverResponse.class), any(), any(), any()))
                .thenReturn(discoverResp);

        // Passa o ID do filme como excluído
        List<RecommendationDTO> resultado = recommendationService.recomendar(USER_ID, Set.of(12345L));

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve excluir filmes já avaliados pelo usuário dos resultados")
    void deveExcluirFilmesJaAvaliadosPeloUsuario() {
        when(favoriteMovieRepository.findByUserIdOrderByPosition(USER_ID))
                .thenReturn(List.of(favoritoMock));

        MovieDTO filmeComGenero = filmeComGenero(550L, "Drama");
        when(restTemplate.getForObject(anyString(), eq(MovieDTO.class), any(), any()))
                .thenReturn(filmeComGenero);

        // Usuário já assistiu ao filme 9999
        Review reviewExistente = Review.builder()
                .id("uuid-review-001").user(usuarioMock).movieId("9999")
                .rating(4.0).createdAt(LocalDateTime.now()).build();
        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(reviewExistente));

        RecommendationDTO.TmdbDiscoverResponse discoverResp = new RecommendationDTO.TmdbDiscoverResponse();
        RecommendationDTO.TmdbDiscoverItem item = new RecommendationDTO.TmdbDiscoverItem();
        item.setId(9999L);
        item.setTitle("Já Assistido");
        discoverResp.setResults(List.of(item));

        when(restTemplate.getForObject(anyString(), eq(RecommendationDTO.TmdbDiscoverResponse.class), any(), any(), any()))
                .thenReturn(discoverResp);

        List<RecommendationDTO> resultado = recommendationService.recomendar(USER_ID, Set.of());

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve excluir o próprio favorito dos resultados de recomendação")
    void deveExcluirFilmeFavoritoDosResultados() {
        when(favoriteMovieRepository.findByUserIdOrderByPosition(USER_ID))
                .thenReturn(List.of(favoritoMock)); // movieId = "550"

        MovieDTO filmeComGenero = filmeComGenero(550L, "Drama");
        when(restTemplate.getForObject(anyString(), eq(MovieDTO.class), any(), any()))
                .thenReturn(filmeComGenero);
        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Collections.emptyList());

        // discover retorna o próprio favorito
        RecommendationDTO.TmdbDiscoverResponse discoverResp = new RecommendationDTO.TmdbDiscoverResponse();
        RecommendationDTO.TmdbDiscoverItem item = new RecommendationDTO.TmdbDiscoverItem();
        item.setId(550L);
        item.setTitle("Fight Club");
        discoverResp.setResults(List.of(item));

        when(restTemplate.getForObject(anyString(), eq(RecommendationDTO.TmdbDiscoverResponse.class), any(), any(), any()))
                .thenReturn(discoverResp);

        List<RecommendationDTO> resultado = recommendationService.recomendar(USER_ID, Set.of());

        // Fight Club não deve aparecer pois é o próprio favorito
        assertThat(resultado).noneMatch(r -> r.getTitle().equals("Fight Club"));
    }

    @Test
    @DisplayName("Deve ignorar favorito com movieId nulo")
    void deveIgnorarFavoritoComMovieIdNulo() {
        FavoriteMovie favSemFilme = FavoriteMovie.builder()
                .id("uuid-fav-003").user(usuarioMock).movieId(null).position(1).build();

        when(favoriteMovieRepository.findByUserIdOrderByPosition(USER_ID))
                .thenReturn(List.of(favSemFilme));

        List<RecommendationDTO> resultado = recommendationService.recomendar(USER_ID, Set.of());

        assertThat(resultado).isEmpty();
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("Deve incluir reason no DTO de recomendação por gênero")
    void deveIncluirReasonNoDTO() {
        when(favoriteMovieRepository.findByUserIdOrderByPosition(USER_ID))
                .thenReturn(List.of(favoritoMock));

        MovieDTO filmeComGenero = filmeComGenero(550L, "Drama");
        when(restTemplate.getForObject(anyString(), eq(MovieDTO.class), any(), any()))
                .thenReturn(filmeComGenero);
        when(reviewRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Collections.emptyList());

        RecommendationDTO.TmdbDiscoverResponse discoverResp = new RecommendationDTO.TmdbDiscoverResponse();
        RecommendationDTO.TmdbDiscoverItem item = new RecommendationDTO.TmdbDiscoverItem();
        item.setId(99001L);
        item.setTitle("Novo Drama");
        item.setPosterPath("/poster.jpg");
        item.setReleaseDate("2025-01-01");
        item.setVoteAverage(7.5);
        discoverResp.setResults(List.of(item));

        when(restTemplate.getForObject(anyString(), eq(RecommendationDTO.TmdbDiscoverResponse.class), any(), any(), any()))
                .thenReturn(discoverResp);

        List<RecommendationDTO> resultado = recommendationService.recomendar(USER_ID, Set.of());

        assertThat(resultado).isNotEmpty();
        assertThat(resultado.get(0).getReason()).startsWith("Baseado em");
        assertThat(resultado.get(0).getTitle()).isEqualTo("Novo Drama");
        assertThat(resultado.get(0).getYear()).isEqualTo("2025");
        assertThat(resultado.get(0).getPoster()).contains("/poster.jpg");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private MovieDTO filmeComGenero(Long id, String genreName) {
        MovieDTO dto = new MovieDTO();
        dto.setId(id);
        dto.setTitle("Filme Teste");
        dto.setReleaseDate("2020-01-01");

        MovieDTO.GenreDTO genre = new MovieDTO.GenreDTO();
        genre.setId(18L); // Drama
        genre.setName(genreName);
        dto.setGenres(List.of(genre));

        dto.setCredits(new MovieDTO.CreditsDTO());
        return dto;
    }
}
