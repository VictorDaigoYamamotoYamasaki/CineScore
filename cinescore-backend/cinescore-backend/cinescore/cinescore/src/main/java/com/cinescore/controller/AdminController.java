package com.cinescore.controller;

import com.cinescore.dto.AdminStatsDTO;
import com.cinescore.dto.PopularMovieDTO;
import com.cinescore.dto.PageResponseDTO;
import com.cinescore.dto.ReviewResponseDTO;
import com.cinescore.dto.UserResponseDTO;
import com.cinescore.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/popular-movies")
    public ResponseEntity<java.util.List<PopularMovieDTO>> filmesPopulares() {
        return ResponseEntity.ok(adminService.buscarFilmesPopulares());
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDTO> stats() {
        return ResponseEntity.ok(adminService.buscarEstatisticas());
    }

    @GetMapping("/users")
    public ResponseEntity<PageResponseDTO<UserResponseDTO>> listarUsuarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.listarUsuariosPaginados(page, size));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id) {
        adminService.deletarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reviews")
    public ResponseEntity<PageResponseDTO<ReviewResponseDTO>> listarReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.listarReviewsPaginadas(page, size));
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deletarReview(@PathVariable Long id) {
        adminService.deletarReview(id);
        return ResponseEntity.noContent().build();
    }
}
