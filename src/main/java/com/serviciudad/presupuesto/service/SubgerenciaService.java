package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.enums.NombreRol;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubgerenciaService {
    private final AreaService areas;
    private final UsuarioService usuarios;
    private final ConfiguracionVigenciaService configuracion;

    public SubgerenciaService(AreaService areas, UsuarioService usuarios, ConfiguracionVigenciaService configuracion) {
        this.areas = areas; this.usuarios = usuarios; this.configuracion = configuracion;
    }

    @Transactional
    public SubgerenciaResponse create(SubgerenciaCreateRequest request) {
        AreaResponse area = areas.create(request.area());
        var responsable = request.responsable();
        UsuarioResponse usuario = usuarios.create(new UsuarioCreateRequest(responsable.username(), responsable.password(),
            responsable.nombre(), NombreRol.RESPONSABLE_AREA, area.id()));
        if (request.vigenciaId() != null) {
            configuracion.setParticipante(request.vigenciaId(), area.id(), new EstadoRequest(true));
        }
        return new SubgerenciaResponse(area, usuario, request.vigenciaId());
    }
}
