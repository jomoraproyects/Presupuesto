package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.entity.*;
import com.serviciudad.presupuesto.enums.*;
import com.serviciudad.presupuesto.exception.*;
import com.serviciudad.presupuesto.repository.*;
import com.serviciudad.presupuesto.security.UsuarioActual;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly = true)
public class FormulacionService {
    private final VigenciaRepository vigencias;
    private final AreaRepository areas;
    private final AreaVigenciaRepository participantes;
    private final RubroRepository rubros;
    private final NecesidadRepository necesidades;
    private final NecesidadMapper mapper;
    private final UsuarioActual actual;
    private final NecesidadService necesidadService;
    private final com.fasterxml.jackson.databind.ObjectMapper json;
    public FormulacionService(VigenciaRepository vigencias, AreaRepository areas, AreaVigenciaRepository participantes,
        RubroRepository rubros, NecesidadRepository necesidades, NecesidadMapper mapper, UsuarioActual actual,
        NecesidadService necesidadService, com.fasterxml.jackson.databind.ObjectMapper json) {
        this.vigencias = vigencias; this.areas = areas; this.participantes = participantes; this.rubros = rubros;
        this.necesidades = necesidades; this.mapper = mapper; this.actual = actual;
        this.necesidadService = necesidadService;
        this.json = json;
    }

    @Transactional
    public FormulacionResponse enviar(Long vigenciaId, Long requestedArea) {
        vigencias.findForUpdate(vigenciaId).orElseThrow(() -> new NotFoundException("Vigencia no encontrada"));
        FormulacionResponse form = get(vigenciaId, requestedArea);
        if (form.enviada()) throw new BusinessException("El formulario ya fue enviado a Presupuesto");
        if (!form.puedeCrear()) throw new BusinessException("El formulario no está habilitado para envío");
        List<Long> pending = form.rubros().stream().flatMap(r -> r.necesidades().stream())
            .filter(n -> n.estado() == EstadoNecesidadPresupuestal.BORRADOR || n.estado() == EstadoNecesidadPresupuestal.DEVUELTA)
            .map(NecesidadResponse::id).sorted().toList();
        for (Long id : pending) necesidadService.send(id);
        AreaVigencia participation = participantes.findByVigenciaIdAndAreaId(vigenciaId, form.areaId())
            .orElseThrow(() -> new BusinessException("El área no participa en esta vigencia"));
        participation.setEnviada(true);
        boolean preReviewed = necesidades.findByVigenciaIdAndAreaIdAndActivaTrueOrderByIdAsc(vigenciaId, form.areaId())
            .stream().anyMatch(n -> n.getEstado() == EstadoNecesidadPresupuestal.EN_REVISION);
        participation.setEstado(preReviewed ? EstadoFormulario.EN_REVISION : EstadoFormulario.ENVIADO);
        participation.setFechaEnvio(java.time.LocalDateTime.now());
        participation.setEnviadoPor(actual.get());
        participantes.saveAndFlush(participation);
        FormulacionResponse sent = get(vigenciaId, requestedArea);
        try { participation.setDatosEnviados(json.writeValueAsString(sent)); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("No se pudo conservar el formulario enviado", e); }
        participantes.saveAndFlush(participation);
        return sent;
    }

    @Transactional
    public FormulacionResponse guardarBorrador(Long vigenciaId, Long requestedArea) {
        FormulacionResponse form = get(vigenciaId, requestedArea);
        if (!form.puedeCrear()) throw new BusinessException("El formulario no está habilitado para guardar borrador");
        AreaVigencia p = participantes.findByVigenciaIdAndAreaId(vigenciaId, form.areaId())
            .orElseThrow(() -> new BusinessException("La dependencia no participa en esta vigencia"));
        p.setEstado(EstadoFormulario.BORRADOR);
        participantes.saveAndFlush(p);
        return get(vigenciaId, requestedArea);
    }

    @Transactional
    public FormulacionResponse aprobar(Long vigenciaId, Long areaId) {
        if (!actual.puedeRevisar(actual.get())) throw new AccessDeniedException("Solo Presupuesto puede aprobar");
        vigencias.findForUpdate(vigenciaId).orElseThrow(() -> new NotFoundException("Vigencia no encontrada"));
        AreaVigencia p = participantes.findByVigenciaIdAndAreaId(vigenciaId, areaId)
            .orElseThrow(() -> new BusinessException("La dependencia no participa en esta vigencia"));
        if (!p.isEnviada() || p.getEstado() == EstadoFormulario.APROBADO)
            throw new BusinessException("El formulario debe estar enviado y pendiente de aprobación");
        for (NecesidadPresupuestal n : necesidades.findByVigenciaIdAndAreaIdAndActivaTrueOrderByIdAsc(vigenciaId, areaId)) {
            if (n.getEstado() == EstadoNecesidadPresupuestal.ENVIADA) necesidadService.startReview(n.getId());
            if (n.getEstado() == EstadoNecesidadPresupuestal.EN_REVISION) necesidadService.approve(n.getId());
        }
        p.setEstado(EstadoFormulario.APROBADO);
        participantes.saveAndFlush(p);
        return get(vigenciaId, areaId);
    }

    public FormulacionResponse get(Long vigenciaId, Long requestedArea) {
        Usuario user = actual.get();
        Long areaId = requestedArea;
        if (actual.is(user, NombreRol.RESPONSABLE_AREA)) {
            areaId = user.getArea().getId();
            if (requestedArea != null && !requestedArea.equals(areaId)) throw new AccessDeniedException("No puede consultar la formulación de otra área");
        }
        if (areaId == null) throw new BusinessException("Seleccione un área");
        Area area = areas.findById(areaId).orElseThrow(() -> new NotFoundException("Área no encontrada"));
        Vigencia v = vigencias.findById(vigenciaId).orElseThrow(() -> new NotFoundException("Vigencia no encontrada"));
        AreaVigencia participation = participantes.findByVigenciaIdAndAreaId(vigenciaId, areaId).orElse(null);
        boolean sent = participation != null && participation.isEnviada();
        boolean available = participation != null && participation.isActiva();
        boolean canCreate = !sent && area.isActiva() && v.isActiva() && v.getEstado() == EstadoVigencia.ABIERTA
            && available;
        List<RubroPresupuestal> catalog = rubros.findAllByOrderByOrdenAscIdAsc();
        Set<String> scopedCodes = RubrosAnuales.codigosPropios(catalog, vigenciaId);
        Set<Long> parents = new HashSet<>();
        catalog.stream().filter(RubroPresupuestal::isActivo).filter(r -> r.getRubroPadre() != null)
            .forEach(r -> parents.add(r.getRubroPadre().getId()));
        Map<Long, RubroPresupuestal> categories = new LinkedHashMap<>();
        RubrosAnuales.efectivos(catalog, vigenciaId).stream().filter(r -> r.isActivo() && r.isPermiteMovimiento() && r.isCategoriaFormulacion() && !parents.contains(r.getId()))
            .filter(r -> !RubrosAnuales.plantillaAnterior(r))
            .filter(r -> r.getVigencia() == null || r.getVigencia().getId().equals(vigenciaId)).forEach(r -> categories.put(r.getId(), r));
        Map<Long, List<NecesidadResponse>> items = new HashMap<>();
        for (NecesidadPresupuestal n : necesidades.findByVigenciaIdAndAreaIdAndActivaTrueOrderByIdAsc(vigenciaId, areaId)) {
            categories.putIfAbsent(n.getRubroPresupuestal().getId(), n.getRubroPresupuestal());
            items.computeIfAbsent(n.getRubroPresupuestal().getId(), ignored -> new ArrayList<>()).add(mapper.toResponse(n));
        }
        List<FormulacionRubroResponse> result = new ArrayList<>();
        BigDecimal totalRequested = BigDecimal.ZERO.setScale(2), totalApproved = BigDecimal.ZERO.setScale(2);
        for (var category : categories.values().stream().sorted(Comparator.comparingInt(RubroPresupuestal::getOrden)
            .thenComparing(RubroPresupuestal::getId)).toList()) {
            var children = items.getOrDefault(category.getId(), List.of());
            BigDecimal requested = children.stream().map(NecesidadResponse::valorTotalSolicitado).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
            BigDecimal approved = children.stream().map(n -> n.valorTotalAprobado() == null ? BigDecimal.ZERO : n.valorTotalAprobado())
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
            boolean enabled = !RubrosAnuales.plantillaAnterior(category) && category.isActivo() && category.isPermiteMovimiento() && category.isCategoriaFormulacion() && !parents.contains(category.getId())
                && (category.getVigencia() != null || !scopedCodes.contains(category.getCodigo()));
            result.add(new FormulacionRubroResponse(category.getId(), category.getCodigo(), category.getNombre(), enabled,
                requested, approved, approved.subtract(requested), children.size(), children, category.getOrden()));
            totalRequested = totalRequested.add(requested); totalApproved = totalApproved.add(approved);
        }
        return new FormulacionResponse(vigenciaId, areaId, area.getNombre(), canCreate, sent, totalRequested, totalApproved,
            totalApproved.subtract(totalRequested), result, participation == null ? null : participation.getFechaEnvio(),
            participation == null || participation.getEnviadoPor() == null ? null : participation.getEnviadoPor().getId(),
            participation == null || participation.getEnviadoPor() == null ? null : participation.getEnviadoPor().getNombre(),
            available, participation == null ? EstadoFormulario.PENDIENTE : participation.getEstado());
    }
}
