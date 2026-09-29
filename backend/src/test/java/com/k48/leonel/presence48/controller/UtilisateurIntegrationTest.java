package com.k48.leonel.presence48.controller;

import static com.k48.leonel.presence48.support.Connexion.MDP_ETUDIANT;
import static com.k48.leonel.presence48.support.Connexion.MDP_FORMATEUR;
import static com.k48.leonel.presence48.support.Connexion.connecter;
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
 * #60 / SF-20 / EF21 : CRUD des comptes par l'ADMIN. RG27 : login unique (409 LOGIN_DEJA_UTILISE).
 * RG28 : jamais de suppression physique, désactivation (la connexion d'un compte désactivé est
 * refusée 403 COMPTE_DESACTIVE) ; le dernier admin actif ne peut pas se désactiver lui-même.
 * RG23 : un compte créé ou réinitialisé doit changer son mot de passe à la première connexion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UtilisateurIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private MockHttpSession admin() throws Exception {
    return connecter(mvc, "admin", "admin");
  }

  private long etudiant(String nom) {
    return jdbc.queryForObject("SELECT id FROM etudiant WHERE nom = ?", Long.class, nom);
  }

  private ResultActions liste(MockHttpSession session) throws Exception {
    return mvc.perform(get("/api/utilisateurs").session(session).with(csrf()));
  }

  private ResultActions creer(MockHttpSession session, String corps) throws Exception {
    return mvc.perform(post("/api/utilisateurs").session(session).with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content(corps));
  }

  private static void erreur(ResultActions r, int statut, String code) throws Exception {
    r.andExpect(status().is(statut)).andExpect(jsonPath("$.code").value(code));
  }

  @Test
  void testAdminListeLesComptesDeDemonstration() throws Exception {
    liste(admin())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contenu[?(@.login=='awa')].role").value("ETUDIANT"))
        .andExpect(jsonPath("$.contenu[?(@.login=='formateur')].role").value("FORMATEUR"))
        .andExpect(jsonPath("$.contenu[?(@.login=='admin')].doitChangerMotDePasse").value(true))
        .andExpect(jsonPath("$.total").isNumber());
  }

  @Test
  void testEf21CreerUnCompteEtudiantLieAUneFiche() throws Exception {
    creer(admin(), """
        {"login":"clara","nomAffiche":"Clara Ndongo","role":"ETUDIANT",
         "motDePasseInitial":"MotDePasse9","etudiantId":%d}
        """.formatted(etudiant("Sara Ebode")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.login").value("clara"))
        .andExpect(jsonPath("$.role").value("ETUDIANT"))
        .andExpect(jsonPath("$.actif").value(true))
        .andExpect(jsonPath("$.doitChangerMotDePasse").value(true))
        .andExpect(jsonPath("$.motDePasseInitial").doesNotExist());
  }

  @Test
  void testRg27LoginDejaUtiliseRenvoie409() throws Exception {
    erreur(creer(admin(), """
        {"login":"awa","nomAffiche":"Double","role":"ETUDIANT",
         "motDePasseInitial":"MotDePasse9","etudiantId":%d}
        """.formatted(etudiant("Yann Fotso"))), 409, "LOGIN_DEJA_UTILISE");
  }

  @Test
  void testRg23LeCompteCreeDoitChangerSonMotDePasseAvantTout() throws Exception {
    creer(admin(), """
        {"login":"clara","nomAffiche":"Clara Ndongo","role":"ETUDIANT",
         "motDePasseInitial":"MotDePasse9","etudiantId":%d}
        """.formatted(etudiant("Sara Ebode"))).andExpect(status().isCreated());
    var session = connecter(mvc, "clara", "MotDePasse9");
    mvc.perform(get("/api/moi").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.doitChangerMotDePasse").value(true));
    // RG23 : une autre route est bloquée tant que le mot de passe initial n'est pas changé
    erreur(mvc.perform(get("/api/utilisateurs").session(session).with(csrf())), 403,
        "CHANGEMENT_MOT_DE_PASSE_REQUIS");
    // après le changement, l'accès fonctionne
    mvc.perform(put("/api/moi/mot-de-passe").session(session).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"ancien\":\"MotDePasse9\",\"nouveau\":\"MotDePasse10\"}"))
        .andExpect(status().isNoContent());
    // après le changement, les routes protégées répondent normalement (RG23 levé) ;
    // clara est ETUDIANT : son récapitulatif répond 200
    mvc.perform(get("/api/moi/recap").session(session)).andExpect(status().isOk());
  }

  @Test
  void testRg28DesactiverUnCompteCoupeSaConnexion() throws Exception {
    var formateur2 = creer(admin(), """
        {"login":"formateur2","nomAffiche":"Deuxieme Formateur","role":"FORMATEUR",
         "motDePasseInitial":"MotDePasse9"}
        """).andReturn().getResponse().getContentAsString();
    org.assertj.core.api.Assertions.assertThat(formateur2).contains("\"id\"");
    mvc.perform(delete("/api/utilisateurs/6").session(admin()).with(csrf()))
        .andExpect(status().isNoContent());
    erreur(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
        .content("{\"login\":\"formateur2\",\"motDePasse\":\"MotDePasse9\"}")), 403, "COMPTE_DESACTIVE");
    // le compte désactivé apparaît inactif dans la liste
    liste(admin()).andExpect(jsonPath("$.contenu[?(@.login=='formateur2')].actif").value(false));
  }

  @Test
  void testRg28LeDernierAdminActifNePeutPasSeDesactiver() throws Exception {
    erreur(mvc.perform(delete("/api/utilisateurs/1").session(admin()).with(csrf())), 409,
        "SUPPRESSION_IMPOSSIBLE");
    mvc.perform(get("/api/moi").session(connecter(mvc, "admin", "admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ADMIN"));
  }

  @Test
  void testModifierRoleEtFicheLiee() throws Exception {
    var formateur1 = jdbc.queryForObject("SELECT id FROM utilisateur WHERE login = 'formateur'", Long.class);
    var sara = etudiant("Sara Ebode");
    mvc.perform(put("/api/utilisateurs/" + formateur1).session(admin()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"nomAffiche":"Jean Fokam","role":"ETUDIANT","actif":true,"etudiantId":%d}
                """.formatted(sara)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ETUDIANT"))
        .andExpect(jsonPath("$.etudiantId").value(sara));
    // rôle ETUDIANT sans fiche liée : refusé (contrainte V3)
    erreur(mvc.perform(put("/api/utilisateurs/" + formateur1).session(admin()).with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nomAffiche\":\"X\",\"role\":\"ETUDIANT\",\"actif\":true}")), 400, "CHAMP_MANQUANT");
  }

  @Test
  void testReinitialiserMotDePasseRemetLeChangementObligatoire() throws Exception {
    var paul = jdbc.queryForObject("SELECT id FROM utilisateur WHERE login = 'paul'", Long.class);
    mvc.perform(post("/api/utilisateurs/" + paul + "/reinitialiser-mot-de-passe").session(admin()).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"motDePasseInitial\":\"MotDePasse9\"}"))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/moi").session(connecter(mvc, "paul", "MotDePasse9")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.doitChangerMotDePasse").value(true));
  }

  @Test
  void testAccesRefuseAuxAutresRoles() throws Exception {
    var awa = connecter(mvc, "awa", MDP_ETUDIANT);
    erreur(liste(awa), 403, "ACCES_REFUSE");
    erreur(creer(awa, "{\"login\":\"x\",\"nomAffiche\":\"X\",\"role\":\"ETUDIANT\",\"motDePasseInitial\":\"MotDePasse9\"}"),
        403, "ACCES_REFUSE");
    var formateur = connecter(mvc, "formateur", MDP_FORMATEUR);
    erreur(liste(formateur), 403, "ACCES_REFUSE");
  }

  @Test
  void testSansSessionRenvoie401EtInconnuRenvoie404() throws Exception {
    erreur(mvc.perform(get("/api/utilisateurs").with(csrf())), 401, "NON_AUTHENTIFIE");
    erreur(mvc.perform(get("/api/utilisateurs/9999").session(admin()).with(csrf())), 404,
        "UTILISATEUR_INTROUVABLE");
  }
}
