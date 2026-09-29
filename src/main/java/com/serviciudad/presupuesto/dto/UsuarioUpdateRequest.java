package com.serviciudad.presupuesto.dto;

import com.serviciudad.presupuesto.enums.NombreRol;
import jakarta.validation.constraints.*;

public record UsuarioUpdateRequest(@NotBlank @Size(max = 150) String nombre,
    @NotNull NombreRol rol, Long areaId) {}
