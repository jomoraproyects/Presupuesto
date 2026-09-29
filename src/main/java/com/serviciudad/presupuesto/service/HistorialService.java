package com.serviciudad.presupuesto.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviciudad.presupuesto.dto.NecesidadResponse;
import com.serviciudad.presupuesto.entity.*;
import com.serviciudad.presupuesto.enums.AccionHistorial;
import com.serviciudad.presupuesto.repository.HistorialNecesidadRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class HistorialService {
    private final HistorialNecesidadRepository repository;
    private final NecesidadMapper mapper;
    private final ObjectMapper json;
    public HistorialService(HistorialNecesidadRepository repository, NecesidadMapper mapper, ObjectMapper json) {
        this.repository = repository; this.mapper = mapper; this.json = json;
    }
    public void register(NecesidadPresupuestal after, Usuario actor, AccionHistorial action,
                         String description, NecesidadResponse before) {
        NecesidadResponse current = mapper.toResponse(after);
        HistorialNecesidad history = new HistorialNecesidad();
        history.setNecesidad(after);
        history.setUsuario(actor);
        history.setAccion(action);
        history.setDescripcion(description);
        history.setEstadoAnterior(before == null ? null : before.estado());
        history.setEstadoNuevo(current.estado());
        history.setValorSolicitado(current.valorTotalSolicitado());
        history.setValorAprobadoAnterior(before == null ? null : before.valorTotalAprobado());
        history.setValorAprobadoNuevo(current.valorTotalAprobado());
        history.setDetalleAnterior(before == null ? null : serialize(before));
        history.setDetalleNuevo(serialize(current));
        history.setFecha(LocalDateTime.now());
        repository.save(history);
    }
    private String serialize(NecesidadResponse response) {
        try { return json.writeValueAsString(response); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("No fue posible registrar la auditoría", ex); }
    }
}
