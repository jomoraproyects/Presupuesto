package com.serviciudad.presupuesto.dto;

import com.serviciudad.presupuesto.enums.AccionHistorial;
import com.serviciudad.presupuesto.enums.EstadoNecesidadPresupuestal;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistorialNecesidadResponse(Long id, Long usuarioId, AccionHistorial accion, String descripcion,
    EstadoNecesidadPresupuestal estadoAnterior, EstadoNecesidadPresupuestal estadoNuevo,
    BigDecimal valorSolicitado, BigDecimal valorAprobadoAnterior, BigDecimal valorAprobadoNuevo,
    String detalleAnterior, String detalleNuevo, LocalDateTime fecha) {}
