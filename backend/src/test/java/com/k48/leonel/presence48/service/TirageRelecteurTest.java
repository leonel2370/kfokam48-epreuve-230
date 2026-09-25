package com.k48.leonel.presence48.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

/** B6 : test unitaire des règles RG5 (jamais l'auteur) et RG7 (un présent tiré au hasard). */
class TirageRelecteurTest {

  private static final Long AUTEUR = 1L;
  private static final List<Long> PRESENTS = List.of(2L, AUTEUR, 3L, 4L);

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
  void testRg5Rg7ChaqueTirageDonneUnPresentAutreQueLAuteur() {
    var aleatoire = new Cyclique();
    var tires = new HashSet<Long>();
    for (var i = 0; i < PRESENTS.size(); i++) {
      var tire = TirageRelecteur.choisir(PRESENTS, AUTEUR, aleatoire);
      assertThat(tire).as("un relecteur est tiré").isPresent();
      assertThat(tire.get()).as("RG5 : jamais l'auteur").isNotEqualTo(AUTEUR);
      tires.add(tire.get());
    }
    assertThat(tires).as("RG7 : tirage parmi les présents uniquement, tous atteignables")
        .containsExactlyInAnyOrder(2L, 3L, 4L);
  }

  @Test
  void testRg11SansAutrePresentAucunRelecteur() {
    var aleatoire = new Cyclique();
    assertThat(TirageRelecteur.choisir(List.of(), AUTEUR, aleatoire)).as("personne de présent").isEmpty();
    assertThat(TirageRelecteur.choisir(List.of(AUTEUR), AUTEUR, aleatoire)).as("seul l'auteur est présent").isEmpty();
    assertThat(aleatoire.nextLong()).as("aucun tirage consommé sans candidat").isZero();
  }
}
