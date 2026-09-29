package com.k48.leonel.presence48.controller;

import static com.k48.leonel.presence48.support.Connexion.MDP_ETUDIANT;
import static com.k48.leonel.presence48.support.Connexion.MDP_FORMATEUR;
import static com.k48.leonel.presence48.support.Connexion.connecter;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * #112 / SF-17 : GET /api/moi/recap — la ligne de tableau (EF10) de l'étudiant connecté, calculée par le
 * serveur (F3, même requête agrégée que le tableau). 401 sans session, 403 pour un compte non étudiant.
 * Données V2 : Awa = 1 présence, 1 exercice relu noté 14 (moyenne 14.00), 0 relecture en attente ;
 * Lina = 1 exercice en attente de relecture (moyenne null) et 1 relecture à rendre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RecapIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Test
  void testRg16RecapDeLEtudiantConnecte() throws Exception {
    mvc.perform(get("/api/moi/recap").session(connecter(mvc, "awa", MDP_ETUDIANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.etudiantId").value(1))
        .andExpect(jsonPath("$.nom").value("Awa Ndiaye"))
        .andExpect(jsonPath("$.presences").value(1))
        .andExpect(jsonPath("$.exercicesDeposes").value(1))
        .andExpect(jsonPath("$.moyenne").value(14.0))
        .andExpect(jsonPath("$.relecturesEnAttente").value(0));
  }

  @Test
  void testRecapSansNoteEtAvecRelecturesEnAttente() throws Exception {
    mvc.perform(get("/api/moi/recap").session(connecter(mvc, "lina", MDP_ETUDIANT)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.presences").value(1))
        .andExpect(jsonPath("$.exercicesDeposes").value(1))
        .andExpect(jsonPath("$.moyenne").value(org.hamcrest.Matchers.contains((Object) null)))
        .andExpect(jsonPath("$.relecturesEnAttente").value(1));
  }

  @Test
  void testSansSessionRenvoie401() throws Exception {
    mvc.perform(get("/api/moi/recap")).andExpect(status().isUnauthorized());
  }

  @Test
  void testCompteNonEtudiantRenvoie403() throws Exception {
    mvc.perform(get("/api/moi/recap").session(connecter(mvc, "formateur", MDP_FORMATEUR)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
  }
}
