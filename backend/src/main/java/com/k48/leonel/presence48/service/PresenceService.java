package com.k48.leonel.presence48.service;

import com.k48.leonel.presence48.dto.response.PresenceReponse;
import com.k48.leonel.presence48.entity.Presence;
import com.k48.leonel.presence48.entity.SourcePresence;
import com.k48.leonel.presence48.entity.TentativeCode;
import com.k48.leonel.presence48.exception.MetierException;
import com.k48.leonel.presence48.repository.EtudiantRepository;
import com.k48.leonel.presence48.repository.PresenceRepository;
import com.k48.leonel.presence48.repository.SessionCoursRepository;
import com.k48.leonel.presence48.repository.TentativeCodeRepository;
import com.k48.leonel.presence48.securite.ControleAcces;
import java.time.Clock;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SF-3 : présence par code, validée automatiquement (RG21). Contrôles dans l'ordre du diagramme D3 :
 * étudiant → blocage (RG4) → code connu → session non clôturée (RG18) → code non expiré (RG1)
 * → promotion (RG19) → pas déjà présent (RG3).
 */
@Service
public class PresenceService {

  /** SF-3 : le code saisi est accepté avec espaces et minuscules. */
  private static final Pattern ESPACES = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

  private final SessionCoursRepository sessions;
  private final EtudiantRepository etudiants;
  private final PresenceRepository presences;
  private final TentativeCodeRepository tentatives;
  private final ControleAcces acces;
  private final Clock horloge;

  public PresenceService(SessionCoursRepository sessions, EtudiantRepository etudiants,
      PresenceRepository presences, TentativeCodeRepository tentatives, ControleAcces acces, Clock horloge) {
    this.sessions = sessions;
    this.etudiants = etudiants;
    this.presences = presences;
    this.tentatives = tentatives;
    this.acces = acces;
    this.horloge = horloge;
  }

  /** Le compteur d'échecs (RG4) doit survivre à l'erreur 400 CODE_INCONNU : pas de rollback métier. */
  @Transactional(noRollbackFor = MetierException.class)
  public PresenceReponse marquer(String code, Long etudiantId) {
    acces.verifierIdentite(etudiantId);
    if (!etudiants.existsById(etudiantId)) {
      throw new MetierException(HttpStatus.BAD_REQUEST, "ETUDIANT_INCONNU", "Cet étudiant n'existe pas.");
    }
    var maintenant = horloge.instant();
    var tentative = tentatives.findById(etudiantId).orElseGet(() -> new TentativeCode(etudiantId));
    if (tentative.estBloque(maintenant)) {
      throw new MetierException(HttpStatus.TOO_MANY_REQUESTS, "TROP_DE_TENTATIVES",
          "Trop de codes erronés : réessayez dans 2 minutes.");
    }
    var saisi = ESPACES.matcher(code).replaceAll("").toUpperCase(Locale.ROOT);
    var session = sessions.findFirstByCodeOrderByOuvertureAtDesc(saisi).orElse(null);
    if (session == null) {
      tentative.echec(maintenant);
      tentatives.save(tentative);
      throw new MetierException(HttpStatus.BAD_REQUEST, "CODE_INCONNU", "Ce code ne correspond à aucune session.");
    }
    tentative.reussite();
    tentatives.save(tentative);
    if (session.estCloturee()) {
      throw new MetierException(HttpStatus.CONFLICT, "SESSION_CLOTUREE", "Cette session est clôturée.");
    }
    if (session.codeExpire(maintenant)) {
      throw new MetierException(HttpStatus.GONE, "CODE_EXPIRE",
          "Le code a expiré : demandez au formateur de vous ajouter.");
    }
    if (!session.getPromotionId().equals(etudiants.getReferenceById(etudiantId).getPromotionId())) {
      throw new MetierException(HttpStatus.BAD_REQUEST, "ETUDIANT_HORS_PROMOTION",
          "Cette session n'est pas celle de votre promotion.");
    }
    if (presences.existsBySessionIdAndEtudiantId(session.getId(), etudiantId)) {
      throw new MetierException(HttpStatus.CONFLICT, "DEJA_PRESENT", "Votre présence est déjà enregistrée.");
    }
    var presence = presences.save(new Presence(session.getId(), etudiantId, SourcePresence.ETUDIANT, maintenant));
    return new PresenceReponse(presence.getId(), presence.getSessionId(), presence.getEtudiantId(),
        presence.getSource());
  }
}
