package com.k48.leonel.presence48.repository;

import com.k48.leonel.presence48.entity.Presence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

  /** RG3 : une seule présence par (session, étudiant). */
  boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

  /** SF-7 : les candidats au tirage sont les présents de la session. */
  @Query("SELECT p.etudiantId FROM Presence p WHERE p.sessionId = :sessionId")
  List<Long> etudiantsPresents(Long sessionId);
}
