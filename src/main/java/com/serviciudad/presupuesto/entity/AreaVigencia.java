package com.serviciudad.presupuesto.entity;

import jakarta.persistence.*;
import com.serviciudad.presupuesto.enums.EstadoFormulario;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "area_vigencia", uniqueConstraints = @UniqueConstraint(columnNames = {"vigencia_id", "area_id"}))
@Getter @Setter
public class AreaVigencia extends Auditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "vigencia_id", nullable = false)
    private Vigencia vigencia;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "area_id", nullable = false)
    private Area area;
    @Column(nullable = false) private boolean activa = true;
    @Column(nullable = false) private boolean enviada;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private EstadoFormulario estado = EstadoFormulario.PENDIENTE;
    @Column(name = "fecha_envio") private java.time.LocalDateTime fechaEnvio;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "enviado_por_id") private Usuario enviadoPor;
    @Column(name = "datos_enviados", columnDefinition = "LONGTEXT") private String datosEnviados;
}
