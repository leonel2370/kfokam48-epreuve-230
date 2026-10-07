package com.k48.leonel.presence48.controller;

import static com.k48.leonel.presence48.support.Connexion.MDP_ETUDIANT;
import static com.k48.leonel.presence48.support.Connexion.MDP_FORMATEUR;
import static com.k48.leonel.presence48.support.Connexion.connecterAvecCsrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * #129 / RG26 : un formateur ne gère que les fiches de SES promotions. Le compte de démonstration
 * « formateur » est rattaché à P1-2026 seulement ; Nora Bella est une étudiante de P2-2026.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FichesEtudiantsRg26IntegrationTest {

  private static final String P1 = "P1-2026";
  private static final String P2 = "P2-2026";
  private static final String NORA = "Nora Bella";
  private static final String AWA = "Awa Ndiaye";

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private long idPromotion(String nom) {
    return jdbc.queryForObject("SELECT id FROM promotion WHERE nom = ?", Long.class, nom);
  }

  private long idEtudiant(String nom) {
    return jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, nom);
  }

  private long promotionDe(long etudiantId) {
    return jdbc.queryForObject("SELECT promotion_id FROM etudiant WHERE id = ?", Long.class, etudiantId);
  }

  private MockHttpSession formateurDeP1() throws Exception {
    return connecterAvecCsrf(mvc, "formateur", MDP_FORMATEUR);
  }

  private ResultActions modifier(MockHttpSession session, long etudiantId, String nom, long promotionId)
      throws Exception {
    return mvc.perform(put("/api/etudiants/" + etudiantId).session(session).with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nom\":\"" + nom + "\",\"promotionId\":" + promotionId + "}"));
  }

  @Test
  void testFormateurNePeutPasSApproprierUneFicheDUneAutrePromotion() throws Exception {
    long nora = idEtudiant(NORA);

    modifier(formateurDeP1(), nora, "Capturee", idPromotion(P1))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));

    assertThat(promotionDe(nora)).as("Nora reste dans sa promotion P2").isEqualTo(idPromotion(P2));
    assertThat(jdbc.queryForObject("SELECT nom FROM etudiant WHERE id = ?", String.class, nora))
        .as("le nom de la fiche n'a pas changé").isEqualTo(NORA);
  }

  @Test
  void testFormateurNePeutPasEnvoyerSaFicheVersUnePromotionQuiNEstPasLaSienne() throws Exception {
    long awa = idEtudiant(AWA);

    modifier(formateurDeP1(), awa, AWA, idPromotion(P2))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));

    assertThat(promotionDe(awa)).as("Awa reste en P1").isEqualTo(idPromotion(P1));
  }

  @Test
  void testFormateurRenommeUneFicheDeSaPromotion() throws Exception {
    modifier(formateurDeP1(), idEtudiant(AWA), "Awa N.", idPromotion(P1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nom").value("Awa N."));
  }

  @Test
  void testFormateurNePeutPasSupprimerUneFicheDUneAutrePromotion() throws Exception {
    long nora = idEtudiant(NORA);

    mvc.perform(delete("/api/etudiants/" + nora).session(formateurDeP1()).with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));

    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM etudiant WHERE id = ? AND actif", Integer.class, nora))
        .as("la fiche de Nora existe toujours et reste active").isEqualTo(1);
  }

  @Test
  void testEtudiantNeGerePasLesFiches() throws Exception {
    var awa = connecterAvecCsrf(mvc, "awa", MDP_ETUDIANT);

    modifier(awa, idEtudiant(AWA), "Autre nom", idPromotion(P1))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
  }
}
