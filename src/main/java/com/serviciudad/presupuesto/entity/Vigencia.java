package com.serviciudad.presupuesto.entity;

import com.serviciudad.presupuesto.enums.EstadoVigencia;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Vigencia extends Auditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Integer anio;
    @Column(nullable = false, length = 150)
    private String nombre;
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;
    @Column(name = "presupuesto_disponible", nullable = false, precision = 19, scale = 2)
    private BigDecimal presupuestoDisponible;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoVigencia estado = EstadoVigencia.BORRADOR;
    @Column(nullable = false)
    private boolean activa = true;
}
