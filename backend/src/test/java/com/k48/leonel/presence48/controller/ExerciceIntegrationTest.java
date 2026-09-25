package com.k48.leonel.presence48.controller;

import static com.k48.leonel.presence48.support.Connexion.MDP_ETUDIANT;
import static com.k48.leonel.presence48.support.Connexion.connecter;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * EF6 / SF-6 : POST /api/exercices (imposé). Chaque test ouvre sa propre session (base H2 partagée).
 * Contexte recréé à chaque test : le post-processeur csrf() remplace le dépôt CSRF du filtre partagé.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ExerciceIntegrationTest {

  private static final String LIEN = "https://github.com/k48/tp";

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private long etudiant(String nom) {
    Long id = jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, nom);
    return id == null ? -1 : id;
  }

  private long ouvrirSession(String promotion) throws Exception {
    var promotionId = jdbc.queryForObject("SELECT id FROM promotion WHERE nom = ?", Long.class, promotion);
    var reponse = mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
            .content("{\"titre\":\"TP dépôt\",\"promotionId\":" + promotionId + "}"))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return ((Number) JsonPath.read(reponse, "$.id")).longValue();
  }

  private ResultActions deposer(long sessionId, long etudiantId, String lien) throws Exception {
    return mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
        .content("{\"sessionId\":" + sessionId + ",\"etudiantId\":" + etudiantId + ",\"lien\":\"" + lien + "\"}"));
  }

  private static void erreur(ResultActions r, int statut, String code) throws Exception {
    r.andExpect(status().is(statut)).andExpect(jsonPath("$.code").value(code));
  }

  @Test
  void testHyp8AbsentDeposeEtObtient201PuisRg13DejaDepose() throws Exception {
    var session = ouvrirSession("P1-2026");
    var hugo = etudiant("Hugo Talla");
    deposer(session, hugo, LIEN)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.statut").value("DEPOSE"))
        .andExpect(jsonPath("$.length()").value(2));
    erreur(deposer(session, hugo, LIEN + "-v2"), 409, "EXERCICE_DEJA_DEPOSE");
  }

  @Test
  void testRg17LienNonHttpRenvoie400() throws Exception {
    var session = ouvrirSession("P1-2026");
    erreur(deposer(session, etudiant("Marc Tchoua"), "ftp://serveur/tp"), 400, "LIEN_INVALIDE");
    erreur(deposer(session, etudiant("Marc Tchoua"), "github.com/k48/tp"), 400, "LIEN_INVALIDE");
  }

  @Test
  void testSessionEtudiantPromotionInvalidesRenvoient400() throws Exception {
    var session = ouvrirSession("P1-2026");
    erreur(deposer(999_999, etudiant("Marc Tchoua"), LIEN), 400, "SESSION_INTROUVABLE");
    erreur(deposer(session, 999_999, LIEN), 400, "ETUDIANT_INCONNU");
    erreur(deposer(session, etudiant("Nora Bella"), LIEN), 400, "ETUDIANT_HORS_PROMOTION");
    erreur(mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON).content("{}")),
        400, "CHAMP_MANQUANT");
  }

  @Test
  void testRg18SessionClotureeRenvoie409() throws Exception {
    var demo = jdbc.queryForObject("SELECT id FROM session WHERE code = 'DEMO01'", Long.class);
    erreur(deposer(demo == null ? -1 : demo, etudiant("Hugo Talla"), LIEN), 409, "SESSION_CLOTUREE");
  }

  @Test
  void testConnecteAvecLIdentiteDUnAutreEtudiantRenvoie403() throws Exception {
    var session = ouvrirSession("P1-2026");
    erreur(mvc.perform(post("/api/exercices").session(connecter(mvc, "awa", MDP_ETUDIANT)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"sessionId\":" + session + ",\"etudiantId\":" + etudiant("Ines Ngono")
                + ",\"lien\":\"" + LIEN + "\"}")),
        403, "IDENTITE_DIFFERENTE");
  }
}
