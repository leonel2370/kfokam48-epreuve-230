package com.k48.leonel.presence48.securite;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** 401 et 403 de Spring Security au format du contrat (RG22, RG25). */
final class GestionnairesErreurSecurite {

  static final String NON_AUTHENTIFIE = "Vous devez être connecté.";
  static final String ACCES_REFUSE = "Vous n'avez pas les droits pour cette action.";

  private GestionnairesErreurSecurite() {
  }

  static AuthenticationEntryPoint nonAuthentifie() {
    return (requete, reponse, e) ->
        ReponseErreurJson.ecrire(reponse, HttpServletResponse.SC_UNAUTHORIZED, "NON_AUTHENTIFIE", NON_AUTHENTIFIE);
  }

  static AccessDeniedHandler accesRefuse() {
    return (requete, reponse, e) ->
        ReponseErreurJson.ecrire(reponse, HttpServletResponse.SC_FORBIDDEN, "ACCES_REFUSE", ACCES_REFUSE);
  }
}
