package com.k48.leonel.presence48.repository;

import com.k48.leonel.presence48.entity.Presence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

  /** RG3 : une seule présence par (session, étudiant). */
  boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);
}
