package com.k48.leonel.presence48.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corps imposé par le contrat : { sessionId, etudiantId, lien }. */
public record DepotExerciceRequete(@NotNull Long sessionId, @NotNull Long etudiantId, @NotBlank String lien) {
}
