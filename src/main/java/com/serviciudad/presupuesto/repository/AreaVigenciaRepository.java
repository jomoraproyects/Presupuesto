package com.serviciudad.presupuesto.repository;

import com.serviciudad.presupuesto.entity.AreaVigencia;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaVigenciaRepository extends JpaRepository<AreaVigencia, Long> {
    List<AreaVigencia> findByVigenciaIdOrderByIdAsc(Long vigenciaId);
    Optional<AreaVigencia> findByVigenciaIdAndAreaId(Long vigenciaId, Long areaId);
    boolean existsByVigenciaIdAndAreaIdAndActivaTrue(Long vigenciaId, Long areaId);
}
