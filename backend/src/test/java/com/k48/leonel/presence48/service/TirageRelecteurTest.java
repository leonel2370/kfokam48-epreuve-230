package com.k48.leonel.presence48.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

/** B6 : RG5 (jamais l'auteur), RG6 v3 (deux pairs différents), RG7 (présents tirés au hasard). */
class TirageRelecteurTest {

  private static final Long AUTEUR = 1L;
  private static final List<Long> PRESENTS = List.of(2L, AUTEUR, 3L, 4L);
  private static final int DEUX = TirageRelecteur.RELECTEURS_PAR_EXERCICE;

  /** Générateur déterministe : 0, 1, 2… modulo la borne, pour parcourir tous les tirages possibles. */
  private static final class Cyclique implements RandomGenerator {
    private int suivant;

    @Override
    public long nextLong() {
      return suivant;
    }

    @Override
    public int nextInt(int borne) {
      return suivant++ % borne;
    }
  }

  @Test
  void testRg6Rg5DeuxPairsDifferentsJamaisLAuteur() {
    var aleatoire = new Cyclique();
    var vus = new HashSet<Long>();
    for (var i = 0; i < PRESENTS.size(); i++) {
      var tires = TirageRelecteur.choisir(PRESENTS, Set.of(AUTEUR), DEUX, aleatoire);
      assertThat(tires).as("deux relecteurs").hasSize(DEUX).doesNotHaveDuplicates().doesNotContain(AUTEUR);
      vus.addAll(tires);
    }
    assertThat(vus).as("RG7 : tous les présents autres que l'auteur sont atteignables")
        .containsExactlyInAnyOrder(2L, 3L, 4L);
  }

  @Test
  void testHyp20CompleteSansRetirerUnRelecteurDejaAssigne() {
    var tires = TirageRelecteur.choisir(PRESENTS, Set.of(AUTEUR, 2L), 1, new Cyclique());
    assertThat(tires).as("un seul relecteur manquant, ni l'auteur ni le relecteur déjà assigné")
        .hasSize(1).doesNotContain(AUTEUR, 2L);
  }

  @Test
  void testRg11UnSeulOuAucunCandidat() {
    var aleatoire = new Cyclique();
    assertThat(TirageRelecteur.choisir(List.of(AUTEUR, 2L), Set.of(AUTEUR), DEUX, aleatoire))
        .as("un seul candidat : un relecteur, le second viendra plus tard").containsExactly(2L);
    assertThat(TirageRelecteur.choisir(List.of(AUTEUR), Set.of(AUTEUR), DEUX, aleatoire))
        .as("seul l'auteur est présent").isEmpty();
  }
}
