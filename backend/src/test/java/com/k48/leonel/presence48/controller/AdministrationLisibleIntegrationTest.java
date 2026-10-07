package com.k48.leonel.presence48.controller;

import static com.k48.leonel.presence48.support.Connexion.MDP_ETUDIANT;
import static com.k48.leonel.presence48.support.Connexion.MDP_FORMATEUR;
import static com.k48.leonel.presence48.support.Connexion.connecterAdmin;
import static com.k48.leonel.presence48.support.Connexion.connecterAvecCsrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * #134 / contrat 2.8 : l'administration lit ce qu'elle modifie (formateurs rattachés, fiches avec leur
 * état et leur compte, comptes par rôle) et reçoit les codes d'erreur du catalogue (SF-20 à SF-22).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdministrationLisibleIntegrationTest {

  private static final String P1 = "P1-2026";
  private static final String P2 = "P2-2026";
  private static final String CODE = "$.code";

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private long idPromotion(String nom) {
    return jdbc.queryForObject("SELECT id FROM promotion WHERE nom = ?", Long.class, nom);
  }

  private long idCompte(String login) {
    return jdbc.queryForObject("SELECT id FROM utilisateur WHERE login = ?", Long.class, login);
  }

  private long idEtudiant(String nom) {
    return jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, nom);
  }

  private String formateurs(String promotion) {
    return "/api/promotions/" + idPromotion(promotion) + "/formateurs";
  }

  private String fiches(String promotion) {
    return "/api/promotions/" + idPromotion(promotion) + "/fiches";
  }

  private ResultActions rattacher(MockHttpSession admin, String promotion, String ids) throws Exception {
    return mvc.perform(put(formateurs(promotion)).session(admin).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"utilisateurIds\":[" + ids + "]}"));
  }

  private ResultActions creerCompte(MockHttpSession admin, String login, String motDePasse, Long etudiantId)
      throws Exception {
    return mvc.perform(post("/api/utilisateurs").session(admin).with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"login\":\"" + login + "\",\"nomAffiche\":\"Nouveau\",\"role\":\"ETUDIANT\","
            + "\"motDePasseInitial\":\"" + motDePasse + "\",\"etudiantId\":" + etudiantId + "}"));
  }

  // --- Formateurs rattachés ---

  @Test
  void testAdminLitLesFormateursRattachesAUnePromotion() throws Exception {
    var admin = connecterAdmin(mvc);

    mvc.perform(get(formateurs(P1)).session(admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].login", contains("formateur")));
    mvc.perform(get(formateurs(P2)).session(admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void testLaLectureDesFormateursEstReserveeALAdmin() throws Exception {
    mvc.perform(get(formateurs(P1)).session(connecterAvecCsrf(mvc, "formateur", MDP_FORMATEUR)))
        .andExpect(status().isForbidden()).andExpect(jsonPath(CODE).value("ACCES_REFUSE"));
    mvc.perform(get(formateurs(P1)))
        .andExpect(status().isUnauthorized()).andExpect(jsonPath(CODE).value("NON_AUTHENTIFIE"));
    mvc.perform(get("/api/promotions/9999/formateurs").session(connecterAdmin(mvc)))
        .andExpect(status().isNotFound()).andExpect(jsonPath(CODE).value("PROMOTION_INCONNUE"));
  }

  @Test
  void testUneListeVideRetireTousLesFormateurs() throws Exception {
    var admin = connecterAdmin(mvc);

    rattacher(admin, P1, "").andExpect(status().isNoContent());

    mvc.perform(get(formateurs(P1)).session(admin)).andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void testLeRattachementRefuseProprementLesIdentifiantsInvalides() throws Exception {
    var admin = connecterAdmin(mvc);

    rattacher(admin, P2, "null").andExpect(status().isBadRequest())
        .andExpect(jsonPath(CODE).value("CHAMP_MANQUANT"));
    rattacher(admin, P2, "9999").andExpect(status().isNotFound())
        .andExpect(jsonPath(CODE).value("UTILISATEUR_INTROUVABLE"));
    rattacher(admin, P2, String.valueOf(idCompte("admin"))).andExpect(status().isBadRequest())
        .andExpect(jsonPath(CODE).value("ROLE_INCOMPATIBLE"));
  }

  @Test
  void testUnFormateurDesactiveNePeutPasEtreRattache() throws Exception {
    var admin = connecterAdmin(mvc);
    long formateur = idCompte("formateur");
    mvc.perform(delete("/api/utilisateurs/" + formateur).session(admin).with(csrf()))
        .andExpect(status().isNoContent());

    rattacher(admin, P2, String.valueOf(formateur)).andExpect(status().isBadRequest())
        .andExpect(jsonPath(CODE).value("ROLE_INCOMPATIBLE"));
  }

  // --- Fiches d'une promotion ---

  @Test
  void testLesFichesDeGestionMontrentLEtatEtLeCompte() throws Exception {
    var admin = connecterAdmin(mvc);
    long marc = idEtudiant("Marc Tchoua");
    mvc.perform(delete("/api/etudiants/" + marc).session(admin).with(csrf()))
        .andExpect(status().isNoContent());

    mvc.perform(get(fiches(P1)).session(admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.nom=='Awa Ndiaye')].actif").value(true))
        .andExpect(jsonPath("$[?(@.nom=='Awa Ndiaye')].compteLogin").value("awa"))
        .andExpect(jsonPath("$[?(@.nom=='Marc Tchoua')].actif").value(false))
        .andExpect(jsonPath("$[?(@.nom=='Hugo Talla')].compteLogin").value((Object) null));
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM etudiant WHERE promotion_id = ?", Integer.class,
        idPromotion(P1))).as("toutes les fiches de P1 sont listées, désactivées comprises").isEqualTo(8);
    mvc.perform(get(fiches(P1)).session(admin)).andExpect(jsonPath("$", hasSize(8)));
  }

  @Test
  void testLesFichesSontLisiblesParLeFormateurDeLaPromotionSeulement() throws Exception {
    var formateur = connecterAvecCsrf(mvc, "formateur", MDP_FORMATEUR);

    mvc.perform(get(fiches(P1)).session(formateur)).andExpect(status().isOk());
    mvc.perform(get(fiches(P2)).session(formateur))
        .andExpect(status().isForbidden()).andExpect(jsonPath(CODE).value("ACCES_REFUSE"));
    mvc.perform(get(fiches(P1)).session(connecterAvecCsrf(mvc, "awa", MDP_ETUDIANT)))
        .andExpect(status().isForbidden()).andExpect(jsonPath(CODE).value("ACCES_REFUSE"));
    mvc.perform(get(fiches(P1)))
        .andExpect(status().isUnauthorized()).andExpect(jsonPath(CODE).value("NON_AUTHENTIFIE"));
  }

  // --- Comptes ---

  @Test
  void testLaListeDesComptesSeFiltreParRole() throws Exception {
    var admin = connecterAdmin(mvc);

    mvc.perform(get("/api/utilisateurs?role=FORMATEUR").session(admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contenu[*].login", contains("formateur")))
        .andExpect(jsonPath("$.total").value(1));
    mvc.perform(get("/api/utilisateurs?role=DIRECTEUR").session(admin))
        .andExpect(status().isBadRequest()).andExpect(jsonPath(CODE).value("CHAMP_MANQUANT"));
  }

  @Test
  void testUnMotDePasseProvisoireTropCourtRenvoieLeCodeDuCatalogue() throws Exception {
    var admin = connecterAdmin(mvc);

    creerCompte(admin, "hugo", "court", idEtudiant("Hugo Talla"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath(CODE).value("MOT_DE_PASSE_TROP_FAIBLE"));
    mvc.perform(post("/api/utilisateurs/" + idCompte("paul") + "/reinitialiser-mot-de-passe")
            .session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"motDePasseInitial\":\"court\"}"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath(CODE).value("MOT_DE_PASSE_TROP_FAIBLE"));
  }

  @Test
  void testUneFicheDejaLieeOuInconnueRenvoieSonPropreCode() throws Exception {
    var admin = connecterAdmin(mvc);

    creerCompte(admin, "awa2", "Provisoire48", idEtudiant("Awa Ndiaye"))
        .andExpect(status().isConflict()).andExpect(jsonPath(CODE).value("FICHE_DEJA_LIEE"));
    creerCompte(admin, "fantome", "Provisoire48", 9999L)
        .andExpect(status().isBadRequest()).andExpect(jsonPath(CODE).value("ETUDIANT_INCONNU"));
  }
}
