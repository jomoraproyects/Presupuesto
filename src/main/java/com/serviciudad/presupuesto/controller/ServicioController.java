package com.serviciudad.presupuesto.controller;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.service.ServicioService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/servicios")
public class ServicioController {
    private final ServicioService service;
    public ServicioController(ServicioService service) { this.service = service; }
    @GetMapping public List<ServicioResponse> list() { return service.list(); }
    @GetMapping("/{id}") public ServicioResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ServicioResponse> create(@Valid @RequestBody ServicioRequest request) {
        ServicioResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/servicios/" + response.id())).body(response);
    }
    @PutMapping("/{id}") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ServicioResponse update(@PathVariable Long id, @Valid @RequestBody ServicioRequest request) { return service.update(id, request); }
    @PatchMapping("/{id}/estado") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ServicioResponse setEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) { return service.setEstado(id, request); }
}
