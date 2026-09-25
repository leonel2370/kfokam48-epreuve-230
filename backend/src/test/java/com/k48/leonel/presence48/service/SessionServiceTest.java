package com.k48.leonel.presence48.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.k48.leonel.presence48.entity.SessionCours;
import com.k48.leonel.presence48.repository.PromotionRepository;
import com.k48.leonel.presence48.repository.SessionCoursRepository;
import com.k48.leonel.presence48.repository.UtilisateurRepository;
import com.k48.leonel.presence48.securite.ControleAcces;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests unitaires des règles RG1 et RG20 (horloge fixe, sans base de données). */
class SessionServiceTest {

  private static final Instant OUVERTURE = Instant.parse("2026-09-25T09:00:00Z");

  private final SessionCoursRepository sessions = mock(SessionCoursRepository.class);
  private final PromotionRepository promotions = mock(PromotionRepository.class);
  private final GenerateurCode generateur = mock(GenerateurCode.class);
  private SessionService service;

  @BeforeEach
  void preparer() {
    when(promotions.existsById(1L)).thenReturn(true);
    when(sessions.save(any(SessionCours.class))).thenAnswer(i -> i.getArgument(0));
    service = new SessionService(sessions, promotions, mock(UtilisateurRepository.class), generateur,
        new ControleAcces(mock(UtilisateurRepository.class)), Clock.fixed(OUVERTURE, ZoneOffset.UTC));
  }

  @Test
  void testRg1ExpirationEgaleOuverturePlus15min() {
    when(generateur.nouveauCode()).thenReturn("K7MX4Q");
    var r = service.ouvrir("  TP Spring JPA ", 1L);
    assertThat(r.ouvertureAt()).as("ouverture = maintenant").isEqualTo(OUVERTURE);
    assertThat(Duration.between(r.ouvertureAt(), r.expirationAt()))
        .as("RG1 : le code expire 15 minutes après l'ouverture").isEqualTo(Duration.ofMinutes(15));
  }

  @Test
  void testRg20CodeDejaUtiliseParUneSessionNonExpireeEstRegenere() {
    when(generateur.nouveauCode()).thenReturn("AAAAAA", "BBBBBB");
    when(sessions.existsByCodeAndExpirationAtAfter(eq("AAAAAA"), any())).thenReturn(true);
    when(sessions.existsByCodeAndExpirationAtAfter(eq("BBBBBB"), any())).thenReturn(false);
    assertThat(service.ouvrir("TP", 1L).code()).as("RG20 : le code en conflit est remplacé").isEqualTo("BBBBBB");
  }

  @Test
  void testGenerateurProduitSixSymbolesNonAmbigus() {
    var code = new GenerateurCode().nouveauCode();
    assertThat(code).as("6 caractères de l'alphabet sans O, 0, I, 1").hasSize(6).matches("[A-HJ-NP-Z2-9]{6}");
  }
}
