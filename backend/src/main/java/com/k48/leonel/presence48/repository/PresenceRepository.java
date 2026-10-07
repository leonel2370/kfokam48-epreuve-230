package com.k48.leonel.presence48.repository;

import com.k48.leonel.presence48.entity.Presence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

  /** RG3 : une seule présence par (session, étudiant). */
  boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

  /** #61 / RG28 : historique de présence — la fiche n'est pas supprimable physiquement. */
  boolean existsByEtudiantId(Long etudiantId);

  /** SF-7 : les candidats au tirage sont les présents de la session dont la fiche est active (RG33). */
  @Query("SELECT p.etudiantId FROM Presence p, Etudiant e "
      + "WHERE e.id = p.etudiantId AND e.actif = true AND p.sessionId = :sessionId")
  List<Long> candidatsAuTirage(Long sessionId);
}
