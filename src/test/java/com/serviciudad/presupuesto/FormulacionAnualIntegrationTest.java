package com.serviciudad.presupuesto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:formulacion_anual;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.username=sa", "spring.datasource.password=", "app.admin.username=admin_anual"
})
class FormulacionAnualIntegrationTest {
    private static final String PASSWORD = UUID.randomUUID() + "A1";
    @DynamicPropertySource static void secrets(DynamicPropertyRegistry r) {
        r.add("app.jwt.secret", () -> UUID.randomUUID().toString() + UUID.randomUUID());
        r.add("app.admin.password", () -> PASSWORD);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired com.serviciudad.presupuesto.repository.AreaVigenciaRepository envios;
    String admin, owner, budget;
    long vigencia, area, root, category;
    @BeforeEach void setup() throws Exception {
        admin = login("admin_anual");
        area = req(post("/api/areas"), admin, Map.of("codigo", "OP", "nombre", "Operativa"), 201).path("id").asLong();
        vigencia = req(post("/api/vigencias"), admin, year(2027), 201).path("id").asLong();
        root = rubro("2", "GASTOS", null, false, true);
        category = rubro("2.1", "CONTRATACIÓN PERSONAL ADMINISTRATIVO", root, true, true);
        req(post("/api/usuarios"), admin, Map.of("username", "owner_anual", "password", PASSWORD, "nombre", "Responsable Operativa", "rol", "RESPONSABLE_AREA", "areaId", area), 201);
        req(post("/api/usuarios"), admin, Map.of("username", "administrador_anual", "password", PASSWORD, "nombre", "Administrador de presupuesto", "rol", "SUPER_ADMIN"), 201);
        owner = login("owner_anual"); budget = login("administrador_anual");
    }

    @Test void formularioCompletoDeTecnicaRecortaYRecalculaTodosLosTotales() throws Exception {
        req(put("/api/areas/" + area), admin, Map.of("codigo", "TEC-TEST", "nombre", "Técnica"), 200);
        participant(area, true);
        // Códigos de prueba aislados: no se cargan ni se presentan como códigos del Excel real.
        long parent = rubro("PRUEBA-009", "TÍTULO MUEBLES", null, false, false);
        category = rubro("009.00000001", "COMPRA MUEBLES Y ENSERES", parent, true, true);
        rubro("001.00000001", "OTRO SUBRUBRO DE PRUEBA", parent, true, true);
        long desk = create("Escritorios", 10, 800000, List.of(), false);
        create("Sillas", 20, 450000, List.of(), false);
        create("Archivadores", 5, 900000, List.of(), false);
        JsonNode draft = req(get("/api/formulacion/vigencias/" + vigencia), owner, null, 200);
        amount(draft, "solicitado", "21500000");
        amount(req(get(base() + "/resumen"), budget, null, 200), "totalSolicitado", "21500000");
        req(put("/api/necesidades/" + desk + "/aprobado"), owner, Map.of("cantidadAprobada", 8), 403);
        JsonNode sent = req(post("/api/formulacion/vigencias/" + vigencia + "/enviar"), owner, null, 200);
        assertThat(sent.path("enviada").asBoolean()).isTrue();
        assertThat(sent.path("puedeCrear").asBoolean()).isFalse();
        amount(sent, "aprobado", "21500000");
        req(post("/api/necesidades"), owner, need("Nuevo ítem", 1, 1, List.of(), false), 409);
        req(put("/api/necesidades/" + desk), owner, need("Escritorios", 8, 800000, List.of(), false), 409);
        JsonNode adjusted = req(put("/api/necesidades/" + desk + "/aprobado"), budget,
            Map.of("cantidadAprobada", 8, "valorUnitarioAprobado", 800000, "observacionPresupuesto", "Recorte presupuestal"), 200);
        amount(adjusted, "cantidadSolicitada", "10");
        amount(adjusted, "valorUnitarioSolicitado", "800000");
        amount(adjusted, "valorTotalSolicitadoOriginal", "8000000");
        amount(adjusted, "valorTotalSolicitado", "8000000");
        amount(adjusted, "valorTotalAprobado", "6400000");
        JsonNode form = req(get("/api/formulacion/vigencias/" + vigencia), budget, null, 409);
        form = req(get("/api/formulacion/vigencias/" + vigencia + "?areaId=" + area), budget, null, 200);
        amount(form, "solicitado", "21500000"); amount(form, "aprobado", "19900000");
        for (JsonNode group : form.path("rubros")) if (group.path("rubroId").asLong() == category) {
            assertThat(group.path("cantidadNecesidades").asInt()).isEqualTo(3);
            amount(group, "solicitado", "21500000"); amount(group, "aprobado", "19900000");
        }
        JsonNode summary = req(get(base() + "/resumen"), budget, null, 200);
        amount(summary, "totalSolicitado", "21500000"); amount(summary, "totalAprobado", "19900000");
        for (JsonNode row : req(get(base() + "/por-area"), budget, null, 200)) if (row.path("areaId").asLong() == area) {
            amount(row, "solicitado", "21500000"); amount(row, "aprobado", "19900000");
        }
        for (JsonNode row : req(get(base() + "/por-rubro"), budget, null, 200)) {
            amount(row, "solicitado", "21500000"); amount(row, "aprobado", "19900000");
        }
        JsonNode master = req(get(base() + "/maestro"), budget, null, 200);
        amount(master, "totalSolicitado", "21500000"); amount(master, "totalAprobado", "19900000");
        for (JsonNode row : master.path("rubros")) if (row.path("rubroId").asLong() == parent) {
            amount(row, "aprobado", "19900000");
            assertThat(row.path("hijos").get(0).path("codigo").asText()).isEqualTo("009.00000001");
            assertThat(row.path("hijos").get(1).path("codigo").asText()).isEqualTo("001.00000001");
            amount(row.path("hijos").get(0), "aprobado", "19900000");
        }
        assertThat(req(get(base() + "/por-servicio"), budget, null, 200).size()).isZero();
        // Un segundo recorte usa la misma operación, sin iniciar revisión ni aprobar por separado.
        req(put("/api/necesidades/" + desk + "/aprobado"), admin,
            Map.of("cantidadAprobada", 7, "observacionPresupuesto", "Segundo recorte"), 200);
        amount(req(get(base() + "/resumen"), budget, null, 200), "totalAprobado", "19100000");
        JsonNode history = req(get("/api/necesidades/" + desk + "/historial"), budget, null, 200);
        assertThat(history.get(2).path("estadoAnterior").asText()).isEqualTo("ENVIADA");
        assertThat(history.get(2).path("estadoNuevo").asText()).isEqualTo("EN_REVISION");
        long next = req(post("/api/vigencias/" + vigencia + "/copiar-configuracion"), budget,
            Map.of("vigencia", year(2028), "copiarParticipantes", true), 201).path("id").asLong();
        JsonNode nextForm = req(get("/api/formulacion/vigencias/" + next), owner, null, 200);
        assertThat(nextForm.path("enviada").asBoolean()).isFalse();
        assertThat(nextForm.path("puedeCrear").asBoolean()).isTrue(); amount(nextForm, "solicitado", "0");
    }

    @Test void variosItemsPorRubroConservanSolicitudYRecalculanPadres() throws Exception {
        participant(area, true);
        long architect = create("Arquitecto", 4, 5000000, List.of(), false);
        long nurse = create("Enfermero", 4, 4000000, List.of(), false);
        long builder = create("Maestro de obra", 4, 3500000, List.of(), false);
        JsonNode form = req(get("/api/formulacion/vigencias/" + vigencia), owner, null, 200);
        amount(form, "solicitado", "50000000");
        assertThat(form.path("rubros").get(0).path("cantidadNecesidades").asInt()).isEqualTo(3);
        for (long id : List.of(architect, nurse, builder)) {
            req(post("/api/necesidades/" + id + "/enviar"), owner, null, 200);
            req(post("/api/necesidades/" + id + "/iniciar-revision"), budget, null, 200);
        }
        req(patch("/api/necesidades/" + architect + "/ajuste"), budget, Map.of("cantidadAprobada", 2), 409);
        JsonNode adjusted = req(patch("/api/necesidades/" + architect + "/ajuste"), budget,
            Map.of("cantidadAprobada", 2, "observacionPresupuesto", "Reducción por techo"), 200);
        amount(adjusted, "cantidadSolicitada", "4"); amount(adjusted, "valorTotalSolicitado", "20000000");
        amount(adjusted, "valorTotalSolicitadoOriginal", "20000000"); amount(adjusted, "valorTotalAprobado", "10000000");
        JsonNode intermediate = req(get(base() + "/resumen"), budget, null, 200);
        amount(intermediate, "totalAprobado", "40000000");
        req(patch("/api/necesidades/" + nurse + "/ajuste"), budget, Map.of("cantidadAprobada", 3, "observacionPresupuesto", "Prioridad anual"), 200);
        for (long id : List.of(architect, nurse, builder)) req(post("/api/necesidades/" + id + "/aprobar"), budget, null, 200);
        JsonNode summary = req(get(base() + "/resumen"), budget, null, 200);
        amount(summary, "totalSolicitado", "50000000"); amount(summary, "totalAprobado", "36000000");
        JsonNode grouped = req(get("/api/formulacion/vigencias/" + vigencia), owner, null, 200).path("rubros").get(0);
        amount(grouped, "solicitado", "50000000"); amount(grouped, "aprobado", "36000000"); amount(grouped, "diferencia", "-14000000");
        JsonNode byArea = req(get(base() + "/por-area"), budget, null, 200).get(0); amount(byArea, "aprobado", "36000000");
        JsonNode byRubro = req(get(base() + "/por-rubro"), budget, null, 200); amount(byRubro.get(0), "aprobado", "36000000");
        JsonNode master = req(get(base() + "/maestro"), budget, null, 200);
        assertThat(master.path("mapeoCompleto").asBoolean()).isTrue(); amount(master, "totalAprobado", "36000000");
        JsonNode parent = master.path("rubros").get(0); amount(parent, "aprobado", "36000000"); amount(parent.path("hijos").get(0), "aprobado", "36000000");
        JsonNode history = req(get("/api/necesidades/" + architect + "/historial"), owner, null, 200);
        JsonNode change = history.get(3); amount(change, "cantidadAnterior", "4"); amount(change, "cantidadNueva", "2");
        assertThat(change.path("motivo").asText()).isEqualTo("Reducción por techo");
        assertThat(history.get(1).path("detalleNuevo").asText()).contains("Arquitecto", "justificacion", "distribuciones", "20000000");
        req(patch("/api/necesidades/" + architect + "/ajuste"), owner, Map.of("cantidadAprobada", 8, "observacionPresupuesto", "Inválido"), 403);
    }

    @Test void participacionDinamicaYDesactivacionConservanHistoricos() throws Exception {
        req(post("/api/necesidades"), owner, need("Sin habilitar", 1, 10, List.of(), false), 409);
        assertThat(req(get("/api/formulacion/vigencias/" + vigencia), owner, null, 200).path("puedeCrear").asBoolean()).isFalse();
        participant(area, true);
        long id = create("Histórico", 1, 10, List.of(), false);
        participant(area, false);
        req(post("/api/necesidades"), owner, need("Otra", 1, 10, List.of(), false), 409);
        req(put("/api/necesidades/" + id), owner, need("Histórico corregido", 1, 20, List.of(), false), 200);
        req(post("/api/necesidades/" + id + "/enviar"), owner, null, 200);
        req(patch("/api/areas/" + area + "/estado"), admin, Map.of("activo", false), 200);
        req(get("/api/necesidades/" + id), owner, null, 200);
        req(get("/api/necesidades/" + id + "/historial"), owner, null, 200);
        amount(req(get(base() + "/por-area"), budget, null, 200).get(0), "solicitado", "20");
        req(patch("/api/areas/" + area + "/estado"), admin, Map.of("activo", true), 200);
        participant(area, true);
        create("Reactivada", 1, 10, List.of(), false);
        long legal = req(post("/api/areas"), admin, Map.of("codigo", "JU", "nombre", "Jurídica"), 201).path("id").asLong();
        participant(legal, true);
        req(post("/api/usuarios"), admin, Map.of("username", "legal_anual", "password", PASSWORD, "nombre", "Jurídica", "rol", "RESPONSABLE_AREA", "areaId", legal), 201);
        req(post("/api/necesidades"), login("legal_anual"), newNeed(legal), 201);
        req(get("/api/formulacion/vigencias/" + vigencia + "?areaId=" + legal), owner, null, 403);
        req(put("/api/vigencias/" + vigencia + "/participantes/" + legal), owner, Map.of("activo", false), 403);
        Map<String, Object> closed = new HashMap<>(year(2027)); closed.put("estado", "CERRADA");
        req(put("/api/vigencias/" + vigencia), budget, closed, 200);
        req(put("/api/vigencias/" + vigencia + "/participantes/" + legal), admin, Map.of("activo", false), 409);
    }

    @Test void distribucionesYMapeoMaestroSumanSinDuplicar() throws Exception {
        participant(area, true);
        req(post("/api/necesidades"), owner, need("Inválida", 1, 100, shares(50, 20, 0), true), 409);
        req(post("/api/necesidades"), owner, need("Contradictoria", 1, 100, shares(100, 0, 0), false), 409);
        long one = create("Un servicio", 1, 100, shares(100, 0, 0), true);
        long two = create("Dos servicios", 1, 200, shares(60, 40, 0), true);
        long three = create("Tres servicios", 1, 90, shares(50, 30, 20), true);
        for (long id : List.of(one, two, three)) {
            req(post("/api/necesidades/" + id + "/enviar"), owner, null, 200);
            req(post("/api/necesidades/" + id + "/iniciar-revision"), budget, null, 200);
        }
        req(patch("/api/necesidades/" + three + "/ajuste"), budget, Map.of("valorUnitarioAprobado", 120), 409);
        req(patch("/api/necesidades/" + three + "/ajuste"), budget, Map.of("valorUnitarioAprobado", 120, "observacionPresupuesto", "Nueva cotización"), 200);
        JsonNode reduction = req(patch("/api/necesidades/" + three + "/ajuste"), budget,
            Map.of("valorUnitarioAprobado", 60, "observacionPresupuesto", "Ajuste de alcance"), 200);
        amount(reduction, "valorTotalSolicitado", "90"); amount(reduction.path("distribuciones").get(0), "valorAprobadoDistribuido", "30");
        amount(reduction.path("distribuciones").get(1), "valorAprobadoDistribuido", "18"); amount(reduction.path("distribuciones").get(2), "valorAprobadoDistribuido", "12");
        for (long id : List.of(one, two, three)) req(post("/api/necesidades/" + id + "/aprobar"), budget, null, 200);
        JsonNode pending = req(get(base() + "/maestro"), budget, null, 200);
        assertThat(pending.path("mapeoCompleto").asBoolean()).isFalse(); assertThat(pending.path("mapeosPendientes").size()).isEqualTo(3);
        amount(pending, "totalSolicitado", "390"); amount(pending, "totalAprobado", "360");
        long masterRoot = rubro("9", "MAESTRO", null, false, false);
        long a = rubro("9.1", "Acueducto", masterRoot, false, false);
        long b = rubro("9.2", "Alcantarillado", masterRoot, false, false);
        long c = rubro("9.3", "Aseo", masterRoot, false, false);
        for (long[] pair : List.of(new long[]{1, a}, new long[]{2, b}, new long[]{3, c})) {
            req(post("/api/vigencias/" + vigencia + "/mapeos"), budget,
                Map.of("rubroOrigenId", category, "servicioId", pair[0], "rubroDestinoId", pair[1]), 201);
        }
        req(post("/api/vigencias/" + vigencia + "/mapeos"), budget, Map.of("rubroOrigenId", category, "servicioId", 1, "rubroDestinoId", a), 409);
        JsonNode master = req(get(base() + "/maestro"), budget, null, 200);
        assertThat(master.path("mapeoCompleto").asBoolean()).isTrue(); amount(master, "totalSolicitado", "390"); amount(master, "totalAprobado", "360");
        JsonNode destination = find(master.path("rubros"), masterRoot); amount(destination, "solicitado", "390"); amount(destination, "aprobado", "360");
        amount(find(destination.path("hijos"), a), "aprobado", "250"); amount(find(destination.path("hijos"), b), "aprobado", "98"); amount(find(destination.path("hijos"), c), "aprobado", "12");
        amount(find(master.path("rubros"), root), "aprobado", "0");
        req(post("/api/necesidades"), owner, new HashMap<>(Map.of("vigenciaId", vigencia, "rubroPresupuestalId", a,
            "descripcion", "Final", "justificacion", "No es categoría", "unidadMedida", "unidad", "cantidadSolicitada", 1, "valorUnitarioSolicitado", 1, "aplicaDistribucion", false)), 409);
        req(post("/api/rubros"), admin, Map.of("codigo", "9.1.1", "nombre", "Hijo indebido", "rubroPadreId", a, "vigenciaId", vigencia), 409);
        req(put("/api/rubros/" + a), admin, Map.of("codigo", "9.1", "nombre", "Acueducto", "rubroPadreId", masterRoot,
            "vigenciaId", vigencia, "permiteMovimiento", true, "categoriaFormulacion", true), 409);
        req(put("/api/rubros/" + category), admin, Map.of("codigo", "2.1", "nombre", "Categoría", "rubroPadreId", root,
            "vigenciaId", vigencia, "permiteMovimiento", false, "categoriaFormulacion", false), 409);
    }

    @Test void categoriasDelExcelMantienenGeneralYServiciosSinDobleConteo() throws Exception {
        participant(area, true);
        String code = "2.1.2.01.01.004.01.01.02";
        long general = rubro(code, "MUEBLES DEL TIPO UTILIZADO EN LA OFICINA", null, false, false);
        long acueducto = rubro(code + ".21-  2103051501", "Compra Muebles y Enseres  Acueducto", general, true, false);
        long alcantarillado = rubro(code + ".22-  2203051501", "Compra Muebles y Enseres  Alcantarillado", general, true, false);
        long aseo = rubro(code + ".23-  2303051501", "Compra Muebles y Enseres  Aseo", general, true, false);
        category = rubro("FORM.XLSX.24", "COMPRA MUEBLES Y ENSERES", general, true, true);
        for (long[] pair : List.of(new long[]{1, acueducto}, new long[]{2, alcantarillado}, new long[]{3, aseo})) {
            req(post("/api/vigencias/" + vigencia + "/mapeos"), admin,
                Map.of("rubroOrigenId", category, "servicioId", pair[0], "rubroDestinoId", pair[1]), 201);
        }
        long desk = create("Escritorios", 10, 800000, List.of(), false);
        JsonNode noDistribution = req(get(base() + "/maestro"), budget, null, 200);
        amount(noDistribution, "totalSolicitado", "8000000");
        JsonNode node = find(noDistribution.path("rubros"), general);
        assertThat(node.path("hijos").size()).isEqualTo(3);
        amount(node, "solicitado", "8000000");
        for (JsonNode child : node.path("hijos")) amount(child, "solicitado", "0");
        assertThat(req(get(base() + "/por-servicio"), budget, null, 200).size()).isZero();
        create("Sillas", 20, 450000, shares(60, 40, 0), true);
        create("Archivadores", 5, 900000, List.of(), false);
        req(post("/api/formulacion/vigencias/" + vigencia + "/enviar"), owner, null, 200);
        req(put("/api/necesidades/" + desk + "/aprobado"), budget,
            Map.of("cantidadAprobada", 8, "observacionPresupuesto", "Recorte de muebles"), 200);
        JsonNode master = req(get(base() + "/maestro"), budget, null, 200);
        amount(master, "totalSolicitado", "21500000"); amount(master, "totalAprobado", "19900000");
        assertThat(master.path("mapeoCompleto").asBoolean()).isTrue();
        node = find(master.path("rubros"), general);
        assertThat(node.path("codigo").asText()).isEqualTo(code);
        amount(node, "aprobado", "19900000");
        amount(find(node.path("hijos"), acueducto), "aprobado", "5400000");
        amount(find(node.path("hijos"), alcantarillado), "aprobado", "3600000");
        amount(find(node.path("hijos"), aseo), "aprobado", "0");
        assertThat(find(node.path("hijos"), acueducto).path("codigo").asText()).isEqualTo(code + ".21-  2103051501");
        amount(req(get(base() + "/resumen"), budget, null, 200), "totalAprobado", "19900000");
    }

    @Test void copiarConfiguracionCreaAnioSinSolicitudesNiValores() throws Exception {
        participant(area, true);
        long target = rubro("9", "Destino maestro", null, false, false);
        req(post("/api/vigencias/" + vigencia + "/mapeos"), admin, Map.of("rubroOrigenId", category, "servicioId", 1, "rubroDestinoId", target), 201);
        long id = create("Solo año anterior", 1, 100, shares(100, 0, 0), true);
        req(post("/api/necesidades/" + id + "/enviar"), owner, null, 200);
        req(post("/api/necesidades/" + id + "/iniciar-revision"), budget, null, 200);
        req(post("/api/necesidades/" + id + "/aprobar"), budget, null, 200);
        long next = req(post("/api/vigencias/" + vigencia + "/copiar-configuracion"), budget,
            Map.of("vigencia", year(2028), "copiarParticipantes", true), 201).path("id").asLong();
        JsonNode tree = req(get("/api/rubros/arbol?vigenciaId=" + next), owner, null, 200);
        assertThat(tree.size()).isEqualTo(67); assertThat(tree.get(0).path("id").asLong()).isNotEqualTo(root);
        assertThat(tree.get(0).path("hijos").get(0).path("id").asLong()).isNotEqualTo(category);
        JsonNode maps = req(get("/api/vigencias/" + next + "/mapeos"), budget, null, 200);
        assertThat(maps.size()).isEqualTo(1); assertThat(maps.get(0).path("rubroOrigenId").asLong()).isNotEqualTo(category);
        assertThat(req(get("/api/formulacion/vigencias/" + next), owner, null, 200).path("puedeCrear").asBoolean()).isTrue();
        amount(req(get("/api/presupuesto/vigencias/" + next + "/resumen"), budget, null, 200), "totalAprobado", "0");
        assertThat(req(get("/api/necesidades?vigenciaId=" + next), owner, null, 200).path("totalElements").asInt()).isZero();
        amount(req(get("/api/presupuesto/vigencias/" + next + "/maestro"), budget, null, 200), "totalSolicitado", "0");
        assertThat(req(get("/api/necesidades/" + id + "/historial"), owner, null, 200).size()).isEqualTo(4);
        long noParticipants = req(post("/api/vigencias/" + vigencia + "/copiar-configuracion"), admin,
            Map.of("vigencia", year(2029), "copiarParticipantes", false), 201).path("id").asLong();
        assertThat(req(get("/api/formulacion/vigencias/" + noParticipants), owner, null, 200).path("puedeCrear").asBoolean()).isFalse();
    }

    @Test void copiarPlantillaGlobalVariosAniosNoDuplicaCategoriasNiValores() throws Exception {
        participant(area, true);
        long globalRoot = req(post("/api/rubros"), admin, Map.of("codigo", "G", "nombre", "Plantilla global"), 201).path("id").asLong();
        long globalCategory = req(post("/api/rubros"), admin, Map.of("codigo", "G.1", "nombre", "Hardware", "rubroPadreId", globalRoot,
            "permiteMovimiento", true, "categoriaFormulacion", true), 201).path("id").asLong();
        long globalDestination = req(post("/api/rubros"), admin, Map.of("codigo", "G.2", "nombre", "Hardware Acueducto", "rubroPadreId", globalRoot,
            "categoriaFormulacion", false), 201).path("id").asLong();
        req(post("/api/vigencias/" + vigencia + "/mapeos"), budget,
            Map.of("rubroOrigenId", globalCategory, "servicioId", 1, "rubroDestinoId", globalDestination), 201);
        var original = need("Hardware", 1, 100, shares(100, 0, 0), true);
        original.put("rubroPresupuestalId", globalCategory);
        long id = req(post("/api/necesidades"), owner, original, 201).path("id").asLong();
        req(post("/api/necesidades/" + id + "/enviar"), owner, null, 200);
        long next = req(post("/api/vigencias/" + vigencia + "/copiar-configuracion"), budget,
            Map.of("vigencia", year(2028), "copiarParticipantes", true), 201).path("id").asLong();
        JsonNode form = req(get("/api/formulacion/vigencias/" + next), owner, null, 200);
        assertThat(form.path("rubros").size()).isEqualTo(67);
        amount(form, "solicitado", "0");
        JsonNode master = req(get("/api/presupuesto/vigencias/" + next + "/maestro"), budget, null, 200);
        assertThat(master.path("rubros").size()).isEqualTo(67);
        amount(master, "totalSolicitado", "0");
        original.put("vigenciaId", next);
        req(post("/api/necesidades"), owner, original, 409);
        long yearAfter = req(post("/api/vigencias/" + next + "/copiar-configuracion"), budget,
            Map.of("vigencia", year(2029), "copiarParticipantes", true), 201).path("id").asLong();
        assertThat(req(get("/api/formulacion/vigencias/" + yearAfter), owner, null, 200).path("rubros").size()).isEqualTo(67);
        JsonNode maps = req(get("/api/vigencias/" + yearAfter + "/mapeos"), budget, null, 200);
        assertThat(maps.size()).isEqualTo(1);
        assertThat(maps.get(0).path("rubroOrigenId").asLong()).isNotEqualTo(globalCategory);
        amount(req(get(base() + "/maestro"), budget, null, 200), "totalSolicitado", "100");
    }

    @Test void unRechazoPorAprobadoCeroExigeMotivo() throws Exception {
        participant(area, true);
        long zero = create("Ítem sin costo", 1, 0, List.of(), false);
        req(post("/api/necesidades/" + zero + "/enviar"), owner, null, 409);
        long id = create("Ítem a excluir", 1, 100, List.of(), false);
        req(post("/api/necesidades/" + id + "/enviar"), owner, null, 200);
        req(post("/api/necesidades/" + id + "/iniciar-revision"), budget, null, 200);
        req(patch("/api/necesidades/" + id + "/ajuste"), budget, Map.of("cantidadAprobada", 0), 409);
        req(post("/api/necesidades/" + id + "/rechazar"), budget, Map.of("observacionPresupuesto", " "), 400);
        req(post("/api/necesidades/" + id + "/devolver"), budget, Map.of("observacionPresupuesto", " "), 400);
        req(patch("/api/necesidades/" + id + "/ajuste"), budget, Map.of("cantidadAprobada", 0, "observacionPresupuesto", "Solicitud sin financiación, se excluye"), 200);
        assertThat(req(post("/api/necesidades/" + id + "/aprobar"), budget, null, 200).path("estado").asText()).isEqualTo("RECHAZADA");
    }

    @Test void conceptosRealesConservanOrdenDuplicadosYFlujoArquitectoIngeniero() throws Exception {
        participant(area, true);
        req(put("/api/areas/" + area), admin, Map.of("codigo", "TEC-TEST", "nombre", "Técnica"), 200);
        JsonNode catalog = req(get("/api/rubros/conceptos?vigenciaId=" + vigencia), owner, null, 200);
        JsonNode reference = json.readTree(java.nio.file.Files.readString(java.nio.file.Path.of("docs/conceptos-formulario.json")));
        assertThat(catalog.size()).isEqualTo(65);
        Set<Long> ids = new HashSet<>();
        for (int i=0; i<65; i++) {
            assertThat(catalog.get(i).path("nombre").asText()).isEqualTo(reference.get(i).path("nombre").asText());
            assertThat(catalog.get(i).path("orden").asInt()).isEqualTo(i+1);
            assertThat(ids.add(catalog.get(i).path("id").asLong())).isTrue();
        }
        assertThat(catalog.get(8).path("nombre")).isEqualTo(catalog.get(10).path("nombre"));
        assertThat(catalog.get(22).path("nombre")).isEqualTo(catalog.get(52).path("nombre"));
        assertThat(catalog.get(31).path("nombre")).isEqualTo(catalog.get(54).path("nombre"));
        category = catalog.get(5).path("id").asLong();
        Map<String,Object> architect = need("Arquitecto", 12, 5000000, List.of(), false);
        architect.remove("justificacion"); architect.remove("aplicaDistribucion"); architect.put("unidadMedida", "MES");
        JsonNode created = req(post("/api/necesidades"), owner, architect, 201);
        long id = created.path("id").asLong();
        assertThat(created.path("aplicaDistribucion").asBoolean()).isFalse();
        assertThat(created.path("distribuciones").isEmpty()).isTrue();
        amount(created, "valorTotalSolicitado", "60000000");
        create("Ingeniero", 12, 4000000, List.of(), false);
        // Otra sesión recupera el borrador completo, con totales calculados.
        JsonNode draft = req(get("/api/formulacion/vigencias/" + vigencia), login("owner_anual"), null, 200);
        amount(draft, "solicitado", "108000000");
        amount(req(get(base() + "/resumen"), budget, null, 200), "totalSolicitado", "108000000");
        JsonNode sent = req(post("/api/formulacion/vigencias/" + vigencia + "/enviar"), owner, null, 200);
        assertThat(sent.path("fechaEnvio").isNull()).isFalse();
        assertThat(sent.path("enviadoPorId").asLong()).isEqualTo(created.path("creadoPorId").asLong());
        assertThat(sent.path("areaId").asLong()).isEqualTo(area);
        assertThat(sent.path("vigenciaId").asLong()).isEqualTo(vigencia);
        var submission = envios.findByVigenciaIdAndAreaId(vigencia, area).orElseThrow();
        String snapshot = submission.getDatosEnviados();
        amount(json.readTree(snapshot), "solicitado", "108000000");
        amount(json.readTree(snapshot), "aprobado", "108000000");
        req(post("/api/formulacion/vigencias/" + vigencia + "/enviar"), owner, null, 409);
        req(put("/api/necesidades/" + id), owner, architect, 409);
        req(put("/api/necesidades/" + id + "/aprobado"), owner, Map.of("cantidadAprobada", 10), 403);
        JsonNode cut = req(put("/api/necesidades/" + id + "/aprobado"), budget,
            Map.of("cantidadAprobada", 10, "observacionPresupuesto", "Ajuste de meses contratados"), 200);
        amount(cut, "cantidadSolicitada", "12"); amount(cut, "valorTotalSolicitadoOriginal", "60000000");
        amount(cut, "valorTotalAprobado", "50000000");
        JsonNode reviewed = req(get("/api/formulacion/vigencias/" + vigencia + "?areaId=" + area), budget, null, 200);
        amount(reviewed, "solicitado", "108000000"); amount(reviewed, "aprobado", "98000000");
        for (JsonNode block : reviewed.path("rubros")) if (block.path("rubroId").asLong() == category) amount(block, "aprobado", "98000000");
        amount(req(get(base() + "/por-area"), budget, null, 200).get(0), "aprobado", "98000000");
        amount(req(get(base() + "/resumen"), budget, null, 200), "totalAprobado", "98000000");
        assertThat(submission.getDatosEnviados()).isEqualTo(snapshot);
        // Aumentar también recalcula, sin sustituir la solicitud original ni el envío.
        req(put("/api/necesidades/" + id + "/aprobado"), budget,
            Map.of("cantidadAprobada", 11, "observacionPresupuesto", "Se restituye un mes"), 200);
        amount(req(get(base() + "/resumen"), budget, null, 200), "totalAprobado", "103000000");
        assertThat(submission.getDatosEnviados()).isEqualTo(snapshot);
    }

    @Test void superAdminRecortaBorradorSinPerderSolicitudNiAjusteAlEnviar() throws Exception {
        participant(area, true);
        long item = create("Tubería PVC", 100, 50000, shares(60, 40, 0), true);
        req(put("/api/necesidades/" + item + "/aprobado"), owner,
            Map.of("cantidadAprobada", 80), 403);
        JsonNode cut = req(put("/api/necesidades/" + item + "/aprobado"), admin,
            Map.of("cantidadAprobada", 80, "valorUnitarioAprobado", 50000,
                "distribucionesAprobadas", shares(100, 0, 0)), 200);
        assertThat(cut.path("estado").asText()).isEqualTo("BORRADOR");
        amount(cut, "valorTotalSolicitado", "5000000");
        amount(cut, "valorTotalAprobado", "4000000");
        amount(req(get(base() + "/resumen"), admin, null, 200), "totalAprobado", "4000000");
        JsonNode edited = req(put("/api/necesidades/" + item), owner,
            need("Tubería PVC", 110, 50000, shares(50, 50, 0), true), 200);
        amount(edited, "valorTotalSolicitado", "5500000");
        amount(edited, "valorTotalAprobado", "4000000");
        amount(edited.path("distribuciones").get(0), "porcentajeAprobado", "100");
        JsonNode sent = req(post("/api/formulacion/vigencias/" + vigencia + "/enviar"), owner, null, 200);
        assertThat(sent.path("estado").asText()).isEqualTo("EN_REVISION");
        JsonNode itemAfter = req(get("/api/necesidades/" + item), admin, null, 200);
        amount(itemAfter, "valorTotalSolicitadoOriginal", "5500000");
        amount(itemAfter, "valorTotalAprobado", "4000000");
        amount(req(get(base() + "/resumen"), admin, null, 200), "totalSolicitado", "5500000");
        amount(req(get(base() + "/resumen"), admin, null, 200), "totalAprobado", "4000000");
    }

    @Test void eliminarUnPresupuestoYReiniciarTodosExigeSuperAdminYConfirmacion() throws Exception {
        participant(area, true);
        create("Tubería PVC", 2, 50000, List.of(), false);
        long next = req(post("/api/vigencias"), admin, year(2028), 201).path("id").asLong();
        req(delete("/api/vigencias/" + vigencia), owner, Map.of("confirmacion", "ELIMINAR 2027"), 403);
        req(delete("/api/vigencias/" + vigencia), admin, Map.of("confirmacion", "2027"), 409);
        req(delete("/api/vigencias/" + vigencia), admin, Map.of("confirmacion", "ELIMINAR 2027"), 204);
        req(get("/api/vigencias/" + vigencia), admin, null, 404);
        req(get("/api/vigencias/" + next), admin, null, 200);
        req(delete("/api/vigencias/presupuestos"), admin, Map.of("confirmacion", "BORRAR TODO"), 409);
        req(delete("/api/vigencias/presupuestos"), admin,
            Map.of("confirmacion", "BORRAR TODOS LOS PRESUPUESTOS"), 204);
        assertThat(req(get("/api/vigencias"), admin, null, 200).size()).isZero();
        assertThat(req(get("/api/areas"), admin, null, 200).size()).isPositive();
        assertThat(req(get("/api/rubros/conceptos"), admin, null, 200).size()).isGreaterThanOrEqualTo(65);
    }

    @Test void administradorGestionaConceptosSinFusionarNombresNiBorrarHistoricos() throws Exception {
        participant(area, true);
        Map<String,Object> concept = Map.of("nombre", "Concepto adicional", "orden", 66, "vigenciaId", vigencia,
            "descripcion", "Descripción inicial", "activo", true);
        req(post("/api/rubros/conceptos"), owner, concept, 403);
        req(post("/api/rubros/conceptos"), admin, Map.of("nombre", " TOTAL ", "orden", 66), 409);
        JsonNode first = req(post("/api/rubros/conceptos"), admin, concept, 201);
        Map<String,Object> inactiveConcept = new HashMap<>(concept); inactiveConcept.put("activo", false);
        JsonNode second = req(post("/api/rubros/conceptos"), admin, inactiveConcept, 201);
        assertThat(first.path("id")).isNotEqualTo(second.path("id"));
        assertThat(first.path("descripcion").asText()).isEqualTo("Descripción inicial");
        assertThat(second.path("activo").asBoolean()).isFalse();
        category = first.path("id").asLong();
        long id = create("Item histórico", 2, 100, List.of(), false);
        req(put("/api/rubros/conceptos/" + category), owner, concept, 403);
        JsonNode edited = req(put("/api/rubros/conceptos/" + category), admin,
            Map.of("nombre", "Nombre actualizado", "orden", 2, "descripcion", "Descripción actualizada", "activo", false), 200);
        assertThat(edited.path("descripcion").asText()).isEqualTo("Descripción actualizada");
        assertThat(edited.path("activo").asBoolean()).isFalse();
        for (String field : List.of("id", "codigo", "rubroPadreId", "nivel", "permiteMovimiento", "categoriaFormulacion", "vigenciaId")) {
            assertThat(edited.path(field)).as("Se conserva el campo interno %s", field).isEqualTo(first.path(field));
        }
        // Omitir la descripción en clientes anteriores la conserva; una cadena vacía permite quitarla.
        JsonNode withoutDescription = req(put("/api/rubros/conceptos/" + category), admin,
            Map.of("nombre", "Nombre actualizado", "orden", 2), 200);
        assertThat(withoutDescription.path("descripcion").asText()).isEqualTo("Descripción actualizada");
        req(put("/api/rubros/conceptos/" + category), admin,
            Map.of("nombre", "Nombre actualizado", "orden", 2, "descripcion", ""), 200);
        assertThat(req(get("/api/rubros/" + category), admin, null, 200).path("descripcion").asText()).isEmpty();
        req(patch("/api/rubros/conceptos/" + category + "/estado"), admin, Map.of("activo", false), 200);
        JsonNode form = req(get("/api/formulacion/vigencias/" + vigencia), owner, null, 200);
        amount(form, "solicitado", "200");
        JsonNode historical = null;
        for (JsonNode block : form.path("rubros")) if (block.path("rubroId").asLong() == category) historical = block;
        assertThat(historical).isNotNull(); assertThat(historical.path("habilitado").asBoolean()).isFalse();
        assertThat(historical.path("nombre").asText()).isEqualTo("Nombre actualizado");
        assertThat(historical.path("orden").asInt()).isEqualTo(2);
        req(get("/api/necesidades/" + id + "/historial"), owner, null, 200);
        req(post("/api/necesidades"), owner, need("No permitido", 1, 100, List.of(), false), 409);
        req(patch("/api/rubros/conceptos/" + category + "/estado"), admin, Map.of("activo", true), 200);
        Map<String,Object> invalid = need("Distribución vacía", 1, 100, List.of(), true);
        req(post("/api/necesidades"), owner, invalid, 409);
        create("Distribuido", 1, 100, shares(60,40,0), true);
        amount(req(get(base() + "/resumen"), budget, null, 200), "totalSolicitado", "300");
        amount(req(get(base() + "/por-servicio"), budget, null, 200).get(0), "solicitado", "60");
        amount(req(get(base() + "/por-servicio"), budget, null, 200).get(1), "solicitado", "40");
        amount(req(get(base() + "/maestro"), budget, null, 200), "totalSolicitado", "300");
    }

    @Test void formularioCompactoSinUnidadDistribuyeYRecortaSinDuplicar() throws Exception {
        participant(area, true);
        Map<String,Object> desks = need("Escritorios", 10, 800000, List.of(), false);
        desks.remove("unidadMedida"); desks.remove("justificacion");
        // Aunque un cliente mande un total, el servidor siempre calcula cantidad por valor unitario.
        desks.put("valorTotalSolicitado", 1);
        JsonNode created = req(post("/api/necesidades"), owner, desks, 201);
        long deskId = created.path("id").asLong();
        assertThat(created.path("unidadMedida").asText()).isEmpty();
        assertThat(created.path("justificacion").asText()).isEmpty();
        assertThat(created.path("distribuciones").isEmpty()).isTrue();
        amount(created, "valorTotalSolicitado", "8000000");

        desks.put("aplicaDistribucion", true); desks.put("distribuciones", shares(40,40,0));
        req(put("/api/necesidades/" + deskId), owner, desks, 409);
        desks.put("distribuciones", shares(60,40,0));
        JsonNode distributed = req(put("/api/necesidades/" + deskId), owner, desks, 200);
        amount(distributed.path("distribuciones").get(0), "valorSolicitadoDistribuido", "4800000");
        amount(distributed.path("distribuciones").get(1), "valorSolicitadoDistribuido", "3200000");
        // Desmarcar todos los servicios retira la distribución sin cambiar el total.
        desks.put("aplicaDistribucion", false); desks.put("distribuciones", List.of());
        assertThat(req(put("/api/necesidades/" + deskId), owner, desks, 200).path("distribuciones").isEmpty()).isTrue();
        desks.put("aplicaDistribucion", true); desks.put("distribuciones", shares(60,40,0));
        req(put("/api/necesidades/" + deskId), owner, desks, 200);

        long chairId = create("Sillas", 20, 450000, List.of(), false);
        Map<String,Object> chairs = need("Sillas", 20, 450000, List.of(), false);
        chairs.remove("unidadMedida"); chairs.remove("justificacion");
        JsonNode legacy = req(put("/api/necesidades/" + chairId), owner, chairs, 200);
        assertThat(legacy.path("unidadMedida").asText()).isEqualTo("unidad");
        assertThat(legacy.path("justificacion").asText()).isEqualTo("Necesidad anual");
        Map<String,Object> cabinets = need("Archivadores", 5, 900000, shares(0,0,100), true);
        cabinets.remove("unidadMedida"); cabinets.remove("justificacion");
        JsonNode single = req(post("/api/necesidades"), owner, cabinets, 201);
        amount(single.path("distribuciones").get(0), "porcentaje", "100");
        amount(single.path("distribuciones").get(0), "valorSolicitadoDistribuido", "4500000");
        amount(req(get(base() + "/resumen"), budget, null, 200), "totalSolicitado", "21500000");
        amount(req(get(base() + "/resumen"), budget, null, 200), "totalAprobado", "0");

        req(post("/api/formulacion/vigencias/" + vigencia + "/enviar"), owner, null, 200);
        String snapshot = envios.findByVigenciaIdAndAreaId(vigencia, area).orElseThrow().getDatosEnviados();
        JsonNode cut = req(put("/api/necesidades/" + deskId + "/aprobado"), budget,
            Map.of("cantidadAprobada", 8, "valorUnitarioAprobado", 800000, "observacionPresupuesto", "Recorte de dos escritorios"), 200);
        amount(cut, "valorTotalSolicitadoOriginal", "8000000");
        amount(cut, "valorTotalSolicitado", "8000000"); amount(cut, "valorTotalAprobado", "6400000");
        amount(cut.path("distribuciones").get(0), "valorAprobadoDistribuido", "3840000");
        amount(cut.path("distribuciones").get(1), "valorAprobadoDistribuido", "2560000");
        JsonNode form = req(get("/api/formulacion/vigencias/" + vigencia + "?areaId=" + area), budget, null, 200);
        amount(form, "solicitado", "21500000"); amount(form, "aprobado", "19900000");
        JsonNode summary = req(get(base() + "/resumen"), budget, null, 200);
        amount(summary, "totalSolicitado", "21500000"); amount(summary, "totalAprobado", "19900000");
        JsonNode byService = req(get(base() + "/por-servicio"), budget, null, 200);
        amount(byService.get(0), "aprobado", "3840000");
        amount(byService.get(1), "aprobado", "2560000");
        amount(byService.get(2), "aprobado", "4500000");
        for (JsonNode block : form.path("rubros")) if (block.path("rubroId").asLong() == category) {
            assertThat(block.path("cantidadNecesidades").asInt()).isEqualTo(3);
            amount(block, "solicitado", "21500000"); amount(block, "aprobado", "19900000");
        }
        assertThat(envios.findByVigenciaIdAndAreaId(vigencia, area).orElseThrow().getDatosEnviados()).isEqualTo(snapshot);
        req(put("/api/necesidades/" + deskId), owner, desks, 409);
    }

    @Test void plantillaAnteriorConItemsSeConsultaSoloEnSuDependencia() throws Exception {
        participant(area, true);
        category = rubro("FORM.XLSX.ANTERIOR", "Categoría anterior", null, true, true);
        create("Necesidad existente", 2, 100, List.of(), false);
        JsonNode own = req(get("/api/formulacion/vigencias/" + vigencia), owner, null, 200);
        JsonNode historical = null;
        for (JsonNode block : own.path("rubros")) if (block.path("rubroId").asLong() == category) historical = block;
        assertThat(historical).isNotNull(); assertThat(historical.path("habilitado").asBoolean()).isFalse();
        amount(historical, "solicitado", "200");
        long otherArea = req(post("/api/areas"), admin, Map.of("codigo", "OTRA", "nombre", "Otra dependencia"), 201).path("id").asLong();
        JsonNode other = req(get("/api/formulacion/vigencias/" + vigencia + "?areaId=" + otherArea), admin, null, 200);
        for (JsonNode block : other.path("rubros")) assertThat(block.path("rubroId").asLong()).isNotEqualTo(category);
        amount(other, "solicitado", "0");
        amount(req(get(base() + "/resumen"), budget, null, 200), "totalSolicitado", "200");
    }

    @Test void presupuesto2028SeEnviaComoFormularioYConservaSolicitadoAlAprobar() throws Exception {
        long otraArea = 0;
        for (JsonNode a : req(get("/api/areas"), admin, null, 200))
            if (a.path("codigo").asText().equals("FIN")) otraArea = a.path("id").asLong();
        assertThat(otraArea).isPositive();
        assertThat(req(get("/api/areas"), owner, null, 200).size()).isEqualTo(1);
        req(get("/api/areas/" + otraArea), owner, null, 403);
        req(post("/api/usuarios"), admin, Map.of("username", "financiera_anual", "password", PASSWORD,
            "nombre", "Responsable Financiera", "rol", "RESPONSABLE_AREA", "areaId", otraArea), 201);
        String financiera = login("financiera_anual");
        JsonNode created = req(post("/api/vigencias/presupuestos"), admin, Map.of("anio", 2028), 201);
        vigencia = created.path("id").asLong();
        assertThat(created.path("estado").asText()).isEqualTo("BORRADOR");
        JsonNode concepts = req(get("/api/rubros/conceptos?vigenciaId=" + vigencia), admin, null, 200);
        assertThat(concepts.size()).isEqualTo(65);
        for (JsonNode c : concepts) if (c.path("codigo").asText().equals("FORM.CONCEPTO.007")) category = c.path("id").asLong();
        assertThat(category).isPositive();
        req(post("/api/rubros/conceptos"), admin, Map.of("nombre", "EQUIPOS ESPECIALIZADOS", "orden", 66,
            "vigenciaId", vigencia), 201);
        req(post("/api/vigencias/" + vigencia + "/enviar-dependencias"), owner, null, 403);
        req(post("/api/vigencias/" + vigencia + "/enviar-dependencias"), admin, null, 200);
        assertThat(req(get("/api/formulacion/vigencias/" + vigencia), owner, null, 200).path("estado").asText())
            .isEqualTo("PENDIENTE");
        req(get("/api/formulacion/vigencias/" + vigencia + "?areaId=" + otraArea), owner, null, 403);

        long pipe = create("Tubería PVC", 100, 50000, shares(60, 40, 0), true);
        long cement = create("Cemento", 50, 40000, List.of(), false);
        JsonNode draft = req(get("/api/formulacion/vigencias/" + vigencia), owner, null, 200);
        assertThat(draft.path("estado").asText()).isEqualTo("BORRADOR");
        amount(draft, "solicitado", "7000000");
        amount(req(get(base() + "/resumen"), admin, null, 200), "totalAprobado", "0");
        req(post("/api/formulacion/vigencias/" + vigencia + "/enviar"), owner, null, 200);
        String snapshot = envios.findByVigenciaIdAndAreaId(vigencia, area).orElseThrow().getDatosEnviados();
        assertThat(req(get("/api/formulacion/vigencias/" + vigencia), owner, null, 200).path("estado").asText())
            .isEqualTo("ENVIADO");
        JsonNode cut = req(put("/api/necesidades/" + pipe + "/aprobado"), admin,
            Map.of("cantidadAprobada", 80, "valorUnitarioAprobado", 50000,
                "observacionPresupuesto", "Ajuste", "distribucionesAprobadas", shares(50, 50, 0)), 200);
        amount(cut, "valorTotalSolicitado", "5000000"); amount(cut, "valorTotalAprobado", "4000000");
        amount(cut.path("distribuciones").get(0), "valorSolicitadoDistribuido", "3000000");
        amount(cut.path("distribuciones").get(0), "valorAprobadoDistribuido", "2000000");
        JsonNode approvedCement = req(put("/api/necesidades/" + cement + "/aprobado"), admin,
            Map.of("distribucionesAprobadas", shares(0, 0, 100)), 200);
        amount(approvedCement.path("distribuciones").get(0), "valorSolicitadoDistribuido", "0");
        amount(approvedCement.path("distribuciones").get(0), "valorAprobadoDistribuido", "2000000");
        amount(req(get(base() + "/maestro"), admin, null, 200), "totalSolicitado", "7000000");
        amount(req(get(base() + "/maestro"), admin, null, 200), "totalAprobado", "6000000");
        assertThat(req(get("/api/formulacion/vigencias/" + vigencia), owner, null, 200).path("estado").asText())
            .isEqualTo("EN_REVISION");
        JsonNode general = req(get(base() + "/general"), admin, null, 200);
        JsonNode materials = null;
        for (JsonNode c : general) if (c.path("conceptoId").asLong() == category) materials = c;
        assertThat(materials).isNotNull();
        amount(materials, "solicitado", "7000000"); amount(materials, "aprobado", "6000000");
        assertThat(materials.path("dependencias").size()).isEqualTo(1);
        JsonNode approved = req(post("/api/formulacion/vigencias/" + vigencia + "/aprobar?areaId=" + area), admin, null, 200);
        assertThat(approved.path("estado").asText()).isEqualTo("APROBADO");
        amount(approved, "solicitado", "7000000"); amount(approved, "aprobado", "6000000");
        req(put("/api/necesidades/" + pipe + "/aprobado"), owner,
            Map.of("cantidadAprobada", 70), 403);
        JsonNode revised = req(put("/api/necesidades/" + pipe + "/aprobado"), admin,
            Map.of("cantidadAprobada", 70, "observacionPresupuesto", "Segunda revisión"), 200);
        amount(revised, "valorTotalSolicitado", "5000000");
        amount(revised, "valorTotalAprobado", "3500000");
        assertThat(req(get("/api/formulacion/vigencias/" + vigencia + "?areaId=" + area), admin, null, 200)
            .path("estado").asText()).isEqualTo("EN_REVISION");
        JsonNode revisedGeneral = req(get(base() + "/general"), admin, null, 200);
        for (JsonNode c : revisedGeneral) if (c.path("conceptoId").asLong() == category) {
            amount(c, "solicitado", "7000000"); amount(c, "aprobado", "5500000");
        }
        JsonNode reapproved = req(post("/api/formulacion/vigencias/" + vigencia + "/aprobar?areaId=" + area), admin, null, 200);
        assertThat(reapproved.path("estado").asText()).isEqualTo("APROBADO");
        amount(reapproved, "solicitado", "7000000"); amount(reapproved, "aprobado", "5500000");
        assertThat(req(get("/api/necesidades/" + pipe + "/historial"), admin, null, 200).toString())
            .contains("Segunda revisión");
        assertThat(envios.findByVigenciaIdAndAreaId(vigencia, area).orElseThrow().getDatosEnviados()).isEqualTo(snapshot);
        assertThat(req(get("/api/vigencias"), financiera, null, 200).size()).isEqualTo(1);
    }

    @Test void formularioSinItemsPuedeEnviarseYSeApruebaConTotalCero() throws Exception {
        participant(area, true);
        assertThat(req(post("/api/formulacion/vigencias/" + vigencia + "/borrador"), owner, null, 200)
            .path("estado").asText()).isEqualTo("BORRADOR");
        JsonNode sent = req(post("/api/formulacion/vigencias/" + vigencia + "/enviar"), owner, null, 200);
        assertThat(sent.path("estado").asText()).isEqualTo("ENVIADO");
        amount(sent, "solicitado", "0");
        JsonNode approved = req(post("/api/formulacion/vigencias/" + vigencia + "/aprobar?areaId=" + area), admin, null, 200);
        assertThat(approved.path("estado").asText()).isEqualTo("APROBADO");
    }

    private String base() { return "/api/presupuesto/vigencias/" + vigencia; }
    private void participant(long areaId, boolean enabled) throws Exception { req(put("/api/vigencias/" + vigencia + "/participantes/" + areaId), admin, Map.of("activo", enabled), 200); }
    private long create(String name, int quantity, int unit, List<Map<String, Object>> distribution, boolean applies) throws Exception {
        return req(post("/api/necesidades"), owner, need(name, quantity, unit, distribution, applies), 201).path("id").asLong();
    }
    private Map<String, Object> need(String name, int quantity, int unit, List<Map<String, Object>> distribution, boolean applies) {
        return new HashMap<>(Map.of("vigenciaId", vigencia, "rubroPresupuestalId", category, "descripcion", name,
            "justificacion", "Necesidad anual", "unidadMedida", "unidad", "cantidadSolicitada", quantity,
            "valorUnitarioSolicitado", unit, "distribuciones", distribution, "aplicaDistribucion", applies));
    }
    private Map<String, Object> newNeed(long areaId) { var body = need("Jurídica", 1, 10, List.of(), false); body.put("areaId", areaId); return body; }
    private List<Map<String, Object>> shares(int a, int b, int c) {
        List<Map<String, Object>> result = new ArrayList<>();
        int[] percentages = {a,b,c}; for (int i=0; i<3; i++) if (percentages[i] > 0) result.add(Map.of("servicioId", i+1, "porcentaje", percentages[i]));
        return result;
    }
    private long rubro(String code, String name, Long parent, boolean movement, boolean formulation) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("codigo", code, "nombre", name, "vigenciaId", vigencia,
            "permiteMovimiento", movement, "categoriaFormulacion", formulation, "orden", 0));
        if (parent != null) body.put("rubroPadreId", parent);
        return req(post("/api/rubros"), admin, body, 201).path("id").asLong();
    }
    private Map<String, Object> year(int year) { return Map.of("anio", year, "nombre", "Presupuesto " + year,
        "fechaInicio", year + "-01-01", "fechaFin", year + "-12-31", "presupuestoDisponible", 60000000, "estado", "ABIERTA"); }
    private String login(String username) throws Exception { return req(post("/api/auth/login"), null, Map.of("username", username, "password", PASSWORD), 200).path("token").asText(); }
    private JsonNode req(MockHttpServletRequestBuilder request, String token, Object body, int expected) throws Exception {
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var response = mvc.perform(request).andReturn().getResponse();
        assertThat(response.getStatus()).withFailMessage(response.getContentAsString()).isEqualTo(expected);
        return response.getContentAsString().isEmpty() ? json.nullNode() : json.readTree(response.getContentAsString());
    }
    private void amount(JsonNode node, String field, String expected) { assertThat(node.path(field).decimalValue()).isEqualByComparingTo(expected); }
    private JsonNode find(JsonNode nodes, long id) { for (JsonNode node : nodes) if (node.path("rubroId").asLong() == id) return node; throw new AssertionError("Rubro no encontrado: " + id); }
}
