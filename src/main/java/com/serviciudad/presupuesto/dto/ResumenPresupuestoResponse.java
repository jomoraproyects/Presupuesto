package com.serviciudad.presupuesto.dto;

import java.math.BigDecimal;

public record ResumenPresupuestoResponse(Long vigenciaId, BigDecimal presupuestoDisponible,
    BigDecimal totalSolicitado, BigDecimal totalAprobado, BigDecimal diferenciaContraTecho) {}
