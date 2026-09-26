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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * #109 (RG25, EF19) : un étudiant connecté ne doit pas pouvoir distinguer les identifiants d'étudiant
 * existants des inexistants. Avant correctif, l'existence (404 ETUDIANT_INCONNU) était testée avant
 * l'identité (403 IDENTITE_DIFFERENTE) : énumération d'identifiants (OWASP A01).
 */
@SpringBootTest
@AutoConfigureMockMvc
class EnumerationIdentifiantIntegrationTest {

  private static final long INCONNU = 999_999;

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private long etudiant(String nom) {
    Long id = jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, nom);
    return id == null ? -1 : id;
  }

  private static void erreur(ResultActions r, int statut, String code) throws Exception {
    r.andExpect(status().is(statut)).andExpect(jsonPath("$.code").value(code));
  }

  @Test
  void testRg25EtudiantConnecteIdentifiantAutreOuInconnuRenvoie403SurSesExercices() throws Exception {
    var awa = etudiant("Awa Ndiaye");
    var session = connecter(mvc, "awa", MDP_ETUDIANT);
    erreur(mvc.perform(get("/api/etudiants/" + INCONNU + "/exercices").session(session)), 403,
        "IDENTITE_DIFFERENTE");
    // Contrôle positif : ses propres données restent lisibles (l'existence n'est demandée qu'après l'identité).
    mvc.perform(get("/api/etudiants/" + awa + "/exercices").session(session)).andExpect(status().isOk());
  }

  @Test
  void testRg25EtudiantConnecteIdentifiantAutreOuInconnuRenvoie403SurSesRelectures() throws Exception {
    var paul = etudiant("Paul Mbarga");
    var session = connecter(mvc, "paul", MDP_ETUDIANT);
    erreur(mvc.perform(get("/api/etudiants/" + INCONNU + "/relectures").session(session)), 403,
        "IDENTITE_DIFFERENTE");
    // Contrôle positif : ses propres données restent lisibles.
    mvc.perform(get("/api/etudiants/" + paul + "/relectures").session(session)).andExpect(status().isOk());
  }

  @Test
  void testRg26FormateurVOitUnEtudiantInconnuRenvoie404EtHorsPromotion403() throws Exception {
    erreur(mvc.perform(get("/api/etudiants/" + INCONNU + "/exercices")
        .session(connecter(mvc, "formateur", MDP_FORMATEUR))), 404, "ETUDIANT_INCONNU");
    // Nora est en P2 : le formateur (rattaché à P1, RG26) ne la voit pas, inconnue ou non.
    erreur(mvc.perform(get("/api/etudiants/" + etudiant("Nora Bella") + "/exercices")
        .session(connecter(mvc, "formateur", MDP_FORMATEUR))), 403, "ACCES_REFUSE");
  }
}
