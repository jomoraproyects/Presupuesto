package com.serviciudad.presupuesto.controller;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.service.VigenciaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vigencias")
public class VigenciaController {
    private final VigenciaService service;
    public VigenciaController(VigenciaService service) { this.service = service; }
    @GetMapping public List<VigenciaResponse> list() { return service.list(); }
    @GetMapping("/{id}") public VigenciaResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<VigenciaResponse> create(@Valid @RequestBody VigenciaRequest request) {
        VigenciaResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/vigencias/" + response.id())).body(response);
    }
    @PutMapping("/{id}") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public VigenciaResponse update(@PathVariable Long id, @Valid @RequestBody VigenciaRequest request) { return service.update(id, request); }
    @PatchMapping("/{id}/estado") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public VigenciaResponse setEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) { return service.setEstado(id, request); }
}
