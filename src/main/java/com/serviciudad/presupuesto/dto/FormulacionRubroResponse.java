package com.serviciudad.presupuesto.dto;
import java.math.BigDecimal;
import java.util.List;
public record FormulacionRubroResponse(Long rubroId, String codigo, String nombre, boolean habilitado,
    BigDecimal solicitado, BigDecimal aprobado, BigDecimal diferencia, int cantidadNecesidades,
    List<NecesidadResponse> necesidades, int orden) {}
