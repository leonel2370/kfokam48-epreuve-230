package com.k48.leonel.presence48.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Corps imposé par le contrat : { note, commentaire }. La note est lue en décimal pour que 12.5 soit
 * refusé (RG9) au lieu d'être tronqué silencieusement en 12 par la désérialisation.
 */
public record RenduRelectureRequete(@NotNull BigDecimal note, @NotBlank String commentaire) {
}
