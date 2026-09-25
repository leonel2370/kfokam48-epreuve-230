package com.k48.leonel.presence48.securite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

/** SF-15 à SF-19 : connexion, déconnexion, profil, mot de passe, compte admin par défaut, routes publiques. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private static String corps(String login, String motDePasse) {
    return "{\"login\":\"" + login + "\",\"motDePasse\":\"" + motDePasse + "\"}";
  }

  private MockHttpSession connecter(String login, String motDePasse) throws Exception {
    return (MockHttpSession) mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(corps(login, motDePasse)))
        .andExpect(status().isOk())
        .andReturn().getRequest().getSession(false);
  }

  @Test
  void testConnexionPuisDeconnexionRenvoie401() throws Exception {
    MockHttpSession session = connecter("formateur", "Formateur48");
    mvc.perform(get("/api/moi").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("FORMATEUR"))
        .andExpect(jsonPath("$.nomAffiche").value("Jean Fokam"))
        .andExpect(jsonPath("$.promotionIds.length()").value(1));
    mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
    mvc.perform(get("/api/moi").session(session))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
  }

  @Test
  void testProfilEtudiantPorteSaFicheEtSaPromotion() throws Exception {
    mvc.perform(get("/api/moi").session(connecter("awa", "Etudiant48")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ETUDIANT"))
        .andExpect(jsonPath("$.etudiantId").isNumber())
        .andExpect(jsonPath("$.promotionIds.length()").value(1))
        .andExpect(jsonPath("$.motDePasseHash").doesNotExist());
  }

  @Test
  void testMauvaisMotDePasseRenvoie401() throws Exception {
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corps("awa", "faux")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("IDENTIFIANTS_INVALIDES"));
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corps("inconnu", "x")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("IDENTIFIANTS_INVALIDES"));
  }

  @Test
  void testRg24CinqEchecsBloquentLeCompte() throws Exception {
    for (int i = 0; i < 5; i++) {
      mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corps("paul", "faux")))
          .andExpect(status().isUnauthorized());
    }
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corps("paul", "Etudiant48")))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("TROP_DE_TENTATIVES"));
  }

  @Test
  void testRg28CompteDesactiveRenvoie403() throws Exception {
    jdbc.update("UPDATE utilisateur SET actif = FALSE WHERE login = 'lina'");
    mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corps("lina", "Etudiant48")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("COMPTE_DESACTIVE"));
  }

  @Test
  void testRg23AdminDoitChangerSonMotDePasse() throws Exception {
    MockHttpSession session = connecter("admin", "admin");
    mvc.perform(get("/api/moi").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.doitChangerMotDePasse").value(true));
    mvc.perform(get("/api/utilisateurs").session(session))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CHANGEMENT_MOT_DE_PASSE_REQUIS"));
    mvc.perform(put("/api/moi/mot-de-passe").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"ancien\":\"admin\",\"nouveau\":\"Admin-2026!\"}"))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/utilisateurs").session(session))
        .andExpect(status().isNotFound());   // la route n'existe pas encore (#60) : le filtre RG23 ne bloque plus
    // Reconnexion depuis un navigateur ayant déjà une session : le jeton CSRF est exigé (et fourni par Angular)
    mvc.perform(post("/api/auth/login").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(corps("admin", "Admin-2026!")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.doitChangerMotDePasse").value(false));
  }

  @Test
  void testRg24MotDePasseCourtOuAncienFauxRefuse() throws Exception {
    MockHttpSession session = connecter("awa", "Etudiant48");
    mvc.perform(put("/api/moi/mot-de-passe").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"ancien\":\"Etudiant48\",\"nouveau\":\"court\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MOT_DE_PASSE_TROP_FAIBLE"));
    mvc.perform(put("/api/moi/mot-de-passe").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"ancien\":\"faux\",\"nouveau\":\"NouveauMdp48\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("IDENTIFIANTS_INVALIDES"));
  }

  @Test
  void testEcritureProtegeeSansJetonCsrfRefusee() throws Exception {
    mvc.perform(put("/api/moi/mot-de-passe").session(connecter("awa", "Etudiant48"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"ancien\":\"Etudiant48\",\"nouveau\":\"NouveauMdp48\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
  }

  @Test
  void testOperationImposeeAvecSessionExigeLeJetonCsrf() throws Exception {
    // Un site tiers ne peut pas utiliser le cookie de session de la victime sur une route publique
    mvc.perform(post("/api/presences").session(connecter("awa", "Etudiant48"))
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
  }

  @Test
  void testRouteProtegeeSansSessionRenvoie401() throws Exception {
    mvc.perform(get("/api/moi"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
  }

  @Test
  void testRg22OperationsImposeesSansSessionJamais401() throws Exception {
    // Les contrôleurs métier arrivent avec leurs tickets : ici on prouve seulement que la sécurité laisse passer.
    int[] statuts = {
        mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andReturn().getResponse().getStatus(),
        mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andReturn().getResponse().getStatus(),
        mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andReturn().getResponse().getStatus(),
        mvc.perform(post("/api/relectures/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andReturn().getResponse().getStatus(),
        mvc.perform(get("/api/tableau").param("promotionId", "1")).andReturn().getResponse().getStatus()
    };
    assertThat(statuts).as("aucune opération imposée ne demande de connexion ni de jeton CSRF")
        .doesNotContain(401, 403);
  }
}
