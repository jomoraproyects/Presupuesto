package com.serviciudad.presupuesto.entity;

import com.serviciudad.presupuesto.enums.NombreRol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Rol {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 40)
    private NombreRol nombre;
}
