package com.serviciudad.presupuesto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ObservacionRequest(@NotBlank @Size(max = 2000) String observacionPresupuesto) {}
