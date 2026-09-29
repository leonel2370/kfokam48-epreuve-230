package com.k48.leonel.presence48.controller;

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

/**
 * #113 (décision PO, RG28) : une fiche étudiant désactivée ne peut plus agir. Les opérations imposées
 * restent publiques (RG22) mais refusent une fiche inactive : 403 ETUDIANT_DESACTIVE sur
 * POST /api/presences et POST /api/exercices, avant tout autre contrôle métier (une fiche
 * désactivée n'appartient plus à la promotion active).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EtudiantDesactiveIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private long desactiver(String nom) {
    jdbc.update("UPDATE etudiant SET actif = FALSE WHERE nom = ?", nom);
    return jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, nom);
  }

  private String ouvrirSession(String promotion) throws Exception {
    var promotionId = jdbc.queryForObject("SELECT id FROM promotion WHERE nom = ?", Long.class, promotion);
    var reponse = mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
            .content("{\"titre\":\"TP désactivé\",\"promotionId\":" + promotionId + "}"))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return JsonPath.read(reponse, "$.code");
  }

  private static void erreur(org.springframework.test.web.servlet.ResultActions r, int statut, String code)
      throws Exception {
    r.andExpect(status().is(statut)).andExpect(jsonPath("$.code").value(code));
  }

  @Test
  void testRg28EtudiantDesactiveNePeutPlusMarquerSaPresence() throws Exception {
    var hugo = desactiver("Hugo Talla");
    var code = ouvrirSession("P1-2026");
    erreur(mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
        .content("{\"code\":\"" + code + "\",\"etudiantId\":" + hugo + "}")), 403, "ETUDIANT_DESACTIVE");
  }

  @Test
  void testRg28EtudiantDesactiveNePeutPasDeposer() throws Exception {
    var marc = desactiver("Marc Tchoua");
    var sessionP1 = jdbc.queryForObject("SELECT id FROM session WHERE code = 'DEMO01'", Long.class);
    erreur(mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON)
        .content("{\"sessionId\":" + sessionP1 + ",\"etudiantId\":" + marc
            + ",\"lien\":\"https://github.com/marc/exercice\"}")), 403, "ETUDIANT_DESACTIVE");
  }

  @Test
  void testRg28EtudiantDesactiveRefuseAvantLesAutresControles() throws Exception {
    // fiche désactivée + session clôturée (DEMO01) : le refus RG28 passe avant le contrôle RG18
    var ines = desactiver("Ines Ngono");
    erreur(mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
        .content("{\"code\":\"DEMO01\",\"etudiantId\":" + ines + "}")), 403, "ETUDIANT_DESACTIVE");
  }

  @Test
  void testEtudiantActifNEstPasAffecte() throws Exception {
    var marc = jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = 'Marc Tchoua'", Long.class);
    var code = ouvrirSession("P1-2026");
    mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"" + code + "\",\"etudiantId\":" + marc + "}"))
        .andExpect(status().isCreated());
  }
}
