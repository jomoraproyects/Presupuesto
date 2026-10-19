package com.serviciudad.presupuesto.dto;
import java.math.BigDecimal;
import java.util.List;
public record MaestroResponse(Long vigenciaId, BigDecimal totalSolicitado, BigDecimal totalAprobado,
    BigDecimal diferencia, boolean mapeoCompleto, List<MapeoPendienteResponse> mapeosPendientes,
    List<MaestroRubroResponse> rubros) {}
