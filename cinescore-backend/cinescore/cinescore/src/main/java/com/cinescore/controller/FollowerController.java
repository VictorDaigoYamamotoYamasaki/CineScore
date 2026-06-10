package com.cinescore.controller;

import com.cinescore.dto.FollowUserDTO;
import com.cinescore.model.User;
import com.cinescore.service.FollowerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowerController {

    private final FollowerService followerService;

    @PostMapping("/{userId}")
    public ResponseEntity<Void> follow(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String userId) {
        followerService.seguir(currentUser.getId(), userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unfollow(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String userId) {
        followerService.deixarDeSeguir(currentUser.getId(), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/status")
    public ResponseEntity<Map<String, Object>> status(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String userId) {
        boolean following    = followerService.verificarSeguindo(currentUser.getId(), userId);
        long followerCount   = followerService.contarSeguidores(userId);
        long followingCount  = followerService.contarSeguindo(userId);
        return ResponseEntity.ok(Map.of(
                "isFollowing",    following,
                "followerCount",  followerCount,
                "followingCount", followingCount
        ));
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<List<FollowUserDTO>> listarSeguidores(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String userId) {
        return ResponseEntity.ok(followerService.listarSeguidores(userId, currentUser.getId()));
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<List<FollowUserDTO>> listarSeguindo(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String userId) {
        return ResponseEntity.ok(followerService.listarSeguindo(userId, currentUser.getId()));
    }
}
