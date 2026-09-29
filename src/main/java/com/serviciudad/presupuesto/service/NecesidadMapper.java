package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.entity.*;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NecesidadMapper {
    public NecesidadResponse toResponse(NecesidadPresupuestal n) {
        List<DistribucionResponse> distributions = n.getDistribuciones().stream().map(this::toResponse).toList();
        return new NecesidadResponse(n.getId(), n.getVigencia().getId(), n.getArea().getId(),
            n.getRubroPresupuestal().getId(), n.getDescripcion(), n.getJustificacion(), n.getUnidadMedida(),
            n.getCantidadSolicitada(), n.getValorUnitarioSolicitado(), n.getValorTotalSolicitado(),
            n.getCantidadSolicitadaOriginal(), n.getValorUnitarioSolicitadoOriginal(), n.getValorTotalSolicitadoOriginal(),
            n.getCantidadAprobada(), n.getValorUnitarioAprobado(), n.getValorTotalAprobado(),
            n.getEstado(), n.getObservacionPresupuesto(), n.getCreadoPor().getId(),
            n.getRevisadoPor() == null ? null : n.getRevisadoPor().getId(), n.isActiva(), n.getVersion(),
            distributions, n.getCreatedAt(), n.getUpdatedAt());
    }
    private DistribucionResponse toResponse(DistribucionNecesidad d) {
        return new DistribucionResponse(d.getId(), d.getServicio().getId(), d.getServicio().getNombre(),
            d.getPorcentaje(), d.getValorSolicitadoDistribuido(), d.getValorAprobadoDistribuido());
    }
    public HistorialNecesidadResponse toResponse(HistorialNecesidad h) {
        return new HistorialNecesidadResponse(h.getId(), h.getUsuario().getId(), h.getAccion(), h.getDescripcion(),
            h.getEstadoAnterior(), h.getEstadoNuevo(), h.getValorSolicitado(), h.getValorAprobadoAnterior(),
            h.getValorAprobadoNuevo(), h.getDetalleAnterior(), h.getDetalleNuevo(), h.getFecha());
    }
}
