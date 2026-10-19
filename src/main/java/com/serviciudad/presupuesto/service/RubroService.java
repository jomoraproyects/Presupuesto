package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.entity.RubroPresupuestal;
import com.serviciudad.presupuesto.entity.Vigencia;
import com.serviciudad.presupuesto.exception.*;
import com.serviciudad.presupuesto.repository.RubroRepository;
import com.serviciudad.presupuesto.repository.VigenciaRepository;
import com.serviciudad.presupuesto.repository.NecesidadRepository;
import com.serviciudad.presupuesto.repository.MapeoRubroServicioRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RubroService {
    private final RubroRepository repository;
    private final VigenciaRepository vigencias;
    private final NecesidadRepository necesidades;
    private final MapeoRubroServicioRepository mapeos;
    public RubroService(RubroRepository repository, VigenciaRepository vigencias, NecesidadRepository necesidades, MapeoRubroServicioRepository mapeos) {
        this.repository = repository; this.vigencias = vigencias;
        this.necesidades = necesidades; this.mapeos = mapeos;
    }
    @Transactional(readOnly = true)
    public List<RubroResponse> list() { return repository.findAllByOrderByOrdenAscIdAsc().stream().map(this::toResponse).toList(); }
    @Transactional(readOnly = true)
    public RubroResponse get(Long id) { return toResponse(find(id)); }
    @Transactional(readOnly = true)
    public List<RubroResponse> conceptos(Long vigenciaId) {
        return RubrosAnuales.efectivos(repository.findAllByOrderByOrdenAscIdAsc(), vigenciaId).stream()
            .filter(RubrosAnuales::concepto).map(this::toResponse).toList();
    }
    public RubroResponse crearConcepto(ConceptoRequest request) {
        RubroResponse created = create(new RubroRequest("FORM.CONCEPTO." + UUID.randomUUID(), nombreConcepto(request.nombre()),
            request.descripcion() == null ? null : request.descripcion().trim(), null, true, request.vigenciaId(), request.orden(), true));
        return Boolean.FALSE.equals(request.activo()) ? setEstado(created.id(), new EstadoRequest(false)) : created;
    }
    public RubroResponse editarConcepto(Long id, ConceptoRequest request) {
        RubroPresupuestal r = concepto(id);
        // La identidad y la vigencia se conservan: editar no reemplaza ítems ni históricos.
        r.setNombre(nombreConcepto(request.nombre())); r.setOrden(request.orden());
        if (request.descripcion() != null) r.setDescripcion(request.descripcion().trim());
        if (request.activo() != null) return setEstado(id, new EstadoRequest(request.activo()));
        return toResponse(repository.saveAndFlush(r));
    }
    public RubroResponse estadoConcepto(Long id, EstadoRequest request) {
        concepto(id);
        return setEstado(id, request);
    }
    private RubroPresupuestal concepto(Long id) {
        RubroPresupuestal r = find(id);
        if (!RubrosAnuales.concepto(r)) throw new BusinessException("El registro no es un concepto del formulario");
        return r;
    }
    private String nombreConcepto(String nombre) {
        if (nombre.trim().equalsIgnoreCase("TOTAL")) throw new BusinessException("TOTAL es calculado y no puede ser un concepto editable");
        return nombre.trim();
    }
    public RubroResponse create(RubroRequest request) {
        RubroPresupuestal rubro = new RubroPresupuestal(); apply(rubro, request);
        return toResponse(repository.saveAndFlush(rubro));
    }
    public RubroResponse update(Long id, RubroRequest request) {
        RubroPresupuestal rubro = find(id);
        Long oldParentId = rubro.getRubroPadre() == null ? null : rubro.getRubroPadre().getId();
        apply(rubro, request);
        repository.saveAndFlush(rubro);
        Long newParentId = rubro.getRubroPadre() == null ? null : rubro.getRubroPadre().getId();
        if (!Objects.equals(oldParentId, newParentId)) updateDescendantLevels(rubro);
        return toResponse(rubro);
    }
    public RubroResponse setEstado(Long id, EstadoRequest request) {
        RubroPresupuestal rubro = find(id);
        if (!request.activo() && repository.existsByRubroPadreIdAndActivoTrue(id)) {
            throw new BusinessException("Desactive primero los rubros hijos activos");
        }
        if (request.activo() && rubro.getRubroPadre() != null && !rubro.getRubroPadre().isActivo()) {
            throw new BusinessException("El rubro padre está inactivo");
        }
        rubro.setActivo(request.activo());
        return toResponse(repository.saveAndFlush(rubro));
    }
    @Transactional(readOnly = true)
    public List<RubroArbolResponse> tree(Long vigenciaId) {
        List<RubroPresupuestal> rubros = repository.findAllByOrderByOrdenAscIdAsc().stream()
            .filter(RubroPresupuestal::isActivo)
            .filter(r -> Objects.equals(scope(r.getVigencia()), vigenciaId)).toList();
        Map<Long, Node> nodes = new LinkedHashMap<>();
        for (RubroPresupuestal rubro : rubros) nodes.put(rubro.getId(), new Node(rubro));
        List<Node> roots = new ArrayList<>();
        for (Node node : nodes.values()) {
            Long parentId = node.rubro.getRubroPadre() == null ? null : node.rubro.getRubroPadre().getId();
            if (parentId == null) roots.add(node);
            else if (nodes.containsKey(parentId)) nodes.get(parentId).children.add(node);
        }
        return roots.stream().map(this::toTree).toList();
    }
    private void apply(RubroPresupuestal rubro, RubroRequest request) {
        Vigencia vigencia = request.vigenciaId() == null ? null : vigencias.findById(request.vigenciaId())
            .orElseThrow(() -> new NotFoundException("Vigencia no encontrada"));
        if (vigencia != null && !vigencia.isActiva()) throw new BusinessException("La vigencia está inactiva");
        RubroPresupuestal parent = request.rubroPadreId() == null ? null : find(request.rubroPadreId());
        if (parent != null && !parent.isActivo()) throw new BusinessException("El rubro padre está inactivo");
        if (parent != null && (necesidades.existsByRubroPresupuestalId(parent.getId())
            || mapeos.existsByRubroDestinoId(parent.getId()) || mapeos.existsByRubroOrigenId(parent.getId()))) {
            throw new BusinessException("No puede convertir en padre un rubro que ya recibe ítems o participa en mapeos");
        }
        if (parent != null && !Objects.equals(scope(parent.getVigencia()), scope(vigencia))) {
            throw new BusinessException("El rubro padre debe pertenecer a la misma vigencia");
        }
        for (RubroPresupuestal current = parent; current != null; current = current.getRubroPadre()) {
            if (Objects.equals(current.getId(), rubro.getId())) throw new BusinessException("La relación de rubros crearía un ciclo");
        }
        if (rubro.getId() != null && !Objects.equals(scope(rubro.getVigencia()), scope(vigencia))
            && (!repository.findByRubroPadreId(rubro.getId()).isEmpty() || necesidades.existsByRubroPresupuestalId(rubro.getId())
                || mapeos.existsByRubroOrigenId(rubro.getId()) || mapeos.existsByRubroDestinoId(rubro.getId()))) {
            throw new BusinessException("No se puede cambiar la vigencia de un rubro con hijos, ítems o mapeos");
        }
        boolean duplicate = repository.findAllByCodigo(request.codigo().trim()).stream()
            .anyMatch(r -> !Objects.equals(r.getId(), rubro.getId()) && Objects.equals(scope(r.getVigencia()), scope(vigencia)));
        if (duplicate) throw new BusinessException("Ya existe un rubro con ese código en la vigencia");
        boolean formulation = request.categoriaFormulacion() == null ? rubro.isCategoriaFormulacion() : request.categoriaFormulacion();
        if (rubro.getId() != null && mapeos.existsByRubroOrigenId(rubro.getId()) && (!formulation || !request.permiteMovimiento())) {
            throw new BusinessException("Un origen de mapeos debe conservar su condición de categoría de movimiento");
        }
        if (rubro.getId() != null && mapeos.existsByRubroDestinoId(rubro.getId()) && formulation) {
            throw new BusinessException("Un destino de mapeos no puede habilitarse como categoría del formulario");
        }
        rubro.setCodigo(request.codigo().trim()); rubro.setNombre(request.nombre().trim());
        rubro.setDescripcion(request.descripcion()); rubro.setRubroPadre(parent);
        rubro.setNivel(parent == null ? 0 : parent.getNivel() + 1);
        rubro.setPermiteMovimiento(request.permiteMovimiento()); rubro.setVigencia(vigencia); rubro.setOrden(request.orden());
        if (request.categoriaFormulacion() != null) rubro.setCategoriaFormulacion(request.categoriaFormulacion());
    }
    private void updateDescendantLevels(RubroPresupuestal parent) {
        for (RubroPresupuestal child : repository.findByRubroPadreId(parent.getId())) {
            child.setNivel(parent.getNivel() + 1);
            updateDescendantLevels(child);
        }
    }
    private Long scope(Vigencia vigencia) { return vigencia == null ? null : vigencia.getId(); }
    private RubroPresupuestal find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Rubro no encontrado"));
    }
    private RubroResponse toResponse(RubroPresupuestal r) {
        return new RubroResponse(r.getId(), r.getCodigo(), r.getNombre(), r.getDescripcion(),
            r.getRubroPadre() == null ? null : r.getRubroPadre().getId(), r.getNivel(), r.isPermiteMovimiento(),
            r.isActivo(), scope(r.getVigencia()), r.getOrden(), r.getCreatedAt(), r.getUpdatedAt(), r.isCategoriaFormulacion());
    }
    private RubroArbolResponse toTree(Node node) {
        RubroPresupuestal r = node.rubro;
        return new RubroArbolResponse(r.getId(), r.getCodigo(), r.getNombre(), r.getDescripcion(), r.getNivel(),
            r.isPermiteMovimiento(), scope(r.getVigencia()), r.getOrden(), node.children.stream().map(this::toTree).toList(), r.isCategoriaFormulacion());
    }
    private static class Node {
        final RubroPresupuestal rubro;
        final List<Node> children = new ArrayList<>();
        Node(RubroPresupuestal rubro) { this.rubro = rubro; }
    }
}
