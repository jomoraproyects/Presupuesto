package com.serviciudad.presupuesto.controller;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.service.ConfiguracionVigenciaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/vigencias/{vigenciaId}")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','CONSULTA')")
public class ConfiguracionVigenciaController {
    private final ConfiguracionVigenciaService service;
    public ConfiguracionVigenciaController(ConfiguracionVigenciaService service) { this.service = service; }
    @GetMapping("/participantes")
    public List<ParticipanteResponse> participantes(@PathVariable Long vigenciaId) { return service.participantes(vigenciaId); }
    @PutMapping("/participantes/{areaId}") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ParticipanteResponse participante(@PathVariable Long vigenciaId, @PathVariable Long areaId, @Valid @RequestBody EstadoRequest request) {
        return service.setParticipante(vigenciaId, areaId, request);
    }
    @GetMapping("/mapeos")
    public List<MapeoResponse> mapeos(@PathVariable Long vigenciaId) { return service.mapeos(vigenciaId); }
    @PostMapping("/mapeos") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<MapeoResponse> crearMapeo(@PathVariable Long vigenciaId, @Valid @RequestBody MapeoRequest request) {
        MapeoResponse m = service.saveMapeo(vigenciaId, null, request);
        return ResponseEntity.created(URI.create("/api/vigencias/" + vigenciaId + "/mapeos/" + m.id())).body(m);
    }
    @PutMapping("/mapeos/{id}") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public MapeoResponse editarMapeo(@PathVariable Long vigenciaId, @PathVariable Long id, @Valid @RequestBody MapeoRequest request) {
        return service.saveMapeo(vigenciaId, id, request);
    }
    @PatchMapping("/mapeos/{id}/estado") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public MapeoResponse estadoMapeo(@PathVariable Long vigenciaId, @PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return service.setMapeoEstado(vigenciaId, id, request);
    }
    @PostMapping("/copiar-configuracion") @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<VigenciaResponse> copiar(@PathVariable Long vigenciaId, @Valid @RequestBody CopiarVigenciaRequest request) {
        VigenciaResponse v = service.copiar(vigenciaId, request);
        return ResponseEntity.created(URI.create("/api/vigencias/" + v.id())).body(v);
    }
}
