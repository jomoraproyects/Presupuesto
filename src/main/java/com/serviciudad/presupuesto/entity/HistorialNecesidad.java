package com.serviciudad.presupuesto.entity;

import com.serviciudad.presupuesto.enums.AccionHistorial;
import com.serviciudad.presupuesto.enums.EstadoNecesidadPresupuestal;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "historial_necesidad")
@Getter @Setter
public class HistorialNecesidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "necesidad_id", nullable = false)
    private NecesidadPresupuestal necesidad;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private AccionHistorial accion;
    @Column(nullable = false, length = 2000)
    private String descripcion;
    @Enumerated(EnumType.STRING) @Column(name = "estado_anterior", length = 30)
    private EstadoNecesidadPresupuestal estadoAnterior;
    @Enumerated(EnumType.STRING) @Column(name = "estado_nuevo", nullable = false, length = 30)
    private EstadoNecesidadPresupuestal estadoNuevo;
    @Column(name = "valor_solicitado", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorSolicitado;
    @Column(name = "valor_aprobado_anterior", precision = 19, scale = 2)
    private BigDecimal valorAprobadoAnterior;
    @Column(name = "valor_aprobado_nuevo", precision = 19, scale = 2)
    private BigDecimal valorAprobadoNuevo;
    @Column(name = "detalle_anterior", columnDefinition = "TEXT")
    private String detalleAnterior;
    @Column(name = "detalle_nuevo", nullable = false, columnDefinition = "TEXT")
    private String detalleNuevo;
    @Column(nullable = false)
    private LocalDateTime fecha;
}
