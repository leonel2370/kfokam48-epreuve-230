package com.k48.leonel.presence48.service;

import com.k48.leonel.presence48.dto.response.LigneTableauReponse;
import com.k48.leonel.presence48.exception.MetierException;
import com.k48.leonel.presence48.repository.PromotionRepository;
import com.k48.leonel.presence48.securite.ControleAcces;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SF-10 : tableau de la promotion, calculé par le serveur uniquement (F3). Une seule requête agrégée,
 * aucune boucle par étudiant (ENF2) ; les étudiants sans activité ont une ligne à zéro.
 * v3 (#85) : la moyenne porte sur la note retenue de chaque exercice (moyenne de ses deux relectures, RG16).
 */
@Service
public class TableauService {

  private static final int DECIMALES_MOYENNE = 2;

  private static final String SQL = """
      SELECT e.id, e.nom,
        (SELECT COUNT(*) FROM presence p JOIN session s ON s.id = p.session_id
          WHERE p.etudiant_id = e.id AND s.promotion_id = e.promotion_id) AS presences,
        (SELECT COUNT(*) FROM exercice x WHERE x.auteur_id = e.id) AS exercices,
        (SELECT AVG((SELECT AVG(CAST(r.note AS NUMERIC(5, 2))) FROM relecture r
            WHERE r.exercice_id = x.id AND r.rendue_at IS NOT NULL))
          FROM exercice x WHERE x.auteur_id = e.id) AS moyenne,
        (SELECT COUNT(*) FROM relecture r WHERE r.relecteur_id = e.id AND r.rendue_at IS NULL) AS en_attente
      FROM etudiant e
      WHERE e.promotion_id = ?
      ORDER BY e.nom""";

  private final JdbcTemplate jdbc;
  private final PromotionRepository promotions;
  private final ControleAcces acces;

  public TableauService(JdbcTemplate jdbc, PromotionRepository promotions, ControleAcces acces) {
    this.jdbc = jdbc;
    this.promotions = promotions;
    this.acces = acces;
  }

  /** Opération imposée : publique sans session ; connecté, réservée à ADMIN et au FORMATEUR de la promotion. */
  @Transactional(readOnly = true)
  public List<LigneTableauReponse> tableau(Long promotionId) {
    if (!promotions.existsById(promotionId)) {
      throw new MetierException(HttpStatus.NOT_FOUND, "PROMOTION_INCONNUE", "Cette promotion n'existe pas.");
    }
    acces.verifierGestionPromotion(promotionId);
    return jdbc.query(SQL, (rs, i) -> new LigneTableauReponse(rs.getLong("id"), rs.getString("nom"),
        rs.getInt("presences"), rs.getInt("exercices"), arrondir(rs.getBigDecimal("moyenne")),
        rs.getInt("en_attente")), promotionId);
  }

  /** RG16 : arrondi à 2 décimales, null s'il n'y a aucune note. */
  static BigDecimal arrondir(BigDecimal moyenne) {
    return moyenne == null ? null : moyenne.setScale(DECIMALES_MOYENNE, RoundingMode.HALF_UP);
  }
}
