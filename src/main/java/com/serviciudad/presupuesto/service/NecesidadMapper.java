package com.serviciudad.presupuesto.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.entity.*;
import java.util.List;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class NecesidadMapper {
    private final ObjectMapper json;
    public NecesidadMapper(ObjectMapper json) { this.json = json; }
    public NecesidadResponse toResponse(NecesidadPresupuestal n) {
        List<DistribucionResponse> distributions = n.getDistribuciones().stream().map(this::toResponse).toList();
        return new NecesidadResponse(n.getId(), n.getVigencia().getId(), n.getArea().getId(),
            n.getRubroPresupuestal().getId(), n.getDescripcion(), n.getJustificacion(), n.getUnidadMedida(),
            n.getCantidadSolicitada(), n.getValorUnitarioSolicitado(), n.getValorTotalSolicitado(),
            n.getCantidadSolicitadaOriginal(), n.getValorUnitarioSolicitadoOriginal(), n.getValorTotalSolicitadoOriginal(),
            n.getCantidadAprobada(), n.getValorUnitarioAprobado(), n.getValorTotalAprobado(),
            n.getEstado(), n.getObservacionPresupuesto(), n.getCreadoPor().getId(),
            n.getRevisadoPor() == null ? null : n.getRevisadoPor().getId(), n.isActiva(), n.getVersion(),
            distributions, n.getCreatedAt(), n.getUpdatedAt(), n.isAplicaDistribucion());
    }
    private DistribucionResponse toResponse(DistribucionNecesidad d) {
        return new DistribucionResponse(d.getId(), d.getServicio().getId(), d.getServicio().getNombre(),
            d.getPorcentaje(), d.getValorSolicitadoDistribuido(), d.getValorAprobadoDistribuido(), d.getPorcentajeAprobado());
    }
    public HistorialNecesidadResponse toResponse(HistorialNecesidad h) {
        JsonNode before = snapshot(h.getDetalleAnterior()), after = snapshot(h.getDetalleNuevo());
        return new HistorialNecesidadResponse(h.getId(), h.getUsuario().getId(), h.getAccion(), h.getDescripcion(),
            h.getEstadoAnterior(), h.getEstadoNuevo(), h.getValorSolicitado(), h.getValorAprobadoAnterior(),
            h.getValorAprobadoNuevo(), h.getDetalleAnterior(), h.getDetalleNuevo(), h.getFecha(), h.getUsuario().getNombre(),
            amount(before, "cantidadAprobada", "cantidadSolicitada"), amount(after, "cantidadAprobada", "cantidadSolicitada"),
            amount(before, "valorUnitarioAprobado", "valorUnitarioSolicitado"), amount(after, "valorUnitarioAprobado", "valorUnitarioSolicitado"),
            after == null || after.path("observacionPresupuesto").isNull() ? null : after.path("observacionPresupuesto").asText(null));
    }
    private JsonNode snapshot(String value) {
        if (value == null) return null;
        try { return json.readTree(value); }
        catch (java.io.IOException ex) { throw new IllegalStateException("Historial presupuestal inválido", ex); }
    }
    private BigDecimal amount(JsonNode node, String approved, String requested) {
        if (node == null) return null;
        JsonNode value = node.get(approved);
        if (value == null || value.isNull()) value = node.get(requested);
        return value == null || value.isNull() ? null : value.decimalValue();
    }
}
