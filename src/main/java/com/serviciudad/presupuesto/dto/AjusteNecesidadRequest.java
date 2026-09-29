package com.serviciudad.presupuesto.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AjusteNecesidadRequest(
    @DecimalMin("0.0000") @Digits(integer = 15, fraction = 4) BigDecimal cantidadAprobada,
    @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal valorUnitarioAprobado,
    @Size(max = 2000) String observacionPresupuesto) {}
