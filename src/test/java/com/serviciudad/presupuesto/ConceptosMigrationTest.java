package com.serviciudad.presupuesto;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.assertThat;

class ConceptosMigrationTest {
    @Test void carga65ConceptosSinBorrarSolicitudesNiCatalogoAnterior() {
        String url = "jdbc:h2:mem:conceptos_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        Flyway.configure().dataSource(url, "sa", "").target("6").load().migrate();
        JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(url, "sa", ""));
        db.update("INSERT INTO area(id,codigo,nombre,created_at,updated_at) VALUES(501,'TEC','Técnica',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        db.update("INSERT INTO vigencia(id,anio,nombre,fecha_inicio,fecha_fin,presupuesto_disponible,estado,created_at,updated_at) VALUES(501,2027,'Histórica','2027-01-01','2027-12-31',0,'ABIERTA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        db.update("INSERT INTO rubro_presupuestal(id,codigo,nombre,nivel,permite_movimiento,created_at,updated_at) VALUES(501,'FORM.XLSX.24','Con histórico',0,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP),(502,'FORM.XLSX.25','Sin histórico',0,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        db.update("INSERT INTO usuario(id,username,password_hash,nombre,rol_id,area_id,created_at,updated_at) SELECT 404,'tecnica','hash-tecnica','Técnica',id,501,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM rol WHERE nombre='RESPONSABLE_AREA'");
        db.update("INSERT INTO necesidad_presupuestal(id,vigencia_id,area_id,rubro_presupuestal_id,descripcion,justificacion,unidad_medida,cantidad_solicitada,valor_unitario_solicitado,valor_total_solicitado,cantidad_solicitada_original,valor_unitario_solicitado_original,valor_total_solicitado_original,cantidad_aprobada,valor_unitario_aprobado,valor_total_aprobado,estado,creado_por_id,aplica_distribucion,created_at,updated_at) VALUES(601,501,501,501,'Escritorios','Histórica','unidad',10,800000,8000000,10,800000,8000000,8,800000,6400000,'APROBADA_CON_AJUSTES',404,FALSE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        db.update("INSERT INTO historial_necesidad(id,necesidad_id,usuario_id,accion,descripcion,estado_nuevo,valor_solicitado,valor_aprobado_nuevo,detalle_nuevo,fecha) VALUES(701,601,404,'AJUSTE','Histórico','APROBADA_CON_AJUSTES',8000000,6400000,'original',CURRENT_TIMESTAMP)");
        Flyway migrated = Flyway.configure().dataSource(url, "sa", "").target("9").load();
        assertThat(migrated.migrate().migrationsExecuted).isEqualTo(3);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM rubro_presupuestal WHERE codigo LIKE 'FORM.CONCEPTO.%'", Integer.class)).isEqualTo(65);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM rubro_presupuestal WHERE nombre='IMPUESTO PREDIAL'", Integer.class)).isEqualTo(2);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM rubro_presupuestal WHERE nombre='OTROS IMPUESTOS'", Integer.class)).isEqualTo(2);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM rubro_presupuestal WHERE nombre='ADECUACION O CONSTRUCCION DE EDIFICACIONES Y TERRENOS'", Integer.class)).isEqualTo(2);
        assertThat(db.queryForObject("SELECT activo FROM rubro_presupuestal WHERE id=501", Boolean.class)).isTrue();
        assertThat(db.queryForObject("SELECT activo FROM rubro_presupuestal WHERE id=502", Boolean.class)).isFalse();
        assertThat(db.queryForObject("SELECT valor_total_solicitado_original FROM necesidad_presupuestal WHERE id=601", java.math.BigDecimal.class)).isEqualByComparingTo("8000000");
        assertThat(db.queryForObject("SELECT valor_total_aprobado FROM necesidad_presupuestal WHERE id=601", java.math.BigDecimal.class)).isEqualByComparingTo("6400000");
        assertThat(db.queryForObject("SELECT detalle_nuevo FROM historial_necesidad WHERE id=701", String.class)).isEqualTo("original");
        assertThat(migrated.migrate().migrationsExecuted).isZero();
    }
}
