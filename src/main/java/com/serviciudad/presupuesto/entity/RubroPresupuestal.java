package com.serviciudad.presupuesto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rubro_presupuestal")
@Getter @Setter
public class RubroPresupuestal extends Auditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 60)
    private String codigo;
    @Column(nullable = false, length = 200)
    private String nombre;
    @Column(length = 500)
    private String descripcion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubro_padre_id")
    private RubroPresupuestal rubroPadre;
    @Column(nullable = false)
    private int nivel;
    @Column(name = "permite_movimiento", nullable = false)
    private boolean permiteMovimiento;
    @Column(name = "categoria_formulacion", nullable = false)
    private boolean categoriaFormulacion = true;
    @Column(nullable = false)
    private boolean activo = true;
    @Column(nullable = false)
    private boolean eliminado;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vigencia_id")
    private Vigencia vigencia;
    @Column(nullable = false)
    private int orden;
}
