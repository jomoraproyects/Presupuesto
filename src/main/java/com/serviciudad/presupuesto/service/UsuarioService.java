package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.entity.*;
import com.serviciudad.presupuesto.enums.NombreRol;
import com.serviciudad.presupuesto.exception.*;
import com.serviciudad.presupuesto.repository.*;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final AreaRepository areas;
    private final PasswordEncoder encoder;
    public UsuarioService(UsuarioRepository usuarios, RolRepository roles, AreaRepository areas, PasswordEncoder encoder) {
        this.usuarios = usuarios; this.roles = roles; this.areas = areas; this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> list() { return usuarios.findAll().stream().map(this::toResponse).toList(); }
    @Transactional(readOnly = true)
    public UsuarioResponse get(Long id) { return toResponse(find(id)); }
    public UsuarioResponse create(UsuarioCreateRequest request) {
        String username = request.username().trim();
        if (usuarios.findByUsername(username).isPresent()) throw new BusinessException("El usuario ya existe");
        Usuario user = new Usuario();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(request.password()));
        apply(user, request.nombre(), request.rol(), request.areaId());
        return toResponse(usuarios.saveAndFlush(user));
    }
    public UsuarioResponse update(Long id, UsuarioUpdateRequest request) {
        Usuario user = find(id);
        apply(user, request.nombre(), request.rol(), request.areaId());
        return toResponse(usuarios.saveAndFlush(user));
    }
    public UsuarioResponse setEstado(Long id, EstadoRequest request) {
        Usuario user = find(id);
        user.setActivo(request.activo());
        return toResponse(usuarios.saveAndFlush(user));
    }
    private void apply(Usuario user, String nombre, NombreRol role, Long areaId) {
        user.setNombre(nombre.trim());
        user.setRol(roles.findByNombre(role).orElseThrow(() -> new NotFoundException("Rol no encontrado")));
        if (role == NombreRol.RESPONSABLE_AREA) {
            if (areaId == null) throw new BusinessException("El responsable debe tener un área");
            Area area = areas.findById(areaId).orElseThrow(() -> new NotFoundException("Área no encontrada"));
            if (!area.isActiva()) throw new BusinessException("El área está inactiva");
            user.setArea(area);
        } else {
            if (areaId != null) throw new BusinessException("Solo RESPONSABLE_AREA puede tener un área asignada");
            user.setArea(null);
        }
    }
    private Usuario find(Long id) { return usuarios.findById(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado")); }
    private UsuarioResponse toResponse(Usuario user) {
        return new UsuarioResponse(user.getId(), user.getUsername(), user.getNombre(), user.getRol().getNombre(),
            user.getArea() == null ? null : user.getArea().getId(), user.isActivo(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
