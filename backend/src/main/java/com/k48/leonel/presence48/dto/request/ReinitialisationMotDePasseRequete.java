package com.k48.leonel.presence48.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** #60 / SF-20 : mot de passe provisoire donné par l'ADMIN (≥ 8, RG24). */
public record ReinitialisationMotDePasseRequete(
    @NotBlank @Size(min = 8) String motDePasseInitial) {
}
