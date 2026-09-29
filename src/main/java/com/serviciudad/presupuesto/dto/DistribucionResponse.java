package com.serviciudad.presupuesto.dto;

import java.math.BigDecimal;

public record DistribucionResponse(Long id, Long servicioId, String servicio, BigDecimal porcentaje,
    BigDecimal valorSolicitadoDistribuido, BigDecimal valorAprobadoDistribuido) {}
