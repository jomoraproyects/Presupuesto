package com.serviciudad.presupuesto.dto;
import java.math.BigDecimal;
public record MapeoPendienteResponse(Long rubroOrigenId, String rubroOrigen, Long servicioId, String servicio,
    BigDecimal solicitado, BigDecimal aprobado) {}
