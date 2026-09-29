package com.serviciudad.presupuesto.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Servicio extends Auditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 40)
    private String codigo;
    @Column(nullable = false, length = 150)
    private String nombre;
    @Column(length = 500)
    private String descripcion;
    @Column(nullable = false)
    private boolean activo = true;
}
