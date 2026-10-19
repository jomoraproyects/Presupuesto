package com.serviciudad.presupuesto.dto;
public record MapeoResponse(Long id, Long vigenciaId, Long rubroOrigenId, String rubroOrigen,
    Long servicioId, String servicio, Long rubroDestinoId, String codigoDestino, String rubroDestino, boolean activo) {}
