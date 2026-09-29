package com.k48.leonel.presence48.service;

import com.k48.leonel.presence48.dto.response.EtudiantReponse;
import com.k48.leonel.presence48.dto.response.PromotionReponse;
import com.k48.leonel.presence48.entity.Etudiant;
import com.k48.leonel.presence48.entity.FormateurPromotion;
import com.k48.leonel.presence48.entity.Promotion;
import com.k48.leonel.presence48.entity.Role;
import com.k48.leonel.presence48.exception.MetierException;
import com.k48.leonel.presence48.repository.EtudiantRepository;
import com.k48.leonel.presence48.repository.ExerciceRepository;
import com.k48.leonel.presence48.repository.FormateurPromotionRepository;
import com.k48.leonel.presence48.repository.PresenceRepository;
import com.k48.leonel.presence48.repository.PromotionRepository;
import com.k48.leonel.presence48.repository.RelectureRepository;
import com.k48.leonel.presence48.repository.SessionCoursRepository;
import com.k48.leonel.presence48.repository.UtilisateurRepository;
import com.k48.leonel.presence48.securite.ControleAcces;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * #61 / SF-21, SF-22 / EF22, EF23 : gestion du référentiel depuis l'application.
 * Promotions : ADMIN (RG25), nom unique (contrainte V1 → 409 CONFLIT), suppression refusée si
 * étudiants ou sessions (RG28). Formateurs : rattachement ADMIN, seuls des comptes FORMATEUR (RG26).
 * Fiches étudiants : ADMIN et FORMATEUR de la promotion (RG26) ; suppression d'une fiche sans
 * historique, désactivation sinon (RG28) — l'historique reste au tableau.
 */
@Service
public class ReferentielAdminService {

  private final PromotionRepository promotions;
  private final EtudiantRepository etudiants;
  private final SessionCoursRepository sessions;
  private final PresenceRepository presences;
  private final ExerciceRepository exercices;
  private final RelectureRepository relectures;
  private final UtilisateurRepository utilisateurs;
  private final FormateurPromotionRepository rattachements;
  private final ControleAcces acces;

  public ReferentielAdminService(PromotionRepository promotions, EtudiantRepository etudiants,
      SessionCoursRepository sessions, PresenceRepository presences, ExerciceRepository exercices,
      RelectureRepository relectures, UtilisateurRepository utilisateurs,
      FormateurPromotionRepository rattachements, ControleAcces acces) {
    this.promotions = promotions;
    this.etudiants = etudiants;
    this.sessions = sessions;
    this.presences = presences;
    this.exercices = exercices;
    this.relectures = relectures;
    this.utilisateurs = utilisateurs;
    this.rattachements = rattachements;
    this.acces = acces;
  }

  // --- Promotions (ADMIN) ---

  @Transactional
  public PromotionReponse creerPromotion(String nom) {
    if (promotions.existsByNom(nom)) {
      throw new MetierException(HttpStatus.CONFLICT, "CONFLIT", "Ce nom de promotion est déjà pris.");
    }
    var p = promotions.save(new Promotion(nom));
    return new PromotionReponse(p.getId(), p.getNom());
  }

  @Transactional
  public PromotionReponse modifierPromotion(Long id, String nom) {
    var p = chargerPromotion(id);
    if (promotions.existsByNomAndIdNot(nom, id)) {
      throw new MetierException(HttpStatus.CONFLICT, "CONFLIT", "Ce nom de promotion est déjà pris.");
    }
    p.setNom(nom);
    return new PromotionReponse(p.getId(), p.getNom());
  }

  /** RG28 : une promotion avec des étudiants ou des sessions n'est pas supprimable. */
  @Transactional
  public void supprimerPromotion(Long id) {
    if (!promotions.existsById(id)) {
      throw new MetierException(HttpStatus.NOT_FOUND, "PROMOTION_INCONNUE", "Cette promotion n'existe pas.");
    }
    if (etudiants.existsByPromotionId(id) || sessionsNonVide(id)) {
      throw new MetierException(HttpStatus.CONFLICT, "SUPPRESSION_IMPOSSIBLE",
          "Cette promotion a des étudiants ou des sessions : elle ne peut pas être supprimée.");
    }
    rattachements.findByPromotionId(id).forEach(rattachements::delete);
    promotions.deleteById(id);
  }

  /** RG26 : définit la liste des formateurs d'une promotion ; seuls des comptes FORMATEUR sont admis. */
  @Transactional
  public void rattacherFormateurs(Long promotionId, List<Long> utilisateurIds) {
    chargerPromotion(promotionId);
    var ids = new LinkedHashSet<>(utilisateurIds);
    for (Long id : ids) {
      var u = utilisateurs.findById(id).orElseThrow(() ->
          new MetierException(HttpStatus.BAD_REQUEST, "CHAMP_MANQUANT", "Cet utilisateur n'existe pas."));
      if (u.getRole() != Role.FORMATEUR) {
        throw new MetierException(HttpStatus.BAD_REQUEST, "CHAMP_MANQUANT",
            "Seuls des comptes FORMATEUR peuvent être rattachés.");
      }
    }
    rattachements.findByPromotionId(promotionId)
        .stream().filter(r -> !ids.contains(r.getId().getUtilisateurId()))
        .forEach(rattachements::delete);
    ids.forEach(id -> {
      var cle = new com.k48.leonel.presence48.entity.FormateurPromotionId(id, promotionId);
      if (!rattachements.existsById(cle)) {
        rattachements.save(new FormateurPromotion(id, promotionId));
      }
    });
  }

  // --- Fiches étudiants (ADMIN, FORMATEUR de la promotion) ---

  @Transactional
  public EtudiantReponse creerEtudiant(Long promotionId, String nom) {
    acces.verifierGestionPromotionStrict(promotionId);
    chargerPromotionDuCorps(promotionId);
    var e = etudiants.save(new Etudiant(nom, promotionId));
    return new EtudiantReponse(e.getId(), e.getNom(), e.getPromotionId());
  }

  @Transactional
  public EtudiantReponse modifierEtudiant(Long id, String nom, Long promotionId) {
    acces.verifierGestionPromotionStrict(promotionId);
    var e = etudiants.findById(id).orElseThrow(() ->
        new MetierException(HttpStatus.NOT_FOUND, "ETUDIANT_INCONNU", "Cet étudiant n'existe pas."));
    chargerPromotionDuCorps(promotionId);
    e.setNom(nom);
    e.setPromotionId(promotionId);
    return new EtudiantReponse(e.getId(), e.getNom(), e.getPromotionId());
  }

  /**
   * RG28 : une fiche sans historique est supprimée ; sinon désactivée — présences et notes
   * restent dans le tableau et la liste de sélection ne la montre plus.
   */
  @Transactional
  public void supprimerOuDesactiverEtudiant(Long id) {
    var e = etudiants.findById(id).orElseThrow(() ->
        new MetierException(HttpStatus.NOT_FOUND, "ETUDIANT_INCONNU", "Cet étudiant n'existe pas."));
    acces.verifierGestionPromotionStrict(e.getPromotionId());
    if (!aUnHistorique(id)) {
      utilisateurs.findByEtudiantId(id).ifPresent(compte -> {
        throw new MetierException(HttpStatus.CONFLICT, "SUPPRESSION_IMPOSSIBLE",
            "Cette fiche est liée à un compte : désactivez d'abord le compte.");
      });
      etudiants.delete(e);
      return;
    }
    e.setActif(false);
    etudiants.save(e);
  }

  private boolean aUnHistorique(Long etudiantId) {
    return presences.existsByEtudiantId(etudiantId) || exercices.existsByAuteurId(etudiantId)
        || relectures.existsByRelecteurId(etudiantId);
  }

  /** #62 prévu sur GET /api/sessions?promotionId= — suffisant pour savoir si la promotion a des sessions. */
  private boolean sessionsNonVide(Long promotionId) {
    return !sessions.findByPromotionIdOrderByOuvertureAtDesc(promotionId).isEmpty();
  }

  private Promotion chargerPromotion(Long id) {
    return promotions.findById(id).orElseThrow(() ->
        new MetierException(HttpStatus.NOT_FOUND, "PROMOTION_INCONNUE", "Cette promotion n'existe pas."));
  }

  /** Promotion visée par le CORPS d'une écriture : identifiant inconnu → 400 (convention des écritures). */
  private Promotion chargerPromotionDuCorps(Long id) {
    return promotions.findById(id).orElseThrow(() ->
        new MetierException(HttpStatus.BAD_REQUEST, "PROMOTION_INCONNUE", "Cette promotion n'existe pas."));
  }
}
