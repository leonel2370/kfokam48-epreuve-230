package com.k48.leonel.presence48.controller;

import static com.k48.leonel.presence48.support.Connexion.connecterAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * #132 / ENF3 : la liste des comptes ne renvoie jamais d'erreur technique. Le contrat borne la
 * pagination : page à partir de 0, taille de 1 à 100.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PaginationComptesIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @ParameterizedTest
  @ValueSource(strings = {"page=-1", "size=0", "size=-5", "size=101", "size=10000000"})
  void testPaginationHorsBornesRenvoie400(String parametre) throws Exception {
    mvc.perform(get("/api/utilisateurs?" + parametre).session(connecterAdmin(mvc)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
  }

  @Test
  void testLaTailleMaximaleEstAcceptee() throws Exception {
    mvc.perform(get("/api/utilisateurs?page=0&size=100").session(connecterAdmin(mvc)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.taille").value(100));
  }

  @Test
  void testUnePageAuDelaDeLaDerniereEstVide() throws Exception {
    mvc.perform(get("/api/utilisateurs?page=50&size=20").session(connecterAdmin(mvc)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contenu").isEmpty());
  }
}
