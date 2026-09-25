package com.k48.leonel.presence48.repository;

import com.k48.leonel.presence48.entity.Utilisateur;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

  Optional<Utilisateur> findByLogin(String login);

  /** Promotions d'un formateur (RG26). */
  @Query(value = "SELECT promotion_id FROM formateur_promotion WHERE utilisateur_id = :id ORDER BY promotion_id",
      nativeQuery = true)
  List<Long> promotionsDuFormateur(@Param("id") Long utilisateurId);

  /** Promotion de la fiche étudiant liée au compte (HYP-15). */
  @Query(value = "SELECT promotion_id FROM etudiant WHERE id = :id", nativeQuery = true)
  List<Long> promotionDeLEtudiant(@Param("id") Long etudiantId);
}
