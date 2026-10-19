import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Carga local de desarrollo, con backend detenido. No modifica solicitudes ni históricos. */
class ImportarRubrosFinal {
    public static void main(String[] args) throws Exception {
        JsonNode catalog = new ObjectMapper().readTree(Files.readString(Path.of(args[0])));
        int year = Integer.parseInt(args[1]);
        if (!"final".equals(catalog.path("hoja").asText())) throw new IllegalArgumentException("Se requiere la hoja final");
        try (Connection db = DriverManager.getConnection(
            "jdbc:h2:file:./.local/data/presupuesto;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE",
            "sa", Objects.requireNonNull(System.getenv("LOCAL_DB_PASSWORD")))) {
            db.setAutoCommit(false);
            try {
                long yearId;
                try (var q = db.prepareStatement("SELECT id FROM vigencia WHERE anio=? AND activa=TRUE")) {
                    q.setInt(1, year);
                    try (var rs = q.executeQuery()) {
                        if (!rs.next()) throw new IllegalStateException("Prepare primero la vigencia " + year);
                        yearId = rs.getLong(1);
                    }
                }
                Map<String, Long> ids = new HashMap<>();
                Map<String, Integer> levels = new HashMap<>();
                for (var row : catalog.path("rubros")) {
                    String parent = row.path("padre").isNull() ? null : row.path("padre").asText();
                    Long parentId = parent == null ? null : Objects.requireNonNull(ids.get(parent), "Padre ausente " + parent);
                    int level = parent == null ? 0 : levels.get(parent) + 1;
                    String code = row.path("codigo").asText();
                    long id = upsert(db, yearId, code, row.path("nombre").asText(), parentId, level,
                        row.path("fila").asInt() * 10, row.path("movimiento").asBoolean(), false,
                        "Ff.xlsx / final / fila " + row.path("fila").asInt() + "; nivel fuente " + row.path("nivelFuente").asInt());
                    ids.put(code, id); levels.put(code, level);
                }
                Map<String, Long> services = new HashMap<>();
                try (var q = db.createStatement(); var rs = q.executeQuery("SELECT codigo,id FROM servicio WHERE activo=TRUE")) {
                    while (rs.next()) services.put(rs.getString(1), rs.getLong(2));
                }
                int mappings = 0;
                for (var row : catalog.path("categorias")) {
                    String parent = row.path("padre").asText();
                    long origin = upsert(db, yearId, row.path("codigo").asText(), row.path("nombre").asText().toUpperCase(Locale.forLanguageTag("es")),
                        Objects.requireNonNull(ids.get(parent)), levels.get(parent) + 1, row.path("fila").asInt() * 10 - 1,
                        true, true, "Categoría del formulario; clave interna sin código presupuestal. Ff.xlsx / final / fila " + row.path("fila").asInt());
                    for (var destination : row.path("destinos")) {
                        long service = Objects.requireNonNull(services.get(destination.path("servicio").asText()));
                        long target = Objects.requireNonNull(ids.get(destination.path("codigo").asText()));
                        try (var q = db.prepareStatement("SELECT id FROM mapeo_rubro_servicio WHERE vigencia_id=? AND rubro_origen_id=? AND servicio_id=?")) {
                            q.setLong(1, yearId); q.setLong(2, origin); q.setLong(3, service);
                            try (var rs = q.executeQuery()) {
                                if (rs.next()) {
                                    try (var update = db.prepareStatement("UPDATE mapeo_rubro_servicio SET rubro_destino_id=?,activo=TRUE,updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
                                        update.setLong(1, target); update.setLong(2, rs.getLong(1)); update.executeUpdate();
                                    }
                                } else try (var insert = db.prepareStatement("INSERT INTO mapeo_rubro_servicio(vigencia_id,rubro_origen_id,servicio_id,rubro_destino_id,activo,created_at,updated_at) VALUES(?,?,?,?,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)")) {
                                    insert.setLong(1, yearId); insert.setLong(2, origin); insert.setLong(3, service); insert.setLong(4, target); insert.executeUpdate();
                                }
                            }
                        }
                        mappings++;
                    }
                }
                // Solo retira del formulario la plantilla vacía anterior; no borra rubros ni ítems.
                try (var q = db.prepareStatement("UPDATE rubro_presupuestal SET activo=FALSE,updated_at=CURRENT_TIMESTAMP WHERE vigencia_id=? AND (codigo='FORM' OR (codigo LIKE 'FORM.%' AND codigo NOT LIKE 'FORM.XLSX.%')) AND NOT EXISTS (SELECT 1 FROM necesidad_presupuestal n WHERE n.rubro_presupuestal_id=rubro_presupuestal.id) AND NOT EXISTS (SELECT 1 FROM mapeo_rubro_servicio m WHERE m.rubro_origen_id=rubro_presupuestal.id OR m.rubro_destino_id=rubro_presupuestal.id)")) {
                    q.setLong(1, yearId); q.executeUpdate();
                }
                db.commit();
                System.out.println("Vigencia " + year + ": " + ids.size() + " códigos originales, " + catalog.path("categorias").size() + " categorías, " + mappings + " destinos. Históricos conservados.");
            } catch (Exception failure) { db.rollback(); throw failure; }
        }
    }

    private static long upsert(Connection db, long year, String code, String name, Long parent, int level, int order,
        boolean movement, boolean category, String description) throws SQLException {
        Long id = null;
        try (var q = db.prepareStatement("SELECT id FROM rubro_presupuestal WHERE vigencia_id=? AND codigo=?")) {
            q.setLong(1, year); q.setString(2, code);
            try (var rs = q.executeQuery()) {
                if (rs.next()) id = rs.getLong(1);
                if (rs.next()) throw new IllegalStateException("Código duplicado en BD: " + code);
            }
        }
        if (id != null) {
            try (var q = db.prepareStatement("UPDATE rubro_presupuestal SET nombre=?,descripcion=?,rubro_padre_id=?,nivel=?,orden=?,permite_movimiento=?,categoria_formulacion=?,activo=TRUE,updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
                bind(q, name, description, parent, level, order, movement, category); q.setLong(8, id); q.executeUpdate();
            }
            return id;
        }
        try (var q = db.prepareStatement("INSERT INTO rubro_presupuestal(nombre,descripcion,rubro_padre_id,nivel,orden,permite_movimiento,categoria_formulacion,codigo,vigencia_id,activo,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,TRUE,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", Statement.RETURN_GENERATED_KEYS)) {
            bind(q, name, description, parent, level, order, movement, category); q.setString(8, code); q.setLong(9, year); q.executeUpdate();
            try (var keys = q.getGeneratedKeys()) { keys.next(); return keys.getLong(1); }
        }
    }
    private static void bind(PreparedStatement q, String name, String desc, Long parent, int level, int order, boolean movement, boolean category) throws SQLException {
        q.setString(1, name); q.setString(2, desc);
        if (parent == null) q.setNull(3, Types.BIGINT); else q.setLong(3, parent);
        q.setInt(4, level); q.setInt(5, order); q.setBoolean(6, movement); q.setBoolean(7, category);
    }
}
