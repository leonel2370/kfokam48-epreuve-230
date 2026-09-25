package com.k48.leonel.presence48.repository;

import com.k48.leonel.presence48.entity.Etudiant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {

  /** Liste de sélection (Q1, EF1) : les étudiants désactivés n'y figurent plus (RG28). */
  List<Etudiant> findByPromotionIdAndActifTrueOrderByNomAsc(Long promotionId);
}
