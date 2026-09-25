package com.k48.leonel.presence48.controller;

import static com.k48.leonel.presence48.support.Connexion.MDP_ETUDIANT;
import static com.k48.leonel.presence48.support.Connexion.MDP_FORMATEUR;
import static com.k48.leonel.presence48.support.Connexion.connecter;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * EF2 / SF-2 : POST /api/sessions (imposé) et GET /api/sessions (protégé).
 * Contexte recréé à chaque test : le post-processeur csrf() de spring-security-test remplace le dépôt CSRF
 * du filtre partagé. La base H2 reste commune aux contextes : aucune assertion ne dépend de l'ordre des tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SessionIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private long promo(String nom) {
    Long id = jdbc.queryForObject("SELECT id FROM promotion WHERE nom = ?", Long.class, nom);
    return id == null ? -1 : id;
  }

  private String corps(String titre, long promotionId) {
    return "{\"titre\":\"" + titre + "\",\"promotionId\":" + promotionId + "}";
  }

  @Test
  void testOuvertureSansSessionRenvoie201AvecCodeEtExpiration() throws Exception {
    mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
            .content(corps("TP JPA", promo("P1-2026"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("[A-HJ-NP-Z2-9]{6}")))
        .andExpect(jsonPath("$.ouvertureAt").isString())
        .andExpect(jsonPath("$.expirationAt").isString())
        .andExpect(jsonPath("$.length()").value(4));
  }

  @Test
  void testOuvrirSessionTitreManquant400() throws Exception {
    mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
            .content("{\"promotionId\":" + promo("P1-2026") + "}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
  }

  @Test
  void testOuvrirSessionPromotionInconnue400() throws Exception {
    mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON).content(corps("TP", 999)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
  }

  @Test
  void testRg25EtudiantConnecteNePeutPasOuvrirDeSession() throws Exception {
    mvc.perform(post("/api/sessions").session(connecter(mvc, "awa", MDP_ETUDIANT)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(corps("TP", promo("P1-2026"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
  }

  @Test
  void testRg26FormateurHorsPromotion403EtDansSaPromotion201() throws Exception {
    var session = connecter(mvc, "formateur", MDP_FORMATEUR);
    mvc.perform(post("/api/sessions").session(session).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(corps("TP", promo("P2-2026"))))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
    mvc.perform(post("/api/sessions").session(session).with(csrf())
            .contentType(MediaType.APPLICATION_JSON).content(corps("TP", promo("P1-2026"))))
        .andExpect(status().isCreated());
  }

  @Test
  void testListeDesSessionsProtegeeEtFiltreeParRole() throws Exception {
    mvc.perform(get("/api/sessions").param("promotionId", String.valueOf(promo("P1-2026"))))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/sessions").param("promotionId", String.valueOf(promo("P1-2026")))
            .session(connecter(mvc, "formateur", MDP_FORMATEUR)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.titre == 'TP Flyway et migrations')].statut").value(hasItem("CLOTUREE")));
    var etudiant = connecter(mvc, "awa", MDP_ETUDIANT);
    mvc.perform(get("/api/sessions").param("promotionId", String.valueOf(promo("P1-2026"))).session(etudiant))
        .andExpect(status().isOk());
    mvc.perform(get("/api/sessions").param("promotionId", String.valueOf(promo("P2-2026"))).session(etudiant))
        .andExpect(status().isForbidden());
  }

  /** Régression #98 : un étudiant ne voit jamais le code de présence (RG2) ; le formateur le voit. */
  @Test
  void testRegression98CodeMasquePourLEtudiant() throws Exception {
    mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
            .content(corps("TP code", promo("P1-2026"))))
        .andExpect(status().isCreated());
    mvc.perform(get("/api/sessions").param("promotionId", String.valueOf(promo("P1-2026")))
            .session(connecter(mvc, "awa", MDP_ETUDIANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].code").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.nullValue())));
    mvc.perform(get("/api/sessions").param("promotionId", String.valueOf(promo("P1-2026")))
            .session(connecter(mvc, "formateur", MDP_FORMATEUR)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.titre == 'TP code')].code").value(org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$[?(@.titre == 'TP code')].code").value(
            org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.matchesPattern("[A-HJ-NP-Z2-9]{6}"))));
  }
}
