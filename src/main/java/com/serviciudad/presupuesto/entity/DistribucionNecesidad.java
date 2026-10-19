package com.serviciudad.presupuesto.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "distribucion_necesidad", uniqueConstraints = @UniqueConstraint(name = "uq_distribucion_servicio", columnNames = {"necesidad_id", "servicio_id"}))
@Getter @Setter
public class DistribucionNecesidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "necesidad_id", nullable = false)
    private NecesidadPresupuestal necesidad;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;
    @Column(name = "porcentaje_aprobado", precision = 5, scale = 2)
    private BigDecimal porcentajeAprobado;
    @Column(name = "valor_solicitado_distribuido", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorSolicitadoDistribuido;
    @Column(name = "valor_aprobado_distribuido", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorAprobadoDistribuido;
}
