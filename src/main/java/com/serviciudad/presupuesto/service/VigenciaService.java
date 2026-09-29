package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.entity.Vigencia;
import com.serviciudad.presupuesto.exception.*;
import com.serviciudad.presupuesto.repository.VigenciaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VigenciaService {
    private final VigenciaRepository repository;
    public VigenciaService(VigenciaRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true)
    public List<VigenciaResponse> list() { return repository.findAll().stream().map(this::toResponse).toList(); }
    @Transactional(readOnly = true)
    public VigenciaResponse get(Long id) { return toResponse(find(id)); }
    public VigenciaResponse create(VigenciaRequest request) {
        Vigencia vigencia = new Vigencia(); apply(vigencia, request);
        return toResponse(repository.saveAndFlush(vigencia));
    }
    public VigenciaResponse update(Long id, VigenciaRequest request) {
        Vigencia vigencia = find(id); apply(vigencia, request);
        return toResponse(repository.saveAndFlush(vigencia));
    }
    public VigenciaResponse setEstado(Long id, EstadoRequest request) {
        Vigencia vigencia = find(id); vigencia.setActiva(request.activo());
        return toResponse(repository.saveAndFlush(vigencia));
    }
    private void apply(Vigencia vigencia, VigenciaRequest request) {
        if (repository.existsByAnioAndIdNot(request.anio(), vigencia.getId() == null ? -1L : vigencia.getId())) {
            throw new BusinessException("Ya existe una vigencia para ese año");
        }
        if (request.fechaFin().isBefore(request.fechaInicio())) throw new BusinessException("La fecha fin debe ser posterior a la fecha inicio");
        vigencia.setAnio(request.anio()); vigencia.setNombre(request.nombre().trim());
        vigencia.setFechaInicio(request.fechaInicio()); vigencia.setFechaFin(request.fechaFin());
        vigencia.setPresupuestoDisponible(request.presupuestoDisponible()); vigencia.setEstado(request.estado());
    }
    private Vigencia find(Long id) { return repository.findById(id).orElseThrow(() -> new NotFoundException("Vigencia no encontrada")); }
    private VigenciaResponse toResponse(Vigencia v) {
        return new VigenciaResponse(v.getId(), v.getAnio(), v.getNombre(), v.getFechaInicio(), v.getFechaFin(),
            v.getPresupuestoDisponible(), v.getEstado(), v.isActiva(), v.getCreatedAt(), v.getUpdatedAt());
    }
}
