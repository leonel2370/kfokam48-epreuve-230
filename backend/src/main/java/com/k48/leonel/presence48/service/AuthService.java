package com.k48.leonel.presence48.service;

import com.k48.leonel.presence48.dto.response.ProfilReponse;
import com.k48.leonel.presence48.entity.Role;
import com.k48.leonel.presence48.entity.Utilisateur;
import com.k48.leonel.presence48.exception.MetierException;
import com.k48.leonel.presence48.repository.UtilisateurRepository;
import com.k48.leonel.presence48.securite.UtilisateurConnecte;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Connexion, profil et mot de passe (SF-15 à SF-18). */
@Service
public class AuthService {

  static final int ECHECS_MAX = 5;
  static final Duration DUREE_BLOCAGE = Duration.ofMinutes(2);
  static final int LONGUEUR_MIN = 8;

  private static final String IDENTIFIANTS_INVALIDES = "IDENTIFIANTS_INVALIDES";
  private static final String MESSAGE_IDENTIFIANTS = "Identifiant ou mot de passe incorrect.";

  private final UtilisateurRepository utilisateurs;
  private final PasswordEncoder encodeur;
  private final Clock horloge;

  public AuthService(UtilisateurRepository utilisateurs, PasswordEncoder encodeur, Clock horloge) {
    this.utilisateurs = utilisateurs;
    this.encodeur = encodeur;
    this.horloge = horloge;
  }

  /**
   * Vérifie les identifiants. RG24 : 5 échecs consécutifs bloquent 2 minutes ; RG28 : compte désactivé refusé.
   * noRollbackFor : le compteur d'échecs doit être enregistré même quand la connexion est refusée.
   */
  @Transactional(noRollbackFor = MetierException.class)
  public UtilisateurConnecte authentifier(String login, String motDePasse) {
    Utilisateur u = utilisateurs.findByLogin(login)
        .orElseThrow(() -> new MetierException(HttpStatus.UNAUTHORIZED, IDENTIFIANTS_INVALIDES, MESSAGE_IDENTIFIANTS));
    Instant maintenant = horloge.instant();
    if (u.getBloqueJusquA() != null && maintenant.isBefore(u.getBloqueJusquA())) {
      throw new MetierException(HttpStatus.TOO_MANY_REQUESTS, "TROP_DE_TENTATIVES",
          "Trop de tentatives, réessayez dans 2 minutes.");
    }
    if (!encodeur.matches(motDePasse, u.getMotDePasseHash())) {
      int echecs = u.getEchecsConnexion() + 1;
      if (echecs >= ECHECS_MAX) {
        u.setBloqueJusquA(maintenant.plus(DUREE_BLOCAGE));
        echecs = 0;
      }
      u.setEchecsConnexion(echecs);
      throw new MetierException(HttpStatus.UNAUTHORIZED, IDENTIFIANTS_INVALIDES, MESSAGE_IDENTIFIANTS);
    }
    if (!u.isActif()) {
      throw new MetierException(HttpStatus.FORBIDDEN, "COMPTE_DESACTIVE", "Ce compte est désactivé.");
    }
    u.setEchecsConnexion(0);
    u.setBloqueJusquA(null);
    return new UtilisateurConnecte(u.getId(), u.getLogin(), u.getRole(), u.getEtudiantId());
  }

  @Transactional(readOnly = true)
  public ProfilReponse profil(Long utilisateurId) {
    Utilisateur u = charger(utilisateurId);
    List<Long> promotions = switch (u.getRole()) {
      case FORMATEUR -> utilisateurs.promotionsDuFormateur(u.getId());
      case ETUDIANT -> utilisateurs.promotionDeLEtudiant(u.getEtudiantId());
      case ADMIN -> List.of();
    };
    return new ProfilReponse(u.getId(), u.getLogin(), u.getNomAffiche(), u.getRole(), u.getEtudiantId(),
        promotions, u.isDoitChangerMotDePasse());
  }

  /** RG24 : 8 caractères minimum, différent de l'ancien ; lève le drapeau RG23. */
  @Transactional
  public void changerMotDePasse(Long utilisateurId, String ancien, String nouveau) {
    Utilisateur u = charger(utilisateurId);
    if (!encodeur.matches(ancien, u.getMotDePasseHash())) {
      throw new MetierException(HttpStatus.UNAUTHORIZED, IDENTIFIANTS_INVALIDES, "Le mot de passe actuel est incorrect.");
    }
    if (nouveau.length() < LONGUEUR_MIN || nouveau.equals(ancien)) {
      throw new MetierException(HttpStatus.BAD_REQUEST, "MOT_DE_PASSE_TROP_FAIBLE",
          "Le mot de passe doit contenir au moins 8 caractères et différer de l'actuel.");
    }
    u.setMotDePasseHash(encodeur.encode(nouveau));
    u.setDoitChangerMotDePasse(false);
  }

  public static boolean estAdmin(UtilisateurConnecte u) {
    return u.role() == Role.ADMIN;
  }

  private Utilisateur charger(Long id) {
    return utilisateurs.findById(id)
        .orElseThrow(() -> new MetierException(HttpStatus.NOT_FOUND, "UTILISATEUR_INTROUVABLE",
            "Cet utilisateur n'existe pas."));
  }
}
