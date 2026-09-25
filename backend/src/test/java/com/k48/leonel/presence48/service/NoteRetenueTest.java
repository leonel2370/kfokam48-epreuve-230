package com.k48.leonel.presence48.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** RG16 v3 : la note retenue est la moyenne des notes rendues, à 2 décimales ; null sans note. */
class NoteRetenueTest {

  @Test
  void testRg16MoyenneDesNotesRendues() {
    assertThat(ExerciceService.noteRetenue(List.of(12, 17))).as("deux notes").isEqualByComparingTo("14.50");
    assertThat(ExerciceService.noteRetenue(List.of(13))).as("une seule note (provisoire)").isEqualByComparingTo("13");
    assertThat(ExerciceService.noteRetenue(List.of())).as("aucune note").isNull();
  }
}
