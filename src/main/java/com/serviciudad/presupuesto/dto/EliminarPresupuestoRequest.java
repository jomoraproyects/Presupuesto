package com.serviciudad.presupuesto.dto;

import jakarta.validation.constraints.NotBlank;

public record EliminarPresupuestoRequest(@NotBlank String confirmacion) {}
