package com.serviciudad.presupuesto.dto;
import java.time.LocalDateTime;
public record RubroResponse(Long id, String codigo, String nombre, String descripcion, Long rubroPadreId,
                            int nivel, boolean permiteMovimiento, boolean activo, Long vigenciaId, int orden,
                            LocalDateTime createdAt, LocalDateTime updatedAt, boolean categoriaFormulacion) {}
