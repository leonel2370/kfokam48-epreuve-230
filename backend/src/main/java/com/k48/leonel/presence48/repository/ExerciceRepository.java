package com.k48.leonel.presence48.repository;

import com.k48.leonel.presence48.entity.Exercice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {

  /** RG13 : un exercice par étudiant et par session. */
  boolean existsBySessionIdAndAuteurId(Long sessionId, Long auteurId);
}
