package com.k48.leonel.presence48.documentation;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** #96 : Swagger UI, la doc générée et le contrat de référence sont servis sans session. */
@SpringBootTest
@AutoConfigureMockMvc
class SwaggerIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Test
  void testSwaggerUiEstPublic() throws Exception {
    mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
  }

  @Test
  void testDocGenereeContientLesOperationsImposees() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/sessions'].post").exists())
        .andExpect(jsonPath("$.paths['/api/presences'].post").exists())
        .andExpect(jsonPath("$.paths['/api/exercices'].post").exists())
        .andExpect(jsonPath("$.paths['/api/relectures/{id}'].post").exists())
        .andExpect(jsonPath("$.paths['/api/tableau'].get").exists());
  }

  @Test
  void testContratDeReferenceServiTelQuel() throws Exception {
    mvc.perform(get("/contrat.yaml"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("title: Contrat PRESENCE48")));
  }

  @Test
  void testConfigurationSwaggerProposeLeContratEnPremier() throws Exception {
    mvc.perform(get("/v3/api-docs/swagger-config"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.urls[0].url").value("/contrat.yaml"))
        .andExpect(jsonPath("$.urls[1].url").value("/v3/api-docs"));
  }

  /** #105 : tout endpoint déclaré à l'avenir devra avoir son tag et son résumé. */
  @Test
  void testChaqueOperationDocumenteeATagEtResume() throws Exception {
    var racine = lireDocs();
    var nombre = 0;
    for (Iterator<Map.Entry<String, JsonNode>> it = racine.path("paths").fields(); it.hasNext();) {
      var entree = it.next();
      var operations = entree.getValue();
      for (var verbe : List.of("get", "post", "put", "delete")) {
        if (!operations.has(verbe)) {
          continue;
        }
        nombre++;
        var operation = operations.path(verbe);
        var chemin = entree.getKey() + " " + verbe;
        org.assertj.core.api.Assertions.assertThat(operation.path("tags"))
            .as("tag de %s", chemin).isNotEmpty();
        org.assertj.core.api.Assertions.assertThat(operation.path("summary").asText())
            .as("résumé de %s", chemin).isNotBlank();
        org.assertj.core.api.Assertions.assertThat(operation.path("description").asText())
            .as("description de %s", chemin).isNotBlank();
      }
    }
    org.assertj.core.api.Assertions.assertThat(nombre).as("les 12 opérations livrées").isEqualTo(12);
  }

  /** #105 : cookieAuth déclaré, opérations imposées publiques, erreurs métier documentées avec exemples. */
  @Test
  void testSecuriteEtErreursImposeesDocumentees() throws Exception {
    var racine = lireDocs();
    org.assertj.core.api.Assertions.assertThat(racine.path("components").path("securitySchemes")
        .path("cookieAuth").path("type").asText()).isEqualTo("apiKey");
    org.assertj.core.api.Assertions.assertThat(racine.path("components").path("securitySchemes")
        .path("cookieAuth").path("in").asText()).isEqualTo("cookie");
    // Opérations imposées publiques (RG22) : security = []
    for (var imposee : List.of("/api/sessions|post", "/api/presences|post", "/api/exercices|post",
        "/api/relectures/{id}|post", "/api/tableau|get")) {
      var morceaux = imposee.split("\\|");
      var operation = racine.path("paths").path(morceaux[0]).path(morceaux[1]);
      org.assertj.core.api.Assertions.assertThat(operation.path("security").isArray())
          .as("%s %s : security vide", morceaux[1], morceaux[0]).isTrue();
      org.assertj.core.api.Assertions.assertThat(operation.path("security").size())
          .as("%s %s : aucune exigence", morceaux[1], morceaux[0]).isZero();
    }
    // Les autres routes exigent la session (cookieAuth).
    var listeSessions = racine.path("paths").path("/api/sessions").path("get").path("security");
    org.assertj.core.api.Assertions.assertThat(listeSessions.size()).as("routes protégées").isEqualTo(1);
    // Codes métier réels documentés sur POST /api/presences.
    var reponses = racine.path("paths").path("/api/presences").path("post").path("responses");
    for (var code : List.of("400", "409", "410", "429")) {
      org.assertj.core.api.Assertions.assertThat(reponses.has(code)).as("réponse %s", code).isTrue();
    }
    org.assertj.core.api.Assertions.assertThat(reponses.path("410").path("content").path("application/json")
        .path("schema").path("$ref").asText()).contains("Erreur");
  }

  /** #105 : Swagger UI envoie le jeton CSRF sur les écritures. */
  @Test
  void testSwaggerUiEnvoieLeJetonCsrf() throws Exception {
    mvc.perform(get("/v3/api-docs/swagger-config"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.csrfEnabled").value(true));
  }

  private JsonNode lireDocs() throws Exception {
    var reponse = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
    return new ObjectMapper().readTree(reponse.getResponse().getContentAsString());
  }
}
