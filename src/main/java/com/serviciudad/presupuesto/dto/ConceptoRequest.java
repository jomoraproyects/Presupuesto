package com.serviciudad.presupuesto.dto;

import jakarta.validation.constraints.*;

/** Administración sencilla de las categorías existentes, sin pedir códigos finales. */
public record ConceptoRequest(@NotBlank @Size(max=200) String nombre, @Min(0) int orden, Long vigenciaId,
    @Size(max=500) String descripcion, Boolean activo) {}
