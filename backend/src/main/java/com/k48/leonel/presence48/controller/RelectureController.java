package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.request.RenduRelectureRequete;
import com.k48.leonel.presence48.dto.response.RelectureRelecteurReponse;
import com.k48.leonel.presence48.service.RelectureService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RelectureController {

  private final RelectureService relectures;

  public RelectureController(RelectureService relectures) {
    this.relectures = relectures;
  }

  /** SF-8 (complément, protégé) : relectures assignées à l'étudiant, sans l'auteur. */
  @GetMapping("/api/etudiants/{etudiantId}/relectures")
  public List<RelectureRelecteurReponse> lister(@PathVariable Long etudiantId,
      @RequestParam(required = false) RelectureService.Filtre statut) {
    return relectures.lister(etudiantId, statut);
  }

  /** Opération imposée (EF9) : identité déclarée par l'en-tête X-Etudiant-Id (HYP-2). */
  @PostMapping("/api/relectures/{id}")
  public void rendre(@PathVariable Long id, @RequestHeader("X-Etudiant-Id") Long etudiantId,
      @Valid @RequestBody RenduRelectureRequete corps) {
    relectures.rendre(id, etudiantId, corps.note(), corps.commentaire());
  }
}
