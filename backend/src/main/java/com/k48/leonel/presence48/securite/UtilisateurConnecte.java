package com.k48.leonel.presence48.securite;

import com.k48.leonel.presence48.entity.Role;

/** Principal stocké dans la session : l'identité de « qui est connecté ». */
public record UtilisateurConnecte(Long id, String login, Role role, Long etudiantId) {
}
