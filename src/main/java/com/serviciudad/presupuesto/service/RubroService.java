package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.entity.RubroPresupuestal;
import com.serviciudad.presupuesto.entity.Vigencia;
import com.serviciudad.presupuesto.exception.*;
import com.serviciudad.presupuesto.repository.RubroRepository;
import com.serviciudad.presupuesto.repository.VigenciaRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RubroService {
    private final RubroRepository repository;
    private final VigenciaRepository vigencias;
    public RubroService(RubroRepository repository, VigenciaRepository vigencias) {
        this.repository = repository; this.vigencias = vigencias;
    }
    @Transactional(readOnly = true)
    public List<RubroResponse> list() { return repository.findAllByOrderByOrdenAscIdAsc().stream().map(this::toResponse).toList(); }
    @Transactional(readOnly = true)
    public RubroResponse get(Long id) { return toResponse(find(id)); }
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
        if (parent != null && !Objects.equals(scope(parent.getVigencia()), scope(vigencia))) {
            throw new BusinessException("El rubro padre debe pertenecer a la misma vigencia");
        }
        for (RubroPresupuestal current = parent; current != null; current = current.getRubroPadre()) {
            if (Objects.equals(current.getId(), rubro.getId())) throw new BusinessException("La relación de rubros crearía un ciclo");
        }
        if (rubro.getId() != null && !Objects.equals(scope(rubro.getVigencia()), scope(vigencia))
            && !repository.findByRubroPadreId(rubro.getId()).isEmpty()) {
            throw new BusinessException("No se puede cambiar la vigencia de un rubro con hijos");
        }
        boolean duplicate = repository.findAllByCodigo(request.codigo().trim()).stream()
            .anyMatch(r -> !Objects.equals(r.getId(), rubro.getId()) && Objects.equals(scope(r.getVigencia()), scope(vigencia)));
        if (duplicate) throw new BusinessException("Ya existe un rubro con ese código en la vigencia");
        rubro.setCodigo(request.codigo().trim()); rubro.setNombre(request.nombre().trim());
        rubro.setDescripcion(request.descripcion()); rubro.setRubroPadre(parent);
        rubro.setNivel(parent == null ? 0 : parent.getNivel() + 1);
        rubro.setPermiteMovimiento(request.permiteMovimiento()); rubro.setVigencia(vigencia); rubro.setOrden(request.orden());
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
            r.isActivo(), scope(r.getVigencia()), r.getOrden(), r.getCreatedAt(), r.getUpdatedAt());
    }
    private RubroArbolResponse toTree(Node node) {
        RubroPresupuestal r = node.rubro;
        return new RubroArbolResponse(r.getId(), r.getCodigo(), r.getNombre(), r.getDescripcion(), r.getNivel(),
            r.isPermiteMovimiento(), scope(r.getVigencia()), r.getOrden(), node.children.stream().map(this::toTree).toList());
    }
    private static class Node {
        final RubroPresupuestal rubro;
        final List<Node> children = new ArrayList<>();
        Node(RubroPresupuestal rubro) { this.rubro = rubro; }
    }
}
