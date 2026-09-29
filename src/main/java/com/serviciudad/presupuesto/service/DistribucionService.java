package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.DistribucionRequest;
import com.serviciudad.presupuesto.entity.DistribucionNecesidad;
import com.serviciudad.presupuesto.entity.NecesidadPresupuestal;
import com.serviciudad.presupuesto.entity.Servicio;
import com.serviciudad.presupuesto.exception.BusinessException;
import com.serviciudad.presupuesto.exception.NotFoundException;
import com.serviciudad.presupuesto.repository.ServicioRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class DistribucionService {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal CENT = new BigDecimal("0.01");
    private final ServicioRepository servicios;
    public DistribucionService(ServicioRepository servicios) { this.servicios = servicios; }

    public void set(NecesidadPresupuestal necesidad, List<DistribucionRequest> requests) {
        List<DistribucionRequest> requested = requests == null ? List.of() : requests;
        if (!requested.isEmpty()) {
            BigDecimal total = requested.stream().map(DistribucionRequest::porcentaje).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.compareTo(HUNDRED) != 0) throw new BusinessException("La distribución debe sumar exactamente 100.00 %");
        }
        Set<Long> ids = new HashSet<>();
        for (DistribucionRequest request : requested) {
            if (!ids.add(request.servicioId())) throw new BusinessException("No se puede repetir un servicio en la distribución");
        }
        necesidad.getDistribuciones().removeIf(d -> !ids.contains(d.getServicio().getId()));
        Map<Long, DistribucionNecesidad> current = new HashMap<>();
        necesidad.getDistribuciones().forEach(d -> current.put(d.getServicio().getId(), d));
        for (DistribucionRequest request : requested) {
            Servicio servicio = servicios.findById(request.servicioId())
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));
            if (!servicio.isActivo()) throw new BusinessException("El servicio está inactivo");
            DistribucionNecesidad distribution = current.get(request.servicioId());
            if (distribution == null) {
                distribution = new DistribucionNecesidad();
                distribution.setNecesidad(necesidad);
                distribution.setServicio(servicio);
                necesidad.getDistribuciones().add(distribution);
            }
            distribution.setPorcentaje(request.porcentaje());
        }
        recalculate(necesidad);
    }

    public void recalculate(NecesidadPresupuestal necesidad) {
        List<DistribucionNecesidad> distributions = necesidad.getDistribuciones();
        if (distributions.isEmpty()) return;
        Map<DistribucionNecesidad, BigDecimal> requested = allocate(necesidad.getValorTotalSolicitado(), distributions);
        BigDecimal approvedTotal = necesidad.getValorTotalAprobado() == null ? BigDecimal.ZERO : necesidad.getValorTotalAprobado();
        Map<DistribucionNecesidad, BigDecimal> approved = allocate(approvedTotal, distributions);
        for (DistribucionNecesidad d : distributions) {
            d.setValorSolicitadoDistribuido(requested.get(d));
            d.setValorAprobadoDistribuido(approved.get(d));
        }
    }

    private Map<DistribucionNecesidad, BigDecimal> allocate(BigDecimal total, List<DistribucionNecesidad> distributions) {
        record Share(DistribucionNecesidad distribution, BigDecimal fraction) {}
        Map<DistribucionNecesidad, BigDecimal> allocated = new IdentityHashMap<>();
        List<Share> fractions = new ArrayList<>();
        BigDecimal floors = BigDecimal.ZERO;
        for (DistribucionNecesidad d : distributions) {
            BigDecimal exact = total.multiply(d.getPorcentaje()).divide(HUNDRED);
            BigDecimal floor = exact.setScale(2, RoundingMode.DOWN);
            allocated.put(d, floor);
            floors = floors.add(floor);
            fractions.add(new Share(d, exact.subtract(floor)));
        }
        fractions.sort(Comparator.comparing(Share::fraction).reversed()
            .thenComparing(s -> s.distribution().getServicio().getId()));
        int cents = total.subtract(floors).movePointRight(2).intValueExact();
        for (int i = 0; i < cents; i++) {
            DistribucionNecesidad d = fractions.get(i % fractions.size()).distribution();
            allocated.put(d, allocated.get(d).add(CENT));
        }
        return allocated;
    }
}
