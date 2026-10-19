package com.serviciudad.presupuesto.dto;

import java.math.BigDecimal;
import com.serviciudad.presupuesto.enums.EstadoFormulario;

public record ConsolidadoAreaResponse(Long areaId, String area, BigDecimal solicitado,
    BigDecimal aprobado, BigDecimal diferencia, boolean enviada, EstadoFormulario estado) {}
