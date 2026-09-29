package com.serviciudad.presupuesto.dto;
import com.serviciudad.presupuesto.enums.EstadoVigencia;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record VigenciaResponse(Long id, Integer anio, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
                               BigDecimal presupuestoDisponible, EstadoVigencia estado, boolean activa,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {}
