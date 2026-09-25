package com.k48.leonel.presence48.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.k48.leonel.presence48.entity.TentativeCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** RG4 (Q4) : 5 codes inconnus consécutifs bloquent 2 minutes ; une réussite remet le compteur à zéro. */
class TentativeCodeTest {

  private static final Instant T0 = Instant.parse("2026-09-25T10:00:00Z");

  @Test
  void testRg4BlocageAuCinquiemeEchecPendantDeuxMinutes() {
    var tentative = new TentativeCode(1L);
    for (var i = 0; i < TentativeCode.ECHECS_MAX - 1; i++) {
      tentative.echec(T0);
    }
    assertThat(tentative.estBloque(T0)).as("4 échecs ne bloquent pas").isFalse();
    tentative.echec(T0);
    assertThat(tentative.estBloque(T0.plusSeconds(119))).as("bloqué pendant 2 minutes").isTrue();
    assertThat(tentative.estBloque(T0.plus(TentativeCode.DUREE_BLOCAGE))).as("débloqué après 2 minutes").isFalse();
  }

  @Test
  void testRg4UneReussiteRemetLeCompteurAZero() {
    var tentative = new TentativeCode(1L);
    tentative.echec(T0);
    tentative.echec(T0);
    tentative.reussite();
    assertThat(tentative.getEchecsConsecutifs()).as("compteur remis à zéro").isZero();
    assertThat(tentative.getBloqueJusquA()).as("aucun blocage").isNull();
    assertThat(tentative.getEtudiantId()).as("étudiant conservé").isEqualTo(1L);
  }
}
