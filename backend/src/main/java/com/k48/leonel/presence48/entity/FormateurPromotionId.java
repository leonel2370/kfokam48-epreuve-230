package com.k48.leonel.presence48.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/** PK composite de formateur_promotion (V3, RG26). */
@Embeddable
public class FormateurPromotionId implements Serializable {

  @Column(name = "utilisateur_id")
  private Long utilisateurId;

  @Column(name = "promotion_id")
  private Long promotionId;

  protected FormateurPromotionId() {
    // requis par JPA
  }

  public FormateurPromotionId(Long utilisateurId, Long promotionId) {
    this.utilisateurId = utilisateurId;
    this.promotionId = promotionId;
  }

  public Long getUtilisateurId() {
    return utilisateurId;
  }

  public Long getPromotionId() {
    return promotionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FormateurPromotionId autre)) {
      return false;
    }
    return Objects.equals(utilisateurId, autre.utilisateurId) && Objects.equals(promotionId, autre.promotionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(utilisateurId, promotionId);
  }
}
