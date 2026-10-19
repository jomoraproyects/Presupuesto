package com.serviciudad.presupuesto.config;

import com.serviciudad.presupuesto.entity.Usuario;
import com.serviciudad.presupuesto.enums.NombreRol;
import com.serviciudad.presupuesto.repository.RolRepository;
import com.serviciudad.presupuesto.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {
    @Bean
    ApplicationRunner seedAdmin(@Value("${app.admin.username:}") String username,
                                @Value("${app.admin.password:}") String password,
                                @Value("${app.admin.sync-password-on-start:false}") boolean syncPasswordOnStart,
                                UsuarioRepository usuarios, RolRepository roles, PasswordEncoder encoder) {
        return args -> {
            if (username.isBlank() && password.isBlank()) return;
            if (username.isBlank() || password.isBlank()) throw new IllegalStateException("ADMIN_USERNAME y ADMIN_PASSWORD deben configurarse juntos");
            if (password.length() < (syncPasswordOnStart ? 11 : 12)) throw new IllegalStateException("ADMIN_PASSWORD es demasiado corta");
            var existing = usuarios.findByUsername(username);
            if (existing.isPresent()) {
                if (syncPasswordOnStart && !encoder.matches(password, existing.get().getPasswordHash())) {
                    Usuario admin = existing.get();
                    admin.setPasswordHash(encoder.encode(password));
                    usuarios.save(admin);
                }
                return;
            }
            Usuario admin = new Usuario();
            admin.setUsername(username);
            admin.setNombre("Administrador de desarrollo");
            admin.setPasswordHash(encoder.encode(password));
            admin.setRol(roles.findByNombre(NombreRol.SUPER_ADMIN).orElseThrow());
            usuarios.save(admin);
        };
    }
}
