package com.cinescore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GenreCountDTO {
    private String genre;
    private Long   count;
}
