package com.serviciudad.presupuesto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviciudad.presupuesto.enums.NombreRol;
import com.serviciudad.presupuesto.repository.UsuarioRepository;
import java.util.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:presupuesto_fase2;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.username=sa", "spring.datasource.password=", "app.admin.username=admin_fase2"
})
class NecesidadIntegrationTest {
    private static final String ADMIN_PASSWORD = UUID.randomUUID().toString() + "A1";
    private static final String USER_PASSWORD = UUID.randomUUID().toString() + "B2";
    @DynamicPropertySource
    static void secrets(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> UUID.randomUUID().toString() + UUID.randomUUID());
        registry.add("app.admin.password", () -> ADMIN_PASSWORD);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;

    @Test
    void flujoCompletoYConsolidados() throws Exception {
        String admin = login("admin_fase2", ADMIN_PASSWORD);
        long area1 = request(post("/api/areas"), admin, Map.of("codigo", "OP", "nombre", "Operativa"), 201).get("id").asLong();
        long area2 = request(post("/api/areas"), admin, Map.of("codigo", "AD", "nombre", "Administrativa"), 201).get("id").asLong();
        long vigencia = request(post("/api/vigencias"), admin, Map.of(
            "anio", 2027, "nombre", "Presupuesto 2027", "fechaInicio", "2027-01-01", "fechaFin", "2027-12-31",
            "presupuestoDisponible", 60, "estado", "ABIERTA"), 201).get("id").asLong();
        request(put("/api/vigencias/" + vigencia + "/participantes/" + area1), admin, Map.of("activo", true), 200);
        request(put("/api/vigencias/" + vigencia + "/participantes/" + area2), admin, Map.of("activo", true), 200);
        long root = request(post("/api/rubros"), admin, Map.of("codigo", "2", "nombre", "GASTOS", "orden", 0), 201).get("id").asLong();
        long child = request(post("/api/rubros"), admin, Map.of("codigo", "2.1", "nombre", "HARDWARE",
            "rubroPadreId", root, "permiteMovimiento", true, "orden", 1), 201).get("id").asLong();

        request(post("/api/usuarios"), admin, Map.of("username", "sin_area", "password", USER_PASSWORD,
            "nombre", "Sin área", "rol", "RESPONSABLE_AREA"), 409);
        createUser(admin, "area_operativa", NombreRol.RESPONSABLE_AREA, area1);
        createUser(admin, "area_administrativa", NombreRol.RESPONSABLE_AREA, area2);
        createUser(admin, "administrador_fase2", NombreRol.SUPER_ADMIN, null);
        createUser(admin, "consulta_fase2", NombreRol.CONSULTA, null);
        assertThat(usuarios.findByUsername("area_operativa").orElseThrow().getPasswordHash()).isNotEqualTo(USER_PASSWORD);
        String areaToken = login("area_operativa", USER_PASSWORD);
        String otherAreaToken = login("area_administrativa", USER_PASSWORD);
        String budgetToken = login("administrador_fase2", USER_PASSWORD);
        String readerToken = login("consulta_fase2", USER_PASSWORD);
        JsonNode profile = request(get("/api/auth/me"), areaToken, null, 200);
        assertThat(profile.get("areaId").asLong()).isEqualTo(area1);
        assertThat(profile.get("rol").asText()).isEqualTo("RESPONSABLE_AREA");

        long acueducto = 1L, alcantarillado = 2L, aseo = 3L;
        List<Map<String, Object>> shares = List.of(
            Map.of("servicioId", acueducto, "porcentaje", 33),
            Map.of("servicioId", alcantarillado, "porcentaje", 33),
            Map.of("servicioId", aseo, "porcentaje", 34));
        Map<String, Object> body = need(vigencia, area1, child, 100, shares);
        request(post("/api/necesidades"), areaToken, need(vigencia, area2, child, 100, shares), 403);
        request(post("/api/necesidades"), areaToken, need(vigencia, area1, child, 100,
            List.of(Map.of("servicioId", acueducto, "porcentaje", 50),
                Map.of("servicioId", alcantarillado, "porcentaje", 20))), 409);

        JsonNode created = request(post("/api/necesidades"), areaToken, body, 201);
        long needId = created.get("id").asLong();
        assertThat(created.get("valorTotalSolicitado").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(created.get("estado").asText()).isEqualTo("BORRADOR");
        JsonNode draftArea = request(get("/api/presupuesto/vigencias/" + vigencia + "/mi-area"), areaToken, null, 200);
        assertThat(draftArea.get("numeroNecesidades").asLong()).isEqualTo(1);
        assertThat(draftArea.get("porEstado").get("BORRADOR").asLong()).isEqualTo(1);
        assertThat(draftArea.get("totalSolicitado").decimalValue()).isEqualByComparingTo("100.00");
        request(get("/api/presupuesto/vigencias/" + vigencia + "/mi-area"), budgetToken, null, 403);
        request(get("/api/necesidades/" + needId), otherAreaToken, null, 403);
        JsonNode ownList = request(get("/api/necesidades?areaId=" + area2 + "&servicioId=" + acueducto), areaToken, null, 200);
        assertThat(ownList.get("totalElements").asInt()).isEqualTo(1);
        request(get("/api/presupuesto/vigencias/" + vigencia + "/resumen"), areaToken, null, 403);

        JsonNode sent = request(post("/api/necesidades/" + needId + "/enviar"), areaToken, null, 200);
        assertThat(sent.get("estado").asText()).isEqualTo("ENVIADA");
        request(put("/api/necesidades/" + needId), areaToken, body, 409);
        request(patch("/api/necesidades/" + needId + "/ajuste"), areaToken,
            Map.of("cantidadAprobada", 70), 403);
        request(post("/api/necesidades/" + needId + "/iniciar-revision"), budgetToken, null, 200);
        JsonNode adjusted = request(patch("/api/necesidades/" + needId + "/ajuste"), budgetToken,
            Map.of("cantidadAprobada", 70, "observacionPresupuesto", "Reducción por techo"), 200);
        assertThat(adjusted.get("valorTotalSolicitado").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(adjusted.get("valorTotalAprobado").decimalValue()).isEqualByComparingTo("70.00");
        assertShare(adjusted, acueducto, "33.00", "23.10");
        assertShare(adjusted, alcantarillado, "33.00", "23.10");
        assertShare(adjusted, aseo, "34.00", "23.80");
        JsonNode approved = request(post("/api/necesidades/" + needId + "/aprobar"), budgetToken, null, 200);
        assertThat(approved.get("estado").asText()).isEqualTo("APROBADA_CON_AJUSTES");
        assertThat(approved.get("cantidadSolicitada").decimalValue()).isEqualByComparingTo("100.0000");
        assertThat(approved.get("cantidadAprobada").decimalValue()).isEqualByComparingTo("70.0000");
        JsonNode approvedArea = request(get("/api/presupuesto/vigencias/" + vigencia + "/mi-area"), areaToken, null, 200);
        assertThat(approvedArea.get("totalSolicitado").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(approvedArea.get("totalAprobado").decimalValue()).isEqualByComparingTo("70.00");
        assertThat(approvedArea.get("porEstado").get("APROBADA_CON_AJUSTES").asLong()).isEqualTo(1);

        JsonNode summary = request(get("/api/presupuesto/vigencias/" + vigencia + "/resumen"), budgetToken, null, 200);
        assertThat(summary.get("totalSolicitado").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(summary.get("totalAprobado").decimalValue()).isEqualByComparingTo("70.00");
        assertThat(summary.get("diferenciaContraTecho").decimalValue()).isEqualByComparingTo("10.00");
        JsonNode byArea = request(get("/api/presupuesto/vigencias/" + vigencia + "/por-area"), budgetToken, null, 200);
        JsonNode ownRow = null;
        for (JsonNode row : byArea) if (row.path("areaId").asLong() == area1) ownRow = row;
        assertThat(ownRow).isNotNull();
        assertThat(ownRow.get("aprobado").decimalValue()).isEqualByComparingTo("70.00");
        JsonNode byService = request(get("/api/presupuesto/vigencias/" + vigencia + "/por-servicio"), budgetToken, null, 200);
        assertThat(byService.size()).isEqualTo(3);
        assertConsolidadoServicio(byService, acueducto, "33.00", "23.10");
        assertConsolidadoServicio(byService, alcantarillado, "33.00", "23.10");
        assertConsolidadoServicio(byService, aseo, "34.00", "23.80");
        JsonNode byRubro = request(get("/api/presupuesto/vigencias/" + vigencia + "/por-rubro"), budgetToken, null, 200);
        assertThat(byRubro.size()).isEqualTo(2);
        assertThat(byRubro.get(0).get("rubroId").asLong()).isEqualTo(root);
        assertThat(byRubro.get(0).get("aprobado").decimalValue()).isEqualByComparingTo("70.00");
        assertThat(byRubro.get(1).get("rubroId").asLong()).isEqualTo(child);
        assertThat(byRubro.get(1).get("aprobado").decimalValue()).isEqualByComparingTo("70.00");
        JsonNode history = request(get("/api/necesidades/" + needId + "/historial"), areaToken, null, 200);
        assertThat(history.findValuesAsText("accion"))
            .containsExactly("CREACION", "ENVIO", "INICIO_REVISION", "AJUSTE", "APROBACION");
        assertThat(history.get(3).get("detalleAnterior").asText()).contains("100.00");
        request(get("/api/presupuesto/vigencias/" + vigencia + "/resumen"), readerToken, null, 200);
        request(post("/api/necesidades"), readerToken, body, 403);
        request(post("/api/usuarios"), readerToken, Map.of("username", "otro", "password", USER_PASSWORD,
            "nombre", "Otro", "rol", "CONSULTA"), 403);
        JsonNode filtered = request(get("/api/necesidades?estado=APROBADA_CON_AJUSTES&texto=computadores"), budgetToken, null, 200);
        assertThat(filtered.get("totalElements").asInt()).isEqualTo(1);

        request(post("/api/necesidades"), areaToken, need(vigencia, area1, child, 20,
            List.of(Map.of("servicioId", acueducto, "porcentaje", 100),
                Map.of("servicioId", acueducto, "porcentaje", 0))), 409);
        Map<String, Object> singleService = need(vigencia, area1, child, 20,
            List.of(Map.of("servicioId", acueducto, "porcentaje", 100)));
        long returnedId = request(post("/api/necesidades"), areaToken, singleService, 201).get("id").asLong();
        request(post("/api/necesidades/" + returnedId + "/enviar"), areaToken, null, 200);
        JsonNode returned = request(post("/api/necesidades/" + returnedId + "/devolver"), budgetToken,
            Map.of("observacionPresupuesto", "Completar justificación"), 200);
        assertThat(returned.get("estado").asText()).isEqualTo("DEVUELTA");
        JsonNode corrected = request(put("/api/necesidades/" + returnedId), areaToken,
            need(vigencia, area1, child, 25, List.of(Map.of("servicioId", acueducto, "porcentaje", 100))), 200);
        assertThat(corrected.get("valorTotalSolicitado").decimalValue()).isEqualByComparingTo("25.00");
        assertThat(corrected.get("valorTotalSolicitadoOriginal").decimalValue()).isEqualByComparingTo("20.00");
        request(post("/api/necesidades/" + returnedId + "/enviar"), areaToken, null, 200);
        request(post("/api/necesidades/" + returnedId + "/iniciar-revision"), budgetToken, null, 200);
        JsonNode rejected = request(post("/api/necesidades/" + returnedId + "/rechazar"), budgetToken,
            Map.of("observacionPresupuesto", "Sin disponibilidad"), 200);
        assertThat(rejected.get("estado").asText()).isEqualTo("RECHAZADA");
        assertThat(rejected.get("valorTotalSolicitado").decimalValue()).isEqualByComparingTo("25.00");
        assertThat(rejected.get("valorTotalSolicitadoOriginal").decimalValue()).isEqualByComparingTo("20.00");
        assertThat(rejected.get("valorTotalAprobado").decimalValue()).isEqualByComparingTo("0.00");
        JsonNode returnHistory = request(get("/api/necesidades/" + returnedId + "/historial"), areaToken, null, 200);
        assertThat(returnHistory.findValuesAsText("accion")).containsExactly("CREACION", "ENVIO", "DEVOLUCION",
            "MODIFICACION", "ENVIO", "INICIO_REVISION", "RECHAZO");
        assertThat(returnHistory.get(3).get("detalleAnterior").asText()).contains("20.00");

        long unchangedId = request(post("/api/necesidades"), areaToken,
            need(vigencia, area1, child, 5, List.of(Map.of("servicioId", aseo, "porcentaje", 100))), 201)
            .get("id").asLong();
        request(post("/api/necesidades/" + unchangedId + "/enviar"), areaToken, null, 200);
        request(post("/api/necesidades/" + unchangedId + "/iniciar-revision"), budgetToken, null, 200);
        JsonNode unchanged = request(post("/api/necesidades/" + unchangedId + "/aprobar"), budgetToken, null, 200);
        assertThat(unchanged.get("estado").asText()).isEqualTo("APROBADA");
        assertThat(unchanged.get("valorTotalAprobado").decimalValue()).isEqualByComparingTo("5.00");

        long centId = request(post("/api/necesidades"), areaToken, Map.of(
            "vigenciaId", vigencia, "areaId", area1, "rubroPresupuestalId", child,
            "descripcion", "Centavo de prueba", "justificacion", "Verificación de redondeo",
            "unidadMedida", "unidad", "cantidadSolicitada", 1, "valorUnitarioSolicitado", 0.01,
            "distribuciones", shares), 201).get("id").asLong();
        request(post("/api/necesidades/" + centId + "/enviar"), areaToken, null, 200);
        request(post("/api/necesidades/" + centId + "/iniciar-revision"), budgetToken, null, 200);
        JsonNode cent = request(post("/api/necesidades/" + centId + "/aprobar"), budgetToken, null, 200);
        assertShare(cent, acueducto, "0.00", "0.00");
        assertShare(cent, alcantarillado, "0.00", "0.00");
        assertShare(cent, aseo, "0.01", "0.01");

        long increasedId = request(post("/api/necesidades"), areaToken,
            need(vigencia, area1, child, 5, List.of(Map.of("servicioId", alcantarillado, "porcentaje", 100))), 201)
            .get("id").asLong();
        request(post("/api/necesidades/" + increasedId + "/enviar"), areaToken, null, 200);
        request(post("/api/necesidades/" + increasedId + "/iniciar-revision"), budgetToken, null, 200);
        JsonNode increased = request(patch("/api/necesidades/" + increasedId + "/ajuste"), budgetToken,
            Map.of("valorUnitarioAprobado", 1.20, "observacionPresupuesto", "Actualización del precio cotizado"), 200);
        assertThat(increased.get("valorTotalSolicitado").decimalValue()).isEqualByComparingTo("5.00");
        assertThat(increased.get("valorTotalAprobado").decimalValue()).isEqualByComparingTo("6.00");
        request(post("/api/necesidades/" + increasedId + "/aprobar"), budgetToken, null, 200);

        JsonNode finalSummary = request(get("/api/presupuesto/vigencias/" + vigencia + "/resumen"), budgetToken, null, 200);
        assertThat(finalSummary.get("totalSolicitado").decimalValue()).isEqualByComparingTo("135.01");
        assertThat(finalSummary.get("totalAprobado").decimalValue()).isEqualByComparingTo("81.01");
        JsonNode finalRubros = request(get("/api/presupuesto/vigencias/" + vigencia + "/por-rubro"), budgetToken, null, 200);
        assertThat(finalRubros.get(0).get("aprobado").decimalValue()).isEqualByComparingTo("81.01");
        assertThat(finalRubros.get(1).get("aprobado").decimalValue()).isEqualByComparingTo("81.01");
        request(put("/api/vigencias/" + vigencia), admin, Map.of(
            "anio", 2027, "nombre", "Presupuesto 2027", "fechaInicio", "2027-01-01", "fechaFin", "2027-12-31",
            "presupuestoDisponible", 60, "estado", "APROBADA"), 200);
        request(post("/api/necesidades"), areaToken, body, 409);
    }

    private Map<String, Object> need(long vigencia, long area, long rubro, int quantity,
        List<Map<String, Object>> shares) {
        return Map.of("vigenciaId", vigencia, "areaId", area, "rubroPresupuestalId", rubro,
            "descripcion", "Compra de computadores", "justificacion", "Renovación tecnológica",
            "unidadMedida", "unidad", "cantidadSolicitada", quantity, "valorUnitarioSolicitado", 1,
            "distribuciones", shares);
    }

    private void createUser(String admin, String username, NombreRol role, Long areaId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username); body.put("nombre", username); body.put("password", USER_PASSWORD);
        body.put("rol", role.name());
        if (areaId != null) body.put("areaId", areaId);
        JsonNode created = request(post("/api/usuarios"), admin, body, 201);
        assertThat(created.has("password")).isFalse();
        assertThat(created.has("passwordHash")).isFalse();
    }

    private String login(String username, String password) throws Exception {
        return request(post("/api/auth/login"), null, Map.of("username", username, "password", password), 200)
            .get("token").asText();
    }

    private JsonNode request(MockHttpServletRequestBuilder builder, String token, Object body, int status) throws Exception {
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (body != null) builder.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        String content = mvc.perform(builder).andExpect(result -> assertThat(result.getResponse().getStatus()).isEqualTo(status))
            .andReturn().getResponse().getContentAsString();
        return content.isBlank() ? com.fasterxml.jackson.databind.node.NullNode.getInstance() : json.readTree(content);
    }

    private void assertShare(JsonNode response, long serviceId, String requested, String approved) {
        for (JsonNode share : response.get("distribuciones")) {
            if (share.get("servicioId").asLong() == serviceId) {
                assertThat(share.get("valorSolicitadoDistribuido").decimalValue()).isEqualByComparingTo(requested);
                assertThat(share.get("valorAprobadoDistribuido").decimalValue()).isEqualByComparingTo(approved);
                return;
            }
        }
        throw new AssertionError("Servicio no encontrado en la distribución: " + serviceId);
    }

    private void assertConsolidadoServicio(JsonNode rows, long serviceId, String requested, String approved) {
        for (JsonNode row : rows) {
            if (row.get("servicioId").asLong() == serviceId) {
                assertThat(row.get("solicitado").decimalValue()).isEqualByComparingTo(requested);
                assertThat(row.get("aprobado").decimalValue()).isEqualByComparingTo(approved);
                return;
            }
        }
        throw new AssertionError("Servicio no encontrado en el consolidado: " + serviceId);
    }
}
