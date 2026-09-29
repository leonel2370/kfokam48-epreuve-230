package com.k48.leonel.presence48.dto.response;

import com.k48.leonel.presence48.entity.Role;
import com.k48.leonel.presence48.entity.Utilisateur;

/** #60 / SF-20 : vue d'un compte — jamais le hash de mot de passe. */
public record UtilisateurReponse(Long id, String login, String nomAffiche, Role role, Long etudiantId,
    boolean actif, boolean doitChangerMotDePasse) {

  public static UtilisateurReponse de(Utilisateur u) {
    return new UtilisateurReponse(u.getId(), u.getLogin(), u.getNomAffiche(), u.getRole(), u.getEtudiantId(),
        u.isActif(), u.isDoitChangerMotDePasse());
  }
}
