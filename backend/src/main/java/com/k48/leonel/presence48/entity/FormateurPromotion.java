package com.k48.leonel.presence48.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** #61 / RG26 : un formateur rattaché à une promotion (table V3, PK composite). */
@Entity
@Table(name = "formateur_promotion")
public class FormateurPromotion {

  @Id
  private FormateurPromotionId id;

  protected FormateurPromotion() {
    // requis par JPA
  }

  public FormateurPromotion(Long utilisateurId, Long promotionId) {
    this.id = new FormateurPromotionId(utilisateurId, promotionId);
  }

  public FormateurPromotionId getId() {
    return id;
  }
}
