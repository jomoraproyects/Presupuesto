package com.serviciudad.presupuesto.repository;
import com.serviciudad.presupuesto.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UsuarioRepository extends JpaRepository<Usuario, Long> { Optional<Usuario> findByUsername(String username); }
