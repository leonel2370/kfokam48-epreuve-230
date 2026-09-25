package com.k48.leonel.presence48;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class Presence48ApplicationTests {

  @Autowired
  private ApplicationContext context;

  @Test
  void testContextLoads() {
    // Le contexte démarre et Flyway a appliqué les migrations sur H2 (profil test)
    assertThat(context.getBean(Presence48Application.class))
        .as("le contexte Spring doit contenir la classe principale")
        .isNotNull();
  }
}
