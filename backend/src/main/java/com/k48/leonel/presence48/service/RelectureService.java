package com.k48.leonel.presence48.service;

import com.k48.leonel.presence48.dto.response.RelectureRelecteurReponse;
import com.k48.leonel.presence48.entity.Relecture;
import com.k48.leonel.presence48.entity.Role;
import com.k48.leonel.presence48.exception.MetierException;
import com.k48.leonel.presence48.repository.EtudiantRepository;
import com.k48.leonel.presence48.repository.ExerciceRepository;
import com.k48.leonel.presence48.repository.RelectureRepository;
import com.k48.leonel.presence48.repository.SessionCoursRepository;
import com.k48.leonel.presence48.securite.ControleAcces;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** SF-8 et SF-9 : relectures du relecteur et envoi définitif de la note. */
@Service
public class RelectureService {

  /** Filtre de SF-8. */
  public enum Filtre {
    A_FAIRE,
    RENDUE
  }

  private static final BigDecimal NOTE_MIN = BigDecimal.ZERO;
  private static final BigDecimal NOTE_MAX = BigDecimal.valueOf(20);

  private final RelectureRepository relectures;
  private final ExerciceRepository exercices;
  private final SessionCoursRepository sessions;
  private final EtudiantRepository etudiants;
  private final ControleAcces acces;
  private final Clock horloge;

  public RelectureService(RelectureRepository relectures, ExerciceRepository exercices,
      SessionCoursRepository sessions, EtudiantRepository etudiants, ControleAcces acces, Clock horloge) {
    this.relectures = relectures;
    this.exercices = exercices;
    this.sessions = sessions;
    this.etudiants = etudiants;
    this.acces = acces;
    this.horloge = horloge;
  }

  /** SF-8 (route protégée) : l'étudiant voit les siennes ; ADMIN et FORMATEUR de la promotion peuvent lire. */
  @Transactional(readOnly = true)
  public List<RelectureRelecteurReponse> lister(Long etudiantId, Filtre filtre) {
    var etudiant = etudiants.findById(etudiantId).orElseThrow(() ->
        new MetierException(HttpStatus.NOT_FOUND, "ETUDIANT_INCONNU", "Cet étudiant n'existe pas."));
    acces.connecte().ifPresent(u -> {
      if (u.role() == Role.ETUDIANT) {
        acces.verifierIdentite(etudiantId);
      } else {
        acces.verifierGestionPromotion(etudiant.getPromotionId());
      }
    });
    return relectures.vuesDuRelecteur(etudiantId).stream()
        .filter(r -> filtre == null || r.rendue() == (filtre == Filtre.RENDUE))
        .toList();
  }

  /** SF-9, dans l'ordre : note (RG9) → relecture → auteur (RG5) → assigné → clôture (RG18) → déjà rendue (RG10). */
  @Transactional
  public void rendre(Long relectureId, Long appelantId, BigDecimal note, String commentaire) {
    acces.verifierIdentite(appelantId);
    if (!noteValide(note)) {
      throw new MetierException(HttpStatus.BAD_REQUEST, "NOTE_INVALIDE", "La note doit être un entier de 0 à 20.");
    }
    var relecture = relectures.findById(relectureId).orElseThrow(() ->
        new MetierException(HttpStatus.NOT_FOUND, "RELECTURE_INTROUVABLE", "Cette relecture n'existe pas."));
    var exercice = exercices.getReferenceById(relecture.getExerciceId());
    if (exercice.getAuteurId().equals(appelantId)) {
      throw new MetierException(HttpStatus.FORBIDDEN, "AUTO_RELECTURE", "Vous ne pouvez pas relire votre exercice.");
    }
    if (!relecture.getRelecteurId().equals(appelantId)) {
      throw new MetierException(HttpStatus.FORBIDDEN, "RELECTEUR_NON_ASSIGNE",
          "Cette relecture est assignée à un autre étudiant.");
    }
    if (sessions.getReferenceById(exercice.getSessionId()).estCloturee()) {
      throw new MetierException(HttpStatus.CONFLICT, "SESSION_CLOTUREE", "Cette session est clôturée.");
    }
    if (relecture.estRendue()) {
      throw new MetierException(HttpStatus.CONFLICT, "RELECTURE_DEJA_RENDUE", "Cette relecture est déjà rendue.");
    }
    relecture.rendre(note.intValueExact(), commentaire.strip(), horloge.instant());
    // RG6 v3, RG31 : RELU seulement quand les deux relecteurs ont rendu ; sinon la note reste provisoire.
    var toutes = relectures.findByExerciceIdOrderByIdAsc(exercice.getId());
    if (toutes.size() >= TirageRelecteur.RELECTEURS_PAR_EXERCICE && toutes.stream().allMatch(Relecture::estRendue)) {
      exercice.marquerRelu();
    }
  }

  /** RG9 : entier de 0 à 20 inclus. */
  static boolean noteValide(BigDecimal note) {
    return note.stripTrailingZeros().scale() <= 0 && note.compareTo(NOTE_MIN) >= 0 && note.compareTo(NOTE_MAX) <= 0;
  }
}
