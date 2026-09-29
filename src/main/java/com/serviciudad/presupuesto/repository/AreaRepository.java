package com.serviciudad.presupuesto.repository;
import com.serviciudad.presupuesto.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AreaRepository extends JpaRepository<Area, Long> { boolean existsByCodigoAndIdNot(String codigo, Long id); }
