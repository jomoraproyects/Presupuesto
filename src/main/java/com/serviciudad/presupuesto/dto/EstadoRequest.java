package com.serviciudad.presupuesto.dto;
import jakarta.validation.constraints.NotNull;
public record EstadoRequest(@NotNull Boolean activo) {}
