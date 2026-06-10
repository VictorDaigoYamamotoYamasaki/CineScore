package com.cinescore.service;

import com.cinescore.dto.AdminStatsDTO;
import com.cinescore.dto.AdminUserResponseDTO;
import com.cinescore.dto.GenreCountDTO;
import com.cinescore.dto.MovieDTO;
import com.cinescore.dto.PageResponseDTO;
import com.cinescore.dto.PopularMovieDTO;
import com.cinescore.dto.RatingDistributionDTO;
import com.cinescore.dto.RatingExtremesDTO;
import com.cinescore.dto.ReviewResponseDTO;
import com.cinescore.dto.ReviewsPerDayDTO;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.model.Review;
import com.cinescore.repository.ReviewRepository;
import com.cinescore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private static final int    TOP_MOVIES_LIMIT = 5;
    private static final int    SINGLE_RESULT    = 1;
    private static final double ROUNDING_FACTOR  = 10.0;
    private static final int    MAX_PAGE_SIZE    = 100;

    private final UserRepository   userRepository;
    private final ReviewRepository reviewRepository;
    private final MovieService     movieService;

    @Transactional(readOnly = true)
    public AdminStatsDTO buscarEstatisticas() {
        return AdminStatsDTO.builder()
                .totalUsuarios(userRepository.count())
                .totalReviews(reviewRepository.count())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<AdminUserResponseDTO> listarUsuariosPaginados(int page, int size, Sort.Direction sortDir) {
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by(sortDir, "id"));
        return PageResponseDTO.fromPage(
                userRepository.findAll(pageable).map(AdminUserResponseDTO::fromUser));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ReviewResponseDTO> listarReviewsPaginadas(int page, int size, Sort.Direction sortDir) {
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by(sortDir, "id"));
        return PageResponseDTO.fromPage(
                reviewRepository.findAll(pageable).map(ReviewResponseDTO::fromReview));
    }

    @Transactional(readOnly = true)
    public List<PopularMovieDTO> buscarFilmesPopulares() {
        List<String> topMovieIds = reviewRepository.findMostReviewedMovies(
                PageRequest.of(0, TOP_MOVIES_LIMIT));
        return topMovieIds.stream()
                .map(this::buildPopularMovieDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public RatingExtremesDTO buscarExtremosDeNota() {
        List<String> topIds    = reviewRepository.findHighestRatedMovieId(PageRequest.of(0, SINGLE_RESULT));
        List<String> bottomIds = reviewRepository.findLowestRatedMovieId(PageRequest.of(0, SINGLE_RESULT));

        RatingExtremesDTO dto = new RatingExtremesDTO();
        if (!topIds.isEmpty())    dto.setHighest(buildPopularMovieDTO(topIds.get(0)));
        if (!bottomIds.isEmpty()) dto.setLowest(buildPopularMovieDTO(bottomIds.get(0)));
        return dto;
    }


    @Transactional(readOnly = true)
    public List<RatingDistributionDTO> distribuicaoDeNotas() {
        List<Double> todasAsNotas = List.of(0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0);
        Map<Double, Long> contagem = reviewRepository.findRatingCounts()
                .stream()
                .collect(Collectors.toMap(row -> (Double) row[0], row -> (Long) row[1]));
        return todasAsNotas.stream()
                .map(nota -> new RatingDistributionDTO(nota, contagem.getOrDefault(nota, 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewsPerDayDTO> reviewsPorDia() {
        LocalDateTime inicioDaSemana = LocalDate.now().minusDays(6).atStartOfDay();
        DateTimeFormatter formatter  = DateTimeFormatter.ofPattern("dd/MM");
        Map<LocalDate, Long> contagem = reviewRepository.findRecentReviews(inicioDaSemana)
                .stream()
                .collect(Collectors.groupingBy(r -> r.getCreatedAt().toLocalDate(), Collectors.counting()));
        List<ReviewsPerDayDTO> resultado = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate data = LocalDate.now().minusDays(i);
            resultado.add(new ReviewsPerDayDTO(data.format(formatter), contagem.getOrDefault(data, 0L)));
        }
        return resultado;
    }

    @Cacheable("admin-genres")
    @Transactional(readOnly = true)
    public List<GenreCountDTO> generosMaisAvaliados() {
        List<String> topMovieIds = reviewRepository.findMostReviewedMovies(PageRequest.of(0, 15));
        Map<String, Long> contagemPorGenero = new HashMap<>();
        for (String movieId : topMovieIds) {
            try {
                MovieDTO movie        = movieService.buscarPorId(movieId);
                long     totalReviews = reviewRepository.countByMovieId(movieId);
                if (movie.getGenre() != null && !movie.getGenre().isBlank()) {
                    for (String genero : movie.getGenre().split(",")) {
                        String nome = genero.trim();
                        if (!nome.isBlank()) contagemPorGenero.merge(nome, totalReviews, Long::sum);
                    }
                }
            } catch (Exception e) {
                log.warn("Erro ao buscar gêneros para movieId={}: {}", movieId, e.getMessage());
            }
        }
        return contagemPorGenero.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(e -> new GenreCountDTO(e.getKey(), e.getValue()))
                .toList();
    }

    @Transactional
    public void deletarUsuario(String userId) {
        if (!userRepository.existsById(userId)) throw new ResourceNotFoundException("Usuário", userId);
        userRepository.deleteById(userId);
        log.info("Admin: usuário deletado id={}", userId);
    }

    @Transactional
    public void deletarReview(String reviewId) {
        if (!reviewRepository.existsById(reviewId)) throw new ResourceNotFoundException("Review", reviewId);
        reviewRepository.deleteById(reviewId);
        log.info("Admin: review deletada id={}", reviewId);
    }


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
        boolean semTitulo = dto.getMovieTitle() == null || dto.getMovieTitle().isBlank();
        boolean semPoster = dto.getMoviePoster() == null;
        if (!semTitulo && !semPoster) return;
        try {
            MovieDTO movie = movieService.buscarPorId(movieId);
            if (semTitulo) dto.setMovieTitle(movie.getTitle());
            if (semPoster) dto.setMoviePoster(movie.getPoster());
        } catch (Exception e) {
            log.warn("Não foi possível enriquecer movieId={}: {}", movieId, e.getMessage());
            if (dto.getMovieTitle() == null) dto.setMovieTitle("Filme #" + movieId);
        }
    }

    private void preencherEstatisticas(PopularMovieDTO dto, String movieId) {
        dto.setReviewCount(reviewRepository.countByMovieId(movieId));
        Double media = reviewRepository.avgRatingByMovieId(movieId);
        dto.setAvgRating(media != null ? Math.round(media * ROUNDING_FACTOR) / ROUNDING_FACTOR : 0.0);
    }
}
