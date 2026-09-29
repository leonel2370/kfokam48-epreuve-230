package com.k48.leonel.presence48.dto.request;

import com.k48.leonel.presence48.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** #60 / SF-20 : création d'un compte (contrat UtilisateurCreation). */
public record CreationUtilisateurRequete(
    @NotBlank @Size(max = 50) String login,
    @NotBlank @Size(max = 150) String nomAffiche,
    @NotNull Role role,
    @NotBlank @Size(min = 8) String motDePasseInitial,
    Long etudiantId) {
}
