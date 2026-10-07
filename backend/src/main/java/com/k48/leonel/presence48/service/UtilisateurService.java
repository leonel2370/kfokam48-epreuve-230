package com.k48.leonel.presence48.service;

import com.k48.leonel.presence48.dto.request.CreationUtilisateurRequete;
import com.k48.leonel.presence48.dto.request.ModificationUtilisateurRequete;
import com.k48.leonel.presence48.dto.response.PageUtilisateursReponse;
import com.k48.leonel.presence48.dto.response.UtilisateurReponse;
import com.k48.leonel.presence48.entity.Role;
import com.k48.leonel.presence48.entity.Utilisateur;
import com.k48.leonel.presence48.exception.MetierException;
import com.k48.leonel.presence48.repository.FormateurPromotionRepository;
import com.k48.leonel.presence48.repository.UtilisateurRepository;
import java.time.Clock;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * #60 / SF-20 / EF21 : CRUD des comptes, réservé à l'ADMIN (RG25).
 * RG27 : login unique → 409 LOGIN_DEJA_UTILISE. RG28 : jamais de suppression physique, désactivation ;
 * le dernier admin actif ne peut être ni désactivé ni rétrogradé (409 SUPPRESSION_IMPOSSIBLE, #130). RG23 : un compte
 * créé ou réinitialisé doit changer son mot de passe à la première connexion ; RG24 : 8 caractères minimum.
 */
@Service
public class UtilisateurService {

  private static final String DERNIER_ADMIN =
      "Le dernier administrateur actif ne peut être ni désactivé ni changé de rôle.";

  private final UtilisateurRepository utilisateurs;
  private final FormateurPromotionRepository rattachements;
  private final PasswordEncoder encodeur;
  private final Clock horloge;

  public UtilisateurService(UtilisateurRepository utilisateurs, FormateurPromotionRepository rattachements,
      PasswordEncoder encodeur, Clock horloge) {
    this.utilisateurs = utilisateurs;
    this.rattachements = rattachements;
    this.encodeur = encodeur;
    this.horloge = horloge;
  }

  @Transactional(readOnly = true)
  public PageUtilisateursReponse lister(int page, int taille) {
    var resultat = utilisateurs.findAllByOrderByLoginAsc(PageRequest.of(page, taille));
    return new PageUtilisateursReponse(resultat.getContent().stream().map(UtilisateurReponse::de).toList(),
        page, taille, resultat.getTotalElements());
  }

  @Transactional(readOnly = true)
  public UtilisateurReponse lire(Long id) {
    return UtilisateurReponse.de(charger(id));
  }

  @Transactional
  public UtilisateurReponse creer(CreationUtilisateurRequete requete) {
    if (utilisateurs.findByLogin(requete.login()).isPresent()) {
      throw new MetierException(HttpStatus.CONFLICT, "LOGIN_DEJA_UTILISE", "Cet identifiant est déjà pris.");
    }
    if (requete.role() == Role.ETUDIANT && requete.etudiantId() == null) {
      throw new MetierException(HttpStatus.BAD_REQUEST, "CHAMP_MANQUANT",
          "Un compte étudiant doit être lié à une fiche étudiant.");
    }
    if (requete.etudiantId() != null && utilisateurs.existsByEtudiantId(requete.etudiantId())) {
      throw new MetierException(HttpStatus.CONFLICT, "LOGIN_DEJA_UTILISE",
          "Cette fiche étudiant a déjà un compte.");
    }
    var u = new Utilisateur(requete.login(), encodeur.encode(requete.motDePasseInitial()), requete.role(),
        requete.nomAffiche(), requete.etudiantId(), true, horloge.instant()); // RG23 : mot de passe provisoire
    return UtilisateurReponse.de(utilisateurs.save(u));
  }

  @Transactional
  public UtilisateurReponse modifier(Long id, ModificationUtilisateurRequete requete) {
    var u = charger(id);
    if (requete.role() == Role.ETUDIANT && requete.etudiantId() == null) {
      throw new MetierException(HttpStatus.BAD_REQUEST, "CHAMP_MANQUANT",
          "Un compte étudiant doit être lié à une fiche étudiant.");
    }
    if (requete.etudiantId() != null) {
      utilisateurs.findByEtudiantId(requete.etudiantId())
          .filter(autre -> !autre.getId().equals(id))
          .ifPresent(autre -> {
            throw new MetierException(HttpStatus.CONFLICT, "LOGIN_DEJA_UTILISE",
                "Cette fiche étudiant a déjà un compte.");
          });
    }
    var resteAdminActif = requete.role() == Role.ADMIN && requete.actif();
    if (!resteAdminActif) {
      refuserSiDernierAdminActif(u);
    }
    if (u.getRole() == Role.FORMATEUR && requete.role() != Role.FORMATEUR) {
      // RG26 : seul un compte FORMATEUR est rattaché à des promotions.
      rattachements.deleteAll(rattachements.findByUtilisateurId(id));
    }
    u.setNomAffiche(requete.nomAffiche());
    u.setRole(requete.role());
    u.setActif(requete.actif());
    u.setEtudiantId(requete.etudiantId());
    return UtilisateurReponse.de(utilisateurs.save(u));
  }

  /** RG28 : désactivation, jamais de suppression physique. */
  @Transactional
  public void desactiver(Long id) {
    var u = charger(id);
    refuserSiDernierAdminActif(u);
    u.setActif(false);
    utilisateurs.save(u);
  }

  /** RG23 + RG24 : mot de passe provisoire, à changer à la connexion suivante. */
  @Transactional
  public void reinitialiserMotDePasse(Long id, String motDePasseInitial) {
    var u = charger(id);
    u.setMotDePasseHash(encodeur.encode(motDePasseInitial));
    u.setDoitChangerMotDePasse(true);
    u.setEchecsConnexion(0);
    u.setBloqueJusquA(null);
    utilisateurs.save(u);
  }

  private Utilisateur charger(Long id) {
    return utilisateurs.findById(id).orElseThrow(() ->
        new MetierException(HttpStatus.NOT_FOUND, "UTILISATEUR_INTROUVABLE", "Ce compte n'existe pas."));
  }

  /** RG28 : le compte s'apprête à ne plus être un administrateur actif ; refusé s'il est le dernier. */
  private void refuserSiDernierAdminActif(Utilisateur u) {
    if (u.getRole() != Role.ADMIN || !u.isActif()) {
      return;
    }
    if (utilisateurs.verrouillerActifs(Role.ADMIN).size() <= 1) {
      throw new MetierException(HttpStatus.CONFLICT, "SUPPRESSION_IMPOSSIBLE", DERNIER_ADMIN);
    }
  }
}
