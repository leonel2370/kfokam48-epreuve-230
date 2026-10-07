package com.k48.leonel.presence48.service;

import com.k48.leonel.presence48.dto.response.EtudiantReponse;
import com.k48.leonel.presence48.dto.response.FicheEtudiantReponse;
import com.k48.leonel.presence48.dto.response.PromotionReponse;
import com.k48.leonel.presence48.dto.response.UtilisateurReponse;
import com.k48.leonel.presence48.entity.Etudiant;
import com.k48.leonel.presence48.entity.FormateurPromotion;
import com.k48.leonel.presence48.entity.Promotion;
import com.k48.leonel.presence48.entity.Role;
import com.k48.leonel.presence48.entity.Utilisateur;
import com.k48.leonel.presence48.exception.MetierException;
import com.k48.leonel.presence48.repository.EtudiantRepository;
import com.k48.leonel.presence48.repository.ExerciceRepository;
import com.k48.leonel.presence48.repository.FormateurPromotionRepository;
import com.k48.leonel.presence48.repository.PresenceRepository;
import com.k48.leonel.presence48.repository.PromotionRepository;
import com.k48.leonel.presence48.repository.RelectureRepository;
import com.k48.leonel.presence48.repository.SessionCoursRepository;
import com.k48.leonel.presence48.repository.TentativeCodeRepository;
import com.k48.leonel.presence48.repository.UtilisateurRepository;
import com.k48.leonel.presence48.securite.ControleAcces;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private final TentativeCodeRepository tentatives;
  private final UtilisateurRepository utilisateurs;
  private final FormateurPromotionRepository rattachements;
  private final ControleAcces acces;

  public ReferentielAdminService(PromotionRepository promotions, EtudiantRepository etudiants,
      SessionCoursRepository sessions, PresenceRepository presences, ExerciceRepository exercices,
      RelectureRepository relectures, TentativeCodeRepository tentatives, UtilisateurRepository utilisateurs,
      FormateurPromotionRepository rattachements, ControleAcces acces) {
    this.promotions = promotions;
    this.etudiants = etudiants;
    this.sessions = sessions;
    this.presences = presences;
    this.exercices = exercices;
    this.relectures = relectures;
    this.tentatives = tentatives;
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

  /** #134 : formateurs rattachés à une promotion, triés par nom affiché (ADMIN). */
  @Transactional(readOnly = true)
  public List<UtilisateurReponse> formateursDe(Long promotionId) {
    chargerPromotion(promotionId);
    var ids = rattachements.findByPromotionId(promotionId).stream()
        .map(r -> r.getId().getUtilisateurId()).toList();
    return utilisateurs.findAllById(ids).stream()
        .sorted(Comparator.comparing(Utilisateur::getNomAffiche))
        .map(UtilisateurReponse::de).toList();
  }

  /**
   * RG26 : remplace la liste des formateurs d'une promotion ; vide, elle les retire tous (#134).
   * Seul un compte FORMATEUR actif est admis (400 ROLE_INCOMPATIBLE) ; inconnu : 404.
   */
  @Transactional
  public void rattacherFormateurs(Long promotionId, List<Long> utilisateurIds) {
    chargerPromotion(promotionId);
    var ids = new LinkedHashSet<>(utilisateurIds);
    var comptes = utilisateurs.findAllById(ids).stream()
        .collect(Collectors.toMap(Utilisateur::getId, Function.identity()));
    for (Long id : ids) {
      var u = comptes.get(id);
      if (u == null) {
        throw new MetierException(HttpStatus.NOT_FOUND, "UTILISATEUR_INTROUVABLE", "Cet utilisateur n'existe pas.");
      }
      if (u.getRole() != Role.FORMATEUR || !u.isActif()) {
        throw new MetierException(HttpStatus.BAD_REQUEST, "ROLE_INCOMPATIBLE",
            "Seul un compte FORMATEUR actif peut être rattaché à une promotion.");
      }
    }
    var actuels = rattachements.findByPromotionId(promotionId);
    rattachements.deleteAll(actuels.stream().filter(r -> !ids.contains(r.getId().getUtilisateurId())).toList());
    var dejaRattaches = actuels.stream().map(r -> r.getId().getUtilisateurId()).collect(Collectors.toSet());
    ids.stream().filter(id -> !dejaRattaches.contains(id))
        .forEach(id -> rattachements.save(new FormateurPromotion(id, promotionId)));
  }

  // --- Fiches étudiants (ADMIN, FORMATEUR de la promotion) ---

  /** #134 : toutes les fiches d'une promotion, désactivées comprises, avec le compte lié (HYP-15). */
  @Transactional(readOnly = true)
  public List<FicheEtudiantReponse> fichesDe(Long promotionId) {
    acces.verifierGestionPromotionStrict(promotionId);
    chargerPromotion(promotionId);
    var fiches = etudiants.findByPromotionIdOrderByNomAsc(promotionId);
    Map<Long, String> logins = utilisateurs.findByEtudiantIdIn(fiches.stream().map(Etudiant::getId).toList())
        .stream().collect(Collectors.toMap(Utilisateur::getEtudiantId, Utilisateur::getLogin));
    return fiches.stream().map(e -> new FicheEtudiantReponse(e.getId(), e.getNom(), e.getPromotionId(),
        e.isActif(), logins.get(e.getId()))).toList();
  }

  @Transactional
  public EtudiantReponse creerEtudiant(Long promotionId, String nom) {
    acces.verifierGestionPromotionStrict(promotionId);
    chargerPromotionDuCorps(promotionId);
    var e = etudiants.save(new Etudiant(nom, promotionId));
    return new EtudiantReponse(e.getId(), e.getNom(), e.getPromotionId());
  }

  /**
   * RG26 (#129) : il faut gérer la promotion ACTUELLE de la fiche et la promotion d'arrivée ; sans le
   * premier contrôle, un formateur déplaçait vers sa promotion la fiche d'un étudiant d'une autre.
   */
  @Transactional
  public EtudiantReponse modifierEtudiant(Long id, String nom, Long promotionId) {
    var e = etudiants.findById(id).orElseThrow(() ->
        new MetierException(HttpStatus.NOT_FOUND, "ETUDIANT_INCONNU", "Cet étudiant n'existe pas."));
    acces.verifierGestionPromotionStrict(e.getPromotionId());
    acces.verifierGestionPromotionStrict(promotionId);
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

  /** Une tentative de code (RG4) est déjà une trace de l'étudiant : elle compte comme historique (#133). */
  private boolean aUnHistorique(Long etudiantId) {
    return presences.existsByEtudiantId(etudiantId) || exercices.existsByAuteurId(etudiantId)
        || relectures.existsByRelecteurId(etudiantId) || tentatives.existsById(etudiantId);
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
