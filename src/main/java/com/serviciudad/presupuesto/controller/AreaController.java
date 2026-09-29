package com.serviciudad.presupuesto.controller;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.service.AreaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/areas")
public class AreaController {
    private final AreaService service;
    public AreaController(AreaService service) { this.service = service; }
    @GetMapping public List<AreaResponse> list() { return service.list(); }
    @GetMapping("/{id}") public AreaResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AreaResponse> create(@Valid @RequestBody AreaRequest request) {
        AreaResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/areas/" + response.id())).body(response);
    }
    @PutMapping("/{id}") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AreaResponse update(@PathVariable Long id, @Valid @RequestBody AreaRequest request) { return service.update(id, request); }
    @PatchMapping("/{id}/estado") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public AreaResponse setEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) { return service.setEstado(id, request); }
}
