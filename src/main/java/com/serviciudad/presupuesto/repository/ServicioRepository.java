package com.serviciudad.presupuesto.repository;
import com.serviciudad.presupuesto.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ServicioRepository extends JpaRepository<Servicio, Long> { boolean existsByCodigoAndIdNot(String codigo, Long id); }
