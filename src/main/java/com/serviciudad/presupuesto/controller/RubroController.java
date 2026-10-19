package com.serviciudad.presupuesto.controller;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.service.RubroService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rubros")
public class RubroController {
    private final RubroService service;
    public RubroController(RubroService service) { this.service = service; }
    @GetMapping public List<RubroResponse> list() { return service.list(); }
    @GetMapping("/conceptos") public List<RubroResponse> conceptos(@RequestParam(required = false) Long vigenciaId) { return service.conceptos(vigenciaId); }
    @PostMapping("/conceptos") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<RubroResponse> crearConcepto(@Valid @RequestBody ConceptoRequest request) {
        RubroResponse response = service.crearConcepto(request);
        return ResponseEntity.created(URI.create("/api/rubros/" + response.id())).body(response);
    }
    @PutMapping("/conceptos/{id}") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public RubroResponse editarConcepto(@PathVariable Long id, @Valid @RequestBody ConceptoRequest request) { return service.editarConcepto(id, request); }
    @PatchMapping("/conceptos/{id}/estado") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public RubroResponse estadoConcepto(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) { return service.estadoConcepto(id, request); }
    @GetMapping("/arbol") public List<RubroArbolResponse> tree(@RequestParam(required = false) Long vigenciaId) { return service.tree(vigenciaId); }
    @GetMapping("/{id}") public RubroResponse get(@PathVariable Long id) { return service.get(id); }
    @PostMapping @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<RubroResponse> create(@Valid @RequestBody RubroRequest request) {
        RubroResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/rubros/" + response.id())).body(response);
    }
    @PutMapping("/{id}") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public RubroResponse update(@PathVariable Long id, @Valid @RequestBody RubroRequest request) { return service.update(id, request); }
    @PatchMapping("/{id}/estado") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public RubroResponse setEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) { return service.setEstado(id, request); }
}
