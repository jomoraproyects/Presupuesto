package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.entity.Servicio;
import com.serviciudad.presupuesto.exception.*;
import com.serviciudad.presupuesto.repository.ServicioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioService {
    private final ServicioRepository repository;
    public ServicioService(ServicioRepository repository) { this.repository = repository; }
    @Transactional(readOnly = true)
    public List<ServicioResponse> list() { return repository.findAll().stream().map(this::toResponse).toList(); }
    @Transactional(readOnly = true)
    public ServicioResponse get(Long id) { return toResponse(find(id)); }
    public ServicioResponse create(ServicioRequest request) {
        Servicio servicio = new Servicio(); apply(servicio, request);
        return toResponse(repository.saveAndFlush(servicio));
    }
    public ServicioResponse update(Long id, ServicioRequest request) {
        Servicio servicio = find(id); apply(servicio, request);
        return toResponse(repository.saveAndFlush(servicio));
    }
    public ServicioResponse setEstado(Long id, EstadoRequest request) {
        Servicio servicio = find(id); servicio.setActivo(request.activo());
        return toResponse(repository.saveAndFlush(servicio));
    }
    private void apply(Servicio servicio, ServicioRequest request) {
        if (repository.existsByCodigoAndIdNot(request.codigo(), servicio.getId() == null ? -1L : servicio.getId())) {
            throw new BusinessException("Ya existe un servicio con ese código");
        }
        servicio.setCodigo(request.codigo().trim()); servicio.setNombre(request.nombre().trim()); servicio.setDescripcion(request.descripcion());
    }
    private Servicio find(Long id) { return repository.findById(id).orElseThrow(() -> new NotFoundException("Servicio no encontrado")); }
    private ServicioResponse toResponse(Servicio s) {
        return new ServicioResponse(s.getId(), s.getCodigo(), s.getNombre(), s.getDescripcion(), s.isActivo(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
