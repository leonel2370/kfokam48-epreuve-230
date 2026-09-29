package com.k48.leonel.presence48.repository;

import com.k48.leonel.presence48.entity.FormateurPromotion;
import com.k48.leonel.presence48.entity.FormateurPromotionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** #61 / RG26 : rattachement des formateurs aux promotions (table V3, PK composite). */
public interface FormateurPromotionRepository extends JpaRepository<FormateurPromotion, FormateurPromotionId> {

  @Query("SELECT fp FROM FormateurPromotion fp WHERE fp.id.utilisateurId = :utilisateurId")
  List<FormateurPromotion> findByUtilisateurId(@Param("utilisateurId") Long utilisateurId);

  @Query("SELECT fp FROM FormateurPromotion fp WHERE fp.id.promotionId = :promotionId")
  List<FormateurPromotion> findByPromotionId(@Param("promotionId") Long promotionId);
}
