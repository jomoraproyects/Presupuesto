package com.serviciudad.presupuesto.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CrearPresupuestoRequest(@NotNull @Min(2000) Integer anio) {}
