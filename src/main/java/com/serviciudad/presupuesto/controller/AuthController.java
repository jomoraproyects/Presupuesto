package com.serviciudad.presupuesto.controller;

import com.serviciudad.presupuesto.dto.LoginRequest;
import com.serviciudad.presupuesto.dto.LoginResponse;
import com.serviciudad.presupuesto.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(service.login(request));
    }
}
