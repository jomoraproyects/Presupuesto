package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.entity.RubroPresupuestal;
import com.serviciudad.presupuesto.entity.Vigencia;
import com.serviciudad.presupuesto.enums.EstadoNecesidadPresupuestal;
import com.serviciudad.presupuesto.exception.NotFoundException;
import com.serviciudad.presupuesto.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConsolidadoService {
    private static final Set<EstadoNecesidadPresupuestal> INCLUDED = EnumSet.of(
        EstadoNecesidadPresupuestal.ENVIADA, EstadoNecesidadPresupuestal.EN_REVISION,
        EstadoNecesidadPresupuestal.APROBADA, EstadoNecesidadPresupuestal.APROBADA_CON_AJUSTES,
        EstadoNecesidadPresupuestal.RECHAZADA);
    private final NecesidadRepository necesidades;
    private final DistribucionRepository distribuciones;
    private final VigenciaRepository vigencias;
    private final RubroRepository rubros;
    public ConsolidadoService(NecesidadRepository necesidades, DistribucionRepository distribuciones,
        VigenciaRepository vigencias, RubroRepository rubros) {
        this.necesidades = necesidades; this.distribuciones = distribuciones;
        this.vigencias = vigencias; this.rubros = rubros;
    }

    public ResumenPresupuestoResponse summary(Long vigenciaId) {
        Vigencia vigencia = findVigencia(vigenciaId);
        NecesidadRepository.Totales totals = necesidades.totals(vigenciaId, INCLUDED);
        BigDecimal requested = zero(totals == null ? null : totals.getSolicitado());
        BigDecimal approved = zero(totals == null ? null : totals.getAprobado());
        return new ResumenPresupuestoResponse(vigenciaId, vigencia.getPresupuestoDisponible(),
            requested, approved, approved.subtract(vigencia.getPresupuestoDisponible()));
    }

    public List<ConsolidadoAreaResponse> byArea(Long vigenciaId) {
        findVigencia(vigenciaId);
        return necesidades.byArea(vigenciaId, INCLUDED).stream()
            .map(row -> new ConsolidadoAreaResponse(row.getAreaId(), row.getArea(), zero(row.getSolicitado()),
                zero(row.getAprobado()), zero(row.getAprobado()).subtract(zero(row.getSolicitado()))))
            .toList();
    }

    public List<ConsolidadoServicioResponse> byServicio(Long vigenciaId) {
        findVigencia(vigenciaId);
        return distribuciones.byServicio(vigenciaId, INCLUDED).stream()
            .map(row -> new ConsolidadoServicioResponse(row.getServicioId(), row.getServicio(),
                zero(row.getSolicitado()), zero(row.getAprobado()),
                zero(row.getAprobado()).subtract(zero(row.getSolicitado()))))
            .toList();
    }

    public List<ConsolidadoRubroResponse> byRubro(Long vigenciaId) {
        findVigencia(vigenciaId);
        Map<Long, RubroPresupuestal> catalog = new HashMap<>();
        rubros.findAll().forEach(r -> catalog.put(r.getId(), r));
        Map<Long, Amounts> rolled = new HashMap<>();
        for (NecesidadRepository.RubroTotal direct : necesidades.byRubro(vigenciaId, INCLUDED)) {
            Long id = direct.getRubroId();
            while (id != null) {
                Amounts amounts = rolled.computeIfAbsent(id, ignored -> new Amounts());
                amounts.solicitado = amounts.solicitado.add(zero(direct.getSolicitado()));
                amounts.aprobado = amounts.aprobado.add(zero(direct.getAprobado()));
                RubroPresupuestal rubro = catalog.get(id);
                if (rubro == null) throw new IllegalStateException("Rubro de la necesidad no encontrado");
                id = rubro.getRubroPadre() == null ? null : rubro.getRubroPadre().getId();
            }
        }
        return rolled.entrySet().stream().map(entry -> {
            RubroPresupuestal r = catalog.get(entry.getKey());
            Amounts a = entry.getValue();
            return new ConsolidadoRubroResponse(r.getId(), r.getRubroPadre() == null ? null : r.getRubroPadre().getId(),
                r.getCodigo(), r.getNombre(), r.getNivel(), a.solicitado, a.aprobado,
                a.aprobado.subtract(a.solicitado));
        }).sorted(Comparator.comparingInt(ConsolidadoRubroResponse::nivel)
            .thenComparing(ConsolidadoRubroResponse::codigo)).toList();
    }

    private Vigencia findVigencia(Long id) {
        return vigencias.findById(id).orElseThrow(() -> new NotFoundException("Vigencia no encontrada"));
    }
    private BigDecimal zero(BigDecimal amount) { return amount == null ? BigDecimal.ZERO.setScale(2) : amount; }
    private static class Amounts {
        BigDecimal solicitado = BigDecimal.ZERO.setScale(2);
        BigDecimal aprobado = BigDecimal.ZERO.setScale(2);
    }
}
