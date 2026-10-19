package com.serviciudad.presupuesto.controller;

import com.serviciudad.presupuesto.dto.ResumenAreaResponse;
import com.serviciudad.presupuesto.service.ConsolidadoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/presupuesto/vigencias/{vigenciaId}/mi-area")
public class ResumenAreaController {
    private final ConsolidadoService service;
    public ResumenAreaController(ConsolidadoService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasRole('RESPONSABLE_AREA')")
    public ResumenAreaResponse get(@PathVariable Long vigenciaId) { return service.myArea(vigenciaId); }
}
