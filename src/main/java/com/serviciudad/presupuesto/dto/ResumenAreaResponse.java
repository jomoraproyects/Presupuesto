package com.serviciudad.presupuesto.dto;

import com.serviciudad.presupuesto.enums.EstadoNecesidadPresupuestal;
import java.math.BigDecimal;
import java.util.Map;

public record ResumenAreaResponse(Long vigenciaId, Long areaId, String area,
    BigDecimal totalSolicitado, BigDecimal totalAprobado, BigDecimal diferencia,
    long numeroNecesidades, Map<EstadoNecesidadPresupuestal, Long> porEstado) {}
