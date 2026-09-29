package com.serviciudad.presupuesto.repository;
import com.serviciudad.presupuesto.entity.RubroPresupuestal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RubroRepository extends JpaRepository<RubroPresupuestal, Long> {
    List<RubroPresupuestal> findAllByOrderByOrdenAscIdAsc();
    List<RubroPresupuestal> findAllByCodigo(String codigo);
    List<RubroPresupuestal> findByRubroPadreId(Long padreId);
    boolean existsByRubroPadreIdAndActivoTrue(Long padreId);
}
