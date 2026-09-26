package com.k48.leonel.presence48.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corps imposé par le contrat : { code, etudiantId }. */
public record PresenceRequete(
    @NotBlank @Schema(description = "Code de présence affiché par le formateur (6 caractères).", example = "K7MX4Q") String code,
    @NotNull @Schema(description = "Identifiant de l'étudiant qui marque sa présence.", example = "1") Long etudiantId) {
}
