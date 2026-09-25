package com.k48.leonel.presence48.db;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** ENF5 : les données de démonstration sont chargées et respectent les règles de gestion. */
@SpringBootTest
class DonneesDemoTest {

  @Autowired
  private JdbcTemplate jdbc;

  private int compter(String sql) {
    Integer n = jdbc.queryForObject(sql, Integer.class);
    return n == null ? 0 : n;
  }

  @Test
  void testDonneesDemoChargees() {
    assertThat(compter("SELECT COUNT(*) FROM promotion")).as("deux promotions").isEqualTo(2);
    assertThat(compter("SELECT COUNT(*) FROM etudiant")).as("douze étudiants").isEqualTo(12);
    assertThat(compter("SELECT COUNT(*) FROM presence")).as("six présences").isEqualTo(6);
    assertThat(compter("SELECT COUNT(*) FROM presence WHERE source = 'FORMATEUR'"))
        .as("une présence ajoutée par le formateur (Q14)").isEqualTo(1);
    assertThat(compter("SELECT COUNT(*) FROM relecture WHERE rendue_at IS NULL"))
        .as("deux relectures en attente (Q11)").isEqualTo(2);
  }

  @Test
  void testRg5Rg7RelecteurPresentEtDifferentDeLAuteur() {
    int violations = compter("""
        SELECT COUNT(*) FROM relecture r
        JOIN exercice e ON e.id = r.exercice_id
        LEFT JOIN presence p ON p.session_id = e.session_id AND p.etudiant_id = r.relecteur_id
        WHERE r.relecteur_id = e.auteur_id OR p.id IS NULL""");
    assertThat(violations).as("aucun relecteur auteur ou absent").isZero();
  }

  @Test
  void testSequencesUtilisablesApresLesDonneesDemo() {
    jdbc.update("INSERT INTO promotion (nom) VALUES ('P-test-sequence')");
    assertThat(compter("SELECT COUNT(*) FROM promotion WHERE nom = 'P-test-sequence'"))
        .as("l'identité générée ne heurte pas les données de démo").isEqualTo(1);
    jdbc.update("DELETE FROM promotion WHERE nom = 'P-test-sequence'");
  }
}
