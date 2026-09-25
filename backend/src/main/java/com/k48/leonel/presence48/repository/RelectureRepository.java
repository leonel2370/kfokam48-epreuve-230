package com.k48.leonel.presence48.repository;

import com.k48.leonel.presence48.dto.response.RelectureRelecteurReponse;
import com.k48.leonel.presence48.entity.Relecture;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

  /** RG6 v3 : jusqu'à deux relectures par exercice. */
  List<Relecture> findByExerciceIdOrderByIdAsc(Long exerciceId);

  /** SF-8 : vue du relecteur, sans l'identité de l'auteur (HYP-10), les plus récentes d'abord. */
  @Query("""
      SELECT new com.k48.leonel.presence48.dto.response.RelectureRelecteurReponse(
        r.id, e.id, s.titre, e.lien, CASE WHEN r.rendueAt IS NULL THEN false ELSE true END, r.note, r.commentaire)
      FROM Relecture r, Exercice e, SessionCours s
      WHERE e.id = r.exerciceId AND s.id = e.sessionId AND r.relecteurId = :relecteurId
      ORDER BY r.assigneeAt DESC""")
  List<RelectureRelecteurReponse> vuesDuRelecteur(Long relecteurId);
}
