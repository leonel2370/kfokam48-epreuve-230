package com.k48.leonel.presence48.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** RG17 : un lien d'exercice est une URL absolue http ou https. */
class ExerciceServiceTest {

  @Test
  void testRg17LiensAcceptes() {
    assertThat(ExerciceService.lienValide("https://github.com/awa/tp")).as("https").isTrue();
    assertThat(ExerciceService.lienValide("HTTP://exemple.cm/tp?x=1")).as("http, schéma en majuscules").isTrue();
  }

  @Test
  void testRg17LiensRefuses() {
    assertThat(ExerciceService.lienValide("ftp://serveur/tp")).as("autre schéma").isFalse();
    assertThat(ExerciceService.lienValide("github.com/awa/tp")).as("URL relative").isFalse();
    assertThat(ExerciceService.lienValide("https://")).as("sans hôte").isFalse();
    assertThat(ExerciceService.lienValide("https://exemple.cm/a b")).as("syntaxe invalide").isFalse();
    assertThat(ExerciceService.lienValide("https://exemple.cm/" + "a".repeat(2048))).as("trop long").isFalse();
  }
}
