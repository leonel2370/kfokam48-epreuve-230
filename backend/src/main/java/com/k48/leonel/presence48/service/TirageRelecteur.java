package com.k48.leonel.presence48.service;

import com.k48.leonel.presence48.entity.Exercice;
import com.k48.leonel.presence48.entity.Relecture;
import com.k48.leonel.presence48.entity.StatutExercice;
import com.k48.leonel.presence48.repository.ExerciceRepository;
import com.k48.leonel.presence48.repository.PresenceRepository;
import com.k48.leonel.presence48.repository.RelectureRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

/**
 * SF-7 : tirage uniforme du relecteur parmi les présents de la session, auteur exclu (RG5, RG7).
 * Sans candidat, l'exercice reste DEPOSE (RG11) et le tirage est retenté à la présence suivante (HYP-3).
 */
@Component
public class TirageRelecteur {

  private static final RandomGenerator ALEATOIRE = new SecureRandom();

  private final PresenceRepository presences;
  private final ExerciceRepository exercices;
  private final RelectureRepository relectures;
  private final EntityManager em;
  private final Clock horloge;

  public TirageRelecteur(PresenceRepository presences, ExerciceRepository exercices,
      RelectureRepository relectures, EntityManager em, Clock horloge) {
    this.presences = presences;
    this.exercices = exercices;
    this.relectures = relectures;
    this.em = em;
    this.horloge = horloge;
  }

  /** Règle pure, testée seule (B6) : un présent au hasard, jamais l'auteur. */
  static Optional<Long> choisir(List<Long> presents, Long auteurId, RandomGenerator aleatoire) {
    var candidats = presents.stream().filter(id -> !id.equals(auteurId)).distinct().toList();
    return candidats.isEmpty() ? Optional.empty() : Optional.of(candidats.get(aleatoire.nextInt(candidats.size())));
  }

  /**
   * Assigne un relecteur à l'exercice s'il n'en a pas encore (RG6) et qu'un candidat existe.
   * #83 : la ligne de l'exercice est verrouillée puis relue avant le tirage. Deux présences simultanées
   * ne tirent donc plus chacune un relecteur (conflit qui annulait l'une des présences) : la seconde
   * attend la première et trouve l'exercice déjà assigné.
   */
  public void assigner(Exercice exercice) {
    em.refresh(exercice, LockModeType.PESSIMISTIC_WRITE);
    if (exercice.getStatut() != StatutExercice.DEPOSE || relectures.findByExerciceId(exercice.getId()).isPresent()) {
      return;
    }
    choisir(presences.etudiantsPresents(exercice.getSessionId()), exercice.getAuteurId(), ALEATOIRE)
        .ifPresent(relecteurId -> {
          relectures.save(new Relecture(exercice.getId(), relecteurId, horloge.instant()));
          exercice.attendreRelecture();
          exercices.save(exercice);
        });
  }

  /** HYP-3 : après une nouvelle présence, retente le tirage des exercices de la session restés DEPOSE. */
  public void reessayer(Long sessionId) {
    exercices.findBySessionIdAndStatut(sessionId, StatutExercice.DEPOSE).forEach(this::assigner);
  }
}
