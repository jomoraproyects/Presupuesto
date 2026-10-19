package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.entity.RubroPresupuestal;
import com.serviciudad.presupuesto.entity.Vigencia;
import com.serviciudad.presupuesto.enums.EstadoNecesidadPresupuestal;
import com.serviciudad.presupuesto.entity.Usuario;
import com.serviciudad.presupuesto.entity.AreaVigencia;
import com.serviciudad.presupuesto.enums.EstadoFormulario;
import com.serviciudad.presupuesto.enums.NombreRol;
import com.serviciudad.presupuesto.exception.NotFoundException;
import com.serviciudad.presupuesto.repository.*;
import com.serviciudad.presupuesto.security.UsuarioActual;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConsolidadoService {
    static final Set<EstadoNecesidadPresupuestal> INCLUDED = EnumSet.of(
        EstadoNecesidadPresupuestal.BORRADOR, EstadoNecesidadPresupuestal.DEVUELTA,
        EstadoNecesidadPresupuestal.ENVIADA, EstadoNecesidadPresupuestal.EN_REVISION,
        EstadoNecesidadPresupuestal.APROBADA, EstadoNecesidadPresupuestal.APROBADA_CON_AJUSTES,
        EstadoNecesidadPresupuestal.RECHAZADA);
    private final NecesidadRepository necesidades;
    private final DistribucionRepository distribuciones;
    private final VigenciaRepository vigencias;
    private final RubroRepository rubros;
    private final UsuarioActual actual;
    private final AreaRepository areas;
    private final AreaVigenciaRepository participantes;
    public ConsolidadoService(NecesidadRepository necesidades, DistribucionRepository distribuciones,
        VigenciaRepository vigencias, RubroRepository rubros, UsuarioActual actual,
        AreaRepository areas, AreaVigenciaRepository participantes) {
        this.necesidades = necesidades; this.distribuciones = distribuciones;
        this.vigencias = vigencias; this.rubros = rubros; this.actual = actual;
        this.areas = areas; this.participantes = participantes;
    }

    public ResumenAreaResponse myArea(Long vigenciaId) {
        findVigencia(vigenciaId);
        Usuario user = actual.get();
        if (!actual.is(user, NombreRol.RESPONSABLE_AREA)) {
            throw new org.springframework.security.access.AccessDeniedException("Se requiere un responsable de área");
        }
        Long areaId = user.getArea().getId();
        Map<EstadoNecesidadPresupuestal, Long> counts = new EnumMap<>(EstadoNecesidadPresupuestal.class);
        for (EstadoNecesidadPresupuestal state : EstadoNecesidadPresupuestal.values()) counts.put(state, 0L);
        BigDecimal requested = BigDecimal.ZERO.setScale(2);
        BigDecimal approved = BigDecimal.ZERO.setScale(2);
        long number = 0;
        for (NecesidadRepository.AreaEstadoTotal row : necesidades.byAreaAndEstado(vigenciaId, areaId)) {
            counts.put(row.getEstado(), row.getCantidad());
            number += row.getCantidad();
            if (INCLUDED.contains(row.getEstado())) {
                requested = requested.add(zero(row.getSolicitado()));
                approved = approved.add(zero(row.getAprobado()));
            }
        }
        return new ResumenAreaResponse(vigenciaId, areaId, user.getArea().getNombre(),
            requested, approved, approved.subtract(requested), number, counts);
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
        Map<Long, ConsolidadoAreaResponse> result = new LinkedHashMap<>();
        Map<Long, AreaVigencia> participants = new HashMap<>();
        participantes.findByVigenciaIdOrderByIdAsc(vigenciaId).stream().filter(AreaVigencia::isActiva)
            .forEach(p -> participants.put(p.getArea().getId(), p));
        participants.values().stream().sorted(Comparator.comparing(p -> p.getArea().getNombre()))
            .forEach(p -> result.put(p.getArea().getId(), new ConsolidadoAreaResponse(p.getArea().getId(),
                p.getArea().getNombre(), zero(null), zero(null), zero(null), p.isEnviada(), p.getEstado())));
        necesidades.byArea(vigenciaId, INCLUDED).forEach(row -> result.put(row.getAreaId(),
            new ConsolidadoAreaResponse(row.getAreaId(), row.getArea(), zero(row.getSolicitado()),
                zero(row.getAprobado()), zero(row.getAprobado()).subtract(zero(row.getSolicitado())),
                participants.containsKey(row.getAreaId()) && participants.get(row.getAreaId()).isEnviada(),
                participants.containsKey(row.getAreaId()) ? participants.get(row.getAreaId()).getEstado() : EstadoFormulario.BORRADOR)));
        return List.copyOf(result.values());
    }

    public List<PresupuestoGeneralConceptoResponse> general(Long vigenciaId) {
        findVigencia(vigenciaId);
        Map<Long, List<PresupuestoGeneralConceptoResponse.DependenciaConcepto>> grouped = new HashMap<>();
        necesidades.byConceptoArea(vigenciaId, INCLUDED).forEach(row -> grouped.computeIfAbsent(row.getRubroId(),
            ignored -> new ArrayList<>()).add(new PresupuestoGeneralConceptoResponse.DependenciaConcepto(
                row.getAreaId(), row.getArea(), zero(row.getSolicitado()), zero(row.getAprobado()),
                zero(row.getAprobado()).subtract(zero(row.getSolicitado())))));
        Map<Long, RubroPresupuestal> concepts = new LinkedHashMap<>();
        RubrosAnuales.efectivos(rubros.findAllByOrderByOrdenAscIdAsc(), vigenciaId).stream()
            .filter(RubrosAnuales::concepto).filter(RubroPresupuestal::isActivo)
            .forEach(r -> concepts.put(r.getId(), r));
        rubros.findAllById(grouped.keySet()).stream().filter(RubroPresupuestal::isCategoriaFormulacion)
            .forEach(r -> concepts.putIfAbsent(r.getId(), r));
        return concepts.values().stream().sorted(Comparator.comparingInt(RubroPresupuestal::getOrden)
            .thenComparing(RubroPresupuestal::getId)).map(r -> {
                List<PresupuestoGeneralConceptoResponse.DependenciaConcepto> rows = grouped.getOrDefault(r.getId(), List.of());
                BigDecimal requested = rows.stream().map(PresupuestoGeneralConceptoResponse.DependenciaConcepto::solicitado)
                    .reduce(zero(null), BigDecimal::add);
                BigDecimal approved = rows.stream().map(PresupuestoGeneralConceptoResponse.DependenciaConcepto::aprobado)
                    .reduce(zero(null), BigDecimal::add);
                return new PresupuestoGeneralConceptoResponse(r.getId(), r.getNombre(), r.getOrden(),
                    requested, approved, approved.subtract(requested), rows);
            }).toList();
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
        }).sorted(Comparator.comparingInt((ConsolidadoRubroResponse r) -> catalog.get(r.rubroId()).getOrden())
            .thenComparing(ConsolidadoRubroResponse::rubroId)).toList();
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
