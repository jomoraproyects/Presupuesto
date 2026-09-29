package com.serviciudad.presupuesto.dto;

import com.serviciudad.presupuesto.enums.NombreRol;
import java.time.LocalDateTime;

public record UsuarioResponse(Long id, String username, String nombre, NombreRol rol, Long areaId,
    boolean activo, LocalDateTime createdAt, LocalDateTime updatedAt) {}
