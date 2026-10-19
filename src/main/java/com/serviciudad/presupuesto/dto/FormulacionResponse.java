package com.serviciudad.presupuesto.dto;
import java.math.BigDecimal;
import java.util.List;
import com.serviciudad.presupuesto.enums.EstadoFormulario;
public record FormulacionResponse(Long vigenciaId, Long areaId, String area, boolean puedeCrear, boolean enviada,
    BigDecimal solicitado, BigDecimal aprobado, BigDecimal diferencia, List<FormulacionRubroResponse> rubros,
    java.time.LocalDateTime fechaEnvio, Long enviadoPorId, String enviadoPor,
    boolean disponible, EstadoFormulario estado) {}
