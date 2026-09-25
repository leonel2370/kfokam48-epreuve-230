package com.k48.leonel.presence48.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Horloge injectable : les tests fixent le temps pour RG1, RG4 et RG24. */
@Configuration
public class HorlogeConfig {

  @Bean
  Clock horloge() {
    return Clock.systemUTC();
  }
}
