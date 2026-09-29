package com.serviciudad.presupuesto.dto;

import com.serviciudad.presupuesto.enums.EstadoNecesidadPresupuestal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record NecesidadResponse(Long id, Long vigenciaId, Long areaId, Long rubroPresupuestalId,
    String descripcion, String justificacion, String unidadMedida,
    BigDecimal cantidadSolicitada, BigDecimal valorUnitarioSolicitado, BigDecimal valorTotalSolicitado,
    BigDecimal cantidadSolicitadaOriginal, BigDecimal valorUnitarioSolicitadoOriginal, BigDecimal valorTotalSolicitadoOriginal,
    BigDecimal cantidadAprobada, BigDecimal valorUnitarioAprobado, BigDecimal valorTotalAprobado,
    EstadoNecesidadPresupuestal estado, String observacionPresupuesto,
    Long creadoPorId, Long revisadoPorId, boolean activa, long version,
    List<DistribucionResponse> distribuciones, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {}
