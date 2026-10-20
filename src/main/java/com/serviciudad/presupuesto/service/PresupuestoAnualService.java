package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.dto.*;
import com.serviciudad.presupuesto.entity.*;
import com.serviciudad.presupuesto.enums.EstadoVigencia;
import com.serviciudad.presupuesto.exception.*;
import com.serviciudad.presupuesto.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class PresupuestoAnualService {
    private final VigenciaService vigenciaService;
    private final VigenciaRepository vigencias;
    private final RubroRepository rubros;
    private final AreaRepository areas;
    private final AreaVigenciaRepository participantes;

    public PresupuestoAnualService(VigenciaService vigenciaService, VigenciaRepository vigencias,
        RubroRepository rubros, AreaRepository areas, AreaVigenciaRepository participantes) {
        this.vigenciaService = vigenciaService; this.vigencias = vigencias; this.rubros = rubros;
        this.areas = areas; this.participantes = participantes;
    }

    public VigenciaResponse crear(int anio) {
        List<RubroPresupuestal> catalogo = rubros.findAllByOrderByOrdenAscIdAsc();
        List<RubroPresupuestal> base = catalogo.stream().filter(RubrosAnuales::concepto)
            .filter(r -> r.getVigencia() == null && !r.isEliminado()).toList();
        VigenciaResponse created = vigenciaService.create(new VigenciaRequest(anio, "Presupuesto " + anio,
            LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31), BigDecimal.ZERO, EstadoVigencia.BORRADOR));
        Vigencia target = vigencias.findById(created.id()).orElseThrow();
        for (RubroPresupuestal r : base) {
            RubroPresupuestal copy = new RubroPresupuestal();
            copy.setCodigo(r.getCodigo()); copy.setNombre(r.getNombre()); copy.setDescripcion(r.getDescripcion());
            copy.setVigencia(target); copy.setActivo(r.isActivo()); copy.setPermiteMovimiento(true);
            copy.setCategoriaFormulacion(true); copy.setOrden(r.getOrden()); copy.setNivel(0);
            rubros.save(copy);
        }
        return created;
    }

    public VigenciaResponse enviar(Long vigenciaId) {
        Vigencia v = vigencias.findForUpdate(vigenciaId).orElseThrow(() -> new NotFoundException("Vigencia no encontrada"));
        if (!v.isActiva() || v.getEstado() != EstadoVigencia.BORRADOR)
            throw new BusinessException("Solo puede enviar un presupuesto preparado en borrador");
        List<Area> active = areas.findAll().stream().filter(Area::isActiva).toList();
        if (active.isEmpty()) throw new BusinessException("No hay dependencias activas para recibir el presupuesto");
        for (Area area : active) {
            AreaVigencia p = participantes.findByVigenciaIdAndAreaId(vigenciaId, area.getId()).orElseGet(AreaVigencia::new);
            p.setVigencia(v); p.setArea(area); p.setActiva(true);
            participantes.save(p);
        }
        v.setEstado(EstadoVigencia.ABIERTA);
        vigencias.saveAndFlush(v);
        return vigenciaService.get(vigenciaId);
    }
}
