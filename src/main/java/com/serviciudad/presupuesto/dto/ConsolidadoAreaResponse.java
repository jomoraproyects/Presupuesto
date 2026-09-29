package com.serviciudad.presupuesto.dto;

import java.math.BigDecimal;

public record ConsolidadoAreaResponse(Long areaId, String area, BigDecimal solicitado,
    BigDecimal aprobado, BigDecimal diferencia) {}
