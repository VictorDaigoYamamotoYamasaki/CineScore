package com.cinescore.controller;

import com.cinescore.dto.MovieDTO;
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
    public ResponseEntity<List<MovieSearchItemDTO>> buscarPorTitulo(
            @RequestParam String title) {

        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(movieService.buscarPorTitulo(title));
    }

    @GetMapping("/{imdbId}")
    public ResponseEntity<MovieDTO> buscarPorId(@PathVariable String imdbId) {
        return ResponseEntity.ok(movieService.buscarPorImdbId(imdbId));
    }
}
