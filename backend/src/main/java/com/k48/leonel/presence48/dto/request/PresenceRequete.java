package com.k48.leonel.presence48.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corps imposé par le contrat : { code, etudiantId }. */
public record PresenceRequete(@NotBlank String code, @NotNull Long etudiantId) {
}
