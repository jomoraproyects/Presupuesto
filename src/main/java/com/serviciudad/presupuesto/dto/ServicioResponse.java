package com.serviciudad.presupuesto.dto;
import java.time.LocalDateTime;
public record ServicioResponse(Long id, String codigo, String nombre, String descripcion, boolean activo,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {}
