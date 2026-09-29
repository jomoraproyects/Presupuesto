package com.serviciudad.presupuesto.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <T> PaginaResponse<T> from(Page<T> source) {
        return new PaginaResponse<>(source.getContent(), source.getNumber(), source.getSize(),
            source.getTotalElements(), source.getTotalPages());
    }
}
