package com.serviciudad.presupuesto.controller;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.service.ConsolidadoService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTA')")
@RequestMapping("/api/presupuesto/vigencias/{vigenciaId}")
public class ConsolidadoController {
    private final ConsolidadoService service;
    public ConsolidadoController(ConsolidadoService service) { this.service = service; }
    @GetMapping("/resumen") public ResumenPresupuestoResponse summary(@PathVariable Long vigenciaId) {
        return service.summary(vigenciaId);
    }
    @GetMapping("/por-area") public List<ConsolidadoAreaResponse> byArea(@PathVariable Long vigenciaId) {
        return service.byArea(vigenciaId);
    }
    @GetMapping("/por-servicio") public List<ConsolidadoServicioResponse> byServicio(@PathVariable Long vigenciaId) {
        return service.byServicio(vigenciaId);
    }
    @GetMapping("/por-rubro") public List<ConsolidadoRubroResponse> byRubro(@PathVariable Long vigenciaId) {
        return service.byRubro(vigenciaId);
    }
    @GetMapping("/general") public List<PresupuestoGeneralConceptoResponse> general(@PathVariable Long vigenciaId) {
        return service.general(vigenciaId);
    }
}
