package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.request.DepotExerciceRequete;
import com.k48.leonel.presence48.dto.response.ExerciceAuteurReponse;
import com.k48.leonel.presence48.dto.response.ExerciceDeposeReponse;
import com.k48.leonel.presence48.service.ExerciceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Dépôt d'exercice (imposé, EF6) et exercices de l'auteur (SF-14, protégé). */
@RestController
@Tag(name = "exercice", description = "Dépôt et consultation des exercices (#105 — EF6, SF-6, SF-14)")
public class ExerciceController {

  private final ExerciceService exercices;

  public ExerciceController(ExerciceService exercices) {
    this.exercices = exercices;
  }

  /** Opération imposée (EF6) : publique sans session, identité vérifiée si l'appelant est connecté. */
  @Operation(operationId = "deposerExercice", summary = "L'étudiant dépose le lien de son exercice (EF6)",
      description = "Opération imposée, publique (RG22). Le dépôt reste possible après l'expiration du code "
          + "(RG12) jusqu'à la clôture (RG18) ; un seul exercice par étudiant et session (RG13).",
      responses = {
          @ApiResponse(responseCode = "201", description = "Exercice déposé, statut initial",
              content = @Content(schema = @Schema(implementation = ExerciceDeposeReponse.class))),
          @ApiResponse(responseCode = "400", description = "Lien invalide (RG17), session ou étudiant inconnu",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class),
                  examples = @ExampleObject(value = "{\"code\":\"LIEN_INVALIDE\","
                      + "\"message\":\"Le lien doit être une adresse http ou https complète.\"}"))),
          @ApiResponse(responseCode = "409", description = "Exercice déjà déposé (RG13) ou session clôturée (RG18)",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class),
                  examples = @ExampleObject(value = "{\"code\":\"EXERCICE_DEJA_DEPOSE\","
                      + "\"message\":\"Vous avez déjà déposé un exercice pour cette session.\"}"))),
      })
  @PostMapping("/api/exercices")
  @ResponseStatus(HttpStatus.CREATED)
  public ExerciceDeposeReponse deposer(@Valid @RequestBody DepotExerciceRequete corps) {
    return exercices.deposer(corps.sessionId(), corps.etudiantId(), corps.lien());
  }

  /** SF-14 (complément, protégé) : note retenue et provisoire (contrat 2.1), sans les relecteurs. */
  @Operation(summary = "Exercices d'un étudiant, avec sa note retenue (SF-14)",
      description = "Route protégée (RG25) : l'étudiant connecté ne peut lire que les siennes (403 sinon, "
          + "identité contrôlée avant l'existence, #109) ; formateur et admin dans leurs promotions (RG26). "
          + "La note retenue est la moyenne des deux relecteurs (RG16 v3), provisoire tant qu'un seul a rendu (RG31).",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "200", description = "Exercices, du plus récent",
              content = @Content(schema = @Schema(implementation = ExerciceAuteurReponse.class))),
          @ApiResponse(responseCode = "401", description = "Non connecté",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class))),
          @ApiResponse(responseCode = "403", description = "Étudiant autre que soi, ou promotion hors des droits",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class))),
          @ApiResponse(responseCode = "404", description = "Étudiant inconnu (formateur/admin seulement)",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class))),
      })
  @GetMapping("/api/etudiants/{etudiantId}/exercices")
  public List<ExerciceAuteurReponse> mesExercices(@PathVariable Long etudiantId) {
    return exercices.exercicesDe(etudiantId);
  }
}
