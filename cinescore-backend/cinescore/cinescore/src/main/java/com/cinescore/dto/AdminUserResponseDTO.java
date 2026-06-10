package com.cinescore.dto;

import com.cinescore.model.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserResponseDTO {

    private String        id;
    private String        name;
    private String        emailMascarado;
    private String        role;
    private LocalDateTime createdAt;

    public static AdminUserResponseDTO fromUser(User user) {
        AdminUserResponseDTO dto = new AdminUserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmailMascarado(mascararEmail(user.getEmail()));
        dto.setRole(user.getRole());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }

    private static String mascararEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        if (email.endsWith("@removed.invalid"))      return "[conta removida]";

        String[] partes  = email.split("@");
        String   local   = partes[0];
        String   dominio = partes[1];

        String visivel = local.length() <= 2
                ? local.substring(0, 1) + "***"
                : local.substring(0, 2) + "***";

        return visivel + "@" + dominio;
    }
}
