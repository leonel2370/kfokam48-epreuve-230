package com.k48.leonel.presence48.repository;

import com.k48.leonel.presence48.entity.SessionCours;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionCoursRepository extends JpaRepository<SessionCours, Long> {

  /** RG20 : un code n'est réutilisable que lorsqu'aucune session non expirée ne le porte. */
  boolean existsByCodeAndExpirationAtAfter(String code, Instant maintenant);

  /** Le code le plus récent l'emporte si un ancien code expiré a été réattribué. */
  Optional<SessionCours> findFirstByCodeOrderByOuvertureAtDesc(String code);

  List<SessionCours> findByPromotionIdOrderByOuvertureAtDesc(Long promotionId);
}
