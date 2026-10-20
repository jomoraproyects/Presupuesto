package com.serviciudad.presupuesto;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.assertThat;

class CatalogoGeneralMigrationTest {
    @Test void reemplazaBaseCon85ConceptosSinRenombrarIdentidadesHistoricasYConservaPersonalizados() {
        String url = "jdbc:h2:mem:catalogo_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        Flyway.configure().dataSource(url, "sa", "").target("9").load().migrate();
        JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(url, "sa", ""));
        db.update("INSERT INTO vigencia(id,anio,nombre,fecha_inicio,fecha_fin,presupuesto_disponible,estado,created_at,updated_at) VALUES(501,2027,'Presupuesto 2027','2027-01-01','2027-12-31',0,'ABIERTA',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        db.update("INSERT INTO rubro_presupuestal(codigo,nombre,nivel,permite_movimiento,categoria_formulacion,activo,orden,vigencia_id,created_at,updated_at) VALUES('FORM.CONCEPTO.PERSONALIZADO','Creado dentro del año',0,TRUE,TRUE,TRUE,90,501,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        Long original = db.queryForObject("SELECT id FROM rubro_presupuestal WHERE codigo='FORM.CONCEPTO.001'", Long.class);
        Flyway migrated = Flyway.configure().dataSource(url, "sa", "").load();
        assertThat(migrated.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM rubro_presupuestal WHERE vigencia_id IS NULL AND eliminado=FALSE", Integer.class)).isEqualTo(86);
        assertThat(db.queryForObject("SELECT nombre FROM rubro_presupuestal WHERE id=?", String.class, original)).isEqualTo("SUELDO PERSONAL NOMINA ADMINISTRATIVO");
        assertThat(db.queryForObject("SELECT eliminado FROM rubro_presupuestal WHERE id=?", Boolean.class, original)).isTrue();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM rubro_presupuestal WHERE vigencia_id=501", Integer.class)).isEqualTo(66);
        assertThat(db.queryForObject("SELECT nombre FROM rubro_presupuestal WHERE codigo='FORM.CONCEPTO.001' AND vigencia_id=501", String.class)).isEqualTo("SUELDO PERSONAL NOMINA ADMINISTRATIVO");
        assertThat(db.queryForObject("SELECT nombre FROM rubro_presupuestal WHERE codigo='FORM.CONCEPTO.PERSONALIZADO' AND vigencia_id IS NULL", String.class)).isEqualTo("CREADO DENTRO DEL AÑO");
        assertThat(db.queryForObject("SELECT COUNT(*) FROM rubro_presupuestal WHERE codigo LIKE 'FORM.CONCEPTO.CG.%' AND orden=81", Integer.class)).isEqualTo(2);
        assertThat(db.queryForObject("SELECT COUNT(DISTINCT id) FROM rubro_presupuestal WHERE codigo LIKE 'FORM.CONCEPTO.CG.%' AND nombre='SEGURIDAD INDUSTRIAL'", Integer.class)).isEqualTo(2);
        assertThat(migrated.migrate().migrationsExecuted).isZero();
    }
}
