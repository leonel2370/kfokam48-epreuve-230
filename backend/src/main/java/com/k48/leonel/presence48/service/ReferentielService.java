package com.k48.leonel.presence48.service;

import com.k48.leonel.presence48.dto.response.EtudiantReponse;
import com.k48.leonel.presence48.dto.response.PromotionReponse;
import com.k48.leonel.presence48.exception.MetierException;
import com.k48.leonel.presence48.repository.EtudiantRepository;
import com.k48.leonel.presence48.repository.PromotionRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Listes de sélection publiques (SF-1, RG22) : identifiants et noms seulement. */
@Service
@Transactional(readOnly = true)
public class ReferentielService {

  private final PromotionRepository promotions;
  private final EtudiantRepository etudiants;

  public ReferentielService(PromotionRepository promotions, EtudiantRepository etudiants) {
    this.promotions = promotions;
    this.etudiants = etudiants;
  }

  public List<PromotionReponse> promotions() {
    return promotions.findAllByOrderByNomAsc().stream().map(p -> new PromotionReponse(p.getId(), p.getNom())).toList();
  }

  public List<EtudiantReponse> etudiantsDeLaPromotion(Long promotionId) {
    if (!promotions.existsById(promotionId)) {
      throw new MetierException(HttpStatus.NOT_FOUND, "PROMOTION_INCONNUE", "Cette promotion n'existe pas.");
    }
    return etudiants.findByPromotionIdAndActifTrueOrderByNomAsc(promotionId).stream()
        .map(e -> new EtudiantReponse(e.getId(), e.getNom(), e.getPromotionId())).toList();
  }
}
