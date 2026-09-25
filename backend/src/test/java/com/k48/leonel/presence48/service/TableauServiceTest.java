package com.k48.leonel.presence48.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** RG16 : moyenne arrondie à 2 décimales, null sans note. */
class TableauServiceTest {

  @Test
  void testRg16ArrondiADeuxDecimalesEtNullSansNote() {
    assertThat(TableauService.arrondir(new BigDecimal("13.3333"))).as("13,33").isEqualByComparingTo("13.33");
    assertThat(TableauService.arrondir(new BigDecimal("12.335"))).as("arrondi supérieur").isEqualByComparingTo("12.34");
    assertThat(TableauService.arrondir(null)).as("aucune note").isNull();
  }
}
