package com.k48.leonel.presence48.dto.request;

import com.k48.leonel.presence48.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** #60 / SF-20 : modification d'un compte (contrat UtilisateurModification). */
public record ModificationUtilisateurRequete(
    @NotBlank @Size(max = 150) String nomAffiche,
    @NotNull Role role,
    @NotNull Boolean actif,
    Long etudiantId) {
}
