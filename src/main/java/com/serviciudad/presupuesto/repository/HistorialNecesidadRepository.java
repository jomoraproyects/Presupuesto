package com.serviciudad.presupuesto.repository;

import com.serviciudad.presupuesto.entity.HistorialNecesidad;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialNecesidadRepository extends JpaRepository<HistorialNecesidad, Long> {
    List<HistorialNecesidad> findByNecesidadIdOrderByFechaAscIdAsc(Long necesidadId);
}
