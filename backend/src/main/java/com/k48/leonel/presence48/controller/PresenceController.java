package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.request.PresenceRequete;
import com.k48.leonel.presence48.dto.response.PresenceReponse;
import com.k48.leonel.presence48.service.PresenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Opération imposée : présence par code (EF3, SF-3). */
@RestController
@Tag(name = "présence", description = "Marquage de la présence par code (#105 — EF3, SF-3)")
public class PresenceController {

  private final PresenceService presences;

  public PresenceController(PresenceService presences) {
    this.presences = presences;
  }

  /** Opération imposée (EF3) : publique sans session, identité vérifiée si l'appelant est connecté. */
  @Operation(operationId = "marquerPresence", summary = "L'étudiant marque sa présence avec le code (EF3)",
      description = "Opération imposée, publique (RG22). Ordre des contrôles : blocage (RG4) → code (RG2) → "
          + "clôture (RG18) → expiration (RG1) → promotion (RG19) → unicité (RG3). Une réussite remet le "
          + "compteur d'erreurs à zéro et peut déclencher un tirage de relecteur (RG7).",
      responses = {
          @ApiResponse(responseCode = "201", description = "Présence enregistrée",
              content = @Content(schema = @Schema(implementation = PresenceReponse.class))),
          @ApiResponse(responseCode = "400", description = "Code inconnu ou étudiant hors promotion",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class),
                  examples = @ExampleObject(value = "{\"code\":\"CODE_INCONNU\","
                      + "\"message\":\"Ce code ne correspond à aucune session.\"}"))),
          @ApiResponse(responseCode = "409", description = "Déjà présent ou session clôturée",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class),
                  examples = @ExampleObject(value = "{\"code\":\"DEJA_PRESENT\","
                      + "\"message\":\"Votre présence est déjà enregistrée.\"}"))),
          @ApiResponse(responseCode = "410", description = "Code expiré (RG1)",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class),
                  examples = @ExampleObject(value = "{\"code\":\"CODE_EXPIRE\","
                      + "\"message\":\"Le code a expiré : demandez au formateur de vous ajouter.\"}"))),
          @ApiResponse(responseCode = "429", description = "Bloqué après 5 codes erronés (RG4)",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class),
                  examples = @ExampleObject(value = "{\"code\":\"TROP_DE_TENTATIVES\","
                      + "\"message\":\"Trop de codes erronés : réessayez dans 2 minutes.\"}"))),
      })
  @PostMapping("/api/presences")
  @ResponseStatus(HttpStatus.CREATED)
  public PresenceReponse marquer(@Valid @RequestBody PresenceRequete corps) {
    return presences.marquer(corps.code(), corps.etudiantId());
  }
}
