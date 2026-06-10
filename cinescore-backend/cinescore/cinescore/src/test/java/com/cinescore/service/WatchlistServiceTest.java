package com.cinescore.service;

import com.cinescore.dto.WatchlistItemDTO;
import com.cinescore.dto.WatchlistRequestDTO;
import com.cinescore.exception.DuplicateResourceException;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.model.User;
import com.cinescore.model.WatchlistItem;
import com.cinescore.repository.UserRepository;
import com.cinescore.repository.WatchlistRepository;
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
@DisplayName("WatchlistService - Testes Unitários")
class WatchlistServiceTest {

    @Mock private WatchlistRepository watchlistRepository;
    @Mock private UserRepository      userRepository;

    @InjectMocks private WatchlistService watchlistService;

    private static final String USER_ID = "uuid-lucas-001";

    private User          usuarioMock;
    private WatchlistItem itemMock;

    @BeforeEach
    void configurar() {
        usuarioMock = User.builder()
                .id(USER_ID).name("Lucas").email("lucas@test.com").role("USER").build();

        itemMock = WatchlistItem.builder()
                .id("uuid-item-010").user(usuarioMock)
                .movieId("862").movieTitle("Toy Story")
                .moviePoster(null).movieYear("1995")
                .movieVoteAverage(8.3)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve retornar lista de itens da watchlist do usuário")
    void deveListarItensDaWatchlist() {
        when(watchlistRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(itemMock));

        List<WatchlistItemDTO> resultado = watchlistService.listarPorUsuario(USER_ID);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getMovieId()).isEqualTo("862");
        assertThat(resultado.get(0).getMovieTitle()).isEqualTo("Toy Story");
        assertThat(resultado.get(0).getMovieYear()).isEqualTo("1995");
        assertThat(resultado.get(0).getMovieVoteAverage()).isEqualTo(8.3);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando usuário não tem filmes na watchlist")
    void deveRetornarListaVaziaParaWatchlistVazia() {
        when(watchlistRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of());

        assertThat(watchlistService.listarPorUsuario(USER_ID)).isEmpty();
    }

    @Test
    @DisplayName("Deve adicionar filme à watchlist com sucesso")
    void deveAdicionarFilmeComSucesso() {
        WatchlistRequestDTO dto = criarRequestDTO("862", "Toy Story", "1995", 8.3);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(watchlistRepository.existsByUserIdAndMovieId(USER_ID, "862")).thenReturn(false);
        when(watchlistRepository.save(any(WatchlistItem.class))).thenReturn(itemMock);

        WatchlistItemDTO resultado = watchlistService.adicionar(USER_ID, dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getMovieId()).isEqualTo("862");
        assertThat(resultado.getMovieTitle()).isEqualTo("Toy Story");
        verify(watchlistRepository).save(any(WatchlistItem.class));
    }

    @Test
    @DisplayName("Deve persistir todos os campos do DTO ao adicionar")
    void devePersistirTodosOsCamposDoDTO() {
        WatchlistRequestDTO dto = criarRequestDTO("680", "Pulp Fiction", "1994", 9.0);
        dto.setMoviePoster("https://poster.url/pulp.jpg");

        WatchlistItem itemSalvo = WatchlistItem.builder()
                .id("uuid-item-011").user(usuarioMock)
                .movieId("680").movieTitle("Pulp Fiction")
                .moviePoster("https://poster.url/pulp.jpg").movieYear("1994")
                .movieVoteAverage(9.0).createdAt(LocalDateTime.now()).build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(watchlistRepository.existsByUserIdAndMovieId(USER_ID, "680")).thenReturn(false);
        when(watchlistRepository.save(any())).thenReturn(itemSalvo);

        WatchlistItemDTO resultado = watchlistService.adicionar(USER_ID, dto);

        assertThat(resultado.getMoviePoster()).isEqualTo("https://poster.url/pulp.jpg");
        assertThat(resultado.getMovieVoteAverage()).isEqualTo(9.0);
        verify(watchlistRepository).save(argThat(item ->
                item.getMovieId().equals("680") &&
                item.getMovieTitle().equals("Pulp Fiction") &&
                item.getMovieYear().equals("1994")));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao adicionar com usuário inexistente")
    void deveLancarExcecaoComUsuarioInexistente() {
        when(userRepository.findById("uuid-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.adicionar("uuid-inexistente", criarRequestDTO("862", "Toy Story", "1995", 8.3)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuário");

        verify(watchlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar DuplicateResourceException ao adicionar filme já na watchlist")
    void deveLancarExcecaoComFilmeDuplicado() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(watchlistRepository.existsByUserIdAndMovieId(USER_ID, "862")).thenReturn(true);

        assertThatThrownBy(() -> watchlistService.adicionar(USER_ID, criarRequestDTO("862", "Toy Story", "1995", 8.3)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Watchlist");

        verify(watchlistRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve aceitar movieVoteAverage nulo ao adicionar")
    void deveAceitarVoteAverageNulo() {
        WatchlistItem semVote = WatchlistItem.builder()
                .id("uuid-item-012").user(usuarioMock).movieId("862")
                .movieTitle("Toy Story").movieYear("1995")
                .movieVoteAverage(null).createdAt(LocalDateTime.now()).build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(usuarioMock));
        when(watchlistRepository.existsByUserIdAndMovieId(USER_ID, "862")).thenReturn(false);
        when(watchlistRepository.save(any())).thenReturn(semVote);

        WatchlistItemDTO resultado = watchlistService.adicionar(USER_ID, criarRequestDTO("862", "Toy Story", "1995", null));

        assertThat(resultado.getMovieVoteAverage()).isNull();
    }

    @Test
    @DisplayName("Deve remover filme da watchlist com sucesso")
    void deveRemoverFilmeComSucesso() {
        when(watchlistRepository.existsByUserIdAndMovieId(USER_ID, "862")).thenReturn(true);

        watchlistService.remover(USER_ID, "862");

        verify(watchlistRepository).deleteByUserIdAndMovieId(USER_ID, "862");
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao remover filme não presente na watchlist")
    void deveLancarExcecaoAoRemoverFilmeInexistente() {
        when(watchlistRepository.existsByUserIdAndMovieId(USER_ID, "99999")).thenReturn(false);

        assertThatThrownBy(() -> watchlistService.remover(USER_ID, "99999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Watchlist");

        verify(watchlistRepository, never()).deleteByUserIdAndMovieId(any(), any());
    }

    @Test
    @DisplayName("Deve verificar existência antes de deletar (não deleta às cegas)")
    void deveVerificarExistenciaAntesDeRemover() {
        when(watchlistRepository.existsByUserIdAndMovieId(USER_ID, "862")).thenReturn(true);

        watchlistService.remover(USER_ID, "862");

        var inOrder = inOrder(watchlistRepository);
        inOrder.verify(watchlistRepository).existsByUserIdAndMovieId(USER_ID, "862");
        inOrder.verify(watchlistRepository).deleteByUserIdAndMovieId(USER_ID, "862");
    }

    private WatchlistRequestDTO criarRequestDTO(String movieId, String title, String year, Double vote) {
        WatchlistRequestDTO dto = new WatchlistRequestDTO();
        dto.setMovieId(movieId);
        dto.setMovieTitle(title);
        dto.setMovieYear(year);
        dto.setMovieVoteAverage(vote);
        return dto;
    }
}
