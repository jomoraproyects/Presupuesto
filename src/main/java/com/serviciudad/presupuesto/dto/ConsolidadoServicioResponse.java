package com.serviciudad.presupuesto.dto;

import java.math.BigDecimal;

public record ConsolidadoServicioResponse(Long servicioId, String servicio, BigDecimal solicitado,
    BigDecimal aprobado, BigDecimal diferencia) {}
