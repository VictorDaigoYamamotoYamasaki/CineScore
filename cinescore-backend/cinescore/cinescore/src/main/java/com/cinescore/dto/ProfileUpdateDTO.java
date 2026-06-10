package com.cinescore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 25, message = "Nome deve ter no máximo 25 caracteres")
    private String name;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @Pattern(
        regexp = "^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{6,}$",
        message = "Senha deve conter pelo menos 1 maiúscula, 1 minúscula e 1 caractere especial"
    )
    private String password;
}
