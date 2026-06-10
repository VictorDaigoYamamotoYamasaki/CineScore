package com.cinescore.controller;

import com.cinescore.dto.AdminStatsDTO;
import com.cinescore.dto.AdminUserResponseDTO;
import com.cinescore.dto.GenreCountDTO;
import com.cinescore.dto.PageResponseDTO;
import com.cinescore.dto.PopularMovieDTO;
import com.cinescore.dto.RatingDistributionDTO;
import com.cinescore.dto.RatingExtremesDTO;
import com.cinescore.dto.ReviewResponseDTO;
import com.cinescore.dto.ReviewsPerDayDTO;
import com.cinescore.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDTO> stats() {
        return ResponseEntity.ok(adminService.buscarEstatisticas());
    }

    @GetMapping("/popular-movies")
    public ResponseEntity<List<PopularMovieDTO>> filmesPopulares() {
        return ResponseEntity.ok(adminService.buscarFilmesPopulares());
    }

    @GetMapping("/stats/rating-distribution")
    public ResponseEntity<List<RatingDistributionDTO>> distribuicaoNotas() {
        return ResponseEntity.ok(adminService.distribuicaoDeNotas());
    }

    @GetMapping("/stats/reviews-per-day")
    public ResponseEntity<List<ReviewsPerDayDTO>> reviewsPorDia() {
        return ResponseEntity.ok(adminService.reviewsPorDia());
    }

    @GetMapping("/stats/genres")
    public ResponseEntity<List<GenreCountDTO>> generos() {
        return ResponseEntity.ok(adminService.generosMaisAvaliados());
    }

    @GetMapping("/stats/rating-extremes")
    public ResponseEntity<RatingExtremesDTO> extremosDeNota() {
        return ResponseEntity.ok(adminService.buscarExtremosDeNota());
    }

    @GetMapping("/users")
    public ResponseEntity<PageResponseDTO<AdminUserResponseDTO>> listarUsuarios(
            @RequestParam(defaultValue = "0")   int    page,
            @RequestParam(defaultValue = "30")  int    size,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort.Direction dir = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return ResponseEntity.ok(adminService.listarUsuariosPaginados(page, size, dir));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable String id) {
        adminService.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reviews")
    public ResponseEntity<PageResponseDTO<ReviewResponseDTO>> listarReviews(
            @RequestParam(defaultValue = "0")   int    page,
            @RequestParam(defaultValue = "30")  int    size,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort.Direction dir = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return ResponseEntity.ok(adminService.listarReviewsPaginadas(page, size, dir));
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deletarReview(@PathVariable String id) {
        adminService.deletarReview(id);
        return ResponseEntity.noContent().build();
    }
}
