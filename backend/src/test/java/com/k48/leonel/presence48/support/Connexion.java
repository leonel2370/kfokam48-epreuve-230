package com.k48.leonel.presence48.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/** Connexion avec un compte de démonstration (V3) ; renvoie la session à passer aux requêtes suivantes. */
public final class Connexion {

  public static final String MDP_ETUDIANT = "Etudiant48";
  public static final String MDP_FORMATEUR = "Formateur48";

  private Connexion() {
  }

  public static MockHttpSession connecter(MockMvc mvc, String login, String motDePasse) throws Exception {
    return (MockHttpSession) mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"login\":\"" + login + "\",\"motDePasse\":\"" + motDePasse + "\"}"))
        .andExpect(status().isOk())
        .andReturn().getRequest().getSession(false);
  }
}
