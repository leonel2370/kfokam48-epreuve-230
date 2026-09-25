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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

/**
 * SF-7 : tirage uniforme des relecteurs parmi les présents de la session, auteur exclu (RG5, RG7).
 * v3 (#85) : deux pairs différents par exercice (RG6). S'il en manque, le tirage est retenté à chaque
 * nouvelle présence (HYP-3, HYP-20) ; sans aucun candidat l'exercice reste DEPOSE (RG11).
 */
@Component
public class TirageRelecteur {

  /** RG6 v3 (enveloppe) : nombre de pairs qui relisent chaque exercice. */
  public static final int RELECTEURS_PAR_EXERCICE = 2;

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

  /** Règle pure, testée seule (B6) : jusqu'à {@code nombre} présents distincts, tirés sans remise, jamais un exclu. */
  static List<Long> choisir(List<Long> presents, Collection<Long> exclus, int nombre, RandomGenerator aleatoire) {
    var candidats = new ArrayList<>(presents.stream().filter(id -> !exclus.contains(id)).distinct().toList());
    var tires = new ArrayList<Long>();
    while (tires.size() < nombre && !candidats.isEmpty()) {
      tires.add(candidats.remove(aleatoire.nextInt(candidats.size())));
    }
    return tires;
  }

  /**
   * Complète les relecteurs de l'exercice jusqu'à deux (RG6 v3), parmi les présents, auteur et relecteurs
   * déjà assignés exclus (RG5). #83 : la ligne de l'exercice est verrouillée puis relue avant le tirage ;
   * deux présences simultanées ne tirent donc jamais deux fois pour la même place.
   */
  public void assigner(Exercice exercice) {
    em.refresh(exercice, LockModeType.PESSIMISTIC_WRITE);
    if (exercice.getStatut() == StatutExercice.RELU) {
      return;
    }
    var deja = relectures.findByExerciceIdOrderByIdAsc(exercice.getId()).stream()
        .map(Relecture::getRelecteurId).toList();
    var manquants = RELECTEURS_PAR_EXERCICE - deja.size();
    if (manquants <= 0) {
      return;
    }
    var exclus = new ArrayList<>(deja);
    exclus.add(exercice.getAuteurId());
    var tires = choisir(presences.etudiantsPresents(exercice.getSessionId()), exclus, manquants, ALEATOIRE);
    tires.forEach(relecteurId -> relectures.save(new Relecture(exercice.getId(), relecteurId, horloge.instant())));
    if (!tires.isEmpty() && exercice.getStatut() == StatutExercice.DEPOSE) {
      exercice.attendreRelecture();
      exercices.save(exercice);
    }
  }

  /** HYP-3, HYP-20 : après une nouvelle présence, complète les relecteurs manquants des exercices non relus. */
  public void reessayer(Long sessionId) {
    exercices.findBySessionIdAndStatutIn(sessionId, List.of(StatutExercice.DEPOSE, StatutExercice.EN_ATTENTE_RELECTURE))
        .forEach(this::assigner);
  }
}
