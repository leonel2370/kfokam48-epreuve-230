package com.k48.leonel.presence48.securite;

import static com.k48.leonel.presence48.support.Connexion.MDP_ETUDIANT;
import static com.k48.leonel.presence48.support.Connexion.connecterAdmin;
import static com.k48.leonel.presence48.support.Connexion.connecterAvecCsrf;
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

/**
 * #131 / RG25, RG28 : une session ouverte suit l'état réel du compte. Désactiver un compte ou
 * changer son rôle prend effet à la requête suivante, sans attendre l'expiration de la session.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SessionCompteModifieIntegrationTest {

  private static final String MDP_PROVISOIRE = "Provisoire48";
  private static final String MDP_DEFINITIF = "Definitif48";

  @Autowired
  private MockMvc mvc;

  @Autowired
  private JdbcTemplate jdbc;

  private long idCompte(String login) {
    return jdbc.queryForObject("SELECT id FROM utilisateur WHERE login = ?", Long.class, login);
  }

  /** Crée un second administrateur et le connecte, mot de passe provisoire déjà changé (RG23). */
  private MockHttpSession secondAdminConnecte(MockHttpSession admin) throws Exception {
    mvc.perform(post("/api/utilisateurs").session(admin).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"login\":\"admin2\",\"nomAffiche\":\"Second admin\",\"role\":\"ADMIN\","
                + "\"motDePasseInitial\":\"" + MDP_PROVISOIRE + "\"}"))
        .andExpect(status().isCreated());
    var session = connecterAvecCsrf(mvc, "admin2", MDP_PROVISOIRE);
    mvc.perform(put("/api/moi/mot-de-passe").session(session).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"ancien\":\"" + MDP_PROVISOIRE + "\",\"nouveau\":\"" + MDP_DEFINITIF + "\"}"))
        .andExpect(status().isNoContent());
    return session;
  }

  @Test
  void testUnCompteDesactivePerdSaSessionOuverte() throws Exception {
    var paul = connecterAvecCsrf(mvc, "paul", MDP_ETUDIANT);
    mvc.perform(get("/api/moi").session(paul)).andExpect(status().isOk());

    mvc.perform(delete("/api/utilisateurs/" + idCompte("paul")).session(connecterAdmin(mvc)).with(csrf()))
        .andExpect(status().isNoContent());

    mvc.perform(get("/api/moi").session(paul))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
  }

  @Test
  void testUnAdminRetrogradePerdSesDroitsSansSeReconnecter() throws Exception {
    var admin = connecterAdmin(mvc);
    var second = secondAdminConnecte(admin);
    mvc.perform(get("/api/utilisateurs").session(second)).andExpect(status().isOk());

    mvc.perform(put("/api/utilisateurs/" + idCompte("admin2")).session(admin).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nomAffiche\":\"Second admin\",\"role\":\"FORMATEUR\",\"actif\":true}"))
        .andExpect(status().isOk());

    mvc.perform(get("/api/utilisateurs").session(second))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
    mvc.perform(get("/api/moi").session(second))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("FORMATEUR"));
  }

  @Test
  void testUnCompteInchangeGardeSaSession() throws Exception {
    var paul = connecterAvecCsrf(mvc, "paul", MDP_ETUDIANT);

    mvc.perform(delete("/api/utilisateurs/" + idCompte("lina")).session(connecterAdmin(mvc)).with(csrf()))
        .andExpect(status().isNoContent());

    mvc.perform(get("/api/moi").session(paul))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.login").value("paul"));
  }
}
