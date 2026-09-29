package com.serviciudad.presupuesto.repository;
import com.serviciudad.presupuesto.entity.Rol;
import com.serviciudad.presupuesto.enums.NombreRol;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RolRepository extends JpaRepository<Rol, Long> { Optional<Rol> findByNombre(NombreRol nombre); }
