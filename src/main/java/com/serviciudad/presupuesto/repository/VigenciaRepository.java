package com.serviciudad.presupuesto.repository;
import com.serviciudad.presupuesto.entity.Vigencia;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface VigenciaRepository extends JpaRepository<Vigencia, Long> {
    boolean existsByAnioAndIdNot(Integer anio, Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vigencia v where v.id = :id")
    Optional<Vigencia> findForUpdate(@Param("id") Long id);
}
