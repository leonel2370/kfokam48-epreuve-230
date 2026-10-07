package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.request.PromotionEcriture;
import com.k48.leonel.presence48.dto.request.RattachementFormateursRequete;
import com.k48.leonel.presence48.dto.response.EtudiantReponse;
import com.k48.leonel.presence48.dto.response.FicheEtudiantReponse;
import com.k48.leonel.presence48.dto.response.PromotionReponse;
import com.k48.leonel.presence48.dto.response.UtilisateurReponse;
import com.k48.leonel.presence48.exception.ErreurReponse;
import com.k48.leonel.presence48.service.ReferentielAdminService;
import com.k48.leonel.presence48.service.ReferentielService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EF1 : choisir sa promotion puis son nom (routes publiques, RG22).
 * #61 / SF-21 / EF22 : le référentiel devient gérable — l'ADMIN (RG25) crée, renomme et supprime
 * les promotions (RG28) et rattache les formateurs (RG26). Les fiches étudiants sont dans
 * {@link EtudiantAdminController}.
 */
@RestController
@RequestMapping("/api/promotions")
@Tag(name = "référentiel", description = "Promotions et étudiants (#105 — EF1, RG22)")
public class ReferentielController {

  private final ReferentielService referentiel;
  private final ReferentielAdminService admin;

  public ReferentielController(ReferentielService referentiel, ReferentielAdminService admin) {
    this.referentiel = referentiel;
    this.admin = admin;
  }

  @Operation(operationId = "listerPromotions", summary = "Liste des promotions (EF1)",
      description = "Publique (RG22) : sert à choisir sa promotion à l'écran ou à appeler les opérations imposées.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Promotions",
              content = @Content(schema = @Schema(implementation = PromotionReponse.class))),
      })
  @GetMapping
  public List<PromotionReponse> promotions() {
    return referentiel.promotions();
  }

  @Operation(operationId = "listerEtudiantsPromotion", summary = "Étudiants d'une promotion (EF1)",
      description = "Publique (RG22), triés par nom ; les fiches désactivées sont masquées (RG28).",
      responses = {
          @ApiResponse(responseCode = "200", description = "Étudiants de la promotion",
              content = @Content(schema = @Schema(implementation = EtudiantReponse.class))),
          @ApiResponse(responseCode = "404", description = "Promotion inconnue",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
      })
  @GetMapping("/{promotionId}/etudiants")
  public List<EtudiantReponse> etudiants(@PathVariable Long promotionId) {
    return referentiel.etudiantsDeLaPromotion(promotionId);
  }

  // --- #134 / contrat 2.8 : lectures de gestion ---

  @Operation(operationId = "listerFormateursDeLaPromotion",
      summary = "Formateurs rattachés à une promotion (SF-21, #134)",
      description = "Réservé à l'ADMIN (RG25). Rend la liste que PUT …/formateurs remplace (RG26).",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "200", description = "Comptes FORMATEUR rattachés, triés par nom affiché",
              content = @Content(array = @ArraySchema(schema = @Schema(implementation = UtilisateurReponse.class)))),
          @ApiResponse(responseCode = "401", description = "Non connecté",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "403", description = "Pas le rôle ADMIN",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "404", description = "PROMOTION_INCONNUE",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
      })
  @GetMapping("/{promotionId}/formateurs")
  @PreAuthorize("hasRole('ADMIN')")
  public List<UtilisateurReponse> formateurs(@PathVariable Long promotionId) {
    return admin.formateursDe(promotionId);
  }

  @Operation(operationId = "listerFichesDeLaPromotion",
      summary = "Fiches étudiants d'une promotion pour la gestion (SF-22, #134)",
      description = "ADMIN, ou FORMATEUR rattaché à la promotion (RG26). Toutes les fiches, désactivées comprises "
          + "(RG28), avec leur état et le compte lié. La liste publique …/etudiants reste limitée aux fiches actives.",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "200", description = "Fiches triées par nom",
              content = @Content(array = @ArraySchema(schema = @Schema(implementation = FicheEtudiantReponse.class)))),
          @ApiResponse(responseCode = "401", description = "Non connecté",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "403", description = "Ni ADMIN ni formateur de cette promotion",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "404", description = "PROMOTION_INCONNUE",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
      })
  @GetMapping("/{promotionId}/fiches")
  public List<FicheEtudiantReponse> fiches(@PathVariable Long promotionId) {
    return admin.fichesDe(promotionId);
  }

  // --- #61 / SF-21, EF22 : écritures, réservées à l'ADMIN (RG25) ---

  @Operation(operationId = "creerPromotion", summary = "Créer une promotion (SF-21, #61)",
      description = "Réservé à l'ADMIN (RG25). Le nom est UNIQUE (V1) : 409 CONFLIT s'il est déjà pris.",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "201", description = "Promotion créée",
              content = @Content(schema = @Schema(implementation = PromotionReponse.class))),
          @ApiResponse(responseCode = "400", description = "Champ manquant",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "401", description = "Non connecté",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "403", description = "Pas le rôle ADMIN",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "409", description = "CONFLIT — nom déjà pris",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
      })
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PromotionReponse> creer(@Valid @RequestBody PromotionEcriture requete) {
    var creee = admin.creerPromotion(requete.nom());
    return ResponseEntity.status(HttpStatus.CREATED).body(creee);
  }

  @Operation(operationId = "modifierPromotion", summary = "Renommer une promotion (SF-21, #61)",
      description = "Réservé à l'ADMIN (RG25). Le nom reste unique : 409 CONFLIT s'il est déjà pris ailleurs.",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "200", description = "Promotion renommée",
              content = @Content(schema = @Schema(implementation = PromotionReponse.class))),
          @ApiResponse(responseCode = "400", description = "Champ manquant",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "401", description = "Non connecté",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "403", description = "Pas le rôle ADMIN",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "404", description = "PROMOTION_INCONNUE",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "409", description = "CONFLIT — nom déjà pris",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
      })
  @PutMapping("/{promotionId}")
  @PreAuthorize("hasRole('ADMIN')")
  public PromotionReponse renommer(@PathVariable Long promotionId,
      @Valid @RequestBody PromotionEcriture requete) {
    return admin.modifierPromotion(promotionId, requete.nom());
  }

  @Operation(operationId = "supprimerPromotion", summary = "Supprimer une promotion vide (RG28, #61)",
      description = "Réservé à l'ADMIN. Refusé (409 SUPPRESSION_IMPOSSIBLE) si la promotion a des "
          + "étudiants ou des sessions ; les rattachements de formateurs sont purgés avec elle.",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "204", description = "Promotion supprimée"),
          @ApiResponse(responseCode = "401", description = "Non connecté",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "403", description = "Pas le rôle ADMIN",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "404", description = "PROMOTION_INCONNUE",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "409", description = "SUPPRESSION_IMPOSSIBLE — étudiants ou sessions",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
      })
  @DeleteMapping("/{promotionId}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> supprimer(@PathVariable Long promotionId) {
    admin.supprimerPromotion(promotionId);
    return ResponseEntity.noContent().build();
  }

  @Operation(operationId = "rattacherFormateurs", summary = "Définir les formateurs d'une promotion (RG26, #61)",
      description = "Réservé à l'ADMIN. Remplace la liste des formateurs rattachés ; seuls des comptes "
          + "FORMATEUR sont admis (400 CHAMP_MANQUANT sinon). Un formateur rattaché accède aux sessions "
          + "et fiches de la promotion (RG26).",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "204", description = "Rattachements enregistrés"),
          @ApiResponse(responseCode = "400", description = "CHAMP_MANQUANT — compte absent ou non formateur",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "401", description = "Non connecté",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "403", description = "Pas le rôle ADMIN",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
          @ApiResponse(responseCode = "404", description = "PROMOTION_INCONNUE",
              content = @Content(schema = @Schema(implementation = ErreurReponse.class))),
      })
  @PutMapping("/{promotionId}/formateurs")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> rattacherFormateurs(@PathVariable Long promotionId,
      @Valid @RequestBody RattachementFormateursRequete requete) {
    admin.rattacherFormateurs(promotionId, requete.utilisateurIds());
    return ResponseEntity.noContent().build();
  }
}
