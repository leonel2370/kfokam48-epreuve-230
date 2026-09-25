package com.k48.leonel.presence48.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** RG9 : une note est un entier de 0 à 20 inclus. */
class RelectureServiceTest {

  @Test
  void testRg9NotesAcceptees() {
    assertThat(RelectureService.noteValide(BigDecimal.ZERO)).as("0").isTrue();
    assertThat(RelectureService.noteValide(BigDecimal.valueOf(20))).as("20").isTrue();
    assertThat(RelectureService.noteValide(new BigDecimal("14.0"))).as("14.0 est entier").isTrue();
  }

  @Test
  void testRg9NotesRefusees() {
    assertThat(RelectureService.noteValide(BigDecimal.valueOf(-1))).as("négative").isFalse();
    assertThat(RelectureService.noteValide(BigDecimal.valueOf(21))).as("au-delà de 20").isFalse();
    assertThat(RelectureService.noteValide(new BigDecimal("12.5"))).as("décimale").isFalse();
  }
}
