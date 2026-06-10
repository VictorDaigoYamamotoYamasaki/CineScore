package com.cinescore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequestDTO {

    @NotBlank(message = "Comentário não pode ser vazio")
    @Size(max = 500, message = "Comentário deve ter no máximo 500 caracteres")
    private String text;
}
