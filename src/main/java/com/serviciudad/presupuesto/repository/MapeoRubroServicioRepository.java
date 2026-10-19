package com.serviciudad.presupuesto.repository;

import com.serviciudad.presupuesto.entity.MapeoRubroServicio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MapeoRubroServicioRepository extends JpaRepository<MapeoRubroServicio, Long> {
    List<MapeoRubroServicio> findByVigenciaIdOrderByIdAsc(Long vigenciaId);
    Optional<MapeoRubroServicio> findByVigenciaIdAndRubroOrigenIdAndServicioId(Long vigenciaId, Long rubroOrigenId, Long servicioId);
    boolean existsByRubroDestinoId(Long rubroId);
    boolean existsByRubroOrigenId(Long rubroId);
}
