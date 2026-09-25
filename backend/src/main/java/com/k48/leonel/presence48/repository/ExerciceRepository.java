package com.k48.leonel.presence48.repository;

import com.k48.leonel.presence48.entity.Exercice;
import com.k48.leonel.presence48.entity.StatutExercice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {

  /** RG13 : un exercice par étudiant et par session. */
  boolean existsBySessionIdAndAuteurId(Long sessionId, Long auteurId);

  /** HYP-3, HYP-20 : exercices auxquels il peut manquer un relecteur, à retenter à chaque nouvelle présence. */
  List<Exercice> findBySessionIdAndStatutIn(Long sessionId, List<StatutExercice> statuts);

  /** SF-14 : exercices d'un auteur, les plus récents d'abord. */
  List<Exercice> findByAuteurIdOrderByDeposeAtDesc(Long auteurId);
}
