package com.k48.leonel.presence48.securite;

import com.k48.leonel.presence48.entity.Role;
import com.k48.leonel.presence48.exception.MetierException;
import com.k48.leonel.presence48.repository.UtilisateurRepository;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Contrôles d'accès métier (RG25, RG26, RISQUE-1). Sur les opérations imposées, ils ne s'appliquent
 * que si l'appelant est connecté : sans session, le comportement v1 est conservé (RG22, B2).
 */
@Component
public class ControleAcces {

  private static final String ACCES_REFUSE = "Vous n'avez pas les droits pour cette action.";

  private final UtilisateurRepository utilisateurs;

  public ControleAcces(UtilisateurRepository utilisateurs) {
    this.utilisateurs = utilisateurs;
  }

  /** L'utilisateur connecté, s'il y en a un. */
  public Optional<UtilisateurConnecte> connecte() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.getPrincipal() instanceof UtilisateurConnecte u ? Optional.of(u) : Optional.empty();
  }

  /** ADMIN, ou FORMATEUR rattaché à la promotion (RG26). Sans session : autorisé (opération imposée). */
  public void verifierGestionPromotion(Long promotionId) {
    connecte().ifPresent(u -> {
      var autorise = u.role() == Role.ADMIN
          || (u.role() == Role.FORMATEUR && utilisateurs.promotionsDuFormateur(u.id()).contains(promotionId));
      if (!autorise) {
        throw new MetierException(HttpStatus.FORBIDDEN, "ACCES_REFUSE", ACCES_REFUSE);
      }
    });
  }

  /** Sur une opération imposée appelée connecté, l'identité envoyée doit être celle de l'étudiant connecté. */
  public void verifierIdentite(Long etudiantId) {
    connecte().ifPresent(u -> {
      if (u.role() != Role.ETUDIANT || !u.etudiantId().equals(etudiantId)) {
        throw new MetierException(HttpStatus.FORBIDDEN, "IDENTITE_DIFFERENTE",
            "L'identité envoyée ne correspond pas à l'utilisateur connecté.");
      }
    });
  }
}
