package com.k48.leonel.presence48.repository;

import com.k48.leonel.presence48.entity.Promotion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

  List<Promotion> findAllByOrderByNomAsc();

  /** #61 : le nom est UNIQUE (V1) — vérifié avant écriture pour un 409 explicite. */
  boolean existsByNom(String nom);

  /** #61 / RG28 : une promotion avec des étudiants ou des sessions n'est pas supprimable. */
  boolean existsByNomAndIdNot(String nom, Long id);
}
