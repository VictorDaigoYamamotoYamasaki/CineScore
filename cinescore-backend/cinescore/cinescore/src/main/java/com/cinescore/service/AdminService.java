package com.cinescore.service;

import com.cinescore.dto.AdminStatsDTO;
import com.cinescore.dto.GenreCountDTO;
import com.cinescore.dto.RatingDistributionDTO;
import com.cinescore.dto.ReviewsPerDayDTO;

import com.cinescore.dto.MovieDTO;
import com.cinescore.dto.PageResponseDTO;
import com.cinescore.dto.PopularMovieDTO;
import com.cinescore.dto.ReviewResponseDTO;
import com.cinescore.dto.UserResponseDTO;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.model.Review;
import com.cinescore.repository.ReviewRepository;
import com.cinescore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final int TOP_MOVIES_LIMIT  = 5;
    private static final int SINGLE_RESULT     = 1;
    private static final double ROUNDING_FACTOR = 10.0;

    private final UserRepository   userRepository;
    private final ReviewRepository reviewRepository;
    private final MovieService     movieService;

    public AdminStatsDTO buscarEstatisticas() {
        return AdminStatsDTO.builder()
                .totalUsuarios(userRepository.count())
                .totalReviews(reviewRepository.count())
                .build();
    }

    public PageResponseDTO<UserResponseDTO> listarUsuariosPaginados(int page, int size) {
        List<UserResponseDTO> todos = userRepository.findAll()
                .stream()
                .map(UserResponseDTO::fromUser)
                .toList();
        return PageResponseDTO.of(todos, page, size);
    }

    public PageResponseDTO<ReviewResponseDTO> listarReviewsPaginadas(int page, int size) {
        List<ReviewResponseDTO> todas = reviewRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .map(ReviewResponseDTO::fromReview)
                .toList();
        return PageResponseDTO.of(todas, page, size);
    }

    public List<PopularMovieDTO> buscarFilmesPopulares() {
        List<String> topMovieIds = reviewRepository.findMostReviewedMovies(
                PageRequest.of(0, TOP_MOVIES_LIMIT));

        return topMovieIds.stream()
                .map(this::buildPopularMovieDTO)
                .toList();
    }


    // ── Gráficos ──────────────────────────────────────────────────────────────

    public List<RatingDistributionDTO> distribuicaoDeNotas() {
        List<Double> todasAsNotas = List.of(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0);

        Map<Double, Long> contagem = reviewRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));

        return todasAsNotas.stream()
                .map(nota -> new RatingDistributionDTO(nota, contagem.getOrDefault(nota, 0L)))
                .toList();
    }

    public List<ReviewsPerDayDTO> reviewsPorDia() {
        LocalDateTime inicioDaSemana = LocalDate.now().minusDays(6).atStartOfDay();
        DateTimeFormatter formatter   = DateTimeFormatter.ofPattern("dd/MM");

        Map<LocalDate, Long> contagem = reviewRepository.findRecentReviews(inicioDaSemana)
                .stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().toLocalDate(),
                        Collectors.counting()
                ));

        List<ReviewsPerDayDTO> resultado = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate data  = LocalDate.now().minusDays(i);
            long totalDoDia = contagem.getOrDefault(data, 0L);
            resultado.add(new ReviewsPerDayDTO(data.format(formatter), totalDoDia));
        }
        return resultado;
    }

    public List<GenreCountDTO> generosMaisAvaliados() {
        List<String> topMovieIds = reviewRepository.findMostReviewedMovies(PageRequest.of(0, 15));

        Map<String, Long> contagemPorGenero = new HashMap<>();

        for (String movieId : topMovieIds) {
            try {
                MovieDTO movie        = movieService.buscarPorId(movieId);
                long    totalReviews  = reviewRepository.countByMovieId(movieId);

                if (movie.getGenre() != null && !movie.getGenre().isBlank()) {
                    for (String genero : movie.getGenre().split(",")) {
                        String nome = genero.trim();
                        if (!nome.isBlank()) {
                            contagemPorGenero.merge(nome, totalReviews, Long::sum);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        return contagemPorGenero.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(e -> new GenreCountDTO(e.getKey(), e.getValue()))
                .toList();
    }

    public void deletarUsuario(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuário", userId);
        }
        userRepository.deleteById(userId);
    }

    public void deletarReview(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new ResourceNotFoundException("Review", reviewId);
        }
        reviewRepository.deleteById(reviewId);
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private PopularMovieDTO buildPopularMovieDTO(String movieId) {
        PopularMovieDTO dto = new PopularMovieDTO();
        dto.setMovieId(movieId);

        preencherDadosDasReviews(dto, movieId);
        enriquecerComTmdbSeNecessario(dto, movieId);
        preencherEstatisticas(dto, movieId);

        return dto;
    }

    private void preencherDadosDasReviews(PopularMovieDTO dto, String movieId) {
        List<Review> reviews = reviewRepository.findTopByMovieIdWithTitle(
                movieId, PageRequest.of(0, SINGLE_RESULT));

        if (!reviews.isEmpty()) {
            dto.setMovieTitle(reviews.get(0).getMovieTitle());
            dto.setMoviePoster(reviews.get(0).getMoviePoster());
        }
    }

    private void enriquecerComTmdbSeNecessario(PopularMovieDTO dto, String movieId) {
        boolean semTitulo  = dto.getMovieTitle() == null || dto.getMovieTitle().isBlank();
        boolean semPoster  = dto.getMoviePoster() == null;

        if (!semTitulo && !semPoster) return;

        try {
            MovieDTO movie = movieService.buscarPorId(movieId);
            if (semTitulo)  dto.setMovieTitle(movie.getTitle());
            if (semPoster)  dto.setMoviePoster(movie.getPoster());
        } catch (Exception ignored) {
            if (dto.getMovieTitle() == null) {
                dto.setMovieTitle("Filme #" + movieId);
            }
        }
    }

    private void preencherEstatisticas(PopularMovieDTO dto, String movieId) {
        dto.setReviewCount(reviewRepository.countByMovieId(movieId));
        Double media = reviewRepository.avgRatingByMovieId(movieId);
        dto.setAvgRating(media != null ? Math.round(media * ROUNDING_FACTOR) / ROUNDING_FACTOR : 0.0);
    }
}
