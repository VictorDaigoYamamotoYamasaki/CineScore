package com.cinescore.dto;

import com.cinescore.model.WatchlistItem;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WatchlistItemDTO {

    private String        id;
    private String        movieId;
    private String        movieTitle;
    private String        moviePoster;
    private String        movieYear;
    private Double        movieVoteAverage;
    private LocalDateTime createdAt;

    public static WatchlistItemDTO fromItem(WatchlistItem item) {
        WatchlistItemDTO dto = new WatchlistItemDTO();
        dto.setId(item.getId());
        dto.setMovieId(item.getMovieId());
        dto.setMovieTitle(item.getMovieTitle());
        dto.setMoviePoster(item.getMoviePoster());
        dto.setMovieYear(item.getMovieYear());
        dto.setMovieVoteAverage(item.getMovieVoteAverage());
        dto.setCreatedAt(item.getCreatedAt());
        return dto;
    }
}
