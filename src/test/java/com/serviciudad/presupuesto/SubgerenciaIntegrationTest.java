package com.serviciudad.presupuesto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest @AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:subgerencias;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.username=sa", "spring.datasource.password=", "app.admin.username=admin_subgerencias"
})
class SubgerenciaIntegrationTest {
    private static final String PASSWORD = UUID.randomUUID() + "A1";
    @DynamicPropertySource static void secrets(DynamicPropertyRegistry r) {
        r.add("app.jwt.secret", () -> UUID.randomUUID().toString() + UUID.randomUUID());
        r.add("app.admin.password", () -> PASSWORD);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test void creaSubgerenciaCredencialesYParticipacionAtomicamente() throws Exception {
        String admin = login("admin_subgerencias");
        long year = req(post("/api/vigencias"), admin, Map.of("anio", 2027, "nombre", "Presupuesto 2027",
            "fechaInicio", "2027-01-01", "fechaFin", "2027-12-31", "presupuestoDisponible", 1000, "estado", "ABIERTA"), 201).path("id").asLong();
        JsonNode created = req(post("/api/areas/con-responsable"), admin, body("OP", "operativa", year), 201);
        long areaId = created.path("area").path("id").asLong();
        assertThat(created.path("responsable").path("areaId").asLong()).isEqualTo(areaId);
        assertThat(created.path("responsable").path("rol").asText()).isEqualTo("RESPONSABLE_AREA");
        assertThat(created.toString()).doesNotContain(PASSWORD, "passwordHash");
        String owner = login("operativa");
        assertThat(req(get("/api/auth/me"), owner, null, 200).path("areaId").asLong()).isEqualTo(areaId);
        assertThat(req(get("/api/formulacion/vigencias/" + year), owner, null, 200).path("puedeCrear").asBoolean()).isTrue();
        long rubro = req(post("/api/rubros"), admin, Map.of("codigo", "FORM", "nombre", "Categoría", "vigenciaId", year,
            "permiteMovimiento", true, "categoriaFormulacion", true), 201).path("id").asLong();
        long first = req(post("/api/necesidades"), owner, Map.of("vigenciaId", year, "rubroPresupuestalId", rubro, "descripcion", "Solicitud",
            "justificacion", "Presupuesto anual", "unidadMedida", "unidad", "cantidadSolicitada", 1, "valorUnitarioSolicitado", 100,
            "aplicaDistribucion", false), 201).path("id").asLong();
        Map<String, Object> secondBody = new HashMap<>(Map.of("vigenciaId", year, "rubroPresupuestalId", rubro, "descripcion", "Segundo ítem",
            "justificacion", "Presupuesto anual", "unidadMedida", "unidad", "cantidadSolicitada", 1, "valorUnitarioSolicitado", 0,
            "aplicaDistribucion", false));
        long second = req(post("/api/necesidades"), owner, secondBody, 201).path("id").asLong();
        req(post("/api/formulacion/vigencias/" + year + "/enviar"), owner, null, 409);
        assertThat(req(get("/api/formulacion/vigencias/" + year), owner, null, 200).path("enviada").asBoolean()).isFalse();
        assertThat(req(get("/api/necesidades/" + first), owner, null, 200).path("estado").asText()).isEqualTo("BORRADOR");
        assertThat(req(get("/api/necesidades/" + first + "/historial"), owner, null, 200).size()).isEqualTo(1);
        secondBody.put("valorUnitarioSolicitado", 200);
        req(put("/api/necesidades/" + second), owner, secondBody, 200);
        req(post("/api/formulacion/vigencias/" + year + "/enviar?areaId=99999"), owner, null, 403);
        JsonNode sent = req(post("/api/formulacion/vigencias/" + year + "/enviar"), owner, null, 200);
        for (JsonNode item : sent.path("rubros").get(0).path("necesidades")) assertThat(item.path("estado").asText()).isEqualTo("ENVIADA");
        req(post("/api/formulacion/vigencias/" + year + "/enviar"), owner, null, 409);
        req(post("/api/areas/con-responsable"), owner, body("JU", "juridica", year), 403);
        req(post("/api/areas/con-responsable"), admin, body("JU", "operativa", year), 409);
        assertThat(req(get("/api/areas"), admin, null, 200).size()).isEqualTo(9);
        req(post("/api/areas/con-responsable"), admin, body("JU", "juridica", 99999L), 404);
        assertThat(req(get("/api/areas"), admin, null, 200).size()).isEqualTo(9);
        assertThat(req(get("/api/usuarios"), admin, null, 200).size()).isEqualTo(2);
        var invalid = body("JU", "juridica", year);
        invalid.put("responsable", Map.of("nombre", "Responsable", "username", "juridica", "password", "corta"));
        req(post("/api/areas/con-responsable"), admin, invalid, 400);
        assertThat(req(get("/api/areas"), admin, null, 200).size()).isEqualTo(9);
        JsonNode withoutYear = req(post("/api/areas/con-responsable"), admin, body("JU", "juridica", null), 201);
        assertThat(withoutYear.path("vigenciaId").isNull()).isTrue();
        assertThat(req(get("/api/formulacion/vigencias/" + year), login("juridica"), null, 200).path("puedeCrear").asBoolean()).isFalse();
    }
    private Map<String, Object> body(String code, String username, Long year) {
        Map<String, Object> body = new HashMap<>();
        body.put("area", Map.of("codigo", code, "nombre", "Subgerencia " + code));
        body.put("responsable", Map.of("nombre", "Responsable " + code, "username", username, "password", PASSWORD));
        body.put("vigenciaId", year); return body;
    }
    private String login(String username) throws Exception {
        return req(post("/api/auth/login"), null, Map.of("username", username, "password", PASSWORD), 200).path("token").asText();
    }
    private JsonNode req(MockHttpServletRequestBuilder request, String token, Object body, int expected) throws Exception {
        if (token != null) request.header("Authorization", "Bearer " + token);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var response = mvc.perform(request).andReturn().getResponse();
        assertThat(response.getStatus()).withFailMessage(response.getContentAsString()).isEqualTo(expected);
        return response.getContentAsString().isEmpty() ? json.nullNode() : json.readTree(response.getContentAsString());
    }
}
