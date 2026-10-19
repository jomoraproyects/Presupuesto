package com.serviciudad.presupuesto.dto;
import jakarta.validation.constraints.NotNull;
public record MapeoRequest(@NotNull Long rubroOrigenId, @NotNull Long servicioId, @NotNull Long rubroDestinoId) {}
