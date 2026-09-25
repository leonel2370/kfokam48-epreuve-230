package com.k48.leonel.presence48.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/** EF1 / SF-1 : listes publiques de sélection (RG22), étudiants désactivés exclus (RG28). */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReferentielIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private long idDe(String nomPromotion) {
    Long id = jdbc.queryForObject("SELECT id FROM promotion WHERE nom = ?", Long.class, nomPromotion);
    return id == null ? -1 : id;
  }

  @Test
  void testPromotionsPubliquesTrieesParNom() throws Exception {
    mvc.perform(get("/api/promotions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].nom").value("P1-2026"))
        .andExpect(jsonPath("$[1].nom").value("P2-2026"));
  }

  @Test
  void testEtudiantsDeLaPromotionSansSession() throws Exception {
    mvc.perform(get("/api/promotions/{id}/etudiants", idDe("P1-2026")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(8))
        .andExpect(jsonPath("$[0].nom").value("Awa Ndiaye"))
        .andExpect(jsonPath("$[0].promotionId").value(idDe("P1-2026")));
  }

  @Test
  void testRg28EtudiantDesactiveAbsentDeLaListe() throws Exception {
    jdbc.update("UPDATE etudiant SET actif = FALSE WHERE nom = 'Awa Ndiaye'");
    mvc.perform(get("/api/promotions/{id}/etudiants", idDe("P1-2026")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(7))
        .andExpect(jsonPath("$[0].nom").value("Hugo Talla"));
  }

  @Test
  void testListeEtudiantsPromotionInconnue404() throws Exception {
    mvc.perform(get("/api/promotions/999/etudiants"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
  }
}
