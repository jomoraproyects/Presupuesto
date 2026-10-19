package com.serviciudad.presupuesto.dto;

import com.serviciudad.presupuesto.enums.NombreRol;

public record PerfilResponse(Long id, String username, String nombre, NombreRol rol, Long areaId) {}
