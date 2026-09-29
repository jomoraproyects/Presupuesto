package com.serviciudad.presupuesto.entity;

import com.serviciudad.presupuesto.enums.EstadoNecesidadPresupuestal;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "necesidad_presupuestal")
@Getter @Setter
public class NecesidadPresupuestal extends Auditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "vigencia_id", nullable = false)
    private Vigencia vigencia;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "area_id", nullable = false)
    private Area area;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "rubro_presupuestal_id", nullable = false)
    private RubroPresupuestal rubroPresupuestal;
    @Column(nullable = false, length = 500)
    private String descripcion;
    @Column(nullable = false, length = 2000)
    private String justificacion;
    @Column(name = "unidad_medida", nullable = false, length = 60)
    private String unidadMedida;
    @Column(name = "cantidad_solicitada", nullable = false, precision = 19, scale = 4)
    private BigDecimal cantidadSolicitada;
    @Column(name = "valor_unitario_solicitado", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorUnitarioSolicitado;
    @Column(name = "valor_total_solicitado", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotalSolicitado;
    @Column(name = "cantidad_solicitada_original", precision = 19, scale = 4)
    private BigDecimal cantidadSolicitadaOriginal;
    @Column(name = "valor_unitario_solicitado_original", precision = 19, scale = 2)
    private BigDecimal valorUnitarioSolicitadoOriginal;
    @Column(name = "valor_total_solicitado_original", precision = 19, scale = 2)
    private BigDecimal valorTotalSolicitadoOriginal;
    @Column(name = "cantidad_aprobada", precision = 19, scale = 4)
    private BigDecimal cantidadAprobada;
    @Column(name = "valor_unitario_aprobado", precision = 19, scale = 2)
    private BigDecimal valorUnitarioAprobado;
    @Column(name = "valor_total_aprobado", precision = 19, scale = 2)
    private BigDecimal valorTotalAprobado;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private EstadoNecesidadPresupuestal estado = EstadoNecesidadPresupuestal.BORRADOR;
    @Column(name = "observacion_presupuesto", length = 2000)
    private String observacionPresupuesto;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "creado_por_id", nullable = false)
    private Usuario creadoPor;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "revisado_por_id")
    private Usuario revisadoPor;
    @Column(nullable = false)
    private boolean activa = true;
    @Version @Column(nullable = false)
    private long version;
    @OneToMany(mappedBy = "necesidad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DistribucionNecesidad> distribuciones = new ArrayList<>();
}
