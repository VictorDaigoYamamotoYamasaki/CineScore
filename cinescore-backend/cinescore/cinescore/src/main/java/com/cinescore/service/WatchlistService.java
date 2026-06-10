package com.cinescore.service;

import com.cinescore.dto.WatchlistItemDTO;
import com.cinescore.dto.WatchlistRequestDTO;
import com.cinescore.exception.DuplicateResourceException;
import com.cinescore.exception.ResourceNotFoundException;
import com.cinescore.model.User;
import com.cinescore.model.WatchlistItem;
import com.cinescore.repository.UserRepository;
import com.cinescore.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository      userRepository;

    @Transactional(readOnly = true)
    public List<WatchlistItemDTO> listarPorUsuario(String userId) {
        return watchlistRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(WatchlistItemDTO::fromItem).toList();
    }

    @Transactional
    public WatchlistItemDTO adicionar(String userId, WatchlistRequestDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));

        if (watchlistRepository.existsByUserIdAndMovieId(userId, dto.getMovieId())) {
            throw new DuplicateResourceException("Watchlist", "filme", dto.getMovieId());
        }

        WatchlistItem saved = watchlistRepository.save(buildItem(user, dto));
        log.info("Filme {} adicionado à watchlist do usuário {}", dto.getMovieId(), userId);
        return WatchlistItemDTO.fromItem(saved);
    }

    @Transactional
    public void remover(String userId, String movieId) {
        if (!watchlistRepository.existsByUserIdAndMovieId(userId, movieId)) {
            throw new ResourceNotFoundException("Watchlist", "movieId", movieId);
        }
        watchlistRepository.deleteByUserIdAndMovieId(userId, movieId);
        log.info("Filme {} removido da watchlist do usuário {}", movieId, userId);
    }

    private WatchlistItem buildItem(User user, WatchlistRequestDTO dto) {
        return WatchlistItem.builder()
                .user(user)
                .movieId(dto.getMovieId())
                .movieTitle(dto.getMovieTitle())
                .moviePoster(dto.getMoviePoster())
                .movieYear(dto.getMovieYear())
                .movieVoteAverage(dto.getMovieVoteAverage())
                .build();
    }
}
