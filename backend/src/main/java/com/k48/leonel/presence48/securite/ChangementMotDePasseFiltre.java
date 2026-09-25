package com.k48.leonel.presence48.securite;

import com.k48.leonel.presence48.repository.UtilisateurRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * RG23 : tant que le mot de passe initial n'est pas changé, seules restent accessibles
 * le profil, le changement de mot de passe et la déconnexion.
 */
public class ChangementMotDePasseFiltre extends OncePerRequestFilter {

  private static final Set<String> AUTORISEES = Set.of(
      "GET /api/moi", "PUT /api/moi/mot-de-passe", "POST /api/auth/logout", "POST /api/auth/login");

  private final UtilisateurRepository utilisateurs;

  public ChangementMotDePasseFiltre(UtilisateurRepository utilisateurs) {
    this.utilisateurs = utilisateurs;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain chaine)
      throws ServletException, IOException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof UtilisateurConnecte connecte
        && !AUTORISEES.contains(requete.getMethod() + " " + requete.getRequestURI())
        && utilisateurs.findById(connecte.id()).map(u -> u.isDoitChangerMotDePasse()).orElse(false)) {
      ReponseErreurJson.ecrire(reponse, 403, "CHANGEMENT_MOT_DE_PASSE_REQUIS",
          "Vous devez changer votre mot de passe avant de continuer.");
      return;
    }
    chaine.doFilter(requete, reponse);
  }
}
