package com.k48.leonel.presence48.service;

import com.k48.leonel.presence48.dto.response.SessionOuverteReponse;
import com.k48.leonel.presence48.dto.response.SessionReponse;
import com.k48.leonel.presence48.entity.Role;
import com.k48.leonel.presence48.entity.SessionCours;
import com.k48.leonel.presence48.exception.MetierException;
import com.k48.leonel.presence48.repository.PromotionRepository;
import com.k48.leonel.presence48.repository.SessionCoursRepository;
import com.k48.leonel.presence48.repository.UtilisateurRepository;
import com.k48.leonel.presence48.securite.ControleAcces;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** SF-2 : ouverture d'une session de cours et de son code de présence. */
@Service
public class SessionService {

  /** RG1 (Q2) : le code expire 15 minutes après l'ouverture. */
  static final Duration VALIDITE_CODE = Duration.ofMinutes(15);
  private static final int ESSAIS_CODE_UNIQUE = 20;

  private final SessionCoursRepository sessions;
  private final PromotionRepository promotions;
  private final UtilisateurRepository utilisateurs;
  private final GenerateurCode generateur;
  private final ControleAcces acces;
  private final Clock horloge;

  public SessionService(SessionCoursRepository sessions, PromotionRepository promotions,
      UtilisateurRepository utilisateurs, GenerateurCode generateur, ControleAcces acces, Clock horloge) {
    this.sessions = sessions;
    this.promotions = promotions;
    this.utilisateurs = utilisateurs;
    this.generateur = generateur;
    this.acces = acces;
    this.horloge = horloge;
  }

  @Transactional
  public SessionOuverteReponse ouvrir(String titre, Long promotionId) {
    if (!promotions.existsById(promotionId)) {
      throw new MetierException(HttpStatus.BAD_REQUEST, "PROMOTION_INCONNUE", "Cette promotion n'existe pas.");
    }
    acces.verifierGestionPromotion(promotionId);
    var ouverture = horloge.instant();
    var session = sessions.save(new SessionCours(titre.strip(), promotionId, codeUnique(ouverture),
        ouverture, ouverture.plus(VALIDITE_CODE)));
    return new SessionOuverteReponse(session.getId(), session.getCode(), session.getOuvertureAt(),
        session.getExpirationAt());
  }

  /** Sessions d'une promotion : ADMIN, FORMATEUR rattaché, ou ETUDIANT de cette promotion (cahier §2 bis). */
  @Transactional(readOnly = true)
  public List<SessionReponse> lister(Long promotionId) {
    if (!promotions.existsById(promotionId)) {
      throw new MetierException(HttpStatus.NOT_FOUND, "PROMOTION_INCONNUE", "Cette promotion n'existe pas.");
    }
    acces.connecte().ifPresent(u -> {
      if (u.role() == Role.ETUDIANT && !utilisateurs.promotionDeLEtudiant(u.etudiantId()).contains(promotionId)) {
        throw new MetierException(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
            "Vous n'avez pas les droits pour cette action.");
      }
      if (u.role() != Role.ETUDIANT) {
        acces.verifierGestionPromotion(promotionId);
      }
    });
    return sessions.findByPromotionIdOrderByOuvertureAtDesc(promotionId).stream().map(SessionService::versReponse)
        .toList();
  }

  static SessionReponse versReponse(SessionCours s) {
    return new SessionReponse(s.getId(), s.getTitre(), s.getPromotionId(), s.getCode(), s.getOuvertureAt(),
        s.getExpirationAt(), s.getStatut(), s.getClotureAt());
  }

  /** RG20 : unique parmi les codes non expirés. */
  private String codeUnique(java.time.Instant maintenant) {
    for (var i = 0; i < ESSAIS_CODE_UNIQUE; i++) {
      var code = generateur.nouveauCode();
      if (!sessions.existsByCodeAndExpirationAtAfter(code, maintenant)) {
        return code;
      }
    }
    throw new IllegalStateException("Impossible de générer un code de présence unique");
  }
}
