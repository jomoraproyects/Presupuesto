package com.serviciudad.presupuesto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record SubgerenciaCreateRequest(@NotNull @Valid AreaRequest area,
    @NotNull @Valid Responsable responsable, Long vigenciaId) {
    public record Responsable(@NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(max = 100) String username, @NotBlank @Size(min = 11, max = 100) String password) {}
}
