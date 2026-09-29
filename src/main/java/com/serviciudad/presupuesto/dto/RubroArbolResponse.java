package com.serviciudad.presupuesto.dto;
import java.util.List;
public record RubroArbolResponse(Long id, String codigo, String nombre, String descripcion, int nivel,
                                boolean permiteMovimiento, Long vigenciaId, int orden, List<RubroArbolResponse> hijos) {}
