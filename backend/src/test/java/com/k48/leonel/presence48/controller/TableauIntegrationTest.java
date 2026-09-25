package com.k48.leonel.presence48.controller;

import static com.k48.leonel.presence48.support.Connexion.MDP_ETUDIANT;
import static com.k48.leonel.presence48.support.Connexion.MDP_FORMATEUR;
import static com.k48.leonel.presence48.support.Connexion.connecter;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** EF10 / SF-10 : GET /api/tableau sur les données de démonstration (V2). */
@SpringBootTest
@AutoConfigureMockMvc
class TableauIntegrationTest {

  private static final Duration LIMITE_ENF2 = Duration.ofSeconds(2);
  private static final int ETUDIANTS_ENF2 = 60;

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private long promo(String nom) {
    Long id = jdbc.queryForObject("SELECT id FROM promotion WHERE nom = ?", Long.class, nom);
    return id == null ? -1 : id;
  }

  private String ligne(String nom, String champ) {
    return "$[?(@.nom == '" + nom + "')]." + champ;
  }

  @Test
  void testRg16Rg11TableauDeLaPromotionDeDemonstration() throws Exception {
    mvc.perform(get("/api/tableau").param("promotionId", String.valueOf(promo("P1-2026"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(8))
        .andExpect(jsonPath("$[0].length()").value(6))
        .andExpect(jsonPath(ligne("Marc Tchoua", "presences")).value(1))
        .andExpect(jsonPath(ligne("Hugo Talla", "presences")).value(0))
        .andExpect(jsonPath(ligne("Hugo Talla", "exercicesDeposes")).value(0))
        .andExpect(jsonPath(ligne("Hugo Talla", "moyenne")).value(org.hamcrest.Matchers.contains((Object) null)))
        .andExpect(jsonPath(ligne("Ines Ngono", "exercicesDeposes")).value(1))
        .andExpect(jsonPath(ligne("Awa Ndiaye", "moyenne")).value(org.hamcrest.Matchers.contains(14.0)))
        .andExpect(jsonPath(ligne("Marc Tchoua", "relecturesEnAttente")).value(1))
        .andExpect(jsonPath(ligne("Awa Ndiaye", "relecturesEnAttente")).value(0));
  }

  @Test
  void testPromotionInconnue404EtDroitsParRole() throws Exception {
    mvc.perform(get("/api/tableau").param("promotionId", "999999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    mvc.perform(get("/api/tableau").param("promotionId", String.valueOf(promo("P1-2026")))
            .session(connecter(mvc, "awa", MDP_ETUDIANT)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
    var formateur = connecter(mvc, "formateur", MDP_FORMATEUR);
    mvc.perform(get("/api/tableau").param("promotionId", String.valueOf(promo("P2-2026"))).session(formateur))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/tableau").param("promotionId", String.valueOf(promo("P1-2026"))).session(formateur))
        .andExpect(status().isOk());
  }

  @Test
  void testEnf2SoixanteEtudiantsEnMoinsDeDeuxSecondes() throws Exception {
    var p2 = promo("P2-2026");
    for (var i = 0; i < ETUDIANTS_ENF2; i++) {
      jdbc.update("INSERT INTO etudiant (nom, promotion_id) VALUES (?, ?)", "Charge " + i, p2);
    }
    assertTimeout(LIMITE_ENF2, () -> mvc.perform(get("/api/tableau").param("promotionId", String.valueOf(p2)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(ETUDIANTS_ENF2 + 4)), "ENF2 : tableau de 64 étudiants en moins de 2 s");
  }
}
