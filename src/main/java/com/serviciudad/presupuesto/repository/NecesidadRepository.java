package com.serviciudad.presupuesto.repository;

import com.serviciudad.presupuesto.entity.NecesidadPresupuestal;
import com.serviciudad.presupuesto.enums.EstadoNecesidadPresupuestal;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface NecesidadRepository extends JpaRepository<NecesidadPresupuestal, Long>, JpaSpecificationExecutor<NecesidadPresupuestal> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NecesidadPresupuestal n where n.id = :id")
    Optional<NecesidadPresupuestal> findForUpdate(@Param("id") Long id);

    @Query("select sum(n.valorTotalSolicitado) as solicitado, sum(n.valorTotalAprobado) as aprobado " +
        "from NecesidadPresupuestal n where n.vigencia.id = :vigenciaId and n.activa = true and n.estado in :estados")
    Totales totals(@Param("vigenciaId") Long vigenciaId, @Param("estados") Collection<EstadoNecesidadPresupuestal> estados);

    @Query("select n.area.id as areaId, n.area.nombre as area, sum(n.valorTotalSolicitado) as solicitado, " +
        "sum(n.valorTotalAprobado) as aprobado from NecesidadPresupuestal n " +
        "where n.vigencia.id = :vigenciaId and n.activa = true and n.estado in :estados group by n.area.id, n.area.nombre")
    List<AreaTotal> byArea(@Param("vigenciaId") Long vigenciaId, @Param("estados") Collection<EstadoNecesidadPresupuestal> estados);

    @Query("select n.rubroPresupuestal.id as rubroId, sum(n.valorTotalSolicitado) as solicitado, " +
        "sum(n.valorTotalAprobado) as aprobado from NecesidadPresupuestal n " +
        "where n.vigencia.id = :vigenciaId and n.activa = true and n.estado in :estados group by n.rubroPresupuestal.id")
    List<RubroTotal> byRubro(@Param("vigenciaId") Long vigenciaId, @Param("estados") Collection<EstadoNecesidadPresupuestal> estados);

    interface Totales { BigDecimal getSolicitado(); BigDecimal getAprobado(); }
    interface AreaTotal extends Totales { Long getAreaId(); String getArea(); }
    interface RubroTotal extends Totales { Long getRubroId(); }
}
