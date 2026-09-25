package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.request.ChangementMotDePasseRequete;
import com.k48.leonel.presence48.dto.request.ConnexionRequete;
import com.k48.leonel.presence48.dto.response.ProfilReponse;
import com.k48.leonel.presence48.securite.UtilisateurConnecte;
import com.k48.leonel.presence48.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Connexion, profil connecté et mot de passe (SF-15, SF-17, SF-18). La déconnexion est gérée par Spring Security. */
@RestController
@RequestMapping("/api")
public class AuthController {

  private final AuthService auth;
  private final SecurityContextRepository depotDeContexte;

  public AuthController(AuthService auth, SecurityContextRepository depotDeContexte) {
    this.auth = auth;
    this.depotDeContexte = depotDeContexte;
  }

  @PostMapping("/auth/login")
  public ProfilReponse connecter(@Valid @RequestBody ConnexionRequete corps, HttpServletRequest requete,
      HttpServletResponse reponse) {
    UtilisateurConnecte u = auth.authentifier(corps.login(), corps.motDePasse());
    requete.getSession(true);
    requete.changeSessionId();   // protection contre la fixation de session
    var jeton = UsernamePasswordAuthenticationToken.authenticated(u, null,
        List.of(new SimpleGrantedAuthority("ROLE_" + u.role().name())));
    SecurityContext contexte = SecurityContextHolder.createEmptyContext();
    contexte.setAuthentication(jeton);
    SecurityContextHolder.setContext(contexte);
    depotDeContexte.saveContext(contexte, requete, reponse);
    return auth.profil(u.id());
  }

  @GetMapping("/moi")
  public ProfilReponse moi(@AuthenticationPrincipal UtilisateurConnecte u) {
    return auth.profil(u.id());
  }

  @PutMapping("/moi/mot-de-passe")
  public ResponseEntity<Void> changerMotDePasse(@AuthenticationPrincipal UtilisateurConnecte u,
      @Valid @RequestBody ChangementMotDePasseRequete corps) {
    auth.changerMotDePasse(u.id(), corps.ancien(), corps.nouveau());
    return ResponseEntity.noContent().build();
  }
}
