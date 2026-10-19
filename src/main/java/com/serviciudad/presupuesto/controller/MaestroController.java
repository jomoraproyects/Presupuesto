package com.serviciudad.presupuesto.controller;
import com.serviciudad.presupuesto.dto.MaestroResponse;
import com.serviciudad.presupuesto.service.MaestroService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/presupuesto/vigencias/{vigenciaId}/maestro")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTA')")
public class MaestroController {
    private final MaestroService service;
    public MaestroController(MaestroService service) { this.service = service; }
    @GetMapping public MaestroResponse get(@PathVariable Long vigenciaId) { return service.get(vigenciaId); }
}
