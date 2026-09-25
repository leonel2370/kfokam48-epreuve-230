package com.k48.leonel.presence48.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Régression #83 (enveloppe, étape 3) : deux étudiants tapent le code en même temps alors qu'un exercice
 * de la session attend son relecteur (HYP-3). Les deux présences doivent être enregistrées (RG21) et
 * l'exercice ne doit recevoir qu'un seul relecteur (RG6).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PresencesSimulteesIntegrationTest {

  private static final int ESSAIS = 5;

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private long etudiant(String nom) {
    Long id = jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, nom);
    return id == null ? -1 : id;
  }

  private int marquer(String code, long etudiantId) throws Exception {
    return mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiantId + "}"))
        .andReturn().getResponse().getStatus();
  }

  /** Session P1 où Hugo, seul présent, a déposé : l'exercice reste DEPOSE. Renvoie [sessionId, code]. */
  private Object[] sessionAvecExerciceEnAttente() throws Exception {
    var promotionId = jdbc.queryForObject("SELECT id FROM promotion WHERE nom = 'P1-2026'", Long.class);
    var session = mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
            .content("{\"titre\":\"TP simultané\",\"promotionId\":" + promotionId + "}"))
        .andReturn().getResponse().getContentAsString();
    String code = JsonPath.read(session, "$.code");
    var sessionId = ((Number) JsonPath.read(session, "$.id")).longValue();
    var hugo = etudiant("Hugo Talla");
    assertThat(marquer(code, hugo)).as("Hugo présent").isEqualTo(201);
    mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
            .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":" + hugo + ",\"lien\":\"https://x.cm/tp\"}"))
        .andExpect(status().isCreated());
    return new Object[] {sessionId, code};
  }

  @Test
  void testRegression83DeuxPresencesSimultaneesSontToutesDeuxEnregistrees() throws Exception {
    var paul = etudiant("Paul Mbarga");
    var lina = etudiant("Lina Kamga");
    try (var pool = Executors.newFixedThreadPool(2)) {
      for (var essai = 0; essai < ESSAIS; essai++) {
        var session = sessionAvecExerciceEnAttente();
        var depart = new CountDownLatch(1);
        List<Callable<Integer>> taches = new ArrayList<>();
        for (var etudiantId : List.of(paul, lina)) {
          taches.add(() -> {
            depart.await();
            return marquer((String) session[1], etudiantId);
          });
        }
        var resultats = taches.stream().map(pool::submit).toList();
        depart.countDown();
        var statuts = new ArrayList<Integer>();
        for (var r : resultats) {
          statuts.add(r.get());
        }
        assertThat(statuts).as("essai %d : deux 201", essai).containsExactly(201, 201);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM presence WHERE session_id = ?", Integer.class, session[0]))
            .as("essai %d : Hugo, Paul et Lina présents", essai).isEqualTo(3);
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*) FROM relecture r JOIN exercice e ON e.id = r.exercice_id WHERE e.session_id = ?""",
            Integer.class, session[0])).as("essai %d : un seul relecteur (RG6)", essai).isEqualTo(1);
      }
    }
  }
}
