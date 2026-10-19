package com.serviciudad.presupuesto.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
public record CopiarVigenciaRequest(@NotNull @Valid VigenciaRequest vigencia, boolean copiarParticipantes) {}
