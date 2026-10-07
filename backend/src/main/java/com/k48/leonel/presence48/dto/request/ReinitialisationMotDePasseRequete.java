package com.k48.leonel.presence48.dto.request;

import jakarta.validation.constraints.NotBlank;

/** #60 / SF-20 : mot de passe provisoire donné par l'ADMIN (longueur vérifiée par PolitiqueMotDePasse, RG24). */
public record ReinitialisationMotDePasseRequete(
    @NotBlank String motDePasseInitial) {
}
