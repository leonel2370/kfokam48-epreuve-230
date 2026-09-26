package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.request.ChangementMotDePasseRequete;
import com.k48.leonel.presence48.dto.request.ConnexionRequete;
import com.k48.leonel.presence48.dto.response.ProfilReponse;
import com.k48.leonel.presence48.securite.UtilisateurConnecte;
import com.k48.leonel.presence48.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
@Tag(name = "sécurité", description = "Connexion, profil, mot de passe (#105 — SF-15, SF-17, SF-18)")
public class AuthController {

  private final AuthService auth;
  private final SecurityContextRepository depotDeContexte;

  public AuthController(AuthService auth, SecurityContextRepository depotDeContexte) {
    this.auth = auth;
    this.depotDeContexte = depotDeContexte;
  }

  @Operation(summary = "Se connecter avec un identifiant et un mot de passe (EF15)",
      description = "Publique (RG22). Ouvre la session serveur (cookie JSESSIONID, HttpOnly, SameSite=Strict) "
          + "et renvoie le profil. Cinq échecs consécutifs bloquent le compte 2 minutes (RG24).",
      responses = {
          @ApiResponse(responseCode = "200", description = "Connecté, profil renvoyé",
              content = @Content(schema = @Schema(implementation = ProfilReponse.class))),
          @ApiResponse(responseCode = "400", description = "Identifiants invalides",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class),
                  examples = @ExampleObject(value = "{\"code\":\"IDENTIFIANTS_INVALIDES\","
                      + "\"message\":\"Identifiant ou mot de passe incorrect.\"}"))),
          @ApiResponse(responseCode = "403", description = "Compte désactivé",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class))),
          @ApiResponse(responseCode = "429", description = "Bloqué après 5 échecs (RG24)",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class))),
      })
  @PostMapping("/auth/login")
  public ProfilReponse connecter(@Valid @RequestBody ConnexionRequete corps, HttpServletRequest requete,
      HttpServletResponse reponse) {
    var u = auth.authentifier(corps.login(), corps.motDePasse());
    requete.getSession(true);
    // Nouvel identifiant de session : protection contre la fixation de session
    requete.changeSessionId();
    var jeton = UsernamePasswordAuthenticationToken.authenticated(u, null,
        List.of(new SimpleGrantedAuthority("ROLE_" + u.role().name())));
    var contexte = SecurityContextHolder.createEmptyContext();
    contexte.setAuthentication(jeton);
    SecurityContextHolder.setContext(contexte);
    depotDeContexte.saveContext(contexte, requete, reponse);
    return auth.profil(u.id());
  }

  @Operation(summary = "Profil de l'utilisateur connecté (SF-17)",
      description = "Route protégée (RG25) : nom affiché, rôle, promotions, et doitChangerMotDePasse (RG23).",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "200", description = "Profil connecté",
              content = @Content(schema = @Schema(implementation = ProfilReponse.class))),
          @ApiResponse(responseCode = "401", description = "Non connecté",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class))),
      })
  @GetMapping("/moi")
  public ProfilReponse moi(@AuthenticationPrincipal UtilisateurConnecte u) {
    return auth.profil(u.id());
  }

  @Operation(summary = "Changer son mot de passe (SF-18)",
      description = "Route protégée (RG25). Nouveau mot de passe de 8 caractères minimum (RG24) ; autorisé "
          + "même quand un changement est imposé (RG23).",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "204", description = "Mot de passe changé"),
          @ApiResponse(responseCode = "400", description = "Nouveau mot de passe trop faible (RG24)",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class),
                  examples = @ExampleObject(value = "{\"code\":\"MOT_DE_PASSE_TROP_FAIBLE\","
                      + "\"message\":\"Le mot de passe doit faire au moins 8 caractères.\"}"))),
          @ApiResponse(responseCode = "401", description = "Non connecté",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class))),
      })
  @PutMapping("/moi/mot-de-passe")
  public ResponseEntity<Void> changerMotDePasse(@AuthenticationPrincipal UtilisateurConnecte u,
      @Valid @RequestBody ChangementMotDePasseRequete corps) {
    auth.changerMotDePasse(u.id(), corps.ancien(), corps.nouveau());
    return ResponseEntity.noContent().build();
  }
}
