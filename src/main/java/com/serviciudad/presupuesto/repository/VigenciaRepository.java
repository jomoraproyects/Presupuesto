package com.serviciudad.presupuesto.repository;
import com.serviciudad.presupuesto.entity.Vigencia;
import org.springframework.data.jpa.repository.JpaRepository;
public interface VigenciaRepository extends JpaRepository<Vigencia, Long> { boolean existsByAnioAndIdNot(Integer anio, Long id); }
