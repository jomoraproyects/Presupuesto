package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.LoginRequest;
import com.serviciudad.presupuesto.dto.LoginResponse;
import com.serviciudad.presupuesto.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwt;
    public AuthService(AuthenticationManager authenticationManager, JwtService jwt) {
        this.authenticationManager = authenticationManager; this.jwt = jwt;
    }
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        return new LoginResponse(jwt.issue(request.username()), "Bearer", jwt.expirationSeconds());
    }
}
