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

    @EntityGraph(attributePaths = {"distribuciones", "distribuciones.servicio", "area", "vigencia", "rubroPresupuestal", "creadoPor", "revisadoPor"})
    List<NecesidadPresupuestal> findByVigenciaIdAndAreaIdAndActivaTrueOrderByIdAsc(Long vigenciaId, Long areaId);
    @EntityGraph(attributePaths = {"distribuciones", "rubroPresupuestal"})
    List<NecesidadPresupuestal> findByVigenciaIdAndActivaTrueOrderByIdAsc(Long vigenciaId);
    boolean existsByRubroPresupuestalId(Long rubroId);

    @Query("select n.rubroPresupuestal.id as rubroId, sum(n.valorTotalSolicitado) as solicitado, " +
        "sum(n.valorTotalAprobado) as aprobado from NecesidadPresupuestal n " +
        "where n.vigencia.id = :vigenciaId and n.activa = true and n.aplicaDistribucion = false " +
        "and n.estado in :estados group by n.rubroPresupuestal.id")
    List<RubroTotal> sinDistribucion(@Param("vigenciaId") Long vigenciaId, @Param("estados") Collection<EstadoNecesidadPresupuestal> estados);

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

    @Query("select n.rubroPresupuestal.id as rubroId, n.area.id as areaId, n.area.nombre as area, " +
        "sum(n.valorTotalSolicitado) as solicitado, sum(n.valorTotalAprobado) as aprobado " +
        "from NecesidadPresupuestal n where n.vigencia.id = :vigenciaId and n.activa = true " +
        "and n.estado in :estados group by n.rubroPresupuestal.id, n.area.id, n.area.nombre")
    List<ConceptoAreaTotal> byConceptoArea(@Param("vigenciaId") Long vigenciaId,
        @Param("estados") Collection<EstadoNecesidadPresupuestal> estados);

    @Query("select n.estado as estado, count(n) as cantidad, sum(n.valorTotalSolicitado) as solicitado, " +
        "sum(n.valorTotalAprobado) as aprobado from NecesidadPresupuestal n " +
        "where n.vigencia.id = :vigenciaId and n.area.id = :areaId and n.activa = true group by n.estado")
    List<AreaEstadoTotal> byAreaAndEstado(@Param("vigenciaId") Long vigenciaId, @Param("areaId") Long areaId);

    interface Totales { BigDecimal getSolicitado(); BigDecimal getAprobado(); }
    interface AreaTotal extends Totales { Long getAreaId(); String getArea(); }
    interface RubroTotal extends Totales { Long getRubroId(); }
    interface ConceptoAreaTotal extends RubroTotal { Long getAreaId(); String getArea(); }
    interface AreaEstadoTotal extends Totales {
        EstadoNecesidadPresupuestal getEstado(); Long getCantidad();
    }
}
