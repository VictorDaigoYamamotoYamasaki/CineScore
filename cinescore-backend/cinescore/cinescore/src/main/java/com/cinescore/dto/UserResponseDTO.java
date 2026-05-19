package com.cinescore.dto;

import com.cinescore.model.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserResponseDTO {

    private Long id;
    private String name;
    private String email;
    private String role;
    private LocalDateTime createdAt;

    public static UserResponseDTO fromUser(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
