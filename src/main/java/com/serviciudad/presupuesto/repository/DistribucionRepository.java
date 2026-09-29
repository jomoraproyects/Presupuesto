package com.serviciudad.presupuesto.repository;

import com.serviciudad.presupuesto.entity.DistribucionNecesidad;
import com.serviciudad.presupuesto.enums.EstadoNecesidadPresupuestal;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DistribucionRepository extends JpaRepository<DistribucionNecesidad, Long> {
    @Query("select d.servicio.id as servicioId, d.servicio.nombre as servicio, " +
        "sum(d.valorSolicitadoDistribuido) as solicitado, sum(d.valorAprobadoDistribuido) as aprobado " +
        "from DistribucionNecesidad d where d.necesidad.vigencia.id = :vigenciaId " +
        "and d.necesidad.activa = true and d.necesidad.estado in :estados group by d.servicio.id, d.servicio.nombre")
    List<ServicioTotal> byServicio(@Param("vigenciaId") Long vigenciaId, @Param("estados") Collection<EstadoNecesidadPresupuestal> estados);

    interface ServicioTotal {
        Long getServicioId(); String getServicio(); BigDecimal getSolicitado(); BigDecimal getAprobado();
    }
}
