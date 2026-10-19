package com.serviciudad.presupuesto.dto;
import jakarta.validation.constraints.*;
public record RubroRequest(@NotBlank @Size(max=60) String codigo, @NotBlank @Size(max=200) String nombre,
                           @Size(max=500) String descripcion, Long rubroPadreId, boolean permiteMovimiento,
                           Long vigenciaId, @Min(0) int orden, Boolean categoriaFormulacion) {}
