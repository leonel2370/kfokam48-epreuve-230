package com.k48.leonel.presence48.controller;

import com.k48.leonel.presence48.dto.response.EtudiantReponse;
import com.k48.leonel.presence48.dto.response.PromotionReponse;
import com.k48.leonel.presence48.service.ReferentielService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** EF1 : choisir sa promotion puis son nom (routes publiques, RG22). */
@RestController
@RequestMapping("/api/promotions")
public class ReferentielController {

  private final ReferentielService referentiel;

  public ReferentielController(ReferentielService referentiel) {
    this.referentiel = referentiel;
  }

  @GetMapping
  public List<PromotionReponse> promotions() {
    return referentiel.promotions();
  }

  @GetMapping("/{promotionId}/etudiants")
  public List<EtudiantReponse> etudiants(@PathVariable Long promotionId) {
    return referentiel.etudiantsDeLaPromotion(promotionId);
  }
}
