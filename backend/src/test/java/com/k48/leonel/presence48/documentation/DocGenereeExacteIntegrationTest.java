package com.k48.leonel.presence48.documentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * #139 : la documentation générée dit ce que le code fait. Chaque test compare la doc servie par
 * /v3/api-docs à un comportement vérifié ailleurs par un test d'intégration.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DocGenereeExacteIntegrationTest {

  private static final List<String> VERBES = List.of("get", "post", "put", "delete");
  private static final String JSON = "application/json";
  private static final Pattern VERSION_CONTRAT = Pattern.compile("(?m)^  version: \"([0-9.]+)\"");

  @Autowired
  private MockMvc mvc;

  private JsonNode doc;

  @BeforeEach
  void lireLaDoc() throws Exception {
    var corps = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    doc = new ObjectMapper().readTree(corps);
  }

  private JsonNode operation(String chemin, String verbe) {
    return doc.path("paths").path(chemin).path(verbe);
  }

  private JsonNode reponse(String chemin, String verbe, String statut) {
    return operation(chemin, verbe).path("responses").path(statut);
  }

  /** Toutes les opérations de la doc, sous la forme « verbe chemin ». */
  private List<Map.Entry<String, JsonNode>> operations() {
    var toutes = new ArrayList<Map.Entry<String, JsonNode>>();
    for (Map.Entry<String, JsonNode> chemin : doc.path("paths").properties()) {
      for (var verbe : VERBES) {
        if (chemin.getValue().has(verbe)) {
          toutes.add(Map.entry(verbe + " " + chemin.getKey(), chemin.getValue().path(verbe)));
        }
      }
    }
    return toutes;
  }

  @Test
  void testLaVersionAfficheeEstCelleDuContrat() throws Exception {
    var contrat = mvc.perform(get("/contrat.yaml")).andReturn().getResponse()
        .getContentAsString(StandardCharsets.UTF_8);
    var version = VERSION_CONTRAT.matcher(contrat);
    assertThat(version.find()).as("le contrat déclare une version").isTrue();

    assertThat(doc.path("info").path("version").asText())
        .as("la doc générée annonce la version du contrat").isEqualTo(version.group(1));
  }

  @Test
  void testAucuneReponseNEstPublieeSansTypeDeContenu() {
    for (var op : operations()) {
      for (Map.Entry<String, JsonNode> rep : op.getValue().path("responses").properties()) {
        assertThat(rep.getValue().path("content").has("*/*"))
            .as("%s, réponse %s : type de contenu indéterminé", op.getKey(), rep.getKey()).isFalse();
      }
    }
  }

  @Test
  void testLesListesSontDocumenteesCommeDesTableaux() {
    for (var liste : List.of("/api/promotions", "/api/promotions/{promotionId}/etudiants", "/api/tableau",
        "/api/sessions", "/api/etudiants/{etudiantId}/exercices", "/api/etudiants/{etudiantId}/relectures")) {
      assertThat(reponse(liste, "get", "200").path("content").path(JSON).path("schema").path("type").asText())
          .as("GET %s renvoie une liste", liste).isEqualTo("array");
    }
  }

  @Test
  void testLaConnexionDocumenteLe401DesIdentifiantsFaux() {
    assertThat(reponse("/api/auth/login", "post", "401").isMissingNode())
        .as("identifiants faux : 401 IDENTIFIANTS_INVALIDES").isFalse();
    assertThat(reponse("/api/auth/login", "post", "400").toString())
        .as("le 400 ne décrit que le champ manquant").doesNotContain("IDENTIFIANTS_INVALIDES");
  }

  @Test
  void testLeChangementDeMotDePasseDocumenteLAncienMotDePasseFaux() {
    assertThat(reponse("/api/moi/mot-de-passe", "put", "401").toString())
        .as("ancien mot de passe incorrect : 401").contains("IDENTIFIANTS_INVALIDES");
  }

  @Test
  void testLaFicheDesactiveeEstDocumenteeSurLaPresenceEtLeDepot() {
    for (var chemin : List.of("/api/presences", "/api/exercices")) {
      assertThat(reponse(chemin, "post", "403").toString())
          .as("POST %s : 403 ETUDIANT_DESACTIVE (#113)", chemin).contains("ETUDIANT_DESACTIVE");
    }
  }

  @Test
  void testChaqueOperationProtegeeDocumenteLe401EtLe403() {
    for (var op : operations()) {
      if (op.getValue().path("security").isEmpty()) {
        continue;
      }
      var reponses = op.getValue().path("responses");
      assertThat(reponses.path("401").toString()).as("%s : 401 sans session", op.getKey())
          .contains("NON_AUTHENTIFIE").contains("Vous devez être connecté.");
      assertThat(reponses.has("403")).as("%s : 403 (rôle, CSRF ou mot de passe à changer)", op.getKey()).isTrue();
    }
  }

  @Test
  void testChaqueTagEstDefiniUneSeuleFoisEtLesEcrituresDePromotionSontDeLAdministration() {
    var noms = new ArrayList<String>();
    doc.path("tags").forEach(tag -> noms.add(tag.path("name").asText()));
    assertThat(noms).as("pas de tag défini deux fois").doesNotHaveDuplicates();
    for (var ecriture : List.of("post /api/promotions", "put /api/promotions/{promotionId}",
        "delete /api/promotions/{promotionId}", "put /api/promotions/{promotionId}/formateurs")) {
      var morceaux = ecriture.split(" ");
      assertThat(operation(morceaux[1], morceaux[0]).path("tags").toString())
          .as("%s est une opération d'administration", ecriture).contains("administration");
    }
  }
}
