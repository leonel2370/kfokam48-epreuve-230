package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.response.EtudiantReponse;
import com.k48.leonel.presence48.dto.response.PromotionReponse;
import com.k48.leonel.presence48.service.ReferentielService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** EF1 : choisir sa promotion puis son nom (routes publiques, RG22). */
@RestController
@RequestMapping("/api/promotions")
@Tag(name = "référentiel", description = "Promotions et étudiants (#105 — EF1, RG22)")
public class ReferentielController {

  private final ReferentielService referentiel;

  public ReferentielController(ReferentielService referentiel) {
    this.referentiel = referentiel;
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
              content = @Content(schema = @Schema(implementation = com.k48.leonel.presence48.exception.ErreurReponse.class))),
      })
  @GetMapping("/{promotionId}/etudiants")
  public List<EtudiantReponse> etudiants(@PathVariable Long promotionId) {
    return referentiel.etudiantsDeLaPromotion(promotionId);
  }
}
