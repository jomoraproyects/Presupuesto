package com.serviciudad.presupuesto.dto;

import com.serviciudad.presupuesto.enums.NombreRol;
import jakarta.validation.constraints.*;

public record UsuarioCreateRequest(@NotBlank @Size(max = 100) String username,
    @NotBlank @Size(min = 12, max = 100) String password,
    @NotBlank @Size(max = 150) String nombre,
    @NotNull NombreRol rol, Long areaId) {}
