package com.serviciudad.presupuesto.security;

import com.serviciudad.presupuesto.entity.Usuario;
import com.serviciudad.presupuesto.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    public UsuarioDetailsService(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarios.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        return User.withUsername(usuario.getUsername()).password(usuario.getPasswordHash())
            .roles(usuario.getRol().getNombre().name()).disabled(!usuario.isActivo()).build();
    }
}
