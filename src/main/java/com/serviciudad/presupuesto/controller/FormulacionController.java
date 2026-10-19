package com.serviciudad.presupuesto.controller;
import com.serviciudad.presupuesto.dto.FormulacionResponse;
import com.serviciudad.presupuesto.service.FormulacionService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
@RestController @RequestMapping("/api/formulacion/vigencias/{vigenciaId}")
public class FormulacionController {
    private final FormulacionService service;
    public FormulacionController(FormulacionService service) { this.service = service; }
    @GetMapping public FormulacionResponse get(@PathVariable Long vigenciaId, @RequestParam(required = false) Long areaId) {
        return service.get(vigenciaId, areaId);
    }
    @PostMapping("/enviar") @PreAuthorize("hasRole('RESPONSABLE_AREA')")
    public FormulacionResponse enviar(@PathVariable Long vigenciaId, @RequestParam(required = false) Long areaId) {
        return service.enviar(vigenciaId, areaId);
    }
    @PostMapping("/borrador") @PreAuthorize("hasRole('RESPONSABLE_AREA')")
    public FormulacionResponse guardarBorrador(@PathVariable Long vigenciaId, @RequestParam(required = false) Long areaId) {
        return service.guardarBorrador(vigenciaId, areaId);
    }
    @PostMapping("/aprobar") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public FormulacionResponse aprobar(@PathVariable Long vigenciaId, @RequestParam Long areaId) {
        return service.aprobar(vigenciaId, areaId);
    }
}
