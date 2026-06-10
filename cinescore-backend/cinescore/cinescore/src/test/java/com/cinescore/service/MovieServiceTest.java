package com.cinescore.service;

import com.cinescore.dto.MovieDTO;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.model.MovieCache;
import com.cinescore.repository.MovieCacheRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MovieService - Cache e Classificação")
class MovieServiceTest {

    @Mock private RestTemplate         restTemplate;
    @Mock private MovieCacheRepository movieCacheRepository;

    @InjectMocks private MovieService movieService;

    @BeforeEach
    void configurar() {
        ReflectionTestUtils.setField(movieService, "apiKey",   "test-key");
        ReflectionTestUtils.setField(movieService, "baseUrl",  "https://api.themoviedb.org/3");
        ReflectionTestUtils.setField(movieService, "imageUrl", "https://image.tmdb.org/t/p/w500");
    }

    // ── Cache de filmes ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve salvar no cache após busca bem-sucedida no TMDB")
    void deveSalvarNoCacheAposBuscaTmdb() {
        MovieDTO tmdbResp = filmeComCredits("550", "Fight Club", "1999");
        when(restTemplate.getForObject(anyString(), eq(MovieDTO.class), any(), any()))
                .thenReturn(tmdbResp);

        movieService.buscarPorId("550");

        verify(movieCacheRepository).save(argThat(c ->
                "550".equals(c.getMovieId()) && "Fight Club".equals(c.getTitle())));
    }

    @Test
    @DisplayName("Deve usar cache local quando TMDB estiver indisponível")
    void deveUsarCacheLocalQuandoTmdbFalhar() {
        when(restTemplate.getForObject(anyString(), eq(MovieDTO.class), any(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        MovieCache cached = MovieCache.builder()
                .movieId("550").title("Fight Club").year("1999")
                .poster("https://poster.url").genres("Drama, Thriller")
                .director("David Fincher").voteAverage(8.8)
                .certification("18").cachedAt(LocalDateTime.now()).build();

        when(movieCacheRepository.findById("550")).thenReturn(Optional.of(cached));

        MovieDTO resultado = movieService.buscarPorId("550");

        assertThat(resultado.getTitle()).isEqualTo("Fight Club");
        assertThat(resultado.getCertification()).isEqualTo("18");
        assertThat(resultado.getDirector()).isEqualTo("David Fincher");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando TMDB falha e cache está vazio")
    void deveLancarExcecaoQuandoTmdbECacheVazios() {
        when(restTemplate.getForObject(anyString(), eq(MovieDTO.class), any(), any()))
                .thenThrow(new RuntimeException("TMDB offline"));
        when(movieCacheRepository.findById("999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.buscarPorId("999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Filme");
    }

    @Test
    @DisplayName("Deve re-lançar ResourceNotFoundException do TMDB sem consultar o cache")
    void deveReLancarNotFoundSemConsultarCache() {
        MovieDTO semId = new MovieDTO();
        when(restTemplate.getForObject(anyString(), eq(MovieDTO.class), any(), any()))
                .thenReturn(semId);

        assertThatThrownBy(() -> movieService.buscarPorId("0"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(movieCacheRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Falha ao salvar no cache não deve interromper o retorno do filme")
    void falhaAoSalvarCacheNaoDeveQuebrararFluxo() {
        MovieDTO tmdbResp = filmeComCredits("278", "Um Sonho de Liberdade", "1994");
        when(restTemplate.getForObject(anyString(), eq(MovieDTO.class), any(), any()))
                .thenReturn(tmdbResp);
        when(movieCacheRepository.save(any()))
                .thenThrow(new RuntimeException("DB connection lost"));

        assertThatNoException().isThrownBy(() -> movieService.buscarPorId("278"));
    }

    // ── Classificação indicativa ──────────────────────────────────────────────

    @Test
    @DisplayName("Deve extrair certificação do Brasil corretamente")
    void deveExtrairCertificacaoBrasil() {
        MovieDTO filme = filmeComCertificacao("278", "14");
        when(restTemplate.getForObject(anyString(), eq(MovieDTO.class), any(), any()))
                .thenReturn(filme);

        MovieDTO resultado = movieService.buscarPorId("278");

        assertThat(resultado.getCertification()).isEqualTo("14");
    }

    @Test
    @DisplayName("Deve retornar null quando não há classificação para o Brasil")
    void deveRetornarNullQuandoSemClassificacaoBrasil() {
        MovieDTO filme = filmeComCredits("155", "Batman", "2008");
        filme.setReleaseDates(null);
        when(restTemplate.getForObject(anyString(), eq(MovieDTO.class), any(), any()))
                .thenReturn(filme);

        MovieDTO resultado = movieService.buscarPorId("155");

        assertThat(resultado.getCertification()).isNull();
    }

    @Test
    @DisplayName("Deve ignorar países diferentes do Brasil na extração de classificação")
    void deveIgnorarOutrosPaisesNaClassificacao() {
        MovieDTO filme = filmeComCertificacaoPais("603", "US", "R");
        when(restTemplate.getForObject(anyString(), eq(MovieDTO.class), any(), any()))
                .thenReturn(filme);

        MovieDTO resultado = movieService.buscarPorId("603");

        assertThat(resultado.getCertification()).isNull();
    }

    @Test
    @DisplayName("Deve salvar certificação no cache")
    void deveSalvarCertificacaoNoCache() {
        MovieDTO filme = filmeComCertificacao("475557", "18");
        when(restTemplate.getForObject(anyString(), eq(MovieDTO.class), any(), any()))
                .thenReturn(filme);

        movieService.buscarPorId("475557");

        verify(movieCacheRepository).save(argThat(c ->
                "18".equals(c.getCertification())));
    }

    // ── Helpers de teste ──────────────────────────────────────────────────────

    private MovieDTO filmeComCredits(String id, String title, String year) {
        MovieDTO dto = new MovieDTO();
        dto.setId(Long.parseLong(id));
        dto.setTitle(title);
        dto.setReleaseDate(year + "-01-01");
        dto.setCredits(new MovieDTO.CreditsDTO());
        return dto;
    }

    private MovieDTO filmeComCertificacao(String id, String cert) {
        MovieDTO dto = filmeComCredits(id, "Filme Teste", "2020");
        dto.setReleaseDates(buildReleaseDates("BR", cert));
        return dto;
    }

    private MovieDTO filmeComCertificacaoPais(String id, String pais, String cert) {
        MovieDTO dto = filmeComCredits(id, "Filme Teste", "2020");
        dto.setReleaseDates(buildReleaseDates(pais, cert));
        return dto;
    }

    private MovieDTO.ReleaseDatesWrapper buildReleaseDates(String isoCode, String cert) {
        var entry    = new MovieDTO.ReleaseDatesWrapper.CountryRelease.CertificationEntry();
        entry.setCertification(cert);

        var country  = new MovieDTO.ReleaseDatesWrapper.CountryRelease();
        country.setIsoCode(isoCode);
        country.setReleaseDates(List.of(entry));

        var wrapper  = new MovieDTO.ReleaseDatesWrapper();
        wrapper.setResults(List.of(country));
        return wrapper;
    }
}
