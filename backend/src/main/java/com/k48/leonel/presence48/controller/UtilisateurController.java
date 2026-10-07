package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.request.CreationUtilisateurRequete;
import com.k48.leonel.presence48.dto.request.ModificationUtilisateurRequete;
import com.k48.leonel.presence48.dto.request.ReinitialisationMotDePasseRequete;
import com.k48.leonel.presence48.dto.response.PageUtilisateursReponse;
import com.k48.leonel.presence48.dto.response.UtilisateurReponse;
import com.k48.leonel.presence48.entity.Role;
import com.k48.leonel.presence48.exception.ErreurReponse;
import com.k48.leonel.presence48.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * #60 / SF-20 / EF21 : gestion des comptes par l'ADMIN (RG25, RG27, RG28).
 * Route protégée : exige une session (401) et le rôle ADMIN (403 ACCES_REFUSE).
 */
@RestController
@RequestMapping("/api/utilisateurs")
@Tag(name = "administration", description = "CRUD des comptes (#60 — SF-20, EF21, RG27, RG28)")
public class UtilisateurController {

  /** Bornes de pagination du contrat (#132) : hors bornes, 400 CHAMP_MANQUANT. */
  private static final String TAILLE_PAR_DEFAUT = "20";
  private static final long TAILLE_MAX = 100;

  private final UtilisateurService service;

  public UtilisateurController(UtilisateurService service) {
    this.service = service;
  }

  @Operation(summary = "Lister les comptes (SF-20, #60)",
      description = "Réservé à l'ADMIN (RG25). Page de comptes, jamais de hash de mot de passe.",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "200", description = "Page de comptes",
              content = @Content(schema = @Schema(implementation = PageUtilisateursReponse.class))),
          @ApiResponse(responseCode = "400", description = "Page négative, taille hors de 1 à 100 ou rôle inconnu",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "401", description = "Non connecté",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "403", description = "Pas le rôle ADMIN",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
      })
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public PageUtilisateursReponse lister(@RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = TAILLE_PAR_DEFAUT) @Min(1) @Max(TAILLE_MAX) int size,
      @Parameter(description = "Ne garder que les comptes de ce rôle (#134)")
      @RequestParam(required = false) Role role) {
    return service.lister(page, size, role);
  }

  @Operation(summary = "Créer un compte (EF21, #60)",
      description = "Réservé à l'ADMIN. Mot de passe initial provisoire : à changer à la première connexion "
          + "(RG23), 8 caractères minimum (RG24). Login unique (RG27 → 409).",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "201", description = "Compte créé",
              content = @Content(schema = @Schema(implementation = UtilisateurReponse.class))),
          @ApiResponse(responseCode = "400", description = "Champ manquant ou fiche étudiant absente",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "409", description = "LOGIN_DEJA_UTILISE (RG27)",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class),
                  examples = @ExampleObject(value = "{\"code\":\"LOGIN_DEJA_UTILISE\","
                      + "\"message\":\"Cet identifiant est déjà pris.\"}"))),
      })
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<UtilisateurReponse> creer(@Valid @RequestBody CreationUtilisateurRequete requete) {
    var cree = service.creer(requete);
    return ResponseEntity.status(201).body(cree);
  }

  @Operation(summary = "Lire un compte (SF-20, #60)",
      description = "Réservé à l'ADMIN (RG25) : login, nom affiché, rôle, fiche étudiant liée, activation "
          + "et drapeau RG23 — jamais de hash de mot de passe.",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "200", description = "Compte",
              content = @Content(schema = @Schema(implementation = UtilisateurReponse.class))),
          @ApiResponse(responseCode = "404", description = "UTILISATEUR_INTROUVABLE",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
      })
  @GetMapping("/{utilisateurId}")
  @PreAuthorize("hasRole('ADMIN')")
  public UtilisateurReponse lire(@PathVariable Long utilisateurId) {
    return service.lire(utilisateurId);
  }

  @Operation(summary = "Modifier nom, rôle, fiche liée ou activation (SF-20, #60)",
      description = "Réservé à l'ADMIN (RG25). Le rôle ETUDIANT exige la fiche liée (HYP-15) ; "
          + "409 si la fiche est déjà liée à un autre compte.",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "200", description = "Compte modifié",
              content = @Content(schema = @Schema(implementation = UtilisateurReponse.class))),
          @ApiResponse(responseCode = "404", description = "UTILISATEUR_INTROUVABLE",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "409", description = "Fiche étudiant déjà liée",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
      })
  @PutMapping("/{utilisateurId}")
  @PreAuthorize("hasRole('ADMIN')")
  public UtilisateurReponse modifier(@PathVariable Long utilisateurId,
      @Valid @RequestBody ModificationUtilisateurRequete requete) {
    return service.modifier(utilisateurId, requete);
  }

  @Operation(summary = "Désactiver un compte (RG28, #60) — jamais de suppression physique",
      description = "Réservé à l'ADMIN. Le compte désactivé ne peut plus se connecter (403 COMPTE_DESACTIVE) ; "
          + "le dernier admin actif est protégé (409 SUPPRESSION_IMPOSSIBLE).",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "204", description = "Compte désactivé"),
          @ApiResponse(responseCode = "409", description = "Dernier admin actif (SUPPRESSION_IMPOSSIBLE)",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
      })
  @DeleteMapping("/{utilisateurId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> desactiver(@PathVariable Long utilisateurId) {
    service.desactiver(utilisateurId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Réinitialiser le mot de passe d'un compte (SF-20, #60)",
      description = "Réservé à l'ADMIN. Mot de passe provisoire : à changer à la connexion suivante (RG23) ; "
          + "remet aussi le compteur d'échecs à zéro (RG24).",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "204", description = "Mot de passe réinitialisé"),
          @ApiResponse(responseCode = "400", description = "Mot de passe trop faible (RG24)",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "404", description = "UTILISATEUR_INTROUVABLE",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
      })
  @PostMapping("/{utilisateurId}/reinitialiser-mot-de-passe")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> reinitialiser(@PathVariable Long utilisateurId,
      @Valid @RequestBody ReinitialisationMotDePasseRequete requete) {
    service.reinitialiserMotDePasse(utilisateurId, requete.motDePasseInitial());
    return ResponseEntity.noContent().build();
  }
}
