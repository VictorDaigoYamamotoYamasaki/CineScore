package com.cinescore.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProfileDTO {
    private Long userId;
    private String name;
    private String email;
    private long followerCount;
    private long followingCount;
    private boolean isFollowing;
    private List<FavoriteMovieDTO> favorites;
    private List<ReviewResponseDTO> reviews;
}
