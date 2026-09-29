package com.serviciudad.presupuesto.dto;
import java.time.LocalDateTime;
public record AreaResponse(Long id, String codigo, String nombre, String descripcion, boolean activa,
                           Long areaPadreId, LocalDateTime createdAt, LocalDateTime updatedAt) {}
