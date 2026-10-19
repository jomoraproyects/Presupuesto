package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.DistribucionRequest;
import com.serviciudad.presupuesto.entity.DistribucionNecesidad;
import com.serviciudad.presupuesto.entity.NecesidadPresupuestal;
import com.serviciudad.presupuesto.entity.Servicio;
import com.serviciudad.presupuesto.enums.EstadoNecesidadPresupuestal;
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
        necesidad.getDistribuciones().removeIf(d -> !ids.contains(d.getServicio().getId())
            && (necesidad.getEstado() != EstadoNecesidadPresupuestal.BORRADOR
                || necesidad.getRevisadoPor() == null || d.getPorcentajeAprobado() == null
                || d.getPorcentajeAprobado().signum() == 0));
        necesidad.getDistribuciones().stream().filter(d -> !ids.contains(d.getServicio().getId()))
            .forEach(d -> d.setPorcentaje(BigDecimal.ZERO));
        Map<Long, DistribucionNecesidad> current = new HashMap<>();
        necesidad.getDistribuciones().forEach(d -> current.put(d.getServicio().getId(), d));
        for (DistribucionRequest request : requested) {
            Servicio servicio = servicios.findById(request.servicioId())
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));
            if (!servicio.isActivo() || !Set.of("21", "22", "23").contains(servicio.getCodigo()))
                throw new BusinessException("El servicio no está disponible para distribución");
            DistribucionNecesidad distribution = current.get(request.servicioId());
            if (distribution == null) {
                distribution = new DistribucionNecesidad();
                distribution.setNecesidad(necesidad);
                distribution.setServicio(servicio);
                necesidad.getDistribuciones().add(distribution);
            }
            distribution.setPorcentaje(request.porcentaje());
            distribution.setPorcentajeAprobado(request.porcentaje());
        }
        recalculate(necesidad);
    }

    public void setApproved(NecesidadPresupuestal necesidad, List<DistribucionRequest> requests) {
        List<DistribucionRequest> approved = requests == null ? List.of() : requests;
        validate(approved);
        Map<Long, DistribucionNecesidad> current = new HashMap<>();
        necesidad.getDistribuciones().forEach(d -> { d.setPorcentajeAprobado(BigDecimal.ZERO); current.put(d.getServicio().getId(), d); });
        for (DistribucionRequest request : approved) {
            Servicio service = servicios.findById(request.servicioId())
                .orElseThrow(() -> new NotFoundException("Servicio no encontrado"));
            if (!service.isActivo() || !Set.of("21", "22", "23").contains(service.getCodigo()))
                throw new BusinessException("El servicio no está disponible para distribución");
            DistribucionNecesidad d = current.get(service.getId());
            if (d == null) {
                d = new DistribucionNecesidad(); d.setNecesidad(necesidad); d.setServicio(service);
                d.setPorcentaje(BigDecimal.ZERO); necesidad.getDistribuciones().add(d);
            }
            d.setPorcentajeAprobado(request.porcentaje());
        }
        recalculate(necesidad);
    }

    private void validate(List<DistribucionRequest> requests) {
        if (!requests.isEmpty() && requests.stream().map(DistribucionRequest::porcentaje)
            .reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(HUNDRED) != 0)
            throw new BusinessException("La distribución aprobada debe sumar exactamente 100.00 %");
        Set<Long> ids = new HashSet<>();
        for (DistribucionRequest request : requests)
            if (!ids.add(request.servicioId())) throw new BusinessException("No se puede repetir un servicio");
    }

    public void recalculate(NecesidadPresupuestal necesidad) {
        List<DistribucionNecesidad> distributions = necesidad.getDistribuciones();
        if (distributions.isEmpty()) return;
        Map<DistribucionNecesidad, BigDecimal> requested = allocate(necesidad.getValorTotalSolicitado(), distributions, false);
        BigDecimal approvedTotal = necesidad.getValorTotalAprobado() == null ? BigDecimal.ZERO : necesidad.getValorTotalAprobado();
        Map<DistribucionNecesidad, BigDecimal> approved = allocate(approvedTotal, distributions, true);
        for (DistribucionNecesidad d : distributions) {
            d.setValorSolicitadoDistribuido(requested.getOrDefault(d, BigDecimal.ZERO.setScale(2)));
            d.setValorAprobadoDistribuido(approved.getOrDefault(d, BigDecimal.ZERO.setScale(2)));
        }
    }

    private Map<DistribucionNecesidad, BigDecimal> allocate(BigDecimal total, List<DistribucionNecesidad> distributions, boolean approved) {
        record Share(DistribucionNecesidad distribution, BigDecimal fraction) {}
        Map<DistribucionNecesidad, BigDecimal> allocated = new IdentityHashMap<>();
        List<Share> fractions = new ArrayList<>();
        BigDecimal floors = BigDecimal.ZERO;
        for (DistribucionNecesidad d : distributions) {
            BigDecimal percentage = approved ? d.getPorcentajeAprobado() : d.getPorcentaje();
            if (percentage == null || percentage.signum() == 0) continue;
            BigDecimal exact = total.multiply(percentage).divide(HUNDRED);
            BigDecimal floor = exact.setScale(2, RoundingMode.DOWN);
            allocated.put(d, floor);
            floors = floors.add(floor);
            fractions.add(new Share(d, exact.subtract(floor)));
        }
        if (fractions.isEmpty()) return allocated;
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
