package com.serviciudad.presupuesto.controller;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.service.SubgerenciaService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/areas/con-responsable")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SubgerenciaController {
    private final SubgerenciaService service;
    public SubgerenciaController(SubgerenciaService service) { this.service = service; }
    @PostMapping
    public ResponseEntity<SubgerenciaResponse> create(@Valid @RequestBody SubgerenciaCreateRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/areas/" + result.area().id())).body(result);
    }
}
