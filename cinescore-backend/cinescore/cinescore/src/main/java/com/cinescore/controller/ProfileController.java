package com.cinescore.controller;

import com.cinescore.dto.FavoriteMovieDTO;
import com.cinescore.dto.ProfileUpdateDTO;
import com.cinescore.dto.UserResponseDTO;
import com.cinescore.dto.FavoriteMovieRequestDTO;
import com.cinescore.dto.ProfileDTO;
import com.cinescore.model.User;
import com.cinescore.service.ProfileService;
import com.cinescore.service.UserService;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ProfileDTO> meuPerfil(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(profileService.buscarPerfil(currentUser.getId(), currentUser.getId()));
    }

    @PutMapping("/me/settings")
    public ResponseEntity<UserResponseDTO> atualizarPerfil(
            @AuthenticationPrincipal User currentUser,
            @jakarta.validation.Valid @RequestBody ProfileUpdateDTO dto) {
        return ResponseEntity.ok(userService.atualizarPerfil(currentUser.getId(), dto));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ProfileDTO> perfilPorId(
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        Long viewerId = currentUser != null ? currentUser.getId() : null;
        return ResponseEntity.ok(profileService.buscarPerfil(userId, viewerId));
    }

    @PutMapping("/favorites/{position}")
    public ResponseEntity<FavoriteMovieDTO> salvarFavorito(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Integer position,
            @RequestBody FavoriteMovieRequestDTO req) {
        return ResponseEntity.ok(profileService.salvarFavorito(currentUser.getId(), position, req));
    }

    @DeleteMapping("/favorites/{position}")
    public ResponseEntity<Void> removerFavorito(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Integer position) {
        profileService.removerFavorito(currentUser.getId(), position);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deletarMinhaConta(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "false") boolean deletarReviews) {
        userService.deletarConta(currentUser.getId(), deletarReviews);
        return ResponseEntity.noContent().build();
    }

}