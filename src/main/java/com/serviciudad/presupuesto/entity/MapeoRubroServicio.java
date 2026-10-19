package com.serviciudad.presupuesto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "mapeo_rubro_servicio", uniqueConstraints = @UniqueConstraint(columnNames = {"vigencia_id", "rubro_origen_id", "servicio_id"}))
@Getter @Setter
public class MapeoRubroServicio extends Auditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "vigencia_id", nullable = false)
    private Vigencia vigencia;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "rubro_origen_id", nullable = false)
    private RubroPresupuestal rubroOrigen;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "rubro_destino_id", nullable = false)
    private RubroPresupuestal rubroDestino;
    @Column(nullable = false) private boolean activo = true;
}
