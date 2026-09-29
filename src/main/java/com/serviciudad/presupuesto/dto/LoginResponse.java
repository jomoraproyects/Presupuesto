package com.serviciudad.presupuesto.dto;
public record LoginResponse(String token, String tokenType, long expiresInSeconds) {}
