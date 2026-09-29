package com.serviciudad.presupuesto.dto;

import java.math.BigDecimal;

public record ConsolidadoRubroResponse(Long rubroId, Long rubroPadreId, String codigo, String rubro,
    int nivel, BigDecimal solicitado, BigDecimal aprobado, BigDecimal diferencia) {}
