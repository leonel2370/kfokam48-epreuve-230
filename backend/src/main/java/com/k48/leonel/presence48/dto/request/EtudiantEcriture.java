package com.k48.leonel.presence48.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** #61 / SF-22 : corps de POST et PUT /api/etudiants (contrat EtudiantEcriture). */
public record EtudiantEcriture(@NotBlank @Size(max = 150) String nom, @NotNull Long promotionId) {
}
