package com.k48.leonel.presence48.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** EF7 / SF-7 / HYP-3 : tirage au dépôt, puis retenté à chaque nouvelle présence. Une session par test. */
@SpringBootTest
@AutoConfigureMockMvc
class TirageIntegrationTest {

  private static final String LIEN = "https://github.com/k48/tp";

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private long etudiant(String nom) {
    Long id = jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, nom);
    return id == null ? -1 : id;
  }

  /** Ouvre une session P1 et renvoie [id, code]. */
  private Object[] ouvrirSession() throws Exception {
    var promotionId = jdbc.queryForObject("SELECT id FROM promotion WHERE nom = 'P1-2026'", Long.class);
    var reponse = mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
            .content("{\"titre\":\"TP tirage\",\"promotionId\":" + promotionId + "}"))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return new Object[] {((Number) JsonPath.read(reponse, "$.id")).longValue(), JsonPath.read(reponse, "$.code")};
  }

  private void marquer(Object code, long etudiantId) throws Exception {
    mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiantId + "}"))
        .andExpect(status().isCreated());
  }

  private ResultActions deposer(Object sessionId, long etudiantId) throws Exception {
    return mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
        .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":" + etudiantId + ",\"lien\":\"" + LIEN + "\"}"))
        .andExpect(status().isCreated());
  }

  private Long relecteurDe(long exerciceId) {
    return jdbc.queryForObject("SELECT relecteur_id FROM relecture WHERE exercice_id = ?", Long.class, exerciceId);
  }

  private static long id(ResultActions r) throws Exception {
    return ((Number) JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
  }

  @Test
  void testRg7UnAutrePresentEstAssigneAuDepot() throws Exception {
    var session = ouvrirSession();
    var paul = etudiant("Paul Mbarga");
    marquer(session[1], paul);
    var depot = deposer(session[0], etudiant("Hugo Talla"))
        .andExpect(jsonPath("$.statut").value("EN_ATTENTE_RELECTURE"));
    assertThat(relecteurDe(id(depot))).as("seul autre présent : Paul").isEqualTo(paul);
  }

  @Test
  void testHyp3SansCandidatResteDeposePuisAssigneALaPresenceSuivante() throws Exception {
    var session = ouvrirSession();
    var hugo = etudiant("Hugo Talla");
    marquer(session[1], hugo);
    var exercice = id(deposer(session[0], hugo).andExpect(jsonPath("$.statut").value("DEPOSE")));
    var lina = etudiant("Lina Kamga");
    marquer(session[1], lina);
    assertThat(jdbc.queryForObject("SELECT statut FROM exercice WHERE id = ?", String.class, exercice))
        .as("statut après la présence de Lina").isEqualTo("EN_ATTENTE_RELECTURE");
    assertThat(relecteurDe(exercice)).as("Lina, seule candidate (RG5 exclut Hugo)").isEqualTo(lina);
  }
}
