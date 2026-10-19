package com.serviciudad.presupuesto.dto;
import java.math.BigDecimal;
import java.util.List;
public record MaestroRubroResponse(Long rubroId, String codigo, String nombre, int nivel,
    BigDecimal solicitado, BigDecimal aprobado, BigDecimal diferencia, List<MaestroRubroResponse> hijos,
    BigDecimal solicitadoGeneral, BigDecimal aprobadoGeneral) {}
