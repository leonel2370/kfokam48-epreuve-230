package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.request.EtudiantEcriture;
import com.k48.leonel.presence48.dto.response.EtudiantReponse;
import com.k48.leonel.presence48.exception.ErreurReponse;
import com.k48.leonel.presence48.service.ReferentielAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * #61 / SF-22 / EF23 : gestion des fiches étudiants par l'ADMIN et le FORMATEUR de la promotion
 * (RG26) — pas de @PreAuthorize FORMATEUR : le service vérifie le rattachement à LA promotion
 * visée via {@link com.k48.leonel.presence48.securite.ControleAcces#verifierGestionPromotionStrict}.
 */
@RestController
@RequestMapping("/api/etudiants")
@Tag(name = "administration", description = "CRUD des fiches étudiants (#61 — SF-22, EF23, RG26, RG28)")
public class EtudiantAdminController {

  private final ReferentielAdminService admin;

  public EtudiantAdminController(ReferentielAdminService admin) {
    this.admin = admin;
  }

  @Operation(operationId = "creerEtudiant", summary = "Créer une fiche étudiant (SF-22, #61)",
      description = "ADMIN, ou FORMATEUR rattaché à la promotion visée (RG26) : 403 hors promotion. "
          + "La fiche est sans compte : l'étudiant s'identifiera par son nom (HYP-2).",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "201", description = "Fiche créée",
              content = @Content(schema = @Schema(implementation = EtudiantReponse.class))),
          @ApiResponse(responseCode = "400", description = "Champ manquant ou promotion inconnue",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "401", description = "Non connecté",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "403", description = "FORMATEUR hors promotion (ACCES_REFUSE)",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
      })
  @PostMapping
  public ResponseEntity<EtudiantReponse> creer(@Valid @RequestBody EtudiantEcriture requete) {
    var creee = admin.creerEtudiant(requete.promotionId(), requete.nom());
    return ResponseEntity.status(HttpStatus.CREATED).body(creee);
  }

  @Operation(operationId = "modifierEtudiant", summary = "Modifier une fiche étudiant (SF-22, #61)",
      description = "ADMIN, ou FORMATEUR rattaché à la promotion visée (RG26). Le nom et la promotion "
          + "peuvent changer (déplacement d'un étudiant).",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "200", description = "Fiche modifiée",
              content = @Content(schema = @Schema(implementation = EtudiantReponse.class))),
          @ApiResponse(responseCode = "400", description = "Champ manquant ou promotion inconnue",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "401", description = "Non connecté",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "403", description = "FORMATEUR hors promotion (ACCES_REFUSE)",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "404", description = "ETUDIANT_INCONNU",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
      })
  @PutMapping("/{etudiantId}")
  public ResponseEntity<EtudiantReponse> modifier(@PathVariable Long etudiantId,
      @Valid @RequestBody EtudiantEcriture requete) {
    return ResponseEntity.ok(admin.modifierEtudiant(etudiantId, requete.nom(), requete.promotionId()));
  }

  @Operation(operationId = "desactiverEtudiant", summary = "Supprimer ou désactiver une fiche (RG28, #61)",
      description = "ADMIN, ou FORMATEUR rattaché à la promotion (RG26). Sans historique : suppression "
          + "physique (refusée si la fiche est liée à un compte) ; sinon désactivation (RG28) — présences "
          + "et notes restent au tableau et la liste de sélection ne montre plus la fiche.",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "204", description = "Fiche supprimée ou désactivée"),
          @ApiResponse(responseCode = "401", description = "Non connecté",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "403", description = "FORMATEUR hors promotion (ACCES_REFUSE)",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "404", description = "ETUDIANT_INCONNU",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "409", description = "SUPPRESSION_IMPOSSIBLE — fiche liée à un compte",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
      })
  @DeleteMapping("/{etudiantId}")
  public ResponseEntity<Void> supprimer(@PathVariable Long etudiantId) {
    admin.supprimerOuDesactiverEtudiant(etudiantId);
    return ResponseEntity.noContent().build();
  }
}
