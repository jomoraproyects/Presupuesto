package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.entity.*;
import com.serviciudad.presupuesto.enums.EstadoVigencia;
import com.serviciudad.presupuesto.exception.*;
import com.serviciudad.presupuesto.repository.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class ConfiguracionVigenciaService {
    private final VigenciaRepository vigencias;
    private final AreaRepository areas;
    private final RubroRepository rubros;
    private final ServicioRepository servicios;
    private final AreaVigenciaRepository participantes;
    private final MapeoRubroServicioRepository mapeos;
    private final VigenciaService vigenciaService;

    public ConfiguracionVigenciaService(VigenciaRepository vigencias, AreaRepository areas, RubroRepository rubros,
        ServicioRepository servicios, AreaVigenciaRepository participantes, MapeoRubroServicioRepository mapeos,
        VigenciaService vigenciaService) {
        this.vigencias = vigencias; this.areas = areas; this.rubros = rubros; this.servicios = servicios;
        this.participantes = participantes; this.mapeos = mapeos; this.vigenciaService = vigenciaService;
    }

    @Transactional(readOnly = true)
    public List<ParticipanteResponse> participantes(Long vigenciaId) {
        findVigencia(vigenciaId);
        Set<Long> enabled = new HashSet<>();
        participantes.findByVigenciaIdOrderByIdAsc(vigenciaId).stream().filter(AreaVigencia::isActiva)
            .forEach(p -> enabled.add(p.getArea().getId()));
        return areas.findAll().stream().sorted(Comparator.comparing(Area::getNombre))
            .map(a -> new ParticipanteResponse(a.getId(), a.getCodigo(), a.getNombre(), a.isActiva(), enabled.contains(a.getId()))).toList();
    }

    public ParticipanteResponse setParticipante(Long vigenciaId, Long areaId, EstadoRequest request) {
        Vigencia v = lock(vigenciaId);
        requireConfigurable(v, false);
        Area area = areas.findById(areaId).orElseThrow(() -> new NotFoundException("Área no encontrada"));
        if (request.activo() && !area.isActiva()) throw new BusinessException("Reactive primero el área");
        AreaVigencia p = participantes.findByVigenciaIdAndAreaId(vigenciaId, areaId).orElseGet(AreaVigencia::new);
        p.setVigencia(v); p.setArea(area); p.setActiva(request.activo());
        participantes.saveAndFlush(p);
        return new ParticipanteResponse(areaId, area.getCodigo(), area.getNombre(), area.isActiva(), p.isActiva());
    }

    @Transactional(readOnly = true)
    public List<MapeoResponse> mapeos(Long vigenciaId) {
        findVigencia(vigenciaId);
        return mapeos.findByVigenciaIdOrderByIdAsc(vigenciaId).stream().map(this::response).toList();
    }

    public MapeoResponse saveMapeo(Long vigenciaId, Long id, MapeoRequest request) {
        Vigencia v = lock(vigenciaId); requireConfigurable(v, true);
        MapeoRubroServicio m = id == null ? new MapeoRubroServicio() : findMapeo(vigenciaId, id);
        var duplicate = mapeos.findByVigenciaIdAndRubroOrigenIdAndServicioId(vigenciaId, request.rubroOrigenId(), request.servicioId());
        if (duplicate.isPresent() && !Objects.equals(duplicate.get().getId(), id)) {
            throw new BusinessException("Ya existe un mapeo para esta categoría y servicio; edítelo o reactívelo");
        }
        RubroPresupuestal origin = rubro(request.rubroOrigenId(), v);
        RubroPresupuestal destination = rubro(request.rubroDestinoId(), v);
        if (!origin.isCategoriaFormulacion() || !origin.isPermiteMovimiento()
            || rubros.existsByRubroPadreIdAndActivoTrue(origin.getId())) {
            throw new BusinessException("El origen debe ser una categoría de formulación de movimiento sin hijos activos");
        }
        if (destination.isCategoriaFormulacion() || rubros.existsByRubroPadreIdAndActivoTrue(destination.getId())) {
            throw new BusinessException("El destino debe ser un rubro final sin hijos activos, excluido del formulario");
        }
        Servicio service = servicios.findById(request.servicioId()).orElseThrow(() -> new NotFoundException("Servicio no encontrado"));
        if (!service.isActivo()) throw new BusinessException("El servicio está inactivo");
        m.setVigencia(v); m.setRubroOrigen(origin); m.setRubroDestino(destination); m.setServicio(service);
        return response(mapeos.saveAndFlush(m));
    }

    public MapeoResponse setMapeoEstado(Long vigenciaId, Long id, EstadoRequest request) {
        requireConfigurable(lock(vigenciaId), true);
        MapeoRubroServicio m = findMapeo(vigenciaId, id);
        if (request.activo()) {
            var origin = rubro(m.getRubroOrigen().getId(), m.getVigencia());
            var destination = rubro(m.getRubroDestino().getId(), m.getVigencia());
            if (!origin.isCategoriaFormulacion() || !origin.isPermiteMovimiento() || destination.isCategoriaFormulacion()
                || rubros.existsByRubroPadreIdAndActivoTrue(origin.getId()) || rubros.existsByRubroPadreIdAndActivoTrue(destination.getId())) {
                throw new BusinessException("El mapeo requiere una categoría de origen y un destino final sin hijos activos");
            }
            if (!m.getServicio().isActivo()) throw new BusinessException("El servicio está inactivo");
        }
        m.setActivo(request.activo()); return response(mapeos.saveAndFlush(m));
    }

    public VigenciaResponse copiar(Long origenId, CopiarVigenciaRequest request) {
        lock(origenId);
        if (request.vigencia().estado() != EstadoVigencia.BORRADOR && request.vigencia().estado() != EstadoVigencia.ABIERTA) {
            throw new BusinessException("La nueva vigencia debe empezar en BORRADOR o ABIERTA");
        }
        List<RubroPresupuestal> source = RubrosAnuales.efectivos(rubros.findAllByOrderByOrdenAscIdAsc(), origenId).stream()
            .sorted(Comparator.comparingInt(RubroPresupuestal::getNivel).thenComparing(RubroPresupuestal::getId)).toList();
        Set<String> codes = new HashSet<>();
        for (var r : source) if (!codes.add(r.getCodigo())) {
            throw new BusinessException("Hay códigos repetidos entre rubros globales y de la vigencia; normalícelos antes de copiar");
        }
        VigenciaResponse created = vigenciaService.create(request.vigencia());
        Vigencia target = findVigencia(created.id());
        Map<String, RubroPresupuestal> copiesByCode = new HashMap<>();
        for (RubroPresupuestal r : source) {
            RubroPresupuestal copy = new RubroPresupuestal();
            copy.setCodigo(r.getCodigo()); copy.setNombre(r.getNombre()); copy.setDescripcion(r.getDescripcion());
            copy.setVigencia(target); copy.setActivo(r.isActivo()); copy.setPermiteMovimiento(r.isPermiteMovimiento());
            copy.setCategoriaFormulacion(r.isCategoriaFormulacion()); copy.setOrden(r.getOrden()); copy.setNivel(r.getNivel());
            if (r.getRubroPadre() != null) {
                RubroPresupuestal parent = copiesByCode.get(r.getRubroPadre().getCodigo());
                if (parent == null) throw new BusinessException("La estructura de rubros de origen no tiene un padre válido");
                copy.setRubroPadre(parent);
            }
            rubros.save(copy);
            copiesByCode.put(r.getCodigo(), copy);
        }
        for (MapeoRubroServicio m : mapeos.findByVigenciaIdOrderByIdAsc(origenId)) {
            MapeoRubroServicio copy = new MapeoRubroServicio();
            copy.setVigencia(target); copy.setRubroOrigen(copiesByCode.get(m.getRubroOrigen().getCodigo()));
            copy.setRubroDestino(copiesByCode.get(m.getRubroDestino().getCodigo())); copy.setServicio(m.getServicio()); copy.setActivo(m.isActivo());
            if (copy.getRubroOrigen() == null || copy.getRubroDestino() == null) throw new BusinessException("El mapeo usa rubros ajenos a la vigencia de origen");
            mapeos.save(copy);
        }
        if (request.copiarParticipantes()) for (AreaVigencia p : participantes.findByVigenciaIdOrderByIdAsc(origenId)) {
            AreaVigencia copy = new AreaVigencia(); copy.setVigencia(target); copy.setArea(p.getArea());
            copy.setActiva(p.isActiva() && p.getArea().isActiva()); participantes.save(copy);
        }
        return created;
    }

    private RubroPresupuestal rubro(Long id, Vigencia v) {
        RubroPresupuestal r = rubros.findById(id).orElseThrow(() -> new NotFoundException("Rubro no encontrado"));
        if (!r.isActivo() || (r.getVigencia() != null && !r.getVigencia().getId().equals(v.getId()))) {
            throw new BusinessException("El rubro debe estar activo y ser global o de esta vigencia");
        }
        return r;
    }
    private void requireConfigurable(Vigencia v, boolean allowReview) {
        if (!v.isActiva() || (v.getEstado() != EstadoVigencia.BORRADOR && v.getEstado() != EstadoVigencia.ABIERTA
            && !(allowReview && v.getEstado() == EstadoVigencia.EN_REVISION))) {
            throw new BusinessException("La vigencia no permite modificar esta configuración");
        }
    }
    private Vigencia lock(Long id) { return vigencias.findForUpdate(id).orElseThrow(() -> new NotFoundException("Vigencia no encontrada")); }
    private Vigencia findVigencia(Long id) { return vigencias.findById(id).orElseThrow(() -> new NotFoundException("Vigencia no encontrada")); }
    private MapeoRubroServicio findMapeo(Long vigenciaId, Long id) {
        return mapeos.findById(id).filter(m -> m.getVigencia().getId().equals(vigenciaId)).orElseThrow(() -> new NotFoundException("Mapeo no encontrado"));
    }
    private MapeoResponse response(MapeoRubroServicio m) {
        return new MapeoResponse(m.getId(), m.getVigencia().getId(), m.getRubroOrigen().getId(), m.getRubroOrigen().getNombre(),
            m.getServicio().getId(), m.getServicio().getNombre(), m.getRubroDestino().getId(), m.getRubroDestino().getCodigo(),
            m.getRubroDestino().getNombre(), m.isActivo());
    }
}
