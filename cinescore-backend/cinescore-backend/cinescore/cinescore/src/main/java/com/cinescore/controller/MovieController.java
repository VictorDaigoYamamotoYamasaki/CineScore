package com.cinescore.controller;

import com.cinescore.dto.ActorDetailsDTO;
import com.cinescore.dto.ActorSearchItemDTO;
import com.cinescore.dto.MovieDTO;
import com.cinescore.dto.RecommendationDTO;
import com.cinescore.dto.MovieSearchItemDTO;
import com.cinescore.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping("/search")
    public ResponseEntity<List<MovieSearchItemDTO>> buscarPorTitulo(@RequestParam String title) {
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(movieService.buscarPorTitulo(title));
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<MovieDTO> buscarPorId(@PathVariable String movieId) {
        return ResponseEntity.ok(movieService.buscarPorId(movieId));
    }

    @GetMapping("/trending")
    public ResponseEntity<List<RecommendationDTO>> trending() {
        return ResponseEntity.ok(movieService.buscarTrending());
    }

    @GetMapping("/actors/search")
    public ResponseEntity<List<ActorSearchItemDTO>> buscarAtor(@RequestParam String name) {
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(movieService.buscarAtorPorNome(name));
    }

    @GetMapping("/actors/{actorId}")
    public ResponseEntity<ActorDetailsDTO> buscarAtorPorId(@PathVariable String actorId) {
        return ResponseEntity.ok(movieService.buscarDetalhesPorAtor(actorId));
    }
}
