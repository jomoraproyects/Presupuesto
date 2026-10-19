package com.serviciudad.presupuesto.controller;

import com.serviciudad.presupuesto.dto.LoginRequest;
import com.serviciudad.presupuesto.dto.LoginResponse;
import com.serviciudad.presupuesto.dto.PerfilResponse;
import com.serviciudad.presupuesto.service.AuthService;
import com.serviciudad.presupuesto.security.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    private final UsuarioActual usuarioActual;
    public AuthController(AuthService service, UsuarioActual usuarioActual) {
        this.service = service;
        this.usuarioActual = usuarioActual;
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(service.login(request));
    }
    @GetMapping("/me")
    public PerfilResponse me() {
        var user = usuarioActual.get();
        return new PerfilResponse(user.getId(), user.getUsername(), user.getNombre(),
            user.getRol().getNombre(), user.getArea() == null ? null : user.getArea().getId());
    }
}
