package com.serviciudad.presupuesto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviciudad.presupuesto.entity.Usuario;
import com.serviciudad.presupuesto.enums.NombreRol;
import com.serviciudad.presupuesto.repository.RolRepository;
import com.serviciudad.presupuesto.repository.ServicioRepository;
import com.serviciudad.presupuesto.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:presupuesto;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "app.admin.username=admin_prueba"
})
class ApiIntegrationTest {
    private static final String ADMIN_PASSWORD = java.util.UUID.randomUUID().toString() + "A1";
    private static final String CONSULTA_PASSWORD = java.util.UUID.randomUUID().toString() + "B2";
    @DynamicPropertySource
    static void secretosDePrueba(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> java.util.UUID.randomUUID().toString() + java.util.UUID.randomUUID());
        registry.add("app.admin.password", () -> ADMIN_PASSWORD);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ServicioRepository servicios;
    @Autowired RolRepository roles;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;

    @Test
    void migracionesAutenticacionRolesYArbol() throws Exception {
        assertThat(servicios.findAll()).hasSize(3);
        assertThat(roles.findAll()).hasSize(3);
        assertThat(usuarios.findByUsername("admin_prueba")).isPresent();
        assertThat(usuarios.findByUsername("admin_prueba").orElseThrow().getPasswordHash()).isNotEqualTo(ADMIN_PASSWORD);

        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/presupuesto-general")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));

        mvc.perform(get("/api/areas")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"admin_prueba\",\"password\":\"incorrecta\"}"))
            .andExpect(status().isUnauthorized());
        String adminToken = login("admin_prueba", ADMIN_PASSWORD);
        mvc.perform(post("/api/usuarios").header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"perfil_anterior\",\"password\":\"" + CONSULTA_PASSWORD + "\",\"nombre\":\"Perfil anterior\",\"rol\":\"PRESUPUESTO\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("admin_prueba"))
            .andExpect(jsonPath("$.rol").value("SUPER_ADMIN"));
        mvc.perform(options("/api/auth/login").header("Origin", "http://localhost:5173")
            .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        mvc.perform(post("/api/areas").header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"ADM\",\"nombre\":\"Administrativa\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber());
        mvc.perform(post("/api/areas").header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"ADM\",\"nombre\":\"Repetida\"}"))
            .andExpect(status().isConflict());

        JsonNode nuevoServicio = mapper.readTree(mvc.perform(post("/api/servicios").header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"24\",\"nombre\":\"Servicio de prueba\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        mvc.perform(patch("/api/servicios/" + nuevoServicio.get("id").asLong() + "/estado")
            .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
            .content("{\"activo\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        mvc.perform(get("/api/servicios/" + nuevoServicio.get("id").asLong())
            .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));

        mvc.perform(post("/api/vigencias").header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"anio\":2027,\"nombre\":\"Presupuesto 2027\",\"fechaInicio\":\"2027-01-01\",\"fechaFin\":\"2027-12-31\",\"presupuestoDisponible\":60000000000.00,\"estado\":\"BORRADOR\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.presupuestoDisponible").value(60000000000.00));
        mvc.perform(post("/api/vigencias").header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"anio\":2028,\"nombre\":\"Inválida\",\"fechaInicio\":\"2028-01-01\",\"fechaFin\":\"2028-12-31\",\"presupuestoDisponible\":-1,\"estado\":\"BORRADOR\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        JsonNode root = mapper.readTree(mvc.perform(post("/api/rubros").header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"2\",\"nombre\":\"Gastos\",\"orden\":0}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long rootId = root.get("id").asLong();
        mvc.perform(post("/api/rubros").header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"2.1\",\"nombre\":\"Funcionamiento\",\"rubroPadreId\":" + rootId + ",\"orden\":0}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.nivel").value(1));
        mvc.perform(get("/api/rubros/arbol").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].hijos[0].codigo").value("2.1"));
        mvc.perform(put("/api/rubros/" + rootId).header("Authorization", "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"2\",\"nombre\":\"Gastos\",\"rubroPadreId\":" + (rootId + 1) + ",\"orden\":0}"))
            .andExpect(status().isConflict());

        Usuario consulta = new Usuario();
        consulta.setUsername("consulta_prueba"); consulta.setNombre("Consulta");
        consulta.setPasswordHash(encoder.encode(CONSULTA_PASSWORD));
        consulta.setRol(roles.findByNombre(NombreRol.CONSULTA).orElseThrow());
        usuarios.saveAndFlush(consulta);
        String consultaToken = login("consulta_prueba", CONSULTA_PASSWORD);
        mvc.perform(get("/api/areas").header("Authorization", "Bearer " + consultaToken)).andExpect(status().isOk());
        mvc.perform(post("/api/servicios").header("Authorization", "Bearer " + consultaToken)
            .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"25\",\"nombre\":\"Nuevo\"}"))
            .andExpect(status().isForbidden());
    }

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(java.util.Map.of("username", username, "password", password))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("token").asText();
    }
}
