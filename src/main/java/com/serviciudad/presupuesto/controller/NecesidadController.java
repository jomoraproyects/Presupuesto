package com.serviciudad.presupuesto.controller;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.enums.EstadoNecesidadPresupuestal;
import com.serviciudad.presupuesto.service.NecesidadService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/necesidades")
public class NecesidadController {
    private final NecesidadService service;
    public NecesidadController(NecesidadService service) { this.service = service; }

    @GetMapping
    public PaginaResponse<NecesidadResponse> list(@RequestParam(required = false) Long vigenciaId,
        @RequestParam(required = false) Long areaId, @RequestParam(required = false) Long servicioId,
        @RequestParam(required = false) Long rubroId, @RequestParam(required = false) EstadoNecesidadPresupuestal estado,
        @RequestParam(required = false) String texto, @PageableDefault(size = 20) Pageable pageable) {
        return PaginaResponse.from(service.list(vigenciaId, areaId, servicioId, rubroId, estado, texto, pageable));
    }

    @GetMapping("/{id}")
    public NecesidadResponse get(@PathVariable Long id) { return service.get(id); }

    @GetMapping("/{id}/historial")
    public List<HistorialNecesidadResponse> history(@PathVariable Long id) { return service.history(id); }

    @PostMapping @PreAuthorize("hasAnyRole('RESPONSABLE_AREA','SUPER_ADMIN')")
    public ResponseEntity<NecesidadResponse> create(@Valid @RequestBody NecesidadRequest request) {
        NecesidadResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/necesidades/" + response.id())).body(response);
    }

    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('RESPONSABLE_AREA','SUPER_ADMIN')")
    public NecesidadResponse update(@PathVariable Long id, @Valid @RequestBody NecesidadRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('RESPONSABLE_AREA','SUPER_ADMIN')")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        service.cancelDraft(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/enviar") @PreAuthorize("hasAnyRole('RESPONSABLE_AREA','SUPER_ADMIN')")
    public NecesidadResponse send(@PathVariable Long id) { return service.send(id); }

    @PostMapping("/{id}/iniciar-revision") @PreAuthorize("hasAnyRole('PRESUPUESTO','SUPER_ADMIN')")
    public NecesidadResponse startReview(@PathVariable Long id) { return service.startReview(id); }

    @PatchMapping("/{id}/ajuste") @PreAuthorize("hasAnyRole('PRESUPUESTO','SUPER_ADMIN')")
    public NecesidadResponse adjust(@PathVariable Long id, @Valid @RequestBody AjusteNecesidadRequest request) {
        return service.adjust(id, request);
    }

    @PostMapping("/{id}/aprobar") @PreAuthorize("hasAnyRole('PRESUPUESTO','SUPER_ADMIN')")
    public NecesidadResponse approve(@PathVariable Long id) { return service.approve(id); }

    @PostMapping("/{id}/rechazar") @PreAuthorize("hasAnyRole('PRESUPUESTO','SUPER_ADMIN')")
    public NecesidadResponse reject(@PathVariable Long id, @Valid @RequestBody ObservacionRequest request) {
        return service.reject(id, request);
    }

    @PostMapping("/{id}/devolver") @PreAuthorize("hasAnyRole('PRESUPUESTO','SUPER_ADMIN')")
    public NecesidadResponse returnToArea(@PathVariable Long id, @Valid @RequestBody ObservacionRequest request) {
        return service.returnToArea(id, request);
    }
}
