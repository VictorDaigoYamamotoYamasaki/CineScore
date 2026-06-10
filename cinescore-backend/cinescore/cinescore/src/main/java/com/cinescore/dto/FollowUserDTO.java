package com.cinescore.dto;

import lombok.Data;

@Data
public class FollowUserDTO {
    private String userId;
    private String name;
    private long reviewCount;
    private boolean isFollowing;
}
