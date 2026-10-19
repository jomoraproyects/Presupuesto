package com.serviciudad.presupuesto.service;

import com.serviciudad.presupuesto.exception.BusinessException;
import com.serviciudad.presupuesto.exception.NotFoundException;
import com.serviciudad.presupuesto.repository.VigenciaRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EliminacionPresupuestoService {
    private final JdbcTemplate jdbc;
    private final VigenciaRepository vigencias;
    private final EntityManager entities;

    public EliminacionPresupuestoService(JdbcTemplate jdbc, VigenciaRepository vigencias, EntityManager entities) {
        this.jdbc = jdbc;
        this.vigencias = vigencias;
        this.entities = entities;
    }

    @Transactional
    public void eliminar(Long id, String confirmacion) {
        var vigencia = vigencias.findForUpdate(id).orElseThrow(() -> new NotFoundException("Presupuesto no encontrado"));
        if (!("ELIMINAR " + vigencia.getAnio()).equals(confirmacion))
            throw new BusinessException("Escriba ELIMINAR " + vigencia.getAnio() + " para confirmar");
        entities.flush();
        jdbc.update("DELETE FROM historial_necesidad WHERE necesidad_id IN (SELECT id FROM necesidad_presupuestal WHERE vigencia_id = ?)", id);
        jdbc.update("DELETE FROM distribucion_necesidad WHERE necesidad_id IN (SELECT id FROM necesidad_presupuestal WHERE vigencia_id = ?)", id);
        jdbc.update("DELETE FROM necesidad_presupuestal WHERE vigencia_id = ?", id);
        jdbc.update("DELETE FROM mapeo_rubro_servicio WHERE vigencia_id = ?", id);
        jdbc.update("DELETE FROM area_vigencia WHERE vigencia_id = ?", id);
        jdbc.update("UPDATE rubro_presupuestal SET rubro_padre_id = NULL WHERE vigencia_id = ?", id);
        jdbc.update("DELETE FROM rubro_presupuestal WHERE vigencia_id = ?", id);
        jdbc.update("DELETE FROM vigencia WHERE id = ?", id);
        entities.clear();
    }

    @Transactional
    public void reiniciarTodo(String confirmacion) {
        if (!"BORRAR TODOS LOS PRESUPUESTOS".equals(confirmacion))
            throw new BusinessException("Escriba BORRAR TODOS LOS PRESUPUESTOS para confirmar");
        entities.flush();
        List<Long> ids = jdbc.queryForList("SELECT id FROM vigencia", Long.class);
        if (ids.isEmpty()) return;
        jdbc.update("DELETE FROM historial_necesidad");
        jdbc.update("DELETE FROM distribucion_necesidad");
        jdbc.update("DELETE FROM necesidad_presupuestal");
        jdbc.update("DELETE FROM mapeo_rubro_servicio");
        jdbc.update("DELETE FROM area_vigencia");
        jdbc.update("UPDATE rubro_presupuestal SET rubro_padre_id = NULL WHERE vigencia_id IS NOT NULL");
        jdbc.update("DELETE FROM rubro_presupuestal WHERE vigencia_id IS NOT NULL");
        jdbc.update("DELETE FROM vigencia");
        entities.clear();
    }
}
