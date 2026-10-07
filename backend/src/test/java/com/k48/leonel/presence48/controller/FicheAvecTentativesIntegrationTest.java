package com.k48.leonel.presence48.controller;

import static com.k48.leonel.presence48.support.Connexion.connecterAdmin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * #133 / RG28 : un étudiant qui s'est seulement trompé de code (RG4) a déjà un historique.
 * Sa fiche est désactivée, pas supprimée : la suppression physique échouait sur la clé étrangère.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FicheAvecTentativesIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  @Test
  void testUneFicheAyantSeulementDesCodesErronesEstDesactivee() throws Exception {
    long nora = jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = 'Nora Bella'", Long.class);
    mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"FAUX00\",\"etudiantId\":" + nora + "}"))
        .andExpect(status().isBadRequest());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tentative_code WHERE etudiant_id = ?", Integer.class, nora))
        .as("le code erroné a laissé une tentative (RG4)").isEqualTo(1);

    mvc.perform(delete("/api/etudiants/" + nora).session(connecterAdmin(mvc)).with(csrf()))
        .andExpect(status().isNoContent());

    assertThat(jdbc.queryForObject("SELECT actif FROM etudiant WHERE id = ?", Boolean.class, nora))
        .as("la fiche est désactivée, pas supprimée (RG28)").isFalse();
  }
}
