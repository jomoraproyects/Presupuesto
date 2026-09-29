package com.serviciudad.presupuesto.dto;
import com.serviciudad.presupuesto.enums.EstadoVigencia;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record VigenciaRequest(@NotNull @Min(2000) Integer anio, @NotBlank @Size(max=150) String nombre,
                              @NotNull LocalDate fechaInicio, @NotNull LocalDate fechaFin,
                              @NotNull @DecimalMin("0.00") @Digits(integer=17, fraction=2) BigDecimal presupuestoDisponible,
                              @NotNull EstadoVigencia estado) {}
