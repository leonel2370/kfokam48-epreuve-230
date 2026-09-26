package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.request.OuvertureSessionRequete;
import com.k48.leonel.presence48.dto.response.SessionOuverteReponse;
import com.k48.leonel.presence48.dto.response.SessionReponse;
import com.k48.leonel.presence48.service.SessionService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Opérations sur les sessions : ouverture (imposée, EF2) et liste (SF-4, protégée). */
@RestController
@RequestMapping("/api/sessions")
@Tag(name = "session", description = "Sessions de cours : ouverture, liste (#105 — SF-2, SF-4)")
public class SessionController {

  private final SessionService sessions;

  public SessionController(SessionService sessions) {
    this.sessions = sessions;
  }

  /** Opération imposée (EF2) : publique sans session, contrôlée si l'appelant est connecté. */
  @Operation(operationId = "ouvrirSession", summary = "Le formateur ouvre une session et obtient un code (EF2)",
      description = "Opération imposée, publique (RG22). Le code expire 15 minutes après l'ouverture (RG1) et "
          + "est unique parmi les codes non expirés (RG20).",
      responses = {
          @ApiResponse(responseCode = "201", description = "Session ouverte",
              content = @Content(schema = @Schema(implementation = SessionOuverteReponse.class))),
          @ApiResponse(responseCode = "400", description = "Champ manquant ou promotion inconnue",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class),
                  examples = @ExampleObject(value = "{\"code\":\"PROMOTION_INCONNUE\","
                      + "\"message\":\"Cette promotion n'existe pas.\"}"))),
      })
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SessionOuverteReponse ouvrir(@Valid @RequestBody OuvertureSessionRequete corps) {
    return sessions.ouvrir(corps.titre(), corps.promotionId());
  }

  /** SF-4 (protégée) : liste des sessions d'une promotion ; le code est masqué aux étudiants (#98). */
  @Operation(summary = "Sessions d'une promotion (SF-4)",
      description = "Route protégée (RG25) : l'étudiant ne voit pas les codes de présence (#98) ; "
          + "le formateur ne voit que ses promotions (RG26).",
      security = @SecurityRequirement(name = "cookieAuth"),
      responses = {
          @ApiResponse(responseCode = "200", description = "Sessions de la promotion, de la plus récente",
              content = @Content(schema = @Schema(implementation = SessionReponse.class))),
          @ApiResponse(responseCode = "400", description = "Paramètre promotionId manquant ou non numérique",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class))),
          @ApiResponse(responseCode = "401", description = "Non connecté",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class),
                  examples = @ExampleObject(value = "{\"code\":\"NON_AUTHENTIFIE\","
                      + "\"message\":\"Connectez-vous pour continuer.\"}"))),
          @ApiResponse(responseCode = "403", description = "Promotion hors des droits de l'appelant",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class))),
      })
  @GetMapping
  public List<SessionReponse> lister(@RequestParam Long promotionId) {
    return sessions.lister(promotionId);
  }
}
