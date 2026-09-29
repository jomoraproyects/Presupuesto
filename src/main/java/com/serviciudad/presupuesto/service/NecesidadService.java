package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.entity.*;
import com.serviciudad.presupuesto.enums.*;
import com.serviciudad.presupuesto.exception.*;
import com.serviciudad.presupuesto.repository.*;
import com.serviciudad.presupuesto.security.UsuarioActual;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NecesidadService {
    private static final BigDecimal MAX_MONEY = new BigDecimal("99999999999999999.99");
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private final NecesidadRepository necesidades;
    private final HistorialNecesidadRepository historiales;
    private final AreaRepository areas;
    private final VigenciaRepository vigencias;
    private final RubroRepository rubros;
    private final DistribucionService distribucion;
    private final NecesidadMapper mapper;
    private final HistorialService historial;
    private final UsuarioActual actual;

    public NecesidadService(NecesidadRepository necesidades, HistorialNecesidadRepository historiales,
        AreaRepository areas, VigenciaRepository vigencias, RubroRepository rubros,
        DistribucionService distribucion, NecesidadMapper mapper, HistorialService historial, UsuarioActual actual) {
        this.necesidades = necesidades; this.historiales = historiales; this.areas = areas;
        this.vigencias = vigencias; this.rubros = rubros; this.distribucion = distribucion;
        this.mapper = mapper; this.historial = historial; this.actual = actual;
    }

    @Transactional(readOnly = true)
    public Page<NecesidadResponse> list(Long vigenciaId, Long areaId, Long servicioId, Long rubroId,
        EstadoNecesidadPresupuestal estado, String texto, Pageable pageable) {
        Usuario user = actual.get();
        Long effectiveAreaId = actual.is(user, NombreRol.RESPONSABLE_AREA) ? user.getArea().getId() : areaId;
        Specification<NecesidadPresupuestal> specification = (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            filters.add(cb.isTrue(root.get("activa")));
            if (vigenciaId != null) filters.add(cb.equal(root.get("vigencia").get("id"), vigenciaId));
            if (effectiveAreaId != null) filters.add(cb.equal(root.get("area").get("id"), effectiveAreaId));
            if (rubroId != null) filters.add(cb.equal(root.get("rubroPresupuestal").get("id"), rubroId));
            if (estado != null) filters.add(cb.equal(root.get("estado"), estado));
            if (servicioId != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<DistribucionNecesidad> d = subquery.from(DistribucionNecesidad.class);
                subquery.select(d.get("necesidad").get("id"))
                    .where(cb.equal(d.get("necesidad").get("id"), root.get("id")),
                        cb.equal(d.get("servicio").get("id"), servicioId));
                filters.add(cb.exists(subquery));
            }
            if (texto != null && !texto.isBlank()) {
                String pattern = "%" + texto.toLowerCase(Locale.ROOT).trim()
                    .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                filters.add(cb.or(cb.like(cb.lower(root.get("descripcion")), pattern, '\\'),
                    cb.like(cb.lower(root.get("justificacion")), pattern, '\\')));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        };
        Pageable bounded = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100),
            Sort.by(Sort.Direction.DESC, "id"));
        return necesidades.findAll(specification, bounded).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public NecesidadResponse get(Long id) { return mapper.toResponse(visible(id, actual.get())); }

    @Transactional(readOnly = true)
    public List<HistorialNecesidadResponse> history(Long id) {
        visible(id, actual.get());
        return historiales.findByNecesidadIdOrderByFechaAscIdAsc(id).stream().map(mapper::toResponse).toList();
    }

    public NecesidadResponse create(NecesidadRequest request) {
        Usuario actor = actual.get();
        requireAreaWriter(actor);
        NecesidadPresupuestal n = new NecesidadPresupuestal();
        n.setCreadoPor(actor);
        applyRequest(n, request, actor);
        necesidades.saveAndFlush(n);
        historial.register(n, actor, AccionHistorial.CREACION, "Necesidad creada en borrador", null);
        return mapper.toResponse(n);
    }

    public NecesidadResponse update(Long id, NecesidadRequest request) {
        Usuario actor = actual.get();
        requireAreaWriter(actor);
        NecesidadPresupuestal n = lockedVisible(id, actor);
        if (n.getEstado() != EstadoNecesidadPresupuestal.BORRADOR && n.getEstado() != EstadoNecesidadPresupuestal.DEVUELTA) {
            throw new BusinessException("Solo se pueden editar necesidades en borrador o devueltas");
        }
        NecesidadResponse before = mapper.toResponse(n);
        applyRequest(n, request, actor);
        necesidades.saveAndFlush(n);
        historial.register(n, actor, AccionHistorial.MODIFICACION, "Necesidad modificada por el área", before);
        return mapper.toResponse(n);
    }

    public NecesidadResponse send(Long id) {
        Usuario actor = actual.get();
        requireAreaWriter(actor);
        NecesidadPresupuestal n = lockedVisible(id, actor);
        requireState(n, EstadoNecesidadPresupuestal.BORRADOR, EstadoNecesidadPresupuestal.DEVUELTA);
        requireFormulationOpen(n.getVigencia());
        if (!n.getArea().isActiva()) throw new BusinessException("El área está inactiva");
        validateRubro(n.getRubroPresupuestal(), n.getVigencia());
        if (n.getValorTotalSolicitado().signum() <= 0) throw new BusinessException("El total solicitado debe ser mayor que cero");
        if (n.getDistribuciones().isEmpty() || n.getDistribuciones().stream()
            .map(DistribucionNecesidad::getPorcentaje).reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(HUNDRED) != 0) {
            throw new BusinessException("La necesidad debe distribuirse al 100.00 % entre servicios");
        }
        if (n.getDistribuciones().stream().anyMatch(d -> !d.getServicio().isActivo())) {
            throw new BusinessException("La distribución contiene un servicio inactivo");
        }
        NecesidadResponse before = mapper.toResponse(n);
        if (n.getValorTotalSolicitadoOriginal() == null) {
            n.setCantidadSolicitadaOriginal(n.getCantidadSolicitada());
            n.setValorUnitarioSolicitadoOriginal(n.getValorUnitarioSolicitado());
            n.setValorTotalSolicitadoOriginal(n.getValorTotalSolicitado());
        }
        n.setEstado(EstadoNecesidadPresupuestal.ENVIADA);
        necesidades.saveAndFlush(n);
        historial.register(n, actor, AccionHistorial.ENVIO, "Necesidad enviada a Presupuesto", before);
        return mapper.toResponse(n);
    }

    public NecesidadResponse startReview(Long id) {
        Usuario actor = actual.get(); requireReviewer(actor);
        NecesidadPresupuestal n = lockedVisible(id, actor);
        requireReviewOpen(n.getVigencia());
        requireState(n, EstadoNecesidadPresupuestal.ENVIADA);
        NecesidadResponse before = mapper.toResponse(n);
        n.setEstado(EstadoNecesidadPresupuestal.EN_REVISION);
        n.setRevisadoPor(actor);
        necesidades.saveAndFlush(n);
        historial.register(n, actor, AccionHistorial.INICIO_REVISION, "Presupuesto inició la revisión", before);
        return mapper.toResponse(n);
    }

    public NecesidadResponse adjust(Long id, AjusteNecesidadRequest request) {
        Usuario actor = actual.get(); requireReviewer(actor);
        NecesidadPresupuestal n = lockedVisible(id, actor);
        requireReviewOpen(n.getVigencia());
        requireState(n, EstadoNecesidadPresupuestal.EN_REVISION);
        if (request.cantidadAprobada() == null && request.valorUnitarioAprobado() == null
            && (request.observacionPresupuesto() == null || request.observacionPresupuesto().isBlank())) {
            throw new BusinessException("El ajuste debe incluir una cantidad, valor unitario u observación");
        }
        NecesidadResponse before = mapper.toResponse(n);
        if (request.cantidadAprobada() != null || request.valorUnitarioAprobado() != null) {
            BigDecimal quantity = request.cantidadAprobada() != null ? request.cantidadAprobada()
                : n.getCantidadAprobada() != null ? n.getCantidadAprobada() : n.getCantidadSolicitada();
            BigDecimal unit = request.valorUnitarioAprobado() != null ? request.valorUnitarioAprobado()
                : n.getValorUnitarioAprobado() != null ? n.getValorUnitarioAprobado() : n.getValorUnitarioSolicitado();
            n.setCantidadAprobada(quantity);
            n.setValorUnitarioAprobado(unit);
            n.setValorTotalAprobado(money(quantity.multiply(unit)));
            distribucion.recalculate(n);
        }
        if (request.observacionPresupuesto() != null) n.setObservacionPresupuesto(request.observacionPresupuesto().trim());
        n.setRevisadoPor(actor);
        necesidades.saveAndFlush(n);
        historial.register(n, actor, AccionHistorial.AJUSTE, "Presupuesto ajustó la necesidad", before);
        return mapper.toResponse(n);
    }

    public NecesidadResponse approve(Long id) {
        Usuario actor = actual.get(); requireReviewer(actor);
        NecesidadPresupuestal n = lockedVisible(id, actor);
        requireReviewOpen(n.getVigencia());
        requireState(n, EstadoNecesidadPresupuestal.EN_REVISION);
        NecesidadResponse before = mapper.toResponse(n);
        if (n.getValorTotalAprobado() == null) {
            n.setCantidadAprobada(n.getCantidadSolicitada());
            n.setValorUnitarioAprobado(n.getValorUnitarioSolicitado());
            n.setValorTotalAprobado(n.getValorTotalSolicitado());
        }
        if (n.getValorTotalAprobado().signum() == 0) n.setEstado(EstadoNecesidadPresupuestal.RECHAZADA);
        else if (n.getCantidadAprobada().compareTo(n.getCantidadSolicitada()) == 0
            && n.getValorUnitarioAprobado().compareTo(n.getValorUnitarioSolicitado()) == 0) {
            n.setEstado(EstadoNecesidadPresupuestal.APROBADA);
        } else n.setEstado(EstadoNecesidadPresupuestal.APROBADA_CON_AJUSTES);
        n.setRevisadoPor(actor);
        distribucion.recalculate(n);
        necesidades.saveAndFlush(n);
        AccionHistorial action = n.getEstado() == EstadoNecesidadPresupuestal.RECHAZADA
            ? AccionHistorial.RECHAZO : AccionHistorial.APROBACION;
        historial.register(n, actor, action, "Presupuesto finalizó la revisión", before);
        return mapper.toResponse(n);
    }

    public NecesidadResponse reject(Long id, ObservacionRequest request) {
        Usuario actor = actual.get(); requireReviewer(actor);
        NecesidadPresupuestal n = lockedVisible(id, actor);
        requireReviewOpen(n.getVigencia());
        requireState(n, EstadoNecesidadPresupuestal.ENVIADA, EstadoNecesidadPresupuestal.EN_REVISION);
        NecesidadResponse before = mapper.toResponse(n);
        n.setCantidadAprobada(BigDecimal.ZERO.setScale(4));
        n.setValorUnitarioAprobado(BigDecimal.ZERO.setScale(2));
        n.setValorTotalAprobado(BigDecimal.ZERO.setScale(2));
        n.setEstado(EstadoNecesidadPresupuestal.RECHAZADA);
        n.setObservacionPresupuesto(request.observacionPresupuesto().trim());
        n.setRevisadoPor(actor);
        distribucion.recalculate(n);
        necesidades.saveAndFlush(n);
        historial.register(n, actor, AccionHistorial.RECHAZO, "Presupuesto rechazó la necesidad", before);
        return mapper.toResponse(n);
    }

    public NecesidadResponse returnToArea(Long id, ObservacionRequest request) {
        Usuario actor = actual.get(); requireReviewer(actor);
        NecesidadPresupuestal n = lockedVisible(id, actor);
        requireReviewOpen(n.getVigencia());
        requireState(n, EstadoNecesidadPresupuestal.ENVIADA, EstadoNecesidadPresupuestal.EN_REVISION);
        NecesidadResponse before = mapper.toResponse(n);
        n.setCantidadAprobada(null);
        n.setValorUnitarioAprobado(null);
        n.setValorTotalAprobado(null);
        n.setEstado(EstadoNecesidadPresupuestal.DEVUELTA);
        n.setObservacionPresupuesto(request.observacionPresupuesto().trim());
        n.setRevisadoPor(actor);
        distribucion.recalculate(n);
        necesidades.saveAndFlush(n);
        historial.register(n, actor, AccionHistorial.DEVOLUCION, "Presupuesto devolvió la necesidad al área", before);
        return mapper.toResponse(n);
    }

    public void cancelDraft(Long id) {
        Usuario actor = actual.get(); requireAreaWriter(actor);
        NecesidadPresupuestal n = lockedVisible(id, actor);
        requireState(n, EstadoNecesidadPresupuestal.BORRADOR);
        NecesidadResponse before = mapper.toResponse(n);
        n.setActiva(false);
        necesidades.saveAndFlush(n);
        historial.register(n, actor, AccionHistorial.CANCELACION, "Borrador cancelado", before);
    }

    private void applyRequest(NecesidadPresupuestal n, NecesidadRequest request, Usuario actor) {
        Area area = resolveArea(request.areaId(), actor, n.getArea());
        Vigencia vigencia = vigencias.findById(request.vigenciaId())
            .orElseThrow(() -> new NotFoundException("Vigencia no encontrada"));
        requireFormulationOpen(vigencia);
        RubroPresupuestal rubro = rubros.findById(request.rubroPresupuestalId())
            .orElseThrow(() -> new NotFoundException("Rubro no encontrado"));
        validateRubro(rubro, vigencia);
        n.setArea(area); n.setVigencia(vigencia); n.setRubroPresupuestal(rubro);
        n.setDescripcion(request.descripcion().trim()); n.setJustificacion(request.justificacion().trim());
        n.setUnidadMedida(request.unidadMedida().trim());
        n.setCantidadSolicitada(request.cantidadSolicitada());
        n.setValorUnitarioSolicitado(request.valorUnitarioSolicitado());
        n.setValorTotalSolicitado(money(request.cantidadSolicitada().multiply(request.valorUnitarioSolicitado())));
        distribucion.set(n, request.distribuciones());
    }

    private Area resolveArea(Long requestedId, Usuario actor, Area current) {
        Long id;
        if (actual.is(actor, NombreRol.RESPONSABLE_AREA)) {
            id = actor.getArea().getId();
            if (requestedId != null && !requestedId.equals(id)) throw new AccessDeniedException("No puede crear necesidades para otra área");
        } else id = requestedId == null && current != null ? current.getId() : requestedId;
        if (id == null) throw new BusinessException("Debe indicar el área");
        Area area = areas.findById(id).orElseThrow(() -> new NotFoundException("Área no encontrada"));
        if (!area.isActiva()) throw new BusinessException("El área está inactiva");
        return area;
    }

    private void validateRubro(RubroPresupuestal rubro, Vigencia vigencia) {
        if (!rubro.isActivo() || !rubro.isPermiteMovimiento()) {
            throw new BusinessException("El rubro debe estar activo y permitir movimiento");
        }
        if (rubro.getVigencia() != null && !rubro.getVigencia().getId().equals(vigencia.getId())) {
            throw new BusinessException("El rubro pertenece a otra vigencia");
        }
    }
    private void requireFormulationOpen(Vigencia vigencia) {
        if (!vigencia.isActiva() || vigencia.getEstado() != EstadoVigencia.ABIERTA) {
            throw new BusinessException("La vigencia debe estar activa y abierta para formulación");
        }
    }
    private void requireReviewOpen(Vigencia vigencia) {
        if (!vigencia.isActiva() || (vigencia.getEstado() != EstadoVigencia.ABIERTA
            && vigencia.getEstado() != EstadoVigencia.EN_REVISION)) {
            throw new BusinessException("La vigencia no permite revisión presupuestal");
        }
    }
    private void requireState(NecesidadPresupuestal n, EstadoNecesidadPresupuestal... allowed) {
        if (Arrays.stream(allowed).noneMatch(s -> s == n.getEstado())) {
            throw new BusinessException("La operación no está permitida en el estado " + n.getEstado());
        }
    }
    private void requireAreaWriter(Usuario user) {
        if (!actual.isAdmin(user) && !actual.is(user, NombreRol.RESPONSABLE_AREA)) {
            throw new AccessDeniedException("Solo el área responsable puede realizar esta operación");
        }
    }
    private void requireReviewer(Usuario user) {
        if (!actual.isPresupuesto(user)) throw new AccessDeniedException("Solo Presupuesto puede revisar necesidades");
    }
    private BigDecimal money(BigDecimal amount) {
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        if (rounded.compareTo(MAX_MONEY) > 0) throw new BusinessException("El valor excede el máximo permitido");
        return rounded;
    }
    private NecesidadPresupuestal visible(Long id, Usuario user) {
        NecesidadPresupuestal n = necesidades.findById(id)
            .filter(NecesidadPresupuestal::isActiva).orElseThrow(() -> new NotFoundException("Necesidad no encontrada"));
        checkVisibility(n, user);
        return n;
    }
    private NecesidadPresupuestal lockedVisible(Long id, Usuario user) {
        NecesidadPresupuestal n = necesidades.findForUpdate(id)
            .filter(NecesidadPresupuestal::isActiva).orElseThrow(() -> new NotFoundException("Necesidad no encontrada"));
        checkVisibility(n, user);
        return n;
    }
    private void checkVisibility(NecesidadPresupuestal n, Usuario user) {
        if (actual.is(user, NombreRol.RESPONSABLE_AREA) && !n.getArea().getId().equals(user.getArea().getId())) {
            throw new AccessDeniedException("La necesidad pertenece a otra área");
        }
    }
}
