package com.k48.leonel.presence48.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corps imposé par le contrat : { titre, promotionId }. */
public record OuvertureSessionRequete(@NotBlank @Size(max = 200) String titre, @NotNull Long promotionId) {
}
