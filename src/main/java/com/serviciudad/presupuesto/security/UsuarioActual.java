package com.serviciudad.presupuesto.security;

import com.serviciudad.presupuesto.entity.Usuario;
import com.serviciudad.presupuesto.enums.NombreRol;
import com.serviciudad.presupuesto.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioActual {
    private final UsuarioRepository usuarios;
    public UsuarioActual(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    public Usuario get() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarios.findByUsername(username)
            .orElseThrow(() -> new AccessDeniedException("Usuario no disponible"));
        if (!usuario.isActivo()) throw new AccessDeniedException("Usuario inactivo");
        if (usuario.getRol().getNombre() == NombreRol.RESPONSABLE_AREA && usuario.getArea() == null) {
            throw new AccessDeniedException("El responsable no tiene un área asignada");
        }
        return usuario;
    }

    public boolean is(Usuario usuario, NombreRol rol) { return usuario.getRol().getNombre() == rol; }
    public boolean isAdmin(Usuario usuario) { return is(usuario, NombreRol.SUPER_ADMIN); }
    public boolean puedeRevisar(Usuario usuario) { return isAdmin(usuario); }
}
