package com.k48.leonel.presence48.securite;

import com.k48.leonel.presence48.entity.Utilisateur;
import com.k48.leonel.presence48.repository.UtilisateurRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * #131 / RG25, RG28 : la session suit l'état réel du compte. À chaque requête d'un utilisateur
 * connecté, le compte est relu : désactivé ou disparu, la session est détruite (401) ; rôle ou fiche
 * modifiés, les droits de la session sont remplacés avant le contrôle d'accès.
 */
public class CompteAJourFiltre extends OncePerRequestFilter {

  private static final String CONNEXION = "POST /api/auth/login";

  private final UtilisateurRepository utilisateurs;
  private final SecurityContextRepository depotDeContexte;

  public CompteAJourFiltre(UtilisateurRepository utilisateurs, SecurityContextRepository depotDeContexte) {
    this.utilisateurs = utilisateurs;
    this.depotDeContexte = depotDeContexte;
  }

  /** Jeton d'authentification d'un compte : son identité et l'autorité de son rôle. */
  public static UsernamePasswordAuthenticationToken jeton(UtilisateurConnecte connecte) {
    return UsernamePasswordAuthenticationToken.authenticated(connecte, null,
        List.of(new SimpleGrantedAuthority("ROLE_" + connecte.role().name())));
  }

  @Override
  protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain chaine)
      throws ServletException, IOException {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof UtilisateurConnecte connecte)) {
      chaine.doFilter(requete, reponse);
      return;
    }
    var compte = utilisateurs.findById(connecte.id()).filter(Utilisateur::isActif);
    if (compte.isEmpty()) {
      fermerLaSession(requete);
      if (CONNEXION.equals(requete.getMethod() + " " + requete.getRequestURI())) {
        chaine.doFilter(requete, reponse);
        return;
      }
      ReponseErreurJson.ecrire(reponse, HttpServletResponse.SC_UNAUTHORIZED, "NON_AUTHENTIFIE",
          MessagesSecurite.NON_AUTHENTIFIE);
      return;
    }
    var aJour = new UtilisateurConnecte(connecte.id(), compte.get().getLogin(), compte.get().getRole(),
        compte.get().getEtudiantId());
    if (!aJour.equals(connecte)) {
      var contexte = SecurityContextHolder.createEmptyContext();
      contexte.setAuthentication(jeton(aJour));
      SecurityContextHolder.setContext(contexte);
      depotDeContexte.saveContext(contexte, requete, reponse);
    }
    chaine.doFilter(requete, reponse);
  }

  private static void fermerLaSession(HttpServletRequest requete) {
    SecurityContextHolder.clearContext();
    var session = requete.getSession(false);
    if (session != null) {
      session.invalidate();
    }
  }
}
