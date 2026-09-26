package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.request.RenduRelectureRequete;
import com.k48.leonel.presence48.dto.response.RelectureRelecteurReponse;
import com.k48.leonel.presence48.service.RelectureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Relectures : liste du relecteur (SF-8) et envoi définitif de la note (imposé, EF9). */
@RestController
@Tag(name = "relecture", description = "Relecture par les pairs (#105 — EF8, EF9, SF-8, SF-9)")
public class RelectureController {

  private final RelectureService relectures;

  public RelectureController(RelectureService relectures) {
    this.relectures = relectures;
  }

  /** SF-8 (complément, protégé) : relectures assignées à l'étudiant, sans l'auteur. */
  @Operation(summary = "Relectures assignées à un étudiant (SF-8)",
      description = "Route protégée (RG25) : l'étudiant connecté ne voit que les siennes ; jamais l'identité "
          + "de l'auteur (HYP-10). Filtre `statut` : A_FAIRE (défaut) ou RENDUE.",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "200", description = "Relectures du relecteur",
              content = @Content(schema = @Schema(implementation = RelectureRelecteurReponse.class))),
          @ApiResponse(responseCode = "401", description = "Non connecté",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class))),
          @ApiResponse(responseCode = "403", description = "Étudiant autre que soi, ou promotion hors des droits",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class))),
      })
  @GetMapping("/api/etudiants/{etudiantId}/relectures")
  public List<RelectureRelecteurReponse> lister(@PathVariable Long etudiantId,
      @RequestParam(required = false) RelectureService.Filtre statut) {
    return relectures.lister(etudiantId, statut);
  }

  /** Opération imposée (EF9) : identité déclarée par l'en-tête X-Etudiant-Id (HYP-2). */
  @Operation(operationId = "rendreRelecture", summary = "Le relecteur rend sa note définitive (EF9)",
      description = "Opération imposée, publique (RG22). Note entière de 0 à 20 (RG9) ; jamais son propre "
          + "exercice (RG5) ; l'exercice passe RELU quand les deux relecteurs ont rendu (RG6 v3, RG31). "
          + "Une relecture rendue ne se modifie pas (RG10, contradiction Q10/Q15 tranchée en §7.1 du cahier).",
      responses = {
          @ApiResponse(responseCode = "200", description = "Note enregistrée, définitive"),
          @ApiResponse(responseCode = "400", description = "Note hors 0–20 ou non entière, ou champ manquant",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class),
                  examples = @ExampleObject(value = "{\"code\":\"NOTE_INVALIDE\","
                      + "\"message\":\"La note doit être un entier de 0 à 20.\"}"))),
          @ApiResponse(responseCode = "403", description = "Relecture de son propre exercice (RG5) ou relecteur non assigné",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class),
                  examples = @ExampleObject(value = "{\"code\":\"AUTO_RELECTURE\","
                      + "\"message\":\"Vous ne pouvez pas relire votre exercice.\"}"))),
          @ApiResponse(responseCode = "404", description = "Relecture inconnue",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class))),
          @ApiResponse(responseCode = "409", description = "Relecture déjà rendue (RG10) ou session clôturée (RG18)",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class),
                  examples = @ExampleObject(value = "{\"code\":\"RELECTURE_DEJA_RENDUE\","
                      + "\"message\":\"Cette relecture est déjà rendue.\"}"))),
      })
  @PostMapping("/api/relectures/{id}")
  public void rendre(@PathVariable Long id, @RequestHeader("X-Etudiant-Id") Long etudiantId,
      @Valid @RequestBody RenduRelectureRequete corps) {
    relectures.rendre(id, etudiantId, corps.note(), corps.commentaire());
  }
}
