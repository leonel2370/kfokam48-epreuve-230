package com.k48.leonel.presence48.service;

import com.k48.leonel.presence48.dto.response.ExerciceAuteurReponse;
import com.k48.leonel.presence48.dto.response.ExerciceDeposeReponse;
import com.k48.leonel.presence48.entity.Relecture;
import com.k48.leonel.presence48.entity.Role;
import com.k48.leonel.presence48.entity.StatutExercice;
import com.k48.leonel.presence48.repository.RelectureRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import com.k48.leonel.presence48.entity.Exercice;
import com.k48.leonel.presence48.exception.MetierException;
import com.k48.leonel.presence48.repository.EtudiantRepository;
import com.k48.leonel.presence48.repository.ExerciceRepository;
import com.k48.leonel.presence48.repository.SessionCoursRepository;
import com.k48.leonel.presence48.securite.ControleAcces;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SF-6 : dépôt du lien d'un exercice. Contrôles dans l'ordre : lien (RG17) → session et étudiant
 * → promotion (RG19) → session non clôturée (RG18) → pas déjà déposé (RG13). La présence de l'auteur
 * n'est pas exigée (HYP-8) et le dépôt reste possible après l'expiration du code (RG12).
 */
@Service
public class ExerciceService {

  private static final Logger LOG = LoggerFactory.getLogger(ExerciceService.class);
  private static final Set<String> SCHEMAS = Set.of("http", "https");
  private static final int LONGUEUR_MAX_LIEN = 2048;
  private static final int DECIMALES_NOTE = 2;

  private final ExerciceRepository exercices;
  private final SessionCoursRepository sessions;
  private final EtudiantRepository etudiants;
  private final RelectureRepository relectures;
  private final TirageRelecteur tirage;
  private final ControleAcces acces;
  private final Clock horloge;

  public ExerciceService(ExerciceRepository exercices, SessionCoursRepository sessions, EtudiantRepository etudiants,
      RelectureRepository relectures, TirageRelecteur tirage, ControleAcces acces, Clock horloge) {
    this.exercices = exercices;
    this.sessions = sessions;
    this.etudiants = etudiants;
    this.relectures = relectures;
    this.tirage = tirage;
    this.acces = acces;
    this.horloge = horloge;
  }

  @Transactional
  public ExerciceDeposeReponse deposer(Long sessionId, Long etudiantId, String lien) {
    acces.verifierIdentite(etudiantId);
    var lienNettoye = lien.strip();
    if (!lienValide(lienNettoye)) {
      throw new MetierException(HttpStatus.BAD_REQUEST, "LIEN_INVALIDE",
          "Le lien doit être une adresse http ou https complète.");
    }
    var session = sessions.findById(sessionId).orElseThrow(() ->
        new MetierException(HttpStatus.BAD_REQUEST, "SESSION_INTROUVABLE", "Cette session n'existe pas."));
    var etudiant = etudiants.findById(etudiantId).orElseThrow(() ->
        new MetierException(HttpStatus.BAD_REQUEST, "ETUDIANT_INCONNU", "Cet étudiant n'existe pas."));
    if (!session.getPromotionId().equals(etudiant.getPromotionId())) {
      throw new MetierException(HttpStatus.BAD_REQUEST, "ETUDIANT_HORS_PROMOTION",
          "Cette session n'est pas celle de votre promotion.");
    }
    if (session.estCloturee()) {
      throw new MetierException(HttpStatus.CONFLICT, "SESSION_CLOTUREE", "Cette session est clôturée.");
    }
    if (exercices.existsBySessionIdAndAuteurId(sessionId, etudiantId)) {
      throw new MetierException(HttpStatus.CONFLICT, "EXERCICE_DEJA_DEPOSE",
          "Vous avez déjà déposé un exercice pour cette session.");
    }
    var exercice = exercices.save(new Exercice(sessionId, etudiantId, lienNettoye, horloge.instant()));
    tirage.assigner(exercice);
    return new ExerciceDeposeReponse(exercice.getId(), exercice.getStatut());
  }

  /**
   * SF-14 (route protégée) : exercices de l'auteur avec la note retenue (moyenne des notes rendues, RG16 v3),
   * provisoire tant que les deux relecteurs n'ont pas rendu (RG31), sans leur identité (RG8).
   * Ordre des contrôles (#109, RG25) : pour un étudiant connecté, l'identité (403) passe AVANT
   * l'existence (404) afin qu'il ne puisse pas énumérer les identifiants (OWASP A01).
   */
  @Transactional(readOnly = true)
  public List<ExerciceAuteurReponse> exercicesDe(Long etudiantId) {
    var connecte = acces.connecte().orElse(null);
    if (connecte != null && connecte.role() == Role.ETUDIANT) {
      acces.verifierIdentite(etudiantId);
    }
    var etudiant = etudiants.findById(etudiantId).orElseThrow(() ->
        new MetierException(HttpStatus.NOT_FOUND, "ETUDIANT_INCONNU", "Cet étudiant n'existe pas."));
    if (connecte != null && connecte.role() != Role.ETUDIANT) {
      acces.verifierGestionPromotion(etudiant.getPromotionId());
    }
    return exercices.findByAuteurIdOrderByDeposeAtDesc(etudiantId).stream().map(x -> {
      var rendues = relectures.findByExerciceIdOrderByIdAsc(x.getId()).stream()
          .filter(Relecture::estRendue).toList();
      var titre = sessions.getReferenceById(x.getSessionId()).getTitre();
      var note = noteRetenue(rendues.stream().map(Relecture::getNote).toList());
      return new ExerciceAuteurReponse(x.getId(), x.getSessionId(), titre, x.getLien(), x.getStatut(), note,
          !rendues.isEmpty() && x.getStatut() != StatutExercice.RELU,
          rendues.stream().map(Relecture::getCommentaire).toList());
    }).toList();
  }

  /** RG16 v3 : moyenne des notes rendues, 2 décimales ; null sans note. */
  static BigDecimal noteRetenue(List<Integer> notes) {
    if (notes.isEmpty()) {
      return null;
    }
    var somme = notes.stream().mapToInt(Integer::intValue).sum();
    return BigDecimal.valueOf(somme).divide(BigDecimal.valueOf(notes.size()), DECIMALES_NOTE, RoundingMode.HALF_UP);
  }

  /** RG17 : URL absolue http(s) avec un hôte. */
  static boolean lienValide(String lien) {
    if (lien.length() > LONGUEUR_MAX_LIEN) {
      return false;
    }
    try {
      var uri = new URI(lien);
      return uri.getScheme() != null && SCHEMAS.contains(uri.getScheme().toLowerCase(Locale.ROOT))
          && uri.getHost() != null;
    } catch (URISyntaxException e) {
      LOG.debug("Lien refusé (RG17) : {}", e.getMessage());
      return false;
    }
  }
}
