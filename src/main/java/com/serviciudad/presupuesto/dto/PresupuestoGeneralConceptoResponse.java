package com.serviciudad.presupuesto.dto;

import java.math.BigDecimal;
import java.util.List;

public record PresupuestoGeneralConceptoResponse(Long conceptoId, String nombre, int orden,
    BigDecimal solicitado, BigDecimal aprobado, BigDecimal diferencia, List<DependenciaConcepto> dependencias) {
    public record DependenciaConcepto(Long areaId, String area, BigDecimal solicitado,
        BigDecimal aprobado, BigDecimal diferencia) {}
}
