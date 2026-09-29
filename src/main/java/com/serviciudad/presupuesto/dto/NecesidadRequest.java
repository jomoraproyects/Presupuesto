package com.serviciudad.presupuesto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record NecesidadRequest(
    @NotNull Long vigenciaId,
    Long areaId,
    @NotNull Long rubroPresupuestalId,
    @NotBlank @Size(max = 500) String descripcion,
    @NotBlank @Size(max = 2000) String justificacion,
    @NotBlank @Size(max = 60) String unidadMedida,
    @NotNull @DecimalMin("0.0001") @Digits(integer = 15, fraction = 4) BigDecimal cantidadSolicitada,
    @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal valorUnitarioSolicitado,
    List<@NotNull @Valid DistribucionRequest> distribuciones) {}
