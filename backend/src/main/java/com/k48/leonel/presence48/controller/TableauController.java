package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.response.LigneTableauReponse;
import com.k48.leonel.presence48.service.TableauService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "tableau", description = "Tableau de la promotion (#105 — EF10, Q16)")
public class TableauController {

  private final TableauService tableau;

  public TableauController(TableauService tableau) {
    this.tableau = tableau;
  }

    @Operation(operationId = "consulterTableau", summary = "Tableau d'une promotion, calculé par le serveur (EF10)",
      description = "Opération imposée, publique (RG22). Une ligne par étudiant : présences, exercices déposés, "
          + "moyenne des notes retenues (null sans note, RG16 v3), relectures en attente (Q11). Aucun recalcul "
          + "côté client (F3).",
      responses = {
          @ApiResponse(responseCode = "200", description = "Lignes du tableau",
              content = @Content(schema = @Schema(implementation = LigneTableauReponse.class))),
          @ApiResponse(responseCode = "400", description = "Paramètre promotionId manquant ou non numérique",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class))),
          @ApiResponse(responseCode = "404", description = "Promotion inconnue",
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class),
                  examples = @ExampleObject(value = "{\"code\":\"PROMOTION_INCONNUE\","
                      + "\"message\":\"Cette promotion n'existe pas.\"}"))),
      })
  @GetMapping("/api/tableau")
  public List<LigneTableauReponse> tableau(@RequestParam Long promotionId) {
    return tableau.tableau(promotionId);
  }
}
