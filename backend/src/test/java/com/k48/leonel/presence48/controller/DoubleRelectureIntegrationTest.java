package com.k48.leonel.presence48.controller;

import static com.k48.leonel.presence48.support.Connexion.MDP_ETUDIANT;
import static com.k48.leonel.presence48.support.Connexion.connecter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Enveloppe, étape 3 (#85, #87) : deux relecteurs, note retenue = moyenne, provisoire tant qu'un seul a rendu. */
@SpringBootTest
@AutoConfigureMockMvc
class DoubleRelectureIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private long etudiant(String nom) {
    Long id = jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, nom);
    return id == null ? -1 : id;
  }

  private void rendre(long relectureId, long relecteurId, int note) throws Exception {
    mvc.perform(post("/api/relectures/" + relectureId).header("X-Etudiant-Id", relecteurId)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"note\":" + note + ",\"commentaire\":\"avis " + note + "\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void testRg6Rg16Rg31DeuxRelecteursMoyenneEtNoteProvisoire() throws Exception {
    var promotionId = jdbc.queryForObject("SELECT id FROM promotion WHERE nom = 'P1-2026'", Long.class);
    var session = mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
        .content("{\"titre\":\"TP double\",\"promotionId\":" + promotionId + "}")).andReturn().getResponse()
        .getContentAsString();
    for (var nom : List.of("Paul Mbarga", "Lina Kamga", "Marc Tchoua")) {
      mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
          .content("{\"code\":\"" + JsonPath.read(session, "$.code") + "\",\"etudiantId\":" + etudiant(nom) + "}"))
          .andExpect(status().isCreated());
    }
    var awa = etudiant("Awa Ndiaye");
    var depot = mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
            .content("{\"sessionId\":" + JsonPath.read(session, "$.id") + ",\"etudiantId\":" + awa
                + ",\"lien\":\"https://github.com/awa/double\"}"))
        .andExpect(jsonPath("$.statut").value("EN_ATTENTE_RELECTURE"))
        .andReturn().getResponse().getContentAsString();
    var exercice = ((Number) JsonPath.read(depot, "$.id")).longValue();
    var relectures = jdbc.queryForList("SELECT id, relecteur_id FROM relecture WHERE exercice_id = ? ORDER BY id",
        exercice);
    assertThat(relectures).as("RG6 v3 : deux relecteurs").hasSize(2);
    assertThat(relectures.stream().map(r -> r.get("relecteur_id")).distinct().count()).as("différents").isEqualTo(2);
    assertThat(relectures.stream().map(r -> ((Number) r.get("relecteur_id")).longValue()))
        .as("RG5 : jamais l'auteur").doesNotContain(awa);

    var premier = relectures.get(0);
    rendre(((Number) premier.get("id")).longValue(), ((Number) premier.get("relecteur_id")).longValue(), 12);
    var moi = connecter(mvc, "awa", MDP_ETUDIANT);
    var filtre = "$[?(@.id == " + exercice + ")].";
    mvc.perform(get("/api/etudiants/" + awa + "/exercices").session(moi))
        .andExpect(status().isOk())
        .andExpect(jsonPath(filtre + "statut").value(org.hamcrest.Matchers.contains("EN_ATTENTE_RELECTURE")))
        .andExpect(jsonPath(filtre + "noteRetenue").value(org.hamcrest.Matchers.contains(12.0)))
        .andExpect(jsonPath(filtre + "provisoire").value(org.hamcrest.Matchers.contains(true)))
        .andExpect(jsonPath("$[0].relecteurId").doesNotExist());

    var second = relectures.get(1);
    rendre(((Number) second.get("id")).longValue(), ((Number) second.get("relecteur_id")).longValue(), 17);
    mvc.perform(get("/api/etudiants/" + awa + "/exercices").session(moi))
        .andExpect(jsonPath(filtre + "statut").value(org.hamcrest.Matchers.contains("RELU")))
        .andExpect(jsonPath(filtre + "noteRetenue").value(org.hamcrest.Matchers.contains(14.5)))
        .andExpect(jsonPath(filtre + "provisoire").value(org.hamcrest.Matchers.contains(false)));
    // RG16 v3 : l'exercice compte pour sa note retenue 14,5 ; avec la note 14 de DEMO01 → (14 + 14,5) / 2
    mvc.perform(get("/api/tableau").param("promotionId", String.valueOf(promotionId)))
        .andExpect(jsonPath("$[?(@.nom == 'Awa Ndiaye')].moyenne").value(org.hamcrest.Matchers.contains(14.25)));
  }
}
