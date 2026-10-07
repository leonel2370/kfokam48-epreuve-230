package com.k48.leonel.presence48.repository;

import com.k48.leonel.presence48.entity.Role;
import com.k48.leonel.presence48.entity.Utilisateur;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

  Optional<Utilisateur> findByLogin(String login);

  /** #60 : liste triée par login, paginée. */
  Page<Utilisateur> findAllByOrderByLoginAsc(Pageable pageable);

  /** RG27 : la fiche étudiant liée à un compte doit être unique. */
  boolean existsByEtudiantId(Long etudiantId);

  Optional<Utilisateur> findByEtudiantId(Long etudiantId);

  /** #134 : comptes liés à un ensemble de fiches, en une requête. */
  List<Utilisateur> findByEtudiantIdIn(Collection<Long> etudiantIds);

  /** #134 : liste filtrée par rôle, triée par login, paginée. */
  Page<Utilisateur> findByRoleOrderByLoginAsc(Role role, Pageable pageable);

  /**
   * RG28 : comptes actifs d'un rôle, verrouillés jusqu'à la fin de la transaction. Deux demandes
   * simultanées de retrait d'un administrateur ne peuvent donc pas se croire toutes deux autorisées (#130).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM Utilisateur u WHERE u.role = :role AND u.actif = true")
  List<Utilisateur> verrouillerActifs(@Param("role") Role role);

  /** Promotions d'un formateur (RG26). */
  @Query(value = "SELECT promotion_id FROM formateur_promotion WHERE utilisateur_id = :id ORDER BY promotion_id",
      nativeQuery = true)
  List<Long> promotionsDuFormateur(@Param("id") Long utilisateurId);

  /** Promotion de la fiche étudiant liée au compte (HYP-15). */
  @Query(value = "SELECT promotion_id FROM etudiant WHERE id = :id", nativeQuery = true)
  List<Long> promotionDeLEtudiant(@Param("id") Long etudiantId);
}
