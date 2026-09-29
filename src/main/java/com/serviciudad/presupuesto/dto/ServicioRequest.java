package com.serviciudad.presupuesto.dto;
import jakarta.validation.constraints.*;
public record ServicioRequest(@NotBlank @Size(max=40) String codigo, @NotBlank @Size(max=150) String nombre,
                              @Size(max=500) String descripcion) {}
