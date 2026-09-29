package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.entity.Area;
import com.serviciudad.presupuesto.exception.*;
import com.serviciudad.presupuesto.repository.AreaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AreaService {
    private final AreaRepository repository;
    public AreaService(AreaRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<AreaResponse> list() { return repository.findAll().stream().map(this::toResponse).toList(); }
    @Transactional(readOnly = true)
    public AreaResponse get(Long id) { return toResponse(find(id)); }
    public AreaResponse create(AreaRequest request) {
        Area area = new Area();
        apply(area, request);
        return toResponse(repository.saveAndFlush(area));
    }
    public AreaResponse update(Long id, AreaRequest request) {
        Area area = find(id);
        apply(area, request);
        return toResponse(repository.saveAndFlush(area));
    }
    public AreaResponse setEstado(Long id, EstadoRequest request) {
        Area area = find(id);
        if (!request.activo() && repository.findAll().stream().anyMatch(a -> a.isActiva() && a.getAreaPadre() != null && a.getAreaPadre().getId().equals(id))) {
            throw new BusinessException("Desactive primero las áreas hijas activas");
        }
        area.setActiva(request.activo());
        return toResponse(repository.saveAndFlush(area));
    }
    private void apply(Area area, AreaRequest request) {
        if (repository.existsByCodigoAndIdNot(request.codigo(), area.getId() == null ? -1L : area.getId())) {
            throw new BusinessException("Ya existe un área con ese código");
        }
        Area parent = request.areaPadreId() == null ? null : find(request.areaPadreId());
        if (parent != null && !parent.isActiva()) throw new BusinessException("El área padre está inactiva");
        for (Area current = parent; current != null; current = current.getAreaPadre()) {
            if (current.getId().equals(area.getId())) throw new BusinessException("La relación de áreas crearía un ciclo");
        }
        area.setCodigo(request.codigo().trim());
        area.setNombre(request.nombre().trim());
        area.setDescripcion(request.descripcion());
        area.setAreaPadre(parent);
    }
    private Area find(Long id) { return repository.findById(id).orElseThrow(() -> new NotFoundException("Área no encontrada")); }
    private AreaResponse toResponse(Area a) {
        return new AreaResponse(a.getId(), a.getCodigo(), a.getNombre(), a.getDescripcion(), a.isActiva(),
            a.getAreaPadre() == null ? null : a.getAreaPadre().getId(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
