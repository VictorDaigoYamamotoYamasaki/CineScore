package com.cinescore.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class PageResponseDTO<T> {
    private List<T> content;
    private int     currentPage;
    private int     totalPages;
    private long    totalElements;

    public static <T> PageResponseDTO<T> fromPage(Page<T> page) {
        PageResponseDTO<T> dto = new PageResponseDTO<>();
        dto.setContent(page.getContent());
        dto.setCurrentPage(page.getNumber());
        dto.setTotalPages(Math.max(page.getTotalPages(), 1));
        dto.setTotalElements(page.getTotalElements());
        return dto;
    }

    public static <T> PageResponseDTO<T> of(List<T> all, int page, int size) {
        int total      = all.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int from       = Math.min(page * size, total);
        int to         = Math.min(from + size, total);
        PageResponseDTO<T> dto = new PageResponseDTO<>();
        dto.setContent(all.subList(from, to));
        dto.setCurrentPage(page);
        dto.setTotalPages(Math.max(totalPages, 1));
        dto.setTotalElements(total);
        return dto;
    }
}
