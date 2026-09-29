package com.serviciudad.presupuesto.controller;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.service.UsuarioService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService service;
    public UsuarioController(UsuarioService service) { this.service = service; }
    @GetMapping public List<UsuarioResponse> list() { return service.list(); }
    @GetMapping("/{id}") public UsuarioResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody UsuarioCreateRequest request) {
        UsuarioResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/usuarios/" + response.id())).body(response);
    }
    @PutMapping("/{id}") public UsuarioResponse update(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest request) {
        return service.update(id, request);
    }
    @PatchMapping("/{id}/estado") public UsuarioResponse setEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return service.setEstado(id, request);
    }
}
